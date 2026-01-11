// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.recipemanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.scripting.objects.Recipe;
import zombie.util.StringUtils;

@UsedFromLua
public class RecipeMonitor {
    private static boolean enabled;
    private static boolean suspended;
    private static int monitorID = -1;
    private static int tabs;
    private static String tabStr = "";
    private static final String tabSize = "  ";
    private static final Color defColor = Color.black;
    public static final Color colGray = new Color(0.5F, 0.5F, 0.5F);
    public static final Color colNeg = Colors.Maroon;
    public static final Color colPos = Colors.DarkGreen;
    public static final Color colHeader = Colors.SaddleBrown;
    private static final ArrayList<String> lines = new ArrayList<>();
    private static final ArrayList<Color> colors = new ArrayList<>();
    private static String recipeName = "none";
    private static Recipe lastRecipe;
    private static final ArrayList<String> recipeLines = new ArrayList<>();

    public static void Enable(boolean b) {
        enabled = b;
    }

    public static boolean IsEnabled() {
        return enabled;
    }

    public static int getMonitorID() {
        return monitorID;
    }

    public static void StartMonitor() {
        if (enabled) {
            monitorID++;
            suspended = false;
            lines.clear();
            colors.clear();
            recipeLines.clear();
            recipeName = "none";
            lastRecipe = null;
            ResetTabs();
            Log("MonitorID = " + monitorID);
        }
    }

    public static Color getColGray() {
        return colGray;
    }

    public static Color getColBlack() {
        return Color.black;
    }

    public static void setRecipe(Recipe recipe) {
        recipeName = recipe.getOriginalname();
        lastRecipe = recipe;
    }

    public static String getRecipeName() {
        return recipeName;
    }

    public static Recipe getRecipe() {
        return lastRecipe;
    }

    @Deprecated
    public static ArrayList<String> getRecipeLines() {
        return recipeLines;
    }

    public static boolean canLog() {
        return Core.debug && enabled && !suspended;
    }

    public static void suspend() {
        suspended = true;
    }

    public static void resume() {
        suspended = false;
    }

    public static void Log(String s) {
        if (canLog()) {
            Log(s, defColor);
        }
    }

    public static void Log(String s, Color c) {
        if (canLog()) {
            lines.add(tabStr + s);
            colors.add(c);
        }
    }

    public static void LogBlanc() {
        if (canLog()) {
            Log("");
        }
    }

    public static <T> void LogList(String tag, ArrayList<T> sourceTypes) {
        if (canLog()) {
            Log(tag + " {");
            IncTab();
            if (sourceTypes != null) {
                for (T sourceType : sourceTypes) {
                    Log(sourceType.toString());
                }
            }

            DecTab();
            Log("}");
        }
    }

    public static void LogInit(
        Recipe recipe,
        IsoGameCharacter character,
        ArrayList<ItemContainer> containers,
        InventoryItem selectedItem,
        ArrayList<InventoryItem> ignoreItems,
        boolean allItems
    ) {
        if (canLog()) {
            Log("[Recipe]", colHeader);
            Log("Starting recipe: " + recipe.getOriginalname());
            Log("All items = " + allItems);
            Log("character = " + character.getFullName());
            Log("selected item = " + selectedItem);
            LogContainers("containers", containers);
            LogBlanc();
        }
    }

    public static String getContainerString(ItemContainer container) {
        if (container == null) {
            return "ItemContainer:[null]";
        } else {
            if (container.getParent() != null) {
                if (container.getParent() instanceof IsoGameCharacter) {
                    return "ItemContainer:[type:" + container.type + ", parent:PlayerInventory]";
                }

                if (container.getParent().getSprite() != null) {
                    return "ItemContainer:[type:" + container.type + ", parent:PlayerInventory, sprite:" + container.getParent().getSprite().name + "]";
                }
            }

            return container.toString();
        }
    }

    private static void LogContainers(String tag, ArrayList<ItemContainer> containers) {
        LogContainers(tag, containers, false);
    }

    private static void LogContainers(String tag, ArrayList<ItemContainer> containers, boolean full) {
        if (canLog()) {
            Log(tag + " {");
            IncTab();
            if (containers != null) {
                for (ItemContainer container : containers) {
                    if (full) {
                        Log(getContainerString(container));
                        IncTab();

                        for (InventoryItem item : container.getItems()) {
                            Log("item > " + item);
                        }

                        DecTab();
                    } else {
                        Log(getContainerString(container));
                    }
                }
            } else {
                Log("null");
            }

            DecTab();
            Log("}");
        }
    }

