// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

import java.util.Arrays;
import java.util.Comparator;

public class TimSort<T> {
    private static final int MIN_MERGE = 32;
    private T[] a;
    private Comparator<? super T> c;
    private static final int MIN_GALLOP = 7;
    private int minGallop = 7;
    private static final int INITIAL_TMP_STORAGE_LENGTH = 256;
    private T[] tmp;
    private int tmpCount;
    private int stackSize;
    private final int[] runBase;
    private final int[] runLen;
    private static final boolean DEBUG = false;

    public TimSort() {
        this.tmp = (T[])(new Object[256]);
        this.runBase = new int[40];
        this.runLen = new int[40];
    }

    public void doSort(T[] a, Comparator<T> c, int lo, int hi) {
        this.stackSize = 0;
        rangeCheck(a.length, lo, hi);
        int nRemaining = hi - lo;
        if (nRemaining >= 2) {
            if (nRemaining < 32) {
                int initRunLen = countRunAndMakeAscending(a, lo, hi, c);
                binarySort(a, lo, hi, lo + initRunLen, c);
            } else {
                this.a = a;
                this.c = c;
                this.tmpCount = 0;
                int minRun = minRunLength(nRemaining);

                do {
                    int runLen = countRunAndMakeAscending(a, lo, hi, c);
                    if (runLen < minRun) {
                        int force = nRemaining <= minRun ? nRemaining : minRun;
                        binarySort(a, lo, lo + force, lo + runLen, c);
                        runLen = force;
                    }

                    this.pushRun(lo, runLen);
                    this.mergeCollapse();
                    lo += runLen;
                    nRemaining -= runLen;
                } while (nRemaining != 0);

                this.mergeForceCollapse();
                this.a = null;
                this.c = null;
                T[] tmp = this.tmp;
                int i = 0;

                for (int n = this.tmpCount; i < n; i++) {
                    tmp[i] = null;
                }
            }
        }
    }

    private TimSort(T[] a, Comparator<? super T> c) {
        this.a = a;
        this.c = c;
        int len = a.length;
        T[] newArray = (T[])(new Object[len < 512 ? len >>> 1 : 256]);
        this.tmp = newArray;
        int stackLen = len < 120 ? 5 : (len < 1542 ? 10 : (len < 119151 ? 19 : 40));
        this.runBase = new int[stackLen];
        this.runLen = new int[stackLen];
    }

    static <T> void sort(T[] a, Comparator<? super T> c) {
        sort(a, 0, a.length, c);
    }

    static <T> void sort(T[] a, int lo, int hi, Comparator<? super T> c) {
        if (c == null) {
            Arrays.sort(a, lo, hi);
        } else {
            rangeCheck(a.length, lo, hi);
            int nRemaining = hi - lo;
            if (nRemaining >= 2) {
                if (nRemaining < 32) {
                    int initRunLen = countRunAndMakeAscending(a, lo, hi, c);
                    binarySort(a, lo, hi, lo + initRunLen, c);
                } else {
                    TimSort<T> ts = new TimSort<>(a, c);
                    int minRun = minRunLength(nRemaining);

                    do {
                        int runLen = countRunAndMakeAscending(a, lo, hi, c);
                        if (runLen < minRun) {
                            int force = nRemaining <= minRun ? nRemaining : minRun;
                            binarySort(a, lo, lo + force, lo + runLen, c);
                            runLen = force;
                        }

                        ts.pushRun(lo, runLen);
                        ts.mergeCollapse();
                        lo += runLen;
                        nRemaining -= runLen;
                    } while (nRemaining != 0);

                    ts.mergeForceCollapse();
                }
            }
        }
    }

    private static <T> void binarySort(T[] a, int lo, int hi, int start, Comparator<? super T> c) {
        if (start == lo) {
            start++;
        }

        while (start < hi) {
            T pivot = a[start];
            int left = lo;
            int right = start;

            while (left < right) {
                int mid = left + right >>> 1;
                if (c.compare(pivot, a[mid]) < 0) {
                    right = mid;
                } else {
                    left = mid + 1;
                }
            }

            int n = start - left;
            switch (n) {
                case 2:
                    a[left + 2] = a[left + 1];
                case 1:
                    a[left + 1] = a[left];
                    break;
                default:
                    System.arraycopy(a, left, a, left + 1, n);
            }

            a[left] = pivot;
            start++;
        }
    }

