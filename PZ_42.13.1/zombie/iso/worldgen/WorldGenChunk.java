// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import se.krka.kahlua.vm.KahluaTable;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.enums.ChunkGenerationStatus;
import zombie.iso.objects.IsoTree;
import zombie.iso.worldgen.biomes.Biome;
import zombie.iso.worldgen.biomes.BiomeRegistry;
import zombie.iso.worldgen.biomes.BiomeType;
import zombie.iso.worldgen.biomes.Feature;
import zombie.iso.worldgen.biomes.FeatureType;
import zombie.iso.worldgen.biomes.IBiome;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.blending.BlendDirection;
import zombie.iso.worldgen.maps.BiomeMap;
import zombie.iso.worldgen.maps.BiomeMapEntry;
import zombie.iso.worldgen.roads.Road;
import zombie.iso.worldgen.roads.RoadConfig;
import zombie.iso.worldgen.roads.RoadGenerator;
import zombie.iso.worldgen.utils.SquareCoord;
import zombie.iso.worldgen.veins.OreVein;
import zombie.iso.worldgen.veins.Veins;
import zombie.iso.worldgen.zones.WorldGenZone;
import zombie.randomizedWorld.RandomizedWorldBase;
import zombie.util.list.PZArrayList;

public class WorldGenChunk {
    private final WorldGenSimplexGenerator simplex;
    private final WorldGenTile wgTile;
    private final RandomizedWorldBase randomizedWorldBase;
    private final Map<String, Biome> biomes;
    private final Map<String, Biome> biomesMap;
    private final long seed;
    private final Map<BiomeType.Landscape, List<Double>> landscape;
    private final Map<BiomeType.Plant, List<Double>> plant;
    private final Map<BiomeType.Bush, List<Double>> bush;
    private final Map<BiomeType.Temperature, List<Double>> temperature;
    private final Map<BiomeType.Hygrometry, List<Double>> hygrometry;
    private final Map<BiomeType.OreLevel, List<Double>> oreLevel;
    private final List<StaticModule> staticModules;
    private final Veins veins;
    private final Map<String, Double> priorities;
    private final Map<String, RoadConfig> roadsConfig;
    private final List<RoadGenerator> roadGenerators;

    public WorldGenChunk(long seed) {
        this.seed = seed;
        this.simplex = new WorldGenSimplexGenerator(seed);
        this.wgTile = new WorldGenTile();
        this.randomizedWorldBase = new RandomizedWorldBase();
        this.runLuaOverride();
        KahluaTable worldgenTable = (KahluaTable)LuaManager.env.rawget("worldgen");
        String biomes = worldgenTable.rawget("biomes_override") == null ? "biomes" : "biomes_override";
        WorldGenReader wgReader = new WorldGenReader();
        this.biomes = wgReader.loadBiomes(worldgenTable, biomes)
            .entrySet()
            .stream()
            .filter(e -> e.getValue().generate())
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        this.biomesMap = wgReader.loadBiomes(worldgenTable, "biomes_map")
            .entrySet()
            .stream()
            .filter(e -> e.getValue().generate())
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        String selection = worldgenTable.rawget("selection_override") == null ? "selection" : "selection_override";
        this.landscape = wgReader.loadSelection(BiomeType.Landscape.NONE, worldgenTable, selection);
        this.plant = wgReader.loadSelection(BiomeType.Plant.NONE, worldgenTable, selection);
        this.bush = wgReader.loadSelection(BiomeType.Bush.NONE, worldgenTable, selection);
        this.temperature = wgReader.loadSelection(BiomeType.Temperature.NONE, worldgenTable, selection);
        this.hygrometry = wgReader.loadSelection(BiomeType.Hygrometry.NONE, worldgenTable, selection);
        this.oreLevel = wgReader.loadSelection(BiomeType.OreLevel.NONE, worldgenTable, selection);
        this.staticModules = wgReader.loadStaticModules(worldgenTable, "static_modules");
        this.veins = new Veins(wgReader.loadVeinsConfig(worldgenTable, "veins"));
        this.priorities = wgReader.loadPriorities(worldgenTable, "priorities");
        this.roadsConfig = wgReader.loadRoadConfig(worldgenTable, "roads");
        this.roadGenerators = new ArrayList<>();
        int offset = 1000;

        for (RoadConfig roadConfig : this.roadsConfig.values()) {
            this.roadGenerators.add(new RoadGenerator(this.seed, roadConfig, offset));
            offset += 1000;
        }
    }

