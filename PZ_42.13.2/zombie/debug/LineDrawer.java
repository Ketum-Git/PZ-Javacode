// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.IndieGL;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;
import zombie.iso.IsoCamera;
import zombie.iso.IsoUtils;
import zombie.iso.PlayerCamera;
import zombie.iso.Vector2;

public final class LineDrawer {
    private static final ArrayList<LineDrawer.DrawableLine> lines = new ArrayList<>();
    private static final ArrayList<LineDrawer.DrawableLine> alphaDecayingLines = new ArrayList<>();
    private static final ArrayDeque<LineDrawer.DrawableLine> pool = new ArrayDeque<>();
    private static final Vector2 tempo = new Vector2();
    private static final Vector2 tempo2 = new Vector2();

    private static void DrawTexturedRect(Texture tex, float x, float y, float width, float height, int z, float r, float g, float bl) {
        x = (int)x;
        y = (int)y;
        Vector2 a = new Vector2(x, y);
        Vector2 b = new Vector2(x + width, y);
        Vector2 c = new Vector2(x + width, y + height);
        Vector2 d = new Vector2(x, y + height);
        Vector2 at = new Vector2(IsoUtils.XToScreen(a.x, a.y, z, 0), IsoUtils.YToScreen(a.x, a.y, z, 0));
        Vector2 bt = new Vector2(IsoUtils.XToScreen(b.x, b.y, z, 0), IsoUtils.YToScreen(b.x, b.y, z, 0));
        Vector2 ct = new Vector2(IsoUtils.XToScreen(c.x, c.y, z, 0), IsoUtils.YToScreen(c.x, c.y, z, 0));
        Vector2 dt = new Vector2(IsoUtils.XToScreen(d.x, d.y, z, 0), IsoUtils.YToScreen(d.x, d.y, z, 0));
        PlayerCamera camera = IsoCamera.cameras[IsoPlayer.getPlayerIndex()];
        at.x = at.x - camera.offX;
        bt.x = bt.x - camera.offX;
        ct.x = ct.x - camera.offX;
        dt.x = dt.x - camera.offX;
        at.y = at.y - camera.offY;
        bt.y = bt.y - camera.offY;
        ct.y = ct.y - camera.offY;
        dt.y = dt.y - camera.offY;
        float offsetY = -240.0F;
        offsetY -= 128.0F;
        float offsetX = -32.0F;
        at.y -= offsetY;
        bt.y -= offsetY;
        ct.y -= offsetY;
        dt.y -= offsetY;
        at.x -= -32.0F;
        bt.x -= -32.0F;
        ct.x -= -32.0F;
        dt.x -= -32.0F;
        SpriteRenderer.instance
            .renderdebug(tex, at.x, at.y, bt.x, bt.y, ct.x, ct.y, dt.x, dt.y, r, g, bl, 1.0F, r, g, bl, 1.0F, r, g, bl, 1.0F, r, g, bl, 1.0F, null);
    }

    static void DrawIsoLine(float x, float y, float x2, float y2, float r, float g, float b, float a, int thickness) {
        tempo.set(x, y);
        tempo2.set(x2, y2);
        Vector2 at = new Vector2(IsoUtils.XToScreen(tempo.x, tempo.y, 0.0F, 0), IsoUtils.YToScreen(tempo.x, tempo.y, 0.0F, 0));
        Vector2 bt = new Vector2(IsoUtils.XToScreen(tempo2.x, tempo2.y, 0.0F, 0), IsoUtils.YToScreen(tempo2.x, tempo2.y, 0.0F, 0));
        at.x = at.x - IsoCamera.getOffX();
        bt.x = bt.x - IsoCamera.getOffX();
        at.y = at.y - IsoCamera.getOffY();
        bt.y = bt.y - IsoCamera.getOffY();
        drawLine(at.x, at.y, bt.x, bt.y, r, g, b, a, thickness);
    }

