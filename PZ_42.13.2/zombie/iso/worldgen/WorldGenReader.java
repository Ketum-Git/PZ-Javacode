// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.iso.worldgen.biomes.Biome;
import zombie.iso.worldgen.biomes.BiomeType;
import zombie.iso.worldgen.biomes.Feature;
import zombie.iso.worldgen.biomes.FeatureType;
import zombie.iso.worldgen.biomes.Grass;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.maps.BiomeMapEntry;
import zombie.iso.worldgen.roads.RoadConfig;
import zombie.iso.worldgen.utils.probabilities.ProbaDouble;
import zombie.iso.worldgen.utils.probabilities.ProbaString;
import zombie.iso.worldgen.utils.probabilities.Probability;
import zombie.iso.worldgen.veins.OreVeinConfig;
import zombie.iso.worldgen.zones.AnimalsPathConfig;

public class WorldGenReader {
    public Map<String, Biome> loadBiomes(KahluaTable worldgenTable, String tableKey) {
        KahluaTable mainTable = (KahluaTable)worldgenTable.rawget(tableKey);
        Map<String, Biome> biomes = new HashMap<>();
        KahluaTableIterator iterMainTable = mainTable.iterator();

        while (iterMainTable.advance()) {
            Object biomeName = iterMainTable.getKey();
            KahluaTable biomeTable = (KahluaTable)iterMainTable.getValue();
            biomes.put(biomeName.toString(), this.loadBiome(biomeName.toString(), biomeTable));
        }

        biomes.forEach(
            (k, v) -> {
                List<Biome> biomeList = new ArrayList<>(List.of((Biome)v));

                while (true) {
                    Biome biome = biomeList.get(0);
                    if (biome.parent() == null || biome.parent().isEmpty()) {
                        if (biomeList.size() == 1) {
                            return;
                        } else {
                            Map<FeatureType, List<Feature>> features = new HashMap<>();
                            Map<String, List<Feature>> replacements = new HashMap<>();
                            EnumSet<BiomeType.Temperature> temperature = biomeList.get(0).temperature();
                            EnumSet<BiomeType.Plant> plant = biomeList.get(0).plant();
                            EnumSet<BiomeType.Bush> bush = biomeList.get(0).bush();
                            EnumSet<BiomeType.Landscape> landscape = biomeList.get(0).landscape();
                            EnumSet<BiomeType.Hygrometry> hygrometry = biomeList.get(0).hygrometry();
                            EnumSet<BiomeType.OreLevel> oreLevel = biomeList.get(0).oreLevel();
                            Map<FeatureType, List<String>> placements = biomeList.get(0).placements();
                            List<String> protected_ = biomeList.get(0).protected_();
                            float zombies = biomeList.get(0).zombies();
                            Grass grass = biomeList.get(0).grass();

                            for (Biome biomex : biomeList) {
                                Map<FeatureType, List<Feature>> biomeFeatures = biomex.getFeatures();
                                if (biomeFeatures != null) {
                                    if (biomeFeatures.get(FeatureType.GROUND) != null) {
                                        features.put(FeatureType.GROUND, biomeFeatures.get(FeatureType.GROUND));
                                    }

                                    if (biomeFeatures.get(FeatureType.TREE) != null) {
                                        features.put(FeatureType.TREE, biomeFeatures.get(FeatureType.TREE));
                                    }

                                    if (biomeFeatures.get(FeatureType.PLANT) != null) {
                                        features.put(FeatureType.PLANT, biomeFeatures.get(FeatureType.PLANT));
                                    }

                                    if (biomeFeatures.get(FeatureType.BUSH) != null) {
                                        features.put(FeatureType.BUSH, biomeFeatures.get(FeatureType.BUSH));
                                    }

                                    if (biomeFeatures.get(FeatureType.ORE) != null) {
                                        features.put(FeatureType.ORE, biomeFeatures.get(FeatureType.ORE));
                                    }
                                }

                                Map<String, List<Feature>> biomeReplacements = biomex.getReplacements();
                                if (biomeReplacements != null) {
                                    replacements.putAll(biomeReplacements);
                                }

                                if (!Objects.equals(biomex.temperature(), EnumSet.allOf(BiomeType.Temperature.class))) {
                                    temperature = biomex.temperature();
                                }

                                if (!Objects.equals(biomex.plant(), EnumSet.allOf(BiomeType.Plant.class))) {
                                    plant = biomex.plant();
                                }

                                if (!Objects.equals(biomex.bush(), EnumSet.allOf(BiomeType.Bush.class))) {
                                    bush = biomex.bush();
                                }

                                if (!Objects.equals(biomex.landscape(), EnumSet.allOf(BiomeType.Landscape.class))) {
                                    landscape = biomex.landscape();
                                }

                                if (!Objects.equals(biomex.hygrometry(), EnumSet.allOf(BiomeType.Hygrometry.class))) {
                                    hygrometry = biomex.hygrometry();
                                }

                                if (!Objects.equals(biomex.oreLevel(), EnumSet.allOf(BiomeType.OreLevel.class))) {
                                    oreLevel = biomex.oreLevel();
                                }

                                if (biomex.placements() != null && !biomex.placements().isEmpty()) {
                                    placements = biomex.placements();
                                }

                                if (biomex.protected_() != null && !biomex.protected_().isEmpty()) {
                                    protected_ = biomex.protected_();
                                }

                                if (biomex.zombies() >= 0.0) {
                                    zombies = biomex.zombies();
                                }

                                grass = biomex.grass();
                            }

                            biomes.put(
                                k,
                                new Biome(
                                    k,
                                    v.parent(),
                                    v.generate(),
                                    features,
                                    replacements,
                                    landscape,
                                    plant,
                                    bush,
                                    temperature,
                                    hygrometry,
                                    oreLevel,
                                    zombies,
                                    placements,
                                    protected_,
                                    grass
                                )
                            );
                            return;
                        }
                    }

                    biomeList.add(0, biomes.get(biome.parent()));
                }
            }
        );
        return biomes;
    }