    public List<RoadGenerator> getRoadGenerators() {
        return this.roadGenerators;
    }

    private void runLuaOverride() {
        String[] worldNames = IsoWorld.instance.getMap().split(";");

        for (String name : worldNames) {
            String filename = ZomboidFileSystem.instance.getString("media/maps/" + name + "/WorldGenOverride.lua");
            File fo = new File(filename);
            if (fo.exists()) {
                LuaManager.RunLua(filename);
            }
        }
    }

    public boolean genRandomChunk(IsoCell cell, IsoChunk ch, int chunkX, int chunkY) {
        int minTileX = chunkX * 8;
        int minTileY = chunkY * 8;
        int maxTileX = (chunkX + 1) * 8;
        int maxTileY = (chunkY + 1) * 8;
        ch.setMinMaxLevel(0, 0);
        ch.setBlendingDoneFull(true);
        ch.setBlendingDonePartial(false);
        ch.setAttachmentsDoneFull(false);

        for (int i = 0; i < 5; i++) {
            ch.setAttachmentsState(i, false);
        }

        ch.addModded(ChunkGenerationStatus.WORLDGEN);
        EnumMap<FeatureType, String[]> toBeDone = new EnumMap<>(FeatureType.class);

        for (FeatureType featureType : FeatureType.values()) {
            toBeDone.put(featureType, new String[64]);
        }

        Set<Road> roads = new HashSet<>();

        for (RoadGenerator generator : this.roadGenerators) {
            roads = generator.getRoads(chunkX, chunkY);
        }

        roads.stream().forEach(k -> DebugLog.log(String.format("Generating road %s", k)));

        try {
            for (int x = minTileX; x < maxTileX; x++) {
                for (int y = minTileY; y < maxTileY; y++) {
                    this.genRandomSquare(cell, ch, x, minTileX, y, minTileY, roads, toBeDone);
                }
            }

            return true;
        } catch (Exception var15) {
            DebugLog.log("Failed to load chunk, blocking out area");
            ExceptionLogger.logException(var15);

            for (int x = minTileX; x < maxTileX; x++) {
                for (int y = minTileY; y < maxTileY; y++) {
                    for (int z = 0; z < ch.maxLevel + 1; z++) {
                        ch.setSquare(x - minTileX, y - minTileY, z, null);
                    }
                }
            }

            return false;
        }
    }

    private void genRandomSquare(IsoCell cell, IsoChunk ch, int x, int minTileX, int y, int minTileY, Set<Road> roads, EnumMap<FeatureType, String[]> toBeDone) {
        int z = 0;
        int tileX = x - minTileX;
        int tileY = y - minTileY;
        int tileZ = 0;
        IsoGridSquare square = ch.getGridSquare(tileX, tileY, 0);
        if (square != null && !square.getObjects().isEmpty()) {
            ch.setBlendingDoneFull(false);
            ch.setBlendingDonePartial(true);
            ch.setModifDepth(BlendDirection.NORTH, Math.min(tileY, ch.getModifDepth(BlendDirection.NORTH)));
            ch.setModifDepth(BlendDirection.SOUTH, Math.max(tileY, ch.getModifDepth(BlendDirection.SOUTH)));
            ch.setModifDepth(BlendDirection.WEST, Math.min(tileX, ch.getModifDepth(BlendDirection.WEST)));
            ch.setModifDepth(BlendDirection.EAST, Math.max(tileX, ch.getModifDepth(BlendDirection.EAST)));
        } else {
            if (square == null) {
                square = IsoGridSquare.getNew(cell, null, x, y, 0);
                ch.setSquare(tileX, tileY, 0, square);
            }

            square.setRoomID(-1L);
            square.ResetIsoWorldRegion();
            List<StaticModule> tmpStaticModules = this.staticModules
                .stream()
                .filter(smx -> x >= smx.xmin() && x <= smx.xmax() && y >= smx.ymin() && y <= smx.ymax())
                .collect(Collectors.toList());
            Random rnd = WorldGenParams.INSTANCE.getRandom(ch.wx * 8 + x, ch.wy * 8 + y);
            if (tmpStaticModules.isEmpty()) {
                boolean placedRoad = false;
                Random rndRoad = WorldGenParams.INSTANCE.getRandom(ch.wx * 8 + x, ch.wy * 8 + y);

                for (Road road : roads) {
                    if (x >= Math.min(road.getA().x, road.getB().x)
                        && x <= Math.max(road.getA().x, road.getB().x)
                        && y >= Math.min(road.getA().y, road.getB().y)
                        && y <= Math.max(road.getA().y, road.getB().y)
                        && rndRoad.nextDouble() < road.getProbability()) {
                        this.placeRoad(road, cell, square, x, y, 0, tileX, tileY, 0, toBeDone, rndRoad);
                        placedRoad = true;
                    }
                }

                if (!placedRoad) {
                    IBiome biome = this.getBiome(x, y);
                    this.applyBiome(biome, cell, ch, square, x, y, 0, tileX, tileY, 0, toBeDone, false, rnd);
                    this.applyOreVeins(cell, square, x, y, 0, tileX, tileY, 0, toBeDone, rnd);
                }
            } else {
                StaticModule sm = tmpStaticModules.get(0);
                if (sm.biome() != null) {
                    IBiome biome = sm.biome();
                    this.applyBiome(biome, cell, ch, square, x, y, 0, tileX, tileY, 0, toBeDone, false, rnd);
                    this.applyOreVeins(cell, square, x, y, 0, tileX, tileY, 0, toBeDone, rnd);
                } else {
                    if (sm.prefab() == null) {
                        throw new RuntimeException("Need at least one of 'biome' or 'prefab' in WorldGenOverride.lua/worlgen.static_modules");
                    }

                    IBiome biome = this.getBiome(x, y);
                    PrefabStructure prefab = sm.prefab();
                    this.applyPrefab(prefab, biome, cell, ch, square, x, y, 0, tileX, tileY, 0, toBeDone, sm.xmin(), sm.xmax(), sm.ymin(), sm.ymax(), rnd);
                }
            }
        }
    }

