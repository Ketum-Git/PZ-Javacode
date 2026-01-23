// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.entity.components.crafting.CraftLogicSystem;
import zombie.entity.components.crafting.FurnaceLogicSystem;
import zombie.entity.components.crafting.MashingLogicSystem;
import zombie.entity.components.fluids.FluidContainerUpdateSystem;
import zombie.entity.components.resources.LogisticsSystem;
import zombie.entity.components.resources.ResourceUpdateSystem;
import zombie.entity.meta.MetaTagComponent;
import zombie.entity.system.RenderLastSystem;
import zombie.entity.util.LongMap;
import zombie.iso.IsoObject;
import zombie.iso.SaveBufferMap;
import zombie.network.GameClient;
import zombie.util.ByteBufferPooledObject;

public class GameEntityManager {
    private static final boolean VERBOSE = true;
    public static boolean debugMode = false;
    private static final boolean DEBUG_DISABLE_SAVE = false;
    private static final String saveFile = "entity_data.bin";
    private static File cacheFile;
    private static ByteBuffer cacheByteBuffer;
    private static final LongMap<GameEntity> idToEntityMap = new LongMap<>();
    private static Engine engine;
    private static EntityDebugger debugger;
    private static final ArrayDeque<MetaEntity> delayedReleaseMetaEntities = new ArrayDeque<>();
    private static boolean initialized;
    private static boolean wasClient;
    public static final int bbBlockSize = 1048576;

    protected GameEntityManager() {
    }

    public static void Init(int WorldVersion) {
        if (engine != null) {
            DebugLog.General.warn("Previous engine not disposed!");
            engine = null;
        }

        cacheFile = ZomboidFileSystem.instance.getFileInCurrentSave("entity_data.bin");
        engine = new Engine();
        debugMode = Core.debug;
        if (Core.debug) {
            debugger = new EntityDebugger(engine);
        }

        int order = 0;
        int UPDATE_usingPlayer = order++;
        int UPDATE_inventoryItem = order++;
        int UPDATE_fluidContainers = order++;
        int UPDATE_logistics = order++;
        int UPDATE_craftLogic = order++;
        int UPDATE_furnaceLogic = order++;
        int UPDATE_mashingLogic = order++;
        int UPDATE_metaEntities = order++;
        int UPDATE_resources = order++;
        order = 0;
        int RENDER_renderLast = order++;
        engine.setEntityListener(new GameEntityManager.EngineEntityListener());
        CustomBuckets.initializeCustomBuckets(engine);
        engine.addSystem(new CraftLogicSystem(UPDATE_craftLogic));
        engine.addSystem(new UsingPlayerUpdateSystem(UPDATE_usingPlayer));
        engine.addSystem(new InventoryItemSystem(UPDATE_inventoryItem));
        engine.addSystem(new MetaEntitySystem(UPDATE_metaEntities));
        engine.addSystem(new LogisticsSystem(UPDATE_logistics));
        engine.addSystem(new ResourceUpdateSystem(UPDATE_resources));
        engine.addSystem(new MashingLogicSystem(UPDATE_mashingLogic));
        engine.addSystem(new FurnaceLogicSystem(UPDATE_furnaceLogic));
        engine.addSystem(new FluidContainerUpdateSystem(UPDATE_fluidContainers));
        engine.addSystem(new RenderLastSystem(RENDER_renderLast));
        if (Core.debug) {
        }

        wasClient = GameClient.client;
        initialized = true;
        load(WorldVersion);
    }

    public static void Update() {
        if (debugger != null) {
            debugger.beginUpdate();
        }

        engine.update();
        EntitySimulation.update();
        int simulationTicks = GameClient.client ? 0 : EntitySimulation.getSimulationTicksThisFrame();
        if (simulationTicks > 0) {
            for (int i = 0; i < simulationTicks; i++) {
                engine.updateSimulation();
            }
        }

        MetaEntity m;
        if (!delayedReleaseMetaEntities.isEmpty()) {
            while ((m = delayedReleaseMetaEntities.poll()) != null) {
                MetaEntity.release(m);
            }
        }

        if (debugger != null) {
            debugger.endUpdate();
        }
    }

    public static boolean isEngineProcessing() {
        return engine != null && engine.isProcessing();
    }

    public static void RenderLast() {
        engine.renderLast();
    }

