// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.core.skinnedmodel.advancedanimation.events.GlobalAnimEvent;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugType;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.lambda.PZOptional;

public class AnimationEventRecordingFrame extends GenericNameValueRecordingFrame {
    private final List<AnimationEventRecordingFrame.EventRecordEntry> events = new ArrayList<>();

    public AnimationEventRecordingFrame(String fileKey) {
        super(fileKey, "_events");
        this.addColumnInternal("sender");
        this.addColumnInternal("animEvent.name");
        this.addColumnInternal("animEvent.parameter");
        this.addColumnInternal("animNode");
        this.addColumnInternal("track");
        this.addColumnInternal("animEvent.time");
    }

    public void logAnimEvent(IAnimatable character, AnimationTrack track, AnimEvent evt) {
        this.events.add(AnimationEventRecordingFrame.EventRecordEntry.alloc(character, evt, PZOptional.ifPresent(track, "", AnimationTrack::getName)));
    }

    public void logGlobalAnimEvent(IAnimatable character, GlobalAnimEvent evt) {
        this.events.add(AnimationEventRecordingFrame.EventRecordEntry.alloc(character, evt.getAnimEvent(), "__GLOBAL__"));
    }

    @Override
    public void reset() {
        IPooledObject.release(this.events);
    }

    @Override
    public String getValueAt(int i) {
        return "";
    }

    @Override
    protected void onColumnAdded() {
    }

    protected void writeData(IAnimatable sender, String track, AnimEvent event, StringBuilder logLine) {
        appendCell(logLine, PZOptional.ifPresent(sender, "!NoSender!", IAnimatable::getUID));
        appendCell(logLine, event.eventName);
        appendCell(logLine, Objects.requireNonNullElse(event.parameterValue, ""));
        appendCell(logLine, event.parentAnimNode != null ? event.parentAnimNode.name : "");
        appendCell(logLine, track);
        if (event.time == AnimEvent.AnimEventTime.PERCENTAGE) {
            appendCell(logLine, event.timePc);
        } else {
            appendCell(logLine, event.time.toString());
        }
    }

    @Override
    protected void writeData() {
        if (this.outValues == null) {
            this.openValuesFile(false);
        }

        StringBuilder logLine = this.lineBuffer;

        for (AnimationEventRecordingFrame.EventRecordEntry eventEntry : this.events) {
            logLine.setLength(0);
            this.buildHeader(logLine);
            String headerStr = logLine.toString();
            logLine.setLength(0);
            this.writeData(eventEntry.sender, eventEntry.trackName, eventEntry.event, logLine);
            this.outValues.print(this.frameNumber);
            this.outValues.println(logLine);
            DebugType.AnimationRecorder.println("AnimEventTriggered:\r\n%s%s\r\n%s%s", "frameNo", headerStr, this.frameNumber, logLine);
        }
    }

    private static class EventRecordEntry extends PooledObject {
        public IAnimatable sender;
        public AnimEvent event;
        public String trackName;
        private static final Pool<AnimationEventRecordingFrame.EventRecordEntry> pool = new Pool<>(AnimationEventRecordingFrame.EventRecordEntry::new);

        public static AnimationEventRecordingFrame.EventRecordEntry alloc(IAnimatable character, AnimEvent evt, String trackName) {
            AnimationEventRecordingFrame.EventRecordEntry newEvent = pool.alloc();
            newEvent.sender = character;
            newEvent.event = evt;
            newEvent.trackName = trackName;
            return newEvent;
        }
    }
}
