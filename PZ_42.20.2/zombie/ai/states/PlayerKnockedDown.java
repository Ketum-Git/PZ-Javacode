// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.network.GameClient;
import zombie.network.GameServer;

@UsedFromLua
public final class PlayerKnockedDown extends State {
    private static final PlayerKnockedDown INSTANCE = new PlayerKnockedDown();

    public static PlayerKnockedDown instance() {
        return INSTANCE;
    }

    private PlayerKnockedDown() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        ((IsoPlayer)owner).setBlockMovement(true);
        owner.setHitReaction("");
        owner.blockTurning = true;
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.isDead()) {
            if (!GameServer.server && !GameClient.client) {
                owner.Kill(null);
            }
        } else {
            owner.setReanimateTimer(owner.getReanimateTimer() - GameTime.getInstance().getThirtyFPSMultiplier());
        }
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("FallOnFront")) {
            owner.setFallOnFront(Boolean.parseBoolean(event.parameterValue));
        }

        if (event.eventName.equalsIgnoreCase("FallOnBack")) {
            owner.setFallOnFront(Boolean.parseBoolean(event.parameterValue));
        }

        if (event.eventName.equalsIgnoreCase("setSitOnGround")) {
            owner.setSitOnGround(Boolean.parseBoolean(event.parameterValue));
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        ((IsoPlayer)owner).setBlockMovement(false);
        owner.setKnockedDown(false);
        owner.setOnFloor(true);
        owner.blockTurning = false;
        owner.wasKnockedDown = true;
    }
}
