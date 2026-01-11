// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.input;

import java.util.ArrayList;
import org.lwjglx.input.Controller;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.core.Core;
import zombie.input.ControllerState;
import zombie.input.ControllerStateCache;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;

/**
 * A wrapped for all keyboard, mouse and controller input
 */
public final class Input {
    /**
     * The controller index to pass to check all controllers
     */
    public static final int ANY_CONTROLLER = -1;
    private final Controller[] controllers = new Controller[16];
    private final ArrayList<Controller> newlyConnected = new ArrayList<>();
    private final ArrayList<Controller> newlyDisconnected = new ArrayList<>();
    private final boolean[][] controllerPressed = new boolean[16][15];
    private final boolean[][] controllerWasPressed = new boolean[16][15];
    private final float[][] controllerPov = new float[16][2];
    private final ControllerStateCache controllerStateCache = new ControllerStateCache();

    /**
     * get the character representation of the key identified by the specified code
     * 
     * @param code The key code of the key to retrieve the name of
     * @return The name or character representation of the key requested
     */
    public static String getKeyName(int code) {
        if (code >= 10000) {
            return "Mouse Btn " + (code - 10000);
        } else {
            String keyName = Keyboard.getKeyName(code);
            if ("LSHIFT".equals(keyName)) {
                return "Left SHIFT";
            } else if ("RSHIFT".equals(keyName)) {
                return "Right SHIFT";
            } else if ("LMENU".equals(keyName)) {
                return "Left ALT";
            } else if ("RMENU".equals(keyName)) {
                return "Right ALT";
            } else {
                if (System.getProperty("os.name").contains("OS X")) {
                    if ("LMETA".equals(keyName)) {
                        return "Left Command";
                    }

                    if ("RMETA".equals(keyName)) {
                        return "Right Command";
                    }
                }

                return keyName;
            }
        }
    }

    public static int getKeyCode(String keyName) {
        if (System.getProperty("os.name").contains("OS X")) {
            if ("Left ALT".equals(keyName)) {
                return 219;
            }

            if ("Right ALT".equals(keyName)) {
                return 220;
            }
        }

        if ("Right SHIFT".equals(keyName)) {
            return 54;
        } else if ("Left SHIFT".equals(keyName)) {
            return 42;
        } else if ("Left ALT".equals(keyName)) {
            return 56;
        } else {
            return "Right ALT".equals(keyName) ? 184 : Keyboard.getKeyIndex(keyName);
        }
    }

    /**
     * get a count of the number of controllers available
     * @return The number of controllers available
     */
    public int getControllerCount() {
        return this.controllers.length;
    }

    /**
     * get the number of axis that are avaiable on a given controller
     * 
     * @param index The index of the controller to check
     * @return The number of axis available on the controller
     */
    public int getAxisCount(int index) {
        Controller controller = this.getController(index);
        return controller == null ? 0 : controller.getAxisCount();
    }

    /**
     * get the value of the axis with the given index
     * 
     * @param index The index of the controller to check
     * @param axis The index of the axis to read
     * @return The axis value at time of reading
     */
    public float getAxisValue(int index, int axis) {
        Controller controller = this.getController(index);
        return controller == null ? 0.0F : controller.getAxisValue(axis);
    }

    /**
     * get the name of the axis with the given index
     * 
     * @param index The index of the controller to check
     * @param axis The index of the axis to read
     * @return The name of the specified axis
     */
    public String getAxisName(int index, int axis) {
        Controller controller = this.getController(index);
        return controller == null ? null : controller.getAxisName(axis);
    }

    /**
     * Check if the controller has the left direction pressed
     * 
     * @param index The index of the controller to check
     * @return True if the controller is pressed to the left
     */
    public boolean isControllerLeftD(int index) {
        if (index == -1) {
            for (int i = 0; i < this.controllers.length; i++) {
                if (this.isControllerLeftD(i)) {
                    return true;
                }
            }

            return false;
        } else {
            Controller controller = this.getController(index);
            return controller == null ? false : controller.getPovX() < -0.5F;
        }
    }

    /**
     * Check if the controller has the right direction pressed
     * 
     * @param index The index of the controller to check
     * @return True if the controller is pressed to the right
     */
    public boolean isControllerRightD(int index) {
        if (index == -1) {
            for (int i = 0; i < this.controllers.length; i++) {
                if (this.isControllerRightD(i)) {
                    return true;
                }
            }

            return false;
        } else {
            Controller controller = this.getController(index);
            return controller == null ? false : controller.getPovX() > 0.5F;
        }
    }

    /**
     * Check if the controller has the up direction pressed
     * 
     * @param index The index of the controller to check
     * @return True if the controller is pressed to the up
     */
    public boolean isControllerUpD(int index) {
        if (index == -1) {
            for (int i = 0; i < this.controllers.length; i++) {
                if (this.isControllerUpD(i)) {
                    return true;
                }
            }

            return false;
        } else {
            Controller controller = this.getController(index);
            return controller == null ? false : controller.getPovY() < -0.5F;
        }
    }

    /**
     * Check if the controller has the down direction pressed
     * 
     * @param index The index of the controller to check
     * @return True if the controller is pressed to the down
     */
    public boolean isControllerDownD(int index) {
        if (index == -1) {
            for (int i = 0; i < this.controllers.length; i++) {
                if (this.isControllerDownD(i)) {
                    return true;
                }
            }

            return false;
        } else {
            Controller controller = this.getController(index);
            return controller == null ? false : controller.getPovY() > 0.5F;
        }
    }

    private Controller checkControllerButton(int index, int button) {
        Controller controller = this.getController(index);
        if (controller == null) {
            return null;
        } else {
            return button >= 0 && button < controller.getButtonCount() ? controller : null;
        }
    }

