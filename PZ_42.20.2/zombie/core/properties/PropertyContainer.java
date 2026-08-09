// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.properties;

import gnu.trove.map.hash.TShortShortHashMap;
import gnu.trove.set.TShortSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import zombie.UsedFromLua;
import zombie.core.TilePropertyAliasMap;
import zombie.core.math.PZMath;
import zombie.iso.IsoDirections;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.util.StringUtils;

@UsedFromLua
public final class PropertyContainer extends TShortShortHashMap {
    private long spriteFlags1;
    private long spriteFlags2;
    private short[] keyArray;
    public static List<Object> sorted = Collections.synchronizedList(new ArrayList<>());
    private byte surface;
    private byte surfaceFlags;
    private short stackReplaceTileOffset;
    private byte itemHeight;
    private IsoDirections slopedSurfaceDirection;
    private byte slopedSurfaceHeightMin;
    private byte slopedSurfaceHeightMax;
    private static final byte SURFACE_VALID = 1;
    private static final byte SURFACE_ISOFFSET = 2;
    private static final byte SURFACE_ISTABLE = 4;
    private static final byte SURFACE_ISTABLETOP = 8;

    public PropertyContainer() {
        super(10, 0.5F, (short)-1, (short)-1);
        this.setAutoCompactionFactor(0.0F);
    }

    public void CreateKeySet() {
        if (this.isEmpty()) {
            this.keyArray = null;
        } else {
            TShortSet keySet = this.keySet();
            this.keyArray = keySet.toArray();
        }
    }

    private void recreateKeyArray() {
        if (this.keyArray != null) {
            this.CreateKeySet();
        }
    }

    public void AddProperties(PropertyContainer other) {
        if (other.keyArray != null) {
            boolean recreateKeyArray = false;

            for (int i1 = 0; i1 < other.keyArray.length; i1++) {
                short key = other.keyArray[i1];
                short oldValue = this.put(key, other.get(key));
                recreateKeyArray |= oldValue == this.getNoEntryValue();
            }

            if (recreateKeyArray) {
                this.recreateKeyArray();
            }
        }

        this.spriteFlags1 = this.spriteFlags1 | other.spriteFlags1;
        this.spriteFlags2 = this.spriteFlags2 | other.spriteFlags2;
    }

    public void Clear() {
        this.spriteFlags1 = 0L;
        this.spriteFlags2 = 0L;
        this.clear();
        this.keyArray = null;
        this.surfaceFlags &= -2;
    }

    public boolean has(IsoFlagType flag) {
        long flags = flag.index() < 64 ? this.spriteFlags1 : this.spriteFlags2;
        return (flags & 1L << (flag.index() & 63)) != 0L;
    }

    public boolean has(Double flag) {
        return this.has(IsoFlagType.fromIndex(flag.intValue()));
    }

    public void set(String tilePropertyKey) {
        this.set(tilePropertyKey, "");
    }

    public void set(IsoPropertyType type, String propValue) {
        this.set(type.getName(), propValue);
    }

    public void set(String propName, String propValue) {
        this.set(propName, propValue, true);
    }

    public void set(IsoPropertyType type, String propValue, boolean checkIsoFlagType) {
        this.set(type.getName(), propValue, checkIsoFlagType);
    }

    public void set(String propName, String propValue, boolean checkIsoFlagType) {
        if (propName != null) {
            if (checkIsoFlagType) {
                IsoFlagType e = IsoFlagType.FromString(propName);
                if (e != IsoFlagType.MAX) {
                    this.set(e);
                    return;
                }
            }

            int p = TilePropertyAliasMap.instance.getIDFromPropertyName(propName);
            if (p != -1) {
                int v = TilePropertyAliasMap.instance.getIDFromPropertyValue(p, propValue);
                this.surfaceFlags &= -2;
                short oldValue = this.put((short)p, (short)v);
                if (oldValue == this.getNoEntryValue()) {
                    this.recreateKeyArray();
                }
            }
        }
    }

    public void set(IsoFlagType flag) {
        if (flag.index() / 64 == 0) {
            this.spriteFlags1 = this.spriteFlags1 | 1L << flag.index() % 64;
        } else {
            this.spriteFlags2 = this.spriteFlags2 | 1L << flag.index() % 64;
        }
    }

    public void set(IsoFlagType flag, String ignored) {
        this.set(flag);
    }

    public void unset(String propName) {
        int p = TilePropertyAliasMap.instance.getIDFromPropertyName(propName);
        short oldValue = this.remove((short)p);
        if (oldValue != this.getNoEntryValue()) {
            this.recreateKeyArray();
        }
    }

    public void unset(IsoFlagType flag) {
        if (flag.index() / 64 == 0) {
            this.spriteFlags1 = this.spriteFlags1 & ~(1L << flag.index() % 64);
        } else {
            this.spriteFlags2 = this.spriteFlags2 & ~(1L << flag.index() % 64);
        }
    }

    public String get(IsoPropertyType type) {
        return this.get(type.getName());
    }

