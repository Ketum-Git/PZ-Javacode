// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.ui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.XuiColorsScript;
import zombie.scripting.objects.XuiConfigScript;
import zombie.scripting.objects.XuiLayoutScript;
import zombie.scripting.objects.XuiSkinScript;

@UsedFromLua
public class XuiManager {
    private static final String DEFAULT_SKIN_NAME = "default";
    public static final EnumSet<ScriptType> XUI_SCRIPT_TYPES = EnumSet.of(
        ScriptType.XuiConfig, ScriptType.XuiLayout, ScriptType.XuiStyle, ScriptType.XuiDefaultStyle, ScriptType.XuiColor, ScriptType.XuiSkin
    );
    private static final Map<String, XuiLayoutScript> layoutScriptsMap = new HashMap<>();
    private static final Map<String, XuiLayoutScript> stylesScriptsMap = new HashMap<>();
    private static final Map<String, XuiLayoutScript> defaultStylesScriptsMap = new HashMap<>();
    private static final ArrayList<XuiScript> combinedList = new ArrayList<>();
    private static final ArrayList<XuiScript> xuiLayoutsList = new ArrayList<>();
    private static final ArrayList<XuiScript> xuiStylesList = new ArrayList<>();
    private static final ArrayList<XuiScript> xuiDefaultStylesList = new ArrayList<>();
    private static final Map<String, XuiScript> xuiLayouts = new HashMap<>();
    private static final Map<String, XuiScript> xuiStyles = new HashMap<>();
    private static final Map<String, XuiScript> xuiDefaultStyles = new HashMap<>();
    private static final Map<String, XuiSkin> xuiSkins = new HashMap<>();
    private static XuiSkin xuiDefaultSkin;
    private static boolean parseOnce;
    private static boolean hasParsedOnce;

    public static String getDefaultSkinName() {
        return "default";
    }

    public static ArrayList<XuiScript> GetCombinedScripts() {
        return combinedList;
    }

    public static ArrayList<XuiScript> GetAllLayouts() {
        return xuiLayoutsList;
    }

    public static ArrayList<XuiScript> GetAllStyles() {
        return xuiStylesList;
    }

    public static ArrayList<XuiScript> GetAllDefaultStyles() {
        return xuiDefaultStylesList;
    }

    public static XuiLayoutScript GetLayoutScript(String name) {
        return name == null ? null : layoutScriptsMap.get(name);
    }

    public static XuiLayoutScript GetStyleScript(String name) {
        return name == null ? null : stylesScriptsMap.get(name);
    }

    public static XuiLayoutScript GetDefaultStyleScript(String name) {
        return name == null ? null : defaultStylesScriptsMap.get(name);
    }

    public static XuiScript GetLayout(String name) {
        return name == null ? null : xuiLayouts.get(name);
    }

    public static XuiScript GetStyle(String style) {
        return style == null ? null : xuiStyles.get(style);
    }

    public static XuiScript GetDefaultStyle(String luaClass) {
        return luaClass == null ? null : xuiDefaultStyles.get(luaClass);
    }

    public static XuiSkin GetDefaultSkin() {
        return xuiDefaultSkin;
    }

    public static XuiSkin GetSkin(String name) {
        XuiSkin skin = xuiSkins.get(name);
        if (skin == null) {
            if (name != null) {
                DebugLog.General.warn("Skin not found: " + name);
            }

            skin = xuiDefaultSkin;
        }

        return skin;
    }

    private static void reset() {
        layoutScriptsMap.clear();
        stylesScriptsMap.clear();
        defaultStylesScriptsMap.clear();
        combinedList.clear();
        xuiLayoutsList.clear();
        xuiStylesList.clear();
        xuiDefaultStylesList.clear();
        xuiLayouts.clear();
        xuiStyles.clear();
        xuiDefaultStyles.clear();

        for (XuiSkin skin : xuiSkins.values()) {
            skin.setInvalidated(true);
        }

        xuiSkins.clear();
        xuiDefaultSkin = null;
        XuiLuaStyle.Reset();
    }

    public static void setParseOnce(boolean b) {
        parseOnce = b;
        hasParsedOnce = false;
    }

