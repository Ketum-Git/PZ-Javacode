// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.ai.states.animals.AnimalPathFindState;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugOptions;
import zombie.debug.options.Multiplayer;
import zombie.input.GameKeyboard;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoHutch;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.fields.character.AnimalStateVariables;
import zombie.network.fields.character.Prediction;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.character.AnimalPacket;
import zombie.network.packets.character.PlayerPacket;
import zombie.pathfind.PathFindBehavior2;
import zombie.util.StringUtils;
import zombie.vehicles.BaseVehicle;

public class NetworkPlayerAI extends NetworkCharacterAI {
    public final UpdateLimit reliable = new UpdateLimit(2000L);
    IsoPlayer player;
    private PathFindBehavior2 pfb2;
    private final UpdateLimit timerMax = new UpdateLimit(1000L);
    private final UpdateLimit timerMin = new UpdateLimit(200L);
    private boolean needUpdate;
    private final Vector2 tempo = new Vector2();
    private IsoGridSquare square;
    public boolean needToMovingUsingPathFinder;
    public boolean moving;
    public short lastBooleanVariables;
    private boolean pressedMovement;
    private boolean pressedCancelAction;
    private long accessLevelTimestamp;
    boolean wasNonPvpZone;
    public boolean disconnected;

    public NetworkPlayerAI(IsoGameCharacter character) {
        super(character);
        this.player = (IsoPlayer)character;
        this.pfb2 = this.player.getPathFindBehavior2();
        character.ulBeatenVehicle.Reset(200L);
        this.wasNonPvpZone = false;
        this.disconnected = false;
        this.player.role = Roles.animal;
    }

    @Override
    public IsoPlayer getRelatedPlayer() {
        if (this.player instanceof IsoAnimal isoAnimal) {
            return isoAnimal.atkTarget instanceof IsoPlayer isoPlayer ? isoPlayer : isoAnimal.getData().getAttachedPlayer();
        } else {
            return null;
        }
    }

    @Override
    public Multiplayer.DebugFlagsOG.IsoGameCharacterOG getBooleanDebugOptions() {
        if (this.player instanceof IsoAnimal) {
            return DebugOptions.instance.multiplayer.debugFlags.animal;
        } else {
            return this.player.isLocal() ? DebugOptions.instance.multiplayer.debugFlags.localPlayer : DebugOptions.instance.multiplayer.debugFlags.remotePlayer;
        }
    }

    public void needToUpdate() {
        this.needUpdate = true;
    }

    private void setStatic(Prediction prediction, IsoObject isoObject) {
        prediction.x = isoObject.getX();
        prediction.y = isoObject.getY();
        prediction.z = (byte)PZMath.fastfloor(isoObject.getZ());
        prediction.type = 0;
    }

    private void setMoving(Prediction prediction) {
        this.player.getDeferredMovement(this.tempo);
        this.tempo.x = this.player.getX() + this.tempo.x * 0.03F * 1000.0F;
        this.tempo.y = this.player.getY() + this.tempo.y * 0.03F * 1000.0F;
        prediction.x = this.tempo.x;
        prediction.y = this.tempo.y;
        if (this.player.getZ() == this.pfb2.getTargetZ()) {
            prediction.z = (byte)PZMath.fastfloor(this.pfb2.getTargetZ());
        } else {
            prediction.z = (byte)PZMath.fastfloor(this.player.getZ());
        }

        prediction.type = 1;
    }

    private void setPathFind(Prediction prediction) {
        prediction.x = this.pfb2.pathNextX;
        prediction.y = this.pfb2.pathNextY;
        prediction.z = (byte)PZMath.fastfloor(this.player.getZ());
        prediction.type = 2;
    }

