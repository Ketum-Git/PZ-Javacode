// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.utils;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Boolean grid
 */
public class BooleanGrid implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private final int width;
    private final int height;
    private final int bitWidth;
    private final int[] value;

    /**
     * C'tor
     */
    public BooleanGrid(int width, int height) {
        this.bitWidth = width;
        this.width = width / 32 + (width % 32 != 0 ? 1 : 0);
        this.height = height;
        this.value = new int[this.width * this.height];
    }

    public BooleanGrid clone() throws CloneNotSupportedException {
        BooleanGrid ret = new BooleanGrid(this.bitWidth, this.height);
        System.arraycopy(this.value, 0, ret.value, 0, this.value.length);
        return ret;
    }

    public void copy(BooleanGrid src) {
        if (src.bitWidth == this.bitWidth && src.height == this.height) {
            System.arraycopy(src.value, 0, this.value, 0, src.value.length);
        } else {
            throw new IllegalArgumentException("src must be same size as this: " + src + " cannot be copied into " + this);
        }
    }

    public void clear() {
        Arrays.fill(this.value, 0);
    }

    public void fill() {
        Arrays.fill(this.value, -1);
    }

    private int getIndex(int x, int y) {
        return x >= 0 && y >= 0 && x < this.width && y < this.height ? x + y * this.width : -1;
    }

    public boolean getValue(int x, int y) {
        if (x < this.bitWidth && x >= 0 && y < this.height && y >= 0) {
            int xx = x / 32;
            int xxx = 1 << (x & 31);
            int idx = this.getIndex(xx, y);
            if (idx == -1) {
                return false;
            } else {
                int v = this.value[idx];
                return (v & xxx) != 0;
            }
        } else {
            return false;
        }
    }

    public void setValue(int x, int y, boolean newValue) {
        if (x < this.bitWidth && x >= 0 && y < this.height && y >= 0) {
            int xx = x / 32;
            int xxx = 1 << (x & 31);
            int idx = this.getIndex(xx, y);
            if (idx != -1) {
                if (newValue) {
                    this.value[idx] = this.value[idx] | xxx;
                } else {
                    this.value[idx] = this.value[idx] & ~xxx;
                }
            }
        }
    }

    /**
     * @return the width
     */
    public final int getWidth() {
        return this.width;
    }

    /**
     * @return the height
     */
    public final int getHeight() {
        return this.height;
    }

    @Override
    public String toString() {
        return "BooleanGrid [width=" + this.width + ", height=" + this.height + ", bitWidth=" + this.bitWidth + "]";
    }

    public void LoadFromByteBuffer(ByteBuffer cache) {
        int w = this.width * this.height;

        for (int x = 0; x < w; x++) {
            this.value[x] = cache.getInt();
        }
    }

    public void PutToByteBuffer(ByteBuffer cache) {
        int w = this.width * this.height;

        for (int x = 0; x < w; x++) {
            cache.putInt(this.value[x]);
        }
    }
}
