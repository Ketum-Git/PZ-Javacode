// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util.assoc;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.function.BiConsumer;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;

@UsedFromLua
public class AssocArray<K, V> {
    private static final int DEFAULT_CAPACITY = 10;
    private static final Object[] EMPTY_ELEMENTDATA = new Object[0];
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = new Object[0];
    transient Object[] elementData;
    private int size;
    protected transient int modCount;

    public AssocArray(int initialCapacity) {
        initialCapacity *= 2;
        if (initialCapacity > 0) {
            this.elementData = new Object[initialCapacity];
        } else {
            if (initialCapacity != 0) {
                throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
            }

            this.elementData = EMPTY_ELEMENTDATA;
        }
    }

    public AssocArray() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    public void trimToSize() {
        this.modCount++;
        int realSize = this.realSize();
        if (realSize < this.elementData.length) {
            this.elementData = realSize == 0 ? EMPTY_ELEMENTDATA : Arrays.copyOf(this.elementData, realSize);
        }
    }

    public void ensureCapacity(int minCapacity) {
        if (minCapacity * 2 > this.elementData.length && (this.elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA || minCapacity > 10)) {
            this.modCount++;
            this.grow(minCapacity);
        }
    }

    private Object[] grow(int minCapacity) {
        minCapacity *= 2;
        int oldCapacity = this.elementData.length;
        if (oldCapacity <= 0 && this.elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            return this.elementData = new Object[Math.max(10, minCapacity)];
        } else {
            int newCapacity = Math.max(minCapacity - oldCapacity, oldCapacity >> 1) + oldCapacity;
            if (newCapacity < 0) {
                throw new OutOfMemoryError("Required array length too large");
            } else {
                return this.elementData = Arrays.copyOf(this.elementData, newCapacity);
            }
        }
    }

    private Object[] grow() {
        return this.grow(this.size + 1);
    }

    protected int getBackingSize() {
        return this.elementData.length;
    }

    public int size() {
        return this.size;
    }

    protected int realSize() {
        return this.size * 2;
    }

    protected int realKeyIndex(int index) {
        return index * 2;
    }

