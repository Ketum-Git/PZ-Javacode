// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.HashMap;
import java.util.Map;
import zombie.core.textures.Texture;
import zombie.scripting.objects.MoodleType;

public final class MoodleTextureSet {
    private final int size;
    private Texture border;
    private Texture background;
    private final Map<MoodleType, Texture> moodleTextures = new HashMap<>();

    public MoodleTextureSet(int size) {
        this.size = size;
        int flags = 0;
        flags |= 64;
        String dir = "media/ui/Moodles/" + size;
        this.border = Texture.getSharedTexture(dir + "/_Moodles_BGoutline.png", flags);
        this.background = Texture.getSharedTexture(dir + "/_Moodles_BGsolid.png", flags);
        this.moodleTextures.put(MoodleType.ENDURANCE, Texture.getSharedTexture(dir + "/Status_DifficultyBreathing.png", flags));
        this.moodleTextures.put(MoodleType.TIRED, Texture.getSharedTexture(dir + "/Mood_Sleepy.png", flags));
        this.moodleTextures.put(MoodleType.HUNGRY, Texture.getSharedTexture(dir + "/Status_Hunger.png", flags));
        this.moodleTextures.put(MoodleType.PANIC, Texture.getSharedTexture(dir + "/Mood_Panicked.png", flags));
        this.moodleTextures.put(MoodleType.SICK, Texture.getSharedTexture(dir + "/Mood_Nauseous.png", flags));
        this.moodleTextures.put(MoodleType.BORED, Texture.getSharedTexture(dir + "/Mood_Bored.png", flags));
        this.moodleTextures.put(MoodleType.UNHAPPY, Texture.getSharedTexture(dir + "/Mood_Sad.png", flags));
        this.moodleTextures.put(MoodleType.BLEEDING, Texture.getSharedTexture(dir + "/Status_Bleeding.png", flags));
        this.moodleTextures.put(MoodleType.WET, Texture.getSharedTexture(dir + "/Status_Wet.png", flags));
        this.moodleTextures.put(MoodleType.HAS_A_COLD, Texture.getSharedTexture(dir + "/Mood_Ill.png", flags));
        this.moodleTextures.put(MoodleType.ANGRY, Texture.getSharedTexture(dir + "/Mood_Angry.png", flags));
        this.moodleTextures.put(MoodleType.STRESS, Texture.getSharedTexture(dir + "/Mood_Stressed.png", flags));
        this.moodleTextures.put(MoodleType.THIRST, Texture.getSharedTexture(dir + "/Status_Thirst.png", flags));
        this.moodleTextures.put(MoodleType.INJURED, Texture.getSharedTexture(dir + "/Status_InjuredMinor.png", flags));
        this.moodleTextures.put(MoodleType.PAIN, Texture.getSharedTexture(dir + "/Mood_Pained.png", flags));
        this.moodleTextures.put(MoodleType.HEAVY_LOAD, Texture.getSharedTexture(dir + "/Status_HeavyLoad.png", flags));
        this.moodleTextures.put(MoodleType.DRUNK, Texture.getSharedTexture(dir + "/Mood_Drunk.png", flags));
        this.moodleTextures.put(MoodleType.DEAD, Texture.getSharedTexture(dir + "/Mood_Dead.png", flags));
        this.moodleTextures.put(MoodleType.ZOMBIE, Texture.getSharedTexture(dir + "/Mood_Zombified.png", flags));
        this.moodleTextures.put(MoodleType.NOXIOUS_SMELL, Texture.getSharedTexture(dir + "/Mood_NoxiousSmell.png", flags));
        this.moodleTextures.put(MoodleType.FOOD_EATEN, Texture.getSharedTexture(dir + "/Status_Hunger.png", flags));
        this.moodleTextures.put(MoodleType.HYPERTHERMIA, Texture.getSharedTexture(dir + "/Status_TemperatureHot.png", flags));
        this.moodleTextures.put(MoodleType.HYPOTHERMIA, Texture.getSharedTexture(dir + "/Status_TemperatureLow.png", flags));
        this.moodleTextures.put(MoodleType.WINDCHILL, Texture.getSharedTexture(dir + "/Status_Windchill.png", flags));
        this.moodleTextures.put(MoodleType.CANT_SPRINT, Texture.getSharedTexture(dir + "/Status_MovementRestricted.png", flags));
        this.moodleTextures.put(MoodleType.UNCOMFORTABLE, Texture.getSharedTexture(dir + "/Mood_Discomfort.png", flags));
    }

    public Texture getTexture(MoodleType moodleType) {
        return this.moodleTextures.get(moodleType);
    }

    public Texture getBorder() {
        return this.border;
    }

    public Texture getBackground() {
        return this.background;
    }

    public int getSize() {
        return this.size;
    }
}
