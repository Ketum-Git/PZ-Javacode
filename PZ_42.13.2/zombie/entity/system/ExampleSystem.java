// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.system;

import java.util.Objects;
import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.IBucketListener;
import zombie.entity.components.crafting.CraftLogic;
import zombie.entity.util.ImmutableArray;

public class ExampleSystem extends EngineSystem {
    EntityBucket craftEntities;
    EntityBucket nonMetaEntities;
    int exampleCounter;

    public ExampleSystem(int updatePriority) {
        super(false, true, updatePriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.craftEntities = engine.getBucket(Family.all(ComponentType.CraftLogic).get());
        this.nonMetaEntities = engine.getCustomBucket("NonMetaEntities");
        this.nonMetaEntities.addListener(0, new ExampleSystem.CraftEntityBucketListener());
    }

    @Override
    public void removedFromEngine(Engine engine) {
    }

    @Override
    public void update() {
        ImmutableArray<GameEntity> entities = this.nonMetaEntities.getEntities();

        for (int i = 0; i < entities.size(); i++) {
            GameEntity entity = entities.get(i);
        }
    }

    @Override
    public void updateSimulation() {
        ImmutableArray<GameEntity> entities = this.craftEntities.getEntities();

        for (int i = 0; i < entities.size(); i++) {
            GameEntity entity = entities.get(i);
            CraftLogic craftLogic = entity.getComponent(ComponentType.CraftLogic);
        }
    }

    @Override
    public void renderLast() {
    }

    private class CraftEntityBucketListener implements IBucketListener {
        private CraftEntityBucketListener() {
            Objects.requireNonNull(ExampleSystem.this);
            super();
        }

        @Override
        public void onBucketEntityAdded(EntityBucket bucket, GameEntity entity) {
            ExampleSystem.this.exampleCounter += 100;
        }

        @Override
        public void onBucketEntityRemoved(EntityBucket bucket, GameEntity entity) {
            ExampleSystem.this.exampleCounter -= 100;
        }
    }
}
