// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.modding;

import java.util.ArrayList;
import java.util.Objects;
import zombie.GameWindow;
import zombie.MapGroups;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.debug.DebugOptions;
import zombie.gameStates.ChooseGameInfo;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class ActiveMods {
    private static final ArrayList<ActiveMods> s_activeMods = new ArrayList<>();
    private static final ActiveMods s_loaded = new ActiveMods("loaded");
    private final String id;
    private final ArrayList<String> mods = new ArrayList<>();
    private final ArrayList<String> mapOrder = new ArrayList<>();

    private static int count() {
        return s_activeMods.size();
    }

    public static ActiveMods getByIndex(int index) {
        return s_activeMods.get(index);
    }

    public static ActiveMods getById(String id) {
        int index = indexOf(id);
        return index == -1 ? create(id) : s_activeMods.get(index);
    }

    public static int indexOf(String id) {
        id = id.trim();
        requireValidId(id);

        for (int i = 0; i < s_activeMods.size(); i++) {
            ActiveMods activeMods = s_activeMods.get(i);
            if (activeMods.id.equalsIgnoreCase(id)) {
                return i;
            }
        }

        return -1;
    }

    private static ActiveMods create(String id) {
        requireValidId(id);
        if (indexOf(id) != -1) {
            throw new IllegalStateException("id \"" + id + "\" exists");
        } else {
            ActiveMods activeMods = new ActiveMods(id);
            s_activeMods.add(activeMods);
            return activeMods;
        }
    }

    private static void requireValidId(String id) {
        if (StringUtils.isNullOrWhitespace(id)) {
            throw new IllegalArgumentException("id is null or whitespace");
        }
    }

    public static void setLoadedMods(ActiveMods activeMods) {
        if (activeMods != null) {
            s_loaded.copyFrom(activeMods);
        }
    }

    public static boolean requiresResetLua(ActiveMods activeMods) {
        Objects.requireNonNull(activeMods);
        return !s_loaded.mods.equals(activeMods.mods);
    }

    public static void renderUI() {
        if (DebugOptions.instance.modRenderLoaded.getValue()) {
            if (!GameWindow.drawReloadingLua) {
                UIFont font = UIFont.DebugConsole;
                int fontHgt = TextManager.instance.getFontHeight(font);
                String label = "Active Mods:";
                int width = TextManager.instance.MeasureStringX(font, "Active Mods:");

                for (int i = 0; i < s_loaded.mods.size(); i++) {
                    String modID = s_loaded.mods.get(i);
                    int width1 = TextManager.instance.MeasureStringX(font, modID);
                    width = Math.max(width, width1);
                }

                int pad = 10;
                width += 20;
                int x = Core.width - 20 - width;
                int y = 20;
                int height = (1 + s_loaded.mods.size()) * fontHgt + 20;
                SpriteRenderer.instance.renderi(null, x, y, width, height, 0.0F, 0.5F, 0.75F, 1.0F, null);
                double var10002 = x + 10;
                y += 10;
                TextManager.instance.DrawString(font, var10002, y, "Active Mods:", 1.0, 1.0, 1.0, 1.0);

                for (int i = 0; i < s_loaded.mods.size(); i++) {
                    String modID = s_loaded.mods.get(i);
                    TextManager.instance.DrawString(font, x + 10, y += fontHgt, modID, 1.0, 1.0, 1.0, 1.0);
                }
            }
        }
    }

    public static void Reset() {
        s_loaded.clear();
    }

    public ActiveMods(String id) {
        requireValidId(id);
        this.id = id;
    }

    public void clear() {
        this.mods.clear();
        this.mapOrder.clear();
    }

    public ArrayList<String> getMods() {
        return this.mods;
    }

    public ArrayList<String> getMapOrder() {
        return this.mapOrder;
    }

    public void copyFrom(ActiveMods other) {
        this.mods.clear();
        this.mapOrder.clear();
        PZArrayUtil.addAll(this.mods, other.mods);
        PZArrayUtil.addAll(this.mapOrder, other.mapOrder);
    }

    public void setModActive(String modID, boolean active) {
        modID = modID.trim();
        if (!StringUtils.isNullOrWhitespace(modID)) {
            if (active) {
                if (!this.mods.contains(modID)) {
                    this.mods.add(modID);
                }
            } else {
                this.mods.remove(modID);
            }
        }
    }

    public boolean isModActive(String modID) {
        modID = modID.trim();
        return StringUtils.isNullOrWhitespace(modID) ? false : this.mods.contains(modID);
    }

    public void removeMod(String modID) {
        modID = modID.trim();
        this.mods.remove(modID);
    }

    public void removeMapOrder(String folder) {
        this.mapOrder.remove(folder);
    }

    public void checkMissingMods() {
        if (!this.mods.isEmpty()) {
            for (int i = this.mods.size() - 1; i >= 0; i--) {
                String modID = this.mods.get(i);
                if (ChooseGameInfo.getAvailableModDetails(modID) == null) {
                    this.mods.remove(i);
                }
            }
        }
    }

    public void checkMissingMaps() {
        if (!this.mapOrder.isEmpty()) {
            MapGroups mapGroups = new MapGroups();
            mapGroups.createGroups(this, false);
            if (mapGroups.checkMapConflicts()) {
                ArrayList<String> allMaps = mapGroups.getAllMapsInOrder();

                for (int i = this.mapOrder.size() - 1; i >= 0; i--) {
                    String mapName = this.mapOrder.get(i);
                    if (!allMaps.contains(mapName)) {
                        this.mapOrder.remove(i);
                    }
                }
            } else {
                this.mapOrder.clear();
            }
        }
    }
}
