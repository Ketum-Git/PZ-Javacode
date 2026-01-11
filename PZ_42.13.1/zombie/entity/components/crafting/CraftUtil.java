// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.entity.MetaEntity;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceEnergy;
import zombie.entity.components.resources.ResourceFluid;
import zombie.entity.components.resources.ResourceIO;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.energy.Energy;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.weather.ClimateManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.Item;

@UsedFromLua
public class CraftUtil {
    private static final ConcurrentLinkedDeque<ArrayList<Resource>> resource_list_pool = new ConcurrentLinkedDeque<>();

    public static ArrayList<Resource> AllocResourceList() {
        ArrayList<Resource> list = resource_list_pool.poll();
        if (list == null) {
            list = new ArrayList<>();
        }

        return list;
    }

    public static void ReleaseResourceList(ArrayList<Resource> list) {
        if (list != null) {
            list.clear();

            assert !Core.debug || !resource_list_pool.contains(list) : "Object already in pool.";

            resource_list_pool.offer(list);
        }
    }

    public static boolean canItemsStack(InventoryItem item, InventoryItem other) {
        return canItemsStack(item, other, false);
    }

    public static boolean canItemsStack(InventoryItem item, InventoryItem other, boolean nullReturn) {
        if (item == null || other == null) {
            return nullReturn;
        } else {
            return item == other ? false : item.getRegistry_id() == other.getRegistry_id();
        }
    }

    public static boolean canItemsStack(Item item, Item other, boolean nullReturn) {
        if (item == null || other == null) {
            return nullReturn;
        } else {
            return item == other ? true : item.getRegistry_id() == other.getRegistry_id();
        }
    }

    public static Resource findResourceOrEmpty(
        ResourceIO resourceIO, List<Resource> outputResources, InventoryItem item, int count, Resource ignoreResource, HashSet<Resource> ignoreSet
    ) {
        return findResourceOrEmpty(resourceIO, outputResources, item.getScriptItem(), count, ignoreResource, ignoreSet);
    }

    public static Resource findResourceOrEmpty(
        ResourceIO resourceIO, List<Resource> resources, Item item, int count, Resource ignoreResource, HashSet<Resource> ignoreSet
    ) {
        Resource bestFit = null;
        if (resources != null && !resources.isEmpty()) {
            for (int i = 0; i < resources.size(); i++) {
                Resource resource = resources.get(i);
                if ((ignoreResource == null || ignoreResource != resource) && (ignoreSet == null || !ignoreSet.contains(resource))) {
                    if (resourceIO != ResourceIO.Any && resource.getIO() != ResourceIO.Output) {
                        if (Core.debug) {
                            DebugLog.General.warn("resource passed does not match selected IO type.");
                        }
                    } else if (resource.getType() == ResourceType.Item && !resource.isFull() && (count <= 0 || resource.getFreeItemCapacity() >= count)) {
                        if (resource.isEmpty() && bestFit == null) {
                            bestFit = resource;
                        }

                        if (resource.canStackItem(item)
                            && (bestFit == null || bestFit.isEmpty() || bestFit.getFreeItemCapacity() >= resource.getFreeItemCapacity())) {
                            bestFit = resource;
                        }
                    }
                }
            }
        }

        return bestFit;
    }

    public static boolean canResourceFitItem(Resource resource, InventoryItem item) {
        return canResourceFitItem(resource, item, 1, null, null);
    }

    public static boolean canResourceFitItem(Resource resource, InventoryItem item, int count) {
        return canResourceFitItem(resource, item, count, null, null);
    }

    public static boolean canResourceFitItem(Resource resource, InventoryItem item, int count, Resource ignoreResource, HashSet<Resource> ignoreSet) {
        if (resource == null || resource.getType() != ResourceType.Item || resource.isFull()) {
            return false;
        } else if (ignoreResource != null && ignoreResource == resource) {
            return false;
        } else if (ignoreSet != null && ignoreSet.contains(resource)) {
            return false;
        } else {
            return count > 0 && resource.getFreeItemCapacity() < count ? false : resource.canStackItem(item);
        }
    }