    public void set(AnimalPacket packet, UdpConnection receiver) {
        if (this.player instanceof IsoAnimal animal) {
            packet.flags = 1;
            packet.variables = AnimalStateVariables.getVariables(animal);
            packet.realState = animal.realState;
            if (animal.isAlive() && animal.getCurrentState() == AnimalPathFindState.instance()) {
                this.setPathFind(packet.prediction);
            } else if (animal.isAlive() && animal.isMoving()) {
                this.setMoving(packet.prediction);
            } else {
                this.setStatic(packet.prediction, this.player);
                packet.prediction.direction = animal.getDirectionAngle();
            }

            packet.location = 0;
            if (animal.getItemID() != 0) {
                packet.location = 3;
            }

            if (animal.getVehicle() != null) {
                packet.location = 2;
                packet.vehicleId.set(animal.getVehicle());
            }

            if (animal.getHutch() != null) {
                packet.location = 1;
                packet.hutchNestBox = (byte)animal.nestBox;
                packet.hutchPosition = (byte)animal.getData().getHutchPosition();
                this.setStatic(packet.prediction, animal.getHutch());
            }

            packet.squareX = PZMath.fastfloor(animal.getX());
            packet.squareY = PZMath.fastfloor(animal.getY());
            packet.squareZ = (byte)PZMath.fastfloor(animal.getZ());
            packet.idleAction = animal.getVariableString("idleAction");
            if (!StringUtils.isNullOrEmpty(packet.idleAction)) {
                packet.flags = (short)(packet.flags | 2);
            }

            if (animal.getHook() != null) {
                packet.location = 4;
                this.setStatic(packet.prediction, animal.getHook());
                return;
            }

            if (animal.isAlerted() && animal.alertedChr instanceof IsoPlayer alerted) {
                packet.alertedId = alerted.getOnlineID();
                packet.flags = (short)(packet.flags | 8);
            }

            packet.type = animal.getAnimalType();
            packet.breed = animal.getBreed().getName();
            if (animal.getData().canHaveMilk()) {
                packet.milkQty = animal.getData().getMilkQuantity();
                packet.lastTimeMilked = animal.getData().lastMilkTimer;
                packet.flags = (short)(packet.flags | 32);
            }

            if (animal.getData().getWoolQuantity() > 0.0F) {
                packet.woolQty = animal.getData().getWoolQuantity();
                packet.flags = (short)(packet.flags | 64);
            }

            if (animal.getData().isPregnant()) {
                packet.flags = (short)(packet.flags | 16);
            }

            if (receiver != null) {
                IsoPlayer playerReceiver = GameServer.getAnyPlayerFromConnection(receiver);
                if (playerReceiver != null) {
                    packet.acceptance = (byte)animal.getPlayerAcceptance(playerReceiver);
                    packet.flags = (short)(packet.flags | 128);
                    if (playerReceiver.role.hasCapability(Capability.AnimalCheats)) {
                        packet.flags = (short)(packet.flags | 1024);
                        packet.pregnantTime = animal.getData().getPregnancyTime();
                        packet.maxMilkActual = animal.getData().getMaxMilkActual();
                    }
                }
            }

            if (!StringUtils.isNullOrEmpty(animal.getCustomName())) {
                packet.customName = animal.getCustomName();
                packet.flags = (short)(packet.flags | 256);
            }

            if (animal.getMother() != null) {
                packet.flags = (short)(packet.flags | 512);
                packet.mother.set(animal.getMother());
            }

            packet.age = animal.getData().getAge();
            packet.weight = animal.getWeight();
            packet.stress = (byte)animal.getStress();
            packet.health = (byte)(animal.getHealth() * 100.0F);
            packet.thirst = (byte)(animal.getThirst() * 100.0F);
            packet.hunger = (byte)(animal.getHunger() * 100.0F);
        }
    }

