// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TShortObjectHashMap;
import java.util.LinkedList;
import java.util.List;
import zombie.core.math.PZMath;

public final class VehicleCache {
    public short id;
    float x;
    float y;
    float z;
    private static final TShortObjectHashMap<VehicleCache> mapId = new TShortObjectHashMap<>();
    private static final TIntObjectHashMap<List<VehicleCache>> mapXY = new TIntObjectHashMap<>();

    public static void vehicleUpdate(short id, float ax, float ay, float az) {
        VehicleCache v = mapId.get(id);
        if (v != null) {
            int vehicle_wx = PZMath.fastfloor(v.x / 8.0F);
            int vehicle_wy = PZMath.fastfloor(v.y / 8.0F);
            int wx = PZMath.fastfloor(ax / 8.0F);
            int wy = PZMath.fastfloor(ay / 8.0F);
            if (vehicle_wx != wx || vehicle_wy != wy) {
                mapXY.get(vehicle_wx * 65536 + vehicle_wy).remove(v);
                if (mapXY.get(wx * 65536 + wy) == null) {
                    mapXY.put(wx * 65536 + wy, new LinkedList<>());
                }

                mapXY.get(wx * 65536 + wy).add(v);
            }

            v.x = ax;
            v.y = ay;
            v.z = az;
        } else {
            VehicleCache v2 = new VehicleCache();
            v2.id = id;
            v2.x = ax;
            v2.y = ay;
            v2.z = az;
            mapId.put(id, v2);
            int wx = PZMath.fastfloor(ax / 8.0F);
            int wy = PZMath.fastfloor(ay / 8.0F);
            if (mapXY.get(wx * 65536 + wy) == null) {
                mapXY.put(wx * 65536 + wy, new LinkedList<>());
            }

            mapXY.get(wx * 65536 + wy).add(v2);
        }
    }

    public static List<VehicleCache> vehicleGet(float ax, float ay) {
        int wx = PZMath.fastfloor(ax / 8.0F);
        int wy = PZMath.fastfloor(ay / 8.0F);
        return mapXY.get(wx * 65536 + wy);
    }

    public static List<VehicleCache> vehicleGet(int wx, int wy) {
        return mapXY.get(wx * 65536 + wy);
    }

    public static void remove(short id) {
        VehicleCache v = mapId.get(id);
        if (v != null) {
            mapId.remove(id);
            int vehicle_wx = PZMath.fastfloor(v.x / 8.0F);
            int vehicle_wy = PZMath.fastfloor(v.y / 8.0F);
            int key = vehicle_wx * 65536 + vehicle_wy;

            assert mapXY.containsKey(key);

            assert mapXY.get(key).contains(v);

            mapXY.get(key).remove(v);
        }
    }

    public static void Reset() {
        mapId.clear();
        mapXY.clear();
    }

    static {
        mapId.setAutoCompactionFactor(0.0F);
        mapXY.setAutoCompactionFactor(0.0F);
    }
}
