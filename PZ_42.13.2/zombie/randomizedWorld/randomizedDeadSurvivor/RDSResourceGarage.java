// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;

@UsedFromLua
public final class RDSResourceGarage extends RandomizedDeadSurvivorBase {
    public RDSResourceGarage() {
        this.name = "Resource Garage";
        this.setChance(10);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoom(def, "garagestorage");
        if (room == null) {
            room = this.getRoom(def, "shed");
        }

        if (room == null) {
            room = this.getRoom(def, "garage");
        }

        if (room == null) {
            room = this.getRoom(def, "farmstorage");
        }

        if (room != null) {
            room.getIsoRoom().spawnRandomWorkstation();
        }
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        } else if (def.isAllExplored() && !force) {
            return false;
        } else if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        } else if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
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

            boolean garageStorage = false;

            for (int ix = 0; ix < def.rooms.size(); ix++) {
                RoomDef room = def.rooms.get(ix);
                if (("garagestorage".equals(room.name) || "shed".equals(room.name) || "garage".equals(room.name) || "farmstorage".equals(room.name))
                    && room.area >= 9) {
                    garageStorage = true;
                    break;
                }
            }

            if (!garageStorage) {
                this.debugLine = "No shed/garage or is too small";
            }

            return garageStorage;
        }
    }
}
