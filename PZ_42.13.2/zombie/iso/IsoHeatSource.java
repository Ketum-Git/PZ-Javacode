// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;

@UsedFromLua
public class IsoHeatSource {
    private final int x;
    private final int y;
    private final int z;
    private int radius;
    private int temperature;

    public IsoHeatSource(int x, int y, int z, int radius, int temperature) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.temperature = temperature;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public int getRadius() {
        return this.radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public int getTemperature() {
        return this.temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public boolean isInBounds(int minX, int minY, int maxX, int maxY) {
        return this.x >= minX && this.x < maxX && this.y >= minY && this.y < maxY;
    }

    public boolean isInBounds() {
        IsoChunkMap[] ChunkMap = IsoWorld.instance.currentCell.chunkMap;

        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            if (!ChunkMap[pn].ignore) {
                int minX = ChunkMap[pn].getWorldXMinTiles();
                int maxX = ChunkMap[pn].getWorldXMaxTiles();
                int minY = ChunkMap[pn].getWorldYMinTiles();
                int maxY = ChunkMap[pn].getWorldYMaxTiles();
                if (this.isInBounds(minX, minY, maxX, maxY)) {
                    return true;
                }
            }
        }

        return false;
    }
}
