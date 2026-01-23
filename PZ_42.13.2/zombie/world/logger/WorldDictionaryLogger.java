// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugLog;
import zombie.network.GameClient;

public class WorldDictionaryLogger {
    private static final ArrayList<Log.BaseLog> _logItems = new ArrayList<>();

    public static void reset() {
        _logItems.clear();
    }

    public static void startLogging() {
        reset();
    }

    public static void log(Log.BaseLog log) {
        if (!GameClient.client) {
            _logItems.add(log);
        }
    }

    public static void log(String msg) {
        log(msg, true);
    }

    public static void log(String msg, boolean debugPrint) {
        if (!GameClient.client) {
            if (debugPrint) {
                DebugLog.log("WorldDictionary: " + msg);
            }

            _logItems.add(new Log.Comment(msg));
        }
    }

    public static void saveLog(String saveFile) throws IOException {
        if (!GameClient.client) {
            boolean changesToSave = false;

            for (int i = 0; i < _logItems.size(); i++) {
                Log.BaseLog log = _logItems.get(i);
                if (!log.isIgnoreSaveCheck()) {
                    changesToSave = true;
                    break;
                }
            }

            if (changesToSave) {
                File path = new File(ZomboidFileSystem.instance.getCurrentSaveDir() + File.separator);
                if (path.exists() && path.isDirectory()) {
                    String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave(saveFile);
                    File f = new File(fileName);

                    try (FileWriter w = new FileWriter(f, true)) {
                        w.write("log = log or {};" + System.lineSeparator());
                        w.write("table.insert(log, {" + System.lineSeparator());

                        for (int ix = 0; ix < _logItems.size(); ix++) {
                            Log.BaseLog log = _logItems.get(ix);
                            log.saveAsText(w, "\t");
                        }

                        w.write("};" + System.lineSeparator());
                    } catch (Exception var11) {
                        var11.printStackTrace();
                        throw new IOException("Error saving WorldDictionary log.");
                    }
                }
            }
        }
    }
}
