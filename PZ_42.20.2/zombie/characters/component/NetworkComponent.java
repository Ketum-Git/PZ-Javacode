// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.component;

import zombie.characters.NetworkCharacterAI;
import zombie.characters.ecs.ECSComponent;
import zombie.network.GameClient;
import zombie.network.GameServer;

public abstract class NetworkComponent extends ECSComponent {
    public abstract boolean isRemote();

    public abstract void updateNetworkAI();

    public abstract NetworkCharacterAI getNetworkAI();

    public final boolean isLocal() {
        return !this.isClient() && !this.isServer() || !this.isRemote();
    }

    public final boolean isServer() {
        return GameServer.server;
    }

    public final boolean isClient() {
        return GameClient.client;
    }
}
