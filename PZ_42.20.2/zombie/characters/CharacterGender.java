// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.core.Translator;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public enum CharacterGender {
    MALE("Male", "IGUI_char_Male"),
    FEMALE("Female", "IGUI_char_Female");

    private final String genderStr;
    private final String displayStringKey;

    private CharacterGender(final String genderStr, final String displayStringKey) {
        this.genderStr = genderStr;
        this.displayStringKey = displayStringKey;
    }

    public String getDisplayString() {
        return Translator.getText(this.getDisplayStringKey());
    }

    public String getDisplayStringKey() {
        return this.displayStringKey;
    }

    public String getGenderStr() {
        return this.genderStr;
    }

    @Override
    public String toString() {
        return this.getGenderStr();
    }

    public static CharacterGender fromString(String genderStr) {
        return PZArrayUtil.findOrDefault(values(), genderStr, (val, str) -> StringUtils.equalsIgnoreCase(val.genderStr, str), null);
    }
}
