// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.hash;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;

public class PZHash {
    private static final int FNV1_32_INIT = -2128831035;
    private static final int FNV1_PRIME_32 = 16777619;
    private static final long FNV1_64_INIT = -3750763034362895579L;
    private static final long FNV1_PRIME_64 = 1099511628211L;

    public static long sha256_64(String input) {
        HashCode hashCode = Hashing.sha256().hashString(input, StandardCharsets.UTF_8);
        return hashCode.asLong();
    }

    public static int fnv_32(String text) {
        byte[] data = text.getBytes();
        return fnv_32(data, data.length);
    }

    public static int fnv_32(byte[] data) {
        return fnv_32(data, data.length);
    }

    public static int fnv_32(byte[] data, int length) {
        int hash = -2128831035;

        for (int i = 0; i < length; i++) {
            hash ^= data[i] & 255;
            hash *= 16777619;
        }

        return hash;
    }

    public static int fnv_32_init() {
        return -2128831035;
    }

    public static int fnv_32_hash(int hash, int data) {
        hash ^= data;
        return hash * 16777619;
    }

    public static long fnv_64(String text) {
        byte[] data = text.getBytes();
        return fnv_64(data, data.length);
    }

    public static long fnv_64(byte[] data) {
        return fnv_64(data, data.length);
    }

    public static long fnv_64(byte[] data, int length) {
        long hash = -3750763034362895579L;

        for (int i = 0; i < length; i++) {
            hash ^= data[i] & 255;
            hash *= 1099511628211L;
        }

        return hash;
    }

    public static int murmur_32(String text) {
        byte[] bytes = text.getBytes();
        return murmur_32(bytes, bytes.length);
    }

    public static int murmur_32(byte[] data, int length) {
        return murmur_32(data, length, 0);
    }

    public static int murmur_32(byte[] data, int length, int seed) {
        int m = 1540483477;
        int r = 24;
        int h = seed ^ length;
        int len_4 = length >> 2;

        for (int i = 0; i < len_4; i++) {
            int i_4 = i << 2;
            int k = data[i_4 + 3];
            k <<= 8;
            k |= data[i_4 + 2] & 255;
            k <<= 8;
            k |= data[i_4 + 1] & 255;
            k <<= 8;
            k |= data[i_4 + 0] & 255;
            k *= 1540483477;
            k ^= k >>> 24;
            k *= 1540483477;
            h *= 1540483477;
            h ^= k;
        }

        int len_m = len_4 << 2;
        int left = length - len_m;
        if (left != 0) {
            if (left >= 3) {
                h ^= data[length - 3] << 16;
            }

            if (left >= 2) {
                h ^= data[length - 2] << 8;
            }

            if (left >= 1) {
                h ^= data[length - 1];
            }

            h *= 1540483477;
        }

        h ^= h >>> 13;
        h *= 1540483477;
        return h ^ h >>> 15;
    }

    public static long murmur_64(String text) {
        byte[] bytes = text.getBytes();
        return murmur_64(bytes, bytes.length);
    }

    public static long murmur_64(byte[] data, int length) {
        return murmur_64(data, length, -512093083);
    }

    public static long murmur_64(byte[] data, int length, int seed) {
        long m = -4132994306676758123L;
        int r = 47;
        long h = seed & 4294967295L ^ length * -4132994306676758123L;
        int length8 = length / 8;

        for (int i = 0; i < length8; i++) {
            int i8 = i * 8;
            long k = (data[i8 + 0] & 255L)
                + ((data[i8 + 1] & 255L) << 8)
                + ((data[i8 + 2] & 255L) << 16)
                + ((data[i8 + 3] & 255L) << 24)
                + ((data[i8 + 4] & 255L) << 32)
                + ((data[i8 + 5] & 255L) << 40)
                + ((data[i8 + 6] & 255L) << 48)
                + ((data[i8 + 7] & 255L) << 56);
            k *= -4132994306676758123L;
            k ^= k >>> 47;
            k *= -4132994306676758123L;
            h ^= k;
            h *= -4132994306676758123L;
        }

        switch (length % 8) {
            case 7:
                h ^= (long)(data[(length & -8) + 6] & 255) << 48;
            case 6:
                h ^= (long)(data[(length & -8) + 5] & 255) << 40;
            case 5:
                h ^= (long)(data[(length & -8) + 4] & 255) << 32;
            case 4:
                h ^= (long)(data[(length & -8) + 3] & 255) << 24;
            case 3:
                h ^= (long)(data[(length & -8) + 2] & 255) << 16;
            case 2:
                h ^= (long)(data[(length & -8) + 1] & 255) << 8;
            case 1:
                h ^= data[length & -8] & 255;
                h *= -4132994306676758123L;
            default:
                h ^= h >>> 47;
                h *= -4132994306676758123L;
                return h ^ h >>> 47;
        }
    }
}
