"""Method calls, object creation and delegates (mixed into MethodX)."""
from .md import MethodDef, MemberRef, FieldDef, tstr, subst, NAME_BY_PRIM
from .jtypes import *
from . import xlat as X


class CallInfo:
    __slots__ = ('md', 'decl', 'dname', 'name', 'sig', 'margs', 'targs', 'static', 'jcls', 'jname', 'jparams',
                 'jret', 'game', 'special', 'virtual_decl')


MAPPED = {  # .NET types whose methods are implemented as static helpers
    'System.String': 'cs.System.StringX', 'System.Object': 'cs.System.ObjectX', 'System.ValueType': 'cs.System.ObjectX',
    'System.Enum': 'cs.System.EnumX', 'System.Array': 'cs.System.ArrayX',
}
for _p, _x in X.PRIM_X.items():
    MAPPED[NAME_BY_PRIM[_p]] = _x


EXT_RENAME = {('System.Collections.IList', 'Add'): 'IList_Add',
              ('System.Collections.IDictionary', 'GetEnumerator'): 'IDictionary_GetEnumerator',
              ('System.Collections.IDictionary', 'get_Item'): 'IDictionary_get_Item',
              ('System.Collections.IDictionary', 'set_Item'): 'IDictionary_set_Item',
              ('System.Collections.Generic.HashSet`1', 'Add'): 'Add$bool'}


def ext_method_name(self, name, sig, dname=None):
    if (dname, name) in EXT_RENAME:
        return EXT_RENAME[(dname, name)]
    if name in ('ToString',) and sig.hasthis and not sig.params:
        return 'toString'
    if name == 'Equals' and sig.hasthis and len(sig.params) == 1 and sig.params[0] == ('P', 'object'):
        return 'equals'
    if name == 'GetHashCode' and sig.hasthis and not sig.params:
        return 'hashCode'
    if name in ('op_Implicit', 'op_Explicit'):
        return name + '$' + sanitize(jsimple(self.ext_jt(sig.ret)).replace('[]', 'Array'))
    return sanitize(name)


def call_info(self, target):
    ci = CallInfo()
    ci.special = None
    if isinstance(target, MethodDef):
        md = target
        ci.decl = ('N', md.owner.fullname, md.owner.is_value) if not md.owner.gparams else \
            ('G', ('N', md.owner.fullname, md.owner.is_value), tuple(('V', k) for k in range(len(md.owner.gparams))))
        ci.name, ci.sig, ci.margs = md.name, md.sig, None
    else:
        md = target.resolved if isinstance(target.resolved, MethodDef) else None
        ci.decl = target.parent
        ci.name, ci.sig, ci.margs = target.name, target.sig, target.margs
    ci.md = md
    ci.targs = ci.decl[2] if ci.decl is not None and ci.decl[0] == 'G' else None
    ci.dname = self.ts.name_of(ci.decl) if ci.decl is not None else None
    ci.static = not ci.sig.hasthis
    ci.game = md is not None and md.owner.fullname in self.ts.game
    ci.virtual_decl = md.virtual if md is not None else True
    if ci.game:
        ci.jcls = jclass_name(md.owner.fullname)
        ci.jname = self.mg.method(md)
        ci.jparams = self.mg.jparams(md)
        ci.jret = self.jt(md.sig.ret) if md.name != '.ctor' else 'void'
    else:
        if ci.dname in MAPPED:
            ci.special = 'mapped'
            ci.jcls = MAPPED[ci.dname]
            ci.jname = sanitize(ci.name)
            pnames = [p[1] for p in ci.sig.params if p[0] == 'N']
            if 'System.StringComparison' in pnames:
                ci.jname += 'Cmp'
            if 'System.StringSplitOptions' in pnames:
                ci.jname += 'Opt'
        elif ci.decl is not None and ci.decl[0] in ('A', 'M'):
            ci.special = 'array'
            ci.jcls = 'cs.rt.MDArray'
            ci.jname = sanitize(ci.name)
        else:
            ci.jcls = self.jt(ci.decl) if ci.decl is not None else 'Object'
            if ci.decl is not None and self.kind(ci.decl) == 'enum':
                ci.jcls = jclass_name(ci.dname)
            ci.jname = ext_method_name(self, ci.name, ci.sig, ci.dname)
        ci.jparams = ['cs.System.Type'] * ci.sig.gen + [self.ext_jt(p) for p in ci.sig.params]
        ci.jret = self.ext_jt(ci.sig.ret) if ci.name != '.ctor' else 'void'
    return ci


