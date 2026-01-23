// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.list;

import java.util.Iterator;
import java.util.Objects;

public final class PZEmptyIterable<T> implements Iterable<T> {
    private static final PZEmptyIterable<Object> instance = new PZEmptyIterable<>();
    private final Iterator<T> it = new Iterator<T>() {
        {
            Objects.requireNonNull(PZEmptyIterable.this);
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public T next() {
            throw new ArrayIndexOutOfBoundsException("Empty Iterator. Has no data.");
        }
    };

    private PZEmptyIterable() {
    }

    public static <E> PZEmptyIterable<E> getInstance() {
        return (PZEmptyIterable<E>)instance;
    }

    @Override
    public Iterator<T> iterator() {
        return this.it;
    }
}
