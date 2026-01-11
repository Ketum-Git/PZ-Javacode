// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.GameTime;
import zombie.ai.states.ClimbOverFenceState;
import zombie.ai.states.ClimbOverWallState;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.ai.states.LungeState;
import zombie.ai.states.PathFindState;
import zombie.ai.states.ThumpState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.ZombieTurnAlerted;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.IAnimatable;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.options.Multiplayer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetworkVariables;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.character.ZombiePacket;
import zombie.pathfind.PathFindBehavior2;
import zombie.popman.NetworkZombieSimulator;
import zombie.util.Type;

public class NetworkZombieAI extends NetworkCharacterAI {
    private final PathFindBehavior2 pfb2;
    public final IsoZombie zombie;
    public boolean isClimbing;
    private byte flags;
    private byte lastDirection;
    public final NetworkZombieMind mindSync;
    public final ObjectID reanimatedBodyId = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
    public boolean wasSeparated;
    public boolean debugInterfaceActive;

    public NetworkZombieAI(IsoGameCharacter character) {
        super(character);
        this.zombie = (IsoZombie)character;
        this.isClimbing = false;
        this.flags = 0;
        this.pfb2 = this.zombie.getPathFindBehavior2();
        this.mindSync = new NetworkZombieMind(this.zombie);
        character.ulBeatenVehicle.Reset(400L);
        this.reanimatedBodyId.reset();
    }

    @Override
    public void reset() {
        super.reset();
        this.usePathFind = true;
        this.targetX = this.zombie.getX();
        this.targetY = this.zombie.getY();
        this.targetZ = (byte)this.zombie.getZ();
        this.isClimbing = false;
        this.flags = 0;
        this.zombie.getHitDir().set(0.0F, 0.0F);
        this.reanimatedBodyId.reset();
    }

    @Override
    public IsoPlayer getRelatedPlayer() {
        return Type.tryCastTo(this.zombie.target, IsoPlayer.class);
    }

    @Override
    public Multiplayer.DebugFlagsOG.IsoGameCharacterOG getBooleanDebugOptions() {
        return DebugOptions.instance.multiplayer.debugFlags.zombie;
    }

    public void extraUpdate() {
        NetworkZombieSimulator.getInstance().addExtraUpdate(this.zombie);
    }

    private void setUsingExtrapolation(ZombiePacket packet, int t) {
        if (this.zombie.isMoving()) {
            Vector2 chrDir = this.zombie.dir.ToVector();
            this.zombie.networkCharacter.checkReset(t);
            NetworkCharacter.Transform transform = this.zombie.networkCharacter.predict(500, t, this.zombie.getX(), this.zombie.getY(), chrDir.x, chrDir.y);
            packet.x = transform.position.x;
            packet.y = transform.position.y;
            packet.z = (byte)this.zombie.getZ();
            packet.predictionType = 1;
        } else {
            packet.x = this.zombie.getX();
            packet.y = this.zombie.getY();
            packet.z = (byte)this.zombie.getZ();
            packet.predictionType = 0;
        }
    }

    private void setUsingThump(ZombiePacket packet) {
        packet.x = ((IsoObject)this.zombie.getThumpTarget()).getX();
        packet.y = ((IsoObject)this.zombie.getThumpTarget()).getY();
        packet.z = (byte)((IsoObject)this.zombie.getThumpTarget()).getZ();
        packet.predictionType = 3;
    }

    private void setUsingClimb(ZombiePacket packet) {
        packet.x = this.zombie.getTarget().getX();
        packet.y = this.zombie.getTarget().getY();
        packet.z = (byte)this.zombie.getTarget().getZ();
        packet.predictionType = 4;
    }

    private void setUsingLungeState(ZombiePacket packet, long t) {
        if (this.zombie.target == null) {
            this.setUsingExtrapolation(packet, (int)t);
        } else {
            float length = IsoUtils.DistanceTo(this.zombie.target.getX(), this.zombie.target.getY(), this.zombie.getX(), this.zombie.getY());
            if (length > 5.0F) {
                packet.x = (this.zombie.getX() + this.zombie.target.getX()) * 0.5F;
                packet.y = (this.zombie.getY() + this.zombie.target.getY()) * 0.5F;
                packet.z = (byte)this.zombie.target.getZ();
                float time = length * 0.5F / 5.0E-4F * this.zombie.speedMod;
                packet.predictionType = 6;
            } else {
                packet.x = this.zombie.target.getX();
                packet.y = this.zombie.target.getY();
                packet.z = (byte)this.zombie.target.getZ();
                float time = length / 5.0E-4F * this.zombie.speedMod;
                packet.predictionType = 5;
            }
        }
    }

