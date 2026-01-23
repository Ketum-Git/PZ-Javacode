// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import java.util.ArrayList;
import java.util.stream.Collectors;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.skills.PerkFactory;
import zombie.combat.CombatConfig;
import zombie.combat.CombatConfigKey;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.physics.BallisticsController;
import zombie.core.physics.RagdollBodyPart;
import zombie.debug.BaseDebugWindow;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.iso.IsoWorld;
import zombie.network.fields.hit.HitInfo;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemType;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayList;

public class FirearmPanel extends BaseDebugWindow {
    private final CombatManager combatManager = CombatManager.getInstance();
    private final CombatConfig combatConfig = this.combatManager.getCombatConfig();
    private String selectedNode = "";
    private String selectedAttachment = "";
    private final int defaultflags = 192;
    private final int selectedflags = 193;
    private boolean disablePanic;
    private final boolean renderAim = false;
    private ArrayList<WeaponPart> attachments = new ArrayList<>();

    @Override
    public String getTitle() {
        return "Firearm Debug";
    }

    @Override
    public int getWindowFlags() {
        return 64;
    }

    boolean doTreeNode(String id, String label, boolean leaf, boolean defaultOpen) {
        boolean selected = id.equals(this.selectedNode);
        int flags = selected ? 193 : 192;
        if (leaf) {
            flags |= 256;
        }

        if (defaultOpen) {
            flags |= 32;
        }

        boolean t = ImGui.treeNodeEx(id, flags, label);
        if (ImGui.isItemClicked()) {
            this.selectedNode = id;
        }

        return t;
    }

    @Override
    protected void doWindowContents() {
        if (this.attachments.isEmpty()) {
            this.attachments = ScriptManager.instance
                .getAllItems()
                .stream()
                .filter(i -> i.isItemType(ItemType.WEAPON_PART))
                .map(i -> InventoryItemFactory.CreateItem(i.getFullName()))
                .collect(Collectors.toCollection(ArrayList::new));
        }

        if (IsoWorld.instance.currentCell != null && IsoPlayer.getInstance() != null) {
            Wrappers.checkbox("Aim Toggling", Core.getInstance()::isToggleToAim, Core.getInstance()::setToggleToAim);
            Wrappers.checkbox("Aim Panning", Core.getInstance()::getOptionPanCameraWhileAiming, Core.getInstance()::setOptionPanCameraWhileAiming);
            this.doSettingTweaks();
            this.doPlayerTweaks(IsoPlayer.getInstance());
            this.doWeaponTweaks(IsoPlayer.getInstance(), IsoPlayer.getInstance().getUseHandWeapon());
            this.doTargetInfo(IsoPlayer.getInstance(), IsoPlayer.getInstance().getUseHandWeapon());
        }
    }

    private void doSettingTweaks() {
        if (this.doTreeNode("gunplaySettings", "Global Settings", false, false)) {
            ImGui.text("General");
            Wrappers.sliderDouble(
                "Noise", 0.3F, 2.0, SandboxOptions.instance.firearmNoiseMultiplier::getValue, SandboxOptions.instance.firearmNoiseMultiplier::setValue
            );
            this.combatConfig
                .set(
                    CombatConfigKey.FIREARM_RECOIL_MUSCLE_STRAIN_MODIFIER,
                    PZMath.roundFloat(
                        Wrappers.sliderFloat("Firearm Muscle Strain", this.combatConfig.get(CombatConfigKey.FIREARM_RECOIL_MUSCLE_STRAIN_MODIFIER), 0.0F, 0.1F),
                        4
                    )
                );
            ImGui.treePop();
        }
    }

