"""
Convert your own copy of Platypus Reclayed into the data pack used by the Android port.

    python3 convert_assets.py <Platypus_Data dir> <out dir> [--astcenc PATH] [--quality thorough] [--jobs N]
                              [--only "Mod Name"] [--skip-scenes]

Output (copy the whole <out dir> to the phone with the in-app "Choose folder" import):
    unity/...                      scenes, prefabs, UI, fonts, settings (export_unity.py)
    StreamingAssets/<mod>/...      mod data (json, .plat, lua, loose GFX/SFX) copied as-is
    StreamingAssets/<mod>/Bundle/HighEnd/<bundle>        JSON marker read by the engine's AssetBundle
    StreamingAssets/<mod>/Bundle/HighEnd/<bundle>.d/     files/<cab>.json, tex/<pid>.astc (+ .a8z alpha), snd/<pid>.ogg
    Workshop/<id>/...              Steam Workshop items (if --workshop DIR is given): listed as normal mods

Textures: the HighEnd (4K) RGBA32 bundle textures are encoded to ASTC 4x4 (8 bits/pixel, -thorough), which keeps
4K detail and uploads straight to the GPU (no CPU decode, low memory). Alpha masks for pixel-perfect colliders
are stored deflated. LowEnd bundles are not converted (the port always uses HighEnd).
Audio: FSB5/Vorbis clips are decoded with FMOD (no sound device needed) and re-encoded to Ogg Vorbis q5 (transparent at the source bitrate).
"""
import argparse
import base64
import concurrent.futures as cf
import json
import os
import shutil
import subprocess
import sys
import tempfile
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))


# ---------------------------------------------------------------- FMOD without a sound card
def _patch_fmod():
    import fmod_toolkit.fmod as F
    from threading import Lock
    pf = F.pyfmodex

    def inst(channels, flags):
        with F.SYSTEM_GLOBAL_LOCK:
            k = (channels, flags)
            if k not in F.SYSTEM_INSTANCES:
                s = pf.System()
                s.output = pf.enums.OUTPUTTYPE.NOSOUND_NRT
                s.init(channels, flags, None)
                F.SYSTEM_INSTANCES[k] = (s, Lock())
            return F.SYSTEM_INSTANCES[k]
    F.get_pyfmodex_system_instance = inst


def clean(v, sf, norm):
    """PPtrs -> [file, pathID]; bytes -> base64; NaN/Inf -> strings (same format as export_unity.py)"""
    if isinstance(v, dict):
        if set(v.keys()) == {'m_FileID', 'm_PathID'}:
            if v['m_PathID'] == 0:
                return None
            fid = v['m_FileID']
            if fid == 0:
                f = norm(sf.name)
            else:
                ext = sf.externals
                f = norm(ext[fid - 1].path) if fid - 1 < len(ext) else f'ext#{fid}'
            return [f, v['m_PathID']]
        return {k: clean(x, sf, norm) for k, x in v.items()}
    if isinstance(v, (list, tuple)):
        return [clean(x, sf, norm) for x in v]
    if isinstance(v, (bytes, bytearray)):
        if len(v) <= 4 * 1024 * 1024:
            return {'_b64': base64.b64encode(bytes(v)).decode('ascii')}
        return {'_bytes': len(v)}
    if isinstance(v, float):
        if v != v:
            return 'NaN'
        if v in (float('inf'), float('-inf')):
            return 'Inf' if v > 0 else '-Inf'
    return v


def norm_name(n):
    n = os.path.basename(n)
    if 'unity default resources' in n:
        return 'unity default resources'
    return n.split('/')[-1]


# ---------------------------------------------------------------- textures
def encode_texture(args):
    """(png_path, astc_path, astcenc, quality) -> error or None"""
    png, astc, enc, quality = args
    try:
        r = subprocess.run([enc, '-cl', png, astc, '4x4', '-' + quality, '-silent'], capture_output=True, timeout=3600)
        if r.returncode != 0:
            return f'astcenc failed for {png}: {r.stderr.decode(errors="replace")[:300]}'
        # strip the 16 byte .astc header: the engine uploads raw blocks
        with open(astc, 'rb') as f:
            data = f.read()
        with open(astc, 'wb') as f:
            f.write(data[16:])
        return None
    finally:
        try:
            os.remove(png)
        except OSError:
            pass


