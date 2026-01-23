// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting;

import java.util.ArrayList;
import java.util.List;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.components.resources.Resources;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.components.crafting.CraftRecipe;

public class DryingLogicSystem extends EngineSystem {
    private EntityBucket dryingLogicEntities;
    private final List<Resource> tempSlotInputResources = new ArrayList<>();
    private final List<Resource> tempSlotOutputResources = new ArrayList<>();
    private final CraftRecipeData slotCraftTestData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
    private final CraftRecipeData slotCraftData = CraftRecipeData.Alloc(CraftMode.Automation, true, false, true, false);
    private final CraftRecipeMonitor debugMonitor = CraftRecipeMonitor.Create();

    public DryingLogicSystem(int updatePriority) {
        super(false, true, updatePriority);
        this.debugMonitor.setPrintToConsole(true);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.dryingLogicEntities = engine.getBucket(Family.all(ComponentType.DryingLogic, ComponentType.Resources).get());
    }

    @Override
    public void removedFromEngine(Engine engine) {
    }

    private boolean isValidEntity(GameEntity entity) {
        return entity.isEntityValid() && entity.isValidEngineEntity();
    }

    @Override
    public void updateSimulation() {
        if (!GameClient.client) {
            ImmutableArray<GameEntity> entities = this.dryingLogicEntities.getEntities();
            if (entities.size() != 0) {
                for (int i = 0; i < entities.size(); i++) {
                    GameEntity entity = entities.get(i);
                    if (this.isValidEntity(entity)) {
                        DryingLogic dryingLogic = entity.getComponent(ComponentType.DryingLogic);
                        Resources resources = entity.getComponent(ComponentType.Resources);
                        if (dryingLogic.isValid() && resources.isValid()) {
                            ResourceGroup fuelInputResources = resources.getResourceGroup(dryingLogic.getFuelInputsGroupName());
                            ResourceGroup fuelOutputResources = resources.getResourceGroup(dryingLogic.getFuelOutputsGroupName());
                            ResourceGroup inputResources = resources.getResourceGroup(dryingLogic.getDryingInputsGroupName());
                            ResourceGroup outputResources = resources.getResourceGroup(dryingLogic.getDryingOutputsGroupName());
                            if (this.verifyDryingLogicSlots(dryingLogic, inputResources, outputResources) && dryingLogic.getSlotSize() != 0) {
                                this.updateDryingLogic(dryingLogic, fuelInputResources, fuelOutputResources);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean verifyDryingLogicSlots(DryingLogic logic, ResourceGroup inputs, ResourceGroup outputs) {
        if (inputs != null && !inputs.getResources().isEmpty() && outputs != null && !outputs.getResources().isEmpty()) {
            if (inputs.getResources().size() != outputs.getResources().size()) {
                return false;
            } else {
                if (logic.getSlotSize() != 0 && logic.getSlotSize() != inputs.getResources().size()) {
                    logic.clearSlots();
                }

                for (int i = 0; i < inputs.getResources().size(); i++) {
                    Resource input = inputs.getResources().get(i);
                    Resource output = outputs.getResources().get(i);
                    if (input.getType() != ResourceType.Item || output.getType() != ResourceType.Item) {
                        DebugLog.General.warn("DryingSlot must be of type item!");
                        logic.clearSlots();
                        return false;
                    }

                    DryingLogic.DryingSlot slot = logic.getSlot(i);
                    if (slot == null) {
                        slot = logic.createSlot(i);
                    }

                    slot.initialize(input.getId(), output.getId());
                }

                return true;
            }
        } else {
            return false;
        }
    }

    private void updateDryingLogic(DryingLogic logic, ResourceGroup fuelInputs, ResourceGroup fuelOutputs) {
        if (!logic.isRunning() && logic.isUsesFuel()) {
            if (logic.isUsesFuel()) {
                if (logic.isStartRequested()) {
                    this.start(logic, StartMode.Manual, logic.getRequestingPlayer(), fuelInputs);
                    logic.setStartRequested(false);
                    logic.setRequestingPlayer(null);
                } else if (logic.getStartMode() == StartMode.Automatic && (logic.isDoAutomaticCraftCheck() || fuelInputs.isDirty())) {
                    this.start(logic, StartMode.Automatic, null, fuelInputs);
                    logic.setDoAutomaticCraftCheck(false);
                }
            }
        } else {
            if (logic.isUsesFuel() && fuelOutputs != null && fuelOutputs.isDirty() && !logic.getCraftData().canCreateOutputs(fuelOutputs.getResources())) {
                this.cancel(logic, fuelOutputs);
                return;
            }

            if (logic.isUsesFuel() && logic.isFinished()) {
                this.finish(logic, fuelOutputs);
                return;
            }

            this.updateDryingSlots(logic, false);
            if (logic.isUsesFuel()) {
                logic.setElapsedTime(logic.getElapsedTime() + 1);
                if (logic.getElapsedTime() > logic.getCurrentRecipe().getTime()) {
                    logic.setElapsedTime(logic.getCurrentRecipe().getTime());
                }
            }

            if (logic.isStopRequested()) {
                this.cancel(logic, fuelOutputs);
                logic.setStopRequested(false);
                logic.setRequestingPlayer(null);
            }
        }
    }

    private void updateDryingSlots(DryingLogic logic, boolean cancel) {
        int size = logic.getSlotSize();

        for (int index = 0; index < size; index++) {
            DryingLogic.DryingSlot slot = logic.getSlot(index);
            if (slot != null) {
                if (cancel) {
                    slot.clearRecipe();
                    Resource inputResource = logic.getInputSlotResource(index);
                    Resource outputResource = logic.getOutputSlotResource(index);
                    if (inputResource != null) {
                        inputResource.setProgress(0.0);
                    }

                    if (outputResource != null) {
                        outputResource.setProgress(0.0);
                    }
                } else {
                    Resource inputResourcex = logic.getInputSlotResource(index);
                    Resource outputResourcex = logic.getOutputSlotResource(index);
                    if (!inputResourcex.isEmpty() && !outputResourcex.isFull()) {
                        if (slot.getCurrentRecipe() == null) {
                            this.getSlotRecipe(logic, slot, index);
                            if (slot.getCurrentRecipe() != null) {
                                slot.setElapsedTime(slot.getElapsedTime() + 1);
                            }
                        } else {
                            CraftRecipe recipe = slot.getCurrentRecipe();
                            if (inputResourcex.isDirty() || outputResourcex.isDirty()) {
                                this.setTempSlotResources(inputResourcex, outputResourcex);
                                this.slotCraftTestData.setRecipe(slot.getCurrentRecipe());
                                if (!this.slotCraftTestData.canConsumeInputs(this.tempSlotInputResources)
                                    || !this.slotCraftTestData.canCreateOutputs(this.tempSlotOutputResources)) {
                                    slot.clearRecipe();
                                    continue;
                                }
                            }

                            slot.setElapsedTime(slot.getElapsedTime() + 1);
                            if (slot.getElapsedTime() >= recipe.getTime()) {
                                this.craftSlotRecipe(logic, slot, inputResourcex, outputResourcex);
                            }

                            inputResourcex.setProgress((double)slot.getElapsedTime() / recipe.getTime());
                        }
                    } else if (slot.getCurrentRecipe() != null) {
                        slot.clearRecipe();
                    }
                }
            }
        }
    }

    private void setTempSlotResources(Resource inputResource, Resource outputResource) {
        this.tempSlotInputResources.clear();
        this.tempSlotOutputResources.clear();
        this.tempSlotInputResources.add(inputResource);
        this.tempSlotOutputResources.add(outputResource);
    }

    private void getSlotRecipe(DryingLogic logic, DryingLogic.DryingSlot slot, int index) {
        Resource inputResource = logic.getInputSlotResource(index);
        Resource outputResource = logic.getOutputSlotResource(index);
        this.setTempSlotResources(inputResource, outputResource);
        this.slotCraftTestData.setRecipe(null);
        CraftRecipe recipe = CraftUtil.getPossibleRecipe(
            this.slotCraftTestData, logic.getDryingRecipes(), this.tempSlotInputResources, this.tempSlotOutputResources
        );
        if (recipe != null) {
            slot.setRecipe(recipe);
        } else {
            slot.clearRecipe();
        }
    }

    private void craftSlotRecipe(DryingLogic logic, DryingLogic.DryingSlot slot, Resource input, Resource output) {
        this.setTempSlotResources(input, output);
        this.slotCraftData.setRecipe(slot.getCurrentRecipe());
        if (this.slotCraftData.canConsumeInputs(this.tempSlotInputResources) && this.slotCraftData.canCreateOutputs(this.tempSlotOutputResources)) {
            this.slotCraftData.consumeInputs(this.tempSlotInputResources);
            this.slotCraftData.createOutputs(this.tempSlotOutputResources);
        }

        slot.clearRecipe();
    }

    private float getDryingFactor(DryingLogic logic) {
        GameEntity entity = logic.getGameEntity();
        float temperature = CraftUtil.getEntityTemperature(entity);
        temperature = PZMath.clamp(temperature, 0.0F, 40.0F);
        temperature /= 20.0F;
        if (temperature < 1.0F) {
            temperature *= temperature;
        }

        return temperature;
    }

    private void start(DryingLogic logic, StartMode startMode, IsoPlayer player, ResourceGroup fuelInputs) {
        if (!GameClient.client) {
            if (logic.isUsesFuel() && logic.canStart(startMode, player)) {
                CraftRecipe recipe = logic.getPossibleRecipe();
                if (recipe == null) {
                    return;
                }

                logic.setRecipe(recipe);
                if (!logic.getCraftData().consumeInputs(fuelInputs.getResources())) {
                    logic.setRecipe(null);
                    return;
                }

                logic.getCraftData().luaCallOnStart();
                if (GameServer.server) {
                }
            }
        }
    }

    private void cancel(DryingLogic logic, ResourceGroup fuelOutputs) {
        this.stop(logic, true, fuelOutputs);
    }

    private void finish(DryingLogic logic, ResourceGroup fuelOutputs) {
        this.stop(logic, false, fuelOutputs);
    }

    private void stop(DryingLogic logic, boolean isCancelled, ResourceGroup fuelOutputs) {
        if (!GameClient.client) {
            if (logic.isValid()) {
                if (logic.isRunning()) {
                    if (logic.isUsesFuel()) {
                        if (isCancelled) {
                            logic.getCraftData().luaCallOnFailed();
                        } else if (logic.getCraftData().createOutputs(fuelOutputs.getResources())) {
                            logic.getCraftData().luaCallOnCreate();
                        }

                        this.updateDryingSlots(logic, true);
                        logic.setDoAutomaticCraftCheck(true);
                        logic.setRecipe(null);
                        if (GameServer.server) {
                        }
                    }
                }
            }
        }
    }
}
