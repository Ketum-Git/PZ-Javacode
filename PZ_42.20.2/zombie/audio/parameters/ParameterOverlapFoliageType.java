// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.debug.DebugOptions;
import zombie.iso.IsoGridSquare;
import zombie.iso.SpriteDetails.IsoObjectType;

public final class ParameterOverlapFoliageType extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterOverlapFoliageType(IsoGameCharacter character) {
        super("OverlapFoliageType");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.getFoliageType().ordinal();
    }

    private ParameterOverlapFoliageType.FoliageType getFoliageType() {
        IsoGridSquare current = this.character.getCurrentSquare();
        if (current == null) {
            return ParameterOverlapFoliageType.FoliageType.None;
        } else if (this.character.isInvisible() && !DebugOptions.instance.character.debug.playSoundWhenInvisible.getValue()) {
            return ParameterOverlapFoliageType.FoliageType.None;
        } else {
            return !current.has(IsoObjectType.tree) && !current.hasBush()
                ? ParameterOverlapFoliageType.FoliageType.None
                : ParameterOverlapFoliageType.FoliageType.Bush;
        }
    }

    static enum FoliageType {
        None,
        Bush;
    }
}
