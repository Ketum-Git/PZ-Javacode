// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;

public class RCONServer {
    public static final int SERVERDATA_RESPONSE_VALUE = 0;
    public static final int SERVERDATA_AUTH_RESPONSE = 2;
    public static final int SERVERDATA_EXECCOMMAND = 2;
    public static final int SERVERDATA_AUTH = 3;
    private static RCONServer instance;
    private ServerSocket welcomeSocket;
    private RCONServer.ServerThread thread;
    private final String password;
    private static final int MAX_PACKET_SIZE = 4096;
    private final ConcurrentLinkedQueue<RCONServer.ExecCommand> toMain = new ConcurrentLinkedQueue<>();

    private RCONServer(int port, String password, boolean isLocal) {
        this.password = password;

        try {
            this.welcomeSocket = new ServerSocket();
            if (isLocal) {
                this.welcomeSocket.bind(new InetSocketAddress("127.0.0.1", port));
            } else if (GameServer.ipCommandline != null) {
                this.welcomeSocket.bind(new InetSocketAddress(GameServer.ipCommandline, port));
            } else {
                this.welcomeSocket.bind(new InetSocketAddress(port));
            }

            DebugLog.log("RCON: listening on port " + port);
        } catch (IOException var7) {
            DebugLog.log("RCON: error creating socket on port " + port);
            DebugType.General.printException(var7, LogSeverity.Error);

            try {
                this.welcomeSocket.close();
                this.welcomeSocket = null;
            } catch (IOException var6) {
                DebugType.General.printException(var6, LogSeverity.Error);
            }

            return;
        }

        this.thread = new RCONServer.ServerThread();
        this.thread.start();
    }

    private void updateMain() {
        for (RCONServer.ExecCommand command = this.toMain.poll(); command != null; command = this.toMain.poll()) {
            command.update();
        }
    }

    public void quit() {
        if (this.welcomeSocket != null) {
            try {
                this.welcomeSocket.close();
            } catch (IOException var2) {
            }

            this.welcomeSocket = null;
            this.thread.quit();
            this.thread = null;
        }
    }

    public static void init(int port, String password, boolean isLocal) {
        instance = new RCONServer(port, password, isLocal);
    }

    public static void update() {
        if (instance != null) {
            instance.updateMain();
        }
    }

    public static void shutdown() {
        if (instance != null) {
            instance.quit();
        }
    }

    private static class ClientThread extends Thread {
        public Socket socket;
        public boolean auth;
        public boolean quit;
        private final String password;
        private InputStream in;
        private OutputStream out;
        private final ConcurrentLinkedQueue<RCONServer.ExecCommand> toThread = new ConcurrentLinkedQueue<>();
        private int pendingCommands;

        public ClientThread(Socket socket, String password) {
            this.socket = socket;
            this.password = password;

            try {
                this.in = socket.getInputStream();
                this.out = socket.getOutputStream();
            } catch (IOException var4) {
                DebugType.General.printException(var4, LogSeverity.Error);
            }

            this.setName("RCONClient" + socket.getLocalPort());
        }

        @Override
        public void run() {
            if (this.in != null) {
                if (this.out != null) {
                    while (!this.quit) {
                        try {
                            this.runInner();
                        } catch (SocketException var3) {
                            this.quit = true;
                        } catch (Exception var4) {
                            DebugType.General.printException(var4, LogSeverity.Error);
                        }
                    }

                    try {
                        this.socket.close();
                    } catch (IOException var2) {
                        DebugType.General.printException(var2, LogSeverity.Error);
                    }

                    DebugType.DetailedInfo.trace("RCON: connection closed " + this.socket.toString());
                }
            }
        }

        private void runInner() throws IOException {
            byte[] bytes = new byte[4];
            int receivedBytes = this.in.read(bytes, 0, 4);
            if (receivedBytes < 0) {
                this.quit = true;
            } else {
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                int packetSize = bb.getInt();
                int remainingBytes = packetSize;
                byte[] packetData = new byte[packetSize];

                do {
                    receivedBytes = this.in.read(packetData, packetSize - remainingBytes, remainingBytes);
                    if (receivedBytes < 0) {
                        this.quit = true;
                        return;
                    }

                    remainingBytes -= receivedBytes;
                } while (remainingBytes > 0);

                bb = ByteBuffer.wrap(packetData);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                int id = bb.getInt();
                int type = bb.getInt();
                String body = new String(bb.array(), bb.position(), bb.limit() - bb.position() - 2);
                this.handlePacket(id, type, body);
            }
        }

