// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import zombie.GameWindow;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.packets.service.ChecksumPacket;

public final class NetChecksum {
    private static final HexFormat HEX = HexFormat.of();
    public static final NetChecksum.Checksummer checksummer = new NetChecksum.Checksummer();
    public static final NetChecksum.Comparer comparer = new NetChecksum.Comparer();

    private static MessageDigest makeDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException var1) {
            throw new IllegalStateException("Every Java platform implementation is required to support MD5", var1);
        }
    }

    public static final class Checksummer {
        private final MessageDigest md = NetChecksum.makeDigest();

        public void reset() {
            this.md.reset();
        }

        public void addFile(String relPath, String absPath) {
            try {
                byte[] bytes = Files.readAllBytes(Path.of(absPath));
                int count = 0;

                for (byte b : bytes) {
                    if (b != 13) {
                        bytes[count++] = b;
                    }
                }

                this.md.update(bytes, 0, count);
                NetChecksum.GroupOfFiles.addFile(relPath, absPath);
                NetChecksum.GroupOfFiles.updateFile(bytes, count);
                NetChecksum.GroupOfFiles.endFile();
            } catch (Exception var9) {
                DebugType.General.printException(var9, "absPath:" + absPath, LogSeverity.Error);
            }
        }

        public String checksumToString() {
            return NetChecksum.HEX.formatHex(this.md.digest());
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();

            for (NetChecksum.GroupOfFiles group : NetChecksum.GroupOfFiles.groups) {
                String message = group.toString();
                stringBuilder.append("\n").append(message);
                if (GameClient.client) {
                    ChecksumPacket.sendError(GameClient.connection, message);
                }
            }

            return stringBuilder.toString();
        }
    }

    public static final class Comparer {
        public static final short NUM_GROUPS_TO_SEND = 10;
        public NetChecksum.Comparer.State state = NetChecksum.Comparer.State.Init;
        public short currentIndex;
        public String error;

        public void beginCompare() {
            this.error = null;
            ChecksumPacket.sendTotalChecksum();
        }

        private void gc() {
            NetChecksum.GroupOfFiles.gc();
        }

        public void update() {
            switch (this.state) {
                case Init:
                case SentTotalChecksum:
                case SentGroupChecksum:
                case SentFileChecksums:
                default:
                    break;
                case Success:
                    this.gc();
                    GameClient.checksumValid = true;
                    break;
                case Failed:
                    this.gc();
                    GameClient.connection.forceDisconnect("checksum-" + this.error);
                    GameWindow.serverDisconnected = true;
                    GameWindow.kickReason = this.error;
            }
        }

        public static enum State {
            Init,
            SentTotalChecksum,
            SentGroupChecksum,
            SentFileChecksums,
            Success,
            Failed;
        }
    }

    public static final class GroupOfFiles {
        public static final int MAX_FILES = 20;
        static final MessageDigest mdTotal = NetChecksum.makeDigest();
        static final MessageDigest mdCurrentFile = NetChecksum.makeDigest();
        public static final ArrayList<NetChecksum.GroupOfFiles> groups = new ArrayList<>();
        static NetChecksum.GroupOfFiles currentGroup;
        public byte[] totalChecksum;
        public short fileCount;
        public final String[] relPaths = new String[20];
        final String[] absPaths = new String[20];
        public final byte[][] checksums = new byte[20][];

        private GroupOfFiles() {
            mdTotal.reset();
            groups.add(this);
        }

        @Override
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder()
                .append(this.fileCount)
                .append(" files, ")
                .append(this.absPaths.length)
                .append("/")
                .append(this.relPaths.length)
                .append("/")
                .append(this.checksums.length)
                .append(" \"")
                .append(NetChecksum.HEX.formatHex(this.totalChecksum))
                .append("\"");

            for (int i = 0; i < 20; i++) {
                stringBuilder.append("\n");
                if (i < this.relPaths.length) {
                    stringBuilder.append(" \"").append(this.relPaths[i]).append("\"");
                }

                if (i < this.checksums.length) {
                    if (this.checksums[i] == null) {
                        stringBuilder.append(" \"\"");
                    } else {
                        stringBuilder.append(" \"").append(NetChecksum.HEX.formatHex(this.checksums[i])).append("\"");
                    }
                }

                if (i < this.absPaths.length) {
                    stringBuilder.append(" \"").append(this.absPaths[i]).append("\"");
                }
            }

            return stringBuilder.toString();
        }

        private void gc_() {
            Arrays.fill(this.relPaths, null);
            Arrays.fill(this.absPaths, null);
            Arrays.fill(this.checksums, null);
        }

        public static void initChecksum() {
            groups.clear();
            currentGroup = null;
        }

        public static void finishChecksum() {
            if (currentGroup != null) {
                currentGroup.totalChecksum = mdTotal.digest();
                currentGroup = null;
            }
        }

        private static void addFile(String relPath, String absPath) {
            if (currentGroup == null) {
                currentGroup = new NetChecksum.GroupOfFiles();
            }

            currentGroup.relPaths[currentGroup.fileCount] = relPath;
            currentGroup.absPaths[currentGroup.fileCount] = absPath;
            mdCurrentFile.reset();
        }

        private static void updateFile(byte[] data, int count) {
            mdCurrentFile.update(data, 0, count);
            mdTotal.update(data, 0, count);
        }

        private static void endFile() {
            currentGroup.checksums[currentGroup.fileCount] = mdCurrentFile.digest();
            currentGroup.fileCount++;
            if (currentGroup.fileCount >= 20) {
                currentGroup.totalChecksum = mdTotal.digest();
                currentGroup = null;
            }
        }

        public static void gc() {
            for (NetChecksum.GroupOfFiles group : groups) {
                group.gc_();
            }

            groups.clear();
        }
    }
}
