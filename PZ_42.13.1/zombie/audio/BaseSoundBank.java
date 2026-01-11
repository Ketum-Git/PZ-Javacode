// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import fmod.fmod.FMODFootstep;
import fmod.fmod.FMODVoice;

public abstract class BaseSoundBank {
    public static BaseSoundBank instance;

    public abstract void addVoice(String alias, String sound, float priority);

    public abstract void addFootstep(String alias, String grass, String wood, String concrete, String upstairs);

    public abstract FMODVoice getVoice(String alias);

    public abstract FMODFootstep getFootstep(String alias);
}
