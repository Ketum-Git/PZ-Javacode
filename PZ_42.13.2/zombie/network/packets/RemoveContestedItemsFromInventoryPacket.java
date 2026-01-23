// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class RemoveContestedItemsFromInventoryPacket implements INetworkPacket {
    ArrayList<Integer> ids = new ArrayList<>();

    @Override
    public void setData(Object... values) {
        this.ids.clear();
        this.ids.addAll((Collection<? extends Integer>)values[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort((short)this.ids.size());

        for (int n = 0; n < this.ids.size(); n++) {
            b.putInt(this.ids.get(n));
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.ids.clear();
        short count = b.getShort();

        for (int n = 0; n < count; n++) {
            int id = b.getInt();
            this.ids.add(id);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        for (Integer id : this.ids) {
            for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                IsoPlayer player = IsoPlayer.players[pn];
                if (player != null && !player.isDead()) {
                    player.getInventory().removeItemWithIDRecurse(id);
                }
            }
        }
    }
}
