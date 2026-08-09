// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.io.IOException;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

public class VehiclePartUsedDelta extends VehicleField implements INetworkPacketField {
    public VehiclePartUsedDelta(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            int numParts = bb.getByte() & 255;
            int totalPartBytes = bb.getShort() & '\uffff';
            int posOfPartsStart = bb.position();
            int expectedPosOfPartsEnd = posOfPartsStart + totalPartBytes;

            for (int i = 0; i < numParts; i++) {
                int partIndex = bb.getByte() & 255;
                float usedDelta = bb.getFloat();
                VehiclePart part = this.getVehicle().getPartByIndex(partIndex);
                if (part == null) {
                    DebugType.Multiplayer.warn("Part %d not found!", partIndex);
                } else {
                    InventoryItem item = part.getInventoryItem();
                    if (item instanceof DrainableComboItem) {
                        item.setCurrentUses((int)(item.getMaxUses() * usedDelta));
                    }
                }
            }

            int posOfPartsEnd = bb.position();
            if (posOfPartsEnd != expectedPosOfPartsEnd) {
                throw new IOException(
                    String.format(
                        "Data-length mismatch. Expected to read bytes: %d. Instead, read bytes: %d",
                        expectedPosOfPartsEnd - posOfPartsStart,
                        posOfPartsEnd - posOfPartsStart
                    )
                );
            }

            int endMarker = bb.getByte() & 255;
            if (endMarker != 255) {
                throw new IOException(String.format("Unexpected endMarker byte: %d. Expected: %d", endMarker, 255));
            }
        } catch (Exception var12) {
            DebugType.Multiplayer.printException(var12, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            int posOfNumParts = b.position();
            b.putByte(0);
            int posOfTotalPartBytes = b.position();
            b.putShort(0);
            int numParts = 0;
            int posOfPartsStart = b.position();
            BaseVehicle vehicle = this.getVehicle();

            for (int j = 0; j < vehicle.getPartCount(); j++) {
                VehiclePart part = vehicle.getPartByIndex(j);
                if (part.getFlag((short)32)) {
                    InventoryItem item = part.getInventoryItem();
                    if (item instanceof DrainableComboItem) {
                        b.putByte(j);
                        b.putFloat(item.getCurrentUsesFloat());
                        numParts++;
                    }
                }
            }

            int posOfPartsEnd = b.position();
            int totalPartBytes = posOfPartsEnd - posOfPartsStart;
            b.position(posOfNumParts);
            b.putByte(numParts);
            b.position(posOfTotalPartBytes);
            b.putShort(totalPartBytes);
            b.position(posOfPartsEnd);
            b.putByte(255);
        } catch (Exception var10) {
            DebugType.Multiplayer.printException(var10, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
