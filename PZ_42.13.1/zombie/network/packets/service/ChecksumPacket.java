// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.ZomboidFileSystem;
import zombie.characters.Capability;
import zombie.core.logger.LoggerManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.NetChecksum;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.ScriptManager;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 7)
public class ChecksumPacket implements INetworkPacket {
    private static final short PacketTotalChecksum = 1;
    private static final short PacketGroupChecksum = 2;
    private static final short PacketFileChecksums = 3;
    private static final short PacketError = 4;
    private static final byte FileDifferent = 1;
    private static final byte FileNotOnServer = 2;
    private static final byte FileNotOnClient = 3;
    short pkt;
    boolean okLua;
    boolean okScript;
    boolean match;
    short groupIndex;
    String relPath;
    byte reason;
    private final byte[] checksum = new byte[64];

    public void setPacketTotalChecksum() {
        this.pkt = 1;
    }

    private void setPacketGroupChecksum(short groupIndex, boolean match) {
        this.pkt = 2;
        this.groupIndex = groupIndex;
        this.match = match;
    }

    private void setPacketGroupChecksum(short groupIndex) {
        this.pkt = 2;
        this.groupIndex = groupIndex;
    }

    private void setPacketFileChecksums(short groupIndex, String relPath, byte reason) {
        this.pkt = 3;
        this.groupIndex = groupIndex;
        this.relPath = relPath;
        this.reason = reason;
    }

    private void setPacketFileChecksums() {
        this.pkt = 3;
    }

    private void setPacketError(String error) {
        this.pkt = 4;
        NetChecksum.comparer.error = error;
    }

