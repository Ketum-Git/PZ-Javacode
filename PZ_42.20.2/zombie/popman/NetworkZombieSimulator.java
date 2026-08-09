// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman;

import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import zombie.VirtualZombieManager;
import zombie.ai.states.ZombieHitReactionState;
import zombie.ai.states.ZombieOnGroundState;
import zombie.ai.states.ZombieTurnAlerted;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.NetworkZombieAI;
import zombie.characters.NetworkZombieVariables;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.network.packets.character.ZombiePacket;
import zombie.network.packets.character.ZombieSimulationPacket;

public class NetworkZombieSimulator {
    public static final int MAX_ZOMBIES_PER_UPDATE = 300;
    private static final NetworkZombieSimulator instance = new NetworkZombieSimulator();
    private static final ZombiePacket zombiePacket = new ZombiePacket();
    public final ArrayList<Short> unknownZombies = new ArrayList<>();
    private final HashSet<Short> authoriseZombies = new HashSet<>();
    private final ArrayDeque<IsoZombie> sendQueue = new ArrayDeque<>();
    private final ArrayDeque<IsoZombie> extraSendQueue = new ArrayDeque<>();
    private HashSet<Short> authoriseZombiesCurrent = new HashSet<>();
    private HashSet<Short> authoriseZombiesLast = new HashSet<>();
    UpdateLimit zombieSimulationReliableLimit = new UpdateLimit(1000L);

    public static NetworkZombieSimulator getInstance() {
        return instance;
    }

    public int getAuthorizedZombieCount() {
        return (int)IsoWorld.instance.currentCell.getZombieList().stream().filter(z -> z.getOwner() == GameClient.connection).count();
    }

    public int getUnauthorizedZombieCount() {
        return (int)IsoWorld.instance.currentCell.getZombieList().stream().filter(z -> z.getOwner() == null).count();
    }

    public void reset() {
        synchronized (this.authoriseZombies) {
            this.authoriseZombies.clear();
        }

        this.authoriseZombiesCurrent.clear();
        this.authoriseZombiesLast.clear();
        this.unknownZombies.clear();
        this.sendQueue.clear();
        this.extraSendQueue.clear();
    }

    public void clear() {
        HashSet<Short> temp = this.authoriseZombiesCurrent;
        this.authoriseZombiesCurrent = this.authoriseZombiesLast;
        this.authoriseZombiesLast = temp;
        this.authoriseZombiesLast.removeIf(zombieId -> GameClient.getZombie(zombieId) == null);
        this.authoriseZombiesCurrent.clear();
    }

    public void addExtraUpdate(IsoZombie zombie) {
        if (GameClient.client || GameServer.server) {
            if (zombie.getOwner() == GameClient.connection && !this.extraSendQueue.contains(zombie)) {
                this.extraSendQueue.add(zombie);
            }
        }
    }

    public void add(short onlineId) {
        this.authoriseZombiesCurrent.add(onlineId);
    }

    public void added() {
        for (Short zombieId : Sets.difference(this.authoriseZombiesCurrent, this.authoriseZombiesLast)) {
            IsoZombie z = GameClient.getZombie(zombieId);
            if (z != null && z.onlineId == zombieId) {
                this.becomeLocal(z);
            } else if (!this.unknownZombies.contains(zombieId)) {
                this.unknownZombies.add(zombieId);
            }
        }

        for (Short zombieIdx : Sets.difference(this.authoriseZombiesLast, this.authoriseZombiesCurrent)) {
            IsoZombie z = GameClient.getZombie(zombieIdx);
            if (z != null) {
                this.becomeRemote(z);
            }
        }

        synchronized (this.authoriseZombies) {
            this.authoriseZombies.clear();
            this.authoriseZombies.addAll(this.authoriseZombiesCurrent);
        }
    }

    public void becomeLocal(IsoZombie z) {
        z.lastRemoteUpdate = 0;
        z.setOwner(GameClient.connection);
        z.setOwnerPlayer(IsoPlayer.getInstance());
        z.allowRepathDelay = 0.0F;
        z.getNetworkCharacterAI().mindSync.restorePFBTarget();
    }

    public void becomeRemote(IsoZombie z) {
        if (z.isDead() && z.getOwner() == GameClient.connection) {
            z.getNetworkCharacterAI().setLocal(true);
        }

        z.lastRemoteUpdate = 0;
        z.setOwner(null);
        z.setOwnerPlayer(null);
        if (z.group != null) {
            z.group.remove(z);
        }
    }

    public boolean isZombieSimulated(Short zombieId) {
        synchronized (this.authoriseZombies) {
            return this.authoriseZombies.contains(zombieId);
        }
    }

    public void receivePacket(ByteBufferReader b, IConnection connection) {
        if (DebugOptions.instance.network.client.updateZombiesFromPacket.getValue()) {
            short num = b.getShort();

            for (short n = 0; n < num; n++) {
                this.parseZombie(b, connection);
            }
        }
    }

