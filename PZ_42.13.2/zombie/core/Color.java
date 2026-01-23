// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.core.utils.Bits;

/**
 * A simple wrapper round the values required for a colour
 */
@UsedFromLua
public final class Color implements Serializable {
    private static final long serialVersionUID = 1393939L;
    /**
     * The fixed color transparent
     */
    public static final Color transparent = new Color(0.0F, 0.0F, 0.0F, 0.0F);
    /**
     * The fixed colour white
     */
    public static final Color white = new Color(1.0F, 1.0F, 1.0F, 1.0F);
    /**
     * The fixed colour yellow
     */
    public static final Color yellow = new Color(1.0F, 1.0F, 0.0F, 1.0F);
    /**
     * The fixed colour red
     */
    public static final Color red = new Color(1.0F, 0.0F, 0.0F, 1.0F);
    /**
     * The fixed colour purple
     */
    public static final Color purple = new Color(196.0F, 0.0F, 171.0F);
    /**
     * The fixed colour blue
     */
    public static final Color blue = new Color(0.0F, 0.0F, 1.0F, 1.0F);
    /**
     * The fixed colour green
     */
    public static final Color green = new Color(0.0F, 1.0F, 0.0F, 1.0F);
    /**
     * The fixed colour black
     */
    public static final Color black = new Color(0.0F, 0.0F, 0.0F, 1.0F);
    /**
     * The fixed colour gray
     */
    public static final Color gray = new Color(0.5F, 0.5F, 0.5F, 1.0F);
    /**
     * The fixed colour cyan
     */
    public static final Color cyan = new Color(0.0F, 1.0F, 1.0F, 1.0F);
    /**
     * The fixed colour dark gray
     */
    public static final Color darkGray = new Color(0.3F, 0.3F, 0.3F, 1.0F);
    /**
     * The fixed colour light gray
     */
    public static final Color lightGray = new Color(0.7F, 0.7F, 0.7F, 1.0F);
    /**
     * The fixed colour dark pink
     */
    public static final Color pink = new Color(255, 175, 175, 255);
    /**
     * The fixed colour dark orange
     */
    public static final Color orange = new Color(255, 200, 0, 255);
    /**
     * The fixed colour dark magenta
     */
    public static final Color magenta = new Color(255, 0, 255, 255);
    /**
     * The fixed colour dark green
     */
    public static final Color darkGreen = new Color(22, 113, 20, 255);
    /**
     * The fixed colour light green
     */
    public static final Color lightGreen = new Color(55, 148, 53, 255);
    /**
     * The alpha component of the colour
     */
    public float a = 1.0F;
    /**
     * The blue component of the colour
     */
    public float b;
    /**
     * The green component of the colour
     */
    public float g;
    /**
     * The red component of the colour
     */
    public float r;

    public float getR() {
        return this.r;
    }

    public float getG() {
        return this.g;
    }

    public float getB() {
        return this.b;
    }

    public Color() {
    }

    /**
     * Copy constructor
     * 
     * @param this The color to copy into the new instance
     */
    public Color(Color color) {
        if (color == null) {
            this.r = 0.0F;
            this.g = 0.0F;
            this.b = 0.0F;
            this.a = 1.0F;
        } else {
            this.r = color.r;
            this.g = color.g;
            this.b = color.b;
            this.a = color.a;
        }
    }

    /**
     * Create a 3 component colour
     * 
     * @param this The red component of the colour (0.0
     * @param r The green component of the colour (0.0
     * @param g The blue component of the colour (0.0
     */
    public Color(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = 1.0F;
    }

    /**
     * Create a 4 component colour
     * 
     * @param this The red component of the colour (0.0
     * @param r The green component of the colour (0.0
     * @param g The blue component of the colour (0.0
     * @param b The alpha component of the colour (0.0
     */
    public Color(float r, float g, float b, float a) {
        this.r = PZMath.clamp(r, 0.0F, 1.0F);
        this.g = PZMath.clamp(g, 0.0F, 1.0F);
        this.b = PZMath.clamp(b, 0.0F, 1.0F);
        this.a = PZMath.clamp(a, 0.0F, 1.0F);
    }

