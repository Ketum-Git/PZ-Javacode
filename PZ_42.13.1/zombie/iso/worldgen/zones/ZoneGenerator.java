// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.zones;

import gnu.trove.list.array.TIntArrayList;
import java.awt.Rectangle;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import se.krka.kahlua.vm.KahluaTable;
import zombie.ChunkMapFilenames;
import zombie.SandboxOptions;
import zombie.Lua.LuaManager;
import zombie.characters.animals.AnimalManagerWorker;
import zombie.characters.animals.AnimalZone;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.inventory.ItemConfigurator;
import zombie.iso.IsoLot;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaChunk;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.LotHeader;
import zombie.iso.MapFiles;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.enums.MetaCellPresence;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.worldgen.WorldGenParams;
import zombie.iso.worldgen.WorldGenReader;
import zombie.iso.worldgen.maps.BiomeMap;
import zombie.iso.worldgen.utils.ChunkCoord;
import zombie.iso.zones.ZoneGeometryType;
import zombie.network.GameClient;
import zombie.network.GameServer;

public class ZoneGenerator {
    private final BiomeMap map;
    private final List<AnimalsPathConfig> animalsPathConfig;
    private final ArrayList<ZoneGenerator.TreeCount> treeCounts = new ArrayList<>();

    public ZoneGenerator(BiomeMap biomeMap) {
        this.map = biomeMap;
        WorldGenReader reader = new WorldGenReader();
        this.animalsPathConfig = reader.loadAnimalsPath((KahluaTable)LuaManager.env.rawget("animals_path_config"));
    }

    public void genForaging(int chunkX, int chunkY) {
        IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
        int metaCellX = PZMath.fastfloor(chunkX / 32.0F);
        int metaCellY = PZMath.fastfloor(chunkY / 32.0F);
        if (metaGrid.hasCellData(metaCellX, metaCellY) == MetaCellPresence.NOT_LOADED) {
            metaGrid.setCellData(metaCellX, metaCellY, new IsoMetaCell(metaCellX, metaCellY));
        }

        IsoMetaChunk metaChunk = metaGrid.getChunkData(chunkX, chunkY);
        if (metaGrid.hasCellData(metaCellX, metaCellY) == MetaCellPresence.LOADED && !metaChunk.doesHaveForaging()) {
            int cellSquareX = metaCellX * 256;
            int cellSquareY = metaCellY * 256;
            int minChunkX = PZMath.fastfloor(cellSquareX / 8.0F);
            int minChunkY = PZMath.fastfloor(cellSquareY / 8.0F);
            Map<Integer, Boolean[]> foraging = new HashMap<>();

            for (int x = 0; x < 32; x++) {
                for (int y = 0; y < 32; y++) {
                    int coords = y * 32 + x;

                    Set<Integer> requestedZones;
                    try {
                        int[] samples = this.map.getZones(minChunkX + x, minChunkY + y, BiomeMap.Type.ZONE);
                        requestedZones = Arrays.stream(samples).distinct().boxed().collect(HashSet::new, HashSet::add, AbstractCollection::addAll);
                    } catch (NullPointerException | ArrayIndexOutOfBoundsException var25) {
                        requestedZones = Set.of(255);
                    }

                    boolean hasForaging = metaGrid.getChunkData(x + minChunkX, y + minChunkY).doesHaveForaging();

                    for (Integer zones : requestedZones) {
                        foraging.computeIfAbsent(zones, k -> {
                            Boolean[] booleans = new Boolean[1024];
                            Arrays.fill(booleans, Boolean.valueOf(true));
                            return booleans;
                        })[coords] = hasForaging;
                    }
                }
            }

            for (Integer key : foraging.keySet()) {
                ItemConfigurator.registerZone(this.map.getZoneName(key));
            }

            for (Entry<Integer, Boolean[]> entry : foraging.entrySet()) {
                Integer key = entry.getKey();
                if (this.map.getZoneName(key) == null) {
                    DebugLog.log("Zone " + key + " not found in ZONE_MAP");
                } else {
                    Boolean[] hasForaging = entry.getValue();
                    int yoffset = 0;

                    while (!Arrays.stream(hasForaging).allMatch(b -> b)) {
                        int minX = 0;
                        int maxX = 0;
                        boolean found = false;

                        for (int x = 0; x < 32; x++) {
                            if (!hasForaging[yoffset * 32 + x]) {
                                minX = x;
                                found = true;
                                break;
                            }
                        }

                        if (!found) {
                            yoffset++;
                        } else {
                            for (int xx = minX; xx <= 32; xx++) {
                                if (xx == 32 || hasForaging[yoffset * 32 + xx]) {
                                    maxX = xx - 1;
                                    break;
                                }
                            }

                            int maxY = 32;

                            label104:
                            for (int y = yoffset + 1; y < 32; y++) {
                                for (int xxx = minX; xxx <= maxX; xxx++) {
                                    if (hasForaging[y * 32 + xxx]) {
                                        maxY = y;
                                        break label104;
                                    }
                                }
                            }

                            IsoWorld.instance
                                .getMetaGrid()
                                .registerZone(
                                    "",
                                    this.map.getZoneName(key),
                                    (minX + minChunkX) * 8,
                                    (yoffset + minChunkY) * 8,
                                    0,
                                    (maxX + 1 - minX) * 8,
                                    (maxY - yoffset) * 8
                                );

                            for (int xxxx = 0; xxxx < 32; xxxx++) {
                                for (int y = 0; y < 32; y++) {
                                    int var24 = y * 32 + xxxx;
                                    hasForaging[var24] = hasForaging[var24]
                                        | metaGrid.getChunkData(xxxx + minChunkX, y + minChunkY).doesHaveZone(this.map.getZoneName(key));
                                }
                            }

                            yoffset = 0;
                        }
                    }
                }
            }
        }
    }

