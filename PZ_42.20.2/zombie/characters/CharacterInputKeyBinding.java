// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.ui.UIManager;

public enum CharacterInputKeyBinding {
    Forward("Forward"),
    Backward("Backward"),
    Left("Left"),
    Right("Right"),
    Aim("Aim"),
    Melee("Melee"),
    Attack("Attack/Click"),
    Run("Run"),
    Interact("Interact"),
    WalkTo("WalkTo"),
    Crouch("Crouch"),
    ReloadWeapon("ReloadWeapon"),
    RackFirearm("Rack Firearm"),
    Sprint("Sprint"),
    CancelAction("CancelAction"),
    ManualFloorAtk("ManualFloorAtk"),
    ZoomIn("Zoom in"),
    ZoomOut("Zoom out");

    private final String key;

    private CharacterInputKeyBinding(final String key) {
        this.key = key;
    }

    public boolean isKeyDown() {
        return GameKeyboard.whichKeyDown(this.key) != 0;
    }

    public boolean isKeyDown(int keyCode) {
        return GameKeyboard.whichKeyDown(this.key) == keyCode;
    }

    public boolean wasKeyDown() {
        return GameKeyboard.wasKeyDown(this.key);
    }

    public int whichKeyDown() {
        return GameKeyboard.whichKeyDown(this.key);
    }

    public boolean isKeyPressed() {
        return GameKeyboard.isKeyPressed(this.key);
    }

    public boolean isMouseKey() {
        return Mouse.isButtonKey(this.whichKeyDown());
    }

    public boolean isCtrlKey() {
        int whichKeyDown = this.whichKeyDown();
        return whichKeyDown == 29 || whichKeyDown == 157;
    }

    public boolean isMouseOrControlKeyOverInventory() {
        return (this.isMouseKey() || this.isCtrlKey()) && UIManager.isMouseOverInventory();
    }

    public boolean isKeyDownInWorld() {
        return !this.isMouseOrControlKeyOverInventory() && this.isKeyDown();
    }
}
