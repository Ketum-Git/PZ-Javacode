// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.iso.zones.Zone;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class RZSSexyTime extends RandomizedZoneStoryBase {
    private final ArrayList<String> pantsMaleItems = new ArrayList<>();
    private final ArrayList<String> pantsFemaleItems = new ArrayList<>();
    private final ArrayList<String> topItems = new ArrayList<>();
    private final ArrayList<String> shoesItems = new ArrayList<>();

    public RZSSexyTime() {
        this.name = "Sexy Time";
        this.chance = 5;
        this.minZoneHeight = 5;
        this.minZoneWidth = 5;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Beach.toString());
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Lake.toString());
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
    public void randomizeZoneStory(Zone zone) {
        this.cleanAreaForStory(this, zone);
        BaseVehicle v = this.addVehicle(
            zone, getSq(zone.pickedXForZoneStory, zone.pickedYForZoneStory, zone.z), null, null, "Base.VanAmbulance", null, null, null
        );
        if (v != null) {
            v.setAlarmed(false);
            v.setPreviouslyMoved(true);
        }

        boolean twoMale = Rand.Next(7) == 0;
        boolean twoFemale = Rand.Next(7) == 0;
        if (twoMale) {
            this.addItemsOnGround(zone, true, v);
            this.addItemsOnGround(zone, true, v);
        } else if (twoFemale) {
            this.addItemsOnGround(zone, false, v);
            this.addItemsOnGround(zone, false, v);
        } else {
            this.addItemsOnGround(zone, true, v);
            this.addItemsOnGround(zone, false, v);
        }
    }

    private void addItemsOnGround(Zone zone, boolean male, BaseVehicle v) {
        int maleChance = 100;
        if (!male) {
            maleChance = 0;
        }

        ArrayList<IsoZombie> zeds = this.addZombiesOnVehicle(1, "Naked", maleChance, v);
        if (!zeds.isEmpty()) {
            IsoZombie zed = zeds.get(0);
            this.addRandomItemOnGround(zed.getSquare(), this.shoesItems);
            this.addRandomItemOnGround(zed.getSquare(), this.topItems);
            this.addRandomItemOnGround(zed.getSquare(), male ? this.pantsMaleItems : this.pantsFemaleItems);
        }
    }
}
