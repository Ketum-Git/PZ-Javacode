// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import zombie.characters.IsoPlayer;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;

public class UsingPlayerUpdateSystem extends EngineSystem {
    EntityBucket isoEntities;

    public UsingPlayerUpdateSystem(int updatePriority) {
        super(true, false, updatePriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.isoEntities = engine.getIsoObjectBucket();
    }

    @Override
    public void removedFromEngine(Engine engine) {
    }

    @Override
    public void update() {
        if (!GameClient.client) {
            ImmutableArray<GameEntity> entities = this.isoEntities.getEntities();

            for (int i = 0; i < entities.size(); i++) {
                GameEntity entity = entities.get(i);
                if (entity.isValidEngineEntity()) {
                    IsoPlayer usingPlayer = entity.getUsingPlayer();
                    if (entity.getUsingPlayer() != null) {
                        int distance = 10;
                        if (usingPlayer.getX() < entity.getX() - 10.0F
                            || usingPlayer.getX() > entity.getX() + 10.0F
                            || usingPlayer.getY() < entity.getY() - 10.0F
                            || usingPlayer.getY() > entity.getY() + 10.0F
                            || usingPlayer.getZ() != entity.getZ()
                            || usingPlayer.isDead()) {
                            entity.setUsingPlayer(null);
                        }
                    }
                }
            }
        }
    }
}
