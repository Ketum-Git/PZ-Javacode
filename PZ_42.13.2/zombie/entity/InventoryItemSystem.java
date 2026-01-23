// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import zombie.entity.util.ImmutableArray;
import zombie.inventory.InventoryItem;

public class InventoryItemSystem extends EngineSystem {
    EntityBucket itemEntities;

    public InventoryItemSystem(int updatePriority) {
        super(true, false, updatePriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.itemEntities = engine.getInventoryItemBucket();
    }

    @Override
    public void removedFromEngine(Engine engine) {
    }

    @Override
    public void update() {
        ImmutableArray<GameEntity> entities = this.itemEntities.getEntities();

        for (int i = 0; i < entities.size(); i++) {
            GameEntity entity = entities.get(i);
            InventoryItem item = (InventoryItem)entity;
            if (item.getEquipParent() == null || item.getEquipParent().isDead()) {
                GameEntityManager.UnregisterEntity(entity);
            }
        }
    }
}
