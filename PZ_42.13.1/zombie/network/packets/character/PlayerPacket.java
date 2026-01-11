// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.characters.Roles;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector3;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheatNoClip;
import zombie.network.anticheats.AntiCheatPlayer;
import zombie.network.anticheats.AntiCheatPower;
import zombie.network.anticheats.AntiCheatSpeed;
import zombie.network.fields.IMovable;
import zombie.network.fields.character.PlayerID;
import zombie.network.fields.character.PlayerVariables;
import zombie.network.fields.character.Prediction;
import zombie.network.packets.INetworkPacket;
import zombie.network.statistics.data.GameStatistic;
import zombie.pathfind.PolygonalMap2;
import zombie.popman.NetworkZombieSimulator;

public class PlayerPacket
    implements INetworkPacket,
    AntiCheatPower.IAntiCheat,
    AntiCheatSpeed.IAntiCheat,
    AntiCheatNoClip.IAntiCheat,
    AntiCheatPlayer.IAntiCheat {
    public static final int PACKET_SIZE_BYTES = 22;
    @JSONField
    public final PlayerID id = new PlayerID();
    @JSONField
    public final Prediction prediction = new Prediction();
    @JSONField
    public short booleanVariables;
    @JSONField
    public boolean disconnected;
    public final PlayerVariables variables = new PlayerVariables();

    @Override
    public int getPacketSizeBytes() {
        return 22;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.id.parse(b, connection);
        this.prediction.parse(b, connection);
        this.booleanVariables = b.getShort();
        this.variables.parse(b, connection);
        this.disconnected = b.get() != 0;
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        this.id.write(b);
        this.prediction.write(b);
        b.putShort(this.booleanVariables);
        this.variables.write(b);
        b.put((byte)(this.disconnected ? 1 : 0));
    }

    public PacketTypes.PacketType set(IsoPlayer chr) {
        this.id.set(chr);
        this.variables.set(chr);
        return chr.networkAi.set(this);
    }

    @Override
    public void processClient(UdpConnection connection) {
        try {
            IsoPlayer player = this.id.getPlayer();
            if (player == null) {
                INetworkPacket.send(PacketTypes.PacketType.PlayerDataRequest, this.id.getID());
            } else {
                GameClient.rememberPlayerPosition(player, this.prediction.position.x, this.prediction.position.y);
                if (!player.networkAi.isHitByVehicle()) {
                    player.networkAi.parse(this);
                }

                player.networkAi.disconnected = this.disconnected;
                if (this.disconnected) {
                    NetworkZombieSimulator.getInstance().clearTargetAuth(player);
                }

                if (player.getVehicle() == null
                    && 2 != this.prediction.type
                    && (
                        player.networkAi.distance.getLength() > 7.0F
                            || IsoUtils.DistanceTo(this.prediction.x, this.prediction.y, this.prediction.z, player.getX(), player.getY(), player.getZ()) > 1.0F
                                && !PolygonalMap2.instance
                                    .lineClearCollide(player.getX(), player.getY(), this.prediction.x, this.prediction.y, this.prediction.z)
                                && player.getZi() != PZMath.fastfloor((float)this.prediction.z)
                    )
                    && (player.getCurrentState() == null || !player.getCurrentState().isSyncOnSquare())) {
                    player.teleportTo(this.prediction.x, this.prediction.y, this.prediction.z);
                }

                IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare((double)this.prediction.x, (double)this.prediction.y, (double)this.prediction.z);
                if (sq != null) {
                    boolean hasObstacle = player.getSquare() != null
                        && (player.isCollidedThisFrame() || player.isCollidedWithVehicle())
                        && IsoUtils.DistanceTo(
                                this.prediction.position.x, this.prediction.position.y, this.prediction.position.z, player.getX(), player.getY(), player.getZ()
                            )
                            > 1.0F;
                    player.setHasObstacleOnPath(hasObstacle);
                    if (player.isAlive() && !IsoWorld.instance.currentCell.getObjectList().contains(player)) {
                        IsoWorld.instance.currentCell.getObjectList().add(player);
                        player.setCurrent(sq);
                        player.getNetworkCharacterAI().resetState();
                        INetworkPacket.send(PacketTypes.PacketType.PlayerDataRequest, this.id.getID());
                    }
                } else if (IsoWorld.instance.currentCell.getObjectList().contains(player)) {
                    player.removeFromWorld();
                    player.removeFromSquare();
                }
            }
        } catch (Exception var5) {
            DebugLog.Multiplayer.printException(var5, "Can't process PlayerPacket", LogSeverity.Error);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoPlayer player = this.id.getPlayer();

        try {
            if (player == null) {
                DebugLog.General
                    .error(
                        "receivePlayerUpdate: Server received position for unknown player (id:" + this.id.getDescription() + "). Server will ignore this data."
                    );
            } else {
                if (!player.networkAi.isHitByVehicle()) {
                    player.networkAi.parse(this);
                }

                if (player.networkAi.distance.getLength() > IsoChunkMap.chunkWidthInTiles) {
                    GameStatistic.getInstance().playersTeleports.increase();
                }

                connection.releventPos[player.playerIndex].set(this.prediction.position);
            }
        } catch (Exception var7) {
            DebugLog.Multiplayer.printException(var7, "Can't process PlayerPacket", LogSeverity.Error);
        }

        if (player != null) {
            for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                UdpConnection c = GameServer.udpEngine.connections.get(n);
                if (connection.getConnectedGUID() != c.getConnectedGUID()
                    && c.isFullyConnected()
                    && (
                        player.checkCanSeeClient(c) && c.RelevantTo(this.prediction.x, this.prediction.y)
                            || packetType == PacketTypes.PacketType.PlayerUpdateReliable
                                && (c.role.getPosition() > connection.role.getPosition() || connection.role == Roles.getDefaultForAdmin())
                    )) {
                    ByteBufferWriter bbw = c.startPacket();
                    packetType.doPacket(bbw);
                    this.write(bbw);
                    packetType.send(c);
                    GameServer.setCustomVariables(player, c);
                }
            }
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.id.getPlayer() == null || this.id.getPlayer().getVehicle() == null;
    }

    @Override
    public short getBooleanVariables() {
        return this.booleanVariables;
    }

    @Override
    public IsoPlayer getPlayer() {
        return this.id.getPlayer();
    }

    @Override
    public IMovable getMovable() {
        return this.getPlayer().getNetworkCharacterAI().speedChecker;
    }

    @Override
    public byte getPlayerIndex() {
        return this.id.getPlayerIndex();
    }

    @Override
    public Vector3 getPosition(Vector3 position) {
        return position.set(this.prediction.position);
    }
}
