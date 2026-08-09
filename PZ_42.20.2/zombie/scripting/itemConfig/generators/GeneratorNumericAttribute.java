// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig.generators;

import zombie.core.random.Rand;
import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorNumericAttribute extends RandomGenerator<GeneratorNumericAttribute> {
    private final AttributeType.Numeric<?, ?> attributeType;
    private final float min;
    private final float max;

    public GeneratorNumericAttribute(AttributeType attributeType, float max) {
        this(attributeType, 1.0F, 0.0F, max);
    }

    public GeneratorNumericAttribute(AttributeType attributeType, float min, float max) {
        this(attributeType, 1.0F, min, max);
    }

    public GeneratorNumericAttribute(AttributeType attributeType, float chance, float min, float max) {
        if (min > max) {
            max = min;
            min = min;
        }

        if (chance < 0.0F) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        } else if (attributeType instanceof AttributeType.Numeric<?, ?> numeric) {
            this.attributeType = numeric;
            this.setChance(chance);
            this.min = min;
            this.max = max;
        } else {
            throw new IllegalArgumentException("AttributeType valueType should be numeric.");
        }
    }

    @Override
    public boolean execute(GameEntity entity) {
        if (entity.getAttributes() != null && AttributeValueType.IsNumeric(this.attributeType.getValueType())) {
            if (entity.getAttributes().contains(this.attributeType)) {
                if (this.min == this.max) {
                    entity.getAttributes().setFloatValue(this.attributeType, this.min);
                } else {
                    entity.getAttributes().setFloatValue(this.attributeType, Rand.Next(this.min, this.max));
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public GeneratorNumericAttribute copy() {
        return new GeneratorNumericAttribute(this.attributeType, this.getChance(), this.min, this.max);
    }
}
