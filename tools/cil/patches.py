"""
Port patches applied to the translated Java (single-player, fullscreen at any aspect ratio).
Each patch is (file, old, new, count); a patch that no longer matches stops the build so it is never
silently lost when the translator output changes.

    python3 -m cil.patches <gen/java>
"""
import os
import sys

PATCHES = [
    # --- true fullscreen: the camera always covers the whole display (no letterbox / pillarbox bars)
    ('cs/global/CameraManager.java',
     '                this.m_camera.set_rect(l2);',
     '                this.m_camera.set_rect(cs.rt.Port.cameraRect(l2));', 1),
    ('cs/global/CameraManager.java',
     '                this.m_camera.set_rect(l4);',
     '                this.m_camera.set_rect(cs.rt.Port.cameraRect(l4));', 1),
    # --- dynamic spawn margins: the original 640-wide layout is pushed out to the real screen edges
    #     (exactly 100 at 16:9 as in the PC game, more on wider phones)
    ('cs/global/GameUtils.java',
     '                $s2I = 100;',
     '                $s2I = cs.rt.Port.wideOffset(t2);', 1),
    ('cs/global/GameUtils.java',
     '                $s2I = (-100);',
     '                $s2I = (-cs.rt.Port.wideOffset(t2));', 1),
    ('cs/global/GameUtils.java',
     '                $s1I = 100;',
     '                $s1I = cs.rt.Port.wideOffset(t2);', 1),
    ('cs/global/GameUtils.java',
     '                $s1I = (-100);',
     '                $s1I = (-cs.rt.Port.wideOffset(t2));', 1),
    # --- the stage clear-colour quad: shown only when a stage clears the background (Unity leaves its sort
    #     order against the sky to chance) and wide enough for any aspect ratio
    ('cs/global/BackgroundManager.java',
     '                t10 = new cs.UnityEngine.Vector3(860.0f, 480.0f, 1.0f);',
     '                t10 = new cs.UnityEngine.Vector3(cs.rt.Port.clearWidth(860.0f), 480.0f, 1.0f);', 1),
    ('cs/global/BackgroundManager.java',
     '                t4.set_sprite(t8);',
     '                t4.set_sprite(t8);\n                t4.set_enabled(false);   // port: only drawn for clear-background stages', 1),
    ('cs/global/BackgroundManager.java',
     '        t2.SetActive((((a0) ? 1 : 0) == 0));\n        return;',
     '        t2.SetActive((((a0) ? 1 : 0) == 0));\n        this.clearColor.set_enabled(a0);\n        return;', 1),
    # --- single player: no "press fire to join" prompts for a second player
    ('cs/global/DisplayPlayerLives.java',
     't2 = cs.global.LocalisationManager.Get("UI.FireToJoin", t1);',
     't2 = "";', 1),
    ('cs/global/ShipPanelScript.java',
     't21 = cs.global.LocalisationManager.Get("ShipMenu.FireToJoin", t20);',
     't21 = "";', 1),
]


def apply(root):
    for rel, old, new, count in PATCHES:
        p = os.path.join(root, rel)
        s = open(p, encoding='utf-8').read()
        n = s.count(old)
        if n == 0 and s.count(new) == count:
            continue                      # already applied
        if n != count:
            raise SystemExit(f'patch mismatch in {rel}: expected {count} x {old.strip()!r}, found {n}')
        s = s.replace(old, new)
        open(p, 'w', encoding='utf-8').write(s)
    print(f'{len(PATCHES)} port patches applied')


if __name__ == '__main__':
    apply(sys.argv[1])
