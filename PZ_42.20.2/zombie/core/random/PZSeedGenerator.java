// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.random;

import org.uncommons.maths.random.SecureRandomSeedGenerator;
import org.uncommons.maths.random.SeedException;
import org.uncommons.maths.random.SeedGenerator;

public class PZSeedGenerator implements SeedGenerator {
    private static final SeedGenerator[] GENERATORS = new SeedGenerator[]{new SecureRandomSeedGenerator()};

    @Override
    public byte[] generateSeed(int length) {
        for (SeedGenerator generator : GENERATORS) {
            try {
                return generator.generateSeed(length);
            } catch (SeedException var7) {
            }
        }

        throw new IllegalStateException("All available seed generation strategies failed.");
    }
}
