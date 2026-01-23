// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.profanity.locales;

import java.util.Objects;
import java.util.regex.Matcher;
import zombie.profanity.Phonizer;

public class LocaleGerman extends LocaleEnglish {
    public LocaleGerman(String tag) {
        super(tag);
    }

    @Override
    protected void Init() {
        this.storeVowelsAmount = 3;
        super.Init();
        this.addPhonizer(new Phonizer("ringelS", "(?<ringelS>\u00df)") {
            {
                Objects.requireNonNull(LocaleGerman.this);
            }

            @Override
            public void execute(Matcher m, StringBuffer s) {
                if (m.group(this.getName()) != null) {
                    m.appendReplacement(s, "S");
                }
            }
        });
    }
}
