// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import zombie.ChunkMapFilenames;
import zombie.GameWindow;
import zombie.MainThread;
import zombie.SystemDisabler;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.ThreadGroups;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.gameStates.GameLoadingState;
import zombie.network.ChunkChecksum;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.NotRequiredInZipPacket;
import zombie.network.packets.RequestZipListPacket;
import zombie.savefile.PlayerDB;
import zombie.vehicles.VehiclesDB2;

public final class WorldStreamer {
    static final WorldStreamer.ChunkComparator comp = new WorldStreamer.ChunkComparator();
    private static final int CRF_CANCEL = 1;
    public static final int CRF_CANCEL_SENT = 2;
    private static final int CRF_DELETE = 4;
    private static final int CRF_TIMEOUT = 8;
    private static final int CRF_RECEIVED = 16;
    private static final int BLOCK_SIZE = 1024;
    public static WorldStreamer instance = new WorldStreamer();
    private final ConcurrentLinkedQueue<IsoChunk> jobQueue = new ConcurrentLinkedQueue<>();
    private final Stack<IsoChunk> jobList = new Stack<>();
    private final ConcurrentLinkedQueue<IsoChunk> chunkRequests0 = new ConcurrentLinkedQueue<>();
    private final ArrayList<IsoChunk> chunkRequests1 = new ArrayList<>();
    private final ArrayList<WorldStreamer.ChunkRequest> pendingRequests = new ArrayList<>();
    private final ArrayList<WorldStreamer.ChunkRequest> pendingRequests1 = new ArrayList<>();
    private final ConcurrentLinkedQueue<WorldStreamer.ChunkRequest> sentRequests = new ConcurrentLinkedQueue<>();
    private final CRC32 crc32 = new CRC32();
    private final ConcurrentLinkedQueue<ByteBuffer> freeBuffers = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<WorldStreamer.ChunkRequest> waitingToSendQ = new ConcurrentLinkedQueue<>();
    private final ArrayList<WorldStreamer.ChunkRequest> tempRequests = new ArrayList<>();
    private final Inflater decompressor = new Inflater();
    private final byte[] readBuf = new byte[1024];
    private final ConcurrentLinkedQueue<WorldStreamer.ChunkRequest> waitingToCancelQ = new ConcurrentLinkedQueue<>();
    public Thread worldStreamer;
    public boolean finished;
    private IsoChunk chunkHeadMain;
    private int requestNumber;
    private boolean compare;
    private boolean networkFileDebug;
    private ByteBuffer inMemoryZip;
    private boolean requestingLargeArea;
    private volatile int largeAreaDownloads;
    private final ByteBuffer bb1 = ByteBuffer.allocate(5120);
    private final ByteBuffer bb2 = ByteBuffer.allocate(5120);

    private int bufferSize(int size) {
        return (size + 1024 - 1) / 1024 * 1024;
    }

    private ByteBuffer ensureCapacity(ByteBuffer bb, int capacity) {
        if (bb == null) {
            return ByteBuffer.allocate(this.bufferSize(capacity));
        } else if (bb.capacity() < capacity) {
            ByteBuffer newBB = ByteBuffer.allocate(this.bufferSize(capacity));
            return newBB.put(bb.array(), 0, bb.position());
        } else {
            return bb;
        }
    }

    private ByteBuffer getByteBuffer(int capacity) {
        ByteBuffer bb = this.freeBuffers.poll();
        if (bb == null) {
            return ByteBuffer.allocate(this.bufferSize(capacity));
        } else {
            bb.clear();
            return this.ensureCapacity(bb, capacity);
        }
    }

    private void releaseBuffer(ByteBuffer bb) {
        this.freeBuffers.add(bb);
    }

    private void sendRequests() throws IOException {
        if (!this.chunkRequests1.isEmpty()) {
            if (!this.requestingLargeArea || this.pendingRequests1.size() <= 20) {
                long time = System.currentTimeMillis();
                WorldStreamer.ChunkRequest head = null;
                WorldStreamer.ChunkRequest tail = null;

                for (int i = this.chunkRequests1.size() - 1; i >= 0; i--) {
                    IsoChunk chunk = this.chunkRequests1.get(i);
                    WorldStreamer.ChunkRequest request = WorldStreamer.ChunkRequest.alloc();
                    request.chunk = chunk;
                    request.requestNumber = this.requestNumber++;
                    request.time = time;
                    request.crc = ChunkChecksum.getChecksum(chunk.wx, chunk.wy);
                    if (head == null) {
                        head = request;
                    } else {
                        tail.next = request;
                    }

                    request.next = null;
                    tail = request;
                    this.pendingRequests1.add(request);
                    this.chunkRequests1.remove(i);
                    if (this.requestingLargeArea && this.pendingRequests1.size() >= 40) {
                        break;
                    }
                }

                this.waitingToSendQ.add(head);
            }
        }
    }

