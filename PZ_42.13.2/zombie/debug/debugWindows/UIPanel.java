// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import java.util.ArrayList;
import zombie.debug.BaseDebugWindow;
import zombie.debug.DebugContext;
import zombie.ui.UIElement;
import zombie.ui.UIElementInterface;
import zombie.ui.UIManager;

public class UIPanel extends BaseDebugWindow {
    private float selectedUiX;
    private float selectedUiY;
    private float selectedUiWidth;
    private float selectedUiHeight;
    private String selectedNode = "";

    @Override
    public String getTitle() {
        return "UI";
    }

    private void doUITree(UIElement element) {
        String name = element.getUIName();
        int flags = 192;
        if (element.controls.isEmpty()) {
            flags |= 256;
        }

        if (String.valueOf(element.hashCode()).equalsIgnoreCase(this.selectedNode)) {
            flags |= 1;
            if (element.isVisible()) {
                this.selectedUiX = element.getAbsoluteX().floatValue();
                this.selectedUiY = element.getAbsoluteY().floatValue();
                this.selectedUiWidth = element.getWidth().floatValue();
                this.selectedUiHeight = element.getHeight().floatValue();
            } else {
                this.selectedUiX = 0.0F;
                this.selectedUiY = 0.0F;
                this.selectedUiWidth = 0.0F;
                this.selectedUiHeight = 0.0F;
            }

            DebugContext.instance
                .viewport
                .highlight(
                    this.selectedUiX, this.selectedUiY, this.selectedUiWidth, this.selectedUiHeight, ImGui.colorConvertFloat4ToU32(1.0F, 0.0F, 0.0F, 1.0F)
                );
        }

        if (!element.isVisible()) {
            ImGui.pushStyleColor(0, ImGui.colorConvertFloat4ToU32(0.4F, 0.4F, 0.4F, 1.0F));
        }

        if (ImGui.treeNodeEx(String.valueOf(element.hashCode()), flags, name)) {
            if (ImGui.isItemClicked()) {
                this.selectedNode = String.valueOf(element.hashCode());
            }

            ArrayList<UIElement> controls = element.getControls();

            for (int i = 0; i < controls.size(); i++) {
                UIElement control = controls.get(i);
                this.doUITree(control);
            }

            ImGui.treePop();
        } else if (ImGui.isItemClicked()) {
            this.selectedNode = String.valueOf(element.hashCode());
        }

        if (!element.isVisible()) {
            ImGui.popStyleColor();
        }
    }

    @Override
    protected void doWindowContents() {
        ArrayList<UIElementInterface> elements = UIManager.getUI();

        for (int i = 0; i < elements.size(); i++) {
            UIElementInterface element = elements.get(i);
            if (element instanceof UIElement uiElement) {
                this.doUITree(uiElement);
            }
        }
    }
}
