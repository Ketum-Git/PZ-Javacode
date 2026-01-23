// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

import java.util.Arrays;
import zombie.core.random.Rand;

public class LongArray {
    public long[] items;
    public int size;
    public boolean ordered;

    public LongArray() {
        this(true, 16);
    }

    public LongArray(int capacity) {
        this(true, capacity);
    }

    public LongArray(boolean ordered, int capacity) {
        this.ordered = ordered;
        this.items = new long[capacity];
    }

    public LongArray(LongArray array) {
        this.ordered = array.ordered;
        this.size = array.size;
        this.items = new long[this.size];
        System.arraycopy(array.items, 0, this.items, 0, this.size);
    }

    public LongArray(long[] array) {
        this(true, array, 0, array.length);
    }

    public LongArray(boolean ordered, long[] array, int startIndex, int count) {
        this(ordered, count);
        this.size = count;
        System.arraycopy(array, startIndex, this.items, 0, count);
    }

    public void add(long value) {
        long[] items = this.items;
        if (this.size == items.length) {
            items = this.resize(Math.max(8, (int)(this.size * 1.75F)));
        }

        items[this.size++] = value;
    }

    public void add(long value1, long value2) {
        long[] items = this.items;
        if (this.size + 1 >= items.length) {
            items = this.resize(Math.max(8, (int)(this.size * 1.75F)));
        }

        items[this.size] = value1;
        items[this.size + 1] = value2;
        this.size += 2;
    }

    public void add(long value1, long value2, long value3) {
        long[] items = this.items;
        if (this.size + 2 >= items.length) {
            items = this.resize(Math.max(8, (int)(this.size * 1.75F)));
        }

        items[this.size] = value1;
        items[this.size + 1] = value2;
        items[this.size + 2] = value3;
        this.size += 3;
    }

    public void add(long value1, long value2, long value3, long value4) {
        long[] items = this.items;
        if (this.size + 3 >= items.length) {
            items = this.resize(Math.max(8, (int)(this.size * 1.8F)));
        }

        items[this.size] = value1;
        items[this.size + 1] = value2;
        items[this.size + 2] = value3;
        items[this.size + 3] = value4;
        this.size += 4;
    }

    public void addAll(LongArray array) {
        this.addAll(array.items, 0, array.size);
    }

    public void addAll(LongArray array, int offset, int length) {
        if (offset + length > array.size) {
            throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
        } else {
            this.addAll(array.items, offset, length);
        }
    }

    public void addAll(long... array) {
        this.addAll(array, 0, array.length);
    }

    public void addAll(long[] array, int offset, int length) {
        long[] items = this.items;
        int sizeNeeded = this.size + length;
        if (sizeNeeded > items.length) {
            items = this.resize(Math.max(Math.max(8, sizeNeeded), (int)(this.size * 1.75F)));
        }

        System.arraycopy(array, offset, items, this.size, length);
        this.size += length;
    }

