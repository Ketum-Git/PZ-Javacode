// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig.generators;

import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorStringAttribute extends RandomGenerator<GeneratorStringAttribute> {
    private final AttributeType.String attributeType;
    private final String str;

    public GeneratorStringAttribute(AttributeType attributeType, String s) {
        this(attributeType, 1.0F, s);
    }

    public GeneratorStringAttribute(AttributeType attributeType, float chance, String s) {
        if (chance < 0.0F) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        } else if (attributeType instanceof AttributeType.String string) {
            this.attributeType = string;
            this.setChance(chance);
            this.str = s;
        } else {
            throw new IllegalArgumentException("AttributeType valueType should be string.");
        }
    }

    @Override
    public boolean execute(GameEntity entity) {
        if (entity.getAttributes() == null || this.attributeType.getValueType() != AttributeValueType.String) {
            return false;
        } else if (entity.getAttributes().contains(this.attributeType)) {
            entity.getAttributes().set(this.attributeType, this.str);
            return true;
        } else {
            return false;
        }
    }

    public GeneratorStringAttribute copy() {
        return new GeneratorStringAttribute(this.attributeType, this.getChance(), this.str);
    }
}
