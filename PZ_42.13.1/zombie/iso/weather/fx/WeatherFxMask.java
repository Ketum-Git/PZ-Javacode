// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.weather.fx;

import gnu.trove.map.hash.TLongObjectHashMap;
import java.util.ArrayList;
import org.joml.Vector2i;
import org.joml.Vector3f;
import zombie.GameProfiler;
import zombie.IndieGL;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderSettings;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.debug.LogSeverity;
import zombie.input.GameKeyboard;
import zombie.iso.DiamondMatrixIterator;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameServer;
import zombie.worldMap.Rasterize;

public class WeatherFxMask {
    private static final boolean DEBUG_KEYS = false;
    private static TextureFBO fboMask;
    private static TextureFBO fboParticles;
    public static IsoSprite floorSprite;
    public static IsoSprite wallNSprite;
    public static IsoSprite wallWSprite;
    public static IsoSprite wallNWSprite;
    public static IsoSprite wallSESprite;
    private static Texture texWhite;
    private static boolean renderingMask;
    private static int curPlayerIndex;
    public static final int BIT_FLOOR = 0;
    public static final int BIT_WALLN = 1;
    public static final int BIT_WALLW = 2;
    public static final int BIT_IS_CUT = 4;
    public static final int BIT_CHARS = 8;
    public static final int BIT_OBJECTS = 16;
    public static final int BIT_WALL_SE = 32;
    public static final int BIT_DOOR = 64;
    public static float offsetX = 32 * Core.tileScale;
    public static float offsetY = 96 * Core.tileScale;
    public static ColorInfo defColorInfo = new ColorInfo();
    private static int diamondRows = 1000;
    public int x;
    public int y;
    public int z;
    public int flags;
    public IsoGridSquare gs;
    public boolean enabled;
    private static final WeatherFxMask.PlayerFxMask[] playerMasks = new WeatherFxMask.PlayerFxMask[4];
    private static final DiamondMatrixIterator dmiter = new DiamondMatrixIterator(0);
    private static final Vector2i diamondMatrixPos = new Vector2i();
    private static final WeatherFxMask.RasterizeBounds tempRasterizeBounds = new WeatherFxMask.RasterizeBounds();
    private static final WeatherFxMask.RasterizeBounds[] rasterizeBounds = new WeatherFxMask.RasterizeBounds[4];
    private static final Rasterize rasterize = new Rasterize();
    private static IsoChunkMap rasterizeChunkMap;
    private static int rasterizeZ;
    private static final Vector3f tmpVec = new Vector3f();
    private static final IsoGameCharacter.TorchInfo tmpTorch = new IsoGameCharacter.TorchInfo();
    private static final ColorInfo tmpColInfo = new ColorInfo();
    private static final int[] test = new int[]{0, 1, 768, 769, 774, 775, 770, 771, 772, 773, 32769, 32770, 32771, 32772, 776, 35065, 35066, 34185, 35067};
    private static final String[] testNames = new String[]{
        "GL_ZERO",
        "GL_ONE",
        "GL_SRC_COLOR",
        "GL_ONE_MINUS_SRC_COLOR",
        "GL_DST_COLOR",
        "GL_ONE_MINUS_DST_COLOR",
        "GL_SRC_ALPHA",
        "GL_ONE_MINUS_SRC_ALPHA",
        "GL_DST_ALPHA",
        "GL_ONE_MINUS_DST_ALPHA",
        "GL_CONSTANT_COLOR",
        "GL_ONE_MINUS_CONSTANT_COLOR",
        "GL_CONSTANT_ALPHA",
        "GL_ONE_MINUS_CONSTANT_ALPHA",
        "GL_SRC_ALPHA_SATURATE",
        "GL_SRC1_COLOR (33)",
        "GL_ONE_MINUS_SRC1_COLOR (33)",
        "GL_SRC1_ALPHA (15)",
        "GL_ONE_MINUS_SRC1_ALPHA (33)"
    };
    private static int var1 = 1;
    private static int var2 = 1;
    private static final float var3 = 1.0F;
    private static int scrMaskAdd = 770;
    private static int dstMaskAdd = 771;
    private static int scrMaskSub = 0;
    private static int dstMaskSub = 0;
    private static int scrParticles = 1;
    private static int dstParticles = 771;
    private static int scrMerge = 770;
    private static int dstMerge = 771;
    private static int scrFinal = 770;
    private static int dstFinal = 771;
    private static int idScrMaskAdd;
    private static int idDstMaskAdd;
    private static int idScrMaskSub;
    private static int idDstMaskSub;
    private static int idScrMerge;
    private static int idDstMerge;
    private static int idScrFinal;
    private static int idDstFinal;
    private static int idScrParticles;
    private static int idDstParticles;
    private static int targetBlend;
    private static boolean debugMask;
    public static boolean maskingEnabled = true;
    private static boolean debugMaskAndParticles;
    private static final boolean DEBUG_THROTTLE_KEYS = true;
    private static int keypause;

    public static TextureFBO getFboMask() {
        return fboMask;
    }

    public static TextureFBO getFboParticles() {
        return fboParticles;
    }

    public static boolean isRenderingMask() {
        return renderingMask;
    }

    public static void init() throws Exception {
        if (!GameServer.server || GameServer.guiCommandline) {
            for (int i = 0; i < playerMasks.length; i++) {
                playerMasks[i] = new WeatherFxMask.PlayerFxMask();
            }

            playerMasks[0].init();
            initGlIds();
            floorSprite = IsoSpriteManager.instance.getSprite("floors_interior_tilesandwood_01_16");
            wallNSprite = IsoSpriteManager.instance.getSprite("walls_interior_house_01_21");
            wallWSprite = IsoSpriteManager.instance.getSprite("walls_interior_house_01_20");
            wallNWSprite = IsoSpriteManager.instance.getSprite("walls_interior_house_01_22");
            wallSESprite = IsoSpriteManager.instance.getSprite("walls_interior_house_01_23");
            texWhite = Texture.getSharedTexture("media/textures/weather/fogwhite.png");
        }
    }

