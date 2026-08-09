// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

import java.util.Comparator;

public class Select {
    private static Select instance;
    private QuickSelect<?> quickSelect;

    public static Select instance() {
        if (instance == null) {
            instance = new Select();
        }

        return instance;
    }

    public <T> T select(T[] items, Comparator<T> comp, int kthLowest, int size) {
        int idx = this.selectIndex(items, comp, kthLowest, size);
        return items[idx];
    }

    public <T> int selectIndex(T[] items, Comparator<T> comp, int kthLowest, int size) {
        if (size < 1) {
            throw new RuntimeException("cannot select from empty array (size < 1)");
        } else if (kthLowest > size) {
            throw new RuntimeException("Kth rank is larger than size. k: " + kthLowest + ", size: " + size);
        } else {
            int idx;
            if (kthLowest == 1) {
                idx = this.fastMin(items, comp, size);
            } else if (kthLowest == size) {
                idx = this.fastMax(items, comp, size);
            } else {
                if (this.quickSelect == null) {
                    this.quickSelect = new QuickSelect();
                }

                idx = this.quickSelect.select(items, comp, kthLowest, size);
            }

            return idx;
        }
    }

    private <T> int fastMin(T[] items, Comparator<T> comp, int size) {
        int lowestIdx = 0;

        for (int i = 1; i < size; i++) {
            int comparison = comp.compare(items[i], items[lowestIdx]);
            if (comparison < 0) {
                lowestIdx = i;
            }
        }

        return lowestIdx;
    }

    private <T> int fastMax(T[] items, Comparator<T> comp, int size) {
        int highestIdx = 0;

        for (int i = 1; i < size; i++) {
            int comparison = comp.compare(items[i], items[highestIdx]);
            if (comparison > 0) {
                highestIdx = i;
            }
        }

        return highestIdx;
    }
}
