// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import zombie.scripting.objects.VehicleScript;

/**
 * Created by kroto on 9/20/2017.
 */
public final class LightbarSirenMode {
    private int mode;
    private final int modeMax = 3;

    public int get() {
        return this.mode;
    }

    public void set(int v) {
        if (v > 3) {
            this.mode = 3;
        } else if (v < 0) {
            this.mode = 0;
        } else {
            this.mode = v;
        }
    }

    public boolean isEnable() {
        return this.mode != 0;
    }

    public String getSoundName(VehicleScript.LightBar lightbar) {
        if (this.isEnable()) {
            if (this.mode == 1) {
                return lightbar.soundSiren0;
            }

            if (this.mode == 2) {
                return lightbar.soundSiren1;
            }

            if (this.mode == 3) {
                return lightbar.soundSiren2;
            }
        }

        return "";
    }
}
