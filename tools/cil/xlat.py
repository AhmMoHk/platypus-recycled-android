"""
IL -> Java source translator.

    python3 -m cil.xlat <out_dir> <assembly.dll>... [--ref other.dll ...]

Translates every type of the given assemblies into Java classes (one file per class) that compile against the
hand-written runtime in rt/java (packages cs.System.*, cs.UnityEngine.*, cs.rt). See jtypes.py for the
conventions. Method bodies are translated instruction by instruction with a typed symbolic stack; control flow
inside a method becomes a `while(true) switch(pc)` dispatcher per protected region, try/catch/finally regions
become Java try statements.
"""
import os
import sys
import collections

from .md import World, MethodDef, FieldDef, MemberRef, tstr, subst, NAME_BY_PRIM, PRIM_BY_NAME
from .il import decode, ENDS_BLOCK, UNCOND, BRANCHES
from .jtypes import *


class XError(Exception):
    pass


MESSAGES = {  # Unity message methods called by the engine (name -> hook signature)
    'Awake': 0, 'Start': 0, 'Update': 0, 'FixedUpdate': 0, 'LateUpdate': 0, 'OnEnable': 0, 'OnDisable': 0,
    'OnDestroy': 0, 'OnGUI': 0, 'OnValidate': 0, 'Reset': 0, 'OnApplicationQuit': 0, 'OnApplicationFocus': 1,
    'OnApplicationPause': 1, 'OnTriggerEnter2D': 1, 'OnTriggerExit2D': 1, 'OnTriggerStay2D': 1,
    'OnCollisionEnter2D': 1, 'OnCollisionExit2D': 1, 'OnCollisionStay2D': 1, 'OnBecameVisible': 0,
    'OnBecameInvisible': 0, 'OnRectTransformDimensionsChange': 0, 'OnTransformParentChanged': 0,
    'OnTransformChildrenChanged': 0, 'OnCanvasGroupChanged': 0, 'OnDidApplyAnimationProperties': 0,
    'OnRenderObject': 0, 'OnWillRenderObject': 0, 'OnPreRender': 0, 'OnPostRender': 0,
}

PRIM_X = {  # .NET primitive type -> runtime helper class for its methods
    'bool': 'cs.System.BooleanX', 'char': 'cs.System.CharX', 'i1': 'cs.System.SByteX', 'u1': 'cs.System.ByteX',
    'i2': 'cs.System.Int16X', 'u2': 'cs.System.UInt16X', 'i4': 'cs.System.Int32X', 'u4': 'cs.System.UInt32X',
    'i8': 'cs.System.Int64X', 'u8': 'cs.System.UInt64X', 'r4': 'cs.System.SingleX', 'r8': 'cs.System.DoubleX',
    'i': 'cs.System.IntPtrX', 'u': 'cs.System.UIntPtrX', 'string': 'cs.System.StringX', 'object': 'cs.System.ObjectX',
}
TYPE_CONST = {'bool': 'BOOLEAN', 'char': 'CHAR', 'i1': 'SBYTE', 'u1': 'BYTE', 'i2': 'INT16', 'u2': 'UINT16',
              'i4': 'INT32', 'u4': 'UINT32', 'i8': 'INT64', 'u8': 'UINT64', 'r4': 'SINGLE', 'r8': 'DOUBLE',
              'i': 'INTPTR', 'u': 'UINTPTR', 'string': 'STRING', 'object': 'OBJECT', 'void': 'VOID', 'typedref': 'OBJECT'}

# kinds of Java values on the stack (for merge variables)
def jkind(jt):
    if jt in ('int', 'boolean', 'char', 'byte', 'short'):
        return 'I'
    if jt == 'long':
        return 'J'
    if jt == 'float':
        return 'F'
    if jt == 'double':
        return 'D'
    return 'O'


KIND_JT = {'I': 'int', 'J': 'long', 'F': 'float', 'D': 'double', 'O': 'Object'}


class Val:
    __slots__ = ('e', 'jt', 'ct', 'deps', 'heap', 'fresh', 'lv', 'ftn', 'token', 'const')

    def __init__(self, e, jt, ct=None, deps=frozenset(), heap=False, fresh=False, lv=None, ftn=None, token=None, const=None):
        self.e, self.jt, self.ct, self.deps, self.heap, self.fresh = e, jt, ct, deps, heap, fresh
        self.lv, self.ftn, self.token, self.const = lv, ftn, token, const

    def __repr__(self):
        return f'Val({self.e}:{self.jt})'


class LV:
    """A managed pointer: 'var' (assignable Java expression), 'cell' (Ref cell object) or 'struct' (struct object)."""
    __slots__ = ('kind', 'e', 'ct', 'jt')

    def __init__(self, kind, e, ct, jt):
        self.kind, self.e, self.ct, self.jt = kind, e, ct, jt

    def read(self):
        return self.e + '.v' if self.kind == 'cell' else self.e


class TypeSys:
    def __init__(self, world, game_modules):
        self.w = world
        self.game = set()
        for m in game_modules:
            for t in m.types:
                self.game.add(t.fullname)
        self.warnings = collections.Counter()
        self.used = {}

    def warn(self, msg):
        self.warnings[msg] += 1

    def name_of(self, ct):
        if ct[0] == 'G':
            ct = ct[1]
        if ct[0] == 'N':
            return ct[1]
        if ct[0] == 'P':
            return NAME_BY_PRIM.get(ct[1])
        return None

    def td(self, ct):
        n = self.name_of(ct)
        return self.w.types.get(n) if n else None

    def is_game(self, ct):
        n = self.name_of(ct)
        return n in self.game

    def kind(self, ct):
        k = ct[0]
        if k == 'P':
            return 'ref' if ct[1] in ('string', 'object', 'typedref') else ('void' if ct[1] == 'void' else 'prim')
        if k in 'VW':
            return 'gp'
        if k == 'A':
            return 'array'
        if k == 'M':
            return 'mdarray'
        if k == 'R':
            return 'byref'
        if k == '*':
            return 'ptr'
        name = self.name_of(ct)
        if name in RENAMES:
            r = RENAMES[name]
            if r in ('Object', 'String'):
                return 'ref'
        td = self.w.types.get(name)
        if td is not None:
            if td.is_enum:
                return 'enum'
            if td.is_value:
                return 'struct'
            if td.is_delegate:
                return 'delegate'
            if td.interface:
                return 'iface'
            return 'class'
        if name in EXT_ENUMS:
            return 'enum'
        if name in EXT_NOCOPY:
            return 'nocopy'
        if name in EXT_STRUCTS:
            return 'struct'
        if name in EXT_DELEGATES:
            return 'delegate'
        base = ct[1] if ct[0] == 'G' else ct
        if base[0] == 'N' and base[2]:
            self.warn(f'unknown external value type {name} (treated as struct)')
            return 'struct'
        return 'class'

    def enum_prim(self, ct):
        name = self.name_of(ct)
        td = self.w.types.get(name)
        if td is not None:
            u = td.enum_underlying()
            return u[1]
        return EXT_ENUMS.get(name, 'i4')

    def jt(self, ct):
        k = self.kind(ct)
        if k == 'prim':
            return PRIM_J[ct[1]]
        if k == 'void':
            return 'void'
        if k == 'ref':
            return 'String' if (ct[0] == 'P' and ct[1] == 'string') or self.name_of(ct) == 'System.String' else 'Object'
        if k == 'gp':
            return 'Object'
        if k == 'enum':
            return PRIM_J[self.enum_prim(ct)]
        if k == 'array':
            if ct[1][0] in 'VW':
                return 'Object'
            return self.jt(ct[1]) + '[]' if self.kind(ct[1]) != 'void' else 'Object[]'
        if k == 'mdarray':
            return 'cs.rt.MDArray'
        if k == 'byref':
            e = ct[1]
            ek = self.kind(e)
            if ek == 'struct':
                return self.jt(e)
            j = self.jt(e)
            return REF_CELL.get(j, 'cs.rt.RefO')
        if k == 'ptr':
            return 'long'
        n = self.name_of(ct)
        j = jclass_name(n)
        if n not in self.game:
            self.used.setdefault(j, (n, k))
        return j

    def is_struct(self, ct):
        return ct is not None and self.kind(ct) == 'struct'

    def type_name(self, ct):
        return tstr(ct)