    private static <T> int countRunAndMakeAscending(T[] a, int lo, int hi, Comparator<? super T> c) {
        int runHi = lo + 1;
        if (runHi == hi) {
            return 1;
        } else {
            if (c.compare(a[runHi++], a[lo]) >= 0) {
                while (runHi < hi && c.compare(a[runHi], a[runHi - 1]) >= 0) {
                    runHi++;
                }
            } else {
                while (runHi < hi && c.compare(a[runHi], a[runHi - 1]) < 0) {
                    runHi++;
                }

                reverseRange(a, lo, runHi);
            }

            return runHi - lo;
        }
    }

    private static void reverseRange(Object[] a, int lo, int hi) {
        hi--;

        while (lo < hi) {
            Object t = a[lo];
            a[lo++] = a[hi];
            a[hi--] = t;
        }
    }

    private static int minRunLength(int n) {
        int r = 0;

        while (n >= 32) {
            r |= n & 1;
            n >>= 1;
        }

        return n + r;
    }

    private void pushRun(int runBase, int runLen) {
        this.runBase[this.stackSize] = runBase;
        this.runLen[this.stackSize] = runLen;
        this.stackSize++;
    }

    private void mergeCollapse() {
        while (true) {
            int n;
            label36: {
                if (this.stackSize > 1) {
                    n = this.stackSize - 2;
                    if (n >= 1 && this.runLen[n - 1] <= this.runLen[n] + this.runLen[n + 1]
                        || n >= 2 && this.runLen[n - 2] <= this.runLen[n] + this.runLen[n - 1]) {
                        if (this.runLen[n - 1] < this.runLen[n + 1]) {
                            n--;
                        }
                        break label36;
                    }

                    if (this.runLen[n] <= this.runLen[n + 1]) {
                        break label36;
                    }
                }

                return;
            }

            this.mergeAt(n);
        }
    }

    private void mergeForceCollapse() {
        while (this.stackSize > 1) {
            int n = this.stackSize - 2;
            if (n > 0 && this.runLen[n - 1] < this.runLen[n + 1]) {
                n--;
            }

            this.mergeAt(n);
        }
    }

    private void mergeAt(int i) {
        int base1 = this.runBase[i];
        int len1 = this.runLen[i];
        int base2 = this.runBase[i + 1];
        int len2 = this.runLen[i + 1];
        this.runLen[i] = len1 + len2;
        if (i == this.stackSize - 3) {
            this.runBase[i + 1] = this.runBase[i + 2];
            this.runLen[i + 1] = this.runLen[i + 2];
        }

        this.stackSize--;
        int k = gallopRight(this.a[base2], this.a, base1, len1, 0, this.c);
        base1 += k;
        len1 -= k;
        if (len1 != 0) {
            len2 = gallopLeft(this.a[base1 + len1 - 1], this.a, base2, len2, len2 - 1, this.c);
            if (len2 != 0) {
                if (len1 <= len2) {
                    this.mergeLo(base1, len1, base2, len2);
                } else {
                    this.mergeHi(base1, len1, base2, len2);
                }
            }
        }
    }

    private static <T> int gallopLeft(T key, T[] a, int base, int len, int hint, Comparator<? super T> c) {
        int lastOfs = 0;
        int ofs = 1;
        if (c.compare(key, a[base + hint]) > 0) {
            int maxOfs = len - hint;

            while (ofs < maxOfs && c.compare(key, a[base + hint + ofs]) > 0) {
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0) {
                    ofs = maxOfs;
                }
            }

            if (ofs > maxOfs) {
                ofs = maxOfs;
            }

            lastOfs += hint;
            ofs += hint;
        } else {
            int maxOfs = hint + 1;

            while (ofs < maxOfs && c.compare(key, a[base + hint - ofs]) <= 0) {
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0) {
                    ofs = maxOfs;
                }
            }

            if (ofs > maxOfs) {
                ofs = maxOfs;
            }

