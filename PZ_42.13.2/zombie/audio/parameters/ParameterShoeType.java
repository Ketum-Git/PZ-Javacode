// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;

public final class ParameterShoeType extends FMODLocalParameter {
    private static final ItemVisuals tempItemVisuals = new ItemVisuals();
    private final IsoGameCharacter character;
    private ParameterShoeType.ShoeType shoeType;

    public ParameterShoeType(IsoGameCharacter character) {
        super("ShoeType");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        if (this.shoeType == null) {
            this.shoeType = this.getShoeType();
        }

        return this.shoeType.label;
    }

    private ParameterShoeType.ShoeType getShoeType() {
        this.character.getItemVisuals(tempItemVisuals);
        Item shoes = null;

        for (int i = 0; i < tempItemVisuals.size(); i++) {
            ItemVisual itemVisual = tempItemVisuals.get(i);
            Item scriptItem = itemVisual.getScriptItem();
            if (scriptItem != null && scriptItem.isBodyLocation(ItemBodyLocation.SHOES)) {
                shoes = scriptItem;
                break;
            }
        }

        if (shoes == null) {
            return ParameterShoeType.ShoeType.Barefoot;
        } else {
            String type = shoes.getName();
            if (type.contains("Boots") || type.contains("Wellies")) {
                return ParameterShoeType.ShoeType.Boots;
            } else if (type.contains("FlipFlop")) {
                return ParameterShoeType.ShoeType.FlipFlops;
            } else if (type.contains("Slippers")) {
                return ParameterShoeType.ShoeType.Slippers;
            } else {
                return type.contains("Trainer") ? ParameterShoeType.ShoeType.Sneakers : ParameterShoeType.ShoeType.Shoes;
            }
        }
    }

    public void setShoeType(ParameterShoeType.ShoeType shoeType) {
        this.shoeType = shoeType;
    }

    private static enum ShoeType {
        Barefoot(0),
        Boots(1),
        FlipFlops(2),
        Shoes(3),
        Slippers(4),
        Sneakers(5);

        final int label;

        private ShoeType(final int label) {
            this.label = label;
        }
    }
}
