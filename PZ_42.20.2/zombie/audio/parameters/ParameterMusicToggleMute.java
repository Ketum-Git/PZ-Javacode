// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.SoundManager;
import zombie.audio.FMODGlobalParameter;

public final class ParameterMusicToggleMute extends FMODGlobalParameter {
    public ParameterMusicToggleMute() {
        super("MusicToggleMute");
    }

    @Override
    public float calculateCurrentValue() {
        return SoundManager.instance.allowMusic ? 0.0F : 1.0F;
    }
}
