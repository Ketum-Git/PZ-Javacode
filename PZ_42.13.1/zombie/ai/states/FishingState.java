// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;

/**
 * TurboTuTone.
 */
@UsedFromLua
public final class FishingState extends State {
    private static final FishingState _instance = new FishingState();
    private static final Integer PARAM_FISHING_FINISHED = 0;
    private static final Integer PARAM_FISHING_STAGE = 1;
    private static final Integer PARAM_FISHING_X = 2;
    private static final Integer PARAM_FISHING_Y = 3;
    private static final Integer PARAM_AIM = 4;

    public static FishingState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setVariable("FishingFinished", false);
        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.isSitOnGround() && ((IsoPlayer)owner).pressedMovement(false)) {
            owner.StopAllActionQueue();
            owner.setVariable("forceGetUp", true);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("forceGetUp");
        owner.setVariable("FishingFinished", true);
        owner.clearVariable("FishingStage");
        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_FISHING_FINISHED, owner.getVariableBoolean("FishingFinished"));
            StateMachineParams.put(PARAM_FISHING_STAGE, owner.getVariableString("FishingStage"));
            StateMachineParams.put(PARAM_FISHING_X, owner.getVariableString("FishingX"));
            StateMachineParams.put(PARAM_FISHING_Y, owner.getVariableString("FishingY"));
            StateMachineParams.put(PARAM_AIM, owner.isAiming());
        } else {
            if (State.Stage.Enter == stage) {
                owner.setIsAiming(true);
            } else if (State.Stage.Exit == stage) {
                owner.setIsAiming((Boolean)StateMachineParams.getOrDefault(PARAM_AIM, false));
            }

            owner.setVariable("FishingFinished", (Boolean)StateMachineParams.getOrDefault(PARAM_FISHING_FINISHED, true));
            owner.setVariable("FishingStage", (String)StateMachineParams.getOrDefault(PARAM_FISHING_STAGE, ""));
            owner.setVariable("FishingX", (String)StateMachineParams.getOrDefault(PARAM_FISHING_X, ""));
            owner.setVariable("FishingY", (String)StateMachineParams.getOrDefault(PARAM_FISHING_Y, ""));
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
        return true;
    }

    @Override
    public boolean isSyncInIdle() {
        return false;
    }
}
