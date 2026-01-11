// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.util.Type;

@UsedFromLua
public final class PlayerFallingState extends State {
    private static final PlayerFallingState _instance = new PlayerFallingState();
    private static final Integer PARAM_LANDING_IMPACT = 0;
    private static final Integer PARAM_CLIMBING = 4;

    public static PlayerFallingState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoPlayer ownerPlayer = Type.tryCastTo(owner, IsoPlayer.class);
        owner.setVariable("bGetUpFromKnees", false);
        owner.setVariable("bGetUpFromProne", false);
        owner.clearVariable("bLandAnimFinished");
        if (ownerPlayer != null && ownerPlayer.getHeightAboveFloor() > 1.5F) {
            ownerPlayer.playerVoiceSound("DeathFall");
        }

        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoPlayer ownerPlayer = Type.tryCastTo(owner, IsoPlayer.class);
        if (ownerPlayer != null && !ownerPlayer.getVariableBoolean("bFalling")) {
            ownerPlayer.stopPlayerVoiceSound("DeathFall");
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("bLandAnimFinished");
        if (owner instanceof IsoPlayer player) {
            player.stopPlayerVoiceSound("DeathFall");
        }

        this.setParams(owner, State.Stage.Exit);
        owner.getStateMachineParams(this).put(PARAM_LANDING_IMPACT, owner.getImpactIsoSpeed());
        owner.clearFallDamage();
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_CLIMBING, owner.isClimbing());
        } else {
            owner.setbClimbing((Boolean)StateMachineParams.getOrDefault(PARAM_CLIMBING, false));
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

    @Override
    public boolean isProcessedOnExit() {
        return true;
    }

    @Override
    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
        float landingImpact = (Float)delegate.getOrDefault(PARAM_LANDING_IMPACT, 0.0F);
        owner.DoLand(landingImpact);
        owner.getNetworkCharacterAI().syncDamage();
    }
}
