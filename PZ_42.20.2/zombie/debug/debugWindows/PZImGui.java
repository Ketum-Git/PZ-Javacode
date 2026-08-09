// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.SoundManager;

public class PZImGui extends Wrappers {
    public static float sliderFloat(String label, float value, float min, float max) {
        float sliderValue = Wrappers.sliderFloat(label, value, min, max);
        if (ImGui.isItemClicked()) {
            SoundManager.instance.playUISound("UISelectListItem");
        }

        return sliderValue;
    }

    public static boolean button(String s) {
        boolean pressed = ImGui.button(s);
        if (ImGui.isItemClicked()) {
            SoundManager.instance.playUISound("UIToggleTickBox");
        }

        return pressed;
    }

    public static boolean combo(String label, ImInt currentItem, String[] items) {
        boolean pressed = ImGui.combo(label, currentItem, items);
        if (ImGui.isItemClicked()) {
            SoundManager.instance.playUISound("UIToggleComboBox");
        }

        return pressed;
    }

    public static boolean collapsingHeader(String s) {
        boolean visible = ImGui.collapsingHeader(s);
        if (ImGui.isItemClicked()) {
            SoundManager.instance.playUISound("UIToggleTickBox");
        }

        return visible;
    }

    public static boolean begin(String title, ImBoolean pOpen, int imGuiWindowFlags) {
        boolean visible = ImGui.begin(title, pOpen, imGuiWindowFlags);
        if (ImGui.isItemClicked() || ImGui.isItemActivated()) {
            SoundManager.instance.playUISound("UIActivateTab");
        }

        return visible;
    }

    public static boolean checkboxWithDefaultValueHighlight(String label, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultValue, int color) {
        valueBoolean.set(getter.get());
        if (valueBoolean.get() != defaultValue) {
            ImGui.pushStyleColor(0, color);
            ImGui.checkbox(label, valueBoolean);
            ImGui.popStyleColor();
        } else {
            ImGui.checkbox(label, valueBoolean);
        }

        if (valueBoolean.get() != getter.get()) {
            setter.accept(valueBoolean.get());
        }

        if (ImGui.isItemClicked()) {
            SoundManager.instance.playUISound("UIToggleTickBox");
        }

        return valueBoolean.get();
    }

    public static boolean checkbox(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        valueBoolean.set(getter.get());
        ImGui.checkbox(label, valueBoolean);
        if (valueBoolean.get() != getter.get()) {
            setter.accept(valueBoolean.get());
        }

        if (ImGui.isItemClicked()) {
            SoundManager.instance.playUISound("UIToggleTickBox");
        }

        return valueBoolean.get();
    }
}
