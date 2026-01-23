// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.characters.HaloTextHelper;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class HaloTextPacket implements INetworkPacket {
    @JSONField
    private final PlayerID player = new PlayerID();
    @JSONField
    private String text;
    @JSONField
    private String separator;

    @Override
    public void setData(Object... values) {
        this.player.set((IsoPlayer)values[0]);
        this.text = (String)values[1];
        this.separator = (String)values[2];
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.player.parse(b, connection);
        this.text = GameWindow.ReadString(b);
        this.separator = GameWindow.ReadString(b);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        b.putUTF(this.text);
        b.putUTF(this.separator);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.player.isConsistent(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        HaloTextHelper.addText(this.player.getPlayer(), this.text, this.separator);
        DebugLog.Multiplayer.debugln("Halo text: %s%s", this.separator, this.text);
    }
}
