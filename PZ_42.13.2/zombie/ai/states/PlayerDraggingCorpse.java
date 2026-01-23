// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.CombatManager;
import zombie.SandboxOptions;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.ui.UIManager;
import zombie.util.StringUtils;

public final class PlayerDraggingCorpse extends State {
    private static final PlayerDraggingCorpse _instance = new PlayerDraggingCorpse();
    private static final Integer PARAM_GRAPPLING_TYPE = 0;
    private static final Integer PARAM_GRAPPLING_TARGET = 1;
    private static final Integer PARAM_DO_GRAPPLE = 2;
    private static final Integer PARAM_DO_CONTINUE_GRAPPLE = 3;
    private static final Integer PARAM_IS_GRAPPLE_WINDOW = 4;
    private static final Integer PARAM_GRUNT_COUNTER = 5;

    public static PlayerDraggingCorpse instance() {
        return _instance;
    }

    public PlayerDraggingCorpse() {
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
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.put(PARAM_GRUNT_COUNTER, 0);
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

    private void OnAnimEvent_AttackAnim(IsoGameCharacter owner, boolean m_ParameterValue) {
        owner.setPerformingAttackAnimation(m_ParameterValue);
    }

    private void OnAnimEvent_BlockTurn(IsoGameCharacter owner, boolean parameterValue) {
        owner.setIgnoreMovement(parameterValue);
    }

    private void OnAnimEvent_ShoveAnim(IsoGameCharacter owner, boolean m_ParameterValue) {
        owner.setPerformingShoveAnimation(m_ParameterValue);
    }

    private void OnAnimEvent_StompAnim(IsoGameCharacter owner, boolean m_ParameterValue) {
        owner.setPerformingStompAnimation(m_ParameterValue);
    }

    private void OnAnimEvent_GrappleGrabAnim(IsoGameCharacter owner, boolean m_ParameterValue) {
        owner.setPerformingGrappleGrabAnimation(m_ParameterValue);
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

    private void OnAnimEvent_PlaySwingSound(IsoGameCharacter owner, String in_swingSoundID) {
        if (IsoPlayer.isLocalPlayer(owner)) {
            if (!owner.getVariableBoolean("PlayedSwingSound")) {
                owner.setVariable("PlayedSwingSound", true);
                OnAnimEvent_PlaySwingSoundAlways(owner, in_swingSoundID);
            }
        }
    }

    private static void OnAnimEvent_PlaySwingSoundAlways(IsoGameCharacter owner, String in_swingSoundID) {
        if (IsoPlayer.isLocalPlayer(owner)) {
            HandWeapon weapon = CombatManager.getInstance().getWeapon(owner);
            if (weapon != null) {
                if (!StringUtils.isNullOrWhitespace(in_swingSoundID)) {
                    String soundName = weapon.getSoundByID(in_swingSoundID);
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
            HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
            int count = (Integer)StateMachineParams.get(PARAM_GRUNT_COUNTER) + 1;
            StateMachineParams.put(PARAM_GRUNT_COUNTER, count);
            if (count < 4) {
                return;
            }

            StateMachineParams.put(PARAM_GRUNT_COUNTER, 0);
        }

        OnAnimEvent_PlayerVoiceSoundAlways(owner, param);
    }

    private static void OnAnimEvent_PlayerVoiceSoundAlways(IsoGameCharacter in_owner, String param) {
        if (in_owner instanceof IsoPlayer ownerPlayer) {
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
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_GRAPPLING_TYPE, owner.getSharedGrappleType());
            StateMachineParams.put(PARAM_GRAPPLING_TARGET, owner.getGrapplingTarget());
            StateMachineParams.put(PARAM_DO_GRAPPLE, owner.isDoGrapple());
            StateMachineParams.put(PARAM_DO_CONTINUE_GRAPPLE, owner.isDoContinueGrapple());
            StateMachineParams.put(PARAM_IS_GRAPPLE_WINDOW, owner.isGrappleThrowOutWindow());
            StateMachineParams.put(PARAM_GRUNT_COUNTER, 0);
        } else {
            IsoGameCharacter target = (IsoGameCharacter)StateMachineParams.getOrDefault(PARAM_GRAPPLING_TARGET, null);
            String type = (String)StateMachineParams.getOrDefault(PARAM_GRAPPLING_TYPE, null);
            if (target != null) {
                owner.AcceptGrapple(target, type);
                owner.setDoGrapple((Boolean)StateMachineParams.getOrDefault(PARAM_DO_GRAPPLE, false));
            } else {
                owner.resetGrappleStateToDefault("");
            }

            owner.setSharedGrappleType(type);
            owner.setDoContinueGrapple((Boolean)StateMachineParams.getOrDefault(PARAM_DO_CONTINUE_GRAPPLE, false));
            owner.setGrappleThrowOutWindow((Boolean)StateMachineParams.getOrDefault(PARAM_IS_GRAPPLE_WINDOW, false));
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
        return false;
    }
}
