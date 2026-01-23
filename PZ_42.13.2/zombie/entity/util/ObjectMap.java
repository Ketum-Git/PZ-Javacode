// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;

public class ObjectMap<K, V> implements Iterable<ObjectMap.Entry<K, V>> {
    static final Object dummy = new Object();
    public int size;
    K[] keyTable;
    V[] valueTable;
    float loadFactor;
    int threshold;
    protected int shift;
    protected int mask;
    transient ObjectMap.Entries<K, V> entries1;
    transient ObjectMap.Entries<K, V> entries2;
    transient ObjectMap.Values<V> values1;
    transient ObjectMap.Values<V> values2;
    transient ObjectMap.Keys<K> keys1;
    transient ObjectMap.Keys<K> keys2;

    public ObjectMap() {
        this(51, 0.8F);
    }

    public ObjectMap(int initialCapacity) {
        this(initialCapacity, 0.8F);
    }

    public ObjectMap(int initialCapacity, float loadFactor) {
        if (!(loadFactor <= 0.0F) && !(loadFactor >= 1.0F)) {
            this.loadFactor = loadFactor;
            int tableSize = ObjectSet.tableSize(initialCapacity, loadFactor);
            this.threshold = (int)(tableSize * loadFactor);
            this.mask = tableSize - 1;
            this.shift = Long.numberOfLeadingZeros(this.mask);
            this.keyTable = (K[])(new Object[tableSize]);
            this.valueTable = (V[])(new Object[tableSize]);
        } else {
            throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
        }
    }

    public ObjectMap(ObjectMap<? extends K, ? extends V> map) {
        this((int)(map.keyTable.length * map.loadFactor), map.loadFactor);
        System.arraycopy(map.keyTable, 0, this.keyTable, 0, map.keyTable.length);
        System.arraycopy(map.valueTable, 0, this.valueTable, 0, map.valueTable.length);
        this.size = map.size;
    }

    protected int place(K item) {
        return (int)(item.hashCode() * -7046029254386353131L >>> this.shift);
    }

