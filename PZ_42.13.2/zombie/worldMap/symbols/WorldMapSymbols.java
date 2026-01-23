// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.characters.animals.pathfind.Mesh;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;
import zombie.util.StringUtils;
import zombie.worldMap.UIWorldMap;

public class WorldMapSymbols {
    public static final int SAVEFILE_VERSION1 = 1;
    public static final int SAVEFILE_VERSION2 = 2;
    public static final int SAVEFILE_VERSION = 2;
    public static final float MIN_VISIBLE_ZOOM = 14.5F;
    public static final float COLLAPSED_RADIUS = 3.0F;
    private final ArrayList<WorldMapBaseSymbol> symbols = new ArrayList<>();
    private final ArrayList<IWorldMapSymbolListener> listeners = new ArrayList<>();
    private boolean userEditing;
    private long modificationCount;

    public static String getDefaultTextLayerID() {
        return "text-note";
    }

    public long getModificationCount() {
        return this.modificationCount;
    }

    public WorldMapTextSymbol addTranslatedText(String text, String layerID, float x, float y, float r, float g, float b, float a) {
        return this.addText(text, true, layerID, x, y, 0.0F, 0.0F, 0.666F, r, g, b, a);
    }

    public WorldMapTextSymbol addUntranslatedText(String text, String layerID, float x, float y, float r, float g, float b, float a) {
        return this.addText(text, false, layerID, x, y, 0.0F, 0.0F, 0.666F, r, g, b, a);
    }

    public WorldMapTextSymbol addText(
        String text, boolean translated, String layerID, float x, float y, float anchorX, float anchorY, float scale, float r, float g, float b, float a
    ) {
        if (StringUtils.isNullOrWhitespace(layerID)) {
            layerID = getDefaultTextLayerID();
        }

        WorldMapTextSymbol symbol = new WorldMapTextSymbol(this);
        symbol.text = text;
        symbol.translated = translated;
        symbol.layerId = layerID;
        symbol.x = x;
        symbol.y = y;
        symbol.anchorX = PZMath.clamp(anchorX, 0.0F, 1.0F);
        symbol.anchorY = PZMath.clamp(anchorY, 0.0F, 1.0F);
        symbol.scale = scale;
        symbol.r = r;
        symbol.g = g;
        symbol.b = b;
        symbol.a = a;
        this.addSymbol(symbol);
        return symbol;
    }

    public WorldMapTextureSymbol addTexture(String symbolID, float x, float y, float r, float g, float b, float a) {
        return this.addTexture(symbolID, x, y, 0.0F, 0.0F, 0.666F, r, g, b, a);
    }

    public WorldMapTextureSymbol addTexture(String symbolID, float x, float y, float anchorX, float anchorY, float scale, float r, float g, float b, float a) {
        WorldMapTextureSymbol symbol = new WorldMapTextureSymbol(this);
        symbol.setSymbolID(symbolID);
        MapSymbolDefinitions.MapSymbolDefinition symbolDefinition = MapSymbolDefinitions.getInstance().getSymbolById(symbolID);
        if (symbolDefinition == null) {
            symbol.width = 18.0F;
            symbol.height = 18.0F;
        } else {
            symbol.texture = GameServer.server ? null : Texture.getSharedTexture(symbolDefinition.getTexturePath());
            symbol.width = symbolDefinition.getWidth();
            symbol.height = symbolDefinition.getHeight();
        }

        if (symbol.texture == null && !GameServer.server) {
            symbol.texture = Texture.getErrorTexture();
        }

        symbol.x = x;
        symbol.y = y;
        symbol.anchorX = PZMath.clamp(anchorX, 0.0F, 1.0F);
        symbol.anchorY = PZMath.clamp(anchorY, 0.0F, 1.0F);
        symbol.scale = scale;
        symbol.r = r;
        symbol.g = g;
        symbol.b = b;
        symbol.a = a;
        this.addSymbol(symbol);
        return symbol;
    }

    public void addSymbol(WorldMapBaseSymbol symbol) {
        if (!this.symbols.contains(symbol)) {
            this.modificationCount++;
            this.symbols.add(symbol);
            this.listeners.forEach(listener -> listener.onAdd(symbol));
        }
    }

    public int indexOf(WorldMapBaseSymbol symbol) {
        return this.symbols.indexOf(symbol);
    }

    public void removeSymbol(WorldMapBaseSymbol symbol) {
        int index = this.symbols.indexOf(symbol);
        if (index != -1) {
            this.removeSymbolByIndex(index);
        }
    }

    public void removeSymbolByIndex(int index) {
        this.modificationCount++;
        WorldMapBaseSymbol symbol = this.symbols.get(index);
        this.listeners.forEach(listener -> listener.onBeforeRemove(symbol));
        this.symbols.remove(index);
        this.listeners.forEach(listener -> listener.onAfterRemove(symbol));
        symbol.release();
    }

    public boolean isUserEditing() {
        return this.userEditing;
    }

    public void setUserEditing(boolean b) {
        this.userEditing = b;
    }

    public void clear() {
        this.modificationCount++;
        this.listeners.forEach(IWorldMapSymbolListener::onBeforeClear);

        for (int i = 0; i < this.symbols.size(); i++) {
            this.symbols.get(i).release();
        }

        this.symbols.clear();
        this.listeners.forEach(IWorldMapSymbolListener::onAfterClear);
    }

    public void clearDefaultAnnotations() {
        for (int i = this.symbols.size() - 1; i >= 0; i--) {
            WorldMapBaseSymbol symbol = this.symbols.get(i);
            if (!symbol.isUserDefined()) {
                this.removeSymbolByIndex(i);
            }
        }
    }

