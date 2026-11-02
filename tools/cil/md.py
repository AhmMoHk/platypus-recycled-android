"""
.NET metadata model for the IL -> Java translator.

Loads assemblies with dnfile and exposes: type definitions (fields, methods, nested types, interfaces, generic
parameters, custom attributes), decoded signatures, method bodies (instructions + exception clauses) and token
resolution (TypeRef/TypeDef/TypeSpec/MemberRef/MethodDef/MethodSpec/Field/UserString) across all loaded assemblies.

Types are plain tuples so they can be hashed / compared:
    ('P', name)                 primitive: void bool char i1 u1 i2 u2 i4 u4 i8 u8 r4 r8 i u string object typedref
    ('N', fullname, isvalue)    named type (fullname uses '/' for nesting, e.g. "DebugMenu/Element")
    ('G', ('N',...), (args..))  generic instantiation
    ('A', elem)                 single-dimension zero-based array
    ('M', elem, rank)           multi-dimensional array
    ('R', elem)                 managed pointer (byref)
    ('*', elem)                 unmanaged pointer
    ('V', n)                    type generic parameter !n
    ('W', n)                    method generic parameter !!n
"""
import struct
import dnfile

ELEM = {0x01: 'void', 0x02: 'bool', 0x03: 'char', 0x04: 'i1', 0x05: 'u1', 0x06: 'i2', 0x07: 'u2', 0x08: 'i4',
        0x09: 'u4', 0x0a: 'i8', 0x0b: 'u8', 0x0c: 'r4', 0x0d: 'r8', 0x0e: 'string', 0x16: 'typedref',
        0x18: 'i', 0x19: 'u', 0x1c: 'object'}
PRIM_BY_NAME = {'System.Void': 'void', 'System.Boolean': 'bool', 'System.Char': 'char', 'System.SByte': 'i1',
                'System.Byte': 'u1', 'System.Int16': 'i2', 'System.UInt16': 'u2', 'System.Int32': 'i4',
                'System.UInt32': 'u4', 'System.Int64': 'i8', 'System.UInt64': 'u8', 'System.Single': 'r4',
                'System.Double': 'r8', 'System.String': 'string', 'System.Object': 'object',
                'System.IntPtr': 'i', 'System.UIntPtr': 'u', 'System.TypedReference': 'typedref'}
NAME_BY_PRIM = {v: k for k, v in PRIM_BY_NAME.items()}

TABLE_NUM = {'Module': 0, 'TypeRef': 1, 'TypeDef': 2, 'Field': 4, 'MethodDef': 6, 'Param': 8, 'InterfaceImpl': 9,
             'MemberRef': 10, 'Constant': 11, 'CustomAttribute': 12, 'StandAloneSig': 17, 'Event': 20,
             'Property': 23, 'ModuleRef': 26, 'TypeSpec': 27, 'Assembly': 32, 'AssemblyRef': 35, 'File': 38,
             'ExportedType': 39, 'GenericParam': 42, 'MethodSpec': 43}


class Blob:
    __slots__ = ('b', 'p')

    def __init__(self, b, p=0):
        self.b = b
        self.p = p

    def u8(self):
        v = self.b[self.p]
        self.p += 1
        return v

    def cint(self):                       # compressed unsigned integer
        b0 = self.b[self.p]
        if b0 & 0x80 == 0:
            self.p += 1
            return b0
        if b0 & 0xc0 == 0x80:
            v = ((b0 & 0x3f) << 8) | self.b[self.p + 1]
            self.p += 2
            return v
        v = ((b0 & 0x1f) << 24) | (self.b[self.p + 1] << 16) | (self.b[self.p + 2] << 8) | self.b[self.p + 3]
        self.p += 4
        return v

    def peek(self):
        return self.b[self.p]

    def more(self):
        return self.p < len(self.b)


class MethodSig:
    __slots__ = ('hasthis', 'explicitthis', 'gen', 'ret', 'params', 'vararg')

    def __init__(self, hasthis, explicitthis, gen, ret, params, vararg=False):
        self.hasthis, self.explicitthis, self.gen, self.ret, self.params, self.vararg = \
            hasthis, explicitthis, gen, ret, params, vararg

    def key(self):
        return (self.hasthis, self.gen, self.ret, tuple(self.params))

    def __repr__(self):
        return f"{'inst ' if self.hasthis else ''}{tstr(self.ret)}({', '.join(tstr(p) for p in self.params)})"


