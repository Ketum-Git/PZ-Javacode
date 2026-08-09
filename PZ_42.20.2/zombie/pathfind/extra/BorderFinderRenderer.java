// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.extra;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;

public class BorderFinderRenderer {
    public static final BorderFinderRenderer instance = new BorderFinderRenderer();
    private final Set<Position> path = new HashSet<>();
    private final Object renderLock = new Object();

    private BorderFinderRenderer() {
    }

    public void addAllPath(Collection<Position> positions) {
        this.path.addAll(positions);
    }

    public void render() {
        if (DebugOptions.instance.pathfindBorderFinder.getValue()) {
            for (Position position : this.path) {
                LineDrawer.addLine(
                    position.coords().x() + 0.45F,
                    position.coords().y() + 0.45F,
                    position.coords().z(),
                    position.coords().x() + 0.55F,
                    position.coords().y() + 0.55F,
                    position.coords().z(),
                    0.5F,
                    1.0F,
                    0.5F,
                    null,
                    false
                );

                for (Direction direction : Direction.values()) {
                    float r = 0.0F;
                    float g = 0.0F;
                    float b = 0.0F;
                    if (position.walls().get(direction) == BorderStatus.OUT_OF_RANGE) {
                        r = 1.0F;
                    } else {
                        b = 1.0F;
                    }

                    if (position.walls().get(direction) != BorderStatus.OPEN) {
                        float xmin = switch (direction) {
                            case NORTH, SOUTH -> 0.0F;
                            case WEST -> 0.2F;
                            case EAST -> 0.8F;
                            default -> 0.5F;
                        };

                        float xmax = switch (direction) {
                            case NORTH, SOUTH -> 1.0F;
                            case WEST -> 0.2F;
                            case EAST -> 0.8F;
                            default -> 0.5F;
                        };

                        float ymin = switch (direction) {
                            case NORTH -> 0.2F;
                            case SOUTH -> 0.8F;
                            case WEST, EAST -> 0.0F;
                            default -> 0.5F;
                        };

                        float ymax = switch (direction) {
                            case NORTH -> 0.2F;
                            case SOUTH -> 0.8F;
                            case WEST, EAST -> 1.0F;
                            default -> 0.5F;
                        };
                        LineDrawer.addLine(
                            position.coords().x() + xmin,
                            position.coords().y() + ymin,
                            position.coords().z(),
                            position.coords().x() + xmax,
                            position.coords().y() + ymax,
                            position.coords().z(),
                            r,
                            0.0F,
                            b,
                            null,
                            false
                        );
                    }
                }
            }
        }
    }
}
