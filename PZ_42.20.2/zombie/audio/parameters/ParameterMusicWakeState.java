// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;

public final class ParameterMusicWakeState extends FMODGlobalParameter {
    private int playerIndex = -1;
    private ParameterMusicWakeState.State state = ParameterMusicWakeState.State.Awake;

    public ParameterMusicWakeState() {
        super("MusicWakeState");
    }

    @Override
    public float calculateCurrentValue() {
        IsoPlayer player = this.choosePlayer();
        if (player != null && this.state == ParameterMusicWakeState.State.Awake && player.isAsleep()) {
            this.state = ParameterMusicWakeState.State.Sleeping;
        }

        return this.state.label;
    }

    public void setState(IsoPlayer player, ParameterMusicWakeState.State state) {
        if (player == this.choosePlayer()) {
            this.state = state;
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
                    this.state = player.isAsleep() ? ParameterMusicWakeState.State.Sleeping : ParameterMusicWakeState.State.Awake;
                    return player;
                }
            }

            return null;
        }
    }

    public static enum State {
        Awake(0),
        Sleeping(1),
        WakeNormal(2),
        WakeNightmare(3),
        WakeZombies(4);

        final int label;

        private State(final int label) {
            this.label = label;
        }
    }
}
