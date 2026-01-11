// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

@UsedFromLua
public final class PlayerStrafeState extends State {
    private static final PlayerStrafeState _instance = new PlayerStrafeState();
    private static final Integer PARAM_AIM = 0;
    private static final Integer PARAM_STRAFE_SPEED = 1;

    public static PlayerStrafeState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_AIM, owner.isAiming());
            StateMachineParams.put(PARAM_STRAFE_SPEED, owner.getVariableFloat("StrafeSpeed", 1.0F));
        } else {
            if (State.Stage.Enter == stage) {
                owner.setIsAiming(true);
            }

            owner.setVariable("StrafeSpeed", (Float)StateMachineParams.getOrDefault(PARAM_STRAFE_SPEED, 1.0F));
        }

        super.setParams(owner, stage);
    }

    @Override
    public boolean isSyncOnEnter() {
        return true;
    }

    @Override
    public boolean isSyncOnExit() {
        return true;
    }

    @Override
    public boolean isSyncOnSquare() {
        return false;
    }

    @Override
    public boolean isSyncInIdle() {
        return false;
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setIsAiming(delegate.get(PARAM_AIM).equals(Boolean.TRUE));
    }

    @Override
    public boolean isProcessedOnExit() {
        return true;
    }

    @Override
    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setIsAiming(delegate.get(PARAM_AIM).equals(Boolean.TRUE));
    }
}