    public static boolean canResourceFitItem(Resource resource, Item item) {
        return canResourceFitItem(resource, item, 1, null, null);
    }

    public static boolean canResourceFitItem(Resource resource, Item item, int count) {
        return canResourceFitItem(resource, item, count, null, null);
    }

    public static boolean canResourceFitItem(Resource resource, Item item, int count, Resource ignoreResource, HashSet<Resource> ignoreSet) {
        if (resource == null || resource.getType() != ResourceType.Item || resource.isFull()) {
            return false;
        } else if (ignoreResource != null && ignoreResource == resource) {
            return false;
        } else if (ignoreSet != null && ignoreSet.contains(resource)) {
            return false;
        } else {
            return count > 0 && resource.getFreeItemCapacity() < count ? false : resource.canStackItem(item);
        }
    }

    public static Resource findResourceOrEmpty(
        ResourceIO resourceIO, List<Resource> resources, Fluid fluid, float amount, Resource ignoreResource, HashSet<Resource> ignoreSet
    ) {
        float bestRatio = 0.0F;
        Resource secondary = null;
        Resource bestFit = null;
        if (resources != null && !resources.isEmpty()) {
            for (int i = 0; i < resources.size(); i++) {
                Resource resource = resources.get(i);
                if ((ignoreResource == null || ignoreResource != resource) && (ignoreSet == null || !ignoreSet.contains(resource))) {
                    if (resourceIO != ResourceIO.Any && resource.getIO() != ResourceIO.Output) {
                        if (Core.debug) {
                            DebugLog.General.warn("resource passed does not match selected IO type.");
                        }
                    } else if (resource.getType() == ResourceType.Fluid
                        && !resource.isFull()
                        && (!(amount > 0.0F) || !(resource.getFreeFluidCapacity() < amount))) {
                        if (resource.isEmpty() && secondary == null) {
                            secondary = resource;
                        }

                        ResourceFluid resourceFluid = (ResourceFluid)resource;
                        if (resourceFluid.getFluidContainer().canAddFluid(fluid)) {
                            if (resourceFluid.getFluidContainer().isPureFluid(fluid)) {
                                if (bestFit != null && bestFit.getFreeFluidCapacity() < resource.getFreeFluidCapacity()) {
                                    continue;
                                }

                                bestFit = resource;
                            }

                            if (bestFit == null && resourceFluid.getFluidContainer().contains(fluid)) {
                                float ratio = resourceFluid.getFluidContainer().getRatioForFluid(fluid);
                                if (!(ratio < bestRatio)) {
                                    bestRatio = ratio;
                                    secondary = resource;
                                }
                            }
                        }
                    }
                }
            }
        }

        return bestFit != null ? bestFit : secondary;
    }

    public static Resource findResourceOrEmpty(
        ResourceIO resourceIO, List<Resource> resources, Energy energy, float amount, Resource ignoreResource, HashSet<Resource> ignoreSet
    ) {
        Resource bestFit = null;
        if (resources != null && !resources.isEmpty()) {
            for (int i = 0; i < resources.size(); i++) {
                Resource resource = resources.get(i);
                if ((ignoreResource == null || ignoreResource != resource) && (ignoreSet == null || !ignoreSet.contains(resource))) {
                    if (resourceIO != ResourceIO.Any && resource.getIO() != ResourceIO.Output) {
                        if (Core.debug) {
                            DebugLog.General.warn("resource passed does not match selected IO type.");
                        }
                    } else if (resource.getType() == ResourceType.Energy
                        && !resource.isFull()
                        && (!(amount > 0.0F) || !(resource.getFreeEnergyCapacity() < amount))) {
                        ResourceEnergy resourceEnergy = (ResourceEnergy)resource;
                        if (resourceEnergy.getEnergy() == energy) {
                            if (resource.isEmpty() && bestFit == null) {
                                bestFit = resource;
                            }

                            if (bestFit == null || bestFit.isEmpty() || !(bestFit.getFreeEnergyCapacity() < resource.getFreeEnergyCapacity())) {
                                bestFit = resource;
                            }
                        }
                    }
                }
            }
        }

        return bestFit;
    }

