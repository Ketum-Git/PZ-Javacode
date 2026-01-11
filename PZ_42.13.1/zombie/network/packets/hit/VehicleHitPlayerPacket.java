// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PVPLogTool;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitDamage;
import zombie.network.anticheats.AntiCheatHitShortDistance;
import zombie.network.anticheats.AntiCheatSafety;
import zombie.network.anticheats.AntiCheatSpeed;
import zombie.network.fields.IMovable;
import zombie.network.fields.hit.Hit;
import zombie.network.fields.hit.Player;
import zombie.vehicles.BaseVehicle;

@PacketSetting(
    ordering = 0,
    priority = 0,
    reliability = 3,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = {AntiCheat.HitDamage, AntiCheat.HitShortDistance, AntiCheat.Safety, AntiCheat.Speed}
)
public class VehicleHitPlayerPacket
    extends VehicleHit
    implements AntiCheatHitDamage.IAntiCheat,
    AntiCheatHitShortDistance.IAntiCheat,
    AntiCheatSafety.IAntiCheat,
    AntiCheatSpeed.IAntiCheat {
    @JSONField
    protected final Player target = new Player();

    public void set(
        IsoPlayer wielder,
        IsoPlayer target,
        BaseVehicle vehicle,
        float damage,
        boolean isTargetHitFromBehind,
        int vehicleDamage,
        float vehicleSpeed,
        boolean isVehicleHitFromBehind
    ) {
        this.set(wielder, target, vehicle, false, damage, isTargetHitFromBehind, vehicleDamage, vehicleSpeed, isVehicleHitFromBehind);
        this.target.set(target, false);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.target.parse(b, connection);
        this.vehicleHit.parse(b, connection);
        this.fall.parse(b, connection);
        if (GameServer.server) {
            if (this.vehicleId.getVehicle().getDriver() != null && this.vehicleId.getVehicle().getDriver() == this.wielder.getPlayer()) {
                this.vehicleHit.vehicleSpeed = this.vehicleId.getVehicle().getDriver().getNetworkCharacterAI().speedChecker.getSpeed();
            } else {
                this.vehicleHit.vehicleSpeed = 0.0F;
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.target.write(b);
        this.vehicleHit.write(b);
        this.fall.write(b);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.target.isConsistent(connection) && this.vehicleHit.isConsistent(connection);
    }

    @Override
    public void preProcess() {
        super.preProcess();
        this.target.process();
    }

    @Override
    public void process() {
        this.vehicleHit.process(this.wielder.getCharacter(), this.target.getCharacter(), this.vehicleId.getVehicle());
        this.fall.process(this.target.getCharacter());
    }

    @Override
    public void postProcess() {
        super.postProcess();
        this.target.process();
    }

    @Override
    public void react() {
        this.target.react();
    }

    @Override
    public void postpone() {
        this.target.getCharacter().getNetworkCharacterAI().setVehicleHit(this);
    }

    @Override
    public void log(UdpConnection connection) {
        PVPLogTool.logCombat(
            this.wielder.getPlayer().getUsername(),
            LoggerManager.getPlayerCoords(this.wielder.getPlayer()),
            this.target.getPlayer().getUsername(),
            LoggerManager.getPlayerCoords(this.target.getPlayer()),
            this.wielder.getPlayer().getX(),
            this.wielder.getPlayer().getY(),
            this.wielder.getPlayer().getZ(),
            "vehicle",
            this.vehicleHit.getSpeed()
        );
    }

    @Override
    public float getDistance() {
        return IsoUtils.DistanceTo(this.target.getX(), this.target.getY(), this.wielder.getX(), this.wielder.getY());
    }

    @Override
    public Hit getHit() {
        return this.vehicleHit;
    }

    @Override
    public IMovable getMovable() {
        return this.vehicleHit;
    }

    @Override
    public IsoGameCharacter getTarget() {
        return this.target.getPlayer();
    }

    @Override
    public IsoPlayer getWielder() {
        return this.wielder.getPlayer();
    }
}
