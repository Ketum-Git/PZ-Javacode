// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.VehiclePart;

public class VehiclePartUsedDelta extends VehicleField implements INetworkPacketField {
    public VehiclePartUsedDelta(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
        try {
            for (int partIndex = bb.get(); partIndex != -1; partIndex = bb.get()) {
                float usedDelta = bb.getFloat();
                VehiclePart part = this.getVehicle().getPartByIndex(partIndex);
                InventoryItem item = part.getInventoryItem();
                if (item instanceof DrainableComboItem) {
                    item.setCurrentUses((int)(item.getMaxUses() * usedDelta));
                }
            }
        } catch (Exception var7) {
            DebugLog.Multiplayer.printException(var7, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            for (int j = 0; j < this.getVehicle().getPartCount(); j++) {
                VehiclePart part = this.getVehicle().getPartByIndex(j);
                if (part.getFlag((short)32)) {
                    InventoryItem item = part.getInventoryItem();
                    if (item instanceof DrainableComboItem) {
                        b.bb.put((byte)j);
                        b.bb.putFloat(item.getCurrentUsesFloat());
                    }
                }
            }

            b.bb.put((byte)-1);
        } catch (Exception var5) {
            DebugLog.Multiplayer.printException(var5, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
