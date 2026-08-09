// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class CollideWithObstacles extends OptionGroup {
    public final CollideWithObstacles.DebugOG debug = this.newOptionGroup(new CollideWithObstacles.DebugOG());
    public final CollideWithObstacles.RenderOG render = this.newOptionGroup(new CollideWithObstacles.RenderOG());

    public static final class DebugOG extends OptionGroup {
        public final BooleanDebugOption slideAwayFromWalls = this.newDebugOnlyOption("SlideAwayFromWalls", true);
    }

    public static final class RenderOG extends OptionGroup {
        public final BooleanDebugOption radius = this.newDebugOnlyOption("Radius", false);
        public final BooleanDebugOption obstacles = this.newDebugOnlyOption("Obstacles", false);
        public final BooleanDebugOption normals = this.newDebugOnlyOption("Normals", false);
    }
}
