// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.DrainableComboItem;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.radio.ZomboidRadio;
import zombie.scripting.objects.VehicleScript;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.lambda.Invokers;
import zombie.util.list.PZArrayList;

public final class VehicleParts {
    private VehiclePartOwner owner;
    private final List<VehiclePart> parts = new ArrayList<>();
    private final Map<String, VehiclePart> partsById = new HashMap<>();
    private VehiclePart battery;
    private VehiclePart engine;

    public void setOwner(VehiclePartOwner owner) {
        VehiclePartOwner ownerOld = this.owner;
        this.owner = owner;
        IsoObject ownerObject = Type.tryCastTo(owner, IsoObject.class);

        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            part.vehicle = owner;
            ItemContainer var7 = part.getItemContainer();
            if (var7 instanceof ItemContainer) {
                var7.setParent(ownerObject);
            }

            if (part.engine != null) {
                part.engine.replaceListener(ownerOld, owner);
            }
        }
    }

    public VehiclePartOwner getOwner() {
        return this.owner;
    }

    public BaseVehicle getVehicle() {
        return Type.tryCastTo(this.getOwner(), BaseVehicle.class);
    }

    public VehicleScript getScript() {
        return this.getOwner().getScript();
    }

    public void clear() {
        this.parts.clear();
        this.partsById.clear();
        this.battery = null;
        this.engine = null;
    }

    public int size() {
        return this.getPartCount();
    }

    public boolean isEmpty() {
        return this.getPartCount() == 0;
    }

    public boolean contains(VehiclePart part) {
        return this.parts.contains(part);
    }

    public int indexOf(VehiclePart part) {
        return this.parts.indexOf(part);
    }

    public void add(VehiclePart part) {
        this.parts.add(part);
        if (!StringUtils.isNullOrWhitespace(part.getId()) && !this.partsById.containsKey(part.getId())) {
            this.partsById.put(part.getId(), part);
        }

        if (zombie.scripting.objects.VehiclePart.BATTERY.toString().equals(part.getId())) {
            this.battery = part;
        }

        if (zombie.scripting.objects.VehiclePart.ENGINE.toString().equals(part.getId())) {
            this.engine = part;
        }
    }

    public VehiclePart get(int index) {
        return this.getPartByIndex(index);
    }

    public int getPartCount() {
        return this.parts.size();
    }

    public VehiclePart getPartByIndex(int index) {
        return index >= 0 && index < this.parts.size() ? this.parts.get(index) : null;
    }

    public VehiclePart getPartByPartId(zombie.scripting.objects.VehiclePart id) {
        return id == null ? null : this.getPartById(id.toString());
    }

    public VehiclePart getPartById(String id) {
        return id == null ? null : this.partsById.getOrDefault(id, null);
    }

    public int getPartIndex(String id) {
        if (id == null) {
            return -1;
        } else {
            for (int i = 0; i < this.parts.size(); i++) {
                VehicleScript.Part scriptPart = this.parts.get(i).getScriptPart();
                if (scriptPart != null && id.equals(scriptPart.id)) {
                    return i;
                }
            }

            return -1;
        }
    }

    public int getNumberOfPartsWithContainers() {
        if (this.getScript() == null) {
            return 0;
        } else {
            int count = 0;

            for (int i = 0; i < this.getScript().getPartCount(); i++) {
                if (this.getScript().getPart(i).container != null) {
                    count++;
                }
            }

            return count;
        }
    }

    public VehiclePart getBattery() {
        return this.battery;
    }

    public float getBatteryCharge() {
        VehiclePart battery = this.getBattery();
        return battery != null && battery.getInventoryItem() instanceof DrainableComboItem
            ? battery.<InventoryItem>getInventoryItem().getCurrentUsesFloat()
            : 0.0F;
    }

    public VehiclePart getEngine() {
        return this.engine;
    }

    public int getEngineCondition() {
        return this.getEngine() == null ? 0 : this.getEngine().getCondition();
    }

    public boolean isEngineWorking() {
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            String functionName = part.getLuaFunction("checkEngine");
            if (functionName != null && !Boolean.TRUE.equals(this.callLuaBoolean(functionName, this, part))) {
                return false;
            }
        }

        return true;
    }

    public VehiclePart getTrunkDoorPart() {
        return this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRUNK_DOOR);
    }

    public VehiclePart getTrunkPart() {
        VehiclePart truckBedPart = this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRUCK_BED);
        return truckBedPart != null ? truckBedPart : this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRUCK_BED_OPEN);
    }

    public VehiclePart getTrailerTrunkPart() {
        return this.getPartByPartId(zombie.scripting.objects.VehiclePart.TRAILER_TRUNK);
    }

    public VehiclePart getPartForSeatContainer(int seat) {
        if (this.getScript() != null && seat >= 0 && seat < this.getScript().getPassengerCount()) {
            for (int i = 0; i < this.getPartCount(); i++) {
                VehiclePart part = this.getPartByIndex(i);
                if (part.getContainerSeatNumber() == seat) {
                    return part;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public <T> PZArrayList<ItemContainer> getVehicleItemContainers(T paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> isValidPredicate) {
        PZArrayList<ItemContainer> containerList = new PZArrayList<>(ItemContainer.class, 10);
        return this.getVehicleItemContainers(paramToCompare, isValidPredicate, containerList);
    }

    public <T> PZArrayList<ItemContainer> getVehicleItemContainers(
        T paramToCompare, Invokers.Params2.Boolean.ICallback<T, ItemContainer> isValidPredicate, PZArrayList<ItemContainer> containerList
    ) {
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            ItemContainer partContainer = part.getItemContainer();
            if (partContainer != null) {
                boolean canStore = isValidPredicate.accept(paramToCompare, partContainer);
                if (canStore) {
                    containerList.addUniqueReference(partContainer);
                }
            }
        }

        return containerList;
    }

    public void createParts() {
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            if (this.isPartMissingUninstallableItem(part)) {
                part.created = false;
            }

            if (this.shouldPartHaveNoItem(part)) {
                part.item = null;
            }

            if (!part.created) {
                part.created = true;
                String functionName = part.getLuaFunction("create");
                if (functionName == null) {
                    part.setRandomCondition(null);
                } else {
                    this.callLuaVoid(functionName, this.getOwner(), part);
                    if (part.getCondition() == -1) {
                        part.setRandomCondition(null);
                    }
                }
            }
        }
    }

    private boolean isPartMissingUninstallableItem(VehiclePart part) {
        List<String> itemType = part.getItemType();
        return part.created && itemType != null && !itemType.isEmpty() && part.getInventoryItem() == null && part.getTable("install") == null;
    }

    private boolean shouldPartHaveNoItem(VehiclePart part) {
        List<String> itemType = part.getItemType();
        return itemType == null || itemType.isEmpty();
    }

    public void initParts() {
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            String functionName = part.getLuaFunction("init");
            if (functionName != null) {
                this.callLuaVoid(functionName, this.getOwner(), part);
            }
        }
    }

    public void setScript(VehicleScript script) {
        if (script == null) {
            this.clear();
        } else {
            ArrayList<VehiclePart> oldParts = new ArrayList<>(this.parts);
            this.clear();

            for (int i = 0; i < script.getPartCount(); i++) {
                VehicleScript.Part scriptPart = script.getPart(i);
                VehiclePart part = null;

                for (int j = 0; j < oldParts.size(); j++) {
                    VehiclePart oldPart = oldParts.get(j);
                    if (oldPart.getScriptPart() != null && scriptPart.id.equals(oldPart.getScriptPart().id)) {
                        part = oldPart;
                        break;
                    }

                    if (oldPart.partId != null && scriptPart.id.equals(oldPart.partId)) {
                        part = oldPart;
                        break;
                    }
                }

                if (part == null) {
                    part = new VehiclePart(this.getOwner());
                }

                part.setScriptPart(scriptPart);
                part.category = scriptPart.category;
                part.specificItem = scriptPart.specificItem;
                part.setDurability(scriptPart.getDurability());
                if (scriptPart.container != null && scriptPart.container.contentType == null) {
                    if (part.getItemContainer() == null) {
                        ItemContainer container = new ItemContainer(scriptPart.id, null, this.getVehicle());
                        part.setItemContainer(container);
                        container.id = 0;
                    }

                    part.getItemContainer().capacity = scriptPart.container.capacity;
                } else {
                    part.setItemContainer(null);
                }

                if (scriptPart.door == null) {
                    part.door = null;
                } else if (part.door == null) {
                    part.door = new VehicleDoor(part);
                    part.door.init(scriptPart.door);
                }

                if (zombie.scripting.objects.VehiclePart.ENGINE.toString().equals(part.getId())) {
                    if (part.engine == null) {
                        part.engine = new VehicleEngine(part);
                        part.engine.addListener(this.getOwner());
                    }
                } else {
                    part.engine = null;
                }

                if (scriptPart.window == null) {
                    part.window = null;
                } else if (part.window == null) {
                    part.window = new VehicleWindow(part);
                    part.window.init(scriptPart.window);
                } else {
                    part.window.openable = scriptPart.window.openable;
                }

                part.parent = null;
                if (part.children != null) {
                    part.children.clear();
                }

                this.add(part);
            }

            for (int i = 0; i < script.getPartCount(); i++) {
                VehiclePart part = this.parts.get(i);
                VehicleScript.Part scriptPart = part.getScriptPart();
                if (scriptPart.parent != null) {
                    part.parent = this.getPartById(scriptPart.parent);
                    if (part.parent != null) {
                        part.parent.addChild(part);
                    }
                }
            }

            for (int ix = 0; ix < script.getPartCount(); ix++) {
                VehiclePart part = this.parts.get(ix);
                part.setInventoryItem(part.item);
            }
        }
    }

    public boolean updatePart(VehiclePart part) {
        part.updateSignalDevice();
        VehicleLight light = part.getLight();
        if (light != null && part.getId().contains("Headlight")) {
            part.setLightActive(this.getOwner().getHeadlightsOn() && part.getInventoryItem() != null && this.getBatteryCharge() > 0.0F);
        }

        String functionName = part.getLuaFunction("update");
        if (functionName == null) {
            return false;
        } else {
            float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();
            part.setLastUpdated(GameTime.checkHours(part.getLastUpdated(), worldAgeHours));
            float elapsedHours = worldAgeHours - part.getLastUpdated();
            if ((int)(elapsedHours * 60.0F) > 0) {
                part.setLastUpdated(worldAgeHours);
                this.callLuaVoid(functionName, this.getOwner(), part, (double)(elapsedHours * 60.0F));
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean update() {
        if (!GameClient.client) {
            boolean didUpdate = false;

            for (int i = 0; i < this.getPartCount(); i++) {
                VehiclePart part = this.getPartByIndex(i);
                if (this.updatePart(part) && !didUpdate) {
                    didUpdate = true;
                }
            }

            return didUpdate;
        } else {
            for (int ix = 0; ix < this.getPartCount(); ix++) {
                VehiclePart part = this.getPartByIndex(ix);
                part.updateSignalDevice();
            }

            return false;
        }
    }

    public void addToWorld() {
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            if (part.getItemContainer() != null) {
                part.getItemContainer().addItemsToProcessItems();
            }

            if (part.getDeviceData() != null) {
                ZomboidRadio.getInstance().RegisterDevice(part);
            }
        }
    }

    public void removeFromWorld() {
        for (int i = 0; i < this.parts.size(); i++) {
            VehiclePart part = this.parts.get(i);
            if (part.getItemContainer() != null) {
                part.getItemContainer().removeItemsFromProcessItems();
            }

            if (part.getDeviceData() != null) {
                part.getDeviceData().cleanSoundsAndEmitter();
                ZomboidRadio.getInstance().UnRegisterDevice(part);
            }
        }
    }

    private void callLuaVoid(String functionName, Object arg1, Object arg2) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, arg1, arg2);
        }
    }

    private void callLuaVoid(String functionName, Object arg1) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, arg1);
        }
    }

    private void callLuaVoid(String functionName, Object arg1, Object arg2, Object arg3) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, arg1, arg2, arg3);
        }
    }

    private Boolean callLuaBoolean(String functionName, Object arg, Object arg2) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        return functionObj == null ? null : LuaManager.caller.protectedCallBoolean(LuaManager.thread, functionObj, arg, arg2);
    }

    private Boolean callLuaBoolean(String functionName, Object arg, Object arg2, Object arg3) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        return functionObj == null ? null : LuaManager.caller.protectedCallBoolean(LuaManager.thread, functionObj, arg, arg2, arg3);
    }
}
