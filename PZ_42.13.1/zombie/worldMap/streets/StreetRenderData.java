// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.streets;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.set.hash.THashSet;
import java.util.ArrayList;
import org.joml.Matrix4f;
import zombie.core.Core;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.textures.TextureID;
import zombie.iso.IsoLot;
import zombie.iso.MapFiles;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapRenderer;

public final class StreetRenderData {
    private static final THashSet<WorldMapStreet> tempStreetSet = new THashSet<>();
    final ArrayList<CharLayout> characters = new ArrayList<>();
    final TFloatArrayList lines = new TFloatArrayList();
    final TFloatArrayList polygon = new TFloatArrayList();
    final TFloatArrayList triangles = new TFloatArrayList();
    float centerWorldX;
    float centerWorldY;
    float worldScale;
    TextureID textureId;
    float sdfThreshold;
    float sdfShadow;
    float sdfOutlineThickness;
    float sdfOutlineR;
    float sdfOutlineG;
    float sdfOutlineB;
    float sdfOutlineA;
    boolean editor;

    public void init(UIWorldMap ui, WorldMapRenderer renderer) {
        WorldMapStreet.s_charLayoutPool.releaseAll(this.characters);
        this.characters.clear();
        this.lines.clear();
        this.worldScale = renderer.getWorldScale(renderer.getDisplayZoomF());
        this.centerWorldX = renderer.getCenterWorldX();
        this.centerWorldY = renderer.getCenterWorldY();
        this.triangles.clear();
        this.editor = ui.isMapEditor();
        if (renderer.getBoolean("ShowStreetNames")) {
            WorldMapStreetsV1 streetsAPI = ui.getAPI().getStreetsAPI();
            if (ui.isMapEditor()) {
                EditStreetsV1 editorAPI = streetsAPI.getEditorAPI();
                WorldMapStreets streets = editorAPI.getStreetData();
                if (streets != null) {
                    streets.render(ui, this);
                }
            } else {
                boolean bEdits = false;

                for (int i = 0; i < streetsAPI.getStreetDataCount(); i++) {
                    WorldMapStreets streets = streetsAPI.getStreetDataByIndex(i);
                    if (streets.checkForEdits()) {
                        bEdits = true;
                    }
                }

                if (bEdits || ui.getWorldMap().combinedStreets.isDirty()) {
                    ui.getWorldMap().combinedStreets.setDirty(false);
                    ui.getWorldMap().combinedStreets.clear();

                    for (int ix = 0; ix < streetsAPI.getStreetDataCount(); ix++) {
                        WorldMapStreets streets = streetsAPI.getStreetDataByIndex(ix);
                        ui.getWorldMap().combinedStreets.combine(streets);
                    }
                }

                ui.getWorldMap().combinedStreets.render(ui, this);
            }

            WorldMapStreet mouseOverStreet = streetsAPI.getMouseOverStreet();
            if (mouseOverStreet != null) {
                if (renderer.getBoolean("HighlightStreet")) {
                    mouseOverStreet.createHighlightPolygons(this.polygon, this.triangles);
                    tempStreetSet.clear();
                    mouseOverStreet.getOwner().getConnectedStreets(mouseOverStreet, tempStreetSet);

                    for (WorldMapStreet street : tempStreetSet) {
                        if (street.isOnScreen(ui)) {
                            street.createHighlightPolygons(this.polygon, this.triangles);
                        }
                    }
                }

                if (ui.isMapEditor()) {
                    this.renderObscuredCells(ui);
                }
            }
        }
    }

