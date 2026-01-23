// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig.generators;

import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeInstance;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorEnumStringSetAttribute extends RandomGenerator<GeneratorEnumStringSetAttribute> {
    private final AttributeType.EnumStringSet attributeType;
    private final String[] enumsValues;
    private final String[] stringValues;
    private final GeneratorEnumStringSetAttribute.Mode mode;

    public GeneratorEnumStringSetAttribute(AttributeType attributeType, GeneratorEnumStringSetAttribute.Mode mode, String[] enums, String[] strings) {
        this(attributeType, mode, 1.0F, enums, strings);
    }

    public GeneratorEnumStringSetAttribute(
        AttributeType attributeType, GeneratorEnumStringSetAttribute.Mode mode, float chance, String[] enums, String[] strings
    ) {
        if (chance < 0.0F) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        } else if (attributeType instanceof AttributeType.EnumStringSet enumStringSet) {
            this.attributeType = enumStringSet;
            this.setChance(chance);
            this.enumsValues = enums;
            this.stringValues = strings;
            this.mode = mode;
        } else {
            throw new IllegalArgumentException("AttributeType valueType should be EnumStringSet.");
        }
    }

    @Override
    public boolean execute(GameEntity entity) {
        if (entity.getAttributes() == null || this.attributeType.getValueType() != AttributeValueType.EnumSet) {
            return false;
        } else if (entity.getAttributes().contains(this.attributeType)) {
            try {
                AttributeInstance.EnumStringSet enumStringSet = (AttributeInstance.EnumStringSet)entity.getAttributes().getAttribute(this.attributeType);
                if (this.mode == GeneratorEnumStringSetAttribute.Mode.Set) {
                    enumStringSet.clear();
                }

                if (this.mode == GeneratorEnumStringSetAttribute.Mode.Remove) {
                    if (this.enumsValues != null) {
                        for (String s : this.enumsValues) {
                            if (!enumStringSet.removeEnumValueFromString(s)) {
                                DebugLog.General.error("Unable to remove value '" + s + "'");
                            }
                        }
                    }

                    if (this.stringValues != null) {
                        for (String sx : this.stringValues) {
                            if (!enumStringSet.removeStringValue(sx)) {
                                DebugLog.General.error("Unable to remove value '" + sx + "'");
                            }
                        }
                    }
                } else {
                    if (this.enumsValues != null) {
                        for (String sxx : this.enumsValues) {
                            enumStringSet.addEnumValueFromString(sxx);
                        }
                    }

                    if (this.stringValues != null) {
                        for (String sxx : this.stringValues) {
                            enumStringSet.addStringValue(sxx);
                        }
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

    public GeneratorEnumStringSetAttribute copy() {
        return new GeneratorEnumStringSetAttribute(this.attributeType, this.mode, this.getChance(), this.enumsValues, this.stringValues);
    }

    public static enum Mode {
        Set,
        Add,
        Remove;
    }
}
