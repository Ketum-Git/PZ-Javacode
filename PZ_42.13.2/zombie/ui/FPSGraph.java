// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.utils.BoundedQueue;
import zombie.input.Mouse;

public final class FPSGraph extends UIElement {
    public static FPSGraph instance;
    private static final int NUM_BARS = 30;
    private static final int BAR_WID = 8;
    private final FPSGraph.Graph fpsGraph = new FPSGraph.Graph();
    private final FPSGraph.Graph upsGraph = new FPSGraph.Graph();
    private final FPSGraph.Graph lpsGraph = new FPSGraph.Graph();
    private final FPSGraph.Graph uiGraph = new FPSGraph.Graph();

    public FPSGraph() {
        this.setVisible(false);
        this.setWidth(232.0);
    }

    public void addRender(long time) {
        while (this.fpsGraph.queue.size() >= 64) {
            this.fpsGraph.queue.poll();
        }

        this.fpsGraph.queue.add(time);
    }

    public void addUpdate(long time) {
        this.upsGraph.add(time);
    }

    public void addLighting(long time) {
        while (this.lpsGraph.queue.size() >= 64) {
            this.lpsGraph.queue.poll();
        }

        this.lpsGraph.queue.add(time);
    }

    public void addUI(long time) {
        this.uiGraph.add(time);
    }

    @Override
    public void update() {
        if (this.isVisible()) {
            this.setHeight(PZMath.max(108, TextManager.instance.getFontHeight(UIFont.Small) * 4));
            this.setWidth(232.0);
            this.setX(20.0);
            this.setY(Core.getInstance().getScreenHeight() - 20 - this.getHeight());
            super.update();
        }
    }

    @Override
    public void render() {
        if (this.isVisible()) {
            if (UIManager.visibleAllUi) {
                int HEIGHT = this.getHeight().intValue() - 4;
                int bar = -1;
                if (this.isMouseOver()) {
                    this.DrawTextureScaledCol(UIElement.white, 0.0, 0.0, this.getWidth(), this.getHeight(), 0.0, 0.2F, 0.0, 0.5);
                    int lx = Mouse.getXA() - this.getAbsoluteX().intValue();
                    bar = lx / 8;
                }

                int fontHgt = TextManager.instance.getFontHeight(UIFont.Small);
                int textY = (int)(this.getHeight() - fontHgt * 4) / 2;
                this.fpsGraph.flushQueue();
                this.fpsGraph.render(0.0F, 1.0F, 0.0F);
                if (bar >= 0 && bar < this.fpsGraph.bars.size()) {
                    this.DrawText("FPS: " + this.fpsGraph.bars.get(bar), 20.0, textY + fontHgt, 0.0, 1.0, 0.0, 1.0);
                }

                this.lpsGraph.flushQueue();
                this.lpsGraph.render(1.0F, 1.0F, 0.0F);
                if (bar >= 0 && bar < this.lpsGraph.bars.size()) {
                    this.DrawText("LPS: " + this.lpsGraph.bars.get(bar), 20.0, textY + fontHgt * 2, 1.0, 1.0, 0.0, 1.0);
                }

                this.upsGraph.render(0.0F, 1.0F, 1.0F);
                if (bar >= 0 && bar < this.upsGraph.bars.size()) {
                    this.DrawText("UPS: " + this.upsGraph.bars.get(bar), 20.0, textY + fontHgt * 3, 0.0, 1.0, 1.0, 1.0);
                    this.DrawTextureScaledCol(UIElement.white, bar * 8 + 4, 0.0, 1.0, this.getHeight(), 1.0, 1.0, 1.0, 0.5);
                }

                this.uiGraph.render(1.0F, 0.0F, 1.0F);
                if (bar >= 0 && bar < this.uiGraph.bars.size()) {
                    this.DrawText("UI: " + this.uiGraph.bars.get(bar), 20.0, textY, 1.0, 0.0, 1.0, 1.0);
                }

                long free = Runtime.getRuntime().freeMemory() / 1048576L;
                long total = Runtime.getRuntime().totalMemory() / 1048576L;
                this.DrawText(
                    "Memory (MB): total %d, used %d, free %d, max %d".formatted(total, total - free, free, Runtime.getRuntime().maxMemory() / 1048576L),
                    0.0,
                    0 - TextManager.instance.getFontHeight(UIFont.Small),
                    1.0,
                    1.0,
                    1.0,
                    1.0
                );
            }
        }
    }

