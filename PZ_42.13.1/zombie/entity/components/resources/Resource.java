// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.resources;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugNonRecursive;
import zombie.entity.GameEntity;
import zombie.entity.util.enums.EnumBitStore;
import zombie.inventory.InventoryItem;
import zombie.network.GameClient;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.objects.Item;
import zombie.ui.ObjectTooltip;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@DebugClassFields
@UsedFromLua
public abstract class Resource {
    @DebugNonRecursive
    protected Resources resourcesComponent;
    protected ResourceGroup group;
    private boolean isLocked;
    private double progress;
    private String id;
    private ResourceType resourceType = ResourceType.Any;
    private ResourceIO resourceIo = ResourceIO.Any;
    private ResourceChannel channel = ResourceChannel.NO_CHANNEL;
    private final EnumBitStore<ResourceFlag> flags = EnumBitStore.noneOf(ResourceFlag.class);
    private String filterName;
    private boolean dirty = true;

    protected Resource() {
    }

    void setGroup(ResourceGroup group) {
        this.group = group;
    }

    ResourceGroup getGroup() {
        return this.group;
    }

    void setResourcesComponent(Resources resources) {
        this.resourcesComponent = resources;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void setDirty() {
        if (!GameClient.client) {
            if (this.resourcesComponent != null) {
                this.dirty = true;
                this.resourcesComponent.setDirty();
                if (this.group != null) {
                    this.group.setDirty();
                }
            } else if (Core.debug) {
                throw new IllegalStateException("ResourceComponent (currently) not set, cannot perform.");
            }
        }
    }

    protected void resetDirty() {
        this.dirty = false;
    }

    public Resources getResourcesComponent() {
        if (this.resourcesComponent == null) {
            DebugLog.CraftLogic.warn("ResourcesComponent (currently) not set!");
        }

        return this.resourcesComponent;
    }

    public GameEntity getGameEntity() {
        if (this.resourcesComponent != null) {
            return this.resourcesComponent.getGameEntity();
        } else if (Core.debug) {
            throw new IllegalStateException("ResourceComponent (currently) not set, cannot perform.");
        } else {
            return null;
        }
    }

    public void DoTooltip(ObjectTooltip tooltipUI) {
        ObjectTooltip.Layout layout = tooltipUI.beginLayout();
        layout.setMinLabelWidth(80);
        int y = tooltipUI.padTop;
        this.DoTooltip(tooltipUI, layout);
        y = layout.render(tooltipUI.padLeft, y, tooltipUI);
        tooltipUI.endLayout(layout);
        y += tooltipUI.padBottom;
        tooltipUI.setHeight(y);
        if (tooltipUI.getWidth() < 150.0) {
            tooltipUI.setWidth(150.0);
        }
    }

    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout != null) {
            if (this.isLocked) {
                ObjectTooltip.LayoutItem item = layout.addItem();
                item.setLabel(Translator.getEntityText("EC_Locked") + ":", 0.9F, 0.3F, 0.3F, 1.0F);
            }

            if (this.progress > 0.0) {
                ObjectTooltip.LayoutItem item = layout.addItem();
                item.setLabel(Translator.getEntityText("EC_Progress") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                item.setProgress((float)this.progress, 0.0F, 1.0F, 0.0F, 1.0F);
            }

            if (!this.isEmpty()) {
                ;
            }
        }
    }

