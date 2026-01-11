// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import fmod.javafmod;
import fmod.fmod.FMODManager;

public abstract class FMODGlobalParameter extends FMODParameter {
    public FMODGlobalParameter(String name) {
        super(name);
        if (this.getParameterDescription() != null) {
            if (!this.getParameterDescription().isGlobal()) {
                boolean var2 = true;
            } else {
                FMODManager.instance.addGlobalParameter(this);
            }
        }
    }

    @Override
    public void setCurrentValue(float value) {
        javafmod.FMOD_Studio_System_SetParameterByID(this.getParameterID(), value, false);
    }

    @Override
    public void startEventInstance(long eventInstance) {
    }

    @Override
    public void stopEventInstance(long eventInstance) {
    }
}
