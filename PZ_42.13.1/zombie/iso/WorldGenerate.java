// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.CRC32;
import zombie.ChunkMapFilenames;
import zombie.ZomboidFileSystem;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.ChunkChecksum;
import zombie.network.GameServer;
import zombie.network.chat.ChatServer;
import zombie.network.server.EventManager;

public class WorldGenerate {
    public static final WorldGenerate instance = new WorldGenerate();
    int minX;
    int minY;
    int width;
    int height;
    private static final UpdateLimit printLimiter = new UpdateLimit(300000L);
    private final WorldGenerate.Status status = new WorldGenerate.Status();
    private String worldName = "";
    private Thread[] threads;
    private boolean quit;
    private final ArrayList<WorldGenerate.MapCell> mapCellArrayList = new ArrayList<>();

    public void init(String worldName) {
        this.quit = false;
        this.worldName = worldName;
        GameServer.close();
        this.status.startTime = System.currentTimeMillis();
        this.initInternal(true);
        this.threads = new Thread[this.status.threadCount];

        for (int i = 0; i < this.status.threadCount; i++) {
            this.threads[i] = new WorldGenerate.WorldGenerateThread(i);
            this.threads[i].start();
        }

        this.waitStop();
    }

    public void recheck(String worldName) {
        this.quit = false;
        this.worldName = worldName;
        GameServer.close();
        this.status.startTime = System.currentTimeMillis();
        this.initInternal(false);
        this.threads = new Thread[this.status.threadCount];

        for (int i = 0; i < this.status.threadCount; i++) {
            this.threads[i] = new WorldGenerate.WorldGenerateThread(i);
            this.threads[i].start();
        }

        this.waitStop();
    }

