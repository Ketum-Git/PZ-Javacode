// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import zombie.DebugFileWatcher;
import zombie.GameTime;
import zombie.PredicatedFileWatcher;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.Core;
import zombie.core.logger.LoggerManager;
import zombie.core.logger.ZLogger;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptType;
import zombie.ui.UIDebugConsole;
import zombie.util.StringUtils;

/**
 * Created by LEMMYPC on 31/12/13.
 */
@UsedFromLua
public final class DebugLog {
    private static final DebugLogStream[] s_streams = new DebugLogStream[DebugType.values().length];
    private static boolean initialized;
    public static boolean printServerTime;
    private static final DebugLog.OutputStreamWrapper s_stdout = new DebugLog.OutputStreamWrapper(System.out);
    private static final DebugLog.OutputStreamWrapper s_stderr = new DebugLog.OutputStreamWrapper(System.err);
    private static final PrintStream s_originalOut = new PrintStream(s_stdout, true);
    private static final PrintStream s_originalErr = new PrintStream(s_stderr, true);
    private static final PrintStream GeneralErr = new DebugLogStream(
        s_originalErr, s_originalErr, s_originalErr, new GeneralErrorDebugLogFormatter(), LogSeverity.All
    );
    private static ZLogger logFileLogger;
    private static PredicatedFileWatcher debugCfgFileWatcher;
    private static String debugCfgFileWatcherPath;
    public static final DebugLogStream ActionSystem = createDebugLogStream(DebugType.ActionSystem);
    public static final DebugLogStream Animation = createDebugLogStream(DebugType.Animation);
    public static final DebugLogStream AnimationDetailed = createDebugLogStream(DebugType.AnimationDetailed);
    public static final DebugLogStream Asset = createDebugLogStream(DebugType.Asset);
    public static final DebugLogStream Clothing = createDebugLogStream(DebugType.Clothing);
    public static final DebugLogStream Combat = createDebugLogStream(DebugType.Combat);
    public static final DebugLogStream Damage = createDebugLogStream(DebugType.Damage);
    public static final DebugLogStream Death = createDebugLogStream(DebugType.Death);
    public static final DebugLogStream Discord = createDebugLogStream(DebugType.Discord);
    public static final DebugLogStream Entity = createDebugLogStream(DebugType.Entity);
    public static final DebugLogStream FileIO = createDebugLogStream(DebugType.FileIO);
    public static final DebugLogStream Fireplace = createDebugLogStream(DebugType.Fireplace);
    public static final DebugLogStream General = createDebugLogStream(DebugType.General, LogSeverity.General);
    public static final DebugLogStream DetailedInfo = createDebugLogStream(DebugType.DetailedInfo);
    public static final DebugLogStream Input = createDebugLogStream(DebugType.Input);
    public static final DebugLogStream IsoRegion = createDebugLogStream(DebugType.IsoRegion);
    public static final DebugLogStream Lua = createDebugLogStream(DebugType.Lua, LogSeverity.General);
    public static final DebugLogStream MapLoading = createDebugLogStream(DebugType.MapLoading);
    public static final DebugLogStream Mod = createDebugLogStream(DebugType.Mod, LogSeverity.General);
    public static final DebugLogStream Multiplayer = createDebugLogStream(DebugType.Multiplayer, LogSeverity.General);
    public static final DebugLogStream Network = createDebugLogStream(DebugType.Network, LogSeverity.Error);
    public static final DebugLogStream NetworkFileDebug = createDebugLogStream(DebugType.NetworkFileDebug);
    public static final DebugLogStream Packet = createDebugLogStream(DebugType.Packet);
    public static final DebugLogStream Objects = createDebugLogStream(DebugType.Objects);
    public static final DebugLogStream Radio = createDebugLogStream(DebugType.Radio);
    public static final DebugLogStream Recipe = createDebugLogStream(DebugType.Recipe);
    public static final DebugLogStream Script = createDebugLogStream(DebugType.Script);
    public static final DebugLogStream Shader = createDebugLogStream(DebugType.Shader);
    public static final DebugLogStream Sound = createDebugLogStream(DebugType.Sound);
    public static final DebugLogStream Sprite = createDebugLogStream(DebugType.Sprite);
    public static final DebugLogStream Statistic = createDebugLogStream(DebugType.Statistic);
    public static final DebugLogStream Vehicle = createDebugLogStream(DebugType.Vehicle);
    public static final DebugLogStream Voice = createDebugLogStream(DebugType.Voice);
    public static final DebugLogStream Zombie = createDebugLogStream(DebugType.Zombie);
    public static final DebugLogStream Animal = createDebugLogStream(DebugType.Animal);
    public static final DebugLogStream ItemPicker = createDebugLogStream(DebugType.ItemPicker);
    public static final DebugLogStream CraftLogic = createDebugLogStream(DebugType.CraftLogic);
    public static final DebugLogStream Action = createDebugLogStream(DebugType.Action);
    public static final DebugLogStream Physics = createDebugLogStream(DebugType.Physics);
    public static final DebugLogStream Zone = createDebugLogStream(DebugType.Zone);
    public static final DebugLogStream WorldGen = createDebugLogStream(DebugType.WorldGen);
    public static final DebugLogStream Lightning = createDebugLogStream(DebugType.Lightning);
    public static final DebugLogStream Grapple = createDebugLogStream(DebugType.Grapple);
    public static final DebugLogStream Foraging = createDebugLogStream(DebugType.Foraging);
    public static final DebugLogStream Saving = createDebugLogStream(DebugType.Saving);
    public static final DebugLogStream Energy = createDebugLogStream(DebugType.Energy);
    public static final DebugLogStream Fluid = createDebugLogStream(DebugType.Fluid);
    public static final DebugLogStream Translation = createDebugLogStream(DebugType.Translation);
    public static final DebugLogStream Moveable = createDebugLogStream(DebugType.Moveable);
    public static final DebugLogStream Basement = createDebugLogStream(DebugType.Basement);
    public static final DebugLogStream Xml = createDebugLogStream(DebugType.Xml);
    public static final DebugLogStream ImGui = createDebugLogStream(DebugType.ImGui);
    private static boolean logTraceFileLocationEnabled;
    public static final int VERSION1 = 1;
    public static final int VERSION2 = 2;
    public static final int VERSION = 4;

