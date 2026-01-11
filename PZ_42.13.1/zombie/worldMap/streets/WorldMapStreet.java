// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.streets;

import gnu.trove.list.array.TFloatArrayList;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.joml.Vector2f;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.math.PZMath;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoUtils;
import zombie.popman.ObjectPool;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.Clipper;
import zombie.vehicles.ClipperOffset;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.styles.WorldMapStyleLayer;
import zombie.worldMap.styles.WorldMapTextStyleLayer;

public final class WorldMapStreet {
    static final ObjectPool<WorldMapStreet> s_pool = new ObjectPool<>(WorldMapStreet::new);
    static final ObjectPool<StreetPoints> s_pointsPool = new ObjectPool<>(StreetPoints::new);
    private static final ClosestPoint s_closestPoint = new ClosestPoint();
    private static final PointOn s_pointOn = new PointOn();
    private static final StreetPoints s_clipped = new StreetPoints();
    private static final StreetPoints s_reverse = new StreetPoints();
    static final ObjectPool<CharLayout> s_charLayoutPool = new ObjectPool<>(CharLayout::new);
    private static final ArrayList<CharLayout> s_layout = new ArrayList<>();
    private static final TFloatArrayList triangles2 = new TFloatArrayList();
    private static double cosA;
    private static double sinA;
    private static double x;
    private static double y;
    private static double xAlongLine;
    private static final int GAP = 200;
    private static double fontScale;
    private static float sdfThreshold;
    private static float shadow;
    private static float outlineThickness;
    private static float outlineR;
    private static float outlineG;
    private static float outlineB;
    private static float outlineA;
    private WorldMapStreets owner;
    private String translatedText;
    private StreetPoints points;
    private int width = 5;
    private final ArrayList<Intersection> intersections = new ArrayList<>();
    WorldMapStreet connectedToStart;
    WorldMapStreet connectedToEnd;
    final ArrayList<WorldMapStreet> splitStreets = new ArrayList<>();
    long changeCount;
    static final WorldMapStreet.LayoutCounts countsForward = new WorldMapStreet.LayoutCounts();
    static final WorldMapStreet.LayoutCounts countsBackward = new WorldMapStreet.LayoutCounts();
    static Clipper clipper;

    private WorldMapStreet() {
    }

    public WorldMapStreet(WorldMapStreets owner, String translatedText, StreetPoints points) {
        this.owner = owner;
        this.translatedText = translatedText;
        this.points = points;
        this.points.calculateBounds();
    }

    WorldMapStreet init(WorldMapStreets owner, String translatedText, StreetPoints points) {
        this.owner = owner;
        this.translatedText = translatedText;
        if (this.points != null) {
            s_pointsPool.release(this.points);
        }

        this.points = points;
        this.points.calculateBounds();
        this.intersections.clear();
        this.splitStreets.clear();
        this.changeCount = 0L;
        return this;
    }

    public WorldMapStreets getOwner() {
        return this.owner;
    }

    public float getMinX() {
        return this.points.getMinX();
    }

    public float getMinY() {
        return this.points.getMinY();
    }

    public float getMaxX() {
        return this.points.getMaxX();
    }

    public float getMaxY() {
        return this.points.getMaxY();
    }

    public int getNumPoints() {
        return this.points.numPoints();
    }

    public float getPointX(int index) {
        return this.points.getX(index);
    }

    public float getPointY(int index) {
        return this.points.getY(index);
    }

    public float getLength(UIWorldMap ui) {
        return this.points.calculateLength(ui);
    }

    public float getLengthSquared(UIWorldMap ui) {
        float length = this.getLength(ui);
        return length * length;
    }

    public void addPoint(float x, float y) {
        this.points.add(x);
        this.points.add(y);
        this.points.invalidateBounds();
    }

    public void insertPoint(int index, float x, float y) {
        this.points.insert(index * 2, x);
        this.points.insert(index * 2 + 1, y);
        this.points.invalidateBounds();
    }

    public void removePoint(int index) {
        this.points.removeAt(index * 2);
        this.points.removeAt(index * 2);
        this.points.invalidateBounds();
    }

