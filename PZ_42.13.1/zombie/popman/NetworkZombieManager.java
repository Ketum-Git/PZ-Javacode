// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman;

import java.util.ArrayList;
import zombie.ai.State;
import zombie.ai.states.ZombieEatBodyState;
import zombie.ai.states.ZombieIdleState;
import zombie.ai.states.ZombieSittingState;
import zombie.ai.states.ZombieTurnAlerted;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.network.NetworkVariables;
import zombie.network.ServerOptions;
import zombie.network.packets.character.ZombieListPacket;
import zombie.util.hash.PZHash;

public class NetworkZombieManager {
    private static final NetworkZombieManager instance = new NetworkZombieManager();
    private final NetworkZombieList owns = new NetworkZombieList();
    private static final float NospottedDistanceSquared = 16.0F;

    public static NetworkZombieManager getInstance() {
        return instance;
    }

    public int getAuthorizedZombieCount(UdpConnection con) {
        return (int)IsoWorld.instance.currentCell.getZombieList().stream().filter(z -> z.getOwner() == con).count();
    }

    public int getUnauthorizedZombieCount() {
        return (int)IsoWorld.instance.currentCell.getZombieList().stream().filter(z -> z.getOwner() == null).count();
    }

    public static boolean canSpotted(IsoZombie zombie) {
        if (zombie.isRemoteZombie()) {
            return false;
        } else if (zombie.target != null && IsoUtils.DistanceToSquared(zombie.getX(), zombie.getY(), zombie.target.getX(), zombie.target.getY()) < 16.0F) {
            return false;
        } else {
            State state = zombie.getCurrentState();
            return state == null
                || state == ZombieIdleState.instance()
                || state == ZombieEatBodyState.instance()
                || state == ZombieSittingState.instance()
                || state == ZombieTurnAlerted.instance();
        }
    }