    private void doPlayerTweaks(IsoPlayer player) {
        if (this.doTreeNode("gunplayPlayer", "Player", false, false)) {
            Wrappers.sliderInt("Strength", 0, 10, player::getPerkLevel, player::setPerkLevelDebug, PerkFactory.Perks.Strength);
            Wrappers.sliderInt("Aiming", 0, 10, player::getPerkLevel, player::setPerkLevelDebug, PerkFactory.Perks.Aiming);
            Wrappers.sliderInt("Reloading", 0, 10, player::getPerkLevel, player::setPerkLevelDebug, PerkFactory.Perks.Reloading);
            Wrappers.sliderInt("Nimble", 0, 10, player::getPerkLevel, player::setPerkLevelDebug, PerkFactory.Perks.Nimble);
            player.getStats()
                .set(
                    CharacterStat.PANIC,
                    Wrappers.sliderFloat(
                        "Panic", player.getStats().get(CharacterStat.PANIC), CharacterStat.PANIC.getMinimumValue(), CharacterStat.PANIC.getMaximumValue()
                    )
                );
            ImGui.sameLine();
            this.disablePanic = Wrappers.checkbox("Disable", this.disablePanic);
            if (this.disablePanic) {
                player.getStats().reset(CharacterStat.PANIC);
            }

            player.getCharacterTraits()
                .set(CharacterTrait.MARKSMAN, Wrappers.checkbox(CharacterTrait.MARKSMAN.toString(), player.hasTrait(CharacterTrait.MARKSMAN)));
            player.getCharacterTraits()
                .set(CharacterTrait.DEXTROUS, Wrappers.checkbox(CharacterTrait.DEXTROUS.toString(), player.hasTrait(CharacterTrait.DEXTROUS)));
            player.getCharacterTraits()
                .set(CharacterTrait.NIGHT_VISION, Wrappers.checkbox(CharacterTrait.NIGHT_VISION.toString(), player.hasTrait(CharacterTrait.NIGHT_VISION)));
            player.getCharacterTraits()
                .set(CharacterTrait.EAGLE_EYED, Wrappers.checkbox(CharacterTrait.EAGLE_EYED.toString(), player.hasTrait(CharacterTrait.EAGLE_EYED)));
            player.getCharacterTraits()
                .set(CharacterTrait.SHORT_SIGHTED, Wrappers.checkbox(CharacterTrait.SHORT_SIGHTED.toString(), player.hasTrait(CharacterTrait.SHORT_SIGHTED)));
            ImGui.treePop();
        }
    }

