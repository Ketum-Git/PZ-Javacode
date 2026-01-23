// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states.animals;

import zombie.SoundManager;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.pathfind.PolygonalMap2;
import zombie.util.StringUtils;

public final class AnimalAttackState extends State {
    private static final AnimalAttackState _instance = new AnimalAttackState();

    public static AnimalAttackState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoAnimal animal = (IsoAnimal)owner;
        if (animal.atkTarget != null) {
            animal.faceThisObject(animal.atkTarget);
        }

        if (animal.thumpTarget != null) {
            animal.faceThisObject(animal.thumpTarget);
            if (animal.thumpTarget.getSquare().DistToProper(animal.getCurrentSquare()) >= 2.0F) {
                animal.thumpTarget = null;
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoAnimal animal = (IsoAnimal)owner;
        if (event.eventName.equalsIgnoreCase("AttackConnect")) {
            owner.setPerformingAttackAnimation(false);
            if (animal.thumpTarget != null) {
                if (GameServer.server) {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalHitThumpable, owner.getX(), owner.getY(), owner, animal.thumpTarget);
                }

                if (animal.thumpTarget instanceof IsoThumpable thump) {
                    thump.animalHit(animal);
                    this.playDamageFenceSound(animal, animal.thumpTarget);
                    if (thump.health <= 0.0F) {
                        thump.destroy();
                        animal.thumpTarget = null;
                        animal.pathToLocation(animal.getPathTargetX(), animal.getPathTargetY(), animal.getPathTargetZ());
                    }

                    if (thump.isDoor() && thump.IsOpen()) {
                        animal.thumpTarget = null;
                        animal.pathToLocation(animal.getPathTargetX(), animal.getPathTargetY(), animal.getPathTargetZ());
                        return;
                    }

                    return;
                }
            }

            if (animal.atkTarget instanceof IsoAnimal target) {
                boolean blocked = PolygonalMap2.instance
                    .lineClearCollide(animal.getX(), animal.getY(), target.getX(), target.getY(), PZMath.fastfloor(target.getZ()));
                if (!blocked) {
                    target.HitByAnimal(animal, false);
                }

                target.getBehavior().blockMovement = false;
                if (!blocked && target.isAnimalAttacking() && target.atkTarget == animal) {
                    animal.HitByAnimal(target, false);
                }

                animal.pathToLocation(target.getXi() - 3, target.getYi(), target.getZi());
                target.pathToLocation(animal.getXi() + 3, animal.getYi(), animal.getZi());
                target.getBehavior().blockMovement = false;
                target.atkTarget = null;
                animal.atkTarget = null;
                if (GameServer.server) {
                    INetworkPacket.sendToRelative(PacketTypes.PacketType.AnimalHitAnimal, owner.getX(), owner.getY(), owner, target, 0.0F, false);
                }
            } else if (animal.atkTarget instanceof IsoPlayer chr) {
                if (chr.getVehicle() != null) {
                    animal.atkTarget = null;
                    return;
                }

                if (chr.DistToProper(animal) > animal.adef.attackDist + 0.5F) {
                    animal.atkTarget = null;
                    return;
                }

                if (chr.isInvisible() || chr.isGhostMode()) {
                    animal.atkTarget = null;
                    return;
                }

                if (PolygonalMap2.instance.lineClearCollide(animal.getX(), animal.getY(), chr.getX(), chr.getY(), PZMath.fastfloor(chr.getZ()))) {
                    animal.atkTarget = null;
                    return;
                }

                if (!GameClient.client) {
                    float damage = animal.calcDamage();
                    chr.hitConsequences(null, owner, false, damage, false);
                    if (GameServer.server) {
                        INetworkPacket.sendToRelative(
                            PacketTypes.PacketType.AnimalHitPlayer, owner.getX(), owner.getY(), owner, animal.atkTarget, damage, false
                        );
                    }
                }
            } else if (animal.thumpTarget instanceof IsoDoor door) {
                if (door.IsOpen()) {
                    animal.thumpTarget = null;
                    animal.pathToLocation(animal.getPathTargetX(), animal.getPathTargetY(), animal.getPathTargetZ());
                    return;
                }

                float baseDmg = 100.0F;
                baseDmg *= animal.calcDamage();
                animal.thumpTarget.Damage(baseDmg);
                this.playDamageFenceSound(animal, door);
            } else if (animal.thumpTarget != null) {
                float baseDmg = 100.0F;
                baseDmg *= animal.calcDamage();
                animal.thumpTarget.Damage(baseDmg);
                this.playDamageFenceSound(animal, animal.thumpTarget);
            }

            animal.getBehavior().blockMovement = false;
            animal.atkTarget = null;
        } else if (event.eventName.equalsIgnoreCase("ActiveAnimFinishing")) {
            owner.setPerformingAttackAnimation(false);
            animal.getBehavior().blockMovement = false;
            if (animal.atkTarget instanceof IsoAnimal target) {
                target.HitByAnimal(animal, false);
                target.getBehavior().blockMovement = false;
                if (target.isAnimalAttacking() && target.atkTarget == animal) {
                    animal.HitByAnimal(target, false);
                }

                animal.pathToLocation(target.getXi() - 3, target.getYi(), target.getZi());
                target.pathToLocation(animal.getXi() + 3, animal.getYi(), animal.getZi());
                target.getBehavior().blockMovement = false;
                target.atkTarget = null;
                animal.atkTarget = null;
            }

            animal.atkTarget = null;
        } else if ("PlayBreedSound".equalsIgnoreCase(event.eventName)) {
            animal.onPlayBreedSoundEvent(event.parameterValue);
        }
    }

