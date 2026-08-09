// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.ServerOptions;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ReloadOptionsPacket implements INetworkPacket {
    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(ServerOptions.instance.getPublicOptions().size());

        for (String key : ServerOptions.instance.getPublicOptions()) {
            b.putUTF(key);
            b.putUTF(ServerOptions.instance.getOption(key));
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        int options = b.getInt();

        for (int i = 0; i < options; i++) {
            ServerOptions.instance.putOption(b.getUTF(), b.getUTF());
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        for (IsoPlayer player : GameClient.IDToPlayerMap.values()) {
            player.resetDisplayName();
        }
    }
}
