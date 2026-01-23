// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.fields.INetworkPacketField;

public class VehicleLights extends VehicleField implements INetworkPacketField {
    public VehicleLights(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
        try {
            this.getVehicle().setHeadlightsOn(bb.get() == 1);
            this.getVehicle().setStoplightsOn(bb.get() == 1);

            for (int i = 0; i < this.getVehicle().getLightCount(); i++) {
                this.getVehicle().getLightByIndex(i).getLight().setActive(bb.get() == 1);
            }
        } catch (Exception var4) {
            DebugLog.Multiplayer.printException(var4, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            b.bb.put((byte)(this.getVehicle().getHeadlightsOn() ? 1 : 0));
            b.bb.put((byte)(this.getVehicle().getStoplightsOn() ? 1 : 0));

            for (int j = 0; j < this.getVehicle().getLightCount(); j++) {
                b.bb.put((byte)(this.getVehicle().getLightByIndex(j).getLight().getActive() ? 1 : 0));
            }
        } catch (Exception var3) {
            DebugLog.Multiplayer.printException(var3, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
