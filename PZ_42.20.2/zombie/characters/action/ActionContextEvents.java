// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.action;

import java.util.HashMap;
import zombie.util.StringUtils;

public final class ActionContextEvents {
    private ActionContextEvents.Event firstEvent;
    private ActionContextEvents.Event eventPool;

    public synchronized void add(String name, String state) {
        if (!this.contains(name, state, false)) {
            ActionContextEvents.Event event = this.allocEvent();
            event.name = name;
            event.state = state;
            event.next = this.firstEvent;
            this.firstEvent = event;
        }
    }

    public boolean contains(String name, String state) {
        return this.contains(name, state, true);
    }

    public boolean contains(String name, String state, boolean bAgnosticLayer) {
        for (ActionContextEvents.Event event = this.firstEvent; event != null; event = event.next) {
            if (event.name.equalsIgnoreCase(name)) {
                if (state == null) {
                    return true;
                }

                if (StringUtils.equalsIgnoreCase(event.state, state)) {
                    return true;
                }

                if (bAgnosticLayer && event.state == null) {
                    return true;
                }
            }
        }

        return false;
    }

    public synchronized void clear() {
        if (this.firstEvent != null) {
            ActionContextEvents.Event last = this.firstEvent;

            while (last.next != null) {
                last = last.next;
            }

            last.next = this.eventPool;
            this.eventPool = this.firstEvent;
            this.firstEvent = null;
        }
    }

    public synchronized void clearEvent(String name) {
        ActionContextEvents.Event prev = null;
        ActionContextEvents.Event event = this.firstEvent;

        while (event != null) {
            ActionContextEvents.Event next = event.next;
            if (event.name.equalsIgnoreCase(name)) {
                this.releaseEvent(event, prev);
            } else {
                prev = event;
            }

            event = next;
        }
    }

    private ActionContextEvents.Event allocEvent() {
        if (this.eventPool == null) {
            return new ActionContextEvents.Event();
        } else {
            ActionContextEvents.Event event = this.eventPool;
            this.eventPool = event.next;
            return event;
        }
    }

    private void releaseEvent(ActionContextEvents.Event event, ActionContextEvents.Event prev) {
        if (prev == null) {
            assert event == this.firstEvent;

            this.firstEvent = event.next;
        } else {
            assert event != this.firstEvent;

            assert prev.next == event;

            prev.next = event.next;
        }

        event.next = this.eventPool;
        this.eventPool = event;
    }

    public void get(HashMap<String, String> events) {
        for (ActionContextEvents.Event event = this.firstEvent; event != null; event = event.next) {
            if (event.state == null) {
                events.put(event.name, event.state);
            }
        }
    }

    private static final class Event {
        String state;
        String name;
        ActionContextEvents.Event next;
    }
}
