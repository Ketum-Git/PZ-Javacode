// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.biomes;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public interface IBiome {
    String name();

    Map<FeatureType, List<Feature>> getFeatures();

    Map<String, List<Feature>> getReplacements();

    EnumSet<BiomeType.Landscape> landscape();

    EnumSet<BiomeType.Plant> plant();

    EnumSet<BiomeType.Bush> bush();

    EnumSet<BiomeType.Temperature> temperature();

    EnumSet<BiomeType.Hygrometry> hygrometry();

    EnumSet<BiomeType.OreLevel> oreLevel();

    Map<FeatureType, List<String>> placements();

    List<String> protected_();

    String parent();

    boolean generate();

    float zombies();

    Grass grass();

    IBiome landscape(BiomeType.Landscape arg0);

    IBiome plant(BiomeType.Plant arg0);

    IBiome bush(BiomeType.Bush arg0);

    IBiome temperature(BiomeType.Temperature arg0);

    IBiome hygrometry(BiomeType.Hygrometry arg0);

    IBiome oreLevel(BiomeType.OreLevel arg0);

    IBiome landscape(EnumSet<BiomeType.Landscape> arg0);

    IBiome plant(EnumSet<BiomeType.Plant> arg0);

    IBiome bush(EnumSet<BiomeType.Bush> arg0);

    IBiome temperature(EnumSet<BiomeType.Temperature> arg0);

    IBiome hygrometry(EnumSet<BiomeType.Hygrometry> arg0);

    IBiome oreLevel(EnumSet<BiomeType.OreLevel> arg0);

    IBiome placements(Map<FeatureType, List<String>> arg0);

    IBiome protected_(List<String> arg0);

    IBiome zombies(float arg0);

    IBiome grass(Grass arg0);
}
