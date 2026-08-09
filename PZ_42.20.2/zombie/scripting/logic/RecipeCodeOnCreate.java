// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.logic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaManager;
import zombie.characters.HaloTextHelper;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.skills.PerkFactory;
import zombie.core.Color;
import zombie.core.Translator;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.Texture;
import zombie.entity.components.crafting.InputFlag;
import zombie.entity.components.crafting.recipe.CraftRecipeData;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidSample;
import zombie.entity.components.fluids.FluidType;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Key;
import zombie.inventory.types.Literature;
import zombie.inventory.types.Radio;
import zombie.inventory.types.WeaponPart;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.radio.devices.DeviceData;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ModelKey;
import zombie.scripting.objects.MoodleType;
import zombie.util.StringUtils;

@UsedFromLua
public class RecipeCodeOnCreate extends RecipeCodeHelper {
    private static final List<ItemKey> mysteryCans = List.of(
        ItemKey.Food.CANNED_BOLOGNESE_OPEN,
        ItemKey.Food.CANNED_CARROTS_OPEN,
        ItemKey.Food.CANNED_CHILI_OPEN,
        ItemKey.Food.CANNED_CORN_OPEN,
        ItemKey.Food.CANNED_FRUIT_COCKTAIL_OPEN,
        ItemKey.Food.CANNED_PEACHES_OPEN,
        ItemKey.Food.CANNED_PEAS_OPEN,
        ItemKey.Food.CANNED_PINEAPPLE_OPEN,
        ItemKey.Food.CANNED_POTATO_OPEN,
        ItemKey.Food.CANNED_TOMATO_OPEN,
        ItemKey.Food.DOGFOOD_OPEN,
        ItemKey.Food.OPEN_BEANS
    );

    public static void makeCoffee(CraftRecipeData data, IsoGameCharacter character) {
        if (data.getAllCreatedItems().getFirst() instanceof Food result) {
            result.setName(Translator.getText("ContextMenu_FoodType_Coffee") + " " + Translator.getText("ContextMenu_EvolvedRecipe_HotDrink"));
            result.addExtraItem(ItemKey.Food.COFFEE_2);
            result.setCooked(true);
            result.setHeat(2.5F);
            result.setBaseHunger(-0.05F);
            result.setHungChange(-0.05F);
            result.setFatigueChange(-0.25F);
            data.getAllConsumedItems().set(0, result);
        }
    }

    public static void refillBlowTorch(CraftRecipeData data, IsoGameCharacter character) {
        if (data.getAllCreatedItems().getFirst() instanceof DrainableComboItem newTorch
            && data.getAllConsumedItems().getFirst() instanceof DrainableComboItem oldTorch
            && data.getAllKeepInputItems().getFirst() instanceof DrainableComboItem propaneTank) {
            newTorch.setCurrentUsesFloat(oldTorch.getCurrentUsesFloat());
            newTorch.setCondition(oldTorch.getCondition());
            double maxPropaneInTorch = newTorch.getMaxUses() * ZomboidGlobals.refillBlowtorchPropaneAmount;
            double currentPropaneInTorch = newTorch.getCurrentUsesFloat() * maxPropaneInTorch;
            double propaneToTransfer = Math.min(maxPropaneInTorch - currentPropaneInTorch, (double)propaneTank.getCurrentUses());
            if (propaneToTransfer > 0.0) {
                newTorch.setCurrentUsesFloat((float)((currentPropaneInTorch + propaneToTransfer) / maxPropaneInTorch));
                propaneTank.setCurrentUses((int)Math.round(propaneTank.getCurrentUses() - propaneToTransfer));
                propaneTank.syncItemFields();
            }

            newTorch.syncItemFields();
        }
    }

    public static void refillLighter(CraftRecipeData data, IsoGameCharacter character) {
        if (data.getAllKeepInputItems().getFirst() instanceof DrainableComboItem lighter
            && data.getAllConsumedItems().getFirst() instanceof DrainableComboItem lighterFluid) {
            lighter.setCurrentUsesFloat(lighterFluid.getCurrentUsesFloat() + 0.3F);
        }
    }

    public static void setEcruColor(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem result = data.getAllCreatedItems().getFirst();
        setColor(result, new Color(0.76F, 0.7F, 0.5F));
    }

    public static void torchBatteryInsert(CraftRecipeData data, IsoGameCharacter character) {
        DrainableComboItem battery = (DrainableComboItem)data.getAllConsumedItems().getFirst();
        Object item = data.getAllKeepInputItems().getFirst();
        if (item instanceof DrainableComboItem flashlight) {
            flashlight.setCurrentUsesFrom(battery);
        } else if (item instanceof WeaponPart flashlight) {
            flashlight.setCurrentUsesFrom(battery);
        } else if (item instanceof Radio radio) {
            radio.getDeviceData().setPower(battery.getCurrentUsesFloat());
            radio.getDeviceData().setHasBattery(true);
            radio.getDeviceData().transmitBatteryChangeServer();
        }
    }

    public static void dismantleFlashlight(CraftRecipeData data, IsoGameCharacter character) {
        for (InventoryItem item : data.getAllConsumedItems()) {
            if (item instanceof DrainableComboItem torch
                && (torch.hasTag(ItemTag.FLASHLIGHT) || torch.hasTag(ItemTag.USES_BATTERY))
                && torch.getCurrentUsesFloat() > 0.0F) {
                DrainableComboItem battery = addItemToCharacterInventory(character, ItemKey.Drainable.BATTERY);
                battery.setCurrentUsesFloat(torch.getCurrentUsesFloat());
                battery.syncItemFields();
            }
        }
    }

    public static void inheritColorFromMaterial(CraftRecipeData data, IsoGameCharacter character) {
        Color color = findInheritedColor(data);
        if (color != null) {
            ArrayList<InventoryItem> results = data.getAllCreatedItems();
            if (results.isEmpty()) {
                results = data.getAllInputItemsWithFlag(InputFlag.IsNotWorn);
            }

            for (InventoryItem result : results) {
                setColor(result, color);
            }
        }
    }

    private static Color findInheritedColor(CraftRecipeData data) {
        InventoryItem item = data.getFirstInputItemWithFlag(InputFlag.InheritColor);
        if (item != null) {
            return item.getColor();
        } else {
            FluidSample fluid = data.getFirstInputFluidWithFlag(InputFlag.InheritColor);
            return fluid != null ? fluid.getColor() : null;
        }
    }

    public static void shotgunSawnoff(CraftRecipeData data, IsoGameCharacter character) {
        HandWeapon result = (HandWeapon)data.getAllCreatedItems().getFirst();
        List<HandWeapon> items = getConsumedItems(data, ItemKey.Weapon.SHOTGUN, ItemKey.Weapon.DOUBLE_BARREL_SHOTGUN);
        Iterator var4 = items.iterator();
        if (var4.hasNext()) {
            HandWeapon weapon = (HandWeapon)var4.next();
            result.copyModData(result.getModData());

            for (WeaponPart part : weapon.getAllWeaponParts()) {
                tryAttachPart(result, part, character);
            }
        }
    }

    private static void tryAttachPart(HandWeapon weapon, WeaponPart part, IsoGameCharacter player) {
        if (part.canAttach(null, weapon)) {
            weapon.attachWeaponPart(player, part);
        } else if (player != null) {
            player.getInventory().addItem(part);
        }
    }

