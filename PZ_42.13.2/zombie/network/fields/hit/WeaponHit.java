// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import java.nio.ByteBuffer;
import zombie.CombatManager;
import zombie.Lua.LuaEventManager;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;

public class WeaponHit extends Hit implements INetworkPacketField {
    @JSONField
    protected float range;
    @JSONField
    protected boolean hitHead;

    public void set(float damage, float range, float hitForce, float hitDirectionX, float hitDirectionY, boolean hitHead) {
        this.set(damage, hitForce, hitDirectionX, hitDirectionY);
        this.range = range;
        this.hitHead = hitHead;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.range = b.getFloat();
        this.hitHead = b.get() != 0;
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putFloat(this.range);
        b.putBoolean(this.hitHead);
    }

    public void process(IsoGameCharacter wielder, IsoGameCharacter target, HandWeapon weapon, boolean ignore) {
        target.Hit(weapon, wielder, this.damage, ignore, this.range, true);
        this.process(wielder, target);
        if (wielder.isAimAtFloor() && !weapon.isRanged() && wielder.isNPC()) {
            CombatManager.getInstance().splash(target, weapon, wielder);
        }

        if (this.hitHead) {
            CombatManager.getInstance().splash(target, weapon, wielder);
            CombatManager.getInstance().splash(target, weapon, wielder);
            target.addBlood(BloodBodyPartType.Head, true, true, true);
            target.addBlood(BloodBodyPartType.Torso_Upper, true, false, false);
            target.addBlood(BloodBodyPartType.UpperArm_L, true, false, false);
            target.addBlood(BloodBodyPartType.UpperArm_R, true, false, false);
        }

        if ((!((IsoLivingCharacter)wielder).isDoShove() || wielder.isAimAtFloor())
            && wielder.DistToSquared(target) < 2.0F
            && Math.abs(wielder.getZ() - target.getZ()) < 0.5F) {
            wielder.addBlood(null, false, false, false);
        }

        if (!target.isDead() && !(target instanceof IsoPlayer) && (!((IsoLivingCharacter)wielder).isDoShove() || wielder.isAimAtFloor())) {
            CombatManager.getInstance().splash(target, weapon, wielder);
        }

        if (GameServer.server) {
            LuaEventManager.triggerEvent("OnWeaponHitXp", wielder, weapon, target, this.damage, 1);
            CombatManager.getInstance().processMaintenanceCheck(wielder, weapon, target);
            GameServer.helmetFall(target, this.hitHead);
        }
    }
}
