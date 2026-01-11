// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

import java.util.Iterator;

public interface Predicate<T> {
    boolean evaluate(T var1);

    public static class PredicateIterable<T> implements Iterable<T> {
        public Iterable<T> iterable;
        public Predicate<T> predicate;
        public Predicate.PredicateIterator<T> iterator;

        public PredicateIterable(Iterable<T> iterable, Predicate<T> predicate) {
            this.set(iterable, predicate);
        }

        public void set(Iterable<T> iterable, Predicate<T> predicate) {
            this.iterable = iterable;
            this.predicate = predicate;
        }

        @Override
        public Iterator<T> iterator() {
            if (Collections.allocateIterators) {
                return new Predicate.PredicateIterator<>(this.iterable.iterator(), this.predicate);
            } else {
                if (this.iterator == null) {
                    this.iterator = new Predicate.PredicateIterator<>(this.iterable.iterator(), this.predicate);
                } else {
                    this.iterator.set(this.iterable.iterator(), this.predicate);
                }

                return this.iterator;
            }
        }
    }

    public static class PredicateIterator<T> implements Iterator<T> {
        public Iterator<T> iterator;
        public Predicate<T> predicate;
        public boolean end;
        public boolean peeked;
        public T next;

        public PredicateIterator(Iterable<T> iterable, Predicate<T> predicate) {
            this(iterable.iterator(), predicate);
        }

        public PredicateIterator(Iterator<T> iterator, Predicate<T> predicate) {
            this.set(iterator, predicate);
        }

        public void set(Iterable<T> iterable, Predicate<T> predicate) {
            this.set(iterable.iterator(), predicate);
        }

        public void set(Iterator<T> iterator, Predicate<T> predicate) {
            this.iterator = iterator;
            this.predicate = predicate;
            this.end = this.peeked = false;
            this.next = null;
        }

        @Override
        public boolean hasNext() {
            if (this.end) {
                return false;
            } else if (this.next != null) {
                return true;
            } else {
                this.peeked = true;

                while (this.iterator.hasNext()) {
                    T n = this.iterator.next();
                    if (this.predicate.evaluate(n)) {
                        this.next = n;
                        return true;
                    }
                }

                this.end = true;
                return false;
            }
        }

        @Override
        public T next() {
            if (this.next == null && !this.hasNext()) {
                return null;
            } else {
                T result = this.next;
                this.next = null;
                this.peeked = false;
                return result;
            }
        }

        @Override
        public void remove() {
            if (this.peeked) {
                throw new RuntimeException("Cannot remove between a call to hasNext() and next().");
            } else {
                this.iterator.remove();
            }
        }
    }
}
