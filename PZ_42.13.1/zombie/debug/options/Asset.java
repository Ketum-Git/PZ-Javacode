// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class Asset extends OptionGroup {
    public final BooleanDebugOption slowLoad = this.newOption("SlowLoad", false);
    public final BooleanDebugOption checkItemTexAndNames = this.newOption("CheckItemTexAndNames", false);
}
