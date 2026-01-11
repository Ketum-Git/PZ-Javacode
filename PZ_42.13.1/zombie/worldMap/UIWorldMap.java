// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.util.ArrayList;
import java.util.HashMap;
import se.krka.kahlua.vm.KahluaTable;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.Faction;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.AnimalZones;
import zombie.characters.animals.pathfind.NestedPathWanderer;
import zombie.characters.animals.pathfind.NestedPaths;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.debug.DebugOptions;
import zombie.inventory.types.MapItem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.areas.SafeHouse;
import zombie.iso.worldgen.WorldGenDebug;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.ServerOptions;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.util.StringUtils;
import zombie.worldMap.editor.WorldMapEditorState;
import zombie.worldMap.markers.WorldMapGridSquareMarker;
import zombie.worldMap.markers.WorldMapMarkers;
import zombie.worldMap.markers.WorldMapMarkersV1;
import zombie.worldMap.network.WorldMapClient;
import zombie.worldMap.streets.WorldMapStreet;
import zombie.worldMap.streets.WorldMapStreetsV1;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.styles.WorldMapStyleLayer;
import zombie.worldMap.styles.WorldMapStyleV1;
import zombie.worldMap.styles.WorldMapStyleV2;
import zombie.worldMap.styles.WorldMapTextStyleLayer;
import zombie.worldMap.symbols.DoublePoint;
import zombie.worldMap.symbols.DoublePointPool;
import zombie.worldMap.symbols.MapSymbolDefinitions;
import zombie.worldMap.symbols.SymbolsLayoutData;
import zombie.worldMap.symbols.TextLayout;
import zombie.worldMap.symbols.WorldMapSymbols;
import zombie.worldMap.symbols.WorldMapSymbolsV1;
import zombie.worldMap.symbols.WorldMapSymbolsV2;
import zombie.worldMap.symbols.WorldMapTextSymbol;

@UsedFromLua
public class UIWorldMap extends UIElement {
    static final ArrayList<WorldMapFeature> s_tempFeatures = new ArrayList<>();
    static final TextLayout s_textLayout = new TextLayout();
    protected final WorldMap worldMap = new WorldMap();
    protected final WorldMapStyle style = new WorldMapStyle();
    protected final WorldMapRenderer renderer = new WorldMapRenderer();
    protected final WorldMapMarkers markers = new WorldMapMarkers();
    protected WorldMapSymbols symbols;
    protected final SymbolsLayoutData symbolsLayoutData = new SymbolsLayoutData(this);
    protected final WorldMapStyleLayer.RGBAf color = new WorldMapStyleLayer.RGBAf().init(0.85882354F, 0.84313726F, 0.7529412F, 1.0F);
    private float clickWorldX;
    private float clickWorldY;
    protected final UIWorldMapV1 apiV1 = new UIWorldMapV1(this);
    protected final UIWorldMapV2 apiV2 = new UIWorldMapV2(this);
    protected final UIWorldMapV3 apiV3 = new UIWorldMapV3(this);
    private final boolean dataWasReady = false;
    private boolean doStencil = true;
    private boolean mapEditor;
    HashMap<Zone, NestedPaths> nestedPathMap = new HashMap<>();
    NestedPathWanderer nestedPathWanderer = new NestedPathWanderer();
    private final ArrayList<BuildingDef> buildingsWithoutFeatures = new ArrayList<>();
    private boolean renderBuildingsWithoutFeatures;
    private final ArrayList<BuildingDef> basements = new ArrayList<>();
    private boolean initBasements;

    public UIWorldMap(KahluaTable table) {
        super(table);
    }

    public UIWorldMapV3 getAPI() {
        return this.apiV3;
    }

    public UIWorldMapV1 getAPIv1() {
        return this.apiV1;
    }

    public UIWorldMapV2 getAPIv2() {
        return this.apiV2;
    }

    public UIWorldMapV3 getAPIv3() {
        return this.apiV3;
    }

    public WorldMapSymbols getSymbolsDirect() {
        return this.symbols;
    }

    public void checkSymbolsLayout() {
        this.symbolsLayoutData.checkLayout();
    }

    public SymbolsLayoutData getSymbolsLayoutData() {
        return this.symbolsLayoutData;
    }

    public WorldMap getWorldMap() {
        return this.worldMap;
    }

    protected void setMapItem(MapItem mapItem) {
        this.symbols = mapItem.getSymbols();
    }

    public boolean isMapEditor() {
        return this.mapEditor;
    }

    public void setMapEditor(boolean b) {
        this.mapEditor = b;
    }

