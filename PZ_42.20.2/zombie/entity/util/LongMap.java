// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;

public class LongMap<V> implements Iterable<LongMap.Entry<V>> {
    public int size;
    long[] keyTable;
    V[] valueTable;
    V zeroValue;
    boolean hasZeroValue;
    private final float loadFactor;
    private int threshold;
    protected int shift;
    protected int mask;
    private transient LongMap.Entries<V> entries1;
    private transient LongMap.Entries<V> entries2;
    private transient LongMap.Values<V> values1;
    private transient LongMap.Values<V> values2;
    private transient LongMap.Keys<V> keys1;
    private transient LongMap.Keys<V> keys2;

    public LongMap() {
        this(51, 0.8F);
    }

    public LongMap(int initialCapacity) {
        this(initialCapacity, 0.8F);
    }

    public LongMap(int initialCapacity, float loadFactor) {
        if (!(loadFactor <= 0.0F) && !(loadFactor >= 1.0F)) {
            this.loadFactor = loadFactor;
            int tableSize = ObjectSet.tableSize(initialCapacity, loadFactor);
            this.threshold = (int)(tableSize * loadFactor);
            this.mask = tableSize - 1;
            this.shift = Long.numberOfLeadingZeros(this.mask);
            this.keyTable = new long[tableSize];
            this.valueTable = (V[])(new Object[tableSize]);
        } else {
            throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
        }
    }

    public LongMap(LongMap<? extends V> map) {
        this((int)(map.keyTable.length * map.loadFactor), map.loadFactor);
        System.arraycopy(map.keyTable, 0, this.keyTable, 0, map.keyTable.length);
        System.arraycopy(map.valueTable, 0, this.valueTable, 0, map.valueTable.length);
        this.size = map.size;
        this.zeroValue = (V)map.zeroValue;
        this.hasZeroValue = map.hasZeroValue;
    }

    protected int place(long item) {
        return (int)((item ^ item >>> 32) * -7046029254386353131L >>> this.shift);
    }

    private int locateKey(long key) {
        long[] keyTable = this.keyTable;
        int i = this.place(key);

        while (true) {
            long other = keyTable[i];
            if (other == 0L) {
                return -(i + 1);
            }

            if (other == key) {
                return i;
            }

            i = i + 1 & this.mask;
        }
    }

    public @Nullable V put(long key, @Nullable V value) {
        if (key == 0L) {
            V oldValue = this.zeroValue;
            this.zeroValue = value;
            if (!this.hasZeroValue) {
                this.hasZeroValue = true;
                this.size++;
            }

            return oldValue;
        } else {
            int i = this.locateKey(key);
            if (i >= 0) {
                V oldValue = this.valueTable[i];
                this.valueTable[i] = value;
                return oldValue;
            } else {
                i = -(i + 1);
                this.keyTable[i] = key;
                this.valueTable[i] = value;
                if (++this.size >= this.threshold) {
                    this.resize(this.keyTable.length << 1);
                }

                return null;
            }
        }
    }

    public void putAll(LongMap<? extends V> map) {
        this.ensureCapacity(map.size);
        if (map.hasZeroValue) {
            this.put(0L, (V)map.zeroValue);
        }

        long[] keyTable = map.keyTable;
        V[] valueTable = (V[])map.valueTable;
        int i = 0;

        for (int n = keyTable.length; i < n; i++) {
            long key = keyTable[i];
            if (key != 0L) {
                this.put(key, valueTable[i]);
            }
        }
    }

    private void putResize(long key, @Nullable V value) {
        long[] keyTable = this.keyTable;
        int i = this.place(key);

        while (keyTable[i] != 0L) {
            i = i + 1 & this.mask;
        }

        keyTable[i] = key;
        this.valueTable[i] = value;
    }

    public @Nullable V get(long key) {
        if (key == 0L) {
            return this.hasZeroValue ? this.zeroValue : null;
        } else {
            int i = this.locateKey(key);
            return i >= 0 ? this.valueTable[i] : null;
        }
    }

    public V get(long key, @Nullable V defaultValue) {
        if (key == 0L) {
            return this.hasZeroValue ? this.zeroValue : defaultValue;
        } else {
            int i = this.locateKey(key);
            return i >= 0 ? this.valueTable[i] : defaultValue;
        }
    }