    public static void Reset() {
        initialized = false;
        engine = null;
        debugger = null;
        idToEntityMap.clear();
        EntitySimulation.reset();
        if (cacheByteBuffer != null) {
            cacheByteBuffer.clear();
        }

        cacheFile = null;
        delayedReleaseMetaEntities.clear();
    }

    public static GameEntity GetEntity(long gameEntityNetID) {
        synchronized (idToEntityMap) {
            return idToEntityMap.get(gameEntityNetID);
        }
    }

    static void RegisterEntity(GameEntity gameEntity) {
        if (gameEntity != null && gameEntity.hasComponents()) {
            if (gameEntity.componentSize() != 1 || !gameEntity.hasComponent(ComponentType.Script)) {
                if (GameClient.client) {
                    long entityNetID = gameEntity.getEntityNetID();
                    if (entityNetID == -1L) {
                        throw new RuntimeException("getEntityNetID returned -1");
                    } else {
                        synchronized (idToEntityMap) {
                            idToEntityMap.put(entityNetID, gameEntity);
                        }

                        gameEntity.addedToEntityManager = true;
                        gameEntity.addedToEngine = true;
                        gameEntity.sendRequestSyncGameEntity();
                    }
                } else {
                    DebugLog.Entity
                        .println(
                            "Registering entity id = "
                                + gameEntity.getEntityNetID()
                                + " [type:"
                                + gameEntity.getGameEntityType()
                                + ", name:"
                                + gameEntity.getEntityFullTypeDebug()
                                + ", comps: "
                                + gameEntity.componentSize()
                                + "]"
                        );
                    boolean loadMeta = false;
                    long storedID = gameEntity.getEntityNetID();
                    if (gameEntity instanceof IsoObject && gameEntity.hasComponent(ComponentType.MetaTag)) {
                        MetaTagComponent metaTag = (MetaTagComponent)gameEntity.removeComponent(ComponentType.MetaTag);
                        storedID = metaTag.getStoredID();
                        loadMeta = true;
                    }

                    GameEntity stored;
                    synchronized (idToEntityMap) {
                        stored = idToEntityMap.get(storedID);
                    }

                    if (loadMeta) {
                        if (!(stored instanceof MetaEntity source)) {
                            return;
                        }

                        DebugLog.Entity.println("IsoObject Entity respawn - " + gameEntity.getEntityNetID() + " loading from MetaEntity...");

                        for (int i = source.componentSize() - 1; i >= 0; i--) {
                            Component component = source.getComponentForIndex(i);
                            source.removeComponent(component);
                            if (gameEntity.hasComponent(component.getComponentType())) {
                                gameEntity.releaseComponent(component.getComponentType());
                            }

                            gameEntity.addComponent(component);
                        }

                        gameEntity.connectComponents();
                        UnregisterEntity(stored);
                    } else if (stored != null) {
                        return;
                    }

                    engine.addEntity(gameEntity);
                    long entityNetID = gameEntity.getEntityNetID();
                    synchronized (idToEntityMap) {
                        idToEntityMap.put(entityNetID, gameEntity);
                    }

                    gameEntity.addedToEntityManager = true;
                    if (gameEntity instanceof IsoObject isoObject && gameEntity.hasComponent(ComponentType.FluidContainer)) {
                        isoObject.sync();
                    }

                    if (loadMeta && gameEntity instanceof IsoObject isoObject) {
                        isoObject.getChunk().requiresHotSave = true;
                    }
                }
            }
        }
    }

    static void UnregisterEntity(GameEntity gameEntity) {
        UnregisterEntity(gameEntity, false);
    }

