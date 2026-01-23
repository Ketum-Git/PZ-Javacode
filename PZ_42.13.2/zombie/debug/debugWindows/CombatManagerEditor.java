// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import zombie.CombatManager;
import zombie.combat.CombatConfig;
import zombie.combat.CombatConfigCategory;
import zombie.combat.CombatConfigKey;

public class CombatManagerEditor extends PZDebugWindow {
    final CombatManager combatManager = CombatManager.getInstance();

    @Override
    public String getTitle() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void doWindowContents() {
        CombatConfig combatConfig = this.combatManager.getCombatConfig();
        if (ImGui.beginTabBar("CombatConfigTabs")) {
            if (ImGui.beginTabItem("General")) {
                ImGui.beginChild("GeneralTabChild");
                this.drawTopLevelFields(combatConfig);
                ImGui.endChild();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Firearm")) {
                ImGui.beginChild("FirearmTabChild");
                this.drawCategory(CombatConfigCategory.FIREARM, combatConfig);
                ImGui.endChild();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Melee")) {
                ImGui.beginChild("MeleeTabChild");
                this.drawCategory(CombatConfigCategory.MELEE, combatConfig);
                ImGui.endChild();
                ImGui.endTabItem();
            }

            if (ImGui.beginTabItem("Ballistics")) {
                ImGui.beginChild("BallisticsTabChild");
                this.drawCategory(CombatConfigCategory.BALLISTICS, combatConfig);
                ImGui.endChild();
                ImGui.endTabItem();
            }

            ImGui.endTabBar();
        }
    }

    private void drawTopLevelFields(CombatConfig combatConfig) {
        for (CombatConfigKey key : CombatConfigKey.values()) {
            if (key.getCategory() == CombatConfigCategory.GENERAL) {
                this.drawEnumField(key, combatConfig);
            }
        }
    }

    private void drawEnumField(CombatConfigKey key, CombatConfig combatConfig) {
        float value = combatConfig.get(key);
        float[] buffer = new float[]{value};
        if (ImGui.sliderFloat(key.name(), buffer, key.getMinimum(), key.getMaximum())) {
            combatConfig.set(key, buffer[0]);
        }
    }

    private void drawCategory(CombatConfigCategory category, CombatConfig combatConfig) {
        for (CombatConfigKey key : CombatConfigKey.values()) {
            if (key.getCategory() == category) {
                this.drawEnumField(key, combatConfig);
            }
        }
    }
}
