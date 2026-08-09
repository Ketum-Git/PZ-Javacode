// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;
import zombie.core.math.PZMath;

public class ObjectSet<T> implements Iterable<T> {
    public int size;
    T[] keyTable;
    float loadFactor;
    int threshold;
    protected int shift;
    protected int mask;
    private transient ObjectSet.ObjectSetIterator<T> iterator1;
    private transient ObjectSet.ObjectSetIterator<T> iterator2;

    public ObjectSet() {
        this(51, 0.8F);
    }

    public ObjectSet(int initialCapacity) {
        this(initialCapacity, 0.8F);
    }

    public ObjectSet(int initialCapacity, float loadFactor) {
        if (!(loadFactor <= 0.0F) && !(loadFactor >= 1.0F)) {
            this.loadFactor = loadFactor;
            int tableSize = tableSize(initialCapacity, loadFactor);
            this.threshold = (int)(tableSize * loadFactor);
            this.mask = tableSize - 1;
            this.shift = Long.numberOfLeadingZeros(this.mask);
            this.keyTable = (T[])(new Object[tableSize]);
        } else {
            throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
        }
    }

    public ObjectSet(ObjectSet<? extends T> set) {
        this((int)(set.keyTable.length * set.loadFactor), set.loadFactor);
        System.arraycopy(set.keyTable, 0, this.keyTable, 0, set.keyTable.length);
        this.size = set.size;
    }

    protected int place(T item) {
        return (int)(item.hashCode() * -7046029254386353131L >>> this.shift);
    }

