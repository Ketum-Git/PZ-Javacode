// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterCharacterMoving extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterCharacterMoving(IsoGameCharacter character) {
        super("CharacterMoving");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.character.isPlayerMoving() ? 1.0F : 0.0F;
    }
}
