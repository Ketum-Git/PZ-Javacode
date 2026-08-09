// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.GameTime;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;

@UsedFromLua
public final class VehicleCollisionOnGroundState extends State {
    private static final VehicleCollisionOnGroundState INSTANCE = new VehicleCollisionOnGroundState();

    public static VehicleCollisionOnGroundState instance() {
        return INSTANCE;
    }

    private VehicleCollisionOnGroundState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setStateEventDelayTimer(getMaxReactTime(owner));
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.isVehicleCollision()) {
            float currentTimer = owner.getStateEventDelayTimer();
            float newGeneratedTimer = getMaxReactTime(owner);
            float newTimer = PZMath.min(newGeneratedTimer, currentTimer);
            owner.setStateEventDelayTimer(newTimer);
        }
    }

    public static float getMaxReactTime(IsoGameCharacter owner) {
        float timeMin = 0.1F;
        float timeMax = 0.2F;
        float forceToTime = 2.0F;
        float hitForce = owner.getHitForce();
        float staggerTimeMod = owner.getStaggerTimeMod();
        float timeRaw = 2.0F * hitForce * staggerTimeMod;
        float time = PZMath.clamp(timeRaw, 0.1F, 0.2F);
        return GameTime.getInstance().getMultiplierFromTimeDelta(time);
    }

    @Override
    public UpdateSchedulerSimulationLevel getMinimumSimulationLevel() {
        return UpdateSchedulerSimulationLevel.HALF;
    }
}
