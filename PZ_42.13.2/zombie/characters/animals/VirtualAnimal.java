// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import org.joml.Vector2f;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;

@UsedFromLua
public final class VirtualAnimal {
    float x;
    float y;
    float z;
    final Vector2f forwardDirection = new Vector2f();
    final ArrayList<IsoAnimal> animals = new ArrayList<>();
    VirtualAnimalState state;
    boolean moveForwardOnZone;
    String currentZoneAction;
    public double nextRestTime = -1.0;
    public double nextEatTime = -1.0;
    public double id;
    public String migrationGroup;
    public float speed = 1.0F;
    public int timeToEat;
    public int timeToSleep;
    public int trackChance = 200;
    public int poopChance = 200;
    public int brokenTwigsChance = 200;
    public int herbGrazeChance = 200;
    public int furChance = 200;
    public int flatHerbChance = 200;
    public double wakeTime;
    public double eatStartTime;
    public ArrayList<Integer> sleepPeriodStart = new ArrayList<>();
    public ArrayList<Integer> sleepPeriodEnd = new ArrayList<>();
    public ArrayList<Integer> eatPeriodStart = new ArrayList<>();
    public ArrayList<Integer> eatPeriodEnd = new ArrayList<>();
    public boolean debugForceSleep;
    public boolean debugForceEat;
    public AnimalZone zone;
    private boolean removed;

    public VirtualAnimal() {
        this.moveForwardOnZone = Rand.NextBool(2);
        this.currentZoneAction = "Follow";
        this.id = Rand.Next(9999999) + 100000.0;
    }

    public float getX() {
        return this.x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return this.z;
    }

    public void setZ(float z) {
        z = Math.max(-32.0F, z);
        z = Math.min(31.0F, z);
        this.z = z;
    }

    public void setState(VirtualAnimalState state) {
        this.state = state;
    }

    public VirtualAnimalState getState() {
        return this.state;
    }

    void update() {
        if (MigrationGroupDefinitions.getMigrationDefs().get(this.migrationGroup) != null) {
            if (this.state == null) {
                this.state = new VirtualAnimalState.StateFollow(this);
                AnimalZone zone = AnimalZones.getInstance().getClosestZone(this.x, this.y, null);
                if (zone != null) {
                    this.currentZoneAction = zone.getAction();
                }
            }

            this.state.update();
        }
    }

    void save(ByteBuffer output) throws IOException {
        output.putDouble(this.id);
        if (this.zone != null) {
            output.putLong(this.zone.id.getMostSignificantBits());
            output.putLong(this.zone.id.getLeastSignificantBits());
        } else {
            output.putLong(0L);
            output.putLong(0L);
        }

        output.putFloat(this.x);
        output.putFloat(this.y);
        output.putFloat(this.z);
        output.putFloat(this.forwardDirection.x);
        output.putFloat(this.forwardDirection.y);
        GameWindow.WriteString(output, this.migrationGroup);
        output.putDouble(this.nextEatTime);
        output.putDouble(this.nextRestTime);
        output.putShort((short)this.animals.size());

        for (int i = 0; i < this.animals.size(); i++) {
            IsoAnimal animal = this.animals.get(i);
            animal.save(output, false);
        }
    }

    void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.id = input.getDouble();
        UUID zoneID = new UUID(input.getLong(), input.getLong());
        this.zone = zoneID.getMostSignificantBits() == 0L && zoneID.getLeastSignificantBits() == 0L
            ? null
            : IsoWorld.instance.metaGrid.animalZoneHandler.getZone(zoneID);
        this.x = input.getFloat();
        this.y = input.getFloat();
        this.z = input.getFloat();
        this.forwardDirection.x = input.getFloat();
        this.forwardDirection.y = input.getFloat();
        this.migrationGroup = GameWindow.ReadString(input);
        this.nextEatTime = input.getDouble();
        this.nextRestTime = input.getDouble();
        int count = input.getShort();

