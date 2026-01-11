// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.player;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;

@UsedFromLua
public class PlayerMovementState extends State {
    private static final PlayerMovementState instance = new PlayerMovementState();
    static final Integer PARAM_RUN = 0;
    static final Integer PARAM_SPRINT = 1;

    public static PlayerMovementState instance() {
        return instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_RUN, owner.isRunning());
            StateMachineParams.put(PARAM_SPRINT, owner.isSprinting());
        } else {
            if (State.Stage.Enter == stage) {
                owner.setIsAiming(false);
            }

            owner.setRunning((Boolean)StateMachineParams.getOrDefault(PARAM_RUN, false));
            owner.setSprinting((Boolean)StateMachineParams.getOrDefault(PARAM_SPRINT, false));
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
}
