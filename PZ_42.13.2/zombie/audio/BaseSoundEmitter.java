// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;

@UsedFromLua
public abstract class BaseSoundEmitter {
    public abstract void randomStart();

    public abstract void setPos(float x, float y, float z);

    public abstract int stopSound(long channel);

    public abstract int stopSoundDelayRelease(long arg0);

    public abstract void stopSoundLocal(long handle);

    public abstract void stopOrTriggerSoundLocal(long arg0);

    public abstract int stopSoundByName(String name);

    public abstract void stopOrTriggerSound(long handle);

    public abstract void stopOrTriggerSoundByName(String name);

    public abstract void setVolume(long handle, float volume);

    public abstract void setPitch(long handle, float pitch);

    public abstract boolean hasSustainPoints(long handle);

    public abstract void setParameterValue(long handle, FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription, float value);

    public abstract void setParameterValueByName(long arg0, String arg1, float arg2);

    public abstract boolean isUsingParameter(long arg0, String arg1);

    public abstract void setTimelinePosition(long handle, String positionName);

    public abstract void triggerCue(long handle);

    public abstract void setVolumeAll(float volume);

    public abstract void stopAll();

    public abstract long playSound(String file);

    public abstract long playSound(String file, IsoGameCharacter character);

    public abstract long playSound(String file, int x, int y, int z);

    public abstract long playSound(String file, IsoGridSquare square);

    public abstract long playSoundImpl(String file, IsoGridSquare square);

    @Deprecated
    public abstract long playSound(String file, boolean doWorldSound);

    @Deprecated
    public abstract long playSoundImpl(String file, boolean doWorldSound, IsoObject parent);

    public abstract long playSoundLooped(String file);

    public abstract long playSoundLoopedImpl(String file);

    public abstract long playSound(String file, IsoObject parent);

    public abstract long playSoundImpl(String file, IsoObject parent);

    public abstract long playClip(GameSoundClip clip, IsoObject parent);

    public abstract long playAmbientSound(String name);

    public abstract long playAmbientLoopedImpl(String file);

    public abstract void set3D(long handle, boolean is3D);

    public abstract void tick();

    public abstract boolean hasSoundsToStart();

    public abstract boolean isEmpty();

    public abstract boolean isPlaying(long channel);

    public abstract boolean isPlaying(String alias);

    public abstract boolean restart(long handle);
}