    public PacketTypes.PacketType set(PlayerPacket packet) {
        PacketTypes.PacketType result = null;
        boolean squareChanged = this.square != this.player.getCurrentSquare();
        this.square = this.player.getCurrentSquare();
        if (4 == ServerOptions.getInstance().antiCheatMovement.getValue()) {
            squareChanged = false;
        }

        if ((this.timerMin.Check() || this.needUpdate || squareChanged) && (this.player.isDead() || !this.player.isSeatedInVehicle())) {
            packet.disconnected = this.disconnected;
            packet.prediction.direction = this.player.getDirectionAngleRadians();
            if (this.pfb2.isMovingUsingPathFind() && this.pfb2.pathNextIsSet) {
                this.setPathFind(packet.prediction);
            } else if (this.player.isPlayerMoving()) {
                this.setStatic(packet.prediction, this.player);
            } else {
                this.setMoving(packet.prediction);
            }

            packet.booleanVariables = NetworkPlayerVariables.getBooleanVariables(this.player);
            boolean flagsChanged = this.lastBooleanVariables != packet.booleanVariables;
            this.lastBooleanVariables = packet.booleanVariables;
            boolean timerChanged = this.timerMax.Check();
            if (timerChanged || flagsChanged) {
                result = PacketTypes.PacketType.PlayerUpdateReliable;
            }

            if (squareChanged) {
                result = PacketTypes.PacketType.PlayerUpdateReliable;
            }

            if (this.needUpdate) {
                result = PacketTypes.PacketType.PlayerUpdateReliable;
                this.needUpdate = false;
            }

            if (PacketTypes.PacketType.PlayerUpdateReliable == result) {
                this.timerMax.Reset(600L);
            }
        }

        return result;
    }