    private void parseZombie(ByteBufferReader b, IConnection connection) {
        ZombiePacket packet = zombiePacket;
        packet.parse(b, connection);
        if (packet.id == -1) {
            DebugType.General.error("NetworkZombieSimulator.parseZombie id=" + packet.id);
        } else {
            try {
                IsoZombie zombie = GameClient.IDToZombieMap.get(packet.id);
                if (zombie == null) {
                    if (IsoDeadBody.isDead(packet.id)) {
                        DebugType.Death.debugln("Skip dead zombie creation id=%d", packet.id);
                        return;
                    }

                    IsoGridSquare g = IsoWorld.instance.currentCell.getGridSquare((double)packet.realX, (double)packet.realY, (double)packet.realZ);
                    if (g != null) {
                        VirtualZombieManager.instance.choices.clear();
                        VirtualZombieManager.instance.choices.add(g);
                        zombie = VirtualZombieManager.instance.createRealZombieAlways(packet.outfitId, IsoDirections.getRandom(), false);
                        DebugType.ActionSystem.debugln("ParseZombie: CreateRealZombieAlways id=%s", packet.id);
                        if (zombie != null) {
                            zombie.getHumanVisual().setSkinTextureIndex(packet.skinTextureIndex);
                            zombie.setFakeDead(false);
                            zombie.onlineId = packet.id;
                            GameClient.IDToZombieMap.put(packet.id, zombie);
                            zombie.setLastX(zombie.setNextX(zombie.setX(packet.realX)));
                            zombie.setLastY(zombie.setNextY(zombie.setY(packet.realY)));
                            zombie.setLastZ(zombie.setZ(packet.realZ));
                            zombie.setDirectionAngle(packet.dirAngleRads * (180.0F / (float)Math.PI));
                            zombie.setCurrent(g);
                            NetworkZombieAI networkAi = zombie.getNetworkCharacterAI();
                            networkAi.targetX = packet.x;
                            networkAi.targetY = packet.y;
                            networkAi.targetZ = packet.z;
                            networkAi.predictionType = packet.predictionType;
                            networkAi.reanimatedBodyId.set(packet.reanimatedBodyId);
                            zombie.setHealth(packet.health / 1000.0F);
                            zombie.setSpeedMod(packet.speedMod / 1000.0F);
                            if (packet.target == -1) {
                                zombie.setTargetSeenTime(0.0F);
                                zombie.target = null;
                            } else {
                                IsoPlayer target = null;
                                if (GameClient.client) {
                                    target = GameClient.IDToPlayerMap.get(packet.target);
                                } else if (GameServer.server) {
                                    target = GameServer.IDToPlayerMap.get(packet.target);
                                }

                                if (target != zombie.target) {
                                    zombie.setTargetSeenTime(0.0F);
                                    zombie.target = target;
                                }
                            }

                            zombie.timeSinceSeenFlesh = packet.timeSinceSeenFlesh;
                            zombie.set(ZombieTurnAlerted.TARGET_ANGLE, packet.smParamTargetAngle / 1000.0F);
                            NetworkZombieVariables.setBooleanVariables(zombie, packet.booleanVariables);
                            if (zombie.isKnockedDown()) {
                                zombie.setOnFloor(true);
                                zombie.changeState(ZombieOnGroundState.instance());
                            }

                            zombie.setWalkType(packet.walkType.toString());
                            zombie.setSpeedTypeFromWalkType();
                            zombie.realState = packet.realState;
                            if (networkAi.reanimatedBodyId.getObjectID() != -1L) {
                                IsoDeadBody deadBody = (IsoDeadBody)networkAi.reanimatedBodyId.getObject();
                                if (deadBody != null) {
                                    zombie.setDir(deadBody.getDir());
                                    zombie.setForwardDirection(deadBody.getDir().ToVector());
                                    zombie.setFallOnFront(deadBody.isFallOnFront());
                                }

                                zombie.getStateMachine().changeState(ZombieOnGroundState.instance(), null, false);
                                zombie.neverDoneAlpha = false;
                            }

                            for (int playerIndex = 0; playerIndex < IsoPlayer.numPlayers; playerIndex++) {
                                IsoPlayer player = IsoPlayer.players[playerIndex];
                                if (g.isCanSee(playerIndex)) {
                                    zombie.setAlphaAndTarget(playerIndex, 1.0F);
                                }

                                if (player != null && player.reanimatedCorpseId == packet.id && packet.id != -1) {
                                    player.reanimatedCorpseId = -1;
                                    player.reanimatedCorpse = zombie;
                                }
                            }

                            networkAi.mindSync.parse(packet);
                            if (packet.grappledBy.getPlayer() != null) {
                                zombie.Grappled(packet.grappledBy.getPlayer(), packet.grappledBy.getPlayer().bareHands, 1.0F, packet.sharedGrappleType);
                            }
                        } else {
                            DebugType.Zombie.error("Error: VirtualZombieManager can't create zombie");
                        }
                    }

                    if (zombie == null) {
                        return;
                    }
                }

                if (getInstance().isZombieSimulated(zombie.onlineId)) {
                    zombie.setOwner(GameClient.connection);
                    zombie.setOwnerPlayer(IsoPlayer.getInstance());
                    return;
                }

                zombie.setOwner(null);
                zombie.setOwnerPlayer(null);
                NetworkZombieAI networkAix = zombie.getNetworkCharacterAI();
                if (!networkAix.isHitByVehicle() || !zombie.isCurrentState(ZombieHitReactionState.instance())) {
                    networkAix.parse(packet);
                    networkAix.mindSync.parse(packet);
                }

                zombie.lastRemoteUpdate = 0;
                if (!IsoWorld.instance.currentCell.getZombieList().contains(zombie)) {
                    IsoWorld.instance.currentCell.getZombieList().add(zombie);
                }

                if (!IsoWorld.instance.currentCell.getObjectList().contains(zombie)) {
                    IsoWorld.instance.currentCell.getObjectList().add(zombie);
                }
            } catch (Exception var9) {
                DebugType.General.printException(var9, LogSeverity.Error);
            }
        }
    }