    public static void inheritFoodNameBowl(CraftRecipeData data, IsoGameCharacter character) {
        for (Food foodInput : getConsumedItems(data, Food.class)) {
            for (Food foodOutput : getCreatedItems(data, Food.class)) {
                if (!foodInput.isCustomName() && !StringUtils.isNullOrEmpty(foodInput.getEvolvedRecipeName())) {
                    foodOutput.setName(Translator.getText("Tooltip_food_Bowl", foodInput.getEvolvedRecipeName()));
                } else {
                    String itemName = foodInput.getDisplayName();
                    if (itemName.contains("Pasta") || itemName.contains("Rice")) {
                        itemName = itemName.contains("Pasta") ? "Pasta" : "Rice";
                    }

                    foodOutput.setName(Translator.getText("Tooltip_food_Bowl", itemName));
                }

                foodOutput.setCustomName(true);
            }
        }
    }

    public static void inheritFoodDisplayName(CraftRecipeData data, IsoGameCharacter character) {
        for (InventoryItem item : data.getAllConsumedItems()) {
            if (item instanceof Food foodInput && !StringUtils.isNullOrEmpty(foodInput.getDisplayName())) {
                for (InventoryItem result : data.getAllCreatedItems()) {
                    if (result instanceof Food foodOutput) {
                        foodOutput.setName(Translator.getText("Tooltip_food_Slice", foodInput.getDisplayName()));
                        foodOutput.setCustomName(true);
                    }
                }
            }
        }
    }

    public static void cutFish(CraftRecipeData data, IsoGameCharacter character) {
        Food fish = getConsumedItems(data, Food.class).getFirst();
        if (fish != null) {
            float fishWeight = fish.getActualWeight();
            float hunger = Math.max(fish.getBaseHunger(), fish.getHungChange());
            float gutWeightMult = 0.1F;
            float roeWeightMult = 0.0F;
            int roeChance = 10;
            String fishName = fish.getDisplayName();
            int currentMonth = GameTime.getInstance().getMonth();
            if (currentMonth == 3) {
                roeChance = 2;
            } else if (currentMonth == 2 || currentMonth == 4) {
                roeChance = 4;
            }

            if (Rand.NextBool(roeChance)) {
                if (fishName.contains(Translator.getText("IGUI_Fish_Legendary"))) {
                    roeWeightMult = 0.15F;
                } else if (fishName.contains(Translator.getText("IGUI_Fish_Big"))) {
                    roeWeightMult = 0.1F;
                } else {
                    roeWeightMult = 0.05F;
                }
            }

            if (!fish.isCooked()) {
                Food guts = InventoryItemFactory.CreateItem(ItemKey.Food.FISH_GUTS);
                float gutHunger = Math.max(guts.getBaseHunger(), guts.getHungChange());
                guts.setWeight(fishWeight * 0.1F);
                guts.setBaseHunger(gutHunger);
                guts.setHungChange(gutHunger);
                addItemToCharacterInventory(character, guts);
                if (roeWeightMult > 0.0F) {
                    Food roe = InventoryItemFactory.CreateItem(ItemKey.Food.FISH_ROE_SAC);
                    float roeHunger = hunger * roeWeightMult;
                    roe.setWeight(fishWeight * roeWeightMult);
                    roe.setBaseHunger(roeHunger);
                    roe.setHungChange(roeHunger);
                    addItemToCharacterInventory(character, roe);
                }
            }

            for (InventoryItem result : data.getAllCreatedItems()) {
                if (result instanceof Food resultFood) {
                    resultFood.setBaseHunger(hunger / 2.0F);
                    resultFood.setHungChange(hunger / 2.0F);
                    resultFood.setActualWeight(fishWeight * (0.1F + roeWeightMult) / 2.0F);
                    resultFood.setWeight(resultFood.getActualWeight());
                    resultFood.setCustomWeight(true);
                    resultFood.setCarbohydrates(fish.getCarbohydrates() / 2.0F);
                    resultFood.setLipids(fish.getLipids() / 2.0F);
                    resultFood.setProteins(fish.getProteins() / 2.0F);
                    resultFood.setCalories(fish.getCalories() / 2.0F);
                    resultFood.setCooked(fish.isCooked());
                }
            }
        }
    }

    public static void makeJar(CraftRecipeData data, IsoGameCharacter character) {
        copyFoodValuesFromList(data, getConsumedItems(data, Food.class));
        modifyLidCondition(data, character);
        data.getAllCreatedItems().getFirst().setCustomWeight(true);
        data.getAllCreatedItems().getFirst().setWeight(0.8F);
    }

    private static void modifyLidCondition(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem item = getConsumedItems(data, ItemKey.Normal.JAR_LID).getFirst();
        InventoryItem result = data.getAllCreatedItems().getFirst();
        KahluaTableImpl mData = (KahluaTableImpl)result.getModData();
        if (Rand.Next(11) >= character.getPerkLevel(PerkFactory.Perks.Cooking)) {
            mData.rawset("LidCondition", item.getCondition() - 1);
        } else {
            mData.rawset("LidCondition", item.getCondition());
        }

        result.syncItemFields();
    }

    public static void applyLidCondition(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem lid = InventoryItemFactory.CreateItem(ItemKey.Normal.JAR_LID);
        KahluaTableImpl mData = (KahluaTableImpl)data.getAllConsumedItems().getFirst().getModData();
        int cond = mData.rawgetInt("LidCondition");
        if (cond == -1) {
            cond = 9;
        }

        lid.setCondition(cond);
        addItemToCharacterInventory(character, lid);
    }

    public static void makeSushi(CraftRecipeData data, IsoGameCharacter character) {
        List<Food> consumedRiceItems = RecipeCodeHelper.getConsumedItems(data, ItemTag.RICE_RECIPE);
        if (!consumedRiceItems.isEmpty()) {
            float riceNeeded = 0.0F;
            float riceAmount = 0.0F;
            float MINIMUM_HUNGER_VALUE = 0.01F;

            for (InputScript recipeInput : data.getRecipe().getInputs()) {
                if (recipeInput.isCookedFoodItem() && recipeInput.getItemTags().contains(ItemTag.RICE_RECIPE)) {
                    riceNeeded = recipeInput.getAmount() / 100.0F;
                    break;
                }
            }

            for (Food riceContainer : consumedRiceItems) {
                riceAmount = Math.abs(riceContainer.getHungChange());
                if (riceAmount - riceNeeded <= 0.01F) {
                    String riceContainerReturnType = riceContainer.getReplaceOnUseFullType();
                    if (riceContainerReturnType != null) {
                        InventoryItem emptyRiceContainer = InventoryItemFactory.CreateItem(riceContainerReturnType);
                        emptyRiceContainer.setCondition(riceContainer.getCondition());
                        addItemToCharacterInventory(character, emptyRiceContainer);
                    }

                    riceNeeded -= riceAmount;
                } else {
                    riceNeeded = 0.0F;
                }

                if (riceNeeded <= 0.01F) {
                    break;
                }
            }
        }
    }

    public static void name_muffins(CraftRecipeData data, IsoGameCharacter character) {
        String muffinName = data.getAllConsumedItems().getFirst().getDisplayName();
        if (data.getAllConsumedItems().getFirst().haveExtraItems()) {
            for (InventoryItem muffin : getCreatedItems(data, Food.class)) {
                muffin.setName(muffinName);
            }
        }
    }

    public static void cutSmallAnimal(CraftRecipeData data, IsoGameCharacter character) {
        List<Food> animals = getConsumedItems(data, Food.class);
        if (!animals.isEmpty()) {
            Food animal = animals.getFirst();

            for (InventoryItem result : data.getAllCreatedItems()) {
                if (result instanceof Food resultFood) {
                    resultFood.setBaseHunger(animal.getHungChange());
                    resultFood.setHungChange(animal.getHungChange());
                    resultFood.setCustomWeight(true);
                    resultFood.setWeight(animal.getWeight() * 0.7F);
                    resultFood.setActualWeight(animal.getActualWeight() * 0.7F);
                    resultFood.setLipids(animal.getLipids() * 0.75F);
                    resultFood.setProteins(animal.getProteins() * 0.75F);
                    resultFood.setCalories(animal.getCalories() * 0.75F);
                    resultFood.setCarbohydrates(animal.getCarbohydrates() * 0.75F);
                    resultFood.setUnhappyChange(animal.getUnhappyChange() * 0.75F);
                }
            }
        }
    }

