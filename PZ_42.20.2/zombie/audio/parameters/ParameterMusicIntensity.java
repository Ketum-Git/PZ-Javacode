// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterMusicIntensity extends FMODGlobalParameter {
    private int playerIndex = -1;
    private ParameterMusicIntensity.Intensity intensity = ParameterMusicIntensity.Intensity.Low;

    public ParameterMusicIntensity() {
        super("MusicIntensity");
    }

    @Override
    public float calculateCurrentValue() {
        IsoPlayer player = this.choosePlayer();
        if (player == null) {
            this.intensity = ParameterMusicIntensity.Intensity.Low;
        } else {
            float intensity = player.getMusicIntensityEvents().getIntensity();
            this.intensity = intensity < 34.0F
                ? ParameterMusicIntensity.Intensity.Low
                : (intensity < 67.0F ? ParameterMusicIntensity.Intensity.Medium : ParameterMusicIntensity.Intensity.High);
        }

        return this.intensity.label;
    }

    public void setState(IsoPlayer player, ParameterMusicIntensity.Intensity state) {
        if (player == this.choosePlayer()) {
            this.intensity = state;
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
                    this.intensity = ParameterMusicIntensity.Intensity.Low;
                    return player;
                }
            }

            return null;
        }
    }

    public static enum Intensity {
        Low(0),
        Medium(1),
        High(2);

        final int label;

        private Intensity(final int label) {
            this.label = label;
        }
    }
}