    public static void DrawRect(float x, float y, float z, float width, float height, float r, float g, float b, float a, int thickness) {
        DrawIsoLine(x - width, y - width, z, x + width, y - width, z, r, g, b, a, thickness);
        DrawIsoLine(x - width, y + width, z, x + width, y + width, z, r, g, b, a, thickness);
        DrawIsoLine(x + width, y - width, z, x + width, y + width, z, r, g, b, a, thickness);
        DrawIsoLine(x - width, y - width, z, x - width, y + width, z, r, g, b, a, thickness);
        DrawIsoLine(x - width, y - width, z + height, x + width, y - width, z + height, r, g, b, a, thickness);
        DrawIsoLine(x - width, y + width, z + height, x + width, y + width, z + height, r, g, b, a, thickness);
        DrawIsoLine(x + width, y - width, z + height, x + width, y + width, z + height, r, g, b, a, thickness);
        DrawIsoLine(x - width, y - width, z + height, x - width, y + width, z + height, r, g, b, a, thickness);
        DrawIsoLine(x - width, y - width, z, x - width, y - width, z + height, r, g, b, a, thickness);
        DrawIsoLine(x + width, y - width, z, x + width, y - width, z + height, r, g, b, a, thickness);
        DrawIsoLine(x - width, y + width, z, x - width, y + width, z + height, r, g, b, a, thickness);
        DrawIsoLine(x + width, y + width, z, x + width, y + width, z + height, r, g, b, a, thickness);
    }

    public static void DrawIsoRect(float x, float y, float width, float height, int z, float r, float g, float bl) {
        DrawIsoRect(x, y, width, height, z, 0, r, g, bl);
    }

    public static void DrawIsoRect(float x, float y, float width, float height, int z, int yPixelOffset, float r, float g, float bl) {
        if (width < 0.0F) {
            width = -width;
            x -= width;
        }

        if (height < 0.0F) {
            height = -height;
            y -= height;
        }

        float ax = IsoUtils.XToScreenExact(x, y, z, 0);
        float ay = IsoUtils.YToScreenExact(x, y, z, 0);
        float bx = IsoUtils.XToScreenExact(x + width, y, z, 0);
        float by = IsoUtils.YToScreenExact(x + width, y, z, 0);
        float cx = IsoUtils.XToScreenExact(x + width, y + height, z, 0);
        float cy = IsoUtils.YToScreenExact(x + width, y + height, z, 0);
        float dx = IsoUtils.XToScreenExact(x, y + height, z, 0);
        float dy = IsoUtils.YToScreenExact(x, y + height, z, 0);
        float F = -yPixelOffset * Core.tileScale;
        ay += F;
        by += F;
        cy += F;
        dy += F;
        int playerIndex = IsoCamera.frameState.playerIndex;
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        float zoom = camera.zoom;
        ax += camera.fixJigglyModelsX * zoom;
        ay += camera.fixJigglyModelsY * zoom;
        bx += camera.fixJigglyModelsX * zoom;
        by += camera.fixJigglyModelsY * zoom;
        cx += camera.fixJigglyModelsX * zoom;
        cy += camera.fixJigglyModelsY * zoom;
        dx += camera.fixJigglyModelsX * zoom;
        dy += camera.fixJigglyModelsY * zoom;
        drawLine(ax, ay, bx, by, r, g, bl);
        drawLine(bx, by, cx, cy, r, g, bl);
        drawLine(cx, cy, dx, dy, r, g, bl);
        drawLine(dx, dy, ax, ay, r, g, bl);
    }

    public static void DrawIsoRectRotated(float x, float y, float z, float w, float h, float angleRadians, float r, float g, float b, float a) {
        Vector2 vec = tempo.setLengthAndDirection(angleRadians, 1.0F);
        Vector2 vec2 = tempo2.set(vec);
        vec2.tangent();
        vec.x *= h / 2.0F;
        vec.y *= h / 2.0F;
        vec2.x *= w / 2.0F;
        vec2.y *= w / 2.0F;
        float fx = x + vec.x;
        float fy = y + vec.y;
        float bx = x - vec.x;
        float by = y - vec.y;
        float fx1 = fx - vec2.x;
        float fy1 = fy - vec2.y;
        float fx2 = fx + vec2.x;
        float fy2 = fy + vec2.y;
        float bx1 = bx - vec2.x;
        float by1 = by - vec2.y;
        float bx2 = bx + vec2.x;
        float by2 = by + vec2.y;
        int thickness = 1;
        DrawIsoLine(fx1, fy1, z, fx2, fy2, z, r, g, b, a, 1);
        DrawIsoLine(fx1, fy1, z, bx1, by1, z, r, g, b, a, 1);
        DrawIsoLine(fx2, fy2, z, bx2, by2, z, r, g, b, a, 1);
        DrawIsoLine(bx1, by1, z, bx2, by2, z, r, g, b, a, 1);
    }