    public static void createLogStack(CraftRecipeData data, IsoGameCharacter character) {
        List<InventoryItem> ropeItems = getConsumedItems(data, ItemTag.ROPE);
        KahluaTableImpl table = (KahluaTableImpl)LuaManager.platform.newTable();

        for (InventoryItem ropeItem : ropeItems) {
            table.rawset(table.delegate.size(), ropeItem.getFullType());
        }

        data.getAllCreatedItems().getFirst().getModData().rawset("ropeItems", table);
    }

    public static void splitLogStack(CraftRecipeData data, IsoGameCharacter character) {
        KahluaTableImpl ropeItems = (KahluaTableImpl)data.getAllConsumedItems().getFirst().getModData().rawget("ropeItems");
        KahluaTableIterator it = ropeItems.iterator();

        while (it.advance()) {
            InventoryItem rope = InventoryItemFactory.CreateItem((String)it.getValue());
            addItemToCharacterInventory(character, rope);
        }
    }

    public static void dismantleMiscElectronics(CraftRecipeData data, IsoGameCharacter character) {
        for (InventoryItem item : data.getAllConsumedItems()) {
            if (item instanceof Radio radio && radio.getDeviceData().getIsBatteryPowered() && radio.getCurrentUsesFloat() > 0.0F) {
                DrainableComboItem battery = addItemToCharacterInventory(character, ItemKey.Drainable.BATTERY);
                battery.setCurrentUsesFloat(radio.getCurrentUsesFloat());
                battery.syncItemFields();
            } else if (item.hasTag(ItemTag.USES_BATTERY) && item.getCurrentUsesFloat() > 0.0F) {
                DrainableComboItem battery = addItemToCharacterInventory(character, ItemKey.Drainable.BATTERY);
                battery.setCurrentUsesFloat(item.getCurrentUsesFloat());
                battery.syncItemFields();
            } else if (item.is(ItemKey.Normal.REMOTE) && Rand.Next(100) < 60) {
                DrainableComboItem battery = addItemToCharacterInventory(character, ItemKey.Drainable.BATTERY);
                battery.setCurrentUsesFloat(Rand.Next(0.01F, 1.0F));
                battery.syncItemFields();
            }
        }
    }

    public static void fixFishingRope(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem result = data.getAllCreatedItems().getFirst();

        for (InventoryItem item : data.getAllConsumedItems()) {
            if (item.hasTag(ItemTag.FISHING_LINE)) {
                result.getModData().rawset("fishing_LineType", item.getFullType());
            }

            if (item.hasTag(ItemTag.FISHING_HOOK)) {
                result.getModData().rawset("fishing_HookType", item.getFullType());
            }
        }
    }

    public static void makeOmelette(CraftRecipeData data, IsoGameCharacter character) {
        copyFoodValuesFromList(data, getConsumedItems(data, ItemTag.EGG));
    }

    private static void copyFoodValuesFromList(CraftRecipeData data, List<Food> foodList) {
        float hunger = 0.0F;
        float calories = 0.0F;
        float carbs = 0.0F;
        float protein = 0.0F;
        float lipids = 0.0F;

        for (Food food : foodList) {
            if (!food.isSpice() || food.is(ItemKey.Food.FISH_ROE)) {
                hunger += food.getBaseHunger();
                calories += food.getCalories();
                carbs += food.getCarbohydrates();
                protein += food.getProteins();
                lipids += food.getLipids();
            }
        }

        Food result = (Food)data.getAllCreatedItems().getFirst();
        result.setBaseHunger(hunger);
        result.setHungChange(hunger);
        result.setCalories(calories);
        result.setCarbohydrates(carbs);
        result.setProteins(protein);
        result.setLipids(lipids);
    }

    public static void dismantleElectronics(CraftRecipeData data, IsoGameCharacter character) {
        for (InventoryItem electronic : data.getAllConsumedItems()) {
            if (electronic.is(ItemKey.Radio.RADIO_RED, ItemKey.Radio.RADIO_BLACK)) {
                dismantleRadio(data, character);
            }

            if (electronic.is(
                ItemKey.Radio.WALKIE_TALKIE_1,
                ItemKey.Radio.WALKIE_TALKIE_2,
                ItemKey.Radio.WALKIE_TALKIE_3,
                ItemKey.Radio.WALKIE_TALKIE_4,
                ItemKey.Radio.WALKIE_TALKIE_5,
                ItemKey.Radio.MAN_PACK_RADIO,
                ItemKey.Radio.HAM_RADIO_1,
                ItemKey.Radio.HAM_RADIO_2
            )) {
                dismantleRadioTwoWay(data, character);
            }

            if (electronic.is(ItemKey.Radio.TV_ANTIQUE, ItemKey.Radio.TV_WIDE_SCREEN, ItemKey.Radio.TV_BLACK)) {
                dismantleRadioTV(data, character);
            }
        }
    }

    private static void dismantleRadioTwoWay(CraftRecipeData data, IsoGameCharacter character) {
        int success = 50 + character.getPerkLevel(PerkFactory.Perks.Electricity) * 5;
        if (Rand.Next(100) < success) {
            addItemToCharacterInventory(character, ItemKey.Normal.RADIO_TRANSMITTER);
        }

        if (Rand.Next(100) < success) {
            addItemToCharacterInventory(character, ItemKey.Normal.LIGHT_BULB_GREEN);
        }

        dismantleRadio(data, character);
    }

    private static void dismantleRadio(CraftRecipeData data, IsoGameCharacter character) {
        int success = 50 + character.getPerkLevel(PerkFactory.Perks.Electricity) * 5;
        getRadioBaseItems(character);
        if (Rand.Next(100) < success) {
            addItemToCharacterInventory(character, ItemKey.Normal.AMPLIFIER);
        }

        if (Rand.Next(100) < success) {
            addItemToCharacterInventory(character, ItemKey.Normal.LIGHT_BULB);
        }

        if (Rand.Next(100) < success) {
            addItemToCharacterInventory(character, ItemKey.Normal.RADIO_RECEIVER);
        }

        for (Radio item : getConsumedItems(data, Radio.class)) {
            item.getDeviceData().getBattery(character.getInventory());
            item.getDeviceData().getHeadphones(character.getInventory());
        }
    }

    private static void getRadioBaseItems(IsoGameCharacter character) {
        int rand = Rand.Next(3);

        for (int i = 0; i < rand; i++) {
            int randItem = Rand.Next(3);
            switch (randItem) {
                case 0:
                    addItemToCharacterInventory(character, ItemKey.Normal.ELECTRONICS_SCRAP);
                    break;
                case 1:
                    addItemToCharacterInventory(character, ItemKey.Normal.ELECTRIC_WIRE);
                    break;
                default:
                    addItemToCharacterInventory(character, ItemKey.Normal.ALUMINUM_FRAGMENTS);
            }
        }
    }

    private static void dismantleRadioTV(CraftRecipeData data, IsoGameCharacter character) {
        int success = 50 + character.getPerkLevel(PerkFactory.Perks.Electricity) * 5;
        getRadioBaseItems(character);
        if (Rand.Next(100) < success) {
            addItemToCharacterInventory(character, ItemKey.Normal.AMPLIFIER);
        }

        if (Rand.Next(100) < success) {
            addItemToCharacterInventory(character, ItemKey.Normal.LIGHT_BULB);
        }

        if (!data.getAllConsumedItems().getFirst().is(ItemKey.Radio.TV_ANTIQUE)) {
            if (Rand.Next(100) < success) {
                addItemToCharacterInventory(character, ItemKey.Normal.LIGHT_BULB_RED);
            }

            if (Rand.Next(100) < success) {
                addItemToCharacterInventory(character, ItemKey.Normal.LIGHT_BULB_GREEN);
            }
        }
    }

