// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.core.Core;

public final class ParameterMusicActionStyle extends FMODGlobalParameter {
    public ParameterMusicActionStyle() {
        super("MusicActionStyle");
    }

    @Override
    public float calculateCurrentValue() {
        return Core.getInstance().getOptionMusicActionStyle() == 2
            ? ParameterMusicActionStyle.State.Legacy.label
            : ParameterMusicActionStyle.State.Official.label;
    }

    public static enum State {
        Official(0),
        Legacy(1);

        final int label;

        private State(final int label) {
            this.label = label;
        }
    }
}
