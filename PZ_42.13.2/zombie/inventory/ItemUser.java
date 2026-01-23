// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import java.util.ArrayList;
import zombie.characters.IsoGameCharacter;
import zombie.debug.DebugLog;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.StringUtils;
import zombie.vehicles.VehiclePart;

public final class ItemUser {
    private static final ArrayList<InventoryItem> tempItems = new ArrayList<>();

    public static void UseItem(InventoryItem item) {
        UseItem(item, false, false, 1, false, false);
    }

    public static int UseItem(InventoryItem item, boolean bCrafting, boolean bInContainer, int consumes, boolean keep, boolean destroy) {
        if (!item.isDisappearOnUse() && !bCrafting && !destroy) {
            return 0;
        } else {
            int consumed = Math.min(item.getCurrentUses(), consumes);
            if (!keep) {
                item.setCurrentUses(item.getCurrentUses() - consumed);
            }

            if (item.replaceOnUse != null && !bInContainer && !bCrafting && !destroy) {
                String fullType = item.replaceOnUse;
                if (!fullType.contains(".")) {
                    fullType = item.module + "." + fullType;
                }

                CreateItem(fullType, tempItems);

                for (int i = 0; i < tempItems.size(); i++) {
                    InventoryItem newItem = tempItems.get(i);
                    AddItem(item, newItem);
                    newItem.copyConditionStatesFrom(item);
                }
            }

            if (item instanceof DrainableComboItem drainableComboItem
                && !StringUtils.isNullOrEmpty(drainableComboItem.getReplaceOnDeplete())
                && item.getCurrentUses() <= 0) {
                String fullType = drainableComboItem.getReplaceOnDeplete();
                if (!fullType.contains(".")) {
                    fullType = item.module + "." + fullType;
                }

                CreateItem(fullType, tempItems);

                for (int i = 0; i < tempItems.size(); i++) {
                    InventoryItem newItem = tempItems.get(i);
                    AddItem(item, newItem);
                    newItem.copyConditionStatesFrom(item);
                }
            }

            if (destroy) {
                RemoveItem(item);
            } else if (item.getCurrentUses() <= 0) {
                if (!item.isKeepOnDeplete()) {
                    RemoveItem(item);
                }
            } else if (GameServer.server) {
                GameServer.sendItemStats(item);
            }

            return consumed;
        }
    }

    public static void CreateItem(String fullType, ArrayList<InventoryItem> result) {
        result.clear();
        Item scriptItem = ScriptManager.instance.FindItem(fullType);
        if (scriptItem == null) {
            DebugLog.General.warn("ERROR: ItemUses.CreateItem: can't find " + fullType);
        } else {
            int count = scriptItem.getCount();

            for (int i = 0; i < count; i++) {
                InventoryItem item = InventoryItemFactory.CreateItem(fullType);
                if (item == null) {
                    return;
                }

                result.add(item);
            }
        }
    }

    public static void AddItem(InventoryItem existingItem, InventoryItem newItem) {
        IsoWorldInventoryObject worldObj = existingItem.getWorldItem();
        if (worldObj != null && worldObj.getWorldObjectIndex() == -1) {
            worldObj = null;
        }

        if (worldObj != null) {
            worldObj.getSquare().AddWorldInventoryItem(newItem, 0.0F, 0.0F, 0.0F, true);
        } else {
            if (existingItem.container != null) {
                VehiclePart vehiclePart = existingItem.container.vehiclePart;
                if (GameServer.server) {
                    GameServer.sendAddItemToContainer(existingItem.container, newItem);
                }

                existingItem.container.AddItem(newItem);
                if (vehiclePart != null) {
                    vehiclePart.setContainerContentAmount(vehiclePart.getItemContainer().getCapacityWeight());
                }
            }
        }
    }

    public static void RemoveItem(InventoryItem item) {
        IsoWorldInventoryObject worldObj = item.getWorldItem();
        if (worldObj != null && worldObj.getWorldObjectIndex() == -1) {
            worldObj = null;
        }

        if (worldObj != null) {
            worldObj.getSquare().transmitRemoveItemFromSquare(worldObj);
            if (item.container != null) {
                item.container.items.remove(item);
                item.container.setDirty(true);
                item.container.setDrawDirty(true);
                item.container = null;
            }
        } else {
            if (item.container != null) {
                IsoObject parent = item.container.parent;
                VehiclePart vehiclePart = item.container.vehiclePart;
                if (parent instanceof IsoGameCharacter chr) {
                    if (item instanceof Clothing clothing && item.isWorn()) {
                        clothing.Unwear();
                    }

                    chr.removeFromHands(item);
                    if (chr.getClothingItem_Back() == item) {
                        chr.setClothingItem_Back(null);
                    }
                }

                if (GameServer.server) {
                    GameServer.sendRemoveItemFromContainer(item.container, item);
                }

                if (GameClient.client) {
                    GameClient.sendRemoveItemFromContainer(item.container, item);
                }

                if (item.container != null) {
                    item.container.items.remove(item);
                    item.container.setDirty(true);
                    item.container.setDrawDirty(true);
                    item.container = null;
                }

                if (parent instanceof IsoDeadBody isoDeadBody) {
                    isoDeadBody.checkClothing(item);
                }

                if (parent instanceof IsoMannequin isoMannequin) {
                    isoMannequin.checkClothing(item);
                }

                if (vehiclePart != null) {
                    vehiclePart.setContainerContentAmount(vehiclePart.getItemContainer().getCapacityWeight());
                }
            }
        }
    }
}
