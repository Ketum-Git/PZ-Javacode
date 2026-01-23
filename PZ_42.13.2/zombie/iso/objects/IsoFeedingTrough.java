// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntityFactory;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidFilter;
import zombie.entity.components.fluids.FluidType;
import zombie.globalObjects.SGlobalObjects;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.network.GameServer;
import zombie.util.StringUtils;

@UsedFromLua
public final class IsoFeedingTrough extends IsoObject {
    private final HashMap<String, Float> feedingTypes = new HashMap<>();
    private int linkedX;
    private int linkedY;
    public ArrayList<IsoAnimal> linkedAnimals = new ArrayList<>();
    private int maxFeed;
    private float water;
    private float maxWater;
    private KahluaTableImpl def;
    public boolean north;

    public IsoFeedingTrough(IsoCell cell) {
        super(cell);
        this.setSpecialTooltip(true);
        this.createFluidContainer();
    }

    public void checkContainer() {
        if (this.getFluidContainer() != null && !this.getFluidContainer().isEmpty()) {
            this.setContainer(null);
        } else if (this.getContainer() == null) {
            ItemContainer container1 = new ItemContainer();
            container1.type = "trough";
            this.setContainer(container1);
        }
    }

    public IsoFeedingTrough(IsoGridSquare square, String spriteName, IsoGridSquare linkedSquare) {
        super(square, spriteName, null);
        if (this.sprite.getProperties().has("ContainerCapacity")) {
            this.container = new ItemContainer(Objects.requireNonNull(this.sprite.getProperties().get("container")), square, this);
            this.container.capacity = Integer.parseInt(Objects.requireNonNull(this.sprite.getProperties().get("ContainerCapacity")));
            this.container.setExplored(true);
        }

        String Facing = this.sprite.getProperties().get("Facing");
        this.setNorth("N".equals(Facing) || "S".equals(Facing));
        if (linkedSquare == null && this.sprite.getSpriteGrid() != null) {
            IsoSpriteGrid spriteGrid = this.sprite.getSpriteGrid();
            int masterIndex = spriteGrid.getWidth() * spriteGrid.getHeight() - 1;
            int thisIndex = spriteGrid.getSpriteIndex(this.sprite);
            if (thisIndex == masterIndex) {
                this.checkZone();
            } else {
                int posX = spriteGrid.getSpriteGridPosX(this.sprite);
                int posY = spriteGrid.getSpriteGridPosY(this.sprite);
                this.setLinkedX(square.getX() + spriteGrid.getWidth() - posX - 1);
                this.setLinkedY(square.getY() + spriteGrid.getHeight() - posY - 1);
            }
        } else if (linkedSquare != null) {
            this.setLinkedX(linkedSquare.getX());
            this.setLinkedY(linkedSquare.getY());
        } else {
            this.checkZone();
        }

        this.initWithDef();
        this.setSpecialTooltip(true);
        if (this.isSlave()) {
            this.container = null;
        } else {
            this.createFluidContainer();
        }
    }

    @Override
    public boolean isItemAllowedInContainer(ItemContainer container, InventoryItem item) {
        return item instanceof Food ? true : !StringUtils.isNullOrEmpty(item.getAnimalFeedType()) && item instanceof DrainableComboItem;
    }

    public void checkZone() {
        DesignationZoneAnimal zone = DesignationZoneAnimal.getZoneF(this.getX(), this.getY(), this.getZ());
        if (zone != null && !zone.troughs.contains(this)) {
            zone.troughs.add(this);
        }
    }

    @Override
    public void removeFromWorld() {
        DesignationZoneAnimal zone = DesignationZoneAnimal.getZoneF(this.getX(), this.getY(), this.getZ());
        if (zone != null) {
            zone.troughs.remove(this);
        }

        super.removeFromWorld();
    }

