// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.characters.CharacterTimedActions.BaseAction;

public final class ParameterActionProgressPercent extends FMODLocalParameter {
    private final IsoPlayer character;
    private boolean wasAction;

    public ParameterActionProgressPercent(IsoPlayer character) {
        super("ActionProgressPercent");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        if (this.character.getCharacterActions().isEmpty()) {
            return this.checkWasAction();
        } else {
            BaseAction action = this.character.getCharacterActions().get(0);
            if (action == null) {
                return this.checkWasAction();
            } else if (!action.started) {
                return this.checkWasAction();
            } else if (action.maxTime == 0) {
                return this.checkWasAction();
            } else if (action.finished()) {
                return 100.0F;
            } else {
                this.wasAction = action.delta > 0.0F;
                return action.delta * 100.0F;
            }
        }
    }

    private float checkWasAction() {
        if (this.wasAction) {
            this.wasAction = false;
            return 100.0F;
        } else {
            return 0.0F;
        }
    }
}
