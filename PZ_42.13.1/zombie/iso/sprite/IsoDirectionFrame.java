// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite;

import java.util.function.Consumer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoDirections;
import zombie.iso.objects.ObjectRenderEffects;

public final class IsoDirectionFrame {
    public final Texture[] directions = new Texture[8];
    boolean doFlip = true;

    public IsoDirectionFrame(Texture tex) {
        this.SetAllDirections(tex);
    }

    public IsoDirectionFrame() {
    }

    public IsoDirectionFrame(Texture nw, Texture n, Texture ne, Texture e, Texture se) {
        this.directions[0] = n;
        this.directions[1] = nw;
        this.directions[2] = n;
        this.directions[3] = ne;
        this.directions[4] = e;
        this.directions[5] = se;
        this.directions[6] = e;
        this.directions[7] = ne;
    }

    public IsoDirectionFrame(Texture n, Texture nw, Texture w, Texture sw, Texture s, Texture se, Texture e, Texture ne) {
        if (s == null) {
            boolean var9 = false;
        }

        this.directions[0] = n;
        this.directions[1] = ne;
        this.directions[2] = e;
        this.directions[3] = se;
        this.directions[4] = s;
        this.directions[5] = sw;
        this.directions[6] = w;
        this.directions[7] = nw;
        this.doFlip = false;
    }

    public IsoDirectionFrame(Texture n, Texture s, Texture e, Texture w) {
        this.directions[0] = n;
        this.directions[1] = n;
        this.directions[2] = w;
        this.directions[3] = w;
        this.directions[4] = s;
        this.directions[5] = s;
        this.directions[6] = e;
        this.directions[7] = e;
        this.doFlip = false;
    }

    public Texture getTexture(IsoDirections dir) {
        return this.directions[dir.index()];
    }

    public void SetAllDirections(Texture tex) {
        this.directions[0] = tex;
        this.directions[1] = tex;
        this.directions[2] = tex;
        this.directions[3] = tex;
        this.directions[4] = tex;
        this.directions[5] = tex;
        this.directions[6] = tex;
        this.directions[7] = tex;
    }

    public void SetDirection(Texture tex, IsoDirections dir) {
        this.directions[dir.index()] = tex;
    }

    public void render(float sx, float sy, IsoDirections dir, ColorInfo info, boolean Flip, Consumer<TextureDraw> texdModifier) {
        Texture tex = this.directions[dir.index()];
        if (tex != null) {
            if (Flip) {
                tex.flip = !tex.flip;
            }

            if (tex != null) {
                if (!this.doFlip) {
                    tex.flip = false;
                }

                tex.render(sx, sy, tex.getWidth(), tex.getHeight(), info.r, info.g, info.b, info.a, texdModifier);
                tex.flip = false;
            }
        }
    }

    void render(float sx, float sy, float width, float height, IsoDirections dir, ColorInfo info, boolean Flip, Consumer<TextureDraw> texdModifier) {
        Texture tex = this.directions[dir.index()];
        if (tex != null) {
            if (Flip) {
                tex.flip = !tex.flip;
            }

            if (!this.doFlip) {
                tex.flip = false;
            }

            tex.render(sx, sy, width, height, info.r, info.g, info.b, info.a, texdModifier);
            tex.flip = false;
        }
    }

    void render(
        ObjectRenderEffects dr,
        float sx,
        float sy,
        float width,
        float height,
        IsoDirections dir,
        ColorInfo info,
        boolean Flip,
        Consumer<TextureDraw> texdModifier
    ) {
        Texture tex = this.directions[dir.index()];
        if (tex != null) {
            if (Flip) {
                tex.flip = !tex.flip;
            }

            if (!this.doFlip) {
                tex.flip = false;
            }

            tex.render(dr, sx, sy, width, height, info.r, info.g, info.b, info.a, texdModifier);
            tex.flip = false;
        }
    }

    public void renderexplicit(int sx, int sy, IsoDirections dir, float scale) {
        this.renderexplicit(sx, sy, dir, scale, null);
    }

    public void renderexplicit(int sx, int sy, IsoDirections dir, float scale, ColorInfo color) {
        Texture tex = this.directions[dir.index()];
        if (tex != null) {
            float a = 1.0F;
            float r = 1.0F;
            float g = 1.0F;
            float b = 1.0F;
            if (color != null) {
                a *= color.a;
                r *= color.r;
                g *= color.g;
                b *= color.b;
            }

            tex.renderstrip(sx, sy, (int)(tex.getWidth() * scale), (int)(tex.getHeight() * scale), r, g, b, a, null);
        }
    }

    public boolean hasNoTextures() {
        for (int i = 0; i < this.directions.length; i++) {
            if (this.directions[i] != null) {
                return false;
            }
        }

        return true;
    }
}
