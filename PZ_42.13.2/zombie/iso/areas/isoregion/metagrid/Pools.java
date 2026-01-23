// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion.metagrid;

import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.popman.ObjectPool;

public final class Pools {
    public static final ObjectPool<BuildingDef> buildingDef = new ObjectPool<>(BuildingDef::new);
    public static final ObjectPool<RoomDef> roomDef = new ObjectPool<>(RoomDef::new);
    public static final ObjectPool<RoomDef.RoomRect> roomRect = new ObjectPool<>(RoomDef.RoomRect::new);
}
