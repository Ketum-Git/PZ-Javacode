// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.ArrayList;
import org.lwjgl.opengl.GL11;
import org.lwjglx.opengl.Display;
import org.lwjglx.opengl.DisplayMode;
import org.lwjglx.opengl.PixelFormat;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.audio.ObjectAmbientEmitters;
import zombie.audio.parameters.ParameterInside;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.VBO.GLVertexBufferObject;
import zombie.core.logger.ExceptionLogger;
import zombie.core.opengl.RenderThread;
import zombie.core.physics.WorldSimulation;
import zombie.core.properties.PropertyContainer;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.model.WorldItemAtlas;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TexturePackPage;
import zombie.debug.LineDrawer;
import zombie.gameStates.DebugChunkState;
import zombie.gameStates.MainScreenState;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.PlayerCamera;
import zombie.iso.WorldMarkers;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoRoom;
import zombie.iso.fboRenderChunk.FBORenderAreaHighlights;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.SkyBox;
import zombie.iso.weather.WorldFlares;
import zombie.iso.weather.fx.WeatherFxMask;
import zombie.ui.TextManager;
import zombie.vehicles.BaseVehicle;

public class ServerGUI {
    private static boolean created;
    private static int minX;
    private static int minY;
    private static int maxX;
    private static int maxY;
    private static int maxZ;
    private static final ArrayList<IsoGridSquare> GridStack = new ArrayList<>();
    private static final ArrayList<IsoGridSquare> MinusFloorCharacters = new ArrayList<>(1000);
    private static final ArrayList<IsoGridSquare> SolidFloor = new ArrayList<>(5000);
    private static final ArrayList<IsoGridSquare> VegetationCorpses = new ArrayList<>(5000);
    private static final ColorInfo defColorInfo = new ColorInfo();

    public static boolean isCreated() {
        return created;
    }

    public static void init() {
        created = true;

        try {
            Display.setFullscreen(false);
            Display.setResizable(false);
            Display.setVSyncEnabled(false);
            Display.setTitle("Project Zomboid Server");
            System.setProperty("org.lwjgl.opengl.Window.undecorated", "false");
            Core.width = 1366;
            Core.height = 768;
            Display.setDisplayMode(new DisplayMode(Core.width, Core.height));
            Display.create(new PixelFormat(32, 0, 24, 8, 0));
            Display.setIcon(MainScreenState.loadIcons());
            GLVertexBufferObject.init();
            Display.makeCurrent();
            SpriteRenderer.instance.create();
            TextManager.instance.Init();

            while (TextManager.instance.font.isEmpty()) {
                GameWindow.fileSystem.updateAsyncTransactions();

                try {
                    Thread.sleep(10L);
                } catch (InterruptedException var1) {
                }
            }

            Core.getInstance().initGlobalShader();
            TexturePackPage.ignoreWorldItemTextures = true;
            int flags = 2;
            GameWindow.LoadTexturePack("UI", flags);
            GameWindow.LoadTexturePack("UI2", flags);
            GameWindow.LoadTexturePack("IconsMoveables", flags);
            GameWindow.LoadTexturePack("RadioIcons", flags);
            GameWindow.LoadTexturePack("ApComUI", flags);
            GameWindow.LoadTexturePack("WeatherFx", flags);
            TexturePackPage.ignoreWorldItemTextures = false;
            int var3 = 0;
            GameWindow.LoadTexturePack("Tiles2x", var3);
            GameWindow.LoadTexturePack("JumboTrees2x", var3);
            GameWindow.LoadTexturePack("Overlays2x", var3);
            GameWindow.LoadTexturePack("Tiles2x.floor", 0);
            GameWindow.DoLoadingText("");
            GameWindow.setTexturePackLookup();
            IsoObjectPicker.Instance.Init();
            Display.makeCurrent();
            GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            Display.releaseContext();
            RenderThread.initServerGUI();
            RenderThread.startRendering();
            Core.getInstance().initFBOs();
            Core.getInstance().initShaders();
        } catch (Exception var2) {
            var2.printStackTrace();
            created = false;
        }
    }

    public static void init2() {
        if (created) {
            BaseVehicle.LoadAllVehicleTextures();
        }
    }

    public static void shutdown() {
        if (created) {
            RenderThread.shutdown();
        }
    }