    public static boolean checkFbos() {
        if (GameServer.server) {
            return false;
        } else {
            TextureFBO fbo = Core.getInstance().getOffscreenBuffer();
            if (Core.getInstance().getOffscreenBuffer() == null) {
                DebugLog.log("fbo=" + (fbo != null));
                return false;
            } else {
                int width = Core.getInstance().getScreenWidth();
                int height = Core.getInstance().getScreenHeight();
                if (fboMask != null && fboParticles != null && fboMask.getTexture().getWidth() == width && fboMask.getTexture().getHeight() == height) {
                    return fboMask != null && fboParticles != null;
                } else {
                    if (fboMask != null) {
                        fboMask.destroy();
                    }

                    if (fboParticles != null) {
                        fboParticles.destroy();
                    }

                    fboMask = null;
                    fboParticles = null;

                    try {
                        Texture tex = new Texture(width, height, 16);
                        fboMask = new TextureFBO(tex);
                    } catch (Exception var5) {
                        DebugLog.General.printException(var5, "", LogSeverity.Error);
                    }

                    try {
                        Texture tex = new Texture(width, height, 16);
                        fboParticles = new TextureFBO(tex);
                    } catch (Exception var4) {
                        DebugLog.General.printException(var4, "", LogSeverity.Error);
                    }

                    return fboMask != null && fboParticles != null;
                }
            }
        }
    }

    public static void destroy() {
        if (fboMask != null) {
            fboMask.destroy();
        }

        fboMask = null;
        if (fboParticles != null) {
            fboParticles.destroy();
        }

        fboParticles = null;
    }

    public static void initMask() {
        if (!GameServer.server) {
            curPlayerIndex = IsoCamera.frameState.playerIndex;
            playerMasks[curPlayerIndex].initMask();
        }
    }

    private static boolean isOnScreen(int x, int y, int z) {
        float sx = (int)IsoUtils.XToScreenInt(x, y, z, 0);
        float sy = (int)IsoUtils.YToScreenInt(x, y, z, 0);
        sx -= (int)IsoCamera.frameState.offX;
        sy -= (int)IsoCamera.frameState.offY;
        if (sx + 32 * Core.tileScale <= 0.0F) {
            return false;
        } else if (sy + 32 * Core.tileScale <= 0.0F) {
            return false;
        } else {
            return sx - 32 * Core.tileScale >= IsoCamera.frameState.offscreenWidth
                ? false
                : !(sy - 96 * Core.tileScale >= IsoCamera.frameState.offscreenHeight);
        }
    }

    public boolean isLoc(int x, int y, int z) {
        return this.x == x && this.y == y && this.z == z;
    }

    public static boolean playerHasMaskToDraw(int plrIndex) {
        return plrIndex < playerMasks.length ? playerMasks[plrIndex].hasMaskToDraw : false;
    }

    public static void setDiamondIterDone(int plrIndex) {
        if (plrIndex < playerMasks.length) {
            playerMasks[plrIndex].diamondIterDone = true;
        }
    }

    public static void forceMaskUpdate(int plrIndex) {
        if (plrIndex < playerMasks.length) {
            playerMasks[plrIndex].plrSquare = null;
        }
    }

    public static void forceMaskUpdateAll() {
        if (!GameServer.server) {
            for (int i = 0; i < playerMasks.length; i++) {
                playerMasks[i].plrSquare = null;
            }
        }
    }

    private static boolean getIsStairs(IsoGridSquare gs) {
        return gs != null
            && (
                gs.has(IsoObjectType.stairsBN)
                    || gs.has(IsoObjectType.stairsBW)
                    || gs.has(IsoObjectType.stairsMN)
                    || gs.has(IsoObjectType.stairsMW)
                    || gs.has(IsoObjectType.stairsTN)
                    || gs.has(IsoObjectType.stairsTW)
            );
    }

    private static boolean getHasDoor(IsoGridSquare gs) {
        return gs != null
                && (gs.has(IsoFlagType.cutN) || gs.has(IsoFlagType.cutW))
                && (gs.has(IsoFlagType.DoorWallN) || gs.has(IsoFlagType.DoorWallW))
                && !gs.has(IsoFlagType.doorN)
                && !gs.has(IsoFlagType.doorW)
            ? gs.getCanSee(curPlayerIndex)
            : false;
    }

    public static void addMaskLocation(IsoGridSquare gs, int x, int y, int z) {
        if (!GameServer.server) {
            WeatherFxMask.PlayerFxMask playerFxMask = playerMasks[curPlayerIndex];
            if (playerFxMask.requiresUpdate) {
                if (playerFxMask.hasMaskToDraw && playerFxMask.playerZ == z) {
                    IsoChunkMap chunkMap = IsoWorld.instance.getCell().getChunkMap(curPlayerIndex);
                    if (isInPlayerBuilding(gs, x, y, z)) {
                        IsoGridSquare square = chunkMap.getGridSquare(x, y - 1, z);
                        boolean connectN = !isInPlayerBuilding(square, x, y - 1, z);
                        square = chunkMap.getGridSquare(x - 1, y, z);
                        boolean connectW = !isInPlayerBuilding(square, x - 1, y, z);
                        square = chunkMap.getGridSquare(x - 1, y - 1, z);
                        boolean connectNW = !isInPlayerBuilding(square, x - 1, y - 1, z);
                        int WALLS = 0;
                        if (connectN) {
                            WALLS |= 1;
                        }

                        if (connectW) {
                            WALLS |= 2;
                        }

                        if (connectNW) {
                            WALLS |= 32;
                        }

                        boolean added = false;
                        boolean isStairs = getIsStairs(gs);
                        if (gs != null && (connectN || connectW || connectNW)) {
                            int CHARS_AND_OBJECTS = 24;
                            if (connectN && !gs.getProperties().has(IsoFlagType.WallN) && !gs.has(IsoFlagType.WallNW)) {
                                playerFxMask.addMask(x - 1, y, z, null, 8, false);
                                playerFxMask.addMask(x, y, z, gs, 24);
                                playerFxMask.addMask(x + 1, y, z, null, 24, false);
                                playerFxMask.addMask(x + 2, y, z, null, 8, false);
                                playerFxMask.addMask(x, y + 1, z, null, 8, false);
                                playerFxMask.addMask(x + 1, y + 1, z, null, 24, false);
                                playerFxMask.addMask(x + 2, y + 1, z, null, 24, false);
                                playerFxMask.addMask(x + 2, y + 2, z, null, 16, false);
                                playerFxMask.addMask(x + 3, y + 2, z, null, 16, false);
                                added = true;
                            }

                            if (connectW && !gs.getProperties().has(IsoFlagType.WallW) && !gs.getProperties().has(IsoFlagType.WallNW)) {
                                playerFxMask.addMask(x, y - 1, z, null, 8, false);
                                playerFxMask.addMask(x, y, z, gs, 24);
                                playerFxMask.addMask(x, y + 1, z, null, 24, false);
                                playerFxMask.addMask(x, y + 2, z, null, 8, false);
                                playerFxMask.addMask(x + 1, y, z, null, 8, false);
                                playerFxMask.addMask(x + 1, y + 1, z, null, 24, false);
                                playerFxMask.addMask(x + 1, y + 2, z, null, 24, false);
                                playerFxMask.addMask(x + 2, y + 2, z, null, 16, false);
                                playerFxMask.addMask(x + 2, y + 3, z, null, 16, false);
                                added = true;
                            }

                            if (connectNW) {
                                int flags = isStairs ? 24 : WALLS;
                                playerFxMask.addMask(x, y, z, gs, flags);
                                added = true;
                            }
                        }

                        if (!added) {
                            int flags = isStairs ? 24 : WALLS;
                            playerFxMask.addMask(x, y, z, gs, flags);
                        }
                    } else {
                        IsoGridSquare squarex = chunkMap.getGridSquare(x, y - 1, z);
                        boolean connectNx = isInPlayerBuilding(squarex, x, y - 1, z);
                        squarex = chunkMap.getGridSquare(x - 1, y, z);
                        boolean connectWx = isInPlayerBuilding(squarex, x - 1, y, z);
                        if (!connectNx && !connectWx) {
                            squarex = chunkMap.getGridSquare(x - 1, y - 1, z);
                            if (isInPlayerBuilding(squarex, x - 1, y - 1, z)) {
                                playerFxMask.addMask(x, y, z, gs, 4);
                            }
                        } else {
                            int flags = 4;
                            if (connectNx) {
                                flags |= 1;
                            }

                            if (connectWx) {
                                flags |= 2;
                            }

                            if (getHasDoor(gs)) {
                                flags |= 64;
                            }

                            playerFxMask.addMask(x, y, z, gs, flags);
                        }
                    }
                }
            }
        }
    }

