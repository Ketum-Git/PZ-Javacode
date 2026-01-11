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
        String result = "";
        if (type != null && severity != null) {
            DebugLog.enableLog(type, severity);
            if (DebugType.Network == type) {
                ZNet.SetLogLevel(severity);
            }

            result = String.format("\"%s\" log level is \"%s\"", type.name().toLowerCase(), severity.name().toLowerCase());
        } else if (DebugType.Packet == type) {
            PacketTypes.PacketType packetType = Arrays.stream(PacketTypes.PacketType.values())
                .filter(packet -> packet.name().equalsIgnoreCase(arg2))
                .findFirst()
                .orElse(null);
            if (packetType != null) {
                packetType.setLogEnabled(!packetType.isLogEnabled());
                result = String.format("\"%s\" log is \"%s\"", packetType.name(), packetType.isLogEnabled());
            } else {
                result = Translator.getText("UI_ServerOptionDesc_SetLogLevel", DebugType.Packet.name().toLowerCase(), "\"packet type\"");
            }
        } else {
            result = Translator.getText(
                "UI_ServerOptionDesc_SetLogLevel",
                type == null ? "\"type\"" : type.name().toLowerCase(),
                severity == null ? "\"severity\"" : severity.name().toLowerCase()
            );
        }

        return result;
    }

    @Override
    protected String Command() {
        return process(this.getCommandArg(0), this.getCommandArg(1));
    }
}
