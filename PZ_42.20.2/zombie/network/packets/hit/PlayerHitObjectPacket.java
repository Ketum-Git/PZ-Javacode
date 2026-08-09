// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.hit;

import java.util.List;
import zombie.CombatManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatHitLongDistance;
import zombie.network.anticheats.AntiCheatHitWeapon;
import zombie.network.fields.hit.Thumpable;
import zombie.network.fields.hit.TracerInfo;

@PacketSetting(
    ordering = 0,
    priority = 0,
    reliability = 3,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = {AntiCheat.HitLongDistance, AntiCheat.HitWeapon}
)
public class PlayerHitObjectPacket extends PlayerHit implements AntiCheatHitLongDistance.IAntiCheat, AntiCheatHitWeapon.IAntiCheat {
    @JSONField
    protected final Thumpable thumpable = new Thumpable();

    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0], (HandWeapon)values[1], (Boolean)values[2], (Boolean)values[3], (List<TracerInfo>)values[4], (IsoObject)values[5]);
    }

    public void set(IsoPlayer wielder, HandWeapon weapon, boolean isIgnoreDamage, boolean isCriticalHit, List<TracerInfo> tracers, IsoObject obj) {
        this.set(wielder, weapon, isIgnoreDamage, isCriticalHit, tracers);
        this.thumpable.set(obj);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.thumpable.write(b);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.thumpable.parse(b, connection);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return super.isConsistent(connection) && this.thumpable.isConsistent(connection);
    }

    @Override
    public void process() {
        if (GameServer.server) {
            CombatManager.getInstance().processMaintenanceCheck(this.wielder.getCharacter(), this.getHandWeapon(), this.thumpable.getIsoObject());
        }

        if (this.thumpable.getIsoObject() instanceof IsoDoor
            || this.thumpable.getIsoObject() instanceof IsoTree
            || this.thumpable.getIsoObject() instanceof IsoWindow
            || this.thumpable.getIsoObject() instanceof IsoBarricade
            || this.thumpable.getIsoObject() instanceof IsoThumpable) {
            this.thumpable.getIsoObject().WeaponHit(this.wielder.getPlayer(), this.weapon.getWeapon());
        }
    }

    @Override
    public void attack() {
        this.wielder.attack(this.getHandWeapon(), false, this.shotID);
        this.processTracers();
    }

    @Override
    public IsoPlayer getWielder() {
        return (IsoPlayer)this.wielder.getCharacter();
    }

    @Override
    public float getDistance() {
        return IsoUtils.DistanceTo(this.thumpable.getX(), this.thumpable.getY(), this.wielder.getX(), this.wielder.getY());
    }

    @Override
    public void log(UdpConnection connection) {
        if (this.thumpable.getIsoObject().getObjectIndex() == -1) {
            LoggerManager.getLogger("map")
                .write(
                    String.format(
                        "%s \"%s\" destroyed %s with %s at %f,%f,%f",
                        connection.getIDStr(),
                        connection.getUserName(),
                        this.thumpable.getName(),
                        this.weapon.getWeapon().getName(),
                        this.thumpable.getIsoObject().getX(),
                        this.thumpable.getIsoObject().getY(),
                        this.thumpable.getIsoObject().getZ()
                    )
                );
        }
    }
}
