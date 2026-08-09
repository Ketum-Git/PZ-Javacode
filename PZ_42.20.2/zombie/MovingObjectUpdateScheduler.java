// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.popman.ZombieCountOptimiser;
import zombie.util.list.PZArrayUtil;

public final class MovingObjectUpdateScheduler {
    public static final MovingObjectUpdateScheduler instance = new MovingObjectUpdateScheduler();
    private final MovingObjectUpdateSchedulerUpdateBucket[] simulationLevels;
    private long frameCounter;
    private boolean isEnabled = true;

    private MovingObjectUpdateScheduler() {
        this.simulationLevels = new MovingObjectUpdateSchedulerUpdateBucket[UpdateSchedulerSimulationLevel.numValues()];

        for (UpdateSchedulerSimulationLevel simulationLevel : UpdateSchedulerSimulationLevel.allValues()) {
            this.simulationLevels[simulationLevel.getUpdateOrderIndex()] = new MovingObjectUpdateSchedulerUpdateBucket(simulationLevel);
        }
    }

    public long getFrameCounter() {
        return this.frameCounter;
    }

    public void startFrame() {
        this.frameCounter++;
        PZArrayUtil.forEach(this.simulationLevels, MovingObjectUpdateSchedulerUpdateBucket::clear);
        float averageFps = GameWindow.averageFPS;
        if (GameServer.server) {
            ZombieCountOptimiser.prepareZombiesForDeletion();
        }

        for (IsoMovingObject isoMovingObject : IsoWorld.instance.getCell().getObjectList()) {
            if (GameServer.server && isoMovingObject instanceof IsoZombie isoZombie) {
                if (GameServer.guiCommandline) {
                    isoZombie.updateForServerGui();
                }
            } else {
                if (isoMovingObject.getCurrentSquare() == null) {
                    isoMovingObject.setCurrentSquareFromPosition();
                }

                UpdateSchedulerSimulationLevel sim = this.getUpdateSchedulerSimulationLevelForObject(isoMovingObject, averageFps);
                this.simulationLevels[sim.getUpdateOrderIndex()].add(isoMovingObject);
            }
        }
    }

    private UpdateSchedulerSimulationLevel getUpdateSchedulerSimulationLevelForObject(IsoMovingObject isoMovingObject, float averageFps) {
        if (this.isEnabled && !GameServer.server) {
            UpdateSchedulerSimulationLevel minSim = isoMovingObject.getMinimumSimulationLevel();
            if (minSim == UpdateSchedulerSimulationLevel.FULL) {
                return minSim;
            } else if (isoMovingObject.getDoRender() && !isoMovingObject.isSceneCulled()) {
                float distance = 1.0E8F;
                int levelSeparation = Integer.MAX_VALUE;
                float alpha = 0.0F;
                float targetAlpha = 0.0F;

                for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
                    IsoPlayer player = IsoPlayer.players[playerIndex];
                    if (player != null) {
                        if (player == isoMovingObject) {
                            return UpdateSchedulerSimulationLevel.FULL;
                        }

                        distance = PZMath.min(isoMovingObject.DistTo(player), distance);
                        levelSeparation = PZMath.min(PZMath.abs(isoMovingObject.getZi() - player.getZi()), levelSeparation);
                        alpha = PZMath.max(isoMovingObject.getAlpha(playerIndex), alpha);
                        targetAlpha = PZMath.max(isoMovingObject.getTargetAlpha(playerIndex), targetAlpha);
                    }
                }

                UpdateSchedulerSimulationLevel sim = UpdateSchedulerSimulationLevel.FULL;
                float minAlpha = 0.25F;
                if (alpha < 0.25F && targetAlpha < 0.25F) {
                    sim = sim.less();
                    if (distance > 10.0F) {
                        sim = sim.less();
                    }

                    if (levelSeparation > 1) {
                        sim = minSim;
                    }
                }

                if (distance > 30.0F) {
                    sim = sim.less();
                }

                if (distance > 60.0F) {
                    sim = sim.less();
                    if (averageFps < 20.0F) {
                        sim = sim.less();
                    }

                    if (averageFps < 10.0F) {
                        sim = sim.less();
                    }
                }

                if (distance > 80.0F) {
                    sim = sim.less();
                    if (averageFps < 20.0F) {
                        sim = sim.less();
                    }
                }

                if (averageFps > 25.0F) {
                    sim = sim.more();
                }

                if (averageFps > 35.0F) {
                    sim = sim.more();
                }

                if (averageFps > 45.0F) {
                    sim = sim.more();
                }

                if (averageFps > 55.0F) {
                    sim = sim.more();
                }

                return sim.max(minSim);
            } else {
                return minSim;
            }
        } else {
            return UpdateSchedulerSimulationLevel.FULL;
        }
    }

    public void update() {
        for (MovingObjectUpdateSchedulerUpdateBucket simulation : this.simulationLevels) {
            simulation.update((int)this.frameCounter);
        }
    }

    public void postupdate() {
        if (GameServer.server) {
            ZombieCountOptimiser.deleteZombies();
        }

        for (MovingObjectUpdateSchedulerUpdateBucket simulation : this.simulationLevels) {
            simulation.postupdate((int)this.frameCounter);
        }
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public void removeObject(IsoMovingObject object) {
        PZArrayUtil.forEach(this.simulationLevels, object, MovingObjectUpdateSchedulerUpdateBucket::removeObject);
    }
}
