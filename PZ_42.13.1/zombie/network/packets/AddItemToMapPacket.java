// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.MapCollisionData;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoGridOcclusionData;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.ServerOptions;
import zombie.network.WorldItemTypes;
import zombie.pathfind.PolygonalMap2;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class AddItemToMapPacket implements INetworkPacket {
    @JSONField
    IsoObject obj;

    public void set(IsoObject o) {
        this.obj = o;
    }

    @Override
    public void setData(Object... values) {
        this.obj = (IsoObject)values[0];
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.obj.writeToRemoteBuffer(b);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.obj = WorldItemTypes.createFromBuffer(b);
        if (GameServer.server && this.obj instanceof IsoFire && ServerOptions.instance.noFire.getValue()) {
            DebugLog.log("user \"" + connection.username + "\" tried to start a fire");
        } else {
            this.obj.loadFromRemoteBuffer(b);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.obj.square != null) {
            if (this.obj instanceof IsoLightSwitch isoLightSwitch) {
                isoLightSwitch.addLightSourceFromSprite();
            }

            this.obj.addToWorld();
            this.obj.square.RecalcProperties();
            this.obj.square.RecalcAllWithNeighbours(true);
            IsoWorld.instance.currentCell.checkHaveRoof(this.obj.square.getX(), this.obj.square.getY());
            if (!(this.obj instanceof IsoWorldInventoryObject)) {
                for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                    LosUtil.cachecleared[pn] = true;
                }

                IsoGridSquare.setRecalcLightTime(-1.0F);
                GameTime.instance.lightSourceUpdate = 100.0F;
                MapCollisionData.instance.squareChanged(this.obj.square);
                PolygonalMap2.instance.squareChanged(this.obj.square);
                if (this.obj == this.obj.square.getPlayerBuiltFloor()) {
                    IsoGridOcclusionData.SquareChanged();
                }

                IsoGenerator.updateGenerator(this.obj.getSquare());
            }

            if (this.obj instanceof IsoFeedingTrough isoFeedingTrough) {
                isoFeedingTrough.checkOverlayAfterAnimalEat();
            }

            if (this.obj instanceof IsoWorldInventoryObject || this.obj.getContainer() != null) {
                LuaEventManager.triggerEvent("OnContainerUpdate", this.obj);
            }

            LuaEventManager.triggerEvent("OnObjectAdded", this.obj);
            if (PerformanceSettings.fboRenderChunk) {
                Core.dirtyGlobalLightsCount++;
            }

            this.obj.invalidateRenderChunkLevel(64L);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.obj.square != null) {
            DebugLog.log(
                DebugType.Objects,
                "object: added " + this.obj + " index=" + this.obj.getObjectIndex() + " " + this.obj.getX() + "," + this.obj.getY() + "," + this.obj.getZ()
            );
            if (this.obj instanceof IsoWorldInventoryObject worldInventoryObject) {
                LoggerManager.getLogger("item")
                    .write(
                        connection.idStr
                            + " \""
                            + connection.username
                            + "\" floor +1 "
                            + this.obj.getXi()
                            + ","
                            + this.obj.getYi()
                            + ","
                            + this.obj.getZi()
                            + " ["
                            + worldInventoryObject.getItem().getFullType()
                            + "]"
                    );
            } else {
                String name = this.obj.getName() != null ? this.obj.getName() : this.obj.getObjectName();
                if (this.obj.getSprite() != null && this.obj.getSprite().getName() != null) {
                    name = name + " (" + this.obj.getSprite().getName() + ")";
                }

                LoggerManager.getLogger("map")
                    .write(
                        connection.idStr
                            + " \""
                            + connection.username
                            + "\" added "
                            + name
                            + " at "
                            + this.obj.getX()
                            + ","
                            + this.obj.getY()
                            + ","
                            + this.obj.getZ()
                    );
            }

            this.obj.addToWorld();
            this.obj.square.RecalcProperties();
            if (!(this.obj instanceof IsoWorldInventoryObject)) {
                this.obj.square.restackSheetRope();
                IsoWorld.instance.currentCell.checkHaveRoof(this.obj.square.getX(), this.obj.square.getY());
                MapCollisionData.instance.squareChanged(this.obj.square);
                PolygonalMap2.instance.squareChanged(this.obj.square);
                ServerMap.instance.physicsCheck(this.obj.square.x, this.obj.square.y);
                IsoRegions.squareChanged(this.obj.square);
                IsoGenerator.updateGenerator(this.obj.square);
            }

            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (c.getConnectedGUID() != connection.getConnectedGUID() && c.RelevantTo(this.obj.square.x, this.obj.square.y)) {
                    ByteBufferWriter b = c.startPacket();
                    PacketTypes.PacketType.AddItemToMap.doPacket(b);
                    this.write(b);
                    PacketTypes.PacketType.AddItemToMap.send(c);
                }
            }

            if (this.obj instanceof IsoWorldInventoryObject isoWorldInventoryObject) {
                isoWorldInventoryObject.dropTime = GameTime.getInstance().getWorldAgeHours();
            } else {
                LuaEventManager.triggerEvent("OnObjectAdded", this.obj);
            }
        } else if (GameServer.debug) {
            DebugLog.log("AddItemToMap: sq is null");
        }
    }
}