    private static float getRandomRadioValue(float valMin, float valMax, int perkLevel) {
        float range = valMax - valMin;
        return Rand.Next(range * ((perkLevel - 1) / 10.0F), range * (perkLevel / 10.0F)) + valMin;
    }

    public static void radioCraft(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem created = data.getAllCreatedItems().getFirst();
        if (created instanceof Radio result) {
            DeviceData deviceData = result.getDeviceData();
            int perk = character.getPerkLevel(PerkFactory.Perks.Electricity);
            int perkInvert = 10 - perk + 1;
            float actualWeight = result.getScriptItem().getActualWeight();
            if (actualWeight <= 3.0F) {
                result.setActualWeight(getRandomRadioValue(1.5F, 3.0F, perk));
            } else {
                result.setActualWeight(actualWeight);
            }

            result.setWeight(result.getActualWeight());
            result.setCustomWeight(true);
            deviceData.setUseDelta(getRandomRadioValue(0.007F, 0.3F, perkInvert));
            deviceData.setBaseVolumeRange(getRandomRadioValue(8.0F, 16.0F, perk));
            deviceData.setMinChannelRange(PZMath.fastfloor(getRandomRadioValue(200.0F, 88000.0F, perkInvert)));
            deviceData.setMaxChannelRange(PZMath.fastfloor(getRandomRadioValue(108000.0F, 1000000.0F, perk)));
            deviceData.setTransmitRange(PZMath.fastfloor(getRandomRadioValue(500.0F, 5000.0F, perk)));
            deviceData.setHasBattery(false);
            deviceData.setPower(0.0F);
            deviceData.transmitBatteryChange();
            if (perk == 10 && Rand.Next(100) < 25) {
                deviceData.setIsHighTier(true);
                deviceData.setTransmitRange(PZMath.fastfloor(getRandomRadioValue(5500.0F, 7500.0F, perk)));
                deviceData.setUseDelta(getRandomRadioValue(0.002F, 0.007F, perk));
            }
        }
    }

    public static void ripClothing(CraftRecipeData data, IsoGameCharacter character) {
        Clothing clothing = getConsumedItems(data, Clothing.class).getFirst();
        if (clothing != null && !StringUtils.isNullOrEmpty(clothing.getFabricType())) {
            KahluaTableImpl def = (KahluaTableImpl)LuaManager.env.rawget("ClothingRecipesDefinitions");
            String material = def.rawgetTable("FabricType").rawgetTable(clothing.getFabricType()).rawgetStr("material");
            String materialDirty = def.rawgetTable("FabricType").rawgetTable(clothing.getFabricType()).rawgetStr("materialDirty");
            int maxMaterial = Math.max(clothing.getNbrOfCoveredParts() - (clothing.getHolesNumber() + clothing.getPatchesNumber()), 1);
            int minMaterial = maxMaterial == 1 ? 1 : 2;
            int nbr = Rand.NextInclusive(minMaterial, maxMaterial);
            nbr = PZMath.clamp(nbr + character.getPerkLevel(PerkFactory.Perks.Tailoring) / 2, 1, maxMaterial);
            float totalGrime = Math.min(clothing.getDirtiness() + clothing.getBloodLevel(), 100.0F);
            float calculatedFilterLife = 1.0F - totalGrime / 100.0F;

            for (int i = 0; i < nbr; i++) {
                boolean dirty = Rand.Next(99) + 1 <= clothing.getDirtiness() + clothing.getBloodLevel();
                InventoryItem scrap = InventoryItemFactory.CreateItem(!dirty ? material : materialDirty);
                if (!dirty && scrap.is(ItemKey.Normal.RIPPED_SHEETS)) {
                    scrap.getModData().rawset("filterLife", calculatedFilterLife);
                    scrap.syncItemFields();
                }

                addItemToCharacterInventory(character, scrap);
            }

            if (clothing.hasTag(ItemTag.BUCKLE)) {
                addItemToCharacterInventory(character, ItemKey.Normal.BUCKLE);
            }
        }
    }

    public static void makeMilkFromPowder(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem bucket = data.getAllKeepInputItems().getFirst();
        bucket.getFluidContainer().addFluid(FluidType.AnimalMilk, bucket.getFluidContainer().getCapacity());
        bucket.sendSyncEntity(null);
    }

    public static void purifyWater(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem bucket = data.getAllKeepInputItems().getFirst();
        float taintedAmount = bucket.getFluidContainer().getSpecificFluidAmount(Fluid.TaintedWater);
        float amount = Math.min(1.0F, taintedAmount);
        bucket.getFluidContainer().adjustSpecificFluidAmount(Fluid.TaintedWater, taintedAmount - amount);
        bucket.getFluidContainer().addFluid(Fluid.Water, amount);
        bucket.sendSyncEntity(null);
    }

    public static void carveSpear(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem createdSpear = data.getAllCreatedItems().getFirst();
        setRandomSpearCondition(character, createdSpear, 2);
    }

    public static void fireHardenSpear(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem result = data.getAllCreatedItems().getFirst();
        result.copyConditionStatesFrom(data.getAllConsumedItems().getFirst());
        setRandomSpearCondition(character, result, result.getCondition());
    }

    private static void setRandomSpearCondition(IsoGameCharacter character, InventoryItem result, int defaultCond) {
        int conditionMax = defaultCond + character.getPerkLevel(PerkFactory.Perks.Carving);
        conditionMax = Rand.Next(conditionMax, conditionMax + 2);
        result.setCondition(PZMath.clamp(conditionMax, 2, result.getConditionMax()));
    }

    public static void dismantleSpear(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem spear = data.getAllConsumedItems().getFirst();

        for (InventoryItem result : data.getAllCreatedItems()) {
            if (result.is(ItemKey.Weapon.LONG_STICK)) {
                result.setConditionFrom(spear);
            } else if (!result.is(ItemKey.Weapon.LONG_STICK_BROKEN)) {
                result.setConditionFromHeadCondition(spear);
            }
        }
    }

    public static void openCan(CraftRecipeData data, IsoGameCharacter character) {
        if (character != null) {
            InventoryItem opener = null;
            boolean canOpener = false;
            boolean chippedStone = false;

            for (InventoryItem item : data.getAllConsumedItems()) {
                if (item.is(ItemKey.Normal.SHARPED_STONE)) {
                    chippedStone = true;
                }

                if (item.hasTag(ItemTag.CAN_OPENER)) {
                    canOpener = true;
                }

                if (item instanceof HandWeapon || item.hasTag(ItemTag.SHARP_KNIFE) || item.is(ItemKey.Normal.SHARPED_STONE)) {
                    opener = item;
                    item.checkSyncItemFields(item.damageCheck());
                }

                if (!canOpener) {
                    int woundChance = 3;
                    if (chippedStone) {
                        woundChance++;
                    }

                    if (character.hasTrait(CharacterTrait.DEXTROUS)) {
                        woundChance -= 2;
                    }

                    if (character.hasTrait(CharacterTrait.CLUMSY)) {
                        woundChance += 2;
                    }

                    if (character.getPerkLevel(PerkFactory.Perks.SmallBlade) > 5) {
                        woundChance -= 2;
                    } else if (character.getPerkLevel(PerkFactory.Perks.SmallBlade) > 3) {
                        woundChance--;
                    } else if (character.getPerkLevel(PerkFactory.Perks.Cooking) < 1) {
                        woundChance++;
                    }

                    int roll = 20;
                    if (woundChance < 1) {
                        woundChance = 1;
                        roll = 30;
                    }

                    if (Rand.Next(roll) <= woundChance) {
                        HandWeapon weapon;
                        if (opener instanceof HandWeapon handWeapon) {
                            weapon = handWeapon;
                        } else {
                            weapon = InventoryItemFactory.CreateItem(ItemKey.Weapon.KITCHEN_KNIFE);
                        }

                        character.getBodyDamage().DamageFromWeapon(weapon, BodyPartType.ToIndex(BodyPartType.Hand_L));
                    }
                }
            }
        }
    }

