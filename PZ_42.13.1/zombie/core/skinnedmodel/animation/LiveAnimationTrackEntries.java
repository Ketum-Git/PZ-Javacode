// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation;

import java.util.Arrays;
import java.util.List;
import zombie.debug.DebugLog;
import zombie.util.Pool;

public class LiveAnimationTrackEntries {
    private int totalAnimBlendCount;
    private final int animBlendIndexCacheSize = 32;
    private final LiveAnimationTrackEntry[] liveAnimTrackEntries = new LiveAnimationTrackEntry[32];
    private final int maxLayers = 4;
    private final int[] layerBlendCounts = new int[4];
    private final float[] layerWeightTotals = new float[4];

    public void clear() {
        this.totalAnimBlendCount = 0;
        clear(this.liveAnimTrackEntries);
        Arrays.fill(this.layerBlendCounts, 0);
        Arrays.fill(this.layerWeightTotals, 0.0F);
    }

    public void setTracks(List<AnimationTrack> tracks, float in_minimumValidAnimWeight, boolean in_normalizeFirstLayerTracks) {
        int tracksCount = tracks.size();
        this.clear();

        for (int trackIdx = 0; trackIdx < tracksCount; trackIdx++) {
            AnimationTrack track = tracks.get(trackIdx);
            float trackWeight = track.getBlendWeight();
            int trackLayer = track.getLayerIdx();
            int trackPriority = track.getPriority();
            if (trackLayer >= 0 && trackLayer < 4) {
                if (!(trackWeight < in_minimumValidAnimWeight) && (trackLayer <= 0 || !track.isFinished())) {
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
                        DebugLog.General
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
                DebugLog.General.error("Layer index is out of range: %d. Range: 0 - %d", trackLayer, 3);
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
            if (in_normalizeFirstLayerTracks) {
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

    private static void insertAt(LiveAnimationTrackEntry[] in_liveAnimTrackEntries, AnimationTrack in_track, int in_insertAt) {
        LiveAnimationTrackEntry newEntry = LiveAnimationTrackEntry.alloc(in_track);
        if (in_liveAnimTrackEntries[in_insertAt] == null) {
            in_liveAnimTrackEntries[in_insertAt] = newEntry;
        } else {
            LiveAnimationTrackEntry previousEntry = newEntry;

            for (int i = in_insertAt; i < in_liveAnimTrackEntries.length; i++) {
                LiveAnimationTrackEntry currentEntry = in_liveAnimTrackEntries[i];
                in_liveAnimTrackEntries[i] = previousEntry;
                previousEntry = currentEntry;
                if (currentEntry == null) {
                    break;
                }
            }

            Pool.tryRelease(previousEntry);
        }
    }

    private static void clear(LiveAnimationTrackEntry[] in_array) {
        for (int i = 0; i < in_array.length; i++) {
            Pool.tryRelease(in_array[i]);
            in_array[i] = null;
        }
    }

    public int count() {
        return this.totalAnimBlendCount;
    }

    public LiveAnimationTrackEntry get(int in_trackIdx) {
        return this.liveAnimTrackEntries[in_trackIdx];
    }
}
