// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoTrap;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

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

    public void set(HandWeapon weapon, IsoGridSquare sq) {
        if (weapon != null) {
            this.isNewItem = true;
            this.item = weapon;
        }

        this.x = sq.x;
        this.y = sq.y;
        this.z = (byte)sq.z;
    }

    @Override
    public void setData(Object... values) {
        if (values.length == 2 && values[1] instanceof IsoGridSquare sq) {
            if (values[0] != null) {
                this.isNewItem = true;
                this.item = (HandWeapon)values[0];
            }

            this.x = sq.x;
            this.y = sq.y;
            this.z = (byte)sq.z;
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
        b.putByte((byte)(this.isNewItem ? 1 : 0));
        if (this.isNewItem) {
            try {
                this.item.saveWithSize(b.bb, false);
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.get();
        this.isNewItem = b.get() != 0;
        if (this.isNewItem) {
            try {
                this.item = InventoryItem.loadItem(b, 241);
            } catch (Exception var4) {
                var4.printStackTrace();
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        if (sq != null) {
            if (this.isNewItem) {
                HandWeapon weapon = (HandWeapon)this.item;
                IsoTrap trap = new IsoTrap(weapon, sq.getCell(), sq);
                sq.AddTileObject(trap);
                trap.triggerExplosion(false);
            } else {
                DebugLog.General.printStackTrace("TODO: Rewrite call of the sq.explodeTrap() function");
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
            DebugLog.log("trap: user \"" + connection.username + "\" added " + this.item.getFullType() + " at " + this.x + "," + this.y + "," + this.z);
            LoggerManager.getLogger("map")
                .write(connection.idStr + " \"" + connection.username + "\" added " + this.item.getFullType() + " at " + this.x + "," + this.y + "," + this.z);
            IsoTrap trap = new IsoTrap(weapon, sq.getCell(), sq);
            sq.AddTileObject(trap);
            if (weapon.isInstantExplosion()) {
                for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                    UdpConnection c = GameServer.udpEngine.connections.get(n);
                    ByteBufferWriter b = c.startPacket();
                    PacketTypes.PacketType.AddExplosiveTrap.doPacket(b);
                    this.write(b);
                    PacketTypes.PacketType.AddExplosiveTrap.send(c);
                }

                trap.triggerExplosion(false);
            } else {
                trap.transmitCompleteItemToClients();
            }
        }
    }
}
