// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaClosure;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.core.BoxedStaticValues;
import zombie.core.math.PZMath;
import zombie.inventory.types.MapItem;
import zombie.network.GameClient;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.network.WorldMapClient;
import zombie.worldMap.network.WorldMapSymbolNetworkInfo;
import zombie.worldMap.styles.WorldMapStyle;
import zombie.worldMap.styles.WorldMapTextStyleLayer;

@UsedFromLua
public final class WorldMapSymbolsV2 extends WorldMapSymbolsAPI {
    private static final Pool<WorldMapSymbolsV2.WorldMapTextSymbolV2> s_textPool = new Pool<>(WorldMapSymbolsV2.WorldMapTextSymbolV2::new);
    private static final Pool<WorldMapSymbolsV2.WorldMapTextureSymbolV2> s_texturePool = new Pool<>(WorldMapSymbolsV2.WorldMapTextureSymbolV2::new);
    private final UIWorldMap ui;
    private final WorldMapSymbols uiSymbols;
    private final ArrayList<WorldMapSymbolsV2.WorldMapBaseSymbolV2> symbols = new ArrayList<>();
    private final WorldMapSymbolsV2.Listener listener = new WorldMapSymbolsV2.Listener(this);
    static final ThreadLocal<TextLayout> TL_textLayout = ThreadLocal.withInitial(TextLayout::new);

    public WorldMapSymbolsV2(UIWorldMap ui, WorldMapSymbols symbols) {
        Objects.requireNonNull(ui);
        this.ui = ui;
        this.uiSymbols = symbols;
        this.uiSymbols.addListener(this.listener);
        this.reinit();
    }

    public WorldMapSymbolsV2.WorldMapTextSymbolV2 addTranslatedText(String text, UIFont font, float x, float y) {
        return this.addTranslatedText(text, WorldMapSymbols.getDefaultTextLayerID(), x, y);
    }

    public WorldMapSymbolsV2.WorldMapTextSymbolV2 addUntranslatedText(String text, UIFont font, float x, float y) {
        return this.addUntranslatedText(text, WorldMapSymbols.getDefaultTextLayerID(), x, y);
    }

    public WorldMapSymbolsV2.WorldMapTextSymbolV2 addTranslatedText(String text, String layerID, float x, float y) {
        WorldMapTextSymbol symbol = this.uiSymbols.addTranslatedText(text, layerID, x, y, 1.0F, 1.0F, 1.0F, 1.0F);
        return (WorldMapSymbolsV2.WorldMapTextSymbolV2)this.symbols.get(this.uiSymbols.indexOf(symbol));
    }

    public WorldMapSymbolsV2.WorldMapTextSymbolV2 addUntranslatedText(String text, String layerID, float x, float y) {
        WorldMapTextSymbol symbol = this.uiSymbols.addUntranslatedText(text, layerID, x, y, 1.0F, 1.0F, 1.0F, 1.0F);
        return (WorldMapSymbolsV2.WorldMapTextSymbolV2)this.symbols.get(this.uiSymbols.indexOf(symbol));
    }

    public WorldMapSymbolsV2.WorldMapTextureSymbolV2 addTexture(String symbolID, float x, float y) {
        WorldMapTextureSymbol symbol = this.uiSymbols.addTexture(symbolID, x, y, 1.0F, 1.0F, 1.0F, 1.0F);
        return (WorldMapSymbolsV2.WorldMapTextureSymbolV2)this.symbols.get(this.uiSymbols.indexOf(symbol));
    }

    public int hitTest(float uiX, float uiY) {
        return this.uiSymbols.hitTest(this.ui, uiX, uiY);
    }

    public int getSymbolCount() {
        return this.symbols.size();
    }

    public WorldMapSymbolsV2.WorldMapBaseSymbolV2 getSymbolByIndex(int index) {
        return this.symbols.get(index);
    }

    public void removeSymbolByIndex(int index) {
        this.uiSymbols.removeSymbolByIndex(index);
    }

    public void removeSymbol(WorldMapSymbolsV2.WorldMapBaseSymbolV2 symbol) {
        int index = this.uiSymbols.indexOf(symbol.symbol);
        if (index == -1) {
            throw new IllegalArgumentException("invalid symbol");
        } else {
            this.removeSymbolByIndex(index);
        }
    }

