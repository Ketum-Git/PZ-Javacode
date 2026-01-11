// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import org.joml.Vector2f;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.entity.util.TimSort;
import zombie.iso.IsoCamera;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.sprite.IsoSprite;
import zombie.popman.ObjectPool;
import zombie.util.list.PZArrayList;

public final class FBORenderObjectPicker {
    private static FBORenderObjectPicker instance;
    private final ObjectPool<IsoObjectPicker.ClickObject> clickObjectPool = new ObjectPool<>(IsoObjectPicker.ClickObject::new);
    private final ArrayList<IsoObject> objects = new ArrayList<>();
    private final PZArrayList<IsoObjectPicker.ClickObject> clickObjects = new PZArrayList<>(IsoObjectPicker.ClickObject.class, 128);
    private final PZArrayList<IsoObjectPicker.ClickObject> choices = new PZArrayList<>(IsoObjectPicker.ClickObject.class, 32);
    private final TimSort timSort = new TimSort();
    private final int[] leftSideXy = new int[]{0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3};
    private final int[] rightSideXy = new int[]{0, 0, 1, 0, 1, 1, 2, 1, 2, 2, 3, 2, 3, 3};

    public static FBORenderObjectPicker getInstance() {
        if (instance == null) {
            instance = new FBORenderObjectPicker();
        }

        return instance;
    }

    public IsoObjectPicker.ClickObject ContextPick(int screenX, int screenY) {
        int playerIndex = 0;
        float zoom = Core.getInstance().getZoom(0);
        float x = screenX * zoom;
        float y = screenY * zoom;
        this.clickObjectPool.releaseAll(this.clickObjects);
        this.clickObjects.clear();
        this.getClickObjects(screenX, screenY, this.clickObjects);
        this.choices.clear();

        for (int n = this.clickObjects.size() - 1; n >= 0; n--) {
            IsoObjectPicker.ClickObject clickObject = this.clickObjects.get(n);
            IsoObject object = clickObject.tile;
            if (!(object instanceof IsoPlayer) || object != IsoPlayer.players[0]) {
                IsoSprite sprite = object.sprite;
                if (sprite != null) {
                    float targetAlpha = object.getRenderInfo(0).targetAlpha;
                    if (targetAlpha == 0.0F
                        || (sprite.properties.has(IsoFlagType.cutW) || sprite.properties.has(IsoFlagType.cutN))
                            && !(object instanceof IsoWindow)
                            && !(object instanceof IsoThumpable isoThumpable && isoThumpable.isDoor())
                            && targetAlpha < 1.0F) {
                        continue;
                    }
                }

                if (x > clickObject.x
                    && y > clickObject.y
                    && x <= clickObject.x + clickObject.width
                    && y <= clickObject.y + clickObject.height
                    && !(object instanceof IsoPlayer)) {
                    if (clickObject.scaleX == 1.0F && clickObject.scaleY == 1.0F) {
                        if (object.isMaskClicked((int)(x - clickObject.x), (int)(y - clickObject.y), clickObject.flip)) {
                            if (object.rerouteMask != null) {
                                clickObject.tile = object.rerouteMask;
                            }

                            clickObject.lx = PZMath.fastfloor(x) - clickObject.x;
                            clickObject.ly = PZMath.fastfloor(y) - clickObject.y;
                            this.choices.add(clickObject);
                        }
                    } else {
                        float x1 = clickObject.x + (x - clickObject.x) / clickObject.scaleX;
                        float y1 = clickObject.y + (y - clickObject.y) / clickObject.scaleY;
                        if (object.isMaskClicked((int)(x1 - clickObject.x), (int)(y1 - clickObject.y), clickObject.flip)) {
                            if (object.rerouteMask != null) {
                                clickObject.tile = object.rerouteMask;
                            }

                            clickObject.lx = PZMath.fastfloor(x) - clickObject.x;
                            clickObject.ly = PZMath.fastfloor(y) - clickObject.y;
                            this.choices.add(clickObject);
                        }
                    }
                }
            }
        }

        if (this.choices.isEmpty()) {
            return null;
        } else {
            for (int i = 0; i < this.choices.size(); i++) {
                IsoObjectPicker.ClickObject choice = this.choices.get(i);
                choice.score = choice.calculateScore();
            }

            try {
                this.timSort.doSort(this.choices.getElements(), IsoObjectPicker.comp, 0, this.choices.size());
            } catch (IllegalArgumentException var13) {
                if (Core.debug) {
                    ExceptionLogger.logException(var13);
                }

                return null;
            }

            return this.choices.get(this.choices.size() - 1);
        }
    }