    public Biome loadBiome(String biomeName, KahluaTable biomeTable) {
        if (biomeTable == null) {
            return null;
        } else {
            String parent = (String)biomeTable.rawget("parent");
            KahluaTable featuresTable = (KahluaTable)biomeTable.rawget("features");
            Map<FeatureType, List<Feature>> features = this.loadBiomeFeatures(featuresTable);
            KahluaTable replacementsTable = (KahluaTable)biomeTable.rawget("replacements");
            Map<String, List<Feature>> replacements = this.loadReplacements(replacementsTable);
            KahluaTable paramsTable = (KahluaTable)biomeTable.rawget("params");
            EnumSet<BiomeType.Landscape> landscapes = this.loadBiomeType(BiomeType.Landscape.NONE, paramsTable.rawget("landscape"));
            EnumSet<BiomeType.Plant> plants = this.loadBiomeType(BiomeType.Plant.NONE, paramsTable.rawget("plant"));
            EnumSet<BiomeType.Bush> bushes = this.loadBiomeType(BiomeType.Bush.NONE, paramsTable.rawget("bush"));
            EnumSet<BiomeType.Temperature> temperatures = this.loadBiomeType(BiomeType.Temperature.NONE, paramsTable.rawget("temperature"));
            EnumSet<BiomeType.Hygrometry> hygrometries = this.loadBiomeType(BiomeType.Hygrometry.NONE, paramsTable.rawget("hygrometry"));
            EnumSet<BiomeType.OreLevel> oreLevels = this.loadBiomeType(BiomeType.OreLevel.NONE, paramsTable.rawget("ore_level"));
            Map<FeatureType, List<String>> placements = this.loadPlacements((KahluaTable)paramsTable.rawget("placements"));
            List<String> protected_ = this.loadList((KahluaTable)paramsTable.rawget("protected"));
            KahluaTable grassTable = (KahluaTable)paramsTable.rawget("grass");
            Grass grass;
            if (grassTable != null) {
                float grassFernChance = this.loadDouble(grassTable.rawget("fernChance"), 0.7).floatValue();
                float grassNoGrassDiv = this.loadDouble(grassTable.rawget("noGrassDiv"), 3.0).floatValue();
                List<Double> noGrassStages = this.loadList((KahluaTable)grassTable.rawget("noGrassStages")).isEmpty()
                    ? List.of(0.4)
                    : this.loadList((KahluaTable)grassTable.rawget("noGrassStages"));
                List<Double> grassStages = this.loadList((KahluaTable)grassTable.rawget("grassStages")).isEmpty()
                    ? List.of(0.33, 0.5)
                    : this.loadList((KahluaTable)grassTable.rawget("grassStages"));
                grass = new Grass(grassFernChance, grassNoGrassDiv, noGrassStages, grassStages);
            } else {
                grass = new Grass(0.7F, 3.0F, List.of(0.4), List.of(0.33, 0.5));
            }

            Double zombies = this.loadDouble(paramsTable.rawget("zombies"), -1.0);
            boolean generate = this.loadBoolean(paramsTable.rawget("generate"), true);
            return new Biome(
                biomeName,
                parent,
                generate,
                features,
                replacements,
                landscapes,
                plants,
                bushes,
                temperatures,
                hygrometries,
                oreLevels,
                zombies.floatValue(),
                placements,
                protected_,
                grass
            );
        }
    }

