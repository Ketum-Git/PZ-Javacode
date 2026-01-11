// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import fmod.fmod.FMOD_STUDIO_EVENT_DESCRIPTION;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import zombie.UsedFromLua;

@UsedFromLua
public final class GameSoundClip {
    public static final short INIT_FLAG_DISTANCE_MIN = 1;
    public static final short INIT_FLAG_DISTANCE_MAX = 2;
    public static final short INIT_FLAG_STOP_IMMEDIATE = 4;
    public final GameSound gameSound;
    public String event;
    public FMOD_STUDIO_EVENT_DESCRIPTION eventDescription;
    public FMOD_STUDIO_EVENT_DESCRIPTION eventDescriptionMp;
    public String file;
    public float volume = 1.0F;
    public float pitch = 1.0F;
    public float distanceMin = 10.0F;
    public float distanceMax = 10.0F;
    public float reverbMaxRange = 10.0F;
    public float reverbFactor;
    public int priority = 5;
    public short initFlags;
    public short reloadEpoch;

    public GameSoundClip(GameSound gameSound) {
        this.gameSound = gameSound;
        this.reloadEpoch = gameSound.reloadEpoch;
    }

    public String getEvent() {
        return this.event;
    }

    public String getFile() {
        return this.file;
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

    public boolean hasMinDistance() {
        return (this.initFlags & 1) != 0;
    }

    public boolean hasMaxDistance() {
        return (this.initFlags & 2) != 0;
    }

    public float getMinDistance() {
        return this.distanceMin;
    }

    public float getMaxDistance() {
        return this.distanceMax;
    }

    public boolean isStopImmediate() {
        return (this.initFlags & 4) != 0;
    }

    public float getEffectiveVolume() {
        return this.volume * this.gameSound.getUserVolume();
    }

    public float getEffectiveVolumeInMenu() {
        return this.volume * this.gameSound.getUserVolume();
    }

    public GameSoundClip checkReloaded() {
        if (this.reloadEpoch == this.gameSound.reloadEpoch) {
            return this;
        } else {
            GameSoundClip bestClip = null;

            for (int i = 0; i < this.gameSound.clips.size(); i++) {
                GameSoundClip otherClip = this.gameSound.clips.get(i);
                if (otherClip == this) {
                    return this;
                }

                if (otherClip.event != null && otherClip.event.equals(this.event)) {
                    bestClip = otherClip;
                }

                if (otherClip.file != null && otherClip.file.equals(this.file)) {
                    bestClip = otherClip;
                }
            }

            if (bestClip == null) {
                this.reloadEpoch = this.gameSound.reloadEpoch;
                return this;
            } else {
                return bestClip;
            }
        }
    }

    public boolean hasSustainPoints() {
        return this.eventDescription != null && this.eventDescription.hasSustainPoints;
    }

    public boolean hasParameter(FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription) {
        return this.eventDescription != null && this.eventDescription.hasParameter(parameterDescription);
    }
}
