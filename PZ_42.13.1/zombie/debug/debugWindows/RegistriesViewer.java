// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.debugWindows;

import imgui.ImGui;
import java.util.List;
import java.util.function.Function;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.Registry;
import zombie.scripting.objects.ResourceLocation;

public class RegistriesViewer extends PZDebugWindow {
    @Override
    public String getTitle() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void doWindowContents() {
        ImGui.beginChild("Begin");
        if (ImGui.beginTabBar("tabSelector")) {
            for (ResourceLocation key : Registries.REGISTRY.keys()) {
                this.renderGenericRegistryTab(key, Registries.REGISTRY.get(key));
            }

            ImGui.endTabBar();
        }

        ImGui.endChild();
    }

    private <T> void renderGenericRegistryTab(ResourceLocation resourceLocation, Registry<T> registry) {
        String tabName = resourceLocation.toString();
        if (ImGui.beginTabItem(tabName)) {
            List<RegistriesViewer.TableColumn<T>> columns = List.of(new RegistriesViewer.TableColumn<>("ID", item -> {
                ResourceLocation loc = registry.getLocation(item);
                return loc != null ? loc.getNamespace() + ":" + loc.getPath() : "";
            }), new RegistriesViewer.TableColumn<>("Namespace", item -> {
                ResourceLocation loc = registry.getLocation(item);
                return loc != null ? loc.getNamespace() : "";
            }), new RegistriesViewer.TableColumn<>("Path", item -> {
                ResourceLocation loc = registry.getLocation(item);
                return loc != null ? loc.getPath() : "";
            }));
            RegistriesViewer.ImGuiTableRenderer.renderTable(tabName, columns, registry);
            ImGui.endTabItem();
        }
    }

    public static class ImGuiTableRenderer {
        public static <T> void renderTable(String tableId, List<RegistriesViewer.TableColumn<T>> columns, Iterable<T> registry) {
            if (ImGui.beginTable(tableId, columns.size(), 1984)) {
                for (RegistriesViewer.TableColumn<T> column : columns) {
                    ImGui.tableSetupColumn(column.header);
                }

                ImGui.tableHeadersRow();

                for (T item : registry) {
                    ImGui.tableNextRow();

                    for (int col = 0; col < columns.size(); col++) {
                        ImGui.tableSetColumnIndex(col);
                        ImGui.text(columns.get(col).valueExtractor.apply(item));
                    }
                }

                ImGui.endTable();
            }
        }
    }

    public static class TableColumn<T> {
        private final String header;
        private final Function<T, String> valueExtractor;

        public TableColumn(String header, Function<T, String> valueExtractor) {
            this.header = header;
            this.valueExtractor = valueExtractor;
        }
    }
}
