// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.network;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import zombie.core.network.ByteBufferWriter;

public enum EntityPacketType {
    UpdateUsingPlayer(PacketGroup.GameEntity),
    SyncGameEntity(PacketGroup.GameEntity),
    RequestSyncGameEntity(PacketGroup.GameEntity),
    CraftLogicSync(PacketGroup.CraftLogic),
    CraftLogicSyncFull(PacketGroup.CraftLogic),
    CraftLogicStartRequest(PacketGroup.CraftLogic),
    CraftLogicStopRequest(PacketGroup.CraftLogic),
    MashingLogicSync(PacketGroup.MashingLogic),
    MashingLogicSyncFull(PacketGroup.MashingLogic),
    MashingLogicStartRequest(PacketGroup.MashingLogic),
    MashingLogicStopRequest(PacketGroup.MashingLogic),
    ResourcesSync(PacketGroup.Resources);

    private static final Map<Short, EntityPacketType> entityPacketMap = new HashMap<>();
    private short id;
    private final PacketGroup group;

    private EntityPacketType() {
        this.group = PacketGroup.Generic;
    }

    private EntityPacketType(final PacketGroup group) {
        this.group = group;
    }

    private EntityPacketType(final short id, final PacketGroup group) {
        this.group = group;
        this.id = id;
    }

    public PacketGroup getGroup() {
        return this.group;
    }

    public boolean isEntityPacket() {
        return this.group == PacketGroup.GameEntity;
    }

    public boolean isComponentPacket() {
        return this.group != PacketGroup.GameEntity;
    }

    public void saveToByteBuffer(ByteBufferWriter bb) {
        bb.putShort(this.id);
    }

    public void saveToByteBuffer(ByteBuffer bb) {
        bb.putShort(this.id);
    }

    public static EntityPacketType FromByteBuffer(ByteBuffer bb) {
        short id = bb.getShort();
        return entityPacketMap.get(id);
    }

    static {
        short nextID = 1000;

        for (EntityPacketType entityPacketType : values()) {
            if (entityPacketType.id == 0) {
                entityPacketType.id = nextID++;
            }

            entityPacketMap.put(entityPacketType.id, entityPacketType);
        }
    }
}
