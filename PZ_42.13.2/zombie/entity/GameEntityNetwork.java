// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.entity.network.PacketGroup;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.util.Type;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class GameEntityNetwork implements INetworkPacket {
    @JSONField
    EntityPacketData data;
    @JSONField
    GameEntity entity;
    @JSONField
    Component component;

    public static EntityPacketData createPacketData(EntityPacketType packetType) {
        return EntityPacketData.alloc(packetType);
    }

    public static void sendPacketDataTo(IsoPlayer player, EntityPacketData data, GameEntity entity, Component component) {
        if (!GameServer.server) {
            throw new RuntimeException("Can only call on server.");
        } else if (player != null) {
            UdpConnection connection = GameServer.getConnectionFromPlayer(player);
            if (connection != null) {
                sendPacketData(data, entity, component, connection, false);
            }
        }
    }

    public static void sendPacketData(EntityPacketData data, GameEntity entity, Component component, UdpConnection connection, boolean isIgnoreConnection) {
        if (entity == null) {
            DebugLog.General.warn("Packet should have entity set.");
        } else if (entity.getGameEntityType() != GameEntityType.Template) {
            if (GameClient.client || GameServer.server) {
                if (data.getEntityPacketType().isEntityPacket() && component != null) {
                    DebugLog.General.warn("Entity Packet should not have component set.");
                } else if (data.getEntityPacketType().isComponentPacket() && component == null) {
                    DebugLog.General.warn("Component Packet requires to have component set.");
                } else {
                    if (GameClient.client) {
                        INetworkPacket.send(PacketTypes.PacketType.GameEntity, data, entity, component);
                    } else if (GameServer.server) {
                        if (isIgnoreConnection) {
                            INetworkPacket.sendToAll(PacketTypes.PacketType.GameEntity, connection, data, entity, component);
                        } else {
                            INetworkPacket.send(connection, PacketTypes.PacketType.GameEntity, data, entity, component);
                        }
                    }

                    EntityPacketData.release(data);
                }
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        long entityNetID = this.entity.getEntityNetID();
        if (entityNetID < 0L) {
            throw new RuntimeException("Invalid EntityNetID");
        } else {
            b.putLong(entityNetID);
            InventoryItem item = Type.tryCastTo(this.entity, InventoryItem.class);
            if (item != null && item.getContainer() != null && item.getContainer().getParent() instanceof IsoPlayer) {
                b.putShort(((IsoPlayer)item.getContainer().getParent()).getOnlineID());
            } else {
                b.putShort((short)-1);
            }

            if (this.component != null) {
                b.putShort(this.component.getComponentType().GetID());
            } else {
                b.putShort((short)-1);
            }

            this.data.bb.limit(this.data.bb.position());
            this.data.bb.position(0);
            b.bb.put(this.data.bb);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        if (GameClient.client || GameServer.server) {
            UdpConnection senderConnection = GameClient.client ? null : connection;
            long entityNetId = b.getLong();
            short onlineID = b.getShort();
            GameEntity gameEntity = GameEntityManager.GetEntity(entityNetId);
            if (gameEntity == null) {
                DebugLog.Objects.debugln("Cannot find registered game entity for id = %d", entityNetId);
                IsoPlayer player = GameClient.IDToPlayerMap.get(onlineID);
                if (player != null) {
                    InventoryItem item = player.getInventory().getItemWithID((int)entityNetId);
                    if (item != null) {
                        gameEntity = Type.tryCastTo(item, GameEntity.class);
                    }
                }

                if (gameEntity == null) {
                    DebugLog.Objects.debugln("Cannot find inventory item for id = %d", entityNetId);
                    return;
                }
            }

            short componentID = b.getShort();
            EntityPacketType packetType = EntityPacketType.FromByteBuffer(b);

            try {
                if (packetType.getGroup() != PacketGroup.GameEntity) {
                    Component component = gameEntity.getComponentFromID(componentID);
                    if (component != null) {
                        boolean success = component.onReceivePacket(b, packetType, senderConnection);
                        if (!success && Core.debug) {
                            DebugLog.Entity
                                .error("ReadPacketContents returned failure. Component = " + component.getComponentType() + ", packetType = " + packetType);
                        }
                    }
                } else {
                    boolean success = gameEntity.onReceiveEntityPacket(b, packetType, senderConnection);
                    if (!success && Core.debug) {
                        DebugLog.Entity.error("ReadPacketContents returned failure. PacketType = " + packetType);
                    }
                }
            } catch (Exception var12) {
                throw new RuntimeException(var12);
            }
        }
    }

    @Override
    public void setData(Object... values) {
        this.data = (EntityPacketData)values[0];
        this.entity = (GameEntity)values[1];
        this.component = (Component)values[2];
    }
}
