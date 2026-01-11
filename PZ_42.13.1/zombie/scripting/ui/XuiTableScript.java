// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.ui;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;

@UsedFromLua
public class XuiTableScript extends XuiScript {
    private final ArrayList<XuiTableScript.XuiTableColumnScript> columns = new ArrayList<>();
    private final ArrayList<XuiTableScript.XuiTableRowScript> rows = new ArrayList<>();
    private final ArrayList<XuiTableScript.XuiTableCellScript> cells = new ArrayList<>();
    private final XuiScript.XuiString xuiCellStyle = this.addVar(new XuiScript.XuiString(this, "xuiCellStyle"));
    private final XuiScript.XuiString xuiRowStyle;
    private final XuiScript.XuiString xuiColumnStyle;

    public XuiTableScript(String xuiLayoutName, boolean readAltKeys, XuiScriptType type) {
        super(xuiLayoutName, readAltKeys, "ISXuiTableLayout", type);
        this.xuiCellStyle.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.xuiCellStyle.setScriptLoadEnabled(false);
        this.xuiCellStyle.setIgnoreStyling(true);
        this.xuiRowStyle = this.addVar(new XuiScript.XuiString(this, "xuiRowStyle"));
        this.xuiRowStyle.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.xuiRowStyle.setScriptLoadEnabled(false);
        this.xuiRowStyle.setIgnoreStyling(true);
        this.xuiColumnStyle = this.addVar(new XuiScript.XuiString(this, "xuiColumnStyle"));
        this.xuiColumnStyle.setAutoApplyMode(XuiAutoApply.Forbidden);
        this.xuiColumnStyle.setScriptLoadEnabled(false);
        this.xuiColumnStyle.setIgnoreStyling(true);
    }

    public XuiScript.XuiString getCellStyle() {
        return this.xuiCellStyle;
    }

    public XuiScript.XuiString getRowStyle() {
        return this.xuiRowStyle;
    }

    public XuiScript.XuiString getColumnStyle() {
        return this.xuiColumnStyle;
    }

    public int getColumnCount() {
        return this.columns.size();
    }

    public int getRowCount() {
        return this.rows.size();
    }

    public XuiScript getColumn(int index) {
        return index >= 0 && index < this.columns.size() ? this.columns.get(index) : null;
    }

    public XuiScript getRow(int index) {
        return index >= 0 && index < this.rows.size() ? this.rows.get(index) : null;
    }

    public XuiScript getCell(int column, int row) {
        int index = column + row * this.columns.size();
        return index >= 0 && index < this.cells.size() ? this.cells.get(index) : null;
    }

    private int readCellIndex(String s, int columnCount) {
        String[] split = s.split(":");
        if (split.length == 2) {
            int x = Integer.parseInt(split[0].trim());
            int y = Integer.parseInt(split[1].trim());
            return x + y * columnCount;
        } else {
            return -1;
        }
    }

