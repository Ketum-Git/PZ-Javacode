// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSHogWild extends RandomizedZoneStoryBase {
    public RZSHogWild() {
        this.name = "Hog Wild";
        this.chance = 1;
        this.minZoneHeight = 2;
        this.minZoneWidth = 2;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.setUnique(true);
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        this.cleanAreaForStory(this, zone);
        int x = zone.x;
        int y = zone.y;
        if (Rand.Next(2) == 0) {
            x += zone.getWidth();
        }

        if (Rand.Next(2) == 0) {
            y += zone.getHeight();
        }

        IsoGridSquare square = this.getRandomExtraFreeSquare(this, zone);
        if (square != null) {
            IsoDeadBody body = this.createSkeletonCorpse(square);
            if (body != null) {
                if (body.getHumanVisual() != null) {
                    body.getHumanVisual().setSkinTextureIndex(2);
                }

                this.addBloodSplat(body.getCurrentSquare(), 20);
            }

            String breed = "largeblack";
            if (Rand.Next(2) == 0) {
                breed = "landrace";
            }

            String pig = "boar";
            if (Rand.Next(2) == 0) {
                pig = "sow";
            }

            IsoAnimal hog = new IsoAnimal(IsoWorld.instance.getCell(), square.getX(), square.getY(), square.getZ(), pig, breed);
            hog.addToWorld();
            hog.randomizeAge();
            this.addBloodSplat(hog.getCurrentSquare(), Rand.Next(7, 12));
            if (Rand.Next(3) == 0) {
                IsoGridSquare square2 = square.getAdjacentSquare(IsoDirections.getRandom());
                if (square2.isFree(true)) {
                    breed = "largeblack";
                    if (Rand.Next(2) == 0) {
                        breed = "landrace";
                    }

                    pig = "boar";
                    if (Rand.Next(2) == 0) {
                        pig = "sow";
                    }

                    IsoAnimal hog2 = new IsoAnimal(IsoWorld.instance.getCell(), square2.getX(), square2.getY(), square2.getZ(), pig, breed);
                    hog2.addToWorld();
                    hog2.randomizeAge();
                    this.addBloodSplat(hog2.getCurrentSquare(), Rand.Next(7, 12));
                }

                if (Rand.Next(4) == 0) {
                    IsoGridSquare square3 = square.getAdjacentSquare(IsoDirections.getRandom());
                    if (square3.isFree(true)) {
                        breed = "largeblack";
                        if (Rand.Next(2) == 0) {
                            breed = "landrace";
                        }

                        pig = "boar";
                        if (Rand.Next(2) == 0) {
                            pig = "sow";
                        }

                        IsoAnimal hog3 = new IsoAnimal(IsoWorld.instance.getCell(), square3.getX(), square3.getY(), square3.getZ(), pig, breed);
                        hog3.addToWorld();
                        hog3.randomizeAge();
                        this.addBloodSplat(hog3.getCurrentSquare(), Rand.Next(7, 12));
                    }
                }
            }
        }
    }
}
