// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.WornItems.WornItem;
import zombie.characters.WornItems.WornItems;
import zombie.characters.skills.PerkFactory;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.ui.ObjectTooltip;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleWindow;

@UsedFromLua
public class Clothing extends InventoryItem {
    private float temperature;
    private float insulation;
    private float windresistance;
    private float waterResistance;
    private HashMap<Integer, Clothing.ClothingPatch> patches;
    protected String spriteName;
    protected String palette;
    public float bloodLevel;
    private float dirtyness;
    private float wetness;
    private float weightWet;
    private float lastWetnessUpdate = -1.0F;
    private final String dirtyString = Translator.getText("IGUI_ClothingName_Dirty");
    private final String bloodyString = Translator.getText("IGUI_ClothingName_Bloody");
    private final String wetString = Translator.getText("IGUI_ClothingName_Wet");
    private final String soakedString = Translator.getText("IGUI_ClothingName_Soaked");
    private final String wornString = Translator.getText("IGUI_ClothingName_Worn");
    private final String brokenString = Translator.getText("Tooltip_broken");
    private int conditionLowerChance = 10;
    private float stompPower = 1.0F;
    private float runSpeedModifier = 1.0F;
    private float combatSpeedModifier = 1.0F;
    private Boolean removeOnBroken = false;
    private Boolean canHaveHoles = true;
    private float biteDefense;
    private float scratchDefense;
    private float bulletDefense;
    public static final int CONDITION_PER_HOLES = 3;
    private float neckProtectionModifier = 1.0F;
    private int chanceToFall;

    @Override
    public String getCategory() {
        return this.mainCategory != null ? this.mainCategory : "Clothing";
    }

    public Clothing(String module, String name, String itemType, String texName, String palette, String SpriteName) {
        super(module, name, itemType, texName);
        this.spriteName = SpriteName;
        this.col = new Color(Rand.Next(255), Rand.Next(255), Rand.Next(255));
        this.palette = palette;
        this.lastWetnessUpdate = (float)GameTime.getInstance().getWorldAgeHours();
        this.itemType = ItemType.CLOTHING;
    }

    public Clothing(String module, String name, String itemType, Item item, String palette, String SpriteName) {
        super(module, name, itemType, item);
        this.spriteName = SpriteName;
        this.col = new Color(Rand.Next(255), Rand.Next(255), Rand.Next(255));
        this.palette = palette;
        this.lastWetnessUpdate = (float)GameTime.getInstance().getWorldAgeHours();
        this.itemType = ItemType.CLOTHING;
    }

    @Override
    public boolean IsClothing() {
        return true;
    }

    public void Unwear() {
        this.Unwear(false);
    }

