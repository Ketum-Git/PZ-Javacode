// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoUtils;
import zombie.network.PVPLogTool;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitShortDistance;
import zombie.network.anticheats.AntiCheatTarget;
import zombie.network.fields.hit.Bite;
import zombie.network.fields.hit.Character;
import zombie.network.fields.hit.Player;

@PacketSetting(
    ordering = 0,
    priority = 0,
    reliability = 3,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = {AntiCheat.HitShortDistance, AntiCheat.Target}
)
public class ZombieHitPlayerPacket extends ZombieHit implements AntiCheatHitShortDistance.IAntiCheat, AntiCheatTarget.IAntiCheat {
    public final Player target = new Player();
    protected final Bite bite = new Bite();

    public void set(IsoZombie wielder, IsoPlayer target) {
        this.set(wielder);
        this.target.set(target, false);
        this.bite.set(wielder);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.target.parse(b, connection);
        this.bite.parse(b, connection);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.target.write(b);
        this.bite.write(b);
    }

    @Override
    public boolean isRelevant(UdpConnection connection) {
        return this.target.isRelevant(connection);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.target.isConsistent(connection) && super.isConsistent(connection) && this.wielder.getZombie().getOwner() == connection;
    }

    @Override
    public void preProcess() {
        this.wielder.process();
        this.target.process();
    }

    @Override
    public void process() {
        this.bite.process((IsoZombie)this.wielder.getCharacter(), this.target.getCharacter());
    }

    @Override
    public void postProcess() {
        this.wielder.process();
        this.target.process();
    }

    @Override
    public void log(UdpConnection connection) {
        PVPLogTool.logCombat(
            this.wielder.getCharacter().getOwnerPlayer().getUsername(),
            LoggerManager.getPlayerCoords(this.wielder.getCharacter()),
            this.target.getPlayer().getUsername(),
            LoggerManager.getPlayerCoords(this.target.getPlayer()),
            this.wielder.getCharacter().getX(),
            this.wielder.getCharacter().getY(),
            this.wielder.getCharacter().getZ(),
            "zombie",
            -1.0F
        );
    }

    @Override
    public void react() {
        this.wielder.react();
        this.target.react();
    }

    @Override
    public float getDistance() {
        return IsoUtils.DistanceTo(this.target.getX(), this.target.getY(), this.wielder.getX(), this.wielder.getY());
    }

    @Override
    public Character getTargetCharacter() {
        return this.target;
    }
}