    public void genAnimalsPath(int chunkX, int chunkY) {
        if (SandboxOptions.getInstance().animalPathChance.getValue() != 1) {
            IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
            int metaCellX = PZMath.fastfloor(chunkX / 32.0F);
            int metaCellY = PZMath.fastfloor(chunkY / 32.0F);
            if (metaGrid.hasCellData(metaCellX, metaCellY) == MetaCellPresence.NOT_LOADED) {
                metaGrid.setCellData(metaCellX, metaCellY, new IsoMetaCell(metaCellX, metaCellY));
            }

            IsoMetaCell metaCell = metaGrid.getCellData(metaCellX, metaCellY);
            if (metaGrid.hasCellData(metaCellX, metaCellY) == MetaCellPresence.LOADED) {
                metaGrid.addCellToSave(metaCell);
                if (metaCell.getAnimalZonesSize() == 0) {
                    int cellSquareX = metaCellX * 256;
                    int cellSquareY = metaCellY * 256;
                    List<Rectangle> aabbs = new ArrayList<>();

                    for (int configIndex = 0; configIndex < this.animalsPathConfig.size(); configIndex++) {
                        AnimalsPathConfig config = this.animalsPathConfig.get(configIndex);
                        Random rnd = WorldGenParams.INSTANCE.getRandom(metaCellX, metaCellY, config.getNameHash() + configIndex);
                        int lineWidth = 0;
                        String name = "";
                        String type = "Animal";
                        String action = "Follow";
                        String animalType = config.animalType();
                        int pointsMin = config.points()[0];
                        int pointsMax = config.points().length > 1 ? config.points()[1] : config.points()[0];
                        int radiusMin = config.radius()[0];
                        int radiusMax = config.radius().length > 1 ? config.radius()[1] : config.radius()[0];
                        int extensionMin = config.extension()[0];
                        int extensionMax = config.extension().length > 1 ? config.extension()[1] : config.extension()[0];
                        if (pointsMin >= 0
                            && (pointsMin != 0 || pointsMax != 0)
                            && radiusMin >= 0
                            && (radiusMin != 0 || radiusMax != 0)
                            && extensionMin >= 0
                            && (extensionMin != 0 || extensionMax != 0)) {
                            float chance = switch (SandboxOptions.getInstance().animalPathChance.getValue()) {
                                case 2 -> 0.01F;
                                case 3 -> 0.05F;
                                case 4 -> 0.1F;
                                case 5 -> 0.2F;
                                default -> 0.65F;
                            };

                            label158:
                            for (int c = 0; c < config.count(); c++) {
                                if (!(rnd.nextFloat() > chance)) {
                                    int minX = Integer.MAX_VALUE;
                                    int minY = Integer.MAX_VALUE;
                                    int maxX = Integer.MIN_VALUE;
                                    int maxY = Integer.MIN_VALUE;
                                    int centerX = radiusMax + rnd.nextInt(Math.max(256 - 2 * radiusMax, 1));
                                    int centerY = radiusMax + rnd.nextInt(Math.max(256 - 2 * radiusMax, 1));
                                    int pointsCount = rnd.nextInt(pointsMax - pointsMin + 1) + pointsMin;
                                    int rotation = rnd.nextInt(360);
                                    List<AnimalZone> zoneExts = new ArrayList<>();
                                    TIntArrayList points = new TIntArrayList();
                                    int extensionCount = 0;

                                    for (int i = 0; i < pointsCount; i++) {
                                        int radius = rnd.nextInt(radiusMax - radiusMin + 1) + radiusMin;
                                        double angle = Math.toRadians(360.0 / pointsCount * i + rotation);
                                        int x = (int)Math.min(
                                            Math.max(radius * Math.cos(angle) + centerX + cellSquareX, (double)(cellSquareX + 1)),
                                            (double)(cellSquareX + 256 - 2)
                                        );
                                        int y = (int)Math.min(
                                            Math.max(radius * Math.sin(angle) + centerY + cellSquareY, (double)(cellSquareY + 1)),
                                            (double)(cellSquareY + 256 - 2)
                                        );
                                        points.add(x);
                                        points.add(y);
                                        minX = Math.min(minX, x);
                                        maxX = Math.max(maxX, x);
                                        minY = Math.min(minY, y);
                                        maxY = Math.max(maxY, y);
                                        if (rnd.nextFloat() < config.extensionChance()
                                            || i == pointsCount - 2 && extensionCount == 0
                                            || i == pointsCount - 1 && extensionCount == 1) {
                                            AnimalZone zoneExt = this.getExtensionZone(
                                                "", "Animal", extensionCount % 2 == 0 ? "Eat" : "Sleep", 0, x, y, rnd, extensionMin, extensionMax
                                            );
                                            zoneExts.add(zoneExt);
                                            extensionCount++;
                                        }
                                    }

                                    for (int ix = 0; ix < points.size(); ix += 2) {
                                        int x = points.get(ix);
                                        int y = points.get(ix + 1);

                                        for (Rectangle aabb : aabbs) {
                                            if (aabb.contains(x, y)) {
                                                continue label158;
                                            }
                                        }

                                        IsoMetaChunk metaChunk = metaCell.getChunk((x - cellSquareX) / 8, (y - cellSquareY) / 8);
                                        if (metaChunk.doesHaveZone("TrailerPark")
                                            || metaChunk.doesHaveZone("TownZone")
                                            || metaChunk.doesHaveZone("Vegitation")
                                            || metaChunk.doesHaveZone("Water")
                                            || !metaChunk.doesHaveForaging()) {
                                            continue label158;
                                        }
                                    }

                                    aabbs.add(new Rectangle(minX, minY, maxX - minX, maxY - minY));
                                    AnimalZone zone = new AnimalZone("", "Animal", minX, minY, 0, maxX - minX + 1, maxY - minY + 1, "Follow", animalType, true);
                                    zone.geometryType = ZoneGeometryType.Polyline;
                                    zone.points.addAll(points);
                                    zone.polylineWidth = 0;
                                    AnimalZone zoneLoop = this.getLoopingZone("", "Animal", "Follow", animalType, 0, points);
                                    metaGrid.registerAnimalZone(zone);
                                    metaGrid.registerAnimalZone(zoneLoop);
                                    zoneExts.forEach(metaGrid::registerAnimalZone);
                                }
                            }
                        }
                    }

                    if (metaCell.getAnimalZonesSize() != 0) {
                        AnimalManagerWorker.getInstance().allocCell(metaCellX, metaCellY);
                    }

                    this.resetTreeCounts();
                }
            }
        }
    }

