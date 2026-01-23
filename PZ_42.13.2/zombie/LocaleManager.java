// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.Locale;

public class LocaleManager {
    private static Locale userLocale = Locale.ROOT;
    private static boolean initialised;

    public static void initialise() {
        if (!initialised) {
            userLocale = Locale.getDefault();
            Locale.setDefault(Locale.ROOT);
            initialised = true;
        }
    }

    public static Locale getUserLocale() {
        return userLocale;
    }
}
