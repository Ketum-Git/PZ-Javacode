// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.Styles;

import java.io.Serializable;

public class IntList implements Serializable {
    private static final long serialVersionUID = 1L;
    private int[] value;
    private int count;
    private final boolean fastExpand;

    public IntList() {
        this(0);
    }

    public IntList(int size) {
        this(true, size);
    }

    public IntList(boolean fastExpand, int size) {
        this.fastExpand = fastExpand;
        this.value = new int[size];
    }

    public int add(short f) {
        if (this.count == this.value.length) {
            int[] oldValue = this.value;
            if (this.fastExpand) {
                this.value = new int[(oldValue.length << 1) + 1];
            } else {
                this.value = new int[oldValue.length + 1];
            }

            System.arraycopy(oldValue, 0, this.value, 0, oldValue.length);
        }

        this.value[this.count] = f;
        return this.count++;
    }

    public int remove(int idx) {
        if (idx < this.count && idx >= 0) {
            int ret = this.value[idx];
            if (idx < this.count - 1) {
                System.arraycopy(this.value, idx + 1, this.value, idx, this.count - idx - 1);
            }

            this.count--;
            return ret;
        } else {
            throw new IndexOutOfBoundsException("Referenced " + idx + ", size=" + this.count);
        }
    }

    public void addAll(short[] f) {
        this.ensureCapacity(this.count + f.length);
        System.arraycopy(f, 0, this.value, this.count, f.length);
        this.count += f.length;
    }

    public void addAll(IntList f) {
        this.ensureCapacity(this.count + f.count);
        System.arraycopy(f.value, 0, this.value, this.count, f.count);
        this.count = this.count + f.count;
    }

    public int[] array() {
        return this.value;
    }

    public int capacity() {
        return this.value.length;
    }

    public void clear() {
        this.count = 0;
    }

    public void ensureCapacity(int size) {
        if (this.value.length < size) {
            int[] oldValue = this.value;
            this.value = new int[size];
            System.arraycopy(oldValue, 0, this.value, 0, oldValue.length);
        }
    }

    public int get(int index) {
        return this.value[index];
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public int size() {
        return this.count;
    }

    public short[] toArray(short[] dest) {
        if (dest == null) {
            dest = new short[this.count];
        }

        System.arraycopy(this.value, 0, dest, 0, this.count);
        return dest;
    }

    public void trimToSize() {
        if (this.count != this.value.length) {
            int[] oldValue = this.value;
            this.value = new int[this.count];
            System.arraycopy(oldValue, 0, this.value, 0, this.count);
        }
    }
}
