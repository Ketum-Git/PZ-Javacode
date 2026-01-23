// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.JSONField;
import zombie.network.fields.hit.Player;
import zombie.network.fields.hit.Weapon;

public abstract class PlayerHit implements HitCharacter {
    @JSONField
    protected final Player wielder = new Player();
    @JSONField
    protected final Weapon weapon = new Weapon();
    @JSONField
    protected boolean isIgnoreDamage;

    public void set(IsoPlayer wielder, HandWeapon weapon, boolean isIgnoreDamage, boolean isCriticalHit) {
        this.wielder.set(wielder, isCriticalHit);
        this.weapon.set(weapon);
        this.isIgnoreDamage = isIgnoreDamage;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.wielder.parse(b, connection);
        this.weapon.parse(b, connection, (IsoLivingCharacter)this.wielder.getCharacter());
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.wielder.write(b);
        this.weapon.write(b);
    }

    @Override
    public boolean isRelevant(UdpConnection connection) {
        return this.wielder.isRelevant(connection);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.wielder.isConsistent(connection) && this.weapon.isConsistent(connection);
    }

    @Override
    public void preProcess() {
        this.wielder.process();
    }

    @Override
    public void postProcess() {
        this.wielder.process();
    }

    @Override
    public void attack() {
        this.wielder.attack(this.weapon.getWeapon(), false);
    }

    public boolean isIgnoreDamage() {
        return this.isIgnoreDamage;
    }

    public HandWeapon getHandWeapon() {
        return this.weapon.getWeapon();
    }
}