class Mangler:
    """Java names and erased signatures of game methods and fields."""

    def __init__(self, ts, game_modules):
        self.ts = ts
        self.mods = game_modules
        self.mname = {}          # MethodDef -> java name
        self.fname = {}          # FieldDef -> java name
        self.bridges = collections.defaultdict(list)   # TypeDef -> [(java name, params jt, ret jt, impl MethodDef, decl info)]
        self._compute()

    def jparams(self, m):
        ts = self.ts
        ps = ['cs.System.Type'] * m.sig.gen + [ts.jt(p) for p in m.sig.params]
        return ps

    def jret(self, m):
        return self.ts.jt(m.sig.ret)

    def natural(self, m):
        n = m.name
        td = m.owner
        if n == '.ctor':
            return '$ctor_' + jsimple(jclass_name(td.fullname))
        if n == '.cctor':
            return '$cctor'
        if m.sig.hasthis and m.virtual:
            if n == 'ToString' and not m.sig.params and m.sig.ret == ('P', 'string'):
                return 'toString'
            if n == 'Equals' and len(m.sig.params) == 1 and m.sig.params[0] == ('P', 'object') and m.sig.ret == ('P', 'bool'):
                return 'equals'
            if n == 'GetHashCode' and not m.sig.params and m.sig.ret == ('P', 'i4'):
                return 'hashCode'
        if n in ('op_Implicit', 'op_Explicit'):
            return n + '$' + sanitize(jsimple(self.ts.jt(m.sig.ret)).replace('[]', 'Array'))
        if '.' in n and m.overrides:
            # explicit interface implementation: keep a unique name; bridges with the interface name are added
            return sanitize(n)
        return sanitize(n)

    def _compute(self):
        ts = self.ts
        alltypes = [t for m in self.mods for t in m.types]
        # fields: sanitize, unique per class
        for t in alltypes:
            used = set()
            for f in t.fields:
                n = sanitize(f.name)
                while n in used:
                    n += '_'
                used.add(n)
                self.fname[f] = n
        # methods: natural names
        for t in alltypes:
            for m in t.methods:
                self.mname[m] = self.natural(m)
        # resolve collisions: non-virtual instance methods that collide with an ancestor/descendant method,
        # and identical java signatures inside one class
        children = collections.defaultdict(list)
        for t in alltypes:
            b = t.base_name()
            if b in ts.game:
                children[b].append(t)

        def ancestors(t):
            out = []
            b = t.base_name()
            while b in ts.game:
                bt = ts.w.types[b]
                out.append(bt)
                b = bt.base_name()
            return out

        def descendants(t):
            out = []
            st = list(children[t.fullname])
            while st:
                c = st.pop()
                out.append(c)
                st.extend(children[c.fullname])
            return out

        def jkey(m):
            return (self.mname[m], tuple(self.jparams(m)))

        for t in alltypes:
            # duplicates inside the class
            seen = {}
            for m in t.methods:
                k = jkey(m)
                if k in seen:
                    other = seen[k]
                    # keep the one with the more specific return; rename this one
                    self.mname[m] = self.mname[m] + '$' + str(m.row)
                else:
                    seen[k] = m
        for t in alltypes:
            rel = ancestors(t) + descendants(t)
            if not rel:
                continue
            relkeys = {}
            for r in rel:
                for m in r.methods:
                    if m.sig.hasthis:
                        relkeys.setdefault(jkey(m), []).append((r, m))
            for m in t.methods:
                if not m.sig.hasthis or m.name in ('.ctor',):
                    continue
                k = jkey(m)
                if k not in relkeys:
                    continue
                if m.virtual and not m.newslot:
                    continue
                if m.virtual and m.newslot:
                    # newslot virtual: collides only if an ancestor has the same virtual slot name
                    anc = ancestors(t)
                    if not any(jkey(x) == k for a in anc for x in a.methods if x.sig.hasthis):
                        continue
                if m.name in MESSAGES and not m.virtual:
                    # private Unity message: unique name; the engine calls it through the $hook
                    pass
                self.mname[m] = self.mname[m] + '$' + sanitize(jsimple(jclass_name(t.fullname)))
        # static methods hiding instance methods of ancestors
        for t in alltypes:
            anc = ancestors(t)
            if not anc:
                continue
            ak = {jkey(x) for a in anc for x in a.methods if x.sig.hasthis}
            for m in t.methods:
                if not m.sig.hasthis and jkey(m) in ak:
                    self.mname[m] += '$s'

    def field(self, f):
        return self.fname[f]

    def method(self, m):
        return self.mname[m]


class Emitter:
    def __init__(self):
        self.lines = []
        self.ind = 0

    def __call__(self, s):
        self.lines.append('    ' * self.ind + s)

    def text(self):
        return '\n'.join(self.lines)