    public void Unwear(boolean drop) {
        if (this.isWorn()) {
            if (this.container != null && this.container.parent instanceof IsoGameCharacter c) {
                c.removeWornItem(this);
                if (c instanceof IsoPlayer) {
                    LuaEventManager.triggerEvent("OnClothingUpdated", c);
                }

                if (drop && c.getSquare() != null && c.getVehicle() == null) {
                    c.getInventory().Remove(this);
                    c.getSquare().AddWorldInventoryItem(this, (float)(Rand.Next(100) / 100), (float)(Rand.Next(100) / 100), 0.0F);
                    LuaEventManager.triggerEvent("OnContainerUpdate");
                }

                IsoWorld.instance.currentCell.addToProcessItemsRemove(this);
            }
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        ColorInfo G2Bgrad = new ColorInfo();
        ColorInfo B2Ggrad = new ColorInfo();
        float tr = 1.0F;
        float tg = 1.0F;
        float tb = 0.8F;
        float ta = 1.0F;
        ColorInfo highlightGood = Core.getInstance().getGoodHighlitedColor();
        ColorInfo highlightBad = Core.getInstance().getBadHighlitedColor();
        float goodR = highlightGood.getR();
        float goodG = highlightGood.getG();
        float goodB = highlightGood.getB();
        float badR = highlightBad.getR();
        float badG = highlightBad.getG();
        float badB = highlightBad.getB();
        if (!this.isCosmetic()) {
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_weapon_Condition") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            float f = (float)this.condition / this.conditionMax;
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, G2Bgrad);
            item.setProgress(f, G2Bgrad.getR(), G2Bgrad.getG(), G2Bgrad.getB(), 1.0F);
            item = layout.addItem();
            item.setLabel(Translator.getText("Tooltip_item_Insulation") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
            f = this.getInsulation();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, G2Bgrad);
            item.setProgress(f, G2Bgrad.getR(), G2Bgrad.getG(), G2Bgrad.getB(), 1.0F);
            f = this.getWindresistance();
            if (f > 0.0F) {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_item_Windresist") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
                Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, G2Bgrad);
                item.setProgress(f, G2Bgrad.getR(), G2Bgrad.getG(), G2Bgrad.getB(), 1.0F);
            }

            f = this.getWaterResistance();
            if (f > 0.0F) {
                item = layout.addItem();
                item.setLabel(Translator.getText("Tooltip_item_Waterresist") + ": ", 1.0F, 1.0F, 0.8F, 1.0F);
                Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), f, G2Bgrad);
                item.setProgress(f, G2Bgrad.getR(), G2Bgrad.getG(), G2Bgrad.getB(), 1.0F);
            }
        }

        if (this.bloodLevel != 0.0F) {
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_clothing_bloody") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            float fx = this.bloodLevel / 100.0F;
            Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), fx, B2Ggrad);
            itemx.setProgress(fx, B2Ggrad.getR(), B2Ggrad.getG(), B2Ggrad.getB(), 1.0F);
        }

        if (this.dirtyness >= 1.0F) {
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_clothing_dirty") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            float fx = this.dirtyness / 100.0F;
            Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), fx, B2Ggrad);
            itemx.setProgress(fx, B2Ggrad.getR(), B2Ggrad.getG(), B2Ggrad.getB(), 1.0F);
        }

        if (this.wetness != 0.0F) {
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_clothing_wet") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            float fx = this.wetness / 100.0F;
            Core.getInstance().getGoodHighlitedColor().interp(Core.getInstance().getBadHighlitedColor(), fx, B2Ggrad);
            itemx.setProgress(fx, B2Ggrad.getR(), B2Ggrad.getG(), B2Ggrad.getB(), 1.0F);
        }

        int numHoles = 0;
        ItemVisual itemVisual = this.getVisual();

        for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
            if (itemVisual.getHole(BloodBodyPartType.FromIndex(i)) > 0.0F) {
                numHoles++;
            }
        }

        if (numHoles > 0) {
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_clothing_holes") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            itemx.setValueRightNoPlus(numHoles);
        }

        if (!this.isEquipped() && tooltipUI.getCharacter() != null) {
            float previousBiteDefense = 0.0F;
            float previousScratchDefense = 0.0F;
            float previousBulletDefense = 0.0F;
            WornItems wornItems = tooltipUI.getCharacter().getWornItems();

            for (int x = 0; x < wornItems.size(); x++) {
                WornItem wornItem = wornItems.get(x);
                if (wornItem.getItem().IsClothing()
                    && wornItem.getLocation() != null
                    && (
                        this.getBodyLocation().equals(wornItem.getLocation())
                            || wornItems.getBodyLocationGroup().isExclusive(this.getBodyLocation(), wornItem.getLocation())
                    )) {
                    previousBiteDefense += ((Clothing)wornItem.getItem()).getBiteDefense();
                    previousScratchDefense += ((Clothing)wornItem.getItem()).getScratchDefense();
                    previousBulletDefense += ((Clothing)wornItem.getItem()).getBulletDefense();
                }
            }

            float newBiteDefense = this.getBiteDefense();
            if (newBiteDefense != previousBiteDefense) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                if (newBiteDefense > 0.0F || previousBiteDefense > 0.0F) {
                    itemx.setLabel(Translator.getText("Tooltip_BiteDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    if (newBiteDefense > previousBiteDefense) {
                        itemx.setValue(
                            (int)newBiteDefense + " (+" + (int)(newBiteDefense - previousBiteDefense) + ")",
                            Core.getInstance().getGoodHighlitedColor().getR(),
                            Core.getInstance().getGoodHighlitedColor().getG(),
                            Core.getInstance().getGoodHighlitedColor().getB(),
                            1.0F
                        );
                    } else {
                        itemx.setValue(
                            (int)newBiteDefense + " (-" + (int)(previousBiteDefense - newBiteDefense) + ")",
                            Core.getInstance().getBadHighlitedColor().getR(),
                            Core.getInstance().getBadHighlitedColor().getG(),
                            Core.getInstance().getBadHighlitedColor().getB(),
                            1.0F
                        );
                    }
                }
            } else if (this.getBiteDefense() != 0.0F) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                itemx.setLabel(Translator.getText("Tooltip_BiteDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                itemx.setValueRightNoPlus((int)this.getBiteDefense());
            }

            float newScratchDefense = this.getScratchDefense();
            if (newScratchDefense != previousScratchDefense) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                if (newScratchDefense > 0.0F || previousScratchDefense > 0.0F) {
                    itemx.setLabel(Translator.getText("Tooltip_ScratchDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    if (newScratchDefense > previousScratchDefense) {
                        itemx.setValue(
                            (int)newScratchDefense + " (+" + (int)(newScratchDefense - previousScratchDefense) + ")",
                            Core.getInstance().getGoodHighlitedColor().getR(),
                            Core.getInstance().getGoodHighlitedColor().getG(),
                            Core.getInstance().getGoodHighlitedColor().getB(),
                            1.0F
                        );
                    } else {
                        itemx.setValue(
                            (int)newScratchDefense + " (-" + (int)(previousScratchDefense - newScratchDefense) + ")",
                            Core.getInstance().getBadHighlitedColor().getR(),
                            Core.getInstance().getBadHighlitedColor().getG(),
                            Core.getInstance().getBadHighlitedColor().getB(),
                            1.0F
                        );
                    }
                }
            } else if (this.getScratchDefense() != 0.0F) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                itemx.setLabel(Translator.getText("Tooltip_ScratchDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                itemx.setValueRightNoPlus((int)this.getScratchDefense());
            }

            float newBulletDefense = this.getBulletDefense();
            if (newBulletDefense != previousBulletDefense) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                if (newBulletDefense > 0.0F || previousBulletDefense > 0.0F) {
                    itemx.setLabel(Translator.getText("Tooltip_BulletDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    if (newBulletDefense > previousBulletDefense) {
                        itemx.setValue(
                            (int)newBulletDefense + " (+" + (int)(newBulletDefense - previousBulletDefense) + ")",
                            Core.getInstance().getGoodHighlitedColor().getR(),
                            Core.getInstance().getGoodHighlitedColor().getG(),
                            Core.getInstance().getGoodHighlitedColor().getB(),
                            1.0F
                        );
                    } else {
                        itemx.setValue(
                            (int)newBulletDefense + " (-" + (int)(previousBulletDefense - newBulletDefense) + ")",
                            Core.getInstance().getBadHighlitedColor().getR(),
                            Core.getInstance().getBadHighlitedColor().getG(),
                            Core.getInstance().getBadHighlitedColor().getB(),
                            1.0F
                        );
                    }
                }
            } else if (this.getBulletDefense() != 0.0F) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                itemx.setLabel(Translator.getText("Tooltip_BulletDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                itemx.setValueRightNoPlus((int)this.getBulletDefense());
            }
        } else {
            if (this.getBiteDefense() != 0.0F) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                itemx.setLabel(Translator.getText("Tooltip_BiteDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                itemx.setValueRightNoPlus((int)this.getBiteDefense());
            }

            if (this.getScratchDefense() != 0.0F) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                itemx.setLabel(Translator.getText("Tooltip_ScratchDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                itemx.setValueRightNoPlus((int)this.getScratchDefense());
            }

            if (this.getBulletDefense() != 0.0F) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                itemx.setLabel(Translator.getText("Tooltip_BulletDefense") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                itemx.setValueRightNoPlus((int)this.getBulletDefense());
            }
        }

        if (!this.hasTag(ItemTag.GAS_MASK) && !this.hasTag(ItemTag.RESPIRATOR)) {
            if (this.hasTag(ItemTag.GAS_MASK_NO_FILTER) || this.hasTag(ItemTag.RESPIRATOR_NO_FILTER)) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                itemx.setLabel(Translator.getText("Tooltip_NoFilter"), 1.0F, 1.0F, 0.8F, 1.0F);
            }
        } else if (this.hasFilter()) {
            String filterName = ScriptManager.instance.getItem(this.getFilterType()).getDisplayName();
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText(filterName) + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            float filterValue = this.getUsedDelta();
            Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), filterValue, G2Bgrad);
            itemx.setProgress(filterValue, G2Bgrad.getR(), G2Bgrad.getG(), G2Bgrad.getB(), 1.0F);
        } else {
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_NoFilter"), 1.0F, 1.0F, 0.8F, 1.0F);
        }

        if (this.hasTag(ItemTag.SCBA)) {
            if (this.hasTank()) {
                String tankName = ScriptManager.instance.getItem(this.getTankType()).getDisplayName();
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                itemx.setLabel(Translator.getText(tankName) + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                float tankValue = this.getUsedDelta();
                Core.getInstance().getBadHighlitedColor().interp(Core.getInstance().getGoodHighlitedColor(), tankValue, G2Bgrad);
                itemx.setProgress(tankValue, G2Bgrad.getR(), G2Bgrad.getG(), G2Bgrad.getB(), 1.0F);
            } else {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                itemx.setLabel(Translator.getText("Tooltip_NoTank"), 1.0F, 1.0F, 0.8F, 1.0F);
            }
        } else if (this.hasTag(ItemTag.SCBANO_TANK)) {
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_NoTank"), 1.0F, 1.0F, 0.8F, 1.0F);
        }

        if (this.getRunSpeedModifier() != 1.0F) {
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_RunSpeedModifier") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            if (this.getRunSpeedModifier() > 1.0F) {
                itemx.setProgress(this.getRunSpeedModifier() - 1.0F, goodR, goodG, goodB, 1.0F);
            } else {
                itemx.setProgress(1.0F - this.getRunSpeedModifier(), badR, badG, badB, 1.0F);
            }
        }

        if (this.getCombatSpeedModifier() != 1.0F) {
            ObjectTooltip.LayoutItem itemx = layout.addItem();
            itemx.setLabel(Translator.getText("Tooltip_CombatSpeedModifier") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            if (this.getRunSpeedModifier() > 1.0F) {
                itemx.setProgress(this.getCombatSpeedModifier() - 1.0F, goodR, goodG, goodB, 1.0F);
            } else {
                itemx.setProgress(1.0F - this.getCombatSpeedModifier(), badR, badG, badB, 1.0F);
            }
        }

        if (Core.debug && DebugOptions.instance.tooltipInfo.getValue()) {
            if (this.bloodLevel != 0.0F) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                itemx.setLabel("DBG: bloodLevel:", 1.0F, 1.0F, 0.8F, 1.0F);
                int value = (int)Math.ceil(this.bloodLevel);
                itemx.setValueRight(value, false);
            }

            if (this.dirtyness != 0.0F) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                itemx.setLabel("DBG: dirtyness:", 1.0F, 1.0F, 0.8F, 1.0F);
                int value = (int)Math.ceil(this.dirtyness);
                itemx.setValueRight(value, false);
            }

            if (this.wetness != 0.0F) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                itemx.setLabel("DBG: wetness:", 1.0F, 1.0F, 0.8F, 1.0F);
                int value = (int)Math.ceil(this.wetness);
                itemx.setValueRight(value, false);
            }
        }
    }

    public boolean isDirty() {
        return this.dirtyness > 15.0F;
    }

    @Override
    public boolean isBloody() {
        return this.getBloodlevel() > 25.0F;
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return this.getName(null);
    }

    @Override
    public String getName(IsoPlayer player) {
        String prefix = "";
        if (this.isDirty()) {
            prefix = prefix + this.dirtyString + ", ";
        }

        if (this.isBloody()) {
            prefix = prefix + this.bloodyString + ", ";
        }

        if (this.getWetness() >= 100.0F) {
            prefix = prefix + this.soakedString + ", ";
        } else if (this.getWetness() > 25.0F) {
            prefix = prefix + this.wetString + ", ";
        }

        if (this.isBroken()) {
            prefix = prefix + this.brokenString + ", ";
        } else if (this.getCondition() < this.getConditionMax() / 3.0F) {
            prefix = prefix + this.wornString + ", ";
        }

        if (prefix.length() > 2) {
            prefix = prefix.substring(0, prefix.length() - 2);
        }

        prefix = prefix.trim();
        if (this.getFluidContainer() != null) {
            return this.getFluidContainer().getUiName();
        } else {
            return prefix.isEmpty() ? this.name : Translator.getText("IGUI_ClothingNaming", prefix, this.name);
        }
    }

    @Override
    public void update() {
        if (this.isActivated() && !this.isWorn()) {
            this.setActivated(false);
        }
    }

    public void updateWetness() {
        this.updateWetness(false);
    }

    public void updateWetness(boolean bIgnoreEquipped) {
        if (bIgnoreEquipped || !this.isEquipped()) {
            if (this.getBloodClothingType() == null) {
                this.setWetness(0.0F);
            } else {
                float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();
                if (this.lastWetnessUpdate < 0.0F) {
                    this.lastWetnessUpdate = worldAgeHours;
                } else if (this.lastWetnessUpdate > worldAgeHours) {
                    this.lastWetnessUpdate = worldAgeHours;
                }

                float elapsed = worldAgeHours - this.lastWetnessUpdate;
                if (!(elapsed < 0.016666668F)) {
                    this.lastWetnessUpdate = worldAgeHours;
                    if (this.hasTag(ItemTag.BREAK_WHEN_WET) && this.getWetness() >= 100.0F) {
                        this.setCondition(0);
                    }

                    switch (this.getWetDryState()) {
                        case Invalid:
                        default:
                            break;
                        case Dryer:
                            if (this.getWetness() > 0.0F) {
                                float dryAmount = elapsed * 20.0F;
                                if (this.isEquipped()) {
                                    dryAmount *= 2.0F;
                                }

                                this.setWetness(this.getWetness() - dryAmount);
                            }
                            break;
                        case Wetter:
                            if (this.getWetness() < 100.0F) {
                                float intensity = ClimateManager.getInstance().getRainIntensity();
                                if (intensity < 0.1F) {
                                    intensity = 0.0F;
                                }

                                float wetAmount = intensity * elapsed * 100.0F;
                                this.setWetness(this.getWetness() + wetAmount);
                            }
                    }
                }
            }
        }
    }

    public float getBulletDefense() {
        return this.getCondition() <= 0 ? 0.0F : this.bulletDefense;
    }

    public void setBulletDefense(float bulletDefense) {
        this.bulletDefense = Math.min(bulletDefense, 100.0F);
    }

    private Clothing.WetDryState getWetDryState() {
        if (this.getWorldItem() != null) {
            if (this.getWorldItem().getSquare() == null) {
                return Clothing.WetDryState.Invalid;
            } else if (this.getWorldItem().getSquare().isInARoom()) {
                return Clothing.WetDryState.Dryer;
            } else {
                return ClimateManager.getInstance().isRaining() ? Clothing.WetDryState.Wetter : Clothing.WetDryState.Dryer;
            }
        } else if (this.container == null) {
            return Clothing.WetDryState.Invalid;
        } else if (this.container.parent instanceof IsoDeadBody chr) {
            if (chr.getSquare() == null) {
                return Clothing.WetDryState.Invalid;
            } else if (chr.getSquare().isInARoom()) {
                return Clothing.WetDryState.Dryer;
            } else {
                return ClimateManager.getInstance().isRaining() ? Clothing.WetDryState.Wetter : Clothing.WetDryState.Dryer;
            }
        } else if (this.container.parent instanceof IsoGameCharacter chrx) {
            if (chrx.getCurrentSquare() == null) {
                return Clothing.WetDryState.Invalid;
            } else if (chrx.getCurrentSquare().isInARoom() || chrx.getCurrentSquare().haveRoof) {
                return Clothing.WetDryState.Dryer;
            } else if (!ClimateManager.getInstance().isRaining()) {
                return Clothing.WetDryState.Dryer;
            } else if (!this.isEquipped()) {
                return Clothing.WetDryState.Dryer;
            } else if (!chrx.isAsleep() && !chrx.isResting()
                || chrx.getBed() == null
                || !chrx.getBed().isTent() && !"Tent".equalsIgnoreCase(chrx.getBed().getName()) && !"Shelter".equalsIgnoreCase(chrx.getBed().getName())) {
                BaseVehicle vehicle = chrx.getVehicle();
                if (vehicle != null && vehicle.hasRoof(vehicle.getSeat(chrx))) {
                    VehiclePart windshield = vehicle.getPartById("Windshield");
                    if (windshield != null) {
                        VehicleWindow window = windshield.getWindow();
                        if (window != null && window.isHittable()) {
                            return Clothing.WetDryState.Dryer;
                        }
                    }
                }

                return Clothing.WetDryState.Wetter;
            } else {
                return Clothing.WetDryState.Dryer;
            }
        } else if (this.container.parent == null) {
            return Clothing.WetDryState.Dryer;
        } else if (this.container.parent instanceof IsoClothingDryer isoClothingDryer && isoClothingDryer.isActivated()) {
            return Clothing.WetDryState.Invalid;
        } else if (this.container.parent instanceof IsoClothingWasher isoClothingWasher && isoClothingWasher.isActivated()) {
            return Clothing.WetDryState.Invalid;
        } else {
            IsoCombinationWasherDryer washerDryer = Type.tryCastTo(this.container.parent, IsoCombinationWasherDryer.class);
            return washerDryer != null && washerDryer.isActivated() ? Clothing.WetDryState.Invalid : Clothing.WetDryState.Dryer;
        }
    }

    public void flushWetness() {
        if (!(this.lastWetnessUpdate < 0.0F)) {
            this.updateWetness(true);
            this.lastWetnessUpdate = -1.0F;
        }
    }

    @Override
    public boolean finishupdate() {
        return this.container != null && this.container.parent instanceof IsoGameCharacter ? !this.isEquipped() : true;
    }

    public void Use(boolean bCrafting, boolean bInContainer) {
        if (this.uses <= 1) {
            this.Unwear();
        }

        this.Use(bCrafting, bInContainer, false);
    }

    @Override
    public boolean CanStack(InventoryItem item) {
        return this.ModDataMatches(item) && this.palette == null && ((Clothing)item).palette == null || this.palette.equals(((Clothing)item).palette);
    }

    public static Clothing CreateFromSprite(String Sprite) {
        try {
            Clothing clothing = null;
            return (Clothing)InventoryItemFactory.CreateItem(Sprite, 1.0F);
        } catch (Exception var2) {
            return null;
        }
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, output);
        if (this.getSpriteName() != null) {
            bits.addFlags(1);
            GameWindow.WriteString(output, this.getSpriteName());
        }

        if (this.dirtyness != 0.0F) {
            bits.addFlags(2);
            output.putFloat(this.dirtyness);
        }

        if (this.bloodLevel != 0.0F) {
            bits.addFlags(4);
            output.putFloat(this.bloodLevel);
        }

        if (this.wetness != 0.0F) {
            bits.addFlags(8);
            output.putFloat(this.wetness);
        }

        if (this.lastWetnessUpdate != 0.0F) {
            bits.addFlags(16);
            output.putFloat(this.lastWetnessUpdate);
        }

        if (this.patches != null) {
            bits.addFlags(32);
            output.put((byte)this.patches.size());

            for (int partIndex : this.patches.keySet()) {
                output.put((byte)partIndex);
                this.patches.get(partIndex).save(output, false);
            }
        }

        bits.write();
        bits.release();
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
        if (!bits.equals(0)) {
            if (bits.hasFlags(1)) {
                this.setSpriteName(GameWindow.ReadString(input));
            }

            if (bits.hasFlags(2)) {
                this.dirtyness = input.getFloat();
            }

            if (bits.hasFlags(4)) {
                this.bloodLevel = input.getFloat();
            }

            if (bits.hasFlags(8)) {
                this.wetness = input.getFloat();
            }

            if (bits.hasFlags(16)) {
                this.lastWetnessUpdate = input.getFloat();
            }

            if (bits.hasFlags(32)) {
                int patchNbr = input.get();

                for (int i = 0; i < patchNbr; i++) {
                    int partIndex = input.get();
                    Clothing.ClothingPatch patch = new Clothing.ClothingPatch();
                    patch.load(input, WorldVersion);
                    if (this.patches == null) {
                        this.patches = new HashMap<>();
                    }

                    this.patches.put(partIndex, patch);
                }
            }
        }

        bits.release();
        this.synchWithVisual();
        this.lastWetnessUpdate = (float)GameTime.getInstance().getWorldAgeHours();
    }

    /**
     * @return the SpriteName
     */
    public String getSpriteName() {
        return this.spriteName;
    }

    /**
     * 
     * @param SpriteName the SpriteName to set
     */
    public void setSpriteName(String SpriteName) {
        this.spriteName = SpriteName;
    }

    /**
     * @return the palette
     */
    public String getPalette() {
        return this.palette == null ? "Trousers_White" : this.palette;
    }

    /**
     * 
     * @param palette the palette to set
     */
    public void setPalette(String palette) {
        this.palette = palette;
    }

    public float getTemperature() {
        return this.temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public void setDirtyness(float delta) {
        this.dirtyness = PZMath.clamp(delta, 0.0F, 100.0F);
    }

    @Override
    public void setBloodLevel(float delta) {
        this.bloodLevel = PZMath.clamp(delta, 0.0F, 100.0F);
    }

    public float getDirtyness() {
        return this.dirtyness;
    }

    public float getBloodlevel() {
        return this.bloodLevel;
    }

    public float getBloodlevelForPart(BloodBodyPartType part) {
        return this.getVisual().getBlood(part);
    }

    @Override
    public float getBloodLevel() {
        return this.bloodLevel;
    }

    public float getBloodLevelForPart(BloodBodyPartType part) {
        return this.getVisual().getBlood(part);
    }

    /**
     * @return the Weight
     */
    @Override
    public float getWeight() {
        float weight = this.getActualWeight();
        float weightWet = this.getWeightWet();
        if (weightWet <= 0.0F) {
            weightWet = weight * 1.25F;
        }

        return PZMath.lerp(weight, weightWet, this.getWetness() / 100.0F);
    }

    public void setWetness(float percent) {
        this.wetness = PZMath.clamp(percent, 0.0F, 100.0F);
    }

    @Override
    public float getWetness() {
        return this.wetness;
    }

    public float getWeightWet() {
        return this.weightWet;
    }

    public void setWeightWet(float weight) {
        this.weightWet = weight;
    }

    @Override
    public int getConditionLowerChance() {
        return this.conditionLowerChance;
    }

    public void setConditionLowerChance(int conditionLowerChance) {
        this.conditionLowerChance = conditionLowerChance;
    }

    /**
     * 
     * @param Condition the Condition to set
     */
    @Override
    public void setCondition(int Condition) {
        this.setCondition(Condition, true);
        if (Condition <= 0) {
            this.setBroken(true);
            if (this.getContainer() != null) {
                this.getContainer().setDrawDirty(true);
            }

            if (this.isWorn() && this.isRemoveOnBroken()) {
                this.Unwear(true);
            }
        }
    }

    public float getClothingDirtynessIncreaseLevel() {
        if (SandboxOptions.instance.clothingDegradation.getValue() == 2) {
            return 2.5E-4F;
        } else {
            return SandboxOptions.instance.clothingDegradation.getValue() == 4 ? 0.025F : 0.0025F;
        }
    }

    public float getInsulation() {
        return this.insulation;
    }

    public void setInsulation(float insulation) {
        this.insulation = insulation;
    }

    public float getStompPower() {
        return this.stompPower;
    }

    public void setStompPower(float stompPower) {
        this.stompPower = stompPower;
    }

    public float getRunSpeedModifier() {
        return this.runSpeedModifier;
    }

    public void setRunSpeedModifier(float runSpeedModifier) {
        this.runSpeedModifier = runSpeedModifier;
    }

    public float getCombatSpeedModifier() {
        return this.combatSpeedModifier;
    }

    public void setCombatSpeedModifier(float combatSpeedModifier) {
        this.combatSpeedModifier = combatSpeedModifier;
    }

    public Boolean isRemoveOnBroken() {
        return this.removeOnBroken;
    }

    public void setRemoveOnBroken(Boolean removeOnBroken) {
        this.removeOnBroken = removeOnBroken;
    }

    public Boolean getCanHaveHoles() {
        return this.canHaveHoles;
    }

    public void setCanHaveHoles(Boolean canHaveHoles) {
        this.canHaveHoles = canHaveHoles;
    }

    public boolean isCosmetic() {
        return this.getScriptItem().isCosmetic();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{ clothingItemName=\"" + this.getClothingItemName() + "\" }";
    }

    public float getBiteDefense() {
        return this.getCondition() <= 0 ? 0.0F : this.biteDefense;
    }

    public void setBiteDefense(float biteDefense) {
        this.biteDefense = Math.min(biteDefense, 100.0F);
    }

    public float getScratchDefense() {
        return this.getCondition() <= 0 ? 0.0F : this.scratchDefense;
    }

    public void setScratchDefense(float scratchDefense) {
        this.scratchDefense = Math.min(scratchDefense, 100.0F);
    }

    public float getNeckProtectionModifier() {
        return this.neckProtectionModifier;
    }

    public void setNeckProtectionModifier(float neckProtectionModifier) {
        this.neckProtectionModifier = neckProtectionModifier;
    }

    public int getChanceToFall() {
        return this.chanceToFall;
    }

    public void setChanceToFall(int chanceToFall) {
        this.chanceToFall = chanceToFall;
    }

    public float getWindresistance() {
        return this.windresistance;
    }

    public void setWindresistance(float windresistance) {
        this.windresistance = windresistance;
    }

    public float getWaterResistance() {
        return this.waterResistance;
    }

    public void setWaterResistance(float waterResistance) {
        this.waterResistance = waterResistance;
    }

    public int getHolesNumber() {
        return this.getVisual() != null ? this.getVisual().getHolesNumber() : 0;
    }

    public int getPatchesNumber() {
        return this.patches == null ? 0 : this.patches.size();
    }

    public float getDefForPart(BloodBodyPartType part, boolean bite, boolean bullet) {
        if (this.getVisual().getHole(part) > 0.0F) {
            return 0.0F;
        } else {
            Clothing.ClothingPatch patch = this.getPatchType(part);
            float defense = this.getScratchDefense();
            if (bite) {
                defense = this.getBiteDefense();
            }

            if (bullet) {
                defense = this.getBulletDefense();
            }

            if (part == BloodBodyPartType.Neck && this.getScriptItem().neckProtectionModifier < 1.0F) {
                defense *= this.getScriptItem().neckProtectionModifier;
            }

            if (patch != null) {
                int patchDefense = patch.scratchDefense;
                if (bite) {
                    patchDefense = patch.biteDefense;
                }

                if (bullet) {
                    patchDefense = patch.biteDefense;
                }

                if (!patch.hasHole) {
                    defense += patchDefense;
                } else {
                    defense = patchDefense;
                }
            }

            return defense;
        }
    }

    /**
     * Used from lua tooltip when repairing clothing
     */
    public static int getBiteDefenseFromItem(IsoGameCharacter chr, InventoryItem fabric) {
        int tailorLvl = Math.max(1, chr.getPerkLevel(PerkFactory.Perks.Tailoring));
        Clothing.ClothingPatchFabricType type = Clothing.ClothingPatchFabricType.fromType(fabric.getFabricType());
        return type.maxBiteDef > 0 ? (int)Math.max(1.0F, type.maxBiteDef * (tailorLvl / 10.0F)) : 0;
    }

    /**
     * Used from lua tooltip when repairing clothing
     */
    public static int getScratchDefenseFromItem(IsoGameCharacter chr, InventoryItem fabric) {
        int tailorLvl = Math.max(1, chr.getPerkLevel(PerkFactory.Perks.Tailoring));
        Clothing.ClothingPatchFabricType type = Clothing.ClothingPatchFabricType.fromType(fabric.getFabricType());
        return (int)Math.max(1.0F, type.maxScratchDef * (tailorLvl / 10.0F));
    }

    public Clothing.ClothingPatch getPatchType(BloodBodyPartType part) {
        return this.patches != null ? this.patches.get(part.index()) : null;
    }

    public void removePatch(BloodBodyPartType part) {
        if (this.patches != null) {
            this.getVisual().removePatch(part.index());
            Clothing.ClothingPatch patch = this.patches.get(part.index());
            if (patch != null && patch.hasHole) {
                this.getVisual().setHole(part);
                this.setCondition(this.getCondition() - patch.conditionGain, false);
            }

            this.patches.remove(part.index());
            if (GameServer.server) {
                IsoPlayer var4 = this.getPlayer();
                if (var4 instanceof IsoPlayer && var4.isEquippedClothing(this)) {
                    INetworkPacket.sendToAll(PacketTypes.PacketType.SyncClothing, var4);
                    INetworkPacket.sendToAll(PacketTypes.PacketType.SyncVisuals, var4);
                }
            }
        }
    }

    public void removeAllPatches() {
        if (this.patches != null) {
            ItemVisual itemVisual = this.getVisual();

            for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
                BloodBodyPartType part = BloodBodyPartType.FromIndex(i);
                Clothing.ClothingPatch patch = this.patches.remove(part.index());
                if (patch != null && itemVisual != null) {
                    if (patch.hasHole) {
                        itemVisual.setHole(part);
                    }

                    itemVisual.removePatch(i);
                }
            }
        }
    }

    public boolean canFullyRestore(IsoGameCharacter chr, BloodBodyPartType part, InventoryItem fabric) {
        return chr.getPerkLevel(PerkFactory.Perks.Tailoring) > 7
            && fabric.getFabricType().equals(this.getFabricType())
            && this.getVisual().getHole(part) > 0.0F;
    }

    public void fullyRestore() {
        this.setCondition(this.getConditionMax());
        this.setDirtyness(0.0F);
        this.setBloodLevel(0.0F);

        for (int i = 0; i < BloodBodyPartType.MAX.index(); i++) {
            BloodBodyPartType part = BloodBodyPartType.FromIndex(i);
            if (this.patches != null) {
                this.getVisual().removePatch(i);
                this.patches.remove(part.index());
            }

            if (this.getVisual().getHole(part) != 0.0F) {
                this.getVisual().removeHole(i);
            }

            this.getVisual().setBlood(part, 0.0F);
        }

        if (GameServer.server) {
            INetworkPacket.sendToAll(PacketTypes.PacketType.SyncClothing, this.getOwner());
        }
    }

    public void addPatchForSync(int partIdx, int tailorLvl, int fabricType, boolean hasHole) {
        if (this.patches == null) {
            this.patches = new HashMap<>();
        }

        Clothing.ClothingPatch patch = new Clothing.ClothingPatch(tailorLvl, fabricType, hasHole);
        this.patches.put(partIdx, patch);
    }

    public void addPatch(IsoGameCharacter chr, BloodBodyPartType part, InventoryItem fabric) {
        Clothing.ClothingPatchFabricType type = Clothing.ClothingPatchFabricType.fromType(fabric.getFabricType());
        if (this.canFullyRestore(chr, part, fabric)) {
            this.getVisual().removeHole(part.index());
            this.setCondition((int)(this.getCondition() + this.getCondLossPerHole()), false);
        } else {
            if (type == Clothing.ClothingPatchFabricType.Cotton) {
                this.getVisual().setBasicPatch(part);
            } else if (type == Clothing.ClothingPatchFabricType.Denim) {
                this.getVisual().setDenimPatch(part);
            } else {
                this.getVisual().setLeatherPatch(part);
            }

            if (this.patches == null) {
                this.patches = new HashMap<>();
            }

            int tailorLvl = Math.max(1, chr.getPerkLevel(PerkFactory.Perks.Tailoring));
            float hole = this.getVisual().getHole(part);
            float conditionGain = this.getCondLossPerHole();
            if (tailorLvl < 3) {
                conditionGain -= 2.0F;
            } else if (tailorLvl < 6) {
                conditionGain--;
            }

            Clothing.ClothingPatch patch = new Clothing.ClothingPatch(tailorLvl, type.index, hole > 0.0F);
            if (hole > 0.0F) {
                conditionGain = Math.max(1.0F, conditionGain);
                this.setCondition((int)(this.getCondition() + conditionGain), false);
                patch.conditionGain = (int)conditionGain;
            }

            this.patches.put(part.index(), patch);
            this.getVisual().removeHole(part.index());
            if (GameServer.server && chr instanceof IsoPlayer player && player.isEquippedClothing(this)) {
                INetworkPacket.sendToAll(PacketTypes.PacketType.SyncClothing, chr);
                INetworkPacket.sendToAll(PacketTypes.PacketType.SyncVisuals, chr);
            }
        }
    }

    public ArrayList<BloodBodyPartType> getCoveredParts() {
        ArrayList<BloodClothingType> types = this.getScriptItem().getBloodClothingType();
        return BloodClothingType.getCoveredParts(types);
    }

    public int getNbrOfCoveredParts() {
        ArrayList<BloodClothingType> types = this.getScriptItem().getBloodClothingType();
        return BloodClothingType.getCoveredPartCount(types);
    }

    public float getCondLossPerHole() {
        int numberOfCoveredParts = this.getNbrOfCoveredParts();
        return PZMath.max(1, this.getConditionMax() / numberOfCoveredParts);
    }

    public void copyPatchesTo(Clothing newClothing) {
        newClothing.patches = this.patches;
    }

    public String getClothingExtraSubmenu() {
        return this.scriptItem.clothingExtraSubmenu;
    }

    public boolean canBe3DRender() {
        return !StringUtils.isNullOrEmpty(this.getWorldStaticItem())
            ? true
            : "Bip01_Head".equalsIgnoreCase(this.getClothingItem().attachBone) && (!this.isCosmetic() || this.isBodyLocation(ItemBodyLocation.EYES));
    }

    @Override
    public boolean isWorn() {
        return this.container != null && this.container.parent instanceof IsoGameCharacter isoGameCharacter && isoGameCharacter.getWornItems().contains(this);
    }

    public void addRandomHole() {
        if (this.getCanHaveHoles() && this.getCoveredParts() != null) {
            ArrayList<BloodBodyPartType> parts = this.getCoveredParts();
            BloodBodyPartType part = parts.get(Rand.Next(parts.size()));
            int holes = 0;
            if (this.getVisual().getHole(part) <= 0.0F) {
                this.getVisual().setHole(part);
                holes++;
            }

            this.setCondition(this.getCondition() - (int)(holes * this.getCondLossPerHole()), false);
        }
    }

    public void addRandomDirt() {
        if (this.getCoveredParts() != null) {
            ArrayList<BloodBodyPartType> parts = this.getCoveredParts();
            BloodBodyPartType part = parts.get(Rand.Next(parts.size()));
            int roll = Rand.Next(100) + 1;
            float dirtLevelPart = roll / 100.0F;
            this.getVisual().setDirt(part, dirtLevelPart);
            BloodClothingType.calcTotalDirtLevel(this);
        }
    }

    public void addRandomBlood() {
        if (this.getCoveredParts() != null) {
            ArrayList<BloodBodyPartType> parts = this.getCoveredParts();
            BloodBodyPartType part = parts.get(Rand.Next(parts.size()));
            int roll = Rand.Next(100) + 1;
            float bloodLevelPart = roll / 100.0F;
            this.getVisual().setBlood(part, bloodLevelPart);
            BloodClothingType.calcTotalBloodLevel(this);
        }
    }

    public void randomizeCondition(int wetChance, int dirtChance, int bloodChance, int holeChance) {
        if (!this.isCosmetic()) {
            if (Rand.Next(100) < wetChance) {
                this.setWetness(Rand.Next(0, 100));
            }

            int parts = this.getNbrOfCoveredParts();
            if (parts >= 1) {
                if (Rand.Next(100) < dirtChance) {
                    for (int i = 0; i < parts; i++) {
                        this.addRandomDirt();
                    }
                }

                if (Rand.Next(100) < bloodChance) {
                    for (int i = 0; i < parts; i++) {
                        this.addRandomBlood();
                    }
                }

                if (Rand.Next(100) < holeChance) {
                    if (this.getCanHaveHoles()) {
                        int rolls = Rand.Next(parts + 1);

                        for (int i = 0; i < rolls; i++) {
                            this.addRandomHole();
                        }
                    } else {
                        this.setCondition(Rand.Next(this.getCondition()) + 1, false);
                    }
                }
            }
        }
    }

    public boolean hasFilter() {
        return (this.hasTag(ItemTag.GAS_MASK) || this.hasTag(ItemTag.RESPIRATOR))
                && this.getModData().rawget("filterType") != null
                && this.getModData().rawget("filterType") != "none"
            ? this.getFilterType() != null
            : false;
    }

    public void setNoFilter() {
        if (this.hasTag(ItemTag.GAS_MASK) || this.hasTag(ItemTag.RESPIRATOR)) {
            this.getModData().rawset("filterType", null);
        }
    }

    public String getFilterType() {
        return this.getModData().rawget("filterType") != null && this.getModData().rawget("filterType") != "none"
            ? (String)this.getModData().rawget("filterType")
            : null;
    }

    public void setFilterType(String filterType) {
        this.getModData().rawset("filterType", filterType);
    }

    public boolean hasTank() {
        return this.hasTag(ItemTag.SCBA) && this.getModData().rawget("tankType") != null && this.getModData().rawget("tankType") != "none"
            ? this.getTankType() != null
            : false;
    }

    public void setNoTank() {
        if (this.hasTag(ItemTag.SCBA)) {
            this.getModData().rawset("tankType", null);
        }
    }

    public String getTankType() {
        return this.getModData().rawget("tankType") != null && this.getModData().rawget("tankType") != "none"
            ? (String)this.getModData().rawget("tankType")
            : null;
    }

    public void setTankType(String tankType) {
        this.getModData().rawset("tankType", tankType);
    }

    public float getUsedDelta() {
        if (this.getModData().rawget("usedDelta") == null) {
            this.setUsedDelta(0.0F);
        }

        return (float)((Double)this.getModData().rawget("usedDelta")).doubleValue();
    }

    public void setUsedDelta(float usedDelta) {
        if (usedDelta < 0.0F) {
            usedDelta = 0.0F;
        }

        this.getModData().rawset("usedDelta", (double)usedDelta);
    }

    @Override
    public float getUseDelta() {
        return this.getScriptItem().getUseDelta();
    }

    public void drainGasMask() {
        this.drainGasMask(1.0F);
    }

    public void drainGasMask(float rate) {
        if ((this.hasTag(ItemTag.GAS_MASK) || this.hasTag(ItemTag.RESPIRATOR)) && this.hasFilter() && !(this.getUsedDelta() <= 0.0F)) {
            float rate2 = rate * ScriptManager.instance.getItem(this.getFilterType()).getUseDelta() * GameTime.getInstance().getMultiplier();
            this.setUsedDelta(this.getUsedDelta() - rate2);
            if (this.getUsedDelta() < 0.0F) {
                this.setUsedDelta(0.0F);
            }
        }
    }

    public void drainSCBA() {
        if (this.getUsedDelta() <= 0.0F) {
            this.setActivated(false);
        }

        if (this.hasTag(ItemTag.SCBA) && this.hasTank() && this.isActivated() && !(this.getUsedDelta() <= 0.0F)) {
            float rate = 0.001F;
            float rate2 = 0.001F * ScriptManager.instance.getItem(this.getTankType()).getUseDelta() * GameTime.getInstance().getMultiplier();
            this.setUsedDelta(this.getUsedDelta() - rate2);
            if (this.getUsedDelta() < 0.0F) {
                this.setUsedDelta(0.0F);
            }

            if (this.getUsedDelta() <= 0.0F) {
                this.setActivated(false);
            }
        }
    }

    public float getCorpseSicknessDefense() {
        if (this.getCondition() <= 0) {
            return 0.0F;
        } else {
            float defense = this.getScriptItem().getCorpseSicknessDefense();
            if (this.hasFilter()) {
                float value = 25.0F;
                if (this.getUsedDelta() > 0.0F) {
                    value = 100.0F;
                }

                if (value > defense) {
                    defense = value;
                }
            }

            if (this.hasFilter() && 25.0F > defense) {
                defense = 25.0F;
            }

            return defense;
        }
    }

    @UsedFromLua
    public static class ClothingPatch {
        public int tailorLvl;
        public int fabricType;
        public int scratchDefense;
        public int biteDefense;
        public boolean hasHole;
        public int conditionGain;

        public String getFabricTypeName() {
            return Translator.getText("IGUI_FabricType_" + this.fabricType);
        }

        public int getScratchDefense() {
            return this.scratchDefense;
        }

        public int getBiteDefense() {
            return this.biteDefense;
        }

        public int getFabricType() {
            return this.fabricType;
        }

        public ClothingPatch() {
        }

        public ClothingPatch(int tailorLvl, int fabricType, boolean hasHole) {
            this.tailorLvl = tailorLvl;
            this.fabricType = fabricType;
            this.hasHole = hasHole;
            Clothing.ClothingPatchFabricType type = Clothing.ClothingPatchFabricType.fromIndex(fabricType);
            this.scratchDefense = (int)Math.max(1.0F, type.maxScratchDef * (tailorLvl / 10.0F));
            if (type.maxBiteDef > 0) {
                this.biteDefense = (int)Math.max(1.0F, type.maxBiteDef * (tailorLvl / 10.0F));
            }
        }

        public void save(ByteBuffer output, boolean net) throws IOException {
            output.put((byte)this.tailorLvl);
            output.put((byte)this.fabricType);
            output.put((byte)this.scratchDefense);
            output.put((byte)this.biteDefense);
            output.put((byte)(this.hasHole ? 1 : 0));
            output.putShort((short)this.conditionGain);
        }

        public void load(ByteBuffer input, int WorldVersion) throws IOException {
            this.tailorLvl = input.get();
            this.fabricType = input.get();
            this.scratchDefense = input.get();
            this.biteDefense = input.get();
            this.hasHole = input.get() == 1;
            this.conditionGain = input.getShort();
        }

        @Deprecated
        public void save_old(ByteBuffer output, boolean net) throws IOException {
            output.putInt(this.tailorLvl);
            output.putInt(this.fabricType);
            output.putInt(this.scratchDefense);
            output.putInt(this.biteDefense);
            output.put((byte)(this.hasHole ? 1 : 0));
            output.putInt(this.conditionGain);
        }

        @Deprecated
        public void load_old(ByteBuffer input, int WorldVersion, boolean net) throws IOException {
            this.tailorLvl = input.getInt();
            this.fabricType = input.getInt();
            this.scratchDefense = input.getInt();
            this.biteDefense = input.getInt();
            this.hasHole = input.get() == 1;
            this.conditionGain = input.getInt();
        }
    }

    @UsedFromLua
    public static enum ClothingPatchFabricType {
        Cotton(1, "Cotton", 5, 0),
        Denim(2, "Denim", 10, 5),
        Leather(3, "Leather", 20, 10);

        public final int index;
        public final String type;
        public final int maxScratchDef;
        public final int maxBiteDef;

        private ClothingPatchFabricType(final int index, final String type, final int maxScratchDef, final int maxBiteDef) {
            this.index = index;
            this.type = type;
            this.maxScratchDef = maxScratchDef;
            this.maxBiteDef = maxBiteDef;
        }

        public String getType() {
            return this.type;
        }

        public static Clothing.ClothingPatchFabricType fromType(String type) {
            if (StringUtils.isNullOrEmpty(type)) {
                return null;
            } else if (Cotton.type.equals(type)) {
                return Cotton;
            } else if (Denim.type.equals(type)) {
                return Denim;
            } else {
                return Leather.type.equals(type) ? Leather : null;
            }
        }

        public static Clothing.ClothingPatchFabricType fromIndex(int index) {
            if (index == 1) {
                return Cotton;
            } else if (index == 2) {
                return Denim;
            } else {
                return index == 3 ? Leather : null;
            }
        }
    }

    private static enum WetDryState {
        Invalid,
        Dryer,
        Wetter;
    }
}