    public void parse(AnimalPacket packet) {
        if (this.player instanceof IsoAnimal animal) {
            this.targetX = PZMath.roundFromEdges(packet.prediction.x);
            this.targetY = PZMath.roundFromEdges(packet.prediction.y);
            this.targetZ = packet.prediction.z;
            this.predictionType = packet.prediction.type;
            this.direction.set((float)Math.cos(packet.prediction.direction), (float)Math.sin(packet.prediction.direction));
            this.distance.set(packet.prediction.x - animal.getX(), packet.prediction.y - animal.getY());
            if (this.usePathFind) {
                this.pfb2.pathToLocationF(packet.prediction.x, packet.prediction.y, packet.prediction.z);
                this.pfb2.walkingOnTheSpot.reset(animal.getX(), animal.getY());
            }

            AnimalStateVariables.setVariables(animal, packet.variables);
            animal.ensureOnTile();
            animal.setVariable("bPathfind", false);
            animal.realx = packet.prediction.position.x;
            animal.realy = packet.prediction.position.y;
            animal.realz = (byte)packet.prediction.position.z;
            boolean isFarFromRealPosition = IsoUtils.DistanceManhatten(animal.realx, animal.realy, this.getCharacter().getX(), this.getCharacter().getY())
                > 0.2F;
            if (packet.isDead() && isFarFromRealPosition) {
                this.predictionType = 2;
                this.setNoCollision(5000L);
            }

            if (this.predictionType == 2) {
                this.needToMovingUsingPathFinder = true;
            } else {
                this.needToMovingUsingPathFinder = false;
            }

            boolean hasObstacle = animal.getSquare() != null
                && (animal.isCollidedThisFrame() || animal.isCollidedWithVehicle())
                && IsoUtils.DistanceTo(animal.realx, animal.realy, animal.realz, animal.getX(), animal.getY(), animal.getZ()) > 1.8F;
            animal.setHasObstacleOnPath(hasObstacle);
            if (packet.location == 1) {
                animal.hutch = IsoHutch.getHutch(
                    PZMath.fastfloor(packet.prediction.x), PZMath.fastfloor(packet.prediction.y), PZMath.fastfloor((float)packet.prediction.z)
                );
                if (animal.hutch != null) {
                    if (animal.nestBox != -1) {
                        animal.hutch.getNestBox(animal.nestBox).animal = null;
                    }

                    if (animal.getData().getHutchPosition() != -1) {
                        animal.hutch.animalInside.put(animal.getData().getHutchPosition(), null);
                    }

                    if (packet.hutchPosition != -1) {
                        IsoAnimal animalInHutch = animal.hutch.animalInside.get(Integer.valueOf(packet.hutchPosition));
                        if (animalInHutch != animal) {
                            animal.getData().setPreferredHutchPosition(packet.hutchPosition);
                            animal.getData().setHutchPosition(animal.getData().getPreferredHutchPosition());
                            animal.hutch.animalInside.put(animal.getData().getHutchPosition(), animal);
                        }
                    } else if (packet.hutchNestBox != -1) {
                        IsoAnimal animalInNestBox = animal.hutch.getNestBox(Integer.valueOf(packet.hutchNestBox)).animal;
                        if (animalInNestBox != animal) {
                            animal.hutch.getNestBox(Integer.valueOf(packet.hutchNestBox)).animal = animal;
                            animal.nestBox = packet.hutchNestBox;
                        }
                    }

                    if (packet.isDead() && animal.hutch.deadBodiesInside.get(Integer.valueOf(packet.hutchPosition)) == null) {
                        IsoDeadBody deadAnimal = new IsoDeadBody(animal, false);
                        animal.hutch.deadBodiesInside.put(Integer.valueOf(packet.hutchPosition), deadAnimal);
                    }

                    animal.getHutch().tryRemoveAnimalFromWorld(animal);
                }
            }

            if (packet.location != 1 && animal.getHutch() != null) {
                animal.getHutch().removeAnimal(animal);
            }

            if ((packet.flags & 2) != 0) {
                animal.setVariable("idleAction", packet.idleAction);
            } else {
                animal.clearVariable("idleAction");
            }

            if (packet.location == 4) {
                animal.setOnHook(true);
                return;
            }

            if ((packet.flags & 32) != 0) {
                animal.getData().canHaveMilk = true;
                animal.getData().milkQty = packet.milkQty;
                animal.getData().lastMilkTimer = packet.lastTimeMilked;
            } else {
                animal.getData().canHaveMilk = false;
            }

            if ((packet.flags & 64) != 0) {
                animal.getData().setWoolQuantity(packet.woolQty, true);
            }

            animal.getData().setPregnant((packet.flags & 16) != 0);
            if ((packet.flags & 128) != 0) {
                animal.playerAcceptanceList.put(IsoPlayer.getInstance().getOnlineID(), (float)packet.acceptance);
            }

            if ((packet.flags & 256) != 0 && !StringUtils.isNullOrEmpty(packet.customName)) {
                animal.setCustomName(packet.customName);
            }

            if ((packet.flags & 512) != 0 && packet.mother.isConsistent(null) && animal.getMother() == null) {
                animal.setMother(packet.mother.getAnimal());
                animal.motherId = packet.mother.getAnimal().getAnimalID();
            }

            if ((packet.flags & 1024) != 0) {
                animal.getData().setPregnancyTime(packet.pregnantTime);
                animal.getData().maxMilkActual = packet.maxMilkActual;
            }

            animal.getData().setAge(packet.age);
            animal.setHoursSurvived(packet.age * 24);
            animal.setWeight(packet.weight);
            animal.stressLevel = packet.stress;
            animal.getStats().set(CharacterStat.THIRST, packet.thirst / 100.0F);
            animal.getStats().set(CharacterStat.HUNGER, packet.hunger / 100.0F);
            if (!packet.isDead() || packet.location != 0) {
                animal.setHealth(packet.health / 100.0F);
            }

            animal.setIsAlerted(!isFarFromRealPosition && (packet.flags & 8) != 0);
            animal.alertedChr = animal.isAlerted() ? GameClient.IDToPlayerMap.get(packet.alertedId) : null;
        }
    }

