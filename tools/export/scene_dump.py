"""Dump a Unity scene/assets file: GameObjects, hierarchy and components with their serialized data (JSON)."""
import UnityPy, os, sys, json
from UnityPy.helpers.TypeTreeGenerator import TypeTreeGenerator

D = sys.argv[1]
which = sys.argv[2]
out = sys.argv[3] if len(sys.argv) > 3 else None
files = [f for f in os.listdir(D) if (f.startswith('level') or f.startswith('sharedassets') or f.startswith('globalgamemanagers') or f == 'resources.assets') and not f.endswith('.resS') and not f.endswith('.resource')]
os.chdir(D)
env = UnityPy.load(*files)
gen = TypeTreeGenerator("2022.3.62f2")
gen.load_local_dll_folder(os.path.abspath("Managed"))
sf = [v for k, v in env.files.items() if os.path.basename(k) == which][0]
ext = [e.path for e in sf.externals]

def ref(p):
    if isinstance(p, dict) and 'm_PathID' in p:
        fid = p['m_FileID']
        if p['m_PathID'] == 0:
            return None
        return {'file': (which if fid == 0 else (ext[fid - 1] if fid - 1 < len(ext) else f'ext#{fid}')), 'id': p['m_PathID']}
    return p

def clean(v):
    if isinstance(v, dict):
        if set(v.keys()) == {'m_FileID', 'm_PathID'}:
            return ref(v)
        return {k: clean(x) for k, x in v.items()}
    if isinstance(v, list):
        if len(v) > 200 and all(isinstance(x, (int, float)) for x in v):
            return {'_array': len(v)}
        return [clean(x) for x in v]
    if isinstance(v, (bytes, bytearray)):
        return {'_bytes': len(v)}
    return v

objs = {}
errs = 0
for pid, o in sf.objects.items():
    t = o.type.name
    try:
        if t == 'MonoBehaviour':
            base = o.read(check_read=False)
            sc = base.m_Script.read()
            asm = sc.m_AssemblyName
            full = (sc.m_Namespace + '.' if sc.m_Namespace else '') + sc.m_ClassName
            try:
                nodes = gen.get_nodes_up(asm, full)
                d = o.read_typetree(nodes=nodes, check_read=False)
            except Exception as e:
                d = {'_error': repr(e)[:200]}
                errs += 1
            d['_script'] = full
            d['_asm'] = asm
        else:
            d = o.read_typetree()
        objs[pid] = {'type': t, 'data': clean(d)}
    except Exception as e:
        objs[pid] = {'type': t, 'error': repr(e)[:200]}
        errs += 1

result = {'file': which, 'externals': ext, 'objects': {str(k): v for k, v in objs.items()}}
if out:
    with open(out, 'w') as f:
        json.dump(result, f, indent=1, default=str)
print('objects', len(objs), 'errors', errs)
