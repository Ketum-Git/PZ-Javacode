// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Stack;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.AmbientStreamManager;
import zombie.FliesSound;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZomboidFileSystem;
import zombie.Lua.Event;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ai.astar.Mover;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.chat.ChatElement;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.config.IntegerConfigOption;
import zombie.config.StringConfigOption;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.properties.PropertyContainer;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;
import zombie.core.utils.BooleanGrid;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.erosion.ErosionData;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.inventory.InventoryItem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoObject;
import zombie.iso.IsoObjectPicker;
import zombie.iso.IsoRoomLight;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.NearestWalls;
import zombie.iso.ParticlesFire;
import zombie.iso.PlayerCamera;
import zombie.iso.RoomDef;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.regions.IWorldRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.fboRenderChunk.FBORenderOcclusion;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.zones.VehicleZone;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.pathfind.LiangBarsky;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.randomizedWorld.randomizedVehicleStory.VehicleStorySpawner;
import zombie.ui.TextDrawObject;
import zombie.ui.TextManager;
import zombie.ui.UIElementInterface;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.ClipperOffset;
import zombie.vehicles.EditVehicleState;

@UsedFromLua
public final class DebugChunkState extends GameState {
    public static DebugChunkState instance;
    private EditVehicleState.LuaEnvironment luaEnv;
    private boolean exit;
    private final ArrayList<UIElementInterface> gameUi = new ArrayList<>();
    private final ArrayList<UIElementInterface> selfUi = new ArrayList<>();
    private boolean suspendUi;
    private final ArrayList<Event> eventList = new ArrayList<>();
    private final HashMap<String, Event> eventMap = new HashMap<>();
    private KahluaTable table;
    private int playerIndex;
    public int z;
    private int gridX = -1;
    private int gridY = -1;
    public float gridXf;
    public float gridYf;
    private static final UIFont FONT = UIFont.DebugConsole;
    private String vehicleStoryName = "Basic Car Crash";
    private static boolean keyQpressed;
    private final DebugChunkState.GeometryDrawer[] geometryDrawers = new DebugChunkState.GeometryDrawer[3];
    private InventoryItem inventoryItem1;
    private static final ClipperOffset m_clipperOffset = null;
    private static ByteBuffer clipperBuffer;
    private static final int VERSION = 1;
    private final ArrayList<ConfigOption> options = new ArrayList<>();
    private final ArrayList<ConfigOption> optionsHidden = new ArrayList<>();
    private final DebugChunkState.BooleanDebugOption buildingRect = new DebugChunkState.BooleanDebugOption("BuildingRect", true);
    private final DebugChunkState.BooleanDebugOption chunkGrid = new DebugChunkState.BooleanDebugOption("ChunkGrid", true);
    private final DebugChunkState.BooleanDebugOption chunkColorTexture = new DebugChunkState.BooleanDebugOption("ChunkColorTexture", true);
    private final DebugChunkState.BooleanDebugOption chunkDepthTexture = new DebugChunkState.BooleanDebugOption("ChunkDepthTexture", false);
    private final DebugChunkState.BooleanDebugOption closestRoomSquare = new DebugChunkState.BooleanDebugOption("ClosestRoomSquare", true);
    private final DebugChunkState.BooleanDebugOption depthValues = new DebugChunkState.BooleanDebugOption("DepthValues", false);
    private final DebugChunkState.BooleanDebugOption emptySquares = new DebugChunkState.BooleanDebugOption("EmptySquares", true);
    private final DebugChunkState.BooleanDebugOption flyBuzzEmitters = new DebugChunkState.BooleanDebugOption("FlyBuzzEmitters", true);
    private final DebugChunkState.BooleanDebugOption lightSquares = new DebugChunkState.BooleanDebugOption("LightSquares", true);
    private final DebugChunkState.BooleanDebugOption lineClearCollide = new DebugChunkState.BooleanDebugOption("LineClearCollide", true);
    private final DebugChunkState.BooleanDebugOption nearestWalls = new DebugChunkState.BooleanDebugOption("NearestWalls", false);
    private final DebugChunkState.BooleanDebugOption nearestExteriorWalls = new DebugChunkState.BooleanDebugOption("NearestExteriorWalls", false);
    private final DebugChunkState.BooleanDebugOption objectAtCursor = new DebugChunkState.BooleanDebugOption("ObjectAtCursor", false);
    private final DebugChunkState.StringDebugOption objectAtCursorId = new DebugChunkState.StringDebugOption("ObjectAtCursor.id", "cylinder");
    private final DebugChunkState.IntegerDebugOption objectAtCursorLevels = new DebugChunkState.IntegerDebugOption("ObjectAtCursor.levels", 1, 10, 1);
    private final DebugChunkState.DoubleDebugOption objectAtCursorScale = new DebugChunkState.DoubleDebugOption("ObjectAtCursor.scale", 1.0, 10.0, 1.0);
    private final DebugChunkState.DoubleDebugOption objectAtCursorWidth = new DebugChunkState.DoubleDebugOption("ObjectAtCursor.width", 0.5, 20.0, 1.0);
    private final DebugChunkState.BooleanDebugOption objectPicker = new DebugChunkState.BooleanDebugOption("ObjectPicker", true);
    private final DebugChunkState.BooleanDebugOption occludedSquares = new DebugChunkState.BooleanDebugOption("OccludedSquares", false);
    private final DebugChunkState.BooleanDebugOption roofHideBuilding = new DebugChunkState.BooleanDebugOption("RoofHideBuilding", false);
    private final DebugChunkState.BooleanDebugOption roomLightRects = new DebugChunkState.BooleanDebugOption("RoomLightRects", true);
    private final DebugChunkState.BooleanDebugOption vehicleStory = new DebugChunkState.BooleanDebugOption("VehicleStory", true);
    private final DebugChunkState.BooleanDebugOption randomSquareInZone = new DebugChunkState.BooleanDebugOption("RandomSquareInZone", true);
    private final DebugChunkState.BooleanDebugOption zoneRect = new DebugChunkState.BooleanDebugOption("ZoneRect", true);

    public DebugChunkState() {
        instance = this;
    }

    @Override
    public void enter() {
        instance = this;
        this.load();
        if (this.luaEnv == null) {
            this.luaEnv = new EditVehicleState.LuaEnvironment(LuaManager.platform, LuaManager.converterManager, LuaManager.env);
        }

        this.saveGameUI();
        if (this.selfUi.isEmpty()) {
            IsoPlayer player = IsoPlayer.players[this.playerIndex];
            this.z = player == null ? 0 : PZMath.fastfloor(player.getZ());
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.luaEnv.env.rawget("DebugChunkState_InitUI"), this);
            if (this.table != null && this.table.getMetatable() != null) {
                this.table.getMetatable().rawset("_LUA_RELOADED_CHECK", Boolean.FALSE);
            }
        } else {
            UIManager.UI.addAll(this.selfUi);
            this.luaEnv.caller.pcall(this.luaEnv.thread, this.table.rawget("showUI"), this.table);
        }

