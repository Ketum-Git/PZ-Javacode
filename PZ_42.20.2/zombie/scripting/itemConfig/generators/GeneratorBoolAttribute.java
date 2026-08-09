// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig.generators;

import zombie.entity.GameEntity;
import zombie.entity.components.attributes.AttributeType;
import zombie.entity.components.attributes.AttributeValueType;
import zombie.scripting.itemConfig.RandomGenerator;

public class GeneratorBoolAttribute extends RandomGenerator<GeneratorBoolAttribute> {
    private final AttributeType.Bool attributeType;
    private final boolean value;

    public GeneratorBoolAttribute(AttributeType attributeType, boolean b) {
        this(attributeType, 1.0F, b);
    }

    public GeneratorBoolAttribute(AttributeType attributeType, float chance, boolean b) {
        if (chance < 0.0F) {
            throw new IllegalArgumentException("Chance may not be <= 0.");
        } else if (attributeType instanceof AttributeType.Bool bool) {
            this.attributeType = bool;
            this.setChance(chance);
            this.value = b;
        } else {
            throw new IllegalArgumentException("AttributeType valueType should be boolean.");
        }
    }

    @Override
    public boolean execute(GameEntity entity) {
        if (entity.getAttributes() == null || this.attributeType.getValueType() != AttributeValueType.Boolean) {
            return false;
        } else if (entity.getAttributes().contains(this.attributeType)) {
            entity.getAttributes().set(this.attributeType, this.value);
            return true;
        } else {
            return false;
        }
    }

    public GeneratorBoolAttribute copy() {
        return new GeneratorBoolAttribute(this.attributeType, this.getChance(), this.value);
    }
}
