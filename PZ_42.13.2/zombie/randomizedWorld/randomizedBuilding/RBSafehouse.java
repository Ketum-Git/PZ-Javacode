// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.ZombiesStageDefinitions;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.ItemPickerJava;
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

/**
 * This building will be barricaded, have a lot of canned food but also lot of zombies inside it
 */
@UsedFromLua
public final class RBSafehouse extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
        def.setHasBeenVisited(true);
        String lootType = "SafehouseLoot";
        int days = (int)(GameTime.getInstance().getWorldAgeHours() / 24.0) + (SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30;
        if (days >= ZombiesStageDefinitions.daysLate) {
            lootType = "SafehouseLoot_Late";
        } else if (days >= ZombiesStageDefinitions.daysEarly) {
            lootType = "SafehouseLoot_Mid";
        }

        ItemPickerJava.ItemPickerRoom safehouseLoot = ItemPickerJava.rooms.get(lootType);
        IsoCell cell = IsoWorld.instance.currentCell;
        int trashFactor = 40 + SandboxOptions.instance.getCurrentLootedChance();
        if (trashFactor > 90) {
            trashFactor = 90;
        }

        boolean mean = Rand.NextBool(8);
        boolean trash = mean || Rand.NextBool(2);

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null) {
                        boolean kidsRoom = sq.getRoom() != null && sq.getRoom().getRoomDef() != null && sq.getRoom().getRoomDef().isKidsRoom();
                        boolean canTrash = trash && !kidsRoom && sq.getObjects().size() < 2 && sq.hasFloor() && !sq.isOutside();

                        for (int o = 0; o < sq.getObjects().size(); o++) {
                            IsoObject obj = sq.getObjects().get(o);
                            if (obj instanceof IsoDoor isoDoor && isoDoor.isBarricadeAllowed() && !SpawnPoints.instance.isSpawnBuilding(def)) {
                                IsoGridSquare outside = sq.getRoom() == null ? sq : isoDoor.getOppositeSquare();
                                if (outside != null && outside.getRoom() == null) {
                                    boolean addOpposite = outside != sq;
                                    IsoBarricade barricade = IsoBarricade.AddBarricadeToObject(isoDoor, addOpposite);
                                    if (barricade != null) {
                                        int numPlanks = Rand.Next(1, 4);

                                        for (int b = 0; b < numPlanks; b++) {
                                            barricade.addPlank(null, null);
                                        }

                                        if (GameServer.server) {
                                            barricade.transmitCompleteItemToClients();
                                        }
                                    }
                                }
                            }

                            if (obj instanceof IsoWindow isoWindow) {
                                IsoGridSquare outside = sq.getRoom() == null ? sq : isoWindow.getOppositeSquare();
                                if (isoWindow.isBarricadeAllowed() && z == 0 && outside != null && outside.getRoom() == null) {
                                    boolean addOpposite = outside != sq;
                                    IsoBarricade barricade = IsoBarricade.AddBarricadeToObject(isoWindow, addOpposite);
                                    if (barricade != null) {
                                        int numPlanks = Rand.Next(1, 4);

                                        for (int b = 0; b < numPlanks; b++) {
                                            barricade.addPlank(null, null);
                                        }

                                        if (GameServer.server) {
                                            barricade.transmitCompleteItemToClients();
                                        }
                                    }
                                } else {
                                    isoWindow.addSheet(null);
                                    isoWindow.HasCurtains().ToggleDoor(null);
                                }
                            }

                            if (!kidsRoom
                                && obj.getContainer() != null
                                && sq.getRoom() != null
                                && sq.getRoom().getBuilding().getDef() == def
                                && Rand.Next(100) <= 70
                                && sq.getRoom().getName() != null
                                && safehouseLoot.containers.containsKey(obj.getContainer().getType())) {
                                ItemPickerJava.fillContainerType(safehouseLoot, obj.getContainer(), "", null);
                                ItemPickerJava.updateOverlaySprite(obj);
                                obj.getContainer().setExplored(true);
                            }

                            if (!kidsRoom && mean && this.isValidGraffSquare(sq, true, false) && Rand.Next(500) <= trashFactor) {
                                this.graffSquare(sq, true);
                            }

                            if (!kidsRoom && mean && this.isValidGraffSquare(sq, false, false) && Rand.Next(500) <= trashFactor) {
                                this.graffSquare(sq, false);
                            }

                            if (canTrash && Rand.Next(1000) <= trashFactor) {
                                this.trashSquare(sq);
                            }
                        }
                    }
                }
            }
        }

        def.setAllExplored(true);
        def.alarmed = false;
        this.addZombies(def, mean);
    }

    private void addZombies(BuildingDef def, boolean mean) {
        if (mean && Rand.NextBool(2)) {
            mean = false;
        }

        if (mean) {
            this.addZombies(def, 0, "Bandit", null, null);
        } else if (Rand.NextBool(2)) {
            this.addZombies(def, 0, "Evacuee", null, null);
        } else {
            this.addZombies(def, 0, null, null, null);
        }

        if (Rand.Next(5) == 0) {
            if (mean) {
                this.addZombies(def, 1, "Bandit", null, null);
            } else {
                String outfitName = switch (Rand.Next(5)) {
                    case 1 -> "Survivalist02";
                    case 2 -> "Survivalist03";
                    case 3 -> "Survivalist04";
                    case 4 -> "Survivalist05";
                    default -> "Survivalist";
                };
                this.addZombies(def, 1, outfitName, null, null);
            }
        }

        if (Rand.Next(100) <= 60) {
            RandomizedBuildingBase.createRandomDeadBody(this.getLivingRoomOrKitchen(def), Rand.Next(3, 7));
        }

        if (Rand.Next(100) <= 60) {
            RandomizedBuildingBase.createRandomDeadBody(this.getLivingRoomOrKitchen(def), Rand.Next(3, 7));
        }
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        if (!super.isValid(def, force)) {
            return false;
        } else if (def.getRooms().size() > 20) {
            return false;
        } else if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        } else if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        } else if (def.getRoom("derelict") == null && def.getRoom("empty") == null) {
            return true;
        } else {
            this.debugLine = "Buildings with derelict or empty rooms as invalid";
            return false;
        }
    }

    public RBSafehouse() {
        this.name = "Safehouse";
        this.setChance(10);
    }
}