    int locateKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null.");
        } else {
            K[] keyTable = this.keyTable;
            int i = this.place(key);

            while (true) {
                K other = keyTable[i];
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

    public @Nullable V put(K key, @Nullable V value) {
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

    public void putAll(ObjectMap<? extends K, ? extends V> map) {
        this.ensureCapacity(map.size);
        K[] keyTable = (K[])map.keyTable;
        V[] valueTable = (V[])map.valueTable;
        int i = 0;

        for (int n = keyTable.length; i < n; i++) {
            K key = keyTable[i];
            if (key != null) {
                this.put(key, valueTable[i]);
            }
        }
    }

    private void putResize(K key, @Nullable V value) {
        K[] keyTable = this.keyTable;
        int i = this.place(key);

        while (keyTable[i] != null) {
            i = i + 1 & this.mask;
        }

        keyTable[i] = key;
        this.valueTable[i] = value;
    }

    public <T extends K> @Nullable V get(T key) {
        int i = this.locateKey((K)key);
        return i < 0 ? null : this.valueTable[i];
    }

    public V get(K key, @Nullable V defaultValue) {
        int i = this.locateKey(key);
        return i < 0 ? defaultValue : this.valueTable[i];
    }

    public @Nullable V remove(K key) {
        int i = this.locateKey(key);
        if (i < 0) {
            return null;
        } else {
            K[] keyTable = this.keyTable;
            V[] valueTable = this.valueTable;
            V oldValue = valueTable[i];
            int mask = this.mask;

            for (int next = i + 1 & mask; (key = keyTable[next]) != null; next = next + 1 & mask) {
                int placement = this.place(key);
                if ((next - placement & mask) > (i - placement & mask)) {
                    keyTable[i] = key;
                    valueTable[i] = valueTable[next];
                    i = next;
                }
            }

            keyTable[i] = null;
            valueTable[i] = null;
            this.size--;
            return oldValue;
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
            this.resize(tableSize);
        }
    }

    public void clear() {
        if (this.size != 0) {
            this.size = 0;
            Arrays.fill(this.keyTable, null);
            Arrays.fill(this.valueTable, null);
        }
    }

    public boolean containsValue(@Nullable Object value, boolean identity) {
        V[] valueTable = this.valueTable;
        if (value == null) {
            K[] keyTable = this.keyTable;

            for (int i = valueTable.length - 1; i >= 0; i--) {
                if (keyTable[i] != null && valueTable[i] == null) {
                    return true;
                }
            }
        } else if (identity) {
            for (int ix = valueTable.length - 1; ix >= 0; ix--) {
                if (valueTable[ix] == value) {
                    return true;
                }
            }
        } else {
            for (int ixx = valueTable.length - 1; ixx >= 0; ixx--) {
                if (value.equals(valueTable[ixx])) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean containsKey(K key) {
        return this.locateKey(key) >= 0;
    }

    public @Nullable K findKey(@Nullable Object value, boolean identity) {
        V[] valueTable = this.valueTable;
        if (value == null) {
            K[] keyTable = this.keyTable;

            for (int i = valueTable.length - 1; i >= 0; i--) {
                if (keyTable[i] != null && valueTable[i] == null) {
                    return keyTable[i];
                }
            }
        } else if (identity) {
            for (int ix = valueTable.length - 1; ix >= 0; ix--) {
                if (valueTable[ix] == value) {
                    return this.keyTable[ix];
                }
            }
        } else {
            for (int ixx = valueTable.length - 1; ixx >= 0; ixx--) {
                if (value.equals(valueTable[ixx])) {
                    return this.keyTable[ixx];
                }
            }
        }

        return null;
    }

    public void ensureCapacity(int additionalCapacity) {
        int tableSize = ObjectSet.tableSize(this.size + additionalCapacity, this.loadFactor);
        if (this.keyTable.length < tableSize) {
            this.resize(tableSize);
        }
    }

    final void resize(int newSize) {
        int oldCapacity = this.keyTable.length;
        this.threshold = (int)(newSize * this.loadFactor);
        this.mask = newSize - 1;
        this.shift = Long.numberOfLeadingZeros(this.mask);
        K[] oldKeyTable = this.keyTable;
        V[] oldValueTable = this.valueTable;
        this.keyTable = (K[])(new Object[newSize]);
        this.valueTable = (V[])(new Object[newSize]);
        if (this.size > 0) {
            for (int i = 0; i < oldCapacity; i++) {
                K key = oldKeyTable[i];
                if (key != null) {
                    this.putResize(key, oldValueTable[i]);
                }
            }
        }
    }

    @Override
    public int hashCode() {
        int h = this.size;
        K[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        int i = 0;

        for (int n = keyTable.length; i < n; i++) {
            K key = keyTable[i];
            if (key != null) {
                h += key.hashCode();
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
        } else if (obj instanceof ObjectMap other) {
            if (other.size != this.size) {
                return false;
            } else {
                K[] keyTable = this.keyTable;
                V[] valueTable = this.valueTable;
                int i = 0;

                for (int n = keyTable.length; i < n; i++) {
                    K key = keyTable[i];
                    if (key != null) {
                        V value = valueTable[i];
                        if (value == null) {
                            if (other.get(key, dummy) != null) {
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
        } else if (obj instanceof ObjectMap other) {
            if (other.size != this.size) {
                return false;
            } else {
                K[] keyTable = this.keyTable;
                V[] valueTable = this.valueTable;
                int i = 0;

                for (int n = keyTable.length; i < n; i++) {
                    K key = keyTable[i];
                    if (key != null && valueTable[i] != other.get(key, dummy)) {
                        return false;
                    }
                }

                return true;
            }
        } else {
            return false;
        }
    }

    public String toString(String separator) {
        return this.toString(separator, false);
    }

    @Override
    public String toString() {
        return this.toString(", ", true);
    }

    protected String toString(String separator, boolean braces) {
        if (this.size == 0) {
            return braces ? "{}" : "";
        } else {
            StringBuilder buffer = new StringBuilder(32);
            if (braces) {
                buffer.append('{');
            }

            K[] keyTable = this.keyTable;
            V[] valueTable = this.valueTable;
            int i = keyTable.length;

            while (i-- > 0) {
                K key = keyTable[i];
                if (key != null) {
                    buffer.append(key == this ? "(this)" : key);
                    buffer.append('=');
                    V value = valueTable[i];
                    buffer.append(value == this ? "(this)" : value);
                    break;
                }
            }

            while (i-- > 0) {
                K key = keyTable[i];
                if (key != null) {
                    buffer.append(separator);
                    buffer.append(key == this ? "(this)" : key);
                    buffer.append('=');
                    V value = valueTable[i];
                    buffer.append(value == this ? "(this)" : value);
                }
            }

            if (braces) {
                buffer.append('}');
            }

            return buffer.toString();
        }
    }

    public ObjectMap.Entries<K, V> iterator() {
        return this.entries();
    }

    public ObjectMap.Entries<K, V> entries() {
        if (Collections.allocateIterators) {
            return new ObjectMap.Entries<>(this);
        } else {
            if (this.entries1 == null) {
                this.entries1 = new ObjectMap.Entries<>(this);
                this.entries2 = new ObjectMap.Entries<>(this);
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

    public ObjectMap.Values<V> values() {
        if (Collections.allocateIterators) {
            return new ObjectMap.Values<>(this);
        } else {
            if (this.values1 == null) {
                this.values1 = new ObjectMap.Values<>(this);
                this.values2 = new ObjectMap.Values<>(this);
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

    public ObjectMap.Keys<K> keys() {
        if (Collections.allocateIterators) {
            return new ObjectMap.Keys<>(this);
        } else {
            if (this.keys1 == null) {
                this.keys1 = new ObjectMap.Keys<>(this);
                this.keys2 = new ObjectMap.Keys<>(this);
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

    public static class Entries<K, V> extends ObjectMap.MapIterator<K, V, ObjectMap.Entry<K, V>> {
        ObjectMap.Entry<K, V> entry = new ObjectMap.Entry<>();

        public Entries(ObjectMap<K, V> map) {
            super(map);
        }

        public ObjectMap.Entry<K, V> next() {
            if (!this.hasNext) {
                throw new NoSuchElementException();
            } else if (!this.valid) {
                throw new RuntimeException("#iterator() cannot be used nested.");
            } else {
                K[] keyTable = this.map.keyTable;
                this.entry.key = keyTable[this.nextIndex];
                this.entry.value = this.map.valueTable[this.nextIndex];
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

        public ObjectMap.Entries<K, V> iterator() {
            return this;
        }
    }

    public static class Entry<K, V> {
        public K key;
        public @Nullable V value;

        @Override
        public String toString() {
            return this.key + "=" + this.value;
        }
    }

    public static class Keys<K> extends ObjectMap.MapIterator<K, Object, K> {
        public Keys(ObjectMap<K, ?> map) {
            super((ObjectMap<K, Object>)map);
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
                K key = this.map.keyTable[this.nextIndex];
                this.currentIndex = this.nextIndex;
                this.findNextIndex();
                return key;
            }
        }

        public ObjectMap.Keys<K> iterator() {
            return this;
        }

        public Array<K> toArray() {
            return this.toArray(new Array<>(true, this.map.size));
        }

        public Array<K> toArray(Array<K> array) {
            while (this.hasNext) {
                array.add(this.next());
            }

            return array;
        }
    }

    private abstract static class MapIterator<K, V, I> implements Iterable<I>, Iterator<I> {
        public boolean hasNext;
        final ObjectMap<K, V> map;
        int nextIndex;
        int currentIndex;
        boolean valid = true;

        public MapIterator(ObjectMap<K, V> map) {
            this.map = map;
            this.reset();
        }

        public void reset() {
            this.currentIndex = -1;
            this.nextIndex = -1;
            this.findNextIndex();
        }

        void findNextIndex() {
            K[] keyTable = this.map.keyTable;
            int n = keyTable.length;

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
                K[] keyTable = this.map.keyTable;
                V[] valueTable = this.map.valueTable;
                int mask = this.map.mask;

                K key;
                for (int next = i + 1 & mask; (key = keyTable[next]) != null; next = next + 1 & mask) {
                    int placement = this.map.place(key);
                    if ((next - placement & mask) > (i - placement & mask)) {
                        keyTable[i] = key;
                        valueTable[i] = valueTable[next];
                        i = next;
                    }
                }

                keyTable[i] = null;
                valueTable[i] = null;
                this.map.size--;
                if (i != this.currentIndex) {
                    this.nextIndex--;
                }

                this.currentIndex = -1;
            }
        }
    }

    public static class Values<V> extends ObjectMap.MapIterator<Object, V, V> {
        public Values(ObjectMap<?, V> map) {
            super((ObjectMap<Object, V>)map);
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
                V value = this.map.valueTable[this.nextIndex];
                this.currentIndex = this.nextIndex;
                this.findNextIndex();
                return value;
            }
        }

        public ObjectMap.Values<V> iterator() {
            return this;
        }

        public Array<V> toArray() {
            return this.toArray(new Array<>(true, this.map.size));
        }

        public Array<V> toArray(Array<V> array) {
            while (this.hasNext) {
                array.add(this.next());
            }

            return array;
        }
    }
}
