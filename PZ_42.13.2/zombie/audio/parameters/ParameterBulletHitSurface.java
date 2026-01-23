// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterBulletHitSurface extends FMODLocalParameter {
    private final IsoGameCharacter character;
    private ParameterBulletHitSurface.Material material = ParameterBulletHitSurface.Material.Default;

    public ParameterBulletHitSurface(IsoGameCharacter character) {
        super("BulletHitSurface");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.getMaterial().label;
    }

    private ParameterBulletHitSurface.Material getMaterial() {
        return this.material;
    }

    public void setMaterial(ParameterBulletHitSurface.Material material) {
        this.material = material;
    }

    public static enum Material {
        Default(0),
        Flesh(1),
        Flesh_Hollow(2),
        Concrete(3),
        Plaster(4),
        Stone(5),
        Wood(6),
        Wood_Solid(7),
        Brick(8),
        Metal(9),
        Metal_Large(10),
        Metal_Light(11),
        Metal_Solid(12),
        Glass(13),
        Glass_Light(14),
        Glass_Solid(15),
        Cinderblock(16),
        Plastic(17),
        Ceramic(18),
        Rubber(19),
        Fabric(20),
        Carpet(21),
        Dirt(22),
        Grass(23),
        Gravel(24),
        Sand(25),
        Snow(26);

        public final int label;

        private Material(final int label) {
            this.label = label;
        }
    }
}