    public static void openMysteryCan(CraftRecipeData data, IsoGameCharacter character) {
        Food food = character.getInventory().addItem(Rand.Next(mysteryCans));
        food.setTexture(Texture.getSharedTexture("Item_CannedUnlabeled_Open"));
        food.setWorldStaticModel(ModelKey.TIN_CAN_EMPTY);
        food.setStaticModel(ModelKey.MYSTERY_CAN_OPEN);
        food.getModData().rawset("NoLabel", true);
        if (GameServer.server) {
            GameServer.sendAddItemToContainer(character.getInventory(), food);
            KahluaTableImpl table = (KahluaTableImpl)LuaManager.platform.newTable();
            table.rawset("itemId", (double)food.getID());
            GameServer.sendServerCommand((IsoPlayer)character, "recipe", "OpenMysteryCan", table);
        }
    }

    public static void openMysteryCanKnife(CraftRecipeData data, IsoGameCharacter character) {
        openCan(data, character);
        openMysteryCan(data, character);
    }

    public static void openWaterCan(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem waterCan = character.getInventory().addItem(ItemKey.Normal.WATER_RATION_CAN_EMPTY);
        waterCan.getFluidContainer().addFluid(FluidType.Water, 0.3F);
        if (GameServer.server) {
            GameServer.sendAddItemToContainer(character.getInventory(), waterCan);
        }
    }

    public static void openWaterCanKnife(CraftRecipeData data, IsoGameCharacter character) {
        openCan(data, character);
        openWaterCan(data, character);
    }

    public static void openDentedCan(CraftRecipeData data, IsoGameCharacter character) {
        Food result = character.getInventory().addItem(Rand.Next(mysteryCans));
        result.setTexture(Texture.getSharedTexture("Item_CannedUnlabeled_Gross"));
        ModelKey modelName = ModelKey.DENTED_CAN_OPEN;
        result.getModData().rawset("NoLabel", true);
        if (Rand.NextBool(10)) {
            result.setAge(result.getOffAgeMax());
            result.setRotten(true);
            modelName = ModelKey.DENTED_CAN_OPEN_GROSS;
        } else if (!Rand.NextBool(10)) {
            result.setAge(Rand.Next(result.getOffAge(), result.getOffAgeMax()));
        }

        if (!result.isFresh() && Rand.NextBool(4)) {
            result.setPoisonPower(Rand.Next(10));
            result.setPoisonDetectionLevel(Rand.Next(5));
        }

        result.setStaticModel(modelName);
        result.setWorldStaticModel(modelName);
        if (GameServer.server) {
            GameServer.sendAddItemToContainer(character.getInventory(), result);
            KahluaTableImpl table = (KahluaTableImpl)LuaManager.platform.newTable();
            table.rawset("itemId", (double)result.getID());
            table.rawset("modelName", modelName.toString());
            GameServer.sendServerCommand((IsoPlayer)character, "recipe", "OpenDentedCan", table);
        }
    }

    public static void openDentedCanKnife(CraftRecipeData data, IsoGameCharacter character) {
        openCan(data, character);
        openDentedCan(data, character);
    }

    public static void addToPack(CraftRecipeData data, IsoGameCharacter character) {
        DrainableComboItem item = getKeepItems(data, DrainableComboItem.class).getFirst();
        item.setCurrentUses(item.getCurrentUses() + 1);
    }

    public static void drawRandomCard(CraftRecipeData data, IsoGameCharacter character) {
        String card = Translator.getText(ServerOptions.getRandomCard());
        IsoPlayer player = (IsoPlayer)character;
        if (!GameServer.server && !GameClient.client) {
            HaloTextHelper.addGoodText(player, card);
        }

        if (GameServer.server) {
            KahluaTableImpl table = (KahluaTableImpl)LuaManager.platform.newTable();
            table.rawset("onlineID", (double)player.getOnlineID());
            table.rawset("type", 4.0);
            table.rawset("text", card);
            GameServer.sendServerCommandToRelevant(player.getX(), player.getY(), "recipe", "SayText", table);
        }
    }

    public static void rollDice(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem dice = data.getAllKeepInputItems().getFirst();
        String diceName = Translator.getText("IGUI_RollDice", dice.getDisplayName());
        int roll = 0;
        if (dice.hasTag(ItemTag.D4)) {
            roll = Rand.NextInclusive(1, 4);
        } else if (dice.hasTag(ItemTag.D6)) {
            roll = Rand.NextInclusive(1, 6);
        } else if (dice.hasTag(ItemTag.D8)) {
            roll = Rand.NextInclusive(1, 8);
        } else if (dice.hasTag(ItemTag.D10)) {
            roll = Rand.NextInclusive(1, 10);
        } else if (dice.hasTag(ItemTag.D12)) {
            roll = Rand.NextInclusive(1, 12);
        } else if (dice.hasTag(ItemTag.D20)) {
            roll = Rand.NextInclusive(1, 20);
        } else if (dice.hasTag(ItemTag.D00)) {
            roll = Rand.NextInclusive(1, 100);
        }

        IsoPlayer player = (IsoPlayer)character;
        if (!GameServer.server && !GameClient.client) {
            HaloTextHelper.addGoodText(player, String.valueOf(roll));
        }

        if (GameServer.server) {
            KahluaTableImpl table = (KahluaTableImpl)LuaManager.platform.newTable();
            table.rawset("onlineID", (double)player.getOnlineID());
            table.rawset("type", 1.0);
            table.rawset("rollText", String.valueOf(roll));
            table.rawset("diceNameText", diceName);
            GameServer.sendServerCommandToRelevant(player.getX(), player.getY(), "recipe", "SayText", table);
        }
    }

    public static void dismantleFishingNet(CraftRecipeData data, IsoGameCharacter character) {
        data.getAllCreatedItems().getFirst().setCurrentUses(Rand.NextInclusive(1, 5));
    }

    public static void refillHurricaneLantern(CraftRecipeData data, IsoGameCharacter character) {
        ArrayList<InventoryItem> items = data.getAllConsumedItems();
        items.addAll(data.getAllKeepInputItems());
        DrainableComboItem result = getCreatedItems(data, DrainableComboItem.class).getFirst();
        InventoryItem petrol = getKeepItems(data, Fluid.Petrol).getFirst();
        DrainableComboItem lantern = getConsumedItems(
                data,
                ItemKey.Drainable.LANTERN_HURRICANE,
                ItemKey.Drainable.LANTERN_HURRICANE_COPPER,
                ItemKey.Drainable.LANTERN_HURRICANE_FORGED,
                ItemKey.Drainable.LANTERN_HURRICANE_GOLD
            )
            .getFirst();
        int usePer100ml = result.getMaxUses() / 10;
        result.setCurrentUses(lantern.getCurrentUses() + usePer100ml);

        while (result.getCurrentUses() < result.getMaxUses() && petrol.getFluidContainer().getAmount() >= 0.1F) {
            result.setCurrentUses(Math.min(result.getCurrentUses() + usePer100ml, result.getMaxUses()));
            petrol.getFluidContainer().adjustAmount(petrol.getFluidContainer().getAmount() - 0.1F);
        }
    }

