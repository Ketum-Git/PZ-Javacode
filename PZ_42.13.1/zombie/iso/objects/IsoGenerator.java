// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.audio.BaseSoundEmitter;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Food;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;

@UsedFromLua
public class IsoGenerator extends IsoObject {
    private static final int MaximumGeneratorCondition = 100;
    private static final float MaximumGeneratorFuel = 10.0F;
    private static final int GeneratorMinimumCondition = 0;
    private static final int GeneratorCriticalCondition = 20;
    private static final int GeneratorWarningCondition = 30;
    private static final int GeneratorLowCondition = 40;
    private static final int GeneratorBackfireChanceCritical = 5;
    private static final int GeneratorBackfireChanceWarning = 10;
    private static final int GeneratorBackfireChanceLow = 15;
    private static final int GeneratorFireChance = 10;
    private static final int GeneratorExplodeChance = 20;
    private static final float GeneratorSoundOffset = 0.5F;
    private static final int GeneratorDefaultSoundRadius = 40;
    private static final int GeneratorDefaultSoundVolume = 60;
    private static final int GeneratorConditionLowerChanceDefault = 30;
    private static final float GeneratorBasePowerConsumption = 0.02F;
    private static int generatorVerticalPowerRange = 3;
    private static final int IsoGeneratorFireStartingEnergy = 1000;
    private static int generatorChunkRange = 2;
    private static final int GeneratorMinZ = -32;
    private static final int GeneratorMaxZ = 31;
    private static final float ClothingAppliancePowerConsumption = 0.09F;
    private static final float TelevisionPowerConsumption = 0.03F;
    private static final float RadioPowerConsumption = 0.01F;
    private static final float StovePowerConsumption = 0.09F;
    private static final float FridgeFreezerPowerConsumption = 0.13F;
    private static final float SingleFridgeOrFreezerPowerConsumption = 0.08F;
    private static final float LightSwitchPowerConsumption = 0.002F;
    private static final float PipedFuelPowerConsumption = 0.03F;
    private static final float BatteryChargerPowerConsumption = 0.05F;
    private static final float StackedWasherDryerPowerConsumption = 0.9F;
    public float fuel;
    public boolean activated;
    public int condition;
    private int lastHour = -1;
    public boolean connected;
    private boolean updateSurrounding;
    private final HashMap<String, String> itemsPowered = new HashMap<>();
    private float totalPowerUsing;
    private static final ArrayList<IsoGenerator> AllGenerators = new ArrayList<>();
    private static int generatorRadius = 20;
    private static final int GENERATOR_SOUND_RADIUS = 20;
    private static final int GENERATOR_SOUND_VOLUME = 20;
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.#### L/h");
    private static final DecimalFormat decimalFormatB = new DecimalFormat(" (#.#### L/h)");

    public IsoGenerator(IsoCell cell) {
        super(cell);
        this.setGeneratorRange();
    }

    public IsoGenerator(InventoryItem item, IsoCell cell, IsoGridSquare sq) {
        super(cell, sq, IsoSpriteManager.instance.getSprite(item.getScriptItem().getWorldObjectSprite()));
        String sprite = item.getScriptItem().getWorldObjectSprite();
        this.setInfoFromItem(item);
        this.sprite = IsoSpriteManager.instance.getSprite(sprite);
        this.square = sq;
        sq.AddSpecialObject(this);
        if (GameServer.server) {
            this.transmitCompleteItemToClients();
        }

        this.setGeneratorRange();
    }

    public IsoGenerator(InventoryItem item, IsoCell cell, IsoGridSquare sq, boolean remote) {
        super(cell, sq, IsoSpriteManager.instance.getSprite(item.getScriptItem().getWorldObjectSprite()));
        String sprite = item.getScriptItem().getWorldObjectSprite();
        this.setInfoFromItem(item);
        this.sprite = IsoSpriteManager.instance.getSprite(sprite);
        this.square = sq;
        sq.AddSpecialObject(this);
        if (GameClient.client && !remote) {
            this.transmitCompleteItemToServer();
        }

        this.setGeneratorRange();
    }

    private void setGeneratorRange() {
        generatorVerticalPowerRange = SandboxOptions.getInstance().generatorVerticalPowerRange.getValue();
        generatorRadius = SandboxOptions.getInstance().generatorTileRange.getValue();
        generatorChunkRange = generatorRadius / 10 + 1;
    }