    public void getClickObjects(int screenX, int screenY, PZArrayList<IsoObjectPicker.ClickObject> clickObjects) {
        int playerIndex = 0;
        float zoom = Core.getInstance().getZoom(0);
        float x = screenX * zoom;
        float y = screenY * zoom;
        this.getObjectsAt((int)x, (int)y, this.objects);

        for (int i = 0; i < this.objects.size(); i++) {
            IsoObject object = this.objects.get(i);
            FBORenderLevels renderLevels = object.getSquare().getChunk().getRenderLevels(0);
            FBORenderChunk renderChunk = renderLevels.getFBOForLevel(object.getSquare().z, zoom);
            if (renderChunk != null && !this.handleWaterShader(object, clickObjects)) {
                ObjectRenderInfo renderInfo = object.getRenderInfo(0);
                if (!renderInfo.cutaway
                    || object instanceof IsoWindowFrame windowFrame && !windowFrame.hasWindow()
                    || object instanceof IsoThumpable thumpable && thumpable.isWindow() && object.getSquare().getWindow(thumpable.getNorth()) == null) {
                    IsoObjectPicker.ClickObject clickObject = this.clickObjectPool.alloc();
                    clickObject.tile = object;
                    clickObject.square = object.getSquare();
                    if (renderInfo.layer == ObjectRenderLayer.Translucent || renderInfo.layer == ObjectRenderLayer.TranslucentFloor) {
                        clickObject.x = (int)renderInfo.renderX;
                        clickObject.y = (int)renderInfo.renderY;
                    } else if (DebugOptions.instance.fboRenderChunk.combinedFbo.getValue()) {
                        clickObject.x = (int)(renderChunk.renderX + renderInfo.renderX);
                        clickObject.y = (int)(renderChunk.renderY + renderInfo.renderY);
                    } else {
                        clickObject.x = (int)(renderChunk.renderX * zoom + renderInfo.renderX);
                        clickObject.y = (int)(renderChunk.renderY * zoom + renderInfo.renderY);
                    }

                    clickObject.width = (int)renderInfo.renderWidth;
                    clickObject.height = (int)renderInfo.renderHeight;
                    clickObject.scaleX = (int)renderInfo.renderScaleX;
                    clickObject.scaleY = (int)renderInfo.renderScaleY;
                    clickObject.flip = false;
                    clickObject.score = 0;
                    clickObjects.add(clickObject);
                }
            }
        }

        this.timSort.doSort(clickObjects.getElements(), (o1, o2) -> {
            int c = o1.square.z - o2.square.z;
            if (c != 0) {
                return c;
            } else {
                c = compareRenderLayer(o1, o2);
                return c != 0 ? c : compareSquare(o1, o2);
            }
        }, 0, clickObjects.size());
    }

    boolean handleWaterShader(IsoObject object, PZArrayList<IsoObjectPicker.ClickObject> clickObjects) {
        int playerIndex = 0;
        if (object.getRenderInfo(0).layer != ObjectRenderLayer.None) {
            return false;
        } else if (object.sprite != null && object.sprite.getProperties().has(IsoFlagType.water)) {
            IsoGridSquare square = object.square;
            IsoObjectPicker.ClickObject clickObject = this.clickObjectPool.alloc();
            clickObject.tile = object;
            clickObject.square = square;
            clickObject.x = (int)(IsoUtils.XToScreen(square.x, square.y, square.z, 0) - IsoCamera.frameState.offX - object.offsetX);
            clickObject.y = (int)(IsoUtils.YToScreen(square.x, square.y, square.z, 0) - IsoCamera.frameState.offY - object.offsetY);
            clickObject.width = 64 * Core.tileScale;
            clickObject.height = 128 * Core.tileScale;
            clickObject.scaleX = 1.0F;
            clickObject.scaleY = 1.0F;
            clickObject.flip = false;
            clickObject.score = 0;
            clickObjects.add(clickObject);
            return true;
        } else {
            return false;
        }
    }

