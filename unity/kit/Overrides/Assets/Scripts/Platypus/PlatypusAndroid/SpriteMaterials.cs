// Platypus Reclayed Android port: the game gives its sprites their per-sprite shading values (layer, fog, emission,
// horizon line, scrolling, hit flash) through MaterialPropertyBlocks. On Unity 6 a SpriteRenderer that carries a
// property block keeps drawing the texture of the sprite it showed before once the sprite changes, so pooled
// objects came out wrong: Wide Shot bullets in the white of the normal shots (and normal shots in yellow), shots
// blown up and cut off, rocket trails as white squares, clouds and a second moon with the moon's texture.
// Here the values of such a block go into a material instead (shared by every sprite with the same values, created
// once per scene), and the sprite keeps no block, which is the ordinary way Unity draws sprites.
// A hit flash (_BlinkStart) gets a material of the sprite's own while it is recent.
using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace PlatypusAndroid
{
    public static class SpriteMaterials
    {
        static readonly string[] FloatNames =
        {
            "_FogDistance", "_Layer", "_Transparency", "_EmissionPower", "_DayCycleMultiplier", "_FogDistanceMultiplier",
            "_UseCustomHorizonLine", "_CustomHorizonLine", "_DoReflectionPass", "_DoRegularPass", "_BlinkDuration",
        };
        const int TransparencyIndex = 2;
        static int[] floatIds;
        static int scrollingId, blinkId;

        struct Key : IEquatable<Key>
        {
            public int scene, baseId, mask;
            public float f0, f1, f2, f3, f4, f5, f6, f7, f8, f9, f10;
            public Vector4 scroll;

            public float this[int i]
            {
                get
                {
                    switch (i)
                    {
                        case 0: return f0; case 1: return f1; case 2: return f2; case 3: return f3; case 4: return f4;
                        case 5: return f5; case 6: return f6; case 7: return f7; case 8: return f8; case 9: return f9;
                        default: return f10;
                    }
                }
                set
                {
                    switch (i)
                    {
                        case 0: f0 = value; break; case 1: f1 = value; break; case 2: f2 = value; break;
                        case 3: f3 = value; break; case 4: f4 = value; break; case 5: f5 = value; break;
                        case 6: f6 = value; break; case 7: f7 = value; break; case 8: f8 = value; break;
                        case 9: f9 = value; break; default: f10 = value; break;
                    }
                }
            }

            public bool Equals(Key o) =>
                scene == o.scene && baseId == o.baseId && mask == o.mask &&
                f0 == o.f0 && f1 == o.f1 && f2 == o.f2 && f3 == o.f3 && f4 == o.f4 && f5 == o.f5 &&
                f6 == o.f6 && f7 == o.f7 && f8 == o.f8 && f9 == o.f9 && f10 == o.f10 && scroll == o.scroll;

            public override bool Equals(object obj) => obj is Key k && Equals(k);

            public override int GetHashCode()
            {
                unchecked
                {
                    int h = scene * 397 ^ baseId;
                    h = h * 31 + mask;
                    h = h * 31 + f0.GetHashCode(); h = h * 31 + f1.GetHashCode(); h = h * 31 + f2.GetHashCode();
                    h = h * 31 + f3.GetHashCode(); h = h * 31 + f4.GetHashCode(); h = h * 31 + f5.GetHashCode();
                    h = h * 31 + f6.GetHashCode(); h = h * 31 + f7.GetHashCode(); h = h * 31 + f8.GetHashCode();
                    h = h * 31 + f9.GetHashCode(); h = h * 31 + f10.GetHashCode(); h = h * 31 + scroll.GetHashCode();
                    return h;
                }
            }
        }

        class Shared { public Material mat; public int scene; }
        class Own { public Material mat; public int scene; public Renderer renderer; }

        static readonly Dictionary<Key, Shared> shared = new Dictionary<Key, Shared>();
        static readonly Dictionary<int, Material> origin = new Dictionary<int, Material>();   // our material -> the game's
        static readonly Dictionary<int, Own> own = new Dictionary<int, Own>();             // by renderer id
        static bool hooked;
        static int created;

        static void Init()
        {
            if (floatIds != null) return;
            floatIds = new int[FloatNames.Length];
            for (int i = 0; i < FloatNames.Length; i++) floatIds[i] = Shader.PropertyToID(FloatNames[i]);
            scrollingId = Shader.PropertyToID("_Scrolling");
            blinkId = Shader.PropertyToID("_BlinkStart");
            if (!hooked) { hooked = true; SceneManager.sceneUnloaded += Purge; }
        }

        /// <summary>Use instead of <c>renderer.SetPropertyBlock(block)</c> for the game's sprites.</summary>
        public static void Apply(Renderer r, MaterialPropertyBlock b)
        {
            if (r == null) return;
            Material current = r.sharedMaterial;
            if (current == null || b == null) return;
            Init();
            Material baseMat = origin.TryGetValue(current.GetInstanceID(), out Material o) && o != null ? o : current;

            var key = new Key { scene = r.gameObject.scene.handle, baseId = baseMat.GetInstanceID() };
            for (int i = 0; i < floatIds.Length; i++)
            {
                float v;
                if (b.HasFloat(floatIds[i])) v = b.GetFloat(floatIds[i]);
                else if (b.HasInteger(floatIds[i])) v = b.GetInteger(floatIds[i]);
                else continue;
                if (i == TransparencyIndex) v = Mathf.Round(v * 128f) / 128f;      // the front fade: a few steps only
                key.mask |= 1 << i;
                key[i] = v;
            }
            if (b.HasVector(scrollingId)) { key.mask |= 1 << 20; key.scroll = b.GetVector(scrollingId); }

            if (!shared.TryGetValue(key, out Shared s) || s.mat == null)
            {
                var m = new Material(baseMat) { name = baseMat.name + " #" + (++created) };
                for (int i = 0; i < floatIds.Length; i++) if ((key.mask & (1 << i)) != 0) m.SetFloat(floatIds[i], key[i]);
                if ((key.mask & (1 << 20)) != 0) m.SetVector(scrollingId, key.scroll);
                s = new Shared { mat = m, scene = key.scene };
                shared[key] = s;
                origin[m.GetInstanceID()] = baseMat;
            }

            // a recent hit flash: a material of this sprite's own, so only this sprite flashes
            if (b.HasFloat(blinkId))
            {
                float start = b.GetFloat(blinkId);
                float age = Time.timeSinceLevelLoad - start;
                if (age > -1f && age < 3f)
                {
                    int id = r.GetInstanceID();
                    if (!own.TryGetValue(id, out Own mine) || mine.mat == null)
                    {
                        if (own.Count > 512) PurgeDead();
                        mine = new Own { mat = new Material(s.mat) { name = baseMat.name + " (flash)" }, scene = key.scene, renderer = r };
                        own[id] = mine;
                        origin[mine.mat.GetInstanceID()] = baseMat;
                    }
                    else
                    {
                        if (mine.mat.shader != s.mat.shader) mine.mat.shader = s.mat.shader;
                        mine.mat.CopyPropertiesFromMaterial(s.mat);
                    }
                    mine.mat.SetFloat(blinkId, start);
                    r.sharedMaterial = mine.mat;
                    return;
                }
            }
            if (current != s.mat) r.sharedMaterial = s.mat;
        }

        static void PurgeDead()
        {
            var dead = new List<int>();
            foreach (var kv in own) if (kv.Value.renderer == null) dead.Add(kv.Key);
            foreach (int id in dead) { Forget(own[id].mat); own.Remove(id); }
        }

        static void Forget(Material m)
        {
            if (m == null) return;
            origin.Remove(m.GetInstanceID());
            UnityEngine.Object.Destroy(m);
        }

        /// <summary>A scene's sprites are gone with it: free the materials made for them.</summary>
        static void Purge(Scene scene)
        {
            int h = scene.handle;
            var keys = new List<Key>();
            foreach (var kv in shared) if (kv.Value.scene == h || kv.Value.mat == null) keys.Add(kv.Key);
            foreach (Key k in keys) { Forget(shared[k].mat); shared.Remove(k); }
            var ids = new List<int>();
            foreach (var kv in own) if (kv.Value.renderer == null || kv.Value.scene == h) ids.Add(kv.Key);
            foreach (int id in ids) { Forget(own[id].mat); own.Remove(id); }
        }
    }
}