    public String get(String name) {
        int p = TilePropertyAliasMap.instance.getIDFromPropertyName(name);
        return !this.containsKey((short)p) ? null : TilePropertyAliasMap.instance.getPropertyValueString(p, this.get((short)p));
    }

    public boolean propertyEquals(IsoPropertyType type, String value) {
        return StringUtils.equalsIgnoreCase(this.get(type), value);
    }

    public boolean propertyEquals(String name, String value) {
        return StringUtils.equalsIgnoreCase(this.get(name), value);
    }

    public boolean has(IsoPropertyType isoPropertyType) {
        return this.has(isoPropertyType.getName());
    }

    public boolean has(IsoPropertyType... isoPropertyType) {
        for (int i = 0; i < isoPropertyType.length; i++) {
            if (this.has(isoPropertyType[i])) {
                return true;
            }
        }

        return false;
    }

    public boolean has(String isoPropertyType) {
        int p = TilePropertyAliasMap.instance.getIDFromPropertyName(isoPropertyType);
        return this.containsKey((short)p);
    }

    public ArrayList<IsoFlagType> getFlagsList() {
        ArrayList<IsoFlagType> ret = new ArrayList<>();

        for (int i = 0; i < 64; i++) {
            if ((this.spriteFlags1 & 1L << i) != 0L) {
                ret.add(IsoFlagType.fromIndex(i));
            }
        }

        for (int ix = 0; ix < 64; ix++) {
            if ((this.spriteFlags2 & 1L << ix) != 0L) {
                ret.add(IsoFlagType.fromIndex(64 + ix));
            }
        }

        return ret;
    }

    public ArrayList<String> getPropertyNames() {
        ArrayList<String> list = new ArrayList<>();
        TShortSet s = this.keySet();
        s.forEach(i -> {
            list.add(TilePropertyAliasMap.instance.properties.get(i).propertyName);
            return true;
        });
        Collections.sort(list);
        return list;
    }

    private void initSurface() {
        if ((this.surfaceFlags & 1) == 0) {
            this.surface = 0;
            this.stackReplaceTileOffset = 0;
            this.surfaceFlags = 1;
            this.itemHeight = 0;
            this.slopedSurfaceDirection = null;
            this.slopedSurfaceHeightMin = 0;
            this.slopedSurfaceHeightMax = 0;
            this.forEachEntry((i, i1) -> {
                TilePropertyAliasMap.TileProperty p = TilePropertyAliasMap.instance.properties.get(i);
                String key = p.propertyName;
                String val = p.possibleValues.get(i1);
                switch (key) {
                    case "Surface":
                        if (val != null) {
                            try {
                                int pixels = Integer.parseInt(val);
                                if (pixels >= 0 && pixels <= 127) {
                                    this.surface = (byte)pixels;
                                }
                            } catch (NumberFormatException var11) {
                            }
                        }
                        break;
                    case "IsSurfaceOffset":
                        this.surfaceFlags = (byte)(this.surfaceFlags | 2);
                        break;
                    case "IsTable":
                        this.surfaceFlags = (byte)(this.surfaceFlags | 4);
                        break;
                    case "IsTableTop":
                        this.surfaceFlags = (byte)(this.surfaceFlags | 8);
                        break;
                    case "StackReplaceTileOffset":
                        try {
                            this.stackReplaceTileOffset = (short)Integer.parseInt(val);
                        } catch (NumberFormatException var10) {
                        }
                        break;
                    case "ItemHeight":
                        try {
                            int pixels = Integer.parseInt(val);
                            if (pixels >= 0 && pixels <= 127) {
                                this.itemHeight = (byte)pixels;
                            }
                        } catch (NumberFormatException var9) {
                        }
                        break;
                    case "SlopedSurfaceDirection":
                        this.slopedSurfaceDirection = IsoDirections.fromString(val);
                        break;
                    case "SlopedSurfaceHeightMin":
                        this.slopedSurfaceHeightMin = (byte)PZMath.clamp(PZMath.tryParseInt(val, 0), 0, 100);
                        break;
                    case "SlopedSurfaceHeightMax":
                        this.slopedSurfaceHeightMax = (byte)PZMath.clamp(PZMath.tryParseInt(val, 0), 0, 100);
                }

                return true;
            });
        }
    }

    public int getSurface() {
        this.initSurface();
        return this.surface;
    }

    public boolean isSurfaceOffset() {
        this.initSurface();
        return (this.surfaceFlags & 2) != 0;
    }

    public boolean isTable() {
        this.initSurface();
        return (this.surfaceFlags & 4) != 0;
    }

    public boolean isTableTop() {
        this.initSurface();
        return (this.surfaceFlags & 8) != 0;
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

    public static class MostTested {
        public IsoFlagType flag;
        public int count;
    }

    private static class ProfileEntryComparitor implements Comparator<Object> {
        public ProfileEntryComparitor() {
        }

        @Override
        public int compare(Object o1, Object o2) {
            double dist1 = ((PropertyContainer.MostTested)o1).count;
            double dist2 = ((PropertyContainer.MostTested)o2).count;
            if (dist1 > dist2) {
                return -1;
            } else {
                return dist2 > dist1 ? 1 : 0;
            }
        }
    }
}
