// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class Cheat extends OptionGroup {
    public final Cheat.ClockOG clock = this.newOptionGroup(new Cheat.ClockOG());
    public final Cheat.DoorOG door = this.newOptionGroup(new Cheat.DoorOG());
    public final Cheat.PlayerOG player = this.newOptionGroup(new Cheat.PlayerOG());
    public final Cheat.TimedActionOG timedAction = this.newOptionGroup(new Cheat.TimedActionOG());
    public final Cheat.VehicleOG vehicle = this.newOptionGroup(new Cheat.VehicleOG());
    public final Cheat.WindowOG window = this.newOptionGroup(new Cheat.WindowOG());
    public final Cheat.FarmingOG farming = this.newOptionGroup(new Cheat.FarmingOG());

    public static final class ClockOG extends OptionGroup {
        public final BooleanDebugOption visible = this.newDebugOnlyOption("Visible", false);
    }

    public static final class DoorOG extends OptionGroup {
        public final BooleanDebugOption unlock = this.newDebugOnlyOption("Unlock", false);
    }

    public static final class FarmingOG extends OptionGroup {
        public final BooleanDebugOption fastGrow = this.newDebugOnlyOption("FastGrow", false);
    }

    public static final class PlayerOG extends OptionGroup {
        public final BooleanDebugOption startInvisible = this.newDebugOnlyOption("StartInvisible", false);
        public final BooleanDebugOption invisibleSprint = this.newDebugOnlyOption("InvisibleSprint", false);
        public final BooleanDebugOption seeEveryone = this.newDebugOnlyOption("SeeEveryone", false);
        public final BooleanDebugOption unlimitedCondition = this.newDebugOnlyOption("UnlimitedCondition", false);
    }

    public static final class TimedActionOG extends OptionGroup {
        public final BooleanDebugOption instant = this.newDebugOnlyOption("Instant", false);
    }

    public static final class VehicleOG extends OptionGroup {
        public final BooleanDebugOption mechanicsAnywhere = this.newDebugOnlyOption("MechanicsAnywhere", false);
        public final BooleanDebugOption startWithoutKey = this.newDebugOnlyOption("StartWithoutKey", false);
    }

    public static final class WindowOG extends OptionGroup {
        public final BooleanDebugOption unlock = this.newDebugOnlyOption("Unlock", false);
    }
}
