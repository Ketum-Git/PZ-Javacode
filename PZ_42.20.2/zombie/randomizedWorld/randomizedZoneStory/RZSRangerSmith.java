// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSRangerSmith extends RandomizedZoneStoryBase {
    public RZSRangerSmith() {
        this.name = "Ranger Smith";
        this.chance = 1;
        this.minZoneHeight = 4;
        this.minZoneWidth = 4;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.setUnique(true);
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        this.cleanAreaForStory(this, zone);
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        if (midSq != null) {
            IsoDeadBody body = createRandomDeadBody(midSq, null, 100, 0, "Ranger");
            if (body != null) {
                this.addBloodSplat(midSq, 100);

                for (int i = 0; i < body.getWornItems().size(); i++) {
                    if (body.getWornItems().get(i).getItem() instanceof Clothing) {
                        ((Clothing)body.getWornItems().get(i).getItem()).randomizeCondition(0, 25, 50, 50);
                    }
                }
            }

            IsoGridSquare sq = midSq.getAdjacentSquare(IsoDirections.getRandom());
            if (sq != null) {
                InventoryContainer bag = InventoryItemFactory.CreateItem("Base.Bag_PicnicBasket");
                if (bag != null) {
                    this.addItemOnGround(sq, bag);
                    ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
                }
            }
        }
    }
}
