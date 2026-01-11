// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.commands;

import java.lang.annotation.Annotation;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zombie.characters.Role;
import zombie.commands.serverCommands.AddAllToWhiteListCommand;
import zombie.commands.serverCommands.AddItemCommand;
import zombie.commands.serverCommands.AddKeyCommand;
import zombie.commands.serverCommands.AddUserCommand;
import zombie.commands.serverCommands.AddUserToSafehouseCommand;
import zombie.commands.serverCommands.AddUserToWhiteListCommand;
import zombie.commands.serverCommands.AddVehicleCommand;
import zombie.commands.serverCommands.AddXPCommand;
import zombie.commands.serverCommands.AlarmCommand;
import zombie.commands.serverCommands.BanIPCommand;
import zombie.commands.serverCommands.BanSteamIDCommand;
import zombie.commands.serverCommands.BanUserCommand;
import zombie.commands.serverCommands.ChangeOptionCommand;
import zombie.commands.serverCommands.CheckModsNeedUpdate;
import zombie.commands.serverCommands.ChopperCommand;
import zombie.commands.serverCommands.ClearCommand;
import zombie.commands.serverCommands.ConnectionsCommand;
import zombie.commands.serverCommands.CreateHorde2Command;
import zombie.commands.serverCommands.CreateHordeCommand;
import zombie.commands.serverCommands.DebugPlayerCommand;
import zombie.commands.serverCommands.GodModeCommand;
import zombie.commands.serverCommands.GodModePlayerCommand;
import zombie.commands.serverCommands.GrantAdminCommand;
import zombie.commands.serverCommands.GunShotCommand;
import zombie.commands.serverCommands.HelpCommand;
import zombie.commands.serverCommands.InvisibleCommand;
import zombie.commands.serverCommands.InvisiblePlayerCommand;
import zombie.commands.serverCommands.KickUserCommand;
import zombie.commands.serverCommands.KickUserFromSafehouseCommand;
import zombie.commands.serverCommands.LightningCommand;
import zombie.commands.serverCommands.ListCommand;
import zombie.commands.serverCommands.LogCommand;
import zombie.commands.serverCommands.NoClipCommand;
import zombie.commands.serverCommands.PlayersCommand;
import zombie.commands.serverCommands.QuitCommand;
import zombie.commands.serverCommands.ReleaseSafehouseCommand;
import zombie.commands.serverCommands.ReloadAllLuaCommand;
import zombie.commands.serverCommands.ReloadLuaCommand;
import zombie.commands.serverCommands.ReloadOptionsCommand;
import zombie.commands.serverCommands.RemoveAdminCommand;
import zombie.commands.serverCommands.RemoveCommand;
import zombie.commands.serverCommands.RemoveItemCommand;
import zombie.commands.serverCommands.RemoveUserFromWhiteList;
import zombie.commands.serverCommands.RemoveZombiesCommand;
import zombie.commands.serverCommands.SaveCommand;
import zombie.commands.serverCommands.ServerMessageCommand;
import zombie.commands.serverCommands.SetAccessLevelCommand;
import zombie.commands.serverCommands.SetPasswordCommand;
import zombie.commands.serverCommands.SetTimeSpeedCommand;
import zombie.commands.serverCommands.ShowOptionsCommand;
import zombie.commands.serverCommands.StartRainCommand;
import zombie.commands.serverCommands.StartStormCommand;
import zombie.commands.serverCommands.StatisticsCommand;
import zombie.commands.serverCommands.StopRainCommand;
import zombie.commands.serverCommands.StopWeatherCommand;
import zombie.commands.serverCommands.TeleportCommand;
import zombie.commands.serverCommands.TeleportPlayerCommand;
import zombie.commands.serverCommands.TeleportToCommand;
import zombie.commands.serverCommands.ThunderCommand;
import zombie.commands.serverCommands.UnbanIPCommand;
import zombie.commands.serverCommands.UnbanSteamIDCommand;
import zombie.commands.serverCommands.UnbanUserCommand;
import zombie.commands.serverCommands.VoiceBanCommand;
import zombie.commands.serverCommands.WorldGeneratorCommand;
import zombie.core.Translator;
import zombie.core.raknet.UdpConnection;

