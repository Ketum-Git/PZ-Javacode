// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.spriteModel;

import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.model.IsoObjectAnimations;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.iso.SpriteModel;
import zombie.iso.SpriteModelsFile;
import zombie.iso.Vector2;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AnimationsMesh;
import zombie.scripting.objects.ModelScript;

public final class TilesetImageCreator {
    private static final int TILE_WIDTH = 128;
    private static final int TILE_HEIGHT = 256;
    TextureFBO fbo;
    final Matrix4f projection = new Matrix4f();
    final Matrix4f modelview = new Matrix4f();

    public void createImage(String modID, String tilesetName, String filePath) {
        SpriteModelsFile.Tileset tileset = SpriteModelManager.getInstance().findTileset(modID, tilesetName);
        if (tileset != null) {
            int cols = 8;
            int rows = this.getTilesetRows(tilesetName);
            Model[] models = new Model[8 * rows];
            Texture[] textures = new Texture[8 * rows];
            FloatBuffer[] matrixPalettes = new FloatBuffer[8 * rows];
            this.loadModelsEtc(8, rows, tileset, models, textures, matrixPalettes);
            this.fbo = this.createFBO(1024, rows * 256);
            RenderThread.invokeOnRenderContext(() -> {
                this.renderTilesetToFBO(tileset, 8, rows, models, textures, matrixPalettes);
                ((Texture)this.fbo.getTexture()).saveOnRenderThread(filePath);
            });
            this.fbo.destroy();
        }
    }

    int getTilesetRows(String tilesetName) {
        int cols = 8;

        for (int row = 63; row >= 0; row--) {
            for (int col = 0; col < 8; col++) {
                int index = col + row * 8;
                Texture texture = Texture.getSharedTexture(tilesetName + "_" + index);
                if (texture != null) {
                    return row + 1;
                }
            }
        }

        return 0;
    }

