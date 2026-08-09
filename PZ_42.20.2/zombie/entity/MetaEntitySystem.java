// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.debug.DebugType;
import zombie.entity.util.ImmutableArray;

public class MetaEntitySystem extends EngineSystem {
    EntityBucket metaEntities;

    public MetaEntitySystem(int updatePriority) {
        super(false, false, Integer.MAX_VALUE, false, Integer.MAX_VALUE);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.metaEntities = engine.getCustomBucket("MetaEntities");
    }

    ByteBuffer saveMetaEntities(ByteBuffer output) throws IOException {
        int sizeEstimate = this.metaEntities.getEntities().size() * 1024;
        output = GameEntityManager.ensureCapacity(output, output.position() + sizeEstimate);
        ImmutableArray<GameEntity> entities = this.metaEntities.getEntities();
        output.putInt(entities.size());
        DebugType.Entity.println("Saving meta entities, size = " + entities.size());

        for (int i = 0; i < entities.size(); i++) {
            GameEntity entity = entities.get(i);
            if (!(entity instanceof MetaEntity metaEntity)) {
                throw new IOException("Expected MetaEntity");
            }

            metaEntity.saveMetaEntity(output);
            if (output.position() > output.capacity() - 1048576) {
                output = GameEntityManager.ensureCapacity(output, output.capacity() + 1048576);
            }
        }

        return output;
    }

    void loadMetaEntities(ByteBuffer input, int worldVersion) throws IOException {
        int storedSize = input.getInt();
        DebugType.Entity.println("Loading meta entities, size = " + storedSize);

        for (int i = 0; i < storedSize; i++) {
            MetaEntity metaEntity = MetaEntity.alloc();
            metaEntity.loadMetaEntity(input, worldVersion);
            GameEntityManager.RegisterEntity(metaEntity);
        }
    }
}
