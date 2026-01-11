// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector4f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.core.BoxedStaticValues;
import zombie.core.ImmutableColor;
import zombie.core.math.PZMath;
import zombie.core.physics.Bullet;
import zombie.core.random.Rand;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class VehicleScript extends BaseScriptObject implements IModelAttachmentOwner {
    private String fileName;
    private String name;
    private final ArrayList<VehicleScript.Model> models = new ArrayList<>();
    public final ArrayList<ModelAttachment> attachments = new ArrayList<>();
    private float mass = 800.0F;
    private final Vector3f centerOfMassOffset = new Vector3f();
    private float engineForce = 3000.0F;
    private float engineIdleSpeed = 750.0F;
    private float steeringIncrement = 0.04F;
    private float steeringClamp = 0.4F;
    private final float steeringClampMax = 0.9F;
    private float wheelFriction = 800.0F;
    private float stoppingMovementForce = 1.0F;
    private float animalTrailerSize;
    private float suspensionStiffness = 20.0F;
    private float suspensionDamping = 2.3F;
    private float suspensionCompression = 4.4F;
    private float suspensionRestLength = 0.6F;
    private float maxSuspensionTravelCm = 500.0F;
    private float rollInfluence = 0.1F;
    private final Vector3f extents = new Vector3f(0.75F, 0.5F, 2.0F);
    private final Vector2f shadowExtents = new Vector2f(0.0F, 0.0F);
    private final Vector2f shadowOffset = new Vector2f(0.0F, 0.0F);
    private boolean hadShadowOExtents;
    private boolean hadShadowOffset;
    private final Vector2f extentsOffset = new Vector2f(0.5F, 0.5F);
    private final Vector3f physicsChassisShape = new Vector3f(0.0F);
    private final ArrayList<VehicleScript.PhysicsShape> physicsShapes = new ArrayList<>();
    private final ArrayList<VehicleScript.Wheel> wheels = new ArrayList<>();
    private final ArrayList<VehicleScript.Passenger> passengers = new ArrayList<>();
    public float maxSpeed = 20.0F;
    private boolean useChassisPhysicsCollision = true;
    public float maxSpeedReverse = 40.0F;
    public boolean isSmallVehicle = true;
    public float spawnOffsetY;
    private int frontEndHealth = 100;
    private int rearEndHealth = 100;
    private int storageCapacity = 100;
    private int engineLoudness = 100;
    private int engineQuality = 100;
    private int seats = 2;
    private int mechanicType;
    private int engineRepairLevel;
    private float playerDamageProtection;
    private float forcedHue = -1.0F;
    private float forcedSat = -1.0F;
    private float forcedVal = -1.0F;
    public ImmutableColor leftSirenCol;
    public ImmutableColor rightSirenCol;
    private String engineRpmType = "jeep";
    private float offroadEfficiency = 1.0F;
    private final TFloatArrayList crawlOffsets = new TFloatArrayList();
    private ArrayList<String> zombieType;
    private ArrayList<String> specialKeyRing;
    private boolean notKillCrops;
    private boolean hasLighter = true;
    private String carMechanicsOverlay;
    private String carModelName;
    private int specialLootChance = 8;
    private int specialKeyRingChance;
    private boolean neverSpawnKey;
    public int gearRatioCount;
    public final float[] gearRatio = new float[9];
    private final VehicleScript.Skin textures = new VehicleScript.Skin();
    private final ArrayList<VehicleScript.Skin> skins = new ArrayList<>();
    private final ArrayList<VehicleScript.Area> areas = new ArrayList<>();
    private final ArrayList<VehicleScript.Part> parts = new ArrayList<>();
    private boolean hasSiren;
    private final VehicleScript.LightBar lightbar = new VehicleScript.LightBar();
    private final VehicleScript.Sounds sound = new VehicleScript.Sounds();
    public boolean textureMaskEnable;
    public static final int PHYSICS_SHAPE_BOX = 1;
    public static final int PHYSICS_SHAPE_SPHERE = 2;
    public static final int PHYSICS_SHAPE_MESH = 3;

    public VehicleScript() {
        super(ScriptType.Vehicle);
        this.gearRatioCount = 4;
        this.gearRatio[0] = 7.09F;
        this.gearRatio[1] = 6.44F;
        this.gearRatio[2] = 4.1F;
        this.gearRatio[3] = 2.29F;
        this.gearRatio[4] = 1.47F;
        this.gearRatio[5] = 1.0F;
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
        ScriptManager scriptMgr = ScriptManager.instance;
        this.fileName = scriptMgr.currentFileName;
        this.name = name;
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);

        for (ScriptParser.BlockElement element : block.elements) {
            if (element.asValue() != null) {
                String[] ss = element.asValue().string.split("=");
                String k = ss[0].trim();
                String v = ss[1].trim();
                if ("extents".equals(k)) {
                    this.LoadVector3f(v, this.extents);
                } else if ("shadowExtents".equals(k)) {
                    this.LoadVector2f(v, this.shadowExtents);
                    this.hadShadowOExtents = true;
                } else if ("shadowOffset".equals(k)) {
                    this.LoadVector2f(v, this.shadowOffset);
                    this.hadShadowOffset = true;
                } else if ("physicsChassisShape".equals(k)) {
                    this.LoadVector3f(v, this.physicsChassisShape);
                } else if ("extentsOffset".equals(k)) {
                    this.LoadVector2f(v, this.extentsOffset);
                } else if ("mass".equals(k)) {
                    this.mass = Float.parseFloat(v);
                } else if ("offRoadEfficiency".equalsIgnoreCase(k)) {
                    this.offroadEfficiency = Float.parseFloat(v);
                } else if ("centerOfMassOffset".equals(k)) {
                    this.LoadVector3f(v, this.centerOfMassOffset);
                } else if ("engineForce".equals(k)) {
                    this.engineForce = Float.parseFloat(v);
                } else if ("engineIdleSpeed".equals(k)) {
                    this.engineIdleSpeed = Float.parseFloat(v);
                } else if ("gearRatioCount".equals(k)) {
                    this.gearRatioCount = Integer.parseInt(v);
                } else if ("gearRatioR".equals(k)) {
                    this.gearRatio[0] = Float.parseFloat(v);
                } else if ("gearRatio1".equals(k)) {
                    this.gearRatio[1] = Float.parseFloat(v);
                } else if ("gearRatio2".equals(k)) {
                    this.gearRatio[2] = Float.parseFloat(v);
                } else if ("gearRatio3".equals(k)) {
                    this.gearRatio[3] = Float.parseFloat(v);
                } else if ("gearRatio4".equals(k)) {
                    this.gearRatio[4] = Float.parseFloat(v);
                } else if ("gearRatio5".equals(k)) {
                    this.gearRatio[5] = Float.parseFloat(v);
                } else if ("gearRatio6".equals(k)) {
                    this.gearRatio[6] = Float.parseFloat(v);
                } else if ("gearRatio7".equals(k)) {
                    this.gearRatio[7] = Float.parseFloat(v);
                } else if ("gearRatio8".equals(k)) {
                    this.gearRatio[8] = Float.parseFloat(v);
                } else if ("textureMaskEnable".equals(k)) {
                    this.textureMaskEnable = Boolean.parseBoolean(v);
                } else if ("textureRust".equals(k)) {
                    this.textures.textureRust = StringUtils.discardNullOrWhitespace(v);
                } else if ("textureMask".equals(k)) {
                    this.textures.textureMask = StringUtils.discardNullOrWhitespace(v);
                } else if ("textureLights".equals(k)) {
                    this.textures.textureLights = StringUtils.discardNullOrWhitespace(v);
                } else if ("textureDamage1Overlay".equals(k)) {
                    this.textures.textureDamage1Overlay = StringUtils.discardNullOrWhitespace(v);
                } else if ("textureDamage1Shell".equals(k)) {
                    this.textures.textureDamage1Shell = StringUtils.discardNullOrWhitespace(v);
                } else if ("textureDamage2Overlay".equals(k)) {
                    this.textures.textureDamage2Overlay = StringUtils.discardNullOrWhitespace(v);
                } else if ("textureDamage2Shell".equals(k)) {
                    this.textures.textureDamage2Shell = StringUtils.discardNullOrWhitespace(v);
                } else if ("textureShadow".equals(k)) {
                    this.textures.textureShadow = StringUtils.discardNullOrWhitespace(v);
                } else if ("rollInfluence".equals(k)) {
                    this.rollInfluence = Float.parseFloat(v);
                } else if ("steeringIncrement".equals(k)) {
                    this.steeringIncrement = Float.parseFloat(v);
                } else if ("steeringClamp".equals(k)) {
                    this.steeringClamp = Float.parseFloat(v);
                } else if ("suspensionStiffness".equals(k)) {
                    this.suspensionStiffness = Float.parseFloat(v);
                } else if ("suspensionDamping".equals(k)) {
                    this.suspensionDamping = Float.parseFloat(v);
                } else if ("suspensionCompression".equals(k)) {
                    this.suspensionCompression = Float.parseFloat(v);
                } else if ("suspensionRestLength".equals(k)) {
                    this.suspensionRestLength = Float.parseFloat(v);
                } else if ("maxSuspensionTravelCm".equals(k)) {
                    this.maxSuspensionTravelCm = Float.parseFloat(v);
                } else if ("wheelFriction".equals(k)) {
                    this.wheelFriction = Float.parseFloat(v);
                } else if ("stoppingMovementForce".equals(k)) {
                    this.stoppingMovementForce = Float.parseFloat(v);
                } else if ("animalTrailerSize".equalsIgnoreCase(k)) {
                    this.animalTrailerSize = Float.parseFloat(v);
                } else if ("maxSpeed".equals(k)) {
                    this.maxSpeed = Float.parseFloat(v);
                } else if ("maxSpeedReverse".equals(k)) {
                    this.maxSpeedReverse = Float.parseFloat(v);
                } else if ("isSmallVehicle".equals(k)) {
                    this.isSmallVehicle = Boolean.parseBoolean(v);
                } else if ("spawnOffsetY".equals(k)) {
                    this.spawnOffsetY = Float.parseFloat(v) - 0.995F;
                } else if ("frontEndDurability".equals(k)) {
                    this.frontEndHealth = Integer.parseInt(v);
                } else if ("rearEndDurability".equals(k)) {
                    this.rearEndHealth = Integer.parseInt(v);
                } else if ("storageCapacity".equals(k)) {
                    this.storageCapacity = Integer.parseInt(v);
                } else if ("engineLoudness".equals(k)) {
                    this.engineLoudness = Integer.parseInt(v);
                } else if ("engineQuality".equals(k)) {
                    this.engineQuality = Integer.parseInt(v);
                } else if ("seats".equals(k)) {
                    this.seats = Integer.parseInt(v);
                } else if ("hasSiren".equals(k)) {
                    this.hasSiren = Boolean.parseBoolean(v);
                } else if ("mechanicType".equals(k)) {
                    this.mechanicType = Integer.parseInt(v);
                } else if ("forcedColor".equals(k)) {
                    String[] hsv = v.split(" ");
                    this.setForcedHue(Float.parseFloat(hsv[0]));
                    this.setForcedSat(Float.parseFloat(hsv[1]));
                    this.setForcedVal(Float.parseFloat(hsv[2]));
                } else if ("engineRPMType".equals(k)) {
                    this.engineRpmType = v.trim();
                } else if ("zombieType".equals(k)) {
                    this.zombieType = new ArrayList<>();
                    String[] split = v.split(";");

                    for (int i = 0; i < split.length; i++) {
                        this.zombieType.add(split[i].trim());
                    }
                } else if ("specialKeyRing".equals(k)) {
                    this.specialKeyRing = new ArrayList<>();
                    String[] split = v.split(";");

                    for (int i = 0; i < split.length; i++) {
                        String itemType = split[i].trim();
                        if (!StringUtils.isNullOrWhitespace(itemType)) {
                            this.specialKeyRing.add(itemType);
                        }
                    }
                } else if ("notKillCrops".equals(k)) {
                    this.notKillCrops = Boolean.parseBoolean(v);
                } else if ("hasLighter".equals(k)) {
                    this.hasLighter = Boolean.parseBoolean(v);
                } else if ("neverSpawnKey".equals(k)) {
                    this.neverSpawnKey = Boolean.parseBoolean(v);
                } else if ("carMechanicsOverlay".equals(k)) {
                    this.carMechanicsOverlay = v.trim();
                } else if ("carModelName".equals(k)) {
                    this.carModelName = v.trim();
                } else if ("specialLootChance".equals(k)) {
                    this.specialLootChance = Integer.parseInt(v);
                } else if ("specialKeyRingChance".equals(k)) {
                    this.specialKeyRingChance = Integer.parseInt(v);
                } else if ("template".equals(k)) {
                    this.LoadTemplate(v);
                } else if ("template!".equals(k)) {
                    VehicleTemplate template = ScriptManager.instance.getVehicleTemplate(v);
                    if (template == null) {
                        DebugLog.log("ERROR: template \"" + v + "\" not found in: " + this.getFileName());
                    } else {
                        this.Load(name, template.body);
                    }
                } else if ("engineRepairLevel".equals(k)) {
                    this.engineRepairLevel = Integer.parseInt(v);
                } else if ("playerDamageProtection".equals(k)) {
                    this.setPlayerDamageProtection(Float.parseFloat(v));
                } else if ("useChassisPhysicsCollision".equals(k)) {
                    this.useChassisPhysicsCollision = Boolean.parseBoolean(v);
                }
            } else {
                ScriptParser.Block child = element.asBlock();
                if ("area".equals(child.type)) {
                    this.LoadArea(child);
                } else if ("attachment".equals(child.type)) {
                    this.LoadAttachment(child);
                } else if ("model".equals(child.type)) {
                    this.LoadModel(child, this.models);
                } else if ("part".equals(child.type)) {
                    if (child.id != null && child.id.contains("*")) {
                        String pattern = child.id;

                        for (VehicleScript.Part part : this.parts) {
                            if (this.globMatch(pattern, part.id)) {
                                child.id = part.id;
                                this.LoadPart(child);
                            }
                        }
                    } else {
                        this.LoadPart(child);
                    }
                } else if ("passenger".equals(child.type)) {
                    if (child.id != null && child.id.contains("*")) {
                        String pattern = child.id;

                        for (VehicleScript.Passenger pngr : this.passengers) {
                            if (this.globMatch(pattern, pngr.id)) {
                                child.id = pngr.id;
                                this.LoadPassenger(child);
                            }
                        }
                    } else {
                        this.LoadPassenger(child);
                    }
                } else if ("physics".equals(child.type)) {
                    VehicleScript.PhysicsShape physicsShape = this.LoadPhysicsShape(child);
                    if (physicsShape != null && this.physicsShapes.size() < 10) {
                        this.physicsShapes.add(physicsShape);
                    }
                } else if ("skin".equals(child.type)) {
                    VehicleScript.Skin skin = this.LoadSkin(child);
                    if (!StringUtils.isNullOrWhitespace(skin.texture)) {
                        this.skins.add(skin);
                    }
                } else if ("wheel".equals(child.type)) {
                    this.LoadWheel(child);
                } else if ("lightbar".equals(child.type)) {
                    for (ScriptParser.Value value : child.values) {
                        String k = value.getKey().trim();
                        String v = value.getValue().trim();
                        if ("soundSiren".equals(k)) {
                            this.lightbar.soundSiren0 = v + "Yelp";
                            this.lightbar.soundSiren1 = v + "Wall";
                            this.lightbar.soundSiren2 = v + "Alarm";
                        }

                        if ("soundSiren0".equals(k)) {
                            this.lightbar.soundSiren0 = v;
                        }

                        if ("soundSiren1".equals(k)) {
                            this.lightbar.soundSiren1 = v;
                        }

                        if ("soundSiren2".equals(k)) {
                            this.lightbar.soundSiren2 = v;
                        }

                        if ("leftCol".equals(k)) {
                            String[] split = v.split(";");
                            this.leftSirenCol = new ImmutableColor(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]));
                        }

                        if ("rightCol".equals(k)) {
                            String[] split = v.split(";");
                            this.rightSirenCol = new ImmutableColor(Float.parseFloat(split[0]), Float.parseFloat(split[1]), Float.parseFloat(split[2]));
                        }

                        this.lightbar.enable = true;
                        if (this.getPartById("lightbar") == null) {
                            VehicleScript.Part partx = new VehicleScript.Part();
                            partx.id = "lightbar";
                            this.parts.add(partx);
                        }
                    }
                } else if ("sound".equals(child.type)) {
                    for (ScriptParser.Value value : child.values) {
                        String kx = value.getKey().trim();
                        String vx = value.getValue().trim();
                        if ("alarm".equals(kx)) {
                            String[] ss = vx.split("\\s+");
                            if (ss.length > 0) {
                                this.sound.alarm.addAll(Arrays.asList(ss));
                                this.sound.alarmEnable = true;
                            }
                        } else if ("alarmLoop".equals(kx)) {
                            String[] ss = vx.split("\\s+");
                            if (ss.length > 0) {
                                this.sound.alarmLoop.addAll(Arrays.asList(ss));
                                this.sound.alarmEnable = true;
                            }
                        } else if ("backSignal".equals(kx)) {
                            this.sound.backSignal = StringUtils.discardNullOrWhitespace(vx);
                            this.sound.backSignalEnable = this.sound.backSignal != null;
                        } else if ("engine".equals(kx)) {
                            this.sound.engine = StringUtils.discardNullOrWhitespace(vx);
                        } else if ("engineStart".equals(kx)) {
                            this.sound.engineStart = StringUtils.discardNullOrWhitespace(vx);
                        } else if ("engineTurnOff".equals(kx)) {
                            this.sound.engineTurnOff = StringUtils.discardNullOrWhitespace(vx);
                        } else if ("handBrake".equals(kx)) {
                            this.sound.handBrake = StringUtils.discardNullOrWhitespace(vx);
                        } else if ("horn".equals(kx)) {
                            this.sound.horn = StringUtils.discardNullOrWhitespace(vx);
                            this.sound.hornEnable = this.sound.horn != null;
                        } else if ("ignitionFail".equals(kx)) {
                            this.sound.ignitionFail = StringUtils.discardNullOrWhitespace(vx);
                        } else if ("ignitionFailNoPower".equals(kx)) {
                            this.sound.ignitionFailNoPower = StringUtils.discardNullOrWhitespace(vx);
                        }

                        this.sound.specified.add(kx);
                    }
                }
            }
        }
    }

    public String getFileName() {
        return this.fileName;
    }

    public void Loaded() {
        float S = this.getModelScale();
        this.extents.mul(S);
        this.maxSuspensionTravelCm *= S;
        this.suspensionRestLength *= S;
        this.centerOfMassOffset.mul(S);
        this.physicsChassisShape.mul(S);
        if (this.hadShadowOExtents) {
            this.shadowExtents.mul(S);
        } else {
            this.shadowExtents.set(this.extents.x(), this.extents.z());
        }

        if (this.hadShadowOffset) {
            this.shadowOffset.mul(S);
        } else {
            this.shadowOffset.set(this.centerOfMassOffset.x(), this.centerOfMassOffset.z());
        }

        for (VehicleScript.Model model : this.models) {
            model.offset.mul(S);
        }

        for (ModelAttachment attachment : this.attachments) {
            attachment.getOffset().mul(S);
        }

        for (VehicleScript.PhysicsShape shape : this.physicsShapes) {
            shape.offset.mul(S);
            switch (shape.type) {
                case 1:
                    shape.extents.mul(S);
                    break;
                case 2:
                    shape.radius *= S;
                    break;
                case 3:
                    shape.extents.mul(S);
            }
        }

        for (VehicleScript.Wheel wheel : this.wheels) {
            wheel.radius *= S;
            wheel.offset.mul(S);
        }

        for (VehicleScript.Area area : this.areas) {
            area.x *= S;
            area.y *= S;
            area.w *= S;
            area.h *= S;
        }

        if (this.hasPhysicsChassisShape() && !this.extents.equals(this.physicsChassisShape)) {
            DebugLog.Script.warn("vehicle \"" + this.name + "\" extents != physicsChassisShape");
        }

        for (int i = 0; i < this.passengers.size(); i++) {
            VehicleScript.Passenger pngr = this.passengers.get(i);

            for (int j = 0; j < pngr.getPositionCount(); j++) {
                VehicleScript.Position posn = pngr.getPosition(j);
                posn.getOffset().mul(S);
            }

            for (int j = 0; j < pngr.switchSeats.size(); j++) {
                VehicleScript.Passenger.SwitchSeat switchSeat = pngr.switchSeats.get(j);
                switchSeat.seat = this.getPassengerIndex(switchSeat.id);

                assert switchSeat.seat != -1;
            }
        }

        for (int i = 0; i < this.parts.size(); i++) {
            VehicleScript.Part part = this.parts.get(i);
            if (part.container != null && part.container.seatId != null && !part.container.seatId.isEmpty()) {
                part.container.seat = this.getPassengerIndex(part.container.seatId);
            }

            if (part.specificItem && part.itemType != null) {
                for (int jx = 0; jx < part.itemType.size(); jx++) {
                    part.itemType.set(jx, part.itemType.get(jx) + this.mechanicType);
                }
            }
        }

        if (!this.sound.alarmEnable && this.sound.hornEnable) {
            this.sound.alarmEnable = true;
            this.sound.alarm.add(this.sound.horn);
        }

        this.initCrawlOffsets();
        if (this.specialKeyRing != null) {
            for (int i = 0; i < this.specialKeyRing.size(); i++) {
                String itemType = this.specialKeyRing.get(i);
                itemType = ScriptManager.instance.resolveItemType(this.getModule(), itemType);
                this.specialKeyRing.set(i, itemType);
            }
        }

        this.compact();
        if (!GameServer.server) {
            this.toBullet();
        }
    }

    private void compact() {
        this.areas.trimToSize();
        this.crawlOffsets.trimToSize();
        this.attachments.trimToSize();
        this.physicsShapes.trimToSize();
        this.models.trimToSize();
        this.parts.trimToSize();

        for (int i = 0; i < this.parts.size(); i++) {
            this.parts.get(i).compact();
        }

        this.passengers.trimToSize();
        this.skins.trimToSize();
        if (this.specialKeyRing != null) {
            this.specialKeyRing.trimToSize();
        }

        this.wheels.trimToSize();
    }

    public void toBullet() {
        float[] params = new float[200];
        int n = 0;
        params[n++] = this.getModelScale();
        params[n++] = this.mass;
        params[n++] = this.rollInfluence;
        params[n++] = this.suspensionStiffness;
        params[n++] = this.suspensionCompression;
        params[n++] = this.suspensionDamping;
        params[n++] = this.maxSuspensionTravelCm;
        params[n++] = this.suspensionRestLength;
        if (SystemDisabler.getdoHighFriction()) {
            params[n++] = this.wheelFriction * 100.0F;
        } else {
            params[n++] = this.wheelFriction;
        }

        params[n++] = this.stoppingMovementForce;
        params[n++] = this.getWheelCount();

        for (int i = 0; i < this.getWheelCount(); i++) {
            VehicleScript.Wheel wheel = this.getWheel(i);
            params[n++] = wheel.front ? 1.0F : 0.0F;
            params[n++] = wheel.offset.x + this.getModel().offset.x - 0.0F * this.centerOfMassOffset.x;
            params[n++] = wheel.offset.y + this.getModel().offset.y - 0.0F * this.centerOfMassOffset.y + 1.0F * this.suspensionRestLength;
            params[n++] = wheel.offset.z + this.getModel().offset.z - 0.0F * this.centerOfMassOffset.z;
            params[n++] = wheel.radius;
        }

        int numShapes = (this.hasPhysicsChassisShape() && this.useChassisPhysicsCollision ? 1 : 0) + this.physicsShapes.size();
        if (numShapes == 0) {
            numShapes = 1;
        }

        params[n++] = numShapes;
        if (this.hasPhysicsChassisShape() && this.useChassisPhysicsCollision) {
            params[n++] = 1.0F;
            params[n++] = this.centerOfMassOffset.x;
            params[n++] = this.centerOfMassOffset.y;
            params[n++] = this.centerOfMassOffset.z;
            params[n++] = this.physicsChassisShape.x;
            params[n++] = this.physicsChassisShape.y;
            params[n++] = this.physicsChassisShape.z;
            params[n++] = 0.0F;
            params[n++] = 0.0F;
            params[n++] = 0.0F;
        } else if (this.physicsShapes.isEmpty()) {
            params[n++] = 1.0F;
            params[n++] = this.centerOfMassOffset.x;
            params[n++] = this.centerOfMassOffset.y;
            params[n++] = this.centerOfMassOffset.z;
            params[n++] = this.extents.x;
            params[n++] = this.extents.y;
            params[n++] = this.extents.z;
            params[n++] = 0.0F;
            params[n++] = 0.0F;
            params[n++] = 0.0F;
        }

        for (int i = 0; i < this.physicsShapes.size(); i++) {
            VehicleScript.PhysicsShape shape = this.physicsShapes.get(i);
            params[n++] = shape.type;
            params[n++] = shape.offset.x;
            params[n++] = shape.offset.y;
            params[n++] = shape.offset.z;
            if (shape.type == 1) {
                params[n++] = shape.extents.x;
                params[n++] = shape.extents.y;
                params[n++] = shape.extents.z;
                params[n++] = shape.rotate.x;
                params[n++] = shape.rotate.y;
                params[n++] = shape.rotate.z;
            } else if (shape.type == 2) {
                params[n++] = shape.radius;
            } else if (shape.type == 3) {
            }
        }

        Bullet.defineVehicleScript(this.getFullName(), params);
    }

    private void LoadVector2f(String s, Vector2f v) {
        String[] ss = s.split(" ");
        v.set(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]));
    }

    private void LoadVector3f(String s, Vector3f v) {
        String[] ss = s.split(" ");
        v.set(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]), Float.parseFloat(ss[2]));
    }

    private void LoadVector4f(String s, Vector4f v) {
        String[] ss = s.split(" ");
        v.set(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]), Float.parseFloat(ss[2]), Float.parseFloat(ss[3]));
    }

    private void LoadVector2i(String s, Vector2i v) {
        String[] ss = s.split(" ");
        v.set(Integer.parseInt(ss[0]), Integer.parseInt(ss[1]));
    }

    private ModelAttachment LoadAttachment(ScriptParser.Block block) {
        ModelAttachment attachment = this.getAttachmentById(block.id);
        if (attachment == null) {
            attachment = new ModelAttachment(block.id);
            attachment.setOwner(this);
            this.attachments.add(attachment);
        }

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("bone".equals(k)) {
                attachment.setBone(v);
            } else if ("offset".equals(k)) {
                this.LoadVector3f(v, attachment.getOffset());
            } else if ("rotate".equals(k)) {
                this.LoadVector3f(v, attachment.getRotate());
            } else if ("canAttach".equals(k)) {
                attachment.setCanAttach(new ArrayList<>(Arrays.asList(v.split(";"))));
            } else if ("zoffset".equals(k)) {
                attachment.setZOffset(Float.parseFloat(v));
            } else if ("updateconstraint".equals(k)) {
                attachment.setUpdateConstraint(Boolean.parseBoolean(v));
            }
        }

        return attachment;
    }

    private VehicleScript.Model LoadModel(ScriptParser.Block block, ArrayList<VehicleScript.Model> models) {
        VehicleScript.Model model = this.getModelById(block.id, models);
        if (model == null) {
            model = new VehicleScript.Model();
            model.id = block.id;
            models.add(model);
        }

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("file".equals(k)) {
                model.file = v;
            } else if ("offset".equals(k)) {
                this.LoadVector3f(v, model.offset);
            } else if ("rotate".equals(k)) {
                this.LoadVector3f(v, model.rotate);
            } else if ("scale".equals(k)) {
                model.scale = Float.parseFloat(v);
            } else if ("attachmentParent".equals(k)) {
                model.attachmentNameParent = v.isEmpty() ? null : v;
            } else if ("attachmentSelf".equals(k)) {
                model.attachmentNameSelf = v.isEmpty() ? null : v;
            } else if ("ignoreVehicleScale".equalsIgnoreCase(k)) {
                model.ignoreVehicleScale = Boolean.parseBoolean(v);
            }
        }

        return model;
    }

    private VehicleScript.Skin LoadSkin(ScriptParser.Block block) {
        VehicleScript.Skin skin = new VehicleScript.Skin();

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("texture".equals(k)) {
                skin.texture = StringUtils.discardNullOrWhitespace(v);
            } else if ("textureRust".equals(k)) {
                skin.textureRust = StringUtils.discardNullOrWhitespace(v);
            } else if ("textureMask".equals(k)) {
                skin.textureMask = StringUtils.discardNullOrWhitespace(v);
            } else if ("textureLights".equals(k)) {
                skin.textureLights = StringUtils.discardNullOrWhitespace(v);
            } else if ("textureDamage1Overlay".equals(k)) {
                skin.textureDamage1Overlay = StringUtils.discardNullOrWhitespace(v);
            } else if ("textureDamage1Shell".equals(k)) {
                skin.textureDamage1Shell = StringUtils.discardNullOrWhitespace(v);
            } else if ("textureDamage2Overlay".equals(k)) {
                skin.textureDamage2Overlay = StringUtils.discardNullOrWhitespace(v);
            } else if ("textureDamage2Shell".equals(k)) {
                skin.textureDamage2Shell = StringUtils.discardNullOrWhitespace(v);
            } else if ("textureShadow".equals(k)) {
                skin.textureShadow = StringUtils.discardNullOrWhitespace(v);
            }
        }

        return skin;
    }

    private VehicleScript.Wheel LoadWheel(ScriptParser.Block block) {
        VehicleScript.Wheel wheel = this.getWheelById(block.id);
        if (wheel == null) {
            wheel = new VehicleScript.Wheel();
            wheel.id = block.id;
            this.wheels.add(wheel);
        }

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("model".equals(k)) {
                wheel.model = v;
            } else if ("front".equals(k)) {
                wheel.front = Boolean.parseBoolean(v);
            } else if ("offset".equals(k)) {
                this.LoadVector3f(v, wheel.offset);
            } else if ("radius".equals(k)) {
                wheel.radius = Float.parseFloat(v);
            } else if ("width".equals(k)) {
                wheel.width = Float.parseFloat(v);
            }
        }

        return wheel;
    }

    private VehicleScript.Passenger LoadPassenger(ScriptParser.Block block) {
        VehicleScript.Passenger pngr = this.getPassengerById(block.id);
        if (pngr == null) {
            pngr = new VehicleScript.Passenger();
            pngr.id = block.id;
            this.passengers.add(pngr);
        }

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("area".equals(k)) {
                pngr.area = v;
            } else if ("door".equals(k)) {
                pngr.door = v;
            } else if ("door2".equals(k)) {
                pngr.door2 = v;
            } else if ("hasRoof".equals(k)) {
                pngr.hasRoof = Boolean.parseBoolean(v);
            } else if ("showPassenger".equals(k)) {
                pngr.showPassenger = Boolean.parseBoolean(v);
            }
        }

        for (ScriptParser.Block child : block.children) {
            if ("anim".equals(child.type)) {
                this.LoadAnim(child, pngr.anims);
            } else if ("position".equals(child.type)) {
                this.LoadPosition(child, pngr.positions);
            } else if ("switchSeat".equals(child.type)) {
                this.LoadPassengerSwitchSeat(child, pngr);
            }
        }

        return pngr;
    }

    private VehicleScript.Anim LoadAnim(ScriptParser.Block block, ArrayList<VehicleScript.Anim> anims) {
        VehicleScript.Anim anim = this.getAnimationById(block.id, anims);
        if (anim == null) {
            anim = new VehicleScript.Anim();
            anim.id = block.id.intern();
            anims.add(anim);
        }

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("angle".equals(k)) {
                this.LoadVector3f(v, anim.angle);
            } else if ("anim".equals(k)) {
                anim.anim = v;
            } else if ("animate".equals(k)) {
                anim.animate = Boolean.parseBoolean(v);
            } else if ("loop".equals(k)) {
                anim.loop = Boolean.parseBoolean(v);
            } else if ("reverse".equals(k)) {
                anim.reverse = Boolean.parseBoolean(v);
            } else if ("rate".equals(k)) {
                anim.rate = Float.parseFloat(v);
            } else if ("offset".equals(k)) {
                this.LoadVector3f(v, anim.offset);
            } else if ("sound".equals(k)) {
                anim.sound = v;
            }
        }

        return anim;
    }

    private VehicleScript.Passenger.SwitchSeat LoadPassengerSwitchSeat(ScriptParser.Block block, VehicleScript.Passenger passenger) {
        VehicleScript.Passenger.SwitchSeat switchSeat = passenger.getSwitchSeatById(block.id);
        if (block.isEmpty()) {
            if (switchSeat != null) {
                passenger.switchSeats.remove(switchSeat);
            }

            return null;
        } else {
            if (switchSeat == null) {
                switchSeat = new VehicleScript.Passenger.SwitchSeat();
                switchSeat.id = block.id;
                passenger.switchSeats.add(switchSeat);
            }

            for (ScriptParser.Value value : block.values) {
                String k = value.getKey().trim();
                String v = value.getValue().trim();
                if ("anim".equals(k)) {
                    switchSeat.anim = v;
                } else if ("rate".equals(k)) {
                    switchSeat.rate = Float.parseFloat(v);
                } else if ("sound".equals(k)) {
                    switchSeat.sound = v.isEmpty() ? null : v;
                }
            }

            return switchSeat;
        }
    }

    private VehicleScript.Area LoadArea(ScriptParser.Block block) {
        VehicleScript.Area area = this.getAreaById(block.id);
        if (area == null) {
            area = new VehicleScript.Area();
            area.id = block.id.intern();
            this.areas.add(area);
        }

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("xywh".equals(k)) {
                String[] ss2 = v.split(" ");
                area.x = Float.parseFloat(ss2[0]);
                area.y = Float.parseFloat(ss2[1]);
                area.w = Float.parseFloat(ss2[2]);
                area.h = Float.parseFloat(ss2[3]);
            }
        }

        return area;
    }

    private VehicleScript.Part LoadPart(ScriptParser.Block block) {
        VehicleScript.Part part = this.getPartById(block.id);
        if (part == null) {
            part = new VehicleScript.Part();
            part.id = block.id;
            this.parts.add(part);
        }

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim().intern();
            String v = value.getValue().trim().intern();
            if ("area".equals(k)) {
                part.area = v.isEmpty() ? null : v;
            }

            if ("mechanicArea".equals(k)) {
                part.mechanicArea = v;
            } else if ("itemType".equals(k)) {
                part.itemType = new ArrayList<>();
                String[] split = v.split(";");

                for (String itype : split) {
                    part.itemType.add(itype.intern());
                }
            } else if ("parent".equals(k)) {
                part.parent = v.isEmpty() ? null : v;
            } else if ("mechanicRequireKey".equals(k)) {
                part.mechanicRequireKey = Boolean.parseBoolean(v);
            } else if ("repairMechanic".equals(k)) {
                part.setRepairMechanic(Boolean.parseBoolean(v));
            } else if ("setAllModelsVisible".equals(k)) {
                part.setAllModelsVisible = Boolean.parseBoolean(v);
            } else if ("wheel".equals(k)) {
                part.wheel = v;
            } else if ("category".equals(k)) {
                part.category = v;
            } else if ("durability".equals(k)) {
                part.durability = Float.parseFloat(v);
            } else if ("specificItem".equals(k)) {
                part.specificItem = Boolean.parseBoolean(v);
            } else if ("hasLightsRear".equals(k)) {
                part.hasLightsRear = Boolean.parseBoolean(v);
            }
        }

        for (ScriptParser.Block child : block.children) {
            if ("anim".equals(child.type)) {
                if (part.anims == null) {
                    part.anims = new ArrayList<>();
                }

                this.LoadAnim(child, part.anims);
            } else if ("container".equals(child.type)) {
                part.container = this.LoadContainer(child, part.container);
            } else if ("door".equals(child.type)) {
                part.door = this.LoadDoor(child);
            } else if ("lua".equals(child.type)) {
                part.luaFunctions = this.LoadLuaFunctions(child);
            } else if ("model".equals(child.type)) {
                if (part.models == null) {
                    part.models = new ArrayList<>();
                }

                this.LoadModel(child, part.models);
            } else if ("table".equals(child.type)) {
                KahluaTable table = this.LoadTable(
                    child, (part.tables == null ? null : part.tables.get(child.id)) instanceof KahluaTable kahluaTable ? kahluaTable : null
                );
                if (part.tables == null) {
                    part.tables = new THashMap<>();
                }

                part.tables.put(child.id, table);
            } else if ("window".equals(child.type)) {
                part.window = this.LoadWindow(child);
            }
        }

        return part;
    }

    private VehicleScript.PhysicsShape LoadPhysicsShape(ScriptParser.Block block) {
        int type = -1;
        String shape = block.id;
        byte var9;
        switch (shape) {
            case "box":
                var9 = 1;
                break;
            case "sphere":
                var9 = 2;
                break;
            case "mesh":
                var9 = 3;
                break;
            default:
                return null;
        }

        VehicleScript.PhysicsShape shape = new VehicleScript.PhysicsShape();
        shape.type = var9;

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("extents".equalsIgnoreCase(k)) {
                this.LoadVector3f(v, shape.extents);
            } else if ("offset".equalsIgnoreCase(k)) {
                this.LoadVector3f(v, shape.offset);
            } else if ("radius".equalsIgnoreCase(k)) {
                shape.radius = Float.parseFloat(v);
            } else if ("rotate".equalsIgnoreCase(k)) {
                this.LoadVector3f(v, shape.rotate);
            } else if ("physicsShapeScript".equalsIgnoreCase(k)) {
                shape.physicsShapeScript = StringUtils.discardNullOrWhitespace(v);
            } else if ("scale".equalsIgnoreCase(k)) {
                float scale = Float.parseFloat(v);
                shape.extents.set(scale);
            }
        }

        switch (shape.type) {
            case 1:
                if (shape.extents.x() <= 0.0F || shape.extents.y() <= 0.0F || shape.extents.z() <= 0.0F) {
                    return null;
                }
                break;
            case 2:
                if (shape.radius <= 0.0F) {
                    return null;
                }
                break;
            case 3:
                if (shape.physicsShapeScript == null) {
                    return null;
                }

                if (shape.extents.x() <= 0.0F) {
                    shape.extents.set(1.0F);
                }
        }

        return shape;
    }

    private VehicleScript.Door LoadDoor(ScriptParser.Block block) {
        VehicleScript.Door door = new VehicleScript.Door();

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String var6 = value.getValue().trim();
        }

        return door;
    }

    private VehicleScript.Window LoadWindow(ScriptParser.Block block) {
        VehicleScript.Window window = new VehicleScript.Window();

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("openable".equals(k)) {
                window.openable = Boolean.parseBoolean(v);
            }
        }

        return window;
    }

    private VehicleScript.Container LoadContainer(ScriptParser.Block block, VehicleScript.Container existing) {
        VehicleScript.Container container = existing == null ? new VehicleScript.Container() : existing;

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim();
            if ("capacity".equals(k)) {
                container.capacity = Integer.parseInt(v);
            } else if ("conditionAffectsCapacity".equals(k)) {
                container.conditionAffectsCapacity = Boolean.parseBoolean(v);
            } else if ("contentType".equals(k)) {
                container.contentType = v;
            } else if ("seat".equals(k)) {
                container.seatId = v;
            } else if ("test".equals(k)) {
                container.luaTest = v;
            }
        }

        return container;
    }

    private THashMap<String, String> LoadLuaFunctions(ScriptParser.Block block) {
        THashMap<String, String> map = new THashMap<>();

        for (ScriptParser.Value value : block.values) {
            if (value.string.indexOf(61) == -1) {
                throw new RuntimeException("expected \"key = value\", got \"" + value.string.trim() + "\" in " + this.getFullName());
            }

            String k = value.getKey().trim();
            String v = value.getValue().trim();
            map.put(k, v);
        }

        return map;
    }

    private Object checkIntegerKey(Object key) {
        if (key instanceof String str) {
            for (int i = 0; i < str.length(); i++) {
                if (!Character.isDigit(str.charAt(i))) {
                    return str.intern();
                }
            }

            return Double.valueOf(str);
        } else {
            return key;
        }
    }

    private KahluaTable LoadTable(ScriptParser.Block block, KahluaTable existing) {
        KahluaTable table = existing == null ? LuaManager.platform.newTable() : existing;

        for (ScriptParser.Value value : block.values) {
            String k = value.getKey().trim();
            String v = value.getValue().trim().intern();
            if (v.isEmpty()) {
                v = null;
            }

            table.rawset(this.checkIntegerKey(k), v);
        }

        for (ScriptParser.Block child : block.children) {
            KahluaTable table2 = this.LoadTable(child, table.rawget(child.type) instanceof KahluaTable kahluaTable ? kahluaTable : null);
            table.rawset(this.checkIntegerKey(child.type), table2);
        }

        return table;
    }

    private void LoadTemplate(String str) {
        if (str.contains("/")) {
            String[] ss = str.split("/");
            if (ss.length == 0 || ss.length > 3) {
                DebugLog.log("ERROR: template \"" + str + "\"");
                return;
            }

            for (int i = 0; i < ss.length; i++) {
                ss[i] = ss[i].trim();
                if (ss[i].isEmpty()) {
                    DebugLog.log("ERROR: template \"" + str + "\"");
                    return;
                }
            }

            String templateName = ss[0];
            VehicleTemplate template = ScriptManager.instance.getVehicleTemplate(templateName);
            if (template == null) {
                DebugLog.log("ERROR: template \"" + str + "\" not found A");
                return;
            }

            VehicleScript script = template.getScript();
            String var6 = ss[1];
            switch (var6) {
                case "area":
                    if (ss.length == 2) {
                        DebugLog.log("ERROR: template \"" + str + "\"");
                        return;
                    }

                    this.copyAreasFrom(script, ss[2]);
                    break;
                case "part":
                    if (ss.length == 2) {
                        DebugLog.log("ERROR: template \"" + str + "\"");
                        return;
                    }

                    this.copyPartsFrom(script, ss[2]);
                    break;
                case "passenger":
                    if (ss.length == 2) {
                        DebugLog.log("ERROR: template \"" + str + "\"");
                        return;
                    }

                    this.copyPassengersFrom(script, ss[2]);
                    break;
                case "wheel":
                    if (ss.length == 2) {
                        DebugLog.log("ERROR: template \"" + str + "\"");
                        return;
                    }

                    this.copyWheelsFrom(script, ss[2]);
                    break;
                case "physics":
                    if (ss.length == 2) {
                        DebugLog.log("ERROR: template \"" + str + "\"");
                        return;
                    }

                    this.copyPhysicsFrom(script, ss[2]);
                    break;
                default:
                    DebugLog.log("ERROR: template \"" + str + "\"");
                    return;
            }
        } else {
            String templateName = str.trim();
            VehicleTemplate template = ScriptManager.instance.getVehicleTemplate(templateName);
            if (template == null) {
                DebugLog.log("ERROR: template \"" + str + "\" not found B");
                return;
            }

            VehicleScript script = template.getScript();
            this.copyAreasFrom(script, "*");
            this.copyPartsFrom(script, "*");
            this.copyPassengersFrom(script, "*");
            this.copySoundFrom(script, "*");
            this.copyWheelsFrom(script, "*");
            this.copyPhysicsFrom(script, "*");
        }
    }

    public void copyAreasFrom(VehicleScript other, String spec) {
        if ("*".equals(spec)) {
            for (int i = 0; i < other.getAreaCount(); i++) {
                VehicleScript.Area otherArea = other.getArea(i);
                int index = this.getIndexOfAreaById(otherArea.id);
                if (index == -1) {
                    this.areas.add(otherArea.makeCopy());
                } else {
                    this.areas.set(index, otherArea.makeCopy());
                }
            }
        } else {
            VehicleScript.Area otherArea = other.getAreaById(spec);
            if (otherArea == null) {
                DebugLog.log("ERROR: area \"" + spec + "\" not found");
                return;
            }

            int index = this.getIndexOfAreaById(otherArea.id);
            if (index == -1) {
                this.areas.add(otherArea.makeCopy());
            } else {
                this.areas.set(index, otherArea.makeCopy());
            }
        }
    }

    public void copyPartsFrom(VehicleScript other, String spec) {
        if ("*".equals(spec)) {
            for (int i = 0; i < other.getPartCount(); i++) {
                VehicleScript.Part otherPart = other.getPart(i);
                int index = this.getIndexOfPartById(otherPart.id);
                if (index == -1) {
                    this.parts.add(otherPart.makeCopy());
                } else {
                    this.parts.set(index, otherPart.makeCopy());
                }
            }
        } else {
            VehicleScript.Part otherPart = other.getPartById(spec);
            if (otherPart == null) {
                DebugLog.log("ERROR: part \"" + spec + "\" not found");
                return;
            }

            int index = this.getIndexOfPartById(otherPart.id);
            if (index == -1) {
                this.parts.add(otherPart.makeCopy());
            } else {
                this.parts.set(index, otherPart.makeCopy());
            }
        }
    }

    public void copyPhysicsFrom(VehicleScript other, String spec) {
        this.physicsShapes.clear();

        for (int i = 0; i < other.getPhysicsShapeCount(); i++) {
            this.physicsShapes.add(i, other.getPhysicsShape(i).makeCopy());
        }

        this.useChassisPhysicsCollision = other.useChassisPhysicsCollision();
    }

    public void copyPassengersFrom(VehicleScript other, String spec) {
        if ("*".equals(spec)) {
            for (int i = 0; i < other.getPassengerCount(); i++) {
                VehicleScript.Passenger otherPngr = other.getPassenger(i);
                int index = this.getPassengerIndex(otherPngr.id);
                if (index == -1) {
                    this.passengers.add(otherPngr.makeCopy());
                } else {
                    this.passengers.set(index, otherPngr.makeCopy());
                }
            }
        } else {
            VehicleScript.Passenger otherPngr = other.getPassengerById(spec);
            if (otherPngr == null) {
                DebugLog.log("ERROR: passenger \"" + spec + "\" not found");
                return;
            }

            int index = this.getPassengerIndex(otherPngr.id);
            if (index == -1) {
                this.passengers.add(otherPngr.makeCopy());
            } else {
                this.passengers.set(index, otherPngr.makeCopy());
            }
        }
    }

    public void copySoundFrom(VehicleScript other, String spec) {
        if ("*".equals(spec)) {
            for (String s : other.sound.specified) {
                switch (s) {
                    case "backSignal":
                        this.sound.backSignal = other.sound.backSignal;
                        this.sound.backSignalEnable = other.sound.backSignalEnable;
                        break;
                    case "engine":
                        this.sound.engine = other.sound.engine;
                        break;
                    case "engineStart":
                        this.sound.engineStart = other.sound.engineStart;
                        break;
                    case "engineTurnOff":
                        this.sound.engineTurnOff = other.sound.engineTurnOff;
                        break;
                    case "handBrake":
                        this.sound.handBrake = other.sound.handBrake;
                        break;
                    case "ignitionFail":
                        this.sound.ignitionFail = other.sound.ignitionFail;
                        break;
                    case "ignitionFailNoPower":
                        this.sound.ignitionFailNoPower = other.sound.ignitionFailNoPower;
                }

                this.sound.specified.add(s);
            }
        }
    }

    public void copyWheelsFrom(VehicleScript other, String spec) {
        if ("*".equals(spec)) {
            for (int i = 0; i < other.getWheelCount(); i++) {
                VehicleScript.Wheel otherWheel = other.getWheel(i);
                int index = this.getIndexOfWheelById(otherWheel.id);
                if (index == -1) {
                    this.wheels.add(otherWheel.makeCopy());
                } else {
                    this.wheels.set(index, otherWheel.makeCopy());
                }
            }
        } else {
            VehicleScript.Wheel otherWheel = other.getWheelById(spec);
            if (otherWheel == null) {
                DebugLog.log("ERROR: wheel \"" + spec + "\" not found");
                return;
            }

            int index = this.getIndexOfWheelById(otherWheel.id);
            if (index == -1) {
                this.wheels.add(otherWheel.makeCopy());
            } else {
                this.wheels.set(index, otherWheel.makeCopy());
            }
        }
    }

    private VehicleScript.Position LoadPosition(ScriptParser.Block block, ArrayList<VehicleScript.Position> positions) {
        VehicleScript.Position position = this.getPositionById(block.id, positions);
        if (block.isEmpty()) {
            if (position != null) {
                positions.remove(position);
            }

            return null;
        } else {
            if (position == null) {
                position = new VehicleScript.Position();
                position.id = block.id.intern();
                positions.add(position);
            }

            for (ScriptParser.Value value : block.values) {
                String k = value.getKey().trim();
                String v = value.getValue().trim();
                if ("rotate".equals(k)) {
                    this.LoadVector3f(v, position.rotate);
                } else if ("offset".equals(k)) {
                    this.LoadVector3f(v, position.offset);
                } else if ("area".equals(k)) {
                    position.area = v.isEmpty() ? null : v;
                }
            }

            return position;
        }
    }

    private void initCrawlOffsets() {
        for (int i = 0; i < this.getWheelCount(); i++) {
            VehicleScript.Wheel wheel = this.getWheel(i);
            if (wheel.id.contains("Left")) {
                this.initCrawlOffsets(wheel);
            }
        }

        float polyLength = this.extents.z + 0.3F;

        for (int ix = 0; ix < this.crawlOffsets.size(); ix++) {
            this.crawlOffsets.set(ix, (this.extents.z / 2.0F + 0.15F + this.crawlOffsets.get(ix) - this.centerOfMassOffset.z) / polyLength);
        }

        this.crawlOffsets.sort();

        for (int ix = 0; ix < this.crawlOffsets.size(); ix++) {
            float o1 = this.crawlOffsets.get(ix);

            for (int j = ix + 1; j < this.crawlOffsets.size(); j++) {
                float o2 = this.crawlOffsets.get(j);
                if ((o2 - o1) * polyLength < 0.15F) {
                    this.crawlOffsets.removeAt(j--);
                }
            }
        }
    }

    private void initCrawlOffsets(VehicleScript.Wheel wheel) {
        float RADIUS = 0.3F;
        float modelOffsetZ = this.getModel() == null ? 0.0F : this.getModel().getOffset().z;
        float front = this.centerOfMassOffset.z + this.extents.z / 2.0F;
        float rear = this.centerOfMassOffset.z - this.extents.z / 2.0F;

        for (int i = 0; i < 10; i++) {
            float zOffset = modelOffsetZ + wheel.offset.z + wheel.radius + 0.3F + 0.3F * i;
            if (zOffset + 0.3F <= front && !this.isOverlappingWheel(zOffset)) {
                this.crawlOffsets.add(zOffset);
            }

            zOffset = modelOffsetZ + wheel.offset.z - wheel.radius - 0.3F - 0.3F * i;
            if (zOffset - 0.3F >= rear && !this.isOverlappingWheel(zOffset)) {
                this.crawlOffsets.add(zOffset);
            }
        }
    }

    private boolean isOverlappingWheel(float zOffset) {
        float RADIUS = 0.3F;
        float modelOffsetZ = this.getModel() == null ? 0.0F : this.getModel().getOffset().z;

        for (int i = 0; i < this.getWheelCount(); i++) {
            VehicleScript.Wheel wheel = this.getWheel(i);
            if (wheel.id.contains("Left") && Math.abs(modelOffsetZ + wheel.offset.z - zOffset) < (wheel.radius + 0.3F) * 0.99F) {
                return true;
            }
        }

        return false;
    }

    public String getName() {
        return this.name;
    }

    public String getFullName() {
        return this.getModule().getName() + "." + this.getName();
    }

    public String getFullType() {
        return this.getFullName();
    }

    public VehicleScript.Model getModel() {
        return this.models.isEmpty() ? null : this.models.get(0);
    }

    public Vector3f getModelOffset() {
        return this.getModel() == null ? null : this.getModel().getOffset();
    }

    public float getModelScale() {
        return this.getModel() == null ? 1.0F : this.getModel().scale;
    }

    public void setModelScale(float scale) {
        VehicleScript.Model model = this.getModel();
        if (model != null) {
            float oldScale = model.scale;
            model.scale = 1.0F / oldScale;
            this.Loaded();
            model.scale = PZMath.clamp(scale, 0.01F, 100.0F);
            this.Loaded();
        }
    }

    public int getModelCount() {
        return this.models.size();
    }

    public VehicleScript.Model getModelByIndex(int index) {
        return this.models.get(index);
    }

    public VehicleScript.Model getModelById(String id, ArrayList<VehicleScript.Model> models) {
        for (int i = 0; i < models.size(); i++) {
            VehicleScript.Model model = models.get(i);
            if (StringUtils.isNullOrWhitespace(model.id) && StringUtils.isNullOrWhitespace(id)) {
                return model;
            }

            if (model.id != null && model.id.equals(id)) {
                return model;
            }
        }

        return null;
    }

    public VehicleScript.Model getModelById(String id) {
        return this.getModelById(id, this.models);
    }

    public int getAttachmentCount() {
        return this.attachments.size();
    }

    public ModelAttachment getAttachment(int index) {
        return this.attachments.get(index);
    }

    public ModelAttachment getAttachmentById(String id) {
        for (int i = 0; i < this.attachments.size(); i++) {
            ModelAttachment attachment = this.attachments.get(i);
            if (attachment.getId().equals(id)) {
                return attachment;
            }
        }

        return null;
    }

    public ModelAttachment addAttachment(ModelAttachment attach) {
        attach.setOwner(this);
        this.attachments.add(attach);
        return attach;
    }

    public ModelAttachment removeAttachment(ModelAttachment attach) {
        attach.setOwner(null);
        this.attachments.remove(attach);
        return attach;
    }

    public ModelAttachment addAttachmentAt(int index, ModelAttachment attach) {
        attach.setOwner(this);
        this.attachments.add(index, attach);
        return attach;
    }

    public ModelAttachment removeAttachment(int index) {
        ModelAttachment attachment = this.attachments.remove(index);
        attachment.setOwner(null);
        return attachment;
    }

    @Override
    public void beforeRenameAttachment(ModelAttachment attachment) {
    }

    @Override
    public void afterRenameAttachment(ModelAttachment attachment) {
    }

    public VehicleScript.LightBar getLightbar() {
        return this.lightbar;
    }

    public VehicleScript.Sounds getSounds() {
        return this.sound;
    }

    public boolean getHasSiren() {
        return this.hasSiren;
    }

    public Vector3f getExtents() {
        return this.extents;
    }

    public Vector3f getPhysicsChassisShape() {
        return this.physicsChassisShape;
    }

    public boolean hasPhysicsChassisShape() {
        return this.physicsChassisShape.lengthSquared() > 0.0F;
    }

    public boolean useChassisPhysicsCollision() {
        return this.useChassisPhysicsCollision;
    }

    public Vector2f getShadowExtents() {
        return this.shadowExtents;
    }

    public Vector2f getShadowOffset() {
        return this.shadowOffset;
    }

    public Vector2f getExtentsOffset() {
        return this.extentsOffset;
    }

    public float getMass() {
        return this.mass;
    }

    public Vector3f getCenterOfMassOffset() {
        return this.centerOfMassOffset;
    }

    public float getEngineForce() {
        return this.engineForce;
    }

    public float getEngineIdleSpeed() {
        return this.engineIdleSpeed;
    }

    public int getEngineQuality() {
        return this.engineQuality;
    }

    public int getEngineLoudness() {
        return this.engineLoudness;
    }

    public float getRollInfluence() {
        return this.rollInfluence;
    }

    public float getSteeringIncrement() {
        return this.steeringIncrement;
    }

    public float getSteeringClamp(float speed) {
        speed = Math.abs(speed);
        float delta = speed / this.maxSpeed;
        if (delta > 1.0F) {
            delta = 1.0F;
        }

        delta = 1.0F - delta;
        return (0.9F - this.steeringClamp) * delta + this.steeringClamp;
    }

    public float getSuspensionStiffness() {
        return this.suspensionStiffness;
    }

    public float getSuspensionDamping() {
        return this.suspensionDamping;
    }

    public float getSuspensionCompression() {
        return this.suspensionCompression;
    }

    public float getSuspensionRestLength() {
        return this.suspensionRestLength;
    }

    public float getSuspensionTravel() {
        return this.maxSuspensionTravelCm;
    }

    public float getWheelFriction() {
        return this.wheelFriction;
    }

    public int getWheelCount() {
        return this.wheels.size();
    }

    public VehicleScript.Wheel getWheel(int index) {
        return this.wheels.get(index);
    }

    public VehicleScript.Wheel getWheelById(String id) {
        for (int i = 0; i < this.wheels.size(); i++) {
            VehicleScript.Wheel wheel = this.wheels.get(i);
            if (wheel.id != null && wheel.id.equals(id)) {
                return wheel;
            }
        }

        return null;
    }

    public int getIndexOfWheelById(String id) {
        for (int i = 0; i < this.wheels.size(); i++) {
            VehicleScript.Wheel wheel = this.wheels.get(i);
            if (wheel.id != null && wheel.id.equals(id)) {
                return i;
            }
        }

        return -1;
    }

    public int getPassengerCount() {
        return this.passengers.size();
    }

    public VehicleScript.Passenger getPassenger(int index) {
        return this.passengers.get(index);
    }

    public VehicleScript.Passenger getPassengerById(String id) {
        for (int i = 0; i < this.passengers.size(); i++) {
            VehicleScript.Passenger passenger = this.passengers.get(i);
            if (passenger.id != null && passenger.id.equals(id)) {
                return passenger;
            }
        }

        return null;
    }

    public int getPassengerIndex(String id) {
        for (int i = 0; i < this.passengers.size(); i++) {
            VehicleScript.Passenger passenger = this.passengers.get(i);
            if (passenger.id != null && passenger.id.equals(id)) {
                return i;
            }
        }

        return -1;
    }

    public int getPhysicsShapeCount() {
        return this.physicsShapes.size();
    }

    public VehicleScript.PhysicsShape getPhysicsShape(int index) {
        return index >= 0 && index < this.physicsShapes.size() ? this.physicsShapes.get(index) : null;
    }

    public VehicleScript.PhysicsShape addPhysicsShape(String type) {
        Objects.requireNonNull(type);
        VehicleScript.PhysicsShape shape = new VehicleScript.PhysicsShape();

        shape.type = switch (type) {
            case "box" -> 1;
            case "sphere" -> 2;
            case "mesh" -> 3;
            default -> throw new IllegalArgumentException("invalid vehicle physics shape \"%s\"".formatted(type));
        };
        switch (shape.type) {
            case 1:
                shape.extents.set(1.0F, 1.0F, 1.0F);
                break;
            case 2:
                shape.radius = 0.5F;
                break;
            case 3:
                shape.physicsShapeScript = "Base.XXX";
                shape.extents.set(1.0F);
        }

        this.physicsShapes.add(shape);
        return shape;
    }

    public VehicleScript.PhysicsShape removePhysicsShape(int index) {
        return this.physicsShapes.remove(index);
    }

    public int getFrontEndHealth() {
        return this.frontEndHealth;
    }

    public int getRearEndHealth() {
        return this.rearEndHealth;
    }

    public int getStorageCapacity() {
        return this.storageCapacity;
    }

    public VehicleScript.Skin getTextures() {
        return this.textures;
    }

    public int getSkinCount() {
        return this.skins.size();
    }

    public VehicleScript.Skin getSkin(int index) {
        return this.skins.get(index);
    }

    public int getAreaCount() {
        return this.areas.size();
    }

    public VehicleScript.Area getArea(int index) {
        return this.areas.get(index);
    }

    public VehicleScript.Area getAreaById(String id) {
        for (int i = 0; i < this.areas.size(); i++) {
            VehicleScript.Area area = this.areas.get(i);
            if (area.id != null && area.id.equals(id)) {
                return area;
            }
        }

        return null;
    }

    public int getIndexOfAreaById(String id) {
        for (int i = 0; i < this.areas.size(); i++) {
            VehicleScript.Area area = this.areas.get(i);
            if (area.id != null && area.id.equals(id)) {
                return i;
            }
        }

        return -1;
    }

    public int getPartCount() {
        return this.parts.size();
    }

    public VehicleScript.Part getPart(int index) {
        return this.parts.get(index);
    }

    public VehicleScript.Part getPartById(String id) {
        for (int i = 0; i < this.parts.size(); i++) {
            VehicleScript.Part part = this.parts.get(i);
            if (part.id != null && part.id.equals(id)) {
                return part;
            }
        }

        return null;
    }

    public int getIndexOfPartById(String id) {
        for (int i = 0; i < this.parts.size(); i++) {
            VehicleScript.Part part = this.parts.get(i);
            if (part.id != null && part.id.equals(id)) {
                return i;
            }
        }

        return -1;
    }

    private VehicleScript.Anim getAnimationById(String id, ArrayList<VehicleScript.Anim> anims) {
        for (int i = 0; i < anims.size(); i++) {
            VehicleScript.Anim anim = anims.get(i);
            if (anim.id != null && anim.id.equals(id)) {
                return anim;
            }
        }

        return null;
    }

    private VehicleScript.Position getPositionById(String id, ArrayList<VehicleScript.Position> positions) {
        for (int i = 0; i < positions.size(); i++) {
            VehicleScript.Position position = positions.get(i);
            if (position.id != null && position.id.equals(id)) {
                return position;
            }
        }

        return null;
    }

    public boolean globMatch(String pattern, String str) {
        Pattern pattern1 = Pattern.compile(pattern.replaceAll("\\*", ".*"));
        return pattern1.matcher(str).matches();
    }

    public int getGearRatioCount() {
        return this.gearRatioCount;
    }

    public int getSeats() {
        return this.seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }

    public int getMechanicType() {
        return this.mechanicType;
    }

    public void setMechanicType(int mechanicType) {
        this.mechanicType = mechanicType;
    }

    public int getEngineRepairLevel() {
        return this.engineRepairLevel;
    }

    public int getHeadlightConfigLevel() {
        return 2;
    }

    public void setEngineRepairLevel(int engineRepairLevel) {
        this.engineRepairLevel = engineRepairLevel;
    }

    public float getPlayerDamageProtection() {
        return this.playerDamageProtection;
    }

    public void setPlayerDamageProtection(float playerDamageProtection) {
        this.playerDamageProtection = playerDamageProtection;
    }

    public float getForcedHue() {
        return this.forcedHue;
    }

    public void setForcedHue(float forcedHue) {
        this.forcedHue = forcedHue;
    }

    public float getForcedSat() {
        return this.forcedSat;
    }

    public void setForcedSat(float forcedSat) {
        this.forcedSat = forcedSat;
    }

    public float getForcedVal() {
        return this.forcedVal;
    }

    public void setForcedVal(float forcedVal) {
        this.forcedVal = forcedVal;
    }

    public String getEngineRPMType() {
        return this.engineRpmType;
    }

    public void setEngineRPMType(String engineRpmType) {
        this.engineRpmType = engineRpmType;
    }

    public float getOffroadEfficiency() {
        return this.offroadEfficiency;
    }

    public void setOffroadEfficiency(float offroadEfficiency) {
        this.offroadEfficiency = offroadEfficiency;
    }

    public TFloatArrayList getCrawlOffsets() {
        return this.crawlOffsets;
    }

    public float getAnimalTrailerSize() {
        return this.animalTrailerSize;
    }

    public ArrayList<String> getZombieType() {
        return this.zombieType;
    }

    public ArrayList<String> getSpecialKeyRing() {
        return this.specialKeyRing;
    }

    public String getRandomZombieType() {
        return this.getZombieType().isEmpty() ? null : this.getZombieType().get(Rand.Next(this.getZombieType().size()));
    }

    public String getRandomSpecialKeyRing() {
        return !this.hasSpecialKeyRing() ? ItemKey.Container.KEY_RING.toString() : PZArrayUtil.pickRandom(this.getSpecialKeyRing());
    }

    public boolean hasSpecialKeyRing() {
        return this.getSpecialKeyRing() == null ? false : !this.getSpecialKeyRing().isEmpty();
    }

    public String getFirstZombieType() {
        return this.getZombieType().isEmpty() ? null : this.getZombieType().get(0);
    }

    public boolean hasZombieType(String outfit) {
        for (int i = 0; i < this.getZombieType().size(); i++) {
            if (this.getZombieType().get(i) != null && this.getZombieType().get(i) == outfit) {
                return true;
            }
        }

        return false;
    }

    public boolean notKillCrops() {
        return this.notKillCrops;
    }

    public boolean hasLighter() {
        return this.hasLighter;
    }

    public String getCarMechanicsOverlay() {
        return this.carMechanicsOverlay;
    }

    public void setCarMechanicsOverlay(String overlay) {
        this.carMechanicsOverlay = overlay;
    }

    public String getCarModelName() {
        return this.carModelName;
    }

    public void setCarModelName(String overlay) {
        this.carModelName = overlay;
    }

    public int getSpecialLootChance() {
        return this.specialLootChance;
    }

    public int getSpecialKeyRingChance() {
        return !this.hasSpecialKeyRing() ? 0 : this.specialKeyRingChance;
    }

    public boolean neverSpawnKey() {
        return this.neverSpawnKey;
    }

    public static final class Anim {
        public String id;
        public String anim;
        public float rate = 1.0F;
        public boolean animate = true;
        public boolean loop;
        public boolean reverse;
        public final Vector3f offset = new Vector3f();
        public final Vector3f angle = new Vector3f();
        public String sound;

        private VehicleScript.Anim makeCopy() {
            VehicleScript.Anim copy = new VehicleScript.Anim();
            copy.id = this.id;
            copy.anim = this.anim;
            copy.rate = this.rate;
            copy.animate = this.animate;
            copy.loop = this.loop;
            copy.reverse = this.reverse;
            copy.offset.set(this.offset);
            copy.angle.set(this.angle);
            copy.sound = this.sound;
            return copy;
        }
    }

    @UsedFromLua
    public static final class Area {
        public String id;
        public float x;
        public float y;
        public float w;
        public float h;

        public String getId() {
            return this.id;
        }

        public Double getX() {
            return BoxedStaticValues.toDouble(this.x);
        }

        public Double getY() {
            return BoxedStaticValues.toDouble(this.y);
        }

        public Double getW() {
            return BoxedStaticValues.toDouble(this.w);
        }

        public Double getH() {
            return BoxedStaticValues.toDouble(this.h);
        }

        public void setX(Double d) {
            this.x = d.floatValue();
        }

        public void setY(Double d) {
            this.y = d.floatValue();
        }

        public void setW(Double d) {
            this.w = d.floatValue();
        }

        public void setH(Double d) {
            this.h = d.floatValue();
        }

        private VehicleScript.Area makeCopy() {
            VehicleScript.Area copy = new VehicleScript.Area();
            copy.id = this.id;
            copy.x = this.x;
            copy.y = this.y;
            copy.w = this.w;
            copy.h = this.h;
            return copy;
        }
    }

    public static final class Container {
        public int capacity;
        public int seat = -1;
        public String seatId;
        public String luaTest;
        public String contentType;
        public boolean conditionAffectsCapacity;

        VehicleScript.Container makeCopy() {
            VehicleScript.Container copy = new VehicleScript.Container();
            copy.capacity = this.capacity;
            copy.seat = this.seat;
            copy.seatId = this.seatId;
            copy.luaTest = this.luaTest;
            copy.contentType = this.contentType;
            copy.conditionAffectsCapacity = this.conditionAffectsCapacity;
            return copy;
        }
    }

    public static final class Door {
        private VehicleScript.Door makeCopy() {
            return new VehicleScript.Door();
        }
    }

    public static final class LightBar {
        public boolean enable;
        public String soundSiren0 = "";
        public String soundSiren1 = "";
        public String soundSiren2 = "";
    }

    @UsedFromLua
    public static final class Model {
        public String id;
        public String file;
        public float scale = 1.0F;
        public final Vector3f offset = new Vector3f();
        public final Vector3f rotate = new Vector3f();
        public String attachmentNameParent;
        public String attachmentNameSelf;
        public boolean ignoreVehicleScale;

        public String getId() {
            return this.id;
        }

        public String getFile() {
            return this.file;
        }

        public float getScale() {
            return this.scale;
        }

        public Vector3f getOffset() {
            return this.offset;
        }

        public Vector3f getRotate() {
            return this.rotate;
        }

        public String getAttachmentNameParent() {
            return this.attachmentNameParent;
        }

        public String getAttachmentNameSelf() {
            return this.attachmentNameSelf;
        }

        VehicleScript.Model makeCopy() {
            VehicleScript.Model copy = new VehicleScript.Model();
            copy.id = this.id;
            copy.file = this.file;
            copy.scale = this.scale;
            copy.offset.set(this.offset);
            copy.rotate.set(this.rotate);
            copy.attachmentNameParent = this.attachmentNameParent;
            copy.attachmentNameSelf = this.attachmentNameSelf;
            copy.ignoreVehicleScale = this.ignoreVehicleScale;
            return copy;
        }
    }

    @UsedFromLua
    public static final class Part {
        public String id = "Unknown";
        public String parent;
        public ArrayList<String> itemType;
        public VehicleScript.Container container;
        public String area;
        public String mechanicArea;
        public String wheel;
        public THashMap<String, KahluaTable> tables;
        public THashMap<String, String> luaFunctions;
        public ArrayList<VehicleScript.Model> models;
        public boolean setAllModelsVisible = true;
        public VehicleScript.Door door;
        public VehicleScript.Window window;
        public ArrayList<VehicleScript.Anim> anims;
        public String category;
        public boolean specificItem = true;
        public boolean mechanicRequireKey;
        public boolean repairMechanic;
        public boolean hasLightsRear;
        private float durability;

        public boolean isMechanicRequireKey() {
            return this.mechanicRequireKey;
        }

        public void setMechanicRequireKey(boolean mechanicRequireKey) {
            this.mechanicRequireKey = mechanicRequireKey;
        }

        public boolean isRepairMechanic() {
            return this.repairMechanic;
        }

        public void setRepairMechanic(boolean repairMechanic) {
            this.repairMechanic = repairMechanic;
        }

        public String getId() {
            return this.id;
        }

        public int getModelCount() {
            return this.models == null ? 0 : this.models.size();
        }

        public VehicleScript.Model getModel(int index) {
            return this.models.get(index);
        }

        public float getDurability() {
            return this.durability;
        }

        public String getMechanicArea() {
            return this.mechanicArea;
        }

        public VehicleScript.Anim getAnimById(String id) {
            if (this.anims == null) {
                return null;
            } else {
                for (int i = 0; i < this.anims.size(); i++) {
                    VehicleScript.Anim anim = this.anims.get(i);
                    if (anim.id.equals(id)) {
                        return anim;
                    }
                }

                return null;
            }
        }

        public VehicleScript.Model getModelById(String id) {
            if (this.models == null) {
                return null;
            } else {
                for (int i = 0; i < this.models.size(); i++) {
                    VehicleScript.Model model = this.models.get(i);
                    if (model.id.equals(id)) {
                        return model;
                    }
                }

                return null;
            }
        }

        VehicleScript.Part makeCopy() {
            VehicleScript.Part copy = new VehicleScript.Part();
            copy.id = this.id;
            copy.parent = this.parent;
            if (this.itemType != null) {
                copy.itemType = new ArrayList<>();
                copy.itemType.addAll(this.itemType);
            }

            if (this.container != null) {
                copy.container = this.container.makeCopy();
            }

            copy.area = this.area;
            copy.mechanicArea = this.mechanicArea;
            copy.wheel = this.wheel;
            if (this.tables != null) {
                copy.tables = new THashMap<>();

                for (Entry<String, KahluaTable> entry : this.tables.entrySet()) {
                    KahluaTable copyOfTable = LuaManager.copyTable(entry.getValue());
                    copy.tables.put(entry.getKey(), copyOfTable);
                }
            }

            if (this.luaFunctions != null) {
                copy.luaFunctions = new THashMap<>();
                copy.luaFunctions.putAll(this.luaFunctions);
            }

            if (this.models != null) {
                copy.models = new ArrayList<>();

                for (int i = 0; i < this.models.size(); i++) {
                    copy.models.add(this.models.get(i).makeCopy());
                }
            }

            copy.setAllModelsVisible = this.setAllModelsVisible;
            if (this.door != null) {
                copy.door = this.door.makeCopy();
            }

            if (this.window != null) {
                copy.window = this.window.makeCopy();
            }

            if (this.anims != null) {
                copy.anims = new ArrayList<>();

                for (int i = 0; i < this.anims.size(); i++) {
                    copy.anims.add(this.anims.get(i).makeCopy());
                }
            }

            copy.category = this.category;
            copy.specificItem = this.specificItem;
            copy.mechanicRequireKey = this.mechanicRequireKey;
            copy.repairMechanic = this.repairMechanic;
            copy.hasLightsRear = this.hasLightsRear;
            copy.durability = this.durability;
            return copy;
        }

        void compact() {
            if (this.anims != null) {
                this.anims.trimToSize();
            }

            if (this.itemType != null) {
                this.itemType.trimToSize();
            }

            if (this.luaFunctions != null) {
                this.luaFunctions.trimToSize();
            }

            if (this.models != null) {
                this.models.trimToSize();
            }

            if (this.tables != null) {
                this.tables.trimToSize();
            }
        }
    }

    @UsedFromLua
    public static final class Passenger {
        public String id;
        public final ArrayList<VehicleScript.Anim> anims = new ArrayList<>();
        public final ArrayList<VehicleScript.Passenger.SwitchSeat> switchSeats = new ArrayList<>();
        public boolean hasRoof = true;
        public boolean showPassenger;
        public String door;
        public String door2;
        public String area;
        public final ArrayList<VehicleScript.Position> positions = new ArrayList<>();

        public String getId() {
            return this.id;
        }

        public VehicleScript.Passenger makeCopy() {
            VehicleScript.Passenger copy = new VehicleScript.Passenger();
            copy.id = this.id;

            for (int i = 0; i < this.anims.size(); i++) {
                copy.anims.add(this.anims.get(i).makeCopy());
            }

            for (int i = 0; i < this.switchSeats.size(); i++) {
                copy.switchSeats.add(this.switchSeats.get(i).makeCopy());
            }

            copy.hasRoof = this.hasRoof;
            copy.showPassenger = this.showPassenger;
            copy.door = this.door;
            copy.door2 = this.door2;
            copy.area = this.area;

            for (int i = 0; i < this.positions.size(); i++) {
                copy.positions.add(this.positions.get(i).makeCopy());
            }

            return copy;
        }

        public int getPositionCount() {
            return this.positions.size();
        }

        public VehicleScript.Position getPosition(int index) {
            return this.positions.get(index);
        }

        public VehicleScript.Position getPositionById(String id) {
            for (int i = 0; i < this.positions.size(); i++) {
                VehicleScript.Position position = this.positions.get(i);
                if (position.id != null && position.id.equals(id)) {
                    return position;
                }
            }

            return null;
        }

        public VehicleScript.Passenger.SwitchSeat getSwitchSeatById(String id) {
            for (int i = 0; i < this.switchSeats.size(); i++) {
                VehicleScript.Passenger.SwitchSeat switchSeat = this.switchSeats.get(i);
                if (switchSeat.id != null && switchSeat.id.equals(id)) {
                    return switchSeat;
                }
            }

            return null;
        }

        public static final class SwitchSeat {
            public String id;
            public int seat;
            public String anim;
            public float rate = 1.0F;
            public String sound;

            public String getId() {
                return this.id;
            }

            public VehicleScript.Passenger.SwitchSeat makeCopy() {
                VehicleScript.Passenger.SwitchSeat copy = new VehicleScript.Passenger.SwitchSeat();
                copy.id = this.id;
                copy.seat = this.seat;
                copy.anim = this.anim;
                copy.rate = this.rate;
                copy.sound = this.sound;
                return copy;
            }
        }
    }

    @UsedFromLua
    public static final class PhysicsShape {
        public int type;
        public final Vector3f offset = new Vector3f();
        public final Vector3f rotate = new Vector3f();
        public final Vector3f extents = new Vector3f();
        public float radius;
        public String physicsShapeScript;

        public String getTypeString() {
            return switch (this.type) {
                case 1 -> "box";
                case 2 -> "sphere";
                case 3 -> "mesh";
                default -> throw new RuntimeException("unhandled VehicleScript.PhysicsShape");
            };
        }

        public Vector3f getOffset() {
            return this.offset;
        }

        public Vector3f getExtents() {
            return this.extents;
        }

        public Vector3f getRotate() {
            return this.rotate;
        }

        public float getRadius() {
            return this.radius;
        }

        public void setRadius(float radius) {
            this.radius = PZMath.clamp(radius, 0.05F, 5.0F);
        }

        public String getPhysicsShapeScript() {
            return this.physicsShapeScript;
        }

        public void setPhysicsShapeScript(String scriptId) {
            this.physicsShapeScript = Objects.requireNonNull(scriptId);
        }

        private VehicleScript.PhysicsShape makeCopy() {
            VehicleScript.PhysicsShape copy = new VehicleScript.PhysicsShape();
            copy.type = this.type;
            copy.extents.set(this.extents);
            copy.offset.set(this.offset);
            copy.rotate.set(this.rotate);
            copy.radius = this.radius;
            return copy;
        }
    }

    @UsedFromLua
    public static final class Position {
        public String id;
        public final Vector3f offset = new Vector3f();
        public final Vector3f rotate = new Vector3f();
        public String area;

        public String getId() {
            return this.id;
        }

        public Vector3f getOffset() {
            return this.offset;
        }

        public Vector3f getRotate() {
            return this.rotate;
        }

        public String getArea() {
            return this.area;
        }

        private VehicleScript.Position makeCopy() {
            VehicleScript.Position copy = new VehicleScript.Position();
            copy.id = this.id;
            copy.offset.set(this.offset);
            copy.rotate.set(this.rotate);
            return copy;
        }
    }

    public static final class Skin {
        public String texture;
        public String textureRust;
        public String textureMask;
        public String textureLights;
        public String textureDamage1Overlay;
        public String textureDamage1Shell;
        public String textureDamage2Overlay;
        public String textureDamage2Shell;
        public String textureShadow;
        public Texture textureData;
        public Texture textureDataRust;
        public Texture textureDataMask;
        public Texture textureDataLights;
        public Texture textureDataDamage1Overlay;
        public Texture textureDataDamage1Shell;
        public Texture textureDataDamage2Overlay;
        public Texture textureDataDamage2Shell;
        public Texture textureDataShadow;

        public void copyMissingFrom(VehicleScript.Skin other) {
            if (this.textureRust == null) {
                this.textureRust = other.textureRust;
            }

            if (this.textureMask == null) {
                this.textureMask = other.textureMask;
            }

            if (this.textureLights == null) {
                this.textureLights = other.textureLights;
            }

            if (this.textureDamage1Overlay == null) {
                this.textureDamage1Overlay = other.textureDamage1Overlay;
            }

            if (this.textureDamage1Shell == null) {
                this.textureDamage1Shell = other.textureDamage1Shell;
            }

            if (this.textureDamage2Overlay == null) {
                this.textureDamage2Overlay = other.textureDamage2Overlay;
            }

            if (this.textureDamage2Shell == null) {
                this.textureDamage2Shell = other.textureDamage2Shell;
            }

            if (this.textureShadow == null) {
                this.textureShadow = other.textureShadow;
            }
        }
    }

    public static final class Sounds {
        public Boolean alarmEnable = false;
        public ArrayList<String> alarm = new ArrayList<>();
        public ArrayList<String> alarmLoop = new ArrayList<>();
        public boolean hornEnable;
        public String horn = "";
        public boolean backSignalEnable;
        public String backSignal = "";
        public String engine;
        public String engineStart;
        public String engineTurnOff;
        public String handBrake;
        public String ignitionFail;
        public String ignitionFailNoPower;
        public final HashSet<String> specified = new HashSet<>();
    }

    @UsedFromLua
    public static final class Wheel {
        public String id;
        public String model;
        public boolean front;
        public final Vector3f offset = new Vector3f();
        public float radius = 0.5F;
        public float width = 0.4F;

        public String getId() {
            return this.id;
        }

        public Vector3f getOffset() {
            return this.offset;
        }

        private VehicleScript.Wheel makeCopy() {
            VehicleScript.Wheel copy = new VehicleScript.Wheel();
            copy.id = this.id;
            copy.model = this.model;
            copy.front = this.front;
            copy.offset.set(this.offset);
            copy.radius = this.radius;
            copy.width = this.width;
            return copy;
        }
    }

    public static final class Window {
        public boolean openable;

        private VehicleScript.Window makeCopy() {
            VehicleScript.Window copy = new VehicleScript.Window();
            copy.openable = this.openable;
            return copy;
        }
    }
}
