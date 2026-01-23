// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatElement;
import zombie.chat.ChatElementOwner;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityType;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Drainable;
import zombie.iso.IsoGridSquare;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.radio.devices.WaveSignalDevice;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIFont;
import zombie.util.StringUtils;

@UsedFromLua
public final class VehiclePart extends GameEntity implements ChatElementOwner, WaveSignalDevice {
    protected BaseVehicle vehicle;
    protected boolean created;
    protected String partId;
    protected VehicleScript.Part scriptPart;
    protected ItemContainer container;
    protected InventoryItem item;
    private KahluaTable modData;
    private float lastUpdated = -1.0F;
    protected short updateFlags;
    protected VehiclePart parent;
    protected VehicleDoor door;
    protected VehicleWindow window;
    protected ArrayList<VehiclePart> children;
    protected String category;
    protected int condition = -1;
    protected boolean specificItem = true;
    private float wheelFriction;
    private int mechanicSkillInstaller;
    private float suspensionDamping;
    private float suspensionCompression;
    private float engineLoudness;
    private float durability;
    protected VehicleLight light;
    protected DeviceData deviceData;
    protected ChatElement chatElement;
    private boolean hasPlayerInRange;

    public VehiclePart(BaseVehicle vehicle) {
        this.vehicle = vehicle;
    }

    public BaseVehicle getVehicle() {
        return this.vehicle;
    }

    public void setScriptPart(VehicleScript.Part scriptPart) {
        this.scriptPart = scriptPart;
    }

    public VehicleScript.Part getScriptPart() {
        return this.scriptPart;
    }

    public ItemContainer getItemContainer() {
        return this.container;
    }

    public void setItemContainer(ItemContainer container) {
        if (container != null) {
            container.parent = this.getVehicle();
            container.vehiclePart = this;
        }

        this.container = container;
    }

    public boolean hasModData() {
        return this.modData != null && !this.modData.isEmpty();
    }

    public KahluaTable getModData() {
        if (this.modData == null) {
            this.modData = LuaManager.platform.newTable();
        }

        return this.modData;
    }

    public float getLastUpdated() {
        return this.lastUpdated;
    }

    public void setLastUpdated(float hours) {
        this.lastUpdated = hours;
    }

    public String getId() {
        return this.scriptPart == null ? this.partId : this.scriptPart.id;
    }

    public int getIndex() {
        return this.vehicle.parts.indexOf(this);
    }

    public String getArea() {
        return this.scriptPart == null ? null : this.scriptPart.area;
    }

    public ArrayList<String> getItemType() {
        return this.scriptPart == null ? null : this.scriptPart.itemType;
    }

    public KahluaTable getTable(String id) {
        if (this.scriptPart != null && this.scriptPart.tables != null) {
            KahluaTable table = this.scriptPart.tables.get(id);
            return table == null ? null : LuaManager.copyTable(table);
        } else {
            return null;
        }
    }

    public <T extends InventoryItem> T getInventoryItem() {
        return (T)this.item;
    }

    public void setInventoryItem(InventoryItem item, int mechanicSkill) {
        this.item = item;
        this.doInventoryItemStats(item, mechanicSkill);
        this.getVehicle().updateTotalMass();
        this.getVehicle().doDamageOverlay = true;
        if (this.isSetAllModelsVisible()) {
            this.setAllModelsVisible(item != null);
        }

        this.getVehicle().updatePartStats();
        if (!GameServer.server) {
            this.getVehicle().updateBulletStats();
        }
    }

    public void setInventoryItem(InventoryItem item) {
        this.setInventoryItem(item, 0);
    }

    public boolean isInventoryItemUninstalled() {
        return this.getItemType() != null && !this.getItemType().isEmpty() && this.getInventoryItem() == null;
    }

    public boolean isSetAllModelsVisible() {
        return this.scriptPart != null && this.scriptPart.setAllModelsVisible;
    }

    public void setAllModelsVisible(boolean visible) {
        if (this.scriptPart != null && this.scriptPart.models != null && !this.scriptPart.models.isEmpty()) {
            for (int i = 0; i < this.scriptPart.models.size(); i++) {
                VehicleScript.Model scriptModel = this.scriptPart.models.get(i);
                this.vehicle.setModelVisible(this, scriptModel, visible);
            }
        }
    }

