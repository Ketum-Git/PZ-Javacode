// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.profanity.locales;

import java.util.Objects;
import java.util.regex.Matcher;
import zombie.profanity.Phonizer;

public class LocaleEnglish extends Locale {
    public LocaleEnglish(String tag) {
        super(tag);
    }

    @Override
    protected void Init() {
        this.storeVowelsAmount = 3;
        this.addFilterRawWord("ass");
        this.addPhonizer(new Phonizer("strt", "(?<strt>^(?:KN|GN|PN|AE|WR))") {
            {
                Objects.requireNonNull(LocaleEnglish.this);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, m.group(this.getName()).substring(1, 2));
                }
            }
        });
        this.addPhonizer(new Phonizer("dropY", "(?<dropY>(?<=M)B$)") {
            {
                Objects.requireNonNull(LocaleEnglish.this);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "");
                }
            }
        });
        this.addPhonizer(new Phonizer("dropB", "(?<dropB>(?<=M)B$)") {
            {
                Objects.requireNonNull(LocaleEnglish.this);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "");
                }
            }
        });
        this.addPhonizer(new Phonizer("z", "(?<z>Z)") {
            {
                Objects.requireNonNull(LocaleEnglish.this);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "S");
                }
            }
        });
        this.addPhonizer(new Phonizer("ck", "(?<ck>CK)") {
            {
                Objects.requireNonNull(LocaleEnglish.this);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "K");
                }
            }
        });
        this.addPhonizer(new Phonizer("q", "(?<q>Q)") {
            {
                Objects.requireNonNull(LocaleEnglish.this);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "K");
                }
            }
        });
        this.addPhonizer(new Phonizer("v", "(?<v>V)") {
            {
                Objects.requireNonNull(LocaleEnglish.this);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "F");
                }
            }
        });
        this.addPhonizer(new Phonizer("xS", "(?<xS>^X)") {
            {
                Objects.requireNonNull(LocaleEnglish.this);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "S");
                }
            }
        });
        this.addPhonizer(new Phonizer("xKS", "(?<xKS>(?<=\\w)X)") {
            {
                Objects.requireNonNull(LocaleEnglish.this);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "KS");
                }
            }
        });
        this.addPhonizer(new Phonizer("ph", "(?<ph>PH)") {
            {
                Objects.requireNonNull(LocaleEnglish.this);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "F");
                }
            }
        });
        this.addPhonizer(new Phonizer("c", "(?<c>C(?=[AUOIE]))") {
            {
                Objects.requireNonNull(LocaleEnglish.this);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "K");
                }
            }
        });
    }
}
