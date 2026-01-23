// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.types;

import java.util.ArrayList;
import java.util.List;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.interfaces.IUpdater;
import zombie.inventory.InventoryItem;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;

@UsedFromLua
public final class WeaponPart extends InventoryItem implements Drainable, IUpdater {
    private float maxRange;
    private float minSightRange;
    private float maxSightRange;
    private float lowLightBonus;
    private float minRangeRanged;
    private float damage;
    private float recoilDelay;
    private int clipSize;
    private int reloadTime;
    private int aimingTime;
    private int hitChance;
    private float angle;
    private float spreadModifier;
    private float weightModifier;
    private final List<String> mountOn = new ArrayList<>();
    private final List<String> mountOnDisplayName = new ArrayList<>();
    private String partType;
    private String canAttachCallback;
    private String canDetachCallback;
    private String onAttachCallback;
    private String onDetachCallback;
    protected int lastUpdateMinutes = -1;
    protected float useDelta = 0.03125F;
    protected float ticks;

    public WeaponPart(String module, String name, String itemType, String texName) {
        super(module, name, itemType, texName);
        this.itemType = ItemType.WEAPON;
    }

    @Override
    public String getCategory() {
        return this.mainCategory != null ? this.mainCategory : "WeaponPart";
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        ObjectTooltip.LayoutItem item = layout.addItem();
        item.setLabel(Translator.getText("Tooltip_weapon_Type") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
        item.setValue(Translator.getText("Tooltip_weapon_" + this.partType), 1.0F, 1.0F, 0.8F, 1.0F);
        item = layout.addItem();
        String mountTxt = Translator.getText("Tooltip_weapon_CanBeMountOn") + this.mountOnDisplayName.toString().replaceAll("\\[", "").replaceAll("\\]", "");
        item.setLabel(mountTxt, 1.0F, 1.0F, 0.8F, 1.0F);
        this.DoBatteryTooltip(tooltipUI, layout);
    }

    public void DoBatteryTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (this.hasTag(ItemTag.USES_BATTERY) && !this.hasTag(ItemTag.HIDE_REMAINING)) {
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel(Translator.getText("IGUI_invpanel_Remaining") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
            float f = this.getCurrentUsesFloat();
            ColorInfo G2Bgrad = new ColorInfo();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, G2Bgrad);
            item.setProgress(f, G2Bgrad.getR(), G2Bgrad.getG(), G2Bgrad.getB(), 1.0F);
        }
    }

    public float getMinSightRange() {
        return this.minSightRange;
    }

    public void setMinSightRange(float value) {
        this.minSightRange = value;
    }

    public float getMaxSightRange() {
        return this.maxSightRange;
    }

    public void setLowLightBonus(float value) {
        this.lowLightBonus = value;
    }

    public float getLowLightBonus() {
        return this.lowLightBonus;
    }

    public void setMaxSightRange(float value) {
        this.maxSightRange = value;
    }

    public float getMinRangeRanged() {
        return this.minRangeRanged;
    }

    public void setMinRangeRanged(float minRangeRanged) {
        this.minRangeRanged = minRangeRanged;
    }

    public float getMaxRange() {
        return this.maxRange;
    }

    public void setMaxRange(float maxRange) {
        this.maxRange = maxRange;
    }

    public float getRecoilDelay() {
        return this.recoilDelay;
    }

    public void setRecoilDelay(float recoilDelay) {
        this.recoilDelay = recoilDelay;
    }

    public int getClipSize() {
        return this.clipSize;
    }

    public void setClipSize(int clipSize) {
        this.clipSize = clipSize;
    }

