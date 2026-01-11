// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class WorldItemAtlas extends OptionGroup {
    public final BooleanDebugOption enable = this.newDebugOnlyOption("Enable", true);
    public final BooleanDebugOption render = this.newDebugOnlyOption("Render", false);
}
