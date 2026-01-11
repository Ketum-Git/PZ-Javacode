// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.debug;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import zombie.ZomboidFileSystem;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.util.list.PZArrayUtil;

public abstract class GenericNameValueRecordingFrame {
    protected String[] columnNames = new String[0];
    protected final HashMap<String, Integer> nameIndices = new HashMap<>();
    protected boolean headerDirty;
    protected final String fileKey;
    protected PrintStream outHeader;
    private ByteArrayOutputStream outHeaderByteArrayStream;
    protected PrintStream outHeaderFile;
    protected PrintStream outValues;
    private String headerFilePath;
    private String valuesFilePath;
    protected int firstFrameNumber = -1;
    protected int frameNumber = -1;
    protected static final String delim = ",";
    protected final String valuesFileNameSuffix;
    private String previousLine;
    private int previousFrameNo = -1;
    protected final StringBuilder lineBuffer = new StringBuilder();

    public GenericNameValueRecordingFrame(String fileKey, String valuesFileNameSuffix) {
        this.fileKey = fileKey;
        this.valuesFileNameSuffix = valuesFileNameSuffix;
    }

    protected int addColumnInternal(String name) {
        int animIndex = this.columnNames.length;
        this.columnNames = PZArrayUtil.add(this.columnNames, name);
        this.nameIndices.put(name, animIndex);
        this.headerDirty = true;
        this.onColumnAdded();
        return animIndex;
    }

    public int getOrCreateColumn(String nodeName) {
        return this.nameIndices.containsKey(nodeName) ? this.nameIndices.get(nodeName) : this.addColumnInternal(nodeName);
    }

    public void setFrameNumber(int frameNumber) {
        if (this.frameNumber != frameNumber) {
            this.frameNumber = frameNumber;
            if (this.firstFrameNumber == -1) {
                this.firstFrameNumber = this.frameNumber;
            }

            this.headerDirty = true;
        }
    }

    public int getColumnCount() {
        return this.columnNames.length;
    }

    public String getNameAt(int i) {
        return this.columnNames[i];
    }

    public abstract String getValueAt(int i);

    protected void openHeader() {
        if (this.outHeader != null) {
            this.outHeader.close();
            this.outHeader = null;
        }

        this.outHeaderByteArrayStream = new ByteArrayOutputStream();
        this.outHeader = new PrintStream(this.outHeaderByteArrayStream, true, StandardCharsets.UTF_8);
    }

    protected void flushHeaderToFile() {
        this.outHeaderFile = AnimationPlayerRecorder.openFileStream(this.fileKey + "_header", false, filePath -> this.headerFilePath = filePath);
        byte[] bytes = this.outHeaderByteArrayStream.toByteArray();

        try {
            this.outHeaderFile.write(bytes);
        } catch (IOException var3) {
            DebugType.General.printException(var3, "Exception thrown trying to write recording header file.", LogSeverity.Error);
        }

        this.outHeaderByteArrayStream.reset();
    }

    protected void openValuesFile(boolean append) {
        if (this.outValues != null) {
            this.outValues.close();
            this.outValues = null;
        }

        this.outValues = AnimationPlayerRecorder.openFileStream(this.fileKey + this.valuesFileNameSuffix, append, filePath -> this.valuesFilePath = filePath);
    }

    public void writeLine() {
        if (this.headerDirty || this.outHeader == null) {
            this.headerDirty = false;
            this.writeHeaderToMemory();
            this.flushHeaderToFile();
        }

        this.writeData();
    }

    public void close() {
        if (this.outHeader != null) {
            this.outHeader.close();
            this.outHeader = null;
        }

        if (this.outValues != null) {
            this.outValues.close();
            this.outValues = null;
        }
    }

    public void closeAndDiscard() {
        this.close();
        ZomboidFileSystem.instance.tryDeleteFile(this.headerFilePath);
        this.headerFilePath = null;
        ZomboidFileSystem.instance.tryDeleteFile(this.valuesFilePath);
        this.valuesFilePath = null;
        this.previousLine = null;
        this.previousFrameNo = -1;
    }

    protected abstract void onColumnAdded();

    public abstract void reset();

    protected void writeHeaderToMemory() {
        StringBuilder logLine = new StringBuilder();
        logLine.append("frameNo");
        this.buildHeader(logLine);
        this.openHeader();
        this.outHeader.println(logLine);
        this.outHeader.println(this.firstFrameNumber + "," + this.frameNumber);
    }

    protected void buildHeader(StringBuilder logLine) {
        int i = 0;

        for (int columnCount = this.getColumnCount(); i < columnCount; i++) {
            appendCell(logLine, this.getNameAt(i));
        }
    }

    protected void writeData() {
        if (this.outValues == null) {
            this.openValuesFile(false);
        }

        StringBuilder logLine = this.lineBuffer;
        logLine.setLength(0);
        this.writeData(logLine);
        if (this.previousLine == null || !this.previousLine.contentEquals(logLine)) {
            this.outValues.print(this.frameNumber);
            this.outValues.println(logLine);
            this.previousLine = logLine.toString();
            this.previousFrameNo = this.frameNumber;
        }
    }

    protected void writeData(StringBuilder logLine) {
        int i = 0;

        for (int columnCount = this.getColumnCount(); i < columnCount; i++) {
            appendCell(logLine, this.getValueAt(i));
        }
    }

    /**
     * Append empty cell
     */
    public static StringBuilder appendCell(StringBuilder logLine) {
        return logLine.append(",");
    }

    /**
     * Append text cell, no quotes
     */
    public static StringBuilder appendCell(StringBuilder logLine, String cell) {
        return logLine.append(",").append(cell);
    }

    /**
     * Append numeric cell
     */
    public static StringBuilder appendCell(StringBuilder logLine, float cell) {
        return logLine.append(",").append(cell);
    }

    /**
     * Append numeric cell
     */
    public static StringBuilder appendCell(StringBuilder logLine, int cell) {
        return logLine.append(",").append(cell);
    }

    /**
     * Append numeric cell
     */
    public static StringBuilder appendCell(StringBuilder logLine, long cell) {
        return logLine.append(",").append(cell);
    }

    /**
     * Append text cell, enclosed in quotes
     */
    public static StringBuilder appendCellQuot(StringBuilder logLine, String cell) {
        return logLine.append(",").append('"').append(cell).append('"');
    }
}