    public boolean anyUnknownZombies() {
        return !this.unknownZombies.isEmpty();
    }

    public void send() {
        if (!this.authoriseZombies.isEmpty() || !this.unknownZombies.isEmpty()) {
            if (!getInstance().unknownZombies.isEmpty()) {
                INetworkPacket.send(PacketTypes.PacketType.ZombieRequest);
            }

            if (this.sendQueue.isEmpty()) {
                synchronized (this.authoriseZombies) {
                    for (Short zombieId : this.authoriseZombies) {
                        IsoZombie z = GameClient.getZombie(zombieId);
                        if (z != null && z.onlineId != -1) {
                            this.sendQueue.add(z);
                        }
                    }
                }
            }

            ZombieSimulationPacket packet = (ZombieSimulationPacket)GameClient.connection.getPacket(PacketTypes.PacketType.ZombieSimulationReliable);
            packet.sendQueue.clear();

            while (!this.sendQueue.isEmpty()) {
                IsoZombie z = this.sendQueue.poll();
                this.extraSendQueue.remove(z);
                if (z.onlineId != -1) {
                    packet.sendQueue.add(z);
                    NetworkZombieAI networkAi = z.getNetworkCharacterAI();
                    networkAi.targetX = z.realx = z.getX();
                    networkAi.targetY = z.realy = z.getY();
                    networkAi.targetZ = z.realz = (byte)z.getZi();
                    if (packet.sendQueue.size() >= 300) {
                        break;
                    }
                }
            }

            if (!packet.sendQueue.isEmpty() || !this.unknownZombies.isEmpty()) {
                ByteBufferWriter b = GameClient.connection.startPacket();
                PacketTypes.PacketType packetType;
                if (!this.unknownZombies.isEmpty() && this.zombieSimulationReliableLimit.Check()) {
                    packetType = PacketTypes.PacketType.ZombieSimulationReliable;
                } else {
                    packetType = PacketTypes.PacketType.ZombieSimulationUnreliable;
                }

                packetType.doPacket(b);
                packet.write(b);
                packetType.send(GameClient.connection);
            }

            if (!this.extraSendQueue.isEmpty()) {
                packet.sendQueue.clear();

                while (!this.extraSendQueue.isEmpty()) {
                    IsoZombie z = this.extraSendQueue.poll();
                    if (z.onlineId != -1) {
                        packet.sendQueue.add(z);
                        NetworkZombieAI networkAi = z.getNetworkCharacterAI();
                        networkAi.targetX = z.realx = z.getX();
                        networkAi.targetY = z.realy = z.getY();
                        networkAi.targetZ = z.realz = (byte)PZMath.fastfloor(z.getZ());
                    }
                }

                if (!packet.sendQueue.isEmpty()) {
                    ByteBufferWriter b = GameClient.connection.startPacket();
                    PacketTypes.PacketType.ZombieSimulationReliable.doPacket(b);
                    packet.write(b);
                    PacketTypes.PacketType.ZombieSimulationReliable.send(GameClient.connection);
                }
            }
        }
    }

    public void remove(IsoZombie zombie) {
        if (zombie != null && zombie.onlineId != -1) {
            GameClient.IDToZombieMap.remove(zombie.onlineId);
        }
    }

    public void clearTargetAuth(IsoPlayer player) {
        DebugType.Multiplayer.debugln("Clear zombies target and auth for player id=%s", player.getOnlineID());
        if (GameClient.client) {
            for (IsoZombie zombie : GameClient.IDToZombieMap.valueCollection()) {
                if (zombie.target == player) {
                    zombie.setTarget(null);
                }
            }
        }
    }
}