def tstr(t):
    k = t[0]
    if k == 'P':
        return t[1]
    if k == 'N':
        return t[1]
    if k == 'G':
        return tstr(t[1]) + '<' + ','.join(tstr(a) for a in t[2]) + '>'
    if k == 'A':
        return tstr(t[1]) + '[]'
    if k == 'M':
        return tstr(t[1]) + '[' + ',' * (t[2] - 1) + ']'
    if k == 'R':
        return tstr(t[1]) + '&'
    if k == '*':
        return tstr(t[1]) + '*'
    if k == 'V':
        return '!' + str(t[1])
    if k == 'W':
        return '!!' + str(t[1])
    return repr(t)


def subst(t, targs, margs):
    """Substitute generic parameters in type t."""
    k = t[0]
    if k == 'V':
        return targs[t[1]] if targs is not None and t[1] < len(targs) else t
    if k == 'W':
        return margs[t[1]] if margs is not None and t[1] < len(margs) else t
    if k == 'G':
        return ('G', t[1], tuple(subst(a, targs, margs) for a in t[2]))
    if k in ('A', 'R', '*'):
        return (k, subst(t[1], targs, margs))
    if k == 'M':
        return ('M', subst(t[1], targs, margs), t[2])
    return t


class FieldDef:
    def __init__(self, owner, row_index, name, flags, ftype):
        self.owner = owner
        self.row = row_index
        self.name = name
        self.flags = flags
        self.type = ftype
        self.static = bool(flags & 0x10)
        self.literal = bool(flags & 0x40)
        self.initonly = bool(flags & 0x20)
        self.rva = None
        self.const = None              # (elemtype, value) for literal fields
        self.attrs = []
        self.public = (flags & 7) == 6

    def __repr__(self):
        return f"Field({self.owner.fullname}::{self.name}:{tstr(self.type)})"


class MethodDef:
    def __init__(self, owner, row_index, name, flags, implflags, sig, rva):
        self.owner = owner
        self.row = row_index
        self.name = name
        self.flags = flags
        self.implflags = implflags
        self.sig = sig
        self.rva = rva
        self.static = bool(flags & 0x10)
        self.virtual = bool(flags & 0x40)
        self.abstract = bool(flags & 0x400)
        self.newslot = bool(flags & 0x100)
        self.final = bool(flags & 0x20)
        self.paramnames = []
        self.gparams = []              # names of method generic parameters
        self.overrides = []            # explicit MethodImpl declarations (method references)
        self.attrs = []
        self._body = None

    @property
    def body(self):
        if self._body is None and self.rva:
            self._body = self.owner.module.read_body(self.rva)
        return self._body

    def __repr__(self):
        return f"Method({self.owner.fullname}::{self.name} {self.sig})"


class TypeDef:
    def __init__(self, module, row_index, ns, name, flags):
        self.module = module
        self.row = row_index
        self.ns = ns
        self.name = name
        self.flags = flags
        self.enclosing = None
        self.nested = []
        self.extends = None            # type tuple
        self.interfaces = []           # type tuples
        self.fields = []
        self.methods = []
        self.gparams = []
        self.attrs = []
        self.layout_size = None
        self.properties = []          # PropertyDef

    @property
    def fullname(self):
        if self.enclosing is not None:
            return self.enclosing.fullname + '/' + self.name
        return (self.ns + '.' + self.name) if self.ns else self.name

    @property
    def interface(self):
        return bool(self.flags & 0x20)

    @property
    def abstract(self):
        return bool(self.flags & 0x80)

    @property
    def sealed(self):
        return bool(self.flags & 0x100)

    def base_name(self):
        if self.extends is None:
            r = self.module.table_rows('TypeDef')[self.row - 1]
            if r.Extends is None or r.Extends.table is None or not r.Extends.row_index:
                return None
            tn, idx = Module.coded(r.Extends)
            if tn == 'TypeRef':
                return self.module.typeref_name(idx)
            if tn == 'TypeDef':
                return self.module.typedefs[idx].fullname
            return None
        e = self.extends
        if e[0] == 'G':
            e = e[1]
        return e[1] if e[0] == 'N' else NAME_BY_PRIM.get(e[1])

    @property
    def is_enum(self):
        return self.base_name() == 'System.Enum'

    @property
    def is_value(self):
        b = self.base_name()
        return b == 'System.Enum' or (b == 'System.ValueType' and self.fullname != 'System.Enum')

    @property
    def is_delegate(self):
        return self.base_name() == 'System.MulticastDelegate'

    def enum_underlying(self):
        for f in self.fields:
            if not f.static:
                return f.type
        return ('P', 'i4')

    def __repr__(self):
        return f"TypeDef({self.fullname})"