    public static void DrawIsoLine(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, float a, int thickness) {
        float sx = IsoUtils.XToScreenExact(x, y, z, 0);
        float sy = IsoUtils.YToScreenExact(x, y, z, 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, z2, 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, z2, 0);
        int playerIndex = IsoCamera.frameState.playerIndex;
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        float zoom = camera.zoom;
        sx += camera.fixJigglyModelsX * zoom;
        sy += camera.fixJigglyModelsY * zoom;
        sx2 += camera.fixJigglyModelsX * zoom;
        sy2 += camera.fixJigglyModelsY * zoom;
        drawLine(sx, sy, sx2, sy2, r, g, b, a, thickness);
    }

    public static void DrawIsoLine(
        float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, float a, float baseThickness, float topThickness
    ) {
        float sx = IsoUtils.XToScreenExact(x, y, z, 0);
        float sy = IsoUtils.YToScreenExact(x, y, z, 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, z2, 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, z2, 0);
        int playerIndex = IsoCamera.frameState.playerIndex;
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        float zoom = camera.zoom;
        sx += camera.fixJigglyModelsX * zoom;
        sy += camera.fixJigglyModelsY * zoom;
        sx2 += camera.fixJigglyModelsX * zoom;
        sy2 += camera.fixJigglyModelsY * zoom;
        drawLine(sx, sy, sx2, sy2, r, g, b, a, baseThickness, topThickness);
    }

    public static void DrawIsoTransform(float px, float py, float z, float rx, float ry, float radius, int segments, float r, float g, float b, float a, int t) {
        DrawIsoCircle(px, py, z, radius, segments, r, g, b, a);
        DrawIsoLine(px, py, z, px + rx + radius / 2.0F, py + ry + radius / 2.0F, z, r, g, b, a, t);
    }

    public static void DrawIsoCircle(float x, float y, float z, float radius, float r, float g, float b, float a) {
        int segments = 16;
        DrawIsoCircle(x, y, z, radius, 16, r, g, b, a);
    }

    public static void DrawIsoCircle(float x, float y, float z, float radius, int segments, float r, float g, float b, float a) {
        double lx = x + radius * Math.cos(Math.toRadians(0.0 / segments));
        double ly = y + radius * Math.sin(Math.toRadians(0.0 / segments));

        for (int i = 1; i <= segments; i++) {
            double cx = x + radius * Math.cos(Math.toRadians(i * 360.0 / segments));
            double cy = y + radius * Math.sin(Math.toRadians(i * 360.0 / segments));
            addLine((float)lx, (float)ly, z, (float)cx, (float)cy, z, r, g, b, a);
            lx = cx;
            ly = cy;
        }
    }

    static void drawLine(float x, float y, float x2, float y2, float r, float g, float b) {
        SpriteRenderer.instance.renderlinef(null, x, y, x2, y2, r, g, b, 1.0F, 1);
    }

    public static void drawLine(float x, float y, float x2, float y2, float r, float g, float b, float a, int thickness) {
        SpriteRenderer.instance.renderlinef(null, x, y, x2, y2, r, g, b, a, thickness);
    }

    public static void drawLine(float x, float y, float x2, float y2, float r, float g, float b, float a, float baseThickness, float topThickness) {
        SpriteRenderer.instance.renderlinef(null, x, y, x2, y2, r, g, b, a, baseThickness, topThickness);
    }

    public static void drawRect(float x, float y, float width, float height, float r, float g, float b, float a, int thickness) {
        SpriteRenderer.instance.render(null, x, y + thickness, thickness, height - thickness * 2, r, g, b, a, null);
        SpriteRenderer.instance.render(null, x, y, width, thickness, r, g, b, a, null);
        SpriteRenderer.instance.render(null, x + width - thickness, y + thickness, thickness, height - thickness * 2, r, g, b, a, null);
        SpriteRenderer.instance.render(null, x, y + height - thickness, width, thickness, r, g, b, a, null);
    }

