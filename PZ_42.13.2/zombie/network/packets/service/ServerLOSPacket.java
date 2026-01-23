// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.network.GameClient;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerLOS;
import zombie.network.ServerMap;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 0, requiredCapability = Capability.ConnectWithDebug, handlingType = 3)
public class ServerLOSPacket implements INetworkPacket {
    byte packetType;
    int squareX;
    int squareY;
    int squareZ;

    @Override
    public void processClient(UdpConnection connection) {
        ByteBufferWriter bbw = GameClient.connection.startPacket();
        PacketTypes.PacketType.ServerLOS.doPacket(bbw);
        this.write(bbw);
        PacketTypes.PacketType.ServerLOS.send(GameClient.connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        int playerIndex = 0;
        IsoPlayer player = connection.players[0];
        IsoGridSquare square = ServerMap.instance.getGridSquare(this.squareX, this.squareY, this.squareZ);
        if (square != null) {
            int px = player.getCurrentSquare().x;
            int py = player.getCurrentSquare().y;
            int pz = player.getCurrentSquare().z;
            LosUtil.TestResults result = LosUtil.lineClear(IsoWorld.instance.currentCell, this.squareX, this.squareY, this.squareZ, px, py, pz, false, 0);
            System.out
                .println(
                    String.format(
                        "LOS: isCouldSee(%d,%d,%d) = %s LosUtil = %s",
                        this.squareX,
                        this.squareY,
                        this.squareZ,
                        ServerLOS.instance.isCouldSee(player, square) ? "true" : "false",
                        result.toString()
                    )
                );
        }
    }

    public void set(byte packetType, int x, int y, int z) {
        this.packetType = packetType;
        this.squareX = x;
        this.squareY = y;
        this.squareZ = z;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.packetType = b.get();
        this.squareX = b.getInt();
        this.squareY = b.getInt();
        this.squareZ = b.getInt();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.packetType);
        b.putInt(this.squareX);
        b.putInt(this.squareY);
        b.putInt(this.squareZ);
    }
}
