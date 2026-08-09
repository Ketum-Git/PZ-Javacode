// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import java.util.Arrays;
import java.util.List;
import zombie.debug.DebugType;
import zombie.util.Pool;

public class LiveAnimationTrackEntries {
    private int totalAnimBlendCount;
    private final int animBlendIndexCacheSize = 32;
    private final LiveAnimationTrackEntry[] liveAnimTrackEntries = new LiveAnimationTrackEntry[32];
    private final int maxLayers = 16;
    private final int[] layerBlendCounts = new int[16];
    private final float[] layerWeightTotals = new float[16];

    public void clear() {
        this.totalAnimBlendCount = 0;
        clear(this.liveAnimTrackEntries);
        Arrays.fill(this.layerBlendCounts, 0);
        Arrays.fill(this.layerWeightTotals, 0.0F);
    }

    public void setTracks(List<AnimationTrack> tracks, float minimumValidAnimWeight, boolean normalizeFirstLayerTracks) {
        int tracksCount = tracks.size();
        this.clear();

        for (int trackIdx = 0; trackIdx < tracksCount; trackIdx++) {
            AnimationTrack track = tracks.get(trackIdx);
            float trackWeight = track.getBlendWeight();
            int trackLayer = track.getLayerIdx();
            int trackPriority = track.getPriority();
            if (trackLayer >= 0 && trackLayer < 16) {
                if (!(trackWeight < minimumValidAnimWeight) && (trackLayer <= 0 || !track.isFinished())) {
                    int insertAt = -1;

                    for (int i = 0; i < this.liveAnimTrackEntries.length; i++) {
                        LiveAnimationTrackEntry trackEntry = this.liveAnimTrackEntries[i];
                        if (trackEntry == null) {
                            insertAt = i;
                            break;
                        }

                        if (trackLayer <= trackEntry.getLayer()) {
                            if (trackLayer < trackEntry.getLayer()) {
                                insertAt = i;
                                break;
                            }

                            if (trackPriority <= trackEntry.getPriority()) {
                                if (trackPriority < trackEntry.getPriority()) {
                                    insertAt = i;
                                    break;
                                }

                                if (trackWeight < trackEntry.getBlendWeight()) {
                                    insertAt = i;
                                    break;
                                }
                            }
                        }
                    }

                    if (insertAt < 0) {
                        DebugType.General
                            .error(
                                "Buffer overflow. Insufficient anim blends in cache. More than %d animations are being blended at once. Will be truncated to %d.",
                                this.liveAnimTrackEntries.length,
                                this.liveAnimTrackEntries.length
                            );
                    } else {
                        insertAt(this.liveAnimTrackEntries, track, insertAt);
                    }
                }
            } else {
                DebugType.General.error("Layer index is out of range: %d. Range: 0 - %d", trackLayer, 15);
            }
        }

        for (int i = 0; i < this.liveAnimTrackEntries.length; i++) {
            LiveAnimationTrackEntry trackEntryx = this.liveAnimTrackEntries[i];
            if (trackEntryx == null) {
                break;
            }

            int layerIdx = trackEntryx.getLayer();
            this.layerWeightTotals[layerIdx] = this.layerWeightTotals[layerIdx] + trackEntryx.getBlendWeight();
            this.layerBlendCounts[layerIdx]++;
            this.totalAnimBlendCount++;
        }

        if (this.totalAnimBlendCount != 0) {
            if (normalizeFirstLayerTracks) {
                int layerIdx = 0;
                int layerNo = this.liveAnimTrackEntries[0].getLayer();
                int layerTrackCount = this.layerBlendCounts[0];
                float layerTotalWeight = this.layerWeightTotals[0];
                if (layerTotalWeight < 1.0F) {
                    for (int i = 0; i < this.totalAnimBlendCount; i++) {
                        LiveAnimationTrackEntry trackEntryx = this.liveAnimTrackEntries[i];
                        int layer = trackEntryx.getLayer();
                        if (layer != layerNo) {
                            break;
                        }

                        if (layerTotalWeight > 0.0F) {
                            trackEntryx.setBlendWeight(trackEntryx.getBlendWeight() / layerTotalWeight);
                        } else {
                            trackEntryx.setBlendWeight(1.0F / layerTrackCount);
                        }
                    }
                }
            }
        }
    }

    private static void insertAt(LiveAnimationTrackEntry[] liveAnimTrackEntries, AnimationTrack track, int insertAt) {
        LiveAnimationTrackEntry newEntry = LiveAnimationTrackEntry.alloc(track);
        if (liveAnimTrackEntries[insertAt] == null) {
            liveAnimTrackEntries[insertAt] = newEntry;
        } else {
            LiveAnimationTrackEntry previousEntry = newEntry;

            for (int i = insertAt; i < liveAnimTrackEntries.length; i++) {
                LiveAnimationTrackEntry currentEntry = liveAnimTrackEntries[i];
                liveAnimTrackEntries[i] = previousEntry;
                previousEntry = currentEntry;
                if (currentEntry == null) {
                    break;
                }
            }

            Pool.tryRelease(previousEntry);
        }
    }

    private static void clear(LiveAnimationTrackEntry[] array) {
        for (int i = 0; i < array.length; i++) {
            Pool.tryRelease(array[i]);
            array[i] = null;
        }
    }

    public int count() {
        return this.totalAnimBlendCount;
    }

    public LiveAnimationTrackEntry get(int trackIdx) {
        return this.liveAnimTrackEntries[trackIdx];
    }
}
