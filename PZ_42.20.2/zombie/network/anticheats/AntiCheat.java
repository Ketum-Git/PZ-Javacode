// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import zombie.characters.Capability;
import zombie.core.Core;
import zombie.core.logger.LoggerManager;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.BanSystem;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.ServerWorldDatabase;
import zombie.network.Userlog;
import zombie.network.chat.ChatServer;
import zombie.network.packets.INetworkPacket;
import zombie.network.server.EventManager;

public enum AntiCheat {
    Safety(AntiCheatSafety.class, ServerOptions.instance.antiCheatSafety, 1),
    HitDamage(AntiCheatHitDamage.class, ServerOptions.instance.antiCheatHit, 1),
    HitVehicle(AntiCheatHitVehicle.class, ServerOptions.instance.antiCheatHit, 1),
    HitLongDistance(AntiCheatHitLongDistance.class, ServerOptions.instance.antiCheatHit, 2),
    HitShortDistance(AntiCheatHitShortDistance.class, ServerOptions.instance.antiCheatHit, 2),
    HitWeapon(AntiCheatHitWeapon.class, ServerOptions.instance.antiCheatHit, 2),
    PacketRakNet(AntiCheatPacketRakNet.class, ServerOptions.instance.antiCheatPacketException, 1),
    PacketException(AntiCheatPacketException.class, ServerOptions.instance.antiCheatPacketException, 1),
    PacketType(AntiCheatPacketType.class, ServerOptions.instance.antiCheatPacketException, 1),
    XPUpdate(AntiCheatXPUpdate.class, ServerOptions.instance.antiCheatXp, 2),
    ChecksumUpdate(AntiCheatChecksumUpdate.class, ServerOptions.instance.antiCheatChecksum, 1),
    Target(AntiCheatTarget.class, ServerOptions.instance.antiCheatPlayer, 2),
    Player(AntiCheatPlayer.class, ServerOptions.instance.antiCheatPlayer, 1),
    PlayerUpdate(AntiCheatPlayerUpdate.class, ServerOptions.instance.antiCheatPlayer, 4),
    Power(AntiCheatPower.class, ServerOptions.instance.antiCheatPermission, 1),
    Capability(AntiCheatCapability.class, ServerOptions.instance.antiCheatPermission, 1),
    SafeHouseOwner(AntiCheatSafeHouseOwner.class, ServerOptions.instance.antiCheatSafeHouse, 1),
    SafeHouseMember(AntiCheatSafeHouseMember.class, ServerOptions.instance.antiCheatSafeHouse, 1),
    SafeHousePlayer(AntiCheatSafeHouseNotMember.class, ServerOptions.instance.antiCheatSafeHouse, 1),
    Speed(AntiCheatSpeed.class, ServerOptions.instance.antiCheatSpeed, 4),
    NoClip(AntiCheatNoClip.class, ServerOptions.instance.antiCheatNoClip, 4),
    None(AntiCheatNone.class, null, 0);

    public static final String REASON_UNKNOWN_RAKNET_PACKET = "unknown RakNet packet";
    public static final String REASON_UNKNOWN_ZED_PACKET = "unknown Zed packet";
    public static final String REASON_EXCEPTION_OCCURRED = "exception occurred";
    public static final String REASON_INCORRECT_CHECKSUM = "incorrect checksum";
    public static final String REASON_NO_CAPABILITY = "no capability";
    public static final String REASON_UPDATE_FAILED = "update failed";
    public static final String REASON_COOLDOWN = "cooldown";
    public static final String REASON_SKIP = "skip";
    private static final long USER_LOG_INTERVAL_MS = 1000L;
    public final AbstractAntiCheat anticheat;
    public final ServerOptions.EnumServerOption option;
    public final int maxSuspiciousCounter;

    private AntiCheat(final Class<? extends AbstractAntiCheat> antiCheat, final ServerOptions.EnumServerOption option, final int maxSuspiciousCounter) {
        AbstractAntiCheat abstractAntiCheat;
        try {
            abstractAntiCheat = antiCheat.getDeclaredConstructor().newInstance();
            abstractAntiCheat.setAntiCheat(this);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException var8) {
            abstractAntiCheat = null;
        }

        this.anticheat = abstractAntiCheat;
        this.option = option;
        this.maxSuspiciousCounter = maxSuspiciousCounter;
    }

    public boolean isEnabled() {
        return (!GameServer.coop || Core.antiCheats) && this.option != null && this.option.getValue() != 4;
    }

