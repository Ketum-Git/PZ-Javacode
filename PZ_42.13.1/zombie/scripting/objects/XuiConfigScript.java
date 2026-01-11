// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.ui.XuiLuaStyle;
import zombie.scripting.ui.XuiVarType;
import zombie.util.StringUtils;

@UsedFromLua
public class XuiConfigScript extends BaseScriptObject {
    private final Map<XuiVarType, ArrayList<String>> varConfigs = new HashMap<>();

    public XuiConfigScript() {
        super(ScriptType.XuiConfig);

        for (XuiVarType varType : XuiLuaStyle.ALLOWED_VAR_TYPES) {
            this.varConfigs.put(varType, new ArrayList<>());
        }
    }

    public Map<XuiVarType, ArrayList<String>> getVarConfigs() {
        return this.varConfigs;
    }

    @Override
    public void reset() {
        for (Entry<XuiVarType, ArrayList<String>> entry : this.varConfigs.entrySet()) {
            entry.getValue().clear();
        }
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
    }

    @Override
    public void Load(String name, String body) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(body);
        block = block.children.get(0);
        this.LoadCommonBlock(block);

        for (ScriptParser.Block child : block.children) {
            XuiVarType varType = XuiVarType.valueOf(child.type);
            if (!XuiLuaStyle.ALLOWED_VAR_TYPES.contains(varType)) {
                throw new Exception("VarType not allowed: " + varType);
            }

            this.LoadVarTypeBlock(varType, child);
        }
    }

    private void LoadVarTypeBlock(XuiVarType varType, ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            if (!StringUtils.isNullOrWhitespace(value.string)) {
                String key = value.string.trim();
                if (this.otherTypesContainsKey(key, varType)) {
                    throw new Exception("Key '" + key + "' duplicate from another value block. this var type = " + varType);
                }

                ArrayList<String> list = this.varConfigs.get(varType);
                if (!list.contains(key)) {
                    list.add(key);
                }
            }
        }
    }

    private boolean otherTypesContainsKey(String key, XuiVarType ignoreType) {
        for (Entry<XuiVarType, ArrayList<String>> entry : this.varConfigs.entrySet()) {
            if (entry.getKey() != ignoreType && entry.getValue().contains(key)) {
                DebugLog.General.warn("Duplicate key '" + key + "' in var type: " + entry.getKey());
                return true;
            }
        }

        return false;
    }

    @Override
    public void PreReload() {
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
    }
}
