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
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RBPoliceSiege extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null) {
                        for (int o = 0; o < sq.getObjects().size(); o++) {
                            IsoObject obj = sq.getObjects().get(o);
                            if (obj instanceof IsoDoor isoDoor && isoDoor.isBarricadeAllowed() && !SpawnPoints.instance.isSpawnBuilding(def)) {
                                IsoGridSquare outside = sq.getRoom() == null ? sq : isoDoor.getOppositeSquare();
                                if (outside != null && outside.getRoom() == null) {
                                    boolean addOpposite = outside == sq;
                                    IsoBarricade barricade = IsoBarricade.AddBarricadeToObject(isoDoor, addOpposite);
                                    if (barricade != null) {
                                        if (Rand.Next(2) == 0) {
                                            int numPlanks = Rand.Next(1, 4);

                                            for (int b = 0; b < numPlanks; b++) {
                                                barricade.addPlank(null, null);
                                            }
                                        } else {
                                            barricade.addMetal(null, null);
                                        }

                                        if (GameServer.server) {
                                            barricade.transmitCompleteItemToClients();
                                        }
                                    }
                                }
                            }

                            if (obj instanceof IsoWindow isoWindow && Rand.Next(100) <= 85) {
                                isoWindow.smashWindow(true, false);
                                IsoGridSquare outside = sq.getRoom() == null ? sq : isoWindow.getOppositeSquare();
                                if (isoWindow.isBarricadeAllowed() && z == 0 && outside != null && outside.getRoom() == null) {
                                    boolean addOpposite = outside == sq;
                                    IsoBarricade barricade = IsoBarricade.AddBarricadeToObject(isoWindow, addOpposite);
                                    if (barricade != null) {
                                        if (Rand.Next(2) == 0) {
                                            int numPlanks = Rand.Next(1, 4);

                                            for (int b = 0; b < numPlanks; b++) {
                                                barricade.addPlank(null, null);
                                            }
                                        } else {
                                            barricade.addMetal(null, null);
                                        }

                                        if (GameServer.server) {
                                            barricade.transmitCompleteItemToClients();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        def.alarmed = false;
        ArrayList<IsoZombie> zeds = this.addZombies(def, Rand.Next(4) + 1, "Police", 50, null);
        InventoryContainer bag = InventoryItemFactory.CreateItem("Base.Bag_WeaponBag");
        ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
        this.addItemOnGround(def.getFreeSquareInRoom(), bag);
        if (Rand.Next(2) == 0) {
            InventoryContainer bag2 = InventoryItemFactory.CreateItem("Base.Bag_SurvivorBag");
            ItemPickerJava.rollContainerItem(bag2, null, ItemPickerJava.getItemPickerContainers().get(bag2.getType()));
            this.addItemOnGround(def.getFreeSquareInRoom(), bag2);
        }

        if (Rand.Next(2) == 0) {
            InventoryContainer bag3 = InventoryItemFactory.CreateItem("Base.Bag_FoodCanned");
            ItemPickerJava.rollContainerItem(bag3, null, ItemPickerJava.getItemPickerContainers().get(bag3.getType()));
            this.addItemOnGround(def.getFreeSquareInRoom(), bag3);
        }

        if (Rand.Next(2) == 0) {
            InventoryContainer bag4 = InventoryItemFactory.CreateItem("Base.Bag_MedicalBag");
            ItemPickerJava.rollContainerItem(bag4, null, ItemPickerJava.getItemPickerContainers().get(bag4.getType()));
            this.addItemOnGround(def.getFreeSquareInRoom(), bag4);
        }

        if (Rand.Next(2) == 0) {
            InventoryContainer bag5 = InventoryItemFactory.CreateItem("Base.Bag_ProtectiveCaseBulkyAmmo");
            ItemPickerJava.rollContainerItem(bag5, null, ItemPickerJava.getItemPickerContainers().get(bag5.getType()));
            this.addItemOnGround(def.getFreeSquareInRoom(), bag5);
        }

        int carType = Rand.Next(2);
        String car = "Base.CarLightsPolice";
        switch (carType) {
            case 1:
                car = "Base.PickUpVanLightsPolice";
            default:
                BaseVehicle vehicle = this.spawnCarOnNearestNav(car, def);
                if (vehicle != null) {
                    vehicle.setAlarmed(false);
                    zeds.get(Rand.Next(zeds.size())).addItemToSpawnAtDeath(vehicle.createVehicleKey());
                }
        }
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        } else {
            return def.getRooms().size() > 30 ? false : def.getRoom("policestorage") != null;
        }
    }

    public RBPoliceSiege() {
        this.name = "Barricaded Police Station";
        this.setChance(5);
        this.setUnique(true);
    }
}
