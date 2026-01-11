// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.CharacterTrait;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapRenderer;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.styles.WorldMapStyleLayer;
import zombie.worldMap.styles.WorldMapTextStyleLayer;

public final class WorldMapTextSymbol extends WorldMapBaseSymbol {
    static final ObjectPool<WorldMapTextSymbol> s_pool = new ObjectPool<>(WorldMapTextSymbol::new);
    public static final String DEFAULT_LAYER = "text-note";
    public static final UIFont DEFAULT_FONT = UIFont.Handwritten;
    public static final float DEFAULT_FONT_LINEHEIGHT = 40.0F;
    public static final boolean FORCE_SDF_SHADER = true;
    String text;
    boolean translated;
    String layerId = "text-note";
    static Shader sdfShader;

    public WorldMapTextSymbol() {
    }

    public WorldMapTextSymbol(WorldMapSymbols owner) {
        super(owner);
    }

    public void setTranslatedText(String text) {
        this.text = text;
        this.translated = true;
    }

    public void setUntranslatedText(String text) {
        this.text = text;
        this.translated = false;
    }

    public String getTranslatedText() {
        boolean illiterate = LuaManager.GlobalObject.getPlayer() != null && LuaManager.GlobalObject.getPlayer().hasTrait(CharacterTrait.ILLITERATE);
        if (illiterate) {
            String text = this.translated ? this.text : Translator.getText(this.text);
            return text.replaceAll("[^ ]", "?");
        } else {
            return this.translated ? this.text : Translator.getText(this.text);
        }
    }

    public String getUntranslatedText() {
        boolean illiterate = LuaManager.GlobalObject.getPlayer() != null && LuaManager.GlobalObject.getPlayer().hasTrait(CharacterTrait.ILLITERATE);
        if (illiterate) {
            if (this.translated) {
                return null;
            } else {
                String text = this.text;
                return text.replaceAll("[^ ]", "?");
            }
        } else {
            return this.translated ? null : this.text;
        }
    }

    public String getLayerID() {
        return this.layerId;
    }

    public void setLayerID(String layerID) {
        layerID = StringUtils.discardNullOrWhitespace(layerID);
        if (layerID == null) {
            throw new IllegalArgumentException("invalid layer \"%s\"".formatted(layerID));
        } else {
            this.layerId = layerID;
        }
    }

    public WorldMapTextStyleLayer getStyleLayer(UIWorldMap ui) {
        WorldMapStyle style = ui.getAPI().getStyle();
        return style.getTextStyleLayerOrDefault(this.getLayerID());
    }

    public UIFont getFont(UIWorldMap ui) {
        return this.getStyleLayer(ui).getFont();
    }

    @Override
    public WorldMapSymbols.WorldMapSymbolType getType() {
        return WorldMapSymbols.WorldMapSymbolType.Text;
    }

    @Override
    boolean hasCustomColor() {
        return this.a > 0.0F;
    }

    @Override
    public float getDisplayScale(UIWorldMap ui) {
        WorldMapTextStyleLayer textLayer = this.getStyleLayer(ui);
        float scale = textLayer.calculateScale(ui);
        if (this.scale > 0.0F) {
            scale *= this.scale;
        }

        if (this.isApplyZoom()) {
            scale *= ui.getSymbolsLayoutData().getWorldScale();
        }

        return scale;
    }

    @Override
    public float getDisplayScale(WorldMapRenderer.Drawer drawer) {
        SymbolLayout layout = drawer.symbolsLayoutData.getLayout(this);
        WorldMapTextStyleLayer textLayer = layout.textLayout.textLayer;
        float scale = textLayer.calculateScale(drawer);
        if (this.scale > 0.0F) {
            scale *= this.scale;
        }

        if (this.isApplyZoom()) {
            scale *= this.getLayoutWorldScale(drawer);
        }

        return scale;
    }

    @Override
    public void layout(UIWorldMap ui, WorldMapSymbolCollisions collisions, float rox, float roy, SymbolLayout layout) {
        WorldMapStyle style = ui.getAPI().getStyle();
        WorldMapTextStyleLayer textLayer = style.getTextStyleLayerOrDefault(this.getLayerID());
        layout.textLayout.set(this.getTranslatedText(), textLayer);
        super.layout(ui, collisions, rox, roy, layout);
    }

    @Override
    public float widthScaled(UIWorldMap ui) {
        SymbolLayout layout = ui.getSymbolsLayoutData().getLayout(this);
        layout.textLayout.set(this.getTranslatedText(), this.getStyleLayer(ui));
        this.width = layout.textLayout.maxLineLength;
        return this.width * this.getDisplayScale(ui);
    }

    @Override
    public float heightScaled(UIWorldMap ui) {
        SymbolLayout layout = ui.getSymbolsLayoutData().getLayout(this);
        layout.textLayout.set(this.getTranslatedText(), this.getStyleLayer(ui));
        int lineHeight = TextManager.instance.getFontHeight(layout.textLayout.textLayer.getFont());
        this.height = layout.textLayout.numLines * lineHeight;
        return this.height * this.getDisplayScale(ui);
    }

