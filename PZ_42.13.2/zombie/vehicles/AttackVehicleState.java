// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import fmod.fmod.FMODSoundEmitter;
import org.joml.Vector3f;
import zombie.GameTime;
import zombie.ai.State;
import zombie.ai.states.ZombieIdleState;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.network.GameServer;
import zombie.util.Type;

public final class AttackVehicleState extends State {
    private static final AttackVehicleState _instance = new AttackVehicleState();
    private BaseSoundEmitter emitter;
    private final Vector3f worldPos = new Vector3f();

    public static AttackVehicleState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        if (zombie.target instanceof IsoGameCharacter target) {
            if (target.isDead()) {
                if (target.getLeaveBodyTimedown() > 3600.0F) {
                    zombie.changeState(ZombieIdleState.instance());
                    zombie.setTarget(null);
                } else {
                    target.setLeaveBodyTimedown(target.getLeaveBodyTimedown() + GameTime.getInstance().getThirtyFPSMultiplier());
                    if (!GameServer.server && !Core.soundDisabled && Rand.Next(Rand.AdjustForFramerate(15)) == 0) {
                        if (this.emitter == null) {
                            this.emitter = new FMODSoundEmitter();
                        }

                        String soundName = zombie.getDescriptor().getVoicePrefix() + "Eating";
                        if (!this.emitter.isPlaying(soundName)) {
                            this.emitter.playSound(soundName);
                        }
                    }
                }

                zombie.timeSinceSeenFlesh = 0.0F;
            } else {
                BaseVehicle vehicle = target.getVehicle();
                if (vehicle != null && vehicle.isCharacterAdjacentTo(owner)) {
                    Vector3f v = vehicle.chooseBestAttackPosition(target, owner, this.worldPos);
                    if (v == null) {
                        if (zombie.allowRepathDelay <= 0.0F) {
                            owner.pathToCharacter(target);
                            zombie.allowRepathDelay = 6.25F;
                        }
                    } else if (v != null && (Math.abs(v.x - owner.getX()) > 0.1F || Math.abs(v.y - owner.getY()) > 0.1F)) {
                        if (!(Math.abs(vehicle.getCurrentSpeedKmHour()) > 0.8F)
                            || !vehicle.isCharacterAdjacentTo(owner) && !(vehicle.DistToSquared(owner) < 16.0F)) {
                            if (zombie.allowRepathDelay <= 0.0F) {
                                owner.pathToCharacter(target);
                                zombie.allowRepathDelay = 6.25F;
                            }
                        }
                    } else {
                        owner.faceThisObject(target);
                    }
                }
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoZombie zombie = (IsoZombie)owner;
        if (zombie.target instanceof IsoGameCharacter target) {
            IsoPlayer var14 = Type.tryCastTo(target, IsoPlayer.class);
            BaseVehicle vehicle = target.getVehicle();
            if (vehicle != null) {
                if (!target.isDead()) {
                    if (event.eventName.equalsIgnoreCase("AttackCollisionCheck")) {
                        target.getBodyDamage().AddRandomDamageFromZombie(zombie, null);
                        target.getBodyDamage().Update();
                        if (target.isDead()) {
                            if (var14 == null) {
                                if (target.isFemale()) {
                                    zombie.getEmitter().playVocals("FemaleBeingEatenDeath");
                                } else {
                                    zombie.getEmitter().playVocals("MaleBeingEatenDeath");
                                }
                            } else {
                                var14.setPlayingDeathSound(true);
                                var14.playerVoiceSound("DeathEaten");
                            }

                            target.setHealth(0.0F);
                        } else if (target.isAsleep()) {
                            if (GameServer.server) {
                                target.sendObjectChange("wakeUp");
                                target.setAsleep(false);
                            } else {
                                target.forceAwake();
                            }
                        }
                    } else if (event.eventName.equalsIgnoreCase("ThumpFrame")) {
                        VehicleWindow window = null;
                        VehiclePart part = null;
                        int seat = vehicle.getSeat(target);
                        String areaId = vehicle.getPassengerArea(seat);
                        if (vehicle.isInArea(areaId, owner)) {
                            VehiclePart door = vehicle.getPassengerDoor(seat);
                            if (door != null && door.getDoor() != null && door.getInventoryItem() != null && !door.getDoor().isOpen()) {
                                window = door.findWindow();
                                if (window != null && !window.isHittable()) {
                                    window = null;
                                }

                                if (window == null) {
                                    part = door;
                                }
                            }
                        } else {
                            part = vehicle.getNearestBodyworkPart(owner);
                            if (part != null) {
                                window = part.getWindow();
                                if (window == null) {
                                    window = part.findWindow();
                                }

                                if (window != null && !window.isHittable()) {
                                    window = null;
                                }

                                if (window != null) {
                                    part = null;
                                }
                            }
                        }

                        if (window != null) {
                            window.damage(zombie.strength);
                            vehicle.setBloodIntensity(window.part.getId(), vehicle.getBloodIntensity(window.part.getId()) + 0.025F);
                            if (!GameServer.server) {
                                zombie.setVehicleHitLocation(vehicle);
                                owner.getEmitter().playSound("ZombieThumpVehicleWindow", vehicle);
                            }

                            zombie.setThumpFlag(3);
                        } else {
                            if (!GameServer.server) {
                                zombie.setVehicleHitLocation(vehicle);
                                owner.getEmitter().playSound("ZombieThumpVehicle", vehicle);
                            }

                            zombie.setThumpFlag(1);
                        }

                        vehicle.setAddThumpWorldSound(true);
                        if (part != null && part.getWindow() == null && part.getCondition() > 0) {
                            part.setCondition(part.getCondition() - zombie.strength);
                            part.doInventoryItemStats(part.getInventoryItem(), 0);
                            vehicle.transmitPartCondition(part);
                        }

                        if (target.isAsleep()) {
                            if (GameServer.server) {
                                target.sendObjectChange("wakeUp");
                                target.setAsleep(false);
                            } else {
                                target.forceAwake();
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isAttacking(IsoGameCharacter owner) {
        return true;
    }

    public boolean isPassengerExposed(IsoGameCharacter owner) {
        if (!(owner instanceof IsoZombie zombie)) {
            return false;
        } else if (zombie.target instanceof IsoGameCharacter target) {
            BaseVehicle vehicle = target.getVehicle();
            if (vehicle == null) {
                return false;
            } else {
                boolean canAttackTarget = false;
                VehicleWindow window = null;
                int seat = vehicle.getSeat(target);
                String areaId = vehicle.getPassengerArea(seat);
                VehiclePart door = null;
                if (vehicle.isInArea(areaId, owner)) {
                    door = vehicle.getPassengerDoor(seat);
                    if (door != null && door.getDoor() != null) {
                        if (door.getInventoryItem() != null && !door.getDoor().isOpen()) {
                            window = door.findWindow();
                            if (window != null) {
                                if (!window.isHittable()) {
                                    window = null;
                                }

                                canAttackTarget = window == null;
                            } else {
                                canAttackTarget = false;
                            }
                        } else {
                            canAttackTarget = true;
                        }
                    }
                } else {
                    door = vehicle.getNearestBodyworkPart(owner);
                    if (door != null) {
                        window = door.findWindow();
                        if (window != null && !window.isHittable()) {
                            window = null;
                        }
                    }
                }

                return canAttackTarget;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isSyncOnEnter() {
        return false;
    }

    @Override
    public boolean isSyncOnExit() {
        return false;
    }

    @Override
    public boolean isSyncOnSquare() {
        return false;
    }

    @Override
    public boolean isSyncInIdle() {
        return false;
    }
}
