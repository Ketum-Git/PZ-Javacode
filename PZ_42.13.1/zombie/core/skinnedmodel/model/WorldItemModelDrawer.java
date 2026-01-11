// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import org.joml.Matrix4f;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderItems;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.sprite.IsoSprite;
import zombie.popman.ObjectPool;

public final class WorldItemModelDrawer extends TextureDraw.GenericDrawer {
    private static final ObjectPool<WorldItemModelDrawer> s_modelDrawerPool = new ObjectPool<>(WorldItemModelDrawer::new);
    private static final ColorInfo tempColorInfo = new ColorInfo();
    private static final Matrix4f s_attachmentXfrm = new Matrix4f();
    public static final ImmutableColor ROTTEN_FOOD_COLOR = new ImmutableColor(0.5F, 0.5F, 0.5F);
    public static final ImmutableColor HIGHLIGHT_COLOR = new ImmutableColor(0.5F, 1.0F, 1.0F);
    public static final boolean NEW_WAY = true;
    private final ItemModelRenderer renderer = new ItemModelRenderer();

    public static ItemModelRenderer.RenderStatus renderMain(
        InventoryItem item, IsoGridSquare square, IsoGridSquare renderSquare, float x, float y, float z, float flipAngle
    ) {
        return renderMain(item, square, renderSquare, x, y, z, flipAngle, -1.0F, false);
    }

    public static ItemModelRenderer.RenderStatus renderMain(
        InventoryItem item,
        IsoGridSquare square,
        IsoGridSquare renderSquare,
        float x,
        float y,
        float z,
        float flipAngle,
        float forcedRotation,
        boolean bIgnoreItemsInChunkTexture
    ) {
        if (item == null || square == null) {
            return ItemModelRenderer.RenderStatus.Failed;
        } else if (!Core.getInstance().isOption3DGroundItem()) {
            return ItemModelRenderer.RenderStatus.NoModel;
        } else if (renderAtlasTexture(item, square, x, y, z, flipAngle, forcedRotation, bIgnoreItemsInChunkTexture)) {
            return ItemModelRenderer.RenderStatus.Ready;
        } else if (!ItemModelRenderer.itemHasModel(item)) {
            return ItemModelRenderer.RenderStatus.NoModel;
        } else {
            WorldItemModelDrawer modelDrawer = s_modelDrawerPool.alloc();
            boolean bRenderToChunkTexture = PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching();
            if (!bIgnoreItemsInChunkTexture && !DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue()) {
                bRenderToChunkTexture = false;
            }

            ItemModelRenderer.RenderStatus status = modelDrawer.renderer
                .renderMain(item, square, renderSquare, x, y, z, flipAngle, forcedRotation, bRenderToChunkTexture);
            if (status == ItemModelRenderer.RenderStatus.Ready) {
                if (item.isDoingExtendedPlacement()) {
                    ExtendedPlacementDrawer epd = ExtendedPlacementDrawer.s_pool.alloc();
                    epd.init(item.getWorldItem(), modelDrawer.renderer.calculateMinModelZ());
                    SpriteRenderer.instance.drawGeneric(epd);
                }

                SpriteRenderer.instance.drawGeneric(modelDrawer);
                return status;
            } else {
                modelDrawer.renderer.reset();
                s_modelDrawerPool.release(modelDrawer);
                return status;
            }
        }
    }

