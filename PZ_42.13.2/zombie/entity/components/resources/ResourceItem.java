// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.resources;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.components.crafting.CraftUtil;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemFilter;
import zombie.inventory.types.DrainableComboItem;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.objects.Item;
import zombie.ui.ObjectTooltip;

@DebugClassFields
@UsedFromLua
public class ResourceItem extends Resource {
    private final ItemFilter itemFilter = new ItemFilter();
    private final ArrayList<InventoryItem> storedItems = new ArrayList<>();
    private float capacity;
    private boolean stackAnyItem;

    protected ResourceItem() {
    }

    @Override
    void loadBlueprint(ResourceBlueprint bp) {
        super.loadBlueprint(bp);
        this.capacity = bp.getCapacity();
        this.stackAnyItem = bp.isStackAnyItem();
        if (this.getFilterName() != null) {
            this.itemFilter.setFilterScript(this.getFilterName());
        }
    }

    public ItemFilter getItemFilter() {
        return this.itemFilter;
    }

    public int storedSize() {
        return this.storedItems.size();
    }

    public boolean isStackAnyItem() {
        return this.stackAnyItem;
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI) {
        ObjectTooltip.Layout layout = tooltipUI.beginLayout();
        layout.setMinLabelWidth(80);
        int y = tooltipUI.padTop;
        this.DoTooltip(tooltipUI, layout);
        y = layout.render(tooltipUI.padLeft, y, tooltipUI);
        tooltipUI.endLayout(layout);
        layout = tooltipUI.beginLayout();
        layout.setMinLabelWidth(80);
        if (!this.isEmpty()) {
            HashSet<Item> renderedItems = new HashSet<>();
            int lineSpacing = tooltipUI.getLineSpacing();
            int tooltipY = y;

            for (int i = 0; i < this.storedSize(); i++) {
                InventoryItem inventoryItem = this.peekItem(i);
                if (inventoryItem != null && !renderedItems.contains(inventoryItem.getScriptItem())) {
                    inventoryItem.DoTooltipEmbedded(tooltipUI, layout, tooltipY);
                    tooltipY += lineSpacing;
                    renderedItems.add(inventoryItem.getScriptItem());
                }
            }
        }

        if (Core.debug && DebugOptions.instance.entityDebugUi.getValue()) {
            this.DoDebugTooltip(tooltipUI, layout);
        }

        y = layout.render(tooltipUI.padLeft, PZMath.max(y, layout.offsetY), tooltipUI);
        tooltipUI.endLayout(layout);
        y += tooltipUI.padBottom;
        tooltipUI.setHeight(y);
        if (tooltipUI.getWidth() < 150.0) {
            tooltipUI.setWidth(150.0);
        }
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout != null) {
            super.DoTooltip(tooltipUI, layout);
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel(Translator.getEntityText("EC_Stored") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
            item.setValue(this.getItemAmount() + "/" + this.getItemCapacity(), 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    protected void DoDebugTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        if (layout != null && Core.debug) {
            super.DoDebugTooltip(tooltipUI, layout);
            float a = 0.7F;
            ObjectTooltip.LayoutItem item = layout.addItem();
            item.setLabel("UsesAmount:", 0.7F, 0.7F, 0.56F, 1.0F);
            item.setValue(this.getItemUsesAmount() + "", 0.7F, 0.7F, 0.7F, 1.0F);
            item = layout.addItem();
            item.setLabel("FluidAmount:", 0.7F, 0.7F, 0.56F, 1.0F);
            item.setValue(this.getFluidAmount() + "", 0.7F, 0.7F, 0.7F, 1.0F);
            item = layout.addItem();
            item.setLabel("EnergyAmount:", 0.7F, 0.7F, 0.56F, 1.0F);
            item.setValue(this.getEnergyAmount() + "", 0.7F, 0.7F, 0.7F, 1.0F);
        }
    }

    @Override
    public boolean isFull() {
        return this.storedItems.size() >= this.capacity;
    }

    @Override
    public boolean isEmpty() {
        return this.storedItems.isEmpty();
    }

    @Override
    public int getItemAmount() {
        return this.storedItems.size();
    }

    public int getItemAmount(Item itemType) {
        int count = 0;

        for (InventoryItem invItem : this.storedItems) {
            if (invItem != null && invItem.getScriptItem() == itemType) {
                count++;
            }
        }

        return count;
    }

    @Override
    public float getItemUses(InputScript inputScript) {
        float itemUses = 0.0F;
        boolean isItemCount = inputScript != null && inputScript.isItemCount();
        ResourceType resourceType = inputScript != null ? inputScript.getResourceType() : ResourceType.Item;

        for (InventoryItem item : this.storedItems) {
            switch (resourceType) {
                case Item:
                    itemUses += isItemCount ? 1.0F : item.getCurrentUses();
                    break;
                case Fluid:
                    FluidContainer fc = item.getFluidContainer();
                    if (fc != null) {
                        itemUses += fc.getAmount();
                    }
            }
        }

        return itemUses;
    }

    @Override
    public float getFluidAmount() {
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0F;
        } else {
            return itemObj.getFluidContainer() != null ? itemObj.getFluidContainer().getAmount() : 0.0F;
        }
    }

    @Override
    public float getEnergyAmount() {
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0F;
        } else {
            return itemObj instanceof DrainableComboItem comboItem && comboItem.isEnergy() ? comboItem.getCurrentUsesFloat() : 0.0F;
        }
    }

