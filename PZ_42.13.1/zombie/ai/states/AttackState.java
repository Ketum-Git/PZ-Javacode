// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.LosUtil;
import zombie.iso.Vector2;
import zombie.network.GameServer;
import zombie.util.StringUtils;

@UsedFromLua
public final class AttackState extends State {
    private static final AttackState s_instance = new AttackState();
    private static final String frontStr = "FRONT";
    private static final String backStr = "BEHIND";
    private static final String rightStr = "LEFT";
    private static final String leftStr = "RIGHT";

    public static AttackState instance() {
        return s_instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.clear();
        StateMachineParams.put(0, Boolean.FALSE);
        zombie.setAttackOutcome("start");
        owner.clearVariable("AttackDidDamage");
        owner.clearVariable("ZombieBiteDone");
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoZombie zomb = (IsoZombie)owner;
        IsoGameCharacter targetChar = (IsoGameCharacter)zomb.target;
        if (targetChar == null || !"Chainsaw".equals(targetChar.getVariableString("ZombieHitReaction"))) {
            String outcome = zomb.getAttackOutcome();
            if ("success".equals(outcome) && owner.getVariableBoolean("bAttack") && owner.isVariable("targethitreaction", "EndDeath")) {
                outcome = "enddeath";
                zomb.setAttackOutcome(outcome);
            }

            if ("success".equals(outcome)
                && !owner.getVariableBoolean("bAttack")
                && !owner.getVariableBoolean("AttackDidDamage")
                && owner.getVariableString("ZombieBiteDone") == null) {
                zomb.setAttackOutcome("interrupted");
            }

            if (targetChar == null || targetChar.isDead()) {
                zomb.setTargetSeenTime(10.0F);
            }

            if (targetChar != null
                && StateMachineParams.get(0) == Boolean.FALSE
                && !"started".equals(outcome)
                && !StringUtils.isNullOrEmpty(owner.getVariableString("PlayerHitReaction"))) {
                StateMachineParams.put(0, Boolean.TRUE);
                targetChar.testDefense(zomb);
            }

            zomb.setShootable(true);
            if (zomb.target != null && !zomb.crawling) {
                if (!"fail".equals(outcome) && !"interrupted".equals(outcome)) {
                    zomb.faceThisObject(zomb.target);
                }

                zomb.setOnFloor(false);
            }

            boolean slowFactor = zomb.speedType == 1;
            if (zomb.target != null && slowFactor && ("start".equals(outcome) || "success".equals(outcome))) {
                IsoGameCharacter chr = (IsoGameCharacter)zomb.target;
                float oldSlowFactor = chr.getSlowFactor();
                if (chr.getSlowFactor() <= 0.0F) {
                    chr.setSlowTimer(30.0F);
                }

                chr.setSlowTimer(chr.getSlowTimer() + GameTime.instance.getMultiplier());
                if (chr.getSlowTimer() > 60.0F) {
                    chr.setSlowTimer(60.0F);
                }

                chr.setSlowFactor(chr.getSlowFactor() + 0.03F);
                if (chr.getSlowFactor() >= 0.5F) {
                    chr.setSlowFactor(0.5F);
                }

                if (GameServer.server && oldSlowFactor != chr.getSlowFactor()) {
                    GameServer.sendSlowFactor(chr);
                }
            }

            if (zomb.target != null) {
                zomb.target.setTimeSinceZombieAttack(0);
                zomb.target.setLastTargettedBy(zomb);
            }

            if (!zomb.crawling) {
                zomb.setVariable("AttackType", "bite");
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        owner.clearVariable("AttackOutcome");
        owner.clearVariable("AttackType");
        owner.clearVariable("PlayerHitReaction");
        owner.setStateMachineLocked(false);
        if (zombie.target != null && zombie.target.isOnFloor()) {
            zombie.setEatBodyTarget(zombie.target, true);
            zombie.setTarget(null);
        }

        zombie.allowRepathDelay = 0.0F;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoZombie zombie = (IsoZombie)owner;
        if (event.eventName.equalsIgnoreCase("SetAttackOutcome")) {
            if (zombie.getVariableBoolean("bAttack")) {
                zombie.setAttackOutcome("success");
            } else {
                zombie.setAttackOutcome("fail");
            }
        }

        if (event.eventName.equalsIgnoreCase("AttackCollisionCheck") && !zombie.isNoTeeth()) {
            IsoGameCharacter targetChar = (IsoGameCharacter)zombie.target;
            if (targetChar == null) {
                return;
            }

            targetChar.setHitFromBehind(zombie.isBehind(targetChar));
            String dotSide = targetChar.testDotSide(zombie);
            boolean isFront = dotSide.equals("FRONT");
            if (isFront && !targetChar.isAimAtFloor() && !StringUtils.isNullOrEmpty(targetChar.getVariableString("AttackType"))) {
                return;
            }

            if ("KnifeDeath".equals(targetChar.getVariableString("ZombieHitReaction"))) {
                int knifeLvl = targetChar.getPerkLevel(PerkFactory.Perks.SmallBlade) + 1;
                int chance = Math.max(0, 9 - knifeLvl * 2);
                if (Rand.NextBool(chance)) {
                    return;
                }
            }

            this.triggerPlayerReaction(owner.getVariableString("PlayerHitReaction"), owner);
            Vector2 hitDir = zombie.getHitDir();
            hitDir.x = zombie.getX();
            hitDir.y = zombie.getY();
            hitDir.x = hitDir.x - targetChar.getX();
            hitDir.y = hitDir.y - targetChar.getY();
            hitDir.normalize();
        }

        if (event.eventName.equalsIgnoreCase("EatBody")) {
            owner.setVariable("EatingStarted", true);
            ((IsoZombie)owner).setEatBodyTarget(((IsoZombie)owner).target, true);
            ((IsoZombie)owner).setTarget(null);
        }

        if (event.eventName.equalsIgnoreCase("SetState")) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Attack);
        }
    }

