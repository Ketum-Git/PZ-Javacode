// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.util.ArrayList;
import zombie.core.opengl.SmartShader;
import zombie.popman.ObjectPool;
import zombie.util.list.PZArrayUtil;

public final class TextureCombinerCommand {
    public static final int DEFAULT_SRC_A = 1;
    public static final int DEFAULT_DST_A = 771;
    public int x = -1;
    public int y = -1;
    public int w = -1;
    public int h = -1;
    public Texture mask;
    public Texture tex;
    public int blendSrc;
    public int blendDest;
    public int blendSrcA;
    public int blendDestA;
    public SmartShader shader;
    public ArrayList<TextureCombinerShaderParam> shaderParams;
    public static final ObjectPool<TextureCombinerCommand> pool = new ObjectPool<>(TextureCombinerCommand::new);

    @Override
    public String toString() {
        String nl = System.lineSeparator();
        String cm = ",";
        String tb = "\t";
        return "{"
            + nl
            + "\tpos: "
            + this.x
            + ","
            + this.y
            + nl
            + "\tsize: "
            + this.w
            + ","
            + this.h
            + nl
            + "\tmask:"
            + this.mask
            + nl
            + "\ttex:"
            + this.tex
            + nl
            + "\tblendSrc:"
            + this.blendSrc
            + nl
            + "\tblendDest:"
            + this.blendDest
            + nl
            + "\tblendSrcA:"
            + this.blendSrcA
            + nl
            + "\tblendDestA:"
            + this.blendDestA
            + nl
            + "\tshader:"
            + this.shader
            + nl
            + "\tshaderParams:"
            + PZArrayUtil.arrayToString(this.shaderParams)
            + nl
            + "}";
    }

    public TextureCombinerCommand initSeparate(Texture tex, SmartShader shader, int src, int dest, int srcA, int destA) {
        this.tex = this.requireNonNull(tex);
        this.shader = shader;
        this.blendSrc = src;
        this.blendDest = dest;
        this.blendSrcA = srcA;
        this.blendDestA = destA;
        return this;
    }

    public TextureCombinerCommand init(Texture tex, SmartShader shader, int src, int dest) {
        return this.initSeparate(tex, shader, src, dest, 1, 771);
    }

    public TextureCombinerCommand init(Texture tex, SmartShader shader) {
        this.tex = this.requireNonNull(tex);
        this.shader = shader;
        this.blendSrc = 770;
        this.blendDest = 771;
        this.blendSrcA = 1;
        this.blendDestA = 771;
        return this;
    }

    public TextureCombinerCommand init(Texture tex, SmartShader shader, Texture mask, int src, int dest) {
        this.tex = this.requireNonNull(tex);
        this.shader = shader;
        this.blendSrc = src;
        this.blendDest = dest;
        this.blendSrcA = 1;
        this.blendDestA = 771;
        this.mask = this.requireNonNull(mask);
        return this;
    }

    public TextureCombinerCommand init(Texture tex, SmartShader shader, int x, int y, int w, int h) {
        this.tex = this.requireNonNull(tex);
        this.shader = shader;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.blendSrc = 770;
        this.blendDest = 771;
        this.blendSrcA = 1;
        this.blendDestA = 771;
        return this;
    }

    public TextureCombinerCommand initSeparate(
        Texture tex, SmartShader shader, ArrayList<TextureCombinerShaderParam> params, Texture mask, int src, int dest, int srcA, int destA
    ) {
        this.tex = this.requireNonNull(tex);
        this.shader = shader;
        this.blendSrc = src;
        this.blendDest = dest;
        this.blendSrcA = srcA;
        this.blendDestA = destA;
        this.mask = this.requireNonNull(mask);
        if (this.shaderParams == null) {
            this.shaderParams = new ArrayList<>();
        }

        this.shaderParams.clear();
        PZArrayUtil.addAll(this.shaderParams, params);
        return this;
    }

    public TextureCombinerCommand init(Texture tex, SmartShader shader, ArrayList<TextureCombinerShaderParam> params, Texture mask, int src, int dest) {
        return this.initSeparate(tex, shader, params, mask, src, dest, 1, 771);
    }

    public TextureCombinerCommand init(Texture tex, SmartShader shader, ArrayList<TextureCombinerShaderParam> params) {
        this.tex = this.requireNonNull(tex);
        this.blendSrc = 770;
        this.blendDest = 771;
        this.blendSrcA = 1;
        this.blendDestA = 771;
        this.shader = shader;
        if (this.shaderParams == null) {
            this.shaderParams = new ArrayList<>();
        }

        this.shaderParams.clear();
        PZArrayUtil.addAll(this.shaderParams, params);
        return this;
    }

    private Texture requireNonNull(Texture tex) {
        return tex == null ? Texture.getErrorTexture() : tex;
    }

    public static TextureCombinerCommand get() {
        TextureCombinerCommand com = pool.alloc();
        com.x = -1;
        com.tex = null;
        com.mask = null;
        com.shader = null;
        if (com.shaderParams != null) {
            com.shaderParams.clear();
        }

        return com;
    }
}