    public static void scratchTicket(CraftRecipeData data, IsoGameCharacter character) {
        Literature result = getCreatedItems(data, Literature.class).getFirst();
        result.getModData().rawset("scratched", true);
        if (Rand.NextBool(5)) {
            scratchTicketWinner((IsoPlayer)character, result);
        } else {
            result.setName(Translator.getText("IGUI_ScratchingTicketNameLoser", result.getDisplayName()));
            result.setTexture(Texture.getSharedTexture("Item_ScratchTicket_Loser"));
            result.setWorldStaticModel(ModelKey.SCRATCH_TICKET_LOSER);
        }

        if (GameServer.server) {
            GameServer.sendReplaceItemInContainer(result.getContainer(), result, result);
        }
    }

    public static void removeGasFilter(CraftRecipeData data, IsoGameCharacter character) {
        removeTankOrFilter(
            character,
            getKeepItems(data, Clothing.class).getFirst(),
            InventoryItemFactory.CreateItem(getKeepItems(data, Clothing.class).getFirst().getFilterType()),
            true
        );
    }

    public static void removeOxygenTank(CraftRecipeData data, IsoGameCharacter character) {
        removeTankOrFilter(
            character,
            getKeepItems(data, Clothing.class).getFirst(),
            InventoryItemFactory.CreateItem(getKeepItems(data, Clothing.class).getFirst().getTankType()),
            false
        );
    }

    private static void removeTankOrFilter(IsoGameCharacter character, Clothing maskOrSuit, DrainableComboItem tankOrFilter, boolean isFilter) {
        if (tankOrFilter.is(ItemKey.Drainable.RAG_FILTER)) {
            if (maskOrSuit.getUsedDelta() >= 0.3F) {
                InventoryItem rag = InventoryItemFactory.CreateItem(ItemKey.Normal.RIPPED_SHEETS);
                rag.getModData().rawset("filterLife", maskOrSuit.getUsedDelta());
                addItemToCharacterInventory(character, rag);
            } else {
                addItemToCharacterInventory(character, ItemKey.Normal.RIPPED_SHEETS_DIRTY);
            }
        } else {
            tankOrFilter.setCurrentUsesFloat(maskOrSuit.getUsedDelta());
            addItemToCharacterInventory(character, tankOrFilter);
        }

        maskOrSuit.setCurrentUses(0);
        if (isFilter) {
            maskOrSuit.setNoFilter();
        } else {
            maskOrSuit.setNoTank();
        }

        if (!StringUtils.isNullOrEmpty(maskOrSuit.getWithoutDrainable())) {
            Clothing withoutDrainable = InventoryItemFactory.CreateItem(maskOrSuit.getWithoutDrainable());
            if (withoutDrainable != null) {
                maskOrSuit.setWorldStaticModel(withoutDrainable.getWorldStaticModel());
                maskOrSuit.setStaticModel(withoutDrainable.getStaticModel());
            }
        }

        IsoPlayer var5 = maskOrSuit.getPlayer();
        if (var5 instanceof IsoPlayer && var5.isEquipped(maskOrSuit)) {
            var5.resetModelNextFrame();
        }
    }

    public static void assignRagFilterLife(CraftRecipeData data, IsoGameCharacter character) {
        Clothing mask = (Clothing)data.getAllCreatedItems().getFirst();
        InventoryItem rag = data.getAllDestroyInputItems().getFirst();
        float filterLife = rag.getModData().rawget("filterLife") instanceof Number number ? number.floatValue() : 1.0F;
        mask.getModData().rawset("filterLife", (double)filterLife);
        mask.setUsedDelta(filterLife);
        mask.setCurrentUsesFloat(filterLife);
        mask.syncItemFields();
    }

    public static void addGasFilter(CraftRecipeData data, IsoGameCharacter character) {
        attachTankOrFilter((Clothing)data.getAllKeepInputItems().getFirst(), data.getAllDestroyInputItems().getFirst(), true);
    }

    public static void addOxygenTank(CraftRecipeData data, IsoGameCharacter character) {
        attachTankOrFilter((Clothing)data.getAllKeepInputItems().getFirst(), data.getAllDestroyInputItems().getFirst(), false);
    }

    private static void attachTankOrFilter(Clothing maskOrSuit, InventoryItem tankOrFilter, boolean isFilter) {
        float currentUsesRemaining = 0.0F;
        if (tankOrFilter.is(ItemKey.Normal.RIPPED_SHEETS)) {
            currentUsesRemaining = tankOrFilter.getModData().rawget("filterLife") instanceof Number number ? number.floatValue() : 1.0F;
            maskOrSuit.setFilterType(ItemKey.Drainable.RAG_FILTER.id());
        } else {
            currentUsesRemaining = tankOrFilter.getCurrentUsesFloat();
            if (isFilter) {
                maskOrSuit.setFilterType(tankOrFilter.getFullType());
            } else {
                maskOrSuit.setTankType(tankOrFilter.getFullType());
            }
        }

        maskOrSuit.setUsedDelta(currentUsesRemaining);
        maskOrSuit.setCurrentUsesFloat(maskOrSuit.getUsedDelta());
        maskOrSuit.setWorldStaticModel(maskOrSuit.getScriptItem().getWorldStaticModel());
        maskOrSuit.setStaticModel(maskOrSuit.getScriptItem().getStaticModel());
        IsoPlayer var7 = maskOrSuit.getPlayer();
        if (var7 instanceof IsoPlayer && var7.isEquipped(maskOrSuit)) {
            var7.resetModelNextFrame();
        }
    }

    public static void scrapJewellery(CraftRecipeData data, IsoGameCharacter character) {
        ItemContainer inv = character.getInventory();

        for (InventoryItem item : data.getAllConsumedItems()) {
            if (item.hasTag(ItemTag.DIAMOND_JEWELLERY)) {
                InventoryItem addedItem = inv.addItem(ItemKey.Normal.DIAMOND);
                if (GameServer.server) {
                    GameServer.sendAddItemToContainer(inv, addedItem);
                }
            }

            if (item.hasTag(ItemTag.TWO_DIAMOND_JEWELLERY)) {
                List<InventoryItem> addedItems = inv.addItems(ItemKey.Normal.DIAMOND, 2);
                if (GameServer.server) {
                    GameServer.sendAddItemsToContainer(inv, (ArrayList<InventoryItem>)addedItems);
                }
            }

            if (item.hasTag(ItemTag.EMERALD_JEWELLERY)) {
                InventoryItem addedItem = inv.addItem(ItemKey.Normal.EMERALD);
                if (GameServer.server) {
                    GameServer.sendAddItemToContainer(inv, addedItem);
                }
            }

            if (item.hasTag(ItemTag.TWO_EMERALD_JEWELLERY)) {
                List<InventoryItem> addedItems = inv.addItems(ItemKey.Normal.EMERALD, 2);
                if (GameServer.server) {
                    GameServer.sendAddItemsToContainer(inv, (ArrayList<InventoryItem>)addedItems);
                }
            }

            if (item.hasTag(ItemTag.RUBY_JEWELLERY)) {
                InventoryItem addedItem = inv.addItem(ItemKey.Normal.RUBY);
                if (GameServer.server) {
                    GameServer.sendAddItemToContainer(inv, addedItem);
                }
            }

            if (item.hasTag(ItemTag.TWO_RUBY_JEWELLERY)) {
                List<InventoryItem> addedItems = inv.addItems(ItemKey.Normal.RUBY, 2);
                if (GameServer.server) {
                    GameServer.sendAddItemsToContainer(inv, (ArrayList<InventoryItem>)addedItems);
                }
            }

            if (item.hasTag(ItemTag.SAPPHIRE_JEWELLERY)) {
                InventoryItem addedItem = inv.addItem(ItemKey.Normal.SAPPHIRE);
                if (GameServer.server) {
                    GameServer.sendAddItemToContainer(inv, addedItem);
                }
            }

            if (item.hasTag(ItemTag.TWO_SAPPHIRE_JEWELLERY)) {
                List<InventoryItem> addedItems = inv.addItems(ItemKey.Normal.SAPPHIRE, 2);
                if (GameServer.server) {
                    GameServer.sendAddItemsToContainer(inv, (ArrayList<InventoryItem>)addedItems);
                }
            }

            if (item.hasTag(ItemTag.AMETHYST_JEWELLERY)) {
                InventoryItem addedItem = inv.addItem(ItemKey.Normal.AMETHYST);
                if (GameServer.server) {
                    GameServer.sendAddItemToContainer(inv, addedItem);
                }
            }
        }
    }

