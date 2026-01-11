// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpawnPoints;
import zombie.vehicles.BaseVehicle;

/**
 * This building will be 70% burnt (no fire started tho)
 *  Also spawn 1 to 3 fireman zombies inside it (65% of them to be male)
 */
@UsedFromLua
public final class RBBurntFireman extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
        int totalFireman = Rand.Next(1, 4);
        def.setHasBeenVisited(true);
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null && Rand.Next(100) < 70) {
                        sq.Burn(false);
                    }
                }
            }
        }

        def.setAllExplored(true);
        BaseVehicle v;
        if (Rand.NextBool(2)) {
            v = this.spawnCarOnNearestNav("Base.PickUpVanLightsFire", def);
        } else {
            v = this.spawnCarOnNearestNav("Base.PickUpTruckLightsFire", def);
        }

        if (v != null) {
            v.setAlarmed(false);
        }

        String outfit = "FiremanFullSuit";
        if (v != null && v.getZombieType() != null) {
            outfit = v.getFirstZombieType();
        }

        ArrayList<IsoZombie> zeds = this.addZombies(def, totalFireman, outfit, 35, this.getLivingRoomOrKitchen(def));

        for (int i = 0; i < zeds.size(); i++) {
            zeds.get(i).getInventory().setExplored(true);
        }

        if (v != null && !zeds.isEmpty()) {
            zeds.get(Rand.Next(zeds.size())).addItemToSpawnAtDeath(v.createVehicleKey());
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
        } else {
            return true;
        }
    }

    public RBBurntFireman() {
        this.name = "Burnt Fireman";
        this.setChance(2);
    }
}
