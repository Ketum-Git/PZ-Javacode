// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import fmod.javafmod;
import gnu.trove.list.array.TLongArrayList;

public class FMODLocalParameter extends FMODParameter {
    private final TLongArrayList instances = new TLongArrayList();

    public FMODLocalParameter(String name) {
        super(name);
        if (this.getParameterDescription() != null && this.getParameterDescription().isGlobal()) {
            boolean var2 = true;
        }
    }

    @Override
    public float calculateCurrentValue() {
        return 0.0F;
    }

    @Override
    public void setCurrentValue(float value) {
        for (int i = 0; i < this.instances.size(); i++) {
            long inst = this.instances.get(i);
            javafmod.FMOD_Studio_EventInstance_SetParameterByID(inst, this.getParameterID(), value, false);
        }
    }

    @Override
    public void startEventInstance(long inst) {
        this.instances.add(inst);
        javafmod.FMOD_Studio_EventInstance_SetParameterByID(inst, this.getParameterID(), this.getCurrentValue(), false);
    }

    @Override
    public void stopEventInstance(long inst) {
        this.instances.remove(inst);
    }
}
