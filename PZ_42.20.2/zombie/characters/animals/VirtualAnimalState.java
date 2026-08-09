// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import java.util.ArrayList;
import org.joml.Vector2f;
import zombie.GameTime;
import zombie.core.random.Rand;
import zombie.util.list.PZArrayUtil;

public abstract class VirtualAnimalState {
    static final ArrayList<AnimalZoneJunction> tempJunctions = new ArrayList<>();
    static final ArrayList<AnimalZoneJunction> possibleJunctions = new ArrayList<>();
    static float distanceBoost = 1.0F;
    protected VirtualAnimal animal;

    public void addAnimalTracks(AnimalTracksDefinitions.AnimalTracksType trackType) {
        if (trackType != null) {
            AnimalChunk chunk = AnimalManagerWorker.getInstance().getAnimalChunk(this.animal.x, this.animal.y);
            if (chunk != null) {
                for (int i = 0; i < chunk.animalTracks.size(); i++) {
                    if (chunk.animalTracks.get(i).animalType.equalsIgnoreCase(this.animal.migrationGroup)
                        && chunk.animalTracks.get(i).trackType.equalsIgnoreCase(trackType.type)) {
                        return;
                    }
                }

                chunk.addTracks(this.animal, trackType);
            }
        }
    }

    public VirtualAnimalState(VirtualAnimal animal) {
        this.animal = animal;
    }

    public abstract void update();

