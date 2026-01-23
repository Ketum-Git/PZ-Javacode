// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.fields.INetworkPacketField;

public class VehicleSounds extends VehicleField implements INetworkPacketField {
    public VehicleSounds(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
        try {
            boolean soundAlarmOn = bb.get() == 1;
            boolean soundHornOn = bb.get() == 1;
            boolean soundBackMoveOn = bb.get() == 1;
            byte lightbarLightsMode = bb.get();
            byte lightbarSirenMode = bb.get();
            if (soundAlarmOn != this.getVehicle().soundAlarmOn) {
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
        } catch (Exception var8) {
            DebugLog.Multiplayer.printException(var8, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            b.bb.put((byte)(this.getVehicle().soundAlarmOn ? 1 : 0));
            b.bb.put((byte)(this.getVehicle().soundHornOn ? 1 : 0));
            b.bb.put((byte)(this.getVehicle().soundBackMoveOn ? 1 : 0));
            b.bb.put((byte)this.getVehicle().lightbarLightsMode.get());
            b.bb.put((byte)this.getVehicle().lightbarSirenMode.get());
        } catch (Exception var3) {
            DebugLog.Multiplayer.printException(var3, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
