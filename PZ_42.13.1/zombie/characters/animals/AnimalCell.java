// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.utils.BooleanGrid;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.SaveBufferMap;
import zombie.iso.SliceY;
import zombie.popman.ObjectPool;
import zombie.util.ByteBufferPooledObject;

public final class AnimalCell {
    protected static final int SQUARES_PER_CHUNK = 8;
    protected static final int CHUNKS_PER_CELL = 32;
    protected static final int SQUARES_PER_CELL = 256;
    int x;
    int y;
    AnimalChunk[] chunks;
    boolean loaded;
    boolean fileLoaded;
    boolean dataChanged;
    long loadedTime;
    BooleanGrid loadedChunks;
    ArrayList<VirtualAnimal> saveRealAnimalHack;
    ArrayList<IsoAnimal> animalListToReattach;
    boolean addedJunctions;
    static final ObjectPool<AnimalCell> pool = new ObjectPool<>(AnimalCell::new);

    AnimalCell init(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    void save() {
        if (this.isLoaded() && !Core.getInstance().isNoSave()) {
            synchronized (SliceY.SliceBufferLock) {
                String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("apop", "apop_" + this.x + "_" + this.y + ".bin");

                try (
                    FileOutputStream fos = new FileOutputStream(fileName);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                ) {
                    SliceY.SliceBuffer.clear();
                    this.save(SliceY.SliceBuffer);
                    bos.write(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
                } catch (IOException var12) {
                    ExceptionLogger.logException(var12);
                }
            }
        }
    }

    void save(ByteBuffer output) throws IOException {
        output.putInt(240);
        ArrayList<VirtualAnimal> animals = new ArrayList<>();

        for (int i = 0; i < this.chunks.length; i++) {
            if (this.saveRealAnimalHack != null && !this.saveRealAnimalHack.isEmpty()) {
                animals.clear();

                for (int j = 0; j < this.saveRealAnimalHack.size(); j++) {
                    VirtualAnimal animal = this.saveRealAnimalHack.get(j);
                    AnimalChunk chunk = this.getChunkFromSquarePos(PZMath.fastfloor(animal.getX()), PZMath.fastfloor(animal.getY()));
                    if (chunk == this.chunks[i]) {
                        animals.add(animal);
                    }
                }

                this.chunks[i].save(output, animals);
            } else {
                this.chunks[i].save(output);
            }
        }

        if (this.saveRealAnimalHack != null) {
            this.saveRealAnimalHack.clear();
            this.saveRealAnimalHack = null;
        }
    }

    void saveToBufferMap(SaveBufferMap bufferMap) {
        if (this.isLoaded() && !Core.getInstance().isNoSave()) {
            synchronized (SliceY.SliceBufferLock) {
                try {
                    SliceY.SliceBuffer.clear();
                    this.save(SliceY.SliceBuffer);
                    String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("apop", "apop_" + this.x + "_" + this.y + ".bin");
                    ByteBufferPooledObject buffer = bufferMap.allocate(SliceY.SliceBuffer.position());
                    buffer.put(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
                    bufferMap.put(fileName, buffer);
                } catch (IOException var6) {
                    ExceptionLogger.logException(var6);
                }
            }
        }
    }

    void load() {
        assert !this.isLoaded();

        this.loaded = true;
        this.chunks = new AnimalChunk[1024];

        for (int i = 0; i < this.chunks.length; i++) {
            this.chunks[i] = AnimalChunk.alloc().init(this.x * 32 + i % 32, this.y * 32 + i / 32);
            this.chunks[i].cell = this;
        }

        this.checkAnimalZonesGenerated();
        String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("apop", "apop_" + this.x + "_" + this.y + ".bin");
        this.fileLoaded = this.load(fileName);
        if (!this.fileLoaded) {
            AnimalZones.getInstance().spawnAnimalsInCell(this);
        }
    }

    private void checkAnimalZonesGenerated() {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        IsoMetaCell metaCell = IsoWorld.instance.getMetaGrid().getCellData(this.x, this.y);
        if (metaCell == null) {
            metaCell = new IsoMetaCell(this.x, this.y);
            metaGrid.setCellData(this.x, this.y, metaCell);
        }

        int chunkX = this.x * 32;
        int chunkY = this.y * 32;
        metaCell.checkAnimalZonesGenerated(chunkX, chunkY);
    }

    boolean load(String fileName) {
        try {
            boolean var20;
            try (
                FileInputStream fis = new FileInputStream(fileName);
                BufferedInputStream bis = new BufferedInputStream(fis);
            ) {
                ByteBuffer input = SliceY.SliceBuffer;
                input.clear();
                int numBytes = bis.read(input.array());
                input.limit(numBytes);
                this.load(input);
                HashMap<Integer, IsoAnimal> animals = new HashMap<>();

                for (int i = 0; i < this.chunks.length; i++) {
                    AnimalChunk chunk = this.chunks[i];

                    for (int j = 0; j < chunk.animals.size(); j++) {
                        VirtualAnimal virtualAnimal = chunk.animals.get(j);

                        for (int k = 0; k < virtualAnimal.animals.size(); k++) {
                            IsoAnimal animal = virtualAnimal.animals.get(k);
                            animals.put(animal.animalId, animal);
                        }
                    }
                }

                if (this.animalListToReattach != null) {
                    for (int i = 0; i < this.animalListToReattach.size(); i++) {
                        IsoAnimal baby = this.animalListToReattach.get(i);
                        IsoAnimal mother = animals.get(baby.attachBackToMother);
                        if (mother != null) {
                            baby.setMother(mother);
                        }
                    }

                    this.animalListToReattach.clear();
                    this.animalListToReattach = null;
                }

                var20 = true;
            }

            return var20;
        } catch (FileNotFoundException var17) {
            return false;
        } catch (Exception var18) {
            ExceptionLogger.logException(var18);
            return false;
        }
    }

    void load(ByteBuffer input) throws IOException {
        int WorldVersion = input.getInt();

        for (int i = 0; i < this.chunks.length; i++) {
            AnimalChunk chunk = this.chunks[i];
            chunk.cell = this;
            chunk.load(input, WorldVersion);
        }
    }

    void unload() {
        assert this.isLoaded();

        if (this.dataChanged) {
            this.dataChanged = false;
            this.save();
        }

        for (int i = 0; i < this.chunks.length; i++) {
            this.chunks[i].release();
            this.chunks[i] = null;
        }

        this.loaded = false;
    }

    boolean isLoaded() {
        return this.loaded;
    }

    AnimalChunk getChunkFromCellPos(int x, int y) {
        if (!this.isLoaded()) {
            return null;
        } else if (x >= 0 && x < 256 && y >= 0 && y < 256) {
            x /= 8;
            y /= 8;
            return this.chunks[x + y * 32];
        } else {
            return null;
        }
    }

    AnimalChunk getChunkFromChunkPos(int x, int y) {
        if (!this.isLoaded()) {
            return null;
        } else {
            x -= this.x * 32;
            y -= this.y * 32;
            return x >= 0 && x < 32 && y >= 0 && y < 32 ? this.chunks[x + y * 32] : null;
        }
    }

    AnimalChunk getChunkFromSquarePos(int x, int y) {
        x -= this.x * 256;
        y -= this.y * 256;
        return this.getChunkFromCellPos(x, y);
    }

    void setChunkLoaded(int x, int y, boolean loaded) {
        if (loaded || this.loadedChunks != null) {
            if (this.loadedChunks == null) {
                this.loadedChunks = new BooleanGrid(32, 32);
            }

            x -= this.x * 32;
            y -= this.y * 32;
            this.loadedChunks.setValue(x, y, loaded);
        }
    }

    boolean isChunkLoadedChunkPos(int x, int y) {
        if (this.loadedChunks == null) {
            return false;
        } else {
            x -= this.x * 32;
            y -= this.y * 32;
            return this.loadedChunks.getValue(x, y);
        }
    }

    boolean isChunkLoadedWorldPos(int x, int y) {
        if (this.loadedChunks == null) {
            return false;
        } else {
            x -= this.x * 256;
            y -= this.y * 256;
            x /= 8;
            y /= 8;
            return this.loadedChunks.getValue(x, y);
        }
    }

    static AnimalCell alloc() {
        return pool.alloc();
    }

    void release() {
        if (this.isLoaded()) {
            this.unload();
        }

        this.chunks = null;
        this.loaded = false;
        this.fileLoaded = false;
        this.dataChanged = false;
        this.loadedTime = 0L;
        if (this.loadedChunks != null) {
            this.loadedChunks.clear();
            this.loadedChunks = null;
        }

        if (this.saveRealAnimalHack != null) {
            this.saveRealAnimalHack.clear();
            this.saveRealAnimalHack = null;
        }

        if (this.animalListToReattach != null) {
            this.animalListToReattach.clear();
            this.animalListToReattach = null;
        }

        this.addedJunctions = false;
        pool.release(this);
    }
}