    public void checkIsoRegion() {
        if (this.getLinkedX() <= 0 || this.getLinkedY() <= 0) {
            ;
        }
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.getCell().addToProcessIsoObject(this);
        this.checkContainer();
        if (this.getSquare() != null) {
            if (this.isSlave()) {
                IsoFeedingTrough master = this.getMasterTrough();
                if (master != null) {
                    master.checkOverlayAfterAnimalEat();
                }

                return;
            }

            this.checkOverlayAfterAnimalEat();
        }
    }

    @Override
    public void update() {
        this.checkWaterFromRain();
        this.checkContainer();
        this.checkIsoRegion();
    }

    public void checkWaterFromRain() {
        if (this.getContainer() != null && !this.getContainer().isEmpty() && this.getFluidContainer() != null) {
            this.removeFluidContainer();
        }
    }

    @Override
    public String getObjectName() {
        return "FeedingTrough";
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        int nb = input.getInt();

        for (int i = 0; i < nb; i++) {
            this.feedingTypes.put(GameWindow.ReadString(input), input.getFloat());
        }

        this.water = input.getFloat();
        if (input.get() == 1) {
            this.linkedX = input.getInt();
            this.linkedY = input.getInt();
            this.container = null;
        }

        this.north = input.get() == 1;
        this.initWithDef();
    }

    @Override
    public void setContainer(ItemContainer container) {
        if (!this.isSlave()) {
            if (container != this.container) {
                if (container == null) {
                    this.container = null;
                } else {
                    container.parent = this;
                    this.container = container;
                }

                if (Thread.currentThread() == GameWindow.gameThread) {
                    LuaEventManager.triggerEvent("OnContainerUpdate");
                }
            }
        }
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        output.putInt(this.feedingTypes.size());

        for (String type : this.feedingTypes.keySet()) {
            GameWindow.WriteString(output, type);
            output.putFloat(this.feedingTypes.get(type));
        }

        output.putFloat(this.water);
        if (this.isSlave()) {
            output.put((byte)1);
            output.putInt(this.linkedX);
            output.putInt(this.linkedY);
        } else {
            output.put((byte)0);
        }

        output.put((byte)(this.north ? 1 : 0));
    }

