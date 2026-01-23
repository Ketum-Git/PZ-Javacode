// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.ui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.scripting.objects.XuiConfigScript;
import zombie.ui.UIFont;

@UsedFromLua
public class XuiLuaStyle {
    public static final EnumSet<XuiVarType> ALLOWED_VAR_TYPES = EnumSet.of(
        XuiVarType.String,
        XuiVarType.StringList,
        XuiVarType.TranslateString,
        XuiVarType.Double,
        XuiVarType.Boolean,
        XuiVarType.FontType,
        XuiVarType.Color,
        XuiVarType.Texture
    );
    private static final Map<String, XuiLuaStyle.XuiVar<?, ?>> varRegisteryMap = new HashMap<>();
    private final String xuiLuaClass;
    private final String xuiStyleName;
    protected XuiSkin xuiSkin;
    protected HashMap<String, XuiLuaStyle.XuiVar<?, ?>> varsMap = new HashMap<>();
    protected ArrayList<XuiLuaStyle.XuiVar<?, ?>> vars = new ArrayList<>();

    private static void addStaticVar(XuiLuaStyle.XuiVar<?, ?> var) {
        if (var == null) {
            throw new RuntimeException("Var is null");
        } else if (var.getLuaTableKey() == null) {
            throw new RuntimeException("Var key is null");
        } else if (varRegisteryMap.containsKey(var.getLuaTableKey())) {
            throw new RuntimeException("Key already exists: " + var.getLuaTableKey());
        } else {
            varRegisteryMap.put(var.getLuaTableKey(), var);
        }
    }

    private static XuiLuaStyle.XuiVar<?, ?> getStaticVar(String name) {
        return varRegisteryMap.get(name);
    }

    public static void ReadConfigs(ArrayList<XuiConfigScript> configs) throws Exception {
        Map<XuiVarType, HashSet<String>> parsed = new HashMap<>();

        for (XuiVarType varType : ALLOWED_VAR_TYPES) {
            parsed.put(varType, new HashSet<>());
        }

        for (XuiConfigScript config : configs) {
            parseConfig(parsed, config);
        }
    }

    private static void parseConfig(Map<XuiVarType, HashSet<String>> parsed, XuiConfigScript config) throws Exception {
        Map<XuiVarType, ArrayList<String>> varConfigs = config.getVarConfigs();

        for (Entry<XuiVarType, ArrayList<String>> entry : varConfigs.entrySet()) {
            XuiVarType varType = entry.getKey();
            if (!ALLOWED_VAR_TYPES.contains(varType)) {
                throw new Exception("Var type not allowed: " + varType);
            }

            for (String key : entry.getValue()) {
                if (otherTypesContainsKey(parsed, key, varType)) {
                    throw new Exception("Duplicate key '" + key + "' in var type: " + entry.getKey() + ", and type: " + varType);
                }

                parsed.get(varType).add(key);
                if (!varRegisteryMap.containsKey(key)) {
                    switch (varType) {
                        case String:
                            addStaticVar(new XuiLuaStyle.XuiString(null, key));
                            break;
                        case StringList:
                            addStaticVar(new XuiLuaStyle.XuiStringList(null, key));
                            break;
                        case TranslateString:
                            addStaticVar(new XuiLuaStyle.XuiTranslateString(null, key));
                            break;
                        case Double:
                            addStaticVar(new XuiLuaStyle.XuiDouble(null, key));
                            break;
                        case Boolean:
                            addStaticVar(new XuiLuaStyle.XuiBoolean(null, key));
                            break;
                        case FontType:
                            addStaticVar(new XuiLuaStyle.XuiFontType(null, key));
                            break;
                        case Color:
                            addStaticVar(new XuiLuaStyle.XuiColor(null, key));
                            break;
                        case Texture:
                            addStaticVar(new XuiLuaStyle.XuiTexture(null, key));
                            break;
                        default:
                            throw new Exception("No handler for: " + varType);
                    }
                }
            }
        }
    }

