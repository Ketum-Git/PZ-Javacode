// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.WeaponType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 0, reliability = 2, requiredCapability = Capability.TeleportToCoordinates, handlingType = 3)
public class TeleportPacket implements INetworkPacket {
    @JSONField
    PlayerID player = new PlayerID();
    @JSONField
    float x;
    @JSONField
    float y;
    @JSONField
    float z;

    @Override
    public void setData(Object... values) {
        this.player.set((IsoPlayer)values[0]);
        this.x = (Float)values[1];
        this.y = (Float)values[2];
        this.z = (Float)values[3];
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.player.write(b);
        b.putFloat(this.x);
        b.putFloat(this.y);
        b.putFloat(this.z);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.player.parse(b, connection);
        this.x = b.getFloat();
        this.y = b.getFloat();
        this.z = b.getFloat();
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoPlayer playerObj = this.player.getPlayer();
        if (playerObj != null && !playerObj.isDead()) {
            if (playerObj.getVehicle() != null) {
                playerObj.getVehicle().exit(playerObj);
                LuaEventManager.triggerEvent("OnExitVehicle", playerObj);
            }

            playerObj.setSitOnFurnitureObject(null);
            playerObj.getNetworkCharacterAI().resetState();
            playerObj.setX(this.x);
            playerObj.setY(this.y);
            playerObj.setZ(this.z);
            playerObj.setLastX(playerObj.getX());
            playerObj.setLastY(playerObj.getY());
            playerObj.setLastZ(playerObj.getZ());
            GameClient.instance.sendPlayer(playerObj);
            WeaponType weaponType = WeaponType.getWeaponType(playerObj, playerObj.getPrimaryHandItem(), playerObj.getSecondaryHandItem());
            playerObj.setVariable("Weapon", weaponType.getType());
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoPlayer playerObj = this.player.getPlayer();
        if (playerObj != null) {
            UdpConnection c = GameServer.getConnectionFromPlayer(playerObj);
            if (c != null) {
                GameServer.sendTeleport(playerObj, this.x, this.y, this.z);
                if (playerObj.isAsleep()) {
                    playerObj.setAsleep(false);
                    playerObj.setAsleepTime(0.0F);
                    INetworkPacket.sendToAll(PacketTypes.PacketType.WakeUpPlayer, playerObj);
                }
            }
        }
    }
}
