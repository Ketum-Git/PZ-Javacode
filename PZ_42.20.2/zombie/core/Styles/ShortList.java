// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.Styles;

import java.io.Serializable;

/**
 * Quickly hacked together expandable list of shorts
 */
public class ShortList implements Serializable {
    private static final long serialVersionUID = 1L;
    private short[] value;
    private short count;
    private final boolean fastExpand;

    /**
     * FloatList constructor comment.
     */
    public ShortList() {
        this(0);
    }

    /**
     * FloatList constructor comment.
     */
    public ShortList(int size) {
        this(true, size);
    }

    /**
     * FloatList constructor comment.
     */
    public ShortList(boolean fastExpand, int size) {
        this.fastExpand = fastExpand;
        this.value = new short[size];
    }

    /**
     * add method comment.
     */
    public short add(short f) {
        if (this.count == this.value.length) {
            short[] oldValue = this.value;
            if (this.fastExpand) {
                this.value = new short[(oldValue.length << 1) + 1];
            } else {
                this.value = new short[oldValue.length + 1];
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
    public short remove(int idx) {
        if (idx < this.count && idx >= 0) {
            short ret = this.value[idx];
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
    public void addAll(short[] f) {
        this.ensureCapacity(this.count + f.length);
        System.arraycopy(f, 0, this.value, this.count, f.length);
        this.count = (short)(this.count + f.length);
    }

    /**
     * add method comment.
     */
    public void addAll(ShortList f) {
        this.ensureCapacity(this.count + f.count);
        System.arraycopy(f.value, 0, this.value, this.count, f.count);
        this.count = (short)(this.count + f.count);
    }

    /**
     * toArray method comment.
     */
    public short[] array() {
        return this.value;
    }

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
            short[] oldValue = this.value;
            this.value = new short[size];
            System.arraycopy(oldValue, 0, this.value, 0, oldValue.length);
        }
    }

    /**
     * get method comment.
     */
    public short get(int index) {
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
    public short[] toArray(short[] dest) {
        if (dest == null) {
            dest = new short[this.count];
        }

        System.arraycopy(this.value, 0, dest, 0, this.count);
        return dest;
    }

    /**
     * Pack list to its minimum size.
     */
    public void trimToSize() {
        if (this.count != this.value.length) {
            short[] oldValue = this.value;
            this.value = new short[this.count];
            System.arraycopy(oldValue, 0, this.value, 0, this.count);
        }
    }
}