    private static boolean otherTypesContainsKey(Map<XuiVarType, HashSet<String>> parsed, String key, XuiVarType ignoreType) throws Exception {
        for (Entry<XuiVarType, HashSet<String>> entry : parsed.entrySet()) {
            if (entry.getKey() != ignoreType && entry.getValue().contains(key)) {
                return true;
            }
        }

        return false;
    }

    public static void Reset() {
        varRegisteryMap.clear();
    }

    protected XuiLuaStyle(String xuiLuaClass, String xuiStyleName) {
        this.xuiLuaClass = xuiLuaClass;
        this.xuiStyleName = xuiStyleName;
    }

    public String getXuiLuaClass() {
        return this.xuiLuaClass;
    }

    public String getXuiStyleName() {
        return this.xuiStyleName;
    }

    public XuiLuaStyle.XuiVar<?, ?> getVar(String key) {
        return this.varsMap.get(key);
    }

    private void addVar(String key, XuiLuaStyle.XuiVar<?, ?> var) {
        if (key == null) {
            throw new RuntimeException("Key is null");
        } else if (var == null) {
            throw new RuntimeException("Var is null");
        } else if (var.getLuaTableKey() == null) {
            throw new RuntimeException("Var key is null");
        } else if (!this.varsMap.containsKey(var.getLuaTableKey()) && !this.vars.contains(var)) {
            this.varsMap.put(key, var);
            this.vars.add(var);
        } else {
            throw new RuntimeException("Var already added: " + var.getLuaTableKey());
        }
    }

    public ArrayList<XuiLuaStyle.XuiVar<?, ?>> getVars() {
        return this.vars;
    }

    public boolean loadVar(String key, String val) throws Exception {
        XuiLuaStyle.XuiVar<?, ?> var = this.varsMap.get(key);
        if (var == null) {
            XuiLuaStyle.XuiVar<?, ?> registered = varRegisteryMap.get(key);
            if (registered == null || !registered.acceptsKey(key)) {
                this.logInfo();
                throw new Exception("Variable '" + key + "' is not registered or key typo. [registered=" + registered + "]");
            }

            var = registered.copy(this);
            this.addVar(key, var);
        }

        if (val != null && var.acceptsKey(key)) {
            return var.load(key, val);
        } else if (val == null && var.acceptsKey(key)) {
            var.setValue(null);
            return true;
        } else {
            return false;
        }
    }

    public void copyVarsFrom(XuiLuaStyle other) {
        this.vars.clear();
        this.varsMap.clear();

        for (int i = 0; i < other.vars.size(); i++) {
            XuiLuaStyle.XuiVar<?, ?> var = other.vars.get(i);
            XuiLuaStyle.XuiVar<?, ?> copy = var.copy(this);
            this.addVar(copy.getLuaTableKey(), copy);
        }
    }

    @Override
    public String toString() {
        String orig = super.toString();
        return "XuiLuaStyle [class=" + this.xuiLuaClass + ", styleName=" + this.xuiStyleName + ",  u=" + orig + "]";
    }

    protected void logWithInfo(String s) {
        DebugLog.General.debugln(s);
        this.logInfo();
    }

    protected void warnWithInfo(String s) {
        DebugLog.General.debugln(s);
        this.logInfo();
    }

    protected void errorWithInfo(String s) {
        DebugLog.General.error(s);
        this.logInfo();
    }

    private void logInfo() {
        DebugLog.log(this.toString());
    }

    protected void debugPrint(String prefix) {
        for (XuiLuaStyle.XuiVar<?, ?> var : this.vars) {
            DebugLog.log(prefix + "-> " + var.getLuaTableKey() + " = " + var.getValueString());
        }
    }

