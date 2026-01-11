// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.sprite;

import gnu.trove.map.hash.TIntObjectHashMap;
import imgui.ImDrawData;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import zombie.core.Color;
import zombie.core.SpriteRenderer;
import zombie.core.Styles.AbstractStyle;
import zombie.core.Styles.Style;
import zombie.core.Styles.TransparentStyle;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.opengl.ShaderUniformSetter;
import zombie.core.profiling.PerformanceProfileProbe;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.iso.fboRenderChunk.FBORenderChunk;

public abstract class GenericSpriteRenderState {
    public final int index;
    public TextureDraw[] sprite = new TextureDraw[2048];
    public Style[] style = new Style[2048];
    public int numSprites;
    public TextureFBO fbo;
    public boolean rendered;
    private boolean isRendering;
    public final ArrayList<TextureDraw> postRender = new ArrayList<>();
    public AbstractStyle defaultStyle = TransparentStyle.instance;
    public boolean cursorVisible = true;
    public final TIntObjectHashMap<FBORenderChunk> cachedRenderChunkIndexMap = new TIntObjectHashMap<>();
    public static final byte UVCA_NONE = -1;
    public static final byte UVCA_CIRCLE = 1;
    public static final byte UVCA_NOCIRCLE = 2;
    public static final byte UVCA_DEPTHTEXTURE = 3;
    private byte useVertColorsArray = -1;
    private int texture2Color0;
    private int texture2Color1;
    private int texture2Color2;
    private int texture2Color3;
    private SpriteRenderer.WallShaderTexRender wallShaderTexRender;
    private Texture texture1Cutaway;
    private int texture1CutawayX;
    private int texture1CutawayY;
    private int texture1CutawayW;
    private int texture1CutawayH;
    private Texture texture2Cutaway;

    protected GenericSpriteRenderState(int index) {
        this.index = index;

        for (int n = 0; n < this.sprite.length; n++) {
            this.sprite[n] = new TextureDraw();
        }
    }

    public void onRendered() {
        this.isRendering = false;
        this.rendered = true;
    }

    public void onRenderAcquired() {
        this.isRendering = true;
    }

    public boolean isRendering() {
        return this.isRendering;
    }

    public void onReady() {
        this.rendered = false;
    }

    public boolean isReady() {
        return !this.rendered;
    }

    public boolean isRendered() {
        return this.rendered;
    }

    public void CheckSpriteSlots() {
        if (this.numSprites == this.sprite.length) {
            TextureDraw[] old_sprite = this.sprite;
            this.sprite = new TextureDraw[this.numSprites * 3 / 2 + 1];

            for (int n = this.numSprites; n < this.sprite.length; n++) {
                this.sprite[n] = new TextureDraw();
            }

            System.arraycopy(old_sprite, 0, this.sprite, 0, this.numSprites);
            Style[] old_style = this.style;
            this.style = new Style[this.numSprites * 3 / 2 + 1];
            System.arraycopy(old_style, 0, this.style, 0, this.numSprites);
        }
    }

    public static void clearSprites(List<TextureDraw> postRender) {
        for (int i = 0; i < postRender.size(); i++) {
            postRender.get(i).postRender();
        }

        postRender.clear();
    }

    public void clear() {
        clearSprites(this.postRender);
        this.numSprites = 0;
        this.cachedRenderChunkIndexMap.clear();
    }

