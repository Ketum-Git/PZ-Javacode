// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.MapCollisionData;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.Core;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Radio;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.pathfind.PolygonalMap2;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class RemoveItemFromSquarePacket implements INetworkPacket {
    @JSONField
    public int x;
    @JSONField
    public int y;
    @JSONField
    byte z;
    @JSONField
    public short index;

    @Override
    public void setData(Object... values) {
        IsoObject obj = (IsoObject)values[0];
        this.x = obj.getSquare().getX();
        this.y = obj.getSquare().getY();
        this.z = (byte)obj.getSquare().getZ();
        this.index = (short)obj.getObjectIndex();
    }

    public void set(IsoObject obj) {
        this.x = obj.getSquare().getX();
        this.y = obj.getSquare().getY();
        this.z = (byte)obj.getSquare().getZ();
        this.index = (short)obj.getObjectIndex();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
        b.putShort(this.index);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.get();
        this.index = b.getShort();
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (IsoWorld.instance.currentCell != null) {
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
            if (sq == null) {
                GameClient.instance.delayPacket(this.x, this.y, this.z);
            } else {
                if (this.index >= 0 && this.index < sq.getObjects().size()) {
                    IsoObject o = sq.getObjects().get(this.index);
                    handleRemoveRadio(o, sq);
                    sq.RemoveTileObject(o, false);
                    if (o instanceof IsoWorldInventoryObject isoWorldInventoryObject && isoWorldInventoryObject.getItem() != null) {
                        isoWorldInventoryObject.getItem().setWorldItem(null);
                    }

                    if (o instanceof IsoWorldInventoryObject || o.getContainer() != null) {
                        LuaEventManager.triggerEvent("OnContainerUpdate", o);
                    }
                } else if (Core.debug) {
                    DebugLog.log("RemoveItemFromSquare: sq is null or index is invalid %d,%d,%d index=%d".formatted(this.x, this.y, this.z, this.index));
                }
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        removeItemFromMap(connection, this.x, this.y, this.z, this.index);
        this.sendToRelativeClients(PacketTypes.PacketType.RemoveItemFromSquare, connection, this.x, this.y);
    }

    public static void removeItemFromMap(UdpConnection connection, int x, int y, int z, int index) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (sq != null && index >= 0 && index < sq.getObjects().size()) {
            IsoObject o = sq.getObjects().get(index);
            if (!(o instanceof IsoWorldInventoryObject)) {
                IsoRegions.setPreviousFlags(sq);
            }

            DebugLog.log(DebugType.Objects, "object: removing " + o + " index=" + index + " " + x + "," + y + "," + z);
            if (o instanceof IsoWorldInventoryObject isoWorldInventoryObject) {
                handleRemoveRadio(o, sq);
                if (connection != null) {
                    LoggerManager.getLogger("item")
                        .write(
                            connection.idStr
                                + " \""
                                + connection.username
                                + "\" floor -1 "
                                + x
                                + ","
                                + y
                                + ","
                                + z
                                + " ["
                                + isoWorldInventoryObject.getItem().getFullType()
                                + "]"
                        );
                }
            } else {
                String name = o.getName() != null ? o.getName() : o.getObjectName();
                if (o.getSprite() != null && o.getSprite().getName() != null) {
                    name = name + " (" + o.getSprite().getName() + ")";
                }

                if (connection != null) {
                    LoggerManager.getLogger("map")
                        .write(connection.idStr + " \"" + connection.username + "\" removed " + name + " at " + x + "," + y + "," + z);
                }
            }

            if (o.isTableSurface()) {
                for (int i = index + 1; i < sq.getObjects().size(); i++) {
                    IsoObject object = sq.getObjects().get(i);
                    if (object.isTableTopObject() || object.isTableSurface()) {
                        object.setRenderYOffset(object.getRenderYOffset() - o.getSurfaceOffset());
                    }
                }
            }

            if (!(o instanceof IsoWorldInventoryObject)) {
                LuaEventManager.triggerEvent("OnObjectAboutToBeRemoved", o);
            }

            if (!sq.getObjects().contains(o)) {
                throw new IllegalArgumentException("OnObjectAboutToBeRemoved not allowed to remove the object");
            }

            o.removeFromWorld();
            o.removeFromSquare();
            sq.RecalcAllWithNeighbours(true);
            if (!(o instanceof IsoWorldInventoryObject)) {
                IsoWorld.instance.currentCell.checkHaveRoof(x, y);
                MapCollisionData.instance.squareChanged(sq);
                PolygonalMap2.instance.squareChanged(sq);
                ServerMap.instance.physicsCheck(x, y);
                IsoRegions.squareChanged(sq, true);
                IsoGenerator.updateGenerator(sq);
            }
        }
    }

    private static void handleRemoveRadio(IsoObject o, IsoGridSquare sq) {
        if (o instanceof IsoWorldInventoryObject isoWorldInventoryObject) {
            InventoryItem invItem = isoWorldInventoryObject.getItem();
            if (invItem != null && invItem instanceof Radio) {
                for (int i = sq.getObjects().size() - 1; i >= 0; i--) {
                    IsoObject objI = sq.getObjects().get(i);
                    if (objI instanceof IsoRadio && !objI.getModData().isEmpty()) {
                        Object idRadio = objI.getModData().rawget("RadioItemID");
                        if (idRadio != null && idRadio instanceof Double d && d.intValue() == invItem.getID()) {
                            sq.transmitRemoveItemFromSquare(objI);
                            sq.RecalcAllWithNeighbours(true);
                            break;
                        }
                    }
                }
            }
        }
    }
}
