// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.ArrayList;
import zombie.debug.DebugLog;
import zombie.network.GameServer;

public class TimeDebugger {
    ArrayList<Long> records = new ArrayList<>();
    ArrayList<String> recordStrings = new ArrayList<>();
    String name = "";

    public TimeDebugger(String _name) {
        this.name = _name;
    }

    public void clear() {
        if (GameServer.server) {
            this.records.clear();
            this.recordStrings.clear();
        }
    }

    public void start() {
        if (GameServer.server) {
            this.records.clear();
            this.recordStrings.clear();
            this.records.add(System.currentTimeMillis());
            this.recordStrings.add("Start");
        }
    }

    public void record() {
        if (GameServer.server) {
            this.records.add(System.currentTimeMillis());
            this.recordStrings.add(String.valueOf(this.records.size()));
        }
    }

    public void record(String note) {
        if (GameServer.server) {
            this.records.add(System.currentTimeMillis());
            this.recordStrings.add(note);
        }
    }

    public void recordTO(String note, int minTime) {
        if (GameServer.server && this.records.get(this.records.size() - 1) - this.records.get(this.records.size() - 2) > minTime) {
            this.records.add(System.currentTimeMillis());
            this.recordStrings.add(note);
        }
    }

    public void add(TimeDebugger td) {
        if (GameServer.server) {
            String tdname = td.name;

            for (int i = 0; i < td.records.size(); i++) {
                this.records.add(td.records.get(i));
                this.recordStrings.add(tdname + "|" + td.recordStrings.get(i));
            }

            td.clear();
        }
    }

    public void print() {
        if (GameServer.server) {
            this.records.add(System.currentTimeMillis());
            this.recordStrings.add("END");
            if (this.records.size() > 1) {
                DebugLog.log("=== DBG " + this.name + " ===");
                long start = this.records.get(0);

                for (int i = 1; i < this.records.size(); i++) {
                    long prevTime = this.records.get(i - 1);
                    long time = this.records.get(i);
                    String note = this.recordStrings.get(i);
                    DebugLog.log("RECORD " + i + " " + note + " A:" + (time - start) + " D:" + (time - prevTime));
                }

                DebugLog.log("=== END " + this.name + " (" + (this.records.get(this.records.size() - 1) - start) + ") ===");
            } else {
                DebugLog.log("<< DBG " + this.name + " ERROR >>");
            }
        }
    }

    public long getExecTime() {
        return this.records.isEmpty() ? 0L : System.currentTimeMillis() - this.records.get(0);
    }
}
