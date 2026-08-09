// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import org.lwjglx.input.KeyEventQueue;
import org.lwjglx.input.Keyboard;
import zombie.core.Core;
import zombie.debug.DebugContext;

public final class KeyboardState {
    private boolean isCreated;
    private boolean[] keyDownStates;
    private final KeyEventQueue keyEventQueue = new KeyEventQueue();
    private boolean wasPolled;

    public void poll() {
        boolean isFirstCreate = !this.isCreated;
        this.isCreated = this.isCreated || Keyboard.isCreated();
        if (this.isCreated) {
            if (isFirstCreate) {
                this.keyDownStates = new boolean[256];
            }

            this.wasPolled = true;

            for (int ikey = 0; ikey < this.keyDownStates.length; ikey++) {
                if (Core.isUseGameViewport() && !DebugContext.instance.focusedGameViewport) {
                    this.keyDownStates[ikey] = false;
                } else {
                    this.keyDownStates[ikey] = Keyboard.isKeyDown(ikey);
                }
            }
        }
    }

    public boolean wasPolled() {
        return this.wasPolled;
    }

    public void set(KeyboardState rhs) {
        this.isCreated = rhs.isCreated;
        if (rhs.keyDownStates != null) {
            if (this.keyDownStates == null || this.keyDownStates.length != rhs.keyDownStates.length) {
                this.keyDownStates = new boolean[rhs.keyDownStates.length];
            }

            System.arraycopy(rhs.keyDownStates, 0, this.keyDownStates, 0, this.keyDownStates.length);
        } else {
            this.keyDownStates = null;
        }

        this.wasPolled = rhs.wasPolled;
    }

    public void reset() {
        this.wasPolled = false;
    }

    public boolean isCreated() {
        return this.isCreated;
    }

    public boolean isKeyDown(int button) {
        return this.keyDownStates[button];
    }

    public int getKeyCount() {
        return this.keyDownStates.length;
    }

    public KeyEventQueue getEventQueue() {
        return this.keyEventQueue;
    }
}