    static void UnregisterEntity(GameEntity gameEntity, boolean offloadToMeta) {
        if (gameEntity != null && gameEntity.addedToEntityManager) {
            if (!GameClient.client && !wasClient) {
                DebugLog.Entity
                    .println(
                        "Unregistering entity id = "
                            + gameEntity.getEntityNetID()
                            + " [type:"
                            + gameEntity.getGameEntityType()
                            + ", name:"
                            + gameEntity.getEntityFullTypeDebug()
                            + ", tryOffloadToMeta:"
                            + offloadToMeta
                            + "]"
                    );
                long entityNetID = gameEntity.getEntityNetID();
                GameEntity stored;
                synchronized (idToEntityMap) {
                    stored = idToEntityMap.remove(entityNetID);
                }

                if (stored != null) {
                    if (stored != gameEntity) {
                        throw new RuntimeException("Stored entity mismatch");
                    } else {
                        engine.removeEntity(gameEntity);
                        if (offloadToMeta && gameEntity instanceof IsoObject && ComponentType.bitsRunInMeta.intersects(gameEntity.getComponentBits())) {
                            DebugLog.Entity.println("IsoObject Entity despawn - " + gameEntity.getEntityNetID() + " saving to MetaEntity...");
                            boolean shouldStoreMeta = false;

                            for (int i = gameEntity.componentSize() - 1; i >= 0; i--) {
                                Component component = gameEntity.getComponentForIndex(i);
                                if (component.getComponentType().isRunInMeta() && component.isQualifiesForMetaStorage()) {
                                    shouldStoreMeta = true;
                                    break;
                                }
                            }

                            if (!shouldStoreMeta) {
                                DebugLog.Entity
                                    .println(
                                        "IsoObject Entity despawn - ignoring meta storage for entity: "
                                            + gameEntity.getEntityNetID()
                                            + ", no components actually require meta updating right now..."
                                    );
                            } else {
                                MetaEntity metaEntity = MetaEntity.alloc(gameEntity);

                                for (int ix = gameEntity.componentSize() - 1; ix >= 0; ix--) {
                                    Component component = gameEntity.getComponentForIndex(ix);
                                    gameEntity.removeComponent(component);
                                    metaEntity.addComponent(component);
                                }

                                metaEntity.connectComponents();
                                RegisterEntity(metaEntity);
                                MetaTagComponent metaTag = (MetaTagComponent)ComponentType.MetaTag.CreateComponent();
                                metaTag.setStoredID(metaEntity.getEntityNetID());
                                gameEntity.addComponent(metaTag);
                            }
                        }

                        gameEntity.addedToEntityManager = false;
                        if (gameEntity instanceof MetaEntity metaEntity) {
                            if (!gameEntity.addedToEngine && !gameEntity.scheduledForEngineRemoval && !gameEntity.removingFromEngine) {
                                MetaEntity.release(metaEntity);
                            } else {
                                delayedReleaseMetaEntities.add(metaEntity);
                            }
                        }
                    }
                }
            } else {
                long entityNetID = gameEntity.getEntityNetID();
                synchronized (idToEntityMap) {
                    idToEntityMap.remove(entityNetID);
                }

                gameEntity.addedToEntityManager = false;
                gameEntity.addedToEngine = false;
            }
        }
    }

    static void onEntityAddedToEngine(GameEntity entity) {
    }

    static void onEntityRemovedFromEngine(GameEntity entity) {
    }

    public static void checkEntityIDChange(GameEntity entity, long oldID, long newID) {
        if (entity != null) {
            if (oldID != -1L) {
                if (oldID != newID) {
                    synchronized (idToEntityMap) {
                        GameEntity storedOld = idToEntityMap.get(oldID);
                        GameEntity storedNew = idToEntityMap.get(newID);
                        if (storedOld == entity) {
                            idToEntityMap.remove(oldID);
                        } else {
                            DebugLog.Entity.error("idToEntityMap(%ld)=%s, expected %s", oldID, storedOld, entity);
                        }

                        if (storedNew instanceof IsoObject newObject) {
                            newObject.getEntityNetID();
                        }

                        if (idToEntityMap.get(newID) == null) {
                            idToEntityMap.put(newID, entity);
                        } else {
                            DebugLog.Entity.error("idToEntityMap(%ld)=%s, expected null", newID, idToEntityMap.get(newID), entity);
                        }
                    }
                }
            }
        }
    }

    public static ByteBuffer ensureCapacity(ByteBuffer bb, int requiredSize) {
        requiredSize = PZMath.max(requiredSize, 1048576);
        if (bb == null) {
            return ByteBuffer.allocate(requiredSize + 1048576);
        } else {
            if (bb.capacity() < requiredSize + 1048576) {
                ByteBuffer old = bb;
                bb = ByteBuffer.allocate(requiredSize + 1048576);
                bb.put(old.array(), 0, old.position());
            }

            return bb;
        }
    }

