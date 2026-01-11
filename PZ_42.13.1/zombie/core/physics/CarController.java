// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.physics;

import java.util.ArrayList;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.input.GameKeyboard;
import zombie.input.JoypadManager;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.pathfind.VehiclePoly;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.VehicleScript;
import zombie.ui.UIManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.EngineRPMData;
import zombie.vehicles.TransmissionNumber;

/**
 * Created by LEMMYCOOLER on 17/04/14.
 */
public final class CarController {
    public final BaseVehicle vehicleObject;
    public float clientForce;
    public float engineForce;
    public float brakingForce;
    private float vehicleSteering;
    private boolean isGas;
    private boolean isGasR;
    private boolean isBreak;
    private float atRestTimer = -1.0F;
    private float regulatorTimer;
    public boolean isEnable;
    private final Transform tempXfrm = new Transform();
    private final Vector2 tempVec2 = new Vector2();
    private final Vector3f tempVec3f = new Vector3f();
    private final Vector3f tempVec3f2 = new Vector3f();
    private final Vector3f tempVec3f3 = new Vector3f();
    private static final Vector3f _UNIT_Y = new Vector3f(0.0F, 1.0F, 0.0F);
    public boolean acceleratorOn;
    public boolean brakeOn;
    public float speed;
    public static CarController.GearInfo[] gears = new CarController.GearInfo[3];
    public final CarController.ClientControls clientControls = new CarController.ClientControls();
    private boolean engineStartingFromKeyboard;
    private static final CarController.BulletVariables bulletVariables = new CarController.BulletVariables();
    float drunkDelayCommandTimer;
    boolean wasBreaking;
    boolean wasGas;
    boolean wasGasR;
    boolean wasSteering;

    public CarController(BaseVehicle vehicleObject) {
        this.vehicleObject = vehicleObject;
        this.engineStartingFromKeyboard = false;
        VehicleScript script = vehicleObject.getScript();
        float physicsZ = vehicleObject.savedPhysicsZ;
        if (Float.isNaN(physicsZ)) {
            float wheelBottom = 0.0F;
            if (script.getWheelCount() > 0) {
                Vector3f modelOffset = script.getModelOffset();
                wheelBottom += modelOffset.y();
                wheelBottom += script.getWheel(0).getOffset().y() - script.getWheel(0).radius;
            }

            float chassisBottom = script.getCenterOfMassOffset().y() - script.getExtents().y() / 2.0F;
            physicsZ = PZMath.fastfloor(vehicleObject.getZ()) * 3 * 0.8164967F - Math.min(wheelBottom, chassisBottom);
            if (script.getWheelCount() == 0) {
                physicsZ = PZMath.max(physicsZ, PZMath.fastfloor(vehicleObject.getZ()) * 3 * 0.8164967F + 0.1F);
            }

            vehicleObject.jniTransform.origin.y = physicsZ;
        }

        if (!GameServer.server) {
            Bullet.addVehicle(
                vehicleObject.vehicleId,
                vehicleObject.getX(),
                vehicleObject.getY(),
                physicsZ,
                vehicleObject.savedRot.x,
                vehicleObject.savedRot.y,
                vehicleObject.savedRot.z,
                vehicleObject.savedRot.w,
                script.getFullName()
            );
            vehicleObject.setPhysicsActive(!vehicleObject.isNetPlayerAuthorization(BaseVehicle.Authorization.Remote));
        }
    }

    public CarController.GearInfo findGear(float speed) {
        for (int i = 0; i < gears.length; i++) {
            if (speed >= gears[i].minSpeed && speed < gears[i].maxSpeed) {
                return gears[i];
            }
        }

        return null;
    }

    public void accelerator(boolean apply) {
        this.acceleratorOn = apply;
    }

    public void brake(boolean apply) {
        this.brakeOn = apply;
    }

    public CarController.ClientControls getClientControls() {
        return this.clientControls;
    }

