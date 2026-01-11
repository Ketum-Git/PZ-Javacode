// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatXP;
import zombie.network.anticheats.AntiCheatXPPlayer;
import zombie.network.fields.Perk;
import zombie.network.fields.character.PlayerID;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 3,
    anticheats = {AntiCheat.XPPlayer, AntiCheat.XP}
)
public class AddXpPacket implements INetworkPacket, AntiCheatXP.IAntiCheat, AntiCheatXPPlayer.IAntiCheat {
    @JSONField
    public final PlayerID target = new PlayerID();
    @JSONField
    protected Perk perk = new Perk();
    @JSONField
    protected float amount;
    @JSONField
    protected boolean noMultiplier;

    @Override
    public void setData(Object... values) {
        if (values.length == 3) {
            this.set((IsoPlayer)values[0], (PerkFactory.Perk)values[1], (Float)values[2]);
        } else if (values.length == 4) {
            this.set((IsoPlayer)values[0], (PerkFactory.Perk)values[1], (Float)values[2], (Boolean)values[3]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoPlayer _target, PerkFactory.Perk _perk, float _amount) {
        this.target.set(_target);
        this.perk.set(_perk);
        this.amount = _amount;
        this.noMultiplier = false;
    }

    public void set(IsoPlayer _target, PerkFactory.Perk _perk, float _amount, boolean _noMultiplier) {
        this.target.set(_target);
        this.perk.set(_perk);
        this.amount = _amount;
        this.noMultiplier = _noMultiplier;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.target.parse(b, connection);
        this.perk.parse(b, connection);
        this.amount = b.getFloat();
        this.noMultiplier = b.get() == 1;
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.target.write(b);
        this.perk.write(b);
        b.putFloat(this.amount);
        b.putBoolean(this.noMultiplier);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        addXp(connection, this.target.getPlayer(), this.perk.getPerk(), this.amount, this.noMultiplier);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.target.getPlayer() != null && !this.target.getPlayer().isDead()) {
            if (this.target.getPlayer() != null && !this.target.getPlayer().isDead()) {
                this.target.getPlayer().getXp().AddXP(this.perk.getPerk(), this.amount, false, !this.noMultiplier, true);
            }
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.target.isConsistent(connection) && this.perk.isConsistent(connection);
    }

    @Override
    public IsoPlayer getPlayer() {
        return this.target.getPlayer();
    }

    @Override
    public float getAmount() {
        return this.amount;
    }

    public static void addXp(UdpConnection connection, IsoPlayer player, PerkFactory.Perk perk, float amount, boolean noMultiplier) {
        if (!GameServer.canModifyPlayerStats(connection, player)) {
            PacketTypes.PacketAuthorization.onUnauthorized(connection, PacketTypes.PacketType.AddXP);
        } else {
            if (player != null && !player.isDead()) {
                player.getXp().AddXP(perk, amount, false, !noMultiplier, true);
                if (GameServer.canModifyPlayerStats(connection, null)) {
                    player.getXp().getGrowthRate();
                }
            }

            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (c.getConnectedGUID() == GameServer.PlayerToAddressMap.get(player)
                    || !connection.havePlayer(player) && c.getConnectedGUID() == connection.getConnectedGUID()) {
                    INetworkPacket packet = connection.getPacket(PacketTypes.PacketType.AddXP);
                    packet.setData(player, perk, amount, noMultiplier);
                    packet.sendToClient(PacketTypes.PacketType.AddXP, connection);
                }
            }
        }
    }
}
