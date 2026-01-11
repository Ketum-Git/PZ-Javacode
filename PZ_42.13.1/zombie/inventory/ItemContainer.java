// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.vm.LuaClosure;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.SurvivorDesc;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.inventory.types.AlarmClock;
import zombie.inventory.types.AlarmClockClothing;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Drainable;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Key;
import zombie.inventory.types.Literature;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.util.StringUtils;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleDoor;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public final class ItemContainer {
    private static final ArrayList<InventoryItem> tempList = new ArrayList<>();
    private static final ThreadLocal<ArrayList<IsoObject>> s_tempObjects = ThreadLocal.withInitial(ArrayList::new);
    public boolean active;
    private boolean dirty = true;
    public boolean isdevice;
    public float ageFactor = 1.0F;
    public float cookingFactor = 1.0F;
    public int capacity = 50;
    public InventoryItem containingItem;
    public ArrayList<InventoryItem> items = new ArrayList<>();
    public ArrayList<InventoryItem> includingObsoleteItems = new ArrayList<>();
    public IsoObject parent;
    public IsoGridSquare sourceGrid;
    public VehiclePart vehiclePart;
    public InventoryContainer inventoryContainer;
    public boolean explored;
    public String type = "none";
    public int id;
    private boolean drawDirty = true;
    private float customTemperature;
    private boolean hasBeenLooted;
    private String openSound;
    private String closeSound;
    private String putSound;
    private String takeSound;
    private String onlyAcceptCategory;
    private String acceptItemFunction;
    private int weightReduction;
    private String containerPosition;
    private String freezerPosition;
    private static final int MAX_CAPACITY = 100;
    private static final int MAX_CAPACITY_BAG = 50;
    private static final int MAX_CAPACITY_VEHICLE = 1000;
    private static final ThreadLocal<ItemContainer.Comparators> TL_comparators = ThreadLocal.withInitial(ItemContainer.Comparators::new);
    private static final ThreadLocal<ItemContainer.InventoryItemListPool> TL_itemListPool = ThreadLocal.withInitial(ItemContainer.InventoryItemListPool::new);
    private static final ThreadLocal<ItemContainer.Predicates> TL_predicates = ThreadLocal.withInitial(ItemContainer.Predicates::new);

    public ItemContainer(int id, String containerName, IsoGridSquare square, IsoObject parent) {
        this.id = id;
        this.parent = parent;
        this.type = containerName;
        this.sourceGrid = square;
        if (containerName.equals("fridge")) {
            this.ageFactor = 0.02F;
            this.cookingFactor = 0.0F;
        }
    }

    public ItemContainer(String containerName, IsoGridSquare square, IsoObject parent) {
        this.id = -1;
        this.parent = parent;
        this.type = containerName;
        this.sourceGrid = square;
        if (containerName.equals("fridge")) {
            this.ageFactor = 0.02F;
            this.cookingFactor = 0.0F;
        }
    }

    public ItemContainer(int id) {
        this.id = id;
    }

    public ItemContainer() {
        this.id = -1;
    }

    public static float floatingPointCorrection(float val) {
        int pow = 100;
        float tmp = val * 100.0F;
        return (int)(tmp - (int)tmp >= 0.5F ? tmp + 1.0F : tmp) / 100.0F;
    }

    public int getCapacity() {
        int capacity = this.capacity;
        if (this.isOccupiedVehicleSeat()) {
            capacity /= 4;
        }

        if (this.parent instanceof BaseVehicle) {
            capacity = Math.min(capacity, 1000);
        } else if (this.containingItem != null && this.containingItem instanceof InventoryItem) {
            capacity = Math.min(capacity, 50);
        }

        return Math.min(capacity, 100);
    }

    public void setCapacity(int capacity) {
        if (this.parent instanceof BaseVehicle && this.capacity > 1000) {
            DebugLog.General.warn("Attempting to set capacity of " + this + "over maximum capacity of 1000");
        } else if (this.containingItem != null && this.containingItem instanceof InventoryItem && this.capacity > 50) {
            DebugLog.General.warn("Attempting to set capacity of " + this.containingItem + "over maximum capacity of 50");
        } else if (capacity > 100) {
            DebugLog.General.warn("Attempting to set capacity of " + this + "over maximum capacity of 100");
        }

        this.capacity = capacity;
    }

    public InventoryItem FindAndReturnWaterItem(int uses) {
        for (int i = 0; i < this.getItems().size(); i++) {
            InventoryItem item = this.getItems().get(i);
            if (item instanceof DrainableComboItem drainableItem && item.isWaterSource() && drainableItem.getCurrentUses() >= uses) {
                return item;
            }
        }

        return null;
    }

    public InventoryItem getItemFromTypeRecurse(String type) {
        return this.getFirstTypeRecurse(type);
    }

    public int getEffectiveCapacity(IsoGameCharacter chr) {
        int capacity = this.getCapacity();
        if (chr != null && !(this.parent instanceof IsoGameCharacter) && !(this.parent instanceof IsoDeadBody) && !"floor".equals(this.getType())) {
            if (chr.hasTrait(CharacterTrait.ORGANIZED)) {
                return (int)Math.max(capacity * 1.3F, (float)(capacity + 1));
            }

            if (chr.hasTrait(CharacterTrait.DISORGANIZED)) {
                return (int)Math.max(capacity * 0.7F, 1.0F);
            }
        }

        return capacity;
    }

    public boolean hasRoomFor(IsoGameCharacter chr, InventoryItem item) {
        if (chr != null && chr.getVehicle() != null && item.hasTag(ItemTag.HEAVY_ITEM) && this.parent instanceof IsoGameCharacter) {
            return false;
        } else if (!this.isItemAllowed(item)) {
            return false;
        } else if (this.containingItem != null
            && this.containingItem.getEquipParent() != null
            && this.containingItem.getEquipParent().getInventory() != null
            && !this.containingItem.getEquipParent().getInventory().contains(item)) {
            return chr != null
                    && floatingPointCorrection(this.containingItem.getEquipParent().getInventory().getCapacityWeight()) + item.getUnequippedWeight()
                        <= this.containingItem.getEquipParent().getInventory().getEffectiveCapacity(chr)
                ? this.hasRoomFor(chr, item.getUnequippedWeight())
                : false;
        } else {
            return this.hasRoomFor(chr, item.getUnequippedWeight());
        }
    }

    public boolean hasRoomFor(IsoGameCharacter chr, float weightVal) {
        if (this.isVehicleSeat()
            && this.vehiclePart.getVehicle().getCharacter(this.vehiclePart.getContainerSeatNumber()) == null
            && this.items.isEmpty()
            && floatingPointCorrection(weightVal) <= 50.0F) {
            return true;
        } else if (this.inventoryContainer != null && this.inventoryContainer.getMaxItemSize() > 0.0F && weightVal > this.inventoryContainer.getMaxItemSize()) {
            return false;
        } else {
            if (chr != null
                && !this.isInCharacterInventory(chr)
                && this.containingItem != null
                && this.containingItem.getWorldItem() != null
                && this.containingItem.getWorldItem().getSquare() != null) {
                IsoGridSquare sq = this.containingItem.getWorldItem().getSquare();
                float ground = sq.getTotalWeightOfItemsOnFloor();
                if (ground + weightVal > 50.0F) {
                    return false;
                }
            }

            VehiclePart vehiclePart1 = this.containingItem != null && this.containingItem.getContainer() != null
                ? this.containingItem.getContainer().getVehiclePart()
                : null;
            return vehiclePart1 != null
                    && floatingPointCorrection(this.containingItem.getContainer().getCapacityWeight() + weightVal)
                        > this.getContainingItem().getContainer().getEffectiveCapacity(chr)
                ? false
                : floatingPointCorrection(this.getCapacityWeight()) + weightVal <= this.getEffectiveCapacity(chr);
        }
    }

    public boolean isItemAllowed(InventoryItem item) {
        if (item == null) {
            return false;
        } else if (item instanceof AnimalInventoryItem && !"floor".equals(this.type)) {
            return false;
        } else if (item.getType().contains("Corpse") && this.parent instanceof IsoDeadBody) {
            return false;
        } else {
            String category = this.getOnlyAcceptCategory();
            if (category != null && !category.equalsIgnoreCase(item.getCategory())) {
                return false;
            } else {
                String functionName = this.getAcceptItemFunction();
                if (functionName != null) {
                    Object functionObj = LuaManager.getFunctionObject(functionName);
                    if (functionObj != null) {
                        Boolean accept = LuaManager.caller.protectedCallBoolean(LuaManager.thread, functionObj, this, item);
                        if (accept != Boolean.TRUE) {
                            return false;
                        }
                    }
                }

                if (this.parent != null && !this.parent.isItemAllowedInContainer(this, item)) {
                    return false;
                } else if (this.getType().equals("clothingrack") && !(item instanceof Clothing)) {
                    return false;
                } else if (this.getParent() != null
                    && this.getParent().getProperties() != null
                    && this.getParent().getProperties().get("CustomName") != null
                    && this.getParent().getProperties().get("CustomName").equals("Toaster")
                    && !item.hasTag(ItemTag.FITS_TOASTER)) {
                    return false;
                } else {
                    if (this.getParent() != null && this.getParent().getProperties() != null && this.getParent().getProperties().get("GroupName") != null) {
                        boolean coffeeMaker = this.getParent().getProperties().get("GroupName").equals("Coffee")
                            || this.getParent().getProperties().get("GroupName").equals("Espresso");
                        if (coffeeMaker && !item.hasTag(ItemTag.COFFEE_MAKER)) {
                            return false;
                        }
                    }

                    return true;
                }
            }
        }
    }

    public boolean isRemoveItemAllowed(InventoryItem item) {
        return item == null ? false : this.parent == null || this.parent.isRemoveItemAllowedFromContainer(this, item);
    }

    public boolean isExplored() {
        return this.explored;
    }

    public void setExplored(boolean b) {
        this.explored = b;
    }

    public boolean isInCharacterInventory(IsoGameCharacter chr) {
        if (chr.getInventory() == this) {
            return true;
        } else {
            if (this.containingItem != null) {
                if (chr.getInventory().contains(this.containingItem, true)) {
                    return true;
                }

                if (this.containingItem.getContainer() != null) {
                    return this.containingItem.getContainer().isInCharacterInventory(chr);
                }
            }

            return false;
        }
    }

    public boolean isInside(InventoryItem item) {
        if (this.containingItem == null) {
            return false;
        } else {
            return this.containingItem == item ? true : this.containingItem.getContainer() != null && this.containingItem.getContainer().isInside(item);
        }
    }

    public InventoryItem getContainingItem() {
        return this.containingItem;
    }

    public InventoryItem DoAddItem(InventoryItem item) {
        return this.AddItem(item);
    }

    public InventoryItem DoAddItemBlind(InventoryItem item) {
        return this.AddItem(item);
    }

    public ArrayList<InventoryItem> AddItems(String type, int count) {
        ArrayList<InventoryItem> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            InventoryItem _item = this.AddItem(type);
            if (_item != null) {
                result.add(_item);
            }
        }

        return result;
    }

    public <T extends InventoryItem> T addItem(ItemKey item) {
        return (T)this.AddItem(item.toString());
    }

    public List<InventoryItem> addItems(ItemKey item, int count) {
        return this.AddItems(item.toString(), count);
    }

    public ArrayList<InventoryItem> AddItems(InventoryItem item, int count) {
        return this.AddItems(item.getFullType(), count);
    }

    public ArrayList<InventoryItem> AddItems(ArrayList<InventoryItem> items) {
        for (InventoryItem item : items) {
            this.AddItem(item);
        }

        return items;
    }

    public int getNumberOfItem(String findItem, boolean includeReplaceOnDeplete) {
        return this.getNumberOfItem(findItem, includeReplaceOnDeplete, false);
    }

    public int getNumberOfItem(String findItem) {
        return this.getNumberOfItem(findItem, false);
    }

    public int getNumberOfItem(String findItem, boolean includeReplaceOnDeplete, ArrayList<ItemContainer> containers) {
        int result = this.getNumberOfItem(findItem, includeReplaceOnDeplete);
        if (containers != null) {
            for (ItemContainer container : containers) {
                if (container != this) {
                    result += container.getNumberOfItem(findItem, includeReplaceOnDeplete);
                }
            }
        }

        return result;
    }

    public int getNumberOfItem(String findItem, boolean includeReplaceOnDeplete, boolean insideInv) {
        int result = 0;

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.getFullType().equals(findItem) || item.getType().equals(findItem)) {
                result++;
            } else if (insideInv && item instanceof InventoryContainer container) {
                result += container.getItemContainer().getNumberOfItem(findItem);
            } else if (includeReplaceOnDeplete
                && item instanceof DrainableComboItem drainable
                && drainable.getReplaceOnDeplete() != null
                && (drainable.getReplaceOnDepleteFullType().equals(findItem) || drainable.getReplaceOnDeplete().equals(findItem))) {
                result++;
            }
        }

        return result;
    }

    public InventoryItem addItem(InventoryItem item) {
        return this.AddItem(item);
    }

    public InventoryItem AddItem(InventoryItem item) {
        if (item == null) {
            return null;
        } else if (this.containsID(item.id)) {
            System.out.println("Error, container already has id");
            return this.getItemWithID(item.id);
        } else {
            this.drawDirty = true;
            if (this.parent != null) {
                this.dirty = true;
            }

            if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
                this.parent.DirtySlice();
            }

            if (item.container != null) {
                item.container.Remove(item);
            }

            item.container = this;
            this.items.add(item);
            if (IsoWorld.instance.currentCell != null) {
                IsoWorld.instance.currentCell.addToProcessItems(item);
            }

            if (this.getParent() instanceof IsoFeedingTrough) {
                ((IsoFeedingTrough)this.getParent()).onFoodAdded();
            }

            item.OnAddedToContainer(this);
            if (this.getParent() != null) {
                this.getParent().flagForHotSave();
            }

            return item;
        }
    }

    public void SpawnItem(InventoryItem item) {
        if (item == null) {
            this.AddItem(item);
        }

        item.SynchSpawn();
    }

    public InventoryItem AddItemBlind(InventoryItem item) {
        if (item == null) {
            return null;
        } else if (item.getWeight() + this.getCapacityWeight() > this.getCapacity()) {
            return null;
        } else {
            if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
                this.parent.DirtySlice();
            }

            this.items.add(item);
            if (this.getParent() != null) {
                this.getParent().flagForHotSave();
            }

            return item;
        }
    }

    public InventoryItem SpawnItem(String type) {
        InventoryItem item = this.AddItem(type);
        if (item == null) {
            return null;
        } else {
            item.SynchSpawn();
            return item;
        }
    }

    public InventoryItem AddItem(String type) {
        this.drawDirty = true;
        if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
            this.dirty = true;
        }

        Item scriptItem = ScriptManager.instance.FindItem(type);
        if (scriptItem == null) {
            DebugLog.log("ERROR: ItemContainer.AddItem: can't find " + type);
            return null;
        } else if (scriptItem.obsolete) {
            return null;
        } else {
            InventoryItem item = InventoryItemFactory.CreateItem(type);
            if (item == null) {
                return null;
            } else {
                item.container = this;
                this.items.add(item);
                if (item instanceof Food food) {
                    food.setHeat(this.getTemprature());
                }

                if (item.hasComponent(ComponentType.FluidContainer) && item.isCookable()) {
                    item.setItemHeat(this.getTemprature());
                }

                if (IsoWorld.instance.currentCell != null) {
                    IsoWorld.instance.currentCell.addToProcessItems(item);
                }

                if (this.getParent() != null) {
                    this.getParent().flagForHotSave();
                }

                return item;
            }
        }
    }

    public boolean SpawnItem(String type, float useDelta) {
        return this.AddItem(type, useDelta, true);
    }

    public boolean AddItem(String type, float useDelta) {
        this.drawDirty = true;
        if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
            this.dirty = true;
        }

        InventoryItem item = InventoryItemFactory.CreateItem(type);
        if (item == null) {
            return false;
        } else {
            if (item instanceof Drainable) {
                item.setCurrentUses((int)(item.getMaxUses() * useDelta));
            }

            item.container = this;
            this.items.add(item);
            if (this.getParent() != null) {
                this.getParent().flagForHotSave();
            }

            return true;
        }
    }

    public boolean AddItem(String type, float useDelta, boolean synchSpawn) {
        this.drawDirty = true;
        if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
            this.dirty = true;
        }

        InventoryItem item = InventoryItemFactory.CreateItem(type);
        if (item == null) {
            return false;
        } else {
            if (item instanceof Drainable) {
                item.setCurrentUses((int)(item.getMaxUses() * useDelta));
            }

            item.container = this;
            this.items.add(item);
            if (this.getParent() != null) {
                this.getParent().flagForHotSave();
            }

            if (synchSpawn || item != null) {
                item.SynchSpawn();
            }

            return true;
        }
    }

    public boolean contains(InventoryItem item) {
        return this.items.contains(item);
    }

    public boolean containsWithModule(String moduleType) {
        return this.containsWithModule(moduleType, false);
    }

    public boolean containsWithModule(String moduleType, boolean withDeltaLeft) {
        String type = moduleType;
        String module = "Base";
        if (moduleType.contains(".")) {
            module = moduleType.split("\\.")[0];
            type = moduleType.split("\\.")[1];
        }

        for (int n = 0; n < this.items.size(); n++) {
            InventoryItem item = this.items.get(n);
            if (item == null) {
                this.items.remove(n);
                n--;
            } else if (item.type.equals(type.trim())
                && module.equals(item.getModule())
                && (!withDeltaLeft || !(item instanceof DrainableComboItem) || item.getCurrentUses() > 0)) {
                return true;
            }
        }

        return false;
    }

    @Deprecated
    public void removeItemOnServer(InventoryItem item) {
        if (GameClient.client) {
            if (this.containingItem != null && this.containingItem.getWorldItem() != null) {
                GameClient.instance.addToItemRemoveSendBuffer(this.containingItem.getWorldItem(), this, item);
            } else {
                GameClient.instance.addToItemRemoveSendBuffer(this.parent, this, item);
            }
        }
    }

    public void addItemOnServer(InventoryItem item) {
        if (GameClient.client) {
            if (this.containingItem != null && this.containingItem.getWorldItem() != null) {
                GameClient.instance.addToItemSendBuffer(this.containingItem.getWorldItem(), this, item);
            } else {
                GameClient.instance.addToItemSendBuffer(this.parent, this, item);
            }
        }
    }

    public boolean contains(InventoryItem in_itemToFind, boolean doInv) {
        return this.contains(in_itemToFind, PZArrayList::referenceEqual, doInv);
    }

    public boolean contains(Invokers.Params2.Boolean.IParam2<InventoryItem> in_predicate, boolean doInv) {
        return this.contains(null, in_predicate, doInv);
    }

    public <T> boolean contains(T in_itemToCompare, Invokers.Params2.Boolean.ICallback<T, InventoryItem> in_predicate, boolean doInv) {
        InventoryItem foundItem = this.findItem(in_itemToCompare, in_predicate, doInv);
        return foundItem != null;
    }

    public InventoryItem findItem(Invokers.Params2.Boolean.IParam2<InventoryItem> in_predicate, boolean doInv) {
        return this.findItem(null, in_predicate, doInv);
    }

    public <T> InventoryItem findItem(T in_itemToCompare, Invokers.Params2.Boolean.ICallback<T, InventoryItem> in_predicate, boolean doInv) {
        ItemContainer.InventoryItemList checkedBags = TL_itemListPool.get().alloc();

        try {
            ArrayList<InventoryItem> items = this.items;

            for (int n = 0; n < items.size(); n++) {
                InventoryItem item = items.get(n);
                if (item == null) {
                    items.remove(n--);
                } else {
                    if (in_predicate.accept(in_itemToCompare, item)) {
                        return item;
                    }

                    if (doInv && item instanceof InventoryContainer itemAsContainer && itemAsContainer.getInventory() != null && !checkedBags.contains(item)) {
                        checkedBags.add(itemAsContainer);
                    }
                }
            }

            for (int i = 0; i < checkedBags.size(); i++) {
                ItemContainer container = ((InventoryContainer)checkedBags.get(i)).getInventory();
                InventoryItem foundItem = container.findItem(in_itemToCompare, in_predicate, doInv);
                if (foundItem != null) {
                    return foundItem;
                }
            }

            return null;
        } finally {
            TL_itemListPool.get().release(checkedBags);
        }
    }

    public InventoryItem findItem(String type, boolean doInv, boolean ignoreBroken) {
        if (type.contains("/")) {
            String[] variants = type.split("/");

            for (String variant : variants) {
                InventoryItem foundItem = this.findItem(variant.trim(), doInv, ignoreBroken);
                if (foundItem != null) {
                    return foundItem;
                }
            }

            return null;
        } else if (type.contains("Type:")) {
            if (type.contains("Food")) {
                return this.findItem(item -> item instanceof Food, doInv);
            } else if (type.contains("Weapon")) {
                return ignoreBroken
                    ? this.findItem(item -> item instanceof HandWeapon, doInv)
                    : this.findItem(item -> item instanceof HandWeapon && !item.isBroken(), doInv);
            } else if (type.contains("AlarmClock")) {
                return this.findItem(item -> item instanceof AlarmClock, doInv);
            } else {
                return type.contains("AlarmClockClothing") ? this.findItem(item -> item instanceof AlarmClockClothing, doInv) : null;
            }
        } else {
            return ignoreBroken
                ? this.findItem(type.trim(), ItemContainer::compareType, doInv)
                : this.findItem(type.trim(), (_type, item) -> compareType(_type, item) && !item.isBroken(), doInv);
        }
    }

    public InventoryItem findHumanCorpseItem() {
        return this.findItem(InventoryItem::isHumanCorpse, false);
    }

    public boolean containsHumanCorpse() {
        return this.contains(InventoryItem::isHumanCorpse, false);
    }

    public boolean contains(String type, boolean doInv) {
        return this.contains(type, doInv, false);
    }

    public boolean containsType(String type) {
        return this.contains(type, false, false);
    }

    public boolean containsTypeRecurse(String type) {
        return this.contains(type, true, false);
    }

    private boolean testBroken(boolean test, InventoryItem item) {
        return !test ? true : !item.isBroken();
    }

    public boolean contains(String type, boolean doInv, boolean ignoreBroken) {
        InventoryItem foundItem = this.findItem(type, doInv, ignoreBroken);
        return foundItem != null;
    }

    public boolean contains(String type) {
        return this.contains(type, false);
    }

    public AnimalInventoryItem getAnimalInventoryItem(IsoAnimal animal) {
        for (InventoryItem item : this.items) {
            if (item instanceof AnimalInventoryItem animalInventoryItem) {
                if (!GameServer.server && !GameClient.client) {
                    if (animalInventoryItem.getAnimal().getAnimalID() == animal.getAnimalID()) {
                        return animalInventoryItem;
                    }
                } else if (animalInventoryItem.getAnimal().getOnlineID() == animal.getOnlineID()) {
                    return animalInventoryItem;
                }
            }
        }

        return null;
    }

    public boolean canHumanCorpseFit() {
        if (this.isVehicleSeat()) {
            if (this.containsHumanCorpse()) {
                return false;
            } else {
                float occupiedSpace = this.getCapacityWeight();
                return !(occupiedSpace > 5.0F);
            }
        } else if (this.isVehiclePart()) {
            float availableSpace = this.getAvailableWeightCapacity();
            float requiredSpace = IsoGameCharacter.getWeightAsCorpse();
            return !(availableSpace < requiredSpace);
        } else {
            float availableSpace = this.getAvailableWeightCapacity();
            float requiredSpace = IsoGameCharacter.getWeightAsCorpse();
            if (availableSpace < requiredSpace) {
                return false;
            } else {
                String[] allowedContainers = new String[]{
                    "bin",
                    "cardboardbox",
                    "crate",
                    "militarycrate",
                    "clothingdryer",
                    "clothingdryerbasic",
                    "clothingwasher",
                    "coffin",
                    "doghouse",
                    "dumpster",
                    "fireplace",
                    "fridge",
                    "freezer",
                    "locker",
                    "militarylocker",
                    "postbox",
                    "shelter",
                    "tent",
                    "wardrobe"
                };
                String containerType = this.getType();
                return PZArrayUtil.contains(allowedContainers, containerType, StringUtils::equalsIgnoreCase);
            }
        }
    }

    public boolean canItemFit(InventoryItem in_item) {
        if (in_item.isHumanCorpse()) {
            return this.canHumanCorpseFit();
        } else {
            float availableSpace = this.getAvailableWeightCapacity();
            float requiredSpace = in_item.getUnequippedWeight();
            return availableSpace >= requiredSpace;
        }
    }

    public boolean isVehiclePart() {
        return this.getVehiclePart() != null;
    }

    public boolean isVehicleSeat() {
        VehiclePart vehiclePart = this.getVehiclePart();
        return vehiclePart == null ? false : vehiclePart.isSeat();
    }

    public boolean isOccupiedVehicleSeat() {
        return this.isVehicleSeat() && this.vehiclePart.getVehicle().getCharacter(this.vehiclePart.getContainerSeatNumber()) != null;
    }

    private static InventoryItem getBestOf(ItemContainer.InventoryItemList items, Comparator<InventoryItem> comparator) {
        if (items != null && !items.isEmpty()) {
            InventoryItem best = items.get(0);

            for (int i = 1; i < items.size(); i++) {
                InventoryItem item = items.get(i);
                if (comparator.compare(item, best) > 0) {
                    best = item;
                }
            }

            return best;
        } else {
            return null;
        }
    }

    public InventoryItem getBest(Predicate<InventoryItem> predicate, Comparator<InventoryItem> comparator) {
        ItemContainer.InventoryItemList items = TL_itemListPool.get().alloc();
        this.getAll(predicate, items);
        InventoryItem best = getBestOf(items, comparator);
        TL_itemListPool.get().release(items);
        return best;
    }

    public InventoryItem getBestRecurse(Predicate<InventoryItem> predicate, Comparator<InventoryItem> comparator) {
        ItemContainer.InventoryItemList items = TL_itemListPool.get().alloc();
        this.getAllRecurse(predicate, items);
        InventoryItem best = getBestOf(items, comparator);
        TL_itemListPool.get().release(items);
        return best;
    }

    public InventoryItem getBestType(String type, Comparator<InventoryItem> comparator) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);

        InventoryItem var4;
        try {
            var4 = this.getBest(predicate, comparator);
        } finally {
            TL_predicates.get().type.release(predicate);
        }

        return var4;
    }

    public InventoryItem getBestTypeRecurse(String type, Comparator<InventoryItem> comparator) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);

        InventoryItem var4;
        try {
            var4 = this.getBestRecurse(predicate, comparator);
        } finally {
            TL_predicates.get().type.release(predicate);
        }

        return var4;
    }

    public InventoryItem getBestEval(LuaClosure predicateObj, LuaClosure comparatorObj) {
        ItemContainer.EvalPredicate predicate = TL_predicates.get().eval.alloc().init(predicateObj);
        ItemContainer.EvalComparator comparator = TL_comparators.get().eval.alloc().init(comparatorObj);

        InventoryItem var5;
        try {
            var5 = this.getBest(predicate, comparator);
        } finally {
            TL_predicates.get().eval.release(predicate);
            TL_comparators.get().eval.release(comparator);
        }

        return var5;
    }

    public InventoryItem getBestEvalRecurse(LuaClosure predicateObj, LuaClosure comparatorObj) {
        ItemContainer.EvalPredicate predicate = TL_predicates.get().eval.alloc().init(predicateObj);
        ItemContainer.EvalComparator comparator = TL_comparators.get().eval.alloc().init(comparatorObj);

        InventoryItem var5;
        try {
            var5 = this.getBestRecurse(predicate, comparator);
        } finally {
            TL_predicates.get().eval.release(predicate);
            TL_comparators.get().eval.release(comparator);
        }

        return var5;
    }

    public InventoryItem getBestEvalArg(LuaClosure predicateObj, LuaClosure comparatorObj, Object arg) {
        ItemContainer.EvalArgPredicate predicate = TL_predicates.get().evalArg.alloc().init(predicateObj, arg);
        ItemContainer.EvalArgComparator comparator = TL_comparators.get().evalArg.alloc().init(comparatorObj, arg);

        InventoryItem var6;
        try {
            var6 = this.getBest(predicate, comparator);
        } finally {
            TL_predicates.get().evalArg.release(predicate);
            TL_comparators.get().evalArg.release(comparator);
        }

        return var6;
    }

    public InventoryItem getBestEvalArgRecurse(LuaClosure predicateObj, LuaClosure comparatorObj, Object arg) {
        ItemContainer.EvalArgPredicate predicate = TL_predicates.get().evalArg.alloc().init(predicateObj, arg);
        ItemContainer.EvalArgComparator comparator = TL_comparators.get().evalArg.alloc().init(comparatorObj, arg);

        InventoryItem var6;
        try {
            var6 = this.getBestRecurse(predicate, comparator);
        } finally {
            TL_predicates.get().evalArg.release(predicate);
            TL_comparators.get().evalArg.release(comparator);
        }

        return var6;
    }

    public InventoryItem getBestTypeEval(String type, LuaClosure comparatorObj) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        ItemContainer.EvalComparator comparator = TL_comparators.get().eval.alloc().init(comparatorObj);

        InventoryItem var5;
        try {
            var5 = this.getBest(predicate, comparator);
        } finally {
            TL_predicates.get().type.release(predicate);
            TL_comparators.get().eval.release(comparator);
        }

        return var5;
    }

    public InventoryItem getBestTypeEvalRecurse(String type, LuaClosure comparatorObj) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        ItemContainer.EvalComparator comparator = TL_comparators.get().eval.alloc().init(comparatorObj);

        InventoryItem var5;
        try {
            var5 = this.getBestRecurse(predicate, comparator);
        } finally {
            TL_predicates.get().type.release(predicate);
            TL_comparators.get().eval.release(comparator);
        }

        return var5;
    }

    public InventoryItem getBestTypeEvalArg(String type, LuaClosure comparatorObj, Object arg) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        ItemContainer.EvalArgComparator comparator = TL_comparators.get().evalArg.alloc().init(comparatorObj, arg);

        InventoryItem var6;
        try {
            var6 = this.getBest(predicate, comparator);
        } finally {
            TL_predicates.get().type.release(predicate);
            TL_comparators.get().evalArg.release(comparator);
        }

        return var6;
    }

    public InventoryItem getBestTypeEvalArgRecurse(String type, LuaClosure comparatorObj, Object arg) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        ItemContainer.EvalArgComparator comparator = TL_comparators.get().evalArg.alloc().init(comparatorObj, arg);

        InventoryItem var6;
        try {
            var6 = this.getBestRecurse(predicate, comparator);
        } finally {
            TL_predicates.get().type.release(predicate);
            TL_comparators.get().evalArg.release(comparator);
        }

        return var6;
    }

    public InventoryItem getBestCondition(Predicate<InventoryItem> predicate) {
        ItemContainer.ConditionComparator comparator = TL_comparators.get().condition.alloc();
        InventoryItem best = this.getBest(predicate, comparator);
        TL_comparators.get().condition.release(comparator);
        if (best != null && best.getCondition() <= 0) {
            best = null;
        }

        return best;
    }

    public InventoryItem getBestConditionRecurse(Predicate<InventoryItem> predicate) {
        ItemContainer.ConditionComparator comparator = TL_comparators.get().condition.alloc();
        InventoryItem best = this.getBestRecurse(predicate, comparator);
        TL_comparators.get().condition.release(comparator);
        if (best != null && best.getCondition() <= 0) {
            best = null;
        }

        return best;
    }

    public InventoryItem getBestCondition(String type) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        InventoryItem best = this.getBestCondition(predicate);
        TL_predicates.get().type.release(predicate);
        return best;
    }

    public InventoryItem getBestConditionRecurse(String type) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        InventoryItem best = this.getBestConditionRecurse(predicate);
        TL_predicates.get().type.release(predicate);
        return best;
    }

    public InventoryItem getBestConditionEval(LuaClosure functionObj) {
        ItemContainer.EvalPredicate predicate = TL_predicates.get().eval.alloc().init(functionObj);
        InventoryItem best = this.getBestCondition(predicate);
        TL_predicates.get().eval.release(predicate);
        return best;
    }

    public InventoryItem getBestConditionEvalRecurse(LuaClosure functionObj) {
        ItemContainer.EvalPredicate predicate = TL_predicates.get().eval.alloc().init(functionObj);
        InventoryItem best = this.getBestConditionRecurse(predicate);
        TL_predicates.get().eval.release(predicate);
        return best;
    }

    public InventoryItem getBestConditionEvalArg(LuaClosure functionObj, Object arg) {
        ItemContainer.EvalArgPredicate predicate = TL_predicates.get().evalArg.alloc().init(functionObj, arg);
        InventoryItem best = this.getBestCondition(predicate);
        TL_predicates.get().evalArg.release(predicate);
        return best;
    }

    public InventoryItem getBestConditionEvalArgRecurse(LuaClosure functionObj, Object arg) {
        ItemContainer.EvalArgPredicate predicate = TL_predicates.get().evalArg.alloc().init(functionObj, arg);
        InventoryItem best = this.getBestConditionRecurse(predicate);
        TL_predicates.get().evalArg.release(predicate);
        return best;
    }

    public InventoryItem getFirstEval(LuaClosure functionObj) {
        ItemContainer.EvalPredicate predicate = TL_predicates.get().eval.alloc().init(functionObj);
        InventoryItem item = this.getFirst(predicate);
        TL_predicates.get().eval.release(predicate);
        return item;
    }

    public InventoryItem getFirstEvalArg(LuaClosure functionObj, Object arg) {
        ItemContainer.EvalArgPredicate predicate = TL_predicates.get().evalArg.alloc().init(functionObj, arg);
        InventoryItem item = this.getFirst(predicate);
        TL_predicates.get().evalArg.release(predicate);
        return item;
    }

    public boolean containsEval(LuaClosure functionObj) {
        return this.getFirstEval(functionObj) != null;
    }

    public boolean containsEvalArg(LuaClosure functionObj, Object arg) {
        return this.getFirstEvalArg(functionObj, arg) != null;
    }

    public boolean containsEvalRecurse(LuaClosure functionObj) {
        return this.getFirstEvalRecurse(functionObj) != null;
    }

    public boolean containsEvalArgRecurse(LuaClosure functionObj, Object arg) {
        return this.getFirstEvalArgRecurse(functionObj, arg) != null;
    }

    public boolean containsTag(ItemTag itemTag) {
        return this.getFirstTag(itemTag) != null;
    }

    public boolean containsTagEval(ItemTag itemTag, LuaClosure functionObj) {
        return this.getFirstTagEval(itemTag, functionObj) != null;
    }

    public boolean containsTagRecurse(ItemTag itemTag) {
        return this.getFirstTagRecurse(itemTag) != null;
    }

    public boolean containsTagEvalRecurse(ItemTag itemTag, LuaClosure functionObj) {
        return this.getFirstTagEvalRecurse(itemTag, functionObj) != null;
    }

    public boolean containsTagEvalArgRecurse(ItemTag itemTag, LuaClosure functionObj, Object arg) {
        return this.getFirstTagEvalArgRecurse(itemTag, functionObj, arg) != null;
    }

    public boolean containsTypeEvalRecurse(String type, LuaClosure functionObj) {
        return this.getFirstTypeEvalRecurse(type, functionObj) != null;
    }

    public boolean containsTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg) {
        return this.getFirstTypeEvalArgRecurse(type, functionObj, arg) != null;
    }

    private static boolean compareType(String type1, String type2) {
        if (type1 != null && type1.contains("/")) {
            int p = type1.indexOf(type2);
            if (p == -1) {
                return false;
            } else {
                char chBefore = p > 0 ? type1.charAt(p - 1) : 0;
                char chAfter = p + type2.length() < type1.length() ? type1.charAt(p + type2.length()) : 0;
                return chBefore == 0 && chAfter == '/' || chBefore == '/' && chAfter == 0 || chBefore == '/' && chAfter == '/';
            }
        } else {
            return type1.equals(type2);
        }
    }

    private static boolean compareType(String type, InventoryItem item) {
        return type != null && type.indexOf(46) == -1
            ? compareType(type, item.getType())
            : compareType(type, item.getFullType()) || compareType(type, item.getType());
    }

    public InventoryItem getFirst(Predicate<InventoryItem> predicate) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item == null) {
                this.items.remove(i);
                i--;
            } else if (predicate.test(item)) {
                return item;
            }
        }

        return null;
    }

    public InventoryItem getFirstRecurse(Predicate<InventoryItem> predicate) {
        ItemContainer.InventoryItemList bags = TL_itemListPool.get().alloc();

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item == null) {
                this.items.remove(i);
                i--;
            } else {
                if (predicate.test(item)) {
                    TL_itemListPool.get().release(bags);
                    return item;
                }

                if (item instanceof InventoryContainer) {
                    bags.add(item);
                }
            }
        }

        for (int ix = 0; ix < bags.size(); ix++) {
            ItemContainer container = ((InventoryContainer)bags.get(ix)).getInventory();
            InventoryItem item = container.getFirstRecurse(predicate);
            if (item != null) {
                TL_itemListPool.get().release(bags);
                return item;
            }
        }

        TL_itemListPool.get().release(bags);
        return null;
    }

    public ArrayList<InventoryItem> getSome(Predicate<InventoryItem> predicate, int count, ArrayList<InventoryItem> result) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item == null) {
                this.items.remove(i);
                i--;
            } else if (predicate.test(item)) {
                result.add(item);
                if (result.size() >= count) {
                    break;
                }
            }
        }

        return result;
    }

    public ArrayList<InventoryItem> getSomeRecurse(Predicate<InventoryItem> predicate, int count, ArrayList<InventoryItem> result) {
        ItemContainer.InventoryItemList bags = TL_itemListPool.get().alloc();

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item == null) {
                this.items.remove(i);
                i--;
            } else {
                if (predicate.test(item)) {
                    result.add(item);
                    if (result.size() >= count) {
                        TL_itemListPool.get().release(bags);
                        return result;
                    }
                }

                if (item instanceof InventoryContainer) {
                    bags.add(item);
                }
            }
        }

        for (int ix = 0; ix < bags.size(); ix++) {
            ItemContainer container = ((InventoryContainer)bags.get(ix)).getInventory();
            container.getSomeRecurse(predicate, count, result);
            if (result.size() >= count) {
                break;
            }
        }

        TL_itemListPool.get().release(bags);
        return result;
    }

    public ArrayList<InventoryItem> getAll(Predicate<InventoryItem> predicate, ArrayList<InventoryItem> result) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item == null) {
                this.items.remove(i);
                i--;
            } else if (predicate.test(item)) {
                result.add(item);
            }
        }

        return result;
    }

    public ArrayList<InventoryItem> getAllRecurse(Predicate<InventoryItem> predicate, ArrayList<InventoryItem> result) {
        ItemContainer.InventoryItemList bags = TL_itemListPool.get().alloc();

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item == null) {
                this.items.remove(i);
                i--;
            } else {
                if (predicate.test(item)) {
                    result.add(item);
                }

                if (item instanceof InventoryContainer) {
                    bags.add(item);
                }
            }
        }

        for (int ix = 0; ix < bags.size(); ix++) {
            ItemContainer container = ((InventoryContainer)bags.get(ix)).getInventory();
            container.getAllRecurse(predicate, result);
        }

        TL_itemListPool.get().release(bags);
        return result;
    }

    public int getCount(Predicate<InventoryItem> predicate) {
        ItemContainer.InventoryItemList items = TL_itemListPool.get().alloc();
        this.getAll(predicate, items);
        int count = items.size();
        TL_itemListPool.get().release(items);
        return count;
    }

    public int getCountRecurse(Predicate<InventoryItem> predicate) {
        ItemContainer.InventoryItemList items = TL_itemListPool.get().alloc();
        this.getAllRecurse(predicate, items);
        int count = items.size();
        TL_itemListPool.get().release(items);
        return count;
    }

    public int getCountTag(ItemTag itemTag) {
        ItemContainer.TagPredicate predicate = TL_predicates.get().tag.alloc().init(itemTag);
        int count = this.getCount(predicate);
        TL_predicates.get().tag.release(predicate);
        return count;
    }

    public int getCountTagEval(ItemTag itemTag, LuaClosure functionObj) {
        ItemContainer.TagEvalPredicate predicate = TL_predicates.get().tagEval.alloc().init(itemTag, functionObj);
        int count = this.getCount(predicate);
        TL_predicates.get().tagEval.release(predicate);
        return count;
    }

    public int getCountTagEvalArg(ItemTag itemTag, LuaClosure functionObj, Object arg) {
        ItemContainer.TagEvalArgPredicate predicate = TL_predicates.get().tagEvalArg.alloc().init(itemTag, functionObj, arg);
        int count = this.getCount(predicate);
        TL_predicates.get().tagEvalArg.release(predicate);
        return count;
    }

    public int getCountTagRecurse(ItemTag itemTag) {
        ItemContainer.TagPredicate predicate = TL_predicates.get().tag.alloc().init(itemTag);
        int count = this.getCountRecurse(predicate);
        TL_predicates.get().tag.release(predicate);
        return count;
    }

    public int getCountTagEvalRecurse(ItemTag itemTag, LuaClosure functionObj) {
        ItemContainer.TagEvalPredicate predicate = TL_predicates.get().tagEval.alloc().init(itemTag, functionObj);
        int count = this.getCountRecurse(predicate);
        TL_predicates.get().tagEval.release(predicate);
        return count;
    }

    public int getCountTagEvalArgRecurse(ItemTag itemTag, LuaClosure functionObj, Object arg) {
        ItemContainer.TagEvalArgPredicate predicate = TL_predicates.get().tagEvalArg.alloc().init(itemTag, functionObj, arg);
        int count = this.getCountRecurse(predicate);
        TL_predicates.get().tagEvalArg.release(predicate);
        return count;
    }

    public int getCountType(String type) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        int count = this.getCount(predicate);
        TL_predicates.get().type.release(predicate);
        return count;
    }

    public int getCountTypeEval(String type, LuaClosure functionObj) {
        ItemContainer.TypeEvalPredicate predicate = TL_predicates.get().typeEval.alloc().init(type, functionObj);
        int count = this.getCount(predicate);
        TL_predicates.get().typeEval.release(predicate);
        return count;
    }

    public int getCountTypeEvalArg(String type, LuaClosure functionObj, Object arg) {
        ItemContainer.TypeEvalArgPredicate predicate = TL_predicates.get().typeEvalArg.alloc().init(type, functionObj, arg);
        int count = this.getCount(predicate);
        TL_predicates.get().typeEvalArg.release(predicate);
        return count;
    }

    public int getCountTypeRecurse(String type) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        int count = this.getCountRecurse(predicate);
        TL_predicates.get().type.release(predicate);
        return count;
    }

    public int getCountTypeEvalRecurse(String type, LuaClosure functionObj) {
        ItemContainer.TypeEvalPredicate predicate = TL_predicates.get().typeEval.alloc().init(type, functionObj);
        int count = this.getCountRecurse(predicate);
        TL_predicates.get().typeEval.release(predicate);
        return count;
    }

    public int getCountTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg) {
        ItemContainer.TypeEvalArgPredicate predicate = TL_predicates.get().typeEvalArg.alloc().init(type, functionObj, arg);
        int count = this.getCountRecurse(predicate);
        TL_predicates.get().typeEvalArg.release(predicate);
        return count;
    }

    public int getCountEval(LuaClosure functionObj) {
        ItemContainer.EvalPredicate predicate = TL_predicates.get().eval.alloc().init(functionObj);
        int count = this.getCount(predicate);
        TL_predicates.get().eval.release(predicate);
        return count;
    }

    public int getCountEvalArg(LuaClosure functionObj, Object arg) {
        ItemContainer.EvalArgPredicate predicate = TL_predicates.get().evalArg.alloc().init(functionObj, arg);
        int count = this.getCount(predicate);
        TL_predicates.get().evalArg.release(predicate);
        return count;
    }

    public int getCountEvalRecurse(LuaClosure functionObj) {
        ItemContainer.EvalPredicate predicate = TL_predicates.get().eval.alloc().init(functionObj);
        int count = this.getCountRecurse(predicate);
        TL_predicates.get().eval.release(predicate);
        return count;
    }

    public int getCountEvalArgRecurse(LuaClosure functionObj, Object arg) {
        ItemContainer.EvalArgPredicate predicate = TL_predicates.get().evalArg.alloc().init(functionObj, arg);
        int count = this.getCountRecurse(predicate);
        TL_predicates.get().evalArg.release(predicate);
        return count;
    }

    public InventoryItem getFirstCategory(String category) {
        ItemContainer.CategoryPredicate predicate = TL_predicates.get().category.alloc().init(category);
        InventoryItem item = this.getFirst(predicate);
        TL_predicates.get().category.release(predicate);
        return item;
    }

    public InventoryItem getFirstCategoryRecurse(String category) {
        ItemContainer.CategoryPredicate predicate = TL_predicates.get().category.alloc().init(category);
        InventoryItem item = this.getFirstRecurse(predicate);
        TL_predicates.get().category.release(predicate);
        return item;
    }

    public InventoryItem getFirstEvalRecurse(LuaClosure functionObj) {
        ItemContainer.EvalPredicate predicate = TL_predicates.get().eval.alloc().init(functionObj);
        InventoryItem item = this.getFirstRecurse(predicate);
        TL_predicates.get().eval.release(predicate);
        return item;
    }

    public InventoryItem getFirstEvalArgRecurse(LuaClosure functionObj, Object arg) {
        ItemContainer.EvalArgPredicate predicate = TL_predicates.get().evalArg.alloc().init(functionObj, arg);
        InventoryItem item = this.getFirstRecurse(predicate);
        TL_predicates.get().evalArg.release(predicate);
        return item;
    }

    public InventoryItem getFirstTag(ItemTag itemTag) {
        ItemContainer.TagPredicate predicate = TL_predicates.get().tag.alloc().init(itemTag);
        InventoryItem item = this.getFirst(predicate);
        TL_predicates.get().tag.release(predicate);
        return item;
    }

    public InventoryItem getFirstTagRecurse(ItemTag itemTag) {
        ItemContainer.TagPredicate predicate = TL_predicates.get().tag.alloc().init(itemTag);
        InventoryItem item = this.getFirstRecurse(predicate);
        TL_predicates.get().tag.release(predicate);
        return item;
    }

    public InventoryItem getFirstTagEval(ItemTag itemTag, LuaClosure functionObj) {
        ItemContainer.TagEvalPredicate predicate = TL_predicates.get().tagEval.alloc().init(itemTag, functionObj);
        InventoryItem item = this.getFirstRecurse(predicate);
        TL_predicates.get().tagEval.release(predicate);
        return item;
    }

    public InventoryItem getFirstTagEvalRecurse(ItemTag itemTag, LuaClosure functionObj) {
        ItemContainer.TagEvalPredicate predicate = TL_predicates.get().tagEval.alloc().init(itemTag, functionObj);
        InventoryItem item = this.getFirstRecurse(predicate);
        TL_predicates.get().tagEval.release(predicate);
        return item;
    }

    public InventoryItem getFirstTagEvalArgRecurse(ItemTag itemTag, LuaClosure functionObj, Object arg) {
        ItemContainer.TagEvalArgPredicate predicate = TL_predicates.get().tagEvalArg.alloc().init(itemTag, functionObj, arg);
        InventoryItem item = this.getFirstRecurse(predicate);
        TL_predicates.get().tagEvalArg.release(predicate);
        return item;
    }

    public InventoryItem getFirstType(String type) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        InventoryItem item = this.getFirst(predicate);
        TL_predicates.get().type.release(predicate);
        return item;
    }

    public InventoryItem getFirstTypeRecurse(ItemKey key) {
        return this.getFirstTypeRecurse(key.toString());
    }

    public InventoryItem getFirstTypeRecurse(String type) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        InventoryItem item = this.getFirstRecurse(predicate);
        TL_predicates.get().type.release(predicate);
        return item;
    }

    public InventoryItem getFirstTypeEval(String type, LuaClosure functionObj) {
        ItemContainer.TypeEvalPredicate predicate = TL_predicates.get().typeEval.alloc().init(type, functionObj);
        InventoryItem item = this.getFirstRecurse(predicate);
        TL_predicates.get().typeEval.release(predicate);
        return item;
    }

    public InventoryItem getFirstTypeEvalRecurse(ItemKey key, LuaClosure functionObj) {
        return this.getFirstTypeEvalRecurse(key.toString(), functionObj);
    }

    public InventoryItem getFirstTypeEvalRecurse(String type, LuaClosure functionObj) {
        ItemContainer.TypeEvalPredicate predicate = TL_predicates.get().typeEval.alloc().init(type, functionObj);
        InventoryItem item = this.getFirstRecurse(predicate);
        TL_predicates.get().typeEval.release(predicate);
        return item;
    }

    public InventoryItem getFirstTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg) {
        ItemContainer.TypeEvalArgPredicate predicate = TL_predicates.get().typeEvalArg.alloc().init(type, functionObj, arg);
        InventoryItem item = this.getFirstRecurse(predicate);
        TL_predicates.get().typeEvalArg.release(predicate);
        return item;
    }

    public ArrayList<InventoryItem> getSomeCategory(String category, int count, ArrayList<InventoryItem> result) {
        ItemContainer.CategoryPredicate predicate = TL_predicates.get().category.alloc().init(category);
        ArrayList<InventoryItem> items = this.getSome(predicate, count, result);
        TL_predicates.get().category.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeCategoryRecurse(String category, int count, ArrayList<InventoryItem> result) {
        ItemContainer.CategoryPredicate predicate = TL_predicates.get().category.alloc().init(category);
        ArrayList<InventoryItem> items = this.getSomeRecurse(predicate, count, result);
        TL_predicates.get().category.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTag(ItemTag itemTag, int count, ArrayList<InventoryItem> result) {
        ItemContainer.TagPredicate predicate = TL_predicates.get().tag.alloc().init(itemTag);
        ArrayList<InventoryItem> items = this.getSome(predicate, count, result);
        TL_predicates.get().tag.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTagEval(ItemTag itemTag, LuaClosure functionObj, int count, ArrayList<InventoryItem> result) {
        ItemContainer.TagEvalPredicate predicate = TL_predicates.get().tagEval.alloc().init(itemTag, functionObj);
        ArrayList<InventoryItem> items = this.getSome(predicate, count, result);
        TL_predicates.get().tagEval.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTagEvalArg(ItemTag itemTag, LuaClosure functionObj, Object arg, int count, ArrayList<InventoryItem> result) {
        ItemContainer.TagEvalArgPredicate predicate = TL_predicates.get().tagEvalArg.alloc().init(itemTag, functionObj, arg);
        ArrayList<InventoryItem> items = this.getSome(predicate, count, result);
        TL_predicates.get().tagEvalArg.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTagRecurse(ItemTag itemTag, int count, ArrayList<InventoryItem> result) {
        ItemContainer.TagPredicate predicate = TL_predicates.get().tag.alloc().init(itemTag);
        ArrayList<InventoryItem> items = this.getSomeRecurse(predicate, count, result);
        TL_predicates.get().tag.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTagEvalRecurse(ItemTag itemTag, LuaClosure functionObj, int count, ArrayList<InventoryItem> result) {
        ItemContainer.TagEvalPredicate predicate = TL_predicates.get().tagEval.alloc().init(itemTag, functionObj);
        ArrayList<InventoryItem> items = this.getSomeRecurse(predicate, count, result);
        TL_predicates.get().tagEval.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTagEvalArgRecurse(ItemTag itemTag, LuaClosure functionObj, Object arg, int count, ArrayList<InventoryItem> result) {
        ItemContainer.TagEvalArgPredicate predicate = TL_predicates.get().tagEvalArg.alloc().init(itemTag, functionObj, arg);
        ArrayList<InventoryItem> items = this.getSomeRecurse(predicate, count, result);
        TL_predicates.get().tagEvalArg.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeType(String type, int count, ArrayList<InventoryItem> result) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        ArrayList<InventoryItem> items = this.getSome(predicate, count, result);
        TL_predicates.get().type.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTypeEval(String type, LuaClosure functionObj, int count, ArrayList<InventoryItem> result) {
        ItemContainer.TypeEvalPredicate predicate = TL_predicates.get().typeEval.alloc().init(type, functionObj);
        ArrayList<InventoryItem> items = this.getSome(predicate, count, result);
        TL_predicates.get().typeEval.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTypeEvalArg(String type, LuaClosure functionObj, Object arg, int count, ArrayList<InventoryItem> result) {
        ItemContainer.TypeEvalArgPredicate predicate = TL_predicates.get().typeEvalArg.alloc().init(type, functionObj, arg);
        ArrayList<InventoryItem> items = this.getSome(predicate, count, result);
        TL_predicates.get().typeEvalArg.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTypeRecurse(String type, int count, ArrayList<InventoryItem> result) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        ArrayList<InventoryItem> items = this.getSomeRecurse(predicate, count, result);
        TL_predicates.get().type.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTypeEvalRecurse(String type, LuaClosure functionObj, int count, ArrayList<InventoryItem> result) {
        ItemContainer.TypeEvalPredicate predicate = TL_predicates.get().typeEval.alloc().init(type, functionObj);
        ArrayList<InventoryItem> items = this.getSomeRecurse(predicate, count, result);
        TL_predicates.get().typeEval.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg, int count, ArrayList<InventoryItem> result) {
        ItemContainer.TypeEvalArgPredicate predicate = TL_predicates.get().typeEvalArg.alloc().init(type, functionObj, arg);
        ArrayList<InventoryItem> items = this.getSomeRecurse(predicate, count, result);
        TL_predicates.get().typeEvalArg.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeEval(LuaClosure functionObj, int count, ArrayList<InventoryItem> result) {
        ItemContainer.EvalPredicate predicate = TL_predicates.get().eval.alloc().init(functionObj);
        ArrayList<InventoryItem> items = this.getSome(predicate, count, result);
        TL_predicates.get().eval.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeEvalArg(LuaClosure functionObj, Object arg, int count, ArrayList<InventoryItem> result) {
        ItemContainer.EvalArgPredicate predicate = TL_predicates.get().evalArg.alloc().init(functionObj, arg);
        ArrayList<InventoryItem> items = this.getSome(predicate, count, result);
        TL_predicates.get().evalArg.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeEvalRecurse(LuaClosure functionObj, int count, ArrayList<InventoryItem> result) {
        ItemContainer.EvalPredicate predicate = TL_predicates.get().eval.alloc().init(functionObj);
        ArrayList<InventoryItem> items = this.getSomeRecurse(predicate, count, result);
        TL_predicates.get().eval.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeEvalArgRecurse(LuaClosure functionObj, Object arg, int count, ArrayList<InventoryItem> result) {
        ItemContainer.EvalArgPredicate predicate = TL_predicates.get().evalArg.alloc().init(functionObj, arg);
        ArrayList<InventoryItem> items = this.getSomeRecurse(predicate, count, result);
        TL_predicates.get().evalArg.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllCategory(String category, ArrayList<InventoryItem> result) {
        ItemContainer.CategoryPredicate predicate = TL_predicates.get().category.alloc().init(category);
        ArrayList<InventoryItem> items = this.getAll(predicate, result);
        TL_predicates.get().category.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllCategoryRecurse(String category, ArrayList<InventoryItem> result) {
        ItemContainer.CategoryPredicate predicate = TL_predicates.get().category.alloc().init(category);
        ArrayList<InventoryItem> items = this.getAllRecurse(predicate, result);
        TL_predicates.get().category.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTag(ItemTag itemTag) {
        ArrayList<InventoryItem> result = new ArrayList<>();
        return this.getAllTag(itemTag, result);
    }

    public ArrayList<InventoryItem> getAllTag(ItemTag itemTag, ArrayList<InventoryItem> result) {
        ItemContainer.TagPredicate predicate = TL_predicates.get().tag.alloc().init(itemTag);
        ArrayList<InventoryItem> items = this.getAll(predicate, result);
        TL_predicates.get().tag.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTagEval(ItemTag itemTag, LuaClosure functionObj, ArrayList<InventoryItem> result) {
        if (result == null) {
            result = new ArrayList<>();
        }

        ItemContainer.TagEvalPredicate predicate = TL_predicates.get().tagEval.alloc().init(itemTag, functionObj);
        ArrayList<InventoryItem> items = this.getAll(predicate, result);
        TL_predicates.get().tagEval.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTagEvalArg(ItemTag itemTag, LuaClosure functionObj, Object arg, ArrayList<InventoryItem> result) {
        ItemContainer.TagEvalArgPredicate predicate = TL_predicates.get().tagEvalArg.alloc().init(itemTag, functionObj, arg);
        ArrayList<InventoryItem> items = this.getAll(predicate, result);
        TL_predicates.get().tagEvalArg.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTagRecurse(ItemTag itemTag, ArrayList<InventoryItem> result) {
        ItemContainer.TagPredicate predicate = TL_predicates.get().tag.alloc().init(itemTag);
        ArrayList<InventoryItem> items = this.getAllRecurse(predicate, result);
        TL_predicates.get().tag.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTagEvalRecurse(ItemTag itemTag, LuaClosure functionObj, ArrayList<InventoryItem> result) {
        ItemContainer.TagEvalPredicate predicate = TL_predicates.get().tagEval.alloc().init(itemTag, functionObj);
        ArrayList<InventoryItem> items = this.getAllRecurse(predicate, result);
        TL_predicates.get().tagEval.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTagEvalArgRecurse(ItemTag itemTag, LuaClosure functionObj, Object arg, ArrayList<InventoryItem> result) {
        ItemContainer.TagEvalArgPredicate predicate = TL_predicates.get().tagEvalArg.alloc().init(itemTag, functionObj, arg);
        ArrayList<InventoryItem> items = this.getAllRecurse(predicate, result);
        TL_predicates.get().tagEvalArg.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllType(String type, ArrayList<InventoryItem> result) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        ArrayList<InventoryItem> items = this.getAll(predicate, result);
        TL_predicates.get().type.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTypeEval(String type, LuaClosure functionObj, ArrayList<InventoryItem> result) {
        ItemContainer.TypeEvalPredicate predicate = TL_predicates.get().typeEval.alloc().init(type, functionObj);
        ArrayList<InventoryItem> items = this.getAll(predicate, result);
        TL_predicates.get().typeEval.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTypeEvalArg(String type, LuaClosure functionObj, Object arg, ArrayList<InventoryItem> result) {
        ItemContainer.TypeEvalArgPredicate predicate = TL_predicates.get().typeEvalArg.alloc().init(type, functionObj, arg);
        ArrayList<InventoryItem> items = this.getAll(predicate, result);
        TL_predicates.get().typeEvalArg.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTypeRecurse(String type, ArrayList<InventoryItem> result) {
        ItemContainer.TypePredicate predicate = TL_predicates.get().type.alloc().init(type);
        ArrayList<InventoryItem> items = this.getAllRecurse(predicate, result);
        TL_predicates.get().type.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTypeEvalRecurse(String type, LuaClosure functionObj, ArrayList<InventoryItem> result) {
        ItemContainer.TypeEvalPredicate predicate = TL_predicates.get().typeEval.alloc().init(type, functionObj);
        ArrayList<InventoryItem> items = this.getAllRecurse(predicate, result);
        TL_predicates.get().typeEval.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg, ArrayList<InventoryItem> result) {
        ItemContainer.TypeEvalArgPredicate predicate = TL_predicates.get().typeEvalArg.alloc().init(type, functionObj, arg);
        ArrayList<InventoryItem> items = this.getAllRecurse(predicate, result);
        TL_predicates.get().typeEvalArg.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllEval(LuaClosure functionObj, ArrayList<InventoryItem> result) {
        ItemContainer.EvalPredicate predicate = TL_predicates.get().eval.alloc().init(functionObj);
        ArrayList<InventoryItem> items = this.getAll(predicate, result);
        TL_predicates.get().eval.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllEvalArg(LuaClosure functionObj, Object arg, ArrayList<InventoryItem> result) {
        ItemContainer.EvalArgPredicate predicate = TL_predicates.get().evalArg.alloc().init(functionObj, arg);
        ArrayList<InventoryItem> items = this.getAll(predicate, result);
        TL_predicates.get().evalArg.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllEvalRecurse(LuaClosure functionObj, ArrayList<InventoryItem> result) {
        ItemContainer.EvalPredicate predicate = TL_predicates.get().eval.alloc().init(functionObj);
        ArrayList<InventoryItem> items = this.getAllRecurse(predicate, result);
        TL_predicates.get().eval.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getAllEvalArgRecurse(LuaClosure functionObj, Object arg, ArrayList<InventoryItem> result) {
        ItemContainer.EvalArgPredicate predicate = TL_predicates.get().evalArg.alloc().init(functionObj, arg);
        ArrayList<InventoryItem> items = this.getAllRecurse(predicate, result);
        TL_predicates.get().evalArg.release(predicate);
        return items;
    }

    public ArrayList<InventoryItem> getSomeCategory(String category, int count) {
        return this.getSomeCategory(category, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeEval(LuaClosure functionObj, int count) {
        return this.getSomeEval(functionObj, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeEvalArg(LuaClosure functionObj, Object arg, int count) {
        return this.getSomeEvalArg(functionObj, arg, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeTypeEval(String type, LuaClosure functionObj, int count) {
        return this.getSomeTypeEval(type, functionObj, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeTypeEvalArg(String type, LuaClosure functionObj, Object arg, int count) {
        return this.getSomeTypeEvalArg(type, functionObj, arg, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeEvalRecurse(LuaClosure functionObj, int count) {
        return this.getSomeEvalRecurse(functionObj, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeEvalArgRecurse(LuaClosure functionObj, Object arg, int count) {
        return this.getSomeEvalArgRecurse(functionObj, arg, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeTag(ItemTag itemTag, int count) {
        return this.getSomeTag(itemTag, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeTagRecurse(ItemTag itemTag, int count) {
        return this.getSomeTagRecurse(itemTag, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeTagEvalRecurse(ItemTag itemTag, LuaClosure functionObj, int count) {
        return this.getSomeTagEvalRecurse(itemTag, functionObj, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeTagEvalArgRecurse(ItemTag itemTag, LuaClosure functionObj, Object arg, int count) {
        return this.getSomeTagEvalArgRecurse(itemTag, functionObj, arg, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeType(String type, int count) {
        return this.getSomeType(type, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeTypeRecurse(String type, int count) {
        return this.getSomeTypeRecurse(type, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeTypeEvalRecurse(String type, LuaClosure functionObj, int count) {
        return this.getSomeTypeEvalRecurse(type, functionObj, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getSomeTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg, int count) {
        return this.getSomeTypeEvalArgRecurse(type, functionObj, arg, count, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAll(Predicate<InventoryItem> predicate) {
        return this.getAll(predicate, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllCategory(String category) {
        return this.getAllCategory(category, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllEval(LuaClosure functionObj) {
        return this.getAllEval(functionObj, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllEvalArg(LuaClosure functionObj, Object arg) {
        return this.getAllEvalArg(functionObj, arg, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllTagEval(ItemTag itemTag, LuaClosure functionObj) {
        return this.getAllTagEval(itemTag, functionObj, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllTagEvalArg(ItemTag itemTag, LuaClosure functionObj, Object arg) {
        return this.getAllTagEvalArg(itemTag, functionObj, arg, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllTypeEval(String type, LuaClosure functionObj) {
        return this.getAllTypeEval(type, functionObj, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllTypeEvalArg(String type, LuaClosure functionObj, Object arg) {
        return this.getAllTypeEvalArg(type, functionObj, arg, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllEvalRecurse(LuaClosure functionObj) {
        return this.getAllEvalRecurse(functionObj, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllEvalArgRecurse(LuaClosure functionObj, Object arg) {
        return this.getAllEvalArgRecurse(functionObj, arg, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllType(String type) {
        return this.getAllType(type, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllTypeRecurse(String type) {
        return this.getAllTypeRecurse(type, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllTypeEvalRecurse(String type, LuaClosure functionObj) {
        return this.getAllTypeEvalRecurse(type, functionObj, new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllTypeEvalArgRecurse(String type, LuaClosure functionObj, Object arg) {
        return this.getAllTypeEvalArgRecurse(type, functionObj, arg, new ArrayList<>());
    }

    public InventoryItem FindAndReturnCategory(String category) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.getCategory().equals(category)) {
                return item;
            }
        }

        return null;
    }

    public ArrayList<InventoryItem> FindAndReturn(String type, int count) {
        return this.getSomeType(type, count);
    }

    public InventoryItem FindAndReturn(String type, ArrayList<InventoryItem> itemToCheck) {
        if (type == null) {
            return null;
        } else {
            for (int i = 0; i < this.items.size(); i++) {
                InventoryItem item = this.items.get(i);
                if (item.type != null && compareType(type, item) && !itemToCheck.contains(item)) {
                    return item;
                }
            }

            return null;
        }
    }

    public InventoryItem FindAndReturn(String type) {
        return this.getFirstType(type);
    }

    public ArrayList<InventoryItem> FindAll(String type) {
        return this.getAllType(type);
    }

    public InventoryItem FindAndReturnStack(String type) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (compareType(type, item)) {
                InventoryItem test = InventoryItemFactory.CreateItem(item.module + "." + type);
                if (item.CanStack(test)) {
                    return item;
                }
            }
        }

        return null;
    }

    public InventoryItem FindAndReturnStack(InventoryItem itemlike) {
        String String = itemlike.type;

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if ((item.type == null ? String == null : item.type.equals(String)) && item.CanStack(itemlike)) {
                return item;
            }
        }

        return null;
    }

    public boolean HasType(ItemType itemType) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.itemType == itemType) {
                return true;
            }
        }

        return false;
    }

    public void Remove(InventoryItem item) {
        if (this.getCharacter() != null) {
            this.getCharacter().removeFromHands(item);
        }

        for (int m = 0; m < this.items.size(); m++) {
            InventoryItem item2 = this.items.get(m);
            if (item2 == item) {
                item.OnBeforeRemoveFromContainer(this);
                this.items.remove(item);
                item.container = null;
                this.drawDirty = true;
                this.dirty = true;
                if (this.parent != null) {
                    this.dirty = true;
                }

                if (this.parent instanceof IsoDeadBody isoDeadBody) {
                    isoDeadBody.checkClothing(item);
                }

                if (this.parent instanceof IsoMannequin isoMannequin) {
                    isoMannequin.checkClothing(item);
                }

                if (this.getParent() != null) {
                    this.getParent().flagForHotSave();
                }

                return;
            }
        }
    }

    public void DoRemoveItem(InventoryItem item) {
        this.drawDirty = true;
        if (this.parent != null) {
            this.dirty = true;
        }

        this.items.remove(item);
        item.container = null;
        if (this.parent instanceof IsoDeadBody isoDeadBody) {
            isoDeadBody.checkClothing(item);
        }

        if (this.parent instanceof IsoMannequin isoMannequin) {
            isoMannequin.checkClothing(item);
        }

        if (this.parent instanceof IsoFeedingTrough isoFeedingTrough) {
            isoFeedingTrough.onRemoveFood();
        }

        if (this.getParent() != null) {
            this.getParent().flagForHotSave();
        }
    }

    public void Remove(String itemTypes) {
        for (int m = 0; m < this.items.size(); m++) {
            InventoryItem item = this.items.get(m);
            if (item.type.equals(itemTypes)) {
                if (item.getCurrentUses() > 1) {
                    item.setCurrentUses(item.getCurrentUses() - 1);
                } else {
                    this.items.remove(item);
                }

                item.container = null;
                this.drawDirty = true;
                this.dirty = true;
                if (this.parent != null) {
                    this.dirty = true;
                }

                if (this.getParent() != null) {
                    this.getParent().flagForHotSave();
                }

                return;
            }
        }
    }

    public InventoryItem Remove(ItemType itemType) {
        for (int m = 0; m < this.items.size(); m++) {
            InventoryItem item = this.items.get(m);
            if (item.itemType == itemType) {
                this.items.remove(item);
                item.container = null;
                this.drawDirty = true;
                this.dirty = true;
                if (this.parent != null) {
                    this.dirty = true;
                }

                if (this.getParent() != null) {
                    this.getParent().flagForHotSave();
                }

                return item;
            }
        }

        return null;
    }

    public InventoryItem Find(String itemType) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.type.equals(itemType)) {
                return item;
            }
        }

        return null;
    }

    public InventoryItem Find(ItemType itemType) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.itemType == itemType) {
                return item;
            }
        }

        return null;
    }

    public ArrayList<InventoryItem> RemoveAll(String itemType) {
        return this.RemoveAll(itemType, this.items.size());
    }

    public ArrayList<InventoryItem> RemoveAll(String itemType, int count) {
        this.drawDirty = true;
        if (this.parent != null) {
            this.dirty = true;
        }

        ArrayList<InventoryItem> toRemoveList = new ArrayList<>();

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.type.equals(itemType) || item.fullType.equals(itemType)) {
                item.container = null;
                toRemoveList.add(item);
                this.dirty = true;
                if (toRemoveList.size() >= count) {
                    break;
                }
            }
        }

        for (InventoryItem toRemove : toRemoveList) {
            this.items.remove(toRemove);
        }

        return toRemoveList;
    }

    public InventoryItem RemoveOneOf(String String, boolean insideInv) {
        this.drawDirty = true;
        if (this.parent != null && !(this.parent instanceof IsoGameCharacter)) {
            this.dirty = true;
        }

        for (int m = 0; m < this.items.size(); m++) {
            InventoryItem item = this.items.get(m);
            if (item.getFullType().equals(String) || item.type.equals(String)) {
                if (item.getCurrentUses() > 1) {
                    item.setCurrentUses(item.getCurrentUses() - 1);
                } else {
                    item.container = null;
                    this.items.remove(item);
                }

                this.dirty = true;
                return item;
            }
        }

        if (insideInv) {
            for (int i = 0; i < this.items.size(); i++) {
                InventoryItem item = this.items.get(i);
                if (item instanceof InventoryContainer container
                    && container.getItemContainer() != null
                    && container.getItemContainer().RemoveOneOf(String, insideInv) != null) {
                    return item;
                }
            }
        }

        return null;
    }

    public void RemoveOneOf(String String) {
        this.RemoveOneOf(String, true);
    }

    public float getContentsWeight() {
        float total = 0.0F;

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            total += item.getUnequippedWeight();
        }

        return total;
    }

    public float getMaxWeight() {
        return this.parent instanceof IsoGameCharacter isoGameCharacter ? isoGameCharacter.getMaxWeight() : this.getCapacity();
    }

    public float getCapacityWeight() {
        if (this.parent instanceof IsoPlayer player) {
            if (Core.debug && player.isGhostMode() || !player.isAccessLevel("None") && player.isUnlimitedCarry()) {
                return 0.0F;
            }

            if (player.isUnlimitedCarry()) {
                return 0.0F;
            }
        }

        if (this.parent instanceof IsoGameCharacter chr) {
            return chr.getInventoryWeight();
        } else {
            return this.parent instanceof IsoDeadBody deadBody ? deadBody.getInventoryWeight() : this.getContentsWeight();
        }
    }

    public float getAvailableWeightCapacity() {
        float occupiedWeight = this.getCapacityWeight();
        float maxWeight = this.getMaxWeight();
        return maxWeight - occupiedWeight;
    }

    public boolean isEmpty() {
        return this.items == null || this.items.isEmpty();
    }

    public boolean isEmptyOrUnwanted(IsoPlayer player) {
        if (this.items != null && !this.items.isEmpty()) {
            for (int i = 0; i < this.getItems().size(); i++) {
                InventoryItem item = this.getItems().get(i);
                if (!item.isUnwanted(player)) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    public boolean isMicrowave() {
        return "microwave".equals(this.getType());
    }

    private static boolean isSquareInRoom(IsoGridSquare square) {
        return square == null ? false : square.getRoom() != null;
    }

    private static boolean isSquarePowered(IsoGridSquare square) {
        if (square == null) {
            return false;
        } else {
            boolean bHydroPower = square.hasGridPower();
            if (bHydroPower && square.getRoom() != null) {
                return true;
            } else if (square.haveElectricity()) {
                return true;
            } else {
                if (bHydroPower && square.getRoom() == null) {
                    IsoGridSquare sqN = square.getAdjacentSquare(IsoDirections.N);
                    IsoGridSquare sqS = square.getAdjacentSquare(IsoDirections.S);
                    IsoGridSquare sqW = square.getAdjacentSquare(IsoDirections.W);
                    IsoGridSquare sqE = square.getAdjacentSquare(IsoDirections.E);
                    if (isSquareInRoom(sqN) || isSquareInRoom(sqS) || isSquareInRoom(sqW) || isSquareInRoom(sqE)) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public boolean isPowered() {
        return this.parent != null && this.parent.getObjectIndex() != -1 && this.parent.checkObjectPowered();
    }

    public static boolean isObjectPowered(IsoObject parent) {
        if (parent != null && parent.getObjectIndex() != -1) {
            ArrayList<IsoObject> s_tempObjects = ItemContainer.s_tempObjects.get();
            parent.getSpriteGridObjects(s_tempObjects);
            if (s_tempObjects.isEmpty()) {
                if (parent.getProperties() != null && parent.getProperties().has("streetlight")) {
                    return parent instanceof IsoLightSwitch lightSwitch && !lightSwitch.isActivated()
                        ? false
                        : GameTime.getInstance().getNight() >= 0.5F && parent.hasGridPower();
                } else {
                    IsoGridSquare sq = parent.getSquare();
                    return isSquarePowered(sq);
                }
            } else {
                IsoLightSwitch lightSwitch = null;

                for (int i = 0; i < s_tempObjects.size(); i++) {
                    IsoObject object = s_tempObjects.get(i);
                    if (object instanceof IsoLightSwitch lightSwitchObject) {
                        lightSwitch = lightSwitchObject;
                        break;
                    }
                }

                for (int ix = 0; ix < s_tempObjects.size(); ix++) {
                    IsoObject object = s_tempObjects.get(ix);
                    if (object.getProperties() != null && object.getProperties().has("streetlight")) {
                        if (lightSwitch != null && !lightSwitch.isActivated()) {
                            return false;
                        }

                        return GameTime.getInstance().getNight() >= 0.5F && object.hasGridPower();
                    }

                    IsoGridSquare testSq = object.getSquare();
                    if (isSquarePowered(testSq)) {
                        return true;
                    }
                }

                return false;
            }
        } else {
            return false;
        }
    }

    public float getTemprature() {
        if (this.customTemperature != 0.0F) {
            return this.customTemperature;
        } else {
            boolean isFridge = false;
            if (this.getParent() != null && this.getParent().getSprite() != null) {
                isFridge = this.getParent().getSprite().getProperties().has("IsFridge");
            }

            if (this.isPowered()) {
                if (this.type.equals("fridge") || this.type.equals("freezer") || isFridge) {
                    return 0.2F;
                }

                if ((this.isStove() || "microwave".equals(this.type)) && this.parent instanceof IsoStove isoStove) {
                    return isoStove.getCurrentTemperature();
                }
            }

            if (this.parent instanceof IsoBarbecue isoBarbecue) {
                return isoBarbecue.getTemperature();
            } else if (this.parent instanceof IsoFireplace isoFireplace) {
                return isoFireplace.getTemperature();
            } else if ((this.type.equals("fridge") || this.type.equals("freezer") || isFridge)
                && (float)(GameTime.getInstance().getWorldAgeHours() / 24.0 + (SandboxOptions.instance.timeSinceApo.getValue() - 1) * 30)
                    == SandboxOptions.instance.getElecShutModifier()
                && GameTime.instance.getTimeOfDay() < 13.0F) {
                float delta = (GameTime.instance.getTimeOfDay() - 7.0F) / 6.0F;
                return GameTime.instance.Lerp(0.2F, 1.0F, delta);
            } else {
                return 1.0F;
            }
        }
    }

    public boolean isTemperatureChanging() {
        if (this.parent instanceof IsoBarbecue bbq) {
            return bbq.isTemperatureChanging();
        } else if (this.parent instanceof IsoFireplace fireplace) {
            return fireplace.isTemperatureChanging();
        } else {
            return this.parent instanceof IsoStove stove ? stove.isTemperatureChanging() : false;
        }
    }

    public ArrayList<InventoryItem> save(ByteBuffer output, IsoGameCharacter noCompress) throws IOException {
        GameWindow.WriteString(output, this.type);
        output.put((byte)(this.explored ? 1 : 0));
        ArrayList<InventoryItem> savedItems = CompressIdenticalItems.save(output, this.items, null);
        output.put((byte)(this.isHasBeenLooted() ? 1 : 0));
        output.putInt(this.capacity);
        return savedItems;
    }

    public ArrayList<InventoryItem> save(ByteBuffer output) throws IOException {
        return this.save(output, null);
    }

    public ArrayList<InventoryItem> load(ByteBuffer input, int WorldVersion) throws IOException {
        this.type = GameWindow.ReadString(input);
        this.explored = input.get() == 1;
        ArrayList<InventoryItem> savedItems = CompressIdenticalItems.load(input, WorldVersion, this.items, this.includingObsoleteItems);

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            item.container = this;
        }

        this.setHasBeenLooted(input.get() == 1);
        this.capacity = input.getInt();
        this.dirty = false;
        return savedItems;
    }

    public boolean isDrawDirty() {
        return this.drawDirty;
    }

    public void setDrawDirty(boolean b) {
        this.drawDirty = b;
    }

    public InventoryItem getBestWeapon(SurvivorDesc desc) {
        InventoryItem best = null;
        float bestscore = -1.0E7F;

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item instanceof HandWeapon) {
                float score = item.getScore(desc);
                if (score >= bestscore) {
                    bestscore = score;
                    best = item;
                }
            }
        }

        return best;
    }

    public InventoryItem getBestWeapon() {
        InventoryItem best = null;
        float bestscore = 0.0F;

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item instanceof HandWeapon) {
                float score = item.getScore(null);
                if (score >= bestscore) {
                    bestscore = score;
                    best = item;
                }
            }
        }

        return best;
    }

    public float getTotalFoodScore(SurvivorDesc desc) {
        float score = 0.0F;

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item instanceof Food) {
                score += item.getScore(desc);
            }
        }

        return score;
    }

    public float getTotalWeaponScore(SurvivorDesc desc) {
        float score = 0.0F;

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item instanceof HandWeapon) {
                score += item.getScore(desc);
            }
        }

        return score;
    }

    public InventoryItem getBestFood(SurvivorDesc descriptor) {
        InventoryItem best = null;
        float bestscore = 0.0F;

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item instanceof Food food) {
                float score = item.getScore(descriptor);
                if (food.isbDangerousUncooked() && !item.isCooked()) {
                    score *= 0.2F;
                }

                if (item.age > item.offAge) {
                    score *= 0.2F;
                }

                if (score >= bestscore) {
                    bestscore = score;
                    best = item;
                }
            }
        }

        return best;
    }

    public InventoryItem getBestBandage(SurvivorDesc descriptor) {
        InventoryItem best = null;

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.isCanBandage()) {
                best = item;
                break;
            }
        }

        return best;
    }

    public int getNumItems(String item) {
        int ncount = 0;
        if (item.contains("Type:")) {
            for (int i = 0; i < this.items.size(); i++) {
                InventoryItem Item = this.items.get(i);
                if (Item instanceof Food && item.contains("Food")) {
                    ncount += Item.getCurrentUses();
                }

                if (Item instanceof HandWeapon && item.contains("Weapon")) {
                    ncount += Item.getCurrentUses();
                }
            }
        } else {
            for (int i = 0; i < this.items.size(); i++) {
                InventoryItem Itemx = this.items.get(i);
                if (Itemx.type.equals(item)) {
                    ncount += Itemx.getCurrentUses();
                }
            }
        }

        return ncount;
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return this.active;
    }

    /**
     * 
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @return the dirty
     */
    public boolean isDirty() {
        return this.dirty;
    }

    /**
     * 
     * @param dirty the dirty to set
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /**
     * @return the IsDevice
     */
    public boolean isIsDevice() {
        return this.isdevice;
    }

    /**
     * 
     * @param IsDevice the IsDevice to set
     */
    public void setIsDevice(boolean IsDevice) {
        this.isdevice = IsDevice;
    }

    /**
     * @return the ageFactor
     */
    public float getAgeFactor() {
        return this.ageFactor;
    }

    /**
     * 
     * @param ageFactor the ageFactor to set
     */
    public void setAgeFactor(float ageFactor) {
        this.ageFactor = ageFactor;
    }

    /**
     * @return the CookingFactor
     */
    public float getCookingFactor() {
        return this.cookingFactor;
    }

    /**
     * 
     * @param CookingFactor the CookingFactor to set
     */
    public void setCookingFactor(float CookingFactor) {
        this.cookingFactor = CookingFactor;
    }

    /**
     * @return the Items
     */
    public ArrayList<InventoryItem> getItems() {
        return this.items;
    }

    /**
     * 
     * @param Items the Items to set
     */
    public void setItems(ArrayList<InventoryItem> Items) {
        this.items = Items;
    }

    public void takeItemsFrom(ItemContainer other) {
        while (!other.isEmpty()) {
            InventoryItem item = other.getItems().get(0);
            other.Remove(item);
            this.AddItem(item);
        }
    }

    /**
     * @return the parent
     */
    public IsoObject getParent() {
        return this.parent;
    }

    /**
     * 
     * @param parent the parent to set
     */
    public void setParent(IsoObject parent) {
        this.parent = parent;
    }

    /**
     * @return the SourceGrid
     */
    public IsoGridSquare getSourceGrid() {
        return this.sourceGrid;
    }

    /**
     * 
     * @param SourceGrid the SourceGrid to set
     */
    public void setSourceGrid(IsoGridSquare SourceGrid) {
        this.sourceGrid = SourceGrid;
    }

    /**
     * @return the type
     */
    public String getType() {
        return this.type;
    }

    /**
     * 
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    public void clear() {
        this.items.clear();
        this.dirty = true;
        this.drawDirty = true;
    }

    public int getWaterContainerCount() {
        int c = 0;

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.hasComponent(ComponentType.FluidContainer)) {
                c++;
            }
        }

        return c;
    }

    public InventoryItem FindWaterSource() {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.isWaterSource()) {
                if (!(item instanceof Drainable)) {
                    return item;
                }

                if (item.getCurrentUses() > 0) {
                    return item;
                }
            }
        }

        return null;
    }

    public ArrayList<InventoryItem> getAllWaterFillables() {
        tempList.clear();

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.hasComponent(ComponentType.FluidContainer) && item.getFluidContainer().isEmpty()) {
                tempList.add(item);
            }
        }

        return tempList;
    }

    public InventoryItem getFirstWaterFluidSources(boolean includeTainted) {
        ArrayList<InventoryItem> result = this.getAllWaterFluidSources(includeTainted);
        return result.isEmpty() ? null : result.get(0);
    }

    public InventoryItem getFirstWaterFluidSources(boolean includeTainted, boolean taintedPriority) {
        ArrayList<InventoryItem> result = this.getAllWaterFluidSources(includeTainted);
        if (taintedPriority) {
            for (int i = 0; i < result.size(); i++) {
                InventoryItem item = result.get(i);
                if (item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("TaintedWater")) {
                    return item;
                }
            }
        }

        return result.isEmpty() ? null : result.get(0);
    }

    public InventoryItem getFirstFluidContainer(String type) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.hasComponent(ComponentType.FluidContainer)) {
                if (item.getFluidContainer().isEmpty()) {
                    return item;
                }

                if (!StringUtils.isNullOrEmpty(type)
                    && item.getFluidContainer().getPrimaryFluid() != null
                    && item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equalsIgnoreCase(type)) {
                    return item;
                }
            }
        }

        return null;
    }

    public ArrayList<InventoryItem> getAvailableFluidContainer(String type) {
        tempList.clear();

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.hasComponent(ComponentType.FluidContainer)) {
                if (item.getFluidContainer().isEmpty()) {
                    tempList.add(item);
                }

                if (!StringUtils.isNullOrEmpty(type)
                    && item.getFluidContainer().getPrimaryFluid() != null
                    && item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equalsIgnoreCase(type)
                    && !item.getFluidContainer().isFull()) {
                    tempList.add(item);
                }
            }
        }

        return tempList;
    }

    public float getAvailableFluidContainersCapacity(String type) {
        float volume = 0.0F;

        for (InventoryItem item : this.getAvailableFluidContainer(type)) {
            volume += item.getFluidContainer().getFreeCapacity();
        }

        return volume;
    }

    public InventoryItem getFirstAvailableFluidContainer(String type) {
        ArrayList<InventoryItem> result = this.getAvailableFluidContainer(type);
        return result.isEmpty() ? null : result.get(0);
    }

    public ArrayList<InventoryItem> getAllWaterFluidSources(boolean includeTainted) {
        ArrayList<InventoryItem> tempList = new ArrayList<>();

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.hasComponent(ComponentType.FluidContainer)
                && !item.getFluidContainer().isEmpty()
                && (
                    item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("Water")
                        || item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("CarbonatedWater")
                        || includeTainted && item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("TaintedWater")
                )) {
                tempList.add(item);
            }
        }

        return tempList;
    }

    public InventoryItem getFirstCleaningFluidSources() {
        ArrayList<InventoryItem> result = this.getAllCleaningFluidSources();
        return result.isEmpty() ? null : result.get(0);
    }

    public ArrayList<InventoryItem> getAllCleaningFluidSources() {
        ArrayList<InventoryItem> tempList = new ArrayList<>();

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.hasComponent(ComponentType.FluidContainer)
                && !item.getFluidContainer().isEmpty()
                && (
                    item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("Bleach")
                        || item.getFluidContainer().getPrimaryFluid().getFluidTypeString().equals("CleaningLiquid")
                )) {
                tempList.add(item);
            }
        }

        return tempList;
    }

    public int getItemCount(String type) {
        return this.getCountType(type);
    }

    public int getItemCountRecurse(String type) {
        return this.getCountTypeRecurse(type);
    }

    public int getItemCount(String type, boolean doBags) {
        return doBags ? this.getCountTypeRecurse(type) : this.getCountType(type);
    }

    private static int getUses(ItemContainer.InventoryItemList items) {
        int count = 0;

        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof DrainableComboItem drainable) {
                count += drainable.getCurrentUses();
            } else {
                count++;
            }
        }

        return count;
    }

    public int getUsesRecurse(Predicate<InventoryItem> predicate) {
        ItemContainer.InventoryItemList items = TL_itemListPool.get().alloc();
        this.getAllRecurse(predicate, items);
        int count = getUses(items);
        TL_itemListPool.get().release(items);
        return count;
    }

    public int getUsesType(String type) {
        ItemContainer.InventoryItemList items = TL_itemListPool.get().alloc();
        this.getAllType(type, items);
        int count = getUses(items);
        TL_itemListPool.get().release(items);
        return count;
    }

    public int getUsesTypeRecurse(String type) {
        ItemContainer.InventoryItemList items = TL_itemListPool.get().alloc();
        this.getAllTypeRecurse(type, items);
        int count = getUses(items);
        TL_itemListPool.get().release(items);
        return count;
    }

    public int getWeightReduction() {
        return this.weightReduction;
    }

    public void setWeightReduction(int weightReduction) {
        weightReduction = Math.min(weightReduction, 100);
        weightReduction = Math.max(weightReduction, 0);
        this.weightReduction = weightReduction;
    }

    public void removeAllItems() {
        this.drawDirty = true;
        if (this.parent != null) {
            this.dirty = true;
        }

        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            item.container = null;
        }

        this.items.clear();
        if (this.parent instanceof IsoDeadBody isoDeadBody) {
            isoDeadBody.checkClothing(null);
        }

        if (this.parent instanceof IsoMannequin isoMannequin) {
            isoMannequin.checkClothing(null);
        }
    }

    public boolean containsRecursive(InventoryItem item) {
        for (int i = 0; i < this.getItems().size(); i++) {
            InventoryItem inventoryItem = this.getItems().get(i);
            if (inventoryItem == item) {
                return true;
            }

            if (inventoryItem instanceof InventoryContainer container && container.getInventory().containsRecursive(item)) {
                return true;
            }
        }

        return false;
    }

    public int getItemCountFromTypeRecurse(String type) {
        int count = 0;

        for (int i = 0; i < this.getItems().size(); i++) {
            InventoryItem inventoryItem = this.getItems().get(i);
            if (inventoryItem.getFullType().equals(type)) {
                count++;
            }

            if (inventoryItem instanceof InventoryContainer container) {
                int ocount = container.getInventory().getItemCountFromTypeRecurse(type);
                count += ocount;
            }
        }

        return count;
    }

    public float getCustomTemperature() {
        return this.customTemperature;
    }

    public void setCustomTemperature(float newTemp) {
        this.customTemperature = newTemp;
    }

    public InventoryItem getItemFromType(String type, IsoGameCharacter chr, boolean notEquipped, boolean ignoreBroken, boolean includeInv) {
        ItemContainer.InventoryItemList bags = TL_itemListPool.get().alloc();
        if (type.contains(".")) {
            type = type.split("\\.")[1];
        }

        for (int i = 0; i < this.getItems().size(); i++) {
            InventoryItem item = this.getItems().get(i);
            if (!item.getFullType().equals(type) && !item.getType().equals(type)) {
                if (includeInv && item instanceof InventoryContainer container && container.getInventory() != null && !bags.contains(item)) {
                    bags.add(item);
                }
            } else if ((!notEquipped || chr == null || !chr.isEquippedClothing(item)) && this.testBroken(ignoreBroken, item)) {
                TL_itemListPool.get().release(bags);
                return item;
            }
        }

        for (int j = 0; j < bags.size(); j++) {
            ItemContainer container = ((InventoryContainer)bags.get(j)).getInventory();
            InventoryItem item = container.getItemFromType(type, chr, notEquipped, ignoreBroken, includeInv);
            if (item != null) {
                TL_itemListPool.get().release(bags);
                return item;
            }
        }

        TL_itemListPool.get().release(bags);
        return null;
    }

    public InventoryItem getItemFromTag(ItemTag itemTag, IsoGameCharacter chr, boolean notEquipped, boolean ignoreBroken, boolean includeInv) {
        ItemContainer.InventoryItemList bags = TL_itemListPool.get().alloc();

        for (int i = 0; i < this.getItems().size(); i++) {
            InventoryItem item = this.getItems().get(i);
            if (item.hasTag(itemTag)) {
                if ((!notEquipped || chr == null || !chr.isEquippedClothing(item)) && this.testBroken(ignoreBroken, item)) {
                    TL_itemListPool.get().release(bags);
                    return item;
                }
            } else if (includeInv && item instanceof InventoryContainer container && container.getInventory() != null && !bags.contains(item)) {
                bags.add(item);
            }
        }

        for (int j = 0; j < bags.size(); j++) {
            ItemContainer container = ((InventoryContainer)bags.get(j)).getInventory();
            InventoryItem item = container.getItemFromTag(itemTag, chr, notEquipped, ignoreBroken, includeInv);
            if (item != null) {
                TL_itemListPool.get().release(bags);
                return item;
            }
        }

        TL_itemListPool.get().release(bags);
        return null;
    }

    public InventoryItem getItemFromType(String type, boolean ignoreBroken, boolean includeInv) {
        return this.getItemFromType(type, null, false, ignoreBroken, includeInv);
    }

    public InventoryItem getItemFromTag(ItemTag itemTag, boolean ignoreBroken, boolean includeInv) {
        return this.getItemFromTag(itemTag, null, false, ignoreBroken, includeInv);
    }

    public InventoryItem getItemFromType(String type) {
        return this.getFirstType(type);
    }

    public ArrayList<InventoryItem> getItemsFromType(String type) {
        return this.getAllType(type);
    }

    public ArrayList<InventoryItem> getItemsFromFullType(String type) {
        return type != null && type.contains(".") ? this.getAllType(type) : new ArrayList<>();
    }

    public ArrayList<InventoryItem> getItemsFromFullType(String type, boolean includeInv) {
        if (type != null && type.contains(".")) {
            return includeInv ? this.getAllTypeRecurse(type) : this.getAllType(type);
        } else {
            return new ArrayList<>();
        }
    }

    public ArrayList<InventoryItem> getItemsFromType(String type, boolean includeInv) {
        return includeInv ? this.getAllTypeRecurse(type) : this.getAllType(type);
    }

    public ArrayList<InventoryItem> getItemsFromCategory(String category) {
        return this.getAllCategory(category);
    }

    public void requestSync() {
        if (GameClient.client && (this.parent == null || this.parent.square == null || this.parent.square.chunk == null)) {
            ;
        }
    }

    public void requestServerItemsForContainer() {
        if (this.parent != null && this.parent.square != null) {
            INetworkPacket.send(PacketTypes.PacketType.RequestItemsForContainer, this);
        }
    }

    public InventoryItem getItemWithIDRecursiv(int id) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.id == id) {
                return item;
            }

            if (item instanceof InventoryContainer container && container.getItemContainer() != null && !container.getItemContainer().getItems().isEmpty()) {
                item = container.getItemContainer().getItemWithIDRecursiv(id);
                if (item != null) {
                    return item;
                }
            }
        }

        return null;
    }

    public InventoryItem getItemWithID(int id) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem Item = this.items.get(i);
            if (Item.id == id) {
                return Item;
            }
        }

        return null;
    }

    public boolean removeItemWithID(int id) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.id == id) {
                this.Remove(item);
                return true;
            }
        }

        return false;
    }

    public boolean containsID(int id) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.id == id) {
                return true;
            }
        }

        return false;
    }

    public boolean removeItemWithIDRecurse(int id) {
        for (int i = 0; i < this.items.size(); i++) {
            InventoryItem item = this.items.get(i);
            if (item.id == id) {
                this.Remove(item);
                return true;
            }

            if (item instanceof InventoryContainer container && container.getInventory().removeItemWithIDRecurse(id)) {
                return true;
            }
        }

        return false;
    }

    public boolean isHasBeenLooted() {
        return this.hasBeenLooted;
    }

    public void setHasBeenLooted(boolean hasBeenLooted) {
        this.hasBeenLooted = hasBeenLooted;
    }

    public String getOpenSound() {
        if (this.openSound == null) {
            IsoObject parent1 = this.getParent();
            return parent1 != null && parent1.getSprite() != null ? parent1.getProperties().get("ContainerOpenSound") : null;
        } else {
            return this.openSound;
        }
    }

    public void setOpenSound(String openSound) {
        this.openSound = openSound;
    }

    public String getCloseSound() {
        if (this.closeSound == null) {
            IsoObject parent1 = this.getParent();
            return parent1 != null && parent1.getSprite() != null ? parent1.getProperties().get("ContainerCloseSound") : null;
        } else {
            return this.closeSound;
        }
    }

    public void setCloseSound(String closeSound) {
        this.closeSound = closeSound;
    }

    public String getPutSound() {
        if (this.putSound == null) {
            IsoObject parent1 = this.getParent();
            return parent1 != null && parent1.getSprite() != null ? parent1.getProperties().get("ContainerPutSound") : null;
        } else {
            return this.putSound;
        }
    }

    public void setPutSound(String putSound) {
        this.putSound = putSound;
    }

    public String getTakeSound() {
        if (this.takeSound == null) {
            IsoObject parent1 = this.getParent();
            return parent1 != null && parent1.getSprite() != null ? parent1.getProperties().get("ContainerTakeSound") : null;
        } else {
            return this.takeSound;
        }
    }

    public void setTakeSound(String takeSound) {
        this.takeSound = takeSound;
    }

    public InventoryItem haveThisKeyId(int keyId) {
        for (int i = 0; i < this.getItems().size(); i++) {
            InventoryItem item = this.getItems().get(i);
            if (item instanceof Key key) {
                if (key.getKeyId() == keyId) {
                    return key;
                }
            } else if ((item.getType().equals("KeyRing") || item.hasTag(ItemTag.KEY_RING))
                && ((InventoryContainer)item).getInventory().haveThisKeyId(keyId) != null) {
                return ((InventoryContainer)item).getInventory().haveThisKeyId(keyId);
            }
        }

        return null;
    }

    public String getOnlyAcceptCategory() {
        return this.onlyAcceptCategory;
    }

    public void setOnlyAcceptCategory(String onlyAcceptCategory) {
        this.onlyAcceptCategory = StringUtils.discardNullOrWhitespace(onlyAcceptCategory);
    }

    public String getAcceptItemFunction() {
        return this.acceptItemFunction;
    }

    public void setAcceptItemFunction(String functionName) {
        this.acceptItemFunction = StringUtils.discardNullOrWhitespace(functionName);
    }

    @Override
    public String toString() {
        return "ItemContainer:[type:" + this.getType() + ", parent:" + this.getParent() + "]";
    }

    public IsoGameCharacter getCharacter() {
        if (this.getParent() instanceof IsoGameCharacter) {
            return (IsoGameCharacter)this.getParent();
        } else {
            return this.containingItem != null && this.containingItem.getContainer() != null ? this.containingItem.getContainer().getCharacter() : null;
        }
    }

    public void emptyIt() {
        this.items = new ArrayList<>();
    }

    public LinkedHashMap<String, InventoryItem> getItems4Admin() {
        LinkedHashMap<String, InventoryItem> items = new LinkedHashMap<>();

        for (int i = 0; i < this.getItems().size(); i++) {
            InventoryItem item = this.getItems().get(i);
            item.setCount(1);
            if (!item.isItemType(ItemType.DRAINABLE)
                && !item.isItemType(ItemType.WEAPON)
                && items.get(item.getFullType()) != null
                && !(item instanceof InventoryContainer)) {
                items.get(item.getFullType()).setCount(items.get(item.getFullType()).getCount() + 1);
            } else if (items.get(item.getFullType()) != null) {
                items.put(item.getFullType() + Rand.Next(100000), item);
            } else {
                items.put(item.getFullType(), item);
            }
        }

        return items;
    }

    public ArrayList<InventoryItem> getAllFoodsForAnimals() {
        ArrayList<InventoryItem> result = new ArrayList<>();

        for (int i = 0; i < this.getItems().size(); i++) {
            InventoryItem item = this.getItems().get(i);
            if (item.isAnimalFeed()
                || item instanceof Food food
                    && (
                        food.getFoodType() != null && (food.getFoodType().equals("Fruits") || food.getFoodType().equals("Vegetables"))
                            || food.getMilkType() != null
                    )
                    && food.getHungerChange() < 0.0F
                    && !food.isRotten()
                    && !item.isSpice()) {
                result.add(item);
            }
        }

        return result;
    }

    public LinkedHashMap<String, InventoryItem> getAllItems(LinkedHashMap<String, InventoryItem> items, boolean inInv) {
        if (items == null) {
            items = new LinkedHashMap<>();
        }

        for (int i = 0; i < this.getItems().size(); i++) {
            InventoryItem item = this.getItems().get(i);
            if (inInv) {
                item.setWorker("inInv");
            }

            item.setCount(1);
            if (!item.isItemType(ItemType.DRAINABLE) && !item.isItemType(ItemType.WEAPON) && items.get(item.getFullType()) != null) {
                items.get(item.getFullType()).setCount(items.get(item.getFullType()).getCount() + 1);
            } else if (items.get(item.getFullType()) != null) {
                items.put(item.getFullType() + Rand.Next(100000), item);
            } else {
                items.put(item.getFullType(), item);
            }

            if (item instanceof InventoryContainer container && container.getItemContainer() != null && !container.getItemContainer().getItems().isEmpty()) {
                items = container.getItemContainer().getAllItems(items, true);
            }
        }

        return items;
    }

    @Deprecated
    public InventoryItem getItemById(long id) {
        for (int i = 0; i < this.getItems().size(); i++) {
            InventoryItem item = this.getItems().get(i);
            if (item.getID() == id) {
                return item;
            }

            if (item instanceof InventoryContainer container && container.getItemContainer() != null && !container.getItemContainer().getItems().isEmpty()) {
                item = container.getItemContainer().getItemById(id);
                if (item != null) {
                    return item;
                }
            }
        }

        return null;
    }

    public void addItemsToProcessItems() {
        IsoWorld.instance.currentCell.addToProcessItems(this.items);
    }

    public void removeItemsFromProcessItems() {
        IsoWorld.instance.currentCell.addToProcessItemsRemove(this.items);
        if (!"floor".equals(this.type)) {
            ItemSoundManager.removeItems(this.items);
        }
    }

    public boolean isExistYet() {
        if (!SystemDisabler.doWorldSyncEnable) {
            return true;
        } else if (this.getCharacter() != null) {
            return true;
        } else if (this.getParent() instanceof BaseVehicle) {
            return true;
        } else if (this.parent instanceof IsoDeadBody) {
            return this.parent.getStaticMovingObjectIndex() != -1;
        } else if (this.parent instanceof IsoCompost) {
            return this.parent.getObjectIndex() != -1;
        } else if (this.containingItem != null && this.containingItem.worldItem != null) {
            return this.containingItem.worldItem.getWorldObjectIndex() != -1;
        } else if (this.getType().equals("floor")) {
            return true;
        } else if (this.sourceGrid == null) {
            return false;
        } else {
            IsoGridSquare sq = this.sourceGrid;
            return !sq.getObjects().contains(this.parent) ? false : this.parent.getContainerIndex(this) != -1;
        }
    }

    public String getContainerPosition() {
        return this.containerPosition;
    }

    public void setContainerPosition(String containerPosition) {
        this.containerPosition = containerPosition;
    }

    public String getFreezerPosition() {
        return this.freezerPosition;
    }

    public void setFreezerPosition(String freezerPosition) {
        this.freezerPosition = freezerPosition;
    }

    public VehiclePart getVehiclePart() {
        return this.vehiclePart;
    }

    public BaseVehicle getVehicle() {
        if (!this.isVehiclePart()) {
            return null;
        } else {
            VehiclePart part = this.getVehiclePart();
            return part.getVehicle();
        }
    }

    public VehiclePart getVehicleDoorPart() {
        if (this.isVehicleSeat()) {
            return this.getVehicleSeatDoorPart();
        } else {
            VehiclePart part = this.getVehiclePart();
            if (part == null) {
                return null;
            } else {
                if (part.isVehicleTrunk()) {
                    BaseVehicle vehicle = this.getVehicle();
                    if (vehicle == null) {
                        return null;
                    }

                    VehiclePart trunkDoorPart = vehicle.getTrunkDoorPart();
                    if (trunkDoorPart != null) {
                        VehicleDoor trunkDoor = trunkDoorPart.getDoor();
                        if (trunkDoor != null) {
                            return trunkDoorPart;
                        }
                    }
                }

                VehicleDoor door = part.getDoor();
                return door == null ? null : part;
            }
        }
    }

    public VehiclePart getVehicleSeatDoorPart() {
        if (!this.isVehicleSeat()) {
            return null;
        } else {
            VehiclePart part = this.getVehiclePart();
            int seatNum = part.getContainerSeatNumber();
            if (seatNum < 0) {
                return null;
            } else {
                BaseVehicle vehicle = this.getVehicle();
                return vehicle == null ? null : vehicle.getPassengerDoor(seatNum);
            }
        }
    }

    public VehicleDoor getVehicleSeatDoor() {
        VehiclePart seatDoorPart = this.getVehicleSeatDoorPart();
        return seatDoorPart == null ? null : seatDoorPart.getDoor();
    }

    public VehicleDoor getVehicleDoor() {
        if (this.isVehicleSeat()) {
            return this.getVehicleSeatDoor();
        } else {
            VehiclePart doorPart = this.getVehicleDoorPart();
            return doorPart == null ? null : doorPart.getDoor();
        }
    }

    public boolean doesVehicleDoorNeedOpening() {
        VehiclePart doorPart = this.getVehicleDoorPart();
        if (doorPart == null) {
            return false;
        } else {
            VehicleDoor door = this.getVehicleDoor();
            if (door == null) {
                return false;
            } else {
                boolean isOpen = door.isOpen();
                return !isOpen;
            }
        }
    }

    public boolean canCharacterOpenVehicleDoor(IsoGameCharacter playerObj) {
        VehiclePart doorPart = this.getVehicleDoorPart();
        if (doorPart == null) {
            return false;
        } else {
            VehicleDoor door = this.getVehicleDoor();
            if (door == null) {
                return false;
            } else {
                BaseVehicle vehicle = this.getVehicle();
                return vehicle == null ? false : vehicle.canOpenDoor(doorPart, playerObj);
            }
        }
    }

    public boolean canCharacterUnlockVehicleDoor(IsoGameCharacter playerObj) {
        VehiclePart doorPart = this.getVehicleDoorPart();
        if (doorPart == null) {
            return false;
        } else {
            VehicleDoor door = this.getVehicleDoor();
            if (door == null) {
                return false;
            } else {
                BaseVehicle vehicle = this.getVehicle();
                return vehicle == null ? false : vehicle.canUnlockDoor(doorPart, playerObj);
            }
        }
    }

    public void reset() {
        for (int i = 0; i < this.getItems().size(); i++) {
            this.getItems().get(i).reset();
        }
    }

    public ItemContainer getOutermostContainer() {
        InventoryItem containingItem = this.getContainingItem();
        if (containingItem == null) {
            return this;
        } else {
            ItemContainer outer = containingItem.getContainer();
            return outer == null ? this : outer.getOutermostContainer();
        }
    }

    public IsoGridSquare getSquare() {
        ItemContainer outerContainer = this.getOutermostContainer();
        if (outerContainer != null && outerContainer != this) {
            return outerContainer.getSquare();
        } else {
            VehiclePart vehiclePart = this.getVehiclePart();
            if (vehiclePart != null) {
                BaseVehicle vehicle = vehiclePart.getVehicle();
                if (vehicle != null) {
                    IsoGridSquare vehicleSquare = vehicle.getSquare();
                    if (vehicleSquare != null) {
                        return vehicleSquare;
                    }
                }
            }

            IsoGridSquare gridSquare = this.getSourceGrid();
            if (gridSquare != null) {
                return gridSquare;
            } else {
                IsoObject parentObject = this.getParent();
                if (parentObject != null) {
                    IsoGridSquare parentSquare = parentObject.getSquare();
                    if (parentSquare != null) {
                        return parentSquare;
                    }
                }

                InventoryItem containingItem = this.getContainingItem();
                if (containingItem != null) {
                    IsoWorldInventoryObject worldItem = containingItem.getWorldItem();
                    if (worldItem != null) {
                        IsoGridSquare worldItemSquare = worldItem.getSquare();
                        if (worldItemSquare != null) {
                            return worldItemSquare;
                        }
                    }
                }

                return null;
            }
        }
    }

    public Vector2 getWorldPosition(Vector2 out_result) {
        ItemContainer outermostContainer = this.getOutermostContainer();
        if (outermostContainer != null && outermostContainer != this) {
            return outermostContainer.getWorldPosition(out_result);
        } else {
            BaseVehicle vehicle = this.getVehicle();
            VehiclePart vehiclePart = this.getVehiclePart();
            if (vehicle != null && vehiclePart != null) {
                String vehiclePartArea = vehiclePart.getArea();
                Vector2 partPos = vehicle.getAreaFacingPosition(vehiclePartArea, out_result);
                if (partPos != null) {
                    return partPos;
                }
            }

            IsoGridSquare gridSquare = this.getSquare();
            out_result.set(gridSquare.getX(), gridSquare.getY());
            return out_result;
        }
    }

    public boolean isStove() {
        return "stove".equals(this.type) || "toaster".equals(this.type) || "coffeemaker".equals(this.type);
    }

    public boolean isShop() {
        if (this.getSquare() == null) {
            return false;
        } else if (this.getSquare().isShop()) {
            return true;
        } else if (this.getParent() != null && this.getParent().getSquare() != null && this.getParent().getSquare().isShop()) {
            return true;
        } else if (this.getSquare().getRoom() == null) {
            return false;
        } else if (this.getSquare().getRoom().isShop()) {
            return true;
        } else {
            String roomName = this.getSquare().getRoom().getName();
            if (ItemPickerJava.rooms.containsKey(roomName)) {
                ItemPickerJava.ItemPickerRoom roomDist = ItemPickerJava.rooms.get(roomName);
                if (roomDist != null && roomDist.isShop) {
                    return true;
                }

                ItemPickerJava.ItemPickerContainer containerDist = null;
                if (roomDist.containers.containsKey(this.getType())) {
                    containerDist = roomDist.containers.get(this.getType());
                }

                if (containerDist == null && roomDist.containers.containsKey("other")) {
                    containerDist = roomDist.containers.get("other");
                }

                if (containerDist == null && roomDist.containers.containsKey("all")) {
                    containerDist = roomDist.containers.get("all");
                }

                if (containerDist != null && containerDist.isShop) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isCorpse() {
        return this.getType().equals("inventorymale") || this.getType().equals("inventoryfemale");
    }

    public boolean hasRecipe(String recipe, IsoGameCharacter chr) {
        return this.hasRecipe(recipe, chr, true);
    }

    public boolean hasRecipe(String recipe, IsoGameCharacter chr, boolean recursive) {
        if (chr instanceof IsoPlayer isoPlayer && isoPlayer.tooDarkToRead()) {
            return false;
        } else {
            boolean illiterate = chr != null && chr.hasTrait(CharacterTrait.ILLITERATE);

            for (int i = 0; i < this.getItems().size(); i++) {
                InventoryItem item = this.getItems().get(i);
                boolean canRead = !illiterate || item.hasTag(ItemTag.PICTUREBOOK);
                if (canRead && item instanceof Literature literature && literature.hasRecipe(recipe)) {
                    return true;
                }

                if (recursive && item instanceof InventoryContainer cont && cont.getInventory().hasRecipe(recipe, chr, true)) {
                    return true;
                }
            }

            return false;
        }
    }

    public InventoryItem getRecipeItem(String recipe, IsoGameCharacter chr, boolean recursive) {
        if (chr instanceof IsoPlayer isoPlayer && isoPlayer.tooDarkToRead()) {
            return null;
        } else {
            boolean illiterate = chr != null && chr.hasTrait(CharacterTrait.ILLITERATE);

            for (int i = 0; i < this.getItems().size(); i++) {
                InventoryItem item = this.getItems().get(i);
                boolean canRead = !illiterate || item.hasTag(ItemTag.PICTUREBOOK);
                if (canRead && item instanceof Literature literature && literature.hasRecipe(recipe)) {
                    return item;
                }

                if (recursive && item instanceof InventoryContainer cont && cont.getInventory().hasRecipe(recipe, chr, recursive)) {
                    return cont.getInventory().getRecipeItem(recipe, chr, recursive);
                }
            }

            return null;
        }
    }

    public void dumpContentsInSquare(IsoGridSquare sq) {
        for (int i = 0; i < this.getItems().size(); i++) {
            InventoryItem item = this.getItems().get(i);
            this.getItems().remove(item);
            sq.AddWorldInventoryItem(item, 0.0F, 0.0F, 0.0F);
        }
    }

    public String getCustomName() {
        String key = this.getType() + "_customContainerName";
        return this.getParent().getModData().rawget(key) != null ? (String)this.getParent().getModData().rawget(key) : null;
    }

    public void setCustomName(String name) {
        String key = this.getType() + "_customContainerName";
        this.getParent().getModData().rawset(key, String.valueOf(name));
    }

    private static final class CategoryPredicate implements Predicate<InventoryItem> {
        private String category;

        private ItemContainer.CategoryPredicate init(String type) {
            this.category = Objects.requireNonNull(type);
            return this;
        }

        public boolean test(InventoryItem item) {
            return item.getCategory().equals(this.category);
        }
    }

    private static final class Comparators {
        ObjectPool<ItemContainer.ConditionComparator> condition = new ObjectPool<>(ItemContainer.ConditionComparator::new);
        ObjectPool<ItemContainer.EvalComparator> eval = new ObjectPool<>(ItemContainer.EvalComparator::new);
        ObjectPool<ItemContainer.EvalArgComparator> evalArg = new ObjectPool<>(ItemContainer.EvalArgComparator::new);
    }

    private static final class ConditionComparator implements Comparator<InventoryItem> {
        public int compare(InventoryItem o1, InventoryItem o2) {
            return o1.getCondition() - o2.getCondition();
        }
    }

    private static final class EvalArgComparator implements Comparator<InventoryItem> {
        private LuaClosure functionObj;
        private Object arg;

        ItemContainer.EvalArgComparator init(LuaClosure functionObj, Object arg) {
            this.functionObj = Objects.requireNonNull(functionObj);
            this.arg = arg;
            return this;
        }

        public int compare(InventoryItem o1, InventoryItem o2) {
            LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, this.functionObj, o1, o2, this.arg);
            if (result.isSuccess() && !result.isEmpty() && result.getFirst() instanceof Double) {
                double v = (Double)result.getFirst();
                return Double.compare(v, 0.0);
            } else {
                return 0;
            }
        }
    }

    private static final class EvalArgPredicate implements Predicate<InventoryItem> {
        private LuaClosure functionObj;
        private Object arg;

        private ItemContainer.EvalArgPredicate init(LuaClosure functionObj, Object arg) {
            this.functionObj = Objects.requireNonNull(functionObj);
            this.arg = arg;
            return this;
        }

        public boolean test(InventoryItem item) {
            return LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.functionObj, item, this.arg) == Boolean.TRUE;
        }
    }

    private static final class EvalComparator implements Comparator<InventoryItem> {
        private LuaClosure functionObj;

        private ItemContainer.EvalComparator init(LuaClosure functionObj) {
            this.functionObj = Objects.requireNonNull(functionObj);
            return this;
        }

        public int compare(InventoryItem o1, InventoryItem o2) {
            LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, this.functionObj, o1, o2);
            if (result.isSuccess() && !result.isEmpty() && result.getFirst() instanceof Double) {
                double v = (Double)result.getFirst();
                return Double.compare(v, 0.0);
            } else {
                return 0;
            }
        }
    }

    private static final class EvalPredicate implements Predicate<InventoryItem> {
        private LuaClosure functionObj;

        private ItemContainer.EvalPredicate init(LuaClosure functionObj) {
            this.functionObj = Objects.requireNonNull(functionObj);
            return this;
        }

        public boolean test(InventoryItem item) {
            return LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.functionObj, item) == Boolean.TRUE;
        }
    }

    private static final class InventoryItemList extends ArrayList<InventoryItem> {
        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    private static final class InventoryItemListPool extends ObjectPool<ItemContainer.InventoryItemList> {
        public InventoryItemListPool() {
            super(ItemContainer.InventoryItemList::new);
        }

        public void release(ItemContainer.InventoryItemList obj) {
            obj.clear();
            super.release(obj);
        }
    }

    private static final class Predicates {
        private final ObjectPool<ItemContainer.CategoryPredicate> category = new ObjectPool<>(ItemContainer.CategoryPredicate::new);
        private final ObjectPool<ItemContainer.EvalPredicate> eval = new ObjectPool<>(ItemContainer.EvalPredicate::new);
        private final ObjectPool<ItemContainer.EvalArgPredicate> evalArg = new ObjectPool<>(ItemContainer.EvalArgPredicate::new);
        private final ObjectPool<ItemContainer.TagPredicate> tag = new ObjectPool<>(ItemContainer.TagPredicate::new);
        private final ObjectPool<ItemContainer.TagEvalPredicate> tagEval = new ObjectPool<>(ItemContainer.TagEvalPredicate::new);
        private final ObjectPool<ItemContainer.TagEvalArgPredicate> tagEvalArg = new ObjectPool<>(ItemContainer.TagEvalArgPredicate::new);
        private final ObjectPool<ItemContainer.TypePredicate> type = new ObjectPool<>(ItemContainer.TypePredicate::new);
        private final ObjectPool<ItemContainer.TypeEvalPredicate> typeEval = new ObjectPool<>(ItemContainer.TypeEvalPredicate::new);
        private final ObjectPool<ItemContainer.TypeEvalArgPredicate> typeEvalArg = new ObjectPool<>(ItemContainer.TypeEvalArgPredicate::new);
    }

    private static final class TagEvalArgPredicate implements Predicate<InventoryItem> {
        private ItemTag itemTag;
        private LuaClosure functionObj;
        private Object arg;

        private ItemContainer.TagEvalArgPredicate init(ItemTag itemTag, LuaClosure functionObj, Object arg) {
            this.itemTag = itemTag;
            this.functionObj = Objects.requireNonNull(functionObj);
            this.arg = arg;
            return this;
        }

        public boolean test(InventoryItem item) {
            return item.hasTag(this.itemTag) && LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.functionObj, item, this.arg) == Boolean.TRUE;
        }
    }

    private static final class TagEvalPredicate implements Predicate<InventoryItem> {
        private ItemTag itemTag;
        private LuaClosure functionObj;

        private ItemContainer.TagEvalPredicate init(ItemTag itemTag, LuaClosure functionObj) {
            this.itemTag = itemTag;
            this.functionObj = Objects.requireNonNull(functionObj);
            return this;
        }

        public boolean test(InventoryItem item) {
            return item.hasTag(this.itemTag) && LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.functionObj, item) == Boolean.TRUE;
        }
    }

    private static final class TagPredicate implements Predicate<InventoryItem> {
        private ItemTag itemTag;

        private ItemContainer.TagPredicate init(ItemTag itemTag) {
            this.itemTag = Objects.requireNonNull(itemTag);
            return this;
        }

        public boolean test(InventoryItem item) {
            return item.hasTag(this.itemTag);
        }
    }

    private static final class TypeEvalArgPredicate implements Predicate<InventoryItem> {
        private String type;
        private LuaClosure functionObj;
        private Object arg;

        private ItemContainer.TypeEvalArgPredicate init(String type, LuaClosure functionObj, Object arg) {
            this.type = type;
            this.functionObj = Objects.requireNonNull(functionObj);
            this.arg = arg;
            return this;
        }

        public boolean test(InventoryItem item) {
            return ItemContainer.compareType(this.type, item)
                && LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.functionObj, item, this.arg) == Boolean.TRUE;
        }
    }

    private static final class TypeEvalPredicate implements Predicate<InventoryItem> {
        private String type;
        private LuaClosure functionObj;

        private ItemContainer.TypeEvalPredicate init(String type, LuaClosure functionObj) {
            this.type = type;
            this.functionObj = Objects.requireNonNull(functionObj);
            return this;
        }

        public boolean test(InventoryItem item) {
            return ItemContainer.compareType(this.type, item)
                && LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.functionObj, item) == Boolean.TRUE;
        }
    }

    private static final class TypePredicate implements Predicate<InventoryItem> {
        private String type;

        private ItemContainer.TypePredicate init(String type) {
            this.type = Objects.requireNonNull(type);
            return this;
        }

        public boolean test(InventoryItem item) {
            return ItemContainer.compareType(this.type, item);
        }
    }
}