class PropertyDef:
    def __init__(self, owner, name, ptype, flags):
        self.owner, self.name, self.type, self.flags = owner, name, ptype, flags
        self.getter = None
        self.setter = None
        self.attrs = []


class MemberRef:
    """A reference to a field or method of some (possibly external, possibly generic-instantiated) type."""
    __slots__ = ('parent', 'name', 'sig', 'isfield', 'margs', 'resolved')

    def __init__(self, parent, name, sig, isfield, margs=None):
        self.parent = parent         # type tuple ('N'/'G'/'A'...), or None for module-level
        self.name = name
        self.sig = sig               # MethodSig or field type
        self.isfield = isfield
        self.margs = margs           # generic method instantiation (tuple of types) or None
        self.resolved = None         # MethodDef/FieldDef if the target is defined in a loaded assembly

    def __repr__(self):
        return f"Ref({tstr(self.parent) if self.parent else '?'}::{self.name} {self.sig if not self.isfield else tstr(self.sig)}" \
               f"{'<' + ','.join(tstr(a) for a in self.margs) + '>' if self.margs else ''})"


class EH:
    __slots__ = ('kind', 'try_start', 'try_end', 'h_start', 'h_end', 'catch_type', 'filter_start')

    def __init__(self, kind, ts, tl, hs, hl, ct, fs):
        self.kind = kind             # 'catch' 'filter' 'finally' 'fault'
        self.try_start, self.try_end = ts, ts + tl
        self.h_start, self.h_end = hs, hs + hl
        self.catch_type = ct
        self.filter_start = fs

    def __repr__(self):
        return f"EH({self.kind} try {self.try_start:x}-{self.try_end:x} h {self.h_start:x}-{self.h_end:x} {self.catch_type})"


class Body:
    def __init__(self, maxstack, localsig_tok, init_locals, code, ehs):
        self.maxstack = maxstack
        self.localsig_tok = localsig_tok
        self.init_locals = init_locals
        self.code = code
        self.ehs = ehs
        self.locals = []


