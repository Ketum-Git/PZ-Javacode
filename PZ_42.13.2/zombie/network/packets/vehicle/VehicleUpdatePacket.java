// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.vehicle;

import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.vehicle.VehicleAuthorization;
import zombie.network.fields.vehicle.VehicleEngine;
import zombie.network.fields.vehicle.VehicleLights;
import zombie.network.fields.vehicle.VehiclePartCondition;
import zombie.network.fields.vehicle.VehiclePartDoor;
import zombie.network.fields.vehicle.VehiclePartItem;
import zombie.network.fields.vehicle.VehiclePartModData;
import zombie.network.fields.vehicle.VehiclePartModels;
import zombie.network.fields.vehicle.VehiclePartUsedDelta;
import zombie.network.fields.vehicle.VehiclePartWindow;
import zombie.network.fields.vehicle.VehiclePassengers;
import zombie.network.fields.vehicle.VehicleProperties;
import zombie.network.fields.vehicle.VehicleSounds;
import zombie.network.packets.INetworkPacket;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleInterpolationData;
import zombie.vehicles.VehiclePart;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class VehicleUpdatePacket extends VehiclePacket implements INetworkPacket {
    @JSONField
    protected final VehicleInterpolationData vehiclePositionOrientation = new VehicleInterpolationData();
    @JSONField
    protected final VehicleEngine vehicleEngine = new VehicleEngine(this.vehicleID);
    @JSONField
    protected final VehicleLights vehicleLights = new VehicleLights(this.vehicleID);
    @JSONField
    protected final VehiclePartModData vehiclePartModData = new VehiclePartModData(this.vehicleID);
    @JSONField
    protected final VehiclePartUsedDelta vehiclePartUsedDelta = new VehiclePartUsedDelta(this.vehicleID);
    @JSONField
    protected final VehiclePartModels vehiclePartModels = new VehiclePartModels(this.vehicleID);
    @JSONField
    protected final VehiclePartItem vehiclePartItem = new VehiclePartItem(this.vehicleID);
    @JSONField
    protected final VehiclePartWindow vehiclePartWindow = new VehiclePartWindow(this.vehicleID);
    @JSONField
    protected final VehiclePartDoor vehiclePartDoor = new VehiclePartDoor(this.vehicleID);
    @JSONField
    protected final VehicleSounds vehicleSounds = new VehicleSounds(this.vehicleID);
    @JSONField
    protected final VehiclePartCondition vehiclePartCondition = new VehiclePartCondition(this.vehicleID);
    @JSONField
    protected final VehicleProperties vehicleProperties = new VehicleProperties(this.vehicleID);
    @JSONField
    protected final VehicleAuthorization vehicleAuthorization = new VehicleAuthorization(this.vehicleID);
    @JSONField
    protected final VehiclePassengers vehiclePassengers = new VehiclePassengers(this.vehicleID);
    @JSONField
    protected short flags;

    @Override
    public void setData(Object... values) {
        super.set((BaseVehicle)values[0]);
        this.flags = (Short)values[1];
        this.vehicleAuthorization.set((BaseVehicle)values[0]);
        this.vehiclePositionOrientation.set((BaseVehicle)values[0]);
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
        super.parse(bb, connection);
        this.flags = bb.getShort();

        try {
            if (this.isConsistent(connection)) {
                if (this.doRemove(connection)) {
                    DebugLog.Multiplayer.debugln("Vehicle %d removed", this.vehicleID.getID());
                } else if (this.doRequest(connection)) {
                    DebugLog.Multiplayer.debugln("Vehicle %d requested", this.vehicleID.getID());
                } else {
                    if ((this.flags & 2) != 0) {
                        this.vehiclePositionOrientation.parse(bb, connection);
                        if (!this.vehicleID.getVehicle().isKeyboardControlled() && this.vehicleID.getVehicle().getJoypad() == -1) {
                            this.vehicleID
                                .getVehicle()
                                .interpolation
                                .interpolationDataAdd(this.vehicleID.getVehicle(), this.vehiclePositionOrientation, GameTime.getServerTimeMills());
                        }
                    }

                    if ((this.flags & 4) != 0) {
                        this.vehicleEngine.parse(bb, connection);
                    }

                    if ((this.flags & 4096) != 0) {
                        this.vehicleProperties.parse(bb, connection);
                    }

                    if ((this.flags & 8) != 0) {
                        this.vehicleLights.parse(bb, connection);
                    }

                    if ((this.flags & 1024) != 0) {
                        this.vehicleSounds.parse(bb, connection);
                    }

                    if ((this.flags & 2048) != 0) {
                        this.vehiclePartCondition.parse(bb, connection);
                    }

                    if ((this.flags & 16) != 0) {
                        this.vehiclePartModData.parse(bb, connection);
                    }

                    if ((this.flags & 32) != 0) {
                        this.vehiclePartUsedDelta.parse(bb, connection);
                    }

                    if ((this.flags & 128) != 0) {
                        this.vehiclePartItem.parse(bb, connection);
                    }

                    if ((this.flags & 512) != 0) {
                        this.vehiclePartDoor.parse(bb, connection);
                    }

                    if ((this.flags & 256) != 0) {
                        this.vehiclePartWindow.parse(bb, connection);
                    }

                    if ((this.flags & 64) != 0) {
                        this.vehiclePartModels.parse(bb, connection);
                    }

                    if ((this.flags & 8192) != 0) {
                        this.vehicleAuthorization.parse(bb, connection);
                        this.vehicleID
                            .getVehicle()
                            .netPlayerFromServerUpdate(this.vehicleAuthorization.getAuthorization(), this.vehicleAuthorization.getAuthorizationPlayer());
                    }

                    if ((this.flags & 16384) != 0) {
                        this.vehiclePassengers.parse(bb, connection);
                    }

                    boolean updateStats = false;

                    for (int i = 0; i < this.vehicleID.getVehicle().getPartCount(); i++) {
                        VehiclePart part = this.vehicleID.getVehicle().getPartByIndex(i);
                        if (part != null && part.getFlag((short)3056)) {
                            if (part.getFlag((short)2048) && part.getFlag((short)128)) {
                                part.doInventoryItemStats(part.getInventoryItem(), part.getMechanicSkillInstaller());
                                updateStats = true;
                            }

                            part.clearFlags();
                        }
                    }

                    if (updateStats) {
                        this.vehicleID.getVehicle().updatePartStats();
                        this.vehicleID.getVehicle().updateBulletStats();
                    }
                }
            }
        } catch (Exception var6) {
            DebugLog.Multiplayer.printException(var6, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putShort(this.flags);
        if ((this.flags & 2) != 0) {
            this.vehiclePositionOrientation.write(b);
        }

        if ((this.flags & 4) != 0) {
            this.vehicleEngine.write(b);
        }

        if ((this.flags & 4096) != 0) {
            this.vehicleProperties.write(b);
        }

        if ((this.flags & 8) != 0) {
            this.vehicleLights.write(b);
        }

        if ((this.flags & 1024) != 0) {
            this.vehicleSounds.write(b);
        }

        if ((this.flags & 2048) != 0) {
            this.vehiclePartCondition.write(b);
        }

        if ((this.flags & 16) != 0) {
            this.vehiclePartModData.write(b);
        }

        if ((this.flags & 32) != 0) {
            this.vehiclePartUsedDelta.write(b);
        }

        if ((this.flags & 128) != 0) {
            this.vehiclePartItem.write(b);
        }

        if ((this.flags & 512) != 0) {
            this.vehiclePartDoor.write(b);
        }

        if ((this.flags & 256) != 0) {
            this.vehiclePartWindow.write(b);
        }

        if ((this.flags & 64) != 0) {
            this.vehiclePartModels.write(b);
        }

        if ((this.flags & 8192) != 0) {
            this.vehicleAuthorization.write(b);
        }

        if ((this.flags & 16384) != 0) {
            this.vehiclePassengers.write(b);
        }
    }
}
