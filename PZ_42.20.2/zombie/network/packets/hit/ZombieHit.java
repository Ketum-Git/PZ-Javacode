// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.hit.Zombie;

public abstract class ZombieHit implements HitCharacter {
    @JSONField
    protected final Zombie wielder = new Zombie();

    public void set(IsoZombie wielder) {
        this.wielder.set(wielder, false);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.wielder.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.wielder.write(b);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.wielder.isConsistent(connection);
    }
}