    @UsedFromLua
    public static class XuiBoolean extends XuiLuaStyle.XuiVar<Boolean, XuiLuaStyle.XuiBoolean> {
        protected XuiBoolean(XuiLuaStyle parent, String key) {
            super(XuiVarType.Boolean, parent, key, false);
        }

        protected XuiBoolean(XuiLuaStyle parent, String key, boolean defaultVal) {
            super(XuiVarType.Boolean, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(Boolean.parseBoolean(val));
            } catch (Exception var3) {
                this.parent.logInfo();
                var3.printStackTrace();
            }
        }

        protected XuiLuaStyle.XuiBoolean copy(XuiLuaStyle parent) {
            XuiLuaStyle.XuiBoolean c = new XuiLuaStyle.XuiBoolean(parent, this.luaTableKey, this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public static class XuiColor extends XuiLuaStyle.XuiVar<Color, XuiLuaStyle.XuiColor> {
        protected XuiColor(XuiLuaStyle parent, String key) {
            super(XuiVarType.Color, parent, key);
        }

        protected XuiColor(XuiLuaStyle parent, String key, Color defaultVal) {
            super(XuiVarType.Color, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                Color color = null;
                if (this.parent.xuiSkin != null) {
                    color = this.parent.xuiSkin.color(val);
                }

                if (color == null) {
                    color = Colors.GetColorByName(val);
                }

                if (color == null && val.contains(":")) {
                    color = new Color();
                    String[] split = val.split(":");
                    if (split.length < 3) {
                        this.parent.errorWithInfo("Warning color has <3 values. color: " + val);
                    }

                    if (split.length > 1 && split[0].trim().equalsIgnoreCase("rgb")) {
                        for (int i = 1; i < split.length; i++) {
                            switch (i) {
                                case 1:
                                    color.r = Float.parseFloat(split[i].trim()) / 255.0F;
                                    break;
                                case 2:
                                    color.g = Float.parseFloat(split[i].trim()) / 255.0F;
                                    break;
                                case 3:
                                    color.b = Float.parseFloat(split[i].trim()) / 255.0F;
                                    break;
                                case 4:
                                    color.a = Float.parseFloat(split[i].trim()) / 255.0F;
                            }
                        }
                    } else {
                        for (int i = 0; i < split.length; i++) {
                            switch (i) {
                                case 0:
                                    color.r = Float.parseFloat(split[i].trim());
                                    break;
                                case 1:
                                    color.g = Float.parseFloat(split[i].trim());
                                    break;
                                case 2:
                                    color.b = Float.parseFloat(split[i].trim());
                                    break;
                                case 3:
                                    color.a = Float.parseFloat(split[i].trim());
                            }
                        }
                    }
                }

                if (color == null) {
                    throw new Exception("Could not read color: " + val);
                }

                this.setValue(color);
            } catch (Exception var5) {
                if (Core.debug) {
                    this.parent.logInfo();
                    var5.printStackTrace();
                } else {
                    DebugLog.General.warn("Could not read color: " + val);
                }
            }
        }

        public float getR() {
            return this.value() != null ? this.value().r : 1.0F;
        }

        public float getG() {
            return this.value() != null ? this.value().g : 1.0F;
        }

        public float getB() {
            return this.value() != null ? this.value().b : 1.0F;
        }

        public float getA() {
            return this.value() != null ? this.value().a : 1.0F;
        }

        @Override
        public String getValueString() {
            return this.getR() + ", " + this.getG() + ", " + this.getB() + ", " + this.getA();
        }

        protected XuiLuaStyle.XuiColor copy(XuiLuaStyle parent) {
            XuiLuaStyle.XuiColor c = new XuiLuaStyle.XuiColor(parent, this.luaTableKey, this.defaultValue);
            this.copyValuesTo(c);
            if (this.value != null) {
                c.value = new Color(this.value);
            }

            return c;
        }
    }

    @UsedFromLua
    public static class XuiDouble extends XuiLuaStyle.XuiVar<Double, XuiLuaStyle.XuiDouble> {
        protected XuiDouble(XuiLuaStyle parent, String key) {
            super(XuiVarType.Double, parent, key, 0.0);
        }

        protected XuiDouble(XuiLuaStyle parent, String key, double defaultVal) {
            super(XuiVarType.Double, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                this.setValue(Double.parseDouble(val));
            } catch (Exception var3) {
                this.parent.logInfo();
                var3.printStackTrace();
            }
        }

        protected XuiLuaStyle.XuiDouble copy(XuiLuaStyle parent) {
            XuiLuaStyle.XuiDouble c = new XuiLuaStyle.XuiDouble(parent, this.luaTableKey, this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public static class XuiFontType extends XuiLuaStyle.XuiVar<UIFont, XuiLuaStyle.XuiFontType> {
        protected XuiFontType(XuiLuaStyle parent, String key) {
            super(XuiVarType.FontType, parent, key, UIFont.Small);
        }

        protected XuiFontType(XuiLuaStyle parent, String key, UIFont defaultVal) {
            super(XuiVarType.FontType, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                if (val.startsWith("UIFont.")) {
                    val = val.substring(val.indexOf(".") + 1);
                }

                this.setValue(UIFont.valueOf(val));
            } catch (Exception var3) {
                this.parent.logInfo();
                var3.printStackTrace();
            }
        }

        protected XuiLuaStyle.XuiFontType copy(XuiLuaStyle parent) {
            XuiLuaStyle.XuiFontType c = new XuiLuaStyle.XuiFontType(parent, this.luaTableKey, this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public static class XuiString extends XuiLuaStyle.XuiVar<String, XuiLuaStyle.XuiString> {
        protected XuiString(XuiLuaStyle parent, String key) {
            super(XuiVarType.String, parent, key);
        }

        protected XuiString(XuiLuaStyle parent, String key, String defaultVal) {
            super(XuiVarType.String, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }

        protected XuiLuaStyle.XuiString copy(XuiLuaStyle parent) {
            XuiLuaStyle.XuiString c = new XuiLuaStyle.XuiString(parent, this.luaTableKey, this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public static class XuiStringList extends XuiLuaStyle.XuiVar<ArrayList<String>, XuiLuaStyle.XuiStringList> {
        protected XuiStringList(XuiLuaStyle parent, String key) {
            super(XuiVarType.StringList, parent, key, new ArrayList<>());
        }

        protected XuiStringList(XuiLuaStyle parent, String key, ArrayList<String> defaultVal) {
            super(XuiVarType.StringList, parent, key, defaultVal);
        }

        @Override
        protected void fromString(String val) {
            try {
                String[] split = val.split(":");
                ArrayList<String> list = new ArrayList<>(split.length);

                for (int i = 0; i < split.length; i++) {
                    list.add(split[i].trim());
                }

                this.setValue(list);
            } catch (Exception var5) {
                this.parent.logInfo();
                var5.printStackTrace();
            }
        }

        protected XuiLuaStyle.XuiStringList copy(XuiLuaStyle parent) {
            XuiLuaStyle.XuiStringList c = new XuiLuaStyle.XuiStringList(parent, this.luaTableKey, this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public static class XuiTexture extends XuiLuaStyle.XuiVar<String, XuiLuaStyle.XuiTexture> {
        protected XuiTexture(XuiLuaStyle parent, String key) {
            super(XuiVarType.Texture, parent, key);
        }

        protected XuiTexture(XuiLuaStyle parent, String key, String defaultVal) {
            super(XuiVarType.Texture, parent, key, defaultVal);
        }

        public Texture getTexture() {
            if (this.value() != null) {
                Texture tex = Texture.getSharedTexture(this.value());
                if (tex != null) {
                    return tex;
                }

                if (Core.debug) {
                    DebugLog.General.warn("Could not find texture for: " + this.value());
                }
            }

            return null;
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }

        protected XuiLuaStyle.XuiTexture copy(XuiLuaStyle parent) {
            XuiLuaStyle.XuiTexture c = new XuiLuaStyle.XuiTexture(parent, this.luaTableKey, this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public static class XuiTranslateString extends XuiLuaStyle.XuiVar<String, XuiLuaStyle.XuiTranslateString> {
        protected XuiTranslateString(XuiLuaStyle parent, String key) {
            super(XuiVarType.TranslateString, parent, key);
        }

        protected XuiTranslateString(XuiLuaStyle parent, String key, String defaultVal) {
            super(XuiVarType.TranslateString, parent, key, defaultVal);
        }

        public String value() {
            return super.value() == null ? null : Translator.getText((String)super.value());
        }

        @Override
        protected void fromString(String val) {
            this.setValue(val);
        }

        @Override
        public String getValueString() {
            return super.value() != null ? (String)super.value() : "null";
        }

        protected XuiLuaStyle.XuiTranslateString copy(XuiLuaStyle parent) {
            XuiLuaStyle.XuiTranslateString c = new XuiLuaStyle.XuiTranslateString(parent, this.luaTableKey, this.defaultValue);
            this.copyValuesTo(c);
            return c;
        }
    }

    @UsedFromLua
    public abstract static class XuiVar<T, C extends XuiLuaStyle.XuiVar<?, ?>> {
        private int uiOrder = 1000;
        protected final XuiVarType type;
        protected final XuiLuaStyle parent;
        protected boolean valueSet;
        protected XuiAutoApply autoApply = XuiAutoApply.IfSet;
        protected T defaultValue;
        protected T value;
        protected final String luaTableKey;

        protected XuiVar(XuiVarType type, XuiLuaStyle parent, String key) {
            this(type, parent, key, null);
        }

        protected XuiVar(XuiVarType type, XuiLuaStyle parent, String key, T defaultVal) {
            this.type = Objects.requireNonNull(type);
            this.parent = parent;
            this.luaTableKey = Objects.requireNonNull(key);
            this.defaultValue = defaultVal;
        }

        protected abstract XuiLuaStyle.XuiVar<T, C> copy(XuiLuaStyle arg0);

        protected XuiLuaStyle.XuiVar<T, C> copyValuesTo(XuiLuaStyle.XuiVar<T, C> c) {
            c.uiOrder = this.uiOrder;
            c.valueSet = this.valueSet;
            c.autoApply = this.autoApply;
            c.defaultValue = this.defaultValue;
            c.value = this.value;
            return c;
        }

        public XuiVarType getType() {
            return this.type;
        }

        public int setUiOrder(int order) {
            this.uiOrder = order;
            return this.uiOrder;
        }

        public int getUiOrder() {
            return this.uiOrder;
        }

        protected void setDefaultValue(T value) {
            this.defaultValue = value;
        }

        protected T getDefaultValue() {
            return this.defaultValue;
        }

        public void setValue(T value) {
            this.value = value;
            this.valueSet = true;
        }

        public void setAutoApplyMode(XuiAutoApply autoApplyMode) {
            this.autoApply = autoApplyMode;
        }

        public XuiAutoApply getAutoApplyMode() {
            return this.autoApply;
        }

        public String getLuaTableKey() {
            return this.luaTableKey;
        }

        protected String getScriptKey() {
            return this.luaTableKey;
        }

        public boolean isValueSet() {
            return this.valueSet;
        }

        public T value() {
            return this.valueSet ? this.value : this.defaultValue;
        }

        public String getValueString() {
            return this.value() != null ? this.value().toString() : "null";
        }

        protected boolean acceptsKey(String key) {
            return this.luaTableKey.equals(key);
        }

        protected abstract void fromString(String arg0);

        protected boolean load(String key, String val) {
            if (this.acceptsKey(key)) {
                this.fromString(val);
                return true;
            } else {
                return false;
            }
        }
    }
}
