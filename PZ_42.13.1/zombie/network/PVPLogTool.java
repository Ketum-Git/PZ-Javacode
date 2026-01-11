// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import zombie.UsedFromLua;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.chat.ChatServer;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@UsedFromLua
public class PVPLogTool {
    private static final int MAX_EVENTS = 10;
    private static final ArrayList<PVPLogTool.PVPEvent> events = new ArrayList<>();

    private PVPLogTool() {
    }

    public static void clearEvents() {
        for (PVPLogTool.PVPEvent event : events) {
            event.reset(null, null, null, 0.0F, 0.0F, 0.0F);
        }
    }

    public static ArrayList<PVPLogTool.PVPEvent> getEvents() {
        return events;
    }

    public static void logSafety(IsoPlayer player, String event) {
        String message = String.format("Safety: \"%s\" %s %s", player.getUsername(), event, player.getSafety().isEnabled());
        String text = String.format(
            "Safety: \"%s\" %s %s %s", player.getUsername(), LoggerManager.getPlayerCoords(player), event, player.getSafety().isEnabled()
        );
        log(message, text, "LOG");
    }

    public static void logKill(IsoPlayer wielder, IsoPlayer target) {
        String message = String.format("Kill: \"%s\" killed \"%s\"", wielder.getUsername(), target.getUsername());
        String text = String.format(
            "Kill: \"%s\" %s killed \"%s\" %s",
            wielder.getUsername(),
            LoggerManager.getPlayerCoords(wielder),
            target.getUsername(),
            LoggerManager.getPlayerCoords(target)
        );
        log(message, text, "IMPORTANT");
        if (ServerOptions.instance.announceDeath.getValue()) {
            ChatServer.getInstance().sendMessageToServerChat(message);
        }
    }

    public static void logCombat(
        String wielder, String wielderPosition, String target, String targetPosition, float x, float y, float z, String weapon, float damage
    ) {
        String message = String.format("Combat: \"%s\" hit \"%s\" \"%s\" %f", wielder, target, weapon, damage);
        String text = String.format("Combat: \"%s\" %s hit \"%s\" %s weapon=\"%s\" damage=%f", wielder, wielderPosition, target, targetPosition, weapon, damage);
        log(message, text, "INFO");
        PVPLogTool.PVPEvent event = events.remove(9);
        event.reset(wielder, target, x, y, z);
        events.add(0, event);

        for (UdpConnection connection : GameServer.udpEngine.connections) {
            if (connection.isFullyConnected() && connection.role.hasCapability(Capability.PVPLogTool)) {
                INetworkPacket.send(connection, PacketTypes.PacketType.PVPEvents, false);
            }
        }
    }

    private static void log(String message, String text, String level) {
        if (GameServer.server) {
            if (ServerOptions.getInstance().pvpLogToolChat.getValue()) {
                ChatServer.getInstance().sendMessageToAdminChat(message);
            }

            if (ServerOptions.getInstance().pvpLogToolFile.getValue()) {
                LoggerManager.getLogger("pvp").write(text, level);
            }

            DebugLog.Multiplayer.debugln(text);
        }
    }

    static {
        for (int i = 0; i < 10; i++) {
            events.add(new PVPLogTool.PVPEvent(null, null, 0.0F, 0.0F, 0.0F));
        }
    }

    @UsedFromLua
    public static class PVPEvent {
        private static final SimpleDateFormat format = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");
        public String timestamp;
        public String wielder;
        public String target;
        public float x;
        public float y;
        public float z;

        public PVPEvent(String wielder, String target, float x, float y, float z) {
            this.reset(wielder, target, x, y, z);
        }

        public void reset(String wielder, String target, float x, float y, float z) {
            this.reset(format.format(Calendar.getInstance().getTime()), wielder, target, x, y, z);
        }

        public void reset(String timestamp, String wielder, String target, float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.wielder = wielder;
            this.target = target;
            this.timestamp = timestamp;
        }

        public String getText() {
            return String.format("[%s] \"%s\" hit \"%s\"", this.timestamp, this.wielder, this.target);
        }

        public boolean isSet() {
            return !StringUtils.isNullOrEmpty(this.wielder) && !StringUtils.isNullOrEmpty(this.target);
        }

        public float getX() {
            return this.x;
        }

        public float getY() {
            return this.y;
        }

        public float getZ() {
            return this.z;
        }
    }
}
