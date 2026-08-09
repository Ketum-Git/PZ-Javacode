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
    private static final PlayerHitReactionPVPState INSTANCE = new PlayerHitReactionPVPState();

    public static PlayerHitReactionPVPState instance() {
        return INSTANCE;
    }

    private PlayerHitReactionPVPState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        if (!owner.getCharacterActions().isEmpty()) {
            owner.getCharacterActions().get(0).forceStop();
        }

        owner.setSitOnGround(false);
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
            String sound = owner.getCharacterGender().getGenderStr();
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
}
