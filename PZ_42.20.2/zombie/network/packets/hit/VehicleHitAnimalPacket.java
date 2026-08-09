// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.IsoUtils;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitDamage;
import zombie.network.anticheats.AntiCheatHitShortDistance;
import zombie.network.anticheats.AntiCheatHitVehicle;
import zombie.network.anticheats.AntiCheatSpeed;
import zombie.network.fields.IMovable;
import zombie.network.fields.character.AnimalID;
import zombie.network.fields.hit.Hit;
import zombie.vehicles.BaseVehicle;

@PacketSetting(
    ordering = 0,
    priority = 0,
    reliability = 3,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 1,
    anticheats = {AntiCheat.HitDamage, AntiCheat.HitShortDistance, AntiCheat.Speed, AntiCheat.HitVehicle}
)
public class VehicleHitAnimalPacket
    extends VehicleHit
    implements AntiCheatHitDamage.IAntiCheat,
    AntiCheatHitShortDistance.IAntiCheat,
    AntiCheatSpeed.IAntiCheat,
    AntiCheatHitVehicle.IAntiCheat {
    @JSONField
    protected final AnimalID target = new AnimalID();

    public void set(IsoPlayer wielder, IsoAnimal target, BaseVehicle vehicle, float damage, boolean isTargetHitFromBehind, float vehicleSpeed) {
        this.set(wielder, target, vehicle, false, damage, isTargetHitFromBehind, vehicleSpeed);
        this.target.set(target);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.target.parse(b, connection);
        this.vehicleHit.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.target.write(b);
        this.vehicleHit.write(b);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return super.isConsistent(connection) && this.target.isConsistent(connection) && this.vehicleHit.isConsistent(connection);
    }

    @Override
    public void process() {
        this.vehicleHit.process(this.wielder.getCharacter(), this.target.getAnimal(), this.vehicleId.getVehicle());
    }

    @Override
    public float getFurthestHitDistance() {
        return IsoUtils.DistanceTo(this.target.getX(), this.target.getY(), this.wielder.getX(), this.wielder.getY());
    }

    @Override
    public Hit getHit() {
        return this.vehicleHit;
    }

    @Override
    public IMovable getMovable(int index) {
        return this.vehicleHit;
    }

    @Override
    public int getMovableCount() {
        return 1;
    }
}
