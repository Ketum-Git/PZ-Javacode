// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.LiveAnimNode;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public final class ThumpAnimations {
    public static final String EVENT_NAME = "ThumpFrame";
    private static final int MAX_THUMP_TIMES = 10;
    private static final float[] thumpTimes = new float[10];

    public static LiveAnimNode getLiveAnimNode(IsoGameCharacter chr) {
        AnimLayer animLayer = chr.getAdvancedAnimator().getRootLayer();
        if (animLayer == null) {
            return null;
        } else {
            for (LiveAnimNode liveAnimNode : animLayer.getLiveAnimNodes()) {
                for (AnimEvent event : liveAnimNode.getSourceNode().events) {
                    if (event.eventName.equalsIgnoreCase("ThumpFrame")) {
                        return liveAnimNode;
                    }
                }
            }

            return null;
        }
    }

    public static AnimationTrack getAnimationTrack(IsoGameCharacter chr) {
        LiveAnimNode liveAnimNode = getLiveAnimNode(chr);
        return liveAnimNode == null ? null : getAnimationTrack(liveAnimNode);
    }

    public static AnimationTrack getAnimationTrack(LiveAnimNode liveAnimNode) {
        for (int i = 0; i < liveAnimNode.getMainAnimationTracksCount(); i++) {
            AnimationTrack track = liveAnimNode.getMainAnimationTrackAt(i);
            if (track.isPlaying) {
                return track;
            }
        }

        return null;
    }

    public static float getDuration(IsoGameCharacter chr) {
        LiveAnimNode liveAnimNode = getLiveAnimNode(chr);
        if (liveAnimNode == null) {
            return -1.0F;
        } else {
            AnimationTrack track = getAnimationTrack(liveAnimNode);
            return track == null ? -1.0F : track.getDuration();
        }
    }

    public static int getThumpTimes(LiveAnimNode liveAnimNode, float[] times) {
        int count = 0;

        for (AnimEvent event : liveAnimNode.getSourceNode().events) {
            if (event.eventName.equalsIgnoreCase("ThumpFrame")) {
                if (event.time == AnimEvent.AnimEventTime.PERCENTAGE) {
                    times[count++] = event.timePc;
                } else {
                    times[count++] = event.time == AnimEvent.AnimEventTime.START ? 0.0F : 1.0F;
                }
            }
        }

        return count;
    }

    public static int countThumpEventsBetween(IsoGameCharacter chr, float startTime, float endTime) {
        LiveAnimNode liveAnimNode = getLiveAnimNode(chr);
        if (liveAnimNode == null) {
            return 0;
        } else {
            AnimationTrack track = getAnimationTrack(liveAnimNode);
            if (track == null) {
                return 0;
            } else {
                int thumpTimesCount = getThumpTimes(liveAnimNode, thumpTimes);
                if (thumpTimesCount == 0) {
                    return 0;
                } else {
                    float duration = track.getDuration();
                    int result = 0;

                    for (int i = 0; i < thumpTimesCount; i++) {
                        int s = (int)Math.floor((startTime - thumpTimes[i] * duration) / duration);
                        int e = (int)Math.floor((endTime - thumpTimes[i] * duration) / duration);
                        result += e - s;
                    }

                    return result;
                }
            }
        }
    }
}