    private void playDamageFenceSound(IsoAnimal animal, IsoObject object) {
        if (SoundManager.instance.isListenerInRange(animal.getX(), animal.getY(), 40.0F)) {
            if (object.getSprite() != null) {
                PropertyContainer props = object.getSprite().getProperties();
                String thumpSound = props.get("ThumpSound");
                if (StringUtils.isNullOrWhitespace(thumpSound) && object instanceof IsoDoor door) {
                    thumpSound = door.getThumpSound();
                }

                if (StringUtils.isNullOrWhitespace(thumpSound) && object instanceof IsoThumpable thumpable) {
                    thumpSound = thumpable.getThumpSound();
                }

                if (!StringUtils.isNullOrWhitespace(thumpSound)) {
                    String soundName = switch (thumpSound) {
                        case "ZombieThumpMetal" -> "AnimalThumpMetal";
                        case "ZombieThumpMetalPoleGate" -> "AnimalThumpMetalPoleGate";
                        case "ZombieThumpChainlinkFence" -> "AnimalThumpChainlinkFence";
                        default -> "AnimalThumpGeneric";
                    };
                    animal.getEmitter().playSoundImpl(soundName, null);
                } else {
                    String typeStr = props.get("FenceTypeLow");
                    if (typeStr != null) {
                        if ("Sandbag".equals(typeStr) && object.getName() != null && StringUtils.containsIgnoreCase(object.getName(), "Gravel")) {
                            typeStr = "Gravelbag";
                        }
                        String soundName = switch (typeStr) {
                            case "Barbwire" -> "AnimalThumpChainlinkFence";
                            case "Gravelbag" -> "AnimalThumpGeneric";
                            case "Metal" -> "AnimalThumpMetal";
                            case "MetalGate" -> "AnimalThumpMetalPoleGate";
                            case "RoadBlock" -> "AnimalThumpGeneric";
                            case "Sandbag" -> "AnimalThumpGeneric";
                            case "Wood" -> "AnimalThumpGeneric";
                            default -> "AnimalThumpGeneric";
                        };
                        animal.getEmitter().playSoundImpl(soundName, null);
                    } else {
                        typeStr = props.get("FenceTypeHigh");
                        if (typeStr != null) {
                            String soundName = switch (typeStr) {
                                case "Metal" -> "AnimalThumpMetal";
                                case "MetalGate" -> "AnimalThumpMetalPoleGate";
                                case "Wood" -> "AnimalThumpGeneric";
                                default -> "AnimalThumpGeneric";
                            };
                            animal.getEmitter().playSoundImpl(soundName, null);
                        }
                    }
                }
            }
        }
    }
}
