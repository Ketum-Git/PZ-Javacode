// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;

public final class ParameterCharacterElevation extends FMODGlobalParameter {
    public ParameterCharacterElevation() {
        super("CharacterElevation");
    }

    @Override
    public float calculateCurrentValue() {
        IsoGameCharacter character = FMODParameterUtils.getFirstListener();
        return character == null ? 0.0F : character.getZ();
    }
}