    private AnimalZone getLoopingZone(String name, String type, String action, String animalType, int lineWidth, TIntArrayList points) {
        TIntArrayList pointsLoop = new TIntArrayList();
        pointsLoop.add(points.get(points.size() - 2));
        pointsLoop.add(points.get(points.size() - 1));
        pointsLoop.add(points.get(0));
        pointsLoop.add(points.get(1));
        AnimalZone zoneLoop = new AnimalZone(
            name,
            type,
            pointsLoop.get(0),
            pointsLoop.get(1),
            0,
            pointsLoop.get(2) - pointsLoop.get(0) + 1,
            pointsLoop.get(3) - pointsLoop.get(1) + 1,
            action,
            animalType,
            false
        );
        zoneLoop.geometryType = ZoneGeometryType.Polyline;
        zoneLoop.points.addAll(pointsLoop);
        zoneLoop.polylineWidth = lineWidth;
        return zoneLoop;
    }

    private AnimalZone getExtensionZone(String name, String type, String action, int lineWidth, int x, int y, Random rnd, int extensionMin, int extensionMax) {
        TIntArrayList pointsExt = new TIntArrayList();
        pointsExt.add(x);
        pointsExt.add(y);
        if (GameServer.server) {
            int rotExt = rnd.nextInt(360);
            int radiusExt = rnd.nextInt(extensionMax - extensionMin + 1) + extensionMin;
            pointsExt.add((int)(radiusExt * Math.cos(rotExt)) + x);
            pointsExt.add((int)(radiusExt * Math.sin(rotExt)) + y);
        } else if (!GameClient.client) {
            this.applyRandomLookup(pointsExt, x, y, extensionMin, extensionMax, rnd);
        }

        AnimalZone zoneExt = new AnimalZone(
            name,
            type,
            pointsExt.get(0),
            pointsExt.get(1),
            0,
            pointsExt.get(2) - pointsExt.get(0) + 1,
            pointsExt.get(3) - pointsExt.get(1) + 1,
            action,
            null,
            false
        );
        zoneExt.geometryType = ZoneGeometryType.Polyline;
        zoneExt.points.addAll(pointsExt);
        zoneExt.polylineWidth = lineWidth;
        return zoneExt;
    }

