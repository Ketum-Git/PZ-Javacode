// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import java.util.HashMap;
import zombie.AchievementManager;
import zombie.statistics.Achievement;

public class AchievementPanel extends PZDebugWindow {
    private static final int Red = -16776961;
    private static final int Green = -16711936;

    @Override
    public String getTitle() {
        return "Achievements";
    }

    @Override
    protected void doWindowContents() {
        this.displayStatistics();
    }

    private void displayStatistics() {
        if (ImGui.beginTable("Achievements", 4, 1984)) {
            ImGui.tableSetupColumn("Name");
            ImGui.tableSetupColumn("Description");
            ImGui.tableSetupColumn("Threshold");
            ImGui.tableSetupColumn("Achieved");
            ImGui.tableHeadersRow();
        }

        HashMap<String, Achievement> achievementHashMapHashMap = AchievementManager.getInstance().getAchievements();
        achievementHashMapHashMap.forEach((key, value) -> {
            int columnIndex = 0;
            ImGui.tableNextRow();
            ImGui.tableSetColumnIndex(columnIndex++);
            ImGui.text(value.getName());
            ImGui.tableSetColumnIndex(columnIndex++);
            ImGui.text(String.format(value.getDescription(), value.getThreshold()));
            ImGui.tableSetColumnIndex(columnIndex++);
            ImGui.text(Float.toString(value.getThreshold()));
            ImGui.tableSetColumnIndex(columnIndex++);
            ImGui.textColored(value.isUnlocked() ? -16711936 : -16776961, Boolean.toString(value.isUnlocked()));
        });
        ImGui.endTable();
    }
}
