// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class VehicleTowingAttachPacket implements INetworkPacket {
    @JSONField
    protected final VehicleID vehicleA = new VehicleID();
    @JSONField
    protected final VehicleID vehicleB = new VehicleID();
    @JSONField
    protected String attachmentA;
    @JSONField
    protected String attachmentB;

    public void set(BaseVehicle vehicleA, BaseVehicle vehicleB, String attachmentA, String attachmentB) {
        if (vehicleA == null) {
            this.vehicleA.setID((short)-1);
        } else {
            this.vehicleA.set(vehicleA);
        }

        if (vehicleB == null) {
            this.vehicleB.setID((short)-1);
        } else {
            this.vehicleB.set(vehicleB);
        }

        this.attachmentA = attachmentA;
        this.attachmentB = attachmentB;
    }

    @Override
    public void setData(Object... values) {
        this.set((BaseVehicle)values[0], (BaseVehicle)values[1], (String)values[2], (String)values[3]);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.vehicleA.parse(b, connection);
        this.vehicleB.parse(b, connection);
        this.attachmentA = GameWindow.ReadString(b);
        this.attachmentB = GameWindow.ReadString(b);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.vehicleA.write(b);
        this.vehicleB.write(b);
        b.putUTF(this.attachmentA);
        b.putUTF(this.attachmentB);
    }

    @Override
    public void processClient(UdpConnection connection) {
        VehicleManager.instance.towedVehicleMap.put(this.vehicleA.getID(), this.vehicleB.getID());
        if (this.vehicleA.getVehicle() != null && this.vehicleB.getVehicle() != null) {
            this.vehicleA.getVehicle().addPointConstraint(null, this.vehicleB.getVehicle(), this.attachmentA, this.attachmentB);
        }
    }
}
