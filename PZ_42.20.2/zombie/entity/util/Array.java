// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jspecify.annotations.Nullable;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.entity.util.reflect.ArrayReflection;

@UsedFromLua
public class Array<T> implements Iterable<T> {
    public T[] items;
    public int size;
    public boolean ordered;
    private Array.ArrayIterable<T> iterable;
    private Predicate.PredicateIterable<T> predicateIterable;

    public Array() {
        this(true, 16);
    }

    public Array(int capacity) {
        this(true, capacity);
    }

    public Array(boolean ordered, int capacity) {
        this.ordered = ordered;
        this.items = (T[])(new Object[capacity]);
    }

    public Array(boolean ordered, int capacity, Class<?> arrayType) {
        this.ordered = ordered;
        this.items = (T[])((Object[])ArrayReflection.newInstance(arrayType, capacity));
    }

    public Array(Class<?> arrayType) {
        this(true, 16, arrayType);
    }

    public Array(Array<? extends T> array) {
        this(array.ordered, array.size, array.items.getClass().getComponentType());
        this.size = array.size;
        System.arraycopy(array.items, 0, this.items, 0, this.size);
    }

    public Array(T[] array) {
        this(true, array, 0, array.length);
    }

    public Array(boolean ordered, T[] array, int start, int count) {
        this(ordered, count, array.getClass().getComponentType());
        this.size = count;
        System.arraycopy(array, start, this.items, 0, this.size);
    }

    public void add(T value) {
        T[] items = this.items;
        if (this.size == items.length) {
            items = this.resize(Math.max(8, (int)(this.size * 1.75F)));
        }

        items[this.size++] = value;
    }

    public void add(T value1, T value2) {
        T[] items = this.items;
        if (this.size + 1 >= items.length) {
            items = this.resize(Math.max(8, (int)(this.size * 1.75F)));
        }

        items[this.size] = value1;
        items[this.size + 1] = value2;
        this.size += 2;
    }

    public void add(T value1, T value2, T value3) {
        T[] items = this.items;
        if (this.size + 2 >= items.length) {
            items = this.resize(Math.max(8, (int)(this.size * 1.75F)));
        }

        items[this.size] = value1;
        items[this.size + 1] = value2;
        items[this.size + 2] = value3;
        this.size += 3;
    }

    public void add(T value1, T value2, T value3, T value4) {
        T[] items = this.items;
        if (this.size + 3 >= items.length) {
            items = this.resize(Math.max(8, (int)(this.size * 1.8F)));
        }

        items[this.size] = value1;
        items[this.size + 1] = value2;
        items[this.size + 2] = value3;
        items[this.size + 3] = value4;
        this.size += 4;
    }

    public void addAll(Array<? extends T> array) {
        this.addAll((T[])array.items, 0, array.size);
    }

    public void addAll(Array<? extends T> array, int start, int count) {
        if (start + count > array.size) {
            throw new IllegalArgumentException("start + count must be <= size: " + start + " + " + count + " <= " + array.size);
        } else {
            this.addAll((T[])array.items, start, count);
        }
    }

    public void addAll(T... array) {
        this.addAll(array, 0, array.length);
    }

    public void addAll(T[] array, int start, int count) {
        T[] items = this.items;
        int sizeNeeded = this.size + count;
        if (sizeNeeded > items.length) {
            items = this.resize(Math.max(Math.max(8, sizeNeeded), (int)(this.size * 1.75F)));
        }

        System.arraycopy(array, start, items, this.size, count);
        this.size = sizeNeeded;
    }

