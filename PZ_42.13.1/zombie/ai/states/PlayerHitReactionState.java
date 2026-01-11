// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.Type;

@UsedFromLua
public final class PlayerHitReactionState extends State {
    private static final PlayerHitReactionState _instance = new PlayerHitReactionState();

    public static PlayerHitReactionState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        if (!owner.getCharacterActions().isEmpty()) {
            owner.getCharacterActions().get(0).forceStop();
        }

        owner.setIsAiming(false);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        owner.setHitReaction("");
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (owner.getAttackedBy() != null && owner.getAttackedBy() instanceof IsoZombie) {
            if (event.eventName.equalsIgnoreCase("PushAwayZombie")) {
                owner.getAttackedBy().setHitForce(0.03F);
                ((IsoZombie)owner.getAttackedBy()).setPlayerAttackPosition(null);
                ((IsoZombie)owner.getAttackedBy()).setStaggerBack(true);
            }

            if (event.eventName.equalsIgnoreCase("Defend")) {
                owner.getAttackedBy().setHitReaction("BiteDefended");
                if (GameClient.client) {
                    GameClient.sendZombieHit((IsoZombie)owner.getAttackedBy(), player);
                }
            }

            if (event.eventName.equalsIgnoreCase("DeathSound")) {
                if (owner.isPlayingDeathSound()) {
                    return;
                }

                owner.setPlayingDeathSound(true);
                if (player == null) {
                    String sound = "Male";
                    if (owner.isFemale()) {
                        sound = "Female";
                    }

                    sound = sound + "BeingEatenDeath";
                    owner.playSound(sound);
                } else {
                    player.playerVoiceSound("DeathEaten");
                }
            }

            if (event.eventName.equalsIgnoreCase("Death")) {
                owner.setOnFloor(true);
                if (!GameServer.server) {
                    owner.Kill(owner.getAttackedBy());
                }
            }
        } else {
            DebugLog.log("PlayerHitReactionState.animEvent (" + event.eventName + ") zombie is null");
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
