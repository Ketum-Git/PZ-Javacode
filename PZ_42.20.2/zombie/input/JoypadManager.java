// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.input;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Function;
import java.util.function.Predicate;
import org.lwjglx.input.Controller;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.BoxedStaticValues;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.iso.Vector2;
import zombie.util.lambda.Consumers;

public final class JoypadManager {
    public static final JoypadManager instance = new JoypadManager();
    public final JoypadManager.Joypad[] joypads = new JoypadManager.Joypad[4];
    public final JoypadManager.Joypad[] joypadsController = new JoypadManager.Joypad[16];
    public final ArrayList<JoypadManager.Joypad> joypadList = new ArrayList<>();
    public final HashSet<String> activeControllerGuids = new HashSet<>();
    private static final int VERSION_1 = 1;
    private static final int VERSION_2 = 2;
    private static final int VERSION_LATEST = 2;

    private void addJoypad(int controller, String guid, String name) {
        JoypadManager.Joypad newJoypad = new JoypadManager.Joypad();
        newJoypad.id = controller;
        newJoypad.guid = guid;
        newJoypad.name = name;
        this.joypadsController[controller] = newJoypad;
        this.doControllerFile(newJoypad);
        if (!newJoypad.isDisabled() && this.activeControllerGuids.contains(guid)) {
            this.joypadList.add(newJoypad);
        }
    }

    private JoypadManager.Joypad checkJoypad(int joypadIndex) {
        if (joypadIndex == -1) {
            return null;
        } else {
            if (this.joypadsController[joypadIndex] == null) {
                Controller controller = GameWindow.GameInput.getController(joypadIndex);
                if (controller != null) {
                    this.addJoypad(joypadIndex, controller.getGUID(), controller.getGamepadName());
                }
            }

            return this.joypadsController[joypadIndex];
        }
    }

    private void doControllerFile(JoypadManager.Joypad j) {
        File file = new File(ZomboidFileSystem.instance.getCacheDirSub("joypads"));
        if (!file.exists()) {
            file.mkdir();
        }

        file = new File(ZomboidFileSystem.instance.getCacheDirSub("joypads" + File.separator + j.guid + ".config"));

        try (
            FileReader fileReader = new FileReader(file.getAbsolutePath());
            BufferedReader br = new BufferedReader(fileReader);
        ) {
            DebugType.DetailedInfo.trace("reloading " + file.getAbsolutePath());
            int version = -1;

            try {
                String line = "";

                while (line != null) {
                    line = br.readLine();
                    if (line != null && !line.trim().isEmpty() && !line.trim().startsWith("//")) {
                        String[] split = line.split("=");
                        if (split.length == 2) {
                            split[0] = split[0].trim();
                            split[1] = split[1].trim();
                            if (split[0].equals("Version")) {
                                version = Integer.parseInt(split[1]);
                                if (version < 1 || version > 2) {
                                    DebugType.DetailedInfo.warn("Unknown version %d in %s", version, file.getAbsolutePath());
                                    break;
                                }

                                if (version == 1) {
                                    DebugType.DetailedInfo.warn("Obsolete version %d in %s.  Using default values.", version, file.getAbsolutePath());
                                    break;
                                }
                            }

                            if (version == -1) {
                                DebugType.General.warn("Ignoring %s=%s because Version is missing", split[0], split[1]);
                            } else if (split[0].equals("MovementAxisX")) {
                                j.movementAxisX = Integer.parseInt(split[1]);
                            } else if (split[0].equals("MovementAxisXFlipped")) {
                                j.movementAxisXFlipped = split[1].equals("true");
                            } else if (split[0].equals("MovementAxisY")) {
                                j.movementAxisY = Integer.parseInt(split[1]);
                            } else if (split[0].equals("MovementAxisYFlipped")) {
                                j.movementAxisYFlipped = split[1].equals("true");
                            } else if (split[0].equals("MovementAxisDeadZone")) {
                                j.movementAxisDeadZone = Float.parseFloat(split[1]);
                            } else if (split[0].equals("AimingAxisX")) {
                                j.aimingAxisX = Integer.parseInt(split[1]);
                            } else if (split[0].equals("AimingAxisXFlipped")) {
                                j.aimingAxisXFlipped = split[1].equals("true");
                            } else if (split[0].equals("AimingAxisY")) {
                                j.aimingAxisY = Integer.parseInt(split[1]);
                            } else if (split[0].equals("AimingAxisYFlipped")) {
                                j.aimingAxisYFlipped = split[1].equals("true");
                            } else if (split[0].equals("AimingAxisDeadZone")) {
                                j.aimingAxisDeadZone = Float.parseFloat(split[1]);
                            } else if (split[0].equals("AButton")) {
                                j.aButton = Integer.parseInt(split[1]);
                            } else if (split[0].equals("BButton")) {
                                j.bButton = Integer.parseInt(split[1]);
                            } else if (split[0].equals("XButton")) {
                                j.xButton = Integer.parseInt(split[1]);
                            } else if (split[0].equals("YButton")) {
                                j.yButton = Integer.parseInt(split[1]);
                            } else if (split[0].equals("LBumper")) {
                                j.bumperLeft = Integer.parseInt(split[1]);
                            } else if (split[0].equals("RBumper")) {
                                j.bumperRight = Integer.parseInt(split[1]);
                            } else if (split[0].equals("L3")) {
                                j.leftStickButton = Integer.parseInt(split[1]);
                            } else if (split[0].equals("R3")) {
                                j.rightStickButton = Integer.parseInt(split[1]);
                            } else if (split[0].equals("Back")) {
                                j.back = Integer.parseInt(split[1]);
                            } else if (split[0].equals("Start")) {
                                j.start = Integer.parseInt(split[1]);
                            } else if (split[0].equals("DPadUp")) {
                                j.dPadUp = Integer.parseInt(split[1]);
                            } else if (split[0].equals("DPadDown")) {
                                j.dPadDown = Integer.parseInt(split[1]);
                            } else if (split[0].equals("DPadLeft")) {
                                j.dPadLeft = Integer.parseInt(split[1]);
                            } else if (split[0].equals("DPadRight")) {
                                j.dPadRight = Integer.parseInt(split[1]);
                            } else if (split[0].equals("TriggersFlipped")) {
                                j.triggersFlipped = split[1].equals("true");
                            } else if (split[0].equals("TriggerLeft")) {
                                j.triggerLeft = Integer.parseInt(split[1]);
                            } else if (split[0].equals("TriggerRight")) {
                                j.triggerRight = Integer.parseInt(split[1]);
                            } else if (split[0].equals("Disabled")) {
                                j.disabled = split[1].equals("true");
                            } else if (split[0].equals("Sensitivity")) {
                                j.setDeadZone(Float.parseFloat(split[1]));
                            }
                        }
                    }
                }
            } catch (Exception var10) {
                ExceptionLogger.logException(var10);
            }
        } catch (FileNotFoundException var13) {
        } catch (IOException var14) {
            ExceptionLogger.logException(var14);
        }

        this.saveFile(j);
    }