    protected AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
        return null;
    }

    protected void reachedEnd() {
    }

    void moveAlongPath(String action, float distance) {
        AnimalZone closestZone = AnimalZones.getInstance().getClosestZone(this.animal.x, this.animal.y, action);
        if (closestZone == null) {
            this.animal.animals.forEach(isoAnimal -> isoAnimal.setAnimalZone(null));
        } else {
            this.moveAlongPath(distance * distanceBoost * this.animal.speed, closestZone);
        }
    }

    void moveAlongPath(float distance, AnimalZone zone) {
        Vector2f pos = new Vector2f();
        float t = zone.getClosestPointOnPolyline(this.animal.x, this.animal.y, pos);
        float zoneLength = zone.getPolylineLength();
        boolean moveForwardOnZone = this.animal.moveForwardOnZone;
        float t2 = moveForwardOnZone ? t + distance / zoneLength : t - distance / zoneLength;
        zone.getJunctionsBetween(t, t2, tempJunctions);
        if (!tempJunctions.isEmpty()) {
            AnimalZoneJunction junction = this.visitJunction(
                zone, tempJunctions.get(0).isFirstPointOnZone1() || tempJunctions.get(0).isLastPointOnZone1(), tempJunctions
            );
            if (junction != null) {
                zone = junction.zoneOther;
                t2 = zone.getDistanceOfPointFromStart(junction.pointIndexOther) / zone.getPolylineLength();
                this.animal.moveForwardOnZone = junction.isFirstPointOnZone2() || !junction.isLastPointOnZone2() && Rand.NextBool(2);
                t2 = this.animal.moveForwardOnZone ? t2 + 1.0F / zone.getPolylineLength() : t2 - 1.0F / zone.getPolylineLength();
                this.animal.currentZoneAction = zone.action;
            }
        }

        if (t2 >= 1.0F && moveForwardOnZone) {
            t2 = 1.0F;
            this.animal.moveForwardOnZone = false;
            this.reachedEnd();
        }

        if (t2 <= 0.0F && !moveForwardOnZone) {
            t2 = 0.0F;
            this.animal.moveForwardOnZone = true;
            this.reachedEnd();
        }

        zone.getPointOnPolyline(t2, pos);
        zone.getDirectionOnPolyline(t2, this.animal.forwardDirection);
        if (!this.animal.moveForwardOnZone) {
            this.animal.forwardDirection.mul(-1.0F);
        }

        AnimalManagerWorker mgrWorker = AnimalManagerWorker.getInstance();
        AnimalZone zone2 = zone;
        this.animal.animals.forEach(isoAnimal -> {
            isoAnimal.setAnimalZone(zone2);
            isoAnimal.setMoveForwardOnZone(this.animal.moveForwardOnZone);
        });
        mgrWorker.moveAnimal(this.animal, pos.x, pos.y);
    }

    public static class StateEat extends VirtualAnimalState {
        public StateEat(VirtualAnimal animal) {
            super(animal);
            animal.eatStartTime = GameTime.getInstance().getWorldAgeHours();
        }

        @Override
        public void update() {
            double worldAgeHours = GameTime.getInstance().getWorldAgeHours();
            this.addAnimalTracks(AnimalTracksDefinitions.getRandomTrack(this.animal.migrationGroup, "eat"));
            if (worldAgeHours - this.animal.eatStartTime > this.animal.timeToEat / 60.0F) {
                this.animal.state = new VirtualAnimalState.StateMoveFromEat(this.animal);
                this.animal.nextEatTime = -1.0;
                this.animal.debugForceEat = false;
            }
        }
    }

    public static class StateFollow extends VirtualAnimalState {
        public StateFollow(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.animal.currentZoneAction, GameTime.getInstance().getMultiplier() / 2.0F * 0.2F);
            if (!this.animal.isRemoved()) {
                this.checkNextEatTime();
                if ("Eat".equals(this.animal.currentZoneAction) && this.animal.isTimeToEat()) {
                    this.animal.state = new VirtualAnimalState.StateMoveToEat(this.animal);
                } else {
                    this.checkNextRestTime();
                    if ("Sleep".equals(this.animal.currentZoneAction) && this.animal.isTimeToSleep()) {
                        this.animal.state = new VirtualAnimalState.StateMoveToSleep(this.animal);
                    } else {
                        this.addAnimalTracks(AnimalTracksDefinitions.getRandomTrack(this.animal.migrationGroup, "walk"));
                    }
                }
            }
        }

        private void checkNextEatTime() {
            if (this.animal.nextEatTime < 0.0) {
                this.animal.nextEatTime = MigrationGroupDefinitions.getNextEatTime(this.animal.migrationGroup);
            }
        }

        private void checkNextRestTime() {
            if (this.animal.nextRestTime < 0.0) {
                this.animal.nextRestTime = MigrationGroupDefinitions.getNextSleepTime(this.animal.migrationGroup);
            }
        }

        @Override
        public AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
            possibleJunctions.clear();

            for (int i = 0; i < junctions.size(); i++) {
                AnimalZoneJunction junction = junctions.get(i);
                if ("Eat".equals(junction.zoneOther.action) && this.animal.isTimeToEat()) {
                    return junction;
                }

                if ("Sleep".equals(junction.zoneOther.action) && this.animal.isTimeToSleep()) {
                    return junction;
                }

                if ("Follow".equals(junction.zoneOther.action)) {
                    possibleJunctions.add(junction);
                }
            }

            return "Follow".equals(zone.action) && !bEndPoint && Rand.NextBool(possibleJunctions.size() + 1) ? null : PZArrayUtil.pickRandom(possibleJunctions);
        }
    }

    public static class StateMoveFromEat extends VirtualAnimalState {
        public StateMoveFromEat(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.animal.currentZoneAction, GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
        }

        @Override
        public AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
            possibleJunctions.clear();

            for (int i = 0; i < junctions.size(); i++) {
                AnimalZoneJunction junction = junctions.get(i);
                if ("Follow".equals(junction.zoneOther.action)) {
                    possibleJunctions.add(junction);
                }
            }

            AnimalZoneJunction junction = PZArrayUtil.pickRandom(possibleJunctions);
            if (junction == null) {
                return null;
            } else {
                this.animal.state = new VirtualAnimalState.StateFollow(this.animal);
                return junction;
            }
        }
    }

    public static class StateMoveFromSleep extends VirtualAnimalState {
        public StateMoveFromSleep(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.animal.currentZoneAction, GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
        }

        @Override
        public AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
            possibleJunctions.clear();

            for (int i = 0; i < junctions.size(); i++) {
                AnimalZoneJunction junction = junctions.get(i);
                if ("Follow".equals(junction.zoneOther.action)) {
                    possibleJunctions.add(junction);
                }
            }

            AnimalZoneJunction junction = PZArrayUtil.pickRandom(possibleJunctions);
            if (junction == null) {
                return null;
            } else {
                this.animal.state = new VirtualAnimalState.StateFollow(this.animal);
                return junction;
            }
        }
    }

    public static class StateMoveToEat extends VirtualAnimalState {
        public StateMoveToEat(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.animal.currentZoneAction, GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
        }

        @Override
        public void reachedEnd() {
            this.animal.state = new VirtualAnimalState.StateEat(this.animal);
        }
    }

    public static class StateMoveToSleep extends VirtualAnimalState {
        public StateMoveToSleep(VirtualAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.animal.currentZoneAction, GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
        }

        @Override
        public void reachedEnd() {
            this.animal.state = new VirtualAnimalState.StateSleep(this.animal);
        }
    }

    public static class StateSleep extends VirtualAnimalState {
        public StateSleep(VirtualAnimal animal) {
            super(animal);
            animal.wakeTime = GameTime.getInstance().getWorldAgeHours() + this.animal.timeToSleep / 60.0F;
        }

        @Override
        public void update() {
            this.addAnimalTracks(AnimalTracksDefinitions.getRandomTrack(this.animal.migrationGroup, "sleep"));
            double worldAgeHours = GameTime.getInstance().getWorldAgeHours();
            if (worldAgeHours > this.animal.wakeTime) {
                this.animal.debugForceSleep = false;
                this.animal.state = new VirtualAnimalState.StateMoveFromSleep(this.animal);
                this.animal.nextRestTime = -1.0;
            }
        }
    }
}
