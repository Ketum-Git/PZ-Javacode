// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;

/**
 * Create 1 to 2 zombies in the bathroom with some bathroom items on the ground
 */
@UsedFromLua
public final class RDSBathroomZed extends RandomizedDeadSurvivorBase {
    private final ArrayList<String> items = new ArrayList<>();

    public RDSBathroomZed() {
        this.name = "Bathroom Zed";
        this.setChance(12);
        this.items.add("Base.BathTowel");
        this.items.add("Base.Razor");
        this.items.add("Base.Lipstick");
        this.items.add("Base.Comb");
        this.items.add("Base.Hairspray2");
        this.items.add("Base.Toothbrush");
        this.items.add("Base.Cologne");
        this.items.add("Base.Perfume");
        this.items.add("Base.HairDryer");
        this.items.add("Base.StraightRazor");
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoom(def, "bathroom");
        int nbrOfZed = 1;
        if (room.area > 6) {
            nbrOfZed = Rand.Next(1, 3);
        }

        this.addZombies(def, nbrOfZed, Rand.Next(2) == 0 ? "Bathrobe" : "Naked", null, room);
        this.addRandomItemsOnGround(room, this.items, Rand.Next(2, 5));
    }
}
