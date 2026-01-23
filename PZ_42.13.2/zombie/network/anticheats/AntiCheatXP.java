// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.SandboxOptions;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;
import zombie.network.packets.INetworkPacket;

public class AntiCheatXP extends AbstractAntiCheat {
    private static final float MAX_XP_GROWTH_RATE = 0.0F;

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatXP.IAntiCheat field = (AntiCheatXP.IAntiCheat)packet;
        if (connection.role.hasCapability(Capability.AddXP)) {
            return result;
        } else {
            double maxGrowthXP = 0.0 * SandboxOptions.instance.multipliersConfig.xpMultiplierGlobal.getValue();
            return field.getAmount() > maxGrowthXP ? String.format("xp=%f > max=%f", field.getAmount(), maxGrowthXP) : result;
        }
    }

    public interface IAntiCheat {
        IsoPlayer getPlayer();

        float getAmount();
    }
}
