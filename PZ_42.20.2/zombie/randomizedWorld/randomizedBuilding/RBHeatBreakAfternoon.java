// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RBHeatBreakAfternoon extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
        RoomDef room = def.getRoom("bank");
        if (room != null) {
            String outfit = "BankRobber";
            if (Rand.NextBool(2)) {
                outfit = "BankRobberSuit";
            }

            this.addZombies(def, Rand.Next(3, 5), outfit, 0, room);
            InventoryContainer bag = InventoryItemFactory.CreateItem("Base.Bag_MoneyBag");
            ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
            IsoGridSquare sq = room.getFreeSquare();
            if (sq != null) {
                this.addItemOnGround(sq, bag);
            }

            if (Rand.Next(2) == 0) {
                bag = InventoryItemFactory.CreateItem("Base.Bag_MoneyBag");
                ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
                sq = room.getFreeSquare();
                if (sq != null) {
                    this.addItemOnGround(sq, bag);
                }
            }

            if (Rand.Next(2) == 0) {
                bag = InventoryItemFactory.CreateItem("Base.Bag_MoneyBag");
                ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
                sq = room.getFreeSquare();
                if (sq != null) {
                    this.addItemOnGround(sq, bag);
                }
            }

            if (Rand.Next(2) == 0) {
                bag = InventoryItemFactory.CreateItem("Base.Bag_MoneyBag");
                ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
                sq = room.getFreeSquare();
                if (sq != null) {
                    this.addItemOnGround(sq, bag);
                }
            }

            this.addZombies(def, Rand.Next(2, 4), "Police", null, room);
        }

        BaseVehicle v = this.spawnCarOnNearestNav("Base.StepVan_LouisvilleSWAT", def);
        if (v != null) {
            v.setAlarmed(false);
            IsoGridSquare sqx = v.getSquare().getCell().getGridSquare(v.getSquare().x - 2, v.getSquare().y - 2, 0);
            ArrayList<IsoZombie> zeds = this.addZombiesOnSquare(Rand.Next(3, 6), "Police_SWAT", null, sqx);
            if (!zeds.isEmpty()) {
                zeds.get(Rand.Next(zeds.size())).addItemToSpawnAtDeath(v.createVehicleKey());
            }
        }
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        } else {
            return def.getRoom("bank") != null;
        }
    }

    public RBHeatBreakAfternoon() {
        this.name = "Bank Robbery";
        this.setChance(10);
        this.setUnique(true);
    }
}