    public void updateMain() {
        UdpConnection connection = GameClient.connection;
        if (this.chunkHeadMain != null) {
            this.chunkRequests0.add(this.chunkHeadMain);
            this.chunkHeadMain = null;
        }

        this.tempRequests.clear();

        for (WorldStreamer.ChunkRequest request = this.waitingToSendQ.poll(); request != null; request = this.waitingToSendQ.poll()) {
            while (request != null) {
                WorldStreamer.ChunkRequest next = request.next;
                if ((request.flagsWs & 1) != 0) {
                    request.flagsUdp |= 16;
                } else {
                    this.tempRequests.add(request);
                }

                request = next;
            }
        }

        if (!this.tempRequests.isEmpty()) {
            RequestZipListPacket packet = new RequestZipListPacket();
            packet.set(this.tempRequests);
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.RequestZipList.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.RequestZipList.send(connection);
            this.sentRequests.addAll(this.tempRequests);
        }

        this.tempRequests.clear();

        for (WorldStreamer.ChunkRequest request = this.waitingToCancelQ.poll(); request != null; request = this.waitingToCancelQ.poll()) {
            this.tempRequests.add(request);
        }

        if (!this.tempRequests.isEmpty()) {
            NotRequiredInZipPacket packet = new NotRequiredInZipPacket();
            packet.set(this.tempRequests);
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.NotRequiredInZip.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.NotRequiredInZip.send(connection);
        }
    }

    private void loadReceivedChunks() throws DataFormatException, IOException {
        boolean debug = false;
        int nReceived = 0;
        int nCancel = 0;

        for (int i = 0; i < this.pendingRequests1.size(); i++) {
            WorldStreamer.ChunkRequest request = this.pendingRequests1.get(i);
            if ((request.flagsUdp & 16) != 0 && ((request.flagsWs & 1) == 0 || (request.flagsMain & 2) != 0)) {
                this.pendingRequests1.remove(i--);
                ChunkSaveWorker.instance.Update(request.chunk);
                if ((request.flagsUdp & 4) != 0) {
                    File file = ChunkMapFilenames.instance.getFilename(request.chunk.wx, request.chunk.wy);
                    if (file.exists()) {
                        if (this.networkFileDebug) {
                            DebugLog.NetworkFileDebug.debugln("deleting " + file.getAbsolutePath() + " because it doesn't exist on the server");
                        }

                        file.delete();
                        ChunkChecksum.setChecksum(request.chunk.wx, request.chunk.wy, 0L);
                    }
                }

                ByteBuffer requestBB = (request.flagsWs & 1) != 0 ? null : request.bb;
                if (requestBB != null) {
                    try {
                        requestBB = this.decompress(requestBB);
                    } catch (DataFormatException var9) {
                        DebugLog.General
                            .error(
                                "WorldStreamer.loadReceivedChunks: Error while the chunk ("
                                    + request.chunk.wx
                                    + ", "
                                    + request.chunk.wy
                                    + ") was decompressing"
                            );
                        this.chunkRequests1.add(request.chunk);
                        continue;
                    }

                    if (this.compare) {
                        File file = ChunkMapFilenames.instance.getFilename(request.chunk.wx, request.chunk.wy);
                        if (file.exists()) {
                            this.compare(request, requestBB, file);
                        }
                    }
                }

                if ((request.flagsWs & 8) == 0) {
                    if ((request.flagsWs & 1) == 0 && !request.chunk.refs.isEmpty()) {
                        if (requestBB != null) {
                            requestBB.position(0);
                        }

                        this.DoChunk(request.chunk, requestBB);
                    } else {
                        if (this.networkFileDebug) {
                            DebugLog.NetworkFileDebug.debugln(request.chunk.wx + "_" + request.chunk.wy + " refs.isEmpty() SafeWrite=" + (requestBB != null));
                        }

                        if (requestBB != null) {
                            long crc = ChunkChecksum.getChecksumIfExists(request.chunk.wx, request.chunk.wy);
                            this.crc32.reset();
                            this.crc32.update(requestBB.array(), 0, requestBB.position());
                            if (crc != this.crc32.getValue()) {
                                ChunkChecksum.setChecksum(request.chunk.wx, request.chunk.wy, this.crc32.getValue());
                                IsoChunk.SafeWrite(request.chunk.wx, request.chunk.wy, requestBB);
                            }
                        }

                        request.chunk.resetForStore();

                        assert !IsoChunkMap.chunkStore.contains(request.chunk);

                        IsoChunkMap.chunkStore.add(request.chunk);
                    }
                }

                if (request.bb != null) {
                    this.releaseBuffer(request.bb);
                }

                WorldStreamer.ChunkRequest.release(request);
            }
        }
    }

