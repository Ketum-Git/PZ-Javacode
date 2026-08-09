// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.combat;

import zombie.entity.ComponentType;
import zombie.iso.enums.MaterialType;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;

public class DurabilityScript extends ComponentScript {
    private float maxHitPoints;
    private float currentHitPoints;
    private MaterialType material;

    private DurabilityScript() {
        super(ComponentType.Durability);
    }

    public float getMaxHitPoints() {
        return this.maxHitPoints;
    }

    public float getCurrentHitPoints() {
        return this.currentHitPoints;
    }

    public MaterialType getMaterial() {
        return this.material;
    }

    @Override
    protected void copyFrom(ComponentScript componentScript) {
        DurabilityScript other = (DurabilityScript)componentScript;
        this.currentHitPoints = other.currentHitPoints;
        this.maxHitPoints = other.maxHitPoints;
        this.material = other.material;
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        super.load(block);

        for (ScriptParser.BlockElement element : block.elements) {
            if (element.asValue() != null) {
                String s = element.asValue().string;
                if (!s.trim().isEmpty() && s.contains("=")) {
                    String[] split = s.split("=");
                    String k = split[0].trim();
                    String v = split[1].trim();
                    if (k.equalsIgnoreCase("MaxHitPoints")) {
                        this.maxHitPoints = Float.parseFloat(v);
                        this.currentHitPoints = this.maxHitPoints;
                    } else if (k.equalsIgnoreCase("Material")) {
                        this.material = MaterialType.valueOf(v);
                    }
                }
            }
        }
    }
}
