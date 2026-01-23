// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public abstract class AbstractAntiCheat {
    protected AntiCheat antiCheat;

    public boolean update(UdpConnection connection) {
        return true;
    }

    public void react(UdpConnection connection, INetworkPacket packet) {
    }

    public String validate(UdpConnection connection, INetworkPacket packet) {
        return null;
    }

    public boolean preUpdate(UdpConnection connection) {
        return true;
    }

    public void setAntiCheat(AntiCheat antiCheat) {
        this.antiCheat = antiCheat;
    }
}
