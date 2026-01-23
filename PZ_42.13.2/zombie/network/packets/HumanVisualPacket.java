// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class HumanVisualPacket implements INetworkPacket {
    @JSONField
    PlayerID player = new PlayerID();

    @Override
    public void setData(Object... values) {
        if (values.length == 1 && values[0] instanceof IsoPlayer) {
            this.set((IsoPlayer)values[0]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    private void set(IsoPlayer player) {
        this.player.set(player);
    }

    public void process(UdpConnection connection) {
        if (GameServer.server) {
            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (c.getConnectedGUID() != connection.getConnectedGUID()) {
                    IsoPlayer p2 = GameServer.getAnyPlayerFromConnection(c);
                    if (p2 != null) {
                        ByteBufferWriter b2 = c.startPacket();
                        PacketTypes.PacketType.HumanVisual.doPacket(b2);

                        try {
                            this.write(b2);
                            PacketTypes.PacketType.HumanVisual.send(c);
                        } catch (RuntimeException var7) {
                            c.cancelPacket();
                            ExceptionLogger.logException(var7);
                        }
                    }
                }
            }
        }

        if (GameClient.client) {
            this.player.getPlayer().resetModelNextFrame();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.process(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.process(connection);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.player.parse(b, connection);
        if (this.player.isConsistent(connection)) {
            try {
                this.player.getPlayer().getHumanVisual().load(b, 241);
            } catch (IOException var4) {
                throw new RuntimeException(var4);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);

        try {
            this.player.getPlayer().getHumanVisual().save(b.bb);
        } catch (IOException var3) {
            throw new RuntimeException(var3);
        }
    }
}