    public void doInventoryItemStats(InventoryItem newItem, int mechanicSkill) {
        if (newItem != null) {
            if (this.isContainer()) {
                if (newItem.getMaxCapacity() > 0 && this.getScriptPart().container.conditionAffectsCapacity) {
                    this.setContainerCapacity((int)getNumberByCondition(newItem.getMaxCapacity(), newItem.getCondition(), 5.0F));
                } else if (newItem.getMaxCapacity() > 0) {
                    this.setContainerCapacity(newItem.getMaxCapacity());
                }

                this.setContainerContentAmount(newItem.getItemCapacity());
            }

            this.setSuspensionCompression(getNumberByCondition(newItem.getSuspensionCompression(), newItem.getCondition(), 0.6F));
            this.setSuspensionDamping(getNumberByCondition(newItem.getSuspensionDamping(), newItem.getCondition(), 0.6F));
            if (newItem.getEngineLoudness() > 0.0F) {
                this.setEngineLoudness(getNumberByCondition(newItem.getEngineLoudness(), newItem.getCondition(), 10.0F));
            }

            this.setCondition(newItem.getCondition());
            this.setMechanicSkillInstaller(mechanicSkill);
            if (newItem.getDurability() > 0.0F) {
                this.setDurability(newItem.getDurability());
            }
        } else {
            if (this.scriptPart != null && this.scriptPart.container != null) {
                if (this.scriptPart.container.capacity > 0) {
                    this.setContainerCapacity(this.scriptPart.container.capacity);
                } else {
                    this.setContainerCapacity(0);
                }
            }

            this.setMechanicSkillInstaller(0);
            this.setContainerContentAmount(0.0F);
            this.setSuspensionCompression(0.0F);
            this.setSuspensionDamping(0.0F);
            this.setWheelFriction(0.0F);
            this.setEngineLoudness(0.0F);
        }
    }

    public void setRandomCondition(InventoryItem item) {
        VehicleType type = VehicleType.getTypeFromName(this.getVehicle().getVehicleType());
        if (this.getVehicle().isGoodCar()) {
            int max = 100;
            if (item != null) {
                max = item.getConditionMax();
            }

            this.setCondition(Rand.Next(max - max / 3, max));
            if (item != null) {
                item.setCondition(this.getCondition(), false);
            }
        } else {
            int maxx = 100;
            if (item != null) {
                maxx = item.getConditionMax();
            }

            if (type != null) {
                maxx = PZMath.fastfloor(maxx * type.getRandomBaseVehicleQuality());
            }

            float cond = 100.0F;
            if (item != null) {
                int chanceToSpawnDamaged = item.getChanceToSpawnDamaged();
                if (type != null) {
                    chanceToSpawnDamaged += type.chanceToPartDamage;
                }

                if (chanceToSpawnDamaged > 0 && Rand.Next(100) < chanceToSpawnDamaged) {
                    cond = Rand.Next(maxx - maxx / 2, maxx);
                }
            } else {
                int chanceToSpawnDamagedx = 30;
                if (type != null) {
                    chanceToSpawnDamagedx += type.chanceToPartDamage;
                }

                if (Rand.Next(100) < chanceToSpawnDamagedx) {
                    cond = Rand.Next(maxx * 0.5F, (float)maxx);
                }
            }

            switch (SandboxOptions.instance.carGeneralCondition.getValue()) {
                case 1:
                    cond -= Rand.Next(cond * 0.3F, Rand.Next(cond * 0.3F, cond * 0.9F));
                    break;
                case 2:
                    cond -= Rand.Next(cond * 0.1F, cond * 0.3F);
                case 3:
                default:
                    break;
                case 4:
                    cond += Rand.Next(cond * 0.2F, cond * 0.4F);
                    break;
                case 5:
                    cond += Rand.Next(cond * 0.5F, cond * 0.9F);
            }

            cond = Math.max(0.0F, cond);
            cond = Math.min(100.0F, cond);
            this.setCondition((int)cond);
            if (item != null) {
                item.setCondition(this.getCondition(), false);
            }
        }
    }

