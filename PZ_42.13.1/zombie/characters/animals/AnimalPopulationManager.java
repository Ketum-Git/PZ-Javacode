// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import gnu.trove.set.hash.TIntHashSet;
import java.util.ArrayList;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaGrid;
import zombie.iso.SaveBufferMap;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class AnimalPopulationManager {
    private static AnimalPopulationManager instance;
    protected static final int SQUARES_PER_CHUNK = 8;
    protected static final int CHUNKS_PER_CELL = 32;
    protected static final int SQUARES_PER_CELL = 256;
    protected int minX;
    protected int minY;
    protected int width;
    protected int height;
    protected boolean stopped;
    protected boolean client;
    private final TIntHashSet newChunks = new TIntHashSet();
    private final ArrayList<IsoAnimal> recentlyRemoved = new ArrayList<>();

    public static AnimalPopulationManager getInstance() {
        if (instance == null) {
            instance = new AnimalPopulationManager();
        }

        return instance;
    }

    AnimalPopulationManager() {
        this.newChunks.setAutoCompactionFactor(0.0F);
    }

    public void init(IsoMetaGrid metaGrid) {
        this.client = GameClient.client;
        this.minX = metaGrid.getMinX();
        this.minY = metaGrid.getMinY();
        this.width = metaGrid.getWidth();
        this.height = metaGrid.getHeight();
        this.stopped = false;
        this.n_init(this.client, GameServer.server, this.minX, this.minY, this.width, this.height);
    }

    public void addChunkToWorld(IsoChunk chunk) {
        if (!this.client) {
            if (chunk.isNewChunk()) {
                int key = chunk.wy << 16 | chunk.wx;
                this.newChunks.add(key);
            }

            this.n_loadChunk(chunk.wx, chunk.wy);
        }
    }

    public void removeChunkFromWorld(IsoChunk chunk) {
        if (!this.client) {
            if (!this.stopped) {
                this.n_unloadChunk(chunk.wx, chunk.wy);

                for (int z = chunk.getMinLevel(); z <= chunk.getMaxLevel(); z++) {
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            IsoGridSquare sq = chunk.getGridSquare(x, y, z);
                            if (sq != null && !sq.getMovingObjects().isEmpty()) {
                                for (int i = 0; i < sq.getMovingObjects().size(); i++) {
                                    if (sq.getMovingObjects().get(i) instanceof IsoAnimal animal) {
                                        animal.unloaded();
                                        this.n_addAnimal(animal);
                                        animal.setStateMachineLocked(false);
                                        animal.setDefaultState();
                                    }
                                }
                            }
                        }
                    }
                }

                int key = chunk.wy << 16 | chunk.wx;
                this.newChunks.remove(key);
            }
        }
    }

    public void virtualizeAnimal(IsoAnimal realAnimal) {
        realAnimal.unloaded();
        this.n_addAnimal(realAnimal);
        realAnimal.delete();
        realAnimal.setStateMachineLocked(false);
        realAnimal.setDefaultState();
    }

    public void addToRecentlyRemoved(IsoAnimal realAnimal) {
        if (!this.recentlyRemoved.contains(realAnimal)) {
            this.recentlyRemoved.add(realAnimal);
        }
    }

    public void update() {
        long currentMS = System.currentTimeMillis();

        for (int i = this.recentlyRemoved.size() - 1; i >= 0; i--) {
            IsoAnimal animal = this.recentlyRemoved.get(i);
            if (!animal.getEmitter().isClear()) {
                animal.updateEmitter();
            }

            if (currentMS - animal.removedFromWorldMs > 5000L) {
                animal.getEmitter().stopAll();
                this.recentlyRemoved.remove(i);
            }
        }
    }

    public void save() {
        if (!this.client) {
            AnimalManagerMain.getInstance().saveRealAnimals();
            this.n_save();
        }
    }

    public void saveToBufferMap(SaveBufferMap bufferMap) {
        if (!this.client) {
            AnimalManagerMain.getInstance().saveRealAnimals();
            AnimalManagerWorker.getInstance().saveToBufferMap(bufferMap);
        }
    }

    public void stop() {
        if (!this.client) {
            this.stopped = true;
            this.n_stop();
            this.newChunks.clear();
            this.recentlyRemoved.clear();
        }
    }

    void n_init(boolean bClient, boolean bServer, int minX, int minY, int width, int height) {
        AnimalManagerWorker.getInstance().init(bClient, bServer, minX, minY, width, height);
    }

    void n_loadChunk(int x, int y) {
        AnimalManagerMain.getInstance().loadChunk(x, y);
    }

    void n_unloadChunk(int x, int y) {
        AnimalManagerMain.getInstance().unloadChunk(x, y);
    }

    void n_addAnimal(IsoAnimal animal) {
        VirtualAnimal virtualAnimal = new VirtualAnimal();
        virtualAnimal.setX(animal.getX());
        virtualAnimal.setY(animal.getY());
        virtualAnimal.setZ(animal.getZ());
        virtualAnimal.moveForwardOnZone = animal.isMoveForwardOnZone();
        virtualAnimal.forwardDirection.set(animal.getForwardDirectionX(), animal.getForwardDirectionY());
        virtualAnimal.animals.add(animal);
        virtualAnimal.id = animal.virtualId;
        virtualAnimal.migrationGroup = animal.migrationGroup;
        MigrationGroupDefinitions.initValueFromDef(virtualAnimal);
        AnimalManagerMain.getInstance().addAnimal(virtualAnimal);
    }

    void n_saveRealAnimals(ArrayList<IsoAnimal> animals) {
        AnimalManagerWorker.getInstance().saveRealAnimals(animals);
    }

    void n_save() {
        AnimalManagerWorker.getInstance().save();
    }

    void n_stop() {
        AnimalManagerWorker.getInstance().stop();
    }
}
