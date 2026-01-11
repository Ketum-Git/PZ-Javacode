// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.GameTime;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.options.Multiplayer;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.objects.IsoHutch;
import zombie.network.GameServer;
import zombie.network.characters.AttackRateChecker;
import zombie.network.fields.IMovable;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.character.AnimalPacket;
import zombie.network.packets.character.PlayerPacket;
import zombie.vehicles.BaseVehicle;

public abstract class NetworkCharacterAI {
    public final NetworkCharacterAI.SpeedChecker speedChecker = new NetworkCharacterAI.SpeedChecker();
    private final NetworkCharacterAI.PostponedPacket deadCharacterPacket = new NetworkCharacterAI.PostponedPacket();
    private final NetworkCharacterAI.PostponedPacket vehicleHitPacket = new NetworkCharacterAI.PostponedPacket();
    protected final AnimalPacket animalPacket = new AnimalPacket();
    protected final PlayerPacket playerPacket = new PlayerPacket();
    public byte predictionType;
    protected BaseAction action;
    protected String performingAction;
    protected long noCollisionTime;
    protected boolean wasLocal;
    protected final HitReactionNetworkAI hitReaction;
    private final IsoGameCharacter character;
    public boolean usePathFind;
    public boolean forcePathFinder;
    public Vector2 direction = new Vector2();
    public Vector2 distance = new Vector2();
    public float targetX;
    public float targetY;
    public int targetZ;
    public boolean moved;
    public int switchTime;
    public final AttackRateChecker attackRateChecker = new AttackRateChecker();
    public final Vector3 tempTarget = new Vector3();
    public final NetworkState state = new NetworkState();

    public NetworkState getState() {
        return this.state;
    }

    public void resetState() {
        this.character.StopAllActionQueue();
        this.character.clearVariables();
        this.character.getActionContext().clearActionContextEvents();
        this.character.getActionContext().setCurrentState(this.character.getActionContext().getGroup().findState("idle"));
        this.character.setDefaultState();
        if (GameServer.server) {
            this.character.updateSpeedModifiers();
            this.character.updateMovementRates();
        }

        this.state.reset();
    }

    public void postUpdate() {
        if (this.character.isLocal()) {
            this.state.send();
        } else {
            boolean isTimeout = this.state.timeout();
            if (isTimeout) {
                this.resetState();
            }
        }
    }

    public NetworkCharacterAI(IsoGameCharacter character) {
        this.character = character;
        this.wasLocal = false;
        this.noCollisionTime = 0L;
        this.hitReaction = new HitReactionNetworkAI(character);
        this.predictionType = 0;
        this.speedChecker.reset();
        this.moved = false;
        this.attackRateChecker.reset();
        this.deadCharacterPacket.reset();
        this.vehicleHitPacket.reset();
    }

    public void reset() {
        this.wasLocal = false;
        this.noCollisionTime = 0L;
        this.hitReaction.reset();
        this.predictionType = 0;
        this.speedChecker.reset();
        this.moved = false;
        this.attackRateChecker.reset();
        this.deadCharacterPacket.reset();
        this.vehicleHitPacket.reset();
    }

    public void setLocal(boolean wasLocal) {
        this.wasLocal = wasLocal;
    }

    public boolean wasLocal() {
        return this.wasLocal;
    }

    public void setPerformingAction(String animation) {
        this.performingAction = animation;
    }

    public String getPerformingAction() {
        return this.performingAction;
    }

    public void setAction(BaseAction action) {
        this.action = action;
    }

    public BaseAction getAction() {
        return this.action;
    }

    public void startAction() {
        if (this.action != null) {
            this.action.start();
        }
    }

    public void stopAction() {
        if (this.action != null) {
            this.setOverride(false, null, null);
            this.action.stop();
        }
    }

    public void setOverride(boolean override, String primaryHandModel, String secondaryHandModel) {
        if (this.action != null) {
            this.action.chr.forceNullOverride = override;
            this.action.chr.overridePrimaryHandModel = primaryHandModel;
            this.action.chr.overrideSecondaryHandModel = secondaryHandModel;
            this.action.chr.resetModelNextFrame();
        }
    }

    public void setVehicleHit(INetworkPacket packet) {
        this.vehicleHitPacket.set(packet, System.currentTimeMillis() + 500L);
    }

    public boolean isHitByVehicle() {
        return this.vehicleHitPacket.packet != null;
    }