    @Override
    public boolean isVisible(UIWorldMap ui) {
        return ui.getSymbolsLayoutData().getMiniMapSymbols() ? false : super.isVisible(ui);
    }

    @Override
    public void save(ByteBuffer output, SymbolSaveData saveData) throws IOException {
        super.save(output, saveData);
        GameWindow.WriteString(output, this.text);
        output.put((byte)(this.translated ? 1 : 0));
        int fontIndex = saveData.fontNameToIndex.get(this.getLayerID());
        output.put((byte)fontIndex);
    }

    @Override
    public void load(ByteBuffer input, SymbolSaveData saveData) throws IOException {
        super.load(input, saveData);
        this.text = GameWindow.ReadString(input);
        this.translated = input.get() == 1;
        if (saveData.symbolsVersion >= 2) {
            int fontIndex = input.get() & 255;
            this.layerId = saveData.indexToFontName.getOrDefault(fontIndex, "text-note");
        }
    }

    @Override
    public void render(WorldMapRenderer.Drawer drawer) {
        SymbolLayout layout = drawer.symbolsLayoutData.getLayout(this);
        if (layout.collided) {
            this.renderCollided(drawer);
        } else {
            if (Core.debug) {
            }

            if (Core.debug) {
            }

            float radians = this.getRotation() * (float) (Math.PI / 180.0);
            ColorInfo col = this.getColor(drawer, s_tempColorInfo);
            WorldMapTextStyleLayer textLayer = layout.textLayout.textLayer;
            WorldMapStyleLayer.RGBAf rgbaf = textLayer.evalColor(drawer.zoomF, layout.textLayout.textLayer.fill);
            if (this.hasCustomColor()) {
                rgbaf.init(this.r, this.g, this.b, rgbaf.a);
            }

            if (this.isPrivate() && drawer.renderer.isDimUnsharedSymbols()) {
                rgbaf.a = 0.25F;
            }

            if (rgbaf.a < 0.25F) {
                if (drawer.symbolsRenderData.mapEditor) {
                    rgbaf.a = 0.25F;
                } else if (drawer.symbolsRenderData.userEditing && this.isUserDefined()) {
                    rgbaf.a = 0.25F;
                }
            }

            if (rgbaf.a <= 0.0F) {
                WorldMapStyleLayer.RGBAf.s_pool.release(rgbaf);
                return;
            }

            this.DrawTextRotated(
                drawer,
                textLayer.getFont(),
                this.getTranslatedText(),
                this.x,
                this.y,
                radians,
                this.getDisplayScale(drawer),
                rgbaf.r,
                rgbaf.g,
                rgbaf.b,
                rgbaf.a
            );
            WorldMapStyleLayer.RGBAf.s_pool.release(rgbaf);
        }
    }

    @Override
    public WorldMapBaseSymbol createCopy() {
        WorldMapTextSymbol copy = s_pool.alloc();
        return copy.initCopy(this);
    }

    @Override
    protected WorldMapBaseSymbol initCopy(WorldMapBaseSymbol original) {
        super.initCopy(original);
        WorldMapTextSymbol textSymbol = (WorldMapTextSymbol)original;
        this.text = textSymbol.text;
        this.translated = textSymbol.translated;
        this.layerId = textSymbol.layerId;
        return this;
    }

    @Override
    public void release() {
        this.text = null;
        this.rotation = 0.0F;
        s_pool.release(this);
    }

    public static double getSdfThreshold(double cosA, double sinA, double scale) {
        double pointOfRotationX = 0.0;
        double pointOfRotationY = 0.0;
        DoublePoint leftTop = getAbsolutePosition(-5.0, 0.0, 0.0, 0.0, cosA, sinA, scale, scale, DoublePointPool.alloc());
        DoublePoint rightTop = getAbsolutePosition(5.0, 0.0, 0.0, 0.0, cosA, sinA, scale, scale, DoublePointPool.alloc());
        double distance = Math.hypot(leftTop.x - rightTop.x, leftTop.y - rightTop.y);
        double threshold = (float)(0.125 / (distance / 10.0));
        DoublePointPool.release(leftTop);
        DoublePointPool.release(rightTop);
        return threshold;
    }

    double getSdfThreshold(WorldMapRenderer.Drawer drawer) {
        float scale = this.getDisplayScale(drawer);
        return getSdfThreshold(this.getCosA(), this.getSinA(), scale);
    }

