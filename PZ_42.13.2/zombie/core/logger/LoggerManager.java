// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.logger;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.debug.DebugLog;

public final class LoggerManager {
    private static boolean isInitialized;
    private static final HashMap<String, ZLogger> s_loggers = new HashMap<>();

    public static synchronized ZLogger getLogger(String loggerName) {
        if (!s_loggers.containsKey(loggerName)) {
            createLogger(loggerName, false);
        }

        return s_loggers.get(loggerName);
    }

    public static synchronized void init() {
        if (!isInitialized) {
            DebugLog.General.debugln("Initializing...");
            isInitialized = true;
            backupOldLogFiles();
        }
    }

    private static void backupOldLogFiles() {
        try {
            File logsDir = new File(getLogsDir());
            File[] logFileList = ZomboidFileSystem.listAllFiles(logsDir);
            if (logFileList.length == 0) {
                return;
            }

            Date lastModified = getLogFileLastModifiedTime(logFileList[0]);
            String backupDirName = "logs_" + ZomboidFileSystem.getDateStampString(lastModified);
            File backupDir = new File(getLogsDir() + File.separator + backupDirName);
            ZomboidFileSystem.ensureFolderExists(backupDir);

            for (int i = 0; i < logFileList.length; i++) {
                File fileToMove = logFileList[i];
                if (fileToMove.isFile()) {
                    fileToMove.renameTo(new File(backupDir.getAbsolutePath() + File.separator + fileToMove.getName()));
                    fileToMove.delete();
                }
            }
        } catch (Exception var5) {
            DebugLog.General.error("Exception thrown trying to initialize LoggerManager, trying to copy old log files.");
            DebugLog.General.error("Exception: ");
            DebugLog.General.error(var5);
            var5.printStackTrace();
        }
    }

    private static Date getLogFileLastModifiedTime(File in_logFile) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(in_logFile.lastModified());
        return calendar.getTime();
    }

    public static synchronized void createLogger(String loggerName, boolean useConsole) {
        init();
        s_loggers.put(loggerName, new ZLogger(loggerName, useConsole));
    }

    public static String getLogsDir() {
        String logsDirPath = ZomboidFileSystem.instance.getCacheDirSub("Logs");
        ZomboidFileSystem.ensureFolderExists(logsDirPath);
        File logsDir = new File(logsDirPath);
        return logsDir.getAbsolutePath();
    }

    public static String getPlayerCoords(IsoGameCharacter player) {
        return "(" + player.getXi() + "," + player.getYi() + "," + player.getZi() + ")";
    }
}
