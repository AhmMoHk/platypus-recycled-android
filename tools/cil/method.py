"""Translation of one method body (IL -> Java statements)."""
import collections

from .md import MethodDef, FieldDef, MemberRef, tstr, subst, NAME_BY_PRIM
from .il import decode, ENDS_BLOCK, UNCOND, BRANCHES
from .jtypes import *
from . import xlat as X



# interfaces .NET arrays implement: a value of static type Object may be an array here
ARRAY_IFACES = {'cs.System.Collections.Generic.IEnumerable_1', 'cs.System.Collections.Generic.ICollection_1',
                'cs.System.Collections.Generic.IList_1', 'cs.System.Collections.Generic.IReadOnlyList_1',
                'cs.System.Collections.Generic.IReadOnlyCollection_1', 'cs.System.Collections.IEnumerable',
                'cs.System.Collections.ICollection', 'cs.System.Collections.IList'}

class Block:
    __slots__ = ('start', 'end', 'ins', 'region', 'entry', 'lines', 'term', 'succ', 'idx', 'reached')

    def __init__(self, start):
        self.start = start
        self.end = None
        self.ins = []
        self.region = None
        self.entry = None        # list of merge kinds ('I','J','F','D','O') with cts, or None (empty)
        self.lines = []
        self.succ = []
        self.reached = False


class Region:
    _n = 0

    def __init__(self, kind, start, end, parent):
        Region._n += 1
        self.id = Region._n
        self.kind = kind          # root | body | handler
        self.start, self.end = start, end
        self.parent = parent      # TryStmt (for body/handler) or None
        self.units = []           # Blocks and TryStmts, sorted by start
        self.clause = None
        self.needs_loop = False
        self.pc = f'$pc{self.id}'
        self.label = f'$L{self.id}'
        self.exvar = None         # catch: java Throwable variable

    def contains(self, off):
        return self.start <= off < self.end

    def owner_region(self):
        return self.parent.region if self.parent is not None else None


class TryStmt:
    _n = 0

    def __init__(self, ts, te, clauses, region):
        TryStmt._n += 1
        self.id = TryStmt._n
        self.start, self.try_end = ts, te
        self.clauses = clauses
        self.end = max([te] + [c.h_end for c in clauses] + [c.filter_start or 0 for c in clauses])
        self.region = region      # region containing this statement
        self.body = None
        self.handlers = []        # (clause, Region)
        self.label = f'$X{self.id}'
        self.breakable = False