    private void applyRandomLookup(TIntArrayList pointsExt, int x, int y, int extensionMin, int extensionMax, Random rnd) {
        int TREE_LIMIT = 3;
        int chunkX = x / 8;
        int chunkY = y / 8;
        int r = (int)Math.ceil(extensionMax / 8.0F);
        int r2 = r * r;
        int chunkXMin = chunkX - r;
        int chunkXMax = chunkX + r;
        int chunkYMin = chunkY - r;
        int chunkYMax = chunkY + r;
        List<ChunkCoord> validChunks = new ArrayList<>();

        for (int xx = chunkXMin; xx <= chunkXMax; xx++) {
            for (int yy = chunkYMin; yy <= chunkYMax; yy++) {
                int dx = xx - chunkX;
                int dy = yy - chunkY;
                int d2 = dx * dx + dy * dy;
                if (d2 - r2 <= 0 && this.getTrees(xx, yy) <= 3) {
                    validChunks.add(new ChunkCoord(xx, yy));
                }
            }
        }

        if (!validChunks.isEmpty()) {
            ChunkCoord validChunk = validChunks.get(rnd.nextInt(validChunks.size()));
            pointsExt.add(validChunk.x() * 8 + 4);
            pointsExt.add(validChunk.y() * 8 + 4);
        } else {
            int rotExt = rnd.nextInt(360);
            int radiusExt = rnd.nextInt(extensionMax - extensionMin + 1) + extensionMin;
            pointsExt.add((int)(radiusExt * Math.cos(rotExt)) + x);
            pointsExt.add((int)(radiusExt * Math.sin(rotExt)) + y);
        }
    }

    private ZoneGenerator.TreeCount getTreeCount(int cellX, int cellY) {
        for (int i = 0; i < this.treeCounts.size(); i++) {
            ZoneGenerator.TreeCount treeCount = this.treeCounts.get(i);
            if (treeCount.cellX == cellX && treeCount.cellY == cellY) {
                return treeCount;
            }
        }

        return null;
    }

    private void resetTreeCounts() {
        this.treeCounts.clear();
    }

