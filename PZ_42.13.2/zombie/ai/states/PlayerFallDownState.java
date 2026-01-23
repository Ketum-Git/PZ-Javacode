// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
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
    private static final PlayerFallDownState _instance = new PlayerFallDownState();
    private static final Integer PARAM_ON_FLOOR = 0;
    private static final Integer PARAM_KNOCKED_DOWN = 1;
    private static final Integer PARAM_DEAD = 2;

    public static PlayerFallDownState instance() {
        return _instance;
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
    public void execute(IsoGameCharacter owner) {
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
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_ON_FLOOR, owner.isOnFloor());
            StateMachineParams.put(PARAM_KNOCKED_DOWN, owner.isKnockedDown());
            StateMachineParams.put(PARAM_DEAD, owner.isDead());
        } else {
            owner.setOnFloor((Boolean)StateMachineParams.getOrDefault(PARAM_ON_FLOOR, false));
            owner.setKnockedDown((Boolean)StateMachineParams.getOrDefault(PARAM_KNOCKED_DOWN, false));
            boolean isDead = (Boolean)StateMachineParams.getOrDefault(PARAM_DEAD, false);
            if (isDead) {
                owner.setHealth(0.0F);
            }
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
