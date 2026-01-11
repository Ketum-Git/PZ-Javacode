// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public final class Weather extends OptionGroup {
    public final BooleanDebugOption fog = this.newDebugOnlyOption("Fog", true);
    public final BooleanDebugOption fx = this.newDebugOnlyOption("Fx", true);
    public final BooleanDebugOption snow = this.newDebugOnlyOption("Snow", true);
    public final BooleanDebugOption showUsablePuddles = this.newDebugOnlyOption("ShowUsablePuddles", false);
    public final BooleanDebugOption waterPuddles = this.newDebugOnlyOption("WaterPuddles", true);
}
