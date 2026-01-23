// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.utils;

import zombie.core.Core;
import zombie.core.math.PZMath;

public class Bits {
    public static final boolean ENABLED = true;
    public static final int BIT_0 = 0;
    public static final int BIT_1 = 1;
    public static final int BIT_2 = 2;
    public static final int BIT_3 = 4;
    public static final int BIT_4 = 8;
    public static final int BIT_5 = 16;
    public static final int BIT_6 = 32;
    public static final int BIT_7 = 64;
    public static final int BIT_BYTE_MAX = 64;
    public static final int BIT_8 = 128;
    public static final int BIT_9 = 256;
    public static final int BIT_10 = 512;
    public static final int BIT_11 = 1024;
    public static final int BIT_12 = 2048;
    public static final int BIT_13 = 4096;
    public static final int BIT_14 = 8192;
    public static final int BIT_15 = 16384;
    public static final int BIT_SHORT_MAX = 16384;
    public static final int BIT_16 = 32768;
    public static final int BIT_17 = 65536;
    public static final int BIT_18 = 131072;
    public static final int BIT_19 = 262144;
    public static final int BIT_20 = 524288;
    public static final int BIT_21 = 1048576;
    public static final int BIT_22 = 2097152;
    public static final int BIT_23 = 4194304;
    public static final int BIT_24 = 8388608;
    public static final int BIT_25 = 16777216;
    public static final int BIT_26 = 33554432;
    public static final int BIT_27 = 67108864;
    public static final int BIT_28 = 134217728;
    public static final int BIT_29 = 268435456;
    public static final int BIT_30 = 536870912;
    public static final int BIT_31 = 1073741824;
    public static final int BIT_INT_MAX = 1073741824;
    public static final long BIT_32 = 2147483648L;
    public static final long BIT_33 = 4294967296L;
    public static final long BIT_34 = 8589934592L;
    public static final long BIT_35 = 17179869184L;
    public static final long BIT_36 = 34359738368L;
    public static final long BIT_37 = 68719476736L;
    public static final long BIT_38 = 137438953472L;
    public static final long BIT_39 = 274877906944L;
    public static final long BIT_40 = 549755813888L;
    public static final long BIT_41 = 1099511627776L;
    public static final long BIT_42 = 2199023255552L;
    public static final long BIT_43 = 4398046511104L;
    public static final long BIT_44 = 8796093022208L;
    public static final long BIT_45 = 17592186044416L;
    public static final long BIT_46 = 35184372088832L;
    public static final long BIT_47 = 70368744177664L;
    public static final long BIT_48 = 140737488355328L;
    public static final long BIT_49 = 281474976710656L;
    public static final long BIT_50 = 562949953421312L;
    public static final long BIT_51 = 1125899906842624L;
    public static final long BIT_52 = 2251799813685248L;
    public static final long BIT_53 = 4503599627370496L;
    public static final long BIT_54 = 9007199254740992L;
    public static final long BIT_55 = 18014398509481984L;
    public static final long BIT_56 = 36028797018963968L;
    public static final long BIT_57 = 72057594037927936L;
    public static final long BIT_58 = 144115188075855872L;
    public static final long BIT_59 = 288230376151711744L;
    public static final long BIT_60 = 576460752303423488L;
    public static final long BIT_61 = 1152921504606846976L;
    public static final long BIT_62 = 2305843009213693952L;
    public static final long BIT_63 = 4611686018427387904L;
    public static final long BIT_LONG_MAX = 4611686018427387904L;
    private static final StringBuilder sb = new StringBuilder();

    public static byte packFloatUnitToByte(float f) {
        if (f < 0.0F || f > 1.0F) {
            if (Core.debug) {
                throw new RuntimeException("UtilsIO Cannot pack float units out of the range 0.0 to 1.0");
            }

            f = PZMath.clamp(f, 0.0F, 1.0F);
        }

        return (byte)(f * 255.0F + -128.0F);
    }

    public static float unpackByteToFloatUnit(byte b) {
        return (b - -128) / 255.0F;
    }

    public static byte addFlags(byte value, int flags) {
        if (flags >= 0 && flags <= 64) {
            byte var2;
            return var2 = (byte)(value | flags);
        } else {
            throw new RuntimeException("Cannot add flags, exceeding byte bounds or negative number flags. (" + flags + ")");
        }
    }

    public static byte addFlags(byte value, long flags) {
        if (flags >= 0L && flags <= 64L) {
            return (byte)(value | flags);
        } else {
            throw new RuntimeException("Cannot add flags, exceeding byte bounds or negative number flags. (" + flags + ")");
        }
    }

    public static short addFlags(short value, int flags) {
        if (flags >= 0 && flags <= 16384) {
            short var2;
            return var2 = (short)(value | flags);
        } else {
            throw new RuntimeException("Cannot add flags, exceeding short bounds or negative number flags. (" + flags + ")");
        }
    }

