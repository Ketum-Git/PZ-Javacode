// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;
import zombie.scripting.objects.CharacterTrait;

public final class ParameterHardOfHearing extends FMODGlobalParameter {
    private int playerIndex = -1;

    public ParameterHardOfHearing() {
        super("HardOfHearing");
    }

    @Override
    public float calculateCurrentValue() {
        IsoPlayer player = this.choosePlayer();
        if (player != null) {
            return player.hasTrait(CharacterTrait.HARD_OF_HEARING) ? 1.0F : 0.0F;
        } else {
            return 0.0F;
        }
    }

    private IsoPlayer choosePlayer() {
        if (this.playerIndex != -1) {
            IsoPlayer player = IsoPlayer.players[this.playerIndex];
            if (player == null) {
                this.playerIndex = -1;
            }
        }

        if (this.playerIndex != -1) {
            return IsoPlayer.players[this.playerIndex];
        } else {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null) {
                    this.playerIndex = i;
                    return player;
                }
            }

            return null;
        }
    }
}
