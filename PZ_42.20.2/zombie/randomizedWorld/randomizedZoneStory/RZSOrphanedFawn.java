// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSOrphanedFawn extends RandomizedZoneStoryBase {
    public RZSOrphanedFawn() {
        this.name = "Orphaned Fawn";
        this.chance = 2;
        this.minZoneHeight = 8;
        this.minZoneWidth = 8;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.setUnique(true);
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        IsoGridSquare square = getRandomExtraFreeUnoccupiedSquare(this, zone);
        if (square != null) {
            RandomizedZoneStoryBase.cleanSquareForStory(square);
            IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), "doe", "whitetailed");
            animal.randomizeAge();
            animal.setHealth(0.0F);
            IsoGridSquare square2 = square.getAdjacentSquare(IsoDirections.getRandom());
            if (square2 != null && !square2.isFree(true)) {
                square2 = getRandomExtraFreeUnoccupiedSquare(this, zone);
            }

            if (square2 != null) {
                IsoAnimal animal2 = new IsoAnimal(IsoWorld.instance.getCell(), square2.getX(), square2.getY(), square2.getZ(), "fawn", "whitetailed");
                animal2.randomizeAge();
                animal2.addToWorld();
                animal2.setDebugStress(100.0F);
            }
        }

        IsoGridSquare square3 = getRandomExtraFreeUnoccupiedSquare(this, zone);
        if (square3 != null) {
            this.addZombiesOnSquare(1, "Hunter", 0, square3);
            ArrayList<String> gun = new ArrayList<>();
            gun.add("Base.VarmintRifle");
            gun.add("Base.HuntingRifle");
            gun.add("Base.Shotgun");
            gun.add("Base.DoubleBarrelShotgun");
            this.addItemOnGround(square3, gun.get(Rand.Next(gun.size())));
        }
    }
}
