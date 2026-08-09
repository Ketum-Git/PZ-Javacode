// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import zombie.iso.IsoObject;

public abstract class BaseCharacterSoundEmitter {
    protected final IsoGameCharacter character;

    public BaseCharacterSoundEmitter(IsoGameCharacter chr) {
        this.character = chr;
    }

    public abstract void register();

    public abstract void unregister();

    public abstract long playVocals(String file);

    public abstract void playFootsteps(String file, float volume);

    public abstract long playSound(String file);

    public abstract long playSound(String file, IsoObject proxy);

    public abstract long playSoundImpl(String file, IsoObject proxy);

    public abstract void tick();

    public abstract void set(float x, float y, float z);

    public abstract boolean isClear();

    public abstract void setPitch(long handle, float pitch);

    public abstract void setVolume(long handle, float volume);

    public abstract int stopSound(long channel);

    public abstract int stopSoundDelayRelease(long arg0);

    public abstract void stopSoundLocal(long handle);

    public abstract void stopOrTriggerSoundLocal(long arg0);

    public abstract int stopSoundByName(String soundName);

    public abstract void stopOrTriggerSound(long handle);

    public abstract void stopOrTriggerSoundByName(String name);

    public abstract void stopAll();

    public abstract boolean hasSoundsToStart();

    public abstract boolean isPlaying(long channel);

    public abstract boolean isPlaying(String alias);

    public abstract void setParameterValue(long soundRef, FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription, float value);

    public abstract void setParameterValueByName(long arg0, String arg1, float arg2);
}