    public static void replaceSawBlade(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem saw = data.getAllConsumedItems().get(0);
        InventoryItem newBlade = InventoryItemFactory.CreateItem(data.getAllConsumedItems().get(1).getFullType());
        newBlade.setCondition(saw.getCondition());
        addItemToCharacterInventory(character, newBlade);
    }

    public static void minorCondition(CraftRecipeData data, IsoGameCharacter character) {
        PerkFactory.Perk highestSkill = data.getRecipe().getHighestRelevantSkill(character);
        if (highestSkill == null) {
            highestSkill = data.getRecipe().getHighestRelevantSkillFromXpAward(character);
        }

        int skill = character.getPerkLevel(highestSkill);

        for (InventoryItem result : data.getAllCreatedItems()) {
            int condPerc = PZMath.clamp(Rand.Next(5 + skill * 10, 10 + skill * 20), 5, 100);
            int cond = Math.round(Math.max(result.getConditionMax() * (condPerc / 100.0F), result.getConditionMax() / 2.0F));
            if (skill >= 10) {
                cond = result.getConditionMax();
            }

            result.setCondition(cond);
            result.syncItemFields();
        }
    }

    public static void sharpenBlade(CraftRecipeData data, IsoGameCharacter character) {
        int damageChance = 10;
        InventoryItem whestone = getKeepItems(data, ItemTag.WHETSTONE, ItemTag.FILE).getFirst();
        if (whestone.hasTag(ItemTag.FILE)) {
            damageChance = 20;
        }

        InventoryItem item = data.getFirstInputItemWithFlag(InputFlag.IsSharpenable);
        if (item == null) {
            item = data.getFirstInputItemWithFlag(InputFlag.IsDamaged);
        }

        sharpenBladeGeneric(data, character, item, damageChance, whestone);
    }

    public static void sharpenBladeGrindstone(CraftRecipeData data, IsoGameCharacter character) {
        sharpenBladeGeneric(data, character, data.getFirstInputItemWithFlag(InputFlag.IsSharpenable), 5, null);
    }

    private static void sharpenBladeGeneric(CraftRecipeData data, IsoGameCharacter character, InventoryItem item, int damageChance, InventoryItem whestone) {
        damageChance -= character.getPerkLevel(PerkFactory.Perks.Maintenance);
        float sharpenMax = item.getMaxSharpness();
        if (item.hasSharpness()) {
            while (item.getSharpness() < sharpenMax && !item.isBroken() && (whestone == null || !whestone.isBroken())) {
                item.setSharpness(item.getSharpness() + 0.1F);
                if (Rand.Next(100) <= damageChance) {
                    if (item.hasHeadCondition()) {
                        item.setHeadCondition(item.getHeadCondition() - 1);
                    } else {
                        item.incrementCondition(-1);
                    }

                    sharpenMax = item.getMaxSharpness();
                }

                if (whestone != null && Rand.NextBool(whestone.getConditionLowerChance())) {
                    whestone.incrementCondition(-1);
                }
            }
        } else {
            while (item.getCondition() < item.getConditionMax() && (whestone == null || !whestone.isBroken())) {
                item.incrementCondition(1);
                if (whestone != null && Rand.NextBool(whestone.getConditionLowerChance())) {
                    whestone.incrementCondition(-1);
                }
            }
        }

        item.setSharpness(Math.max(item.getSharpness(), sharpenMax));
        item.syncItemFields();
        if (whestone != null) {
            whestone.syncItemFields();
        }
    }

    public static void genericFixing(CraftRecipeData data, IsoGameCharacter character) {
        genericFixer(data, character, 1, data.getFirstInputItemWithFlag(InputFlag.IsDamaged));
    }

    public static void genericBetterFixing(CraftRecipeData data, IsoGameCharacter character) {
        genericFixer(data, character, 2, data.getFirstInputItemWithFlag(InputFlag.IsDamaged));
    }

    public static void genericEvenBetterFixing(CraftRecipeData data, IsoGameCharacter character) {
        genericFixer(data, character, 3, data.getFirstInputItemWithFlag(InputFlag.IsDamaged));
    }

    private static void genericFixer(CraftRecipeData data, IsoGameCharacter character, int factor, InventoryItem item) {
        int skill = character.getPerkLevel(PerkFactory.Perks.Maintenance);
        int timesRepaired = item.getHaveBeenRepaired();
        item.setHaveBeenRepaired(timesRepaired + 1);
        int failChance = 25 - factor * 5;
        if (skill > 0) {
            failChance -= skill * 5;
        } else {
            failChance += 10;
        }

        failChance += timesRepaired * 2;
        failChance = PZMath.clamp(failChance, 0, 95);
        if (Rand.Next(100) <= failChance) {
            item.incrementCondition(-1);
            item.syncItemFields();
        } else {
            if (timesRepaired < 1) {
                timesRepaired = 1;
            }

            float percentFixed = (factor * 10 * (1.0F / timesRepaired) + Math.min(skill * 5, 25)) / 100.0F;
            float amountFixed = (item.getConditionMax() - item.getCondition()) * percentFixed;
            amountFixed = Math.max(1.0F, amountFixed);
            item.incrementCondition((int)amountFixed);
            item.syncItemFields();
        }
    }

    public static void sliceAnimalHead(CraftRecipeData data, IsoGameCharacter character) {
        Food head = getConsumedItems(data, ItemTag.ANIMAL_HEAD).getFirst();
        if (!head.isRotten()) {
            addItemToCharacterInventory(character, ItemKey.Food.ANIMAL_BRAIN);
        }
    }

    public static void cutChicken(CraftRecipeData data, IsoGameCharacter character) {
        Food chicken = getConsumedItems(data, Food.class).getFirst();
        float totalHunger = 0.0F;
        float wholeChickenHunger = Math.max(chicken.getBaseHunger(), chicken.getHungChange());
        List<Food> createdItems = getCreatedItems(data, Food.class);

        for (Food result : createdItems) {
            totalHunger += Math.max(result.getBaseHunger(), result.getHungChange());
        }

        float ratio = wholeChickenHunger / totalHunger;

        for (Food result : createdItems) {
            result.setBaseHunger(result.getBaseHunger() * ratio);
            result.setHungChange(result.getHungChange() * ratio);
            result.setActualWeight(result.getActualWeight() * 0.9F * ratio);
            result.setWeight(result.getActualWeight());
            result.setCustomWeight(true);
            result.setCarbohydrates(result.getCarbohydrates() * ratio);
            result.setLipids(result.getLipids() * ratio);
            result.setProteins(result.getProteins() * ratio);
            result.setCalories(result.getCalories() * ratio);
            result.setCooked(chicken.isCooked());
        }
    }

    public static void placeInBox(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem box = data.getAllCreatedItems().getFirst();
        List<DrainableComboItem> consumedItems = getConsumedItems(data, DrainableComboItem.class);

        for (int i = 0; i < consumedItems.size(); i++) {
            DrainableComboItem item = consumedItems.get(i);
            box.getModData().rawset("drainable" + i, item.getCurrentUsesFloat());
        }
    }

