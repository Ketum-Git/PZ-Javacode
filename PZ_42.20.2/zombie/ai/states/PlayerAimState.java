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
public final class PlayerAimState extends State {
    private static final PlayerAimState INSTANCE = new PlayerAimState();
    public static final State.Param<Boolean> AIM = State.Param.of("aim", Boolean.class);
    public static final State.Param<Boolean> AIM_FLOOR = State.Param.ofBool("aim_floor", false);
    public static final State.Param<Float> AIM_FLOOR_DISTANCE = State.Param.ofFloat("aim_floor_distance", 0.0F);

    public static PlayerAimState instance() {
        return INSTANCE;
    }

    private PlayerAimState() {
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
            owner.set(AIM_FLOOR, owner.isAimAtFloor());
            owner.set(AIM_FLOOR_DISTANCE, owner.aimAtFloorTargetDistance());
        } else {
            owner.setAimAtFloor(owner.get(AIM_FLOOR), owner.get(AIM_FLOOR_DISTANCE));
            if (owner.get(AIM) instanceof Boolean aim) {
                owner.setIsAiming(aim);
            }
        }

        super.setParams(owner, stage);
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setIsAiming(AIM.fromDelegate(delegate) == Boolean.TRUE);
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