    private ByteBuffer decompress(ByteBuffer bb) throws DataFormatException {
        this.decompressor.reset();
        this.decompressor.setInput(bb.array(), 0, bb.position());
        int position = 0;
        if (this.inMemoryZip != null) {
            this.inMemoryZip.clear();
        }

        while (!this.decompressor.finished()) {
            int count = this.decompressor.inflate(this.readBuf);
            if (count != 0) {
                this.inMemoryZip = this.ensureCapacity(this.inMemoryZip, position + count);
                this.inMemoryZip.put(this.readBuf, 0, count);
                position += count;
            } else if (!this.decompressor.finished()) {
                throw new DataFormatException();
            }
        }

        this.inMemoryZip.limit(this.inMemoryZip.position());
        return this.inMemoryZip;
    }

    private void threadLoop() throws DataFormatException, InterruptedException, IOException {
        if (GameClient.client && !SystemDisabler.doWorldSyncEnable) {
            this.networkFileDebug = DebugType.NetworkFileDebug.isEnabled();

            for (IsoChunk chunk = this.chunkRequests0.poll(); chunk != null; chunk = this.chunkRequests0.poll()) {
                while (chunk != null) {
                    IsoChunk next = chunk.next;
                    this.chunkRequests1.add(chunk);
                    chunk = next;
                }
            }

            if (!this.chunkRequests1.isEmpty()) {
                comp.init();
                Collections.sort(this.chunkRequests1, comp);
                this.sendRequests();
            }

            this.loadReceivedChunks();
            this.cancelOutOfBoundsRequests();
            this.resendTimedOutRequests();
        }

        for (IsoChunk chunk = this.jobQueue.poll(); chunk != null; chunk = this.jobQueue.poll()) {
            if (this.jobList.contains(chunk)) {
                DebugLog.log("Ignoring duplicate chunk added to WorldStreamer.jobList");
            } else {
                this.jobList.add(chunk);
            }
        }

        if (this.jobList.isEmpty()) {
            ChunkSaveWorker.instance.Update(null);
            if (ChunkSaveWorker.instance.saving) {
                return;
            }

            if (!this.pendingRequests1.isEmpty()) {
                Thread.sleep(20L);
                return;
            }

            Thread.sleep(140L);
        } else {
            for (int i = this.jobList.size() - 1; i >= 0; i--) {
                IsoChunk chunkx = this.jobList.get(i);
                if (chunkx.refs.isEmpty()) {
                    this.jobList.remove(i);
                    chunkx.resetForStore();

                    assert !IsoChunkMap.chunkStore.contains(chunkx);

                    IsoChunkMap.chunkStore.add(chunkx);
                }
            }

            boolean busy = !this.jobList.isEmpty();
            IsoChunk chunkx = null;
            if (busy) {
                comp.init();
                Collections.sort(this.jobList, comp);
                chunkx = this.jobList.remove(this.jobList.size() - 1);
            }

            ChunkSaveWorker.instance.Update(chunkx);
            if (chunkx != null) {
                if (chunkx.refs.isEmpty()) {
                    chunkx.resetForStore();

                    assert !IsoChunkMap.chunkStore.contains(chunkx);

                    IsoChunkMap.chunkStore.add(chunkx);
                } else {
                    this.DoChunk(chunkx, null);
                }
            }

            if (busy || ChunkSaveWorker.instance.saving) {
                return;
            }
        }

        if (!GameClient.client && !GameWindow.loadedAsClient && PlayerDB.isAvailable()) {
            PlayerDB.getInstance().updateWorldStreamer();
        }

        VehiclesDB2.instance.updateWorldStreamer();
        if (IsoPlayer.getInstance() != null) {
            Thread.sleep(140L);
        } else {
            Thread.sleep(0L);
        }
    }

