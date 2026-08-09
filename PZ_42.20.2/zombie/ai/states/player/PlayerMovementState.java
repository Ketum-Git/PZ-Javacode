// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.player;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;

@UsedFromLua
public class PlayerMovementState extends State {
    private static final PlayerMovementState INSTANCE = new PlayerMovementState();
    public static final State.Param<Boolean> RUN = State.Param.ofBool("run", false);
    public static final State.Param<Boolean> SPRINT = State.Param.ofBool("sprint", false);

    public static PlayerMovementState instance() {
        return INSTANCE;
    }

    private PlayerMovementState() {
        super(true, true, false, false);
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
        if (owner.isLocal()) {
            owner.set(RUN, owner.isRunning());
            owner.set(SPRINT, owner.isSprinting());
        } else {
            if (State.Stage.Enter == stage) {
                owner.setIsAiming(false);
            }

            owner.setRunning(owner.get(RUN));
            owner.setSprinting(owner.get(SPRINT));
        }

        super.setParams(owner, stage);
    }
}
