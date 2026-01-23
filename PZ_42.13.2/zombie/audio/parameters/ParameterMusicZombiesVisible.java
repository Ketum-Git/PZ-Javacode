// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;

public final class ParameterMusicZombiesVisible extends FMODGlobalParameter {
    private int playerIndex = -1;

    public ParameterMusicZombiesVisible() {
        super("MusicZombiesVisible");
    }

    @Override
    public float calculateCurrentValue() {
        IsoPlayer player = this.choosePlayer();
        return player != null ? PZMath.clamp(player.getStats().musicZombiesVisible, 0, 50) : 0.0F;
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
