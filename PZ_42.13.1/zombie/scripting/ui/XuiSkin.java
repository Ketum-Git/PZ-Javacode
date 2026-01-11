// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.XuiSkinScript;

@UsedFromLua
public class XuiSkin {
    private final ArrayList<XuiSkin> imports = new ArrayList<>();
    private final Map<String, Color> colorMap = new HashMap<>();
    private final XuiSkin.EntityUiStyle defaultEntityUiStyle = new XuiSkin.EntityUiStyle();
    private final Map<String, XuiSkin.EntityUiStyle> entityUiStyleMap = new HashMap<>();
    private final Map<String, XuiSkin.StyleInfo> styles = new HashMap<>();
    private final String name;
    private final XuiSkinScript script;
    private boolean hasLoaded;
    private boolean invalidated;

    public static XuiSkin Default() {
        return XuiManager.GetDefaultSkin();
    }

    public static String getDefaultSkinName() {
        return XuiManager.getDefaultSkinName();
    }

    public XuiSkin(String name, XuiSkinScript script) {
        this.name = name;
        this.script = script;
    }

    public boolean isInvalidated() {
        return this.invalidated;
    }

    protected void setInvalidated(boolean b) {
        this.invalidated = b;
    }

    public String getName() {
        return this.name;
    }

    public String getEntityDisplayName(String entityAlias) {
        return this.getEntityUiStyle(entityAlias).getDisplayName();
    }

    public XuiSkin.EntityUiStyle getEntityUiStyle(String alias) {
        XuiSkin.EntityUiStyle uiInfo;
        if (alias == null) {
            uiInfo = this.defaultEntityUiStyle;
        } else {
            uiInfo = this.entityUiStyleMap.get(alias);
            if (uiInfo == null) {
                DebugLog.General.warn("Cannot find entity ui info: " + alias + ", attempting default...");
                uiInfo = this.defaultEntityUiStyle;
            }
        }

        return uiInfo;
    }

    public XuiSkin.ComponentUiStyle getComponentUiStyle(String entityAlias, ComponentType componentType) {
        XuiSkin.EntityUiStyle uiInfo = this.getEntityUiStyle(entityAlias);
        return uiInfo.getComponentUiStyle(componentType);
    }

    public Color color(String alias) {
        return this.colorInternal(alias, true, true);
    }

    protected Color colorInternal(String alias, boolean doSystemColorsFallback, boolean allowNull) {
        if (alias == null) {
            return null;
        } else if (this.colorMap.containsKey(alias)) {
            return this.colorMap.get(alias);
        } else if (doSystemColorsFallback) {
            return Colors.GetColorByName(alias);
        } else {
            return !allowNull ? Colors.White : null;
        }
    }

    public XuiLuaStyle getDefault(String luaClass) {
        return this.get(luaClass, null);
    }

    public XuiLuaStyle get(String luaClass, String alias) {
        XuiSkin.StyleInfo styleInfo = this.styles.get(luaClass);
        if (styleInfo != null) {
            return alias == null ? styleInfo.defaultStyle : styleInfo.getStyle(alias);
        } else {
            return null;
        }
    }

    protected void Load() throws Exception {
        if (!this.hasLoaded) {
            this.hasLoaded = true;

            for (int i = 0; i < this.script.getImports().size(); i++) {
                String importName = this.script.getImports().get(i);
                XuiSkin importSkin = XuiManager.GetSkin(importName);
                XuiSkinScript importScript = ScriptManager.instance.getXuiSkinScript(importName);
                if (importSkin == null) {
                    throw new Exception("Import skin '" + importName + "' not found for skin: " + this.name);
                }

                importSkin.Load();
                this.imports.add(importSkin);
                this.LoadColors(importScript);
            }

            this.LoadColors(this.script);

            for (int i = 0; i < this.script.getImports().size(); i++) {
                String importName = this.script.getImports().get(i);
                XuiSkinScript importScript = ScriptManager.instance.getXuiSkinScript(importName);
                this.LoadDefaultEntityUiInfo(importScript);
                this.LoadAllDefaultStyles(importScript);
            }

            this.LoadDefaultEntityUiInfo(this.script);
            this.LoadAllDefaultStyles(this.script);

            for (int i = 0; i < this.script.getImports().size(); i++) {
                String importName = this.script.getImports().get(i);
                XuiSkinScript importScript = ScriptManager.instance.getXuiSkinScript(importName);
                this.LoadEntityUiInfo(importScript);
                this.LoadAllStyles(importScript);
            }

            this.LoadEntityUiInfo(this.script);
            this.LoadAllStyles(this.script);
        }
    }

