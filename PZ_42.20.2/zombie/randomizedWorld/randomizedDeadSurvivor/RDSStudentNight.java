// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.RoomDef;

/**
 * Create some zombies in varsity outfit + 2 naked zombies in bedroom
 */
@UsedFromLua
public final class RDSStudentNight extends RandomizedDeadSurvivorBase {
    private final ArrayList<String> items = new ArrayList<>();
    private final ArrayList<String> otherItems = new ArrayList<>();
    private final ArrayList<String> pantsMaleItems = new ArrayList<>();
    private final ArrayList<String> pantsFemaleItems = new ArrayList<>();
    private final ArrayList<String> topItems = new ArrayList<>();
    private final ArrayList<String> shoesItems = new ArrayList<>();

    public RDSStudentNight() {
        this.name = "Student Night";
        this.setChance(4);
        this.setMaximumDays(60);
        this.otherItems.add("Base.CigaretteSingle");
        this.otherItems.add("Base.Whiskey");
        this.otherItems.add("Base.Wine");
        this.otherItems.add("Base.Wine2");
        this.otherItems.add("Base.WineBox");
        this.otherItems.add("Base.CigaretteRollingPapers");
        this.items.add("Base.Crisps");
        this.items.add("Base.Crisps2");
        this.items.add("Base.Crisps3");
        this.items.add("Base.Pop");
        this.items.add("Base.Pop2");
        this.items.add("Base.Pop3");
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
        RoomDef room = this.getLivingRoomOrKitchen(def);
        this.addZombies(def, Rand.Next(2, 5), null, null, room);
        RoomDef bedroom = this.getRoomNoKids(def, "bedroom");
        if (bedroom != null) {
            this.addZombies(def, 1, "Naked", 0, bedroom);
            this.addItemsOnGround(bedroom, true);
            this.addZombies(def, 1, "Naked", 100, bedroom);
            this.addItemsOnGround(bedroom, false);
            this.addRandomItemsOnGround(room, this.items, Rand.Next(3, 7));
            this.addRandomItemsOnGround(room, this.otherItems, Rand.Next(2, 6));
            def.alarmed = false;
        }
    }

    private void addItemsOnGround(RoomDef room, boolean male) {
        IsoGridSquare sq = getRandomSpawnSquare(room);
        this.addRandomItemOnGround(sq, this.shoesItems);
        this.addRandomItemOnGround(sq, this.topItems);
        this.addRandomItemOnGround(sq, male ? this.pantsMaleItems : this.pantsFemaleItems);
    }
}