    public float getDamage() {
        return this.damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public List<String> getMountOn() {
        return this.mountOn;
    }

    public void setMountOn(List<String> mountOn) {
        this.mountOn.clear();
        this.mountOnDisplayName.clear();

        for (int i = 0; i < mountOn.size(); i++) {
            String weaponType = mountOn.get(i);
            if (!weaponType.contains(".")) {
                weaponType = this.getModule() + "." + weaponType;
            }

            Item scriptItem = ScriptManager.instance.getItem(weaponType);
            if (scriptItem != null) {
                this.mountOn.add(scriptItem.getFullName());
                this.mountOnDisplayName.add(scriptItem.getDisplayName());
            }
        }
    }

    public String getPartType() {
        return this.partType;
    }

    public void setPartType(String partType) {
        this.partType = partType;
    }

    public int getReloadTime() {
        return this.reloadTime;
    }

    public void setReloadTime(int reloadTime) {
        this.reloadTime = reloadTime;
    }

    public int getAimingTime() {
        return this.aimingTime;
    }

    public void setAimingTime(int aimingTime) {
        this.aimingTime = aimingTime;
    }

    public int getHitChance() {
        return this.hitChance;
    }

    public void setHitChance(int hitChance) {
        this.hitChance = hitChance;
    }

    public float getAngle() {
        return this.angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public float getSpreadModifier() {
        return this.spreadModifier;
    }

    public void setSpreadModifier(float modifier) {
        this.spreadModifier = modifier;
    }

    public float getWeightModifier() {
        return this.weightModifier;
    }

    public void setWeightModifier(float weightModifier) {
        this.weightModifier = weightModifier;
    }

    public void setCanAttachCallback(String value) {
        this.canAttachCallback = value;
    }

    public boolean canAttach(IsoGameCharacter character, HandWeapon weapon) {
        if (weapon == null || weapon.getAllWeaponParts().size() > 100) {
            return false;
        } else if (!this.mountOn.isEmpty() && !this.mountOn.contains(weapon.getFullType())) {
            return false;
        } else if (StringUtils.isNullOrEmpty(this.canAttachCallback)) {
            return true;
        } else {
            Object functionObject = LuaManager.getFunctionObject(this.canAttachCallback, null);
            if (functionObject == null) {
                return weapon.getWeaponPart(this) == null;
            } else {
                Boolean aBoolean = LuaManager.caller.protectedCallBoolean(LuaManager.thread, functionObject, character, weapon, this);
                return aBoolean != null && aBoolean;
            }
        }
    }

    public void setCanDetachCallback(String value) {
        this.canDetachCallback = value;
    }

    public boolean canDetach(IsoGameCharacter character, HandWeapon weapon) {
        if (StringUtils.isNullOrEmpty(this.canDetachCallback)) {
            return true;
        } else {
            Object functionObject = LuaManager.getFunctionObject(this.canDetachCallback, null);
            if (functionObject == null) {
                return weapon.getWeaponPart(this) == this;
            } else {
                Boolean aBoolean = LuaManager.caller.protectedCallBoolean(LuaManager.thread, functionObject, character, weapon, this);
                return aBoolean != null && aBoolean;
            }
        }
    }

    public void setOnAttachCallback(String value) {
        this.onAttachCallback = value;
    }

    public void onAttach(IsoGameCharacter character, HandWeapon weapon) {
        if (!StringUtils.isNullOrEmpty(this.onAttachCallback)) {
            Object functionObject = LuaManager.getFunctionObject(this.onAttachCallback, null);
            if (functionObject != null) {
                LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, character, weapon, this);
            }
        }
    }

    public void setOnDetachCallback(String value) {
        this.onDetachCallback = value;
    }

    public void onDetach(IsoGameCharacter character, HandWeapon weapon) {
        if (!StringUtils.isNullOrEmpty(this.onDetachCallback)) {
            Object functionObject = LuaManager.getFunctionObject(this.onDetachCallback, null);
            if (functionObject != null) {
                LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, character, weapon, this);
            }
        }
    }

    @Override
    public void render() {
    }

    @Override
    public int getMaxUses() {
        return (int)Math.floor(1.0F / this.useDelta);
    }

    @Deprecated
    public void setUsedDelta(float delta) {
        this.setCurrentUsesFloat(delta);
    }

    @Override
    public void setCurrentUsesFloat(float newUses) {
        newUses = PZMath.clamp(newUses, 0.0F, 1.0F);
        this.uses = Math.round(newUses / this.useDelta);
    }

    @Override
    public float getCurrentUsesFloat() {
        return this.uses * this.useDelta;
    }

    @Override
    public void setUseDelta(float useDelta) {
        this.useDelta = useDelta;
    }

    @Override
    public void update() {
        if (this.isActivated()) {
            if (this.uses <= 0) {
                this.setActivated(false);
                return;
            }

            int minutes = GameTime.instance.getMinutes() / 10 * 10;
            if (minutes != this.lastUpdateMinutes) {
                if (this.lastUpdateMinutes > -1) {
                    this.uses--;
                }

                this.lastUpdateMinutes = minutes;
            }
        }
    }
}
