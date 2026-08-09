// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.Map;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.util.Type;

@UsedFromLua
public final class PlayerFallingState extends State {
    private static final PlayerFallingState INSTANCE = new PlayerFallingState();
    public static final State.Param<Float> LANDING_IMPACT = State.Param.ofFloat("landing_impact", 0.0F);
    public static final State.Param<Boolean> CLIMBING = State.Param.ofBool("climbing", false);

    public static PlayerFallingState instance() {
        return INSTANCE;
    }

    private PlayerFallingState() {
        super(true, true, true, false);
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
        owner.set(LANDING_IMPACT, owner.getImpactIsoSpeed());
        owner.clearFallDamage();
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        if (owner.isLocal()) {
            owner.set(CLIMBING, owner.isClimbing());
        } else {
            owner.setbClimbing(owner.get(CLIMBING));
        }

        super.setParams(owner, stage);
    }

    @Override
    public boolean isProcessedOnExit() {
        return true;
    }

    @Override
    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.DoLand(LANDING_IMPACT.fromDelegate(delegate));
        owner.getNetworkCharacterAI().syncDamage();
    }
}