    public void setGeneralCondition(InventoryItem item, float baseQuality, float chanceToSpawnDamaged) {
        int max = 100;
        max = (int)(max * baseQuality);
        float cond = 100.0F;
        if (item != null) {
            int chanceToSpawnDamagedTot = item.getChanceToSpawnDamaged();
            chanceToSpawnDamagedTot += (int)chanceToSpawnDamaged;
            if (chanceToSpawnDamagedTot > 0 && Rand.Next(100) < chanceToSpawnDamagedTot) {
                cond = Rand.Next(max - max / 2, max);
            }
        } else {
            int chanceToSpawnDamagedTot = 30;
            chanceToSpawnDamagedTot += (int)chanceToSpawnDamaged;
            if (Rand.Next(100) < chanceToSpawnDamagedTot) {
                cond = Rand.Next(max * 0.5F, (float)max);
            }
        }

        switch (SandboxOptions.instance.carGeneralCondition.getValue()) {
            case 1:
                cond -= Rand.Next(cond * 0.3F, Rand.Next(cond * 0.3F, cond * 0.9F));
                break;
            case 2:
                cond -= Rand.Next(cond * 0.1F, cond * 0.3F);
            case 3:
            default:
                break;
            case 4:
                cond += Rand.Next(cond * 0.2F, cond * 0.4F);
                break;
            case 5:
                cond += Rand.Next(cond * 0.5F, cond * 0.9F);
        }

        cond = Math.max(0.0F, cond);
        cond = Math.min(100.0F, cond);
        this.setCondition((int)cond);
        if (item != null) {
            item.setCondition(this.getCondition(), false);
        }
    }

    public static float getNumberByCondition(float number, float cond, float min) {
        cond += 20.0F * (100.0F - cond) / 100.0F;
        float condDelta = cond / 100.0F;
        return Math.round(Math.max(min, number * condDelta) * 100.0F) / 100.0F;
    }

    public boolean isContainer() {
        return this.scriptPart == null ? false : this.scriptPart.container != null;
    }

    public int getContainerCapacity() {
        return this.getContainerCapacity(null);
    }

    public int getContainerCapacity(IsoGameCharacter chr) {
        if (!this.isContainer()) {
            return 0;
        } else if (this.getItemContainer() != null) {
            return chr == null ? this.getItemContainer().getCapacity() : this.getItemContainer().getEffectiveCapacity(chr);
        } else if (this.getInventoryItem() != null) {
            return this.scriptPart.container.conditionAffectsCapacity
                ? (int)getNumberByCondition(this.<InventoryItem>getInventoryItem().getMaxCapacity(), this.getCondition(), 5.0F)
                : this.<InventoryItem>getInventoryItem().getMaxCapacity();
        } else {
            return this.scriptPart.container.capacity;
        }
    }

    public void setContainerCapacity(int cap) {
        if (this.isContainer()) {
            if (this.getItemContainer() != null) {
                this.getItemContainer().capacity = cap;
            }
        }
    }

    public String getContainerContentType() {
        return !this.isContainer() ? null : this.scriptPart.container.contentType;
    }

    public float getContainerContentAmount() {
        if (!this.isContainer()) {
            return 0.0F;
        } else {
            return this.hasModData() && this.getModData().rawget("contentAmount") instanceof Double d ? d.floatValue() : 0.0F;
        }
    }

    public void setContainerContentAmount(float amount) {
        this.setContainerContentAmount(amount, false, false);
    }

    public void setContainerContentAmount(float amount, boolean force, boolean noUpdateMass) {
        if (this.isContainer()) {
            int cap = this.scriptPart.container.capacity;
            if (this.getInventoryItem() != null) {
                cap = this.<InventoryItem>getInventoryItem().getMaxCapacity();
            }

            if (!force) {
                amount = Math.min(amount, (float)cap);
            }

            amount = Math.max(amount, 0.0F);
            this.getModData().rawset("contentAmount", (double)amount);
            if (this.getInventoryItem() != null) {
                this.<InventoryItem>getInventoryItem().setItemCapacity(amount);
            }

            if (!noUpdateMass) {
                this.getVehicle().updateTotalMass();
            }
        }
    }

