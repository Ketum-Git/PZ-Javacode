// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.biomes;

import java.util.List;

public record Grass(float fernChance, float noGrassDiv, List<Double> noGrassStages, List<Double> grassStages) {
    public static final Grass DEFAULT = new Grass(0.3F, 12.0F, List.of(0.4), List.of(0.33, 0.5));
}
