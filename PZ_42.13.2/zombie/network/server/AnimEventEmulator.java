// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.server;

import java.util.ArrayList;
import zombie.GameTime;
import zombie.core.NetTimedAction;
import zombie.debug.DebugLog;

public class AnimEventEmulator {
    private static final AnimEventEmulator instance = new AnimEventEmulator();
    private final ArrayList<AnimEventEmulator.AnimEvent> events = new ArrayList<>();

    public static AnimEventEmulator getInstance() {
        return instance;
    }

    private AnimEventEmulator() {
    }

    public long getDurationMax() {
        return 1800000L;
    }

    public void create(NetTimedAction action, long duration, boolean isOnce, String event, String parameter) {
        DebugLog.Action.debugln("%s %s (%s) %s %d ms", action.type, event, parameter, isOnce ? "after" : "every", duration);
        this.events.add(new AnimEventEmulator.AnimEvent(action, duration, isOnce, event, parameter));
    }

    public void remove(NetTimedAction action) {
        this.events.removeIf(event -> event.action == action);
    }

    public void update() {
        long time = GameTime.getServerTimeMills();

        for (AnimEventEmulator.AnimEvent event : this.events) {
            if (event.action != null && time >= event.time + event.duration) {
                event.action.animEvent(event.event, event.parameter);
                if (event.isOnce) {
                    event.time = event.start + this.getDurationMax();
                } else {
                    event.time = GameTime.getServerTimeMills();
                }
            }
        }

        this.events.removeIf(eventx -> time >= eventx.start + this.getDurationMax());
    }

    public static class AnimEvent {
        private final NetTimedAction action;
        private final String event;
        private final String parameter;
        private final long start;
        private final long duration;
        private final boolean isOnce;
        private long time;

        private AnimEvent(NetTimedAction action, long duration, boolean isOnce, String event, String parameter) {
            long time = GameTime.getServerTimeMills();
            this.action = action;
            this.start = time;
            this.time = time;
            this.duration = duration;
            this.isOnce = isOnce;
            this.event = event;
            this.parameter = parameter;
        }
    }
}