def convert_bundle(path, out_marker, rel_base, gen, astcenc, quality, jobs, log):
    """convert one AssetBundle file; out_marker is the marker path written at the bundle's place"""
    import UnityPy
    from PIL import Image
    env = UnityPy.load(path)
    name = os.path.basename(out_marker)
    ddir = out_marker + '.d'
    for sub in ('files', 'tex', 'snd'):
        os.makedirs(os.path.join(ddir, sub), exist_ok=True)
    rel_d = rel_base + '/' + name + '.d'
    files = []
    tex_jobs = []
    tmp = tempfile.mkdtemp(prefix='prr-tex-')
    def serialized(files):
        for fname, f in list(files.items()):
            if hasattr(f, 'objects'):
                yield fname, f
            elif hasattr(f, 'files'):
                yield from serialized(f.files)
    for fname, sf in serialized(env.files):
        cab = norm_name(fname)
        if cab.endswith('.resS') or cab.endswith('.resource'):
            continue
        files.append(cab)
        objs = {}
        for pid, o in sf.objects.items():
            t = o.type.name
            if t in ('MonoScript', 'PreloadData'):
                continue
            try:
                if t == 'MonoBehaviour':
                    base = o.read(check_read=False)
                    try:
                        sc = base.m_Script.read()
                        asm, full = sc.m_AssemblyName, (sc.m_Namespace + '.' if sc.m_Namespace else '') + sc.m_ClassName
                        d = o.read_typetree(nodes=gen.get_nodes_up(asm, full), check_read=False)
                    except Exception as e:
                        d, asm, full = {'_error': repr(e)[:200]}, None, None
                    d = clean(d, sf, norm_name)
                    d['_script'] = full
                    d['_asm'] = asm
                elif t == 'Texture2D':
                    tx = o.read()
                    d = {'m_Name': tx.m_Name, 'm_Width': tx.m_Width, 'm_Height': tx.m_Height, 'm_TextureFormat': 4,
                         'm_MipCount': 1, 'm_IsReadable': bool(getattr(tx, 'm_IsReadable', False)),
                         'm_TextureSettings': clean(o.read_typetree().get('m_TextureSettings', {}), sf, norm_name)}
                    if tx.m_Width > 0 and tx.m_Height > 0:
                        img = tx.image.convert('RGBA').transpose(Image.FLIP_TOP_BOTTOM)   # GL row 0 = bottom
                        png = os.path.join(tmp, f'{pid}.png')
                        img.save(png, compress_level=1)
                        tex_jobs.append((png, os.path.join(ddir, 'tex', f'{pid}.astc'), astcenc, quality))
                        d['_astc'] = f'{rel_d}/tex/{pid}.astc'
                        d['_astcFormat'] = 0x93B0
                        alpha = img.getchannel('A').tobytes()
                        with open(os.path.join(ddir, 'tex', f'{pid}.a8z'), 'wb') as f:
                            f.write(zlib.compress(alpha, 6))
                        d['_alpha'] = f'{rel_d}/tex/{pid}.a8z'
                elif t == 'AudioClip':
                    ac = o.read()
                    d = clean(o.read_typetree(), sf, norm_name)
                    d.pop('m_Resource', None)
                    try:
                        samples = ac.samples
                        wav = next(iter(samples.values()))
                        ogg = os.path.join(ddir, 'snd', f'{pid}.ogg')
                        r = subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-f', 'wav', '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', ogg],
                                           input=wav, capture_output=True, timeout=600)
                        if r.returncode == 0:
                            d['_ogg'] = f'{rel_d}/snd/{pid}.ogg'
                        else:
                            log(f'ffmpeg failed {ac.m_Name}: {r.stderr[:200]}')
                    except Exception as e:
                        log(f'audio failed {ac.m_Name}: {e!r}'[:300])
                else:
                    d = clean(o.read_typetree(), sf, norm_name)
                objs[str(pid)] = {'t': t, 'd': d}
            except Exception as e:
                objs[str(pid)] = {'t': t, 'err': repr(e)[:200]}
        with open(os.path.join(ddir, 'files', cab + '.json'), 'w') as f:
            json.dump({'file': cab, 'externals': [norm_name(e.path) for e in sf.externals], 'objects': objs}, f, separators=(',', ':'))
    # container: asset path -> list of [file, pathID] (from the AssetBundle object's m_Container)
    container = {}
    for cab in files:
        with open(os.path.join(ddir, 'files', cab + '.json')) as f:
            fo = json.load(f)['objects']
        for o in fo.values():
            if o.get('t') != 'AssetBundle':
                continue
            for k, v in o['d'].get('m_Container', []):
                asset = v.get('asset') if isinstance(v, dict) else None
                if isinstance(asset, list):
                    container.setdefault(k.lower(), []).append(asset)
    with open(out_marker, 'w') as f:
        json.dump({'prrBundle': 1, 'dir': name + '.d', 'files': files, 'container': container}, f, separators=(',', ':'))
    log(f'  {name}: {len(files)} files, {len(tex_jobs)} textures to encode')
    errs = 0
    with cf.ThreadPoolExecutor(max_workers=jobs) as ex:
        for i, err in enumerate(ex.map(encode_texture, tex_jobs)):
            if err:
                errs += 1
                log(err)
            if (i + 1) % 50 == 0:
                log(f'    encoded {i + 1}/{len(tex_jobs)}')
    shutil.rmtree(tmp, ignore_errors=True)
    return errs


