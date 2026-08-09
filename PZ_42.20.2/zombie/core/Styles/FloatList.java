// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.Styles;

import java.io.Serializable;

/**
 * Quickly hacked together expandable list of floats
 */
public class FloatList implements Serializable {
    private static final long serialVersionUID = 1L;
    private float[] value;
    private int count;
    private final FloatList.ExpandStyle expandStyle;

    /**
     * FloatList constructor comment.
     */
    public FloatList() {
        this(0);
    }

    /**
     * FloatList constructor comment.
     */
    public FloatList(int size) {
        this(FloatList.ExpandStyle.Fast, size);
    }

    public FloatList(FloatList.ExpandStyle style, int size) {
        this.expandStyle = style;
        this.value = new float[size];
    }

    /**
     * add method comment.
     */
    public float add(float f) {
        if (this.count == this.value.length) {
            float[] oldValue = this.value;
            if (this.expandStyle == FloatList.ExpandStyle.Fast) {
                this.value = new float[(oldValue.length << 1) + 1];
            } else if (this.expandStyle == FloatList.ExpandStyle.Normal) {
                this.value = new float[oldValue.length + oldValue.length / 10 + 10];
            } else {
                this.value = new float[oldValue.length + 1];
            }

            System.arraycopy(oldValue, 0, this.value, 0, oldValue.length);
        }

        this.value[this.count] = f;
        return this.count++;
    }

    /**
     * Remove an element and return it.
     * 
     * @param idx The index of the element to remove
     * @return the removed value
     */
    public float remove(int idx) {
        if (idx < this.count && idx >= 0) {
            float ret = this.value[idx];
            if (idx < this.count - 1) {
                System.arraycopy(this.value, idx + 1, this.value, idx, this.count - idx - 1);
            }

            this.count--;
            return ret;
        } else {
            throw new IndexOutOfBoundsException("Referenced " + idx + ", size=" + this.count);
        }
    }

    /**
     * add method comment.
     */
    public void addAll(float[] f) {
        this.ensureCapacity(this.count + f.length);
        System.arraycopy(f, 0, this.value, this.count, f.length);
        this.count += f.length;
    }

    /**
     * add method comment.
     */
    public void addAll(FloatList f) {
        this.ensureCapacity(this.count + f.count);
        System.arraycopy(f.value, 0, this.value, this.count, f.count);
        this.count = this.count + f.count;
    }

    /**
     * toArray method comment.
     */
    public float[] array() {
        return this.value;
    }

    /**
     * Insert the method's description here. Creation date: (11/03/2001
     *  17:19:01)
     */
    public int capacity() {
        return this.value.length;
    }

    /**
     * clear method comment.
     */
    public void clear() {
        this.count = 0;
    }

    /**
     * Ensure the list is at least 'size' elements big.
     */
    public void ensureCapacity(int size) {
        if (this.value.length < size) {
            float[] oldValue = this.value;
            this.value = new float[size];
            System.arraycopy(oldValue, 0, this.value, 0, oldValue.length);
        }
    }

    /**
     * get method comment.
     */
    public float get(int index) {
        return this.value[index];
    }

    /**
     * isEmpty method comment.
     */
    public boolean isEmpty() {
        return this.count == 0;
    }

    /**
     * size method comment.
     */
    public int size() {
        return this.count;
    }

    /**
     * Stash everything in an array.
     */
    public void toArray(Object[] dest) {
        System.arraycopy(this.value, 0, dest, 0, this.count);
    }

    /**
     * Pack list to its minimum size.
     */
    public void trimToSize() {
        if (this.count != this.value.length) {
            float[] oldValue = this.value;
            this.value = new float[this.count];
            System.arraycopy(oldValue, 0, this.value, 0, this.count);
        }
    }

    public static enum ExpandStyle {
        Slow,
        Normal,
        Fast;
    }
}
