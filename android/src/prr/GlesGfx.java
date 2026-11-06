package prr;

import android.opengl.GLES20;
import cs.UnityEngine.Gfx;
import cs.UnityEngine.Material;
import cs.UnityEngine.MaterialPropertyBlock;
import cs.UnityEngine.Shader;
import cs.UnityEngine.Texture;
import cs.UnityEngine.Texture2D;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;

/**
 * GLES2 backend. One vertex shader; a fragment program per game shader family (sprite, UI, TMP SDF,
 * morph-text tint, multi-tint). Vertex data is streamed from direct buffers, textures are uploaded lazily
 * (RGBA8 from CPU pixels, or the converted compressed blob as-is).
 */
public final class GlesGfx extends Gfx {
    // ------------------------------------------------------------------ programs
    private static final String VS =
        "uniform mat4 uMVP;\n" +
        "attribute vec3 aPos;\n" +
        "attribute vec4 aCol;\n" +
        "attribute vec4 aUV0;\n" +
        "attribute vec4 aUV1;\n" +
        "uniform vec4 uTint;\n" +
        "uniform vec4 uST;\n" +            // _MainTex scale/offset
        "uniform vec2 uScroll;\n" +
        "varying vec4 vCol;\n" +
        "varying vec4 vUV0;\n" +
        "varying vec4 vUV1;\n" +
        "varying vec3 vWorld;\n" +
        "uniform mat4 uModel;\n" +
        "void main() {\n" +
        "  gl_Position = uMVP * vec4(aPos, 1.0);\n" +
        "  vCol = aCol * uTint;\n" +
        "  vUV0 = vec4(aUV0.xy * uST.xy + uST.zw + uScroll, aUV0.zw);\n" +
        "  vUV1 = aUV1;\n" +
        "  vWorld = (uModel * vec4(aPos, 1.0)).xyz;\n" +
        "}\n";

    /** plain textured quad (UI, sprites without effects, TMP bitmap) */
    private static final String FS_UI =
        "precision mediump float;\n" +
        "uniform sampler2D uTex;\n" +
        "varying vec4 vCol;\n" +
        "varying vec4 vUV0;\n" +
        "void main() {\n" +
        "  vec4 c = texture2D(uTex, vUV0.xy) * vCol;\n" +
        "  if (c.a <= 0.0) discard;\n" +
        "  gl_FragColor = c;\n" +
        "}\n";

    /** SpriteShader / SpriteReflectionCombinedShader: fog by distance, emission, blink, day cycle, transparency */
    private static final String FS_SPRITE =
        "precision mediump float;\n" +
        "uniform sampler2D uTex;\n" +
        "uniform vec4 uFogColor;\n" +
        "uniform float uFogDistance, uFogMul, uEmission, uDayCycle, uTransparency, uBlink, uLayer;\n" +
        "varying vec4 vCol;\n" +
        "varying vec4 vUV0;\n" +
        "varying vec3 vWorld;\n" +
        "void main() {\n" +
        "  vec4 c = texture2D(uTex, vUV0.xy) * vCol;\n" +
        "  if (c.a <= 0.0) discard;\n" +
        "  c.rgb *= mix(1.0, uDayCycle, step(0.001, uDayCycle));\n" +
        "  float fog = clamp(uFogDistance * uFogMul, 0.0, 1.0);\n" +
        "  c.rgb = mix(c.rgb, uFogColor.rgb, fog * uFogColor.a);\n" +
        "  c.rgb += c.rgb * uEmission;\n" +
        "  c.rgb = mix(c.rgb, vec3(1.0), uBlink);\n" +
        "  c.a *= uTransparency;\n" +
        "  gl_FragColor = c;\n" +
        "}\n";