    public static void setDefaultLogSeverity() {
        LogSeverity logSeverity = getDefaultLogSeverity();

        for (DebugType debugType : DebugType.values()) {
            if (logSeverity.ordinal() < debugType.getLogStream().getLogSeverity().ordinal()) {
                enableLog(debugType, logSeverity);
            }
        }
    }

    private static LogSeverity getDefaultLogSeverity() {
        if (Core.debug) {
            return LogSeverity.General;
        } else {
            return GameServer.server ? LogSeverity.Warning : LogSeverity.Off;
        }
    }

    private static DebugLogStream createDebugLogStream(DebugType in_debugType, LogSeverity in_logSeverity) {
        DebugLogStream out_newStream = new DebugLogStream(s_originalOut, s_originalOut, s_originalErr, new GenericDebugLogFormatter(in_debugType));
        if (s_streams[in_debugType.ordinal()] != null) {
            throw new IllegalArgumentException(
                String.format("DebugType.%s is already registered. Existing logger: %s", in_debugType.name(), s_streams[in_debugType.ordinal()].toString())
            );
        } else {
            s_streams[in_debugType.ordinal()] = out_newStream;
            out_newStream.setLogSeverity(in_logSeverity);
            return out_newStream;
        }
    }

    private static DebugLogStream createDebugLogStream(DebugType in_debugType) {
        return createDebugLogStream(in_debugType, LogSeverity.Off);
    }

