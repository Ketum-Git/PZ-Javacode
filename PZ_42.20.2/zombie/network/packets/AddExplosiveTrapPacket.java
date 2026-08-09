// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoTrap;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class AddExplosiveTrapPacket implements INetworkPacket {
    @JSONField
    int x;
    @JSONField
    int y;
    @JSONField
    byte z;
    @JSONField
    InventoryItem item;
    @JSONField
    boolean isNewItem;
    @JSONField
    final PlayerID attackerID = new PlayerID();

    public void set(HandWeapon weapon, IsoPlayer attacker, IsoGridSquare sq) {
        this.isNewItem = weapon != null;
        this.item = weapon;
        this.attackerID.set(attacker);
        this.x = sq.x;
        this.y = sq.y;
        this.z = (byte)sq.z;
    }

    @Override
    public void setData(Object... values) {
        if (values.length == 3 && values[2] instanceof IsoGridSquare sq) {
            if (values[0] instanceof HandWeapon weapon) {
                this.isNewItem = true;
                this.item = weapon;
                this.attackerID.set((IsoPlayer)values[1]);
            } else {
                this.isNewItem = false;
                this.item = null;
                this.attackerID.clear();
            }

            this.x = sq.x;
            this.y = sq.y;
            this.z = (byte)sq.z;
        } else {
            DebugType.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
        if (b.putBoolean(this.isNewItem)) {
            this.attackerID.write(b);

            try {
                this.item.saveWithSize(b.bb, false);
            } catch (IOException var3) {
                DebugType.General.printException(var3, LogSeverity.Error);
            }
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.getByte();
        this.isNewItem = b.getBoolean();
        if (this.isNewItem) {
            this.attackerID.parse(b, connection);

            try {
                this.item = InventoryItem.loadItem(b.bb, 249);
            } catch (Exception var4) {
                DebugType.General.printException(var4, LogSeverity.Error);
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (sq != null && this.isNewItem) {
            HandWeapon weapon = (HandWeapon)this.item;
            IsoTrap trap = new IsoTrap(weapon, sq.getCell(), sq);
            sq.AddTileObject(trap);
            if (trap.isInstantExplosion()) {
                trap.triggerExplosion();
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (sq != null && this.isNewItem) {
            if (this.item == null) {
                return;
            }

            HandWeapon weapon = (HandWeapon)this.item;
            DebugLog.log("trap: user \"" + connection.getUserName() + "\" added " + this.item.getFullType() + " at " + this.x + "," + this.y + "," + this.z);
            LoggerManager.getLogger("map")
                .write(
                    connection.getIDStr()
                        + " \""
                        + connection.getUserName()
                        + "\" added "
                        + this.item.getFullType()
                        + " at "
                        + this.x
                        + ","
                        + this.y
                        + ","
                        + this.z
                );
            IsoTrap trap = new IsoTrap(this.attackerID.getPlayer(), weapon, sq.getCell(), sq);
            sq.AddTileObject(trap);
            if (weapon.isInstantExplosion()) {
                for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                    UdpConnection c = GameServer.udpEngine.connections.get(n);
                    ByteBufferWriter b = c.startPacket();
                    PacketTypes.PacketType.AddExplosiveTrap.doPacket(b);
                    this.write(b);
                    PacketTypes.PacketType.AddExplosiveTrap.send(c);
                }

                trap.triggerExplosion();
            } else {
                trap.transmitCompleteItemToClients();
            }
        }
    }
}