    public Color(Color A, Color B, float delta) {
        float r = (B.r - A.r) * delta;
        float g = (B.g - A.g) * delta;
        float b = (B.b - A.b) * delta;
        float a = (B.a - A.a) * delta;
        this.r = A.r + r;
        this.g = A.g + g;
        this.b = A.b + b;
        this.a = A.a + a;
    }

    public void setColor(Color A, Color B, float delta) {
        float r = (B.r - A.r) * delta;
        float g = (B.g - A.g) * delta;
        float b = (B.b - A.b) * delta;
        float a = (B.a - A.a) * delta;
        this.r = A.r + r;
        this.g = A.g + g;
        this.b = A.b + b;
        this.a = A.a + a;
    }

    /**
     * Create a 3 component colour
     * 
     * @param this The red component of the colour (0
     * @param r The green component of the colour (0
     * @param g The blue component of the colour (0
     */
    public Color(int r, int g, int b) {
        this.r = r / 255.0F;
        this.g = g / 255.0F;
        this.b = b / 255.0F;
        this.a = 1.0F;
    }

    /**
     * Create a 4 component colour
     * 
     * @param this The red component of the colour (0
     * @param r The green component of the colour (0
     * @param g The blue component of the colour (0
     * @param b The alpha component of the colour (0
     */
    public Color(int r, int g, int b, int a) {
        this.r = r / 255.0F;
        this.g = g / 255.0F;
        this.b = b / 255.0F;
        this.a = a / 255.0F;
    }

    /**
     * Create a colour from an evil integer packed 0xAARRGGBB. If AA
     *  is specified as zero then it will be interpreted as unspecified
     *  and hence a value of 255 will be recorded.
     * 
     * @param this The value to interpret for the colour
     */
    public Color(int value) {
        int b = (value & 0xFF0000) >> 16;
        int g = (value & 0xFF00) >> 8;
        int r = value & 0xFF;
        int a = (value & 0xFF000000) >> 24;
        if (a < 0) {
            a += 256;
        }

        if (a == 0) {
            a = 255;
        }

        this.r = r / 255.0F;
        this.g = g / 255.0F;
        this.b = b / 255.0F;
        this.a = a / 255.0F;
    }

    /**
     * Converts the supplied binary value into color values, and sets the result to this object.
     *   Performs a clamp on the alpha channel.
     *   Performs a special-case on the alpha channel, where if it is 0, it is set to MAX instead.
     */
    @Deprecated
    public void fromColor(int valueABGR) {
        int b = (valueABGR & 0xFF0000) >> 16;
        int g = (valueABGR & 0xFF00) >> 8;
        int r = valueABGR & 0xFF;
        int a = (valueABGR & 0xFF000000) >> 24;
        if (a < 0) {
            a += 256;
        }

        if (a == 0) {
            a = 255;
        }

        this.r = r / 255.0F;
        this.g = g / 255.0F;
        this.b = b / 255.0F;
        this.a = a / 255.0F;
    }

    public void setABGR(int valueABGR) {
        abgrToColor(valueABGR, this);
    }

    public static Color abgrToColor(int valueABGR, Color out_result) {
        int a = valueABGR >> 24 & 0xFF;
        int b = valueABGR >> 16 & 0xFF;
        int g = valueABGR >> 8 & 0xFF;
        int r = valueABGR & 0xFF;
        float byteToFloatChannel = 0.003921569F;
        float rc = 0.003921569F * r;
        float gc = 0.003921569F * g;
        float bc = 0.003921569F * b;
        float ac = 0.003921569F * a;
        out_result.r = rc;
        out_result.g = gc;
        out_result.b = bc;
        out_result.a = ac;
        return out_result;
    }

    public static int colorToABGR(Color val) {
        return colorToABGR(val.r, val.g, val.b, val.a);
    }

    public static int colorToABGR(ColorInfo val) {
        return colorToABGR(val.r, val.g, val.b, val.a);
    }