    public void create() {
        if (this.worldStreamer == null) {
            if (!GameServer.server) {
                this.finished = false;
                this.worldStreamer = new Thread(ThreadGroups.Workers, () -> {
                    while (!this.finished) {
                        try {
                            this.threadLoop();
                        } catch (Throwable var2) {
                            ExceptionLogger.logException(var2);
                        }
                    }
                });
                this.worldStreamer.setPriority(5);
                this.worldStreamer.setDaemon(true);
                this.worldStreamer.setName("World Streamer");
                this.worldStreamer.setUncaughtExceptionHandler(GameWindow::uncaughtException);
                this.worldStreamer.start();
            }
        }
    }

    public void addJob(IsoChunk chunk, int wx, int wy, boolean bDoServerRequest) {
        if (!GameServer.server) {
            chunk.wx = wx;
            chunk.wy = wy;
            if (GameClient.client && !SystemDisabler.doWorldSyncEnable && bDoServerRequest) {
                chunk.next = this.chunkHeadMain;
                this.chunkHeadMain = chunk;
            } else {
                assert !this.jobQueue.contains(chunk);

                assert !this.jobList.contains(chunk);

                this.jobQueue.add(chunk);
            }
        }
    }

    public void DoChunk(IsoChunk chunk, ByteBuffer fromServer) {
        if (!GameServer.server) {
            this.DoChunkAlways(chunk, fromServer);
        }
    }