def type_args(self, ci):
    if not ci.margs:
        return []
    return [self.x.type_expr(a, self) for a in ci.margs]


def actual(self, ci, t):
    return subst(t, ci.targs, ci.margs)


def arg_for_param(self, v, pd, pa, jp, pre, post):
    """Java expression passing Val v for a parameter declared pd (actual pa, java type jp)."""
    if pd[0] == 'R':
        lv = v.lv
        if lv is None:
            if v.jt == 'null':
                return f'(({jp}) null)'
            if v.ct is not None and v.ct[0] == 'R':
                lv = self.lv_of(v)
            else:
                self.err(f'byref argument is not a pointer: {v.e}')
        pte = pa[1]
        if lv.kind == 'struct' and self.kind(pte) == 'struct' and pd[1][0] not in 'VW':
            return lv.e
        if lv.kind == 'cell' and lv.e and jp == self.jt(('R', lv.ct)) and jp != 'cs.rt.RefO':
            return lv.e
        if lv.kind == 'cell' and jp == 'cs.rt.RefO' and self.jt(('R', lv.ct)) == 'cs.rt.RefO':
            return lv.e
        # copy-in / copy-out through a temporary cell
        cell = self.temp(jp)
        pre.append(f'{cell} = new {jp}();')
        cur = self.read_lv(lv, lv.ct)
        if jp == 'cs.rt.RefO':
            pre.append(f'{cell}.v = {self.boxed(cur, lv.ct) if cur.jt in JPRIMS else (cur.e + ".$copy()" if self.kind(lv.ct) == "struct" else cur.e)};')
            back = self.from_object(f'{cell}.v', lv.ct) if self.jt(lv.ct) != 'Object' else f'{cell}.v'
        else:
            vj = {v_: k_ for k_, v_ in REF_CELL.items()}.get(jp, 'Object')
            pre.append(f'{cell}.v = {self.conv(cur, vj)};')
            back = f'{cell}.v'
        if lv.kind == 'struct':
            post.append(f'{lv.e}.$set({back});')
        else:
            tgt = lv.read()
            lj = self.jt(lv.ct) if lv.kind != 'var' else lv.jt
            if lv.kind == 'cell' and self.jt(lv.ct) not in JPRIMS:
                post.append(f'{tgt} = {back};' if jp == 'cs.rt.RefO' else f'{tgt} = {X.box_expr(self.x, back, vj, lv.ct)};')
            elif lv.kind == 'var' and lv.jt == 'Object':
                post.append(f'{tgt} = {back if jp == "cs.rt.RefO" else X.box_expr(self.x, back, vj, lv.ct)};')
            else:
                bv = X.Val(back, self.jt(lv.ct) if jp == 'cs.rt.RefO' else vj, lv.ct)
                if lj == 'byte' and bv.jt != 'byte':
                    post.append(f'{tgt} = (byte) ({self.conv(bv, "int")});')
                else:
                    post.append(f'{tgt} = {self.conv(bv, lj)};')
        return cell
    if pd[0] in 'VW':
        if pa[0] in 'VW':
            return v.e if v.jt not in JPRIMS else self.boxed(v, v.ct)
        return self.boxed(v, pa) if (v.jt in JPRIMS or self.kind(pa) == 'struct') else (v.e if v.jt != 'null' else 'null')
    if jp == 'Object':
        if v.jt in JPRIMS:
            return self.boxed(v, v.ct if v.ct is not None else pa)
        if v.lv is not None:
            return v.lv.read() if v.lv.kind != 'struct' else v.lv.e
        return v.e if v.jt != 'null' else '(Object) null'
    if v.jt.endswith('[]') and not jp.endswith('[]') and jp not in JPRIMS:
        return f'(({jp}) cs.rt.Rt.iface({v.e}))'
    if v.ftn is not None or v.token is not None:
        self.err('function pointer / token as argument')
    if v.lv is not None and v.lv.kind == 'struct':
        return v.lv.e
    return self.conv(v, jp, pa)


