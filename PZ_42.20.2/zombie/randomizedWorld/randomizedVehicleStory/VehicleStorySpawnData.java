// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import zombie.iso.IsoChunk;
import zombie.iso.zones.Zone;

public final class VehicleStorySpawnData {
    public RandomizedVehicleStoryBase story;
    public Zone zone;
    public float spawnX;
    public float spawnY;
    public float direction;
    public int x1;
    public int y1;
    public int x2;
    public int y2;

    public VehicleStorySpawnData(RandomizedVehicleStoryBase story, Zone zone, float spawnX, float spawnY, float direction, int x1, int y1, int x2, int y2) {
        this.story = story;
        this.zone = zone;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
        this.direction = direction;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public boolean isValid(Zone zone, IsoChunk chunk) {
        if (zone != this.zone) {
            return false;
        } else if (!this.story.isFullyStreamedIn(this.x1, this.y1, this.x2, this.y2)) {
            return false;
        } else {
            chunk.setRandomVehicleStoryToSpawnLater(null);
            return this.story.isValid(zone, chunk, false);
        }
    }
}
