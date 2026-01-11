// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.VehiclePart;

public class VehiclePartWindow extends VehicleField implements INetworkPacketField {
    public VehiclePartWindow(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
        try {
            for (int partIndex = bb.get(); partIndex != -1; partIndex = bb.get()) {
                VehiclePart part = this.getVehicle().getPartByIndex(partIndex);
                part.getWindow().load(bb, 240);
            }

            this.getVehicle().doDamageOverlay();
        } catch (Exception var5) {
            DebugLog.Multiplayer.printException(var5, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            for (int j = 0; j < this.getVehicle().getPartCount(); j++) {
                VehiclePart part = this.getVehicle().getPartByIndex(j);
                if (part.getFlag((short)256)) {
                    b.bb.put((byte)j);
                    part.getWindow().save(b.bb);
                }
            }

            b.bb.put((byte)-1);
        } catch (Exception var4) {
            DebugLog.Multiplayer.printException(var4, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