    public static int colorToABGR(float r, float g, float b, float a) {
        r = PZMath.clamp(r, 0.0F, 1.0F);
        g = PZMath.clamp(g, 0.0F, 1.0F);
        b = PZMath.clamp(b, 0.0F, 1.0F);
        a = PZMath.clamp(a, 0.0F, 1.0F);
        float floatChannelToByte = 255.0F;
        int byteR = (int)(r * 255.0F);
        int byteG = (int)(g * 255.0F);
        int byteB = (int)(b * 255.0F);
        int byteA = (int)(a * 255.0F);
        return (byteA & 0xFF) << 24 | (byteB & 0xFF) << 16 | (byteG & 0xFF) << 8 | byteR & 0xFF;
    }

    public static int multiplyABGR(int valueABGR, int multiplierABGR) {
        float rc = getRedChannelFromABGR(valueABGR);
        float gc = getGreenChannelFromABGR(valueABGR);
        float bc = getBlueChannelFromABGR(valueABGR);
        float ac = getAlphaChannelFromABGR(valueABGR);
        float mrc = getRedChannelFromABGR(multiplierABGR);
        float mgc = getGreenChannelFromABGR(multiplierABGR);
        float mbc = getBlueChannelFromABGR(multiplierABGR);
        float mac = getAlphaChannelFromABGR(multiplierABGR);
        return colorToABGR(rc * mrc, gc * mgc, bc * mbc, ac * mac);
    }

    public static int multiplyBGR(int valueABGR, int multiplierABGR) {
        float rc = getRedChannelFromABGR(valueABGR);
        float gc = getGreenChannelFromABGR(valueABGR);
        float bc = getBlueChannelFromABGR(valueABGR);
        float ac = getAlphaChannelFromABGR(valueABGR);
        float mrc = getRedChannelFromABGR(multiplierABGR);
        float mgc = getGreenChannelFromABGR(multiplierABGR);
        float mbc = getBlueChannelFromABGR(multiplierABGR);
        return colorToABGR(rc * mrc, gc * mgc, bc * mbc, ac);
    }

    public static int blendBGR(int valueABGR, int targetABGR) {
        float r = getRedChannelFromABGR(valueABGR);
        float g = getGreenChannelFromABGR(valueABGR);
        float b = getBlueChannelFromABGR(valueABGR);
        float a = getAlphaChannelFromABGR(valueABGR);
        float tr = getRedChannelFromABGR(targetABGR);
        float tg = getGreenChannelFromABGR(targetABGR);
        float tb = getBlueChannelFromABGR(targetABGR);
        float ta = getAlphaChannelFromABGR(targetABGR);
        return colorToABGR(r * (1.0F - ta) + tr * ta, g * (1.0F - ta) + tg * ta, b * (1.0F - ta) + tb * ta, a);
    }

    public static int blendABGR(int valueABGR, int targetABGR) {
        float r = getRedChannelFromABGR(valueABGR);
        float g = getGreenChannelFromABGR(valueABGR);
        float b = getBlueChannelFromABGR(valueABGR);
        float a = getAlphaChannelFromABGR(valueABGR);
        float tr = getRedChannelFromABGR(targetABGR);
        float tg = getGreenChannelFromABGR(targetABGR);
        float tb = getBlueChannelFromABGR(targetABGR);
        float ta = getAlphaChannelFromABGR(targetABGR);
        return colorToABGR(r * (1.0F - ta) + tr * ta, g * (1.0F - ta) + tg * ta, b * (1.0F - ta) + tb * ta, a * (1.0F - ta) + ta * ta);
    }

    public static int tintABGR(int targetABGR, int tintABGR) {
        float r = getRedChannelFromABGR(tintABGR);
        float g = getGreenChannelFromABGR(tintABGR);
        float b = getBlueChannelFromABGR(tintABGR);
        float a = getAlphaChannelFromABGR(tintABGR);
        float tr = getRedChannelFromABGR(targetABGR);
        float tg = getGreenChannelFromABGR(targetABGR);
        float tb = getBlueChannelFromABGR(targetABGR);
        float ta = getAlphaChannelFromABGR(targetABGR);
        return colorToABGR(r * a + tr * (1.0F - a), g * a + tg * (1.0F - a), b * a + tb * (1.0F - a), ta);
    }

