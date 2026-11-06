import glob, os, sys, struct
import UnityPy
from UnityPy.helpers import CompressionHelper
data, out = sys.argv[1], sys.argv[2]
WANT = {'Unlit/SpriteShader': 'resources.assets', 'Unlit/SpriteReflectionCombinedShader': 'resources.assets', 'Unlit/MorphTextShader': 'resources.assets',
        'Unlit/MorphTextFlashShader': 'sharedassets1.assets', 'Unlit/MultiTintShader': 'sharedassets4.assets', 'Unlit/UIWorldSpaceTexture': 'sharedassets0.assets'}
for f in sorted(set(WANT.values())):
    env = UnityPy.load(os.path.join(data, f))
    for o in env.objects:
        if o.type.name != 'Shader': continue
        sh = o.read()
        nm = sh.m_ParsedForm.m_Name
        if nm not in WANT: continue
        short = nm.split('/')[-1]
        blob = bytes(sh.compressedBlob)
        print(nm, 'platforms', list(sh.platforms))
        for pi, plat in enumerate(sh.platforms):
            offs, clen, dlen = sh.offsets[pi], sh.compressedLengths[pi], sh.decompressedLengths[pi]
            # 2022: per platform lists (one entry per blob segment)
            offs = offs if isinstance(offs, list) else [offs]; clen = clen if isinstance(clen, list) else [clen]; dlen = dlen if isinstance(dlen, list) else [dlen]
            for si, (a, c, d) in enumerate(zip(offs, clen, dlen)):
                raw = CompressionHelper.decompress_lz4(blob[a:a + c], d)
                n = 0; i = raw.find(b'DXBC')
                while i >= 0:
                    size = struct.unpack_from('<I', raw, i + 24)[0]
                    open(os.path.join(out, f'{short}_p{plat}_s{si}_{n:02d}.dxbc'), 'wb').write(raw[i:i + size])
                    n += 1; i = raw.find(b'DXBC', i + size)
                print('  platform', plat, 'segment', si, 'size', d, 'dxbc', n)