def recv_for(self, thisv, ci, constrained):
    if thisv.lv is not None:
        lv = thisv.lv
        if lv.kind == 'struct':
            return lv.e, self.jt(lv.ct)
        val = self.read_lv(lv, lv.ct)
        return val.e, val.jt
    return thisv.e, thisv.jt


def ret_val(self, ci, expr):
    rd = ci.sig.ret if ci.name != '.ctor' else ('P', 'void')
    if ci.jret == 'void':
        self.emit(expr + ';')
        return None
    ra = actual(self, ci, rd)
    if ra[0] in 'VW':
        ja = 'Object'
    else:
        ja = self.jt(ra)
    t = self.temp(ja)
    if rd[0] in 'VW' and ra[0] not in 'VW':
        e = self.from_object(expr, ra, ja)
    elif rd[0] == 'A' and rd[1][0] in 'VW' and ra[0] == 'A' and ra[1][0] not in 'VW':
        e = f'(({ja}) cs.rt.Rt.castArray({expr}, {self.x.type_expr(ra[1], self)}))'
    elif ja != ci.jret and ja not in JPRIMS and ci.jret not in JPRIMS and not self.assignable(ci.jret, ja):
        e = f'(({ja}) {expr})'
    elif ja in JPRIMS and ci.jret in JPRIMS and ja != ci.jret:
        e = X.prim_conv(expr, ci.jret, ja)
    else:
        e = expr
    self.emit(f'{t} = {e};')
    fresh = self.kind(ra) == 'struct'
    if ra[0] == 'P' and ra[1] == 'u1':
        return X.Val(f'({t} & 0xff)', 'int', ra, frozenset([t]))
    return X.Val(t, ja, ra, frozenset([t]), False, fresh)


def call_special(self, ci, args, thisv, virtual, constrained, i):
    name, dname = ci.name, ci.dname
    if dname == 'System.Type' and name == 'GetTypeFromHandle' and args and args[0].token is not None:
        tk = args[0].token
        self.push(X.Val(self.x.type_expr(tk[1], self), 'cs.System.Type', ('N', 'System.Type', False)))
        return True
    if dname == 'System.Runtime.CompilerServices.RuntimeHelpers' and name == 'InitializeArray':
        arr, tok = args
        fd = tok.token[1]
        if isinstance(fd, MemberRef):
            fd = fd.resolved
        self.spill_heap()
        self.emit(f'cs.rt.Rt.initArray({arr.e}, {jclass_name(fd.owner.fullname)}.{self.mg.field(fd)}$data);')
        return True
    if dname == 'System.Reflection.MethodBase' and name == 'GetMethodFromHandle':
        self.err('GetMethodFromHandle')
    if dname == 'System.Reflection.FieldInfo' and name == 'GetFieldFromHandle':
        self.err('GetFieldFromHandle')
    if any(a.token is not None for a in args):
        tk = [a for a in args if a.token is not None][0].token
        if tk[0] == 'type':
            # a RuntimeTypeHandle passed elsewhere: pass the Type
            for a in args:
                if a.token is not None:
                    a.e = self.x.type_expr(a.token[1], self)
                    a.jt = 'cs.System.Type'
                    a.token = None
        else:
            self.err(f'token argument to {dname}::{name}')
    return False


