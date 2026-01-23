// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.system;

import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.GameEntity;
import zombie.entity.util.ImmutableArray;
import zombie.iso.IsoObject;
import zombie.vehicles.VehiclePart;

public class RenderLastSystem extends EngineSystem {
    EntityBucket nonMetaRenderers;

    public RenderLastSystem(int renderPriority) {
        super(false, false, Integer.MAX_VALUE, true, renderPriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.nonMetaRenderers = engine.getCustomBucket("NonMetaRenderers");
    }

    @Override
    public void removedFromEngine(Engine engine) {
    }

    @Override
    public void renderLast() {
        ImmutableArray<GameEntity> entities = this.nonMetaRenderers.getEntities();

        for (int i = 0; i < entities.size(); i++) {
            GameEntity entity = entities.get(i);
            if (entity.isValidEngineEntity() && (entity instanceof IsoObject || entity instanceof VehiclePart)) {
                entity.renderlastComponents();
            }
        }
    }
}