    public static CraftRecipeMonitor debugCanStart(
        IsoPlayer player, CraftRecipeData craftTestData, List<CraftRecipe> recipes, List<Resource> inputs, List<Resource> outputs, CraftRecipeMonitor monitor
    ) {
        try {
            craftTestData.setMonitor(monitor);
            canStart(craftTestData, recipes, inputs, outputs, monitor);
            craftTestData.setMonitor(null);
            return monitor.seal();
        } catch (Exception var10) {
            var10.printStackTrace();
        } finally {
            craftTestData.setMonitor(null);
        }

        return null;
    }

    public static boolean canStart(CraftRecipeData craftTestData, List<CraftRecipe> recipes, List<Resource> inputs, List<Resource> outputs) {
        return canStart(craftTestData, recipes, inputs, outputs, null);
    }

    public static boolean canStart(
        CraftRecipeData craftTestData, List<CraftRecipe> recipes, List<Resource> inputs, List<Resource> outputs, CraftRecipeMonitor _m
    ) {
        if (_m != null) {
            _m.log("starting craftProcessor 'can start test'...");
        }

        CraftRecipe recipe = getPossibleRecipe(craftTestData, recipes, inputs, outputs, _m);
        if (_m != null) {
            if (recipe != null) {
                _m.success("selected recipe: " + recipe.getScriptObjectFullType());
                _m.setRecipe(recipe);
                _m.logRecipe(recipe, false);
            } else {
                _m.warn("no recipe can be performed for this craftProcessor");
            }
        }

        return recipe != null;
    }

    public static boolean canPerformRecipe(CraftRecipe recipe, CraftRecipeData craftTestData, List<Resource> inputs, List<Resource> outputs) {
        return canPerformRecipe(recipe, craftTestData, inputs, outputs, null);
    }

    public static boolean canPerformRecipe(
        CraftRecipe recipe, CraftRecipeData craftTestData, List<Resource> inputs, List<Resource> outputs, CraftRecipeMonitor _m
    ) {
        if (recipe != null && craftTestData != null && inputs != null) {
            craftTestData.setRecipe(recipe);
            if (craftTestData.canConsumeInputs(inputs) && (outputs == null || craftTestData.canCreateOutputs(outputs))) {
                if (_m != null) {
                    _m.log("Input and Output passed, Calling LuaTest...");
                }

                boolean success = craftTestData.luaCallOnTest();
                if (_m != null) {
                    if (success) {
                        _m.success("LuaTest: OK");
                    } else {
                        _m.warn("LuaTest: FAILED");
                    }
                }

                return success;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static CraftRecipe getPossibleRecipe(CraftRecipeData craftTestData, List<CraftRecipe> recipes, List<Resource> inputs, List<Resource> outputs) {
        return getPossibleRecipe(craftTestData, recipes, inputs, outputs, null);
    }

    public static CraftRecipe getPossibleRecipe(
        CraftRecipeData craftTestData, List<CraftRecipe> recipes, List<Resource> inputs, List<Resource> outputs, CraftRecipeMonitor _m
    ) {
        if (craftTestData != null && recipes != null && inputs != null) {
            if (_m != null) {
                _m.log("Get possible recipe...");
                _m.open();
            }

            for (int i = 0; i < recipes.size(); i++) {
                CraftRecipe recipe = recipes.get(i);
                if (_m != null) {
                    _m.log("[" + i + "] test recipe = " + recipe.getScriptObjectFullType());
                }

                if (canPerformRecipe(recipe, craftTestData, inputs, outputs, _m)) {
                    if (_m != null) {
                        _m.close();
                    }

                    return recipe;
                }
            }

            if (_m != null) {
                _m.close();
            }

            return null;
        } else {
            return null;
        }
    }

    public static float getEntityTemperature(GameEntity entity) {
        float ambientTemp = ClimateManager.getInstance().getTemperature();
        if (entity instanceof MetaEntity) {
            return ambientTemp;
        } else {
            IsoGridSquare square = entity.getSquare();
            return square == null ? ambientTemp : ClimateManager.getInstance().getAirTemperatureForSquare(square);
        }
    }
}
