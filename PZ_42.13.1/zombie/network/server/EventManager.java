// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.server;

import java.util.ArrayList;

public class EventManager {
    private static final EventManager instance = new EventManager();
    private static final ArrayList<IEventController> callbacks = new ArrayList<>();

    private EventManager() {
    }

    public static EventManager instance() {
        return instance;
    }

    public void registerCallback(IEventController controller) {
        if (controller != null) {
            callbacks.add(controller);
            instance().report("[SERVER] New event controller \"" + controller.getClass().getSimpleName() + "\"");
        }
    }

    public void report(String event) {
        for (IEventController controller : callbacks) {
            controller.process(event);
        }
    }
}