    public void genMapChunk(IsoCell cell, IsoChunk ch, int chunkX, int chunkY) {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        IsoMetaChunk metaChunk = metaGrid.getChunkData(chunkX, chunkY);
        BiomeMap map = IsoWorld.instance.getBiomeMap();
        int minTileX = chunkX * 8;
        int minTileY = chunkY * 8;
        int maxTileX = (chunkX + 1) * 8;
        int maxTileY = (chunkY + 1) * 8;
        EnumMap<FeatureType, String[]> toBeDone = new EnumMap<>(FeatureType.class);

        for (FeatureType featureType : FeatureType.values()) {
            toBeDone.put(featureType, new String[64]);
        }

        int[] biomes = map.getZones(ch.wx, ch.wy, BiomeMap.Type.BIOME);
        if (biomes != null) {
            for (int x = minTileX; x < maxTileX; x++) {
                for (int y = minTileY; y < maxTileY; y++) {
                    this.genMapSquare(cell, ch, x, minTileX, y, minTileY, map, biomes, toBeDone);
                }
            }
        }
    }

    private void genMapSquare(
        IsoCell cell, IsoChunk ch, int x, int minTileX, int y, int minTileY, BiomeMap map, int[] biomes, EnumMap<FeatureType, String[]> toBeDone
    ) {
        int z = 0;
        int tileX = x - minTileX;
        int tileY = y - minTileY;
        int tileZ = 0;
        this.replaceSquare(cell, ch, x, minTileX, y, minTileY, map, biomes);
        WorldGenZone worldgenZone = this.getWorldGenZoneAt(x, y, 0);
        boolean rocks = true;
        if (worldgenZone != null) {
            rocks = worldgenZone.getRocks();
        }

        IsoGridSquare square = ch.getGridSquare(tileX, tileY, 0);
        if (square != null) {
            IsoObject floor = square.getFloor();
            if (floor != null) {
                BiomeMapEntry lookup = map.getEntry(biomes[tileY * 8 + tileX]);
                if (lookup != null) {
                    if (Objects.equals(lookup.biome(), "$random")) {
                        IsoWorld.instance.getAttachmentsHandler().resetAttachments(ch);
                        square.discard();
                        square = IsoGridSquare.getNew(cell, null, x, y, 0);
                        ch.setSquare(tileX, tileY, 0, square);
                        this.genRandomSquare(cell, ch, x, minTileX, y, minTileY, new HashSet<>(), toBeDone);
                    } else {
                        Random rnd = WorldGenParams.INSTANCE.getRandom(ch.wx * 8 + x, ch.wy * 8 + y);

                        for (FeatureType featureType : FeatureType.values()) {
                            String tile = toBeDone.get(featureType)[tileY * 8 + tileX];
                            if (tile != null) {
                                IsoTree tree = square.getTree();
                                IsoObject bush = square.getBush();
                                IsoObject grass = square.getGrass();
                                switch (tile) {
                                    case "NO_TREE":
                                        square.DeleteTileObject(tree);
                                        break;
                                    case "NO_BUSH":
                                        square.DeleteTileObject(bush);
                                        break;
                                    case "NO_GRASS":
                                        square.DeleteTileObject(grass);
                                        break;
                                    default:
                                        square.DeleteTileObject(tree);
                                        square.DeleteTileObject(bush);
                                        square.DeleteTileObject(grass);
                                        this.wgTile.applyTile(tile, square, cell, x, y, 0, rnd);
                                }
                            }
                        }

                        IsoTree tree = square.getTree();
                        IsoObject bush = square.getBush();
                        IsoObject grass = square.getGrass();
                        String floorName = floor.getSprite().getName();
                        if (tree != null || bush != null || grass != null) {
                            IBiome biome = this.getMapBiome(x, y, lookup.biome());
                            if (biome == null) {
                                return;
                            }

                            if (tree != null && !this.protected_(biome.protected_(), tree.sprite.name)) {
                                if (WorldGenUtils.INSTANCE.canPlace(biome.placements().get(FeatureType.TREE), floorName)) {
                                    if (this.applyBiome(biome, FeatureType.TREE, cell, ch, square, x, y, 0, tileX, tileY, 0, toBeDone, true, rnd)) {
                                        square.DeleteTileObject(tree);
                                    }
                                } else {
                                    square.DeleteTileObject(tree);
                                }
                            }

                            if (bush != null && !this.protected_(biome.protected_(), bush.sprite.name)) {
                                if (WorldGenUtils.INSTANCE.canPlace(biome.placements().get(FeatureType.BUSH), floorName)) {
                                    if (this.applyBiome(biome, FeatureType.BUSH, cell, ch, square, x, y, 0, tileX, tileY, 0, toBeDone, true, rnd)) {
                                        square.DeleteTileObject(bush);
                                    }
                                } else {
                                    square.DeleteTileObject(bush);
                                }
                            }

                            if (grass != null && !this.protected_(biome.protected_(), grass.sprite.name)) {
                                if (WorldGenUtils.INSTANCE.canPlace(biome.placements().get(FeatureType.PLANT), floorName)) {
                                    if (this.applyBiome(biome, FeatureType.PLANT, cell, ch, square, x, y, 0, tileX, tileY, 0, toBeDone, true, rnd)) {
                                        square.DeleteTileObject(grass);
                                    }
                                } else {
                                    square.DeleteTileObject(grass);
                                }
                            }
                        }

                        List<IsoObject> grasses = square.getGrassLike();
                        if (square.getObjects().size() - grasses.size() == 1) {
                            IBiome biomex = this.getMapBiome(x, y, lookup.ore());
                            if (biomex == null) {
                                return;
                            }

                            if (!WorldGenUtils.INSTANCE.canPlace(biomex.placements().get(FeatureType.ORE), floorName)) {
                                return;
                            }

                            if (rocks && this.applyBiome(biomex, FeatureType.ORE, cell, ch, square, x, y, 0, tileX, tileY, 0, toBeDone, true, rnd)) {
                                for (int i = 0; i < grasses.size(); i++) {
                                    square.DeleteTileObject(grasses.get(i));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void replaceTiles(IsoCell cell, IsoChunk ch, int chunkX, int chunkY) {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        IsoMetaChunk metaChunk = metaGrid.getChunkData(chunkX, chunkY);
        BiomeMap map = IsoWorld.instance.getBiomeMap();
        int minTileX = chunkX * 8;
        int minTileY = chunkY * 8;
        int maxTileX = (chunkX + 1) * 8;
        int maxTileY = (chunkY + 1) * 8;
        int[] biomes = map.getZones(ch.wx, ch.wy, BiomeMap.Type.BIOME);
        if (biomes != null) {
            for (int x = minTileX; x < maxTileX; x++) {
                for (int y = minTileY; y < maxTileY; y++) {
                    this.replaceSquare(cell, ch, x, minTileX, y, minTileY, map, biomes);
                }
            }
        }
    }

    private void replaceSquare(IsoCell cell, IsoChunk ch, int x, int minTileX, int y, int minTileY, BiomeMap map, int[] biomes) {
        int z = 0;
        int tileX = x - minTileX;
        int tileY = y - minTileY;
        int tileZ = 0;
        BiomeMapEntry lookup = map.getEntry(biomes[tileY * 8 + tileX]);
        if (lookup != null) {
            IBiome biome = this.getMapBiome(x, y, lookup.biome());
            if (biome != null) {
                Map<String, List<Feature>> replacements = biome.getReplacements();
                if (replacements != null && !replacements.isEmpty()) {
                    IsoGridSquare square = ch.getGridSquare(tileX, tileY, 0);
                    if (square != null) {
                        Random rnd = WorldGenParams.INSTANCE.getRandom(ch.wx * 8 + x, ch.wy * 8 + y);
                        PZArrayList<IsoObject> objects = square.getObjects();

                        for (int i = 0; i < objects.size(); i++) {
                            IsoObject object = objects.get(i);
                            if (replacements.containsKey(object.getSprite().getName())) {
                                List<Feature> features = replacements.get(object.getSprite().getName());
                                float prefilterProba = features.stream()
                                    .reduce(0.0F, (subtotal, featurex) -> subtotal + featurex.probability().getValue(), Float::sum);
                                Feature feature = this.wgTile.findFeature(features, prefilterProba, prefilterProba, rnd);
                                if (feature != null && feature.probability().getValue() != 0.0) {
                                    square.DeleteTileObject(object);
                                    List<TileGroup> tileGroups = feature.tileGroups();
                                    TileGroup tileGroup = tileGroups.get(rnd.nextInt(tileGroups.size()));
                                    String tile = tileGroup.tiles().get(0);
                                    this.wgTile.applyTile(tile, square, cell, x, y, 0, rnd);
                                    IsoWorld.instance.getAttachmentsHandler().resetAttachments(ch);
                                    ch.setAttachmentsPartial(new SquareCoord(x, y, 0));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private WorldGenZone getWorldGenZoneAt(int squareX, int squareY, int squareZ) {
        IsoMetaCell metaCell = IsoWorld.instance.metaGrid.getCellData(squareX / 256, squareY / 256);
        if (metaCell != null && metaCell.worldGenZones != null) {
            ArrayList<WorldGenZone> zones = metaCell.worldGenZones;

            for (int i = 0; i < zones.size(); i++) {
                WorldGenZone zone = zones.get(i);
                if (zone.contains(squareX, squareY, squareZ)) {
                    return zone;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private void placeRoad(
        Road road, IsoCell cell, IsoGridSquare square, int x, int y, int z, int tileX, int tileY, int tileZ, EnumMap<FeatureType, String[]> done, Random rnd
    ) {
        this.wgTile.setTile(road, square, cell, x, y, z, tileX, tileY, tileZ, done, rnd);
    }

    private void applyBiome(
        IBiome biome,
        IsoCell cell,
        IsoChunk ch,
        IsoGridSquare square,
        int x,
        int y,
        int z,
        int tileX,
        int tileY,
        int tileZ,
        EnumMap<FeatureType, String[]> done,
        boolean isMap,
        Random rnd
    ) {
        this.wgTile.setTiles(biome, square, ch, cell, x, y, z, tileX, tileY, tileZ, done, isMap, rnd);
        square.FixStackableObjects();
        this.generateZombies(biome.zombies(), square, rnd);
    }

    private boolean applyBiome(
        IBiome biome,
        FeatureType type,
        IsoCell cell,
        IsoChunk ch,
        IsoGridSquare square,
        int x,
        int y,
        int z,
        int tileX,
        int tileY,
        int tileZ,
        EnumMap<FeatureType, String[]> done,
        boolean isMap,
        Random rnd
    ) {
        if (this.wgTile.setTiles(biome, type, square, ch, cell, x, y, z, tileX, tileY, tileZ, done, rnd)) {
            square.FixStackableObjects();
            this.generateZombies(biome.zombies(), square, rnd);
            return true;
        } else {
            return false;
        }
    }

    private void applyOreVeins(
        IsoCell cell, IsoGridSquare square, int x, int y, int z, int tileX, int tileY, int tileZ, EnumMap<FeatureType, String[]> done, Random rnd
    ) {
        for (int px = -10; px <= 10; px++) {
            for (int py = -10; py <= 10; py++) {
                int targetCellX = cell.getWorldX() + px;
                int targetCellY = cell.getWorldY() + py;

                for (OreVein oreVein : this.veins.get(targetCellX, targetCellY)) {
                    if (oreVein.isValid(x, y, rnd)) {
                        this.wgTile.setTile(oreVein, square, cell, x, y, z, tileX, tileY, tileZ, done, rnd);
                    }
                }
            }
        }
    }

    private void applyPrefab(
        PrefabStructure prefab,
        IBiome biome,
        IsoCell cell,
        IsoChunk ch,
        IsoGridSquare square,
        int x,
        int y,
        int z,
        int tileX,
        int tileY,
        int tileZ,
        EnumMap<FeatureType, String[]> done,
        int xmin,
        int xmax,
        int ymin,
        int ymax,
        Random rnd
    ) {
        int prefabX = Math.abs(x - xmin) % prefab.getX();
        int prefabY = Math.abs(y - ymin) % prefab.getY();

        for (String category : prefab.getCategories()) {
            if (prefab.hasCategory(category)) {
                int tileRef = prefab.getTileRef(category, prefabX, prefabY);
                if (tileRef == 0) {
                    if (category.equals("Floor")) {
                        this.wgTile.setTiles(biome, FeatureType.GROUND, square, ch, cell, x, y, z, tileX, tileY, tileZ, done, rnd);
                    }
                } else {
                    String tileName = prefab.getTile(tileRef - 1);
                    this.wgTile.applyTile(tileName, square, cell, x, y, z, rnd);
                }
            }
        }

        square.FixStackableObjects();
        this.generateZombies(prefab.getZombies(), square, rnd);
    }

    private void generateZombies(float zombiesRnd, IsoGridSquare square, Random rnd) {
        if (rnd.nextFloat() < zombiesRnd) {
            square.chunk.proceduralZombieSquares.add(square);
        }
    }

    public void addZombieToSquare(IsoGridSquare square) {
        try {
            this.randomizedWorldBase.addZombiesOnSquare(1, null, 50, square);
        } catch (Exception var3) {
            DebugLog.log("Failed to load zombie");
            ExceptionLogger.logException(var3);
        }
    }

    public IBiome getBiome(int x, int y) {
        return BiomeRegistry.instance
            .get(
                this.biomes,
                this.simplex.noise(x, y),
                this.simplex.selector(x, y),
                this.landscape,
                this.plant,
                this.bush,
                this.temperature,
                this.hygrometry,
                this.oreLevel
            );
    }

    public IBiome getMapBiome(int x, int y, String filter) {
        return BiomeRegistry.instance.get(this.biomesMap, filter, this.simplex.noise(x, y), this.simplex.selector(x, y), this.bush, this.oreLevel);
    }

    public boolean priority(String tile, String tileRemote) {
        return this.priorities.get(tile) != null && this.priorities.get(tileRemote) != null
            ? this.priorities.get(tile) < this.priorities.get(tileRemote)
            : false;
    }

    public boolean protected_(List<String> protected_, String tile) {
        for (String check : protected_) {
            check = "^" + check;
            check = check.replace(".", "\\.");
            check = check.replace("*", ".*");
            check = check.replace("?", ".?");
            if (tile.matches(check)) {
                return true;
            }
        }

        return false;
    }

    public void cleanChunk(IsoChunk chunk, String material, String filter) {
        int cleaned = 0;
        int z = 0;
        String mat = "^" + material;
        mat = mat.replace(".", "\\.");
        mat = mat.replace("*", ".*");
        mat = mat.replace("?", ".?");
        List<IsoObject> toRemove = new ArrayList<>();

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                IsoGridSquare square = chunk.getGridSquare(x, y, 0);
                if (square != null) {
                    IsoObject floor = square.getFloor();
                    if (floor != null) {
                        String floorMaterial = floor.getSprite().getProperties().get("FloorMaterial");
                        if (floorMaterial != null && floorMaterial.matches(mat)) {
                            for (int i = 0; i < square.getObjects().size(); i++) {
                                IsoObject object = square.getObjects().get(i);
                                if (object.getSprite().getName().startsWith(filter)) {
                                    toRemove.add(object);
                                    cleaned++;
                                }
                            }
                        }

                        for (IsoObject object : toRemove) {
                            square.DeleteTileObject(object);
                        }
                    }
                }
            }
        }

        if (cleaned > 0) {
            DebugLog.WorldGen.debugln("%s | %s > Cleaned %s tiles", material, filter, cleaned);
        }
    }
}
