// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;

@UsedFromLua
public final class RDSRatKing extends RandomizedDeadSurvivorBase {
    public RDSRatKing() {
        this.name = "Rat King";
        this.setChance(1);
        this.setUnique(true);
        this.isRat = true;
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        String roomType = "bedroom";
        int roll = Rand.Next(3);
        if (roll == 0) {
            roomType = "kitchen";
        }

        if (roll == 1) {
            roomType = "livingroom";
        }

        RoomDef room = this.getRoom(def, roomType);
        if (room == null) {
            room = this.getRoom(def, "kitchen");
        }

        if (room == null) {
            room = this.getRoom(def, "livingroom");
        }

        if (room == null) {
            room = this.getRoom(def, "bedroom");
        }

        if (room != null) {
            this.addItemOnGround(room.getFreeSquare(), "Base.RatKing");
            int max = room.getIsoRoom().getSquares().size();
            if (max > 21) {
                max = 21;
            }

            int min = max / 2;
            if (min < 1) {
                min = 1;
            }

            if (min > 9) {
                min = 9;
            }

            int nbrOfRats = Rand.Next(min, max);

            for (int i = 0; i < nbrOfRats; i++) {
                IsoGridSquare square = room.getFreeUnoccupiedSquare();
                if (square != null && square.isFree(true)) {
                    IsoAnimal animal;
                    if (Rand.NextBool(2)) {
                        animal = new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "rat", "grey");
                    } else {
                        animal = new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "ratfemale", "grey");
                    }

                    animal.randomizeAge();
                    IsoDeadBody deadAnimal = new IsoDeadBody(animal, false);
                    deadAnimal.addToWorld();
                }
            }

            int nbrOfPoops = Rand.Next(min, max);

            for (int ix = 0; ix < nbrOfPoops; ix++) {
                IsoGridSquare square = room.getFreeSquare();
                if (square != null && !square.isOutside() && square.getRoom() != null && square.hasRoomDef()) {
                    this.addItemOnGround(square, "Base.Dung_Rat");
                }
            }

            RDSRatInfested.ratRoom(room);

            for (int ixx = 0; ixx < def.rooms.size(); ixx++) {
                RDSRatInfested.ratRoom(def.rooms.get(ixx));
            }
        }
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        } else if (!force && !Rand.NextBool(100)) {
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
