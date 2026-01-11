// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.Translator;
import zombie.core.logger.LoggerManager;
import zombie.debug.DebugLog;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;

@UsedFromLua
public final class EvolvedRecipe extends BaseScriptObject {
    private static final DecimalFormat DECIMAL_FORMAT = (DecimalFormat)NumberFormat.getInstance(Locale.US);
    public String name;
    public String displayName;
    private String originalname;
    public int maxItems;
    public final Map<String, ItemRecipe> itemsList = new HashMap<>();
    public String resultItem;
    public String baseItem;
    public boolean cookable;
    public boolean addIngredientIfCooked;
    public boolean canAddSpicesEmpty;
    public String addIngredientSound;
    public boolean hidden;
    public boolean allowFrozenItem;
    public String template;
    public Float minimumWater = 0.0F;

    public EvolvedRecipe(String name) {
        super(ScriptType.EvolvedRecipe);
        this.name = name;
    }

    @Override
    public void Load(String name, String token) throws Exception {
        String[] waypoint = token.split("[{}]");
        String[] coords = waypoint[1].split(",");
        this.LoadCommonBlock(token);
        this.Load(name, coords);
    }

    private void Load(String name, String[] strArray) {
        this.displayName = Translator.getRecipeName(name);
        this.originalname = name;

        for (int i = 0; i < strArray.length; i++) {
            if (!strArray[i].trim().isEmpty() && strArray[i].contains("=")) {
                String[] split = strArray[i].split("=", 2);
                String key = split[0].trim();
                String value = split[1].trim();
                if (key.equals("BaseItem")) {
                    this.baseItem = value;
                } else if (key.equals("Name")) {
                    this.displayName = Translator.getRecipeName(value);
                    this.originalname = value;
                } else if (key.equals("ResultItem")) {
                    this.resultItem = value;
                    if (!value.contains(".")) {
                        this.resultItem = value;
                    }
                } else if (key.equals("Cookable")) {
                    this.cookable = true;
                } else if (key.equals("MaxItems")) {
                    this.maxItems = Integer.parseInt(value);
                } else if (key.equals("AddIngredientIfCooked")) {
                    this.addIngredientIfCooked = Boolean.parseBoolean(value);
                } else if (key.equals("AddIngredientSound")) {
                    this.addIngredientSound = StringUtils.discardNullOrWhitespace(value);
                } else if (key.equals("CanAddSpicesEmpty")) {
                    this.canAddSpicesEmpty = Boolean.parseBoolean(value);
                } else if (key.equals("IsHidden")) {
                    this.hidden = Boolean.parseBoolean(value);
                } else if (key.equals("AllowFrozenItem")) {
                    this.allowFrozenItem = Boolean.parseBoolean(value);
                } else if (key.equals("Template")) {
                    this.template = value;
                } else if (key.equals("MinimumWater")) {
                    this.minimumWater = Float.parseFloat(value);
                }
            }
        }

        if (this.template == null) {
            this.template = name;
        }
    }

    public boolean needToBeCooked(InventoryItem itemTest) {
        ItemRecipe itemRecipe = this.getItemRecipe(itemTest);
        return itemRecipe == null ? true : itemRecipe.cooked == itemTest.isCooked() || itemRecipe.cooked == itemTest.isBurnt() || !itemRecipe.cooked;
    }

