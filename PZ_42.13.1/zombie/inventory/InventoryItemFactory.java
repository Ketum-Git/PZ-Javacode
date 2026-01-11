// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.inventory.types.AlarmClockClothing;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Drainable;
import zombie.inventory.types.Food;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Moveable;
import zombie.inventory.types.Radio;
import zombie.network.GameClient;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AmmoType;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemKey;
import zombie.util.StringUtils;
import zombie.world.ItemInfo;
import zombie.world.WorldDictionary;

public final class InventoryItemFactory {
    public static <T extends InventoryItem> T CreateItem(ItemKey key) {
        return (T)CreateItem(key.toString(), 1.0F);
    }

    public static <T extends InventoryItem> T CreateItem(String itemType) {
        return (T)CreateItem(itemType, 1.0F);
    }

    public static <T extends InventoryItem> T CreateItem(AmmoType ammoType) {
        return (T)CreateItem(ammoType.getItemKey(), 1.0F);
    }

    public static InventoryItem CreateItem(String itemType, Food food) {
        InventoryItem item = CreateItem(itemType, 1.0F);
        if (item instanceof Food newItem) {
            newItem.setBaseHunger(food.getBaseHunger());
            newItem.setHungChange(food.getHungChange());
            newItem.setBoredomChange(food.getBoredomChangeUnmodified());
            newItem.setUnhappyChange(food.getUnhappyChangeUnmodified());
            newItem.setCarbohydrates(food.getCarbohydrates());
            newItem.setLipids(food.getLipids());
            newItem.setProteins(food.getProteins());
            newItem.setCalories(food.getCalories());
            return item;
        } else {
            return null;
        }
    }

    public static InventoryItem CreateItem(String itemType, float useDelta) {
        return CreateItem(itemType, useDelta, true);
    }

    public static Item getItem(String itemType, boolean moduleDefaultsToBase) {
        Item scriptItem = ScriptManager.instance.FindItem(itemType, moduleDefaultsToBase);
        if (scriptItem == null && itemType.endsWith("Empty")) {
            itemType = itemType.substring(0, itemType.length() - 5);
            scriptItem = ScriptManager.instance.FindItem(itemType, moduleDefaultsToBase);
            if (scriptItem != null) {
                if (scriptItem.containsComponent(ComponentType.Durability)) {
                    DebugLog.Entity.debugln("Durability Component: " + itemType);
                } else if (!scriptItem.containsComponent(ComponentType.FluidContainer)) {
                    scriptItem = null;
                }
            }
        }

        return scriptItem;
    }

    public static InventoryItem CreateItem(String itemType, float useDelta, boolean moduleDefaultsToBase) {
        return createItemInternal(itemType, useDelta, moduleDefaultsToBase, true);
    }

    private static InventoryItem createItemInternal(String itemType, float useDelta, boolean moduleDefaultsToBase, boolean isFirstTimeCreated) {
        InventoryItem item = null;
        Item scriptItem = null;
        boolean isMoveable = false;
        String movName = null;
        boolean isEmptyFluidContainer = false;

        try {
            if (itemType.startsWith("Moveables.") && !itemType.equalsIgnoreCase("Moveables.Moveable")) {
                String[] split = itemType.split("\\.");
                movName = split[1];
                isMoveable = true;
                itemType = "Moveables.Moveable";
            }

            scriptItem = ScriptManager.instance.FindItem(itemType, moduleDefaultsToBase);
            if (scriptItem == null && itemType.endsWith("Empty")) {
                itemType = itemType.substring(0, itemType.length() - 5);
                scriptItem = ScriptManager.instance.FindItem(itemType, moduleDefaultsToBase);
                if (scriptItem != null) {
                    if (scriptItem.containsComponent(ComponentType.Durability)) {
                        DebugLog.log("Durability Component " + itemType);
                    } else if (!scriptItem.containsComponent(ComponentType.FluidContainer)) {
                        scriptItem = null;
                    } else {
                        isEmptyFluidContainer = true;
                    }
                }
            }
        } catch (Exception var10) {
            DebugLog.log("couldn't find item " + itemType);
        }

        if (scriptItem == null) {
            DebugLog.log("Couldn't find item " + itemType);
            return null;
        } else {
            item = scriptItem.InstanceItem(null, isFirstTimeCreated);
            if (isEmptyFluidContainer && item.hasComponent(ComponentType.FluidContainer)) {
                item.getFluidContainer().Empty();
            }

            if (GameClient.client && (Core.getInstance().getPoisonousBerry() == null || Core.getInstance().getPoisonousBerry().isEmpty())) {
                Core.getInstance().setPoisonousBerry(GameClient.poisonousBerry);
            }

            if (GameClient.client && (Core.getInstance().getPoisonousMushroom() == null || Core.getInstance().getPoisonousMushroom().isEmpty())) {
                Core.getInstance().setPoisonousMushroom(GameClient.poisonousMushroom);
            }

            if (itemType.equals(Core.getInstance().getPoisonousBerry())) {
                ((Food)item).poison = true;
                ((Food)item).setPoisonLevelForRecipe(1);
                ((Food)item).setPoisonDetectionLevel(1);
                ((Food)item).setPoisonPower(5);
                ((Food)item).setUseForPoison((int)(Math.abs(((Food)item).getHungChange()) * 100.0F));
            }

            if (itemType.equals(Core.getInstance().getPoisonousMushroom())) {
                ((Food)item).poison = true;
                ((Food)item).setPoisonLevelForRecipe(2);
                ((Food)item).setPoisonDetectionLevel(2);
                ((Food)item).setPoisonPower(10);
                ((Food)item).setUseForPoison((int)(Math.abs(((Food)item).getHungChange()) * 100.0F));
            }

            item.id = Rand.Next(2146250223) + 1233423;
            if (item instanceof Drainable) {
                item.setCurrentUses((int)(item.getMaxUses() * useDelta));
            }

            if (isMoveable) {
                item.type = movName;
                item.fullType = item.module + "." + movName;
                if (item instanceof Moveable moveable && !moveable.ReadFromWorldSprite(movName) && item instanceof Radio) {
                    DebugLog.log("InventoryItemFactory -> Radio item = " + (itemType != null ? itemType : "unknown"));
                }
            }

            return item;
        }
    }

