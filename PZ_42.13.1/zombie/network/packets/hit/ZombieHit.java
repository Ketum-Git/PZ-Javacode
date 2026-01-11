// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.fields.hit.Zombie;

public abstract class ZombieHit implements HitCharacter {
    @JSONField
    protected final Zombie wielder = new Zombie();

    public void set(IsoZombie wielder) {
        this.wielder.set(wielder);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.wielder.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.wielder.write(b);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.wielder.isConsistent(connection);
    }
}
