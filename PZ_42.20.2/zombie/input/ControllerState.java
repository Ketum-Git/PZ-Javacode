// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import org.lwjglx.input.Controller;
import org.lwjglx.input.Controllers;
import org.lwjglx.input.GamepadState;

public class ControllerState {
    private boolean isCreated;
    private boolean wasPolled;
    private final GamepadState[] gamepadState = new GamepadState[16];

    ControllerState() {
        for (int i = 0; i < this.gamepadState.length; i++) {
            this.gamepadState[i] = new GamepadState();
        }
    }

    public void poll() {
        this.isCreated = this.isCreated || Controllers.isCreated();
        if (this.isCreated) {
            Controllers.poll(this.gamepadState);
            this.wasPolled = true;
        }
    }

    public boolean wasPolled() {
        return this.wasPolled;
    }

    public void set(ControllerState rhs) {
        this.isCreated = rhs.isCreated;

        for (int i = 0; i < this.gamepadState.length; i++) {
            this.gamepadState[i].set(rhs.gamepadState[i]);
        }

        this.wasPolled = rhs.wasPolled;
    }

    public boolean isCreated() {
        return this.isCreated;
    }

    public void quit() {
        for (GamepadState state : this.gamepadState) {
            state.quit();
        }
    }

    public void onStateActive(ControllerStateCache stateCache) {
        for (int i = 0; i < 16; i++) {
            Controller controller = stateCache.getController(i);
            if (controller != null) {
                controller.gamepadState = this.gamepadState[i];
            }
        }
    }

    public void onStatePolling(ControllerStateCache stateCache) {
        this.set(stateCache.getState());
        this.wasPolled = false;
    }
}
