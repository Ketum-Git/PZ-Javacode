// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.recipemanager;

import java.util.ArrayDeque;
import zombie.debug.DebugLog;
import zombie.entity.components.fluids.Fluid;
import zombie.util.StringUtils;

public class SourceType {
    private static final ArrayDeque<SourceType> pool = new ArrayDeque<>();
    private String itemType;
    private Fluid sourceFluid;
    private boolean usesFluid;

    protected static SourceType alloc(String itemType) {
        return pool.isEmpty() ? new SourceType().init(itemType) : pool.pop().init(itemType);
    }

    protected static void release(SourceType o) {
        assert !pool.contains(o);

        pool.push(o.reset());
    }

    private SourceType() {
    }

    private SourceType init(String itemType) {
        this.itemType = itemType;
        if (StringUtils.startsWithIgnoreCase(this.itemType, "Fluid.".toLowerCase())) {
            this.usesFluid = true;
            String fluidType = this.itemType.substring("Fluid.".length());
            Fluid fluid = Fluid.Get(fluidType);
            if (fluid != null) {
                this.sourceFluid = fluid;
            } else {
                this.sourceFluid = null;
                DebugLog.General.error("Could not find fluid type '" + this.itemType + "'");
            }
        } else {
            this.usesFluid = false;
            this.sourceFluid = null;
        }

        return this;
    }

    private SourceType reset() {
        this.itemType = null;
        this.sourceFluid = null;
        this.usesFluid = false;
        return this;
    }

    protected boolean isUsesFluid() {
        return this.usesFluid;
    }

    protected Fluid getSourceFluid() {
        return this.sourceFluid;
    }

    protected String getItemType() {
        return this.itemType;
    }

    @Override
    public String toString() {
        return "SourceType [itemType=" + this.itemType + ", sourceFluid=" + this.sourceFluid + ", usesFluid=" + this.usesFluid + "]";
    }
}
