// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import se.krka.kahlua.vm.KahluaTable;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.ContainerID;

@PacketSetting(ordering = 1, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class SyncItemModDataPacket implements INetworkPacket {
    @JSONField
    ContainerID containerId = new ContainerID();
    @JSONField
    int itemId;
    @JSONField
    KahluaTable table;

    @Override
    public void setData(Object... values) {
        InventoryItem item = (InventoryItem)values[0];
        this.containerId.set(item.getContainer());
        this.itemId = item.getID();
        this.table = item.getModData();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.containerId.write(b);
        b.putInt(this.itemId);

        try {
            this.table.save(b.bb);
        } catch (IOException var3) {
            DebugType.General.printException(var3, LogSeverity.Error);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.containerId.parse(b, connection);
        this.itemId = b.getInt();
        InventoryItem item = this.containerId.getContainer().getItemWithID(this.itemId);
        if (item != null) {
            this.table = item.getModData();

            try {
                this.table.load(b.bb, 249);
            } catch (IOException var5) {
                DebugType.General.printException(var5, LogSeverity.Error);
            }
        }
    }
}
