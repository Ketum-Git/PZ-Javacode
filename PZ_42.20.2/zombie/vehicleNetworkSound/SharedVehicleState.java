// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleNetworkSound;

import zombie.audio.parameters.ParameterVehicleRoadMaterial;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.LightbarSirenMode;

public class SharedVehicleState {
    public short id;
    public String scriptName;
    public float x;
    public float y;
    public float z;
    public BaseVehicle.engineStateTypes engineState = BaseVehicle.engineStateTypes.Idle;
    public short vehicleStateFlags;
    public short engineSpeed;
    public byte currentSpeedKmHour;
    public byte gear;
    public byte engineCondition;
    public byte engineQuality;
    public String chosenAlarmSound;
    public final LightbarSirenMode lightbarSirenMode = new LightbarSirenMode();
    public byte minWheelSkid;
    public byte steering;
    public ParameterVehicleRoadMaterial.Material roadMaterial = ParameterVehicleRoadMaterial.Material.Concrete;

    public SharedVehicleState set(BaseVehicle vehicle) {
        this.id = vehicle.getId();
        this.scriptName = vehicle.getScriptName();
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
        this.engineState = vehicle.getEngineState();
        this.vehicleStateFlags = VehicleStateFlags.fromVehicle(vehicle);
        this.engineSpeed = this.engineSpeedToShort(vehicle);
        this.currentSpeedKmHour = (byte)vehicle.getCurrentSpeedKmHour();
        this.gear = (byte)vehicle.getTransmissionNumber();
        this.engineCondition = (byte)vehicle.getEngineCondition();
        this.engineQuality = (byte)vehicle.getEngineQuality();
        this.chosenAlarmSound = vehicle.getChosenAlarmSound();
        this.lightbarSirenMode.set(vehicle.getLightbarSirenMode());
        this.minWheelSkid = (byte)(vehicle.getMinWheelSkid() * 100.0F);
        this.steering = (byte)(vehicle.getMaxWheelSteering() * 100.0F);
        this.roadMaterial = vehicle.getRoadMaterial();
        return this;
    }

    public SharedVehicleState set(SharedVehicleState other) {
        this.id = other.id;
        this.scriptName = other.scriptName;
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.engineState = other.engineState;
        this.vehicleStateFlags = other.vehicleStateFlags;
        this.engineSpeed = other.engineSpeed;
        this.currentSpeedKmHour = other.currentSpeedKmHour;
        this.gear = other.gear;
        this.engineCondition = other.engineCondition;
        this.engineQuality = other.engineQuality;
        this.chosenAlarmSound = other.chosenAlarmSound;
        this.lightbarSirenMode.set(other.lightbarSirenMode.get());
        this.minWheelSkid = other.minWheelSkid;
        this.steering = other.steering;
        this.roadMaterial = other.roadMaterial;
        return this;
    }

    public void write(ByteBufferWriter b) {
        b.putShort(this.id);
        b.putUTF(this.scriptName);
        b.putFloat(this.x);
        b.putFloat(this.y);
        b.putFloat(this.z);
        b.putEnum(this.engineState);
        b.putShort(this.vehicleStateFlags);
        b.putShort(this.engineSpeed);
        b.putByte(this.currentSpeedKmHour);
        b.putByte(this.gear);
        b.putByte(this.engineCondition);
        b.putByte(this.engineQuality);
        b.putUTF(this.chosenAlarmSound);
        b.putByte(this.lightbarSirenMode.get());
        b.putByte(this.minWheelSkid);
        b.putByte(this.steering);
        b.putEnum(this.roadMaterial);
    }

    public void parse(ByteBufferReader bb) {
        this.id = bb.getShort();
        this.scriptName = bb.getUTF();
        this.x = bb.getFloat();
        this.y = bb.getFloat();
        this.z = bb.getFloat();
        this.engineState = bb.getEnum(BaseVehicle.engineStateTypes.class);
        this.vehicleStateFlags = bb.getShort();
        this.engineSpeed = bb.getShort();
        this.currentSpeedKmHour = bb.getByte();
        this.gear = bb.getByte();
        this.engineCondition = bb.getByte();
        this.engineQuality = bb.getByte();
        this.chosenAlarmSound = StringUtils.discardNullOrWhitespace(bb.getUTF());
        this.lightbarSirenMode.set(bb.getByte());
        this.minWheelSkid = bb.getByte();
        this.steering = bb.getByte();
        this.roadMaterial = bb.getEnum(ParameterVehicleRoadMaterial.Material.class);
    }