    private void LoadDefaultEntityUiInfo(XuiSkinScript script) throws Exception {
        this.defaultEntityUiStyle.Load(script.getDefaultEntityUiScript());
    }

    private void LoadEntityUiInfo(XuiSkinScript script) throws Exception {
        for (Entry<String, XuiSkinScript.EntityUiScript> entry : script.getEntityUiScriptMap().entrySet()) {
            XuiSkin.EntityUiStyle uiInfo = this.entityUiStyleMap.get(entry.getKey());
            if (uiInfo == null) {
                uiInfo = this.defaultEntityUiStyle.copy();
                this.entityUiStyleMap.put(entry.getKey(), uiInfo);
            }

            uiInfo.Load(entry.getValue());
        }
    }

    private void LoadColors(XuiSkinScript script) throws Exception {
        for (Entry<String, Color> entry : script.getColorsScript().getColorMap().entrySet()) {
            this.colorMap.put(entry.getKey(), entry.getValue());
        }
    }

    private void LoadAllDefaultStyles(XuiSkinScript script) throws Exception {
        for (Entry<String, XuiSkinScript.StyleInfoScript> entry : script.getStyleInfoMap().entrySet()) {
            this.LoadDefaultStyle(entry.getKey(), entry.getValue());
        }
    }

    private void LoadDefaultStyle(String luaClassName, XuiSkinScript.StyleInfoScript script) throws Exception {
        XuiSkin.StyleInfo styleInfo = this.styles.get(luaClassName);
        if (styleInfo == null) {
            styleInfo = new XuiSkin.StyleInfo();
            this.styles.put(luaClassName, styleInfo);
        }

        Map<String, String> map = script.getDefaultStyleBlock();
        if (styleInfo.defaultStyle == null) {
            styleInfo.defaultStyle = new XuiLuaStyle(luaClassName, "default");
            styleInfo.defaultStyle.xuiSkin = this;
        }

        for (Entry<String, String> kv : map.entrySet()) {
            if (!styleInfo.defaultStyle.loadVar(kv.getKey(), kv.getValue())) {
                DebugLog.General.warn("Cannot load key = " + kv.getKey() + ", val = " + kv.getValue() + " for class = " + luaClassName);

                assert false;
            }
        }
    }

    private void LoadAllStyles(XuiSkinScript script) throws Exception {
        for (Entry<String, XuiSkinScript.StyleInfoScript> entry : script.getStyleInfoMap().entrySet()) {
            this.LoadStyle(entry.getKey(), entry.getValue());
        }
    }

    private void LoadStyle(String luaClassName, XuiSkinScript.StyleInfoScript script) throws Exception {
        XuiSkin.StyleInfo styleInfo = this.styles.get(luaClassName);
        if (styleInfo == null) {
            styleInfo = new XuiSkin.StyleInfo();
            this.styles.put(luaClassName, styleInfo);
        }

        for (Entry<String, HashMap<String, String>> style : script.getStyleBlocks().entrySet()) {
            String styleAlias = style.getKey();
            Map<String, String> map = style.getValue();
            XuiLuaStyle xuiStyle = styleInfo.getStyle(style.getKey());
            if (xuiStyle == null) {
                xuiStyle = new XuiLuaStyle(luaClassName, styleAlias);
                xuiStyle.xuiSkin = this;
                if (styleInfo.defaultStyle != null) {
                    xuiStyle.copyVarsFrom(styleInfo.defaultStyle);
                }

                styleInfo.styles.put(style.getKey(), xuiStyle);
            }

            for (Entry<String, String> kv : map.entrySet()) {
                if (!xuiStyle.loadVar(kv.getKey(), kv.getValue())) {
                    DebugLog.General.warn("Cannot load key = " + kv.getKey() + ", val = " + kv.getValue() + " for class = " + luaClassName);

                    assert false;
                }
            }
        }
    }

    public void debugPrint() {
        DebugLog.log("---------------- SKIN ----------------");
        DebugLog.log("SkinName: " + this.name);
        DebugLog.log("imports {");

        for (XuiSkin xuiSkin : this.imports) {
            DebugLog.log("   skin - " + xuiSkin.name);
        }

        DebugLog.log("}");
        DebugLog.log("---------------- ENTITY ----------------");
        this.printEntityStyle("Default Entity Style", this.defaultEntityUiStyle);

        for (Entry<String, XuiSkin.EntityUiStyle> entry : this.entityUiStyleMap.entrySet()) {
            this.printEntityStyle("style-" + entry.getKey(), entry.getValue());
        }

        DebugLog.log("---------------- STYLES ----------------");

        for (Entry<String, XuiSkin.StyleInfo> entry : this.styles.entrySet()) {
            this.printStyle(entry.getKey(), entry.getValue());
        }

        DebugLog.log("---------------- END ----------------");
    }