    public void update() {
        if (this.vehicleObject.getVehicleTowedBy() == null) {
            VehicleScript script = this.vehicleObject.getScript();
            this.speed = this.vehicleObject.getCurrentSpeedKmHour();
            boolean bDrunkDriver = this.vehicleObject.getDriver() != null && this.vehicleObject.getDriver().getMoodles().getMoodleLevel(MoodleType.DRUNK) > 1;
            float dot = 0.0F;
            Vector3f velocity = this.vehicleObject.getLinearVelocity(this.tempVec3f2);
            velocity.y = 0.0F;
            if (velocity.length() > 0.5) {
                velocity.normalize();
                Vector3f forward = this.tempVec3f;
                this.vehicleObject.getForwardVector(forward);
                dot = velocity.dot(forward);
            }

            float limitMod = 1.0F;
            if (GameClient.client) {
                float delta = this.vehicleObject.getCurrentSpeedKmHour() / Math.min(120.0F, (float)ServerOptions.instance.speedLimit.getValue());
                delta *= delta;
                limitMod = GameTime.getInstance().Lerp(1.0F, BaseVehicle.getFakeSpeedModifier(), delta);
            }

            float speedLimited = this.vehicleObject.getCurrentSpeedKmHour() * limitMod;
            this.isGas = false;
            this.isGasR = false;
            this.isBreak = false;
            if (this.clientControls.forward) {
                if (dot < 0.0F) {
                    this.isBreak = true;
                }

                if (dot >= 0.0F) {
                    this.isGas = true;
                }

                this.isGasR = false;
            }

            if (this.clientControls.backward) {
                if (dot > 0.0F) {
                    this.isBreak = true;
                }

                if (dot <= 0.0F) {
                    this.isGasR = true;
                }

                this.isGas = false;
            }

            if (this.clientControls.brake) {
                this.isBreak = true;
                this.isGas = false;
                this.isGasR = false;
            }

            if (this.clientControls.forward && this.clientControls.backward) {
                this.isBreak = true;
                this.isGas = false;
                this.isGasR = false;
            }

            if (bDrunkDriver && this.vehicleObject.engineState != BaseVehicle.engineStateTypes.Idle) {
                if (this.isBreak && !this.wasBreaking) {
                    this.isBreak = this.delayCommandWhileDrunk(this.isBreak);
                }

                if (this.isGas && !this.wasGas) {
                    this.isGas = this.delayCommandWhileDrunk(this.isGas);
                }

                if (this.isGasR && !this.wasGasR) {
                    this.isGasR = this.delayCommandWhileDrunk(this.isGas);
                }

                if (this.clientControls.steering != 0.0F && !this.wasSteering) {
                    this.clientControls.steering = this.delayCommandWhileDrunk(this.clientControls.steering);
                }
            }

            this.updateRegulator();
            this.wasBreaking = this.isBreak;
            this.wasGas = this.isGas;
            this.wasGasR = this.isGasR;
            this.wasSteering = this.clientControls.steering != 0.0F;
            if (!this.isGasR && this.vehicleObject.isInvalidChunkAhead()) {
                this.isBreak = true;
                this.isGas = false;
                this.isGasR = false;
            } else if (!this.isGas && this.vehicleObject.isInvalidChunkBehind()) {
                this.isBreak = true;
                this.isGas = false;
                this.isGasR = false;
            }

            if (this.clientControls.shift) {
                this.isGas = false;
                this.isBreak = false;
                this.isGasR = false;
                this.clientControls.wasUsingParkingBrakes = false;
            }

            float throttle = this.vehicleObject.throttle;
            if (!this.isGas && !this.isGasR) {
                throttle -= GameTime.getInstance().getMultiplier() / 30.0F;
            } else {
                throttle += GameTime.getInstance().getMultiplier() / 30.0F;
            }

            if (throttle < 0.0F) {
                throttle = 0.0F;
            }

            if (throttle > 1.0F) {
                throttle = 1.0F;
            }

            if (this.vehicleObject.isRegulator() && !this.isGas && !this.isGasR) {
                throttle = 0.5F;
                if (speedLimited < this.vehicleObject.getRegulatorSpeed()) {
                    this.isGas = true;
                }
            }

            this.vehicleObject.throttle = throttle;
            float fpsMod = GameTime.getInstance().getMultiplier() / 0.8F;
            CarController.ControlState controlState = CarController.ControlState.NoControl;
            if (this.isBreak) {
                controlState = CarController.ControlState.Braking;
            } else if (this.isGas && !this.isGasR) {
                controlState = CarController.ControlState.Forward;
            } else if (!this.isGas && this.isGasR) {
                controlState = CarController.ControlState.Reverse;
            }

            if (controlState != CarController.ControlState.NoControl) {
                UIManager.speedControls.SetCurrentGameSpeed(1);
            }

            if (controlState == CarController.ControlState.NoControl) {
                this.control_NoControl();
            }

            if (controlState == CarController.ControlState.Reverse) {
                this.control_Reverse(speedLimited);
            }

            if (controlState == CarController.ControlState.Forward) {
                this.control_ForwardNew(speedLimited);
            }

            this.updateBackSignal();
            if (controlState == CarController.ControlState.Braking) {
                this.control_Braking();
            }

            this.updateBrakeLights();
            BaseVehicle vehicleTowedBy = this.vehicleObject.getVehicleTowedBy();
            if (vehicleTowedBy != null && vehicleTowedBy.getDriver() == null && this.vehicleObject.getDriver() != null && !GameClient.client) {
                this.vehicleObject.addPointConstraint(null, vehicleTowedBy, this.vehicleObject.getTowAttachmentSelf(), vehicleTowedBy.getTowAttachmentSelf());
            }

            this.updateRammingSound(speedLimited);
            if (Math.abs(this.clientControls.steering) > 0.1F) {
                float delta = 1.0F - this.speed / this.vehicleObject.getMaxSpeed();
                if (delta < 0.1F) {
                    delta = 0.1F;
                }

                this.vehicleSteering = this.vehicleSteering - (this.clientControls.steering + this.vehicleSteering) * 0.06F * fpsMod * delta;
            } else if (Math.abs(this.vehicleSteering) <= 0.04) {
                this.vehicleSteering = 0.0F;
            } else if (this.vehicleSteering > 0.0F) {
                this.vehicleSteering -= 0.04F * fpsMod;
                this.vehicleSteering = Math.max(this.vehicleSteering, 0.0F);
            } else {
                this.vehicleSteering += 0.04F * fpsMod;
                this.vehicleSteering = Math.min(this.vehicleSteering, 0.0F);
            }

            float steeringClamp = script.getSteeringClamp(this.speed);
            this.vehicleSteering = PZMath.clamp(this.vehicleSteering, -steeringClamp, steeringClamp);
            CarController.BulletVariables bv = bulletVariables.set(this.vehicleObject, this.engineForce, this.brakingForce, this.vehicleSteering);
            this.checkTire(bv);
            this.engineForce = bv.engineForce;
            this.brakingForce = bv.brakingForce;
            this.vehicleSteering = bv.vehicleSteering;
            if (this.vehicleObject.isDoingOffroad()) {
                int tNumb = this.vehicleObject.getTransmissionNumber();
                if (tNumb <= 0) {
                    tNumb = 1;
                }

                float mult1 = this.vehicleObject.getVehicleTowing() == null ? 0.6F : 0.8F;
                limitMod = PZMath.lerp(0.0F, 1.0F, 1.0F - PZMath.clamp(tNumb - 1, 0, 7) / 15.0F)
                    * mult1
                    * this.vehicleObject.getScript().getOffroadEfficiency();
                this.engineForce *= limitMod;
            }

            this.vehicleObject.setCurrentSteering(this.vehicleSteering);
            this.vehicleObject.setBraking(this.isBreak);
            if (!GameServer.server) {
                this.checkShouldBeActive();
                Bullet.controlVehicle(this.vehicleObject.vehicleId, this.engineForce, this.brakingForce, this.vehicleSteering);
                if (this.engineForce > 0.0F && this.vehicleObject.engineState == BaseVehicle.engineStateTypes.Idle && !this.engineStartingFromKeyboard) {
                    this.engineStartingFromKeyboard = true;
                    if (GameClient.client) {
                        Boolean haveKey = this.vehicleObject.getDriver().getInventory().haveThisKeyId(this.vehicleObject.getKeyId()) != null
                            ? Boolean.TRUE
                            : Boolean.FALSE;
                        GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "startEngine", "haveKey", haveKey);
                    } else if (!GameClient.client && !GameServer.server) {
                        Boolean haveKey = this.vehicleObject.getDriver().getInventory().haveThisKeyId(this.vehicleObject.getKeyId()) != null
                            ? Boolean.TRUE
                            : Boolean.FALSE;
                        this.vehicleObject.tryStartEngine(haveKey);
                    } else {
                        this.vehicleObject.tryStartEngine();
                    }
                }

                if (this.engineStartingFromKeyboard && this.engineForce == 0.0F) {
                    this.engineStartingFromKeyboard = false;
                }
            }

            if (this.vehicleObject.engineState != BaseVehicle.engineStateTypes.Running) {
                this.acceleratorOn = false;
                if (!GameServer.server && this.vehicleObject.getCurrentSpeedKmHour() > 5.0F && this.vehicleObject.getScript().getWheelCount() > 0) {
                    Bullet.controlVehicle(this.vehicleObject.vehicleId, 0.0F, this.brakingForce, this.vehicleSteering);
                } else {
                    this.park();
                }
            }
        }
    }

    public void updateTrailer() {
        BaseVehicle vehicleTowedBy = this.vehicleObject.getVehicleTowedBy();
        if (vehicleTowedBy != null) {
            if (GameServer.server) {
                if (vehicleTowedBy.getDriver() == null && this.vehicleObject.getDriver() != null) {
                    this.vehicleObject
                        .addPointConstraint(null, vehicleTowedBy, this.vehicleObject.getTowAttachmentSelf(), vehicleTowedBy.getTowAttachmentSelf());
                }
            } else {
                this.speed = this.vehicleObject.getCurrentSpeedKmHour();
                this.isGas = false;
                this.isGasR = false;
                this.isBreak = false;
                this.wasGas = false;
                this.wasGasR = false;
                this.wasBreaking = false;
                this.vehicleObject.throttle = 0.0F;
                if (vehicleTowedBy.getDriver() == null && this.vehicleObject.getDriver() != null && !GameClient.client) {
                    this.vehicleObject
                        .addPointConstraint(null, vehicleTowedBy, this.vehicleObject.getTowAttachmentSelf(), vehicleTowedBy.getTowAttachmentSelf());
                } else {
                    this.checkShouldBeActive();
                    this.engineForce = 0.0F;
                    this.brakingForce = 0.0F;
                    this.vehicleSteering = 0.0F;
                    if (!this.vehicleObject.getScriptName().contains("Trailer")) {
                        this.brakingForce = 10.0F;
                    }

                    Bullet.controlVehicle(this.vehicleObject.vehicleId, this.engineForce, this.brakingForce, this.vehicleSteering);
                }
            }
        }
    }

    private void updateRegulator() {
        if (this.regulatorTimer > 0.0F) {
            this.regulatorTimer = this.regulatorTimer - GameTime.getInstance().getThirtyFPSMultiplier();
        }

        if (this.clientControls.shift) {
            if (this.clientControls.forward && this.regulatorTimer <= 0.0F) {
                if (this.vehicleObject.getRegulatorSpeed() < this.vehicleObject.getMaxSpeed() + 20.0F
                    && (!this.vehicleObject.isRegulator() && this.vehicleObject.getRegulatorSpeed() == 0.0F || this.vehicleObject.isRegulator())) {
                    if (this.vehicleObject.getRegulatorSpeed() == 0.0F
                        && this.vehicleObject.getCurrentSpeedForRegulator() != this.vehicleObject.getRegulatorSpeed()) {
                        this.vehicleObject.setRegulatorSpeed(this.vehicleObject.getCurrentSpeedForRegulator());
                    } else {
                        this.vehicleObject.setRegulatorSpeed(this.vehicleObject.getRegulatorSpeed() + 5.0F);
                    }
                }

                this.vehicleObject.setRegulator(true);
                this.regulatorTimer = 20.0F;
            } else if (this.clientControls.backward && this.regulatorTimer <= 0.0F) {
                this.regulatorTimer = 20.0F;
                if (this.vehicleObject.getRegulatorSpeed() >= 5.0F
                    && (!this.vehicleObject.isRegulator() && this.vehicleObject.getRegulatorSpeed() == 0.0F || this.vehicleObject.isRegulator())) {
                    this.vehicleObject.setRegulatorSpeed(this.vehicleObject.getRegulatorSpeed() - 5.0F);
                }

                this.vehicleObject.setRegulator(true);
                if (this.vehicleObject.getRegulatorSpeed() <= 0.0F) {
                    this.vehicleObject.setRegulatorSpeed(0.0F);
                    this.vehicleObject.setRegulator(false);
                }
            }
        } else if (this.isGasR || this.isBreak) {
            this.vehicleObject.setRegulator(false);
        }
    }

    public void control_NoControl() {
        float fpsMult = GameTime.getInstance().getMultiplier() / 0.8F;
        if (!this.vehicleObject.isEngineRunning()) {
            if (this.vehicleObject.engineSpeed > 0.0) {
                this.vehicleObject.engineSpeed = Math.max(this.vehicleObject.engineSpeed - 50.0F * fpsMult, 0.0);
            }
        } else if (this.vehicleObject.engineSpeed > this.vehicleObject.getScript().getEngineIdleSpeed()) {
            if (!this.vehicleObject.isRegulator()) {
                this.vehicleObject.engineSpeed -= 20.0F * fpsMult;
            }
        } else {
            this.vehicleObject.engineSpeed += 20.0F * fpsMult;
        }

        if (!this.vehicleObject.isRegulator()) {
            this.vehicleObject.transmissionNumber = TransmissionNumber.N;
        }

        this.engineForce = 0.0F;
        if (this.vehicleObject.engineSpeed > 1000.0) {
            this.brakingForce = 15.0F;
        } else {
            this.brakingForce = 10.0F;
        }
    }

    private void control_Braking() {
        float fpsMult = GameTime.getInstance().getMultiplier() / 0.8F;
        if (this.vehicleObject.engineSpeed > this.vehicleObject.getScript().getEngineIdleSpeed()) {
            this.vehicleObject.engineSpeed = this.vehicleObject.engineSpeed - Rand.Next(10, 30) * fpsMult;
        } else {
            this.vehicleObject.engineSpeed = this.vehicleObject.engineSpeed + Rand.Next(20) * fpsMult;
        }

        this.vehicleObject.transmissionNumber = TransmissionNumber.N;
        this.engineForce = 0.0F;
        this.brakingForce = this.vehicleObject.getBrakingForce();
        if (this.clientControls.brake) {
            this.brakingForce *= 13.0F;
        }
    }

    private void control_Forward(float speed) {
        float fpsMult = GameTime.getInstance().getMultiplier() / 0.8F;
        IsoGameCharacter driver = this.vehicleObject.getDriver();
        boolean trainFastDriver = driver != null && driver.hasTrait(CharacterTrait.SPEED_DEMON);
        boolean trainSlowDriver = driver != null && driver.hasTrait(CharacterTrait.SUNDAY_DRIVER);
        int gearRatioCount = this.vehicleObject.getScript().gearRatioCount;
        float engineSpeedCalc = 0.0F;
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.N) {
            this.vehicleObject.transmissionNumber = TransmissionNumber.Speed1;
            boolean isChangeTransmission = false;

            while (true) {
                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed1) {
                    engineSpeedCalc = 3000.0F * speed / 30.0F;
                }

                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed2) {
                    engineSpeedCalc = 3000.0F * speed / 40.0F;
                }

                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed3) {
                    engineSpeedCalc = 3000.0F * speed / 60.0F;
                }

                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed4) {
                    engineSpeedCalc = 3000.0F * speed / 85.0F;
                }

                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed5) {
                    engineSpeedCalc = 3000.0F * speed / 105.0F;
                }

                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed6) {
                    engineSpeedCalc = 3000.0F * speed / 130.0F;
                }

                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed7) {
                    engineSpeedCalc = 3000.0F * speed / 160.0F;
                }

                if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed8) {
                    engineSpeedCalc = 3000.0F * speed / 200.0F;
                }

                if (engineSpeedCalc > 3000.0F) {
                    this.vehicleObject.changeTransmission(this.vehicleObject.transmissionNumber.getNext(gearRatioCount));
                    isChangeTransmission = true;
                }

                if (!isChangeTransmission || this.vehicleObject.transmissionNumber.getIndex() >= gearRatioCount) {
                    break;
                }

                isChangeTransmission = false;
            }
        }

        if (this.vehicleObject.engineSpeed > 3000.0 && this.vehicleObject.transmissionChangeTime.Check()) {
            this.vehicleObject.changeTransmission(this.vehicleObject.transmissionNumber.getNext(gearRatioCount));
        }

        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed1) {
            engineSpeedCalc = 3000.0F * speed / 30.0F;
        }

        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed2) {
            engineSpeedCalc = 3000.0F * speed / 40.0F;
        }

        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed3) {
            engineSpeedCalc = 3000.0F * speed / 60.0F;
        }

        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed4) {
            engineSpeedCalc = 3000.0F * speed / 85.0F;
        }

        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed5) {
            engineSpeedCalc = 3000.0F * speed / 105.0F;
        }

        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed6) {
            engineSpeedCalc = 3000.0F * speed / 130.0F;
        }

        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed7) {
            engineSpeedCalc = 3000.0F * speed / 160.0F;
        }

        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed8) {
            engineSpeedCalc = 3000.0F * speed / 200.0F;
        }

        this.vehicleObject.engineSpeed = this.vehicleObject.engineSpeed - Math.min(0.5 * (this.vehicleObject.engineSpeed - engineSpeedCalc), 100.0) * fpsMult;
        if (trainFastDriver) {
            if (speed < 50.0F) {
                this.vehicleObject.engineSpeed = this.vehicleObject.engineSpeed
                    - Math.min(0.06 * (this.vehicleObject.engineSpeed - 7000.0), (double)(30.0F - speed)) * fpsMult;
            }
        } else if (speed < 30.0F) {
            this.vehicleObject.engineSpeed = this.vehicleObject.engineSpeed
                - Math.min(0.02 * (this.vehicleObject.engineSpeed - 7000.0), (double)(30.0F - speed)) * fpsMult;
        }

        this.engineForce = (float)(this.vehicleObject.getEnginePower() * (0.5 + this.vehicleObject.engineSpeed / 24000.0));
        this.engineForce = this.engineForce - this.engineForce * (speed / 200.0F);
        boolean towingBurntVehicle = false;
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed1 && this.vehicleObject.getVehicleTowedBy() != null) {
            if (this.vehicleObject.getVehicleTowedBy().getScript().getPassengerCount() == 0
                && this.vehicleObject.getVehicleTowedBy().getScript().getMass() > 200.0F) {
                towingBurntVehicle = true;
            }

            if (speed < (towingBurntVehicle ? 20 : 5)) {
                this.engineForce = this.engineForce * Math.min(1.2F, this.vehicleObject.getVehicleTowedBy().getMass() / 500.0F);
                if (towingBurntVehicle) {
                    this.engineForce *= 4.0F;
                }
            }
        }

        if (this.vehicleObject.engineSpeed > 6000.0) {
            this.engineForce = (float)(this.engineForce * ((7000.0 - this.vehicleObject.engineSpeed) / 1000.0));
        }

        if (trainSlowDriver) {
            this.engineForce *= 0.75F;
            if (speed > this.vehicleObject.getMaxSpeed() * 0.6F) {
                this.engineForce = this.engineForce * ((this.vehicleObject.getMaxSpeed() * 0.75F + 20.0F - speed) / 20.0F);
            }
        }

        if (trainFastDriver) {
            if (speed > this.vehicleObject.getMaxSpeed() * 1.15F) {
                this.engineForce = this.engineForce * ((this.vehicleObject.getMaxSpeed() * 1.15F + 20.0F - speed) / 20.0F);
            }
        } else if (speed > this.vehicleObject.getMaxSpeed()) {
            this.engineForce = this.engineForce * ((this.vehicleObject.getMaxSpeed() + 20.0F - speed) / 20.0F);
        }

        this.brakingForce = 0.0F;
        if (this.clientControls.wasUsingParkingBrakes) {
            this.clientControls.wasUsingParkingBrakes = false;
            this.engineForce *= 8.0F;
        }

        if (GameClient.client && this.vehicleObject.getCurrentSpeedKmHour() >= ServerOptions.instance.speedLimit.getValue()) {
            this.engineForce = 0.0F;
        }
    }

    private void control_ForwardNew(float speed) {
        float fpsMult = GameTime.getInstance().getMultiplier() / 0.8F;
        IsoGameCharacter driver = this.vehicleObject.getDriver();
        boolean trainFastDriver = driver != null && driver.hasTrait(CharacterTrait.SPEED_DEMON);
        boolean trainSlowDriver = driver != null && driver.hasTrait(CharacterTrait.SUNDAY_DRIVER);
        int gearRatioCount = this.vehicleObject.getScript().gearRatioCount;
        float engineSpeedCalc = 0.0F;
        EngineRPMData[] CarRPMData = this.vehicleObject.getVehicleEngineRPM().rpmData;
        float speedPerGear = this.vehicleObject.getMaxSpeed() / gearRatioCount;
        float speedClamped = PZMath.clamp(speed, 0.0F, this.vehicleObject.getMaxSpeed());
        int gearForSpeed = (int)PZMath.floor(speedClamped / speedPerGear) + 1;
        gearForSpeed = PZMath.min(gearForSpeed, gearRatioCount);
        engineSpeedCalc = CarRPMData[gearForSpeed - 1].gearChange;
        TransmissionNumber targetTransmissionNumber = TransmissionNumber.Speed1;
        switch (gearForSpeed) {
            case 1:
                targetTransmissionNumber = TransmissionNumber.Speed1;
                break;
            case 2:
                targetTransmissionNumber = TransmissionNumber.Speed2;
                break;
            case 3:
                targetTransmissionNumber = TransmissionNumber.Speed3;
                break;
            case 4:
                targetTransmissionNumber = TransmissionNumber.Speed4;
                break;
            case 5:
                targetTransmissionNumber = TransmissionNumber.Speed5;
                break;
            case 6:
                targetTransmissionNumber = TransmissionNumber.Speed6;
                break;
            case 7:
                targetTransmissionNumber = TransmissionNumber.Speed7;
                break;
            case 8:
                targetTransmissionNumber = TransmissionNumber.Speed8;
        }

        if (this.vehicleObject.transmissionNumber == TransmissionNumber.N) {
            this.vehicleObject.transmissionNumber = targetTransmissionNumber;
        } else if (this.vehicleObject.transmissionNumber.getIndex() - 1 >= 0
            && this.vehicleObject.transmissionNumber.getIndex() < targetTransmissionNumber.getIndex()
            && this.vehicleObject.getEngineSpeed() >= CarRPMData[this.vehicleObject.transmissionNumber.getIndex() - 1].gearChange
            && speed >= speedPerGear * this.vehicleObject.transmissionNumber.getIndex()) {
            this.vehicleObject.transmissionNumber = targetTransmissionNumber;
            this.vehicleObject.engineSpeed = CarRPMData[this.vehicleObject.transmissionNumber.getIndex() - 1].afterGearChange;
        }

        if (this.vehicleObject.transmissionNumber.getIndex() < gearRatioCount && this.vehicleObject.transmissionNumber.getIndex() - 1 >= 0) {
            this.vehicleObject.engineSpeed = Math.min(
                this.vehicleObject.engineSpeed, (double)(CarRPMData[this.vehicleObject.transmissionNumber.getIndex() - 1].gearChange + 100.0F)
            );
        }

        if (this.vehicleObject.engineSpeed > engineSpeedCalc) {
            this.vehicleObject.engineSpeed = this.vehicleObject.engineSpeed
                - Math.min(0.5 * (this.vehicleObject.engineSpeed - engineSpeedCalc), 10.0) * fpsMult;
        } else {
            float rpmIncrease = switch (this.vehicleObject.transmissionNumber) {
                case Speed1 -> 10.0F;
                case Speed2 -> 8.0F;
                case Speed3 -> 7.0F;
                case Speed4 -> 6.0F;
                case Speed5 -> 5.0F;
                default -> 4.0F;
            };
            this.vehicleObject.engineSpeed += rpmIncrease * fpsMult;
        }

        float enginePower = this.vehicleObject.getEnginePower();
        enginePower = this.vehicleObject.getScript().getEngineForce();

        enginePower *= switch (this.vehicleObject.transmissionNumber) {
            case Speed1 -> 1.5F;
            default -> 1.0F;
        };
        this.engineForce = (float)(enginePower * (0.3F + this.vehicleObject.engineSpeed / 30000.0));
        this.engineForce = this.engineForce - this.engineForce * (speed / 200.0F);
        boolean towingBurntVehicle = false;
        if (this.vehicleObject.transmissionNumber == TransmissionNumber.Speed1 && this.vehicleObject.getVehicleTowedBy() != null) {
            if (this.vehicleObject.getVehicleTowedBy().getScript().getPassengerCount() == 0
                && this.vehicleObject.getVehicleTowedBy().getScript().getMass() > 200.0F) {
                towingBurntVehicle = true;
            }

            if (speed < (towingBurntVehicle ? 20 : 5)) {
                this.engineForce = this.engineForce * Math.min(1.2F, this.vehicleObject.getVehicleTowedBy().getMass() / 500.0F);
                if (towingBurntVehicle) {
                    this.engineForce *= 4.0F;
                }
            }
        }

        if (this.vehicleObject.engineSpeed > 6000.0) {
            this.engineForce = (float)(this.engineForce * ((7000.0 - this.vehicleObject.engineSpeed) / 1000.0));
        }

        if (trainSlowDriver) {
            this.engineForce *= 0.75F;
            if (speed > this.vehicleObject.getMaxSpeed() * 0.6F) {
                this.engineForce = this.engineForce * ((this.vehicleObject.getMaxSpeed() * 0.75F + 20.0F - speed) / 20.0F);
            }
        }

        if (trainFastDriver) {
            if (speed > this.vehicleObject.getMaxSpeed() * 1.15F) {
                this.engineForce = this.engineForce * ((this.vehicleObject.getMaxSpeed() * 1.15F + 20.0F - speed) / 20.0F);
            }
        } else if (speed > this.vehicleObject.getMaxSpeed()) {
            this.engineForce = this.engineForce * ((this.vehicleObject.getMaxSpeed() + 20.0F - speed) / 20.0F);
        }

        this.brakingForce = 0.0F;
        if (this.clientControls.wasUsingParkingBrakes) {
            this.clientControls.wasUsingParkingBrakes = false;
            this.engineForce *= 8.0F;
        }

        if (GameClient.client && this.vehicleObject.getCurrentSpeedKmHour() >= ServerOptions.instance.speedLimit.getValue()) {
            this.engineForce = 0.0F;
        }
    }

    private void control_Reverse(float speed) {
        float fpsMult = GameTime.getInstance().getMultiplier() / 0.8F;
        speed *= 1.5F;
        IsoGameCharacter driver = this.vehicleObject.getDriver();
        boolean trainFastDriver = driver != null && driver.hasTrait(CharacterTrait.SPEED_DEMON);
        boolean trainSlowDriver = driver != null && driver.hasTrait(CharacterTrait.SUNDAY_DRIVER);
        this.vehicleObject.transmissionNumber = TransmissionNumber.R;
        float engineSpeedCalc = 1000.0F * speed / 30.0F;
        this.vehicleObject.engineSpeed = this.vehicleObject.engineSpeed - Math.min(0.5 * (this.vehicleObject.engineSpeed - engineSpeedCalc), 100.0) * fpsMult;
        if (trainFastDriver) {
            this.vehicleObject.engineSpeed = this.vehicleObject.engineSpeed
                - Math.min(0.06 * (this.vehicleObject.engineSpeed - 7000.0), (double)(30.0F - speed)) * fpsMult;
        } else {
            this.vehicleObject.engineSpeed = this.vehicleObject.engineSpeed
                - Math.min(0.02 * (this.vehicleObject.engineSpeed - 7000.0), (double)(30.0F - speed)) * fpsMult;
        }

        this.engineForce = (float)(-1.0F * this.vehicleObject.getEnginePower() * (0.75 + this.vehicleObject.engineSpeed / 24000.0));
        if (this.vehicleObject.engineSpeed > 6000.0) {
            this.engineForce = (float)(this.engineForce * ((7000.0 - this.vehicleObject.engineSpeed) / 1000.0));
        }

        if (trainSlowDriver) {
            this.engineForce *= 0.7F;
            if (speed < -5.0F) {
                this.engineForce *= (15.0F + speed) / 10.0F;
            }
        }

        if (speed < -1.0F * this.vehicleObject.getScript().maxSpeedReverse) {
            this.engineForce = 0.0F;
        }

        this.brakingForce = 0.0F;
    }

    private void updateRammingSound(float speed) {
        if (this.vehicleObject.isEngineRunning()
            && (
                speed < 1.0F && this.engineForce > this.vehicleObject.getScript().getEngineIdleSpeed() * 2.0F
                    || speed > -0.5F && this.engineForce < this.vehicleObject.getScript().getEngineIdleSpeed() * -2.0F
            )) {
            if (this.vehicleObject.ramSound == 0L) {
                this.vehicleObject.ramSound = this.vehicleObject.playSoundImpl("VehicleSkid", null);
                this.vehicleObject.ramSoundTime = System.currentTimeMillis() + 1000L + Rand.Next(2000);
            }

            if (this.vehicleObject.ramSound != 0L && this.vehicleObject.ramSoundTime < System.currentTimeMillis()) {
                this.vehicleObject.stopSound(this.vehicleObject.ramSound);
                this.vehicleObject.ramSound = 0L;
            }
        } else if (this.vehicleObject.ramSound != 0L) {
            this.vehicleObject.stopSound(this.vehicleObject.ramSound);
            this.vehicleObject.ramSound = 0L;
        }
    }

    private void updateBackSignal() {
        if (this.isGasR && this.vehicleObject.isEngineRunning() && this.vehicleObject.hasBackSignal() && !this.vehicleObject.isBackSignalEmitting()) {
            if (GameClient.client) {
                GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "onBackSignal", "state", "start");
            } else {
                this.vehicleObject.onBackMoveSignalStart();
            }
        }

        if (!this.isGasR && this.vehicleObject.isBackSignalEmitting()) {
            if (GameClient.client) {
                GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "onBackSignal", "state", "stop");
            } else {
                this.vehicleObject.onBackMoveSignalStop();
            }
        }
    }

    private void updateBrakeLights() {
        if (this.isBreak) {
            if (this.vehicleObject.getStoplightsOn()) {
                return;
            }

            if (GameClient.client) {
                GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "setStoplightsOn", "on", Boolean.TRUE);
            }

            if (!GameServer.server) {
                this.vehicleObject.setStoplightsOn(true);
            }
        } else {
            if (!this.vehicleObject.getStoplightsOn()) {
                return;
            }

            if (GameClient.client) {
                GameClient.instance.sendClientCommandV((IsoPlayer)this.vehicleObject.getDriver(), "vehicle", "setStoplightsOn", "on", Boolean.FALSE);
            }

            if (!GameServer.server) {
                this.vehicleObject.setStoplightsOn(false);
            }
        }
    }

    private boolean delayCommandWhileDrunk(boolean command) {
        this.drunkDelayCommandTimer = this.drunkDelayCommandTimer + GameTime.getInstance().getMultiplier();
        if (Rand.AdjustForFramerate(4 * this.vehicleObject.getDriver().getMoodles().getMoodleLevel(MoodleType.DRUNK)) < this.drunkDelayCommandTimer) {
            this.drunkDelayCommandTimer = 0.0F;
            return true;
        } else {
            return false;
        }
    }

    private float delayCommandWhileDrunk(float steering) {
        this.drunkDelayCommandTimer = this.drunkDelayCommandTimer + GameTime.getInstance().getMultiplier();
        if (Rand.AdjustForFramerate(4 * this.vehicleObject.getDriver().getMoodles().getMoodleLevel(MoodleType.DRUNK)) < this.drunkDelayCommandTimer) {
            this.drunkDelayCommandTimer = 0.0F;
            return steering;
        } else {
            return 0.0F;
        }
    }

    private void checkTire(CarController.BulletVariables bv) {
        if (this.vehicleObject.getPartById("TireFrontLeft") == null || this.vehicleObject.getPartById("TireFrontLeft").getInventoryItem() == null) {
            bv.brakingForce = (float)(bv.brakingForce / 1.2);
            bv.engineForce = (float)(bv.engineForce / 1.2);
        }

        if (this.vehicleObject.getPartById("TireFrontRight") == null || this.vehicleObject.getPartById("TireFrontRight").getInventoryItem() == null) {
            bv.brakingForce = (float)(bv.brakingForce / 1.2);
            bv.engineForce = (float)(bv.engineForce / 1.2);
        }

        if (this.vehicleObject.getPartById("TireRearLeft") == null || this.vehicleObject.getPartById("TireRearLeft").getInventoryItem() == null) {
            bv.brakingForce = (float)(bv.brakingForce / 1.3);
            bv.engineForce = (float)(bv.engineForce / 1.3);
        }

        if (this.vehicleObject.getPartById("TireRearRight") == null || this.vehicleObject.getPartById("TireRearRight").getInventoryItem() == null) {
            bv.brakingForce = (float)(bv.brakingForce / 1.3);
            bv.engineForce = (float)(bv.engineForce / 1.3);
        }
    }

    public void updateControls() {
        if (!GameServer.server) {
            if (this.vehicleObject.isKeyboardControlled()) {
                boolean left = GameKeyboard.isKeyDown("Left");
                boolean right = GameKeyboard.isKeyDown("Right");
                boolean forward = GameKeyboard.isKeyDown("Forward");
                boolean backward = GameKeyboard.isKeyDown("Backward");
                boolean brake = GameKeyboard.isKeyDown("Brake");
                boolean shift = GameKeyboard.isKeyDown("CruiseControl");
                this.clientControls.steering = 0.0F;
                if (left) {
                    this.clientControls.steering--;
                }

                if (right) {
                    this.clientControls.steering++;
                }

                this.clientControls.forward = forward;
                this.clientControls.backward = backward;
                this.clientControls.brake = brake;
                this.clientControls.shift = shift;
                if (this.clientControls.brake) {
                    this.clientControls.wasUsingParkingBrakes = true;
                }
            }

            int joypad = this.vehicleObject.getJoypad();
            if (joypad != -1) {
                boolean leftx = JoypadManager.instance.isLeftPressed(joypad);
                boolean rightx = JoypadManager.instance.isRightPressed(joypad);
                boolean forwardx = JoypadManager.instance.isRTPressed(joypad);
                boolean backwardx = JoypadManager.instance.isLTPressed(joypad);
                boolean brakex = JoypadManager.instance.isBPressed(joypad);
                float xVal2 = JoypadManager.instance.getMovementAxisX(joypad);
                this.clientControls.steering = xVal2;
                this.clientControls.forward = forwardx;
                this.clientControls.backward = backwardx;
                this.clientControls.brake = brakex;
            }

            if (this.clientControls.forceBrake != 0L) {
                long dt = System.currentTimeMillis() - this.clientControls.forceBrake;
                if (dt > 0L && dt < 1000L) {
                    this.clientControls.brake = true;
                    this.clientControls.shift = false;
                }
            }
        }
    }

    public void park() {
        if (!GameServer.server && this.vehicleObject.getScript().getWheelCount() > 0) {
            Bullet.controlVehicle(this.vehicleObject.vehicleId, 0.0F, this.vehicleObject.getBrakingForce(), 0.0F);
        }

        this.isGas = this.wasGas = false;
        this.isGasR = this.wasGasR = false;
        this.clientControls.reset();
        this.vehicleObject.transmissionNumber = TransmissionNumber.N;
        if (this.vehicleObject.getVehicleTowing() != null) {
            this.vehicleObject.getVehicleTowing().getController().park();
        }
    }

    protected boolean shouldBeActive() {
        if (this.vehicleObject.physicActiveCheck != -1L) {
            return true;
        } else {
            BaseVehicle vehicleTowedBy = this.vehicleObject.getVehicleTowedBy();
            if (vehicleTowedBy == null) {
                float engineForce = this.vehicleObject.isEngineRunning() ? this.engineForce : 0.0F;
                return Math.abs(engineForce) > 0.01F;
            } else {
                return vehicleTowedBy.getController() == null ? false : vehicleTowedBy.getController().shouldBeActive();
            }
        }
    }

    public void checkShouldBeActive() {
        if (this.shouldBeActive()) {
            if (!this.isEnable) {
                this.vehicleObject.setPhysicsActive(true);
                this.isEnable = true;
            }

            this.atRestTimer = 1.0F;
        } else if (this.isEnable && this.vehicleObject.isAtRest()) {
            if (this.atRestTimer > 0.0F) {
                this.atRestTimer = this.atRestTimer - GameTime.getInstance().getTimeDelta();
            }

            if (this.atRestTimer <= 0.0F) {
                this.vehicleObject.setPhysicsActive(false);
                this.isEnable = false;
            }
        }
    }

    public boolean isGasPedalPressed() {
        return this.isGas || this.isGasR;
    }

    public boolean isBrakePedalPressed() {
        return this.isBreak;
    }

    public void debug() {
        if (Core.debug && DebugOptions.instance.vehicleRenderOutline.getValue()) {
            VehicleScript script = this.vehicleObject.getScript();
            int zi = PZMath.fastfloor(this.vehicleObject.getZ());
            Vector3f vec = this.tempVec3f;
            this.vehicleObject.getForwardVector(vec);
            Transform out = this.tempXfrm;
            this.vehicleObject.getWorldTransform(out);
            VehiclePoly poly = this.vehicleObject.getPoly();
            LineDrawer.addLine(poly.x1, poly.y1, zi, poly.x2, poly.y2, zi, 1.0F, 1.0F, 1.0F, null, true);
            LineDrawer.addLine(poly.x2, poly.y2, zi, poly.x3, poly.y3, zi, 1.0F, 1.0F, 1.0F, null, true);
            LineDrawer.addLine(poly.x3, poly.y3, zi, poly.x4, poly.y4, zi, 1.0F, 1.0F, 1.0F, null, true);
            LineDrawer.addLine(poly.x4, poly.y4, zi, poly.x1, poly.y1, zi, 1.0F, 1.0F, 1.0F, null, true);
            Vector2f closestPos = BaseVehicle.allocVector2f();
            float px = IsoCamera.frameState.camCharacterX;
            float py = IsoCamera.frameState.camCharacterY;
            this.vehicleObject.getClosestPointOnPoly(px, py, closestPos);
            if (this.vehicleObject.isPointLeftOfCenter(closestPos.x, closestPos.y)) {
                this.drawCircle(closestPos.x, closestPos.y, 0.05F, 0.0F, 1.0F, 0.0F, 1.0F);
            } else {
                this.drawCircle(closestPos.x, closestPos.y, 0.05F, 0.0F, 0.0F, 1.0F, 1.0F);
            }

            BaseVehicle.releaseVector2f(closestPos);
            _UNIT_Y.set(0.0F, 1.0F, 0.0F);

            for (int i = 0; i < this.vehicleObject.getScript().getWheelCount(); i++) {
                VehicleScript.Wheel scriptWheel = script.getWheel(i);
                this.tempVec3f.set(scriptWheel.getOffset());
                if (script.getModel() != null) {
                    this.tempVec3f.add(script.getModelOffset());
                }

                this.vehicleObject.getWorldPos(this.tempVec3f, this.tempVec3f);
                float originX = this.tempVec3f.x;
                float originY = this.tempVec3f.y;
                this.vehicleObject.getWheelForwardVector(i, this.tempVec3f);
                LineDrawer.addLine(originX, originY, zi, originX + this.tempVec3f.x, originY + this.tempVec3f.z, zi, 1.0F, 1.0F, 1.0F, null, true);
                this.drawRect(
                    this.tempVec3f,
                    originX - WorldSimulation.instance.offsetX,
                    originY - WorldSimulation.instance.offsetY,
                    scriptWheel.width,
                    scriptWheel.radius
                );
            }

            if (this.vehicleObject.collideX != -1.0F) {
                this.vehicleObject.getForwardVector(vec);
                this.drawCircle(this.vehicleObject.collideX, this.vehicleObject.collideY, 0.3F);
                this.vehicleObject.collideX = -1.0F;
                this.vehicleObject.collideY = -1.0F;
            }

            int joypad = this.vehicleObject.getJoypad();
            if (joypad != -1) {
                float xVal2 = JoypadManager.instance.getMovementAxisX(joypad);
                float yVal2 = JoypadManager.instance.getMovementAxisY(joypad);
                float deadZone = JoypadManager.instance.getDeadZone(joypad, 0);
                if (Math.abs(yVal2) > deadZone || Math.abs(xVal2) > deadZone) {
                    Vector2 vecAim = this.tempVec2.set(xVal2, yVal2);
                    vecAim.setLength(4.0F);
                    vecAim.rotate((float) (-Math.PI / 4));
                    LineDrawer.addLine(
                        this.vehicleObject.getX(),
                        this.vehicleObject.getY(),
                        this.vehicleObject.getZ(),
                        this.vehicleObject.getX() + vecAim.x,
                        this.vehicleObject.getY() + vecAim.y,
                        this.vehicleObject.getZ(),
                        1.0F,
                        1.0F,
                        1.0F,
                        null,
                        true
                    );
                }
            }

            float x = this.vehicleObject.getX();
            float y = this.vehicleObject.getY();
            float z = this.vehicleObject.getZ();
            LineDrawer.addLine(x - 0.5F, y, z, x + 0.5F, y, z, 1.0F, 1.0F, 1.0F, null, true);
            LineDrawer.addLine(x, y - 0.5F, z, x, y + 0.5F, z, 1.0F, 1.0F, 1.0F, null, true);
            this.renderClosestPointToOtherVehicle();
        }
    }

    private void renderClosestPointToOtherVehicle() {
        ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();
        BaseVehicle closest = null;
        float closestDistSq = Float.MAX_VALUE;

        for (int i = 0; i < vehicles.size(); i++) {
            BaseVehicle other = vehicles.get(i);
            if (other != this.vehicleObject) {
                float distSq = IsoUtils.DistanceToSquared(this.vehicleObject.getX(), this.vehicleObject.getY(), other.getX(), other.getY());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closest = other;
                }
            }
        }

        if (closest != null && !(closestDistSq > 100.0F)) {
            Vector2f p1 = BaseVehicle.allocVector2f();
            Vector2f p2 = BaseVehicle.allocVector2f();
            closestDistSq = this.vehicleObject.getClosestPointOnPoly(closest, p1, p2);
            if (closestDistSq == 0.0F) {
                LineDrawer.addRect(p1.x, p1.y, this.vehicleObject.getZ(), 0.05F, 0.05F, 0.0F, 1.0F, 1.0F);
            } else {
                LineDrawer.addLine(p1.x, p1.y, this.vehicleObject.getZ(), p2.x, p2.y, closest.getZ(), 0.0F, 1.0F, 1.0F, 1.0F);
            }

            BaseVehicle.releaseVector2f(p1);
            BaseVehicle.releaseVector2f(p2);
        }
    }

    public void drawRect(Vector3f vec, float x, float y, float w, float h) {
        this.drawRect(vec, x, y, w, h, 1.0F, 1.0F, 1.0F);
    }

    public void drawRect(Vector3f vec, float x, float y, float w, float h, float r, float g, float b) {
        float vecX = vec.x;
        float vecY = vec.y;
        float vecZ = vec.z;
        Vector3f vec2 = this.tempVec3f3;
        vec.cross(_UNIT_Y, vec2);
        float mul = 1.0F;
        vec.x *= 1.0F * h;
        vec.z *= 1.0F * h;
        vec2.x *= 1.0F * w;
        vec2.z *= 1.0F * w;
        float fx = x + vec.x;
        float fy = y + vec.z;
        float bx = x - vec.x;
        float by = y - vec.z;
        float fx1 = fx - vec2.x / 2.0F;
        float fx2 = fx + vec2.x / 2.0F;
        float bx1 = bx - vec2.x / 2.0F;
        float bx2 = bx + vec2.x / 2.0F;
        float by1 = by - vec2.z / 2.0F;
        float by2 = by + vec2.z / 2.0F;
        float fy1 = fy - vec2.z / 2.0F;
        float fy2 = fy + vec2.z / 2.0F;
        fx1 += WorldSimulation.instance.offsetX;
        fy1 += WorldSimulation.instance.offsetY;
        fx2 += WorldSimulation.instance.offsetX;
        fy2 += WorldSimulation.instance.offsetY;
        bx1 += WorldSimulation.instance.offsetX;
        by1 += WorldSimulation.instance.offsetY;
        bx2 += WorldSimulation.instance.offsetX;
        by2 += WorldSimulation.instance.offsetY;
        int z = PZMath.fastfloor(this.vehicleObject.getZ());
        float a = this.vehicleObject.getAlpha(IsoPlayer.getPlayerIndex());
        LineDrawer.addLine(fx1, fy1, z, fx2, fy2, z, r, g, b, a);
        LineDrawer.addLine(fx1, fy1, z, bx1, by1, z, r, g, b, a);
        LineDrawer.addLine(fx2, fy2, z, bx2, by2, z, r, g, b, a);
        LineDrawer.addLine(bx1, by1, z, bx2, by2, z, r, g, b, a);
        vec.set(vecX, vecY, vecZ);
    }

    public void drawCircle(float x, float y, float radius) {
        this.drawCircle(x, y, radius, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void drawCircle(float x, float y, float radius, float r, float g, float b, float a) {
        LineDrawer.DrawIsoCircle(x, y, this.vehicleObject.getZ(), radius, 16, r, g, b, a);
    }

    static {
        gears[0] = new CarController.GearInfo(0, 25, 0.0F);
        gears[1] = new CarController.GearInfo(25, 50, 0.5F);
        gears[2] = new CarController.GearInfo(50, 1000, 0.5F);
    }

    public static final class BulletVariables {
        private float engineForce;
        private float brakingForce;
        private float vehicleSteering;
        private BaseVehicle vehicle;

        private CarController.BulletVariables set(BaseVehicle vehicle, float EngineForce, float BrakingForce, float VehicleSteering) {
            this.vehicle = vehicle;
            this.engineForce = EngineForce;
            this.brakingForce = BrakingForce;
            this.vehicleSteering = VehicleSteering;
            return this;
        }
    }

    public static final class ClientControls {
        public float steering;
        public boolean forward;
        public boolean backward;
        public boolean brake;
        public boolean shift;
        public boolean wasUsingParkingBrakes;
        public long forceBrake;

        public void reset() {
            this.steering = 0.0F;
            this.forward = false;
            this.backward = false;
            this.brake = false;
            this.shift = false;
            this.wasUsingParkingBrakes = false;
            this.forceBrake = 0L;
        }
    }

    static enum ControlState {
        NoControl,
        Braking,
        Forward,
        Reverse;
    }

    public static final class GearInfo {
        private int minSpeed;
        private int maxSpeed;
        private float minRpm;

        private GearInfo(int minSpeed, int maxSpeed, float rpm) {
            this.minSpeed = minSpeed;
            this.maxSpeed = maxSpeed;
            this.minRpm = rpm;
        }
    }
}
