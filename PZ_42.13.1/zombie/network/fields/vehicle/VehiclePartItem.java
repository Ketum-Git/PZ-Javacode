// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.vehicle;

import java.nio.ByteBuffer;
import zombie.Lua.LuaEventManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.VehiclePart;

public class VehiclePartItem extends VehicleField implements INetworkPacketField {
    public VehiclePartItem(VehicleID vehicleID) {
        super(vehicleID);
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
        try {
            for (int partIndex = bb.get(); partIndex != -1; partIndex = bb.get()) {
                VehiclePart part = this.getVehicle().getPartByIndex(partIndex);
                part.setFlag((short)128);
                boolean hasItem = bb.get() != 0;
                if (hasItem) {
                    InventoryItem item = InventoryItem.loadItem(bb, 240);
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
            DebugLog.Multiplayer.printException(var7, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            for (int j = 0; j < this.getVehicle().getPartCount(); j++) {
                VehiclePart part = this.getVehicle().getPartByIndex(j);
                if (part.getFlag((short)128)) {
                    b.bb.put((byte)j);
                    InventoryItem item = part.getInventoryItem();
                    if (item == null) {
                        b.bb.put((byte)0);
                    } else {
                        b.bb.put((byte)1);
                        part.<InventoryItem>getInventoryItem().saveWithSize(b.bb, false);
                    }
                }
            }

            b.bb.put((byte)-1);
        } catch (Exception var5) {
            DebugLog.Multiplayer.printException(var5, this.getClass().getSimpleName() + ": failed", LogSeverity.Error);
        }
    }
}
