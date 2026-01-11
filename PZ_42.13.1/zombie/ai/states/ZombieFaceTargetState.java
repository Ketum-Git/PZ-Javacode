// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;

public final class ZombieFaceTargetState extends State {
    private static final ZombieFaceTargetState _instance = new ZombieFaceTargetState();

    public static ZombieFaceTargetState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        if (zombie.getTarget() != null) {
            zombie.faceThisObject(zombie.getTarget());
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
