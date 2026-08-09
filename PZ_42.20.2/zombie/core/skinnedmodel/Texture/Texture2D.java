// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.Texture;

import zombie.core.textures.Texture;

public class Texture2D {
    Texture tex;

    public Texture2D(Texture tex) {
        this.tex = tex;
    }

    public int getWidth() {
        return this.tex.getWidth();
    }

    public int getHeight() {
        return this.tex.getHeight();
    }

    public int getTexture() {
        return this.tex.getID();
    }

    public void Apply() {
    }
}
