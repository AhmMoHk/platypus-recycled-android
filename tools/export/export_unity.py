"""
Export the Unity player data (scenes, shared assets, Resources) of Platypus Reclayed into a simple JSON + image
format read by the Java runtime.

    python3 export_unity.py <Platypus_Data dir> <out dir>

out/unity/files/<file>.json : {"objects": {pathID: {"t": type, "d": data}}}   (PPtrs -> ["file", pathID])
out/unity/tex/<file>_<pathID>.png : decoded textures (UI / small textures; game sprites come from the mod bundles)
out/unity/fonts/<file>_<pathID>.ttf : font data
out/unity/meta.json : scenes, Resources paths, scripts (class, assembly, execution order), tags/layers, physics2D,
                      quality/player/time settings
"""
import json
import os
import sys

import UnityPy
from UnityPy.helpers.TypeTreeGenerator import TypeTreeGenerator

D = os.path.abspath(sys.argv[1])
OUT = os.path.abspath(sys.argv[2])
os.makedirs(os.path.join(OUT, 'unity', 'files'), exist_ok=True)
os.makedirs(os.path.join(OUT, 'unity', 'tex'), exist_ok=True)
os.makedirs(os.path.join(OUT, 'unity', 'fonts'), exist_ok=True)

files = sorted(f for f in os.listdir(D) if (f.startswith('level') or f.startswith('sharedassets') or f.startswith('globalgamemanagers')
                                            or f == 'resources.assets') and not f.endswith('.resS') and not f.endswith('.resource'))
os.chdir(D)
env = UnityPy.load(*files)
gen = TypeTreeGenerator("2022.3.62f2")
gen.load_local_dll_folder(os.path.abspath("Managed"))

SKIP_TYPES = {'MonoScript', 'PreloadData', 'NavMeshSettings', 'LightmapSettings', 'Cubemap', 'VideoClip'}
script_info = {}           # (file, pathID) -> (assembly, fullname, execOrder)


def norm_file(n):
    n = os.path.basename(n)
    return 'unity default resources' if 'unity default resources' in n else n


def clean(v, sf):
    if isinstance(v, dict):
        if set(v.keys()) == {'m_FileID', 'm_PathID'}:
            if v['m_PathID'] == 0:
                return None
            fid = v['m_FileID']
            if fid == 0:
                f = norm_file(sf.name)
            else:
                ext = sf.externals
                f = norm_file(ext[fid - 1].path) if fid - 1 < len(ext) else f'ext#{fid}'
            return [f, v['m_PathID']]
        return {k: clean(x, sf) for k, x in v.items()}
    if isinstance(v, (list, tuple)):
        return [clean(x, sf) for x in v]
    if isinstance(v, (bytes, bytearray)):
        if len(v) <= 4 * 1024 * 1024:
            import base64
            return {'_b64': base64.b64encode(bytes(v)).decode('ascii')}
        return {'_bytes': len(v)}
    if isinstance(v, float):
        if v != v:
            return 'NaN'
        if v in (float('inf'), float('-inf')):
            return 'Inf' if v > 0 else '-Inf'
    return v


def script_of(o):
    base = o.read(check_read=False)
    pp = base.m_Script
    key = None
    try:
        sc = pp.read()
        full = (sc.m_Namespace + '.' if sc.m_Namespace else '') + sc.m_ClassName
        return sc.m_AssemblyName, full, sc.m_ExecutionOrder
    except Exception:
        return None, None, 0


def export_texture(o, d, fname, pid):
    path = os.path.join(OUT, 'unity', 'tex', f'{fname}_{pid}.png')
    if os.path.exists(path):
        return
    try:
        t = o.read()
        img = t.image
        img.save(path)
    except Exception as e:
        print('texture failed', fname, pid, d.get('m_Name'), repr(e)[:100])


def export_font(o, d, fname, pid):
    try:
        f = o.read()
        data = getattr(f, 'm_FontData', None)
        if data:
            b = bytes(data) if not isinstance(data, bytes) else data
            with open(os.path.join(OUT, 'unity', 'fonts', f'{fname}_{pid}.ttf'), 'wb') as fp:
                fp.write(b)
    except Exception as e:
        print('font failed', fname, pid, repr(e)[:100])


meta = {'scenes': [], 'resources': {}, 'scripts': {}}
stats = {}
for name, sf in list(env.files.items()):
    fname = norm_file(name)
    objs = {}
    for pid, o in sf.objects.items():
        t = o.type.name
        if t in SKIP_TYPES and t != 'MonoScript':
            continue
        try:
            if t == 'MonoScript':
                sc = o.read()
                full = (sc.m_Namespace + '.' if sc.m_Namespace else '') + sc.m_ClassName
                meta['scripts'][f'{fname}#{pid}'] = [sc.m_AssemblyName, full, sc.m_ExecutionOrder]
                continue
            if t == 'MonoBehaviour':
                asm, full, order = script_of(o)
                d = None
                if full is not None:
                    try:
                        nodes = gen.get_nodes_up(asm, full)
                        d = o.read_typetree(nodes=nodes, check_read=False)
                    except Exception as e:
                        d = {'_error': repr(e)[:200]}
                else:
                    d = {'_error': 'no script'}
                d = clean(d, sf)
                d['_script'] = full
                d['_asm'] = asm
            else:
                d = clean(o.read_typetree(), sf)
            if t == 'Texture2D':
                export_texture(o, d, fname, pid)
                d.pop('image data', None)
                d['_png'] = f'tex/{fname}_{pid}.png'
                d.pop('m_StreamData', None)
            elif t == 'Font':
                export_font(o, d, fname, pid)
                d.pop('m_FontData', None)
            elif t == 'Shader':
                pf = d.get('m_ParsedForm', {})
                d = {'m_Name': pf.get('m_Name'), 'props': [p.get('m_Name') for p in pf.get('m_PropInfo', {}).get('m_Props', [])]}
            elif t == 'AudioClip':
                d.pop('m_Resource', None)
            objs[str(pid)] = {'t': t, 'd': d}
        except Exception as e:
            objs[str(pid)] = {'t': t, 'err': repr(e)[:200]}
    stats[fname] = len(objs)
    with open(os.path.join(OUT, 'unity', 'files', fname + '.json'), 'w') as f:
        json.dump({'file': fname, 'externals': [norm_file(e.path) for e in sf.externals], 'objects': objs}, f, separators=(',', ':'))

# global managers
gg = [v for k, v in list(env.files.items()) if os.path.basename(k) == 'globalgamemanagers'][0]
for pid, o in gg.objects.items():
    t = o.type.name
    try:
        d = clean(o.read_typetree(), gg)
    except Exception as e:
        continue
    if t == 'BuildSettings':
        meta['scenes'] = d.get('scenes', [])
    elif t == 'ResourceManager':
        meta['resources'] = {k: v for k, v in d.get('m_Container', [])}
    elif t in ('TagManager', 'TimeManager', 'QualitySettings', 'Physics2DSettings', 'PlayerSettings', 'AudioManager',
               'GraphicsSettings', 'InputManager', 'MonoManager', 'ScriptMapper'):
        meta[t] = d
with open(os.path.join(OUT, 'unity', 'meta.json'), 'w') as f:
    json.dump(meta, f, indent=1)
print(json.dumps(stats))
print('resources', len(meta['resources']), 'scripts', len(meta['scripts']))
