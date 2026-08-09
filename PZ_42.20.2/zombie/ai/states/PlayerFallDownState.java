// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.network.GameClient;
import zombie.network.GameServer;

@UsedFromLua
public final class PlayerFallDownState extends State {
    private static final PlayerFallDownState INSTANCE = new PlayerFallDownState();
    public static final State.Param<Boolean> ON_FLOOR = State.Param.ofBool("on_floor", false);
    public static final State.Param<Boolean> KNOCKED_DOWN = State.Param.ofBool("knocked_down", false);
    public static final State.Param<Boolean> DEAD = State.Param.ofBool("dead", false);

    public static PlayerFallDownState instance() {
        return INSTANCE;
    }

    private PlayerFallDownState() {
        super(true, true, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.clearVariable("bKnockedDown");
        if (owner.isDead() && !GameServer.server && !GameClient.client) {
            owner.Kill(null);
        }

        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        owner.setOnFloor(true);
        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (GameClient.client && event.eventName.equalsIgnoreCase("FallOnFront")) {
            owner.setFallOnFront(Boolean.parseBoolean(event.parameterValue));
        }
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        if (owner.isLocal()) {
            owner.set(ON_FLOOR, owner.isOnFloor());
            owner.set(KNOCKED_DOWN, owner.isKnockedDown());
            owner.set(DEAD, owner.isDead());
        } else {
            owner.setOnFloor(owner.get(ON_FLOOR));
            owner.setKnockedDown(owner.get(KNOCKED_DOWN));
            boolean isDead = owner.get(DEAD);
            if (isDead) {
                owner.setHealth(0.0F);
            }
        }

        super.setParams(owner, stage);
    }
}
