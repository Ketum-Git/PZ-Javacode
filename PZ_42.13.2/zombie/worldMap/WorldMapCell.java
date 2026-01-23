// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import org.lwjgl.system.MemoryUtil;

public final class WorldMapCell {
    public int x;
    public int y;
    public final ArrayList<WorldMapFeature> features = new ArrayList<>();
    public int priority = -1;
    public int[] clipHigherPriorityCells;
    public ShortBuffer pointBuffer;
    public ShortBuffer indexBuffer;
    public FloatBuffer triangleBuffer;

    public void hitTest(float x, float y, ArrayList<WorldMapFeature> features) {
        x -= this.x * 256;
        y -= this.y * 256;

        for (int i = 0; i < this.features.size(); i++) {
            WorldMapFeature feature = this.features.get(i);
            if (feature.containsPoint(x, y)) {
                features.add(feature);
            }
        }
    }

    public ShortBuffer getPointBuffer(int addPoints) {
        if (this.pointBuffer == null) {
            return this.pointBuffer = MemoryUtil.memAllocShort(addPoints * 2);
        } else {
            int newSize = this.pointBuffer.position() + addPoints * 2;
            return this.pointBuffer.capacity() < this.pointBuffer.position() + newSize
                ? (this.pointBuffer = MemoryUtil.memRealloc(this.pointBuffer, newSize))
                : this.pointBuffer;
        }
    }

    public ShortBuffer getIndexBuffer(int count) {
        if (this.indexBuffer == null) {
            return this.indexBuffer = MemoryUtil.memAllocShort(count);
        } else {
            int newSize = this.indexBuffer.position() + count;
            return this.indexBuffer.capacity() < this.indexBuffer.position() + newSize
                ? (this.indexBuffer = MemoryUtil.memRealloc(this.indexBuffer, newSize))
                : this.indexBuffer;
        }
    }

    public FloatBuffer getTriangleBuffer(int count) {
        if (this.triangleBuffer == null) {
            return this.triangleBuffer = MemoryUtil.memAllocFloat(count);
        } else {
            int newSize = this.triangleBuffer.position() + count;
            return this.triangleBuffer.capacity() < this.triangleBuffer.position() + newSize
                ? (this.triangleBuffer = MemoryUtil.memRealloc(this.triangleBuffer, newSize))
                : this.triangleBuffer;
        }
    }

    public void clearTriangles() {
        this.priority = -1;
        this.clipHigherPriorityCells = null;

        for (WorldMapFeature feature : this.features) {
            feature.clearTriangles();
        }

        MemoryUtil.memFree(this.indexBuffer);
        this.indexBuffer = null;
        MemoryUtil.memFree(this.triangleBuffer);
        this.triangleBuffer = null;
    }

    public void dispose() {
        for (WorldMapFeature feature : this.features) {
            feature.dispose();
        }

        this.features.clear();
        this.priority = -1;
        this.clipHigherPriorityCells = null;
        MemoryUtil.memFree(this.pointBuffer);
        this.pointBuffer = null;
        MemoryUtil.memFree(this.indexBuffer);
        this.indexBuffer = null;
        MemoryUtil.memFree(this.triangleBuffer);
        this.triangleBuffer = null;
    }
}