public abstract class CommandBase {
    private final Role playerRole;
    private final String username;
    private final String command;
    private String[] commandArgs;
    private boolean parsingSuccessful;
    private boolean parsed;
    private String message = "";
    protected final UdpConnection connection;
    protected String argsName = "default args name. Nothing match";
    protected static final String defaultArgsName = "default args name. Nothing match";
    protected final String description;
    private static final Class<?>[] childrenClasses = new Class[]{
        SaveCommand.class,
        ServerMessageCommand.class,
        ConnectionsCommand.class,
        AddUserCommand.class,
        GrantAdminCommand.class,
        RemoveAdminCommand.class,
        DebugPlayerCommand.class,
        QuitCommand.class,
        AlarmCommand.class,
        ChopperCommand.class,
        AddAllToWhiteListCommand.class,
        KickUserCommand.class,
        TeleportCommand.class,
        TeleportPlayerCommand.class,
        TeleportToCommand.class,
        ReleaseSafehouseCommand.class,
        StartRainCommand.class,
        StopRainCommand.class,
        ThunderCommand.class,
        GunShotCommand.class,
        ReloadOptionsCommand.class,
        BanUserCommand.class,
        BanSteamIDCommand.class,
        BanIPCommand.class,
        UnbanUserCommand.class,
        UnbanSteamIDCommand.class,
        UnbanIPCommand.class,
        AddUserToWhiteListCommand.class,
        AddUserToSafehouseCommand.class,
        KickUserFromSafehouseCommand.class,
        RemoveUserFromWhiteList.class,
        ChangeOptionCommand.class,
        ShowOptionsCommand.class,
        GodModeCommand.class,
        GodModePlayerCommand.class,
        VoiceBanCommand.class,
        NoClipCommand.class,
        InvisibleCommand.class,
        InvisiblePlayerCommand.class,
        HelpCommand.class,
        ClearCommand.class,
        PlayersCommand.class,
        AddItemCommand.class,
        RemoveItemCommand.class,
        AddXPCommand.class,
        AddVehicleCommand.class,
        CreateHordeCommand.class,
        CreateHorde2Command.class,
        ReloadLuaCommand.class,
        ReloadAllLuaCommand.class,
        RemoveZombiesCommand.class,
        RemoveCommand.class,
        ListCommand.class,
        SetAccessLevelCommand.class,
        LogCommand.class,
        LightningCommand.class,
        StopWeatherCommand.class,
        StartStormCommand.class,
        CheckModsNeedUpdate.class,
        AddKeyCommand.class,
        SetTimeSpeedCommand.class,
        SetPasswordCommand.class,
        StatisticsCommand.class,
        WorldGeneratorCommand.class
    };

    public static Class<?>[] getSubClasses() {
        return childrenClasses;
    }

    public static Class<?> findCommandCls(String command) {
        for (Class<?> cls : childrenClasses) {
            if (!isDisabled(cls)) {
                CommandName[] nameAnnotations = cls.getAnnotationsByType(CommandName.class);

                for (CommandName nameAnnotation : nameAnnotations) {
                    Pattern p = Pattern.compile("^" + nameAnnotation.name() + "\\b", 2);
                    if (p.matcher(command).find()) {
                        return cls;
                    }
                }
            }
        }

        return null;
    }

    public static String getHelp(Class<?> cls) {
        CommandHelp annotation = getAnnotation(CommandHelp.class, cls);
        if (annotation == null) {
            return null;
        } else if (annotation.shouldTranslated()) {
            String textID = annotation.helpText();
            return Translator.getText(textID);
        } else {
            return annotation.helpText();
        }
    }

    public static String getCommandName(Class<?> cls) {
        Annotation[] nameAnnotations = cls.getAnnotationsByType(CommandName.class);
        return ((CommandName)nameAnnotations[0]).name();
    }

    public static boolean isDisabled(Class<?> cls) {
        DisabledCommand annotation = getAnnotation(DisabledCommand.class, cls);
        return annotation != null;
    }

    protected CommandBase(String username, Role userRole, String command, UdpConnection connection) {
        this.username = username;
        this.command = command;
        this.connection = connection;
        this.playerRole = userRole;
        ArrayList<String> args = new ArrayList<>();
        Matcher m1 = Pattern.compile("([^\"]\\S*|\".*?\")\\s*").matcher(command);

        while (m1.find()) {
            args.add(m1.group(1).replace("\"", ""));
        }

        this.commandArgs = new String[args.size() - 1];

        for (int i = 1; i < args.size(); i++) {
            this.commandArgs[i - 1] = args.get(i);
        }

        this.description = "cmd=\""
            + command
            + "\" user=\""
            + username
            + "\" role=\""
            + this.playerRole.getName()
            + "\" "
            + (connection != null ? "guid=\"" + connection.getConnectedGUID() + "\" id=\"" + connection.idStr : "unknown connection")
            + "\"";
    }

    public String Execute() throws SQLException {
        return this.canBeExecuted() ? this.Command() : this.message;
    }

