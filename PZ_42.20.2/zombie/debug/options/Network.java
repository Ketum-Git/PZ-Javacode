// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import java.util.Objects;
import zombie.debug.BooleanDebugOption;

public final class Network extends OptionGroup {
    public final Network.Client client = new Network.Client(this.group);
    public final Network.Server server = new Network.Server(this.group);
    public final Network.PublicServerUtil publicServerUtil = new Network.PublicServerUtil(this.group);

    public final class Client extends OptionGroup {
        public final BooleanDebugOption mainLoop;
        public final BooleanDebugOption updateZombiesFromPacket;
        public final BooleanDebugOption syncIsoObject;

        public Client(final IDebugOptionGroup parent) {
            Objects.requireNonNull(Network.this);
            super(parent, "Client");
            this.mainLoop = this.newDebugOnlyOption("MainLoop", true);
            this.updateZombiesFromPacket = this.newDebugOnlyOption("UpdateZombiesFromPacket", true);
            this.syncIsoObject = this.newDebugOnlyOption("SyncIsoObject", true);
        }
    }

    public final class PublicServerUtil extends OptionGroup {
        public final BooleanDebugOption enabled;

        public PublicServerUtil(final IDebugOptionGroup parent) {
            Objects.requireNonNull(Network.this);
            super(parent, "PublicServerUtil");
            this.enabled = this.newDebugOnlyOption("Enabled", true);
        }
    }

    public final class Server extends OptionGroup {
        public final BooleanDebugOption syncIsoObject;

        public Server(final IDebugOptionGroup parent) {
            Objects.requireNonNull(Network.this);
            super(parent, "Server");
            this.syncIsoObject = this.newDebugOnlyOption("SyncIsoObject", true);
        }
    }
}
