// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.maps;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import se.krka.kahlua.vm.KahluaTable;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.debug.DebugLog;
import zombie.iso.IsoWorld;
import zombie.iso.worldgen.WorldGenReader;
import zombie.iso.worldgen.utils.CacheMap;
import zombie.iso.worldgen.utils.CellCoord;

public class BiomeMap {
    private Map<Integer, BiomeMapEntry> zoneMap;
    private Map<CellCoord, BiomeRaster> cache = new CacheMap<>(16);
    private List<String> foragingZones;

    public BiomeMap() {
        WorldGenReader reader = new WorldGenReader();
        this.zoneMap = reader.loadBiomeMapConfig((KahluaTable)LuaManager.env.rawget("biome_map_config"));
        this.foragingZones = new ArrayList<>();

        for (Entry<Integer, BiomeMapEntry> entry : this.zoneMap.entrySet()) {
            this.foragingZones.add(entry.getValue().zone());
        }
    }

    private BiomeRaster getRaster(CellCoord coord) {
        if (this.cache.containsKey(coord)) {
            return this.cache.get(coord);
        } else {
            String[] worldNames = IsoWorld.instance.getMap().split(";");
            String filename = null;

            for (String name : worldNames) {
                String relativePath = String.format("media/maps/%s/maps/biomemap_%d_%d.png", name, coord.x(), coord.y());
                String absPath = ZomboidFileSystem.instance.getString(relativePath);
                File fo = new File(absPath);
                if (fo.exists()) {
                    filename = relativePath;
                    break;
                }
            }

            BiomeRaster raster;
            if (filename != null) {
                try {
                    raster = new BiomeRaster();
                    raster.createFromImage(filename);
                } catch (Exception var11) {
                    DebugLog.log(String.format("Loading error of BiomeMap at (%d, %d)", coord.x(), coord.y()));
                    raster = null;
                }
            } else {
                DebugLog.log(String.format("BiomeMap could not find any biome image file at (%d, %d)", coord.x(), coord.y()));
                raster = null;
            }

            this.cache.put(coord, raster);
            return raster;
        }
    }

    public int[] getZones(int chunkX, int chunkY, BiomeMap.Type type) {
        int cellX = chunkX / 32;
        int cellY = chunkY / 32;
        int minChunkX = cellX * 32;
        int minChunkY = cellY * 32;
        if (chunkX - minChunkX >= 0 && chunkY - minChunkY >= 0) {
            BiomeRaster raster = this.getRaster(new CellCoord(cellX, cellY));
            return raster == null ? null : raster.getSamples((chunkX - minChunkX) * 8, (chunkY - minChunkY) * 8, 8, 8, type.getId(), null);
        } else {
            return null;
        }
    }

    public String getZoneName(int index) {
        BiomeMapEntry entry = this.zoneMap.getOrDefault(index, null);
        return entry == null ? null : entry.zone();
    }

    public String getBiomeName(int index) {
        BiomeMapEntry entry = this.zoneMap.getOrDefault(index, null);
        return entry == null ? null : entry.biome();
    }

    public String getOreName(int index) {
        BiomeMapEntry entry = this.zoneMap.getOrDefault(index, null);
        return entry == null ? null : entry.ore();
    }

    public List<String> getForagingZones() {
        return this.foragingZones;
    }

    public BiomeMapEntry getEntry(int index) {
        return this.zoneMap.getOrDefault(index, null);
    }

    public void Dispose() {
        this.cache = null;
        this.zoneMap = null;
        this.foragingZones = null;
    }

    public static enum Type {
        BIOME(0),
        ZONE(1);

        private final int id;

        private Type(final int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }
    }
}
