// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.iso.fboRenderChunk.FBORenderWorldMarkers;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;

/**
 * TurboTuTone.
 */
@UsedFromLua
public final class WorldMarkers {
    private static final float CIRCLE_TEXTURE_SCALE = 1.5F;
    public static final WorldMarkers instance = new WorldMarkers();
    private static int nextGridSquareMarkerId;
    private static int nextHomingPointId;
    private final List<WorldMarkers.GridSquareMarker> gridSquareMarkers = new ArrayList<>();
    private final WorldMarkers.PlayerHomingPointList[] homingPoints = new WorldMarkers.PlayerHomingPointList[4];
    private final WorldMarkers.DirectionArrowList[] directionArrows = new WorldMarkers.DirectionArrowList[4];
    private static final ColorInfo stCol = new ColorInfo();
    private final WorldMarkers.PlayerScreen playerScreen = new WorldMarkers.PlayerScreen();
    private final WorldMarkers.Point intersectPoint = new WorldMarkers.Point(0.0F, 0.0F);
    private final WorldMarkers.Point arrowStart = new WorldMarkers.Point(0.0F, 0.0F);
    private final WorldMarkers.Point arrowEnd = new WorldMarkers.Point(0.0F, 0.0F);
    private final WorldMarkers.Line arrowLine = new WorldMarkers.Line(this.arrowStart, this.arrowEnd);

    private WorldMarkers() {
    }

    public void init() {
        if (!GameServer.server) {
            for (int i = 0; i < this.homingPoints.length; i++) {
                this.homingPoints[i] = new WorldMarkers.PlayerHomingPointList();
            }

            for (int i = 0; i < this.directionArrows.length; i++) {
                this.directionArrows[i] = new WorldMarkers.DirectionArrowList();
            }
        }
    }

    public void reset() {
        for (int i = 0; i < this.homingPoints.length; i++) {
            this.homingPoints[i].clear();
        }

        for (int i = 0; i < this.directionArrows.length; i++) {
            this.directionArrows[i].clear();
        }

        this.gridSquareMarkers.clear();
    }

    private int GetDistance(int dx, int dy, int sx, int sy) {
        return (int)Math.sqrt(Math.pow(dx - sx, 2.0) + Math.pow(dy - sy, 2.0));
    }

    private float getAngle(int px, int py, int tx, int ty) {
        float angle = (float)Math.toDegrees(Math.atan2(ty - py, tx - px));
        if (angle < 0.0F) {
            angle += 360.0F;
        }

        return angle;
    }

    private float angleDegrees(float angle) {
        if (angle < 0.0F) {
            angle += 360.0F;
        }

        if (angle > 360.0F) {
            angle -= 360.0F;
        }

        return angle;
    }

    public WorldMarkers.PlayerHomingPoint getHomingPoint(int id) {
        for (int i = 0; i < this.homingPoints.length; i++) {
            for (int j = this.homingPoints[i].size() - 1; j >= 0; j++) {
                if (this.homingPoints[i].get(j).id == id) {
                    return this.homingPoints[i].get(j);
                }
            }
        }

        return null;
    }

    public WorldMarkers.PlayerHomingPoint addPlayerHomingPoint(IsoPlayer player, int x, int y) {
        return this.addPlayerHomingPoint(player, x, y, "arrow_triangle", 1.0F, 1.0F, 1.0F, 1.0F, true, 20);
    }

    public WorldMarkers.PlayerHomingPoint addPlayerHomingPoint(IsoPlayer player, int x, int y, float r, float g, float b, float a) {
        return this.addPlayerHomingPoint(player, x, y, "arrow_triangle", r, g, b, a, true, 20);
    }

    public WorldMarkers.PlayerHomingPoint addPlayerHomingPoint(
        IsoPlayer player, int x, int y, String texname, float r, float g, float b, float a, boolean homeOnTarget, int homeOnDist
    ) {
        if (GameServer.server) {
            return null;
        } else {
            WorldMarkers.PlayerHomingPoint point = new WorldMarkers.PlayerHomingPoint(player.playerIndex);
            point.setActive(true);
            point.setTexture(texname);
            point.setX(x);
            point.setY(y);
            point.setR(r);
            point.setG(g);
            point.setB(b);
            point.setA(a);
            point.setHomeOnTargetInView(homeOnTarget);
            point.setHomeOnTargetDist(homeOnDist);
            this.homingPoints[player.playerIndex].add(point);
            return point;
        }
    }

    public boolean removeHomingPoint(WorldMarkers.PlayerHomingPoint point) {
        return this.removeHomingPoint(point.getID());
    }

    public boolean removeHomingPoint(int id) {
        for (int i = 0; i < this.homingPoints.length; i++) {
            for (int j = this.homingPoints[i].size() - 1; j >= 0; j--) {
                if (this.homingPoints[i].get(j).id == id) {
                    this.homingPoints[i].get(j).remove();
                    this.homingPoints[i].remove(j);
                    return true;
                }
            }
        }

        return false;
    }

    public boolean removePlayerHomingPoint(IsoPlayer player, WorldMarkers.PlayerHomingPoint point) {
        return this.removePlayerHomingPoint(player, point.getID());
    }

    public boolean removePlayerHomingPoint(IsoPlayer player, int id) {
        for (int i = this.homingPoints[player.playerIndex].size() - 1; i >= 0; i--) {
            if (this.homingPoints[player.playerIndex].get(i).id == id) {
                this.homingPoints[player.playerIndex].get(i).remove();
                this.homingPoints[player.playerIndex].remove(i);
                return true;
            }
        }

        return false;
    }

    public void removeAllHomingPoints(IsoPlayer player) {
        this.homingPoints[player.playerIndex].clear();
    }

    public WorldMarkers.DirectionArrow getDirectionArrow(int id) {
        for (int i = 0; i < this.directionArrows.length; i++) {
            for (int j = this.directionArrows[i].size() - 1; j >= 0; j--) {
                if (this.directionArrows[i].get(j).id == id) {
                    return this.directionArrows[i].get(j);
                }
            }
        }

        return null;
    }