    public long get(int index) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        } else {
            return this.items[index];
        }
    }

    public void set(int index, long value) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        } else {
            this.items[index] = value;
        }
    }

    public void incr(int index, long value) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        } else {
            this.items[index] = this.items[index] + value;
        }
    }

    public void incr(long value) {
        long[] items = this.items;
        int i = 0;

        for (int n = this.size; i < n; i++) {
            items[i] += value;
        }
    }

    public void mul(int index, long value) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        } else {
            this.items[index] = this.items[index] * value;
        }
    }

    public void mul(long value) {
        long[] items = this.items;
        int i = 0;

        for (int n = this.size; i < n; i++) {
            items[i] *= value;
        }
    }

    public void insert(int index, long value) {
        if (index > this.size) {
            throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + this.size);
        } else {
            long[] items = this.items;
            if (this.size == items.length) {
                items = this.resize(Math.max(8, (int)(this.size * 1.75F)));
            }

            if (this.ordered) {
                System.arraycopy(items, index, items, index + 1, this.size - index);
            } else {
                items[this.size] = items[index];
            }

            this.size++;
            items[index] = value;
        }
    }

    public void insertRange(int index, int count) {
        if (index > this.size) {
            throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + this.size);
        } else {
            int sizeNeeded = this.size + count;
            if (sizeNeeded > this.items.length) {
                this.items = this.resize(Math.max(Math.max(8, sizeNeeded), (int)(this.size * 1.75F)));
            }

            System.arraycopy(this.items, index, this.items, index + count, this.size - index);
            this.size = sizeNeeded;
        }
    }

    public void swap(int first, int second) {
        if (first >= this.size) {
            throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + this.size);
        } else if (second >= this.size) {
            throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + this.size);
        } else {
            long[] items = this.items;
            long firstValue = items[first];
            items[first] = items[second];
            items[second] = firstValue;
        }
    }

    public boolean contains(long value) {
        int i = this.size - 1;
        long[] items = this.items;

        while (i >= 0) {
            if (items[i--] == value) {
                return true;
            }
        }

        return false;
    }

    public int indexOf(long value) {
        long[] items = this.items;
        int i = 0;

        for (int n = this.size; i < n; i++) {
            if (items[i] == value) {
                return i;
            }
        }

        return -1;
    }

    public int lastIndexOf(char value) {
        long[] items = this.items;

        for (int i = this.size - 1; i >= 0; i--) {
            if (items[i] == value) {
                return i;
            }
        }

        return -1;
    }

    public boolean removeValue(long value) {
        long[] items = this.items;
        int i = 0;

        for (int n = this.size; i < n; i++) {
            if (items[i] == value) {
                this.removeIndex(i);
                return true;
            }
        }

        return false;
    }

    public long removeIndex(int index) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        } else {
            long[] items = this.items;
            long value = items[index];
            this.size--;
            if (this.ordered) {
                System.arraycopy(items, index + 1, items, index, this.size - index);
            } else {
                items[index] = items[this.size];
            }

            return value;
        }
    }

    public void removeRange(int start, int end) {
        int n = this.size;
        if (end >= n) {
            throw new IndexOutOfBoundsException("end can't be >= size: " + end + " >= " + this.size);
        } else if (start > end) {
            throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end);
        } else {
            int count = end - start + 1;
            int lastIndex = n - count;
            if (this.ordered) {
                System.arraycopy(this.items, start + count, this.items, start, n - (start + count));
            } else {
                int i = Math.max(lastIndex, end + 1);
                System.arraycopy(this.items, i, this.items, start, n - i);
            }

            this.size = n - count;
        }
    }

    public boolean removeAll(LongArray array) {
        int size = this.size;
        int startSize = size;
        long[] items = this.items;
        int i = 0;

        for (int n = array.size; i < n; i++) {
            long item = array.get(i);

            for (int ii = 0; ii < size; ii++) {
                if (item == items[ii]) {
                    this.removeIndex(ii);
                    size--;
                    break;
                }
            }
        }

        return size != startSize;
    }

    public long pop() {
        return this.items[--this.size];
    }

    public long peek() {
        return this.items[this.size - 1];
    }

    public long first() {
        if (this.size == 0) {
            throw new IllegalStateException("Array is empty.");
        } else {
            return this.items[0];
        }
    }

    public boolean notEmpty() {
        return this.size > 0;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public void clear() {
        this.size = 0;
    }

    public long[] shrink() {
        if (this.items.length != this.size) {
            this.resize(this.size);
        }

        return this.items;
    }

    public long[] ensureCapacity(int additionalCapacity) {
        if (additionalCapacity < 0) {
            throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
        } else {
            int sizeNeeded = this.size + additionalCapacity;
            if (sizeNeeded > this.items.length) {
                this.resize(Math.max(Math.max(8, sizeNeeded), (int)(this.size * 1.75F)));
            }

            return this.items;
        }
    }

    public long[] setSize(int newSize) {
        if (newSize < 0) {
            throw new IllegalArgumentException("newSize must be >= 0: " + newSize);
        } else {
            if (newSize > this.items.length) {
                this.resize(Math.max(8, newSize));
            }

            this.size = newSize;
            return this.items;
        }
    }

    protected long[] resize(int newSize) {
        long[] newItems = new long[newSize];
        long[] items = this.items;
        System.arraycopy(items, 0, newItems, 0, Math.min(this.size, newItems.length));
        this.items = newItems;
        return newItems;
    }

    public void sort() {
        Arrays.sort(this.items, 0, this.size);
    }

    public void reverse() {
        long[] items = this.items;
        int i = 0;
        int lastIndex = this.size - 1;

        for (int n = this.size / 2; i < n; i++) {
            int ii = lastIndex - i;
            long temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    public void shuffle() {
        long[] items = this.items;

        for (int i = this.size - 1; i >= 0; i--) {
            int ii = Rand.Next(i);
            long temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    public void truncate(int newSize) {
        if (this.size > newSize) {
            this.size = newSize;
        }
    }

    public long random() {
        return this.size == 0 ? 0L : this.items[Rand.Next(0, this.size - 1)];
    }

    public long[] toArray() {
        long[] array = new long[this.size];
        System.arraycopy(this.items, 0, array, 0, this.size);
        return array;
    }

    @Override
    public int hashCode() {
        if (!this.ordered) {
            return super.hashCode();
        } else {
            long[] items = this.items;
            int h = 1;
            int i = 0;

            for (int n = this.size; i < n; i++) {
                long item = items[i];
                h = h * 31 + (int)(item ^ item >>> 32);
            }

            return h;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        } else if (!this.ordered) {
            return false;
        } else if (object instanceof LongArray array) {
            if (!array.ordered) {
                return false;
            } else {
                int n = this.size;
                if (n != array.size) {
                    return false;
                } else {
                    long[] items1 = this.items;
                    long[] items2 = array.items;

                    for (int i = 0; i < n; i++) {
                        if (items1[i] != items2[i]) {
                            return false;
                        }
                    }

                    return true;
                }
            }
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        if (this.size == 0) {
            return "[]";
        } else {
            long[] items = this.items;
            StringBuilder buffer = new StringBuilder(32);
            buffer.append('[');
            buffer.append(items[0]);

            for (int i = 1; i < this.size; i++) {
                buffer.append(", ");
                buffer.append(items[i]);
            }

            buffer.append(']');
            return buffer.toString();
        }
    }

    public String toString(String separator) {
        if (this.size == 0) {
            return "";
        } else {
            long[] items = this.items;
            StringBuilder buffer = new StringBuilder(32);
            buffer.append(items[0]);

            for (int i = 1; i < this.size; i++) {
                buffer.append(separator);
                buffer.append(items[i]);
            }

            return buffer.toString();
        }
    }

    public static LongArray with(long... array) {
        return new LongArray(array);
    }
}
