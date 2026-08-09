// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.list;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class PZUnmodifiableCollection<E> implements Collection<E> {
    final Collection<? extends E> c;

    PZUnmodifiableCollection(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        } else {
            this.c = c;
        }
    }

    @Override
    public int size() {
        return this.c.size();
    }

    @Override
    public boolean isEmpty() {
        return this.c.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.c.contains(o);
    }

    @Override
    public Object[] toArray() {
        return this.c.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return (T[])this.c.toArray(a);
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> f) {
        return this.c.toArray(f);
    }

    @Override
    public String toString() {
        return this.c.toString();
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Iterator<? extends E> i;

            {
                Objects.requireNonNull(PZUnmodifiableCollection.this);
                this.i = PZUnmodifiableCollection.this.c.iterator();
            }

            @Override
            public boolean hasNext() {
                return this.i.hasNext();
            }

            @Override
            public E next() {
                return (E)this.i.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void forEachRemaining(Consumer<? super E> action) {
                this.i.forEachRemaining(action);
            }
        };
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> coll) {
        return this.c.containsAll(coll);
    }

    @Override
    public boolean addAll(Collection<? extends E> coll) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> coll) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> coll) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        this.c.forEach(action);
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Spliterator<E> spliterator() {
        return (Spliterator<E>)this.c.spliterator();
    }

    @Override
    public Stream<E> stream() {
        return (Stream<E>)this.c.stream();
    }

    @Override
    public Stream<E> parallelStream() {
        return (Stream<E>)this.c.parallelStream();
    }
}
