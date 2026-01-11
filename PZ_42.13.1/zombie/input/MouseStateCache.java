// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

public final class MouseStateCache {
    private final Object lock = "MouseStateCache Lock";
    private int stateIndexUsing;
    private int stateIndexPolling = 1;
    private final MouseState[] states = new MouseState[]{new MouseState(), new MouseState()};

    public void poll() {
        synchronized (this.lock) {
            MouseState statePolling = this.getStatePolling();
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

    public MouseState getState() {
        synchronized (this.lock) {
            return this.states[this.stateIndexUsing];
        }
    }

    private MouseState getStatePolling() {
        synchronized (this.lock) {
            return this.states[this.stateIndexPolling];
        }
    }
}
