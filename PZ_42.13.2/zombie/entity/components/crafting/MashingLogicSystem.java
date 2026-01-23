// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting;

import java.util.List;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.resources.ResourceFluid;
import zombie.entity.components.resources.ResourceGroup;
import zombie.entity.components.resources.ResourceType;
import zombie.entity.components.resources.Resources;
import zombie.entity.util.ImmutableArray;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.OutputScript;

public class MashingLogicSystem extends EngineSystem {
    private static final float fermentMinTemp = 12.0F;
    private static final float fermentOptimalTemp = 21.0F;
    private static final float fermentMaxTemp = 30.0F;
    private static double currentWorldAge;
    private EntityBucket mashingEntities;

    public MashingLogicSystem(int updatePriority) {
        super(true, false, updatePriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.mashingEntities = engine.getBucket(Family.all(ComponentType.MashingLogic, ComponentType.Resources).get());
    }

    @Override
    public void removedFromEngine(Engine engine) {
    }

    private boolean isValidEntity(GameEntity entity) {
        return entity.isEntityValid() && entity.isValidEngineEntity();
    }

    @Override
    public void update() {
        if (!GameClient.client) {
            ImmutableArray<GameEntity> entities = this.mashingEntities.getEntities();
            if (entities.size() != 0) {
                currentWorldAge = GameTime.instance.getWorldAgeHours();

                for (int i = 0; i < entities.size(); i++) {
                    GameEntity entity = entities.get(i);
                    if (this.isValidEntity(entity)) {
                        MashingLogic mashingLogic = entity.getComponent(ComponentType.MashingLogic);
                        Resources resources = entity.getComponent(ComponentType.Resources);
                        if (mashingLogic.isValid() && resources.isValid()) {
                            ResourceGroup inputResources = resources.getResourceGroup(mashingLogic.getInputsGroupName());
                            if (inputResources != null && !inputResources.getResources().isEmpty()) {
                                ResourceFluid fluidBarrel = (ResourceFluid)inputResources.get(mashingLogic.getResourceFluidID());
                                if (fluidBarrel != null) {
                                    this.updateMashingLogic(mashingLogic, inputResources, fluidBarrel);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateMashingLogic(MashingLogic logic, ResourceGroup inputResources, ResourceFluid fluidBarrel) {
        if (logic.isRunning()) {
            if (logic.isFinished()) {
                this.finish(logic, fluidBarrel);
                return;
            }

            float temperature = CraftUtil.getEntityTemperature(logic.getGameEntity());
            if (temperature < 12.0F) {
                logic.setLastWorldAge(currentWorldAge);
            } else {
                if (temperature > 30.0F) {
                    this.cancel(logic, fluidBarrel);
                    return;
                }

                double time = 0.0;
                if (logic.getLastWorldAge() >= 0.0) {
                    time = currentWorldAge - logic.getLastWorldAge();
                }

                logic.setElapsedTime(logic.getElapsedTime() + time);
                if (logic.getElapsedTime() > logic.getCurrentRecipe().getTime()) {
                    logic.setElapsedTime(logic.getCurrentRecipe().getTime());
                }

                logic.setLastWorldAge(currentWorldAge);
            }

            if (logic.isStopRequested()) {
                this.cancel(logic, fluidBarrel);
                logic.setStopRequested(false);
                logic.setRequestingPlayer(null);
            }
        } else if (logic.isStartRequested()) {
            DebugLog.General.debugln("Requesting start...");
            this.start(logic, StartMode.Manual, logic.getRequestingPlayer(), inputResources, fluidBarrel);
            logic.setStartRequested(false);
            logic.setRequestingPlayer(null);
        }
    }

    private void start(MashingLogic logic, StartMode startMode, IsoPlayer player, ResourceGroup inputResources, ResourceFluid fluidBarrel) {
        if (!GameClient.client) {
            if (logic.canStart(startMode, player)) {
                DebugLog.General.debugln("Start...");
                CraftRecipe recipe = logic.getPossibleRecipe();
                if (recipe == null) {
                    return;
                }

                logic.setRecipe(recipe);
                logic.setElapsedTime(0.0);
                logic.setLastWorldAge(currentWorldAge);
                FluidContainer container = fluidBarrel.getFluidContainer();
                logic.setBarrelConsumedAmount(container.getAmount());
                if (!logic.getCraftData().consumeInputs(inputResources.getResources())) {
                    logic.setRecipe(null);
                    return;
                }

                logic.getCraftData().luaCallOnStart();
                DebugLog.General.debugln("START_PASSED");
                if (GameServer.server) {
                }
            }
        }
    }

    private void cancel(MashingLogic logic, ResourceFluid fluidBarrel) {
        this.stop(logic, true, fluidBarrel);
    }

    private void finish(MashingLogic logic, ResourceFluid fluidBarrel) {
        this.stop(logic, false, fluidBarrel);
    }

    private void stop(MashingLogic logic, boolean isCancelled, ResourceFluid fluidBarrel) {
        if (!GameClient.client) {
            if (logic.isValid()) {
                if (logic.isRunning()) {
                    DebugLog.General.debugln("Stop, cancelled = " + isCancelled);
                    if (isCancelled) {
                        logic.getCraftData().luaCallOnFailed();
                    } else {
                        this.createResultFluid(logic, isCancelled, fluidBarrel);
                    }

                    logic.setRecipe(null);
                    if (GameServer.server) {
                    }
                }
            }
        }
    }

    private void createResultFluid(MashingLogic logic, boolean isCancelled, ResourceFluid fluidBarrel) {
        FluidContainer container = fluidBarrel.getFluidContainer();
        fluidBarrel.clear();
        float amount = logic.getBarrelConsumedAmount();
        if (isCancelled) {
            container.addFluid(Fluid.TaintedWater, amount);
        } else {
            CraftRecipe recipe = logic.getCurrentRecipe();
            if (recipe == null) {
                return;
            }

            List<OutputScript> outputs = recipe.getOutputs();

            for (int i = 0; i < outputs.size(); i++) {
                OutputScript output = outputs.get(i);
                if (output.getResourceType() == ResourceType.Fluid) {
                    Fluid fluid = output.getFluid();
                    container.addFluid(fluid, amount);
                    return;
                }
            }
        }
    }
}
