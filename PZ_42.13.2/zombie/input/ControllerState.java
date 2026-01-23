// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import org.lwjglx.input.Controller;
import org.lwjglx.input.Controllers;
import org.lwjglx.input.GamepadState;

public class ControllerState {
    private boolean isCreated;
    private boolean wasPolled;
    private final Controller[] controllers = new Controller[16];
    private final GamepadState[] gamepadState = new GamepadState[16];

    ControllerState() {
        for (int i = 0; i < this.controllers.length; i++) {
            this.gamepadState[i] = new GamepadState();
        }
    }

    public void poll() {
        boolean isFirstCreate = !this.isCreated;
        this.isCreated = this.isCreated || Controllers.isCreated();
        if (this.isCreated) {
            if (isFirstCreate) {
            }

            this.wasPolled = true;
            Controllers.poll(this.gamepadState);

            for (int i = 0; i < Controllers.getControllerCount(); i++) {
                this.controllers[i] = Controllers.getController(i);
            }
        }
    }

    public boolean wasPolled() {
        return this.wasPolled;
    }

    public void set(ControllerState rhs) {
        this.isCreated = rhs.isCreated;

        for (int i = 0; i < this.controllers.length; i++) {
            this.controllers[i] = rhs.controllers[i];
            if (this.controllers[i] != null) {
                this.gamepadState[i].set(rhs.gamepadState[i]);
                this.controllers[i].gamepadState = this.gamepadState[i];
            }
        }

        this.wasPolled = rhs.wasPolled;
    }

    public void reset() {
        this.wasPolled = false;
    }

    public boolean isCreated() {
        return this.isCreated;
    }

    public Controller getController(int index) {
        return this.controllers[index];
    }

    public void quit() {
        for (int i = 0; i < this.controllers.length; i++) {
            this.gamepadState[i].quit();
        }
    }
}
