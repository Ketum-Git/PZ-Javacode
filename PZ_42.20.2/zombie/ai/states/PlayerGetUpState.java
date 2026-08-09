// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.AttackType;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.scripting.objects.MoodleType;

@UsedFromLua
public final class PlayerGetUpState extends State {
    private static final PlayerGetUpState INSTANCE = new PlayerGetUpState();
    public static final State.Param<Boolean> FORCE = State.Param.ofBool("force", false);
    public static final State.Param<Boolean> MOVING = State.Param.ofBool("moving", false);
    public static final State.Param<IsoDirections> ISO_DIRECTION = State.Param.of("iso_direction", IsoDirections.class);

    public static PlayerGetUpState instance() {
        return INSTANCE;
    }

    private PlayerGetUpState() {
        super(true, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        IsoPlayer player = (IsoPlayer)owner;
        player.setInitiateAttack(false);
        player.setAttackStarted(false);
        player.setAttackType(AttackType.NONE);
        player.setBlockMovement(true);
        player.setForceRun(false);
        player.setForceSprint(false);
        owner.setVariable("getUpQuick", owner.getVariableBoolean("pressedRunButton"));
        if (owner.getMoodles().getMoodleLevel(MoodleType.PANIC) > 1) {
            owner.setVariable("getUpQuick", true);
        }

        if (owner.getVariableBoolean("pressedMovement")) {
            owner.setVariable("getUpWalk", true);
        }

        owner.set(ISO_DIRECTION, owner.getDir());
        if (owner.getVariableBoolean("SittingOnFurniture")) {
            String dirStr = owner.getVariableString("SitOnFurnitureDirection");
            if (dirStr != null) {
                float angleDeg = 0.0F;
                owner.faceDirection(owner.getDir());
            }

            IsoObject object = owner.getSitOnFurnitureObject();
            if (object != null && object.getProperties() != null && object.getProperties().has("SeatMaterial")) {
                String soundSuffix = object.getProperties().get("SeatMaterial");
                owner.playSoundLocal("StandUp" + soundSuffix);
            } else if (object != null) {
                owner.playSoundLocal("StandUpFabric");
            }
        } else {
            owner.playSoundLocal("SurfaceStandUp");
        }

        if (GameClient.client) {
            owner.setKnockedDown(false);
        }

        if (owner.wasKnockedDown) {
            owner.blockTurning = true;
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.isOnBed()) {
            String dirStr = owner.getVariableString("OnBedDirection");
            if (dirStr != null) {
                float angleDeg = 0.0F;
                IsoDirections startDir = owner.get(ISO_DIRECTION);
                switch (dirStr) {
                    case "Foot":
                        return;
                    case "FootLeft":
                    case "HeadLeft":
                        angleDeg = 0.0F;
                        if (startDir == IsoDirections.N) {
                            angleDeg = 180.0F;
                        }

                        if (startDir == IsoDirections.W) {
                            angleDeg = 90.0F;
                        }

                        if (startDir == IsoDirections.E) {
                            angleDeg = 270.0F;
                        }
                        break;
                    case "FootRight":
                    case "HeadRight":
                        angleDeg = 180.0F;
                        if (startDir == IsoDirections.N) {
                            angleDeg = 0.0F;
                        }

                        if (startDir == IsoDirections.W) {
                            angleDeg = 270.0F;
                        }

                        if (startDir == IsoDirections.E) {
                            angleDeg = 90.0F;
                        }
                }

                owner.blockTurning = true;
                Vector2 v = Vector2.fromLengthDirection(1.0F, angleDeg * (float) (Math.PI / 180.0));
                owner.faceLocationF(owner.getX() + v.x, owner.getY() + v.y);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("getUpWalk");
        if (owner.isOnBed()) {
            owner.blockTurning = false;
            owner.setHideWeaponModel(false);
        }

        if (owner.isSittingOnFurniture()) {
            owner.setHideWeaponModel(false);
        }

        if (owner.getVariableBoolean("sitonground")) {
            owner.setHideWeaponModel(false);
        }

        owner.setIgnoreMovement(false);
        owner.setFallOnFront(false);
        owner.setOnFloor(false);
        ((IsoPlayer)owner).setBlockMovement(false);
        IsoObject object = owner.getSitOnFurnitureObject();
        if (object != null) {
            object.setSatChair(false);
            this.ejectFromSolidFurniture(owner, object);
        }

        owner.setOnBed(false);
        owner.setSittingOnFurniture(false);
        owner.setSitOnFurnitureObject(null);
        owner.setSitOnFurnitureDirection(null);
        owner.setSitOnGround(false);
        if (owner.wasKnockedDown) {
            owner.wasKnockedDown = false;
            owner.blockTurning = false;
        }
    }

    private void ejectFromSolidFurniture(IsoGameCharacter owner, IsoObject object) {
        IsoGridSquare chairSquare = object.getSquare();
        if (chairSquare != null) {
            if (chairSquare.isSolid() || chairSquare.isSolidTrans()) {
                if (chairSquare == owner.getCurrentSquare()) {
                    IsoGridSquare adjacent = chairSquare.getAdjacentSquare(owner.getDir());
                    if (adjacent != null) {
                        int dx = adjacent.getX() - chairSquare.getX();
                        int dy = adjacent.getY() - chairSquare.getY();
                        if (!chairSquare.testCollideAdjacent(owner, dx, dy, 0)) {
                            if (owner.getDir() == IsoDirections.N) {
                                owner.setY(chairSquare.getY() - 0.05F);
                            } else if (owner.getDir() == IsoDirections.S) {
                                owner.setY(adjacent.getY() + 0.05F);
                            } else if (owner.getDir() == IsoDirections.W) {
                                owner.setX(chairSquare.getX() - 0.05F);
                            } else if (owner.getDir() == IsoDirections.E) {
                                owner.setX(adjacent.getX() + 0.05F);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        if (owner.isLocal()) {
            owner.set(FORCE, owner.getVariableBoolean("forceGetUp"));
            owner.set(MOVING, owner.getVariableBoolean("isMoving"));
        } else {
            boolean getUp = owner.get(FORCE) || owner.get(MOVING);
            owner.setVariable("forceGetUp", getUp);
            if (State.Stage.Enter == stage) {
                owner.setVariable("forceGetUp", true);
            }
        }

        super.setParams(owner, stage);
    }
}