    public boolean isValid(UdpConnection connection, INetworkPacket packet) {
        if (GameServer.server && this.isEnabled()) {
            String result = this.anticheat.validate(connection, packet);
            if (result != null) {
                this.anticheat.react(connection, packet);
                if ("".equals(result)) {
                    log(connection, this, connection.getValidator().getCounters().getOrDefault(this, 0), packet.getClass().getSimpleName() + ": skip");
                } else {
                    this.act(connection, packet.getClass().getSimpleName() + ": " + result);
                }

                return false;
            }
        }

        return true;
    }

    public static void update(UdpConnection connection) {
        for (AntiCheat antiCheat : values()) {
            if (!antiCheat.anticheat.update(connection) && !GameServer.isDelayedDisconnect(connection)) {
                antiCheat.act(connection, "update failed");
            }
        }
    }

    public static void log(UdpConnection connection, String message) {
        message = message + " ping=" + connection.getLastPing();
        DebugType.Multiplayer.warn(message);
        LoggerManager.getLogger("user").write(message);
        ChatServer.getInstance().sendMessageToAdminChat(message);
        EventManager.instance().report(message);
    }

    public static void log(UdpConnection connection, AntiCheat antiCheat, int counter, String text) {
        if (antiCheat.isEnabled()) {
            try {
                String message = String.format(
                    "Anti-cheat=\"%s\" is triggered for connection=\"%s\" server-option=\"%s\" reason=\"%s\" counter=%d/%d action=\"%s\"",
                    antiCheat.name(),
                    connection.getUserName(),
                    antiCheat.option.getName(),
                    text,
                    counter,
                    antiCheat.maxSuspiciousCounter,
                    AntiCheat.Policy.name(antiCheat.option.getValue())
                );
                log(connection, message);
            } catch (Exception var5) {
                DebugType.Multiplayer.printException(var5, "Log anti-cheat error", LogSeverity.Error);
            }
        }
    }

    public void act(UdpConnection connection, String text) {
        if (this.isEnabled() && connection.isFullyConnected() && !connection.getRole().hasCapability(zombie.characters.Capability.CantBeKickedByAnticheat)) {
            int counter = connection.getValidator().report(this);
            log(connection, this, counter, text);
            if (!Core.debug) {
                doLogUser(connection, this.option.getName(), text);
                if (counter >= this.maxSuspiciousCounter) {
                    switch (this.option.getValue()) {
                        case 1:
                            doBanUser(connection, this.option.getName(), text);
                            break;
                        case 2:
                            doKickUser(connection, this.option.getName(), text);
                    }
                }
            }
        }
    }

    public static void doLogUser(UdpConnection connection, String issuedBy, String text) {
        long currentTime = System.currentTimeMillis();
        if (currentTime > connection.lastUnauthorizedPacket) {
            connection.lastUnauthorizedPacket = currentTime + 1000L;
            ServerWorldDatabase.instance.addUserlog(connection.getUserName(), Userlog.UserlogType.SuspiciousActivity, text, issuedBy, 1);
        }
    }

    public static void doKickUser(UdpConnection connection, String issuedBy, String text) {
        if (!connection.getRole().hasCapability(zombie.characters.Capability.CantBeKickedByAnticheat)) {
            ServerWorldDatabase.instance.addUserlog(connection.getUserName(), Userlog.UserlogType.Kicked, text, issuedBy, 1);
            GameServer.kick(connection, "UI_Policy_Kick", "UI_ValidationFailed");
            connection.forceDisconnect(issuedBy);
        }
    }

    public static void doBanUser(UdpConnection connection, String issuedBy, String text) {
        if (!connection.getRole().hasCapability(zombie.characters.Capability.CantBeBannedByAnticheat)) {
            ServerWorldDatabase.instance.addUserlog(connection.getUserName(), Userlog.UserlogType.Banned, text, issuedBy, 1);

            try {
                BanSystem.BanUser(connection.getUserName(), null, issuedBy, true);
                if (SteamUtils.isSteamModeEnabled()) {
                    String steamID = SteamUtils.convertSteamIDToString(connection.getSteamId());
                    BanSystem.BanUserBySteamID(steamID, null, issuedBy, true);
                }
            } catch (SQLException var4) {
                DebugType.Multiplayer.printException(var4, "User ban error", LogSeverity.Error);
            }

            GameServer.kick(connection, "UI_Policy_Ban", "UI_ValidationFailed");
            connection.forceDisconnect(issuedBy);
        }
    }

    public static class Policy {
        public static final int Ban = 1;
        public static final int Kick = 2;
        public static final int Log = 3;
        public static final int Disabled = 4;

        public static String name(int value) {
            return switch (value) {
                case 1 -> "Ban";
                case 2 -> "Kick";
                case 3 -> "Log";
                default -> "Unknown";
            };
        }
    }
}
