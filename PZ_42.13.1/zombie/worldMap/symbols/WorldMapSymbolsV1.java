// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

import java.util.ArrayList;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.ui.UIFont;
import zombie.util.Pool;
import zombie.util.PooledObject;
import zombie.util.StringUtils;
import zombie.worldMap.UIWorldMap;

@UsedFromLua
public class WorldMapSymbolsV1 extends WorldMapSymbolsAPI {
    private static final Pool<WorldMapSymbolsV1.WorldMapTextSymbolV1> s_textPool = new Pool<>(WorldMapSymbolsV1.WorldMapTextSymbolV1::new);
    private static final Pool<WorldMapSymbolsV1.WorldMapTextureSymbolV1> s_texturePool = new Pool<>(WorldMapSymbolsV1.WorldMapTextureSymbolV1::new);
    private final UIWorldMap ui;
    private final WorldMapSymbols uiSymbols;
    private final ArrayList<WorldMapSymbolsV1.WorldMapBaseSymbolV1> symbols = new ArrayList<>();
    private final WorldMapSymbolsV1.Listener listener = new WorldMapSymbolsV1.Listener(this);

    public WorldMapSymbolsV1(UIWorldMap ui, WorldMapSymbols symbols) {
        Objects.requireNonNull(ui);
        this.ui = ui;
        this.uiSymbols = symbols;
        this.uiSymbols.addListener(this.listener);
        this.reinit();
    }

    public WorldMapSymbolsV1.WorldMapTextSymbolV1 addTranslatedText(String text, UIFont font, float x, float y) {
        WorldMapTextSymbol symbol = this.uiSymbols.addTranslatedText(text, WorldMapSymbols.getDefaultTextLayerID(), x, y, 1.0F, 1.0F, 1.0F, 1.0F);
        return (WorldMapSymbolsV1.WorldMapTextSymbolV1)this.symbols.get(this.symbols.size() - 1);
    }

    public WorldMapSymbolsV1.WorldMapTextSymbolV1 addUntranslatedText(String text, UIFont font, float x, float y) {
        WorldMapTextSymbol symbol = this.uiSymbols.addUntranslatedText(text, WorldMapSymbols.getDefaultTextLayerID(), x, y, 1.0F, 1.0F, 1.0F, 1.0F);
        return (WorldMapSymbolsV1.WorldMapTextSymbolV1)this.symbols.get(this.symbols.size() - 1);
    }

    public WorldMapSymbolsV1.WorldMapTextureSymbolV1 addTexture(String symbolID, float x, float y) {
        WorldMapTextureSymbol symbol = this.uiSymbols.addTexture(symbolID, x, y, 1.0F, 1.0F, 1.0F, 1.0F);
        return (WorldMapSymbolsV1.WorldMapTextureSymbolV1)this.symbols.get(this.symbols.size() - 1);
    }

    public int hitTest(float uiX, float uiY) {
        return this.uiSymbols.hitTest(this.ui, uiX, uiY);
    }

    public int getSymbolCount() {
        return this.symbols.size();
    }

    public WorldMapSymbolsV1.WorldMapBaseSymbolV1 getSymbolByIndex(int index) {
        return this.symbols.get(index);
    }

    public void removeSymbolByIndex(int index) {
        this.uiSymbols.removeSymbolByIndex(index);
    }

    public void clear() {
        this.uiSymbols.clear();
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
                WorldMapSymbolsV1.WorldMapTextSymbolV1 symbolV1 = s_textPool.alloc().init(this, textSymbol);
                this.symbols.add(symbolV1);
            }

            if (symbol instanceof WorldMapTextureSymbol textureSymbol) {
                WorldMapSymbolsV1.WorldMapTextureSymbolV1 symbolV1 = s_texturePool.alloc().init(this, textureSymbol);
                this.symbols.add(symbolV1);
            }
        }
    }

    public static void setExposed(LuaManager.Exposer exposer) {
        exposer.setExposed(WorldMapSymbolsAPI.class);
        exposer.setExposed(WorldMapSymbolsV1.class);
        exposer.setExposed(WorldMapSymbolsV1.WorldMapTextSymbolV1.class);
        exposer.setExposed(WorldMapSymbolsV1.WorldMapTextureSymbolV1.class);
    }

    private static final class Listener implements IWorldMapSymbolListener {
        final WorldMapSymbolsV1 api;

        Listener(WorldMapSymbolsV1 api) {
            this.api = api;
        }

        @Override
        public void onAdd(WorldMapBaseSymbol symbol) {
            int index = this.indexOf(symbol);
            if (symbol instanceof WorldMapTextSymbol textSymbol) {
                WorldMapSymbolsV1.WorldMapTextSymbolV1 symbolV1 = WorldMapSymbolsV1.s_textPool.alloc().init(this.api, textSymbol);
                this.api.symbols.add(index, symbolV1);
            } else if (symbol instanceof WorldMapTextureSymbol textureSymbol) {
                WorldMapSymbolsV1.WorldMapTextureSymbolV1 symbolV1 = WorldMapSymbolsV1.s_texturePool.alloc().init(this.api, textureSymbol);
                this.api.symbols.add(index, symbolV1);
            } else {
                throw new RuntimeException("unhandled symbol class " + symbol.getClass().getSimpleName());
            }
        }

        @Override
        public void onBeforeRemove(WorldMapBaseSymbol symbol) {
            int index = this.indexOf(symbol);
            WorldMapSymbolsV1.WorldMapBaseSymbolV1 symbolV1 = this.api.symbols.remove(index);
            symbolV1.release();
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

    protected static class WorldMapBaseSymbolV1 extends PooledObject {
        WorldMapSymbolsV1 owner;
        WorldMapBaseSymbol symbol;

        WorldMapSymbolsV1.WorldMapBaseSymbolV1 init(WorldMapSymbolsV1 owner, WorldMapBaseSymbol symbol) {
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

        public void setAnchor(float x, float y) {
            this.symbol.setAnchor(x, y);
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

        public void setScale(float scale) {
            this.symbol.setScale(scale);
        }

        public boolean isText() {
            return false;
        }

        public boolean isTexture() {
            return false;
        }
    }

    @UsedFromLua
    public static class WorldMapTextSymbolV1 extends WorldMapSymbolsV1.WorldMapBaseSymbolV1 {
        WorldMapTextSymbol textSymbol;

        WorldMapSymbolsV1.WorldMapTextSymbolV1 init(WorldMapSymbolsV1 owner, WorldMapTextSymbol symbol) {
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

        @Override
        public boolean isText() {
            return true;
        }
    }

    @UsedFromLua
    public static class WorldMapTextureSymbolV1 extends WorldMapSymbolsV1.WorldMapBaseSymbolV1 {
        WorldMapTextureSymbol textureSymbol;

        WorldMapSymbolsV1.WorldMapTextureSymbolV1 init(WorldMapSymbolsV1 owner, WorldMapTextureSymbol symbol) {
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
