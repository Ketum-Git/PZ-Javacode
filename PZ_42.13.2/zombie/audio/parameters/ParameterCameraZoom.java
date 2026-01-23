// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;

public final class ParameterCameraZoom extends FMODGlobalParameter {
    public ParameterCameraZoom() {
        super("CameraZoom");
    }

    @Override
    public float calculateCurrentValue() {
        IsoPlayer player = this.getPlayer();
        if (player == null) {
            return 0.0F;
        } else {
            float zoom = Core.getInstance().getZoom(player.playerIndex) - Core.getInstance().offscreenBuffer.getMinZoom();
            float range = Core.getInstance().offscreenBuffer.getMaxZoom() - Core.getInstance().offscreenBuffer.getMinZoom();
            return zoom / range;
        }
    }

    private IsoPlayer getPlayer() {
        IsoPlayer player = null;

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoPlayer player2 = IsoPlayer.players[i];
            if (player2 != null && (player == null || player.isDead() && player2.isAlive())) {
                player = player2;
            }
        }

        return player;
    }
}
