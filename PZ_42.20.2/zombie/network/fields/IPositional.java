// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

public interface IPositional {
    float getX();

    float getY();

    float getZ();

    default boolean isInRange(IPositional other, float range) {
        return other != null
            && Math.abs(this.getX() - other.getX()) < range
            && Math.abs(this.getY() - other.getY()) < range
            && Math.abs(this.getZ() - other.getZ()) < range;
    }
}
