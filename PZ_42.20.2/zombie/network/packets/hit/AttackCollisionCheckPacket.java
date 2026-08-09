// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import zombie.CombatManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.fields.hit.Weapon;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 0, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class AttackCollisionCheckPacket implements INetworkPacket {
    @JSONField
    protected final PlayerID wielder = new PlayerID();
    @JSONField
    protected final Weapon weapon = new Weapon();
    @JSONField
    protected int hitCount;
    @JSONField
    protected short flags;

    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0], (HandWeapon)values[1], (Integer)values[4]);
    }

    public void set(IsoPlayer wielder, HandWeapon weapon, int hitCount) {
        this.wielder.set(wielder);
        this.weapon.set(weapon);
        this.hitCount = hitCount;
        this.flags = 0;
        if (wielder.isAimAtFloor()) {
            this.flags = (short)(this.flags | 1);
        }

        if (wielder.isDoShove()) {
            this.flags = (short)(this.flags | 2);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.wielder.parse(b, connection);
        this.weapon.parse(b, connection, this.wielder.getPlayer());
        this.hitCount = b.getInt();
        this.flags = b.getShort();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.wielder.write(b);
        this.weapon.write(b);
        b.putInt(this.hitCount);
        b.putShort(this.flags);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.wielder.isConsistent(connection) && this.weapon.isConsistent(connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.wielder.getPlayer().setAimAtFloor((this.flags & 1) != 0);
        this.wielder.getPlayer().setDoShove((this.flags & 2) != 0);
        CombatManager.getInstance().processWeaponEndurance(this.wielder.getPlayer(), this.weapon.getWeapon());
        this.wielder.getPlayer().addCombatMuscleStrain(this.weapon.getWeapon(), this.hitCount);
        this.wielder.getPlayer().setLastHitCount(this.hitCount);
    }

    public static class Flags {
        public static final short AimAtFloor = 1;
        public static final short DoShove = 2;
    }
}
