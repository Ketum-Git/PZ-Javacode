// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import fmod.fmod.FMODManager;
import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.scripting.objects.CharacterTrait;
import zombie.util.list.PZArrayList;

public final class ParameterDragMaterial extends FMODLocalParameter {
    private final IsoGameCharacter character;

    public ParameterDragMaterial(IsoGameCharacter character) {
        super("DragMaterial");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return !this.character.isDraggingCorpse() ? ParameterDragMaterial.DragMaterial.Concrete.label : this.getMaterial().label;
    }

    private ParameterDragMaterial.DragMaterial getMaterial() {
        if (FMODManager.instance.getNumListeners() == 1) {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null && player != this.character && !player.hasTrait(CharacterTrait.DEAF)) {
                    if (PZMath.fastfloor(player.getZ()) < PZMath.fastfloor(this.character.getZ())) {
                        return ParameterDragMaterial.DragMaterial.Upstairs;
                    }
                    break;
                }
            }
        }

        IsoObject solidFloor = null;
        IsoObject staircase = null;
        IsoObject withMaterial = null;
        IsoGridSquare square = this.character.getCurrentSquare();
        if (square != null) {
            if (IsoWorld.instance.currentCell.gridSquareIsSnow(square.x, square.y, square.z)) {
                return ParameterDragMaterial.DragMaterial.Snow;
            }

            PZArrayList<IsoObject> objects = square.getObjects();

            for (int ix = 0; ix < objects.size(); ix++) {
                IsoObject object = objects.get(ix);
                if (!(object instanceof IsoWorldInventoryObject)) {
                    PropertyContainer props = object.getProperties();
                    if (props != null) {
                        if (props.has(IsoFlagType.solidfloor)) {
                            ;
                        }

                        if (object.isStairsObject()) {
                            staircase = object;
                        }

                        if (props.has("FootstepMaterial")) {
                            withMaterial = object;
                        }
                    }
                }
            }
        }

        if (withMaterial != null) {
            try {
                String material = withMaterial.getProperties().get("FootstepMaterial");
                return ParameterDragMaterial.DragMaterial.valueOf(material);
            } catch (IllegalArgumentException var9) {
                boolean var13 = true;
            }
        }

        return staircase != null ? ParameterDragMaterial.DragMaterial.Wood : ParameterDragMaterial.DragMaterial.Concrete;
    }

    static enum DragMaterial {
        Upstairs(0),
        BrokenGlass(1),
        Concrete(2),
        Grass(3),
        Gravel(4),
        Puddle(5),
        Snow(6),
        Wood(7),
        Carpet(8),
        Dirt(9),
        Sand(10),
        Ceramic(11),
        Metal(12);

        final int label;

        private DragMaterial(final int label) {
            this.label = label;
        }
    }
}
