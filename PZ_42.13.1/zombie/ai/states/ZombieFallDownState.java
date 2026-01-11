// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;

@UsedFromLua
public final class ZombieFallDownState extends State {
    private static final ZombieFallDownState _instance = new ZombieFallDownState();

    public static ZombieFallDownState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie ownerZombie = (IsoZombie)owner;
        ownerZombie.blockTurning = true;
        ownerZombie.setStaggerBack(false);
        if (ownerZombie.isAlive()) {
            ownerZombie.setHitReaction("");
        }

        ownerZombie.setEatBodyTarget(null, false);
        ownerZombie.setSitAgainstWall(false);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.blockTurning = false;
        owner.setOnFloor(true);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("FallOnFront")) {
            owner.setFallOnFront(Boolean.parseBoolean(event.parameterValue));
        }

        if (event.eventName.equalsIgnoreCase("PlayDeathSound")) {
            owner.setDoDeathSound(false);
            owner.playDeadSound();
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
