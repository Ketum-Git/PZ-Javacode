// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;

public final class ParameterTripObstacleType extends FMODLocalParameter {
    public ParameterTripObstacleType() {
        super("TripObstacleType");
    }

    public static enum ObstacleType {
        WOOD(0),
        METAL(1),
        SANDBAG(2),
        GRAVELBAG(3),
        BARBWIRE(4),
        TREE(5),
        ZOMBIE(6),
        COLLIDE_WITH_WALL(7),
        METAL_BARS(8),
        WINDOW(9);

        private final int value;

        private ObstacleType(final int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }
}
