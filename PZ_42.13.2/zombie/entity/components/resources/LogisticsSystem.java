// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.resources;

import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;

public class LogisticsSystem extends EngineSystem {
    private EntityBucket resourcesEntities;

    public LogisticsSystem(int updatePriority) {
        super(false, true, updatePriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.resourcesEntities = engine.getBucket(Family.all(ComponentType.Resources).get());
    }

    @Override
    public void removedFromEngine(Engine engine) {
    }

    private boolean isValidEntity(GameEntity entity) {
        return entity.isEntityValid() && entity.isValidEngineEntity();
    }

    @Override
    public void updateSimulation() {
        if (!GameClient.client) {
            ImmutableArray<GameEntity> entities = this.resourcesEntities.getEntities();
            if (entities.size() != 0) {
                for (int i = 0; i < entities.size(); i++) {
                    GameEntity entity = entities.get(i);
                    if (this.isValidEntity(entity)) {
                        Resources resources = entity.getComponent(ComponentType.Resources);
                        if (!resources.isValid()) {
                        }
                    }
                }
            }
        }
    }
}
