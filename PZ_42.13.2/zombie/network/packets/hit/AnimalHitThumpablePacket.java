// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitShortDistance;
import zombie.network.fields.hit.Thumpable;

@PacketSetting(
    ordering = 0,
    priority = 0,
    reliability = 3,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = AntiCheat.HitShortDistance
)
public class AnimalHitThumpablePacket extends AnimalHit implements AntiCheatHitShortDistance.IAntiCheat {
    @JSONField
    protected final Thumpable thumpable = new Thumpable();

    @Override
    public void setData(Object... values) {
        if (values.length == 2 && values[0] instanceof IsoAnimal animal && values[1] instanceof IsoObject object) {
            this.set(animal, object);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoAnimal wielder, IsoObject thumpable) {
        this.set(wielder);
        this.thumpable.set(thumpable);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
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
    public void preProcess() {
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.thumpable.isConsistent(connection);
    }

    @Override
    public void process() {
        this.thumpable.process(this.wielder.getAnimal());
    }

    @Override
    public void postProcess() {
    }

    @Override
    public float getDistance() {
        return IsoUtils.DistanceTo(this.thumpable.getX(), this.thumpable.getY(), this.wielder.getX(), this.wielder.getY());
    }
}