    public void glDepthMask(boolean b) {
        this.CheckSpriteSlots();
        TextureDraw.glDepthMask(this.sprite[this.numSprites], b);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void renderflipped(Texture tex, float x, float y, float width, float height, float r, float g, float b, float a, Consumer<TextureDraw> texdModifier) {
        this.render(tex, x, y, width, height, r, g, b, a, texdModifier);
        this.sprite[this.numSprites - 1].flipped = true;
    }

    public void drawSkyBox(Shader shader, int playerIndex, int apiId, int bufferId) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.drawSkyBox(this.sprite[this.numSprites], shader, playerIndex, apiId, bufferId);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void drawWater(Shader shader, int playerIndex, int apiId, boolean bShore) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        shader.startMainThread(this.sprite[this.numSprites], playerIndex);
        TextureDraw.drawWater(this.sprite[this.numSprites], shader, playerIndex, apiId, bShore);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void drawPuddles(int playerIndex, int z, int firstSquare, int numSquares) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.drawPuddles(this.sprite[this.numSprites], playerIndex, z, firstSquare, numSquares);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void drawParticles(int playerIndex, int var1, int var2) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.drawParticles(this.sprite[this.numSprites], playerIndex, var1, var2);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void glDisable(int a) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glDisable(this.sprite[this.numSprites], a);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void NewFrame() {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.NewFrame(this.sprite[this.numSprites]);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glDepthFunc(int a) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glDepthFunc(this.sprite[this.numSprites], a);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glEnable(int a) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glEnable(this.sprite[this.numSprites], a);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glStencilMask(int a) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glStencilMask(this.sprite[this.numSprites], a);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glClear(int a) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glClear(this.sprite[this.numSprites], a);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glBindFramebuffer(int binding, int fbo) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glBindFramebuffer(this.sprite[this.numSprites], binding, fbo);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glClearColor(int r, int g, int b, int a) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glClearColor(this.sprite[this.numSprites], r, g, b, a);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glClearDepth(float d) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glClearDepth(this.sprite[this.numSprites], d);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glStencilFunc(int a, int b, int c) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glStencilFunc(this.sprite[this.numSprites], a, b, c);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glStencilOp(int a, int b, int c) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glStencilOp(this.sprite[this.numSprites], a, b, c);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glColorMask(int a, int b, int c, int d) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glColorMask(this.sprite[this.numSprites], a, b, c, d);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glAlphaFunc(int a, float b) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glAlphaFunc(this.sprite[this.numSprites], a, b);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glBlendFunc(int a, int b) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glBlendFunc(this.sprite[this.numSprites], a, b);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glBlendFuncSeparate(int a, int b, int c, int d) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glBlendFuncSeparate(this.sprite[this.numSprites], a, b, c, d);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glBlendEquation(int a) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glBlendEquation(this.sprite[this.numSprites], a);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void render(
        Texture tex,
        double x1,
        double y1,
        double x2,
        double y2,
        double x3,
        double y3,
        double x4,
        double y4,
        double depth,
        float r,
        float g,
        float b,
        float a,
        Consumer<TextureDraw> texdModifier
    ) {
        this.render(tex, x1, y1, x2, y2, x3, y3, x4, y4, depth, r, g, b, a, r, g, b, a, r, g, b, a, r, g, b, a, texdModifier);
    }

    public void render(
        Texture tex,
        double x1,
        double y1,
        double x2,
        double y2,
        double x3,
        double y3,
        double x4,
        double y4,
        float r,
        float g,
        float b,
        float a,
        Consumer<TextureDraw> texdModifier
    ) {
        this.render(tex, x1, y1, x2, y2, x3, y3, x4, y4, r, g, b, a, r, g, b, a, r, g, b, a, r, g, b, a, texdModifier);
    }

    public void render(
        Texture tex,
        double x1,
        double y1,
        double x2,
        double y2,
        double x3,
        double y3,
        double x4,
        double y4,
        double u1,
        double v1,
        double u2,
        double v2,
        double u3,
        double v3,
        double u4,
        double v4,
        float r,
        float g,
        float b,
        float a
    ) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        this.sprite[this.numSprites].reset();
        int c = Color.colorToABGR(r, g, b, a);
        TextureDraw.Create(
            this.sprite[this.numSprites],
            tex,
            (float)x1,
            (float)y1,
            (float)x2,
            (float)y2,
            (float)x3,
            (float)y3,
            (float)x4,
            (float)y4,
            c,
            c,
            c,
            c,
            (float)u1,
            (float)v1,
            (float)u2,
            (float)v2,
            (float)u3,
            (float)v3,
            (float)u4,
            (float)v4,
            null
        );
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void render(
        Texture tex,
        double x1,
        double y1,
        double x2,
        double y2,
        double x3,
        double y3,
        double x4,
        double y4,
        float r1,
        float g1,
        float b1,
        float a1,
        float r2,
        float g2,
        float b2,
        float a2,
        float r3,
        float g3,
        float b3,
        float a3,
        float r4,
        float g4,
        float b4,
        float a4,
        Consumer<TextureDraw> texdModifier
    ) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        this.sprite[this.numSprites].reset();
        TextureDraw.Create(
            this.sprite[this.numSprites],
            tex,
            (float)x1,
            (float)y1,
            (float)x2,
            (float)y2,
            (float)x3,
            (float)y3,
            (float)x4,
            (float)y4,
            r1,
            g1,
            b1,
            a1,
            r2,
            g2,
            b2,
            a2,
            r3,
            g3,
            b3,
            a3,
            r4,
            g4,
            b4,
            a4,
            texdModifier
        );
        if (this.useVertColorsArray != -1) {
            TextureDraw texd = this.sprite[this.numSprites];
            texd.useAttribArray = this.useVertColorsArray;
            texd.tex1Col0 = this.texture2Color0;
            texd.tex1Col1 = this.texture2Color1;
            texd.tex1Col2 = this.texture2Color2;
            texd.tex1Col3 = this.texture2Color3;
        }

        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void render(
        Texture tex,
        double x1,
        double y1,
        double x2,
        double y2,
        double x3,
        double y3,
        double x4,
        double y4,
        double depth,
        float r1,
        float g1,
        float b1,
        float a1,
        float r2,
        float g2,
        float b2,
        float a2,
        float r3,
        float g3,
        float b3,
        float a3,
        float r4,
        float g4,
        float b4,
        float a4,
        Consumer<TextureDraw> texdModifier
    ) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        this.sprite[this.numSprites].reset();
        TextureDraw.Create(
            this.sprite[this.numSprites],
            tex,
            (float)x1,
            (float)y1,
            (float)x2,
            (float)y2,
            (float)x3,
            (float)y3,
            (float)x4,
            (float)y4,
            r1,
            g1,
            b1,
            a1,
            r2,
            g2,
            b2,
            a2,
            r3,
            g3,
            b3,
            a3,
            r4,
            g4,
            b4,
            a4,
            texdModifier
        );
        TextureDraw texd = this.sprite[this.numSprites];
        if (this.useVertColorsArray != -1) {
            texd.useAttribArray = this.useVertColorsArray;
            texd.tex1Col0 = this.texture2Color0;
            texd.tex1Col1 = this.texture2Color1;
            texd.tex1Col2 = this.texture2Color2;
            texd.tex1Col3 = this.texture2Color3;
        }

        texd.z = (float)depth;
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void setUseVertColorsArray(byte whichShader, int c0, int c1, int c2, int c3) {
        this.useVertColorsArray = whichShader;
        this.texture2Color0 = c0;
        this.texture2Color1 = c1;
        this.texture2Color2 = c2;
        this.texture2Color3 = c3;
    }

    public void clearUseVertColorsArray() {
        this.useVertColorsArray = -1;
    }

    public void renderdebug(
        Texture tex,
        float x1,
        float y1,
        float x2,
        float y2,
        float x3,
        float y3,
        float x4,
        float y4,
        float r1,
        float g1,
        float b1,
        float a1,
        float r2,
        float g2,
        float b2,
        float a2,
        float r3,
        float g3,
        float b3,
        float a3,
        float r4,
        float g4,
        float b4,
        float a4,
        Consumer<TextureDraw> texdModifier
    ) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        this.sprite[this.numSprites].reset();
        TextureDraw.Create(
            this.sprite[this.numSprites], tex, x1, y1, x2, y2, x3, y3, x4, y4, r1, g1, b1, a1, r2, g2, b2, a2, r3, g3, b3, a3, r4, g4, b4, a4, texdModifier
        );
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void renderline(Texture tex, float x1, float y1, float x2, float y2, float r, float g, float b, float a, float thickness) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        this.sprite[this.numSprites].reset();
        if (x1 <= x2 && y1 <= y2) {
            TextureDraw.Create(
                this.sprite[this.numSprites],
                tex,
                x1 + thickness,
                y1 - thickness,
                x2 + thickness,
                y2 - thickness,
                x2 - thickness,
                y2 + thickness,
                x1 - thickness,
                y1 + thickness,
                r,
                g,
                b,
                a
            );
        } else if (x1 >= x2 && y1 >= y2) {
            TextureDraw.Create(
                this.sprite[this.numSprites],
                tex,
                x1 + thickness,
                y1 - thickness,
                x1 - thickness,
                y1 + thickness,
                x2 - thickness,
                y2 + thickness,
                x2 + thickness,
                y2 - thickness,
                r,
                g,
                b,
                a
            );
        } else if (x1 >= x2 && y1 <= y2) {
            TextureDraw.Create(
                this.sprite[this.numSprites],
                tex,
                x2 - thickness,
                y2 - thickness,
                x1 - thickness,
                y1 - thickness,
                x1 + thickness,
                y1 + thickness,
                x2 + thickness,
                y2 + thickness,
                r,
                g,
                b,
                a
            );
        } else if (x1 <= x2 && y1 >= y2) {
            TextureDraw.Create(
                this.sprite[this.numSprites],
                tex,
                x1 - thickness,
                y1 - thickness,
                x1 + thickness,
                y1 + thickness,
                x2 + thickness,
                y2 + thickness,
                x2 - thickness,
                y2 - thickness,
                r,
                g,
                b,
                a
            );
        }

        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void renderline(Texture tex, float x1, float y1, float x2, float y2, float r, float g, float b, float a, float baseThickness, float topThickness) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        this.sprite[this.numSprites].reset();
        if (x1 <= x2 && y1 <= y2) {
            TextureDraw.Create(
                this.sprite[this.numSprites],
                tex,
                x1 + baseThickness,
                y1 - baseThickness,
                x2 + topThickness,
                y2 - topThickness,
                x2 - topThickness,
                y2 + topThickness,
                x1 - baseThickness,
                y1 + baseThickness,
                r,
                g,
                b,
                a
            );
        } else if (x1 >= x2 && y1 >= y2) {
            TextureDraw.Create(
                this.sprite[this.numSprites],
                tex,
                x1 + baseThickness,
                y1 - baseThickness,
                x1 - baseThickness,
                y1 + baseThickness,
                x2 - topThickness,
                y2 + topThickness,
                x2 + topThickness,
                y2 - topThickness,
                r,
                g,
                b,
                a
            );
        } else if (x1 >= x2 && y1 <= y2) {
            TextureDraw.Create(
                this.sprite[this.numSprites],
                tex,
                x2 - topThickness,
                y2 - topThickness,
                x1 - baseThickness,
                y1 - baseThickness,
                x1 + baseThickness,
                y1 + baseThickness,
                x2 + topThickness,
                y2 + topThickness,
                r,
                g,
                b,
                a
            );
        } else if (x1 <= x2 && y1 >= y2) {
            TextureDraw.Create(
                this.sprite[this.numSprites],
                tex,
                x1 - baseThickness,
                y1 - baseThickness,
                x1 + baseThickness,
                y1 + baseThickness,
                x2 + topThickness,
                y2 + topThickness,
                x2 - topThickness,
                y2 - topThickness,
                r,
                g,
                b,
                a
            );
        }

        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void renderline(Texture tex, int x1, int y1, int x2, int y2, float r, float g, float b, float a) {
        this.renderline(tex, x1, y1, x2, y2, r, g, b, a, 1.0F);
    }

    public void renderlinef(Texture tex, float x1, float y1, float x2, float y2, float r, float g, float b, float a, int thickness) {
        this.renderline(tex, x1, y1, x2, y2, r, g, b, a, thickness);
    }

    public void renderlinef(Texture tex, float x1, float y1, float x2, float y2, float r, float g, float b, float a, float baseThickness, float topThickness) {
        this.renderline(tex, x1, y1, x2, y2, r, g, b, a, baseThickness, topThickness);
    }

    public void render(Texture tex, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int c1, int c2, int c3, int c4) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        this.sprite[this.numSprites].reset();
        TextureDraw.Create(this.sprite[this.numSprites], tex, x1, y1, x2, y2, x3, y3, x4, y4, c1, c2, c3, c4);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void render(Texture tex, float x, float y, float width, float height, float r, float g, float b, float a, Consumer<TextureDraw> texdModifier) {
        if (tex == null || tex.isReady()) {
            if (a != 0.0F) {
                if (this.numSprites == this.sprite.length) {
                    this.CheckSpriteSlots();
                }

                this.sprite[this.numSprites].reset();
                TextureDraw texd;
                if (this.texture1Cutaway == null && this.texture2Cutaway == null) {
                    int col0 = Color.colorToABGR(r, g, b, a);
                    float x2 = x + width;
                    float y2 = y + height;
                    if (this.wallShaderTexRender == null) {
                        texd = TextureDraw.Create(this.sprite[this.numSprites], tex, x, y, x2, y, x2, y2, x, y2, col0, col0, col0, col0, texdModifier);
                    } else {
                        texd = TextureDraw.Create(this.sprite[this.numSprites], tex, this.wallShaderTexRender, x, y, x2 - x, y2 - y, r, g, b, a, texdModifier);
                    }
                } else {
                    Texture tex1 = this.texture1Cutaway;
                    Texture tex2 = this.texture2Cutaway;
                    float tex0_offsetX = tex == null ? 0.0F : tex.offsetX;
                    float tex0_offsetY = tex == null ? 0.0F : tex.offsetY;
                    int tex0_width = tex == null ? 128 : tex.getWidth();
                    int tex0_height = tex == null ? 256 : tex.getHeight();
                    float tex1_offsetX = tex1 == null ? 0.0F : this.texture1CutawayX % 128;
                    float tex1_offsetY = tex1 == null ? 0.0F : this.texture1CutawayY % 226;
                    int tex1_width = tex1 == null ? 128 : this.texture1CutawayW;
                    int tex1_height = tex1 == null ? 226 : this.texture1CutawayH;
                    float tex2_offsetX = tex2 == null ? 0.0F : tex2.offsetX;
                    float tex2_offsetY = tex2 == null ? 0.0F : tex2.offsetY;
                    int tex2_width = tex2 == null ? 128 : tex2.getWidth();
                    int tex2_height = tex2 == null ? 256 : tex2.getHeight();
                    float _x3;
                    float _x0 = _x3 = PZMath.max(tex0_offsetX, tex1_offsetX, tex2_offsetX);
                    float _x2;
                    float _x1 = _x2 = PZMath.min(tex0_offsetX + tex0_width, tex1_offsetX + tex1_width, tex2_offsetX + tex2_width);
                    float _y1;
                    float _y0 = _y1 = PZMath.max(tex0_offsetY, tex1_offsetY, tex2_offsetY);
                    float _y3;
                    float _y2 = _y3 = PZMath.min(tex0_offsetY + tex0_height, tex1_offsetY + tex1_height, tex2_offsetY + tex2_height);
                    if (this.wallShaderTexRender == SpriteRenderer.WallShaderTexRender.LeftOnly) {
                        _x1 = _x2 = PZMath.min(_x1, 63.0F);
                    }

                    if (this.wallShaderTexRender == SpriteRenderer.WallShaderTexRender.RightOnly) {
                        _x0 = _x3 = PZMath.max(_x0, 63.0F);
                    }

                    int col0 = Color.colorToABGR(r, g, b, a);
                    float u0 = 0.0F;
                    float v0 = 0.0F;
                    float u1 = 1.0F;
                    float v1 = 0.0F;
                    float u2 = 1.0F;
                    float v2 = 1.0F;
                    float u3 = 0.0F;
                    float v3 = 1.0F;
                    if (tex != null) {
                        int widthHW = tex.getWidthHW();
                        int heightHW = tex.getHeightHW();
                        u0 = (tex.getXStart() * widthHW + (_x0 - tex0_offsetX)) / widthHW;
                        u1 = (tex.getXStart() * widthHW + (_x1 - tex0_offsetX)) / widthHW;
                        u2 = (tex.getXStart() * widthHW + (_x2 - tex0_offsetX)) / widthHW;
                        u3 = (tex.getXStart() * widthHW + (_x3 - tex0_offsetX)) / widthHW;
                        v0 = (tex.getYStart() * heightHW + (_y0 - tex0_offsetY)) / heightHW;
                        v1 = (tex.getYStart() * heightHW + (_y1 - tex0_offsetY)) / heightHW;
                        v2 = (tex.getYStart() * heightHW + (_y2 - tex0_offsetY)) / heightHW;
                        v3 = (tex.getYStart() * heightHW + (_y3 - tex0_offsetY)) / heightHW;
                    }

                    float left = x - tex0_offsetX;
                    float top = y - tex0_offsetY;
                    texd = TextureDraw.Create(
                        this.sprite[this.numSprites],
                        tex,
                        left + _x0,
                        top + _y0,
                        left + _x1,
                        top + _y1,
                        left + _x2,
                        top + _y2,
                        left + _x3,
                        top + _y3,
                        col0,
                        col0,
                        col0,
                        col0,
                        u0,
                        v0,
                        u1,
                        v1,
                        u2,
                        v2,
                        u3,
                        v3,
                        texdModifier
                    );
                    if (tex1 != null) {
                        int widthHW = tex1.getWidthHW();
                        int heightHW = tex1.getHeightHW();
                        texd.tex1 = tex1;
                        texd.tex1U0 = (this.texture1CutawayX + (_x0 - tex1_offsetX)) / widthHW;
                        texd.tex1U1 = (this.texture1CutawayX + (_x1 - tex1_offsetX)) / widthHW;
                        texd.tex1U2 = (this.texture1CutawayX + (_x2 - tex1_offsetX)) / widthHW;
                        texd.tex1U3 = (this.texture1CutawayX + (_x3 - tex1_offsetX)) / widthHW;
                        texd.tex1V0 = (this.texture1CutawayY + (_y0 - tex1_offsetY)) / heightHW;
                        texd.tex1V1 = (this.texture1CutawayY + (_y1 - tex1_offsetY)) / heightHW;
                        texd.tex1V2 = (this.texture1CutawayY + (_y2 - tex1_offsetY)) / heightHW;
                        texd.tex1V3 = (this.texture1CutawayY + (_y3 - tex1_offsetY)) / heightHW;
                    }

                    if (tex2 != null) {
                        int widthHW = tex2.getWidthHW();
                        int heightHW = tex2.getHeightHW();
                        texd.tex2 = tex2;
                        texd.tex2U0 = (tex2.getXStart() * widthHW + (_x0 - tex2_offsetX)) / widthHW;
                        texd.tex2U1 = (tex2.getXStart() * widthHW + (_x1 - tex2_offsetX)) / widthHW;
                        texd.tex2U2 = (tex2.getXStart() * widthHW + (_x2 - tex2_offsetX)) / widthHW;
                        texd.tex2U3 = (tex2.getXStart() * widthHW + (_x3 - tex2_offsetX)) / widthHW;
                        texd.tex2V0 = (tex2.getYStart() * heightHW + (_y0 - tex2_offsetY)) / heightHW;
                        texd.tex2V1 = (tex2.getYStart() * heightHW + (_y1 - tex2_offsetY)) / heightHW;
                        texd.tex2V2 = (tex2.getYStart() * heightHW + (_y2 - tex2_offsetY)) / heightHW;
                        texd.tex2V3 = (tex2.getYStart() * heightHW + (_y3 - tex2_offsetY)) / heightHW;
                    }
                }

                if (this.useVertColorsArray != -1) {
                    texd.useAttribArray = this.useVertColorsArray;
                    texd.tex1Col0 = this.texture2Color0;
                    texd.tex1Col1 = this.texture2Color1;
                    texd.tex1Col2 = this.texture2Color2;
                    texd.tex1Col3 = this.texture2Color3;
                }

                this.style[this.numSprites] = this.defaultStyle;
                this.numSprites++;
            }
        }
    }

    public void render(
        Texture tex, Texture tex2, float x, float y, float width, float height, float r, float g, float b, float a, Consumer<TextureDraw> texdModifier
    ) {
        if (tex == null || tex.isReady()) {
            if (a != 0.0F) {
                if (this.numSprites == this.sprite.length) {
                    this.CheckSpriteSlots();
                }

                this.sprite[this.numSprites].reset();
                int col0 = Color.colorToABGR(r, g, b, a);
                float x2 = x + width;
                float y2 = y + height;
                TextureDraw texd;
                if (this.wallShaderTexRender == null) {
                    texd = TextureDraw.Create(this.sprite[this.numSprites], tex, x, y, x2, y, x2, y2, x, y2, col0, col0, col0, col0, texdModifier);
                } else {
                    texd = TextureDraw.Create(this.sprite[this.numSprites], tex, this.wallShaderTexRender, x, y, x2 - x, y2 - y, r, g, b, a, texdModifier);
                }

                texd.tex1 = tex2;
                texd.tex1U0 = tex2.getXStart();
                texd.tex1U1 = tex2.getXEnd();
                texd.tex1U2 = tex2.getXEnd();
                texd.tex1U3 = tex2.getXStart();
                texd.tex1V0 = tex2.getYStart();
                texd.tex1V1 = tex2.getYStart();
                texd.tex1V2 = tex2.getYEnd();
                texd.tex1V3 = tex2.getYEnd();
                if (this.useVertColorsArray != -1) {
                    texd.useAttribArray = this.useVertColorsArray;
                    texd.tex1Col0 = this.texture2Color0;
                    texd.tex1Col1 = this.texture2Color1;
                    texd.tex1Col2 = this.texture2Color2;
                    texd.tex1Col3 = this.texture2Color3;
                }

                if (this.texture1Cutaway != null) {
                    texd.tex1 = this.texture1Cutaway;
                    float uSpan = this.texture1Cutaway.xEnd - this.texture1Cutaway.xStart;
                    float vSpan = this.texture1Cutaway.yEnd - this.texture1Cutaway.yStart;
                    float u1 = (float)this.texture1CutawayX / this.texture1Cutaway.getWidth();
                    float u2 = (float)(this.texture1CutawayX + this.texture1CutawayW) / this.texture1Cutaway.getWidth();
                    float v1 = (float)this.texture1CutawayY / this.texture1Cutaway.getHeight();
                    float v2 = (float)(this.texture1CutawayY + this.texture1CutawayH) / this.texture1Cutaway.getHeight();
                    texd.tex1U0 = texd.tex1U3 = this.texture1Cutaway.xStart + u1 * uSpan;
                    texd.tex1V0 = texd.tex1V1 = this.texture1Cutaway.yStart + v1 * vSpan;
                    texd.tex1U1 = texd.tex1U2 = this.texture1Cutaway.xStart + u2 * uSpan;
                    texd.tex1V2 = texd.tex1V3 = this.texture1Cutaway.yStart + v2 * vSpan;
                }

                if (this.texture2Cutaway != null) {
                    texd.tex2 = this.texture2Cutaway;
                    texd.tex2U0 = texd.tex2U3 = this.texture2Cutaway.xStart;
                    texd.tex2V0 = texd.tex2V1 = this.texture2Cutaway.yStart;
                    texd.tex2U1 = texd.tex2U2 = this.texture2Cutaway.xEnd;
                    texd.tex2V2 = texd.tex2V3 = this.texture2Cutaway.yEnd;
                    if (this.wallShaderTexRender == SpriteRenderer.WallShaderTexRender.LeftOnly) {
                        texd.tex2U1 = texd.tex2U0 * 0.5F + texd.tex2U1 * 0.5F;
                        texd.tex2U2 = texd.tex2U2 * 0.5F + texd.tex2U3 * 0.5F;
                    }

                    if (this.wallShaderTexRender == SpriteRenderer.WallShaderTexRender.RightOnly) {
                        texd.tex2U0 = texd.tex2U0 * 0.5F + texd.tex2U1 * 0.5F;
                        texd.tex2U3 = texd.tex2U2 * 0.5F + texd.tex2U3 * 0.5F;
                    }
                }

                this.style[this.numSprites] = this.defaultStyle;
                this.numSprites++;
            }
        }
    }

    public void renderRect(int x, int y, int width, int height, float r, float g, float b, float a) {
        if (a != 0.0F) {
            if (this.numSprites == this.sprite.length) {
                this.CheckSpriteSlots();
            }

            this.sprite[this.numSprites].reset();
            TextureDraw.Create(this.sprite[this.numSprites], null, x, y, width, height, r, g, b, a, null);
            this.style[this.numSprites] = this.defaultStyle;
            this.numSprites++;
        }
    }

    public void renderPoly(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float r, float g, float b, float a) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        this.sprite[this.numSprites].reset();
        TextureDraw.Create(this.sprite[this.numSprites], null, x1, y1, x2, y2, x3, y3, x4, y4, r, g, b, a);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void renderPoly(Texture tex, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float r, float g, float b, float a) {
        if (tex == null || tex.isReady()) {
            if (this.numSprites == this.sprite.length) {
                this.CheckSpriteSlots();
            }

            this.sprite[this.numSprites].reset();
            TextureDraw.Create(this.sprite[this.numSprites], tex, x1, y1, x2, y2, x3, y3, x4, y4, r, g, b, a);
            if (tex != null) {
                float xend = tex.getXEnd();
                float xstart = tex.getXStart();
                float yend = tex.getYEnd();
                float ystart = tex.getYStart();
                TextureDraw texd = this.sprite[this.numSprites];
                texd.u0 = xstart;
                texd.u1 = xend;
                texd.u2 = xend;
                texd.u3 = xstart;
                texd.v0 = ystart;
                texd.v1 = ystart;
                texd.v2 = yend;
                texd.v3 = yend;
            }

            this.style[this.numSprites] = this.defaultStyle;
            this.numSprites++;
        }
    }

    public void renderPoly(
        Texture tex,
        float x1,
        float y1,
        float x2,
        float y2,
        float x3,
        float y3,
        float x4,
        float y4,
        float r,
        float g,
        float b,
        float a,
        float u1,
        float v1,
        float u2,
        float v2,
        float u3,
        float v3,
        float u4,
        float v4
    ) {
        if (tex == null || tex.isReady()) {
            if (this.numSprites == this.sprite.length) {
                this.CheckSpriteSlots();
            }

            this.sprite[this.numSprites].reset();
            TextureDraw.Create(this.sprite[this.numSprites], tex, x1, y1, x2, y2, x3, y3, x4, y4, r, g, b, a);
            if (tex != null) {
                TextureDraw texd = this.sprite[this.numSprites];
                texd.u0 = u1;
                texd.u1 = u2;
                texd.u2 = u3;
                texd.u3 = u4;
                texd.v0 = v1;
                texd.v1 = v2;
                texd.v2 = v3;
                texd.v3 = v4;
            }

            this.style[this.numSprites] = this.defaultStyle;
            this.numSprites++;
        }
    }

    public void render(
        Texture tex,
        float x,
        float y,
        float width,
        float height,
        float r,
        float g,
        float b,
        float a,
        float u1,
        float v1,
        float u2,
        float v2,
        float u3,
        float v3,
        float u4,
        float v4,
        Consumer<TextureDraw> texdModifier
    ) {
        if (a != 0.0F) {
            if (this.numSprites == this.sprite.length) {
                this.CheckSpriteSlots();
            }

            this.sprite[this.numSprites].reset();
            TextureDraw.Create(this.sprite[this.numSprites], tex, x, y, width, height, r, g, b, a, u1, v1, u2, v2, u3, v3, u4, v4, texdModifier);
            this.style[this.numSprites] = this.defaultStyle;
            this.numSprites++;
        }
    }

    public void glBuffer(int i, int p) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glBuffer(this.sprite[this.numSprites], i, p);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glDoStartFrame(int w, int h, float zoom, int player) {
        this.glDoStartFrame(w, h, zoom, player, false);
    }

    public void glDoStartFrame(int w, int h, float zoom, int player, boolean isTextFrame) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glDoStartFrame(this.sprite[this.numSprites], w, h, zoom, player, isTextFrame);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glDoStartFrameNoZoom(int w, int h, float zoom, int player) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glDoStartFrameNoZoom(this.sprite[this.numSprites], w, h, zoom, player);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glDoStartFrameFlipY(int w, int h, float zoom, int player) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glDoStartFrameFlipY(this.sprite[this.numSprites], w, h, zoom, player);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glDoStartFrameFx(int w, int h, int player) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glDoStartFrameFx(this.sprite[this.numSprites], w, h, player);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glIgnoreStyles(boolean b) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glIgnoreStyles(this.sprite[this.numSprites], b);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void pushIsoView(float ox, float oy, float oz, float useangle, boolean vehicle) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.pushIsoView(this.sprite[this.numSprites], ox, oy, oz, useangle, vehicle);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void popIsoView() {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.popIsoView(this.sprite[this.numSprites]);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glDoEndFrame() {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glDoEndFrame(this.sprite[this.numSprites]);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glDoEndFrameFx(int player) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glDoEndFrameFx(this.sprite[this.numSprites], player);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void doCoreIntParam(int id, float val) {
        this.CheckSpriteSlots();
        TextureDraw.doCoreIntParam(this.sprite[this.numSprites], id, val);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glTexParameteri(int a, int b, int c) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glTexParameteri(this.sprite[this.numSprites], a, b, c);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void setCutawayTexture(Texture tex, int x, int y, int w, int h) {
        this.texture1Cutaway = tex;
        this.texture1CutawayX = x;
        this.texture1CutawayY = y;
        this.texture1CutawayW = w;
        this.texture1CutawayH = h;
    }

    public void clearCutawayTexture() {
        this.texture1Cutaway = null;
        this.texture2Cutaway = null;
    }

    public void setCutawayTexture2(Texture tex, int x, int y, int w, int h) {
        this.texture2Cutaway = tex;
    }

    public void setExtraWallShaderParams(SpriteRenderer.WallShaderTexRender wallTexRender) {
        this.wallShaderTexRender = wallTexRender;
    }

    public void ShaderUpdate1i(int shaderID, int uniform, int uniformValue) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.ShaderUpdate1i(this.sprite[this.numSprites], shaderID, uniform, uniformValue);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void ShaderUpdate1f(int shaderID, int uniform, float uniformValue) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.ShaderUpdate1f(this.sprite[this.numSprites], shaderID, uniform, uniformValue);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void ShaderUpdate2f(int shaderID, int uniform, float value1, float value2) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.ShaderUpdate2f(this.sprite[this.numSprites], shaderID, uniform, value1, value2);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void ShaderUpdate3f(int shaderID, int uniform, float value1, float value2, float value3) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.ShaderUpdate3f(this.sprite[this.numSprites], shaderID, uniform, value1, value2, value3);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void ShaderUpdate4f(int shaderID, int uniform, float value1, float value2, float value3, float value4) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.ShaderUpdate4f(this.sprite[this.numSprites], shaderID, uniform, value1, value2, value3, value4);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glLoadIdentity() {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glLoadIdentity(this.sprite[this.numSprites]);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glGenerateMipMaps(int a) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glGenerateMipMaps(this.sprite[this.numSprites], a);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void glBind(int a) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glBind(this.sprite[this.numSprites], a);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void glViewport(int x, int y, int width, int height) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.glViewport(this.sprite[this.numSprites], x, y, width, height);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void drawModel(ModelManager.ModelSlot model) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.drawModel(this.sprite[this.numSprites], model);

        assert this.sprite[this.numSprites].drawer != null;

        ArrayList<TextureDraw> postRender = this.postRender;
        postRender.add(this.sprite[this.numSprites]);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
        model.renderRefCount++;
    }

    public TextureDraw drawGeneric(TextureDraw.GenericDrawer gd) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        this.sprite[this.numSprites].type = TextureDraw.Type.DrawModel;
        this.sprite[this.numSprites].drawer = gd;
        this.style[this.numSprites] = this.defaultStyle;
        ArrayList<TextureDraw> postRender = this.postRender;
        postRender.add(this.sprite[this.numSprites]);
        this.numSprites++;
        return this.sprite[this.numSprites - 1];
    }

    public void render(ImDrawData drawData) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        this.sprite[this.numSprites].type = TextureDraw.Type.DrawImGui;
        this.sprite[this.numSprites].imDrawData = drawData;
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void drawQueued(ModelManager.ModelSlot model) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.DrawQueued(this.sprite[this.numSprites], model);

        assert this.sprite[this.numSprites].drawer != null;

        ArrayList<TextureDraw> postRender = this.postRender;
        postRender.add(this.sprite[this.numSprites]);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
        model.renderRefCount++;
    }

    public void renderQueued() {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.RenderQueued(this.sprite[this.numSprites]);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void beginProfile(PerformanceProfileProbe probe) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.BeginProfile(this.sprite[this.numSprites], probe);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void endProfile(PerformanceProfileProbe probe) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.EndProfile(this.sprite[this.numSprites], probe);
        this.style[this.numSprites] = this.defaultStyle;
        this.numSprites++;
    }

    public void StartShader(int iD, int playerIndex) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.StartShader(this.sprite[this.numSprites], iD);
        if (iD != 0 && Shader.ShaderMap.containsKey(iD)) {
            Shader.ShaderMap.get(iD).startMainThread(this.sprite[this.numSprites], playerIndex);
            ArrayList<TextureDraw> postRender = this.postRender;
            postRender.add(this.sprite[this.numSprites]);
        }

        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void StartShader(int iD, int playerIndex, ShaderUniformSetter uniforms) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.StartShader(this.sprite[this.numSprites], iD, uniforms);
        if (iD != 0 && Shader.ShaderMap.containsKey(iD)) {
            Shader.ShaderMap.get(iD).startMainThread(this.sprite[this.numSprites], playerIndex);
            ArrayList<TextureDraw> postRender = this.postRender;
            postRender.add(this.sprite[this.numSprites]);
        }

        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void EndShader() {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.StartShader(this.sprite[this.numSprites], 0);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void FBORenderChunkStart(int index, boolean bClear) {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.FBORenderChunkStart(this.sprite[this.numSprites], index, bClear);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void FBORenderChunkEnd() {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.FBORenderChunkEnd(this.sprite[this.numSprites]);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }

    public void releaseFBORenderChunkLock() {
        if (this.numSprites == this.sprite.length) {
            this.CheckSpriteSlots();
        }

        TextureDraw.releaseFBORenderChunkLock(this.sprite[this.numSprites]);
        this.style[this.numSprites] = TransparentStyle.instance;
        this.numSprites++;
    }
}