    public static short addFlags(short value, long flags) {
        if (flags >= 0L && flags <= 16384L) {
            return (short)(value | flags);
        } else {
            throw new RuntimeException("Cannot add flags, exceeding short bounds or negative number flags. (" + flags + ")");
        }
    }

    public static int addFlags(int value, int flags) {
        if (flags >= 0 && flags <= 1073741824) {
            int var2;
            return var2 = value | flags;
        } else {
            throw new RuntimeException("Cannot add flags, exceeding short bounds or negative number flags. (" + flags + ")");
        }
    }

    public static int addFlags(int value, long flags) {
        if (flags >= 0L && flags <= 1073741824L) {
            return (int)(value | flags);
        } else {
            throw new RuntimeException("Cannot add flags, exceeding integer bounds or negative number flags. (" + flags + ")");
        }
    }

    public static long addFlags(long value, int flags) {
        if (flags >= 0 && flags <= 4611686018427387904L) {
            long var3;
            return var3 = value | flags;
        } else {
            throw new RuntimeException("Cannot add flags, exceeding long bounds or negative number flags. (" + flags + ")");
        }
    }

    public static long addFlags(long value, long flags) {
        if (flags >= 0L && flags <= 4611686018427387904L) {
            long var4;
            return var4 = value | flags;
        } else {
            throw new RuntimeException("Cannot add flags, exceeding long bounds or negative number flags. (" + flags + ")");
        }
    }

    public static byte removeFlags(byte value, int flags) {
        if (flags >= 0 && flags <= 64) {
            byte var2;
            return var2 = (byte)(value & ~flags);
        } else {
            throw new RuntimeException("Cannot remove flags, exceeding byte bounds or negative number flags. (" + flags + ")");
        }
    }

    public static byte removeFlags(byte value, long flags) {
        if (flags >= 0L && flags <= 64L) {
            return (byte)(value & ~flags);
        } else {
            throw new RuntimeException("Cannot remove flags, exceeding byte bounds or negative number flags. (" + flags + ")");
        }
    }

    public static short removeFlags(short value, int flags) {
        if (flags >= 0 && flags <= 16384) {
            short var2;
            return var2 = (short)(value & ~flags);
        } else {
            throw new RuntimeException("Cannot remove flags, exceeding short bounds or negative number flags. (" + flags + ")");
        }
    }

    public static short removeFlags(short value, long flags) {
        if (flags >= 0L && flags <= 16384L) {
            return (short)(value & ~flags);
        } else {
            throw new RuntimeException("Cannot remove flags, exceeding short bounds or negative number flags. (" + flags + ")");
        }
    }

    public static int removeFlags(int value, int flags) {
        if (flags >= 0 && flags <= 1073741824) {
            int var2;
            return var2 = value & ~flags;
        } else {
            throw new RuntimeException("Cannot remove flags, exceeding short bounds or negative number flags. (" + flags + ")");
        }
    }

    public static int removeFlags(int value, long flags) {
        if (flags >= 0L && flags <= 1073741824L) {
            return (int)(value & ~flags);
        } else {
            throw new RuntimeException("Cannot remove flags, exceeding integer bounds or negative number flags. (" + flags + ")");
        }
    }

    public static long removeFlags(long value, int flags) {
        if (flags >= 0 && flags <= 4611686018427387904L) {
            long var3;
            return var3 = value & ~flags;
        } else {
            throw new RuntimeException("Cannot remove flags, exceeding long bounds or negative number flags. (" + flags + ")");
        }
    }

    public static long removeFlags(long value, long flags) {
        if (flags >= 0L && flags <= 4611686018427387904L) {
            long var4;
            return var4 = value & ~flags;
        } else {
            throw new RuntimeException("Cannot remove flags, exceeding long bounds or negative number flags. (" + flags + ")");
        }
    }

    public static boolean hasFlags(byte value, int flags) {
        return checkFlags(value, flags, 64, Bits.CompareOption.ContainsAll);
    }

    public static boolean hasFlags(byte value, long flags) {
        return checkFlags((long)value, flags, 64L, Bits.CompareOption.ContainsAll);
    }

    public static boolean hasEitherFlags(byte value, int flags) {
        return checkFlags(value, flags, 64, Bits.CompareOption.HasEither);
    }

    public static boolean hasEitherFlags(byte value, long flags) {
        return checkFlags((long)value, flags, 64L, Bits.CompareOption.HasEither);
    }

    public static boolean notHasFlags(byte value, int flags) {
        return checkFlags(value, flags, 64, Bits.CompareOption.NotHas);
    }

    public static boolean notHasFlags(byte value, long flags) {
        return checkFlags((long)value, flags, 64L, Bits.CompareOption.NotHas);
    }

    public static boolean hasFlags(short value, int flags) {
        return checkFlags(value, flags, 16384, Bits.CompareOption.ContainsAll);
    }

    public static boolean hasFlags(short value, long flags) {
        return checkFlags((long)value, flags, 16384L, Bits.CompareOption.ContainsAll);
    }

    public static boolean hasEitherFlags(short value, int flags) {
        return checkFlags(value, flags, 16384, Bits.CompareOption.HasEither);
    }