    public void DoChunkAlways(IsoChunk chunk, ByteBuffer fromServer) {
        if (Core.debug && DebugOptions.instance.worldStreamerSlowLoad.getValue()) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException var5) {
            }
        }

        if (chunk != null) {
            try {
                if (!chunk.LoadOrCreate(chunk.wx, chunk.wy, fromServer)) {
                    if (GameClient.client) {
                        ChunkChecksum.setChecksum(chunk.wx, chunk.wy, 0L);
                    }

                    chunk.Blam(chunk.wx, chunk.wy);
                    if (!chunk.LoadBrandNew(chunk.wx, chunk.wy)) {
                        return;
                    }
                }

                if (fromServer == null) {
                    VehiclesDB2.instance.loadChunk(chunk);
                }
            } catch (Exception var6) {
                DebugLog.General.error("Exception thrown while trying to load chunk: " + chunk.wx + ", " + chunk.wy);
                ExceptionLogger.logException(var6);
                if (GameClient.client) {
                    ChunkChecksum.setChecksum(chunk.wx, chunk.wy, 0L);
                }

                chunk.Blam(chunk.wx, chunk.wy);
                if (!chunk.LoadBrandNew(chunk.wx, chunk.wy)) {
                    return;
                }
            }

            if (chunk.jobType != IsoChunk.JobType.Convert && chunk.jobType != IsoChunk.JobType.SoftReset) {
                try {
                    if (!chunk.refs.isEmpty()) {
                        chunk.loadInWorldStreamerThread();
                    }
                } catch (Exception var4) {
                    ExceptionLogger.logException(var4);
                }

                IsoChunk.loadGridSquare.add(chunk);
            } else {
                chunk.doLoadGridsquare();
                chunk.loaded = true;
            }
        }
    }

    public void addJobInstant(IsoChunk chunk, int x, int y, int wx, int wy) {
        if (!GameServer.server) {
            chunk.wx = wx;
            chunk.wy = wy;

            try {
                this.DoChunkAlways(chunk, null);
            } catch (Exception var7) {
                var7.printStackTrace();
            }
        }
    }

    public void addJobConvert(IsoChunk chunk, int x, int y, int wx, int wy) {
        if (!GameServer.server) {
            chunk.wx = wx;
            chunk.wy = wy;
            chunk.jobType = IsoChunk.JobType.Convert;

            try {
                this.DoChunk(chunk, null);
            } catch (Exception var7) {
                var7.printStackTrace();
            }
        }
    }

    public void addJobWipe(IsoChunk chunk, int x, int y, int wx, int wy) {
        chunk.wx = wx;
        chunk.wy = wy;
        chunk.jobType = IsoChunk.JobType.SoftReset;

        try {
            this.DoChunkAlways(chunk, null);
        } catch (Exception var7) {
            var7.printStackTrace();
        }
    }

    public boolean isBusy() {
        return !GameClient.client
                || this.chunkRequests0.isEmpty()
                    && this.chunkRequests1.isEmpty()
                    && this.chunkHeadMain == null
                    && this.waitingToSendQ.isEmpty()
                    && this.waitingToCancelQ.isEmpty()
                    && this.sentRequests.isEmpty()
                    && this.pendingRequests.isEmpty()
                    && this.pendingRequests1.isEmpty()
            ? !this.jobQueue.isEmpty() || !this.jobList.isEmpty()
            : true;
    }

    public void stop() {
        DebugType.ExitDebug.debugln("WorldStreamer.stop 1");
        if (this.worldStreamer != null) {
            this.finished = true;
            DebugType.ExitDebug.debugln("WorldStreamer.stop 2");

            while (this.worldStreamer.isAlive()) {
                MainThread.busyWait();
            }

            DebugType.ExitDebug.debugln("WorldStreamer.stop 3");
            this.worldStreamer = null;
            this.jobList.clear();
            this.jobQueue.clear();
            DebugType.ExitDebug.debugln("WorldStreamer.stop 4");
            ChunkSaveWorker.instance.SaveNow();
            ChunkChecksum.Reset();
            DebugType.ExitDebug.debugln("WorldStreamer.stop 5");
        }
    }

    public void quit() {
        this.stop();
    }

    public void requestLargeAreaZip(int wx, int wy, int range) throws IOException {
        INetworkPacket.send(PacketTypes.PacketType.RequestLargeAreaZip, wx, wy);
        this.requestingLargeArea = true;
        this.largeAreaDownloads = 0;
        GameLoadingState.gameLoadingString = Translator.getText("IGUI_MP_RequestMapData");
        int numRequests = 0;
        int minX = wx - range;
        int minY = wy - range;
        int maxX = wx + range;
        int maxY = wy + range;

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (IsoWorld.instance.metaGrid.isValidChunk(x, y)) {
                    IsoChunk chunk = IsoChunkMap.chunkStore.poll();
                    if (chunk == null) {
                        chunk = new IsoChunk(IsoWorld.instance.currentCell);
                    }

                    this.addJob(chunk, x, y, true);
                    numRequests++;
                }
            }
        }

        DebugLog.log("Requested " + numRequests + " chunks from the server");
        long start = System.currentTimeMillis();
        long received = start;
        int seconds = 0;
        int downloads = 0;

        while (this.isBusy()) {
            long now = System.currentTimeMillis();
            if (now - received > 60000L) {
                GameLoadingState.mapDownloadFailed = true;
                throw new IOException("map download from server timed out");
            }

            int largeAreaDownloads = this.largeAreaDownloads;
            GameLoadingState.gameLoadingString = Translator.getText("IGUI_MP_DownloadedMapData", largeAreaDownloads, numRequests);
            long elapsed = now - start;
            if (elapsed / 1000L > seconds) {
                DebugLog.log("Received " + largeAreaDownloads + " / " + numRequests + " chunks");
                seconds = (int)(elapsed / 1000L);
            }

            if (downloads < largeAreaDownloads) {
                received = now;
                downloads = largeAreaDownloads;
            }

            try {
                Thread.sleep(100L);
            } catch (InterruptedException var21) {
            }
        }

        DebugLog.log("Received " + this.largeAreaDownloads + " / " + numRequests + " chunks");
        this.requestingLargeArea = false;
    }

    private void cancelOutOfBoundsRequests() {
        if (!this.requestingLargeArea) {
            for (int i = 0; i < this.pendingRequests1.size(); i++) {
                WorldStreamer.ChunkRequest request = this.pendingRequests1.get(i);
                if ((request.flagsWs & 1) == 0 && request.chunk.refs.isEmpty()) {
                    request.flagsWs |= 1;
                    this.waitingToCancelQ.add(request);
                }
            }
        }
    }

    private void resendTimedOutRequests() {
        long time = System.currentTimeMillis();

        for (int i = 0; i < this.pendingRequests1.size(); i++) {
            WorldStreamer.ChunkRequest request = this.pendingRequests1.get(i);
            if ((request.flagsWs & 1) == 0 && request.time + 8000L < time) {
                if (this.networkFileDebug) {
                    DebugLog.NetworkFileDebug.debugln("chunk request timed out " + request.chunk.wx + "," + request.chunk.wy);
                }

                this.chunkRequests1.add(request.chunk);
                request.flagsWs |= 9;
                request.flagsMain |= 2;
            }
        }
    }

    public void receiveChunkPart(ByteBuffer bb) {
        for (WorldStreamer.ChunkRequest request = this.sentRequests.poll(); request != null; request = this.sentRequests.poll()) {
            this.pendingRequests.add(request);
        }

        int requestNumber = bb.getInt();
        int numChunks = bb.getInt();
        int chunkIndex = bb.getInt();
        int fileSize = bb.getInt();
        int offset = bb.getInt();
        int count = bb.getInt();

        for (int i = 0; i < this.pendingRequests.size(); i++) {
            WorldStreamer.ChunkRequest request = this.pendingRequests.get(i);
            if ((request.flagsWs & 1) != 0) {
                this.pendingRequests.remove(i--);
                request.flagsUdp |= 16;
            } else if (request.requestNumber == requestNumber) {
                if (request.bb == null) {
                    request.bb = this.getByteBuffer(fileSize);
                }

                System.arraycopy(bb.array(), bb.position(), request.bb.array(), offset, count);
                if (request.partsReceived == null) {
                    request.partsReceived = new boolean[numChunks];
                }

                request.partsReceived[chunkIndex] = true;
                if (request.isReceived()) {
                    if (this.networkFileDebug) {
                        DebugLog.NetworkFileDebug.debugln("received all parts for " + request.chunk.wx + "," + request.chunk.wy);
                    }

                    request.bb.position(fileSize);
                    this.pendingRequests.remove(i);
                    request.flagsUdp |= 16;
                    if (this.requestingLargeArea) {
                        this.largeAreaDownloads++;
                    }
                }
                break;
            }
        }
    }

    public void receiveNotRequired(ByteBuffer bb) {
        for (WorldStreamer.ChunkRequest request = this.sentRequests.poll(); request != null; request = this.sentRequests.poll()) {
            this.pendingRequests.add(request);
        }

        int count = bb.getInt();

        for (int n = 0; n < count; n++) {
            int requestNumber = bb.getInt();
            boolean sameOnServer = bb.get() == 1;

            for (int i = 0; i < this.pendingRequests.size(); i++) {
                WorldStreamer.ChunkRequest request = this.pendingRequests.get(i);
                if ((request.flagsWs & 1) != 0) {
                    this.pendingRequests.remove(i--);
                    request.flagsUdp |= 16;
                } else if (request.requestNumber == requestNumber) {
                    if (this.networkFileDebug) {
                        DebugLog.NetworkFileDebug.debugln("NotRequiredInZip " + request.chunk.wx + "," + request.chunk.wy + " delete=" + !sameOnServer);
                    }

                    if (!sameOnServer) {
                        request.flagsUdp |= 4;
                    }

                    this.pendingRequests.remove(i);
                    request.flagsUdp |= 16;
                    if (this.requestingLargeArea) {
                        this.largeAreaDownloads++;
                    }
                    break;
                }
            }
        }
    }

    private void compare(WorldStreamer.ChunkRequest request, ByteBuffer requestBB, File file) throws IOException {
        IsoChunk chunkDownloaded = IsoChunkMap.chunkStore.poll();
        if (chunkDownloaded == null) {
            chunkDownloaded = new IsoChunk(IsoWorld.instance.getCell());
        }

        chunkDownloaded.wx = request.chunk.wx;
        chunkDownloaded.wy = request.chunk.wy;
        IsoChunk chunkOnDisk = IsoChunkMap.chunkStore.poll();
        if (chunkOnDisk == null) {
            chunkOnDisk = new IsoChunk(IsoWorld.instance.getCell());
        }

        chunkOnDisk.wx = request.chunk.wx;
        chunkOnDisk.wy = request.chunk.wy;
        int position = requestBB.position();
        requestBB.position(0);
        chunkDownloaded.LoadFromBuffer(request.chunk.wx, request.chunk.wy, requestBB);
        requestBB.position(position);
        this.crc32.reset();
        this.crc32.update(requestBB.array(), 0, position);
        DebugLog.log("downloaded crc=" + this.crc32.getValue() + " on-disk crc=" + ChunkChecksum.getChecksumIfExists(request.chunk.wx, request.chunk.wy));
        chunkOnDisk.LoadFromDisk();
        DebugLog.log("downloaded size=" + position + " on-disk size=" + file.length());
        this.compareChunks(chunkDownloaded, chunkOnDisk);
        chunkDownloaded.resetForStore();

        assert !IsoChunkMap.chunkStore.contains(chunkDownloaded);

        IsoChunkMap.chunkStore.add(chunkDownloaded);
        chunkOnDisk.resetForStore();

        assert !IsoChunkMap.chunkStore.contains(chunkOnDisk);

        IsoChunkMap.chunkStore.add(chunkOnDisk);
    }

    private void compareChunks(IsoChunk chunk1, IsoChunk chunk2) {
        DebugLog.log("comparing " + chunk1.wx + "," + chunk1.wy);

        try {
            this.compareErosion(chunk1, chunk2);
            if (chunk1.lootRespawnHour != chunk2.lootRespawnHour) {
                DebugLog.log("lootRespawnHour " + chunk1.lootRespawnHour + " != " + chunk2.lootRespawnHour);
            }

            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    IsoGridSquare sq1 = chunk1.getGridSquare(x, y, 0);
                    IsoGridSquare sq2 = chunk2.getGridSquare(x, y, 0);
                    this.compareSquares(sq1, sq2);
                }
            }
        } catch (Exception var7) {
            var7.printStackTrace();
        }
    }

    private void compareErosion(IsoChunk chunk1, IsoChunk chunk2) {
        if (chunk1.getErosionData().init != chunk2.getErosionData().init) {
            DebugLog.log("init " + chunk1.getErosionData().init + " != " + chunk2.getErosionData().init);
        }

        if (chunk1.getErosionData().eTickStamp != chunk2.getErosionData().eTickStamp) {
            DebugLog.log("eTickStamp " + chunk1.getErosionData().eTickStamp + " != " + chunk2.getErosionData().eTickStamp);
        }

        if (chunk1.getErosionData().moisture != chunk2.getErosionData().moisture) {
            DebugLog.log("moisture " + chunk1.getErosionData().moisture + " != " + chunk2.getErosionData().moisture);
        }

        if (chunk1.getErosionData().minerals != chunk2.getErosionData().minerals) {
            DebugLog.log("minerals " + chunk1.getErosionData().minerals + " != " + chunk2.getErosionData().minerals);
        }

        if (chunk1.getErosionData().epoch != chunk2.getErosionData().epoch) {
            DebugLog.log("epoch " + chunk1.getErosionData().epoch + " != " + chunk2.getErosionData().epoch);
        }

        if (chunk1.getErosionData().soil != chunk2.getErosionData().soil) {
            DebugLog.log("soil " + chunk1.getErosionData().soil + " != " + chunk2.getErosionData().soil);
        }
    }

    private void compareSquares(IsoGridSquare sq1, IsoGridSquare sq2) {
        if (sq1 != null && sq2 != null) {
            try {
                this.bb1.clear();
                sq1.save(this.bb1, null);
                this.bb1.flip();
                this.bb2.clear();
                sq2.save(this.bb2, null);
                this.bb2.flip();
                if (this.bb1.compareTo(this.bb2) != 0) {
                    boolean seasonMatch = true;
                    int j = -1;
                    if (this.bb1.limit() == this.bb2.limit()) {
                        for (int i = 0; i < this.bb1.limit(); i++) {
                            if (this.bb1.get(i) != this.bb2.get(i)) {
                                j = i;
                                break;
                            }
                        }

                        for (int r = 0; r < sq1.getErosionData().regions.size(); r++) {
                            if (sq1.getErosionData().regions.get(r).dispSeason != sq2.getErosionData().regions.get(r).dispSeason) {
                                DebugLog.log(
                                    "season1=" + sq1.getErosionData().regions.get(r).dispSeason + " season2=" + sq2.getErosionData().regions.get(r).dispSeason
                                );
                                seasonMatch = false;
                            }
                        }
                    }

                    DebugLog.log(
                        "square "
                            + sq1.x
                            + ","
                            + sq1.y
                            + " mismatch at "
                            + j
                            + " seasonMatch="
                            + seasonMatch
                            + " #regions="
                            + sq1.getErosionData().regions.size()
                    );
                    if (sq1.getObjects().size() == sq2.getObjects().size()) {
                        for (int ix = 0; ix < sq1.getObjects().size(); ix++) {
                            IsoObject obj1 = sq1.getObjects().get(ix);
                            IsoObject obj2 = sq2.getObjects().get(ix);
                            this.bb1.clear();
                            obj1.save(this.bb1);
                            this.bb1.flip();
                            this.bb2.clear();
                            obj2.save(this.bb2);
                            this.bb2.flip();
                            if (this.bb1.compareTo(this.bb2) != 0) {
                                DebugLog.log(
                                    "  1: " + obj1.getClass().getName() + " " + obj1.getName() + " " + (obj1.sprite == null ? "no sprite" : obj1.sprite.name)
                                );
                                DebugLog.log(
                                    "  2: " + obj2.getClass().getName() + " " + obj2.getName() + " " + (obj2.sprite == null ? "no sprite" : obj2.sprite.name)
                                );
                            }
                        }
                    } else {
                        for (int ixx = 0; ixx < sq1.getObjects().size(); ixx++) {
                            IsoObject obj = sq1.getObjects().get(ixx);
                            DebugLog.log("  " + obj.getClass().getName() + " " + obj.getName() + " " + (obj.sprite == null ? "no sprite" : obj.sprite.name));
                        }
                    }
                }
            } catch (Exception var8) {
                var8.printStackTrace();
            }
        } else {
            if (sq1 != null || sq2 != null) {
                DebugLog.log("one square is null, the other isn't");
            }
        }
    }

    private static class ChunkComparator implements Comparator<IsoChunk> {
        private final Vector2[] pos = new Vector2[4];

        public ChunkComparator() {
            for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                this.pos[playerIndex] = new Vector2();
            }
        }

        public void init() {
            for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                Vector2 pos = this.pos[playerIndex];
                pos.x = pos.y = -1.0F;
                IsoPlayer player = IsoPlayer.players[playerIndex];
                if (player != null) {
                    if (player.getLastX() == player.getX() && player.getLastY() == player.getY()) {
                        pos.x = player.getX();
                        pos.y = player.getY();
                    } else {
                        pos.x = player.getX() - player.getLastX();
                        pos.y = player.getY() - player.getLastY();
                        pos.normalize();
                        pos.setLength(10.0F);
                        pos.x = pos.x + player.getX();
                        pos.y = pos.y + player.getY();
                    }
                }
            }
        }

        public int compare(IsoChunk a, IsoChunk b) {
            int CPW = 8;
            float aScore = Float.MAX_VALUE;
            float bScore = Float.MAX_VALUE;

            for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                if (this.pos[playerIndex].x != -1.0F || this.pos[playerIndex].y != -1.0F) {
                    float x = this.pos[playerIndex].x;
                    float y = this.pos[playerIndex].y;
                    aScore = Math.min(aScore, IsoUtils.DistanceToSquared(x, y, a.wx * 8 + 4.0F, a.wy * 8 + 4.0F));
                    bScore = Math.min(bScore, IsoUtils.DistanceToSquared(x, y, b.wx * 8 + 4.0F, b.wy * 8 + 4.0F));
                }
            }

            if (aScore < bScore) {
                return 1;
            } else {
                return aScore > bScore ? -1 : 0;
            }
        }
    }

    public static final class ChunkRequest {
        static final ArrayDeque<WorldStreamer.ChunkRequest> pool = new ArrayDeque<>();
        public IsoChunk chunk;
        public int requestNumber;
        boolean[] partsReceived;
        public long crc;
        ByteBuffer bb;
        public transient int flagsMain;
        transient int flagsUdp;
        transient int flagsWs;
        long time;
        WorldStreamer.ChunkRequest next;

        boolean isReceived() {
            if (this.partsReceived == null) {
                return false;
            } else {
                for (int i = 0; i < this.partsReceived.length; i++) {
                    if (!this.partsReceived[i]) {
                        return false;
                    }
                }

                return true;
            }
        }

        static WorldStreamer.ChunkRequest alloc() {
            return pool.isEmpty() ? new WorldStreamer.ChunkRequest() : pool.pop();
        }

        static void release(WorldStreamer.ChunkRequest request) {
            request.chunk = null;
            request.partsReceived = null;
            request.bb = null;
            request.flagsMain = 0;
            request.flagsUdp = 0;
            request.flagsWs = 0;
            pool.push(request);
        }
    }
}