    public void setInfoFromItem(InventoryItem item) {
        this.condition = item.getCondition();
        if (item.getModData().rawget("fuel") instanceof Double) {
            this.fuel = ((Double)item.getModData().rawget("fuel")).floatValue();
        }

        this.getModData().rawset("generatorFullType", String.valueOf(item.getFullType()));
    }

    @Override
    public void update() {
        if (this.updateSurrounding && this.getSquare() != null) {
            this.setSurroundingElectricity();
            this.updateSurrounding = false;
        }

        if (this.isActivated()) {
            if (!GameServer.server && (this.emitter == null || !this.emitter.isPlaying(this.getSoundPrefix() + "Loop"))) {
                this.playGeneratorSound("Loop");
            }

            if (GameClient.client) {
                this.emitter.tick();
                return;
            }

            Item item = null;
            if (this.getModData().rawget("generatorFullType") != null
                && this.getModData().rawget("generatorFullType") instanceof String itemType
                && ScriptManager.instance.getItem(itemType) != null) {
                item = ScriptManager.instance.getItem(itemType);
            }

            int soundRadius = 20;
            int soundVolume = 20;
            if (item != null) {
                if (item.getSoundRadius() > 0) {
                    soundRadius = item.getSoundRadius();
                }

                if (item.getSoundVolume() > 0) {
                    soundVolume = item.getSoundVolume();
                }
            }

            if (this.getSquare().getRoom() != null) {
                soundRadius /= 2;
            }

            WorldSoundManager.instance
                .addSoundRepeating(
                    this, PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), soundRadius, soundVolume, false
                );
            if ((int)GameTime.getInstance().getWorldAgeHours() != this.lastHour) {
                if (!this.getSquare().getProperties().has(IsoFlagType.exterior) && this.getSquare().getBuilding() != null) {
                    this.getSquare().getBuilding().setToxic(this.isActivated());
                }

                int elapsedHours = (int)GameTime.getInstance().getWorldAgeHours() - this.lastHour;
                float subtractFuel = 0.0F;
                int subtractCondition = 0;
                int conditionLowerChance = item != null ? item.getConditionLowerChance() : 30;

                for (int i = 0; i < elapsedHours; i++) {
                    float lowerFuel = this.totalPowerUsing;
                    lowerFuel = (float)(lowerFuel * SandboxOptions.instance.generatorFuelConsumption.getValue());
                    subtractFuel += lowerFuel;
                    if (Rand.Next(conditionLowerChance) == 0) {
                        subtractCondition += Rand.Next(2) + 1;
                    }

                    if (this.fuel - subtractFuel <= 0.0F || this.condition - subtractCondition <= 0) {
                        break;
                    }
                }

                this.fuel -= subtractFuel;
                if (this.fuel <= 0.0F) {
                    this.setActivated(false);
                    this.fuel = 0.0F;
                }

                this.condition -= subtractCondition;
                if (this.condition <= 0) {
                    this.setActivated(false);
                    this.condition = 0;
                }

                boolean bBackfire = false;
                if (this.condition <= 20) {
                    bBackfire = Rand.Next(5) == 0;
                } else if (this.condition <= 30) {
                    bBackfire = Rand.Next(10) == 0;
                } else if (this.condition <= 40) {
                    bBackfire = Rand.Next(15) == 0;
                }

                if (bBackfire) {
                    if (GameServer.server) {
                        GameServer.PlayWorldSoundServer(this.getSoundPrefix() + "Backfire", this.getSquare(), 40.0F, -1);
                    } else {
                        this.playGeneratorSound("Backfire");
                    }

                    WorldSoundManager.instance.addSound(this, this.square.getX(), this.square.getY(), this.square.getZ(), 40, 60, false, 0.0F, 15.0F);
                }

                if (this.condition <= 20) {
                    if (Rand.Next(10) == 0) {
                        IsoFireManager.StartFire(this.getCell(), this.square, true, 1000);
                        this.condition = 0;
                        this.setActivated(false);
                    } else if (Rand.Next(20) == 0) {
                        this.explode(this.square);
                        this.condition = 0;
                        this.setActivated(false);
                    }
                }

                this.lastHour = (int)GameTime.getInstance().getWorldAgeHours();
                if (GameServer.server) {
                    this.syncIsoObject(false, (byte)0, null, null);
                }
            }
        }