    public static void LogSources(List<Recipe.Source> sources) {
        if (canLog()) {
            Log("[Sources]", colHeader);
            if (sources == null) {
                Log("Sources null.", colNeg);
            } else {
                for (int i = 0; i < sources.size(); i++) {
                    LogSource("[" + i + "] Source: ", sources.get(i));
                }
            }
        }
    }

    private static void LogSource(String tag, Recipe.Source source) {
        if (canLog()) {
            Log(tag + " {");
            IncTab();
            if (source != null) {
                Log((source.keep ? "(keep)" : "") + (source.destroy ? "(destroy)" : "") + "(count=" + source.count + ")(use=" + source.use + "):");
                IncTab();
                Log("items=" + source.getItems().toString());
                Log("orig=" + source.getOriginalItems().toString());
                DecTab();
            }

            DecTab();
            Log("}");
        }
    }

    public static void LogItem(String tag, InventoryItem item) {
        if (canLog()) {
            Log(tag + " = " + item);
        }
    }

    public static String getResultString(Recipe.Result result) {
        return "result = [" + result.getFullType() + ", count=" + result.getCount() + ", drain=" + result.getDrainableCount() + "]";
    }

    private static void setTabStr() {
        if (tabs > 0) {
            tabStr = "  ".repeat(tabs);
        } else {
            tabStr = "";
        }
    }

    public static void ResetTabs() {
        tabs = 0;
        setTabStr();
    }

    public static void SetTab(int i) {
        if (canLog()) {
            tabs = i;
            setTabStr();
        }
    }

    public static void IncTab() {
        if (canLog()) {
            tabs++;
            setTabStr();
        }
    }

    public static void DecTab() {
        if (canLog()) {
            tabs--;
            if (tabs < 0) {
                tabs = 0;
            }

            setTabStr();
        }
    }

    public static ArrayList<String> GetLines() {
        return lines;
    }

    public static ArrayList<Color> GetColors() {
        return colors;
    }

    public static Color GetColorForLine(int i) {
        return i >= 0 && i < colors.size() ? colors.get(i) : defColor;
    }

    public static String GetSaveDir() {
        return ZomboidFileSystem.instance.getCacheDir() + File.separator + "RecipeLogs" + File.separator;
    }

    public static void SaveToFile() {
        if (!lines.isEmpty()) {
            try {
                String STAMP = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String SAVE_FILE = "log_" + STAMP;
                String name = recipeName;
                if (name != null) {
                    name = name.toLowerCase();
                    name = name.replaceAll("\\s", "_");
                    name = name.replace("\\.", "");
                }

                if (StringUtils.isNullOrWhitespace(name)) {
                    name = "unkown";
                }

                SAVE_FILE = SAVE_FILE + "_" + name;
                String path = GetSaveDir();
                File pathFile = new File(path);
                if (!pathFile.exists() && !pathFile.mkdirs()) {
                    DebugLog.log("Failed to create path = " + path);
                    return;
                }

                String fileName = path + SAVE_FILE + ".txt";
                DebugLog.log("Attempting to save recipe log to: " + fileName);
                File f = new File(fileName);

                try (BufferedWriter w = new BufferedWriter(new FileWriter(f, false))) {
                    w_write(w, "Recipe name = " + recipeName);
                    w_write(w, "# Recipe at time of recording:");
                    w_blanc(w);

                    for (String s : recipeLines) {
                        w_write(w, s);
                    }

                    w_blanc(w);
                    w_write(w, "# Recipe monitor log:");
                    w_blanc(w);

                    for (String s : lines) {
                        w_write(w, s);
                    }
                } catch (Exception var12) {
                    var12.printStackTrace();
                }
            } catch (Exception var13) {
                var13.printStackTrace();
            }
        }
    }

    private static void w_blanc(BufferedWriter w) throws IOException {
        w_write(w, null);
    }

    private static void w_write(BufferedWriter w, String line) throws IOException {
        if (line != null) {
            w.write(line);
        }

        w.newLine();
    }
}
