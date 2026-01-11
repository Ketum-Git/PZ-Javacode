// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;

@UsedFromLua
public class DummySoundEmitter extends BaseSoundEmitter {
    @Override
    public void randomStart() {
    }

    @Override
    public void setPos(float x, float y, float z) {
    }

    @Override
    public int stopSound(long channel) {
        return 0;
    }

    @Override
    public int stopSoundDelayRelease(long channel) {
        return 0;
    }

    @Override
    public void stopSoundLocal(long handle) {
    }

    @Override
    public void stopOrTriggerSoundLocal(long handle) {
    }

    @Override
    public int stopSoundByName(String name) {
        return 0;
    }

    @Override
    public void stopOrTriggerSound(long handle) {
    }

    @Override
    public void stopOrTriggerSoundByName(String name) {
    }

    @Override
    public void setVolume(long handle, float volume) {
    }

    @Override
    public void setPitch(long handle, float volume) {
    }

    @Override
    public boolean hasSustainPoints(long handle) {
        return false;
    }

    @Override
    public void setParameterValue(long handle, FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription, float value) {
    }

    @Override
    public void setParameterValueByName(long handle, String parameterName, float value) {
    }

    @Override
    public boolean isUsingParameter(long handle, String parameterName) {
        return false;
    }

    @Override
    public void setTimelinePosition(long handle, String positionName) {
    }

    @Override
    public void triggerCue(long handle) {
    }

    @Override
    public void set3D(long handle, boolean is3D) {
    }

    @Override
    public void setVolumeAll(float volume) {
    }

    @Override
    public void stopAll() {
    }

    @Override
    public long playSound(String file) {
        return 0L;
    }

    @Override
    public long playSound(String file, IsoGameCharacter character) {
        return 0L;
    }

    @Override
    public long playSound(String file, int x, int y, int z) {
        return 0L;
    }

    @Override
    public long playSound(String file, IsoGridSquare square) {
        return 0L;
    }

    @Override
    public long playSoundImpl(String file, IsoGridSquare square) {
        return 0L;
    }

    @Override
    public long playSound(String file, boolean doWorldSound) {
        return 0L;
    }

    @Override
    public long playSoundImpl(String file, boolean doWorldSound, IsoObject parent) {
        return 0L;
    }

    @Override
    public long playSound(String file, IsoObject parent) {
        return 0L;
    }

    @Override
    public long playSoundImpl(String file, IsoObject parent) {
        return 0L;
    }

    @Override
    public long playClip(GameSoundClip clip, IsoObject parent) {
        return 0L;
    }

    @Override
    public long playAmbientSound(String name) {
        return 0L;
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean hasSoundsToStart() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isPlaying(long channel) {
        return false;
    }

    @Override
    public boolean isPlaying(String alias) {
        return false;
    }

    @Override
    public boolean restart(long handle) {
        return false;
    }

    @Override
    public long playSoundLooped(String file) {
        return 0L;
    }

    @Override
    public long playSoundLoopedImpl(String file) {
        return 0L;
    }

    @Override
    public long playAmbientLoopedImpl(String file) {
        return 0L;
    }
}