    private void printEntityStyle(String styleName, XuiSkin.EntityUiStyle style) {
        DebugLog.log("[" + styleName + "]");
        DebugLog.log("WindowClass: " + style.luaWindowClass);
        DebugLog.log("XuiStyle: " + style.xuiStyle);
        DebugLog.log("DisplayName: " + style.displayName);
        DebugLog.log("Description: " + style.description);
        DebugLog.log("BuildDescription: " + style.buildDescription);
        DebugLog.log("Icon: " + style.icon);
        DebugLog.log("luaCanOpenWindow: " + style.luaCanOpenWindow);
        DebugLog.log("luaOpenWindow: " + style.luaOpenWindow);
        DebugLog.log("");
        DebugLog.log("-> <components> ");

        for (Entry<ComponentType, XuiSkin.ComponentUiStyle> entry : style.componentUiStyleMap.entrySet()) {
            this.printComponentStyle("  ", entry.getKey(), entry.getValue());
        }

        DebugLog.log("");
    }

    private void printComponentStyle(String prefix, ComponentType componentType, XuiSkin.ComponentUiStyle style) {
        DebugLog.log(prefix + "[" + componentType + "]");
        DebugLog.log(prefix + "luaPanelClass: " + style.luaPanelClass);
        DebugLog.log(prefix + "xuiStyle: " + style.xuiStyle);
        DebugLog.log(prefix + "displayName: " + style.displayName);
        DebugLog.log(prefix + "icon: " + style.icon);
        DebugLog.log(prefix + "listOrderZ: " + style.listOrderZ);
        DebugLog.log(prefix + "enabled: " + style.enabled);
        DebugLog.log("");
    }

    private void printStyle(String luaClassName, XuiSkin.StyleInfo styleInfo) {
        DebugLog.log("[" + luaClassName + "]");
        if (styleInfo.defaultStyle != null) {
            DebugLog.log("defaultStyle: " + styleInfo.defaultStyle.getXuiStyleName());
            styleInfo.defaultStyle.debugPrint("");
        } else {
            DebugLog.log("defaultStyle: null");
        }

        for (Entry<String, XuiLuaStyle> entry : styleInfo.styles.entrySet()) {
            this.printStyle("  ", entry.getKey(), entry.getValue());
        }

        DebugLog.log("");
    }

    private void printStyle(String prefix, String styleName, XuiLuaStyle style) {
        DebugLog.log(prefix + "[" + styleName + "]");
        if (style != null) {
            DebugLog.log(prefix + "style: " + style.getXuiStyleName());
            style.debugPrint(prefix);
        } else {
            DebugLog.log(prefix + "style: null");
        }

        DebugLog.log("");
    }

    @UsedFromLua
    public static class ComponentUiStyle {
        private String luaPanelClass;
        private String xuiStyle;
        private String displayName;
        private Texture icon;
        private int listOrderZ;
        private boolean enabled = true;

        public String getLuaPanelClass() {
            return this.luaPanelClass;
        }

        public String getXuiStyle() {
            return this.xuiStyle;
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public Texture getIcon() {
            return this.icon;
        }

        public int getListOrderZ() {
            return this.listOrderZ;
        }

        public String getDisplayName() {
            return Translator.getText(this.displayName);
        }

        private XuiSkin.ComponentUiStyle copy() {
            XuiSkin.ComponentUiStyle c = new XuiSkin.ComponentUiStyle();
            c.luaPanelClass = this.luaPanelClass;
            c.xuiStyle = this.xuiStyle;
            c.displayName = this.displayName;
            c.icon = this.icon;
            c.listOrderZ = this.listOrderZ;
            c.enabled = this.enabled;
            return c;
        }
    }

    @UsedFromLua
    public static class EntityUiStyle {
        private String luaWindowClass;
        private String xuiStyle;
        private String luaCanOpenWindow;
        private String luaOpenWindow;
        private final Map<ComponentType, XuiSkin.ComponentUiStyle> componentUiStyleMap = new HashMap<>();
        private String displayName;
        private String description;
        private String buildDescription;
        private Texture icon;

        public String getLuaWindowClass() {
            return this.luaWindowClass;
        }

        public String getXuiStyle() {
            return this.xuiStyle;
        }

        public Object getLuaCanOpenWindow() {
            return this.luaCanOpenWindow != null ? LuaManager.getFunctionObject(this.luaCanOpenWindow) : null;
        }

        public Object getLuaOpenWindow() {
            return this.luaOpenWindow != null ? LuaManager.getFunctionObject(this.luaOpenWindow) : null;
        }

        public String getDisplayName() {
            return Translator.getText(this.displayName);
        }