    /** TextMeshPro distance field (face + outline + underlay) */
    private static final String FS_SDF =
        "precision mediump float;\n" +
        "uniform sampler2D uTex;\n" +
        "uniform vec4 uFaceColor, uOutlineColor, uUnderlayColor;\n" +
        "uniform float uOutlineWidth, uOutlineSoftness, uFaceDilate, uGradientScale, uScaleRatioA, uWeight;\n" +
        "uniform vec2 uUnderlayOffset;\n" +
        "uniform float uUnderlayDilate, uUnderlaySoftness, uTexW, uTexH;\n" +
        "varying vec4 vCol;\n" +
        "varying vec4 vUV0;\n" +
        "void main() {\n" +
        "  float sd = texture2D(uTex, vUV0.xy).a;\n" +
        "  float scale = uGradientScale * uScaleRatioA;\n" +
        "  float bias = 0.5 - uWeight - uFaceDilate * 0.5;\n" +
        "  float outline = uOutlineWidth * uScaleRatioA * 0.5;\n" +
        "  float sharp = max(fwidth(sd), 0.0001) + uOutlineSoftness * 0.25;\n" +
        "  float face = smoothstep(bias - sharp, bias + sharp, sd);\n" +
        "  float all = smoothstep(bias - outline - sharp, bias - outline + sharp, sd);\n" +
        "  vec4 fc = uFaceColor * vCol;\n" +
        "  vec4 oc = uOutlineColor;\n" +
        "  oc.a *= vCol.a;\n" +
        "  vec4 c = mix(oc, fc, face);\n" +
        "  c.a *= all;\n" +
        "  if (uUnderlayColor.a > 0.0) {\n" +
        "    float ud = texture2D(uTex, vUV0.xy - vec2(uUnderlayOffset.x / uTexW, uUnderlayOffset.y / uTexH)).a;\n" +
        "    float ua = smoothstep(bias - uUnderlayDilate - uUnderlaySoftness, bias - uUnderlayDilate + uUnderlaySoftness, ud);\n" +
        "    vec4 uc = uUnderlayColor;\n" +
        "    uc.a *= ua * vCol.a * (1.0 - c.a);\n" +
        "    c = vec4(mix(uc.rgb, c.rgb, c.a), c.a + uc.a);\n" +
        "  }\n" +
        "  if (c.a <= 0.0) discard;\n" +
        "  gl_FragColor = c;\n" +
        "}\n";

    /** MorphTextShader / MorphTextFlashShader: luminance split into high/low tints */
    private static final String FS_MORPH =
        "precision mediump float;\n" +
        "uniform sampler2D uTex;\n" +
        "uniform vec4 uHigh, uLow, uHighFlash, uLowFlash, uTMPColor;\n" +
        "uniform float uThreshold, uSmooth, uFlashRate, uTime;\n" +
        "varying vec4 vCol;\n" +
        "varying vec4 vUV0;\n" +
        "void main() {\n" +
        "  vec4 t = texture2D(uTex, vUV0.xy);\n" +
        "  if (t.a <= 0.0) discard;\n" +
        "  float l = dot(t.rgb, vec3(0.299, 0.587, 0.114));\n" +
        "  float k = smoothstep(uThreshold - uSmooth, uThreshold + uSmooth, l);\n" +
        "  vec4 hi = uHigh, lo = uLow;\n" +
        "  if (uFlashRate > 0.0) {\n" +
        "    float f = step(0.5, fract(uTime * uFlashRate));\n" +
        "    hi = mix(uHigh, uHighFlash, f);\n" +
        "    lo = mix(uLow, uLowFlash, f);\n" +
        "  }\n" +
        "  vec4 c = mix(lo, hi, k) * uTMPColor * vCol;\n" +
        "  c.a *= t.a;\n" +
        "  gl_FragColor = c;\n" +
        "}\n";

    /** MultiTintShader: each source channel picks its own high/low tint */
    private static final String FS_MULTITINT =
        "precision mediump float;\n" +
        "uniform sampler2D uTex;\n" +
        "uniform vec4 uRH, uRL, uGH, uGL, uBH, uBL;\n" +
        "uniform float uRT, uRS, uGT, uGS, uBT, uBS;\n" +
        "varying vec4 vCol;\n" +
        "varying vec4 vUV0;\n" +
        "void main() {\n" +
        "  vec4 t = texture2D(uTex, vUV0.xy);\n" +
        "  if (t.a <= 0.0) discard;\n" +
        "  vec3 c = mix(uRL.rgb, uRH.rgb, smoothstep(uRT - uRS, uRT + uRS, t.r)) * t.r\n" +
        "         + mix(uGL.rgb, uGH.rgb, smoothstep(uGT - uGS, uGT + uGS, t.g)) * t.g\n" +
        "         + mix(uBL.rgb, uBH.rgb, smoothstep(uBT - uBS, uBT + uBS, t.b)) * t.b;\n" +
        "  gl_FragColor = vec4(c, t.a) * vCol;\n" +
        "}\n";

