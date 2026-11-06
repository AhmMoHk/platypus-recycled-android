#!/usr/bin/env python3
"""Minimal DXBC (shader model 4/5) disassembler: RDEF names, signatures and SHEX instructions, with constant
buffer reads annotated by variable name. Enough to read the game's small custom shaders.

    python3 dxbc_dis.py file.dxbc [...]
"""
import struct
import sys

OPS = ("add and break breakc call callc case continue continuec cut default deriv_rtx deriv_rty discard div dp2 dp3 "
       "dp4 else emit emitthencut endif endloop endswitch eq exp frc ftoi ftou ge iadd if ieq ige ilt imad imax imin imul "
       "ine ineg ishl ishr itof label ld ld_ms log loop lt mad min max customdata mov movc mul ne nop not or resinfo ret "
       "retc round_ne round_ni round_pi round_z rsq sample sample_c sample_c_lz sample_l sample_d sample_b sqrt switch "
       "sincos udiv ult uge umul umad umax umin ushr utof xor dcl_resource dcl_constantbuffer dcl_sampler "
       "dcl_index_range dcl_gs_output_topology dcl_gs_input_primitive dcl_max_output_vertex_count dcl_input "
       "dcl_input_sgv dcl_input_siv dcl_input_ps dcl_input_ps_sgv dcl_input_ps_siv dcl_output dcl_output_sgv "
       "dcl_output_siv dcl_temps dcl_indexable_temp dcl_global_flags reserved lod gather4 sample_pos sample_info").split()
COND = {3, 5, 8, 13, 31, 63}             # ops whose bit 18 selects _nz / _z
REG = {0: 'r', 1: 'v', 2: 'o', 3: 'x', 6: 's', 7: 't', 8: 'cb', 9: 'icb', 0x0C: 'oDepth', 0x0D: 'null'}
CH = 'xyzw'


def cstr(b, o):
    return b[o:b.index(b'\0', o)].decode('latin1')


