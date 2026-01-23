// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

import java.util.HashMap;
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
import zombie.network.GameServer;

public class AnimalFollowWallState extends State {
    private static final AnimalFollowWallState _instance = new AnimalFollowWallState();
    private final Vector2 temp = new Vector2();
    private static final Integer PARAM_REPATHDELAY = 0;
    private static final Integer PARAM_TIMETOSTOP_FOLLOWING_WALL = 1;
    private static final Integer PARAM_CW = 2;
    private static final Integer PARAM_CURRENTDIR = 3;

    public static AnimalFollowWallState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        if (owner.getCurrentSquare() != null) {
            HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
            StateMachineParams.put(PARAM_REPATHDELAY, 0.0F);
            float timer = owner.getCurrentSquare().DistToProper(owner.getPathTargetX(), owner.getPathTargetY()) * 80.0F;
            if (timer < 500.0F) {
                timer = 500.0F;
            }

            StateMachineParams.put(PARAM_TIMETOSTOP_FOLLOWING_WALL, timer);
            StateMachineParams.put(PARAM_CW, null);
            StateMachineParams.put(PARAM_CURRENTDIR, null);
            owner.setVariable("bMoving", true);
        }
    }

    public boolean decideRotation(IsoAnimal animal, IsoDirections collideDir) {
        HashMap<Object, Object> StateMachineParams = animal.getStateMachineParams(this);
        Boolean rotation = (Boolean)StateMachineParams.get(PARAM_CW);
        if (rotation != null) {
            return rotation;
        } else if (animal.getDir() == collideDir) {
            StateMachineParams.put(PARAM_CW, Rand.NextBool(2));
            return (Boolean)StateMachineParams.get(PARAM_CW);
        } else if (collideDir == IsoDirections.N) {
            if (animal.getDir() != IsoDirections.NE && animal.getDir() != IsoDirections.E) {
                StateMachineParams.put(PARAM_CW, false);
                return (Boolean)StateMachineParams.get(PARAM_CW);
            } else {
                StateMachineParams.put(PARAM_CW, true);
                return (Boolean)StateMachineParams.get(PARAM_CW);
            }
        } else if (collideDir == IsoDirections.E) {
            if (animal.getDir() != IsoDirections.SE && animal.getDir() != IsoDirections.S) {
                StateMachineParams.put(PARAM_CW, false);
                return (Boolean)StateMachineParams.get(PARAM_CW);
            } else {
                StateMachineParams.put(PARAM_CW, true);
                return (Boolean)StateMachineParams.get(PARAM_CW);
            }
        } else if (collideDir == IsoDirections.S) {
            if (animal.getDir() != IsoDirections.SW && animal.getDir() != IsoDirections.W) {
                StateMachineParams.put(PARAM_CW, false);
                return (Boolean)StateMachineParams.get(PARAM_CW);
            } else {
                StateMachineParams.put(PARAM_CW, true);
                return (Boolean)StateMachineParams.get(PARAM_CW);
            }
        } else if (collideDir == IsoDirections.W) {
            if (animal.getDir() != IsoDirections.NW && animal.getDir() != IsoDirections.N) {
                StateMachineParams.put(PARAM_CW, false);
                return (Boolean)StateMachineParams.get(PARAM_CW);
            } else {
                StateMachineParams.put(PARAM_CW, true);
                return (Boolean)StateMachineParams.get(PARAM_CW);
            }
        } else {
            StateMachineParams.put(PARAM_CW, false);
            return (Boolean)StateMachineParams.get(PARAM_CW);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoAnimal animal = (IsoAnimal)owner;
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        this.updateParams(animal);
        float repath = (Float)StateMachineParams.get(PARAM_REPATHDELAY);
        if (repath > 0.0F) {
            repath -= GameTime.getInstance().getMultiplier();
            StateMachineParams.put(PARAM_REPATHDELAY, repath);
        } else {
            StateMachineParams.put(PARAM_REPATHDELAY, 10.0F);
            this.temp.x = animal.getPathFindBehavior2().getTargetX();
            this.temp.y = animal.getPathFindBehavior2().getTargetY();
            this.temp.x = this.temp.x - animal.getX();
            this.temp.y = this.temp.y - animal.getY();
            float dist = this.temp.getLength();
            boolean bCollidedWithObject = owner.isCollidedThisFrame();
            if (bCollidedWithObject) {
                this.followWall(animal);
            }

            this.checkNoCollide(animal);
            if (!bCollidedWithObject) {
            }

            if (!GameServer.server) {
                float distScale = Math.min(dist / 2.0F, 4.0F);
                float x = (owner.getID() + animal.animalId) % 20 / 10.0F - 1.0F;
                float y = (animal.getID() + animal.animalId) % 20 / 10.0F - 1.0F;
                this.temp.x = this.temp.x + animal.getX();
                this.temp.y = this.temp.y + animal.getY();
                this.temp.x += x * distScale;
                this.temp.y += y * distScale;
                this.temp.x = this.temp.x - animal.getX();
                this.temp.y = this.temp.y - animal.getY();
            }

            this.temp.normalize();
            animal.setDir(IsoDirections.fromAngle(this.temp));
            animal.setForwardDirection(this.temp);
        }
    }

    public void checkNoCollide(IsoAnimal animal) {
        if (Rand.NextBool(5)) {
            int walkOffset = 7;
            HashMap<Object, Object> StateMachineParams = animal.getStateMachineParams(this);
            IsoDirections dir = (IsoDirections)StateMachineParams.get(PARAM_CURRENTDIR);
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
        HashMap<Object, Object> StateMachineParams = animal.getStateMachineParams(this);
        Boolean cw = (Boolean)StateMachineParams.get(PARAM_CW);
        IsoDirections currentDir = (IsoDirections)StateMachineParams.get(PARAM_CURRENTDIR);
        return false;
    }

    public void followWall(IsoAnimal animal) {
        animal.followingWall = true;
        HashMap<Object, Object> StateMachineParams = animal.getStateMachineParams(this);
        Boolean cw = (Boolean)StateMachineParams.get(PARAM_CW);
        IsoDirections currentDir = (IsoDirections)StateMachineParams.get(PARAM_CURRENTDIR);
        if (!this.continueFollowingWall(animal)) {
            int walkOffset = 40;
            int avoidWallOffset = 4;
            if (animal.getCurrentSquare().testCollideAdjacent(animal, 0, -1, 0)) {
                cw = this.decideRotation(animal, IsoDirections.N);
                if (cw && !animal.getCurrentSquare().testCollideAdjacent(animal, 1, 0, 0)) {
                    if (currentDir != IsoDirections.E) {
                        this.go((int)animal.getX() + 40, (int)animal.getY() + 4, animal);
                        StateMachineParams.put(PARAM_CURRENTDIR, IsoDirections.E);
                    }
                } else if (!cw && !animal.getCurrentSquare().testCollideAdjacent(animal, -1, 0, 0)) {
                    if (currentDir != IsoDirections.W) {
                        this.go((int)animal.getX() - 40, (int)animal.getY() + 4, animal);
                        StateMachineParams.put(PARAM_CURRENTDIR, IsoDirections.W);
                    }
                } else {
                    this.go((int)animal.getX(), (int)animal.getY() + 40, animal);
                }
            } else if (animal.getCurrentSquare().testCollideAdjacent(animal, 1, 0, 0)) {
                cw = this.decideRotation(animal, IsoDirections.E);
                if (cw && !animal.getCurrentSquare().testCollideAdjacent(animal, -1, 1, 0)) {
                    if (currentDir != IsoDirections.S) {
                        this.go((int)animal.getX() - 4, (int)animal.getY() + 40, animal);
                        StateMachineParams.put(PARAM_CURRENTDIR, IsoDirections.S);
                    }
                } else if (!cw && !animal.getCurrentSquare().testCollideAdjacent(animal, 0, -1, 0)) {
                    if (currentDir != IsoDirections.N) {
                        this.go((int)animal.getX() - 4, (int)animal.getY() - 40, animal);
                        StateMachineParams.put(PARAM_CURRENTDIR, IsoDirections.N);
                    }
                } else {
                    this.go((int)animal.getX() - 40, (int)animal.getY(), animal);
                }
            } else if (animal.getCurrentSquare().testCollideAdjacent(animal, 0, 1, 0)) {
                cw = this.decideRotation(animal, IsoDirections.S);
                if (cw && !animal.getCurrentSquare().testCollideAdjacent(animal, -1, 0, 0)) {
                    if (currentDir != IsoDirections.W) {
                        this.go((int)animal.getX() - 40, (int)animal.getY() - 4, animal);
                        StateMachineParams.put(PARAM_CURRENTDIR, IsoDirections.W);
                    }
                } else if (!cw && !animal.getCurrentSquare().testCollideAdjacent(animal, 1, 0, 0)) {
                    if (currentDir != IsoDirections.E) {
                        this.go((int)animal.getX() + 40, (int)animal.getY() - 4, animal);
                        StateMachineParams.put(PARAM_CURRENTDIR, IsoDirections.E);
                    }
                } else {
                    this.go((int)animal.getX(), (int)animal.getY() - 40, animal);
                }
            } else if (animal.getCurrentSquare().testCollideAdjacent(animal, -1, 0, 0)) {
                cw = this.decideRotation(animal, IsoDirections.W);
                if (cw && !animal.getCurrentSquare().testCollideAdjacent(animal, 0, -1, 0)) {
                    if (currentDir != IsoDirections.N) {
                        this.go((int)animal.getX() + 4, (int)animal.getY() - 40, animal);
                        StateMachineParams.put(PARAM_CURRENTDIR, IsoDirections.N);
                    }
                } else if (!cw && !animal.getCurrentSquare().testCollideAdjacent(animal, 0, 1, 0)) {
                    if (currentDir != IsoDirections.S) {
                        this.go((int)animal.getX() + 4, (int)animal.getY() + 40, animal);
                        StateMachineParams.put(PARAM_CURRENTDIR, IsoDirections.S);
                    }
                } else {
                    this.go((int)animal.getX() + 40, (int)animal.getY(), animal);
                }
            }
        }
    }

    public void go(int x, int y, IsoAnimal animal) {
        HashMap<Object, Object> StateMachineParams = animal.getStateMachineParams(this);
        animal.getPathFindBehavior2().reset();
        animal.getPathFindBehavior2().pathToLocation(x, y, animal.getZi());
    }

    public void updateParams(IsoAnimal animal) {
        HashMap<Object, Object> StateMachineParams = animal.getStateMachineParams(this);
        float timeToStop = (Float)StateMachineParams.get(PARAM_TIMETOSTOP_FOLLOWING_WALL);
        timeToStop -= GameTime.getInstance().getMultiplier();
        if (timeToStop <= 0.0F) {
            animal.getPathFindBehavior2().reset();
            animal.setVariable("bMoving", false);
            animal.followingWall = false;
            animal.setShouldFollowWall(false);
        } else {
            StateMachineParams.put(PARAM_TIMETOSTOP_FOLLOWING_WALL, timeToStop);
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
