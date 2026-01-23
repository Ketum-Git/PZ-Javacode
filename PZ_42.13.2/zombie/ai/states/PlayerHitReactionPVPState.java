// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.network.GameServer;

@UsedFromLua
public final class PlayerHitReactionPVPState extends State {
    private static final PlayerHitReactionPVPState _instance = new PlayerHitReactionPVPState();

    public static PlayerHitReactionPVPState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        if (!owner.getCharacterActions().isEmpty()) {
            owner.getCharacterActions().get(0).forceStop();
        }

        owner.setSitOnGround(false);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        owner.setHitReaction("");
        owner.setVariable("hitpvp", false);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("PushAwayZombie")) {
            owner.getAttackedBy().setHitForce(0.03F);
            if (owner.getAttackedBy() instanceof IsoZombie) {
                ((IsoZombie)owner.getAttackedBy()).setPlayerAttackPosition(null);
                ((IsoZombie)owner.getAttackedBy()).setStaggerBack(true);
            }
        }

        if (event.eventName.equalsIgnoreCase("Defend")) {
            owner.getAttackedBy().setHitReaction("BiteDefended");
        }

        if (event.eventName.equalsIgnoreCase("DeathSound")) {
            if (owner.isPlayingDeathSound()) {
                return;
            }

            owner.setPlayingDeathSound(true);
            String sound = "Male";
            if (owner.isFemale()) {
                sound = "Female";
            }

            sound = sound + "BeingEatenDeath";
            owner.playSound(sound);
        }

        if (event.eventName.equalsIgnoreCase("Death")) {
            owner.setOnFloor(true);
            if (!GameServer.server) {
                owner.Kill(owner.getAttackedBy());
            }
        }
    }

    @Override
    public boolean isSyncOnEnter() {
        return false;
    }

    @Override
    public boolean isSyncOnExit() {
        return false;
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
