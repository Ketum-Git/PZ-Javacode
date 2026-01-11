// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class ItemStatsPacket implements INetworkPacket {
    ContainerID containerId = new ContainerID();
    @JSONField
    int id;
    @JSONField
    int uses;
    @JSONField
    float usedDelta;
    @JSONField
    boolean isFood;
    @JSONField
    boolean frozen;
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
    public ArrayList<String> extraItems;
    @JSONField
    boolean alcoholic;
    @JSONField
    float baseHunger;
    @JSONField
    boolean customName;
    @JSONField
    boolean tainted;
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
                this.frozen = food.isFrozen();
                this.heat = food.getHeat();
                this.isCooked = food.isCooked();
                this.isBurnt = food.isBurnt();
                this.cookingTime = food.getCookingTime();
                this.minutesToCook = food.getMinutesToCook();
                this.minutesToBurn = food.getMinutesToBurn();
                this.hungChange = food.getHungChange();
                this.calories = food.getCalories();
                this.carbohydrates = food.getCarbohydrates();
                this.lipids = food.getLipids();
                this.proteins = food.getProteins();
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
                this.extraItems = food.extraItems;
                this.alcoholic = food.isAlcoholic();
                this.baseHunger = food.getBaseHunger();
                this.customName = food.isCustomName();
                this.name = food.getDisplayName();
                this.tainted = food.isTainted();
                this.actualWeight = food.getActualWeight();
            } else {
                this.isFood = false;
            }
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.containerId.write(b);
        b.putInt(this.id);
        b.putInt(this.uses);
        b.putFloat(this.usedDelta);
        b.putBoolean(this.isFluidContainer);
        if (this.isFluidContainer) {
            try {
                this.fluidContainer.save(b.bb);
            } catch (IOException var3) {
                throw new RuntimeException(var3);
            }
        }

        b.putFloat(this.heat);
        b.putBoolean(this.isFood);
        if (this.isFood) {
            b.putBoolean(this.frozen);
            b.putBoolean(this.tainted);
            b.putBoolean(this.isCooked);
            b.putBoolean(this.isBurnt);
            b.putFloat(this.cookingTime);
            b.putFloat(this.minutesToCook);
            b.putFloat(this.minutesToBurn);
            b.putFloat(this.hungChange);
            b.putFloat(this.calories);
            b.putFloat(this.carbohydrates);
            b.putFloat(this.lipids);
            b.putFloat(this.proteins);
            b.putFloat(this.thirstChange);
            b.putInt(this.fluReduction);
            b.putFloat(this.painReduction);
            b.putFloat(this.endChange);
            b.putInt(this.foodSicknessChange);
            b.putFloat(this.stressChange);
            b.putFloat(this.fatigueChange);
            b.putFloat(this.unhappyChange);
            b.putFloat(this.boredomChange);
            b.putInt(this.poisonPower);
            b.putInt(this.poisonDetectionLevel);
            b.putFloat(this.actualWeight);
            if (this.extraItems == null) {
                b.putByte((byte)0);
            } else {
                b.putByte((byte)1);
                b.putShort((byte)this.extraItems.size());

                for (int i = 0; i < this.extraItems.size(); i++) {
                    b.putUTF(this.extraItems.get(i));
                }
            }

            b.putBoolean(this.alcoholic);
            b.putFloat(this.baseHunger);
            b.putBoolean(this.customName);
            b.putUTF(this.name);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.containerId.parse(b, connection);
        this.id = b.getInt();
        this.uses = b.getInt();
        this.usedDelta = b.getFloat();
        this.isFluidContainer = b.get() != 0;
        if (this.isFluidContainer) {
            try {
                this.fluidContainer.load(b, 240);
            } catch (IOException var5) {
                throw new RuntimeException(var5);
            }
        }

        this.heat = b.getFloat();
        this.isFood = b.get() != 0;
        if (this.isFood) {
            this.frozen = b.get() != 0;
            this.tainted = b.get() != 0;
            this.isCooked = b.get() != 0;
            this.isBurnt = b.get() != 0;
            this.cookingTime = b.getFloat();
            this.minutesToCook = b.getFloat();
            this.minutesToBurn = b.getFloat();
            this.hungChange = b.getFloat();
            this.calories = b.getFloat();
            this.carbohydrates = b.getFloat();
            this.lipids = b.getFloat();
            this.proteins = b.getFloat();
            this.thirstChange = b.getFloat();
            this.fluReduction = b.getInt();
            this.painReduction = b.getFloat();
            this.endChange = b.getFloat();
            this.foodSicknessChange = b.getInt();
            this.stressChange = b.getFloat();
            this.fatigueChange = b.getFloat();
            this.unhappyChange = b.getFloat();
            this.boredomChange = b.getFloat();
            this.poisonPower = b.getInt();
            this.poisonDetectionLevel = b.getInt();
            this.actualWeight = b.getFloat();
            if (b.get() != 0) {
                short size = b.getShort();
                this.extraItems = new ArrayList<>();

                for (int i = 0; i < size; i++) {
                    this.extraItems.add(GameWindow.ReadString(b));
                }
            }

            this.alcoholic = b.get() != 0;
            this.baseHunger = b.getFloat();
            this.customName = b.get() != 0;
            this.name = GameWindow.ReadString(b);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        ItemContainer container = this.containerId.getContainer();
        if (container != null) {
            InventoryItem item = container.getItemWithID(this.id);
            if (item != null) {
                this.applyItemStats(item);

                for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                    UdpConnection c = GameServer.udpEngine.connections.get(n);
                    if (c != connection && c.RelevantTo(this.containerId.x, this.containerId.y)) {
                        ByteBufferWriter bbw = c.startPacket();
                        PacketTypes.PacketType.ItemStats.doPacket(bbw);
                        this.write(bbw);
                        PacketTypes.PacketType.ItemStats.send(c);
                    }
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        ItemContainer container = this.containerId.getContainer();
        if (container != null) {
            InventoryItem item = container.getItemWithID(this.id);
            if (item != null) {
                this.applyItemStats(item);
            }
        }
    }

    private void applyItemStats(InventoryItem item) {
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
            food.setFrozen(this.frozen);
            food.setTainted(this.tainted);
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
            if (food.extraItems != null) {
                if (this.extraItems != null) {
                    food.extraItems.clear();
                    food.extraItems.addAll(this.extraItems);
                } else {
                    food.extraItems.clear();
                    food.extraItems = null;
                }
            } else if (this.extraItems != null) {
                food.extraItems = this.extraItems;
            }

            food.setAlcoholic(this.alcoholic);
            food.setBaseHunger(this.baseHunger);
            food.setCustomName(this.customName);
            food.setName(this.name);
        }
    }
}
