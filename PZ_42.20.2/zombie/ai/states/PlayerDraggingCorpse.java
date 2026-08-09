// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.CombatManager;
import zombie.SandboxOptions;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoDirections;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.ui.UIManager;
import zombie.util.StringUtils;

public final class PlayerDraggingCorpse extends State {
    private static final PlayerDraggingCorpse INSTANCE = new PlayerDraggingCorpse();
    public static final State.Param<String> GRAPPLING_TYPE = State.Param.ofString("grappling_type", null);
    public static final State.Param<IGrappleable> GRAPPLING_TARGET = State.Param.of("grappling_target", IGrappleable.class);
    public static final State.Param<Boolean> DO_GRAPPLE = State.Param.ofBool("do_grapple", false);
    public static final State.Param<Boolean> DO_CONTINUE_GRAPPLE = State.Param.ofBool("do_continue_grapple", false);
    public static final State.Param<Boolean> IS_THROW_OUT_WINDOW = State.Param.ofBool("is_throw_out_window", false);
    public static final State.Param<Boolean> IS_THROW_OVER_FENCE = State.Param.ofBool("is_throw_over_fence", false);
    public static final State.Param<Boolean> IS_THROW_INTO_CONTAINER = State.Param.ofBool("is_throw_into_container", false);
    public static final State.Param<Integer> GRUNT_COUNTER = State.Param.ofInt("grunt_counter", 0);
    public static final State.Param<Float> BEARING_FROM_GRAPPLE_TARGET = State.Param.ofFloat("bearing_from_grapple_target", 0.0F);
    public static final State.Param<IsoObject> THROWN_OUT_WINDOW_OBJ = State.Param.of("thrown_out_window_obj", IsoObject.class);
    public static final State.Param<IsoDirections> THROWN_OVER_FENCE_DIR = State.Param.of("thrown_over_fence_dir", IsoDirections.class);
    public static final State.Param<ItemContainer> THROWN_INTO_CONTAINER_OBJ = State.Param.of("thrown_into_container_obj", ItemContainer.class);

    public static PlayerDraggingCorpse instance() {
        return INSTANCE;
    }

