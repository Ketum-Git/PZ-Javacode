// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.fileSystem;

public final class FileOpenMode {
    public static final int NONE = 0;
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int OPEN = 4;
    public static final int CREATE = 8;
    public static final int STREAM = 16;
    public static final int CREATE_AND_WRITE = 10;
    public static final int OPEN_AND_READ = 5;

    public static String toStringMode(int bits) {
        StringBuilder sb = new StringBuilder();
        if ((bits & 1) != 0) {
            sb.append('r');
        }

        if ((bits & 2) != 0) {
            sb.append('w');
        }

        return sb.toString();
    }
}
