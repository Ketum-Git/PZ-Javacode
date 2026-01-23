// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import java.nio.ByteBuffer;
import zombie.AttackType;
import zombie.EffectsManager;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.ServerOptions;
import zombie.network.fields.INetworkPacketField;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;

public class Player extends Character implements INetworkPacketField {
    @JSONField
    protected short playerIndex;
    @JSONField
    protected short playerFlags;
    @JSONField
    protected float charge;
    @JSONField
    protected float perkAiming;
    @JSONField
    protected float combatSpeed;
    @JSONField
    protected AttackType attackType;
    @JSONField
    protected AttackVars attackVars = new AttackVars();
    private final PZArrayList<HitInfo> hitList = new PZArrayList<>(HitInfo.class, 8);

    public void set(IsoPlayer player, boolean isCriticalHit) {
        this.set(player);
        this.playerFlags = 0;
        this.playerFlags = (short)(this.playerFlags | (short)(player.isAimAtFloor() ? 1 : 0));
        this.playerFlags = (short)(this.playerFlags | (short)(player.isDoShove() ? 2 : 0));
        this.playerFlags = (short)(this.playerFlags | (short)(player.isAttackFromBehind() ? 4 : 0));
        this.playerFlags |= (short)(isCriticalHit ? 8 : 0);
        this.playerFlags = (short)(this.playerFlags | (short)(player.isDoGrapple() ? 16 : 0));
        this.playerFlags = (short)(this.playerFlags | (short)(player.isDeathDragDown() ? 32 : 0));
        this.charge = player.useChargeDelta;
        this.perkAiming = player.getPerkLevel(PerkFactory.Perks.Aiming);
        this.combatSpeed = player.getVariableFloat("CombatSpeed", 1.0F);
        this.attackType = player.getAttackType();
        this.attackVars.copy(player.getAttackVars());
        this.hitList.clear();
        PZArrayUtil.addAll(this.hitList, player.getHitInfoList());
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.playerIndex = b.getShort();
        this.playerFlags = b.getShort();
        this.charge = b.getFloat();
        this.perkAiming = b.getFloat();
        this.combatSpeed = b.getFloat();
        this.attackType = AttackType.valueOf(GameWindow.ReadString(b));
        this.attackVars.parse(b, connection);
        byte count = b.get();

        for (int i = 0; i < count; i++) {
            HitInfo hit = new HitInfo();
            hit.parse(b, connection);
            this.hitList.add(hit);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putShort(this.playerIndex);
        b.putShort(this.playerFlags);
        b.putFloat(this.charge);
        b.putFloat(this.perkAiming);
        b.putFloat(this.combatSpeed);
        b.putUTF(this.attackType.name());
        this.attackVars.write(b);
        byte count = (byte)this.hitList.size();
        b.putByte(count);

        for (int i = 0; i < count; i++) {
            this.hitList.get(i).write(b);
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection);
    }

    @Override
    public void process() {
        super.process();
        this.getPlayer().useChargeDelta = this.charge;
        this.getPlayer().setVariable("recoilVarX", this.perkAiming / 10.0F);
        this.getPlayer().setAttackType(this.attackType);
        this.getPlayer().setVariable("CombatSpeed", this.combatSpeed);
        this.getPlayer().setVariable("AimFloorAnim", (this.playerFlags & 1) != 0);
        this.getPlayer().setAimAtFloor((this.playerFlags & 1) != 0);
        this.getPlayer().setDoShove((this.playerFlags & 2) != 0);
        this.getPlayer().setDoGrapple((this.playerFlags & 16) != 0);
        this.getPlayer().setAttackFromBehind((this.playerFlags & 4) != 0);
        this.getPlayer().setCriticalHit((this.playerFlags & 8) != 0);
        this.getPlayer().setDeathDragDown((this.playerFlags & 32) != 0);
    }

    public void attack(HandWeapon weapon, boolean isPVP) {
        if (GameClient.client) {
            this.getPlayer().setAttackStarted(false);
            this.getPlayer().getAttackVars().copy(this.attackVars);
            this.getPlayer().getHitInfoList().clear();
            PZArrayUtil.addAll(this.getPlayer().getHitInfoList(), this.hitList);
            this.getPlayer().pressedAttack();
            if (this.getPlayer().isAttackStarted() && weapon.isRanged() && !this.getPlayer().isDoShove() && !this.getPlayer().isDoGrapple()) {
                EffectsManager.getInstance().startMuzzleFlash(this.getPlayer(), 1);
            }

            if (weapon.getPhysicsObject() != null) {
                this.getPlayer().Throw(weapon);
            }
        } else if (GameServer.server) {
            if (isPVP && !this.getPlayer().getSafety().isEnabled()) {
                this.getPlayer()
                    .getSafety()
                    .setCooldown(this.getPlayer().getSafety().getCooldown() + ServerOptions.getInstance().safetyCooldownTimer.getValue());
                GameServer.sendChangeSafety(this.getPlayer().getSafety());
            }

            if (weapon != null && weapon.isAimedFirearm()) {
                LuaEventManager.triggerEvent("OnWeaponSwingHitPoint", this.getPlayer(), weapon);
            }
        }
    }

    public IsoPlayer getPlayer() {
        return Type.tryCastTo(this.getCharacter(), IsoPlayer.class);
    }

    public boolean isRelevant(UdpConnection connection) {
        return connection.RelevantTo(this.positionX, this.positionY);
    }
}
