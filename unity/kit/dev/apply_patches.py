#!/usr/bin/env python3
"""Apply PlatypusAndroidKit/patches.json to a decompiled Scripts tree (same logic as tools/setup.ps1 on Windows)."""
import json, os, sys
root = sys.argv[1]
kit = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'PlatypusAndroidKit')
P = json.load(open(os.path.join(kit, 'patches.json'), encoding='utf-8'))
for rel in P.get('delete', []):
    p = os.path.join(root, rel)
    if os.path.isdir(p):
        import shutil; shutil.rmtree(p)
    elif os.path.exists(p):
        os.remove(p)
        if os.path.exists(p + '.meta'): os.remove(p + '.meta')
bad = 0
for e in P['patches']:
    p = os.path.join(root, e['file'])
    s = open(p, encoding='utf-8-sig').read().replace('\r\n', '\n')
    n = s.count(e['old'])
    if n != e.get('count', 1):
        print(f"PATCH FAILED ({n} matches): {e['file']}: {e['old'][:70]!r}"); bad += 1; continue
    s = s.replace(e['old'], e['new'])
    open(p, 'w', encoding='utf-8').write(s)
print(f"{len(P['patches']) - bad}/{len(P['patches'])} patches applied")
sys.exit(1 if bad else 0)
