// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;

@UsedFromLua
public final class VehicleEngine {
    private static final double DEFAULT_IDLE_SPEED = 1000.0;
    private static final float MYSTERY_LOUDNESS_SCALE = 0.37037036F;
    private final VehiclePart part;
    private BaseVehicle.engineStateTypes state = BaseVehicle.engineStateTypes.Idle;
    private double speed;
    private int quality;
    private int loudness;
    private int power;
    private long engineCheckTime;
    private long updateStateTime;
    private VehicleEngineStateChangeReason reason;
    private final Set<IVehicleEngineListener> listeners = new HashSet<>();
    private static final long ENGINE_CHECK_INTERVAL_MS = 1000L;
    private static final long RETRY_START_DELAY_MS = 2000L;
    private static final long START_SUCCESS_TRANSITION_MS = 1500L;
    private static final long START_FAILURE_TRANSITION_MS = 1500L;
    private static final long STALLING_TRANSITION_MS = 3000L;
    private static final long SHUTDOWN_TRANSITION_MS = 2000L;
    private static final int AUTO_STALL_CONDITION_THRESHOLD = 50;
    private static final int AUTO_STALL_CHANCE_MULTIPLIER = 12;
    private static final int ENGINE_LOUDNESS_SPEED_DIVISOR = 2500;
    private static final int MAX_ENGINE_SPEED = 2000;
    private static final int SPEED_LOUDNESS_SCALE_DIVISOR = 4000;
    private static final double PRIMARY_SOUND_BASE_CHANCE = 120.0;
    private static final double SECONDARY_SOUND_CHANCE_OFFSET = 85.0;
    private static final double TERTIARY_SOUND_CHANCE_OFFSET = 110.0;
    private static final int MIN_SOUND_RADIUS = 8;
    private static final int MIN_SOUND_VOLUME = 6;
    private static final int MIN_ANIMAL_FLEE_SOUND_RADIUS = PZMath.fastfloor(26.666666F);
    private static final float MIN_BATTERY_CHARGE_TO_START = 0.1F;
    private static final int PERFECT_ENGINE_QUALITY = 100;
    private static final int COLD_START_QUALITY_THRESHOLD = 65;
    private static final float COLD_START_TEMPERATURE_C = 2.0F;
    private static final int WEATHER_PENALTY_PER_DEGREE = 2;
    private static final int MAX_WEATHER_PENALTY = 30;
    private static final int START_FAILURE_BASE_MODIFIER = 50;
    private static final int START_FAILURE_THRESHOLD = 30;

    public VehicleEngine(VehiclePart part) {
        this.part = part;
    }

    public void addListener(IVehicleEngineListener listener) {
        Objects.requireNonNull(listener);
        if (this.listeners.contains(listener)) {
            throw new IllegalArgumentException("already contains listener");
        } else {
            this.listeners.add(listener);
        }
    }

    public void replaceListener(IVehicleEngineListener listenerOld, IVehicleEngineListener listenerNew) {
        Objects.requireNonNull(listenerOld);
        Objects.requireNonNull(listenerNew);
        this.listeners.remove(listenerOld);
        this.listeners.add(listenerNew);
    }

    private void notifyListeners(BaseVehicle.engineStateTypes oldState, BaseVehicle.engineStateTypes newState, VehicleEngineStateChangeReason reason) {
        for (IVehicleEngineListener listener : this.listeners) {
            listener.onEngineStateChanged(oldState, newState, reason);
        }
    }

    public void setFeatures(int quality, int loudness, int power) {
        this.quality = PZMath.clamp(quality, 0, 100);
        this.loudness = (int)(loudness * 0.37037036F);
        this.power = power;
    }

    public int getQuality() {
        return this.quality;
    }

    public void setLoudness(int loudness) {
        this.loudness = loudness;
    }

