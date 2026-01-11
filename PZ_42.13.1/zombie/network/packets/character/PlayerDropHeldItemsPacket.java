// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public final class PlayerDropHeldItemsPacket extends PlayerID implements INetworkPacket {
    @JSONField
    boolean heavy;
    @JSONField
    int x;
    @JSONField
    int y;
    @JSONField
    int z;

    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0]);
        this.x = (Integer)values[1];
        this.y = (Integer)values[2];
        this.z = (Integer)values[3];
        this.heavy = (Boolean)values[4];
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        if (this.isConsistent(connection)) {
            this.x = b.getInt();
            this.y = b.getInt();
            this.z = b.get();
            this.heavy = b.get() == 1;
            this.getPlayer().dropHeldItems(this.x, this.y, this.z, this.heavy);
            this.sendToClients(PacketTypes.PacketType.Equip, null);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte((byte)this.z);
        b.putBoolean(this.heavy);
    }
}
