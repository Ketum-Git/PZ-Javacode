// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

import java.util.Iterator;
import zombie.UsedFromLua;

@UsedFromLua
public class ImmutableArray<T> implements Iterable<T> {
    private final Array<T> array;
    private Array.ArrayIterable<T> iterable;

    public ImmutableArray(Array<T> array) {
        this.array = array;
    }

    public int size() {
        return this.array.size;
    }

    public T get(int index) {
        return this.array.get(index);
    }

    public boolean contains(T value, boolean identity) {
        return this.array.contains(value, identity);
    }

    public int indexOf(T value, boolean identity) {
        return this.array.indexOf(value, identity);
    }

    public int lastIndexOf(T value, boolean identity) {
        return this.array.lastIndexOf(value, identity);
    }

    public T peek() {
        return this.array.peek();
    }

    public T first() {
        return this.array.first();
    }

    public T random() {
        return this.array.random();
    }

    public T[] toArray() {
        return this.array.toArray();
    }

    public <V> V[] toArray(Class<V> type) {
        return this.array.toArray(type);
    }

    @Override
    public int hashCode() {
        return this.array.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return this.array.equals(object);
    }

    @Override
    public String toString() {
        return this.array.toString();
    }

    public String toString(String separator) {
        return this.array.toString(separator);
    }

    @Override
    public Iterator<T> iterator() {
        if (this.iterable == null) {
            this.iterable = new Array.ArrayIterable<>(this.array, false);
        }

        return this.iterable.iterator();
    }
}