    private static final int UI = 0, SPRITE = 1, SDF = 2, MORPH = 3, MULTITINT = 4;
    private final int[] prog = new int[5];
    private final HashMap<String, Integer>[] uniforms = new HashMap[5];
    private final int[] aPos = new int[5], aCol = new int[5], aUV0 = new int[5], aUV1 = new int[5];

    // streamed vertex data
    private FloatBuffer posBuf, uv0Buf, uv1Buf;
    private ByteBuffer colBuf;
    private ShortBuffer idxBuf;
    private int w, h, vpX, vpY, vpW, vpH;
    private boolean scissorOn;

    // property ids
    private static final int P_MAINTEX = Shader.PropertyToID("_MainTex"), P_COLOR = Shader.PropertyToID("_Color"),
        P_FOGDIST = Shader.PropertyToID("_FogDistance"), P_FOGMUL = Shader.PropertyToID("_FogDistanceMultiplier"),
        P_EMISSION = Shader.PropertyToID("_EmissionPower"), P_SCROLL = Shader.PropertyToID("_Scrolling"),
        P_DAY = Shader.PropertyToID("_DayCycleMultiplier"), P_TRANSP = Shader.PropertyToID("_Transparency"),
        P_BLINKSTART = Shader.PropertyToID("_BlinkStart"), P_BLINKDUR = Shader.PropertyToID("_BlinkDuration"),
        P_LAYER = Shader.PropertyToID("_Layer"), P_FOGCOLOR = Shader.PropertyToID("_FogColor"),
        P_FACECOLOR = Shader.PropertyToID("_FaceColor"), P_OUTLINECOLOR = Shader.PropertyToID("_OutlineColor"),
        P_OUTLINEWIDTH = Shader.PropertyToID("_OutlineWidth"), P_OUTLINESOFT = Shader.PropertyToID("_OutlineSoftness"),
        P_FACEDILATE = Shader.PropertyToID("_FaceDilate"), P_GRADSCALE = Shader.PropertyToID("_GradientScale"),
        P_RATIOA = Shader.PropertyToID("_ScaleRatioA"), P_WEIGHT = Shader.PropertyToID("_WeightNormal"),
        P_UNDERLAYCOLOR = Shader.PropertyToID("_UnderlayColor"), P_UNDERLAYX = Shader.PropertyToID("_UnderlayOffsetX"),
        P_UNDERLAYY = Shader.PropertyToID("_UnderlayOffsetY"), P_UNDERLAYDIL = Shader.PropertyToID("_UnderlayDilate"),
        P_UNDERLAYSOFT = Shader.PropertyToID("_UnderlaySoftness"), P_TEXW = Shader.PropertyToID("_TextureWidth"),
        P_TEXH = Shader.PropertyToID("_TextureHeight"),
        P_HIGH = Shader.PropertyToID("_HighTint"), P_LOW = Shader.PropertyToID("_LowTint"),
        P_HIGHFLASH = Shader.PropertyToID("_HighTintFlash"), P_LOWFLASH = Shader.PropertyToID("_LowTintFlash"),
        P_FLASHRATE = Shader.PropertyToID("_FlashRate"), P_TMPCOLOR = Shader.PropertyToID("_TMPColor"),
        P_THRESHOLD = Shader.PropertyToID("_HighLowThreshold"), P_SMOOTH = Shader.PropertyToID("_Smoothness"),
        P_RH = Shader.PropertyToID("_RedHighTint"), P_RL = Shader.PropertyToID("_RedLowTint"),
        P_RT = Shader.PropertyToID("_RedHighLowThreshold"), P_RS = Shader.PropertyToID("_RedSmoothness"),
        P_GH = Shader.PropertyToID("_GreenHighTint"), P_GL = Shader.PropertyToID("_GreenLowTint"),
        P_GT = Shader.PropertyToID("_GreenHighLowThreshold"), P_GS = Shader.PropertyToID("_GreenSmoothness"),
        P_BH = Shader.PropertyToID("_BlueHighTint"), P_BL = Shader.PropertyToID("_BlueLowTint"),
        P_BT = Shader.PropertyToID("_BlueHighLowThreshold"), P_BS = Shader.PropertyToID("_BlueSmoothness");

