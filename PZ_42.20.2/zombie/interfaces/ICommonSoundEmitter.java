// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.interfaces;

/**
 * TurboTuTone.
 */
public interface ICommonSoundEmitter {
    void setPos(float x, float y, float z);

    long playSound(String file);

    @Deprecated
    long playSound(String file, boolean doWorldSound);

    void tick();

    boolean isEmpty();

    void setPitch(long handle, float pitch);

    void setVolume(long handle, float volume);

    boolean hasSustainPoints(long handle);

    void triggerCue(long handle);

    int stopSound(long channel);

    void stopOrTriggerSound(long handle);

    void stopOrTriggerSoundLocal(long arg0);

    void stopOrTriggerSoundByName(String name);

    boolean isPlaying(long channel);

    boolean isPlaying(String alias);
}