class Xlat:
    def __init__(self, world, game_modules, outdir):
        self.w = world
        self.mods = game_modules
        self.ts = TypeSys(world, game_modules)
        self.mg = Mangler(self.ts, game_modules)
        self.out = outdir
        self.ext_refs = collections.defaultdict(set)    # java class -> set of member descriptions
        self.errors = []
        self.used_delegates = set()

    # ------------------------------------------------------------------ helpers
    def jt(self, ct):
        return self.ts.jt(ct)

    def kind(self, ct):
        return self.ts.kind(ct)

    def note_ext(self, cls, desc):
        self.ext_refs[cls].add(desc)

    def ext_member(self, cls, kind, name, jparams, jret, static=None):
        if cls in JPRIMS or cls in ('Object', 'String') or cls.endswith('[]'):
            return
        if not hasattr(self, 'ext_members'):
            self.ext_members = collections.defaultdict(dict)
        key = (kind, name, tuple(jparams))
        old = self.ext_members[cls].get(key)
        if old is not None and not (old[3] == 'Object' and jret not in (None, 'Object')):
            return
        self.ext_members[cls][key] = (kind, name, tuple(jparams), jret, static)




    # ------------------------------------------------------------------ Unity serialization rules
    UNITY_SER_STRUCTS = {'UnityEngine.Vector2', 'UnityEngine.Vector3', 'UnityEngine.Vector4', 'UnityEngine.Rect', 'UnityEngine.Quaternion',
                         'UnityEngine.Matrix4x4', 'UnityEngine.Color', 'UnityEngine.Color32', 'UnityEngine.LayerMask', 'UnityEngine.Bounds',
                         'UnityEngine.Vector2Int', 'UnityEngine.Vector3Int', 'UnityEngine.RectInt', 'UnityEngine.BoundsInt',
                         'UnityEngine.AnimationCurve', 'UnityEngine.Gradient', 'UnityEngine.RectOffset', 'UnityEngine.GUIStyle',
                         'UnityEngine.Hash128', 'UnityEngine.Keyframe', 'UnityEngine.GradientColorKey', 'UnityEngine.GradientAlphaKey',
                         'UnityEngine.UI.Navigation', 'UnityEngine.UI.ColorBlock', 'UnityEngine.UI.SpriteState', 'UnityEngine.UI.AnimationTriggers'}

    def unity_serialized(self, f):
        if f.static or f.literal or f.initonly:
            return False
        has_sf = any(self.attr_name(a) in ('UnityEngine.SerializeField', 'UnityEngine.SerializeReference') for a in f.attrs)
        if not has_sf and (not f.public or (f.flags & 0x80)):
            return False
        return self.unity_serializable_type(f.type, 0)

    def unity_serializable_type(self, ct, depth):
        k = ct[0]
        if k == 'P':
            return ct[1] not in ('object', 'typedref', 'i', 'u')
        if k == 'A':
            return depth == 0 and self.unity_serializable_type(ct[1], 1)
        if k == 'G':
            name = ct[1][1]
            if name == 'System.Collections.Generic.List`1':
                return depth == 0 and self.unity_serializable_type(ct[2][0], 1)
            td = self.w.types.get(name)
            return td is not None and bool(td.flags & 0x2000) and not td.interface
        if k != 'N':
            return False
        name = ct[1]
        if name in self.UNITY_SER_STRUCTS:
            return True
        kind = self.ts.kind(ct)
        if kind == 'enum':
            return True
        if kind in ('delegate', 'iface'):
            return False
        td = self.w.types.get(name)
        if td is None:
            # external: UnityEngine.Object-derived types and UnityEvents
            return name.startswith('UnityEngine.') or name.startswith('TMPro.') or name.startswith('Rewired.')
        # UnityEngine.Object derived?
        b = td
        while b is not None:
            bn = b.base_name()
            if bn in ('UnityEngine.MonoBehaviour', 'UnityEngine.ScriptableObject', 'UnityEngine.Object', 'UnityEngine.Component',
                      'UnityEngine.Behaviour', 'UnityEngine.EventSystems.UIBehaviour') or (bn and bn.startswith('UnityEngine.Events.UnityEvent')):
                return True
            b = self.w.types.get(bn) if bn else None
        return bool(td.flags & 0x2000)
    # ------------------------------------------------------------------ reflection metadata
    ATTR_SKIP_NS = ('System.Runtime.CompilerServices', 'System.Diagnostics', 'System.Reflection', 'System.Runtime.InteropServices',
                    'System.Security', 'System.Runtime.Versioning', 'System.CodeDom')
    ATTR_SKIP = {'UnityEngine.SerializeField', 'UnityEngine.TooltipAttribute', 'UnityEngine.HeaderAttribute', 'UnityEngine.RangeAttribute',
                 'UnityEngine.HideInInspector', 'UnityEngine.Serialization.FormerlySerializedAsAttribute', 'UnityEngine.ContextMenu',
                 'UnityEngine.ExecuteInEditMode', 'UnityEngine.CreateAssetMenuAttribute', 'UnityEngine.TextAreaAttribute',
                 'UnityEngine.SpaceAttribute', 'UnityEngine.MultilineAttribute', 'UnityEngine.AddComponentMenu',
                 'UnityEngine.Scripting.PreserveAttribute', 'System.SerializableAttribute', 'System.NonSerializedAttribute',
                 'System.ObsoleteAttribute', 'System.ParamArrayAttribute', 'System.ComponentModel.EditorBrowsableAttribute',
                 'Unity.Profiling.IgnoredByDeepProfilerAttribute', 'System.FlagsAttribute', 'UnityEngine.ContextMenuItemAttribute',
                 'UnityEngine.ExecuteAlways', 'UnityEngine.HelpURLAttribute', 'UnityEngine.SelectionBaseAttribute'}

    def method_flags(self, m):
        a = m.flags & 7
        f = (1 if m.static else 0) | (2 if a == 6 else 0) | (4 if m.virtual else 0) | (8 if m.abstract else 0)
        f |= (16 if m.flags & 0x800 else 0) | (32 if m.name in ('.ctor', '.cctor') else 0) | (64 if a == 1 else 0)
        f |= (128 if a == 4 else 0) | (256 if a == 3 else 0) | (512 if a == 5 else 0) | (1024 if a == 2 else 0)
        f |= (2048 if m.sig.gen else 0) | (4096 if m.final else 0)
        return f

    def emit_member_meta(self, t, e):
        mm = []
        for m in t.methods:
            if m.name == '.cctor':
                continue
            ps = ', '.join(jstr_literal(tstr(p)) for p in m.sig.params)
            pn = ', '.join(jstr_literal(n or '') for n in m.paramnames)
            mm.append(f'new Object[]{{{jstr_literal(self.mg.method(m))}, {jstr_literal(m.name)}, {self.method_flags(m)}, '
                      f'{jstr_literal(tstr(m.sig.ret))}, new String[]{{{ps}}}, new String[]{{{pn}}}}}')
        e(f'public static final Object[][] $mmeta = {{{", ".join(mm)}}};')
        pm = []
        for pr in t.properties:
            g = jstr_literal(self.mg.method(pr.getter)) if pr.getter is not None else 'null'
            st = jstr_literal(self.mg.method(pr.setter)) if pr.setter is not None else 'null'
            pm.append(f'new Object[]{{{jstr_literal(pr.name)}, {jstr_literal(tstr(pr.type))}, {g}, {st}}}')
        e(f'public static final Object[][] $pmeta = {{{", ".join(pm)}}};')
        # custom attributes
        items = []
        def add(key, attrs):
            for a in attrs:
                code = self.attr_code(a)
                if code is not None:
                    items.append((key, code))
        add('', t.attrs)
        for f in t.fields:
            add('F:' + f.name, f.attrs)
        for m in t.methods:
            add('M:' + self.mg.method(m), m.attrs)
        for pr in t.properties:
            add('P:' + pr.name, pr.attrs)
        if items:
            e('public static Object[] $cattrs() {')
            e('    java.util.ArrayList<Object> r = new java.util.ArrayList<>();')
            for k, (decl, stmts) in items:
                e('    {')
                e(f'        {decl}')
                for st in stmts:
                    e(f'        {st}')
                e(f'        r.add({jstr_literal(k)}); r.add(a);')
                e('    }')
            e('    return r.toArray();')
            e('}')

    def attr_lit(self, v):
        k = v[0]
        if k == 'P':
            code, val = v[1], v[2]
            if code == 'string':
                return jstr_literal(val) if val is not None else 'null', 'String'
            if code == 'bool':
                return ('true' if val else 'false'), 'boolean'
            if code in ('r4',):
                import struct as _s
                return jfloat(('r4', val, _s.unpack('<I', _s.pack('<f', val))[0])), 'float'
            if code in ('r8',):
                import struct as _s
                return jdouble(('r8', val, _s.unpack('<Q', _s.pack('<d', val))[0])), 'double'
            if code in ('i8', 'u8'):
                return jlong(val), 'long'
            if code == 'char':
                return f'(char) {val}', 'char'
            return jint(val), 'int'
        if k == 'E':
            return jint(v[2]), 'int'
        if k == 'T':
            name = v[1]
            if name is None:
                return 'null', 'cs.System.Type'
            base = name.split(',')[0].strip().replace('+', '/')
            return f'cs.System.Reflection.Assembly.resolveType({jstr_literal(base)})', 'cs.System.Type'
        if k == 'A':
            if v[2] is None:
                return 'null', 'Object'
            et = self.ts.jt(v[1]) if v[1][0] != 'N' or v[1][1] != 'System.Type' else 'cs.System.Type'
            if v[1][0] == 'P' and v[1][1] == 'object':
                et = 'Object'
            elems = [self.attr_lit(x)[0] for x in v[2]]
            return f'new {et}[]{{{", ".join(elems)}}}', et + '[]'
        return 'null', 'Object'

    def attr_code(self, a):
        ctor, blob = a
        if isinstance(ctor, MethodDef):
            aname, sig, game = ctor.owner.fullname, ctor.sig, ctor.owner.fullname in self.ts.game
            md = ctor
        elif isinstance(ctor, MemberRef):
            aname, sig = self.ts.name_of(ctor.parent), ctor.sig
            md = ctor.resolved if isinstance(ctor.resolved, MethodDef) else None
            game = md is not None and md.owner.fullname in self.ts.game
        else:
            return None
        if aname is None or aname in self.ATTR_SKIP or any(aname.startswith(ns + '.') for ns in self.ATTR_SKIP_NS):
            return None
        mod = self.mods[0]
        def enum_size(n):
            td = self.w.types.get(n)
            if td is not None and td.is_enum:
                return {'i1': 1, 'u1': 1, 'i2': 2, 'u2': 2, 'i4': 4, 'u4': 4, 'i8': 8, 'u8': 8}.get(td.enum_underlying()[1], 4)
            return 4
        dec = mod.decode_attr(sig, blob, enum_size)
        if dec is None:
            return None
        fixed, named = dec
        cls = jclass_name(aname)
        self.ts.used.setdefault(cls, (aname, 'class')) if not game else None
        args = []
        for i, v in enumerate(fixed):
            lit, jt = self.attr_lit(v)
            pj = self.ts.jt(sig.params[i])
            if pj != jt and jt not in JPRIMS and pj not in JPRIMS:
                lit = f'(({pj}) (Object) {lit})'
            elif pj != jt and pj in JPRIMS and jt in JPRIMS:
                lit = f'(({pj}) {lit})'
            elif pj == 'Object' and jt in JPRIMS:
                lit = f'{BOX_J[jt]}.valueOf({lit})'
            args.append(lit)
        stmts = []
        if game:
            decl = f'{cls} a = new {cls}();'
            stmts.append(f'a.{self.mg.method(md)}({", ".join(args)});')
        else:
            decl = f'{cls} a = new {cls}({", ".join(args)});'
            self.ext_member(cls, 'ctor', None, [self.ts.jt(p) for p in sig.params], None)
        for kind, name, t, v in named:
            lit, jt = self.attr_lit(v)
            if kind == 'F':
                stmts.append(f'a.{sanitize(name)} = {lit};')
                if not game:
                    self.ext_member(cls, 'field', sanitize(name), [], jt if jt != 'Object' else 'Object', static=False)
            else:
                stmts.append(f'a.set_{sanitize(name)}({lit});')
                if not game:
                    self.ext_member(cls, 'inst', 'set_' + sanitize(name), [jt], 'void')
        return decl, stmts
    # ------------------------------------------------------------------ type expressions / hierarchy
    def enum_class_expr(self, ct):
        name = self.ts.name_of(ct)
        j = jclass_name(name)
        if name not in self.ts.game:
            self.ts.used.setdefault(j, (name, 'enum'))
        return f'{j}.$info'

    def type_expr(self, ct, mx=None):
        k = ct[0]
        if k == 'W':
            return f'$T{ct[1]}'
        if k == 'V':
            # class generic parameters are erased; the one runtime use (TMP_ObjectPool<T> with T : new()) is only
            # ever instantiated with List<X> (TMP_ListPool)
            owner = getattr(getattr(mx, 'm', None), 'owner', None) if mx is not None else None
            if owner is not None and owner.fullname == 'TMPro.TMP_ObjectPool`1':
                return 'cs.System.Type.of(cs.System.Collections.Generic.List_1.class)'
            self.ts.warn('runtime use of class generic parameter')
            return 'cs.System.Type.OBJECT'
        if k == 'P':
            return f'cs.System.Type.{TYPE_CONST[ct[1]]}'
        if k == 'A':
            return f'cs.System.Type.arrayOf({self.type_expr(ct[1], mx)})'
        if k in 'R*':
            return self.type_expr(ct[1], mx)
        if k == 'M':
            return 'cs.System.Type.OBJECT'
        if k == 'G':
            base = jclass_name(ct[1][1])
            if ct[1][1] not in self.ts.game:
                self.ts.used.setdefault(base, (ct[1][1], self.ts.kind(ct)))
            args = ', '.join(self.type_expr(a, mx) for a in ct[2])
            return f'cs.System.Type.generic({base}.class, {args})'
        name = ct[1]
        j = jclass_name(name)
        if name not in self.ts.game and j not in ('Object', 'String'):
            self.ts.used.setdefault(j, (name, self.ts.kind(ct)))
        if j == 'Object':
            return 'cs.System.Type.OBJECT'
        if j == 'String':
            return 'cs.System.Type.STRING'
        return f'cs.System.Type.of({j}.class)'

    def java_subclass(self, src, dst):
        if not hasattr(self, '_jh'):
            self._jh = {}
            for m in self.mods:
                for t in m.types:
                    j = jclass_name(t.fullname)
                    sup = []
                    if t.extends is not None:
                        sup.append(self.ts.jt(t.extends))
                    for i in t.interfaces:
                        sup.append(self.ts.jt(i))
                    if t.is_value:
                        sup.append('cs.rt.Struct')
                    self._jh[j] = sup
            for k, v in RT_HIERARCHY.items():
                self._jh.setdefault(k, v)
        seen = set()
        st = [src]
        while st:
            c = st.pop()
            if c == dst:
                return True
            if c in seen:
                continue
            seen.add(c)
            st.extend(self._jh.get(c, []))
        return False

    def write_support(self):
        d = os.path.join(self.out, 'cs', 'rt')
        os.makedirs(d, exist_ok=True)
        names = []
        for m in self.mods:
            for t in m.types:
                if t.name == '<Module>':
                    continue
                names.append(jclass_name(t.fullname))
        # static methods carrying platform / startup attributes ("javaClass|csMethod|attributeFullName")
        attributed = []
        for m in self.mods:
            for t in m.types:
                for md in t.methods:
                    for ctor, blob in getattr(md, 'attrs', []):
                        if isinstance(ctor, MethodDef):
                            an = ctor.owner.fullname
                        elif isinstance(ctor, MemberRef):
                            an = self.ts.name_of(ctor.parent)
                        else:
                            continue
                        if an and (an.startswith('Blackdrop.') or an.startswith('UnityEngine.RuntimeInitializeOnLoadMethod')):
                            attributed.append(f'{jclass_name(t.fullname)}|{md.name}|{an}')
        with open(os.path.join(d, 'GameTypes.java'), 'w') as f:
            f.write('package cs.rt;\n\npublic final class GameTypes {\n    public static final String[] NAMES = {\n')
            for n in names:
                f.write(f'        "{n}",\n')
            f.write('    };\n    public static final String[] ATTRIBUTED = {\n')
            for n in attributed:
                f.write(f'        "{n}",\n')
            f.write('    };\n}\n')

    # ------------------------------------------------------------------ class emission
    def run(self):
        n = 0
        for m in self.mods:
            for t in m.types:
                if t.name == '<Module>' and not t.methods and not t.fields:
                    continue
                try:
                    src = self.emit_type(t)
                except XError as e:
                    self.errors.append(f'{t.fullname}: {e}')
                    continue
                fq = jclass_name(t.fullname)
                pkg, cls = fq.rsplit('.', 1)
                d = os.path.join(self.out, *pkg.split('.'))
                os.makedirs(d, exist_ok=True)
                with open(os.path.join(d, cls + '.java'), 'w') as f:
                    f.write(src)
                n += 1
        return n

    def emit_type(self, t):
        ts = self.ts
        fq = jclass_name(t.fullname)
        pkg, cls = fq.rsplit('.', 1)
        e = Emitter()
        e(f'package {pkg};')
        e('')
        e('@SuppressWarnings({"unchecked", "rawtypes", "unused"})')
        if t.is_enum:
            return self.emit_enum(t, e, cls)
        if t.is_delegate:
            return self.emit_delegate(t, e, cls)
        ifs = []
        for i in t.interfaces:
            j = self.jt(i)
            if j not in ('Object',) and j not in ifs:
                ifs.append(j)
        if t.interface:
            ext = (' extends ' + ', '.join(ifs)) if ifs else ''
            e(f'public interface {cls}{ext} {{')
            e.ind += 1
            for f in t.fields:
                if f.static:
                    self.emit_field(t, f, e)
            for m in t.methods:
                if m.name == '.cctor':
                    continue
                if not m.static:
                    e(f'{self.jret_of(m)} {self.mg.method(m)}({self.param_decl(m)});')
            e.ind -= 1
            e('}')
            return e.text() + '\n'
        base = None
        bn = t.base_name()
        if t.is_value:
            base = None
            ifs.insert(0, 'cs.rt.Struct')
        elif bn and bn != 'System.Object':
            base = self.jt(t.extends)
            if base == 'Object':
                base = None
        mods = 'public '
        if t.abstract and not t.sealed:
            mods += 'abstract '
        ext = f' extends {base}' if base else ''
        imp = (' implements ' + ', '.join(ifs)) if ifs else ''
        e(f'{mods}class {cls}{ext}{imp} {{')
        e.ind += 1
        for f in t.fields:
            self.emit_field(t, f, e)
        meta = []
        for f in t.fields:
            if f.literal:
                continue
            flags = (1 if f.static else 0) | (2 if f.public else 0) | (4 if f.initonly else 0) | (8 if self.unity_serialized(f) else 0)
            meta += [jstr_literal(self.mg.field(f)), jstr_literal(f.name), jstr_literal(tstr(f.type)), str(flags)]
        e(f'public static final Object[] $fmeta = {{{", ".join(meta)}}};')
        self.emit_member_meta(t, e)
        if any(m.name == '.cctor' for m in t.methods):
            e('static { $cctor(); }')
        if t.is_value:
            self.emit_struct_support(t, e, cls)
        for m in t.methods:
            try:
                self.emit_method(t, m, e)
            except XError as ex:
                self.errors.append(f'{t.fullname}::{m.name}: {ex}')
                e(f'// ERROR translating {m.name}: {ex}')
                self.emit_stub(m, e, str(ex))
            except Exception as ex:
                import traceback
                self.errors.append(f'{t.fullname}::{m.name}: INTERNAL {ex!r}\n{traceback.format_exc()}')
                e(f'// INTERNAL ERROR translating {m.name}')
                self.emit_stub(m, e, 'internal error')
        self.emit_hooks(t, e)
        self.emit_bridges(t, e)
        e.ind -= 1
        e('}')
        return e.text() + '\n'

    def emit_enum(self, t, e, cls):
        prim = t.enum_underlying()[1]
        j = PRIM_J[prim]
        e(f'public final class {cls} {{')
        e.ind += 1
        names, vals = [], []
        for f in t.fields:
            if f.static and f.literal and f.const is not None:
                v = f.const[1]
                lit = jlong(v) if j == 'long' else (jint(v) if j in ('int',) else f'({j}){int(v)}')
                e(f'public static final {j} {self.mg.field(f)} = {lit};')
                names.append(jstr_literal(f.name))
                vals.append(jlong(v))
        flags = any(self.attr_name(a) == 'System.FlagsAttribute' for a in t.attrs)
        e(f'public static final cs.rt.EnumInfo $info = new cs.rt.EnumInfo({jstr_literal(t.fullname)}, {cls}.class, '
          f'{"true" if flags else "false"}, new String[]{{{", ".join(names)}}}, new long[]{{{", ".join(vals)}}});')
        e.ind -= 1
        e('}')
        return e.text() + '\n'

    def attr_name(self, a):
        ctor, _ = a
        if isinstance(ctor, MethodDef):
            return ctor.owner.fullname
        if isinstance(ctor, MemberRef) and ctor.parent:
            return self.ts.name_of(ctor.parent)
        return None

    def delegate_sig(self, ct):
        """(ret, params) of a delegate type's Invoke, with the delegate's own generic args substituted."""
        name = self.ts.name_of(ct)
        targs = ct[2] if ct[0] == 'G' else None
        td = self.w.types.get(name)
        if td is not None:
            inv = [m for m in td.methods if m.name == 'Invoke'][0]
            return subst(inv.sig.ret, targs, None), [subst(p, targs, None) for p in inv.sig.params], inv.sig.ret, inv.sig.params
        if name in EXT_DELEGATES:
            r, ps = EXT_DELEGATES[name]
            return subst(r, targs, None), [subst(p, targs, None) for p in ps], r, ps
        raise XError(f'unknown delegate type {name}')

    def emit_delegate(self, t, e, cls):
        inv = [m for m in t.methods if m.name == 'Invoke'][0]
        e(f'public class {cls} extends cs.System.Delegate {{')
        e.ind += 1
        ret = self.jt(inv.sig.ret)
        pd = ', '.join(f'{self.jt(p)} a{i}' for i, p in enumerate(inv.sig.params))
        pa = ', '.join(f'a{i}' for i in range(len(inv.sig.params)))
        e(f'public {ret} Invoke({pd}) {{')
        if ret == 'void':
            e(f'    if ($list != null) {{ for (cs.System.Delegate d : $list) (({cls}) d).$call({pa}); return; }}')
            e(f'    $call({pa});')
        else:
            e(f'    if ($list != null) {{ {ret} r = {self.default_of(ret)}; for (cs.System.Delegate d : $list) r = (({cls}) d).$call({pa}); return r; }}')
            e(f'    return $call({pa});')
        e('}')
        e(f'protected {ret} $call({pd}) {{ throw new IllegalStateException("unbound delegate"); }}')
        e(f'protected cs.System.Delegate $empty() {{ return new {cls}(); }}')
        e.ind -= 1
        e('}')
        return e.text() + '\n'

    @staticmethod
    def default_of(jt):
        if jt == 'boolean':
            return 'false'
        if jt in ('int', 'short', 'byte', 'char'):
            return '0' if jt == 'int' else f'({jt})0'
        if jt == 'long':
            return '0L'
        if jt == 'float':
            return '0f'
        if jt == 'double':
            return '0d'
        return 'null'

    def emit_field(self, t, f, e):
        ts = self.ts
        j = self.jt(f.type)
        name = self.mg.field(f)
        st = 'static ' if f.static else ''
        if f.literal:
            if f.const is None:
                return
            v = f.const[1]
            if j == 'String':
                lit = jstr_literal(v) if v is not None else 'null'
            elif j == 'boolean':
                lit = 'true' if v else 'false'
            elif j == 'float':
                import struct as _s
                lit = jfloat(('r4', v, _s.unpack('<I', _s.pack('<f', v))[0]))
            elif j == 'double':
                import struct as _s
                lit = jdouble(('r8', v, _s.unpack('<Q', _s.pack('<d', v))[0]))
            elif j == 'long':
                lit = jlong(v)
            elif j == 'char':
                lit = f'(char){int(v)}'
            elif j in ('byte', 'short'):
                lit = f'({j}){int(v)}'
            elif j == 'int':
                lit = jint(v)
            else:
                lit = 'null'
            e(f'public static final {j} {name} = {lit};')
            return
        init = ''
        if ts.kind(f.type) == 'struct':
            init = f' = new {j}()'
        e(f'public {st}{j} {name}{init};')
        if f.rva is not None:
            # static data blob (used by RuntimeHelpers.InitializeArray)
            size = t.module.typedefs and self.field_data_size(f)
            data = bytes(t.module.data[t.module.pe.get_offset_from_rva(f.rva): t.module.pe.get_offset_from_rva(f.rva) + size])
            import base64
            e(f'public static final byte[] {name}$data = cs.rt.Rt.b64({jstr_literal(base64.b64encode(data).decode())});')

    def field_data_size(self, f):
        t = f.type
        if t[0] == 'N':
            td = self.w.types.get(t[1])
            if td is not None and td.layout_size:
                return td.layout_size
        if t[0] == 'P':
            return {'i1': 1, 'u1': 1, 'i2': 2, 'u2': 2, 'char': 2, 'i4': 4, 'u4': 4, 'r4': 4, 'i8': 8, 'u8': 8, 'r8': 8}.get(t[1], 0)
        return 0

    def emit_struct_support(self, t, e, cls):
        flds = [f for f in t.fields if not f.static]
        e(f'public {cls} $copy() {{ {cls} c = new {cls}(); c.$set(this); return c; }}')
        e(f'public void $set(Object o) {{')
        e(f'    {cls} s = ({cls}) o;')
        for f in flds:
            n = self.mg.field(f)
            if self.ts.kind(f.type) == 'struct':
                e(f'    this.{n}.$set(s.{n});')
            else:
                e(f'    this.{n} = s.{n};')
        e('}')
        e('public void $clear() {')
        for f in flds:
            n = self.mg.field(f)
            if self.ts.kind(f.type) == 'struct':
                e(f'    this.{n}.$clear();')
            else:
                e(f'    this.{n} = {self.default_of(self.jt(f.type))};')
        e('}')
        e(f'public static {cls}[] $newArray(int n) {{ {cls}[] a = new {cls}[n]; for (int i = 0; i < n; i++) a[i] = new {cls}(); return a; }}')
        has_eq = any(self.mg.method(m) == 'equals' for m in t.methods)
        has_hc = any(self.mg.method(m) == 'hashCode' for m in t.methods)
        if not has_eq:
            conds = ' && '.join(f'cs.System.ObjectX.Equals(this.{self.mg.field(f)}, s.{self.mg.field(f)})'
                                if self.jt(f.type) not in JPRIMS else f'this.{self.mg.field(f)} == s.{self.mg.field(f)}'
                                for f in flds) or 'true'
            e(f'public boolean equals(Object o) {{ if (!(o instanceof {cls})) return false; {cls} s = ({cls}) o; return {conds}; }}')
        if not has_hc:
            parts = ' * 31 + '.join(f'cs.System.ObjectX.hash(this.{self.mg.field(f)})' for f in flds) or '0'
            e(f'public int hashCode() {{ return {parts}; }}')

    def param_decl(self, m):
        ps = []
        for i in range(m.sig.gen):
            ps.append(f'cs.System.Type $T{i}')
        for i, p in enumerate(m.sig.params):
            ps.append(f'{self.jt(p)} a{i}')
        return ', '.join(ps)

    def jret_of(self, m):
        if m.name == '.ctor':
            return 'void'
        return self.jt(m.sig.ret)

    def emit_stub(self, m, e, why):
        mods = 'public ' + ('static ' if m.static else '')
        e(f'{mods}{self.jret_of(m)} {self.mg.method(m)}({self.param_decl(m)}) {{ throw new RuntimeException({jstr_literal("untranslated: " + m.owner.fullname + "::" + m.name + " " + why)}); }}')

    def emit_method(self, t, m, e):
        name = self.mg.method(m)
        ret = self.jret_of(m)
        mods = 'public '
        if m.static:
            mods += 'static '
        if m.abstract:
            if t.interface:
                return
            e(f'public abstract {ret} {name}({self.param_decl(m)});')
            return
        if not m.rva:
            # extern / runtime implemented
            self.emit_stub(m, e, 'no body')
            return
        from .method import MethodX
        mx = MethodX(self, t, m)
        body = mx.translate()
        e(f'{mods}{ret} {name}({self.param_decl(m)}) {{')
        e.ind += 1
        for line in body:
            e(line)
        e.ind -= 1
        e('}')

    def emit_hooks(self, t, e):
        """Unity message hooks: the engine calls $Name(...) which forwards to the most derived declaration."""
        for m in t.methods:
            if m.static or m.name not in MESSAGES or len(m.sig.params) != MESSAGES[m.name]:
                continue
            ps = self.param_decl(m)
            args = ', '.join(f'a{i}' for i in range(len(m.sig.params)))
            r = self.jt(m.sig.ret)
            if r == 'void':
                e(f'public Object ${m.name}({ps}) {{ {self.mg.method(m)}({args}); return null; }}')
            else:
                e(f'public Object ${m.name}({ps}) {{ return {self.mg.method(m)}({args}); }}')

    # ------------------------------------------------------------------ interface bridges
    def iface_methods(self, ict):
        """[(java name, java params, java ret, decl params (ct), decl ret (ct), targs)] for an interface type"""
        name = self.ts.name_of(ict)
        targs = ict[2] if ict[0] == 'G' else None
        td = self.w.types.get(name)
        out = []
        if td is not None:
            for m in td.methods:
                if m.static or m.name == '.cctor':
                    continue
                out.append((self.mg.method(m), [self.jt(p) for p in m.sig.params], self.jt(m.sig.ret), m.sig.params, m.sig.ret, targs, m.name))
                # inherited interfaces handled by caller
            return out
        ext = EXT_IFACES.get(name)
        if ext is None:
            self.ts.warn(f'unknown external interface {name}')
            return out
        for (n, ps, r) in ext:
            out.append((n, [self.jt(p) for p in ps], self.jt(r), ps, r, targs, n))
        return out

    def all_ifaces(self, t):
        res = []
        seen = set()
        st = list(t.interfaces)
        while st:
            i = st.pop(0)
            k = tstr(i)
            if k in seen:
                continue
            seen.add(k)
            res.append(i)
            td = self.ts.td(i)
            if td is not None:
                targs = i[2] if i[0] == 'G' else None
                st.extend(subst(x, targs, None) for x in td.interfaces)
            else:
                for x in EXT_IFACE_PARENTS.get(self.ts.name_of(i), []):
                    st.append(subst(x, i[2] if i[0] == 'G' else None, None))
        return res

    def emit_bridges(self, t, e):
        if t.interface:
            return
        have = {}
        for m in t.methods:
            if m.sig.hasthis:
                have[(self.mg.method(m), tuple(['cs.System.Type'] * m.sig.gen + [self.jt(p) for p in m.sig.params]))] = m
        emitted = set()
        for ict in self.all_ifaces(t):
            iname = self.ts.name_of(ict)
            for (jn, jps, jr, dps, dr, targs, cname) in self.iface_methods(ict):
                key = (jn, tuple(jps))
                if key in have or key in emitted:
                    continue
                # find the implementing method: explicit override, else by name and substituted signature
                impl = None
                for m in t.methods:
                    for o in m.overrides:
                        on = o.name if hasattr(o, 'name') else None
                        opn = self.ts.name_of(o.parent) if isinstance(o, MemberRef) and o.parent else (o.owner.fullname if isinstance(o, MethodDef) else None)
                        if on == cname and opn == iname and len(m.sig.params) == len(dps):
                            impl = m
                if impl is None:
                    for m in t.methods:
                        if m.sig.hasthis and m.name == cname and len(m.sig.params) == len(dps):
                            if [tstr(p) for p in m.sig.params] == [tstr(subst(p, targs, None)) for p in dps]:
                                impl = m
                if impl is None:
                    # implemented by a base class method: check inherited
                    impl = self.find_inherited(t, cname, [subst(p, targs, None) for p in dps])
                    if impl is None:
                        continue
                    if (self.mg.method(impl), tuple(self.jt(p) for p in impl.sig.params)) == key:
                        continue
                emitted.add(key)
                # bridge
                pd = ', '.join(f'{jps[i]} a{i}' for i in range(len(jps)))
                args = []
                for i, p in enumerate(impl.sig.params):
                    args.append(self.coerce_simple(f'a{i}', jps[i], subst(dps[i], targs, None), self.jt(p), p))
                call = f'{self.mg.method(impl)}({", ".join(args)})'
                if jr == 'void':
                    e(f'public void {jn}({pd}) {{ {call}; }}')
                else:
                    rv = self.coerce_simple(call, self.jt(impl.sig.ret), impl.sig.ret, jr, dr if dr[0] not in 'VW' else ('P', 'object'))
                    e(f'public {jr} {jn}({pd}) {{ return {rv}; }}')

    def find_inherited(self, t, name, params):
        b = t.base_name()
        while b in self.ts.game:
            bt = self.w.types[b]
            for m in bt.methods:
                if m.sig.hasthis and m.name == name and [tstr(p) for p in m.sig.params] == [tstr(p) for p in params]:
                    return m
            b = bt.base_name()
        return None

    def coerce_simple(self, expr, from_jt, ct, to_jt, to_ct):
        """Convert expr (java type from_jt, .NET type ct) to java type to_jt."""
        if from_jt == to_jt:
            return expr
        if to_jt == 'void':
            return expr
        if to_jt in JPRIMS:
            if from_jt in JPRIMS:
                return prim_conv(expr, from_jt, to_jt)
            return unbox_expr(expr, to_jt)
        if from_jt in JPRIMS:
            return box_expr(self, expr, from_jt, ct)
        if to_jt == 'Object':
            return expr
        return f'(({to_jt}) (Object) ({expr}))'