def emit_call(self, i, ci, virtual, constrained=None):
    n = len(ci.sig.params)
    args = self.popn(n)
    thisv = self.pop() if ci.sig.hasthis else None
    if call_special(self, ci, args, thisv, virtual, constrained, i):
        return
    pre, post = [], []
    targs = type_args(self, ci)
    jargs = list(targs)
    params_decl = ci.sig.params
    jps = ci.jparams[ci.sig.gen:]
    for k, v in enumerate(args):
        pd = params_decl[k]
        pa = actual(self, ci, pd)
        jargs.append(arg_for_param(self, v, pd, pa, jps[k], pre, post))
    self.spill_heap()
    for s in pre:
        self.emit(s)
    # -- receiver and call expression
    if ci.name == '.ctor' and ci.dname in ('System.Object', 'System.ValueType') and not ci.static:
        for s_ in post:
            self.emit(s_)
        return
    if ci.special == 'mapped':
        if thisv is not None:
            if thisv.lv is not None:
                if thisv.lv.kind == 'struct':
                    recv = thisv.lv.e
                else:
                    rv = self.read_lv(thisv.lv, thisv.lv.ct)
                    recv = rv.e
                    if constrained is not None and self.kind(constrained) == 'enum':
                        recv = f'cs.rt.BoxedEnum.of({self.x.enum_class_expr(constrained)}, {X.prim_conv(rv.e, rv.jt, "long")})'
                    elif ci.dname in ('System.Object', 'System.ValueType', 'System.Enum') and rv.jt in JPRIMS:
                        recv = self.boxed(rv, constrained or rv.ct)
            else:
                recv = thisv.e
                if ci.dname in ('System.Object', 'System.ValueType', 'System.Enum') and thisv.jt in JPRIMS:
                    recv = self.boxed(thisv, thisv.ct)
            rjt0 = {'cs.System.StringX': 'String'}.get(ci.jcls)
            if rjt0 is not None and thisv.jt != rjt0 and thisv.lv is None:
                recv = self.conv(X.Val(recv, thisv.jt, thisv.ct), rjt0)
            nm = ci.jname
            if not virtual and ci.dname in ('System.Object', 'System.ValueType') and thisv.e == 'this' and \
                    ci.name in ('ToString', 'Equals', 'GetHashCode'):
                nm = 'base' + ci.name
            jargs = jargs[:len(targs)] + [recv] + jargs[len(targs):]
            call = f'{ci.jcls}.{nm}({", ".join(jargs)})'
        else:
            call = f'{ci.jcls}.{ci.jname}({", ".join(jargs)})'
        if thisv is not None:
            rjt = {'cs.System.StringX': 'String', 'cs.System.ObjectX': 'Object', 'cs.System.EnumX': 'Object',
                   'cs.System.ArrayX': 'Object'}.get(ci.jcls)
            if rjt is None:
                pk = {v_: k_ for k_, v_ in X.PRIM_X.items()}.get(ci.jcls)
                rjt = 'int' if pk == 'u1' else PRIM_J.get(pk, 'Object')
            self.x.ext_member(ci.jcls, 'static', nm, ci.jparams[:ci.sig.gen] + [rjt] + ci.jparams[ci.sig.gen:], ci.jret)
        else:
            self.x.ext_member(ci.jcls, 'static', ci.jname, ci.jparams, ci.jret)
        v = ret_val(self, ci, call)
    elif ci.special == 'array':
        if ci.decl[0] != 'M':
            self.err(f'array method {ci.name} on {tstr(ci.decl)}')
        el = ci.decl[1]
        arr = thisv.e
        rank = ci.decl[2]
        idx = ', '.join(self.conv(a, 'int') for a in args[:rank])
        if ci.name == 'Get':
            e = f'{arr}.data[{arr}.index({idx})]'
            v = X.Val(self.from_object(e, el) if self.jt(el) != 'Object' else e, self.jt(el), el, frozenset(), True)
        elif ci.name == 'Set':
            self.emit(f'{arr}.data[{arr}.index({idx})] = {self.boxed(args[rank], el) if args[rank].jt in JPRIMS else args[rank].e};')
            v = None
        elif ci.name == 'Address':
            t = self.temp('Object')
            self.emit(f'{t} = {arr}.data[{arr}.index({idx})];')
            v = X.Val(t, 'Object', ('R', el), frozenset([t]), lv=X.LV('var', t, el, 'Object'))
        else:
            self.err(f'md array method {ci.name}')
    elif ci.static:
        call = f'{ci.jcls}.{ci.jname}({", ".join(jargs)})'
        if not ci.game:
            self.x.ext_member(ci.jcls, 'static', ci.jname, ci.jparams, ci.jret)
        v = ret_val(self, ci, call)
    else:
        if ci.name == '.ctor':
            # constructor call on an existing object: base/this chaining or in-place struct init
            if ci.game:
                recv, rj = recv_for(self, thisv, ci, constrained)
                self.emit(f'{self.obj_expr(X.Val(recv, rj), ci.jcls) if recv != "this" else "this"}.{ci.jname}({", ".join(jargs)});')
                v = None
            else:
                kind = self.kind(ci.decl)
                if kind == 'struct':
                    recv, rj = recv_for(self, thisv, ci, constrained)
                    self.emit(f'{recv}.$set(new {ci.jcls}({", ".join(jargs)}));')
                    self.x.ext_member(ci.jcls, 'ctor', None, ci.jparams, None)
                elif args:
                    self.emit(f'this.$init({", ".join(jargs)});')
                    self.x.ext_member(ci.jcls, 'inst', '$init', ci.jparams, 'void')
                v = None
        else:
            recv, rj = recv_for(self, thisv, ci, constrained)
            if constrained is not None and self.kind(constrained) in ('prim', 'enum') and thisv.lv is not None:
                # constrained call on a primitive: route to its helper class
                ck = self.kind(constrained)
                rv = self.read_lv(thisv.lv, thisv.lv.ct)
                if ck == 'enum':
                    recv = f'cs.rt.BoxedEnum.of({self.x.enum_class_expr(constrained)}, {X.prim_conv(rv.e, rv.jt, "long")})'
                    call = f'cs.System.EnumX.{sanitize(ci.name)}({", ".join([recv] + jargs)})'
                else:
                    call = f'{X.PRIM_X[constrained[1]]}.{sanitize(ci.name)}({", ".join([rv.e] + jargs)})'
                v = ret_val(self, ci, call)
            elif ci.jname in ('toString', 'equals', 'hashCode') and (rj in JPRIMS or rj == 'Object' or
                                                                     (constrained is not None and self.kind(constrained) == 'gp')):
                h = {'toString': 'ToString', 'equals': 'Equals', 'hashCode': 'GetHashCode'}[ci.jname]
                rexp = recv if rj not in JPRIMS else X.box_expr(self.x, recv, rj, thisv.ct)
                call = f'cs.System.ObjectX.{h}({", ".join([rexp] + jargs)})'
                v = ret_val(self, ci, call)
            else:
                sup = False
                if not virtual and recv == 'this' and ci.game and ci.md.virtual and ci.md.owner is not self.t:
                    sup = True
                if not virtual and recv == 'this' and not ci.game and self.kind(ci.decl) == 'class' and \
                        self.jt(ci.decl) != self.cls and ci.dname not in ('System.Object',):
                    # base call into a runtime class (e.g. base.OnEnable())
                    sup = True
                if sup:
                    call = f'super.{ci.jname}({", ".join(jargs)})'
                else:
                    if thisv.lv is not None and thisv.lv.kind == 'struct':
                        r = recv
                    else:
                        r = self.obj_expr(X.Val(recv, rj, thisv.ct), ci.jcls)
                    call = f'{r}.{ci.jname}({", ".join(jargs)})'
                if not ci.game:
                    self.x.ext_member(ci.jcls, 'inst', ci.jname, ci.jparams, ci.jret)
                v = ret_val(self, ci, call)
    for s in post:
        self.emit(s)
    if v is not None:
        self.push(v)