    public String getDefaultLayerID() {
        return WorldMapSymbols.getDefaultTextLayerID();
    }

    public float getDisplayScale(String layerID, float scale, boolean bApplyZoom) {
        WorldMapTextStyleLayer textLayer = this.ui.getAPI().getStyle().getTextStyleLayerOrDefault(layerID);
        UIFont font = textLayer.getFont();
        scale *= textLayer.calculateScale(this.ui);
        if (bApplyZoom) {
            float worldScale = this.ui.getAPI().getWorldScale();
            if (this.ui.getAPI().getBoolean("MiniMapSymbols")) {
                scale = PZMath.min(worldScale, 1.0F);
            } else {
                scale *= worldScale;
            }
        }

        return scale;
    }

    public int getTextLayoutWidth(String text, String layerID) {
        WorldMapStyle style = this.ui.getAPI().getStyle();
        WorldMapTextStyleLayer textLayer = style.getTextStyleLayerOrDefault(layerID);
        TextLayout textLayout = TL_textLayout.get();
        textLayout.set(text, textLayer);
        return textLayout.maxLineLength;
    }

    public int getTextLayoutHeight(String text, String layerID) {
        WorldMapStyle style = this.ui.getAPI().getStyle();
        WorldMapTextStyleLayer textLayer = style.getTextStyleLayerOrDefault(layerID);
        TextLayout textLayout = TL_textLayout.get();
        textLayout.set(text, textLayer);
        return textLayout.numLines * TextManager.instance.getFontHeight(textLayer.getFont());
    }

    public boolean isUserEditing() {
        return this.uiSymbols.isUserEditing();
    }

    public void setUserEditing(boolean b) {
        this.uiSymbols.setUserEditing(b);
    }

    public String getDefaultTextLayerID() {
        return "text-note";
    }

    public void clear() {
        this.uiSymbols.clear();
    }

    public void reinitDefaultAnnotations() {
        MapItem singleton = MapItem.getSingleton();
        if (singleton != null) {
            singleton.clearDefaultAnnotations();
        }
    }

    public void initDefaultAnnotations() {
        MapItem singleton = MapItem.getSingleton();
        if (singleton == null || this.ui.getSymbolsDirect() != singleton.getSymbols() || singleton.checkDefaultAnnotationsLoaded()) {
            ArrayList<String> lotDirs = LuaManager.GlobalObject.getLotDirectories();
            if (lotDirs != null && !lotDirs.isEmpty()) {
                for (int i = 0; i < lotDirs.size(); i++) {
                    String dirName = lotDirs.get(i);
                    String absFilePath = ZomboidFileSystem.instance.getString("media/maps/" + dirName + "/worldmap-annotations.lua");
                    Path path = FileSystems.getDefault().getPath(absFilePath);
                    if (Files.exists(path)) {
                        Object functionObj = LuaManager.GlobalObject.reloadLuaFile(absFilePath);
                        if (functionObj instanceof JavaFunction || functionObj instanceof LuaClosure) {
                            LuaManager.caller.pcallvoid(LuaManager.thread, functionObj, this.ui.getTable());
                        }
                    }
                }
            }
        }
    }

    public void clearUserAnnotations() {
        this.uiSymbols.clearUserAnnotations();
    }

    private void checkLayout() {
        this.ui.checkSymbolsLayout();
    }

    private void reinit() {
        for (int i = 0; i < this.symbols.size(); i++) {
            this.symbols.get(i).release();
        }

        this.symbols.clear();

        for (int i = 0; i < this.uiSymbols.getSymbolCount(); i++) {
            WorldMapBaseSymbol symbol = this.uiSymbols.getSymbolByIndex(i);
            if (symbol instanceof WorldMapTextSymbol textSymbol) {
                WorldMapSymbolsV2.WorldMapTextSymbolV2 symbolV2 = s_textPool.alloc().init(this, textSymbol);
                this.symbols.add(symbolV2);
            }

            if (symbol instanceof WorldMapTextureSymbol textureSymbol) {
                WorldMapSymbolsV2.WorldMapTextureSymbolV2 symbolV2 = s_texturePool.alloc().init(this, textureSymbol);
                this.symbols.add(symbolV2);
            }
        }
    }