    public <T extends Enum<T>> Map<T, List<Double>> loadSelection(T biomeType, KahluaTable worldgenTable, String tableKey) {
        KahluaTable mainTable = (KahluaTable)worldgenTable.rawget(tableKey);
        KahluaTableIterator iterTable = ((KahluaTable)mainTable.rawget(BiomeType.keys.get(biomeType.getDeclaringClass()))).iterator();
        Map<T, List<Double>> selections = new HashMap<>();

        while (iterTable.advance()) {
            T name = Enum.valueOf(biomeType.getDeclaringClass(), (String)iterTable.getKey());
            List<Double> values = this.loadList((KahluaTable)iterTable.getValue());
            selections.put(name, values);
        }

        return selections;
    }

    private Map<FeatureType, List<Feature>> loadBiomeFeatures(KahluaTable mainTable) {
        if (mainTable == null) {
            return null;
        } else {
            Map<FeatureType, List<Feature>> types = new HashMap<>();
            KahluaTableIterator iterTypesTable = mainTable.iterator();

            while (iterTypesTable.advance()) {
                String typeName = (String)iterTypesTable.getKey();
                KahluaTable typeTable = (KahluaTable)iterTypesTable.getValue();
                List<Feature> features = new ArrayList<>();
                KahluaTableIterator iterTypeTable = typeTable.iterator();

                while (iterTypeTable.advance()) {
                    Object featuresObj = ((KahluaTable)iterTypeTable.getValue()).rawget("f");
                    Object probaObj = ((KahluaTable)iterTypeTable.getValue()).rawget("p");
                    if (featuresObj == null || probaObj == null) {
                        throw new RuntimeException(String.format("Features not found or probability absents | %s | %s", featuresObj, probaObj));
                    }

                    List<TileGroup> tileGroups = this.loadFeatures((KahluaTable)((KahluaTable)featuresObj).rawget("main"));
                    Probability probability;
                    if (probaObj instanceof Double d) {
                        probability = new ProbaDouble(d);
                    } else {
                        if (!(probaObj instanceof String s)) {
                            throw new RuntimeException("Unsupported probability type");
                        }

                        probability = new ProbaString(s);
                    }

                    int minSize = Integer.MAX_VALUE;
                    int maxSize = 0;

                    for (TileGroup tileGroup : tileGroups) {
                        minSize = Math.min(Math.min(minSize, tileGroup.sx()), tileGroup.sy());
                        maxSize = Math.max(Math.max(maxSize, tileGroup.sx()), tileGroup.sy());
                    }

                    features.add(new Feature(tileGroups, minSize, maxSize, probability));
                }

                types.put(FeatureType.valueOf(typeName), features);
            }

            return types;
        }
    }

