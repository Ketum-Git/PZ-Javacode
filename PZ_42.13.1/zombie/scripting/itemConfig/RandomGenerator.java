// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig;

import zombie.entity.GameEntity;

public abstract class RandomGenerator<T extends RandomGenerator<T>> {
    private float chance = 1.0F;

    protected void setChance(float f) {
        this.chance = f;
    }

    protected float getChance() {
        return this.chance;
    }

    public abstract boolean execute(GameEntity arg0);

    public abstract T copy();
}