    public ArrayList<InventoryItem> getItemsCanBeUse(IsoGameCharacter chr, InventoryItem baseItem, ArrayList<ItemContainer> containers) {
        int cookingLvl = chr.getPerkLevel(PerkFactory.Perks.Cooking);
        if (containers == null) {
            containers = new ArrayList<>();
        }

        ArrayList<InventoryItem> result = new ArrayList<>();
        if (!baseItem.haveExtraItems() && this.getMinimumWater() > 0.0F && !this.hasMinimumWater(baseItem)) {
            return result;
        } else if (!baseItem.haveExtraItems()
            && this.getMinimumWater() == 0.0F
            && baseItem.getFluidContainer() != null
            && !baseItem.getFluidContainer().isEmpty()) {
            return result;
        } else {
            Iterator<String> it = this.itemsList.keySet().iterator();
            if (!containers.contains(chr.getInventory())) {
                containers.add(chr.getInventory());
            }

            while (it.hasNext()) {
                String type = it.next();

                for (ItemContainer itemContainer : containers) {
                    this.checkItemCanBeUse(itemContainer, type, baseItem, cookingLvl, result, chr);
                }
            }

            if (baseItem.haveExtraItems() && baseItem.getExtraItems().size() >= 3) {
                IsoPlayer player = (IsoPlayer)chr;

                for (int c = 0; c < containers.size(); c++) {
                    ItemContainer container = containers.get(c);

                    for (int i = 0; i < container.getItems().size(); i++) {
                        InventoryItem item = container.getItems().get(i);
                        boolean noUseInRecipes = false;
                        if (player != null && item.isNoRecipes(player)) {
                            noUseInRecipes = true;
                        }

                        if (item instanceof Food food
                            && food.getPoisonLevelForRecipe() >= 0
                            && chr.isKnownPoison(item)
                            && !result.contains(item)
                            && !noUseInRecipes) {
                            result.add(item);
                        }
                    }
                }
            }

            return result;
        }
    }

    private void checkItemCanBeUse(
        ItemContainer itemContainer, String type, InventoryItem baseItem, int cookingLvl, ArrayList<InventoryItem> result, IsoGameCharacter chr
    ) {
        ArrayList<InventoryItem> itemsInChr = itemContainer.getItemsFromType(type);
        IsoPlayer player = (IsoPlayer)chr;

        for (int i = 0; i < itemsInChr.size(); i++) {
            InventoryItem item = itemsInChr.get(i);
            boolean ok = false;
            if (item instanceof Food itemFood && this.itemsList.get(type).use != -1) {
                if (itemFood.isSpice()) {
                    if (this.isResultItem(baseItem)) {
                        ok = !this.isSpiceAdded(baseItem, item);
                    } else if (this.canAddSpicesEmpty) {
                        ok = true;
                    }

                    if (itemFood.isBurnt()) {
                        ok = false;
                    } else if (itemFood.isRotten() && cookingLvl < 7) {
                        ok = false;
                    }
                } else if (!baseItem.haveExtraItems() || baseItem.extraItems.size() < this.maxItems) {
                    if (itemFood.isBurnt()) {
                        ok = false;
                    } else if (!itemFood.isRotten() || cookingLvl >= 7) {
                        ok = true;
                    }
                }

                if (itemFood.isFrozen() && !this.allowFrozenItem) {
                    ok = false;
                }

                if (itemFood.isbDangerousUncooked() && !itemFood.isCooked() && !InventoryItemFactory.<InventoryItem>CreateItem(this.resultItem).isCookable()) {
                }
            } else if (item.isSpice()) {
                if (this.isResultItem(baseItem)) {
                    ok = !this.isSpiceAdded(baseItem, item);
                } else if (this.canAddSpicesEmpty) {
                    ok = true;
                }
            } else {
                ok = true;
            }

            if (player != null && item.isNoRecipes(player)) {
                ok = false;
            }

            ItemRecipe itemRecipe = this.getItemRecipe(item);
            if (ok) {
                result.add(item);
            }
        }
    }

