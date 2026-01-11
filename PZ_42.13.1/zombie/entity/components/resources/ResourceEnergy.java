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
import zombie.entity.energy.Energy;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.network.GameClient;
import zombie.ui.ObjectTooltip;

@DebugClassFields
@UsedFromLua
public class ResourceEnergy extends Resource {
    private float storedEnergy;
    private Energy energy;
    private float capacity;

    protected ResourceEnergy() {
    }

    @Override
    void loadBlueprint(ResourceBlueprint bp) {
        super.loadBlueprint(bp);
        this.capacity = bp.getCapacity();
        this.setEnergyType(this.getFilterName());
    }

    private void setEnergyType(String energyType) {
        if (energyType != null) {
            this.energy = Energy.Get(energyType);
            if (this.energy == null) {
                DebugLog.General.warn("Energy not found: " + energyType);
            }
        } else {
            DebugLog.General.warn("Energy Type is null!");
            this.energy = Energy.VoidEnergy;
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout != null) {
            super.DoTooltip(tooltipUI, layout);
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel(Translator.getEntityText("EC_Stored") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            item.setValue((int)(this.getEnergyRatio() * 100.0F) + " %", 1.0F, 1.0F, 1.0F, 1.0F);
            if (this.energy != null) {
                item = layout.addItem();
                item.setLabel(Translator.getEntityText("EC_Energy") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                item.setValue(this.energy.getDisplayName(), 1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                item = layout.addItem();
                item.setLabel(Translator.getEntityText("EC_Energy_Not_Set"), 1.0F, 1.0F, 0.8F, 1.0F);
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
            float a = 0.7F;
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel("EnergyStored:", 0.7F, 0.7F, 0.56F, 1.0F);
            item.setValue(this.storedEnergy + "", 0.7F, 0.7F, 0.7F, 1.0F);
            item = layout.addItem();
            item.setLabel("EnergyCapacity:", 0.7F, 0.7F, 0.56F, 1.0F);
            item.setValue(this.capacity + "", 0.7F, 0.7F, 0.7F, 1.0F);
        }
    }

    @Override
    public boolean isFull() {
        return this.storedEnergy >= this.capacity;
    }

    @Override
    public boolean isEmpty() {
        return this.storedEnergy <= 0.0F;
    }

    public Energy getEnergy() {
        return this.energy;
    }

    @Override
    public float getEnergyAmount() {
        return this.storedEnergy;
    }

    @Override
    public float getEnergyCapacity() {
        return this.capacity;
    }

    @Override
    public float getFreeEnergyCapacity() {
        return this.getEnergyCapacity() - this.getEnergyAmount();
    }

    public float getEnergyRatio() {
        return this.capacity <= 0.0F ? 0.0F : PZMath.clamp_01(this.storedEnergy / this.capacity);
    }

    public boolean setEnergyAmount(float amount) {
        if (GameClient.client) {
            return false;
        } else {
            amount = PZMath.min(PZMath.max(0.0F, amount), this.capacity);
            if (this.storedEnergy != amount) {
                this.storedEnergy = amount;
                this.setDirty();
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean canDrainToItem(InventoryItem item) {
        if (!GameClient.client && !this.isEmpty()) {
            return item != null && item instanceof DrainableComboItem comboItem && !comboItem.isFullUses() && comboItem.isEnergy()
                ? this.getEnergy().equals(comboItem.getEnergy())
                : false;
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
            DrainableComboItem comboItem = (DrainableComboItem)item;
            float transfer = PZMath.min(1.0F - comboItem.getCurrentUsesFloat(), this.getEnergyAmount());
            this.setEnergyAmount(this.getEnergyAmount() - transfer);
            int transferint = (int)(comboItem.getMaxUses() * transfer);
            comboItem.setCurrentUses(comboItem.getCurrentUses() + transferint);
            this.setDirty();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canDrainFromItem(InventoryItem item) {
        if (!GameClient.client && !this.isFull()) {
            return item != null && item instanceof DrainableComboItem comboItem && !comboItem.isEmptyUses() && comboItem.isEnergy()
                ? this.getEnergy().equals(comboItem.getEnergy())
                : false;
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
            DrainableComboItem comboItem = (DrainableComboItem)item;
            float transfer = PZMath.min(this.getFreeEnergyCapacity(), comboItem.getCurrentUsesFloat());
            int transferint = (int)(comboItem.getMaxUses() * transfer);
            comboItem.setCurrentUses(comboItem.getCurrentUses() - transferint);
            this.setEnergyAmount(this.getEnergyAmount() + transfer);
            this.setDirty();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void tryTransferTo(Resource target) {
        this.tryTransferTo(target, this.getEnergyAmount());
    }

    @Override
    public void tryTransferTo(Resource target, float amount) {
        if (!this.isEmpty() && target != null && !target.isFull()) {
            if (target instanceof ResourceEnergy resourceEnergy) {
                this.transferTo(resourceEnergy, amount);
            }
        }
    }

    public void transferTo(ResourceEnergy target, float transferAmount) {
        if (!this.isEmpty() && target != null && !target.isFull()) {
            float amount = PZMath.min(PZMath.min(transferAmount, this.getEnergyAmount()), target.getFreeEnergyCapacity());
            if (!(amount <= 0.0F)) {
                this.setEnergyAmount(this.getEnergyAmount() - amount);
                target.setEnergyAmount(target.getEnergyAmount() + amount);
                this.setDirty();
            }
        }
    }

    @Override
    public void clear() {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
        } else {
            this.storedEnergy = 0.0F;
            this.setDirty();
        }
    }

    @Override
    protected void reset() {
        super.reset();
        this.storedEnergy = 0.0F;
        this.capacity = 0.0F;
        this.energy = null;
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
        Energy.saveEnergy(this.energy, output);
        output.putFloat(this.capacity);
        output.putFloat(this.storedEnergy);
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        this.storedEnergy = 0.0F;
        this.energy = Energy.loadEnergy(input, WorldVersion);
        this.capacity = input.getFloat();
        this.storedEnergy = input.getFloat();
    }
}
