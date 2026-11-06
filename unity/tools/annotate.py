"""Annotate the disassembly of every program with Unity's constant-buffer layout (names from m_ParsedForm)."""
import os, sys, subprocess, json
import UnityPy
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import dxbc_dis
data = "/home/claude/prr/x/steamapps/common/Platypus Reclayed/Platypus_Data/"
WANT = {'Unlit/SpriteShader': 'resources.assets', 'Unlit/SpriteReflectionCombinedShader': 'resources.assets', 'Unlit/MorphTextShader': 'resources.assets',
        'Unlit/MorphTextFlashShader': 'sharedassets1.assets', 'Unlit/MultiTintShader': 'sharedassets4.assets', 'Unlit/UIWorldSpaceTexture': 'sharedassets0.assets'}
os.makedirs('hlsl_ref', exist_ok=True)
for f in sorted(set(WANT.values())):
    env = UnityPy.load(data + f)
    for o in env.objects:
        if o.type.name != 'Shader': continue
        d = o.read_typetree(); pf = d['m_ParsedForm']; nm = pf['m_Name']
        if nm not in WANT: continue
        short = nm.split('/')[-1]
        out = [f'// {nm}  (DXBC disassembly with Unity parameter names)', '// properties: ' + ', '.join(p['m_Name'] for p in pf['m_PropInfo']['m_Props'])]
        for si, ss in enumerate(pf['m_SubShaders']):
            for pi, ps in enumerate(ss['m_Passes']):
                names = {v: k for k, v in ps['m_NameIndices']}
                st = ps['m_State']
                out.append(f'\n////// subshader {si} pass {pi} name={ps.get("m_Name") or st.get("m_Name")} tags={ps["m_Tags"]}')
                out.append(f'// state: blend src={st["rtBlend0"]["srcBlend"]["val"]} dst={st["rtBlend0"]["destBlend"]["val"]} zwrite={st["zWrite"]["val"]} cull={st["culling"]["val"]} ztest={st["zTest"]["val"]}')
                for stage in ('progVertex', 'progFragment'):
                    pg = ps[stage]
                    cp = pg['m_CommonParameters']
                    for lst in pg['m_PlayerSubPrograms']:
                        for sp in lst:
                            bi = sp['m_BlobIndex']
                            kw = [pf['m_KeywordNames'][k] if 'm_KeywordNames' in pf and k < len(pf['m_KeywordNames']) else k for k in sp['m_KeywordIndices']]
                            path = f'dxbc/{short}_p4_s0_{bi:02d}.dxbc'
                            if not os.path.exists(path): continue
                            sh = dxbc_dis.Shader(open(path, 'rb').read())
                            for cbi, cb in enumerate(cp['m_ConstantBuffers']):
                                slot = next((b['m_Index'] for b in cp['m_ConstantBufferBindings'] if b['m_NameIndex'] == cb['m_NameIndex']), cbi)
                                for vp in cb['m_VectorParams'] + cb['m_MatrixParams']:
                                    n = names.get(vp['m_NameIndex'], '?'); arr = max(1, vp.get('m_ArraySize', 0))
                                    rows = vp.get('m_RowCount', 1)
                                    span = (16 * arr if arr > 1 else 4 * vp.get('m_Dim', 4)) if 'm_RowCount' not in vp else 16 * rows * vp.get('m_ColumnCount', 4) // 4
                                    for k in range(0, max(4, span), 4):
                                        sh.cbvars[(slot, vp['m_Index'] + k)] = (n, k)
                                out.append(f'// cb{slot} {names.get(cb["m_NameIndex"])}: ' + ', '.join(f'{names.get(v["m_NameIndex"])}@{v["m_Index"]}' + (f'[{v["m_ArraySize"]}]' if v.get('m_ArraySize') else '') for v in cb['m_VectorParams'] + cb['m_MatrixParams']))
                            for tp in cp['m_TextureParams']:
                                out.append(f'// t{tp["m_Index"]} = {names.get(tp["m_NameIndex"])}')
                            out.append(f'//// {stage} blob {bi} keywords {kw}')
                            for x in sh.sig('ISGN'): out.append('// in  ' + x)
                            for x in sh.sig('OSGN'): out.append('// out ' + x)
                            out += [l for l in sh.dis() if not l.startswith('dcl_')]
        open(f'hlsl_ref/{short}.dis.txt', 'w').write('\n'.join(out) + '\n')
        print(short, len(out), 'lines')