# runtime class hierarchy (java names) used for cast elision
RT_HIERARCHY = {
    'cs.UnityEngine.Component': ['cs.UnityEngine.UObject'], 'cs.UnityEngine.GameObject': ['cs.UnityEngine.UObject'],
    'cs.UnityEngine.Behaviour': ['cs.UnityEngine.Component'], 'cs.UnityEngine.MonoBehaviour': ['cs.UnityEngine.Behaviour'],
    'cs.UnityEngine.Transform': ['cs.UnityEngine.Component'], 'cs.UnityEngine.RectTransform': ['cs.UnityEngine.Transform'],
    'cs.UnityEngine.Renderer': ['cs.UnityEngine.Component'], 'cs.UnityEngine.SpriteRenderer': ['cs.UnityEngine.Renderer'],
    'cs.UnityEngine.LineRenderer': ['cs.UnityEngine.Renderer'], 'cs.UnityEngine.TrailRenderer': ['cs.UnityEngine.Renderer'],
    'cs.UnityEngine.ScriptableObject': ['cs.UnityEngine.UObject'], 'cs.UnityEngine.Sprite': ['cs.UnityEngine.UObject'],
    'cs.UnityEngine.Texture': ['cs.UnityEngine.UObject'], 'cs.UnityEngine.Texture2D': ['cs.UnityEngine.Texture'],
    'cs.UnityEngine.RenderTexture': ['cs.UnityEngine.Texture'], 'cs.UnityEngine.Material': ['cs.UnityEngine.UObject'],
    'cs.UnityEngine.AudioClip': ['cs.UnityEngine.UObject'], 'cs.UnityEngine.AudioSource': ['cs.UnityEngine.Behaviour'],
    'cs.UnityEngine.Camera': ['cs.UnityEngine.Behaviour'], 'cs.UnityEngine.Collider2D': ['cs.UnityEngine.Behaviour'],
    'cs.UnityEngine.PolygonCollider2D': ['cs.UnityEngine.Collider2D'], 'cs.UnityEngine.EdgeCollider2D': ['cs.UnityEngine.Collider2D'],
    'cs.UnityEngine.CircleCollider2D': ['cs.UnityEngine.Collider2D'], 'cs.UnityEngine.BoxCollider2D': ['cs.UnityEngine.Collider2D'],
    'cs.UnityEngine.CustomCollider2D': ['cs.UnityEngine.Collider2D'], 'cs.UnityEngine.Rigidbody2D': ['cs.UnityEngine.Component'],
    'cs.UnityEngine.Animator': ['cs.UnityEngine.Behaviour'], 'cs.UnityEngine.Canvas': ['cs.UnityEngine.Behaviour'],
    'cs.UnityEngine.CanvasGroup': ['cs.UnityEngine.Behaviour'], 'cs.UnityEngine.Shader': ['cs.UnityEngine.UObject'],
    'cs.UnityEngine.Font': ['cs.UnityEngine.UObject'], 'cs.UnityEngine.TextAsset': ['cs.UnityEngine.UObject'],
    'cs.UnityEngine.Rendering.SortingGroup': ['cs.UnityEngine.Behaviour'],
    'cs.UnityEngine.EventSystems.UIBehaviour': ['cs.UnityEngine.MonoBehaviour'],
    'cs.UnityEngine.UI.Graphic': ['cs.UnityEngine.EventSystems.UIBehaviour'],
    'cs.UnityEngine.UI.MaskableGraphic': ['cs.UnityEngine.UI.Graphic'], 'cs.UnityEngine.UI.Image': ['cs.UnityEngine.UI.MaskableGraphic'],
    'cs.UnityEngine.UI.RawImage': ['cs.UnityEngine.UI.MaskableGraphic'], 'cs.UnityEngine.UI.Text': ['cs.UnityEngine.UI.MaskableGraphic'],
    'cs.UnityEngine.UI.Selectable': ['cs.UnityEngine.EventSystems.UIBehaviour'], 'cs.UnityEngine.UI.Button': ['cs.UnityEngine.UI.Selectable'],
    'cs.UnityEngine.UI.Toggle': ['cs.UnityEngine.UI.Selectable'], 'cs.UnityEngine.UI.Slider': ['cs.UnityEngine.UI.Selectable'],
    'cs.UnityEngine.UI.Mask': ['cs.UnityEngine.EventSystems.UIBehaviour'], 'cs.UnityEngine.UI.AspectRatioFitter': ['cs.UnityEngine.EventSystems.UIBehaviour'],
    'cs.UnityEngine.UI.LayoutGroup': ['cs.UnityEngine.EventSystems.UIBehaviour'], 'cs.UnityEngine.UI.GridLayoutGroup': ['cs.UnityEngine.UI.LayoutGroup'],
    'cs.UnityEngine.EventSystems.EventSystem': ['cs.UnityEngine.EventSystems.UIBehaviour'],
    'cs.TMPro.TMP_Text': ['cs.UnityEngine.UI.MaskableGraphic'], 'cs.TMPro.TextMeshProUGUI': ['cs.TMPro.TMP_Text'],
    'cs.TMPro.TextMeshPro': ['cs.TMPro.TMP_Text'], 'cs.UnityEngine.Video.VideoPlayer': ['cs.UnityEngine.Behaviour'],
    'cs.System.Collections.Generic.IEnumerator_1': ['cs.System.Collections.IEnumerator', 'cs.System.IDisposable'],
    'cs.System.Collections.Generic.IEnumerable_1': ['cs.System.Collections.IEnumerable'],
    'cs.System.Collections.Generic.List_1': ['cs.System.Collections.Generic.IList_1', 'cs.System.Collections.IList'],
    'cs.System.Collections.Generic.IList_1': ['cs.System.Collections.Generic.ICollection_1'],
    'cs.System.Collections.Generic.ICollection_1': ['cs.System.Collections.Generic.IEnumerable_1'],
    'cs.System.Collections.IList': ['cs.System.Collections.ICollection'], 'cs.System.Collections.ICollection': ['cs.System.Collections.IEnumerable'],
    'cs.System.Collections.Generic.Dictionary_2': ['cs.System.Collections.Generic.IDictionary_2'],
    'cs.System.Collections.Generic.IDictionary_2': ['cs.System.Collections.Generic.ICollection_1'],
    'cs.System.Collections.Generic.HashSet_1': ['cs.System.Collections.Generic.ICollection_1'],
    'cs.System.Collections.Generic.Queue_1': ['cs.System.Collections.Generic.IEnumerable_1'],
    'cs.System.Collections.Generic.Stack_1': ['cs.System.Collections.Generic.IEnumerable_1'],
    'cs.System.Exception': ['java.lang.RuntimeException'],
}

