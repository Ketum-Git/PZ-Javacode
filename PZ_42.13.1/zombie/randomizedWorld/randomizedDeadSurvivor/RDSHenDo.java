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
 * Create some zombies female zombies with 1 naked male, some alcohol around
 */
@UsedFromLua
public final class RDSHenDo extends RandomizedDeadSurvivorBase {
    public RDSHenDo() {
        this.name = "Hen Do";
        this.setChance(2);
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
        this.addZombies(def, Rand.Next(5, 7), null, 100, room);
        this.addZombies(def, 1, "Naked", 0, room);
        this.addRandomItemsOnGround(room, this.getHenDoSnacks(), Rand.Next(3, 7));
        this.addRandomItemsOnGround(room, this.getHenDoDrinks(), Rand.Next(2, 6));
        def.alarmed = false;
    }
}
