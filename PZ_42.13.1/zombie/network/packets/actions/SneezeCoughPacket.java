// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.actions;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.Translator;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 3, reliability = 0, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SneezeCoughPacket implements INetworkPacket {
    protected final PlayerID wielder = new PlayerID();
    protected byte value;

    public void set(IsoPlayer _wielder, int sneezingCoughing, byte sneezeVar) {
        this.wielder.set(_wielder);
        this.value = 0;
        if (sneezingCoughing % 2 == 0) {
            this.value = (byte)(this.value | 1);
        }

        if (sneezingCoughing > 2) {
            this.value = (byte)(this.value | 2);
        }

        if (sneezeVar > 1) {
            this.value = (byte)(this.value | 4);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.wielder.write(b);
        b.putByte(this.value);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.wielder.parse(b, connection);
        this.value = b.get();
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.wielder.getPlayer() != null) {
            boolean isSneeze = (this.value & 1) == 0;
            boolean isMuffled = (this.value & 2) != 0;
            int sneezeVar = (this.value & 4) == 0 ? 1 : 2;
            this.wielder.getPlayer().setVariable("Ext", isSneeze ? "Sneeze" + sneezeVar : "Cough");
            this.wielder.getPlayer().Say(Translator.getText("IGUI_PlayerText_" + (isSneeze ? "Sneeze" : "Cough") + (isMuffled ? "Muffled" : "")));
            this.wielder.getPlayer().reportEvent("EventDoExt");
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.wielder.getPlayer() != null) {
            float x = this.wielder.getPlayer().getX();
            float y = this.wielder.getPlayer().getY();
            int n = 0;

            for (int size = GameServer.udpEngine.connections.size(); n < size; n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (connection.getConnectedGUID() != c.getConnectedGUID() && c.RelevantTo(x, y)) {
                    ByteBufferWriter b2 = c.startPacket();
                    PacketTypes.PacketType.SneezeCough.doPacket(b2);
                    this.write(b2);
                    PacketTypes.PacketType.SneezeCough.send(c);
                }
            }
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.wielder.isConsistent(connection);
    }
}