    public WorldMarkers.DirectionArrow addDirectionArrow(IsoPlayer player, int x, int y, int z, String texname, float r, float g, float b, float a) {
        if (GameServer.server) {
            return null;
        } else {
            WorldMarkers.DirectionArrow arrow = new WorldMarkers.DirectionArrow(player.playerIndex);
            arrow.setActive(true);
            arrow.setTexture(texname);
            arrow.setTexDown("dir_arrow_down");
            arrow.setTexStairsUp("dir_arrow_stairs_up");
            arrow.setTexStairsDown("dir_arrow_stairs_down");
            arrow.setX(x);
            arrow.setY(y);
            arrow.setZ(z);
            arrow.setR(r);
            arrow.setG(g);
            arrow.setB(b);
            arrow.setA(a);
            this.directionArrows[player.playerIndex].add(arrow);
            return arrow;
        }
    }

    public boolean removeDirectionArrow(WorldMarkers.DirectionArrow arrow) {
        return this.removeDirectionArrow(arrow.getID());
    }

    public boolean removeDirectionArrow(int id) {
        for (int i = 0; i < this.directionArrows.length; i++) {
            for (int j = this.directionArrows[i].size() - 1; j >= 0; j--) {
                if (this.directionArrows[i].get(j).id == id) {
                    this.directionArrows[i].get(j).remove();
                    this.directionArrows[i].remove(j);
                    return true;
                }
            }
        }

        return false;
    }

    public boolean removePlayerDirectionArrow(IsoPlayer player, WorldMarkers.DirectionArrow arrow) {
        return this.removePlayerDirectionArrow(player, arrow.getID());
    }

    public boolean removePlayerDirectionArrow(IsoPlayer player, int id) {
        for (int j = this.directionArrows[player.playerIndex].size() - 1; j >= 0; j--) {
            if (this.directionArrows[player.playerIndex].get(j).id == id) {
                this.directionArrows[player.playerIndex].get(j).remove();
                this.directionArrows[player.playerIndex].remove(j);
                return true;
            }
        }

        return false;
    }

    public void removeAllDirectionArrows(IsoPlayer player) {
        this.directionArrows[player.playerIndex].clear();
    }

    public void update() {
        if (!GameServer.server) {
            this.updateGridSquareMarkers();
            this.updateHomingPoints();
            this.updateDirectionArrows();
        }
    }