class Shader:
    def __init__(self, data):
        assert data[:4] == b'DXBC'
        n = struct.unpack_from('<I', data, 28)[0]
        self.chunks = {}
        for i in range(n):
            off = struct.unpack_from('<I', data, 32 + 4 * i)[0]
            tag = data[off:off + 4].decode()
            size = struct.unpack_from('<I', data, off + 4)[0]
            self.chunks[tag] = data[off + 8:off + 8 + size]
        self.cbvars = {}     # (cb slot, byte offset) -> name
        self.cbnames = []
        self.resources = []
        if 'RDEF' in self.chunks:
            self.rdef(self.chunks['RDEF'])

    def rdef(self, b):
        ncb, cboff, nres, resoff = struct.unpack_from('<IIII', b, 0)
        ver = struct.unpack_from('<I', b, 16)[0]
        major = (ver >> 4) & 0xF
        stride = 40 if major >= 5 else 24
        slots = {}
        for i in range(nres):
            nm, typ, rt, dim, ns, bp, bc, fl = struct.unpack_from('<8I', b, resoff + 32 * i)
            name = cstr(b, nm)
            self.resources.append((name, typ, bp))
            if typ == 0:                   # cbuffer: its bind point
                slots[name] = bp
        for i in range(ncb):
            nm, nv, voff, size, fl, typ = struct.unpack_from('<6I', b, cboff + 24 * i)
            name = cstr(b, nm)
            slot = slots.get(name, i)
            self.cbnames.append((slot, name, size))
            for v in range(nv):
                vn, start, vsize, vflags, vtype, vdef = struct.unpack_from('<6I', b, voff + stride * v)
                vname = cstr(b, vn)
                for k in range(0, max(4, vsize), 4):
                    self.cbvars[(slot, start + k)] = (vname, k)

    def sig(self, tag):
        b = self.chunks.get(tag)
        if b is None:
            return []
        n = struct.unpack_from('<I', b, 0)[0]
        out = []
        for i in range(n):
            nm, idx, sv, ct, reg, mask = struct.unpack_from('<IIIIIB', b, 8 + 24 * i)
            out.append(f'{cstr(b, nm)}{idx or ""} -> {"o" if tag == "OSGN" else "v"}{reg}.{"".join(CH[j] for j in range(4) if mask >> j & 1)}')
        return out

    # ----------------------------------------------------------------- operands
    def operand(self, t, p):
        tok = t[p]; p += 1
        ncomp = tok & 3
        sel = (tok >> 2) & 3
        typ = (tok >> 12) & 0xFF
        dim = (tok >> 20) & 3
        reps = [(tok >> (22 + 3 * i)) & 7 for i in range(dim)]
        mod = ''
        if tok >> 31:
            ext = t[p]; p += 1
            m = (ext >> 6) & 0xFF
            mod = {1: '-', 2: '|', 3: '-|'}.get(m, '')
        comps = ''
        if ncomp == 2:
            if sel == 0:
                comps = '.' + ''.join(CH[j] for j in range(4) if tok >> (4 + j) & 1)
            elif sel == 1:
                comps = '.' + ''.join(CH[(tok >> (4 + 2 * j)) & 3] for j in range(4))
            else:
                comps = '.' + CH[(tok >> 4) & 3]
        if typ == 4:                      # immediate
            n = 1 if ncomp == 1 else 4
            vals = t[p:p + n]; p += n
            fs = []
            for v in vals:
                f = struct.unpack('<f', struct.pack('<I', v))[0]
                fs.append(str(v) if (v < 0x00800000 and v != 0) else f'{f:.6g}')
            return f'{mod}l({", ".join(fs)})', p
        idx = []
        for r in reps:
            if r == 0:
                idx.append(str(t[p])); p += 1
            elif r == 2:
                s, p = self.operand(t, p); idx.append(s)
            elif r == 3:
                base = t[p]; p += 1
                s, p = self.operand(t, p); idx.append(f'{s} + {base}')
            else:
                idx.append(f'imm64({t[p]})'); p += 2
        name = REG.get(typ, f'reg{typ}')
        if typ == 8 and len(idx) == 2 and idx[1].isdigit():
            s = f'cb{idx[0]}[{idx[1]}]{comps}'
            names = []
            for c in (comps[1:] if comps else 'xyzw'):
                v = self.cbvars.get((int(idx[0]), int(idx[1]) * 16 + CH.index(c) * 4))
                if v:
                    names.append(v[0] + ('' if v[1] == 0 else f'+{v[1] // 4}'))
            if names:
                uniq = []
                for n_ in names:
                    if n_ not in uniq:
                        uniq.append(n_)
                s += ' /*' + ','.join(uniq) + '*/'
            out = s
        elif len(idx) == 1:
            out = f'{name}{idx[0]}{comps}'
        elif len(idx) == 0:
            out = name + comps
        else:
            out = f'{name}{idx[0]}' + ''.join(f'[{i}]' for i in idx[1:]) + comps
        if mod in ('|', '-|'):
            return f'{mod}{out}|', p
        return f'{mod}{out}', p

    def dis(self):
        b = self.chunks.get('SHEX') or self.chunks.get('SHDR')
        t = struct.unpack(f'<{len(b) // 4}I', b)
        ver = t[0]
        kind = {0: 'ps', 1: 'vs', 2: 'gs'}.get(ver >> 16, '?')
        lines = [f'{kind}_{(ver >> 4) & 0xF}_{ver & 0xF}']
        p = 2
        depth = 0
        while p < len(t):
            tok = t[p]
            op = tok & 0x7FF
            ln = (tok >> 24) & 0x7F
            if op == 53:                   # customdata (immediate constant buffer)
                ln = t[p + 1]
                lines.append('  ' * depth + f'customdata ({ln} dwords)')
                p += ln
                continue
            end = p + ln
            q = p + 1
            ext = tok >> 31
            while ext:
                ext = t[q] >> 31; q += 1
            name = OPS[op] if op < len(OPS) else f'op{op}'
            if op in COND:
                name += '_nz' if tok >> 18 & 1 else '_z'
            if tok >> 13 & 1 and op < 88:
                name += '_sat'
            if op in (18, 21, 22):          # else endif endloop
                depth = max(0, depth - 1)
            if name.startswith('dcl_'):
                if op == 89:
                    s, _ = self.operand(t, q)
                    lines.append(f'{name} {s}')
                elif op == 104:
                    lines.append(f'{name} {t[q]}')
                else:
                    try:
                        s, _ = self.operand(t, q)
                    except Exception:
                        s = ''
                    lines.append(f'{name} {s}')
                p = end
                continue
            ops = []
            try:
                while q < end:
                    s, q = self.operand(t, q)
                    ops.append(s)
            except Exception:
                ops.append('?')
            lines.append('  ' * depth + name + (' ' + ', '.join(ops) if ops else ''))
            if op in (31, 48, 18):          # if loop else
                depth += 1
            p = end
        return lines


def main():
    for f in sys.argv[1:]:
        s = Shader(open(f, 'rb').read())
        print(f'==== {f}')
        for slot, name, size in s.cbnames:
            vars_ = sorted({(o, v[0]) for (sl, o), v in s.cbvars.items() if sl == slot and v[1] == 0})
            print(f'// cbuffer {name} (cb{slot}, {size} bytes): ' + ', '.join(f'{n}@{o}' for o, n in vars_))
        for r in s.resources:
            if r[1] != 0:
                print(f'// resource {r[0]} type {r[1]} slot {r[2]}')
        for x in s.sig('ISGN'):
            print('// in  ' + x)
        for x in s.sig('OSGN'):
            print('// out ' + x)
        print('\n'.join(s.dis()))


if __name__ == '__main__':
    main()
