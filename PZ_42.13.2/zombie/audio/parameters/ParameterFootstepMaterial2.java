// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoBrokenGlass;
import zombie.iso.sprite.IsoSprite;

public final class ParameterFootstepMaterial2 extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterFootstepMaterial2(IsoGameCharacter character) {
        super("FootstepMaterial2");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.getMaterial().label;
    }

    private ParameterFootstepMaterial2.FootstepMaterial2 getMaterial() {
        IsoGridSquare square = this.character.getCurrentSquare();
        if (square == null) {
            return ParameterFootstepMaterial2.FootstepMaterial2.None;
        } else {
            IsoBrokenGlass brokenGlass = square.getBrokenGlass();
            if (brokenGlass != null) {
                return ParameterFootstepMaterial2.FootstepMaterial2.BrokenGlass;
            } else {
                for (int i = 0; i < square.getObjects().size(); i++) {
                    IsoObject object = square.getObjects().get(i);
                    IsoSprite sprite = object.getSprite();
                    if (sprite != null && ("d_trash_1".equals(sprite.tilesetName) || "trash_01".equals(sprite.tilesetName))) {
                        return ParameterFootstepMaterial2.FootstepMaterial2.Garbage;
                    }
                }

                float puddles = square.getPuddlesInGround();
                if (puddles > 0.5F) {
                    return ParameterFootstepMaterial2.FootstepMaterial2.PuddleDeep;
                } else {
                    return puddles > 0.1F ? ParameterFootstepMaterial2.FootstepMaterial2.PuddleShallow : ParameterFootstepMaterial2.FootstepMaterial2.None;
                }
            }
        }
    }

    static enum FootstepMaterial2 {
        None(0),
        BrokenGlass(1),
        PuddleShallow(2),
        PuddleDeep(3),
        Garbage(4);

        final int label;

        private FootstepMaterial2(final int label) {
            this.label = label;
        }
    }
}
