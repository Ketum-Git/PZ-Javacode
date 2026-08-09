// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.core.Core;

public final class ParameterMusicLibrary extends FMODGlobalParameter {
    public ParameterMusicLibrary() {
        super("MusicLibrary");
    }

    @Override
    public float calculateCurrentValue() {
        return switch (Core.getInstance().getOptionMusicLibrary()) {
            case 2 -> ParameterMusicLibrary.Library.EarlyAccess.label;
            case 3 -> ParameterMusicLibrary.Library.Random.label;
            default -> ParameterMusicLibrary.Library.Official.label;
        };
    }

    public static enum Library {
        Official(0),
        EarlyAccess(1),
        Random(2);

        final int label;

        private Library(final int label) {
            this.label = label;
        }
    }
}
