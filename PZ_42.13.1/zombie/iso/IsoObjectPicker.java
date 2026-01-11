// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoSurvivor;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.entity.ComponentType;
import zombie.entity.components.spriteconfig.SpriteConfig;
import zombie.entity.components.ui.UiConfig;
import zombie.input.Mouse;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.fboRenderChunk.FBORenderObjectPicker;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.sprite.IsoSprite;
import zombie.scripting.ui.XuiSkin;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public final class IsoObjectPicker {
    public static final IsoObjectPicker Instance = new IsoObjectPicker();
    static final ArrayList<IsoObjectPicker.ClickObject> choices = new ArrayList<>();
    static final Vector2 tempo = new Vector2();
    static final Vector2 tempo2 = new Vector2();
    public static final Comparator<IsoObjectPicker.ClickObject> comp = new Comparator<IsoObjectPicker.ClickObject>() {
        public int compare(IsoObjectPicker.ClickObject a, IsoObjectPicker.ClickObject b) {
            int aScore = a.getScore();
            int bScore = b.getScore();
            if (aScore > bScore) {
                return 1;
            } else if (aScore < bScore) {
                return -1;
            } else {
                return a.tile != null && a.tile.square != null && b.tile != null && a.tile.square == b.tile.square
                    ? a.tile.getObjectIndex() - b.tile.getObjectIndex()
                    : 0;
            }
        }
    };
    public IsoObjectPicker.ClickObject[] clickObjectStore = new IsoObjectPicker.ClickObject[15000];
    public int count;
    public int counter;
    public int maxcount;
    public final ArrayList<IsoObjectPicker.ClickObject> thisFrame = new ArrayList<>();
    public boolean dirty = true;
    public float xOffSinceDirty;
    public float yOffSinceDirty;
    public boolean wasDirty;
    IsoObjectPicker.ClickObject lastPickObject;
    float lx;
    float ly;

    public IsoObjectPicker getInstance() {
        return Instance;
    }

    public void Add(int x, int y, int width, int height, IsoGridSquare gridSquare, IsoObject tile, boolean flip, float scaleX, float scaleY) {
        if (!(x + width <= this.lx - 32.0F) && !(x >= this.lx + 32.0F) && !(y + height <= this.ly - 32.0F) && !(y >= this.ly + 32.0F)) {
            if (this.thisFrame.size() < 15000) {
                if (!tile.noPicking) {
                    if (tile instanceof IsoSurvivor) {
                        boolean obj = false;
                    }

                    if (tile instanceof IsoDoor) {
                        boolean var11 = false;
                    }

                    if (x <= Core.getInstance().getOffscreenWidth(0)) {
                        if (y <= Core.getInstance().getOffscreenHeight(0)) {
                            if (x + width >= 0) {
                                if (y + height >= 0) {
                                    IsoObjectPicker.ClickObject obj = this.clickObjectStore[this.thisFrame.size()];
                                    this.thisFrame.add(obj);
                                    this.count = this.thisFrame.size();
                                    obj.x = x;
                                    obj.y = y;
                                    obj.width = width;
                                    obj.height = height;
                                    obj.square = gridSquare;
                                    obj.tile = tile;
                                    obj.flip = flip;
                                    obj.scaleX = scaleX;
                                    obj.scaleY = scaleY;
                                    if (obj.tile instanceof IsoGameCharacter) {
                                        obj.flip = false;
                                    }

                                    if (this.count > this.maxcount) {
                                        this.maxcount = this.count;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void Init() {
        this.thisFrame.clear();
        this.lastPickObject = null;

        for (int n = 0; n < 15000; n++) {
            this.clickObjectStore[n] = new IsoObjectPicker.ClickObject();
        }
    }

    public IsoObjectPicker.ClickObject ContextPick(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().ContextPick(screenX, screenY);
        } else {
            float x = screenX * Core.getInstance().getZoom(0);
            float y = screenY * Core.getInstance().getZoom(0);
            choices.clear();
            this.counter++;

            for (int n = this.thisFrame.size() - 1; n >= 0; n--) {
                IsoObjectPicker.ClickObject obj = this.thisFrame.get(n);
                if ((!(obj.tile instanceof IsoPlayer) || obj.tile != IsoPlayer.players[0])
                    && (
                        obj.tile.sprite == null
                            || obj.tile.getTargetAlpha(0) != 0.0F
                                && (
                                    !obj.tile.sprite.properties.has(IsoFlagType.cutW) && !obj.tile.sprite.properties.has(IsoFlagType.cutN)
                                        || obj.tile instanceof IsoWindow
                                        || obj.tile instanceof IsoThumpable isoThumpable && isoThumpable.isDoor()
                                        || !(obj.tile.getTargetAlpha(0) < 1.0F)
                                )
                    )) {
                    if (obj.tile != null && obj.tile.sprite != null) {
                    }

                    if (x > obj.x && y > obj.y && x <= obj.x + obj.width && y <= obj.y + obj.height && !(obj.tile instanceof IsoPlayer)) {
                        if (obj.scaleX == 1.0F && obj.scaleY == 1.0F) {
                            if (obj.tile.isMaskClicked((int)(x - obj.x), (int)(y - obj.y), obj.flip)) {
                                if (obj.tile.rerouteMask != null) {
                                    obj.tile = obj.tile.rerouteMask;
                                }

                                obj.lx = PZMath.fastfloor(x) - obj.x;
                                obj.ly = PZMath.fastfloor(y) - obj.y;
                                this.lastPickObject = obj;
                                choices.add(obj);
                            }
                        } else {
                            float x1 = obj.x + (x - obj.x) / obj.scaleX;
                            float y1 = obj.y + (y - obj.y) / obj.scaleY;
                            if (obj.tile.isMaskClicked((int)(x1 - obj.x), (int)(y1 - obj.y), obj.flip)) {
                                if (obj.tile.rerouteMask != null) {
                                    obj.tile = obj.tile.rerouteMask;
                                }

                                obj.lx = PZMath.fastfloor(x) - obj.x;
                                obj.ly = PZMath.fastfloor(y) - obj.y;
                                this.lastPickObject = obj;
                                choices.add(obj);
                            }
                        }
                    }
                }
            }

            if (choices.isEmpty()) {
                return null;
            } else {
                for (int i = 0; i < choices.size(); i++) {
                    IsoObjectPicker.ClickObject choice = choices.get(i);
                    choice.score = choice.calculateScore();
                }

                try {
                    Collections.sort(choices, comp);
                } catch (IllegalArgumentException var9) {
                    if (Core.debug) {
                        ExceptionLogger.logException(var9);
                    }

                    return null;
                }

                return choices.get(choices.size() - 1);
            }
        }
    }

    public IsoObjectPicker.ClickObject Pick(int xx, int yy) {
        float x = xx;
        float y = yy;
        float width = Core.getInstance().getScreenWidth();
        float height = Core.getInstance().getScreenHeight();
        float targetScreenWidth = width * Core.getInstance().getZoom(0);
        float targetScreenHeight = height * Core.getInstance().getZoom(0);
        float offscreenBWidth = Core.getInstance().getOffscreenWidth(0);
        float offscreenBHeight = Core.getInstance().getOffscreenHeight(0);
        float delX = offscreenBWidth / targetScreenWidth;
        float delY = offscreenBHeight / targetScreenHeight;
        x -= width / 2.0F;
        y -= height / 2.0F;
        x /= delX;
        y /= delY;
        x += width / 2.0F;
        y += height / 2.0F;
        this.counter++;

        for (int n = this.thisFrame.size() - 1; n >= 0; n--) {
            IsoObjectPicker.ClickObject obj = this.thisFrame.get(n);
            if (obj.tile.square != null) {
            }

            if (!(obj.tile instanceof IsoPlayer) && (obj.tile.sprite == null || obj.tile.getTargetAlpha(0) != 0.0F)) {
                if (obj.tile != null && obj.tile.sprite != null) {
                }

                if (x > obj.x && y > obj.y && x <= obj.x + obj.width && y <= obj.y + obj.height) {
                    if (obj.tile instanceof IsoSurvivor) {
                        int dd = 0;
                    } else if (obj.tile.isMaskClicked((int)(x - obj.x), (int)(y - obj.y), obj.flip)) {
                        if (obj.tile.rerouteMask != null) {
                            obj.tile = obj.tile.rerouteMask;
                        }

                        obj.lx = PZMath.fastfloor(x) - obj.x;
                        obj.ly = PZMath.fastfloor(y) - obj.y;
                        this.lastPickObject = obj;
                        return obj;
                    }
                }
            }
        }

        return null;
    }

    public void StartRender() {
        float x = Mouse.getX();
        float y = Mouse.getY();
        if (x != this.lx || y != this.ly) {
            this.dirty = true;
        }

        this.lx = x;
        this.ly = y;
        if (this.dirty) {
            this.thisFrame.clear();
            this.count = 0;
            this.wasDirty = true;
            this.dirty = false;
            this.xOffSinceDirty = 0.0F;
            this.yOffSinceDirty = 0.0F;
        } else {
            this.wasDirty = false;
        }
    }

    public IsoMovingObject PickTarget(int xx, int yy) {
        float x = xx;
        float y = yy;
        float width = Core.getInstance().getScreenWidth();
        float height = Core.getInstance().getScreenHeight();
        float targetScreenWidth = width * Core.getInstance().getZoom(0);
        float targetScreenHeight = height * Core.getInstance().getZoom(0);
        float offscreenBWidth = Core.getInstance().getOffscreenWidth(0);
        float offscreenBHeight = Core.getInstance().getOffscreenHeight(0);
        float delX = offscreenBWidth / targetScreenWidth;
        float delY = offscreenBHeight / targetScreenHeight;
        x -= width / 2.0F;
        y -= height / 2.0F;
        x /= delX;
        y /= delY;
        x += width / 2.0F;
        y += height / 2.0F;
        this.counter++;

        for (int n = this.thisFrame.size() - 1; n >= 0; n--) {
            IsoObjectPicker.ClickObject obj = this.thisFrame.get(n);
            if (obj.tile.square != null) {
            }

            if (obj.tile != IsoPlayer.getInstance() && (obj.tile.sprite == null || obj.tile.getTargetAlpha() != 0.0F)) {
                if (obj.tile != null && obj.tile.sprite != null) {
                }

                if (x > obj.x
                    && y > obj.y
                    && x <= obj.x + obj.width
                    && y <= obj.y + obj.height
                    && obj.tile instanceof IsoMovingObject isoMovingObject
                    && obj.tile.isMaskClicked(PZMath.fastfloor(x - obj.x), PZMath.fastfloor(y - obj.y), obj.flip)) {
                    if (obj.tile.rerouteMask != null) {
                    }

                    obj.lx = PZMath.fastfloor(x - obj.x);
                    obj.ly = PZMath.fastfloor(y - obj.y);
                    this.lastPickObject = obj;
                    return isoMovingObject;
                }
            }
        }

        return null;
    }

    public IsoObject PickDoor(int screenX, int screenY, boolean bTransparent) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickDoor(screenX, screenY, bTransparent);
        } else {
            float x = screenX * Core.getInstance().getZoom(0);
            float y = screenY * Core.getInstance().getZoom(0);
            int playerIndex = IsoPlayer.getPlayerIndex();

            for (int n = this.thisFrame.size() - 1; n >= 0; n--) {
                IsoObjectPicker.ClickObject obj = this.thisFrame.get(n);
                if (obj.tile instanceof IsoDoor
                    && obj.tile.getTargetAlpha(playerIndex) != 0.0F
                    && bTransparent == obj.tile.getTargetAlpha(playerIndex) < 1.0F
                    && x >= obj.x
                    && y >= obj.y
                    && x < obj.x + obj.width
                    && y < obj.y + obj.height) {
                    int lx = PZMath.fastfloor(x - obj.x);
                    int ly = PZMath.fastfloor(y - obj.y);
                    if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                        return obj.tile;
                    }
                }
            }

            return null;
        }
    }

    public IsoObject PickWindow(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickWindow(screenX, screenY);
        } else {
            float x = screenX * Core.getInstance().getZoom(0);
            float y = screenY * Core.getInstance().getZoom(0);

            for (int n = this.thisFrame.size() - 1; n >= 0; n--) {
                IsoObjectPicker.ClickObject obj = this.thisFrame.get(n);
                if ((obj.tile instanceof IsoWindow || obj.tile instanceof IsoCurtain)
                    && (obj.tile.sprite == null || obj.tile.getTargetAlpha() != 0.0F)
                    && x >= obj.x
                    && y >= obj.y
                    && x < obj.x + obj.width
                    && y < obj.y + obj.height) {
                    int lx = PZMath.fastfloor(x - obj.x);
                    int ly = PZMath.fastfloor(y - obj.y);
                    if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                        return obj.tile;
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
                            return obj.tile;
                        }
                    }
                }
            }

            return null;
        }
    }

    public IsoObject PickWindowFrame(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickWindowFrame(screenX, screenY);
        } else {
            float x = screenX * Core.getInstance().getZoom(0);
            float y = screenY * Core.getInstance().getZoom(0);

            for (int n = this.thisFrame.size() - 1; n >= 0; n--) {
                IsoObjectPicker.ClickObject obj = this.thisFrame.get(n);
                if (obj.tile instanceof IsoWindowFrame
                    && (obj.tile.sprite == null || obj.tile.getTargetAlpha() != 0.0F)
                    && x >= obj.x
                    && y >= obj.y
                    && x < obj.x + obj.width
                    && y < obj.y + obj.height) {
                    int lx = PZMath.fastfloor(x - obj.x);
                    int ly = PZMath.fastfloor(y - obj.y);
                    if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                        return obj.tile;
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
                        return obj.tile;
                    }
                }
            }

            return null;
        }
    }

    public IsoObject PickThumpable(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickThumpable(screenX, screenY);
        } else {
            float x = screenX * Core.getInstance().getZoom(0);
            float y = screenY * Core.getInstance().getZoom(0);

            for (int n = this.thisFrame.size() - 1; n >= 0; n--) {
                IsoObjectPicker.ClickObject obj = this.thisFrame.get(n);
                if (obj.tile instanceof IsoThumpable thump
                    && (obj.tile.sprite == null || obj.tile.getTargetAlpha() != 0.0F)
                    && x >= obj.x
                    && y >= obj.y
                    && x < obj.x + obj.width
                    && y < obj.y + obj.height) {
                    int lx = (int)(x - obj.x);
                    int ly = (int)(y - obj.y);
                    if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                        return obj.tile;
                    }
                }
            }

            return null;
        }
    }

    public IsoObject PickHoppable(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickHoppable(screenX, screenY);
        } else {
            float x = screenX * Core.getInstance().getZoom(0);
            float y = screenY * Core.getInstance().getZoom(0);

            for (int n = this.thisFrame.size() - 1; n >= 0; n--) {
                IsoObjectPicker.ClickObject obj = this.thisFrame.get(n);
                if (obj.tile.isHoppable()
                    && (obj.tile.sprite == null || obj.tile.getTargetAlpha() != 0.0F)
                    && x >= obj.x
                    && y >= obj.y
                    && x < obj.x + obj.width
                    && y < obj.y + obj.height) {
                    int lx = (int)(x - obj.x);
                    int ly = (int)(y - obj.y);
                    if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                        return obj.tile;
                    }
                }
            }

            return null;
        }
    }

    public IsoObject PickCorpse(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickCorpse(screenX, screenY);
        } else {
            float x = screenX * Core.getInstance().getZoom(0);
            float y = screenY * Core.getInstance().getZoom(0);

            for (int n = this.thisFrame.size() - 1; n >= 0; n--) {
                IsoObjectPicker.ClickObject obj = this.thisFrame.get(n);
                if (x >= obj.x && y >= obj.y && x < obj.x + obj.width && y < obj.y + obj.height && !(obj.tile.getTargetAlpha() < 1.0F)) {
                    if (obj.tile.isMaskClicked((int)(x - obj.x), (int)(y - obj.y), obj.flip) && !(obj.tile instanceof IsoWindow)) {
                        return null;
                    }

                    if (obj.tile instanceof IsoDeadBody isoDeadBody && isoDeadBody.isMouseOver(x, y)) {
                        return obj.tile;
                    }
                }
            }

            return null;
        }
    }

    public IsoObject PickTree(int screenX, int screenY) {
        if (PerformanceSettings.fboRenderChunk) {
            return FBORenderObjectPicker.getInstance().PickTree(screenX, screenY);
        } else {
            float x = screenX * Core.getInstance().getZoom(0);
            float y = screenY * Core.getInstance().getZoom(0);

            for (int n = this.thisFrame.size() - 1; n >= 0; n--) {
                IsoObjectPicker.ClickObject obj = this.thisFrame.get(n);
                if (obj.tile instanceof IsoTree
                    && (obj.tile.sprite == null || obj.tile.getTargetAlpha() != 0.0F)
                    && x >= obj.x
                    && y >= obj.y
                    && x < obj.x + obj.width
                    && y < obj.y + obj.height) {
                    int lx = (int)(x - obj.x);
                    int ly = (int)(y - obj.y);
                    if (obj.tile.isMaskClicked(lx, ly, obj.flip)) {
                        return obj.tile;
                    }
                }
            }

            return null;
        }
    }

    public BaseVehicle PickVehicle(int screenX, int screenY) {
        int z = PZMath.fastfloor(IsoPlayer.players[0].getZ());
        float worldX = IsoUtils.XToIso(screenX, screenY, z);
        float worldY = IsoUtils.YToIso(screenX, screenY, z);

        for (int i = 0; i < IsoWorld.instance.currentCell.getVehicles().size(); i++) {
            BaseVehicle vehicle = IsoWorld.instance.currentCell.getVehicles().get(i);
            if (vehicle.isInBounds(worldX, worldY)) {
                return vehicle;
            }
        }

        return null;
    }

    public static final class ClickObject {
        public int height;
        public IsoGridSquare square;
        public IsoObject tile;
        public int width;
        public int x;
        public int y;
        public int lx;
        public int ly;
        public float scaleX;
        public float scaleY;
        public boolean flip;
        public int score;

        public int calculateScore() {
            float score = 1.0F;
            IsoPlayer player = IsoPlayer.getInstance();
            IsoGridSquare playerSq = player.getCurrentSquare();
            IsoObjectPicker.tempo.x = this.square.getX() + 0.5F;
            IsoObjectPicker.tempo.y = this.square.getY() + 0.5F;
            IsoObjectPicker.tempo.x = IsoObjectPicker.tempo.x - player.getX();
            IsoObjectPicker.tempo.y = IsoObjectPicker.tempo.y - player.getY();
            IsoObjectPicker.tempo.normalize();
            Vector2 vecB = player.getVectorFromDirection(IsoObjectPicker.tempo2);
            float angle = vecB.dot(IsoObjectPicker.tempo);
            score += Math.abs(angle * 4.0F);
            IsoGridSquare square = this.square;
            IsoObject object = this.tile;
            IsoSprite sprite = object.sprite;
            IsoDoor door = Type.tryCastTo(object, IsoDoor.class);
            IsoThumpable thumpable = Type.tryCastTo(object, IsoThumpable.class);
            if (door == null && (thumpable == null || !thumpable.isDoor())) {
                if (object instanceof IsoWindow) {
                    score += 4.0F;
                    if (player.getZ() > square.getZ()) {
                        score -= 1000.0F;
                    }
                } else {
                    if (playerSq != null && square.getRoom() == playerSq.getRoom()) {
                        score++;
                    } else {
                        score -= 100000.0F;
                    }

                    if (player.getZ() > square.getZ()) {
                        score -= 1000.0F;
                    }

                    if (object instanceof IsoPlayer) {
                        score -= 100000.0F;
                    } else if (object instanceof IsoThumpable
                        && object.getTargetAlpha() < 0.99F
                        && !this.isInteractiveEntity(object)
                        && (object.getTargetAlpha() < 0.5F || object.getContainer() == null)) {
                        score -= 100000.0F;
                    }

                    if (object instanceof IsoCurtain) {
                        score += 3.0F;
                    } else if (object instanceof IsoLightSwitch) {
                        score += 20.0F;
                    } else if (sprite.properties.has(IsoFlagType.bed)) {
                        score += 2.0F;
                    } else if (object.container != null) {
                        score += 10.0F;
                    } else if (object instanceof IsoGenerator) {
                        score += 11.0F;
                    } else if (object instanceof IsoWaveSignal) {
                        score += 20.0F;
                    } else if (thumpable != null && thumpable.getLightSource() != null) {
                        score += 3.0F;
                    } else if (sprite.properties.has(IsoFlagType.waterPiped)) {
                        score += 3.0F;
                    } else if (sprite.properties.has(IsoFlagType.solidfloor)) {
                        score -= 100.0F;
                    } else if (sprite.getType() == IsoObjectType.WestRoofB) {
                        score -= 100.0F;
                    } else if (sprite.getType() == IsoObjectType.WestRoofM) {
                        score -= 100.0F;
                    } else if (sprite.getType() == IsoObjectType.WestRoofT) {
                        score -= 100.0F;
                    } else if (sprite.properties.has(IsoFlagType.cutW) || sprite.properties.has(IsoFlagType.cutN)) {
                        score -= 2.0F;
                    } else if (this.isInteractiveEntity(object)) {
                        score += 2.0F;
                    }
                }
            } else {
                score += 6.0F;
                if (door != null && door.isAdjacentToSquare(playerSq) || thumpable != null && thumpable.isAdjacentToSquare(playerSq)) {
                    score++;
                }

                if (player.getZ() > square.getZ()) {
                    score -= 1000.0F;
                }
            }

            float dist = IsoUtils.DistanceManhatten(square.getX() + 0.5F, square.getY() + 0.5F, player.getX(), player.getY());
            score -= dist / 2.0F;
            return (int)score;
        }

        public int getScore() {
            return this.score;
        }

        public boolean contains(float x, float y) {
            return x >= this.x && y >= this.y && x < this.x + this.width && y < this.y + this.height;
        }

        private boolean isInteractiveEntity(IsoObject object) {
            SpriteConfig spriteConfig = object.getSpriteConfig();
            IsoObject master = spriteConfig == null ? null : spriteConfig.getMultiSquareMaster();
            if (master != null && master != object) {
                return this.isInteractiveEntity(master);
            } else {
                UiConfig uiConfig = object.getComponent(ComponentType.UiConfig);
                if (uiConfig != null && uiConfig.isUiEnabled()) {
                    XuiSkin.EntityUiStyle uiStyle = uiConfig.getEntityUiStyle();
                    return uiStyle != null && uiStyle.getLuaCanOpenWindow() != null ? true : uiStyle != null && uiStyle.getLuaWindowClass() != null;
                } else {
                    return false;
                }
            }
        }
    }
}
