// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.ui;

import zombie.UsedFromLua;

@UsedFromLua
public enum VectorPosAlign {
    None(0.0F, 0.0F),
    TopLeft(0.0F, 0.0F),
    TopMiddle(0.5F, 0.5F),
    TopRight(1.0F, 1.0F),
    CenterLeft(0.0F, 0.5F),
    CenterMiddle(0.5F, 0.5F),
    CenterRight(1.0F, 0.5F),
    BottomLeft(0.0F, 1.0F),
    BottomMiddle(0.5F, 1.0F),
    BottomRight(1.0F, 1.0F);

    private final float xmod;
    private final float ymod;

    private VectorPosAlign(final float xmod, final float ymod) {
        this.xmod = xmod;
        this.ymod = ymod;
    }

    public float getXmod() {
        return this.xmod;
    }

    public float getYmod() {
        return this.ymod;
    }

    public float getX(XuiScript.XuiVector v) {
        return v.isxPercent() ? v.getX() : v.getX() - v.getW() * this.xmod;
    }

    public float getY(XuiScript.XuiVector v) {
        return v.isyPercent() ? v.getY() : v.getY() - v.getH() * this.ymod;
    }
}
