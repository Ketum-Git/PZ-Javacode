// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import java.util.ArrayList;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.core.random.Rand;

@UsedFromLua
public final class GameSound {
    public String name;
    public String category = "General";
    public boolean loop;
    public boolean is3d = true;
    public final ArrayList<GameSoundClip> clips = new ArrayList<>();
    private float userVolume = 1.0F;
    public GameSound.MasterVolume master = GameSound.MasterVolume.Primary;
    public int maxInstancesPerEmitter = -1;
    public short reloadEpoch;

    public String getName() {
        return this.name;
    }

    public String getCategory() {
        return this.category;
    }

    public boolean isLooped() {
        return this.loop;
    }

    public void setUserVolume(float gain) {
        this.userVolume = Math.max(0.0F, Math.min(2.0F, gain));
    }

    public float getUserVolume() {
        return !SystemDisabler.getEnableAdvancedSoundOptions() ? 1.0F : this.userVolume;
    }

    public GameSoundClip getRandomClip() {
        return this.clips.get(Rand.Next(this.clips.size()));
    }

    public String getMasterName() {
        return this.master.name();
    }

    public int numClipsUsingParameter(String parameterName) {
        FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription = FMODManager.instance.getParameterDescription(parameterName);
        if (parameterDescription == null) {
            return 0;
        } else {
            int result = 0;

            for (int i = 0; i < this.clips.size(); i++) {
                GameSoundClip clip = this.clips.get(i);
                if (clip.hasParameter(parameterDescription)) {
                    result++;
                }
            }

            return result;
        }
    }

    public void reset() {
        this.name = null;
        this.category = "General";
        this.loop = false;
        this.is3d = true;
        this.clips.clear();
        this.userVolume = 1.0F;
        this.master = GameSound.MasterVolume.Primary;
        this.maxInstancesPerEmitter = -1;
        this.reloadEpoch++;
    }

    public static enum MasterVolume {
        Primary,
        Ambient,
        Music,
        VehicleEngine;
    }
}