    private int getTrees(int chunkX, int chunkY) {
        int cellX = PZMath.fastfloor(chunkX / 32.0F);
        int cellY = PZMath.fastfloor(chunkY / 32.0F);
        ZoneGenerator.TreeCount treeCount = this.getTreeCount(cellX, cellY);
        if (treeCount != null && treeCount.hasCount(chunkX, chunkY)) {
            return treeCount.getCount(chunkX, chunkY);
        } else {
            String key = ChunkMapFilenames.instance.getHeader(cellX, cellY);
            LotHeader lotHeader = IsoLot.InfoHeaders.get(key);
            if (lotHeader == null) {
                return 0;
            } else {
                boolean[] bTreeSquares = new boolean[64];
                IsoLot lot = IsoLot.get(lotHeader.mapFiles, cellX, cellY, chunkX, chunkY, null);

                try {
                    boolean[] bDoneSquares = new boolean[64];
                    int nonEmptySquareCount = this.PlaceLot(lot, lot.minLevel, bTreeSquares, bDoneSquares);
                    if (nonEmptySquareCount < 64) {
                        for (int i = lot.info.mapFiles.priority + 1; i < IsoLot.MapFiles.size(); i++) {
                            MapFiles mapFiles = IsoLot.MapFiles.get(i);
                            if (mapFiles.hasCell(cellX, cellY)) {
                                IsoLot lot2 = null;

                                try {
                                    lot2 = IsoLot.get(mapFiles, cellX, cellY, chunkX, chunkY, null);
                                    nonEmptySquareCount = this.PlaceLot(lot2, lot2.minLevel, bTreeSquares, bDoneSquares);
                                    if (nonEmptySquareCount == 64) {
                                        break;
                                    }
                                } finally {
                                    if (lot2 != null) {
                                        IsoLot.put(lot2);
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    if (lot != null) {
                        IsoLot.put(lot);
                    }
                }

                int trees = 0;

                for (int xx = 0; xx < 8; xx++) {
                    for (int yy = 0; yy < 8; yy++) {
                        if (bTreeSquares[xx + yy * 8]) {
                            trees++;
                        }
                    }
                }

                if (treeCount == null) {
                    treeCount = new ZoneGenerator.TreeCount();
                    treeCount.cellX = cellX;
                    treeCount.cellY = cellY;
                    this.treeCounts.add(treeCount);
                }

                treeCount.setCount(chunkX, chunkY, trees);
                return trees;
            }
        }
    }

    private int PlaceLot(IsoLot lot, int sz, boolean[] bTreeSquares, boolean[] bDoneSquares) {
        int z = 0;
        int minLevel = Math.max(sz, -32);
        int maxLevel = Math.min(sz + lot.maxLevel - lot.minLevel - 1, 31);
        if (0 >= minLevel && 0 <= maxLevel) {
            int nonEmptySquareCount = 0;

            for (int x = 0; x < 8; x++) {
                for (int y = 0; y < 8; y++) {
                    if (bDoneSquares[x + y * 8]) {
                        nonEmptySquareCount++;
                    } else {
                        int lz = 0 - lot.info.minLevel;
                        int squareXYZ = x + y * 8 + lz * 8 * 8;
                        int offsetInData = lot.offsetInData[squareXYZ];
                        if (offsetInData != -1) {
                            int numInts = lot.data.getQuick(offsetInData);
                            if (numInts > 0) {
                                if (!bDoneSquares[x + y * 8]) {
                                    bDoneSquares[x + y * 8] = true;
                                    nonEmptySquareCount++;
                                }

                                for (int n = 0; n < numInts; n++) {
                                    String tile = lot.info.tilesUsed.get(lot.data.get(offsetInData + 1 + n));
                                    IsoSprite spr = IsoSpriteManager.instance.namedMap.get(tile);
                                    if (spr != null && spr.getType() == IsoObjectType.tree) {
                                        bTreeSquares[x + y * 8] = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return nonEmptySquareCount;
        } else {
            return 0;
        }
    }

    private static final class TreeCount {
        int cellX;
        int cellY;
        final byte[] countPerChunk = new byte[1024];

        TreeCount() {
            Arrays.fill(this.countPerChunk, (byte)-1);
        }

        boolean hasCount(int chunkX, int chunkY) {
            return this.getCount(chunkX, chunkY) >= 0;
        }

        int getCount(int chunkX, int chunkY) {
            int index = chunkX - this.cellX * 32 + (chunkY - this.cellY * 32) * 32;
            return this.countPerChunk[index];
        }

        void setCount(int chunkX, int chunkY, int count) {
            int index = chunkX - this.cellX * 32 + (chunkY - this.cellY * 32) * 32;
            this.countPerChunk[index] = (byte)PZMath.clamp(count, 0, 127);
        }
    }
}
