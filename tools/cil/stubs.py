"""Generate compile-only stubs for runtime classes that are referenced by translated code but not yet written."""
import os
import collections
from .jtypes import *
from . import xlat as X

IFACE_HINT = lambda csname: (csname.split('/')[-1].split('.')[-1][:1] == 'I' and csname.split('/')[-1].split('.')[-1][1:2].isupper()
                             and (csname.startswith('System.') or csname.startswith('UnityEngine.')))


def default(j):
    return X.Xlat.default_of(j)


def gen(x, outdir, rtdir):
    used = dict(x.ts.used)
    members = getattr(x, 'ext_members', {})
    for c in members:
        if c not in used and c not in ('Object', 'String'):
            used[c] = (None, 'class')
    hand = set()
    for root, _, files in os.walk(rtdir):
        for f in files:
            if f.endswith('.java'):
                rel = os.path.relpath(os.path.join(root, f), rtdir)[:-5].replace(os.sep, '.')
                hand.add(rel)
    # close over parents (base classes / interfaces) so stubs extend real types
    rev = {jclass_name(k): k for k in EXT_INDEX}
    work = list(used)
    while work:
        c = work.pop()
        for p in X.RT_HIERARCHY.get(c, []):
            if p in used or not p.startswith('cs.'):
                continue
            cn = rev.get(p)
            e = EXT_INDEX.get(cn, {}) if cn else {}
            used[p] = (cn, {'iface': 'iface', 'class': 'class', 'struct': 'struct', 'enum': 'enum', 'delegate': 'delegate'}.get(e.get('k'), 'class'))
            work.append(p)
    n = 0
    report = []
    for cls, (csname, kind) in sorted(used.items()):
        if cls in ('Object', 'String') or cls in JPRIMS:
            continue
        mem = members.get(cls, {})
        if cls in hand:
            report.append((cls, mem))
            continue
        pkg, simple = cls.rsplit('.', 1)
        lines = [f'package {pkg};', '', '@SuppressWarnings({"unchecked", "rawtypes", "unused"})']
        iface = csname is not None and (IFACE_HINT(csname) or EXT_INDEX.get(csname, {}).get('k') == 'iface')
        if kind == 'iface':
            iface = True
        if kind == 'enum':
            lines.append(f'public final class {simple} {{')
            ie = EXT_INDEX.get(csname, {})
            nm = ', '.join(jstr_literal(a) for a in ie.get('names', []))
            vl = ', '.join(f'{v}L' for v in ie.get('vals', []))
            fl = 'true' if ie.get('flags') else 'false'
            lines.append(f'    public static final cs.rt.EnumInfo $info = new cs.rt.EnumInfo("{csname}", {simple}.class, {fl}, new String[]{{{nm}}}, new long[]{{{vl}}});')
            lines.append('}')
        elif kind == 'delegate':
            ret, ps = EXT_DELEGATES.get(csname, (('P', 'void'), []))
            j = lambda t: 'Object' if t[0] in 'VW' else x.ts.jt(t)
            rj = j(ret)
            pd = ', '.join(f'{j(p)} a{i}' for i, p in enumerate(ps))
            pa = ', '.join(f'a{i}' for i in range(len(ps)))
            lines.append(f'public class {simple} extends cs.System.Delegate {{')
            if rj == 'void':
                lines.append(f'    public void Invoke({pd}) {{ if ($list != null) {{ for (cs.System.Delegate d : $list) (({simple}) d).$call({pa}); return; }} $call({pa}); }}')
            else:
                lines.append(f'    public {rj} Invoke({pd}) {{ if ($list != null) {{ {rj} r = {default(rj)}; for (cs.System.Delegate d : $list) r = (({simple}) d).$call({pa}); return r; }} return $call({pa}); }}')
            lines.append(f'    protected {rj} $call({pd}) {{ throw new IllegalStateException("unbound delegate"); }}')
            lines.append(f'    protected cs.System.Delegate $empty() {{ return new {simple}(); }}')
            lines.append('}')
        else:
            parents = X.RT_HIERARCHY.get(cls, [])
            if iface:
                ext = ' extends ' + ', '.join(parents) if parents else ''
                lines.append(f'public interface {simple}{ext} {{')
            else:
                isif = lambda p: IFACE_HINT_J(p) or EXT_INDEX.get(rev.get(p), {}).get('k') == 'iface'
                sup = [p for p in parents if not isif(p)]
                ifs = [p for p in parents if isif(p)]
                if kind == 'struct':
                    ifs = ['cs.rt.Struct'] + ifs
                ext = f' extends {sup[0]}' if sup else ''
                imp = (' implements ' + ', '.join(ifs)) if ifs else ''
                lines.append(f'public class {simple}{ext}{imp} {{')
                lines.append(f'    public {simple}() {{}}')
            # merge members with same signature: prefer specific return types
            merged = {}
            for (kind_, name, ps), (k2, nm, jps, jret, st) in mem.items():
                key = (k2, nm, jps)
                if key in merged:
                    old = merged[key]
                    if old[3] == 'Object' and jret != 'Object':
                        merged[key] = (k2, nm, jps, jret, st)
                else:
                    merged[key] = (k2, nm, jps, jret, st)
            # same name+params with different kind (static vs inst) - keep both is illegal: prefer inst
            sigs = set()
            fields = []
            for (k2, nm, jps), (_, _, _, jret, st) in sorted(merged.items(), key=lambda kv: (kv[0][0], kv[0][1] or '', kv[0][2])):
                pd = ', '.join(f'{p} a{i}' for i, p in enumerate(jps))
                if k2 == 'field':
                    fields.append((nm, jret, st))
                    continue
                if k2 == 'ctor':
                    if iface or not jps:
                        continue
                    if ('ctor', jps) in sigs:
                        continue
                    sigs.add(('ctor', jps))
                    lines.append(f'    public {simple}({pd}) {{ throw cs.rt.Rt.todo("{cls}.<init>"); }}')
                    continue
                if (nm, jps) in sigs:
                    continue
                sigs.add((nm, jps))
                if iface and k2 == 'inst':
                    lines.append(f'    {jret} {nm}({pd});')
                    continue
                stat = 'static ' if k2 == 'static' else ''
                if iface:
                    lines.append(f'    {stat}{jret} {nm}({pd}) {{ throw cs.rt.Rt.todo("{cls}.{nm}"); }}' if stat else f'    {jret} {nm}({pd});')
                    continue
                lines.append(f'    public {stat}{jret} {nm}({pd}) {{ throw cs.rt.Rt.todo("{cls}.{nm}"); }}')
            seenf = set()
            for nm, jt, st in fields:
                if nm in seenf:
                    continue
                seenf.add(nm)
                init = ''
                lines.append(f'    public {"static " if st else ""}{jt} {nm};')
            if kind == 'struct' and not iface:
                lines.append(f'    public {simple} $copy() {{ throw cs.rt.Rt.todo("{cls}.$copy"); }}')
                lines.append(f'    public void $set(Object o) {{ throw cs.rt.Rt.todo("{cls}.$set"); }}')
                lines.append(f'    public void $clear() {{ throw cs.rt.Rt.todo("{cls}.$clear"); }}')
                lines.append(f'    public static {simple}[] $newArray(int n) {{ {simple}[] a = new {simple}[n]; for (int i = 0; i < n; i++) a[i] = new {simple}(); return a; }}')
            lines.append('}')
        d = os.path.join(outdir, *pkg.split('.'))
        os.makedirs(d, exist_ok=True)
        with open(os.path.join(d, simple + '.java'), 'w') as f:
            f.write('\n'.join(lines) + '\n')
        n += 1
    # report of members used on hand-written classes (for review)
    with open(os.path.join(outdir, 'rt_members_used.txt'), 'w') as f:
        for cls, mem in report:
            f.write(cls + '\n')
            for (k2, nm, jps), (_, _, _, jret, st) in sorted(mem.items(), key=lambda kv: (kv[0][0], kv[0][1] or '', kv[0][2])):
                f.write(f'    {k2} {nm}({", ".join(jps)}) {jret or ""}{" static" if st else ""}\n')
    return n


def IFACE_HINT_J(j):
    s = j.rsplit('.', 1)[-1]
    return s[:1] == 'I' and s[1:2].isupper() and (j.startswith('cs.System.') or j.startswith('cs.UnityEngine.'))
