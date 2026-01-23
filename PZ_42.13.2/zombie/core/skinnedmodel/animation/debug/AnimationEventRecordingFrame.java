// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.debug;

import java.util.ArrayList;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.animation.AnimationTrack;

public class AnimationEventRecordingFrame extends GenericNameValueRecordingFrame {
    private final ArrayList<AnimEvent> events = new ArrayList<>();
    private final ArrayList<String> tracks = new ArrayList<>();

    public AnimationEventRecordingFrame(String fileKey) {
        super(fileKey, "_events");
        this.addColumnInternal("animNode");
        this.addColumnInternal("track");
        this.addColumnInternal("animEvent.name");
        this.addColumnInternal("animEvent.time");
        this.addColumnInternal("animEvent.parameter");
    }

    public void logAnimEvent(AnimationTrack track, AnimEvent evt) {
        this.tracks.add(track != null ? track.getName() : "");
        this.events.add(evt);
    }

    public void logGlobalAnimEvent(AnimEvent evt) {
        this.tracks.add("__GLOBAL__");
        this.events.add(evt);
    }

    @Override
    public void reset() {
        this.tracks.clear();
        this.events.clear();
    }

    @Override
    public String getValueAt(int i) {
        return "";
    }

    @Override
    protected void onColumnAdded() {
    }

    protected void writeData(String track, AnimEvent event, StringBuilder logLine) {
        appendCell(logLine, event.parentAnimNode != null ? event.parentAnimNode.name : "");
        appendCell(logLine, track);
        appendCell(logLine, event.eventName);
        if (event.time == AnimEvent.AnimEventTime.PERCENTAGE) {
            appendCell(logLine, event.timePc);
        } else {
            appendCell(logLine, event.time.toString());
        }

        appendCell(logLine, event.parameterValue != null ? event.parameterValue : "");
    }

    @Override
    protected void writeData() {
        if (this.outValues == null) {
            this.openValuesFile(false);
        }

        StringBuilder logLine = this.lineBuffer;

        for (int i = 0; i < this.events.size(); i++) {
            logLine.setLength(0);
            AnimEvent event = this.events.get(i);
            String track = this.tracks.get(i);
            this.writeData(track, event, logLine);
            this.outValues.print(this.frameNumber);
            this.outValues.println(logLine);
        }
    }
}