for _n, _e in EXT_INDEX.items():
    _j = jclass_name(_n)
    if _j in RT_HIERARCHY or _e['k'] not in ('class', 'iface'):
        continue
    _sup = []
    _b = _e.get('base')
    if _b and _b in EXT_INDEX:
        _sup.append(jclass_name(_b))
    for _i in _e['ifaces']:
        if _i in EXT_INDEX:
            _sup.append(jclass_name(_i))
    if _sup:
        RT_HIERARCHY[_j] = _sup


# external interfaces implemented by game types: name -> [(method, params, ret)]
_O = ('P', 'object')
EXT_IFACES = {
    'System.Collections.IEnumerator': [('MoveNext', [], ('P', 'bool')), ('get_Current', [], _O), ('Reset', [], ('P', 'void'))],
    'System.Collections.Generic.IEnumerator`1': [('get_Current', [], ('V', 0))],
    'System.Collections.IEnumerable': [('GetEnumerator', [], ('N', 'System.Collections.IEnumerator', False))],
    'System.Collections.Generic.IEnumerable`1': [('GetEnumerator', [], ('G', ('N', 'System.Collections.Generic.IEnumerator`1', False), (('V', 0),)))],
    'System.IDisposable': [('Dispose', [], ('P', 'void'))],
    'System.Collections.IComparer': [('Compare', [_O, _O], ('P', 'i4'))],
    'System.Collections.ICollection': [('get_Count', [], ('P', 'i4'))],
    'System.Collections.IList': [('IList_Add', [_O], ('P', 'i4')), ('get_Item', [('P', 'i4')], _O), ('set_Item', [('P', 'i4'), _O], ('P', 'void')),
                                 ('Clear', [], ('P', 'void')), ('Contains', [_O], ('P', 'bool')), ('IndexOf', [_O], ('P', 'i4')),
                                 ('RemoveAt', [('P', 'i4')], ('P', 'void'))],
    'System.Collections.Generic.ICollection`1': [('get_Count', [], ('P', 'i4')), ('Add', [('V', 0)], ('P', 'void')), ('Clear', [], ('P', 'void')),
                                                 ('Contains', [('V', 0)], ('P', 'bool')), ('Remove', [('V', 0)], ('P', 'bool'))],
    'System.Collections.Generic.IList`1': [('get_Item', [('P', 'i4')], ('V', 0)), ('set_Item', [('P', 'i4'), ('V', 0)], ('P', 'void')),
                                           ('IndexOf', [('V', 0)], ('P', 'i4')), ('Insert', [('P', 'i4'), ('V', 0)], ('P', 'void')),
                                           ('RemoveAt', [('P', 'i4')], ('P', 'void'))],
    'System.Collections.Generic.IReadOnlyCollection`1': [('get_Count', [], ('P', 'i4'))],
    'System.Collections.Generic.IReadOnlyList`1': [('get_Item', [('P', 'i4')], ('V', 0))],
    'System.Collections.Generic.IDictionary`2': [('Add', [('V', 0), ('V', 1)], ('P', 'void')), ('ContainsKey', [('V', 0)], ('P', 'bool')),
                                                 ('TryGetValue', [('V', 0), ('R', ('V', 1))], ('P', 'bool')), ('get_Item', [('V', 0)], ('V', 1)),
                                                 ('set_Item', [('V', 0), ('V', 1)], ('P', 'void'))],
    'System.Collections.Generic.IComparer`1': [('Compare', [('V', 0), ('V', 0)], ('P', 'i4'))],
    'System.IComparable': [('CompareTo', [_O], ('P', 'i4'))],
    'System.IComparable`1': [('CompareTo', [('V', 0)], ('P', 'i4'))],
    'System.IEquatable`1': [('Equals', [('V', 0)], ('P', 'bool'))],
    'System.Collections.Generic.IEqualityComparer`1': [('Equals', [('V', 0), ('V', 0)], ('P', 'bool')), ('GetHashCode', [('V', 0)], ('P', 'i4'))],
    'UnityEngine.EventSystems.IEventSystemHandler': [],
    'UnityEngine.EventSystems.IPointerEnterHandler': [('OnPointerEnter', [('N', 'UnityEngine.EventSystems.PointerEventData', False)], ('P', 'void'))],
    'UnityEngine.EventSystems.IPointerExitHandler': [('OnPointerExit', [('N', 'UnityEngine.EventSystems.PointerEventData', False)], ('P', 'void'))],
    'UnityEngine.EventSystems.IPointerClickHandler': [('OnPointerClick', [('N', 'UnityEngine.EventSystems.PointerEventData', False)], ('P', 'void'))],
    'UnityEngine.EventSystems.IPointerDownHandler': [('OnPointerDown', [('N', 'UnityEngine.EventSystems.PointerEventData', False)], ('P', 'void'))],
    'UnityEngine.EventSystems.IPointerUpHandler': [('OnPointerUp', [('N', 'UnityEngine.EventSystems.PointerEventData', False)], ('P', 'void'))],
    'UnityEngine.EventSystems.ISelectHandler': [('OnSelect', [('N', 'UnityEngine.EventSystems.BaseEventData', False)], ('P', 'void'))],
    'UnityEngine.EventSystems.IDeselectHandler': [('OnDeselect', [('N', 'UnityEngine.EventSystems.BaseEventData', False)], ('P', 'void'))],
    'UnityEngine.EventSystems.ISubmitHandler': [('OnSubmit', [('N', 'UnityEngine.EventSystems.BaseEventData', False)], ('P', 'void'))],
    'UnityEngine.EventSystems.ICancelHandler': [('OnCancel', [('N', 'UnityEngine.EventSystems.BaseEventData', False)], ('P', 'void'))],
    'UnityEngine.EventSystems.IMoveHandler': [('OnMove', [('N', 'UnityEngine.EventSystems.AxisEventData', False)], ('P', 'void'))],
    'UnityEngine.ISerializationCallbackReceiver': [('OnBeforeSerialize', [], ('P', 'void')), ('OnAfterDeserialize', [], ('P', 'void'))],
}
EXT_IFACE_PARENTS = {
    'System.Collections.Generic.ICollection`1': [('G', ('N', 'System.Collections.Generic.IEnumerable`1', False), (('V', 0),))],
    'System.Collections.Generic.IList`1': [('G', ('N', 'System.Collections.Generic.ICollection`1', False), (('V', 0),))],
    'System.Collections.Generic.IReadOnlyList`1': [('G', ('N', 'System.Collections.Generic.IReadOnlyCollection`1', False), (('V', 0),))],
    'System.Collections.Generic.IReadOnlyCollection`1': [('G', ('N', 'System.Collections.Generic.IEnumerable`1', False), (('V', 0),))],
    'System.Collections.IList': [('N', 'System.Collections.ICollection', False)],
    'System.Collections.ICollection': [('N', 'System.Collections.IEnumerable', False)],
    'System.Collections.Generic.IEnumerator`1': [('N', 'System.Collections.IEnumerator', False), ('N', 'System.IDisposable', False)],
    'System.Collections.Generic.IEnumerable`1': [('N', 'System.Collections.IEnumerable', False)],
    'UnityEngine.EventSystems.IPointerEnterHandler': [('N', 'UnityEngine.EventSystems.IEventSystemHandler', False)],
}


