// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.RoomDef;

/**
 * Create 2 naked zombies in the bedroom with clothing lying around
 */
@UsedFromLua
public final class RDSBedroomZed extends RandomizedDeadSurvivorBase {
    private final ArrayList<String> pantsMaleItems = new ArrayList<>();
    private final ArrayList<String> pantsFemaleItems = new ArrayList<>();
    private final ArrayList<String> topItems = new ArrayList<>();
    private final ArrayList<String> shoesItems = new ArrayList<>();

    public RDSBedroomZed() {
        this.name = "Bedroom Zed";
        this.setChance(7);
        this.shoesItems.add("Base.Shoes_Random");
        this.shoesItems.add("Base.Shoes_TrainerTINT");
        this.pantsMaleItems.add("Base.TrousersMesh_DenimLight");
        this.pantsMaleItems.add("Base.Trousers_DefaultTEXTURE_TINT");
        this.pantsMaleItems.add("Base.Trousers_Denim");
        this.pantsFemaleItems.add("Base.Skirt_Knees");
        this.pantsFemaleItems.add("Base.Skirt_Long");
        this.pantsFemaleItems.add("Base.Skirt_Short");
        this.pantsFemaleItems.add("Base.Skirt_Normal");
        this.topItems.add("Base.Shirt_FormalWhite");
        this.topItems.add("Base.Shirt_FormalWhite_ShortSleeve");
        this.topItems.add("Base.Tshirt_DefaultTEXTURE_TINT");
        this.topItems.add("Base.Tshirt_PoloTINT");
        this.topItems.add("Base.Tshirt_WhiteLongSleeveTINT");
        this.topItems.add("Base.Tshirt_WhiteTINT");
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoomNoKids(def, "bedroom");
        boolean twoMale = Rand.Next(7) == 0;
        boolean twoFemale = Rand.Next(7) == 0;
        if (twoMale) {
            this.addZombies(def, 2, "Naked", 0, room);
            this.addItemsOnGround(room, true);
            this.addItemsOnGround(room, true);
        } else if (twoFemale) {
            this.addZombies(def, 2, "Naked", 100, room);
            this.addItemsOnGround(room, false);
            this.addItemsOnGround(room, false);
        } else {
            this.addZombies(def, 1, "Naked", 0, room);
            this.addItemsOnGround(room, true);
            this.addZombies(def, 1, "Naked", 100, room);
            this.addItemsOnGround(room, false);
        }
    }

    private void addItemsOnGround(RoomDef room, boolean male) {
        IsoGridSquare sq = getRandomSpawnSquare(room);
        this.addRandomItemOnGround(sq, this.shoesItems);
        this.addRandomItemOnGround(sq, this.topItems);
        this.addRandomItemOnGround(sq, male ? this.pantsMaleItems : this.pantsFemaleItems);
    }
}
