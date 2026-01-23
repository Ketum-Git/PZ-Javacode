// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedBuilding.RandomizedBuildingBase;

@UsedFromLua
public class RandomizedDeadSurvivorBase extends RandomizedBuildingBase {
    public void randomizeDeadSurvivor(BuildingDef def) {
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        if (!force) {
            for (int i = 0; i < GameServer.Players.size(); i++) {
                IsoPlayer player = GameServer.Players.get(i);
                if (player.getSquare() != null && player.getSquare().getBuilding() != null && player.getSquare().getBuilding().def == def) {
                    return false;
                }
            }
        }

        if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
        }

        if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        } else {
            return !SpawnPoints.instance.isSpawnBuilding(def);
        }
    }
}
