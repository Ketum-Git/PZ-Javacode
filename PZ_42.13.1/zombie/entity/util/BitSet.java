// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.util;

import java.util.Arrays;
import zombie.UsedFromLua;

@UsedFromLua
public class BitSet {
    long[] bits = new long[]{0L};

    public BitSet() {
    }

    public BitSet(int nbits) {
        this.checkCapacity(nbits >>> 6);
    }

    public BitSet(BitSet bitsToCpy) {
        this.bits = new long[bitsToCpy.bits.length];
        System.arraycopy(bitsToCpy.bits, 0, this.bits, 0, bitsToCpy.bits.length);
    }

    public boolean get(int index) {
        int word = index >>> 6;
        return word >= this.bits.length ? false : (this.bits[word] & 1L << (index & 63)) != 0L;
    }

    public boolean getAndClear(int index) {
        int word = index >>> 6;
        if (word >= this.bits.length) {
            return false;
        } else {
            long oldBits = this.bits[word];
            this.bits[word] = this.bits[word] & ~(1L << (index & 63));
            return this.bits[word] != oldBits;
        }
    }

    public boolean getAndSet(int index) {
        int word = index >>> 6;
        this.checkCapacity(word);
        long oldBits = this.bits[word];
        this.bits[word] = this.bits[word] | 1L << (index & 63);
        return this.bits[word] == oldBits;
    }

    public void set(int index) {
        int word = index >>> 6;
        this.checkCapacity(word);
        this.bits[word] = this.bits[word] | 1L << (index & 63);
    }

    public void flip(int index) {
        int word = index >>> 6;
        this.checkCapacity(word);
        this.bits[word] = this.bits[word] ^ 1L << (index & 63);
    }

    private void checkCapacity(int len) {
        if (len >= this.bits.length) {
            long[] newBits = new long[len + 1];
            System.arraycopy(this.bits, 0, newBits, 0, this.bits.length);
            this.bits = newBits;
        }
    }

    public void clear(int index) {
        int word = index >>> 6;
        if (word < this.bits.length) {
            this.bits[word] = this.bits[word] & ~(1L << (index & 63));
        }
    }

    public void clear() {
        Arrays.fill(this.bits, 0L);
    }

    public int numBits() {
        return this.bits.length << 6;
    }

    public int length() {
        long[] bits = this.bits;

        for (int word = bits.length - 1; word >= 0; word--) {
            long bitsAtWord = bits[word];
            if (bitsAtWord != 0L) {
                for (int bit = 63; bit >= 0; bit--) {
                    if ((bitsAtWord & 1L << (bit & 63)) != 0L) {
                        return (word << 6) + bit + 1;
                    }
                }
            }
        }

        return 0;
    }

    public boolean notEmpty() {
        return !this.isEmpty();
    }

    public boolean isEmpty() {
        long[] bits = this.bits;
        int length = bits.length;

        for (int i = 0; i < length; i++) {
            if (bits[i] != 0L) {
                return false;
            }
        }

        return true;
    }

    public int nextSetBit(int fromIndex) {
        long[] bits = this.bits;
        int word = fromIndex >>> 6;
        int bitsLength = bits.length;
        if (word >= bitsLength) {
            return -1;
        } else {
            long bitsAtWord = bits[word];
            if (bitsAtWord != 0L) {
                for (int i = fromIndex & 63; i < 64; i++) {
                    if ((bitsAtWord & 1L << (i & 63)) != 0L) {
                        return (word << 6) + i;
                    }
                }
            }

            word++;

            for (; word < bitsLength; word++) {
                if (word != 0) {
                    bitsAtWord = bits[word];
                    if (bitsAtWord != 0L) {
                        for (int ix = 0; ix < 64; ix++) {
                            if ((bitsAtWord & 1L << (ix & 63)) != 0L) {
                                return (word << 6) + ix;
                            }
                        }
                    }
                }
            }

            return -1;
        }
    }