    public T get(int index) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        } else {
            return this.items[index];
        }
    }

    public void set(int index, T value) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        } else {
            this.items[index] = value;
        }
    }

    public void insert(int index, T value) {
        if (index > this.size) {
            throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + this.size);
        } else {
            T[] items = this.items;
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
            T[] items = this.items;
            T firstValue = items[first];
            items[first] = items[second];
            items[second] = firstValue;
        }
    }

    public boolean contains(@Nullable T value, boolean identity) {
        T[] items = this.items;
        int i = this.size - 1;
        if (!identity && value != null) {
            while (i >= 0) {
                if (value.equals(items[i--])) {
                    return true;
                }
            }
        } else {
            while (i >= 0) {
                if (items[i--] == value) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean containsAll(Array<? extends T> values, boolean identity) {
        T[] items = (T[])values.items;
        int i = 0;

        for (int n = values.size; i < n; i++) {
            if (!this.contains(items[i], identity)) {
                return false;
            }
        }

        return true;
    }

    public boolean containsAny(Array<? extends T> values, boolean identity) {
        T[] items = (T[])values.items;
        int i = 0;

        for (int n = values.size; i < n; i++) {
            if (this.contains(items[i], identity)) {
                return true;
            }
        }

        return false;
    }

    public int indexOf(@Nullable T value, boolean identity) {
        T[] items = this.items;
        if (!identity && value != null) {
            int i = 0;

            for (int n = this.size; i < n; i++) {
                if (value.equals(items[i])) {
                    return i;
                }
            }
        } else {
            int i = 0;

            for (int nx = this.size; i < nx; i++) {
                if (items[i] == value) {
                    return i;
                }
            }
        }

        return -1;
    }

    public int lastIndexOf(@Nullable T value, boolean identity) {
        T[] items = this.items;
        if (!identity && value != null) {
            for (int i = this.size - 1; i >= 0; i--) {
                if (value.equals(items[i])) {
                    return i;
                }
            }
        } else {
            for (int ix = this.size - 1; ix >= 0; ix--) {
                if (items[ix] == value) {
                    return ix;
                }
            }
        }

        return -1;
    }

    public boolean removeValue(@Nullable T value, boolean identity) {
        T[] items = this.items;
        if (!identity && value != null) {
            int i = 0;

            for (int n = this.size; i < n; i++) {
                if (value.equals(items[i])) {
                    this.removeIndex(i);
                    return true;
                }
            }
        } else {
            int i = 0;

            for (int nx = this.size; i < nx; i++) {
                if (items[i] == value) {
                    this.removeIndex(i);
                    return true;
                }
            }
        }

        return false;
    }

    public T removeIndex(int index) {
        if (index >= this.size) {
            throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + this.size);
        } else {
            T[] items = this.items;
            T value = items[index];
            this.size--;
            if (this.ordered) {
                System.arraycopy(items, index + 1, items, index, this.size - index);
            } else {
                items[index] = items[this.size];
            }

            items[this.size] = null;
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
            T[] items = this.items;
            int count = end - start + 1;
            int lastIndex = n - count;
            if (this.ordered) {
                System.arraycopy(items, start + count, items, start, n - (start + count));
            } else {
                int i = Math.max(lastIndex, end + 1);
                System.arraycopy(items, i, items, start, n - i);
            }

            for (int i = lastIndex; i < n; i++) {
                items[i] = null;
            }

            this.size = n - count;
        }
    }

    public boolean removeAll(Array<? extends T> array, boolean identity) {
        int size = this.size;
        int startSize = size;
        T[] items = this.items;
        if (identity) {
            int i = 0;

            for (int n = array.size; i < n; i++) {
                T item = (T)array.get(i);

                for (int ii = 0; ii < size; ii++) {
                    if (item == items[ii]) {
                        this.removeIndex(ii);
                        size--;
                        break;
                    }
                }
            }
        } else {
            int i = 0;

            for (int n = array.size; i < n; i++) {
                T item = (T)array.get(i);

                for (int iix = 0; iix < size; iix++) {
                    if (item.equals(items[iix])) {
                        this.removeIndex(iix);
                        size--;
                        break;
                    }
                }
            }
        }

        return size != startSize;
    }

    public T pop() {
        if (this.size == 0) {
            throw new IllegalStateException("Array is empty.");
        } else {
            this.size--;
            T item = this.items[this.size];
            this.items[this.size] = null;
            return item;
        }
    }

    public T peek() {
        if (this.size == 0) {
            throw new IllegalStateException("Array is empty.");
        } else {
            return this.items[this.size - 1];
        }
    }

    public T first() {
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
        Arrays.fill(this.items, 0, this.size, null);
        this.size = 0;
    }

    public T[] shrink() {
        if (this.items.length != this.size) {
            this.resize(this.size);
        }

        return this.items;
    }

    public T[] ensureCapacity(int additionalCapacity) {
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

    public T[] setSize(int newSize) {
        this.truncate(newSize);
        if (newSize > this.items.length) {
            this.resize(Math.max(8, newSize));
        }

        this.size = newSize;
        return this.items;
    }

    protected T[] resize(int newSize) {
        T[] items = this.items;
        T[] newItems = (T[])((Object[])ArrayReflection.newInstance(items.getClass().getComponentType(), newSize));
        System.arraycopy(items, 0, newItems, 0, Math.min(this.size, newItems.length));
        this.items = newItems;
        return newItems;
    }

    public void sort() {
        Sort.instance().sort(this.items, 0, this.size);
    }

    public void sort(Comparator<? super T> comparator) {
        Sort.instance().sort(this.items, comparator, 0, this.size);
    }

    public T selectRanked(Comparator<T> comparator, int kthLowest) {
        if (kthLowest < 1) {
            throw new RuntimeException("nth_lowest must be greater than 0, 1 = first, 2 = second...");
        } else {
            return Select.instance().select(this.items, comparator, kthLowest, this.size);
        }
    }

    public int selectRankedIndex(Comparator<T> comparator, int kthLowest) {
        if (kthLowest < 1) {
            throw new RuntimeException("nth_lowest must be greater than 0, 1 = first, 2 = second...");
        } else {
            return Select.instance().selectIndex(this.items, comparator, kthLowest, this.size);
        }
    }

    public void reverse() {
        T[] items = this.items;
        int i = 0;
        int lastIndex = this.size - 1;

        for (int n = this.size / 2; i < n; i++) {
            int ii = lastIndex - i;
            T temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    public void shuffle() {
        T[] items = this.items;

        for (int i = this.size - 1; i >= 0; i--) {
            int ii = Rand.Next(i);
            T temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    public Array.ArrayIterator<T> iterator() {
        if (Collections.allocateIterators) {
            return new Array.ArrayIterator<>(this, true);
        } else {
            if (this.iterable == null) {
                this.iterable = new Array.ArrayIterable<>(this);
            }

            return this.iterable.iterator();
        }
    }

    public Iterable<T> select(Predicate<T> predicate) {
        if (Collections.allocateIterators) {
            return new Predicate.PredicateIterable<>(this, predicate);
        } else {
            if (this.predicateIterable == null) {
                this.predicateIterable = new Predicate.PredicateIterable<>(this, predicate);
            } else {
                this.predicateIterable.set(this, predicate);
            }

            return this.predicateIterable;
        }
    }

    public void truncate(int newSize) {
        if (newSize < 0) {
            throw new IllegalArgumentException("newSize must be >= 0: " + newSize);
        } else if (this.size > newSize) {
            for (int i = newSize; i < this.size; i++) {
                this.items[i] = null;
            }

            this.size = newSize;
        }
    }

    public @Nullable T random() {
        return this.size == 0 ? null : this.items[Rand.Next(0, this.size - 1)];
    }

    public T[] toArray() {
        return (T[])this.toArray(this.items.getClass().getComponentType());
    }

    public <V> V[] toArray(Class<V> type) {
        V[] result = (V[])ArrayReflection.newInstance(type, this.size);
        System.arraycopy(this.items, 0, result, 0, this.size);
        return result;
    }

    @Override
    public int hashCode() {
        if (!this.ordered) {
            return super.hashCode();
        } else {
            Object[] items = this.items;
            int h = 1;
            int i = 0;

            for (int n = this.size; i < n; i++) {
                h *= 31;
                Object item = items[i];
                if (item != null) {
                    h += item.hashCode();
                }
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
        } else if (!(object instanceof Array<?> array)) {
            return false;
        } else if (!array.ordered) {
            return false;
        } else {
            int n = this.size;
            if (n != array.size) {
                return false;
            } else {
                Object[] items1 = this.items;
                Object[] items2 = array.items;

                for (int i = 0; i < n; i++) {
                    Object o1 = items1[i];
                    Object o2 = items2[i];
                    if (o1 == null ? o2 != null : !o1.equals(o2)) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    public boolean equalsIdentity(Object object) {
        if (object == this) {
            return true;
        } else if (!this.ordered) {
            return false;
        } else if (object instanceof Array<?> array) {
            if (!array.ordered) {
                return false;
            } else {
                int n = this.size;
                if (n != array.size) {
                    return false;
                } else {
                    Object[] items1 = this.items;
                    Object[] items2 = array.items;

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
            T[] items = this.items;
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
            T[] items = this.items;
            StringBuilder buffer = new StringBuilder(32);
            buffer.append(items[0]);

            for (int i = 1; i < this.size; i++) {
                buffer.append(separator);
                buffer.append(items[i]);
            }

            return buffer.toString();
        }
    }

    public static <T> Array<T> of(Class<T> arrayType) {
        return new Array<>(arrayType);
    }

    public static <T> Array<T> of(boolean ordered, int capacity, Class<T> arrayType) {
        return new Array<>(ordered, capacity, arrayType);
    }

    public static <T> Array<T> with(T... array) {
        return new Array<>(array);
    }

    public static class ArrayIterable<T> implements Iterable<T> {
        private final Array<T> array;
        private final boolean allowRemove;
        private Array.ArrayIterator<T> iterator1;
        private Array.ArrayIterator<T> iterator2;

        public ArrayIterable(Array<T> array) {
            this(array, true);
        }

        public ArrayIterable(Array<T> array, boolean allowRemove) {
            this.array = array;
            this.allowRemove = allowRemove;
        }

        public Array.ArrayIterator<T> iterator() {
            if (Collections.allocateIterators) {
                return new Array.ArrayIterator<>(this.array, this.allowRemove);
            } else {
                if (this.iterator1 == null) {
                    this.iterator1 = new Array.ArrayIterator<>(this.array, this.allowRemove);
                    this.iterator2 = new Array.ArrayIterator<>(this.array, this.allowRemove);
                }

                if (!this.iterator1.valid) {
                    this.iterator1.index = 0;
                    this.iterator1.valid = true;
                    this.iterator2.valid = false;
                    return this.iterator1;
                } else {
                    this.iterator2.index = 0;
                    this.iterator2.valid = true;
                    this.iterator1.valid = false;
                    return this.iterator2;
                }
            }
        }
    }

    public static class ArrayIterator<T> implements Iterator<T>, Iterable<T> {
        private final Array<T> array;
        private final boolean allowRemove;
        int index;
        boolean valid = true;

        public ArrayIterator(Array<T> array) {
            this(array, true);
        }

        public ArrayIterator(Array<T> array, boolean allowRemove) {
            this.array = array;
            this.allowRemove = allowRemove;
        }

        @Override
        public boolean hasNext() {
            if (!this.valid) {
                throw new RuntimeException("#iterator() cannot be used nested.");
            } else {
                return this.index < this.array.size;
            }
        }

        @Override
        public T next() {
            if (this.index >= this.array.size) {
                throw new NoSuchElementException(String.valueOf(this.index));
            } else if (!this.valid) {
                throw new RuntimeException("#iterator() cannot be used nested.");
            } else {
                return this.array.items[this.index++];
            }
        }

        @Override
        public void remove() {
            if (!this.allowRemove) {
                throw new RuntimeException("Remove not allowed.");
            } else {
                this.index--;
                this.array.removeIndex(this.index);
            }
        }

        public void reset() {
            this.index = 0;
        }

        public Array.ArrayIterator<T> iterator() {
            return this;
        }
    }
}
