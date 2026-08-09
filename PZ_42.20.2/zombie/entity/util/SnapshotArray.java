// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

import java.util.Comparator;

public class SnapshotArray<T> extends Array<T> {
    private T[] snapshot;
    private T[] recycled;
    private int snapshots;

    public SnapshotArray() {
    }

    public SnapshotArray(Array<T> array) {
        super(array);
    }

    public SnapshotArray(boolean ordered, int capacity, Class<?> arrayType) {
        super(ordered, capacity, arrayType);
    }

    public SnapshotArray(boolean ordered, int capacity) {
        super(ordered, capacity);
    }

    public SnapshotArray(boolean ordered, T[] array, int startIndex, int count) {
        super(ordered, array, startIndex, count);
    }

    public SnapshotArray(Class<?> arrayType) {
        super(arrayType);
    }

    public SnapshotArray(int capacity) {
        super(capacity);
    }

    public SnapshotArray(T[] array) {
        super(array);
    }

    public T[] begin() {
        this.modified();
        this.snapshot = this.items;
        this.snapshots++;
        return this.items;
    }

    public void end() {
        this.snapshots = Math.max(0, this.snapshots - 1);
        if (this.snapshot != null) {
            if (this.snapshot != this.items && this.snapshots == 0) {
                this.recycled = this.snapshot;
                int i = 0;

                for (int n = this.recycled.length; i < n; i++) {
                    this.recycled[i] = null;
                }
            }

            this.snapshot = null;
        }
    }

    private void modified() {
        if (this.snapshot != null && this.snapshot == this.items) {
            if (this.recycled != null && this.recycled.length >= this.size) {
                System.arraycopy(this.items, 0, this.recycled, 0, this.size);
                this.items = this.recycled;
                this.recycled = null;
            } else {
                this.resize(this.items.length);
            }
        }
    }

    @Override
    public void set(int index, T value) {
        this.modified();
        super.set(index, value);
    }

    @Override
    public void insert(int index, T value) {
        this.modified();
        super.insert(index, value);
    }

    @Override
    public void insertRange(int index, int count) {
        this.modified();
        super.insertRange(index, count);
    }

    @Override
    public void swap(int first, int second) {
        this.modified();
        super.swap(first, second);
    }

    @Override
    public boolean removeValue(T value, boolean identity) {
        this.modified();
        return super.removeValue(value, identity);
    }

    @Override
    public T removeIndex(int index) {
        this.modified();
        return super.removeIndex(index);
    }

    @Override
    public void removeRange(int start, int end) {
        this.modified();
        super.removeRange(start, end);
    }

    @Override
    public boolean removeAll(Array<? extends T> array, boolean identity) {
        this.modified();
        return super.removeAll(array, identity);
    }

    @Override
    public T pop() {
        this.modified();
        return super.pop();
    }

    @Override
    public void clear() {
        this.modified();
        super.clear();
    }

    @Override
    public void sort() {
        this.modified();
        super.sort();
    }

    @Override
    public void sort(Comparator<? super T> comparator) {
        this.modified();
        super.sort(comparator);
    }

    @Override
    public void reverse() {
        this.modified();
        super.reverse();
    }

    @Override
    public void shuffle() {
        this.modified();
        super.shuffle();
    }

    @Override
    public void truncate(int newSize) {
        this.modified();
        super.truncate(newSize);
    }

    @Override
    public T[] setSize(int newSize) {
        this.modified();
        return super.setSize(newSize);
    }

    public static <T> SnapshotArray<T> with(T... array) {
        return new SnapshotArray<>(array);
    }
}