    public void parse(BaseVehicle vehicle) {
        this.player.setTimeSinceLastNetData(0);
        IsoGridSquare sq = vehicle.getCurrentSquare();
        if (sq != null) {
            if (this.player.isAlive() && !IsoWorld.instance.currentCell.getObjectList().contains(this.player)) {
                IsoWorld.instance.currentCell.getObjectList().add(this.player);
                this.player.setCurrent(sq);
            }
        } else if (IsoWorld.instance.currentCell.getObjectList().contains(this.player)) {
            IsoWorld.instance.currentCell.getObjectList().remove(this.player);
            this.player.removeFromWorld();
            this.player.removeFromSquare();
        }
    }

    public void parse(PlayerPacket packet) {
        this.targetX = PZMath.roundFromEdges(packet.prediction.x);
        this.targetY = PZMath.roundFromEdges(packet.prediction.y);
        this.targetZ = packet.prediction.z;
        this.predictionType = packet.prediction.type;
        this.needToMovingUsingPathFinder = 2 == packet.prediction.type;
        this.direction.set((float)Math.cos(packet.prediction.direction), (float)Math.sin(packet.prediction.direction));
        this.distance.set(packet.prediction.x - this.player.getX(), packet.prediction.y - this.player.getY());
        if (this.usePathFind) {
            this.pfb2.pathToLocationF(packet.prediction.x, packet.prediction.y, packet.prediction.z);
            this.pfb2.walkingOnTheSpot.reset(this.player.getX(), this.player.getY());
        }

        NetworkPlayerVariables.setBooleanVariables(this.player, packet.booleanVariables);
        this.player.setbSeenThisFrame(false);
        this.player.setbCouldBeSeenThisFrame(false);
        this.player.setTimeSinceLastNetData(0);
        this.player.ensureOnTile();
        this.player.realx = packet.prediction.position.x;
        this.player.realy = packet.prediction.position.y;
        this.player.realz = (byte)packet.prediction.position.z;
        if (GameServer.server) {
            this.player.setForwardDirection(this.direction);
        }

        packet.variables.apply(this.player);
        this.setPressedMovement(false);
        this.setPressedCancelAction(false);
    }

    public boolean isPressedMovement() {
        return this.pressedMovement;
    }

    public void setPressedMovement(boolean pressedMovement) {
        if (!this.pressedMovement && pressedMovement) {
            boolean var3 = true;
        } else {
            boolean var10000 = false;
        }

        this.pressedMovement = pressedMovement;
    }

    public boolean isPressedCancelAction() {
        return this.pressedCancelAction;
    }

    public void setPressedCancelAction(boolean pressedCancelAction) {
        if (!this.pressedCancelAction && pressedCancelAction) {
            boolean var3 = true;
        } else {
            boolean var10000 = false;
        }

        this.pressedCancelAction = pressedCancelAction;
    }

    public void setCheckAccessLevelDelay(long delay) {
        this.accessLevelTimestamp = System.currentTimeMillis() + delay;
    }

    public boolean doCheckAccessLevel() {
        if (this.accessLevelTimestamp == 0L) {
            return true;
        } else if (System.currentTimeMillis() > this.accessLevelTimestamp) {
            this.accessLevelTimestamp = 0L;
            return true;
        } else {
            return false;
        }
    }

