// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.scripting.objects.CharacterTrait;

public final class ParameterDeaf extends FMODLocalParameter {
    private final IsoPlayer player;

    public ParameterDeaf(IsoPlayer player) {
        super("Deaf");
        this.player = player;
    }

    @Override
    public float calculateCurrentValue() {
        return this.player.hasTrait(CharacterTrait.DEAF) ? 1.0F : 0.0F;
    }
}