    public void clearUserAnnotations() {
        for (int i = this.symbols.size() - 1; i >= 0; i--) {
            WorldMapBaseSymbol symbol = this.symbols.get(i);
            if (symbol.isUserDefined()) {
                this.removeSymbolByIndex(i);
            }
        }
    }

    public void invalidateLayout() {
        this.modificationCount++;
    }

    public int getSymbolCount() {
        return this.symbols.size();
    }

    public WorldMapBaseSymbol getSymbolByIndex(int index) {
        return this.symbols.get(index);
    }

    boolean isSymbolVisible(UIWorldMap ui, WorldMapBaseSymbol symbol) {
        return !(symbol.widthScaled(ui) < 10.0F) && !(symbol.heightScaled(ui) < 10.0F) ? symbol.isVisible(ui) : false;
    }

    int hitTest(UIWorldMap ui, float uiX, float uiY) {
        ui.checkSymbolsLayout();
        float closestDist = Float.MAX_VALUE;
        int closestIndex = -1;
        DoublePoint leftTop = DoublePointPool.alloc();
        DoublePoint rightTop = DoublePointPool.alloc();
        DoublePoint rightBottom = DoublePointPool.alloc();
        DoublePoint leftBottom = DoublePointPool.alloc();

        try {
            for (int i = 0; i < this.symbols.size(); i++) {
                WorldMapBaseSymbol symbol = this.symbols.get(i);
                if (this.isSymbolVisible(ui, symbol) && (symbol.isUserDefined() || ui.isMapEditor())) {
                    SymbolLayout layout = ui.getSymbolsLayoutData().getLayout(symbol);
                    float x1 = layout.x;
                    float y1 = layout.y;
                    float x2 = x1 + symbol.widthScaled(ui);
                    float y2 = y1 + symbol.heightScaled(ui);
                    if (layout.collided) {
                        x1 += symbol.widthScaled(ui) / 2.0F - 1.5F;
                        y1 += symbol.heightScaled(ui) / 2.0F - 1.5F;
                        x2 = x1 + 6.0F;
                        y2 = y1 + 6.0F;
                        float dist = IsoUtils.DistanceToSquared((x1 + x2) / 2.0F, (y1 + y2) / 2.0F, uiX, uiY);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closestIndex = i;
                        }
                    }

                    symbol.getOutlinePoints(ui, leftTop, rightTop, rightBottom, leftBottom);
                    float z = 0.0F;
                    if (Mesh.testPointInTriangle(
                        uiX,
                        uiY,
                        0.0F,
                        (float)leftTop.x,
                        (float)leftTop.y,
                        0.0F,
                        (float)rightTop.x,
                        (float)rightTop.y,
                        0.0F,
                        (float)rightBottom.x,
                        (float)rightBottom.y,
                        0.0F
                    )) {
                        return i;
                    }

                    if (Mesh.testPointInTriangle(
                        uiX,
                        uiY,
                        0.0F,
                        (float)leftTop.x,
                        (float)leftTop.y,
                        0.0F,
                        (float)rightBottom.x,
                        (float)rightBottom.y,
                        0.0F,
                        (float)leftBottom.x,
                        (float)leftBottom.y,
                        0.0F
                    )) {
                        return i;
                    }
                }
            }
        } finally {
            DoublePointPool.release(leftTop);
            DoublePointPool.release(rightTop);
            DoublePointPool.release(rightBottom);
            DoublePointPool.release(leftBottom);
        }

        return closestIndex != -1 && closestDist < 100.0F ? closestIndex : -1;
    }

    public void save(ByteBuffer output) throws IOException {
        output.putShort((short)2);
        SymbolSaveData saveData = new SymbolSaveData(241, 2);
        saveData.save(output, this);
        int count = 0;

        for (int i = 0; i < this.symbols.size(); i++) {
            WorldMapBaseSymbol symbol = this.symbols.get(i);
            if (symbol.isUserDefined() && symbol.getNetworkInfo() == null) {
                count++;
            }
        }

        output.putInt(count);

        for (int ix = 0; ix < this.symbols.size(); ix++) {
            WorldMapBaseSymbol symbol = this.symbols.get(ix);
            if (symbol.isUserDefined() && symbol.getNetworkInfo() == null) {
                output.put((byte)symbol.getType().index());
                symbol.save(output, saveData);
            }
        }
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        int SymbolsVersion = input.getShort();
        if (SymbolsVersion >= 1 && SymbolsVersion <= 2) {
            SymbolSaveData saveData = new SymbolSaveData(WorldVersion, SymbolsVersion);
            if (SymbolsVersion >= 2) {
                saveData.load(input);
            }

            int symbolCount = input.getInt();

            for (int i = 0; i < symbolCount; i++) {
                int symbolType = input.get();
                if (symbolType == WorldMapSymbols.WorldMapSymbolType.Text.index()) {
                    WorldMapTextSymbol textSymbol = new WorldMapTextSymbol(this);
                    textSymbol.load(input, saveData);
                    this.symbols.add(textSymbol);
                } else {
                    if (symbolType != WorldMapSymbols.WorldMapSymbolType.Texture.index()) {
                        throw new IOException("unknown map symbol type " + symbolType);
                    }

                    WorldMapTextureSymbol textureSymbol = new WorldMapTextureSymbol(this);
                    textureSymbol.load(input, saveData);
                    this.symbols.add(textureSymbol);
                }
            }
        } else {
            throw new IOException("unknown map symbols version " + SymbolsVersion);
        }
    }

    public void addListener(IWorldMapSymbolListener listener) {
        this.listeners.add(listener);
    }

    public static enum WorldMapSymbolType {
        NONE(-1),
        Text(0),
        Texture(1);

        private final byte type;

        private WorldMapSymbolType(final int type) {
            this.type = (byte)type;
        }

        public int index() {
            return this.type;
        }
    }
}
