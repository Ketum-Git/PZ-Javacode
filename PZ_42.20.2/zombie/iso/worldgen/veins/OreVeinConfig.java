// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.veins;

import java.util.List;
import zombie.iso.worldgen.biomes.TileGroup;

public class OreVeinConfig {
    private final List<TileGroup> tiles;
    private final int armsAmountMin;
    private final int armsAmountMax;
    private final int armsDistMin;
    private final int armsDistMax;
    private final int armsDeltaAngle;
    private final float armsProb;
    private final float centerProb;
    private final float probability;
    private final int centerRadius;

    public OreVeinConfig(
        List<TileGroup> tiles,
        int centerRadius,
        float centerProb,
        int armsAmountMin,
        int armsAmountMax,
        int armsDistMin,
        int armsDistMax,
        int armsDeltaAngle,
        float armsProb,
        float probability
    ) {
        this.tiles = tiles;
        this.centerRadius = centerRadius;
        this.centerProb = centerProb;
        this.armsAmountMin = armsAmountMin;
        this.armsAmountMax = armsAmountMax;
        this.armsDistMin = armsDistMin;
        this.armsDistMax = armsDistMax;
        this.armsDeltaAngle = armsDeltaAngle;
        this.armsProb = armsProb;
        this.probability = probability;
    }

    public List<TileGroup> getTiles() {
        return this.tiles;
    }

    public float getCenterProb() {
        return this.centerProb;
    }

    public float getArmsProb() {
        return this.armsProb;
    }

    public int getCenterRadius() {
        return this.centerRadius;
    }

    public int getArmsAmountMin() {
        return this.armsAmountMin;
    }

    public int getArmsAmountMax() {
        return this.armsAmountMax;
    }

    public int getArmsDistMin() {
        return this.armsDistMin;
    }

    public int getArmsDistMax() {
        return this.armsDistMax;
    }

    public int getArmsDeltaAngle() {
        return this.armsDeltaAngle;
    }

    public float getProbability() {
        return this.probability;
    }
}