    public static int lerpABGR(int colA, int colB, float alpha) {
        float r = getRedChannelFromABGR(colA);
        float g = getGreenChannelFromABGR(colA);
        float b = getBlueChannelFromABGR(colA);
        float a = getAlphaChannelFromABGR(colA);
        float tr = getRedChannelFromABGR(colB);
        float tg = getGreenChannelFromABGR(colB);
        float tb = getBlueChannelFromABGR(colB);
        float ta = getAlphaChannelFromABGR(colB);
        return colorToABGR(r * (1.0F - alpha) + tr * alpha, g * (1.0F - alpha) + tg * alpha, b * (1.0F - alpha) + tb * alpha, a * (1.0F - alpha) + ta * alpha);
    }

    public static float getAlphaChannelFromABGR(int valueABGR) {
        int a = valueABGR >> 24 & 0xFF;
        float byteToFloatChannel = 0.003921569F;
        return 0.003921569F * a;
    }

    public static float getBlueChannelFromABGR(int valueABGR) {
        int b = valueABGR >> 16 & 0xFF;
        float byteToFloatChannel = 0.003921569F;
        return 0.003921569F * b;
    }

    public static float getGreenChannelFromABGR(int valueABGR) {
        int g = valueABGR >> 8 & 0xFF;
        float byteToFloatChannel = 0.003921569F;
        return 0.003921569F * g;
    }

    public static float getRedChannelFromABGR(int valueABGR) {
        int r = valueABGR & 0xFF;
        float byteToFloatChannel = 0.003921569F;
        return 0.003921569F * r;
    }

    public static int setAlphaChannelToABGR(int valueABGR, float a) {
        a = PZMath.clamp(a, 0.0F, 1.0F);
        float floatChannelToByte = 255.0F;
        int byteA = (int)(a * 255.0F);
        return (byteA & 0xFF) << 24 | valueABGR & 16777215;
    }

    public static int setBlueChannelToABGR(int valueABGR, float b) {
        b = PZMath.clamp(b, 0.0F, 1.0F);
        float floatChannelToByte = 255.0F;
        int byteB = (int)(b * 255.0F);
        return (byteB & 0xFF) << 16 | valueABGR & -16711681;
    }

    public static int setGreenChannelToABGR(int valueABGR, float g) {
        g = PZMath.clamp(g, 0.0F, 1.0F);
        float floatChannelToByte = 255.0F;
        int byteG = (int)(g * 255.0F);
        return (byteG & 0xFF) << 8 | valueABGR & -65281;
    }

    public static int setRedChannelToABGR(int valueABGR, float r) {
        r = PZMath.clamp(r, 0.0F, 1.0F);
        float floatChannelToByte = 255.0F;
        int byteR = (int)(r * 255.0F);
        return byteR & 0xFF | valueABGR & -256;
    }

    /**
     * Create a random color.
     */
    public static Color random() {
        return Colors.GetRandomColor();
    }

    /**
     * Decode a number in a string and process it as a colour
     *  reference.
     * 
     * @param nm The number string to decode
     * @return The color generated from the number read
     */
    public static Color decode(String nm) {
        return new Color(Integer.decode(nm));
    }

    /**
     * Add another colour to this one
     * 
     * @param c The colour to add
     */
    public void add(Color c) {
        this.r = this.r + c.r;
        this.g = this.g + c.g;
        this.b = this.b + c.b;
        this.a = this.a + c.a;
    }

    /**
     * Add another colour to this one
     * 
     * @param c The colour to add
     * @return The copy which has had the color added to it
     */
    public Color addToCopy(Color c) {
        Color copy = new Color(this.r, this.g, this.b, this.a);
        copy.r = copy.r + c.r;
        copy.g = copy.g + c.g;
        copy.b = copy.b + c.b;
        copy.a = copy.a + c.a;
        return copy;
    }

    /**
     * Make a brighter instance of this colour
     * @return The brighter version of this colour
     */
    public Color brighter() {
        return this.brighter(0.2F);
    }

    /**
     * Make a brighter instance of this colour
     * 
     * @param scale The scale up of RGB (i.e. if you supply 0.03 the colour will be brightened by 3%)
     * @return The brighter version of this colour
     */
    public Color brighter(float scale) {
        this.r = this.r += scale;
        this.g = this.g += scale;
        this.b = this.b += scale;
        return this;
    }

