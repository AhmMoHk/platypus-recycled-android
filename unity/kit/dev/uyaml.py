#!/usr/bin/env python3
"""Tiny Unity scene YAML helper: object index, names, parents and components (dev inspection only)."""
import re, sys
class Scene:
    def __init__(s, path):
        txt = open(path, encoding='utf-8', errors='replace').read()
        s.docs = {}
        for m in re.finditer(r'^--- !u!(\d+) &(-?\d+)[^\n]*\n(.*?)(?=^--- |\Z)', txt, re.S | re.M):
            s.docs[int(m.group(2))] = (int(m.group(1)), m.group(3))
    def field(s, fid, name):
        t = s.docs.get(fid)
        if not t: return None
        m = re.search(r'^\s*' + re.escape(name) + r':\s*(.*)$', t[1], re.M)
        return m.group(1).strip() if m else None
    def ref(s, fid, name):
        v = s.field(fid, name)
        m = re.search(r'fileID: (-?\d+)', v or '')
        return int(m.group(1)) if m else 0
    def go(s, fid):            # component or transform -> gameobject id
        t = s.docs.get(fid)
        if t and t[0] == 1: return fid
        return s.ref(fid, 'm_GameObject')
    def name(s, fid): return s.field(s.go(fid), 'm_Name')
    def comps(s, gid):
        t = s.docs[gid][1]
        return [int(x) for x in re.findall(r'component: \{fileID: (-?\d+)\}', t)]
    def transform(s, gid):
        for c in s.comps(gid):
            if s.docs[c][0] in (4, 224): return c
    def parent(s, gid):
        tr = s.transform(gid); p = s.ref(tr, 'm_Father')
        return s.go(p) if p else 0
    def children(s, gid):
        tr = s.transform(gid); t = s.docs[tr][1]
        m = re.search(r'm_Children:\n((?:\s*- \{fileID: -?\d+\}\n)*)', t)
        return [s.go(int(x)) for x in re.findall(r'fileID: (-?\d+)', m.group(1))] if m else []
    def script(s, cid): return s.field(cid, 'm_Script')
    def path(s, gid):
        out = []
        while gid: out.append(s.field(gid, 'm_Name')); gid = s.parent(gid)
        return '/'.join(reversed(out))
