// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 1, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SyncHandWeaponFieldsPacket implements INetworkPacket {
    @JSONField
    PlayerID playerId = new PlayerID();
    @JSONField
    ContainerID containerId = new ContainerID();
    @JSONField
    int itemId;
    @JSONField
    int currentAmmoCount;
    @JSONField
    boolean roundChambered;
    @JSONField
    boolean containsClip;
    @JSONField
    int spentRoundCount;
    @JSONField
    boolean spentRoundChambered;
    @JSONField
    boolean isJammed;
    @JSONField
    float maxRange;
    @JSONField
    float minRangeRanged;
    @JSONField
    int clipSize;
    @JSONField
    int reloadTime;
    @JSONField
    int recoilDelay;
    @JSONField
    int aimingTime;
    @JSONField
    int hitChance;
    @JSONField
    float minAngle;
    @JSONField
    float minDamage;
    @JSONField
    float maxDamage;
    @JSONField
    ArrayList<WeaponPart> attachments = new ArrayList<>();
    @JSONField
    KahluaTable moddata;

    @Override
    public void setData(Object... values) {
        if (values.length != 2) {
            DebugLog.Multiplayer.error("%s.set get invalid arguments", this.getClass().getSimpleName());
        } else {
            this.playerId.set((IsoPlayer)values[0]);
            HandWeapon item = (HandWeapon)values[1];
            this.containerId.set(item.getContainer());
            this.itemId = item.getID();
            this.currentAmmoCount = item.getCurrentAmmoCount();
            this.roundChambered = item.isRoundChambered();
            this.containsClip = item.isContainsClip();
            this.spentRoundCount = item.getSpentRoundCount();
            this.spentRoundChambered = item.isSpentRoundChambered();
            this.isJammed = item.isJammed();
            this.maxRange = item.getMaxRange();
            this.minRangeRanged = item.getMinRangeRanged();
            this.clipSize = item.getClipSize();
            this.reloadTime = item.getReloadTime();
            this.recoilDelay = item.getRecoilDelay();
            this.aimingTime = item.getAimingTime();
            this.hitChance = item.getHitChance();
            this.minAngle = item.getMinAngle();
            this.minDamage = item.getMinDamage();
            this.maxDamage = item.getMaxDamage();
            this.moddata = item.hasModData() ? item.getModData() : null;
            item.getAllWeaponParts(this.attachments);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        this.containerId.write(b);
        b.putInt(this.itemId);
        b.putInt(this.currentAmmoCount);
        b.putBoolean(this.roundChambered);
        b.putBoolean(this.containsClip);
        b.putInt(this.spentRoundCount);
        b.putBoolean(this.spentRoundChambered);
        b.putBoolean(this.isJammed);
        b.putFloat(this.maxRange);
        b.putFloat(this.minRangeRanged);
        b.putInt(this.clipSize);
        b.putInt(this.reloadTime);
        b.putInt(this.recoilDelay);
        b.putInt(this.aimingTime);
        b.putInt(this.hitChance);
        b.putFloat(this.minAngle);
        b.putFloat(this.minDamage);
        b.putFloat(this.maxDamage);
        b.putByte((byte)this.attachments.size());

        try {
            for (int i = 0; i < this.attachments.size(); i++) {
                this.attachments.get(i).save(b.bb, false);
            }
        } catch (IOException var4) {
            var4.printStackTrace();
            throw new RuntimeException(var4.getMessage());
        }

        if (this.moddata != null) {
            b.putByte((byte)1);

            try {
                this.moddata.save(b.bb);
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        } else {
            b.putByte((byte)0);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerId.parse(b, connection);
        this.containerId.parse(b, connection);
        this.itemId = b.getInt();
        this.currentAmmoCount = b.getInt();
        this.roundChambered = b.get() != 0;
        this.containsClip = b.get() != 0;
        this.spentRoundCount = b.getInt();
        this.spentRoundChambered = b.get() != 0;
        this.isJammed = b.get() != 0;
        this.maxRange = b.getFloat();
        this.minRangeRanged = b.getFloat();
        this.clipSize = b.getInt();
        this.reloadTime = b.getInt();
        this.recoilDelay = b.getInt();
        this.aimingTime = b.getInt();
        this.hitChance = b.getInt();
        this.minAngle = b.getFloat();
        this.minDamage = b.getFloat();
        this.maxDamage = b.getFloat();

        try {
            WeaponPart part = null;
            byte count = b.get();
            this.attachments.clear();

            for (byte i = 0; i < count; i++) {
                short ID = b.getShort();
                b.get();
                part = (WeaponPart)InventoryItemFactory.CreateItem(ID);
                if (part != null) {
                    this.attachments.add(part);
                    part.load(b, 241);
                }
            }
        } catch (IOException var8) {
            var8.printStackTrace();
            throw new RuntimeException(var8.getMessage());
        }

        byte hasModData = b.get();
        if (hasModData > 0) {
            this.moddata = LuaManager.platform.newTable();

            try {
                this.moddata.load(b, 241);
            } catch (IOException var7) {
                var7.printStackTrace();
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.containerId.getContainer().getItemWithID(this.itemId) instanceof HandWeapon item) {
            item.setCurrentAmmoCount(this.currentAmmoCount);
            item.setRoundChambered(this.roundChambered);
            item.setContainsClip(this.containsClip);
            item.setSpentRoundCount(this.spentRoundCount);
            item.setSpentRoundChambered(this.spentRoundChambered);
            item.setJammed(this.isJammed);
            item.setMaxRange(this.maxRange);
            item.setMinRangeRanged(this.minRangeRanged);
            item.setClipSize(this.clipSize);
            item.setReloadTime(this.reloadTime);
            item.setRecoilDelay(this.recoilDelay);
            item.setAimingTime(this.aimingTime);
            item.setHitChance(this.hitChance);
            item.setMinAngle(this.minAngle);
            item.setMinDamage(this.minDamage);
            item.setMaxDamage(this.maxDamage);
            item.clearAllWeaponParts();

            for (int i = 0; i < this.attachments.size(); i++) {
                item.setWeaponPart(this.attachments.get(i));
            }

            if (item.hasModData() && this.moddata != null) {
                item.getModData().wipe();
                KahluaTableIterator iterator = this.moddata.iterator();

                while (iterator.advance()) {
                    Object key = iterator.getKey();
                    item.getModData().rawset(key, this.moddata.rawget(key));
                }
            }

            this.playerId.getPlayer().resetEquippedHandsModels();
        } else {
            DebugLog.Multiplayer.error("HandWeapon %d not found", this.itemId);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        InventoryItem item = this.containerId.getContainer().getItemWithID(this.itemId);
        if (item instanceof HandWeapon handWeapon) {
            item.setCurrentAmmoCount(this.currentAmmoCount);
            handWeapon.setRoundChambered(this.roundChambered);
            handWeapon.setContainsClip(this.containsClip);
            handWeapon.setSpentRoundCount(this.spentRoundCount);
            handWeapon.setSpentRoundChambered(this.spentRoundChambered);
            handWeapon.setJammed(this.isJammed);
        } else {
            DebugLog.Multiplayer.error("HandWeapon %d not found", this.itemId);
        }
    }
}