    static int compareRenderLayer(IsoObjectPicker.ClickObject o1, IsoObjectPicker.ClickObject o2) {
        return renderLayerIndex(o1) - renderLayerIndex(o2);
    }

    static int renderLayerIndex(IsoObjectPicker.ClickObject o) {
        return switch (o.tile.getRenderInfo(0).layer) {
            case None -> 1000;
            case Floor, TranslucentFloor -> 0;
            case Vegetation -> 1;
            case Corpse -> 2;
            case MinusFloor, Translucent -> 3;
            case WorldInventoryObject -> 4;
            case MinusFloorSE -> 5;
        };
    }

    static int compareSquare(IsoObjectPicker.ClickObject o1, IsoObjectPicker.ClickObject o2) {
        int index1 = o1.square.x + o1.square.y * 100000;
        int index2 = o2.square.x + o2.square.y * 100000;
        return index1 - index2;
    }

    void getObjectsAt(int screenX, int screenY, ArrayList<IsoObject> objects) {
        objects.clear();
        int playerIndex = 0;
        IsoPlayer player = IsoPlayer.players[0];
        if (player.getZ() < 0.0F) {
            for (int z = -32; z < 0; z++) {
                float worldX = IsoUtils.XToIso(screenX, screenY, z);
                float worldY = IsoUtils.YToIso(screenX, screenY, z);
                boolean bRightOfSquare = worldX % 1.0F > worldY % 1.0F;
                int[] dxy = bRightOfSquare ? this.rightSideXy : this.leftSideXy;

                for (int i = 0; i < dxy.length; i += 2) {
                    this.getObjectsOnSquare((int)worldX + dxy[i], (int)worldY + dxy[i + 1], z, objects);
                }

                this.getCorpsesNear(PZMath.fastfloor(worldX), PZMath.fastfloor(worldY), z, objects, bRightOfSquare, screenX, screenY);
            }
        } else {
            for (int z = 0; z <= 31; z++) {
                float worldX = IsoUtils.XToIso(screenX, screenY, z);
                float worldY = IsoUtils.YToIso(screenX, screenY, z);
                boolean bRightOfSquare = PZMath.coordmodulof(worldX, 1) > PZMath.coordmodulof(worldY, 1);
                int[] dxy = bRightOfSquare ? this.rightSideXy : this.leftSideXy;

                for (int i = 0; i < dxy.length; i += 2) {
                    this.getObjectsOnSquare(PZMath.fastfloor(worldX) + dxy[i], PZMath.fastfloor(worldY) + dxy[i + 1], z, objects);
                }

                this.getCorpsesNear(PZMath.fastfloor(worldX), PZMath.fastfloor(worldY), z, objects, bRightOfSquare, screenX, screenY);
            }
        }
    }

    void getObjectsOnSquare(int worldX, int worldY, int z, ArrayList<IsoObject> objects) {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(worldX, worldY, z);
        if (square != null) {
            int playerIndex = 0;
            IsoObject[] objects1 = square.getObjects().getElements();
            int numObjects = square.getObjects().size();

            for (int i = 0; i < numObjects; i++) {
                IsoObject object = objects1[i];
                ObjectRenderInfo renderInfo = object.getRenderInfo(0);
                if (renderInfo.layer != ObjectRenderLayer.None && renderInfo.targetAlpha != 0.0F) {
                    if (!(renderInfo.renderWidth <= 0.0F) && !(renderInfo.renderHeight <= 0.0F)) {
                        objects.add(object);
                    }
                } else if (object.sprite != null && object.sprite.getProperties().has(IsoFlagType.water)) {
                    objects.add(object);
                }
            }
        }
    }

