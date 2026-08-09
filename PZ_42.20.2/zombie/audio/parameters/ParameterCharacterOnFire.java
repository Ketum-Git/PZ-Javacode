// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterCharacterOnFire extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterCharacterOnFire(IsoGameCharacter character) {
        super("CharacterOnFire");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.character.isOnFire() ? 1.0F : 0.0F;
    }
}