    @Deprecated
    public void update() {
        if (!GameServer.server && GameClient.client) {
            if (!ServerOptions.getInstance().knockedDownAllowed.getValue()
                && this.player.isLocalPlayer()
                && this.player.getVehicle() == null
                && this.player.isUnderVehicleRadius(0.0F)) {
                this.player.setJustMoved(true);
                this.player.setMoveDelta(1.0F);
                this.player.setForwardDirection(Rand.Next(-1, 1), Rand.Next(-1, 1));
            }

            if (Core.debug && this.player == IsoPlayer.getInstance() && GameKeyboard.isKeyDown(29)) {
                if (GameKeyboard.isKeyPressed(44)) {
                    GameClient.SendCommandToServer(
                        String.format(
                            "/createhorde2 -x %d -y %d -z %d -count %d -radius %d -crawler %s -isFallOnFront %s -isFakeDead %s -knockedDown %s -health %s -outfit %s ",
                            PZMath.fastfloor(this.player.getX() + this.player.getForwardDirection().getX()),
                            PZMath.fastfloor(this.player.getY() + this.player.getForwardDirection().getY()),
                            PZMath.fastfloor(this.player.getZ()),
                            1,
                            0,
                            "false",
                            "false",
                            "false",
                            "false",
                            "1",
                            ""
                        )
                    );
                }

                if (GameKeyboard.isKeyPressed(45)) {
                    GameClient.instance
                        .sendClientCommandV(
                            this.player,
                            "animal",
                            "add",
                            "type",
                            "bull",
                            "breed",
                            "angus",
                            "x",
                            PZMath.fastfloor(this.player.getX() + this.player.getForwardDirection().getX()),
                            "y",
                            PZMath.fastfloor(this.player.getY() + this.player.getForwardDirection().getY()),
                            "z",
                            PZMath.fastfloor(this.player.getZ()),
                            "skeleton",
                            false
                        );
                }

                if (GameKeyboard.isKeyPressed(47)) {
                    GameClient.SendCommandToServer("/addvehicle Base.SportsCar");
                }

                if (GameKeyboard.isKeyPressed(18)) {
                    throw new IllegalArgumentException("Test exception.");
                }
            }
        }
    }

    public boolean isDismantleAllowed() {
        return true;
    }

    public boolean isDisconnected() {
        return this.disconnected;
    }

    public void setDisconnected(boolean disconnected) {
        this.disconnected = disconnected;
    }

    public boolean isReliable() {
        return this.reliable.Check();
    }

    @Override
    public void resetState() {
        super.resetState();
        this.player.setPerformingAnAction(false);
        this.player.overridePrimaryHandModel = null;
        this.player.overrideSecondaryHandModel = null;
        this.player.resetModelNextFrame();
    }

    @Override
    public void syncDamage() {
        if (GameServer.server) {
            UdpConnection connection = GameServer.getConnectionFromPlayer(this.player);
            if (connection != null && connection.isFullyConnected() && !GameServer.isDelayedDisconnect(connection)) {
                this.player.updateSpeedModifiers();
                this.player.updateMovementRates();
                INetworkPacket.send(this.player, PacketTypes.PacketType.PlayerInjuries, this.player);
                INetworkPacket.send(this.player, PacketTypes.PacketType.PlayerDamage, this.player);
            }
        }
    }

    @Override
    public void syncStats() {
        if (GameServer.server) {
            UdpConnection connection = GameServer.getConnectionFromPlayer(this.player);
            if (connection != null && connection.isFullyConnected() && !GameServer.isDelayedDisconnect(connection)) {
                INetworkPacket.send(this.player, PacketTypes.PacketType.PlayerStats, this.player);
                INetworkPacket.send(this.player, PacketTypes.PacketType.PlayerEffects, this.player);
            }
        }
    }

    @Override
    public void syncXp() {
        if (GameServer.server) {
            UdpConnection connection = GameServer.getConnectionFromPlayer(this.player);
            if (connection != null && connection.isFullyConnected() && !GameServer.isDelayedDisconnect(connection)) {
                INetworkPacket.send(this.player, PacketTypes.PacketType.PlayerXp, this.player);
            }
        }
    }

    @Override
    public void syncHealth() {
        if (GameServer.server) {
            UdpConnection connection = GameServer.getConnectionFromPlayer(this.player);
            if (connection != null && connection.isFullyConnected() && !GameServer.isDelayedDisconnect(connection)) {
                INetworkPacket.send(this.player, PacketTypes.PacketType.PlayerHealth, this.player);
            }
        }
    }

    @Override
    public void setAnimalPacket(UdpConnection receiver) {
        this.set(this.animalPacket, receiver);
    }

    public static class AnimalLocationFlags {
        public static final byte world = 0;
        public static final byte hutch = 1;
        public static final byte vehicle = 2;
        public static final byte container = 3;
        public static final byte hook = 4;
    }
}