def prim_conv(e, f, t):
    if f == t:
        return e
    if f == 'boolean':
        e = f'(({e}) ? 1 : 0)'
        f = 'int'
        if t == 'int':
            return e
    if t == 'boolean':
        if f in ('float', 'double'):
            return f'(({e}) != 0)'
        if f == 'long':
            return f'(({e}) != 0L)'
        return f'(({e}) != 0)'
    if t == 'int' and f in ('char', 'short', 'byte'):
        return e
    if t == 'long' and f in ('int', 'char', 'short', 'byte'):
        return e
    if t in ('float', 'double') and f in ('int', 'char', 'short', 'byte', 'long'):
        return e if t == 'double' or f != 'long' else f'(float) ({e})'
    if t == 'double' and f == 'float':
        return e
    return f'(({t}) ({e}))'


def box_expr(x, e, jt, ct):
    """Box a java primitive (or enum) value as Object."""
    if ct is not None and x.ts.kind(ct) == 'enum':
        ecls = x.enum_class_expr(ct)
        return f'cs.rt.BoxedEnum.of({ecls}, {prim_conv(e, jt, "long")})'
    if ct is not None and ct[0] == 'P' and ct[1] in ('u1',) and jt == 'byte':
        return f'Byte.valueOf({e})'
    if ct is not None and ct[0] == 'P' and ct[1] in ('u2', 'char'):
        return f'Character.valueOf({prim_conv(e, jt, "char")})'
    if ct is not None and ct[0] == 'P' and ct[1] in PRIM_J and PRIM_J[ct[1]] in BOX_J:
        tj = PRIM_J[ct[1]]
        return f'{BOX_J[tj]}.valueOf({prim_conv(e, jt, tj)})'
    return f'{BOX_J.get(jt, "Integer")}.valueOf({e})'


