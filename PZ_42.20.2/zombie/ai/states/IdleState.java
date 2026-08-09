// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public final class IdleState extends State {
    private static final IdleState INSTANCE = new IdleState();
    public static final State.Param<Boolean> AIM = State.Param.ofBool("aim", false);

    public static IdleState instance() {
        return INSTANCE;
    }

    private IdleState() {
        super(true, false, false, false);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (event.eventName.equalsIgnoreCase("PlaySound") && !StringUtils.isNullOrEmpty(event.parameterValue)) {
            owner.getSquare().playSound(event.parameterValue);
        }

        if (event.eventName.equalsIgnoreCase("PlayerVoiceSound") && player != null && player.getVariableBoolean("dbgForceAnim")) {
            player.stopPlayerVoiceSound(event.parameterValue);
            player.playerVoiceSound(event.parameterValue);
        }
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        if (owner.isLocal()) {
            owner.set(AIM, owner.isAiming());
        } else if (State.Stage.Enter == stage) {
            owner.setIsAiming(false);
        } else if (State.Stage.Exit == stage) {
            owner.setIsAiming(owner.get(AIM));
        }

        super.setParams(owner, stage);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        this.setParams(owner, State.Stage.Enter);
        owner.setOnFloor(false);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        this.setParams(owner, State.Stage.Exit);
    }
}
