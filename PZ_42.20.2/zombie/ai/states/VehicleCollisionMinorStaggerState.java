// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.GameTime;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;

@UsedFromLua
public final class VehicleCollisionMinorStaggerState extends State {
    private static final VehicleCollisionMinorStaggerState INSTANCE = new VehicleCollisionMinorStaggerState();

    public static VehicleCollisionMinorStaggerState instance() {
        return INSTANCE;
    }

    private VehicleCollisionMinorStaggerState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setStateEventDelayTimer(getMaxStaggerTime(owner));
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.isVehicleCollision()) {
            float currentTimer = owner.getStateEventDelayTimer();
            float newGeneratedTimer = getMaxStaggerTime(owner);
            float newTimer = PZMath.min(newGeneratedTimer, currentTimer);
            owner.setStateEventDelayTimer(newTimer);
        }
    }

    public static float getMaxStaggerTime(IsoGameCharacter owner) {
        float timeMin = 0.1F;
        float timeMax = 0.2F;
        float forceToTime = 3.0F;
        float hitForce = owner.getHitForce();
        float staggerTimeMod = owner.getStaggerTimeMod();
        float timeRaw = 3.0F * hitForce * staggerTimeMod;
        float time = PZMath.clamp(timeRaw, 0.1F, 0.2F);
        return GameTime.getInstance().getMultiplierFromTimeDelta(time);
    }

    @Override
    public UpdateSchedulerSimulationLevel getMinimumSimulationLevel() {
        return UpdateSchedulerSimulationLevel.HALF;
    }
}