        private void handlePacket(int id, int type, String body) throws IOException {
            if (!"players".equals(body)) {
                DebugType.DetailedInfo.trace("RCON: ID=" + id + " Type=" + type + " Body='" + body + "' " + this.socket.toString());
            }

            switch (type) {
                case 0:
                    if (this.checkAuth()) {
                        ByteBuffer bb = ByteBuffer.allocate(14);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.putInt(bb.capacity() - 4);
                        bb.putInt(id);
                        bb.putInt(0);
                        bb.putShort((short)0);
                        this.out.write(bb.array());
                        this.out.write(bb.array());
                    }
                    break;
                case 1:
                default:
                    DebugLog.log("RCON: unknown packet Type=" + type);
                    break;
                case 2:
                    if (!this.checkAuth()) {
                        break;
                    }

                    RCONServer.ExecCommand command = new RCONServer.ExecCommand(id, body, this);
                    this.pendingCommands++;
                    RCONServer.instance.toMain.add(command);

                    while (this.pendingCommands > 0) {
                        command = this.toThread.poll();
                        if (command != null) {
                            this.pendingCommands--;
                            this.handleResponse(command);
                        } else {
                            try {
                                Thread.sleep(50L);
                            } catch (InterruptedException var7) {
                                if (this.quit) {
                                    return;
                                }
                            }
                        }
                    }
                    break;
                case 3:
                    this.auth = body.equals(this.password);
                    if (!this.auth) {
                        DebugLog.log("RCON: password doesn't match");
                        this.quit = true;
                    }

                    ByteBuffer bb = ByteBuffer.allocate(14);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    bb.putInt(bb.capacity() - 4);
                    bb.putInt(id);
                    bb.putInt(0);
                    bb.putShort((short)0);
                    this.out.write(bb.array());
                    bb.clear();
                    bb.putInt(bb.capacity() - 4);
                    bb.putInt(this.auth ? id : -1);
                    bb.putInt(2);
                    bb.putShort((short)0);
                    this.out.write(bb.array());
            }
        }

        public void handleResponse(RCONServer.ExecCommand command) {
            String s = Objects.requireNonNullElse(command.response, "");
            byte[] data = s.getBytes(StandardCharsets.UTF_8);
            ByteBuffer bb = ByteBuffer.allocate(Math.min(12 + data.length + 2, 4100));
            bb.order(ByteOrder.LITTLE_ENDIAN);
            int sendBytes = 0;

            while (sendBytes < data.length) {
                int size = Math.min(data.length - sendBytes, 4086);
                bb.clear();
                bb.putInt(8 + size + 2);
                bb.putInt(command.id);
                bb.putInt(0);
                bb.put(data, sendBytes, size);
                bb.putShort((short)0);

                try {
                    this.out.write(bb.array(), 0, 12 + size + 2);
                } catch (IOException var8) {
                    DebugType.General.printException(var8, LogSeverity.Error);
                    break;
                }

                sendBytes += size;
            }
        }

        private boolean checkAuth() throws IOException {
            if (this.auth) {
                return true;
            } else {
                this.quit = true;
                ByteBuffer bb = ByteBuffer.allocate(14);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.putInt(bb.capacity() - 4);
                bb.putInt(-1);
                bb.putInt(2);
                bb.putShort((short)0);
                this.out.write(bb.array());
                return false;
            }
        }

        public void quit() {
            if (this.socket != null) {
                try {
                    this.socket.close();
                } catch (IOException var3) {
                }
            }

            this.quit = true;
            this.interrupt();

            while (this.isAlive()) {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException var2) {
                    DebugType.General.printException(var2, LogSeverity.Error);
                }
            }
        }
    }

    private static class ExecCommand {
        public int id;
        public String command;
        public String response;
        public RCONServer.ClientThread thread;

        public ExecCommand(int id, String command, RCONServer.ClientThread thread) {
            this.id = id;
            this.command = command;
            this.thread = thread;
        }

        public void update() {
            this.response = GameServer.rcon(this.command);
            if (this.thread.isAlive()) {
                this.thread.toThread.add(this);
            }
        }
    }

    private class ServerThread extends Thread {
        private final ArrayList<RCONServer.ClientThread> connections;
        public boolean quit;

        public ServerThread() {
            Objects.requireNonNull(RCONServer.this);
            super();
            this.connections = new ArrayList<>();
            this.setName("RCONServer");
        }

        @Override
        public void run() {
            while (!this.quit) {
                this.runInner();
            }
        }

        private void runInner() {
            try {
                Socket socket = RCONServer.this.welcomeSocket.accept();

                for (int i = 0; i < this.connections.size(); i++) {
                    RCONServer.ClientThread connection = this.connections.get(i);
                    if (!connection.isAlive()) {
                        this.connections.remove(i--);
                    }
                }

                if (this.connections.size() >= 5) {
                    socket.close();
                    return;
                }

                DebugType.DetailedInfo.trace("RCON: new connection " + socket.toString());
                RCONServer.ClientThread connection = new RCONServer.ClientThread(socket, RCONServer.this.password);
                this.connections.add(connection);
                connection.start();
            } catch (IOException var4) {
                if (!this.quit) {
                    DebugType.General.printException(var4, LogSeverity.Error);
                }
            }
        }

        public void quit() {
            this.quit = true;

            while (this.isAlive()) {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException var3) {
                    DebugType.General.printException(var3, LogSeverity.Error);
                }
            }

            for (int i = 0; i < this.connections.size(); i++) {
                RCONServer.ClientThread connection = this.connections.get(i);
                connection.quit();
            }
        }
    }
}
