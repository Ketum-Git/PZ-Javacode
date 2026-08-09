// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import java.util.ArrayList;
import zombie.characters.BaseCharacterSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;
import zombie.scripting.objects.MoodleType;

public final class ParameterMoodles {
    private final IsoGameCharacter character;
    private final ArrayList<ParameterMoodles.ParameterMoodle> moodles = new ArrayList<>();

    public ParameterMoodles(IsoGameCharacter character) {
        this.character = character;
        this.addMoodle("Bleeding", MoodleType.BLEEDING);
        this.addMoodle("Bored", MoodleType.BORED);
        this.addMoodle("Drunk", MoodleType.DRUNK);
        this.addMoodle("Endurance", MoodleType.ENDURANCE);
        this.addMoodle("FoodEaten", MoodleType.FOOD_EATEN);
        this.addMoodle("HasACold", MoodleType.HAS_A_COLD);
        this.addMoodle("HeavyLoad", MoodleType.HEAVY_LOAD);
        this.addMoodle("Hungry", MoodleType.HUNGRY);
        this.addMoodle("Hyperthermia", MoodleType.HYPERTHERMIA);
        this.addMoodle("Hypothermia", MoodleType.HYPOTHERMIA);
        this.addMoodle("Injured", MoodleType.INJURED);
        this.addMoodle("Pain", MoodleType.PAIN);
        this.addMoodle("Panic", MoodleType.PANIC);
        this.addMoodle("Sick", MoodleType.SICK);
        this.addMoodle("Stress", MoodleType.STRESS);
        this.addMoodle("Thirst", MoodleType.THIRST);
        this.addMoodle("Tired", MoodleType.TIRED);
        this.addMoodle("Unhappy", MoodleType.UNHAPPY);
        this.addMoodle("Wet", MoodleType.WET);
        this.addMoodle("Windchill", MoodleType.WINDCHILL);
    }

    private void addMoodle(String name, MoodleType moodleType) {
        ParameterMoodles.ParameterMoodle moodle = new ParameterMoodles.ParameterMoodle("Moodle" + name, moodleType);
        this.moodles.add(moodle);
    }

    public void update(long eventInstance) {
        if (eventInstance != 0L) {
            if (this.character.getMoodles() != null) {
                for (int i = 0; i < this.moodles.size(); i++) {
                    ParameterMoodles.ParameterMoodle moodle = this.moodles.get(i);
                    float currentValue = moodle.calculateCurrentValue(this.character);
                    currentValue = PZMath.clamp(currentValue, 0.0F, 1.0F);
                    moodle.setCurrentValue(this.character.getEmitter(), eventInstance, currentValue);
                }
            }
        }
    }

    public void reset() {
        for (int i = 0; i < this.moodles.size(); i++) {
            ParameterMoodles.ParameterMoodle moodle = this.moodles.get(i);
            moodle.reset();
        }
    }

    private static final class ParameterMoodle {
        final String parameterName;
        final MoodleType moodleType;
        FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription;
        float currentValue = Float.NaN;

        ParameterMoodle(String parameterName, MoodleType moodleType) {
            this.parameterName = parameterName;
            this.moodleType = moodleType;
            this.parameterDescription = FMODManager.instance.getParameterDescription(parameterName);
        }

        float calculateCurrentValue(IsoGameCharacter character) {
            return character.isDead() ? 0.0F : character.getMoodles().getMoodleLevel(this.moodleType) / 4.0F;
        }

        void setCurrentValue(BaseCharacterSoundEmitter emitter, long eventInstance, float value) {
            if (value != this.currentValue) {
                this.currentValue = value;
                if (this.parameterDescription != null) {
                    emitter.setParameterValue(eventInstance, this.parameterDescription, value);
                }
            }
        }

        void reset() {
            this.currentValue = Float.NaN;
        }
    }
}