    public int nextClearBit(int fromIndex) {
        long[] bits = this.bits;
        int word = fromIndex >>> 6;
        int bitsLength = bits.length;
        if (word >= bitsLength) {
            return bits.length << 6;
        } else {
            long bitsAtWord = bits[word];

            for (int i = fromIndex & 63; i < 64; i++) {
                if ((bitsAtWord & 1L << (i & 63)) == 0L) {
                    return (word << 6) + i;
                }
            }

            word++;

            while (word < bitsLength) {
                if (word == 0) {
                    return word << 6;
                }

                bitsAtWord = bits[word];

                for (int ix = 0; ix < 64; ix++) {
                    if ((bitsAtWord & 1L << (ix & 63)) == 0L) {
                        return (word << 6) + ix;
                    }
                }

                word++;
            }

            return bits.length << 6;
        }
    }

    public void and(BitSet other) {
        int commonWords = Math.min(this.bits.length, other.bits.length);

        for (int i = 0; commonWords > i; i++) {
            this.bits[i] = this.bits[i] & other.bits[i];
        }

        if (this.bits.length > commonWords) {
            int i = commonWords;

            for (int s = this.bits.length; s > i; i++) {
                this.bits[i] = 0L;
            }
        }
    }

    public void andNot(BitSet other) {
        int i = 0;
        int j = this.bits.length;

        for (int k = other.bits.length; i < j && i < k; i++) {
            this.bits[i] = this.bits[i] & ~other.bits[i];
        }
    }

    public void or(BitSet other) {
        int commonWords = Math.min(this.bits.length, other.bits.length);

        for (int i = 0; commonWords > i; i++) {
            this.bits[i] = this.bits[i] | other.bits[i];
        }

        if (commonWords < other.bits.length) {
            this.checkCapacity(other.bits.length);
            int i = commonWords;

            for (int s = other.bits.length; s > i; i++) {
                this.bits[i] = other.bits[i];
            }
        }
    }

    public void xor(BitSet other) {
        int commonWords = Math.min(this.bits.length, other.bits.length);

        for (int i = 0; commonWords > i; i++) {
            this.bits[i] = this.bits[i] ^ other.bits[i];
        }

        if (commonWords < other.bits.length) {
            this.checkCapacity(other.bits.length);
            int i = commonWords;

            for (int s = other.bits.length; s > i; i++) {
                this.bits[i] = other.bits[i];
            }
        }
    }

    public boolean intersects(BitSet other) {
        long[] bits = this.bits;
        long[] otherBits = other.bits;

        for (int i = Math.min(bits.length, otherBits.length) - 1; i >= 0; i--) {
            if ((bits[i] & otherBits[i]) != 0L) {
                return true;
            }
        }

        return false;
    }

    public boolean containsAll(BitSet other) {
        long[] bits = this.bits;
        long[] otherBits = other.bits;
        int otherBitsLength = otherBits.length;
        int bitsLength = bits.length;

        for (int i = bitsLength; i < otherBitsLength; i++) {
            if (otherBits[i] != 0L) {
                return false;
            }
        }

        for (int ix = Math.min(bitsLength, otherBitsLength) - 1; ix >= 0; ix--) {
            if ((bits[ix] & otherBits[ix]) != otherBits[ix]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int word = this.length() >>> 6;
        int hash = 0;

        for (int i = 0; word >= i; i++) {
            hash = 127 * hash + (int)(this.bits[i] ^ this.bits[i] >>> 32);
        }

        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            BitSet other = (BitSet)obj;
            long[] otherBits = other.bits;
            int commonWords = Math.min(this.bits.length, otherBits.length);

            for (int i = 0; commonWords > i; i++) {
                if (this.bits[i] != otherBits[i]) {
                    return false;
                }
            }

            return this.bits.length == otherBits.length ? true : this.length() == other.length();
        }
    }
}
