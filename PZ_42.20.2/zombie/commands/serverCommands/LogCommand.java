// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands.serverCommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import zombie.characters.Capability;
import zombie.characters.Role;
import zombie.commands.CommandArgs;
import zombie.commands.CommandBase;
import zombie.commands.CommandHelp;
import zombie.commands.CommandName;
import zombie.commands.RequiredCapability;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.raknet.UdpConnection;
import zombie.core.znet.ZNet;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.network.PacketTypes;

@CommandName(name = "log")
@CommandArgs(required = {"(.+)", "(.+)"})
@CommandHelp(helpText = "UI_ServerOptionDesc_SetLogLevel")
@RequiredCapability(requiredCapability = Capability.DebugConsole)
public class LogCommand extends CommandBase {
    private static final String CMD_ALL = "all";
    private static final String CMD_SAVE = "save";

    public LogCommand(String username, Role userRole, String command, UdpConnection connection) {
        super(username, userRole, command, connection);
    }

    public static DebugType getDebugType(String debugType) {
        List<DebugType> types = new ArrayList<>();

        for (DebugType type : DebugType.values()) {
            if (type.name().equalsIgnoreCase(debugType)) {
                return type;
            }

            if (type.name().toLowerCase().startsWith(debugType.toLowerCase())) {
                types.add(type);
            }
        }

        return types.size() == 1 ? types.get(0) : null;
    }

    public static LogSeverity getLogSeverity(String logSeverity) {
        List<LogSeverity> severities = new ArrayList<>();

        for (LogSeverity severity : LogSeverity.values()) {
            if (severity.name().toLowerCase().startsWith(logSeverity.toLowerCase())) {
                severities.add(severity);
            }
        }

        return severities.size() == 1 ? severities.get(0) : null;
    }

    public static String process(String arg1, String arg2) {
        DebugType type = getDebugType(arg1);
        LogSeverity severity = getLogSeverity(arg2);
        if (type != null && severity != null) {
            type.setLogSeverity(severity);
            if (DebugType.ZNet == type) {
                ZNet.setLogLevel(severity);
            }

            DebugLog.updateSelectedProfile(type, severity);
            return String.format("Debug type \"%s\" log level is set to \"%s\"", type.name().toLowerCase(), severity.name().toLowerCase());
        } else if ("all".equals(arg1) && severity != null) {
            for (DebugType debugType : DebugType.values()) {
                debugType.setLogSeverity(severity);
            }

            DebugLog.updateSelectedProfileAll(severity);
            return String.format("All debug type log levels are set to \"%s\"", severity.name().toLowerCase());
        } else if ("save".equals(arg1) && "all".equals(arg2)) {
            try {
                DebugLog.writeConfigFile();
                return "DebugLog save succeeded";
            } catch (Exception var8) {
                ExceptionLogger.logException(var8);
                return "DebugLog save failed";
            }
        } else {
            if (DebugType.Packet == type) {
                if ("all".equals(arg2)) {
                    for (PacketTypes.PacketType packetType : PacketTypes.PacketType.values()) {
                        packetType.setLogEnabled(true);
                    }

                    return "All packet types logging is enabled";
                }

                if ("none".equals(arg2)) {
                    for (PacketTypes.PacketType packetType : PacketTypes.PacketType.values()) {
                        packetType.setLogEnabled(false);
                    }

                    return "All packet types logging is disabled";
                }

                PacketTypes.PacketType packetType = Arrays.stream(PacketTypes.PacketType.values())
                    .filter(packet -> packet.name().equalsIgnoreCase(arg2))
                    .findFirst()
                    .orElse(null);
                if (packetType != null) {
                    packetType.setLogEnabled(!packetType.isLogEnabled());
                    return String.format("Packet type \"%s\" logging is \"%s\"", packetType.name(), packetType.isLogEnabled() ? "enabled" : "disabled");
                }
            }

            return Translator.getText(
                "UI_ServerOptionDesc_SetLogLevel",
                type == null ? "\"packet type\"" : type.name().toLowerCase(),
                severity == null ? "\"log severity\"" : severity.name().toLowerCase()
            );
        }
    }

    @Override
    protected String Command() {
        return process(this.getCommandArg(0), this.getCommandArg(1));
    }
}
