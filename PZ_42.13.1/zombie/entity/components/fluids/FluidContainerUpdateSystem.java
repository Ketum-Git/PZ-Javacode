// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.fluids;

import zombie.SandboxOptions;
import zombie.core.utils.UpdateLimit;
import zombie.entity.ComponentType;
import zombie.entity.Engine;
import zombie.entity.EngineSystem;
import zombie.entity.EntityBucket;
import zombie.entity.EntitySimulation;
import zombie.entity.Family;
import zombie.entity.GameEntity;
import zombie.entity.util.ImmutableArray;
import zombie.iso.IsoObject;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;

public class FluidContainerUpdateSystem extends EngineSystem {
    private EntityBucket fluidContainerEntities;
    final UpdateLimit objectSyncLimiter = new UpdateLimit(1000L);

    public FluidContainerUpdateSystem(int updatePriority) {
        super(false, true, updatePriority);
    }

    @Override
    public void addedToEngine(Engine engine) {
        this.fluidContainerEntities = engine.getBucket(Family.all(ComponentType.FluidContainer).get());
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
            boolean doSync = this.objectSyncLimiter.Check();
            ImmutableArray<GameEntity> entities = this.fluidContainerEntities.getEntities();
            if (entities.size() != 0) {
                for (int i = 0; i < entities.size(); i++) {
                    GameEntity entity = entities.get(i);
                    if (this.isValidEntity(entity)) {
                        FluidContainer fluidContainer = entity.getComponent(ComponentType.FluidContainer);
                        if (fluidContainer.isValid() && (entity.isMeta() || fluidContainer.isQualifiesForMetaStorage())) {
                            this.updateEntity(entity, fluidContainer, doSync);
                        }
                    }
                }
            }
        }
    }

    private void updateEntity(GameEntity entity, FluidContainer fluidContainer, boolean doSync) {
        if (fluidContainer.canPlayerEmpty()
            && fluidContainer.getRainCatcher() > 0.0F
            && !fluidContainer.isEmpty()
            && fluidContainer.getPrimaryFluid().getFluidTypeString().equals("Petrol")) {
            float amount = 1.0E-4F * fluidContainer.getRainCatcher() / (SandboxOptions.getInstance().getDayLengthMinutes() * 24 / 60);
            if (fluidContainer.getAmount() < amount) {
                amount = fluidContainer.getAmount();
            }

            fluidContainer.adjustAmount(fluidContainer.getAmount() - amount);
            if (doSync && entity instanceof IsoObject isoObject) {
                isoObject.sync();
            }
        }

        if (ClimateManager.getInstance().getPrecipitationIntensity() > 0.0F
            && fluidContainer.getGameEntity().isOutside()
            && fluidContainer.canPlayerEmpty()
            && fluidContainer.getRainCatcher() > 0.0F) {
            FluidType waterType = FluidType.TaintedWater;
            Float snowModifier = 1.0F;
            if (fluidContainer.isFilledWithCleanWater()) {
                waterType = FluidType.Water;
            }

            if (ClimateManager.getInstance().getPrecipitationIsSnow()) {
                snowModifier = 0.5F;
            }

            if (fluidContainer.canAddFluid(Fluid.Get(waterType))) {
                float rainAmount = 0.005F
                    * ClimateManager.getInstance().getPrecipitationIntensity()
                    * snowModifier
                    * fluidContainer.getRainCatcher()
                    * (float)EntitySimulation.getGameSecondsPerTick();
                if (fluidContainer.getFreeCapacity() < rainAmount) {
                    fluidContainer.adjustAmount(fluidContainer.getCapacity() - rainAmount);
                }

                fluidContainer.addFluid(waterType, rainAmount);
                if (doSync && entity instanceof IsoObject isoObject) {
                    isoObject.sync();
                }
            }
        }
    }
}
