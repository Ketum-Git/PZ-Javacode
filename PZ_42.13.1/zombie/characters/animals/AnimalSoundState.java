// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import java.util.HashMap;
import zombie.SandboxOptions;
import zombie.util.StringUtils;

public final class AnimalSoundState {
    private final IsoAnimal animal;
    private String desiredSoundName;
    private int desiredSoundPriority;
    private String playingSoundName;
    private long eventInstance;
    private int priority;
    private final HashMap<String, Long> lastPlayedTimeMs = new HashMap<>();
    private final HashMap<String, Long> intervalExpireTime = new HashMap<>();

    public AnimalSoundState(IsoAnimal animal) {
        this.animal = animal;
    }

    public IsoAnimal getAnimal() {
        return this.animal;
    }

    public long getEventInstance() {
        return this.eventInstance;
    }

    public long getLastPlayedTimeMS(String id) {
        return this.lastPlayedTimeMs.getOrDefault(id, 0L);
    }

    public int getPriority() {
        return this.priority;
    }

    public void setDesiredSoundPriority(int priority) {
        this.desiredSoundPriority = priority;
    }

    public int getDesiredSoundPriority() {
        return this.desiredSoundPriority;
    }

    public boolean shouldPlay() {
        if (this.desiredSoundName == null) {
            return false;
        } else if (this.isPlayingDesiredSound()) {
            return true;
        } else {
            return this.getIntervalExpireTime(this.desiredSoundName) == 0L
                ? true
                : System.currentTimeMillis() >= this.getIntervalExpireTime(this.desiredSoundName);
        }
    }

    public void setDesiredSoundName(String soundName) {
        this.desiredSoundName = StringUtils.discardNullOrWhitespace(soundName);
    }

    public String getDesiredSoundName() {
        return this.desiredSoundName;
    }

    public void setIntervalExpireTime(String id, long ms) {
        this.intervalExpireTime.put(id, ms);
    }

    public long getIntervalExpireTime(String id) {
        return this.intervalExpireTime.getOrDefault(id, 0L);
    }

    public long start(String soundName, int priority) {
        this.stop();
        if (StringUtils.isNullOrEmpty(soundName)) {
            return 0L;
        } else {
            this.playingSoundName = soundName;
            this.eventInstance = this.animal.getEmitter().playVocals(soundName);
            if (SandboxOptions.instance.animalSoundAttractZombies.getValue()
                && this.animal.adef != null
                && this.animal.adef.idleSoundRadius > 0.0F
                && this.animal.adef.idleSoundVolume > 0.0F) {
                this.animal.addWorldSoundUnlessInvisible((int)this.animal.adef.idleSoundRadius, (int)this.animal.adef.idleSoundVolume, false);
            }

            this.lastPlayedTimeMs.put(soundName, System.currentTimeMillis());
            this.priority = priority;
            return this.eventInstance;
        }
    }

    public void stop() {
        if (!this.isPlaying()) {
            this.playingSoundName = null;
            this.eventInstance = 0L;
            this.priority = 0;
        } else {
            this.animal.getEmitter().stopOrTriggerSoundLocal(this.eventInstance);
            this.eventInstance = 0L;
            this.playingSoundName = null;
            this.priority = 0;
        }
    }

    public boolean isPlaying() {
        return this.eventInstance != 0L && this.animal.getEmitter().isPlaying(this.eventInstance);
    }

    public boolean isPlayingDesiredSound() {
        if (this.desiredSoundName == null) {
            return false;
        } else {
            return !this.isPlaying() ? false : StringUtils.equals(this.playingSoundName, this.desiredSoundName);
        }
    }

    public boolean isPlaying(String soundName) {
        return this.isPlaying() && soundName != null && StringUtils.equalsIgnoreCase(soundName, this.playingSoundName);
    }
}