    public static DebugLogStream getOrCreateDebugLogStream(DebugType in_type) {
        return s_streams[in_type.ordinal()] != null ? s_streams[in_type.ordinal()] : createDebugLogStream(in_type);
    }

    public static void printLogLevels() {
        if (!GameServer.server) {
            DetailedInfo.trace("You can setup the log levels in the " + getConfigFileName() + " file");
        }

        General.println("Logs configuration:");
        LogSeverity defaultLogSeverity = getDefaultLogSeverity();

        for (DebugType type : DebugType.values()) {
            DebugLogStream logStream = getOrCreateDebugLogStream(type);
            if (logStream.getLogSeverity() != defaultLogSeverity) {
                General.println("%12s: %s", type.name(), logStream.getLogSeverity().name());
            }
        }

        General.println("%12s: %s", "Default", defaultLogSeverity);
    }

    public static void enableLog(DebugType type, LogSeverity severity) {
        setLogSeverity(type, severity);
    }

    public static LogSeverity getLogLevel(DebugType type) {
        return getLogSeverity(type);
    }

    public static LogSeverity getLogSeverity(DebugType in_type) {
        return getOrCreateDebugLogStream(in_type).getLogSeverity();
    }

    public static void setLogSeverity(DebugType in_type, LogSeverity in_logSeverity) {
        getOrCreateDebugLogStream(in_type).setLogSeverity(in_logSeverity);
    }

    public static boolean isEnabled(DebugType type) {
        return getOrCreateDebugLogStream(type).isEnabled();
    }

    public static boolean isLogEnabled(DebugType in_type, LogSeverity in_logSeverity) {
        return getOrCreateDebugLogStream(in_type).isLogEnabled(in_logSeverity);
    }

    public static String formatString(DebugType type, LogSeverity logSeverity, Object affix, boolean in_allowRepeat, String formatNoParams) {
        return isLogEnabled(type, logSeverity) ? formatStringVarArgs(type, logSeverity, affix, in_allowRepeat, "%s", formatNoParams) : null;
    }

    public static String formatString(DebugType type, LogSeverity logSeverity, Object affix, boolean in_allowRepeat, String format, Object... params) {
        return isLogEnabled(type, logSeverity) ? formatStringVarArgs(type, logSeverity, affix, in_allowRepeat, format, params) : null;
    }

    public static String formatStringVarArgs(DebugType type, LogSeverity logSeverity, Object affix, boolean in_allowRepeat, String format, Object... params) {
        if (!isLogEnabled(type, logSeverity)) {
            return null;
        } else {
            String ms = generateCurrentTimeMillisStr();
            int frameNo = IsoWorld.instance.getFrameNo();
            String typeStr = StringUtils.leftJustify(type.toString(), 12);
            String formattedOutputStr = String.format(format, params);
            String affixedOutputStr = affix + formattedOutputStr;
            if (!DebugLog.RepeatWatcher.check(type, logSeverity, affixedOutputStr, in_allowRepeat)) {
                return null;
            } else {
                String formattedLine = logSeverity.logPrefix + typeStr + " f:" + frameNo + ", t:" + ms + "> " + affixedOutputStr;
                echoToLogFile(formattedLine);
                return formattedLine;
            }
        }
    }

    private static String generateCurrentTimeMillisStr() {
        String ms = String.valueOf(System.currentTimeMillis());
        if (GameServer.server || GameClient.client || printServerTime) {
            ms = ms + ", st:" + NumberFormat.getNumberInstance().format(TimeUnit.NANOSECONDS.toMillis(GameTime.getServerTime()));
        }

        return ms;
    }

