// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.type.ImBoolean;
import zombie.debug.debugWindows.PZImGui;

public class BaseDebugWindow {
    protected float x;
    protected float y;
    protected float width;
    protected float height;
    protected ImVec2 contentMin;
    protected ImBoolean open = new ImBoolean(true);
    protected boolean wasWindowOpened = true;
    protected boolean wasWindowDocked;

    public String getTitle() {
        return "";
    }

    public void doWindow() {
        ImGui.setNextWindowSize(500.0F, 500.0F, 4);
        if (PZImGui.begin(this.getTitle(), this.open, this.getWindowFlags() | (this.hasMenu() ? 1024 : 0))) {
            this.x = ImGui.getWindowPosX();
            this.y = ImGui.getWindowPosY();
            this.width = ImGui.getWindowWidth();
            this.height = ImGui.getWindowHeight();
            this.contentMin = ImGui.getWindowContentRegionMin();
            this.doMenu();
            this.doWindowContents();
            this.doKeyInputInternal();
        }

        this.updateWindowState();
        this.updateWindowDockedState();
        ImGui.end();
    }

    protected void updateWindowDockedState() {
        boolean isDocked = ImGui.isWindowDocked();
        if (isDocked && !this.wasWindowDocked) {
            this.onWindowDocked();
            this.wasWindowDocked = true;
        } else if (!isDocked && this.wasWindowDocked) {
            this.onWindowUndocked();
            this.wasWindowDocked = false;
        }
    }

    protected void updateWindowState() {
        boolean isOpen = this.open.get();
        if (isOpen && !this.wasWindowOpened) {
            this.onOpenWindow();
            this.wasWindowOpened = true;
        } else if (!isOpen && this.wasWindowOpened) {
            this.onCloseWindow();
            this.wasWindowOpened = false;
        }
    }

    protected void onWindowDocked() {
    }

    protected void onWindowUndocked() {
    }

    protected void onCloseWindow() {
    }

    protected void onOpenWindow() {
    }

    protected void doMenu() {
    }

    protected boolean isWindowFocused() {
        return ImGui.isWindowFocused();
    }

    private void doKeyInputInternal() {
        if (this.isWindowFocused()) {
            ImGuiIO io = ImGui.getIO();
            this.doKeyInput(io, io.getKeyShift(), io.getKeyCtrl(), io.getKeyAlt());
        }
    }

    protected void doKeyInput(ImGuiIO io, boolean keyShift, boolean keyCtrl, boolean keyAlt) {
    }

    protected boolean hasMenu() {
        return false;
    }

    public int getWindowFlags() {
        return 0;
    }

    protected void doWindowContents() {
    }

    public boolean doFrameStartTick() {
        return false;
    }

    public void startFrameTick() {
        if (this.doFrameStartTick()) {
            this.doWindow();
        }
    }

    public void endFrameTick() {
        if (!this.doFrameStartTick()) {
            this.doWindow();
        }
    }
}
