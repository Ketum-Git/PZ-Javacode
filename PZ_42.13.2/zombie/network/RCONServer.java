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
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.debug.DebugLog;

public class RCONServer {
    public static final int SERVERDATA_RESPONSE_VALUE = 0;
    public static final int SERVERDATA_AUTH_RESPONSE = 2;
    public static final int SERVERDATA_EXECCOMMAND = 2;
    public static final int SERVERDATA_AUTH = 3;
    private static RCONServer instance;
    private ServerSocket welcomeSocket;
    private RCONServer.ServerThread thread;
    private final String password;
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
            var7.printStackTrace();

            try {
                this.welcomeSocket.close();
                this.welcomeSocket = null;
            } catch (IOException var6) {
                var6.printStackTrace();
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
                var4.printStackTrace();
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
                            var4.printStackTrace();
                        }
                    }

                    try {
                        this.socket.close();
                    } catch (IOException var2) {
                        var2.printStackTrace();
                    }

                    DebugLog.DetailedInfo.trace("RCON: connection closed " + this.socket.toString());
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
                int ID = bb.getInt();
                int Type = bb.getInt();
                String Body = new String(bb.array(), bb.position(), bb.limit() - bb.position() - 2);
                this.handlePacket(ID, Type, Body);
            }
        }

        private void handlePacket(int ID, int Type, String Body) throws IOException {
            if (!"players".equals(Body)) {
                DebugLog.DetailedInfo.trace("RCON: ID=" + ID + " Type=" + Type + " Body='" + Body + "' " + this.socket.toString());
            }

            switch (Type) {
                case 0:
                    if (this.checkAuth()) {
                        ByteBuffer bb = ByteBuffer.allocate(14);
                        bb.order(ByteOrder.LITTLE_ENDIAN);
                        bb.putInt(bb.capacity() - 4);
                        bb.putInt(ID);
                        bb.putInt(0);
                        bb.putShort((short)0);
                        this.out.write(bb.array());
                        this.out.write(bb.array());
                    }
                    break;
                case 1:
                default:
                    DebugLog.log("RCON: unknown packet Type=" + Type);
                    break;
                case 2:
                    if (!this.checkAuth()) {
                        break;
                    }

                    RCONServer.ExecCommand command = new RCONServer.ExecCommand(ID, Body, this);
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
                    this.auth = Body.equals(this.password);
                    if (!this.auth) {
                        DebugLog.log("RCON: password doesn't match");
                        this.quit = true;
                    }

                    ByteBuffer bb = ByteBuffer.allocate(14);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    bb.putInt(bb.capacity() - 4);
                    bb.putInt(ID);
                    bb.putInt(0);
                    bb.putShort((short)0);
                    this.out.write(bb.array());
                    bb.clear();
                    bb.putInt(bb.capacity() - 4);
                    bb.putInt(this.auth ? ID : -1);
                    bb.putInt(2);
                    bb.putShort((short)0);
                    this.out.write(bb.array());
            }
        }

        public void handleResponse(RCONServer.ExecCommand command) {
            String s = command.response;
            if (s == null) {
                s = "";
            }

            ByteBuffer bb = ByteBuffer.allocate(12 + s.length() + 2);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            bb.putInt(bb.capacity() - 4);
            bb.putInt(command.id);
            bb.putInt(0);
            bb.put(s.getBytes());
            bb.putShort((short)0);

            try {
                this.out.write(bb.array());
            } catch (IOException var5) {
                var5.printStackTrace();
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
                    var2.printStackTrace();
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

                DebugLog.DetailedInfo.trace("RCON: new connection " + socket.toString());
                RCONServer.ClientThread connection = new RCONServer.ClientThread(socket, RCONServer.this.password);
                this.connections.add(connection);
                connection.start();
            } catch (IOException var4) {
                if (!this.quit) {
                    var4.printStackTrace();
                }
            }
        }

        public void quit() {
            this.quit = true;

            while (this.isAlive()) {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException var3) {
                    var3.printStackTrace();
                }
            }

            for (int i = 0; i < this.connections.size(); i++) {
                RCONServer.ClientThread connection = this.connections.get(i);
                connection.quit();
            }
        }
    }
}
