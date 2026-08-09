// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion.jobs;

import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;

public final class RegionJobManager {
    private static final ConcurrentLinkedQueue<JobSquareUpdate> poolSquareUpdate = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<JobChunkUpdate> poolChunkUpdate = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<JobApplyChanges> poolApplyChanges = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<JobServerSendFullData> poolServerSendFullData = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<JobDebugResetAllData> poolDebugResetAllData = new ConcurrentLinkedQueue<>();

    public static JobSquareUpdate allocSquareUpdate(int x, int y, int z, byte flags) {
        JobSquareUpdate j = poolSquareUpdate.poll();
        if (j == null) {
            j = new JobSquareUpdate();
        }

        j.worldSquareX = x;
        j.worldSquareY = y;
        j.worldSquareZ = z;
        j.newSquareFlags = flags;
        return j;
    }

    public static JobChunkUpdate allocChunkUpdate() {
        JobChunkUpdate j = poolChunkUpdate.poll();
        if (j == null) {
            j = new JobChunkUpdate();
        }

        return j;
    }

    public static JobApplyChanges allocApplyChanges(boolean saveToDisk) {
        JobApplyChanges j = poolApplyChanges.poll();
        if (j == null) {
            j = new JobApplyChanges();
        }

        j.saveToDisk = saveToDisk;
        return j;
    }

    public static JobServerSendFullData allocServerSendFullData(UdpConnection conn) {
        JobServerSendFullData j = poolServerSendFullData.poll();
        if (j == null) {
            j = new JobServerSendFullData();
        }

        j.targetConn = conn;
        return j;
    }

    public static JobDebugResetAllData allocDebugResetAllData() {
        JobDebugResetAllData j = poolDebugResetAllData.poll();
        if (j == null) {
            j = new JobDebugResetAllData();
        }

        return j;
    }

    public static void release(RegionJob job) {
        job.reset();
        switch (job.getJobType()) {
            case SquareUpdate:
                poolSquareUpdate.add((JobSquareUpdate)job);
                break;
            case ApplyChanges:
                poolApplyChanges.add((JobApplyChanges)job);
                break;
            case ChunkUpdate:
                poolChunkUpdate.add((JobChunkUpdate)job);
                break;
            case ServerSendFullData:
                poolServerSendFullData.add((JobServerSendFullData)job);
                break;
            case DebugResetAllData:
                poolDebugResetAllData.add((JobDebugResetAllData)job);
                break;
            default:
                if (Core.debug) {
                    throw new RuntimeException("No pooling for this job type?");
                }
        }
    }
}
