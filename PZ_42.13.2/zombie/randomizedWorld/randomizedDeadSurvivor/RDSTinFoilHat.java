// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;

/**
 * Zombies family with tin foil hat
 */
@UsedFromLua
public final class RDSTinFoilHat extends RandomizedDeadSurvivorBase {
    public RDSTinFoilHat() {
        this.name = "Tin foil hat family";
        this.setUnique(true);
        this.setChance(2);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getLivingRoomOrKitchen(def);
        this.addZombies(def, Rand.Next(2, 5), "TinFoilHat", null, room);
    }
}
