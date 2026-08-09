// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleNetworkSound;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;
import zombie.vehicleNetworkSound.client.Manager;

@PacketSetting(ordering = 8, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 6)
public final class AddVehiclePacket extends SharedVehicleState implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        SharedVehicleState state = (SharedVehicleState)values[0];
        this.set(state);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        super.parse(bb);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        this.processClient(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        Manager mgr = Manager.getInstance();
        mgr.addVehicle(this.id, this.scriptName);
        mgr.updateVehicle(this);
    }
}
