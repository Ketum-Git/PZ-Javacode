// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import zombie.UsedFromLua;

@UsedFromLua
public final class Language {
    private final int index;
    private final String name;
    private final String text;
    private final String charset;
    private final String base;
    private final boolean azerty;

    Language(int index, String name, String text, String charset, String base, boolean azerty) {
        this.index = index;
        this.name = name;
        this.text = text;
        this.charset = charset;
        this.base = base;
        this.azerty = azerty;
    }

    public int index() {
        return this.index;
    }

    public String name() {
        return this.name;
    }

    public String text() {
        return this.text;
    }

    public String charset() {
        return this.charset;
    }

    public String base() {
        return this.base;
    }

    public boolean isAzerty() {
        return this.azerty;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static Language fromIndex(int index) {
        return Languages.instance.getByIndex(index);
    }

    public static Language FromString(String str) {
        Language language = Languages.instance.getByName(str);
        if (language == null) {
            language = Languages.instance.getDefaultLanguage();
        }

        return language;
    }
}