    public void sendShareSymbol(WorldMapSymbolsV2.WorldMapBaseSymbolV2 symbolV2, WorldMapSymbolNetworkInfo networkInfo) {
        WorldMapClient.getInstance().sendShareSymbol(symbolV2.symbol, networkInfo);
    }

    public void sendRemoveSymbol(WorldMapSymbolsV2.WorldMapBaseSymbolV2 symbolV2) {
        WorldMapClient.getInstance().sendRemoveSymbol(symbolV2.symbol);
    }

    public void sendModifySymbol(WorldMapSymbolsV2.WorldMapBaseSymbolV2 symbolV2) {
        WorldMapClient.getInstance().sendModifySymbol(symbolV2.symbol);
    }

    public void sendSetPrivateSymbol(WorldMapSymbolsV2.WorldMapBaseSymbolV2 symbolV2) {
        WorldMapClient.getInstance().sendSetPrivateSymbol(symbolV2.symbol);
    }

    public static void setExposed(LuaManager.Exposer exposer) {
        exposer.setExposed(WorldMapSymbolsV2.class);
        exposer.setExposed(WorldMapSymbolsV2.WorldMapTextSymbolV2.class);
        exposer.setExposed(WorldMapSymbolsV2.WorldMapTextureSymbolV2.class);
    }

    private static final class Listener implements IWorldMapSymbolListener {
        final WorldMapSymbolsV2 api;

        Listener(WorldMapSymbolsV2 api) {
            this.api = api;
        }

        @Override
        public void onAdd(WorldMapBaseSymbol symbol) {
            int index = this.indexOf(symbol);
            if (symbol instanceof WorldMapTextSymbol textSymbol) {
                WorldMapSymbolsV2.WorldMapTextSymbolV2 symbolV2 = WorldMapSymbolsV2.s_textPool.alloc().init(this.api, textSymbol);
                this.api.symbols.add(index, symbolV2);
            } else if (symbol instanceof WorldMapTextureSymbol textureSymbol) {
                WorldMapSymbolsV2.WorldMapTextureSymbolV2 symbolV2 = WorldMapSymbolsV2.s_texturePool.alloc().init(this.api, textureSymbol);
                this.api.symbols.add(index, symbolV2);
            } else {
                throw new RuntimeException("unhandled symbol class " + symbol.getClass().getSimpleName());
            }
        }

        @Override
        public void onBeforeRemove(WorldMapBaseSymbol symbol) {
            int index = this.indexOf(symbol);
            WorldMapSymbolsV2.WorldMapBaseSymbolV2 symbolV2 = this.api.symbols.remove(index);
            symbolV2.release();
        }

        @Override
        public void onAfterRemove(WorldMapBaseSymbol symbol) {
        }

        @Override
        public void onBeforeClear() {
        }

        @Override
        public void onAfterClear() {
            this.api.reinit();
        }

        int indexOf(WorldMapBaseSymbol symbol) {
            return this.api.uiSymbols.indexOf(symbol);
        }
    }

    protected static class WorldMapBaseSymbolV2 extends PooledObject {
        WorldMapSymbolsV2 owner;
        WorldMapBaseSymbol symbol;

        WorldMapSymbolsV2.WorldMapBaseSymbolV2 init(WorldMapSymbolsV2 owner, WorldMapBaseSymbol symbol) {
            this.owner = owner;
            this.symbol = symbol;
            return this;
        }

        public float getWorldX() {
            return this.symbol.x;
        }

        public float getWorldY() {
            return this.symbol.y;
        }

        public boolean isUserDefined() {
            return this.symbol.isUserDefined();
        }

        public void setUserDefined(boolean b) {
            this.symbol.setUserDefined(b);
        }

        public float getScale() {
            return this.symbol.scale;
        }

        public float getDisplayX() {
            this.owner.checkLayout();
            SymbolLayout layout = this.owner.ui.getSymbolsLayoutData().getLayout(this.symbol);
            return layout.x + this.owner.ui.getAPIv1().worldOriginX();
        }

