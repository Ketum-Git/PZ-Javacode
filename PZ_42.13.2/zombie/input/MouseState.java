// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import zombie.core.Core;
import zombie.debug.DebugContext;

public final class MouseState {
    private boolean isCreated;
    private boolean[] buttonDownStates;
    private int mouseX = -1;
    private int mouseY = -1;
    private int wheelDelta;
    private boolean wasPolled;

    public void poll() {
        boolean isFirstCreate = !this.isCreated;
        this.isCreated = this.isCreated || org.lwjglx.input.Mouse.isCreated();
        if (this.isCreated) {
            if (isFirstCreate) {
                this.buttonDownStates = new boolean[org.lwjglx.input.Mouse.getButtonCount()];
            }

            this.mouseX = org.lwjglx.input.Mouse.getX();
            this.mouseY = org.lwjglx.input.Mouse.getY();
            this.wheelDelta = org.lwjglx.input.Mouse.getDWheel();
            if (Core.isUseGameViewport() && !DebugContext.instance.focusedGameViewport) {
                this.wheelDelta = 0;
                this.mouseX = -1;
                this.mouseY = -1;
            }

            this.wasPolled = true;

            for (int ibutton = 0; ibutton < this.buttonDownStates.length; ibutton++) {
                if (Core.isUseGameViewport() && !DebugContext.instance.focusedGameViewport) {
                    this.buttonDownStates[ibutton] = false;
                } else {
                    this.buttonDownStates[ibutton] = org.lwjglx.input.Mouse.isButtonDown(ibutton);
                }
            }
        }
    }

    public boolean wasPolled() {
        return this.wasPolled;
    }

    public void set(MouseState rhs) {
        this.isCreated = rhs.isCreated;
        if (rhs.buttonDownStates != null) {
            if (this.buttonDownStates == null || this.buttonDownStates.length != rhs.buttonDownStates.length) {
                this.buttonDownStates = new boolean[rhs.buttonDownStates.length];
            }

            System.arraycopy(rhs.buttonDownStates, 0, this.buttonDownStates, 0, this.buttonDownStates.length);
        } else {
            this.buttonDownStates = null;
        }

        this.mouseX = rhs.mouseX;
        this.mouseY = rhs.mouseY;
        this.wheelDelta = rhs.wheelDelta;
        this.wasPolled = rhs.wasPolled;
    }

    public void reset() {
        this.wasPolled = false;
    }

    public boolean isCreated() {
        return this.isCreated;
    }

    public int getX() {
        return DebugContext.isUsingGameViewportWindow() ? DebugContext.instance.getViewportMouseX() : this.mouseX;
    }

    public int getY() {
        return DebugContext.isUsingGameViewportWindow() ? DebugContext.instance.getViewportMouseY() : this.mouseY;
    }

    public int getDWheel() {
        return this.wheelDelta;
    }

    public void resetDWheel() {
        this.wheelDelta = 0;
    }

    public boolean isButtonDown(int button) {
        return button >= this.buttonDownStates.length ? false : this.buttonDownStates[button];
    }

    public int getButtonCount() {
        return this.isCreated() ? this.buttonDownStates.length : 0;
    }

    public void setCursorPosition(int new_x, int new_y) {
        org.lwjglx.input.Mouse.setCursorPosition(new_x, new_y);
    }
}