def op_call(self, i, b):
    ci = call_info(self, self.mod.member_token(i.arg))
    emit_call(self, i, ci, False)


def op_callvirt(self, i, b):
    ci = call_info(self, self.mod.member_token(i.arg))
    constrained = None
    if i.prefix and i.prefix[0] == 'constrained.':
        constrained = self.mod.type_token(i.prefix[1])
    emit_call(self, i, ci, True, constrained)


def op_newobj(self, i, b):
    ci = call_info(self, self.mod.member_token(i.arg))
    if ci.decl is not None and self.kind(ci.decl) == 'delegate':
        ftn = self.pop()
        target = self.pop()
        if ftn.ftn is None:
            self.err('delegate ctor without ldftn')
        self.push(make_delegate(self, ci.decl, target, ftn.ftn))
        return
    n = len(ci.sig.params)
    args = self.popn(n)
    if ci.decl is not None and ci.decl[0] == 'M':
        t = self.temp('cs.rt.MDArray')
        el = ci.decl[1]
        dims = ', '.join(self.conv(a, 'int') for a in args)
        self.emit(f'{t} = new cs.rt.MDArray({dims});')
        if self.jt(el) in JPRIMS:
            self.emit(f'{t}.fill({self.boxed(X.Val(X.Xlat.default_of(self.jt(el)), self.jt(el), el), el)});')
        self.push(X.Val(t, 'cs.rt.MDArray', ci.decl, frozenset([t]), False, True))
        return
    if ci.decl is not None and ci.decl[0] == 'A':
        self.err('newobj of array type')
    pre, post = [], []
    jargs = []
    jps = ci.jparams[ci.sig.gen:]
    for k, v in enumerate(args):
        pd = ci.sig.params[k]
        pa = actual(self, ci, pd)
        jargs.append(arg_for_param(self, v, pd, pa, jps[k], pre, post))
    self.spill_heap()
    for s in pre:
        self.emit(s)
    cls = self.jt(ci.decl) if not ci.game else ci.jcls
    if ci.decl[0] == 'G' and ci.game:
        cls = ci.jcls
    t = self.temp(cls)
    if ci.game:
        self.emit(f'{t} = new {cls}();')
        self.emit(f'{t}.{ci.jname}({", ".join(jargs)});')
    else:
        if self.kind(ci.decl) == 'enum' or cls in JPRIMS:
            self.err(f'newobj of primitive {ci.dname}')
        if cls == 'String':
            self.emit(f'{t} = cs.System.StringX.$new({", ".join(jargs)});')
        elif cls == 'Object':
            self.emit(f'{t} = new Object();')
        else:
            self.emit(f'{t} = new {cls}({", ".join(jargs)});')
            self.x.ext_member(cls, 'ctor', None, ci.jparams, None)
    for s in post:
        self.emit(s)
    self.push(X.Val(t, cls, ci.decl, frozenset([t]), False, True))