    public void scaleWidthToHeight() {
        double zoom = MapProjection.zoomAtMetersPerPixel(this.worldMap.getHeightInSquares() / this.getHeight(), this.getHeight());
        float worldScale = this.renderer.getWorldScale((float)zoom);
        this.setWidth(this.worldMap.getWidthInSquares() * worldScale);
        if (this.getTable() != null) {
            this.getTable().rawset("width", this.getWidth());
        }

        this.renderer
            .setMap(this.worldMap, this.getAbsoluteX().intValue(), this.getAbsoluteY().intValue(), this.getWidth().intValue(), this.getHeight().intValue());
        this.renderer.resetView();
    }

    @Override
    public void render() {
        if (this.isVisible()) {
            if (this.parent == null || this.parent.getMaxDrawHeight() == -1.0 || !(this.parent.getMaxDrawHeight() <= this.getY())) {
                if (!this.worldMap.hasData()) {
                }

                Double sx1 = this.clampToParentX(this.getAbsoluteX().intValue());
                Double sx2 = this.clampToParentX(this.getAbsoluteX().intValue() + this.getWidth());
                Double sy1 = this.clampToParentY(this.getAbsoluteY().intValue());
                Double sy2 = this.clampToParentY(this.getAbsoluteY().intValue() + this.getHeight());
                if (this.doStencil) {
                    this.setStencilRect(
                        sx1.intValue() - this.getAbsoluteX().intValue(),
                        sy1.intValue() - this.getAbsoluteY().intValue(),
                        sx2.intValue() - sx1.intValue(),
                        sy2.intValue() - sy1.intValue()
                    );
                }

                this.DrawTextureScaledColor(
                    null, 0.0, 0.0, this.getWidth(), this.getHeight(), (double)this.color.r, (double)this.color.g, (double)this.color.b, (double)this.color.a
                );
                this.renderer
                    .setMap(
                        this.worldMap, this.getAbsoluteX().intValue(), this.getAbsoluteY().intValue(), this.getWidth().intValue(), this.getHeight().intValue()
                    );
                this.renderer.updateView();
                float zoomF = this.renderer.getDisplayZoomF();
                float centerWorldX = this.renderer.getCenterWorldX();
                float centerWorldY = this.renderer.getCenterWorldY();
                float worldScale = this.apiV1.getWorldScale(zoomF);
                if (this.renderer.getBoolean("HideUnvisited") && WorldMapVisited.getInstance() != null) {
                    this.renderer.setVisited(WorldMapVisited.getInstance());
                } else {
                    this.renderer.setVisited(null);
                }

                this.renderer.render(this);
                this.markers.render(this);
                this.renderLocalPlayers();
                this.renderRemotePlayers();
                if (this.renderer.getBoolean("LargeStreetLabel")) {
                    this.renderStreetLabel();
                }

                if (this.renderer.getBoolean("WGRoads")) {
                    WorldGenDebug.getInstance().renderRoads(this);
                }

                if (this.renderer.getBoolean("Animals") || this.renderer.getBoolean("AnimalTracks")) {
                    AnimalZones.getInstance().render(this, this.renderer.getBoolean("Animals"), this.renderer.getBoolean("AnimalTracks"));
                }

                if (this.renderer.getBoolean("Players") && (!(zoomF >= 20.0F) || !this.renderer.getBoolean("PlayerModel"))) {
                    for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                        IsoPlayer player = IsoPlayer.players[pn];
                        if (player != null && !player.isDead()) {
                            float playerX = player.getX();
                            float playerY = player.getY();
                            if (player.getVehicle() != null) {
                                playerX = player.getVehicle().getX();
                                playerY = player.getVehicle().getY();
                            }

                            float uiX = this.apiV1
                                .worldToUIX(playerX, playerY, zoomF, centerWorldX, centerWorldY, this.renderer.getModelViewProjectionMatrix());
                            float uiY = this.apiV1
                                .worldToUIY(playerX, playerY, zoomF, centerWorldX, centerWorldY, this.renderer.getModelViewProjectionMatrix());
                            uiX = PZMath.floor(uiX);
                            uiY = PZMath.floor(uiY);
                            this.DrawTextureScaledColor(null, uiX - 3.0, uiY - 3.0, 6.0, 6.0, 1.0, 0.0, 0.0, 1.0);
                        }
                    }
                }

                int fontHgt = TextManager.instance.getFontHeight(UIFont.Small);
                if (Core.debug && this.renderer.getBoolean("DebugInfo")) {
                    this.DrawTextureScaledColor(null, 0.0, 0.0, 200.0, fontHgt * 6.0, 1.0, 1.0, 1.0, 1.0);
                    float worldX = this.apiV1.mouseToWorldX();
                    float worldY = this.apiV1.mouseToWorldY();
                    double r = 0.0;
                    double g = 0.0;
                    double b = 0.0;
                    double a = 1.0;
                    int y = 0;
                    this.DrawText("SQUARE = " + (int)worldX + "," + (int)worldY, 0.0, y, 0.0, 0.0, 0.0, 1.0);
                    y += fontHgt;
                    this.DrawText("CELL = " + (int)(worldX / 256.0F) + "," + (int)(worldY / 256.0F), 0.0, y, 0.0, 0.0, 0.0, 1.0);
                    y += fontHgt;
                    this.DrawText("CELL (300) = " + (int)(worldX / 300.0F) + "," + (int)(worldY / 300.0F), 0.0, y, 0.0, 0.0, 0.0, 1.0);
                    y += fontHgt;
                    this.DrawText(
                        "CENTER = " + PZMath.fastfloor(this.renderer.getCenterWorldX()) + "," + PZMath.fastfloor(this.renderer.getCenterWorldY()),
                        0.0,
                        y,
                        0.0,
                        0.0,
                        0.0,
                        1.0
                    );
                    y += fontHgt;
                    this.DrawText("ZOOM = " + this.renderer.getDisplayZoomF(), 0.0, y, 0.0, 0.0, 0.0, 1.0);
                    y += fontHgt;
                    this.DrawText("SCALE = " + this.renderer.getWorldScale(this.renderer.getZoomF()), 0.0, y, 0.0, 0.0, 0.0, 1.0);
                    y += fontHgt;
                }

                if (this.doStencil) {
                    this.clearStencilRect();
                    this.repaintStencilRect(0.0, 0.0, this.width, this.height);
                }

                if (Core.debug && DebugOptions.instance.uiRenderOutline.getValue()) {
                    Double x = -this.getXScroll();
                    Double y = -this.getYScroll();
                    double r1 = this.isMouseOver() ? 0.0 : 1.0;
                    this.DrawTextureScaledColor(null, x, y, 1.0, (double)this.height, r1, 1.0, 1.0, 0.5);
                    this.DrawTextureScaledColor(null, x + 1.0, y, this.width - 2.0, 1.0, r1, 1.0, 1.0, 0.5);
                    this.DrawTextureScaledColor(null, x + this.width - 1.0, y, 1.0, (double)this.height, r1, 1.0, 1.0, 0.5);
                    this.DrawTextureScaledColor(null, x + 1.0, y + this.height - 1.0, this.width - 2.0, 1.0, r1, 1.0, 1.0, 0.5);
                }

                if (Core.debug && this.renderer.getBoolean("HitTest")) {
                    float worldX = this.apiV1.mouseToWorldX();
                    float worldY = this.apiV1.mouseToWorldY();
                    s_tempFeatures.clear();

                    for (WorldMapData data : this.worldMap.data) {
                        if (data.isReady()) {
                            data.hitTest(worldX, worldY, s_tempFeatures);
                        }
                    }

                    if (!s_tempFeatures.isEmpty()) {
                        WorldMapFeature feature = s_tempFeatures.get(s_tempFeatures.size() - 1);
                        int cx = feature.cell.x * 256;
                        int cy = feature.cell.y * 256;
                        int absX = this.getAbsoluteX().intValue();
                        int absY = this.getAbsoluteY().intValue();
                        WorldMapGeometry geometry = feature.geometry;

                        for (int i = 0; i < geometry.points.size(); i++) {
                            WorldMapPoints outer = geometry.points.get(i);

                            for (int j = 0; j < outer.numPoints(); j++) {
                                int p1x = outer.getX(j);
                                int p1y = outer.getY(j);
                                int p2x = outer.getX((j + 1) % outer.numPoints());
                                int p2y = outer.getY((j + 1) % outer.numPoints());
                                float x1 = this.apiV1.worldToUIX(cx + p1x, cy + p1y);
                                float y1 = this.apiV1.worldToUIY(cx + p1x, cy + p1y);
                                float x2 = this.apiV1.worldToUIX(cx + p2x, cy + p2y);
                                float y2 = this.apiV1.worldToUIY(cx + p2x, cy + p2y);
                                SpriteRenderer.instance
                                    .renderline(null, absX + (int)x1, absY + (int)y1, absX + (int)x2, absY + (int)y2, 1.0F, 0.0F, 0.0F, 1.0F);
                            }
                        }
                    }
                }

                if (Core.debug && this.renderer.getBoolean("BuildingsWithoutFeatures")) {
                    this.renderBuildingsWithoutFeatures();
                } else {
                    this.renderBuildingsWithoutFeatures = false;
                }

                if (Core.debug && this.renderer.getBoolean("Basements")) {
                    this.renderBasements();
                }

                super.render();
            }
        }
    }

    private void renderStreetLabel() {
        WorldMapStreetsV1 streetsV1 = this.getAPI().getStreetsAPI();
        if (streetsV1 != null) {
            WorldMapStreet mouseOverStreet = streetsV1.getMouseOverStreet();
            if (mouseOverStreet != null) {
                mouseOverStreet.clipToObscuredCells();
                float mouseOverStreetWorldX = streetsV1.getMouseOverStreetWorldX();
                float mouseOverStreetWorldY = streetsV1.getMouseOverStreetWorldY();
                float uiX = this.getAPI().worldToUIX(mouseOverStreetWorldX, mouseOverStreetWorldY);
                float uiY = this.getAPI().worldToUIY(mouseOverStreetWorldX, mouseOverStreetWorldY);
                UIFont font = UIFont.SdfBold;
                float textW = TextManager.instance.MeasureStringX(font, mouseOverStreet.getTranslatedText());
                float textH = TextManager.instance.getFontHeight(font);
                uiX -= textW / 2.0F;
                uiY -= textH * 1.5F;
                uiX = Math.round(uiX);
                uiY = Math.round(uiY);
                SpriteRenderer.instance
                    .renderPoly(
                        uiX,
                        uiY,
                        uiX,
                        uiY + textH,
                        uiX - textH / 3.0F,
                        uiY + textH * 2.0F / 3.0F,
                        uiX - textH / 3.0F,
                        uiY + textH * 1.0F / 3.0F,
                        0.19607843F,
                        0.50980395F,
                        0.9647059F,
                        1.0F
                    );
                SpriteRenderer.instance
                    .renderPoly(
                        uiX + textW,
                        uiY,
                        uiX + textW,
                        uiY + textH,
                        uiX + textW + textH / 3.0F,
                        uiY + textH * 2.0F / 3.0F,
                        uiX + textW + textH / 3.0F,
                        uiY + textH * 1.0F / 3.0F,
                        0.19607843F,
                        0.50980395F,
                        0.9647059F,
                        1.0F
                    );
                SpriteRenderer.instance
                    .render(null, uiX, uiY, uiX + textW, uiY, uiX + textW, uiY + textH, uiX, uiY + textH, 0.19607843F, 0.50980395F, 0.9647059F, 1.0F, null);
                this.DrawTextSdf(font, mouseOverStreet.getTranslatedText(), uiX, uiY, 1.0, 1.0, 1.0, 1.0, 1.0);
            }
        }
    }

    private void renderLocalPlayers() {
        if (this.renderer.getBoolean("Players")) {
            float zoomF = this.renderer.getDisplayZoomF();
            if (!(zoomF >= 20.0F)) {
                for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                    IsoPlayer player = IsoPlayer.players[pn];
                    if (player != null && !player.isDead()) {
                        float playerX = player.getX();
                        float playerY = player.getY();
                        if (player.getVehicle() != null) {
                            playerX = player.getVehicle().getX();
                            playerY = player.getVehicle().getY();
                        }

                        this.renderPlayer(playerX, playerY);
                        if (GameClient.client) {
                            this.renderPlayerName(playerX, playerY, player.getUsername());
                        }
                    }
                }
            }
        }
    }

    private void renderRemotePlayers() {
        if (GameClient.client) {
            if (this.renderer.getBoolean("Players")) {
                if (this.renderer.getBoolean("RemotePlayers")) {
                    ArrayList<WorldMapRemotePlayer> remotePlayers = WorldMapRemotePlayers.instance.getPlayers();

                    for (int i = 0; i < remotePlayers.size(); i++) {
                        WorldMapRemotePlayer remotePlayer = remotePlayers.get(i);
                        if (this.shouldShowRemotePlayer(remotePlayer)) {
                            this.renderPlayer(remotePlayer.getX(), remotePlayer.getY());
                            this.renderPlayerName(remotePlayer.getX(), remotePlayer.getY(), remotePlayer.getUsername());
                        }
                    }
                }
            }
        }
    }

    private boolean shouldShowRemotePlayer(WorldMapRemotePlayer remotePlayer) {
        if (!remotePlayer.hasFullData()) {
            return false;
        } else if (remotePlayer.isInvisible()) {
            return this.isAdminSeeRemotePlayers(remotePlayer);
        } else if (this.isAdminSeeRemotePlayers(remotePlayer)) {
            return true;
        } else {
            boolean cansee = false;

            for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
                IsoPlayer player = IsoPlayer.players[pn];
                if (player != null) {
                    IsoPlayer remoteIsoPlayer = null;

                    for (IsoPlayer isoPlayer : GameClient.instance.getPlayers()) {
                        if (isoPlayer.getUsername().equals(remotePlayer.getUsername())) {
                            remoteIsoPlayer = isoPlayer;
                        }
                    }

                    if (player != null && remoteIsoPlayer != null && player.checkCanSeeClient(remoteIsoPlayer)) {
                        cansee = true;
                    }
                }
            }

            if (ServerOptions.getInstance().mapRemotePlayerVisibility.getValue() == 1) {
                return cansee;
            } else if (ServerOptions.getInstance().mapRemotePlayerVisibility.getValue() == 3) {
                return true;
            } else {
                for (int pnx = 0; pnx < IsoPlayer.numPlayers; pnx++) {
                    IsoPlayer player = IsoPlayer.players[pnx];
                    if (player != null) {
                        if (this.isInSameFaction(player, remotePlayer)) {
                            return true;
                        }

                        if (SafeHouse.isInSameSafehouse(player.getUsername(), remotePlayer.getUsername())) {
                            return true;
                        }
                    }
                }

                return false;
            }
        }
    }

    private boolean isAdminSeeRemotePlayers(WorldMapRemotePlayer remotePlayer) {
        for (int pn = 0; pn < IsoPlayer.numPlayers; pn++) {
            IsoPlayer player = IsoPlayer.players[pn];
            if (player != null
                && player.getRole().hasCapability(Capability.CanSeeAll)
                && player.getRole().getCapabilities().size() >= remotePlayer.getRolePower()) {
                return true;
            }
        }

        return false;
    }

    private boolean isInSameFaction(IsoPlayer player1, WorldMapRemotePlayer player2) {
        Faction factionLocal = Faction.getPlayerFaction(player1);
        Faction factionRemote = Faction.getPlayerFaction(player2.getUsername());
        return factionLocal != null && factionLocal == factionRemote;
    }

    private void renderPlayer(float playerX, float playerY) {
        float zoomF = this.renderer.getDisplayZoomF();
        float centerWorldX = this.renderer.getCenterWorldX();
        float centerWorldY = this.renderer.getCenterWorldY();
        float uiX = this.apiV1.worldToUIX(playerX, playerY, zoomF, centerWorldX, centerWorldY, this.renderer.getModelViewProjectionMatrix());
        float uiY = this.apiV1.worldToUIY(playerX, playerY, zoomF, centerWorldX, centerWorldY, this.renderer.getModelViewProjectionMatrix());
        uiX = PZMath.floor(uiX);
        uiY = PZMath.floor(uiY);
        this.DrawTextureScaledColor(null, uiX - 3.0, uiY - 3.0, 6.0, 6.0, 1.0, 0.0, 0.0, 1.0);
    }

    private void renderPlayerName(float playerX, float playerY, String userName) {
        if (this.renderer.getBoolean("PlayerNames")) {
            if (!StringUtils.isNullOrWhitespace(userName)) {
                float zoomF = this.renderer.getDisplayZoomF();
                float centerWorldX = this.renderer.getCenterWorldX();
                float centerWorldY = this.renderer.getCenterWorldY();
                float uiX = this.apiV1.worldToUIX(playerX, playerY, zoomF, centerWorldX, centerWorldY, this.renderer.getModelViewProjectionMatrix());
                float uiY = this.apiV1.worldToUIY(playerX, playerY, zoomF, centerWorldX, centerWorldY, this.renderer.getModelViewProjectionMatrix());
                uiX = PZMath.floor(uiX);
                uiY = PZMath.floor(uiY);
                int textWidth = TextManager.instance.MeasureStringX(UIFont.Small, userName) + 16;
                int fontHeight = TextManager.instance.font.getLineHeight();
                int textHeight = (int)Math.ceil(fontHeight * 1.25);
                this.DrawTextureScaledColor(null, uiX - textWidth / 2.0, uiY + 4.0, (double)textWidth, (double)textHeight, 0.5, 0.5, 0.5, 0.5);
                this.DrawTextCentre(userName, uiX, uiY + 4.0F + (textHeight - fontHeight) / 2.0, 0.0, 0.0, 0.0, 1.0);
            }
        }
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public Boolean onMouseDown(double x, double y) {
        return super.onMouseDown(x, y);
    }

    @Override
    public Boolean onMouseUp(double x, double y) {
        return super.onMouseUp(x, y);
    }

    @Override
    public void onMouseUpOutside(double x, double y) {
        super.onMouseUpOutside(x, y);
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        return super.onMouseMove(dx, dy);
    }

    @Override
    public Boolean onMouseWheel(double delta) {
        return super.onMouseWheel(delta);
    }

    public static void setExposed(LuaManager.Exposer exposer) {
        exposer.setExposed(MapItem.class);
        exposer.setExposed(MapSymbolDefinitions.class);
        exposer.setExposed(MapSymbolDefinitions.MapSymbolDefinition.class);
        exposer.setExposed(UIWorldMap.class);
        exposer.setExposed(UIWorldMapV1.class);
        exposer.setExposed(UIWorldMapV2.class);
        exposer.setExposed(UIWorldMapV3.class);
        exposer.setExposed(WorldMapGridSquareMarker.class);
        exposer.setExposed(WorldMapMarkers.class);
        exposer.setExposed(WorldMapRenderer.WorldMapBooleanOption.class);
        exposer.setExposed(WorldMapRenderer.WorldMapDoubleOption.class);
        exposer.setExposed(WorldMapVisited.class);
        WorldMapMarkersV1.setExposed(exposer);
        WorldMapStreetsV1.setExposed(exposer);
        WorldMapStyleV1.setExposed(exposer);
        WorldMapStyleV2.setExposed(exposer);
        WorldMapSymbolsV1.setExposed(exposer);
        WorldMapSymbolsV2.setExposed(exposer);
        exposer.setExposed(WorldMapEditorState.class);
        exposer.setExposed(WorldMapSettings.class);
        exposer.setExposed(WorldMapClient.class);
    }

    private void renderBuildingsWithoutFeatures() {
        if (this.renderBuildingsWithoutFeatures) {
            long seconds = System.currentTimeMillis() / 500L;
            if ((seconds & 1L) == 0L) {
                for (BuildingDef buildingDef : this.buildingsWithoutFeatures) {
                    this.debugRenderBuilding(buildingDef, false, 1.0F, 0.0F, 0.0F, 1.0F);
                }
            }
        } else {
            this.renderBuildingsWithoutFeatures = true;
            this.buildingsWithoutFeatures.clear();
            IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;

            for (int i = 0; i < metaGrid.buildings.size(); i++) {
                BuildingDef buildingDef = metaGrid.buildings.get(i);
                boolean hasFeature = false;

                for (int j = 0; j < buildingDef.rooms.size(); j++) {
                    RoomDef roomDef = buildingDef.rooms.get(j);
                    if (roomDef.level <= 0) {
                        ArrayList<RoomDef.RoomRect> rects = roomDef.getRects();

                        for (int k = 0; k < rects.size(); k++) {
                            RoomDef.RoomRect rect = rects.get(k);
                            s_tempFeatures.clear();

                            for (WorldMapData data : this.worldMap.data) {
                                if (data.isReady()) {
                                    data.hitTest(rect.x + rect.w / 2.0F, rect.y + rect.h / 2.0F, s_tempFeatures);
                                }
                            }

                            for (int f = 0; f < s_tempFeatures.size(); f++) {
                                WorldMapFeature feature = s_tempFeatures.get(f);
                                if (feature.properties.containsKey("building")) {
                                    hasFeature = true;
                                    break;
                                }
                            }

                            if (hasFeature) {
                                break;
                            }
                        }

                        if (hasFeature) {
                            break;
                        }
                    }
                }

                if (!hasFeature) {
                    this.buildingsWithoutFeatures.add(buildingDef);
                }
            }
        }
    }

    private void renderBasements() {
        if (this.initBasements) {
            long seconds = System.currentTimeMillis() / 500L;
            if ((seconds & 1L) == 0L) {
                for (BuildingDef buildingDef : this.basements) {
                    this.debugRenderBuilding(buildingDef, true, 1.0F, 0.0F, 0.0F, 1.0F);
                }
            }
        } else {
            this.initBasements = true;
            this.basements.clear();
            IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;

            for (int i = 0; i < metaGrid.buildings.size(); i++) {
                BuildingDef buildingDef = metaGrid.buildings.get(i);

                for (int j = 0; j < buildingDef.rooms.size(); j++) {
                    RoomDef roomDef = buildingDef.rooms.get(j);
                    if (roomDef.level < 0) {
                        this.basements.add(buildingDef);
                        break;
                    }
                }
            }
        }
    }

    private void debugRenderBuilding(BuildingDef buildingDef, boolean bBelowGround, float r, float g, float b, float a) {
        for (int i = 0; i < buildingDef.rooms.size(); i++) {
            RoomDef roomDef = buildingDef.rooms.get(i);
            if (bBelowGround == roomDef.level < 0) {
                ArrayList<RoomDef.RoomRect> rects = roomDef.getRects();

                for (int rr = 0; rr < rects.size(); rr++) {
                    RoomDef.RoomRect rect = rects.get(rr);
                    float x1 = this.apiV1.worldToUIX(rect.x, rect.y);
                    float y1 = this.apiV1.worldToUIY(rect.x, rect.y);
                    float x2 = this.apiV1.worldToUIX(rect.getX2(), rect.getY());
                    float y2 = this.apiV1.worldToUIY(rect.getX2(), rect.getY());
                    float x3 = this.apiV1.worldToUIX(rect.getX2(), rect.getY2());
                    float y3 = this.apiV1.worldToUIY(rect.getX2(), rect.getY2());
                    float x4 = this.apiV1.worldToUIX(rect.getX(), rect.getY2());
                    float y4 = this.apiV1.worldToUIY(rect.getX(), rect.getY2());
                    this.DrawTexture(null, x1, y1, x2, y2, x3, y3, x4, y4, r, g, b, a);
                }
            }
        }
    }

    public void DrawSymbol(
        Texture tex,
        double pointOfRotationX,
        double pointOfRotationY,
        double width,
        double height,
        double degrees,
        double scale,
        boolean bMatchPerspective,
        boolean bApplyZoom,
        double r,
        double g,
        double b,
        double a
    ) {
        IndieGL.StartShader(TextManager.sdfShader);
        TextManager.sdfShader.updateThreshold(0.1F);
        TextManager.sdfShader.updateShadow(0.0F);
        TextManager.sdfShader.updateOutline(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        if (bMatchPerspective) {
            double worldX = this.getAPI().uiToWorldX((float)pointOfRotationX, (float)pointOfRotationY);
            double worldY = this.getAPI().uiToWorldY((float)pointOfRotationX, (float)pointOfRotationY);
            pointOfRotationX = worldX;
            pointOfRotationY = worldY;
        }

        if (bMatchPerspective) {
            scale /= this.getAPI().getWorldScale();
        }

        double radians = degrees * (float) (Math.PI / 180.0);
        double cosA = Math.cos(radians);
        double sinA = Math.sin(radians);
        DoublePoint leftTop = DoublePointPool.alloc();
        DoublePoint rightTop = DoublePointPool.alloc();
        DoublePoint rightBottom = DoublePointPool.alloc();
        DoublePoint leftBottom = DoublePointPool.alloc();
        double x0 = 0.0 - width / 2.0;
        double y0 = 0.0 - height / 2.0;
        double x1 = x0 + width;
        double y1 = y0 + height;
        this.getAbsolutePosition(x0, y0, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, leftTop);
        this.getAbsolutePosition(x1, y0, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, rightTop);
        this.getAbsolutePosition(x1, y1, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, rightBottom);
        this.getAbsolutePosition(x0, y1, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, leftBottom);
        if (bMatchPerspective) {
            this.worldToUI(leftTop);
            this.worldToUI(rightTop);
            this.worldToUI(rightBottom);
            this.worldToUI(leftBottom);
        }

        leftTop.translate(this.getAbsoluteX(), this.getAbsoluteY());
        rightTop.translate(this.getAbsoluteX(), this.getAbsoluteY());
        rightBottom.translate(this.getAbsoluteX(), this.getAbsoluteY());
        leftBottom.translate(this.getAbsoluteX(), this.getAbsoluteY());
        SpriteRenderer.instance
            .render(
                tex,
                leftTop.x,
                leftTop.y,
                rightTop.x,
                rightTop.y,
                rightBottom.x,
                rightBottom.y,
                leftBottom.x,
                leftBottom.y,
                (float)r,
                (float)g,
                (float)b,
                (float)a,
                null
            );
        DoublePointPool.release(leftTop);
        DoublePointPool.release(rightTop);
        DoublePointPool.release(rightBottom);
        DoublePointPool.release(leftBottom);
        IndieGL.EndShader();
    }

    public void DrawTextSdf(UIFont font, String text, double x, double y, double scale, double r, double g, double b, double alpha) {
        if (!TextManager.instance.isSdf(font)) {
        }

        TextManager.sdfShader.updateThreshold((float)WorldMapTextSymbol.getSdfThreshold(1.0, 0.0, scale));
        TextManager.sdfShader.updateShadow(0.0F);
        TextManager.sdfShader.updateOutline(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        IndieGL.StartShader(TextManager.sdfShader);
        this.DrawText(font, text, x, y, scale, r, g, b, alpha);
        if (!TextManager.instance.isSdf(font)) {
        }

        IndieGL.EndShader();
    }

    public void DrawTextSdfRotated(
        String layerID,
        String text,
        double pointOfRotationX,
        double pointOfRotationY,
        double anchorX,
        double anchorY,
        double degrees,
        double scale,
        boolean bMatchPerspective,
        boolean bApplyZoom,
        double r,
        double g,
        double b,
        double alpha
    ) {
        WorldMapTextStyleLayer textLayer = this.style.getTextStyleLayerOrDefault(layerID);
        UIFont uiFont = textLayer.getFont();
        double radians = degrees * (float) (Math.PI / 180.0);
        double cosA = Math.cos(radians);
        double sinA = Math.sin(radians);
        TextLayout textLayout = s_textLayout.set(text, textLayer);
        double width = textLayout.maxLineLength;
        double height = textLayout.numLines * TextManager.instance.getFontHeight(uiFont);
        AngelCodeFont font = TextManager.instance.getFontFromEnum(uiFont);
        AngelCodeFont.CharDef lastCharDef = null;
        if (bMatchPerspective) {
            double worldX = this.getAPI().uiToWorldX((float)pointOfRotationX, (float)pointOfRotationY);
            double worldY = this.getAPI().uiToWorldY((float)pointOfRotationX, (float)pointOfRotationY);
            pointOfRotationX = worldX;
            pointOfRotationY = worldY;
        }

        double dx = width * anchorX;
        double dy = height * anchorY;
        if (alpha == 0.0) {
            WorldMapStyleLayer.RGBAf rgbaf = textLayer.evalColor(this.getAPI().getZoomF(), textLayer.fill);
            r = rgbaf.r;
            g = rgbaf.g;
            b = rgbaf.b;
            alpha = PZMath.max(rgbaf.a, 0.25F);
            WorldMapStyleLayer.RGBAf.s_pool.release(rgbaf);
        }

        if (!TextManager.instance.isSdf(uiFont)) {
        }

        TextManager.sdfShader.updateThreshold((float)WorldMapTextSymbol.getSdfThreshold(cosA, sinA, scale));
        TextManager.sdfShader.updateShadow(0.0F);
        float thick = 0.0F;
        if (TextManager.instance.isSdf(uiFont) && r == 1.0 && g == 1.0 && b == 1.0) {
            thick = 0.1F;
        }

        TextManager.sdfShader.updateOutline(thick, 0.0F, 0.0F, 0.0F, thick > 0.0F ? (float)alpha : 0.0F);
        IndieGL.StartShader(TextManager.sdfShader);
        if (bMatchPerspective) {
            scale /= this.getAPI().getWorldScale();
        }

        DoublePoint leftTop = DoublePointPool.alloc();
        DoublePoint rightTop = DoublePointPool.alloc();
        DoublePoint rightBottom = DoublePointPool.alloc();
        DoublePoint leftBottom = DoublePointPool.alloc();

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
                            this.getAbsolutePosition(x0, y0, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, leftTop);
                            this.getAbsolutePosition(x1, y0, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, rightTop);
                            this.getAbsolutePosition(x1, y1, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, rightBottom);
                            this.getAbsolutePosition(x0, y1, pointOfRotationX, pointOfRotationY, cosA, sinA, scale, scale, leftBottom);
                            if (bMatchPerspective) {
                                this.worldToUI(leftTop);
                                this.worldToUI(rightTop);
                                this.worldToUI(rightBottom);
                                this.worldToUI(leftBottom);
                            }

                            leftTop.translate(this.getAbsoluteX(), this.getAbsoluteY());
                            rightTop.translate(this.getAbsoluteX(), this.getAbsoluteY());
                            rightBottom.translate(this.getAbsoluteX(), this.getAbsoluteY());
                            leftBottom.translate(this.getAbsoluteX(), this.getAbsoluteY());
                            SpriteRenderer.instance
                                .render(
                                    charDef.image,
                                    leftTop.x,
                                    leftTop.y,
                                    rightTop.x,
                                    rightTop.y,
                                    rightBottom.x,
                                    rightBottom.y,
                                    leftBottom.x,
                                    leftBottom.y,
                                    (float)r,
                                    (float)g,
                                    (float)b,
                                    (float)alpha,
                                    null
                                );
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
        if (!TextManager.instance.isSdf(uiFont)) {
        }

        IndieGL.EndShader();
    }

    private DoublePoint getAbsolutePosition(
        double localX, double localY, double pointOfRotationX, double pointOfRotationY, double cosA, double sinA, double scaleX, double scaleY, DoublePoint out
    ) {
        localX *= scaleX;
        localY *= scaleY;
        out.x = pointOfRotationX + localX * cosA + localY * sinA;
        out.y = pointOfRotationY - localX * sinA + localY * cosA;
        return out;
    }

    private void worldToUI(DoublePoint pos) {
        double x = this.getAPI().worldToUIX((float)pos.x, (float)pos.y);
        double y = this.getAPI().worldToUIY((float)pos.x, (float)pos.y);
        pos.set(x, y);
    }

    public void setDoStencil(boolean value) {
        this.doStencil = value;
    }
}