        for (int i = 0; i < count; i++) {
            if (!(IsoObject.factoryFromFileInput(IsoWorld.instance.currentCell, input) instanceof IsoAnimal animal)) {
                throw new RuntimeException("expected IsoAnimal here");
            }

            animal.load(input, WorldVersion);
            IsoWorld.instance.currentCell.getAddList().remove(animal);
            IsoWorld.instance.currentCell.getObjectList().remove(animal);
            this.animals.add(animal);
        }
    }

    public void forceRest() {
        this.debugForceSleep = true;
    }

    public void forceEat() {
        this.debugForceEat = true;
    }

    public void forceWakeUp() {
        this.wakeTime = 0.0;
    }

    public void forceStopEat() {
        this.eatStartTime = -100000.0;
    }

    public boolean isEating() {
        return this.state instanceof VirtualAnimalState.StateEat;
    }

    public boolean isSleeping() {
        return this.state instanceof VirtualAnimalState.StateSleep;
    }

    public boolean isTimeToSleep() {
        if (this.debugForceSleep) {
            return true;
        } else if (!this.sleepPeriodStart.isEmpty() && !this.sleepPeriodEnd.isEmpty() && this.sleepPeriodStart.size() == this.sleepPeriodEnd.size()) {
            for (int i = 0; i < this.sleepPeriodStart.size(); i++) {
                int currentHour = GameTime.getInstance().getHour();
                int start = this.sleepPeriodStart.get(i);
                int end = this.sleepPeriodEnd.get(i);
                if (start < end) {
                    if (currentHour >= start && currentHour < end) {
                        return true;
                    }

                    if (currentHour >= end && currentHour < start) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return GameTime.getInstance().getWorldAgeHours() > this.nextRestTime;
        }
    }

    public boolean isTimeToEat() {
        if (this.debugForceEat) {
            return true;
        } else if (!this.eatPeriodStart.isEmpty() && !this.eatPeriodEnd.isEmpty() && this.eatPeriodStart.size() == this.eatPeriodEnd.size()) {
            for (int i = 0; i < this.eatPeriodStart.size(); i++) {
                int currentHour = GameTime.getInstance().getHour();
                int start = this.eatPeriodStart.get(i);
                int end = this.eatPeriodEnd.get(i);
                if (start < end) {
                    if (currentHour >= start && currentHour < end) {
                        return true;
                    }

                    if (currentHour >= end && currentHour < start) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return GameTime.getInstance().getWorldAgeHours() > this.nextEatTime;
        }
    }

    public String getNextSleepPeriod() {
        int period = 100000;
        if (this.isTimeToSleep()) {
            return this.getEndSleepPeriod();
        } else if (!this.sleepPeriodStart.isEmpty() && !this.sleepPeriodEnd.isEmpty() && this.sleepPeriodStart.size() == this.sleepPeriodEnd.size()) {
            for (int i = 0; i < this.sleepPeriodStart.size(); i++) {
                int currentHour = GameTime.getInstance().getHour();
                int start = this.sleepPeriodStart.get(i);
                if (start < currentHour) {
                    start += 24;
                }

                if (start - currentHour < period) {
                    period = start - currentHour;
                }
            }

            return "in " + (period * 60 - (GameTime.getInstance().getHour() + GameTime.getInstance().getMinutes())) + " mins";
        } else {
            return "in " + Math.max(0, (int)Math.floor((this.nextRestTime - GameTime.getInstance().getWorldAgeHours()) * 60.0) + 1) + " mins";
        }
    }

    public String getEndSleepPeriod() {
        int period = 100000;
        if (this.debugForceSleep) {
            return "now";
        } else if (!this.sleepPeriodStart.isEmpty() && !this.sleepPeriodEnd.isEmpty() && this.sleepPeriodStart.size() == this.sleepPeriodEnd.size()) {
            for (int i = 0; i < this.sleepPeriodStart.size(); i++) {
                int currentHour = GameTime.getInstance().getHour();
                int end = this.sleepPeriodEnd.get(i);
                if (end > currentHour && end - currentHour < period) {
                    period = end - currentHour;
                }
            }

            return "now, ends in " + (period * 60 - (GameTime.getInstance().getHour() + GameTime.getInstance().getMinutes())) + " mins";
        } else {
            return "now";
        }
    }

    public String getEndEatPeriod() {
        int period = 100000;
        if (this.debugForceEat) {
            return "now";
        } else if (!this.eatPeriodStart.isEmpty() && !this.eatPeriodEnd.isEmpty() && this.eatPeriodStart.size() == this.eatPeriodEnd.size()) {
            for (int i = 0; i < this.eatPeriodStart.size(); i++) {
                int currentHour = GameTime.getInstance().getHour();
                int end = this.eatPeriodEnd.get(i);
                if (end > currentHour && end - currentHour < period) {
                    period = end - currentHour;
                }
            }

            return "now, ends in " + (period * 60 - (GameTime.getInstance().getHour() + GameTime.getInstance().getMinutes())) + " mins";
        } else {
            return "now";
        }
    }

    public String getNextEatPeriod() {
        int period = 100000;
        if (this.isTimeToEat()) {
            return this.getEndEatPeriod();
        } else if (!this.eatPeriodStart.isEmpty() && !this.eatPeriodEnd.isEmpty() && this.eatPeriodStart.size() == this.eatPeriodEnd.size()) {
            for (int i = 0; i < this.eatPeriodStart.size(); i++) {
                int currentHour = GameTime.getInstance().getHour();
                int start = this.eatPeriodStart.get(i);
                if (start < currentHour) {
                    start += 24;
                }

                if (start - currentHour < period) {
                    period = start - currentHour;
                }
            }

            return "in " + (period * 60 - (GameTime.getInstance().getHour() + GameTime.getInstance().getMinutes())) + " mins";
        } else {
            return "in " + Math.max(0, (int)Math.floor((this.nextEatTime - GameTime.getInstance().getWorldAgeHours()) * 60.0) + 1) + " mins";
        }
    }

    public void setRemoved(boolean bRemoved) {
        this.removed = bRemoved;
    }

    public boolean isRemoved() {
        return this.removed;
    }

    public IsoAnimal findAnimalById(int animalID) {
        for (int i = 0; i < this.animals.size(); i++) {
            IsoAnimal isoAnimal = this.animals.get(i);
            if (isoAnimal.getAnimalID() == animalID) {
                return isoAnimal;
            }
        }

        return null;
    }
}
