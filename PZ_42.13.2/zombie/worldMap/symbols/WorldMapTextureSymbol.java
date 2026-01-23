// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.popman.ObjectPool;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapRenderer;

public final class WorldMapTextureSymbol extends WorldMapBaseSymbol {
    static final ObjectPool<WorldMapTextureSymbol> s_pool = new ObjectPool<>(WorldMapTextureSymbol::new);
    private String symbolId;
    Texture texture;
    static Shader sdfShader;

    public WorldMapTextureSymbol() {
    }

    public WorldMapTextureSymbol(WorldMapSymbols owner) {
        super(owner);
    }

    public void setSymbolID(String symbolID) {
        this.symbolId = symbolID;
    }

    public String getSymbolID() {
        return this.symbolId;
    }

    public void checkTexture() {
        if (this.texture == null) {
            MapSymbolDefinitions.MapSymbolDefinition symbolDefinition = MapSymbolDefinitions.getInstance().getSymbolById(this.getSymbolID());
            if (symbolDefinition == null) {
                this.width = 18.0F;
                this.height = 18.0F;
            } else {
                this.texture = Texture.getSharedTexture(symbolDefinition.getTexturePath());
                this.width = symbolDefinition.getWidth();
                this.height = symbolDefinition.getHeight();
            }

            if (this.texture == null) {
                this.texture = Texture.getErrorTexture();
            }
        }
    }

    @Override
    public WorldMapSymbols.WorldMapSymbolType getType() {
        return WorldMapSymbols.WorldMapSymbolType.Texture;
    }

    @Override
    public void layout(UIWorldMap ui, WorldMapSymbolCollisions collisions, float rox, float roy, SymbolLayout layout) {
        this.checkTexture();
        super.layout(ui, collisions, rox, roy, layout);
    }

    @Override
    public void save(ByteBuffer output, SymbolSaveData saveData) throws IOException {
        super.save(output, saveData);
        GameWindow.WriteString(output, this.symbolId);
    }

    @Override
    public void load(ByteBuffer input, SymbolSaveData saveData) throws IOException {
        super.load(input, saveData);
        this.symbolId = GameWindow.ReadString(input);
    }

    @Override
    public void render(WorldMapRenderer.Drawer drawer) {
        if (sdfShader == null) {
            sdfShader = ShaderManager.instance.getOrCreateShader("vboRenderer_SDF", true, false);
        }

        if (sdfShader.getShaderProgram().isCompiled()) {
            SymbolLayout layout = drawer.symbolsLayoutData.getLayout(this);
            if (layout.collided) {
                this.renderCollided(drawer);
            } else {
                this.checkTexture();
                ColorInfo color = this.getColor(drawer, s_tempColorInfo);
                if (Core.debug) {
                }

                double dx = this.width * this.anchorX;
                double dy = this.height * this.anchorY;
                float scale = this.getDisplayScale(drawer);
                if (this.isMatchPerspective()) {
                    scale /= this.getLayoutWorldScale(drawer);
                }

                double pointOfRotationX = this.x;
                double pointOfRotationY = this.y;
                if (!this.isMatchPerspective()) {
                    float uiX = drawer.worldToUIX((float)pointOfRotationX, (float)pointOfRotationY);
                    float uiY = drawer.worldToUIY((float)pointOfRotationX, (float)pointOfRotationY);
                    pointOfRotationX = uiX;
                    pointOfRotationY = uiY;
                }

                DoublePoint leftTop = DoublePointPool.alloc();
                DoublePoint rightTop = DoublePointPool.alloc();
                DoublePoint rightBottom = DoublePointPool.alloc();
                DoublePoint leftBottom = DoublePointPool.alloc();
                double x0 = 0.0 - dx;
                double y0 = 0.0 - dy;
                double x1 = x0 + this.width;
                double y1 = y0 + this.height;
                getAbsolutePosition(x0, y0, pointOfRotationX, pointOfRotationY, this.getCosA(), this.getSinA(), scale, scale, leftTop);
                getAbsolutePosition(x1, y0, pointOfRotationX, pointOfRotationY, this.getCosA(), this.getSinA(), scale, scale, rightTop);
                getAbsolutePosition(x1, y1, pointOfRotationX, pointOfRotationY, this.getCosA(), this.getSinA(), scale, scale, rightBottom);
                getAbsolutePosition(x0, y1, pointOfRotationX, pointOfRotationY, this.getCosA(), this.getSinA(), scale, scale, leftBottom);
                if (this.isMatchPerspective()) {
                    this.worldToUI(drawer, leftTop);
                    this.worldToUI(drawer, rightTop);
                    this.worldToUI(drawer, rightBottom);
                    this.worldToUI(drawer, leftBottom);
                }

                VBORenderer vbor = VBORenderer.getInstance();
                vbor.startRun(vbor.formatPositionColorUv);
                vbor.setMode(7);
                vbor.setTextureID(this.texture.getTextureId());
                vbor.setShaderProgram(sdfShader.getShaderProgram());
                vbor.cmdUseProgram(sdfShader.getShaderProgram());
                vbor.cmdShader1f("sdfThreshold", 0.1F);
                vbor.cmdShader1f("sdfShadow", 0.0F);
                float thick = 0.0F;
                vbor.cmdShader1f("sdfOutlineThick", 0.5F);
                vbor.cmdShader4f("sdfOutlineColor", 0.0F, 0.0F, 0.0F, 0.0F);
                vbor.addQuad(
                    (float)leftTop.x,
                    (float)leftTop.y,
                    this.texture.xStart,
                    this.texture.yStart,
                    (float)rightTop.x,
                    (float)rightTop.y,
                    this.texture.xEnd,
                    this.texture.yStart,
                    (float)rightBottom.x,
                    (float)rightBottom.y,
                    this.texture.xEnd,
                    this.texture.yEnd,
                    (float)leftBottom.x,
                    (float)leftBottom.y,
                    this.texture.xStart,
                    this.texture.yEnd,
                    0.0F,
                    color.r,
                    color.g,
                    color.b,
                    color.a
                );
                vbor.endRun();
                DoublePointPool.release(leftTop);
                DoublePointPool.release(rightTop);
                DoublePointPool.release(rightBottom);
                DoublePointPool.release(leftBottom);
            }
        }
    }

    @Override
    public WorldMapBaseSymbol createCopy() {
        WorldMapTextureSymbol copy = s_pool.alloc();
        return copy.initCopy(this);
    }

    @Override
    protected WorldMapBaseSymbol initCopy(WorldMapBaseSymbol original) {
        super.initCopy(original);
        WorldMapTextureSymbol textureSymbol = (WorldMapTextureSymbol)original;
        this.symbolId = textureSymbol.symbolId;
        this.texture = textureSymbol.texture;
        return this;
    }

    @Override
    public void release() {
        this.symbolId = null;
        this.texture = null;
        this.rotation = 0.0F;
        s_pool.release(this);
    }
}