        public float getDisplayY() {
            this.owner.checkLayout();
            SymbolLayout layout = this.owner.ui.getSymbolsLayoutData().getLayout(this.symbol);
            return layout.y + this.owner.ui.getAPIv1().worldOriginY();
        }

        public float getDisplayWidth() {
            this.owner.checkLayout();
            return this.symbol.widthScaled(this.owner.ui);
        }

        public float getDisplayHeight() {
            this.owner.checkLayout();
            return this.symbol.heightScaled(this.owner.ui);
        }

        public float getDisplayScale() {
            this.owner.checkLayout();
            return this.symbol.getDisplayScale(this.owner.ui);
        }

        public void setAnchor(float x, float y) {
            this.symbol.setAnchor(x, y);
        }

        public float getAnchorX() {
            return this.symbol.getAnchorX();
        }

        public float getAnchorY() {
            return this.symbol.getAnchorY();
        }

        public float getRotation() {
            return this.symbol.getRotation();
        }

        public void setRotation(float degrees) {
            this.symbol.setRotation(degrees);
        }

        public boolean isMatchPerspective() {
            return this.symbol.isMatchPerspective();
        }

        public void setMatchPerspective(boolean b) {
            this.symbol.setMatchPerspective(b);
        }

        public boolean isApplyZoom() {
            return this.symbol.isApplyZoom();
        }

        public void setApplyZoom(boolean b) {
            this.symbol.setApplyZoom(b);
        }

        public float getMinZoom() {
            return this.symbol.getMinZoom();
        }

        public void setMinZoom(float zoomF) {
            this.symbol.setMinZoom(zoomF);
        }

        public float getMaxZoom() {
            return this.symbol.getMaxZoom();
        }

        public void setMaxZoom(float zoomF) {
            this.symbol.setMaxZoom(zoomF);
        }

        public void setPosition(float x, float y) {
            this.symbol.setPosition(x, y);
            this.owner.uiSymbols.invalidateLayout();
        }

        public void setCollide(boolean collide) {
            this.symbol.setCollide(collide);
        }

        public void setVisible(boolean visible) {
            this.symbol.setVisible(visible);
        }

        public boolean isVisible() {
            return this.symbol.isVisible(this.owner.ui);
        }

        public void setRGBA(float r, float g, float b, float a) {
            this.symbol.setRGBA(r, g, b, a);
        }

        public float getRed() {
            return this.symbol.r;
        }

        public float getGreen() {
            return this.symbol.g;
        }

        public float getBlue() {
            return this.symbol.b;
        }

        public float getAlpha() {
            return this.symbol.a;
        }

        public boolean hasCustomColor() {
            return this.symbol.hasCustomColor();
        }

        public void setScale(float scale) {
            this.symbol.setScale(scale);
            this.owner.uiSymbols.invalidateLayout();
        }

        public boolean isText() {
            return false;
        }

        public boolean isTexture() {
            return false;
        }

        public void setSharing(KahluaTable table) {
            if (table != null && !table.isEmpty()) {
                KahluaTableImpl tableImpl = (KahluaTableImpl)table;
                WorldMapSymbolNetworkInfo networkInfo = new WorldMapSymbolNetworkInfo();
                networkInfo.setAuthor(GameClient.username);
                boolean bEveryone = tableImpl.rawgetBool("everyone");
                boolean bFaction = tableImpl.rawgetBool("faction");
                boolean bSafehouse = tableImpl.rawgetBool("safehouse");
                networkInfo.setVisibleToEveryone(bEveryone);
                networkInfo.setVisibleToFaction(bFaction && !bEveryone);
                networkInfo.setVisibleToSafehouse(bSafehouse && !bEveryone);
                if (!bEveryone) {
                    KahluaTableImpl playerTable = Type.tryCastTo(tableImpl.rawget("players"), KahluaTableImpl.class);
                    if (playerTable != null && !playerTable.isEmpty()) {
                        int i = 1;

                        for (int count = playerTable.len(); i <= count; i++) {
                            String username = playerTable.rawgetStr(BoxedStaticValues.toDouble(i));
                            networkInfo.addPlayer(username);
                        }
                    }
                }

                this.owner.sendShareSymbol(this, networkInfo);
            } else if (!this.symbol.isPrivate()) {
                this.owner.sendSetPrivateSymbol(this);
            }
        }

