// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.network.JSONField;

public class PlayerItem extends IDShort implements INetworkPacketField {
    @JSONField
    protected InventoryItem item;

    public void set(InventoryItem _item) {
        this.item = _item;
        if (_item == null) {
            this.setID((short)-1);
        } else {
            this.setID(_item.getRegistry_id());
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        boolean hasItem = b.get() == 1;
        if (hasItem) {
            this.setID(b.getShort());
            b.get();

            try {
                this.item = InventoryItemFactory.CreateItem(this.getID());
                if (this.item != null) {
                    this.item.load(b, 240);
                }
            } catch (BufferUnderflowException | IOException var5) {
                DebugLog.Multiplayer.printException(var5, "Item load error", LogSeverity.Error);
                this.item = null;
            }
        } else {
            this.item = null;
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        if (this.item == null) {
            b.putByte((byte)0);
        } else {
            b.putByte((byte)1);

            try {
                this.item.save(b.bb, false);
            } catch (IOException var3) {
                DebugLog.Multiplayer.printException(var3, "Item write error", LogSeverity.Error);
            }
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.item != null;
    }

    public InventoryItem getItem() {
        return this.item;
    }
}
