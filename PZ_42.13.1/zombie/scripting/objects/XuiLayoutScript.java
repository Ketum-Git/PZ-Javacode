// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.ui.XuiScript;
import zombie.scripting.ui.XuiScriptType;

@UsedFromLua
public class XuiLayoutScript extends BaseScriptObject {
    private XuiScript xuiScript;
    private String name;
    private String totalFile;
    private boolean hasParsed;
    private final XuiScriptType scriptType;
    private ScriptParser.Block block;

    public XuiLayoutScript(ScriptType scriptType, XuiScriptType xuiScriptType) {
        super(scriptType);
        this.scriptType = xuiScriptType;
    }

    public String getName() {
        return this.name;
    }

    public XuiScriptType getScriptType() {
        return this.scriptType;
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        this.hasParsed = false;
        this.totalFile = totalFile;
        this.name = name;
        this.LoadCommonBlock(totalFile);
    }

    public void preParse() {
        this.block = ScriptParser.parse(this.totalFile);
        this.block = this.block.children.get(0);
        String luaClass = XuiScript.ReadLuaClassValue(this.block);
        this.xuiScript = XuiScript.CreateScriptForClass(this.name, luaClass, true, this.scriptType);
    }

    public void parseScript() {
        if (!this.hasParsed) {
            this.hasParsed = true;
            this.xuiScript.Load(this.block);
            this.block = null;
        }
    }

    public XuiScript getXuiScript() {
        return this.xuiScript;
    }
}