    private int countRowsOrColumns(ScriptParser.Block block) {
        int count = 0;

        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty() && key.startsWith("[") && key.contains("]")) {
                int index = this.getIndex(key);
                count = PZMath.max(index, count);
            }
        }

        return count;
    }

    public <T extends XuiScript> void LoadColumnsRows(ScriptParser.Block block, ArrayList<T> list) {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty() && key.startsWith("[") && key.contains("]")) {
                int index = this.getIndex(key);
                if (index >= 0 && index < list.size()) {
                    list.get(index).loadVar(this.getPostIndexKey(key), val);
                }
            }
        }
    }

    private String getPostIndexKey(String s) {
        if (s.contains("]")) {
            int i = s.indexOf("]");
            return s.substring(i + 1).trim();
        } else {
            return s;
        }
    }

    private int getIndex(String s) {
        if (s.startsWith("[") && s.contains("]")) {
            int i = s.indexOf("]");
            return Integer.parseInt(s.substring(1, i));
        } else {
            return -1;
        }
    }

    @Override
    public void Load(ScriptParser.Block block) {
        XuiScript cellStyle = null;
        XuiScript rowStyle = null;
        XuiScript columnStyle = null;
        if (this.isLayout()) {
            for (ScriptParser.Value value : block.values) {
                String key = value.getKey().trim();
                String val = value.getValue().trim();
                if (!key.isEmpty() && !val.isEmpty()) {
                    if (this.xuiCellStyle.acceptsKey(key)) {
                        this.xuiCellStyle.fromString(val);
                    } else if (this.xuiRowStyle.acceptsKey(key)) {
                        this.xuiRowStyle.fromString(val);
                    } else if (this.xuiColumnStyle.acceptsKey(key)) {
                        this.xuiColumnStyle.fromString(val);
                    }
                }
            }

            cellStyle = XuiManager.GetStyle(this.xuiCellStyle.value());
            rowStyle = XuiManager.GetStyle(this.xuiRowStyle.value());
            columnStyle = XuiManager.GetStyle(this.xuiColumnStyle.value());
        }

        super.Load(block);

        for (ScriptParser.Value valuex : block.values) {
            String key = valuex.getKey().trim();
            String val = valuex.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty() && this.isLayout() && (key.equalsIgnoreCase("xuiColumns") || key.equalsIgnoreCase("xuiRows"))) {
                String[] split = val.split(":");

                for (int i = 0; i < split.length; i++) {
                    String ss = split[i].trim();
                    if (key.equalsIgnoreCase("xuiRows")) {
                        XuiTableScript.XuiTableRowScript script = new XuiTableScript.XuiTableRowScript(this.xuiLayoutName, this.readAltKeys, rowStyle);
                        script.loadVar("height", ss);
                        this.rows.add(script);
                    } else {
                        XuiTableScript.XuiTableColumnScript script = new XuiTableScript.XuiTableColumnScript(this.xuiLayoutName, this.readAltKeys, columnStyle);
                        script.loadVar("width", ss);
                        this.columns.add(script);
                    }
                }
            }
        }

        for (ScriptParser.Block child : block.children) {
            if (this.isLayout() && (child.type.equalsIgnoreCase("xuiColumns") || child.type.equalsIgnoreCase("xuiRows"))) {
                boolean isRow = child.type.equalsIgnoreCase("xuiRows");
                int count = this.countRowsOrColumns(child);
                int listCount = isRow ? this.rows.size() : this.columns.size();

                for (int ix = 0; ix < count; ix++) {
                    if (ix >= listCount) {
                        if (isRow) {
                            XuiTableScript.XuiTableRowScript script = new XuiTableScript.XuiTableRowScript(this.xuiLayoutName, this.readAltKeys, rowStyle);
                            script.height.setValue(1.0F, true);
                            this.rows.add(script);
                        } else {
                            XuiTableScript.XuiTableColumnScript script = new XuiTableScript.XuiTableColumnScript(
                                this.xuiLayoutName, this.readAltKeys, columnStyle
                            );
                            script.width.setValue(1.0F, true);
                            this.columns.add(script);
                        }
                    }
                }

                if (isRow) {
                    this.LoadColumnsRows(child, this.rows);
                } else {
                    this.LoadColumnsRows(child, this.columns);
                }
            }
        }

        if (this.isLayout() && !this.columns.isEmpty() && !this.rows.isEmpty()) {
            int cellCount = this.columns.size() * this.rows.size();

            for (int ixx = 0; ixx < cellCount; ixx++) {
                XuiTableScript.XuiTableCellScript script = new XuiTableScript.XuiTableCellScript(this.xuiLayoutName, this.readAltKeys, cellStyle);
                this.cells.add(script);
            }

            for (ScriptParser.Block childx : block.children) {
                if (childx.type.equalsIgnoreCase("xuiCell")) {
                    int index = this.readCellIndex(childx.id, this.columns.size());
                    if (index >= 0 && index < cellCount) {
                        this.cells.get(index).Load(childx);
                        this.cells.get(index).cellHasLoaded = true;
                    }
                }
            }
        } else if (this.isLayout() && (!this.columns.isEmpty() || !this.rows.isEmpty())) {
            this.warnWithInfo("XuiScript has only rows or columns.");
        }
    }

    @Override
    protected void postLoad() {
        super.postLoad();

        for (XuiTableScript.XuiTableRowScript element : this.rows) {
            element.postLoad();
        }

        for (XuiTableScript.XuiTableColumnScript element : this.columns) {
            element.postLoad();
        }

        for (XuiTableScript.XuiTableCellScript element : this.cells) {
            element.postLoad();
        }
    }

    @UsedFromLua
    public static class XuiTableCellScript extends XuiScript {
        protected boolean cellHasLoaded;

        public XuiTableCellScript(String xuiLayoutName, boolean readAltKeys, XuiScript style) {
            super(xuiLayoutName, readAltKeys, "ISXuiTableLayoutCell", XuiScriptType.Layout);
            if (style != null) {
                this.setStyle(style);
            }

            this.tryToSetDefaultStyle();
        }

        public boolean isCellHasLoaded() {
            return this.cellHasLoaded;
        }
    }

    @UsedFromLua
    public static class XuiTableColumnScript extends XuiScript {
        public XuiTableColumnScript(String xuiLayoutName, boolean readAltKeys, XuiScript style) {
            super(xuiLayoutName, readAltKeys, "ISXuiTableLayoutColumn", XuiScriptType.Layout);
            if (style != null) {
                this.setStyle(style);
            }

            this.tryToSetDefaultStyle();
        }
    }

    @UsedFromLua
    public static class XuiTableRowScript extends XuiScript {
        public XuiTableRowScript(String xuiLayoutName, boolean readAltKeys, XuiScript style) {
            super(xuiLayoutName, readAltKeys, "ISXuiTableLayoutRow", XuiScriptType.Layout);
            if (style != null) {
                this.setStyle(style);
            }

            this.tryToSetDefaultStyle();
        }
    }
}
