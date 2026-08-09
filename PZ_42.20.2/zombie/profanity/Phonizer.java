// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.profanity;

import java.util.regex.Matcher;

public class Phonizer {
    private final String name;
    private final String regex;

    public Phonizer(String name, String regex) {
        this.name = name;
        this.regex = regex;
    }

    public String getName() {
        return this.name;
    }

    public String getRegex() {
        return this.regex;
    }

    public void execute(Matcher m, StringBuffer s) {
        if (m.group(this.name) != null) {
            m.appendReplacement(s, "${" + this.name + "}");
        }
    }
}