    @Override
    public float getItemUsesAmount() {
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0F;
        } else {
            return itemObj instanceof DrainableComboItem ? itemObj.getCurrentUsesFloat() : 0.0F;
        }
    }

    @Override
    public int getItemCapacity() {
        return (int)this.capacity;
    }

    @Override
    public float getFluidCapacity() {
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0F;
        } else {
            return itemObj.getFluidContainer() != null ? itemObj.getFluidContainer().getCapacity() : 0.0F;
        }
    }

    @Override
    public float getEnergyCapacity() {
        return this.getItemUsesCapacity();
    }

    @Override
    public float getItemUsesCapacity() {
        return 1.0F;
    }

    @Override
    public int getFreeItemCapacity() {
        return (int)this.capacity - this.storedItems.size();
    }

    @Override
    public float getFreeFluidCapacity() {
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0F;
        } else {
            return itemObj.getFluidContainer() != null ? itemObj.getFluidContainer().getFreeCapacity() : 0.0F;
        }
    }

    @Override
    public float getFreeEnergyCapacity() {
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0F;
        } else {
            return itemObj instanceof DrainableComboItem comboItem && comboItem.isEnergy() ? this.getEnergyCapacity() - comboItem.getCurrentUsesFloat() : 0.0F;
        }
    }

    @Override
    public float getFreeItemUsesCapacity() {
        InventoryItem itemObj = this.peekItem();
        if (itemObj == null) {
            return 0.0F;
        } else {
            return itemObj instanceof DrainableComboItem ? this.getItemUsesCapacity() - itemObj.getCurrentUsesFloat() : 0.0F;
        }
    }

    @Override
    public boolean containsItem(InventoryItem item) {
        return this.storedItems.contains(item);
    }

    @Override
    public boolean acceptsItem(InventoryItem item, boolean ignoreFilters) {
        if (item == null) {
            return false;
        } else {
            if (!this.isLocked() && !this.isFull()) {
                boolean canStack = CraftUtil.canItemsStack(this.peekItem(), item, true) || this.stackAnyItem;
                if (canStack && !this.containsItem(item)) {
                    return ignoreFilters || this.itemFilter.allows(item);
                }
            }

            return false;
        }
    }

    @Override
    public boolean canStackItem(InventoryItem item) {
        if (item == null) {
            return false;
        } else if (this.isEmpty()) {
            return this.itemFilter.allows(item);
        } else {
            boolean canStack = CraftUtil.canItemsStack(this.peekItem(), item, true) || this.stackAnyItem;
            return !this.isFull() && canStack ? this.itemFilter.allows(item) : false;
        }
    }

    @Override
    public boolean canStackItem(Item item) {
        if (item == null) {
            return false;
        } else if (this.isEmpty()) {
            return this.itemFilter.allows(item);
        } else {
            Item peekItem = this.peekItem().getScriptItem();
            boolean canStack = CraftUtil.canItemsStack(peekItem, item, true) || this.stackAnyItem;
            return !this.isFull() && canStack ? this.itemFilter.allows(item) : false;
        }
    }

    @Override
    public InventoryItem offerItem(InventoryItem item, boolean ignoreFilters) {
        return this.offerItem(item, ignoreFilters, false, true);
    }

    @Override
    public InventoryItem offerItem(InventoryItem item, boolean ignoreFilters, boolean force, boolean syncEntity) {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return item;
        } else if (!force || item != null && !this.containsItem(item) && !this.isFull()) {
            if (!force && !this.acceptsItem(item, ignoreFilters)) {
                return item;
            } else {
                ItemContainer container = item.getContainer();
                if (container != null) {
                    if (!InventoryItem.RemoveFromContainer(item)) {
                        if (Core.debug) {
                            throw new RuntimeException("WARNING OfferItem -> item not removed from container.");
                        }

                        return item;
                    }

                    if (GameServer.server) {
                        GameServer.sendRemoveItemFromContainer(container, item);
                    }
                }

                this.storedItems.add(item);
                this.setDirty();
                if (syncEntity && GameServer.server) {
                    this.sync();
                }

                return null;
            }
        } else {
            return item;
        }
    }

    public ArrayList<InventoryItem> offerItems(ArrayList<InventoryItem> items) {
        return this.offerItems(items, false);
    }

    public ArrayList<InventoryItem> offerItems(ArrayList<InventoryItem> items, boolean ignoreFilters) {
        ArrayList<InventoryItem> added = new ArrayList<>();
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return added;
        } else {
            for (InventoryItem item : items) {
                if (this.isFull()) {
                    break;
                }

                if (this.offerItem(item, ignoreFilters) == null) {
                    added.add(item);
                }
            }

            return added;
        }
    }

    public ArrayList<InventoryItem> removeAllItems(ArrayList<InventoryItem> list) {
        return this.removeAllItems(list, null);
    }

    public ArrayList<InventoryItem> removeAllItems(ArrayList<InventoryItem> list, Item itemType) {
        if (list == null) {
            list = new ArrayList<>();
        }

        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return list;
        } else {
            if (!this.isLocked() && !this.storedItems.isEmpty()) {
                if (itemType != null) {
                    InventoryItem inventoryItem = null;

                    for (int i = this.storedItems.size() - 1; i >= 0; i--) {
                        inventoryItem = this.storedItems.get(i);
                        if (inventoryItem != null && inventoryItem.getScriptItem() == itemType) {
                            list.add(inventoryItem);
                            this.storedItems.remove(i);
                        }
                    }
                } else {
                    list.addAll(this.storedItems);
                    this.storedItems.clear();
                }
            }

            if (!list.isEmpty()) {
                this.setDirty();
            }

            return list;
        }
    }

    @Override
    public InventoryItem pollItem() {
        return this.pollItem(false, true);
    }

    @Override
    public InventoryItem pollItem(boolean force, boolean syncEntity) {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return null;
        } else if ((force || !this.isLocked()) && !this.storedItems.isEmpty()) {
            InventoryItem item = this.storedItems.remove(this.storedItems.size() - 1);
            this.setDirty();
            if (syncEntity && GameServer.server) {
                this.sync();
            }

            return item;
        } else {
            return null;
        }
    }

    @Override
    public InventoryItem peekItem() {
        return this.peekItem(0);
    }

    public InventoryItem removeItem(InventoryItem item) {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
            return null;
        } else if (!this.containsItem(item)) {
            return null;
        } else {
            this.storedItems.remove(item);
            this.setDirty();
            if (GameServer.server) {
                this.sync();
            }

            return item;
        }
    }

    @Override
    public InventoryItem peekItem(int offset) {
        if (!this.storedItems.isEmpty()) {
            int index = this.storedItems.size() - 1;
            if (offset > 0) {
                index -= offset;
                if (index < 0) {
                    return null;
                }
            }

            return this.storedItems.get(index);
        } else {
            return null;
        }
    }

    public ArrayList<InventoryItem> getStoredItems() {
        return this.storedItems;
    }

    public ArrayList<InventoryItem> getStoredItemsOfType(Item itemType) {
        ArrayList<InventoryItem> output = new ArrayList<>();

        for (InventoryItem inventoryItem : this.storedItems) {
            if (inventoryItem != null && !output.contains(inventoryItem) && inventoryItem.getScriptItem() == itemType) {
                output.add(inventoryItem);
            }
        }

        return output;
    }

    public ArrayList<Item> getUniqueItems() {
        ArrayList<Item> output = new ArrayList<>();

        for (InventoryItem inventoryItem : this.storedItems) {
            if (inventoryItem != null && !output.contains(inventoryItem.getScriptItem())) {
                output.add(inventoryItem.getScriptItem());
            }
        }

        return output;
    }

    @Override
    public void tryTransferTo(Resource target) {
        if (!this.isEmpty() && target != null && !target.isFull()) {
            if (target instanceof ResourceItem resourceItem) {
                this.transferTo(resourceItem, this.getItemAmount());
            }
        }
    }

    @Override
    public void tryTransferTo(Resource target, float amount) {
        if (!this.isEmpty() && target != null && !target.isFull()) {
            if (target instanceof ResourceItem resourceItem) {
                this.transferTo(resourceItem, (int)amount);
            }
        }
    }

    public void transferTo(ResourceItem target, int transferAmount) {
        if (!this.isEmpty() && target != null && !target.isFull()) {
            int amount = PZMath.min(PZMath.min(transferAmount, this.getItemAmount()), target.getFreeItemCapacity());
            if (amount > 0) {
                for (int i = 0; i < amount; i++) {
                    InventoryItem item = this.pollItem();
                    if (target.offerItem(item) != null) {
                        this.storedItems.add(item);
                        break;
                    }
                }

                this.setDirty();
            }
        }
    }

    @Override
    public void clear() {
        if (GameClient.client) {
            DebugLog.General.warn("Not allowed on client");
        } else {
            this.storedItems.clear();
            this.setDirty();
        }
    }

    @Override
    protected void reset() {
        super.reset();
        this.storedItems.clear();
        this.capacity = 0.0F;
        this.itemFilter.setFilterScript(null);
        this.stackAnyItem = false;
    }

    @Override
    public void saveSync(ByteBuffer output) throws IOException {
        super.saveSync(output);
        output.putFloat(this.capacity);
        output.put((byte)(this.stackAnyItem ? 1 : 0));
        output.putInt(this.storedItems.size());
        if (!this.storedItems.isEmpty()) {
            String type = this.storedItems.get(0).getFullType();
            GameWindow.WriteString(output, type);

            for (int i = 0; i < this.storedItems.size(); i++) {
                InventoryItem item = this.storedItems.get(i);
                if (!item.getFullType().equals(type)) {
                    throw new IOException("Type mismatch '" + item.getFullType() + "' vs '" + type + "'");
                }

                item.saveWithSize(output, true);
            }
        }
    }

    @Override
    public void loadSync(ByteBuffer input, int WorldVersion) throws IOException {
        super.loadSync(input, WorldVersion);
        this.capacity = input.getFloat();
        if (WorldVersion >= 238) {
            this.stackAnyItem = input.get() == 1;
        }

        int size = input.getInt();
        if (size == 0) {
            this.storedItems.clear();
        } else {
            String expectedType = GameWindow.ReadString(input);
            int posReset = input.position();
            boolean loaded = false;
            if (this.storedItems.size() == size) {
                try {
                    loaded = this.tryLoadSyncItems(input, WorldVersion, size, expectedType, false);
                } catch (Exception var9) {
                    if (Core.debug) {
                        DebugLog.General.warn("Unable to load items (may be ignored)");
                    }
                }
            }

            if (!loaded) {
                try {
                    input.position(posReset);
                    this.tryLoadSyncItems(input, WorldVersion, size, expectedType, true);
                } catch (Exception var8) {
                    var8.printStackTrace();
                }
            }
        }
    }

    public boolean tryLoadSyncItems(ByteBuffer input, int WorldVersion, int size, String type, boolean forceCreate) throws IOException {
        if (forceCreate) {
            this.storedItems.clear();
        }

        for (int i = 0; i < size; i++) {
            InventoryItem item;
            if (forceCreate) {
                item = InventoryItemFactory.CreateItem(type);
            } else {
                item = this.storedItems.get(i);
                if (!item.getFullType().equals(type)) {
                    throw new IOException("Type mismatch '" + item.getFullType() + "' vs '" + type + "'");
                }
            }

            InventoryItem.loadItem(input, WorldVersion, true, item);
        }

        return true;
    }

    @Override
    public void save(ByteBuffer output) throws IOException {
        super.save(output);
        output.putFloat(this.capacity);
        output.put((byte)(this.stackAnyItem ? 1 : 0));
        if (this.storedItems.size() == 1) {
            CompressIdenticalItems.save(output, this.storedItems.get(0));
        } else {
            CompressIdenticalItems.save(output, this.storedItems, null);
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        this.capacity = input.getFloat();
        if (WorldVersion >= 238) {
            this.stackAnyItem = input.get() == 1;
        }

        this.storedItems.clear();
        CompressIdenticalItems.load(input, WorldVersion, this.storedItems, null);
        if (this.getFilterName() != null) {
            this.itemFilter.setFilterScript(this.getFilterName());
        } else {
            this.itemFilter.setFilterScript(null);
        }
    }
}
