// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import zombie.Lua.LuaEventManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.VehiclePart;

public class VehiclePartItem extends VehicleField implements INetworkPacketField {
    public VehiclePartItem(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBufferReader bb, IConnection connection) {
        try {
            for (int partIndex = bb.getByte() & 255; partIndex != -1; partIndex = bb.getByte()) {
                VehiclePart part = this.getVehicle().getPartByIndex(partIndex);
                part.setFlag((short)128);
                boolean hasItem = bb.getBoolean();
                if (hasItem) {
                    InventoryItem item = InventoryItem.loadItem(bb.bb, 249);
                    if (item != null) {
                        part.setInventoryItem(item);
                    }
                } else {
                    part.setInventoryItem(null);
                }

                int wheelIndex = part.getWheelIndex();
                if (wheelIndex != -1) {
                    this.getVehicle().setTireRemoved(wheelIndex, !hasItem);
                }

                if (part.isContainer()) {
                    LuaEventManager.triggerEvent("OnContainerUpdate");
                }
            }
        } catch (Exception var7) {
            DebugType.Multiplayer.printException(var7, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            for (int j = 0; j < this.getVehicle().getPartCount(); j++) {
                VehiclePart part = this.getVehicle().getPartByIndex(j);
                if (part.getFlag((short)128)) {
                    b.putByte(j);
                    InventoryItem item = part.getInventoryItem();
                    if (b.putBoolean(item != null)) {
                        part.<InventoryItem>getInventoryItem().saveWithSize(b.bb, false);
                    }
                }
            }

            b.putByte(255);
        } catch (Exception var5) {
            DebugType.Multiplayer.printException(var5, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
