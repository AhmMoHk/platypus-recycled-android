#!/usr/bin/env python3
"""Upgrade an older converted data pack in place: tex/<pid>.astc -> tex/<pid>.astcz (deflated, lossless)
and point the bundle JSON at the new files.

    python3 deflate_astc.py <converted data dir>
"""
import concurrent.futures as cf
import os
import sys
import zlib


def pack(path):
    with open(path, 'rb') as f:
        d = f.read()
    with open(path + 'z.tmp', 'wb') as f:
        f.write(zlib.compress(d, 9))
    os.replace(path + 'z.tmp', path + 'z')
    os.remove(path)
    return len(d)


def main():
    root = sys.argv[1]
    texs, jsons = [], []
    for dp, dn, fn in os.walk(root):
        for f in fn:
            p = os.path.join(dp, f)
            if f.endswith('.astc') and os.path.basename(dp) == 'tex':
                texs.append(p)
            elif f.endswith('.json') and os.path.basename(dp) == 'files' and dp.endswith('.d/files') or dp.endswith('.d' + os.sep + 'files'):
                jsons.append(p)
    with cf.ProcessPoolExecutor(os.cpu_count()) as ex:
        total = sum(ex.map(pack, texs, chunksize=4))
    for j in jsons:
        with open(j, encoding='utf-8') as f:
            s = f.read()
        s2 = s.replace('.astc"', '.astcz"')
        if s2 != s:
            with open(j, 'w', encoding='utf-8') as f:
                f.write(s2)
    print(f'{len(texs)} textures deflated ({total >> 20} MB raw), {len(jsons)} bundle files updated')


if __name__ == '__main__':
    main()
