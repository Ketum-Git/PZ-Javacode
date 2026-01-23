// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;

@UsedFromLua
public final class RDSRatWar extends RandomizedDeadSurvivorBase {
    public RDSRatWar() {
        this.name = "Rat War";
        this.setChance(0);
        this.setUnique(true);
        this.isRat = true;
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        for (int i = 0; i < def.rooms.size(); i++) {
            RDSRatInfested.ratRoom(def.rooms.get(i));
        }

        def.alarmed = false;
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