    public SharedVehicleState set(SharedVehicleState other, int changeBits) {
        this.id = other.id;
        if ((changeBits & 1) != 0) {
            this.scriptName = other.scriptName;
        }

        if ((changeBits & 2) != 0) {
            this.x = other.x;
            this.y = other.y;
            this.z = other.z;
        }

        if ((changeBits & 4) != 0) {
            this.engineState = other.engineState;
        }

        if ((changeBits & 8) != 0) {
            this.vehicleStateFlags = other.vehicleStateFlags;
        }

        if ((changeBits & 16) != 0) {
            this.engineSpeed = other.engineSpeed;
        }

        if ((changeBits & 32) != 0) {
            this.currentSpeedKmHour = other.currentSpeedKmHour;
        }

        if ((changeBits & 64) != 0) {
            this.gear = other.gear;
        }

        if ((changeBits & 128) != 0) {
            this.engineCondition = other.engineCondition;
            this.engineQuality = other.engineQuality;
        }

        if ((changeBits & 256) != 0) {
            this.chosenAlarmSound = other.chosenAlarmSound;
        }

        if ((changeBits & 512) != 0) {
            this.lightbarSirenMode.set(other.lightbarSirenMode.get());
        }

        if ((changeBits & 1024) != 0) {
            this.minWheelSkid = other.minWheelSkid;
        }

        if ((changeBits & 2048) != 0) {
            this.steering = other.steering;
        }

        if ((changeBits & 4096) != 0) {
            this.roadMaterial = other.roadMaterial;
        }

        return this;
    }

    public void write(ByteBufferWriter b, int changeBits) {
        b.putShort(this.id);
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Short, b.bb);
        header.addFlags(changeBits);
        if ((changeBits & 1) != 0) {
            b.putUTF(this.scriptName);
        }

        if ((changeBits & 2) != 0) {
            b.putFloat(this.x);
            b.putFloat(this.y);
            b.putFloat(this.z);
        }

        if ((changeBits & 4) != 0) {
            b.putEnum(this.engineState);
        }

        if ((changeBits & 8) != 0) {
            b.putShort(this.vehicleStateFlags);
        }

        if ((changeBits & 16) != 0) {
            b.putShort(this.engineSpeed);
        }

        if ((changeBits & 32) != 0) {
            b.putByte(this.currentSpeedKmHour);
        }

        if ((changeBits & 64) != 0) {
            b.putByte(this.gear);
        }

        if ((changeBits & 128) != 0) {
            b.putByte(this.engineCondition);
            b.putByte(this.engineQuality);
        }

        if ((changeBits & 256) != 0) {
            b.putUTF(this.chosenAlarmSound);
        }

        if ((changeBits & 512) != 0) {
            b.putByte(this.lightbarSirenMode.get());
        }

        if ((changeBits & 1024) != 0) {
            b.putByte(this.minWheelSkid);
        }

        if ((changeBits & 2048) != 0) {
            b.putByte(this.steering);
        }

        if ((changeBits & 4096) != 0) {
            b.putEnum(this.roadMaterial);
        }

        header.write();
        header.release();
    }

    public int parseUpdate(ByteBufferReader bb) {
        this.id = bb.getShort();
        int changeBits = 0;
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Short, bb.bb);
        if (header.hasFlags(1)) {
            this.scriptName = bb.getUTF();
            changeBits |= 1;
        }

        if (header.hasFlags(2)) {
            this.x = bb.getFloat();
            this.y = bb.getFloat();
            this.z = bb.getFloat();
            changeBits |= 2;
        }

        if (header.hasFlags(4)) {
            this.engineState = bb.getEnum(BaseVehicle.engineStateTypes.class);
            changeBits |= 4;
        }

        if (header.hasFlags(8)) {
            this.vehicleStateFlags = bb.getShort();
            changeBits |= 8;
        }

        if (header.hasFlags(16)) {
            this.engineSpeed = bb.getShort();
            changeBits |= 16;
        }

        if (header.hasFlags(32)) {
            this.currentSpeedKmHour = bb.getByte();
            changeBits |= 32;
        }

        if (header.hasFlags(64)) {
            this.gear = bb.getByte();
            changeBits |= 64;
        }

        if (header.hasFlags(128)) {
            this.engineCondition = bb.getByte();
            this.engineQuality = bb.getByte();
            changeBits |= 128;
        }

        if (header.hasFlags(256)) {
            this.chosenAlarmSound = StringUtils.discardNullOrWhitespace(bb.getUTF());
            changeBits |= 256;
        }

        if (header.hasFlags(512)) {
            this.lightbarSirenMode.set(bb.getByte());
            changeBits |= 512;
        }

        if (header.hasFlags(1024)) {
            this.minWheelSkid = bb.getByte();
            changeBits |= 1024;
        }

        if (header.hasFlags(2048)) {
            this.steering = bb.getByte();
            changeBits |= 2048;
        }

        if (header.hasFlags(4096)) {
            this.roadMaterial = bb.getEnum(ParameterVehicleRoadMaterial.Material.class);
            changeBits |= 4096;
        }

        header.release();
        return changeBits;
    }

    private short engineSpeedToShort(BaseVehicle vehicle) {
        double engineSpeed = Math.floor(vehicle.getEngineSpeed() / 100.0) * 100.0;
        return (short)engineSpeed;
    }
}