    public static void update() {
        if (created) {
            Mouse.update();
            GameKeyboard.update();
            Display.processMessages();
            if (RenderThread.isCloseRequested()) {
            }

            int wheel = Mouse.getWheelState();
            if (wheel != 0) {
                int del = wheel - 0 < 0 ? 1 : -1;
                Core.getInstance().doZoomScroll(0, del);
            }

            wheel = 0;
            IsoPlayer player = getPlayerToFollow();
            if (player == null) {
                Core.getInstance().StartFrame();
                Core.getInstance().EndFrame();
                Core.getInstance().StartFrameUI();
                SpriteRenderer.instance
                    .renderi(null, 0, 0, Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), 0.0F, 0.0F, 0.0F, 1.0F, null);
                Core.getInstance().EndFrameUI();
            } else {
                DebugChunkState.checkInstance();
                IsoPlayer.setInstance(player);
                IsoPlayer.players[0] = player;
                IsoCamera.setCameraCharacter(player);
                IsoWorld.instance.currentCell.chunkMap[0].calculateZExtentsForChunkMap();
                Core.getInstance().StartFrame(0, true);
                renderWorld();
                Core.getInstance().EndFrame(0);
                Core.getInstance().RenderOffScreenBuffer();
                Core.getInstance().StartFrameUI();
                renderUI();
                Core.getInstance().EndFrameUI();
            }
        }
    }

    private static IsoPlayer getPlayerToFollow() {
        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.isFullyConnected()) {
                for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
                    IsoPlayer player = c.players[playerIndex];
                    if (player != null && player.onlineId != -1) {
                        return player;
                    }
                }
            }
        }

        return null;
    }

    private static void updateCamera(IsoPlayer player) {
        int playerIndex = 0;
        PlayerCamera camera = IsoCamera.cameras[0];
        float OffX = IsoUtils.XToScreen(player.getX() + camera.deferedX, player.getY() + camera.deferedY, player.getZ(), 0);
        float OffY = IsoUtils.YToScreen(player.getX() + camera.deferedX, player.getY() + camera.deferedY, player.getZ(), 0);
        OffX -= IsoCamera.getOffscreenWidth(0) / 2;
        OffY -= IsoCamera.getOffscreenHeight(0) / 2;
        OffY -= player.getOffsetY() * 1.5F;
        OffX += IsoCamera.playerOffsetX;
        OffY += IsoCamera.playerOffsetY;
        camera.offX = OffX;
        camera.offY = OffY;
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        IsoCamera.FrameState frameState = IsoCamera.frameState;
        frameState.paused = false;
        frameState.playerIndex = 0;
        frameState.camCharacter = player;
        frameState.camCharacterX = isoGameCharacter.getX();
        frameState.camCharacterY = isoGameCharacter.getY();
        frameState.camCharacterZ = isoGameCharacter.getZ();
        frameState.camCharacterSquare = isoGameCharacter.getCurrentSquare();
        frameState.camCharacterRoom = frameState.camCharacterSquare == null ? null : frameState.camCharacterSquare.getRoom();
        frameState.offX = IsoCamera.getOffX();
        frameState.offY = IsoCamera.getOffY();
        frameState.offscreenWidth = IsoCamera.getOffscreenWidth(0);
        frameState.offscreenHeight = IsoCamera.getOffscreenHeight(0);
    }

    private static void renderWorld() {
        IsoPlayer player = getPlayerToFollow();
        if (player != null) {
            int playerIndex = 0;
            IsoPlayer.setInstance(player);
            IsoPlayer.players[0] = player;
            IsoCamera.setCameraCharacter(player);
            updateCamera(player);
            SpriteRenderer.instance.doCoreIntParam(0, player.getX());
            SpriteRenderer.instance.doCoreIntParam(1, player.getY());
            SpriteRenderer.instance.doCoreIntParam(2, player.getZ());
            IsoWorld.instance.sceneCullZombies();
            IsoWorld.instance.sceneCullAnimals();
            IsoSprite.globalOffsetX = -1.0F;
            IsoCell cell = IsoWorld.instance.currentCell;

            try {
                WeatherFxMask.initMask();
                DeadBodyAtlas.instance.render();
                WorldItemAtlas.instance.render();
                cell.render();
                DeadBodyAtlas.instance.renderDebug();
                WorldSoundManager.instance.render();
                WorldFlares.debugRender();
                WorldMarkers.instance.debugRender();
                ObjectAmbientEmitters.getInstance().render();
                if (PerformanceSettings.fboRenderChunk) {
                    FBORenderAreaHighlights.getInstance().render();
                }

                ParameterInside.renderDebug();
                LineDrawer.render();
                SkyBox.getInstance().render();
            } catch (Throwable var13) {
                ExceptionLogger.logException(var13);
            }

            int x1 = 0;
            int y1 = 0;
            int x2 = 0 + IsoCamera.getOffscreenWidth(0);
            int y2 = 0 + IsoCamera.getOffscreenHeight(0);
            float topLeftX = IsoUtils.XToIso(0.0F, 0.0F, 0.0F);
            float topRightY = IsoUtils.YToIso(x2, 0.0F, 0.0F);
            float bottomRightX = IsoUtils.XToIso(x2, y2, 6.0F);
            float bottomLeftY = IsoUtils.YToIso(0.0F, y2, 6.0F);
            minY = (int)topRightY;
            maxY = (int)bottomLeftY;
            minX = (int)topLeftX;
            maxX = (int)bottomRightX;
            minX -= 2;
            minY -= 2;
            maxZ = (int)player.getZ();
            IsoCell cellx = IsoWorld.instance.currentCell;
            cellx.DrawStencilMask();
            IsoObjectPicker.Instance.StartRender();
            RenderTiles();

            for (int n = 0; n < cellx.getObjectList().size(); n++) {
                IsoMovingObject obj = cellx.getObjectList().get(n);
                obj.renderlast();
            }

            for (int i = 0; i < cellx.getStaticUpdaterObjectList().size(); i++) {
                IsoObject obj = cellx.getStaticUpdaterObjectList().get(i);
                obj.renderlast();
            }

            if (WorldSimulation.instance.created) {
                TextureDraw.GenericDrawer drawer = WorldSimulation.getDrawer(0);
                SpriteRenderer.instance.drawGeneric(drawer);
            }

            WorldSoundManager.instance.render();
            LineDrawer.clear();
        }
    }

    private static void RenderTiles() {
        IsoCell cell = IsoWorld.instance.currentCell;
        int playerIndex = 0;
        if (IsoCell.perPlayerRender[0] == null) {
            IsoCell.perPlayerRender[0] = new IsoCell.PerPlayerRender();
        }

        IsoCell.PerPlayerRender perPlayerRender = IsoCell.perPlayerRender[0];
        if (perPlayerRender == null) {
            IsoCell.perPlayerRender[0] = new IsoCell.PerPlayerRender();
        }

        perPlayerRender.setSize(maxX - minX + 1, maxY - minY + 1);

        for (int zza = 0; zza <= maxZ; zza++) {
            GridStack.clear();

            for (int n = minY; n < maxY; n++) {
                int x = minX;
                IsoGridSquare square = ServerMap.instance.getGridSquare(x, n, zza);
                IsoDirections navStepDirIdx = IsoDirections.E;

                while (x < maxX) {
                    if (square != null && square.getY() != n) {
                        square = null;
                    }

                    if (square == null) {
                        square = ServerMap.instance.getGridSquare(x, n, zza);
                        if (square == null) {
                            x++;
                            continue;
                        }
                    }

                    IsoChunk c = square.getChunk();
                    if (c != null && square.IsOnScreen()) {
                        GridStack.add(square);
                    }

                    square = square.getAdjacentSquare(navStepDirIdx);
                    x++;
                }
            }

            SolidFloor.clear();
            VegetationCorpses.clear();
            MinusFloorCharacters.clear();

            for (int i = 0; i < GridStack.size(); i++) {
                IsoGridSquare square = GridStack.get(i);
                square.setLightInfoServerGUIOnly(defColorInfo);
                int flags = renderFloor(square);
                if (!square.getStaticMovingObjects().isEmpty()) {
                    flags |= 2;
                }

                for (int m = 0; m < square.getMovingObjects().size(); m++) {
                    IsoMovingObject mov = square.getMovingObjects().get(m);
                    boolean bOnFloor = mov.isOnFloor();
                    if (bOnFloor && mov instanceof IsoZombie zombie) {
                        bOnFloor = zombie.crawling
                            || zombie.legsSprite.currentAnim != null && zombie.legsSprite.currentAnim.name.equals("ZombieDeath") && zombie.def.isFinished();
                    }

                    if (bOnFloor) {
                        flags |= 2;
                    } else {
                        flags |= 4;
                    }
                }

                if ((flags & 1) != 0) {
                    SolidFloor.add(square);
                }

                if ((flags & 2) != 0) {
                    VegetationCorpses.add(square);
                }

                if ((flags & 4) != 0) {
                    MinusFloorCharacters.add(square);
                }
            }

            LuaEventManager.triggerEvent("OnPostFloorLayerDraw", zza);

            for (int i = 0; i < VegetationCorpses.size(); i++) {
                IsoGridSquare squarex = VegetationCorpses.get(i);
                renderMinusFloor(squarex, false, true);
                renderCharacters(squarex, true);
            }

            for (int i = 0; i < MinusFloorCharacters.size(); i++) {
                IsoGridSquare squarex = MinusFloorCharacters.get(i);
                boolean hasSE = renderMinusFloor(squarex, false, false);
                renderCharacters(squarex, false);
                if (hasSE) {
                    renderMinusFloor(squarex, true, false);
                }
            }
        }

        MinusFloorCharacters.clear();
        SolidFloor.clear();
        VegetationCorpses.clear();
    }

    private static int renderFloor(IsoGridSquare square) {
        int flags = 0;
        int playerIndex = 0;

        for (int i = 0; i < square.getObjects().size(); i++) {
            IsoObject obj = square.getObjects().get(i);
            boolean bDoIt = true;
            if (obj.sprite != null && !obj.sprite.properties.has(IsoFlagType.solidfloor)) {
                bDoIt = false;
                flags |= 4;
            }

            if (bDoIt) {
                IndieGL.glAlphaFunc(516, 0.0F);
                obj.setAlphaAndTarget(0, 1.0F);
                obj.render(square.x, square.y, square.z, defColorInfo, true, false, null);
                obj.renderObjectPicker(square.x, square.y, square.z, defColorInfo);
                if (obj.isHighlightRenderOnce()) {
                    obj.setHighlighted(false, false);
                }

                flags |= 1;
            }

            if (!bDoIt && obj.sprite != null && (obj.sprite.properties.has(IsoFlagType.canBeRemoved) || obj.sprite.properties.has(IsoFlagType.attachedFloor))) {
                flags |= 2;
            }
        }

        return flags;
    }

    private static boolean isSpriteOnSouthOrEastWall(IsoObject obj) {
        if (obj instanceof IsoBarricade) {
            return obj.getDir() == IsoDirections.S || obj.getDir() == IsoDirections.E;
        } else if (obj instanceof IsoCurtain curtain) {
            return curtain.getType() == IsoObjectType.curtainS || curtain.getType() == IsoObjectType.curtainE;
        } else {
            PropertyContainer properties = obj.getProperties();
            return properties != null && (properties.has(IsoFlagType.attachedE) || properties.has(IsoFlagType.attachedS));
        }
    }

    private static int DoWallLightingN(IsoGridSquare square, IsoObject obj, int stenciled) {
        obj.render(square.x, square.y, square.z, defColorInfo, true, false, null);
        return stenciled;
    }

    private static int DoWallLightingW(IsoGridSquare square, IsoObject obj, int stenciled) {
        obj.render(square.x, square.y, square.z, defColorInfo, true, false, null);
        return stenciled;
    }

    private static int DoWallLightingNW(IsoGridSquare square, IsoObject obj, int stenciled) {
        obj.render(square.x, square.y, square.z, defColorInfo, true, false, null);
        return stenciled;
    }

    private static boolean renderMinusFloor(IsoGridSquare square, boolean doSE, boolean vegitationRender) {
        int start = doSE ? square.getObjects().size() - 1 : 0;
        int end = doSE ? 0 : square.getObjects().size() - 1;
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoGridSquare CamCharacterSquare = IsoCamera.frameState.camCharacterSquare;
        IsoRoom CamCharacterRoom = IsoCamera.frameState.camCharacterRoom;
        boolean bCouldSee = true;
        float darkMulti = 1.0F;
        int sx = (int)(IsoUtils.XToScreenInt(square.x, square.y, square.z, 0) - IsoCamera.frameState.offX);
        int sy = (int)(IsoUtils.YToScreenInt(square.x, square.y, square.z, 0) - IsoCamera.frameState.offY);
        boolean bInStencilRect = true;
        IsoCell cell = square.getCell();
        if (sx + 32 * Core.tileScale <= cell.stencilX1
            || sx - 32 * Core.tileScale >= cell.stencilX2
            || sy + 32 * Core.tileScale <= cell.stencilY1
            || sy - 96 * Core.tileScale >= cell.stencilY2) {
            bInStencilRect = false;
        }

        int stenciled = 0;
        boolean hasSE = false;

        for (int n = start; doSE ? n >= end : n <= end; n += doSE ? -1 : 1) {
            IsoObject obj = square.getObjects().get(n);
            boolean bDoIt = true;
            IsoGridSquare.circleStencil = false;
            if (obj.sprite != null && obj.sprite.getProperties().has(IsoFlagType.solidfloor)) {
                bDoIt = false;
            }

            if ((
                    !vegitationRender
                        || obj.sprite == null
                        || obj.sprite.properties.has(IsoFlagType.canBeRemoved)
                        || obj.sprite.properties.has(IsoFlagType.attachedFloor)
                )
                && (
                    vegitationRender
                        || obj.sprite == null
                        || !obj.sprite.properties.has(IsoFlagType.canBeRemoved) && !obj.sprite.properties.has(IsoFlagType.attachedFloor)
                )) {
                IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
                if (obj.sprite != null
                    && (
                        obj.sprite.getType() == IsoObjectType.WestRoofB
                            || obj.sprite.getType() == IsoObjectType.WestRoofM
                            || obj.sprite.getType() == IsoObjectType.WestRoofT
                    )
                    && square.z == maxZ
                    && square.z == isoGameCharacter.getZi()) {
                    bDoIt = false;
                }

                if (isoGameCharacter.isClimbing() && obj.sprite != null && !obj.sprite.getProperties().has(IsoFlagType.solidfloor)) {
                    bDoIt = true;
                }

                if (isSpriteOnSouthOrEastWall(obj)) {
                    if (!doSE) {
                        bDoIt = false;
                    }

                    hasSE = true;
                } else if (doSE) {
                    bDoIt = false;
                }

                if (bDoIt) {
                    IndieGL.glAlphaFunc(516, 0.0F);
                    if (obj.sprite == null
                        || square.getProperties().has(IsoFlagType.blueprint)
                        || obj.sprite.getType() != IsoObjectType.doorFrW
                            && obj.sprite.getType() != IsoObjectType.doorFrN
                            && obj.sprite.getType() != IsoObjectType.doorW
                            && obj.sprite.getType() != IsoObjectType.doorN
                            && !obj.sprite.getProperties().has(IsoFlagType.cutW)
                            && !obj.sprite.getProperties().has(IsoFlagType.cutN)) {
                        if (CamCharacterSquare != null) {
                        }

                        obj.setTargetAlpha(playerIndex, 1.0F);
                        if (isoGameCharacter != null
                            && obj.getProperties() != null
                            && (obj.getProperties().has(IsoFlagType.solid) || obj.getProperties().has(IsoFlagType.solidtrans))) {
                            int dx = square.getX() - (int)isoGameCharacter.getX();
                            int dy = square.getY() - (int)isoGameCharacter.getY();
                            if (dx > 0 && dx < 3 && dy >= 0 && dy < 3 || dy > 0 && dy < 3 && dx >= 0 && dx < 3) {
                                obj.setTargetAlpha(playerIndex, 0.99F);
                            }
                        }

                        if (obj instanceof IsoWindow w && obj.getTargetAlpha(playerIndex) < 1.0E-4F) {
                            IsoGridSquare oppositeSq = w.getOppositeSquare();
                            if (oppositeSq != null && oppositeSq != square && oppositeSq.lighting[playerIndex].bSeen()) {
                                obj.setTargetAlpha(playerIndex, oppositeSq.lighting[playerIndex].darkMulti() * 2.0F);
                            }
                        }

                        if (obj instanceof IsoTree isoTree) {
                            if (bInStencilRect
                                && square.x >= (int)IsoCamera.frameState.camCharacterX
                                && square.y >= (int)IsoCamera.frameState.camCharacterY
                                && CamCharacterSquare != null
                                && CamCharacterSquare.has(IsoFlagType.exterior)) {
                                isoTree.renderFlag = true;
                            } else {
                                isoTree.renderFlag = false;
                            }
                        }

                        obj.render(square.x, square.y, square.z, defColorInfo, true, false, null);
                    } else {
                        if (obj.getTargetAlpha(playerIndex) < 1.0F) {
                            boolean bForceNoStencil = false;
                            if (bForceNoStencil) {
                                if (obj.sprite.getProperties().has(IsoFlagType.cutW) && square.getProperties().has(IsoFlagType.WallSE)) {
                                    IsoGridSquare toNorthWest = square.getAdjacentSquare(IsoDirections.NW);
                                    if (toNorthWest == null || toNorthWest.getRoom() == null) {
                                        bForceNoStencil = false;
                                    }
                                } else if (obj.sprite.getType() != IsoObjectType.doorFrW
                                    && obj.sprite.getType() != IsoObjectType.doorW
                                    && !obj.sprite.getProperties().has(IsoFlagType.cutW)) {
                                    if (obj.sprite.getType() == IsoObjectType.doorFrN
                                        || obj.sprite.getType() == IsoObjectType.doorN
                                        || obj.sprite.getProperties().has(IsoFlagType.cutN)) {
                                        IsoGridSquare toNorth = square.getAdjacentSquare(IsoDirections.N);
                                        if (toNorth == null || toNorth.getRoom() == null) {
                                            bForceNoStencil = false;
                                        }
                                    }
                                } else {
                                    IsoGridSquare toWest = square.getAdjacentSquare(IsoDirections.W);
                                    if (toWest == null || toWest.getRoom() == null) {
                                        bForceNoStencil = false;
                                    }
                                }
                            }

                            if (!bForceNoStencil) {
                                IsoGridSquare.circleStencil = bInStencilRect;
                            }

                            obj.setAlphaAndTarget(playerIndex, 1.0F);
                        }

                        if (obj.sprite.getProperties().has(IsoFlagType.cutW) && obj.sprite.getProperties().has(IsoFlagType.cutN)) {
                            stenciled = DoWallLightingNW(square, obj, stenciled);
                        } else if (obj.sprite.getType() == IsoObjectType.doorFrW
                            || obj.sprite.getType() == IsoObjectType.doorW
                            || obj.sprite.getProperties().has(IsoFlagType.cutW)) {
                            stenciled = DoWallLightingW(square, obj, stenciled);
                        } else if (obj.sprite.getType() == IsoObjectType.doorFrN
                            || obj.sprite.getType() == IsoObjectType.doorN
                            || obj.sprite.getProperties().has(IsoFlagType.cutN)) {
                            stenciled = DoWallLightingN(square, obj, stenciled);
                        }
                    }

                    if (obj.sprite != null) {
                        obj.renderObjectPicker(square.x, square.y, square.z, defColorInfo);
                    }

                    if (obj.isHighlightRenderOnce()) {
                        obj.setHighlighted(false, false);
                    }
                }
            }
        }

        return hasSE;
    }

    private static void renderCharacters(IsoGridSquare square, boolean deadRender) {
        int size = square.getStaticMovingObjects().size();

        for (int n = 0; n < size; n++) {
            IsoMovingObject mov = square.getStaticMovingObjects().get(n);
            if (mov.sprite != null && (!deadRender || mov instanceof IsoDeadBody) && (deadRender || !(mov instanceof IsoDeadBody))) {
                mov.render(mov.getX(), mov.getY(), mov.getZ(), defColorInfo, true, false, null);
                mov.renderObjectPicker(mov.getX(), mov.getY(), mov.getZ(), defColorInfo);
            }
        }

        size = square.getMovingObjects().size();

        for (int nx = 0; nx < size; nx++) {
            IsoMovingObject mov = square.getMovingObjects().get(nx);
            if (mov != null && mov.sprite != null) {
                boolean bOnFloor = mov.isOnFloor();
                if (bOnFloor && mov instanceof IsoZombie zombie) {
                    bOnFloor = zombie.crawling
                        || zombie.legsSprite.currentAnim != null && zombie.legsSprite.currentAnim.name.equals("ZombieDeath") && zombie.def.isFinished();
                }

                if ((!deadRender || bOnFloor) && (deadRender || !bOnFloor)) {
                    mov.setAlphaAndTarget(0, 1.0F);
                    if (mov instanceof IsoGameCharacter chr) {
                        chr.renderServerGUI();
                    } else {
                        mov.render(mov.getX(), mov.getY(), mov.getZ(), defColorInfo, true, false, null);
                    }

                    mov.renderObjectPicker(mov.getX(), mov.getY(), mov.getZ(), defColorInfo);
                }
            }
        }
    }

    private static void renderUI() {
    }
}