    void getCorpsesNear(int worldX, int worldY, int z, ArrayList<IsoObject> objects, boolean bRightOfSquare, int screenX, int screenY) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                this.getCorpsesNear(worldX + dx, worldY + dy, z, objects, screenX, screenY);
            }
        }
    }

    void getCorpsesNear(int worldX, int worldY, int z, ArrayList<IsoObject> objects, int screenX, int screenY) {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(worldX, worldY, z);
        if (square != null) {
            ArrayList<IsoMovingObject> corpses = square.getStaticMovingObjects();

            for (int i = 0; i < corpses.size(); i++) {
                if (corpses.get(i) instanceof IsoDeadBody deadBody && deadBody.isMouseOver(screenX, screenY)) {
                    objects.add(deadBody);
                }
            }
        }
    }

    IsoObject getFirst(int screenX, int screenY, FBORenderObjectPicker.IObjectPickerPredicate predicate) {
        int playerIndex = 0;
        float zoom = Core.getInstance().getZoom(0);
        float x = screenX * zoom;
        float y = screenY * zoom;
        this.clickObjectPool.releaseAll(this.clickObjects);
        this.clickObjects.clear();
        this.getClickObjects(screenX, screenY, this.clickObjects);

        for (int i = this.clickObjects.size() - 1; i >= 0; i--) {
            IsoObjectPicker.ClickObject clickObject = this.clickObjects.get(i);
            int c = predicate.test(clickObject, x, y);
            if (c == -1) {
                return null;
            }

            if (c == 1) {
                return clickObject.tile;
            }
        }

        return null;
    }

    public IsoObject PickDoor(int screenX, int screenY, boolean bTransparent) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            int playerIndex = 0;
            if (!(obj.tile instanceof IsoDoor)) {
                return 0;
            } else if (obj.tile.getRenderInfo(0).targetAlpha == 0.0F) {
                return 0;
            } else if (bTransparent != obj.tile.getRenderInfo(0).targetAlpha < 1.0F) {
                return 0;
            } else {
                if (obj.contains(x, y)) {
                    int lx = PZMath.fastfloor(x - obj.x);
                    int ly = PZMath.fastfloor(y - obj.y);
                    if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                        return 1;
                    }
                }

                return 0;
            }
        });
    }

    public IsoObject PickWindow(int screenX, int screenY) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            int playerIndex = 0;
            if (!(obj.tile instanceof IsoWindow) && !(obj.tile instanceof IsoCurtain)) {
                return 0;
            } else if (obj.tile.sprite != null && obj.tile.getRenderInfo(0).targetAlpha == 0.0F) {
                return 0;
            } else {
                if (obj.contains(x, y)) {
                    int lx = PZMath.fastfloor(x - obj.x);
                    int ly = PZMath.fastfloor(y - obj.y);
                    if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                        return 1;
                    }

                    if (obj.tile instanceof IsoWindow) {
                        boolean above = false;
                        boolean below = false;

                        for (int ty = ly; ty >= 0; ty--) {
                            if (obj.tile.isMaskClicked(lx, ty)) {
                                above = true;
                                break;
                            }
                        }

                        for (int tyx = ly; tyx < obj.height; tyx++) {
                            if (obj.tile.isMaskClicked(lx, tyx)) {
                                below = true;
                                break;
                            }
                        }

                        if (above && below) {
                            return 1;
                        }
                    }
                }

                return 0;
            }
        });
    }

    public IsoObject PickWindowFrame(int screenX, int screenY) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            int playerIndex = 0;
            if (!(obj.tile instanceof IsoWindowFrame)) {
                return 0;
            } else if (obj.tile.sprite != null && obj.tile.getRenderInfo(0).targetAlpha == 0.0F) {
                return 0;
            } else {
                if (obj.contains(x, y)) {
                    int lx = PZMath.fastfloor(x - obj.x);
                    int ly = PZMath.fastfloor(y - obj.y);
                    if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                        return 1;
                    }

                    boolean above = false;
                    boolean below = false;

                    for (int ty = ly; ty >= 0; ty--) {
                        if (obj.tile.isMaskClicked(lx, ty)) {
                            above = true;
                            break;
                        }
                    }

                    for (int tyx = ly; tyx < obj.height; tyx++) {
                        if (obj.tile.isMaskClicked(lx, tyx)) {
                            below = true;
                            break;
                        }
                    }

                    if (above && below) {
                        return 1;
                    }
                }

                return 0;
            }
        });
    }

    public IsoObject PickThumpable(int screenX, int screenY) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            int playerIndex = 0;
            if (obj.tile instanceof IsoThumpable thump) {
                if (obj.tile.sprite != null && obj.tile.getRenderInfo(0).targetAlpha == 0.0F) {
                    return 0;
                } else {
                    if (obj.contains(x, y)) {
                        int lx = (int)(x - obj.x);
                        int ly = (int)(y - obj.y);
                        if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                            return 1;
                        }

                        if (thump.isWindow()) {
                            boolean above = false;
                            boolean below = false;

                            for (int ty = ly; ty >= 0; ty--) {
                                if (obj.tile.isMaskClicked(lx, ty)) {
                                    above = true;
                                    break;
                                }
                            }

                            for (int tyx = ly; tyx < obj.height; tyx++) {
                                if (obj.tile.isMaskClicked(lx, tyx)) {
                                    below = true;
                                    break;
                                }
                            }

                            if (above && below) {
                                return 1;
                            }
                        }
                    }

                    return 0;
                }
            } else {
                return 0;
            }
        });
    }

    public IsoObject PickHoppable(int screenX, int screenY) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            int playerIndex = 0;
            if (!obj.tile.isHoppable()) {
                return 0;
            } else if (obj.tile.sprite != null && obj.tile.getRenderInfo(0).targetAlpha == 0.0F) {
                return 0;
            } else {
                if (obj.contains(x, y)) {
                    int lx = (int)(x - obj.x);
                    int ly = (int)(y - obj.y);
                    if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                        return 1;
                    }
                }

                return 0;
            }
        });
    }

    public IsoObject PickCorpse(int screenX, int screenY) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            int playerIndex = 0;
            if (obj.tile instanceof IsoDeadBody isoDeadBody) {
                return isoDeadBody.isMouseOver(x, y) ? 1 : 0;
            } else {
                if (obj.contains(x, y)) {
                    if (obj.tile.getRenderInfo(0).targetAlpha < 1.0F) {
                        return 0;
                    }

                    if (obj.tile.isMaskClicked((int)(x - obj.x), (int)(y - obj.y), obj.flip) && !(obj.tile instanceof IsoWindow)) {
                        return -1;
                    }
                }

                return 0;
            }
        });
    }

    public IsoObject PickTree(int screenX, int screenY) {
        return this.getFirst(screenX, screenY, (obj, x, y) -> {
            int playerIndex = 0;
            if (!(obj.tile instanceof IsoTree)) {
                return 0;
            } else if (obj.tile.sprite != null && obj.tile.getRenderInfo(0).targetAlpha == 0.0F) {
                return 0;
            } else {
                if (obj.contains(x, y)) {
                    int lx = (int)(x - obj.x);
                    int ly = (int)(y - obj.y);
                    if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                        return 1;
                    }
                }

                return 0;
            }
        });
    }

    public Vector2f getPointRelativeToTopLeftOfTexture(IsoObject object, int screenX, int screenY, Vector2f out) {
        if (object == null) {
            return null;
        } else if (object.getSprite() == null) {
            return null;
        } else {
            Texture texture = object.getSprite().getTextureForCurrentFrame(object.getDir());
            if (texture == null) {
                return null;
            } else if (object.getSquare() == null) {
                return null;
            } else {
                int playerIndex = 0;
                float zoom = Core.getInstance().getZoom(0);
                FBORenderLevels renderLevels = object.getSquare().getChunk().getRenderLevels(0);
                FBORenderChunk renderChunk = renderLevels.getFBOForLevel(object.getSquare().z, zoom);
                if (renderChunk == null) {
                    return null;
                } else {
                    ObjectRenderInfo renderInfo = object.getRenderInfo(0);
                    int clickObjectX;
                    int clickObjectY;
                    if (renderInfo.layer == ObjectRenderLayer.Translucent || renderInfo.layer == ObjectRenderLayer.TranslucentFloor) {
                        clickObjectX = (int)renderInfo.renderX;
                        clickObjectY = (int)renderInfo.renderY;
                    } else if (DebugOptions.instance.fboRenderChunk.combinedFbo.getValue()) {
                        clickObjectX = (int)(renderChunk.renderX + renderInfo.renderX);
                        clickObjectY = (int)(renderChunk.renderY + renderInfo.renderY);
                    } else {
                        clickObjectX = (int)(renderChunk.renderX * zoom + renderInfo.renderX);
                        clickObjectY = (int)(renderChunk.renderY * zoom + renderInfo.renderY);
                    }

                    out.x = screenX * zoom - clickObjectX - texture.getOffsetX();
                    out.y = screenY * zoom - clickObjectY - texture.getOffsetY();
                    return out;
                }
            }
        }
    }

    public interface IObjectPickerPredicate {
        int test(IsoObjectPicker.ClickObject var1, float var2, float var3);
    }
}
