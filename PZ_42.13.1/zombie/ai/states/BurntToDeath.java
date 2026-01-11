// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoSurvivor;
import zombie.characters.IsoZombie;
import zombie.iso.IsoDirections;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.ui.TutorialManager;

@UsedFromLua
public final class BurntToDeath extends State {
    private static final BurntToDeath _instance = new BurntToDeath();

    public static BurntToDeath instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        if (owner instanceof IsoSurvivor) {
            owner.getDescriptor().setDead(true);
        }

        if (!(owner instanceof IsoZombie)) {
            owner.PlayAnimUnlooped("Die");
        } else {
            owner.PlayAnimUnlooped("ZombieDeath");
        }

        owner.def.animFrameIncrease = 0.25F;
        owner.setStateMachineLocked(true);
        String t = owner.getDescriptor().getVoicePrefix() + "Death";
        owner.getEmitter().playVocals(t);
        if (GameServer.server && owner instanceof IsoZombie isoZombie) {
            GameServer.sendZombieSound(IsoZombie.ZombieSound.Burned, isoZombie);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if ((int)owner.def.frame == owner.sprite.currentAnim.frames.size() - 1) {
            if (owner == TutorialManager.instance.wife) {
                owner.dir = IsoDirections.S;
            }

            owner.RemoveAttachedAnims();
            if (!GameClient.client) {
                new IsoDeadBody(owner);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }
}
