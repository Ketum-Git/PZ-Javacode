// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.recipemanager;

import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.entity.components.fluids.FluidConsume;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Food;

public class UsedItemProperties {
    boolean tainted;
    boolean cooked;
    boolean burnt;
    int poisonLevel = -1;
    int poisonPower;
    boolean rotten;
    boolean stale;
    float condition;
    float rottenness;
    int itemUsed;
    int foodUsed;

    protected void reset() {
        this.tainted = false;
        this.cooked = false;
        this.burnt = false;
        this.poisonLevel = -1;
        this.poisonPower = 0;
        this.rotten = false;
        this.stale = false;
        this.condition = 0.0F;
        this.rottenness = 0.0F;
        this.itemUsed = 0;
        this.foodUsed = 0;
    }

    protected void addFluidConsume(FluidConsume fluidConsume) {
    }

    protected void addInventoryItem(InventoryItem usedItem) {
        if (usedItem instanceof Food food) {
            if (food.isTainted()) {
                this.tainted = true;
            }

            if (usedItem.isCooked()) {
                this.cooked = true;
            }

            if (usedItem.isBurnt()) {
                this.burnt = true;
            }

            if (food.getPoisonDetectionLevel() >= 0) {
                if (this.poisonLevel == -1) {
                    this.poisonLevel = food.getPoisonDetectionLevel();
                } else {
                    this.poisonLevel = PZMath.min(this.poisonLevel, food.getPoisonDetectionLevel());
                }
            }

            this.poisonPower = PZMath.max(this.poisonPower, food.getPoisonPower());
            this.foodUsed++;
            if (usedItem.getAge() > usedItem.getOffAgeMax()) {
                this.rotten = true;
            } else if (!this.rotten && usedItem.getOffAgeMax() < 1000000000) {
                if (usedItem.getAge() < usedItem.getOffAge()) {
                    this.rottenness = this.rottenness + 0.5F * usedItem.getAge() / usedItem.getOffAge();
                } else {
                    this.stale = true;
                    this.rottenness = this.rottenness
                        + (0.5F + 0.5F * (usedItem.getAge() - usedItem.getOffAge()) / (usedItem.getOffAgeMax() - usedItem.getOffAge()));
                }
            }
        }

        this.condition = this.condition + (float)usedItem.getCondition() / usedItem.getConditionMax();
        this.itemUsed++;
    }

    protected void transferToResults(ArrayList<InventoryItem> resultItems, ArrayList<InventoryItem> usedItems) {
        this.rottenness = this.rottenness / this.foodUsed;

        for (int i = 0; i < resultItems.size(); i++) {
            InventoryItem resultItem = resultItems.get(i);
            if (resultItem instanceof Food foodItem && foodItem.isCookable()) {
                foodItem.setCooked(this.cooked);
                foodItem.setBurnt(this.burnt);
                foodItem.setPoisonDetectionLevel(this.poisonLevel);
                foodItem.setPoisonPower(this.poisonPower);
                if (this.tainted) {
                    foodItem.setTainted(true);
                }
            }

            if (resultItem.getOffAgeMax() != 1.0E9) {
                if (this.rotten) {
                    resultItem.setAge(resultItem.getOffAgeMax());
                } else {
                    if (this.stale && this.rottenness < 0.5F) {
                        this.rottenness = 0.5F;
                    }

                    if (this.rottenness < 0.5F) {
                        resultItem.setAge(2.0F * this.rottenness * resultItem.getOffAge());
                    } else {
                        resultItem.setAge(resultItem.getOffAge() + 2.0F * (this.rottenness - 0.5F) * (resultItem.getOffAgeMax() - resultItem.getOffAge()));
                    }
                }
            }

            resultItem.setCondition(Math.round(resultItem.getConditionMax() * (this.condition / this.itemUsed)));

            for (int j = 0; j < usedItems.size(); j++) {
                InventoryItem usedItem = usedItems.get(j);
                resultItem.setConditionFromModData(usedItem);
                if (resultItem.getScriptItem() == usedItem.getScriptItem() && usedItem.isFavorite()) {
                    resultItem.setFavorite(true);
                }
            }
        }
    }
}