def unbox_expr(e, to_jt):
    fn = {'int': 'unboxI', 'long': 'unboxJ', 'float': 'unboxF', 'double': 'unboxD', 'boolean': 'unboxZ',
          'char': 'unboxC', 'byte': 'unboxB', 'short': 'unboxS'}[to_jt]
    return f'cs.rt.Rt.{fn}({e})'




def main():
    args = sys.argv[1:]
    out = args[0]
    dlls, refs = [], []
    cur = dlls
    for a in args[1:]:
        if a == '--ref':
            cur = refs
            continue
        cur.append(a)
    w = World(dlls + refs, extra_value_types=set(EXT_ENUMS) | EXT_STRUCTS | EXT_NOCOPY)
    game = [m for m in w.modules if m.path in dlls]
    x = Xlat(w, game, out)
    n = x.run()
    x.write_support()
    if os.environ.get('STUBS'):
        from . import stubs
        k = stubs.gen(x, os.environ['STUBS'], os.environ.get('RTDIR', '/nonexistent'))
        print(f'{k} stub classes written to {os.environ["STUBS"]}')
    print(f'{n} classes written to {out}; {len(x.errors)} errors')
    with open(os.path.join(out, 'xlat_errors.txt'), 'w') as f:
        f.write('\n'.join(x.errors))
    with open(os.path.join(out, 'xlat_warnings.txt'), 'w') as f:
        for k, v in x.ts.warnings.most_common():
            f.write(f'{v:6d} {k}\n')
    with open(os.path.join(out, 'xlat_extrefs.txt'), 'w') as f:
        for c in sorted(x.ext_refs):
            f.write(c + '\n')
            for d in sorted(x.ext_refs[c]):
                f.write('    ' + d + '\n')


if __name__ == '__main__':
    main()
