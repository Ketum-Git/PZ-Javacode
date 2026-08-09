// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoDirections;
import zombie.iso.Vector2;

public class AnimalFollowWallState extends State {
    private static final AnimalFollowWallState INSTANCE = new AnimalFollowWallState();
    public static final State.Param<Float> REPATHDELAY = State.Param.ofFloat("repathdelay", 0.0F);
    public static final State.Param<Float> TIMETOSTOP_FOLLOWING_WALL = State.Param.ofFloat("timetostop_following_wall", 0.0F);
    public static final State.Param<Boolean> CW = State.Param.of("cw", Boolean.class, null);
    public static final State.Param<IsoDirections> CURRENTDIR = State.Param.of("currentdir", IsoDirections.class);
    private final Vector2 temp = new Vector2();

    public static AnimalFollowWallState instance() {
        return INSTANCE;
    }

    private AnimalFollowWallState() {
        super(false, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        if (owner.getCurrentSquare() != null) {
            owner.set(REPATHDELAY, 0.0F);
            float timer = owner.getCurrentSquare().DistToProper(owner.getPathTargetX(), owner.getPathTargetY()) * 80.0F;
            if (timer < 500.0F) {
                timer = 500.0F;
            }

            owner.set(TIMETOSTOP_FOLLOWING_WALL, timer);
            owner.set(CW, null);
            owner.set(CURRENTDIR, null);
            owner.setVariable("bMoving", true);
        }
    }

    public boolean decideRotation(IsoAnimal animal, IsoDirections collideDir) {
        Boolean rotation = animal.get(CW);
        if (rotation != null) {
            return rotation;
        } else if (animal.getDir() == collideDir) {
            animal.set(CW, Rand.NextBool(2));
            return animal.get(CW);
        } else if (collideDir == IsoDirections.N) {
            if (animal.getDir() != IsoDirections.NE && animal.getDir() != IsoDirections.E) {
                animal.set(CW, false);
                return animal.get(CW);
            } else {
                animal.set(CW, true);
                return animal.get(CW);
            }
        } else if (collideDir == IsoDirections.E) {
            if (animal.getDir() != IsoDirections.SE && animal.getDir() != IsoDirections.S) {
                animal.set(CW, false);
                return animal.get(CW);
            } else {
                animal.set(CW, true);
                return animal.get(CW);
            }
        } else if (collideDir == IsoDirections.S) {
            if (animal.getDir() != IsoDirections.SW && animal.getDir() != IsoDirections.W) {
                animal.set(CW, false);
                return animal.get(CW);
            } else {
                animal.set(CW, true);
                return animal.get(CW);
            }
        } else if (collideDir == IsoDirections.W) {
            if (animal.getDir() != IsoDirections.NW && animal.getDir() != IsoDirections.N) {
                animal.set(CW, false);
                return animal.get(CW);
            } else {
                animal.set(CW, true);
                return animal.get(CW);
            }
        } else {
            animal.set(CW, false);
            return animal.get(CW);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoAnimal animal = (IsoAnimal)owner;
        this.updateParams(animal);
        float repath = owner.get(REPATHDELAY);
        if (repath > 0.0F) {
            repath -= GameTime.getInstance().getMultiplier();
            owner.set(REPATHDELAY, repath);
        } else {
            owner.set(REPATHDELAY, 10.0F);
            if (owner.isCollidedThisFrame()) {
                this.followWall(animal);
            }

            this.checkNoCollide(animal);
            float dx = animal.getPathFindBehavior2().getTargetX() - animal.getX();
            float dy = animal.getPathFindBehavior2().getTargetY() - animal.getY();
            if (dx == 0.0F && dy == 0.0F) {
                dy = 1.0E-4F;
            }

            animal.setForwardDirection(dx, dy);
        }
    }

    public void checkNoCollide(IsoAnimal animal) {
        if (Rand.NextBool(5)) {
            int walkOffset = 7;
            IsoDirections dir = animal.get(CURRENTDIR);
            if (animal.getDir().dy() != 0 || (animal.getDir().dx() != 1 || dir != IsoDirections.E) && (animal.getDir().dx() != -1 || dir != IsoDirections.W)) {
                if (animal.getDir().dx() == 0 && (animal.getDir().dy() == 1 && dir == IsoDirections.S || animal.getDir().dy() == -1 && dir == IsoDirections.N)) {
                    if (!animal.getCurrentSquare().testCollideAdjacent(animal, -1, 0, 0) && !animal.getCurrentSquare().testCollideAdjacent(animal, 1, 0, 0)) {
                        if (Rand.NextBool(2) && !animal.getCurrentSquare().testCollideAdjacent(animal, 1, 0, 0)) {
                            animal.setShouldFollowWall(false);
                            animal.pathToLocation(animal.getXi() + 7, animal.getYi(), animal.getZi());
                            animal.setVariable("bMoving", true);
                        } else if (!animal.getCurrentSquare().testCollideAdjacent(animal, -1, 0, 0)) {
                            animal.setShouldFollowWall(false);
                            animal.pathToLocation(animal.getXi() - 7, animal.getYi(), animal.getZi());
                            animal.setVariable("bMoving", true);
                        }
                    }
                }
            } else if (!animal.getCurrentSquare().testCollideAdjacent(animal, 0, -1, 0) && !animal.getCurrentSquare().testCollideAdjacent(animal, 0, 1, 0)) {
                if (Rand.NextBool(2) && !animal.getCurrentSquare().testCollideAdjacent(animal, 0, 1, 0)) {
                    animal.setShouldFollowWall(false);
                    animal.pathToLocation(animal.getXi(), animal.getYi() + 7, animal.getZi());
                    animal.setVariable("bMoving", true);
                } else if (!animal.getCurrentSquare().testCollideAdjacent(animal, 0, -1, 0)) {
                    animal.setShouldFollowWall(false);
                    animal.pathToLocation(animal.getXi(), animal.getYi() - 7, animal.getZi());
                    animal.setVariable("bMoving", true);
                }
            }
        }
    }

    public void noCollide(IsoAnimal animal) {
        if (Rand.NextBool(7)) {
            int walkOffset = 7;
            if (animal.getDir().dy() != 0 || animal.getDir().dx() != 1 && animal.getDir().dx() != -1) {
                if (animal.getDir().dx() == 0 && (animal.getDir().dy() == 1 || animal.getDir().dy() == -1)) {
                    if (!animal.getCurrentSquare().testCollideAdjacent(animal, -1, 0, 0) && !animal.getCurrentSquare().testCollideAdjacent(animal, 1, 0, 0)) {
                        if (Rand.NextBool(2) && !animal.getCurrentSquare().testCollideAdjacent(animal, 1, 0, 0)) {
                            this.go((int)animal.getX() + 7, (int)animal.getY(), animal);
                        } else if (!animal.getCurrentSquare().testCollideAdjacent(animal, -1, 0, 0)) {
                            this.go((int)animal.getX() - 7, (int)animal.getY(), animal);
                        }
                    }
                }
            } else if (!animal.getCurrentSquare().testCollideAdjacent(animal, 0, -1, 0) && !animal.getCurrentSquare().testCollideAdjacent(animal, 0, 1, 0)) {
                if (Rand.NextBool(2) && !animal.getCurrentSquare().testCollideAdjacent(animal, 0, 1, 0)) {
                    this.go((int)animal.getX(), (int)animal.getY() + 7, animal);
                } else if (!animal.getCurrentSquare().testCollideAdjacent(animal, 0, -1, 0)) {
                    this.go((int)animal.getX(), (int)animal.getY() - 7, animal);
                }
            }
        }
    }

    public boolean continueFollowingWall(IsoAnimal animal) {
        return false;
    }

    public void followWall(IsoAnimal animal) {
        animal.followingWall = true;
        IsoDirections currentDir = animal.get(CURRENTDIR);
        if (!this.continueFollowingWall(animal)) {
            int walkOffset = 40;
            int avoidWallOffset = 4;
            if (animal.getCurrentSquare().testCollideAdjacent(animal, 0, -1, 0)) {
                boolean cw = this.decideRotation(animal, IsoDirections.N);
                if (cw && !animal.getCurrentSquare().testCollideAdjacent(animal, 1, 0, 0)) {
                    if (currentDir != IsoDirections.E) {
                        this.go((int)animal.getX() + 40, (int)animal.getY() + 4, animal);
                        animal.set(CURRENTDIR, IsoDirections.E);
                    }
                } else if (!cw && !animal.getCurrentSquare().testCollideAdjacent(animal, -1, 0, 0)) {
                    if (currentDir != IsoDirections.W) {
                        this.go((int)animal.getX() - 40, (int)animal.getY() + 4, animal);
                        animal.set(CURRENTDIR, IsoDirections.W);
                    }
                } else {
                    this.go((int)animal.getX(), (int)animal.getY() + 40, animal);
                }
            } else if (animal.getCurrentSquare().testCollideAdjacent(animal, 1, 0, 0)) {
                boolean cw = this.decideRotation(animal, IsoDirections.E);
                if (cw && !animal.getCurrentSquare().testCollideAdjacent(animal, -1, 1, 0)) {
                    if (currentDir != IsoDirections.S) {
                        this.go((int)animal.getX() - 4, (int)animal.getY() + 40, animal);
                        animal.set(CURRENTDIR, IsoDirections.S);
                    }
                } else if (!cw && !animal.getCurrentSquare().testCollideAdjacent(animal, 0, -1, 0)) {
                    if (currentDir != IsoDirections.N) {
                        this.go((int)animal.getX() - 4, (int)animal.getY() - 40, animal);
                        animal.set(CURRENTDIR, IsoDirections.N);
                    }
                } else {
                    this.go((int)animal.getX() - 40, (int)animal.getY(), animal);
                }
            } else if (animal.getCurrentSquare().testCollideAdjacent(animal, 0, 1, 0)) {
                boolean cw = this.decideRotation(animal, IsoDirections.S);
                if (cw && !animal.getCurrentSquare().testCollideAdjacent(animal, -1, 0, 0)) {
                    if (currentDir != IsoDirections.W) {
                        this.go((int)animal.getX() - 40, (int)animal.getY() - 4, animal);
                        animal.set(CURRENTDIR, IsoDirections.W);
                    }
                } else if (!cw && !animal.getCurrentSquare().testCollideAdjacent(animal, 1, 0, 0)) {
                    if (currentDir != IsoDirections.E) {
                        this.go((int)animal.getX() + 40, (int)animal.getY() - 4, animal);
                        animal.set(CURRENTDIR, IsoDirections.E);
                    }
                } else {
                    this.go((int)animal.getX(), (int)animal.getY() - 40, animal);
                }
            } else if (animal.getCurrentSquare().testCollideAdjacent(animal, -1, 0, 0)) {
                boolean cw = this.decideRotation(animal, IsoDirections.W);
                if (cw && !animal.getCurrentSquare().testCollideAdjacent(animal, 0, -1, 0)) {
                    if (currentDir != IsoDirections.N) {
                        this.go((int)animal.getX() + 4, (int)animal.getY() - 40, animal);
                        animal.set(CURRENTDIR, IsoDirections.N);
                    }
                } else if (!cw && !animal.getCurrentSquare().testCollideAdjacent(animal, 0, 1, 0)) {
                    if (currentDir != IsoDirections.S) {
                        this.go((int)animal.getX() + 4, (int)animal.getY() + 40, animal);
                        animal.set(CURRENTDIR, IsoDirections.S);
                    }
                } else {
                    this.go((int)animal.getX() + 40, (int)animal.getY(), animal);
                }
            }
        }
    }

    public void go(int x, int y, IsoAnimal animal) {
        animal.getPathFindBehavior2().reset();
        animal.getPathFindBehavior2().pathToLocation(x, y, animal.getZi());
    }

    public void updateParams(IsoAnimal animal) {
        float timeToStop = animal.get(TIMETOSTOP_FOLLOWING_WALL);
        timeToStop -= GameTime.getInstance().getMultiplier();
        if (timeToStop <= 0.0F) {
            animal.getPathFindBehavior2().reset();
            animal.setVariable("bMoving", false);
            animal.followingWall = false;
            animal.setShouldFollowWall(false);
        } else {
            animal.set(TIMETOSTOP_FOLLOWING_WALL, timeToStop);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setVariable("bMoving", false);
        ((IsoAnimal)owner).followingWall = false;
        ((IsoAnimal)owner).setShouldFollowWall(false);
        ((IsoAnimal)owner).getBehavior().doBehaviorAction();
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoAnimal animal = (IsoAnimal)owner;
        if ("PlayBreedSound".equalsIgnoreCase(event.eventName)) {
            animal.onPlayBreedSoundEvent(event.parameterValue);
        }

        super.animEvent(owner, layer, track, event);
    }

    @Override
    public boolean isMoving(IsoGameCharacter owner) {
        return true;
    }
}
