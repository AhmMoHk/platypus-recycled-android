"""
Convert your own copy of Platypus Reclayed into the data pack used by the Android port.

    python3 convert_assets.py <Platypus_Data dir> <out dir> [--astcenc PATH] [--quality thorough] [--jobs N]
                              [--only "Mod Name"] [--skip-scenes]

Output (copy the whole <out dir> to the phone with the in-app "Choose folder" import):
    unity/...                      scenes, prefabs, UI, fonts, settings (export_unity.py)
    StreamingAssets/<mod>/...      mod data (json, .plat, lua, loose GFX/SFX) copied as-is
    StreamingAssets/<mod>/Bundle/HighEnd/<bundle>        JSON marker read by the engine's AssetBundle
    StreamingAssets/<mod>/Bundle/HighEnd/<bundle>.d/     files/<cab>.json, tex/<pid>.astcz (deflated ASTC blocks, + .a8z alpha), snd/<pid>.ogg
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



# ---------------------------------------------------------------- streaming bundle extraction (low memory)
def _cstr(f):
    b = bytearray()
    while True:
        c = f.read(1)
        if not c or c == b'\0':
            return b.decode('utf-8', 'replace')
        b += c


def extract_bundle_nodes(path, outdir):
    """Decompress a UnityFS bundle block by block into its node files (CAB, CAB.resS, ...) on disk.
    Returns {node name: file path}. Peak memory is one block (UnityPy would hold the whole bundle twice)."""
    import struct
    from UnityPy.helpers import CompressionHelper as C
    os.makedirs(outdir, exist_ok=True)
    with open(path, 'rb') as f:
        sig = _cstr(f)
        if sig != 'UnityFS':
            raise ValueError('not a UnityFS bundle: ' + sig)
        ver = struct.unpack('>I', f.read(4))[0]
        _cstr(f); _cstr(f)
        size, csize, usize, flags = struct.unpack('>qIII', f.read(20))
        if ver >= 7:
            f.seek((f.tell() + 15) & ~15)
        if flags & 0x80:
            here = f.tell()
            f.seek(size - csize)
            info = f.read(csize)
            f.seek(here)
        else:
            info = f.read(csize)
        comp = flags & 0x3F
        info = decompress(C, info, usize, comp)
        o = 16
        nblocks = struct.unpack_from('>i', info, o)[0]; o += 4
        blocks = []
        for _ in range(nblocks):
            u, c, bf = struct.unpack_from('>IIH', info, o); o += 10
            blocks.append((u, c, bf))
        nnodes = struct.unpack_from('>i', info, o)[0]; o += 4
        nodes = []
        for _ in range(nnodes):
            off, sz, nf = struct.unpack_from('>qqI', info, o); o += 20
            e = info.index(b'\0', o)
            nodes.append((off, sz, info[o:e].decode()))
            o = e + 1
        if flags & 0x200:
            f.seek((f.tell() + 15) & ~15)
        stream = os.path.join(outdir, '_stream.bin')
        with open(stream, 'wb') as out:
            for u, c, bf in blocks:
                out.write(decompress(C, f.read(c), u, bf & 0x3F))
    res = {}
    with open(stream, 'rb') as src:
        for off, sz, name in nodes:
            dst = os.path.join(outdir, os.path.basename(name))
            src.seek(off)
            with open(dst, 'wb') as out:
                left = sz
                while left > 0:
                    chunk = src.read(min(left, 64 << 20))
                    out.write(chunk)
                    left -= len(chunk)
            res[os.path.basename(name)] = dst
    os.remove(stream)
    return res


def decompress(C, data, usize, comp):
    if comp == 0:
        return data
    if comp in (2, 3):
        import lz4.block
        return lz4.block.decompress(data, uncompressed_size=usize)
    if comp == 1:
        return C.decompress_lzma(data)
    raise ValueError(f'unsupported compression {comp}')


def read_node(nodes, source, offset, size):
    """bytes of a resource stream reference ('archive:/CAB-x/CAB-x.resS')"""
    p = nodes.get(os.path.basename(source))
    if p is None:
        return None
    with open(p, 'rb') as f:
        f.seek(offset)
        return f.read(size)


def raw_texture_image(d, nodes):
    """Unity RGBA32/ARGB32/RGB24/Alpha8 texture from its stream data, row 0 = bottom (Unity order)"""
    from PIL import Image
    sd = d.get('m_StreamData') or {}
    w, h, fmt = d['m_Width'], d['m_Height'], d['m_TextureFormat']
    raw = read_node(nodes, sd.get('path', ''), sd.get('offset', 0), sd.get('size', 0)) if sd.get('size') else None
    if raw is None:
        return None
    if fmt == 4:
        return Image.frombytes('RGBA', (w, h), raw[:w * h * 4])
    if fmt == 5:
        a, r, g, b = Image.frombytes('RGBA', (w, h), raw[:w * h * 4]).split()
        return Image.merge('RGBA', (r, g, b, a))
    if fmt == 3:
        return Image.frombytes('RGB', (w, h), raw[:w * h * 3]).convert('RGBA')
    if fmt == 1:
        a = Image.frombytes('L', (w, h), raw[:w * h])
        return Image.merge('RGBA', (Image.new('L', (w, h), 255),) * 3 + (a,))
    if fmt == 14:      # BGRA32
        return Image.frombytes('RGBA', (w, h), raw[:w * h * 4], 'raw', 'BGRA')
    if fmt == 63:      # R8: used as a mask -> white with alpha = r
        a = Image.frombytes('L', (w, h), raw[:w * h])
        return Image.merge('RGBA', (Image.new('L', (w, h), 255),) * 3 + (a,))
    import texture2ddecoder
    if fmt == 12:
        return Image.frombytes('RGBA', (w, h), texture2ddecoder.decode_bc3(raw, w, h), 'raw', 'BGRA')
    if fmt == 10:
        return Image.frombytes('RGBA', (w, h), texture2ddecoder.decode_bc1(raw, w, h), 'raw', 'BGRA')
    raise ValueError(f'texture format {fmt} not supported')

# ---------------------------------------------------------------- textures
def texture_settings(tx):
    ts = getattr(tx, 'm_TextureSettings', None)
    if ts is None:
        return {}
    return {k: getattr(ts, k) for k in ('m_FilterMode', 'm_Aniso', 'm_MipBias', 'm_WrapU', 'm_WrapV', 'm_WrapW') if hasattr(ts, k)}


def encode_texture(args):
    """(png_path, astc_path, astcenc, quality) -> error or None"""
    png, astc, enc, quality = args
    try:
        r = subprocess.run([enc, '-cl', png, astc, '4x4', '-' + quality, '-silent'], capture_output=True, timeout=3600)
        if r.returncode != 0:
            return f'astcenc failed for {png}: {r.stderr.decode(errors="replace")[:300]}'
        # strip the 16 byte .astc header (the engine uploads raw blocks) and deflate them:
        # lossless, ~5x smaller on disk, inflated once at GPU upload
        with open(astc, 'rb') as f:
            data = f.read()
        os.remove(astc)
        with open(astc + 'z', 'wb') as f:
            f.write(zlib.compress(data[16:], 9))
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
    name = os.path.basename(out_marker)
    ddir = out_marker + '.d'
    for sub in ('files', 'tex', 'snd'):
        os.makedirs(os.path.join(ddir, sub), exist_ok=True)
    rel_d = rel_base + '/' + name + '.d'
    work = tempfile.mkdtemp(prefix='prr-bundle-')
    nodes = extract_bundle_nodes(path, work)
    cabs = [p for n, p in nodes.items() if not (n.endswith('.resS') or n.endswith('.resource'))]
    env = UnityPy.load(*cabs)
    files = []
    tex_jobs = []
    tmp = tempfile.mkdtemp(prefix='prr-tex-')
    pool = cf.ThreadPoolExecutor(max_workers=jobs)
    futures = []
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
                        try:        # bundles carry their own type trees; the generated one is the fallback
                            d = o.read_typetree()
                        except Exception:
                            d = o.read_typetree(nodes=gen.get_nodes_up(asm, full), check_read=False)
                    except Exception as e:
                        d, asm, full = {'_error': repr(e)[:200]}, None, None
                    d = clean(d, sf, norm_name)
                    d['_script'] = full
                    d['_asm'] = asm
                elif t == 'Texture2D':
                    tt = o.read_typetree()
                    tt.pop('image data', None)
                    d = {'m_Name': tt.get('m_Name'), 'm_Width': tt['m_Width'], 'm_Height': tt['m_Height'], 'm_TextureFormat': 4,
                         'm_MipCount': 1, 'm_IsReadable': bool(tt.get('m_IsReadable', False)),
                         'm_TextureSettings': clean(tt.get('m_TextureSettings', {}), sf, norm_name)}
                    if tt['m_Width'] > 0 and tt['m_Height'] > 0:
                        img = raw_texture_image(tt, nodes)
                        if img is None:
                            img = o.read().image.convert('RGBA').transpose(Image.FLIP_TOP_BOTTOM)
                        png = os.path.join(tmp, f'{pid}.png')
                        img.save(png, compress_level=1)       # PNG top row = Unity bottom row = GL row 0
                        job = (png, os.path.join(ddir, 'tex', f'{pid}.astc'), astcenc, quality)
                        tex_jobs.append(job)
                        futures.append(pool.submit(encode_texture, job))
                        d['_astc'] = f'{rel_d}/tex/{pid}.astcz'
                        d['_astcFormat'] = 0x93B0
                        alpha = img.getchannel('A').tobytes()
                        with open(os.path.join(ddir, 'tex', f'{pid}.a8z'), 'wb') as f:
                            f.write(zlib.compress(alpha, 6))
                        d['_alpha'] = f'{rel_d}/tex/{pid}.a8z'
                        del img, alpha
                    del tt
                elif t == 'AudioClip':
                    tt = o.read_typetree()
                    d = clean(tt, sf, norm_name)
                    d.pop('m_Resource', None)
                    try:
                        import fmod_toolkit
                        r = tt.get('m_Resource') or {}
                        raw = read_node(nodes, r.get('m_Source', ''), r.get('m_Offset', 0), r.get('m_Size', 0))
                        if not raw:
                            raise ValueError('no audio data')
                        if raw[:4] == b'OggS':
                            ogg = os.path.join(ddir, 'snd', f'{pid}.ogg')
                            with open(ogg, 'wb') as f:
                                f.write(raw)
                            d['_ogg'] = f'{rel_d}/snd/{pid}.ogg'
                        else:
                            wav = next(iter(fmod_toolkit.raw_to_wav(raw, tt.get('m_Name', 'clip'), tt.get('m_Channels') or 2, tt.get('m_Frequency') or 44100).values()))
                            ogg = os.path.join(ddir, 'snd', f'{pid}.ogg')
                            pr = subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-f', 'wav', '-i', 'pipe:0', '-c:a', 'libvorbis', '-q:a', '5', ogg],
                                                input=wav, capture_output=True, timeout=600)
                            if pr.returncode == 0:
                                d['_ogg'] = f'{rel_d}/snd/{pid}.ogg'
                            else:
                                log(f'ffmpeg failed {tt.get("m_Name")}: {pr.stderr[:200]}')
                    except Exception as e:
                        log(f'audio failed {tt.get("m_Name")}: {e!r}'[:300])
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
    for i, fu in enumerate(futures):
        err = fu.result()
        if err:
            errs += 1
            log(err)
        if (i + 1) % 200 == 0:
            log(f'    encoded {i + 1}/{len(futures)}')
    pool.shutdown()
    shutil.rmtree(work, ignore_errors=True)
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
