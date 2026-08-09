// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class FileName {
    private static final int NOT_FOUND = -1;
    private static final char UNIX_NAME_SEPARATOR = '/';
    private static final char WINDOWS_NAME_SEPARATOR = '\\';
    private static final Pattern INVALID_CHARS = Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1F]");
    private static final Pattern TRAILING_DOTS_WHITESPACE = Pattern.compile("[.\\s]+$");
    private static final Set<String> RESERVED_NAMES = new HashSet<>(
        Arrays.asList(
            "CON",
            "PRN",
            "AUX",
            "NUL",
            "COM1",
            "COM2",
            "COM3",
            "COM4",
            "COM5",
            "COM6",
            "COM7",
            "COM8",
            "COM9",
            "LPT1",
            "LPT2",
            "LPT3",
            "LPT4",
            "LPT5",
            "LPT6",
            "LPT7",
            "LPT8",
            "LPT9"
        )
    );

    public static String sanitize(String name) {
        if (name == null) {
            return null;
        } else {
            String sanitized = INVALID_CHARS.matcher(name).replaceAll("_");
            sanitized = TRAILING_DOTS_WHITESPACE.matcher(sanitized).replaceAll("");
            if (sanitized.isEmpty()) {
                sanitized = "_";
            }

            String baseName = sanitized.split("\\.", 2)[0].toUpperCase(Locale.ROOT);
            if (RESERVED_NAMES.contains(baseName)) {
                sanitized = "_" + sanitized;
            }

            return sanitized;
        }
    }

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
