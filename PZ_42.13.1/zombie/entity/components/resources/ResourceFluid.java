// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.resources;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidFilter;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.ui.ObjectTooltip;

@DebugClassFields
@UsedFromLua
public class ResourceFluid extends Resource {
    private final FluidContainer fluidContainer;
    private final FluidFilter fluidFilter = new FluidFilter();

    protected ResourceFluid() {
        this.fluidContainer = FluidContainer.CreateContainer();
    }

    @Override
    void loadBlueprint(ResourceBlueprint bp) {
        super.loadBlueprint(bp);
        this.fluidContainer.setCapacity(bp.getCapacity());
        if (this.getFilterName() != null) {
            this.fluidFilter.setFilterScript(this.getFilterName());
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout != null) {
            super.DoTooltip(tooltipUI, layout);
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel(Translator.getEntityText("EC_Stored") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            item.setValue((int)(this.getFluidRatio() * 100.0F) + " %", 1.0F, 1.0F, 1.0F, 1.0F);
            if (this.fluidContainer != null) {
                this.fluidContainer.DoTooltip(tooltipUI, layout);
            }

            if (Core.debug && DebugOptions.instance.entityDebugUi.getValue()) {
                this.DoDebugTooltip(tooltipUI, layout);
            }
        }
    }

    @Override
    protected void DoDebugTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout != null && Core.debug) {
            super.DoDebugTooltip(tooltipUI, layout);
        }
    }

    public FluidContainer getFluidContainer() {
        return this.fluidContainer;
    }

    @Override
    public boolean isFull() {
        return this.fluidContainer.isFull();
    }

    @Override
    public boolean isEmpty() {
        return this.fluidContainer.isEmpty();
    }

    @Override
    public float getFluidAmount() {
        return this.fluidContainer.getAmount();
    }

    @Override
    public float getFluidCapacity() {
        return this.fluidContainer.getCapacity();
    }

    @Override
    public float getFreeFluidCapacity() {
        return this.fluidContainer.getFreeCapacity();
    }

    public float getFluidRatio() {
        return this.getFluidCapacity() <= 0.0F ? 0.0F : PZMath.clamp_01(this.getFluidAmount() / this.getFluidCapacity());
    }

    @Override
    public boolean canDrainToItem(InventoryItem item) {
        if (!GameClient.client && !this.isEmpty()) {
            if (item != null && item.getFluidContainer() != null) {
                FluidContainer fc = item.getFluidContainer();
                if (!fc.isFull()) {
                    return FluidContainer.CanTransfer(this.fluidContainer, fc);
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public boolean drainToItem(InventoryItem item) {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return false;
        } else if (this.canDrainToItem(item)) {
            FluidContainer.Transfer(this.fluidContainer, item.getFluidContainer());
            this.setDirty();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canDrainFromItem(InventoryItem item) {
        if (!GameClient.client && !this.isFull()) {
            if (item != null && item.getFluidContainer() != null) {
                FluidContainer fc = item.getFluidContainer();
                if (!fc.isEmpty()) {
                    return FluidContainer.CanTransfer(fc, this.fluidContainer);
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public boolean drainFromItem(InventoryItem item) {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return false;
        } else if (this.canDrainFromItem(item)) {
            FluidContainer.Transfer(item.getFluidContainer(), this.fluidContainer);
            this.setDirty();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void tryTransferTo(Resource target) {
        this.tryTransferTo(target, this.getFluidAmount());
    }

    @Override
    public void tryTransferTo(Resource target, float amount) {
        if (!this.isEmpty() && target != null && !target.isFull()) {
            if (target instanceof ResourceFluid resourceFluid) {
                this.transferTo(resourceFluid, amount);
            }
        }
    }

    public void transferTo(ResourceFluid target, float transferAmount) {
        if (!this.isEmpty() && target != null && !target.isFull()) {
            float amount = PZMath.min(PZMath.min(transferAmount, this.getFluidAmount()), target.getFreeFluidCapacity());
            if (!(amount <= 0.0F)) {
                if (FluidContainer.CanTransfer(this.fluidContainer, target.fluidContainer)) {
                    FluidContainer.Transfer(this.fluidContainer, target.fluidContainer, amount);
                    this.setDirty();
                }
            }
        }
    }

    @Override
    public void clear() {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
        } else {
            this.fluidContainer.Empty();
            this.setDirty();
        }
    }

    @Override
    protected void reset() {
        super.reset();
        this.fluidContainer.Empty();
        this.fluidFilter.setFilterScript(null);
    }

    @Override
    public void saveSync(ByteBuffer output) throws IOException {
        this.save(output);
    }

    @Override
    public void loadSync(ByteBuffer input, int WorldVersion) throws IOException {
        this.load(input, WorldVersion);
    }

    @Override
    public void save(ByteBuffer output) throws IOException {
        super.save(output);
        this.fluidContainer.save(output);
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        if (this.fluidContainer != null && !this.fluidContainer.isEmpty()) {
            this.fluidContainer.Empty();
        }

        this.fluidContainer.load(input, WorldVersion);
        if (this.getFilterName() != null) {
            this.fluidFilter.setFilterScript(this.getFilterName());
        } else {
            this.fluidFilter.setFilterScript(null);
        }
    }
}
