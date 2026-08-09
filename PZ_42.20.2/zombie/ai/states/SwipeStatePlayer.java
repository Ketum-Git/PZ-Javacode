// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

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
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableReference;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventBroadcaster;
import zombie.core.skinnedmodel.animation.AnimationTrack;
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
    private static final SwipeStatePlayer INSTANCE = new SwipeStatePlayer();
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
    public static final State.Param<Boolean> LOWER_CONDITION = State.Param.ofBool("lower_condition", false);
    public static final State.Param<Boolean> ATTACKED = State.Param.ofBool("attacked", true);
    public static final State.Param<String> GRAPPLING_TYPE = State.Param.ofString("grappling_type", null);
    public static final State.Param<IGrappleable> GRAPPLING_TARGET = State.Param.of("grappling_target", IGrappleable.class);
    public static final State.Param<Boolean> DO_GRAPPLE = State.Param.ofBool("do_grapple", false);
    public static final State.Param<Boolean> DO_CONTINUE_GRAPPLE = State.Param.ofBool("do_continue_grapple", false);
    public static final State.Param<Boolean> IS_GRAPPLE_WINDOW = State.Param.ofBool("is_grapple_window", false);
    public static final State.Param<Boolean> IS_THROWING = State.Param.ofBool("is_throwing", false);
    private static AnimEventBroadcaster dbgGlobalEventBroadcaster;

    public static SwipeStatePlayer instance() {
        return INSTANCE;
    }

    private SwipeStatePlayer() {
        super(true, false, false, true);
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

    public static void dbgOnGlobalAnimEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (Core.debug) {
            if (!(owner.getCurrentState() instanceof SwipeStatePlayer)) {
                if (dbgGlobalEventBroadcaster == null) {
                    dbgGlobalEventBroadcaster = new AnimEventBroadcaster();
                    dbgGlobalEventBroadcaster.addListener("playRackSound", SwipeStatePlayer::OnAnimEvent_PlayRackSound);
                    dbgGlobalEventBroadcaster.addListener("playClickSound", SwipeStatePlayer::OnAnimEvent_PlayClickSound);
                    dbgGlobalEventBroadcaster.addListener("PlaySwingSound", SwipeStatePlayer::OnAnimEvent_PlaySwingSoundAlways);
                    dbgGlobalEventBroadcaster.addListener("PlayerVoiceSound", SwipeStatePlayer::OnAnimEvent_PlayerVoiceSoundAlways);
                }

                DebugType.Animation.trace("Received anim event: %s", event);
                dbgGlobalEventBroadcaster.animEvent(owner, layer, track, event);
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

    private void doAttack(IsoPlayer ownerPlayer, float chargeDelta, String clickSound, AttackVars vars) {
        ownerPlayer.setClickSound(clickSound);
        ownerPlayer.useChargeDelta = Math.min(chargeDelta, 90.0F) / 25.0F;
        InventoryItem attackItem = ownerPlayer.getPrimaryHandItem();
        if (!(attackItem instanceof HandWeapon) || vars.doShove || vars.doGrapple) {
            attackItem = ownerPlayer.bareHands;
        }

        if (attackItem instanceof HandWeapon handWeapon) {
            ownerPlayer.setUseHandWeapon(handWeapon);
            if (ownerPlayer.playerIndex == 0
                && ownerPlayer.getJoypadBind() == -1
                && UIManager.getPicked() != null
                && (!GameClient.client || ownerPlayer.isLocalPlayer())) {
                if (UIManager.getPicked().tile instanceof IsoMovingObject isoMovingObject) {
                    ownerPlayer.setAttackTargetSquare(isoMovingObject.getCurrentSquare());
                } else {
                    ownerPlayer.setAttackTargetSquare(UIManager.getPicked().square);
                }
            }

            if (ownerPlayer.isLocalPlayer() && ownerPlayer.getJoypadBind() != -1) {
                ownerPlayer.setAttackTargetSquare(ownerPlayer.getCurrentSquare());
            }

            ownerPlayer.setRecoilDelay(vars.recoilDelay);
            CombatManager.getInstance().setAimingDelay(ownerPlayer, handWeapon);
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

        owner.set(LOWER_CONDITION, false);
        owner.set(ATTACKED, false);
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
            if (isStompAnim && isStompingDisabled(owner, true)) {
                isStompAnim = false;
            }

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
        IsoPlayer ownerPlayer = Type.tryCastTo(owner, IsoPlayer.class);
        boolean lowerCondition = owner.get(LOWER_CONDITION);
        if (lowerCondition) {
            owner.set(LOWER_CONDITION, false);
            HandWeapon weapon = CombatManager.getInstance().getWeapon(owner);
            WeaponLowerConditionEvent(weapon, owner);
        }
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

    private void OnAnimEvent_AttackCollisionCheck(IsoGameCharacter owner, AttackType attackTypeModifier) {
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

        if (!owner.get(ATTACKED) && IsoPlayer.isLocalPlayer(owner)) {
            DebugType.Combat.debugln("AttackType: %s", attackTypeModifier.toString());
            CombatManager.getInstance().attackCollisionCheck(owner, weapon, this, attackTypeModifier);
        }
    }

    private void OnAnimEvent_GrappleGrabCollisionCheck(IsoGameCharacter owner, String grappleType) {
        if (IsoPlayer.isLocalPlayer(owner)) {
            HandWeapon weapon = CombatManager.getInstance().getWeapon(owner);
            if (!owner.get(ATTACKED)) {
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
        if (!owner.getVariableBoolean("PlayerVoiceSound")) {
            owner.setVariable("PlayerVoiceSound", true);
            OnAnimEvent_PlayerVoiceSoundAlways(owner, param);
        }
    }

    private static void OnAnimEvent_PlayerVoiceSoundAlways(IsoGameCharacter owner, String param) {
        if (owner instanceof IsoPlayer ownerPlayer) {
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
                    shoes.reduceCondition();
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
        boolean attacked = owner.get(ATTACKED);
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

    private void GrappleGrabCollisionCheck(IsoGameCharacter owner, HandWeapon weapon, String grappleType) {
        if (!(owner instanceof IsoLivingCharacter ownerLiving)) {
            DebugType.Grapple.warn("GrappleGrabCollisionCheck. Failed. Character is not an IsoLivingCharacter.");
        } else if (!owner.isPerformingGrappleGrabAnimation()) {
            DebugType.Grapple.warn("GrappleGrabCollisionCheck. Failed. Character isPerformingGrappleGrabAnimation returned FALSE.");
        } else {
            if (GameServer.server) {
                DebugType.Grapple.println("GrappleGrabCollisionCheck.");
            }

            LuaEventManager.triggerEvent("GrappleGrabCollisionCheck", owner, weapon);
            owner.getAttackVars().setWeapon(weapon);
            owner.getAttackVars().targetOnGround.set(ownerLiving.targetOnGround);
            owner.getAttackVars().aimAtFloor = owner.isAimAtFloor();
            owner.getAttackVars().doShove = false;
            owner.getAttackVars().doGrapple = true;
            CombatManager.getInstance().calculateHitInfoList(owner);
            if (DebugType.Grapple.isEnabled()) {
                DebugType.Grapple.debugln("HitList: ");
                DebugType.Grapple.debugln("{");

                for (HitInfo hitInfo : owner.getHitInfoList()) {
                    DebugType.Grapple.debugln("\t%s", hitInfo.getDescription());
                }

                DebugType.Grapple.debugln("} // HitList end. ");
            }

            int hitCount = owner.getHitInfoList().size();
            owner.setLastHitCount(hitCount);
            if (hitCount == 0) {
                DebugType.Grapple.println("GrappleGrabCollisionCheck. Missed.");
            } else {
                DebugType.Grapple.println("GrappleGrabCollisionCheck. Hit.");
                DebugType.Grapple.println("{");
                HitInfo grappledTargetHitInfo = null;
                IsoGameCharacter grappledCharacter = null;
                IsoDeadBody grappledBody = null;

                for (int i = 0; i < hitCount; i++) {
                    HitInfo hitInfo = owner.getHitInfoList().get(i);
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
                    DebugType.Grapple.println("    No grapple-able characters found.");
                    DebugType.Grapple.println("}");
                } else {
                    DebugType.Grapple.println("    Grapple target found: %s", grappledTargetHitInfo.getDescription());
                    DebugType.Grapple.println("}");
                    IsoPlayer ownerPlayer = Type.tryCastTo(owner, IsoPlayer.class);
                    float grappleEffectiveness = owner.calculateGrappleEffectivenessFromTraits();
                    if (ownerPlayer.isLocalPlayer() || owner.isNpc()) {
                        boolean corpsesOnly = grappleType.endsWith("_CorpseOnly");
                        if (grappledCharacter != null && !corpsesOnly) {
                            grappledCharacter.Grappled(ownerLiving, weapon, grappleEffectiveness, grappleType);
                        } else if (grappledBody != null) {
                            grappledBody.Grappled(ownerLiving, weapon, grappleEffectiveness, grappleType);
                        }
                    }

                    owner.set(LOWER_CONDITION, false);
                    owner.set(ATTACKED, true);
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
        if (owner.isLocal()) {
            owner.set(GRAPPLING_TYPE, owner.getSharedGrappleType());
            owner.set(GRAPPLING_TARGET, owner.getGrapplingTarget());
            owner.set(DO_GRAPPLE, owner.isDoGrapple());
            owner.set(DO_CONTINUE_GRAPPLE, owner.isDoContinueGrapple());
            owner.set(IS_GRAPPLE_WINDOW, owner.isGrappleThrowOutWindow());
            if (owner.getUseHandWeapon() != null && owner instanceof IsoPlayer isoPlayer && isoPlayer.isInitiateAttack() && isoPlayer.isAttackStarted()) {
                owner.set(IS_THROWING, owner.getUseHandWeapon().getPhysicsObject() != null);
            }
        } else {
            IGrappleable target = owner.get(GRAPPLING_TARGET);
            if (target != null) {
                owner.AcceptGrapple(target, owner.get(GRAPPLING_TYPE));
                owner.setDoGrapple(owner.get(DO_GRAPPLE));
            } else {
                owner.resetGrappleStateToDefault("");
            }

            owner.setDoContinueGrapple(owner.get(DO_CONTINUE_GRAPPLE));
            owner.setGrappleThrowOutWindow(owner.get(IS_GRAPPLE_WINDOW));
            if (owner instanceof IsoPlayer isoPlayer) {
                isoPlayer.setInitiateAttack(owner.get(IS_THROWING));
                isoPlayer.setAttackStarted(owner.get(IS_THROWING));
            }
        }

        super.setParams(owner, stage);
    }

    public static boolean isStompingDisabled(IsoGameCharacter owner, boolean doShove) {
        return !owner.isManualFloorAtkButtonDown() && !owner.isMeleeButtonDown() && !Core.getInstance().isOptionAutoProneAtk() && doShove;
    }
}
