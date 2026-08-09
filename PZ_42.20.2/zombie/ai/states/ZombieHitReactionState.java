// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.GameTime;
import zombie.UpdateSchedulerSimulationLevel;
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
    private static final ZombieHitReactionState INSTANCE = new ZombieHitReactionState();
    public static final State.Param<Boolean> TURN_TO_PLAYER = State.Param.ofBool("turn_to_player", false);
    public static final State.Param<Float> HIT_REACTION_TIMER = State.Param.ofFloat("hit_reaction_timer", 0.0F);

    public static ZombieHitReactionState instance() {
        return INSTANCE;
    }

    private ZombieHitReactionState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        zombie.collideWhileHit = true;
        owner.set(TURN_TO_PLAYER, false);
        owner.set(HIT_REACTION_TIMER, 0.0F);
        owner.clearVariable("onknees");
        if (zombie.isSitAgainstWall()) {
            owner.setHitReaction(null);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        owner.set(HIT_REACTION_TIMER, owner.get(HIT_REACTION_TIMER) + GameTime.getInstance().getMultiplier());
        if (owner.get(TURN_TO_PLAYER)) {
            if (!owner.isHitFromBehind()) {
                owner.setDir(IsoDirections.fromAngle(owner.getHitDir()).Rot180());
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
            owner.set(TURN_TO_PLAYER, turnToPlayer);
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
    public UpdateSchedulerSimulationLevel getMinimumSimulationLevel() {
        return UpdateSchedulerSimulationLevel.HALF;
    }
}
