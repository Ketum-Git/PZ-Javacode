// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import java.util.HashMap;
import zombie.statistics.Statistic;
import zombie.statistics.StatisticsManager;

public class StatisticsPanel extends PZDebugWindow {
    @Override
    public String getTitle() {
        return "Statistics";
    }

    @Override
    protected void doWindowContents() {
        this.displayStatistics();
    }

    private void displayStatistics() {
        if (ImGui.beginTable("Statistics", 4, 1984)) {
            ImGui.tableSetupColumn("Name");
            ImGui.tableSetupColumn("Type");
            ImGui.tableSetupColumn("Category");
            ImGui.tableSetupColumn("Value");
            ImGui.tableHeadersRow();
        }

        HashMap<String, Statistic> statisticHashMap = StatisticsManager.getInstance().getStatistics();
        statisticHashMap.forEach((key, value) -> {
            int columnIndex = 0;
            ImGui.tableNextRow();
            ImGui.tableSetColumnIndex(columnIndex++);
            ImGui.text(value.getName());
            ImGui.tableSetColumnIndex(columnIndex++);
            ImGui.text(value.getStatisticType().toString());
            ImGui.tableSetColumnIndex(columnIndex++);
            ImGui.text(value.getStatisticCategory().toString());
            ImGui.tableSetColumnIndex(columnIndex++);
            ImGui.text(Float.toString(value.getValue()));
        });
        ImGui.endTable();
    }
}
