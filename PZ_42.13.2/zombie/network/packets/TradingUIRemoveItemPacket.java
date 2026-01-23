// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.TradingManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class TradingUIRemoveItemPacket implements INetworkPacket {
    PlayerID playerA = new PlayerID();
    PlayerID playerB = new PlayerID();
    protected int itemId;

    public void set(IsoPlayer you, IsoPlayer other, InventoryItem item) {
        this.playerA.set(you);
        this.playerB.set(other);
        this.itemId = item.getID();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerA.write(b);
        this.playerB.write(b);
        b.putInt(this.itemId);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerA.parse(b, connection);
        this.playerB.parse(b, connection);
        this.itemId = b.getInt();
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.playerA.getPlayer() != null) {
            LuaEventManager.triggerEvent("TradingUIRemoveItem", this.playerA.getPlayer(), this.itemId);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        TradingManager.getInstance().removeItem(this.playerA.getPlayer(), this.itemId);
        connection = GameServer.getConnectionFromPlayer(this.playerB.getPlayer());
        ByteBufferWriter b = connection.startPacket();
        PacketTypes.PacketType.TradingUIRemoveItem.doPacket(b);
        this.write(b);
        PacketTypes.PacketType.TradingUIRemoveItem.send(connection);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.playerA.getPlayer() != null && this.playerB.getPlayer() != null;
    }
}
