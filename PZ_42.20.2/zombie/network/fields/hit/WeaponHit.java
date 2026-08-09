// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import zombie.CombatManager;
import zombie.Lua.LuaEventManager;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoLivingCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;

public class WeaponHit extends Hit implements INetworkPacketField {
    private static final byte HIT_PART_HEAD = 1;
    private static final byte HIT_PART_LEGS = 2;
    private static final byte HIT_PART_OTHER = 4;
    @JSONField
    protected float range;
    @JSONField
    protected byte hitPart;
    @JSONField
    protected boolean removeKnife;

    public void set(float damage, float range, float hitForce, float hitDirectionX, float hitDirectionY, boolean hitHead, boolean hitLegs, boolean removeKnife) {
        this.set(damage, hitForce, hitDirectionX, hitDirectionY);
        this.range = range;
        if (hitHead) {
            this.hitPart = 1;
        } else if (hitLegs) {
            this.hitPart = 2;
        } else {
            this.hitPart = 4;
        }

        this.removeKnife = removeKnife;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.range = b.getFloat();
        this.hitPart = b.getByte();
        this.removeKnife = b.getBoolean();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putFloat(this.range);
        b.putByte(this.hitPart);
        b.putBoolean(this.removeKnife);
    }

    public void process(IsoGameCharacter wielder, IsoGameCharacter target, HandWeapon weapon, boolean ignore) {
        if (this.removeKnife && wielder instanceof IsoPlayer owner && target instanceof IsoZombie zombie) {
            InventoryItem item = owner.getPrimaryHandItem();
            owner.getInventory().Remove(item);
            owner.removeFromHands(item);
            zombie.setAttachedItem("JawStab", item);
            zombie.setJawStabAttach(true);
            zombie.setKnifeDeath(true);
        }

        target.Hit(weapon, wielder, this.damage, ignore, this.range, true);
        this.process(wielder, target);
        if (wielder.isAimAtFloor() && !weapon.isRanged() && wielder.isNpc()) {
            CombatManager.getInstance().splash(target, weapon, wielder);
        }

        if (this.isHitHead()) {
            CombatManager.getInstance().splash(target, weapon, wielder);
            CombatManager.getInstance().splash(target, weapon, wielder);
            target.addBlood(BloodBodyPartType.Head, true, true, true);
            target.addBlood(BloodBodyPartType.Torso_Upper, true, false, false);
            target.addBlood(BloodBodyPartType.UpperArm_L, true, false, false);
            target.addBlood(BloodBodyPartType.UpperArm_R, true, false, false);
            if (GameServer.server && target instanceof IsoPlayer player) {
                player.syncVisuals();
            }
        }

        if ((!((IsoLivingCharacter)wielder).isDoShove() || wielder.isAimAtFloor())
            && wielder.DistToSquared(target) < 2.0F
            && Math.abs(wielder.getZ() - target.getZ()) < 0.5F
            && !(wielder instanceof IsoPlayer)) {
            wielder.addBlood(null, false, false, false);
        }

        if (!target.isDead() && !(target instanceof IsoPlayer) && (!((IsoLivingCharacter)wielder).isDoShove() || wielder.isAimAtFloor())) {
            CombatManager.getInstance().splash(target, weapon, wielder);
        }

        if (GameServer.server) {
            LuaEventManager.triggerEvent("OnWeaponHitXp", wielder, weapon, target, this.damage, 1);
            CombatManager.getInstance().processMaintenanceCheck(wielder, weapon, target);
            target.helmetFall(this.isHitHead());
        }
    }

    public boolean isHitHead() {
        return this.hitPart == 1;
    }

    public boolean isHitLegs() {
        return this.hitPart == 2;
    }
}
