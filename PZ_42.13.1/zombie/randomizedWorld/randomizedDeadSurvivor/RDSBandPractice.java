// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.list.PZArrayUtil;

/**
 * Need a garage with a size of at least 10 tiles
 *  Spawn some rocker zombies & music instruments on ground & shelves
 *  Corpse with a guitar in his hand
 */
@UsedFromLua
public final class RDSBandPractice extends RandomizedDeadSurvivorBase {
    private final ArrayList<String> instrumentsList = new ArrayList<>();

    public RDSBandPractice() {
        this.name = "Band Practice";
        this.setChance(10);
        this.setMaximumDays(60);
        this.instrumentsList.add("GuitarAcoustic");
        this.instrumentsList.add("GuitarElectric");
        this.instrumentsList.add("GuitarElectric");
        this.instrumentsList.add("GuitarElectric");
        this.instrumentsList.add("GuitarElectricBass");
        this.instrumentsList.add("GuitarElectricBass");
        this.instrumentsList.add("GuitarElectricBass");
        this.instrumentsList.add("Harmonica");
        this.instrumentsList.add("Microphone");
        this.instrumentsList.add("Bag_ProtectiveCaseBulky_Audio");
        this.instrumentsList.add("Speaker");
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        this.spawnItemsInContainers(def, "BandPractice", 90);
        RoomDef room = this.getRoom(def, "garagestorage");
        if (room == null) {
            room = this.getRoom(def, "shed");
        }

        if (room == null) {
            room = this.getRoom(def, "garage");
        }

        this.addZombies(def, Rand.Next(2, 4), "Rocker", 20, room);
        IsoGridSquare sq = getRandomSpawnSquare(room);
        if (sq != null) {
            ItemSpawner.spawnItem(PZArrayUtil.pickRandom(this.instrumentsList), sq, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            if (Rand.Next(4) == 0) {
                ItemSpawner.spawnItem(PZArrayUtil.pickRandom(this.instrumentsList), sq, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }

            if (Rand.Next(4) == 0) {
                ItemSpawner.spawnItem(PZArrayUtil.pickRandom(this.instrumentsList), sq, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }

            def.alarmed = false;
        }
    }

    /**
     * Description copied from class: RandomizedBuildingBase
     */
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
                if (("garagestorage".equals(room.name) || "shed".equals(room.name) || "garage".equals(room.name)) && room.area >= 9) {
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
