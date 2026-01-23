// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.roads;

import java.util.List;
import java.util.Objects;
import org.joml.Vector2i;
import zombie.core.math.PZMath;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.utils.ChunkCoord;

public class Road {
    private final Vector2i a;
    private final Vector2i b;
    private final ChunkCoord ca;
    private final ChunkCoord cb;
    private final RoadDirection direction;
    private final List<TileGroup> tileGroups;
    private final double probability;

    public Road(Vector2i a, Vector2i b, List<TileGroup> tileGroups, double probability) {
        boolean test = a.lengthSquared() > b.lengthSquared();
        this.a = test ? a : b;
        this.b = test ? b : a;
        this.ca = new ChunkCoord(PZMath.fastfloor(this.a.x / 8.0F), PZMath.fastfloor(this.a.y / 8.0F));
        this.cb = new ChunkCoord(PZMath.fastfloor(this.b.x / 8.0F), PZMath.fastfloor(this.b.y / 8.0F));
        this.direction = a.x == b.x ? RoadDirection.NS : RoadDirection.WE;
        this.tileGroups = tileGroups;
        this.probability = probability;
    }

    public Vector2i getA() {
        return this.a;
    }

    public Vector2i getB() {
        return this.b;
    }

    public ChunkCoord getCA() {
        return this.ca;
    }

    public ChunkCoord getCB() {
        return this.cb;
    }

    public RoadDirection getDirection() {
        return this.direction;
    }

    public List<TileGroup> getSingleFeatures() {
        return this.tileGroups;
    }

    public double getProbability() {
        return this.probability;
    }

    @Override
    public String toString() {
        return String.format("Road{ a=(%d, %d), b=(%d, %d), direction=%s, tiles=%s }", this.a.x, this.a.y, this.b.x, this.b.y, this.direction, this.tileGroups);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            Road road = (Road)o;
            return Objects.equals(this.a, road.a) && Objects.equals(this.b, road.b) && this.direction == road.direction;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.a, this.b, this.direction);
    }
}
