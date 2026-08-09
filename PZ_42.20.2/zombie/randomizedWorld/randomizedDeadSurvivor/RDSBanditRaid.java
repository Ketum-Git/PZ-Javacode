// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class RDSBanditRaid extends RandomizedDeadSurvivorBase {
    public RDSBanditRaid() {
        this.name = "Bandit Raid";
        this.setChance(1);
        this.setMinimumDays(30);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        int size = def.getRooms().size();
        BaseVehicle vehicle = null;
        if (Rand.Next(2) == 0) {
            vehicle = this.spawnCarOnNearestNav("Base.VanSeats", def);
        }

        def.alarmed = false;
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null) {
                        for (int o = 0; o < sq.getObjects().size(); o++) {
                            IsoObject obj = sq.getObjects().get(o);
                            if (Rand.Next(100) <= 85 && obj instanceof IsoDoor isoDoor && !isoDoor.isBarricaded()) {
                                isoDoor.destroy();
                            } else if (obj instanceof IsoDoor isoDoor) {
                                isoDoor.setLocked(false);
                            }

                            if (Rand.Next(100) <= 85 && obj instanceof IsoWindow isoWindow) {
                                isoWindow.smashWindow(true, false);
                                IsoGridSquare outside = sq.getRoom() == null ? sq : isoWindow.getOppositeSquare();
                                if (isoWindow.isBarricadeAllowed() && z == 0 && outside != null && outside.getRoom() == null) {
                                    boolean addOpposite = outside == sq;
                                    IsoBarricade barricade = IsoBarricade.AddBarricadeToObject(isoWindow, addOpposite);
                                    if (barricade != null) {
                                        int numPlanks = Rand.Next(0, 4);

                                        for (int b = 0; b < numPlanks; b++) {
                                            barricade.addPlank(null, null);
                                        }

                                        if (GameServer.server) {
                                            barricade.transmitCompleteItemToClients();
                                        }
                                    }
                                }
                            }

                            if (obj.getContainer() != null && obj.getContainer().getItems() != null) {
                                for (int k = 0; k < obj.getContainer().getItems().size(); k++) {
                                    if (Rand.Next(100) < 80) {
                                        obj.getContainer().getItems().remove(k);
                                        k--;
                                    }
                                }

                                ItemPickerJava.updateOverlaySprite(obj);
                                obj.getContainer().setExplored(true);
                            }
                        }
                    }
                }
            }
        }

        def.setAllExplored(true);
        def.alarmed = false;
        RoomDef room = this.getLivingRoomOrKitchen(def);
        String zombieType = "Bandit";
        if (Rand.NextBool(3)) {
            zombieType = "PrivateMilitia";
        }

        ArrayList<IsoZombie> zombies = this.addZombies(def, Rand.Next(2, 4), zombieType, null, room);
        if (zombies.get(0) != null && vehicle != null) {
            vehicle.trySpawnVehicleKeyOnZombie(zombies.get(0));
        } else if (vehicle != null) {
            vehicle.addKeyToWorld();
        }

        room = this.getLivingRoomOrKitchen(def);
        int corpseNbr = Rand.Next(2, 4);

        for (int i = 0; i < corpseNbr; i++) {
            IsoGridSquare freeSQ = getRandomSquareForCorpse(room);
            createRandomDeadBody(freeSQ, null, Rand.Next(5, 10), 5, null);
        }

        if (size > 5) {
            room = this.getRandomRoomNoKids(def, 6);
            this.addZombies(def, Rand.Next(2, 4), zombieType, null, room);
            room = this.getRandomRoom(def, 6);
            corpseNbr = Rand.Next(2, 4);

            for (int i = 0; i < corpseNbr; i++) {
                IsoGridSquare freeSQ = getRandomSquareForCorpse(room);
                createRandomDeadBody(freeSQ, null, Rand.Next(5, 10), 5, null);
            }

            size = (int)Math.floor(size / 10);
            if (size > 10) {
                size = 10;
            }

            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    room = this.getRandomRoomNoKids(def, 6);
                    this.addZombies(def, Rand.Next(2, 4), zombieType, null, room);
                    room = this.getRandomRoomNoKids(def, 6);
                    if (room != null) {
                        corpseNbr = Rand.Next(2, 4);

                        for (i = 0; i < corpseNbr; i++) {
                            IsoGridSquare freeSQ = getRandomSquareForCorpse(room);
                            createRandomDeadBody(freeSQ, null, Rand.Next(5, 10), 5, null);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        } else if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        } else if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        } else if (def.isAllExplored() && !force) {
            return false;
        } else if (this.getRoom(def, "kitchen") == null && this.getRoom(def, "bedroom") == null) {
            return false;
        } else {
            if (!force) {
                for (int i = 0; i < GameServer.Players.size(); i++) {
                    IsoPlayer player = GameServer.Players.get(i);
                    if (player.getSquare() != null && player.getSquare().getBuilding() != null && player.getSquare().getBuilding().def == def) {
                        return false;
                    }
                }
            }

            if (def.getRooms().size() > 100) {
                this.debugLine = "Building is too large";
                return false;
            } else {
                return true;
            }
        }
    }
}