    public static void ParseScripts() throws Exception {
        if (!parseOnce || !hasParsedOnce) {
            hasParsedOnce = true;
            reset();
            ArrayList<XuiConfigScript> configs = ScriptManager.instance.getAllXuiConfigScripts();
            XuiLuaStyle.ReadConfigs(configs);
            ArrayList<XuiColorsScript> colors = ScriptManager.instance.getAllXuiColors();
            ArrayList<XuiLayoutScript> styles = ScriptManager.instance.getAllXuiStyles();
            ArrayList<XuiLayoutScript> defaultStyles = ScriptManager.instance.getAllXuiDefaultStyles();
            ArrayList<XuiLayoutScript> layouts = ScriptManager.instance.getAllXuiLayouts();
            ArrayList<XuiSkinScript> skins = ScriptManager.instance.getAllXuiSkinScripts();

            for (XuiColorsScript colorsScript : colors) {
                for (Entry<String, Color> entry : colorsScript.getColorMap().entrySet()) {
                    if (Colors.GetColorInfo(entry.getKey()) != null) {
                        Colors.ColNfo nfo = Colors.GetColorInfo(entry.getKey());
                        if (nfo.getColorSet() == Colors.ColorSet.Game) {
                            nfo.getColor().set(entry.getValue());
                            continue;
                        }
                    }

                    if (Colors.GetColorByName(entry.getKey()) == null) {
                        Colors.AddGameColor(entry.getKey(), entry.getValue());
                    } else {
                        DebugLog.General.error("Color '" + entry.getKey() + "' is already defined in Colors.java");
                    }
                }
            }

            for (XuiLayoutScript script : defaultStyles) {
                if (script.getName() != null) {
                    defaultStylesScriptsMap.put(script.getName(), script);
                }

                registerLayout(script);
            }

            for (XuiLayoutScript script : styles) {
                if (script.getName() != null) {
                    stylesScriptsMap.put(script.getName(), script);
                }

                registerLayout(script);
            }

            for (XuiLayoutScript script : layouts) {
                if (script.getName() != null) {
                    layoutScriptsMap.put(script.getName(), script);
                }

                registerLayout(script);
            }

            for (XuiLayoutScript script : defaultStyles) {
                parseLayout(script);
            }

            for (XuiLayoutScript script : styles) {
                parseLayout(script);
            }

            for (XuiLayoutScript script : layouts) {
                parseLayout(script);
            }

            combinedList.addAll(xuiLayoutsList);
            combinedList.addAll(xuiStylesList);
            combinedList.addAll(xuiDefaultStylesList);

            for (XuiSkinScript script : skins) {
                String name = script.getScriptObjectName();
                if (!script.getModule().getName().equals("Base")) {
                    DebugLog.General.warn("XuiSkin '" + script.getScriptObjectFullType() + "' ignored, skin needs to be module Base.");
                } else {
                    XuiSkin skin = new XuiSkin(name, script);
                    xuiSkins.put(name, skin);
                }
            }

            for (XuiSkin skin : xuiSkins.values()) {
                skin.Load();
            }

            xuiDefaultSkin = GetSkin("default");
        }
    }

    private static void registerLayout(XuiLayoutScript layoutScript) {
        try {
            layoutScript.preParse();
            XuiScript script = layoutScript.getXuiScript();
            if (script != null) {
                if (script.getScriptType() == XuiScriptType.Layout) {
                    xuiLayoutsList.add(script);
                    xuiLayouts.put(layoutScript.getName(), script);
                } else if (script.getScriptType() == XuiScriptType.Style) {
                    xuiStylesList.add(script);
                    xuiStyles.put(layoutScript.getName(), script);
                } else if (script.getScriptType() == XuiScriptType.DefaultStyle) {
                    xuiDefaultStylesList.add(script);
                    xuiDefaultStyles.put(layoutScript.getName(), script);
                }
            } else {
                DebugLog.General.error("No XuiScript in XuiConfig: " + layoutScript.getName());
            }
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }

    private static void parseLayout(XuiLayoutScript layoutScript) {
        try {
            layoutScript.parseScript();
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }
}
