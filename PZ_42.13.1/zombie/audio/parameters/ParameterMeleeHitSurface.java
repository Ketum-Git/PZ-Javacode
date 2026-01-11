// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterMeleeHitSurface extends FMODLocalParameter {
    private final IsoGameCharacter character;
    private ParameterMeleeHitSurface.Material material = ParameterMeleeHitSurface.Material.Default;

    public ParameterMeleeHitSurface(IsoGameCharacter character) {
        super("MeleeHitSurface");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.getMaterial().label;
    }

    private ParameterMeleeHitSurface.Material getMaterial() {
        return this.material;
    }

    public void setMaterial(ParameterMeleeHitSurface.Material material) {
        this.material = material;
    }

    public static enum Material {
        Default(0),
        Body(1),
        Fabric(2),
        Glass(3),
        Head(4),
        Metal(5),
        Plastic(6),
        Stone(7),
        Wood(8),
        GarageDoor(9),
        MetalDoor(10),
        MetalGate(11),
        PrisonMetalDoor(12),
        SlidingGlassDoor(13),
        WoodDoor(14),
        WoodGate(15),
        Tree(16);

        final int label;

        private Material(final int label) {
            this.label = label;
        }
    }
}