    public @Nullable V remove(long key) {
        if (key == 0L) {
            if (!this.hasZeroValue) {
                return null;
            } else {
                this.hasZeroValue = false;
                V oldValue = this.zeroValue;
                this.zeroValue = null;
                this.size--;
                return oldValue;
            }
        } else {
            int i = this.locateKey(key);
            if (i < 0) {
                return null;
            } else {
                long[] keyTable = this.keyTable;
                V[] valueTable = this.valueTable;
                V oldValue = valueTable[i];
                int mask = this.mask;

                for (int next = i + 1 & mask; (key = keyTable[next]) != 0L; next = next + 1 & mask) {
                    int placement = this.place(key);
                    if ((next - placement & mask) > (i - placement & mask)) {
                        keyTable[i] = key;
                        valueTable[i] = valueTable[next];
                        i = next;
                    }
                }

                keyTable[i] = 0L;
                valueTable[i] = null;
                this.size--;
                return oldValue;
            }
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
            int tableSize = ObjectSet.tableSize(maximumCapacity, this.loadFactor);
            if (this.keyTable.length > tableSize) {
                this.resize(tableSize);
            }
        }
    }

    public void clear(int maximumCapacity) {
        int tableSize = ObjectSet.tableSize(maximumCapacity, this.loadFactor);
        if (this.keyTable.length <= tableSize) {
            this.clear();
        } else {
            this.size = 0;
            this.hasZeroValue = false;
            this.zeroValue = null;
            this.resize(tableSize);
        }
    }

    public void clear() {
        if (this.size != 0) {
            this.size = 0;
            Arrays.fill(this.keyTable, 0L);
            Arrays.fill(this.valueTable, null);
            this.zeroValue = null;
            this.hasZeroValue = false;
        }
    }

