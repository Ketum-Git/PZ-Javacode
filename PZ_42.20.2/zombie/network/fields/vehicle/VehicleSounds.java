// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;

public class VehicleSounds extends VehicleField implements INetworkPacketField {
    public VehicleSounds(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            int flags = bb.getByte() & 255;
            boolean soundAlarmOn = (flags & 1) != 0;
            boolean alarmActive = (flags & 2) != 0;
            boolean soundHornOn = (flags & 4) != 0;
            boolean soundBackMoveOn = (flags & 8) != 0;
            double alarmStartTime = alarmActive ? bb.getDouble() : 0.0;
            byte lightbarLightsMode = bb.getByte();
            byte lightbarSirenMode = bb.getByte();
            this.getVehicle().getVehicleAlarmObject().setStartTime(alarmStartTime);
            if (soundAlarmOn != this.getVehicle().isAlarmSoundOn()) {
                if (soundAlarmOn) {
                    this.getVehicle().onAlarmStart();
                } else {
                    this.getVehicle().onAlarmStop();
                }
            }

            if (soundHornOn != this.getVehicle().soundHornOn) {
                if (soundHornOn) {
                    this.getVehicle().onHornStart();
                } else {
                    this.getVehicle().onHornStop();
                }
            }

            if (soundBackMoveOn != this.getVehicle().soundBackMoveOn) {
                if (soundBackMoveOn) {
                    this.getVehicle().onBackMoveSignalStart();
                } else {
                    this.getVehicle().onBackMoveSignalStop();
                }
            }

            if (this.getVehicle().lightbarLightsMode.get() != lightbarLightsMode) {
                this.getVehicle().setLightbarLightsMode(lightbarLightsMode);
            }

            if (this.getVehicle().lightbarSirenMode.get() != lightbarSirenMode) {
                this.getVehicle().setLightbarSirenMode(lightbarSirenMode);
            }
        } catch (Exception var12) {
            DebugType.Multiplayer.printException(var12, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            int flags = 0;
            if (this.getVehicle().isAlarmSoundOn()) {
                flags |= 1;
            }

            if (this.getVehicle().isAlarmActive()) {
                flags |= 2;
            }

            if (this.getVehicle().soundHornOn) {
                flags |= 4;
            }

            if (this.getVehicle().soundBackMoveOn) {
                flags |= 8;
            }

            b.putByte(flags);
            if ((flags & 2) != 0) {
                b.putDouble(this.getVehicle().getVehicleAlarmObject().getStartTime());
            }

            b.putByte(this.getVehicle().lightbarLightsMode.get());
            b.putByte(this.getVehicle().lightbarSirenMode.get());
        } catch (Exception var3) {
            DebugType.Multiplayer.printException(var3, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