    private void waitStop() {
        for (int i = 0; i < this.threads.length; i++) {
            if (this.threads[i] != null) {
                while (this.threads[i] != null && this.threads[i].isAlive()) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException var4) {
                    }
                }
            }
        }

        GameServer.open();
        long time = (System.currentTimeMillis() - this.status.startTime) / 1000L;
        String message = "World Generate: " + this.status.allChunks + " chunks were generated on " + time + " seconds";
        DebugLog.General.println(message);
        ChatServer.getInstance().sendMessageToAdminChat(message);
        EventManager.instance().report("[SERVER] " + message);
    }

    public void stop() {
        this.quit = true;

        for (int i = 0; i < this.threads.length; i++) {
            if (this.threads[i] != null) {
                while (this.threads[i].isAlive()) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException var3) {
                    }
                }

                this.threads[i] = null;
            }
        }

        GameServer.open();
    }

    public WorldGenerate.Status getStatus() {
        return this.status;
    }

    private void initInternal(boolean checkMetacell) {
        this.mapCellArrayList.clear();
        this.minX = IsoWorld.instance.metaGrid.minNonProceduralX;
        this.minY = IsoWorld.instance.metaGrid.minNonProceduralY;
        this.width = IsoWorld.instance.metaGrid.maxNonProceduralX - this.minX + 1;
        this.height = IsoWorld.instance.metaGrid.maxNonProceduralY - this.minY + 1;
        File fo = new File(ZomboidFileSystem.instance.getGameModeCacheDir() + File.separator + this.worldName + File.separator + "metagrid" + File.separator);
        if (!fo.exists()) {
            fo.mkdir();
        }

        HashSet<String> mapNames = new HashSet<>(Arrays.stream(fo.list()).toList());

        for (int cy = this.minY; cy < this.minY + this.height; cy++) {
            for (int cx = this.minX; cx < this.minX + this.width; cx++) {
                if (!checkMetacell || !mapNames.contains("metacell_" + cx + "_" + cy + ".bin")) {
                    String filenamepack = "world_" + cx + "_" + cy + ".lotpack";
                    if (IsoLot.InfoFileNames.containsKey(filenamepack)) {
                        File fo2 = new File(IsoLot.InfoFileNames.get(filenamepack));
                        if (fo2.exists()) {
                            WorldGenerate.MapCell mapCellEntity = new WorldGenerate.MapCell();
                            mapCellEntity.cx = cx;
                            mapCellEntity.cy = cy;
                            this.mapCellArrayList.add(mapCellEntity);
                        }
                    }
                }
            }
        }

        mapNames.clear();
        this.status.allChunks = this.mapCellArrayList.size() * 32 * 32;
        String message = "World Generate: Generating map started. Chunks:" + this.status.allChunks + " using " + this.status.threadCount + " threads";
        DebugLog.General.println(message);
        ChatServer.getInstance().sendMessageToAdminChat(message);
        EventManager.instance().report("[SERVER] " + message);
    }

    private void processInternal(int tid) {
        ByteBuffer SliceBuffer = ByteBuffer.allocate(65536);
        CRC32 crcSave = new CRC32();
        WorldStreamer.instance.create();
        int len = (int)Math.ceil((double)this.mapCellArrayList.size() / this.status.threadCount);

        for (int i = tid * len; i < (tid + 1) * len && i < this.mapCellArrayList.size(); i++) {
            WorldGenerate.MapCell mapCell = this.mapCellArrayList.get(i);

            for (int wx = mapCell.cx * 32; wx < (mapCell.cx + 1) * 32; wx++) {
                for (int wy = mapCell.cy * 32; wy < (mapCell.cy + 1) * 32; wy++) {
                    File inFile = ChunkMapFilenames.instance.getFilename(wx, wy);
                    if (inFile.exists()) {
                        this.status.generatedChunks++;
                    } else {
                        IsoChunk chunk = IsoChunkMap.chunkStore.poll();
                        if (chunk == null) {
                            chunk = new IsoChunk((IsoCell)null);
                        }

                        try {
                            if (chunk.LoadOrCreate(wx, wy, null)) {
                                chunk.loaded = true;
                            } else {
                                ChunkChecksum.setChecksum(wx, wy, 0L);
                                chunk.Blam(wx, wy);
                                if (chunk.LoadBrandNew(wx, wy)) {
                                    chunk.loaded = true;
                                }
                            }
                        } catch (Exception var13) {
                            DebugLog.General.printException(var13, "Error load chunk on coordinates " + wx + "," + wy, LogSeverity.Error);
                        }

                        try {
                            if (this.status.threadCount == 1) {
                                chunk.Save(true);
                            } else {
                                SliceBuffer = chunk.Save(SliceBuffer, crcSave, false);
                                IsoChunk.SafeWrite(wx, wy, SliceBuffer);
                            }
                        } catch (IOException var12) {
                            DebugLog.General.printException(var12, "Error save chunk on coordinates " + wx + "," + wy, LogSeverity.Error);
                        }

                        chunk.resetForStore();
                        IsoChunkMap.chunkStore.add(chunk);
                        this.status.generatedChunks++;
                        if (this.quit) {
                            break;
                        }
                    }
                }

                if (this.quit) {
                    break;
                }
            }

            if (printLimiter.Check()) {
                long speed = (this.status.generatedChunks - this.status.lastGeneratedChunks) / (printLimiter.getDelay() / 1000L);
                this.status.lastGeneratedChunks = this.status.generatedChunks;
                String message = "World Generate: Thread "
                    + tid
                    + " Generating map: "
                    + this.status.generatedChunks
                    + "/"
                    + this.status.allChunks
                    + " ETA:"
                    + (this.status.allChunks - this.status.generatedChunks) / speed
                    + " sec";
                DebugLog.General.println(message);
                ChatServer.getInstance().sendMessageToAdminChat(message);
                EventManager.instance().report("[SERVER] " + message);
            }

            if (this.quit) {
                break;
            }
        }

        String message = "World Generate: Thread " + tid + " Finished";
        DebugLog.General.println(message);
        ChatServer.getInstance().sendMessageToAdminChat(message);
        EventManager.instance().report("[SERVER] " + message);
    }

    public static class MapCell {
        int priority;
        int cx;
        int cy;
    }

    public static class Status {
        boolean isRun;
        boolean isFast;
        boolean highPriorityGenerated;
        boolean lowPriorityGenerated;
        public int allChunks;
        public int generatedChunks;
        public int lastGeneratedChunks;
        public int threadCount = 1;
        public long startTime;

        public Status() {
            this.isRun = false;
            this.isFast = false;
            this.highPriorityGenerated = false;
            this.lowPriorityGenerated = false;
            this.allChunks = 0;
            this.generatedChunks = 0;
            this.lastGeneratedChunks = 0;
            this.threadCount = 1;
            this.startTime = System.currentTimeMillis();
        }
    }

    static class WorldGenerateThread extends Thread {
        private int tid;
        public boolean isRun;

        public WorldGenerateThread(int tid) {
            this.tid = tid;
            this.setName("WorldGenerate_" + tid);
        }

        @Override
        public void run() {
            this.isRun = true;
            WorldGenerate.instance.processInternal(this.tid);
            this.isRun = false;
        }
    }
}
