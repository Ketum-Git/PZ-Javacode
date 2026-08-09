// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterElevation extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterElevation(IsoGameCharacter character) {
        super("Elevation");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.character.getZ();
    }
}