    public void setPoint(int index, float x, float y) {
        this.points.set(index * 2, x);
        this.points.set(index * 2 + 1, y);
        this.points.invalidateBounds();
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getWidth() {
        return this.width;
    }

    public void reverseDirection() {
        this.points.reverse();
    }

    public String getTranslatedText() {
        return this.translatedText;
    }

    public void setTranslatedText(String text) {
        this.translatedText = StringUtils.isNullOrWhitespace(text) ? "" : text;
    }

    public UIFont getFont(UIWorldMap ui) {
        WorldMapStyle style = ui.getAPI().getStyle();
        WorldMapTextStyleLayer textLayer = style.getTextStyleLayerOrDefault("text-street");
        return textLayer.getFont();
    }

    public double getFontScale(UIWorldMap ui) {
        float worldScale = ui.getAPI().getWorldScale();
        worldScale = PZMath.clamp(worldScale, 1.9805F, 3.66F);
        WorldMapStyle style = ui.getAPI().getStyle();
        WorldMapTextStyleLayer textLayer = style.getTextStyleLayerOrDefault("text-street");
        worldScale *= textLayer.calculateScale(ui);
        return worldScale * 0.2;
    }

    public ArrayList<Intersection> getIntersections() {
        return this.intersections;
    }

    public void resetIntersectionRenderFlag() {
        for (int i = 0; i < this.getIntersections().size(); i++) {
            Intersection intersection = this.getIntersections().get(i);
            intersection.renderedLabelOver = false;
        }
    }

    boolean setIntersectionRenderFlag(UIWorldMap ui, float start, float end) {
        boolean set = false;

        for (int i = 0; i < this.getIntersections().size(); i++) {
            Intersection intersection = this.getIntersections().get(i);
            if (intersection.getDistanceFromStart(ui, this) >= start - 10.0F && intersection.getDistanceFromStart(ui, this) <= end + 10.0F) {
                intersection.renderedLabelOver = true;
                set = true;
            }
        }

        return set;
    }

    Intersection getMatchingIntersection(WorldMapStreet street, float worldX, float worldY) {
        for (int i = 0; i < this.getIntersections().size(); i++) {
            Intersection intersection = this.getIntersections().get(i);
            if ((street == null || intersection.street == street)
                && PZMath.equal(intersection.worldX, worldX, 0.01F)
                && PZMath.equal(intersection.worldY, worldY)) {
                return intersection;
            }
        }

        return null;
    }

    boolean overlapsAnotherLabel(UIWorldMap ui, float start, float end) {
        for (int i = 0; i < this.getIntersections().size(); i++) {
            Intersection intersection = this.getIntersections().get(i);
            if (intersection.getDistanceFromStart(ui, this) >= start && intersection.getDistanceFromStart(ui, this) <= end) {
                Intersection intersectionOther = intersection.street.getMatchingIntersection(null, intersection.worldX, intersection.worldY);
                if (intersectionOther != null && intersectionOther.renderedLabelOver) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean getPointOn(UIWorldMap ui, float t, PointOn pointOn) {
        t = PZMath.clamp_01(t);
        pointOn.x = pointOn.y = 0.0F;
        pointOn.worldX = pointOn.worldY = 0.0F;
        pointOn.segment = -1;
        float length = this.getLength(ui);
        if (length <= 0.0F) {
            return false;
        } else {
            float distanceFromStart = length * t;
            float segmentStart = 0.0F;

            for (int i = 0; i < this.getNumPoints() - 1; i++) {
                float worldX1 = this.points.getX(i);
                float worldY1 = this.points.getY(i);
                float worldX2 = this.points.getX(i + 1);
                float worldY2 = this.points.getY(i + 1);
                float x1 = ui.getAPI().worldToUIX(worldX1, worldY1);
                float y1 = ui.getAPI().worldToUIY(worldX1, worldY1);
                float x2 = ui.getAPI().worldToUIX(worldX2, worldY2);
                float y2 = ui.getAPI().worldToUIY(worldX2, worldY2);
                float segmentLength = Vector2f.length(x2 - x1, y2 - y1);
                if (segmentStart + segmentLength >= distanceFromStart) {
                    float f = (distanceFromStart - segmentStart) / segmentLength;
                    pointOn.x = x1 + (x2 - x1) * f;
                    pointOn.y = y1 + (y2 - y1) * f;
                    pointOn.worldX = worldX1 + (worldX2 - worldX1) * f;
                    pointOn.worldY = worldY1 + (worldY2 - worldY1) * f;
                    pointOn.segment = i;
                    pointOn.distFromSegmentStart = distanceFromStart - segmentStart;
                    pointOn.distanceFromStart = distanceFromStart;
                    return true;
                }

                segmentStart += segmentLength;
            }

            return false;
        }
    }

    @Deprecated
    void splitAtOccludedIntersections(ArrayList<WorldMapStreet> split) {
        StreetPoints points1 = s_reverse;
        points1.clear();
        points1.addAll(this.points);

        for (int i = this.getIntersections().size() - 1; i >= 0; i--) {
            Intersection intersection = this.getIntersections().get(i);
            Intersection intersectionOther = intersection.street.getMatchingIntersection(intersection.street, intersection.worldX, intersection.worldY);
            if (intersectionOther != null && intersectionOther.renderedLabelOver) {
                WorldMapStreet street2 = s_pool.alloc();
                StreetPoints points2 = street2.points == null ? new StreetPoints() : street2.points;
                points2.clear();
                points2.add(intersection.worldX);
                points2.add(intersection.worldY);

                for (int j = (intersection.segment + 1) * 2; j < points1.size(); j++) {
                    points2.add(points1.get(j));
                }

                street2.init(this.owner, this.getTranslatedText(), points2);
                split.add(0, street2);
                points1.remove((intersection.segment + 1) * 2, points1.size() - (intersection.segment + 1) * 2);
                points1.add(intersection.worldX);
                points1.add(intersection.worldY);
            }
        }

        if (!points1.isEmpty() && !split.isEmpty()) {
            WorldMapStreet street2 = s_pool.alloc();
            StreetPoints points2 = street2.points == null ? new StreetPoints() : street2.points;
            points2.clear();
            points2.addAll(points1);
            street2.init(this.owner, this.getTranslatedText(), points2);
            split.add(0, street2);
        }
    }

    public void render(UIWorldMap ui, StreetRenderData renderData) {
        if (this.splitStreets.isEmpty()) {
            this.render2(ui, null, renderData);
        } else {
            for (int i = 0; i < this.splitStreets.size(); i++) {
                WorldMapStreet street = this.splitStreets.get(i);
                street.render2(ui, this, renderData);
            }
        }

        if (ui.getAPI().getBoolean("OutlineStreets")) {
            this.createPolygon(renderData.polygon);

            for (int i = 0; i < renderData.polygon.size() / 2; i++) {
                float x1 = renderData.polygon.get(i * 2);
                float y1 = renderData.polygon.get(i * 2 + 1);
                int j = (i + 1) % (renderData.polygon.size() / 2);
                float x2 = renderData.polygon.get(j * 2);
                float y2 = renderData.polygon.get(j * 2 + 1);
                this.renderLine(ui, x1, y1, x2, y2, 0.0F, 0.0F, 1.0F, 1.0F, 1, renderData);
            }
        }
    }

    float getDistanceAlongOriginalStreet(UIWorldMap ui, float t, WorldMapStreet originalStreet) {
        this.getPointOn(ui, t, s_pointOn);
        originalStreet.getClosestPointOn(ui, s_pointOn.x, s_pointOn.y, s_closestPoint);
        return s_closestPoint.distanceFromStart;
    }

    void render2(UIWorldMap ui, WorldMapStreet originalStreet, StreetRenderData renderData) {
        UIFont uiFont = this.getFont(ui);
        AngelCodeFont font = TextManager.instance.getFontFromEnum(uiFont);
        float streetLength = this.getLength(ui);
        fontScale = this.getFontScale(ui);
        double textWidth = font.getWidth(this.getTranslatedText()) * fontScale;
        if (!(textWidth > streetLength)) {
            float lx1 = 0.0F;
            float ly1 = 0.0F;
            float lx2 = 10.0F;
            float ly2 = 0.0F;
            double radian = (float)Math.atan2(-0.0, 10.0);
            cosA = Math.cos(radian);
            sinA = Math.sin(radian);
            sdfThreshold = this.getSdfThreshold(ui);
            shadow = 0.0F;
            outlineThickness = 0.25F;
            outlineR = 0.0F;
            outlineG = 0.0F;
            outlineB = 0.0F;
            outlineA = 1.0F;
            if (!TextManager.instance.isSdf(uiFont)) {
                outlineThickness = 0.0F;
                outlineA = 0.0F;
            }

            this.render2b(ui, originalStreet, renderData, (float)textWidth, streetLength, 0.0F, 1.0F);
        }
    }

    void render2b(
        UIWorldMap ui,
        WorldMapStreet originalStreet,
        StreetRenderData renderData,
        float textWidth,
        float streetLength,
        float startFractionWS,
        float endFractionWS
    ) {
        float middleFraction = startFractionWS + (endFractionWS - startFractionWS) / 2.0F;
        this.getPointOn(ui, middleFraction, s_pointOn);
        double xAlongStreet = s_pointOn.distanceFromStart - textWidth / 2.0F;
        float minX = s_pointOn.x - textWidth / 2.0F;
        float minY = s_pointOn.y - textWidth / 2.0F;
        float maxX = s_pointOn.x + textWidth / 2.0F;
        float maxY = s_pointOn.y + textWidth / 2.0F;
        boolean bRendered = false;
        boolean bOnScreen = minX < ui.getWidth() && maxX > 0.0F && minY < ui.getHeight() && maxY > 0.0F;
        if (!bOnScreen) {
            boolean middleLeft = true;
        } else if (originalStreet != null) {
            float start = this.getDistanceAlongOriginalStreet(ui, (float)xAlongStreet / streetLength, originalStreet);
            float end = this.getDistanceAlongOriginalStreet(ui, (float)(xAlongStreet + textWidth) / streetLength, originalStreet);
            if (originalStreet.overlapsAnotherLabel(ui, start, end)) {
                this.renderSection(
                    ui, (float)xAlongStreet / streetLength, (float)(xAlongStreet + textWidth) / streetLength, 1.0F, 0.0F, 0.0F, 0.5F, 3, renderData
                );
            } else if (this.shouldRenderForward(ui, textWidth, (float)xAlongStreet, streetLength)) {
                if (originalStreet.setIntersectionRenderFlag(ui, start, end)) {
                    this.renderSection(
                        ui, (float)xAlongStreet / streetLength, (float)(xAlongStreet + textWidth) / streetLength, 0.0F, 1.0F, 0.0F, 0.75F, 3, renderData
                    );
                }

                this.renderForward(ui, textWidth, (float)xAlongStreet, streetLength, renderData);
                bRendered = true;
            } else {
                if (originalStreet.setIntersectionRenderFlag(ui, start, end)) {
                    this.renderSection(
                        ui, (float)xAlongStreet / streetLength, (float)(xAlongStreet + textWidth) / streetLength, 0.0F, 1.0F, 0.0F, 0.75F, 3, renderData
                    );
                }

                this.renderBackward(ui, textWidth, (float)xAlongStreet, streetLength, renderData);
                bRendered = true;
            }
        } else if (this.overlapsAnotherLabel(ui, (float)xAlongStreet, (float)(xAlongStreet + textWidth))) {
            this.renderSection(ui, (float)xAlongStreet / streetLength, (float)(xAlongStreet + textWidth) / streetLength, 1.0F, 0.0F, 0.0F, 0.5F, 3, renderData);
        } else if (this.shouldRenderForward(ui, textWidth, (float)xAlongStreet, streetLength)) {
            if (this.setIntersectionRenderFlag(ui, (float)xAlongStreet, (float)(xAlongStreet + textWidth))) {
                this.renderSection(
                    ui, (float)xAlongStreet / streetLength, (float)(xAlongStreet + textWidth) / streetLength, 0.0F, 1.0F, 0.0F, 0.75F, 3, renderData
                );
            }

            this.renderForward(ui, textWidth, (float)xAlongStreet, streetLength, renderData);
            bRendered = true;
        } else {
            if (this.setIntersectionRenderFlag(ui, (float)xAlongStreet, (float)(xAlongStreet + textWidth))) {
                this.renderSection(
                    ui, (float)xAlongStreet / streetLength, (float)(xAlongStreet + textWidth) / streetLength, 0.0F, 1.0F, 0.0F, 0.75F, 3, renderData
                );
            }

            this.renderBackward(ui, textWidth, (float)xAlongStreet, streetLength, renderData);
            bRendered = true;
        }

        float middleLeft = startFractionWS + (middleFraction - startFractionWS) / 2.0F;
        if (!bRendered && bOnScreen) {
            if (middleFraction * streetLength >= middleLeft * streetLength + textWidth / 2.0F + 200.0F) {
                this.render2b(ui, originalStreet, renderData, textWidth, streetLength, startFractionWS, middleFraction);
                this.render2b(ui, originalStreet, renderData, textWidth, streetLength, middleFraction, endFractionWS);
            }
        } else if (middleFraction * streetLength - textWidth / 2.0F >= middleLeft * streetLength + textWidth / 2.0F + 200.0F) {
            this.render2b(ui, originalStreet, renderData, textWidth, streetLength, startFractionWS, middleFraction);
            this.render2b(ui, originalStreet, renderData, textWidth, streetLength, middleFraction, endFractionWS);
        }
    }

    void renderSection(UIWorldMap ui, float start, float end, float r, float g, float b, float a, int thickness, StreetRenderData renderData) {
    }

    public void renderLines(UIWorldMap ui, float r, float g, float b, float a, int thickness, StreetRenderData renderData) {
        for (int i = 0; i < this.getNumPoints() - 1; i++) {
            float x1 = this.points.getX(i);
            float y1 = this.points.getY(i);
            float x2 = this.points.getX(i + 1);
            float y2 = this.points.getY(i + 1);
            this.renderLine(ui, x1, y1, x2, y2, r, g, b, a, thickness, renderData);
        }
    }

    void renderLine(
        UIWorldMap ui,
        float worldX1,
        float worldY1,
        float worldX2,
        float worldY2,
        float r,
        float g,
        float b,
        float a,
        int thickness,
        StreetRenderData renderData
    ) {
        float uiX1 = ui.getAPI().worldToUIX(worldX1, worldY1);
        float uiY1 = ui.getAPI().worldToUIY(worldX1, worldY1);
        float uiX2 = ui.getAPI().worldToUIX(worldX2, worldY2);
        float uiY2 = ui.getAPI().worldToUIY(worldX2, worldY2);
        if (renderData == null) {
            ui.DrawLine(null, uiX1, uiY1, uiX2, uiY2, thickness, r, g, b, a);
        } else {
            renderData.lines.add(uiX1);
            renderData.lines.add(uiY1);
            renderData.lines.add(uiX2);
            renderData.lines.add(uiY2);
            renderData.lines.add(r);
            renderData.lines.add(g);
            renderData.lines.add(b);
            renderData.lines.add(a);
            renderData.lines.add(thickness);
        }
    }

    public void renderIntersections(UIWorldMap ui, float r, float g, float b, float a) {
        for (Intersection ixn : this.getIntersections()) {
            float x1 = ixn.worldX - 1.0F;
            float y1 = ixn.worldY - 1.0F;
            float x2 = ixn.worldX + 1.0F;
            float y2 = ixn.worldY + 1.0F;
            int thickness = ixn.renderedLabelOver ? 3 : 1;
            this.renderLine(ui, x1, y1, x2, y1, r, g, b, a, thickness, null);
            this.renderLine(ui, x2, y1, x2, y2, r, g, b, a, thickness, null);
            this.renderLine(ui, x2, y2, x1, y2, r, g, b, a, thickness, null);
            this.renderLine(ui, x1, y2, x1, y1, r, g, b, a, thickness, null);
        }
    }

    private boolean shouldRenderForward(UIWorldMap ui, double textWidth, float xAlongStreet, float streetLength) {
        this.layoutForward(ui, xAlongStreet, streetLength);
        this.countUpsideDownCharacters(countsForward);
        boolean bForwardOK = false;
        if (countsForward.upsideDown == 0) {
            return true;
        } else {
            if (countsForward.total == countsForward.up) {
                bForwardOK = true;
            }

            if (countsForward.upsideDown == countsForward.up) {
                bForwardOK = true;
            }

            this.layoutBackward(ui, textWidth, xAlongStreet, streetLength);
            this.countUpsideDownCharacters(countsBackward);
            if (countsBackward.upsideDown == 0) {
                return false;
            } else if (bForwardOK) {
                return true;
            } else {
                return countsBackward.upsideDown == countsBackward.up ? false : countsForward.upsideDown < countsBackward.upsideDown;
            }
        }
    }

    private void countUpsideDownCharacters(WorldMapStreet.LayoutCounts counts) {
        counts.total = counts.upsideDown = counts.up = counts.down = 0;

        for (int i = 0; i < s_layout.size(); i++) {
            CharLayout charLayout = s_layout.get(i);
            double dx = charLayout.rightTop[0] - charLayout.leftTop[0];
            double dy = charLayout.rightTop[1] - charLayout.leftTop[1];
            if (dx != 0.0 || dy != 0.0) {
                counts.total++;
                double radians = (float)Math.atan2(-dy, dx);
                long degrees = Math.round(radians * 180.0F / (float)Math.PI);
                if (degrees < 0L) {
                    degrees += 360L;
                }

                if (degrees == 90L) {
                    counts.up++;
                }

                if (degrees == 270L) {
                    counts.down++;
                }

                boolean bUpright = degrees >= 0L && degrees < 90L || degrees >= 270L;
                if (!bUpright) {
                    counts.upsideDown++;
                }
            }
        }
    }

    public void clipToObscuredCells() {
        s_pool.releaseAll(this.splitStreets);
        this.splitStreets.clear();
        s_clipped.clear();
        if (clipper == null) {
            clipper = new Clipper();
        }

        clipper.clear();
        if (IsoMetaGrid.clipperOffset == null) {
            IsoMetaGrid.clipperOffset = new ClipperOffset();
            IsoMetaGrid.clipperBuffer = ByteBuffer.allocateDirect(3072);
        }

        ByteBuffer bb = IsoMetaGrid.clipperBuffer;
        bb.clear();
        if (this.points.isClockwise()) {
            for (int i = this.getNumPoints() - 1; i >= 0; i--) {
                bb.putFloat(this.getPointX(i));
                bb.putFloat(this.getPointY(i));
            }
        } else {
            for (int i = 0; i < this.getNumPoints(); i++) {
                bb.putFloat(this.getPointX(i));
                bb.putFloat(this.getPointY(i));
            }
        }

        clipper.addPath(this.getNumPoints(), bb, false, false);

        for (int i = 0; i < this.getOwner().obscuredCells.size() / 2; i++) {
            int cellX = this.getOwner().obscuredCells.get(i * 2);
            int cellY = this.getOwner().obscuredCells.get(i * 2 + 1);
            clipper.clipAABB(cellX * 300, cellY * 300, (cellX + 1) * 300, (cellY + 1) * 300);
        }

        int numPolygons = clipper.generatePolygons();

        for (int i = 0; i < numPolygons; i++) {
            bb.clear();
            clipper.getPolygon(i, bb);
            short numPoints = bb.getShort();
            if (numPoints >= 2) {
                StreetPoints points = s_pointsPool.alloc();
                points.clear();

                for (int j = 0; j < numPoints; j++) {
                    float x = bb.getFloat();
                    float y = bb.getFloat();
                    points.add(x, y);
                }

                WorldMapStreet street = s_pool.alloc().init(this.getOwner(), this.getTranslatedText(), points);
                street.setWidth(this.getWidth());
                this.splitStreets.add(street);
            }
        }
    }

    private double layoutForward(UIWorldMap ui, float xAlongStreet, float streetLength) {
        s_charLayoutPool.releaseAll(s_layout);
        s_layout.clear();
        float t = xAlongStreet / streetLength;
        this.getPointOn(ui, t, s_pointOn);
        if (s_pointOn.segment == -1) {
            return xAlongStreet;
        } else {
            int firstChar = 0;
            xAlongLine = s_pointOn.distFromSegmentStart;

            for (int i = s_pointOn.segment; i < this.points.numPoints() - 1; i++) {
                float x1 = this.points.getX(i);
                float y1 = this.points.getY(i);
                float x2 = this.points.getX(i + 1);
                float y2 = this.points.getY(i + 1);
                float uiX1 = ui.getAPI().worldToUIX(x1, y1);
                float uiY1 = ui.getAPI().worldToUIY(x1, y1);
                float uiX2 = ui.getAPI().worldToUIX(x2, y2);
                float uiY2 = ui.getAPI().worldToUIY(x2, y2);
                float lineLength = IsoUtils.DistanceTo(uiX1, uiY1, uiX2, uiY2);

                while (xAlongLine < lineLength) {
                    double xAlongLine1 = xAlongLine;
                    int numCharsRendered = this.layoutTextAlongLine(ui, firstChar, uiX1, uiY1, uiX2, uiY2);
                    if (numCharsRendered < 1) {
                        break;
                    }

                    firstChar += numCharsRendered;
                    xAlongStreet += (float)(xAlongLine - xAlongLine1);
                    if (firstChar >= this.translatedText.length()) {
                        return xAlongStreet;
                    }
                }

                xAlongLine = Math.max(0.0, xAlongLine - lineLength);
            }

            return xAlongStreet;
        }
    }

    private double layoutBackward(UIWorldMap ui, double textWidth, float xAlongStreet, float streetLength) {
        StreetPoints oldPoints = this.points;
        this.points.setReverse(s_reverse);
        this.points = s_reverse;

        double var9;
        try {
            double xAlongStreet1 = this.layoutForward(ui, (float)(streetLength - (xAlongStreet + textWidth)), streetLength);
            var9 = xAlongStreet + textWidth;
        } finally {
            this.points = oldPoints;
        }

        return var9;
    }

    private double renderForward(UIWorldMap ui, float textWidth, float xAlongStreet, float streetLength, StreetRenderData renderData) {
        xAlongStreet = (float)this.layoutForward(ui, xAlongStreet, streetLength);
        PZArrayUtil.addAll(renderData.characters, s_layout);
        s_layout.clear();
        return xAlongStreet;
    }

    private double renderBackward(UIWorldMap ui, double textWidth, float xAlongStreet, float streetLength, StreetRenderData renderData) {
        xAlongStreet = (float)this.layoutBackward(ui, textWidth, xAlongStreet, streetLength);
        PZArrayUtil.addAll(renderData.characters, s_layout);
        s_layout.clear();
        return xAlongStreet;
    }

    private int layoutTextAlongLine(UIWorldMap ui, int firstChar, float lx1, float ly1, float lx2, float ly2) {
        int numCharsRendered = 0;
        WorldMapStreet.x = lx1;
        WorldMapStreet.y = ly1;
        float lineLength = IsoUtils.DistanceTo(lx1, ly1, lx2, ly2);
        double radian = (float)Math.atan2(-(ly2 - ly1), lx2 - lx1);
        cosA = Math.cos(radian);
        sinA = Math.sin(radian);
        WorldMapStyle style = ui.getAPI().getStyle();
        WorldMapTextStyleLayer textLayer = style.getTextStyleLayerOrDefault("text-street");
        WorldMapStyleLayer.RGBAf rgbaf = textLayer.evalColor(ui.getAPI().getZoomF(), textLayer.fill);
        AngelCodeFont font = TextManager.instance.getFontFromEnum(this.getFont(ui));
        AngelCodeFont.CharDef lastCharDef = null;
        double scale = fontScale;
        double x = xAlongLine / scale;
        double y = -font.getHeight("Marvin Place", false, false) / 2.0;
        double textWidth = 0.0;
        double dx = 0.0;
        double dy = 0.0;

        for (int i = firstChar; i < this.translatedText.length(); i++) {
            char ch = this.translatedText.charAt(i);
            if (ch >= font.chars.length) {
                ch = '?';
            }

            AngelCodeFont.CharDef charDef = font.chars[ch];
            if (charDef != null) {
                if (lastCharDef != null) {
                    x += lastCharDef.getKerning(ch);
                }

                if (i > firstChar && (x + charDef.xadvance) * scale >= lineLength) {
                    break;
                }

                if (charDef.width > 0 && charDef.height > 0) {
                    double x0 = x + charDef.xoffset - 0.0;
                    double y0 = y + charDef.yoffset - 0.0;
                    double x1 = x0 + charDef.width;
                    double y1 = y0 + charDef.height;
                    CharLayout charLayout = s_charLayoutPool.alloc();
                    charLayout.charDef = charDef;
                    this.getAbsolutePosition(ui, x0, y0, charLayout.leftTop);
                    this.getAbsolutePosition(ui, x1, y0, charLayout.rightTop);
                    this.getAbsolutePosition(ui, x1, y1, charLayout.rightBottom);
                    this.getAbsolutePosition(ui, x0, y1, charLayout.leftBottom);
                    charLayout.r = rgbaf.r;
                    charLayout.g = rgbaf.g;
                    charLayout.b = rgbaf.b;
                    charLayout.a = rgbaf.a;
                    charLayout.sdfThreshold = sdfThreshold;
                    charLayout.sdfShadow = shadow;
                    charLayout.outlineThickness = outlineThickness;
                    charLayout.outlineR = outlineR;
                    charLayout.outlineG = outlineG;
                    charLayout.outlineB = outlineB;
                    charLayout.outlineA = outlineA * charLayout.a;
                    s_layout.add(charLayout);
                }

                lastCharDef = charDef;
                x += charDef.xadvance;
                textWidth += charDef.xadvance;
                numCharsRendered++;
            }
        }

        xAlongLine += textWidth * scale;
        WorldMapStyleLayer.RGBAf.s_pool.release(rgbaf);
        return numCharsRendered;
    }

    private float getSdfThreshold(UIWorldMap ui) {
        double[] p0 = this.getAbsolutePosition(ui, -5.0, 0.0, WorldMapStreet.L_getSdfThreshold.p0);
        double[] p1 = this.getAbsolutePosition(ui, 5.0, 0.0, WorldMapStreet.L_getSdfThreshold.p1);
        double distance = Math.hypot(p0[0] - p1[0], p0[1] - p1[1]);
        return (float)(0.125 / (distance / 10.0));
    }

    private double[] getAbsolutePosition(UIWorldMap ui, double localX, double localY, double[] xy) {
        double scale = fontScale;
        localX *= scale;
        localY *= scale;
        xy[0] = localX * cosA + localY * sinA + x;
        xy[1] = -localX * sinA + localY * cosA + y;
        return xy;
    }

    public float getClosestPointOn(UIWorldMap ui, float uiX, float uiY, ClosestPoint closestPoint) {
        double closestDistSq = Double.MAX_VALUE;
        Vector2f closest = BaseVehicle.allocVector2f();
        float distanceFromStart = 0.0F;

        for (int i = 0; i < this.getNumPoints() - 1; i++) {
            float x1 = this.getPointX(i);
            float y1 = this.getPointY(i);
            float x2 = this.getPointX(i + 1);
            float y2 = this.getPointY(i + 1);
            float uiX1 = ui.getAPI().worldToUIX(x1, y1);
            float uiY1 = ui.getAPI().worldToUIY(x1, y1);
            float uiX2 = ui.getAPI().worldToUIX(x2, y2);
            float uiY2 = ui.getAPI().worldToUIY(x2, y2);
            double distSq = PZMath.closestPointOnLineSegment(uiX1, uiY1, uiX2, uiY2, uiX, uiY, 0.0, closest);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestPoint.x = ui.getAPI().uiToWorldX(closest.x, closest.y);
                closestPoint.y = ui.getAPI().uiToWorldY(closest.x, closest.y);
                closestPoint.distSq = (float)distSq;
                closestPoint.distanceFromStart = distanceFromStart + Vector2f.length(closest.x - uiX1, closest.y - uiY1);
                closestPoint.index = i;
            }

            distanceFromStart += Vector2f.length(uiX2 - uiX1, uiY2 - uiY1);
        }

        BaseVehicle.releaseVector2f(closest);
        return closestPoint.distSq;
    }

    public float getClosestPointOn(float worldX, float worldY, ClosestPoint closestPoint) {
        double closestDistSq = Double.MAX_VALUE;
        Vector2f closest = BaseVehicle.allocVector2f();
        float distanceFromStart = 0.0F;

        for (int i = 0; i < this.getNumPoints() - 1; i++) {
            float x1 = this.getPointX(i);
            float y1 = this.getPointY(i);
            float x2 = this.getPointX(i + 1);
            float y2 = this.getPointY(i + 1);
            double distSq = PZMath.closestPointOnLineSegment(x1, y1, x2, y2, worldX, worldY, 0.0, closest);
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestPoint.x = closest.x;
                closestPoint.y = closest.y;
                closestPoint.distSq = (float)distSq;
                closestPoint.distanceFromStart = distanceFromStart + Vector2f.length(closest.x - x1, closest.y - y1);
                closestPoint.index = i;
            }

            distanceFromStart += Vector2f.length(x2 - x1, y2 - y1);
        }

        BaseVehicle.releaseVector2f(closest);
        return closestPoint.distSq;
    }

    public int pickPoint(UIWorldMap ui, float uiX, float uiY) {
        float closestDistSq = Float.MAX_VALUE;
        int closest = -1;

        for (int i = 0; i < this.getNumPoints(); i++) {
            float x1 = this.getPointX(i);
            float y1 = this.getPointY(i);
            float uiX1 = ui.getAPI().worldToUIX(x1, y1);
            float uiY1 = ui.getAPI().worldToUIY(x1, y1);
            float distSq = IsoUtils.DistanceToSquared(uiX, uiY, uiX1, uiY1);
            if (distSq < closestDistSq && distSq < 100.0F) {
                closestDistSq = distSq;
                closest = i;
            }
        }

        return closest;
    }

    public ClosestPoint getAddPointLocation(UIWorldMap ui, float uiX, float uiY, ClosestPoint closestPoint) {
        float distSq = this.getClosestPointOn(ui, uiX, uiY, closestPoint);
        if (distSq >= 100.0F) {
            return null;
        } else {
            closestPoint.x = Math.round(closestPoint.x * 2.0F) / 2.0F;
            closestPoint.y = Math.round(closestPoint.y * 2.0F) / 2.0F;
            return closestPoint;
        }
    }

    void calculateIntersections(WorldMapStreet other) {
        Vector2f v = BaseVehicle.allocVector2f();
        float distanceFromStart = 0.0F;

        for (int i = 0; i < this.getNumPoints() - 1; i++) {
            float x1 = this.getPointX(i);
            float y1 = this.getPointY(i);
            float x2 = this.getPointX(i + 1);
            float y2 = this.getPointY(i + 1);

            for (int j = 0; j < other.getNumPoints() - 1; j++) {
                float x1o = other.getPointX(j);
                float y1o = other.getPointY(j);
                float x2o = other.getPointX(j + 1);
                float y2o = other.getPointY(j + 1);
                if (PZMath.intersectLineSegments(x1, y1, x2, y2, x1o, y1o, x2o, y2o, v)) {
                    Intersection intersection = new Intersection();
                    intersection.street = other;
                    intersection.worldX = v.x;
                    intersection.worldY = v.y;
                    intersection.segment = i;
                    intersection.distanceFromStart = distanceFromStart + Vector2f.length(v.x - x1, v.y - y1);
                    if (this.getMatchingIntersection(other, v.x, v.y) == null) {
                        this.getIntersections().add(intersection);
                    }
                }
            }

            distanceFromStart += Vector2f.length(x2 - x1, y2 - y1);
        }

        BaseVehicle.releaseVector2f(v);
    }

    public void createHighlightPolygons(TFloatArrayList polygon, TFloatArrayList triangles) {
        for (int i = 0; i < this.splitStreets.size(); i++) {
            WorldMapStreet street = this.splitStreets.get(i);
            street.createPolygon(polygon);
            street.triangulate(polygon, triangles2);
            triangles.addAll(triangles2);
        }
    }

    public void createPolygon(TFloatArrayList points) {
        points.clear();
        if (IsoMetaGrid.clipperOffset == null) {
            IsoMetaGrid.clipperOffset = new ClipperOffset();
            IsoMetaGrid.clipperBuffer = ByteBuffer.allocateDirect(3072);
        }

        ClipperOffset clipperOffset = IsoMetaGrid.clipperOffset;
        ByteBuffer clipperBuffer = IsoMetaGrid.clipperBuffer;
        clipperOffset.clear();
        clipperBuffer.clear();
        float dxy = 0.0F;

        for (int j = 0; j < this.getNumPoints(); j++) {
            float x1 = this.getPointX(j);
            float y1 = this.getPointY(j);
            clipperBuffer.putFloat(x1 + 0.0F);
            clipperBuffer.putFloat(y1 + 0.0F);
        }

        clipperBuffer.flip();
        clipperOffset.addPath(this.getNumPoints(), clipperBuffer, ClipperOffset.JoinType.Miter.ordinal(), ClipperOffset.EndType.Butt.ordinal());
        float thickness = this.width;
        clipperOffset.execute(thickness / 2.0F);
        int numPolys = clipperOffset.getPolygonCount();
        if (numPolys >= 1) {
            clipperBuffer.clear();
            clipperOffset.getPolygon(0, clipperBuffer);
            short pointCount = clipperBuffer.getShort();
            points.ensureCapacity(pointCount * 2);

            for (int k = 0; k < pointCount; k++) {
                points.add(clipperBuffer.getFloat());
                points.add(clipperBuffer.getFloat());
            }
        }
    }

    boolean isClockwise(TFloatArrayList polygon) {
        float sum = 0.0F;
        int numPoints = polygon.size() / 2;

        for (int i = 0; i < numPoints; i++) {
            float p1x = polygon.get(i * 2);
            float p1y = polygon.get(i * 2 + 1);
            int j = i % numPoints;
            float p2x = polygon.get(j * 2);
            float p2y = polygon.get(j * 2 + 1);
            sum += (p2x - p1x) * (p2y + p1y);
        }

        return sum > 0.0;
    }

    public void triangulate(TFloatArrayList polygon, TFloatArrayList triangles) {
        triangles.clear();
        if (polygon.size() >= 6) {
            if (clipper == null) {
                clipper = new Clipper();
            }

            clipper.clear();
            if (IsoMetaGrid.clipperOffset == null) {
                IsoMetaGrid.clipperOffset = new ClipperOffset();
                IsoMetaGrid.clipperBuffer = ByteBuffer.allocateDirect(4096);
            }

            ByteBuffer bb = IsoMetaGrid.clipperBuffer;
            bb.clear();
            if (this.isClockwise(polygon)) {
                for (int i = polygon.size() / 2 - 1; i >= 0; i--) {
                    bb.putFloat(polygon.get(i * 2));
                    bb.putFloat(polygon.get(i * 2 + 1));
                }
            } else {
                for (int i = 0; i < polygon.size() / 2; i++) {
                    bb.putFloat(polygon.get(i * 2));
                    bb.putFloat(polygon.get(i * 2 + 1));
                }
            }

            clipper.addPath(polygon.size() / 2, bb, false);
            int numPolygons = clipper.generatePolygons();
            if (numPolygons >= 1) {
                bb.clear();

                try {
                    int numPoints = clipper.triangulate(0, bb);
                    triangles.clear();

                    for (int i = 0; i < numPoints; i++) {
                        triangles.add(bb.getFloat());
                        triangles.add(bb.getFloat());
                    }
                } catch (BufferOverflowException var7) {
                    IsoMetaGrid.clipperBuffer = ByteBuffer.allocateDirect(bb.capacity() + 1024);
                }
            }
        }
    }

    public boolean isOnScreen(UIWorldMap ui) {
        float x1 = ui.getAPI().worldToUIX(this.getMinX(), this.getMinY());
        float y1 = ui.getAPI().worldToUIY(this.getMinX(), this.getMinY());
        float x2 = ui.getAPI().worldToUIX(this.getMaxX(), this.getMinY());
        float y2 = ui.getAPI().worldToUIY(this.getMaxX(), this.getMinY());
        float x3 = ui.getAPI().worldToUIX(this.getMaxX(), this.getMaxY());
        float y3 = ui.getAPI().worldToUIY(this.getMaxX(), this.getMaxY());
        float x4 = ui.getAPI().worldToUIX(this.getMinX(), this.getMaxY());
        float y4 = ui.getAPI().worldToUIY(this.getMinX(), this.getMaxY());
        float minX = PZMath.min(x1, x2, x3, x4);
        float minY = PZMath.min(y1, y2, y3, y4);
        float maxX = PZMath.max(x1, x2, x3, x4);
        float maxY = PZMath.max(y1, y2, y3, y4);
        return minX < ui.getWidth() && maxX > 0.0F && minY < ui.getHeight() && maxY > 0.0F;
    }

    public WorldMapStreet createCopy(WorldMapStreets owner) {
        StreetPoints points = s_pointsPool.alloc();
        points.clear();
        points.addAll(this.points);
        WorldMapStreet street = s_pool.alloc().init(owner, this.getTranslatedText(), points);
        street.setWidth(this.getWidth());
        return street;
    }

    static final class L_getSdfThreshold {
        static final double[] p0 = new double[2];
        static final double[] p1 = new double[2];
    }

    static final class LayoutCounts {
        int total;
        int upsideDown;
        int up;
        int down;
    }
}