    void loadModelsEtc(int cols, int rows, SpriteModelsFile.Tileset tileset, Model[] models, Texture[] textures, FloatBuffer[] matrixPalettes) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                SpriteModelsFile.Tile tile = tileset.getTile(col, row);
                if (tile == null) {
                    textures[col + row * cols] = Texture.getSharedTexture(String.format("%s_%d", tileset.getName(), col + row * cols));
                } else {
                    this.loadModelsEtc(col, row, cols, tile.spriteModel, models, textures, matrixPalettes);
                    if (models[col + row * cols] == null) {
                        textures[col + row * cols] = Texture.getSharedTexture(String.format("%s_%d", tileset.getName(), col + row * cols));
                    }
                }
            }
        }

        while (GameWindow.fileSystem.hasWork()) {
            GameWindow.fileSystem.updateAsyncTransactions();
        }
    }

    void loadModelsEtc(int col, int row, int columns, SpriteModel spriteModel, Model[] models, Texture[] textures, FloatBuffer[] matrixPalettes) {
        ModelScript modelScript = ScriptManager.instance.getModelScript(spriteModel.modelScriptName);
        if (modelScript != null) {
            String meshName = modelScript.getMeshName();
            String texName = modelScript.getTextureName();
            String shaderName = modelScript.getShaderName();
            boolean bStatic = modelScript.isStatic;
            Model model = ModelManager.instance.tryGetLoadedModel(meshName, texName, bStatic, shaderName, true);
            if (model == null && !bStatic && modelScript.animationsMesh != null) {
                AnimationsMesh animationsMesh = ScriptManager.instance.getAnimationsMesh(modelScript.animationsMesh);
                if (animationsMesh != null && animationsMesh.modelMesh != null) {
                    model = ModelManager.instance.loadModel(meshName, texName, animationsMesh.modelMesh, shaderName);
                }
            }

            if (model == null) {
                ModelManager.instance.loadAdditionalModel(meshName, texName, bStatic, shaderName);
                model = ModelManager.instance.getLoadedModel(meshName, texName, bStatic, shaderName);
            }

            if (model != null) {
                if (!model.isFailure()) {
                    Texture texture = null;
                    if (spriteModel.getTextureName() != null) {
                        if (spriteModel.getTextureName().contains("media/")) {
                            texture = Texture.getSharedTexture(spriteModel.getTextureName());
                        } else {
                            texture = Texture.getSharedTexture("media/textures/" + spriteModel.getTextureName() + ".png");
                        }
                    } else if (model.tex != null) {
                        texture = model.tex;
                    }

                    if (texture != null && !texture.isFailure()) {
                        int index = col + row * columns;
                        models[index] = model;
                        textures[index] = texture;
                        matrixPalettes[index] = null;
                        if (!bStatic && spriteModel.getAnimationName() != null) {
                            matrixPalettes[index] = IsoObjectAnimations.getInstance()
                                .getMatrixPaletteForFrame(model, spriteModel.getAnimationName(), spriteModel.getAnimationTime());
                        }
                    }
                }
            }
        }
    }

    TextureFBO createFBO(int width, int height) {
        Texture texture = new Texture(width, height, 16);
        return new TextureFBO(texture, false);
    }

    void renderTilesetToFBO(SpriteModelsFile.Tileset tileset, int cols, int rows, Model[] models, Texture[] textures, FloatBuffer[] matrixPalettes) {
        GL11.glPushAttrib(1048575);
        GL11.glPushClientAttrib(-1);
        GL11.glDepthMask(true);
        GL11.glColorMask(true, true, true, true);
        GL11.glDisable(3089);
        this.fbo.startDrawing(true, true);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = col + row * cols;
                SpriteModelsFile.Tile tile = tileset.getTile(col, row);
                if (tile == null) {
                    this.renderTileToFBO(col, row, textures[index]);
                } else {
                    this.renderTileToFBO(col, row, tile.spriteModel, models[index], textures[index], matrixPalettes[index]);
                }
            }
        }

        this.fbo.endDrawing();
        Texture.lastTextureID = -1;
        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        SpriteRenderer.ringBuffer.restoreVbos = true;
        GL11.glPopAttrib();
        GL11.glPopClientAttrib();
    }

    void renderTileToFBO(int col, int row, Texture texture) {
        if (texture != null) {
            GL11.glViewport(0, 0, this.fbo.getWidth(), this.fbo.getHeight());
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2929);
            GL11.glDisable(2884);
            this.projection.setOrtho2D(0.0F, this.fbo.getWidth(), 0.0F, this.fbo.getHeight());
            this.modelview.identity();
            VBORenderer vbor = VBORenderer.getInstance();
            vbor.cmdPushAndLoadMatrix(5889, this.projection);
            vbor.cmdPushAndLoadMatrix(5888, this.modelview);
            vbor.startRun(vbor.formatPositionColorUv);
            vbor.setMode(7);
            vbor.setTextureID(texture.getTextureId());
            int x = col * 128;
            int y = row * 256;
            vbor.addQuad(
                x + texture.offsetX,
                y + texture.offsetY,
                texture.xStart,
                texture.yStart,
                x + texture.offsetX + texture.getWidth(),
                y + texture.offsetY + texture.getHeight(),
                texture.xEnd,
                texture.yEnd,
                0.0F,
                1.0F,
                1.0F,
                1.0F,
                1.0F
            );
            vbor.endRun();
            vbor.cmdPopMatrix(5889);
            vbor.cmdPopMatrix(5888);
            vbor.flush();
        }
    }

    void renderTileToFBO(int col, int row, SpriteModel spriteModel, Model model, Texture texture, FloatBuffer matrixPalette) {
        if (model.isStatic || matrixPalette != null) {
            if (model.isReady()) {
                if (texture.isReady()) {
                    Shader Effect = model.effect;
                    if (Effect != null && model.mesh != null && model.mesh.isReady()) {
                        int x = col * 128 - (this.fbo.getWidth() - 128) / 2;
                        int y = row * 256 - (this.fbo.getHeight() - 256) / 2;
                        y += 96;
                        GL11.glViewport(x, y, this.fbo.getWidth(), this.fbo.getHeight());
                        this.calcMatrices(this.projection, this.modelview, spriteModel, model);
                        PZGLUtil.pushAndLoadMatrix(5889, this.projection);
                        PZGLUtil.pushAndLoadMatrix(5888, this.modelview);
                        IndieGL.glDefaultBlendFuncA();
                        GL11.glDepthFunc(513);
                        GL11.glDepthMask(true);
                        GL11.glDepthRange(0.0, 1.0);
                        GL11.glEnable(2929);
                        if (Effect.getShaderProgram().getName().contains("door")) {
                            GL11.glEnable(2884);
                            GL11.glCullFace(1029);
                        } else {
                            GL11.glDisable(2884);
                        }

                        Effect.Start();
                        if (texture == null) {
                            Effect.setTexture(Texture.getErrorTexture(), "Texture", 0);
                        } else {
                            if (!texture.getTextureId().hasMipMaps()) {
                                GL11.glBlendFunc(770, 771);
                            }

                            Effect.setTexture(texture, "Texture", 0);
                            if (Effect.getShaderProgram().getName().equalsIgnoreCase("door")) {
                                int widthHW = texture.getWidthHW();
                                int heightHW = texture.getHeightHW();
                                float x1 = texture.xStart * widthHW - texture.offsetX;
                                float y1 = texture.yStart * heightHW - texture.offsetY;
                                float x2 = x1 + texture.getWidthOrig();
                                float y2 = y1 + texture.getHeightOrig();
                                Vector2 tempVector2_1 = new Vector2();
                                Effect.getShaderProgram().setValue("UVOffset", tempVector2_1.set(x1 / widthHW, y1 / heightHW));
                                Effect.getShaderProgram().setValue("UVScale", tempVector2_1.set((x2 - x1) / widthHW, (y2 - y1) / heightHW));
                            }
                        }

                        Effect.setDepthBias(0.0F);
                        Effect.setTargetDepth(0.5F);
                        Effect.setAmbient(1.0F, 1.0F, 1.0F);
                        Effect.setLightingAmount(1.0F);
                        Effect.setHueShift(1.0F);
                        Effect.setTint(1.0F, 1.0F, 1.0F);
                        Effect.setAlpha(1.0F);

                        for (int i = 0; i < 5; i++) {
                            Effect.setLight(i, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Float.NaN, 0.0F, 0.0F, 0.0F, null);
                        }

                        if (model.isStatic) {
                            Effect.setTransformMatrix(new Matrix4f(), false);
                        } else {
                            matrixPalette.position(0);
                            Effect.setMatrixPalette(matrixPalette, true);
                        }

                        model.mesh.Draw(Effect);
                        Effect.End();
                        PZGLUtil.popMatrix(5889);
                        PZGLUtil.popMatrix(5888);
                    }
                }
            }
        }
    }

    void calcMatrices(Matrix4f projection, Matrix4f modelView, SpriteModel spriteModel, Model model) {
        double screenWidth = this.fbo.getWidth() / 1920.0F;
        double screenHeight = this.fbo.getHeight() / 1920.0F;
        projection.setOrtho(-((float)screenWidth) / 2.0F, (float)screenWidth / 2.0F, (float)screenHeight / 2.0F, (float)(-screenHeight) / 2.0F, -1.0F, 1.0F);
        modelView.identity();
        modelView.scale(Core.scale * Core.tileScale / 2.0F);
        modelView.rotate((float) (Math.PI / 6), 1.0F, 0.0F, 0.0F);
        modelView.rotate((float) (Math.PI * 3.0 / 4.0), 0.0F, 1.0F, 0.0F);
        modelView.rotateY((float) Math.PI);
        modelView.scale(-1.5F, 1.5F, 1.5F);
        modelView.translate(-spriteModel.translate.x / 1.5F, spriteModel.translate.y / 1.5F, spriteModel.translate.z / 1.5F);
        modelView.rotateXYZ(
            spriteModel.rotate.x * (float) (Math.PI / 180.0),
            -spriteModel.rotate.y * (float) (Math.PI / 180.0),
            spriteModel.rotate.z * (float) (Math.PI / 180.0)
        );
        modelView.scale(spriteModel.scale);
        ModelScript modelScript = ScriptManager.instance.getModelScript(spriteModel.modelScriptName);
        modelView.scale(modelScript.scale);
        ModelInstanceRenderData.postMultiplyMeshTransform(modelView, model.mesh);
    }
}