    protected void DoDebugTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout != null && Core.debug) {
            float a = 0.7F;
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel("[DEBUG_INFO]", 0.7F, 0.7F, 0.56F, 1.0F);
            item = layout.addItem();
            item.setLabel("id:", 0.7F, 0.7F, 0.56F, 1.0F);
            item.setValue(this.id, 0.7F, 0.7F, 0.7F, 1.0F);
            item = layout.addItem();
            item.setLabel("ResourceType:", 0.7F, 0.7F, 0.56F, 1.0F);
            item.setValue(this.resourceType.toString(), 0.7F, 0.7F, 0.7F, 1.0F);
            item = layout.addItem();
            item.setLabel("ResourceIO:", 0.7F, 0.7F, 0.56F, 1.0F);
            item.setValue(this.resourceIo.toString(), 0.7F, 0.7F, 0.7F, 1.0F);
            item = layout.addItem();
            item.setLabel("Channel-#:", 0.7F, 0.7F, 0.56F, 1.0F);
            item.setValue(this.channel.toString(), 0.7F, 0.7F, 0.7F, 1.0F);
            item = layout.addItem();
            item.setLabel("Flags:", 0.7F, 0.7F, 0.56F, 1.0F);
            item.setValue(this.flags.toString(), 0.7F, 0.7F, 0.7F, 1.0F);
            item = layout.addItem();
            item.setLabel("Dirty:", 0.7F, 0.7F, 0.56F, 1.0F);
            item.setValue(Boolean.toString(this.dirty), 0.7F, 0.7F, 0.7F, 1.0F);
            if (this.group != null) {
                item = layout.addItem();
                item.setLabel("Group:", 0.7F, 0.7F, 0.56F, 1.0F);
                item.setValue(this.group.getName(), 0.7F, 0.7F, 0.7F, 1.0F);
            }

            if (this.progress > 0.0) {
                item = layout.addItem();
                item.setLabel("Progress:", 0.7F, 0.7F, 0.56F, 1.0F);
                item.setValue(Double.toString(this.progress), 0.7F, 0.7F, 0.7F, 1.0F);
            }
        }
    }

    public String getId() {
        return this.id;
    }

    public ResourceType getType() {
        return this.resourceType;
    }

    public ResourceIO getIO() {
        return this.resourceIo;
    }

    public ResourceChannel getChannel() {
        return this.channel;
    }

    public boolean isAutoDecay() {
        return this.flags.contains(ResourceFlag.AutoDecay);
    }

    public boolean hasFlag(ResourceFlag flag) {
        return this.flags.contains(flag);
    }

    public String getDebugFlagsString() {
        return this.flags.toString();
    }

    public String getFilterName() {
        return this.filterName;
    }

    void loadBlueprint(ResourceBlueprint bp) {
        this.id = bp.getId();
        this.resourceType = bp.getType();
        this.resourceIo = bp.getIO();
        this.channel = bp.getChannel();
        this.flags.setBits(bp.getFlagBits());
        this.filterName = bp.getFilter();
    }

    public void setProgress(double progress) {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
        } else {
            progress = PZMath.clampDouble_01(progress);
            if (this.progress != progress) {
                this.progress = PZMath.clampDouble_01(progress);
                this.setDirty();
            }
        }
    }

    public double getProgress() {
        return this.progress;
    }

    public boolean isLocked() {
        return this.isLocked;
    }

    public void setLocked(boolean locked) {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
        } else {
            if (this.isLocked != locked) {
                this.isLocked = locked;
                this.setDirty();
            }
        }
    }

    public abstract boolean isFull();

    public abstract boolean isEmpty();

    public int getItemAmount() {
        return 0;
    }

    public float getItemUses(InputScript inputScript) {
        return 0.0F;
    }

    public float getFluidAmount() {
        return 0.0F;
    }

    public float getEnergyAmount() {
        return 0.0F;
    }

    public float getItemUsesAmount() {
        return 0.0F;
    }

    public int getItemCapacity() {
        return 0;
    }

    public float getFluidCapacity() {
        return 0.0F;
    }

    public float getEnergyCapacity() {
        return 0.0F;
    }

    public float getItemUsesCapacity() {
        return 0.0F;
    }

    public int getFreeItemCapacity() {
        return 0;
    }

    public float getFreeFluidCapacity() {
        return 0.0F;
    }

    public float getFreeEnergyCapacity() {
        return 0.0F;
    }

    public float getFreeItemUsesCapacity() {
        return 0.0F;
    }

    public boolean canMoveItemsToOutput() {
        return true;
    }

    public boolean containsItem(InventoryItem item) {
        return false;
    }

    public final boolean acceptsItem(InventoryItem item) {
        return this.acceptsItem(item, false);
    }

    public boolean acceptsItem(InventoryItem item, boolean ignoreFilters) {
        return false;
    }

    public boolean canStackItem(InventoryItem item) {
        return false;
    }

    public boolean canStackItem(Item item) {
        return false;
    }

    public final InventoryItem offerItem(InventoryItem item) {
        return this.offerItem(item, false);
    }

    public InventoryItem offerItem(InventoryItem item, boolean ignoreFilters) {
        return item;
    }

    public InventoryItem offerItem(InventoryItem item, boolean ignoreFilters, boolean force, boolean syncEntity) {
        return item;
    }

    public InventoryItem pollItem() {
        return null;
    }

    public InventoryItem pollItem(boolean force, boolean syncEntity) {
        return null;
    }

    public InventoryItem peekItem() {
        return null;
    }

    public InventoryItem peekItem(int offset) {
        return null;
    }

    public boolean canDrainToItem(InventoryItem item) {
        return false;
    }

    public boolean drainToItem(InventoryItem item) {
        return false;
    }

    public boolean canDrainFromItem(InventoryItem item) {
        return false;
    }

    public boolean drainFromItem(InventoryItem item) {
        return false;
    }

    public void tryTransferTo(Resource target) {
    }

    public void tryTransferTo(Resource target, float amount) {
    }

    public abstract void clear();

    protected void reset() {
        this.resourcesComponent = null;
        this.id = null;
        this.resourceType = ResourceType.Any;
        this.resourceIo = ResourceIO.Any;
        this.channel = ResourceChannel.NO_CHANNEL;
        this.flags.clear();
        this.filterName = null;
        this.progress = 0.0;
        this.isLocked = false;
        this.dirty = false;
    }

    public void saveSync(ByteBuffer output) throws IOException {
        output.put((byte)(this.isLocked ? 1 : 0));
        output.putDouble(this.progress);
    }

    public void loadSync(ByteBuffer input, int WorldVersion) throws IOException {
        this.isLocked = input.get() == 1;
        this.progress = input.getDouble();
    }

    public void save(ByteBuffer output) throws IOException {
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, output);
        GameWindow.WriteString(output, this.id);
        output.put(this.resourceType.getId());
        output.put(this.resourceIo.getId());
        if (this.isLocked) {
            header.addFlags(1);
        }

        if (this.progress > 0.0) {
            header.addFlags(2);
            output.putDouble(this.progress);
        }

        if (this.channel != ResourceChannel.NO_CHANNEL) {
            header.addFlags(4);
            output.put(this.channel.getByteId());
        }

        if (!this.flags.isEmpty()) {
            header.addFlags(8);
            this.flags.save(output);
        }

        header.write();
        header.release();
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
        this.isLocked = false;
        this.progress = 0.0;
        this.id = null;
        this.flags.clear();
        this.id = GameWindow.ReadString(input);
        this.resourceType = ResourceType.fromId(input.get());
        this.resourceIo = ResourceIO.fromId(input.get());
        if (header.hasFlags(1)) {
            this.isLocked = true;
        }

        if (header.hasFlags(2)) {
            this.progress = input.getDouble();
        }

        if (header.hasFlags(4)) {
            this.channel = ResourceChannel.fromId(input.get());
        }

        if (header.hasFlags(8)) {
            this.flags.load(input);
        }

        header.release();
    }
}
