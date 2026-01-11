// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class EquipPacket implements INetworkPacket {
    @JSONField
    PlayerID player = new PlayerID();
    @JSONField
    int primaryHand = -1;
    @JSONField
    int secondaryHand = -1;

    @Override
    public void setData(Object... values) {
        if (values.length == 1 && values[0] instanceof IsoPlayer player) {
            this.player.set(player);
            this.primaryHand = player.getPrimaryHandItem() == null ? -1 : player.getPrimaryHandItem().getID();
            this.secondaryHand = player.getSecondaryHandItem() == null ? -1 : player.getSecondaryHandItem().getID();
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        if (GameClient.client) {
            b.putInt(this.primaryHand);
            b.putInt(this.secondaryHand);
        }

        if (GameServer.server) {
            if (this.player.getPlayer().getPrimaryHandItem() == null) {
                b.putByte((byte)EquipPacket.HandStatus.Nothing.ordinal());
            } else {
                b.putByte((byte)EquipPacket.HandStatus.ItemExist.ordinal());

                try {
                    this.player.getPlayer().getPrimaryHandItem().saveWithSize(b.bb, false);
                    if (this.player.getPlayer().getPrimaryHandItem().getVisual() != null) {
                        b.putBoolean(true);
                        this.player.getPlayer().getPrimaryHandItem().getVisual().save(b.bb);
                    } else {
                        b.putBoolean(false);
                    }
                } catch (IOException var4) {
                    DebugLog.Multiplayer.printException(var4, "Primary ItemVisual save failed", LogSeverity.Error);
                }
            }

            if (this.player.getPlayer().getSecondaryHandItem() == null) {
                b.putByte((byte)EquipPacket.HandStatus.Nothing.ordinal());
            } else if (this.player.getPlayer().getSecondaryHandItem() == this.player.getPlayer().getPrimaryHandItem()) {
                b.putByte((byte)EquipPacket.HandStatus.SameItemAsPrimary.ordinal());
            } else {
                b.putByte((byte)EquipPacket.HandStatus.ItemExist.ordinal());

                try {
                    this.player.getPlayer().getSecondaryHandItem().saveWithSize(b.bb, false);
                    if (this.player.getPlayer().getSecondaryHandItem().getVisual() != null) {
                        b.putBoolean(true);
                        this.player.getPlayer().getSecondaryHandItem().getVisual().save(b.bb);
                    } else {
                        b.putBoolean(false);
                    }
                } catch (IOException var3) {
                    DebugLog.Multiplayer.printException(var3, "Primary ItemVisual save failed", LogSeverity.Error);
                }
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.player.parse(b, connection);
        if (GameServer.server) {
            this.primaryHand = b.getInt();
            this.secondaryHand = b.getInt();
        }

        if (!GameClient.client || this.player.getPlayer() != null) {
            if (GameClient.client) {
                EquipPacket.HandStatus primaryHand = EquipPacket.HandStatus.values()[b.get()];
                if (primaryHand == EquipPacket.HandStatus.Nothing) {
                    if (this.player.getPlayer() != null) {
                        this.player.getPlayer().setPrimaryHandItem(null);
                    }
                } else {
                    InventoryItem primaryHandItem = null;

                    try {
                        primaryHandItem = InventoryItem.loadItem(b, 240);
                        if (this.player.getPlayer().isLocalPlayer()) {
                            primaryHandItem = this.player.getPlayer().getInventory().getItemWithID(primaryHandItem.getID());
                        } else {
                            primaryHandItem.setContainer(this.player.getPlayer().getInventory());
                        }

                        this.player.getPlayer().setPrimaryHandItem(primaryHandItem);
                        byte hasVisual = b.get();
                        if (hasVisual == 1) {
                            primaryHandItem.getVisual().load(b, 240);
                        }
                    } catch (Exception var8) {
                        ExceptionLogger.logException(var8);
                    }

                    if (primaryHandItem == null) {
                        LoggerManager.getLogger("user").write(connection.idStr + " equipped unknown item type");
                        return;
                    }
                }

                EquipPacket.HandStatus secondaryHand = EquipPacket.HandStatus.values()[b.get()];
                if (secondaryHand == EquipPacket.HandStatus.Nothing) {
                    this.player.getPlayer().setSecondaryHandItem(null);
                } else if (secondaryHand == EquipPacket.HandStatus.SameItemAsPrimary) {
                    this.player.getPlayer().setSecondaryHandItem(this.player.getPlayer().getPrimaryHandItem());
                } else {
                    InventoryItem secondaryHandItem = null;

                    try {
                        secondaryHandItem = InventoryItem.loadItem(b, 240);
                        if (this.player.getPlayer().isLocalPlayer()) {
                            secondaryHandItem = this.player.getPlayer().getInventory().getItemWithID(secondaryHandItem.getID());
                        } else {
                            secondaryHandItem.setContainer(this.player.getPlayer().getInventory());
                        }

                        this.player.getPlayer().setSecondaryHandItem(secondaryHandItem);
                        byte hasVisual = b.get();
                        if (hasVisual == 1) {
                            secondaryHandItem.getVisual().load(b, 240);
                        }
                    } catch (Exception var7) {
                        ExceptionLogger.logException(var7);
                    }

                    if (secondaryHandItem == null) {
                        LoggerManager.getLogger("user").write(connection.idStr + " equipped unknown item type");
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.player.getPlayer().isLocalPlayer()) {
            this.primaryHand = this.player.getPlayer().getPrimaryHandItem() == null ? -1 : this.player.getPlayer().getPrimaryHandItem().getID();
            this.secondaryHand = this.player.getPlayer().getSecondaryHandItem() == null ? -1 : this.player.getPlayer().getSecondaryHandItem().getID();
            this.sendToServer(PacketTypes.PacketType.Equip);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.primaryHand == -1) {
            this.player.getPlayer().setPrimaryHandItem(null);
        } else {
            this.player.getPlayer().setPrimaryHandItem(this.player.getPlayer().getInventory().getItemWithID(this.primaryHand));
        }

        if (this.secondaryHand == -1) {
            this.player.getPlayer().setSecondaryHandItem(null);
        } else if (this.secondaryHand == this.primaryHand) {
            this.player.getPlayer().setSecondaryHandItem(this.player.getPlayer().getPrimaryHandItem());
        } else {
            this.player.getPlayer().setSecondaryHandItem(this.player.getPlayer().getInventory().getItemWithID(this.secondaryHand));
        }

        this.sendToClients(PacketTypes.PacketType.Equip, connection);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.player.getPlayer() != null;
    }

    static enum HandStatus {
        Nothing,
        ItemExist,
        SameItemAsPrimary;
    }
}
