// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen;

import java.util.Random;
import zombie.iso.weather.SimplexNoise;
import zombie.iso.worldgen.biomes.BiomeNoise;

public class WorldGenSimplexGenerator {
    private final WorldGenSimplexGenerator.WGSimplexNoise noises;
    private final WorldGenSimplexGenerator.WGSimplex selector;

    public WorldGenSimplexGenerator(long seed) {
        Random rnd = new Random(seed + 100L);
        WorldGenSimplexGenerator.WGSimplex tree = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 128.0, 0.0);
        rnd = new Random(seed + 200L);
        WorldGenSimplexGenerator.WGSimplex plant = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 128.0, 0.0);
        rnd = new Random(seed + 300L);
        WorldGenSimplexGenerator.WGSimplex bush = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 128.0, 0.0);
        rnd = new Random(seed + 400L);
        WorldGenSimplexGenerator.WGSimplex temperature = new WorldGenSimplexGenerator.WGSimplex(
            rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 128.0, 0.0
        );
        rnd = new Random(seed + 500L);
        WorldGenSimplexGenerator.WGSimplex hygrometry = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 128.0, 0.0);
        rnd = new Random(seed + 600L);
        WorldGenSimplexGenerator.WGSimplex ore = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 128.0, 0.0);
        this.noises = new WorldGenSimplexGenerator.WGSimplexNoise(tree, plant, bush, temperature, hygrometry, ore);
        rnd = new Random(seed + 700L);
        this.selector = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 16.0, 0.0);
    }

    public BiomeNoise noise(double x, double y) {
        return new BiomeNoise(
            this.noises.tree().noise(x, y),
            this.noises.plant().noise(x, y),
            this.noises.bush().noise(x, y),
            this.noises.temperature().noise(x, y),
            this.noises.hygrometry().noise(x, y),
            this.noises.ore().noise(x, y)
        );
    }

    public double selector(double x, double y) {
        return (this.selector.noise(x, y) + 1.0) / 2.0;
    }

    private record WGSimplex(double offsetX, double offsetY, double depth, double scale, double offsetNoise) {
        public double noise(double x, double y) {
            return SimplexNoise.noise((x + this.offsetX) / this.scale, (y + this.offsetY) / this.scale, this.depth);
        }
    }

    private record WGSimplexNoise(
        WorldGenSimplexGenerator.WGSimplex tree,
        WorldGenSimplexGenerator.WGSimplex plant,
        WorldGenSimplexGenerator.WGSimplex bush,
        WorldGenSimplexGenerator.WGSimplex temperature,
        WorldGenSimplexGenerator.WGSimplex hygrometry,
        WorldGenSimplexGenerator.WGSimplex ore
    ) {
    }
}