    public void initWithDef() {
        if (!this.isSlave()) {
            KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("FeedingTroughDef");
            if (definitions != null) {
                KahluaTableIterator iterator = definitions.iterator();

                while (iterator.advance()) {
                    KahluaTableImpl def = (KahluaTableImpl)iterator.getValue();
                    KahluaTableIterator it2 = def.iterator();

                    while (it2.advance()) {
                        String key = it2.getKey().toString();
                        Object value = it2.getValue();
                        if ("spriteW".equals(key) || "spriteN".equals(key)) {
                            KahluaTable spriteTable = (KahluaTable)value;
                            KahluaTableIterator it3 = spriteTable.iterator();

                            while (it3.advance()) {
                                if (it3.getValue().equals(this.getSprite().getName())) {
                                    this.doDef(def);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void doDef(KahluaTableImpl def) {
        this.maxWater = def.rawgetFloat("maxWater");
        this.maxFeed = def.rawgetInt("maxFeed");
        this.def = def;
    }

    public void checkOverlayFull(boolean transmit) {
        this.checkOverlay(this.def, this.getCurrentFeedAmount(), this.getMaxFeed(), false, true, false, transmit);
        this.checkOverlay(this.def, this.getWater(), this.getMaxWater(), true, true, false, transmit);
    }

    public void checkOverlayAfterAnimalEat() {
        boolean transmit = GameServer.server;
        if (this.getContainer() != null) {
            this.checkOverlay(this.def, this.getCurrentFeedAmount(), this.getMaxFeed(), false, true, true, transmit);
        } else {
            this.checkOverlay(this.def, this.getWater(), this.getMaxWater(), true, true, true, transmit);
        }
    }

    public void onFoodAdded() {
        this.removeFluidContainer();
        boolean transmit = GameServer.server;
        this.checkOverlay(this.def, this.getCurrentFeedAmount(), this.getMaxFeed(), false, true, true, transmit);
        ArrayList<DesignationZoneAnimal> connectedZones = DesignationZoneAnimal.getAllDZones(
            null, DesignationZoneAnimal.getZone(this.getSquare().x, this.getSquare().y, this.getSquare().z), null
        );

        for (int j = 0; j < connectedZones.size(); j++) {
            DesignationZoneAnimal zone = connectedZones.get(j);
            ArrayList<IsoAnimal> animals = zone.getAnimalsConnected();

            for (int k = 0; k < animals.size(); k++) {
                IsoAnimal animal = animals.get(k);
                animal.getData().callToTrough(this);
            }
        }
    }

    public void onRemoveFood() {
        boolean transmit = GameServer.server;
        this.checkOverlay(this.def, this.getCurrentFeedAmount(), this.getMaxFeed(), false, true, true, transmit);
        if (this.getContainer().isEmpty()) {
            this.createFluidContainer();
        }
    }

    private void checkOverlay(
        KahluaTableImpl def, float feedAmount, float maxFeed, boolean isWater, boolean checkOtherTile, boolean deletePrevious, boolean transmit
    ) {
        if (def != null) {
            if (checkOtherTile && this.isSlave()) {
                IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare(this.getLinkedX(), this.getLinkedY(), this.getSquare().z);
                if (sq2 != null) {
                    for (int i = 0; i < sq2.getObjects().size(); i++) {
                        if (sq2.getObjects().get(i) instanceof IsoFeedingTrough trough) {
                            if (isWater) {
                                feedAmount = trough.getWater();
                                maxFeed = trough.getMaxWater();
                            } else {
                                feedAmount = trough.getCurrentFeedAmount();
                                maxFeed = trough.getMaxFeed();
                            }
                        }
                    }
                }
            }

            String spriteName = !deletePrevious && this.overlaySprite != null ? this.overlaySprite.getName() : null;
            if (isWater) {
                if (feedAmount > 10.0F && feedAmount < maxFeed / 2.0F) {
                    if (def.rawget("spriteWaterOverlay1") instanceof KahluaTableImpl table1) {
                        spriteName = table1.rawgetStr(this.sprite.getName());
                    }
                } else if (feedAmount >= maxFeed / 2.0F && def.rawget("spriteWaterOverlay2") instanceof KahluaTableImpl table1) {
                    spriteName = table1.rawgetStr(this.sprite.getName());
                }
            } else if (feedAmount > 1.0F && feedAmount < maxFeed / 2.0F) {
                if (def.rawget("spriteFoodOverlay1") instanceof KahluaTableImpl table1) {
                    spriteName = table1.rawgetStr(this.sprite.getName());
                }
            } else if (feedAmount >= maxFeed / 2.0F && def.rawget("spriteFoodOverlay2") instanceof KahluaTableImpl table1) {
                spriteName = table1.rawgetStr(this.sprite.getName());
            }

            this.setOverlaySprite(spriteName, transmit);
            if (checkOtherTile) {
                if (this.hasSpriteGrid()) {
                    for (IsoObject object : this.getSpriteGridObjectsExcludingSelf(new ArrayList<>())) {
                        if (object instanceof IsoFeedingTrough troughx) {
                            troughx.checkOverlay(def, feedAmount, maxFeed, isWater, false, deletePrevious, transmit);
                        }
                    }
                }
            }
        }
    }

    public float getFeedAmount(String type) {
        return this.feedingTypes.get(type) == null ? 0.0F : this.feedingTypes.get(type);
    }

    public void updateLuaObject() {
        SGlobalObjects.OnIsoObjectChangedItself("feedingTrough", this);
    }

    public ArrayList<String> getAllFeedingTypes() {
        return new ArrayList<>(this.feedingTypes.keySet());
    }

    public int getLinkedX() {
        return this.linkedX;
    }

    public int getLinkedY() {
        return this.linkedY;
    }

    public void setLinkedX(int x) {
        this.linkedX = x;
    }

    public void setLinkedY(int y) {
        this.linkedY = y;
    }

    private boolean isSlave() {
        return this.linkedX > 0 && this.linkedY > 0;
    }

    public IsoFeedingTrough getMasterTrough() {
        if (this.isSlave()) {
            IsoGridSquare square1 = this.getCell().getGridSquare(this.getLinkedX(), this.getLinkedY(), PZMath.fastfloor(this.getZ()));
            if (square1 == null) {
                return null;
            } else {
                for (int i = 0; i < square1.getObjects().size(); i++) {
                    IsoObject object = square1.getObjects().get(i);
                    if (object instanceof IsoFeedingTrough trough) {
                        return trough;
                    }
                }

                return null;
            }
        } else {
            return this;
        }
    }

    public float getMaxWater() {
        return this.getFluidContainer() != null ? this.getFluidContainer().getCapacity() : 0.0F;
    }

    public void setMaxWater(float maxWater) {
        this.maxWater = maxWater;
    }

    public float getWater() {
        return this.getFluidContainer() != null ? this.getFluidContainer().getAmount() : 0.0F;
    }

    public void removeWater(float water) {
        if (this.getFluidContainer() != null) {
            if (water < 0.0F) {
                water = 0.0F;
            }

            this.getFluidContainer().removeFluid(water);
        }
    }

    public void addWater(FluidType type, float amount) {
        this.getFluidContainer().addFluid(type, amount);
        this.checkContainer();
        this.checkOverlayAfterAnimalEat();
        this.updateLuaObject();
    }

    public ArrayList<IsoAnimal> getLinkedAnimals() {
        return this.linkedAnimals;
    }

    public void setLinkedAnimals(ArrayList<IsoAnimal> linkedAnimals) {
        this.linkedAnimals = linkedAnimals;
    }

    public boolean isEmptyFeed() {
        return this.feedingTypes.isEmpty();
    }

    public int getMaxFeed() {
        return this.maxFeed;
    }

    public void setMaxFeed(int maxFeed) {
        this.maxFeed = maxFeed;
    }

    public void setDef(KahluaTableImpl def) {
        this.def = def;
    }

    public void setNorth(boolean north) {
        this.north = north;
    }

    public void addLinkedAnimal(IsoAnimal animal) {
        if (!this.linkedAnimals.contains(animal)) {
            this.linkedAnimals.add(animal);
        }
    }

    public float getCurrentFeedAmount() {
        if (this.getContainer() == null) {
            return 0.0F;
        } else {
            float result = 0.0F;

            for (int i = 0; i < this.getContainer().getItems().size(); i++) {
                InventoryItem item = this.getContainer().getItems().get(i);
                if (item instanceof Food food) {
                    result += Math.abs(food.getHungerChange());
                }

                if (!StringUtils.isNullOrEmpty(item.getAnimalFeedType()) && item instanceof DrainableComboItem food) {
                    result += food.getCurrentUses() * 0.1F;
                }
            }

            return Math.round(result * 100.0F) / 100.0F;
        }
    }

    public void createFluidContainer() {
        Component component = ComponentType.FluidContainer.CreateComponent();
        FluidContainer fc = (FluidContainer)component;
        fc.setCapacity(this.maxWater);
        fc.setRainCatcher(0.55F);
        FluidFilter ff = new FluidFilter();
        ff.add(FluidType.Water);
        ff.add(FluidType.TaintedWater);
        fc.setWhitelist(ff);
        GameEntityFactory.AddComponent(this, true, fc);
    }

    public void removeFluidContainer() {
        GameEntityFactory.RemoveComponent(this, this.getFluidContainer());
    }

    @Override
    public void onFluidContainerUpdate() {
        this.checkContainer();
        this.checkOverlayAfterAnimalEat();
    }
}
