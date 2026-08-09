// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class ItemStatsPacket implements INetworkPacket {
    private static final int BIT_IS_EMPTY = 0;
    private static final int BIT_IS_FROZEN = 1;
    private static final int BIT_IS_TAINTED = 2;
    private static final int BIT_IS_COOKED = 4;
    private static final int BIT_IS_BURNED = 8;
    private static final int BIT_COOKING_TIME = 16;
    private static final int BIT_MINUTES_TO_COOK = 32;
    private static final int BIT_MINUTES_TO_BURN = 64;
    private static final int BIT_HUNG_CHANGE = 128;
    private static final int BIT_CALORIES = 256;
    private static final int BIT_THIRST_CHANGE = 512;
    private static final int BIT_FLU_REDUCTION = 1024;
    private static final int BIT_PAIN_REDUCTION = 2048;
    private static final int BIT_END_CHANGE = 4096;
    private static final int BIT_FOOD_SICKNESS_CHANGE = 8192;
    private static final int BIT_STRESS_CHANGE = 16384;
    private static final int BIT_FATIGUE_CHANGE = 32768;
    private static final int BIT_UNHAPPY_CHANGE = 65536;
    private static final int BIT_BOREDOM_CHANGE = 131072;
    private static final int BIT_POISON_POWER = 262144;
    private static final int BIT_POISON_DETECTION_LEVEL = 524288;
    private static final int BIT_IS_ALCOHOLIC = 1048576;
    private static final int BIT_BASE_HUNGER = 2097152;
    private static final int BIT_EXTRA_ITEMS = 4194304;
    private static final int BIT_SPICES = 8388608;
    private static final int BIT_IS_CUSTOM_NAME = 16777216;
    private static final int BIT_IS_FERTILIZED = 33554432;
    ContainerID containerId = new ContainerID();
    @JSONField
    int id;
    @JSONField
    int condition;
    @JSONField
    int uses;
    @JSONField
    float usedDelta;
    @JSONField
    boolean isFood;
    @JSONField
    boolean isFrozen;
    @JSONField
    float heat;
    @JSONField
    float cookingTime;
    @JSONField
    float minutesToCook;
    @JSONField
    float minutesToBurn;
    @JSONField
    float hungChange;
    @JSONField
    float calories;
    @JSONField
    float carbohydrates;
    @JSONField
    float lipids;
    @JSONField
    float proteins;
    @JSONField
    float thirstChange;
    @JSONField
    int fluReduction;
    @JSONField
    float painReduction;
    @JSONField
    float endChange;
    @JSONField
    int foodSicknessChange;
    @JSONField
    float stressChange;
    @JSONField
    float fatigueChange;
    @JSONField
    float unhappyChange;
    @JSONField
    float boredomChange;
    @JSONField
    int poisonPower;
    @JSONField
    int poisonDetectionLevel;
    @JSONField
    private final ArrayList<String> extraItems = new ArrayList<>();
    @JSONField
    boolean isAlcoholic;
    @JSONField
    float baseHunger;
    @JSONField
    boolean isCustomName;
    @JSONField
    boolean isTainted;
    @JSONField
    boolean isFluidContainer;
    @JSONField
    FluidContainer fluidContainer = FluidContainer.CreateContainer();
    @JSONField
    boolean isCooked;
    @JSONField
    boolean isBurnt;
    @JSONField
    float freezingTime;
    @JSONField
    String name;
    @JSONField
    float actualWeight;
    @JSONField
    boolean isFertilized;
    @JSONField
    int fertilizedTime;
    @JSONField
    boolean isWet;
    @JSONField
    float wetCooldown;
    @JSONField
    private final ArrayList<String> spices = new ArrayList<>();

    @Override
    public void setData(Object... values) {
        if (values.length == 2) {
            ItemContainer container = (ItemContainer)values[0];
            InventoryItem item = (InventoryItem)values[1];
            if (container.getType().equals("floor") && item.getWorldItem() != null) {
                this.containerId.setFloor(container, item.getWorldItem().square);
            } else {
                this.containerId.set(container);
            }

            this.id = item.id;
            this.condition = item.getCondition();
            this.uses = item.getCurrentUses();
            this.usedDelta = item instanceof DrainableComboItem ? item.getCurrentUsesFloat() : 0.0F;
            FluidContainer copiedFluidContainer = item.getFluidContainer();
            if (copiedFluidContainer != null) {
                this.isFluidContainer = true;
                this.fluidContainer.setCapacity(copiedFluidContainer.getCapacity());
                this.fluidContainer.copyFluidsFrom(copiedFluidContainer);
            } else {
                this.isFluidContainer = false;
            }

            this.heat = item.getItemHeat();
            if (item instanceof Food food) {
                this.isFood = true;
                this.isFrozen = food.isFrozen();
                this.isTainted = food.isTainted();
                this.heat = food.getHeat();
                this.isCooked = food.isCooked();
                this.isBurnt = food.isBurnt();
                this.cookingTime = food.getCookingTime();
                this.minutesToCook = food.getMinutesToCook();
                this.minutesToBurn = food.getMinutesToBurn();
                this.hungChange = food.getHungChange();
                this.calories = food.getCalories();
                this.proteins = food.getProteins();
                this.lipids = food.getLipids();
                this.carbohydrates = food.getCarbohydrates();
                this.thirstChange = food.getThirstChange();
                this.fluReduction = food.getFluReduction();
                this.painReduction = food.getPainReduction();
                this.endChange = food.getEndChange();
                this.foodSicknessChange = food.getFoodSicknessChange();
                this.stressChange = food.getStressChange();
                this.fatigueChange = food.getFatigueChange();
                this.unhappyChange = food.getUnhappyChange();
                this.boredomChange = food.getBoredomChange();
                this.poisonPower = food.getPoisonPower();
                this.poisonDetectionLevel = food.getPoisonDetectionLevel();
                this.isAlcoholic = food.isAlcoholic();
                this.baseHunger = food.getBaseHunger();
                this.extraItems.clear();
                if (food.extraItems != null) {
                    this.extraItems.addAll(food.extraItems);
                }

                this.spices.clear();
                if (food.spices != null) {
                    this.spices.addAll(food.spices);
                }

                this.actualWeight = food.getActualWeightUnmodded();
                this.isCustomName = food.isCustomName();
                this.name = food.getDisplayName();
                this.isFertilized = food.isFertilized();
                this.fertilizedTime = food.getFertilizedTime();
            } else {
                this.isFood = false;
            }

            this.isWet = item.isWet();
            if (this.isWet) {
                this.wetCooldown = item.getWetCooldown();
            }
        } else {
            DebugType.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.containerId.write(b);
        b.putInt(this.id);
        b.putInt(this.condition);
        b.putInt(this.uses);
        b.putFloat(this.usedDelta);
        if (b.putBoolean(this.isFluidContainer)) {
            try {
                this.fluidContainer.save(b.bb);
            } catch (IOException var5) {
                throw new RuntimeException(var5);
            }
        }

        b.putFloat(this.heat);
        if (b.putBoolean(this.isFood)) {
            BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Integer, b.bb);
            if (this.isFrozen) {
                bits.addFlags(1);
            }

            if (this.isTainted) {
                bits.addFlags(2);
            }

            if (this.isCooked) {
                bits.addFlags(4);
            }

            if (this.isBurnt) {
                bits.addFlags(8);
            }

            if (this.cookingTime != 0.0F) {
                bits.addFlags(16);
                b.putFloat(this.cookingTime);
            }

            if (this.minutesToCook != 0.0F) {
                bits.addFlags(32);
                b.putFloat(this.minutesToCook);
            }

            if (this.minutesToBurn != 0.0F) {
                bits.addFlags(64);
                b.putFloat(this.minutesToBurn);
            }

            if (this.hungChange != 0.0F) {
                bits.addFlags(128);
                b.putFloat(this.hungChange);
            }

            if (this.calories != 0.0F || this.proteins != 0.0F || this.lipids != 0.0F || this.carbohydrates != 0.0F) {
                bits.addFlags(256);
                b.putFloat(this.calories);
                b.putFloat(this.proteins);
                b.putFloat(this.lipids);
                b.putFloat(this.carbohydrates);
            }

            if (this.thirstChange != 0.0F) {
                bits.addFlags(512);
                b.putFloat(this.thirstChange);
            }

            if (this.fluReduction != 0) {
                bits.addFlags(1024);
                b.putInt(this.fluReduction);
            }

            if (this.painReduction != 0.0F) {
                bits.addFlags(2048);
                b.putFloat(this.painReduction);
            }

            if (this.endChange != 0.0F) {
                bits.addFlags(4096);
                b.putFloat(this.endChange);
            }

            if (this.foodSicknessChange != 0) {
                bits.addFlags(8192);
                b.putInt(this.foodSicknessChange);
            }

            if (this.stressChange != 0.0F) {
                bits.addFlags(16384);
                b.putFloat(this.stressChange);
            }

            if (this.fatigueChange != 0.0F) {
                bits.addFlags(32768);
                b.putFloat(this.fatigueChange);
            }

            if (this.unhappyChange != 0.0F) {
                bits.addFlags(65536);
                b.putFloat(this.unhappyChange);
            }

            if (this.boredomChange != 0.0F) {
                bits.addFlags(131072);
                b.putFloat(this.boredomChange);
            }

            bits.addFlags(262144);
            b.putByte(this.poisonPower);
            if (this.poisonDetectionLevel != -1) {
                bits.addFlags(524288);
                b.putByte(this.poisonDetectionLevel);
            }

            if (this.isAlcoholic) {
                bits.addFlags(1048576);
            }

            if (this.baseHunger != 0.0F) {
                bits.addFlags(2097152);
                b.putFloat(this.baseHunger);
            }

            if (!this.extraItems.isEmpty()) {
                bits.addFlags(4194304);
                b.putByte(this.extraItems.size());

                for (String extraItem : this.extraItems) {
                    b.putUTF(extraItem);
                }
            }

            if (!this.spices.isEmpty()) {
                bits.addFlags(8388608);
                b.putByte(this.spices.size());

                for (String spice : this.spices) {
                    b.putUTF(spice);
                }
            }

            if (this.isFertilized) {
                bits.addFlags(33554432);
                b.putInt(this.fertilizedTime);
            }

            if (this.isCustomName) {
                bits.addFlags(16777216);
            }

            b.putUTF(this.name);
            b.putFloat(this.actualWeight);
            bits.write();
            bits.release();
        }

        if (b.putBoolean(this.isWet)) {
            b.putFloat(this.wetCooldown);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.containerId.parse(b, connection);
        this.id = b.getInt();
        this.condition = b.getInt();
        this.uses = b.getInt();
        this.usedDelta = b.getFloat();
        this.isFluidContainer = b.getBoolean();
        if (this.isFluidContainer) {
            try {
                this.fluidContainer.load(b.bb, 249);
            } catch (IOException var6) {
                throw new RuntimeException(var6);
            }
        }

        this.heat = b.getFloat();
        this.isFood = b.getBoolean();
        if (this.isFood) {
            BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Integer, b.bb);
            if (!bits.equals(0)) {
                this.isFrozen = bits.hasFlags(1);
                this.isTainted = bits.hasFlags(2);
                this.isCooked = bits.hasFlags(4);
                this.isBurnt = bits.hasFlags(8);
                if (bits.hasFlags(16)) {
                    this.cookingTime = b.getFloat();
                }

                if (bits.hasFlags(32)) {
                    this.minutesToCook = b.getFloat();
                }

                if (bits.hasFlags(64)) {
                    this.minutesToBurn = b.getFloat();
                }

                if (bits.hasFlags(128)) {
                    this.hungChange = b.getFloat();
                }

                if (bits.hasFlags(256)) {
                    this.calories = b.getFloat();
                    this.proteins = b.getFloat();
                    this.lipids = b.getFloat();
                    this.carbohydrates = b.getFloat();
                }

                if (bits.hasFlags(512)) {
                    this.thirstChange = b.getFloat();
                }

                if (bits.hasFlags(1024)) {
                    this.fluReduction = b.getInt();
                }

                if (bits.hasFlags(2048)) {
                    this.painReduction = b.getFloat();
                }

                if (bits.hasFlags(4096)) {
                    this.endChange = b.getFloat();
                }

                if (bits.hasFlags(8192)) {
                    this.foodSicknessChange = b.getInt();
                }

                if (bits.hasFlags(16384)) {
                    this.stressChange = b.getFloat();
                }

                if (bits.hasFlags(32768)) {
                    this.fatigueChange = b.getFloat();
                }

                if (bits.hasFlags(65536)) {
                    this.unhappyChange = b.getFloat();
                }

                if (bits.hasFlags(131072)) {
                    this.boredomChange = b.getFloat();
                }

                if (bits.hasFlags(262144)) {
                    this.poisonPower = b.getByte();
                }

                if (bits.hasFlags(524288)) {
                    this.poisonDetectionLevel = b.getByte();
                }

                this.isAlcoholic = bits.hasFlags(1048576);
                if (bits.hasFlags(2097152)) {
                    this.baseHunger = b.getFloat();
                }

                this.extraItems.clear();
                if (bits.hasFlags(4194304)) {
                    byte extraItemsSize = b.getByte();

                    for (int i = 0; i < extraItemsSize; i++) {
                        this.extraItems.add(b.getUTF());
                    }
                }

                this.spices.clear();
                if (bits.hasFlags(8388608)) {
                    byte spicesSize = b.getByte();

                    for (int i = 0; i < spicesSize; i++) {
                        this.spices.add(b.getUTF());
                    }
                }

                this.isFertilized = bits.hasFlags(33554432);
                if (this.isFertilized) {
                    this.fertilizedTime = b.getInt();
                }

                this.isCustomName = bits.hasFlags(16777216);
                this.name = b.getUTF();
                this.actualWeight = b.getFloat();
            }

            bits.release();
        }

        this.isWet = b.getBoolean();
        if (this.isWet) {
            this.wetCooldown = b.getFloat();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        ItemContainer container = this.containerId.getContainer();
        if (container != null) {
            InventoryItem item = container.getItemWithID(this.id);
            if (item != null) {
                this.applyItemStats(item);
                float containerX = this.containerId.x;
                float containerY = this.containerId.y;
                if (container.getParent() instanceof IsoPlayer player) {
                    containerX = player.getX();
                    containerY = player.getY();
                }

                this.sendToRelativeClients(PacketTypes.PacketType.ItemStats, connection, containerX, containerY);
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        ItemContainer container = this.containerId.getContainer();
        if (container != null) {
            InventoryItem item = container.getItemWithID(this.id);
            if (item == null && container.getParent() instanceof IsoPlayer player) {
                InventoryItem primary = player.getPrimaryHandItem();
                InventoryItem secondary = player.getSecondaryHandItem();
                if (primary != null && primary.getID() == this.id) {
                    item = primary;
                } else if (secondary != null && secondary.getID() == this.id) {
                    item = secondary;
                }
            }

            if (item != null) {
                this.applyItemStats(item);
            }
        }
    }

    private void applyItemStats(InventoryItem item) {
        item.setCondition(this.condition);
        if (item instanceof DrainableComboItem) {
            item.setCurrentUses((int)(item.getMaxUses() * this.usedDelta));
        }

        if (this.isFluidContainer) {
            item.getFluidContainer().setCapacity(this.fluidContainer.getCapacity());
            item.getFluidContainer().copyFluidsFrom(this.fluidContainer);
        }

        item.setItemHeat(this.heat);
        if (this.isFood) {
            Food food = (Food)item;
            food.setFrozen(this.isFrozen);
            food.setTainted(this.isTainted);
            food.setHeat(this.heat);
            food.setCooked(this.isCooked);
            food.setBurnt(this.isBurnt);
            food.setCookingTime(this.cookingTime);
            food.setMinutesToCook(this.minutesToCook);
            food.setMinutesToBurn(this.minutesToBurn);
            food.setHungChange(this.hungChange);
            food.setCalories(this.calories);
            food.setCarbohydrates(this.carbohydrates);
            food.setLipids(this.lipids);
            food.setProteins(this.proteins);
            food.setThirstChange(this.thirstChange);
            food.setFluReduction(this.fluReduction);
            food.setPainReduction(this.painReduction);
            food.setEndChange(this.endChange);
            food.setFoodSicknessChange(this.foodSicknessChange);
            food.setStressChange(this.stressChange);
            food.setFatigueChange(this.fatigueChange);
            food.setUnhappyChange(this.unhappyChange);
            food.setBoredomChange(this.boredomChange);
            food.setPoisonPower(this.poisonPower);
            food.setPoisonDetectionLevel(this.poisonDetectionLevel);
            food.setActualWeight(this.actualWeight);
            if (food.extraItems == null) {
                food.extraItems = new ArrayList<>();
            }

            food.extraItems.clear();
            food.extraItems.addAll(this.extraItems);
            food.setAlcoholic(this.isAlcoholic);
            food.setBaseHunger(this.baseHunger);
            food.setCustomName(this.isCustomName);
            food.setName(this.name);
            if (food.spices == null) {
                food.spices = new ArrayList<>();
            }

            food.spices.clear();
            food.spices.addAll(this.spices);
            food.setFertilized(this.isFertilized);
            food.setFertilizedTime(this.fertilizedTime);
        }

        item.setWet(this.isWet);
        if (this.isWet) {
            item.setWetCooldown(this.wetCooldown);
        }
    }
}
