// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.component;

import zombie.characters.IsoPlayer;
import zombie.characters.NetworkPlayerAI;

public class NetworkPlayerComponent extends NetworkComponent {
    private final NetworkPlayerAI networkAi;

    public NetworkPlayerComponent(IsoPlayer player) {
        this.networkAi = new NetworkPlayerAI(player);
    }

    @Override
    public boolean isRemote() {
        return !this.isLocalPlayer();
    }

    private boolean isLocalPlayer() {
        return this.getECSOwnerEntity(IsoPlayer.class).isLocalPlayer();
    }

    @Override
    public void updateNetworkAI() {
        this.networkAi.update();
    }

    public NetworkPlayerAI getNetworkAI() {
        return this.networkAi;
    }
}
