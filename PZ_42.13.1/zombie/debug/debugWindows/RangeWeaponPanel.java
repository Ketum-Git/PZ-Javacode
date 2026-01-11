// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.HandWeapon;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;

public class RangeWeaponPanel extends PZDebugWindow {
    private final RangeWeaponPanel.RangeWeaponsConfigOptions rangeWeaponsConfigOptions = new RangeWeaponPanel.RangeWeaponsConfigOptions();
    private final ArrayList<Item> allRangeWeaponScriptItems = new ArrayList<>();
    private boolean allowSavingDefaults;

    @Override
    public String getTitle() {
        return "Range Weapon Editor";
    }

    @Override
    protected void doWindowContents() {
        this.cacheAllRangeWeaponScriptItems();
        boolean showWeaponTab = IsoPlayer.players[0] != null && !this.allRangeWeaponScriptItems.isEmpty();
        ImGui.beginChild("Begin");
        this.allowSavingDefaults = Wrappers.checkbox("Allow Saving Defaults", this.allowSavingDefaults);
        if (this.allowSavingDefaults) {
            ImGui.sameLine();
            if (ImGui.button("Save All Defaults")) {
                for (Item item : this.allRangeWeaponScriptItems) {
                    if (item.isRanged() && !item.hasTag(ItemTag.FAKE_WEAPON)) {
                        this.rangeWeaponsConfigOptions.saveDefaults(item, item.getName());
                    }
                }

                this.allowSavingDefaults = false;
            }
        }

        if (ImGui.button("Load All Defaults")) {
            for (Item itemx : this.allRangeWeaponScriptItems) {
                if (itemx.isRanged() && !itemx.hasTag(ItemTag.FAKE_WEAPON)) {
                    this.rangeWeaponsConfigOptions.loadDefaults(itemx, itemx.getName());
                }
            }
        }

        ImGui.sameLine();
        if (ImGui.button("Save All Custom Data")) {
            for (Item itemxx : this.allRangeWeaponScriptItems) {
                if (itemxx.isRanged() && !itemxx.hasTag(ItemTag.FAKE_WEAPON)) {
                    this.rangeWeaponsConfigOptions.save(itemxx, itemxx.getName());
                }
            }
        }

        ImGui.sameLine();
        if (ImGui.button("Load All Custom Data")) {
            for (Item itemxxx : this.allRangeWeaponScriptItems) {
                if (itemxxx.isRanged() && !itemxxx.hasTag(ItemTag.FAKE_WEAPON)) {
                    this.rangeWeaponsConfigOptions.load(itemxxx, itemxxx.getName());
                }
            }
        }

        if (showWeaponTab) {
            if (ImGui.beginTabBar("tabSelector")) {
                this.weaponTab();
            }

            ImGui.endTabBar();
        }

        ImGui.endChild();
    }

    private void cacheAllRangeWeaponScriptItems() {
        if (this.allRangeWeaponScriptItems.isEmpty()) {
            ArrayList<Item> allScriptItems = ScriptManager.instance.getAllItems();
            if (allScriptItems != null) {
                for (Item item : allScriptItems) {
                    if (item.isRanged() && !item.hasTag(ItemTag.FAKE_WEAPON)) {
                        this.allRangeWeaponScriptItems.add(item);
                    }
                }
            }
        }
    }

    private void weaponTab() {
        for (Item item : this.allRangeWeaponScriptItems) {
            boolean tabSelected = false;
            if (item.isRanged() && !item.hasTag(ItemTag.FAKE_WEAPON)) {
                String itemName = item.getName();
                if (ImGui.beginTabItem(itemName, 0)) {
                    if (this.editItem(item)) {
                        this.updateInventoryItems(item);
                    }

                    if (ImGui.button("Load " + itemName + " Defaults") && item.isRanged() && !item.hasTag(ItemTag.FAKE_WEAPON)) {
                        this.rangeWeaponsConfigOptions.loadDefaults(item, itemName);
                    }

                    ImGui.sameLine();
                    if (ImGui.button("Save " + itemName + " Custom Data") && item.isRanged() && !item.hasTag(ItemTag.FAKE_WEAPON)) {
                        this.rangeWeaponsConfigOptions.save(item, itemName);
                    }

                    ImGui.sameLine();
                    if (ImGui.button("Load " + itemName + " Custom Data") && item.isRanged() && !item.hasTag(ItemTag.FAKE_WEAPON)) {
                        this.rangeWeaponsConfigOptions.load(item, itemName);
                    }

                    ImGui.endTabItem();
                }
            }
        }
    }

