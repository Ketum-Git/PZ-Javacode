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
import zombie.network.fields.vehicle.VehicleID;
import zombie.vehicles.BaseVehicle;

@PacketSetting(
    ordering = 0,
    priority = 0,
    reliability = 3,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = {AntiCheat.HitLongDistance, AntiCheat.HitWeaponAmmo}
)
public class PlayerHitVehiclePacket extends PlayerHit implements AntiCheatHitLongDistance.IAntiCheat, AntiCheatHitWeaponAmmo.IAntiCheat {
    @JSONField
    protected final VehicleID vehicleId = new VehicleID();
    @JSONField
    protected float damage;

    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0], (HandWeapon)values[1], (Boolean)values[2], (Boolean)values[3], (BaseVehicle)values[4], (Float)values[5]);
    }

    public void set(IsoPlayer wielder, HandWeapon weapon, boolean isIgnoreDamage, boolean isCriticalHit, BaseVehicle vehicle, float damage) {
        this.set(wielder, weapon, isIgnoreDamage, isCriticalHit);
        this.vehicleId.set(vehicle);
        this.damage = damage;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.vehicleId.parse(b, connection);
        this.damage = b.getFloat();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.vehicleId.write(b);
        b.putFloat(this.damage);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.vehicleId.isConsistent(connection);
    }

    @Override
    public void process() {
        if (GameServer.server) {
            this.vehicleId.getVehicle().processHit(this.wielder.getCharacter(), this.getHandWeapon(), this.damage);
            if (this.getHandWeapon().getPhysicsObject() == null) {
                CombatManager.getInstance().processMaintenanceCheck(this.wielder.getCharacter(), this.getHandWeapon(), this.vehicleId.getVehicle());
            }

            CombatManager.getInstance().processWeaponEndurance(this.wielder.getCharacter(), this.getHandWeapon());
        }
    }

    @Override
    public IsoPlayer getWielder() {
        return (IsoPlayer)this.wielder.getCharacter();
    }

    @Override
    public float getDistance() {
        return IsoUtils.DistanceTo(this.vehicleId.getX(), this.vehicleId.getY(), this.wielder.getX(), this.wielder.getY());
    }
}
