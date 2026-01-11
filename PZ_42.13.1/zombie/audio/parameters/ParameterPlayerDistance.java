// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;

public final class ParameterPlayerDistance extends FMODLocalParameter {
    private final IsoZombie zombie;

    public ParameterPlayerDistance(IsoZombie zombie) {
        super("PlayerDistance");
        this.zombie = zombie;
    }

    @Override
    public float calculateCurrentValue() {
        return this.zombie.target == null ? 1000.0F : (int)PZMath.ceil(this.zombie.DistToProper(this.zombie.target));
    }
}