class Module:
    def __init__(self, path, world):
        self.path = path
        self.world = world
        self.pe = dnfile.dnPE(path)
        self.md = self.pe.net.mdtables
        self.data = self.pe.__data__
        self.name = str(self.md.Assembly.rows[0].Name) if self.md.Assembly and self.md.Assembly.rows else path
        self.types = []
        self._typespec_cache = {}
        self._memberref_cache = {}
        self._load_types()

    # ------------------------------------------------------------------ helpers
    def blob(self, item):
        if item is None:
            return b''
        v = item.value if hasattr(item, 'value') else item
        return bytes(v) if v is not None else b''

    def user_string(self, off):
        us = self.pe.net.user_strings
        s = us.get(off)
        if s is None:
            return ''
        if hasattr(s, 'value'):
            s = s.value
        if isinstance(s, (bytes, bytearray)):
            s = s.decode('utf-16-le', 'surrogatepass')
        return s

    def table_rows(self, name):
        t = getattr(self.md, name, None)
        return t.rows if t is not None and t.rows else []

    @staticmethod
    def coded(ci):
        """(table name, 1-based row index) of a dnfile coded index"""
        if ci is None or ci.table is None:
            return (None, 0)
        return (ci.table.name, ci.row_index)

    # ------------------------------------------------------------------ type loading
    def _load_types(self):
        rows = self.table_rows('TypeDef')
        fields = self.table_rows('Field')
        methods = self.table_rows('MethodDef')
        params = self.table_rows('Param')
        self.typedefs = [None]
        self.fielddefs = [None] * (len(fields) + 1)
        self.methoddefs = [None] * (len(methods) + 1)
        nt = len(rows)
        for i, r in enumerate(rows, 1):
            td = TypeDef(self, i, str(r.TypeNamespace or ''), str(r.TypeName or ''), r.struct.Flags)
            self.typedefs.append(td)
        # field / method ranges
        def start_of(lst):
            return lst[0].row_index if lst else None
        fstarts, mstarts = [], []
        for r in rows:
            fstarts.append(start_of(r.FieldList))
            mstarts.append(start_of(r.MethodList))
        def ranges(starts, total):
            out = []
            n = len(starts)
            for i in range(n):
                s = starts[i]
                if s is None:
                    out.append((0, 0))
                    continue
                e = total + 1
                for j in range(i + 1, n):
                    if starts[j] is not None:
                        e = starts[j]
                        break
                out.append((s, e))
            return out
        fr = ranges(fstarts, len(fields))
        mr = ranges(mstarts, len(methods))
        # params: ranges per method
        pstarts = [start_of(m.ParamList) for m in methods]
        pr = ranges(pstarts, len(params)) if methods else []
        for i, r in enumerate(rows):
            td = self.typedefs[i + 1]
            for fi in range(*fr[i]):
                f = fields[fi - 1]
                fd = FieldDef(td, fi, str(f.Name), f.struct.Flags, None)
                fd._sigblob = self.blob(f.Signature)
                td.fields.append(fd)
                self.fielddefs[fi] = fd
            for mi in range(*mr[i]):
                m = methods[mi - 1]
                md = MethodDef(td, mi, str(m.Name), m.struct.Flags, m.struct.ImplFlags, None, m.Rva)
                md._sigblob = self.blob(m.Signature)
                s, e = pr[mi - 1] if pr else (0, 0)
                names = {}
                for pi in range(s, e):
                    p = params[pi - 1]
                    names[p.Sequence] = str(p.Name)
                md._pnames = names
                td.methods.append(md)
                self.methoddefs[mi] = md
        for r in self.table_rows('NestedClass'):
            inner = self.typedefs[r.NestedClass.row_index]
            outer = self.typedefs[r.EnclosingClass.row_index]
            inner.enclosing = outer
            outer.nested.append(inner)
        for r in self.table_rows('ClassLayout'):
            self.typedefs[r.Parent.row_index].layout_size = r.ClassSize
        for r in self.table_rows('FieldRva'):
            self.fielddefs[r.Field.row_index].rva = r.Rva
        for r in self.table_rows('GenericParam'):
            tn, idx = self.coded(r.Owner)
            if tn == 'TypeDef':
                o = self.typedefs[idx].gparams
            else:
                o = self.methoddefs[idx].gparams
            while len(o) <= r.Number:
                o.append(None)
            o[r.Number] = str(r.Name)
        self.types = self.typedefs[1:]

    @staticmethod
    def _flagval(f):
        if isinstance(f, int):
            return f
        v = getattr(f, 'value', None)
        if isinstance(v, int):
            return v
        # dnfile flag objects: rebuild the integer from the struct value
        return int(f)

    def _flags(self, r):
        return self._flagval(r.Flags)

    def finish(self):
        """Second phase (after all modules are loaded): decode signatures, base types, interfaces, constants."""
        rows = self.table_rows('TypeDef')
        for i, r in enumerate(rows, 1):
            td = self.typedefs[i]
            if r.Extends is not None and r.Extends.table is not None and r.Extends.row_index:
                td.extends = self.type_from_coded(r.Extends)
        for i, r in enumerate(rows, 1):
            td = self.typedefs[i]
            for f in td.fields:
                f.type = self.field_sig(f._sigblob)
            for m in td.methods:
                m.sig = self.method_sig(m._sigblob)
                m.paramnames = [m._pnames.get(k + 1, f'p{k}') for k in range(len(m.sig.params))]
    def finish2(self):
        for r in self.table_rows('InterfaceImpl'):
            td = self.typedefs[r.Class.row_index]
            td.interfaces.append(self.type_from_coded(r.Interface))
        for r in self.table_rows('MethodImpl'):
            body = self.method_token(r.MethodBody)
            decl = self.method_token(r.MethodDeclaration)
            if isinstance(body, MethodDef):
                body.overrides.append(decl)
        # properties
        prop_owner = {}
        pmaps = self.table_rows('PropertyMap')
        props = self.table_rows('Property')
        starts = [(r.Parent.row_index, r.PropertyList[0].row_index if r.PropertyList else None) for r in pmaps]
        for k, (tidx, st) in enumerate(starts):
            if st is None:
                continue
            en = len(props) + 1
            for j in range(k + 1, len(starts)):
                if starts[j][1] is not None:
                    en = starts[j][1]
                    break
            for pi in range(st, en):
                prop_owner[pi] = self.typedefs[tidx]
        self.propdefs = [None] * (len(props) + 1)
        for pi, r in enumerate(props, 1):
            td = prop_owner.get(pi)
            if td is None:
                continue
            bl = Blob(self.blob(r.Type))
            bl.u8()
            n = bl.cint()
            try:
                ptype = self.read_type(bl)
            except Exception:
                ptype = ('P', 'object')
            pd = PropertyDef(td, str(r.Name), ptype, r.struct.Flags)
            td.properties.append(pd)
            self.propdefs[pi] = pd
        for r in self.table_rows('MethodSemantics'):
            tn, idx = self.coded(r.Association)
            if tn != 'Property':
                continue
            pd = self.propdefs[idx]
            m = self.methoddefs[r.Method.row_index]
            sem = r.struct.Semantics
            if pd is None:
                continue
            if sem & 2:
                pd.getter = m
            elif sem & 1:
                pd.setter = m
        for r in self.table_rows('Constant'):
            tn, idx = self.coded(r.Parent)
            if tn == 'Field':
                fd = self.fielddefs[idx]
                fd.const = (r.Type, self.const_value(r.Type, self.blob(r.Value)))
        for r in self.table_rows('CustomAttribute'):
            tn, idx = self.coded(r.Parent)
            ctor = self.method_token(r.Type)
            val = self.blob(r.Value)
            target = None
            if tn == 'TypeDef':
                target = self.typedefs[idx]
            elif tn == 'MethodDef':
                target = self.methoddefs[idx]
            elif tn == 'Field':
                target = self.fielddefs[idx]
            elif tn == 'Property':
                target = self.propdefs[idx]
            if target is not None:
                target.attrs.append((ctor, val))

    @staticmethod
    def const_value(t, b):
        if t == 0x02:
            return b[0] != 0
        if t == 0x03:
            return struct.unpack('<H', b)[0]
        fmt = {0x04: '<b', 0x05: '<B', 0x06: '<h', 0x07: '<H', 0x08: '<i', 0x09: '<I', 0x0a: '<q', 0x0b: '<Q',
               0x0c: '<f', 0x0d: '<d'}
        if t in fmt:
            return struct.unpack(fmt[t], b)[0]
        if t == 0x0e:
            return b.decode('utf-16-le', 'surrogatepass')
        if t == 0x12:
            return None
        return b

    # ------------------------------------------------------------------ custom attributes
    def decode_attr(self, ctor_sig, blob, enum_size):
        """(fixed args, named args [(kind 'F'/'P', name, type, value)]) or None if undecodable"""
        bl = Blob(blob)
        if len(blob) < 2 or blob[0] != 1 or blob[1] != 0:
            return None
        bl.p = 2

        def ser_string():
            if bl.b[bl.p] == 0xff:
                bl.p += 1
                return None
            n = bl.cint()
            v = bytes(bl.b[bl.p:bl.p + n]).decode('utf-8', 'replace')
            bl.p += n
            return v

        def elem(t):
            k = t[0]
            if k == 'P':
                code = t[1]
                fmt = {'bool': '<?', 'char': '<H', 'i1': '<b', 'u1': '<B', 'i2': '<h', 'u2': '<H', 'i4': '<i',
                       'u4': '<I', 'i8': '<q', 'u8': '<Q', 'r4': '<f', 'r8': '<d'}
                if code in fmt:
                    v = struct.unpack_from(fmt[code], bl.b, bl.p)[0]
                    bl.p += struct.calcsize(fmt[code])
                    return ('P', code, v)
                if code == 'string':
                    return ('P', 'string', ser_string())
                if code == 'object':
                    tb = bl.u8()
                    return elem(boxed_type(tb))
            if k == 'N' and t[1] == 'System.Type':
                return ('T', ser_string())
            if k == 'N':
                size = enum_size(t[1])
                fmt = {1: '<b', 2: '<h', 4: '<i', 8: '<q'}[size]
                v = struct.unpack_from(fmt, bl.b, bl.p)[0]
                bl.p += size
                return ('E', t[1], v)
            if k == 'A':
                n = struct.unpack_from('<I', bl.b, bl.p)[0]
                bl.p += 4
                if n == 0xffffffff:
                    return ('A', t[1], None)
                return ('A', t[1], [elem(t[1]) for _ in range(n)])
            raise ValueError('attr elem ' + repr(t))

        def boxed_type(tb):
            if tb in ELEM:
                return ('P', ELEM[tb])
            if tb == 0x50:
                return ('N', 'System.Type', False)
            if tb == 0x51:
                return ('P', 'object')
            if tb == 0x55:
                return ('N', ser_string(), True)
            if tb == 0x1d:
                return ('A', boxed_type(bl.u8()))
            raise ValueError('boxed type %x' % tb)

        try:
            fixed = [elem(p) for p in ctor_sig.params]
            named = []
            if bl.p + 2 <= len(bl.b):
                n = struct.unpack_from('<H', bl.b, bl.p)[0]
                bl.p += 2
                for _ in range(n):
                    kind = 'F' if bl.u8() == 0x53 else 'P'
                    t = boxed_type(bl.u8())
                    name = ser_string()
                    named.append((kind, name, t, elem(t)))
            return fixed, named
        except Exception:
            return None

    # ------------------------------------------------------------------ signatures
    def type_from_coded(self, ci):
        tn, idx = self.coded(ci)
        return self.type_token(TABLE_NUM[tn] << 24 | idx)

    def typedef_type(self, td):
        return ('N', td.fullname, td.is_value) if td.fullname not in PRIM_BY_NAME else ('P', PRIM_BY_NAME[td.fullname])

    def typeref_name(self, idx):
        r = self.table_rows('TypeRef')[idx - 1]
        name = str(r.TypeName)
        ns = str(r.TypeNamespace or '')
        tn, ridx = self.coded(r.ResolutionScope)
        if tn == 'TypeRef':
            return self.typeref_name(ridx) + '/' + name
        return (ns + '.' + name) if ns else name

    def type_token(self, tok, isvalue=None):
        table = tok >> 24
        idx = tok & 0xffffff
        if table == 2:
            td = self.typedefs[idx]
            return self.typedef_type(td)
        if table == 1:
            name = self.typeref_name(idx)
            if name in PRIM_BY_NAME:
                return ('P', PRIM_BY_NAME[name])
            if isvalue is None:
                isvalue = self.world.is_value_name(name)
            return ('N', name, isvalue)
        if table == 27:
            if idx not in self._typespec_cache:
                b = self.blob(self.table_rows('TypeSpec')[idx - 1].Signature)
                self._typespec_cache[idx] = self.read_type(Blob(b))
            return self._typespec_cache[idx]
        raise ValueError(f'type token {tok:08x}')

    def read_type(self, bl):
        e = bl.u8()
        if e in ELEM:
            return ('P', ELEM[e])
        if e == 0x1f or e == 0x20:           # cmod_reqd / cmod_opt
            bl.cint()
            return self.read_type(bl)
        if e == 0x45:                        # pinned
            return self.read_type(bl)
        if e == 0x41:                        # sentinel
            return self.read_type(bl)
        if e in (0x11, 0x12):                # valuetype / class
            tok = self.decode_tdor(bl.cint())
            t = self.type_token(tok, isvalue=(e == 0x11))
            if t[0] == 'N' and t[2] != (e == 0x11):
                t = ('N', t[1], e == 0x11)
            return t
        if e == 0x15:                        # genericinst
            k = bl.u8()
            tok = self.decode_tdor(bl.cint())
            base = self.type_token(tok, isvalue=(k == 0x11))
            if base[0] == 'N':
                base = ('N', base[1], k == 0x11)
            n = bl.cint()
            args = tuple(self.read_type(bl) for _ in range(n))
            return ('G', base, args)
        if e == 0x1d:
            return ('A', self.read_type(bl))
        if e == 0x14:                        # array
            el = self.read_type(bl)
            rank = bl.cint()
            ns = bl.cint()
            for _ in range(ns):
                bl.cint()
            nl = bl.cint()
            for _ in range(nl):
                bl.cint()
            return ('M', el, rank)
        if e == 0x10:
            return ('R', self.read_type(bl))
        if e == 0x0f:
            return ('*', self.read_type(bl))
        if e == 0x13:
            return ('V', bl.cint())
        if e == 0x1e:
            return ('W', bl.cint())
        if e == 0x1b:                        # fnptr
            self.read_method_sig(bl)
            return ('P', 'i')
        raise ValueError(f'elem type {e:#x}')

    @staticmethod
    def decode_tdor(v):
        tag = v & 3
        idx = v >> 2
        return ({0: 2, 1: 1, 2: 27}[tag] << 24) | idx

    def read_method_sig(self, bl):
        cc = bl.u8()
        hasthis = bool(cc & 0x20)
        explicit = bool(cc & 0x40)
        gen = 0
        if cc & 0x10:
            gen = bl.cint()
        n = bl.cint()
        ret = self.read_type(bl)
        params = []
        vararg = False
        for _ in range(n):
            if bl.more() and bl.peek() == 0x41:
                bl.u8()
                vararg = True
            params.append(self.read_type(bl))
        return MethodSig(hasthis, explicit, gen, ret, params, vararg)

    def method_sig(self, b):
        return self.read_method_sig(Blob(b))

    def field_sig(self, b):
        bl = Blob(b)
        assert bl.u8() == 0x06
        return self.read_type(bl)

    def local_sig(self, tok):
        if not tok:
            return []
        b = self.blob(self.table_rows('StandAloneSig')[(tok & 0xffffff) - 1].Signature)
        bl = Blob(b)
        assert bl.u8() == 0x07
        n = bl.cint()
        return [self.read_type(bl) for _ in range(n)]

    # ------------------------------------------------------------------ token resolution
    def method_token(self, ci_or_tok):
        if not isinstance(ci_or_tok, int):
            tn, idx = self.coded(ci_or_tok)
            tok = (TABLE_NUM[tn] << 24) | idx
        else:
            tok = ci_or_tok
        return self.member_token(tok)

    def member_token(self, tok):
        """MethodDef / FieldDef for local definitions, MemberRef for references and MethodSpecs."""
        table = tok >> 24
        idx = tok & 0xffffff
        if table == 6:
            return self.methoddefs[idx]
        if table == 4:
            return self.fielddefs[idx]
        if table == 10:
            if idx in self._memberref_cache:
                return self._memberref_cache[idx]
            r = self.table_rows('MemberRef')[idx - 1]
            tn, pidx = self.coded(r.Class)
            parent = None
            if tn in ('TypeRef', 'TypeDef', 'TypeSpec'):
                parent = self.type_token(TABLE_NUM[tn] << 24 | pidx)
            elif tn == 'MethodDef':
                return self.methoddefs[pidx]
            b = self.blob(r.Signature)
            if b[0] == 0x06:
                mr = MemberRef(parent, str(r.Name), self.field_sig(b), True)
            else:
                mr = MemberRef(parent, str(r.Name), self.method_sig(b), False)
            self.world.resolve_ref(mr)
            self._memberref_cache[idx] = mr
            return mr
        if table == 43:
            r = self.table_rows('MethodSpec')[idx - 1]
            m = self.method_token(r.Method)
            bl = Blob(self.blob(r.Instantiation))
            assert bl.u8() == 0x0a
            n = bl.cint()
            args = tuple(self.read_type(bl) for _ in range(n))
            if isinstance(m, MethodDef):
                mr = MemberRef(self.typedef_type(m.owner) if not m.owner.gparams else
                               ('G', self.typedef_type(m.owner), tuple(('V', i) for i in range(len(m.owner.gparams)))),
                               m.name, m.sig, False, args)
                mr.resolved = m
                return mr
            mr2 = MemberRef(m.parent, m.name, m.sig, False, args)
            mr2.resolved = m.resolved
            return mr2
        raise ValueError(f'member token {tok:08x}')

    def field_token(self, tok):
        return self.member_token(tok)

    # ------------------------------------------------------------------ method bodies
    def read_body(self, rva):
        off = self.pe.get_offset_from_rva(rva)
        d = self.data
        h = d[off]
        if h & 3 == 2:                                   # tiny
            size = h >> 2
            code = bytes(d[off + 1: off + 1 + size])
            return Body(8, 0, False, code, [])
        flags = struct.unpack_from('<H', d, off)[0]
        hsize = (flags >> 12) * 4
        maxstack = struct.unpack_from('<H', d, off + 2)[0]
        codesize = struct.unpack_from('<I', d, off + 4)[0]
        ltok = struct.unpack_from('<I', d, off + 8)[0]
        code = bytes(d[off + hsize: off + hsize + codesize])
        ehs = []
        if flags & 0x8:                                  # more sections
            p = off + hsize + codesize
            p = (p + 3) & ~3
            while True:
                kind = d[p]
                if kind & 0x40:                          # fat
                    n = (struct.unpack_from('<I', d, p)[0] >> 8) & 0xffffff
                    cnt = (n - 4) // 24
                    for i in range(cnt):
                        fl, ts, tl, hs, hl, ct = struct.unpack_from('<IIIIII', d, p + 4 + i * 24)
                        ehs.append(self._eh(fl, ts, tl, hs, hl, ct))
                    p += n
                else:
                    n = d[p + 1]
                    cnt = (n - 4) // 12
                    for i in range(cnt):
                        fl, ts, tl, hs, hl, ct = struct.unpack_from('<HHBHBI', d, p + 4 + i * 12)
                        ehs.append(self._eh(fl, ts, tl, hs, hl, ct))
                    p += n
                if not (kind & 0x80):
                    break
                p = (p + 3) & ~3
        b = Body(maxstack, ltok, bool(flags & 0x10), code, ehs)
        b.locals = self.local_sig(ltok)
        return b

    def _eh(self, fl, ts, tl, hs, hl, ct):
        kind = {0: 'catch', 1: 'filter', 2: 'finally', 4: 'fault'}[fl & 7]
        catch_type = self.type_token(ct) if kind == 'catch' else None
        return EH(kind, ts, tl, hs, hl, catch_type, ct if kind == 'filter' else None)


