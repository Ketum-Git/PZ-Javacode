// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;

@UsedFromLua
public final class PlayerOnGroundState extends State {
    private static final PlayerOnGroundState _instance = new PlayerOnGroundState();

    public static PlayerOnGroundState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        ((IsoPlayer)owner).setBlockMovement(true);
        owner.setVariable("bAnimEnd", false);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.isDead()) {
            owner.die();
        } else {
            owner.setReanimateTimer(owner.getReanimateTimer() - GameTime.getInstance().getThirtyFPSMultiplier());
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        ((IsoPlayer)owner).setBlockMovement(false);
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
