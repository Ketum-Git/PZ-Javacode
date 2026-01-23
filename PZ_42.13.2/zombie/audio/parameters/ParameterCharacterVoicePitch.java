// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterCharacterVoicePitch extends FMODLocalParameter {
    private final IsoPlayer player;

    public ParameterCharacterVoicePitch(IsoPlayer player) {
        super("CharacterVoicePitch");
        this.player = player;
    }

    @Override
    public float calculateCurrentValue() {
        return this.player.getVoicePitch();
    }
}
