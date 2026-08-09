// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.network;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;

public class EntityPacketData {
    private static final ConcurrentLinkedDeque<EntityPacketData> pool = new ConcurrentLinkedDeque<>();
    private EntityPacketType entityPacketType;
    public final ByteBuffer bb = ByteBuffer.allocate(1000000);

    public static EntityPacketData alloc(EntityPacketType packetType) {
        EntityPacketData packetData = pool.poll();
        if (packetData == null) {
            packetData = new EntityPacketData();
        }

        packetData.entityPacketType = packetType;
        packetType.saveToByteBuffer(packetData.bb);
        return packetData;
    }

    public static void release(EntityPacketData packetData) {
        packetData.bb.clear();

        assert !Core.debug || !pool.contains(packetData) : "Object already exists in pool.";

        pool.offer(packetData);
    }

    public EntityPacketType getEntityPacketType() {
        return this.entityPacketType;
    }
}