    private void doWeaponTweaks(IsoPlayer player, HandWeapon weapon) {
        if (weapon != null && weapon.isAimedFirearm()) {
            if (this.doTreeNode("gunplayFirearm", "Firearm", false, true)) {
                ImGui.text(weapon.getDisplayName());
                if (this.doTreeNode("gunplayAmmo", "Ammo", false, false)) {
                    Wrappers.sliderInt("Count", 0, weapon.getMaxAmmo(), weapon::getCurrentAmmoCount, weapon::setCurrentAmmoCount);
                    Wrappers.sliderInt("Max", 0, 100, weapon::getMaxAmmo, weapon::setMaxAmmo);
                    Wrappers.sliderInt("Max Hit Count", 0, 10, weapon::getMaxHitCount, weapon::setMaxHitCount);
                    Wrappers.sliderInt("Projectile Count", 0, 10, weapon::getProjectileCount, weapon::setProjectileCount);
                    Wrappers.sliderInt("Recoil Delay", 0, 100, weapon::getRecoilDelay, weapon::setRecoilDelay);
                    Wrappers.sliderFloat("Rate of Fire", 0.0F, 2.0F, 2, weapon::getCyclicRateMultiplier, weapon::setCyclicRateMultiplier);
                    Wrappers.checkbox("Round Chambered", weapon::isRoundChambered, weapon::setRoundChambered);
                    ImGui.sameLine();
                    Wrappers.checkbox("Jammed", weapon::isJammed, weapon::setJammed);
                    ImGui.treePop();
                }

                if (this.doTreeNode("gunplayAccuracy", "Accuracy", false, false)) {
                    Wrappers.sliderInt("AimingTime", 0, 100, weapon::getAimingTime, weapon::setAimingTime);
                    Wrappers.sliderInt("HitChance", 0, 100, weapon::getHitChance, weapon::setHitChance);
                    Wrappers.sliderFloat("* Per Lvl", 0.0F, 20.0F, 2, weapon::getAimingPerkHitChanceModifier, weapon::setAimingPerkHitChanceModifier);
                    Wrappers.sliderFloat("MinAngle", 0.8F, 1.0F, 3, weapon::getMinAngle, weapon::setMinAngle);
                    Wrappers.sliderFloat("MinSightRange", 1.0F, 40.0F, 2, weapon::getMinSightRange, weapon::setMinSightRange);
                    Wrappers.sliderFloat("MaxSightRange", 1.0F, 40.0F, 2, weapon::getMaxSightRange, weapon::setMaxSightRange);
                    ImGui.treePop();
                }

                if (this.doTreeNode("gunplayDamage", "Damage", false, false)) {
                    Wrappers.sliderFloat("MinDamage", 0.0F, 10.0F, 2, weapon::getMinDamage, weapon::setMinDamage);
                    Wrappers.sliderFloat("MaxDamage", 0.0F, 10.0F, 2, weapon::getMaxDamage, weapon::setMaxDamage);
                    Wrappers.sliderFloat("Crit Chance", 0.0F, 100.0F, 2, weapon::getCriticalChance, weapon::setCriticalChance);
                    Wrappers.sliderFloat("* Multiplier", 0.0F, 100.0F, 2, weapon::getCriticalDamageMultiplier, weapon::setCriticalDamageMultiplier);
                    Wrappers.sliderInt("* Per Lvl", 0, 50, weapon::getAimingPerkCritModifier, weapon::setAimingPerkCritModifier);
                    ImGui.treePop();
                }

                if (this.doTreeNode("gunplayMisc", "Misc", false, false)) {
                    Wrappers.sliderFloat("MaxRange", 0.0F, 100.0F, 2, weapon::getMaxRange, weapon::setMaxRange);
                    Wrappers.sliderFloat("* Per Lvl", 0.0F, 20.0F, 2, weapon::getAimingPerkRangeModifier, weapon::setAimingPerkRangeModifier);
                    Wrappers.sliderFloat("Swingtime", 0.1F, 5.0F, 2, weapon::getSwingTime, weapon::setSwingTime);
                    Wrappers.sliderFloat("* Min", 0.1F, 5.0F, 2, weapon::getMinimumSwingTime, weapon::setMinimumSwingTime);
                    Wrappers.sliderInt("Condition", 0, weapon.getConditionMax(), weapon::getCondition, weapon::setCondition);
                    Wrappers.sliderInt("* Lower Chance", 0, 300, weapon::getConditionLowerChance, weapon::setConditionLowerChance);
                    ImGui.treePop();
                }

                if (this.doTreeNode("gunplayAttach", "Attachments", false, false)) {
                    for (WeaponPart i : weapon.getAllWeaponParts()) {
                        ImGui.text(i.getFullType());
                        ImGui.sameLine();
                        if (ImGui.button("Remove")) {
                            weapon.detachWeaponPart(i);
                        }
                    }

                    if (ImGui.beginCombo("Add attachment", this.selectedAttachment)) {
                        for (WeaponPart ix : this.attachments) {
                            if (ix.canAttach(player, weapon)) {
                                Wrappers.valueBoolean.set(this.selectedAttachment.equals(ix.getFullType()));
                                ImGui.selectable(ix.getFullType(), Wrappers.valueBoolean);
                                if (Wrappers.valueBoolean.get()) {
                                    this.selectedAttachment = ix.getFullType();
                                    ImGui.setItemDefaultFocus();
                                }
                            }
                        }

                        ImGui.endCombo();
                    }

                    ImGui.sameLine();
                    if (ImGui.button("Add") && !StringUtils.isNullOrEmpty(this.selectedAttachment)) {
                        weapon.attachWeaponPart(InventoryItemFactory.CreateItem(this.selectedAttachment));
                    }

                    ImGui.treePop();
                }

                ImGui.treePop();
            }
        }
    }

