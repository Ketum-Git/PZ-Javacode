// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;

@UsedFromLua
public class MetaEntity extends GameEntity {
    private static final ConcurrentLinkedQueue<MetaEntity> pool = new ConcurrentLinkedQueue<>();
    private long entityNetId;
    private GameEntityType originalEntityType;
    private float x;
    private float y;
    private float z;
    private boolean isOutsideCached;
    boolean scheduleForReleaseToPool;

    static MetaEntity alloc(GameEntity entity) {
        if (Core.debug && !(entity instanceof IsoObject)) {
            throw new RuntimeException("Can't alloc non-isoObject");
        } else {
            MetaEntity metaEntity = pool.poll();
            if (metaEntity == null) {
                metaEntity = new MetaEntity();
            }

            metaEntity.entityNetId = entity.getEntityNetID();
            metaEntity.x = entity.getX();
            metaEntity.y = entity.getY();
            metaEntity.z = entity.getZ();
            metaEntity.originalEntityType = entity.getGameEntityType();
            metaEntity.isOutsideCached = entity.getSquare().isOutside();
            return metaEntity;
        }
    }

    static MetaEntity alloc() {
        MetaEntity metaEntity = pool.poll();
        if (metaEntity == null) {
            metaEntity = new MetaEntity();
        }

        return metaEntity;
    }

    static void release(MetaEntity metaEntity) {
        if (metaEntity != null) {
            metaEntity.reset();
            pool.offer(metaEntity);
        }
    }

    private MetaEntity() {
    }

    public final void saveMetaEntity(ByteBuffer output) throws IOException {
        output.putLong(this.entityNetId);
        output.put(this.originalEntityType.getByteId());
        output.putFloat(this.x);
        output.putFloat(this.y);
        output.putFloat(this.z);
        output.put((byte)(this.isOutsideCached ? 1 : 0));
        this.saveEntity(output);
    }

    public final void loadMetaEntity(ByteBuffer input, int WorldVersion) throws IOException {
        this.entityNetId = input.getLong();
        this.originalEntityType = GameEntityType.FromID(input.get());
        this.x = input.getFloat();
        this.y = input.getFloat();
        this.z = input.getFloat();
        if (WorldVersion >= 233) {
            this.isOutsideCached = input.get() == 1;
        }

        this.loadEntity(input, WorldVersion);
    }

    @Override
    public GameEntityType getGameEntityType() {
        return GameEntityType.MetaEntity;
    }

    public GameEntityType getOriginalGameEntityType() {
        return this.originalEntityType;
    }

    @Override
    public boolean isEntityValid() {
        return true;
    }

    @Override
    public IsoGridSquare getSquare() {
        return null;
    }

    @Override
    public long getEntityNetID() {
        return this.entityNetId;
    }

    @Override
    public float getX() {
        return this.x;
    }

    @Override
    public float getY() {
        return this.y;
    }

    @Override
    public float getZ() {
        return this.z;
    }

    @Override
    public boolean isMeta() {
        return true;
    }

    @Override
    public boolean isOutside() {
        return this.isOutsideCached;
    }

    @Override
    public boolean isUsingPlayer(IsoPlayer target) {
        return false;
    }

    @Override
    public IsoPlayer getUsingPlayer() {
        return null;
    }

    @Override
    public void setUsingPlayer(IsoPlayer player) {
    }

    @Override
    public void reset() {
        super.reset();
        this.scheduleForReleaseToPool = false;
        this.entityNetId = Long.MIN_VALUE;
        this.x = 0.0F;
        this.y = 0.0F;
        this.z = 0.0F;
    }
}