    private void setUsingWalkTowardState(ZombiePacket packet) {
        if (this.zombie.getPath2() == null) {
            float length = this.pfb2.getPathLength();
            if (length > 5.0F) {
                packet.x = (this.zombie.getX() + this.pfb2.getTargetX()) * 0.5F;
                packet.y = (this.zombie.getY() + this.pfb2.getTargetY()) * 0.5F;
                packet.z = (byte)this.pfb2.getTargetZ();
                float time = length * 0.5F / 5.0E-4F * this.zombie.speedMod;
                packet.predictionType = 8;
            } else {
                packet.x = this.pfb2.getTargetX();
                packet.y = this.pfb2.getTargetY();
                packet.z = (byte)this.pfb2.getTargetZ();
                float time = length / 5.0E-4F * this.zombie.speedMod;
                packet.predictionType = 7;
            }
        } else {
            packet.x = this.pfb2.pathNextX;
            packet.y = this.pfb2.pathNextY;
            packet.z = (byte)this.zombie.getZ();
            float time = IsoUtils.DistanceTo(this.zombie.getX(), this.zombie.getY(), this.pfb2.pathNextX, this.pfb2.pathNextY) / 5.0E-4F * this.zombie.speedMod;
            packet.predictionType = 7;
        }
    }

    private void setUsingPathFindState(ZombiePacket packet) {
        packet.x = this.pfb2.pathNextX;
        packet.y = this.pfb2.pathNextY;
        packet.z = (byte)this.zombie.getZ();
        float time = IsoUtils.DistanceTo(this.zombie.getX(), this.zombie.getY(), this.pfb2.pathNextX, this.pfb2.pathNextY) / 5.0E-4F * this.zombie.speedMod;
        packet.predictionType = 2;
    }

    public void set(ZombiePacket packet) {
        int currentTime = (int)(GameTime.getServerTime() / 1000000L);
        packet.booleanVariables = NetworkZombieVariables.getBooleanVariables(this.zombie);
        packet.health = (short)(this.zombie.health * 1000.0F);
        packet.target = this.zombie.target instanceof IAnimatable animated ? animated.getOnlineID() : -1;
        packet.speedMod = (short)(this.zombie.speedMod * 1000.0F);
        packet.timeSinceSeenFlesh = (short)this.zombie.timeSinceSeenFlesh;
        packet.smParamTargetAngle = (short)(
            (Float)this.zombie.getStateMachineParams(ZombieTurnAlerted.instance()).getOrDefault(ZombieTurnAlerted.PARAM_TARGET_ANGLE, 0.0F) * 1000.0F
        );
        packet.walkType = NetworkVariables.WalkType.fromString(this.zombie.getVariableString("zombieWalkType"));
        packet.realX = this.zombie.getX();
        packet.realY = this.zombie.getY();
        packet.realZ = (byte)this.zombie.getZ();
        if (ModelManager.instance.isCreated()) {
            packet.targetAngle = this.zombie.getAnimationPlayer().getTargetAngle();
        }

        this.zombie.realState = NetworkVariables.ZombieState.fromString(this.zombie.getAdvancedAnimator().getCurrentStateName());
        packet.realState = this.zombie.realState;
        packet.reanimatedBodyId.set(this.reanimatedBodyId);
        if (this.zombie.getCurrentState() == ThumpState.instance() && this.zombie.getThumpTarget() != null && !this.zombie.getThumpTarget().isDestroyed()) {
            if (this.zombie.getThumpTarget() instanceof IsoObject isoObject && isoObject.getSquare() != null) {
                this.setUsingThump(packet);
            } else {
                this.setUsingExtrapolation(packet, currentTime);
                DebugLog.Multiplayer.error("Unexpected thump target %s", this.zombie.getThumpTarget().getClass().getSimpleName());
            }
        } else if (this.zombie.getTarget() == null
            || this.isClimbing
            || this.zombie.getCurrentState() != ClimbOverFenceState.instance()
                && this.zombie.getCurrentState() != ClimbOverWallState.instance()
                && this.zombie.getCurrentState() != ClimbThroughWindowState.instance()) {
            if (this.zombie.getCurrentState() == WalkTowardState.instance()) {
                this.setUsingWalkTowardState(packet);
            } else if (this.zombie.getCurrentState() == LungeState.instance()) {
                this.setUsingLungeState(packet, currentTime);
            } else if (this.zombie.getCurrentState() == PathFindState.instance() && this.zombie.isMoving()) {
                this.setUsingPathFindState(packet);
            } else {
                this.setUsingExtrapolation(packet, currentTime);
            }
        } else {
            this.setUsingClimb(packet);
            this.isClimbing = true;
        }

        Vector2 chrDir = this.zombie.dir.ToVector();
        this.zombie.networkCharacter.updateExtrapolationPoint(currentTime, this.zombie.getX(), this.zombie.getY(), chrDir.x, chrDir.y);
    }

