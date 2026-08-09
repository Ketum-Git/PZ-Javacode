// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.random;

import org.uncommons.maths.random.CellularAutomatonRNG;
import org.uncommons.maths.random.SeedException;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;

public class RandLua extends RandAbstract {
    public static final RandLua INSTANCE = new RandLua();

    protected RandLua() {
    }

    @Override
    public void init() {
        try {
            this.rand = new CellularAutomatonRNG(new PZSeedGenerator());
        } catch (SeedException var2) {
            DebugType.General.printException(var2, LogSeverity.Error, "SeedException thrown.");
        }
    }
}
