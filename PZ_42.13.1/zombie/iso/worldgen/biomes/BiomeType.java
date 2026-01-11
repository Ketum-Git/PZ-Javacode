// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.biomes;

import java.util.Map;

public class BiomeType {
    public static Map<Class<?>, String> keys = Map.of(
        BiomeType.Hygrometry.class,
        "hygrometry",
        BiomeType.Landscape.class,
        "landscape",
        BiomeType.Plant.class,
        "plant",
        BiomeType.Bush.class,
        "bush",
        BiomeType.Temperature.class,
        "temperature",
        BiomeType.OreLevel.class,
        "ore_level"
    );

    public static enum Bush {
        DRY,
        REGULAR,
        FAT,
        NONE;
    }

    public static enum Hygrometry {
        FLOODING,
        RAIN,
        DRY,
        NONE;
    }

    public static enum Landscape {
        LIGHT_FOREST,
        FOREST,
        PLAIN,
        NONE;
    }

    public static enum OreLevel {
        VERY_LOW,
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH,
        NONE;
    }

    public static enum Plant {
        FLOWER,
        GRASS,
        NONE;
    }

    public static enum Temperature {
        COLD,
        MEDIUM,
        HOT,
        NONE;
    }
}
