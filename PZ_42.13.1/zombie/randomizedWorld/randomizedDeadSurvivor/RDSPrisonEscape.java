// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;

/**
 * 2-3 zombies in inmate jumpsuits with some duffel bags on them with ropes,
 *  duct tape, etc.
 */
@UsedFromLua
public final class RDSPrisonEscape extends RandomizedDeadSurvivorBase {
    public RDSPrisonEscape() {
        this.name = "Prison Escape";
        this.setChance(3);
        this.setMaximumDays(90);
        this.setUnique(true);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getLivingRoomOrKitchen(def);
        this.addZombies(def, Rand.Next(2, 4), "InmateEscaped", 0, room);
        def.alarmed = false;
    }
}
