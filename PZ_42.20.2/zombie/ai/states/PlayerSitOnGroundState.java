// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

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
    private static final PlayerSitOnGroundState INSTANCE = new PlayerSitOnGroundState();
    private static final long FireCheckBaseTime = 5000L;
    private static final int ChangeAnimRandomMinTime = 30000;
    private static final int ChangeAnimRandomMaxTime = 90000;
    private static final int RAND_EXT = 2500;
    public static final State.Param<Boolean> FIRE = State.Param.ofBool("fire", false);
    public static final State.Param<String> SITGROUNDANIM = State.Param.ofString("sitgroundanim", "Idle");
    public static final State.Param<Long> CHECK_FIRE = State.Param.ofLong("check_fire", 0L);
    public static final State.Param<Long> CHANGE_ANIM = State.Param.ofLong("change_anim", 0L);

    public static PlayerSitOnGroundState instance() {
        return INSTANCE;
    }

    private PlayerSitOnGroundState() {
        super(true, true, true, true);
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
        IsoPlayer player = (IsoPlayer)owner;
        if (player.pressedMovement(false)) {
            owner.StopAllActionQueue();
            owner.setVariable("forceGetUp", true);
        }

        long currentMS = System.currentTimeMillis();
        if (currentMS > owner.get(CHECK_FIRE) + 5000L) {
            owner.set(FIRE, this.checkFire(owner));
            owner.set(CHECK_FIRE, currentMS);
        }

        if (owner.hasTimedActions() && owner.getVariableBoolean("SitGroundStarted")) {
            owner.set(FIRE, false);
            owner.setVariable("SitGroundAnim", "Idle");
        }

        boolean isFireNear = owner.get(FIRE);
        if (isFireNear) {
            boolean bChangeAnim = currentMS > owner.get(CHANGE_ANIM);
            if (bChangeAnim) {
                if ("Idle".equals(owner.getVariableString("SitGroundAnim"))) {
                    owner.setVariable("SitGroundAnim", "WarmHands");
                } else if ("WarmHands".equals(owner.getVariableString("SitGroundAnim"))) {
                    owner.setVariable("SitGroundAnim", "Idle");
                }

                owner.set(CHANGE_ANIM, currentMS + Rand.Next(30000, 90000));
            }
        } else if (owner.getVariableBoolean("SitGroundStarted")) {
            owner.clearVariable("FireNear");
            owner.setVariable("SitGroundAnim", "Idle");
        }

        if ("WarmHands".equals(owner.getVariableString("SitGroundAnim")) && Rand.Next(Rand.AdjustForFramerate(2500)) == 0) {
            owner.set(SITGROUNDANIM, owner.getVariableString("SitGroundAnim"));
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
            boolean isFireNear = owner.get(FIRE);
            if (isFireNear) {
                owner.setVariable("SitGroundAnim", "WarmHands");
            } else {
                owner.setVariable("SitGroundAnim", "Idle");
            }
        }

        if (event.eventName.equalsIgnoreCase("ResetSitOnGroundAnim")) {
            owner.setVariable("SitGroundAnim", owner.get(SITGROUNDANIM));
        }
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        if (owner.isLocal()) {
            owner.set(FIRE, this.checkFire(owner));
            owner.set(SITGROUNDANIM, owner.getVariableString("SitGroundAnim"));
            owner.set(CHECK_FIRE, System.currentTimeMillis());
            owner.set(CHANGE_ANIM, 0L);
        } else {
            owner.setRunning(false);
            owner.setSprinting(false);
            owner.set(FIRE, this.checkFire(owner));
            owner.setVariable("SitGroundAnim", owner.get(SITGROUNDANIM));
            owner.set(CHECK_FIRE, System.currentTimeMillis());
            owner.set(CHANGE_ANIM, 0L);
        }

        super.setParams(owner, stage);
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
