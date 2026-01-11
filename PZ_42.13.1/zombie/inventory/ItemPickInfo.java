// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import java.util.ArrayList;
import java.util.Objects;
import zombie.GameTime;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.IsoRoom;
import zombie.iso.zones.Zone;
import zombie.scripting.itemConfig.SelectorBucket;
import zombie.scripting.itemConfig.enums.SelectorType;
import zombie.scripting.itemConfig.enums.SituatedType;
import zombie.scripting.objects.VehicleScript;
import zombie.util.StringUtils;

public class ItemPickInfo {
    private ItemPickerJava.ItemPickerRoom roomDist;
    private ItemContainer itemContainer;
    private int worldAgeDays;
    private boolean exterior;
    private boolean isJunk;
    private int roomId = -1;
    private int containerId = -1;
    private int vehicleId = -1;
    private final int[] zones = new int[32];
    private int zoneCount;
    private final int[] tiles = new int[16];
    private int tileCount;
    private float resultValue;
    private static final ItemPickInfo instance = new ItemPickInfo();

    private ItemPickInfo() {
    }

    private void reset() {
        this.worldAgeDays = -1;
        this.exterior = true;
        this.isJunk = false;
        this.roomId = -1;
        this.containerId = -1;
        this.vehicleId = -1;
        this.zoneCount = 0;
        this.tileCount = 0;
        this.resultValue = 0.0F;
        this.roomDist = null;
        this.itemContainer = null;
    }

    protected boolean isShop() {
        return this.roomDist != null && this.roomDist.isShop;
    }

    protected boolean isJunk() {
        return this.isJunk;
    }

    protected void setJunk(boolean b) {
        this.isJunk = b;
    }

    protected void updateRoomDist(ItemPickerJava.ItemPickerRoom roomDist) {
        if (roomDist != null) {
            this.roomDist = roomDist;
        }
    }

    public boolean isMatch(SelectorBucket selectorBucket) {
        if (selectorBucket.getSelectorType() != SelectorType.Default && selectorBucket.getSelectorType() != SelectorType.None) {
            switch (selectorBucket.getSelectorType()) {
                case Container:
                    return selectorBucket.containsSelectorID(this.containerId);
                case Room:
                    return selectorBucket.containsSelectorID(this.roomId);
                case Zone:
                    if (selectorBucket.hasSelectorIDs()) {
                        for (int ix = 0; ix < this.zoneCount; ix++) {
                            if (selectorBucket.containsSelectorID(this.zones[ix])) {
                                return true;
                            }
                        }
                    }
                    break;
                case Situated:
                    if (selectorBucket.getSelectorSituated() == SituatedType.Exterior) {
                        return this.exterior;
                    }

                    if (selectorBucket.getSelectorSituated() == SituatedType.Interior) {
                        return !this.exterior;
                    }

                    if (selectorBucket.getSelectorSituated() == SituatedType.Shop) {
                        return this.isShop();
                    }

                    if (selectorBucket.getSelectorSituated() == SituatedType.Junk) {
                        return this.isJunk();
                    }
                    break;
                case WorldAge:
                    return this.worldAgeDays >= selectorBucket.getSelectorWorldAge();
                case Tile:
                    if (selectorBucket.hasSelectorIDs()) {
                        for (int i = 0; i < this.tileCount; i++) {
                            if (selectorBucket.containsSelectorID(this.tiles[i])) {
                                return true;
                            }
                        }
                    }
                    break;
                case Vehicle:
                    return selectorBucket.containsSelectorID(this.vehicleId);
            }

            return false;
        } else {
            return true;
        }
    }

    public void setResultValue(float f) {
        this.resultValue = f;
    }

    public float getResultValue() {
        return this.resultValue;
    }

    private static ItemPickerJava.ItemPickerRoom getRoomDist(IsoGridSquare sq, ItemContainer container) {
        IsoRoom room = sq.getRoom();
        if (room == null) {
            return null;
        } else {
            ItemPickerJava.ItemPickerRoom roomDist = null;
            if (ItemPickerJava.rooms.containsKey("all")) {
                roomDist = ItemPickerJava.rooms.get("all");
            }

            if (ItemPickerJava.rooms.containsKey(room.getName())) {
                roomDist = ItemPickerJava.rooms.get(room.getName());
            }

            return roomDist;
        }
    }

