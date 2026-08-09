// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.ui;

import zombie.UsedFromLua;
import zombie.scripting.ScriptParser;

@UsedFromLua
public class XuiReference extends XuiScript {
    public final XuiScript.XuiString layout = this.addVar(new XuiScript.XuiString(this, "layout"));
    public final XuiScript.XuiBoolean dynamic;
    private XuiScript referenceScript;

    public XuiReference(String xuiLayoutName, boolean readAltKeys) {
        super(xuiLayoutName, readAltKeys, "Reference", XuiScriptType.Reference);
        this.layout.setIgnoreStyling(true);
        this.layout.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.dynamic = this.addVar(new XuiScript.XuiBoolean(this, "dynamic"));
        this.dynamic.setIgnoreStyling(true);
        this.dynamic.setAutoApplyMode(XuiAutoApply.Forbidden);
    }

    public XuiScript getReferenceLayout() {
        return this.referenceScript;
    }

    public XuiScript.XuiString getLayout() {
        return this.layout;
    }

    public XuiScript.XuiBoolean getDynamic() {
        return this.dynamic;
    }

    @Override
    public void Load(ScriptParser.Block block) {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (this.layout.acceptsKey(key)) {
                    this.layout.fromString(val);
                } else if (this.dynamic.acceptsKey(key)) {
                    this.dynamic.fromString(val);
                }
            }
        }

        if (this.layout.value() != null && !this.dynamic.value()) {
            XuiScript script = XuiManager.GetLayout(this.layout.value());
            if (script != null) {
                this.referenceScript = script;
            }
        }
    }

    @Override
    public void setStyle(XuiScript style) {
        this.warnWithInfo("Cannot set style on style.");
    }

    @Override
    public void setDefaultStyle(XuiScript defaultStyle) {
        this.warnWithInfo("Cannot set style on style.");
    }

    @Override
    public void addChild(XuiScript child) {
        this.warnWithInfo("Cannot add children to style.");
    }
}
