#!/usr/bin/env python3
"""Re-read the MonoBehaviours of an already converted bundle with the bundle's own (embedded) type trees.

    python3 reread_monobehaviours.py <original bundle> <converted marker> [work dir]
"""
import json, os, shutil, sys, tempfile
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import convert_assets as ca
import UnityPy


def main():
    bundle, marker = sys.argv[1], sys.argv[2]
    work = sys.argv[3] if len(sys.argv) > 3 else tempfile.mkdtemp(prefix='prr-mb-')
    nodes = ca.extract_bundle_nodes(bundle, work)
    for n in list(nodes):
        if n.endswith('.resS') or n.endswith('.resource'):
            os.remove(nodes.pop(n))
    env = UnityPy.load(*nodes.values())
    ddir = marker + '.d/files'
    fixed = 0
    for fname, sf in env.files.items():
        cab = ca.norm_name(fname)
        jp = os.path.join(ddir, cab + '.json')
        if not os.path.exists(jp):
            continue
        J = json.load(open(jp, encoding='utf-8'))
        for pid, o in sf.objects.items():
            if o.type.name != 'MonoBehaviour':
                continue
            cur = J['objects'].get(str(pid))
            if cur is None:
                continue
            try:
                d = o.read_typetree()
            except Exception as e:
                print('  keep', pid, repr(e)[:80])
                continue
            d = ca.clean(d, sf, ca.norm_name)
            d['_script'] = cur['d'].get('_script')
            d['_asm'] = cur['d'].get('_asm')
            if not d['_script']:
                try:
                    sc = o.read(check_read=False).m_Script.read()
                    d['_script'] = (sc.m_Namespace + '.' if sc.m_Namespace else '') + sc.m_ClassName
                    d['_asm'] = sc.m_AssemblyName
                except Exception:
                    pass
            if d != cur['d']:
                cur['d'] = d
                fixed += 1
        json.dump(J, open(jp, 'w', encoding='utf-8'), separators=(',', ':'))
    shutil.rmtree(work, ignore_errors=True)
    print(f'{fixed} MonoBehaviours updated')


if __name__ == '__main__':
    main()
