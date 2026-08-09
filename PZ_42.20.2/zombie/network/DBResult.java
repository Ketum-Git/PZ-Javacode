// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.UsedFromLua;

@UsedFromLua
public class DBResult {
    private final HashMap<String, String> values = new HashMap<>();
    private ArrayList<String> columns = new ArrayList<>();
    private String type;
    private String tableName;

    public HashMap<String, String> getValues() {
        return this.values;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<String> getColumns() {
        return this.columns;
    }

    public void setColumns(ArrayList<String> columns) {
        this.columns = columns;
    }

    public String getTableName() {
        return this.tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
}
