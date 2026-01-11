// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyPart;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
@UsedFromLua
public class BodyPartSyncPacket implements INetworkPacket {
    public static final long BD_Health = 1L;
    public static final long BD_bandaged = 2L;
    public static final long BD_bitten = 4L;
    public static final long BD_bleeding = 8L;
    public static final long BD_IsBleedingStemmed = 16L;
    public static final long BD_IsCauterized = 32L;
    public static final long BD_scratched = 64L;
    public static final long BD_stitched = 128L;
    public static final long BD_deepWounded = 256L;
    public static final long BD_IsInfected = 512L;
    public static final long BD_IsFakeInfected = 1024L;
    public static final long BD_bandageLife = 2048L;
    public static final long BD_scratchTime = 4096L;
    public static final long BD_biteTime = 8192L;
    public static final long BD_alcoholicBandage = 16384L;
    public static final long BD_woundInfectionLevel = 32768L;
    public static final long BD_infectedWound = 65536L;
    public static final long BD_bleedingTime = 131072L;
    public static final long BD_deepWoundTime = 262144L;
    public static final long BD_haveGlass = 524288L;
    public static final long BD_stitchTime = 1048576L;
    public static final long BD_alcoholLevel = 2097152L;
    public static final long BD_additionalPain = 4194304L;
    public static final long BD_bandageType = 8388608L;
    public static final long BD_getBandageXp = 16777216L;
    public static final long BD_getStitchXp = 33554432L;
    public static final long BD_getSplintXp = 67108864L;
    public static final long BD_fractureTime = 134217728L;
    public static final long BD_splint = 268435456L;
    public static final long BD_splintFactor = 536870912L;
    public static final long BD_haveBullet = 1073741824L;
    public static final long BD_burnTime = 2147483648L;
    public static final long BD_needBurnWash = 4294967296L;
    public static final long BD_lastTimeBurnWash = 8589934592L;
    public static final long BD_splintItem = 17179869184L;
    public static final long BD_plantainFactor = 34359738368L;
    public static final long BD_comfreyFactor = 68719476736L;
    public static final long BD_garlicFactor = 137438953472L;
    public static final long BD_cut = 274877906944L;
    public static final long BD_cutTime = 549755813888L;
    public static final long BD_stiffness = 1099511627776L;
    public static final long BD_BodyDamage = 2199023255552L;
    @JSONField
    PlayerID playerId = new PlayerID();
    @JSONField
    BodyPart bodyPart;
    @JSONField
    long syncParams;

    @Override
    public void setData(Object... values) {
        this.bodyPart = (BodyPart)values[0];
        this.playerId.set((IsoPlayer)this.bodyPart.getParentChar());
        this.syncParams = (Long)values[1];
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerId.parse(b, connection);
        byte bodyPartIdx = b.get();
        this.bodyPart = this.playerId.getPlayer().getBodyDamage().getBodyParts().get(bodyPartIdx);
        this.syncParams = b.getLong();

        for (int i = 0; i < 42; i++) {
            if ((this.syncParams >> i & 1L) > 0L) {
                this.bodyPart.sync(b, (byte)(i + 1));
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putByte((byte)this.bodyPart.getIndex());
        b.putLong(this.syncParams);

        for (int i = 0; i < 42; i++) {
            if ((this.syncParams >> i & 1L) > 0L) {
                this.bodyPart.syncWrite(b, i + 1);
            }
        }
    }
}
