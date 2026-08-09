// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

public class RegExFilenameFilter implements FilenameFilter {
    private final Pattern pattern;

    public RegExFilenameFilter(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    public RegExFilenameFilter(Pattern regex) {
        this.pattern = regex;
    }

    @Override
    public boolean accept(File dir, String name) {
        return this.pattern.matcher(name).matches();
    }
}