    private PlayerDraggingCorpse() {
        super(true, true, false, false);
        this.addAnimEventListener("ActiveAnimFinishing", this::OnAnimEvent_ActiveAnimFinishing);
        this.addAnimEventListener("NonLoopedAnimFadeOut", this::OnAnimEvent_ActiveAnimFinishing);
        this.addAnimEventListener("AttackAnim", this::OnAnimEvent_AttackAnim);
        this.addAnimEventListener("BlockTurn", this::OnAnimEvent_BlockTurn);
        this.addAnimEventListener("ShoveAnim", this::OnAnimEvent_ShoveAnim);
        this.addAnimEventListener("StompAnim", this::OnAnimEvent_StompAnim);
        this.addAnimEventListener("GrappleGrabAnim", this::OnAnimEvent_GrappleGrabAnim);
        this.addAnimEventListener("BlockMovement", this::OnAnimEvent_BlockMovement);
        this.addAnimEventListener("ShotDone", this::OnAnimEvent_ShotDone);
        this.addAnimEventListener(this::OnAnimEvent_SetVariable);
        this.addAnimEventListener("SetMeleeDelay", this::OnAnimEvent_SetMeleeDelay);
        this.addAnimEventListener("PlaySwingSound", this::OnAnimEvent_PlaySwingSound);
        this.addAnimEventListener("PlayerVoiceSound", this::OnAnimEvent_PlayerVoiceSound);
        this.addAnimEventListener("SitGroundStarted", this::OnAnimEvent_SitGroundStarted);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoPlayer player = (IsoPlayer)owner;
        if ("HitReaction".equals(player.getHitReaction())) {
            player.clearVariable("HitReaction");
        }

        if (!GameServer.server) {
            UIManager.speedControls.SetCurrentGameSpeed(1);
        }

        player.setVariable("ShotDone", false);
        player.setPerformingShoveAnimation(false);
        player.setPerformingGrappleGrabAnimation(false);
        if (!GameClient.client || player.isLocalPlayer()) {
            player.setVariable("AimFloorAnim", player.getAttackVars().aimAtFloor);
        }

        player.StopAllActionQueue();
        if (player.isLocalPlayer()) {
            IsoWorld.instance.currentCell.setDrag(null, player.playerIndex);
        }

        player.setAimAtFloor(false);
        player.setDoShove(false);
        player.setPerformingGrappleGrabAnimation(player.getAttackVars().doGrapple);
        player.useChargeDelta = player.getAttackVars().useChargeDelta;
        player.targetOnGround = (IsoGameCharacter)player.getAttackVars().targetOnGround.getObject();
        if (owner.getGrapplingTarget() instanceof IsoZombie zombie) {
            player.setVariable("WalkSpeedGrapple", zombie.isSkeleton() ? 1.2F : 0.8F);
        }

        owner.set(GRUNT_COUNTER, 0);
        if (GameClient.client && player == IsoPlayer.getInstance()) {
            GameClient.instance.sendPlayer(player);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (!owner.isDraggingCorpse() || !owner.isCurrentActionAllowedWhileDraggingCorpses()) {
            owner.StopAllActionQueue();
        }
    }

    private void OnAnimEvent_ActiveAnimFinishing(IsoGameCharacter owner) {
    }

    private void OnAnimEvent_AttackAnim(IsoGameCharacter owner, boolean parameterValue) {
        owner.setPerformingAttackAnimation(parameterValue);
    }

    private void OnAnimEvent_BlockTurn(IsoGameCharacter owner, boolean parameterValue) {
        owner.setIgnoreMovement(parameterValue);
    }

    private void OnAnimEvent_ShoveAnim(IsoGameCharacter owner, boolean parameterValue) {
        owner.setPerformingShoveAnimation(parameterValue);
    }

    private void OnAnimEvent_StompAnim(IsoGameCharacter owner, boolean parameterValue) {
        owner.setPerformingStompAnimation(parameterValue);
    }

    private void OnAnimEvent_GrappleGrabAnim(IsoGameCharacter owner, boolean parameterValue) {
        owner.setPerformingGrappleGrabAnimation(parameterValue);
    }

    private void OnAnimEvent_BlockMovement(IsoGameCharacter owner, AnimEvent event) {
        if (SandboxOptions.instance.attackBlockMovements.getValue()) {
            owner.setVariable("SlowingMovement", Boolean.parseBoolean(event.parameterValue));
        }
    }

    private void OnAnimEvent_ShotDone(IsoGameCharacter owner) {
        HandWeapon weapon = CombatManager.getInstance().getWeapon(owner);
        if (weapon != null && weapon.isRackAfterShoot()) {
            owner.setVariable("ShotDone", true);
        }
    }

    private void OnAnimEvent_SetVariable(IsoGameCharacter owner, AnimationVariableReference variable, String variableValue) {
        if ("ShotDone".equalsIgnoreCase(variable.getName())) {
            HandWeapon weapon = CombatManager.getInstance().getWeapon(owner);
            owner.setVariable("ShotDone", owner.getVariableBoolean("ShotDone") && weapon != null && weapon.isRackAfterShoot());
        }
    }

    private void OnAnimEvent_PlaySwingSound(IsoGameCharacter owner, String swingSoundId) {
        if (IsoPlayer.isLocalPlayer(owner)) {
            if (!owner.getVariableBoolean("PlayedSwingSound")) {
                owner.setVariable("PlayedSwingSound", true);
                OnAnimEvent_PlaySwingSoundAlways(owner, swingSoundId);
            }
        }
    }

    private static void OnAnimEvent_PlaySwingSoundAlways(IsoGameCharacter owner, String swingSoundId) {
        if (IsoPlayer.isLocalPlayer(owner)) {
            HandWeapon weapon = CombatManager.getInstance().getWeapon(owner);
            if (weapon != null) {
                if (!StringUtils.isNullOrWhitespace(swingSoundId)) {
                    String soundName = weapon.getSoundByID(swingSoundId);
                    if (soundName != null) {
                        owner.playSound(soundName);
                        return;
                    }
                }

                owner.playSound(weapon.getSwingSound());
            }
        }
    }

    private void OnAnimEvent_PlayerVoiceSound(IsoGameCharacter owner, String param) {
        if ("CorpseDragging".equalsIgnoreCase(param)) {
            int count = owner.get(GRUNT_COUNTER) + 1;
            owner.set(GRUNT_COUNTER, count);
            if (count < 4) {
                return;
            }

            owner.set(GRUNT_COUNTER, 0);
        }

        OnAnimEvent_PlayerVoiceSoundAlways(owner, param);
    }

    private static void OnAnimEvent_PlayerVoiceSoundAlways(IsoGameCharacter owner, String param) {
        if (owner instanceof IsoPlayer ownerPlayer) {
            ownerPlayer.stopPlayerVoiceSound(param);
            ownerPlayer.playerVoiceSound(param);
        }
    }

    private void OnAnimEvent_SetMeleeDelay(IsoGameCharacter owner, float param) {
        owner.setMeleeDelay(param);
    }

    private void OnAnimEvent_SitGroundStarted(IsoGameCharacter owner) {
        owner.setVariable("SitGroundAnim", "Idle");
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setSprinting(false);
        ((IsoPlayer)owner).setForceSprint(false);
        owner.setIgnoreMovement(false);
        owner.setPerformingShoveAnimation(false);
        owner.setPerformingStompAnimation(false);
        owner.setPerformingGrappleGrabAnimation(false);
        owner.setPerformingAttackAnimation(false);
        owner.setVariable("AimFloorAnim", false);
        ((IsoPlayer)owner).setBlockMovement(false);
        if (owner.isGrappling() && StringUtils.isNullOrEmpty(owner.getGrappleResult())) {
            owner.LetGoOfGrappled("Dropped");
        }
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        if (owner.isLocal()) {
            owner.set(GRAPPLING_TYPE, owner.getSharedGrappleType());
            owner.set(GRAPPLING_TARGET, owner.getGrapplingTarget());
            owner.set(DO_GRAPPLE, owner.isDoGrapple());
            owner.set(DO_CONTINUE_GRAPPLE, owner.isDoContinueGrapple());
            owner.set(IS_THROW_OUT_WINDOW, owner.isGrappleThrowOutWindow());
            owner.set(IS_THROW_OVER_FENCE, owner.isGrappleThrowOverFence());
            owner.set(IS_THROW_INTO_CONTAINER, owner.isGrappleThrowIntoContainer());
            owner.set(GRUNT_COUNTER, 0);
            owner.set(BEARING_FROM_GRAPPLE_TARGET, owner.getVariableFloat("bearingFromGrappledTarget", 0.0F));
        } else {
            owner.setVariable("bearingFromGrappledTarget", owner.get(BEARING_FROM_GRAPPLE_TARGET));
            IGrappleable target = owner.get(GRAPPLING_TARGET);
            String type = owner.get(GRAPPLING_TYPE);
            if (target != null) {
                if (target instanceof IsoGameCharacter corpse) {
                    if (owner.get(THROWN_OUT_WINDOW_OBJ) != null) {
                        GrappledThrownOutWindowState.instance().setParams(corpse, owner.get(THROWN_OUT_WINDOW_OBJ));
                    }

                    if (owner.get(THROWN_OVER_FENCE_DIR) != null) {
                        GrappledThrownOverFenceState.instance().setParams(corpse, owner.getCurrentSquare(), owner.get(THROWN_OVER_FENCE_DIR));
                    }

                    if (owner.get(THROWN_INTO_CONTAINER_OBJ) != null) {
                        GrappledThrownIntoContainerState.instance().setParams(corpse, owner.get(THROWN_INTO_CONTAINER_OBJ));
                    }
                }

                owner.AcceptGrapple(target, type);
                owner.setDoGrapple(owner.get(DO_GRAPPLE));
                owner.setDoContinueGrapple(owner.get(DO_CONTINUE_GRAPPLE));
                owner.setGrappleThrowOutWindow(owner.get(IS_THROW_OUT_WINDOW));
                owner.setGrappleThrowOverFence(owner.get(IS_THROW_OVER_FENCE));
                owner.setGrappleThrowIntoContainer(owner.get(IS_THROW_INTO_CONTAINER));
            } else {
                if (owner.isGrappling()) {
                    owner.LetGoOfGrappled("Aborted");
                }

                owner.resetGrappleStateToDefault("Aborted");
            }
        }

        super.setParams(owner, stage);
    }
}
