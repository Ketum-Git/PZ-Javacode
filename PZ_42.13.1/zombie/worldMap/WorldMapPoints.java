// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.nio.ShortBuffer;
import zombie.core.math.PZMath;
import zombie.iso.IsoUtils;

public final class WorldMapPoints {
    WorldMapGeometry owner;
    short firstPoint;
    short pointCount;
    int minX = Integer.MAX_VALUE;
    int minY;
    int maxX;
    int maxY;

    public WorldMapPoints(WorldMapGeometry owner) {
        this.owner = owner;
    }

    public void setPoints(short firstPoint, short pointCount) {
        this.firstPoint = firstPoint;
        this.pointCount = pointCount;
    }

    public int numPoints() {
        return this.pointCount;
    }

    public int getX(int index) {
        ShortBuffer pointBuffer = this.owner.cell.pointBuffer;
        return pointBuffer.get(this.firstPoint + index * 2);
    }

    public int getY(int index) {
        ShortBuffer pointBuffer = this.owner.cell.pointBuffer;
        return pointBuffer.get(this.firstPoint + index * 2 + 1);
    }

    public int getMinX() {
        this.calculateBoundIfNeeded();
        return this.minX;
    }

    public int getMinY() {
        this.calculateBoundIfNeeded();
        return this.minY;
    }

    public int getMaxX() {
        this.calculateBoundIfNeeded();
        return this.maxX;
    }

    public int getMaxY() {
        this.calculateBoundIfNeeded();
        return this.maxY;
    }

    public void invalidateBounds() {
        this.minX = Integer.MAX_VALUE;
    }

    public void calculateBoundIfNeeded() {
        if (this.minX == Integer.MAX_VALUE) {
            this.calculateBounds();
        }
    }

    public void calculateBounds() {
        this.minX = this.minY = Integer.MAX_VALUE;
        this.maxX = this.maxY = Integer.MIN_VALUE;

        for (int i = 0; i < this.numPoints(); i++) {
            int px = this.getX(i);
            int py = this.getY(i);
            this.minX = PZMath.min(this.minX, px);
            this.minY = PZMath.min(this.minY, py);
            this.maxX = PZMath.max(this.maxX, px);
            this.maxY = PZMath.max(this.maxY, py);
        }
    }

    public boolean isClockwise() {
        float sum = 0.0F;

        for (int i = 0; i < this.numPoints(); i++) {
            int p1x = this.getX(i);
            int p1y = this.getY(i);
            int p2x = this.getX((i + 1) % this.numPoints());
            int p2y = this.getY((i + 1) % this.numPoints());
            sum += (p2x - p1x) * (p2y + p1y);
        }

        return sum > 0.0;
    }

    public float calculateLength(UIWorldMap ui) {
        float length = 0.0F;

        for (int i = 0; i < this.numPoints() - 1; i++) {
            int worldX1 = this.getX(i);
            int worldY1 = this.getY(i);
            int worldX2 = this.getX(i + 1);
            int worldY2 = this.getY(i + 1);
            float uiX1 = ui.getAPI().worldToUIX(worldX1, worldY1);
            float uiY1 = ui.getAPI().worldToUIY(worldX1, worldY1);
            float uiX2 = ui.getAPI().worldToUIX(worldX2, worldY2);
            float uiY2 = ui.getAPI().worldToUIY(worldX2, worldY2);
            length += IsoUtils.DistanceTo(uiX1, uiY1, uiX2, uiY2);
        }

        return length;
    }
}
