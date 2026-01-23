// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class VehicleEnterPacket implements INetworkPacket {
    @JSONField
    protected final VehicleID vehicleId = new VehicleID();
    @JSONField
    protected final PlayerID playerId = new PlayerID();
    @JSONField
    protected int seatTo;
    private IsoPlayer passenger;

    public void set(BaseVehicle vehicle, IsoPlayer player, int seatTo) {
        this.vehicleId.set(vehicle);
        this.playerId.set(player);
        this.seatTo = seatTo;
    }

    @Override
    public void setData(Object... values) {
        this.set((BaseVehicle)values[0], (IsoPlayer)values[1], (Integer)values[2]);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.vehicleId.parse(b, connection);
        this.playerId.parse(b, connection);
        this.seatTo = b.getInt();
        if (this.vehicleId.getVehicle() != null) {
            this.passenger = Type.tryCastTo(this.vehicleId.getVehicle().getCharacter(this.seatTo), IsoPlayer.class);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.vehicleId.write(b);
        this.playerId.write(b);
        b.putInt(this.seatTo);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.vehicleId.isConsistent(connection)
            && this.playerId.isConsistent(connection)
            && (this.passenger == null || this.passenger == this.playerId.getPlayer());
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.vehicleId.getVehicle().enterRSync(this.seatTo, this.playerId.getPlayer(), this.vehicleId.getVehicle());
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.vehicleId.getVehicle().enter(this.seatTo, this.playerId.getPlayer());
        if (this.seatTo == 0 && this.vehicleId.getVehicle().isNetPlayerAuthorization(BaseVehicle.Authorization.Server)) {
            this.vehicleId.getVehicle().authorizationServerOnSeat(this.playerId.getPlayer(), true);
        }

        this.sendToClients(packetType, null);
    }
}
