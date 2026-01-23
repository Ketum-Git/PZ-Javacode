// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugNonRecursive;

@DebugClassFields
@UsedFromLua
public class PoisonInfo {
    @DebugNonRecursive
    private final Fluid fluid;
    private final PoisonEffect maxEffect;
    private final float minAmount;
    private final float diluteRatio;

    protected PoisonInfo(Fluid fluid, float minAmount, float diluteRatio, PoisonEffect maxEffect) {
        this.fluid = fluid;
        this.minAmount = minAmount;
        this.diluteRatio = PZMath.clamp(diluteRatio, 0.0F, 1.0F);
        this.maxEffect = maxEffect;
    }

    public Fluid getFluid() {
        return this.fluid;
    }

    public PoisonEffect getPoisonEffect(float volume, float ratio) {
        return this.maxEffect;
    }
}
