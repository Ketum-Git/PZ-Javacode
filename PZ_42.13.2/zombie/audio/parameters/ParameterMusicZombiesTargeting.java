// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;
import zombie.characters.Stats;
import zombie.core.math.PZMath;

public final class ParameterMusicZombiesTargeting extends FMODGlobalParameter {
    private int playerIndex = -1;

    public ParameterMusicZombiesTargeting() {
        super("MusicZombiesTargeting");
    }

    @Override
    public float calculateCurrentValue() {
        IsoPlayer player = this.choosePlayer();
        if (player != null) {
            Stats stats = player.getStats();
            return PZMath.clamp(
                stats.musicZombiesTargetingDistantNotMoving
                    + stats.musicZombiesTargetingNearbyNotMoving
                    + stats.musicZombiesTargetingDistantMoving
                    + stats.musicZombiesTargetingNearbyMoving,
                0,
                50
            );
        } else {
            return 0.0F;
        }
    }

    private IsoPlayer choosePlayer() {
        if (this.playerIndex != -1) {
            IsoPlayer player = IsoPlayer.players[this.playerIndex];
            if (player == null || player.isDead()) {
                this.playerIndex = -1;
            }
        }

        if (this.playerIndex != -1) {
            return IsoPlayer.players[this.playerIndex];
        } else {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null && !player.isDead()) {
                    this.playerIndex = i;
                    return player;
                }
            }

            return null;
        }
    }
}
