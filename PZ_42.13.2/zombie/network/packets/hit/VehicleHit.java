// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.fields.hit.Fall;
import zombie.network.fields.hit.Player;
import zombie.network.fields.hit.VehicleHitField;
import zombie.network.fields.vehicle.VehicleID;
import zombie.vehicles.BaseVehicle;

public abstract class VehicleHit implements HitCharacter {
    @JSONField
    protected final Player wielder = new Player();
    @JSONField
    protected final VehicleID vehicleId = new VehicleID();
    @JSONField
    public final VehicleHitField vehicleHit = new VehicleHitField();
    @JSONField
    protected final Fall fall = new Fall();

    public void set(
        IsoPlayer wielder,
        IsoGameCharacter character,
        BaseVehicle vehicle,
        boolean isCriticalHit,
        float damage,
        boolean isTargetHitFromBehind,
        int vehicleDamage,
        float vehicleSpeed,
        boolean isVehicleHitFromBehind
    ) {
        this.wielder.set(wielder, isCriticalHit);
        this.vehicleId.set(vehicle);
        this.vehicleHit
            .set(
                false,
                damage,
                character.getHitForce(),
                character.getHitDir().x,
                character.getHitDir().y,
                vehicleDamage,
                vehicleSpeed,
                isVehicleHitFromBehind,
                isTargetHitFromBehind
            );
        this.fall.set(character.getHitReactionNetworkAI());
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.wielder.parse(b, connection);
        this.vehicleId.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.wielder.write(b);
        this.vehicleId.write(b);
    }

    @Override
    public boolean isRelevant(UdpConnection connection) {
        return this.wielder.isRelevant(connection);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.wielder.isConsistent(connection) && this.vehicleId.isConsistent(connection);
    }

    @Override
    public void preProcess() {
        this.wielder.process();
    }

    @Override
    public void postProcess() {
        this.wielder.process();
    }

    @Override
    public boolean isPostponed() {
        return true;
    }
}
