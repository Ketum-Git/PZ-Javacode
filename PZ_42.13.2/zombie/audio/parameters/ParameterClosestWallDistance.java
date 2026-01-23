// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;
import zombie.iso.NearestWalls;

public final class ParameterClosestWallDistance extends FMODGlobalParameter {
    public ParameterClosestWallDistance() {
        super("ClosestWallDistance");
    }

    @Override
    public float calculateCurrentValue() {
        IsoGameCharacter character = FMODParameterUtils.getFirstListener();
        return character == null ? 127.0F : NearestWalls.ClosestWallDistance(character.getCurrentSquare(), false);
    }
}
