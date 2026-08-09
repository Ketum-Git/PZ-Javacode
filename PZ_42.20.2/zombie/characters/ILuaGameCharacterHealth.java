// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItem;

/**
 * ILuaGameCharacterHealth
 *   Provides the functions expected by LUA when dealing with objects of this type.
 */
public interface ILuaGameCharacterHealth {
    void setSleepingTabletEffect(float SleepingTabletEffect);

    float getSleepingTabletEffect();

    float getFatigueMod();

    boolean Eat(InventoryItem arg0, float arg1, boolean arg2);

    boolean Eat(InventoryItem info, float percentage);

    boolean Eat(InventoryItem info);

    boolean DrinkFluid(InventoryItem arg0, float arg1, boolean arg2);

    boolean DrinkFluid(InventoryItem arg0, float arg1);

    boolean DrinkFluid(InventoryItem arg0);

    boolean DrinkFluid(FluidContainer arg0, float arg1, boolean arg2);

    boolean DrinkFluid(FluidContainer arg0, float arg1);

    float getReduceInfectionPower();

    void setReduceInfectionPower(float reduceInfectionPower);

    int getLastHourSleeped();

    void setLastHourSleeped(int lastHourSleeped);

    void setTimeOfSleep(float timeOfSleep);
}
