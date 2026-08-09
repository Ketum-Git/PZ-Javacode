// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import org.lwjglx.input.Controller;
import org.lwjglx.input.Controllers;

public class ControllerStateCache {
    private final Object lock = "ControllerStateCache Lock";
    private int stateIndexUsing;
    private int stateIndexPolling = 1;
    private final ControllerState[] states = new ControllerState[]{new ControllerState(), new ControllerState()};
    private final Controller[] controllers = new Controller[16];

    public void poll() {
        synchronized (this.lock) {
            if (Controllers.isCreated()) {
                ControllerState statePolling = this.getStatePolling();
                if (!statePolling.wasPolled()) {
                    for (int i = 0; i < 16; i++) {
                        this.controllers[i] = Controllers.getController(i);
                    }

                    statePolling.poll();
                }
            }
        }
    }

    public void swap() {
        synchronized (this.lock) {
            ControllerState prevStatePolling = this.getStatePolling();
            if (prevStatePolling.wasPolled()) {
                this.stateIndexUsing = this.stateIndexPolling;
                this.stateIndexPolling = this.stateIndexPolling == 1 ? 0 : 1;
                ControllerState stateActive = this.getState();
                stateActive.onStateActive(this);
                ControllerState statePolling = this.getStatePolling();
                statePolling.onStatePolling(this);
            }
        }
    }

    public ControllerState getState() {
        synchronized (this.lock) {
            return this.states[this.stateIndexUsing];
        }
    }

    private ControllerState getStatePolling() {
        synchronized (this.lock) {
            return this.states[this.stateIndexPolling];
        }
    }

    public void quit() {
        this.states[0].quit();
        this.states[1].quit();
    }

    public Controller getController(int index) {
        synchronized (this.lock) {
            return this.controllers[index];
        }
    }
}
