// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.actions;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.WorldItemTypes;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class AddCorpseToMapPacket implements INetworkPacket {
    IsoDeadBody body;
    short onlineId;
    protected final ObjectID objectId = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
    int x;
    int y;
    byte z;

    public void set(IsoGridSquare sq, IsoDeadBody body) {
        this.body = body;
        this.objectId.set(body.getObjectID());
        this.onlineId = body.getCharacterOnlineID();
        this.x = sq.x;
        this.y = sq.y;
        this.z = (byte)sq.z;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.objectId.load(b);
        this.onlineId = b.getShort();
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.get();
        this.body = (IsoDeadBody)WorldItemTypes.createFromBuffer(b);
        if (this.body != null) {
            this.body.loadFromRemoteBuffer(b, false);
            this.body.getObjectID().set(this.objectId);
            if (GameServer.server) {
                IsoGridSquare sq = ServerMap.instance.getGridSquare(this.x, this.y, this.z);
                if (sq != null) {
                    sq.addCorpse(this.body, true);

                    for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                        UdpConnection c = GameServer.udpEngine.connections.get(n);
                        if (c.getConnectedGUID() != connection.getConnectedGUID() && c.RelevantTo(this.x, this.y)) {
                            ByteBufferWriter b2 = c.startPacket();
                            PacketTypes.PacketType.AddCorpseToMap.doPacket(b2);
                            b.rewind();
                            b2.bb.put(b);
                            PacketTypes.PacketType.AddCorpseToMap.send(c);
                        }
                    }
                }

                LoggerManager.getLogger("item").write(connection.idStr + " \"" + connection.username + "\" corpse +1 " + this.x + "," + this.y + "," + this.z);
            } else {
                this.body.setCharacterOnlineID(this.onlineId);
                ObjectIDManager.getInstance().addObject(this.body);
                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
                if (sq == null) {
                    GameClient.instance.delayPacket(this.x, this.y, this.z);
                } else {
                    sq.addCorpse(this.body, true);
                }
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.body.getObjectID().save(b.bb);
        b.putShort(this.onlineId);
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
        this.body.writeToRemoteBuffer(b);
    }
}
