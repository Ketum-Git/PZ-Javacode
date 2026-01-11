// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class VehiclePassengerPositionPacket implements INetworkPacket {
    @JSONField
    protected final VehicleID vehicleId = new VehicleID();
    @JSONField
    protected int seat;
    @JSONField
    protected String position;
    private IsoGameCharacter passenger;

    public void set(BaseVehicle vehicle, int seat, String position) {
        this.vehicleId.set(vehicle);
        this.seat = seat;
        this.position = position;
    }

    @Override
    public void setData(Object... values) {
        this.set((BaseVehicle)values[0], (Integer)values[1], (String)values[2]);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.vehicleId.parse(b, connection);
        this.seat = b.getInt();
        this.position = GameWindow.ReadString(b);
        if (this.vehicleId.isConsistent(connection)) {
            this.passenger = this.vehicleId.getVehicle().getCharacter(this.seat);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.vehicleId.write(b);
        b.putInt(this.seat);
        b.putUTF(this.position);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.vehicleId.isConsistent(connection) && this.passenger != null;
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.vehicleId.getVehicle().setCharacterPosition(this.passenger, this.seat, this.position);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.vehicleId.getVehicle().setCharacterPosition(this.passenger, this.seat, this.position);
        this.sendToClients(packetType, connection);
    }
}