class World:
    """All loaded assemblies; resolves names across them."""

    def __init__(self, paths, extra_value_types=()):
        self.modules = []
        self.types = {}                  # fullname -> TypeDef
        self.extra_value_types = set(extra_value_types)
        for p in paths:
            m = Module(p, self)
            self.modules.append(m)
            for t in m.types:
                if t.fullname in self.types and t.enclosing is None:
                    # same name in two assemblies (compiler generated helpers): make it unique
                    t.name = t.name + '_' + m.name.replace('.', '_')
                self.types.setdefault(t.fullname, t)
        for m in self.modules:
            m.finish()
        for m in self.modules:
            m.finish2()

    def is_value_name(self, name):
        t = self.types.get(name)
        if t is not None:
            return t.is_value
        return name in self.extra_value_types

    def resolve_ref(self, mr):
        p = mr.parent
        if p is None:
            return
        if p[0] == 'G':
            p = p[1]
        if p[0] != 'N':
            return
        td = self.types.get(p[1])
        if td is None:
            return
        if mr.isfield:
            for f in td.fields:
                if f.name == mr.name:
                    mr.resolved = f
                    return
        else:
            cands = [m for m in td.methods if m.name == mr.name and len(m.sig.params) == len(mr.sig.params)
                     and m.sig.hasthis == mr.sig.hasthis and m.sig.gen == mr.sig.gen]
            if len(cands) == 1:
                mr.resolved = cands[0]
                return
            for m in cands:
                if m.sig.params == mr.sig.params and m.sig.ret == mr.sig.ret:
                    mr.resolved = m
                    return
            if cands:
                # signatures in refs use the same encoding; fall back to param tuple compare loosely
                for m in cands:
                    if [tstr(a) for a in m.sig.params] == [tstr(a) for a in mr.sig.params]:
                        mr.resolved = m
                        return

    def typedef(self, t):
        if t is None:
            return None
        if t[0] == 'G':
            t = t[1]
        if t[0] == 'N':
            return self.types.get(t[1])
        if t[0] == 'P' and t[1] in NAME_BY_PRIM:
            return self.types.get(NAME_BY_PRIM[t[1]])
        return None
