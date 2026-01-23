// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.characters.Capability;
import zombie.characters.CharacterStat;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.CanModifyBodyStats, handlingType = 3)
@UsedFromLua
public class SyncPlayerStatsPacket implements INetworkPacket {
    @JSONField
    private final PlayerID playerId = new PlayerID();
    @JSONField
    private int syncParams;

    public static int getBitMaskForStat(CharacterStat stat) {
        for (int i = 0; i < CharacterStat.ORDERED_STATS.length; i++) {
            if (CharacterStat.ORDERED_STATS[i] == stat) {
                return 1 << i;
            }
        }

        return 0;
    }

    @Override
    public void setData(Object... values) {
        if (values[0] instanceof IsoPlayer) {
            this.playerId.set((IsoPlayer)values[0]);
            this.syncParams = (Integer)values[1];
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
            DebugLog.Multiplayer.printStackTrace();
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerId.parse(b, connection);
        this.syncParams = b.getInt();
        if (this.syncParams == -1) {
            this.playerId.getPlayer().getNutrition().load(b);
        } else {
            for (byte i = 0; i < CharacterStat.ORDERED_STATS.length; i++) {
                if ((this.syncParams >> i & 1) != 0) {
                    this.playerId.getPlayer().getStats().parse(b, i);
                }
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putInt(this.syncParams);
        if (this.syncParams == -1) {
            this.playerId.getPlayer().getNutrition().save(b.bb);
        } else {
            for (byte i = 0; i < CharacterStat.ORDERED_STATS.length; i++) {
                if ((this.syncParams >> i & 1) != 0) {
                    this.playerId.getPlayer().getStats().write(b.bb, i);
                }
            }
        }
    }
}
