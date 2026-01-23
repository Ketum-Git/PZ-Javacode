// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.BaseVehicle;

public class VehicleEngine extends VehicleField implements INetworkPacketField {
    public VehicleEngine(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
        try {
            byte engineState = bb.get();
            switch (BaseVehicle.engineStateTypes.Values[engineState]) {
                case Idle:
                    this.getVehicle().engineDoIdle();
                    break;
                case RetryingStarting:
                    this.getVehicle().engineDoRetryingStarting();
                    break;
                case StartingSuccess:
                    this.getVehicle().engineDoStartingSuccess();
                    break;
                case StartingFailed:
                    this.getVehicle().engineDoStartingFailed();
                    break;
                case StartingFailedNoPower:
                    this.getVehicle().engineDoStartingFailedNoPower();
                    break;
                case Running:
                    this.getVehicle().engineDoRunning();
                    break;
                case Stalling:
                    this.getVehicle().engineDoStalling();
                    break;
                case ShutingDown:
                    this.getVehicle().engineDoShuttingDown();
            }

            this.getVehicle().setEngineFeature(bb.getInt(), bb.getInt(), bb.getInt());
        } catch (Exception var4) {
            DebugLog.Multiplayer.printException(var4, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            b.bb.put((byte)this.getVehicle().engineState.ordinal());
            b.bb.putInt(this.getVehicle().getEngineQuality());
            b.bb.putInt(this.getVehicle().getEngineLoudness());
            b.bb.putInt(this.getVehicle().getEnginePower());
        } catch (Exception var3) {
            DebugLog.Multiplayer.printException(var3, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
