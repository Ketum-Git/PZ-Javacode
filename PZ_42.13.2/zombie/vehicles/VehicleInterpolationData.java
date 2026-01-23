// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.nio.ByteBuffer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.Transform;
import zombie.core.physics.WorldSimulation;
import zombie.core.raknet.UdpConnection;
import zombie.network.fields.INetworkPacketField;

/**
 * Created by kroto on 1/17/2017.
 */
public class VehicleInterpolationData implements Comparable<VehicleInterpolationData>, INetworkPacketField {
    private final Transform tempTransform = new Transform();
    protected long time;
    protected float x;
    protected float y;
    protected float z;
    protected float qx;
    protected float qy;
    protected float qz;
    protected float qw;
    protected float vx;
    protected float vy;
    protected float vz;
    protected float engineSpeed;
    protected float throttle;
    protected short wheelsCount = 4;
    protected float[] wheelSteering = new float[4];
    protected float[] wheelRotation = new float[4];
    protected float[] wheelSkidInfo = new float[4];
    protected float[] wheelSuspensionLength = new float[4];

    protected void setNumWheels(short count) {
        if (count > this.wheelsCount) {
            this.wheelSteering = new float[count];
            this.wheelRotation = new float[count];
            this.wheelSkidInfo = new float[count];
            this.wheelSuspensionLength = new float[count];
        }

        this.wheelsCount = count;
    }

    void copy(VehicleInterpolationData a) {
        this.time = a.time;
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.qx = a.qx;
        this.qy = a.qy;
        this.qz = a.qz;
        this.qw = a.qw;
        this.vx = a.vx;
        this.vy = a.vy;
        this.vz = a.vz;
        this.engineSpeed = a.engineSpeed;
        this.throttle = a.throttle;
        this.setNumWheels(a.wheelsCount);

        for (int i = 0; i < a.wheelsCount; i++) {
            this.wheelSteering[i] = a.wheelSteering[i];
            this.wheelRotation[i] = a.wheelRotation[i];
            this.wheelSkidInfo[i] = a.wheelSkidInfo[i];
            this.wheelSuspensionLength[i] = a.wheelSuspensionLength[i];
        }
    }

    public void getPosition(Vector3f out) {
        out.set(this.x, this.y, this.z);
    }

    public void getVelocity(Vector3f out) {
        out.set(this.vx, this.vy, this.vz);
    }

    public int compareTo(VehicleInterpolationData o) {
        return Long.compare(this.time, o.time);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.time = b.getLong();
        this.x = b.getFloat();
        this.y = b.getFloat();
        this.z = b.getFloat();
        this.qx = b.getFloat();
        this.qy = b.getFloat();
        this.qz = b.getFloat();
        this.qw = b.getFloat();
        this.vx = b.getFloat();
        this.vy = b.getFloat();
        this.vz = b.getFloat();
        this.engineSpeed = b.getFloat();
        this.throttle = b.getFloat();
        this.setNumWheels(b.getShort());

        for (int i = 0; i < this.wheelsCount; i++) {
            this.wheelSteering[i] = b.getFloat();
            this.wheelRotation[i] = b.getFloat();
            this.wheelSkidInfo[i] = b.getFloat();
            this.wheelSuspensionLength[i] = b.getFloat();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.bb.putLong(this.time);
        b.bb.putFloat(this.x);
        b.bb.putFloat(this.y);
        b.bb.putFloat(this.z);
        b.bb.putFloat(this.qx);
        b.bb.putFloat(this.qy);
        b.bb.putFloat(this.qz);
        b.bb.putFloat(this.qw);
        b.bb.putFloat(this.vx);
        b.bb.putFloat(this.vy);
        b.bb.putFloat(this.vz);
        b.bb.putFloat(this.engineSpeed);
        b.bb.putFloat(this.throttle);
        b.bb.putShort(this.wheelsCount);

        for (int i = 0; i < this.wheelsCount; i++) {
            b.bb.putFloat(this.wheelSteering[i]);
            b.bb.putFloat(this.wheelRotation[i]);
            b.bb.putFloat(this.wheelSkidInfo[i]);
            b.bb.putFloat(this.wheelSuspensionLength[i]);
        }
    }

    public void set(BaseVehicle vehicle) {
        this.time = WorldSimulation.instance.time;
        Quaternionf q = vehicle.savedRot;
        Transform t = vehicle.getWorldTransform(this.tempTransform);
        t.getRotation(q);
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.jniTransform.origin.y;
        this.qx = q.x;
        this.qy = q.y;
        this.qz = q.z;
        this.qw = q.w;
        this.vx = vehicle.jniLinearVelocity.x;
        this.vy = vehicle.jniLinearVelocity.y;
        this.vz = vehicle.jniLinearVelocity.z;
        this.engineSpeed = (float)vehicle.engineSpeed;
        this.throttle = vehicle.throttle;
        this.setNumWheels((short)vehicle.wheelInfo.length);

        for (int i = 0; i < vehicle.wheelInfo.length; i++) {
            this.wheelSteering[i] = vehicle.wheelInfo[i].steering;
            this.wheelRotation[i] = vehicle.wheelInfo[i].rotation;
            this.wheelSkidInfo[i] = vehicle.wheelInfo[i].skidInfo;
            this.wheelSuspensionLength[i] = vehicle.wheelInfo[i].suspensionLength;
        }
    }

    public void getPhysicsData(float[] physicsData) {
        int d = 0;
        physicsData[d++] = this.x;
        physicsData[d++] = this.y;
        physicsData[d++] = this.z;
        physicsData[d++] = this.qx;
        physicsData[d++] = this.qy;
        physicsData[d++] = this.qz;
        physicsData[d++] = this.qw;
        physicsData[d++] = this.vx;
        physicsData[d++] = this.vy;
        physicsData[d++] = this.vz;
        physicsData[d++] = this.wheelsCount;

        for (int i = 0; i < this.wheelsCount; i++) {
            physicsData[d++] = this.wheelSteering[i];
            physicsData[d++] = this.wheelRotation[i];
            physicsData[d++] = this.wheelSkidInfo[i];
            physicsData[d++] = this.wheelSuspensionLength[i];
        }
    }
}
