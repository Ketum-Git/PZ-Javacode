// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSMusicFestStage extends RandomizedZoneStoryBase {
    public RZSMusicFestStage() {
        this.name = "Music Festival Stage";
        this.chance = 100;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.MusicFestStage.toString());
        this.alwaysDo = true;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        for (int i = 0; i < 2; i++) {
            if (Rand.NextBool(4)) {
                this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.GuitarAcoustic");
            } else {
                this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.GuitarElectric");
            }

            if (Rand.NextBool(2)) {
                this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.GuitarPick");
            }
        }

        this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.GuitarElectricBass");
        if (Rand.NextBool(6)) {
            this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.Keytar");
        }

        this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.Speaker");
        this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.Speaker");
        this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.Drumstick");
        this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.Bag_ProtectiveCaseBulky_Audio");
        if (Rand.NextBool(2)) {
            this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.GuitarElectricNeck_Broken");
        }

        if (Rand.NextBool(2)) {
            this.addItemOnGround(this.getRandomFreeSquareFullZone(this, zone), "Base.GuitarElectricBassNeck_Broken");
        }

        this.addZombiesOnSquare(1, "Punk", 0, this.getRandomFreeSquareFullZone(this, zone));
        this.addZombiesOnSquare(1, "Punk", 0, this.getRandomFreeSquareFullZone(this, zone));
        this.addZombiesOnSquare(1, "Punk", 0, this.getRandomFreeSquareFullZone(this, zone));
        this.addZombiesOnSquare(1, "Punk", 0, this.getRandomFreeSquareFullZone(this, zone));
        this.addZombiesOnSquare(1, "Punk", 100, this.getRandomFreeSquareFullZone(this, zone));
    }
}
