// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSMurderScene extends RandomizedZoneStoryBase {
    public RZSMurderScene() {
        this.name = "Murder Scene";
        this.chance = 1;
        this.minZoneHeight = 4;
        this.minZoneWidth = 4;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.setUnique(true);
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        this.cleanAreaForStory(this, zone);
        IsoDeadBody body = null;
        body = createRandomDeadBody(getSq(midX, midY, zone.z), null, true, Rand.Next(100), 10, null, 0);
        if (body != null) {
            this.addBloodSplat(body.getSquare(), 20);

            for (int i = 0; i < body.getWornItems().size(); i++) {
                if (body.getWornItems().get(i).getItem() instanceof Clothing) {
                    ((Clothing)body.getWornItems().get(i).getItem()).randomizeCondition(0, 25, 50, 50);
                }
            }
        }

        if (Rand.Next(2) == 0) {
            int x = zone.x;
            int y = zone.y;
            if (Rand.Next(2) == 0) {
                x += zone.getWidth();
            }

            if (Rand.Next(2) == 0) {
                y += zone.getHeight();
            }

            this.addVehicle(zone, getSq(x, y, zone.z), null, "normalburnt", null, null, null, null);
            if (Rand.Next(2) == 0) {
                InventoryItem square = this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), "Base.EmptyPetrolCan");
            }
        }

        if (Rand.Next(2) == 0) {
            IsoGridSquare square = this.getRandomExtraFreeSquare(this, zone);
            if (square != null) {
                int xx = square.getX();
                int yx = square.getY();
                this.addTileObject(xx, yx, zone.z, "location_community_cemetary_01_32");
                this.addTileObject(xx + 1, yx, zone.z, "location_community_cemetary_01_33");
                this.addItemOnGround(getSq(square.x, square.y + 2, square.z), "Base.Shovel");
            }
        }

        int nbOfItem = Rand.Next(2, 5);

        for (int ix = 0; ix < nbOfItem; ix++) {
            IsoGridSquare square = this.getRandomExtraFreeSquare(this, zone);
            if (square != null) {
                this.addBloodSplat(square, Rand.Next(15, 20));
                InventoryItem item = InventoryItemFactory.CreateItem(getMurderSceneClutterItem());
                if (item instanceof HandWeapon) {
                    item.setBloodLevel(1.0F);
                    item.randomizeGeneralCondition();
                }

                if (item instanceof Clothing clothing) {
                    clothing.randomizeCondition(25, 50, 100, 0);
                    clothing.randomizeCondition(0, 0, 100, 0);
                }

                if (item instanceof DrainableComboItem) {
                    float maxUse = item.getMaxUses();
                    float maxUses = maxUse - 1.0F;
                    item.setCurrentUses(Rand.Next(1, (int)maxUses));
                }

                if (item != null) {
                    this.addItemOnGround(square, item);
                }
            }
        }

        this.addZombiesOnSquare(Rand.Next(3, 4), "MobCasual", 0, this.getRandomExtraFreeSquare(this, zone));
    }
}