    public static boolean hasEitherFlags(short value, long flags) {
        return checkFlags((long)value, flags, 16384L, Bits.CompareOption.HasEither);
    }

    public static boolean notHasFlags(short value, int flags) {
        return checkFlags(value, flags, 16384, Bits.CompareOption.NotHas);
    }

    public static boolean notHasFlags(short value, long flags) {
        return checkFlags((long)value, flags, 16384L, Bits.CompareOption.NotHas);
    }

    public static boolean hasFlags(int value, int flags) {
        return checkFlags(value, flags, 1073741824, Bits.CompareOption.ContainsAll);
    }

    public static boolean hasFlags(int value, long flags) {
        return checkFlags((long)value, flags, 1073741824L, Bits.CompareOption.ContainsAll);
    }

    public static boolean hasEitherFlags(int value, int flags) {
        return checkFlags(value, flags, 1073741824, Bits.CompareOption.HasEither);
    }

    public static boolean hasEitherFlags(int value, long flags) {
        return checkFlags((long)value, flags, 1073741824L, Bits.CompareOption.HasEither);
    }

    public static boolean notHasFlags(int value, int flags) {
        return checkFlags(value, flags, 1073741824, Bits.CompareOption.NotHas);
    }

    public static boolean notHasFlags(int value, long flags) {
        return checkFlags((long)value, flags, 1073741824L, Bits.CompareOption.NotHas);
    }

    public static boolean hasFlags(long value, int flags) {
        return checkFlags(value, (long)flags, 4611686018427387904L, Bits.CompareOption.ContainsAll);
    }

    public static boolean hasFlags(long value, long flags) {
        return checkFlags(value, flags, 4611686018427387904L, Bits.CompareOption.ContainsAll);
    }

    public static boolean hasEitherFlags(long value, int flags) {
        return checkFlags(value, (long)flags, 4611686018427387904L, Bits.CompareOption.HasEither);
    }

    public static boolean hasEitherFlags(long value, long flags) {
        return checkFlags(value, flags, 4611686018427387904L, Bits.CompareOption.HasEither);
    }

    public static boolean notHasFlags(long value, int flags) {
        return checkFlags(value, (long)flags, 4611686018427387904L, Bits.CompareOption.NotHas);
    }

    public static boolean notHasFlags(long value, long flags) {
        return checkFlags(value, flags, 4611686018427387904L, Bits.CompareOption.NotHas);
    }

    public static boolean checkFlags(int value, int flags, int limit, Bits.CompareOption option) {
        if (flags < 0 || flags > limit) {
            throw new RuntimeException("Cannot check for flags, exceeding byte bounds or negative number flags. (" + flags + ")");
        } else if (option == Bits.CompareOption.ContainsAll) {
            return (value & flags) == flags;
        } else if (option == Bits.CompareOption.HasEither) {
            return (value & flags) != 0;
        } else if (option == Bits.CompareOption.NotHas) {
            return (value & flags) == 0;
        } else {
            throw new RuntimeException("No valid compare option.");
        }
    }

    public static boolean checkFlags(long value, long flags, long limit, Bits.CompareOption option) {
        if (flags < 0L || flags > limit) {
            throw new RuntimeException("Cannot check for flags, exceeding byte bounds or negative number flags. (" + flags + ")");
        } else if (option == Bits.CompareOption.ContainsAll) {
            return (value & flags) == flags;
        } else if (option == Bits.CompareOption.HasEither) {
            return (value & flags) != 0L;
        } else if (option == Bits.CompareOption.NotHas) {
            return (value & flags) == 0L;
        } else {
            throw new RuntimeException("No valid compare option.");
        }
    }

    public static int getLen(byte b) {
        return 1;
    }

    public static int getLen(short s) {
        return 2;
    }

    public static int getLen(int i) {
        return 4;
    }

    public static int getLen(long l) {
        return 8;
    }

    private static void clearStringBuilder() {
        if (!sb.isEmpty()) {
            sb.delete(0, sb.length());
        }
    }

    public static String getBitsString(byte bits) {
        return getBitsString(bits, 8);
    }

    public static String getBitsString(short bits) {
        return getBitsString(bits, 16);
    }

    public static String getBitsString(int bits) {
        return getBitsString(bits, 32);
    }

    public static String getBitsString(long bits) {
        return getBitsString(bits, 64);
    }

    private static String getBitsString(long bits, int len) {
        clearStringBuilder();
        if (bits != 0L) {
            sb.append("Bits(" + (len - 1) + "): ");
            long bitval = 1L;

            for (int i = 1; i < len; i++) {
                sb.append("[" + i + "]");
                if ((bits & bitval) == bitval) {
                    sb.append("1");
                } else {
                    sb.append("0");
                }

                if (i < len - 1) {
                    sb.append(" ");
                }

                bitval *= 2L;
            }
        } else {
            sb.append("No bits saved, 0x0.");
        }

        return sb.toString();
    }

    public static enum CompareOption {
        ContainsAll,
        HasEither,
        NotHas;
    }
}
