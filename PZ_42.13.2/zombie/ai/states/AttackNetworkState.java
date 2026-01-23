// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.ai.State;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.network.GameClient;
import zombie.util.StringUtils;

public class AttackNetworkState extends State {
    private static final AttackNetworkState s_instance = new AttackNetworkState();
    private String attackOutcome;

    public static AttackNetworkState instance() {
        return s_instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.clear();
        StateMachineParams.put(0, Boolean.FALSE);
        this.attackOutcome = zombie.getAttackOutcome();
        zombie.setAttackOutcome("start");
        owner.clearVariable("AttackDidDamage");
        owner.clearVariable("ZombieBiteDone");
        zombie.setTargetSeenTime(1.0F);
        if (!zombie.crawling) {
            zombie.setVariable("AttackType", "bite");
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoGameCharacter targetChar = (IsoGameCharacter)zombie.target;
        if (targetChar == null || !"Chainsaw".equals(targetChar.getVariableString("ZombieHitReaction"))) {
            String outcome = zombie.getAttackOutcome();
            if ("success".equals(outcome)
                && !owner.getVariableBoolean("bAttack")
                && (targetChar == null || !targetChar.isGodMod())
                && !owner.getVariableBoolean("AttackDidDamage")
                && owner.getVariableString("ZombieBiteDone") != "true") {
                zombie.setAttackOutcome("interrupted");
            }

            if (targetChar == null || targetChar.isDead()) {
                zombie.setTargetSeenTime(10.0F);
            }

            if (targetChar != null
                && StateMachineParams.get(0) == Boolean.FALSE
                && !"started".equals(outcome)
                && !StringUtils.isNullOrEmpty(owner.getVariableString("PlayerHitReaction"))) {
                StateMachineParams.put(0, Boolean.TRUE);
            }

            zombie.setShootable(true);
            if (zombie.target != null && !zombie.crawling) {
                if (!"fail".equals(outcome) && !"interrupted".equals(outcome)) {
                    zombie.faceThisObject(zombie.target);
                }

                zombie.setOnFloor(false);
            }

            if (zombie.target != null) {
                zombie.target.setTimeSinceZombieAttack(0);
                zombie.target.setLastTargettedBy(zombie);
            }

            if (!zombie.crawling) {
                zombie.setVariable("AttackType", "bite");
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
        if (GameClient.client && zombie.isRemoteZombie()) {
            if (event.eventName.equalsIgnoreCase("SetAttackOutcome")) {
                zombie.setAttackOutcome("fail".equals(this.attackOutcome) ? "fail" : "success");
            }

            if (event.eventName.equalsIgnoreCase("AttackCollisionCheck") && zombie.target instanceof IsoPlayer player) {
                if (zombie.scratch) {
                    zombie.getEmitter().playSoundImpl("ZombieScratch", zombie);
                } else if (zombie.laceration) {
                    zombie.getEmitter().playSoundImpl("ZombieScratch", zombie);
                } else {
                    zombie.getEmitter().playSoundImpl(zombie.getBiteSoundName(), zombie);
                    player.splatBloodFloorBig();
                    player.splatBloodFloorBig();
                    player.splatBloodFloorBig();
                }
            }

            if (event.eventName.equalsIgnoreCase("EatBody")) {
                owner.setVariable("EatingStarted", true);
                ((IsoZombie)owner).setEatBodyTarget(((IsoZombie)owner).target, true);
                ((IsoZombie)owner).setTarget(null);
            }
        }

        if (event.eventName.equalsIgnoreCase("SetState")) {
            zombie.parameterZombieState.setState(ParameterZombieState.State.Attack);
        }
    }

    @Override
    public boolean isAttacking(IsoGameCharacter owner) {
        return true;
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