    private void renderObscuredCells(UIWorldMap ui) {
        for (MapFiles mapFiles : IsoLot.MapFiles) {
            for (int cellY = mapFiles.minCell300Y; cellY <= mapFiles.maxCell300Y; cellY++) {
                for (int cellX = mapFiles.minCell300X; cellX <= mapFiles.maxCell300X; cellX++) {
                    if (mapFiles.hasCell300(cellX, cellY)) {
                        for (int i = 0; i < mapFiles.priority; i++) {
                            MapFiles mapFiles1 = IsoLot.MapFiles.get(i);
                            if (mapFiles1.hasCell300(cellX, cellY)) {
                                int x1 = cellX * 300;
                                int y1 = cellY * 300;
                                int x2 = (cellX + 1) * 300;
                                int y2 = (cellY + 1) * 300;
                                this.addLine(ui, x1, y1, x2, y1, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F);
                                this.addLine(ui, x2, y1, x2, y2, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F);
                                this.addLine(ui, x2, y2, x1, y2, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F);
                                this.addLine(ui, x1, y2, x1, y1, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addLine(UIWorldMap ui, float x1, float y1, float x2, float y2, float r, float g, float b, float a, float thickness) {
        this.lines.add(ui.getAPI().worldToUIX(x1, y1));
        this.lines.add(ui.getAPI().worldToUIY(x1, y1));
        this.lines.add(ui.getAPI().worldToUIX(x2, y2));
        this.lines.add(ui.getAPI().worldToUIY(x2, y2));
        this.lines.add(r);
        this.lines.add(g);
        this.lines.add(b);
        this.lines.add(a);
        this.lines.add(thickness);
    }

    public void render(WorldMapRenderer renderer, VBORenderer vbor) {
        this.textureId = null;
        this.sdfThreshold = Float.NaN;
        this.sdfShadow = Float.NaN;
        this.sdfOutlineThickness = Float.NaN;
        this.sdfOutlineR = Float.NaN;
        this.sdfOutlineG = Float.NaN;
        this.sdfOutlineB = Float.NaN;
        this.sdfOutlineA = Float.NaN;
        Shader shader = ShaderManager.instance.getOrCreateShader("vboRenderer_SDF", true, false);
        if (shader.getShaderProgram().isCompiled()) {
            float z = 0.0F;
            if (!this.triangles.isEmpty()) {
                float F = 0.66F;
                float r = 0.12941177F;
                float g = 0.33647063F;
                float b = 0.63670594F;
                float a = this.editor ? 0.25F : 1.0F;
                vbor.startRun(vbor.formatPositionColor);
                vbor.setMode(4);

                for (int i = 0; i < this.triangles.size(); i += 6) {
                    int y1 = i + 1;
                    float x0 = (this.triangles.get(i) - this.centerWorldX) * this.worldScale;
                    float y0 = (this.triangles.get(y1++) - this.centerWorldY) * this.worldScale;
                    float x1 = (this.triangles.get(y1++) - this.centerWorldX) * this.worldScale;
                    float y1x = (this.triangles.get(y1++) - this.centerWorldY) * this.worldScale;
                    float x2 = (this.triangles.get(y1++) - this.centerWorldX) * this.worldScale;
                    float y2 = (this.triangles.get(y1) - this.centerWorldY) * this.worldScale;
                    vbor.addTriangle(x0, y0, 0.0F, x1, y1x, 0.0F, x2, y2, 0.0F, 0.12941177F, 0.33647063F, 0.63670594F, a);
                }

                vbor.endRun();
            }

            Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
            PROJECTION.setOrtho2D(0.0F, renderer.getWidth(), renderer.getHeight(), 0.0F);
            vbor.cmdPushAndLoadMatrix(5889, PROJECTION);
            Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
            MODELVIEW.identity();
            vbor.cmdPushAndLoadMatrix(5888, MODELVIEW);
            boolean bRunInProgress = false;

            for (int i = 0; i < this.characters.size(); i++) {
                CharLayout charLayout = this.characters.get(i);
                boolean bStartRun = this.checkShaderUniforms(charLayout);
                if (bStartRun) {
                    if (bRunInProgress) {
                        vbor.endRun();
                    }

                    vbor.startRun(vbor.formatPositionColorUv);
                    vbor.setMode(7);
                    vbor.setShaderProgram(shader.getShaderProgram());
                    this.checkShaderUniforms(charLayout, vbor);
                    bRunInProgress = true;
                }

                float u0 = charLayout.charDef.image.xStart;
                float v0 = charLayout.charDef.image.yStart;
                float u1 = charLayout.charDef.image.xEnd;
                float v1 = charLayout.charDef.image.yStart;
                float u2 = charLayout.charDef.image.xEnd;
                float v2 = charLayout.charDef.image.yEnd;
                float u3 = charLayout.charDef.image.xStart;
                float v3 = charLayout.charDef.image.yEnd;
                vbor.addQuad(
                    (float)charLayout.leftTop[0],
                    (float)charLayout.leftTop[1],
                    u0,
                    v0,
                    (float)charLayout.rightTop[0],
                    (float)charLayout.rightTop[1],
                    u1,
                    v1,
                    (float)charLayout.rightBottom[0],
                    (float)charLayout.rightBottom[1],
                    u2,
                    v2,
                    (float)charLayout.leftBottom[0],
                    (float)charLayout.leftBottom[1],
                    u3,
                    v3,
                    0.0F,
                    charLayout.r,
                    charLayout.g,
                    charLayout.b,
                    charLayout.a
                );
            }

            if (bRunInProgress) {
                vbor.endRun();
            }

            vbor.startRun(vbor.formatPositionColor);
            vbor.setMode(1);

            for (int i = 0; i < this.lines.size() / 9; i++) {
                int n = i * 9;
                float x1 = this.lines.get(n++);
                float y1 = this.lines.get(n++);
                float x2 = this.lines.get(n++);
                float y2 = this.lines.get(n++);
                float r = this.lines.get(n++);
                float g = this.lines.get(n++);
                float b = this.lines.get(n++);
                float a = this.lines.get(n++);
                float thickness = this.lines.get(n++);
                vbor.addLine(x1, y1, 0.0F, x2, y2, 0.0F, r, g, b, a);
            }

            vbor.endRun();
            vbor.cmdPopMatrix(5889);
            vbor.cmdPopMatrix(5888);
            Core.getInstance().projectionMatrixStack.release(PROJECTION);
            Core.getInstance().modelViewMatrixStack.release(MODELVIEW);
            vbor.flush();
        }
    }

    boolean checkShaderUniforms(CharLayout charLayout) {
        if (charLayout.charDef.image.getTextureId() != this.textureId) {
            return true;
        } else if (this.sdfThreshold != charLayout.sdfThreshold) {
            return true;
        } else if (this.sdfShadow != charLayout.sdfShadow) {
            return true;
        } else {
            return this.sdfOutlineThickness != charLayout.outlineThickness
                ? true
                : this.sdfOutlineR != charLayout.outlineR
                    || this.sdfOutlineG != charLayout.outlineG
                    || this.sdfOutlineB != charLayout.outlineB
                    || this.sdfOutlineA != charLayout.outlineA;
        }
    }

    void checkShaderUniforms(CharLayout charLayout, VBORenderer vbor) {
        if (charLayout.charDef.image.getTextureId() != this.textureId) {
            vbor.setTextureID(this.textureId = charLayout.charDef.image.getTextureId());
        }

        if (this.sdfThreshold != charLayout.sdfThreshold) {
            vbor.cmdShader1f("sdfThreshold", this.sdfThreshold = charLayout.sdfThreshold);
        }

        if (this.sdfShadow != charLayout.sdfShadow) {
            vbor.cmdShader1f("sdfShadow", this.sdfShadow = charLayout.sdfShadow);
        }

        if (this.sdfOutlineThickness != charLayout.outlineThickness) {
            vbor.cmdShader1f("sdfOutlineThick", this.sdfOutlineThickness = charLayout.outlineThickness);
        }

        if (this.sdfOutlineR != charLayout.outlineR
            || this.sdfOutlineG != charLayout.outlineG
            || this.sdfOutlineB != charLayout.outlineB
            || this.sdfOutlineA != charLayout.outlineA) {
            vbor.cmdShader4f(
                "sdfOutlineColor",
                this.sdfOutlineR = charLayout.outlineR,
                this.sdfOutlineG = charLayout.outlineG,
                this.sdfOutlineB = charLayout.outlineB,
                this.sdfOutlineA = charLayout.outlineA
            );
        }
    }
}
