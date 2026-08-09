// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.Map;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;

/**
 * TurboTuTone.
 */
@UsedFromLua
public final class FishingState extends State {
    private static final FishingState INSTANCE = new FishingState();
    public static final State.Param<Boolean> FISHING_FINISHED = State.Param.ofBool("fishing_finished", true);
    public static final State.Param<String> FISHING_STAGE = State.Param.ofString("fishing_stage", "");
    public static final State.Param<String> FISHING_X = State.Param.ofString("fishing_x", "");
    public static final State.Param<String> FISHING_Y = State.Param.ofString("fishing_y", "");
    public static final State.Param<Boolean> AIM = State.Param.ofBool("aim", false);

    public static FishingState instance() {
        return INSTANCE;
    }

    private FishingState() {
        super(true, true, true, false);
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
        if (owner.isLocal()) {
            owner.set(FISHING_FINISHED, owner.getVariableBoolean("FishingFinished"));
            owner.set(FISHING_STAGE, owner.getVariableString("FishingStage"));
            owner.set(FISHING_X, owner.getVariableString("FishingX"));
            owner.set(FISHING_Y, owner.getVariableString("FishingY"));
            owner.set(AIM, owner.isAiming());
        } else {
            if (State.Stage.Enter == stage) {
                owner.setIsAiming(true);
            } else if (State.Stage.Exit == stage) {
                owner.setIsAiming(owner.get(AIM));
            }

            owner.setVariable("FishingFinished", owner.get(FISHING_FINISHED));
            owner.setVariable("FishingStage", owner.get(FISHING_STAGE));
            owner.setVariable("FishingX", owner.get(FISHING_X));
            owner.setVariable("FishingY", owner.get(FISHING_Y));
        }

        super.setParams(owner, stage);
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setIsAiming(false);
    }

    @Override
    public boolean isProcessedOnExit() {
        return true;
    }

    @Override
    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setIsAiming(AIM.fromDelegate(delegate) == Boolean.TRUE);
    }
}
