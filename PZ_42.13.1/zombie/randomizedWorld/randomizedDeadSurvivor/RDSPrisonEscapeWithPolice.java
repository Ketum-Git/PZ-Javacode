// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.RoomDef;
import zombie.vehicles.BaseVehicle;

/**
 * 2-3 zombies in inmate jumpsuits with some duffel bags on them with ropes,
 *  duct tape, etc.
 *  Cops in the house too with a police car waiting outside.
 */
@UsedFromLua
public final class RDSPrisonEscapeWithPolice extends RandomizedDeadSurvivorBase {
    public RDSPrisonEscapeWithPolice() {
        this.name = "Prison Escape with Police";
        this.setChance(2);
        this.setMaximumDays(90);
        this.setUnique(true);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getLivingRoomOrKitchen(def);
        this.addZombies(def, Rand.Next(2, 4), "InmateEscaped", 0, room);
        ArrayList<IsoZombie> zeds = this.addZombies(def, Rand.Next(2, 4), "Police", null, room);
        BaseVehicle v = this.spawnCarOnNearestNav("Base.CarLightsPolice", def);
        if (v != null) {
            v.setAlarmed(false);
        }

        if (v != null) {
            String outfit = "Police";
            if (v.getZombieType() != null) {
                outfit = v.getRandomZombieType();
            }

            IsoGridSquare sq = v.getSquare().getCell().getGridSquare(v.getSquare().x - 2, v.getSquare().y - 2, 0);
            ArrayList<IsoZombie> zombies = this.addZombiesOnSquare(3, outfit, null, sq);
            if (!zeds.isEmpty()) {
                zeds.addAll(zombies);
                zeds.get(Rand.Next(zeds.size())).addItemToSpawnAtDeath(v.createVehicleKey());
                def.alarmed = false;
            }
        }
    }
}
