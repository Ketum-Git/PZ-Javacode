// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import java.util.Map;
import zombie.AttackType;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoFireplace;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public final class PlayerSitOnGroundState extends State {
    private static final PlayerSitOnGroundState _instance = new PlayerSitOnGroundState();
    private static final long FireCheckBaseTime = 5000L;
    private static final int ChangeAnimRandomMinTime = 30000;
    private static final int ChangeAnimRandomMaxTime = 90000;
    private static final int RAND_EXT = 2500;
    private static final Integer PARAM_FIRE = 0;
    private static final Integer PARAM_SITGROUNDANIM = 1;
    private static final Integer PARAM_CHECK_FIRE = 2;
    private static final Integer PARAM_CHANGE_ANIM = 3;

    public static PlayerSitOnGroundState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setSitOnGround(true);
        if ((owner.getPrimaryHandItem() == null || !(owner.getPrimaryHandItem() instanceof HandWeapon))
            && (owner.getSecondaryHandItem() == null || !(owner.getSecondaryHandItem() instanceof HandWeapon))) {
            owner.setHideWeaponModel(true);
        }

        if (owner.getStateMachine().getPrevious() == IdleState.instance()) {
            owner.clearVariable("SitGroundStarted");
            owner.clearVariable("forceGetUp");
            owner.clearVariable("SitGroundAnim");
        }

        if (owner.getStateMachine().getPrevious() == FishingState.instance()) {
            owner.setVariable("SitGroundAnim", "Idle");
        }

        this.setParams(owner, State.Stage.Enter);
    }

    private boolean checkFire(IsoGameCharacter owner) {
        IsoGridSquare currentSq = owner.getCurrentSquare();
        if (currentSq == null) {
            return false;
        } else {
            for (int x = -4; x < 4; x++) {
                for (int y = -4; y < 4; y++) {
                    IsoGridSquare sq = currentSq.getCell().getGridSquare(currentSq.x + x, currentSq.y + y, currentSq.z);
                    if (sq != null) {
                        if (sq.haveFire()) {
                            return true;
                        }

                        for (int i = 0; i < sq.getObjects().size(); i++) {
                            IsoFireplace fireplace = Type.tryCastTo(sq.getObjects().get(i), IsoFireplace.class);
                            if (fireplace != null && fireplace.isLit()) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoPlayer player = (IsoPlayer)owner;
        if (player.pressedMovement(false)) {
            owner.StopAllActionQueue();
            owner.setVariable("forceGetUp", true);
        }

        long currentMS = System.currentTimeMillis();
        if (currentMS > (Long)StateMachineParams.get(PARAM_CHECK_FIRE) + 5000L) {
            StateMachineParams.put(PARAM_FIRE, this.checkFire(owner));
            StateMachineParams.put(PARAM_CHECK_FIRE, currentMS);
        }

        if (owner.hasTimedActions() && owner.getVariableBoolean("SitGroundStarted")) {
            StateMachineParams.put(PARAM_FIRE, false);
            owner.setVariable("SitGroundAnim", "Idle");
        }

        boolean isFireNear = (Boolean)StateMachineParams.get(PARAM_FIRE);
        if (isFireNear) {
            boolean bChangeAnim = currentMS > (Long)StateMachineParams.get(PARAM_CHANGE_ANIM);
            if (bChangeAnim) {
                if ("Idle".equals(owner.getVariableString("SitGroundAnim"))) {
                    owner.setVariable("SitGroundAnim", "WarmHands");
                } else if ("WarmHands".equals(owner.getVariableString("SitGroundAnim"))) {
                    owner.setVariable("SitGroundAnim", "Idle");
                }

                StateMachineParams.put(PARAM_CHANGE_ANIM, currentMS + Rand.Next(30000, 90000));
            }
        } else if (owner.getVariableBoolean("SitGroundStarted")) {
            owner.clearVariable("FireNear");
            owner.setVariable("SitGroundAnim", "Idle");
        }

        if ("WarmHands".equals(owner.getVariableString("SitGroundAnim")) && Rand.Next(Rand.AdjustForFramerate(2500)) == 0) {
            StateMachineParams.put(PARAM_SITGROUNDANIM, owner.getVariableString("SitGroundAnim"));
            owner.setVariable("SitGroundAnim", "rubhands");
        }

        player.setInitiateAttack(false);
        player.setAttackStarted(false);
        player.setAttackType(AttackType.NONE);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setHideWeaponModel(false);
        if (StringUtils.isNullOrEmpty(owner.getVariableString("HitReaction"))) {
            owner.clearVariable("SitGroundStarted");
            owner.clearVariable("forceGetUp");
            owner.clearVariable("SitGroundAnim");
            owner.setIgnoreMovement(false);
        }

        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (event.eventName.equalsIgnoreCase("SitGroundStarted")) {
            owner.setVariable("SitGroundStarted", true);
            boolean isFireNear = (Boolean)owner.getStateMachineParams(this).get(PARAM_FIRE);
            if (isFireNear) {
                owner.setVariable("SitGroundAnim", "WarmHands");
            } else {
                owner.setVariable("SitGroundAnim", "Idle");
            }
        }

        if (event.eventName.equalsIgnoreCase("ResetSitOnGroundAnim")) {
            owner.setVariable("SitGroundAnim", (String)owner.getStateMachineParams(this).get(PARAM_SITGROUNDANIM));
        }
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_FIRE, this.checkFire(owner));
            StateMachineParams.put(PARAM_SITGROUNDANIM, owner.getVariableString("SitGroundAnim"));
            StateMachineParams.put(PARAM_CHECK_FIRE, System.currentTimeMillis());
            StateMachineParams.put(PARAM_CHANGE_ANIM, 0L);
        } else {
            owner.setRunning(false);
            owner.setSprinting(false);
            StateMachineParams.put(PARAM_FIRE, this.checkFire(owner));
            owner.setVariable("SitGroundAnim", (String)StateMachineParams.getOrDefault(PARAM_SITGROUNDANIM, "Idle"));
            StateMachineParams.put(PARAM_CHECK_FIRE, System.currentTimeMillis());
            StateMachineParams.put(PARAM_CHANGE_ANIM, 0L);
        }

        super.setParams(owner, stage);
    }

    @Override
    public boolean isSyncOnEnter() {
        return true;
    }

    @Override
    public boolean isSyncOnExit() {
        return true;
    }

    @Override
    public boolean isSyncOnSquare() {
        return true;
    }

    @Override
    public boolean isSyncInIdle() {
        return true;
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setSitOnGround(true);
    }

    @Override
    public boolean isProcessedOnExit() {
        return true;
    }

    @Override
    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setSitOnGround(false);
    }
}