    public static ItemPickInfo GetPickInfo(ItemContainer container, ItemPickInfo.Caller caller) {
        instance.reset();
        if (container == null) {
            if (Core.debug) {
                DebugLog.log("ItemPickInfo -> unable to set pick info: container == null, Caller: " + caller);
            }

            return null;
        } else {
            instance.itemContainer = container;
            instance.worldAgeDays = (int)(GameTime.instance.getWorldAgeHours() / 24.0);
            String container_name = container.getType();
            if (container_name != null) {
                instance.containerId = ItemConfigurator.GetIdForString(container_name);
                if (instance.containerId == -1) {
                    DebugLog.log("ItemPickInfo -> cannot get ID for container: " + container_name);
                }
            }

            ItemContainer outerContainer = container.getOutermostContainer();
            IsoGridSquare sq = null;
            if (outerContainer.getVehiclePart() != null && outerContainer.getVehiclePart().getVehicle() != null) {
                sq = outerContainer.getVehiclePart().getVehicle().getSquare();
                VehicleScript script = outerContainer.getVehiclePart().getVehicle().getScript();
                if (script != null && script.getName() != null) {
                    instance.vehicleId = ItemConfigurator.GetIdForString(script.getName());
                }
            }

            if (sq == null) {
                sq = outerContainer.getSquare();
            }

            if (sq == null && Objects.equals(outerContainer.type, "floor")) {
            }

            if (sq != null) {
                instance.roomDist = getRoomDist(sq, container);
                instance.exterior = !sq.isInARoom();
                String room_name = sq.getRoom() != null ? sq.getRoom().getName() : null;
                if (room_name != null) {
                    instance.roomId = ItemConfigurator.GetIdForString(room_name);
                    if (instance.roomId == -1) {
                        DebugLog.log("ItemPickInfo -> cannot get ID for room: " + room_name);
                    }
                }

                for (int i = 0; i < sq.getObjects().size(); i++) {
                    IsoObject obj = sq.getObjects().get(i);
                    if (obj.getSprite() != null && obj.getSprite().getID() >= 0 && obj.getSprite().getName() != null) {
                        int tileID = obj.getSprite().getID();
                        if (tileID != -1) {
                            instance.tiles[instance.tileCount++] = tileID;
                            if (instance.tileCount >= instance.tiles.length) {
                                break;
                            }
                        } else {
                            DebugLog.log("ItemPickInfo -> cannot get ID for tile: " + obj.getSprite().getName());
                        }
                    }
                }

                ArrayList<Zone> metazones = IsoWorld.instance.metaGrid.getZonesAt(sq.x, sq.y, 0);

                for (int ix = 0; ix < metazones.size(); ix++) {
                    Zone zone = metazones.get(ix);
                    if (zone.type != null && !StringUtils.isNullOrWhitespace(zone.type)) {
                        int zoneID = ItemConfigurator.GetIdForString(zone.type);
                        if (zoneID >= 0) {
                            instance.zones[instance.zoneCount++] = zoneID;
                            if (instance.zoneCount >= instance.zones.length) {
                                break;
                            }
                        } else {
                            DebugLog.log("ItemPickInfo -> cannot get ID for zone: " + zone.type);
                        }
                    }

                    if (zone.name != null && !StringUtils.isNullOrWhitespace(zone.name)) {
                        int zoneID = ItemConfigurator.GetIdForString(zone.name);
                        if (zoneID >= 0) {
                            instance.zones[instance.zoneCount++] = zoneID;
                            if (instance.zoneCount >= instance.zones.length) {
                                break;
                            }
                        } else {
                            DebugLog.log("ItemPickInfo -> cannot get ID for zone: " + zone.name);
                        }
                    }
                }
            } else if (Core.debug && caller != ItemPickInfo.Caller.FillContainerType) {
                DebugLog.log("ItemPickInfo -> unable to set source grid pick info, Caller: " + caller + ", for Container: " + container_name);
                if (outerContainer != container) {
                    DebugLog.log("ItemPickInfo -> Outermost Container: " + outerContainer.type);
                }

                if (container.getVehiclePart() != null && container.getVehiclePart().getVehicle() != null) {
                    DebugLog.log("ItemPickInfo -> Vehicle: " + container.getVehiclePart().getVehicle().getName());
                }
            }

            return instance;
        }
    }

    public static enum Caller {
        FillContainer,
        FillContainerType,
        RollProceduralItem,
        RollItem,
        DoRollItem,
        RollContainerItem,
        Unknown;
    }
}