def copy_tree(src, dst, skip_bundle=True):
    for root, dirs, fs in os.walk(src):
        rel = os.path.relpath(root, src)
        parts = rel.split(os.sep)
        if skip_bundle and parts and parts[0] == 'Bundle':
            continue
        os.makedirs(os.path.join(dst, rel), exist_ok=True)
        for fn in fs:
            s, t = os.path.join(root, fn), os.path.join(dst, rel, fn)
            if not os.path.exists(t) or os.path.getsize(t) != os.path.getsize(s):
                shutil.copy2(s, t)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('data', help='Platypus_Data directory of your installed game')
    ap.add_argument('out', help='output data pack directory')
    ap.add_argument('--astcenc', default=shutil.which('astcenc-avx2') or shutil.which('astcenc') or 'astcenc')
    ap.add_argument('--quality', default='thorough', choices=['fast', 'medium', 'thorough', 'exhaustive'])
    ap.add_argument('--jobs', type=int, default=os.cpu_count() or 2)
    ap.add_argument('--only', action='append', help='convert only these mods')
    ap.add_argument('--skip-scenes', action='store_true')
    ap.add_argument('--workshop', help='Steam workshop content folder (steamapps/workshop/content/<appid>)')
    a = ap.parse_args()
    data, out = os.path.abspath(a.data), os.path.abspath(a.out)
    os.makedirs(out, exist_ok=True)

    def log(m):
        print(m, flush=True)

    if not a.skip_scenes:
        log('exporting scenes and global assets ...')
        subprocess.run([sys.executable, os.path.join(HERE, 'export_unity.py'), data, out], check=True)
        shutil.copy2(os.path.join(data, 'RuntimeInitializeOnLoads.json'), os.path.join(out, 'unity', 'RuntimeInitializeOnLoads.json'))

    _patch_fmod()
    from UnityPy.helpers.TypeTreeGenerator import TypeTreeGenerator
    gen = TypeTreeGenerator('2022.3.62f2')
    gen.load_local_dll_folder(os.path.join(data, 'Managed'))

    sa, osa = os.path.join(data, 'StreamingAssets'), os.path.join(out, 'StreamingAssets')
    os.makedirs(osa, exist_ok=True)
    total_err = 0
    for item in sorted(os.listdir(sa)):
        src = os.path.join(sa, item)
        if os.path.isfile(src):
            shutil.copy2(src, os.path.join(osa, item))
            continue
        if a.only and item not in a.only:
            continue
        log(f'mod: {item}')
        copy_tree(src, os.path.join(osa, item))
        he = os.path.join(src, 'Bundle', 'HighEnd')
        if os.path.isdir(he):
            ohe = os.path.join(osa, item, 'Bundle', 'HighEnd')
            os.makedirs(ohe, exist_ok=True)
            for b in sorted(os.listdir(he)):
                bp = os.path.join(he, b)
                if b.endswith('.manifest') or b == 'HighEnd' or os.path.isdir(bp):
                    continue
                total_err += convert_bundle(bp, os.path.join(ohe, b), f'StreamingAssets/{item}/Bundle/HighEnd', gen, a.astcenc, a.quality, a.jobs, log)
    if a.workshop and os.path.isdir(a.workshop):
        ow = os.path.join(out, 'Workshop')
        for item in sorted(os.listdir(a.workshop)):
            if os.path.isdir(os.path.join(a.workshop, item)):
                log(f'workshop item: {item}')
                copy_tree(os.path.join(a.workshop, item), os.path.join(ow, item), skip_bundle=False)
    log(f'done ({total_err} texture errors). Copy "{out}" to the phone and pick it in the app.')


if __name__ == '__main__':
    main()
