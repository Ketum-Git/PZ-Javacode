// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import fmod.fmod.FMODFootstep;
import fmod.fmod.FMODVoice;
import zombie.UsedFromLua;

@UsedFromLua
public class DummySoundBank extends BaseSoundBank {
    @Override
    public void addVoice(String alias, String sound, float priority) {
    }

    @Override
    public void addFootstep(String alias, String grass, String wood, String concrete, String upstairs) {
    }

    @Override
    public FMODVoice getVoice(String alias) {
        return null;
    }

    @Override
    public FMODFootstep getFootstep(String alias) {
        return null;
    }
}
