// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSForestCampEaten extends RandomizedZoneStoryBase {
    public RZSForestCampEaten() {
        this.name = "Forest Camp Eaten";
        this.chance = 10;
        this.minZoneHeight = 6;
        this.minZoneWidth = 10;
        this.minimumDays = 30;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        ArrayList<String> clutter = RZSForestCamp.getForestClutter();
        ArrayList<String> coolerClutter = RZSForestCamp.getCoolerClutter();
        ArrayList<String> fireClutter = RZSForestCamp.getFireClutter();
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        this.cleanAreaForStory(this, zone);
        this.cleanSquareAndNeighbors(midSq);
        this.addCampfireOrPit(midSq);
        this.addItemOnGround(getSq(midX, midY, zone.z), fireClutter.get(Rand.Next(fireClutter.size())));
        int randX = 0;
        int randY = 0;
        this.addRandomTentNorthSouth(midX - 4, midY + 0 - 2, zone.z);
        randX += Rand.Next(1, 3);
        this.addRandomTentNorthSouth(midX - 3 + randX, midY + 0 - 2, zone.z);
        randX += Rand.Next(1, 3);
        this.addRandomTentNorthSouth(midX - 2 + randX, midY + 0 - 2, zone.z);
        if (Rand.NextBool(1)) {
            randX += Rand.Next(1, 3);
            this.addRandomTentNorthSouth(midX - 1 + randX, midY + 0 - 2, zone.z);
        }

        if (Rand.NextBool(2)) {
            randX += Rand.Next(1, 3);
            this.addRandomTentNorthSouth(midX + randX, midY + 0 - 2, zone.z);
        }

        InventoryContainer cooler = InventoryItemFactory.CreateItem("Base.Cooler");
        int nbOfItem = Rand.Next(2, 5);

        for (int i = 0; i < nbOfItem; i++) {
            cooler.getItemContainer().AddItem(coolerClutter.get(Rand.Next(coolerClutter.size())));
        }

        this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), cooler);
        nbOfItem = Rand.Next(3, 7);

        for (int i = 0; i < nbOfItem; i++) {
            this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), clutter.get(Rand.Next(clutter.size())));
        }

        String outfit = "Camper";
        if (Rand.NextBool(2)) {
            outfit = "Backpacker";
        }

        ArrayList<IsoZombie> zombies = this.addZombiesOnSquare(1, outfit, null, this.getRandomExtraFreeSquare(this, zone));
        IsoZombie eatingZed = zombies.isEmpty() ? null : zombies.get(0);
        int randCorpse = Rand.Next(3, 7);
        IsoDeadBody body = null;

        for (int i = 0; i < randCorpse; i++) {
            body = createRandomDeadBody(this.getRandomExtraFreeSquare(this, zone), null, Rand.Next(5, 10), 0, outfit);
            if (body != null) {
                this.addBloodSplat(body.getSquare(), 10);
            }
        }

        body = createRandomDeadBody(getSq(midX, midY + 3, zone.z), null, Rand.Next(5, 10), 0, outfit);
        if (body != null) {
            this.addBloodSplat(body.getSquare(), 10);
            if (eatingZed != null) {
                eatingZed.faceLocationF(body.getX(), body.getY());
                eatingZed.setX(body.getX() + 1.0F);
                eatingZed.setY(body.getY());
                eatingZed.setEatBodyTarget(body, true);
            }
        }
    }
}
