// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import java.util.ArrayList;
import zombie.characters.action.ActionGroup;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.gameStates.IngameState;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.util.StringUtils;

public final class AnimalManagerMain {
    private static AnimalManagerMain instance;

    static AnimalManagerMain getInstance() {
        if (instance == null) {
            instance = new AnimalManagerMain();
        }

        return instance;
    }

    void loadChunk(int x, int y) {
        AnimalManagerWorker.getInstance().loadChunk(x, y);
    }

    void unloadChunk(int x, int y) {
        AnimalManagerWorker.getInstance().unloadChunk(x, y);
    }

    void addAnimal(VirtualAnimal animal) {
        AnimalManagerWorker.getInstance().addAnimal(animal);
    }

    void removeFromWorld(IsoAnimal animal) {
        AnimalManagerWorker.getInstance().removeFromWorld(animal);
    }

    void saveRealAnimals() {
        ArrayList<IsoAnimal> realAnimals = new ArrayList<>();
        ArrayList<IsoMovingObject> objects = IsoWorld.instance.currentCell.getObjectList();

        for (int i = 0; i < objects.size(); i++) {
            if (objects.get(i) instanceof IsoAnimal animal) {
                realAnimals.add(animal);
            }
        }

        AnimalPopulationManager.getInstance().n_saveRealAnimals(realAnimals);
    }

    void fromWorker(ArrayList<VirtualAnimal> animals) {
        for (int i = 0; i < animals.size(); i++) {
            VirtualAnimal virtualAnimal = animals.get(i);
            IsoGridSquare square = IsoWorld.instance
                .currentCell
                .getGridSquare(PZMath.fastfloor(virtualAnimal.getX()), PZMath.fastfloor(virtualAnimal.getY()), PZMath.fastfloor(virtualAnimal.getZ()));
            if (square != null) {
                float PAD = 0.1F;
                float minX = square.chunk.wx * 8;
                float minY = square.chunk.wy * 8;
                float maxX = minX + 8.0F;
                float maxY = minY + 8.0F;

                for (int j = 0; j < virtualAnimal.animals.size(); j++) {
                    IsoAnimal isoAnimal = virtualAnimal.animals.get(j);
                    if (isoAnimal.virtualId == 0.0 && StringUtils.isNullOrEmpty(isoAnimal.migrationGroup)) {
                        isoAnimal.setForceX(virtualAnimal.getX());
                        isoAnimal.setForceY(virtualAnimal.getY());
                        square = IsoWorld.instance
                            .currentCell
                            .getGridSquare(PZMath.fastfloor(isoAnimal.getX()), PZMath.fastfloor(isoAnimal.getY()), PZMath.fastfloor(virtualAnimal.getZ()));
                    } else {
                        float randX = virtualAnimal.getX() + Rand.Next(-3, 3);
                        float randY = virtualAnimal.getY() + Rand.Next(-3, 3);
                        isoAnimal.setForceX(PZMath.clamp(randX, minX + 0.1F, maxX - 0.1F));
                        isoAnimal.setForceY(PZMath.clamp(randY, minY + 0.1F, maxY - 0.1F));
                        square = IsoWorld.instance
                            .currentCell
                            .getGridSquare(PZMath.fastfloor(isoAnimal.getX()), PZMath.fastfloor(isoAnimal.getY()), PZMath.fastfloor(virtualAnimal.getZ()));
                    }

                    isoAnimal.setZ(virtualAnimal.getZ());
                    isoAnimal.setDir(IsoDirections.getRandom());
                    isoAnimal.setForwardDirection(virtualAnimal.forwardDirection.x + Rand.Next(360), virtualAnimal.forwardDirection.y + Rand.Next(360));
                    isoAnimal.setCurrent(square);
                    isoAnimal.setLast(square);
                    isoAnimal.setMovingSquareNow();
                    isoAnimal.virtualId = virtualAnimal.id;
                    isoAnimal.migrationGroup = virtualAnimal.migrationGroup;
                    if (!IngameState.loading) {
                        isoAnimal.fromMeta = true;
                    }

                    isoAnimal.addToWorld();
                    isoAnimal.getActionContext().setGroup(ActionGroup.getActionGroup(isoAnimal.adef.animset));
                    isoAnimal.advancedAnimator.OnAnimDataChanged(false);
                    isoAnimal.updateStatsAway(0);

                    assert !IsoWorld.instance.currentCell.getObjectList().contains(isoAnimal);

                    if (IsoWorld.instance.currentCell.getObjectList().contains(isoAnimal)) {
                        DebugLog.Animal.error("Animal already in IsoCell.ObjectList.");
                    } else {
                        IsoWorld.instance.currentCell.getObjectList().add(isoAnimal);
                    }
                }
            }
        }
    }
}