    public static void Save() {
        if (!initialized || engine == null) {
            throw new UnsupportedOperationException("Can not save when manager not initialized.");
        } else if (!Core.getInstance().isNoSave()) {
            if (Core.debug) {
            }

            try {
                DebugLog.Entity.println("Saving GameEntityManager...");
                if (debugger != null) {
                    debugger.beginSave(cacheFile);
                }

                ByteBuffer bb = ensureCapacity(cacheByteBuffer, 1048576);
                bb.clear();
                MetaEntitySystem system = engine.getSystem(MetaEntitySystem.class);
                bb = system.saveMetaEntities(bb);

                try (FileOutputStream output = new FileOutputStream(cacheFile)) {
                    output.getChannel().truncate(0L);
                    output.write(bb.array(), 0, bb.position());
                } catch (Exception var7) {
                    ExceptionLogger.logException(var7);
                    return;
                }

                cacheByteBuffer = bb;
                if (debugger != null) {
                    debugger.endSave(cacheByteBuffer);
                }
            } catch (Exception var8) {
                ExceptionLogger.logException(var8);
            }
        }
    }

    public static void saveToBufferMap(SaveBufferMap bufferMap) {
        if (!initialized || engine == null) {
            throw new UnsupportedOperationException("Can not save when manager not initialized.");
        } else if (!Core.getInstance().isNoSave()) {
            if (Core.debug) {
            }

            try {
                DebugLog.Entity.println("Saving GameEntityManager...");
                if (debugger != null) {
                    debugger.beginSave(cacheFile);
                }

                ByteBuffer bb = ensureCapacity(cacheByteBuffer, 1048576);
                bb.clear();
                MetaEntitySystem system = engine.getSystem(MetaEntitySystem.class);
                bb = system.saveMetaEntities(bb);
                String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("entity_data.bin");
                ByteBufferPooledObject buffer = bufferMap.allocate(bb.position());
                buffer.put(bb.array(), 0, bb.position());
                bufferMap.put(fileName, buffer);
                cacheByteBuffer = bb;
                if (debugger != null) {
                    debugger.endSave(cacheByteBuffer);
                }
            } catch (Exception var5) {
                ExceptionLogger.logException(var5);
            }
        }
    }

    private static void load(int WorldVersion) {
        if (!initialized || engine == null) {
            throw new UnsupportedOperationException("Can not load when manager not initialized.");
        } else if (!Core.getInstance().isNoSave()) {
            if (Core.debug) {
            }

            try {
                DebugLog.Entity.println("Loading GameEntityManager...");
                if (!cacheFile.exists()) {
                    DebugLog.Entity.println("- skipping entity loading, no file -");
                    return;
                }

                if (debugger != null) {
                    debugger.beginLoad(cacheFile);
                }

                ByteBuffer bb;
                try (FileInputStream inStream = new FileInputStream(cacheFile)) {
                    bb = ensureCapacity(cacheByteBuffer, (int)cacheFile.length());
                    bb.clear();
                    int len = inStream.read(bb.array());
                    bb.limit(PZMath.max(len, 0));
                } catch (Exception var7) {
                    ExceptionLogger.logException(var7);
                    return;
                }

                MetaEntitySystem system = engine.getSystem(MetaEntitySystem.class);
                system.loadMetaEntities(bb, WorldVersion);
                cacheByteBuffer = bb;
                if (debugger != null) {
                    debugger.endLoad(cacheByteBuffer);
                }
            } catch (Exception var8) {
                ExceptionLogger.logException(var8);
            }
        }
    }

    public static ArrayList<GameEntity> getIsoEntitiesDebug() {
        return debugger != null ? debugger.getIsoEntitiesDebug() : null;
    }

    public static void reloadDebug() throws Exception {
        if (debugger != null) {
            debugger.reloadDebug();
        }
    }

    public static void reloadDebugEntity(GameEntity gameEntity) throws Exception {
        if (debugger != null) {
            debugger.reloadDebugEntity(gameEntity);
        }
    }

    public static void reloadEntityFromScriptDebug(GameEntity gameEntity) throws Exception {
        if (debugger != null) {
            debugger.reloadEntityFromScriptDebug(gameEntity);
        }
    }

    private static class EngineEntityListener implements Engine.EntityListener {
        @Override
        public void onEntityAddedToEngine(GameEntity entity) {
            GameEntityManager.onEntityAddedToEngine(entity);
        }

        @Override
        public void onEntityRemovedFromEngine(GameEntity entity) {
            GameEntityManager.onEntityRemovedFromEngine(entity);
        }
    }
}
