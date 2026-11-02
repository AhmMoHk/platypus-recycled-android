"""CIL instruction decoding."""
import struct

# operand kinds: none i8 i32 i64 r4 r8 tok br8 br32 switch var8 var16 str sig
_ONE = {
    0x00: ('nop', None), 0x01: ('break', None),
    0x02: ('ldarg.0', None), 0x03: ('ldarg.1', None), 0x04: ('ldarg.2', None), 0x05: ('ldarg.3', None),
    0x06: ('ldloc.0', None), 0x07: ('ldloc.1', None), 0x08: ('ldloc.2', None), 0x09: ('ldloc.3', None),
    0x0a: ('stloc.0', None), 0x0b: ('stloc.1', None), 0x0c: ('stloc.2', None), 0x0d: ('stloc.3', None),
    0x0e: ('ldarg.s', 'u8'), 0x0f: ('ldarga.s', 'u8'), 0x10: ('starg.s', 'u8'),
    0x11: ('ldloc.s', 'u8'), 0x12: ('ldloca.s', 'u8'), 0x13: ('stloc.s', 'u8'),
    0x14: ('ldnull', None), 0x15: ('ldc.i4.m1', None),
    0x16: ('ldc.i4.0', None), 0x17: ('ldc.i4.1', None), 0x18: ('ldc.i4.2', None), 0x19: ('ldc.i4.3', None),
    0x1a: ('ldc.i4.4', None), 0x1b: ('ldc.i4.5', None), 0x1c: ('ldc.i4.6', None), 0x1d: ('ldc.i4.7', None),
    0x1e: ('ldc.i4.8', None), 0x1f: ('ldc.i4.s', 'i8'), 0x20: ('ldc.i4', 'i32'), 0x21: ('ldc.i8', 'i64'),
    0x22: ('ldc.r4', 'r4'), 0x23: ('ldc.r8', 'r8'), 0x25: ('dup', None), 0x26: ('pop', None),
    0x27: ('jmp', 'tok'), 0x28: ('call', 'tok'), 0x29: ('calli', 'tok'), 0x2a: ('ret', None),
    0x2b: ('br.s', 'br8'), 0x2c: ('brfalse.s', 'br8'), 0x2d: ('brtrue.s', 'br8'), 0x2e: ('beq.s', 'br8'),
    0x2f: ('bge.s', 'br8'), 0x30: ('bgt.s', 'br8'), 0x31: ('ble.s', 'br8'), 0x32: ('blt.s', 'br8'),
    0x33: ('bne.un.s', 'br8'), 0x34: ('bge.un.s', 'br8'), 0x35: ('bgt.un.s', 'br8'), 0x36: ('ble.un.s', 'br8'),
    0x37: ('blt.un.s', 'br8'),
    0x38: ('br', 'br32'), 0x39: ('brfalse', 'br32'), 0x3a: ('brtrue', 'br32'), 0x3b: ('beq', 'br32'),
    0x3c: ('bge', 'br32'), 0x3d: ('bgt', 'br32'), 0x3e: ('ble', 'br32'), 0x3f: ('blt', 'br32'),
    0x40: ('bne.un', 'br32'), 0x41: ('bge.un', 'br32'), 0x42: ('bgt.un', 'br32'), 0x43: ('ble.un', 'br32'),
    0x44: ('blt.un', 'br32'), 0x45: ('switch', 'switch'),
    0x46: ('ldind.i1', None), 0x47: ('ldind.u1', None), 0x48: ('ldind.i2', None), 0x49: ('ldind.u2', None),
    0x4a: ('ldind.i4', None), 0x4b: ('ldind.u4', None), 0x4c: ('ldind.i8', None), 0x4d: ('ldind.i', None),
    0x4e: ('ldind.r4', None), 0x4f: ('ldind.r8', None), 0x50: ('ldind.ref', None),
    0x51: ('stind.ref', None), 0x52: ('stind.i1', None), 0x53: ('stind.i2', None), 0x54: ('stind.i4', None),
    0x55: ('stind.i8', None), 0x56: ('stind.r4', None), 0x57: ('stind.r8', None),
    0x58: ('add', None), 0x59: ('sub', None), 0x5a: ('mul', None), 0x5b: ('div', None), 0x5c: ('div.un', None),
    0x5d: ('rem', None), 0x5e: ('rem.un', None), 0x5f: ('and', None), 0x60: ('or', None), 0x61: ('xor', None),
    0x62: ('shl', None), 0x63: ('shr', None), 0x64: ('shr.un', None), 0x65: ('neg', None), 0x66: ('not', None),
    0x67: ('conv.i1', None), 0x68: ('conv.i2', None), 0x69: ('conv.i4', None), 0x6a: ('conv.i8', None),
    0x6b: ('conv.r4', None), 0x6c: ('conv.r8', None), 0x6d: ('conv.u4', None), 0x6e: ('conv.u8', None),
    0x6f: ('callvirt', 'tok'), 0x70: ('cpobj', 'tok'), 0x71: ('ldobj', 'tok'), 0x72: ('ldstr', 'tok'),
    0x73: ('newobj', 'tok'), 0x74: ('castclass', 'tok'), 0x75: ('isinst', 'tok'), 0x76: ('conv.r.un', None),
    0x79: ('unbox', 'tok'), 0x7a: ('throw', None), 0x7b: ('ldfld', 'tok'), 0x7c: ('ldflda', 'tok'),
    0x7d: ('stfld', 'tok'), 0x7e: ('ldsfld', 'tok'), 0x7f: ('ldsflda', 'tok'), 0x80: ('stsfld', 'tok'),
    0x81: ('stobj', 'tok'), 0x82: ('conv.ovf.i1.un', None), 0x83: ('conv.ovf.i2.un', None),
    0x84: ('conv.ovf.i4.un', None), 0x85: ('conv.ovf.i8.un', None), 0x86: ('conv.ovf.u1.un', None),
    0x87: ('conv.ovf.u2.un', None), 0x88: ('conv.ovf.u4.un', None), 0x89: ('conv.ovf.u8.un', None),
    0x8a: ('conv.ovf.i.un', None), 0x8b: ('conv.ovf.u.un', None), 0x8c: ('box', 'tok'), 0x8d: ('newarr', 'tok'),
    0x8e: ('ldlen', None), 0x8f: ('ldelema', 'tok'), 0x90: ('ldelem.i1', None), 0x91: ('ldelem.u1', None),
    0x92: ('ldelem.i2', None), 0x93: ('ldelem.u2', None), 0x94: ('ldelem.i4', None), 0x95: ('ldelem.u4', None),
    0x96: ('ldelem.i8', None), 0x97: ('ldelem.i', None), 0x98: ('ldelem.r4', None), 0x99: ('ldelem.r8', None),
    0x9a: ('ldelem.ref', None), 0x9b: ('stelem.i', None), 0x9c: ('stelem.i1', None), 0x9d: ('stelem.i2', None),
    0x9e: ('stelem.i4', None), 0x9f: ('stelem.i8', None), 0xa0: ('stelem.r4', None), 0xa1: ('stelem.r8', None),
    0xa2: ('stelem.ref', None), 0xa3: ('ldelem', 'tok'), 0xa4: ('stelem', 'tok'), 0xa5: ('unbox.any', 'tok'),
    0xb3: ('conv.ovf.i1', None), 0xb4: ('conv.ovf.u1', None), 0xb5: ('conv.ovf.i2', None), 0xb6: ('conv.ovf.u2', None),
    0xb7: ('conv.ovf.i4', None), 0xb8: ('conv.ovf.u4', None), 0xb9: ('conv.ovf.i8', None), 0xba: ('conv.ovf.u8', None),
    0xc2: ('refanyval', 'tok'), 0xc3: ('ckfinite', None), 0xc6: ('mkrefany', 'tok'), 0xd0: ('ldtoken', 'tok'),
    0xd1: ('conv.u2', None), 0xd2: ('conv.u1', None), 0xd3: ('conv.i', None), 0xd4: ('conv.ovf.i', None),
    0xd5: ('conv.ovf.u', None), 0xd6: ('add.ovf', None), 0xd7: ('add.ovf.un', None), 0xd8: ('mul.ovf', None),
    0xd9: ('mul.ovf.un', None), 0xda: ('sub.ovf', None), 0xdb: ('sub.ovf.un', None), 0xdc: ('endfinally', None),
    0xdd: ('leave', 'br32'), 0xde: ('leave.s', 'br8'), 0xdf: ('stind.i', None), 0xe0: ('conv.u', None),
}
_TWO = {
    0x00: ('arglist', None), 0x01: ('ceq', None), 0x02: ('cgt', None), 0x03: ('cgt.un', None), 0x04: ('clt', None),
    0x05: ('clt.un', None), 0x06: ('ldftn', 'tok'), 0x07: ('ldvirtftn', 'tok'), 0x09: ('ldarg', 'u16'),
    0x0a: ('ldarga', 'u16'), 0x0b: ('starg', 'u16'), 0x0c: ('ldloc', 'u16'), 0x0d: ('ldloca', 'u16'),
    0x0e: ('stloc', 'u16'), 0x0f: ('localloc', None), 0x11: ('endfilter', None), 0x12: ('unaligned.', 'u8'),
    0x13: ('volatile.', None), 0x14: ('tail.', None), 0x15: ('initobj', 'tok'), 0x16: ('constrained.', 'tok'),
    0x17: ('cpblk', None), 0x18: ('initblk', None), 0x19: ('no.', 'u8'), 0x1a: ('rethrow', None),
    0x1c: ('sizeof', 'tok'), 0x1d: ('refanytype', None), 0x1e: ('readonly.', None),
}


