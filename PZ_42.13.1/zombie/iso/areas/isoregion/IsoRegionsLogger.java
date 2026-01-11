// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Core;
import zombie.debug.DebugLog;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class IsoRegionsLogger {
    private final ConcurrentLinkedQueue<IsoRegionsLogger.IsoRegionLog> pool = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<IsoRegionsLogger.IsoRegionLog> loggerQueue = new ConcurrentLinkedQueue<>();
    private final boolean consolePrint;
    private final ArrayList<IsoRegionsLogger.IsoRegionLog> logs = new ArrayList<>();
    private final int maxLogs = 100;
    private boolean isDirtyUi;

    public IsoRegionsLogger(boolean doConsolePrint) {
        this.consolePrint = doConsolePrint;
    }

    public ArrayList<IsoRegionsLogger.IsoRegionLog> getLogs() {
        return this.logs;
    }

    public boolean isDirtyUI() {
        return this.isDirtyUi;
    }

    public void unsetDirtyUI() {
        this.isDirtyUi = false;
    }

    private IsoRegionsLogger.IsoRegionLog getLog() {
        IsoRegionsLogger.IsoRegionLog log = this.pool.poll();
        if (log == null) {
            log = new IsoRegionsLogger.IsoRegionLog();
        }

        return log;
    }

    protected void log(String str) {
        this.log(str, null);
    }

    protected void log(String str, Color col) {
        if (Core.debug) {
            if (this.consolePrint) {
                DebugLog.IsoRegion.println(str);
            }

            IsoRegionsLogger.IsoRegionLog log = this.getLog();
            log.str = str;
            log.type = IsoRegionLogType.Normal;
            log.col = col;
            this.loggerQueue.offer(log);
        }
    }

    protected void warn(String str) {
        DebugLog.IsoRegion.warn(str);
        if (Core.debug) {
            IsoRegionsLogger.IsoRegionLog log = this.getLog();
            log.str = str;
            log.type = IsoRegionLogType.Warn;
            this.loggerQueue.offer(log);
        }
    }

    protected void update() {
        if (Core.debug) {
            for (IsoRegionsLogger.IsoRegionLog log = this.loggerQueue.poll(); log != null; log = this.loggerQueue.poll()) {
                if (this.logs.size() >= 100) {
                    IsoRegionsLogger.IsoRegionLog removed = this.logs.remove(0);
                    removed.col = null;
                    this.pool.offer(removed);
                }

                this.logs.add(log);
                this.isDirtyUi = true;
            }
        }
    }

    @UsedFromLua
    public static class IsoRegionLog {
        private String str;
        private IsoRegionLogType type;
        private Color col;

        public String getStr() {
            return this.str;
        }

        public IsoRegionLogType getType() {
            return this.type;
        }

        public Color getColor() {
            if (this.col != null) {
                return this.col;
            } else {
                return this.type == IsoRegionLogType.Warn ? Color.red : Color.white;
            }
        }
    }
}
