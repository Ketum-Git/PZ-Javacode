// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.CombatManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitLongDistance;
import zombie.network.anticheats.AntiCheatHitWeaponAmmo;
import zombie.network.fields.Square;

@PacketSetting(
    ordering = 0,
    priority = 0,
    reliability = 3,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = {AntiCheat.HitLongDistance, AntiCheat.HitWeaponAmmo}
)
public class PlayerHitSquarePacket extends PlayerHit implements AntiCheatHitLongDistance.IAntiCheat, AntiCheatHitWeaponAmmo.IAntiCheat {
    @JSONField
    protected final Square square = new Square();

    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0], (HandWeapon)values[1], (Boolean)values[2], (Boolean)values[3]);
    }

    @Override
    public void set(IsoPlayer wielder, HandWeapon weapon, boolean isIgnoreDamage, boolean isCriticalHit) {
        super.set(wielder, weapon, isIgnoreDamage, isCriticalHit);
        this.square.set(wielder);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.square.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.square.write(b);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.square.isConsistent(connection);
    }

    @Override
    public void process() {
        this.square.process(this.wielder.getCharacter());
        if (GameServer.server) {
            CombatManager.getInstance().processWeaponEndurance(this.wielder.getCharacter(), this.getHandWeapon());
        }
    }

    @Override
    public IsoPlayer getWielder() {
        return (IsoPlayer)this.wielder.getCharacter();
    }

    @Override
    public float getDistance() {
        return IsoUtils.DistanceTo(this.square.getX(), this.square.getY(), this.wielder.getX(), this.wielder.getY());
    }
}
