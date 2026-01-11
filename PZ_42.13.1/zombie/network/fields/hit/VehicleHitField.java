// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.fields.IMovable;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.BaseVehicle;

public class VehicleHitField extends Hit implements IMovable, INetworkPacketField {
    @JSONField
    public int vehicleDamage;
    @JSONField
    public float vehicleSpeed;
    @JSONField
    public boolean isVehicleHitFromBehind;
    @JSONField
    public boolean isTargetHitFromBehind;

    public void set(
        boolean ignore,
        float damage,
        float hitForce,
        float hitDirectionX,
        float hitDirectionY,
        int vehicleDamage,
        float vehicleSpeed,
        boolean isVehicleHitFromBehind,
        boolean isTargetHitFromBehind
    ) {
        this.set(damage, hitForce, hitDirectionX, hitDirectionY);
        this.vehicleDamage = vehicleDamage;
        this.vehicleSpeed = vehicleSpeed;
        this.isVehicleHitFromBehind = isVehicleHitFromBehind;
        this.isTargetHitFromBehind = isTargetHitFromBehind;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.vehicleDamage = b.getInt();
        this.vehicleSpeed = b.getFloat();
        this.isVehicleHitFromBehind = b.get() != 0;
        this.isTargetHitFromBehind = b.get() != 0;
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putInt(this.vehicleDamage);
        b.putFloat(this.vehicleSpeed);
        b.putBoolean(this.isVehicleHitFromBehind);
        b.putBoolean(this.isTargetHitFromBehind);
    }

    public void process(IsoGameCharacter wielder, IsoGameCharacter target, BaseVehicle vehicle) {
        this.process(wielder, target);
        if (GameServer.server) {
            if (this.vehicleDamage != 0) {
                if (this.isVehicleHitFromBehind) {
                    vehicle.addDamageFrontHitAChr(this.vehicleDamage);
                } else {
                    vehicle.addDamageRearHitAChr(this.vehicleDamage);
                }

                vehicle.transmitBlood();
            }

            if (target instanceof IsoAnimal isoAnimal) {
                isoAnimal.applyDamageFromVehicle(this.vehicleSpeed, this.damage);
            } else if (target instanceof IsoZombie isoZombie) {
                isoZombie.applyDamageFromVehicle(this.vehicleSpeed, this.damage);
            } else if (target instanceof IsoPlayer isoPlayer) {
                isoPlayer.getDamageFromHitByACar(this.vehicleSpeed);
            }
        } else if (GameClient.client && target instanceof IsoPlayer) {
            target.getActionContext().reportEvent("washit");
            target.setVariable("hitpvp", false);
        }
    }

    @Override
    public float getSpeed() {
        return this.vehicleSpeed;
    }

    @Override
    public boolean isVehicle() {
        return true;
    }
}
