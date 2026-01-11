// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;

@UsedFromLua
public class XuiSkinScript extends BaseScriptObject {
    private static final String protectedDefaultName = "default";
    private final ArrayList<String> imports = new ArrayList<>();
    private final XuiSkinScript.EntityUiScript defaultEntityUiScript = new XuiSkinScript.EntityUiScript();
    private final Map<String, XuiSkinScript.EntityUiScript> entityUiScriptMap = new HashMap<>();
    private final Map<String, XuiSkinScript.StyleInfoScript> styleInfoMap = new HashMap<>();
    private final XuiColorsScript colorsScript = new XuiColorsScript();

    public XuiSkinScript() {
        super(ScriptType.XuiSkin);
    }

    public ArrayList<String> getImports() {
        return this.imports;
    }

    public XuiSkinScript.EntityUiScript getDefaultEntityUiScript() {
        return this.defaultEntityUiScript;
    }

    public final Map<String, XuiSkinScript.EntityUiScript> getEntityUiScriptMap() {
        return this.entityUiScriptMap;
    }

    public Map<String, XuiSkinScript.StyleInfoScript> getStyleInfoMap() {
        return this.styleInfoMap;
    }

    public XuiColorsScript getColorsScript() {
        return this.colorsScript;
    }

    @Override
    public void reset() {
        this.imports.clear();
        this.defaultEntityUiScript.reset();
        this.entityUiScriptMap.clear();
        this.styleInfoMap.clear();
        this.colorsScript.getColorMap().clear();
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

        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                DebugLog.General.warn("Unknown line in script: " + value.string);
            }
        }

        for (ScriptParser.Block child : block.children) {
            if ("imports".equalsIgnoreCase(child.type)) {
                this.LoadImports(child);
            } else if ("entity".equalsIgnoreCase(child.type)) {
                this.LoadEntityBlock(child);
            } else if ("colors".equalsIgnoreCase(child.type)) {
                this.colorsScript.LoadColorsBlock(child);
            } else {
                this.LoadStyleBlock(child);
            }
        }
    }

    private void LoadStyleBlock(ScriptParser.Block block) throws Exception {
        if (!this.styleInfoMap.containsKey(block.type)) {
            this.styleInfoMap.put(block.type, new XuiSkinScript.StyleInfoScript());
        }

        XuiSkinScript.StyleInfoScript styleInfo = this.styleInfoMap.get(block.type);
        Map<String, String> map;
        if (StringUtils.isNullOrWhitespace(block.id)) {
            map = styleInfo.defaultStyleBlock;
        } else {
            if (block.id.equalsIgnoreCase("default")) {
                throw new Exception("Default is protected and cannot be used as style name.");
            }

            if (block.id.contains(".")) {
                throw new Exception("Style name may not contain '.' (dot).");
            }

            if (!styleInfo.styleBlocks.containsKey(block.id)) {
                styleInfo.styleBlocks.put(block.id, new HashMap<>());
            }

            map = styleInfo.styleBlocks.get(block.id);
        }

        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty()) {
                val = StringUtils.trimSurroundingQuotes(val);
                map.put(key, !val.isEmpty() && !val.equalsIgnoreCase("nil") && !val.equalsIgnoreCase("null") ? val : null);
            }
        }
    }

    private void LoadImports(ScriptParser.Block block) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String s = value.string != null ? value.string.trim() : "";
            if (!StringUtils.isNullOrWhitespace(s)) {
                if (s.equalsIgnoreCase(this.getScriptObjectName())) {
                    DebugLog.General.warn("Cannot import self: " + this.getScriptObjectName());
                    if (Core.debug) {
                        throw new Exception("Cannot import self: " + this.getScriptObjectName());
                    }
                }

                this.imports.add(s);
            }
        }
    }

    private void LoadEntityBlock(ScriptParser.Block block) throws Exception {
        XuiSkinScript.EntityUiScript entityUiScript = null;
        if (StringUtils.isNullOrWhitespace(block.id)) {
            entityUiScript = this.defaultEntityUiScript;
        } else {
            if (block.id.equalsIgnoreCase("default")) {
                throw new Exception("Default is protected and cannot be used as style name.");
            }

            entityUiScript = this.entityUiScriptMap.get(block.id);
            if (entityUiScript == null) {
                entityUiScript = new XuiSkinScript.EntityUiScript();
                this.entityUiScriptMap.put(block.id, entityUiScript);
            }
        }

        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("luaWindowClass")) {
                    entityUiScript.luaWindowClass = val;
                } else if (key.equalsIgnoreCase("xuiStyle")) {
                    entityUiScript.xuiStyle = val;
                } else if (key.equalsIgnoreCase("luaCanOpenWindow")) {
                    entityUiScript.luaCanOpenWindow = val;
                } else if (key.equalsIgnoreCase("luaOpenWindow")) {
                    entityUiScript.luaOpenWindow = val;
                } else if (key.equalsIgnoreCase("displayName")) {
                    String newVal = val.replace(" ", "");
                    String translatedText = Translator.getRecipeName(newVal);
                    if (!translatedText.equalsIgnoreCase(newVal)) {
                        val = translatedText;
                    }

                    entityUiScript.displayName = val;
                } else if (key.equalsIgnoreCase("description")) {
                    entityUiScript.description = val;
                } else if (key.equalsIgnoreCase("buildDescription")) {
                    entityUiScript.buildDescription = val;
                } else if (key.equalsIgnoreCase("icon")) {
                    entityUiScript.iconPath = val;
                } else if (key.equalsIgnoreCase("clearComponents")) {
                    entityUiScript.clearComponents = val.equalsIgnoreCase("true");
                } else {
                    DebugLog.General.warn("Unknown line in script: " + value.string);
                }
            }
        }

        for (ScriptParser.Block child : block.children) {
            if ("components".equalsIgnoreCase(child.type)) {
                this.LoadComponents(child, entityUiScript);
            }
        }
    }

    private void LoadComponents(ScriptParser.Block block, XuiSkinScript.EntityUiScript entityUiScript) throws Exception {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                ComponentType componentType = ComponentType.valueOf(key);
                String luaClass = val;
                String style = null;
                if (val.contains(":")) {
                    String[] s = val.split(":");
                    luaClass = s[0];
                    style = s[1];
                }

                XuiSkinScript.ComponentUiScript uiInfo = entityUiScript.componentUiScriptMap.get(componentType);
                if (uiInfo == null) {
                    uiInfo = new XuiSkinScript.ComponentUiScript();
                    entityUiScript.componentUiScriptMap.put(componentType, uiInfo);
                }

                uiInfo.luaPanelClass = luaClass;
                if (style != null) {
                    uiInfo.xuiStyle = style;
                }
            }
        }

        for (ScriptParser.Block child : block.children) {
            this.LoadComponentBlock(child, entityUiScript);
        }
    }

    private void LoadComponentBlock(ScriptParser.Block block, XuiSkinScript.EntityUiScript entityUiScript) throws Exception {
        ComponentType componentType = ComponentType.valueOf(block.type);
        XuiSkinScript.ComponentUiScript uiInfo = entityUiScript.componentUiScriptMap.get(componentType);
        if (uiInfo == null) {
            uiInfo = new XuiSkinScript.ComponentUiScript();
            entityUiScript.componentUiScriptMap.put(componentType, uiInfo);
        }

        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("luaPanelClass")) {
                    uiInfo.luaPanelClass = val;
                } else if (key.equalsIgnoreCase("xuiStyle")) {
                    uiInfo.xuiStyle = val;
                } else if (key.equalsIgnoreCase("displayName")) {
                    uiInfo.displayName = val;
                } else if (key.equalsIgnoreCase("icon")) {
                    uiInfo.iconPath = val;
                } else if (key.equalsIgnoreCase("listOrderZ")) {
                    uiInfo.listOrderZ = Integer.parseInt(val);
                    uiInfo.listOrderZset = true;
                } else if (key.equalsIgnoreCase("enabled")) {
                    uiInfo.enabled = val.equalsIgnoreCase("true");
                    uiInfo.enabledSet = true;
                } else {
                    DebugLog.General.warn("Unknown line in script: " + value.string);
                }
            }
        }
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

    public static class ComponentUiScript {
        private String luaPanelClass;
        private String xuiStyle;
        private String displayName;
        private String iconPath;
        private boolean listOrderZset;
        private int listOrderZ;
        private boolean enabledSet;
        private boolean enabled = true;

        public String getLuaPanelClass() {
            return this.luaPanelClass;
        }

        public String getXuiStyle() {
            return this.xuiStyle;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getIconPath() {
            return this.iconPath;
        }

        public boolean isListOrderZ() {
            return this.listOrderZset;
        }

        public int getListOrderZ() {
            return this.listOrderZ;
        }

        public boolean isEnabledSet() {
            return this.enabledSet;
        }

        public boolean isEnabled() {
            return this.enabled;
        }
    }

    public static class EntityUiScript {
        private String luaWindowClass;
        private String xuiStyle;
        private String luaCanOpenWindow;
        private String luaOpenWindow;
        private String displayName;
        private String description;
        private String buildDescription;
        private String iconPath;
        private boolean clearComponents;
        private final Map<ComponentType, XuiSkinScript.ComponentUiScript> componentUiScriptMap = new HashMap<>();

        public String getLuaWindowClass() {
            return this.luaWindowClass;
        }

        public String getXuiStyle() {
            return this.xuiStyle;
        }

        public String getLuaCanOpenWindow() {
            return this.luaCanOpenWindow;
        }

        public String getLuaOpenWindow() {
            return this.luaOpenWindow;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public String getDescription() {
            return this.description;
        }

        public String getBuildDescription() {
            return this.buildDescription;
        }

        public String getIconPath() {
            return this.iconPath;
        }

        public boolean isClearComponents() {
            return this.clearComponents;
        }

        public Map<ComponentType, XuiSkinScript.ComponentUiScript> getComponentUiScriptMap() {
            return this.componentUiScriptMap;
        }

        protected void reset() {
            this.luaWindowClass = null;
            this.xuiStyle = null;
            this.luaCanOpenWindow = null;
            this.luaOpenWindow = null;
            this.displayName = null;
            this.description = null;
            this.buildDescription = null;
            this.iconPath = null;
            this.clearComponents = false;
            this.componentUiScriptMap.clear();
        }
    }

    public static class StyleInfoScript {
        private final HashMap<String, String> defaultStyleBlock = new HashMap<>();
        private final Map<String, HashMap<String, String>> styleBlocks = new HashMap<>();

        public HashMap<String, String> getDefaultStyleBlock() {
            return this.defaultStyleBlock;
        }

        public Map<String, HashMap<String, String>> getStyleBlocks() {
            return this.styleBlocks;
        }
    }
}
