// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterMusicThreat extends FMODGlobalParameter {
    private int playerIndex = -1;
    private ParameterMusicThreat.ThreatLevel threatLevel = ParameterMusicThreat.ThreatLevel.Low;

    public ParameterMusicThreat() {
        super("MusicThreat");
    }

    @Override
    public float calculateCurrentValue() {
        IsoPlayer player = this.choosePlayer();
        if (player == null) {
            this.threatLevel = ParameterMusicThreat.ThreatLevel.Low;
        } else {
            float intensity = player.getMusicThreatStatuses().getIntensity();
            this.threatLevel = intensity < 34.0F
                ? ParameterMusicThreat.ThreatLevel.Low
                : (intensity < 67.0F ? ParameterMusicThreat.ThreatLevel.Medium : ParameterMusicThreat.ThreatLevel.High);
        }

        return this.threatLevel.label;
    }

    public void setState(IsoPlayer player, ParameterMusicThreat.ThreatLevel state) {
        if (player == this.choosePlayer()) {
            this.threatLevel = state;
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
                    this.threatLevel = ParameterMusicThreat.ThreatLevel.Low;
                    return player;
                }
            }

            return null;
        }
    }

    public static enum ThreatLevel {
        Low(0),
        Medium(1),
        High(2);

        final int label;

        private ThreatLevel(final int label) {
            this.label = label;
        }
    }
}