class Ins:
    __slots__ = ('off', 'op', 'arg', 'next', 'prefix')

    def __init__(self, off, op, arg, nxt):
        self.off, self.op, self.arg, self.next = off, op, arg, nxt
        self.prefix = None

    def __repr__(self):
        a = '' if self.arg is None else (' ' + (hex(self.arg) if isinstance(self.arg, int) and self.op.endswith(('call', 'callvirt', 'newobj')) else repr(self.arg)))
        return f'IL_{self.off:04x}: {self.op}{a}'


def decode(code):
    out = []
    p = 0
    n = len(code)
    pending_prefix = None
    prefix_start = None
    while p < n:
        start = p
        b = code[p]
        p += 1
        if b == 0xfe:
            b2 = code[p]
            p += 1
            name, kind = _TWO[b2]
        else:
            name, kind = _ONE[b]
        arg = None
        if kind == 'u8':
            arg = code[p]; p += 1
        elif kind == 'i8':
            arg = struct.unpack_from('<b', code, p)[0]; p += 1
        elif kind == 'u16':
            arg = struct.unpack_from('<H', code, p)[0]; p += 2
        elif kind == 'i32':
            arg = struct.unpack_from('<i', code, p)[0]; p += 4
        elif kind == 'tok':
            arg = struct.unpack_from('<I', code, p)[0]; p += 4
        elif kind == 'i64':
            arg = struct.unpack_from('<q', code, p)[0]; p += 8
        elif kind == 'r4':
            arg = struct.unpack_from('<f', code, p)[0]
            raw = struct.unpack_from('<I', code, p)[0]
            arg = ('r4', arg, raw); p += 4
        elif kind == 'r8':
            arg = struct.unpack_from('<d', code, p)[0]
            raw = struct.unpack_from('<Q', code, p)[0]
            arg = ('r8', arg, raw); p += 8
        elif kind == 'br8':
            d = struct.unpack_from('<b', code, p)[0]; p += 1
            arg = p + d
        elif kind == 'br32':
            d = struct.unpack_from('<i', code, p)[0]; p += 4
            arg = p + d
        elif kind == 'switch':
            cnt = struct.unpack_from('<I', code, p)[0]; p += 4
            ds = struct.unpack_from('<%di' % cnt, code, p); p += 4 * cnt
            arg = [p + d for d in ds]
        if name.endswith('.') and name not in ('readonly.',) or name in ('readonly.',):
            # prefix: attach to the next instruction
            pending_prefix = (name, arg)
            if prefix_start is None:
                prefix_start = start
            continue
        ins = Ins(start if prefix_start is None else prefix_start, name, arg, p)
        prefix_start = None
        if pending_prefix is not None:
            ins.prefix = pending_prefix
            pending_prefix = None
        # normalise short forms
        if name.endswith('.s') and name not in ('ldc.i4.s',):
            ins.op = name[:-2]
        elif name.startswith('ldarg.') and name[6:].isdigit():
            ins.op, ins.arg = 'ldarg', int(name[6:])
        elif name.startswith('ldloc.') and name[6:].isdigit():
            ins.op, ins.arg = 'ldloc', int(name[6:])
        elif name.startswith('stloc.') and name[6:].isdigit():
            ins.op, ins.arg = 'stloc', int(name[6:])
        elif name.startswith('ldc.i4.') and name not in ('ldc.i4.s',):
            ins.op, ins.arg = 'ldc.i4', (-1 if name == 'ldc.i4.m1' else int(name[7:]))
        elif name == 'ldc.i4.s':
            ins.op = 'ldc.i4'
        out.append(ins)
    return out


BRANCHES = {'br', 'brfalse', 'brtrue', 'beq', 'bge', 'bgt', 'ble', 'blt', 'bne.un', 'bge.un', 'bgt.un', 'ble.un',
            'blt.un', 'leave'}
ENDS_BLOCK = BRANCHES | {'ret', 'throw', 'rethrow', 'endfinally', 'endfilter', 'switch', 'jmp'}
UNCOND = {'br', 'leave', 'ret', 'throw', 'rethrow', 'endfinally', 'endfilter', 'jmp'}