    private List<TileGroup> loadFeatures(KahluaTable mainTable) {
        List<TileGroup> features = new ArrayList<>();
        KahluaTableIterator iterMainTable = mainTable.iterator();

        while (iterMainTable.advance()) {
            Object value = iterMainTable.getValue();
            if (value instanceof String s) {
                features.add(new TileGroup(1, 1, List.of(s)));
            } else {
                if (!(value instanceof KahluaTable kahluaTable)) {
                    throw new RuntimeException("Only strings and tables in there!");
                }

                int sx = 0;
                int sy = 0;
                List<String> tiles = new ArrayList<>();
                KahluaTableIterator iterOuter = kahluaTable.iterator();

                while (iterOuter.advance()) {
                    sy++;
                    List<String> t = this.loadList((KahluaTable)iterOuter.getValue());
                    sx = t.size();
                    tiles.addAll(t);
                }

                features.add(new TileGroup(sx, sy, tiles));
            }
        }

        return features;
    }

    public Map<String, List<Feature>> loadReplacements(KahluaTable replacementsTable) {
        Map<String, List<Feature>> replacements = new HashMap<>();
        if (replacementsTable != null) {
            KahluaTableIterator iterReplacementsTable = replacementsTable.iterator();

            while (iterReplacementsTable.advance()) {
                String key = (String)iterReplacementsTable.getKey();
                KahluaTable value = (KahluaTable)iterReplacementsTable.getValue();
                List<Feature> features = new ArrayList<>();
                KahluaTableIterator iterFeaturesTable = value.iterator();

                while (iterFeaturesTable.advance()) {
                    Object featuresObj = ((KahluaTable)iterFeaturesTable.getValue()).rawget("f");
                    Object probaObj = ((KahluaTable)iterFeaturesTable.getValue()).rawget("p");
                    if (featuresObj == null || probaObj == null) {
                        throw new RuntimeException(String.format("Features not found or probability absents | %s | %s", featuresObj, probaObj));
                    }

                    List<TileGroup> tileGroups = this.loadFeatures((KahluaTable)((KahluaTable)featuresObj).rawget("main"));
                    Probability probability;
                    if (probaObj instanceof Double d) {
                        probability = new ProbaDouble(d);
                    } else {
                        if (!(probaObj instanceof String s)) {
                            throw new RuntimeException("Unsupported probability type");
                        }

                        probability = new ProbaString(s);
                    }

                    int minSize = Integer.MAX_VALUE;
                    int maxSize = 0;

                    for (TileGroup tileGroup : tileGroups) {
                        minSize = Math.min(Math.min(minSize, tileGroup.sx()), tileGroup.sy());
                        maxSize = Math.max(Math.max(maxSize, tileGroup.sx()), tileGroup.sy());
                    }

                    features.add(new Feature(tileGroups, minSize, maxSize, probability));
                }

                replacements.put(key, features);
            }
        }

        return replacements;
    }

    private Map<FeatureType, List<String>> loadPlacements(KahluaTable placementsTable) {
        if (placementsTable == null) {
            return null;
        } else {
            Map<FeatureType, List<String>> placements = new HashMap<>();
            KahluaTableIterator iter = placementsTable.iterator();
            Map<String, List<String>> map = new HashMap<>();

            while (iter.advance()) {
                String typeName = (String)iter.getKey();
                List<String> placement = this.loadList((KahluaTable)iter.getValue());
                map.put(typeName, placement);
            }

            for (FeatureType type : FeatureType.values()) {
                List<String> p = new ArrayList<>();
                if (map.containsKey("GENERIC")) {
                    p.addAll(map.get("GENERIC"));
                }

                if (map.containsKey(type.toString())) {
                    p.addAll(map.get(type.toString()));
                }

                placements.put(type, p);
            }

            return placements;
        }
    }

