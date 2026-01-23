// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.streets;

import gnu.trove.list.array.TFloatArrayList;
import zombie.core.math.PZMath;
import zombie.iso.IsoUtils;
import zombie.worldMap.UIWorldMap;

public final class StreetPoints extends TFloatArrayList {
    float minX = Float.MAX_VALUE;
    float minY;
    float maxX;
    float maxY;

    public int numPoints() {
        return this.size() / 2;
    }

    public void add(float x, float y) {
        this.add(x);
        this.add(y);
    }

    public float getX(int index) {
        return this.get(index * 2);
    }

    public float getY(int index) {
        return this.get(index * 2 + 1);
    }

    public float getMinX() {
        this.calculateBoundIfNeeded();
        return this.minX;
    }

    public float getMinY() {
        this.calculateBoundIfNeeded();
        return this.minY;
    }

    public float getMaxX() {
        this.calculateBoundIfNeeded();
        return this.maxX;
    }

    public float getMaxY() {
        this.calculateBoundIfNeeded();
        return this.maxY;
    }

    public void invalidateBounds() {
        this.minX = Float.MAX_VALUE;
    }

    public void calculateBoundIfNeeded() {
        if (this.minX == Float.MAX_VALUE) {
            this.calculateBounds();
        }
    }

    public void calculateBounds() {
        this.minX = this.minY = Float.MAX_VALUE;
        this.maxX = this.maxY = Float.MIN_VALUE;

        for (int i = 0; i < this.numPoints(); i++) {
            float px = this.getX(i);
            float py = this.getY(i);
            this.minX = PZMath.min(this.minX, px);
            this.minY = PZMath.min(this.minY, py);
            this.maxX = PZMath.max(this.maxX, px);
            this.maxY = PZMath.max(this.maxY, py);
        }
    }

    public boolean isClockwise() {
        float sum = 0.0F;

        for (int i = 0; i < this.numPoints(); i++) {
            float p1x = this.getX(i);
            float p1y = this.getY(i);
            float p2x = this.getX((i + 1) % this.numPoints());
            float p2y = this.getY((i + 1) % this.numPoints());
            sum += (p2x - p1x) * (p2y + p1y);
        }

        return sum > 0.0;
    }

    public void setReverse(StreetPoints dest) {
        dest.resetQuick();

        for (int i = 0; i < this.numPoints(); i++) {
            float x = this.getX(i);
            float y = this.getY(i);
            dest.insert(0, x);
            dest.insert(1, y);
        }
    }

    public float calculateLength(UIWorldMap ui) {
        float length = 0.0F;

        for (int i = 0; i < this.numPoints() - 1; i++) {
            float worldX1 = this.getX(i);
            float worldY1 = this.getY(i);
            float worldX2 = this.getX(i + 1);
            float worldY2 = this.getY(i + 1);
            float uiX1 = ui.getAPI().worldToUIX(worldX1, worldY1);
            float uiY1 = ui.getAPI().worldToUIY(worldX1, worldY1);
            float uiX2 = ui.getAPI().worldToUIX(worldX2, worldY2);
            float uiY2 = ui.getAPI().worldToUIY(worldX2, worldY2);
            length += IsoUtils.DistanceTo(uiX1, uiY1, uiX2, uiY2);
        }

        return length;
    }

    public float calculateLength() {
        float length = 0.0F;

        for (int i = 0; i < this.numPoints() - 1; i++) {
            float worldX1 = this.getX(i);
            float worldY1 = this.getY(i);
            float worldX2 = this.getX(i + 1);
            float worldY2 = this.getY(i + 1);
            length += IsoUtils.DistanceTo(worldX1, worldY1, worldX2, worldY2);
        }

        return length;
    }
}