        this.exit = false;
    }

    @Override
    public void yield() {
        this.restoreGameUI();
    }

    @Override
    public void reenter() {
        this.saveGameUI();
    }

    @Override
    public void exit() {
        this.save();
        this.restoreGameUI();

        for (int i = 0; i < IsoCamera.cameras.length; i++) {
            IsoCamera.cameras[i].deferedX = IsoCamera.cameras[i].deferedY = 0.0F;
        }
    }

    @Override
    public void render() {
        IsoPlayer.setInstance(IsoPlayer.players[this.playerIndex]);
        IsoCamera.setCameraCharacter(IsoPlayer.players[this.playerIndex]);
        boolean clear = true;

        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            if (pn != this.playerIndex && IsoPlayer.players[pn] != null) {
                Core.getInstance().StartFrame(pn, clear);
                Core.getInstance().EndFrame(pn);
                clear = false;
            }
        }

        Core.getInstance().StartFrame(this.playerIndex, clear);
        this.renderScene();
        Core.getInstance().EndFrame(this.playerIndex);
        Core.getInstance().RenderOffScreenBuffer();

        for (int pnx = 0; pnx < IsoPlayer.numPlayers; pnx++) {
            TextDrawObject.NoRender(pnx);
            ChatElement.NoRender(pnx);
        }

        if (Core.getInstance().StartFrameUI()) {
            this.renderUI();
        }

        Core.getInstance().EndFrameUI();
    }

    @Override
    public GameStateMachine.StateAction update() {
        return !this.exit && !GameKeyboard.isKeyPressed(60) ? this.updateScene() : GameStateMachine.StateAction.Continue;
    }

    public static DebugChunkState checkInstance() {
        instance = null;
        if (instance != null) {
            if (instance.table != null && instance.table.getMetatable() != null) {
                if (instance.table.getMetatable().rawget("_LUA_RELOADED_CHECK") == null) {
                    instance = null;
                }
            } else {
                instance = null;
            }
        }

        return instance == null ? new DebugChunkState() : instance;
    }

    public void renderScene() {
        IsoCamera.frameState.set(this.playerIndex);
        IsoCamera.frameState.paused = true;
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        SpriteRenderer.instance.doCoreIntParam(0, isoGameCharacter.getX());
        SpriteRenderer.instance.doCoreIntParam(1, isoGameCharacter.getY());
        SpriteRenderer.instance.doCoreIntParam(2, IsoCamera.frameState.camCharacterZ);
        IsoSprite.globalOffsetX = -1.0F;
        IsoWorld.instance.currentCell.render();
        this.drawTestModels();
        IndieGL.StartShader(0, this.playerIndex);
        IndieGL.disableDepthTest();
        if (this.chunkGrid.getValue()) {
            this.drawGrid();
        }

        this.drawCursor();
        if (this.lightSquares.getValue()) {
            Stack<IsoLightSource> lampposts = IsoWorld.instance.getCell().getLamppostPositions();

            for (int i = 0; i < lampposts.size(); i++) {
                IsoLightSource l = lampposts.get(i);
                if (l.z == this.z) {
                    this.paintSquare(l.x, l.y, l.z, 1.0F, 1.0F, 0.0F, 0.5F);
                }
            }
        }

        if (this.zoneRect.getValue()) {
            this.drawZones();
        }

        if (this.buildingRect.getValue()) {
            IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(this.gridX, this.gridY, this.z);
            if (sq != null && sq.getBuilding() != null) {
                BuildingDef def = sq.getBuilding().getDef();
                this.DrawIsoLine(def.getX(), def.getY(), def.getX2(), def.getY(), 1.0F, 1.0F, 1.0F, 1.0F, 2);
                this.DrawIsoLine(def.getX2(), def.getY(), def.getX2(), def.getY2(), 1.0F, 1.0F, 1.0F, 1.0F, 2);
                this.DrawIsoLine(def.getX2(), def.getY2(), def.getX(), def.getY2(), 1.0F, 1.0F, 1.0F, 1.0F, 2);
                this.DrawIsoLine(def.getX(), def.getY2(), def.getX(), def.getY(), 1.0F, 1.0F, 1.0F, 1.0F, 2);
            }
        }

        if (this.buildingRect.getValue() && this.z >= 0) {
            IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(this.gridX, this.gridY, this.z);
            RoomDef roomDef2 = IsoWorld.instance.getMetaGrid().getRoomAt(this.gridX, this.gridY, this.z);
            IWorldRegion wr = IsoRegions.getIsoWorldRegion(this.gridX, this.gridY, this.z);
            if (sq != null && sq.getIsoWorldRegion() instanceof IsoWorldRegion worldRegion) {
                BuildingDef buildingDef = worldRegion.getBuildingDef();
                if (buildingDef != null) {
                    for (RoomDef roomDef : buildingDef.getRooms()) {
                        if (roomDef.level == this.z) {
                            float r = 1.0F;
                            float g = 1.0F;
                            float b = 1.0F;
                            if (roomDef.contains(this.gridX, this.gridY)) {
                                g = 0.0F;
                                r = 0.0F;
                                RoomDef roomDef1 = IsoWorld.instance.getMetaGrid().getRoomAt(this.gridX, this.gridY, this.z);
                                boolean rect = true;
                            }

                            for (RoomDef.RoomRect rect : roomDef.getRects()) {
                                this.DrawIsoRect(rect.getX(), rect.getY(), rect.getW(), rect.getH(), r, g, 1.0F, 1.0F, 1);
                            }
                        }
                    }
                }
            }
        }

        if (this.roomLightRects.getValue()) {
            ArrayList<IsoRoomLight> roomLights = IsoWorld.instance.currentCell.roomLights;

            for (int ix = 0; ix < roomLights.size(); ix++) {
                IsoRoomLight roomLight = roomLights.get(ix);
                if (roomLight.z == this.z) {
                    this.DrawIsoRect(roomLight.x, roomLight.y, roomLight.width, roomLight.height, 0.0F, 1.0F, 1.0F, 1.0F, 1);
                }
            }
        }

        if (this.flyBuzzEmitters.getValue()) {
            FliesSound.instance.render(this.playerIndex);
        }

        if (this.closestRoomSquare.getValue()) {
            float px = IsoPlayer.players[this.playerIndex].getX();
            float py = IsoPlayer.players[this.playerIndex].getY();
            if (AmbientStreamManager.getInstance() instanceof AmbientStreamManager) {
                BuildingDef buildingDef = AmbientStreamManager.getNearestBuilding(px, py);
                if (buildingDef != null) {
                    Vector2f closestXY = BaseVehicle.allocVector2f();
                    buildingDef.getClosestPoint(px, py, closestXY);
                    this.DrawIsoLine(px, py, closestXY.x, closestXY.y, 1.0F, 1.0F, 1.0F, 1.0F, 1);
                    BaseVehicle.releaseVector2f(closestXY);
                }
            }
        }

        if (this.table != null && this.table.rawget("selectedSquare") != null && this.table.rawget("selectedSquare") instanceof IsoGridSquare square) {
            this.DrawIsoRect(square.x, square.y, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 2);
        }

        LineDrawer.render();
        LineDrawer.clear();
    }

    private void renderUI() {
        int playerIndex = this.playerIndex;
        Stack<IsoLightSource> lampposts = IsoWorld.instance.getCell().getLamppostPositions();
        int activeLights = 0;

        for (int i = 0; i < lampposts.size(); i++) {
            IsoLightSource l = lampposts.get(i);
            if (l.active) {
                activeLights++;
            }
        }

        if (PerformanceSettings.fboRenderChunk) {
            int x = 220 + Core.getInstance().getOptionFontSizeReal() * 50;
            int y = 80;
            if (this.chunkColorTexture.getValue()) {
                x = this.renderChunkTexture(x, 80, true);
            }

            if (this.chunkDepthTexture.getValue()) {
                this.renderChunkTexture(x, 80, false);
            }
        }

        UIManager.render();
    }

    private int renderChunkTexture(int x, int y, boolean colorTexture) {
        IsoChunk chunk1 = IsoWorld.instance.currentCell.getChunkForGridSquare(this.gridX, this.gridY, 0);
        if (chunk1 == null) {
            return x;
        } else {
            FBORenderLevels renderLevels = chunk1.getRenderLevels(this.playerIndex);
            FBORenderChunk rc = renderLevels.getFBOForLevel(this.z, Core.getInstance().getZoom(this.playerIndex));
            if (rc != null && rc.isInit) {
                Texture tex = colorTexture ? rc.tex : rc.depth;
                float scale = 0.5F;
                boolean bRCM = DebugOptions.instance.fboRenderChunk.combinedFbo.getValue();
                if (bRCM) {
                    tex = colorTexture ? FBORenderChunkManager.instance.combinedTexture : FBORenderChunkManager.instance.combinedDepthTexture;
                    scale = 0.125F;
                }

                if (tex == null) {
                    return x;
                } else {
                    SpriteRenderer.instance.render(null, x, y, tex.getWidth() * scale, tex.getHeight() * scale, 0.2F, 0.2F, 0.2F, 1.0F, null);
                    SpriteRenderer.instance.render(tex, x, y, tex.getWidth() * scale, tex.getHeight() * scale, 1.0F, 1.0F, 1.0F, 1.0F, null);
                    TextManager.instance
                        .DrawString(x + 4, y + 4, "Level %d-%d / %d-%d".formatted(rc.getMinLevel(), rc.getTopLevel(), chunk1.minLevel, chunk1.maxLevel));
                    return x + (int)Math.ceil(tex.getWidth() * scale);
                }
            } else {
                return x;
            }
        }
    }

    public void setTable(KahluaTable table) {
        this.table = table;
    }

    public GameStateMachine.StateAction updateScene() {
        IsoPlayer.setInstance(IsoPlayer.players[this.playerIndex]);
        IsoCamera.setCameraCharacter(IsoPlayer.players[this.playerIndex]);
        UIManager.setPicked(IsoObjectPicker.Instance.ContextPick(Mouse.getXA(), Mouse.getYA()));
        IsoObject mouseOverObject = UIManager.getPicked() == null ? null : UIManager.getPicked().tile;
        UIManager.setLastPicked(mouseOverObject);
        if (GameKeyboard.isKeyDown(19)) {
            if (!keyQpressed) {
                DebugOptions.instance.terrain.renderTiles.newRender.setValue(true);
                keyQpressed = true;
                DebugLog.General.debugln("IsoCell.newRender = %s", DebugOptions.instance.terrain.renderTiles.newRender.getValue());
            }
        } else {
            keyQpressed = false;
        }

        if (GameKeyboard.isKeyDown(20)) {
            if (!keyQpressed) {
                DebugOptions.instance.terrain.renderTiles.newRender.setValue(false);
                keyQpressed = true;
                DebugLog.General.debugln("IsoCell.newRender = %s", DebugOptions.instance.terrain.renderTiles.newRender.getValue());
            }
        } else {
            keyQpressed = false;
        }

        if (GameKeyboard.isKeyDown(31)) {
            if (!keyQpressed) {
                ParticlesFire.getInstance().reloadShader();
                keyQpressed = true;
                DebugLog.General.debugln("ParticlesFire.reloadShader");
            }
        } else {
            keyQpressed = false;
        }

        IsoCamera.update();
        this.updateCursor();
        return GameStateMachine.StateAction.Remain;
    }

    private void saveGameUI() {
        this.gameUi.clear();
        this.gameUi.addAll(UIManager.UI);
        UIManager.UI.clear();
        this.suspendUi = UIManager.suspend;
        UIManager.suspend = false;
        UIManager.setShowPausedMessage(false);
        UIManager.defaultthread = this.luaEnv.thread;
        LuaEventManager.getEvents(this.eventList, this.eventMap);
        LuaEventManager.setEvents(new ArrayList<>(), new HashMap<>());
    }

    private void restoreGameUI() {
        this.selfUi.clear();
        this.selfUi.addAll(UIManager.UI);
        UIManager.UI.clear();
        UIManager.UI.addAll(this.gameUi);
        UIManager.suspend = this.suspendUi;
        UIManager.setShowPausedMessage(true);
        UIManager.defaultthread = LuaManager.thread;
        LuaEventManager.setEvents(this.eventList, this.eventMap);
        this.eventList.clear();
        this.eventMap.clear();
    }

    public Object fromLua0(String func) {
        switch (func) {
            case "exit":
                this.exit = true;
                return null;
            case "getCameraDragX":
                return BoxedStaticValues.toDouble(-IsoCamera.cameras[this.playerIndex].deferedX);
            case "getCameraDragY":
                return BoxedStaticValues.toDouble(-IsoCamera.cameras[this.playerIndex].deferedY);
            case "getPlayerIndex":
                return BoxedStaticValues.toDouble(this.playerIndex);
            case "getVehicleStory":
                return this.vehicleStoryName;
            case "getZ":
                return BoxedStaticValues.toDouble(this.z);
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\"", func));
        }
    }

    public Object fromLua1(String func, Object arg0) {
        switch (func) {
            case "getCameraDragX":
                return BoxedStaticValues.toDouble(-IsoCamera.cameras[this.playerIndex].deferedX);
            case "getCameraDragY":
                return BoxedStaticValues.toDouble(-IsoCamera.cameras[this.playerIndex].deferedY);
            case "getObjectAtCursor":
                String var5 = (String)arg0;

                return switch (var5) {
                    case "id" -> this.objectAtCursorId.getValue();
                    case "levels" -> this.objectAtCursorLevels.getValue();
                    case "scale" -> this.objectAtCursorScale.getValue();
                    case "width" -> this.objectAtCursorWidth.getValue();
                    default -> throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
                };
            case "setPlayerIndex":
                this.playerIndex = PZMath.clamp(((Double)arg0).intValue(), 0, 3);
                return null;
            case "setVehicleStory":
                this.vehicleStoryName = (String)arg0;
                return null;
            case "setZ":
                this.z = PZMath.clamp(((Double)arg0).intValue(), -31, 31);
                return null;
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\"", func, arg0));
        }
    }

    public Object fromLua2(String func, Object arg0, Object arg1) {
        switch (func) {
            case "dragCamera":
                float dx = ((Double)arg0).floatValue();
                float dy = ((Double)arg1).floatValue();
                IsoCamera.cameras[this.playerIndex].deferedX = -dx;
                IsoCamera.cameras[this.playerIndex].deferedY = -dy;
                return null;
            case "setObjectAtCursor":
                String dxx = (String)arg0;
                switch (dxx) {
                    case "id":
                        this.objectAtCursorId.setValue((String)arg1);
                        break;
                    case "levels":
                        this.objectAtCursorLevels.setValue(((Double)arg1).intValue());
                        break;
                    case "scale":
                        this.objectAtCursorScale.setValue((Double)arg1);
                        break;
                    case "width":
                        this.objectAtCursorWidth.setValue((Double)arg1);
                }

                return null;
            default:
                throw new IllegalArgumentException(String.format("unhandled \"%s\" \"%s\" \\\"%s\\\"", func, arg0, arg1));
        }
    }

    private void updateCursor() {
        int playerIndex = this.playerIndex;
        int SCL = Core.tileScale;
        float x = Mouse.getXA();
        float y = Mouse.getYA();
        x -= IsoCamera.getScreenLeft(playerIndex);
        y -= IsoCamera.getScreenTop(playerIndex);
        x *= Core.getInstance().getZoom(playerIndex);
        y *= Core.getInstance().getZoom(playerIndex);
        int z = PZMath.fastfloor((float)this.z);
        this.gridXf = IsoUtils.XToIso(x, y, z);
        this.gridYf = IsoUtils.YToIso(x, y, z);
        this.gridX = PZMath.fastfloor(this.gridXf);
        this.gridY = PZMath.fastfloor(this.gridYf);
    }

    private void DrawIsoLine(float x, float y, float x2, float y2, float r, float g, float b, float a, int thickness) {
        float z = this.z;
        float sx = IsoUtils.XToScreenExact(x, y, z, 0);
        float sy = IsoUtils.YToScreenExact(x, y, z, 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, z, 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, z, 0);
        int playerIndex = IsoCamera.frameState.playerIndex;
        PlayerCamera camera = IsoCamera.cameras[playerIndex];
        float zoom = camera.zoom;
        sx += camera.fixJigglyModelsX * zoom;
        sy += camera.fixJigglyModelsY * zoom;
        sx2 += camera.fixJigglyModelsX * zoom;
        sy2 += camera.fixJigglyModelsY * zoom;
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, a, thickness);
    }

    private void DrawIsoRect(float x, float y, float width, float height, float r, float g, float b, float a, int thickness) {
        this.DrawIsoLine(x, y, x + width, y, r, g, b, a, thickness);
        this.DrawIsoLine(x + width, y, x + width, y + height, r, g, b, a, thickness);
        this.DrawIsoLine(x + width, y + height, x, y + height, r, g, b, a, thickness);
        this.DrawIsoLine(x, y + height, x, y, r, g, b, a, thickness);
    }

    private void drawGrid() {
        int playerIndex = this.playerIndex;
        float topLeftX = IsoUtils.XToIso(-128.0F, -256.0F, this.z);
        float topRightY = IsoUtils.YToIso(Core.getInstance().getOffscreenWidth(playerIndex) + 128, -256.0F, this.z);
        float bottomRightX = IsoUtils.XToIso(
            Core.getInstance().getOffscreenWidth(playerIndex) + 128, Core.getInstance().getOffscreenHeight(playerIndex) + 256, this.z
        );
        float bottomLeftY = IsoUtils.YToIso(-128.0F, Core.getInstance().getOffscreenHeight(playerIndex) + 256, this.z);
        int minY = (int)topRightY;
        int maxY = (int)bottomLeftY;
        int minX = (int)topLeftX;
        int maxX = (int)bottomRightX;
        minX -= 2;
        minY -= 2;

        for (int y = minY; y <= maxY; y++) {
            if (PZMath.coordmodulo(y, 8) == 0) {
                this.DrawIsoLine(minX, y, maxX, y, 1.0F, 1.0F, 1.0F, 0.5F, 1);
            }
        }

        for (int x = minX; x <= maxX; x++) {
            if (PZMath.coordmodulo(x, 8) == 0) {
                this.DrawIsoLine(x, minY, x, maxY, 1.0F, 1.0F, 1.0F, 0.5F, 1);
            }
        }

        for (int yx = minY; yx <= maxY; yx++) {
            if (yx % 256 == 0) {
                this.DrawIsoLine(minX, yx, maxX, yx, 0.0F, 1.0F, 0.0F, 0.5F, 1);
            }
        }

        for (int xx = minX; xx <= maxX; xx++) {
            if (xx % 256 == 0) {
                this.DrawIsoLine(xx, minY, xx, maxY, 0.0F, 1.0F, 0.0F, 0.5F, 1);
            }
        }

        if (GameClient.client) {
            for (int yxx = minY; yxx <= maxY; yxx++) {
                if (yxx % 64 == 0) {
                    this.DrawIsoLine(minX, yxx, maxX, yxx, 1.0F, 0.0F, 0.0F, 0.5F, 1);
                }
            }

            for (int xxx = minX; xxx <= maxX; xxx++) {
                if (xxx % 64 == 0) {
                    this.DrawIsoLine(xxx, minY, xxx, maxY, 1.0F, 0.0F, 0.0F, 0.5F, 1);
                }
            }
        }
    }

    public void drawObjectAtCursor() {
        if (this.objectAtCursor.getValue()) {
            int stateIndex = SpriteRenderer.instance.getMainStateIndex();
            if (this.geometryDrawers[stateIndex] == null) {
                this.geometryDrawers[stateIndex] = new DebugChunkState.GeometryDrawer();
            }

            DebugChunkState.GeometryDrawer drawer = this.geometryDrawers[stateIndex];
            drawer.renderMain();
            SpriteRenderer.instance.drawGeneric(drawer);
        }
    }

    private void drawTestModels() {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.gridX, this.gridY, this.z);
        if (square != null) {
            ;
        }
    }

    private void drawCursor() {
        int playerIndex = this.playerIndex;
        int SCL = Core.tileScale;
        float z = this.z;
        int sx = (int)IsoUtils.XToScreenExact(this.gridX, this.gridY + 1, z, 0);
        int sy = (int)IsoUtils.YToScreenExact(this.gridX, this.gridY + 1, z, 0);
        SpriteRenderer.instance.renderPoly(sx, sy, sx + 32 * SCL, sy - 16 * SCL, sx + 64 * SCL, sy, sx + 32 * SCL, sy + 16 * SCL, 0.0F, 0.0F, 1.0F, 0.5F);
        if (PerformanceSettings.fboRenderChunk && this.depthValues.getValue()) {
            float playerX = IsoPlayer.players[playerIndex].getX();
            float playerY = IsoPlayer.players[playerIndex].getY();
            IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(
                PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), this.gridXf, this.gridYf, this.z
            );
            float characterDepth = results.depthStart - 0.001F;
            TextManager.instance.DrawString(FONT, sx, sy, Float.toString(characterDepth), 1.0, 1.0, 1.0, 1.0);
            float squareDepth = IsoSprite.calculateDepth(this.gridXf, this.gridYf, this.z);
            results = IsoDepthHelper.getChunkDepthData(
                PZMath.fastfloor(playerX / 8.0F),
                PZMath.fastfloor(playerY / 8.0F),
                PZMath.fastfloor(this.gridXf / 8.0F),
                PZMath.fastfloor(this.gridYf / 8.0F),
                this.z
            );
            float depth = results.depthStart;
            TextManager.instance
                .DrawString(
                    FONT,
                    sx,
                    sy + TextManager.instance.getFontHeight(FONT),
                    String.format("%.4f (%.4f + %.4f)", depth + squareDepth, depth, squareDepth),
                    1.0,
                    1.0,
                    1.0,
                    1.0
                );
        }

        IsoChunkMap cm = IsoWorld.instance.getCell().chunkMap[playerIndex];

        for (int y = cm.getWorldYMinTiles(); y < cm.getWorldYMaxTiles(); y++) {
            for (int x = cm.getWorldXMinTiles(); x < cm.getWorldXMaxTiles(); x++) {
                IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare((double)x, (double)y, (double)z);
                if (sq != null) {
                    if (sq != cm.getGridSquare(x, y, PZMath.fastfloor(z))) {
                        sx = (int)IsoUtils.XToScreenExact(x, y + 1, z, 0);
                        sy = (int)IsoUtils.YToScreenExact(x, y + 1, z, 0);
                        SpriteRenderer.instance.renderPoly(sx, sy, sx + 32, sy - 16, sx + 64, sy, sx + 32, sy + 16, 1.0F, 0.0F, 0.0F, 0.8F);
                    }

                    if (sq == null
                        || sq.getX() != x
                        || sq.getY() != y
                        || sq.getZ() != z
                        || sq.e != null && sq.e.w != null && sq.e.w != sq
                        || sq.w != null && sq.w.e != null && sq.w.e != sq
                        || sq.n != null && sq.n.s != null && sq.n.s != sq
                        || sq.s != null && sq.s.n != null && sq.s.n != sq
                        || sq.nw != null && sq.nw.se != null && sq.nw.se != sq
                        || sq.se != null && sq.se.nw != null && sq.se.nw != sq) {
                        sx = (int)IsoUtils.XToScreenExact(x, y + 1, z, 0);
                        sy = (int)IsoUtils.YToScreenExact(x, y + 1, z, 0);
                        SpriteRenderer.instance.renderPoly(sx, sy, sx + 32, sy - 16, sx + 64, sy, sx + 32, sy + 16, 1.0F, 0.0F, 0.0F, 0.5F);
                    }

                    if (sq != null) {
                        IsoGridSquare w = sq.testPathFindAdjacent(null, -1, 0, 0) ? null : sq.getAdjacentSquare(IsoDirections.W);
                        IsoGridSquare n = sq.testPathFindAdjacent(null, 0, -1, 0) ? null : sq.getAdjacentSquare(IsoDirections.N);
                        IsoGridSquare e = sq.testPathFindAdjacent(null, 1, 0, 0) ? null : sq.getAdjacentSquare(IsoDirections.E);
                        IsoGridSquare s = sq.testPathFindAdjacent(null, 0, 1, 0) ? null : sq.getAdjacentSquare(IsoDirections.S);
                        IsoGridSquare nw = sq.testPathFindAdjacent(null, -1, -1, 0) ? null : sq.getAdjacentSquare(IsoDirections.NW);
                        IsoGridSquare ne = sq.testPathFindAdjacent(null, 1, -1, 0) ? null : sq.getAdjacentSquare(IsoDirections.NE);
                        IsoGridSquare sw = sq.testPathFindAdjacent(null, -1, 1, 0) ? null : sq.getAdjacentSquare(IsoDirections.SW);
                        IsoGridSquare se = sq.testPathFindAdjacent(null, 1, 1, 0) ? null : sq.getAdjacentSquare(IsoDirections.SE);
                        if (w != sq.w || n != sq.n || e != sq.e || s != sq.s || nw != sq.nw || ne != sq.ne || sw != sq.sw || se != sq.se) {
                            this.paintSquare(x, y, PZMath.fastfloor(z), 1.0F, 0.0F, 0.0F, 0.5F);
                        }
                    }

                    if (sq != null
                        && (
                            sq.getAdjacentSquare(IsoDirections.NW) != null && sq.getAdjacentSquare(IsoDirections.NW).getAdjacentSquare(IsoDirections.SE) != sq
                                || sq.getAdjacentSquare(IsoDirections.NE) != null
                                    && sq.getAdjacentSquare(IsoDirections.NE).getAdjacentSquare(IsoDirections.SW) != sq
                                || sq.getAdjacentSquare(IsoDirections.SW) != null
                                    && sq.getAdjacentSquare(IsoDirections.SW).getAdjacentSquare(IsoDirections.NE) != sq
                                || sq.getAdjacentSquare(IsoDirections.SE) != null
                                    && sq.getAdjacentSquare(IsoDirections.SE).getAdjacentSquare(IsoDirections.NW) != sq
                                || sq.getAdjacentSquare(IsoDirections.N) != null
                                    && sq.getAdjacentSquare(IsoDirections.N).getAdjacentSquare(IsoDirections.S) != sq
                                || sq.getAdjacentSquare(IsoDirections.S) != null
                                    && sq.getAdjacentSquare(IsoDirections.S).getAdjacentSquare(IsoDirections.N) != sq
                                || sq.getAdjacentSquare(IsoDirections.W) != null
                                    && sq.getAdjacentSquare(IsoDirections.W).getAdjacentSquare(IsoDirections.E) != sq
                                || sq.getAdjacentSquare(IsoDirections.E) != null
                                    && sq.getAdjacentSquare(IsoDirections.E).getAdjacentSquare(IsoDirections.W) != sq
                        )) {
                        sx = (int)IsoUtils.XToScreenExact(x, y + 1, z, 0);
                        sy = (int)IsoUtils.YToScreenExact(x, y + 1, z, 0);
                        SpriteRenderer.instance.renderPoly(sx, sy, sx + 32, sy - 16, sx + 64, sy, sx + 32, sy + 16, 1.0F, 0.0F, 0.0F, 0.5F);
                    }

                    if (this.emptySquares.getValue() && sq.getObjects().isEmpty()) {
                        this.paintSquare(x, y, PZMath.fastfloor(z), 1.0F, 1.0F, 0.0F, 0.5F);
                    }

                    if (sq.getRoom() != null && sq.isFree(false) && !VirtualZombieManager.instance.canSpawnAt(x, y, PZMath.fastfloor(z))) {
                        this.paintSquare(x, y, PZMath.fastfloor(z), 1.0F, 1.0F, 1.0F, 1.0F);
                    }

                    if (this.roofHideBuilding.getValue() && sq.roofHideBuilding != null) {
                        this.paintSquare(x, y, PZMath.fastfloor(z), 0.0F, 0.0F, 1.0F, 0.25F);
                    }

                    if (this.occludedSquares.getValue() && FBORenderOcclusion.getInstance().isOccluded(sq.x, sq.y, sq.z)) {
                        this.paintSquare(x, y, sq.z, 1.0F, 1.0F, 1.0F, 0.5F);
                    }
                }
            }
        }

        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        if (!PerformanceSettings.fboRenderChunk
            && isoGameCharacter.getCurrentSquare() != null
            && Math.abs(this.gridX - PZMath.fastfloor(isoGameCharacter.getX())) <= 1
            && Math.abs(this.gridY - PZMath.fastfloor(isoGameCharacter.getY())) <= 1) {
            IsoGridSquare next = IsoWorld.instance.currentCell.getGridSquare(this.gridX, this.gridY, this.z);
            IsoObject thump = isoGameCharacter.getCurrentSquare().testCollideSpecialObjects(next);
            if (thump != null) {
                thump.getSprite().RenderGhostTileRed(PZMath.fastfloor(thump.getX()), PZMath.fastfloor(thump.getY()), PZMath.fastfloor(thump.getZ()));
            }
        }

        if (this.lineClearCollide.getValue()) {
            this.lineClearCached(
                IsoWorld.instance.currentCell,
                this.gridX,
                this.gridY,
                PZMath.fastfloor(z),
                PZMath.fastfloor(isoGameCharacter.getX()),
                PZMath.fastfloor(isoGameCharacter.getY()),
                this.z,
                false
            );
        }

        if (this.nearestWalls.getValue()) {
            NearestWalls.render(this.gridX, this.gridY, this.z, false);
        }

        if (this.nearestExteriorWalls.getValue()) {
            NearestWalls.render(this.gridX, this.gridY, this.z, true);
        }

        if (this.vehicleStory.getValue()) {
            this.drawVehicleStory();
        }
    }

    private void drawZones() {
        ArrayList<Zone> zones = IsoWorld.instance.metaGrid.getZonesAt(this.gridX, this.gridY, this.z, new ArrayList<>());
        Zone topZone = null;

        for (int i = 0; i < zones.size(); i++) {
            Zone zone = zones.get(i);
            if (zone.isPreferredZoneForSquare) {
                topZone = zone;
            }

            if (!zone.isPolyline()) {
                if (!zone.points.isEmpty()) {
                    for (int j = 0; j < zone.points.size(); j += 2) {
                        int x1 = zone.points.get(j);
                        int y1 = zone.points.get(j + 1);
                        int x2 = zone.points.get((j + 2) % zone.points.size());
                        int y2 = zone.points.get((j + 3) % zone.points.size());
                        this.DrawIsoLine(x1, y1, x2, y2, 1.0F, 1.0F, 0.0F, 1.0F, 1);
                    }
                } else {
                    this.DrawIsoLine(zone.x, zone.y, zone.x + zone.w, zone.y, 1.0F, 1.0F, 0.0F, 1.0F, 1);
                    this.DrawIsoLine(zone.x, zone.y + zone.h, zone.x + zone.w, zone.y + zone.h, 1.0F, 1.0F, 0.0F, 1.0F, 1);
                    this.DrawIsoLine(zone.x, zone.y, zone.x, zone.y + zone.h, 1.0F, 1.0F, 0.0F, 1.0F, 1);
                    this.DrawIsoLine(zone.x + zone.w, zone.y, zone.x + zone.w, zone.y + zone.h, 1.0F, 1.0F, 0.0F, 1.0F, 1);
                }
            }
        }

        zones = IsoWorld.instance.metaGrid.getZonesIntersecting(this.gridX - 1, this.gridY - 1, this.z, 3, 3, new ArrayList<>());
        LiangBarsky LB = new LiangBarsky();
        double[] t1t2 = new double[2];
        IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(this.gridX, this.gridY, this.z);

        for (int i = 0; i < zones.size(); i++) {
            Zone zonex = zones.get(i);
            if (zonex != null && zonex.isPolyline() && !zonex.points.isEmpty()) {
                for (int j = 0; j < zonex.points.size() - 2; j += 2) {
                    int x1 = zonex.points.get(j);
                    int y1 = zonex.points.get(j + 1);
                    int x2 = zonex.points.get(j + 2);
                    int y2 = zonex.points.get(j + 3);
                    this.DrawIsoLine(x1, y1, x2, y2, 1.0F, 1.0F, 0.0F, 1.0F, 1);
                    float dx = x2 - x1;
                    float dy = y2 - y1;
                    if (chunk != null && LB.lineRectIntersect(x1, y1, dx, dy, chunk.wx * 10, chunk.wy * 10, chunk.wx * 10 + 10, chunk.wy * 10 + 10, t1t2)) {
                        this.DrawIsoLine(
                            x1 + (float)t1t2[0] * dx, y1 + (float)t1t2[0] * dy, x1 + (float)t1t2[1] * dx, y1 + (float)t1t2[1] * dy, 0.0F, 1.0F, 0.0F, 1.0F, 1
                        );
                    }
                }

                if (zonex.polylineOutlinePoints != null) {
                    float[] points = zonex.polylineOutlinePoints;

                    for (int jx = 0; jx < points.length; jx += 2) {
                        float x1 = points[jx];
                        float y1 = points[jx + 1];
                        float x2 = points[(jx + 2) % points.length];
                        float y2 = points[(jx + 3) % points.length];
                        this.DrawIsoLine(x1, y1, x2, y2, 1.0F, 1.0F, 0.0F, 1.0F, 1);
                    }
                }
            }
        }

        VehicleZone vehicleZone = IsoWorld.instance.metaGrid.getVehicleZoneAt(this.gridX, this.gridY, this.z);
        if (vehicleZone != null) {
            float r = 0.5F;
            float g = 1.0F;
            float b = 0.5F;
            float a = 1.0F;
            if (vehicleZone.isPolygon()) {
                for (int jx = 0; jx < vehicleZone.points.size(); jx += 2) {
                    int x1 = vehicleZone.points.get(jx);
                    int y1 = vehicleZone.points.get(jx + 1);
                    int x2 = vehicleZone.points.get((jx + 2) % vehicleZone.points.size());
                    int y2 = vehicleZone.points.get((jx + 3) % vehicleZone.points.size());
                    this.DrawIsoLine(x1, y1, x2, y2, 1.0F, 1.0F, 0.0F, 1.0F, 1);
                }
            } else if (vehicleZone.isPolyline()) {
                for (int jx = 0; jx < vehicleZone.points.size() - 2; jx += 2) {
                    int x1 = vehicleZone.points.get(jx);
                    int y1 = vehicleZone.points.get(jx + 1);
                    int x2 = vehicleZone.points.get(jx + 2);
                    int y2 = vehicleZone.points.get(jx + 3);
                    this.DrawIsoLine(x1, y1, x2, y2, 1.0F, 1.0F, 0.0F, 1.0F, 1);
                }

                if (vehicleZone.polylineOutlinePoints != null) {
                    float[] points = vehicleZone.polylineOutlinePoints;

                    for (int jx = 0; jx < points.length; jx += 2) {
                        float x1 = points[jx];
                        float y1 = points[jx + 1];
                        float x2 = points[(jx + 2) % points.length];
                        float y2 = points[(jx + 3) % points.length];
                        this.DrawIsoLine(x1, y1, x2, y2, 1.0F, 1.0F, 0.0F, 1.0F, 1);
                    }
                }
            } else {
                this.DrawIsoLine(vehicleZone.x, vehicleZone.y, vehicleZone.x + vehicleZone.w, vehicleZone.y, 0.5F, 1.0F, 0.5F, 1.0F, 1);
                this.DrawIsoLine(
                    vehicleZone.x, vehicleZone.y + vehicleZone.h, vehicleZone.x + vehicleZone.w, vehicleZone.y + vehicleZone.h, 0.5F, 1.0F, 0.5F, 1.0F, 1
                );
                this.DrawIsoLine(vehicleZone.x, vehicleZone.y, vehicleZone.x, vehicleZone.y + vehicleZone.h, 0.5F, 1.0F, 0.5F, 1.0F, 1);
                this.DrawIsoLine(
                    vehicleZone.x + vehicleZone.w, vehicleZone.y, vehicleZone.x + vehicleZone.w, vehicleZone.y + vehicleZone.h, 0.5F, 1.0F, 0.5F, 1.0F, 1
                );
            }
        }

        if (this.randomSquareInZone.getValue() && topZone != null) {
            IsoGridSquare square = topZone.getRandomSquareInZone();
            if (square != null) {
                this.paintSquare(square.x, square.y, square.z, 0.0F, 1.0F, 0.0F, 0.5F);
            }
        }
    }

    private void drawVehicleStory() {
        ArrayList<Zone> zones = IsoWorld.instance.metaGrid.getZonesIntersecting(this.gridX - 1, this.gridY - 1, this.z, 3, 3, new ArrayList<>());
        if (!zones.isEmpty()) {
            IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(this.gridX, this.gridY, this.z);
            if (chunk != null) {
                for (int i = 0; i < zones.size(); i++) {
                    Zone zone = zones.get(i);
                    if ("Nav".equals(zone.type)) {
                        VehicleStorySpawner spawner = VehicleStorySpawner.getInstance();
                        RandomizedVehicleStoryBase story = IsoWorld.instance.getRandomizedVehicleStoryByName(this.vehicleStoryName);
                        if (story != null && story.isValid(zone, chunk, true) && story.initVehicleStorySpawner(zone, chunk, true)) {
                            int spawnW = story.getMinZoneWidth();
                            int spawnH = story.getMinZoneHeight();
                            float[] xyd = new float[3];
                            if (story.getSpawnPoint(zone, chunk, xyd)) {
                                float spawnX = xyd[0];
                                float spawnY = xyd[1];
                                float angle = xyd[2] + (float) (Math.PI / 2);
                                spawner.spawn(spawnX, spawnY, 0.0F, angle, (spawner2, element) -> {});
                                spawner.render(spawnX, spawnY, 0.0F, spawnW, spawnH, xyd[2]);
                            }
                        }
                    }
                }
            }
        }
    }

    private void DrawBehindStuff() {
        this.IsBehindStuff(IsoCamera.getCameraCharacter().getCurrentSquare());
    }

    private boolean IsBehindStuff(IsoGridSquare sq) {
        for (int z = 1; z < 8 && sq.getZ() + z < 8; z++) {
            for (int y = -5; y <= 6; y++) {
                for (int x = -5; x <= 6; x++) {
                    if (x >= y - 5 && x <= y + 5) {
                        this.paintSquare(sq.getX() + x + z * 3, sq.getY() + y + z * 3, sq.getZ() + z, 1.0F, 1.0F, 0.0F, 0.25F);
                    }
                }
            }
        }

        return true;
    }

    private boolean IsBehindStuffRecY(int x, int y, int z) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (z >= 15) {
            return false;
        } else {
            this.paintSquare(x, y, z, 1.0F, 1.0F, 0.0F, 0.25F);
            return this.IsBehindStuffRecY(x, y + 1, z + 1);
        }
    }

    private boolean IsBehindStuffRecXY(int x, int y, int z, int n) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (z >= 15) {
            return false;
        } else {
            this.paintSquare(x, y, z, 1.0F, 1.0F, 0.0F, 0.25F);
            return this.IsBehindStuffRecXY(x + n, y + n, z + 1, n);
        }
    }

    private boolean IsBehindStuffRecX(int x, int y, int z) {
        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (z >= 15) {
            return false;
        } else {
            this.paintSquare(x, y, z, 1.0F, 1.0F, 0.0F, 0.25F);
            return this.IsBehindStuffRecX(x + 1, y, z + 1);
        }
    }

    private void paintSquare(int x, int y, int z, float r, float g, float b, float a) {
        int SCL = Core.tileScale;
        int sx = (int)IsoUtils.XToScreenExact(x, y + 1, z, 0);
        int sy = (int)IsoUtils.YToScreenExact(x, y + 1, z, 0);
        SpriteRenderer.instance.renderPoly(sx, sy, sx + 32 * SCL, sy - 16 * SCL, sx + 64 * SCL, sy, sx + 32 * SCL, sy + 16 * SCL, r, g, b, a);
    }

    private void drawModData() {
        int z = this.z;
        IsoGridSquare sq = IsoWorld.instance.getCell().getGridSquare(this.gridX, this.gridY, z);
        int x = Core.getInstance().getScreenWidth() - 250;
        int y = 10;
        int lineHgt = TextManager.instance.getFontFromEnum(FONT).getLineHeight();
        if (sq != null && sq.getModData() != null) {
            KahluaTable table = sq.getModData();
            int var12;
            this.DrawString(x, var12 = y + lineHgt, "MOD DATA x,y,z=" + sq.getX() + "," + sq.getY() + "," + sq.getZ());
            KahluaTableIterator it = table.iterator();

            while (it.advance()) {
                this.DrawString(x, var12 += lineHgt, it.getKey().toString() + " = " + it.getValue().toString());
                if (it.getValue() instanceof KahluaTable) {
                    KahluaTableIterator it2 = ((KahluaTable)it.getValue()).iterator();

                    while (it2.advance()) {
                        this.DrawString(x + 8, var12 += lineHgt, it2.getKey().toString() + " = " + it2.getValue().toString());
                    }
                }
            }

            y = var12 + lineHgt;
        }

        if (sq != null) {
            PropertyContainer pc = sq.getProperties();
            ArrayList<String> keys = pc.getPropertyNames();
            if (!keys.isEmpty()) {
                this.DrawString(x, y += lineHgt, "PROPERTIES x,y,z=" + sq.getX() + "," + sq.getY() + "," + sq.getZ());
                Collections.sort(keys);

                for (String key : keys) {
                    this.DrawString(x, y += lineHgt, key + " = \"" + pc.get(key) + "\"");
                }
            }

            for (IsoFlagType f : IsoFlagType.values()) {
                if (pc.has(f)) {
                    this.DrawString(x, y += lineHgt, f.toString());
                }
            }
        }

        if (sq != null) {
            ErosionData.Square sqErosionData = sq.getErosionData();
            if (sqErosionData != null) {
                y += lineHgt;
                int var14;
                this.DrawString(x, var14 = y + lineHgt, "EROSION x,y,z=" + sq.getX() + "," + sq.getY() + "," + sq.getZ());
                this.DrawString(x, y = var14 + lineHgt, "init=" + sqErosionData.init);
                int var16;
                this.DrawString(x, var16 = y + lineHgt, "doNothing=" + sqErosionData.doNothing);
                this.DrawString(x, y = var16 + lineHgt, "chunk.init=" + sq.chunk.getErosionData().init);
            }
        }
    }

    private void drawPlayerInfo() {
        int x = Core.getInstance().getScreenWidth() - 250;
        int y = Core.getInstance().getScreenHeight() / 2;
        int lineHgt = TextManager.instance.getFontFromEnum(FONT).getLineHeight();
        IsoGameCharacter isoGameCharacter = IsoCamera.getCameraCharacter();
        int var5;
        this.DrawString(x, var5 = y + lineHgt, CharacterStat.BOREDOM.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.BOREDOM));
        this.DrawString(x, y = var5 + lineHgt, CharacterStat.ENDURANCE.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.ENDURANCE));
        int var7;
        this.DrawString(x, var7 = y + lineHgt, CharacterStat.FATIGUE.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.FATIGUE));
        this.DrawString(x, y = var7 + lineHgt, CharacterStat.HUNGER.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.HUNGER));
        int var9;
        this.DrawString(x, var9 = y + lineHgt, CharacterStat.PAIN.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.PAIN));
        this.DrawString(x, y = var9 + lineHgt, CharacterStat.PANIC.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.PANIC));
        int var11;
        this.DrawString(x, var11 = y + lineHgt, CharacterStat.STRESS.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.STRESS));
        this.DrawString(x, y = var11 + lineHgt, "clothingTemp = " + ((IsoPlayer)isoGameCharacter).getPlayerClothingTemperature());
        int var13;
        this.DrawString(x, var13 = y + lineHgt, CharacterStat.TEMPERATURE.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.TEMPERATURE));
        this.DrawString(x, y = var13 + lineHgt, CharacterStat.THIRST.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.THIRST));
        int var15;
        this.DrawString(x, var15 = y + lineHgt, CharacterStat.FOOD_SICKNESS.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.FOOD_SICKNESS));
        this.DrawString(x, y = var15 + lineHgt, CharacterStat.POISON.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.POISON));
        int var17;
        this.DrawString(x, var17 = y + lineHgt, CharacterStat.UNHAPPINESS.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.UNHAPPINESS));
        this.DrawString(x, y = var17 + lineHgt, "infected = " + isoGameCharacter.getBodyDamage().isInfected());
        int var19;
        this.DrawString(
            x, var19 = y + lineHgt, CharacterStat.ZOMBIE_INFECTION.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.ZOMBIE_INFECTION)
        );
        this.DrawString(x, y = var19 + lineHgt, CharacterStat.ZOMBIE_FEVER.getId() + " = " + isoGameCharacter.getStats().get(CharacterStat.ZOMBIE_FEVER));
        y += lineHgt;
        int var22;
        this.DrawString(x, var22 = y + lineHgt, "WORLD");
        this.DrawString(x, y = var22 + lineHgt, "globalTemperature = " + IsoWorld.instance.getGlobalTemperature());
    }

    public LosUtil.TestResults lineClearCached(IsoCell cell, int x1, int y1, int z1, int x0, int y0, int z0, boolean bIgnoreDoors) {
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        int cx = dx + 100;
        int cy = dy + 100;
        int cz = dz + 16;
        if (cx >= 0 && cy >= 0 && cz >= 0 && cx < 200 && cy < 200) {
            LosUtil.TestResults res = LosUtil.TestResults.Clear;
            int resultToPropagate = 1;
            float t = 0.5F;
            float t2 = 0.5F;
            IsoGridSquare b = cell.getGridSquare(x0, y0, z0);
            if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Math.abs(dz)) {
                float m = (float)dy / dx;
                float m2 = (float)dz / dx;
                t += y0;
                t2 += z0;
                dx = dx < 0 ? -1 : 1;
                m *= dx;
                m2 *= dx;

                while (x0 != x1) {
                    x0 += dx;
                    t += m;
                    t2 += m2;
                    IsoGridSquare a = cell.getGridSquare(x0, PZMath.fastfloor(t), PZMath.fastfloor(t2));
                    this.paintSquare(x0, PZMath.fastfloor(t), PZMath.fastfloor(t2), 1.0F, 1.0F, 1.0F, 0.5F);
                    if (a != null
                        && b != null
                        && a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors)
                            == LosUtil.TestResults.Blocked) {
                        this.paintSquare(x0, PZMath.fastfloor(t), PZMath.fastfloor(t2), 1.0F, 0.0F, 0.0F, 0.5F);
                        this.paintSquare(b.getX(), b.getY(), b.getZ(), 1.0F, 0.0F, 0.0F, 0.5F);
                        resultToPropagate = 4;
                    }

                    b = a;
                    int var39 = (int)t;
                    int var40 = (int)t2;
                }
            } else if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
                float m = (float)dx / dy;
                float m2 = (float)dz / dy;
                t += x0;
                t2 += z0;
                dy = dy < 0 ? -1 : 1;
                m *= dy;
                m2 *= dy;

                while (y0 != y1) {
                    y0 += dy;
                    t += m;
                    t2 += m2;
                    IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t), y0, PZMath.fastfloor(t2));
                    this.paintSquare(PZMath.fastfloor(t), y0, PZMath.fastfloor(t2), 1.0F, 1.0F, 1.0F, 0.5F);
                    if (a != null
                        && b != null
                        && a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors)
                            == LosUtil.TestResults.Blocked) {
                        this.paintSquare(PZMath.fastfloor(t), y0, PZMath.fastfloor(t2), 1.0F, 0.0F, 0.0F, 0.5F);
                        this.paintSquare(b.getX(), b.getY(), b.getZ(), 1.0F, 0.0F, 0.0F, 0.5F);
                        resultToPropagate = 4;
                    }

                    b = a;
                    int var38 = (int)t;
                    int lz = (int)t2;
                }
            } else {
                float m = (float)dx / dz;
                float m2 = (float)dy / dz;
                t += x0;
                t2 += y0;
                dz = dz < 0 ? -1 : 1;
                m *= dz;
                m2 *= dz;

                while (z0 != z1) {
                    z0 += dz;
                    t += m;
                    t2 += m2;
                    IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t), PZMath.fastfloor(t2), z0);
                    this.paintSquare(PZMath.fastfloor(t), PZMath.fastfloor(t2), z0, 1.0F, 1.0F, 1.0F, 0.5F);
                    if (a != null
                        && b != null
                        && a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors)
                            == LosUtil.TestResults.Blocked) {
                        resultToPropagate = 4;
                    }

                    b = a;
                    int lx = (int)t;
                    int ly = (int)t2;
                }
            }

            if (resultToPropagate == 1) {
                return LosUtil.TestResults.Clear;
            } else if (resultToPropagate == 2) {
                return LosUtil.TestResults.ClearThroughOpenDoor;
            } else if (resultToPropagate == 3) {
                return LosUtil.TestResults.ClearThroughWindow;
            } else {
                return resultToPropagate == 4 ? LosUtil.TestResults.Blocked : LosUtil.TestResults.Blocked;
            }
        } else {
            return LosUtil.TestResults.Blocked;
        }
    }

    private void DrawString(int x, int y, String text) {
        int width = TextManager.instance.MeasureStringX(FONT, text);
        int height = TextManager.instance.getFontFromEnum(FONT).getLineHeight();
        SpriteRenderer.instance.renderi(null, x - 1, y, width + 2, height, 0.0F, 0.0F, 0.0F, 0.8F, null);
        TextManager.instance.DrawString(FONT, x, y, text, 1.0, 1.0, 1.0, 1.0);
    }

    public float getObjectAtCursorScale() {
        return !this.objectAtCursorId.getValue().equalsIgnoreCase("player") ? 1.0F : (float)this.objectAtCursorScale.getValue();
    }

    private void registerOption(ConfigOption option) {
        String var2 = option.getName();
        switch (var2) {
            case "ObjectAtCursor.id":
            case "ObjectAtCursor.levels":
            case "ObjectAtCursor.scale":
            case "ObjectAtCursor.width":
                this.optionsHidden.add(option);
                break;
            default:
                this.options.add(option);
        }
    }

    public ConfigOption getOptionByName(String name) {
        for (int i = 0; i < this.options.size(); i++) {
            ConfigOption setting = this.options.get(i);
            if (setting.getName().equals(name)) {
                return setting;
            }
        }

        return null;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public void setBoolean(String name, boolean value) {
        if (this.getOptionByName(name) instanceof BooleanConfigOption booleanConfigOption) {
            booleanConfigOption.setValue(value);
        }
    }

    public boolean getBoolean(String name) {
        return this.getOptionByName(name) instanceof BooleanConfigOption booleanConfigOption ? booleanConfigOption.getValue() : false;
    }

    public void save() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "debugChunkState-options.ini";
        ConfigFile configFile = new ConfigFile();
        ArrayList<ConfigOption> options1 = new ArrayList<>(this.options);
        options1.addAll(this.optionsHidden);
        configFile.write(fileName, 1, options1);
        options1.clear();
    }

    public void load() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "debugChunkState-options.ini";
        ConfigFile configFile = new ConfigFile();
        if (configFile.read(fileName)) {
            for (int i = 0; i < configFile.getOptions().size(); i++) {
                ConfigOption configOption = configFile.getOptions().get(i);
                ConfigOption myOption = this.getOptionByName(configOption.getName());
                if (myOption == null) {
                    for (int j = 0; j < this.optionsHidden.size(); j++) {
                        ConfigOption myOption1 = this.optionsHidden.get(j);
                        if (myOption1.getName().equals(configOption.getName())) {
                            myOption = myOption1;
                            break;
                        }
                    }
                }

                if (myOption != null) {
                    myOption.parse(configOption.getValueAsString());
                }
            }
        }
    }

    @UsedFromLua
    public class BooleanDebugOption extends BooleanConfigOption {
        public BooleanDebugOption(final String name, final boolean defaultValue) {
            Objects.requireNonNull(DebugChunkState.this);
            super(name, defaultValue);
            DebugChunkState.this.registerOption(this);
        }
    }

    public class DoubleDebugOption extends DoubleConfigOption {
        public DoubleDebugOption(final String name, final double min, final double max, final double defaultValue) {
            Objects.requireNonNull(DebugChunkState.this);
            super(name, min, max, defaultValue);
            DebugChunkState.this.registerOption(this);
        }
    }

    private class FloodFill {
        private IsoGridSquare start;
        private static final int FLOOD_SIZE = 11;
        private final BooleanGrid visited;
        private final Stack<IsoGridSquare> stack;
        private IsoBuilding building;
        private Mover mover;

        private FloodFill() {
            Objects.requireNonNull(DebugChunkState.this);
            super();
            this.visited = new BooleanGrid(11, 11);
            this.stack = new Stack<>();
        }

        private void calculate(Mover mv, IsoGridSquare sq) {
            this.start = sq;
            this.mover = mv;
            if (this.start.getRoom() != null) {
                this.building = this.start.getRoom().getBuilding();
            }

            boolean spanLeft = false;
            boolean spanRight = false;
            if (this.push(this.start.getX(), this.start.getY())) {
                while ((sq = this.pop()) != null) {
                    int x = sq.getX();
                    int y1 = sq.getY();

                    while (this.shouldVisit(x, y1, x, y1 - 1)) {
                        y1--;
                    }

                    spanRight = false;
                    spanLeft = false;

                    do {
                        this.visited.setValue(this.gridX(x), this.gridY(y1), true);
                        if (!spanLeft && this.shouldVisit(x, y1, x - 1, y1)) {
                            if (!this.push(x - 1, y1)) {
                                return;
                            }

                            spanLeft = true;
                        } else if (spanLeft && !this.shouldVisit(x, y1, x - 1, y1)) {
                            spanLeft = false;
                        } else if (spanLeft && !this.shouldVisit(x - 1, y1, x - 1, y1 - 1) && !this.push(x - 1, y1)) {
                            return;
                        }

                        if (!spanRight && this.shouldVisit(x, y1, x + 1, y1)) {
                            if (!this.push(x + 1, y1)) {
                                return;
                            }

                            spanRight = true;
                        } else if (spanRight && !this.shouldVisit(x, y1, x + 1, y1)) {
                            spanRight = false;
                        } else if (spanRight && !this.shouldVisit(x + 1, y1, x + 1, y1 - 1) && !this.push(x + 1, y1)) {
                            return;
                        }

                        y1++;
                    } while (this.shouldVisit(x, y1 - 1, x, y1));
                }
            }
        }

        private boolean shouldVisit(int x1, int y1, int x2, int y2) {
            if (this.gridX(x2) < 11 && this.gridX(x2) >= 0) {
                if (this.gridY(y2) < 11 && this.gridY(y2) >= 0) {
                    if (this.visited.getValue(this.gridX(x2), this.gridY(y2))) {
                        return false;
                    } else {
                        IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x2, y2, this.start.getZ());
                        if (sq == null) {
                            return false;
                        } else if (sq.has(IsoObjectType.stairsBN) || sq.has(IsoObjectType.stairsMN) || sq.has(IsoObjectType.stairsTN)) {
                            return false;
                        } else if (sq.has(IsoObjectType.stairsBW) || sq.has(IsoObjectType.stairsMW) || sq.has(IsoObjectType.stairsTW)) {
                            return false;
                        } else if (sq.getRoom() != null && this.building == null) {
                            return false;
                        } else {
                            return sq.getRoom() == null && this.building != null
                                ? false
                                : !IsoWorld.instance.currentCell.blocked(this.mover, x2, y2, this.start.getZ(), x1, y1, this.start.getZ());
                        }
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        private boolean push(int x, int y) {
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(x, y, this.start.getZ());
            this.stack.push(sq);
            return true;
        }

        private IsoGridSquare pop() {
            return this.stack.isEmpty() ? null : this.stack.pop();
        }

        private int gridX(int x) {
            return x - (this.start.getX() - 5);
        }

        private int gridY(int y) {
            return y - (this.start.getY() - 5);
        }

        private int gridX(IsoGridSquare sq) {
            return sq.getX() - (this.start.getX() - 5);
        }

        private int gridY(IsoGridSquare sq) {
            return sq.getY() - (this.start.getY() - 5);
        }

        private void draw() {
            int minX = this.start.getX() - 5;
            int minY = this.start.getY() - 5;

            for (int y = 0; y < 11; y++) {
                for (int x = 0; x < 11; x++) {
                    if (this.visited.getValue(x, y)) {
                        int sx = (int)IsoUtils.XToScreenExact(minX + x, minY + y + 1, this.start.getZ(), 0);
                        int sy = (int)IsoUtils.YToScreenExact(minX + x, minY + y + 1, this.start.getZ(), 0);
                        SpriteRenderer.instance.renderPoly(sx, sy, sx + 32, sy - 16, sx + 64, sy, sx + 32, sy + 16, 1.0F, 1.0F, 0.0F, 0.5F);
                    }
                }
            }
        }
    }

    private static final class GeometryDrawer extends TextureDraw.GenericDrawer {
        String object;
        int levels;
        float scale;
        float width;
        float playerX;
        float playerY;
        float x;
        float y;
        float z;

        public void renderMain() {
            this.object = DebugChunkState.instance.objectAtCursorId.getValue();
            this.levels = DebugChunkState.instance.objectAtCursorLevels.getValue();
            this.scale = (float)DebugChunkState.instance.objectAtCursorScale.getValue();
            this.width = (float)DebugChunkState.instance.objectAtCursorWidth.getValue();
            this.playerX = IsoPlayer.players[DebugChunkState.instance.playerIndex].getX();
            this.playerY = IsoPlayer.players[DebugChunkState.instance.playerIndex].getY();
            this.x = DebugChunkState.instance.gridXf;
            this.y = DebugChunkState.instance.gridYf;
            this.z = DebugChunkState.instance.z;
        }

        @Override
        public void render() {
            Shader shader = ShaderManager.instance.getOrCreateShader("debug_chunk_state_geometry", false, false);
            if (shader.getShaderProgram() != null && shader.getShaderProgram().isCompiled()) {
                boolean bGeometryOriginAtCenter = "box".equalsIgnoreCase(this.object);
                Core.getInstance().DoPushIsoStuff(this.x, this.y, this.z + (bGeometryOriginAtCenter ? this.levels / 2.0F : 0.0F), 0.0F, false);
                VBORenderer vbor = VBORenderer.getInstance();
                vbor.setDepthTestForAllRuns(Boolean.TRUE);
                float HEIGHT = 2.44949F * this.levels;
                Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
                MODELVIEW.identity();
                if ("box".equalsIgnoreCase(this.object)) {
                }

                if ("cylinder".equalsIgnoreCase(this.object)) {
                    MODELVIEW.rotateXYZ((float) (Math.PI * 3.0 / 2.0), 0.0F, (float)(IngameState.instance.numberTicks % 360L) * (float) (Math.PI / 180.0));
                }

                if ("plane".equalsIgnoreCase(this.object)) {
                    MODELVIEW.rotateXYZ((float) (Math.PI / 2), 0.0F, 0.0F);
                }

                MODELVIEW.scale(0.6666667F);
                Core.getInstance().modelViewMatrixStack.peek().mul(MODELVIEW, MODELVIEW);
                Core.getInstance().modelViewMatrixStack.push(MODELVIEW);
                float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0F, 0.0F, 0.0F);
                IsoDepthHelper.Results results = IsoDepthHelper.getSquareDepthData(
                    PZMath.fastfloor(this.playerX),
                    PZMath.fastfloor(this.playerY),
                    this.x,
                    this.y,
                    this.z + (bGeometryOriginAtCenter ? this.levels / 2.0F : 0.0F)
                );
                float targetDepth = results.depthStart - (depthBufferValue + 1.0F) / 2.0F;
                vbor.setUserDepthForAllRuns(targetDepth);
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDepthFunc(515);
                GL11.glDisable(2884);
                if ("box".equalsIgnoreCase(this.object)) {
                    float r = 1.0F;
                    float g = 1.0F;
                    float b = 1.0F;
                    float a = 1.0F;
                    vbor.addBox(this.width, HEIGHT, this.width, 1.0F, 1.0F, 1.0F, 1.0F, shader.getShaderProgram());
                }

                if ("cylinder".equalsIgnoreCase(this.object)) {
                    float THICKNESS = this.width;
                    float r = 1.0F;
                    float g = 1.0F;
                    float b = 1.0F;
                    float a = 1.0F;
                    vbor.addCylinder_Fill(THICKNESS / 2.0F, THICKNESS / 2.0F, HEIGHT, 48, 1, 1.0F, 1.0F, 1.0F, 1.0F, shader.getShaderProgram());
                }

                if ("plane".equalsIgnoreCase(this.object)) {
                    float z = 0.0F;
                    float r = 1.0F;
                    float g = 1.0F;
                    float b = 1.0F;
                    float a = 1.0F;
                    vbor.startRun(vbor.formatPositionColor);
                    vbor.setMode(7);
                    vbor.addQuad(-this.width / 2.0F, -this.width / 2.0F, this.width / 2.0F, this.width / 2.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                    vbor.endRun();
                }

                vbor.flush();
                vbor.setDepthTestForAllRuns(null);
                vbor.setUserDepthForAllRuns(null);
                Core.getInstance().modelViewMatrixStack.pop();
                Core.getInstance().DoPopIsoStuff();
                GLStateRenderThread.restore();
            }
        }
    }

    public class IntegerDebugOption extends IntegerConfigOption {
        public IntegerDebugOption(final String name, final int min, final int max, final int defaultValue) {
            Objects.requireNonNull(DebugChunkState.this);
            super(name, min, max, defaultValue);
            DebugChunkState.this.registerOption(this);
        }
    }

    public class StringDebugOption extends StringConfigOption {
        public StringDebugOption(final String name, final String defaultValue) {
            Objects.requireNonNull(DebugChunkState.this);
            super(name, defaultValue, -1);
            DebugChunkState.this.registerOption(this);
        }
    }
}
