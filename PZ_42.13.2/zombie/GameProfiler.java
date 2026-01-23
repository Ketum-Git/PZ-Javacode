// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import zombie.core.profiling.TriggerGameProfilerFile;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.IsoCamera;
import zombie.ui.TextManager;
import zombie.util.IPooledObject;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class GameProfiler {
    private static final String s_currentSessionUUID = UUID.randomUUID().toString();
    private static final ThreadLocal<GameProfiler> s_instance = ThreadLocal.withInitial(GameProfiler::new);
    private final Stack<GameProfiler.ProfileArea> stack = new Stack<>();
    private final GameProfiler.RecordingFrame currentFrame = new GameProfiler.RecordingFrame();
    private final GameProfiler.RecordingFrame previousFrame = new GameProfiler.RecordingFrame();
    private boolean isInFrame;
    private boolean isRunning;
    private final GameProfileRecording recorder;
    private static final Object m_gameProfilerRecordingTriggerLock = "Game Profiler Recording Watcher, synchronization lock";
    private static PredicatedFileWatcher gameProfilerRecordingTriggerWatcher;
    private static final ArrayList<String> m_validThreadNames = new ArrayList<>();
    private static final int MAX_DEPTH = 20;

    private GameProfiler() {
        String currentThreadName = Thread.currentThread().getName();
        String recordingClassName = currentThreadName.replace("-", "").replace(" ", "");
        String recordingUUID = String.format("%s_GameProfiler_%s", this.getCurrentSessionUUID(), recordingClassName);
        this.recorder = new GameProfileRecording(recordingUUID);
    }

    public static boolean isValidThread() {
        return m_validThreadNames.contains(Thread.currentThread().getName());
    }

    private static void onTrigger_setAnimationRecorderTriggerFile(TriggerGameProfilerFile triggerXml) {
        DebugOptions.instance.gameProfilerEnabled.setValue(triggerXml.isRecording);
    }

    private String getCurrentSessionUUID() {
        return s_currentSessionUUID;
    }

    public static GameProfiler getInstance() {
        return s_instance.get();
    }

    public void startFrame(String frameInvokerKey) {
        if (this.isInFrame) {
            throw new RuntimeException("Already inside a frame.");
        } else {
            this.isInFrame = true;
            this.isRunning = DebugOptions.instance.gameProfilerEnabled.getValue();
            if (!this.stack.empty()) {
                throw new RuntimeException("Recording stack should be empty at the start of a frame.");
            } else if (this.isRunning) {
                int frameCount = IsoCamera.frameState.frameCount;
                if (this.currentFrame.frameNo != frameCount) {
                    this.previousFrame.transferFrom(this.currentFrame);
                    if (this.previousFrame.frameNo != -1) {
                        this.recorder.writeLine();
                    }

                    long timeNs = getTimeNs();
                    this.currentFrame.frameNo = frameCount;
                    this.currentFrame.frameInvokerKey = frameInvokerKey;
                    this.currentFrame.startTime = timeNs;
                    this.recorder.reset();
                    this.recorder.setFrameNumber(this.currentFrame.frameNo);
                    this.recorder.setStartTime(this.currentFrame.startTime);
                }
            }
        }
    }

    public void endFrame() {
        try {
            if (!this.isInFrame) {
                throw new RuntimeException("Not inside a frame.");
            }

            if (!this.isRunning) {
                return;
            }

            this.currentFrame.endTime = getTimeNs();
            this.currentFrame.totalTime = this.currentFrame.endTime - this.currentFrame.startTime;
            if (!this.stack.empty()) {
                throw new RuntimeException("Recording stack should be empty at the end of a frame.");
            }
        } finally {
            this.isInFrame = false;
            this.isRunning = DebugOptions.instance.gameProfilerEnabled.getValue();
        }
    }

    private boolean checkShouldMeasure() {
        if (!isRunning()) {
            return false;
        } else if (!this.isInFrame) {
            DebugLog.General.warn("Not inside in a frame. Find the root caller function for this thread, and add call to invokeAndMeasureFrame.");
            return false;
        } else {
            return true;
        }
    }

    public static boolean isRunning() {
        return getInstance().isRunning;
    }

    public GameProfiler.@Nullable ProfileArea profile(String key) {
        return this.checkShouldMeasure() ? this.start(key) : null;
    }

    @Deprecated
    public GameProfiler.ProfileArea start(String areaKey) {
        if (this.stack.size() >= 20) {
            return null;
        } else {
            long timeNs = getTimeNs();
            GameProfiler.ProfileArea area = GameProfiler.ProfileArea.alloc();
            area.key = areaKey;
            return this.start(area, timeNs);
        }
    }

    private synchronized GameProfiler.ProfileArea start(GameProfiler.ProfileArea area, long timeNs) {
        area.startTime = timeNs;
        area.depth = this.stack.size();
        if (!this.stack.isEmpty()) {
            GameProfiler.ProfileArea parentArea = this.stack.peek();
            parentArea.children.add(area);
        }

        this.stack.push(area);
        return area;
    }

    @Deprecated
    public synchronized void end(GameProfiler.ProfileArea area) {
        if (area != null) {
            area.endTime = getTimeNs();
            area.total = area.endTime - area.startTime;
            if (this.stack.peek() != area) {
                throw new RuntimeException("Incorrect exit. ProfileArea " + area + " is not at the top of the stack: " + this.stack.peek());
            } else {
                this.stack.pop();
                if (this.stack.isEmpty()) {
                    this.recorder.logTimeSpan(area);
                    area.release();
                }
            }
        }
    }

    private void renderPercent(String label, long time, int x, int y, float r, float g, float b) {
        float tFloat = (float)time / (float)this.previousFrame.totalTime;
        tFloat *= 100.0F;
        tFloat = (int)(tFloat * 10.0F) / 10.0F;
        TextManager.instance.DrawString(x, y, label, r, g, b, 1.0);
        TextManager.instance.DrawString(x + 300, y, tFloat + "%", r, g, b, 1.0);
    }

    public void render(int x, int y) {
        this.renderPercent(this.previousFrame.frameInvokerKey, this.previousFrame.totalTime, x, y, 1.0F, 1.0F, 1.0F);
    }

    public static long getTimeNs() {
        return System.nanoTime();
    }

    public static void init() {
        initTriggerWatcher();
    }

    private static void initTriggerWatcher() {
        if (gameProfilerRecordingTriggerWatcher == null) {
            synchronized (m_gameProfilerRecordingTriggerLock) {
                if (gameProfilerRecordingTriggerWatcher == null) {
                    gameProfilerRecordingTriggerWatcher = new PredicatedFileWatcher(
                        ZomboidFileSystem.instance.getMessagingDirSub("Trigger_PerformanceProfiler.xml"),
                        TriggerGameProfilerFile.class,
                        GameProfiler::onTrigger_setAnimationRecorderTriggerFile
                    );
                    DebugFileWatcher.instance.add(gameProfilerRecordingTriggerWatcher);
                }
            }
        }
    }

    static {
        m_validThreadNames.add("main");
        m_validThreadNames.add("MainThread");
    }

    public static class ProfileArea extends PooledObject implements AutoCloseable {
        public String key;
        public long startTime;
        public long endTime;
        public long total;
        public int depth;
        public float r = 1.0F;
        public float g = 1.0F;
        public float b = 1.0F;
        public final List<GameProfiler.ProfileArea> children = new ArrayList<>();
        private static final Pool<GameProfiler.ProfileArea> s_pool = new Pool<>(GameProfiler.ProfileArea::new);

        @Override
        public void onReleased() {
            super.onReleased();
            this.clear();
        }

        public void clear() {
            this.startTime = 0L;
            this.endTime = 0L;
            this.total = 0L;
            this.depth = 0;
            IPooledObject.release(this.children);
        }

        public static GameProfiler.ProfileArea alloc() {
            return s_pool.alloc();
        }

        @Override
        public void close() {
            GameProfiler.getInstance().end(this);
        }
    }

    public static class RecordingFrame {
        private String frameInvokerKey = "";
        private int frameNo = -1;
        private long startTime;
        private long endTime;
        private long totalTime;

        public void transferFrom(GameProfiler.RecordingFrame srcFrame) {
            this.clear();
            this.frameNo = srcFrame.frameNo;
            this.frameInvokerKey = srcFrame.frameInvokerKey;
            this.startTime = srcFrame.startTime;
            this.endTime = srcFrame.endTime;
            this.totalTime = srcFrame.totalTime;
            srcFrame.clear();
        }

        public void clear() {
            this.frameNo = -1;
            this.frameInvokerKey = "";
            this.startTime = 0L;
            this.endTime = 0L;
            this.totalTime = 0L;
        }
    }
}
