// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoUtils;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitShortDistance;
import zombie.network.fields.character.AnimalID;
import zombie.network.fields.hit.Damage;

@PacketSetting(
    ordering = 0,
    priority = 0,
    reliability = 3,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = AntiCheat.HitShortDistance
)
public class AnimalHitAnimalPacket extends AnimalHit implements AntiCheatHitShortDistance.IAntiCheat {
    @JSONField
    protected final AnimalID target = new AnimalID();
    @JSONField
    protected final Damage damage = new Damage();

    @Override
    public void setData(Object... values) {
        if (values.length == 4
            && values[0] instanceof IsoAnimal animal
            && values[1] instanceof IsoAnimal animalTarget
            && values[2] instanceof Float damageValue
            && values[3] instanceof Boolean ignore) {
            this.set(animal, animalTarget, ignore, damageValue);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoAnimal wielder, IsoAnimal target, boolean ignore, float damage) {
        this.set(wielder);
        this.target.set(target);
        this.damage.set(ignore, damage);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.target.parse(b, connection);
        this.damage.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.target.write(b);
        this.damage.write(b);
    }

    @Override
    public boolean isRelevant(UdpConnection connection) {
        return this.target.isRelevant(connection);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.target.isConsistent(connection);
    }

    @Override
    public void preProcess() {
    }

    @Override
    public void process() {
        this.damage.processAnimal(this.wielder.getAnimal(), this.target.getAnimal());
    }

    @Override
    public void postProcess() {
    }

    @Override
    public float getDistance() {
        return IsoUtils.DistanceTo(this.target.getX(), this.target.getY(), this.wielder.getX(), this.wielder.getY());
    }
}
