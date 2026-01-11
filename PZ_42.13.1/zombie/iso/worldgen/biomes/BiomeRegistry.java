// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.biomes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BiomeRegistry {
    public static BiomeRegistry instance = new BiomeRegistry();
    private final Map<BiomeRegistry.BiomeGetter, List<IBiome>> biomeCache = new HashMap<>();
    private final Map<BiomeRegistry.BiomeGetterFiltered, List<IBiome>> biomeCacheFiltered = new HashMap<>();

    private BiomeRegistry() {
    }

    public void reset() {
        this.biomeCache.clear();
        this.biomeCacheFiltered.clear();
    }

    public IBiome get(
        Map<String, Biome> biomesIn,
        double[] noises,
        double selector,
        Map<BiomeType.Landscape, List<Double>> landscapeProb,
        Map<BiomeType.Plant, List<Double>> plantProb,
        Map<BiomeType.Bush, List<Double>> bushProb,
        Map<BiomeType.Temperature, List<Double>> temperatureProb,
        Map<BiomeType.Hygrometry, List<Double>> hygrometryProb,
        Map<BiomeType.OreLevel, List<Double>> oreLevelProb
    ) {
        BiomeType.Landscape landscape = this.getLandscape(noises, landscapeProb);
        BiomeType.Plant plant = this.getPlant(noises, plantProb);
        BiomeType.Bush bush = this.getBush(noises, bushProb);
        BiomeType.Temperature temperature = this.getTemperature(noises, temperatureProb);
        BiomeType.Hygrometry hygrometry = this.getHygrometry(noises, hygrometryProb);
        BiomeType.OreLevel oreLevel = this.getOre(noises, oreLevelProb);
        BiomeRegistry.BiomeGetter biomeGetter = new BiomeRegistry.BiomeGetter(landscape, plant, bush, temperature, hygrometry, oreLevel);
        List<IBiome> biomes = this.biomeCache
            .computeIfAbsent(
                biomeGetter,
                k -> biomesIn.values()
                    .stream()
                    .filter(b -> b.landscape().contains(landscape))
                    .filter(b -> b.plant().contains(plant))
                    .filter(b -> b.bush().contains(bush))
                    .filter(b -> b.temperature().contains(temperature))
                    .filter(b -> b.hygrometry().contains(hygrometry))
                    .filter(b -> b.oreLevel().contains(oreLevel))
                    .collect(Collectors.toList())
            );
        return (IBiome)(biomes.isEmpty() ? Biome.DEFAULT_BIOME : biomes.get((int)(selector * biomes.size())));
    }

    public IBiome get(
        Map<String, Biome> biomesIn,
        String filter,
        double[] noises,
        double selector,
        Map<BiomeType.Bush, List<Double>> bushProb,
        Map<BiomeType.OreLevel, List<Double>> oreLevelProb
    ) {
        if (filter == null) {
            return null;
        } else {
            BiomeType.Bush bush = this.getBush(noises, bushProb);
            BiomeType.OreLevel oreLevel = this.getOre(noises, oreLevelProb);
            BiomeRegistry.BiomeGetterFiltered biomeGetter = new BiomeRegistry.BiomeGetterFiltered(filter, bush, oreLevel);
            List<IBiome> biomes = this.biomeCacheFiltered
                .computeIfAbsent(
                    biomeGetter,
                    k -> biomesIn.values()
                        .stream()
                        .filter(b -> b.name().startsWith(filter))
                        .filter(b -> b.bush().contains(bush))
                        .filter(b -> b.oreLevel().contains(oreLevel))
                        .collect(Collectors.toList())
                );
            return (IBiome)(biomes.isEmpty() ? Biome.DEFAULT_BIOME : biomes.get((int)(selector * biomes.size())));
        }
    }

    private BiomeType.Landscape getLandscape(double[] noises, Map<BiomeType.Landscape, List<Double>> landscapeProb) {
        BiomeType.Landscape landscape;
        if (noises[0] >= landscapeProb.get(BiomeType.Landscape.FOREST).get(0) && noises[0] < landscapeProb.get(BiomeType.Landscape.FOREST).get(1)) {
            landscape = BiomeType.Landscape.FOREST;
        } else if (noises[0] >= landscapeProb.get(BiomeType.Landscape.LIGHT_FOREST).get(0)
            && noises[0] < landscapeProb.get(BiomeType.Landscape.LIGHT_FOREST).get(1)) {
            landscape = BiomeType.Landscape.LIGHT_FOREST;
        } else if (noises[0] >= landscapeProb.get(BiomeType.Landscape.PLAIN).get(0) && noises[0] < landscapeProb.get(BiomeType.Landscape.PLAIN).get(1)) {
            landscape = BiomeType.Landscape.PLAIN;
        } else {
            landscape = BiomeType.Landscape.NONE;
        }

        return landscape;
    }

    private BiomeType.Plant getPlant(double[] noises, Map<BiomeType.Plant, List<Double>> plantProb) {
        BiomeType.Plant plant;
        if (noises[1] >= plantProb.get(BiomeType.Plant.FLOWER).get(0) && noises[1] < plantProb.get(BiomeType.Plant.FLOWER).get(1)) {
            plant = BiomeType.Plant.FLOWER;
        } else if (noises[1] >= plantProb.get(BiomeType.Plant.GRASS).get(0) && noises[1] < plantProb.get(BiomeType.Plant.GRASS).get(1)) {
            plant = BiomeType.Plant.GRASS;
        } else {
            plant = BiomeType.Plant.NONE;
        }

        return plant;
    }

    private BiomeType.Bush getBush(double[] noises, Map<BiomeType.Bush, List<Double>> bushProb) {
        BiomeType.Bush bush;
        if (noises[1] >= bushProb.get(BiomeType.Bush.DRY).get(0) && noises[1] < bushProb.get(BiomeType.Bush.DRY).get(1)) {
            bush = BiomeType.Bush.DRY;
        } else if (noises[1] >= bushProb.get(BiomeType.Bush.REGULAR).get(0) && noises[1] < bushProb.get(BiomeType.Bush.REGULAR).get(1)) {
            bush = BiomeType.Bush.REGULAR;
        } else if (noises[1] >= bushProb.get(BiomeType.Bush.FAT).get(0) && noises[1] < bushProb.get(BiomeType.Bush.FAT).get(1)) {
            bush = BiomeType.Bush.FAT;
        } else {
            bush = BiomeType.Bush.NONE;
        }

        return bush;
    }

    private BiomeType.Temperature getTemperature(double[] noises, Map<BiomeType.Temperature, List<Double>> temperatureProb) {
        BiomeType.Temperature temperature;
        if (noises[2] >= temperatureProb.get(BiomeType.Temperature.HOT).get(0) && noises[2] < temperatureProb.get(BiomeType.Temperature.HOT).get(1)) {
            temperature = BiomeType.Temperature.HOT;
        } else if (noises[2] >= temperatureProb.get(BiomeType.Temperature.MEDIUM).get(0)
            && noises[2] < temperatureProb.get(BiomeType.Temperature.MEDIUM).get(1)) {
            temperature = BiomeType.Temperature.MEDIUM;
        } else if (noises[2] >= temperatureProb.get(BiomeType.Temperature.COLD).get(0) && noises[2] < temperatureProb.get(BiomeType.Temperature.COLD).get(1)) {
            temperature = BiomeType.Temperature.COLD;
        } else {
            temperature = BiomeType.Temperature.NONE;
        }

        return temperature;
    }

    private BiomeType.Hygrometry getHygrometry(double[] noises, Map<BiomeType.Hygrometry, List<Double>> hygrometryProb) {
        BiomeType.Hygrometry hygrometry;
        if (noises[3] >= hygrometryProb.get(BiomeType.Hygrometry.FLOODING).get(0) && noises[3] < hygrometryProb.get(BiomeType.Hygrometry.FLOODING).get(1)) {
            hygrometry = BiomeType.Hygrometry.FLOODING;
        } else if (noises[3] >= hygrometryProb.get(BiomeType.Hygrometry.RAIN).get(0) && noises[3] < hygrometryProb.get(BiomeType.Hygrometry.RAIN).get(1)) {
            hygrometry = BiomeType.Hygrometry.RAIN;
        } else if (noises[3] >= hygrometryProb.get(BiomeType.Hygrometry.DRY).get(0) && noises[3] < hygrometryProb.get(BiomeType.Hygrometry.DRY).get(1)) {
            hygrometry = BiomeType.Hygrometry.DRY;
        } else {
            hygrometry = BiomeType.Hygrometry.NONE;
        }

        return hygrometry;
    }

    private BiomeType.OreLevel getOre(double[] noises, Map<BiomeType.OreLevel, List<Double>> oreLevelProb) {
        BiomeType.OreLevel oreLevel;
        if (noises[3] >= oreLevelProb.get(BiomeType.OreLevel.VERY_HIGH).get(0) && noises[3] < oreLevelProb.get(BiomeType.OreLevel.VERY_HIGH).get(1)) {
            oreLevel = BiomeType.OreLevel.VERY_HIGH;
        } else if (noises[3] >= oreLevelProb.get(BiomeType.OreLevel.HIGH).get(0) && noises[3] < oreLevelProb.get(BiomeType.OreLevel.HIGH).get(1)) {
            oreLevel = BiomeType.OreLevel.HIGH;
        } else if (noises[3] >= oreLevelProb.get(BiomeType.OreLevel.MEDIUM).get(0) && noises[3] < oreLevelProb.get(BiomeType.OreLevel.MEDIUM).get(1)) {
            oreLevel = BiomeType.OreLevel.MEDIUM;
        } else if (noises[3] >= oreLevelProb.get(BiomeType.OreLevel.LOW).get(0) && noises[3] < oreLevelProb.get(BiomeType.OreLevel.LOW).get(1)) {
            oreLevel = BiomeType.OreLevel.LOW;
        } else if (noises[3] >= oreLevelProb.get(BiomeType.OreLevel.VERY_LOW).get(0) && noises[3] < oreLevelProb.get(BiomeType.OreLevel.VERY_LOW).get(1)) {
            oreLevel = BiomeType.OreLevel.VERY_LOW;
        } else {
            oreLevel = BiomeType.OreLevel.NONE;
        }

        return oreLevel;
    }

    private record BiomeGetter(
        BiomeType.Landscape landscape,
        BiomeType.Plant plant,
        BiomeType.Bush bush,
        BiomeType.Temperature temperature,
        BiomeType.Hygrometry hygrometry,
        BiomeType.OreLevel oreLevel
    ) {
    }

    private record BiomeGetterFiltered(String filter, BiomeType.Bush bush, BiomeType.OreLevel oreLevel) {
    }
}
