// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.ThreadGroups;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.iso.objects.IsoTree;

/**
 * Created by LEMMY on 07/05/2017.
 */
public final class WorldReuserThread {
    public static final WorldReuserThread instance = new WorldReuserThread();
    private final ArrayList<IsoObject> objectsToReuse = new ArrayList<>();
    private final ArrayList<IsoTree> treesToReuse = new ArrayList<>();
    public boolean finished;
    private Thread worldReuser;
    private final LinkedBlockingQueue<IsoChunk> reuseGridSquares = new LinkedBlockingQueue<>();
    private final IsoChunk finishLoop = new IsoChunk((WorldReuserThread)null);

    public void run() {
        this.worldReuser = new Thread(ThreadGroups.Workers, () -> {
            while (!this.finished) {
                this.testReuseChunk();
                this.reconcileReuseObjects();
            }
        });
        this.worldReuser.setName("WorldReuser");
        this.worldReuser.setDaemon(true);
        this.worldReuser.setUncaughtExceptionHandler(GameWindow::uncaughtException);
        this.worldReuser.start();
    }

    public void stop() {
        this.reuseGridSquares.add(this.finishLoop);

        while (!this.finished) {
            Thread.onSpinWait();

            try {
                Thread.sleep(20L);
            } catch (InterruptedException var2) {
                throw new RuntimeException(var2);
            }
        }
    }

    public void reconcileReuseObjects() {
        if (!this.objectsToReuse.isEmpty()) {
            if (CellLoader.isoObjectCache.size() < 320000) {
                CellLoader.isoObjectCache.push(this.objectsToReuse);
            }

            this.objectsToReuse.clear();
        }

        if (!this.treesToReuse.isEmpty()) {
            if (CellLoader.isoTreeCache.size() < 40000) {
                CellLoader.isoTreeCache.push(this.treesToReuse);
            }

            this.treesToReuse.clear();
        }
    }

    private void testReuseChunk() {
        try {
            for (IsoChunk chunk = this.reuseGridSquares.take(); chunk != null; chunk = this.reuseGridSquares.take()) {
                if (chunk == this.finishLoop) {
                    this.finished = true;
                    return;
                }

                if (Core.debug) {
                    if (ChunkSaveWorker.instance.toSaveQueue.contains(chunk)) {
                        DebugLog.log("ERROR: reusing chunk that needs to be saved");
                    }

                    if (IsoChunkMap.chunkStore.contains(chunk)) {
                        DebugLog.log("ERROR: reusing chunk in chunkStore");
                    }

                    if (!chunk.refs.isEmpty()) {
                        DebugLog.log("ERROR: reusing chunk with refs");
                    }
                }

                if (Core.debug) {
                }

                this.reuseGridSquares(chunk);
                if (this.treesToReuse.size() > 1000 || this.objectsToReuse.size() > 5000) {
                    this.reconcileReuseObjects();
                }
            }
        } catch (Throwable var2) {
            ExceptionLogger.logException(var2);
        }
    }

    public void addReuseChunk(IsoChunk chunk) {
        this.reuseGridSquares.add(chunk);
    }

    private void reuseGridSquares(IsoChunk chunk) {
        int to = 64;

        for (int n = chunk.minLevel; n <= chunk.maxLevel; n++) {
            for (int m = 0; m < 64; m++) {
                int squaresIndexOfLevel = chunk.squaresIndexOfLevel(n);
                IsoGridSquare sq = chunk.squares[squaresIndexOfLevel][m];
                if (sq != null) {
                    for (int a = 0; a < sq.getObjects().size(); a++) {
                        IsoObject o = sq.getObjects().get(a);
                        if (o instanceof IsoTree isoTree) {
                            o.reset();
                            synchronized (this.treesToReuse) {
                                this.treesToReuse.add(isoTree);
                            }
                        } else if (o.getClass() == IsoObject.class) {
                            o.reset();
                            synchronized (this.objectsToReuse) {
                                this.objectsToReuse.add(o);
                            }
                        } else {
                            o.reuseGridSquare();
                        }
                    }

                    sq.discard();
                    chunk.squares[squaresIndexOfLevel][m] = null;
                }
            }
        }

        chunk.resetForStore();
        IsoChunkMap.chunkStore.add(chunk);
    }
}
