// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.zones.Zone;

/**
 * Align some graves with possible flower, possible clutter One grave open with
 *  a zombie
 */
@UsedFromLua
public class RZSBuryingCamp extends RandomizedZoneStoryBase {
    public RZSBuryingCamp() {
        this.name = "Burying Camp";
        this.chance = 7;
        this.minZoneHeight = 6;
        this.minZoneWidth = 6;
        this.minimumDays = 20;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        this.cleanAreaForStory(this, zone);
        boolean horizontal = Rand.NextBool(2);
        int x = zone.x + 1;
        int y = zone.y + 1;
        int openGravesX = 0;
        int openGravesY = 0;
        int gravesNb = Rand.Next(3, 7);

        for (int i = 0; i < gravesNb; i++) {
            if (horizontal) {
                this.addTileObject(x + i, zone.y + 2, zone.z, "location_community_cemetary_01_22");
                if (i == 2) {
                    this.addTileObject(x + i, zone.y + 3, zone.z, "location_community_cemetary_01_35");
                    this.addTileObject(x + i, zone.y + 4, zone.z, "location_community_cemetary_01_34");
                    openGravesX = x + i;
                    openGravesY = zone.y + 5;
                } else {
                    this.addTileObject(x + i, zone.y + 3, zone.z, "location_community_cemetary_01_43");
                    this.addTileObject(x + i, zone.y + 4, zone.z, "location_community_cemetary_01_42");
                    if (Rand.NextBool(2)) {
                        this.addTileObject(x + i, zone.y + 6, zone.z, "vegetation_ornamental_01_" + Rand.Next(16, 19));
                    }
                }
            } else {
                this.addTileObject(zone.x + 2, y + i, zone.z, "location_community_cemetary_01_23");
                if (i == 2) {
                    this.addTileObject(zone.x + 3, y + i, zone.z, "location_community_cemetary_01_32");
                    this.addTileObject(zone.x + 4, y + i, zone.z, "location_community_cemetary_01_33");
                    openGravesX = zone.x + 5;
                    openGravesY = y + i;
                } else {
                    this.addTileObject(zone.x + 3, y + i, zone.z, "location_community_cemetary_01_40");
                    this.addTileObject(zone.x + 4, y + i, zone.z, "location_community_cemetary_01_41");
                    if (Rand.NextBool(2)) {
                        this.addTileObject(zone.x + 6, y + i, zone.z, "vegetation_ornamental_01_" + Rand.Next(16, 19));
                    }
                }
            }
        }

        this.addItemOnGround(getSq(openGravesX + 1, openGravesY + 1, zone.z), "Base.Shovel");
        ArrayList<IsoZombie> zeds = this.addZombiesOnSquare(1, null, null, this.getRandomExtraFreeSquare(this, zone));
        if (zeds != null && !zeds.isEmpty()) {
            IsoZombie eatingZed = zeds.get(0);
            IsoDeadBody body = createRandomDeadBody(getSq(openGravesX, openGravesY, zone.z), null, Rand.Next(7, 12), 0, null);
            if (body != null) {
                this.addBloodSplat(body.getSquare(), 10);
                eatingZed.faceLocationF(body.getX(), body.getY());
                eatingZed.setX(body.getX() + 1.0F);
                eatingZed.setY(body.getY());
                eatingZed.setEatBodyTarget(body, true);
            }
        }

        this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), "Base.WhiskeyEmpty");
        this.addItemOnGround(this.getRandomExtraFreeSquare(this, zone), "Base.WineEmpty");
    }
}
