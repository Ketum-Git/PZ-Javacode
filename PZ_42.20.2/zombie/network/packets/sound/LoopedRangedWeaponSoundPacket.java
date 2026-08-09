// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.sound;

import zombie.GameSounds;
import zombie.audio.GameSound;
import zombie.audio.LoopedRangedWeaponSounds;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class LoopedRangedWeaponSoundPacket implements INetworkPacket {
    @JSONField
    boolean stop;
    @JSONField
    short playerId;
    @JSONField
    boolean cancelPrevious;
    @JSONField
    String soundName;
    @JSONField
    float x;
    @JSONField
    float y;
    @JSONField
    byte z;
    @JSONField
    float firearmInside;
    @JSONField
    float firearmRoomSize;

    @Override
    public void setData(Object... values) {
        this.stop = (Boolean)values[0];
        this.playerId = (Short)values[1];
        if (this.stop) {
            this.cancelPrevious = (Boolean)values[2];
        } else {
            this.soundName = (String)values[2];
            this.x = (Float)values[3];
            this.y = (Float)values[4];
            this.z = ((Float)values[5]).byteValue();
            this.firearmInside = (Float)values[6];
            this.firearmRoomSize = (Float)values[7];
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isConsistent(connection)) {
            int radius = 70;
            GameSound gameSound = GameSounds.getSound(this.soundName);
            if (gameSound != null) {
                radius = Math.max(radius, (int)gameSound.getMaxDistanceOfClips());
            }

            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (c.getConnectedGUID() != connection.getConnectedGUID() && c.isFullyConnected()) {
                    IsoPlayer p = GameServer.getAnyPlayerFromConnection(c);
                    if (p != null && c.RelevantTo(this.x, this.y, radius)) {
                        ByteBufferWriter b2 = c.startPacket();
                        PacketTypes.PacketType.LoopedRangedWeaponSound.doPacket(b2);
                        this.write(b2);
                        PacketTypes.PacketType.LoopedRangedWeaponSound.send(c);
                    }
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        Object owner = LoopedRangedWeaponSounds.INSTANCE.getOwnerObjectForRemotePlayer(this.playerId);
        if (this.stop) {
            LoopedRangedWeaponSounds.INSTANCE.stopAttackLoopSound(owner, this.cancelPrevious);
        } else {
            LoopedRangedWeaponSounds.INSTANCE.setPosition(owner, this.x, this.y, this.z);
            LoopedRangedWeaponSounds.INSTANCE.setParameters(owner, this.firearmInside, this.firearmRoomSize);
            LoopedRangedWeaponSounds.INSTANCE.startAttackLoopSound(owner, this.soundName);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.stop = b.getBoolean();
        this.playerId = b.getShort();
        if (this.stop) {
            this.cancelPrevious = b.getBoolean();
        } else {
            this.soundName = b.getUTF();
            this.x = b.getFloat();
            this.y = b.getFloat();
            this.z = b.getByte();
            this.firearmInside = b.getFloat();
            this.firearmRoomSize = b.getFloat();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putBoolean(this.stop);
        b.putShort(this.playerId);
        if (this.stop) {
            b.putBoolean(this.cancelPrevious);
        } else {
            b.putUTF(this.soundName);
            b.putFloat(this.x);
            b.putFloat(this.y);
            b.putByte(this.z);
            b.putFloat(this.firearmInside);
            b.putFloat(this.firearmRoomSize);
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (GameClient.client) {
            return !StringUtils.isNullOrWhitespace(this.soundName);
        } else if (!connection.hasPlayer(this.playerId)) {
            return false;
        } else {
            IsoPlayer player = GameServer.IDToPlayerMap.get(this.playerId);
            if (player == null) {
                return false;
            } else {
                if (!this.stop) {
                    if (!(player.getPrimaryHandItem() instanceof HandWeapon weapon)) {
                        return false;
                    }

                    int ammoCount = player.isUnlimitedAmmo() ? weapon.getMaxAmmo() : weapon.getCurrentAmmoCount();
                    if (!weapon.isAimedFirearm() || ammoCount == 0) {
                        return false;
                    }
                }

                return this.stop || !StringUtils.isNullOrWhitespace(this.soundName);
            }
        }
    }
}
