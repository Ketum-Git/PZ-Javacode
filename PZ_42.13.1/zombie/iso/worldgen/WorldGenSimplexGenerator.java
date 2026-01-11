// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen;

import java.util.Random;
import zombie.iso.weather.SimplexNoise;

public class WorldGenSimplexGenerator {
    public static final int NUM_NOISES = 6;
    private final WorldGenSimplexGenerator.WGSimplex[] noises = new WorldGenSimplexGenerator.WGSimplex[6];
    private final WorldGenSimplexGenerator.WGSimplex selector;

    public WorldGenSimplexGenerator(long seed) {
        Random rnd = new Random(seed + 100L);
        this.noises[0] = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 128.0, 0.0);
        rnd = new Random(seed + 200L);
        this.noises[1] = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 128.0, 0.0);
        rnd = new Random(seed + 300L);
        this.noises[2] = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 128.0, 0.0);
        rnd = new Random(seed + 400L);
        this.noises[3] = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 128.0, 0.0);
        rnd = new Random(seed + 500L);
        this.noises[4] = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 128.0, 0.0);
        rnd = new Random(seed + 600L);
        this.noises[5] = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 128.0, 0.0);
        rnd = new Random(seed + 700L);
        this.selector = new WorldGenSimplexGenerator.WGSimplex(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble(), 16.0, 0.0);
    }

    public double[] noise(double x, double y) {
        double[] ret = new double[6];

        for (int i = 0; i < this.noises.length; i++) {
            ret[i] = this.noises[i].noise(x, y);
        }

        return ret;
    }

    public double selector(double x, double y) {
        return (this.selector.noise(x, y) + 1.0) / 2.0;
    }

    private static class WGSimplex {
        private final double offsetX;
        private final double offsetY;
        private final double depth;
        private final double scale;
        private final double offsetNoise;

        public WGSimplex(double offsetX, double offsetY, double depth, double scale, double offsetNoise) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.depth = depth;
            this.scale = scale;
            this.offsetNoise = offsetNoise;
        }

        public double noise(double x, double y) {
            return SimplexNoise.noise((x + this.offsetX) / this.scale, (y + this.offsetY) / this.scale, this.depth);
        }
    }
}
