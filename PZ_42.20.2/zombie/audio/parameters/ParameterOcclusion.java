// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import fmod.fmod.FMODSoundEmitter;
import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;

public final class ParameterOcclusion extends FMODLocalParameter {
    private final FMODSoundEmitter emitter;
    private float currentValue = Float.NaN;

    public ParameterOcclusion(FMODSoundEmitter emitter) {
        super("Occlusion");
        this.emitter = emitter;
    }

    @Override
    public float calculateCurrentValue() {
        float occlusion = 1.0F;

        for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
            float value = this.calculateValueForPlayer(playerIndex);
            occlusion = PZMath.min(occlusion, value);
        }

        this.currentValue = occlusion;
        return (int)(this.currentValue * 1000.0F) / 1000.0F;
    }

    @Override
    public void resetToDefault() {
        this.currentValue = Float.NaN;
    }

    private float calculateValueForPlayer(int playerIndex) {
        IsoPlayer player = IsoPlayer.players[playerIndex];
        if (player == null) {
            return 1.0F;
        } else {
            IsoGridSquare sqPlayer = player.getCurrentSquare();
            IsoGridSquare sqSound = IsoWorld.instance.getCell().getGridSquare((double)this.emitter.x, (double)this.emitter.y, (double)this.emitter.z);
            if (sqSound == null) {
                boolean occlusion = true;
            }

            float occlusion = 0.0F;
            if (sqPlayer != null && sqSound != null && !sqSound.isCouldSee(playerIndex)) {
                occlusion = 1.0F;
            }

            return occlusion;
        }
    }
}