        if (this.emitter != null) {
            this.emitter.tick();
        }
    }

    public void setSurroundingElectricity() {
        this.itemsPowered.clear();
        this.totalPowerUsing = 0.02F;
        if (this.square != null && this.square.chunk != null) {
            int chunkX = this.square.chunk.wx;
            int chunkY = this.square.chunk.wy;

            for (int dy = -generatorChunkRange; dy <= generatorChunkRange; dy++) {
                for (int dx = -generatorChunkRange; dx <= generatorChunkRange; dx++) {
                    IsoChunk chunk = GameServer.server
                        ? ServerMap.instance.getChunk(chunkX + dx, chunkY + dy)
                        : IsoWorld.instance.currentCell.getChunk(chunkX + dx, chunkY + dy);
                    if (chunk != null && this.touchesChunk(chunk)) {
                        if (this.isActivated()) {
                            chunk.addGeneratorPos(this.square.x, this.square.y, this.square.z);
                        } else {
                            chunk.removeGeneratorPos(this.square.x, this.square.y, this.square.z);
                        }
                    }
                }
            }

            boolean AllowExteriorGenerator = SandboxOptions.getInstance().allowExteriorGenerator.getValue();
            int minX = this.square.getX() - generatorRadius;
            int maxX = this.square.getX() + generatorRadius;
            int minY = this.square.getY() - generatorRadius;
            int maxY = this.square.getY() + generatorRadius;
            int minZ = Math.max(-32, this.getSquare().getZ() - generatorVerticalPowerRange);
            int maxZ = Math.min(31, this.getSquare().getZ() + generatorVerticalPowerRange);

            for (int z = minZ; z < maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        if (!(
                            IsoUtils.DistanceToSquared(x + 0.5F, y + 0.5F, this.getSquare().getX() + 0.5F, this.getSquare().getY() + 0.5F)
                                > generatorRadius * generatorRadius
                        )) {
                            IsoGridSquare sq = this.getCell().getGridSquare(x, y, z);
                            if (sq != null) {
                                for (int i = 0; i < sq.getObjects().size(); i++) {
                                    IsoObject obj = sq.getObjects().get(i);
                                    if (obj != null && !(obj instanceof IsoWorldInventoryObject)) {
                                        if (obj instanceof IsoClothingDryer isoClothingDryer && isoClothingDryer.isActivated()) {
                                            this.addPoweredItem(obj, 0.09F);
                                        }

                                        if (obj instanceof IsoClothingWasher isoClothingWasher && isoClothingWasher.isActivated()) {
                                            this.addPoweredItem(obj, 0.09F);
                                        }

                                        if (obj instanceof IsoCombinationWasherDryer isoCombinationWasherDryer && isoCombinationWasherDryer.isActivated()) {
                                            this.addPoweredItem(obj, 0.09F);
                                        }

                                        if (obj instanceof IsoStackedWasherDryer swd) {
                                            float power = 0.0F;
                                            if (swd.isDryerActivated()) {
                                                power += 0.9F;
                                            }

                                            if (swd.isWasherActivated()) {
                                                power += 0.9F;
                                            }

                                            if (power > 0.0F) {
                                                this.addPoweredItem(obj, power);
                                            }
                                        }

                                        if (obj instanceof IsoTelevision isoTelevision && isoTelevision.getDeviceData().getIsTurnedOn()) {
                                            this.addPoweredItem(obj, 0.03F);
                                        }

                                        if (obj instanceof IsoRadio isoRadio
                                            && isoRadio.getDeviceData().getIsTurnedOn()
                                            && !isoRadio.getDeviceData().getIsBatteryPowered()) {
                                            this.addPoweredItem(obj, 0.01F);
                                        }

                                        if (obj instanceof IsoStove isoStove && isoStove.Activated()) {
                                            this.addPoweredItem(obj, 0.09F);
                                        }

                                        boolean bFridge = obj.getContainerByType("fridge") != null;
                                        boolean bFreezer = obj.getContainerByType("freezer") != null;
                                        if (bFridge && bFreezer) {
                                            this.addPoweredItem(obj, 0.13F);
                                        } else if (bFridge || bFreezer) {
                                            this.addPoweredItem(obj, 0.08F);
                                        }

                                        if (obj instanceof IsoLightSwitch isoLightSwitch && isoLightSwitch.activated && !isoLightSwitch.streetLight) {
                                            this.addPoweredItem(obj, 0.002F);
                                        }

                                        if (obj instanceof IsoCarBatteryCharger isoCarBatteryCharger && isoCarBatteryCharger.isActivated()) {
                                            this.addPoweredItem(obj, 0.05F);
                                        }

                                        if (obj.getPipedFuelAmount() > 0) {
                                            this.addPoweredItem(obj, 0.03F);
                                        }

                                        obj.checkHaveElectricity();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void addPoweredItem(IsoObject obj, float powerConsumption) {
        String name = Translator.getText("IGUI_VehiclePartCatOther");
        if (obj.getPipedFuelAmount() > 0) {
            name = Translator.getText("IGUI_GasPump");
        }

        PropertyContainer props = obj.getProperties();
        if (props != null && props.has("CustomName")) {
            String customName = "Moveable Object";
            if (props.has("CustomName")) {
                if (props.has("GroupName")) {
                    customName = props.get("GroupName") + " " + props.get("CustomName");
                } else {
                    customName = props.get("CustomName");
                }
            }

            name = Translator.getMoveableDisplayName(customName);
        }

        if (obj instanceof IsoLightSwitch) {
            name = Translator.getText("IGUI_Lights");
        }

        if (obj instanceof IsoCarBatteryCharger) {
            name = Translator.getText("IGUI_VehiclePartCatOther");
        }

        int nbr = 1;

        for (String test : this.itemsPowered.keySet()) {
            if (test.startsWith(name)) {
                nbr = Integer.parseInt(test.replaceAll("[\\D]", ""));
                this.totalPowerUsing -= powerConsumption * nbr;
                nbr++;
                this.itemsPowered.remove(test);
                break;
            }
        }

        this.itemsPowered.put(name + " x" + nbr, decimalFormatB.format(powerConsumption * nbr * SandboxOptions.instance.generatorFuelConsumption.getValue()));
        this.totalPowerUsing += powerConsumption * nbr;
    }

    private void updateFridgeFreezerItems(IsoObject object) {
        for (int i = 0; i < object.getContainerCount(); i++) {
            ItemContainer container = object.getContainerByIndex(i);
            if ("fridge".equals(container.getType()) || "freezer".equals(container.getType())) {
                ArrayList<InventoryItem> items = container.getItems();

                for (int j = 0; j < items.size(); j++) {
                    InventoryItem item = items.get(j);
                    if (item instanceof Food) {
                        item.updateAge();
                    }
                }
            }
        }
    }

    private void updateFridgeFreezerItems(IsoGridSquare square) {
        int objCount = square.getObjects().size();
        IsoObject[] objects = square.getObjects().getElements();

        for (int i = 0; i < objCount; i++) {
            IsoObject object = objects[i];
            this.updateFridgeFreezerItems(object);
        }
    }

    private void updateFridgeFreezerItems() {
        if (this.square != null) {
            int minX = this.square.getX() - generatorRadius;
            int maxX = this.square.getX() + generatorRadius;
            int minY = this.square.getY() - generatorRadius;
            int maxY = this.square.getY() + generatorRadius;
            int minZ = Math.max(-32, this.square.getZ() - generatorVerticalPowerRange);
            int maxZ = Math.min(31, this.square.getZ() + generatorVerticalPowerRange);

            for (int z = minZ; z < maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        if (IsoUtils.DistanceToSquared(x, y, this.square.x, this.square.y) <= generatorRadius * generatorRadius) {
                            IsoGridSquare square1 = this.getCell().getGridSquare(x, y, z);
                            if (square1 != null) {
                                this.updateFridgeFreezerItems(square1);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.connected = input.get() == 1;
        this.activated = input.get() == 1;
        this.fuel = Math.min(input.getFloat(), 10.0F);
        this.condition = input.getInt();
        this.lastHour = input.getInt();
        this.updateSurrounding = true;
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        output.put((byte)(this.isConnected() ? 1 : 0));
        output.put((byte)(this.isActivated() ? 1 : 0));
        output.putFloat(this.getFuel());
        output.putInt(this.getCondition());
        output.putInt(this.lastHour);
    }

    public void remove() {
        if (this.getSquare() != null) {
            this.getSquare().transmitRemoveItemFromSquare(this);
        }
    }

    @Override
    public void addToWorld() {
        this.getCell().addToProcessIsoObject(this);
        if (!AllGenerators.contains(this)) {
            AllGenerators.add(this);
        }
    }

    @Override
    public void removeFromWorld() {
        AllGenerators.remove(this);
        if (this.emitter != null) {
            this.emitter.stopAll();
            IsoWorld.instance.returnOwnershipOfEmitter(this.emitter);
            this.emitter = null;
        }

        super.removeFromWorld();
    }

    @Override
    public String getObjectName() {
        return "IsoGenerator";
    }

    public double getBasePowerConsumption() {
        return 0.02F * SandboxOptions.instance.generatorFuelConsumption.getValue();
    }

    public String getBasePowerConsumptionString() {
        return decimalFormat.format(0.02F * SandboxOptions.instance.generatorFuelConsumption.getValue());
    }

    public float getFuel() {
        return this.fuel;
    }

    public float getFuelPercentage() {
        return this.fuel / 10.0F * 100.0F;
    }

    public float getMaxFuel() {
        return 10.0F;
    }

    public void setFuel(float fuel) {
        this.fuel = Math.max(0.0F, Math.min(fuel, 10.0F));
        if (GameServer.server) {
            this.syncIsoObject(false, (byte)0, null, null);
        }

        if (GameClient.client) {
            this.syncIsoObject(false, (byte)0, null, null);
        }
    }

    public boolean isActivated() {
        return this.activated;
    }

    public void setActivated(boolean activated) {
        if (activated != this.activated) {
            if (!this.getSquare().getProperties().has(IsoFlagType.exterior) && this.getSquare().getBuilding() != null) {
                this.getSquare().getBuilding().setToxic(activated);
            }

            if (activated) {
                this.lastHour = (int)GameTime.getInstance().getWorldAgeHours();
                this.playGeneratorSound("Starting");
            } else {
                this.stopAllSounds();
                this.playGeneratorSound("Stopping");
            }

            try {
                this.updateFridgeFreezerItems();
            } catch (Throwable var3) {
                ExceptionLogger.logException(var3);
            }

            this.activated = activated;
            this.setSurroundingElectricity();
            if (GameClient.client) {
                this.syncIsoObject(false, (byte)0, null, null);
            }

            if (GameServer.server) {
                this.syncIsoObject(false, (byte)0, null, null);
            }
        }
    }

    public void failToStart() {
        if (!GameServer.server) {
            this.playGeneratorSound("FailedToStart");
        }
    }

    public int getCondition() {
        return this.condition;
    }

    public void setCondition(int condition) {
        this.condition = Math.max(0, Math.min(condition, 100));
        if (GameServer.server) {
            this.syncIsoObject(false, (byte)0, null, null);
        }

        if (GameClient.client) {
            this.syncIsoObject(false, (byte)0, null, null);
        }
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        if (GameClient.client) {
            this.syncIsoObject(false, (byte)0, null, null);
        }

        if (GameServer.server) {
            this.syncIsoObject(false, (byte)0, null, null);
        }
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        byte index = (byte)this.getObjectIndex();
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        b.putByte(index);
        b.putByte((byte)1);
        b.putByte((byte)0);
        b.putFloat(this.fuel);
        b.putInt(this.condition);
        b.putByte((byte)(this.activated ? 1 : 0));
        b.putByte((byte)(this.connected ? 1 : 0));
    }

    @Override
    public void syncIsoObjectReceive(ByteBuffer bb) {
        float fuel = bb.getFloat();
        int condition = bb.getInt();
        boolean activated = bb.get() == 1;
        boolean connected = bb.get() == 1;
        this.fuel = fuel;
        this.condition = condition;
        this.connected = connected;
        if (this.activated != activated) {
            try {
                this.updateFridgeFreezerItems();
            } catch (Throwable var7) {
                ExceptionLogger.logException(var7);
            }

            this.activated = activated;
            if (activated) {
                this.lastHour = (int)GameTime.getInstance().getWorldAgeHours();
                if (GameClient.client) {
                    this.playGeneratorSound("Starting");
                }
            } else if (GameClient.client) {
                this.stopAllSounds();
                this.playGeneratorSound("Stopping");
            }

            this.setSurroundingElectricity();
        }
    }

    private boolean touchesChunk(IsoChunk chunk) {
        IsoGridSquare square = this.getSquare();

        assert square != null;

        if (square == null) {
            return false;
        } else {
            int minX = chunk.wx * 8;
            int minY = chunk.wy * 8;
            int maxX = minX + 8 - 1;
            int maxY = minY + 8 - 1;
            if (square.x - generatorRadius > maxX) {
                return false;
            } else if (square.x + generatorRadius < minX) {
                return false;
            } else {
                return square.y - generatorRadius > maxY ? false : square.y + generatorRadius >= minY;
            }
        }
    }

    public static void chunkLoaded(IsoChunk chunk) {
        chunk.checkForMissingGenerators();

        for (int dy = -generatorChunkRange; dy <= generatorChunkRange; dy++) {
            for (int dx = -generatorChunkRange; dx <= generatorChunkRange; dx++) {
                if (dx != 0 || dy != 0) {
                    IsoChunk chunk1 = GameServer.server
                        ? ServerMap.instance.getChunk(chunk.wx + dx, chunk.wy + dy)
                        : IsoWorld.instance.currentCell.getChunk(chunk.wx + dx, chunk.wy + dy);
                    if (chunk1 != null) {
                        chunk1.checkForMissingGenerators();
                    }
                }
            }
        }

        for (int i = 0; i < AllGenerators.size(); i++) {
            IsoGenerator generator = AllGenerators.get(i);
            if (!generator.updateSurrounding && generator.touchesChunk(chunk)) {
                generator.updateSurrounding = true;
            }
        }
    }

    public static void updateSurroundingNow() {
        for (int i = 0; i < AllGenerators.size(); i++) {
            IsoGenerator generator = AllGenerators.get(i);
            if (generator.updateSurrounding && generator.getSquare() != null) {
                generator.updateSurrounding = false;
                generator.setSurroundingElectricity();
            }
        }
    }

    public static void updateGenerator(IsoGridSquare sq) {
        if (sq != null) {
            for (int i = 0; i < AllGenerators.size(); i++) {
                IsoGenerator generator = AllGenerators.get(i);
                if (generator.getSquare() != null) {
                    float distSq = IsoUtils.DistanceToSquared(
                        sq.x + 0.5F, sq.y + 0.5F, generator.getSquare().getX() + 0.5F, generator.getSquare().getY() + 0.5F
                    );
                    if (distSq <= generatorRadius * generatorRadius) {
                        generator.updateSurrounding = true;
                    }
                }
            }
        }
    }

    public static void Reset() {
        assert AllGenerators.isEmpty();

        AllGenerators.clear();
    }

    public static boolean isPoweringSquare(int generatorX, int generatorY, int generatorZ, int x, int y, int z) {
        int minZ = Math.max(-32, generatorZ - generatorVerticalPowerRange);
        int maxZ = Math.min(31, generatorZ + generatorVerticalPowerRange);
        return z >= minZ && z <= maxZ
            ? IsoUtils.DistanceToSquared(generatorX + 0.5F, generatorY + 0.5F, x + 0.5F, y + 0.5F) <= generatorRadius * generatorRadius
            : false;
    }

    public ArrayList<String> getItemsPowered() {
        ArrayList<String> result = new ArrayList<>();

        for (String test : this.itemsPowered.keySet()) {
            result.add(test + this.itemsPowered.get(test));
        }

        result.sort(String::compareToIgnoreCase);
        return result;
    }

    public float getTotalPowerUsing() {
        return (float)(this.totalPowerUsing * SandboxOptions.instance.generatorFuelConsumption.getValue());
    }

    public String getTotalPowerUsingString() {
        return decimalFormat.format(this.totalPowerUsing * SandboxOptions.instance.generatorFuelConsumption.getValue());
    }

    public void setTotalPowerUsing(float totalPowerUsing) {
        this.totalPowerUsing = totalPowerUsing;
    }

    public String getSoundPrefix() {
        if (this.getSprite() == null) {
            return "Generator";
        } else {
            PropertyContainer props = this.getSprite().getProperties();
            return props.has("GeneratorSound") ? props.get("GeneratorSound") : "Generator";
        }
    }

    private void stopAllSounds() {
        if (!GameServer.server) {
            if (this.emitter != null) {
                this.emitter.stopAll();
            }
        }
    }

    private void playGeneratorSound(String suffix) {
        if (!GameServer.server) {
            if (this.emitter == null) {
                this.emitter = IsoWorld.instance.getFreeEmitter(this.getXi() + 0.5F, this.getYi() + 0.5F, this.getZi());
                IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
            }

            this.playGeneratorSound(this.emitter, suffix);
        }
    }

    private void playGeneratorSound(BaseSoundEmitter emitter, String suffix) {
        emitter.playSoundImpl(this.getSoundPrefix() + suffix, this);
    }

    private void explode(IsoGridSquare isoGridSquare) {
        IsoFireManager.explode(isoGridSquare.getCell(), isoGridSquare, 100000);
    }
}
