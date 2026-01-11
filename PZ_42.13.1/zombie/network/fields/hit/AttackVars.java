// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import java.nio.ByteBuffer;
import zombie.characters.IsoLivingCharacter;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.MovingObject;
import zombie.util.list.PZArrayList;

public class AttackVars implements INetworkPacketField {
    @JSONField
    private boolean isBareHeadsWeapon;
    @JSONField
    public boolean aimAtFloor;
    @JSONField
    public boolean closeKill;
    @JSONField
    public boolean doShove;
    @JSONField
    public boolean doGrapple;
    @JSONField
    public float useChargeDelta;
    @JSONField
    public int recoilDelay;
    public final PZArrayList<HitInfo> targetsStanding = new PZArrayList<>(HitInfo.class, 8);
    public final PZArrayList<HitInfo> targetsProne = new PZArrayList<>(HitInfo.class, 8);
    public MovingObject targetOnGround = new MovingObject();
    public MovingObject targetStanding = new MovingObject();
    public float targetDistance;
    public boolean isProcessed;

    public void setWeapon(HandWeapon weapon) {
        this.isBareHeadsWeapon = "BareHands".equals(weapon.getType());
    }

    public HandWeapon getWeapon(IsoLivingCharacter owner) {
        return !this.isBareHeadsWeapon && owner.getUseHandWeapon() != null ? owner.getUseHandWeapon() : owner.bareHands;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        byte flags = b.get();
        this.isBareHeadsWeapon = AttackVars.AttackFlags.isFlagSet(flags, AttackVars.AttackFlags.isBareHeadsWeapon);
        this.aimAtFloor = AttackVars.AttackFlags.isFlagSet(flags, AttackVars.AttackFlags.aimAtFloor);
        this.closeKill = AttackVars.AttackFlags.isFlagSet(flags, AttackVars.AttackFlags.closeKill);
        this.doShove = AttackVars.AttackFlags.isFlagSet(flags, AttackVars.AttackFlags.doShove);
        this.doGrapple = AttackVars.AttackFlags.isFlagSet(flags, AttackVars.AttackFlags.doGrapple);
        this.targetOnGround.parse(b, connection);
        this.useChargeDelta = b.getFloat();
        this.recoilDelay = b.getInt();
        byte count = b.get();
        this.targetsStanding.clear();

        for (int i = 0; i < count; i++) {
            HitInfo hit = new HitInfo();
            hit.parse(b, connection);
            this.targetsStanding.add(hit);
        }

        count = b.get();
        this.targetsProne.clear();

        for (int i = 0; i < count; i++) {
            HitInfo hit = new HitInfo();
            hit.parse(b, connection);
            this.targetsProne.add(hit);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        byte flags = 0;
        flags = AttackVars.AttackFlags.setFlagState(flags, AttackVars.AttackFlags.isBareHeadsWeapon, this.isBareHeadsWeapon);
        flags = AttackVars.AttackFlags.setFlagState(flags, AttackVars.AttackFlags.aimAtFloor, this.aimAtFloor);
        flags = AttackVars.AttackFlags.setFlagState(flags, AttackVars.AttackFlags.closeKill, this.closeKill);
        flags = AttackVars.AttackFlags.setFlagState(flags, AttackVars.AttackFlags.doShove, this.doShove);
        flags = AttackVars.AttackFlags.setFlagState(flags, AttackVars.AttackFlags.doGrapple, this.doGrapple);
        b.putByte(flags);
        this.targetOnGround.write(b);
        b.putFloat(this.useChargeDelta);
        b.putInt(this.recoilDelay);
        byte count = (byte)Math.min(100, this.targetsStanding.size());
        b.putByte(count);

        for (int i = 0; i < count; i++) {
            HitInfo hit = this.targetsStanding.get(i);
            hit.write(b);
        }

        count = (byte)Math.min(100, this.targetsProne.size());
        b.putByte(count);

        for (int i = 0; i < count; i++) {
            HitInfo hit = this.targetsProne.get(i);
            hit.write(b);
        }
    }

    @Override
    public int getPacketSizeBytes() {
        int size = 11 + this.targetOnGround.getPacketSizeBytes();
        byte count = (byte)Math.min(100, this.targetsStanding.size());

        for (int i = 0; i < count; i++) {
            HitInfo hit = this.targetsStanding.get(i);
            size += hit.getPacketSizeBytes();
        }

        count = (byte)Math.min(100, this.targetsProne.size());

        for (int i = 0; i < count; i++) {
            HitInfo hit = this.targetsProne.get(i);
            size += hit.getPacketSizeBytes();
        }

        return size;
    }

    public void copy(AttackVars original) {
        this.isBareHeadsWeapon = original.isBareHeadsWeapon;
        this.targetOnGround = original.targetOnGround;
        this.aimAtFloor = original.aimAtFloor;
        this.closeKill = original.closeKill;
        this.doShove = original.doShove;
        this.doGrapple = original.doGrapple;
        this.useChargeDelta = original.useChargeDelta;
        this.recoilDelay = original.recoilDelay;
        this.targetsStanding.clear();
        byte count = (byte)Math.min(100, original.targetsStanding.size());

        for (int i = 0; i < count; i++) {
            HitInfo hit = original.targetsStanding.get(i);
            this.targetsStanding.add(hit);
        }

        this.targetsProne.clear();
        count = (byte)Math.min(100, original.targetsProne.size());

        for (int i = 0; i < count; i++) {
            HitInfo hit = original.targetsProne.get(i);
            this.targetsProne.add(hit);
        }
    }

    public void clear() {
        this.targetOnGround.set(null);
        this.targetsStanding.clear();
        this.targetsProne.clear();
        this.isProcessed = false;
    }

    public static class AttackFlags {
        public static byte isBareHeadsWeapon = 1;
        public static byte aimAtFloor = 2;
        public static byte closeKill = 4;
        public static byte doShove = 8;
        public static byte doGrapple = 16;

        public static boolean isFlagSet(byte flags, byte flag) {
            return (flags & flag) == flag;
        }

        public static boolean anyFlagsSet(byte flags, byte anyFlags) {
            return (flags & anyFlags) != 0;
        }

        public static byte setFlagState(byte in_flags, byte in_newFlag, boolean in_set) {
            return in_set ? (byte)(in_flags | in_newFlag) : (byte)(in_flags & ~in_newFlag);
        }
    }
}
