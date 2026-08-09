// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.types.HandWeapon;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 5, priority = 2, reliability = 0, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class PlayerEmptyShotPacket extends PlayerID implements INetworkPacket {
    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0]);
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoPlayer player = this.getPlayer();
        HandWeapon weapon = player.getAttackVars().getWeapon(player);
        if (weapon != null && weapon.isAimedFirearm()) {
            this.getPlayer().setInitiateAttack(true);
            this.getPlayer().setAttackStarted(true);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        INetworkPacket.sendToRelative(PacketTypes.PacketType.PlayerEmptyShot, connection, this.getPlayer().getX(), this.getPlayer().getY(), this.getPlayer());
    }
}