    /**
     * Make a darker instance of this colour
     * @return The darker version of this colour
     */
    public Color darker() {
        return this.darker(0.5F);
    }

    /**
     * Make a darker instance of this colour
     * 
     * @param scale The scale down of RGB (i.e. if you supply 0.03 the colour will be darkened by 3%)
     * @return The darker version of this colour
     */
    public Color darker(float scale) {
        this.r = this.r -= scale;
        this.g = this.g -= scale;
        this.b = this.b -= scale;
        return this;
    }

    @Override
    public boolean equals(Object other) {
        return !(other instanceof Color o) ? false : o.r == this.r && o.g == this.g && o.b == this.b && o.a == this.a;
    }

    public boolean equalBytes(Color other) {
        return other == null
            ? false
            : this.getRedByte() == other.getRedByte()
                && this.getBlueByte() == other.getBlueByte()
                && this.getGreenByte() == other.getGreenByte()
                && this.getAlphaByte() == other.getAlphaByte();
    }

    public Color set(Color other) {
        this.r = other.r;
        this.g = other.g;
        this.b = other.b;
        this.a = other.a;
        return this;
    }

    public Color set(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = 1.0F;
        return this;
    }

    public Color set(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }

    public void save(ByteBuffer output) {
        output.putFloat(this.r);
        output.putFloat(this.g);
        output.putFloat(this.b);
        output.putFloat(this.a);
    }

    public void load(ByteBuffer input, int WorldVersion) {
        this.r = input.getFloat();
        this.g = input.getFloat();
        this.b = input.getFloat();
        this.a = input.getFloat();
    }

    /**
     * get the alpha byte component of this colour
     * @return The alpha component (range 0-255)
     */
    public int getAlpha() {
        return (int)(this.a * 255.0F);
    }

    public float getAlphaFloat() {
        return this.a;
    }

    public float getRedFloat() {
        return this.r;
    }

    public float getGreenFloat() {
        return this.g;
    }

    public float getBlueFloat() {
        return this.b;
    }

    /**
     * get the alpha byte component of this colour
     * @return The alpha component (range 0-255)
     */
    public int getAlphaByte() {
        return (int)(this.a * 255.0F);
    }

    /**
     * get the blue byte component of this colour
     * @return The blue component (range 0-255)
     */
    public int getBlue() {
        return (int)(this.b * 255.0F);
    }

    /**
     * get the blue byte component of this colour
     * @return The blue component (range 0-255)
     */
    public int getBlueByte() {
        return (int)(this.b * 255.0F);
    }

    /**
     * get the green byte component of this colour
     * @return The green component (range 0-255)
     */
    public int getGreen() {
        return (int)(this.g * 255.0F);
    }

    /**
     * get the green byte component of this colour
     * @return The green component (range 0-255)
     */
    public int getGreenByte() {
        return (int)(this.g * 255.0F);
    }

    /**
     * get the red byte component of this colour
     * @return The red component (range 0-255)
     */
    public int getRed() {
        return (int)(this.r * 255.0F);
    }

    /**
     * get the red byte component of this colour
     * @return The red component (range 0-255)
     */
    public int getRedByte() {
        return (int)(this.r * 255.0F);
    }

    @Override
    public int hashCode() {
        return (int)(this.r + this.g + this.b + this.a) * 255;
    }

    /**
     * Multiply this color by another
     * 
     * @param c the other color
     * @return product of the two colors
     */
    public Color multiply(Color c) {
        return new Color(this.r * c.r, this.g * c.g, this.b * c.b, this.a * c.a);
    }

    /**
     * Scale the components of the colour by the given value
     * 
     * @param value The value to scale by
     */
    public Color scale(float value) {
        this.r *= value;
        this.g *= value;
        this.b *= value;
        this.a *= value;
        return this;
    }

    /**
     * Scale the components of the colour by the given value
     * 
     * @param value The value to scale by
     * @return The copy which has been scaled
     */
    public Color scaleCopy(float value) {
        Color copy = new Color(this.r, this.g, this.b, this.a);
        copy.r *= value;
        copy.g *= value;
        copy.b *= value;
        copy.a *= value;
        return copy;
    }

