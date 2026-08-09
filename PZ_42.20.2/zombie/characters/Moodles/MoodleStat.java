// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.Moodles;

import java.util.HashMap;
import java.util.Map;
import zombie.scripting.objects.MoodleType;

public class MoodleStat {
    private static final Map<MoodleType, MoodleStat> REGISTRY = new HashMap<>();
    public static final MoodleStat ENDURANCE = register(MoodleType.ENDURANCE, 0.75F, 0.5F, 0.25F, 0.1F, 0.0F);
    public static final MoodleStat ANGRY = register(MoodleType.ANGRY, 0.0F, 0.1F, 0.25F, 0.5F, 0.75F);
    public static final MoodleStat TIRED = register(MoodleType.TIRED, 0.0F, 0.6F, 0.7F, 0.8F, 0.9F);
    public static final MoodleStat HUNGRY = register(MoodleType.HUNGRY, 0.0F, 0.15F, 0.25F, 0.45F, 0.7F);
    public static final MoodleStat PANIC = register(MoodleType.PANIC, 0.0F, 6.0F, 30.0F, 65.0F, 80.0F);
    public static final MoodleStat SICK = register(MoodleType.SICK, 0.0F, 0.25F, 0.5F, 0.75F, 0.9F);
    public static final MoodleStat BORED = register(MoodleType.BORED, 0.0F, 25.0F, 50.0F, 75.0F, 90.0F);
    public static final MoodleStat UNHAPPY = register(MoodleType.UNHAPPY, 0.0F, 20.0F, 45.0F, 60.0F, 80.0F);
    public static final MoodleStat STRESS = register(MoodleType.STRESS, 0.0F, 0.25F, 0.5F, 0.75F, 0.9F);
    public static final MoodleStat THIRST = register(MoodleType.THIRST, 0.0F, 0.12F, 0.25F, 0.7F, 0.84F);
    public static final MoodleStat PAIN = register(MoodleType.PAIN, 0.0F, 10.0F, 20.0F, 50.0F, 75.0F);
    public static final MoodleStat WET = register(MoodleType.WET, 0.0F, 15.0F, 40.0F, 70.0F, 90.0F);
    public static final MoodleStat HAS_A_COLD = register(MoodleType.HAS_A_COLD, 0.0F, 20.0F, 40.0F, 60.0F, 75.0F);
    public static final MoodleStat INJURED = register(MoodleType.INJURED, 0.0F, 20.0F, 40.0F, 60.0F, 75.0F);
    public static final MoodleStat DRUNK = register(MoodleType.DRUNK, 0.0F, 10.0F, 30.0F, 50.0F, 70.0F);
    public static final MoodleStat UNCOMFORTABLE = register(MoodleType.UNCOMFORTABLE, 0.0F, 20.0F, 40.0F, 60.0F, 80.0F);
    public static final MoodleStat NOXIOUS_SMELL = register(MoodleType.NOXIOUS_SMELL, 0.0F, 0.001F, 0.001F, 0.002F, 0.002F);
    public static final MoodleStat HYPOTHERMIA = register(MoodleType.HYPOTHERMIA, 0.0F, 30.0F, 70.0F, 9.0F, 100.0F);
    public static final MoodleStat WINDCHILL = register(MoodleType.WINDCHILL, 0.0F, 5.0F, 10.0F, 15.0F, 20.0F);
    public static final MoodleStat HEAVY_LOAD = register(MoodleType.HEAVY_LOAD, 0.0F, 1.0F, 1.25F, 1.5F, 1.75F);
    private final MoodleType moodleType;
    private float minimumThreshold;
    private float lowestThreshold;
    private float moderateThreshold;
    private float highestThreshold;
    private float maximumThreshold;

    private MoodleStat(
        MoodleType moodleType, float minimumThreshold, float lowThreshold, float moderateThreshold, float highestThreshold, float maximumThreshold
    ) {
        this.moodleType = moodleType;
        this.minimumThreshold = minimumThreshold;
        this.lowestThreshold = lowThreshold;
        this.moderateThreshold = moderateThreshold;
        this.highestThreshold = highestThreshold;
        this.maximumThreshold = maximumThreshold;
    }

    public static MoodleStat register(
        MoodleType moodleType, float minimumThreshold, float lowThreshold, float moderateThreshold, float highestThreshold, float maximumThreshold
    ) {
        return REGISTRY.computeIfAbsent(
            moodleType, key -> new MoodleStat(key, minimumThreshold, lowThreshold, moderateThreshold, highestThreshold, maximumThreshold)
        );
    }

    public static MoodleStat get(MoodleType moodleType) {
        return REGISTRY.get(moodleType);
    }

    public float getMinimumThreshold() {
        return this.minimumThreshold;
    }

    public void setMinimumThreshold(float minimumThreshold) {
        this.minimumThreshold = minimumThreshold;
    }

    public float getLowestThreshold() {
        return this.lowestThreshold;
    }

    public void setLowestThreshold(float lowestThreshold) {
        this.lowestThreshold = lowestThreshold;
    }

    public float getModerateThreshold() {
        return this.moderateThreshold;
    }

    public void setModerateThreshold(float moderate) {
        this.moderateThreshold = moderate;
    }

    public float getHighestThreshold() {
        return this.highestThreshold;
    }

    public void setHighestThreshold(float highestThreshold) {
        this.highestThreshold = highestThreshold;
    }

    public float getMaximumThreshold() {
        return this.maximumThreshold;
    }

    public void setMaximumThreshold(float maximumThreshold) {
        this.maximumThreshold = maximumThreshold;
    }

    public MoodleType getMoodleType() {
        return this.moodleType;
    }
}