    protected int realValueIndex(int index) {
        return index * 2 + 1;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public boolean containsKey(K o) {
        return this.indexOfKey(o) >= 0;
    }

    public boolean containsValue(V o) {
        return this.indexOfValue(o) >= 0;
    }

    public int indexOfKey(K o) {
        return this.indexOfRange(o, 0, this.realSize(), 0);
    }

    public int indexOfValue(V o) {
        return this.indexOfRange(o, 0, this.realSize(), 1);
    }

    int indexOfRange(Object o, int start, int end, int offset) {
        Object[] es = this.elementData;
        if (o != null) {
            for (int i = start + offset; i < end; i += 2) {
                if (o.equals(es[i])) {
                    return (i - offset) / 2;
                }
            }
        }

        return -1;
    }

    public int lastIndexOfKey(K o) {
        return this.lastIndexOfRange(o, 0, this.realSize(), 1);
    }

    public int lastIndexOfValue(V o) {
        return this.lastIndexOfRange(o, 0, this.realSize(), 0);
    }

    int lastIndexOfRange(Object o, int start, int end, int offset) {
        Object[] es = this.elementData;
        if (o != null) {
            for (int i = end - offset - 1; i >= start; i -= 2) {
                if (o.equals(es[i])) {
                    return (i - (1 - offset)) / 2;
                }
            }
        }

        return -1;
    }

    K keyData(int frontIndex) {
        return (K)this.elementData[this.realKeyIndex(frontIndex)];
    }

    V valueData(int frontIndex) {
        return (V)this.elementData[this.realValueIndex(frontIndex)];
    }

    static <E> E valueAt(Object[] es, int index) {
        return (E)es[index * 2 + 1];
    }

    static <E> E keyAt(Object[] es, int index) {
        return (E)es[index * 2];
    }

    public K getKey(int frontIndex) {
        Objects.checkIndex(frontIndex, this.size);
        return this.keyData(frontIndex);
    }

    public V getValue(int frontIndex) {
        Objects.checkIndex(frontIndex, this.size);
        return this.valueData(frontIndex);
    }

    public V set(K k, V v) {
        Objects.requireNonNull(k);
        Objects.requireNonNull(v);
        int index = this.indexOfKey(k);
        Objects.checkIndex(index, this.size);
        return this.setInternal(index, v);
    }

    private V setInternal(int frontIndex, V v) {
        V oldValue = this.valueData(frontIndex);
        this.elementData[this.realValueIndex(frontIndex)] = v;
        return oldValue;
    }

    public V put(K k, V v) {
        Objects.requireNonNull(k);
        Objects.requireNonNull(v);
        int index = this.indexOfKey(k);
        if (index >= 0) {
            return this.setInternal(index, v);
        } else {
            this.add(k, v);
            return null;
        }
    }

    public V get(K k) {
        int index = this.indexOfKey(k);
        if (index < 0) {
            return null;
        } else {
            V e;
            return (e = this.getValue(index)) == null ? null : e;
        }
    }

    private void add(K k, V v, Object[] elementData, int s) {
        if (s * 2 + 1 >= elementData.length) {
            elementData = this.grow();
        }

        elementData[this.realKeyIndex(s)] = k;
        elementData[this.realValueIndex(s)] = v;
        this.size = s + 1;
    }

    public boolean add(K k, V v) {
        Objects.requireNonNull(k);
        Objects.requireNonNull(v);
        if (this.containsKey(k)) {
            throw new UnsupportedOperationException("Key already exists.");
        } else {
            this.modCount++;
            this.add(k, v, this.elementData, this.size);
            return true;
        }
    }

    public void add(int frontIndex, K k, V v) {
        Objects.requireNonNull(k);
        Objects.requireNonNull(v);
        if (this.containsKey(k)) {
            throw new UnsupportedOperationException("Key already exists.");
        } else {
            this.rangeCheckForAdd(frontIndex);
            this.modCount++;
            int s = this.realSize();
            int realIndex = this.realKeyIndex(frontIndex);
            Object[] elementData = this.elementData;
            if (s == this.elementData.length) {
                elementData = this.grow();
            }

            System.arraycopy(elementData, realIndex, elementData, realIndex + 2, s - realIndex);
            elementData[realIndex] = k;
            elementData[realIndex + 1] = v;
            this.size++;
        }
    }

    public V removeIndex(int frontIndex) {
        Objects.checkIndex(frontIndex, this.size);
        Object[] es = this.elementData;
        V oldValue = (V)es[this.realValueIndex(frontIndex)];
        this.fastRemove(es, this.realKeyIndex(frontIndex));
        return oldValue;
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass() == AssocArray.class && o == this;
    }

    private void checkForComodification(int expectedModCount) {
        if (this.modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public int hashCode() {
        int expectedModCount = this.modCount;
        int hash = this.hashCodeRange(0, this.realSize());
        this.checkForComodification(expectedModCount);
        return hash;
    }

    int hashCodeRange(int from, int to) {
        Object[] es = this.elementData;
        if (to > es.length) {
            throw new ConcurrentModificationException();
        } else {
            int hashCode = 1;

            for (int i = from; i < to; i++) {
                Object e = es[i];
                hashCode = 31 * hashCode + (e == null ? 0 : e.hashCode());
            }

            return hashCode;
        }
    }

    public V remove(K o) {
        Object[] es = this.elementData;
        int size = this.realSize();
        int i = 0;
        if (o != null) {
            while (i < size) {
                if (o.equals(es[i])) {
                    V val = (V)es[i + 1];
                    this.fastRemove(es, i);
                    return val;
                }

                i += 2;
            }
        }

        return null;
    }

    protected void fastRemove(Object[] es, int realIndex) {
        this.modCount++;
        int newSize;
        if ((newSize = this.size - 1) > realIndex / 2) {
            System.arraycopy(es, realIndex + 2, es, realIndex, newSize * 2 - realIndex);
        }

        this.size = newSize;
        es[this.size * 2] = null;
        es[this.size * 2 + 1] = null;
    }

    public void clear() {
        this.modCount++;
        Object[] es = this.elementData;
        int to = this.realSize();

        for (int i = this.size = 0; i < to; i++) {
            es[i] = null;
        }
    }

    public void putAll(AssocArray<K, V> other) {
        for (int i = 0; i < other.size; i++) {
            K otherK = other.getKey(i);
            V otherV = other.getValue(i);
            if (this.containsKey(otherK)) {
                this.put(otherK, otherV);
            } else {
                this.add(otherK, otherV);
            }
        }
    }

    public void addAll(AssocArray<K, V> other) {
        for (int i = 0; i < other.size; i++) {
            K otherK = other.getKey(i);
            V otherV = other.getValue(i);
            if (!this.containsKey(otherK)) {
                this.add(otherK, otherV);
            }
        }
    }

    public void setAll(AssocArray<K, V> other) {
        for (int i = 0; i < other.size; i++) {
            K otherK = other.getKey(i);
            V otherV = other.getValue(i);
            if (this.containsKey(otherK)) {
                this.set(otherK, otherV);
            }
        }
    }

    private void rangeCheckForAdd(int frontIndex) {
        if (frontIndex > this.size || frontIndex < 0) {
            throw new IndexOutOfBoundsException(this.outOfBoundsMsg(frontIndex));
        }
    }

    private String outOfBoundsMsg(int fontIndex) {
        return "Index: " + fontIndex + ", Size: " + this.size;
    }

    private static String outOfBoundsMsg(int fromIndex, int toIndex) {
        return "From Index: " + fromIndex + " > To Index: " + toIndex;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = this.modCount;
        Object[] es = this.elementData;
        int size = this.size;

        for (int i = 0; this.modCount == expectedModCount && i < size; i++) {
            action.accept(keyAt(es, i), valueAt(es, i));
        }

        if (this.modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    protected void debugPrint() {
        DebugLog.log("--- Contents ---");
        DebugLog.log("Size = " + this.size + ", real size = " + this.realSize());
        DebugLog.log("backing array size = " + this.elementData.length);

        for (int i = 0; i < this.size; i++) {
            DebugLog.log("[" + i + "][" + this.realKeyIndex(i) + "] " + this.elementData[this.realKeyIndex(i)]);
            DebugLog.log("   [" + this.realValueIndex(i) + "] " + this.elementData[this.realValueIndex(i)]);
        }
    }
}