class MethodX:
    def __init__(self, x, t, m):
        self.x = x
        self.ts = x.ts
        self.mg = x.mg
        self.t = t
        self.m = m
        self.mod = t.module
        self.body = m.body
        self.cls = jclass_name(t.fullname)
        self.owner_ct = ('N', t.fullname, t.is_value) if not t.gparams else \
            ('G', ('N', t.fullname, t.is_value), tuple(('V', i) for i in range(len(t.gparams))))
        self.temps = {}           # name -> jt
        self.ntemp = 0
        self.stack = []
        self.out = None           # current block lines
        self.locals = [self.ts.jt(l) for l in self.body.locals]
        self.local_cts = list(self.body.locals)
        self.param_cts = list(m.sig.params)
        self.merge = {}           # (depth, kind) -> var
        self.cur_block = None
        self.extra_decls = []
        self.copy_params = set()

    # ------------------------------------------------------------------ utilities
    def err(self, msg):
        raise X.XError(msg)

    def temp(self, jt):
        self.ntemp += 1
        n = f't{self.ntemp}'
        self.temps[n] = jt if jt != 'null' else 'Object'
        return n

    def emit(self, s):
        self.out.append(s)

    def push(self, v):
        self.stack.append(v)

    def pop(self):
        if not self.stack:
            self.err('stack underflow')
        return self.stack.pop()

    def popn(self, n):
        if n == 0:
            return []
        if len(self.stack) < n:
            self.err('stack underflow')
        r = self.stack[-n:]
        del self.stack[-n:]
        return r

    def jt(self, ct):
        return self.ts.jt(ct)

    def kind(self, ct):
        return self.ts.kind(ct)

    def ext_jt(self, ct):
        """erasure used for signatures of external (runtime) methods: arrays of generic params become Object"""
        if ct[0] == 'A' and ct[1][0] in 'VW':
            return 'Object'
        if ct[0] == 'R' and ct[1][0] == 'A' and ct[1][1][0] in 'VW':
            return 'cs.rt.RefO'
        return self.jt(ct)

    def simple(self, e):
        return e.replace('_', 'a').replace('$', 'a').replace('.', 'a').isalnum() or e in ('this', 'null')

    def spill_val(self, v, copy_struct=True):
        if v.lv is not None or v.ftn is not None or v.token is not None:
            return v
        if v.const is not None or (v.jt == 'null'):
            return v
        t = self.temp(v.jt)
        e = v.e
        if copy_struct and v.ct is not None and self.kind(v.ct) == 'struct' and not v.fresh:
            e = f'{e}.$copy()' if v.jt != 'Object' else f'cs.rt.Rt.copyStruct({e})'
        self.emit(f'{t} = {e};')
        return X.Val(t, v.jt, v.ct, frozenset([t]), False, v.fresh)

    def spill_if(self, pred):
        for i, v in enumerate(self.stack):
            if pred(v):
                self.stack[i] = self.spill_val(v)

    def spill_heap(self):
        self.spill_if(lambda v: v.heap and v.lv is None)

    def spill_var(self, name):
        self.spill_if(lambda v: name in v.deps and v.lv is None)

    def spill_all(self):
        self.spill_if(lambda v: v.lv is None and v.ftn is None and v.token is None and v.const is None and not v.e.startswith('t') or (v.heap and v.lv is None))

    # ------------------------------------------------------------------ type conversion
    def assignable(self, src, dst):
        if src == dst or dst == 'Object':
            return True
        if src == 'null':
            return True
        if src in JPRIMS or dst in JPRIMS:
            return False
        if src.endswith('[]') or dst.endswith('[]'):
            return False
        return self.x.java_subclass(src, dst)

    def conv(self, v, to_jt, to_ct=None):
        """Expression converting Val v to java type to_jt."""
        f = v.jt
        if to_jt == f:
            return v.e
        if to_jt in JPRIMS:
            if f in JPRIMS:
                return X.prim_conv(v.e, f, to_jt)
            if f == 'null':
                return X.Xlat.default_of(to_jt)
            return X.unbox_expr(v.e, to_jt)
        if f in JPRIMS:
            return X.box_expr(self.x, v.e, f, v.ct)
        if f == 'null':
            return f'(({to_jt}) null)'
        if self.assignable(f, to_jt):
            return v.e
        if to_jt.endswith('[]') and not f.endswith('[]') and f != 'Object':
            return f'(({to_jt}) (Object) ({v.e}))'
        if (f == 'Object' or f.endswith('[]')) and to_jt in ARRAY_IFACES:
            return f'(({to_jt}) cs.rt.Rt.iface({v.e}))'
        if f == 'Object' or to_jt.endswith('[]'):
            return f'(({to_jt}) {v.e})' if self.simple(v.e) else f'(({to_jt}) ({v.e}))'
        return f'(({to_jt}) (Object) ({v.e}))'

    def boxed(self, v, ct):
        """Value as Object, boxed according to .NET type ct (for generic parameters / object)."""
        k = self.kind(ct) if ct is not None else None
        if v.jt in JPRIMS:
            if k == 'enum':
                return f'cs.rt.BoxedEnum.of({self.x.enum_class_expr(ct)}, {X.prim_conv(v.e, v.jt, "long")})'
            if k == 'prim':
                tj = PRIM_J[ct[1]]
                if ct[1] in ('u2', 'char'):
                    return f'Character.valueOf({X.prim_conv(v.e, v.jt, "char")})'
                return f'{BOX_J[tj]}.valueOf({X.prim_conv(v.e, v.jt, tj)})'
            return X.box_expr(self.x, v.e, v.jt, v.ct)
        if k == 'struct' and not v.fresh:
            if v.jt == 'Object':
                return f'cs.rt.Rt.copyStruct({v.e})'
            return f'({v.e}).$copy()' if not self.simple(v.e) else f'{v.e}.$copy()'
        return v.e

    def from_object(self, e, ct, jt=None):
        """Convert an Object-typed expression (erased generic) to the java type of .NET type ct."""
        k = self.kind(ct)
        j = self.jt(ct) if jt is None else jt
        if k == 'prim':
            return X.unbox_expr(e, j)
        if k == 'enum':
            return X.unbox_expr(e, j)
        if j == 'Object':
            return e
        if k == 'array':
            return f'(({j}) cs.rt.Rt.castArray({e}, {self.x.type_expr(ct[1], self)}))'
        if j in ARRAY_IFACES:
            return f'(({j}) cs.rt.Rt.iface({e}))'
        return f'(({j}) ({e}))'

    # ------------------------------------------------------------------ translation driver
    def translate(self):
        code = self.body.code
        ins = decode(code)
        if not ins:
            return ['throw new IllegalStateException("empty body");']
        self.ins = ins
        offs = {i.off: k for k, i in enumerate(ins)}
        self.offs = offs
        ehs = self.body.ehs
        for eh in ehs:
            if eh.kind == 'filter':
                self.err('exception filter')
        # leaders
        lead = {0}
        for i in ins:
            if i.op in BRANCHES:
                lead.add(i.arg)
                lead.add(i.next)
            elif i.op == 'switch':
                lead.update(i.arg)
                lead.add(i.next)
            elif i.op in ENDS_BLOCK:
                lead.add(i.next)
        for eh in ehs:
            lead.update([eh.try_start, eh.try_end, eh.h_start, eh.h_end])
        end = len(code)
        lead = sorted(o for o in lead if o < end and o in offs)
        blocks = []
        bmap = {}
        for k, s in enumerate(lead):
            b = Block(s)
            b.end = lead[k + 1] if k + 1 < len(lead) else end
            b.idx = k
            blocks.append(b)
            bmap[s] = b
        for i in ins:
            # find block
            pass
        bi = 0
        for i in ins:
            while not (blocks[bi].start <= i.off < blocks[bi].end):
                bi += 1
            blocks[bi].ins.append(i)
        self.blocks = blocks
        self.bmap = bmap
        self.build_regions()
        self.prescan()
        # translate blocks in order
        self.blocks_entry_kinds = {}
        for eh in ehs:
            if eh.kind == 'catch':
                self.blocks_entry_kinds[eh.h_start] = [('O', eh.catch_type)]
        bmap[0].reached = True
        for b in blocks:
            self.translate_block(b)
        return self.assemble()

    # ------------------------------------------------------------------ regions
    def build_regions(self):
        ehs = self.body.ehs
        root = Region('root', 0, len(self.body.code), None)
        self.root = root
        groups = collections.OrderedDict()
        for eh in ehs:
            groups.setdefault((eh.try_start, eh.try_end), []).append(eh)
        stmts = []
        for (ts, te), cl in groups.items():
            stmts.append(TryStmt(ts, te, cl, None))
        # sort: outer first (by start, then larger span first)
        stmts.sort(key=lambda s: (s.start, -s.end, -s.try_end))
        regions = [root]

        def innermost(start, end):
            best = root
            for r in regions:
                if r.start <= start and end <= r.end:
                    if (r.end - r.start) <= (best.end - best.start):
                        best = r
            return best

        for s in stmts:
            parent = innermost(s.start, s.end)
            s.region = parent
            parent.units.append(s)
            s.body = Region('body', s.start, s.try_end, s)
            regions.append(s.body)
            for c in s.clauses:
                hr = Region('handler', c.h_start, c.h_end, s)
                hr.clause = c
                s.handlers.append((c, hr))
                regions.append(hr)
        self.regions = regions
        for b in self.blocks:
            r = innermost(b.start, b.start + 1)
            b.region = r
            r.units.append(b)
        for r in regions:
            r.units.sort(key=lambda u: (u.start, 0 if isinstance(u, TryStmt) else 1))
        for st in stmts:
            for c, h in st.handlers:
                h.exvar = f'$ex{st.id}'

    def region_chain(self, r):
        out = []
        while r is not None:
            out.append(r)
            r = r.parent.region if r.parent is not None else None
        return out

    def jump(self, src_block, target):
        """Java statements jumping from src_block to IL offset target."""
        rb = src_block.region
        tb = self.bmap.get(target)
        if tb is None:
            self.err(f'jump to non-block {target:x}')
        rt = tb.region
        # target is the start of a try statement (possibly nested) contained in rb's units -> jump inside rb
        chain_t = self.region_chain(rt)
        if rt is not rb and rb in chain_t:
            # entering nested try statements at their start: jump to the outermost one below rb
            k = chain_t.index(rb)
            child = chain_t[k - 1].parent      # TryStmt directly in rb
            if child.start != target:
                self.err('jump into the middle of a try region')
            rb.needs_loop = True
            return [f'{rb.pc} = {target};', f'continue {rb.label};']
        if rt is rb:
            rb.needs_loop = True
            return [f'{rb.pc} = {target};', f'continue {rb.label};']
        chain_b = self.region_chain(rb)
        if rt in chain_b:
            k = chain_b.index(rt)
            child = chain_b[k - 1].parent
            child.breakable = True
            rt.needs_loop = True
            return [f'{rt.pc} = {target};', f'break {child.label};']
        self.err(f'unsupported jump {src_block.start:x}->{target:x}')

    # ------------------------------------------------------------------ prescan
    def prescan(self):
        hasthis = self.m.sig.hasthis
        for i in self.ins:
            if i.op in ('starg', 'ldarga'):
                n = i.arg - (1 if hasthis else 0)
                if n >= 0 and self.kind(self.param_cts[n]) == 'struct':
                    self.copy_params.add(n)

    # ------------------------------------------------------------------ blocks
    def entry_stack(self, b):
        kinds = self.blocks_entry_kinds.get(b.start)
        st = []
        if not kinds:
            return st
        for d, (k, ct) in enumerate(kinds):
            if k == 'P':
                st.append(ct)
                continue
            var = self.merge_var(d, k)
            jt = X.KIND_JT[k]
            if k == 'O' and ct is not None:
                j = self.jt(ct)
                if j not in JPRIMS:
                    jt = 'Object'
            st.append(X.Val(var, jt, ct, frozenset([var])))
        return st

    def merge_var(self, d, k):
        key = (d, k)
        if key not in self.merge:
            n = f'$s{d}{k}'
            self.merge[key] = n
            self.temps[n] = X.KIND_JT[k]
        return self.merge[key]

    def flow_to(self, target, src_block):
        """Assign the current stack to the merge variables of target block."""
        if not self.stack:
            if target in self.blocks_entry_kinds and self.blocks_entry_kinds[target]:
                pass
            return
        kinds = []
        for v in self.stack:
            if v.lv is not None:
                kinds.append(('P', v))
                continue
            k = X.jkind(v.jt) if v.jt not in ('null',) else 'O'
            kinds.append((k, v.ct))
        prev = self.blocks_entry_kinds.get(target)
        if prev is not None:
            if len(prev) != len(kinds):
                self.err('stack depth mismatch at merge')
            merged = []
            for (pk, pct), (k, ct) in zip(prev, kinds):
                if pk == 'P' or k == 'P':
                    if pk != k or pct.lv.e != ct.lv.e:
                        self.err('different pointers merged')
                    merged.append((pk, pct))
                    continue
                if pk != k:
                    if {pk, k} <= {'F', 'D'}:
                        k = 'D'
                    elif {pk, k} <= {'I', 'J'}:
                        k = 'J'
                    else:
                        self.err(f'stack kind mismatch at merge {pk} {k}')
                merged.append((k, pct or ct))
            kinds = merged
            tb = self.bmap[target]
            if tb.lines and tb.start <= src_block.start:
                if [k for k, _ in prev] != [k for k, _ in kinds]:
                    self.err('merge type change on back edge')
        self.blocks_entry_kinds[target] = kinds

    def assign_merge(self, target):
        kinds = self.blocks_entry_kinds.get(target) or []
        for d, v in enumerate(self.stack):
            if v.lv is not None:
                continue
            k = kinds[d][0] if d < len(kinds) else X.jkind(v.jt)
            var = self.merge_var(d, k)
            e = self.conv(v, X.KIND_JT[k]) if k != 'O' else (self.boxed(v, v.ct) if v.jt in JPRIMS else v.e)
            if k == 'O' and v.ct is not None and self.kind(v.ct) == 'struct' and not v.fresh:
                e = f'{v.e}.$copy()' if v.jt != 'Object' else f'cs.rt.Rt.copyStruct({v.e})'
            self.emit(f'{var} = {e};')

    def translate_block(self, b):
        self.cur_block = b
        self.out = []
        self.stack = self.entry_stack(b)
        if b.region.kind == 'handler' and b.start == b.region.start and b.region.clause.kind == 'catch':
            pass
        cur = None
        try:
            for i in b.ins:
                cur = i
                self.ins_one(i, b)
            last = b.ins[-1] if b.ins else None
            if last is None or last.op not in ENDS_BLOCK:
                # falls through to next block
                nxt = last.next if last is not None else b.end
                if nxt in self.bmap:
                    self.flow_to(nxt, b)
                    self.assign_merge(nxt)
                    if self.bmap[nxt].region is not b.region:
                        # fall into a nested try statement at its start (fine: next case label) - else jump
                        nb = self.bmap[nxt]
                        chain = self.region_chain(nb.region)
                        if b.region not in chain:
                            self.out.extend(self.jump(b, nxt))
        except X.XError as e:
            raise X.XError(f'IL_{(cur.off if cur else 0):04x} {cur.op if cur else ""}: {e}')
        b.lines = self.out

    # ------------------------------------------------------------------ assembly
    def assemble(self):
        lines = []
        decl = []
        hasthis = self.m.sig.hasthis
        for n in sorted(self.copy_params):
            decl.append(f'a{n} = a{n}.$copy();')
        for k, j in enumerate(self.locals):
            ct = self.local_cts[k]
            if ct[0] == 'R':
                continue
            if self.kind(ct) == 'struct':
                decl.append(f'{j} l{k} = new {j}();')
            else:
                decl.append(f'{j} l{k} = {X.Xlat.default_of(j)};')
        for n, j in self.temps.items():
            decl.append(f'{j} {n} = {X.Xlat.default_of(j)};')
        exv = set()
        for r in self.regions:
            decl.append(f'int {r.pc} = {r.start};')
            if r.exvar and r.exvar not in exv:
                exv.add(r.exvar)
                decl.append(f'cs.System.Exception {r.exvar} = null;')
        body = self.emit_region(self.root, 0)
        return decl + body

    def emit_region(self, r, depth):
        out = []
        units = r.units
        single = len(units) == 1 and not r.needs_loop
        if single:
            out.extend(self.emit_unit(units[0], r, depth))
            return out
        out.append(f'{r.label}: while (true) {{')
        out.append(f'    switch ({r.pc}) {{')
        for u in units:
            out.append(f'    case {u.start}:')
            for line in self.emit_unit(u, r, depth):
                out.append('        ' + line)
        out.append('    default: throw new IllegalStateException("bad pc " + ' + r.pc + ');')
        out.append('    }')
        out.append('}')
        return out

    def emit_unit(self, u, r, depth):
        if isinstance(u, Block):
            if not u.reached and not u.lines:
                return ['throw new IllegalStateException("unreachable");']
            return list(u.lines)
        s = u
        out = [f'{s.body.pc} = {s.start};', f'{s.label}: {{', '    try {']
        for line in self.emit_region(s.body, depth + 1):
            out.append('        ' + line)
        catches = [(c, h) for c, h in s.handlers if c.kind == 'catch']
        fin = [(c, h) for c, h in s.handlers if c.kind in ('finally', 'fault')]
        tv = f'$t{s.id}'
        if catches:
            out.append(f'    }} catch (Throwable {tv}) {{')
            ev = catches[0][1].exvar
            out.append(f'        {ev} = cs.System.Exception.wrap({tv});')
            closed = False
            for k, (c, h) in enumerate(catches):
                jt = self.jt(c.catch_type)
                cond = None if jt in ('Object', 'cs.System.Exception') else f'{ev} instanceof {jt}'
                hl = [f'{h.pc} = {h.start};', f'{self.merge_var(0, "O")} = {ev};'] + self.emit_region(h, depth + 1)
                if cond is None:
                    out.append('        {' if k == 0 else '        else {')
                    closed = True
                else:
                    out.append(f'        {"if" if k == 0 else "else if"} ({cond}) {{')
                out.extend('            ' + y for y in hl)
                out.append('        }')
                if closed:
                    break
            if not closed:
                out.append(f'        else throw {tv};')
        if fin:
            c, h = fin[0]
            if c.kind == 'finally':
                out.append('    } finally {')
                out.append(f'        {h.pc} = {h.start};')
                for y in self.emit_region(h, depth + 1):
                    out.append('        ' + y)
            else:  # fault
                if catches:
                    self.err('fault with catch')
                out.append(f'    }} catch (Throwable {tv}f) {{')
                out.append(f'        {h.pc} = {h.start};')
                for y in self.emit_region(h, depth + 1):
                    out.append('        ' + y)
                out.append(f'        throw {tv}f;')
        out.append('    }')
        out.append('}')
        if s.breakable:
            out.append(f'continue {r.label};')
        return out

    # ------------------------------------------------------------------ instructions
    def ins_one(self, i, b):
        op = i.op
        h = getattr(self, 'op_' + op.replace('.', '_'), None)
        if h is None:
            self.err(f'unsupported opcode {op}')
        h(i, b)

    # --- args / locals
    def arg_info(self, n):
        if self.m.sig.hasthis:
            if n == 0:
                return None
            n -= 1
        return n

    def op_ldarg(self, i, b):
        n = self.arg_info(i.arg)
        if n is None:
            if self.t.is_value:
                self.push(X.Val('this', self.cls, ('R', self.owner_ct), lv=X.LV('struct', 'this', self.owner_ct, self.cls)))
            else:
                self.push(X.Val('this', self.cls, self.owner_ct))
            return
        ct = self.param_cts[n]
        name = f'a{n}'
        if ct[0] == 'R':
            el = ct[1]
            j = self.jt(ct)
            if self.kind(el) == 'struct':
                self.push(X.Val(name, j, ct, frozenset([name]), lv=X.LV('struct', name, el, self.jt(el))))
            else:
                self.push(X.Val(name, j, ct, frozenset([name]), lv=X.LV('cell', name, el, self.jt(el))))
            return
        self.push(self.load_typed(name, ct, frozenset([name]), False))

    def load_typed(self, e, ct, deps, heap):
        j = self.jt(ct)
        if ct[0] == 'P' and ct[1] == 'u1':
            return X.Val(f'({e} & 0xff)', 'int', ct, deps, heap)
        return X.Val(e, j, ct, deps, heap)

    def op_ldarga(self, i, b):
        n = self.arg_info(i.arg)
        if n is None:
            self.push(X.Val('this', self.cls, ('R', self.owner_ct), lv=X.LV('struct', 'this', self.owner_ct, self.cls)))
            return
        ct = self.param_cts[n]
        name = f'a{n}'
        if ct[0] == 'R':
            self.err('ldarga of byref param')
        k = 'struct' if self.kind(ct) == 'struct' else 'var'
        self.push(X.Val(name, self.jt(ct), ('R', ct), frozenset([name]), lv=X.LV(k, name, ct, self.jt(ct))))

    def op_starg(self, i, b):
        v = self.pop()
        n = self.arg_info(i.arg)
        if n is None:
            self.err('starg this')
        name = f'a{n}'
        ct = self.param_cts[n]
        self.spill_var(name)
        if ct[0] == 'R':
            self.err('starg byref')
        self.store(name, ct, v)

    def op_ldloc(self, i, b):
        if self.local_cts[i.arg][0] == 'R':
            v = getattr(self, 'byref_locals', {}).get(i.arg)
            if v is None:
                self.err('byref local read before assignment')
            self.push(v)
            return
        name = f'l{i.arg}'
        self.push(self.load_typed(name, self.local_cts[i.arg], frozenset([name]), False))

    def op_ldloca(self, i, b):
        name = f'l{i.arg}'
        ct = self.local_cts[i.arg]
        if ct[0] == 'R':
            self.err('ldloca of byref local')
        k = 'struct' if self.kind(ct) == 'struct' else 'var'
        self.push(X.Val(name, self.jt(ct), ('R', ct), frozenset([name]), lv=X.LV(k, name, ct, self.jt(ct))))

    def op_stloc(self, i, b):
        v = self.pop()
        name = f'l{i.arg}'
        ct = self.local_cts[i.arg]
        if ct[0] == 'R':
            # byref local (C# ref locals): keep the pointer symbolically
            if v.lv is None:
                self.err('stloc byref from non-pointer')
            if not hasattr(self, 'byref_locals'):
                self.byref_locals = {}
            prev = self.byref_locals.get(i.arg)
            if prev is not None and prev.lv.e != v.lv.e:
                self.err('byref local re-pointed')
            self.byref_locals[i.arg] = v
            return
        self.spill_var(name)
        self.store(name, ct, v)

    def store(self, target, ct, v, heap_target=False):
        k = self.kind(ct)
        if k == 'struct':
            if v.jt == 'null':
                self.emit(f'{target}.$clear();')
            else:
                self.emit(f'{target}.$set({v.e});')
            return
        j = self.jt(ct)
        if ct[0] == 'P' and ct[1] in ('u1', 'i1') and v.jt != 'byte':
            self.emit(f'{target} = (byte) ({self.conv(v, "int")});')
            return
        if v.ftn is not None or v.token is not None:
            self.err('store of function pointer/token')
        if v.lv is not None and ct[0] != 'R':
            self.err('store of pointer into non-byref')
        self.emit(f'{target} = {self.conv(v, j, ct)};')

    # --- constants
    def op_ldnull(self, i, b):
        self.push(X.Val('null', 'null', None, const=None))

    def op_ldc_i4(self, i, b):
        self.push(X.Val(jint(i.arg), 'int', ('P', 'i4'), const=i.arg))

    def op_ldc_i8(self, i, b):
        self.push(X.Val(jlong(i.arg), 'long', ('P', 'i8'), const=i.arg))

    def op_ldc_r4(self, i, b):
        self.push(X.Val(jfloat(i.arg), 'float', ('P', 'r4'), const=i.arg[1]))

    def op_ldc_r8(self, i, b):
        self.push(X.Val(jdouble(i.arg), 'double', ('P', 'r8'), const=i.arg[1]))

    def op_ldstr(self, i, b):
        s = self.mod.user_string(i.arg & 0xffffff)
        self.push(X.Val(jstr_literal(s), 'String', ('P', 'string'), const=s))

    def op_dup(self, i, b):
        v = self.pop()
        if v.lv is None and v.ftn is None and v.token is None and v.const is None and v.jt != 'null' and not self.simple(v.e):
            t = self.temp(v.jt)
            self.emit(f'{t} = {v.e};')
            v = X.Val(t, v.jt, v.ct, frozenset([t]), False, v.fresh)
        self.push(v)
        self.push(v)

    def op_pop(self, i, b):
        self.pop()

    def op_nop(self, i, b):
        pass

    op_break = op_nop

    def op_ckfinite(self, i, b):
        pass

    # --- arithmetic
    def num_type(self, a, c):
        ta, tc = a.jt, c.jt
        for t in ('double', 'float', 'long'):
            if ta == t or tc == t:
                return t
        return 'int'

    def binop(self, i, sym):
        c = self.pop()
        a = self.pop()
        if sym in ('&', '|', '^') and a.jt == 'boolean' and c.jt == 'boolean':
            self.push(X.Val(f'({a.e} {sym} {c.e})', 'boolean', ('P', 'bool'), a.deps | c.deps, a.heap or c.heap))
            return
        t = self.num_type(a, c)
        ea, ec = self.conv(a, t), self.conv(c, t)
        if sym in ('<<', '>>', '>>>'):
            t = a.jt if a.jt in ('long',) else 'int'
            ea = self.conv(a, t)
            ec = self.conv(c, 'int')
        e = f'({ea} {sym} {ec})'
        self.push(X.Val(e, t, None, a.deps | c.deps, a.heap or c.heap))

    def op_add(self, i, b): self.binop(i, '+')
    def op_sub(self, i, b): self.binop(i, '-')
    def op_mul(self, i, b): self.binop(i, '*')
    def op_div(self, i, b): self.binop(i, '/')
    def op_rem(self, i, b): self.binop(i, '%')
    def op_and(self, i, b): self.binop(i, '&')
    def op_or(self, i, b): self.binop(i, '|')
    def op_xor(self, i, b): self.binop(i, '^')
    def op_shl(self, i, b): self.binop(i, '<<')
    def op_shr(self, i, b): self.binop(i, '>>')
    def op_shr_un(self, i, b): self.binop(i, '>>>')
    op_add_ovf = op_add_ovf_un = op_add
    op_sub_ovf = op_sub_ovf_un = op_sub
    op_mul_ovf = op_mul_ovf_un = op_mul

    def unsigned_op(self, fn):
        c = self.pop()
        a = self.pop()
        t = self.num_type(a, c)
        if t in ('float', 'double'):
            sym = '/' if fn == 'divideUnsigned' else '%'
            self.push(X.Val(f'({self.conv(a, t)} {sym} {self.conv(c, t)})', t, None, a.deps | c.deps, a.heap or c.heap))
            return
        fnj = {'divideUnsigned': 'divu', 'remainderUnsigned': 'remu'}[fn] + ('L' if t == 'long' else '')
        self.push(X.Val(f'cs.rt.Rt.{fnj}({self.conv(a, t)}, {self.conv(c, t)})', t, None, a.deps | c.deps, a.heap or c.heap))

    def op_div_un(self, i, b): self.unsigned_op('divideUnsigned')
    def op_rem_un(self, i, b): self.unsigned_op('remainderUnsigned')

    def op_neg(self, i, b):
        a = self.pop()
        t = a.jt if a.jt in ('long', 'float', 'double') else 'int'
        self.push(X.Val(f'(-{self.conv(a, t)})', t, None, a.deps, a.heap))

    def op_not(self, i, b):
        a = self.pop()
        t = 'long' if a.jt == 'long' else 'int'
        self.push(X.Val(f'(~{self.conv(a, t)})', t, None, a.deps, a.heap))

    # --- conversions
    def to_int(self, a):
        if a.jt in ('int', 'char', 'short', 'byte'):
            return a.e
        if a.jt == 'boolean':
            return f'({a.e} ? 1 : 0)'
        return f'((int) {a.e})' if self.simple(a.e) else f'((int) ({a.e}))'

    def conv_op(self, i, b, target):
        a = self.pop()
        e = None
        if target == 'i1':
            e, t = f'((byte) {self.to_int(a)})', 'byte'
        elif target == 'u1':
            e, t = f'({self.to_int(a)} & 0xff)', 'int'
        elif target == 'i2':
            e, t = f'((short) {self.to_int(a)})', 'short'
        elif target == 'u2':
            e, t = f'((char) {self.to_int(a)})', 'char'
        elif target in ('i4', 'u4'):
            if target == 'u4' and a.jt in ('float', 'double'):
                e, t = f'((int) (long) ({a.e}))', 'int'
            else:
                e, t = self.to_int(a), 'int'
        elif target in ('i8', 'i'):
            e, t = (f'((long) {a.e})' if a.jt != 'boolean' else f'({a.e} ? 1L : 0L)'), 'long'
        elif target in ('u8', 'u'):
            if a.jt in ('int', 'char', 'short', 'byte', 'boolean'):
                e, t = f'((long) {self.to_int(a)} & 0xffffffffL)', 'long'
            else:
                e, t = f'((long) {a.e})', 'long'
        elif target == 'r4':
            e, t = (f'((float) {self.to_int(a) if a.jt == "boolean" else a.e})'), 'float'
        elif target == 'r8':
            e, t = (f'((double) {self.to_int(a) if a.jt == "boolean" else a.e})'), 'double'
        elif target == 'r.un':
            if a.jt == 'long':
                e, t = f'cs.rt.Rt.ulongToDouble({a.e})', 'double'
            else:
                e, t = f'((double) ((long) {self.to_int(a)} & 0xffffffffL))', 'double'
        cts = {'i1': 'i1', 'u1': 'u1', 'i2': 'i2', 'u2': 'u2', 'i4': 'i4', 'u4': 'u4', 'i8': 'i8', 'u8': 'u8',
               'r4': 'r4', 'r8': 'r8', 'i': 'i', 'u': 'u', 'r.un': 'r8'}
        self.push(X.Val(e, t, ('P', cts[target]), a.deps, a.heap))

    def __getattr__(self, name):
        if name.startswith('op_conv_'):
            rest = name[8:]
            rest = rest.replace('ovf_', '').replace('_un', '')
            if rest == 'r':
                rest = 'r.un'
            return lambda i, b, _t=rest: self.conv_op(i, b, _t)
        raise AttributeError(name)

    # --- comparisons
    def cmp_expr(self, a, c, op, unsigned=False):
        """Java boolean expression for a OP c."""
        if a.jt not in JPRIMS or c.jt not in JPRIMS:
            # references
            if op == '==':
                return f'({a.e} == {self.conv(c, "Object") if c.jt in JPRIMS else c.e})' if a.jt != 'null' else f'({c.e} == null)'
            if op == '!=':
                return f'({a.e} != {c.e})'
            if op == '>' and unsigned:          # cgt.un x, null
                return f'({a.e} != {c.e})'
            self.err(f'reference compare {op}')
        if a.jt == 'boolean' and c.jt == 'boolean' and op in ('==', '!='):
            return f'({a.e} {op} {c.e})'
        t = self.num_type(a, c)
        ea, ec = self.conv(a, t), self.conv(c, t)
        if unsigned and t in ('int', 'long') and op not in ('==', '!='):
            return f'(cs.rt.Rt.cmpu{"L" if t == "long" else ""}({ea}, {ec}) {op} 0)'
        if unsigned and t in ('float', 'double') and op not in ('==', '!='):
            inv = {'<': '>=', '<=': '>', '>': '<=', '>=': '<'}[op]
            return f'(!({ea} {inv} {ec}))'
        return f'({ea} {op} {ec})'

    def cmp_push(self, op, unsigned=False):
        c = self.pop()
        a = self.pop()
        self.push(X.Val(self.cmp_expr(a, c, op, unsigned), 'boolean', ('P', 'bool'), a.deps | c.deps, a.heap or c.heap))

    def op_ceq(self, i, b): self.cmp_push('==')
    def op_cgt(self, i, b): self.cmp_push('>')
    def op_cgt_un(self, i, b): self.cmp_push('>', True)
    def op_clt(self, i, b): self.cmp_push('<')
    def op_clt_un(self, i, b): self.cmp_push('<', True)

    # --- branches
    def cond_jump(self, b, cond, target):
        # the remaining stack flows into both successors
        nxt = b.ins[-1].next
        self.flow_to(target, b)
        if nxt in self.bmap:
            self.flow_to(nxt, b)
        self.assign_merge(target)
        j = self.jump(b, target)
        self.emit(f'if ({cond}) {{ ' + ' '.join(j) + ' }')
        if nxt in self.bmap:
            self.bmap[nxt].reached = True
        self.bmap[target].reached = True

    def truth(self, v):
        if v.jt == 'boolean':
            return v.e
        if v.jt in ('int', 'char', 'short', 'byte'):
            return f'({v.e} != 0)'
        if v.jt == 'long':
            return f'({v.e} != 0L)'
        if v.jt in ('float', 'double'):
            return f'({v.e} != 0)'
        return f'({v.e} != null)'

    def op_brtrue(self, i, b):
        v = self.pop()
        self.cond_jump(b, self.truth(v), i.arg)

    def op_brfalse(self, i, b):
        v = self.pop()
        t = self.truth(v)
        self.cond_jump(b, f'!{t}' if t.startswith('(') else f'!({t})', i.arg)

    def bcmp(self, i, b, op, unsigned=False):
        c = self.pop()
        a = self.pop()
        if op == '!=' and unsigned:
            unsigned = False      # bne.un: plain inequality (true for NaN)
        self.cond_jump(b, self.cmp_expr(a, c, op, unsigned), i.arg)

    def op_beq(self, i, b): self.bcmp(i, b, '==')
    def op_bne_un(self, i, b): self.bcmp(i, b, '!=', True)
    def op_bge(self, i, b): self.bcmp(i, b, '>=')
    def op_bgt(self, i, b): self.bcmp(i, b, '>')
    def op_ble(self, i, b): self.bcmp(i, b, '<=')
    def op_blt(self, i, b): self.bcmp(i, b, '<')
    def op_bge_un(self, i, b): self.bcmp(i, b, '>=', True)
    def op_bgt_un(self, i, b): self.bcmp(i, b, '>', True)
    def op_ble_un(self, i, b): self.bcmp(i, b, '<=', True)
    def op_blt_un(self, i, b): self.bcmp(i, b, '<', True)

    def op_br(self, i, b):
        self.flow_to(i.arg, b)
        self.assign_merge(i.arg)
        self.bmap[i.arg].reached = True
        self.out.extend(self.jump(b, i.arg))

    def op_leave(self, i, b):
        self.stack = []
        self.bmap[i.arg].reached = True
        self.out.extend(self.jump(b, i.arg))

    def op_endfinally(self, i, b):
        r = b.region
        r.needs_loop = True
        self.emit(f'break {r.label};')

    def op_switch(self, i, b):
        v = self.pop()
        self.flow_to(i.next, b)
        for t in i.arg:
            self.flow_to(t, b)
        self.assign_merge(i.next)
        self.emit(f'switch ({self.conv(v, "int")}) {{')
        for k, t in enumerate(i.arg):
            self.bmap[t].reached = True
            self.emit(f'    case {k}: ' + ' '.join(self.jump(b, t)))
        self.emit('}')
        if i.next in self.bmap:
            self.bmap[i.next].reached = True

    def op_ret(self, i, b):
        r = self.m.sig.ret
        if self.m.name == '.ctor' or r == ('P', 'void'):
            self.emit('return;')
            return
        v = self.pop()
        j = self.jt(r)
        if self.kind(r) == 'struct':
            e = v.e
            if not v.fresh and not (v.e.startswith('l') and v.e[1:].isdigit()):
                e = f'{v.e}.$copy()' if v.jt != 'Object' else f'(({j}) cs.rt.Rt.copyStruct({v.e}))'
            elif v.jt != j:
                e = f'(({j}) {v.e})'
            self.emit(f'return {e};')
            return
        if self.kind(r) in ('gp',) and v.jt in JPRIMS:
            self.emit(f'return {self.boxed(v, v.ct)};')
            return
        self.emit(f'return {self.conv(v, j, r)};')

    def op_throw(self, i, b):
        v = self.pop()
        self.emit(f'throw cs.rt.Rt.toThrowable({v.e});')

    def op_rethrow(self, i, b):
        r = b.region
        while r is not None and not (r.kind == 'handler' and r.clause.kind == 'catch'):
            r = r.parent.region if r.parent is not None else None
        if r is None:
            self.err('rethrow outside catch')
        self.emit(f'throw cs.rt.Rt.toThrowable({r.exvar});')

    # --- fields
    def field_info(self, tok):
        f = self.mod.member_token(tok)
        if isinstance(f, FieldDef):
            decl = ('N', f.owner.fullname, f.owner.is_value) if not f.owner.gparams else \
                ('G', ('N', f.owner.fullname, f.owner.is_value), tuple(('V', k) for k in range(len(f.owner.gparams))))
            return decl, f.type, f.name, f, f.static
        if isinstance(f, MemberRef):
            fd = f.resolved if isinstance(f.resolved, FieldDef) else None
            static = fd.static if fd is not None else None
            return f.parent, f.sig, f.name, fd, static
        self.err('bad field token')

    def field_access(self, decl, ftype, name, fd, obj=None, st=False):
        """(java expression prefix, java field name, actual .NET field type, declared java type)"""
        targs = decl[2] if decl[0] == 'G' else None
        actual = subst(ftype, targs, None)
        if fd is not None and fd.owner.fullname in self.ts.game:
            cls = jclass_name(fd.owner.fullname)
            fname = self.mg.field(fd)
        else:
            dname = self.ts.name_of(decl)
            from .calls import MAPPED
            cls = MAPPED.get(dname) or (jclass_name(dname) if dname else 'Object')
            fname = sanitize(name)
            self.x.ext_member(cls, 'field', fname, [], self.ext_jt(ftype), static=st)
        return cls, fname, actual, self.jt(ftype)

    def obj_expr(self, v, cls):
        if v.lv is not None:
            if v.lv.kind == 'struct':
                return v.lv.e
            return v.lv.read()
        if self.assignable(v.jt, cls):
            return v.e
        return f'(({cls}) {v.e})' if v.jt == 'Object' and self.simple(v.e) else f'(({cls}) (Object) ({v.e}))'

    def op_ldfld(self, i, b):
        o = self.pop()
        decl, ftype, name, fd, static = self.field_info(i.arg)
        cls, fname, actual, djt = self.field_access(decl, ftype, name, fd)
        if static:
            e = f'{cls}.{fname}'
        else:
            e = f'{self.obj_expr(o, cls)}.{fname}'
        self.push_field_value(e, ftype, actual, djt, o.deps)

    def push_field_value(self, e, ftype, actual, djt, deps):
        if ftype[0] in 'VW' or (ftype[0] == 'A' and ftype[1][0] in 'VW' and actual[0] == 'A' and actual[1][0] not in 'VW'):
            if ftype[0] in 'VW':
                self.push(X.Val(self.from_object(e, actual), self.jt(actual), actual, deps, True))
                return
        self.push(self.load_typed(e, actual if actual[0] not in 'VW' else ftype, deps, True))

    def op_ldsfld(self, i, b):
        decl, ftype, name, fd, static = self.field_info(i.arg)
        cls, fname, actual, djt = self.field_access(decl, ftype, name, fd, st=True)
        self.push_field_value(f'{cls}.{fname}', ftype, actual, djt, frozenset())

    def op_ldflda(self, i, b):
        o = self.pop()
        decl, ftype, name, fd, static = self.field_info(i.arg)
        cls, fname, actual, djt = self.field_access(decl, ftype, name, fd)
        if static:
            base = cls
        else:
            oe = self.obj_expr(o, cls)
            if not self.simple(oe):
                t = self.temp(cls)
                self.emit(f'{t} = {oe};')
                oe = t
            base = oe
        e = f'{base}.{fname}'
        if self.kind(actual) == 'struct':
            self.push(X.Val(e, self.jt(actual), ('R', actual), frozenset(), True, lv=X.LV('struct', e, actual, self.jt(actual))))
        else:
            self.push(X.Val(e, self.jt(actual), ('R', actual), frozenset(), True, lv=X.LV('var', e, actual, self.jt(ftype))))

    def op_ldsflda(self, i, b):
        decl, ftype, name, fd, static = self.field_info(i.arg)
        cls, fname, actual, djt = self.field_access(decl, ftype, name, fd, st=True)
        e = f'{cls}.{fname}'
        k = 'struct' if self.kind(actual) == 'struct' else 'var'
        self.push(X.Val(e, self.jt(actual), ('R', actual), frozenset(), True, lv=X.LV(k, e, actual, self.jt(ftype))))

    def op_stfld(self, i, b):
        v = self.pop()
        o = self.pop()
        decl, ftype, name, fd, static = self.field_info(i.arg)
        cls, fname, actual, djt = self.field_access(decl, ftype, name, fd)
        self.spill_heap()
        target = f'{cls}.{fname}' if static else f'{self.obj_expr(o, cls)}.{fname}'
        self.store_field(target, ftype, actual, v)

    def store_field(self, target, ftype, actual, v):
        if ftype[0] in 'VW':
            self.emit(f'{target} = {self.boxed(v, actual)};')
            return
        self.store(target, actual if actual[0] not in 'VW' else ftype, v)

    def op_stsfld(self, i, b):
        v = self.pop()
        decl, ftype, name, fd, static = self.field_info(i.arg)
        cls, fname, actual, djt = self.field_access(decl, ftype, name, fd, st=True)
        self.spill_heap()
        self.store_field(f'{cls}.{fname}', ftype, actual, v)

    # --- pointers
    def type_tok(self, tok):
        return self.mod.type_token(tok)

    def gsubst(self, ct):
        """substitute nothing (types in method bodies may mention the method's own generic params)"""
        return ct

    def lv_of(self, v):
        if v.lv is None:
            # a pointer stored in a value (e.g. byref param loaded) - try to treat value as struct object
            if v.ct is not None and v.ct[0] == 'R':
                el = v.ct[1]
                if self.kind(el) == 'struct':
                    return X.LV('struct', v.e, el, self.jt(el))
                return X.LV('cell', v.e, el, self.jt(el))
            self.err(f'expected pointer, got {v.e}')
        return v.lv

    def read_lv(self, lv, ct=None):
        ct = ct or lv.ct
        e = lv.read()
        if lv.kind == 'struct':
            return X.Val(e, self.jt(lv.ct), lv.ct, frozenset(), True)
        if lv.kind == 'cell' and lv.e.startswith('a'):
            pass
        j = lv.jt if lv.kind != 'cell' else ('Object' if self.jt(ct) not in JPRIMS and REF_CELL.get(self.jt(ct)) is None else self.jt(ct))
        if lv.kind == 'cell' and self.jt(ct) not in JPRIMS:
            # RefO: value is Object
            return X.Val(self.from_object(e, ct) if self.jt(ct) != 'Object' else e, self.jt(ct), ct, frozenset(), True)
        if lv.kind == 'var' and lv.jt == 'Object' and self.jt(ct) != 'Object':
            return X.Val(self.from_object(e, ct), self.jt(ct), ct, frozenset(), True)
        if ct[0] == 'P' and ct[1] == 'u1':
            return X.Val(f'({e} & 0xff)', 'int', ct, frozenset(), True)
        return X.Val(e, self.jt(ct), ct, frozenset(), True)

    def write_lv(self, lv, v):
        self.spill_heap()
        if lv.kind == 'struct':
            self.emit(f'{lv.e}.$set({v.e});' if v.jt != 'null' else f'{lv.e}.$clear();')
            return
        target = lv.read()
        ct = lv.ct
        if lv.kind == 'cell' and self.jt(ct) not in JPRIMS:
            self.emit(f'{target} = {self.boxed(v, ct) if v.jt in JPRIMS else v.e};')
            return
        if lv.kind == 'var' and lv.jt == 'Object':
            self.emit(f'{target} = {self.boxed(v, ct)};')
            return
        self.store(target, ct, v)

    def op_ldobj(self, i, b):
        p = self.pop()
        ct = self.type_tok(i.arg)
        lv = self.lv_of(p)
        self.push(self.read_lv(lv, ct))

    def op_stobj(self, i, b):
        v = self.pop()
        p = self.pop()
        self.write_lv(self.lv_of(p), v)

    def op_initobj(self, i, b):
        p = self.pop()
        ct = self.type_tok(i.arg)
        lv = self.lv_of(p)
        self.spill_heap()
        if lv.kind == 'struct':
            self.emit(f'{lv.e}.$clear();')
            return
        j = self.jt(ct)
        if ct[0] == 'W':
            self.emit(f'{lv.read()} = cs.rt.Rt.defaultOf($T{ct[1]});')
            return
        self.emit(f'{lv.read()} = {X.Xlat.default_of(j) if not (lv.kind == "cell" and j not in JPRIMS) else "null"};')

    def ldind(self, i, b, ct):
        p = self.pop()
        lv = self.lv_of(p)
        self.push(self.read_lv(lv, lv.ct if lv.ct is not None else ct))

    def stind(self, i, b, ct):
        v = self.pop()
        p = self.pop()
        self.write_lv(self.lv_of(p), v)

    def op_ldind_i1(self, i, b): self.ldind(i, b, ('P', 'i1'))
    def op_ldind_u1(self, i, b): self.ldind(i, b, ('P', 'u1'))
    def op_ldind_i2(self, i, b): self.ldind(i, b, ('P', 'i2'))
    def op_ldind_u2(self, i, b): self.ldind(i, b, ('P', 'u2'))
    def op_ldind_i4(self, i, b): self.ldind(i, b, ('P', 'i4'))
    def op_ldind_u4(self, i, b): self.ldind(i, b, ('P', 'u4'))
    def op_ldind_i8(self, i, b): self.ldind(i, b, ('P', 'i8'))
    def op_ldind_i(self, i, b): self.ldind(i, b, ('P', 'i'))
    def op_ldind_r4(self, i, b): self.ldind(i, b, ('P', 'r4'))
    def op_ldind_r8(self, i, b): self.ldind(i, b, ('P', 'r8'))

    def op_ldind_ref(self, i, b):
        p = self.pop()
        lv = self.lv_of(p)
        self.push(self.read_lv(lv, lv.ct))

    def op_stind_ref(self, i, b): self.stind(i, b, None)
    op_stind_i1 = op_stind_i2 = op_stind_i4 = op_stind_i8 = op_stind_r4 = op_stind_r8 = op_stind_i = op_stind_ref

    # --- arrays
    def op_newarr(self, i, b):
        n = self.pop()
        ct = self.type_tok(i.arg)
        j = self.jt(ct)
        t = self.temp(j + '[]')
        ne = self.conv(n, 'int')
        if ct[0] in 'VW':
            self.temps[t] = 'Object'
            te = self.x.type_expr(ct, self)
            self.emit(f'{t} = java.lang.reflect.Array.newInstance({te}.arrayElementClass(), {ne});')
            self.push(X.Val(t, 'Object', ('A', ct), frozenset([t]), False, True))
            return
        if self.kind(ct) == 'struct':
            self.emit(f'{t} = {j}.$newArray({ne});')
        else:
            base = j
            dims = ''
            while base.endswith('[]'):
                base = base[:-2]
                dims += '[]'
            self.emit(f'{t} = new {base}[{ne}]{dims};')
        self.push(X.Val(t, j + '[]', ('A', ct), frozenset([t]), False, True))

    def arr_expr(self, a):
        if a.jt.endswith('[]'):
            return a.e, a.jt[:-2]
        return None, None

    def op_ldlen(self, i, b):
        a = self.pop()
        if a.jt.endswith('[]'):
            self.push(X.Val(f'{a.e}.length', 'int', ('P', 'i4'), a.deps, a.heap))
        else:
            self.push(X.Val(f'java.lang.reflect.Array.getLength({a.e})', 'int', ('P', 'i4'), a.deps, a.heap))

    def elem_ct(self, a):
        if a.ct is not None and a.ct[0] == 'A':
            return a.ct[1]
        return None

    def ldelem(self, i, b, ct):
        idx = self.pop()
        a = self.pop()
        ae = a.e
        ej = None
        deps = a.deps | idx.deps
        ect = self.elem_ct(a) or ct
        if ect is None:
            ect = ('P', 'object')
        if not a.jt.endswith('[]'):
            # array of a generic parameter (java Object): reflective access
            e = f'java.lang.reflect.Array.get({a.e}, {self.conv(idx, "int")})'
            if ect[0] in 'VW' or self.jt(ect) == 'Object':
                self.push(X.Val(e, 'Object', ect, deps, True))
            else:
                self.push(X.Val(self.from_object(e, ect), self.jt(ect), ect, deps, True))
            return
        ej = a.jt[:-2]
        e = f'{ae}[{self.conv(idx, "int")}]'
        if ect[0] == 'P' and ect[1] == 'u1' and ej == 'byte':
            self.push(X.Val(f'({e} & 0xff)', 'int', ('P', 'u1'), deps, True))
            return
        if ct is not None and ct[0] == 'P' and ct[1] == 'u4' and ej == 'int':
            pass
        if ej == 'Object' and ect[0] not in 'VW' and self.jt(ect) != 'Object':
            self.push(X.Val(self.from_object(e, ect), self.jt(ect), ect, deps, True))
            return
        self.push(X.Val(e, ej, ect, deps, True))

    def op_ldelem_i1(self, i, b): self.ldelem(i, b, ('P', 'i1'))
    def op_ldelem_u1(self, i, b): self.ldelem(i, b, ('P', 'u1'))
    def op_ldelem_i2(self, i, b): self.ldelem(i, b, ('P', 'i2'))
    def op_ldelem_u2(self, i, b): self.ldelem(i, b, ('P', 'u2'))
    def op_ldelem_i4(self, i, b): self.ldelem(i, b, ('P', 'i4'))
    def op_ldelem_u4(self, i, b): self.ldelem(i, b, ('P', 'u4'))
    def op_ldelem_i8(self, i, b): self.ldelem(i, b, ('P', 'i8'))
    def op_ldelem_i(self, i, b): self.ldelem(i, b, ('P', 'i'))
    def op_ldelem_r4(self, i, b): self.ldelem(i, b, ('P', 'r4'))
    def op_ldelem_r8(self, i, b): self.ldelem(i, b, ('P', 'r8'))
    def op_ldelem_ref(self, i, b): self.ldelem(i, b, None)

    def op_ldelem(self, i, b):
        self.ldelem(i, b, self.type_tok(i.arg))

    def stelem(self, i, b, ct):
        v = self.pop()
        idx = self.pop()
        a = self.pop()
        self.spill_heap()
        ect = self.elem_ct(a) or ct or ('P', 'object')
        if not a.jt.endswith('[]'):
            self.emit(f'cs.rt.Rt.arraySet({a.e}, {self.conv(idx, "int")}, {self.boxed(v, v.ct if v.ct is not None else ect) if v.jt in JPRIMS else v.e});')
            return
        ej = a.jt[:-2]
        ae = a.e
        target = f'{ae}[{self.conv(idx, "int")}]'
        if self.kind(ect) == 'struct' and ej != 'Object':
            self.emit(f'{target}.$set({v.e});')
            return
        if ej == 'Object':
            self.emit(f'{target} = {self.boxed(v, v.ct if v.ct is not None else ect)};')
            return
        if ej == 'byte':
            self.emit(f'{target} = (byte) ({self.conv(v, "int")});')
            return
        self.emit(f'{target} = {self.conv(v, ej)};')

    def op_stelem_i1(self, i, b): self.stelem(i, b, ('P', 'i1'))
    def op_stelem_i2(self, i, b): self.stelem(i, b, ('P', 'i2'))
    def op_stelem_i4(self, i, b): self.stelem(i, b, ('P', 'i4'))
    def op_stelem_i8(self, i, b): self.stelem(i, b, ('P', 'i8'))
    def op_stelem_i(self, i, b): self.stelem(i, b, ('P', 'i'))
    def op_stelem_r4(self, i, b): self.stelem(i, b, ('P', 'r4'))
    def op_stelem_r8(self, i, b): self.stelem(i, b, ('P', 'r8'))
    def op_stelem_ref(self, i, b): self.stelem(i, b, None)

    def op_stelem(self, i, b):
        self.stelem(i, b, self.type_tok(i.arg))

    def op_ldelema(self, i, b):
        idx = self.pop()
        a = self.pop()
        ct = self.type_tok(i.arg)
        ae = a.e
        if not a.jt.endswith('[]'):
            # element of an array typed as a generic parameter: read-only temporary
            t = self.temp('Object')
            self.emit(f'{t} = java.lang.reflect.Array.get({a.e}, {self.conv(idx, "int")});')
            self.push(X.Val(t, 'Object', ('R', ct), frozenset([t]), lv=X.LV('var', t, ct, 'Object')))
            return
        if not self.simple(ae):
            t = self.temp(a.jt)
            self.emit(f'{t} = {ae};')
            ae = t
        ie = self.conv(idx, 'int')
        if not self.simple(ie):
            t = self.temp('int')
            self.emit(f'{t} = {ie};')
            ie = t
        e = f'{ae}[{ie}]'
        if self.kind(ct) == 'struct':
            self.push(X.Val(e, self.jt(ct), ('R', ct), frozenset(), True, lv=X.LV('struct', e, ct, self.jt(ct))))
        else:
            ej = a.jt[:-2] if a.jt.endswith('[]') else self.jt(ct)
            self.push(X.Val(e, ej, ('R', ct), frozenset(), True, lv=X.LV('var', e, ct, ej)))

    # --- objects
    def op_box(self, i, b):
        v = self.pop()
        ct = self.type_tok(i.arg)
        k = self.kind(ct)
        if k in ('prim', 'enum'):
            self.push(X.Val(self.boxed(v, ct), 'Object', ('P', 'object'), v.deps, v.heap))
        elif k == 'struct':
            if v.lv is not None:
                v = X.Val(v.lv.e, v.jt, ct, v.deps, v.heap)
            if v.fresh:
                e = v.e
            elif v.jt == 'Object':
                e = f'cs.rt.Rt.copyStruct({v.e})'
            else:
                e = f'{v.e}.$copy()'
            self.push(X.Val(e, self.jt(ct) if v.jt != 'Object' else 'Object', ct, v.deps, v.heap))
        else:
            self.push(v if v.jt not in JPRIMS else X.Val(self.boxed(v, v.ct), 'Object', ('P', 'object'), v.deps, v.heap))

    def op_unbox_any(self, i, b):
        v = self.pop()
        ct = self.type_tok(i.arg)
        k = self.kind(ct)
        if k in ('prim', 'enum'):
            self.push(X.Val(X.unbox_expr(v.e, self.jt(ct)), self.jt(ct), ct, v.deps, v.heap))
        elif k == 'gp':
            self.push(X.Val(v.e, 'Object', ct, v.deps, v.heap))
        else:
            j = self.jt(ct)
            self.push(X.Val(self.conv(v, j), j, ct, v.deps, v.heap))

    def op_unbox(self, i, b):
        v = self.pop()
        ct = self.type_tok(i.arg)
        if self.kind(ct) == 'struct':
            e = f'(({self.jt(ct)}) {v.e})'
            self.push(X.Val(e, self.jt(ct), ('R', ct), v.deps, True, lv=X.LV('struct', e, ct, self.jt(ct))))
        else:
            # pointer to a boxed primitive: read-only use
            t = self.temp(self.jt(ct))
            self.emit(f'{t} = {X.unbox_expr(v.e, self.jt(ct))};')
            self.push(X.Val(t, self.jt(ct), ('R', ct), frozenset([t]), lv=X.LV('var', t, ct, self.jt(ct))))

    def op_castclass(self, i, b):
        v = self.pop()
        ct = self.type_tok(i.arg)
        k = self.kind(ct)
        if k in ('gp', 'prim', 'enum') or self.jt(ct) == 'Object':
            self.push(v)
            return
        j = self.jt(ct)
        self.push(X.Val(f'(({j}) {self.conv(v, "Object") if v.jt in JPRIMS else v.e})' if self.simple(v.e) or v.jt == 'Object' else f'(({j}) (Object) ({v.e}))',
                        j, ct, v.deps, v.heap))

    def op_isinst(self, i, b):
        v = self.pop()
        ct = self.type_tok(i.arg)
        k = self.kind(ct)
        e = v.e
        if not self.simple(e):
            t = self.temp(v.jt if v.jt != 'null' else 'Object')
            self.emit(f'{t} = {e};')
            e = t
        if k == 'gp':
            self.push(X.Val(e, 'Object', ct, frozenset([e]), v.heap))
            self.ts.warn('isinst generic parameter')
            return
        if k == 'prim':
            j = BOX_J.get(PRIM_J[ct[1]], 'Object')
            if ct[1] in ('u2',):
                j = 'Character'
            self.push(X.Val(f'({e} instanceof {j} ? {e} : null)', 'Object', ('P', 'object'), frozenset([e]), v.heap))
            return
        if k == 'enum':
            self.push(X.Val(f'cs.rt.Rt.isEnum({e}, {self.x.enum_class_expr(ct)})', 'Object', ('P', 'object'), frozenset([e]), v.heap))
            return
        j = self.jt(ct)
        if j == 'Object':
            self.push(X.Val(e, 'Object', ct, frozenset([e]), v.heap))
            return
        self.push(X.Val(f'({e} instanceof {j} ? ({j}) {e} : null)', j, ct, frozenset([e]), v.heap))

    def op_ldtoken(self, i, b):
        tok = i.arg
        table = tok >> 24
        if table in (1, 2, 27):
            ct = self.type_tok(tok)
            self.push(X.Val('<token>', 'Object', None, token=('type', ct)))
        elif table in (4, 10):
            f = self.mod.member_token(tok)
            self.push(X.Val('<token>', 'Object', None, token=('field', f)))
        else:
            self.push(X.Val('<token>', 'Object', None, token=('method', self.mod.member_token(tok))))

    def op_sizeof(self, i, b):
        ct = self.type_tok(i.arg)
        sizes = {'i1': 1, 'u1': 1, 'bool': 1, 'i2': 2, 'u2': 2, 'char': 2, 'i4': 4, 'u4': 4, 'r4': 4, 'i8': 8, 'u8': 8, 'r8': 8, 'i': 8, 'u': 8}
        if ct[0] == 'P' and ct[1] in sizes:
            self.push(X.Val(str(sizes[ct[1]]), 'int', ('P', 'i4'), const=sizes[ct[1]]))
        else:
            self.err('sizeof')

    def op_ldftn(self, i, b):
        self.push(X.Val('<ftn>', 'Object', None, ftn=(self.mod.member_token(i.arg), False)))

    def op_ldvirtftn(self, i, b):
        o = self.pop()
        self.push(X.Val('<ftn>', 'Object', None, ftn=(self.mod.member_token(i.arg), True)))

    # --- calls (in calls.py)
    from .calls import op_call, op_callvirt, op_newobj, emit_call, call_info, make_delegate, arg_for_param, \
        call_special, recv_for, ret_val, type_args
