// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.inventory.ItemContainer;
import zombie.iso.objects.IsoDoor;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;

public class IsoObjectUtils {
    private static final boolean THROW_ERROR = false;
    private static final ThreadLocal<ArrayList<IsoObject>> threadLocalObjects = ThreadLocal.withInitial(ArrayList::new);

    public static boolean isObjectMultiSquare(IsoObject object) {
        if (object == null) {
            return false;
        } else if (IsoDoor.getDoubleDoorIndex(object) != -1 || IsoDoor.getGarageDoorIndex(object) != -1) {
            return true;
        } else {
            return object.getSpriteConfig() != null && object.getSpriteConfig().isValidMultiSquare()
                ? true
                : object.getSprite() != null && object.getSprite().getSpriteGrid() != null;
        }
    }

    public static int safelyRemoveTileObjectFromSquare(IsoObject object) {
        if (isObjectMultiSquare(object)) {
            ArrayList<IsoObject> objects = threadLocalObjects.get();
            objects.clear();
            if (!getAllMultiTileObjects(object, objects)) {
                if (Core.debug) {
                    DebugLog.General.warn("Failed to find all parts of a multi-tile object!");
                }

                return -1;
            } else {
                int objectIndex = -1;

                for (IsoObject obj : objects) {
                    IsoGridSquare sq = obj.square;
                    if (sq != null) {
                        int idx = sq.RemoveTileObject(obj, false);
                        if (object == obj) {
                            objectIndex = idx;
                        }
                    } else if (Core.debug) {
                        DebugLog.General.warn("Failed to find all parts of a multi-tile object!");
                    }
                }

                return objectIndex;
            }
        } else {
            return object != null && object.square != null ? object.square.RemoveTileObject(object, false) : -1;
        }
    }

    public static boolean getAllMultiTileObjects(IsoObject object, ArrayList<IsoObject> outList) {
        int ddIndex = IsoDoor.getDoubleDoorIndex(object);
        if (ddIndex != -1) {
            IsoObject object1 = IsoDoor.getDoubleDoorObject(object, ddIndex);
            IsoObject object2 = IsoDoor.getDoubleDoorObject(object, IsoDoor.getDoubleDoorPartnerIndex(ddIndex));
            if (object1 != null) {
                outList.add(object1);
            }

            if (object2 != null) {
                outList.add(object2);
            }

            return !outList.isEmpty();
        } else {
            int gdIndex = IsoDoor.getGarageDoorIndex(object);
            if (gdIndex == -1) {
                if (object.getSpriteConfig() != null && object.getSpriteConfig().isValidMultiSquare()) {
                    return object.getSpriteConfig().getAllMultiSquareObjects(outList);
                } else {
                    return object.getSprite() != null && object.getSprite().getSpriteGrid() != null ? getSpriteGridMultiTileObjects(object, outList) : false;
                }
            } else {
                for (IsoObject object1x = IsoDoor.getGarageDoorFirst(object); object1x != null; object1x = IsoDoor.getGarageDoorNext(object1x)) {
                    outList.add(object1x);
                }

                return !outList.isEmpty();
            }
        }
    }

    private static boolean getSpriteGridMultiTileObjects(IsoObject object, ArrayList<IsoObject> outList) {
        if (object.getSprite() != null && object.getSprite().getSpriteGrid() != null) {
            IsoGridSquare origSquare = object.square;
            IsoSpriteGrid spriteGrid = object.getSpriteGrid();
            int ox = spriteGrid.getSpriteGridPosX(object.getSprite());
            int oy = spriteGrid.getSpriteGridPosY(object.getSprite());
            int oz = spriteGrid.getSpriteGridPosZ(object.getSprite());

            for (int z = 0; z < spriteGrid.getLevels(); z++) {
                for (int x = 0; x < spriteGrid.getWidth(); x++) {
                    for (int y = 0; y < spriteGrid.getHeight(); y++) {
                        int cx = x - ox;
                        int cy = y - oy;
                        int cz = z - oz;
                        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(origSquare.x + cx, origSquare.y + cy, origSquare.z + cz);
                        IsoSprite testSprite = spriteGrid.getSprite(x, y, z);
                        boolean multiFound = false;
                        if (sq != null) {
                            for (int i = 0; i < sq.getObjects().size(); i++) {
                                IsoObject sqObj = sq.getObjects().get(i);
                                if (verifyObject(sqObj, testSprite)) {
                                    outList.add(sqObj);
                                    multiFound = true;
                                    break;
                                }
                            }
                        }

                        if (!multiFound) {
                            outList.clear();
                            return false;
                        }
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private static boolean verifyObject(IsoObject object, IsoSprite testSprite) {
        return object.getSprite() != null ? object.getSprite() == testSprite : false;
    }

    public static void dumpContentsInSquare(IsoObject object) {
        if (object.getSquare() != null) {
            for (int i = 0; i < object.getContainerCount(); i++) {
                ItemContainer container = object.getContainerByIndex(i);
                container.dumpContentsInSquare(object.getSquare());
            }

            if (isObjectMultiSquare(object)) {
                ArrayList<IsoObject> objects = threadLocalObjects.get();
                objects.clear();
                if (!getAllMultiTileObjects(object, objects)) {
                    if (Core.debug) {
                        DebugLog.General.warn("Failed to find all parts of a multi-tile object!");
                    }

                    return;
                }

                for (IsoObject obj : objects) {
                    IsoGridSquare sq = obj.square;
                    if (sq != null) {
                        for (int i = 0; i < obj.getContainerCount(); i++) {
                            obj.getContainerByIndex(i).dumpContentsInSquare(sq);
                        }
                    }
                }
            }

            ArrayList<IsoObject> multiSquareObjects = new ArrayList<>();
            if (object.hasComponent(ComponentType.SpriteConfig)) {
                object.<SpriteConfig>getComponent(ComponentType.SpriteConfig).getAllMultiSquareObjects(multiSquareObjects);
            } else {
                multiSquareObjects.add(object);
            }

            for (IsoObject subObject : multiSquareObjects) {
                for (int i = 0; i < subObject.componentSize(); i++) {
                    Component component = subObject.getComponentForIndex(i);
                    if (component != null) {
                        component.dumpContentsInSquare();
                    }
                }
            }
        }
    }
}
