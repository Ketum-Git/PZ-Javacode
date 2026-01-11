// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.viewCone;

import zombie.core.Core;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;

public class ViewConeTextureFBO {
    public TextureFBO viewConeFbo;
    public static ViewConeTextureFBO instance = new ViewConeTextureFBO();
    private Texture tex;
    boolean inited;
    int ww;
    int hh;

    public void init() {
        this.resize(Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight());
        this.inited = true;
    }

    public void resize(int w, int h) {
        if (this.viewConeFbo != null) {
            this.viewConeFbo.releaseTexture();
            this.viewConeFbo.destroy();
        }

        if (this.tex != null) {
            this.tex.destroy();
        }

        this.tex = new Texture(w / 4, h / 4, 16);
        this.viewConeFbo = new TextureFBO(this.tex, false);
        this.ww = w;
        this.hh = h;
    }

    public void stopDrawing() {
        if (!this.inited) {
            this.init();
        }

        this.viewConeFbo.endDrawing();
    }

    public void startDrawing() {
        if (!this.inited) {
            this.init();
        } else if (Core.getInstance().getScreenWidth() != this.ww || Core.getInstance().getScreenHeight() != this.hh) {
            this.resize(Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight());
        }

        this.viewConeFbo.startDrawing(true, true);
    }

    public Texture getTexture() {
        return this.tex;
    }
}
