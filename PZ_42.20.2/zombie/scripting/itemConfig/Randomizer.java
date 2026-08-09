// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig;

import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.entity.GameEntity;

public class Randomizer {
    private final RandomGenerator[] generators;

    public Randomizer(RandomGenerator[] rngConfigs) throws ItemConfig.ItemConfigException {
        this.generators = rngConfigs;
        if (this.generators.length == 0) {
            throw new ItemConfig.ItemConfigException("Attempting to construct a Randomizer with no entries.");
        } else {
            PZMath.normalize(this.generators, RandomGenerator::getChance, RandomGenerator::setChance);
        }
    }

    public Randomizer(Randomizer other) {
        this.generators = new RandomGenerator[other.generators.length];

        for (int i = 0; i < this.generators.length; i++) {
            this.generators[i] = other.generators[i].copy();
        }
    }

    public boolean execute(GameEntity entity) {
        if (this.generators.length > 1) {
            float roll = Rand.Next(0.0F, 1.0F);
            float chance = 1.0F;

            for (int i = this.generators.length - 1; i >= 1; i--) {
                RandomGenerator generator = this.generators[i];
                if (roll > chance - generator.getChance() && roll <= chance) {
                    return generator.execute(entity);
                }

                chance -= generator.getChance();
            }
        }

        RandomGenerator generator = this.generators[0];
        return generator.execute(entity);
    }

    public Randomizer copy() {
        return new Randomizer(this);
    }
}