    private static boolean isInPlayerBuilding(IsoGridSquare gs, int x, int y, int z) {
        WeatherFxMask.PlayerFxMask playerFxMask = playerMasks[curPlayerIndex];
        if (gs != null && gs.has(IsoFlagType.solidfloor)) {
            if (gs.getBuilding() != null && gs.getBuilding() == playerFxMask.player.getBuilding()) {
                return true;
            }

            if (gs.getBuilding() == null) {
                return playerFxMask.curIsoWorldRegion != null
                    && gs.getIsoWorldRegion() != null
                    && gs.getIsoWorldRegion().isFogMask()
                    && (gs.getIsoWorldRegion() == playerFxMask.curIsoWorldRegion || playerFxMask.curConnectedRegions.contains(gs.getIsoWorldRegion()));
            }
        } else {
            if (isInteriorLocation(x, y, z)) {
                return true;
            }

            if (gs != null && gs.getBuilding() == null) {
                return playerFxMask.curIsoWorldRegion != null
                    && gs.getIsoWorldRegion() != null
                    && gs.getIsoWorldRegion().isFogMask()
                    && (gs.getIsoWorldRegion() == playerFxMask.curIsoWorldRegion || playerFxMask.curConnectedRegions.contains(gs.getIsoWorldRegion()));
            }

            if (gs == null && playerFxMask.curIsoWorldRegion != null) {
                IWorldRegion mr = IsoRegions.getIsoWorldRegion(x, y, z);
                return mr != null && mr.isFogMask() && (mr == playerFxMask.curIsoWorldRegion || playerFxMask.curConnectedRegions.contains(mr));
            }
        }

        return false;
    }

    private static boolean isInteriorLocation(int x, int y, int maxZ) {
        WeatherFxMask.PlayerFxMask playerFxMask = playerMasks[curPlayerIndex];

        for (int z = maxZ; z >= 0; z--) {
            IsoGridSquare square = IsoWorld.instance.getCell().getChunkMap(curPlayerIndex).getGridSquare(x, y, z);
            if (square != null) {
                if (square.getBuilding() != null && square.getBuilding() == playerFxMask.player.getBuilding()) {
                    return true;
                }

                if (square.has(IsoFlagType.exterior)) {
                    return false;
                }
            }
        }

        return false;
    }

    private static void scanForTilesOld(int nPlayer) {
        WeatherFxMask.PlayerFxMask playerFxMask = playerMasks[curPlayerIndex];
        if (!playerFxMask.diamondIterDone) {
            IsoPlayer player = IsoPlayer.players[nPlayer];
            int maxZ = PZMath.fastfloor(player.getZ());
            int x1 = 0;
            int y1 = 0;
            int x2 = 0 + IsoCamera.getOffscreenWidth(nPlayer);
            int y2 = 0 + IsoCamera.getOffscreenHeight(nPlayer);
            float topLeftX = IsoUtils.XToIso(0.0F, 0.0F, 0.0F);
            float topRightY = IsoUtils.YToIso(x2, 0.0F, 0.0F);
            float bottomRightX = IsoUtils.XToIso(x2, y2, 6.0F);
            float bottomLeftY = IsoUtils.YToIso(0.0F, y2, 6.0F);
            float topRightX = IsoUtils.XToIso(x2, 0.0F, 0.0F);
            int minY = (int)topRightY;
            int maxY = (int)bottomLeftY;
            int minX = (int)topLeftX;
            int maxX = (int)bottomRightX;
            diamondRows = (int)topRightX * 4;
            minX -= 2;
            minY -= 2;
            dmiter.reset(maxX - minX);
            Vector2i v = diamondMatrixPos;
            IsoChunkMap chunkMap = IsoWorld.instance.getCell().getChunkMap(nPlayer);

            while (dmiter.next(v)) {
                if (v != null) {
                    IsoGridSquare square = chunkMap.getGridSquare(v.x + minX, v.y + minY, maxZ);
                    if (square == null) {
                        addMaskLocation(null, v.x + minX, v.y + minY, maxZ);
                    } else {
                        IsoChunk c = square.getChunk();
                        if (c != null && square.IsOnScreen()) {
                            addMaskLocation(square, v.x + minX, v.y + minY, maxZ);
                        }
                    }
                }
            }
        }
    }