    private void updateInventoryItems(Item item) {
        if (IsoPlayer.players[0] != null) {
            ItemContainer itemContainer = IsoPlayer.players[0].getInventory();

            for (InventoryItem inventoryItem : itemContainer.items) {
                Item scriptItem = inventoryItem.getScriptItem();
                if (item == scriptItem) {
                    inventoryItem.setName("ScriptItem");
                    if (inventoryItem instanceof HandWeapon handWeapon) {
                        handWeapon.setConditionLowerChance(scriptItem.conditionLowerChance);
                        handWeapon.setMinDamage(scriptItem.minDamage);
                        handWeapon.setMaxDamage(scriptItem.maxDamage);
                        handWeapon.setMinRange(scriptItem.minRange);
                        handWeapon.setMinSightRange(scriptItem.minSightRange);
                        handWeapon.setMaxSightRange(scriptItem.maxSightRange);
                        handWeapon.setMaxAmmo(scriptItem.maxAmmo);
                        handWeapon.setDoorDamage(scriptItem.doorDamage);
                        handWeapon.setSoundRadius(scriptItem.soundRadius);
                        handWeapon.setToHitModifier(scriptItem.toHitModifier);
                        handWeapon.setCriticalChance(scriptItem.criticalChance);
                        handWeapon.setCriticalDamageMultiplier(scriptItem.critDmgMultiplier);
                        handWeapon.setAimingPerkCritModifier(scriptItem.aimingPerkCritModifier);
                        handWeapon.setAimingPerkRangeModifier(scriptItem.aimingPerkRangeModifier);
                        handWeapon.setAimingPerkHitChanceModifier(scriptItem.aimingPerkHitChanceModifier);
                        handWeapon.setHitChance(scriptItem.hitChance);
                        handWeapon.setRecoilDelay(scriptItem.recoilDelay);
                        handWeapon.setReloadTime(scriptItem.reloadTime);
                        handWeapon.setAimingTime(scriptItem.aimingTime);
                        handWeapon.setJamGunChance(scriptItem.jamGunChance);
                        handWeapon.setCyclicRateMultiplier(scriptItem.cyclicRateMultiplier);
                        handWeapon.setProjectileCount(scriptItem.projectileCount);
                        handWeapon.setProjectileSpread(scriptItem.projectileSpread);
                        handWeapon.setProjectileWeightCenter(scriptItem.projectileWeightCenter);
                    }
                }
            }
        }
    }

