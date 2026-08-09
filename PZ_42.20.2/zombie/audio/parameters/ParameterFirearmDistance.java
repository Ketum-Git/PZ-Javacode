// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;

public final class ParameterFirearmDistance extends FMODLocalParameter {
    private final IsoPlayer character;

    public ParameterFirearmDistance(IsoPlayer character) {
        super("FirearmDistance");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        float distSq = FMODParameterUtils.getClosestListenerDistanceSquared(this.character.getX(), this.character.getY(), this.character.getZ());
        return PZMath.min(PZMath.sqrt(distSq), 1000.0F);
    }
}