        public boolean isShared() {
            return this.symbol.getNetworkInfo() != null;
        }

        public boolean isPrivate() {
            return this.symbol.getNetworkInfo() == null;
        }

        public String getAuthor() {
            return this.isShared() ? this.symbol.getNetworkInfo().getAuthor() : GameClient.username;
        }

        public boolean isVisibleToEveryone() {
            return this.isShared() && this.symbol.getNetworkInfo().isVisibleToEveryone();
        }

        public boolean isVisibleToFaction() {
            return this.isShared() && this.symbol.getNetworkInfo().isVisibleToFaction();
        }

        public boolean isVisibleToSafehouse() {
            return this.isShared() && this.symbol.getNetworkInfo().isVisibleToSafehouse();
        }

        public int getVisibleToPlayerCount() {
            return this.isShared() ? this.symbol.getNetworkInfo().getPlayerCount() : 0;
        }

        public String getVisibleToPlayerByIndex(int index) {
            return this.isShared() ? this.symbol.getNetworkInfo().getPlayerByIndex(index) : null;
        }

        public boolean canClientModify() {
            return this.isShared() ? StringUtils.equals(GameClient.username, this.getAuthor()) : true;
        }

        public void renderOutline(float r, float g, float b, float a, float thickness) {
            this.symbol.renderOutline(this.owner.ui, r, g, b, a, thickness);
        }

        public WorldMapSymbolsV2.WorldMapBaseSymbolV2 createCopy() {
            WorldMapBaseSymbol copy = this.symbol.createCopy();
            this.owner.uiSymbols.addSymbol(copy);
            return this.owner.getSymbolByIndex(this.owner.uiSymbols.indexOf(copy));
        }
    }

    @UsedFromLua
    public static class WorldMapTextSymbolV2 extends WorldMapSymbolsV2.WorldMapBaseSymbolV2 {
        WorldMapTextSymbol textSymbol;

        WorldMapSymbolsV2.WorldMapTextSymbolV2 init(WorldMapSymbolsV2 owner, WorldMapTextSymbol symbol) {
            super.init(owner, symbol);
            this.textSymbol = symbol;
            return this;
        }

        public void setTranslatedText(String text) {
            if (!StringUtils.isNullOrWhitespace(text)) {
                this.textSymbol.setTranslatedText(text);
                this.owner.uiSymbols.invalidateLayout();
            }
        }

        public void setUntranslatedText(String text) {
            if (!StringUtils.isNullOrWhitespace(text)) {
                this.textSymbol.setUntranslatedText(text);
                this.owner.uiSymbols.invalidateLayout();
            }
        }

        public String getTranslatedText() {
            return this.textSymbol.getTranslatedText();
        }

        public String getUntranslatedText() {
            return this.textSymbol.getUntranslatedText();
        }

        public String getLayerID() {
            return this.textSymbol.getLayerID();
        }

        public void setLayerID(String layerID) {
            this.textSymbol.setLayerID(layerID);
            this.owner.uiSymbols.invalidateLayout();
        }

        public UIFont getFont() {
            return this.textSymbol.getFont(this.owner.ui);
        }

        @Override
        public boolean isText() {
            return true;
        }
    }

    @UsedFromLua
    public static class WorldMapTextureSymbolV2 extends WorldMapSymbolsV2.WorldMapBaseSymbolV2 {
        WorldMapTextureSymbol textureSymbol;

        WorldMapSymbolsV2.WorldMapTextureSymbolV2 init(WorldMapSymbolsV2 owner, WorldMapTextureSymbol symbol) {
            super.init(owner, symbol);
            this.textureSymbol = symbol;
            return this;
        }

        public String getSymbolID() {
            return this.textureSymbol.getSymbolID();
        }

        @Override
        public boolean isTexture() {
            return true;
        }
    }
}