    public Map<String, Double> loadPriorities(KahluaTable worldgenTable, String tableKey) {
        Map<String, Double> priorities = new HashMap<>();
        KahluaTable priorityTable = (KahluaTable)worldgenTable.rawget(tableKey);
        KahluaTableIterator iterPriorityTable = priorityTable.iterator();

        while (iterPriorityTable.advance()) {
            priorities.put((String)iterPriorityTable.getValue(), (Double)iterPriorityTable.getKey());
        }

        return priorities;
    }

    private <T extends Enum<T>> EnumSet<T> loadBiomeType(T biomeType, Object object) {
        if (object == null) {
            return null;
        } else {
            KahluaTable landscapeTable = (KahluaTable)object;
            KahluaTableIterator iterLandscapeTable = landscapeTable.iterator();
            List<T> tmpLandscapes = new ArrayList<>();

            while (iterLandscapeTable.advance()) {
                tmpLandscapes.add(Enum.valueOf(biomeType.getDeclaringClass(), (String)iterLandscapeTable.getValue()));
            }

            return tmpLandscapes.isEmpty() ? EnumSet.of(biomeType) : EnumSet.copyOf(tmpLandscapes);
        }
    }

    public List<StaticModule> loadStaticModules(KahluaTable worldGenTable, String tableKey) {
        KahluaTable mainTable = (KahluaTable)worldGenTable.rawget(tableKey);
        if (mainTable == null) {
            return new ArrayList<>();
        } else {
            List<StaticModule> staticModules = new ArrayList<>();
            KahluaTableIterator mainTableIter = mainTable.iterator();

            while (mainTableIter.advance()) {
                KahluaTable staticModuleTable = (KahluaTable)mainTableIter.getValue();
                KahluaTable positionTable = (KahluaTable)staticModuleTable.rawget("position");
                Double xmin = this.loadDouble(positionTable.rawget("xmin"), -Double.MAX_VALUE);
                Double xmax = this.loadDouble(positionTable.rawget("xmax"), Double.MAX_VALUE);
                Double ymin = this.loadDouble(positionTable.rawget("ymin"), -Double.MAX_VALUE);
                Double ymax = this.loadDouble(positionTable.rawget("ymax"), Double.MAX_VALUE);
                Biome biome = this.loadBiome("", (KahluaTable)staticModuleTable.rawget("biome"));
                PrefabStructure structure = this.loadPrefab((KahluaTable)staticModuleTable.rawget("prefab"));
                if (biome == null && structure == null) {
                    throw new RuntimeException("Need at least one of 'biome' or 'prefab' in WorldGenOverride.lua/worlgen.static_modules");
                }

                staticModules.add(new StaticModule(biome, structure, xmin.intValue(), xmax.intValue(), ymin.intValue(), ymax.intValue()));
            }

            return staticModules;
        }
    }

    private PrefabStructure loadPrefab(KahluaTable prefabTable) {
        if (prefabTable == null) {
            return null;
        } else {
            KahluaTable dimensionTable = (KahluaTable)prefabTable.rawget("dimensions");
            KahluaTableIterator dimensionTableIter = dimensionTable.iterator();
            int[] dimensions = new int[2];

            while (dimensionTableIter.advance()) {
                int i = ((Double)dimensionTableIter.getKey()).intValue() - 1;
                dimensions[i] = ((Double)dimensionTableIter.getValue()).intValue();
            }

            List<String> tiles = this.loadList((KahluaTable)prefabTable.rawget("tiles"));
            KahluaTable schematicTable = (KahluaTable)prefabTable.rawget("schematic");
            KahluaTableIterator schematicTableIter = schematicTable.iterator();
            Map<String, int[][]> schematic = new HashMap<>();

            while (schematicTableIter.advance()) {
                String key = (String)schematicTableIter.getKey();
                KahluaTable valueTable = (KahluaTable)schematicTableIter.getValue();
                KahluaTableIterator valueTableIter = valueTable.iterator();
                int[][] subSchem = new int[dimensions[1]][dimensions[0]];

                while (valueTableIter.advance()) {
                    int i = ((Double)valueTableIter.getKey()).intValue() - 1;
                    subSchem[i] = Stream.of(((String)valueTableIter.getValue()).split(",")).mapToInt(Integer::parseInt).toArray();
                }

                schematic.put(key, subSchem);
            }

            Double zombies = this.loadDouble(prefabTable.rawget("zombies"), 0.0);
            return new PrefabStructure(dimensions, tiles, schematic, zombies.floatValue());
        }
    }

