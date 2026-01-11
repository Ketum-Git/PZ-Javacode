// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import java.nio.ByteBuffer;
import org.joml.Vector3f;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.Bullet;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class VehicleImpulsePacket implements INetworkPacket {
    @JSONField
    protected final VehicleID vehicleId = new VehicleID();
    private final Vector3f impulse = new Vector3f();
    private final Vector3f position = new Vector3f();
    private final Vector3f torque = new Vector3f();

    public void set(BaseVehicle vehicle, Vector3f impulse, Vector3f position) {
        this.vehicleId.set(vehicle);
        this.impulse.set(impulse);
        this.position.set(position);
    }

    @Override
    public void setData(Object... values) {
        this.set((BaseVehicle)values[0], (Vector3f)values[1], (Vector3f)values[2]);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.vehicleId.parse(b, connection);
        this.impulse.set(b.getFloat(), b.getFloat(), b.getFloat());
        this.position.set(b.getFloat(), b.getFloat(), b.getFloat());
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.vehicleId.write(b);
        b.putFloat(this.impulse.x);
        b.putFloat(this.impulse.y);
        b.putFloat(this.impulse.z);
        b.putFloat(this.position.x);
        b.putFloat(this.position.y);
        b.putFloat(this.position.z);
    }

    @Override
    public void processClient(UdpConnection connection) {
        Bullet.applyCentralForceToVehicle(this.vehicleId.getID(), this.impulse.x, this.impulse.y, this.impulse.z);
        this.torque.set(this.position.cross(this.impulse));
        Bullet.applyTorqueToVehicle(this.vehicleId.getID(), this.torque.x, this.torque.y, this.torque.z);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.vehicleId.isConsistent(connection);
    }
}