    public static boolean checkVisibleSquares(int playerIndex, int z) {
        if (!playerMasks[playerIndex].hasMaskToDraw) {
            return false;
        } else if (rasterizeBounds[playerIndex] == null) {
            return true;
        } else {
            tempRasterizeBounds.calculate(playerIndex, z);
            return !tempRasterizeBounds.equals(rasterizeBounds[playerIndex]);
        }
    }

    private static void scanForTiles(int nPlayer) {
        WeatherFxMask.PlayerFxMask playerFxMask = playerMasks[nPlayer];
        if (!playerFxMask.diamondIterDone) {
            IsoPlayer player = IsoPlayer.players[nPlayer];
            int playerZ = PZMath.fastfloor(player.getZ());
            if (rasterizeBounds[nPlayer] == null) {
                rasterizeBounds[nPlayer] = new WeatherFxMask.RasterizeBounds();
            }

            WeatherFxMask.RasterizeBounds rb = rasterizeBounds[nPlayer];
            GameProfiler profiler = GameProfiler.getInstance();

            try (GameProfiler.ProfileArea ignored = profiler.profile("Calc Bounds")) {
                rb.calculate(nPlayer, playerZ);
            }

            if (Core.debug) {
            }

            boolean bRender = false;

            try (GameProfiler.ProfileArea ignored = profiler.profile("scanTriangle")) {
                rasterizeChunkMap = IsoWorld.instance.getCell().getChunkMap(nPlayer);
                rasterizeZ = playerZ;
                rasterize.scanTriangle(rb.x1, rb.y1, rb.x2, rb.y2, rb.x4, rb.y4, 0, 100000, (vx, vy) -> {
                    IsoGridSquare square = rasterizeChunkMap.getGridSquare(vx, vy, rasterizeZ);
                    addMaskLocation(square, vx, vy, rasterizeZ);
                    if (bRender) {
                        LineDrawer.addRect(vx + 0.05F, vy + 0.05F, rasterizeZ, 0.9F, 0.9F, 1.0F, 0.0F, 0.0F);
                    }
                });
                rasterize.scanTriangle(rb.x2, rb.y2, rb.x3, rb.y3, rb.x4, rb.y4, 0, 100000, (vx, vy) -> {
                    IsoGridSquare square = rasterizeChunkMap.getGridSquare(vx, vy, rasterizeZ);
                    addMaskLocation(square, vx, vy, rasterizeZ);
                    if (bRender) {
                        LineDrawer.addRect(vx, vy, rasterizeZ, 1.0F, 1.0F, 0.0F, 1.0F, 0.0F);
                    }
                });
            }

            if (bRender) {
                LineDrawer.addLine(rb.x1, rb.y1, rasterizeZ, rb.x2, rb.y2, rasterizeZ, 1.0F, 1.0F, 1.0F, 0.5F);
                LineDrawer.addLine(rb.x2, rb.y2, rasterizeZ, rb.x3, rb.y3, rasterizeZ, 1.0F, 1.0F, 1.0F, 0.5F);
                LineDrawer.addLine(rb.x3, rb.y3, rasterizeZ, rb.x4, rb.y4, rasterizeZ, 1.0F, 1.0F, 1.0F, 0.5F);
                LineDrawer.addLine(rb.x1, rb.y1, rasterizeZ, rb.x4, rb.y4, rasterizeZ, 1.0F, 1.0F, 1.0F, 0.5F);
                float ox = IsoCamera.getOffX();
                float oy = IsoCamera.getOffY();
                LineDrawer.drawLine(rb.cx1 - ox, rb.cy1 - oy, rb.cx2 - ox, rb.cy2 - oy, 1.0F, 1.0F, 1.0F, 0.5F, 2);
                LineDrawer.drawLine(rb.cx2 - ox, rb.cy2 - oy, rb.cx3 - ox, rb.cy3 - oy, 1.0F, 1.0F, 1.0F, 0.5F, 2);
                LineDrawer.drawLine(rb.cx3 - ox, rb.cy3 - oy, rb.cx4 - ox, rb.cy4 - oy, 1.0F, 1.0F, 1.0F, 0.5F, 2);
                LineDrawer.drawLine(rb.cx4 - ox, rb.cy4 - oy, rb.cx1 - ox, rb.cy1 - oy, 1.0F, 1.0F, 1.0F, 0.5F, 2);
            }
        }
    }

