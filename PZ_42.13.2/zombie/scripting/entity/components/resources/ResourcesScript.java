// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.components.resources.ResourceBlueprint;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.util.StringUtils;

public class ResourcesScript extends ComponentScript {
    private final HashMap<String, ArrayList<String>> groupLines = new HashMap<>();
    private final ArrayList<String> groupNames = new ArrayList<>();
    private final ArrayList<ResourceBlueprint> blueprints = new ArrayList<>();
    private final HashMap<String, ArrayList<ResourceBlueprint>> groups = new HashMap<>();

    private ResourcesScript() {
        super(ComponentType.Resources);
    }

    public ArrayList<String> getGroupNames() {
        return this.groupNames;
    }

    public ArrayList<ResourceBlueprint> getBlueprintGroup(String group) {
        return this.groups.get(group);
    }

    @Override
    protected void copyFrom(ComponentScript other) {
    }

    @Override
    public boolean isoMasterOnly() {
        return true;
    }

    @Override
    public void PreReload() {
        this.groupLines.clear();
        this.groupNames.clear();
        this.blueprints.clear();
        this.groups.clear();
    }

    @Override
    public void reset() {
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);

        for (Entry<String, ArrayList<String>> entry : this.groupLines.entrySet()) {
            String groupName = entry.getKey();
            ArrayList<ResourceBlueprint> groupList = this.groups.computeIfAbsent(groupName, k -> new ArrayList<>());
            this.groupNames.add(groupName);

            for (String line : entry.getValue()) {
                try {
                    ResourceBlueprint blueprint = ResourceBlueprint.DeserializeFromScript(line);
                    groupList.add(blueprint);
                    this.blueprints.add(blueprint);
                } catch (Exception var9) {
                    DebugLog.log("Error in resource blueprint line: " + line + ", entity: " + this.getName());
                    var9.printStackTrace();
                    throw new Exception(var9);
                }
            }
        }
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        this.loadResourceBlock("resources", block);

        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("group")) {
                String group = child.id;
                if (StringUtils.isNullOrWhitespace(group)) {
                    throw new Exception("Group name cannot be null or whitespace.");
                }

                this.loadResourceBlock(group, child);
            } else if (child.type.equalsIgnoreCase("internal")) {
                DebugLog.General.warn("internal block is deprecated");
            } else if (child.type.equalsIgnoreCase("external")) {
                DebugLog.General.warn("external block is deprecated");
            } else {
                DebugLog.General.error("Unknown block '" + child.type + "' in entity script: " + this.getName());
            }
        }
    }

    private void loadResourceBlock(String groupName, ScriptParser.Block block) throws Exception {
        if (StringUtils.isNullOrWhitespace(groupName)) {
            throw new Exception("GroupName cannot be null or whitespace");
        } else {
            ArrayList<String> list = this.groupLines.computeIfAbsent(groupName, k -> new ArrayList<>());

            for (ScriptParser.Value value : block.values) {
                String line = value.string != null ? value.string.trim() : null;
                if (line != null && !StringUtils.isNullOrWhitespace(line) && line.contains("@")) {
                    String key = value.getKey().trim();
                    String val = value.getValue().trim();
                    if (!key.isEmpty() && !val.isEmpty()) {
                        line = key + "@" + val;
                    } else {
                        line = groupName + "_" + list.size() + "@" + line;
                    }

                    list.add(line);
                }
            }
        }
    }
}
