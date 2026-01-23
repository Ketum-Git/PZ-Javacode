// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.ImVec2;
import zombie.core.Core;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;
import zombie.input.Mouse;

public class Viewport extends BaseDebugWindow {
    private float viewWidth;
    private float viewHeight;
    private float highlightX1;
    private float highlightY1;
    private float highlightX2;
    private float highlightY2;
    private int highlightCol;

    @Override
    public boolean doFrameStartTick() {
        return true;
    }

    @Override
    public String getTitle() {
        return "Viewport";
    }

    public float transformXToGame(float x) {
        x -= this.contentMin.x;
        x -= this.x;
        x /= this.viewWidth;
        return x * Core.width;
    }

    public float transformXToWindow(float x) {
        x /= Core.width;
        x *= this.viewWidth;
        x += this.x;
        return x + this.contentMin.x;
    }

    public float transformYToGame(float y) {
        y -= this.contentMin.y;
        y -= this.y;
        y /= this.viewHeight;
        return y * Core.height;
    }

    public float transformYToWindow(float y) {
        y /= Core.height;
        y *= this.viewHeight;
        y += this.y;
        return y + this.contentMin.y;
    }

    @Override
    protected void doWindowContents() {
        float x_ratio = (float)DebugContext.instance.debugViewportTexture.getWidth() / DebugContext.instance.debugViewportTexture.getTexture().getWidthHW();
        float y_ratio = (float)DebugContext.instance.debugViewportTexture.getHeight() / DebugContext.instance.debugViewportTexture.getTexture().getHeightHW();
        float aspect_ratio = (float)DebugContext.instance.debugViewportTexture.getHeight() / DebugContext.instance.debugViewportTexture.getWidth();
        float width = (ImGui.getWindowHeight() - this.contentMin.y) / aspect_ratio;
        float height = ImGui.getWindowHeight() - this.contentMin.y;
        width -= 10.0F;
        height -= 10.0F;
        if (width > ImGui.getWindowWidth()) {
            float var14 = ImGui.getWindowWidth() - this.contentMin.x;
            width = var14 - 10.0F;
            height = width * aspect_ratio;
        }

        float x = ImGui.getWindowPosX();
        float y = ImGui.getWindowPosY();
        ImGui.getCurrentDrawList()
            .addRectFilled(x, y, x + ImGui.getWindowWidth(), y + ImGui.getWindowWidth(), ImGui.colorConvertFloat4ToU32(0.0F, 0.0F, 0.0F, 1.0F));
        ImGui.image(DebugContext.instance.debugViewportTexture.getTexture().getID(), width, height, 0.0F, y_ratio, x_ratio, 0.0F);
        DebugContext.instance.focusedGameViewport = ImGui.isWindowFocused();
        this.viewWidth = width;
        this.viewHeight = height;
        this.contentMin = this.contentMin;
        float viewportMouseX = DebugContext.instance.getViewportMouseX();
        float viewportMouseY = DebugContext.instance.getViewportMouseY();
        float lastViewportMouseX = 0.0F;
        float lastViewportMouseY = 0.0F;
        if (DebugContext.instance.focusedGameViewport) {
            viewportMouseX = ImGui.getMousePosX();
            viewportMouseY = ImGui.getMousePosY();
            ImVec2 contentMax = ImGui.getWindowContentRegionMax();
            viewportMouseX -= this.contentMin.x;
            viewportMouseY -= this.contentMin.y;
            viewportMouseX -= ImGui.getWindowPosX();
            viewportMouseY -= ImGui.getWindowPosY();
            viewportMouseX /= width;
            viewportMouseY /= height;
            viewportMouseX *= Core.width;
            viewportMouseY *= Core.height;
            DebugContext.instance.setViewportX(viewportMouseX);
            DebugContext.instance.setViewportY(viewportMouseY);
        } else {
            DebugContext.instance.setViewportX(-1.0F);
            DebugContext.instance.setViewportY(-1.0F);
        }

        if (DebugContext.instance.focusedGameViewport) {
            if (Mouse.isButtonDown(1)) {
                ImGui.setMouseCursor(-1);
            } else {
                ImGui.setMouseCursor(0);
            }
        }

        ImGui.getCurrentDrawList().addRect(this.highlightX1, this.highlightY1, this.highlightX2, this.highlightY2, this.highlightCol, 0.0F, 0, 3.0F);
    }

    public void highlight(float x, float y, float w, float h, int col) {
        float x1 = this.transformXToWindow(x);
        float x2 = this.transformXToWindow(x + w);
        float y1 = this.transformYToWindow(y);
        float y2 = this.transformYToWindow(y + h);
        this.highlightX1 = x1;
        this.highlightX2 = x2;
        this.highlightY1 = y1;
        this.highlightY2 = y2;
        this.highlightCol = col;
    }
}
