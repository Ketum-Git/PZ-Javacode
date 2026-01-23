// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

import org.joml.Vector3f;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.behavior.BehaviorAction;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.pathfind.PolygonalMap2;

public final class AnimalWalkState extends State {
    private static final AnimalWalkState _instance = new AnimalWalkState();
    private final Vector2 temp = new Vector2();
    private final Vector3f worldPos = new Vector3f();

    public static AnimalWalkState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        if (owner.isLocal()) {
            owner.setVariable("bMoving", true);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoAnimal animal = (IsoAnimal)owner;
        if (animal.isLocalPlayer()) {
            this.temp.x = animal.getPathFindBehavior2().getTargetX();
            this.temp.y = animal.getPathFindBehavior2().getTargetY();
            this.temp.x = this.temp.x - animal.getX();
            this.temp.y = this.temp.y - animal.getY();
            float dist = this.temp.getLength();
            boolean bCollidedWithObject = owner.isCollidedThisFrame();
            if (bCollidedWithObject) {
                animal.pathToLocation(owner.getPathTargetX(), owner.getPathTargetY(), owner.getPathTargetZ());
                if (!animal.getVariableBoolean("bPathfind")) {
                    animal.setVariable("bPathfind", true);
                    animal.setVariable("bMoving", true);
                }
            } else {
                if (!GameClient.client) {
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
                float xDiff = Math.abs(animal.getX() - animal.getPathFindBehavior2().getTargetX());
                float yDiff = Math.abs(animal.getY() - animal.getPathFindBehavior2().getTargetY());
                if (xDiff < 0.5 && yDiff < 0.5) {
                    owner.setVariable("bMoving", false);
                }

                if (owner.getPathFindBehavior2().walkingOnTheSpot.check(owner) && animal.spottedChr == null) {
                    owner.setVariable("bMoving", false);
                    owner.setMoving(false);
                    owner.getPathFindBehavior2().reset();
                    owner.setPath2(null);
                    animal.getBehavior().walkedOnSpot();
                }

                if (animal.getBehavior().isDoingBehavior
                    && animal.getBehavior().behaviorAction == BehaviorAction.FIGHTANIMAL
                    && animal.getBehavior().behaviorObject instanceof IsoMovingObject isoMovingObject) {
                    float distProper = animal.DistToProper(isoMovingObject);
                    if (distProper <= animal.adef.attackDist) {
                        this.exit(owner);
                    }
                }

                if (animal.isLocalPlayer()) {
                    animal.getBehavior().wanderIdle();
                }
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        if (owner.isLocal()) {
            owner.setVariable("bMoving", false);
            ((IsoAnimal)owner).getBehavior().doBehaviorAction();
        }
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

    private boolean isPathClear(IsoGameCharacter owner, float x, float y, float z) {
        int chunkX = (int)x / 8;
        int chunkY = (int)y / 8;
        IsoChunk chunk = GameServer.server
            ? ServerMap.instance.getChunk(chunkX, chunkY)
            : IsoWorld.instance.currentCell.getChunkForGridSquare(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
        if (chunk != null) {
            int flags = 1;
            flags |= 2;
            return !PolygonalMap2.instance
                .lineClearCollide(owner.getX(), owner.getY(), x, y, PZMath.fastfloor(z), owner.getPathFindBehavior2().getTargetChar(), flags);
        } else {
            return false;
        }
    }
}
