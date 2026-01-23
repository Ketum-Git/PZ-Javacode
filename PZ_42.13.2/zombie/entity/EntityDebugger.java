// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.entity.components.script.EntityScriptInfo;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.entity.util.ImmutableArray;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.GameEntityScript;

public class EntityDebugger {
    private final Engine engine;
    private long updateStamp;
    private int updatePeakMilli;
    private final int[] updateTimes = new int[60];
    private int updateTimeIdx;
    private int updateTimeMilliAverage;
    private int lifeTimeSaves;
    private int lifeTimeLoads;
    private byte[] bytesCache;

    EntityDebugger(Engine engine) {
        this.engine = engine;
    }

    void beginUpdate() {
        this.updateStamp = System.nanoTime();
    }

    void endUpdate() {
        int t = (int)((System.nanoTime() - this.updateStamp) / 1000000L);
        this.updatePeakMilli = PZMath.max(this.updatePeakMilli, t);
        this.updateTimes[this.updateTimeIdx] = t;
        this.updateTimeIdx++;
        if (this.updateTimeIdx >= this.updateTimes.length) {
            this.updateTimeIdx = 0;
            int avg = 0;

            for (int i = 0; i < this.updateTimes.length; i++) {
                avg += this.updateTimes[i];
            }

            this.updateTimeMilliAverage = avg / this.updateTimes.length;
        }
    }

    void beginSave(File file) {
        this.lifeTimeSaves++;
    }

    void endSave(ByteBuffer byteBuffer) {
    }

    void beginLoad(File file) {
        this.lifeTimeLoads++;
    }

    void endLoad(ByteBuffer byteBuffer) {
    }

    ArrayList<GameEntity> getIsoEntitiesDebug() {
        if (!Core.debug) {
            return null;
        } else {
            ImmutableArray<GameEntity> entities = this.engine.getIsoObjectBucket().getEntities();
            ArrayList<GameEntity> list = new ArrayList<>();

            for (int i = 0; i < entities.size(); i++) {
                GameEntity gameEntity = entities.get(i);
                if (gameEntity.getGameEntityType() == GameEntityType.IsoObject
                    && gameEntity instanceof IsoObject
                    && (
                        !gameEntity.hasComponent(ComponentType.SpriteConfig)
                            || gameEntity.<SpriteConfig>getComponent(ComponentType.SpriteConfig).isMultiSquareMaster()
                    )) {
                    list.add(gameEntity);
                }
            }

            return list;
        }
    }

    void reloadDebug() throws Exception {
        if (Core.debug && !GameClient.client && !GameServer.server) {
            if (this.engine.isProcessing()) {
                DebugLog.General.println("Cannot reload entities when engine is processing.");
                return;
            }

            ImmutableArray<GameEntity> entities = this.engine.getIsoObjectBucket().getEntities();
            DebugLog.General.println("-- Reloading '" + entities.size() + "' Instantiated IsoObject Entities --");
            if (entities.size() == 0) {
                return;
            }

            byte[] bytes = this.bytesCache != null ? this.bytesCache : new byte[2097152];
            this.bytesCache = bytes;
            ByteBuffer bb = ByteBuffer.wrap(bytes);

            for (int i = 0; i < entities.size(); i++) {
                GameEntity gameEntity = entities.get(i);

                try {
                    DebugLog.General.println("Reloading: " + gameEntity.getEntityFullTypeDebug());
                    this.reloadEntity(bb, gameEntity);
                } catch (Exception var7) {
                    var7.printStackTrace();
                }
            }
        }
    }

    void reloadDebugEntity(GameEntity gameEntity) throws Exception {
        if (Core.debug && !GameClient.client && !GameServer.server) {
            if (this.engine.isProcessing()) {
                DebugLog.General.println("Cannot reload entities when engine is processing.");
                return;
            }

            DebugLog.General.println("-- Reloading Entity --");
            if (gameEntity.getGameEntityType() != GameEntityType.IsoObject || !(gameEntity instanceof IsoObject)) {
                DebugLog.General.println("Failed to reload entity, not IsoObject");
                return;
            }

            byte[] bytes = this.bytesCache != null ? this.bytesCache : new byte[2097152];
            this.bytesCache = bytes;
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            this.reloadEntity(bb, gameEntity);
        }
    }

    private void removeEntityComponents(GameEntity gameEntity) {
        if (!this.engine.isProcessing() && !gameEntity.addedToEntityManager && !gameEntity.addedToEngine) {
            if (gameEntity.hasComponents()) {
                for (int i = gameEntity.componentSize() - 1; i >= 0; i--) {
                    Component component = gameEntity.getComponentForIndex(i);
                    gameEntity.releaseComponent(component);
                }
            }
        } else {
            throw new RuntimeException("Cannot remove components when engine is processing or entity added to manager.");
        }
    }

    private void reloadEntity(ByteBuffer bb, GameEntity gameEntity) throws Exception {
        if (this.engine.isProcessing()) {
            DebugLog.General.println("Cannot reload entities when engine is processing.");
        } else {
            boolean added = false;
            if (gameEntity.addedToEntityManager) {
                added = true;
                GameEntityManager.UnregisterEntity(gameEntity);
            }

            bb.clear();
            gameEntity.saveEntity(bb);
            bb.flip();
            this.removeEntityComponents(gameEntity);
            gameEntity.loadEntity(bb, 241);
            if (added) {
                GameEntityManager.RegisterEntity(gameEntity);
            }
        }
    }

    void reloadEntityFromScriptDebug(GameEntity gameEntity) throws Exception {
        if (Core.debug && !GameClient.client && !GameServer.server) {
            if (this.engine.isProcessing()) {
                DebugLog.General.println("Cannot reload entities when engine is processing.");
                return;
            }

            DebugLog.General.println("-- Reloading Entity From Script --");
            if (gameEntity.getGameEntityType() != GameEntityType.IsoObject || !(gameEntity instanceof IsoObject)) {
                DebugLog.General.println("Failed to reload entity from script, not IsoObject");
                return;
            }

            EntityScriptInfo entityScript = gameEntity.getComponent(ComponentType.Script);
            if (entityScript != null && entityScript.getScript() != null) {
                boolean added = false;
                if (gameEntity.addedToEntityManager) {
                    added = true;
                    GameEntityManager.UnregisterEntity(gameEntity);
                }

                this.removeEntityComponents(gameEntity);
                GameEntityScript script = entityScript.getScript();
                GameEntityFactory.CreateEntityDebugReload(gameEntity, script, false);
                if (added) {
                    GameEntityManager.RegisterEntity(gameEntity);
                }
            } else {
                DebugLog.General.warn("Failed to reload from script, no script component or component has no valid script.");
            }
        }
    }
}
