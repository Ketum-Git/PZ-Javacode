// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;

/**
 * Create some zombies in sports outfits with crisps, alcohol & cigarettes lying around
 */
@UsedFromLua
public final class RDSFootballNight extends RandomizedDeadSurvivorBase {
    public RDSFootballNight() {
        this.name = "Football Night";
        this.setChance(5);
        this.setMaximumDays(60);
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

            if (this.getRoom(def, "livingroom") != null) {
                return true;
            } else {
                this.debugLine = "No living room";
                return false;
            }
        }
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoom(def, "livingroom");
        this.addZombies(def, Rand.Next(3, 6), "SportsFan", 10, room);
        this.addRandomItemsOnGround(room, this.getFootballNightSnacks(), Rand.Next(3, 7));
        this.addRandomItemsOnGround(room, this.getFootballNightDrinks(), Rand.Next(2, 6));
        def.alarmed = false;
    }
}