    int locateKey(T key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null.");
        } else {
            T[] keyTable = this.keyTable;
            int i = this.place(key);

            while (true) {
                T other = keyTable[i];
                if (other == null) {
                    return -(i + 1);
                }

                if (other.equals(key)) {
                    return i;
                }

                i = i + 1 & this.mask;
            }
        }
    }

    public boolean add(T key) {
        int i = this.locateKey(key);
        if (i >= 0) {
            return false;
        } else {
            i = -(i + 1);
            this.keyTable[i] = key;
            if (++this.size >= this.threshold) {
                this.resize(this.keyTable.length << 1);
            }

            return true;
        }
    }

    public void addAll(Array<? extends T> array) {
        this.addAll((T[])array.items, 0, array.size);
    }

    public void addAll(Array<? extends T> array, int offset, int length) {
        if (offset + length > array.size) {
            throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
        } else {
            this.addAll((T[])array.items, offset, length);
        }
    }

    public boolean addAll(T... array) {
        return this.addAll(array, 0, array.length);
    }

    public boolean addAll(T[] array, int offset, int length) {
        this.ensureCapacity(length);
        int oldSize = this.size;
        int i = offset;

        for (int n = offset + length; i < n; i++) {
            this.add(array[i]);
        }

        return oldSize != this.size;
    }

    public void addAll(ObjectSet<T> set) {
        this.ensureCapacity(set.size);
        T[] keyTable = set.keyTable;
        int i = 0;

        for (int n = keyTable.length; i < n; i++) {
            T key = keyTable[i];
            if (key != null) {
                this.add(key);
            }
        }
    }

    private void addResize(T key) {
        T[] keyTable = this.keyTable;
        int i = this.place(key);

        while (keyTable[i] != null) {
            i = i + 1 & this.mask;
        }

        keyTable[i] = key;
    }

    public boolean remove(T key) {
        int i = this.locateKey(key);
        if (i < 0) {
            return false;
        } else {
            T[] keyTable = this.keyTable;
            int mask = this.mask;

            for (int next = i + 1 & mask; (key = keyTable[next]) != null; next = next + 1 & mask) {
                int placement = this.place(key);
                if ((next - placement & mask) > (i - placement & mask)) {
                    keyTable[i] = key;
                    i = next;
                }
            }

            keyTable[i] = null;
            this.size--;
            return true;
        }
    }

    public boolean notEmpty() {
        return this.size > 0;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public void shrink(int maximumCapacity) {
        if (maximumCapacity < 0) {
            throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
        } else {
            int tableSize = tableSize(maximumCapacity, this.loadFactor);
            if (this.keyTable.length > tableSize) {
                this.resize(tableSize);
            }
        }
    }

    public void clear(int maximumCapacity) {
        int tableSize = tableSize(maximumCapacity, this.loadFactor);
        if (this.keyTable.length <= tableSize) {
            this.clear();
        } else {
            this.size = 0;
            this.resize(tableSize);
        }
    }

    public void clear() {
        if (this.size != 0) {
            this.size = 0;
            Arrays.fill(this.keyTable, null);
        }
    }

    public boolean contains(T key) {
        return this.locateKey(key) >= 0;
    }

    public @Nullable T get(T key) {
        int i = this.locateKey(key);
        return i < 0 ? null : this.keyTable[i];
    }

    public T first() {
        T[] keyTable = this.keyTable;
        int i = 0;

        for (int n = keyTable.length; i < n; i++) {
            if (keyTable[i] != null) {
                return keyTable[i];
            }
        }

        throw new IllegalStateException("ObjectSet is empty.");
    }

    public void ensureCapacity(int additionalCapacity) {
        int tableSize = tableSize(this.size + additionalCapacity, this.loadFactor);
        if (this.keyTable.length < tableSize) {
            this.resize(tableSize);
        }
    }

    private void resize(int newSize) {
        int oldCapacity = this.keyTable.length;
        this.threshold = (int)(newSize * this.loadFactor);
        this.mask = newSize - 1;
        this.shift = Long.numberOfLeadingZeros(this.mask);
        T[] oldKeyTable = this.keyTable;
        this.keyTable = (T[])(new Object[newSize]);
        if (this.size > 0) {
            for (int i = 0; i < oldCapacity; i++) {
                T key = oldKeyTable[i];
                if (key != null) {
                    this.addResize(key);
                }
            }
        }
    }

    @Override
    public int hashCode() {
        int h = this.size;
        T[] keyTable = this.keyTable;
        int i = 0;

        for (int n = keyTable.length; i < n; i++) {
            T key = keyTable[i];
            if (key != null) {
                h += key.hashCode();
            }
        }

        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ObjectSet other) {
            if (other.size != this.size) {
                return false;
            } else {
                T[] keyTable = this.keyTable;
                int i = 0;

                for (int n = keyTable.length; i < n; i++) {
                    if (keyTable[i] != null && !other.contains(keyTable[i])) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "{" + this.toString(", ") + "}";
    }

    public String toString(String separator) {
        if (this.size == 0) {
            return "";
        } else {
            StringBuilder buffer = new StringBuilder(32);
            T[] keyTable = this.keyTable;
            int i = keyTable.length;

            while (i-- > 0) {
                T key = keyTable[i];
                if (key != null) {
                    buffer.append(key == this ? "(this)" : key);
                    break;
                }
            }

            while (i-- > 0) {
                T key = keyTable[i];
                if (key != null) {
                    buffer.append(separator);
                    buffer.append(key == this ? "(this)" : key);
                }
            }

            return buffer.toString();
        }
    }

    public ObjectSet.ObjectSetIterator<T> iterator() {
        if (Collections.allocateIterators) {
            return new ObjectSet.ObjectSetIterator<>(this);
        } else {
            if (this.iterator1 == null) {
                this.iterator1 = new ObjectSet.ObjectSetIterator<>(this);
                this.iterator2 = new ObjectSet.ObjectSetIterator<>(this);
            }

            if (!this.iterator1.valid) {
                this.iterator1.reset();
                this.iterator1.valid = true;
                this.iterator2.valid = false;
                return this.iterator1;
            } else {
                this.iterator2.reset();
                this.iterator2.valid = true;
                this.iterator1.valid = false;
                return this.iterator2;
            }
        }
    }

    public static <T> ObjectSet<T> with(T... array) {
        ObjectSet<T> set = new ObjectSet<>();
        set.addAll(array);
        return set;
    }

    static int tableSize(int capacity, float loadFactor) {
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity must be >= 0: " + capacity);
        } else {
            int tableSize = PZMath.nextPowerOfTwo(Math.max(2, (int)Math.ceil(capacity / loadFactor)));
            if (tableSize > 1073741824) {
                throw new IllegalArgumentException("The required capacity is too large: " + capacity);
            } else {
                return tableSize;
            }
        }
    }

    public static class ObjectSetIterator<K> implements Iterable<K>, Iterator<K> {
        public boolean hasNext;
        final ObjectSet<K> set;
        int nextIndex;
        int currentIndex;
        boolean valid = true;

        public ObjectSetIterator(ObjectSet<K> set) {
            this.set = set;
            this.reset();
        }

        public void reset() {
            this.currentIndex = -1;
            this.nextIndex = -1;
            this.findNextIndex();
        }

        private void findNextIndex() {
            K[] keyTable = this.set.keyTable;
            int n = this.set.keyTable.length;

            while (++this.nextIndex < n) {
                if (keyTable[this.nextIndex] != null) {
                    this.hasNext = true;
                    return;
                }
            }

            this.hasNext = false;
        }

        @Override
        public void remove() {
            int i = this.currentIndex;
            if (i < 0) {
                throw new IllegalStateException("next must be called before remove.");
            } else {
                K[] keyTable = this.set.keyTable;
                int mask = this.set.mask;

                K key;
                for (int next = i + 1 & mask; (key = keyTable[next]) != null; next = next + 1 & mask) {
                    int placement = this.set.place(key);
                    if ((next - placement & mask) > (i - placement & mask)) {
                        keyTable[i] = key;
                        i = next;
                    }
                }

                keyTable[i] = null;
                this.set.size--;
                if (i != this.currentIndex) {
                    this.nextIndex--;
                }

                this.currentIndex = -1;
            }
        }

        @Override
        public boolean hasNext() {
            if (!this.valid) {
                throw new RuntimeException("#iterator() cannot be used nested.");
            } else {
                return this.hasNext;
            }
        }

        @Override
        public K next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            } else if (!this.valid) {
                throw new RuntimeException("#iterator() cannot be used nested.");
            } else {
                K key = this.set.keyTable[this.nextIndex];
                this.currentIndex = this.nextIndex;
                this.findNextIndex();
                return key;
            }
        }

        public ObjectSet.ObjectSetIterator<K> iterator() {
            return this;
        }

        public Array<K> toArray(Array<K> array) {
            while (this.hasNext) {
                array.add(this.next());
            }

            return array;
        }

        public Array<K> toArray() {
            return this.toArray(new Array(true, this.set.size));
        }
    }
}