    private void updateDirectionArrows() {
        int playerIndex = IsoCamera.frameState.playerIndex;

        for (int plrIndex = 0; plrIndex < this.directionArrows.length; plrIndex++) {
            if (plrIndex == playerIndex && !this.directionArrows[plrIndex].isEmpty()) {
                for (int index = this.directionArrows[plrIndex].size() - 1; index >= 0; index--) {
                    if (this.directionArrows[plrIndex].get(index).isRemoved()) {
                        this.directionArrows[plrIndex].remove(index);
                    }
                }

                this.playerScreen.update(plrIndex);

                for (int indexx = 0; indexx < this.directionArrows[plrIndex].size(); indexx++) {
                    WorldMarkers.DirectionArrow arrow = this.directionArrows[plrIndex].get(indexx);
                    if (arrow.active && IsoPlayer.players[plrIndex] != null) {
                        IsoPlayer player = IsoPlayer.players[plrIndex];
                        if (player.getSquare() != null) {
                            PlayerCamera camera = IsoCamera.cameras[plrIndex];
                            float ZOOM = Core.getInstance().getZoom(plrIndex);
                            int px = player.getSquare().getX();
                            int py = player.getSquare().getY();
                            int pz = player.getSquare().getZ();
                            int dist = this.GetDistance(px, py, arrow.x, arrow.y);
                            boolean isInAccurateDist = false;
                            boolean isWithinViewRectangle = false;
                            float targetScreenX = 0.0F;
                            float targetScreenY = 0.0F;
                            if (dist < 300) {
                                isInAccurateDist = true;
                                targetScreenX = camera.XToScreenExact(arrow.x, arrow.y, pz, 0) / ZOOM;
                                targetScreenY = camera.YToScreenExact(arrow.x, arrow.y, pz, 0) / ZOOM;
                                if (this.playerScreen.isWithinInner(targetScreenX, targetScreenY)) {
                                    isWithinViewRectangle = true;
                                }
                            }

                            if (isWithinViewRectangle) {
                                arrow.renderWithAngle = false;
                                arrow.isDrawOnWorld = false;
                                arrow.renderSizeMod = 1.0F;
                                if (ZOOM > 1.0F) {
                                    arrow.renderSizeMod /= ZOOM;
                                }

                                arrow.renderScreenX = targetScreenX;
                                arrow.renderScreenY = targetScreenY;
                                if (pz == arrow.z) {
                                    arrow.renderTexture = arrow.texDown != null ? arrow.texDown : arrow.texture;
                                } else if (arrow.z > pz) {
                                    arrow.renderTexture = arrow.texStairsUp != null ? arrow.texStairsUp : arrow.texture;
                                } else {
                                    arrow.renderTexture = arrow.texStairsDown != null ? arrow.texStairsUp : arrow.texture;
                                }

                                arrow.lastWasWithinView = true;
                            } else {
                                arrow.renderWithAngle = true;
                                arrow.isDrawOnWorld = false;
                                arrow.renderTexture = arrow.texture;
                                arrow.renderSizeMod = 1.0F;
                                float centerScreenX = this.playerScreen.centerX;
                                float centerScreenY = this.playerScreen.centerY;
                                float angle = 0.0F;
                                if (!isInAccurateDist) {
                                    angle = this.getAngle(arrow.x, arrow.y, px, py);
                                    angle = this.angleDegrees(180.0F - angle);
                                    angle = this.angleDegrees(angle + 45.0F);
                                } else {
                                    angle = this.getAngle((int)centerScreenX, (int)centerScreenY, (int)targetScreenX, (int)targetScreenY);
                                    angle = this.angleDegrees(180.0F - angle);
                                    angle = this.angleDegrees(angle - 90.0F);
                                }

                                if (angle != arrow.angle) {
                                    if (!arrow.lastWasWithinView) {
                                        arrow.angle = PZMath.lerpAngle(
                                            PZMath.degToRad(arrow.angle), PZMath.degToRad(angle), 0.25F * GameTime.instance.getMultiplier()
                                        );
                                        arrow.angle = PZMath.radToDeg(arrow.angle);
                                    } else {
                                        arrow.angle = angle;
                                    }
                                }

                                float endX = centerScreenX + 32000.0F * (float)Math.sin(Math.toRadians(arrow.angle));
                                float endY = centerScreenY + 32000.0F * (float)Math.cos(Math.toRadians(arrow.angle));
                                arrow.renderScreenX = centerScreenX;
                                arrow.renderScreenY = centerScreenY;
                                this.arrowStart.set(centerScreenX, centerScreenY);
                                this.arrowEnd.set(endX, endY);
                                WorldMarkers.Line[] borders = this.playerScreen.getBorders();

                                for (int i = 0; i < borders.length; i++) {
                                    this.intersectPoint.set(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
                                    if (intersectLineSegments(this.arrowLine, borders[i], this.intersectPoint)) {
                                        arrow.renderScreenX = this.intersectPoint.x;
                                        arrow.renderScreenY = this.intersectPoint.y;
                                        break;
                                    }
                                }

                                arrow.lastWasWithinView = false;
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateHomingPoints() {
        int playerIndex = IsoCamera.frameState.playerIndex;

        for (int i = 0; i < this.homingPoints.length; i++) {
            if (i == playerIndex && !this.homingPoints[i].isEmpty()) {
                for (int j = this.homingPoints[i].size() - 1; j >= 0; j--) {
                    if (this.homingPoints[i].get(j).isRemoved) {
                        this.homingPoints[i].remove(j);
                    }
                }

                this.playerScreen.update(i);

                for (int jx = 0; jx < this.homingPoints[i].size(); jx++) {
                    WorldMarkers.PlayerHomingPoint point = this.homingPoints[i].get(jx);
                    if (point.active && IsoPlayer.players[i] != null) {
                        IsoPlayer player = IsoPlayer.players[i];
                        if (player.getSquare() != null) {
                            PlayerCamera camera = IsoCamera.cameras[i];
                            float ZOOM = Core.getInstance().getZoom(i);
                            point.renderSizeMod = 1.0F;
                            if (ZOOM > 1.0F) {
                                point.renderSizeMod /= ZOOM;
                            }

                            int px = player.getSquare().getX();
                            int py = player.getSquare().getY();
                            point.dist = this.GetDistance(px, py, point.x, point.y);
                            point.targetOnScreen = false;
                            if (point.dist < 200.0F) {
                                point.targetScreenX = camera.XToScreenExact(point.x, point.y, 0.0F, 0) / ZOOM;
                                point.targetScreenY = camera.YToScreenExact(point.x, point.y, 0.0F, 0) / ZOOM;
                                point.targetScreenX = point.targetScreenX + point.homeOnOffsetX / ZOOM;
                                point.targetScreenY = point.targetScreenY + point.homeOnOffsetY / ZOOM;
                                point.targetOnScreen = this.playerScreen.isOnScreen(point.targetScreenX, point.targetScreenY);
                            }

                            float x = this.playerScreen.centerX;
                            float baseRenderX = x + point.renderOffsetX / ZOOM;
                            float y = this.playerScreen.centerY;
                            float baseRenderY = y + point.renderOffsetY / ZOOM;
                            if (!point.customTargetAngle) {
                                float angle = 0.0F;
                                if (!point.targetOnScreen) {
                                    angle = this.getAngle(point.x, point.y, px, py);
                                    angle = this.angleDegrees(180.0F - angle);
                                    angle = this.angleDegrees(angle + 45.0F);
                                } else {
                                    angle = this.getAngle((int)baseRenderX, (int)baseRenderY, (int)point.targetScreenX, (int)point.targetScreenY);
                                    angle = this.angleDegrees(180.0F - angle);
                                    angle = this.angleDegrees(angle - 90.0F);
                                }

                                point.targetAngle = angle;
                            }

                            if (point.targetAngle != point.angle) {
                                point.angle = PZMath.lerpAngle(
                                    PZMath.degToRad(point.angle), PZMath.degToRad(point.targetAngle), point.angleLerpVal * GameTime.instance.getMultiplier()
                                );
                                point.angle = PZMath.radToDeg(point.angle);
                            }

                            float offset = point.stickToCharDist / ZOOM;
                            point.targRenderX = baseRenderX + offset * (float)Math.sin(Math.toRadians(point.angle));
                            point.targRenderY = baseRenderY + offset * (float)Math.cos(Math.toRadians(point.angle));
                            float multi = point.movementLerpVal;
                            if (point.targetOnScreen) {
                                float distTargRender = this.GetDistance(
                                    (int)point.targRenderX, (int)point.targRenderY, (int)point.targetScreenX, (int)point.targetScreenY
                                );
                                float distBaseRender = this.GetDistance((int)baseRenderX, (int)baseRenderY, (int)point.targetScreenX, (int)point.targetScreenY);
                                if (distBaseRender < distTargRender || point.homeOnTargetInView && point.dist <= point.homeOnTargetDist) {
                                    distBaseRender *= 0.75F;
                                    point.targRenderX = baseRenderX + distBaseRender * (float)Math.sin(Math.toRadians(point.targetAngle));
                                    point.targRenderY = baseRenderY + distBaseRender * (float)Math.cos(Math.toRadians(point.targetAngle));
                                }
                            }

                            point.targRenderX = this.playerScreen.clampToInnerX(point.targRenderX);
                            point.targRenderY = this.playerScreen.clampToInnerY(point.targRenderY);
                            if (point.targRenderX != point.renderX) {
                                point.renderX = PZMath.lerp(point.renderX, point.targRenderX, multi * GameTime.instance.getMultiplier());
                            }

                            if (point.targRenderY != point.renderY) {
                                point.renderY = PZMath.lerp(point.renderY, point.targRenderY, multi * GameTime.instance.getMultiplier());
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateGridSquareMarkers() {
        if (IsoCamera.frameState.playerIndex == 0) {
            if (!this.gridSquareMarkers.isEmpty()) {
                for (int i = this.gridSquareMarkers.size() - 1; i >= 0; i--) {
                    if (this.gridSquareMarkers.get(i).isRemoved()) {
                        this.gridSquareMarkers.remove(i);
                    }
                }

                for (int ix = 0; ix < this.gridSquareMarkers.size(); ix++) {
                    WorldMarkers.GridSquareMarker m = this.gridSquareMarkers.get(ix);
                    if (m.alphaInc) {
                        m.alpha = m.alpha + GameTime.getInstance().getMultiplier() * m.fadeSpeed;
                        if (m.alpha > m.alphaMax) {
                            m.alphaInc = false;
                            m.alpha = m.alphaMax;
                        }
                    } else {
                        m.alpha = m.alpha - GameTime.getInstance().getMultiplier() * m.fadeSpeed;
                        if (m.alpha < m.alphaMin) {
                            m.alphaInc = true;
                            m.alpha = 0.3F;
                        }
                    }
                }
            }
        }
    }

    public boolean removeGridSquareMarker(WorldMarkers.GridSquareMarker marker) {
        return this.removeGridSquareMarker(marker.getID());
    }

    public boolean removeGridSquareMarker(int id) {
        for (int i = this.gridSquareMarkers.size() - 1; i >= 0; i--) {
            if (this.gridSquareMarkers.get(i).getID() == id) {
                this.gridSquareMarkers.get(i).remove();
                this.gridSquareMarkers.remove(i);
                return true;
            }
        }

        return false;
    }

    public WorldMarkers.GridSquareMarker getGridSquareMarker(int id) {
        for (int i = 0; i < this.gridSquareMarkers.size(); i++) {
            if (this.gridSquareMarkers.get(i).getID() == id) {
                return this.gridSquareMarkers.get(i);
            }
        }

        return null;
    }

    public WorldMarkers.GridSquareMarker addGridSquareMarker(IsoGridSquare gs, float r, float g, float b, boolean doAlpha, float size) {
        return this.addGridSquareMarker("circle_center", "circle_only_highlight", gs, r, g, b, doAlpha, size, 0.006F, 0.3F, 1.0F);
    }

    public WorldMarkers.GridSquareMarker addGridSquareMarker(
        String texid, String overlay, IsoGridSquare gs, float r, float g, float b, boolean doAlpha, float size
    ) {
        return this.addGridSquareMarker(texid, overlay, gs, r, g, b, doAlpha, size, 0.006F, 0.3F, 1.0F);
    }

    public WorldMarkers.GridSquareMarker addGridSquareMarker(
        String texid, String overlay, IsoGridSquare gs, float r, float g, float b, boolean doAlpha, float size, float fadeSpeed, float fadeMin, float fadeMax
    ) {
        if (GameServer.server) {
            return null;
        } else {
            WorldMarkers.GridSquareMarker m = new WorldMarkers.GridSquareMarker();
            m.init(texid, overlay, gs.x, gs.y, gs.z, size);
            m.setR(r);
            m.setG(g);
            m.setB(b);
            m.setA(1.0F);
            m.setDoAlpha(doAlpha);
            m.setFadeSpeed(fadeSpeed);
            m.setAlpha(0.0F);
            m.setAlphaMin(fadeMin);
            m.setAlphaMax(fadeMax);
            this.gridSquareMarkers.add(m);
            return m;
        }
    }

    public void renderGridSquareMarkers(IsoCell.PerPlayerRender perPlayerRender, int zLayer, int playerIndex) {
        if (!GameServer.server && !this.gridSquareMarkers.isEmpty()) {
            IsoPlayer player = IsoPlayer.players[playerIndex];
            if (player != null) {
                for (int i = 0; i < this.gridSquareMarkers.size(); i++) {
                    WorldMarkers.GridSquareMarker m = this.gridSquareMarkers.get(i);
                    if (m.z == zLayer && m.z == player.getZ() && m.active) {
                        float offsetX = 0.0F;
                        float offsetY = 0.0F;
                        stCol.set(m.r, m.g, m.b, m.a);
                        if (m.doBlink) {
                            m.sprite.alpha = Core.blinkAlpha;
                        } else {
                            m.sprite.alpha = m.doAlpha ? m.alpha : 1.0F;
                        }

                        m.sprite.render(null, m.x, m.y, m.z, IsoDirections.N, 0.0F, 0.0F, stCol);
                        if (m.spriteOverlay != null) {
                            m.spriteOverlay.alpha = 1.0F;
                            m.spriteOverlay.render(null, m.x, m.y, m.z, IsoDirections.N, 0.0F, 0.0F, stCol);
                        }
                    }
                }
            }
        }
    }

    public void renderGridSquareMarkers(int z) {
        if (PerformanceSettings.fboRenderChunk) {
            FBORenderWorldMarkers.getInstance().render(z, this.gridSquareMarkers);
        }
    }

    public void debugRender() {
    }

    public void render() {
        this.update();
        this.renderHomingPoint();
        this.renderDirectionArrow(false);
    }

    public void renderHomingPoint() {
        if (!GameServer.server) {
            int playerIndex = IsoCamera.frameState.playerIndex;

            for (int i = 0; i < this.homingPoints.length; i++) {
                if (i == playerIndex && !this.homingPoints[i].isEmpty()) {
                    for (int j = 0; j < this.homingPoints[i].size(); j++) {
                        WorldMarkers.PlayerHomingPoint point = this.homingPoints[i].get(j);
                        if (point.active && point.texture != null) {
                            float angle = 180.0F - point.angle;
                            if (angle < 0.0F) {
                                angle += 360.0F;
                            }

                            float alpha = point.a;
                            if (ClimateManager.getInstance().getFogIntensity() > 0.0F && alpha < 1.0F) {
                                float diff = 1.0F - alpha;
                                alpha += diff * ClimateManager.getInstance().getFogIntensity() * 2.0F;
                                alpha = PZMath.clamp_01(alpha);
                            }

                            this.DrawTextureAngle(
                                point.texture,
                                point.renderWidth,
                                point.renderHeight,
                                point.renderX,
                                point.renderY,
                                angle,
                                point.r,
                                point.g,
                                point.b,
                                alpha,
                                point.renderSizeMod
                            );
                        }
                    }
                }
            }
        }
    }

    public void renderDirectionArrow(boolean worldDraw) {
        if (!GameServer.server) {
            int playerIndex = IsoCamera.frameState.playerIndex;

            for (int i = 0; i < this.directionArrows.length; i++) {
                if (i == playerIndex && !this.directionArrows[i].isEmpty()) {
                    for (int j = 0; j < this.directionArrows[i].size(); j++) {
                        WorldMarkers.DirectionArrow arrow = this.directionArrows[i].get(j);
                        if (arrow.active && arrow.renderTexture != null && arrow.isDrawOnWorld == worldDraw) {
                            float angle = 0.0F;
                            if (arrow.renderWithAngle) {
                                angle = 180.0F - arrow.angle;
                                if (angle < 0.0F) {
                                    angle += 360.0F;
                                }
                            }

                            this.DrawTextureAngle(
                                arrow.renderTexture,
                                arrow.renderWidth,
                                arrow.renderHeight,
                                arrow.renderScreenX,
                                arrow.renderScreenY,
                                angle,
                                arrow.r,
                                arrow.g,
                                arrow.b,
                                arrow.a,
                                arrow.renderSizeMod
                            );
                        }
                    }
                }
            }
        }
    }

    private void DrawTextureAngle(
        Texture tex, float width, float height, double centerX, double centerY, double angle, float r, float g, float b, float a, float renderSize
    ) {
        float dx = width * renderSize / 2.0F;
        float dy = height * renderSize / 2.0F;
        double radian = Math.toRadians(180.0 + angle);
        double xCos = Math.cos(radian) * dx;
        double xSin = Math.sin(radian) * dx;
        double yCos = Math.cos(radian) * dy;
        double ySin = Math.sin(radian) * dy;
        double x1 = xCos - ySin;
        double y1 = yCos + xSin;
        double x2 = -xCos - ySin;
        double y2 = yCos - xSin;
        double x3 = -xCos + ySin;
        double y3 = -yCos - xSin;
        double x4 = xCos + ySin;
        double y4 = -yCos + xSin;
        x1 += centerX;
        y1 += centerY;
        x2 += centerX;
        y2 += centerY;
        x3 += centerX;
        y3 += centerY;
        x4 += centerX;
        y4 += centerY;
        SpriteRenderer.instance.render(tex, x1, y1, x2, y2, x3, y3, x4, y4, r, g, b, a, r, g, b, a, r, g, b, a, r, g, b, a, null);
    }

    public static boolean intersectLineSegments(WorldMarkers.Line l1, WorldMarkers.Line l2, WorldMarkers.Point intersection) {
        float x1 = l1.s.x;
        float y1 = l1.s.y;
        float x2 = l1.e.x;
        float y2 = l1.e.y;
        float x3 = l2.s.x;
        float y3 = l2.s.y;
        float x4 = l2.e.x;
        float y4 = l2.e.y;
        float d = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1);
        if (d == 0.0F) {
            return false;
        } else {
            float yd = y1 - y3;
            float xd = x1 - x3;
            float ua = ((x4 - x3) * yd - (y4 - y3) * xd) / d;
            if (!(ua < 0.0F) && !(ua > 1.0F)) {
                float ub = ((x2 - x1) * yd - (y2 - y1) * xd) / d;
                if (!(ub < 0.0F) && !(ub > 1.0F)) {
                    if (intersection != null) {
                        intersection.set(x1 + (x2 - x1) * ua, y1 + (y2 - y1) * ua);
                    }

                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @UsedFromLua
    public class DirectionArrow {
        public static final boolean doDebug = false;
        private WorldMarkers.DirectionArrow.DebugStuff debugStuff;
        private final int id;
        private boolean active;
        private boolean isRemoved;
        private boolean isDrawOnWorld;
        private Texture renderTexture;
        private Texture texture;
        private Texture texStairsUp;
        private Texture texStairsDown;
        private Texture texDown;
        private int x;
        private int y;
        private int z;
        private float r;
        private float g;
        private float b;
        private float a;
        private float renderWidth;
        private float renderHeight;
        private float angle;
        private final float angleLerpVal;
        private boolean lastWasWithinView;
        private float renderScreenX;
        private float renderScreenY;
        private boolean renderWithAngle;
        private float renderSizeMod;

        public DirectionArrow(final int plrIndex) {
            Objects.requireNonNull(WorldMarkers.this);
            super();
            this.active = true;
            this.renderWidth = 32.0F;
            this.renderHeight = 32.0F;
            this.angleLerpVal = 0.25F;
            this.lastWasWithinView = true;
            this.renderWithAngle = true;
            this.renderSizeMod = 1.0F;
            if (Core.debug) {
            }

            this.id = WorldMarkers.nextHomingPointId++;
        }

        public void setTexture(String texname) {
            if (texname == null) {
                texname = "dir_arrow_up";
            }

            this.texture = Texture.getSharedTexture("media/textures/highlights/" + texname + ".png");
        }

        public void setTexDown(String texname) {
            this.texDown = Texture.getSharedTexture("media/textures/highlights/" + texname + ".png");
        }

        public void setTexStairsDown(String texname) {
            this.texStairsDown = Texture.getSharedTexture("media/textures/highlights/" + texname + ".png");
        }

        public void setTexStairsUp(String texname) {
            this.texStairsUp = Texture.getSharedTexture("media/textures/highlights/" + texname + ".png");
        }

        /**
         * When called will remove the pointer next tick
         */
        public void remove() {
            this.isRemoved = true;
        }

        public boolean isRemoved() {
            return this.isRemoved;
        }

        /**
         * Active can be set to false, the pointer will remain but wont be drawn.
         */
        public boolean isActive() {
            return this.active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public float getR() {
            return this.r;
        }

        public void setR(float r) {
            this.r = r;
        }

        public float getB() {
            return this.b;
        }

        public void setB(float b) {
            this.b = b;
        }

        public float getG() {
            return this.g;
        }

        public void setG(float g) {
            this.g = g;
        }

        public float getA() {
            return this.a;
        }

        public void setA(float a) {
            this.a = a;
        }

        public void setRGBA(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        public int getID() {
            return this.id;
        }

        /**
         * The target position on the map for this pointer.
         */
        public int getX() {
            return this.x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return this.y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getZ() {
            return this.z;
        }

        public void setZ(int z) {
            z = Math.max(-32, z);
            z = Math.min(31, z);
            this.z = z;
        }

        /**
         * Render width and height for the pointer texture.
         */
        public float getRenderWidth() {
            return this.renderWidth;
        }

        public void setRenderWidth(float renderWidth) {
            this.renderWidth = renderWidth;
        }

        public float getRenderHeight() {
            return this.renderHeight;
        }

        public void setRenderHeight(float renderHeight) {
            this.renderHeight = renderHeight;
        }

        private class DebugStuff {
            private float centerX;
            private float centerY;
            private float endX;
            private float endY;

            private DebugStuff() {
                Objects.requireNonNull(DirectionArrow.this);
                super();
            }
        }
    }

    class DirectionArrowList extends ArrayList<WorldMarkers.DirectionArrow> {
        DirectionArrowList() {
            Objects.requireNonNull(WorldMarkers.this);
            super();
        }
    }

    @UsedFromLua
    public static final class GridSquareMarker {
        private final int id;
        private String textureName;
        private String overlayTextureName;
        private IsoSpriteInstance sprite;
        private IsoSpriteInstance spriteOverlay;
        private float origX;
        private float origY;
        private float origZ;
        private float x;
        private float y;
        private float z;
        private float scaleRatio;
        private float r;
        private float g;
        private float b;
        private float a;
        private float size;
        private boolean doBlink;
        private boolean doAlpha;
        private boolean scaleCircleTexture;
        private float fadeSpeed = 0.006F;
        private float alpha;
        private float alphaMax = 1.0F;
        private float alphaMin = 0.3F;
        private boolean alphaInc = true;
        private boolean active = true;
        private boolean isRemoved;

        public GridSquareMarker() {
            this.id = WorldMarkers.nextGridSquareMarkerId++;
        }

        public int getID() {
            return this.id;
        }

        public void remove() {
            this.isRemoved = true;
        }

        public boolean isRemoved() {
            return this.isRemoved;
        }

        public void init(String texid, String overlay, int x, int y, int z, float size) {
            if (texid == null) {
                texid = "circle_center";
            }

            this.textureName = texid;
            this.overlayTextureName = overlay;
            Texture tex = Texture.getSharedTexture("media/textures/highlights/" + texid + ".png");
            float w = tex.getWidth();
            float tileW = 64.0F * Core.tileScale;
            this.scaleRatio = 1.0F / (w / tileW);
            this.sprite = new IsoSpriteInstance(IsoSpriteManager.instance.getSprite("media/textures/highlights/" + texid + ".png"));
            if (overlay != null) {
                this.spriteOverlay = new IsoSpriteInstance(IsoSpriteManager.instance.getSprite("media/textures/highlights/" + overlay + ".png"));
            }

            this.setPosAndSize(x, y, z, size);
        }

        public void setPosAndSize(int x, int y, int z, float size) {
            float sizeScaled = size * (this.scaleCircleTexture ? 1.5F : 1.0F);
            float scale = this.scaleRatio * sizeScaled;
            this.sprite.setScale(scale, scale);
            if (this.spriteOverlay != null) {
                this.spriteOverlay.setScale(scale, scale);
            }

            this.size = size;
            this.origX = x;
            this.origY = y;
            this.origZ = z;
            this.x = x - (sizeScaled - 0.5F);
            this.y = y + 0.5F;
            this.z = z;
        }

        public void setPos(int x, int y, int z) {
            float sizeScaled = this.size * (this.scaleCircleTexture ? 1.5F : 1.0F);
            this.origX = x;
            this.origY = y;
            this.origZ = z;
            this.x = x - (sizeScaled - 0.5F);
            this.y = y + 0.5F;
            this.z = z;
        }

        public void setSize(float size) {
            float sizeScaled = size * (this.scaleCircleTexture ? 1.5F : 1.0F);
            float scale = this.scaleRatio * sizeScaled;
            this.sprite.setScale(scale, scale);
            if (this.spriteOverlay != null) {
                this.spriteOverlay.setScale(scale, scale);
            }

            this.size = size;
            this.x = this.origX - (sizeScaled - 0.5F);
            this.y = this.origY + 0.5F;
            this.z = this.origZ;
        }

        public boolean isActive() {
            return this.active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public float getSize() {
            return this.size;
        }

        public float getX() {
            return this.x;
        }

        public float getY() {
            return this.y;
        }

        public float getZ() {
            return this.z;
        }

        public float getR() {
            return this.r;
        }

        public void setR(float r) {
            this.r = r;
        }

        public float getG() {
            return this.g;
        }

        public void setG(float g) {
            this.g = g;
        }

        public float getB() {
            return this.b;
        }

        public void setB(float b) {
            this.b = b;
        }

        public float getA() {
            return this.a;
        }

        public void setA(float a) {
            this.a = a;
        }

        public float getAlpha() {
            return this.alpha;
        }

        public void setAlpha(float alpha) {
            this.alpha = alpha;
        }

        public float getAlphaMax() {
            return this.alphaMax;
        }

        public void setAlphaMax(float alphaMax) {
            this.alphaMax = alphaMax;
        }

        public float getAlphaMin() {
            return this.alphaMin;
        }

        public void setAlphaMin(float alphaMin) {
            this.alphaMin = alphaMin;
        }

        public boolean isDoAlpha() {
            return this.doAlpha;
        }

        public void setDoAlpha(boolean doAlpha) {
            this.doAlpha = doAlpha;
        }

        public float getFadeSpeed() {
            return this.fadeSpeed;
        }

        public void setFadeSpeed(float fadeSpeed) {
            this.fadeSpeed = fadeSpeed;
        }

        /**
         * If blink set uses Core.blinkAlpha, this takes precedence over other alpha settings.
         */
        public boolean isDoBlink() {
            return this.doBlink;
        }

        public void setDoBlink(boolean doBlink) {
            this.doBlink = doBlink;
        }

        public boolean isScaleCircleTexture() {
            return this.scaleCircleTexture;
        }

        public void setScaleCircleTexture(boolean bScale) {
            this.scaleCircleTexture = bScale;
            float sizeScaled = this.size * (this.scaleCircleTexture ? 1.5F : 1.0F);
            float scale = this.scaleRatio * sizeScaled;
            if (this.sprite != null) {
                this.sprite.setScale(scale, scale);
            }

            if (this.spriteOverlay != null) {
                this.spriteOverlay.setScale(scale, scale);
            }

            this.x = this.origX - (sizeScaled - 0.5F);
        }

        public float getOriginalX() {
            return this.origX;
        }

        public float getOriginalY() {
            return this.origY;
        }

        public float getOriginalZ() {
            return this.origZ;
        }

        public String getTextureName() {
            return this.textureName;
        }

        public String getOverlayTextureName() {
            return this.overlayTextureName;
        }
    }

    private static class Line {
        WorldMarkers.Point s;
        WorldMarkers.Point e;

        Line(WorldMarkers.Point s, WorldMarkers.Point e) {
            this.s = s;
            this.e = e;
        }

        @Override
        public String toString() {
            return String.format("{s: %s, e: %s}", this.s.toString(), this.e.toString());
        }
    }

    @UsedFromLua
    public static class PlayerHomingPoint {
        private final int id;
        private Texture texture;
        private int x;
        private int y;
        private float r;
        private float g;
        private float b;
        private float a;
        private float angle;
        private float targetAngle;
        private boolean customTargetAngle;
        private float angleLerpVal = 0.25F;
        private float movementLerpVal = 0.25F;
        private int dist;
        private float targRenderX = Core.getInstance().getScreenWidth() / 2.0F;
        private float targRenderY = Core.getInstance().getScreenHeight() / 2.0F;
        private float renderX = this.targRenderX;
        private float renderY = this.targRenderY;
        private float renderOffsetX;
        private float renderOffsetY = 50.0F;
        private float renderWidth = 32.0F;
        private float renderHeight = 32.0F;
        private float renderSizeMod = 1.0F;
        private float targetScreenX;
        private float targetScreenY;
        private boolean targetOnScreen;
        private float stickToCharDist = 130.0F;
        private boolean active;
        private boolean homeOnTargetInView = true;
        private int homeOnTargetDist = 20;
        private float homeOnOffsetX;
        private float homeOnOffsetY;
        private boolean isRemoved;

        public PlayerHomingPoint(int plrIndex) {
            this.id = WorldMarkers.nextHomingPointId++;
            float x = IsoCamera.getScreenLeft(plrIndex);
            float y = IsoCamera.getScreenTop(plrIndex);
            float width = IsoCamera.getScreenWidth(plrIndex);
            float height = IsoCamera.getScreenHeight(plrIndex);
            this.targRenderX = x + width / 2.0F;
            this.targRenderY = y + height / 2.0F;
        }

        public void setTexture(String texname) {
            if (texname == null) {
                texname = "arrow_triangle";
            }

            this.texture = Texture.getSharedTexture("media/textures/highlights/" + texname + ".png");
        }

        /**
         * When called will remove the pointer next tick
         */
        public void remove() {
            this.isRemoved = true;
        }

        public boolean isRemoved() {
            return this.isRemoved;
        }

        /**
         * Active can be set to false, the pointer will remain but wont be drawn.
         */
        public boolean isActive() {
            return this.active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public float getR() {
            return this.r;
        }

        public void setR(float r) {
            this.r = r;
        }

        public float getB() {
            return this.b;
        }

        public void setB(float b) {
            this.b = b;
        }

        public float getG() {
            return this.g;
        }

        public void setG(float g) {
            this.g = g;
        }

        public float getA() {
            return this.a;
        }

        public void setA(float a) {
            this.a = a;
        }

        /**
         * The distance in tiles uppon which the pointer will jump to target (if homeOnTarget is enabled, and the target is onScreen)
         */
        public int getHomeOnTargetDist() {
            return this.homeOnTargetDist;
        }

        public void setHomeOnTargetDist(int homeOnTargetDist) {
            this.homeOnTargetDist = homeOnTargetDist;
        }

        public int getID() {
            return this.id;
        }

        public float getTargetAngle() {
            return this.targetAngle;
        }

        public void setTargetAngle(float targetAngle) {
            this.targetAngle = targetAngle;
        }

        /**
         * When enabled will ommit angle calculation, custom angle be set with 'setTargetAngle'.
         */
        public boolean isCustomTargetAngle() {
            return this.customTargetAngle;
        }

        public void setCustomTargetAngle(boolean customTargetAngle) {
            this.customTargetAngle = customTargetAngle;
        }

        /**
         * The target position on the map for this pointer.
         */
        public int getX() {
            return this.x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return this.y;
        }

        public void setY(int y) {
            this.y = y;
        }

        /**
         * The lerp value for angle adjustment, can be tweaked to be more slowly or faster responding.
         */
        public float getAngleLerpVal() {
            return this.angleLerpVal;
        }

        public void setAngleLerpVal(float angleLerpVal) {
            this.angleLerpVal = angleLerpVal;
        }

        /**
         * The lerp value for jumping to target (homeOneTarget), can be tweaked to be more slowly or faster responding.
         */
        public float getMovementLerpVal() {
            return this.movementLerpVal;
        }

        public void setMovementLerpVal(float movementLerpVal) {
            this.movementLerpVal = movementLerpVal;
        }

        /**
         * if enabled the pointer will jump to the target when its in view (and within the 'homeOnTargetDist'.
         */
        public boolean isHomeOnTargetInView() {
            return this.homeOnTargetInView;
        }

        public void setHomeOnTargetInView(boolean homeOnTargetInView) {
            this.homeOnTargetInView = homeOnTargetInView;
        }

        /**
         * Render width and height for the pointer texture.
         */
        public float getRenderWidth() {
            return this.renderWidth;
        }

        public void setRenderWidth(float renderWidth) {
            this.renderWidth = renderWidth;
        }

        public float getRenderHeight() {
            return this.renderHeight;
        }

        public void setRenderHeight(float renderHeight) {
            this.renderHeight = renderHeight;
        }

        /**
         * The distance in pixels the pointer will hover around the character.
         */
        public float getStickToCharDist() {
            return this.stickToCharDist;
        }

        public void setStickToCharDist(float stickToCharDist) {
            this.stickToCharDist = stickToCharDist;
        }

        /**
         * The base render position for pointers is the center of the screen, adjust this to have it more at feet or head of character for example.
         */
        public float getRenderOffsetX() {
            return this.renderOffsetX;
        }

        public void setRenderOffsetX(float renderOffsetX) {
            this.renderOffsetX = renderOffsetX;
        }

        public float getRenderOffsetY() {
            return this.renderOffsetY;
        }

        public void setRenderOffsetY(float renderOffsetY) {
            this.renderOffsetY = renderOffsetY;
        }

        /**
         * Offset the screen target point, for example to point to top of counter by offsetting Y value
         */
        public float getHomeOnOffsetX() {
            return this.homeOnOffsetX;
        }

        public void setHomeOnOffsetX(float homeOnOffsetX) {
            this.homeOnOffsetX = homeOnOffsetX;
        }

        public float getHomeOnOffsetY() {
            return this.homeOnOffsetY;
        }

        public void setHomeOnOffsetY(float homeOnOffsetY) {
            this.homeOnOffsetY = homeOnOffsetY;
        }

        public void setTableSurface() {
            this.homeOnOffsetY = -30.0F * Core.tileScale;
        }

        public void setHighCounter() {
            this.homeOnOffsetY = -50.0F * Core.tileScale;
        }

        public void setYOffsetScaled(float offset) {
            this.homeOnOffsetY = offset * Core.tileScale;
        }

        public void setXOffsetScaled(float offset) {
            this.homeOnOffsetX = offset * Core.tileScale;
        }
    }

    class PlayerHomingPointList extends ArrayList<WorldMarkers.PlayerHomingPoint> {
        PlayerHomingPointList() {
            Objects.requireNonNull(WorldMarkers.this);
            super();
        }
    }

    class PlayerScreen {
        private float centerX;
        private float centerY;
        private float x;
        private float y;
        private float width;
        private float height;
        private final float padTop;
        private final float padLeft;
        private final float padBot;
        private final float padRight;
        private float innerX;
        private float innerY;
        private float innerX2;
        private float innerY2;
        private final WorldMarkers.Line borderTop;
        private final WorldMarkers.Line borderRight;
        private final WorldMarkers.Line borderBot;
        private final WorldMarkers.Line borderLeft;
        private final WorldMarkers.Line[] borders;

        PlayerScreen() {
            Objects.requireNonNull(WorldMarkers.this);
            super();
            this.padTop = 100.0F;
            this.padLeft = 100.0F;
            this.padBot = 100.0F;
            this.padRight = 100.0F;
            this.borderTop = new WorldMarkers.Line(new WorldMarkers.Point(0.0F, 0.0F), new WorldMarkers.Point(0.0F, 0.0F));
            this.borderRight = new WorldMarkers.Line(new WorldMarkers.Point(0.0F, 0.0F), new WorldMarkers.Point(0.0F, 0.0F));
            this.borderBot = new WorldMarkers.Line(new WorldMarkers.Point(0.0F, 0.0F), new WorldMarkers.Point(0.0F, 0.0F));
            this.borderLeft = new WorldMarkers.Line(new WorldMarkers.Point(0.0F, 0.0F), new WorldMarkers.Point(0.0F, 0.0F));
            this.borders = new WorldMarkers.Line[4];
        }

        private void update(int plrIndex) {
            this.x = 0.0F;
            this.y = 0.0F;
            this.width = IsoCamera.getScreenWidth(plrIndex);
            this.height = IsoCamera.getScreenHeight(plrIndex);
            this.centerX = this.x + this.width / 2.0F;
            this.centerY = this.y + this.height / 2.0F;
            this.innerX = this.x + 100.0F;
            this.innerY = this.y + 100.0F;
            float innerWidth = this.width - 200.0F;
            float innerHeight = this.height - 200.0F;
            this.innerX2 = this.innerX + innerWidth;
            this.innerY2 = this.innerY + innerHeight;
        }

        private WorldMarkers.Line[] getBorders() {
            this.borders[0] = this.getBorderTop();
            this.borders[1] = this.getBorderRight();
            this.borders[2] = this.getBorderBot();
            this.borders[3] = this.getBorderLeft();
            return this.borders;
        }

        private WorldMarkers.Line getBorderTop() {
            this.borderTop.s.set(this.innerX, this.innerY);
            this.borderTop.e.set(this.innerX2, this.innerY);
            return this.borderTop;
        }

        private WorldMarkers.Line getBorderRight() {
            this.borderRight.s.set(this.innerX2, this.innerY);
            this.borderRight.e.set(this.innerX2, this.innerY2);
            return this.borderRight;
        }

        private WorldMarkers.Line getBorderBot() {
            this.borderBot.s.set(this.innerX, this.innerY2);
            this.borderBot.e.set(this.innerX2, this.innerY2);
            return this.borderBot;
        }

        private WorldMarkers.Line getBorderLeft() {
            this.borderLeft.s.set(this.innerX, this.innerY);
            this.borderLeft.e.set(this.innerX, this.innerY2);
            return this.borderLeft;
        }

        private float clampToInnerX(float x) {
            return PZMath.clamp(x, this.innerX, this.innerX2);
        }

        private float clampToInnerY(float y) {
            return PZMath.clamp(y, this.innerY, this.innerY2);
        }

        private boolean isOnScreen(float x, float y) {
            return x >= this.x && x < this.x + this.width && y >= this.y && y < this.y + this.height;
        }

        private boolean isWithinInner(float x, float y) {
            return x >= this.innerX && x < this.innerX2 && y >= this.innerY && y < this.innerY2;
        }
    }

    private static class Point {
        float x;
        float y;

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public WorldMarkers.Point set(float x, float y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public boolean notInfinite() {
            return !Float.isInfinite(this.x) && !Float.isInfinite(this.y);
        }

        @Override
        public String toString() {
            return String.format("{%f, %f}", this.x, this.y);
        }
    }
}