    /**
     * Check if controller button is pressed
     * 
     * @param button The index of the button to check
     * @param index The index of the controller to check
     * @return True if the button is pressed
     */
    public boolean isButtonPressedD(int button, int index) {
        if (index == -1) {
            for (int i = 0; i < this.controllers.length; i++) {
                if (this.isButtonPressedD(button, i)) {
                    return true;
                }
            }

            return false;
        } else {
            Controller controller = this.checkControllerButton(index, button);
            return controller == null ? false : this.controllerPressed[index][button];
        }
    }

    /**
     * Check if a controller button was pressed the previous frame.
     * 
     * @param index The controller index.
     * @param button The button index.
     * @return true if the controller button was in the pressed state the previous frame.
     */
    public boolean wasButtonPressed(int index, int button) {
        Controller controller = this.checkControllerButton(index, button);
        return controller == null ? false : this.controllerWasPressed[index][button];
    }

    public boolean isButtonStartPress(int index, int button) {
        return !this.wasButtonPressed(index, button) && this.isButtonPressedD(button, index);
    }

    public boolean isButtonReleasePress(int index, int button) {
        return this.wasButtonPressed(index, button) && !this.isButtonPressedD(button, index);
    }

    /**
     * Initialise the controllers system
     */
    public void initControllers() {
        this.updateGameThread();
        this.updateGameThread();
    }

    private void onControllerConnected(Controller controller) {
        JoypadManager.instance.onControllerConnected(controller);
        if (LuaManager.env != null) {
            LuaEventManager.triggerEvent("OnGamepadConnect", controller.getID());
        }
    }

    private void onControllerDisconnected(Controller controller) {
        JoypadManager.instance.onControllerDisconnected(controller);
        if (LuaManager.env != null) {
            LuaEventManager.triggerEvent("OnGamepadDisconnect", controller.getID());
        }
    }

    /**
     * Poll the state of the input
     */
    public void poll() {
        if (!Core.getInstance().isDoingTextEntry()) {
            while (GameKeyboard.getEventQueuePolling().next()) {
            }
        }

        while (Mouse.next()) {
        }

        this.controllerStateCache.poll();
    }

    public Controller getController(int index) {
        return index >= 0 && index < this.controllers.length ? this.controllers[index] : null;
    }

    public int getButtonCount(int index) {
        Controller controller = this.getController(index);
        return controller == null ? null : controller.getButtonCount();
    }

    public String getButtonName(int index, int button) {
        Controller controller = this.getController(index);
        return controller == null ? null : controller.getButtonName(button);
    }

    public void updateGameThread() {
        if (!this.controllerStateCache.getState().isCreated()) {
            this.controllerStateCache.swap();
        } else {
            ControllerState controllerState = this.controllerStateCache.getState();
            if (this.checkConnectDisconnect(controllerState)) {
                for (int i = 0; i < this.newlyDisconnected.size(); i++) {
                    Controller controller = this.newlyDisconnected.get(i);
                    this.onControllerDisconnected(controller);
                }

                for (int i = 0; i < this.newlyConnected.size(); i++) {
                    Controller controller = this.newlyConnected.get(i);
                    this.onControllerConnected(controller);
                }
            }

            for (int i = 0; i < this.getControllerCount(); i++) {
                Controller controller = this.getController(i);
                if (controller != null) {
                    int count = controller.getButtonCount();

                    for (int c = 0; c < count; c++) {
                        this.controllerWasPressed[i][c] = this.controllerPressed[i][c];
                        if (this.controllerPressed[i][c] && !controller.isButtonPressed(c)) {
                            this.controllerPressed[i][c] = false;
                        } else if (!this.controllerPressed[i][c] && controller.isButtonPressed(c)) {
                            this.controllerPressed[i][c] = true;
                            JoypadManager.instance.onPressed(i, c);
                        }
                    }

                    count = controller.getAxisCount();

                    for (int cx = 0; cx < count; cx++) {
                        float axisValue = controller.getAxisValue(cx);
                        if ((!controller.isGamepad() || cx != 4) && cx != 5) {
                            if (axisValue < -0.5F) {
                                JoypadManager.instance.onPressedAxisNeg(i, cx);
                            }

                            if (axisValue > 0.5F) {
                                JoypadManager.instance.onPressedAxis(i, cx);
                            }
                        } else if (axisValue > 0.0F) {
                            JoypadManager.instance.onPressedTrigger(i, cx);
                        }
                    }

                    float povX = controller.getPovX();
                    float povY = controller.getPovY();
                    if (povX != this.controllerPov[i][0] || povY != this.controllerPov[i][1]) {
                        this.controllerPov[i][0] = povX;
                        this.controllerPov[i][1] = povY;
                        JoypadManager.instance.onPressedPov(i);
                    }
                }
            }

            this.controllerStateCache.swap();
        }
    }

    private boolean checkConnectDisconnect(ControllerState controllerState) {
        boolean bChanged = false;
        this.newlyConnected.clear();
        this.newlyDisconnected.clear();

        for (int i = 0; i < 16; i++) {
            Controller controller = controllerState.getController(i);
            if (controller != this.controllers[i]) {
                bChanged = true;
                if (controller != null && controller.isGamepad()) {
                    this.newlyConnected.add(controller);
                } else {
                    if (this.controllers[i] != null) {
                        this.newlyDisconnected.add(this.controllers[i]);
                    }

                    controller = null;
                }

                this.controllers[i] = controller;
            }
        }

        return bChanged;
    }

    public void quit() {
        this.controllerStateCache.quit();
    }
}
