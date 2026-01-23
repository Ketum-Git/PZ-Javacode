// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoMovingObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.MovingObject;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 0, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ZombieControlPacket implements INetworkPacket {
    @JSONField
    protected final MovingObject movingObject = new MovingObject();
    @JSONField
    protected short targetId;

    @Override
    public void write(ByteBufferWriter b) {
        this.movingObject.write(b);
        b.putShort(this.targetId);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.movingObject.parse(b, connection);
        this.targetId = b.getShort();
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoZombie zombie = (IsoZombie)this.movingObject.getObject();
        if (this.targetId == -1) {
            zombie.setTargetSeenTime(0.0F);
            zombie.target = null;
        } else {
            IsoPlayer target = null;
            if (GameClient.client) {
                target = GameClient.IDToPlayerMap.get(this.targetId);
            } else if (GameServer.server) {
                target = GameServer.IDToPlayerMap.get(this.targetId);
            }

            if (target != zombie.target) {
                zombie.setTargetSeenTime(0.0F);
                zombie.target = target;
            }
        }
    }

    @Override
    public void setData(Object... values) {
        this.movingObject.set((IsoMovingObject)values[0]);
        this.targetId = (Short)values[1];
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.movingObject.getObject() != null;
    }
}
