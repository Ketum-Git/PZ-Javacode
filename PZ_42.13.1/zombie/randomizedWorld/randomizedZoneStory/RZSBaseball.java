// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSBaseball extends RandomizedZoneStoryBase {
    public RZSBaseball() {
        this.name = "Baseball";
        this.chance = 100;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Baseball.toString());
        this.minZoneWidth = 20;
        this.minZoneHeight = 20;
        this.alwaysDo = true;
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        int team1 = Rand.Next(0, 3);
        int team2 = Rand.Next(0, 3);

        while (team1 == team2) {
            team2 = Rand.Next(0, 3);
        }

        String team1Outfit = "BaseballPlayer_KY";
        if (team1 == 1) {
            team1Outfit = "BaseballPlayer_Rangers";
        }

        if (team1 == 2) {
            team1Outfit = "BaseballPlayer_Z";
        }

        String team2Outfit = "BaseballPlayer_KY";
        if (team2 == 1) {
            team2Outfit = "BaseballPlayer_Rangers";
        }

        if (team2 == 2) {
            team2Outfit = "BaseballPlayer_Z";
        }

        for (int i = 0; i < 20; i++) {
            if (Rand.NextBool(4)) {
                this.addItemOnGround(this.getRandomFreeSquare(this, zone), "Base.BaseballBat");
            }

            if (Rand.NextBool(6)) {
                this.addItemOnGround(this.getRandomFreeSquare(this, zone), "Base.Baseball");
            }
        }

        for (int i = 0; i <= 9; i++) {
            this.addZombiesOnSquare(1, team1Outfit, 0, this.getRandomFreeSquare(this, zone));
            this.addZombiesOnSquare(1, team2Outfit, 0, this.getRandomFreeSquare(this, zone));
        }
    }
}
