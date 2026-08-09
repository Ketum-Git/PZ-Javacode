// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.character.AnimalID;

public abstract class AnimalHit implements HitCharacter {
    @JSONField
    protected final AnimalID wielder = new AnimalID();

    public void set(IsoAnimal wielder) {
        this.wielder.set(wielder);
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