    private void DrawTextRotated(
        WorldMapRenderer.Drawer drawer,
        UIFont uiFont,
        String text,
        float pointOfRotationX,
        float pointOfRotationY,
        float radians,
        float scale,
        float r,
        float g,
        float b,
        float a
    ) {
        if (sdfShader == null) {
            sdfShader = ShaderManager.instance.getOrCreateShader("vboRenderer_SDF", true, false);
        }

        if (sdfShader.getShaderProgram().isCompiled()) {
            if (this.isMatchPerspective()) {
                scale /= this.getLayoutWorldScale(drawer);
            }

            if (!this.isMatchPerspective()) {
                float uiX = drawer.worldToUIX(pointOfRotationX, pointOfRotationY);
                float uiY = drawer.worldToUIY(pointOfRotationX, pointOfRotationY);
                pointOfRotationX = uiX;
                pointOfRotationY = uiY;
            }

            double cosA = Math.cos(radians);
            double sinA = Math.sin(radians);
            AngelCodeFont font = TextManager.instance.getFontFromEnum(uiFont);
            AngelCodeFont.CharDef lastCharDef = null;
            double dx = this.width * this.anchorX;
            double dy = this.height * this.anchorY;
            DoublePoint leftTop = DoublePointPool.alloc();
            DoublePoint rightTop = DoublePointPool.alloc();
            DoublePoint rightBottom = DoublePointPool.alloc();
            DoublePoint leftBottom = DoublePointPool.alloc();
            SymbolLayout layout = drawer.symbolsLayoutData.getLayout(this);
            TextLayout textLayout = layout.textLayout;

            for (int i = 0; i < textLayout.numLines; i++) {
                double tx = textLayout.getLineOffsetX(i);
                double ty = i * font.getLineHeight();
                int start = textLayout.getFirstChar(i);
                int end = textLayout.getLastChar(i);

                for (int j = start; j <= end; j++) {
                    char ch = textLayout.textWithoutFormatting.charAt(j);
                    if (ch != '\n') {
                        if (ch >= font.chars.length) {
                            ch = '?';
                        }

                        AngelCodeFont.CharDef charDef = font.chars[ch];
                        if (charDef != null) {
                            if (lastCharDef != null) {
                                tx += lastCharDef.getKerning(ch);
                            }

                            if (charDef.width > 0 && charDef.height > 0) {
                                double x0 = tx + charDef.xoffset - dx;
                                double y0 = ty + charDef.yoffset - dy;
                                double x1 = x0 + charDef.width;
                                double y1 = y0 + charDef.height;
                                getAbsolutePosition(x0, y0, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, leftTop);
                                getAbsolutePosition(x1, y0, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, rightTop);
                                getAbsolutePosition(x1, y1, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, rightBottom);
                                getAbsolutePosition(x0, y1, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, leftBottom);
                                if (this.isMatchPerspective()) {
                                    this.worldToUI(drawer, leftTop);
                                    this.worldToUI(drawer, rightTop);
                                    this.worldToUI(drawer, rightBottom);
                                    this.worldToUI(drawer, leftBottom);
                                    float scale1 = scale * drawer.worldScale;
                                }

                                VBORenderer vbor = VBORenderer.getInstance();
                                vbor.startRun(vbor.formatPositionColorUv);
                                vbor.setMode(7);
                                Texture tex = charDef.image;
                                vbor.setTextureID(tex.getTextureId());
                                if (!TextManager.instance.isSdf(uiFont)) {
                                }

                                vbor.setShaderProgram(sdfShader.getShaderProgram());
                                vbor.cmdUseProgram(sdfShader.getShaderProgram());
                                vbor.cmdShader1f("sdfThreshold", (float)this.getSdfThreshold(drawer));
                                vbor.cmdShader1f("sdfShadow", 0.0F);
                                float thick = 0.0F;
                                if (TextManager.instance.isSdf(uiFont) && r == 1.0F && g == 1.0F && b == 1.0F) {
                                    thick = 0.1F;
                                }

                                vbor.cmdShader1f("sdfOutlineThick", (1.0F - thick) / 2.0F);
                                vbor.cmdShader4f("sdfOutlineColor", 1.0F - r, 1.0F - g, 1.0F - b, thick > 0.0F ? a : 0.0F);
                                vbor.addQuad(
                                    (float)leftTop.x,
                                    (float)leftTop.y,
                                    tex.xStart,
                                    tex.yStart,
                                    (float)rightTop.x,
                                    (float)rightTop.y,
                                    tex.xEnd,
                                    tex.yStart,
                                    (float)rightBottom.x,
                                    (float)rightBottom.y,
                                    tex.xEnd,
                                    tex.yEnd,
                                    (float)leftBottom.x,
                                    (float)leftBottom.y,
                                    tex.xStart,
                                    tex.yEnd,
                                    0.0F,
                                    r,
                                    g,
                                    b,
                                    a
                                );
                                vbor.endRun();
                            }

                            lastCharDef = charDef;
                            tx += charDef.xadvance;
                        }
                    }
                }
            }

            DoublePointPool.release(leftTop);
            DoublePointPool.release(rightTop);
            DoublePointPool.release(rightBottom);
            DoublePointPool.release(leftBottom);
        }
    }
}