    public boolean containsValue(@Nullable Object value, boolean identity) {
        V[] valueTable = this.valueTable;
        if (value == null) {
            if (this.hasZeroValue && this.zeroValue == null) {
                return true;
            }

            long[] keyTable = this.keyTable;

            for (int i = valueTable.length - 1; i >= 0; i--) {
                if (keyTable[i] != 0L && valueTable[i] == null) {
                    return true;
                }
            }
        } else if (identity) {
            if (value == this.zeroValue) {
                return true;
            }

            for (int ix = valueTable.length - 1; ix >= 0; ix--) {
                if (valueTable[ix] == value) {
                    return true;
                }
            }
        } else {
            if (this.hasZeroValue && value.equals(this.zeroValue)) {
                return true;
            }

            for (int ixx = valueTable.length - 1; ixx >= 0; ixx--) {
                if (value.equals(valueTable[ixx])) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean containsKey(long key) {
        return key == 0L ? this.hasZeroValue : this.locateKey(key) >= 0;
    }

    public long findKey(@Nullable Object value, boolean identity, long notFound) {
        V[] valueTable = this.valueTable;
        if (value == null) {
            if (this.hasZeroValue && this.zeroValue == null) {
                return 0L;
            }

            long[] keyTable = this.keyTable;

            for (int i = valueTable.length - 1; i >= 0; i--) {
                if (keyTable[i] != 0L && valueTable[i] == null) {
                    return keyTable[i];
                }
            }
        } else if (identity) {
            if (value == this.zeroValue) {
                return 0L;
            }

            for (int ix = valueTable.length - 1; ix >= 0; ix--) {
                if (valueTable[ix] == value) {
                    return this.keyTable[ix];
                }
            }
        } else {
            if (this.hasZeroValue && value.equals(this.zeroValue)) {
                return 0L;
            }

            for (int ixx = valueTable.length - 1; ixx >= 0; ixx--) {
                if (value.equals(valueTable[ixx])) {
                    return this.keyTable[ixx];
                }
            }
        }

        return notFound;
    }

    public void ensureCapacity(int additionalCapacity) {
        int tableSize = ObjectSet.tableSize(this.size + additionalCapacity, this.loadFactor);
        if (this.keyTable.length < tableSize) {
            this.resize(tableSize);
        }
    }

    private void resize(int newSize) {
        int oldCapacity = this.keyTable.length;
        this.threshold = (int)(newSize * this.loadFactor);
        this.mask = newSize - 1;
        this.shift = Long.numberOfLeadingZeros(this.mask);
        long[] oldKeyTable = this.keyTable;
        V[] oldValueTable = this.valueTable;
        this.keyTable = new long[newSize];
        this.valueTable = (V[])(new Object[newSize]);
        if (this.size > 0) {
            for (int i = 0; i < oldCapacity; i++) {
                long key = oldKeyTable[i];
                if (key != 0L) {
                    this.putResize(key, oldValueTable[i]);
                }
            }
        }
    }

    @Override
    public int hashCode() {
        int h = this.size;
        if (this.hasZeroValue && this.zeroValue != null) {
            h += this.zeroValue.hashCode();
        }

        long[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        int i = 0;

        for (int n = keyTable.length; i < n; i++) {
            long key = keyTable[i];
            if (key != 0L) {
                h = (int)(h + key * 31L);
                V value = valueTable[i];
                if (value != null) {
                    h += value.hashCode();
                }
            }
        }

        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof LongMap other) {
            if (other.size != this.size) {
                return false;
            } else if (other.hasZeroValue != this.hasZeroValue) {
                return false;
            } else {
                if (this.hasZeroValue) {
                    if (other.zeroValue == null) {
                        if (this.zeroValue != null) {
                            return false;
                        }
                    } else if (!other.zeroValue.equals(this.zeroValue)) {
                        return false;
                    }
                }

                long[] keyTable = this.keyTable;
                V[] valueTable = this.valueTable;
                int i = 0;

                for (int n = keyTable.length; i < n; i++) {
                    long key = keyTable[i];
                    if (key != 0L) {
                        V value = valueTable[i];
                        if (value == null) {
                            if (other.get(key, ObjectMap.dummy) != null) {
                                return false;
                            }
                        } else if (!value.equals(other.get(key))) {
                            return false;
                        }
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public boolean equalsIdentity(@Nullable Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof LongMap other) {
            if (other.size != this.size) {
                return false;
            } else if (other.hasZeroValue != this.hasZeroValue) {
                return false;
            } else if (this.hasZeroValue && this.zeroValue != other.zeroValue) {
                return false;
            } else {
                long[] keyTable = this.keyTable;
                V[] valueTable = this.valueTable;
                int i = 0;

                for (int n = keyTable.length; i < n; i++) {
                    long key = keyTable[i];
                    if (key != 0L && valueTable[i] != other.get(key, ObjectMap.dummy)) {
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
        if (this.size == 0) {
            return "[]";
        } else {
            StringBuilder buffer = new StringBuilder(32);
            buffer.append('[');
            long[] keyTable = this.keyTable;
            V[] valueTable = this.valueTable;
            int i = keyTable.length;
            if (this.hasZeroValue) {
                buffer.append("0=");
                buffer.append(this.zeroValue);
            } else {
                while (i-- > 0) {
                    long key = keyTable[i];
                    if (key != 0L) {
                        buffer.append(key);
                        buffer.append('=');
                        buffer.append(valueTable[i]);
                        break;
                    }
                }
            }

            while (i-- > 0) {
                long key = keyTable[i];
                if (key != 0L) {
                    buffer.append(", ");
                    buffer.append(key);
                    buffer.append('=');
                    buffer.append(valueTable[i]);
                }
            }

            buffer.append(']');
            return buffer.toString();
        }
    }

    @Override
    public Iterator<LongMap.Entry<V>> iterator() {
        return this.entries();
    }

    public LongMap.Entries<V> entries() {
        if (Collections.allocateIterators) {
            return new LongMap.Entries<>(this);
        } else {
            if (this.entries1 == null) {
                this.entries1 = new LongMap.Entries<>(this);
                this.entries2 = new LongMap.Entries<>(this);
            }

            if (!this.entries1.valid) {
                this.entries1.reset();
                this.entries1.valid = true;
                this.entries2.valid = false;
                return this.entries1;
            } else {
                this.entries2.reset();
                this.entries2.valid = true;
                this.entries1.valid = false;
                return this.entries2;
            }
        }
    }

    public LongMap.Values<V> values() {
        if (Collections.allocateIterators) {
            return new LongMap.Values<>(this);
        } else {
            if (this.values1 == null) {
                this.values1 = new LongMap.Values<>(this);
                this.values2 = new LongMap.Values<>(this);
            }

            if (!this.values1.valid) {
                this.values1.reset();
                this.values1.valid = true;
                this.values2.valid = false;
                return this.values1;
            } else {
                this.values2.reset();
                this.values2.valid = true;
                this.values1.valid = false;
                return this.values2;
            }
        }
    }

    public LongMap.Keys<V> keys() {
        if (Collections.allocateIterators) {
            return new LongMap.Keys<>(this);
        } else {
            if (this.keys1 == null) {
                this.keys1 = new LongMap.Keys<>(this);
                this.keys2 = new LongMap.Keys<>(this);
            }

            if (!this.keys1.valid) {
                this.keys1.reset();
                this.keys1.valid = true;
                this.keys2.valid = false;
                return this.keys1;
            } else {
                this.keys2.reset();
                this.keys2.valid = true;
                this.keys1.valid = false;
                return this.keys2;
            }
        }
    }

    public static class Entries<V> extends LongMap.MapIterator<V> implements Iterable<LongMap.Entry<V>>, Iterator<LongMap.Entry<V>> {
        private final LongMap.Entry<V> entry = new LongMap.Entry<>();

        public Entries(LongMap<V> map) {
            super(map);
        }

        public LongMap.Entry<V> next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            } else if (!this.valid) {
                throw new RuntimeException("#iterator() cannot be used nested.");
            } else {
                long[] keyTable = this.map.keyTable;
                if (this.nextIndex == -1) {
                    this.entry.key = 0L;
                    this.entry.value = this.map.zeroValue;
                } else {
                    this.entry.key = keyTable[this.nextIndex];
                    this.entry.value = this.map.valueTable[this.nextIndex];
                }

                this.currentIndex = this.nextIndex;
                this.findNextIndex();
                return this.entry;
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
        public Iterator<LongMap.Entry<V>> iterator() {
            return this;
        }
    }

    public static class Entry<V> {
        public long key;
        public @Nullable V value;

        @Override
        public String toString() {
            return this.key + "=" + this.value;
        }
    }

    public static class Keys<V> extends LongMap.MapIterator<V> {
        public Keys(LongMap<V> map) {
            super(map);
        }

        public long next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            } else if (!this.valid) {
                throw new RuntimeException("#iterator() cannot be used nested.");
            } else {
                long key = this.nextIndex == -1 ? 0L : this.map.keyTable[this.nextIndex];
                this.currentIndex = this.nextIndex;
                this.findNextIndex();
                return key;
            }
        }

        public LongArray toArray() {
            LongArray array = new LongArray(true, this.map.size);

            while (this.hasNext) {
                array.add(this.next());
            }

            return array;
        }

        public LongArray toArray(LongArray array) {
            while (this.hasNext) {
                array.add(this.next());
            }

            return array;
        }
    }

    private static class MapIterator<V> {
        private static final int INDEX_ILLEGAL = -2;
        static final int INDEX_ZERO = -1;
        public boolean hasNext;
        final LongMap<V> map;
        int nextIndex;
        int currentIndex;
        boolean valid = true;

        public MapIterator(LongMap<V> map) {
            this.map = map;
            this.reset();
        }

        public void reset() {
            this.currentIndex = -2;
            this.nextIndex = -1;
            if (this.map.hasZeroValue) {
                this.hasNext = true;
            } else {
                this.findNextIndex();
            }
        }

        void findNextIndex() {
            long[] keyTable = this.map.keyTable;
            int n = keyTable.length;

            while (++this.nextIndex < n) {
                if (keyTable[this.nextIndex] != 0L) {
                    this.hasNext = true;
                    return;
                }
            }

            this.hasNext = false;
        }

        public void remove() {
            int i = this.currentIndex;
            if (i == -1 && this.map.hasZeroValue) {
                this.map.hasZeroValue = false;
                this.map.zeroValue = null;
            } else {
                if (i < 0) {
                    throw new IllegalStateException("next must be called before remove.");
                }

                long[] keyTable = this.map.keyTable;
                V[] valueTable = this.map.valueTable;
                int mask = this.map.mask;

                long key;
                for (int next = i + 1 & mask; (key = keyTable[next]) != 0L; next = next + 1 & mask) {
                    int placement = this.map.place(key);
                    if ((next - placement & mask) > (i - placement & mask)) {
                        keyTable[i] = key;
                        valueTable[i] = valueTable[next];
                        i = next;
                    }
                }

                keyTable[i] = 0L;
                valueTable[i] = null;
                if (i != this.currentIndex) {
                    this.nextIndex--;
                }
            }

            this.currentIndex = -2;
            this.map.size--;
        }
    }

    public static class Values<V> extends LongMap.MapIterator<V> implements Iterable<V>, Iterator<V> {
        public Values(LongMap<V> map) {
            super(map);
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
        public @Nullable V next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            } else if (!this.valid) {
                throw new RuntimeException("#iterator() cannot be used nested.");
            } else {
                V value;
                if (this.nextIndex == -1) {
                    value = this.map.zeroValue;
                } else {
                    value = this.map.valueTable[this.nextIndex];
                }

                this.currentIndex = this.nextIndex;
                this.findNextIndex();
                return value;
            }
        }

        @Override
        public Iterator<V> iterator() {
            return this;
        }

        public Array<V> toArray() {
            Array<V> array = new Array<>(true, this.map.size);

            while (this.hasNext) {
                array.add(this.next());
            }

            return array;
        }
    }
}
