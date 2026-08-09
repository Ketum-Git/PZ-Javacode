// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting.recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.objects.Item;

public class OverlayMapper {
    private final ArrayList<String> defaultStyles = new ArrayList<>();
    private final HashMap<Item, String> styleMap = new HashMap<>();
    private InputScript inputScript;

    public void clear() {
        this.defaultStyles.clear();
        this.styleMap.clear();
        this.inputScript = null;
    }

    public void setDefaultOverlayStyle(String style) {
        String[] split = style.split(";");

        for (String val : split) {
            if (val != null && !val.isEmpty()) {
                this.defaultStyles.add(val);
            }
        }
    }

    public void addOverlayStyleEntry(String style, String[] items) throws Exception {
        for (String itemName : items) {
            Item item = ScriptManager.instance.getItem(itemName);
            if (item == null) {
                throw new Exception("OverlayMapper item not found: " + itemName);
            }

            if (this.styleMap.containsKey(item)) {
                throw new Exception(
                    "OverlayMapper item " + itemName + " already has style mapping: " + this.styleMap.get(item) + ". Cannot assign style: " + style
                );
            }

            this.styleMap.put(item, style);
        }
    }

    public void registerInputScript(InputScript inputScript) {
        this.inputScript = inputScript;
    }

    public boolean isEmpty() {
        return this.defaultStyles.isEmpty() && this.styleMap.isEmpty();
    }

    public String getStyle(List<String> availableStyles, CraftRecipeData recipeData) {
        if (recipeData != null && this.inputScript != null && !this.styleMap.isEmpty()) {
            CraftRecipeData.InputScriptData inputData = recipeData.getDataForInputScript(this.inputScript);
            if (inputData != null) {
                for (int i = 0; i < inputData.getAppliedItemsCount(); i++) {
                    Item item = inputData.getAppliedItem(i).getScriptItem();
                    if (this.styleMap.containsKey(item)) {
                        String targetStyle = this.styleMap.get(item);

                        for (String style : availableStyles) {
                            String[] split = style.split("_");
                            if (split.length > 0 && split[0].equals(targetStyle)) {
                                return targetStyle;
                            }
                        }
                    }
                }
            }

            return this.getFirstAvailableDefaultStyle(availableStyles);
        } else {
            return this.getFirstAvailableDefaultStyle(availableStyles);
        }
    }

    public String getFirstAvailableDefaultStyle(List<String> availableStyles) {
        for (String style : this.defaultStyles) {
            for (String availableStyle : availableStyles) {
                String[] split = availableStyle.split("_");
                if (split.length > 0 && split[0].equals(style)) {
                    return style;
                }
            }
        }

        return null;
    }
}
