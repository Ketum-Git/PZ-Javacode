// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import zombie.characters.Capability;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.IsoObject;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.hit.Thumpable;

@PacketSetting(ordering = 0, priority = 0, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class AnimalHitThumpablePacket extends AnimalHit {
    @JSONField
    protected final Thumpable thumpable = new Thumpable();

    @Override
    public void setData(Object... values) {
        if (values.length == 2 && values[0] instanceof IsoAnimal animal && values[1] instanceof IsoObject object) {
            this.set(animal, object);
        } else {
            DebugType.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoAnimal wielder, IsoObject thumpable) {
        this.set(wielder);
        this.thumpable.set(thumpable);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.thumpable.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.thumpable.write(b);
    }

    @Override
    public boolean isRelevant(UdpConnection connection) {
        return this.thumpable.isRelevant(connection);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return super.isConsistent(connection) && this.thumpable.isConsistent(connection);
    }

    @Override
    public void process() {
        this.thumpable.process(this.wielder.getAnimal());
    }
}