        public String getDescription() {
            return Translator.getText(this.description);
        }

        public String getBuildDescription() {
            return Translator.getText(this.buildDescription);
        }

        public Texture getIcon() {
            return this.icon;
        }

        public XuiSkin.ComponentUiStyle getComponentUiStyle(ComponentType componentType) {
            return this.componentUiStyleMap.get(componentType);
        }

        public boolean isComponentEnabled(ComponentType componentType) {
            XuiSkin.ComponentUiStyle info = this.componentUiStyleMap.get(componentType);
            return info != null && info.isEnabled();
        }

        private XuiSkin.EntityUiStyle copy() {
            XuiSkin.EntityUiStyle c = new XuiSkin.EntityUiStyle();
            c.luaWindowClass = this.luaWindowClass;
            c.xuiStyle = this.xuiStyle;
            c.luaCanOpenWindow = this.luaCanOpenWindow;
            c.luaOpenWindow = this.luaOpenWindow;
            c.displayName = this.displayName;
            c.description = this.description;
            c.buildDescription = this.buildDescription;
            c.icon = this.icon;

            for (Entry<ComponentType, XuiSkin.ComponentUiStyle> entry : this.componentUiStyleMap.entrySet()) {
                c.componentUiStyleMap.put(entry.getKey(), entry.getValue().copy());
            }

            return c;
        }

        private void Load(XuiSkinScript.EntityUiScript script) {
            if (script.getLuaWindowClass() != null) {
                this.luaWindowClass = script.getLuaWindowClass();
            }

            if (script.getXuiStyle() != null) {
                this.xuiStyle = script.getXuiStyle();
            }

            if (script.getLuaCanOpenWindow() != null) {
                this.luaCanOpenWindow = script.getLuaCanOpenWindow();
            }

            if (script.getLuaOpenWindow() != null) {
                this.luaOpenWindow = script.getLuaOpenWindow();
            }

            if (script.getDisplayName() != null) {
                this.displayName = script.getDisplayName();
            }

            if (script.getDescription() != null) {
                this.description = script.getDescription();
            }

            if (script.getBuildDescription() != null) {
                this.buildDescription = script.getBuildDescription();
            }

            if (script.getIconPath() != null) {
                Texture tex = Texture.trygetTexture(script.getIconPath());
                if (tex != null) {
                    this.icon = tex;
                } else {
                    DebugLog.General.warn("Could not find icon: " + script.getIconPath() + ", script = " + script.getDisplayName());

                    assert false;

                    if (this.icon == null) {
                        this.icon = Texture.getSharedTexture("media/inventory/Question_On.png");
                    }
                }
            }

            if (script.isClearComponents()) {
                this.componentUiStyleMap.clear();
            }

            this.LoadComponentInfo(script);
        }

        private void LoadComponentInfo(XuiSkinScript.EntityUiScript script) {
            for (Entry<ComponentType, XuiSkinScript.ComponentUiScript> entry : script.getComponentUiScriptMap().entrySet()) {
                XuiSkin.ComponentUiStyle info = this.componentUiStyleMap.get(entry.getKey());
                if (info == null) {
                    info = new XuiSkin.ComponentUiStyle();
                    this.componentUiStyleMap.put(entry.getKey(), info);
                }

                if (entry.getValue().getLuaPanelClass() != null) {
                    info.luaPanelClass = entry.getValue().getLuaPanelClass();
                }

                if (entry.getValue().getXuiStyle() != null) {
                    info.xuiStyle = entry.getValue().getXuiStyle();
                }

                if (entry.getValue().getDisplayName() != null) {
                    info.displayName = entry.getValue().getDisplayName();
                }

                if (entry.getValue().getIconPath() != null) {
                    Texture tex = Texture.trygetTexture(entry.getValue().getIconPath());
                    if (tex != null) {
                        info.icon = tex;
                    } else {
                        DebugLog.General.warn("Could not find icon: " + entry.getValue().getIconPath());

                        assert false;

                        if (info.icon == null) {
                            info.icon = Texture.getSharedTexture("media/inventory/Question_On.png");
                        }
                    }
                }

                if (entry.getValue().isListOrderZ()) {
                    info.listOrderZ = entry.getValue().getListOrderZ();
                }

                if (entry.getValue().isEnabledSet()) {
                    info.enabled = entry.getValue().isEnabled();
                }
            }
        }
    }

    private static class StyleInfo {
        private XuiLuaStyle defaultStyle;
        private final Map<String, XuiLuaStyle> styles = new HashMap<>();

        public XuiLuaStyle getDefaultStyle() {
            return this.defaultStyle;
        }

        public XuiLuaStyle getStyle(String name) {
            return this.styles.get(name);
        }
    }
}