    public static void drawArc(float cx, float cy, float cz, float radius, float direction, float angle, int segments, float r, float g, float b, float a) {
        float startAngleRadians = direction + (float)Math.acos(angle);
        float endAngleRadians = direction - (float)Math.acos(angle);
        float x1 = cx + (float)Math.cos(startAngleRadians) * radius;
        float y1 = cy + (float)Math.sin(startAngleRadians) * radius;

        for (int i = 1; i <= segments; i++) {
            float angleRadians = startAngleRadians + (endAngleRadians - startAngleRadians) * i / segments;
            float x2 = cx + (float)Math.cos(angleRadians) * radius;
            float y2 = cy + (float)Math.sin(angleRadians) * radius;
            DrawIsoLine(x1, y1, cz, x2, y2, cz, r, g, b, a, 1);
            x1 = x2;
            y1 = y2;
        }
    }

    public static void drawCircle(float x, float y, float radius, int segments, float r, float g, float b) {
        double lx = x + radius * Math.cos(Math.toRadians(0.0 / segments));
        double ly = y + radius * Math.sin(Math.toRadians(0.0 / segments));

        for (int i = 1; i <= segments; i++) {
            double cx = x + radius * Math.cos(Math.toRadians(i * 360.0 / segments));
            double cy = y + radius * Math.sin(Math.toRadians(i * 360.0 / segments));
            drawLine((float)lx, (float)ly, (float)cx, (float)cy, r, g, b, 1.0F, 1);
            lx = cx;
            ly = cy;
        }
    }

    public static void drawDirectionLine(float cx, float cy, float cz, float radius, float radians, float r, float g, float b, float a, int thickness) {
        float x2 = cx + (float)Math.cos(radians) * radius;
        float y2 = cy + (float)Math.sin(radians) * radius;
        DrawIsoLine(cx, cy, cz, x2, y2, cz, r, g, b, a, thickness);
    }

    public static void drawDirectionLine(
        float cx, float cy, float cz, float radius, float radians, float r, float g, float b, float a, float baseThickness, float topThickness
    ) {
        float x2 = cx + (float)Math.cos(radians) * radius;
        float y2 = cy + (float)Math.sin(radians) * radius;
        DrawIsoLine(cx, cy, cz, x2, y2, cz, r, g, b, a, baseThickness, topThickness);
    }

    public static void drawDotLines(float cx, float cy, float cz, float radius, float direction, float dot, float r, float g, float b, float a, int thickness) {
        drawDirectionLine(cx, cy, cz, radius, direction + (float)Math.acos(dot), r, g, b, a, thickness);
        drawDirectionLine(cx, cy, cz, radius, direction - (float)Math.acos(dot), r, g, b, a, thickness);
    }

    public static void addAlphaDecayingIsoCircle(float x, float y, float z, float radius, int segments, float r, float g, float b, float a) {
        double lx = x + radius * Math.cos(Math.toRadians(0.0 / segments));
        double ly = y + radius * Math.sin(Math.toRadians(0.0 / segments));

        for (int i = 1; i <= segments; i++) {
            double cx = x + radius * Math.cos(Math.toRadians(i * 360.0 / segments));
            double cy = y + radius * Math.sin(Math.toRadians(i * 360.0 / segments));
            addAlphaDecayingLine((float)lx, (float)ly, z, (float)cx, (float)cy, z, r, g, b, a);
            lx = cx;
            ly = cy;
        }
    }

    public static void addAlphaDecayingLine(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, float a) {
        LineDrawer.DrawableLine line = pool.isEmpty() ? new LineDrawer.DrawableLine() : pool.pop();
        alphaDecayingLines.add(line.init(x, y, z, x2, y2, z2, r, g, b, a));
    }

    public static void addLine(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, boolean bLine) {
        LineDrawer.DrawableLine line = pool.isEmpty() ? new LineDrawer.DrawableLine() : pool.pop();
        alphaDecayingLines.add(line.init(x, y, z, x2, y2, z2, r, g, b, "", bLine));
    }

    public static void addLine(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, float a) {
        LineDrawer.DrawableLine line = pool.isEmpty() ? new LineDrawer.DrawableLine() : pool.pop();
        lines.add(line.init(x, y, z, x2, y2, z2, r, g, b, a));
    }

    public static void addLine(float x, float y, float z, float x2, float y2, float z2, int r, int g, int b, String name) {
        addLine(x, y, z, x2, y2, z2, r, g, b, name, true);
    }

    public static void addLine(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, String name, boolean bLine) {
        LineDrawer.DrawableLine line = pool.isEmpty() ? new LineDrawer.DrawableLine() : pool.pop();
        lines.add(line.init(x, y, z, x2, y2, z2, r, g, b, name, bLine));
    }

