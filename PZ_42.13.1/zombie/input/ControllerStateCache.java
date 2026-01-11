// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

public class ControllerStateCache {
    private final Object lock = "ControllerStateCache Lock";
    private int stateIndexUsing;
    private int stateIndexPolling = 1;
    private final ControllerState[] states = new ControllerState[]{new ControllerState(), new ControllerState()};

    public void poll() {
        synchronized (this.lock) {
            ControllerState statePolling = this.getStatePolling();
            if (!statePolling.wasPolled()) {
                statePolling.poll();
            }
        }
    }

    public void swap() {
        synchronized (this.lock) {
            if (this.getStatePolling().wasPolled()) {
                this.stateIndexUsing = this.stateIndexPolling;
                this.stateIndexPolling = this.stateIndexPolling == 1 ? 0 : 1;
                this.getStatePolling().set(this.getState());
                this.getStatePolling().reset();
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
}
