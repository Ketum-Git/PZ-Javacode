// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

public final class KeyboardStateCache {
    private final Object lock = "KeyboardStateCache Lock";
    private int stateIndexUsing;
    private int stateIndexPolling = 1;
    private final KeyboardState[] states = new KeyboardState[]{new KeyboardState(), new KeyboardState()};

    public void poll() {
        synchronized (this.lock) {
            KeyboardState statePolling = this.getStatePolling();
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

    public KeyboardState getState() {
        synchronized (this.lock) {
            return this.states[this.stateIndexUsing];
        }
    }

    public KeyboardState getStatePolling() {
        synchronized (this.lock) {
            return this.states[this.stateIndexPolling];
        }
    }
}