    private void doTargetInfo(IsoPlayer player, HandWeapon weapon) {
        if (weapon != null && weapon.isAimedFirearm()) {
            if (this.doTreeNode("gunplayTargets", "Targets", false, false)) {
                if (ImGui.beginTable("gunplayTargetTable", 10, 1984)) {
                    if (player.isAiming()) {
                        this.combatManager.calculateAttackVars(player);
                        this.combatManager.calculateHitInfoList(player);
                    }

                    ImGui.tableSetupColumn("id");
                    ImGui.tableSetupColumn("distance");
                    ImGui.tableSetupColumn("penalty");
                    ImGui.tableSetupColumn("speed");
                    ImGui.tableSetupColumn("light");
                    ImGui.tableSetupColumn("penalty");
                    ImGui.tableSetupColumn("health");
                    ImGui.tableSetupColumn("chance");
                    ImGui.tableSetupColumn("isCameraTarget");
                    ImGui.tableSetupColumn("BodyPart");
                    ImGui.tableHeadersRow();
                    float max = weapon.getMaxSightRange(player);
                    float min = weapon.getMinSightRange(player);
                    int id = -1;
                    PZArrayList<HitInfo> hitInfoList = player.getHitInfoList();

                    for (int i = 0; i < hitInfoList.size(); i++) {
                        int columnIndex = 0;
                        HitInfo hitInfo = hitInfoList.get(i);
                        if (hitInfo.getObject() instanceof IsoGameCharacter obj) {
                            IsoZombie zombie = (IsoZombie)hitInfo.getObject();
                            ImGui.tableNextRow();
                            ImGui.tableSetColumnIndex(columnIndex++);
                            if (zombie == null) {
                                ImGui.text("ID");
                            } else {
                                id = zombie.getID();
                                ImGui.text(Integer.toString(id));
                            }

                            ImGui.tableSetColumnIndex(columnIndex++);
                            float dist = PZMath.sqrt(hitInfo.distSq);
                            float chance = 0.0F;
                            if (dist < min) {
                                if (dist > this.combatConfig.get(CombatConfigKey.POINT_BLANK_DISTANCE)) {
                                    chance -= (dist - min) * 3.0F;
                                }
                            } else if (dist >= max) {
                                chance -= (dist - max) * 3.0F;
                            } else {
                                float diff = (max - min) * 0.5F;
                                chance += this.combatConfig.get(CombatConfigKey.OPTIMAL_RANGE_TO_HIT_MAXIMUM_BONUS)
                                    * (1.0F - Math.abs((dist - min - diff) / diff));
                            }

                            ImGui.text(Float.toString(PZMath.roundFloat(dist, 3)));
                            ImGui.tableSetColumnIndex(columnIndex++);
                            ImGui.text(Float.toString(PZMath.roundFloat(chance, 3)));
                            ImGui.tableSetColumnIndex(columnIndex++);
                            ImGui.text(Float.toString(PZMath.roundFloat(obj.getMovementSpeed() * GameTime.getInstance().getInvMultiplier(), 6)));
                            ImGui.tableSetColumnIndex(columnIndex++);
                            ImGui.text(Float.toString(PZMath.roundFloat(obj.getCurrentSquare().getLightLevel(player.getPlayerNum()), 3)));
                            ImGui.tableSetColumnIndex(columnIndex++);
                            ImGui.text(
                                Float.toString(
                                    PZMath.max(
                                        0.0F,
                                        this.combatConfig.get(CombatConfigKey.LOW_LIGHT_TO_HIT_MAXIMUM_PENALTY)
                                            * (
                                                1.0F
                                                    - obj.getCurrentSquare().getLightLevel(player.getPlayerNum())
                                                        / this.combatConfig.get(CombatConfigKey.LOW_LIGHT_THRESHOLD)
                                            )
                                    )
                                )
                            );
                            ImGui.tableSetColumnIndex(columnIndex++);
                            ImGui.text(Float.toString(PZMath.roundFloat(obj.getHealth(), 3)));
                            ImGui.tableSetColumnIndex(columnIndex++);
                            ImGui.text(Integer.toString(hitInfo.chance));
                            BallisticsController ballisticsController = player.getBallisticsController();
                            if (ballisticsController != null) {
                                boolean isCameraTarget = ballisticsController.isCachedCameraTarget(id);
                                ImGui.tableSetColumnIndex(columnIndex++);
                                ImGui.text(Boolean.toString(isCameraTarget));
                                int targetedBodyPart = RagdollBodyPart.BODYPART_COUNT.ordinal();
                                if (isCameraTarget) {
                                    targetedBodyPart = ballisticsController.getCachedTargetedBodyPart(zombie.getID());
                                    String bodyPartName = RagdollBodyPart.values()[targetedBodyPart].name();
                                    ImGui.tableSetColumnIndex(columnIndex++);
                                    ImGui.text(bodyPartName);
                                }
                            }
                        }
                    }

                    ImGui.endTable();
                }

                ImGui.treePop();
            }
        }
    }
}
