// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import zombie.core.skinnedmodel.animation.debug.AnimationPlayerRecorder;
import zombie.core.skinnedmodel.animation.debug.GenericNameValueRecordingFrame;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.list.PZArrayUtil;

public final class GameProfileRecording extends GenericNameValueRecordingFrame {
    private long startTime;
    private final GameProfileRecording.Row rootRow = new GameProfileRecording.Row();
    private final HashMap<String, Integer> keyValueTable = new HashMap<>();
    protected PrintStream outSegment;
    private long firstFrameNo = -1L;
    private final List<String> segmentFilePaths = new ArrayList<>();
    private int numFramesPerFile = 60;
    private int currentSegmentFrameCount;

    public GameProfileRecording(String fileKey) {
        super(fileKey, "_times");
        this.addColumnInternal("StartTime");
        this.addColumnInternal("EndTime");
        this.addColumnInternal("SegmentNo");
        this.addColumnInternal("Spans");
        this.addColumnInternal("key");
        this.addColumnInternal("Depth");
        this.addColumnInternal("StartTime");
        this.addColumnInternal("EndTime");
        this.addColumnInternal("Time Format");
        this.addColumnInternal("x * 100ns");
    }

    public void setNumFramesPerSegment(int count) {
        this.numFramesPerFile = count;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void logTimeSpan(GameProfiler.ProfileArea area) {
        if (this.firstFrameNo == -1L) {
            this.firstFrameNo = this.frameNumber;
        }

        GameProfileRecording.Span newSpan = this.allocSpan(area);
        GameProfileRecording.Row parentRow = this.rootRow;
        if (parentRow.spans.isEmpty()) {
            parentRow.startTime = newSpan.startTime;
        }

        parentRow.endTime = newSpan.endTime;
        parentRow.spans.add(newSpan);
    }

    protected GameProfileRecording.Span allocSpan(GameProfiler.ProfileArea area) {
        int keyIdx = this.getOrCreateKey(area.key);
        long startTime = area.startTime - this.startTime;
        long endTime = area.endTime - this.startTime;
        GameProfileRecording.Span newSpan = GameProfileRecording.Span.alloc();
        newSpan.key = keyIdx;
        newSpan.depth = area.depth;
        newSpan.startTime = startTime;
        newSpan.endTime = endTime;
        int i = 0;

        for (int childSpanCount = area.children.size(); i < childSpanCount; i++) {
            GameProfiler.ProfileArea childArea = area.children.get(i);
            GameProfileRecording.Span childSpan = this.allocSpan(childArea);
            newSpan.children.add(childSpan);
        }

        return newSpan;
    }

    private int getOrCreateKey(String key) {
        Integer idx = this.keyValueTable.get(key);
        if (idx == null) {
            idx = this.keyValueTable.size();
            this.keyValueTable.put(key, idx);
            this.headerDirty = true;
        }

        return idx;
    }

    @Override
    public String getValueAt(int i) {
        throw new RuntimeException("Not implemented. Use getValueAt(row, col)");
    }

    @Override
    protected void onColumnAdded() {
    }

    @Override
    public void reset() {
        this.rootRow.reset();
    }

    protected void openSegmentFile(boolean append) {
        if (this.outSegment != null) {
            this.outSegment.flush();
            this.outSegment.close();
        }

        String fileKey = String.format("%s%s_%04d", this.fileKey, this.valuesFileNameSuffix, this.segmentFilePaths.size());
        this.outSegment = AnimationPlayerRecorder.openFileStream(fileKey, append, this.segmentFilePaths::add);
        this.currentSegmentFrameCount = 0;
        this.headerDirty = true;
    }

    @Override
    public void close() {
        if (this.outSegment != null) {
            this.outSegment.close();
            this.outSegment = null;
        }
    }

    @Override
    public void closeAndDiscard() {
        super.closeAndDiscard();
        PZArrayUtil.forEach(this.segmentFilePaths, ZomboidFileSystem.instance::tryDeleteFile);
        this.segmentFilePaths.clear();
    }

    @Override
    protected void writeData() {
        if (this.outValues == null) {
            this.openValuesFile(false);
        }

        StringBuilder logLine = this.lineBuffer;
        logLine.setLength(0);
        this.currentSegmentFrameCount++;
        if (this.outSegment == null || this.currentSegmentFrameCount >= this.numFramesPerFile) {
            this.openSegmentFile(false);
        }

        this.writeDataRow(logLine, this.rootRow);
        this.outSegment.print(this.frameNumber);
        this.outSegment.println(logLine);
        logLine = this.lineBuffer;
        logLine.setLength(0);
        this.writeFrameTimeRow(logLine, this.rootRow, this.segmentFilePaths.size() - 1);
        this.outValues.print(this.frameNumber);
        this.outValues.println(logLine);
    }

    private void writeDataRow(StringBuilder logLine, GameProfileRecording.Row row) {
        int i = 0;

        for (int columnCount = row.spans.size(); i < columnCount; i++) {
            GameProfileRecording.Span span = row.spans.get(i);
            this.writeSpan(logLine, row, span);
        }
    }

    private void writeFrameTimeRow(StringBuilder logLine, GameProfileRecording.Row row, int segmentNo) {
        appendCell(logLine, row.startTime / 100L);
        appendCell(logLine, row.endTime / 100L);
        appendCell(logLine, segmentNo);
    }

    private void writeSpan(StringBuilder logLine, GameProfileRecording.Row parentRow, GameProfileRecording.Span span) {
        long startTime = (span.startTime - parentRow.startTime) / 100L;
        long length = (span.endTime - span.startTime) / 100L;
        appendCell(logLine, span.key);
        appendCell(logLine, span.depth);
        appendCell(logLine, startTime);
        appendCell(logLine, length);
        int i = 0;

        for (int spanCount = span.children.size(); i < spanCount; i++) {
            GameProfileRecording.Span childSpan = span.children.get(i);
            this.writeSpan(logLine, parentRow, childSpan);
        }
    }

    @Override
    protected void writeHeaderToMemory() {
        super.writeHeaderToMemory();
        this.outHeader.println();
        this.outHeader.println("Segmentation Info");
        this.outHeader.println("FirstFrame," + this.firstFrameNo);
        this.outHeader.println("NumFramesPerFile," + this.numFramesPerFile);
        this.outHeader.println("NumFiles," + this.segmentFilePaths.size());
        this.outHeader.println();
        this.outHeader.println("KeyNamesTable");
        this.outHeader.println("Index,Name");
        StringBuilder line = new StringBuilder();

        for (Entry<String, Integer> entry : this.keyValueTable.entrySet()) {
            line.setLength(0);
            line.append(entry.getValue());
            line.append(",");
            line.append(entry.getKey());
            this.outHeader.println(line);
        }
    }

    public static class Row {
        long startTime;
        long endTime;
        final List<GameProfileRecording.Span> spans = new ArrayList<>();

        public void reset() {
            IPooledObject.release(this.spans);
        }
    }

    public static class Span extends PooledObject {
        int key;
        int depth;
        long startTime;
        long endTime;
        final List<GameProfileRecording.Span> children = new ArrayList<>();
        private static final Pool<GameProfileRecording.Span> s_pool = new Pool<>(GameProfileRecording.Span::new);

        @Override
        public void onReleased() {
            super.onReleased();
            IPooledObject.release(this.children);
        }

        public static GameProfileRecording.Span alloc() {
            return s_pool.alloc();
        }
    }
}