    @Override
    public String toString() {
        return "Color (" + this.r + "," + this.g + "," + this.b + "," + this.a + ")";
    }

    public void interp(Color to, float delta, Color dest) {
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

    public void changeHSBValue(float hFactor, float sFactor, float bFactor) {
        float[] hsb = java.awt.Color.RGBtoHSB(this.getRedByte(), this.getGreenByte(), this.getBlueByte(), null);
        int newValue = java.awt.Color.HSBtoRGB(hsb[0] * hFactor, hsb[1] * sFactor, hsb[2] * bFactor);
        this.r = (newValue >> 16 & 0xFF) / 255.0F;
        this.g = (newValue >> 8 & 0xFF) / 255.0F;
        this.b = (newValue & 0xFF) / 255.0F;
    }

    public static Color HSBtoRGB(float hue, float saturation, float brightness, Color result) {
        int r = 0;
        int g = 0;
        int b = 0;
        if (saturation == 0.0F) {
            r = g = b = (int)(brightness * 255.0F + 0.5F);
        } else {
            float h = (hue - (float)Math.floor(hue)) * 6.0F;
            float f = h - (float)Math.floor(h);
            float p = brightness * (1.0F - saturation);
            float q = brightness * (1.0F - saturation * f);
            float t = brightness * (1.0F - saturation * (1.0F - f));
            switch ((int)h) {
                case 0:
                    r = (int)(brightness * 255.0F + 0.5F);
                    g = (int)(t * 255.0F + 0.5F);
                    b = (int)(p * 255.0F + 0.5F);
                    break;
                case 1:
                    r = (int)(q * 255.0F + 0.5F);
                    g = (int)(brightness * 255.0F + 0.5F);
                    b = (int)(p * 255.0F + 0.5F);
                    break;
                case 2:
                    r = (int)(p * 255.0F + 0.5F);
                    g = (int)(brightness * 255.0F + 0.5F);
                    b = (int)(t * 255.0F + 0.5F);
                    break;
                case 3:
                    r = (int)(p * 255.0F + 0.5F);
                    g = (int)(q * 255.0F + 0.5F);
                    b = (int)(brightness * 255.0F + 0.5F);
                    break;
                case 4:
                    r = (int)(t * 255.0F + 0.5F);
                    g = (int)(p * 255.0F + 0.5F);
                    b = (int)(brightness * 255.0F + 0.5F);
                    break;
                case 5:
                    r = (int)(brightness * 255.0F + 0.5F);
                    g = (int)(p * 255.0F + 0.5F);
                    b = (int)(q * 255.0F + 0.5F);
            }
        }

        return result.set(r / 255.0F, g / 255.0F, b / 255.0F);
    }

    public static Color HSBtoRGB(float hue, float saturation, float brightness) {
        return HSBtoRGB(hue, saturation, brightness, new Color());
    }

    public void saveCompactNoAlpha(ByteBuffer output) throws IOException {
        this.saveCompact(output, false);
    }

    public void loadCompactNoAlpha(ByteBuffer input) throws IOException {
        this.loadCompact(input, false);
    }

    public void saveCompact(ByteBuffer output) throws IOException {
        this.saveCompact(output, true);
    }

    public void loadCompact(ByteBuffer input) throws IOException {
        this.loadCompact(input, true);
    }

    private void saveCompact(ByteBuffer output, boolean saveAlpha) throws IOException {
        output.put(Bits.packFloatUnitToByte(this.r));
        output.put(Bits.packFloatUnitToByte(this.g));
        output.put(Bits.packFloatUnitToByte(this.b));
        if (saveAlpha) {
            output.put(Bits.packFloatUnitToByte(this.a));
        }
    }

    private void loadCompact(ByteBuffer input, boolean loadAlpha) throws IOException {
        this.r = Bits.unpackByteToFloatUnit(input.get());
        this.g = Bits.unpackByteToFloatUnit(input.get());
        this.b = Bits.unpackByteToFloatUnit(input.get());
        if (loadAlpha) {
            this.a = Bits.unpackByteToFloatUnit(input.get());
        }
    }
}