    private static void renderMaskFloor(int x, int y, int z) {
        floorSprite.render(null, x, y, z, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
    }

    private static void renderMaskWall(IsoGridSquare square, int x, int y, int z, boolean N, boolean W, int playerIndex) {
        if (square != null) {
            IsoGridSquare squareN = square.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare squareS = square.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare squareW = square.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare squareE = square.getAdjacentSquare(IsoDirections.E);
            long currentTimeMillis = System.currentTimeMillis();
            int cutawaySelf = square.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
            int cutawayN = squareN == null ? 0 : squareN.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
            int cutawayS = squareS == null ? 0 : squareS.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
            int cutawayW = squareW == null ? 0 : squareW.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
            int cutawayE = squareE == null ? 0 : squareE.getPlayerCutawayFlag(playerIndex, currentTimeMillis);
            IsoSprite sprite;
            IsoDirections dir;
            if (N && W) {
                sprite = wallNWSprite;
                dir = IsoDirections.NW;
            } else if (N) {
                sprite = wallNSprite;
                dir = IsoDirections.N;
            } else if (W) {
                sprite = wallWSprite;
                dir = IsoDirections.W;
            } else {
                sprite = wallSESprite;
                dir = IsoDirections.SE;
            }

            square.DoCutawayShaderSprite(sprite, dir, cutawaySelf, cutawayN, cutawayS, cutawayW, cutawayE);
        }
    }

    private static void renderMaskWallNoCuts(int x, int y, int z, boolean N, boolean W) {
        if (N && W) {
            wallNWSprite.render(null, x, y, z, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
        } else if (N) {
            wallNSprite.render(null, x, y, z, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
        } else if (W) {
            wallWSprite.render(null, x, y, z, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
        } else {
            wallSESprite.render(null, x, y, z, IsoDirections.N, offsetX, offsetY, defColorInfo, false);
        }
    }

    public static void renderFxMask(int nPlayer) {
        if (!(IsoCamera.frameState.camCharacterZ < 0.0F)) {
            if (DebugOptions.instance.weather.fx.getValue()) {
                if (!GameServer.server) {
                    if (IsoWeatherFX.instance != null) {
                        if (LuaManager.thread == null || !LuaManager.thread.step) {
                            if (playerMasks[nPlayer].maskEnabled) {
                                WeatherFxMask.PlayerFxMask playerFxMask = playerMasks[curPlayerIndex];
                                if (playerFxMask.maskEnabled) {
                                    if (maskingEnabled && !checkFbos()) {
                                        maskingEnabled = false;
                                    }

                                    if (maskingEnabled && playerFxMask.hasMaskToDraw) {
                                        GameProfiler profiler = GameProfiler.getInstance();

                                        try (GameProfiler.ProfileArea ignored = profiler.profile("scanForTiles")) {
                                            scanForTiles(nPlayer);
                                        }

                                        SpriteRenderer.instance.glIgnoreStyles(true);
                                        if (maskingEnabled) {
                                            try (GameProfiler.ProfileArea ignored = profiler.profile("drawFxMask")) {
                                                drawFxMask(nPlayer);
                                            }
                                        }

                                        if (debugMaskAndParticles) {
                                            SpriteRenderer.instance.glClearColor(0, 0, 0, 255);
                                            SpriteRenderer.instance.glClear(16640);
                                            SpriteRenderer.instance.glClearColor(0, 0, 0, 255);
                                        } else if (debugMask) {
                                            SpriteRenderer.instance.glClearColor(0, 255, 0, 255);
                                            SpriteRenderer.instance.glClear(16640);
                                            SpriteRenderer.instance.glClearColor(0, 0, 0, 255);
                                        }

                                        try (GameProfiler.ProfileArea ignored = profiler.profile("drawFxLayered")) {
                                            if (!RenderSettings.getInstance().getPlayerSettings(nPlayer).isExterior()) {
                                                drawFxLayered(nPlayer, false, false, false);
                                            }

                                            if (IsoWeatherFX.instance.hasCloudsToRender()) {
                                                drawFxLayered(nPlayer, true, false, false);
                                            }

                                            if (IsoWeatherFX.instance.hasFogToRender() && PerformanceSettings.fogQuality == 2) {
                                                drawFxLayered(nPlayer, false, true, false);
                                            }

                                            if (Core.getInstance().getOptionRenderPrecipitation() == 1 && IsoWeatherFX.instance.hasPrecipitationToRender()) {
                                                drawFxLayered(nPlayer, false, false, true);
                                            }
                                        }

                                        SpriteRenderer.glBlendfuncEnabled = true;
                                        SpriteRenderer.instance.glIgnoreStyles(false);
                                    } else {
                                        if (IsoWorld.instance.getCell() != null && IsoWorld.instance.getCell().getWeatherFX() != null) {
                                            SpriteRenderer.instance.glIgnoreStyles(true);
                                            IndieGL.glBlendFunc(770, 771);
                                            IsoWorld.instance.getCell().getWeatherFX().render();
                                            SpriteRenderer.instance.glIgnoreStyles(false);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void drawFxMask(int nPlayer) {
        int ow = IsoCamera.getOffscreenWidth(nPlayer);
        int oh = IsoCamera.getOffscreenHeight(nPlayer);
        renderingMask = true;
        SpriteRenderer.instance.glBuffer(4, nPlayer);
        SpriteRenderer.instance.glDoStartFrameFx(ow, oh, nPlayer);
        IsoWorld.instance.getCell().DrawStencilMask();
        IndieGL.glDepthMask(true);
        IndieGL.enableDepthTest();
        SpriteRenderer.instance.glClearColor(0, 0, 0, 0);
        SpriteRenderer.instance.glClear(16640);
        SpriteRenderer.instance.glClearColor(0, 0, 0, 255);
        IndieGL.glDepthMask(false);
        IndieGL.disableDepthTest();
        SpriteRenderer.instance.StartShader(0, nPlayer);
        boolean isMaskedDraw = true;
        boolean doObjects = false;
        WeatherFxMask[] masks = playerMasks[nPlayer].masks;
        int maskPointer = playerMasks[nPlayer].maskPointer;

        for (int i = 0; i < maskPointer; i++) {
            WeatherFxMask mask = masks[i];
            if (mask.enabled) {
                if ((mask.flags & 4) == 4) {
                    SpriteRenderer.glBlendfuncEnabled = true;
                    IndieGL.glBlendFunc(scrMaskSub, dstMaskSub);
                    SpriteRenderer.instance.glBlendEquation(32779);
                    IndieGL.enableAlphaTest();
                    IndieGL.glAlphaFunc(516, 0.02F);
                    SpriteRenderer.glBlendfuncEnabled = false;
                    boolean wN = (mask.flags & 1) == 1;
                    boolean wW = (mask.flags & 2) == 2;
                    renderMaskWall(mask.gs, mask.x, mask.y, mask.z, wN, wW, nPlayer);
                    SpriteRenderer.glBlendfuncEnabled = true;
                    SpriteRenderer.instance.glBlendEquation(32774);
                    SpriteRenderer.glBlendfuncEnabled = false;
                    boolean door = (mask.flags & 64) == 64;
                    if (door && mask.gs != null) {
                        SpriteRenderer.glBlendfuncEnabled = true;
                        IndieGL.glBlendFunc(scrMaskAdd, dstMaskAdd);
                        SpriteRenderer.glBlendfuncEnabled = false;
                        mask.gs.RenderOpenDoorOnly();
                    }
                } else {
                    SpriteRenderer.glBlendfuncEnabled = true;
                    IndieGL.glBlendFunc(scrMaskAdd, dstMaskAdd);
                    SpriteRenderer.glBlendfuncEnabled = false;
                    renderMaskFloor(mask.x, mask.y, mask.z);
                    doObjects = (mask.flags & 16) == 16;
                    boolean doChars = (mask.flags & 8) == 8;
                    if (!doObjects) {
                        boolean wN = (mask.flags & 1) == 1;
                        boolean wW = (mask.flags & 2) == 2;
                        if (!wN && !wW) {
                            if ((mask.flags & 32) == 32) {
                                renderMaskWall(mask.gs, mask.x, mask.y, mask.z, false, false, nPlayer);
                            }
                        } else {
                            renderMaskWall(mask.gs, mask.x, mask.y, mask.z, wN, wW, nPlayer);
                        }
                    }

                    if (doObjects && mask.gs != null) {
                        mask.gs.RenderMinusFloorFxMask(mask.z + 1, false, false);
                    }

                    if (doChars && mask.gs != null) {
                        mask.gs.renderCharacters(mask.z + 1, false, false);
                        SpriteRenderer.glBlendfuncEnabled = true;
                        IndieGL.glBlendFunc(scrMaskAdd, dstMaskAdd);
                        SpriteRenderer.glBlendfuncEnabled = false;
                    }
                }
            }
        }

        IndieGL.glBlendFunc(770, 771);
        SpriteRenderer.instance.glBuffer(5, nPlayer);
        SpriteRenderer.instance.glDoEndFrameFx(nPlayer);
        renderingMask = false;
    }

    private static void drawFxLayered(int nPlayer, boolean doClouds, boolean doFog, boolean doPrecip) {
        int ox = IsoCamera.getOffscreenLeft(nPlayer);
        int oy = IsoCamera.getOffscreenTop(nPlayer);
        int ow = IsoCamera.getOffscreenWidth(nPlayer);
        int oh = IsoCamera.getOffscreenHeight(nPlayer);
        int sx = IsoCamera.getScreenLeft(nPlayer);
        int sy = IsoCamera.getScreenTop(nPlayer);
        int sw = IsoCamera.getScreenWidth(nPlayer);
        int sh = IsoCamera.getScreenHeight(nPlayer);
        IndieGL.glDepthMask(false);
        IndieGL.disableDepthTest();
        SpriteRenderer.instance.glBuffer(6, nPlayer);
        SpriteRenderer.instance.glDoStartFrameFx(ow, oh, nPlayer);
        if (!doClouds && !doFog && !doPrecip) {
            Color c = RenderSettings.getInstance().getMaskClearColorForPlayer(nPlayer);
            SpriteRenderer.glBlendfuncEnabled = true;
            IndieGL.glBlendFuncSeparate(scrParticles, dstParticles, 1, 771);
            SpriteRenderer.glBlendfuncEnabled = false;
            SpriteRenderer.instance.renderi(texWhite, 0, 0, ow, oh, c.r, c.g, c.b, c.a, null);
            SpriteRenderer.glBlendfuncEnabled = true;
        } else if (IsoWorld.instance.getCell() != null && IsoWorld.instance.getCell().getWeatherFX() != null) {
            SpriteRenderer.glBlendfuncEnabled = true;
            IndieGL.glBlendFuncSeparate(scrParticles, dstParticles, 1, 771);
            SpriteRenderer.glBlendfuncEnabled = false;
            IsoWorld.instance.getCell().getWeatherFX().renderLayered(doClouds, doFog, doPrecip);
            SpriteRenderer.glBlendfuncEnabled = true;
        }

        if (maskingEnabled) {
            IndieGL.glBlendFunc(scrMerge, dstMerge);
            SpriteRenderer.instance.glBlendEquation(32779);
            ((Texture)fboMask.getTexture()).rendershader2(0.0F, 0.0F, ow, oh, sx, sy, sw, sh, 1.0F, 1.0F, 1.0F, 1.0F);
            SpriteRenderer.instance.glBlendEquation(32774);
        }

        IndieGL.glBlendFunc(770, 771);
        SpriteRenderer.instance.glBuffer(7, nPlayer);
        SpriteRenderer.instance.glDoEndFrameFx(nPlayer);
        Texture tex;
        if ((debugMask || debugMaskAndParticles) && !debugMaskAndParticles) {
            tex = (Texture)fboMask.getTexture();
            IndieGL.glBlendFunc(770, 771);
        } else {
            tex = (Texture)fboParticles.getTexture();
            IndieGL.glBlendFunc(scrFinal, dstFinal);
        }

        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;
        float a = 1.0F;
        float sx1 = (float)sx / tex.getWidthHW();
        float ey1 = (float)sy / tex.getHeightHW();
        float ex1 = (float)(sx + sw) / tex.getWidthHW();
        float sy1 = (float)(sy + sh) / tex.getHeightHW();
        SpriteRenderer.instance.render(tex, 0.0F, 0.0F, ow, oh, 1.0F, 1.0F, 1.0F, 1.0F, sx1, sy1, ex1, sy1, ex1, ey1, sx1, ey1);
        IndieGL.glDefaultBlendFunc();
    }

    private static void initGlIds() {
        for (int i = 0; i < test.length; i++) {
            if (test[i] == scrMaskAdd) {
                idScrMaskAdd = i;
            } else if (test[i] == dstMaskAdd) {
                idDstMaskAdd = i;
            } else if (test[i] == scrMaskSub) {
                idScrMaskSub = i;
            } else if (test[i] == dstMaskSub) {
                idDstMaskSub = i;
            } else if (test[i] == scrParticles) {
                idScrParticles = i;
            } else if (test[i] == dstParticles) {
                idDstParticles = i;
            } else if (test[i] == scrMerge) {
                idScrMerge = i;
            } else if (test[i] == dstMerge) {
                idDstMerge = i;
            } else if (test[i] == scrFinal) {
                idScrFinal = i;
            } else if (test[i] == dstFinal) {
                idDstFinal = i;
            }
        }
    }

    private static void updateDebugKeys() {
        if (keypause > 0) {
            keypause--;
        }

        if (keypause == 0) {
            boolean modechanged = false;
            boolean targetchanged = false;
            boolean debugchanged = false;
            boolean finalchanged = false;
            boolean domaskingchanged = false;
            if (targetBlend == 0) {
                var1 = idScrMaskAdd;
                var2 = idDstMaskAdd;
            } else if (targetBlend == 1) {
                var1 = idScrMaskSub;
                var2 = idDstMaskSub;
            } else if (targetBlend == 2) {
                var1 = idScrMerge;
                var2 = idDstMerge;
            } else if (targetBlend == 3) {
                var1 = idScrFinal;
                var2 = idDstFinal;
            } else if (targetBlend == 4) {
                var1 = idScrParticles;
                var2 = idDstParticles;
            }

            if (GameKeyboard.isKeyDown(79)) {
                var1--;
                if (var1 < 0) {
                    var1 = test.length - 1;
                }

                modechanged = true;
            } else if (GameKeyboard.isKeyDown(81)) {
                var1++;
                if (var1 >= test.length) {
                    var1 = 0;
                }

                modechanged = true;
            } else if (GameKeyboard.isKeyDown(75)) {
                var2--;
                if (var2 < 0) {
                    var2 = test.length - 1;
                }

                modechanged = true;
            } else if (GameKeyboard.isKeyDown(77)) {
                var2++;
                if (var2 >= test.length) {
                    var2 = 0;
                }

                modechanged = true;
            } else if (GameKeyboard.isKeyDown(71)) {
                targetBlend--;
                if (targetBlend < 0) {
                    targetBlend = 4;
                }

                modechanged = true;
                targetchanged = true;
            } else if (GameKeyboard.isKeyDown(73)) {
                targetBlend++;
                if (targetBlend >= 5) {
                    targetBlend = 0;
                }

                modechanged = true;
                targetchanged = true;
            } else if (maskingEnabled && GameKeyboard.isKeyDown(82)) {
                debugMask = !debugMask;
                modechanged = true;
                debugchanged = true;
            } else if (maskingEnabled && GameKeyboard.isKeyDown(80)) {
                debugMaskAndParticles = !debugMaskAndParticles;
                modechanged = true;
                finalchanged = true;
            } else if (!GameKeyboard.isKeyDown(72) && GameKeyboard.isKeyDown(76)) {
                maskingEnabled = !maskingEnabled;
                modechanged = true;
                domaskingchanged = true;
            }

            if (modechanged) {
                if (targetchanged) {
                    if (targetBlend == 0) {
                        DebugLog.log("TargetBlend = MASK_ADD");
                    } else if (targetBlend == 1) {
                        DebugLog.log("TargetBlend = MASK_SUB");
                    } else if (targetBlend == 2) {
                        DebugLog.log("TargetBlend = MERGE");
                    } else if (targetBlend == 3) {
                        DebugLog.log("TargetBlend = FINAL");
                    } else if (targetBlend == 4) {
                        DebugLog.log("TargetBlend = PARTICLES");
                    }
                } else if (debugchanged) {
                    DebugLog.log("DEBUG_MASK = " + debugMask);
                } else if (finalchanged) {
                    DebugLog.log("DEBUG_MASK_AND_PARTICLES = " + debugMaskAndParticles);
                } else if (domaskingchanged) {
                    DebugLog.log("MASKING_ENABLED = " + maskingEnabled);
                } else {
                    if (targetBlend == 0) {
                        idScrMaskAdd = var1;
                        idDstMaskAdd = var2;
                        scrMaskAdd = test[idScrMaskAdd];
                        dstMaskAdd = test[idDstMaskAdd];
                    } else if (targetBlend == 1) {
                        idScrMaskSub = var1;
                        idDstMaskSub = var2;
                        scrMaskSub = test[idScrMaskSub];
                        dstMaskSub = test[idDstMaskSub];
                    } else if (targetBlend == 2) {
                        idScrMerge = var1;
                        idDstMerge = var2;
                        scrMerge = test[idScrMerge];
                        dstMerge = test[idDstMerge];
                    } else if (targetBlend == 3) {
                        idScrFinal = var1;
                        idDstFinal = var2;
                        scrFinal = test[idScrFinal];
                        dstFinal = test[idDstFinal];
                    } else if (targetBlend == 4) {
                        idScrParticles = var1;
                        idDstParticles = var2;
                        scrParticles = test[idScrParticles];
                        dstParticles = test[idDstParticles];
                    }

                    DebugLog.log("Blendmode = " + testNames[var1] + " -> " + testNames[var2]);
                }

                keypause = 30;
            }
        }
    }

    public static class PlayerFxMask {
        private WeatherFxMask[] masks;
        private int maskPointer;
        private boolean maskEnabled;
        private IsoGridSquare plrSquare;
        private int disabledMasks;
        private boolean requiresUpdate;
        private boolean hasMaskToDraw = true;
        private int playerIndex;
        private IsoPlayer player;
        private int playerZ;
        private IWorldRegion curIsoWorldRegion;
        private final ArrayList<IWorldRegion> curConnectedRegions = new ArrayList<>();
        private final ArrayList<IWorldRegion> isoWorldRegionTemp = new ArrayList<>();
        private final TLongObjectHashMap<WeatherFxMask> maskHashMap = new TLongObjectHashMap<>();
        private boolean diamondIterDone;
        private boolean isFirstSquare = true;
        private IsoGridSquare firstSquare;

        private void init() {
            this.masks = new WeatherFxMask[30000];

            for (int i = 0; i < this.masks.length; i++) {
                if (this.masks[i] == null) {
                    this.masks[i] = new WeatherFxMask();
                }
            }

            this.maskEnabled = true;
        }

        private void initMask() {
            if (!GameServer.server) {
                if (!this.maskEnabled) {
                    this.init();
                }

                this.playerIndex = IsoCamera.frameState.playerIndex;
                this.player = IsoPlayer.players[this.playerIndex];
                this.playerZ = PZMath.fastfloor(this.player.getZ());
                this.diamondIterDone = false;
                this.requiresUpdate = false;
                if (this.player != null) {
                    if (this.isFirstSquare || this.plrSquare == null || this.plrSquare != this.player.getSquare()) {
                        this.plrSquare = this.player.getSquare();
                        this.maskPointer = 0;
                        this.maskHashMap.clear();
                        this.disabledMasks = 0;
                        this.requiresUpdate = true;
                        if (this.firstSquare == null) {
                            this.firstSquare = this.plrSquare;
                        }

                        if (this.firstSquare != null && this.firstSquare != this.plrSquare) {
                            this.isFirstSquare = false;
                        }
                    }

                    this.curIsoWorldRegion = this.player.getMasterRegion();
                    this.curConnectedRegions.clear();
                    if (this.curIsoWorldRegion != null && this.player.getMasterRegion().isFogMask()) {
                        this.isoWorldRegionTemp.clear();
                        this.isoWorldRegionTemp.add(this.curIsoWorldRegion);

                        while (!this.isoWorldRegionTemp.isEmpty()) {
                            IWorldRegion current = this.isoWorldRegionTemp.remove(0);
                            this.curConnectedRegions.add(current);
                            if (!current.getNeighbors().isEmpty()) {
                                for (IsoWorldRegion neighbor : current.getNeighbors()) {
                                    if (!this.isoWorldRegionTemp.contains(neighbor) && !this.curConnectedRegions.contains(neighbor) && neighbor.isFogMask()) {
                                        this.isoWorldRegionTemp.add(neighbor);
                                    }
                                }
                            }
                        }
                    } else {
                        this.curIsoWorldRegion = null;
                    }
                }

                if (IsoWeatherFX.instance == null) {
                    this.hasMaskToDraw = false;
                } else {
                    this.hasMaskToDraw = true;
                    if (this.hasMaskToDraw) {
                        if ((
                                this.player.getSquare() == null
                                    || this.player.getSquare().getBuilding() == null && this.player.getSquare().has(IsoFlagType.exterior)
                            )
                            && (this.curIsoWorldRegion == null || !this.curIsoWorldRegion.isFogMask())) {
                            this.hasMaskToDraw = false;
                        } else {
                            this.hasMaskToDraw = true;
                        }
                    }
                }
            }
        }

        private void addMask(int x, int y, int z, IsoGridSquare gs, int flags) {
            this.addMask(x, y, z, gs, flags, true);
        }

        private void addMask(int x, int y, int z, IsoGridSquare gs, int flags, boolean enabled) {
            if (this.hasMaskToDraw && this.requiresUpdate) {
                if (!this.maskEnabled) {
                    this.init();
                }

                WeatherFxMask locA = this.getMask(x, y, z);
                if (locA == null) {
                    WeatherFxMask mask = this.getFreeMask();
                    mask.x = x;
                    mask.y = y;
                    mask.z = z;
                    mask.flags = flags;
                    mask.gs = gs;
                    mask.enabled = enabled;
                    if (!enabled && this.disabledMasks < WeatherFxMask.diamondRows) {
                        this.disabledMasks++;
                    }

                    this.maskHashMap.put((long)y << 32 | x, mask);
                } else {
                    if (locA.flags != flags) {
                        locA.flags |= flags;
                    }

                    if (!locA.enabled && enabled) {
                        WeatherFxMask mask = this.getFreeMask();
                        mask.x = x;
                        mask.y = y;
                        mask.z = z;
                        mask.flags = locA.flags;
                        mask.gs = gs;
                        mask.enabled = enabled;
                        this.maskHashMap.put((long)y << 32 | x, mask);
                    } else {
                        locA.enabled = locA.enabled ? locA.enabled : enabled;
                        if (enabled && gs != null && locA.gs == null) {
                            locA.gs = gs;
                        }
                    }
                }
            }
        }

        private WeatherFxMask getFreeMask() {
            if (this.maskPointer >= this.masks.length) {
                DebugLog.log("Weather Mask buffer out of bounds. Increasing cache.");
                WeatherFxMask[] old = this.masks;
                this.masks = new WeatherFxMask[this.masks.length + 10000];

                for (int i = 0; i < this.masks.length; i++) {
                    if (i < old.length && old[i] != null) {
                        this.masks[i] = old[i];
                    } else {
                        this.masks[i] = new WeatherFxMask();
                    }
                }
            }

            return this.masks[this.maskPointer++];
        }

        private boolean masksContains(int x, int y, int z) {
            return this.getMask(x, y, z) != null;
        }

        private WeatherFxMask getMask(int x, int y, int z) {
            return this.maskHashMap.get((long)y << 32 | x);
        }
    }

    public static final class RasterizeBounds {
        float x1;
        float y1;
        float x2;
        float y2;
        float x3;
        float y3;
        float x4;
        float y4;
        int cx1;
        int cy1;
        int cx2;
        int cy2;
        int cx3;
        int cy3;
        int cx4;
        int cy4;

        public void calculate(int playerIndex, int z) {
            int x1 = 0;
            int y1 = 0;
            int x2 = 0 + IsoCamera.getOffscreenWidth(playerIndex);
            int y2 = 0 + IsoCamera.getOffscreenHeight(playerIndex);
            this.x1 = IsoUtils.XToIso(0.0F, 0.0F, z);
            this.y1 = IsoUtils.YToIso(0.0F, 0.0F, z);
            this.x2 = IsoUtils.XToIso(x2, 0.0F, z);
            this.y2 = IsoUtils.YToIso(x2, 0.0F, z);
            this.x3 = IsoUtils.XToIso(x2, y2, z);
            this.y3 = IsoUtils.YToIso(x2, y2, z);
            this.x4 = IsoUtils.XToIso(0.0F, y2, z);
            this.y4 = IsoUtils.YToIso(0.0F, y2, z);
            this.cx1 = (int)IsoUtils.XToScreen(PZMath.fastfloor(this.x1) - 0.5F, PZMath.fastfloor(this.y1) + 0.5F, -666.0F, -666);
            this.cy1 = (int)IsoUtils.YToScreen(PZMath.fastfloor(this.x1) - 0.5F, PZMath.fastfloor(this.y1) + 0.5F, -666.0F, -666);
            this.cx2 = (int)IsoUtils.XToScreen(PZMath.fastfloor(this.x2) + 0.5F, PZMath.fastfloor(this.y2) - 0.5F, -666.0F, -666);
            this.cy2 = (int)IsoUtils.YToScreen(PZMath.fastfloor(this.x2) + 0.5F, PZMath.fastfloor(this.y2) - 0.5F, -666.0F, -666);
            this.cx3 = (int)IsoUtils.XToScreen(PZMath.fastfloor(this.x3) + 1.5F, PZMath.fastfloor(this.y3) + 0.5F, -666.0F, -666);
            this.cy3 = (int)IsoUtils.YToScreen(PZMath.fastfloor(this.x3) + 1.5F, PZMath.fastfloor(this.y3) + 0.5F, -666.0F, -666);
            this.cx4 = (int)IsoUtils.XToScreen(PZMath.fastfloor(this.x4) + 0.5F, PZMath.fastfloor(this.y4) + 1.5F, -666.0F, -666);
            this.cy4 = (int)IsoUtils.YToScreen(PZMath.fastfloor(this.x4) + 0.5F, PZMath.fastfloor(this.y4) + 1.5F, -666.0F, -666);
            this.x3 += 3.0F;
            this.y3 += 3.0F;
            this.x4 += 3.0F;
            this.y4 += 3.0F;
        }

        @Override
        public boolean equals(Object rhs) {
            return !(rhs instanceof WeatherFxMask.RasterizeBounds rhs1)
                ? false
                : this.cx1 == rhs1.cx1
                    && this.cy1 == rhs1.cy1
                    && this.cx2 == rhs1.cx2
                    && this.cy2 == rhs1.cy2
                    && this.cx3 == rhs1.cx3
                    && this.cy3 == rhs1.cy3
                    && this.cx4 == rhs1.cx4
                    && this.cy4 == rhs1.cy4;
        }
    }
}
