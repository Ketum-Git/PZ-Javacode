// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.actions;

import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.IsoGridSquare;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.Square;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class RemoveBloodPacket implements INetworkPacket {
    @JSONField
    Square position = new Square();
    @JSONField
    boolean onlyWall;

    @Override
    public void setData(Object... values) {
        if (values.length == 2) {
            this.position.set((IsoGridSquare)values[0]);
            this.onlyWall = (Boolean)values[1];
        } else {
            DebugType.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.position.parse(b, connection);
        this.onlyWall = b.getBoolean();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.position.write(b);
        b.putBoolean(this.onlyWall);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.position.getSquare().removeBlood(false, this.onlyWall);

        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c != connection && c.isRelevantTo(this.position.getX(), this.position.getY())) {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.RemoveBlood.doPacket(b2);
                this.write(b2);
                PacketTypes.PacketType.RemoveBlood.send(c);
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.position.getSquare().removeBlood(true, this.onlyWall);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.position.isConsistent(connection);
    }
}
