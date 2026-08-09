// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSTragicPicnic extends RandomizedZoneStoryBase {
    public RZSTragicPicnic() {
        this.name = "Tragic Picnic";
        this.chance = 2;
        this.minZoneHeight = 6;
        this.minZoneWidth = 6;
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
            this.cleanSquareAndNeighbors(midSq);
            InventoryContainer bag = InventoryItemFactory.CreateItem("Base.Bag_PicnicBasket");
            if (bag != null) {
                this.addItemOnGround(midSq, bag);
                ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
                bag.getItemContainer().AddItem("Base.Ring_Left_RingFinger_GoldDiamond");
            }

            IsoGridSquare sq = midSq.getAdjacentSquare(IsoDirections.getRandom());
            if (sq != null) {
                RandomizedZoneStoryBase.cleanSquareForStory(sq);
                if (Rand.NextBool(2)) {
                    this.addItemOnGround(sq, "Base.GuitarAcoustic");
                } else {
                    this.addItemOnGround(sq, "Base.Paperback_Sexy");
                    if (Rand.NextBool(2)) {
                        this.addItemOnGround(sq, "Base.Bra_Strapless_AnimalPrint");
                    }
                }
            }

            IsoGridSquare sq2 = midSq.getAdjacentSquare(IsoDirections.getRandom());
            if (sq2 != null) {
                if (sq != sq2) {
                    RandomizedZoneStoryBase.cleanSquareForStory(sq);
                }

                if (Rand.NextBool(2)) {
                    this.addItemOnGround(sq2, "Base.Wine");
                } else {
                    this.addItemOnGround(sq2, "Base.Wine2");
                }

                this.addItemOnGround(sq2, "Base.Corkscrew");
                this.addItemOnGround(sq2, "Base.GlassWine");
                this.addItemOnGround(sq2, "Base.GlassWine");
            }

            IsoGridSquare sq3 = midSq.getAdjacentSquare(IsoDirections.getRandom());
            if (sq3 != null) {
                if (sq3 != sq && sq3 != sq2) {
                    RandomizedZoneStoryBase.cleanSquareForStory(sq);
                }

                this.addItemOnGround(sq3, "Base.Chocolate_HeartBox");
            }

            if (Rand.NextBool(2)) {
                return;
            }

            IsoGridSquare sq4 = midSq.getAdjacentSquare(IsoDirections.getRandom());
            if (sq4 != null) {
                if (sq4 != sq && sq4 != sq2) {
                    RandomizedZoneStoryBase.cleanSquareForStory(sq);
                }

                this.addItemOnGround(sq4, "Base.Pillow_Heart");
            }
        }
    }
}
