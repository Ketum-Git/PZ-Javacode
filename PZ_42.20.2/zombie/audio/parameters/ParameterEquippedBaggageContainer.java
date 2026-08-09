// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoGameCharacter;

public final class ParameterEquippedBaggageContainer extends FMODLocalParameter {
    private final IsoGameCharacter character;
    private ParameterEquippedBaggageContainer.ContainerType containerType = ParameterEquippedBaggageContainer.ContainerType.None;

    public ParameterEquippedBaggageContainer(IsoGameCharacter character) {
        super("EquippedBaggageContainer");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        return this.containerType.label;
    }

    public void setContainerType(ParameterEquippedBaggageContainer.ContainerType containerType) {
        this.containerType = containerType;
    }

    public void setContainerType(String containerType) {
        if (containerType != null) {
            try {
                this.containerType = ParameterEquippedBaggageContainer.ContainerType.valueOf(containerType);
            } catch (IllegalArgumentException var3) {
            }
        }
    }

    public static enum ContainerType {
        None(0),
        HikingBag(1),
        DuffleBag(2),
        PlasticBag(3),
        SchoolBag(4),
        ToteBag(5),
        GarbageBag(6);

        public final int label;

        private ContainerType(final int label) {
            this.label = label;
        }
    }
}
