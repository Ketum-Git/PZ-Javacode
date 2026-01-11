// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class VehicleRequestPacket extends VehicleID implements INetworkPacket {
    @JSONField
    protected short flag;

    @Override
    public void setData(Object... values) {
        this.setID((Short)values[0]);
        this.flag = (Short)values[1];
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.flag = b.getShort();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putShort(this.flag);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.vehicle.connectionState[connection.index] == null) {
            this.vehicle.connectionState[connection.index] = new BaseVehicle.ServerVehicleState();
        }

        if (this.flag == 16384) {
            if (!connection.RelevantTo(this.vehicle.getX(), this.vehicle.getY())) {
                INetworkPacket.send(connection, PacketTypes.PacketType.VehicleRemove, this.vehicle);
            }
        } else {
            this.vehicle.connectionState[connection.index].flags = (short)(this.vehicle.connectionState[connection.index].flags | this.flag);
        }
    }
}
