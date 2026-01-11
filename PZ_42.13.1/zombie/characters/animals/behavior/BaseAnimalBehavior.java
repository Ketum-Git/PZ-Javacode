// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.ai.states.animals.AnimalIdleState;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.Position3D;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandles;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.RainManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public class BaseAnimalBehavior {
    private static final Vector2 tempVector2 = new Vector2();
    protected IsoAnimal parent;
    public float wanderMulMod;
    public boolean blockMovement;
    private float goToMomTimer;
    public int sitInTime;
    public int sitOutTime;
    public float blockedFor;
    public float attackAnimalTimer;
    private int followChrTimer;
    public float lastAlerted;
    private float idleAnimTimer;
    public float behaviorCheckTimer;
    public BehaviorAction behaviorAction;
    public Object behaviorObject;
    public boolean isDoingBehavior;
    public float behaviorMaxTime = 5000.0F;
    public float behaviorFailsafe;
    public int hutchPathTimer = -1;
    public int enterHutchTimerAfterDestroy;
    public long forcedOutsideHutch;
    private float timerFleeAgain;
    private boolean wildAndHurt;
    private float wildDropDeadTimer;

    public BaseAnimalBehavior(IsoAnimal parent) {
        this.parent = parent;
    }

    public void wanderIdle() {
        if (this.blockMovement) {
            this.blockedFor = this.blockedFor + GameTime.getInstance().getMultiplier();
            if (this.blockedFor >= 8000.0F) {
                this.blockMovement = false;
                this.blockedFor = 0.0F;
            }
        }

        if (!this.isDoingBehavior
            && !this.blockMovement
            && !this.parent.isAlerted()
            && !this.parent.getData().goingToMom
            && !this.parent.isAnimalEating()
            && !this.parent.isAnimalMoving()
            && this.parent.soundSourceTarget == null
            && !this.parent.walkToCharLuring
            && this.parent.drinkFromTrough == null
            && this.parent.eatFromTrough == null
            && this.parent.eatFromGround == null) {
            if (this.parent.isOnFire() && !this.parent.isAnimalMoving()) {
                this.parent.setVariable(AnimationVariableHandles.animalRunning, true);
                this.parent
                    .pathToLocation(
                        Rand.Next(this.parent.getCurrentSquare().x - 10, this.parent.getCurrentSquare().x + 10),
                        Rand.Next(this.parent.getCurrentSquare().y - 10, this.parent.getCurrentSquare().y + 10),
                        this.parent.getCurrentSquare().getZ()
                    );
            } else {
                if (this.parent.getStateEventDelayTimer() < -1000.0F && this.parent.isAnimalMoving()) {
                    this.parent.setStateEventDelayTimer(0.0F);
                    this.parent.stopAllMovementNow();
                }

                if (this.parent.adef.sitRandomly) {
                    this.checkSit();
                }

                if (this.parent.isAnimalSitting()) {
                    if (Rand.NextBool(this.parent.adef.idleEmoteChance)
                        && StringUtils.isNullOrEmpty(this.parent.getVariableString("sittingAnim"))
                        && this.parent.adef.sittingTypeNbr > 0) {
                        this.parent.setVariable("sittingAnim", "sit" + Rand.Next(1, this.parent.adef.sittingTypeNbr + 1));
                    }
                } else {
                    if (this.parent.mother != null
                        && this.parent.mother.getCurrentSquare() != null
                        && this.parent.canGoThere(this.parent.mother.getCurrentSquare())) {
                        if (this.goToMomTimer > 0.0F) {
                            this.goToMomTimer = this.goToMomTimer - GameTime.getInstance().getMultiplier();
                        } else {
                            float dist = -1.0F;
                            if (this.parent.getCurrentSquare() != null && this.parent.mother.getCurrentSquare() != null) {
                                dist = this.parent.getCurrentSquare().DistToProper(this.parent.mother.getCurrentSquare());
                            }

                            if (dist >= 6.0F && this.parent.getCurrentState() == AnimalIdleState.instance()) {
                                this.parent.setVariable(AnimationVariableHandles.animalRunning, true);
                                this.goToMomTimer = 300.0F;
                                this.parent
                                    .pathToLocation(
                                        this.parent.mother.getCurrentSquare().x,
                                        this.parent.mother.getCurrentSquare().y,
                                        this.parent.mother.getCurrentSquare().getZ()
                                    );
                                return;
                            }
                        }
                    }

                    if (this.idleAnimTimer > 0.0F) {
                        this.idleAnimTimer = this.idleAnimTimer - GameTime.getInstance().getMultiplier();
                        if (this.idleAnimTimer < 0.0F) {
                            this.idleAnimTimer = 0.0F;
                        }
                    }

                    if (Rand.NextBool(this.parent.adef.idleEmoteChance)
                        && StringUtils.isNullOrEmpty(this.parent.getVariableString("idleAction"))
                        && this.idleAnimTimer == 0.0F) {
                        if (this.parent.stressLevel < 10.0F && this.parent.adef.happyAnim > 0 && Rand.NextBool(3)) {
                            this.parent.setVariable("idleAction", "happy" + Rand.Next(1, this.parent.adef.happyAnim + 1));
                        } else if (this.parent.adef.idleTypeNbr > 0) {
                            this.parent.setVariable("idleAction", "idle" + Rand.Next(1, this.parent.adef.idleTypeNbr + 1));
                        }

                        this.idleAnimTimer = 1500.0F;
                    }

                    if (this.parent.movingToFood != null) {
                        this.parent.setStateEventDelayTimer(this.pickRandomWanderInterval());
                    }

                    if (this.parent.getStateEventDelayTimer() <= 0.0F) {
                        this.checkBehavior();
                        this.parent.setVariable(AnimationVariableHandles.animalRunning, false);
                        int xRef = (int)this.parent.getX();
                        int yRef = (int)this.parent.getY();
                        if (this.parent.getData().getAttachedPlayer() != null) {
                            xRef = PZMath.fastfloor(this.parent.getData().getAttachedPlayer().getX());
                            yRef = PZMath.fastfloor(this.parent.getData().getAttachedPlayer().getY());
                        } else if (this.parent.getData().getAttachedTree() != null) {
                            xRef = this.parent.getData().getAttachedTreeX();
                            yRef = this.parent.getData().getAttachedTreeY();
                        } else if (this.parent.mother != null && this.parent.mother.getCurrentSquare() != null) {
                            xRef = this.parent.mother.getCurrentSquare().getX();
                            yRef = this.parent.mother.getCurrentSquare().getY();
                        }

                        this.parent.setStateEventDelayTimer(this.pickRandomWanderInterval());
                        if (this.parent.getStats().get(CharacterStat.HUNGER) > 0.3F) {
                            this.parent.setStateEventDelayTimer(200.0F);
                        }

                        int x = xRef + Rand.Next(16) - 8;
                        int y = yRef + Rand.Next(16) - 8;
                        if (this.parent.getDZone() != null
                            && this.parent.getStats().get(CharacterStat.HUNGER) < 0.9F
                            && this.parent.getStats().get(CharacterStat.THIRST) < 0.9F) {
                            DesignationZoneAnimal zone = DesignationZoneAnimal.getZone(x, y, this.parent.getCurrentSquare().getZ());

                            for (int tries = 0; zone == null && tries < 100; tries++) {
                                x = xRef + Rand.Next(16) - 8;
                                y = yRef + Rand.Next(16) - 8;
                                zone = DesignationZoneAnimal.getZone(x, y, this.parent.getCurrentSquare().getZ());
                            }

                            if (RainManager.isRaining() && RainManager.getRainIntensity() > 0.05) {
                                ArrayList<DesignationZoneAnimal> roofedZone = new ArrayList<>();

                                for (int j = 0; j < this.parent.getConnectedDZone().size(); j++) {
                                    DesignationZoneAnimal zoneRoof = this.parent.getConnectedDZone().get(j);
                                    if (!zoneRoof.getRoofAreas().isEmpty() && !roofedZone.contains(zoneRoof)) {
                                        roofedZone.add(zoneRoof);
                                    }
                                }

                                if (!roofedZone.isEmpty()) {
                                    DesignationZoneAnimal zoneRoof = roofedZone.get(Rand.Next(0, roofedZone.size()));
                                    Position3D pos = zoneRoof.getRoofAreas().get(Rand.Next(0, zoneRoof.getRoofAreas().size()));
                                    x = (int)pos.x;
                                    y = (int)pos.y;
                                }
                            }
                        }

                        if (this.parent.getCell().getGridSquare(x, y, this.parent.getZi()) != null
                            && this.parent.getCell().getGridSquare(x, y, this.parent.getZi()).isFree(true)) {
                            if (this.parent.adef.periodicRun && Rand.NextBool(5)) {
                                this.parent.setVariable(AnimationVariableHandles.animalRunning, true);
                            }

                            this.parent.pathToLocation(x, y, this.parent.getZi());
                        }
                    }
                }
            }
        }
    }

    public void walkedOnSpot() {
        this.parent.setMoving(false);
        this.parent.getPathFindBehavior2().reset();
        this.parent.setStateEventDelayTimer(200.0F);
    }

    public void goAttack(IsoGameCharacter fightingOpponent) {
        if (!this.blockMovement && BehaviorAction.FIGHTANIMAL != this.behaviorAction) {
            if (fightingOpponent != null
                && !fightingOpponent.isDead()
                && fightingOpponent.getCurrentSquare() != null
                && fightingOpponent.isExistInTheWorld()
                && fightingOpponent.getVehicle() == null) {
                this.behaviorAction = BehaviorAction.FIGHTANIMAL;
                this.behaviorObject = fightingOpponent;
                this.setDoingBehavior(true);
                this.parent.stopAllMovementNow();
                this.parent.pathToCharacter(fightingOpponent);
                if (fightingOpponent instanceof IsoAnimal isoAnimal) {
                    isoAnimal.fightingOpponent = this.parent;
                    isoAnimal.getBehavior().blockMovement = true;
                }
            } else {
                this.parent.fightingOpponent = null;
                this.attackAnimalTimer = 0.0F;
            }
        }
    }

    public void checkSit() {
        if (this.parent.getStats().get(CharacterStat.HUNGER) > 0.2F || this.parent.getData().getAttachedPlayer() != null) {
            this.parent.clearVariable("idleAction");
            this.sitInTime = 0;
            this.sitOutTime = 0;
        }

        if (this.parent.getCurrentSquare() != null) {
            if (RainManager.isRaining() && RainManager.getRainIntensity() > 0.05F && !this.parent.getCurrentSquare().haveRoof) {
                this.parent.clearVariable("idleAction");
                this.sitInTime = 0;
                this.sitOutTime = 0;
            }

            if (!(this.parent.getStats().get(CharacterStat.HUNGER) > 0.3F) && this.parent.getData().getAttachedPlayer() == null) {
                if (!RainManager.isRaining() || this.parent.getCurrentSquare() == null || this.parent.getCurrentSquare().haveRoof) {
                    if (this.sitInTime == 0 && this.sitOutTime == 0) {
                        this.sitInTime = Long.valueOf(GameTime.instance.getCalender().getTimeInMillis() / 1000L).intValue() + Rand.Next(10800, 25200);
                        this.sitOutTime = this.sitInTime + Rand.Next(3600, 7200);
                    }

                    if (this.sitInTime > 0 && GameTime.instance.getCalender().getTimeInMillis() / 1000L > this.sitInTime) {
                        this.parent.setVariable("idleAction", "sit");
                        this.sitInTime = 0;
                    }

                    if (this.sitOutTime > 0 && GameTime.instance.getCalender().getTimeInMillis() / 1000L > this.sitOutTime) {
                        this.parent.clearVariable("idleAction");
                        this.sitOutTime = 0;
                    }
                }
            }
        }
    }

    public float pickRandomWanderInterval() {
        float interval = Rand.Next(this.parent.adef.wanderMul - this.wanderMulMod, (this.parent.adef.wanderMul - this.wanderMulMod) * 5.0F);
        if (ServerOptions.getInstance().ultraSpeedDoesnotAffectToAnimals.getValue() && GameTime.getInstance().getMultiplier() > 60.0F) {
            interval *= 10.0F;
        }

        if (this.parent.geneticDisorder.contains("fidget")) {
            interval /= 10.0F;
        }

        if (RainManager.isRaining()) {
            interval *= 3.0F;
        }

        return interval;
    }

    public void updateAttackTimer() {
        if (this.attackAnimalTimer > 0.0F) {
            this.attackAnimalTimer = this.attackAnimalTimer - GameTime.getInstance().getMultiplier();
            if (this.attackAnimalTimer < 0.0F) {
                this.attackAnimalTimer = 0.0F;
            }
        }
    }

    public void update() {
        this.fleeFromAttacker();
        this.fleeFromChr();
        this.followChr();
        this.updateAttackTimer();
        this.updateAcceptance();
        this.updateGoingToHutch();
        if (this.behaviorCheckTimer > 150.0F
            && (this.parent.getStats().get(CharacterStat.HUNGER) > 0.5F || this.parent.getStats().get(CharacterStat.THIRST) > 0.5F)) {
            this.behaviorCheckTimer = 150.0F;
        }

        if (this.behaviorCheckTimer > 0.0F) {
            this.behaviorCheckTimer = this.behaviorCheckTimer - GameTime.getInstance().getMultiplier();
            if (this.behaviorCheckTimer < 0.0F) {
                this.behaviorCheckTimer = 0.0F;
            }
        } else {
            this.behaviorCheckTimer = 500.0F;
            this.checkBehavior();
        }

        if (this.isDoingBehavior) {
            this.behaviorFailsafe = this.behaviorFailsafe + GameTime.getInstance().getMultiplier();
            if (this.behaviorFailsafe > this.behaviorMaxTime) {
                this.behaviorFailsafe = 0.0F;
                this.setDoingBehavior(false);
                this.behaviorAction = null;
                this.behaviorObject = null;
            }
        }
    }

    private void updateGoingToHutch() {
        if (this.isDoingBehavior && BehaviorAction.ENTERHUTCH == this.behaviorAction) {
            IsoHutch hutchToEnter = (IsoHutch)this.behaviorObject;
            if (this.hutchPathTimer > -1 && hutchToEnter != null) {
                this.hutchPathTimer = (int)(this.hutchPathTimer - GameTime.getInstance().getMultiplier());
                if (this.hutchPathTimer <= 0 && hutchToEnter != null) {
                    this.parent.stopAllMovementNow();
                    this.parent
                        .pathToLocation(
                            hutchToEnter.getSquare().x + hutchToEnter.getEnterSpotX(),
                            hutchToEnter.getSquare().y + hutchToEnter.getEnterSpotY(),
                            hutchToEnter.getSquare().getZ()
                        );
                    this.hutchPathTimer = -1;
                }
            }
        }
    }

    public void doBehaviorAction() {
        if (this.isDoingBehavior) {
            this.setDoingBehavior(false);
            if (this.behaviorAction == null) {
                System.out.println("action was null, object: " + this.behaviorObject);
                return;
            }

            switch (this.behaviorAction) {
                case EATTROUGH:
                    this.eatFromTrough();
                    break;
                case EATGROUND:
                    this.eatFromGround();
                    break;
                case DRINKGROUND:
                    this.drinkFromGround();
                    break;
                case EATMOM:
                    this.eatFromMom();
                    break;
                case DRINK:
                    this.drinkFromTrough();
                    break;
                case FERTILIZE:
                    this.fertilize();
                    break;
                case ENTERHUTCH:
                    this.enterHutch();
                    break;
                case FIGHTANIMAL:
                    this.fightAnimal();
                    break;
                case DRINKFROMRIVER:
                    this.drinkFromRiver();
                    break;
                case DRINKFROMPUDDLE:
                    this.drinkFromPuddle();
                    break;
                case EATGRASS:
                    this.eatGrass();
            }

            this.resetBehaviorAction();
        }

        this.timerFleeAgain = 0.0F;
    }

    public void fightAnimal() {
        if (this.parent.fightingOpponent != null
            && !this.parent.fightingOpponent.isDead()
            && this.parent.fightingOpponent.getCurrentSquare() != null
            && this.parent.fightingOpponent.isExistInTheWorld()) {
            if (this.parent.fightingOpponent.DistToProper(this.parent) <= this.parent.adef.attackDist) {
                this.parent.stopAllMovementNow();
                this.parent.faceThisObject(this.parent.fightingOpponent);
                this.parent.atkTarget = this.parent.fightingOpponent;
                this.attackAnimalTimer = this.parent.adef.attackTimer;
                if (this.parent.fightingOpponent instanceof IsoAnimal opponent) {
                    opponent.atkTarget = this.parent;
                    opponent.getBehavior().attackAnimalTimer = opponent.adef.attackTimer;
                    opponent.getBehavior().blockMovement = true;
                } else {
                    this.attackAnimalTimer = 1500.0F;
                }
            }

            if (this.parent.fightingOpponent instanceof IsoAnimal opponent) {
                opponent.fightingOpponent = null;
            }

            this.parent.fightingOpponent = null;
        } else {
            if (this.parent.fightingOpponent instanceof IsoAnimal opponent) {
                opponent.fightingOpponent = null;
            }

            this.parent.fightingOpponent = null;
        }
    }

    private void enterHutch() {
        IsoHutch hutchToEnter = (IsoHutch)this.behaviorObject;
        if (hutchToEnter != null
            && hutchToEnter.isOpen()
            && hutchToEnter.getSquare() != null
            && this.parent.getCurrentSquare() != null
            && hutchToEnter.getAnimalInside().size() < hutchToEnter.getMaxAnimals()
            && hutchToEnter.getSquare().DistToProper(this.parent.getCurrentSquare()) < 2.0F
            && hutchToEnter.addAnimalInside(this.parent)) {
            hutchToEnter.animalOutside.remove(this.parent);
            this.parent.removeFromWorld();
            this.parent.removeFromSquare();
        }
    }

    public void resetBehaviorAction() {
        this.parent.setTurnDelta(this.parent.adef.turnDelta);
        this.behaviorAction = null;
        this.behaviorObject = null;
        this.parent.fightingOpponent = null;
    }

    private void fertilize() {
        IsoAnimal female = (IsoAnimal)this.behaviorObject;
        female.getBehavior().blockMovement = false;
        if (female.getSquare() != null && this.parent.getCurrentSquare() != null && female.getSquare().DistToProper(this.parent.getCurrentSquare()) >= 3.0F) {
            this.resetBehaviorAction();
        } else {
            female.fertilize(this.parent, false);
        }
    }

    private void drinkFromTrough() {
        IsoFeedingTrough trough = (IsoFeedingTrough)this.behaviorObject;
        if (this.parent.getCurrentSquare() != null && this.parent.getCurrentSquare().DistToProper(trough.getSquare()) > this.parent.adef.distToEat) {
            this.parent.ignoredTrough.add(trough);
        } else {
            this.parent.setVariable("idleAction", "eat");
            if (this.parent.adef.eatingTypeNbr > 0) {
                this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
            }

            this.parent.drinkFromTrough = trough;
        }
    }

    private void eatFromTrough() {
        IsoFeedingTrough trough = (IsoFeedingTrough)this.behaviorObject;
        if (this.parent.getCurrentSquare() != null
            && trough.getSquare() != null
            && this.parent.getCurrentSquare().DistToProper(trough.getSquare()) > this.parent.adef.distToEat) {
            this.parent.ignoredTrough.add(trough);
        } else {
            this.parent.setVariable("idleAction", "eat");
            if (this.parent.adef.eatingTypeNbr > 0) {
                this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
            }

            this.parent.eatFromTrough = trough;
        }
    }

    private void eatFromGround() {
        IsoWorldInventoryObject food = (IsoWorldInventoryObject)this.behaviorObject;
        if (this.parent.getCurrentSquare() == null
            || food.getSquare() == null
            || !(this.parent.getCurrentSquare().DistToProper(food.getSquare()) > this.parent.adef.distToEat)) {
            this.parent.setVariable("idleAction", "eat");
            if (this.parent.adef.eatingTypeNbr > 0) {
                this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
            }

            this.parent.eatFromGround = food;
        }
    }

    private void drinkFromGround() {
        IsoWorldInventoryObject food = (IsoWorldInventoryObject)this.behaviorObject;
        if (this.parent.getCurrentSquare() == null
            || food.getSquare() == null
            || !(this.parent.getCurrentSquare().DistToProper(food.getSquare()) > this.parent.adef.distToEat)) {
            this.parent.setVariable("idleAction", "eat");
            if (this.parent.adef.eatingTypeNbr > 0) {
                this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
            }

            this.parent.eatFromGround = food;
        }
    }

    private void clearIdleAction() {
        if (!StringUtils.isNullOrEmpty(this.parent.getVariableString("idleAction")) && this.parent.getVariableString("idleAction").startsWith("idle")) {
            this.parent.clearVariable("idleAction");
        }
    }

    public void checkBehavior() {
        if (!this.isDoingBehavior) {
            if (!this.blockMovement) {
                this.parent.fightingOpponent = null;
            }

            this.clearIdleAction();
            if (this.parent.getVehicle() != null) {
                this.setDoingBehavior(false);
                this.parent.luredBy = null;
                this.blockMovement = false;
                this.parent.clearVariable("idleAction");
            }

            if ((this.parent.getVehicle() != null || this.parent.isExistInTheWorld())
                && this.parent.luredBy == null
                && !this.parent.getBehavior().blockMovement
                && !this.isDoingBehavior
                && StringUtils.isNullOrEmpty(this.parent.getVariableString("idleAction"))) {
                if (this.checkDrinkBehavior()) {
                    if (this.parent.getVehicle() == null) {
                        this.setDoingBehavior(true);
                        this.parent.setTurnDelta(0.9F);
                    }
                } else if (this.checkEatBehavior()) {
                    if (this.parent.getVehicle() == null) {
                        this.setDoingBehavior(true);
                        this.parent.setTurnDelta(0.9F);
                    }
                } else if (this.checkFertilizeFemale()) {
                    if (this.parent.getVehicle() == null) {
                        this.setDoingBehavior(true);
                        this.parent.setTurnDelta(0.9F);
                    }
                } else if (this.callToHutch(null, false)) {
                    if (this.parent.getVehicle() == null) {
                        this.setDoingBehavior(true);
                        this.parent.setTurnDelta(0.9F);
                    }
                } else if (this.checkAttackBehavior()) {
                    if (this.parent.getVehicle() == null) {
                        this.setDoingBehavior(true);
                        this.parent.setTurnDelta(0.9F);
                    }
                }
            }
        }
    }

    private boolean checkAttackBehavior() {
        if (this.parent.isFemale() || this.parent.isBaby()) {
            return false;
        } else if (this.parent.adef.dontAttackOtherMale) {
            return false;
        } else if (!this.parent.canDoAction() || this.parent.hasHitReaction()) {
            return false;
        } else if (this.attackAnimalTimer > 0.0F) {
            return false;
        } else if (this.parent.isInMatingSeason() && this.parent.getAge() >= this.parent.getMinAgeForBaby()) {
            for (int j = 0; j < this.parent.getConnectedDZone().size(); j++) {
                DesignationZoneAnimal zone = this.parent.getConnectedDZone().get(j);
                ArrayList<IsoAnimal> animals = zone.getAnimalsConnected();

                for (int i = 0; i < animals.size(); i++) {
                    IsoAnimal animal = animals.get(i);
                    if (!animal.isFemale()
                        && animal != this.parent
                        && animal.fightingOpponent == null
                        && animal.getAnimalType().equalsIgnoreCase(this.parent.getAnimalType())
                        && animal.getAge() >= animal.getMinAgeForBaby()
                        && !animal.getBehavior().isDoingBehavior
                        && !animal.isAnimalEating()) {
                        this.goAttack(animal);
                        this.parent.fightingOpponent = animal;
                        return true;
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public boolean callToHutch(IsoHutch hutch, boolean force) {
        if (this.enterHutchTimerAfterDestroy > 0) {
            this.enterHutchTimerAfterDestroy = (int)(this.enterHutchTimerAfterDestroy - GameTime.getInstance().getMultiplier());
            if (this.enterHutchTimerAfterDestroy < 0) {
                this.enterHutchTimerAfterDestroy = 0;
            }

            return false;
        } else if (force
            || this.parent.isExistInTheWorld()
                && this.parent.adef.enterHutchTime != 0
                && this.parent.adef.exitHutchTime != 0
                && this.parent.adef.hutches != null
                && this.parent.adef.isInsideHutchTime(null)
                && this.parent.hutch == null) {
            boolean list = false;
            if (hutch == null) {
                hutch = this.parent.getData().getRegionHutch();
                list = true;
            }

            if (hutch == null) {
                return false;
            } else {
                if (force) {
                    this.forcedOutsideHutch = GameTime.getInstance().getCalender().getTimeInMillis() + 7200001L;
                }

                if (!list) {
                    return this.canGoToHutch(hutch, force);
                } else {
                    for (int k = 0; k < this.parent.getConnectedDZone().size(); k++) {
                        DesignationZoneAnimal zone = this.parent.getConnectedDZone().get(k);

                        for (int i = 0; i < zone.hutchs.size(); i++) {
                            hutch = zone.hutchs.get(i);
                            if (this.canGoToHutch(hutch, force)) {
                                return true;
                            }
                        }
                    }

                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public boolean canGoToHutch(IsoHutch hutch, boolean force) {
        if (hutch.getAnimalInside().size() < hutch.getMaxAnimals()) {
            if (force && !hutch.isOpen()) {
                hutch.toggleDoor();
            }

            if (hutch.isOpen()) {
                if (this.hutchPathTimer <= -1) {
                    if (this.parent.isBaby()) {
                        this.hutchPathTimer = Rand.Next(10, 30);
                    } else if (this.parent.isFemale()) {
                        this.hutchPathTimer = Rand.Next(350, 500);
                    } else {
                        this.hutchPathTimer = Rand.Next(950, 1000);
                    }
                }

                if (force) {
                    this.hutchPathTimer = 1;
                }

                this.setDoingBehavior(true);
                this.behaviorAction = BehaviorAction.ENTERHUTCH;
                this.behaviorObject = hutch;
                return true;
            }
        }

        return false;
    }

    private boolean checkFertilizeFemale() {
        this.parent.getData().findFemaleToInseminate(null);
        if (!this.parent.getData().animalToInseminate.isEmpty()) {
            IsoAnimal femaleToCheck = this.parent.getData().animalToInseminate.get(Rand.Next(0, this.parent.getData().animalToInseminate.size()));
            femaleToCheck.getBehavior().blockMovement = true;
            femaleToCheck.stopAllMovementNow();
            this.parent.stopAllMovementNow();
            this.behaviorAction = BehaviorAction.FERTILIZE;
            this.behaviorObject = femaleToCheck;
            this.parent.getData().animalToInseminate.remove(femaleToCheck);
            this.parent.pathToCharacter(femaleToCheck);
            return true;
        } else {
            return false;
        }
    }

    private void drinkFromRiver() {
        IsoGridSquare riverSq = (IsoGridSquare)this.behaviorObject;
        if (this.parent.getCurrentSquare() == null || riverSq == null || !(this.parent.getCurrentSquare().DistToProper(riverSq) > this.parent.adef.distToEat)) {
            this.parent.setVariable("idleAction", "eat");
            if (this.parent.adef.eatingTypeNbr > 0) {
                this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
            }

            this.parent.drinkFromRiver = riverSq;
        }
    }

    private void eatGrass() {
        this.parent.stopAllMovementNow();
        this.parent.setVariable("idleAction", "eat");
        if (this.parent.adef.eatingTypeNbr > 0) {
            this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
        }

        this.parent.getBehavior().wanderMulMod = 0.0F;
        this.parent.getData().eatingGrass = true;
    }

    private void drinkFromPuddle() {
        IsoGridSquare puddleFloor = (IsoGridSquare)this.behaviorObject;
        if (this.parent.getCurrentSquare() == null
            || puddleFloor == null
            || !(this.parent.getCurrentSquare().DistToProper(puddleFloor) > this.parent.adef.distToEat)) {
            this.parent.setVariable("idleAction", "eat");
            if (this.parent.adef.eatingTypeNbr > 0) {
                this.parent.setVariable("eatingAnim", "eat" + Rand.Next(1, this.parent.adef.eatingTypeNbr + 1));
            }

            this.parent.drinkFromPuddle = puddleFloor;
        }
    }

    private IsoGridSquare getRandomRiverSq() {
        ArrayList<IsoGridSquare> allSqRiver = this.parent.getDZone().getNearWaterSquaresConnected();
        if (allSqRiver.isEmpty()) {
            return null;
        } else {
            shuffleListSq(allSqRiver);
            return allSqRiver.get(0);
        }
    }

    public static void shuffleListSq(ArrayList<IsoGridSquare> a) {
        int n = a.size();
        Random random = new Random();
        random.nextInt();

        for (int i = 0; i < n; i++) {
            int change = i + random.nextInt(n - i);
            swapSq(a, i, change);
        }
    }

    private static void swapSq(List<IsoGridSquare> a, int i, int change) {
        IsoGridSquare helper = a.get(i);
        a.set(i, a.get(change));
        a.set(change, helper);
    }

    public IsoGridSquare getNearestWaterSquare(IsoGridSquare sq) {
        for (int x2 = sq.getX() - 1; x2 < sq.getX() + 2; x2++) {
            for (int y2 = sq.getY() - 1; y2 < sq.getY() + 2; y2++) {
                IsoGridSquare sq2 = sq.getCell().getGridSquare(x2, y2, sq.z);
                if (sq2 != null) {
                    for (int i = 0; i < sq2.getObjects().size(); i++) {
                        IsoObject obj = sq2.getObjects().get(i);
                        if (obj.getProperties() != null && obj.getProperties().has(IsoFlagType.water)) {
                            return sq2;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean tryDrinkFromRiver() {
        if (this.parent.getDZone() == null) {
            return false;
        } else {
            IsoGridSquare nearRiverSq = this.getRandomRiverSq();
            if (nearRiverSq == null) {
                return false;
            } else {
                this.behaviorAction = BehaviorAction.DRINKFROMRIVER;
                IsoGridSquare riverSq = this.getNearestWaterSquare(nearRiverSq);
                if (riverSq == null) {
                    return false;
                } else {
                    this.behaviorObject = riverSq;
                    if (this.parent.getCurrentSquare() == null || !(nearRiverSq.DistToProper(this.parent.getCurrentSquare()) <= 1.0F)) {
                        this.parent.stopAllMovementNow();
                        this.parent.pathToLocation(nearRiverSq.x, nearRiverSq.y, nearRiverSq.z);
                    } else if (this.parent.getVehicle() == null) {
                        this.setDoingBehavior(true);
                        this.doBehaviorAction();
                    }

                    return true;
                }
            }
        }
    }

    private boolean checkDrinkBehavior() {
        if (!this.parent.isBaby()
            && !(this.parent.getStats().get(CharacterStat.THIRST) < this.parent.adef.thirstHungerTrigger)
            && this.parent.fightingOpponent == null) {
            if (this.tryDrinkFromRiver()) {
                return true;
            } else if (this.tryDrinkFromPuddle()) {
                return true;
            } else {
                for (int i = 0; i < this.parent.getConnectedDZone().size(); i++) {
                    DesignationZoneAnimal zone = this.parent.getConnectedDZone().get(i);
                    if (this.tryDrinkFromGround(zone)) {
                        return true;
                    }
                }

                return this.tryDrinkFromTrough();
            }
        } else {
            return false;
        }
    }

    public IsoObject tryAndGetPuddle(int searchRadius) {
        if (searchRadius <= 0) {
            searchRadius = this.parent.searchRadius;
        }

        if (this.parent.getSquare() == null) {
            return null;
        } else if (this.parent.getSquare().getPuddleFloor() != null) {
            return this.parent.getSquare().getPuddleFloor();
        } else {
            for (int x = this.parent.getSquare().x - searchRadius; x <= this.parent.getSquare().x + searchRadius; x++) {
                for (int y = this.parent.getSquare().y - searchRadius; y <= this.parent.getSquare().y + searchRadius; y++) {
                    IsoGridSquare sq = this.parent.getSquare().getCell().getGridSquare(x, y, this.parent.getSquare().getZ());
                    if (sq != null && this.parent.getConnectedDZone().contains(DesignationZoneAnimal.getZone(sq.x, sq.y, sq.z))) {
                        IsoObject puddleFloor = sq.getPuddleFloor();
                        if (puddleFloor != null) {
                            return puddleFloor;
                        }
                    }
                }
            }

            return null;
        }
    }

    public IsoObject tryAndGetGrassFloor() {
        if (this.parent.getSquare() == null) {
            return null;
        } else {
            if (this.parent.getSquare().getFloor() != null) {
                IsoObject floor = this.parent.getSquare().getFloor();
                if (floor != null && floor.getSprite().getProperties().has("grassFloor") && this.parent.getCurrentSquare().checkHaveGrass()) {
                    return floor;
                }
            }

            for (int x = this.parent.getSquare().x - this.parent.searchRadius; x <= this.parent.getSquare().x + this.parent.searchRadius; x++) {
                for (int y = this.parent.getSquare().y - this.parent.searchRadius; y <= this.parent.getSquare().y + this.parent.searchRadius; y++) {
                    IsoGridSquare sq = this.parent.getSquare().getCell().getGridSquare(x, y, this.parent.getSquare().getZ());
                    if (sq != null && this.parent.getConnectedDZone().contains(DesignationZoneAnimal.getZone(sq.x, sq.y, sq.z))) {
                        IsoObject floor = sq.getFloor();
                        if (floor != null && floor.getSprite().getProperties().has("grassFloor") && sq.checkHaveGrass()) {
                            return floor;
                        }
                    }
                }
            }

            return null;
        }
    }

    private boolean tryDrinkFromPuddle() {
        if (this.parent.getVehicle() == null && this.parent.getSquare() != null) {
            IsoObject puddleFloor = this.tryAndGetPuddle(0);
            if (puddleFloor == null) {
                if (this.parent.getStateEventDelayTimer() > 50.0F) {
                    this.parent.setStateEventDelayTimer(50.0F);
                }

                return false;
            } else {
                this.behaviorAction = BehaviorAction.DRINKFROMPUDDLE;
                this.behaviorObject = puddleFloor.getSquare();
                if (this.parent.getCurrentSquare() != null && puddleFloor.getSquare().DistToProper(this.parent.getCurrentSquare()) > 1.0F) {
                    this.parent.stopAllMovementNow();
                    this.parent.pathToLocation(puddleFloor.getSquare().getX(), puddleFloor.getSquare().getY(), puddleFloor.getSquare().getZ());
                } else if (this.parent.getVehicle() == null) {
                    this.setDoingBehavior(true);
                    this.doBehaviorAction();
                }

                return true;
            }
        } else {
            return false;
        }
    }

    private boolean tryDrinkFromGround(DesignationZoneAnimal zone) {
        if (!zone.foodOnGround.isEmpty()) {
            for (int j = 0; j < zone.foodOnGround.size(); j++) {
                IsoWorldInventoryObject food = zone.foodOnGround.get(j);
                if (food.isPureWater(true)) {
                    this.behaviorAction = BehaviorAction.DRINKGROUND;
                    this.behaviorObject = food;
                    if (food.getSquare() != null
                        && this.parent.getCurrentSquare() != null
                        && food.getSquare().DistToProper(this.parent.getCurrentSquare()) > 1.0F) {
                        this.parent.stopAllMovementNow();
                        this.parent.pathToLocation(food.getSquare().getX(), food.getSquare().getY(), food.getSquare().getZ());
                    } else if (this.parent.getVehicle() == null) {
                        this.setDoingBehavior(true);
                        this.doBehaviorAction();
                    }

                    return true;
                }
            }
        }

        return false;
    }

    private boolean tryDrinkFromTrough() {
        ArrayList<IsoFeedingTrough> allTrough = this.getRandomTroughList();

        for (int i = 0; i < allTrough.size(); i++) {
            IsoFeedingTrough trough = allTrough.get(i);
            if (!this.parent.ignoredTrough.contains(trough) && this.canDrinkFromTrough(trough)) {
                this.behaviorAction = BehaviorAction.DRINK;
                this.behaviorObject = trough;
                if (trough.getSquare() != null
                    && this.parent.getCurrentSquare() != null
                    && trough.getSquare().DistToProper(this.parent.getCurrentSquare()) > 1.0F) {
                    this.parent.stopAllMovementNow();
                    this.parent.pathToTrough(trough);
                } else if (this.parent.getVehicle() == null) {
                    this.setDoingBehavior(true);
                    this.doBehaviorAction();
                }

                return true;
            }
        }

        return false;
    }

    public boolean canDrinkFromTrough(IsoFeedingTrough trough) {
        return trough != null && !(trough.getWater() <= 0.0F);
    }

    public boolean canEatThis(InventoryItem item) {
        return !this.parent.adef.eatTypeTrough.contains("All")
                && !this.parent.adef.eatTypeTrough.contains(item.getFullType())
                && !this.parent.adef.eatTypeTrough.contains(item.getAnimalFeedType())
            ? item instanceof Food food && this.parent.adef.eatTypeTrough.contains(food.getFoodType())
            : true;
    }

    private boolean tryEatFromGround(DesignationZoneAnimal zone) {
        if (!zone.foodOnGround.isEmpty()) {
            for (int j = 0; j < zone.foodOnGround.size(); j++) {
                IsoWorldInventoryObject food = zone.foodOnGround.get(j);
                if (this.parent.adef.eatTypeTrough != null && this.canEatThis(food.getItem())) {
                    this.behaviorAction = BehaviorAction.EATGROUND;
                    this.behaviorObject = food;
                    if (food.getSquare() != null
                        && this.parent.getCurrentSquare() != null
                        && food.getSquare().DistToProper(this.parent.getCurrentSquare()) > 1.0F) {
                        this.parent.stopAllMovementNow();
                        this.parent.pathToLocation(food.getSquare().getX(), food.getSquare().getY(), food.getSquare().getZ());
                    } else {
                        this.setDoingBehavior(true);
                        this.doBehaviorAction();
                    }

                    return true;
                }
            }
        }

        return false;
    }

    public boolean checkEatBehavior() {
        if (!(this.parent.getStats().get(CharacterStat.HUNGER) < this.parent.adef.thirstHungerTrigger) && this.parent.fightingOpponent == null) {
            if (this.parent.getVehicle() != null) {
                return this.eatFromVehicle();
            } else if (this.parent.isBaby() && this.tryToEatFromMom(true)) {
                return true;
            } else {
                for (int i = 0; i < this.parent.getConnectedDZone().size(); i++) {
                    DesignationZoneAnimal zone = this.parent.getConnectedDZone().get(i);
                    if (this.tryEatFromGround(zone)) {
                        return true;
                    }
                }

                return this.tryEatFromTrough() ? true : this.tryEatGrass();
            }
        } else {
            return false;
        }
    }

    public void forceEatFromMom() {
        this.parent.stopAllMovementNow();
        this.tryToEatFromMom(false);
        this.behaviorCheckTimer = 500.0F;
    }

    private boolean tryToEatFromMom(boolean callOtherBabies) {
        if (this.parent.mother != null
            && this.parent.mother.getBehavior() != null
            && !this.parent.mother.getBehavior().isDoingBehavior
            && this.parent.mother.isExistInTheWorld()
            && this.parent.mother.haveEnoughMilkToFeedFrom()
            && this.parent.mother.getCurrentState().equals(AnimalIdleState.instance())) {
            this.parent.stopAllMovementNow();
            this.parent.mother.getBehavior().blockMovement = true;
            this.parent.mother.stopAllMovementNow();
            this.parent
                .pathToLocation(this.parent.mother.getCurrentSquare().x, this.parent.mother.getCurrentSquare().y, this.parent.mother.getCurrentSquare().getZ());
            this.behaviorObject = this.parent.mother;
            this.behaviorAction = BehaviorAction.EATMOM;
            this.setDoingBehavior(true);
            if (callOtherBabies && this.parent.mother != null && !this.parent.mother.getBabies().isEmpty()) {
                for (int i = 0; i < this.parent.mother.getBabies().size(); i++) {
                    if (this.parent.mother.getBabies().get(i) != this.parent) {
                        this.parent.mother.getBabies().get(i).getBehavior().forceEatFromMom();
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private void eatFromMom() {
        this.parent.mother.getBehavior().blockMovement = false;
        this.parent.faceThisObject(this.parent.mother);
        this.parent.setVariable("idleAction", "eat");
        this.parent.setVariable("eatingAnim", "feed");
    }

    private boolean tryEatFromTrough() {
        ArrayList<IsoFeedingTrough> allTrough = this.getRandomTroughList();

        for (int i = 0; i < allTrough.size(); i++) {
            IsoFeedingTrough trough = allTrough.get(i);
            if (!this.parent.ignoredTrough.contains(trough) && this.canEatFromTrough(trough)) {
                this.behaviorAction = BehaviorAction.EATTROUGH;
                this.behaviorObject = trough;
                if (trough.getSquare() != null
                    && this.parent.getCurrentSquare() != null
                    && trough.getSquare().DistToProper(this.parent.getCurrentSquare()) > 1.0F) {
                    this.parent.stopAllMovementNow();
                    this.parent.pathToTrough(trough);
                } else {
                    this.setDoingBehavior(true);
                    this.doBehaviorAction();
                }

                return true;
            }
        }

        return false;
    }

    private boolean tryEatGrass() {
        if (this.parent.adef.eatGrass && this.parent.getCurrentSquare() != null) {
            IsoObject floor = this.tryAndGetGrassFloor();
            if (floor != null) {
                this.behaviorAction = BehaviorAction.EATGRASS;
                this.behaviorObject = floor.getSquare();
                if (this.parent.getCurrentSquare() != null && floor.getSquare().DistToProper(this.parent.getCurrentSquare()) > 1.0F) {
                    this.parent.stopAllMovementNow();
                    this.parent.pathToLocation(floor.getSquare().getX(), floor.getSquare().getY(), floor.getSquare().getZ());
                } else if (this.parent.getVehicle() == null) {
                    this.setDoingBehavior(true);
                    this.doBehaviorAction();
                }

                return true;
            }
        }

        if (this.parent.getStateEventDelayTimer() <= 50.0F) {
            this.parent.setStateEventDelayTimer(50.0F);
        }

        return false;
    }

    private boolean canEatFromTrough(IsoFeedingTrough trough) {
        if (this.parent.adef.eatTypeTrough != null && trough.getContainer() != null) {
            for (int i = 0; i < trough.getContainer().getItems().size(); i++) {
                InventoryItem item = trough.getContainer().getItems().get(i);
                if (!(item instanceof Food food && food.isRotten())) {
                    if (this.parent.adef.eatTypeTrough.contains("All")
                        || this.parent.adef.eatTypeTrough.contains(item.getFullType())
                        || this.parent.adef.eatTypeTrough.contains(item.getAnimalFeedType())) {
                        return true;
                    }

                    if (item instanceof Food foodx && this.parent.adef.eatTypeTrough.contains(foodx.getFoodType())) {
                        return true;
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public ArrayList<IsoFeedingTrough> getRandomTroughList() {
        ArrayList<IsoFeedingTrough> result = new ArrayList<>();

        for (int i = 0; i < this.parent.getConnectedDZone().size(); i++) {
            DesignationZoneAnimal zone = this.parent.getConnectedDZone().get(i);
            result.addAll(zone.troughs);
        }

        shuffleList(result);
        return result;
    }

    public static void shuffleList(ArrayList<IsoFeedingTrough> a) {
        int n = a.size();
        Random random = new Random();
        random.nextInt();

        for (int i = 0; i < n; i++) {
            int change = i + random.nextInt(n - i);
            swap(a, i, change);
        }
    }

    private static void swap(List<IsoFeedingTrough> a, int i, int change) {
        IsoFeedingTrough helper = a.get(i);
        a.set(i, a.get(change));
        a.set(change, helper);
    }

    public boolean eatFromVehicle() {
        if (this.parent.isBaby() && this.parent.needMom() && this.parent.getMother() != null && this.parent.mother.haveEnoughMilkToFeedFrom()) {
            this.parent.getStats().remove(CharacterStat.HUNGER, 0.2F);
            this.parent.getStats().remove(CharacterStat.THIRST, 0.2F);
            this.parent
                .mother
                .getData()
                .setMilkQuantity(
                    this.parent.mother.getData().getMilkQuantity() - Rand.Next(0.2F / this.parent.adef.hungerBoost, 0.5F / this.parent.adef.hungerBoost)
                );
            return true;
        } else {
            BaseVehicle vehicle = this.parent.getVehicle();
            VehiclePart foodCont = vehicle.getPartById("TrailerAnimalFood");
            InventoryItem edibleItem = null;
            if (foodCont != null && foodCont.getItemContainer() != null) {
                for (int i = 0; i < foodCont.getItemContainer().getItems().size(); i++) {
                    InventoryItem item = foodCont.getItemContainer().getItems().get(i);
                    if (this.parent.adef.eatTypeTrough != null) {
                        for (int k = 0; k < this.parent.adef.eatTypeTrough.size(); k++) {
                            String type = this.parent.adef.eatTypeTrough.get(k);
                            if (item instanceof Food food) {
                                if (type.equals(food.getFoodType()) || type.equals(item.getAnimalFeedType())) {
                                    edibleItem = item;
                                    break;
                                }
                            } else if (item instanceof DrainableComboItem && type.equals(item.getAnimalFeedType())) {
                                edibleItem = item;
                                break;
                            }
                        }
                    }

                    if (edibleItem != null) {
                        break;
                    }
                }

                if (edibleItem != null) {
                    this.parent.getData().eatItem(edibleItem, false);
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    private void updateAcceptance() {
        if (this.parent.heldBy != null) {
            float acceptLevel = this.parent.getAcceptanceLevel(this.parent.heldBy);
            acceptLevel += Rand.Next(0.002F, 0.005F) * ((100.0F - this.parent.stressLevel) / 100.0F) * GameTime.getInstance().getMultiplier();
            acceptLevel = Math.min(100.0F, acceptLevel);
            if (acceptLevel > 100.0F) {
                acceptLevel = 100.0F;
            }

            this.parent.playerAcceptanceList.put(this.parent.heldBy.getOnlineID(), acceptLevel);
        }
    }

    private void followChr() {
        if (!this.blockMovement && this.parent.eatFromTrough == null && this.parent.eatFromGround == null) {
            if (this.parent.getData().getAttachedPlayer() != null) {
                if (this.followChrTimer > 0) {
                    this.followChrTimer = (int)(this.followChrTimer - GameTime.getInstance().getMultiplier());
                    return;
                }

                this.followChrTimer = 150;
                IsoGameCharacter chr = this.parent.getData().getAttachedPlayer();
                float dist = IsoUtils.DistanceTo(this.parent.getX(), this.parent.getY(), chr.getX(), chr.getY());
                if (chr.isPlayerMoving() && dist > 2.0F) {
                    this.walkToChr(chr, 2);
                }

                if (dist < 4.0F && !this.parent.isMoving()) {
                    return;
                }

                if (dist >= 4.0F && !chr.isPlayerMoving()) {
                    this.walkToChr(chr, 5);
                }

                if (dist >= 15.0F) {
                    this.parent.getData().getAttachedPlayer().getAttachedAnimals().remove(this.parent);
                    this.parent.getData().setAttachedPlayer(null);
                }
            } else {
                this.parent.setVariable("animalSpeed", 1.0F);
                this.followChrTimer = 0;
            }
        }
    }

    private void walkToChr(IsoGameCharacter chr, Integer pushedLength) {
        this.parent.setVariable("animalSpeed", 1.2F);
        tempVector2.x = chr.getX();
        tempVector2.y = chr.getY();
        tempVector2.x = tempVector2.x - this.parent.getX();
        tempVector2.y = tempVector2.y - this.parent.getY();
        float undershootValue = -1.0F;
        tempVector2.setLength(tempVector2.getLength() + -1.0F);
        this.parent.pathToLocation(PZMath.fastfloor(this.parent.getX() + tempVector2.x), PZMath.fastfloor(this.parent.getY() + tempVector2.y), chr.getZi());
        if (tempVector2.getLength() > 5.0F) {
            this.parent.setVariable(AnimationVariableHandles.animalRunning, true);
        } else {
            this.parent.setVariable(AnimationVariableHandles.animalRunning, false);
        }
    }

    private void wildAnimalFLeeFromAttacker() {
        if (this.isWildAndHurt()) {
            this.setWildDropDeadTimer(this.getWildDropDeadTimer() - GameTime.getInstance().getMultiplier());
            if (this.parent.getCurrentSquare() != null && this.parent.getCurrentSquare().getChunk() != null && Rand.NextBool(5)) {
                this.parent
                    .getCurrentSquare()
                    .getChunk()
                    .addBloodSplat(
                        this.parent.getCurrentSquare().x + Rand.Next(-0.8F, 0.8F),
                        this.parent.getCurrentSquare().y + Rand.Next(-0.8F, 0.8F),
                        this.parent.getCurrentSquare().getZ(),
                        Rand.Next(20)
                    );
                if (Rand.NextBool(2)) {
                    this.parent
                        .getCurrentSquare()
                        .getChunk()
                        .addBloodSplat(
                            this.parent.getCurrentSquare().x + Rand.Next(-0.8F, 0.8F),
                            this.parent.getCurrentSquare().y + Rand.Next(-0.8F, 0.8F),
                            this.parent.getCurrentSquare().getZ(),
                            Rand.Next(20)
                        );
                }

                if (Rand.NextBool(2)) {
                    this.parent
                        .getCurrentSquare()
                        .getChunk()
                        .addBloodSplat(
                            this.parent.getCurrentSquare().x + Rand.Next(-0.8F, 0.8F),
                            this.parent.getCurrentSquare().y + Rand.Next(-0.8F, 0.8F),
                            this.parent.getCurrentSquare().getZ(),
                            Rand.Next(20)
                        );
                }
            }

            if (this.getWildDropDeadTimer() <= 0.0F) {
                this.parent.setHealth(0.0F);
            }
        }
    }

    private void fleeFromAttacker() {
        if (this.parent.attackedBy == null) {
            this.removeAttacked();
        } else {
            long timeNow = GameTime.getInstance().getCalender().getTimeInMillis();
            if (timeNow - this.parent.attackedTimer <= 1100000L && !(this.parent.DistTo(this.parent.fightingOpponent) > 40.0F)) {
                if (this.parent.getData().getAttachedPlayer() != null) {
                    this.parent.getData().getAttachedPlayer().removeAttachedAnimal(this.parent);
                    this.parent.getData().setAttachedPlayer(null);
                }

                this.parent.getData().setAttachedTree(null);
                if (this.parent.adef.attackBack && this.parent.attackedBy instanceof IsoPlayer isoPlayer) {
                    if (!this.parent.attackedBy.isInvisible() && !isoPlayer.isGhostMode()) {
                        this.parent.fightingOpponent = this.parent.attackedBy;
                    } else {
                        this.parent.fightingOpponent = null;
                    }
                } else {
                    tempVector2.x = this.parent.getX();
                    tempVector2.y = this.parent.getY();
                    tempVector2.x = tempVector2.x - this.parent.attackedBy.getX();
                    tempVector2.y = tempVector2.y - this.parent.attackedBy.getY();
                    tempVector2.setLength(Math.max(20.0F, 20.0F - tempVector2.getLength()));
                    int goX = tempVector2.floorX();
                    int goY = tempVector2.floorY();
                    this.parent.setVariable(AnimationVariableHandles.animalRunning, true);
                    if (!this.parent.isAnimalMoving() && !this.parent.isAnimalAttacking()) {
                        this.resetBehaviorAction();
                        if (this.parent.getDir() != IsoDirections.fromAngle(tempVector2)) {
                            this.parent.pathToLocation(this.parent.getXi() + goX, this.parent.getYi() + goY, this.parent.getZi());
                        }

                        if (!this.parent.isAnimalMoving()) {
                            this.parent.pathToLocation(this.parent.getXi() + goX, this.parent.getYi() + goY, this.parent.getZi());
                        }
                    }

                    if (this.parent.isWild()) {
                        this.wildAnimalFLeeFromAttacker();
                    }
                }
            } else {
                this.removeAttacked();
            }
        }
    }

    private void removeAttacked() {
        this.parent.setAttackedBy(null);
        this.parent.attackedTimer = -1L;
    }

    private void fleeFromChr() {
        if (this.parent.spottedChr != null) {
            if (this.parent.spottedChr != this.parent.atkTarget) {
                if (this.parent.getData().getAttachedPlayer() != this.parent.spottedChr) {
                    if (this.parent.fightingOpponent != this.parent.spottedChr) {
                        if (this.timerFleeAgain > 0.0F) {
                            this.timerFleeAgain = this.timerFleeAgain - GameTime.getInstance().getMultiplier();
                            if (this.timerFleeAgain < 0.0F) {
                                this.timerFleeAgain = 0.0F;
                            }
                        }

                        boolean craven = this.parent.geneticDisorder.contains("craven");
                        if (!craven && !this.parent.isWild() && this.parent.spottedChr instanceof IsoPlayer spottedPlayer && !this.parent.adef.alwaysFleeHumans
                            )
                         {
                            if (!spottedPlayer.isPlayerMoving()) {
                                return;
                            }

                            int baseChance = 0;
                            baseChance += (int)(this.parent.stressLevel / 1.8F);
                            float accept = this.parent.getAcceptanceLevel(spottedPlayer);
                            baseChance -= (int)(accept / 2.0F);
                            if (accept < 70.0F) {
                                float dist = this.parent.DistTo(this.parent.spottedChr);
                                baseChance += (int)(dist * 5.0F);
                            }

                            if (spottedPlayer.isRunning()) {
                                baseChance *= 4;
                            }

                            baseChance = Math.max(0, baseChance);
                            if (baseChance == 0) {
                                return;
                            }

                            if (accept >= 40.0F && this.parent.stressLevel < 70.0F) {
                                return;
                            }

                            if (Rand.Next(10000) > baseChance) {
                                return;
                            }
                        }

                        if (craven
                            || this.parent.luredBy != null
                            || this.parent.isWild()
                            || !(this.parent.spottedChr instanceof IsoZombie)
                            || this.parent.adef.fleeZombies) {
                            tempVector2.x = this.parent.getX();
                            tempVector2.y = this.parent.getY();
                            tempVector2.x = tempVector2.x - this.parent.spottedChr.getX();
                            tempVector2.y = tempVector2.y - this.parent.spottedChr.getY();
                            tempVector2.setLength(Math.max(10.0F, 10.0F - tempVector2.getLength()));
                            if (this.parent.isWild()) {
                                tempVector2.setLength(Math.max(30.0F, 30.0F - tempVector2.getLength()));
                            }

                            int goX = tempVector2.floorX();
                            int goY = tempVector2.floorY();
                            this.parent.setVariable(AnimationVariableHandles.animalRunning, Math.abs(goY) > 5 || Math.abs(goX) > 5);
                            if (this.parent.isWild()) {
                                this.parent.setVariable(AnimationVariableHandles.animalRunning, true);
                            }

                            if (!this.parent.isWild()
                                && this.parent.spottedChr instanceof IsoPlayer spottedPlayer
                                && this.parent.getAcceptanceLevel(spottedPlayer) > 50.0F) {
                                this.parent.setVariable(AnimationVariableHandles.animalRunning, false);
                            }

                            if (this.parent.stressLevel > 50.0F) {
                                this.parent.setVariable(AnimationVariableHandles.animalRunning, true);
                            }

                            if (!this.parent.isAnimalMoving() && this.timerFleeAgain == 0.0F && !this.parent.isAnimalAttacking()) {
                                this.resetBehaviorAction();
                                this.timerFleeAgain = 200.0F;
                                this.parent.playStressedSound();
                                if (this.parent.getDir() != IsoDirections.fromAngle(tempVector2)) {
                                    this.parent.pathToLocation(this.parent.getXi() + goX, this.parent.getYi() + goY, this.parent.getZi());
                                }

                                if (!this.parent.isAnimalMoving()) {
                                    this.parent.pathToLocation(this.parent.getXi() + goX, this.parent.getYi() + goY, this.parent.getZi());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void forceFleeFromChr(IsoGameCharacter chr) {
        tempVector2.x = this.parent.getX();
        tempVector2.y = this.parent.getY();
        tempVector2.x = tempVector2.x - chr.getX();
        tempVector2.y = tempVector2.y - chr.getY();
        tempVector2.setLength(Math.max(10.0F, 10.0F - tempVector2.getLength()));
        int goX = tempVector2.floorX();
        int goY = tempVector2.floorY();
        this.parent.setVariable(AnimationVariableHandles.animalRunning, true);
        this.resetBehaviorAction();
        this.timerFleeAgain = 200.0F;
        this.parent.playStressedSound();
        if (this.parent.getDir() != IsoDirections.fromAngle(tempVector2)) {
            this.parent.pathToLocation(this.parent.getXi() + goX, this.parent.getYi() + goY, this.parent.getZi());
        }

        if (!this.parent.isAnimalMoving()) {
            this.parent.pathToLocation(this.parent.getXi() + goX, this.parent.getYi() + goY, this.parent.getZi());
        }
    }

    public void spotted(IsoMovingObject other, boolean bForced, float dist) {
        this.parent.spottedChr = null;
        if (this.lastAlerted > 0.0F) {
            this.lastAlerted = this.lastAlerted - GameTime.getInstance().getMultiplier();
        }

        if (this.lastAlerted < 0.0F) {
            this.lastAlerted = 0.0F;
        }

        if (!GameClient.client) {
            if (this.parent.getCurrentSquare() != null) {
                if (other.getCurrentSquare() != null) {
                    if (!this.parent.getCurrentSquare().getProperties().has(IsoFlagType.smoke)) {
                        if (!(other instanceof IsoPlayer player && player.isGhostMode())) {
                            IsoGameCharacter otherCharacter = Type.tryCastTo(other, IsoGameCharacter.class);
                            if (otherCharacter == null || !otherCharacter.isDead()) {
                                if (otherCharacter instanceof IsoPlayer otherPlayer && !this.parent.isWild() && dist < 10.0F) {
                                    float acceptLevel = this.parent.getAcceptanceLevel(otherPlayer);
                                    acceptLevel += Rand.Next(5.0E-4F, 6.0E-4F)
                                        * ((100.0F - this.parent.stressLevel) / 100.0F)
                                        * GameTime.getInstance().getMultiplier();
                                    acceptLevel = Math.min(100.0F, acceptLevel);
                                    if (acceptLevel > 100.0F) {
                                        acceptLevel = 100.0F;
                                    }

                                    this.parent.playerAcceptanceList.put(otherCharacter.getOnlineID(), acceptLevel);
                                    if (otherCharacter.isRunning()) {
                                        this.parent.changeStress(GameTime.getInstance().getMultiplier() / 1000.0F);
                                    }

                                    if (!this.parent.isAnimalMoving()
                                        && this.parent.fightingOpponent == null
                                        && this.parent.adef.attackIfStressed
                                        && this.parent.stressLevel > 80.0F
                                        && acceptLevel < 30.0F
                                        && !this.parent.getBehavior().isDoingBehavior
                                        && this.parent.atkTarget == null
                                        && this.attackAnimalTimer == 0.0F
                                        && Rand.NextBool(300 - (int)this.parent.stressLevel)) {
                                        this.parent.fightingOpponent = otherCharacter;
                                        this.parent.setVariable(AnimationVariableHandles.animalRunning, true);
                                        this.parent.getBehavior().goAttack(otherCharacter);
                                        return;
                                    }
                                }

                                if (!(otherCharacter instanceof IsoZombie) || this.parent.adef.fleeZombies) {
                                    if (this.parent.isWild() && dist < 3.0F) {
                                        this.parent.spottedChr = other;
                                        this.fleeFromChr();
                                    } else {
                                        boolean spotted = false;
                                        if (this.parent.isWild() && otherCharacter instanceof IsoPlayer isoPlayer && otherCharacter.isPlayerMoving()) {
                                            float baseChance = 8000.0F;
                                            if (otherCharacter.isSneaking()) {
                                                baseChance = 800.0F;
                                            }

                                            if (otherCharacter.isRunning()) {
                                                baseChance = 500000.0F;
                                            }

                                            if (dist <= this.parent.adef.spottingDist) {
                                                baseChance *= 1.0F + (this.parent.adef.spottingDist - dist);
                                            }

                                            baseChance /= (otherCharacter.getPerkLevel(PerkFactory.Perks.Tracking) + 2) * 0.5F;
                                            baseChance /= (otherCharacter.getPerkLevel(PerkFactory.Perks.Sneak) + 2) * 0.3F;
                                            baseChance /= (otherCharacter.getPerkLevel(PerkFactory.Perks.Lightfoot) + 2) * 0.25F;
                                            baseChance /= (otherCharacter.getPerkLevel(PerkFactory.Perks.Nimble) + 2) * 0.25F;
                                            Vector2 thisToOther = IsoGameCharacter.getTempo();
                                            thisToOther.x = other.getX();
                                            thisToOther.y = other.getY();
                                            thisToOther.x = thisToOther.x - this.parent.getX();
                                            thisToOther.y = thisToOther.y - this.parent.getY();
                                            if (other.getCurrentSquare().getZ() != this.parent.getCurrentSquare().getZ()) {
                                                int dif = Math.abs(other.getCurrentSquare().getZ() - this.parent.getCurrentSquare().getZ()) * 5;
                                                dif++;
                                                baseChance /= dif;
                                            }

                                            thisToOther.normalize();
                                            Vector2 thisForward = this.parent.getLookVector(IsoGameCharacter.getTempo2());
                                            float cosAngle = thisForward.dot(thisToOther);
                                            if (cosAngle < -0.4F) {
                                                baseChance /= 16.0F;
                                            } else if (cosAngle < -0.2F) {
                                                baseChance /= 3.0F;
                                            } else if (cosAngle < -0.0F) {
                                                baseChance /= 1.5F;
                                            } else if (cosAngle < 0.2F) {
                                                baseChance /= 1.5F;
                                            } else if (cosAngle <= 0.4F) {
                                                baseChance *= 3.0F;
                                            } else if (cosAngle <= 0.6F) {
                                                baseChance *= 11.0F;
                                            } else if (cosAngle <= 0.8F) {
                                                baseChance *= 24.0F;
                                            } else {
                                                baseChance *= 44.0F;
                                            }

                                            int rand = Rand.Next(25000);
                                            if (rand > (int)baseChance) {
                                                if (otherCharacter.isSneaking()
                                                    && otherCharacter.isOutside()
                                                    && dist <= this.parent.adef.spottingDist
                                                    && this.parent.adef.addTrackingXp) {
                                                    if (GameServer.server) {
                                                        if (otherCharacter.isPlayerMoving() && Rand.NextBool(200)) {
                                                            GameServer.addXp(isoPlayer, PerkFactory.Perks.Tracking, (int)Rand.Next(1.0F, 3.0F));
                                                        } else if (!otherCharacter.isPlayerMoving() && Rand.NextBool(200)) {
                                                            GameServer.addXp(isoPlayer, PerkFactory.Perks.Tracking, (int)Rand.Next(1.0F, 3.0F));
                                                        }

                                                        if (otherCharacter.isSneaking()) {
                                                            if (otherCharacter.isPlayerMoving() && Rand.NextBool(385)) {
                                                                GameServer.addXp(isoPlayer, PerkFactory.Perks.Sneak, (int)Rand.Next(1.0F, 3.0F));
                                                            }

                                                            if (otherCharacter.isPlayerMoving() && Rand.NextBool(385)) {
                                                                GameServer.addXp(isoPlayer, PerkFactory.Perks.Nimble, (int)Rand.Next(1.0F, 3.0F));
                                                            }

                                                            if (otherCharacter.isPlayerMoving() && Rand.NextBool(385)) {
                                                                GameServer.addXp(isoPlayer, PerkFactory.Perks.Lightfoot, (int)Rand.Next(1.0F, 3.0F));
                                                            }
                                                        }
                                                    } else if (!GameClient.client) {
                                                        if (otherCharacter.isPlayerMoving() && Rand.NextBool(200)) {
                                                            otherCharacter.getXp().AddXP(PerkFactory.Perks.Tracking, Rand.Next(1.0F, 3.0F));
                                                        } else if (!otherCharacter.isPlayerMoving() && Rand.NextBool(200)) {
                                                            otherCharacter.getXp().AddXP(PerkFactory.Perks.Tracking, Rand.Next(1.0F, 3.0F));
                                                        }

                                                        if (otherCharacter.isSneaking()) {
                                                            if (otherCharacter.isPlayerMoving() && Rand.NextBool(385)) {
                                                                otherCharacter.getXp().AddXP(PerkFactory.Perks.Sneak, Rand.Next(1.0F, 3.0F));
                                                            }

                                                            if (otherCharacter.isPlayerMoving() && Rand.NextBool(385)) {
                                                                otherCharacter.getXp().AddXP(PerkFactory.Perks.Nimble, Rand.Next(1.0F, 3.0F));
                                                            }

                                                            if (otherCharacter.isPlayerMoving() && Rand.NextBool(385)) {
                                                                otherCharacter.getXp().AddXP(PerkFactory.Perks.Lightfoot, Rand.Next(1.0F, 3.0F));
                                                            }
                                                        }
                                                    }
                                                }

                                                return;
                                            }

                                            spotted = true;
                                        }

                                        if (otherCharacter instanceof IsoZombie && dist <= 10.0F) {
                                            spotted = true;
                                            this.parent
                                                .setDebugStress(
                                                    this.parent.getStress()
                                                        + Rand.Next(2.0E-4F, 8.0E-4F) * GameTime.getInstance().getMultiplier() / (12.0F - dist)
                                                );
                                            if (this.parent.getStress() > 100.0F) {
                                                this.parent.setDebugStress(100.0F);
                                            }

                                            if (dist <= 6.0F) {
                                                this.parent.spottedChr = other;
                                                this.fleeFromChr();
                                                return;
                                            }
                                        }

                                        if (this.parent.getCurrentSquare() == null) {
                                            this.parent.ensureOnTile();
                                        }

                                        if (other.getCurrentSquare() == null) {
                                            other.ensureOnTile();
                                        }

                                        if (dist < this.parent.adef.spottingDist && spotted) {
                                            if (this.parent.adef.canBeAlerted && this.lastAlerted <= 0.0F) {
                                                this.parent.setIsAlerted(true);
                                                this.parent.alertedChr = other;
                                            } else {
                                                this.parent.spottedChr = other;
                                                this.fleeFromChr();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean canBeAttached() {
        return this.parent.adef.canBeAttached;
    }

    public void setBlockMovement(boolean block) {
        if (block) {
            this.parent.stopAllMovementNow();
        } else {
            this.blockedFor = 0.0F;
        }

        this.blockMovement = block;
    }

    public void setHourBeforeLeavingHutch(int hours) {
        this.forcedOutsideHutch = GameTime.getInstance().getCalender().getTimeInMillis() + 3600000L * hours + 1L;
    }

    public void setDoingBehavior(boolean doingBehavior) {
        if (this.parent.getVehicle() != null) {
            doingBehavior = false;
        }

        this.isDoingBehavior = doingBehavior;
    }

    public boolean isWildAndHurt() {
        return this.wildAndHurt;
    }

    public void setWildAndHurt(boolean wildAndHurt) {
        this.wildAndHurt = wildAndHurt;
    }

    public float getWildDropDeadTimer() {
        return this.wildDropDeadTimer;
    }

    public void setWildDropDeadTimer(float wildDropDeadTimer) {
        this.wildDropDeadTimer = wildDropDeadTimer;
    }
}