    private void saveFile(JoypadManager.Joypad j) {
        File file = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "joypads");
        if (!file.exists()) {
            file.mkdir();
        }

        file = new File(ZomboidFileSystem.instance.getCacheDirSub("joypads" + File.separator + j.guid + ".config"));

        try (
            FileWriter fileWriter = new FileWriter(file.getAbsolutePath());
            BufferedWriter bw = new BufferedWriter(fileWriter);
        ) {
            String lineSep = System.getProperty("line.separator");
            bw.write("Version=2" + lineSep);
            bw.write("Name=" + j.name + lineSep);
            bw.write("MovementAxisX=" + j.movementAxisX + lineSep);
            bw.write("MovementAxisXFlipped=" + j.movementAxisXFlipped + lineSep);
            bw.write("MovementAxisY=" + j.movementAxisY + lineSep);
            bw.write("MovementAxisYFlipped=" + j.movementAxisYFlipped + lineSep);
            bw.write("// Set the dead zone to the smallest number between 0.0 and 1.0." + lineSep);
            bw.write("// This is to fix \"loose sticks\"." + lineSep);
            bw.write("MovementAxisDeadZone=" + j.movementAxisDeadZone + lineSep);
            bw.write("AimingAxisX=" + j.aimingAxisX + lineSep);
            bw.write("AimingAxisXFlipped=" + j.aimingAxisXFlipped + lineSep);
            bw.write("AimingAxisY=" + j.aimingAxisY + lineSep);
            bw.write("AimingAxisYFlipped=" + j.aimingAxisYFlipped + lineSep);
            bw.write("AimingAxisDeadZone=" + j.aimingAxisDeadZone + lineSep);
            bw.write("AButton=" + j.aButton + lineSep);
            bw.write("BButton=" + j.bButton + lineSep);
            bw.write("XButton=" + j.xButton + lineSep);
            bw.write("YButton=" + j.yButton + lineSep);
            bw.write("LBumper=" + j.bumperLeft + lineSep);
            bw.write("RBumper=" + j.bumperRight + lineSep);
            bw.write("L3=" + j.leftStickButton + lineSep);
            bw.write("R3=" + j.rightStickButton + lineSep);
            bw.write("Back=" + j.back + lineSep);
            bw.write("Start=" + j.start + lineSep);
            bw.write("// Normally the D-pad is treated as a single axis (the POV Hat), and these should be -1." + lineSep);
            bw.write("// If your D-pad is actually 4 separate buttons, set the button numbers here." + lineSep);
            bw.write("DPadUp=" + j.dPadUp + lineSep);
            bw.write("DPadDown=" + j.dPadDown + lineSep);
            bw.write("DPadLeft=" + j.dPadLeft + lineSep);
            bw.write("DPadRight=" + j.dPadRight + lineSep);
            bw.write("TriggersFlipped=" + j.triggersFlipped + lineSep);
            bw.write("// If your triggers are buttons, set the button numbers here." + lineSep);
            bw.write("// If these are set to something other than -1, then Triggers= is ignored." + lineSep);
            bw.write("TriggerLeft=" + j.triggerLeft + lineSep);
            bw.write("TriggerRight=" + j.triggerRight + lineSep);
            bw.write("Disabled=" + j.disabled + lineSep);
            bw.write("Sensitivity=" + j.getDeadZone(0) + lineSep);
        } catch (IOException var11) {
            ExceptionLogger.logException(var11);
        }
    }

    public void reloadControllerFiles() {
        for (int i = 0; i < GameWindow.GameInput.getControllerCount(); i++) {
            Controller controller = GameWindow.GameInput.getController(i);
            if (controller != null) {
                if (this.joypadsController[i] == null) {
                    this.addJoypad(i, controller.getGUID(), controller.getGamepadName());
                } else {
                    this.doControllerFile(this.joypadsController[i]);
                }
            }
        }
    }

    public void assignJoypad(int joypadIndex, int player) {
        this.checkJoypad(joypadIndex);
        this.joypads[player] = this.joypadsController[joypadIndex];
        this.joypads[player].player = player;
    }

    public JoypadManager.Joypad getFromPlayer(int player) {
        return this.joypads[player];
    }

    public JoypadManager.Joypad getFromControllerID(int id) {
        return this.joypadsController[id];
    }

    public void onPressed(int joypadIndex, int buttonIdx) {
        this.checkJoypad(joypadIndex);
        this.joypadsController[joypadIndex].onPressed(buttonIdx);
    }

    private boolean anyJoypad(Predicate<JoypadManager.Joypad> predicate) {
        for (JoypadManager.Joypad joypad : this.joypadList) {
            if (predicate.test(joypad)) {
                return true;
            }
        }

        return false;
    }

    private boolean checkJoypad(int joypadIndex, Predicate<JoypadManager.Joypad> predicate) {
        JoypadManager.Joypad joypad = this.checkJoypad(joypadIndex);
        return joypad != null && predicate.test(joypad);
    }

    private boolean checkOrAnyJoypad(int joypadIndex, Predicate<JoypadManager.Joypad> predicate) {
        return joypadIndex < 0 ? this.anyJoypad(predicate) : this.checkJoypad(joypadIndex, predicate);
    }

    private boolean isButtonStartPress(int joypadIndex, Function<JoypadManager.Joypad, Integer> button) {
        JoypadManager.Joypad joypad = this.checkJoypad(joypadIndex);
        return joypad != null && joypad.isButtonStartPress(button.apply(joypad));
    }

    private boolean isButtonReleasePress(int joypadIndex, Function<JoypadManager.Joypad, Integer> button) {
        JoypadManager.Joypad joypad = this.checkJoypad(joypadIndex);
        return joypad != null && joypad.isButtonReleasePress(button.apply(joypad));
    }

    public boolean isMovementAxisBeingApplied(int joypadIndex) {
        return this.checkJoypad(joypadIndex, JoypadManager.Joypad::isMovementAxisBeingApplied);
    }

    public float getJoypadAxis(int joypadIndex, Predicate<JoypadManager.Joypad> predicate, Function<JoypadManager.Joypad, Float> getAxis) {
        JoypadManager.Joypad joypad = this.checkJoypad(joypadIndex);
        return joypad != null && predicate.test(joypad) ? getAxis.apply(joypad) : 0.0F;
    }

    public void setJoypadAxis(int joypadIndex, Consumers.Params1.ICallback<JoypadManager.Joypad, Float> setAxis, float newValue) {
        JoypadManager.Joypad joypad = this.checkJoypad(joypadIndex);
        if (joypad != null) {
            setAxis.accept(joypad, newValue);
        }
    }

    public boolean isAimingAxisBeingApplied(int joypadIndex) {
        return this.checkJoypad(joypadIndex, JoypadManager.Joypad::isAimingAxisBeingApplied);
    }

    public boolean isDownPressed(int c) {
        return this.checkJoypad(c, JoypadManager.Joypad::isDownPressed);
    }

    public boolean isUpPressed(int c) {
        return this.checkJoypad(c, JoypadManager.Joypad::isUpPressed);
    }

    public boolean isRightPressed(int c) {
        return this.checkJoypad(c, JoypadManager.Joypad::isRightPressed);
    }

    public boolean isLeftPressed(int c) {
        return this.checkJoypad(c, JoypadManager.Joypad::isLeftPressed);
    }

    public boolean isLBPressed(int c) {
        return this.checkJoypad(c, JoypadManager.Joypad::isLBPressed);
    }

    public boolean isRBPressed(int c) {
        return this.checkJoypad(c, JoypadManager.Joypad::isRBPressed);
    }

    public boolean isL3Pressed(int c) {
        return this.checkOrAnyJoypad(c, JoypadManager.Joypad::isL3Pressed);
    }

    public boolean isR3Pressed(int c) {
        return this.checkOrAnyJoypad(c, JoypadManager.Joypad::isR3Pressed);
    }

    public boolean isRTPressed(int c) {
        return this.checkOrAnyJoypad(c, JoypadManager.Joypad::isRTPressed);
    }

    public boolean isLTPressed(int c) {
        return this.checkOrAnyJoypad(c, JoypadManager.Joypad::isLTPressed);
    }

    public float getRTValue(int c) {
        return this.getJoypadAxis(c, var0 -> true, JoypadManager.Joypad::getRTValue);
    }

    public float getLTValue(int c) {
        return this.getJoypadAxis(c, var0 -> true, JoypadManager.Joypad::getLTValue);
    }

    public boolean isAPressed(int c) {
        return this.checkOrAnyJoypad(c, JoypadManager.Joypad::isAPressed);
    }

    public boolean isBPressed(int c) {
        return this.checkOrAnyJoypad(c, JoypadManager.Joypad::isBPressed);
    }

    public boolean isXPressed(int c) {
        return this.checkOrAnyJoypad(c, JoypadManager.Joypad::isXPressed);
    }

    public boolean isYPressed(int c) {
        return this.checkOrAnyJoypad(c, JoypadManager.Joypad::isYPressed);
    }

    public boolean isAButtonStartPress(int c) {
        return this.isButtonStartPress(c, JoypadManager.Joypad::getAButton);
    }

    public boolean isBButtonStartPress(int c) {
        return this.isButtonStartPress(c, JoypadManager.Joypad::getBButton);
    }

    public boolean isXButtonStartPress(int c) {
        return this.isButtonStartPress(c, JoypadManager.Joypad::getXButton);
    }

    public boolean isYButtonStartPress(int c) {
        return this.isButtonStartPress(c, JoypadManager.Joypad::getYButton);
    }

    public boolean isAButtonReleasePress(int c) {
        return this.isButtonReleasePress(c, JoypadManager.Joypad::getAButton);
    }

    public boolean isBButtonReleasePress(int c) {
        return this.isButtonReleasePress(c, JoypadManager.Joypad::getBButton);
    }

    public boolean isXButtonReleasePress(int c) {
        return this.isButtonReleasePress(c, JoypadManager.Joypad::getXButton);
    }

    public boolean isYButtonReleasePress(int c) {
        return this.isButtonReleasePress(c, JoypadManager.Joypad::getYButton);
    }

    public boolean isBackPressed(int joypadBind) {
        return this.checkOrAnyJoypad(joypadBind, JoypadManager.Joypad::isBackPressed);
    }

    public boolean isStartPressed(int joypadBind) {
        return this.checkOrAnyJoypad(joypadBind, JoypadManager.Joypad::isStartPressed);
    }

    public boolean isGuidePressed(int joypadBind) {
        return this.checkOrAnyJoypad(joypadBind, JoypadManager.Joypad::isGuidePressed);
    }

    public float getMovementAxisX(int joypadIndex) {
        return this.getJoypadAxis(joypadIndex, JoypadManager.Joypad::isMovementAxisBeingApplied, JoypadManager.Joypad::getMovementAxisX);
    }

    public float getMovementAxisY(int joypadIndex) {
        return this.getJoypadAxis(joypadIndex, JoypadManager.Joypad::isMovementAxisBeingApplied, JoypadManager.Joypad::getMovementAxisY);
    }

    public Vector2 getMovementAxis(int joypadIndex, Vector2 out) {
        return out.set(
            this.getJoypadAxis(joypadIndex, JoypadManager.Joypad::isMovementAxisBeingApplied, JoypadManager.Joypad::getMovementAxisX),
            this.getJoypadAxis(joypadIndex, JoypadManager.Joypad::isMovementAxisBeingApplied, JoypadManager.Joypad::getMovementAxisY)
        );
    }

    public float getAimingAxisX(int joypadIndex) {
        return this.getJoypadAxis(joypadIndex, JoypadManager.Joypad::isAimingAxisBeingApplied, JoypadManager.Joypad::getAimingAxisX);
    }

    public float getAimingAxisY(int joypadIndex) {
        return this.getJoypadAxis(joypadIndex, JoypadManager.Joypad::isAimingAxisBeingApplied, JoypadManager.Joypad::getAimingAxisY);
    }

    public Vector2 getAimingAxis(int joypadIndex, Vector2 out) {
        return out.set(
            this.getJoypadAxis(joypadIndex, JoypadManager.Joypad::isAimingAxisBeingApplied, JoypadManager.Joypad::getAimingAxisX),
            this.getJoypadAxis(joypadIndex, JoypadManager.Joypad::isAimingAxisBeingApplied, JoypadManager.Joypad::getAimingAxisY)
        );
    }

    public void onPressedAxis(int joypadIndex, int i) {
        this.checkJoypad(joypadIndex);
        this.joypadsController[joypadIndex].onPressedAxis(i);
    }

    public void onPressedAxisNeg(int joypadIndex, int i) {
        this.checkJoypad(joypadIndex);
        this.joypadsController[joypadIndex].onPressedAxisNeg(i);
    }

    public void onPressedTrigger(int joypadIndex, int i) {
        this.checkJoypad(joypadIndex);
        this.joypadsController[joypadIndex].onPressedTrigger(i);
    }

    public void onPressedPov(int joypadIndex) {
        this.checkJoypad(joypadIndex);
        this.joypadsController[joypadIndex].onPressedPov();
    }

    public float getDeadZone(int joypadIndex, int axis) {
        JoypadManager.Joypad joypad = this.checkJoypad(joypadIndex);
        return joypad == null ? 0.0F : joypad.getDeadZone(axis);
    }

    public void setDeadZone(int joypadIndex, int axis, float value) {
        this.checkJoypad(joypadIndex);
        this.joypadsController[joypadIndex].setDeadZone(axis, value);
    }

    public float getLeftTriggerDeadZone(int joypadIndex) {
        return this.getJoypadAxis(joypadIndex, var0 -> true, JoypadManager.Joypad::getLTDeadZone);
    }

    public void setLeftTriggerDeadZone(int joypadIndex, float newValue) {
        this.setJoypadAxis(joypadIndex, JoypadManager.Joypad::setLTDeadZone, newValue);
    }

    public float getRightTriggerDeadZone(int joypadIndex) {
        return this.getJoypadAxis(joypadIndex, var0 -> true, JoypadManager.Joypad::getRTDeadZone);
    }

    public void setRightTriggerDeadZone(int joypadIndex, float newValue) {
        this.setJoypadAxis(joypadIndex, JoypadManager.Joypad::setRTDeadZone, newValue);
    }

    public float getMovementAxisDeadZoneX(int joypadIndex) {
        return this.getJoypadAxis(joypadIndex, var0 -> true, JoypadManager.Joypad::getMovementAxisDeadZoneX);
    }

    public void setMovementAxisDeadZoneX(int joypadIndex, float newValue) {
        this.setJoypadAxis(joypadIndex, JoypadManager.Joypad::setMovementAxisDeadZoneX, newValue);
    }

    public float getMovementAxisDeadZoneY(int joypadIndex) {
        return this.getJoypadAxis(joypadIndex, var0 -> true, JoypadManager.Joypad::getMovementAxisDeadZoneY);
    }

    public void setMovementAxisDeadZoneY(int joypadIndex, float newValue) {
        this.setJoypadAxis(joypadIndex, JoypadManager.Joypad::setMovementAxisDeadZoneY, newValue);
    }

    public float getAimingAxisDeadZoneX(int joypadIndex) {
        return this.getJoypadAxis(joypadIndex, var0 -> true, JoypadManager.Joypad::getAimingAxisDeadZoneX);
    }

    public void setAimingAxisDeadZoneX(int joypadIndex, float newValue) {
        this.setJoypadAxis(joypadIndex, JoypadManager.Joypad::setAimingAxisDeadZoneX, newValue);
    }

    public float getAimingAxisDeadZoneY(int joypadIndex) {
        return this.getJoypadAxis(joypadIndex, var0 -> true, JoypadManager.Joypad::getAimingAxisDeadZoneY);
    }

    public void setAimingAxisDeadZoneY(int joypadIndex, float newValue) {
        this.setJoypadAxis(joypadIndex, JoypadManager.Joypad::setAimingAxisDeadZoneY, newValue);
    }

    public void saveControllerSettings(int joypadIndex) {
        this.checkJoypad(joypadIndex);
        this.saveFile(this.joypadsController[joypadIndex]);
    }

    public long getLastActivity(int joypadIndex) {
        return this.joypadsController[joypadIndex] == null ? 0L : this.joypadsController[joypadIndex].lastActivity;
    }

    public void setControllerActive(String guid, boolean active) {
        if (active) {
            this.activeControllerGuids.add(guid);
        } else {
            this.activeControllerGuids.remove(guid);
        }

        this.syncActiveControllers();
    }

    public void syncActiveControllers() {
        this.joypadList.clear();

        for (JoypadManager.Joypad joypad : this.joypadsController) {
            if (joypad != null && !joypad.isDisabled() && this.activeControllerGuids.contains(joypad.guid)) {
                this.joypadList.add(joypad);
            }
        }
    }

    public boolean isJoypadConnected(int index) {
        if (index >= 0 && index < 16) {
            assert Thread.currentThread() == GameWindow.gameThread;

            return GameWindow.GameInput.getController(index) != null;
        } else {
            return false;
        }
    }

    public void onControllerConnected(Controller controller) {
        JoypadManager.Joypad joypad = this.joypadsController[controller.getID()];
        if (joypad != null) {
            LuaEventManager.triggerEvent("OnJoypadBeforeReactivate", BoxedStaticValues.toDouble(joypad.getID()));
            joypad.connected = true;
            LuaEventManager.triggerEvent("OnJoypadReactivate", BoxedStaticValues.toDouble(joypad.getID()));
        }
    }

    public void onControllerDisconnected(Controller controller) {
        JoypadManager.Joypad joypad = this.joypadsController[controller.getID()];
        if (joypad != null) {
            LuaEventManager.triggerEvent("OnJoypadBeforeDeactivate", BoxedStaticValues.toDouble(joypad.getID()));
            joypad.connected = false;
            LuaEventManager.triggerEvent("OnJoypadDeactivate", BoxedStaticValues.toDouble(joypad.getID()));
        }
    }

    public void revertToKeyboardAndMouseFromMainMenu() {
        if (GameWindow.activatedJoyPad != null) {
            GameWindow.activatedJoyPad = null;
        }
    }

    public void revertToKeyboardAndMouse() {
        for (int i = 0; i < this.joypadList.size(); i++) {
            JoypadManager.Joypad joypad = this.joypadList.get(i);
            if (joypad.player == 0) {
                if (GameWindow.activatedJoyPad == joypad) {
                    GameWindow.activatedJoyPad = null;
                }

                IsoPlayer player = IsoPlayer.players[0];
                if (player != null) {
                    player.setJoypadBind(-1);
                }

                this.joypadsController[joypad.getID()] = null;
                this.joypads[0] = null;
                this.joypadList.remove(i);
                break;
            }
        }
    }

    public void renderUI() {
        assert Thread.currentThread() == GameWindow.gameThread;

        if (!GameWindow.drawReloadingLua) {
            boolean debugDrawUI = DebugOptions.instance.joypadRenderUi.getValue();
            boolean debugDrawNavigation = DebugOptions.instance.joypadRenderUiNavigation.getValue();
            if (debugDrawUI || debugDrawNavigation) {
                LuaEventManager.triggerEvent("OnJoypadDebugRenderUIOptionSet", debugDrawUI, debugDrawNavigation);
                LuaEventManager.triggerEvent("OnJoypadRenderUI");
            }
        }
    }

    public void Reset() {
        Arrays.fill(this.joypads, null);
    }

    public static final class Joypad {
        String guid;
        String name;
        int id;
        int player = -1;
        int movementAxisX = 0;
        boolean movementAxisXFlipped;
        int movementAxisY = 1;
        boolean movementAxisYFlipped;
        float movementAxisDeadZone;
        int aimingAxisX = 2;
        boolean aimingAxisXFlipped;
        int aimingAxisY = 3;
        boolean aimingAxisYFlipped;
        float aimingAxisDeadZone;
        int aButton = 0;
        int bButton = 1;
        int xButton = 2;
        int yButton = 3;
        int dPadUp = -1;
        int dPadDown = -1;
        int dPadLeft = -1;
        int dPadRight = -1;
        int bumperLeft = 4;
        int bumperRight = 5;
        int back = 6;
        int start = 7;
        int guide = 8;
        int leftStickButton = 9;
        int rightStickButton = 10;
        boolean triggersFlipped;
        int triggerLeft = 4;
        int triggerRight = 5;
        boolean disabled;
        boolean connected = true;
        long lastActivity;
        private static final Vector2 tempVec2 = new Vector2();

        public boolean isDownPressed() {
            return this.dPadDown != -1 ? GameWindow.GameInput.isButtonPressedD(this.dPadDown, this.id) : GameWindow.GameInput.isControllerDownD(this.id);
        }

        public boolean isUpPressed() {
            return this.dPadUp != -1 ? GameWindow.GameInput.isButtonPressedD(this.dPadUp, this.id) : GameWindow.GameInput.isControllerUpD(this.id);
        }

        public boolean isRightPressed() {
            return this.dPadRight != -1 ? GameWindow.GameInput.isButtonPressedD(this.dPadRight, this.id) : GameWindow.GameInput.isControllerRightD(this.id);
        }

        public boolean isLeftPressed() {
            return this.dPadLeft != -1 ? GameWindow.GameInput.isButtonPressedD(this.dPadLeft, this.id) : GameWindow.GameInput.isControllerLeftD(this.id);
        }

        public boolean isLBPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.bumperLeft, this.id);
        }

        public boolean isRBPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.bumperRight, this.id);
        }

        public boolean isL3Pressed() {
            return GameWindow.GameInput.isButtonPressedD(this.leftStickButton, this.id);
        }

        public boolean isR3Pressed() {
            return GameWindow.GameInput.isButtonPressedD(this.rightStickButton, this.id);
        }

        public boolean isRTPressed() {
            return this.getRTValue() > 0.7F;
        }

        public boolean isLTPressed() {
            return this.getLTValue() > 0.7F;
        }

        public float getRTValue() {
            int trigger = this.triggerRight;
            if (GameWindow.GameInput.getAxisCount(this.id) <= trigger) {
                return this.isRBPressed() ? 1.0F : -1.0F;
            } else {
                float axisDeadZone = GameWindow.GameInput.getController(this.id).getDeadZone(trigger);
                float axisRaw = this.triggersFlipped
                    ? -GameWindow.GameInput.getAxisValue(this.id, trigger)
                    : GameWindow.GameInput.getAxisValue(this.id, trigger);
                float axisMax = 1.0F;
                float axisMin = -1.0F + axisDeadZone;
                float axisLength = 2.0F;
                float normalizedAxis = 2.0F * (axisRaw - axisMin) / (1.0F - axisMin) - 1.0F;
                return PZMath.clamp(normalizedAxis, -1.0F, 1.0F);
            }
        }

        public float getLTValue() {
            int trigger = this.triggerLeft;
            if (GameWindow.GameInput.getAxisCount(this.id) <= trigger) {
                return this.isLBPressed() ? 1.0F : -1.0F;
            } else {
                float axisDeadZone = GameWindow.GameInput.getController(this.id).getDeadZone(trigger);
                float axisRaw = this.triggersFlipped
                    ? -GameWindow.GameInput.getAxisValue(this.id, trigger)
                    : GameWindow.GameInput.getAxisValue(this.id, trigger);
                float axisMax = 1.0F;
                float axisMin = -1.0F + axisDeadZone;
                float axisLength = 2.0F;
                float normalizedAxis = 2.0F * (axisRaw - axisMin) / (1.0F - axisMin) - 1.0F;
                return PZMath.clamp(normalizedAxis, -1.0F, 1.0F);
            }
        }

        public boolean isAPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.aButton, this.id);
        }

        public boolean isBPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.bButton, this.id);
        }

        public boolean isXPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.xButton, this.id);
        }

        public boolean isYPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.yButton, this.id);
        }

        public boolean isBackPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.back, this.id);
        }

        public boolean isStartPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.start, this.id);
        }

        public boolean isGuidePressed() {
            return GameWindow.GameInput.isButtonPressedD(this.guide, this.id);
        }

        public boolean isButtonPressed(int button) {
            return GameWindow.GameInput.isButtonPressedD(button, this.id);
        }

        public boolean wasButtonPressed(int button) {
            return GameWindow.GameInput.wasButtonPressed(this.id, button);
        }

        public boolean isButtonStartPress(int button) {
            return GameWindow.GameInput.isButtonStartPress(this.id, button);
        }

        public boolean isButtonReleasePress(int button) {
            return GameWindow.GameInput.isButtonReleasePress(this.id, button);
        }

        public float getMovementAxisX() {
            float axisXraw = this.getMovementAxisXRaw();
            if (PZMath.equal(axisXraw, 0.0F)) {
                return 0.0F;
            } else {
                float axisYraw = this.getMovementAxisYRaw();
                if (PZMath.equal(axisYraw, 0.0F)) {
                    return axisXraw;
                } else {
                    float squaringScaler = getAxisSquaringScaler(axisXraw, axisYraw);
                    return PZMath.clamp(axisXraw * squaringScaler, -1.0F, 1.0F);
                }
            }
        }

        public float getMovementAxisXRaw() {
            if (GameWindow.GameInput.getAxisCount(this.id) <= this.movementAxisX) {
                return 0.0F;
            } else {
                this.movementAxisDeadZone = GameWindow.GameInput.getController(this.id).getDeadZone(this.movementAxisX);
                float deadZone = this.movementAxisDeadZone;
                if (deadZone > 0.0F && deadZone < 1.0F) {
                    float xAxis = GameWindow.GameInput.getAxisValue(this.id, this.movementAxisX);
                    float yAxis = GameWindow.GameInput.getAxisValue(this.id, this.movementAxisY);
                    Vector2 stickInput = tempVec2.set(xAxis, yAxis);
                    if (stickInput.getLength() < deadZone) {
                        stickInput.set(0.0F, 0.0F);
                    } else {
                        stickInput.setLength((stickInput.getLength() - deadZone) / (1.0F - deadZone));
                    }

                    return this.movementAxisXFlipped ? -stickInput.getX() : stickInput.getX();
                } else {
                    return this.movementAxisXFlipped
                        ? -GameWindow.GameInput.getAxisValue(this.id, this.movementAxisX)
                        : GameWindow.GameInput.getAxisValue(this.id, this.movementAxisX);
                }
            }
        }

        public float getMovementAxisY() {
            float axisYraw = this.getMovementAxisYRaw();
            if (PZMath.equal(axisYraw, 0.0F)) {
                return 0.0F;
            } else {
                float axisXraw = this.getMovementAxisXRaw();
                if (PZMath.equal(axisXraw, 0.0F)) {
                    return axisYraw;
                } else {
                    float squaringScaler = getAxisSquaringScaler(axisXraw, axisYraw);
                    return PZMath.clamp(axisYraw * squaringScaler, -1.0F, 1.0F);
                }
            }
        }

        public float getMovementAxisYRaw() {
            if (GameWindow.GameInput.getAxisCount(this.id) <= this.movementAxisY) {
                return 0.0F;
            } else {
                this.movementAxisDeadZone = GameWindow.GameInput.getController(this.id).getDeadZone(this.movementAxisY);
                float deadZone = this.movementAxisDeadZone;
                if (deadZone > 0.0F && deadZone < 1.0F) {
                    float xAxis = GameWindow.GameInput.getAxisValue(this.id, this.movementAxisX);
                    float yAxis = GameWindow.GameInput.getAxisValue(this.id, this.movementAxisY);
                    Vector2 stickInput = tempVec2.set(xAxis, yAxis);
                    if (stickInput.getLength() < deadZone) {
                        stickInput.set(0.0F, 0.0F);
                    } else {
                        stickInput.setLength((stickInput.getLength() - deadZone) / (1.0F - deadZone));
                    }

                    return this.movementAxisYFlipped ? -stickInput.getY() : stickInput.getY();
                } else {
                    return this.movementAxisYFlipped
                        ? -GameWindow.GameInput.getAxisValue(this.id, this.movementAxisY)
                        : GameWindow.GameInput.getAxisValue(this.id, this.movementAxisY);
                }
            }
        }

        public float getAimingAxisX() {
            float axisXraw = this.getAimingAxisXRaw();
            if (PZMath.equal(axisXraw, 0.0F)) {
                return 0.0F;
            } else {
                float axisYraw = this.getAimingAxisYRaw();
                if (PZMath.equal(axisYraw, 0.0F)) {
                    return axisXraw;
                } else {
                    float squaringScaler = getAxisSquaringScaler(axisXraw, axisYraw);
                    return PZMath.clamp(axisXraw * squaringScaler, -1.0F, 1.0F);
                }
            }
        }

        public float getAimingAxisXRaw() {
            if (GameWindow.GameInput.getAxisCount(this.id) <= this.aimingAxisX) {
                return 0.0F;
            } else {
                this.aimingAxisDeadZone = GameWindow.GameInput.getController(this.id).getDeadZone(this.aimingAxisX);
                float deadZone = this.aimingAxisDeadZone;
                if (deadZone > 0.0F && deadZone < 1.0F) {
                    float xAxis = GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisX);
                    float yAxis = GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisY);
                    Vector2 stickInput = tempVec2.set(xAxis, yAxis);
                    if (stickInput.getLength() < deadZone) {
                        stickInput.set(0.0F, 0.0F);
                    } else {
                        stickInput.setLength((stickInput.getLength() - deadZone) / (1.0F - deadZone));
                    }

                    return this.aimingAxisXFlipped ? -stickInput.getX() : stickInput.getX();
                } else {
                    return this.aimingAxisXFlipped
                        ? -GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisX)
                        : GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisX);
                }
            }
        }

        public float getAimingAxisY() {
            float axisYraw = this.getAimingAxisYRaw();
            if (PZMath.equal(axisYraw, 0.0F)) {
                return 0.0F;
            } else {
                float axisXraw = this.getAimingAxisXRaw();
                if (PZMath.equal(axisXraw, 0.0F)) {
                    return axisYraw;
                } else {
                    float squaringScaler = getAxisSquaringScaler(axisXraw, axisYraw);
                    return PZMath.clamp(axisYraw * squaringScaler, -1.0F, 1.0F);
                }
            }
        }

        public float getAimingAxisYRaw() {
            if (GameWindow.GameInput.getAxisCount(this.id) <= this.aimingAxisY) {
                return 0.0F;
            } else {
                this.aimingAxisDeadZone = GameWindow.GameInput.getController(this.id).getDeadZone(this.aimingAxisY);
                float deadZone = this.aimingAxisDeadZone;
                if (deadZone > 0.0F && deadZone < 1.0F) {
                    float xAxis = GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisX);
                    float yAxis = GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisY);
                    Vector2 stickInput = tempVec2.set(xAxis, yAxis);
                    if (stickInput.getLength() < deadZone) {
                        stickInput.set(0.0F, 0.0F);
                    } else {
                        stickInput.setLength((stickInput.getLength() - deadZone) / (1.0F - deadZone));
                    }

                    return this.aimingAxisYFlipped ? -stickInput.getY() : stickInput.getY();
                } else {
                    return this.aimingAxisYFlipped
                        ? -GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisY)
                        : GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisY);
                }
            }
        }

        private static float getAxisSquaringScaler(float axisXraw, float axisYraw) {
            float axisLength = PZMath.getLength(axisXraw, axisYraw);
            if (PZMath.equal(axisLength, 0.0F)) {
                return 1.0F;
            } else {
                float axisXnorm = axisXraw / axisLength;
                float axisYnorm = axisYraw / axisLength;
                float axisXAbsnorm = PZMath.abs(axisXnorm);
                float axisYAbsnorm = PZMath.abs(axisYnorm);
                float diagX = 0.70710677F;
                float diagY = 0.70710677F;
                float angleFromDiag = PZMath.angleBetweenNormalized(0.70710677F, axisXAbsnorm, 0.70710677F, axisYAbsnorm);
                float angleFromDiagAbs = PZMath.abs(angleFromDiag);
                float multiplierAlpha = PZMath.clamp(1.0F - angleFromDiagAbs * 4.0F / (float) Math.PI, 0.0F, 1.0F);
                return PZMath.lerp(1.0F, 1.4142F, multiplierAlpha);
            }
        }

        public void onPressed(int i) {
            this.lastActivity = System.currentTimeMillis();
        }

        public void onPressedAxis(int i) {
            this.lastActivity = System.currentTimeMillis();
        }

        public void onPressedAxisNeg(int i) {
            this.lastActivity = System.currentTimeMillis();
        }

        public void onPressedTrigger(int i) {
            this.lastActivity = System.currentTimeMillis();
        }

        public void onPressedPov() {
            this.lastActivity = System.currentTimeMillis();
        }

        public float getMovementAxisDeadZoneX() {
            return this.getDeadZone(this.movementAxisX);
        }

        public float getMovementAxisDeadZoneY() {
            return this.getDeadZone(this.movementAxisY);
        }

        public float getAimingAxisDeadZoneX() {
            return this.getDeadZone(this.aimingAxisX);
        }

        public float getAimingAxisDeadZoneY() {
            return this.getDeadZone(this.aimingAxisY);
        }

        public void setMovementAxisDeadZoneX(float newValue) {
            this.setDeadZone(this.movementAxisX, newValue);
        }

        public void setMovementAxisDeadZoneY(float newValue) {
            this.setDeadZone(this.movementAxisY, newValue);
        }

        public void setAimingAxisDeadZoneX(float newValue) {
            this.setDeadZone(this.aimingAxisX, newValue);
        }

        public void setAimingAxisDeadZoneY(float newValue) {
            this.setDeadZone(this.aimingAxisY, newValue);
        }

        public float getLTDeadZone() {
            return this.getDeadZone(this.triggerLeft);
        }

        public void setLTDeadZone(float newValue) {
            this.setDeadZone(this.triggerLeft, newValue);
        }

        public float getRTDeadZone() {
            return this.getDeadZone(this.triggerRight);
        }

        public void setRTDeadZone(float newValue) {
            this.setDeadZone(this.triggerRight, newValue);
        }

        public float getDeadZone(int axis) {
            float deadZoneFromSettings = 0.0F;
            if ((axis == this.movementAxisX || axis == this.movementAxisY) && this.movementAxisDeadZone > 0.0F && this.movementAxisDeadZone < 1.0F) {
                deadZoneFromSettings = this.movementAxisDeadZone;
            }

            if ((axis == this.aimingAxisX || axis == this.aimingAxisY) && this.aimingAxisDeadZone > 0.0F && this.aimingAxisDeadZone < 1.0F) {
                deadZoneFromSettings = this.aimingAxisDeadZone;
            }

            if (axis >= 0 && axis < GameWindow.GameInput.getAxisCount(this.id)) {
                float deadZoneFromController = GameWindow.GameInput.getController(this.id).getDeadZone(axis);
                return Math.max(deadZoneFromController, deadZoneFromSettings);
            } else {
                return deadZoneFromSettings;
            }
        }

        public void setDeadZone(int axis, float value) {
            if (axis >= 0 && axis < GameWindow.GameInput.getAxisCount(this.id)) {
                GameWindow.GameInput.getController(this.id).setDeadZone(axis, value);
            }
        }

        public void setDeadZone(float value) {
            for (int axis = 0; axis < GameWindow.GameInput.getAxisCount(this.id); axis++) {
                GameWindow.GameInput.getController(this.id).setDeadZone(axis, value);
            }
        }

        public int getID() {
            return this.id;
        }

        public boolean isDisabled() {
            return this.disabled;
        }

        public int getAButton() {
            return this.aButton;
        }

        public int getBButton() {
            return this.bButton;
        }

        public int getXButton() {
            return this.xButton;
        }

        public int getYButton() {
            return this.yButton;
        }

        public int getLBumper() {
            return this.bumperLeft;
        }

        public int getRBumper() {
            return this.bumperRight;
        }

        public int getL3() {
            return this.leftStickButton;
        }

        public int getR3() {
            return this.rightStickButton;
        }

        public int getBackButton() {
            return this.back;
        }

        public int getStartButton() {
            return this.start;
        }

        public boolean isMovementInsideDeadZone(float x, float y) {
            float movementDistanceSq = x * x + y * y;
            float deadZoneX = this.getMovementAxisDeadZoneX();
            float deadZoneY = this.getMovementAxisDeadZoneY();
            float deadZoneSq = deadZoneX * deadZoneY;
            return movementDistanceSq < deadZoneSq;
        }

        public boolean isMovementAxisBeingApplied() {
            float moveX = this.getMovementAxisX();
            float moveY = this.getMovementAxisY();
            return !this.isMovementInsideDeadZone(moveX, moveY);
        }

        public boolean isAimingInsideDeadZone(float x, float y) {
            float aimDistanceSq = x * x + y * y;
            float deadZoneX = this.getAimingAxisDeadZoneX();
            float deadZoneY = this.getAimingAxisDeadZoneY();
            float deadZoneSq = deadZoneX * deadZoneX + deadZoneY * deadZoneY;
            return aimDistanceSq < deadZoneSq;
        }

        public boolean isAimingAxisBeingApplied() {
            float aimX = this.getAimingAxisX();
            float aimY = this.getAimingAxisY();
            return !this.isAimingInsideDeadZone(aimX, aimY);
        }
    }
}