    public InventoryItem addItem(InventoryItem baseItem, InventoryItem usedItem, IsoGameCharacter chr) {
        int cookingLvl = chr.getPerkLevel(PerkFactory.Perks.Cooking);
        if (!this.isResultItem(baseItem)) {
            InventoryItem previousItem = baseItem instanceof Food ? baseItem : null;
            InventoryItem item = InventoryItemFactory.CreateItem(this.resultItem);
            if (item != null) {
                if (baseItem.getColorRed() != 1.0 || baseItem.getColorGreen() != 1.0 || baseItem.getColorBlue() != 1.0) {
                    item.setColorRed(baseItem.getColorRed());
                    item.setColorGreen(baseItem.getColorGreen());
                    item.setColorBlue(baseItem.getColorBlue());
                }

                if (baseItem.getModelIndex() != -1) {
                    item.setModelIndex(baseItem.getModelIndex());
                }

                if (baseItem instanceof HandWeapon) {
                    item.setConditionFrom(baseItem);
                    item.getModData().rawset("condition:" + baseItem.getType(), (double)baseItem.getCondition() / baseItem.getConditionMax());
                }

                InventoryItem oldBaseItem = baseItem;
                baseItem = item;
                if (item instanceof Food food) {
                    food.setCalories(0.0F);
                    food.setCarbohydrates(0.0F);
                    food.setProteins(0.0F);
                    food.setLipids(0.0F);
                    if (usedItem instanceof Food usedFood && usedFood.getPoisonLevelForRecipe() >= 0 && usedFood.isPoison()) {
                        this.addPoison(usedItem, item, chr);
                    }

                    item.setIsCookable(this.cookable);
                    if (previousItem != null) {
                        food.setHungChange(((Food)previousItem).getHungChange());
                        food.setBaseHunger(((Food)previousItem).getBaseHunger());
                    } else {
                        food.setHungChange(0.0F);
                        food.setBaseHunger(0.0F);
                    }

                    if (oldBaseItem instanceof Food && oldBaseItem.getOffAgeMax() != 1000000000 && item.getOffAgeMax() != 1000000000) {
                        float age = oldBaseItem.getAge() / oldBaseItem.getOffAgeMax();
                        item.setAge(item.getOffAgeMax() * age);
                    }

                    if (previousItem instanceof Food prevFood) {
                        food.setTainted(prevFood.isTainted());
                        food.setCalories(prevFood.getCalories());
                        food.setProteins(prevFood.getProteins());
                        food.setLipids(prevFood.getLipids());
                        food.setCarbohydrates(prevFood.getCarbohydrates());
                        food.setThirstChange(prevFood.getThirstChange());
                    }
                }

                item.setUnhappyChange(0.0F);
                item.setBoredomChange(0.0F);
                item.setCondition(oldBaseItem.getCondition(), false);
                item.setFavorite(oldBaseItem.isFavorite());
                chr.getInventory().Remove(oldBaseItem);
                chr.getInventory().AddItem(item);
                if (GameServer.server) {
                    GameServer.sendReplaceItemInContainer(chr.getInventory(), oldBaseItem, item);
                }
            }
        }

        if (this.itemsList.get(usedItem.getType()) != null && this.itemsList.get(usedItem.getType()).use > -1) {
            if (!(usedItem instanceof Food usedItemFood)) {
                if (usedItem.getScriptItem().isSpice() && baseItem instanceof Food food) {
                    this.useSpice(usedItem, food, 1, cookingLvl, chr);
                    return baseItem;
                }

                usedItem.UseAndSync();
            } else {
                float usedHunger = this.itemsList.get(usedItem.getType()).use.intValue() / 100.0F;
                Food baseItemFood = (Food)baseItem;
                boolean herbalTea = baseItemFood.hasTag(ItemTag.HERBAL_TEA) && usedItemFood.hasTag(ItemTag.HERBAL_TEA);
                if (usedItemFood.isSpice() && baseItem instanceof Food food) {
                    if (baseItem instanceof Food && herbalTea) {
                        baseItemFood.setFoodSicknessChange(baseItemFood.getFoodSicknessChange() + usedItemFood.getFoodSicknessChange());
                        baseItemFood.setPainReduction(baseItemFood.getPainReduction() + usedItemFood.getPainReduction());
                        baseItemFood.setFluReduction(baseItemFood.getFluReduction() + usedItemFood.getFluReduction());
                        baseItemFood.setStressChange(baseItemFood.getStressChange() + usedItemFood.getStressChange());
                        baseItemFood.setReduceInfectionPower(baseItemFood.getReduceInfectionPower() + usedItemFood.getReduceInfectionPower());
                        if (usedItemFood.getEnduranceChange() > 0.0F) {
                            baseItemFood.setEnduranceChange(baseItemFood.getEnduranceChange() + usedItemFood.getEnduranceChange());
                        }

                        if (baseItemFood.getFoodSicknessChange() > 12) {
                            baseItemFood.setFoodSicknessChange(12);
                        }

                        if (usedItemFood.hasTag(ItemTag.BOOSTS_FLU_RECOVERY)) {
                            baseItemFood.setFluReduction(baseItemFood.getFluReduction() + 5);
                        }
                    }

                    this.useSpice(usedItemFood, food, usedHunger, cookingLvl, chr);
                    return baseItem;
                }

                boolean useAll = false;
                if (usedItemFood.isRotten()) {
                    DecimalFormat df = DECIMAL_FORMAT;
                    df.setRoundingMode(RoundingMode.HALF_EVEN);
                    if (cookingLvl == 7 || cookingLvl == 8) {
                        usedHunger = Float.parseFloat(
                            df.format(Math.abs(usedItemFood.getBaseHunger() - (usedItemFood.getBaseHunger() - 0.05F * usedItemFood.getBaseHunger())))
                                .replace(",", ".")
                        );
                    } else if (cookingLvl == 9 || cookingLvl == 10) {
                        usedHunger = Float.parseFloat(
                            df.format(Math.abs(usedItemFood.getBaseHunger() - (usedItemFood.getBaseHunger() - 0.1F * usedItemFood.getBaseHunger())))
                                .replace(",", ".")
                        );
                    }

                    useAll = true;
                }

                if (Math.abs(usedItemFood.getHungerChange()) < usedHunger) {
                    DecimalFormat df = DECIMAL_FORMAT;
                    df.setRoundingMode(RoundingMode.DOWN);
                    usedHunger = Math.abs(Float.parseFloat(df.format(usedItemFood.getHungerChange()).replace(",", ".")));
                    useAll = true;
                }

                if (baseItem instanceof Food food) {
                    if (usedItem instanceof Food usedFood && usedFood.getPoisonLevelForRecipe() >= 0 && usedFood.isPoison()) {
                        this.addPoison(usedItem, baseItem, chr);
                    }

                    baseItemFood.setHungChange(baseItemFood.getHungChange() - usedHunger);
                    baseItemFood.setBaseHunger(baseItemFood.getBaseHunger() - usedHunger);
                    if (usedItemFood.isbDangerousUncooked() && !usedItemFood.isCooked() && !usedItemFood.isBurnt()) {
                        baseItemFood.setbDangerousUncooked(true);
                    }

                    int changer = 0;
                    if (baseItem.extraItems != null) {
                        for (int i = 0; i < baseItem.extraItems.size(); i++) {
                            if (baseItem.extraItems.get(i).equals(usedItem.getFullType())) {
                                changer++;
                            }
                        }
                    }

                    if (baseItem.extraItems != null && baseItem.extraItems.size() - 2 > cookingLvl) {
                        changer += baseItem.extraItems.size() - 2 - cookingLvl * 3;
                    }

                    float realUsedHunger = usedHunger - 3 * cookingLvl / 100.0F * usedHunger;
                    float percentageUsed = Math.abs(realUsedHunger / usedItemFood.getHungChange());
                    if (percentageUsed > 1.0F) {
                        percentageUsed = 1.0F;
                    }

                    baseItem.setUnhappyChange(food.getUnhappyChangeUnmodified() - (5 - changer * 5));
                    if (baseItem.getUnhappyChange() > 25.0F) {
                        baseItem.setUnhappyChange(25.0F);
                    }

                    float nutritionBoost = cookingLvl / 15.0F + 1.0F;
                    baseItemFood.setCalories(baseItemFood.getCalories() + usedItemFood.getCalories() * nutritionBoost * percentageUsed);
                    baseItemFood.setProteins(baseItemFood.getProteins() + usedItemFood.getProteins() * nutritionBoost * percentageUsed);
                    baseItemFood.setCarbohydrates(baseItemFood.getCarbohydrates() + usedItemFood.getCarbohydrates() * nutritionBoost * percentageUsed);
                    baseItemFood.setLipids(baseItemFood.getLipids() + usedItemFood.getLipids() * nutritionBoost * percentageUsed);
                    float thirstChange = usedItemFood.getThirstChangeUnmodified() * nutritionBoost * percentageUsed;
                    if (!usedItemFood.hasTag(ItemTag.DRIED_FOOD)) {
                        baseItemFood.setThirstChange(baseItemFood.getThirstChangeUnmodified() + thirstChange);
                    }

                    if (usedItemFood.isCooked()) {
                        realUsedHunger = (float)(realUsedHunger / 1.3);
                    }

                    usedItemFood.setHungChange(usedItemFood.getHungChange() + realUsedHunger);
                    usedItemFood.setBaseHunger(usedItemFood.getBaseHunger() + realUsedHunger);
                    usedItemFood.setThirstChange(usedItemFood.getThirstChange() - thirstChange);
                    usedItemFood.setUnhappyChange(usedItemFood.getUnhappyChange() - usedItemFood.getUnhappyChange() * percentageUsed);
                    usedItemFood.setCalories(usedItemFood.getCalories() - usedItemFood.getCalories() * percentageUsed);
                    usedItemFood.setProteins(usedItemFood.getProteins() - usedItemFood.getProteins() * percentageUsed);
                    usedItemFood.setCarbohydrates(usedItemFood.getCarbohydrates() - usedItemFood.getCarbohydrates() * percentageUsed);
                    usedItemFood.setLipids(usedItemFood.getLipids() - usedItemFood.getLipids() * percentageUsed);
                    if (baseItemFood.hasTag(ItemTag.ALCOHOLIC_BEVERAGE) && usedItemFood.isAlcoholic()) {
                        baseItemFood.setAlcoholic(true);
                    }

                    if (herbalTea) {
                        baseItemFood.setFoodSicknessChange(baseItemFood.getFoodSicknessChange() + usedItemFood.getFoodSicknessChange());
                        baseItemFood.setPainReduction(baseItemFood.getPainReduction() + usedItemFood.getPainReduction());
                        baseItemFood.setFluReduction(baseItemFood.getFluReduction() + usedItemFood.getFluReduction());
                        baseItemFood.setStressChange(baseItemFood.getStressChange() + usedItemFood.getStressChange());
                        baseItemFood.setReduceInfectionPower(baseItemFood.getReduceInfectionPower() + usedItemFood.getReduceInfectionPower());
                        if (baseItemFood.getFoodSicknessChange() > 12) {
                            baseItemFood.setFoodSicknessChange(12);
                        }

                        if (usedItemFood.hasTag(ItemTag.BOOSTS_FLU_RECOVERY)) {
                            baseItemFood.setFluReduction(baseItemFood.getFluReduction() + 5);
                        }
                    }

                    if (usedItemFood.getHungerChange() >= -0.02 || useAll) {
                        usedItem.UseAndSync();
                    }

                    if (usedItemFood.getFatigueChange() < 0.0F) {
                        baseItem.setFatigueChange(usedItemFood.getFatigueChange() * percentageUsed);
                        usedItemFood.setFatigueChange(usedItemFood.getFatigueChange() - usedItemFood.getFatigueChange() * percentageUsed);
                    }

                    if (usedItemFood.getPoisonPower() > 0) {
                        usedItemFood.setPoisonPower((int)(usedItemFood.getPoisonPower() - usedItemFood.getPoisonPower() * percentageUsed + 0.999));
                        food.setPoisonPower((int)(usedItemFood.getPoisonPower() * percentageUsed + 0.999));
                    }
                }
            }

            baseItem.addExtraItem(usedItem.getFullType());
            if (GameServer.server) {
                if (baseItem.getContainer().getParent() instanceof IsoPlayer) {
                    INetworkPacket.send((IsoPlayer)baseItem.getContainer().getParent(), PacketTypes.PacketType.ItemStats, baseItem.getContainer(), baseItem);
                } else {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, chr.getX(), chr.getY(), baseItem.getContainer(), this);
                }
            }
        } else if (usedItem instanceof Food food && food.getPoisonLevelForRecipe() >= 0 && food.isPoison()) {
            this.addPoison(usedItem, baseItem, chr);
        }