    public void updateAuth(IsoZombie zombie) {
        if (GameServer.server) {
            if (System.currentTimeMillis() - zombie.lastChangeOwner >= 2000L || zombie.getOwner() == null) {
                if (ServerOptions.getInstance().switchZombiesOwnershipEachUpdate.getValue() && GameServer.getPlayerCount() > 1) {
                    if (zombie.getOwner() == null) {
                        for (int i = 0; i < GameServer.udpEngine.connections.size(); i++) {
                            UdpConnection c = GameServer.udpEngine.connections.get(i);
                            if (c != null) {
                                this.moveZombie(zombie, c, null);
                                break;
                            }
                        }
                    } else {
                        int idx = GameServer.udpEngine.connections.indexOf(zombie.getOwner()) + 1;

                        for (int ix = 0; ix < GameServer.udpEngine.connections.size(); ix++) {
                            UdpConnection c = GameServer.udpEngine.connections.get((ix + idx) % GameServer.udpEngine.connections.size());
                            if (c != null) {
                                this.moveZombie(zombie, c, null);
                                break;
                            }
                        }
                    }
                } else {
                    if (zombie.getWrappedGrappleable().getGrappledBy() instanceof IsoPlayer isoPlayer) {
                        UdpConnection c = GameServer.getConnectionFromPlayer(isoPlayer);
                        if (c != null && c.isFullyConnected() && !GameServer.isDelayedDisconnect(c)) {
                            this.moveZombie(zombie, c, isoPlayer);
                            return;
                        }
                    }

                    if (zombie.target instanceof IsoPlayer isoPlayerx) {
                        UdpConnection c = GameServer.getConnectionFromPlayer(isoPlayerx);
                        if (c != null && c.isFullyConnected() && !GameServer.isDelayedDisconnect(c)) {
                            float d = isoPlayerx.getRelevantAndDistance(zombie.getX(), zombie.getY(), c.releventRange - 2);
                            if (!Float.isInfinite(d)) {
                                this.moveZombie(zombie, c, isoPlayerx);
                                return;
                            }
                        }
                    }

                    UdpConnection connection = zombie.getOwner();
                    IsoPlayer player = zombie.getOwnerPlayer();
                    float distance = Float.POSITIVE_INFINITY;
                    if (connection != null) {
                        distance = connection.getRelevantAndDistance(zombie.getX(), zombie.getY(), zombie.getZ());
                    }

                    for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                        UdpConnection c = GameServer.udpEngine.connections.get(n);
                        if (c != connection && !GameServer.isDelayedDisconnect(c)) {
                            for (IsoPlayer p : c.players) {
                                if (p != null && p.isAlive()) {
                                    float d = p.getRelevantAndDistance(zombie.getX(), zombie.getY(), c.releventRange - 2);
                                    if (!Float.isInfinite(d) && (connection == null || distance > d * 1.618034F)) {
                                        connection = c;
                                        distance = d;
                                        player = p;
                                    }
                                }
                            }
                        }
                    }

                    if (connection == null && zombie.isReanimatedPlayer()) {
                        for (int nx = 0; nx < GameServer.udpEngine.connections.size(); nx++) {
                            UdpConnection c = GameServer.udpEngine.connections.get(nx);
                            if (c != connection && !GameServer.isDelayedDisconnect(c)) {
                                for (IsoPlayer px : c.players) {
                                    if (px != null && px.isDead() && px.reanimatedCorpse == zombie) {
                                        connection = c;
                                        player = px;
                                    }
                                }
                            }
                        }
                    }

                    if (connection != null && !connection.RelevantTo(zombie.getX(), zombie.getY(), (connection.releventRange - 2) * 10)) {
                        connection = null;
                    }

                    this.moveZombie(zombie, connection, player);
                }
            }
        }
    }

    public void moveZombie(IsoZombie zombie, UdpConnection to, IsoPlayer player) {
        if (zombie.isDead()) {
            if (zombie.getOwner() == null && zombie.getOwnerPlayer() == null) {
                zombie.die();
            } else if (NetworkVariables.ZombieState.OnGround == zombie.realState) {
                synchronized (this.owns.lock) {
                    zombie.setOwner(null);
                    zombie.setOwnerPlayer(null);
                    zombie.getNetworkCharacterAI().resetSpeedLimiter();
                }

                NetworkZombiePacker.getInstance().setExtraUpdate();
            }
        } else {
            if (player != null
                && player.getVehicle() != null
                && player.getVehicle().getSpeed2D() > 2.0F
                && player.getVehicle().getDriver() != player
                && player.getVehicle().getDriver() instanceof IsoPlayer) {
                player = (IsoPlayer)player.getVehicle().getDriver();
                to = GameServer.getConnectionFromPlayer(player);
            }

            if (zombie.getOwner() != to) {
                synchronized (this.owns.lock) {
                    if (zombie.getOwner() != null) {
                        NetworkZombieList.NetworkZombie nz = this.owns.getNetworkZombie(zombie.getOwner());
                        if (nz != null && !nz.zombies.remove(zombie)) {
                            DebugLog.log("moveZombie: There are no zombies in nz.zombies.");
                        }
                    }

                    if (to != null) {
                        NetworkZombieList.NetworkZombie nz2 = this.owns.getNetworkZombie(to);
                        if (nz2 != null) {
                            nz2.zombies.add(zombie);
                            zombie.setOwner(to);
                            zombie.setOwnerPlayer(player);
                            zombie.getNetworkCharacterAI().resetSpeedLimiter();
                            to.timerSendZombie.reset(0L);
                        }
                    } else {
                        zombie.setOwner(null);
                        zombie.setOwnerPlayer(null);
                        zombie.getNetworkCharacterAI().resetSpeedLimiter();
                    }
                }

                zombie.lastChangeOwner = System.currentTimeMillis();
                NetworkZombiePacker.getInstance().setExtraUpdate();
            }
        }
    }

    public int getZombieAuth(UdpConnection connection, ZombieListPacket packet) {
        int hash = PZHash.fnv_32_init();
        NetworkZombieList.NetworkZombie nz = this.owns.getNetworkZombie(connection);
        packet.zombiesAuth.clear();
        synchronized (this.owns.lock) {
            nz.zombies.removeIf(zombiex -> zombiex.onlineId == -1);

            for (IsoZombie zombie : nz.zombies) {
                if (zombie.onlineId != -1) {
                    packet.zombiesAuth.add(zombie.onlineId);
                    hash = PZHash.fnv_32_hash(hash, zombie.onlineId);
                } else {
                    DebugLog.General.error("getZombieAuth: zombie.OnlineID == -1");
                }
            }

            return hash;
        }
    }

    public void clearTargetAuth(UdpConnection connection, IsoPlayer player) {
        if (Core.debug) {
            DebugLog.log(DebugType.Multiplayer, "Clear zombies target and auth for player id=" + player.getOnlineID());
        }

        if (GameServer.server) {
            for (int i = 0; i < IsoWorld.instance.currentCell.getZombieList().size(); i++) {
                IsoZombie zombie = IsoWorld.instance.currentCell.getZombieList().get(i);
                if (zombie.target == player) {
                    zombie.setTarget(null);
                }

                if (zombie.getOwner() == connection) {
                    zombie.setOwner(null);
                    zombie.setOwnerPlayer(null);
                    zombie.getNetworkCharacterAI().resetSpeedLimiter();
                    getInstance().updateAuth(zombie);
                }
            }
        }
    }

    public static void removeZombies(UdpConnection connection) {
        int radius = (IsoChunkMap.chunkGridWidth + 2) * 8;

        for (IsoPlayer player : connection.players) {
            if (player != null) {
                ArrayList<IsoZombie> zl = IsoWorld.instance.currentCell.getZombieList();
                ArrayList<IsoZombie> zombiesForDelete = new ArrayList<>();

                for (IsoZombie zombie : zl) {
                    if (Math.abs(zombie.getX() - player.getX()) < radius && Math.abs(zombie.getY() - player.getY()) < radius) {
                        zombiesForDelete.add(zombie);
                    }
                }

                for (IsoZombie zombiex : zombiesForDelete) {
                    NetworkZombiePacker.getInstance().deleteZombie(zombiex);
                    zombiex.removeFromWorld();
                    zombiex.removeFromSquare();
                }
            }
        }
    }

    public void recheck(UdpConnection connection) {
        synchronized (this.owns.lock) {
            NetworkZombieList.NetworkZombie nz = this.owns.getNetworkZombie(connection);
            if (nz != null) {
                nz.zombies.removeIf(zombie -> zombie.getOwner() != connection);
            }
        }
    }
}
