// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.AttackType;
import zombie.CombatManager;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaHookManager;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventBroadcaster;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.fields.hit.AttackVars;
import zombie.network.fields.hit.HitInfo;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemTag;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public final class SwipeStatePlayer extends State {
    private static final float ShoveChargeDeltaMultiplier = 2.0F;
    private static final float MaxStartChargeDelta = 90.0F;
    private static final float ChargeDeltaModifier = 25.0F;
    private static final float ShoveRecoilDelay = 10.0F;
    private static final float WeaponEmptyRecoilDelay = 10.0F;
    private static final int BaseAimingDelay = 10;
    private static final float DefaultChargeDelta = 2.0F;
    private static final float BreakMultiplierBase = 1.0F;
    private static final float BreakMultiplerChargeModifier = 1.5F;
    private static final float DefaultMaintenanceXP = 1.0F;
    private static final int ConditionLowerChance = 10;
    private static final int FootDamageBaseRange = 10;
    private static final int NoShoesFootDamageBaseRange = 3;
    private static final float AutoShootSpeed = 6.4F;
    private static final float DefaultAutoShootSpeed = 1.0F;
    private static final float MinimumSingleShootSpeed = 0.5F;
    private static final float SingleShootSpeedBase = 0.8F;
    public static final float MaxStompDistance = 0.6F;
    private static final SwipeStatePlayer _instance = new SwipeStatePlayer();
    private static final Integer PARAM_LOWER_CONDITION = 0;
    private static final Integer PARAM_ATTACKED = 1;
    private static final Integer PARAM_GRAPPLING_TYPE = 2;
    private static final Integer PARAM_GRAPPLING_TARGET = 3;
    private static final Integer PARAM_DO_GRAPPLE = 4;
    private static final Integer PARAM_DO_CONTINUE_GRAPPLE = 5;
    private static final Integer PARAM_IS_GRAPPLE_WINDOW = 6;
    private static AnimEventBroadcaster dbgGlobalEventBroadcaster;

    public static SwipeStatePlayer instance() {
        return _instance;
    }

    public SwipeStatePlayer() {
        this.addAnimEventListener("ActiveAnimFinishing", this::OnAnimEvent_ActiveAnimFinishing);
        this.addAnimEventListener("NonLoopedAnimFadeOut", this::OnAnimEvent_ActiveAnimFinishing);
        this.addAnimEventListener("AttackAnim", this::OnAnimEvent_AttackAnim);
        this.addAnimEventListener("BlockTurn", this::OnAnimEvent_BlockTurn);
        this.addAnimEventListener("ShoveAnim", this::OnAnimEvent_ShoveAnim);
        this.addAnimEventListener("StompAnim", this::OnAnimEvent_StompAnim);
        this.addAnimEventListener("GrappleGrabAnim", this::OnAnimEvent_GrappleGrabAnim);
        this.addAnimEventListener("AttackCollisionCheck", this::OnAnimEvent_AttackCollisionCheck, AttackType.NONE);
        this.addAnimEventListener("GrappleGrabCollisionCheck", this::OnAnimEvent_GrappleGrabCollisionCheck);
        this.addAnimEventListener("BlockMovement", this::OnAnimEvent_BlockMovement);
        this.addAnimEventListener("WeaponEmptyCheck", this::OnAnimEvent_WeaponEmptyCheck);
        this.addAnimEventListener("ShotDone", this::OnAnimEvent_ShotDone);
        this.addAnimEventListener(this::OnAnimEvent_SetVariable);
        this.addAnimEventListener("SetMeleeDelay", this::OnAnimEvent_SetMeleeDelay);
        this.addAnimEventListener("playRackSound", SwipeStatePlayer::OnAnimEvent_PlayRackSound);
        this.addAnimEventListener("playClickSound", SwipeStatePlayer::OnAnimEvent_PlayClickSound);
        this.addAnimEventListener("PlaySwingSound", this::OnAnimEvent_PlaySwingSound);
        this.addAnimEventListener("PlayerVoiceSound", this::OnAnimEvent_PlayerVoiceSound);
        this.addAnimEventListener("PistolWhipAnim", this::OnAnimEvent_PistolWhipAnim);
        this.addAnimEventListener("SitGroundStarted", this::OnAnimEvent_SitGroundStarted);
    }

    public static void dbgOnGlobalAnimEvent(IsoGameCharacter in_owner, AnimLayer in_layer, AnimationTrack in_track, AnimEvent in_event) {
        if (Core.debug) {
            if (!(in_owner.getCurrentState() instanceof SwipeStatePlayer)) {
                if (dbgGlobalEventBroadcaster == null) {
                    dbgGlobalEventBroadcaster = new AnimEventBroadcaster();
                    dbgGlobalEventBroadcaster.addListener("playRackSound", SwipeStatePlayer::OnAnimEvent_PlayRackSound);
                    dbgGlobalEventBroadcaster.addListener("playClickSound", SwipeStatePlayer::OnAnimEvent_PlayClickSound);
                    dbgGlobalEventBroadcaster.addListener("PlaySwingSound", SwipeStatePlayer::OnAnimEvent_PlaySwingSoundAlways);
                    dbgGlobalEventBroadcaster.addListener("PlayerVoiceSound", SwipeStatePlayer::OnAnimEvent_PlayerVoiceSoundAlways);
                }

                DebugLog.Animation.trace("Received anim event: %s", in_event);
                dbgGlobalEventBroadcaster.animEvent(in_owner, in_layer, in_track, in_event);
            }
        }
    }

    private static void WeaponLowerConditionEvent(HandWeapon weapon, IsoGameCharacter owner) {
        if (weapon.getCondition() <= 0) {
            IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
            if (player != null && (!player.getAttackVars().targetsStanding.isEmpty() || !player.getAttackVars().targetsProne.isEmpty())) {
                player.triggerMusicIntensityEvent("WeaponBreaksDuringCombat");
            }
        }
    }

    private void doAttack(IsoPlayer in_ownerPlayer, float chargeDelta, String clickSound, AttackVars vars) {
        in_ownerPlayer.setClickSound(clickSound);
        in_ownerPlayer.useChargeDelta = Math.min(chargeDelta, 90.0F) / 25.0F;
        InventoryItem attackItem = in_ownerPlayer.getPrimaryHandItem();
        if (!(attackItem instanceof HandWeapon) || vars.doShove || vars.doGrapple) {
            attackItem = in_ownerPlayer.bareHands;
        }

        if (attackItem instanceof HandWeapon handWeapon) {
            in_ownerPlayer.setUseHandWeapon(handWeapon);
            if (in_ownerPlayer.playerIndex == 0
                && in_ownerPlayer.joypadBind == -1
                && UIManager.getPicked() != null
                && (!GameClient.client || in_ownerPlayer.isLocalPlayer())) {
                if (UIManager.getPicked().tile instanceof IsoMovingObject isoMovingObject) {
                    in_ownerPlayer.setAttackTargetSquare(isoMovingObject.getCurrentSquare());
                } else {
                    in_ownerPlayer.setAttackTargetSquare(UIManager.getPicked().square);
                }
            }

            in_ownerPlayer.setRecoilDelay(vars.recoilDelay);
            CombatManager.getInstance().setAimingDelay(in_ownerPlayer, handWeapon);
        }
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoPlayer player = (IsoPlayer)owner;
        if ("HitReaction".equals(player.getHitReaction())) {
            player.clearVariable("HitReaction");
        }

        player.setInitiateAttack(false);
        if (!GameServer.server) {
            UIManager.speedControls.SetCurrentGameSpeed(1);
        }

        HashMap<Object, Object> StateMachineParams = player.getStateMachineParams(this);
        StateMachineParams.put(PARAM_LOWER_CONDITION, Boolean.FALSE);
        StateMachineParams.put(PARAM_ATTACKED, Boolean.FALSE);
        CombatManager.getInstance().calculateAttackVars(player);
        this.doAttack(player, 2.0F, player.getClickSound(), player.getAttackVars());
        HandWeapon weapon = player.getUseHandWeapon();
        if (weapon != null) {
            if (!player.remote) {
                player.setRecoilVarY(0.0F);
                float recoilDelay = weapon.getRecoilDelay();
                float currentDelay = weapon.getRecoilDelay(player);
                if (currentDelay < recoilDelay && recoilDelay != 0.0F) {
                    float recoilVarX = 1.0F - currentDelay / recoilDelay;
                    player.setRecoilVarX(recoilVarX);
                } else {
                    player.setRecoilVarX(0.0F);
                }
            }

            if ("Auto".equals(player.getFireMode())) {
                player.setVariable("autoShootSpeed", weapon.getCyclicRateMultiplier() > 0.0F ? weapon.getCyclicRateMultiplier() : 1.0F);
                player.setVariable("autoShootVarY", 0.0F);
                player.setVariable("autoShootVarX", 1.0F);
            } else {
                owner.setVariable("singleShootSpeed", PZMath.max(0.5F, 0.8F + (1.0F - (float)weapon.getRecoilDelay(owner) / weapon.getRecoilDelay())));
            }
        }

        player.setVariable("ShotDone", false);
        player.setPerformingShoveAnimation(false);
        player.setPerformingGrappleGrabAnimation(false);
        boolean aimAtFloorAnim = player.getAttackVars().aimAtFloor;
        boolean isStompAnim = false;
        LuaEventManager.triggerEvent("OnWeaponSwing", player, weapon);
        if (LuaHookManager.TriggerHook("WeaponSwing", player, weapon)) {
            player.getStateMachine().revertToPreviousState(this);
        }

        player.StopAllActionQueue();
        if (player.isLocalPlayer()) {
            IsoWorld.instance.currentCell.setDrag(null, player.playerIndex);
        }

        weapon = player.getAttackVars().getWeapon(player);
        player.setAimAtFloor(player.getAttackVars().aimAtFloor);
        boolean bDoShove = player.isDoShove();
        player.setDoShove(player.getAttackVars().doShove);
        player.setPerformingGrappleGrabAnimation(player.getAttackVars().doGrapple);
        player.useChargeDelta = player.getAttackVars().useChargeDelta;
        player.targetOnGround = (IsoGameCharacter)player.getAttackVars().targetOnGround.getObject();
        if (!player.isDoShove() && !bDoShove && !weapon.isRanged() && player.isLocalPlayer()) {
            player.clearVariable("PlayedSwingSound");
        } else if ((player.isDoShove() || bDoShove) && player.isLocalPlayer() && !player.isGrappling()) {
            isStompAnim = player.targetOnGround != null && player.getAttackVars().targetDistance < 0.6F;
            if (isStompAnim) {
                player.playSound("AttackStomp");
            } else {
                player.playSound("AttackShove");
            }
        }

        player.clearVariable("PistolWhipAnim");
        player.setShoveStompAnim(isStompAnim);
        if (!GameClient.client || player.isLocalPlayer()) {
            player.setVariable("AimFloorAnim", aimAtFloorAnim);
        }

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
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        IsoPlayer ownerPlayer = Type.tryCastTo(owner, IsoPlayer.class);
        boolean lowerCondition = StateMachineParams.get(PARAM_LOWER_CONDITION) == Boolean.TRUE;
        if (lowerCondition) {
            StateMachineParams.put(PARAM_LOWER_CONDITION, Boolean.FALSE);
            HandWeapon weapon = CombatManager.getInstance().getWeapon(owner);
            WeaponLowerConditionEvent(weapon, owner);
        }
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

    private void OnAnimEvent_AttackCollisionCheck(IsoGameCharacter owner, AttackType in_attackTypeModifier) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        HandWeapon weapon = CombatManager.getInstance().getWeapon(owner);
        if (weapon.hasTag(ItemTag.FAKE_WEAPON)) {
            if (owner.isUnlimitedAmmo()) {
                return;
            }

            if (weapon.getCurrentAmmoCount() > 0) {
                weapon.setCurrentAmmoCount(weapon.getCurrentAmmoCount() - 1);
                return;
            }
        }

        if (StateMachineParams.get(PARAM_ATTACKED) == Boolean.FALSE && IsoPlayer.isLocalPlayer(owner)) {
            DebugType.Combat.debugln("AttackType: %s", in_attackTypeModifier.toString());
            CombatManager.getInstance().attackCollisionCheck(owner, weapon, this, in_attackTypeModifier);
        }
    }

    private void OnAnimEvent_GrappleGrabCollisionCheck(IsoGameCharacter owner, String grappleType) {
        if (IsoPlayer.isLocalPlayer(owner)) {
            HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
            HandWeapon weapon = CombatManager.getInstance().getWeapon(owner);
            if (StateMachineParams.get(PARAM_ATTACKED) == Boolean.FALSE) {
                this.GrappleGrabCollisionCheck(owner, weapon, grappleType);
            }
        }
    }

    private void OnAnimEvent_BlockMovement(IsoGameCharacter owner, AnimEvent event) {
        if (SandboxOptions.instance.attackBlockMovements.getValue()) {
            owner.setVariable("SlowingMovement", Boolean.parseBoolean(event.parameterValue));
        }
    }

    private void OnAnimEvent_WeaponEmptyCheck(IsoGameCharacter owner) {
        if (owner.getClickSound() != null) {
            if (!IsoPlayer.isLocalPlayer(owner)) {
                return;
            }

            owner.playSound(owner.getClickSound());
            owner.setRecoilDelay(10.0F);
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

    private static void OnAnimEvent_PlayRackSound(IsoGameCharacter owner) {
        HandWeapon weapon = CombatManager.getInstance().getWeapon(owner);
        if (IsoPlayer.isLocalPlayer(owner)) {
            owner.playSound(weapon.getRackSound());
        }
    }

    private static void OnAnimEvent_PlayClickSound(IsoGameCharacter owner) {
        HandWeapon weapon = CombatManager.getInstance().getWeapon(owner);
        if (IsoPlayer.isLocalPlayer(owner)) {
            owner.playSound(weapon.getClickSound());
            checkRangedWeaponFailedToShoot(owner);
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
        if (!owner.getVariableBoolean("PlayerVoiceSound")) {
            owner.setVariable("PlayerVoiceSound", true);
            OnAnimEvent_PlayerVoiceSoundAlways(owner, param);
        }
    }

    private static void OnAnimEvent_PlayerVoiceSoundAlways(IsoGameCharacter in_owner, String param) {
        if (in_owner instanceof IsoPlayer ownerPlayer) {
            ownerPlayer.stopPlayerVoiceSound(param);
            ownerPlayer.playerVoiceSound(param);
        }
    }

    private void OnAnimEvent_PistolWhipAnim(IsoGameCharacter owner, String param) {
        owner.setVariable("PistolWhipAnim", StringUtils.tryParseBoolean(param));
    }

    private void OnAnimEvent_SetMeleeDelay(IsoGameCharacter owner, float param) {
        owner.setMeleeDelay(param);
    }

    private void OnAnimEvent_SitGroundStarted(IsoGameCharacter owner) {
        owner.setVariable("SitGroundAnim", "Idle");
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        owner.setSprinting(false);
        ((IsoPlayer)owner).setForceSprint(false);
        owner.setIgnoreMovement(false);
        owner.setPerformingShoveAnimation(false);
        owner.setPerformingStompAnimation(false);
        owner.setPerformingGrappleGrabAnimation(false);
        owner.setPerformingAttackAnimation(false);
        owner.setShoveStompAnim(false);
        owner.setVariable("AimFloorAnim", false);
        ((IsoPlayer)owner).setBlockMovement(false);
        if (owner.isAimAtFloor() && ((IsoLivingCharacter)owner).isDoShove()) {
            Clothing shoes = (Clothing)owner.getWornItem(ItemBodyLocation.SHOES);
            int randFootDmg = 10;
            if (shoes == null) {
                randFootDmg = 3;
            } else {
                randFootDmg += shoes.getConditionLowerChance() / 2;
                if (Rand.Next(shoes.getConditionLowerChance()) == 0) {
                    shoes.setCondition(shoes.getCondition() - 1);
                }
            }

            if (Rand.Next(randFootDmg) == 0) {
                if (shoes == null) {
                    owner.getBodyDamage().getBodyPart(BodyPartType.Foot_R).AddDamage(Rand.Next(5, 10));
                    owner.getBodyDamage()
                        .getBodyPart(BodyPartType.Foot_R)
                        .setAdditionalPain(owner.getBodyDamage().getBodyPart(BodyPartType.Foot_R).getAdditionalPain() + Rand.Next(5, 10));
                } else {
                    owner.getBodyDamage().getBodyPart(BodyPartType.Foot_R).AddDamage(Rand.Next(1, 5));
                    owner.getBodyDamage()
                        .getBodyPart(BodyPartType.Foot_R)
                        .setAdditionalPain(owner.getBodyDamage().getBodyPart(BodyPartType.Foot_R).getAdditionalPain() + Rand.Next(1, 5));
                }
            }
        }

        HandWeapon weapon = CombatManager.getInstance().getWeapon(owner);
        owner.clearVariable("ZombieHitReaction");
        ((IsoPlayer)owner).setAttackStarted(false);
        ((IsoPlayer)owner).setAttackType(AttackType.NONE);
        ((IsoLivingCharacter)owner).setDoShove(false);
        owner.setDoGrapple(false);
        owner.clearVariable("RackWeapon");
        owner.clearVariable("PlayedSwingSound");
        owner.clearVariable("PlayerVoiceSound");
        owner.clearVariable("PistolWhipAnim");
        owner.clearVariable("SlowingMovement");
        boolean attacked = StateMachineParams.get(PARAM_ATTACKED) == Boolean.TRUE;
        if (weapon != null && (weapon.getCondition() <= 0 || attacked && weapon.isUseSelf())) {
            owner.removeFromHands(weapon);
            owner.getInventory().setDrawDirty(true);
        }

        if (owner.isRangedWeaponEmpty()) {
            owner.setRecoilDelay(10.0F);
        }

        owner.setRangedWeaponEmpty(false);
        owner.setClickSound(null);
        if (attacked) {
            LuaEventManager.triggerEvent("OnPlayerAttackFinished", owner, weapon);
        }

        if (GameClient.client && !owner.isLocal() && weapon != null && weapon.getPhysicsObject() != null) {
            owner.setPrimaryHandItem(null);
        }
    }

    private void GrappleGrabCollisionCheck(IsoGameCharacter in_owner, HandWeapon in_weapon, String in_grappleType) {
        HashMap<Object, Object> StateMachineParams = in_owner.getStateMachineParams(this);
        if (!(in_owner instanceof IsoLivingCharacter ownerLiving)) {
            DebugLog.Grapple.warn("GrappleGrabCollisionCheck. Failed. Character is not an IsoLivingCharacter.");
        } else if (!in_owner.isPerformingGrappleGrabAnimation()) {
            DebugLog.Grapple.warn("GrappleGrabCollisionCheck. Failed. Character isPerformingGrappleGrabAnimation returned FALSE.");
        } else {
            if (GameServer.server) {
                DebugLog.Grapple.println("GrappleGrabCollisionCheck.");
            }

            LuaEventManager.triggerEvent("GrappleGrabCollisionCheck", in_owner, in_weapon);
            in_owner.getAttackVars().setWeapon(in_weapon);
            in_owner.getAttackVars().targetOnGround.set(ownerLiving.targetOnGround);
            in_owner.getAttackVars().aimAtFloor = in_owner.isAimAtFloor();
            in_owner.getAttackVars().doShove = false;
            in_owner.getAttackVars().doGrapple = true;
            CombatManager.getInstance().calculateHitInfoList(in_owner);
            if (DebugLog.Grapple.isEnabled()) {
                DebugLog.Grapple.debugln("HitList: ");
                DebugLog.Grapple.debugln("{");

                for (HitInfo hitInfo : in_owner.getHitInfoList()) {
                    DebugLog.Grapple.debugln("\t%s", hitInfo.getDescription());
                }

                DebugLog.Grapple.debugln("} // HitList end. ");
            }

            int hitCount = in_owner.getHitInfoList().size();
            in_owner.setLastHitCount(hitCount);
            if (hitCount == 0) {
                DebugLog.Grapple.println("GrappleGrabCollisionCheck. Missed.");
            } else {
                DebugLog.Grapple.println("GrappleGrabCollisionCheck. Hit.");
                DebugLog.Grapple.println("{");
                HitInfo grappledTargetHitInfo = null;
                IsoGameCharacter grappledCharacter = null;
                IsoDeadBody grappledBody = null;

                for (int i = 0; i < hitCount; i++) {
                    HitInfo hitInfo = in_owner.getHitInfoList().get(i);
                    IsoMovingObject grappledObject = hitInfo.getObject();
                    grappledCharacter = Type.tryCastTo(grappledObject, IsoGameCharacter.class);
                    if (grappledCharacter != null) {
                        grappledTargetHitInfo = hitInfo;
                        break;
                    }

                    if (grappledBody == null) {
                        grappledBody = Type.tryCastTo(grappledObject, IsoDeadBody.class);
                        if (grappledBody != null) {
                            grappledTargetHitInfo = hitInfo;
                        }
                    }
                }

                if (grappledTargetHitInfo == null) {
                    DebugLog.Grapple.println("    No grapple-able characters found.");
                    DebugLog.Grapple.println("}");
                } else {
                    DebugLog.Grapple.println("    Grapple target found: %s", grappledTargetHitInfo.getDescription());
                    DebugLog.Grapple.println("}");
                    IsoPlayer ownerPlayer = Type.tryCastTo(in_owner, IsoPlayer.class);
                    float grappleEffectiveness = in_owner.calculateGrappleEffectivenessFromTraits();
                    if (ownerPlayer.isLocalPlayer() || in_owner.isNPC()) {
                        boolean corpsesOnly = in_grappleType.endsWith("_CorpseOnly");
                        if (grappledCharacter != null && !corpsesOnly) {
                            grappledCharacter.Grappled(ownerLiving, in_weapon, grappleEffectiveness, in_grappleType);
                        } else if (grappledBody != null) {
                            grappledBody.Grappled(ownerLiving, in_weapon, grappleEffectiveness, in_grappleType);
                        }
                    }

                    StateMachineParams.put(PARAM_LOWER_CONDITION, Boolean.FALSE);
                    StateMachineParams.put(PARAM_ATTACKED, Boolean.TRUE);
                }
            }
        }
    }

    private void changeWeapon(HandWeapon weapon, IsoGameCharacter owner) {
        if (weapon != null && weapon.isUseSelf()) {
            owner.getInventory().setDrawDirty(true);

            for (InventoryItem item : owner.getInventory().getItems()) {
                if (item != weapon && item instanceof HandWeapon && item.getType() == weapon.getType() && item.getCondition() > 0) {
                    if (owner.getPrimaryHandItem() == weapon && owner.getSecondaryHandItem() == weapon) {
                        owner.setPrimaryHandItem(item);
                        owner.setSecondaryHandItem(item);
                    } else if (owner.getPrimaryHandItem() == weapon) {
                        owner.setPrimaryHandItem(item);
                    } else if (owner.getSecondaryHandItem() == weapon) {
                        owner.setSecondaryHandItem(item);
                    }

                    return;
                }
            }
        }

        if (weapon == null || weapon.getCondition() <= 0 || weapon.isUseSelf()) {
            HandWeapon weap = (HandWeapon)owner.getInventory().getBestWeapon(owner.getDescriptor());
            owner.setPrimaryHandItem(null);
            if (owner.getSecondaryHandItem() == weapon) {
                owner.setSecondaryHandItem(null);
            }

            if (weap != null && weap != owner.getPrimaryHandItem() && weap.getCondition() > 0) {
                owner.setPrimaryHandItem(weap);
                if (weap.isTwoHandWeapon() && owner.getSecondaryHandItem() == null) {
                    owner.setSecondaryHandItem(weap);
                }
            }
        }
    }

    private static void checkRangedWeaponFailedToShoot(IsoGameCharacter owner) {
        if (!GameServer.server) {
            IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
            if (player != null && player.isLocalPlayer()) {
                int numZombies = player.getStats().musicZombiesTargetingNearbyMoving;
                numZombies += player.getStats().musicZombiesTargetingNearbyNotMoving;
                if (numZombies > 0) {
                    player.triggerMusicIntensityEvent("RangedWeaponFailedToShoot");
                }
            }
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
        } else {
            IsoGameCharacter target = (IsoGameCharacter)StateMachineParams.getOrDefault(PARAM_GRAPPLING_TARGET, null);
            if (target != null) {
                owner.AcceptGrapple(target, (String)StateMachineParams.getOrDefault(PARAM_GRAPPLING_TYPE, null));
                owner.setDoGrapple((Boolean)StateMachineParams.getOrDefault(PARAM_DO_GRAPPLE, false));
            } else {
                owner.resetGrappleStateToDefault("");
            }

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
        return false;
    }

    @Override
    public boolean isSyncOnSquare() {
        return false;
    }

    @Override
    public boolean isSyncInIdle() {
        return true;
    }
}
