// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;

public final class ZombieFaceTargetState extends State {
    private static final ZombieFaceTargetState INSTANCE = new ZombieFaceTargetState();

    public static ZombieFaceTargetState instance() {
        return INSTANCE;
    }

    private ZombieFaceTargetState() {
        super(false, false, false, false);
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
}
