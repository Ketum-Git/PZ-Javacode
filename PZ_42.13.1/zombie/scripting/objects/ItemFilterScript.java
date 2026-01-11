// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Set;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.inventory.InventoryItem;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

@DebugClassFields
@UsedFromLua
public class ItemFilterScript extends BaseScriptObject {
    private final ItemFilterScript.FilterTypeInfo whitelist = new ItemFilterScript.FilterTypeInfo();
    private final ItemFilterScript.FilterTypeInfo blacklist = new ItemFilterScript.FilterTypeInfo();
    private boolean hasParsed;
    private String name;
    private final ArrayList<Item> tempScriptItems = new ArrayList<>();

    public ItemFilterScript() {
        super(ScriptType.ItemFilter);
    }

    public String getName() {
        return this.name;
    }

    @Override
    public void PreReload() {
        this.hasParsed = false;
        this.whitelist.reset();
        this.blacklist.reset();
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) {
    }

    @Override
    public void OnLoadedAfterLua() {
        this.parseFilter();
    }

    private void parseFilter() {
        if (!this.hasParsed) {
            this.resolveItemTypes(this.whitelist);
            this.resolveItemTypes(this.blacklist);
            this.whitelist.items.clear();
            this.whitelist.items.addAll(this.whitelist.loadedItems);
            if (!this.whitelist.items.isEmpty()) {
                ScriptManager.resolveGetItemTypes(this.whitelist.items, this.tempScriptItems);
            }

            this.blacklist.items.clear();
            this.blacklist.items.addAll(this.blacklist.loadedItems);
            if (!this.blacklist.items.isEmpty()) {
                ScriptManager.resolveGetItemTypes(this.blacklist.items, this.tempScriptItems);
            }

            this.hasParsed = true;
        } else {
            DebugLog.General.warn("Already parsed filter: " + this.name);
        }
    }

    private void resolveItemTypes(ItemFilterScript.FilterTypeInfo info) {
        if (!info.loadedTypes.isEmpty()) {
            for (String s : info.loadedTypes) {
                ItemType itemType = ItemType.get(ResourceLocation.of(s));
                if (!info.itemTypes.contains(itemType)) {
                    info.itemTypes.add(itemType);
                }
            }
        }
    }

    @Override
    public void OnPostWorldDictionaryInit() {
    }

    public boolean allowsItem(InventoryItem item) {
        return this.blacklist.containsItem(item) ? false : !this.whitelist.hasEntries() || this.whitelist.containsItem(item);
    }

    public boolean allowsItem(Item item) {
        return this.blacklist.containsItem(item) ? false : !this.whitelist.hasEntries() || this.whitelist.containsItem(item);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.name = name;
        this.LoadCommonBlock(block);
        this.readBlock(block, this.whitelist);
    }

    private void readBlock(ScriptParser.Block block, ItemFilterScript.FilterTypeInfo info) {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("items")) {
                    this.parseInputString(info.loadedItems, val);
                } else if (key.equalsIgnoreCase("types")) {
                    this.parseInputString(info.loadedTypes, val);
                } else if (key.equalsIgnoreCase("tags")) {
                    this.parseInputString(info.tags, val);
                }
            }
        }

        for (ScriptParser.Block child : block.children) {
            if ("items".equalsIgnoreCase(child.type)) {
                this.readFilterBlock(child, info.loadedItems);
            } else if ("types".equalsIgnoreCase(child.type)) {
                this.readFilterBlock(child, info.loadedTypes);
            } else if ("tags".equalsIgnoreCase(child.type)) {
                this.readFilterBlock(child, info.tags);
            } else if ("blacklist".equalsIgnoreCase(child.type)) {
                this.readBlock(child, this.blacklist);
            }
        }
    }

    private void readFilterBlock(ScriptParser.Block block, ArrayList<String> list) {
        for (ScriptParser.Value value : block.values) {
            if (value.string != null && !value.string.trim().isEmpty()) {
                String s = value.string.trim();
                if (!s.contains("=")) {
                    this.parseInputString(list, s);
                }
            }
        }
    }

    private void parseInputString(ArrayList<String> list, String input) {
        String[] split = input.split("/");

        for (String s : split) {
            s = s.trim();
            if (!list.contains(s)) {
                list.add(s);
            }
        }
    }

    @DebugClassFields
    private static class FilterTypeInfo {
        private final ArrayList<String> loadedItems = new ArrayList<>();
        private final ArrayList<String> loadedTypes = new ArrayList<>();
        private final ArrayList<String> items = new ArrayList<>();
        private final ArrayList<ItemType> itemTypes = new ArrayList<>();
        private final ArrayList<String> tags = new ArrayList<>();

        private void reset() {
            this.loadedItems.clear();
            this.loadedTypes.clear();
            this.items.clear();
            this.itemTypes.clear();
            this.tags.clear();
        }

        private boolean hasEntries() {
            return !this.items.isEmpty() || !this.itemTypes.isEmpty() || !this.tags.isEmpty();
        }

        private boolean containsItem(InventoryItem item) {
            if (item == null) {
                return false;
            } else {
                return item.getScriptItem() != null
                    ? this.containsItem(item.getFullType(), item.getScriptItem().getItemType(), item.getTags())
                    : this.containsItem(item.getFullType(), ItemType.NORMAL, item.getTags());
            }
        }

        private boolean containsItem(Item item) {
            return item == null ? false : this.containsItem(item.getFullName(), item.getItemType(), item.getTags());
        }

        private boolean containsItem(String itemFullType, ItemType itemType, Set<ItemTag> itemTags) {
            if (itemFullType == null || !this.hasEntries()) {
                return false;
            } else if (!this.items.isEmpty() && this.items.contains(itemFullType)) {
                return true;
            } else if (!this.itemTypes.isEmpty() && this.itemTypes.contains(itemType)) {
                return true;
            } else if (!this.tags.isEmpty() && itemTags != null && !itemTags.isEmpty()) {
                for (ItemTag t : itemTags) {
                    String path = Registries.ITEM_TAG.getLocation(t).getPath();
                    if (this.tags.contains(path)) {
                        return true;
                    }
                }

                return false;
            } else {
                return false;
            }
        }
    }
}
