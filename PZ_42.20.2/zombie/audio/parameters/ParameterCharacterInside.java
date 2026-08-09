// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterCharacterInside extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterCharacterInside(IsoGameCharacter character) {
        super("CharacterInside");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        if (this.character.getVehicle() == null) {
            return this.character.getCurrentBuilding() == null ? 0.0F : 1.0F;
        } else {
            return 2.0F;
        }
    }
}
