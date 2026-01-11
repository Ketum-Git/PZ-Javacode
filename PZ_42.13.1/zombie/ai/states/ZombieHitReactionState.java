// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.GameTime;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoDirections;
import zombie.iso.objects.IsoZombieGiblets;

public final class ZombieHitReactionState extends State {
    private static final ZombieHitReactionState _instance = new ZombieHitReactionState();
    private static final int TURN_TO_PLAYER = 1;
    private static final int HIT_REACTION_TIMER = 2;

    public static ZombieHitReactionState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        zombie.collideWhileHit = true;
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.put(1, Boolean.FALSE);
        StateMachineParams.put(2, 0.0F);
        owner.clearVariable("onknees");
        if (zombie.isSitAgainstWall()) {
            owner.setHitReaction(null);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.put(2, (Float)StateMachineParams.get(2) + GameTime.getInstance().getMultiplier());
        if (StateMachineParams.get(1) == Boolean.TRUE) {
            if (!owner.isHitFromBehind()) {
                owner.setDir(IsoDirections.reverse(IsoDirections.fromAngle(owner.getHitDir())));
            } else {
                owner.setDir(IsoDirections.fromAngle(owner.getHitDir()));
            }
        } else if (owner.hasAnimationPlayer()) {
            owner.getAnimationPlayer().setTargetToAngle();
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        IsoZombie ownerZombie = (IsoZombie)owner;
        ownerZombie.collideWhileHit = true;
        if (ownerZombie.target != null) {
            ownerZombie.allowRepathDelay = 0.0F;
            ownerZombie.setTarget(ownerZombie.target);
        }

        ownerZombie.setStaggerBack(false);
        if (owner.isAlive()) {
            ownerZombie.setHitReaction("");
        }

        ownerZombie.setEatBodyTarget(null, false);
        ownerZombie.setSitAgainstWall(false);
        ownerZombie.setShootable(true);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoZombie zombie = (IsoZombie)owner;
        if (event.eventName.equalsIgnoreCase("DoDeath") && Boolean.parseBoolean(event.parameterValue) && owner.isAlive()) {
            owner.Kill(owner.getAttackedBy());
            if (owner.getAttackedBy() != null) {
                owner.getAttackedBy().setZombieKills(owner.getAttackedBy().getZombieKills() + 1);
            }
        }

        if (event.eventName.equalsIgnoreCase("PlayDeathSound")) {
            owner.setDoDeathSound(false);
            owner.playDeadSound();
        }

        if (event.eventName.equalsIgnoreCase("FallOnFront")) {
            owner.setFallOnFront(Boolean.parseBoolean(event.parameterValue));
        }

        if (event.eventName.equalsIgnoreCase("ActiveAnimFinishing")) {
        }

        if (event.eventName.equalsIgnoreCase("Collide") && ((IsoZombie)owner).speedType == 1) {
            ((IsoZombie)owner).collideWhileHit = false;
        }

        if (event.eventName.equalsIgnoreCase("ZombieTurnToPlayer")) {
            boolean turnToPlayer = Boolean.parseBoolean(event.parameterValue);
            StateMachineParams.put(1, turnToPlayer ? Boolean.TRUE : Boolean.FALSE);
        }

        if (event.eventName.equalsIgnoreCase("CancelKnockDown")) {
            boolean cancelKnockdown = Boolean.parseBoolean(event.parameterValue);
            if (cancelKnockdown) {
                owner.setKnockedDown(false);
            }
        }

        if (event.eventName.equalsIgnoreCase("KnockDown")) {
            owner.setOnFloor(true);
            owner.setKnockedDown(true);
        }

        if (event.eventName.equalsIgnoreCase("SplatBlood")) {
            zombie.addBlood(null, true, false, false);
            zombie.addBlood(null, true, false, false);
            zombie.addBlood(null, true, false, false);
            zombie.playBloodSplatterSound();

            for (int i = 0; i < 10; i++) {
                zombie.getCurrentSquare()
                    .getChunk()
                    .addBloodSplat(zombie.getX() + Rand.Next(-0.5F, 0.5F), zombie.getY() + Rand.Next(-0.5F, 0.5F), zombie.getZ(), Rand.Next(8));
                if (Rand.Next(5) == 0) {
                    new IsoZombieGiblets(
                        IsoZombieGiblets.GibletType.B,
                        zombie.getCell(),
                        zombie.getX(),
                        zombie.getY(),
                        zombie.getZ() + 0.3F,
                        Rand.Next(-0.2F, 0.2F) * 1.5F,
                        Rand.Next(-0.2F, 0.2F) * 1.5F
                    );
                } else {
                    new IsoZombieGiblets(
                        IsoZombieGiblets.GibletType.A,
                        zombie.getCell(),
                        zombie.getX(),
                        zombie.getY(),
                        zombie.getZ() + 0.3F,
                        Rand.Next(-0.2F, 0.2F) * 1.5F,
                        Rand.Next(-0.2F, 0.2F) * 1.5F
                    );
                }
            }
        }

        if (event.eventName.equalsIgnoreCase("SetState") && !zombie.isDead()) {
            if (zombie.getAttackedBy() != null && zombie.getAttackedBy().getVehicle() != null && "Floor".equals(zombie.getHitReaction())) {
                zombie.parameterZombieState.setState(ParameterZombieState.State.RunOver);
                return;
            }

            zombie.parameterZombieState.setState(ParameterZombieState.State.Hit);
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
