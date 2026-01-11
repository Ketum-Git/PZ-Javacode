// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.TradingManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class TradingUIAddItemPacket implements INetworkPacket {
    PlayerID playerA = new PlayerID();
    PlayerID playerB = new PlayerID();
    protected int itemId;
    protected InventoryItem item;

    public void set(IsoPlayer you, IsoPlayer other, InventoryItem item) {
        this.playerA.set(you);
        this.playerB.set(other);
        this.itemId = item.getID();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerA.write(b);
        this.playerB.write(b);
        if (GameClient.client) {
            b.putInt(this.itemId);
        } else {
            try {
                this.item.saveWithSize(b.bb, false);
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerA.parse(b, connection);
        this.playerB.parse(b, connection);
        if (GameServer.server) {
            this.itemId = b.getInt();
            if (this.playerA.getPlayer() != null) {
                this.item = this.playerA.getPlayer().getInventory().getItemWithID(this.itemId);
            }
        } else {
            try {
                this.item = InventoryItem.loadItem(b, 240);
            } catch (Exception var4) {
                var4.printStackTrace();
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.playerA.getPlayer() != null) {
            LuaEventManager.triggerEvent("TradingUIAddItem", this.playerA.getPlayer(), this.item);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        TradingManager.getInstance().addItem(this.playerA.getPlayer(), this.item);
        connection = GameServer.getConnectionFromPlayer(this.playerB.getPlayer());
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.TradingUIAddItem.doPacket(b);
        this.write(b);
        PacketTypes.PacketType.TradingUIAddItem.send(connection);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.playerA.getPlayer() == null || this.playerB.getPlayer() == null ? false : !GameServer.server || this.item != null;
    }
}
