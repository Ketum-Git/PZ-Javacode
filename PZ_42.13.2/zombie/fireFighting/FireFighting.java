// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.fireFighting;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.entity.ComponentType;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidType;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoFire;

public class FireFighting {
    public static boolean isSquareToExtinguish(IsoGridSquare square) {
        if (square == null) {
            return false;
        } else {
            if (square.has(IsoFlagType.burning)) {
                for (int i = 0; i < square.getObjects().size(); i++) {
                    IsoObject object = square.getObjects().get(i);
                    if (object instanceof IsoFire isoFire && !isoFire.isPermanent()) {
                        return true;
                    }
                }
            }

            for (int ix = 0; ix < square.getMovingObjects().size(); ix++) {
                IsoMovingObject movingObj = square.getMovingObjects().get(ix);
                if (movingObj instanceof IsoGameCharacter isoGameCharacter && isoGameCharacter.isOnFire()) {
                    return true;
                }
            }

            return false;
        }
    }

    public static IsoGridSquare getSquareToExtinguish(IsoGridSquare square) {
        if (isSquareToExtinguish(square)) {
            return square;
        } else {
            int x = square.getX();
            int y = square.getY();
            int z = square.getZ();

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx != 0 || dy != 0) {
                        square = IsoWorld.instance.getCell().getGridSquare(x + dx, y + dy, z);
                        if (isSquareToExtinguish(square)) {
                            return square;
                        }
                    }
                }
            }

            return null;
        }
    }

    public static InventoryItem getExtinguisher(IsoPlayer playerObj) {
        InventoryItem primary = playerObj.getPrimaryHandItem();
        if (isExtinguisher(primary)) {
            return primary;
        } else {
            InventoryItem secondary = playerObj.getSecondaryHandItem();
            if (isExtinguisher(secondary)) {
                return secondary;
            } else {
                InventoryItem extinguisher = null;
                InventoryItem bagOfX = null;
                InventoryItem waterSource = null;

                for (int i = 0; i < playerObj.getInventory().getItems().size(); i++) {
                    InventoryItem item = playerObj.getInventory().getItems().get(i);
                    if (isExtinguisher(item)) {
                        if (item.isWaterSource() && waterSource == null) {
                            waterSource = item;
                        }

                        if (item.getType().equals("Extinguisher") && extinguisher == null) {
                            extinguisher = item;
                        }

                        if ((item.getType().equals("Sandbag") || item.getType().equals("Gravelbag") || item.getType().equals("Dirtbag")) && bagOfX == null) {
                            bagOfX = item;
                        }
                    }
                }

                if (extinguisher != null) {
                    return extinguisher;
                } else {
                    return bagOfX != null ? bagOfX : waterSource;
                }
            }
        }
    }

    public static boolean isExtinguisher(InventoryItem item) {
        if (item == null) {
            return false;
        } else if (item.getType().equals("Extinguisher")) {
            return item.getCurrentUses() >= getExtinguisherUses(item);
        } else if (item.getType().equals("Sandbag") || item.getType().equals("Gravelbag") || item.getType().equals("Dirtbag")) {
            return item.getCurrentUses() >= getExtinguisherUses(item);
        } else {
            return item.isWaterSource() ? getWaterUsesInteger(item) >= getExtinguisherUses(item) : false;
        }
    }

    public static int getExtinguisherUses(InventoryItem item) {
        if (item == null) {
            return 10000;
        } else if (item.getType().equals("Extinguisher")) {
            return 1;
        } else if (item.getType().equals("Sandbag") || item.getType().equals("Gravelbag") || item.getType().equals("Dirtbag")) {
            return 1;
        } else {
            return item.isWaterSource() ? 10 : 10000;
        }
    }

    public static int getWaterUsesInteger(InventoryItem item) {
        float FluidContainerMillilitresPerUse = 100.0F;
        if (item == null) {
            return 0;
        } else {
            if (item.hasComponent(ComponentType.FluidContainer)) {
                FluidContainer fluidContainer = item.getFluidContainer();
                if (fluidContainer.getPrimaryFluid() == null) {
                    return 0;
                }

                FluidType fluidTypeString = fluidContainer.getPrimaryFluid().getFluidType();
                if (fluidTypeString == FluidType.Water || fluidTypeString == FluidType.TaintedWater) {
                    float millilitres = fluidContainer.getAmount() * 1000.0F;
                    return (int)Math.floor(millilitres / 100.0F);
                }
            }

            return item.IsDrainable() && item.isWaterSource() ? item.getCurrentUses() : 0;
        }
    }
}
