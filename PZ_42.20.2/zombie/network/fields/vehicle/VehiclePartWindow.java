// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.VehiclePart;

public class VehiclePartWindow extends VehicleField implements INetworkPacketField {
    public VehiclePartWindow(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            for (int partIndex = bb.getByte() & 255; partIndex != -1; partIndex = bb.getByte()) {
                VehiclePart part = this.getVehicle().getPartByIndex(partIndex);
                part.getWindow().load(bb.bb, 249);
            }

            this.getVehicle().doDamageOverlay();
        } catch (Exception var5) {
            DebugType.Multiplayer.printException(var5, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            for (int j = 0; j < this.getVehicle().getPartCount(); j++) {
                VehiclePart part = this.getVehicle().getPartByIndex(j);
                if (part.getFlag((short)256)) {
                    b.putByte(j);
                    part.getWindow().save(b.bb);
                }
            }

            b.putByte(255);
        } catch (Exception var4) {
            DebugType.Multiplayer.printException(var4, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
