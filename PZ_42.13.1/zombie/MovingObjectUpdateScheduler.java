// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;

public final class MovingObjectUpdateScheduler {
    public static final MovingObjectUpdateScheduler instance = new MovingObjectUpdateScheduler();
    final MovingObjectUpdateSchedulerUpdateBucket fullSimulation = new MovingObjectUpdateSchedulerUpdateBucket(1);
    final MovingObjectUpdateSchedulerUpdateBucket halfSimulation = new MovingObjectUpdateSchedulerUpdateBucket(2);
    final MovingObjectUpdateSchedulerUpdateBucket quarterSimulation = new MovingObjectUpdateSchedulerUpdateBucket(4);
    final MovingObjectUpdateSchedulerUpdateBucket eighthSimulation = new MovingObjectUpdateSchedulerUpdateBucket(8);
    final MovingObjectUpdateSchedulerUpdateBucket sixteenthSimulation = new MovingObjectUpdateSchedulerUpdateBucket(16);
    long frameCounter;
    private boolean isEnabled = true;

    public long getFrameCounter() {
        return this.frameCounter;
    }

    public void startFrame() {
        this.frameCounter++;
        this.fullSimulation.clear();
        this.halfSimulation.clear();
        this.quarterSimulation.clear();
        this.eighthSimulation.clear();
        this.sixteenthSimulation.clear();
        ArrayList<IsoMovingObject> objectList = IsoWorld.instance.getCell().getObjectList();

        for (int i = 0; i < objectList.size(); i++) {
            IsoMovingObject isoMovingObject = objectList.get(i);
            if (GameServer.server && isoMovingObject instanceof IsoZombie isoZombie) {
                if (GameServer.guiCommandline) {
                    isoZombie.updateForServerGui();
                }
            } else {
                boolean couldSee = false;
                boolean canSee = false;
                float distance = 1.0E8F;
                int levelSeparation = Integer.MAX_VALUE;
                boolean isPlayer = false;

                for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
                    IsoPlayer player = IsoPlayer.players[playerIndex];
                    if (player != null) {
                        if (isoMovingObject.getCurrentSquare() == null) {
                            isoMovingObject.setCurrentSquareFromPosition();
                        }

                        if (player == isoMovingObject) {
                            isPlayer = true;
                        }

                        if (isoMovingObject.getCurrentSquare() != null) {
                            if (isoMovingObject.getCurrentSquare().isCouldSee(playerIndex)) {
                                couldSee = true;
                            }

                            if (isoMovingObject.getCurrentSquare().isCanSee(playerIndex)) {
                                canSee = true;
                            }

                            float dist = isoMovingObject.DistTo(player);
                            if (dist < distance) {
                                distance = dist;
                            }
                        }

                        int levelSeparation1 = (int)PZMath.abs(isoMovingObject.getZi() - player.getZi());
                        levelSeparation = PZMath.min(levelSeparation1, levelSeparation);
                    }
                }

                int sim = 3;
                if (!canSee) {
                    sim--;
                }

                if (!couldSee && distance > 10.0F) {
                    sim--;
                }

                if (distance > 30.0F) {
                    sim--;
                }

                if (distance > 60.0F) {
                    sim--;
                }

                if (distance > 80.0F) {
                    sim--;
                }

                if (!canSee && levelSeparation > 1) {
                    sim = -1;
                }

                if (isoMovingObject instanceof IsoPlayer) {
                    sim = 3;
                }

                if (isoMovingObject instanceof BaseVehicle) {
                    sim = 3;
                }

                if (GameServer.server) {
                    sim = 3;
                }

                if (isPlayer) {
                    sim = 3;
                }

                if (!this.isEnabled) {
                    sim = 3;
                }

                if (sim == 3) {
                    this.fullSimulation.add(isoMovingObject);
                }

                if (sim == 2) {
                    this.halfSimulation.add(isoMovingObject);
                }

                if (sim == 1) {
                    this.quarterSimulation.add(isoMovingObject);
                }

                if (sim == 0) {
                    this.eighthSimulation.add(isoMovingObject);
                }

                if (sim < 0) {
                    this.sixteenthSimulation.add(isoMovingObject);
                }
            }
        }
    }

    public void update() {
        GameTime.getInstance().perObjectMultiplier = 1.0F;
        this.fullSimulation.update((int)this.frameCounter);
        this.halfSimulation.update((int)this.frameCounter);
        this.quarterSimulation.update((int)this.frameCounter);
        this.eighthSimulation.update((int)this.frameCounter);
        this.sixteenthSimulation.update((int)this.frameCounter);
    }

    public void postupdate() {
        GameTime.getInstance().perObjectMultiplier = 1.0F;
        this.fullSimulation.postupdate((int)this.frameCounter);
        this.halfSimulation.postupdate((int)this.frameCounter);
        this.quarterSimulation.postupdate((int)this.frameCounter);
        this.eighthSimulation.postupdate((int)this.frameCounter);
        this.sixteenthSimulation.postupdate((int)this.frameCounter);
    }

    public void updateAnimation() {
        GameTime.getInstance().perObjectMultiplier = 1.0F;
        this.fullSimulation.updateAnimation((int)this.frameCounter);
        this.halfSimulation.updateAnimation((int)this.frameCounter);
        this.quarterSimulation.updateAnimation((int)this.frameCounter);
        this.eighthSimulation.updateAnimation((int)this.frameCounter);
        this.sixteenthSimulation.updateAnimation((int)this.frameCounter);
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public void removeObject(IsoMovingObject object) {
        this.fullSimulation.removeObject(object);
        this.halfSimulation.removeObject(object);
        this.quarterSimulation.removeObject(object);
        this.eighthSimulation.removeObject(object);
        this.sixteenthSimulation.removeObject(object);
    }

    public ArrayList<IsoMovingObject> getBucket() {
        return this.fullSimulation.getBucket((int)this.frameCounter);
    }
}
