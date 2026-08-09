// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSCampsite extends RandomizedZoneStoryBase {
    public RZSCampsite() {
        this.name = "Campsite";
        this.chance = 5;
        this.minZoneHeight = 9;
        this.minZoneWidth = 9;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        this.cleanAreaForStory(this, zone);
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        boolean bench = false;
        boolean fancy = false;
        int logs = 0;
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        if (midSq != null) {
            this.cleanSquareAndNeighbors(midSq);
            int roll = Rand.Next(3);
            IsoObject object = null;
            switch (roll) {
                case 0:
                    this.addCampfire(midSq);
                    break;
                case 1:
                    this.addSimpleCookingPit(midSq);
                    break;
                case 2:
                    this.addCookingPit(midSq);
                    fancy = true;
            }

            if (Rand.NextBool(2)) {
                this.addItemOnGround(midSq, "Base.TinCanEmpty");
            }

            int ns = 0;
            int ew = 0;
            boolean log = !Rand.NextBool(3);
            if (Rand.NextBool(3)) {
                bench = true;
                IsoGridSquare sq = getSq(midX - 1, midY - 2 + 0, zone.z);
                if (log) {
                    this.addTileObject(sq, "crafted_02_58", true);
                } else {
                    this.addTileObject(sq, "furniture_seating_outdoor_01_20", true);
                }

                sq = getSq(midX, midY - 2 + 0, zone.z);
                if (log) {
                    this.addTileObject(sq, "crafted_02_59", true);
                } else {
                    this.addTileObject(sq, "furniture_seating_outdoor_01_21", true);
                }
            }

            if (Rand.NextBool(3)) {
                bench = true;
                IsoGridSquare sqx = getSq(midX - 1, midY + 2 + 0, zone.z);
                if (log) {
                    this.addTileObject(sqx, "crafted_02_58", true);
                } else {
                    this.addTileObject(sqx, "furniture_seating_outdoor_01_20", true);
                }

                sqx = getSq(midX, midY + 2 + 0, zone.z);
                if (log) {
                    this.addTileObject(sqx, "crafted_02_59", true);
                } else {
                    this.addTileObject(sqx, "furniture_seating_outdoor_01_21", true);
                }
            }

            if (Rand.NextBool(3)) {
                bench = true;
                IsoGridSquare sqxx = getSq(midX - 2 + 0, midY, zone.z);
                if (log) {
                    this.addTileObject(sqxx, "crafted_02_57", true);
                } else {
                    this.addTileObject(sqxx, "furniture_seating_outdoor_01_23", true);
                }

                sqxx = getSq(midX - 2 + 0, midY + 1, zone.z);
                if (log) {
                    this.addTileObject(sqxx, "crafted_02_56", true);
                } else {
                    this.addTileObject(sqxx, "furniture_seating_outdoor_01_22", true);
                }
            }

            if (Rand.NextBool(3)) {
                bench = true;
                IsoGridSquare sqxxx = getSq(midX + 2 + 0, midY, zone.z);
                if (log) {
                    this.addTileObject(sqxxx, "crafted_02_57", true);
                } else {
                    this.addTileObject(sqxxx, "furniture_seating_outdoor_01_23", true);
                }

                sqxxx = getSq(midX + 2 + 0, midY + 1, zone.z);
                if (log) {
                    this.addTileObject(sqxxx, "crafted_02_56", true);
                } else {
                    this.addTileObject(sqxxx, "furniture_seating_outdoor_01_22", true);
                }
            }

            if (logs < 2 && Rand.NextBool(3)) {
                logs++;
                this.addTileObject(getSq(midX - 1, midY - 4, zone.z), "camping_01_28", true);
                this.addTileObject(getSq(midX, midY - 4, zone.z), "camping_01_29", true);
            }

            if (logs < 2 && Rand.NextBool(3)) {
                logs++;
                this.addTileObject(getSq(midX - 4, midY, zone.z), "camping_01_35", true);
                this.addTileObject(getSq(midX - 4, midY + 1, zone.z), "camping_01_34", true);
            }

            if (logs < 2 && Rand.NextBool(3)) {
                logs++;
                this.addTileObject(getSq(midX - 1, midY + 4, zone.z), "camping_01_28", true);
                this.addTileObject(getSq(midX, midY + 4, zone.z), "camping_01_29", true);
            }

            if (logs < 2 && Rand.NextBool(3)) {
                this.addTileObject(getSq(midX + 4, midY, zone.z), "camping_01_35", true);
                this.addTileObject(getSq(midX + 4, midY + 1, zone.z), "camping_01_34", true);
            }

            if ((fancy || bench) && Rand.NextBool(3)) {
                IsoGridSquare sqxxxx = this.getRandomExtraFreeSquare(this, zone);
                if (!sqxxxx.isAdjacentTo(midSq)) {
                    this.addTileObject(sqxxxx, "camping_01_6" + (4 + Rand.Next(4)), true);
                }
            }

            if (fancy && bench && Rand.NextBool(3)) {
                IsoGridSquare sqxxxx = this.getRandomExtraFreeSquare(this, zone);
                if (!sqxxxx.isAdjacentTo(midSq)) {
                    this.addTileObject(sqxxxx, "trashcontainers_01_16", true);
                }
            }
        }
    }
}
