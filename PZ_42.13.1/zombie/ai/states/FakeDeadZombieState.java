// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;

@UsedFromLua
public final class FakeDeadZombieState extends State {
    private static final FakeDeadZombieState _instance = new FakeDeadZombieState();

    public static FakeDeadZombieState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setVisibleToNPCs(false);
        owner.setCollidable(false);
        ((IsoZombie)owner).setFakeDead(true);
        owner.setOnFloor(true);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.isDead()) {
            if (!GameClient.client) {
                new IsoDeadBody(owner);
            }
        } else if (Core.lastStand) {
            ((IsoZombie)owner).setFakeDead(false);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        ((IsoZombie)owner).setFakeDead(false);
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
