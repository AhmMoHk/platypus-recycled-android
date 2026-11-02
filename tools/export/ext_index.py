"""Index external (reference-only) assemblies: kind, base type, interfaces, enum underlying type and constants.
    python3 -m tools.ext_index <out.json> <dll>...   (run from re/)"""
import json, sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from cil.md import World

out = sys.argv[1]
w = World(sys.argv[2:])
idx = {}
def tname(t):
    if t is None: return None
    if t[0] == 'G': t = t[1]
    return t[1] if t[0] == 'N' else None
for name, td in w.types.items():
    if not (name.startswith('UnityEngine') or name.startswith('Unity.') or name.startswith('Rewired') or name.startswith('Blackdrop') or name.startswith('Steamworks')):
        continue
    if td.is_enum: kind = 'enum'
    elif td.is_delegate: kind = 'delegate'
    elif td.is_value: kind = 'struct'
    elif td.interface: kind = 'iface'
    else: kind = 'class'
    e = {'k': kind, 'base': tname(td.extends), 'ifaces': [tname(i) for i in td.interfaces if tname(i)], 'gp': len(td.gparams)}
    if kind == 'enum':
        ut = 'i4'; names = []; vals = []
        for f in td.fields:
            if not f.static and f.type[0] == 'P': ut = f.type[1]
            elif f.literal and f.const is not None:
                names.append(f.name); v = f.const[1]
                vals.append(int(v) if not isinstance(v, bool) else int(v))
        e['ut'] = ut; e['names'] = names; e['vals'] = vals
        e['flags'] = any(a for a in td.attrs if 'FlagsAttribute' in str(getattr(a, 'ctor', a)))
    if kind == 'delegate':
        inv = [m for m in td.methods if m.name == 'Invoke']
        if inv:
            e['sig'] = [inv[0].sig.ret, list(inv[0].sig.params)]
    idx[name] = e
json.dump(idx, open(out, 'w'), indent=0, separators=(',', ':'))
print(len(idx), 'types;', sum(1 for v in idx.values() if v['k'] == 'enum'), 'enums')