    public static void unpackBox(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem box = data.getAllConsumedItems().getFirst();
        List<DrainableComboItem> createdItems = getCreatedItems(data, DrainableComboItem.class);
        KahluaTableImpl modData = (KahluaTableImpl)box.getModData();

        for (int i = 0; i < createdItems.size(); i++) {
            DrainableComboItem item = createdItems.get(i);
            if (modData.rawget("drainable" + i) != null) {
                item.setCurrentUsesFloat(modData.rawgetFloat("drainable" + i));
            }
        }
    }

    public static void placeItemTypeInBox(CraftRecipeData data, IsoGameCharacter character) {
        for (int i = 0; i < getConsumedItems(data, InventoryItem.class).size(); i++) {
            InventoryItem packedItem = getConsumedItems(data, InventoryItem.class).get(i);
            data.getAllCreatedItems().getFirst().getModData().rawset("packedItem" + i, packedItem.getFullType());
            data.getAllCreatedItems().getFirst().getModData().rawset("numberOfPackedItems", getConsumedItems(data, InventoryItem.class).size());
        }
    }

    public static void unpackItemTypeFromBox(CraftRecipeData data, IsoGameCharacter character) {
        KahluaTableImpl modData = (KahluaTableImpl)data.getAllConsumedItems().getFirst().getModData();
        int numberOfItems = modData.rawgetInt("numberOfPackedItems");
        if (numberOfItems > 0) {
            data.getToOutputItems().clear();

            for (int i = 0; i < numberOfItems; i++) {
                String unpackedItem = modData.rawgetStr("packedItem" + i);
                if (unpackedItem == null || unpackedItem.isEmpty()) {
                    break;
                }

                InventoryItem item = InventoryItemFactory.CreateItem(unpackedItem);
                if (item != null) {
                    addItemToCharacterInventory(character, item);
                }
            }
        }
    }

    public static void knappFlake(CraftRecipeData data, IsoGameCharacter character) {
        int skill = character.getPerkLevel(PerkFactory.Perks.FlintKnapping);
        if (Rand.Next(11) < skill) {
            addItemToCharacterInventory(character, ItemKey.Normal.SHARPED_STONE);
        }
    }

    public static void slightlyMoreDurable(CraftRecipeData data, IsoGameCharacter character) {
        if (!getConsumedItems(data, ItemTag.INFERIOR_BINDING).isEmpty()) {
            data.getAllCreatedItems().getFirst().incrementCondition(1);
        }
    }

    public static void untieHeadband(CraftRecipeData data, IsoGameCharacter character) {
        if (data.getAllConsumedItems().getFirst() instanceof Clothing clothing && (clothing.isDirty() || clothing.isBloody())) {
            removeItemFromCharacterInventory(character, data.getAllCreatedItems().getFirst());
            addItemToCharacterInventory(character, ItemKey.Normal.LEATHER_STRIPS_DIRTY);
        }
    }

    public static void smeltIronOrSteelSmall(CraftRecipeData data, IsoGameCharacter character) {
        smeltIronOrSteel(data, character, 1);
    }

    public static void smeltIronOrSteelMedium(CraftRecipeData data, IsoGameCharacter character) {
        smeltIronOrSteel(data, character, 2);
    }

    public static void smeltIronOrSteelMediumPlus(CraftRecipeData data, IsoGameCharacter character) {
        smeltIronOrSteel(data, character, 3);
    }

    public static void smeltIronOrSteelLarge(CraftRecipeData data, IsoGameCharacter character) {
        smeltIronOrSteel(data, character, 4);
    }

    public static void smeltIronOrSteelIngot(CraftRecipeData data, IsoGameCharacter character) {
        smeltIronOrSteel(data, character, 12);
    }

    private static void smeltIronOrSteel(CraftRecipeData data, IsoGameCharacter character, int add) {
        int uses = 0;
        List<DrainableComboItem> consumedItems = getConsumedItems(data, DrainableComboItem.class);
        if (!consumedItems.isEmpty()) {
            uses = consumedItems.getFirst().getCurrentUses();
        }

        DrainableComboItem result = getCreatedItems(data, DrainableComboItem.class).getFirst();
        result.setCurrentUses(Math.min(result.getMaxUses(), uses + add));
        result.syncItemFields();
    }

    public static void sewHideJacket(CraftRecipeData data, IsoGameCharacter character) {
        InventoryItem result = data.getAllCreatedItems().getFirst();
        ItemVisual newVisual = result.getVisual();
        int skill = character.getPerkLevel(PerkFactory.Perks.Tailoring);
        if (skill >= 6 && Rand.Next(21) < skill) {
            newVisual.setBaseTexture(1);
            newVisual.setTextureChoice(1);
        } else {
            newVisual.setBaseTexture(0);
            newVisual.setTextureChoice(0);
        }
    }

    public static void pickAramidThread(CraftRecipeData data, IsoGameCharacter character) {
        Clothing item = getConsumedItems(data, ItemTag.PICK_ARAMID_THREAD).getFirst();
        int nbrOfCoveredParts = Math.max(1, item.getNbrOfCoveredParts() - (item.getHolesNumber() + item.getPatchesNumber()));
        int skill = character.getPerkLevel(PerkFactory.Perks.Tailoring) / 2;
        int nbr = Rand.Next(nbrOfCoveredParts, nbrOfCoveredParts * 2 + skill);
        nbr = PZMath.clamp(nbr, 1, 10);
        data.getAllCreatedItems().getFirst().setCurrentUses(nbr);
        if (item.hasTag(ItemTag.BUCKLE)) {
            addItemToCharacterInventory(character, ItemKey.Normal.BUCKLE);
        }
    }

    public static void openAndEat(CraftRecipeData data, IsoGameCharacter character) {
        if (!(data.getEatPercentage() <= 0.0F) && character.getMoodles().getMoodleLevel(MoodleType.FOOD_EATEN) < 3) {
            if (GameServer.server) {
                KahluaTable table = LuaManager.platform.newTable();
                table.rawset("onlineID", (double)character.getOnlineID());
                table.rawset("itemId", (double)data.getAllCreatedItems().getFirst().getID());
                table.rawset("eatPercentage", data.getEatPercentage() / 100.0);
                GameServer.sendServerCommand((IsoPlayer)character, "recipe", "openAndEat", table);
            } else {
                KahluaTableImpl eatFoodActionLua = (KahluaTableImpl)LuaManager.env.rawget("ISEatFoodAction");
                Object[] eatFoodActionCallback = LuaManager.caller
                    .pcall(
                        LuaManager.thread,
                        eatFoodActionLua.rawget("new"),
                        eatFoodActionLua,
                        character,
                        data.getAllCreatedItems().getFirst(),
                        data.getEatPercentage() / 100
                    );
                if ((Boolean)eatFoodActionCallback[0]) {
                    LuaManager.caller
                        .pcall(LuaManager.thread, ((KahluaTableImpl)LuaManager.env.rawget("ISTimedActionQueue")).rawget("add"), eatFoodActionCallback[1]);
                }
            }
        }
    }

    public static void copyKey(CraftRecipeData data, IsoGameCharacter character) {
        Key sourceKey = getInputItems(data, ItemTag.BUILDING_KEY).getFirst();
        data.getFirstCreatedItem().setKeyId(sourceKey.getKeyId());
    }

    public static void openMacAndCheese(CraftRecipeData data, IsoGameCharacter isoGameCharacter) {
        Food macaroni = getCreatedItems(data, ItemTag.PASTA).getFirst();
        macaroni.copyFoodFromSplit(macaroni, 6);
    }
}
