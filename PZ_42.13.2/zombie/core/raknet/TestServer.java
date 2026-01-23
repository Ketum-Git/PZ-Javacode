// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.raknet;

import java.nio.ByteBuffer;

public class TestServer {
    static RakNetPeerInterface server;
    static ByteBuffer buf = ByteBuffer.allocate(2048);

    public static void main(String[] args) {
        server = new RakNetPeerInterface();
        server.SetServerPort(12203, 12204);
        server.Init(false);
        int result = server.Startup(32);
        System.out.println("Result: " + result);
        server.SetMaximumIncomingConnections(32);
        server.SetOccasionalPing(true);
        server.SetIncomingPassword("spiffo");
        boolean bDone = false;

        while (true) {
            String test = "This is a test message";
            ByteBuffer buf = Receive();
            decode(buf);
        }
    }

    private static void decode(ByteBuffer buf) {
        int packetIdentifier = buf.get() & 255;
        switch (packetIdentifier) {
            case 0:
            case 1:
                System.out.println("PING");
                break;
            case 19:
                int id = buf.get() & 255;
                long guid = server.getGuidFromIndex(id);
                break;
            case 21:
                System.out.println("ID_DISCONNECTION_NOTIFICATION");
                break;
            case 22:
                System.out.println("ID_CONNECTION_LOST");
                break;
            case 25:
                System.out.println("ID_INCOMPATIBLE_PROTOCOL_VERSION");
                break;
            default:
                System.out.println("Other: " + packetIdentifier);
        }
    }

    public static ByteBuffer Receive() {
        int lastPosition = buf.position();
        boolean bRead = false;

        do {
            try {
                Thread.sleep(1L);
            } catch (InterruptedException var3) {
                var3.printStackTrace();
            }

            bRead = server.Receive(buf);
        } while (!bRead);

        return buf;
    }
}
