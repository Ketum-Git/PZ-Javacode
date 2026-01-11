// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class AddItemInInventoryPacket implements INetworkPacket {
    @JSONField
    PlayerID player = new PlayerID();
    @JSONField
    ArrayList<InventoryItem> items = new ArrayList<>();

    @Override
    public void setData(Object... values) {
        this.player.set((IsoPlayer)values[0]);
        this.items.clear();
        this.items.addAll((Collection<? extends InventoryItem>)values[1]);
    }

    public void set(IsoPlayer player, ArrayList<InventoryItem> items) {
        this.items.clear();
        this.items.addAll(items);
        this.player.set(player);
    }

    @Override
    public void processClient(UdpConnection connection) {
        for (InventoryItem item : this.items) {
            this.player.getPlayer().getInventory().addItem(item);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.items.clear();
        this.player.parse(b, connection);
        short size = b.getShort();

        for (int i = 0; i < size; i++) {
            short ID = b.getShort();
            b.get();

            try {
                InventoryItem item = InventoryItemFactory.CreateItem(ID);
                if (item != null) {
                    item.load(b, 240);
                }

                this.items.add(item);
            } catch (BufferUnderflowException | IOException var7) {
                DebugLog.Multiplayer.printException(var7, "Item load error", LogSeverity.Error);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        b.putShort((short)this.items.size());

        for (int i = 0; i < this.items.size(); i++) {
            try {
                this.items.get(i).save(b.bb, true);
            } catch (IOException var4) {
                var4.printStackTrace();
            }
        }
    }
}