    /** called on the GL thread after the context is (re)created */
    public void init() {
        for (int i = 0; i < 5; i++) {
            String fs = i == UI ? FS_UI : i == SPRITE ? FS_SPRITE : i == SDF ? FS_SDF : i == MORPH ? FS_MORPH : FS_MULTITINT;
            if (i == SDF) fs = "#extension GL_OES_standard_derivatives : enable\n" + fs;
            prog[i] = link(VS, fs);
            uniforms[i] = new HashMap<>();
            aPos[i] = GLES20.glGetAttribLocation(prog[i], "aPos");
            aCol[i] = GLES20.glGetAttribLocation(prog[i], "aCol");
            aUV0[i] = GLES20.glGetAttribLocation(prog[i], "aUV0");
            aUV1[i] = GLES20.glGetAttribLocation(prog[i], "aUV1");
        }
        texCache.clear();
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static int link(String vs, String fs) {
        int v = compile(GLES20.GL_VERTEX_SHADER, vs), f = compile(GLES20.GL_FRAGMENT_SHADER, fs);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) android.util.Log.e("prr", "link: " + GLES20.glGetProgramInfoLog(p));
        GLES20.glDeleteShader(v);
        GLES20.glDeleteShader(f);
        return p;
    }

    private static int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) android.util.Log.e("prr", "compile: " + GLES20.glGetShaderInfoLog(s) + "\n" + src);
        return s;
    }

    private int u(int p, String name) {
        Integer i = uniforms[p].get(name);
        if (i == null) {
            i = GLES20.glGetUniformLocation(prog[p], name);
            uniforms[p].put(name, i);
        }
        return i;
    }

    // ------------------------------------------------------------------ frame
    @Override public void begin(int width, int height) {
        w = width;
        h = height;
        scissorOn = false;
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        GLES20.glDisable(GLES20.GL_STENCIL_TEST);
        GLES20.glColorMask(true, true, true, true);
    }

    @Override public void viewport(int x, int y, int vw, int vh) {
        vpX = x; vpY = y; vpW = vw; vpH = vh;
        GLES20.glViewport(x, y, Math.max(0, vw), Math.max(0, vh));
    }

    @Override public void clear(boolean color, boolean depth, float r, float g, float b, float a) {
        int mask = 0;
        if (color) { GLES20.glClearColor(r, g, b, a); mask |= GLES20.GL_COLOR_BUFFER_BIT; }
        if (depth) mask |= GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT;
        if (mask != 0) {
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            GLES20.glClear(mask);
            if (scissorOn) GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
        }
    }

    @Override public void end() {
        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
        GLES20.glDisable(GLES20.GL_STENCIL_TEST);
    }

    private static int programFor(String name) {
        if (name == null) return UI;
        if (name.indexOf("Distance Field") >= 0) return SDF;
        if (name.indexOf("SpriteShader") >= 0 || name.indexOf("SpriteReflection") >= 0) return SPRITE;
        if (name.indexOf("MorphText") >= 0) return MORPH;
        if (name.indexOf("MultiTint") >= 0) return MULTITINT;
        return UI;
    }

    private float prop(Item it, int id, float def) {
        if (it.block != null) { float v = it.block.$float(id, Float.NaN); if (v == v) return v; }
        if (it.mat != null) { float v = it.mat.$props().$float(id, Float.NaN); if (v == v) return v; }
        return Shader.$globals().$float(id, def);
    }

    private float[] vec(Item it, int id, float[] def) {
        if (it.block != null) { float[] v = it.block.$vector(id); if (v != null) return v; }
        if (it.mat != null) { float[] v = it.mat.$props().$vector(id); if (v != null) return v; }
        float[] v = Shader.$globals().$vector(id);
        return v != null ? v : def;
    }

    private static final float[] WHITE = {1, 1, 1, 1}, CLEAR = {0, 0, 0, 0};

    @Override public void draw(Item it) {
        if (it.count == 0 || it.idxCount == 0 || it.pos == null) return;
        int p = programFor(it.shader);
        GLES20.glUseProgram(prog[p]);
        // geometry
        ensure(it.count, it.idxCount);
        posBuf.clear();
        posBuf.put(it.pos, 0, it.count * 3).position(0);
        GLES20.glEnableVertexAttribArray(aPos[p]);
        GLES20.glVertexAttribPointer(aPos[p], 3, GLES20.GL_FLOAT, false, 0, posBuf);
        colBuf.clear();
        if (it.col != null) for (int i = 0; i < it.count; i++) {
            int c = it.col[i];
            colBuf.put((byte) (c >> 16)).put((byte) (c >> 8)).put((byte) c).put((byte) (c >>> 24));
        } else for (int i = 0; i < it.count; i++) colBuf.putInt(0xFFFFFFFF);
        colBuf.position(0);
        GLES20.glEnableVertexAttribArray(aCol[p]);
        GLES20.glVertexAttribPointer(aCol[p], 4, GLES20.GL_UNSIGNED_BYTE, true, 0, colBuf);
        uv(p, aUV0[p], uv0Buf, it.uv0, it.count);
        uv(p, aUV1[p], uv1Buf, it.uv1, it.count);
        idxBuf.clear();
        for (int i = 0; i < it.idxCount; i++) idxBuf.put((short) it.idx[i]);
        idxBuf.position(0);
        // common uniforms
        GLES20.glUniformMatrix4fv(u(p, "uMVP"), 1, false, it.mvp, 0);
        GLES20.glUniformMatrix4fv(u(p, "uModel"), 1, false, it.model, 0);
        GLES20.glUniform4f(u(p, "uTint"), it.tint[0], it.tint[1], it.tint[2], it.tint[3]);
        float[] st = it.mat != null ? it.mat.$props().$texST(P_MAINTEX) : null;
        GLES20.glUniform4f(u(p, "uST"), st == null ? 1 : st[0], st == null ? 1 : st[1], st == null ? 0 : st[2], st == null ? 0 : st[3]);
        float scroll = prop(it, P_SCROLL, 0);
        GLES20.glUniform2f(u(p, "uScroll"), scroll == 0 ? 0 : scroll * cs.UnityEngine.Time.get_time(), 0);
        // texture
        Texture tex = it.tex;
        if (tex == null && it.mat != null) tex = it.mat.$props().$texture(P_MAINTEX);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture(tex));
        GLES20.glUniform1i(u(p, "uTex"), 0);
        switch (p) {
            case SPRITE: {
                float[] fog = vec(it, P_FOGCOLOR, CLEAR);
                GLES20.glUniform4f(u(p, "uFogColor"), fog[0], fog[1], fog[2], fog[3]);
                GLES20.glUniform1f(u(p, "uFogDistance"), prop(it, P_FOGDIST, 0));
                GLES20.glUniform1f(u(p, "uFogMul"), prop(it, P_FOGMUL, 1));
                GLES20.glUniform1f(u(p, "uEmission"), prop(it, P_EMISSION, 0));
                GLES20.glUniform1f(u(p, "uDayCycle"), prop(it, P_DAY, 0));
                GLES20.glUniform1f(u(p, "uTransparency"), prop(it, P_TRANSP, 1));
                GLES20.glUniform1f(u(p, "uLayer"), prop(it, P_LAYER, 0));
                float bs = prop(it, P_BLINKSTART, -1), bd = prop(it, P_BLINKDUR, 0);
                float blink = 0;
                if (bd > 0 && bs >= 0) {
                    float t = cs.UnityEngine.Time.get_time() - bs;
                    if (t >= 0 && t < bd) blink = (float) (0.5 + 0.5 * Math.sin(t * 40.0));
                }
                GLES20.glUniform1f(u(p, "uBlink"), blink);
                break;
            }
            case SDF: {
                float[] fc = vec(it, P_FACECOLOR, WHITE), oc = vec(it, P_OUTLINECOLOR, CLEAR), uc = vec(it, P_UNDERLAYCOLOR, CLEAR);
                GLES20.glUniform4f(u(p, "uFaceColor"), fc[0], fc[1], fc[2], fc[3]);
                GLES20.glUniform4f(u(p, "uOutlineColor"), oc[0], oc[1], oc[2], oc[3]);
                GLES20.glUniform4f(u(p, "uUnderlayColor"), uc[0], uc[1], uc[2], uc[3]);
                GLES20.glUniform1f(u(p, "uOutlineWidth"), prop(it, P_OUTLINEWIDTH, 0));
                GLES20.glUniform1f(u(p, "uOutlineSoftness"), prop(it, P_OUTLINESOFT, 0));
                GLES20.glUniform1f(u(p, "uFaceDilate"), prop(it, P_FACEDILATE, 0));
                GLES20.glUniform1f(u(p, "uGradientScale"), prop(it, P_GRADSCALE, 10));
                GLES20.glUniform1f(u(p, "uScaleRatioA"), prop(it, P_RATIOA, 1));
                GLES20.glUniform1f(u(p, "uWeight"), prop(it, P_WEIGHT, 0) * 0.01f);
                GLES20.glUniform2f(u(p, "uUnderlayOffset"), prop(it, P_UNDERLAYX, 0), prop(it, P_UNDERLAYY, 0));
                GLES20.glUniform1f(u(p, "uUnderlayDilate"), prop(it, P_UNDERLAYDIL, 0));
                GLES20.glUniform1f(u(p, "uUnderlaySoftness"), prop(it, P_UNDERLAYSOFT, 0));
                GLES20.glUniform1f(u(p, "uTexW"), Math.max(1, prop(it, P_TEXW, tex == null ? 1024 : tex.get_width())));
                GLES20.glUniform1f(u(p, "uTexH"), Math.max(1, prop(it, P_TEXH, tex == null ? 1024 : tex.get_height())));
                break;
            }
            case MORPH: {
                float[] hi = vec(it, P_HIGH, WHITE), lo = vec(it, P_LOW, WHITE);
                float[] hf = vec(it, P_HIGHFLASH, hi), lf = vec(it, P_LOWFLASH, lo), tc = vec(it, P_TMPCOLOR, WHITE);
                GLES20.glUniform4f(u(p, "uHigh"), hi[0], hi[1], hi[2], hi[3]);
                GLES20.glUniform4f(u(p, "uLow"), lo[0], lo[1], lo[2], lo[3]);
                GLES20.glUniform4f(u(p, "uHighFlash"), hf[0], hf[1], hf[2], hf[3]);
                GLES20.glUniform4f(u(p, "uLowFlash"), lf[0], lf[1], lf[2], lf[3]);
                GLES20.glUniform4f(u(p, "uTMPColor"), tc[0], tc[1], tc[2], tc[3]);
                GLES20.glUniform1f(u(p, "uThreshold"), prop(it, P_THRESHOLD, 0.5f));
                GLES20.glUniform1f(u(p, "uSmooth"), Math.max(0.001f, prop(it, P_SMOOTH, 0.05f)));
                GLES20.glUniform1f(u(p, "uFlashRate"), prop(it, P_FLASHRATE, 0));
                GLES20.glUniform1f(u(p, "uTime"), cs.UnityEngine.Time.get_time());
                break;
            }
            case MULTITINT: {
                bindTint(p, "uRH", vec(it, P_RH, WHITE));
                bindTint(p, "uRL", vec(it, P_RL, WHITE));
                bindTint(p, "uGH", vec(it, P_GH, WHITE));
                bindTint(p, "uGL", vec(it, P_GL, WHITE));
                bindTint(p, "uBH", vec(it, P_BH, WHITE));
                bindTint(p, "uBL", vec(it, P_BL, WHITE));
                GLES20.glUniform1f(u(p, "uRT"), prop(it, P_RT, .5f));
                GLES20.glUniform1f(u(p, "uRS"), Math.max(.001f, prop(it, P_RS, .05f)));
                GLES20.glUniform1f(u(p, "uGT"), prop(it, P_GT, .5f));
                GLES20.glUniform1f(u(p, "uGS"), Math.max(.001f, prop(it, P_GS, .05f)));
                GLES20.glUniform1f(u(p, "uBT"), prop(it, P_BT, .5f));
                GLES20.glUniform1f(u(p, "uBS"), Math.max(.001f, prop(it, P_BS, .05f)));
                break;
            }
        }
        // scissor / stencil / color mask
        if (it.scissor) {
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
            GLES20.glScissor(it.sx, it.sy, it.sw, it.sh);
            scissorOn = true;
        } else if (scissorOn) {
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            scissorOn = false;
        }
        if (it.stencilRef != 0 || it.stencilComp != 8) {
            GLES20.glEnable(GLES20.GL_STENCIL_TEST);
            GLES20.glStencilFunc(cmp(it.stencilComp), it.stencilRef, it.stencilRead);
            int op = stencilOp(it.stencilOp);
            GLES20.glStencilOp(op, op, op);
            GLES20.glStencilMask(it.stencilWrite);
        } else {
            GLES20.glDisable(GLES20.GL_STENCIL_TEST);
        }
        GLES20.glColorMask((it.colorMask & 8) != 0, (it.colorMask & 4) != 0, (it.colorMask & 2) != 0, (it.colorMask & 1) != 0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, it.idxCount, GLES20.GL_UNSIGNED_SHORT, idxBuf);
        GLES20.glColorMask(true, true, true, true);
    }

    private void bindTint(int p, String n, float[] v) { GLES20.glUniform4f(u(p, n), v[0], v[1], v[2], v[3]); }

    private void uv(int p, int attr, FloatBuffer buf, float[] data, int count) {
        if (attr < 0) return;
        buf.clear();
        if (data != null) buf.put(data, 0, Math.min(data.length, count * 4));
        for (int i = data == null ? 0 : data.length / 4; i < count; i++) buf.put(0).put(0).put(0).put(0);
        buf.position(0);
        GLES20.glEnableVertexAttribArray(attr);
        GLES20.glVertexAttribPointer(attr, 4, GLES20.GL_FLOAT, false, 0, buf);
    }

    /** CompareFunction: Disabled 0, Never 1, Less 2, Equal 3, LEqual 4, Greater 5, NotEqual 6, GEqual 7, Always 8 */
    private static int cmp(int c) {
        switch (c) {
            case 1: return GLES20.GL_NEVER;
            case 2: return GLES20.GL_LESS;
            case 3: return GLES20.GL_EQUAL;
            case 4: return GLES20.GL_LEQUAL;
            case 5: return GLES20.GL_GREATER;
            case 6: return GLES20.GL_NOTEQUAL;
            case 7: return GLES20.GL_GEQUAL;
            default: return GLES20.GL_ALWAYS;
        }
    }

    /** StencilOp: Keep 0, Zero 1, Replace 2, IncrSat 3, DecrSat 4, Invert 5 */
    private static int stencilOp(int o) {
        switch (o) {
            case 1: return GLES20.GL_ZERO;
            case 2: return GLES20.GL_REPLACE;
            case 3: return GLES20.GL_INCR;
            case 4: return GLES20.GL_DECR;
            case 5: return GLES20.GL_INVERT;
            default: return GLES20.GL_KEEP;
        }
    }

    private void ensure(int verts, int idx) {
        if (posBuf == null || posBuf.capacity() < verts * 3) {
            posBuf = alloc(Math.max(verts * 3, 4096));
            uv0Buf = alloc(Math.max(verts * 4, 4096));
            uv1Buf = alloc(Math.max(verts * 4, 4096));
            colBuf = ByteBuffer.allocateDirect(Math.max(verts * 4, 4096)).order(ByteOrder.nativeOrder());
        }
        if (uv0Buf.capacity() < verts * 4) {
            uv0Buf = alloc(verts * 4);
            uv1Buf = alloc(verts * 4);
        }
        if (colBuf.capacity() < verts * 4) colBuf = ByteBuffer.allocateDirect(verts * 4).order(ByteOrder.nativeOrder());
        if (idxBuf == null || idxBuf.capacity() < idx) idxBuf = ByteBuffer.allocateDirect(Math.max(idx, 4096) * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
    }

    private static FloatBuffer alloc(int floats) { return ByteBuffer.allocateDirect(floats * 4).order(ByteOrder.nativeOrder()).asFloatBuffer(); }

    // ------------------------------------------------------------------ textures
    private final HashMap<Texture, int[]> texCache = new HashMap<>();
    private int white;

    private int texture(Texture t) {
        if (t == null) return whiteTex();
        int[] e = texCache.get(t);
        if (e != null && e[1] == t.$version) return e[0];
        int id;
        if (e != null) id = e[0];
        else {
            int[] ids = new int[1];
            GLES20.glGenTextures(1, ids, 0);
            id = ids[0];
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        upload(t);
        int filter = t.get_filterMode() == 0 ? GLES20.GL_NEAREST : GLES20.GL_LINEAR;
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, filter);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, filter);
        int wrap = t.get_wrapMode() == 0 ? GLES20.GL_REPEAT : GLES20.GL_CLAMP_TO_EDGE;
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrap);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrap);
        texCache.put(t, new int[]{id, t.$version});
        return id;
    }

    private void upload(Texture t) {
        int tw = t.get_width(), th = t.get_height();
        if (t instanceof Texture2D) {
            Texture2D t2 = (Texture2D) t;
            if (t2.$compressed == null && t2.$compressedSrc != null && !t2.$hasPixels()) {
                byte[] b = cs.UnityEngine.Host.get().readData(t2.$compressedSrc);
                if (b != null) {
                    ByteBuffer bb = ByteBuffer.allocateDirect(b.length).order(ByteOrder.nativeOrder());
                    bb.put(b).position(0);
                    GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D, 0, t2.$compressedFormat, tw, th, 0, b.length, bb);
                    return;
                }
            }
            if (t2.$compressed != null) {
                int fmt = t2.$compressedFormat;
                for (int level = 0; level < t2.$compressed.length; level++) {
                    byte[] b = t2.$compressed[level];
                    if (b == null) continue;
                    ByteBuffer bb = ByteBuffer.allocateDirect(b.length).order(ByteOrder.nativeOrder());
                    bb.put(b).position(0);
                    GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D, level, fmt, Math.max(1, tw >> level), Math.max(1, th >> level), 0, b.length, bb);
                }
                return;
            }
            int[] px = t2.$pixels();
            ByteBuffer bb = ByteBuffer.allocateDirect(Math.max(4, px.length * 4)).order(ByteOrder.nativeOrder());
            for (int i = 0; i < px.length; i++) {
                int c = px[i];
                bb.put((byte) (c >> 16)).put((byte) (c >> 8)).put((byte) c).put((byte) (c >>> 24));
            }
            bb.position(0);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, Math.max(1, tw), Math.max(1, th), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);
            return;
        }
        ByteBuffer bb = ByteBuffer.allocateDirect(Math.max(4, tw * th * 4)).order(ByteOrder.nativeOrder());
        while (bb.remaining() > 0) bb.putInt(0xFFFFFFFF);
        bb.position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, Math.max(1, tw), Math.max(1, th), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);
    }

    private int whiteTex() {
        if (white != 0) return white;
        int[] ids = new int[1];
        GLES20.glGenTextures(1, ids, 0);
        white = ids[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, white);
        ByteBuffer bb = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        bb.putInt(0xFFFFFFFF).position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        return white;
    }

    @Override public void release(Texture t) {
        int[] e = texCache.remove(t);
        if (e != null) GLES20.glDeleteTextures(1, new int[]{e[0]}, 0);
    }

    @Override public int[] readPixels(int x, int y, int rw, int rh) {
        if (rw <= 0 || rh <= 0) return null;
        ByteBuffer bb = ByteBuffer.allocateDirect(rw * rh * 4).order(ByteOrder.nativeOrder());
        GLES20.glReadPixels(x, y, rw, rh, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, bb);
        int[] out = new int[rw * rh];
        bb.position(0);
        for (int i = 0; i < out.length; i++) {
            int r = bb.get() & 0xff, g = bb.get() & 0xff, b = bb.get() & 0xff, a = bb.get() & 0xff;
            out[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return out;
    }
}
