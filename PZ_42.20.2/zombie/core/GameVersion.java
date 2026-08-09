// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;

@UsedFromLua
public final class GameVersion {
    private final int major;
    private final int minor;
    private final String suffix;
    private final String string;

    public GameVersion(int major, int minor, String suffix) {
        if (major < 0) {
            throw new IllegalArgumentException("major version must be greater than zero");
        } else if (minor >= 0 && minor <= 999) {
            this.major = major;
            this.minor = minor;
            this.suffix = suffix;
            this.string = String.format(Locale.ENGLISH, "%d.%d%s", this.major, this.minor, this.suffix == null ? "" : this.suffix);
        } else {
            throw new IllegalArgumentException("minor version must be from 0 to 999");
        }
    }

    public int getMajor() {
        return this.major;
    }

    public int getMinor() {
        return this.minor;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public int getInt() {
        return this.major * 1000 + this.minor;
    }

    public boolean isGreaterThan(GameVersion rhs) {
        return this.getInt() > rhs.getInt();
    }

    public boolean isGreaterThanOrEqualTo(GameVersion rhs) {
        return this.getInt() >= rhs.getInt();
    }

    public boolean isLessThan(GameVersion rhs) {
        return this.getInt() < rhs.getInt();
    }

    public boolean isLessThanOrEqualTo(GameVersion rhs) {
        return this.getInt() <= rhs.getInt();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else {
            return !(obj instanceof GameVersion version) ? false : version.major == this.major && version.minor == this.minor;
        }
    }

    @Override
    public String toString() {
        return this.string;
    }

    public static GameVersion parse(String str) {
        Matcher m1 = Pattern.compile("([0-9]+)\\.([0-9]+)(.*)").matcher(str);
        if (m1.matches()) {
            int major = PZMath.tryParseInt(m1.group(1), 0);
            int minor = PZMath.tryParseInt(m1.group(2), 0);
            String suffix = m1.group(3);
            return new GameVersion(major, minor, suffix);
        } else {
            throw new IllegalArgumentException("invalid game version \"" + str + "\"");
        }
    }
}
