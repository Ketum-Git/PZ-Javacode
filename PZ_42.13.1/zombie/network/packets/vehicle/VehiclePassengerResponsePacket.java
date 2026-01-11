// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PassengerMap;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class VehiclePassengerResponsePacket implements INetworkPacket {
    @JSONField
    protected final VehicleID vehicleId = new VehicleID();
    @JSONField
    protected int seat;
    @JSONField
    protected int wx;
    @JSONField
    protected int wy;
    @JSONField
    protected long loaded;

    public void set(BaseVehicle vehicle, int seat, int wx, int wy, long loaded) {
        this.vehicleId.set(vehicle);
        this.seat = seat;
        this.wx = wx;
        this.wy = wy;
        this.loaded = loaded;
    }

    @Override
    public void setData(Object... values) {
        this.set((BaseVehicle)values[0], (Integer)values[1], (Integer)values[2], (Integer)values[3], (Long)values[4]);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.vehicleId.parse(b, connection);
        this.seat = b.getInt();
        this.wx = b.getInt();
        this.wy = b.getInt();
        this.loaded = b.getLong();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.vehicleId.write(b);
        b.putInt(this.seat);
        b.putInt(this.wx);
        b.putInt(this.wy);
        b.putLong(this.loaded);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.vehicleId.isConsistent(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            IsoPlayer player = IsoPlayer.players[pn];
            if (player != null && player.getVehicle() != null) {
                BaseVehicle vehicle = player.getVehicle();
                if (vehicle.vehicleId == this.vehicleId.getID() && vehicle.isDriver(player)) {
                    PassengerMap.clientReceivePacket(pn, this.seat, this.wx, this.wy, this.loaded);
                }
            }
        }
    }
}
