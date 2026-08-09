// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.ServerMap;
import zombie.network.fields.character.PlayerID;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public class ContainerID implements INetworkPacketField {
    @JSONField
    public final PlayerID playerId = new PlayerID();
    @JSONField
    public ContainerID.ContainerType containerType = ContainerID.ContainerType.Undefined;
    @JSONField
    public int x;
    @JSONField
    public int y;
    @JSONField
    public byte z;
    @JSONField
    short index = -1;
    @JSONField
    short containerIndex = -1;
    @JSONField
    short vid = -1;
    @JSONField
    public int worldItemId = -1;
    @JSONField
    public int[] floorXY;
    ItemContainer container;
    IsoObject object;

    public ContainerID.ContainerType getContainerType() {
        return this.containerType;
    }

    public void set(ItemContainer container) {
        if (container == null) {
            this.containerType = ContainerID.ContainerType.Undefined;
        } else {
            IsoObject o = container.getParent();
            if (!(o instanceof IsoPlayer)) {
                if (container.getContainingItem() != null && container.getContainingItem().getWorldItem() != null) {
                    o = container.getContainingItem().getWorldItem();
                }

                if (container.containingItem != null && container.containingItem.getOutermostContainer() != null) {
                    ItemContainer outermost = container.containingItem.getOutermostContainer();
                    o = outermost.getParent();
                    if (o instanceof IsoPlayer player) {
                        this.setInventoryContainer(container, player);
                    } else if (o instanceof BaseVehicle) {
                        this.setObjectInVehicle(container, o, o.square, outermost);
                    } else {
                        this.setObject(container, o, o.square);
                    }

                    return;
                }
            }

            if (o == null) {
                ItemContainer floorContainer = this.getFloorContainer(container);
                if (floorContainer == null) {
                    throw new IllegalStateException("Unable to resolve container location.");
                } else {
                    IsoGridSquare floorSquare = this.getFloorSquare(floorContainer);
                    if (floorSquare != null) {
                        this.setFloor(floorContainer, floorSquare);
                    }
                }
            } else {
                this.set(container, o);
            }
        }
    }

    public void copy(ContainerID other) {
        this.containerType = other.containerType;
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.floorXY = other.floorXY == null ? null : Arrays.copyOf(other.floorXY, other.floorXY.length);
        this.index = other.index;
        this.containerIndex = other.containerIndex;
        this.vid = other.vid;
        this.worldItemId = other.worldItemId;
        this.playerId.copy(other.playerId);
        this.container = other.container;
        this.object = other.object;
    }

    public void setFloor(ItemContainer container, IsoGridSquare sq) {
        this.x = sq.getX();
        this.y = sq.getY();
        this.z = (byte)sq.getZ();
        this.calculateFloorXY(container);
        this.containerType = ContainerID.ContainerType.Floor;
        this.container = container;
    }

    public void setObject(ItemContainer container, IsoObject o, IsoGridSquare sq) {
        this.x = sq.getX();
        this.y = sq.getY();
        this.z = (byte)sq.getZ();
        this.container = container;
        if (o != null) {
            this.containerType = ContainerID.ContainerType.ObjectContainer;
            this.index = (short)o.square.getObjects().indexOf(o);
            this.containerIndex = (short)o.getContainerIndex(container);
        } else {
            this.containerType = ContainerID.ContainerType.IsoObject;
            this.index = -1;
            this.containerIndex = -1;
        }
    }

    public void setObjectInVehicle(ItemContainer container, IsoObject o, IsoGridSquare sq, ItemContainer part) {
        this.x = sq.getX();
        this.y = sq.getY();
        this.z = (byte)sq.getZ();
        this.container = container;
        this.containerType = ContainerID.ContainerType.ObjectInVehicle;
        this.index = (short)part.vehiclePart.getIndex();
        this.worldItemId = container.getContainingItem() == null ? -1 : container.getContainingItem().getID();
        this.vid = ((BaseVehicle)o).vehicleId;
    }

    public void setInventoryContainer(ItemContainer container, IsoPlayer player) {
        this.x = player.square.getX();
        this.y = player.square.getY();
        this.z = (byte)player.square.getZ();
        this.containerType = ContainerID.ContainerType.InventoryContainer;
        this.playerId.set(player);
        this.worldItemId = container.containingItem.id;
        this.container = container;
    }

    public void set(ItemContainer container, IsoObject o) {
        if (o.square != null) {
            this.x = o.square.getX();
            this.y = o.square.getY();
            this.z = (byte)o.square.getZ();
        }

        if (o instanceof IsoDeadBody) {
            this.containerType = ContainerID.ContainerType.DeadBody;
            this.index = (short)o.getStaticMovingObjectIndex();
        } else if (o instanceof IsoWorldInventoryObject isoWorldInventoryObject) {
            this.containerType = ContainerID.ContainerType.WorldObject;
            this.worldItemId = isoWorldInventoryObject.getItem().id;
        } else if (o instanceof BaseVehicle baseVehicle) {
            this.containerType = ContainerID.ContainerType.Vehicle;
            this.vid = baseVehicle.vehicleId;
            this.index = (short)container.vehiclePart.getIndex();
        } else if (o instanceof IsoPlayer isoPlayer) {
            this.containerType = ContainerID.ContainerType.PlayerInventory;
            this.playerId.set(isoPlayer);
        } else if (o.getContainerIndex(container) != -1) {
            this.containerType = ContainerID.ContainerType.ObjectContainer;
            this.index = (short)o.square.getObjects().indexOf(o);
            this.containerIndex = (short)o.getContainerIndex(container);
        } else {
            this.containerType = ContainerID.ContainerType.IsoObject;
            this.index = (short)o.square.getObjects().indexOf(o);
            this.containerIndex = -1;
        }

        this.container = container;
    }

    public boolean isContainerTheSame(int itemId, ItemContainer source) {
        if (!"floor".equals(source.getType())) {
            return this.container == source;
        } else {
            if (ContainerID.ContainerType.WorldObject == this.containerType) {
                for (InventoryItem item : source.getItems()) {
                    if (item.id == this.worldItemId) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public ItemContainer getContainer() {
        return this.container;
    }

    public IsoObject getObject() {
        return this.object;
    }

    public VehiclePart getPart() {
        if (this.containerType != ContainerID.ContainerType.Vehicle) {
            return null;
        } else {
            BaseVehicle vehicle = VehicleManager.instance.getVehicleByID(this.vid);
            return vehicle == null ? null : vehicle.getPartByIndex(this.index);
        }
    }

    public BaseVehicle getVehicle() {
        return this.containerType != ContainerID.ContainerType.Vehicle ? null : VehicleManager.instance.getVehicleByID(this.vid);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.container = null;
        this.floorXY = null;
        this.containerType = b.getEnum(ContainerID.ContainerType.class);
        if (this.containerType != ContainerID.ContainerType.Undefined) {
            if (this.containerType == ContainerID.ContainerType.PlayerInventory) {
                this.playerId.parse(b, connection);
            } else if (this.containerType == ContainerID.ContainerType.InventoryContainer) {
                this.playerId.parse(b, connection);
                this.worldItemId = b.getInt();
            } else {
                this.x = b.getInt();
                this.y = b.getInt();
                this.z = b.getByte();
                if (this.containerType == ContainerID.ContainerType.DeadBody) {
                    this.index = b.getShort();
                } else if (this.containerType == ContainerID.ContainerType.WorldObject) {
                    this.worldItemId = b.getInt();
                } else if (this.containerType == ContainerID.ContainerType.IsoObject) {
                    this.index = b.getShort();
                    this.containerIndex = -1;
                } else if (this.containerType == ContainerID.ContainerType.ObjectContainer) {
                    this.index = b.getShort();
                    this.containerIndex = b.getShort();
                } else if (this.containerType == ContainerID.ContainerType.Vehicle) {
                    this.vid = b.getShort();
                    this.index = b.getShort();
                } else if (this.containerType == ContainerID.ContainerType.ObjectInVehicle) {
                    this.vid = b.getShort();
                    this.index = b.getShort();
                    this.worldItemId = b.getInt();
                } else if (this.containerType == ContainerID.ContainerType.Floor) {
                    int squareCount = b.getByte();
                    if (squareCount > 0) {
                        this.floorXY = new int[squareCount * 2];

                        for (int i = 0; i < squareCount; i++) {
                            this.floorXY[i * 2] = b.getInt();
                            this.floorXY[i * 2 + 1] = b.getInt();
                        }
                    }
                }
            }

            this.findObject();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putEnum(this.containerType);
        if (this.containerType != ContainerID.ContainerType.Undefined) {
            if (this.containerType == ContainerID.ContainerType.PlayerInventory) {
                this.playerId.write(b);
            } else if (this.containerType == ContainerID.ContainerType.InventoryContainer) {
                this.playerId.write(b);
                b.putInt(this.worldItemId);
            } else {
                b.putInt(this.x);
                b.putInt(this.y);
                b.putByte(this.z);
                if (this.containerType == ContainerID.ContainerType.Floor) {
                    if (this.floorXY != null && this.floorXY.length != 2) {
                        b.putByte((byte)(this.floorXY.length / 2));

                        for (int i = 0; i < this.floorXY.length; i++) {
                            b.putInt(this.floorXY[i]);
                        }
                    } else {
                        b.putByte((byte)0);
                    }
                }

                if (this.containerType == ContainerID.ContainerType.DeadBody) {
                    b.putShort(this.index);
                } else if (this.containerType == ContainerID.ContainerType.WorldObject) {
                    b.putInt(this.worldItemId);
                } else if (this.containerType == ContainerID.ContainerType.Vehicle) {
                    b.putShort(this.vid);
                    b.putShort(this.index);
                } else if (this.containerType == ContainerID.ContainerType.IsoObject) {
                    b.putShort(this.index);
                } else if (this.containerType == ContainerID.ContainerType.ObjectContainer) {
                    b.putShort(this.index);
                    b.putShort(this.containerIndex);
                } else if (this.containerType == ContainerID.ContainerType.ObjectInVehicle) {
                    b.putShort(this.vid);
                    b.putShort(this.index);
                    b.putInt(this.worldItemId);
                }
            }
        }
    }

    public void write(ByteBuffer bb) {
        bb.put((byte)this.containerType.ordinal());
        if (this.containerType != ContainerID.ContainerType.Undefined) {
            if (this.containerType == ContainerID.ContainerType.PlayerInventory) {
                this.playerId.write(bb);
            } else if (this.containerType == ContainerID.ContainerType.InventoryContainer) {
                this.playerId.write(bb);
                bb.putInt(this.worldItemId);
            } else {
                bb.putInt(this.x);
                bb.putInt(this.y);
                bb.put(this.z);
                if (this.containerType == ContainerID.ContainerType.Floor) {
                    if (this.floorXY != null && this.floorXY.length != 2) {
                        bb.put((byte)(this.floorXY.length / 2));

                        for (int i = 0; i < this.floorXY.length; i++) {
                            bb.putInt(this.floorXY[i]);
                        }
                    } else {
                        bb.put((byte)0);
                    }
                }

                if (this.containerType == ContainerID.ContainerType.DeadBody) {
                    bb.putShort(this.index);
                } else if (this.containerType == ContainerID.ContainerType.WorldObject) {
                    bb.putInt(this.worldItemId);
                } else if (this.containerType == ContainerID.ContainerType.Vehicle) {
                    bb.putShort(this.vid);
                    bb.putShort(this.index);
                } else if (this.containerType == ContainerID.ContainerType.IsoObject) {
                    bb.putShort(this.index);
                } else if (this.containerType == ContainerID.ContainerType.ObjectContainer) {
                    bb.putShort(this.index);
                    bb.putShort(this.containerIndex);
                } else if (this.containerType == ContainerID.ContainerType.ObjectInVehicle) {
                    bb.putShort(this.vid);
                    bb.putShort(this.index);
                    bb.putInt(this.worldItemId);
                }
            }
        }
    }

    public void findObject() {
        if (this.containerType == ContainerID.ContainerType.PlayerInventory) {
            this.container = this.playerId.getPlayer().getInventory();
        } else if (this.containerType == ContainerID.ContainerType.InventoryContainer) {
            ItemContainer playerContainer = this.playerId.getPlayer().getInventory();
            InventoryContainer containingItem = (InventoryContainer)playerContainer.getItemWithIDRecursiv(this.worldItemId);
            this.container = containingItem.getItemContainer();
        } else if (IsoWorld.instance.currentCell != null) {
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
            if (GameServer.server && sq == null) {
                sq = ServerMap.instance.getGridSquare(this.x, this.y, this.z);
            }

            if (sq != null) {
                this.container = null;
                if (this.containerType == ContainerID.ContainerType.Floor) {
                    this.object = new IsoWorldInventoryObject(null, sq, 0.0F, 0.0F, 0.0F);
                    this.container = new ItemContainer("floor", sq, null);
                    if (this.floorXY == null) {
                        this.collectFloorItems(sq);
                    } else {
                        for (int i = 0; i < this.floorXY.length; i += 2) {
                            int floorX = this.floorXY[i];
                            int floorY = this.floorXY[i + 1];
                            IsoGridSquare floorSq = IsoWorld.instance.currentCell.getGridSquare(floorX, floorY, this.z);
                            this.collectFloorItems(floorSq);
                        }
                    }
                } else if (this.containerType == ContainerID.ContainerType.DeadBody) {
                    if (this.index < 0 || this.index >= sq.getStaticMovingObjects().size()) {
                        DebugLog.log("ERROR: ContainerID: invalid corpse index");
                        return;
                    }

                    this.object = sq.getStaticMovingObjects().get(this.index);
                    if (this.object != null && this.object.getContainer() != null) {
                        this.container = this.object.getContainer();
                    }
                } else if (this.containerType == ContainerID.ContainerType.WorldObject) {
                    for (int i = 0; i < sq.getWorldObjects().size(); i++) {
                        IsoWorldInventoryObject wo = sq.getWorldObjects().get(i);
                        if (wo != null && wo.getItem() != null && wo.getItem().id == this.worldItemId) {
                            this.object = wo;
                            InventoryItem item = wo.getItem();
                            if (item instanceof InventoryContainer inventoryContainer) {
                                this.container = inventoryContainer.getItemContainer();
                            } else {
                                this.container = new ItemContainer("floor", sq, null);
                                this.container.getItems().add(item);
                            }
                            break;
                        }
                    }

                    if (this.container == null) {
                        DebugLog.log("ERROR: sendItemsToContainer: can't find world item with id=" + this.worldItemId);
                    }
                } else if (this.containerType == ContainerID.ContainerType.IsoObject) {
                    if (this.index >= 0 && this.index < sq.getObjects().size()) {
                        this.object = sq.getObjects().get(this.index);
                        this.container = this.object != null ? this.object.getContainerByIndex(this.containerIndex) : null;
                    } else {
                        this.object = new IsoObject();
                        this.object.setSquare(sq);
                        this.container = new ItemContainer("object", sq, this.object);
                    }
                } else if (this.containerType == ContainerID.ContainerType.ObjectContainer) {
                    if (this.index >= 0 && this.index < sq.getObjects().size()) {
                        this.object = sq.getObjects().get(this.index);
                        this.container = this.object != null ? this.object.getContainerByIndex(this.containerIndex) : null;
                    } else {
                        this.object = new IsoObject();
                        this.object.setSquare(sq);
                    }
                } else if (this.containerType == ContainerID.ContainerType.Vehicle) {
                    BaseVehicle vehicle = VehicleManager.instance.getVehicleByID(this.vid);
                    if (vehicle == null) {
                        DebugLog.log("ERROR: sendItemsToContainer: invalid vehicle id");
                    } else {
                        VehiclePart part = vehicle.getPartByIndex(this.index);
                        if (part == null) {
                            DebugLog.log("ERROR: sendItemsToContainer: invalid part index");
                        } else {
                            this.object = vehicle;
                            this.container = part.getItemContainer();
                            if (this.container == null) {
                                DebugLog.log("ERROR: sendItemsToContainer: part " + part.getId() + " has no container");
                            }
                        }
                    }
                } else if (this.containerType == ContainerID.ContainerType.ObjectInVehicle) {
                    BaseVehicle vehicle = VehicleManager.instance.getVehicleByID(this.vid);
                    if (vehicle == null) {
                        DebugLog.log("ERROR: sendItemsToContainer: invalid vehicle id");
                    } else {
                        VehiclePart part = vehicle.getPartByIndex(this.index);
                        if (part == null) {
                            DebugLog.log("ERROR: sendItemsToContainer: invalid part index");
                        } else {
                            InventoryItem item = part.getItemContainer().getItemWithID(this.worldItemId);
                            if (!(item instanceof InventoryContainer inventoryContainer)) {
                                DebugLog.log("ERROR: sendItemsToContainer: invalid item id=" + this.worldItemId);
                                return;
                            }

                            this.container = inventoryContainer.getItemContainer();
                            if (this.container == null) {
                                DebugLog.log("ERROR: sendItemsToContainer: item " + item.getID() + " has no container");
                            }
                        }
                    }
                } else {
                    DebugLog.log("ERROR: sendItemsToContainer: unknown container type");
                }
            } else if (GameClient.client) {
                GameClient.instance.delayPacket(this.x, this.y, this.z);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ContainerID that = (ContainerID)o;
            return this.containerType != ContainerID.ContainerType.InventoryContainer && this.containerType != ContainerID.ContainerType.PlayerInventory
                ? this.containerType == that.containerType
                    && this.x == that.x
                    && this.y == that.y
                    && this.z == that.z
                    && Arrays.equals(this.floorXY, that.floorXY)
                    && this.index == that.index
                    && this.containerIndex == that.containerIndex
                    && this.vid == that.vid
                    && this.worldItemId == that.worldItemId
                : this.containerType == that.containerType && this.playerId.getID() == that.playerId.getID() && this.worldItemId == that.worldItemId;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            this.containerType, this.x, this.y, this.z, Arrays.hashCode(this.floorXY), this.index, this.containerIndex, this.vid, this.worldItemId
        );
    }

    @Override
    public String toString() {
        return "" + this.containerType + this.hashCode();
    }

    private boolean contains(int[] floorXY, int x, int y) {
        for (int i = 0; i < floorXY.length; i += 2) {
            if (floorXY[i] == x && floorXY[i + 1] == y) {
                return true;
            }
        }

        return false;
    }

    private int[] addFloorXY(int x, int y, int[] floorXY) {
        if (floorXY == null) {
            return new int[]{x, y};
        } else if (this.contains(floorXY, x, y)) {
            return floorXY;
        } else {
            int[] floorXYcopy = Arrays.copyOf(floorXY, floorXY.length + 2);
            floorXYcopy[floorXY.length] = x;
            floorXYcopy[floorXY.length + 1] = y;
            return floorXYcopy;
        }
    }

    private void calculateFloorXY(ItemContainer container) {
        this.floorXY = null;

        for (int n = 0; n < container.getItems().size(); n++) {
            InventoryItem inventoryItem = container.getItems().get(n);
            IsoWorldInventoryObject worldItem = inventoryItem.getWorldItem();
            if (worldItem != null && worldItem.getSquare() != null) {
                this.floorXY = this.addFloorXY(worldItem.getSquare().getX(), worldItem.getSquare().getY(), this.floorXY);
            }
        }
    }

    private void collectFloorItems(IsoGridSquare sq) {
        if (sq != null) {
            for (int i = 0; i < sq.getWorldObjects().size(); i++) {
                IsoWorldInventoryObject wo = sq.getWorldObjects().get(i);
                if (wo != null && wo.getItem() != null) {
                    this.container.getItems().add(wo.getItem());
                }
            }
        }
    }

    private ItemContainer getFloorContainer(ItemContainer container) {
        if (container.getType().equals("floor")) {
            return container;
        } else {
            return container.getOutermostContainer().getType().equals("floor") ? container.getOutermostContainer() : null;
        }
    }

    private IsoGridSquare getFloorSquare(ItemContainer floorContainer) {
        for (int n = 0; n < floorContainer.getItems().size(); n++) {
            InventoryItem inventoryItem = floorContainer.getItems().get(n);
            if (inventoryItem.getWorldItem() != null && inventoryItem.getWorldItem().getSquare() != null) {
                return inventoryItem.getWorldItem().getSquare();
            }
        }

        return null;
    }

    @UsedFromLua
    public static enum ContainerType {
        Undefined,
        DeadBody,
        WorldObject,
        IsoObject,
        ObjectContainer,
        ObjectInVehicle,
        Vehicle,
        PlayerInventory,
        InventoryContainer,
        Floor;
    }
}
