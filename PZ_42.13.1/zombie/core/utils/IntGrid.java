// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.utils;

import java.io.Serializable;
import java.util.Arrays;

public class IntGrid implements Serializable, Cloneable {
    private static final long serialVersionUID = 1L;
    private final int width;
    private final int height;
    private final int[] value;

    /**
     * C'tor
     */
    public IntGrid(int width, int height) {
        this.width = width;
        this.height = height;
        this.value = new int[width * height];
    }

    public IntGrid clone() throws CloneNotSupportedException {
        IntGrid ret = new IntGrid(this.width, this.height);
        System.arraycopy(this.value, 0, ret.value, 0, this.value.length);
        return ret;
    }

    public void clear() {
        Arrays.fill(this.value, 0);
    }

    public void fill(int newValue) {
        Arrays.fill(this.value, newValue);
    }

    private int getIndex(int x, int y) {
        return x >= 0 && y >= 0 && x < this.width && y < this.height ? x + y * this.width : -1;
    }

    public int getValue(int x, int y) {
        int idx = this.getIndex(x, y);
        return idx == -1 ? 0 : this.value[idx];
    }

    public void setValue(int x, int y, int newValue) {
        int idx = this.getIndex(x, y);
        if (idx != -1) {
            this.value[idx] = newValue;
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
}
