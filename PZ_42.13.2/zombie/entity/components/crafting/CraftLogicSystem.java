// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting;

import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.debug.DebugType;
import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.EntitySimulation;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.Resources;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.components.crafting.CraftRecipe;

public class CraftLogicSystem extends EngineSystem {
    private EntityBucket craftLogicEntities;

    public CraftLogicSystem(int updatePriority) {
        super(false, true, updatePriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.craftLogicEntities = engine.getBucket(Family.all(ComponentType.Resources).one(ComponentType.CraftLogic, ComponentType.DryingCraftLogic).get());
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
            ImmutableArray<GameEntity> entities = this.craftLogicEntities.getEntities();
            if (entities.size() != 0) {
                for (int i = 0; i < entities.size(); i++) {
                    GameEntity entity = entities.get(i);
                    if (this.isValidEntity(entity)) {
                        CraftLogic craftLogic = entity.getComponentAny(ComponentType.CraftLogic, ComponentType.DryingCraftLogic);
                        Resources resources = entity.getComponent(ComponentType.Resources);
                        if (craftLogic.isValid() && resources.isValid()) {
                            ResourceGroup inputResources = resources.getResourceGroup(craftLogic.getInputsGroupName());
                            ResourceGroup outputResources = resources.getResourceGroup(craftLogic.getOutputsGroupName());
                            if (inputResources != null
                                && !inputResources.getResources().isEmpty()
                                && outputResources != null
                                && !outputResources.getResources().isEmpty()) {
                                this.updateCraftLogic(craftLogic, inputResources, outputResources);
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateCraftLogic(CraftLogic logic, ResourceGroup inputResources, ResourceGroup outputResources) {
        if (logic.isRunning()) {
            for (CraftRecipeData craftData : logic.getAllInProgressCraftData()) {
                if (outputResources.isDirty() && !craftData.canCreateOutputs(outputResources.getResources())) {
                    this.cancel(logic, craftData, outputResources);
                    return;
                }

                if (craftData.isFinished()) {
                    this.finish(logic, craftData, outputResources);
                    return;
                }

                craftData.setElapsedTime(craftData.getElapsedTime() + EntitySimulation.getGameSecondsPerTick());
                if (craftData.getElapsedTime() > craftData.getRecipe().getTime()) {
                    craftData.setElapsedTime(craftData.getRecipe().getTime());
                }

                logic.onUpdate(craftData);
                craftData.luaCallOnUpdate();
            }

            if (logic.isStopRequested()) {
                ArrayList<CraftRecipeData> allInProgressCraftData = logic.getAllInProgressCraftData();

                for (int i = allInProgressCraftData.size() - 1; i >= 0; i--) {
                    this.cancel(logic, allInProgressCraftData.get(i), outputResources);
                }

                logic.setStopRequested(false);
                logic.setRequestingPlayer(null);
            }
        }

        if (logic.isStartRequested()) {
            this.start(logic, StartMode.Manual, logic.getRequestingPlayer(), inputResources);
            logic.setStartRequested(false);
            logic.setRequestingPlayer(null);
        } else if (logic.getStartMode() == StartMode.Automatic && (logic.isDoAutomaticCraftCheck() || inputResources.isDirty())) {
            this.start(logic, StartMode.Automatic, null, inputResources);
            logic.setDoAutomaticCraftCheck(false);
        }
    }

    private void start(CraftLogic logic, StartMode startMode, IsoPlayer player, ResourceGroup inputResources) {
        if (!GameClient.client) {
            if (logic.canStart(startMode, player)) {
                CraftRecipe recipe = logic.getPossibleRecipe();
                if (recipe == null) {
                    return;
                }

                logic.setRecipe(recipe);
                CraftRecipeData pendingCraftData = logic.getPendingCraftData();
                if (!pendingCraftData.consumeInputs(inputResources.getResources())) {
                    logic.setRecipe(null);
                    return;
                }

                logic.onStart();
                pendingCraftData.luaCallOnStart();
                player.getPlayerCraftHistory().addCraftHistoryCraftedEvent(pendingCraftData.getRecipe().getName());
                if (GameServer.server) {
                }
            }
        }
    }

    private void cancel(CraftLogic logic, CraftRecipeData craftData, ResourceGroup outputResources) {
        this.stop(logic, craftData, true, outputResources);
    }

    private void finish(CraftLogic logic, CraftRecipeData craftData, ResourceGroup outputResources) {
        this.stop(logic, craftData, false, outputResources);
    }

    private void stop(CraftLogic logic, CraftRecipeData craftData, boolean isCancelled, ResourceGroup outputResources) {
        if (!GameClient.client) {
            if (logic.isValid()) {
                if (logic.isRunning()) {
                    if (isCancelled) {
                        logic.returnConsumedItemsToResourcesOrSquare(craftData);
                        craftData.luaCallOnFailed();
                    } else if (craftData.createOutputs(outputResources.getResources())) {
                        craftData.luaCallOnCreate();
                        DebugType.CraftLogic
                            .debugln("Craft Complete: %s from %s", craftData.getRecipe().getName(), logic.getGameEntity().getEntityDisplayName());
                    }

                    logic.onStop(craftData, isCancelled);
                    logic.setDoAutomaticCraftCheck(true);
                    logic.finaliseRecipe(craftData);
                    if (GameServer.server) {
                        logic.sendCraftLogicSync();
                    }
                }
            }
        }
    }
}