            lastOfs = hint - ofs;
            ofs = hint - lastOfs;
        }

        lastOfs++;

        while (lastOfs < ofs) {
            int m = lastOfs + (ofs - lastOfs >>> 1);
            if (c.compare(key, a[base + m]) > 0) {
                lastOfs = m + 1;
            } else {
                ofs = m;
            }
        }

        return ofs;
    }

    private static <T> int gallopRight(T key, T[] a, int base, int len, int hint, Comparator<? super T> c) {
        int ofs = 1;
        int lastOfs = 0;
        if (c.compare(key, a[base + hint]) < 0) {
            int maxOfs = hint + 1;

            while (ofs < maxOfs && c.compare(key, a[base + hint - ofs]) < 0) {
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0) {
                    ofs = maxOfs;
                }
            }

            if (ofs > maxOfs) {
                ofs = maxOfs;
            }

            lastOfs = hint - ofs;
            ofs = hint - lastOfs;
        } else {
            int maxOfs = len - hint;

            while (ofs < maxOfs && c.compare(key, a[base + hint + ofs]) >= 0) {
                lastOfs = ofs;
                ofs = (ofs << 1) + 1;
                if (ofs <= 0) {
                    ofs = maxOfs;
                }
            }

            if (ofs > maxOfs) {
                ofs = maxOfs;
            }

            lastOfs += hint;
            ofs += hint;
        }

        lastOfs++;

        while (lastOfs < ofs) {
            int m = lastOfs + (ofs - lastOfs >>> 1);
            if (c.compare(key, a[base + m]) < 0) {
                ofs = m;
            } else {
                lastOfs = m + 1;
            }
        }

        return ofs;
    }

    private void mergeLo(int base1, int len1, int base2, int len2) {
        T[] a = this.a;
        T[] tmp = this.ensureCapacity(len1);
        System.arraycopy(a, base1, tmp, 0, len1);
        int cursor1 = 0;
        int dest = base1 + 1;
        int cursor2 = base2 + 1;
        a[base1] = a[base2];
        if (--len2 == 0) {
            System.arraycopy(tmp, cursor1, a, dest, len1);
        } else if (len1 == 1) {
            System.arraycopy(a, cursor2, a, dest, len2);
            a[dest + len2] = tmp[cursor1];
        } else {
            Comparator<? super T> c = this.c;
            int minGallop = this.minGallop;

            label81:
            while (true) {
                int count1 = 0;
                int count2 = 0;

                do {
                    if (c.compare(a[cursor2], tmp[cursor1]) < 0) {
                        a[dest++] = a[cursor2++];
                        count2++;
                        count1 = 0;
                        if (--len2 == 0) {
                            break label81;
                        }
                    } else {
                        a[dest++] = tmp[cursor1++];
                        count1++;
                        count2 = 0;
                        if (--len1 == 1) {
                            break label81;
                        }
                    }
                } while ((count1 | count2) < minGallop);

                do {
                    count1 = gallopRight(a[cursor2], tmp, cursor1, len1, 0, c);
                    if (count1 != 0) {
                        System.arraycopy(tmp, cursor1, a, dest, count1);
                        dest += count1;
                        cursor1 += count1;
                        len1 -= count1;
                        if (len1 <= 1) {
                            break label81;
                        }
                    }

                    a[dest++] = a[cursor2++];
                    if (--len2 == 0) {
                        break label81;
                    }

                    count2 = gallopLeft(tmp[cursor1], a, cursor2, len2, 0, c);
                    if (count2 != 0) {
                        System.arraycopy(a, cursor2, a, dest, count2);
                        dest += count2;
                        cursor2 += count2;
                        len2 -= count2;
                        if (len2 == 0) {
                            break label81;
                        }
                    }

                    a[dest++] = tmp[cursor1++];
                    if (--len1 == 1) {
                        break label81;
                    }

                    minGallop--;
                } while (count1 >= 7 | count2 >= 7);

                if (minGallop < 0) {
                    minGallop = 0;
                }

                minGallop += 2;
            }

            this.minGallop = minGallop < 1 ? 1 : minGallop;
            if (len1 == 1) {
                System.arraycopy(a, cursor2, a, dest, len2);
                a[dest + len2] = tmp[cursor1];
            } else {
                if (len1 == 0) {
                    throw new IllegalArgumentException("Comparison method violates its general contract!");
                }

                System.arraycopy(tmp, cursor1, a, dest, len1);
            }
        }
    }

    private void mergeHi(int base1, int len1, int base2, int len2) {
        T[] a = this.a;
        T[] tmp = this.ensureCapacity(len2);
        System.arraycopy(a, base2, tmp, 0, len2);
        int cursor1 = base1 + len1 - 1;
        int cursor2 = len2 - 1;
        int dest = base2 + len2 - 1;
        a[dest--] = a[cursor1--];
        if (--len1 == 0) {
            System.arraycopy(tmp, 0, a, dest - (len2 - 1), len2);
        } else if (len2 == 1) {
            dest -= len1;
            cursor1 -= len1;
            System.arraycopy(a, cursor1 + 1, a, dest + 1, len1);
            a[dest] = tmp[cursor2];
        } else {
            Comparator<? super T> c = this.c;
            int minGallop = this.minGallop;

            label81:
            while (true) {
                int count1 = 0;
                int count2 = 0;

                do {
                    if (c.compare(tmp[cursor2], a[cursor1]) < 0) {
                        a[dest--] = a[cursor1--];
                        count1++;
                        count2 = 0;
                        if (--len1 == 0) {
                            break label81;
                        }
                    } else {
                        a[dest--] = tmp[cursor2--];
                        count2++;
                        count1 = 0;
                        if (--len2 == 1) {
                            break label81;
                        }
                    }
                } while ((count1 | count2) < minGallop);

                do {
                    count1 = len1 - gallopRight(tmp[cursor2], a, base1, len1, len1 - 1, c);
                    if (count1 != 0) {
                        dest -= count1;
                        cursor1 -= count1;
                        len1 -= count1;
                        System.arraycopy(a, cursor1 + 1, a, dest + 1, count1);
                        if (len1 == 0) {
                            break label81;
                        }
                    }

                    a[dest--] = tmp[cursor2--];
                    if (--len2 == 1) {
                        break label81;
                    }

                    count2 = len2 - gallopLeft(a[cursor1], tmp, 0, len2, len2 - 1, c);
                    if (count2 != 0) {
                        dest -= count2;
                        cursor2 -= count2;
                        len2 -= count2;
                        System.arraycopy(tmp, cursor2 + 1, a, dest + 1, count2);
                        if (len2 <= 1) {
                            break label81;
                        }
                    }

                    a[dest--] = a[cursor1--];
                    if (--len1 == 0) {
                        break label81;
                    }

                    minGallop--;
                } while (count1 >= 7 | count2 >= 7);

                if (minGallop < 0) {
                    minGallop = 0;
                }

                minGallop += 2;
            }

            this.minGallop = minGallop < 1 ? 1 : minGallop;
            if (len2 == 1) {
                dest -= len1;
                cursor1 -= len1;
                System.arraycopy(a, cursor1 + 1, a, dest + 1, len1);
                a[dest] = tmp[cursor2];
            } else {
                if (len2 == 0) {
                    throw new IllegalArgumentException("Comparison method violates its general contract!");
                }

                System.arraycopy(tmp, 0, a, dest - (len2 - 1), len2);
            }
        }
    }

    private T[] ensureCapacity(int minCapacity) {
        this.tmpCount = Math.max(this.tmpCount, minCapacity);
        if (this.tmp.length < minCapacity) {
            int newSize = minCapacity | minCapacity >> 1;
            newSize |= newSize >> 2;
            newSize |= newSize >> 4;
            newSize |= newSize >> 8;
            newSize |= newSize >> 16;
            if (++newSize < 0) {
                newSize = minCapacity;
            } else {
                newSize = Math.min(newSize, this.a.length >>> 1);
            }

            T[] newArray = (T[])(new Object[newSize]);
            this.tmp = newArray;
        }

        return this.tmp;
    }

    private static void rangeCheck(int arrayLen, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        } else if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        } else if (toIndex > arrayLen) {
            throw new ArrayIndexOutOfBoundsException(toIndex);
        }
    }
}