    public static void addRect(float x, float y, float z, float width, float height, float r, float g, float b) {
        LineDrawer.DrawableLine line = pool.isEmpty() ? new LineDrawer.DrawableLine() : pool.pop();
        lines.add(line.init(x, y, z, x + width, y + height, z, r, g, b, null, false));
    }

    public static void addRectYOffset(float x, float y, float z, float width, float height, int yOffset, float r, float g, float b) {
        LineDrawer.DrawableLine line = pool.isEmpty() ? new LineDrawer.DrawableLine() : pool.pop();
        lines.add(line.init(x, y, z, x + width, y + height, z, r, g, b, null, false));
        line.yPixelOffset = yOffset;
    }

    public static void clear() {
        if (!lines.isEmpty()) {
            for (int i = 0; i < lines.size(); i++) {
                pool.push(lines.get(i));
            }

            lines.clear();
        }
    }

    public void removeLine(String name) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).name.equals(name)) {
                lines.remove(lines.get(i));
                i--;
            }
        }
    }

    public static void render() {
        if (PerformanceSettings.fboRenderChunk) {
            IndieGL.StartShader(0);
            IndieGL.disableDepthTest();
            IndieGL.glBlendFunc(770, 771);
        }

        for (int n = 0; n < lines.size(); n++) {
            LineDrawer.DrawableLine line = lines.get(n);
            if (!line.line) {
                DrawIsoRect(
                    line.xstart,
                    line.ystart,
                    line.xend - line.xstart,
                    line.yend - line.ystart,
                    (int)line.zstart,
                    line.yPixelOffset,
                    line.red,
                    line.green,
                    line.blue
                );
            } else {
                DrawIsoLine(line.xstart, line.ystart, line.zstart, line.xend, line.yend, line.zend, line.red, line.green, line.blue, line.alpha, 1);
            }
        }

        for (LineDrawer.DrawableLine line : alphaDecayingLines) {
            if (!line.line) {
                DrawIsoRect(
                    line.xstart,
                    line.ystart,
                    line.xend - line.xstart,
                    line.yend - line.ystart,
                    (int)line.zstart,
                    line.yPixelOffset,
                    line.red,
                    line.green,
                    line.blue
                );
            } else {
                DrawIsoLine(line.xstart, line.ystart, line.zstart, line.xend, line.yend, line.zend, line.red, line.green, line.blue, line.alpha, 1);
            }

            line.alpha -= 0.01F;
            if (line.alpha < 0.0F) {
                pool.push(line);
            }
        }

        alphaDecayingLines.removeIf(linex -> linex.alpha < 0.0F);
    }

    public static void drawLines() {
        clear();
    }

    public static class DrawableLine {
        public boolean line;
        private String name;
        private float red;
        private float green;
        private float blue;
        private float alpha;
        private float xstart;
        private float ystart;
        private float zstart;
        private float xend;
        private float yend;
        private float zend;
        private int yPixelOffset;

        public LineDrawer.DrawableLine init(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, String name) {
            this.xstart = x;
            this.ystart = y;
            this.zstart = z;
            this.xend = x2;
            this.yend = y2;
            this.zend = z2;
            this.red = r;
            this.green = g;
            this.blue = b;
            this.alpha = 1.0F;
            this.name = name;
            this.yPixelOffset = 0;
            return this;
        }

        public LineDrawer.DrawableLine init(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, String name, boolean bLine) {
            this.xstart = x;
            this.ystart = y;
            this.zstart = z;
            this.xend = x2;
            this.yend = y2;
            this.zend = z2;
            this.red = r;
            this.green = g;
            this.blue = b;
            this.alpha = 1.0F;
            this.name = name;
            this.line = bLine;
            this.yPixelOffset = 0;
            return this;
        }

        public LineDrawer.DrawableLine init(float x, float y, float z, float x2, float y2, float z2, float r, float g, float b, float a) {
            this.xstart = x;
            this.ystart = y;
            this.zstart = z;
            this.xend = x2;
            this.yend = y2;
            this.zend = z2;
            this.red = r;
            this.green = g;
            this.blue = b;
            this.alpha = a;
            this.name = null;
            this.line = true;
            this.yPixelOffset = 0;
            return this;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof LineDrawer.DrawableLine drawableLine ? drawableLine.name.equals(this.name) : o.equals(this);
        }
    }
}
