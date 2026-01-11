// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting.recipe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.utils.ByteBlock;
import zombie.debug.DebugLog;
import zombie.entity.components.crafting.CraftMode;
import zombie.entity.components.crafting.CraftRecipeMonitor;
import zombie.entity.components.crafting.CraftUtil;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.OutputFlag;
import zombie.entity.components.fluids.FluidConsume;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidSample;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceIO;
import zombie.entity.components.resources.ResourceItem;
import zombie.entity.components.resources.ResourceType;
import zombie.inventory.CompressIdenticalItems;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemUser;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Radio;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.entity.components.crafting.OutputScript;
import zombie.scripting.objects.CraftRecipeTag;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public class CraftRecipeData {
    private static final float FLOAT_EPSILON = 1.0E-5F;
    private static final int MAX_CRAFT_COUNT = 100;
    private static final ArrayDeque<CraftRecipeData> DATA_POOL = new ArrayDeque<>();
    private CraftRecipeMonitor craftRecipeMonitor;
    private CraftRecipe recipe;
    public final ArrayList<CraftRecipeData.InputScriptData> inputs = new ArrayList<>();
    private final ArrayList<CraftRecipeData.OutputScriptData> outputs = new ArrayList<>();
    private final HashSet<Resource> usedResources = new HashSet<>();
    private final HashSet<InventoryItem> usedItems = new HashSet<>();
    private boolean allowInputResources = true;
    private boolean allowInputItems;
    private boolean allowOutputResources = true;
    private boolean allowOutputItems;
    private CraftMode craftMode = CraftMode.Automation;
    private boolean hasConsumedInputs;
    private boolean hasTestedInputs;
    private final ItemDataList toOutputItems = new ItemDataList(32);
    private final HashSet<InventoryItem> consumedUsedItems = new HashSet<>();
    private final ArrayList<InventoryItem> allViableItems = new ArrayList<>();
    private final ArrayList<Resource> allViableResources = new ArrayList<>();
    private final HashMap<CraftRecipe.LuaCall, Object> luaFunctionMap = new HashMap<>();
    private static String luaOnTestCacheString;
    private static Object luaOnTestCacheObject;
    private float targetVariableInputRatio = Float.MAX_VALUE;
    private float calculatedVariableInputRatio = 1.0F;
    private final HashMap<InputScript, HashSet<InventoryItem>> variableInputOverfilledItems = new HashMap<>();
    private final HashMap<InputScript, HashMap<Resource, ArrayList<InventoryItem>>> variableInputOverfilledResources = new HashMap<>();
    private int eatPercentage;
    private double elapsedTime;
    private KahluaTable modData;

    public static CraftRecipeData Alloc(
        CraftMode craftMode, boolean allowInputResources, boolean allowInputItems, boolean allowOutputResources, boolean allowOutputItems
    ) {
        CraftRecipeData data = DATA_POOL.poll();
        if (data == null) {
            data = new CraftRecipeData();
        }

        data.craftMode = craftMode;
        data.allowInputResources = allowInputResources;
        data.allowInputItems = allowInputItems;
        data.allowOutputResources = allowOutputResources;
        data.allowOutputItems = allowOutputItems;
        return data;
    }

    public static void Release(CraftRecipeData data) {
        data.reset();
        DATA_POOL.add(data);
    }

    private CraftRecipeData() {
    }

    public CraftRecipeData(CraftMode craftMode, boolean allowInputResources, boolean allowInputItems, boolean allowOutputResources, boolean allowOutputItems) {
        this.craftMode = craftMode;
        this.allowInputResources = allowInputResources;
        this.allowInputItems = allowInputItems;
        this.allowOutputResources = allowOutputResources;
        this.allowOutputItems = allowOutputItems;
    }

    public void setMonitor(CraftRecipeMonitor monitor) {
        this.craftRecipeMonitor = monitor;
    }

    public boolean isAllowInputItems() {
        return this.allowInputItems;
    }

    public boolean isAllowOutputItems() {
        return this.allowOutputItems;
    }

    public boolean isAllowInputResources() {
        return this.allowInputResources;
    }

    public boolean isAllowOutputResources() {
        return this.allowOutputResources;
    }

    public ItemDataList getToOutputItems() {
        return this.toOutputItems;
    }

    public void reset() {
        this.allowInputResources = true;
        this.allowInputItems = false;
        this.allowOutputResources = true;
        this.allowOutputItems = false;
        this.craftMode = CraftMode.Automation;
        this.toOutputItems.reset();
        this.elapsedTime = 0.0;
        this.clearRecipe();
        this.craftRecipeMonitor = null;
    }

    private void clearRecipe() {
        this.recipe = null;

        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData input = this.inputs.get(i);
            CraftRecipeData.InputScriptData.Release(input);
        }

        this.inputs.clear();

        for (int i = 0; i < this.outputs.size(); i++) {
            CraftRecipeData.OutputScriptData output = this.outputs.get(i);
            CraftRecipeData.OutputScriptData.Release(output);
        }

        this.outputs.clear();
        this.luaFunctionMap.clear();
        luaOnTestCacheString = null;
        luaOnTestCacheObject = null;
        this.clearCaches();
        this.allViableItems.clear();
        this.allViableResources.clear();
        this.clearTargetVariableInputRatio();
    }

    private void clearCaches() {
        if (!this.inputs.isEmpty()) {
            for (int i = 0; i < this.inputs.size(); i++) {
                CraftRecipeData.InputScriptData input = this.inputs.get(i);
                input.clearCache();
            }
        }

        if (!this.outputs.isEmpty()) {
            for (int i = 0; i < this.outputs.size(); i++) {
                CraftRecipeData.OutputScriptData output = this.outputs.get(i);
                output.clearCache();
            }
        }

        this.hasConsumedInputs = false;
        this.hasTestedInputs = false;
        this.usedResources.clear();
        this.usedItems.clear();
        this.toOutputItems.clear();
        if (this.modData != null) {
            this.modData.wipe();
        }

        this.calculatedVariableInputRatio = 1.0F;
        this.variableInputOverfilledItems.clear();
    }

    public void setRecipe(CraftRecipe recipe) {
        if (recipe == null) {
            this.clearRecipe();
        } else if (this.recipe != recipe) {
            this.clearRecipe();
            this.recipe = recipe;

            for (int i = 0; i < recipe.getInputs().size(); i++) {
                CraftRecipeData.InputScriptData data = CraftRecipeData.InputScriptData.Alloc(this, recipe.getInputs().get(i));
                this.inputs.add(data);
            }

            for (int i = 0; i < recipe.getOutputs().size(); i++) {
                CraftRecipeData.OutputScriptData data = CraftRecipeData.OutputScriptData.Alloc(this, recipe.getOutputs().get(i));
                this.outputs.add(data);
            }
        } else {
            this.clearCaches();
        }
    }

    public CraftRecipe getRecipe() {
        return this.recipe;
    }

    public CraftRecipeData.InputScriptData getDataForInputScript(InputScript script) {
        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData data = this.inputs.get(i);
            if (data.inputScript == script) {
                return data;
            }

            if (data.inputScript.hasConsumeFromItem() && data.inputScript.getConsumeFromItemScript() == script) {
                return data;
            }
        }

        return null;
    }

    protected CraftRecipeData.OutputScriptData getDataForOutputScript(OutputScript script) {
        for (int i = 0; i < this.outputs.size(); i++) {
            CraftRecipeData.OutputScriptData data = this.outputs.get(i);
            if (data.outputScript == script) {
                return data;
            }
        }

        return null;
    }

    public InventoryItem getFirstManualInputFor(InputScript inputScript) {
        if (this.recipe != null && this.recipe.containsIO(inputScript)) {
            CraftRecipeData.InputScriptData data = this.getDataForInputScript(inputScript);
            return data.getFirstInputItem();
        } else {
            return null;
        }
    }

    public boolean canOfferInputItem(InventoryItem inventoryItem) {
        return this.canOfferInputItem(inventoryItem, false);
    }

    public boolean canOfferInputItem(InventoryItem inventoryItem, boolean verbose) {
        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData data = this.inputs.get(i);
            if (this.canOfferInputItem(data.inputScript, inventoryItem, verbose)) {
                return true;
            }
        }

        return false;
    }

    public boolean canOfferInputItem(InputScript inputScript, InventoryItem item) {
        return this.canOfferInputItem(inputScript, item, false);
    }

    public boolean canOfferInputItem(InputScript inputScript, InventoryItem item, boolean verbose) {
        Objects.requireNonNull(inputScript);
        Objects.requireNonNull(item);
        if (this.recipe == null) {
            return false;
        } else if (!this.recipe.containsIO(inputScript)) {
            if (verbose) {
                DebugLog.CraftLogic.warn("Input script not part of current recipe.");
            }

            return false;
        } else {
            CraftRecipeData.InputScriptData data = this.getDataForInputScript(inputScript);
            if (data == null) {
                if (verbose) {
                    DebugLog.CraftLogic.warn("Data is null for input script");
                }

                return false;
            } else if (data.getInputScript().getResourceType() != ResourceType.Item) {
                if (verbose) {
                    DebugLog.CraftLogic.warn("Cannot offer items to input scripts that are not ResourceType.Item");
                }

                return false;
            } else {
                return data.acceptsInputItem(item);
            }
        }
    }

    public boolean offerAndReplaceInputItem(InventoryItem inventoryItem) {
        for (int i = 0; i < this.inputs.size(); i++) {
            if (this.offerAndReplaceInputItem(this.inputs.get(i), inventoryItem)) {
                return true;
            }
        }

        return false;
    }

    public boolean offerAndReplaceInputItem(CraftRecipeData.InputScriptData data, InventoryItem inventoryItem) {
        if (this.canOfferInputItem(data.inputScript, inventoryItem, false) && !this.containsInputItem(inventoryItem)) {
            if (data.inputScript.isExclusive()
                && data.getFirstInputItem() != null
                && !data.getFirstInputItem().getFullType().equals(inventoryItem.getFullType())) {
                while (data.getLastInputItem() != null) {
                    data.removeInputItem(data.getLastInputItem());
                }
            }

            if (data.isInputItemsSatisfied() && data.isInputItemsSatisifiedToMaximum()) {
                data.removeInputItem(data.getLastInputItem());
            }

            return data.addInputItem(inventoryItem);
        } else {
            return false;
        }
    }

    public boolean offerInputItem(InputScript inputScript, InventoryItem item) {
        return this.offerInputItem(inputScript, item, false);
    }

    public boolean offerInputItem(InputScript inputScript, InventoryItem item, boolean verbose) {
        if (this.canOfferInputItem(inputScript, item, verbose) && !this.containsInputItem(item)) {
            CraftRecipeData.InputScriptData data = this.getDataForInputScript(inputScript);
            if (inputScript.isExclusive() && data.getFirstInputItem() != null && !data.getFirstInputItem().getFullType().equals(item.getFullType())) {
                return false;
            }

            if (!data.isInputItemsSatisfied() || !data.isInputItemsSatisifiedToMaximum()) {
                return data.addInputItem(item);
            }
        }

        return false;
    }

    public boolean containsInputItem(InventoryItem inventoryItem) {
        for (int i = 0; i < this.inputs.size(); i++) {
            if (this.containsInputItem(this.inputs.get(i), inventoryItem)) {
                return true;
            }
        }

        return false;
    }

    public boolean containsInputItem(CraftRecipeData.InputScriptData data, InventoryItem inventoryItem) {
        return data.inputScript.getResourceType() == ResourceType.Item && data.inputItems.contains(inventoryItem);
    }

    public boolean removeInputItem(InventoryItem inventoryItem) {
        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData data = this.inputs.get(i);
            if (data.removeInputItem(inventoryItem)) {
                return true;
            }
        }

        return false;
    }

    public boolean areAllInputItemsSatisfied() {
        for (int i = 0; i < this.inputs.size(); i++) {
            if (!this.inputs.get(i).isInputItemsSatisfied()) {
                return false;
            }
        }

        return true;
    }

    public boolean luaCallOnTest() {
        return true;
    }

    private boolean initLuaFunctions() {
        if (this.recipe == null) {
            return false;
        } else {
            for (CraftRecipe.LuaCall luaCall : CraftRecipe.LuaCall.values()) {
                if (this.recipe.hasLuaCall(luaCall)) {
                    Object functionObject = LuaManager.getFunctionObject(this.recipe.getLuaCallString(luaCall), null);
                    if (functionObject != null) {
                        this.luaFunctionMap.put(luaCall, functionObject);
                    } else {
                        DebugLog.CraftLogic.warn("Could not find lua function: " + this.recipe.getLuaCallString(luaCall));
                    }
                }
            }

            return true;
        }
    }

    public void luaCallOnStart() {
        this.luaCallOnStart(null);
    }

    public void luaCallOnStart(IsoGameCharacter character) {
        if (this.initLuaFunctions()) {
            Object functionObject = this.luaFunctionMap.get(CraftRecipe.LuaCall.OnStart);
            if (functionObject != null) {
                LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, this, character);
            }
        }
    }

    public void luaCallOnUpdate() {
        if (this.initLuaFunctions()) {
            Object functionObject = this.luaFunctionMap.get(CraftRecipe.LuaCall.OnUpdate);
            if (functionObject != null) {
                LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, this);
            }
        }
    }

    public void luaCallOnCreate() {
        this.luaCallOnCreate(null);
    }

    public void luaCallOnCreate(IsoGameCharacter character) {
        if (this.initLuaFunctions()) {
            Object functionObject = this.luaFunctionMap.get(CraftRecipe.LuaCall.OnCreate);
            if (functionObject != null) {
                LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, this, character);
            }
        }
    }

    public void luaCallOnFailed() {
        if (this.initLuaFunctions()) {
            Object functionObject = this.luaFunctionMap.get(CraftRecipe.LuaCall.OnFailed);
            if (functionObject != null) {
                LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObject, this);
            }
        }
    }

    public boolean canPerform(
        IsoGameCharacter character,
        List<Resource> inputResources,
        List<InventoryItem> overrideInputItems,
        boolean forceTestAll,
        ArrayList<ItemContainer> containers
    ) {
        return !CraftRecipeManager.isValidRecipeForCharacter(this.recipe, character, this.craftRecipeMonitor, containers)
            ? false
            : this.consumeInputsInternal(character, true, inputResources, overrideInputItems, forceTestAll, false)
                && this.createOutputsInternal(true, null, character);
    }

    public boolean perform(
        IsoGameCharacter character, List<Resource> inputResources, List<InventoryItem> overrideInputItems, ArrayList<ItemContainer> containers
    ) {
        if (!CraftRecipeManager.isValidRecipeForCharacter(this.recipe, character, this.craftRecipeMonitor, containers)) {
            return false;
        } else {
            boolean success = this.consumeInputsInternal(character, false, inputResources, overrideInputItems);
            if (success) {
                this.luaCallOnStart(character);
                if (this.createOutputsInternal(false, null, character)) {
                    if (this.recipe.xpAward != null) {
                        this.addXP(character);
                    }

                    ((IsoPlayer)character).getPlayerCraftHistory().addCraftHistoryCraftedEvent(this.recipe.getName());
                    return true;
                }
            }

            return false;
        }
    }

    private void addXP(IsoGameCharacter character) {
        this.recipe.addXP(character);
    }

    public void processDestroyAndUsedItems(IsoGameCharacter character) {
        ArrayList<InputScript> recipeinputs = this.recipe.getInputs();
        float[] requiredTally = new float[recipeinputs.size()];

        for (int i = 0; i < recipeinputs.size(); i++) {
            if (recipeinputs.get(i).isVariableAmount()) {
                requiredTally[i] = recipeinputs.get(i).getIntAmount() * this.calculatedVariableInputRatio;
            } else {
                requiredTally[i] = recipeinputs.get(i).getIntAmount();
            }
        }

        for (InventoryItem inventoryItem : this.consumedUsedItems) {
            Item item = inventoryItem.getScriptItem();
            DebugLog.CraftLogic.println("post process -> Sync using item: " + inventoryItem.getFullType());

            for (int j = 0; j < recipeinputs.size(); j++) {
                CraftRecipeData.InputScriptData inputData = this.getDataForInputScript(recipeinputs.get(j));
                if (inputData.hasAppliedItem(inventoryItem)) {
                    InputScript input = inputData.getInputScript();
                    if (input.hasFlag(InputFlag.Unseal) && inventoryItem.getFluidContainer() != null) {
                    }

                    if (!recipeinputs.get(j).isItemCount() && (recipeinputs.get(j).hasConsumeFromItem() || inventoryItem.getFluidContainer() != null)) {
                        continue;
                    }

                    this.processKeepInputItem(inputData, character, false, inventoryItem);
                    if (requiredTally[j] > 1.0E-5F) {
                        float reqtally = (float)Math.ceil(requiredTally[j]);
                        boolean destroy = recipeinputs.get(j).isDestroy();
                        if (recipeinputs.get(j).hasFlag(InputFlag.DontReplace)
                            && (inventoryItem.getReplaceOnUse() != null || inventoryItem.getScriptItem().getReplaceOnDeplete() != null)) {
                            destroy = true;
                        }

                        if (recipeinputs.get(j).isItemCount()) {
                            int toConsume = inventoryItem.getCurrentUses();
                            ItemUser.UseItem(inventoryItem, true, false, toConsume, recipeinputs.get(j).isKeep(), destroy);
                            float itemscale = 1.0F / recipeinputs.get(j).getRelativeScale(item.getFullName());
                            requiredTally[j] -= itemscale;
                        } else {
                            float itemscale = recipeinputs.get(j).getRelativeScale(item.getFullName());
                            int toConsume = (int)(reqtally * itemscale);
                            toConsume = Math.min(toConsume, (int)reqtally);
                            int consumed = ItemUser.UseItem(inventoryItem, true, false, toConsume, recipeinputs.get(j).isKeep(), destroy);
                            requiredTally[j] -= consumed / itemscale;
                        }
                    }
                }

                if (GameServer.server) {
                    GameServer.sendItemStats(inventoryItem);
                }
            }
        }
    }

    public int getPossibleCraftCount(
        List<Resource> inputResources,
        List<InventoryItem> inputItems,
        List<Resource> consumedResources,
        List<InventoryItem> consumedItems,
        boolean limitItemsToAppliedItems
    ) {
        consumedItems.clear();
        consumedResources.clear();
        int possibleCraftCount = 100;

        for (int i = 0; i < this.inputs.size(); i++) {
            int inputItemsPossibleCraftCount = this.getInputCraftCount(this.inputs.get(i), inputItems, consumedItems, limitItemsToAppliedItems);
            int inputResourcePossibleCraftCount = this.getResourceCraftCount(this.inputs.get(i), inputResources, consumedResources, limitItemsToAppliedItems);
            possibleCraftCount = Math.min(possibleCraftCount, inputItemsPossibleCraftCount + inputResourcePossibleCraftCount);
            if (possibleCraftCount == 0) {
                break;
            }
        }

        return possibleCraftCount;
    }

    private int getInputCraftCount(
        CraftRecipeData.InputScriptData inputData, List<InventoryItem> inputItems, List<InventoryItem> consumedItems, boolean limitItemsToAppliedItems
    ) {
        float FLOAT_EPSILON = 1.0E-5F;
        InputScript inputScript = inputData.getInputScript();
        double inputFilledAmount = 0.0;
        int createToItemSlots = 0;
        if (inputItems != null) {
            for (int j = 0; j < inputItems.size(); j++) {
                InventoryItem invItem = inputItems.get(j);
                boolean canUse = inputScript.canUseItem(invItem);
                if (limitItemsToAppliedItems) {
                    canUse = canUse && inputData.hasAppliedItemType(invItem.getScriptItem());
                }

                if (canUse) {
                    double itemScale = inputData.getInputScript().getRelativeScale(invItem.getFullType());
                    int uses = inputData.getInputScript().isItemCount() ? 1 : invItem.getCurrentUses();
                    double scaledUses = uses / itemScale;
                    if (!inputScript.isItemCount()) {
                        if (inputScript.hasConsumeFromItem()) {
                            InputScript script = inputScript.getConsumeFromItemScript();
                            switch (script.getResourceType()) {
                                case Fluid:
                                    scaledUses = invItem.getFluidContainer().getAmount();
                            }
                        }

                        if (inputScript.hasCreateToItem()) {
                            OutputScript script = inputScript.getCreateToItemScript();
                            switch (script.getResourceType()) {
                                case Fluid:
                                    float availableSlots = invItem.getFluidContainer().getFreeCapacity() / script.getAmount();
                                    if (script.hasFlag(OutputFlag.RespectCapacity)) {
                                        createToItemSlots += (int)Math.floor(availableSlots);
                                    } else {
                                        createToItemSlots += (int)Math.ceil(availableSlots);
                                    }
                            }
                        }
                    }

                    inputFilledAmount += scaledUses;
                    consumedItems.add(invItem);
                }
            }
        }

        inputFilledAmount += 1.0E-5F;
        if (inputScript.isKeep() && !inputScript.hasConsumeFromItem() && !inputScript.hasCreateToItem()) {
            int inputReqdAmount = inputScript.getIntAmount();
            return inputFilledAmount < inputReqdAmount ? 0 : 100;
        } else {
            float inputReqdAmount = inputScript.getAmount();
            if (inputScript.hasConsumeFromItem() && !inputScript.isItemCount()) {
                InputScript script = inputScript.getConsumeFromItemScript();
                switch (script.getResourceType()) {
                    case Fluid:
                        inputReqdAmount = script.getAmount();
                }
            }

            int thisItemPossibleCraftCount = (int)Math.floor(inputFilledAmount / inputReqdAmount);
            if (inputScript.hasCreateToItem() && !inputScript.isItemCount()) {
                thisItemPossibleCraftCount = createToItemSlots;
            }

            return thisItemPossibleCraftCount;
        }
    }

    private int getResourceCraftCount(
        CraftRecipeData.InputScriptData inputData, List<Resource> inputResources, List<Resource> consumedResources, boolean limitItemsToAppliedItems
    ) {
        int possibleCraftCount = 0;
        int j = 0;

        while (j < inputResources.size()) {
            Resource resource = inputResources.get(j);
            switch (resource.getType()) {
                case Item:
                    ArrayList<InventoryItem> allItems = ((ResourceItem)resource).getStoredItems();
                    ArrayList<InventoryItem> consumedItems = new ArrayList<>();
                    possibleCraftCount += this.getInputCraftCount(inputData, allItems, consumedItems, limitItemsToAppliedItems);
                    if (!consumedItems.isEmpty()) {
                        consumedResources.add(resource);
                    }
                default:
                    j++;
            }
        }

        return Math.min(possibleCraftCount, 100);
    }

    public boolean canConsumeInputs(List<Resource> inputResources, List<InventoryItem> overrideInputItems, boolean forceTestAll, boolean clearAllViable) {
        return this.consumeInputsInternal(null, true, inputResources, overrideInputItems, forceTestAll, clearAllViable);
    }

    public boolean canConsumeInputs(List<Resource> inputResources) {
        return this.consumeInputsInternal(null, true, inputResources, null);
    }

    public boolean consumeInputs(List<Resource> inputResources) {
        return this.consumeInputsInternal(null, false, inputResources, null);
    }

    public boolean consumeOnTickInputs(List<Resource> inputResources) {
        if (this.recipe == null) {
            return false;
        } else {
            return !this.recipe.hasOnTickInputs() ? true : this.consumeRecipeInputsOnTick(inputResources);
        }
    }

    public boolean canCreateOutputs(List<Resource> outputResources) {
        return this.createOutputsInternal(true, outputResources, null);
    }

    public boolean createOutputs(List<Resource> outputResources) {
        return this.createOutputsInternal(false, outputResources, null);
    }

    public boolean canCreateOutputs(List<Resource> outputResources, IsoGameCharacter character) {
        return this.createOutputsInternal(true, outputResources, character);
    }

    public boolean createOutputs(List<Resource> outputResources, IsoGameCharacter character) {
        return this.createOutputsInternal(false, outputResources, character);
    }

    public boolean createOnTickOutputs(List<Resource> outputResources) {
        if (this.recipe == null) {
            return false;
        } else {
            return !this.recipe.hasOnTickOutputs() ? true : this.createRecipeOutputsOnTick(outputResources);
        }
    }

    private boolean consumeInputsInternal(IsoGameCharacter character, boolean testOnly, List<Resource> inputResources, List<InventoryItem> overrideInputItems) {
        return this.consumeInputsInternal(character, testOnly, inputResources, overrideInputItems, false, false);
    }

    private boolean consumeInputsInternal(
        IsoGameCharacter character,
        boolean testOnly,
        List<Resource> inputResources,
        List<InventoryItem> overrideInputItems,
        boolean forceTestAll,
        boolean clearAllViable
    ) {
        if (!testOnly && GameClient.client) {
            throw new RuntimeException("Cannot call with testOnly==false on client.");
        } else if (this.hasConsumedInputs) {
            return true;
        } else if (this.recipe != null && !this.recipe.canBeDoneInDark() && character instanceof IsoPlayer player && player.tooDarkToRead()) {
            return false;
        } else {
            this.clearCaches();
            boolean success = this.consumeRecipeInputs(testOnly, inputResources, overrideInputItems, forceTestAll, clearAllViable, character);
            if (success) {
                this.hasConsumedInputs = !testOnly;
                this.hasTestedInputs = testOnly;
            }

            return success;
        }
    }

    private boolean createOutputsInternal(boolean testOnly, List<Resource> outputResources, IsoGameCharacter character) {
        if (!testOnly && GameClient.client) {
            throw new RuntimeException("Cannot call with testOnly==false on client.");
        } else if (!testOnly && !this.hasConsumedInputs) {
            if (Core.debug) {
                throw new RuntimeException("createOutputs requires consumeInputs to be called first");
            } else {
                return false;
            }
        } else if (!testOnly || this.hasTestedInputs || this.hasConsumedInputs) {
            boolean success = this.createRecipeOutputs(testOnly, outputResources, character);
            if (!testOnly) {
                this.hasConsumedInputs = false;
            } else {
                this.hasTestedInputs = false;
            }

            if (!testOnly && success) {
                this.destroyAllSurvivingDestroyInputs();
            }

            return success;
        } else if (Core.debug) {
            throw new RuntimeException("(test) createOutputs requires consumeInputs to be called first");
        } else {
            return false;
        }
    }

    private boolean consumeRecipeInputsOnTick(List<Resource> inputResources) {
        if (this.recipe == null) {
            return false;
        } else {
            this.usedResources.clear();
            boolean consumed = false;
            boolean failed = false;

            for (int i = 0; i < this.inputs.size(); i++) {
                CraftRecipeData.InputScriptData data = this.inputs.get(i);
                if (data.inputScript.isApplyOnTick()
                    && data.inputScript.getResourceType() != ResourceType.Item
                    && (!data.inputScript.hasFlag(InputFlag.HandcraftOnly) || this.craftMode == CraftMode.Handcraft)
                    && (!data.inputScript.hasFlag(InputFlag.AutomationOnly) || this.craftMode == CraftMode.Automation)) {
                    consumed = false;
                    if (this.allowInputResources && inputResources != null && !inputResources.isEmpty()) {
                        consumed = this.consumeInputFromResources(data.inputScript, inputResources, false, null, null);
                    }

                    if (!consumed) {
                        failed = true;
                        break;
                    }
                }
            }

            return !failed;
        }
    }

    private boolean consumeRecipeInputs(
        boolean testOnly,
        List<Resource> inputResources,
        List<InventoryItem> overrideInputItems,
        boolean forceTestAll,
        boolean clearAllViable,
        IsoGameCharacter character
    ) {
        if (this.recipe == null) {
            return false;
        } else {
            this.usedResources.clear();
            this.usedItems.clear();
            if (clearAllViable) {
                this.allViableResources.clear();
                this.allViableItems.clear();
            }

            if (this.craftRecipeMonitor != null) {
                this.craftRecipeMonitor.log("[ConsumeRecipeInputs]");
                this.craftRecipeMonitor.open();
                this.craftRecipeMonitor.log("test = " + testOnly);
                this.craftRecipeMonitor.log("overrideInputItems = " + (overrideInputItems != null));
            }

            boolean consumed = false;
            boolean failed = false;
            this.calculatedVariableInputRatio = this.isVariableAmount() ? this.targetVariableInputRatio : 1.0F;

            for (int i = 0; i < this.inputs.size(); i++) {
                CraftRecipeData.InputScriptData data = this.inputs.get(i);
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.log("[" + i + "] input, line = \"" + data.inputScript.getOriginalLine().trim() + "\"");
                }

                if (data.inputScript.hasFlag(InputFlag.HandcraftOnly) && this.craftMode != CraftMode.Handcraft) {
                    if (this.craftRecipeMonitor != null) {
                        this.craftRecipeMonitor.log("-> skipping line, 'handcraft' only");
                    }
                } else if (data.inputScript.hasFlag(InputFlag.AutomationOnly) && this.craftMode != CraftMode.Automation) {
                    if (this.craftRecipeMonitor != null) {
                        this.craftRecipeMonitor.log("-> skipping line, 'automation' only");
                    }
                } else if (data.inputScript.isApplyOnTick()) {
                    if (this.craftRecipeMonitor != null) {
                        this.craftRecipeMonitor.log("-> skipping line, onTick = true");
                    }
                } else {
                    consumed = false;
                    List<InventoryItem> items = (List<InventoryItem>)(overrideInputItems != null ? overrideInputItems : data.inputItems);
                    if (this.allowInputItems && !items.isEmpty() && data.inputScript.getResourceType() == ResourceType.Item) {
                        consumed = this.consumeInputFromItems(data.inputScript, items, testOnly, data, false, character);
                        if (consumed && this.craftRecipeMonitor != null) {
                            this.craftRecipeMonitor.success("consumed from supplied items list");
                        }
                    }

                    if (!consumed && this.allowInputResources && inputResources != null && !inputResources.isEmpty()) {
                        consumed = this.consumeInputFromResources(data.inputScript, inputResources, testOnly, data, character);
                    }

                    data.cachedCanConsume = consumed;
                    if (!consumed) {
                        failed = true;
                        if (!forceTestAll) {
                            break;
                        }
                    }
                }
            }

            if (failed) {
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.warn("NOT CONSUMED!");
                    this.craftRecipeMonitor.close();
                }

                return false;
            } else {
                if (!testOnly) {
                    for (int i = 0; i < this.inputs.size(); i++) {
                        InputScript script = this.inputs.get(i).inputScript;
                        if (this.variableInputOverfilledItems.containsKey(script)) {
                            int itemsToAdd = (int)Math.ceil(this.calculatedVariableInputRatio * script.getAmount()) - script.getIntAmount();

                            for (InventoryItem item : this.variableInputOverfilledItems.get(script)) {
                                if (itemsToAdd <= 0) {
                                    break;
                                }

                                this.consumedUsedItems.add(item);
                                itemsToAdd--;
                            }

                            if (Core.debug && itemsToAdd > 0) {
                                throw new RuntimeException("Calculated ratio calls for more items than we have. This should not be possible");
                            }
                        }

                        if (this.variableInputOverfilledResources.containsKey(script)) {
                            int itemsToAdd = (int)Math.ceil(this.calculatedVariableInputRatio * script.getAmount()) - script.getIntAmount();

                            for (Resource res : this.variableInputOverfilledResources.get(script).keySet()) {
                                for (InventoryItem item : this.variableInputOverfilledResources.get(script).get(res)) {
                                    if (itemsToAdd <= 0) {
                                        break;
                                    }

                                    if (res.getType() == ResourceType.Item && (!script.isKeep() || res.canMoveItemsToOutput())) {
                                        InventoryItem removedItem = ((ResourceItem)res).removeItem(item);
                                        if ((removedItem == null || removedItem != item) && Core.debug) {
                                            throw new RuntimeException("Item didn't get removed.");
                                        }
                                    }

                                    itemsToAdd--;
                                }
                            }

                            if (Core.debug && itemsToAdd > 0) {
                                throw new RuntimeException("Calculated ratio calls for more items than we have. This should not be possible");
                            }
                        }
                    }
                }

                if (this.allowInputResources && inputResources != null && !inputResources.isEmpty()) {
                    for (int i = 0; i < inputResources.size(); i++) {
                        Resource resource = inputResources.get(i);
                        if (resource.getType() == ResourceType.Item && !resource.isEmpty() && !this.usedResources.contains(resource)) {
                            if (this.craftRecipeMonitor != null) {
                                this.craftRecipeMonitor.warn("CANCEL, not all [Resource] items could be consumed!");
                                this.craftRecipeMonitor.close();
                            }

                            return false;
                        }
                    }
                }

                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.log("[ALL PASSED] returning");
                    this.craftRecipeMonitor.close();
                }

                return true;
            }
        }
    }

    public boolean OnTestItem(InventoryItem inventoryItem) {
        return this.recipe.OnTestItem(inventoryItem);
    }

    private boolean consumeInputFromItems(
        InputScript input, List<InventoryItem> items, boolean testOnly, CraftRecipeData.CacheData cacheData, boolean clearUsed, IsoGameCharacter character
    ) {
        if (input != null && input.getResourceType() == ResourceType.Item) {
            if (items.isEmpty()) {
                return false;
            } else {
                float targetRequiredAmount = input.getIntAmount();
                float maxAllowedAmount = input.getIntMaxAmount();
                if (input.hasConsumeFromItem()) {
                    InputScript consumeScript = input.getConsumeFromItemScript();
                    targetRequiredAmount = consumeScript.getAmount();
                    maxAllowedAmount = consumeScript.getMaxAmount();
                }

                if (targetRequiredAmount <= 0.0F) {
                    return false;
                } else {
                    if (clearUsed) {
                        this.usedItems.clear();
                    }

                    HashSet<InventoryItem> tempUsedItems = new HashSet<>();
                    HashSet<InventoryItem> tempAllViableItems = new HashSet<>();
                    HashSet<InventoryItem> tempConsumedUsedItems = new HashSet<>();
                    HashSet<InventoryItem> tempVariableOverfillUsedItems = new HashSet<>();
                    float requiredAmount = targetRequiredAmount;
                    float allowedAmount = maxAllowedAmount;
                    List<Item> inputItems = input.getPossibleInputItems();

                    for (int j = 0; j < inputItems.size(); j++) {
                        for (int i = 0; i < items.size(); i++) {
                            InventoryItem inventoryItem = items.get(i);
                            if (inventoryItem.getScriptItem().getFullName().equals(inputItems.get(j).getFullName())
                                && !this.usedItems.contains(inventoryItem)
                                && !tempUsedItems.contains(inventoryItem)) {
                                float preUsedFluidAvail = 0.0F;
                                if (!testOnly && input.hasConsumeFromItem()) {
                                    InputScript consumeScript = input.getConsumeFromItemScript();
                                    switch (consumeScript.getResourceType()) {
                                        case Fluid:
                                            preUsedFluidAvail = inventoryItem.getFluidContainer().getAmount();
                                    }
                                }

                                if (CraftRecipeManager.consumeInputItem(input, inventoryItem, testOnly, cacheData, character)) {
                                    tempUsedItems.add(inventoryItem);
                                    if (!this.allViableItems.contains(inventoryItem)) {
                                        tempAllViableItems.add(inventoryItem);
                                    }

                                    if (!testOnly) {
                                        if (requiredAmount > 1.0E-5F) {
                                            tempConsumedUsedItems.add(inventoryItem);
                                        } else {
                                            tempVariableOverfillUsedItems.add(inventoryItem);
                                        }
                                    }

                                    float uses = input.isItemCount() ? 1.0F : inventoryItem.getCurrentUses();
                                    float scale = input.getRelativeScale(inventoryItem.getFullType());
                                    float requses = uses / scale;
                                    if (input.hasConsumeFromItem()) {
                                        InputScript script = input.getConsumeFromItemScript();
                                        switch (script.getResourceType()) {
                                            case Fluid:
                                                if (!testOnly) {
                                                    float postUsedFluidAvail = inventoryItem.getFluidContainer().getAmount();
                                                    uses = preUsedFluidAvail - postUsedFluidAvail;
                                                    requses = uses;
                                                } else {
                                                    uses = inventoryItem.getFluidContainer().getAmount();
                                                    requses = uses;
                                                }
                                        }
                                    }

                                    uses = Math.min(requses, uses);
                                    requiredAmount -= uses;
                                    allowedAmount -= uses;
                                    if (allowedAmount <= 1.0E-5F) {
                                        break;
                                    }
                                }
                            }
                        }

                        if (requiredAmount <= 1.0E-5F) {
                            this.usedItems.addAll(tempUsedItems);
                            this.consumedUsedItems.addAll(tempConsumedUsedItems);
                            this.allViableItems.addAll(tempAllViableItems);
                            if (input.isVariableAmount()) {
                                float filledAmount = Math.min(targetRequiredAmount - requiredAmount, maxAllowedAmount);
                                float filledRatio = filledAmount / targetRequiredAmount;
                                this.calculatedVariableInputRatio = Math.min(this.calculatedVariableInputRatio, filledRatio);
                                this.variableInputOverfilledItems.put(input, tempVariableOverfillUsedItems);
                            }

                            return true;
                        }

                        if (input.isExclusive()) {
                            requiredAmount = targetRequiredAmount;
                            allowedAmount = maxAllowedAmount;
                            tempUsedItems.clear();
                            tempAllViableItems.clear();
                            tempConsumedUsedItems.clear();
                            tempVariableOverfillUsedItems.clear();
                        }
                    }

                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private boolean consumeInputFromResources(
        InputScript input, List<Resource> resources, boolean testOnly, CraftRecipeData.CacheData cacheData, IsoGameCharacter character
    ) {
        if (input != null && resources != null && !resources.isEmpty()) {
            if (cacheData == null) {
                throw new RuntimeException("Input requires cache data.");
            } else {
                for (int i = 0; i < resources.size(); i++) {
                    Resource resource = resources.get(i);
                    if (input.getResourceType() == resource.getType() && !this.usedResources.contains(resource)) {
                        boolean success = CraftRecipeManager.consumeInputFromResource(input, resource, testOnly, cacheData, character);
                        if (success) {
                            this.usedResources.add(resource);
                            this.allViableResources.add(resource);
                            if (this.craftRecipeMonitor != null) {
                                this.craftRecipeMonitor.success("consumed by resource: " + resource.getId());
                            }

                            return true;
                        }

                        cacheData.softReset();
                    }
                }

                return false;
            }
        } else {
            return false;
        }
    }

    private boolean createRecipeOutputsOnTick(List<Resource> outputResources) {
        if (this.recipe == null) {
            return false;
        } else {
            this.usedResources.clear();
            boolean failed = false;

            for (int i = 0; i < this.outputs.size(); i++) {
                CraftRecipeData.OutputScriptData data = this.outputs.get(i);
                if (data.outputScript.isApplyOnTick()
                    && data.outputScript.getResourceType() != ResourceType.Item
                    && (!data.outputScript.hasFlag(OutputFlag.HandcraftOnly) || this.craftMode == CraftMode.Handcraft)
                    && (!data.outputScript.hasFlag(OutputFlag.AutomationOnly) || this.craftMode == CraftMode.Automation)) {
                    boolean created = false;
                    if (this.allowOutputResources && outputResources != null && !outputResources.isEmpty()) {
                        created = this.createOutputToResources(data.outputScript, outputResources, false, null);
                    }

                    if (!created) {
                        failed = true;
                        break;
                    }
                }
            }

            return !failed;
        }
    }

    public boolean createRecipeOutputs(boolean testOnly, List<Resource> outputResources, IsoGameCharacter character) {
        if (this.recipe == null) {
            return false;
        } else {
            this.toOutputItems.clear();
            this.usedResources.clear();
            if (this.craftRecipeMonitor != null) {
                this.craftRecipeMonitor.log("[CreateRecipeOutputs]");
                this.craftRecipeMonitor.open();
                this.craftRecipeMonitor.log("test = " + testOnly);
            }

            boolean failed = false;

            for (int i = 0; i < this.outputs.size(); i++) {
                CraftRecipeData.OutputScriptData data = this.outputs.get(i);
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.log("[" + i + "] output, line = \"" + data.outputScript.getOriginalLine().trim() + "\"");
                }

                if (data.outputScript.hasFlag(OutputFlag.HandcraftOnly) && this.craftMode != CraftMode.Handcraft) {
                    if (this.craftRecipeMonitor != null) {
                        this.craftRecipeMonitor.log("-> skipping line, 'handcraft' only");
                    }
                } else if (data.outputScript.hasFlag(OutputFlag.AutomationOnly) && this.craftMode != CraftMode.Automation) {
                    if (this.craftRecipeMonitor != null) {
                        this.craftRecipeMonitor.log("-> skipping line, 'automation' only");
                    }
                } else if (data.outputScript.isApplyOnTick()) {
                    if (this.craftRecipeMonitor != null) {
                        this.craftRecipeMonitor.log("-> skipping line, onTick = true");
                    }
                } else {
                    boolean created = false;
                    if (data.outputScript.getResourceType() == ResourceType.Item) {
                        created = this.createOutputItems(data.outputScript, this.toOutputItems, testOnly, data, character);
                    }

                    if (!created && this.allowOutputResources && outputResources != null && !outputResources.isEmpty()) {
                        created = this.createOutputToResources(data.outputScript, outputResources, testOnly, data);
                    }

                    if (!created) {
                        failed = true;
                        break;
                    }
                }
            }

            if (!failed) {
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.log("[Collecting keep items]");
                }

                this.collectKeepItems(this.toOutputItems, testOnly);
            }

            if (!failed && this.allowOutputResources && this.toOutputItems.size() > 0 && outputResources != null && !outputResources.isEmpty()) {
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.log("[Distribute items to outputs]");
                }

                this.distributeItemsToResources(outputResources, this.toOutputItems, testOnly);
            }

            if (!failed && !this.allowOutputItems && this.toOutputItems.hasUnprocessed()) {
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.warn("FAILED: unable to offload all created items to output resources!");
                }

                failed = true;
            }

            if (failed) {
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.warn("NOT CREATED!");
                    this.craftRecipeMonitor.close();
                }

                return false;
            } else {
                if (this.craftRecipeMonitor != null) {
                    this.craftRecipeMonitor.log("[ALL PASSED] returning");
                    this.craftRecipeMonitor.close();
                }

                if (character != null && this.getFirstInputItemWithFlag("ResearchInput") != null) {
                    ArrayList<InventoryItem> researchInputs = this.getAllInputItemsWithFlag("ResearchInput");
                    if (!researchInputs.isEmpty()) {
                        for (int j = 0; j < researchInputs.size(); j++) {
                            InventoryItem researchInput = researchInputs.get(j);
                            if (!researchInput.getResearchableRecipes(character).isEmpty()) {
                                researchInput.researchRecipes(character);
                            }
                        }
                    }
                }

                return true;
            }
        }
    }

    private void processKeepInputItem(CraftRecipeData.InputScriptData inputData, IsoGameCharacter character, boolean testOnly, InventoryItem inventoryItem) {
        CraftRecipe recipe = inputData.getRecipeData().getRecipe();
        if (inputData.getInputScript().isKeep() && !testOnly) {
            DebugLog.CraftLogic.debugln("Recipe is " + inputData.getRecipeData().getRecipe().getName());
            DebugLog.CraftLogic.debugln("Item is " + inventoryItem.getType());
            InputScript input = inputData.getInputScript();
            if (input.hasFlag(InputFlag.MayDegradeHeavy)) {
                int skill = 0;
                if (character != null) {
                    skill = recipe.getHighestRelevantSkillLevel(character) + inventoryItem.getMaintenanceMod(character);
                }

                inventoryItem.damageCheck(skill, 1.0F, false);
            } else if (input.hasFlag(InputFlag.MayDegrade)) {
                int skill = 0;
                if (character != null) {
                    skill = recipe.getHighestRelevantSkillLevel(character) + inventoryItem.getMaintenanceMod(character);
                }

                inventoryItem.damageCheck(skill, 2.0F, false);
            } else if (input.hasFlag(InputFlag.MayDegradeLight)) {
                int skill = 0;
                if (character != null) {
                    skill = recipe.getHighestRelevantSkillLevel(character) + inventoryItem.getMaintenanceMod(character);
                }

                inventoryItem.damageCheck(skill, 3.0F, false);
            } else if (input.hasFlag(InputFlag.MayDegradeVeryLight)) {
                int skill = 0;
                if (character != null) {
                    skill = recipe.getHighestRelevantSkillLevel(character) + inventoryItem.getMaintenanceMod(character);
                }

                inventoryItem.damageCheck(skill, 6.0F, false);
            }

            if (input.hasFlag(InputFlag.SharpnessCheck)) {
                int skill = 0;
                if (character != null) {
                    skill = recipe.getHighestRelevantSkillLevel(character) + inventoryItem.getMaintenanceMod(character);
                }

                if (!inventoryItem.hasSharpness()) {
                    inventoryItem.damageCheck(skill, 6.0F, false);
                } else {
                    inventoryItem.sharpnessCheck(skill, 1.0F, false);
                }
            }

            if (input.hasFlag(InputFlag.Unseal) && inventoryItem.getFluidContainer() != null) {
                inventoryItem.getFluidContainer().unseal();
            }

            if (inventoryItem.hasTag(ItemTag.BREAK_ON_SMITHING) && this.recipe.isSmithing()) {
                inventoryItem.setCondition(0, true);
            }

            inventoryItem.syncItemFields();
        }
    }

    private boolean createOutputItems(
        OutputScript output, ItemDataList items, boolean testOnly, CraftRecipeData.CacheData cacheData, IsoGameCharacter character
    ) {
        if (output != null && output.getResourceType() == ResourceType.Item) {
            if (output.getIntAmount() <= 0) {
                return false;
            } else {
                Item outputItem = output.getItem(this);
                if (outputItem == null) {
                    return false;
                } else {
                    int createQty = output.getIntAmount();
                    if (output.isVariableAmount()) {
                        float ratio = testOnly ? this.calculatedVariableInputRatio : this.targetVariableInputRatio;
                        createQty = Math.min((int)Math.ceil(output.getAmount() * ratio), output.getIntMaxAmount());
                    }

                    if (testOnly) {
                        for (int i = 0; i < createQty; i++) {
                            items.addItem(outputItem);
                        }

                        return true;
                    } else if (this.recipe.hasTag(CraftRecipeTag.REMOVE_RESULT_ITEMS)) {
                        return true;
                    } else {
                        for (int i = 0; i < createQty; i++) {
                            if (!CraftRecipeManager.createOutputItem(output, outputItem, testOnly, cacheData, character)) {
                                DebugLog.CraftLogic.warn("Failed to create output item for: " + output.getOriginalLine());
                                if (this.craftRecipeMonitor != null) {
                                    this.craftRecipeMonitor.warn("Failed to create item: " + outputItem.getFullName());
                                }

                                return true;
                            }

                            if (cacheData.getMostRecentItem() != null) {
                                InventoryItem item = cacheData.getMostRecentItem();
                                items.addItem(item);
                                InventoryItem inheritColorItem = this.getFirstInputItemWithFlag(InputFlag.InheritColor);
                                if (inheritColorItem != null) {
                                    item.setColorRed(inheritColorItem.getColorRed());
                                    item.setColorGreen(inheritColorItem.getColorGreen());
                                    item.setColorBlue(inheritColorItem.getColorBlue());
                                    item.setColor(new Color(inheritColorItem.getColorRed(), inheritColorItem.getColorGreen(), inheritColorItem.getColorBlue()));
                                    item.setCustomColor(true);
                                }

                                InventoryItem inheritModelVariationItem = this.getFirstInputItemWithFlag(InputFlag.InheritModelVariation);
                                if (inheritModelVariationItem != null) {
                                    item.setModelIndex(inheritModelVariationItem.getModelIndex());
                                    if (inheritModelVariationItem.getVisual() != null && item.getVisual() != null) {
                                        item.getVisual().setTextureChoice(inheritModelVariationItem.getVisual().getTextureChoice());
                                    }

                                    item.synchWithVisual();
                                }

                                InventoryItem inheritConditionItem = this.getFirstInputItemWithFlag(InputFlag.InheritCondition);
                                if (inheritConditionItem != null
                                    && !item.hasTag(ItemTag.DONT_INHERIT_CONDITION)
                                    && !output.hasFlag(OutputFlag.DontInheritCondition)) {
                                    item.setConditionFrom(inheritConditionItem);
                                    item.setHaveBeenRepaired(inheritConditionItem.getHaveBeenRepaired());
                                }

                                InventoryItem isHeadPartItem = this.getFirstInputItemWithFlag(InputFlag.IsHeadPart);
                                if (isHeadPartItem != null && item.hasHeadCondition()) {
                                    item.setHeadConditionFromCondition(isHeadPartItem);
                                    if (item.hasSharpness() && isHeadPartItem.hasSharpness()) {
                                        item.setSharpnessFrom(isHeadPartItem);
                                    }

                                    item.setTimesHeadRepaired(this.getFirstInputItemWithFlag(InputFlag.IsHeadPart).getHaveBeenRepaired());
                                }

                                InventoryItem inheritHeadConditionItem = this.getFirstInputItemWithFlag(InputFlag.InheritHeadCondition);
                                if (inheritHeadConditionItem != null && item.hasHeadCondition()) {
                                    item.setHeadCondition(inheritHeadConditionItem.getHeadCondition());
                                    if (item.hasSharpness() && inheritHeadConditionItem.hasSharpness()) {
                                        item.setSharpnessFrom(inheritHeadConditionItem);
                                    }

                                    item.setTimesRepaired(inheritHeadConditionItem.getTimesHeadRepaired());
                                } else if (inheritHeadConditionItem != null && !item.hasHeadCondition()) {
                                    item.setConditionFromHeadCondition(inheritHeadConditionItem);
                                    if (item.hasSharpness() && inheritHeadConditionItem.hasSharpness()) {
                                        item.setSharpnessFrom(inheritHeadConditionItem);
                                    }

                                    item.setTimesRepaired(inheritHeadConditionItem.getTimesHeadRepaired());
                                }

                                InventoryItem inheritEquipped = this.getFirstInputItemWithFlag(InputFlag.InheritEquipped);
                                if (inheritEquipped != null && inheritEquipped.isEquipped()) {
                                    if (character.isPrimaryHandItem(inheritEquipped)) {
                                        character.setPrimaryHandItem(item);
                                    }

                                    if (character.isSecondaryHandItem(inheritEquipped)) {
                                        character.setSecondaryHandItem(item);
                                    }
                                }

                                InventoryItem inheritSharpnessItem = this.getFirstInputItemWithFlag(InputFlag.InheritSharpness);
                                if (inheritSharpnessItem != null && inheritSharpnessItem.hasSharpness() && item.hasSharpness()) {
                                    item.setSharpnessFrom(inheritSharpnessItem);
                                }

                                InventoryItem inheritUsesItem = this.getFirstInputItemWithFlag(InputFlag.InheritUses);
                                if (inheritUsesItem != null) {
                                    if (item instanceof Radio radio) {
                                        radio.getDeviceData().setPower(item.getCurrentUsesFloat());
                                        radio.getDeviceData().setHasBattery(true);
                                    } else {
                                        item.setCurrentUsesFrom(inheritUsesItem);
                                    }
                                }

                                InventoryItem inheritUsesAndEmptyItem = this.getFirstInputItemWithFlag(InputFlag.InheritUsesAndEmpty);
                                if (inheritUsesAndEmptyItem != null) {
                                    item.setCurrentUsesFrom(inheritUsesAndEmptyItem);
                                    inheritUsesAndEmptyItem.setCurrentUsesFloat(0.0F);
                                }

                                if (inheritUsesAndEmptyItem instanceof Radio radio) {
                                    float power = radio.getDeviceData().getPower();
                                    int uses = (int)(item.getMaxUses() * power);
                                    item.setCurrentUses(uses);
                                    radio.getDeviceData().setPower(0.0F);
                                    radio.getDeviceData().setHasBattery(false);
                                    radio.getDeviceData().setIsTurnedOn(false);
                                }

                                InventoryItem inheritFavoriteItem = this.getFirstInputItemWithFlag(InputFlag.InheritFavorite);
                                if (inheritFavoriteItem != null) {
                                    item.setFavorite(inheritFavoriteItem.isFavorite());
                                }

                                InventoryItem inheritAmmunitionItem = this.getFirstInputItemWithFlag(InputFlag.InheritAmmunition);
                                if (item instanceof HandWeapon weapon && inheritAmmunitionItem instanceof HandWeapon handWeapon) {
                                    weapon.inheritAmmunition(handWeapon);
                                }

                                InventoryItem copyClothingItem = this.getFirstInputItemWithFlag(InputFlag.CopyClothing);
                                if (copyClothingItem != null && item.getClothingItem() != null && copyClothingItem.getClothingItem() != null) {
                                    item.copyClothing(copyClothingItem);
                                }

                                InventoryItem inheritWeightItem = this.getFirstInputItemWithFlag(InputFlag.InheritWeight);
                                if (inheritWeightItem != null) {
                                    item.setWeight(inheritWeightItem.getWeight() / 2.0F);
                                    item.setActualWeight(inheritWeightItem.getActualWeight() / 2.0F);
                                }

                                InventoryItem inheritFreezingItem = this.getFirstInputItemWithFlag(InputFlag.InheritFreezingTime);
                                if (item instanceof Food newFoodItem && inheritFreezingItem instanceof Food oldFoodItem) {
                                    newFoodItem.copyFrozenFrom(oldFoodItem);
                                }

                                InventoryItem inheritNameItem = this.getFirstInputItemWithFlag(InputFlag.InheritName);
                                if (inheritNameItem != null) {
                                    item.setName(inheritNameItem.getDisplayName());
                                    item.setCustomName(true);
                                }

                                InventoryItem inheritFoodItem = this.getFirstInputItemWithFlag(InputFlag.InheritFood);
                                if (item instanceof Food newFoodItem && inheritFoodItem instanceof Food oldFoodItem) {
                                    newFoodItem.copyFoodFromSplit(oldFoodItem, output.getIntAmount());
                                }

                                InventoryItem inheritCookedItem = this.getFirstInputItemWithFlag(InputFlag.InheritCooked);
                                if (item instanceof Food newFoodItem && inheritCookedItem instanceof Food oldFoodItem) {
                                    newFoodItem.copyCookedBurntFrom(oldFoodItem);
                                }

                                InventoryItem inheritFoodAgeItem = this.getFirstInputItemWithFlag(InputFlag.InheritFoodAge);
                                if (inheritFoodAgeItem != null && item.isFood()) {
                                    float oldestAge = 0.0F;
                                    Food oldFood = null;

                                    for (int j = 0; j < this.getAllInputItemsWithFlag(InputFlag.InheritFoodAge).size(); j++) {
                                        InventoryItem foodInput = this.getAllInputItemsWithFlag(InputFlag.InheritFoodAge).get(j);
                                        if (foodInput.getAge() > oldestAge) {
                                            oldestAge = foodInput.getAge();
                                            oldFood = (Food)foodInput;
                                            ((Food)item).copyAgeFrom((Food)foodInput);
                                        }

                                        if (oldFood != null) {
                                            ((Food)item).copyAgeFrom(oldFood);
                                        }
                                    }
                                }

                                if (this.craftRecipeMonitor != null) {
                                    this.craftRecipeMonitor.log("created item = " + item.getFullType());
                                }
                            }
                        }

                        return true;
                    }
                }
            }
        } else {
            return false;
        }
    }

    private void collectKeepItems(ItemDataList items, boolean testOnly) {
        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() == ResourceType.Item && input.isMoveToOutputs() && input.getAppliedItemsCount() != 0) {
                for (int j = 0; j < input.getAppliedItemsCount(); j++) {
                    items.addItem(input.getAppliedItem(j), true);
                    if (this.craftRecipeMonitor != null) {
                        this.craftRecipeMonitor.log("item = " + input.getAppliedItem(j).getFullType());
                    }
                }
            }
        }
    }

    private void distributeItemsToResources(List<Resource> outputResources, ItemDataList items, boolean testOnly) {
        if (outputResources != null && items != null && !outputResources.isEmpty() && items.size() != 0) {
            if (this.craftRecipeMonitor != null) {
                this.craftRecipeMonitor.log("items = " + items.size());
                this.craftRecipeMonitor.log("hasUnprocessed = " + items.hasUnprocessed());
            }

            boolean doEmpty = false;
            int doubleSize = outputResources.size() * 2;

            for (int i = 0; i < doubleSize; i++) {
                int index = i;
                if (!items.hasUnprocessed()) {
                    break;
                }

                if (i >= outputResources.size()) {
                    doEmpty = true;
                    index = i - outputResources.size();
                }

                Resource resource = outputResources.get(index);
                if (resource.getType() == ResourceType.Item && !resource.isFull() && (doEmpty || !resource.isEmpty()) && (!doEmpty || resource.isEmpty())) {
                    int resourceCapacity = resource.getFreeItemCapacity();
                    if (this.craftRecipeMonitor != null) {
                        this.craftRecipeMonitor.log("testing resource '" + resource.getId() + "'");
                        this.craftRecipeMonitor.log("capacity = " + resourceCapacity);
                    }

                    for (int j = 0; j < items.size() && resourceCapacity > 0 && items.hasUnprocessed(); j++) {
                        if (!items.isProcessed(j)) {
                            if (!testOnly) {
                                InventoryItem inventoryItem = items.getInventoryItem(j);

                                assert inventoryItem != null;

                                if (this.craftRecipeMonitor != null) {
                                    this.craftRecipeMonitor.log("-> testing item = " + inventoryItem.getFullType());
                                }

                                if (CraftUtil.canResourceFitItem(resource, inventoryItem)) {
                                    if (this.craftRecipeMonitor != null) {
                                        this.craftRecipeMonitor
                                            .success("-> offloaded item '" + inventoryItem.getFullType() + "' to resource: " + resource.getId());
                                    }

                                    resource.offerItem(inventoryItem, true, true, false);
                                    items.setProcessed(j);
                                    resourceCapacity--;
                                }
                            } else {
                                Item item = items.getItem(j);

                                assert item != null;

                                if (this.craftRecipeMonitor != null) {
                                    this.craftRecipeMonitor.log("-> testing item = " + item.getFullName());
                                }

                                if (CraftUtil.canResourceFitItem(resource, item)) {
                                    if (this.craftRecipeMonitor != null) {
                                        this.craftRecipeMonitor.success("-> offloaded item '" + item.getFullName() + "' to resource: " + resource.getId());
                                    }

                                    items.setProcessed(j);
                                    resourceCapacity--;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean createOutputToResources(OutputScript output, List<Resource> resources, boolean testOnly, CraftRecipeData.CacheData cacheData) {
        if (output != null && resources != null && !resources.isEmpty()) {
            Resource resource = null;
            switch (output.getResourceType()) {
                case Fluid:
                    resource = CraftUtil.findResourceOrEmpty(ResourceIO.Output, resources, output.getFluid(), output.getAmount(), null, this.usedResources);
                    break;
                case Energy:
                    resource = CraftUtil.findResourceOrEmpty(ResourceIO.Output, resources, output.getEnergy(), output.getAmount(), null, this.usedResources);
            }

            if (resource != null) {
                boolean success = CraftRecipeManager.createOutputToResource(output, resource, testOnly, cacheData);
                if (success) {
                    this.usedResources.add(resource);
                    if (this.craftRecipeMonitor != null) {
                        this.craftRecipeMonitor.success("created by resource: " + resource.getId());
                    }

                    return true;
                }

                cacheData.softReset();
            }

            return false;
        } else {
            return false;
        }
    }

    public void save(ByteBuffer output) throws IOException {
        ByteBlock block = ByteBlock.Start(output, ByteBlock.Mode.Save);
        output.put((byte)(this.recipe != null ? 1 : 0));
        if (this.recipe != null) {
            GameWindow.WriteString(output, this.recipe.getScriptObjectFullType());
            output.putLong(this.recipe.getScriptVersion());
        }

        output.putDouble(this.elapsedTime);
        output.putInt(this.inputs.size());

        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData data = this.inputs.get(i);
            data.saveInputs(output);
        }

        output.put((byte)(this.modData != null && !this.modData.isEmpty() ? 1 : 0));
        if (this.modData != null && !this.modData.isEmpty()) {
            this.modData.save(output);
        }

        output.put((byte)(this.hasConsumedInputs ? 1 : 0));
        output.putFloat(this.targetVariableInputRatio);
        ByteBlock.End(output, block);
    }

    public boolean load(ByteBuffer input, int WorldVersion, CraftRecipe recipe, boolean recipeInvalidated) throws IOException {
        ByteBlock block = ByteBlock.Start(input, ByteBlock.Mode.Load);
        block.safelyForceSkipOnEnd(true);
        boolean valid = true;
        if (!recipeInvalidated) {
            try {
                if (WorldVersion < 238 || input.get() == 1) {
                    String recipeName = GameWindow.ReadString(input);
                    recipe = ScriptManager.instance.getCraftRecipe(recipeName);
                    long scriptVersion = input.getLong();
                    if (recipe == null || scriptVersion != recipe.getScriptVersion()) {
                        valid = false;
                        DebugLog.General
                            .warn(
                                "CraftRecipe '" + recipeName + "' is null (" + (recipe == null) + ", or has script version mismatch. Cancelling current craft."
                            );
                    }
                }

                this.setRecipe(recipe);
                if (WorldVersion >= 238) {
                    this.elapsedTime = input.getDouble();
                }

                int size = input.getInt();
                if (size != this.inputs.size()) {
                    DebugLog.CraftLogic.warn("Recipe inputs changed or mismatch with saved data.");
                    valid = false;
                } else {
                    for (int i = 0; i < size; i++) {
                        CraftRecipeData.InputScriptData data = this.inputs.get(i);
                        data.loadInputs(input, WorldVersion);
                    }
                }

                if (input.get() == 1) {
                    if (this.modData == null) {
                        this.modData = LuaManager.platform.newTable();
                    }

                    this.modData.load(input, WorldVersion);
                }

                this.hasConsumedInputs = input.get() == 1;
                if (WorldVersion >= 235) {
                    this.targetVariableInputRatio = input.getFloat();
                    this.calculatedVariableInputRatio = this.targetVariableInputRatio;
                }
            } catch (Exception var11) {
                var11.printStackTrace();
                this.setRecipe(null);
                valid = false;
            }
        }

        if (recipeInvalidated || !valid) {
            this.setRecipe(null);
        }

        ByteBlock.End(input, block);
        return valid;
    }

    public KahluaTable getModData() {
        if (this.modData == null) {
            this.modData = LuaManager.platform.newTable();
        }

        return this.modData;
    }

    public String getModelHandOne() {
        return this.getModel(true);
    }

    public String getModelHandTwo() {
        return this.getModel(false);
    }

    private String getModel(boolean isHandOne) {
        if (this.recipe == null) {
            return null;
        } else {
            for (int i = 0; i < this.inputs.size(); i++) {
                CraftRecipeData.InputScriptData input = this.inputs.get(i);
                if (input.inputScript.getResourceType() == ResourceType.Item
                    && (isHandOne && input.inputScript.isProp1() || !isHandOne && input.inputScript.isProp2())
                    && input.getMostRecentItem() != null) {
                    return input.getMostRecentItem().getStaticModel();
                }
            }

            if (this.recipe == null || this.recipe.getTimedActionScript() == null) {
                return null;
            } else {
                return isHandOne ? this.recipe.getTimedActionScript().getProp1() : this.recipe.getTimedActionScript().getProp2();
            }
        }
    }

    public ArrayList<InventoryItem> getAllConsumedItems() {
        return this.getAllConsumedItems(new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllRecordedConsumedItems() {
        return this.getAllRecordedConsumedItems(new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllConsumedItems(ArrayList<InventoryItem> list) {
        return this.getAllConsumedItems(list, false);
    }

    public ArrayList<InventoryItem> getAllRecordedConsumedItems(ArrayList<InventoryItem> list) {
        return this.getAllConsumedItems(list, false, true);
    }

    public ArrayList<InventoryItem> getAllConsumedItems(ArrayList<InventoryItem> list, boolean includeKeep) {
        return this.getAllConsumedItems(list, includeKeep, false);
    }

    public ArrayList<InventoryItem> getAllConsumedItems(ArrayList<InventoryItem> list, boolean includeKeep, boolean onlyRecorded) {
        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() == ResourceType.Item
                && (!input.inputScript.isKeep() || includeKeep)
                && (!onlyRecorded || input.inputScript.isRecordInput())) {
                input.addAppliedItemsToList(list);
            }
        }

        return list;
    }

    public ArrayList<InventoryItem> getAllKeepInputItems() {
        return this.getAllKeepInputItems(new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllKeepInputItems(ArrayList<InventoryItem> list) {
        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() == ResourceType.Item && input.inputScript.isKeep()) {
                input.addAppliedItemsToList(list);
            }
        }

        return list;
    }

    public ArrayList<InventoryItem> getAllInputItemsWithFlag(InputFlag flag) {
        return this.getAllInputItemsWithFlag(flag.name());
    }

    public ArrayList<InventoryItem> getAllInputItemsWithFlag(String flag) {
        ArrayList<InventoryItem> list = new ArrayList<>();

        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() == ResourceType.Item) {
                InputFlag flag2 = InputFlag.valueOf(flag);
                if (input.getInputScript().hasFlag(flag2)) {
                    input.addAppliedItemsToList(list);
                }
            }
        }

        return list;
    }

    public ArrayList<InventoryItem> getInputItems(Integer index) {
        if (this.inputs.get(index) != null) {
            CraftRecipeData.InputScriptData input = this.inputs.get(index);
            if (input.inputScript.getResourceType() == ResourceType.Item) {
                return input.inputItems;
            }
        }

        return null;
    }

    public InventoryItem getFirstInputItemWithFlag(InputFlag flag) {
        return this.getFirstInputItemWithFlag(flag.name());
    }

    public InventoryItem getFirstInputItemWithFlag(String flag) {
        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() == ResourceType.Item) {
                InputFlag flag2 = InputFlag.valueOf(flag);
                if (input.getInputScript().hasFlag(flag2)) {
                    InventoryItem item = input.getFirstAppliedItem();
                    if (item != null) {
                        return item;
                    }
                }
            }
        }

        return null;
    }

    public InventoryItem getFirstInputItemWithTag(ItemTag itemTag) {
        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() == ResourceType.Item) {
                InventoryItem item = input.getFirstAppliedItem();
                if (item != null && item.hasTag(itemTag)) {
                    return item;
                }
            }
        }

        return null;
    }

    public ArrayList<InventoryItem> getAllInputItems() {
        ArrayList<InventoryItem> list = new ArrayList<>();

        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() == ResourceType.Item) {
                input.addAppliedItemsToList(list);
            }
        }

        return list;
    }

    public HashSet<String> getAppliedInputItemTypes(HashSet<String> appliedItemTypes) {
        ArrayList<InventoryItem> currentInputs = this.getAllInputItems();

        for (int i = 0; i < currentInputs.size(); i++) {
            appliedItemTypes.add(currentInputs.get(i).getFullType());
        }

        return appliedItemTypes;
    }

    public ArrayList<InventoryItem> getAllDestroyInputItems() {
        ArrayList<InventoryItem> list = new ArrayList<>();

        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() == ResourceType.Item && input.isDestroy()) {
                input.addAppliedItemsToList(list);
            }
        }

        return list;
    }

    public ArrayList<InventoryItem> getAllPutBackInputItems() {
        ArrayList<InventoryItem> list = new ArrayList<>();

        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() == ResourceType.Item && !input.getInputScript().dontPutBack()) {
                input.addAppliedItemsToList(list);
            }
        }

        return list;
    }

    public ArrayList<InventoryItem> getAllNotKeepInputItems() {
        ArrayList<InventoryItem> list = new ArrayList<>();

        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData input = this.inputs.get(i);
            if (input.inputScript.getResourceType() == ResourceType.Item && !input.inputScript.isKeep()) {
                input.addAppliedItemsToList(list);
            }
        }

        return list;
    }

    public InventoryItem getFirstCreatedItem() {
        return !this.getAllCreatedItems().isEmpty() ? this.getAllCreatedItems().get(0) : null;
    }

    public ArrayList<InventoryItem> getAllCreatedItems() {
        return this.getAllCreatedItems(new ArrayList<>());
    }

    public ArrayList<InventoryItem> getAllCreatedItems(ArrayList<InventoryItem> list) {
        for (int i = 0; i < this.outputs.size(); i++) {
            CraftRecipeData.OutputScriptData output = this.outputs.get(i);
            if (output.outputScript.getResourceType() == ResourceType.Item) {
                output.addAppliedItemsToList(list);
            }
        }

        return list;
    }

    public FluidSample getFirstInputFluidWithFlag(InputFlag flag) {
        return this.getFirstInputFluidWithFlag(flag.name());
    }

    public FluidSample getFirstInputFluidWithFlag(String flag) {
        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData input = this.inputs.get(i);
            if (input.getInputScript().getConsumeFromItemScript() != null) {
                InputFlag flag2 = InputFlag.valueOf(flag);
                InputScript childScript = input.getInputScript().getConsumeFromItemScript();
                if (childScript.hasFlag(flag2)) {
                    CraftRecipeData.InputScriptData data = this.getDataForInputScript(childScript);
                    FluidSample sample = data.fluidSample;
                    if (sample != null) {
                        return sample;
                    }
                }
            }
        }

        return null;
    }

    public int getAllViableItemsCount() {
        return this.allViableItems.size();
    }

    public InventoryItem getViableItem(int index) {
        return this.allViableItems.get(index);
    }

    public int getAllViableResourcesCount() {
        return this.allViableResources.size();
    }

    public Resource getViableResource(int index) {
        return this.allViableResources.get(index);
    }

    private void destroyAllSurvivingDestroyInputs() {
        ArrayList<InventoryItem> list = this.getAllDestroyInputItems();

        for (int i = 0; i < list.size(); i++) {
            InventoryItem item = list.get(i);
            if (item != null) {
                DebugLog.CraftLogic.debugln("Destroying surviving destroy input item " + item.getFullType());
                ItemUser.RemoveItem(item);
            }
        }
    }

    public boolean isVariableAmount() {
        return this.inputs.stream().anyMatch(inputScriptData -> inputScriptData.inputScript.isVariableAmount());
    }

    public float getVariableInputRatio() {
        return this.calculatedVariableInputRatio == Float.MAX_VALUE ? 1.0F : this.calculatedVariableInputRatio;
    }

    public void setTargetVariableInputRatio(float target) {
        this.targetVariableInputRatio = target;
    }

    public void clearTargetVariableInputRatio() {
        this.targetVariableInputRatio = Float.MAX_VALUE;
    }

    public void addOverfilledResource(InputScript input, HashMap<Resource, ArrayList<InventoryItem>> resources) {
        this.variableInputOverfilledResources.put(input, resources);
    }

    public float getCalculatedVariableInputRatio() {
        return this.calculatedVariableInputRatio;
    }

    public void setCalculatedVariableInputRatio(float value) {
        this.calculatedVariableInputRatio = value;
    }

    public ArrayList<InventoryItem> getManualInputsFor(InputScript inputScript, ArrayList<InventoryItem> list) {
        if (this.getRecipe() != null && this.getRecipe().containsIO(inputScript)) {
            CraftRecipeData.InputScriptData data = this.getDataForInputScript(inputScript);
            if (inputScript.getResourceType() == ResourceType.Item) {
                data.getManualInputItems(list);

                for (InventoryItem inventoryItem : list) {
                    DebugLog.CraftLogic.println("get m-input: " + inventoryItem.getFullType());
                }
            }

            return list;
        } else {
            return list;
        }
    }

    public void clearManualInputs() {
        this.clearManualInputs(null);
    }

    public void clearManualInputs(CraftRecipeData.InputScriptData input) {
        for (int i = 0; i < this.inputs.size(); i++) {
            CraftRecipeData.InputScriptData data = this.inputs.get(i);
            if (input == null || input == data) {
                while (data.getLastInputItem() != null) {
                    data.removeInputItem(data.getLastInputItem());
                }
            }
        }
    }

    public boolean setManualInputsFor(InputScript inputScript, ArrayList<InventoryItem> list) {
        if (this.getRecipe() != null && this.getRecipe().containsIO(inputScript)) {
            CraftRecipeData.InputScriptData data = this.getDataForInputScript(inputScript);
            if (inputScript.getResourceType() != ResourceType.Item) {
                return false;
            } else {
                while (data.getLastInputItem() != null) {
                    data.removeInputItem(data.getLastInputItem());
                }

                for (int i = 0; i < list.size(); i++) {
                    InventoryItem inputItem = list.get(i);
                    if ((inputItem.getContainer() != null || inputItem.getWorldItem() != null && inputItem.getWorldItem().getWorldObjectIndex() != -1)
                        && !this.containsInputItem(inputItem)
                        && data.addInputItem(inputItem)) {
                        DebugLog.CraftLogic.println("add m-input: " + list.get(i).getFullType());
                    }
                }

                return data.isInputItemsSatisfied();
            }
        } else {
            return false;
        }
    }

    public void populateInputs(List<InventoryItem> inputItems, List<Resource> resources, boolean clearExisting) {
        if (this.getRecipe() != null) {
            ArrayList<InputScript> inputScripts = this.getRecipe().getInputs();

            for (int i = 0; i < inputScripts.size(); i++) {
                InputScript inputScript = inputScripts.get(i);
                CraftRecipeData.InputScriptData inputScriptData = this.getDataForInputScript(inputScript);
                if (clearExisting) {
                    while (inputScriptData.getLastInputItem() != null) {
                        inputScriptData.removeInputItem(inputScriptData.getLastInputItem());
                    }
                }

                if (!inputScriptData.isInputItemsSatisfied()) {
                    for (int j = 0; j < inputItems.size(); j++) {
                        this.offerInputItem(inputScript, inputItems.get(j));
                        if (inputScriptData.isInputItemsSatisfied()) {
                            break;
                        }
                    }
                }
            }
        }
    }

    public void setEatPercentage(int percentage) {
        percentage = Math.min(percentage, 100);
        percentage = Math.max(percentage, 0);
        this.eatPercentage = percentage;
    }

    public int getEatPercentage() {
        return this.eatPercentage;
    }

    public double getElapsedTime() {
        return this.elapsedTime;
    }

    public void setElapsedTime(double elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public boolean isFinished() {
        return this.getElapsedTime() >= this.getRecipe().getTime();
    }

    @UsedFromLua
    public abstract static class CacheData {
        protected InventoryItem mostRecentItem;
        private final ArrayList<InventoryItem> appliedItems = new ArrayList<>();
        private boolean moveToOutputs;
        protected float usesConsumed;
        protected float fluidConsumed;
        protected float energyConsumed;
        protected FluidSample fluidSample = FluidSample.Alloc();
        protected FluidConsume fluidConsume = FluidConsume.Alloc();
        protected float usesCreated;
        protected float fluidCreated;
        protected float energyCreated;
        protected boolean cachedCanConsume;

        protected void addAppliedItem(InventoryItem inventoryItem) {
            assert !this.appliedItems.contains(inventoryItem) : "Item already added to applied list.";

            this.appliedItems.add(inventoryItem);
            this.mostRecentItem = inventoryItem;
        }

        public int getAppliedItemsCount() {
            return this.appliedItems.size();
        }

        public boolean hasAppliedItem(InventoryItem item) {
            return this.appliedItems.contains(item);
        }

        public boolean hasAppliedItemType(Item item) {
            for (int i = 0; i < this.appliedItems.size(); i++) {
                if (this.appliedItems.get(i).getScriptItem() == item) {
                    return true;
                }
            }

            return false;
        }

        public InventoryItem getMostRecentItem() {
            return this.mostRecentItem;
        }

        protected void setMostRecentItemNull() {
            this.mostRecentItem = null;
        }

        public InventoryItem getAppliedItem(int index) {
            return this.appliedItems.get(index);
        }

        public InventoryItem getFirstAppliedItem() {
            return this.appliedItems.isEmpty() ? null : this.appliedItems.get(0);
        }

        public void addAppliedItemsToList(ArrayList<InventoryItem> items) {
            PZArrayUtil.addAll(items, this.appliedItems);
        }

        protected abstract CraftRecipeData getRecipeData();

        protected void clearCache() {
            this.moveToOutputs = false;
            this.mostRecentItem = null;
            this.appliedItems.clear();
            this.usesConsumed = 0.0F;
            this.fluidConsumed = 0.0F;
            this.energyConsumed = 0.0F;
            this.fluidSample.clear();
            this.fluidConsume.clear();
            this.usesCreated = 0.0F;
            this.fluidCreated = 0.0F;
            this.energyCreated = 0.0F;
        }

        public boolean isMoveToOutputs() {
            return this.moveToOutputs;
        }

        public void setMoveToOutputs(boolean b) {
            this.moveToOutputs = b;
        }

        protected void softReset() {
            this.softResetInput();
            this.softResetOutput();
        }

        protected void softResetInput() {
            this.mostRecentItem = null;
            if (!this.appliedItems.isEmpty()) {
                this.appliedItems.clear();
            }

            this.fluidSample.clear();
            this.fluidConsume.clear();
            this.usesConsumed = 0.0F;
            this.fluidConsumed = 0.0F;
            this.energyConsumed = 0.0F;
        }

        protected void softResetOutput() {
            if (!this.appliedItems.isEmpty()) {
                this.appliedItems.clear();
            }

            this.usesCreated = 0.0F;
            this.fluidCreated = 0.0F;
            this.energyCreated = 0.0F;
        }

        protected void saveInputs(ByteBuffer output) throws IOException {
            output.put((byte)(this.moveToOutputs ? 1 : 0));
            output.putFloat(this.usesConsumed);
            output.putFloat(this.fluidConsumed);
            output.putFloat(this.energyConsumed);
            FluidSample.Save(this.fluidSample, output);
            FluidConsume.Save(this.fluidConsume, output);
            if (this.appliedItems.size() == 1) {
                CompressIdenticalItems.save(output, this.appliedItems.get(0));
            } else {
                CompressIdenticalItems.save(output, this.appliedItems, null);
            }

            CompressIdenticalItems.save(output, this.mostRecentItem);
            output.put((byte)(this.cachedCanConsume ? 1 : 0));
        }

        protected void loadInputs(ByteBuffer input, int WorldVersion) throws IOException {
            this.moveToOutputs = input.get() == 1;
            this.usesConsumed = input.getFloat();
            this.fluidConsumed = input.getFloat();
            this.energyConsumed = input.getFloat();
            FluidSample.Load(this.fluidSample, input, WorldVersion);
            FluidConsume.Load(this.fluidConsume, input, WorldVersion);
            this.appliedItems.clear();
            CompressIdenticalItems.load(input, WorldVersion, this.appliedItems, null);
            ArrayList<InventoryItem> mostRecentItemArray = new ArrayList<>();
            CompressIdenticalItems.load(input, WorldVersion, mostRecentItemArray, null);
            if (!mostRecentItemArray.isEmpty()) {
                this.mostRecentItem = mostRecentItemArray.get(0);
            }

            this.cachedCanConsume = input.get() == 1;
        }
    }

    @UsedFromLua
    public static class InputScriptData extends CraftRecipeData.CacheData {
        private CraftRecipeData recipeData;
        private InputScript inputScript;
        private final ArrayList<InventoryItem> inputItems = new ArrayList<>();
        private static final ArrayDeque<CraftRecipeData.InputScriptData> pool = new ArrayDeque<>();

        private static CraftRecipeData.InputScriptData Alloc(CraftRecipeData recipeData, InputScript inputScript) {
            CraftRecipeData.InputScriptData data = pool.poll();
            if (data == null) {
                data = new CraftRecipeData.InputScriptData();
            }

            data.recipeData = recipeData;
            data.inputScript = inputScript;
            return data;
        }

        private static void Release(CraftRecipeData.InputScriptData data) {
        }

        @Override
        protected CraftRecipeData getRecipeData() {
            return this.recipeData;
        }

        private void reset() {
            this.clearCache();
            this.inputScript = null;
            this.inputItems.clear();
            this.recipeData = null;
            this.cachedCanConsume = false;
        }

        public InputScript getInputScript() {
            return this.inputScript;
        }

        public boolean isCachedCanConsume() {
            return this.cachedCanConsume;
        }

        public void getManualInputItems(ArrayList<InventoryItem> list) {
            list.addAll(this.inputItems);
        }

        public int getInputItemCount() {
            return this.inputItems.size();
        }

        public int getInputItemUses() {
            if (this.getInputScript().isItemCount()) {
                return this.getInputItemCount();
            } else {
                int totalUses = 0;

                for (int i = 0; i < this.inputItems.size(); i++) {
                    if (this.inputItems.get(i) != null) {
                        totalUses += this.inputItems.get(i).getCurrentUses();
                    }
                }

                return totalUses;
            }
        }

        public float getInputItemFluidUses() {
            float uses = 0.0F;

            for (int i = 0; i < this.inputItems.size(); i++) {
                if (this.inputItems.get(i) != null) {
                    FluidContainer fluidContainer = this.inputItems.get(i).getFluidContainer();
                    if (fluidContainer != null) {
                        uses += fluidContainer.getAmount();
                    }
                }
            }

            return uses;
        }

        public InventoryItem getFirstInputItem() {
            return !this.inputItems.isEmpty() ? this.inputItems.get(0) : null;
        }

        public InventoryItem getLastInputItem() {
            return !this.inputItems.isEmpty() ? this.inputItems.get(this.inputItems.size() - 1) : null;
        }

        public boolean isInputItemsSatisfied() {
            return this.inputScript.getResourceType() == ResourceType.Item && !this.inputItems.isEmpty()
                ? this.recipeData.consumeInputFromItems(this.inputScript, this.inputItems, true, null, true, null)
                : false;
        }

        public boolean isInputItemsSatisifiedToMaximum() {
            if (this.inputScript.getResourceType() == ResourceType.Item
                && !this.inputItems.isEmpty()
                && this.recipeData.consumeInputFromItems(this.inputScript, this.inputItems, true, null, true, null)) {
                int usedItems = this.recipeData.usedItems.size();
                int overfilledItems = this.recipeData.variableInputOverfilledItems.containsKey(this.inputScript)
                    ? this.recipeData.variableInputOverfilledItems.get(this.inputScript).size()
                    : 0;
                int targetFillAmount = (int)Math.min(
                    this.inputScript.getAmount() * this.recipeData.targetVariableInputRatio, (float)this.inputScript.getIntMaxAmount()
                );
                return usedItems + overfilledItems >= targetFillAmount;
            } else {
                return false;
            }
        }

        public boolean acceptsInputItem(InventoryItem inventoryItem) {
            if (this.inputScript.getResourceType() == ResourceType.Item) {
                return !this.inputItems.isEmpty() && this.inputItems.contains(inventoryItem)
                    ? false
                    : CraftRecipeManager.consumeInputItem(this.inputScript, inventoryItem, true, null, null);
            } else {
                return false;
            }
        }

        public boolean addInputItem(InventoryItem inventoryItem) {
            if (this.inputScript.getResourceType() == ResourceType.Item) {
                if (this.acceptsInputItem(inventoryItem) && (!this.isInputItemsSatisfied() || !this.isInputItemsSatisifiedToMaximum())) {
                    this.inputItems.add(inventoryItem);
                    return true;
                }
            } else {
                DebugLog.CraftLogic.warn("input script does not accept items, line=" + this.inputScript.getOriginalLine());
            }

            return false;
        }

        public boolean removeInputItem(InventoryItem item) {
            return this.inputItems.remove(item);
        }

        public void verifyInputItems(ArrayList<InventoryItem> playerItems) {
            for (int i = this.inputItems.size() - 1; i >= 0; i--) {
                InventoryItem inventoryItem = this.inputItems.get(i);
                boolean accepts = false;
                if (this.inputScript.getResourceType() == ResourceType.Item) {
                    accepts = CraftRecipeManager.consumeInputItem(this.inputScript, inventoryItem, true, null, null);
                }

                if (!this.inputScript.isKeep() || !playerItems.contains(inventoryItem) || !accepts) {
                    DebugLog.CraftLogic
                        .println(
                            " :: REMOVING ITEM: "
                                + inventoryItem.getFullType()
                                + " [0]="
                                + (!this.inputScript.isKeep() || !this.inputScript.isTool())
                                + ", [1]="
                                + !playerItems.contains(inventoryItem)
                                + ", [2]="
                                + !accepts
                        );
                    this.inputItems.remove(i);
                }
            }
        }

        public boolean isDestroy() {
            return this.getInputScript().isDestroy();
        }
    }

    @UsedFromLua
    public static class OutputScriptData extends CraftRecipeData.CacheData {
        private static final ArrayDeque<CraftRecipeData.OutputScriptData> pool = new ArrayDeque<>();
        private CraftRecipeData recipeData;
        private OutputScript outputScript;

        private static CraftRecipeData.OutputScriptData Alloc(CraftRecipeData recipeData, OutputScript outputScript) {
            CraftRecipeData.OutputScriptData data = pool.poll();
            if (data == null) {
                data = new CraftRecipeData.OutputScriptData();
            }

            data.recipeData = recipeData;
            data.outputScript = outputScript;
            return data;
        }

        private static void Release(CraftRecipeData.OutputScriptData data) {
        }

        @Override
        protected CraftRecipeData getRecipeData() {
            return this.recipeData;
        }

        public OutputScript getOutputScript() {
            return this.outputScript;
        }
    }
}
