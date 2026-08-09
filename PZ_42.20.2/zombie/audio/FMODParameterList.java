// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import java.util.ArrayList;

public final class FMODParameterList {
    public final ArrayList<FMODParameter> parameterList = new ArrayList<>();
    public final FMODParameter[] parameterArray = new FMODParameter[128];

    public void add(FMODParameter parameter) {
        this.parameterList.add(parameter);
        if (parameter.getParameterDescription() != null) {
            this.parameterArray[parameter.getParameterDescription().globalIndex] = parameter;
        }
    }

    public FMODParameter get(FMOD_STUDIO_PARAMETER_DESCRIPTION pdesc) {
        return pdesc == null ? null : this.parameterArray[pdesc.globalIndex];
    }

    public void update() {
        for (int i = 0; i < this.parameterList.size(); i++) {
            this.parameterList.get(i).update();
        }
    }
}
