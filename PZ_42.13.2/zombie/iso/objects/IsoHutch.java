// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SystemDisabler;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Food;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.popman.animal.AnimalSynchronizationManager;
import zombie.popman.animal.HutchManager;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public class IsoHutch extends IsoObject {
    int linkedX;
    int linkedY;
    int linkedZ;
    boolean open;
    boolean openEggHatch;
    KahluaTableImpl def;
    public int savedX;
    public int savedY;
    public int savedZ;
    public HashMap<Integer, IsoAnimal> animalInside = new HashMap<>();
    public HashMap<Integer, IsoDeadBody> deadBodiesInside = new HashMap<>();
    public ArrayList<IsoAnimal> animalOutside = new ArrayList<>();
    public String type;
    public int lastHourCheck = -1;
    private float exitTimer;
    private int enterSpotX;
    private int enterSpotY;
    private int maxAnimals;
    private int maxNestBox;
    private final HashMap<Integer, IsoHutch.NestBox> nestBoxes = new HashMap<>();
    private float nestBoxDirt;
    private float hutchDirt;
    UpdateLimit updateAnimal = new UpdateLimit(3500L);
    byte animalInsideSize;
    boolean sendUpdate;

    public IsoHutch(IsoCell cell) {
        super(cell);
    }

    public IsoHutch(IsoGridSquare sq, boolean north, String mainSprite, KahluaTableImpl def, IsoGridSquare linkedSq) {
        super(sq, mainSprite, null);
        this.def = def;
        sq.AddSpecialObject(this);
        if (linkedSq != null) {
            this.linkedX = linkedSq.x;
            this.linkedY = linkedSq.y;
            this.linkedZ = linkedSq.z;
            HutchManager.getInstance().remove(this);
        } else if (def != null) {
            this.type = def.rawgetStr("name");
            KahluaTableImpl extraSprites = (KahluaTableImpl)def.rawget("extraSprites");
            if (extraSprites != null) {
                this.savedX = sq.x;
                this.savedY = sq.y;
                this.savedZ = sq.z;
                if (!HutchManager.getInstance().checkHutchExistInList(this)) {
                    HutchManager.getInstance().add(this);
                }

                DesignationZoneAnimal zone = DesignationZoneAnimal.getZoneF(this.getX(), this.getY(), this.getZ());
                if (zone != null && !zone.hutchs.contains(this)) {
                    zone.hutchs.add(this);
                }

                KahluaTableIterator iterator = extraSprites.iterator();

                while (iterator.advance()) {
                    KahluaTableImpl spriteDef = (KahluaTableImpl)iterator.getValue();
                    int xoffset = spriteDef.rawgetInt("xoffset");
                    int yoffset = spriteDef.rawgetInt("yoffset");
                    int zoffset = 0;
                    String sprite = spriteDef.rawgetStr("sprite");
                    IsoGridSquare sq2 = IsoWorld.instance.currentCell.getGridSquare(sq.getX() + xoffset, sq.getY() + yoffset, sq.getZ() + 0);
                    if (sq2 != null) {
                        new IsoHutch(sq2, north, sprite, def, sq);
                    }
                }

                for (int i = 0; i < this.getMaxNestBox() + 1; i++) {
                    this.nestBoxes.put(i, new IsoHutch.NestBox(i));
                }
            }
        }
    }

    public IsoHutch getHutch() {
        if (!this.isSlave()) {
            return this;
        } else {
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(this.linkedX, this.linkedY, this.linkedZ);
            if (sq == null) {
                return null;
            } else {
                for (int i = 0; i < sq.getSpecialObjects().size(); i++) {
                    if (sq.getSpecialObjects().get(i) instanceof IsoHutch hutch) {
                        return hutch;
                    }
                }

                return null;
            }
        }
    }

    public static IsoHutch getHutch(int x, int y, int z) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (sq == null) {
            return null;
        } else {
            for (IsoObject isoObject : sq.getSpecialObjects()) {
                if (isoObject instanceof IsoHutch hutch) {
                    return hutch;
                }
            }

            return null;
        }
    }

    @Override
    public void transmitCompleteItemToClients() {
        if (GameServer.server) {
            if (GameServer.udpEngine == null) {
                return;
            }

            if (SystemDisabler.doWorldSyncEnable) {
                return;
            }

            KahluaTableImpl extraSprites = (KahluaTableImpl)this.def.rawget("extraSprites");
            if (extraSprites != null) {
                KahluaTableIterator iterator = extraSprites.iterator();

                while (iterator.advance()) {
                    KahluaTableImpl spriteDef = (KahluaTableImpl)iterator.getValue();
                    int xoffset = spriteDef.rawgetInt("xoffset");
                    int yoffset = spriteDef.rawgetInt("yoffset");
                    int zoffset = 0;
                    IsoGridSquare sq2 = IsoWorld.instance
                        .currentCell
                        .getGridSquare(this.square.getX() + xoffset, this.square.getY() + yoffset, this.square.getZ() + 0);

                    for (int i = 0; i < sq2.getSpecialObjects().size(); i++) {
                        if (sq2.getSpecialObjects().get(i) instanceof IsoHutch hutch) {
                            INetworkPacket.sendToRelative(PacketTypes.PacketType.AddItemToMap, this.square.x, this.square.y, hutch);
                        }
                    }
                }
            }
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
        b.putByte((byte)(this.open ? 1 : 0));
        b.putByte((byte)(this.openEggHatch ? 1 : 0));
        b.putFloat(this.getHutchDirt());
        b.putFloat(this.getNestBoxDirt());
        b.putByte((byte)this.nestBoxes.size());

        for (IsoHutch.NestBox nestBox : this.nestBoxes.values()) {
            b.putByte((byte)nestBox.eggs.size());

            for (Food egg : nestBox.eggs) {
                try {
                    egg.saveWithSize(b.bb, true);
                } catch (IOException var8) {
                    throw new RuntimeException(var8);
                }
            }
        }
    }

    @Override
    public void syncIsoObjectReceive(ByteBuffer bb) {
        boolean open = bb.get() == 1;
        boolean openEggHatch = bb.get() == 1;
        float hutchDirt = bb.getFloat();
        float nestBoxDirt = bb.getFloat();
        if (this.open != open) {
            this.toggleDoor();
        }

        if (this.openEggHatch != openEggHatch) {
            this.toggleEggHatchDoor();
        }

        this.setHutchDirt(hutchDirt);
        this.setNestBoxDirt(nestBoxDirt);
        byte nestBoxes = bb.get();

        for (int i = 0; i < nestBoxes; i++) {
            byte eggs = bb.get();
            IsoHutch.NestBox nestBox = this.getNestBox(i);
            nestBox.eggs.clear();

            for (int j = 0; j < eggs; j++) {
                try {
                    if (InventoryItem.loadItem(bb, IsoWorld.getWorldVersion()) instanceof Food food) {
                        nestBox.eggs.add(food);
                    }
                } catch (IOException var13) {
                    throw new RuntimeException(var13);
                }
            }
        }
    }

    public boolean haveRoomForNewEggs() {
        for (IsoHutch.NestBox nest : this.nestBoxes.values()) {
            if (nest.getEggsNb() < 10) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void update() {
        if (!this.isSlave() && this.isExistInTheWorld()) {
            if (this.isOwner()) {
                this.sendUpdate = this.updateAnimal.Check() || this.animalInsideSize != this.animalInside.size();
                boolean hourGrow = false;
                if (GameTime.getInstance().getHour() != this.lastHourCheck) {
                    this.lastHourCheck = GameTime.getInstance().getHour();
                    hourGrow = true;
                }

                int prob = 8000 - this.animalInside.size() * 100;
                if (prob < 4500) {
                    prob = 4500;
                }

                if (!this.animalInside.isEmpty() && Rand.NextBool(prob)) {
                    this.hutchDirt = Math.min(this.hutchDirt + 1.0F, 100.0F);
                    this.sync();
                }

                Map<Integer, IsoAnimal> animalInside1 = new HashMap<>(this.animalInside);

                for (Entry<Integer, IsoAnimal> entry : animalInside1.entrySet()) {
                    Integer hutchPosition = entry.getKey();
                    IsoAnimal animal = entry.getValue();
                    if (animal != null) {
                        if (animal.nestBox > -1) {
                            this.animalInside.remove(hutchPosition);
                        } else {
                            if (animal.getData().getHutchPosition() != hutchPosition) {
                                DebugLog.Animal
                                    .warn("animal hutchPosition %d != animalInside index %d".formatted(animal.getData().getHutchPosition(), hutchPosition));
                                animal.getData().setHutchPosition(hutchPosition);
                            }

                            this.updateAnimalInside(animal, hourGrow);
                            if (animal.isDead() && this.deadBodiesInside.get(hutchPosition) == null) {
                                IsoDeadBody deadAnimal = new IsoDeadBody(animal, false, false);
                                this.deadBodiesInside.put(hutchPosition, deadAnimal);
                            }
                        }
                    }
                }

                for (IsoHutch.NestBox box : this.nestBoxes.values()) {
                    IsoAnimal animal = box.animal;
                    if (animal != null && this.nestBoxes.get(animal.nestBox) != null) {
                        this.updateAnimalInside(animal, hourGrow);
                        if (animal.getHealth() <= 0.0F) {
                            this.nestBoxes.get(animal.nestBox).animal = null;
                            animal.nestBox = -1;
                            this.addAnimalInside(animal);
                            break;
                        }
                    }
                }

                if (this.exitTimer > 0.0F) {
                    this.exitTimer = Math.max(0.0F, this.exitTimer - GameTime.getInstance().getMultiplier());
                } else if (!this.animalInside.isEmpty()) {
                    ArrayList<Integer> possibleIndexHen = new ArrayList<>();
                    ArrayList<Integer> possibleIndexRooster = new ArrayList<>();

                    for (int index : this.animalInside.keySet()) {
                        IsoAnimal animal = this.animalInside.get(index);
                        if (animal != null && !animal.isDead()) {
                            if (!animal.isFemale() && !animal.isBaby()) {
                                possibleIndexRooster.add(index);
                            } else {
                                possibleIndexHen.add(index);
                            }
                        }
                    }

                    IsoAnimal animal = null;
                    if (!possibleIndexRooster.isEmpty()) {
                        animal = this.animalInside.get(possibleIndexRooster.get(Rand.Next(0, possibleIndexRooster.size())));
                    } else if (!possibleIndexHen.isEmpty()) {
                        animal = this.animalInside.get(possibleIndexHen.get(Rand.Next(0, possibleIndexHen.size())));
                    }

                    this.checkAnimalExitHutch(animal);
                }

                for (int i = 0; i < this.nestBoxes.size(); i++) {
                    IsoHutch.NestBox nestBox = this.nestBoxes.get(i);

                    for (int j = 0; j < nestBox.getEggsNb(); j++) {
                        Food egg = nestBox.getEgg(j);
                        if (egg.checkEggHatch(this)) {
                            nestBox.removeEgg(j);
                            j--;
                        }

                        egg.update();
                    }
                }

                if (this.sendUpdate) {
                    this.sync();
                    this.animalInsideSize = (byte)this.animalInside.size();
                }
            }
        }
    }

    private void updateAnimalInside(IsoAnimal animal, boolean hourGrow) {
        if (animal != null) {
            if (animal.isDead()) {
                if (this.sendUpdate) {
                    this.sendAnimalUpdate(animal);
                }
            } else {
                if (animal.nestBox > -1) {
                    animal.eggTimerInHutch = Float.valueOf(Math.max(0.0F, animal.eggTimerInHutch - GameTime.getInstance().getMultiplier())).intValue();
                    if (Rand.NextBool(300)) {
                        this.nestBoxDirt = Math.min(this.nestBoxDirt + 1.0F, 100.0F);
                    }

                    if (animal.eggTimerInHutch <= 0) {
                        animal.eggTimerInHutch = 0;
                        this.addEgg(animal);
                        this.sendAnimalUpdate(animal);
                        this.sync();
                    }
                }

                if (animal.adef.isInsideHutchTime(null)) {
                    animal.updateStress();
                } else {
                    animal.changeStress(GameTime.getInstance().getMultiplier() / 15000.0F);
                }

                this.updateAnimalHealthInside(animal, hourGrow);
                if (animal.getHealth() <= 0.0F) {
                    this.killAnimal(animal);
                    this.sendAnimalUpdate(animal);
                    this.sync();
                }

                animal.getData().checkEggs(GameTime.instance.getCalender(), false);
                if (hourGrow) {
                    animal.getData().checkFertilizedTime();
                    if (this.getHutchDirt() < 20.0F) {
                        animal.setHealth(Math.min(1.0F, animal.getHealth() + animal.getData().getHealthLoss(1.0F)));
                    }

                    animal.setHoursSurvived(animal.getHoursSurvived() + 1.0);
                    animal.getData().updateHungerAndThirst(false);
                    if (!this.isDoorClosed()) {
                        animal.checkKilledByMetaPredator(GameTime.getInstance().getHour());
                    }

                    if (animal.getData().getAge() != animal.getData().getDaysSurvived()) {
                        float mod = animal.getData().getAgeGrowModifier();
                        animal.getData().setAge(Float.valueOf(animal.getData().getDaysSurvived() + (mod - 1.0F)).intValue());
                        animal.setHoursSurvived(animal.getData().getAge() * 24);
                        animal.getData().growUp(false);
                    }
                }

                if (this.sendUpdate) {
                    this.sendAnimalUpdate(animal);
                }
            }
        }
    }

    public void doMeta(int hours) {
        for (int i = 0; i < hours; i++) {
            int prob = 25 - (this.animalInside.size() + this.animalOutside.size());
            if (prob > 10) {
                prob = 10;
            }

            if (Rand.NextBool(prob)) {
                this.hutchDirt = Math.min(this.hutchDirt + 1.0F, 100.0F);
            }

            if (Rand.NextBool(prob)) {
                this.nestBoxDirt = Math.min(this.nestBoxDirt + 1.0F, 100.0F);
            }
        }
    }

    private void updateAnimalHealthInside(IsoAnimal animal, boolean hourGrow) {
        float dirt = this.getHutchDirt();
        if (animal.nestBox > -1) {
            dirt = this.getNestBoxDirt();
        }

        if (dirt > 20.0F && Rand.NextBool(250 - (int)dirt)) {
            animal.setHealth(animal.getHealth() - 0.01F * (dirt / 1000.0F) * GameTime.getInstance().getMultiplier());
        }

        if (hourGrow) {
            animal.getData().updateHealth();
        }
    }

    public void killAnimal(IsoAnimal animal) {
        animal.setHealth(0.0F);
        int hutchPosition = animal.getData().getHutchPosition();
        IsoDeadBody deadAnimal = new IsoDeadBody(animal, false, false);
        this.deadBodiesInside.put(hutchPosition, deadAnimal);
    }

    private boolean checkAnimalExitHutch(IsoAnimal animal) {
        if (animal != null && animal.adef != null && !animal.isDead()) {
            boolean exit = false;
            if (animal.getBehavior().forcedOutsideHutch > 0L
                && GameTime.getInstance().getCalender().getTimeInMillis() > animal.getBehavior().forcedOutsideHutch) {
                exit = true;
                animal.getBehavior().forcedOutsideHutch = 0L;
            }

            if (animal.getBehavior().forcedOutsideHutch == 0L && animal.adef.isOutsideHutchTime() && this.isOpen() && animal.nestBox == -1) {
                exit = true;
            }

            if (exit && !this.isDoorClosed()) {
                IsoGridSquare animalSq = IsoWorld.instance
                    .currentCell
                    .getGridSquare(this.savedX + this.getEnterSpotX(), this.savedY + this.getEnterSpotY(), this.savedZ);
                if (animalSq == null) {
                    return false;
                } else if (animal.nestBox > -1) {
                    return false;
                } else {
                    this.releaseAnimal(animalSq, animal);
                    this.exitTimer = Rand.Next(200.0F, 300.0F);
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void releaseAnimal(IsoGridSquare animalSq, IsoAnimal animal) {
        if (animalSq == null) {
            animalSq = IsoWorld.instance.currentCell.getGridSquare(this.savedX + this.getEnterSpotX(), this.savedY + this.getEnterSpotY(), this.savedZ);
        }

        if (animalSq != null) {
            if (!GameClient.client) {
                animal.hutch = null;
                animal.getData().setPreferredHutchPosition(-1);
                animal.getData().enterHutchTimerAfterDestroy = 300;
                animal.addToWorld();
                animal.setX(animalSq.getX());
                animal.setY(animalSq.getY());
                animal.setZ(animalSq.getZ());
                if (!animal.getCell().getObjectList().contains(animal) && !animal.getCell().getAddList().contains(animal)) {
                    animal.getCell().getAddList().add(animal);
                }

                animal.setStateEventDelayTimer(0.0F);
            }

            this.animalInside.remove(animal.getData().getHutchPosition());
            animal.getData().setHutchPosition(-1);
            this.animalOutside.add(animal);
            if (this.isOwner()) {
                this.sync();
            }
        }
    }

    public void removeAnimal(IsoAnimal animal) {
        animal.hutch = null;
        this.animalInside.remove(animal.getData().getHutchPosition());
        this.deadBodiesInside.remove(animal.getData().getHutchPosition());
        animal.getData().setHutchPosition(-1);
        this.sendAnimalUpdate(animal);
    }

    private void removeAnimalFromNestBox(IsoHutch.NestBox nestBox) {
        IsoAnimal animal = nestBox.animal;
        nestBox.animal.nestBox = -1;
        nestBox.animal.getData().eggTime = Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() + Rand.Next(0, 86400);
        nestBox.animal = null;
        this.addAnimalInside(animal);
    }

    public void tryFindAndRemoveAnimalFromNestBox(IsoAnimal animal) {
        this.nestBoxes.values().forEach(nestBox -> {
            if (nestBox.animal != null && nestBox.animal.getAnimalID() == animal.getAnimalID()) {
                nestBox.animal.nestBox = -1;
                nestBox.animal.getData().eggTime = Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() + Rand.Next(0, 86400);
                nestBox.animal = null;
            }
        });
    }

    public boolean addAnimalInNestBox(IsoAnimal animal) {
        for (int i = 0; i < this.getMaxNestBox() + 1; i++) {
            IsoHutch.NestBox nestBox = this.nestBoxes.get(i);
            if (nestBox.animal == null && nestBox.eggs.size() < 10) {
                nestBox.animal = animal;
                animal.hutch = this;
                animal.nestBox = i;
                animal.eggTimerInHutch = Rand.Next(350, 600);
                this.animalInside.remove(animal.getData().getHutchPosition());
                animal.getData().setHutchPosition(-1);
                if (this.isOwner()) {
                    this.sendAnimalUpdate(animal);
                    this.sync();
                }

                return true;
            }
        }

        return false;
    }

    public void addEgg(IsoAnimal animal) {
        Food egg = animal.createEgg();
        this.nestBoxes.get(animal.nestBox).addEgg(egg);
        this.removeAnimalFromNestBox(this.nestBoxes.get(animal.nestBox));
    }

    public void toggleEggHatchDoor() {
        this.openEggHatch = !this.openEggHatch;
        KahluaTableImpl doors = (KahluaTableImpl)this.def.rawget("eggHatchDoors");
        KahluaTableIterator it = doors.iterator();

        while (it.advance()) {
            KahluaTableImpl info = (KahluaTableImpl)it.getValue();
            String testSpriteName = info.rawgetStr("sprite");
            String newSpriteName = info.rawgetStr("closedSprite");
            if (this.openEggHatch) {
                testSpriteName = info.rawgetStr("closedSprite");
                newSpriteName = info.rawgetStr("sprite");
            }

            int hatchXOffset = info.rawgetInt("xoffset");
            int hatchYOffset = info.rawgetInt("yoffset");
            int hatchZOffset = info.rawgetInt("zoffset");
            if (!StringUtils.isNullOrEmpty(testSpriteName) && !StringUtils.isNullOrEmpty(newSpriteName)) {
                IsoSprite testSprite = IsoSpriteManager.instance.namedMap.get(testSpriteName);
                IsoSprite newSprite = IsoSpriteManager.instance.namedMap.get(newSpriteName);
                if (testSprite == null || newSprite == null) {
                    return;
                }

                IsoGridSquare sq2 = IsoWorld.instance
                    .currentCell
                    .getGridSquare(this.square.getX() + hatchXOffset, this.square.getY() + hatchYOffset, this.square.getZ() + hatchZOffset);
                if (sq2 == null) {
                    return;
                }

                for (int i = 0; i < sq2.getSpecialObjects().size(); i++) {
                    IsoHutch hutch = Type.tryCastTo(sq2.getSpecialObjects().get(i), IsoHutch.class);
                    if (hutch != null && testSpriteName.equals(hutch.sprite.getName())) {
                        hutch.setSprite(newSprite);
                    }
                }
            }
        }
    }

    public void reforceUpdate() {
        HutchManager.getInstance().reforceUpdate(this);
    }

    public void toggleDoor() {
        this.reforceUpdate();
        this.open = !this.open;
        if (this.open) {
            for (int i = 0; i < this.animalOutside.size(); i++) {
                this.animalOutside.get(i).getBehavior().callToHutch(this, false);
            }
        }

        KahluaTableImpl extraSprites = (KahluaTableImpl)this.def.rawget("extraSprites");
        if (extraSprites != null) {
            KahluaTableIterator iterator = extraSprites.iterator();

            while (iterator.advance()) {
                KahluaTableImpl spriteDef = (KahluaTableImpl)iterator.getValue();
                int xoffset = spriteDef.rawgetInt("xoffset");
                int yoffset = spriteDef.rawgetInt("yoffset");
                int zoffset = 0;
                String testSpriteName = spriteDef.rawgetStr("sprite");
                String newSpriteName = spriteDef.rawgetStr("spriteOpen");
                if (!this.open) {
                    testSpriteName = spriteDef.rawgetStr("spriteOpen");
                    newSpriteName = spriteDef.rawgetStr("sprite");
                }

                if (!StringUtils.isNullOrEmpty(newSpriteName) && !StringUtils.isNullOrEmpty(testSpriteName)) {
                    IsoSprite testSprite = IsoSpriteManager.instance.namedMap.get(testSpriteName);
                    IsoSprite newSprite = IsoSpriteManager.instance.namedMap.get(newSpriteName);
                    if (testSprite != null && newSprite != null) {
                        IsoGridSquare sq2 = IsoWorld.instance
                            .currentCell
                            .getGridSquare(this.square.getX() + xoffset, this.square.getY() + yoffset, this.square.getZ() + 0);
                        if (sq2 != null) {
                            for (int i = 0; i < sq2.getSpecialObjects().size(); i++) {
                                IsoHutch hutch = Type.tryCastTo(sq2.getSpecialObjects().get(i), IsoHutch.class);
                                if (hutch != null && testSpriteName.equals(hutch.sprite.getName())) {
                                    hutch.setSprite(newSprite);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    private void sendAnimalUpdate(IsoAnimal animal) {
        if (GameServer.server) {
            animal.networkAi.setAnimalPacket(null);
            AnimalSynchronizationManager.getInstance().setSendToClients(animal.onlineId);
        }
    }

    private KahluaTableImpl getDefFromSprite() {
        if (StringUtils.isNullOrEmpty(this.getSprite().getName())) {
            return null;
        } else {
            KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("HutchDefinitions");
            if (definitions == null) {
                return null;
            } else {
                KahluaTableImpl hutchs = (KahluaTableImpl)definitions.rawget("hutchs");
                KahluaTableIterator iterator = hutchs.iterator();

                while (iterator.advance()) {
                    KahluaTableImpl value = (KahluaTableImpl)iterator.getValue();
                    KahluaTableImpl extraSprites = (KahluaTableImpl)value.rawget("extraSprites");
                    String baseSprite = value.rawgetStr("baseSprite");
                    if (!StringUtils.isNullOrEmpty(this.getSprite().getName()) && this.getSprite().getName().equals(baseSprite)) {
                        return value;
                    }

                    KahluaTableIterator iterator2 = extraSprites.iterator();

                    while (iterator2.advance()) {
                        KahluaTableImpl spriteDef = (KahluaTableImpl)iterator2.getValue();
                        if (!StringUtils.isNullOrEmpty(this.getSprite().getName())
                            && (
                                this.getSprite().getName().equals(spriteDef.rawgetStr("sprite"))
                                    || this.getSprite().getName().equals(spriteDef.rawgetStr("spriteOpen"))
                            )) {
                            return value;
                        }
                    }
                }

                return null;
            }
        }
    }

    private boolean checkNestBoxPrefPosition(int pos) {
        for (int i = 0; i < this.getMaxNestBox() + 1; i++) {
            IsoAnimal nestAnimal = this.nestBoxes.get(i).animal;
            if (nestAnimal != null && nestAnimal.getData().getPreferredHutchPosition() == pos) {
                return true;
            }
        }

        return false;
    }

    public boolean addAnimalInside(IsoAnimal animal) {
        return this.addAnimalInside(animal, true);
    }

    public boolean addAnimalInside(IsoAnimal animal, boolean bSync) {
        if (this.animalInside.containsValue(animal)) {
            DebugLog.Animal.warn("animal already exists in animalInside");
            return false;
        } else {
            if (animal.getData().getPreferredHutchPosition() == -1) {
                animal.getData().setPreferredHutchPosition(Rand.Next(0, this.getMaxAnimals()));
            }

            int tries = 0;

            while (
                this.animalInside.get(animal.getData().getPreferredHutchPosition()) != null
                    || this.deadBodiesInside.get(animal.getData().getPreferredHutchPosition()) != null
                    || this.checkNestBoxPrefPosition(animal.getData().getPreferredHutchPosition())
            ) {
                if (++tries > 100) {
                    break;
                }

                animal.getData().setPreferredHutchPosition(Rand.Next(0, this.getMaxAnimals()));
            }

            if (this.animalInside.get(animal.getData().getPreferredHutchPosition()) == null) {
                this.animalInside.put(animal.getData().getPreferredHutchPosition(), animal);
                animal.hutch = this;
                animal.getData().setHutchPosition(animal.getData().getPreferredHutchPosition());
                if (bSync && this.isOwner()) {
                    this.sendAnimalUpdate(animal);
                    this.sync();
                }

                this.tryRemoveAnimalFromWorld(animal);
                return true;
            } else {
                return false;
            }
        }
    }

    public void addAnimalOutside(IsoAnimal animal) {
        if (!GameClient.client) {
            animal.hutch = null;
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.linkedX = input.getInt();
        this.linkedY = input.getInt();
        this.linkedZ = input.getInt();
        if (!this.isSlave()) {
            if (WorldVersion >= 204) {
                this.spriteName = GameWindow.ReadString(input);
                this.sprite = IsoSpriteManager.instance.getSprite(this.spriteName);
            }

            this.def = this.getDefFromSprite();
            if (this.def == null) {
                throw new IOException("hutch definition not found");
            }

            this.type = this.def.rawgetStr("name");
            this.open = input.get() != 0;
            if (WorldVersion >= 204) {
                this.openEggHatch = input.get() != 0;
            }

            this.savedX = input.getInt();
            this.savedY = input.getInt();
            this.savedZ = input.getInt();
            ArrayList<IsoAnimal> loadedAnimals = new ArrayList<>();
            if (WorldVersion >= 212) {
                int size = input.getInt();
                if (GameClient.client) {
                    input.position(input.position() + size);
                } else {
                    int nbOfAnimals = input.get();

                    for (int i = 0; i < nbOfAnimals; i++) {
                        IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell());
                        boolean seri = input.get() == 1;
                        byte classID = input.get();
                        animal.load(input, WorldVersion, IS_DEBUG_SAVE);
                        loadedAnimals.add(animal);
                        animal.removeFromSquare();
                    }
                }
            } else {
                int nbOfAnimals = input.get();

                for (int i = 0; i < nbOfAnimals; i++) {
                    IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell());
                    boolean seri = input.get() == 1;
                    byte classID = input.get();
                    animal.load(input, WorldVersion, IS_DEBUG_SAVE);
                    loadedAnimals.add(animal);
                    animal.removeFromSquare();
                }
            }

            this.hutchDirt = input.getFloat();
            this.nestBoxDirt = input.getFloat();
            int nestBoxCount = input.get();

            for (int i = 0; i < nestBoxCount; i++) {
                IsoHutch.NestBox nestBox = i < this.nestBoxes.size() ? this.nestBoxes.get(i) : new IsoHutch.NestBox(i);
                nestBox.load(input, WorldVersion);
                if (!this.nestBoxes.containsKey(i) && i <= this.getMaxNestBox()) {
                    this.nestBoxes.put(i, nestBox);
                }
            }

            for (int ix = 0; ix < loadedAnimals.size(); ix++) {
                IsoAnimal animal = loadedAnimals.get(ix);
                this.addAnimalInside(animal, false);
            }
        }
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        output.putInt(this.linkedX);
        output.putInt(this.linkedY);
        output.putInt(this.linkedZ);
        if (!this.isSlave()) {
            GameWindow.WriteString(output, this.spriteName);
            output.put((byte)(this.isOpen() ? 1 : 0));
            output.put((byte)(this.isEggHatchDoorOpen() ? 1 : 0));
            output.putInt(this.savedX);
            output.putInt(this.savedY);
            output.putInt(this.savedZ);
            int pos = output.position();
            output.putInt(0);
            int posStart = output.position();
            ArrayList<IsoAnimal> animals = new ArrayList<>(this.animalInside.values());
            output.put((byte)animals.size());

            for (int i = 0; i < animals.size(); i++) {
                animals.get(i).save(output, IS_DEBUG_SAVE);
            }

            int posEnd = output.position();
            output.position(pos);
            output.putInt(posEnd - posStart);
            output.position(posEnd);
            output.putFloat(this.hutchDirt);
            output.putFloat(this.nestBoxDirt);
            output.put((byte)this.nestBoxes.size());

            for (int i = 0; i < this.nestBoxes.size(); i++) {
                IsoHutch.NestBox nestBox = this.nestBoxes.get(i);
                nestBox.save(output);
            }
        }
    }

    public boolean addMetaEgg(IsoAnimal animal) {
        for (int i = 0; i < this.getMaxNestBox() + 1; i++) {
            if (this.nestBoxes.get(i).animal == null && this.nestBoxes.get(i).eggs.size() < 10) {
                this.nestBoxes.get(i).addEgg(animal.createEgg());
                return true;
            }
        }

        return false;
    }

    public boolean isSlave() {
        return this.linkedX > 0 && this.linkedY > 0;
    }

    @Override
    public String getObjectName() {
        return "IsoHutch";
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        if (!this.isSlave() && !HutchManager.getInstance().checkHutchExistInList(this)) {
            HutchManager.getInstance().add(this);
        }
    }

    public void removeHutch() {
        for (int x = this.square.x - 2; x < this.square.x + 3; x++) {
            for (int y = this.square.y - 2; y < this.square.y + 3; y++) {
                IsoGridSquare sq = this.square.getCell().getGridSquare(x, y, this.square.z);
                if (sq != null) {
                    ArrayList<IsoHutch> hutch = sq.getHutchTiles();

                    for (int i = 0; i < hutch.size(); i++) {
                        hutch.get(i).releaseAllAnimals();
                        hutch.get(i).dropAllEggs();
                        hutch.get(i).removeFromWorld();
                        hutch.get(i).getSquare().transmitRemoveItemFromSquare(hutch.get(i));
                    }
                }
            }
        }
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        HutchManager.getInstance().remove(this);
    }

    public void dropAllEggs() {
        for (IsoHutch.NestBox nestBox : this.nestBoxes.values()) {
            for (int i = 0; i < nestBox.eggs.size(); i++) {
                this.square.AddWorldInventoryItem(nestBox.eggs.get(i), Rand.Next(0.0F, 0.8F), Rand.Next(0.0F, 0.8F), 0.0F);
            }
        }
    }

    public void releaseAllAnimals() {
        if (!this.animalInside.isEmpty()) {
            ArrayList<IsoAnimal> animals = new ArrayList<>();

            for (IsoAnimal animal : this.animalInside.values()) {
                animals.add(animal);
            }

            for (int i = 0; i < animals.size(); i++) {
                this.releaseAnimal(null, animals.get(i));
            }
        }
    }

    public HashMap<Integer, IsoAnimal> getAnimalInside() {
        return this.animalInside;
    }

    public IsoAnimal getAnimal(Integer index) {
        return this.animalInside.get(index);
    }

    public IsoDeadBody getDeadBody(Integer index) {
        return this.deadBodiesInside.get(index);
    }

    public int getMaxAnimals() {
        if (this.maxAnimals == 0) {
            this.maxAnimals = this.def.rawgetInt("maxAnimals");
        }

        return this.maxAnimals;
    }

    public int getMaxNestBox() {
        if (this.maxNestBox == 0) {
            this.maxNestBox = this.def.rawgetInt("maxNestBox");
        }

        return this.maxNestBox;
    }

    public int getEnterSpotX() {
        if (this.enterSpotX == 0) {
            this.enterSpotX = this.def.rawgetInt("enterSpotX");
        }

        return this.enterSpotX;
    }

    public int getEnterSpotY() {
        if (this.enterSpotY == 0) {
            this.enterSpotY = this.def.rawgetInt("enterSpotY");
        }

        return this.enterSpotY;
    }

    public boolean haveEggHatchDoor() {
        return !StringUtils.isNullOrEmpty(this.def.rawgetStr("openHatchSprite"));
    }

    public boolean isEggHatchDoorOpen() {
        return this.openEggHatch;
    }

    public boolean isEggHatchDoorClosed() {
        return !this.openEggHatch;
    }

    public IsoGridSquare getEntrySq() {
        return this.getSquare()
            .getCell()
            .getGridSquare(this.getSquare().x + this.getEnterSpotX(), this.getSquare().y + this.getEnterSpotY(), this.getSquare().z);
    }

    public IsoAnimal getAnimalInNestBox(Integer index) {
        return this.nestBoxes.get(index) != null ? this.nestBoxes.get(index).animal : null;
    }

    public IsoHutch.NestBox getNestBox(Integer index) {
        return this.nestBoxes.get(index);
    }

    public float getHutchDirt() {
        return this.hutchDirt;
    }

    public void setHutchDirt(float hutchDirt) {
        this.hutchDirt = hutchDirt;
    }

    public float getNestBoxDirt() {
        return this.nestBoxDirt;
    }

    public void setNestBoxDirt(float nestBoxDirt) {
        this.nestBoxDirt = nestBoxDirt;
    }

    public boolean isDoorClosed() {
        return !this.open;
    }

    public boolean isAllDoorClosed() {
        return !this.open && !this.openEggHatch;
    }

    public boolean isOwner() {
        return !GameClient.client;
    }

    public void tryRemoveAnimalFromWorld(IsoAnimal animal) {
        if (GameClient.client && animal != null && animal.isExistInTheWorld()) {
            this.animalOutside.remove(animal);
            animal.removeFromSquare();
            animal.removeFromWorld();
        }
    }

    class AgeComparator implements Comparator<IsoAnimal> {
        AgeComparator() {
            Objects.requireNonNull(IsoHutch.this);
            super();
        }

        public int compare(IsoAnimal a, IsoAnimal b) {
            return b.getData().getAge() - a.getData().getAge();
        }
    }

    @UsedFromLua
    public class NestBox {
        public IsoAnimal animal;
        ArrayList<Food> eggs;
        public static final int maxEggs = 10;
        final int index;

        public NestBox(final int index) {
            Objects.requireNonNull(IsoHutch.this);
            super();
            this.eggs = new ArrayList<>();
            this.index = index;
        }

        public int getIndex() {
            return this.index;
        }

        public int getEggsNb() {
            return this.eggs.size();
        }

        public void addEgg(Food egg) {
            this.eggs.add(egg);
        }

        public Food getEgg(int index) {
            return this.eggs.get(index);
        }

        public Food removeEgg(int index) {
            return this.eggs.remove(index);
        }

        void save(ByteBuffer output) throws IOException {
            output.put((byte)this.eggs.size());

            for (int i = 0; i < this.eggs.size(); i++) {
                Food egg = this.eggs.get(i);
                egg.saveWithSize(output, false);
            }

            output.put((byte)(this.animal != null ? 1 : 0));
            if (this.animal != null) {
                this.animal.save(output, false, false);
            }
        }

        void load(ByteBuffer input, int WorldVersion) throws IOException {
            int numEggs = input.get();

            for (int i = 0; i < numEggs; i++) {
                if (InventoryItem.loadItem(input, WorldVersion) instanceof Food food) {
                    this.eggs.add(food);
                }
            }

            boolean nestBoxHasAnimal = input.get() != 0;
            if (nestBoxHasAnimal) {
                this.animal = new IsoAnimal(IsoWorld.instance.getCell());
                this.animal.load(input, WorldVersion);
                this.animal.nestBox = this.index;
            }
        }
    }
}