    public void parse(ZombiePacket packet) {
        if (this.usePathFind) {
            this.pfb2.pathToLocationF(packet.realX, packet.realY, packet.realZ);
            this.pfb2.walkingOnTheSpot.reset(this.zombie.getX(), this.zombie.getY());
        }

        this.targetX = packet.x;
        this.targetY = packet.y;
        this.targetZ = packet.z;
        this.predictionType = packet.predictionType;
        if (packet.target == -1) {
            this.zombie.setTargetSeenTime(0.0F);
            this.zombie.target = null;
        } else {
            IsoPlayer target = null;
            if (GameClient.client) {
                target = GameClient.IDToPlayerMap.get(packet.target);
            } else if (GameServer.server) {
                target = GameServer.IDToPlayerMap.get(packet.target);
            }

            if (target != this.zombie.target) {
                this.zombie.setTargetSeenTime(0.0F);
                this.zombie.target = target;
            }
        }

        this.zombie.timeSinceSeenFlesh = packet.timeSinceSeenFlesh;
        if (this.zombie.isRemoteZombie()) {
            this.zombie.setSpeedMod(packet.speedMod);
            this.zombie.getStateMachineParams(ZombieTurnAlerted.instance()).put(ZombieTurnAlerted.PARAM_TARGET_ANGLE, packet.smParamTargetAngle / 1000.0F);
            NetworkZombieVariables.setBooleanVariables(this.zombie, packet.booleanVariables);
            this.zombie.setWalkType(packet.walkType.toString());
            this.zombie.realState = packet.realState;
        }

        this.zombie.realx = packet.realX;
        this.zombie.realy = packet.realY;
        this.zombie.realz = packet.realZ;
        if ((
                IsoUtils.DistanceToSquared(this.zombie.getX(), this.zombie.getY(), this.zombie.realx, this.zombie.realy) > 9.0F
                    || this.zombie.getZ() != this.zombie.realz
            )
            && (
                this.zombie.isRemoteZombie()
                    || IsoPlayer.getInstance() != null
                        && IsoUtils.DistanceToSquared(this.zombie.getX(), this.zombie.getY(), IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY())
                            > 2.0F
            )) {
            this.zombie.teleportTo(this.zombie.realx, this.zombie.realy, this.zombie.realz);
        }
    }

    public void preupdate() {
        if (GameClient.client) {
            if (this.zombie.target != null) {
                this.zombie.setTargetSeenTime(this.zombie.getTargetSeenTime() + GameTime.getInstance().getRealworldSecondsSinceLastUpdate());
            }
        } else if (GameServer.server) {
            byte flags = (byte)((this.zombie.getVariableBoolean("bMoving") ? 1 : 0) | (this.zombie.getVariableBoolean("bPathfind") ? 2 : 0));
            if (this.flags != flags) {
                this.flags = flags;
                this.extraUpdate();
            }

            byte direction = (byte)IsoDirections.fromAngleActual(this.zombie.getForwardDirection()).index();
            if (this.lastDirection != direction) {
                this.lastDirection = direction;
                this.extraUpdate();
            }
        }
    }
}
