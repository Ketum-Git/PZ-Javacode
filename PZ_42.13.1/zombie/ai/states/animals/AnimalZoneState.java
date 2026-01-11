// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

import java.util.ArrayList;
import java.util.HashMap;
import org.joml.Vector2f;
import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.AnimalZone;
import zombie.characters.animals.AnimalZoneJunction;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.util.list.PZArrayUtil;

public final class AnimalZoneState extends State {
    private static final int PARAMETER_ACTION = 0;
    private static final int PARAMETER_STATE = 1;
    private static final AnimalZoneState _instance = new AnimalZoneState();

    public static AnimalZoneState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.put(0, ((IsoAnimal)owner).getAnimalZone().getAction());
        StateMachineParams.put(1, new AnimalZoneState.StateFollow((IsoAnimal)owner));
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        AnimalZoneState.ZoneState zoneState = (AnimalZoneState.ZoneState)StateMachineParams.get(1);
        zoneState.update();
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
    }

    @Override
    public boolean isMoving(IsoGameCharacter owner) {
        return owner.isMoving();
    }

    public static class StateEat extends AnimalZoneState.ZoneState {
        double startTime = GameTime.getInstance().getWorldAgeHours();

        public StateEat(IsoAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            double worldAgeHours = GameTime.getInstance().getWorldAgeHours();
            if (worldAgeHours - this.startTime > 0.03333333333333333) {
                this.setState(new AnimalZoneState.StateMoveFromEat(this.animal));
            }
        }
    }

    public static class StateFollow extends AnimalZoneState.ZoneState {
        double nextRestTime = -1.0;

        public StateFollow(IsoAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.getCurrentZoneAction(), GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
            if ("Eat".equals(this.getCurrentZoneAction())) {
                this.setState(new AnimalZoneState.StateMoveToEat(this.animal));
            } else {
                if (this.nextRestTime < 0.0) {
                    this.nextRestTime = GameTime.getInstance().getWorldAgeHours() + Rand.Next(2, 5) / 60.0;
                }

                if (GameTime.getInstance().getWorldAgeHours() > this.nextRestTime) {
                    this.setState(new AnimalZoneState.StateSleep(this.animal));
                }
            }
        }

        @Override
        public AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
            possibleJunctions.clear();

            for (int i = 0; i < junctions.size(); i++) {
                AnimalZoneJunction junction = junctions.get(i);
                if ("Eat".equals(junction.zoneOther.getAction())) {
                    return junction;
                }

                if ("Follow".equals(junction.zoneOther.getAction())) {
                    possibleJunctions.add(junction);
                }
            }

            return "Follow".equals(zone.getAction()) && !bEndPoint && Rand.NextBool(possibleJunctions.size() + 1)
                ? null
                : PZArrayUtil.pickRandom(possibleJunctions);
        }
    }

    public static class StateMoveFromEat extends AnimalZoneState.ZoneState {
        public StateMoveFromEat(IsoAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.getCurrentZoneAction(), GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
        }

        @Override
        public AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
            possibleJunctions.clear();

            for (int i = 0; i < junctions.size(); i++) {
                AnimalZoneJunction junction = junctions.get(i);
                if ("Follow".equals(junction.zoneOther.getAction())) {
                    possibleJunctions.add(junction);
                }
            }

            AnimalZoneJunction junction = PZArrayUtil.pickRandom(possibleJunctions);
            if (junction == null) {
                return null;
            } else {
                this.setState(new AnimalZoneState.StateFollow(this.animal));
                return junction;
            }
        }
    }

    public static class StateMoveToEat extends AnimalZoneState.ZoneState {
        public StateMoveToEat(IsoAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            this.moveAlongPath(this.getCurrentZoneAction(), GameTime.getInstance().getThirtyFPSMultiplier() * 0.2F);
        }

        @Override
        public void reachedEnd() {
            this.setState(new AnimalZoneState.StateEat(this.animal));
        }
    }

    public static class StateSleep extends AnimalZoneState.ZoneState {
        double wakeTime = GameTime.getInstance().getWorldAgeHours() + Rand.Next(1, 3) / 60.0;

        public StateSleep(IsoAnimal animal) {
            super(animal);
        }

        @Override
        public void update() {
            double worldAgeHours = GameTime.getInstance().getWorldAgeHours();
            if (worldAgeHours > this.wakeTime) {
                this.setCurrentZoneAction("Follow");
                this.setState(new AnimalZoneState.StateFollow(this.animal));
            }
        }
    }

    private abstract static class ZoneState {
        protected static final ArrayList<AnimalZoneJunction> tempJunctions = new ArrayList<>();
        protected static final ArrayList<AnimalZoneJunction> possibleJunctions = new ArrayList<>();
        IsoAnimal animal;

        ZoneState(IsoAnimal animal) {
            this.animal = animal;
        }

        String getCurrentZoneAction() {
            HashMap<Object, Object> StateMachineParams = this.animal.getStateMachineParams(AnimalZoneState.instance());
            return (String)StateMachineParams.get(0);
        }

        void setCurrentZoneAction(String action) {
            HashMap<Object, Object> StateMachineParams = this.animal.getStateMachineParams(AnimalZoneState.instance());
            StateMachineParams.put(0, action);
        }

        void setState(AnimalZoneState.ZoneState state) {
            HashMap<Object, Object> StateMachineParams = this.animal.getStateMachineParams(AnimalZoneState.instance());
            StateMachineParams.put(1, state);
        }

        public abstract void update();

        protected AnimalZoneJunction visitJunction(AnimalZone zone, boolean bEndPoint, ArrayList<AnimalZoneJunction> junctions) {
            return null;
        }

        protected void reachedEnd() {
        }

        void moveAlongPath(String action, float distance) {
            AnimalZone closestZone = this.animal.getAnimalZone();
            if (closestZone == null) {
                this.animal.setAnimalZone(null);
            } else {
                this.moveAlongPath(distance, closestZone);
            }
        }

        void moveAlongPath(float distance, AnimalZone zone) {
            Vector2f pos = new Vector2f();
            float t = zone.getClosestPointOnPolyline(this.animal.getX(), this.animal.getY(), pos);
            float zoneLength = zone.getPolylineLength();
            boolean moveForwardOnZone = this.animal.isMoveForwardOnZone();
            float t2 = moveForwardOnZone ? t + distance / zoneLength : t - distance / zoneLength;
            zone.getJunctionsBetween(t, t2, tempJunctions);
            if (!tempJunctions.isEmpty()) {
                AnimalZoneJunction junction = this.visitJunction(
                    zone, tempJunctions.get(0).isFirstPointOnZone1() || tempJunctions.get(0).isLastPointOnZone1(), tempJunctions
                );
                if (junction != null) {
                    zone = junction.zoneOther;
                    t2 = zone.getDistanceOfPointFromStart(junction.pointIndexOther) / zone.getPolylineLength();
                    this.animal.setMoveForwardOnZone(junction.isFirstPointOnZone2() || !junction.isLastPointOnZone2() && Rand.NextBool(2));
                    t2 = this.animal.isMoveForwardOnZone() ? t2 + 1.0F / zone.getPolylineLength() : t2 - 1.0F / zone.getPolylineLength();
                    this.setCurrentZoneAction(zone.getAction());
                }
            }

            if (t2 >= 1.0F && moveForwardOnZone) {
                t2 = 1.0F;
                this.animal.setMoveForwardOnZone(false);
                this.reachedEnd();
            }

            if (t2 <= 0.0F && moveForwardOnZone) {
                t2 = 0.0F;
                this.animal.setMoveForwardOnZone(true);
                this.reachedEnd();
            }

            zone.getPointOnPolyline(t2, pos);
            this.animal.setNextX(pos.x);
            this.animal.setNextY(pos.y);
            zone.getDirectionOnPolyline(t2, pos);
            if (!this.animal.isMoveForwardOnZone()) {
                pos.mul(-1.0F);
            }

            this.animal.setForwardDirection(pos.x, pos.y);
            this.animal.setAnimalZone(zone);
        }
    }
}