    private static void echoToLogFile(String formattedLine) {
        if (logFileLogger == null) {
            if (initialized) {
                return;
            }

            logFileLogger = new ZLogger(GameServer.server ? "DebugLog-server" : "DebugLog", false);
        }

        try {
            logFileLogger.writeUnsafe(formattedLine, null, false);
        } catch (Exception var2) {
            s_originalErr.println("Exception thrown writing to log file.");
            s_originalErr.println(var2);
            var2.printStackTrace(s_originalErr);
        }
    }

    public static void log(DebugType type, String str) {
        type.println(str);
    }

    public static void setLogEnabled(DebugType type, boolean bEnabled) {
        DebugLogStream logStream = getOrCreateDebugLogStream(type);
        if (logStream.isEnabled() != bEnabled) {
            logStream.setLogSeverity(bEnabled ? getDefaultLogSeverity() : LogSeverity.Off);
        }
    }

    public static void log(String str) {
        log(DebugType.General, str);
    }

    public static ArrayList<DebugType> getDebugTypes() {
        ArrayList<DebugType> debugTypes = new ArrayList<>(Arrays.asList(DebugType.values()));
        debugTypes.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()));
        return debugTypes;
    }

    public static void save() {
        LogSeverity[] logSeverityValues = LogSeverity.values();
        String[] logSeverityNames = new String[logSeverityValues.length];

        for (int i = 0; i < logSeverityValues.length; i++) {
            logSeverityNames[i] = logSeverityValues[i].name();
        }

        ArrayList<ConfigOption> options = new ArrayList<>();

        for (DebugType debugType : DebugType.values()) {
            StringConfigOption option = new StringConfigOption(debugType.name(), LogSeverity.Off.name(), logSeverityNames);
            option.setValue(getLogSeverity(debugType).name());
            options.add(option);
        }

        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "debuglog.ini";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 4, options);
    }

    private static String getConfigFileName() {
        return ZomboidFileSystem.instance.getCacheDir() + File.separator + "debuglog.ini";
    }

    public static void load() {
        String fileName = getConfigFileName();
        ConfigFile configFile = new ConfigFile();
        File file = new File(fileName);
        if (!file.exists()) {
            setDefaultLogSeverity();
            save();
        }

        if (configFile.read(fileName)) {
            if (configFile.getVersion() != 4) {
                setDefaultLogSeverity();
                save();
            } else {
                for (int i = 0; i < configFile.getOptions().size(); i++) {
                    ConfigOption configOption = configFile.getOptions().get(i);

                    try {
                        DebugType debugType = DebugType.valueOf(configOption.getName());
                        if (configFile.getVersion() == 1) {
                            setLogEnabled(debugType, StringUtils.tryParseBoolean(configOption.getValueAsString()));
                        } else {
                            LogSeverity logSeverity = LogSeverity.valueOf(configOption.getValueAsString());
                            setLogSeverity(debugType, logSeverity);
                        }
                    } catch (Exception var7) {
                    }
                }
            }
        }
    }

    public static boolean isLogTraceFileLocationEnabled() {
        return logTraceFileLocationEnabled;
    }

    public static void setStdOut(OutputStream out) {
        s_stdout.setStream(out);
    }

    public static void setStdErr(OutputStream out) {
        s_stderr.setStream(out);
    }

    public static void init() {
        if (!initialized) {
            initialized = true;
            setStdOut(System.out);
            setStdErr(System.err);
            System.setOut(General);
            System.setErr(GeneralErr);
            if (!GameServer.server) {
                load();
            }

            logFileLogger = LoggerManager.getLogger(GameServer.server ? "DebugLog-server" : "DebugLog");
        }
    }

    public static void loadDebugConfig(String filepath) {
        if (!GameServer.server) {
            try {
                if (filepath == null) {
                    filepath = ZomboidFileSystem.instance.getCacheDir() + File.separator + "debuglog.cfg";
                    File file = new File(filepath);
                    if (!file.exists() || !file.isFile()) {
                        return;
                    }
                }

                log("Attempting to read debug config...");
                File file = new File(filepath);
                if (!file.exists() || !file.isFile()) {
                    log("Attempting relative path...");
                    File p = new File("");
                    Path path = Path.of(p.toURI()).getParent();
                    file = new File(path + File.separator + filepath);
                }

                DetailedInfo.trace("file = " + file.getAbsolutePath());
                if (!file.exists() || !file.isFile()) {
                    log("Could not find debug config.");
                    return;
                }

                String selectedConfig = null;
                HashMap<String, ArrayList<String>> configs = new HashMap<>();
                HashMap<String, String> aliases = new HashMap<>();
                ArrayList<String> commands = null;
                boolean opened = false;
                BufferedReader br = new BufferedReader(new FileReader(file));

                try {
                    String lastLine = null;
                    String line = null;

                    String l;
                    while ((l = br.readLine()) != null) {
                        lastLine = line;
                        line = l.trim();
                        if (!line.startsWith("//") && !line.startsWith("#") && !StringUtils.isNullOrWhitespace(line)) {
                            if (line.startsWith("=")) {
                                selectedConfig = line.substring(1).trim();
                            } else if (line.startsWith("$")) {
                                try {
                                    String s = line.substring(1).trim();
                                    int i = s.indexOf(61);
                                    String alias = s.substring(0, i).trim();
                                    String command = s.substring(i + 1).trim();
                                    aliases.put(alias, command);
                                } catch (Exception var16) {
                                    var16.printStackTrace();
                                }
                            } else if (!opened && line.startsWith("{") && lastLine != null) {
                                opened = true;
                                commands = new ArrayList<>();
                                configs.put(lastLine, commands);
                            } else if (opened) {
                                if (line.startsWith("}")) {
                                    opened = false;
                                } else {
                                    commands.add(line);
                                }
                            }
                        }
                    }
                } catch (Throwable var17) {
                    try {
                        br.close();
                    } catch (Throwable var15) {
                        var17.addSuppressed(var15);
                    }

                    throw var17;
                }

                br.close();
                if (selectedConfig != null) {
                    if (selectedConfig.startsWith("$")) {
                        log("Selected debug alias = '" + selectedConfig + "'");
                        selectedConfig = aliases.get(selectedConfig.substring(1).trim());
                    } else {
                        log("Selected debug profile = '" + selectedConfig + "'");
                    }

                    String[] ss = selectedConfig.split("\\+");

                    for (String elem : ss) {
                        String profile = elem.trim();
                        if (configs.containsKey(profile)) {
                            log("Debug.cfg loading profile '" + profile + "'");

                            for (String s : configs.get(profile)) {
                                if (s.startsWith("+")) {
                                    readConfigCommand(s.substring(1), true);
                                } else if (s.startsWith("-")) {
                                    readConfigCommand(s.substring(1), false);
                                } else {
                                    log("unknown command: '" + s + "'");
                                }
                            }
                        } else {
                            log("Debug.cfg profile note found: '" + profile + "'");
                        }
                    }
                }

                startWatchingDebugCfgFile(file);
            } catch (Exception var18) {
                var18.printStackTrace();
            }
        }
    }

    private static void startWatchingDebugCfgFile(File file) {
        if (debugCfgFileWatcher == null || !debugCfgFileWatcherPath.equalsIgnoreCase(file.getPath())) {
            if (debugCfgFileWatcher != null) {
                stopWatchingDebugCfgFile();
            }

            String cfgFileDir = file.getParent();
            DebugFileWatcher.instance.addDirectory(cfgFileDir);
            debugCfgFileWatcherPath = file.getPath();
            debugCfgFileWatcher = new PredicatedFileWatcher(debugCfgFileWatcherPath, DebugLog::isDebugCfgPath, DebugLog::onDebugCfgFileChanged);
            DebugFileWatcher.instance.add(debugCfgFileWatcher);
        }
    }

    private static void stopWatchingDebugCfgFile() {
        DebugFileWatcher.instance.remove(debugCfgFileWatcher);
        debugCfgFileWatcher = null;
        debugCfgFileWatcherPath = null;
    }

    private static void onDebugCfgFileChanged(String in_path) {
        loadDebugConfig(debugCfgFileWatcherPath);
        printLogLevels();
    }

    private static boolean isDebugCfgPath(String in_path) {
        return StringUtils.equalsIgnoreCase(debugCfgFileWatcherPath, in_path);
    }

    private static void readConfigCommand(String s, boolean enable) {
        try {
            String logTypeStr = s;
            String logSeverityStr = null;
            if (StringUtils.containsWhitespace(s)) {
                String[] split = s.split("\\s+");
                logTypeStr = split[0].trim();
                logSeverityStr = split[1].trim();
            }

            LogSeverity logSeverity = LogSeverity.Debug;
            if (!StringUtils.isNullOrWhitespace(logSeverityStr)) {
                logSeverity = LogSeverity.valueOf(logSeverityStr);
            }

            if (logTypeStr.equalsIgnoreCase("LogTraceFileLocation")) {
                logTraceFileLocationEnabled = enable;
                return;
            }

            if (logTypeStr.equalsIgnoreCase("all")) {
                for (DebugType type : DebugType.values()) {
                    if (type != DebugType.General || enable) {
                        setLogSeverity(type, logSeverity);
                        setLogEnabled(type, enable);
                    }
                }

                return;
            }

            DebugType typex;
            if (logTypeStr.contains(".")) {
                String[] split = logTypeStr.split("\\.");
                typex = DebugType.valueOf(split[0]);
                ScriptType scriptType = ScriptType.valueOf(split[1]);
                ScriptManager.EnableDebug(scriptType, enable);
            } else {
                typex = DebugType.valueOf(logTypeStr);
            }

            setLogSeverity(typex, logSeverity);
            setLogEnabled(typex, enable);
        } catch (Exception var9) {
            General.printException(var9, "Exception thrown in readConfigCommand", LogSeverity.Error);
        }
    }

    public static void nativeLog(String in_logType, String in_logSeverity, String in_logTxt) {
        DebugType logType = StringUtils.tryParseEnum(DebugType.class, in_logType, DebugType.General);
        LogSeverity logSeverity = StringUtils.tryParseEnum(LogSeverity.class, in_logSeverity, LogSeverity.General);
        logType.routedWrite(1, logSeverity, in_logTxt);
    }

    private static final class OutputStreamWrapper extends FilterOutputStream {
        public OutputStreamWrapper(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.out.write(b, off, len);
            if (Core.debug && UIDebugConsole.instance != null && DebugOptions.instance.uiDebugConsoleDebugLog.getValue()) {
                UIDebugConsole.instance.addOutput(b, off, len);
            }
        }

        public void setStream(OutputStream out) {
            this.out = out;
        }
    }

    private static final class RepeatWatcher {
        private static final Object Lock = "RepeatWatcher_Lock";
        private static String lastLine;
        private static DebugType lastDebugType;
        private static LogSeverity lastLogSeverity;

        public static boolean check(DebugType in_type, LogSeverity in_logSeverity, String in_newLine, boolean in_allowRepeat) {
            synchronized (Lock) {
                if (in_allowRepeat) {
                    lastLine = null;
                    lastDebugType = null;
                    lastLogSeverity = null;
                    return true;
                } else if (lastLine == null) {
                    lastLine = in_newLine;
                    lastDebugType = in_type;
                    lastLogSeverity = in_logSeverity;
                    return true;
                } else if (lastDebugType == in_type && lastLogSeverity == in_logSeverity && lastLine.equals(in_newLine)) {
                    return false;
                } else {
                    lastLine = in_newLine;
                    lastDebugType = in_type;
                    lastLogSeverity = in_logSeverity;
                    return true;
                }
            }
        }
    }
}
