// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoUtils;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitDamage;
import zombie.network.anticheats.AntiCheatHitLongDistance;
import zombie.network.anticheats.AntiCheatHitWeaponAmmo;
import zombie.network.anticheats.AntiCheatHitWeaponRange;
import zombie.network.anticheats.AntiCheatHitWeaponRate;
import zombie.network.fields.character.AnimalID;
import zombie.network.fields.hit.Hit;
import zombie.network.fields.hit.WeaponHit;

@PacketSetting(
    ordering = 0,
    priority = 0,
    reliability = 3,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = {AntiCheat.HitDamage, AntiCheat.HitLongDistance, AntiCheat.HitWeaponAmmo, AntiCheat.HitWeaponRange, AntiCheat.HitWeaponRate}
)
public class PlayerHitAnimalPacket
    extends PlayerHit
    implements AntiCheatHitDamage.IAntiCheat,
    AntiCheatHitLongDistance.IAntiCheat,
    AntiCheatHitWeaponAmmo.IAntiCheat,
    AntiCheatHitWeaponRange.IAntiCheat,
    AntiCheatHitWeaponRate.IAntiCheat {
    @JSONField
    protected final AnimalID target = new AnimalID();
    @JSONField
    protected final WeaponHit hit = new WeaponHit();

    @Override
    public void setData(Object... values) {
        this.set(
            (IsoPlayer)values[0],
            (HandWeapon)values[1],
            (Boolean)values[2],
            (Boolean)values[3],
            (IsoAnimal)values[4],
            (Float)values[5],
            (Float)values[6],
            (Boolean)values[7]
        );
    }

    public void set(
        IsoPlayer wielder, HandWeapon weapon, boolean isIgnoreDamage, boolean isCriticalHit, IsoAnimal target, float damage, float range, boolean hitHead
    ) {
        this.set(wielder, weapon, isIgnoreDamage, isCriticalHit);
        this.target.set(target);
        this.hit.set(damage, range, target.getHitForce(), target.getHitDir().x, target.getHitDir().y, hitHead);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.target.parse(b, connection);
        this.hit.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.target.write(b);
        this.hit.write(b);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.target.isConsistent(connection) && this.hit.isConsistent(connection);
    }

    @Override
    public void process() {
        this.hit.process(this.wielder.getCharacter(), this.target.getAnimal(), this.getHandWeapon(), this.isIgnoreDamage());
    }

    @Override
    public void attack() {
        this.wielder.attack(this.getHandWeapon(), false);
    }

    @Override
    public float getDistance() {
        return IsoUtils.DistanceTo(this.target.getX(), this.target.getY(), this.wielder.getX(), this.wielder.getY());
    }

    @Override
    public IsoPlayer getWielder() {
        return this.wielder.getPlayer();
    }

    @Override
    public IsoGameCharacter getTarget() {
        return this.target.getAnimal();
    }

    @Override
    public Hit getHit() {
        return this.hit;
    }
}