def make_delegate(self, dct, target, ftn):
    m, virt = ftn
    ret_a, params_a, ret_d, params_d = self.x.delegate_sig(dct)
    dcls = self.jt(dct)
    self.x.used_delegates.add(self.ts.name_of(dct))
    jps = [self.ext_jt(p) if not self.ts.is_game(dct) else self.jt(p) for p in params_d]
    jr = self.ext_jt(ret_d) if not self.ts.is_game(dct) else self.jt(ret_d)
    ci = call_info(self, m)
    # arguments inside the anonymous class: a0.. of java types jps, .NET types params_a
    inner_args = list(type_args(self, ci))
    tps = ci.jparams[ci.sig.gen:]
    for k, pd in enumerate(ci.sig.params):
        pa = actual(self, ci, pd)
        src = X.Val(f'a{k}', jps[k], params_a[k])
        if jps[k] == 'Object' and tps[k] != 'Object':
            if pd[0] in 'VW':
                inner_args.append(src.e)
            else:
                inner_args.append(self.from_object(src.e, pa, tps[k]))
        elif pd[0] in 'VW' and jps[k] in JPRIMS:
            inner_args.append(self.boxed(src, params_a[k]))
        else:
            inner_args.append(self.conv(src, tps[k], pa))
    if ci.static:
        call = f'{ci.jcls}.{ci.jname}({", ".join(inner_args)})'
    elif ci.special == 'mapped':
        call = f'{ci.jcls}.{ci.jname}({", ".join(["$target"] + inner_args)})'
    else:
        call = f'(({ci.jcls}) $target).{ci.jname}({", ".join(inner_args)})'
    if jr == 'void':
        body = call + ';'
    else:
        rv = X.Val(call, ci.jret, actual(self, ci, ci.sig.ret))
        if ret_d[0] in 'VW' or jr == 'Object':
            e = self.boxed(rv, ret_a) if rv.jt in JPRIMS else rv.e
        else:
            e = self.conv(rv, jr)
        body = f'return {e};'
    pd = ', '.join(f'{jps[k]} a{k}' for k in range(len(jps)))
    key = jstr_literal(f'{ci.jcls}::{ci.jname}#{len(ci.sig.params)}')
    if ci.static:
        tgt = 'null'
    else:
        tgt = target.e if target.jt not in JPRIMS else self.boxed(target, target.ct)
    e = f'(({dcls}) new {dcls}() {{ protected {jr} $call({pd}) {{ {body} }} }}.$bind({tgt}, {key}))'
    t = self.temp(dcls)
    self.spill_heap()
    self.emit(f'{t} = {e};')
    return X.Val(t, dcls, dct, frozenset([t]), False, True)