    public Map<String, OreVeinConfig> loadVeinsConfig(KahluaTable worldGenTable, String tableKey) {
        KahluaTable mainTable = (KahluaTable)worldGenTable.rawget(tableKey);
        if (mainTable == null) {
            return new HashMap<>();
        } else {
            Map<String, OreVeinConfig> configs = new HashMap<>();
            KahluaTableIterator iterMainTable = mainTable.iterator();

            while (iterMainTable.advance()) {
                String key = (String)iterMainTable.getKey();
                KahluaTable subTable = (KahluaTable)iterMainTable.getValue();
                KahluaTable featureTable = (KahluaTable)subTable.rawget("feature");
                List<TileGroup> tiles = this.loadFeatures((KahluaTable)((KahluaTable)featureTable.rawget("f")).rawget("main"));
                KahluaTable armsTable = (KahluaTable)subTable.rawget("arms");
                int armsAmountMin = this.loadInteger(armsTable.rawget("amount_min"), 3);
                int armsAmountMax = this.loadInteger(armsTable.rawget("amount_max"), 6);
                int armsDistMin = this.loadInteger(armsTable.rawget("distance_min"), 100);
                int armsDistMax = this.loadInteger(armsTable.rawget("distance_max"), 400);
                int armsDeltaAngle = this.loadInteger(armsTable.rawget("delta_angle"), 5);
                float armsProb = this.loadDouble(armsTable.rawget("p"), 0.25).floatValue();
                KahluaTable centerTable = (KahluaTable)subTable.rawget("center");
                int centerRadius = this.loadInteger(centerTable.rawget("radius"), 5);
                float centerProb = this.loadDouble(centerTable.rawget("p"), 0.5).floatValue();
                float probability = this.loadDouble(subTable.rawget("p"), 0.01).floatValue();
                configs.put(
                    key,
                    new OreVeinConfig(
                        tiles, centerRadius, centerProb, armsAmountMin, armsAmountMax, armsDistMin, armsDistMax, armsDeltaAngle, armsProb, probability
                    )
                );
            }

            return configs;
        }
    }

    public Map<String, RoadConfig> loadRoadConfig(KahluaTable worldGenTable, String tableKey) {
        KahluaTable mainTable = (KahluaTable)worldGenTable.rawget(tableKey);
        if (mainTable == null) {
            return new HashMap<>();
        } else {
            Map<String, RoadConfig> configs = new HashMap<>();
            KahluaTableIterator iterMainTable = mainTable.iterator();

            while (iterMainTable.advance()) {
                String key = (String)iterMainTable.getKey();
                KahluaTable subTable = (KahluaTable)iterMainTable.getValue();
                KahluaTable featureTable = (KahluaTable)subTable.rawget("feature");
                List<TileGroup> tiles = this.loadFeatures((KahluaTable)((KahluaTable)featureTable.rawget("f")).rawget("main"));
                double probaRoads = this.loadDouble(featureTable.rawget("p"), 1.0);
                double filter = this.loadDouble(subTable.rawget("filter_edge"), 5.0E8);
                double probability = this.loadDouble(subTable.rawget("p"), 5.0E-4);
                configs.put(key, new RoadConfig(tiles, probaRoads, probability, filter));
            }

            return configs;
        }
    }

