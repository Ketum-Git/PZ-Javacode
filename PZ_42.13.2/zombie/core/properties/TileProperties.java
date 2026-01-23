// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.iso.IsoDirections;

@UsedFromLua
public final class TileProperties {
    private final Map<TilePropertyKey, String> properties = new HashMap<>();
    private final EnumSet<TileProperties.SurfaceFlag> surfaceFlags = EnumSet.noneOf(TileProperties.SurfaceFlag.class);
    private boolean surfaceValid;
    private byte surface;
    private byte itemHeight;
    private short stackReplaceTileOffset;
    private IsoDirections slopedSurfaceDirection;
    private byte slopedSurfaceHeightMin;
    private byte slopedSurfaceHeightMax;

    public boolean has(TilePropertyKey tilePropertyKey) {
        return this.properties.containsKey(tilePropertyKey);
    }

    public void set(TilePropertyKey tilePropertyKey, String value) {
        if (tilePropertyKey != null) {
            this.properties.put(tilePropertyKey, value == null ? "" : value);
            this.invalidateSurfaceCache();
        }
    }

    public void set(TilePropertyKey tilePropertyKey) {
        this.set(tilePropertyKey, "");
    }

    public void unset(TilePropertyKey tilePropertyKey) {
        if (tilePropertyKey != null) {
            this.properties.remove(tilePropertyKey);
            this.invalidateSurfaceCache();
        }
    }

    public String get(TilePropertyKey tilePropertyKey) {
        return this.properties.get(tilePropertyKey);
    }

    public boolean propertyEquals(TilePropertyKey tilePropertyKey, String value) {
        String v = this.properties.get(tilePropertyKey);
        return v != null && v.equals(value);
    }

    public void addProperties(TileProperties other) {
        if (other != null) {
            this.properties.putAll(other.properties);
            this.invalidateSurfaceCache();
        }
    }

    public List<TilePropertyKey> getTileProperties() {
        List<TilePropertyKey> list = new ArrayList<>(this.properties.keySet());
        list.sort(Comparator.comparing(TilePropertyKey::toString));
        return list;
    }

    public Map<TilePropertyKey, String> getAll() {
        return Collections.unmodifiableMap(this.properties);
    }

    public void clear() {
        this.properties.clear();
        this.surfaceFlags.clear();
        this.invalidateSurfaceCache();
    }

    private void invalidateSurfaceCache() {
        this.surfaceValid = false;
    }

    private void initSurface() {
        if (!this.surfaceValid) {
            this.surfaceFlags.clear();
            this.surface = 0;
            this.itemHeight = 0;
            this.stackReplaceTileOffset = 0;
            this.slopedSurfaceDirection = null;
            this.slopedSurfaceHeightMin = 0;
            this.slopedSurfaceHeightMax = 0;

            for (Entry<TilePropertyKey, String> entry : this.properties.entrySet()) {
                TilePropertyKey tilePropertyKey = entry.getKey();
                String val = entry.getValue();
                if (tilePropertyKey == TilePropertyKey.SURFACE) {
                    try {
                        this.surface = (byte)Integer.parseInt(val);
                    } catch (NumberFormatException var6) {
                    }
                } else if (tilePropertyKey == TilePropertyKey.IS_SURFACE_OFFSET) {
                    this.surfaceFlags.add(TileProperties.SurfaceFlag.ISOFFSET);
                } else if (tilePropertyKey == TilePropertyKey.IS_TABLE) {
                    this.surfaceFlags.add(TileProperties.SurfaceFlag.ISTABLE);
                } else if (tilePropertyKey == TilePropertyKey.IS_TABLE_TOP) {
                    this.surfaceFlags.add(TileProperties.SurfaceFlag.ISTABLETOP);
                } else if (tilePropertyKey == TilePropertyKey.STACK_REPLACE_TILE_OFFSET) {
                    try {
                        this.stackReplaceTileOffset = (short)Integer.parseInt(val);
                    } catch (NumberFormatException var8) {
                    }
                } else if (tilePropertyKey == TilePropertyKey.ITEM_HEIGHT) {
                    try {
                        this.itemHeight = (byte)Integer.parseInt(val);
                    } catch (NumberFormatException var7) {
                    }
                } else if (tilePropertyKey == TilePropertyKey.SLOPED_SURFACE_DIRECTION) {
                    this.slopedSurfaceDirection = IsoDirections.fromString(val);
                } else if (tilePropertyKey == TilePropertyKey.SLOPED_SURFACE_HEIGHT_MIN) {
                    this.slopedSurfaceHeightMin = (byte)PZMath.clamp(PZMath.tryParseInt(val, 0), 0, 100);
                } else if (tilePropertyKey == TilePropertyKey.SLOPED_SURFACE_HEIGHT_MAX) {
                    this.slopedSurfaceHeightMax = (byte)PZMath.clamp(PZMath.tryParseInt(val, 0), 0, 100);
                }
            }

            this.surfaceValid = true;
        }
    }

    public int getSurface() {
        this.initSurface();
        return this.surface;
    }

    public boolean isSurfaceOffset() {
        this.initSurface();
        return this.surfaceFlags.contains(TileProperties.SurfaceFlag.ISOFFSET);
    }

    public boolean isTable() {
        this.initSurface();
        return this.surfaceFlags.contains(TileProperties.SurfaceFlag.ISTABLE);
    }

    public boolean isTableTop() {
        this.initSurface();
        return this.surfaceFlags.contains(TileProperties.SurfaceFlag.ISTABLETOP);
    }

    public int getStackReplaceTileOffset() {
        this.initSurface();
        return this.stackReplaceTileOffset;
    }

    public int getItemHeight() {
        this.initSurface();
        return this.itemHeight;
    }

    public IsoDirections getSlopedSurfaceDirection() {
        this.initSurface();
        return this.slopedSurfaceDirection;
    }

    public int getSlopedSurfaceHeightMin() {
        this.initSurface();
        return this.slopedSurfaceHeightMin;
    }

    public int getSlopedSurfaceHeightMax() {
        this.initSurface();
        return this.slopedSurfaceHeightMax;
    }

    @Override
    public String toString() {
        return "TilePropertyContainer{properties=" + this.properties + ", surfaceFlags=" + this.surfaceFlags + "}";
    }

    public static enum SurfaceFlag {
        INVALID,
        ISOFFSET,
        ISTABLE,
        ISTABLETOP;
    }
}
