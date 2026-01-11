// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.ServerOptions;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ReloadOptionsPacket implements INetworkPacket {
    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(ServerOptions.instance.getPublicOptions().size());
        String key = null;

        for (String var4 : ServerOptions.instance.getPublicOptions()) {
            b.putUTF(var4);
            b.putUTF(ServerOptions.instance.getOption(var4));
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        int options = b.getInt();

        for (int i = 0; i < options; i++) {
            ServerOptions.instance.putOption(GameWindow.ReadString(b), GameWindow.ReadString(b));
        }
    }
}