    public List<AnimalsPathConfig> loadAnimalsPath(KahluaTable animalsPathTable) {
        List<AnimalsPathConfig> animalsPathConfig = new ArrayList<>();
        KahluaTableIterator iterAnimalsPath = animalsPathTable.iterator();

        while (iterAnimalsPath.advance()) {
            Object animalName = iterAnimalsPath.getKey();
            KahluaTable subTable = (KahluaTable)iterAnimalsPath.getValue();
            Object animalObj = subTable.rawget("animal");
            Object countObj = subTable.rawget("count");
            Object chanceObj = subTable.rawget("chance");
            Object pointsObj = subTable.rawget("points");
            Object radiusObj = subTable.rawget("radius");
            Object extensionObj = subTable.rawget("extension");
            Object extensionChanceObj = subTable.rawget("extension_chance");
            String animal = this.loadString(animalObj, null);
            int count = this.loadInteger(countObj, 1);
            Double chance = this.loadDouble(chanceObj, 0.0);
            List<Double> pointsDouble = pointsObj instanceof KahluaTable table ? this.loadList(table) : List.of(this.loadDouble(pointsObj, -1.0));
            List<Double> radiusDouble = radiusObj instanceof KahluaTable tablex ? this.loadList(tablex) : List.of(this.loadDouble(radiusObj, -1.0));
            List<Double> extensionDouble = extensionObj instanceof KahluaTable tablexx ? this.loadList(tablexx) : List.of(this.loadDouble(extensionObj, -1.0));
            Double extensionChance = this.loadDouble(extensionChanceObj, 1.0);
            int[] points = pointsDouble.stream().mapToInt(Double::intValue).toArray();
            int[] radius = radiusDouble.stream().mapToInt(Double::intValue).toArray();
            int[] extension = extensionDouble.stream().mapToInt(Double::intValue).toArray();
            if (animal != null) {
                animalsPathConfig.add(new AnimalsPathConfig(animal, count, chance.floatValue(), points, radius, extension, extensionChance.floatValue()));
            }
        }

        return animalsPathConfig;
    }

    public Map<Integer, BiomeMapEntry> loadBiomeMapConfig(KahluaTable biomeMapTable) {
        Map<Integer, BiomeMapEntry> biomeMap = new HashMap<>();
        KahluaTableIterator iterBiomeMapConfig = biomeMapTable.iterator();

        while (iterBiomeMapConfig.advance()) {
            KahluaTable subTable = (KahluaTable)iterBiomeMapConfig.getValue();
            int pixel = this.loadInteger(subTable.rawget("pixel"), 0);
            String biome = this.loadString(subTable.rawget("biome"), null);
            String ore = this.loadString(subTable.rawget("ore"), null);
            String zone = this.loadString(subTable.rawget("zone"), null);
            biomeMap.put(pixel, new BiomeMapEntry(pixel, biome, ore, zone));
        }

        return biomeMap;
    }

    private <T> T[] loadArray(KahluaTable mainTable) {
        return (T[])this.loadList(mainTable).toArray();
    }

    private <T> List<T> loadList(KahluaTable mainTable) {
        List<T> tiles = new ArrayList<>();
        if (mainTable == null) {
            return tiles;
        } else {
            KahluaTableIterator iterFeaturesTable = mainTable.iterator();

            while (iterFeaturesTable.advance()) {
                tiles.add((T)iterFeaturesTable.getValue());
            }

            return tiles;
        }
    }

    private int loadInteger(Object object, int default_) {
        return object == null ? default_ : ((Double)object).intValue();
    }

    private Double loadDouble(Object object, Double default_) {
        return object == null ? default_ : (Double)object;
    }

    private String loadString(Object object, String default_) {
        return object == null ? default_ : (String)object;
    }

    private boolean loadBoolean(Object object, boolean default_) {
        return object == null ? default_ : (Boolean)object;
    }
}