    private final class Graph {
        private final TLongArrayList times;
        private final BoundedQueue<Long> times2;
        private final TIntArrayList bars;
        private final ConcurrentLinkedQueue<Long> queue;

        private Graph() {
            Objects.requireNonNull(FPSGraph.this);
            super();
            this.times = new TLongArrayList();
            this.times2 = new BoundedQueue<>(300);
            this.bars = new TIntArrayList();
            this.queue = new ConcurrentLinkedQueue<>();
        }

        void flushQueue() {
            for (Long ms = this.queue.poll(); ms != null; ms = this.queue.poll()) {
                this.add(ms);
            }
        }

        public void add(long time) {
            this.times.add(time);
            this.bars.resetQuick();
            long start = this.times.get(0);
            int count = 1;

            for (int i = 1; i < this.times.size(); i++) {
                if (i != this.times.size() - 1 && this.times.get(i) - start <= 1000L) {
                    count++;
                } else {
                    long noUpdates = (this.times.get(i) - start) / 1000L - 1L;

                    for (int j = 0; j < noUpdates; j++) {
                        this.bars.add(0);
                    }

                    this.bars.add(count);
                    count = 1;
                    start = this.times.get(i);
                }
            }

            while (this.bars.size() > 30) {
                int numTimes = this.bars.get(0);

                for (int ix = 0; ix < numTimes; ix++) {
                    this.times.removeAt(0);
                }

                this.bars.removeAt(0);
            }

            this.times2.add(time);
        }

        public void render(float r, float g, float b) {
            if (!this.bars.isEmpty()) {
                float HEIGHT = FPSGraph.this.getHeight().intValue() - 4;
                float BOTTOM = FPSGraph.this.getHeight().intValue() - 2;
                int fps = Math.max(PerformanceSettings.getLockFPS(), PerformanceSettings.lightingFps);
                int barX = 8;
                float lastBarHeight = HEIGHT * ((float)Math.min(fps, this.bars.get(0)) / fps);

                for (int i = 1; i < this.bars.size() - 1; i++) {
                    float barHeight = HEIGHT * ((float)Math.min(fps, this.bars.get(i)) / fps);
                    SpriteRenderer.instance
                        .renderline(
                            null,
                            FPSGraph.this.getAbsoluteX().intValue() + barX - 8 + 4,
                            FPSGraph.this.getAbsoluteY().intValue() + (int)(BOTTOM - lastBarHeight),
                            FPSGraph.this.getAbsoluteX().intValue() + barX + 4,
                            FPSGraph.this.getAbsoluteY().intValue() + (int)(BOTTOM - barHeight),
                            r,
                            g,
                            b,
                            0.35F,
                            1.0F
                        );
                    barX += 8;
                    lastBarHeight = barHeight;
                }
            }
        }

        public void renderFrameTimes(float r, float g, float b) {
            if (!this.times2.isEmpty()) {
                int barX = 0;
                int NUM_BARS = (int)(Core.getInstance().getScreenWidth() - FPSGraph.this.getAbsoluteX() * 2.0) / 8;
                NUM_BARS = PZMath.min(NUM_BARS, this.times2.size());

                for (int i = 0; i < NUM_BARS - 1; i++) {
                    long elapsed = this.times2.get(this.times2.size() - NUM_BARS + i + 1) - this.times2.get(this.times2.size() - NUM_BARS + i);
                    float barHeight = (float)(elapsed * 10L);
                    SpriteRenderer.instance
                        .renderi(
                            null,
                            FPSGraph.this.getAbsoluteX().intValue() + barX,
                            FPSGraph.this.getAbsoluteY().intValue() + FPSGraph.this.getHeight().intValue() - (int)barHeight,
                            8,
                            (int)barHeight,
                            r,
                            g,
                            b,
                            0.35F,
                            null
                        );
                    barX += 8;
                }

                float dy = 1000.0F / PerformanceSettings.getLockFPS() * 10.0F;
                SpriteRenderer.instance
                    .render(
                        null,
                        FPSGraph.this.getAbsoluteX().intValue(),
                        (int)(FPSGraph.this.getAbsoluteY() + FPSGraph.this.getHeight() - dy),
                        Core.getInstance().getScreenWidth(),
                        2.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F,
                        null
                    );
            }
        }
    }
}
