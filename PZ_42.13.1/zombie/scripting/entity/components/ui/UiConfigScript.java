// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.ui;

import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.ui.XuiManager;
import zombie.scripting.ui.XuiSkin;

@UsedFromLua
public class UiConfigScript extends ComponentScript {
    private String xuiSkinName;
    private String entityStyle;
    private boolean uiEnabled = true;

    private UiConfigScript() {
        super(ComponentType.UiConfig);
    }

    public String getXuiSkinName() {
        return this.xuiSkinName;
    }

    public String getEntityStyle() {
        return this.entityStyle;
    }

    public boolean isUiEnabled() {
        return this.uiEnabled;
    }

    public String getDisplayNameDebug() {
        XuiSkin skin = XuiManager.GetSkin(this.xuiSkinName);
        return skin != null ? skin.getEntityDisplayName(this.entityStyle) : GameEntity.getDefaultEntityDisplayName();
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
        this.xuiSkinName = null;
        this.entityStyle = null;
        this.uiEnabled = true;
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
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("xuiSkin")) {
                    this.xuiSkinName = val;
                } else if (key.equalsIgnoreCase("entityStyle")) {
                    this.entityStyle = val;
                } else if (key.equalsIgnoreCase("uiEnabled")) {
                    this.uiEnabled = Boolean.parseBoolean(val);
                }
            }
        }

        for (ScriptParser.Block child : block.children) {
            if (!child.type.equalsIgnoreCase("someType")) {
                DebugLog.General.error("Unknown block '" + child.type + "' in entity script: " + this.getName());
            }
        }
    }
}
