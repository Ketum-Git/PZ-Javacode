// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.Map;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

@UsedFromLua
public final class PlayerStrafeState extends State {
    private static final PlayerStrafeState INSTANCE = new PlayerStrafeState();
    public static final State.Param<Boolean> AIM = State.Param.ofBool("aim", false);
    public static final State.Param<Float> STRAFE_SPEED = State.Param.ofFloat("strafe_speed", 1.0F);

    public static PlayerStrafeState instance() {
        return INSTANCE;
    }

    private PlayerStrafeState() {
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
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        if (owner.isLocal()) {
            owner.set(AIM, owner.isAiming());
            owner.set(STRAFE_SPEED, owner.getVariableFloat("StrafeSpeed", 1.0F));
        } else {
            if (State.Stage.Enter == stage) {
                owner.setIsAiming(true);
            }

            owner.setVariable("StrafeSpeed", owner.get(STRAFE_SPEED));
        }

        super.setParams(owner, stage);
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setIsAiming(AIM.fromDelegate(delegate));
    }

    @Override
    public boolean isProcessedOnExit() {
        return true;
    }

    @Override
    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setIsAiming(AIM.fromDelegate(delegate));
    }
}
