// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.network.IConnection;
import zombie.network.JSONField;

public class PlayerItem extends IDShort implements INetworkPacketField {
    @JSONField
    protected InventoryItem item;

    public void set(InventoryItem item) {
        this.item = item;
        if (item == null) {
            this.setID((short)-1);
        } else {
            this.setID(item.getRegistry_id());
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        boolean hasItem = b.getBoolean();
        if (hasItem) {
            this.setID(b.getShort());
            b.getByte();

            try {
                this.item = InventoryItemFactory.CreateItem(this.getID());
                if (this.item != null) {
                    this.item.load(b.bb, 249);
                }
            } catch (BufferUnderflowException | IOException var5) {
                DebugType.Multiplayer.printException(var5, "Item load error", LogSeverity.Error);
                this.item = null;
            }
        } else {
            this.item = null;
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        if (b.putBoolean(this.item != null)) {
            try {
                this.item.save(b.bb, false);
            } catch (IOException var3) {
                DebugType.Multiplayer.printException(var3, "Item write error", LogSeverity.Error);
            }
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return super.isConsistent(connection) && this.item != null;
    }

    public InventoryItem getItem() {
        return this.item;
    }
}
