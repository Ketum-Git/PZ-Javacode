// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.network.GameClient;
import zombie.popman.ZombiePopulationManager;

@UsedFromLua
public final class ZombieSittingState extends State {
    private static final ZombieSittingState _instance = new ZombieSittingState();

    public static ZombieSittingState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        zombie.setSitAgainstWall(true);
        zombie.setOnFloor(true);
        zombie.setKnockedDown(false);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        if (GameClient.client && owner.getCurrentSquare() != null) {
            ZombiePopulationManager.instance.sitAgainstWall(zombie, zombie.getCurrentSquare());
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
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