    public int getLoudness() {
        return this.loudness;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getPower() {
        return this.power;
    }

    public BaseVehicle.engineStateTypes getState() {
        return this.state;
    }

    public VehicleEngineStateChangeReason getStateChangeReason() {
        return this.reason;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void addEngineSpeed(double speed) {
        this.speed += speed;
    }

    public double getSpeed() {
        return this.speed;
    }

    public long getUpdateStateTime() {
        return this.updateStateTime;
    }

    private VehiclePartOwner getOwner() {
        return this.part.getOwner();
    }

    private float getX() {
        return this.part.getX();
    }

    private float getY() {
        return this.part.getY();
    }

    private float getZ() {
        return this.part.getZ();
    }

    private int getXi() {
        return PZMath.fastfloor(this.getX());
    }

    private int getYi() {
        return PZMath.fastfloor(this.getY());
    }

    private int getZi() {
        return PZMath.fastfloor(this.getZ());
    }

    public void load(boolean engineRunning, int loudness, int quality, int power) {
        if (engineRunning) {
            this.state = BaseVehicle.engineStateTypes.Running;
        }

        this.loudness = loudness;
        this.quality = quality;
        this.power = power;
    }

    public void addToWorld() {
        if (this.getState() != BaseVehicle.engineStateTypes.Idle) {
            this.speed = this.getOwner().getScript() == null ? 1000.0 : this.getOwner().getScript().getEngineIdleSpeed();
        }
    }

    public void scriptReloaded() {
        if (this.isRunning()) {
            this.doShuttingDown(VehicleEngineStateChangeReason.ScriptReloaded);
            this.notifyListeners(this.state, this.state = BaseVehicle.engineStateTypes.Idle, VehicleEngineStateChangeReason.ScriptReloaded);
        }
    }

    public void setSmashed() {
        this.quality = 0;
    }

    public void softReset() {
        this.state = BaseVehicle.engineStateTypes.Idle;
    }

    public void update() {
        if (!GameClient.client) {
            long currentMs = System.currentTimeMillis();
            if (currentMs - this.engineCheckTime > 1000L) {
                this.engineCheckTime = currentMs;
                if (this.shouldAutoShutDown()) {
                    this.shutOff(this.getAutoShutDownReason());
                }
            }

            if (this.state == BaseVehicle.engineStateTypes.Starting) {
                this.updateStarting();
            }

            if (this.state == BaseVehicle.engineStateTypes.RetryingStarting && currentMs - this.updateStateTime > 2000L) {
                this.doStarting();
            }

            if (this.state == BaseVehicle.engineStateTypes.StartingSuccess && currentMs - this.updateStateTime > 1500L) {
                this.doRunning();
            }

            if (this.state == BaseVehicle.engineStateTypes.StartingFailed && currentMs - this.updateStateTime > 1500L) {
                this.doIdle();
            }

            if (this.state == BaseVehicle.engineStateTypes.Stalling && currentMs - this.updateStateTime > 3000L) {
                this.doIdle();
            }

            if (this.state == BaseVehicle.engineStateTypes.ShuttingDown && currentMs - this.updateStateTime > 2000L) {
                this.doIdle();
            }

            if (this.state != BaseVehicle.engineStateTypes.Idle) {
                this.updateWorldSounds();
            }

            if (this.shouldAutoStall()) {
                this.shutOff(this.getAutoStallReason());
            }
        }
    }

    private boolean shouldAutoShutDown() {
        return this.state == BaseVehicle.engineStateTypes.Running && !this.getOwner().isEngineWorking();
    }

    private VehicleEngineStateChangeReason getAutoShutDownReason() {
        VehicleEngineStateChangeReason reason = VehicleEngineStateChangeReason.EngineNotWorking;
        if (this.getOwner().getGasRemaining() <= 0.0F) {
            reason = VehicleEngineStateChangeReason.OutOfFuel;
        }

        return reason;
    }

    private boolean shouldAutoStall() {
        int chance = Rand.AdjustForFramerate(this.part.getCondition() * 12);
        return this.state == BaseVehicle.engineStateTypes.Running && this.part.getCondition() < 50 && Rand.Next(chance) == 0;
    }

    private VehicleEngineStateChangeReason getAutoStallReason() {
        return VehicleEngineStateChangeReason.EngineConditionLow;
    }

    private void updateWorldSounds() {
        int newEngineLoudness = (int)(this.loudness * this.speed / 2500.0);
        double maxSpeed = Math.min(this.getSpeed(), 2000.0);
        newEngineLoudness *= (int)(1.0 + maxSpeed / 4000.0);
        double baseRand = 120.0;
        if (GameServer.server) {
            baseRand *= ServerOptions.getInstance().carEngineAttractionModifier.getValue();
            newEngineLoudness = (int)(newEngineLoudness * ServerOptions.getInstance().carEngineAttractionModifier.getValue());
        }

        if (Rand.Next((int)(baseRand * GameTime.instance.getInvMultiplier())) == 0) {
            WorldSoundManager.instance
                .addSoundRepeating(
                    this.getOwner(), this.getXi(), this.getYi(), this.getZi(), Math.max(8, newEngineLoudness), Math.max(6, newEngineLoudness / 3), false, true
                );
        }

        if (Rand.Next((int)((baseRand - 85.0) * GameTime.instance.getInvMultiplier())) == 0) {
            WorldSoundManager.instance
                .addSoundRepeating(
                    this.getOwner(),
                    this.getXi(),
                    this.getYi(),
                    this.getZi(),
                    Math.max(8, newEngineLoudness / 2),
                    Math.max(6, newEngineLoudness / 3),
                    false,
                    true
                );
        }

        if (Rand.Next((int)((baseRand - 110.0) * GameTime.instance.getInvMultiplier())) == 0) {
            WorldSoundManager.instance
                .addSoundRepeating(
                    this.getOwner(),
                    this.getXi(),
                    this.getYi(),
                    this.getZi(),
                    Math.max(8, newEngineLoudness / 4),
                    Math.max(6, newEngineLoudness / 3),
                    false,
                    true
                );
        }

        WorldSoundManager.instance
            .addSoundRepeating(
                this.getOwner(), this.getXi(), this.getYi(), this.getZi(), Math.max(8, newEngineLoudness / 6), Math.max(6, newEngineLoudness / 3), false, true
            );
        this.emitAnimalFleeingWorldSound(newEngineLoudness);
    }

    private void emitAnimalFleeingWorldSound(int loudness) {
        short flags = 1;
        WorldSoundManager.instance
            .addSoundRepeating(
                this.getOwner(),
                this.getXi(),
                this.getYi(),
                this.getZi(),
                Math.max(MIN_ANIMAL_FLEE_SOUND_RADIUS, loudness / 6),
                Math.max(6, loudness / 3),
                (short)1
            );
    }

    private void updateStarting() {
        if (this.getOwner().getBatteryCharge() < 0.1F) {
            this.doStartingFailedNoPower();
        } else if (this.getOwner().getGasRemaining() <= 0.0F) {
            this.doStartingFailed(VehicleEngineStateChangeReason.OutOfFuel);
        } else if (this.shouldFailToStartDueToCold()) {
            this.doStartingFailed(VehicleEngineStateChangeReason.EngineQualityLow);
        } else {
            if (Rand.Next(this.quality) != 0) {
                this.doStartingSuccess();
            } else {
                this.doRetryingStarting();
            }
        }
    }

    private boolean shouldFailToStartDueToCold() {
        if (SandboxOptions.instance.vehicleEasyUse.getValue()) {
            return false;
        } else if (this.quality >= 100) {
            return false;
        } else {
            int weatherAffect = 0;
            float airTemp = ClimateManager.getInstance().getAirTemperatureForSquare(this.getOwner().getSquare());
            if (this.quality < 65 && airTemp <= 2.0F) {
                weatherAffect = (int)Math.min((2.0F - airTemp) * 2.0F, 30.0F);
            }

            return Rand.Next(this.quality + 50 - weatherAffect) <= 30;
        }
    }

    public void doIdle() {
        this.state = BaseVehicle.engineStateTypes.Idle;
        this.updateStateTime = System.currentTimeMillis();
        this.getOwner().transmitEngine();
    }

    public void doStarting() {
        BaseVehicle.engineStateTypes oldState = this.state;
        this.state = BaseVehicle.engineStateTypes.Starting;
        this.updateStateTime = System.currentTimeMillis();
        this.notifyListeners(oldState, this.state, null);
    }

    public boolean isRunning() {
        return this.state == BaseVehicle.engineStateTypes.Running;
    }

    public boolean isStarting() {
        return this.state == BaseVehicle.engineStateTypes.Starting
            || this.state == BaseVehicle.engineStateTypes.StartingFailed
            || this.state == BaseVehicle.engineStateTypes.StartingSuccess;
    }

    public boolean isStarted() {
        return this.state == BaseVehicle.engineStateTypes.Starting
            || this.state == BaseVehicle.engineStateTypes.StartingFailed
            || this.state == BaseVehicle.engineStateTypes.StartingSuccess
            || this.state == BaseVehicle.engineStateTypes.RetryingStarting;
    }

    public void doRetryingStarting() {
        BaseVehicle.engineStateTypes oldState = this.state;
        this.state = BaseVehicle.engineStateTypes.RetryingStarting;
        this.updateStateTime = System.currentTimeMillis();
        this.notifyListeners(oldState, this.state, null);
    }

    public void doStartingSuccess() {
        BaseVehicle.engineStateTypes oldState = this.state;
        this.state = BaseVehicle.engineStateTypes.StartingSuccess;
        this.updateStateTime = System.currentTimeMillis();
        this.notifyListeners(oldState, this.state, null);
    }

    public void doStartingFailed(VehicleEngineStateChangeReason reason) {
        BaseVehicle.engineStateTypes oldState = this.state;
        this.state = BaseVehicle.engineStateTypes.StartingFailed;
        this.updateStateTime = System.currentTimeMillis();
        this.reason = reason;
        this.notifyListeners(oldState, this.state, reason);
    }

    public void doStartingFailedNoPower() {
        this.doStartingFailed(VehicleEngineStateChangeReason.NoPower);
    }

    public void doRunning() {
        BaseVehicle.engineStateTypes oldState = this.state;
        this.state = BaseVehicle.engineStateTypes.Running;
        this.updateStateTime = System.currentTimeMillis();
        this.notifyListeners(oldState, this.state, null);
    }

    public void doStalling() {
        BaseVehicle.engineStateTypes oldState = this.state;
        this.state = BaseVehicle.engineStateTypes.Stalling;
        this.updateStateTime = System.currentTimeMillis();
        this.notifyListeners(oldState, this.state, null);
    }

    public void doShuttingDown() {
        this.doShuttingDown(VehicleEngineStateChangeReason.TurnedOff);
    }

    public void doShuttingDown(VehicleEngineStateChangeReason reason) {
        BaseVehicle.engineStateTypes oldState = this.state;
        this.state = BaseVehicle.engineStateTypes.ShuttingDown;
        this.updateStateTime = System.currentTimeMillis();
        this.reason = reason;
        this.notifyListeners(oldState, this.state, reason);
    }

    public void shutOff() {
        this.shutOff(VehicleEngineStateChangeReason.TurnedOff);
    }

    public void shutOff(VehicleEngineStateChangeReason reason) {
        if (this.getOwner().getGasRemaining() <= 0.0F) {
            this.doStalling();
        } else {
            this.doShuttingDown(reason);
        }
    }
}
