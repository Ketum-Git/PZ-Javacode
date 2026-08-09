// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.roads;

import java.util.List;
import zombie.iso.worldgen.biomes.TileGroup;

public record RoadConfig(List<TileGroup> tiles, double probaRoads, double probability, double filter) {
}
