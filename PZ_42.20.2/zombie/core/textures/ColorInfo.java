// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.ImmutableColor;
import zombie.core.math.PZMath;

@UsedFromLua
public final class ColorInfo {
    public float a;
    public float b;
    public float g;
    public float r;

    public ColorInfo() {
        this.r = 1.0F;
        this.g = 1.0F;
        this.b = 1.0F;
        this.a = 1.0F;
    }

    public ColorInfo(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    @Override
    public boolean equals(Object obj) {
        return !(obj instanceof ColorInfo rhs) ? false : this.r == rhs.r && this.g == rhs.g && this.b == rhs.b && this.a == rhs.a;
    }

    public ColorInfo set(ColorInfo other) {
        this.r = other.r;
        this.g = other.g;
        this.b = other.b;
        this.a = other.a;
        return this;
    }

    public ColorInfo set(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }

    public ColorInfo setRGB(float rgb) {
        return this.set(rgb, rgb, rgb, this.getA());
    }

    public ColorInfo setRGB(float r, float g, float b) {
        return this.set(r, g, b, this.getA());
    }

    public ColorInfo setABGR(int abgr) {
        this.r = Color.getRedChannelFromABGR(abgr);
        this.g = Color.getGreenChannelFromABGR(abgr);
        this.b = Color.getBlueChannelFromABGR(abgr);
        this.a = Color.getAlphaChannelFromABGR(abgr);
        return this;
    }

    public ColorInfo min(float r, float g, float b, float a) {
        this.r = PZMath.min(this.r, r);
        this.g = PZMath.min(this.g, g);
        this.b = PZMath.min(this.b, b);
        this.a = PZMath.min(this.a, a);
        return this;
    }

    public ColorInfo minRGB(float rgb) {
        return this.minRGB(rgb, rgb, rgb);
    }

    public ColorInfo minRGB(float r, float g, float b) {
        return this.min(r, g, b, 1.0F);
    }

    public float getR() {
        return this.r;
    }

    public float getG() {
        return this.g;
    }

    public float getB() {
        return this.b;
    }

    public Color toColor() {
        return new Color(this.r, this.g, this.b, this.a);
    }

    public ImmutableColor toImmutableColor() {
        return new ImmutableColor(this.r, this.g, this.b, this.a);
    }

    public float getA() {
        return this.a;
    }

    public void desaturate(float s) {
        float gray = this.r * 0.3086F + this.g * 0.6094F + this.b * 0.082F;
        this.r = gray * s + this.r * (1.0F - s);
        this.g = gray * s + this.g * (1.0F - s);
        this.b = gray * s + this.b * (1.0F - s);
    }

    public void interp(ColorInfo to, float delta, ColorInfo dest) {
        float r = to.r - this.r;
        float g = to.g - this.g;
        float b = to.b - this.b;
        float a = to.a - this.a;
        r *= delta;
        g *= delta;
        b *= delta;
        a *= delta;
        dest.r = this.r + r;
        dest.g = this.g + g;
        dest.b = this.b + b;
        dest.a = this.a + a;
    }

    @Override
    public String toString() {
        return "Color (" + this.r + "," + this.g + "," + this.b + "," + this.a + ")";
    }
}
