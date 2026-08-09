// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleNetworkSound.server;

import zombie.audio.parameters.ParameterVehicleRoadMaterial;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;
import zombie.vehicleNetworkSound.SharedVehicleState;
import zombie.vehicleNetworkSound.VehicleStateFlags;
import zombie.vehicles.BaseVehicle;

final class VehicleState extends SharedVehicleState {
    void sendNew(UdpConnection udpConnection) {
        INetworkPacket.send(udpConnection, PacketTypes.PacketType.VehicleSoundAddVehicle, this);
    }

    void update(BaseVehicle vehicle, UdpConnection udpConnection) {
        int changeBits = 0;
        if (!vehicle.getScriptName().equals(this.scriptName)) {
            this.scriptName = vehicle.getScriptName();
            changeBits |= 1;
        }

        if (this.x != vehicle.getX() || this.y != vehicle.getY() || this.z != vehicle.getZ()) {
            this.x = vehicle.getX();
            this.y = vehicle.getY();
            this.z = vehicle.getZ();
            changeBits |= 2;
        }

        if (this.engineState != vehicle.getEngineState()) {
            this.engineState = vehicle.getEngineState();
            changeBits |= 4;
        }

        short vehicleStateFlags1 = VehicleStateFlags.fromVehicle(vehicle);
        if (this.vehicleStateFlags != vehicleStateFlags1) {
            this.vehicleStateFlags = vehicleStateFlags1;
            changeBits |= 8;
        }

        if (this.engineSpeed != this.engineSpeedToShort(vehicle)) {
            this.engineSpeed = this.engineSpeedToShort(vehicle);
            changeBits |= 16;
        }

        if (this.currentSpeedKmHour != (byte)vehicle.getCurrentSpeedKmHour()) {
            this.currentSpeedKmHour = (byte)vehicle.getCurrentSpeedKmHour();
            changeBits |= 32;
        }

        if (this.gear != (byte)vehicle.getTransmissionNumber()) {
            this.gear = (byte)vehicle.getTransmissionNumber();
            changeBits |= 64;
        }

        if (this.engineCondition != (byte)vehicle.getEngineCondition() || this.engineQuality != (byte)vehicle.getEngineQuality()) {
            this.engineCondition = (byte)vehicle.getEngineCondition();
            this.engineQuality = (byte)vehicle.getEngineQuality();
            changeBits |= 128;
        }

        if (!StringUtils.equals(this.chosenAlarmSound, vehicle.getChosenAlarmSound())) {
            this.chosenAlarmSound = vehicle.getChosenAlarmSound();
            changeBits |= 256;
        }

        if (this.lightbarSirenMode.get() != vehicle.getLightbarSirenMode()) {
            this.lightbarSirenMode.set(vehicle.getLightbarSirenMode());
            changeBits |= 512;
        }

        if (this.minWheelSkid != (byte)(vehicle.getMinWheelSkid() * 100.0F)) {
            this.minWheelSkid = (byte)(vehicle.getMinWheelSkid() * 100.0F);
            changeBits |= 1024;
        }

        if (this.steering != (byte)(vehicle.getMaxWheelSteering() * 100.0F)) {
            this.steering = (byte)(vehicle.getMaxWheelSteering() * 100.0F);
            changeBits |= 2048;
        }

        ParameterVehicleRoadMaterial.Material roadMaterial1 = vehicle.getRoadMaterial();
        if (this.roadMaterial != roadMaterial1) {
            this.roadMaterial = roadMaterial1;
            changeBits |= 4096;
        }

        if (changeBits != 0) {
            INetworkPacket.send(udpConnection, PacketTypes.PacketType.VehicleSoundUpdateVehicle, this, changeBits);
        }
    }

    void remove(UdpConnection udpConnection) {
        INetworkPacket.send(udpConnection, PacketTypes.PacketType.VehicleSoundRemoveVehicle, this.id);
    }

    @Override
    short engineSpeedToShort(BaseVehicle vehicle) {
        double engineSpeed = Math.floor(vehicle.getEngineSpeed() / 100.0) * 100.0;
        return (short)engineSpeed;
    }
}
