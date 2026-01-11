// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig.generators;

import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorEnumAttribute extends RandomGenerator<GeneratorEnumAttribute> {
    private final AttributeType.Enum attributeType;
    private final String str;

    public GeneratorEnumAttribute(AttributeType attributeType, String s) {
        this(attributeType, 1.0F, s);
    }

    public GeneratorEnumAttribute(AttributeType attributeType, float chance, String s) {
        if (chance < 0.0F) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        } else if (attributeType instanceof AttributeType.Enum anEnum) {
            this.attributeType = anEnum;
            this.setChance(chance);
            this.str = s;
        } else {
            throw new IllegalArgumentException("AttributeType valueType should be Enum.");
        }
    }

    @Override
    public boolean execute(GameEntity entity) {
        if (entity.getAttributes() == null || this.attributeType.getValueType() != AttributeValueType.Enum) {
            return false;
        } else if (entity.getAttributes().contains(this.attributeType)) {
            entity.getAttributes().putFromScript(this.attributeType, this.str);
            return true;
        } else {
            return false;
        }
    }

    public GeneratorEnumAttribute copy() {
        return new GeneratorEnumAttribute(this.attributeType, this.getChance(), this.str);
    }
}
