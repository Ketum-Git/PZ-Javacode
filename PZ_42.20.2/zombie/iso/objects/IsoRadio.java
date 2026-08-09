// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import zombie.UsedFromLua;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.sprite.IsoSprite;

@UsedFromLua
public class IsoRadio extends IsoWaveSignal {
    public IsoRadio(IsoCell cell) {
        super(cell);
    }

    public IsoRadio(IsoCell cell, IsoGridSquare sq, IsoSprite spr) {
        super(cell, sq, spr);
    }

    @Override
    public String getObjectName() {
        return "Radio";
    }

    @Override
    public boolean couldBePoweredByGenerator() {
        return true;
    }

    @Override
    public float getGeneratorPowerConsumption() {
        return this.getDeviceData().getIsTurnedOn() && !this.getDeviceData().getIsBatteryPowered() ? 0.01F : 0.0F;
    }
}
