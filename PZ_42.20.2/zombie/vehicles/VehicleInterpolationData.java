// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.io.IOException;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.physics.Transform;
import zombie.core.physics.WorldSimulation;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.IConnection;
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
    protected TransmissionNumber transmissionNumber;
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
    public void parse(ByteBufferReader b, IConnection connection) {
        try {
            int totalByteCount = b.getShort();
            int positionOfDataStart = b.position();
            int expectedDataEndPos = positionOfDataStart + totalByteCount;
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

            int dataEndPos = b.position();
            if (dataEndPos != expectedDataEndPos) {
                throw new IOException(
                    String.format(
                        "Data mismatch. Expected to read bytes: %d. Instead read bytes: %s",
                        expectedDataEndPos - positionOfDataStart,
                        dataEndPos - positionOfDataStart
                    )
                );
            }

            int endMarker = b.getByte() & 255;
            if (endMarker != 255) {
                throw new IOException(String.format("Unexpected byte: %d. Expected: %d", endMarker, 255));
            }
        } catch (Exception var8) {
            DebugType.Multiplayer.printException(var8, LogSeverity.Error, "5s: Failed to read.", this.getClass().getSimpleName());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        int positionOfTotalByteCount = b.position();
        b.putShort(0);
        int positionOfDataStart = b.position();
        b.putLong(this.time);
        b.putFloat(this.x);
        b.putFloat(this.y);
        b.putFloat(this.z);
        b.putFloat(this.qx);
        b.putFloat(this.qy);
        b.putFloat(this.qz);
        b.putFloat(this.qw);
        b.putFloat(this.vx);
        b.putFloat(this.vy);
        b.putFloat(this.vz);
        b.putFloat(this.engineSpeed);
        b.putFloat(this.throttle);
        b.putShort(this.wheelsCount);

        for (int i = 0; i < this.wheelsCount; i++) {
            b.putFloat(this.wheelSteering[i]);
            b.putFloat(this.wheelRotation[i]);
            b.putFloat(this.wheelSkidInfo[i]);
            b.putFloat(this.wheelSuspensionLength[i]);
        }

        int totalByteCount = b.position() - positionOfDataStart;
        b.putByte(255);
        int positionOfEnd = b.position();
        b.position(positionOfTotalByteCount);
        b.putShort(totalByteCount);
        b.position(positionOfEnd);
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
        this.engineSpeed = (float)vehicle.getEngineSpeed();
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