    public int getContainerSeatNumber() {
        return !this.isContainer() ? -1 : this.scriptPart.container.seat;
    }

    public boolean isSeat() {
        VehicleScript.Part part = this.scriptPart;
        if (part == null) {
            return false;
        } else if (part.container == null) {
            return false;
        } else if (part.container.seatId == null) {
            return false;
        } else {
            return part.container.seatId.isEmpty() ? false : part.container.seat > -1;
        }
    }

    public boolean isVehicleTrunk() {
        String partId = this.getId();
        return partId == null ? false : StringUtils.containsIgnoreCase(partId, "TruckBed");
    }

    public String getLuaFunction(String name) {
        return this.scriptPart != null && this.scriptPart.luaFunctions != null ? this.scriptPart.luaFunctions.get(name) : null;
    }

    protected VehicleScript.Model getScriptModelById(String id) {
        if (this.scriptPart != null && this.scriptPart.models != null) {
            for (int i = 0; i < this.scriptPart.models.size(); i++) {
                VehicleScript.Model scriptModel = this.scriptPart.models.get(i);
                if (id.equals(scriptModel.id)) {
                    return scriptModel;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public void setModelVisible(String id, boolean visible) {
        VehicleScript.Model model = this.getScriptModelById(id);
        if (model != null) {
            this.vehicle.setModelVisible(this, model, visible);
        }
    }

    public VehiclePart getParent() {
        return this.parent;
    }

    public void addChild(VehiclePart child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }

        this.children.add(child);
    }

    public int getChildCount() {
        return this.children == null ? 0 : this.children.size();
    }

    public VehiclePart getChild(int index) {
        return this.children != null && index >= 0 && index < this.children.size() ? this.children.get(index) : null;
    }

    public VehicleDoor getDoor() {
        return this.door;
    }

    public VehicleDoor getEnclosingDoor() {
        return this.door == null && this.parent != null ? this.parent.getEnclosingDoor() : this.door;
    }

    public VehicleWindow getWindow() {
        return this.window;
    }

    public VehiclePart getChildWindow() {
        for (int i = 0; i < this.getChildCount(); i++) {
            VehiclePart child = this.getChild(i);
            if (child.getWindow() != null) {
                return child;
            }
        }

        return null;
    }

    public VehicleWindow findWindow() {
        VehiclePart windowPart = this.getChildWindow();
        return windowPart == null ? null : windowPart.getWindow();
    }

    public VehicleScript.Anim getAnimById(String id) {
        if (this.scriptPart != null && this.scriptPart.anims != null) {
            for (int i = 0; i < this.scriptPart.anims.size(); i++) {
                VehicleScript.Anim anim = this.scriptPart.anims.get(i);
                if (anim.id.equals(id)) {
                    return anim;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public void save(ByteBuffer output) throws IOException {
        GameWindow.WriteStringUTF(output, this.getId());
        output.put((byte)(this.created ? 1 : 0));
        output.putFloat(this.lastUpdated);
        if (this.getInventoryItem() == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.<InventoryItem>getInventoryItem().saveWithSize(output, false);
        }

        if (this.getItemContainer() == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.getItemContainer().save(output);
        }

        if (this.hasModData() && !this.getModData().isEmpty()) {
            output.put((byte)1);
            this.getModData().save(output);
        } else {
            output.put((byte)0);
        }

        if (this.getDeviceData() == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.getDeviceData().save(output, false);
        }

        if (this.light == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.light.save(output);
        }

        if (this.door == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.door.save(output);
        }

        if (this.window == null) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.window.save(output);
        }

        output.putInt(this.condition);
        output.putFloat(this.wheelFriction);
        output.putInt(this.mechanicSkillInstaller);
        output.putFloat(this.suspensionCompression);
        output.putFloat(this.suspensionDamping);
        if (!this.requiresEntitySave()) {
            output.put((byte)0);
        } else {
            output.put((byte)1);
            this.saveEntity(output);
        }
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.partId = GameWindow.ReadStringUTF(input);
        this.created = input.get() == 1;
        this.lastUpdated = input.getFloat();
        if (input.get() == 1) {
            InventoryItem item = InventoryItem.loadItem(input, WorldVersion);
            this.item = item;
        }

        if (input.get() == 1) {
            if (this.container == null) {
                this.container = new ItemContainer();
                this.container.parent = this.getVehicle();
                this.container.vehiclePart = this;
            }

            this.container.getItems().clear();
            this.container.id = 0;
            this.container.load(input, WorldVersion);
        }

        if (input.get() == 1) {
            this.getModData().load(input, WorldVersion);
        }

        if (input.get() == 1) {
            if (this.getDeviceData() == null) {
                this.createSignalDevice();
            }

            this.getDeviceData().load(input, WorldVersion, false);
        }

        if (input.get() == 1) {
            if (this.light == null) {
                this.light = new VehicleLight();
            }

            this.light.load(input, WorldVersion);
        }

        if (input.get() == 1) {
            if (this.door == null) {
                this.door = new VehicleDoor(this);
            }

            this.door.load(input, WorldVersion);
        }

        if (input.get() == 1) {
            if (this.window == null) {
                this.window = new VehicleWindow(this);
            }

            this.window.load(input, WorldVersion);
        }

        this.setCondition(input.getInt());
        this.setWheelFriction(input.getFloat());
        this.setMechanicSkillInstaller(input.getInt());
        this.setSuspensionCompression(input.getFloat());
        this.setSuspensionDamping(input.getFloat());
        if (WorldVersion >= 200 && input.get() == 1) {
            this.loadEntity(input, WorldVersion);
        }
    }

    public int getWheelIndex() {
        if (this.scriptPart != null && this.scriptPart.wheel != null) {
            for (int i = 0; i < this.vehicle.script.getWheelCount(); i++) {
                VehicleScript.Wheel scriptWheel = this.vehicle.script.getWheel(i);
                if (this.scriptPart.wheel.equals(scriptWheel.id)) {
                    return i;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }

    public void createSpotLight(float xOffset, float yOffset, float dist, float intensity, float dot, int focusing) {
        this.light = this.light == null ? new VehicleLight() : this.light;
        this.light.offset.set(xOffset, yOffset, 0.0F);
        this.light.dist = dist;
        this.light.intensity = intensity;
        this.light.dot = dot;
        this.light.focusing = focusing;
    }

    public VehicleLight getLight() {
        return this.light;
    }

    public float getLightDistance() {
        return this.light == null ? 0.0F : PZMath.lerp(this.light.dist * 0.5F, this.light.dist, this.getCondition() / 100.0F);
    }

    public float getLightIntensity() {
        return this.light == null ? 0.0F : PZMath.lerp(this.light.intensity * 0.5F, this.light.intensity, this.getCondition() / 100.0F);
    }

    public float getLightFocusing() {
        return this.light == null ? 0.0F : 20 + (int)(80.0F * (1.0F - this.getCondition() / 100.0F));
    }

    public void setLightActive(boolean active) {
        if (this.light != null && this.light.active != active) {
            this.light.active = active;
            if (GameServer.server) {
                this.vehicle.updateFlags = (short)(this.vehicle.updateFlags | 8);
            }
        }
    }

    public DeviceData createSignalDevice() {
        if (this.deviceData == null) {
            this.deviceData = new DeviceData(this);
        }

        if (this.chatElement == null) {
            this.chatElement = new ChatElement(this, 5, "device");
        }

        return this.deviceData;
    }

    public boolean hasDevicePower() {
        return this.vehicle.getBatteryCharge() > 0.0F;
    }

    @Override
    public DeviceData getDeviceData() {
        return this.deviceData;
    }

    @Override
    public void setDeviceData(DeviceData data) {
        if (data == null) {
            data = new DeviceData(this);
        }

        if (this.deviceData != null) {
            this.deviceData.cleanSoundsAndEmitter();
        }

        this.deviceData = data;
        this.deviceData.setParent(this);
    }

    @Override
    public float getDelta() {
        return this.deviceData != null ? this.deviceData.getPower() : 0.0F;
    }

    @Override
    public void setDelta(float d) {
        if (this.deviceData != null) {
            this.deviceData.setPower(d);
        }
    }

    @Override
    public float getX() {
        return this.vehicle.getX();
    }

    @Override
    public float getY() {
        return this.vehicle.getY();
    }

    @Override
    public float getZ() {
        return this.vehicle.getZ();
    }

    @Override
    public IsoGridSquare getSquare() {
        return this.vehicle.getSquare();
    }

    @Override
    public void AddDeviceText(String line, float r, float g, float b, String guid, String codes, int distance) {
        if (this.deviceData != null && this.deviceData.getIsTurnedOn()) {
            if (!ZomboidRadio.isStaticSound(line)) {
                this.deviceData.doReceiveSignal(distance);
            }

            if (this.deviceData.getDeviceVolume() > 0.0F) {
                this.chatElement
                    .addChatLine(line, r, g, b, UIFont.Medium, this.deviceData.getDeviceVolumeRange(), "default", true, true, true, true, true, true);
                if (codes != null) {
                    LuaEventManager.triggerEvent("OnDeviceText", guid, codes, this.getX(), this.getY(), this.getZ(), line, this);
                }
            }
        }
    }

    @Override
    public boolean HasPlayerInRange() {
        return this.hasPlayerInRange;
    }

    private boolean playerWithinBounds(IsoPlayer player, float dist) {
        return player != null && !player.isDead()
            ? (player.getX() > this.getX() - dist || this.getX() < this.getX() + dist)
                && (player.getY() > this.getY() - dist || this.getY() < this.getY() + dist)
            : false;
    }

    public void updateSignalDevice() {
        if (this.deviceData != null) {
            if (this.deviceData.getIsTurnedOn() && this.isInventoryItemUninstalled()) {
                this.deviceData.setIsTurnedOn(false);
            }

            if (GameClient.client) {
                this.deviceData.updateSimple();
            } else {
                this.deviceData.update(true, this.hasPlayerInRange);
            }

            if (!GameServer.server) {
                this.hasPlayerInRange = false;
                if (this.deviceData.getIsTurnedOn()) {
                    for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                        IsoPlayer player = IsoPlayer.players[i];
                        if (this.playerWithinBounds(player, this.deviceData.getDeviceVolumeRange() * 0.6F)) {
                            this.hasPlayerInRange = true;
                            break;
                        }
                    }
                }

                this.chatElement.setHistoryRange(this.deviceData.getDeviceVolumeRange() * 0.6F);
                this.chatElement.update();
            } else {
                this.hasPlayerInRange = false;
            }
        }
    }

    public String getCategory() {
        return this.category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getCondition() {
        return this.condition;
    }

    public void setCondition(int condition) {
        condition = Math.min(100, condition);
        condition = Math.max(0, condition);
        if (this.getVehicle().getDriverRegardlessOfTow() != null) {
            if (this.condition > 60 && condition < 60 && condition > 40) {
                LuaEventManager.triggerEvent("OnVehicleDamageTexture", this.getVehicle().getDriverRegardlessOfTow());
            }

            if (this.condition > 40 && condition < 40) {
                LuaEventManager.triggerEvent("OnVehicleDamageTexture", this.getVehicle().getDriverRegardlessOfTow());
            }
        }

        this.condition = condition;
        if (this.getInventoryItem() != null) {
            this.<InventoryItem>getInventoryItem().setCondition(condition, false);
        }

        this.getVehicle().doDamageOverlay = true;
        if (condition <= 0 && "lightbar".equals(this.getId())) {
            this.getVehicle().lightbarLightsMode.set(0);
            this.getVehicle().setLightbarSirenMode(0);
        }

        if (this.scriptPart != null && this.scriptPart.id != null && this.scriptPart.id.equals("TrailerTrunk")) {
            this.getItemContainer().setCapacity(Math.max(80, condition));
        }
    }

    public void damage(int amount) {
        if (this.getWindow() != null) {
            this.getWindow().damage(amount);
        } else {
            this.setCondition(this.getCondition() - amount);
            this.getVehicle().transmitPartCondition(this);
        }
    }

    public boolean isSpecificItem() {
        return this.specificItem;
    }

    public void setSpecificItem(boolean specificItem) {
        this.specificItem = specificItem;
    }

    public float getWheelFriction() {
        return this.wheelFriction;
    }

    public void setWheelFriction(float wheelFriction) {
        this.wheelFriction = wheelFriction;
    }

    public int getMechanicSkillInstaller() {
        return this.mechanicSkillInstaller;
    }

    public void setMechanicSkillInstaller(int mechanicSkillInstaller) {
        this.mechanicSkillInstaller = mechanicSkillInstaller;
    }

    public float getSuspensionDamping() {
        return this.suspensionDamping;
    }

    public void setSuspensionDamping(float suspensionDamping) {
        this.suspensionDamping = suspensionDamping;
    }

    public float getSuspensionCompression() {
        return this.suspensionCompression;
    }

    public void setSuspensionCompression(float suspensionCompression) {
        this.suspensionCompression = suspensionCompression;
    }

    public float getEngineLoudness() {
        return this.engineLoudness;
    }

    public void setEngineLoudness(float engineLoudness) {
        this.engineLoudness = engineLoudness;
    }

    public void repair() {
        VehicleScript vehicleScript = this.vehicle.getScript();
        float contentAmount = this.getContainerContentAmount();
        if (this.isInventoryItemUninstalled()) {
            String itemType = this.getItemType().get(Rand.Next(this.getItemType().size()));
            if (itemType != null && !itemType.isEmpty()) {
                InventoryItem item = InventoryItemFactory.CreateItem(itemType);
                if (item != null) {
                    this.setInventoryItem(item);
                    if (item.getMaxCapacity() > 0) {
                        item.setItemCapacity(item.getMaxCapacity());
                    }

                    this.vehicle.transmitPartItem(this);
                    this.callLuaVoid(this.getLuaFunction("init"), this.vehicle, this);
                }
            }
        }

        if (this.getDoor() != null && this.getDoor().isLockBroken()) {
            this.getDoor().setLockBroken(false);
            this.vehicle.transmitPartDoor(this);
        }

        if (this.getCondition() != 100) {
            this.setCondition(100);
            if (this.getInventoryItem() != null) {
                this.doInventoryItemStats(this.getInventoryItem(), this.getMechanicSkillInstaller());
            }

            this.vehicle.transmitPartCondition(this);
        }

        if (this.isContainer() && this.getItemContainer() == null && contentAmount != this.getContainerCapacity()) {
            this.setContainerContentAmount(this.getContainerCapacity());
            this.vehicle.transmitPartModData(this);
        }

        if (this.getInventoryItem() instanceof Drainable && this.<InventoryItem>getInventoryItem().getCurrentUsesFloat() < 1.0F) {
            this.<InventoryItem>getInventoryItem().setCurrentUses(this.<InventoryItem>getInventoryItem().getMaxUses());
            this.vehicle.transmitPartUsedDelta(this);
        }

        if ("Engine".equalsIgnoreCase(this.getId())) {
            int quality = 100;
            int loudness = (int)(vehicleScript.getEngineLoudness() * SandboxOptions.getInstance().zombieAttractionMultiplier.getValue());
            int power = (int)vehicleScript.getEngineForce();
            this.vehicle.setEngineFeature(100, loudness, power);
            this.vehicle.transmitEngine();
        }

        this.vehicle.updatePartStats();
        this.vehicle.updateBulletStats();
    }

    private void callLuaVoid(String functionName, Object arg1, Object arg2) {
        Object functionObj = LuaManager.getFunctionObject(functionName);
        if (functionObj != null) {
            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, arg1, arg2);
        }
    }

    public ChatElement getChatElement() {
        return this.chatElement;
    }

    @Override
    public GameEntityType getGameEntityType() {
        return GameEntityType.VehiclePart;
    }

    @Override
    public boolean isEntityValid() {
        return true;
    }

    @Override
    public long getEntityNetID() {
        long a = (long)this.getVehicle().vehicleId << 31;
        long b = this.getIndex();
        return a + b;
    }

    public void setDurability(float durability) {
        if (durability > 0.0F) {
            this.durability = durability;
        }
    }

    public float getDurability() {
        return this.durability;
    }

    public String getMechanicArea() {
        return this.scriptPart.getMechanicArea();
    }

    public void setFlag(short flag) {
        this.updateFlags |= flag;
    }

    public boolean getFlag(short flag) {
        return (this.updateFlags & flag) != 0;
    }

    public void clearFlags() {
        this.updateFlags = 0;
    }
}
