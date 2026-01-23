// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityManager;
import zombie.network.JSONField;

public class GameEntityID implements INetworkPacketField {
    protected GameEntity entity = null;
    @JSONField
    protected long entityNetID = -1L;

    public void set(GameEntity entity) {
        this.entity = entity;
        this.entityNetID = entity.getEntityNetID();
        if (this.entityNetID < 0L) {
            throw new RuntimeException("Invalid EntityNetID");
        }
    }

    public GameEntity getGameEntity() {
        return this.entity;
    }

    public void clear() {
        this.entityNetID = -1L;
        this.entity = null;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.entityNetID = b.getLong();
        this.entity = GameEntityManager.GetEntity(this.entityNetID);
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putLong(this.entityNetID);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.entityNetID != -1L && this.entity != null;
    }
}
