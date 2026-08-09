// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

import java.util.Comparator;

public class QuickSelect<T> {
    private T[] array;
    private Comparator<? super T> comp;

    public int select(T[] items, Comparator<T> comp, int n, int size) {
        this.array = items;
        this.comp = comp;
        return this.recursiveSelect(0, size - 1, n);
    }

    private int partition(int left, int right, int pivot) {
        T pivotValue = this.array[pivot];
        this.swap(right, pivot);
        int storage = left;

        for (int i = left; i < right; i++) {
            if (this.comp.compare(this.array[i], pivotValue) < 0) {
                this.swap(storage, i);
                storage++;
            }
        }

        this.swap(right, storage);
        return storage;
    }

    private int recursiveSelect(int left, int right, int k) {
        if (left == right) {
            return left;
        } else {
            int pivotIndex = this.medianOfThreePivot(left, right);
            int pivotNewIndex = this.partition(left, right, pivotIndex);
            int pivotDist = pivotNewIndex - left + 1;
            int result;
            if (pivotDist == k) {
                result = pivotNewIndex;
            } else if (k < pivotDist) {
                result = this.recursiveSelect(left, pivotNewIndex - 1, k);
            } else {
                result = this.recursiveSelect(pivotNewIndex + 1, right, k - pivotDist);
            }

            return result;
        }
    }

    private int medianOfThreePivot(int leftIdx, int rightIdx) {
        T left = this.array[leftIdx];
        int midIdx = (leftIdx + rightIdx) / 2;
        T mid = this.array[midIdx];
        T right = this.array[rightIdx];
        if (this.comp.compare(left, mid) > 0) {
            if (this.comp.compare(mid, right) > 0) {
                return midIdx;
            } else {
                return this.comp.compare(left, right) > 0 ? rightIdx : leftIdx;
            }
        } else if (this.comp.compare(left, right) > 0) {
            return leftIdx;
        } else {
            return this.comp.compare(mid, right) > 0 ? rightIdx : midIdx;
        }
    }

    private void swap(int left, int right) {
        T tmp = this.array[left];
        this.array[left] = this.array[right];
        this.array[right] = tmp;
    }
}
