// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.HitReactionNetworkAI;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.network.GameClient;

@UsedFromLua
public final class StaggerBackState extends State {
    private static final StaggerBackState _instance = new StaggerBackState();

    public static StaggerBackState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setStateEventDelayTimer(this.getMaxStaggerTime(owner));
        if (GameClient.client && HitReactionNetworkAI.isEnabled(owner)) {
            owner.setDeferredMovementEnabled(false);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.hasAnimationPlayer()) {
            owner.getAnimationPlayer().setTargetToAngle();
        }

        owner.setForwardDirectionFromIsoDirection();
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        if (owner.isZombie()) {
            ((IsoZombie)owner).setStaggerBack(false);
        }

        owner.setShootable(true);
        if (GameClient.client && HitReactionNetworkAI.isEnabled(owner)) {
            owner.setDeferredMovementEnabled(true);
        }
    }

    public float getMaxStaggerTime(IsoGameCharacter owner) {
        float time = 35.0F * owner.getHitForce() * owner.getStaggerTimeMod();
        if (time < 20.0F) {
            time = 20.0F;
        } else if (time > 30.0F) {
            time = 30.0F;
        }

        return time;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("FallOnFront")) {
            owner.setFallOnFront(Boolean.parseBoolean(event.parameterValue));
        }

        if (event.eventName.equalsIgnoreCase("SetState")) {
            IsoZombie zombie = (IsoZombie)owner;
            zombie.parameterZombieState.setState(ParameterZombieState.State.Pushed);
        }
    }

    @Override
    public boolean isSyncOnEnter() {
        return false;
    }

    @Override
    public boolean isSyncOnExit() {
        return false;
    }

    @Override
    public boolean isSyncOnSquare() {
        return false;
    }

    @Override
    public boolean isSyncInIdle() {
        return false;
    }
}
