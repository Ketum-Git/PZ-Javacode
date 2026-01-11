// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting;

public class FileName {
    private static final int NOT_FOUND = -1;
    private static final char UNIX_NAME_SEPARATOR = '/';
    private static final char WINDOWS_NAME_SEPARATOR = '\\';

    public static int indexOfLastSeparator(String fileName) {
        if (fileName == null) {
            return -1;
        } else {
            int lastUnixPos = fileName.lastIndexOf(47);
            int lastWindowsPos = fileName.lastIndexOf(92);
            return Math.max(lastUnixPos, lastWindowsPos);
        }
    }

    public static String getName(String fileName) {
        return fileName == null ? null : requireNonNullChars(fileName).substring(indexOfLastSeparator(fileName) + 1);
    }

    private static String requireNonNullChars(String path) {
        if (path.indexOf(0) >= 0) {
            throw new IllegalArgumentException(
                "Null character present in file/path name. There are no known legitimate use cases for such data, but several injection attacks may use it"
            );
        } else {
            return path;
        }
    }
}