    @Deprecated
    public static InventoryItem CreateItem(String itemType, float useDelta, String param) {
        InventoryItem item = null;
        Item scriptItem = ScriptManager.instance.getItem(itemType);
        if (scriptItem == null) {
            DebugLog.log(itemType + " item not found.");
            return null;
        } else {
            item = scriptItem.InstanceItem(param, true);
            if (item instanceof Drainable) {
                item.setCurrentUses((int)(item.getMaxUses() * useDelta));
            }

            return item;
        }
    }

    @Deprecated
    public static InventoryItem CreateItem(String module, String name, String type, String tex) {
        InventoryItem item = new InventoryItem(module, name, type, tex);
        item.id = Rand.Next(2146250223) + 1233423;
        return item;
    }

    public static InventoryItem CreateItem(short registryID) {
        ItemInfo info = WorldDictionary.getItemInfoFromID(registryID);
        if (info != null && info.isValid()) {
            String itemType = info.getFullType();
            if (itemType != null) {
                InventoryItem item = createItemInternal(itemType, 1.0F, false, false);
                if (item != null) {
                    return item;
                }

                DebugLog.log(
                    "InventoryItemFactory.CreateItem() unknown item type \""
                        + (itemType != null ? itemType : "unknown")
                        + "\", registry id = \""
                        + registryID
                        + "\". Make sure all mods used in save are installed."
                );
            } else {
                DebugLog.log(
                    "InventoryItemFactory.CreateItem() unknown item (full type=null) with registry ID \""
                        + registryID
                        + "\". Make sure all mods used in save are installed."
                );
            }
        } else if (info == null) {
            DebugLog.log(
                "InventoryItemFactory.CreateItem() unknown item with registry ID \"" + registryID + "\". Make sure all mods used in save are installed."
            );
        } else {
            DebugLog.log("InventoryItemFactory.CreateItem() cannot create item: " + info.ToString());
        }

        return null;
    }

    public static InventoryItem CreateItem(InventoryItem item, String itemType) {
        ItemVisual visual = item.getVisual();
        InventoryItem newItem = CreateItem(itemType);
        ItemVisual newVisual = newItem.getVisual();
        newVisual.setTint(visual.getTint(item.getClothingItem()));
        newVisual.setBaseTexture(visual.getBaseTexture());
        newVisual.setTextureChoice(visual.getTextureChoice());
        newVisual.setDecal(visual.getDecal(item.getClothingItem()));
        if (newItem instanceof InventoryContainer newContainer && item instanceof InventoryContainer container) {
            newContainer.getItemContainer().takeItemsFrom(container.getItemContainer());
            if (!StringUtils.equals(item.getName(), item.getScriptItem().getDisplayName())) {
                newItem.setName(item.getName());
            }
        }

        newItem.setColor(item.getColor());
        newVisual.copyDirt(visual);
        newVisual.copyBlood(visual);
        newVisual.copyHoles(visual);
        newVisual.copyPatches(visual);
        if (newItem instanceof Clothing newClothing && item instanceof Clothing clothing) {
            clothing.copyPatchesTo(newClothing);
            newClothing.setWetness(item.getWetness());
        }

        if (newItem instanceof AlarmClockClothing clockClothing && item instanceof AlarmClockClothing alarmClockClothing) {
            clockClothing.setAlarmSet(alarmClockClothing.isAlarmSet());
            clockClothing.setHour(alarmClockClothing.getHour());
            clockClothing.setMinute(alarmClockClothing.getMinute());
            clockClothing.syncAlarmClock();
            alarmClockClothing.setAlarmSet(false);
            alarmClockClothing.syncAlarmClock();
        }

        newItem.setConditionNoSound(item.getCondition());
        newItem.setFavorite(item.isFavorite());
        if (item.hasModData()) {
            newItem.copyModData(item.getModData());
        }

        newItem.synchWithVisual();
        return newItem;
    }
}