    /**
     * Description copied from class: State
     */
    @Override
    public boolean isAttacking(IsoGameCharacter owner) {
        return true;
    }

    private void triggerPlayerReaction(String HitReaction, IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        IsoGameCharacter targetChar = (IsoGameCharacter)zombie.target;
        if (targetChar != null) {
            if (!(zombie.DistTo(targetChar) > 1.0F) || zombie.crawling) {
                if (!zombie.isFakeDead() && !zombie.crawling || !(zombie.DistTo(targetChar) > 1.3F)) {
                    if ((!targetChar.isDead() || targetChar.getHitReaction().equals("EndDeath")) && !targetChar.isOnFloor()) {
                        if (!targetChar.isDead()) {
                            targetChar.setHitFromBehind(zombie.isBehind(targetChar));
                            String dotSide = targetChar.testDotSide(zombie);
                            boolean isFront = dotSide.equals("FRONT");
                            boolean isBack = dotSide.equals("BEHIND");
                            if (dotSide.equals("RIGHT")) {
                                HitReaction = HitReaction + "LEFT";
                            }

                            if (dotSide.equals("LEFT")) {
                                HitReaction = HitReaction + "RIGHT";
                            }

                            if (!((IsoPlayer)targetChar).isDoShove() || !isFront || targetChar.isAimAtFloor()) {
                                if (!((IsoPlayer)targetChar).isDoShove() || isFront || isBack || Rand.Next(100) <= 75) {
                                    if (!(Math.abs(zombie.getZ() - targetChar.getZ()) >= 0.2F)) {
                                        LosUtil.TestResults testResults = LosUtil.lineClear(
                                            zombie.getCell(),
                                            PZMath.fastfloor(zombie.getX()),
                                            PZMath.fastfloor(zombie.getY()),
                                            PZMath.fastfloor(zombie.getZ()),
                                            PZMath.fastfloor(targetChar.getX()),
                                            PZMath.fastfloor(targetChar.getY()),
                                            PZMath.fastfloor(targetChar.getZ()),
                                            false
                                        );
                                        if (testResults != LosUtil.TestResults.Blocked && testResults != LosUtil.TestResults.ClearThroughClosedDoor) {
                                            if (!targetChar.getSquare().isSomethingTo(zombie.getCurrentSquare())) {
                                                targetChar.setAttackedBy(zombie);
                                                boolean bDamaged = targetChar.getBodyDamage().AddRandomDamageFromZombie(zombie, HitReaction);
                                                owner.setVariable("AttackDidDamage", bDamaged);
                                                targetChar.getBodyDamage().Update();
                                                if (targetChar.isDead()) {
                                                    targetChar.setHealth(0.0F);
                                                    zombie.setEatBodyTarget(targetChar, true);
                                                    zombie.setTarget(null);
                                                } else if (targetChar.isAsleep()) {
                                                    if (GameServer.server) {
                                                        targetChar.sendObjectChange("wakeUp");
                                                    } else {
                                                        targetChar.forceAwake();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        zombie.setEatBodyTarget(targetChar, true);
                    }
                }
            }
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
