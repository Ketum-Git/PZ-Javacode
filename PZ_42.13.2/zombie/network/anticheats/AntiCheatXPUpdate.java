// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.raknet.UdpConnection;

public class AntiCheatXPUpdate extends AbstractAntiCheat {
    private static final float MAX_XP_GROWTH_RATE = 1000.0F;

    @Override
    public boolean update(UdpConnection connection) {
        super.update(connection);
        boolean result = true;

        for (IsoPlayer player : connection.players) {
            if (player != null) {
                result &= this.update(connection, player);
            }
        }

        return result;
    }

    private boolean update(UdpConnection connection, IsoGameCharacter character) {
        AntiCheatXPUpdate.IAntiCheatUpdate field = character.getXp();
        if (this.antiCheat.isEnabled() && field.intervalCheck()) {
            float XPGrowthRate = field.getGrowthRate();
            float XPMultiplier = field.getMultiplier();
            if (XPGrowthRate > 1000.0F * XPMultiplier) {
                return false;
            }
        }

        return true;
    }

    public interface IAntiCheatUpdate {
        boolean intervalCheck();

        float getGrowthRate();

        float getMultiplier();
    }
}