    @Override
    public void write(ByteBufferWriter b) {
        if (GameServer.server) {
            b.putShort(this.pkt);
            if (this.pkt == 1) {
                b.putBoolean(this.okLua);
                b.putBoolean(this.okScript);
            }

            if (this.pkt == 2) {
                b.putShort(this.groupIndex);
                b.putBoolean(this.match);
            }

            if (this.pkt == 3) {
                b.putShort(this.groupIndex);
                b.putUTF(this.relPath);
                b.putByte(this.reason);
            }

            if (this.pkt == 4) {
                b.putUTF(NetChecksum.comparer.error);
            }
        } else {
            b.putShort(this.pkt);
            if (this.pkt == 1) {
                b.putUTF(GameClient.checksum);
                b.putUTF(ScriptManager.instance.getChecksum());
            }

            if (this.pkt == 2) {
                b.putShort(NetChecksum.comparer.currentIndex);
                b.putShort(this.groupIndex);

                for (short index = NetChecksum.comparer.currentIndex; index <= this.groupIndex; index++) {
                    NetChecksum.GroupOfFiles files = NetChecksum.GroupOfFiles.groups.get(index);
                    b.putShort((short)files.totalChecksum.length);
                    b.bb.put(files.totalChecksum);
                }
            }

            if (this.pkt == 3) {
                NetChecksum.GroupOfFiles files = NetChecksum.GroupOfFiles.groups.get(NetChecksum.comparer.currentIndex);
                b.bb.putShort(NetChecksum.comparer.currentIndex);
                b.putShort(files.fileCount);

                for (int i = 0; i < files.fileCount; i++) {
                    b.putUTF(files.relPaths[i]);
                    b.putByte((byte)files.checksums[i].length);
                    b.bb.put(files.checksums[i]);
                }
            }

            if (this.pkt == 4) {
                b.putUTF(NetChecksum.comparer.error);
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.pkt = b.getShort();
        switch (this.pkt) {
            case 1:
                if (NetChecksum.comparer.state != NetChecksum.Comparer.State.SentTotalChecksum) {
                    NetChecksum.comparer.error = "NetChecksum: received PacketTotalChecksum in state " + NetChecksum.comparer.state;
                    NetChecksum.comparer.state = NetChecksum.Comparer.State.Failed;
                } else {
                    this.okLua = b.get() == 1;
                    this.okScript = b.get() == 1;
                    DebugLog.Multiplayer.noise("total checksum lua=" + this.okLua + " script=" + this.okScript);
                    if (this.okLua && this.okScript) {
                        NetChecksum.comparer.state = NetChecksum.Comparer.State.Success;
                    } else {
                        NetChecksum.comparer.currentIndex = 0;
                        this.sendGroupChecksum();
                    }
                }
                break;
            case 2:
                if (NetChecksum.comparer.state != NetChecksum.Comparer.State.SentGroupChecksum) {
                    NetChecksum.comparer.error = "NetChecksum: received PacketGroupChecksum in state " + NetChecksum.comparer.state;
                    NetChecksum.comparer.state = NetChecksum.Comparer.State.Failed;
                } else {
                    short index = b.getShort();
                    this.match = b.get() == 1;
                    if (index >= NetChecksum.comparer.currentIndex && index < NetChecksum.comparer.currentIndex + 10) {
                        DebugLog.Multiplayer.noise("group checksum " + index + " match=" + this.match);
                        if (this.match) {
                            NetChecksum.comparer.currentIndex = (short)(NetChecksum.comparer.currentIndex + 10);
                            this.sendGroupChecksum();
                        } else {
                            NetChecksum.comparer.currentIndex = index;
                            this.sendFileChecksums();
                        }
                    } else {
                        NetChecksum.comparer.error = "NetChecksum: expected PacketGroupChecksum " + NetChecksum.comparer.currentIndex + " but got " + index;
                        NetChecksum.comparer.state = NetChecksum.Comparer.State.Failed;
                    }
                }
                break;
            case 3:
                if (NetChecksum.comparer.state != NetChecksum.Comparer.State.SentFileChecksums) {
                    NetChecksum.comparer.error = "NetChecksum: received PacketFileChecksums in state " + NetChecksum.comparer.state;
                    NetChecksum.comparer.state = NetChecksum.Comparer.State.Failed;
                } else {
                    this.groupIndex = b.getShort();
                    this.relPath = GameWindow.ReadStringUTF(b);
                    this.reason = b.get();
                    if (this.groupIndex != NetChecksum.comparer.currentIndex) {
                        NetChecksum.comparer.error = "NetChecksum: expected PacketFileChecksums "
                            + NetChecksum.comparer.currentIndex
                            + " but got "
                            + this.groupIndex;
                        NetChecksum.comparer.state = NetChecksum.Comparer.State.Failed;
                    } else {
                        NetChecksum.comparer.error = this.getReason(this.reason);
                        if (DebugLog.isLogEnabled(DebugType.Checksum, LogSeverity.Debug)) {
                            LoggerManager.getLogger("checksum").write(String.format("%s%s", NetChecksum.comparer.error, NetChecksum.checksummer));
                        }

                        NetChecksum.comparer.error = NetChecksum.comparer.error + ":\n" + this.relPath;
                        String absPath = ZomboidFileSystem.instance.getString(this.relPath);
                        if (!absPath.equals(this.relPath)) {
                            NetChecksum.comparer.error = NetChecksum.comparer.error + "\n" + absPath;
                        }

                        NetChecksum.comparer.state = NetChecksum.Comparer.State.Failed;
                    }
                }
                break;
            case 4:
                NetChecksum.comparer.error = GameWindow.ReadStringUTF(b);
                NetChecksum.comparer.state = NetChecksum.Comparer.State.Failed;
                break;
            default:
                NetChecksum.comparer.error = "NetChecksum: unhandled packet " + this.pkt;
                NetChecksum.comparer.state = NetChecksum.Comparer.State.Failed;
        }
    }

    @Override
    public void parseServer(ByteBuffer b, UdpConnection connection) {
        this.pkt = b.getShort();
        switch (this.pkt) {
            case 1:
                String luaChecksum = GameWindow.ReadString(b);
                String scriptChecksum = GameWindow.ReadString(b);
                this.okLua = luaChecksum.equals(GameServer.checksum);
                this.okScript = scriptChecksum.equals(ScriptManager.instance.getChecksum());
                DebugLog.Multiplayer.noise("PacketTotalChecksum lua=" + this.okLua + " script=" + this.okScript);
                if (connection.role.hasCapability(Capability.BypassLuaChecksum)) {
                    this.okLua = this.okScript = true;
                }

                connection.checksumState = this.okLua && this.okScript ? UdpConnection.ChecksumState.Done : UdpConnection.ChecksumState.Different;
                connection.checksumTime = System.currentTimeMillis();
                if (!this.okLua || !this.okScript) {
                    DebugLog.log("user " + connection.username + " will be kicked because Lua/script checksums do not match");
                    String text = "";
                    if (!this.okLua) {
                        text = text + "Lua";
                    }

                    if (!this.okScript) {
                        text = text + "Script";
                    }

                    ServerWorldDatabase.instance.addUserlog(connection.username, Userlog.UserlogType.LuaChecksum, text, this.getClass().getSimpleName(), 1);
                }

                ByteBufferWriter bbw = connection.startPacket();
                PacketTypes.PacketType.Checksum.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.Checksum.send(connection);
                break;
            case 2:
                short firstIndex = b.getShort();
                short lastIndex = b.getShort();
                if (firstIndex >= 0 && lastIndex >= firstIndex && lastIndex < firstIndex + 10) {
                    short index = firstIndex;

                    while (index <= lastIndex) {
                        short numBytes = b.getShort();
                        if (numBytes < 0 || numBytes > this.checksum.length) {
                            sendError(connection, "PacketGroupChecksum: numBytes is invalid");
                            return;
                        }

                        b.get(this.checksum, 0, numBytes);
                        if (index < NetChecksum.GroupOfFiles.groups.size()) {
                            NetChecksum.GroupOfFiles files = NetChecksum.GroupOfFiles.groups.get(index);
                            if (this.checksumEquals(files.totalChecksum)) {
                                index++;
                                continue;
                            }
                        }

                        this.setPacketGroupChecksum(index, false);
                        ByteBufferWriter bbw = connection.startPacket();
                        PacketTypes.PacketType.Checksum.doPacket(bbw);
                        this.write(bbw);
                        PacketTypes.PacketType.Checksum.send(connection);
                        return;
                    }

                    this.setPacketGroupChecksum(firstIndex, true);
                    ByteBufferWriter bbw = connection.startPacket();
                    PacketTypes.PacketType.Checksum.doPacket(bbw);
                    this.write(bbw);
                    PacketTypes.PacketType.Checksum.send(connection);
                } else {
                    sendError(connection, "PacketGroupChecksum: firstIndex and/or lastIndex are invalid");
                }
                break;
            case 3:
                short groupIndex = b.getShort();
                short fileCount = b.getShort();
                if (groupIndex < 0 || fileCount <= 0 || fileCount > 20) {
                    sendError(connection, "PacketFileChecksums: groupIndex and/or fileCount are invalid");
                    return;
                }

                if (groupIndex >= NetChecksum.GroupOfFiles.groups.size()) {
                    String relPath = GameWindow.ReadStringUTF(b);
                    this.sendFileMismatch(connection, groupIndex, relPath, (byte)2);
                    return;
                }

                NetChecksum.GroupOfFiles files = NetChecksum.GroupOfFiles.groups.get(groupIndex);

                for (short fileIndex = 0; fileIndex < fileCount; fileIndex++) {
                    String relPath = GameWindow.ReadStringUTF(b);
                    byte numBytesx = b.get();
                    if (numBytesx < 0 || numBytesx > this.checksum.length) {
                        sendError(connection, "PacketFileChecksums: numBytes is invalid");
                        return;
                    }

                    if (fileIndex >= files.fileCount) {
                        this.sendFileMismatch(connection, groupIndex, relPath, (byte)2);
                        return;
                    }

                    if (!relPath.equals(files.relPaths[fileIndex])) {
                        String absPath = ZomboidFileSystem.instance.getString(relPath);
                        if (absPath.equals(relPath)) {
                            this.sendFileMismatch(connection, groupIndex, relPath, (byte)2);
                            return;
                        }

                        this.sendFileMismatch(connection, groupIndex, files.relPaths[fileIndex], (byte)3);
                        return;
                    }

                    if (numBytesx > files.checksums[fileIndex].length) {
                        this.sendFileMismatch(connection, groupIndex, files.relPaths[fileIndex], (byte)1);
                        return;
                    }

                    b.get(this.checksum, 0, numBytesx);
                    if (!this.checksumEquals(files.checksums[fileIndex])) {
                        this.sendFileMismatch(connection, groupIndex, files.relPaths[fileIndex], (byte)1);
                        return;
                    }
                }

                if (files.fileCount > fileCount) {
                    this.sendFileMismatch(connection, groupIndex, files.relPaths[fileCount], (byte)3);
                    return;
                }

                sendError(connection, "PacketFileChecksums: all checks passed when they shouldn't");
                break;
            case 4:
                String message = GameWindow.ReadStringUTF(b);
                if (DebugLog.isLogEnabled(DebugType.Checksum, LogSeverity.Debug)) {
                    LoggerManager.getLogger("checksum-" + connection.idStr).write(message, null, true);
                }
                break;
            default:
                sendError(connection, "Unknown packet " + this.pkt);
        }
    }

    private String getReason(byte reason) {
        return switch (reason) {
            case 1 -> "File doesn't match the one on the server";
            case 2 -> "File doesn't exist on the server";
            case 3 -> "File doesn't exist on the client";
            default -> "File status unknown";
        };
    }

    private void sendFileMismatch(UdpConnection connection, short groupIndex, String relPath, byte reason) {
        if (GameServer.server) {
            this.setPacketFileChecksums(groupIndex, relPath, reason);
            ByteBufferWriter bbw = connection.startPacket();
            PacketTypes.PacketType.Checksum.doPacket(bbw);
            this.write(bbw);
            PacketTypes.PacketType.Checksum.send(connection);
            if (DebugLog.isLogEnabled(DebugType.Checksum, LogSeverity.Debug)) {
                LoggerManager.getLogger("checksum").write(String.format("%s%s", this.getReason(reason), NetChecksum.checksummer));
                LoggerManager.getLogger("checksum-" + connection.idStr).write(this.getReason(reason));
            }
        }
    }

    public static void sendTotalChecksum() {
        if (GameClient.client) {
            DebugLog.Multiplayer.noise("send total checksum");
            ChecksumPacket packet = new ChecksumPacket();
            packet.setPacketTotalChecksum();
            ByteBufferWriter bbw = GameClient.connection.startPacket();
            PacketTypes.PacketType.Checksum.doPacket(bbw);
            packet.write(bbw);
            PacketTypes.PacketType.Checksum.send(GameClient.connection);
            NetChecksum.comparer.state = NetChecksum.Comparer.State.SentTotalChecksum;
        }
    }

    public static void sendError(UdpConnection connection, String error) {
        ChecksumPacket packet = new ChecksumPacket();
        packet.setPacketError(error);
        ByteBufferWriter bbw = connection.startPacket();
        PacketTypes.PacketType.Checksum.doPacket(bbw);
        packet.write(bbw);
        PacketTypes.PacketType.Checksum.send(connection);
    }

    private boolean checksumEquals(byte[] other) {
        if (other == null) {
            return false;
        } else if (this.checksum.length < other.length) {
            return false;
        } else {
            for (int i = 0; i < other.length; i++) {
                if (this.checksum[i] != other[i]) {
                    return false;
                }
            }

            return true;
        }
    }

    private void sendGroupChecksum() {
        if (GameClient.client) {
            if (NetChecksum.comparer.currentIndex >= NetChecksum.GroupOfFiles.groups.size()) {
                NetChecksum.comparer.state = NetChecksum.Comparer.State.Success;
            } else {
                short lastIndex = (short)Math.min(NetChecksum.comparer.currentIndex + 10 - 1, NetChecksum.GroupOfFiles.groups.size() - 1);
                DebugLog.Multiplayer.noise("send group checksums " + NetChecksum.comparer.currentIndex + "-" + lastIndex);
                this.setPacketGroupChecksum(lastIndex);
                ByteBufferWriter bbw = GameClient.connection.startPacket();
                PacketTypes.PacketType.Checksum.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.Checksum.send(GameClient.connection);
                NetChecksum.comparer.state = NetChecksum.Comparer.State.SentGroupChecksum;
            }
        }
    }

    private void sendFileChecksums() {
        if (GameClient.client) {
            DebugLog.Multiplayer.noise("send file checksums " + NetChecksum.comparer.currentIndex);
            this.setPacketFileChecksums();
            ByteBufferWriter bbw = GameClient.connection.startPacket();
            PacketTypes.PacketType.Checksum.doPacket(bbw);
            this.write(bbw);
            PacketTypes.PacketType.Checksum.send(GameClient.connection);
            NetChecksum.comparer.state = NetChecksum.Comparer.State.SentFileChecksums;
        }
    }
}
