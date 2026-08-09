// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.biomes;

import java.util.List;
import zombie.iso.worldgen.utils.probabilities.Probability;

public record Feature(List<TileGroup> tileGroups, int minSize, int maxSize, Probability probability) {
}