    public boolean isVehicleHitTimeout() {
        return this.vehicleHitPacket.isTimeout();
    }

    public void hitByVehicle() {
        this.vehicleHitPacket.process();
    }

    public void setCorpse(INetworkPacket packet) {
        this.deadCharacterPacket.set(packet, System.currentTimeMillis() + 5000L);
    }

    public boolean isDeadBodyTimeout() {
        return this.deadCharacterPacket.isTimeout();
    }

    public void becomeCorpse() {
        this.deadCharacterPacket.process();
    }

    public boolean isCollisionEnabled() {
        return this.noCollisionTime == 0L;
    }

    public boolean isNoCollisionTimeout() {
        boolean isTimeout = GameTime.getServerTimeMills() > this.noCollisionTime;
        if (isTimeout) {
            this.setNoCollision(0L);
        }

        return isTimeout;
    }

    public void setNoCollision(long interval) {
        if (interval == 0L) {
            this.noCollisionTime = 0L;
            if (Core.debug) {
                DebugLog.log(DebugType.Multiplayer, "SetNoCollision: disabled");
            }
        } else {
            this.noCollisionTime = GameTime.getServerTimeMills() + interval;
            if (Core.debug) {
                DebugLog.log(DebugType.Multiplayer, "SetNoCollision: enabled for " + interval + " ms");
            }
        }
    }

    public void resetSpeedLimiter() {
        this.speedChecker.reset();
    }

    public short getOnlineID() {
        return this.character.getOnlineID();
    }

    public abstract IsoPlayer getRelatedPlayer();

    public abstract Multiplayer.DebugFlagsOG.IsoGameCharacterOG getBooleanDebugOptions();

    public IsoHutch getHutch() {
        return this.character instanceof IsoAnimal isoAnimal ? isoAnimal.hutch : null;
    }

    public BaseVehicle getVehile() {
        return this.character instanceof IsoAnimal ? this.character.getVehicle() : null;
    }

    public boolean isDead() {
        return this.character.isDead();
    }

    public IsoGameCharacter getCharacter() {
        return this.character;
    }

    public void syncDamage() {
    }

    public void syncStats() {
    }

    public void syncXp() {
    }

    public void syncHealth() {
    }

    public AnimalPacket getAnimalPacket() {
        return this.animalPacket;
    }

    public void setAnimalPacket(UdpConnection receiver) {
    }

    public PlayerPacket getPlayerPacket() {
        return this.playerPacket;
    }

    public static class PostponedPacket {
        private static final long VEHICLE_HIT_DELAY_MS = 500L;
        private static final long DEAD_BODY_DELAY_MS = 5000L;
        private INetworkPacket packet;
        private long timestamp;

        public void set(INetworkPacket packet, long timeout) {
            this.packet = packet;
            this.timestamp = timeout;
        }

        public void reset() {
            this.set(null, 0L);
        }

        public void process() {
            if (this.packet != null) {
                this.packet.processClient(null);
            }

            this.reset();
        }

        public boolean isTimeout() {
            return this.packet != null && System.currentTimeMillis() > this.timestamp;
        }
    }

    public static class SpeedChecker implements IMovable {
        private static final int checkDelay = 5000;
        private static final int checkInterval = 1000;
        private final UpdateLimit updateLimit = new UpdateLimit(5000L);
        private final Vector2 position = new Vector2();
        private boolean isInVehicle;
        private float speed;

        @Override
        public float getSpeed() {
            return this.speed;
        }

        @Override
        public boolean isVehicle() {
            return this.isInVehicle;
        }

        public void set(float x, float y, boolean isInVehicle) {
            if (this.updateLimit.Check()) {
                if (5000L == this.updateLimit.getDelay()) {
                    this.updateLimit.Reset(1000L);
                    this.position.set(0.0F, 0.0F);
                    this.speed = 0.0F;
                }

                this.isInVehicle = isInVehicle;
                if (this.position.getLength() != 0.0F) {
                    this.speed = IsoUtils.DistanceTo(this.position.x, this.position.y, x, y);
                }

                this.position.set(x, y);
            }
        }

        private void reset() {
            this.updateLimit.Reset(5000L);
            this.isInVehicle = false;
            this.position.set(0.0F, 0.0F);
            this.speed = 0.0F;
        }

        public String getDescription() {
            return "SpeedChecker: speed=" + this.speed + " x=" + this.position.x + " y=" + this.position.y + " vehicle=" + this.isInVehicle;
        }
    }
}
