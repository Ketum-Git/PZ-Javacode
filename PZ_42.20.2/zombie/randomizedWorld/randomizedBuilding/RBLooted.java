// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import java.util.List;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;

/**
 * This building will be almost empty of loot, and lot of the doors/windows will be broken
 */
@UsedFromLua
public final class RBLooted extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;
        ArrayList<InventoryItem> removedItems = new ArrayList<>();

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null) {
                        for (int o = 0; o < sq.getObjects().size(); o++) {
                            IsoObject obj = sq.getObjects().get(o);
                            if (obj instanceof IsoDoor isoDoor && isoDoor.isExterior() && Rand.Next(100) >= 85 && !isoDoor.isBarricaded()) {
                                isoDoor.destroy();
                            } else if (obj instanceof IsoDoor isoDoor) {
                                isoDoor.setLocked(false);
                            }

                            if (Rand.Next(100) >= 85 && obj instanceof IsoWindow isoWindow) {
                                isoWindow.smashWindow(true, false);
                            }

                            for (int i = 0; i < obj.getContainerCount(); i++) {
                                this.lootContainer(obj, obj.getContainerByIndex(i), removedItems);
                            }
                        }
                    }
                }
            }
        }

        def.setAllExplored(true);
        def.alarmed = false;
    }

    private void lootContainer(IsoObject obj, ItemContainer container, ArrayList<InventoryItem> removedItems) {
        if (container != null && container.getItems() != null) {
            removedItems.clear();
            List<InventoryItem> items = container.getItems();

            for (int i = 0; i < items.size(); i++) {
                if (Rand.Next(100) < 80) {
                    removedItems.add(items.get(i));
                }
            }

            if (!removedItems.isEmpty()) {
                if (GameServer.server) {
                    GameServer.sendRemoveItemsFromContainer(container, removedItems);
                }

                for (InventoryItem item : removedItems) {
                    container.DoRemoveItem(item);
                }

                ItemPickerJava.updateOverlaySprite(obj);
                container.setExplored(true);
            }
        }
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        if (!super.isValid(def, force)) {
            return false;
        } else if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        } else if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        } else if (SandboxOptions.instance.getCurrentLootedChance() < 1 && !force) {
            return false;
        } else {
            int max = SandboxOptions.instance.maximumLootedBuildingRooms.getValue();
            if (def.getRooms().size() > max) {
                this.debugLine = "Building is too large, maximum " + max + " rooms";
                return false;
            } else {
                return true;
            }
        }
    }

    public RBLooted() {
        this.name = "Looted";
        this.setChance(10);
    }
}
