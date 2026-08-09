// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public class Multiplayer extends OptionGroup {
    public final Multiplayer.DebugOG debug = new Multiplayer.DebugOG(this.group);
    public final Multiplayer.DebugFlagsOG debugFlags = new Multiplayer.DebugFlagsOG(this.group);

    public static final class DebugFlagsOG extends OptionGroup {
        public final Multiplayer.DebugFlagsOG.IsoGameCharacterOG localPlayer = new Multiplayer.DebugFlagsOG.IsoGameCharacterOG(this.group, "LocalPlayer");
        public final Multiplayer.DebugFlagsOG.IsoGameCharacterOG remotePlayer = new Multiplayer.DebugFlagsOG.IsoGameCharacterOG(this.group, "RemotePlayer");
        public final Multiplayer.DebugFlagsOG.IsoGameCharacterOG animal = new Multiplayer.DebugFlagsOG.IsoGameCharacterOG(this.group, "Animal");
        public final Multiplayer.DebugFlagsOG.IsoGameCharacterOG zombie = new Multiplayer.DebugFlagsOG.IsoGameCharacterOG(this.group, "Zombie");
        public final Multiplayer.DebugFlagsOG.IsoDeadBodyOG deadBody = new Multiplayer.DebugFlagsOG.IsoDeadBodyOG(this.group);

        DebugFlagsOG(IDebugOptionGroup parentGroup) {
            super(parentGroup, "DebugFlags");
        }

        public static final class IsoDeadBodyOG extends OptionGroup {
            public final BooleanDebugOption enable = this.newDebugOnlyOption("Enable", false);
            public final BooleanDebugOption position = this.newDebugOnlyOption("Position", false);

            IsoDeadBodyOG(IDebugOptionGroup parentGroup) {
                super(parentGroup, "DeadBody");
            }
        }

        public static final class IsoGameCharacterOG extends OptionGroup {
            public final BooleanDebugOption enable = this.newDebugOnlyOption("Enable", false);
            public final BooleanDebugOption position = this.newDebugOnlyOption("Position", false);
            public final BooleanDebugOption prediction = this.newDebugOnlyOption("Prediction", false);
            public final BooleanDebugOption state = this.newDebugOnlyOption("State", false);
            public final BooleanDebugOption stateVariables = this.newDebugOnlyOption("StateVariables", false);
            public final BooleanDebugOption animation = this.newDebugOnlyOption("Animation", false);
            public final BooleanDebugOption variables = this.newDebugOnlyOption("Variables", false);

            IsoGameCharacterOG(IDebugOptionGroup parentGroup, String name) {
                super(parentGroup, name);
            }
        }
    }

    public static final class DebugOG extends OptionGroup {
        public final BooleanDebugOption attackPlayer = this.newDebugOnlyOption("Attack.Player", false);
        public final BooleanDebugOption followPlayer = this.newDebugOnlyOption("Follow.Player", false);
        public final BooleanDebugOption seeNonPvpZones = this.newDebugOnlyOption("SeeNonPvpZones", false);
        public final BooleanDebugOption anticlippingAlgorithm = this.newDebugOnlyOption("AnticlippingAlgorithm", true);

        DebugOG(IDebugOptionGroup parentGroup) {
            super(parentGroup, "Debug");
        }
    }
}
