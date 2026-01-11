// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.raknet;

import java.nio.ByteBuffer;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.network.GameServer;

public class VoiceTest {
    protected static boolean quit;
    protected static ByteBuffer serverBuf = ByteBuffer.allocate(500000);
    protected static ByteBuffer clientBuf = ByteBuffer.allocate(500000);
    protected static RakNetPeerInterface rnclientPeer;
    protected static RakNetPeerInterface rnserverPeer;

    protected static void rakNetServer(int port) {
        int maxConnections = 2;
        String serverPassword = "test";
        rnserverPeer = new RakNetPeerInterface();
        DebugLog.log("Initialising RakNet...");
        rnserverPeer.Init(false);
        rnserverPeer.SetMaximumIncomingConnections(2);
        if (GameServer.ipCommandline != null) {
            rnserverPeer.SetServerIP(GameServer.ipCommandline);
        }

        rnserverPeer.SetServerPort(port, port + 1);
        rnserverPeer.SetIncomingPassword("test");
        rnserverPeer.SetOccasionalPing(true);
        int startupResult = rnserverPeer.Startup(2);
        System.out.println("RakNet.Startup() return code: " + startupResult + " (0 means success)");
    }

    public static ByteBuffer rakNetServerReceive() {
        boolean bRead = false;

        do {
            try {
                Thread.sleep(1L);
            } catch (InterruptedException var2) {
                var2.printStackTrace();
            }

            bRead = rnserverPeer.Receive(serverBuf);
        } while (!quit && !bRead);

        return serverBuf;
    }

    private static void rakNetServerDecode(ByteBuffer buf) {
        int packetIdentifier = buf.get() & 255;
        switch (packetIdentifier) {
            case 0:
            case 1:
                System.out.println("PING");
                break;
            case 16: {
                System.out.println("Connection Request Accepted");
                int id = buf.get() & 255;
                long guid = rnserverPeer.getGuidOfPacket();
                VoiceManager.instance.VoiceConnectReq(guid);
                break;
            }
            case 19: {
                System.out.println("ID_NEW_INCOMING_CONNECTION");
                int id = buf.get() & 255;
                long guid = rnserverPeer.getGuidOfPacket();
                System.out.println("id=" + id + " guid=" + guid);
                VoiceManager.instance.VoiceConnectReq(guid);
                break;
            }
            default:
                System.out.println("Received: " + packetIdentifier);
        }
    }

    protected static void rakNetClient() {
        int maxConnections = 2;
        String serverPassword = "test";
        rnclientPeer = new RakNetPeerInterface();
        DebugLog.log("Initialising RakNet...");
        rnclientPeer.Init(false);
        rnclientPeer.SetMaximumIncomingConnections(2);
        rnclientPeer.SetClientPort(GameServer.defaultPort + Rand.Next(10000) + 1234);
        rnclientPeer.SetOccasionalPing(true);
        int startupResult = rnclientPeer.Startup(2);
        System.out.println("RakNet.Startup() return code: " + startupResult + " (0 means success)");
    }

    public static ByteBuffer rakNetClientReceive() {
        boolean bRead = false;

        do {
            try {
                Thread.sleep(1L);
            } catch (InterruptedException var2) {
                var2.printStackTrace();
            }

            bRead = rnclientPeer.Receive(clientBuf);
        } while (!quit && !bRead);

        return clientBuf;
    }

    private static void rakNetClientDecode(ByteBuffer buf) {
        int packetIdentifier = buf.get() & 255;
        switch (packetIdentifier) {
            case 0:
            case 1:
                System.out.println("PING");
                break;
            case 16: {
                System.out.println("Connection Request Accepted");
                int id = buf.get() & 255;
                long guid = rnclientPeer.getGuidOfPacket();
                VoiceManager.instance.VoiceConnectReq(guid);
                break;
            }
            case 19: {
                System.out.println("ID_NEW_INCOMING_CONNECTION");
                int id = buf.get() & 255;
                long guid = rnclientPeer.getGuidOfPacket();
                System.out.println("id=" + id + " guid=" + guid);
                VoiceManager.instance.VoiceConnectReq(guid);
                break;
            }
            default:
                System.out.println("Received: " + packetIdentifier);
        }
    }

    public static void main(String[] args) {
        DebugLog.log("VoiceTest: START");
        DebugLog.log("version=" + Core.getInstance().getVersion() + " demo=false");
        DebugLog.log("VoiceTest: SteamUtils.init - EXEC");
        SteamUtils.init();
        DebugLog.log("VoiceTest: SteamUtils.init - OK");
        DebugLog.log("VoiceTest: RakNetPeerInterface - EXEC");
        RakNetPeerInterface.init();
        DebugLog.log("VoiceTest: RakNetPeerInterface - OK");
        DebugLog.log("VoiceTest: VoiceManager.InitVMServer - EXEC");
        VoiceManager.instance.InitVMServer();
        DebugLog.log("VoiceTest: VoiceManager.InitVMServer - OK");
        DebugLog.log("VoiceTest: rakNetServer - EXEC");
        rakNetServer(16000);
        DebugLog.log("VoiceTest: rakNetServer - OK");
        DebugLog.log("VoiceTest: rakNetClient - EXEC");
        rakNetClient();
        DebugLog.log("VoiceTest: rakNetClient - OK");
        DebugLog.log("VoiceTest: rnclientPeer.Connect - EXEC");
        rnclientPeer.Connect("127.0.0.1", 16000, "test", false);
        DebugLog.log("VoiceTest: rnclientPeer.Connect - OK");
        Thread serverThread = new Thread() {
            @Override
            public void run() {
                while (!VoiceTest.quit && !VoiceTest.quit) {
                    ByteBuffer buffer = VoiceTest.rakNetServerReceive();

                    try {
                        VoiceTest.rakNetServerDecode(buffer);
                    } catch (Exception var3) {
                        var3.printStackTrace();
                    }
                }
            }
        };
        serverThread.setName("serverThread");
        serverThread.start();
        Thread clientThread = new Thread() {
            @Override
            public void run() {
                while (!VoiceTest.quit && !VoiceTest.quit) {
                    ByteBuffer buffer = VoiceTest.rakNetClientReceive();

                    try {
                        VoiceTest.rakNetClientDecode(buffer);
                    } catch (Exception var3) {
                        var3.printStackTrace();
                    }
                }
            }
        };
        clientThread.setName("clientThread");
        clientThread.start();
        DebugLog.log("VoiceTest: sleep 10 sec");

        try {
            Thread.sleep(10000L);
        } catch (InterruptedException var4) {
            var4.printStackTrace();
        }
    }
}
