// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.IMovable;
import zombie.network.fields.INetworkPacketField;
import zombie.vehicles.BaseVehicle;

public class VehicleHitField extends Hit implements IMovable, INetworkPacketField {
    @JSONField
    public float vehicleSpeed;
    @JSONField
    public boolean isTargetHitFromBehind;
    @JSONField
    public boolean isStaggerBack;
    @JSONField
    public boolean isKnockedDown;

    public void set(
        boolean ignore,
        float damage,
        float hitForce,
        float hitDirectionX,
        float hitDirectionY,
        float vehicleSpeed,
        boolean isTargetHitFromBehind,
        boolean isStaggerBack,
        boolean isKnockedDown
    ) {
        this.set(damage, hitForce, hitDirectionX, hitDirectionY);
        this.vehicleSpeed = vehicleSpeed;
        this.isTargetHitFromBehind = isTargetHitFromBehind;
        this.isStaggerBack = isStaggerBack;
        this.isKnockedDown = isKnockedDown;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.vehicleSpeed = b.getFloat();
        this.isTargetHitFromBehind = b.getBoolean();
        this.isStaggerBack = b.getBoolean();
        this.isKnockedDown = b.getBoolean();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putFloat(this.vehicleSpeed);
        b.putBoolean(this.isTargetHitFromBehind);
        b.putBoolean(this.isStaggerBack);
        b.putBoolean(this.isKnockedDown);
    }

    public void process(IsoGameCharacter wielder, IsoGameCharacter target, BaseVehicle vehicle) {
        this.process(wielder, target);
        if (GameServer.server) {
            if (target instanceof IsoAnimal isoAnimal) {
                isoAnimal.setHealth(0.0F);
            } else if (target instanceof IsoZombie isoZombie) {
                isoZombie.applyDamageFromVehicleHit(vehicle, this.vehicleSpeed, this.damage);
                isoZombie.setKnockedDown(this.isKnockedDown);
                isoZombie.setStaggerBack(this.isStaggerBack);
            } else if (target instanceof IsoPlayer isoPlayer) {
                isoPlayer.applyDamageFromVehicleHit(vehicle, this.vehicleSpeed, this.damage);
                isoPlayer.setKnockedDown(this.isKnockedDown);
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
