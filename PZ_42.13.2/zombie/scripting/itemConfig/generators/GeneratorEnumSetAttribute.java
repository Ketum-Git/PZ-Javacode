// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig.generators;

import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeInstance;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorEnumSetAttribute extends RandomGenerator<GeneratorEnumSetAttribute> {
    private final AttributeType.EnumSet attributeType;
    private final String[] values;
    private final GeneratorEnumSetAttribute.Mode mode;

    public GeneratorEnumSetAttribute(AttributeType attributeType, GeneratorEnumSetAttribute.Mode mode, String[] s) {
        this(attributeType, mode, 1.0F, s);
    }

    public GeneratorEnumSetAttribute(AttributeType attributeType, GeneratorEnumSetAttribute.Mode mode, float chance, String[] s) {
        if (chance < 0.0F) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        } else if (attributeType instanceof AttributeType.EnumSet enumSet) {
            this.attributeType = enumSet;
            this.setChance(chance);
            this.values = s;
            this.mode = mode;
        } else {
            throw new IllegalArgumentException("AttributeType valueType should be EnumSet.");
        }
    }

    @Override
    public boolean execute(GameEntity entity) {
        if (entity.getAttributes() == null || this.attributeType.getValueType() != AttributeValueType.EnumSet) {
            return false;
        } else if (entity.getAttributes().contains(this.attributeType)) {
            try {
                AttributeInstance.EnumSet enumSet = (AttributeInstance.EnumSet)entity.getAttributes().getAttribute(this.attributeType);
                if (this.mode == GeneratorEnumSetAttribute.Mode.Set) {
                    enumSet.clear();
                }

                if (this.mode == GeneratorEnumSetAttribute.Mode.Remove) {
                    for (String s : this.values) {
                        if (!enumSet.removeValueFromString(s)) {
                            DebugLog.General.error("Unable to remove value '" + s + "'");
                        }
                    }
                } else {
                    for (String sx : this.values) {
                        enumSet.addValueFromString(sx);
                    }
                }
            } catch (Exception var7) {
                var7.printStackTrace();
            }

            return true;
        } else {
            return false;
        }
    }

    public GeneratorEnumSetAttribute copy() {
        return new GeneratorEnumSetAttribute(this.attributeType, this.mode, this.getChance(), this.values);
    }

    public static enum Mode {
        Set,
        Add,
        Remove;
    }
}
