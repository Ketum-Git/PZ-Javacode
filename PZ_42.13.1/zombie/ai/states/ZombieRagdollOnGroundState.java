// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.GameTime;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.network.GameClient;

public final class ZombieRagdollOnGroundState extends State {
    private static final ZombieRagdollOnGroundState _instance = new ZombieRagdollOnGroundState();

    public static ZombieRagdollOnGroundState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie ownerZombie = (IsoZombie)owner;
        if (!ownerZombie.isDead() && !ownerZombie.isFakeDead()) {
            if (!ownerZombie.isBecomeCrawler()) {
                if (!"Tutorial".equals(Core.gameMode)) {
                    ZombieOnGroundState.startReanimateTimer(ownerZombie);
                }

                ownerZombie.parameterZombieState.setState(ParameterZombieState.State.Idle);
            }
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        if (!zombie.isDead() && !zombie.isFakeDead()) {
            if (!zombie.isBecomeCrawler()) {
                zombie.setReanimateTimer(zombie.getReanimateTimer() - GameTime.getInstance().getThirtyFPSMultiplier());
                if (zombie.getReanimateTimer() <= 2.0F) {
                    if (GameClient.client) {
                        if (zombie.isBeingSteppedOn() && !zombie.isReanimatedPlayer()) {
                            ZombieOnGroundState.startReanimateTimer(zombie);
                        }
                    } else if (zombie.isBeingSteppedOn() && zombie.getReanimatedPlayer() == null) {
                        ZombieOnGroundState.startReanimateTimer(zombie);
                    }
                }
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }
}