    private static boolean renderAtlasTexture(
        InventoryItem item, IsoGridSquare square, float x, float y, float z, float flipAngle, float forcedRotation, boolean bIgnoreItemsInChunkTexture
    ) {
        if (flipAngle > 0.0F) {
            return false;
        } else if (forcedRotation >= 0.0F) {
            return false;
        } else if (item.isDoingExtendedPlacement()) {
            return false;
        } else {
            boolean bUseAtlas = DebugOptions.instance.worldItemAtlas.enable.getValue();
            boolean bRenderToChunkTexture = PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching();
            if (!bIgnoreItemsInChunkTexture && !DebugOptions.instance.fboRenderChunk.itemsInChunkTexture.getValue()) {
                bRenderToChunkTexture = false;
            }

            if (bRenderToChunkTexture) {
                bUseAtlas = false;
            }

            if (!bUseAtlas) {
                return false;
            } else {
                int playerIndex = IsoCamera.frameState.playerIndex;
                float zoom = Core.getInstance().getZoom(playerIndex);
                if (!PerformanceSettings.fboRenderChunk || !FBORenderChunkManager.instance.isCaching() && FBORenderLevels.getTextureScale(zoom) != 1) {
                    boolean var22 = false;
                } else {
                    boolean var10000 = true;
                }

                boolean bMaxZoomIsOne = !Core.getInstance().getOptionHighResPlacedItems() || zoom >= 0.75F;
                if (item.atlasTexture != null && !item.atlasTexture.isStillValid(item, bMaxZoomIsOne)) {
                    item.atlasTexture = null;
                }

                if (item.atlasTexture == null) {
                    item.atlasTexture = WorldItemAtlas.instance.getItemTexture(item, bMaxZoomIsOne);
                }

                if (item.atlasTexture == null) {
                    return false;
                } else if (item.atlasTexture.isTooBig()) {
                    return false;
                } else {
                    float alpha = IsoWorldInventoryObject.getSurfaceAlpha(square, z - (int)z);
                    if (alpha <= 0.0F) {
                        return true;
                    } else {
                        if (IsoSprite.globalOffsetX == -1.0F) {
                            IsoSprite.globalOffsetX = -IsoCamera.frameState.offX;
                            IsoSprite.globalOffsetY = -IsoCamera.frameState.offY;
                        }

                        float sx = IsoUtils.XToScreen(x, y, z, 0);
                        float sy = IsoUtils.YToScreen(x, y, z, 0);
                        if (FBORenderChunkManager.instance.isCaching()) {
                            sx = IsoUtils.XToScreen(PZMath.coordmodulof(x, 8), PZMath.coordmodulof(y, 8), z, 0);
                            sy = IsoUtils.YToScreen(PZMath.coordmodulof(x, 8), PZMath.coordmodulof(y, 8), z, 0);
                            sx += FBORenderChunkManager.instance.getXOffset();
                            sy += FBORenderChunkManager.instance.getYOffset();
                            TextureDraw.nextZ = IsoDepthHelper.calculateDepth(x + 0.25F, y + 0.25F, z) * 2.0F - 1.0F;
                        } else {
                            sx += IsoSprite.globalOffsetX;
                            sy += IsoSprite.globalOffsetY;
                        }

                        if (PerformanceSettings.fboRenderChunk && !FBORenderChunkManager.instance.isCaching()) {
                            sx += IsoCamera.cameras[playerIndex].fixJigglyModelsX * zoom;
                            sy += IsoCamera.cameras[playerIndex].fixJigglyModelsY * zoom;
                        }

                        square.interpolateLight(tempColorInfo, x % 1.0F, y % 1.0F);
                        if (FBORenderCell.instance.isBlackedOutBuildingSquare(square)) {
                            float fade = 1.0F - FBORenderCell.instance.getBlackedOutRoomFadeRatio(square);
                            tempColorInfo.set(tempColorInfo.r * fade, tempColorInfo.g * fade, tempColorInfo.b * fade, tempColorInfo.a);
                        }

                        if (DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                            tempColorInfo.set(1.0F, 1.0F, 1.0F, tempColorInfo.a);
                        }

                        if (item.getWorldItem() != null && item.getWorldItem().isHighlighted()) {
                            ColorInfo highlightColor = item.getWorldItem().getHighlightColor();
                            tempColorInfo.set(highlightColor.r, highlightColor.g, highlightColor.b, 1.0F);
                        }

                        if (PerformanceSettings.fboRenderChunk && !FBORenderChunkManager.instance.isCaching()) {
                            TextureDraw.nextZ = IsoDepthHelper.getSquareDepthData(
                                            PZMath.fastfloor(IsoCamera.frameState.camCharacterX),
                                            PZMath.fastfloor(IsoCamera.frameState.camCharacterY),
                                            x + 0.25F,
                                            y + 0.25F,
                                            z
                                        )
                                        .depthStart
                                    * 2.0F
                                - 1.0F;
                        }

                        item.atlasTexture.render(x, y, z, sx, sy, tempColorInfo.r, tempColorInfo.g, tempColorInfo.b, alpha);
                        WorldItemAtlas.instance.render();
                        return item.atlasTexture.isRenderMainOK();
                    }
                }
            }
        }
    }

    @Override
    public void render() {
        FBORenderChunk renderChunk = FBORenderChunkManager.instance.renderThreadCurrent;
        if (PerformanceSettings.fboRenderChunk && renderChunk != null) {
            FBORenderItems.getInstance().setCamera(renderChunk, this.renderer.x, this.renderer.y, this.renderer.z, this.renderer.angle);
            this.renderer.DoRender(FBORenderItems.getInstance().getCamera(), true, renderChunk.highRes);
        } else {
            this.renderer.DoRenderToWorld(this.renderer.x, this.renderer.y, this.renderer.z, this.renderer.angle);
        }
    }

    @Override
    public void postRender() {
        this.renderer.reset();
        s_modelDrawerPool.release(this);
    }
}
