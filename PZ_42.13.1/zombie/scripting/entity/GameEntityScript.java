// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugMethod;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.Attribute;
import zombie.entity.components.attributes.AttributeType;
import zombie.network.GameClient;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.entity.components.ui.UiConfigScript;
import zombie.scripting.objects.BaseScriptObject;
import zombie.util.StringUtils;
import zombie.world.WorldDictionary;

@DebugClassFields
@UsedFromLua
public class GameEntityScript extends BaseScriptObject {
    private String name;
    private final ArrayList<ComponentScript> componentScripts = new ArrayList<>();
    private short registryId = -1;
    private boolean existsAsVanilla;
    private String modId;
    private String fileAbsPath;

    public GameEntityScript() {
        super(ScriptType.Entity);
    }

    protected GameEntityScript(ScriptType override) {
        super(override);
    }

    @DebugMethod
    public String getName() {
        return this.getParent() != null ? this.getParent().getScriptObjectName() : this.name;
    }

    public String getDisplayNameDebug() {
        UiConfigScript script = this.getComponentScriptFor(ComponentType.UiConfig);
        return script != null ? script.getDisplayNameDebug() : GameEntity.getDefaultEntityDisplayName();
    }

    public String getModuleName() {
        return this.getModule().getName();
    }

    @DebugMethod
    public String getFullName() {
        return this.getScriptObjectFullType();
    }

    @Override
    public boolean getObsolete() {
        return false;
    }

    @Override
    public void PreReload() {
        this.componentScripts.clear();
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        for (int i = 0; i < this.componentScripts.size(); i++) {
            this.componentScripts.get(i).OnScriptsLoaded(loadMode);
        }
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
        for (int i = 0; i < this.componentScripts.size(); i++) {
            this.componentScripts.get(i).OnLoadedAfterLua();
        }
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
        for (int i = 0; i < this.componentScripts.size(); i++) {
            this.componentScripts.get(i).OnPostWorldDictionaryInit();
        }
    }

    public ArrayList<ComponentScript> getComponentScripts() {
        return this.componentScripts;
    }

    public boolean hasComponents() {
        return !this.componentScripts.isEmpty();
    }

    public boolean containsComponent(ComponentType componentType) {
        for (int i = 0; i < this.componentScripts.size(); i++) {
            if (this.componentScripts.get(i).type == componentType) {
                return true;
            }
        }

        return false;
    }

    private ComponentScript getOrCreateComponentScript(ComponentType componentType) {
        ComponentScript componentScript = this.getComponentScript(componentType);
        if (componentScript == null) {
            componentScript = componentType.CreateComponentScript();
            if (componentScript != null) {
                componentScript.setParent(this);
                this.componentScripts.add(componentScript);
            }
        }

        if (componentScript != null) {
            componentScript.InitLoadPP(this.getScriptObjectName());
            componentScript.setModule(this.getModule());
        }

        return componentScript;
    }

    public <T extends ComponentScript> T getComponentScriptFor(ComponentType componentType) {
        return (T)(this.containsComponent(componentType) ? this.getComponentScript(componentType) : null);
    }

    private ComponentScript getComponentScript(ComponentType componentType) {
        for (int i = 0; i < this.componentScripts.size(); i++) {
            if (this.componentScripts.get(i).type == componentType) {
                return this.componentScripts.get(i);
            }
        }

        return null;
    }

    public void copyFrom(GameEntityScript other) {
        for (ComponentScript script : other.componentScripts) {
            ComponentScript componentScript = this.getOrCreateComponentScript(script.type);
            componentScript.copyFrom(script);
        }
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
        ScriptManager scriptMgr = ScriptManager.instance;
        this.name = name;
        this.modId = ScriptManager.getCurrentLoadFileMod();
        if (this.modId.equals("pz-vanilla")) {
            this.existsAsVanilla = true;
        }

        this.fileAbsPath = ScriptManager.getCurrentLoadFileAbsPath();
        WorldDictionary.onLoadEntity(this);
    }

    @Override
    public void Load(String name, String body) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(body);
        block = block.children.get(0);
        this.LoadCommonBlock(block);

        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty() && key.equalsIgnoreCase("entitytemplate")) {
                GameEntityTemplate template = ScriptManager.instance.getGameEntityTemplate(val);
                GameEntityScript script = template.getScript();
                this.copyFrom(script);
            }
        }

        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("component")) {
                this.LoadComponentBlock(child);
            } else {
                DebugLog.General.error("Unknown block '" + child.type + "' in entity  script: " + this.getName());
            }
        }

        this.Load(block);
    }

    protected void Load(ScriptParser.Block block) {
    }

    public boolean LoadAttribute(String k, String v) {
        AttributeType attributeType = Attribute.TypeFromName(k.trim());
        if (attributeType != null) {
            ComponentScript componentScript = this.getOrCreateComponentScript(ComponentType.Attributes);
            return componentScript.parseKeyValue(k, v);
        } else {
            return false;
        }
    }

    public void LoadComponentBlock(ScriptParser.Block block) throws Exception {
        ComponentType componentType = ComponentType.Undefined;
        if (block.type.equalsIgnoreCase("component") && !StringUtils.isNullOrWhitespace(block.id)) {
            try {
                componentType = ComponentType.valueOf(block.id);
            } catch (Exception var4) {
                var4.printStackTrace();
                componentType = ComponentType.Undefined;
            }

            if (componentType != ComponentType.Undefined) {
                ComponentScript componentScript = this.getOrCreateComponentScript(componentType);
                componentScript.load(block);
            } else {
                DebugLog.General.warn("Could not parse component block id = " + (block.id != null ? block.id : "null"));
            }
        }
    }

    public short getRegistry_id() {
        return this.registryId;
    }

    public void setRegistry_id(short id) {
        if (this.registryId != -1) {
            WorldDictionary.DebugPrintEntity(id);
            throw new RuntimeException(
                "Cannot override existing registry id ("
                    + this.registryId
                    + ") to new id ("
                    + id
                    + "), item: "
                    + (this.getFullName() != null ? this.getFullName() : "unknown")
            );
        } else {
            this.registryId = id;
        }
    }

    public String getModID() {
        return this.modId;
    }

    public boolean getExistsAsVanilla() {
        return this.existsAsVanilla;
    }

    public String getFileAbsPath() {
        return this.fileAbsPath;
    }

    public void setModID(String modid) {
        if (GameClient.client) {
            if (this.modId == null) {
                this.modId = modid;
            } else if (!modid.equals(this.modId) && Core.debug) {
                WorldDictionary.DebugPrintEntity(this);
                throw new RuntimeException("Cannot override modID. ModID=" + (modid != null ? modid : "null"));
            }
        }
    }
}
