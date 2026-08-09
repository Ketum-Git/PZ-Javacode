// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.Moodles;

import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.Translator;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.Registries;

@UsedFromLua
public class Moodles {
    public static final int NeutralMoodleType = 0;
    public static final int GoodMoodleType = 1;
    public static final int BadMoodleType = 2;
    private boolean moodlesStateChanged;
    private final Map<MoodleType, Moodle> moodles = new HashMap<>();

    public Moodles(IsoGameCharacter parent) {
        for (MoodleType type : Registries.MOODLE_TYPE.values()) {
            this.moodles.put(type, new Moodle(type, parent));
        }
    }

    public int getGoodBadNeutral(MoodleType moodleType) {
        return this.GoodBadNeutral(moodleType);
    }

    public String getMoodleDisplayString(MoodleType moodleType) {
        return this.getDisplayName(moodleType, this.moodles.get(moodleType).getLevel());
    }

    public String getMoodleDescriptionString(MoodleType moodleType) {
        return this.getDescriptionText(this.moodles.get(moodleType).getMoodleType(), this.moodles.get(moodleType).getLevel());
    }

    public int getMoodleLevel(MoodleType moodleType) {
        return this.moodles.get(moodleType).getLevel();
    }

    public boolean isMaxMoodleLevel(MoodleType moodleType) {
        return this.moodles.get(moodleType).getLevel() == Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
    }

    public boolean UI_RefreshNeeded() {
        if (this.moodlesStateChanged) {
            this.moodlesStateChanged = false;
            return true;
        } else {
            return false;
        }
    }

    public void setMoodlesStateChanged(boolean refresh) {
        this.moodlesStateChanged = refresh;
    }

    public void Update() {
        for (Moodle moodle : this.moodles.values()) {
            if (moodle.Update()) {
                this.moodlesStateChanged = true;
            }
        }
    }

    private String getDisplayName(MoodleType moodleType, int level) {
        if (level > Moodle.MoodleLevel.MaxMoodleLevel.ordinal()) {
            level = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
        } else if (level == Moodle.MoodleLevel.MinMoodleLevel.ordinal()) {
            return "Invalid Moodle Level";
        }

        String key = "Moodles_" + moodleType.getTranslationName() + "_lvl" + level;
        return Translator.getText(key);
    }

    private String getDescriptionText(MoodleType moodleType, int level) {
        if (level > Moodle.MoodleLevel.MaxMoodleLevel.ordinal()) {
            level = Moodle.MoodleLevel.MaxMoodleLevel.ordinal();
        } else if (level == Moodle.MoodleLevel.MinMoodleLevel.ordinal()) {
            return "Invalid Moodle Level";
        }

        String key = "Moodles_" + moodleType.getTranslationName() + "_desc_lvl" + level;
        return Translator.getText(key);
    }

    private int GoodBadNeutral(MoodleType moodleType) {
        return moodleType == MoodleType.FOOD_EATEN ? 1 : 2;
    }
}
