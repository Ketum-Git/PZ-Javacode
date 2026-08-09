// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import org.joml.Vector3f;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

@UsedFromLua
public class PhysicsShapeScript extends BaseScriptObject {
    public String meshName;
    public final Vector3f translate = new Vector3f();
    public final Vector3f rotate = new Vector3f();
    public float scale = 1.0F;
    public String postProcess;
    public boolean allMeshes;

    protected PhysicsShapeScript() {
        super(ScriptType.PhysicsShape);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);

        for (ScriptParser.Block child : block.children) {
            if ("xxx".equals(child.type)) {
            }
        }

        boolean bUndoCoreScale = false;

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("mesh".equalsIgnoreCase(k)) {
                this.meshName = v;
            } else if ("translate".equalsIgnoreCase(k)) {
                this.parseVector3f(v, this.translate);
            } else if ("rotate".equalsIgnoreCase(k)) {
                this.parseVector3f(v, this.rotate);
            } else if ("scale".equalsIgnoreCase(k)) {
                this.scale = Float.parseFloat(v);
            } else if ("allMeshes".equalsIgnoreCase(k)) {
                this.allMeshes = Boolean.parseBoolean(v);
            } else if ("postProcess".equalsIgnoreCase(k)) {
                this.postProcess = v;
            } else if ("undoCoreScale".equalsIgnoreCase(k)) {
                bUndoCoreScale = Boolean.parseBoolean(v);
            }
        }

        if (bUndoCoreScale) {
            this.scale *= 0.6666667F;
        }
    }

    private void parseVector3f(String str, Vector3f v) {
        String[] ss = str.split(" ");
        v.setComponent(0, PZMath.tryParseFloat(ss[0], 0.0F));
        v.setComponent(1, PZMath.tryParseFloat(ss[1], 0.0F));
        v.setComponent(2, PZMath.tryParseFloat(ss[2], 0.0F));
    }
}
