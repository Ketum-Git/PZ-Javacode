// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

/**
 * Created by LEMMY on 6/30/2016.
 */
public final class TextureCombinerShaderParam {
    public String name;
    public float min;
    public float max;

    public TextureCombinerShaderParam(String name, float min, float max) {
        this.name = name;
        this.min = min;
        this.max = max;
    }

    public TextureCombinerShaderParam(String name, float val) {
        this.name = name;
        this.min = val;
        this.max = val;
    }
}