    private boolean editItem(Item item) {
        Wrappers.clearValueChanged();
        item.conditionLowerChance = Wrappers.sliderIntShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getId(),
            item.conditionLowerChance,
            (int)RangeWeaponPanel.RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getMin(),
            (int)RangeWeaponPanel.RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getMax()
        );
        item.minDamage = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.MIN_DAMAGE.getId(),
            item.minDamage,
            (float)RangeWeaponPanel.RangeWeaponAttribute.MIN_DAMAGE.getMin(),
            item.maxDamage
        );
        item.maxDamage = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.MAX_DAMAGE.getId(),
            item.maxDamage,
            item.minDamage,
            (float)RangeWeaponPanel.RangeWeaponAttribute.MAX_DAMAGE.getMax()
        );
        item.minRange = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.MIN_RANGE.getId(),
            item.minRange,
            (float)RangeWeaponPanel.RangeWeaponAttribute.MIN_RANGE.getMin(),
            item.maxRange
        );
        item.maxRange = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.MAX_RANGE.getId(),
            item.maxRange,
            item.minRange,
            (float)RangeWeaponPanel.RangeWeaponAttribute.MAX_RANGE.getMax()
        );
        item.minSightRange = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.MIN_SIGHT_RANGE.getId(),
            item.minSightRange,
            (float)RangeWeaponPanel.RangeWeaponAttribute.MIN_SIGHT_RANGE.getMin(),
            item.maxSightRange
        );
        item.maxSightRange = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.MAX_SIGHT_RANGE.getId(),
            item.maxSightRange,
            item.minSightRange,
            (float)RangeWeaponPanel.RangeWeaponAttribute.MAX_SIGHT_RANGE.getMax()
        );
        item.maxHitCount = Wrappers.sliderIntShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.MAX_HIT_COUNT.getId(),
            item.maxHitCount,
            (int)RangeWeaponPanel.RangeWeaponAttribute.MAX_HIT_COUNT.getMin(),
            (int)RangeWeaponPanel.RangeWeaponAttribute.MAX_HIT_COUNT.getMax()
        );
        item.maxAmmo = Wrappers.sliderIntShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.MAX_AMMO.getId(),
            item.maxAmmo,
            (int)RangeWeaponPanel.RangeWeaponAttribute.MAX_AMMO.getMin(),
            (int)RangeWeaponPanel.RangeWeaponAttribute.MAX_AMMO.getMax()
        );
        item.doorDamage = Wrappers.sliderIntShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.DOOR_DAMAGE.getId(),
            item.doorDamage,
            (int)RangeWeaponPanel.RangeWeaponAttribute.DOOR_DAMAGE.getMin(),
            (int)RangeWeaponPanel.RangeWeaponAttribute.DOOR_DAMAGE.getMax()
        );
        item.soundRadius = Wrappers.sliderIntShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.SOUND_RADIUS.getId(),
            item.soundRadius,
            (int)RangeWeaponPanel.RangeWeaponAttribute.SOUND_RADIUS.getMin(),
            (int)RangeWeaponPanel.RangeWeaponAttribute.SOUND_RADIUS.getMax()
        );
        item.toHitModifier = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.TO_HIT_MODIFIER.getId(),
            item.toHitModifier,
            (float)RangeWeaponPanel.RangeWeaponAttribute.TO_HIT_MODIFIER.getMin(),
            (float)RangeWeaponPanel.RangeWeaponAttribute.TO_HIT_MODIFIER.getMax()
        );
        item.criticalChance = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.CRITICAL_CHANCE.getId(),
            item.criticalChance,
            (float)RangeWeaponPanel.RangeWeaponAttribute.CRITICAL_CHANCE.getMin(),
            (float)RangeWeaponPanel.RangeWeaponAttribute.CRITICAL_CHANCE.getMax()
        );
        item.critDmgMultiplier = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getId(),
            item.critDmgMultiplier,
            (float)RangeWeaponPanel.RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getMin(),
            (float)RangeWeaponPanel.RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getMax()
        );
        item.aimingPerkCritModifier = Wrappers.sliderIntShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getId(),
            item.aimingPerkCritModifier,
            (int)RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getMin(),
            (int)RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getMax()
        );
        item.aimingPerkRangeModifier = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getId(),
            item.aimingPerkRangeModifier,
            (float)RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getMin(),
            (float)RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getMax()
        );
        item.aimingPerkHitChanceModifier = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getId(),
            item.aimingPerkHitChanceModifier,
            (float)RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getMin(),
            (float)RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getMax()
        );
        item.hitChance = Wrappers.sliderIntShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.HIT_CHANCE.getId(),
            item.hitChance,
            (int)RangeWeaponPanel.RangeWeaponAttribute.HIT_CHANCE.getMin(),
            (int)RangeWeaponPanel.RangeWeaponAttribute.HIT_CHANCE.getMax()
        );
        item.recoilDelay = Wrappers.sliderIntShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.RECOIL_DELAY.getId(),
            item.recoilDelay,
            (int)RangeWeaponPanel.RangeWeaponAttribute.RECOIL_DELAY.getMin(),
            (int)RangeWeaponPanel.RangeWeaponAttribute.RECOIL_DELAY.getMax()
        );
        item.reloadTime = Wrappers.sliderIntShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.RELOAD_TIME.getId(),
            item.reloadTime,
            (int)RangeWeaponPanel.RangeWeaponAttribute.RELOAD_TIME.getMin(),
            (int)RangeWeaponPanel.RangeWeaponAttribute.RELOAD_TIME.getMax()
        );
        item.aimingTime = Wrappers.sliderIntShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.AIMING_TIME.getId(),
            item.aimingTime,
            (int)RangeWeaponPanel.RangeWeaponAttribute.AIMING_TIME.getMin(),
            (int)RangeWeaponPanel.RangeWeaponAttribute.AIMING_TIME.getMax()
        );
        item.jamGunChance = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.JAM_GUN_CHANCE.getId(),
            item.jamGunChance,
            (float)RangeWeaponPanel.RangeWeaponAttribute.JAM_GUN_CHANCE.getMin(),
            (float)RangeWeaponPanel.RangeWeaponAttribute.JAM_GUN_CHANCE.getMax()
        );
        item.cyclicRateMultiplier = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getId(),
            item.cyclicRateMultiplier,
            (float)RangeWeaponPanel.RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getMin(),
            (float)RangeWeaponPanel.RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getMax()
        );
        item.projectileCount = Wrappers.sliderIntShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_COUNT.getId(),
            item.projectileCount,
            (int)RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_COUNT.getMin(),
            (int)RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_COUNT.getMax()
        );
        item.projectileSpread = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_SPREAD.getId(),
            item.projectileSpread,
            (float)RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_SPREAD.getMin(),
            (float)RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_SPREAD.getMax()
        );
        item.projectileWeightCenter = Wrappers.sliderFloatShowRange(
            RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getId(),
            item.projectileWeightCenter,
            (float)RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getMin(),
            (float)RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getMax()
        );
        return Wrappers.didValuesChange();
    }

    private static enum RangeWeaponAttribute {
        CONDITION_LOWER_CHANCE("ConditionLowerChance", 1.0, 300.0, 1.0),
        MIN_DAMAGE("MinDamage", 0.0, 10.0, 1.0),
        MAX_DAMAGE("MaxDamage", 0.0, 10.0, 1.0),
        MIN_RANGE("MinRange", 0.0, 50.0, 1.0),
        MAX_RANGE("MaxRange", 0.0, 50.0, 1.0),
        MIN_SIGHT_RANGE("minSightRange", 0.0, 20.0, 1.0),
        MAX_SIGHT_RANGE("maxSightRange", 0.0, 20.0, 1.0),
        MAX_HIT_COUNT("MaxHitCount", 1.0, 10.0, 1.0),
        MAX_AMMO("maxAmmo", 1.0, 100.0, 1.0),
        DOOR_DAMAGE("DoorDamage", 1.0, 20.0, 1.0),
        SOUND_RADIUS("SoundRadius", 1.0, 200.0, 1.0),
        TO_HIT_MODIFIER("ToHitModifier", 0.0, 100.0, 0.0),
        CRITICAL_CHANCE("CriticalChance", 0.0, 100.0, 0.0),
        CRIT_DMG_MULTIPLIER("critDmgMultiplier", 0.0, 20.0, 1.0),
        AIMING_PERK_CRIT_MOD("AimingPerkCritModifier", 0.0, 20.0, 0.0),
        AIMING_PERK_RANGE_MOD("AimingPerkRangeModifier", 0.0, 20.0, 0.0),
        AIMING_PERK_HIT_CHANCE_MOD("AimingPerkHitChanceModifier", 0.0, 100.0, 0.0),
        HIT_CHANCE("HitChance", 1.0, 100.0, 1.0),
        RECOIL_DELAY("RecoilDelay", 1.0, 100.0, 1.0),
        RELOAD_TIME("reloadTime", 1.0, 100.0, 1.0),
        AIMING_TIME("aimingTime", 1.0, 100.0, 1.0),
        JAM_GUN_CHANCE("jamGunChance", 0.0, 20.0, 0.0),
        CYCLIC_RATE_MULTIPLIER("cyclicRateMultiplier", 0.0, 100.0, 1.0),
        PROJECTILE_COUNT("ProjectileCount", 1.0, 9.0, 1.0),
        PROJECTILE_SPREAD("projectileSpread", 0.0, 10.0, 0.0),
        PROJECTILE_WEIGHT_CENTER("projectileWeightCenter", 0.0, 10.0, 0.0);

        private final String id;
        private final double min;
        private final double max;
        private final double defaultValue;

        private RangeWeaponAttribute(final String id, final double min, final double max, final double defaultValue) {
            this.id = id;
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
        }

        public String getId() {
            return this.id;
        }

        public double getMin() {
            return this.min;
        }

        public double getMax() {
            return this.max;
        }

        public double getDefaultValue() {
            return this.defaultValue;
        }
    }

    public static class RangeWeaponsConfigOption extends DoubleConfigOption {
        public RangeWeaponsConfigOption(
            RangeWeaponPanel.RangeWeaponAttribute rangeWeaponAttribute,
            double min,
            double max,
            double defaultValue,
            ArrayList<RangeWeaponPanel.RangeWeaponsConfigOption> options
        ) {
            super(rangeWeaponAttribute.getId(), min, max, defaultValue);
            options.add(this);
        }
    }

    public class RangeWeaponsConfigOptions {
        private static final int VERSION = 1;
        private final ArrayList<RangeWeaponPanel.RangeWeaponsConfigOption> options;

        RangeWeaponsConfigOptions() {
            Objects.requireNonNull(RangeWeaponPanel.this);
            super();
            this.options = new ArrayList<>();
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.CONDITION_LOWER_CHANCE,
                RangeWeaponPanel.RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.MIN_DAMAGE,
                RangeWeaponPanel.RangeWeaponAttribute.MIN_DAMAGE.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.MIN_DAMAGE.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.MIN_DAMAGE.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.MAX_DAMAGE,
                RangeWeaponPanel.RangeWeaponAttribute.MAX_DAMAGE.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.MAX_DAMAGE.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.MAX_DAMAGE.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.MIN_RANGE,
                RangeWeaponPanel.RangeWeaponAttribute.MIN_RANGE.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.MIN_RANGE.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.MIN_RANGE.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.MAX_RANGE,
                RangeWeaponPanel.RangeWeaponAttribute.MAX_RANGE.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.MAX_RANGE.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.MAX_RANGE.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.MIN_SIGHT_RANGE,
                RangeWeaponPanel.RangeWeaponAttribute.MIN_SIGHT_RANGE.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.MIN_SIGHT_RANGE.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.MIN_SIGHT_RANGE.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.MAX_SIGHT_RANGE,
                RangeWeaponPanel.RangeWeaponAttribute.MAX_SIGHT_RANGE.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.MAX_SIGHT_RANGE.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.MAX_SIGHT_RANGE.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.MAX_HIT_COUNT,
                RangeWeaponPanel.RangeWeaponAttribute.MAX_HIT_COUNT.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.MAX_HIT_COUNT.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.MAX_HIT_COUNT.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.MAX_AMMO,
                RangeWeaponPanel.RangeWeaponAttribute.MAX_AMMO.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.MAX_AMMO.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.MAX_AMMO.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.DOOR_DAMAGE,
                RangeWeaponPanel.RangeWeaponAttribute.DOOR_DAMAGE.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.DOOR_DAMAGE.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.DOOR_DAMAGE.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.SOUND_RADIUS,
                RangeWeaponPanel.RangeWeaponAttribute.SOUND_RADIUS.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.SOUND_RADIUS.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.SOUND_RADIUS.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.TO_HIT_MODIFIER,
                RangeWeaponPanel.RangeWeaponAttribute.TO_HIT_MODIFIER.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.TO_HIT_MODIFIER.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.TO_HIT_MODIFIER.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.CRITICAL_CHANCE,
                RangeWeaponPanel.RangeWeaponAttribute.CRITICAL_CHANCE.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.CRITICAL_CHANCE.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.CRITICAL_CHANCE.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.CRIT_DMG_MULTIPLIER,
                RangeWeaponPanel.RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_CRIT_MOD,
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_RANGE_MOD,
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD,
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.HIT_CHANCE,
                RangeWeaponPanel.RangeWeaponAttribute.HIT_CHANCE.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.HIT_CHANCE.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.HIT_CHANCE.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.RECOIL_DELAY,
                RangeWeaponPanel.RangeWeaponAttribute.RECOIL_DELAY.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.RECOIL_DELAY.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.RECOIL_DELAY.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.RELOAD_TIME,
                RangeWeaponPanel.RangeWeaponAttribute.RELOAD_TIME.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.RELOAD_TIME.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.RELOAD_TIME.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_TIME,
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_TIME.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_TIME.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.AIMING_TIME.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.JAM_GUN_CHANCE,
                RangeWeaponPanel.RangeWeaponAttribute.JAM_GUN_CHANCE.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.JAM_GUN_CHANCE.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.JAM_GUN_CHANCE.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER,
                RangeWeaponPanel.RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_COUNT,
                RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_COUNT.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_COUNT.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_COUNT.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_SPREAD,
                RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_SPREAD.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_SPREAD.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_SPREAD.getDefaultValue(),
                this.options
            );
            new RangeWeaponPanel.RangeWeaponsConfigOption(
                RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER,
                RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getMin(),
                RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getMax(),
                RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getDefaultValue(),
                this.options
            );
        }

        private int getOptionCount() {
            return this.options.size();
        }

        private ConfigOption getOptionByIndex(int index) {
            return this.options.get(index);
        }

        private ConfigOption getOptionByName(String name) {
            for (int i = 0; i < this.options.size(); i++) {
                ConfigOption setting = this.options.get(i);
                if (setting.getName().equals(name)) {
                    return setting;
                }
            }

            return null;
        }

        private void setOptionValue(RangeWeaponPanel.RangeWeaponAttribute rangeWeaponAttribute, double value) {
            RangeWeaponPanel.RangeWeaponsConfigOption rangeWeaponsConfigOption = (RangeWeaponPanel.RangeWeaponsConfigOption)RangeWeaponPanel.this.rangeWeaponsConfigOptions
                .getOptionByName(rangeWeaponAttribute.getId());
            if (rangeWeaponsConfigOption != null) {
                rangeWeaponsConfigOption.setValue(value);
            }
        }

        public void save(Item item, String weaponKey) {
            String fileName = ZomboidFileSystem.instance.getMediaRootPath()
                + File.separator
                + "editor"
                + File.separator
                + "custom_range_weapon_data_"
                + weaponKey.toLowerCase()
                + ".txt";
            this.save(fileName, item);
        }

        public void saveDefaults(Item item, String weaponKey) {
            String fileName = ZomboidFileSystem.instance.getMediaRootPath()
                + File.separator
                + "editor"
                + File.separator
                + "default_range_weapon_data_"
                + weaponKey.toLowerCase()
                + ".txt";
            this.save(fileName, item);
        }

        private void save(String fileName, Item item) {
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.CONDITION_LOWER_CHANCE, item.conditionLowerChance);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.MIN_DAMAGE, item.minDamage);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.MAX_DAMAGE, item.maxDamage);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.MIN_RANGE, item.minRange);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.MAX_RANGE, item.maxRange);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.MIN_SIGHT_RANGE, item.minSightRange);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.MAX_SIGHT_RANGE, item.maxSightRange);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.MAX_HIT_COUNT, item.maxHitCount);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.MAX_AMMO, item.maxAmmo);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.DOOR_DAMAGE, item.doorDamage);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.SOUND_RADIUS, item.soundRadius);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.TO_HIT_MODIFIER, item.toHitModifier);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.CRITICAL_CHANCE, item.criticalChance);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.CRIT_DMG_MULTIPLIER, item.critDmgMultiplier);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_CRIT_MOD, item.aimingPerkCritModifier);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_RANGE_MOD, item.aimingPerkRangeModifier);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD, item.aimingPerkHitChanceModifier);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.HIT_CHANCE, item.hitChance);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.RECOIL_DELAY, item.recoilDelay);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.RELOAD_TIME, item.reloadTime);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.AIMING_TIME, item.aimingTime);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.JAM_GUN_CHANCE, item.jamGunChance);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER, item.cyclicRateMultiplier);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_COUNT, item.projectileCount);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_SPREAD, item.projectileSpread);
            this.setOptionValue(RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER, item.projectileWeightCenter);
            ConfigFile configFile = new ConfigFile();
            configFile.write(fileName, 1, this.options);
        }

        public void load(Item item, String weaponKey) {
            String fileName = ZomboidFileSystem.instance.getMediaRootPath()
                + File.separator
                + "editor"
                + File.separator
                + "custom_range_weapon_data_"
                + weaponKey.toLowerCase()
                + ".txt";
            this.load(fileName, item);
        }

        public void loadDefaults(Item item, String weaponKey) {
            String fileName = ZomboidFileSystem.instance.getMediaRootPath()
                + File.separator
                + "editor"
                + File.separator
                + "default_range_weapon_data_"
                + weaponKey.toLowerCase()
                + ".txt";
            this.load(fileName, item);
        }

        private void load(String fileName, Item item) {
            ConfigFile configFile = new ConfigFile();
            if (configFile.read(fileName)) {
                for (int i = 0; i < configFile.getOptions().size(); i++) {
                    ConfigOption configOption = configFile.getOptions().get(i);
                    RangeWeaponPanel.RangeWeaponsConfigOption rangeWeaponsConfigOption = (RangeWeaponPanel.RangeWeaponsConfigOption)RangeWeaponPanel.this.rangeWeaponsConfigOptions
                        .getOptionByName(configOption.getName());
                    if (rangeWeaponsConfigOption != null) {
                        rangeWeaponsConfigOption.parse(configOption.getValueAsString());
                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.CONDITION_LOWER_CHANCE.getId())) {
                            item.conditionLowerChance = (int)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.MIN_DAMAGE.getId())) {
                            item.minDamage = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.MAX_DAMAGE.getId())) {
                            item.maxDamage = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.MIN_RANGE.getId())) {
                            item.minRange = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.MAX_RANGE.getId())) {
                            item.maxRange = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.MIN_SIGHT_RANGE.getId())) {
                            item.minSightRange = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.MAX_SIGHT_RANGE.getId())) {
                            item.maxSightRange = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.MAX_HIT_COUNT.getId())) {
                            item.maxHitCount = (int)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.MAX_AMMO.getId())) {
                            item.maxAmmo = (int)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.DOOR_DAMAGE.getId())) {
                            item.doorDamage = (int)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.SOUND_RADIUS.getId())) {
                            item.soundRadius = (int)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.TO_HIT_MODIFIER.getId())) {
                            item.toHitModifier = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.CRITICAL_CHANCE.getId())) {
                            item.criticalChance = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.CRIT_DMG_MULTIPLIER.getId())) {
                            item.critDmgMultiplier = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_CRIT_MOD.getId())) {
                            item.aimingPerkCritModifier = (int)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_RANGE_MOD.getId())) {
                            item.aimingPerkRangeModifier = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.AIMING_PERK_HIT_CHANCE_MOD.getId())) {
                            item.aimingPerkHitChanceModifier = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.HIT_CHANCE.getId())) {
                            item.hitChance = (int)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.RECOIL_DELAY.getId())) {
                            item.recoilDelay = (int)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.RELOAD_TIME.getId())) {
                            item.reloadTime = (int)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.AIMING_TIME.getId())) {
                            item.aimingTime = (int)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.JAM_GUN_CHANCE.getId())) {
                            item.jamGunChance = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.CYCLIC_RATE_MULTIPLIER.getId())) {
                            item.cyclicRateMultiplier = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_COUNT.getId())) {
                            item.projectileCount = (int)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_SPREAD.getId())) {
                            item.projectileSpread = (float)rangeWeaponsConfigOption.getValue();
                        }

                        if (rangeWeaponsConfigOption.getName().equals(RangeWeaponPanel.RangeWeaponAttribute.PROJECTILE_WEIGHT_CENTER.getId())) {
                            item.projectileWeightCenter = (float)rangeWeaponsConfigOption.getValue();
                        }
                    }
                }
            }
        }
    }
}