        this.checkUniqueRecipe(baseItem);
        if (GameServer.server) {
            GameServer.addXp((IsoPlayer)chr, PerkFactory.Perks.Cooking, 3.0F);
        } else if (!GameClient.client) {
            chr.getXp().AddXP(PerkFactory.Perks.Cooking, 3.0F);
        }

        return baseItem;
    }

    private void checkUniqueRecipe(InventoryItem baseItem) {
        if (baseItem instanceof Food food) {
            Stack<UniqueRecipe> uniqueRecipe = ScriptManager.instance.getAllUniqueRecipes();

            for (int i = 0; i < uniqueRecipe.size(); i++) {
                ArrayList<Integer> usedIndex = new ArrayList<>();
                UniqueRecipe recipe = uniqueRecipe.get(i);
                if (recipe.getBaseRecipe().equals(baseItem.getType())) {
                    boolean findAll = true;

                    for (int j = 0; j < recipe.getItems().size(); j++) {
                        boolean ok = false;

                        for (int x = 0; x < food.getExtraItems().size(); x++) {
                            if (!usedIndex.contains(x) && food.getExtraItems().get(x).equals(recipe.getItems().get(j))) {
                                ok = true;
                                usedIndex.add(x);
                                break;
                            }
                        }

                        if (!ok) {
                            findAll = false;
                            break;
                        }
                    }

                    if (food.getExtraItems().size() == recipe.getItems().size() && findAll) {
                        food.setName(recipe.getName());
                        food.setBaseHunger(food.getBaseHunger() - recipe.getHungerBonus() / 100.0F);
                        food.setHungChange(food.getBaseHunger());
                        food.setBoredomChange(food.getBoredomChangeUnmodified() - recipe.getBoredomBonus());
                        food.setUnhappyChange(food.getUnhappyChangeUnmodified() - recipe.getHapinessBonus());
                        food.setCustomName(true);
                    }
                }
            }
        }
    }

    private void addPoison(InventoryItem usedItem, InventoryItem baseItem, IsoGameCharacter chr) {
        Food usedItemFood = (Food)usedItem;
        if (baseItem instanceof Food baseItemFood) {
            int level = usedItemFood.getPoisonLevelForRecipe() - chr.getPerkLevel(PerkFactory.Perks.Cooking);
            if (level < 1) {
                level = 1;
            }

            Float poisonPowerPercentage = 0.0F;
            if (usedItemFood.getThirstChange() <= -0.01F) {
                float use = usedItemFood.getUseForPoison() / 100.0F;
                if (Math.abs(usedItemFood.getThirstChange()) < use) {
                    use = Math.abs(usedItemFood.getThirstChange());
                }

                poisonPowerPercentage = Math.abs(use / usedItemFood.getThirstChange());
                poisonPowerPercentage = (float)(Math.round(poisonPowerPercentage.doubleValue() * 100.0) / 100.0);
                usedItemFood.setThirstChange(usedItemFood.getThirstChange() + use);
                if (usedItemFood.getThirstChange() > -0.01) {
                    usedItemFood.UseAndSync();
                }
            } else if (usedItemFood.getBaseHunger() <= -0.01F) {
                float usex = usedItemFood.getUseForPoison() / 100.0F;
                if (Math.abs(usedItemFood.getBaseHunger()) < usex) {
                    usex = Math.abs(usedItemFood.getThirstChange());
                }

                poisonPowerPercentage = Math.abs(usex / usedItemFood.getBaseHunger());
                poisonPowerPercentage = (float)(Math.round(poisonPowerPercentage.doubleValue() * 100.0) / 100.0);
            }

            if (baseItemFood.getPoisonDetectionLevel() == -1) {
                baseItemFood.setPoisonDetectionLevel(0);
            }

            baseItemFood.setPoisonDetectionLevel(baseItemFood.getPoisonDetectionLevel() + level);
            if (baseItemFood.getPoisonDetectionLevel() > 10) {
                baseItemFood.setPoisonDetectionLevel(10);
            }

            int usedPoisonPower = (int)(poisonPowerPercentage * (usedItemFood.getPoisonPower() / 100.0F) * 100.0F);
            int newBaseItemFoodPoisonPower = baseItemFood.getPoisonPower() + usedPoisonPower;
            baseItemFood.setPoisonPower(baseItemFood.getPoisonPower() + usedPoisonPower);
            usedItemFood.setPoisonPower(usedItemFood.getPoisonPower() - usedPoisonPower);
            baseItemFood.getModData().rawset("addedPoisonBy", chr.getFullName());
            String debugStr = String.format("Char %s poisoned item %s with power %d", chr.getName(), baseItem.getDisplayName(), newBaseItemFoodPoisonPower);
            DebugLog.Objects.debugln(debugStr);
            LoggerManager.getLogger("user").write(debugStr);
            if (GameServer.server) {
                if (baseItem.getContainer().getParent() instanceof IsoPlayer) {
                    INetworkPacket.send((IsoPlayer)baseItem.getContainer().getParent(), PacketTypes.PacketType.ItemStats, baseItem.getContainer(), baseItem);
                } else {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, chr.getX(), chr.getY(), baseItem.getContainer(), baseItem);
                }

                if (usedItem.getContainer().getParent() instanceof IsoPlayer) {
                    INetworkPacket.send((IsoPlayer)usedItem.getContainer().getParent(), PacketTypes.PacketType.ItemStats, usedItem.getContainer(), usedItem);
                } else {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, chr.getX(), chr.getY(), usedItem.getContainer(), usedItem);
                }
            }
        }
    }

    private void useSpice(Food usedSpice, Food baseItem, float usedHunger, int cookingLvl, IsoGameCharacter chr) {
        if (!this.isSpiceAdded(baseItem, usedSpice)) {
            if (baseItem.spices == null) {
                baseItem.spices = new ArrayList<>();
            }

            baseItem.spices.add(usedSpice.getFullType());
            float realUsedHunger = usedHunger;
            if (usedSpice.isRotten()) {
                DecimalFormat df = DECIMAL_FORMAT;
                df.setRoundingMode(RoundingMode.HALF_EVEN);
                if (cookingLvl == 7 || cookingLvl == 8) {
                    usedHunger = Float.parseFloat(
                        df.format(Math.abs(usedSpice.getBaseHunger() - (usedSpice.getBaseHunger() - 0.05F * usedSpice.getBaseHunger()))).replace(",", ".")
                    );
                } else if (cookingLvl == 9 || cookingLvl == 10) {
                    usedHunger = Float.parseFloat(
                        df.format(Math.abs(usedSpice.getBaseHunger() - (usedSpice.getBaseHunger() - 0.1F * usedSpice.getBaseHunger()))).replace(",", ".")
                    );
                }
            }

            float percentageUsed = Math.abs(usedHunger / usedSpice.getHungChange());
            if (percentageUsed > 1.0F) {
                percentageUsed = 1.0F;
            }

            float nutritionBoost = cookingLvl / 15.0F + 1.0F;
            baseItem.setUnhappyChange(baseItem.getUnhappyChangeUnmodified() - usedHunger * 200.0F);
            baseItem.setBoredomChange(baseItem.getBoredomChangeUnmodified() - usedHunger * 200.0F);
            baseItem.setCalories(baseItem.getCalories() + usedSpice.getCalories() * nutritionBoost * percentageUsed);
            baseItem.setProteins(baseItem.getProteins() + usedSpice.getProteins() * nutritionBoost * percentageUsed);
            baseItem.setCarbohydrates(baseItem.getCarbohydrates() + usedSpice.getCarbohydrates() * nutritionBoost * percentageUsed);
            baseItem.setLipids(baseItem.getLipids() + usedSpice.getLipids() * nutritionBoost * percentageUsed);
            percentageUsed = Math.abs(realUsedHunger / usedSpice.getHungChange());
            if (percentageUsed > 1.0F) {
                percentageUsed = 1.0F;
            }

            usedSpice.setCalories(usedSpice.getCalories() - usedSpice.getCalories() * percentageUsed);
            usedSpice.setProteins(usedSpice.getProteins() - usedSpice.getProteins() * percentageUsed);
            usedSpice.setCarbohydrates(usedSpice.getCarbohydrates() - usedSpice.getCarbohydrates() * percentageUsed);
            usedSpice.setLipids(usedSpice.getLipids() - usedSpice.getLipids() * percentageUsed);
            usedSpice.setHungChange(usedSpice.getHungChange() + realUsedHunger);
            if (usedSpice.getHungerChange() > -0.01) {
                usedSpice.UseAndSync();
            }

            if (GameServer.server) {
                if (baseItem.getContainer().getParent() instanceof IsoPlayer) {
                    INetworkPacket.send((IsoPlayer)baseItem.getContainer().getParent(), PacketTypes.PacketType.ItemStats, baseItem.getContainer(), baseItem);
                } else {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, chr.getX(), chr.getY(), baseItem.getContainer(), baseItem);
                }
            }
        }
    }

    private void useSpice(InventoryItem usedSpice, Food baseItem, int uses, int cookingLvl, IsoGameCharacter chr) {
        if (!this.isSpiceAdded(baseItem, usedSpice)) {
            if (baseItem.spices == null) {
                baseItem.spices = new ArrayList<>();
            }

            baseItem.spices.add(usedSpice.getFullType());
            usedSpice.UseAndSync();
            if (GameServer.server) {
                if (baseItem.getContainer().getParent() instanceof IsoPlayer) {
                    INetworkPacket.send((IsoPlayer)baseItem.getContainer().getParent(), PacketTypes.PacketType.ItemStats, baseItem.getContainer(), baseItem);
                } else {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.ItemStats, chr.getX(), chr.getY(), baseItem.getContainer(), baseItem);
                }
            }
        }
    }

    public ItemRecipe getItemRecipe(InventoryItem usedItem) {
        return this.itemsList.get(usedItem.getType());
    }

    public String getName() {
        return this.displayName;
    }

    public String getOriginalname() {
        return this.originalname;
    }

    public String getUntranslatedName() {
        return this.name;
    }

    public String getBaseItem() {
        return this.baseItem;
    }

    public float getMinimumWater() {
        return this.minimumWater;
    }

    public boolean hasMinimumWater(InventoryItem item) {
        if (item.getFluidContainer() == null) {
            return false;
        } else {
            FluidContainer fluidCont = item.getFluidContainer();
            if (!fluidCont.isAllCategory(FluidCategory.Water)) {
                return false;
            } else {
                float water = fluidCont.getFilledRatio();
                return !(water < this.getMinimumWater());
            }
        }
    }

    public Map<String, ItemRecipe> getItemsList() {
        return this.itemsList;
    }

    public ArrayList<ItemRecipe> getPossibleItems() {
        ArrayList<ItemRecipe> result = new ArrayList<>();

        for (ItemRecipe recipe : this.itemsList.values()) {
            result.add(recipe);
        }

        return result;
    }

    public String getResultItem() {
        return !this.resultItem.contains(".") ? this.resultItem : this.resultItem.split("\\.")[1];
    }

    public String getFullResultItem() {
        return this.resultItem;
    }

    public boolean isCookable() {
        return this.cookable;
    }

    public int getMaxItems() {
        return this.maxItems;
    }

    public boolean isResultItem(InventoryItem item) {
        return item == null ? false : this.getResultItem().equals(item.getType());
    }

    public boolean isSpiceAdded(InventoryItem baseItem, InventoryItem spiceItem) {
        if (!this.isResultItem(baseItem)) {
            return false;
        } else if (baseItem instanceof Food food && spiceItem.isSpice()) {
            ArrayList<String> spices = food.getSpices();
            return spices == null ? false : spices.contains(spiceItem.getFullType());
        } else {
            return false;
        }
    }

    public String getAddIngredientSound() {
        return this.addIngredientSound;
    }

    public void setIsHidden(boolean hide) {
        this.hidden = hide;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public boolean isAllowFrozenItem() {
        return this.allowFrozenItem;
    }

    public void setAllowFrozenItem(boolean allow) {
        this.allowFrozenItem = allow;
    }

    static {
        DECIMAL_FORMAT.applyPattern("#.##");
    }
}