    public boolean canBeExecuted() {
        if (this.parsed) {
            return this.parsingSuccessful;
        } else if (!this.PlayerSatisfyRequiredRights()) {
            this.message = this.playerHasNoRightError();
            return false;
        } else {
            this.parsingSuccessful = this.parseCommand();
            return this.parsingSuccessful;
        }
    }

    public boolean isCommandComeFromServerConsole() {
        return this.connection == null;
    }

    protected RequiredCapability getRequiredCapability() {
        return this.getClass().getAnnotation(RequiredCapability.class);
    }

    protected CommandArgs[] getCommandArgVariants() {
        Class<?> cls = this.getClass();
        return cls.getAnnotationsByType(CommandArgs.class);
    }

    public boolean hasHelp() {
        Class<?> cls = this.getClass();
        CommandHelp annotation = cls.getAnnotation(CommandHelp.class);
        return annotation != null;
    }

    protected String getHelp() {
        Class<?> cls = this.getClass();
        return getHelp(cls);
    }

    public String getCommandArg(Integer argNumber) {
        return this.commandArgs != null && argNumber >= 0 && argNumber < this.commandArgs.length ? this.commandArgs[argNumber] : null;
    }

    public boolean hasOptionalArg(Integer argNumber) {
        return this.commandArgs != null && argNumber >= 0 && argNumber < this.commandArgs.length;
    }

    public int getCommandArgsCount() {
        return this.commandArgs.length;
    }

    protected abstract String Command() throws SQLException;

    public boolean parseCommand() {
        CommandArgs[] commandArgsAnnotations = this.getCommandArgVariants();
        if (commandArgsAnnotations.length == 1 && commandArgsAnnotations[0].varArgs()) {
            this.parsed = true;
            return true;
        } else {
            boolean valid = commandArgsAnnotations.length != 0 && this.commandArgs.length != 0
                || commandArgsAnnotations.length == 0 && this.commandArgs.length == 0;
            ArrayList<String> capturedArgs = new ArrayList<>();

            for (CommandArgs annotationArgs : commandArgsAnnotations) {
                capturedArgs.clear();
                this.message = "";
                int lastParsedArg = 0;
                valid = true;

                for (int i = 0; i < annotationArgs.required().length; i++) {
                    String expectedArgType = annotationArgs.required()[i];
                    if (lastParsedArg == this.commandArgs.length) {
                        valid = false;
                        break;
                    }

                    Matcher matcher = Pattern.compile(expectedArgType).matcher(this.commandArgs[lastParsedArg]);
                    if (!matcher.matches()) {
                        valid = false;
                        break;
                    }

                    for (int j = 0; j < matcher.groupCount(); j++) {
                        capturedArgs.add(matcher.group(j + 1));
                    }

                    lastParsedArg++;
                }

                if (valid) {
                    if (lastParsedArg == this.commandArgs.length) {
                        this.argsName = annotationArgs.argName();
                        break;
                    }

                    if (!annotationArgs.optional().equals("no value")) {
                        Matcher matcher = Pattern.compile(annotationArgs.optional()).matcher(this.commandArgs[lastParsedArg]);
                        if (matcher.matches()) {
                            for (int j = 0; j < matcher.groupCount(); j++) {
                                capturedArgs.add(matcher.group(j + 1));
                            }
                        } else {
                            valid = false;
                        }
                    } else if (lastParsedArg < this.commandArgs.length) {
                        valid = false;
                    }

                    if (valid) {
                        this.argsName = annotationArgs.argName();
                        break;
                    }
                }
            }

            if (valid) {
                this.commandArgs = new String[capturedArgs.size()];
                this.commandArgs = capturedArgs.toArray(this.commandArgs);
            } else {
                this.message = this.invalidCommand();
                this.commandArgs = new String[0];
            }

            this.parsed = true;
            return valid;
        }
    }

    protected Role getRole() {
        return this.playerRole;
    }

    protected String getExecutorUsername() {
        return this.username;
    }

    protected String getCommand() {
        return this.command;
    }

    protected static <T extends Annotation> T getAnnotation(Class<T> annotation, Class<?> src) {
        return src.getAnnotation(annotation);
    }

    public boolean isParsingSuccessful() {
        if (!this.parsed) {
            this.parsingSuccessful = this.parseCommand();
        }

        return this.parsingSuccessful;
    }

    private boolean PlayerSatisfyRequiredRights() {
        RequiredCapability requiredRight = this.getRequiredCapability();
        return this.playerRole.hasCapability(requiredRight.requiredCapability());
    }

    private String invalidCommand() {
        return this.hasHelp() ? this.getHelp() : Translator.getText("UI_command_arg_parse_failed", this.command);
    }

    private String playerHasNoRightError() {
        return Translator.getText("UI_has_no_right_to_execute_command", this.username, this.command);
    }
}
