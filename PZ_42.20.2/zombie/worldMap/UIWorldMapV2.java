// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import zombie.UsedFromLua;
import zombie.worldMap.symbols.WorldMapSymbolsAPI;
import zombie.worldMap.symbols.WorldMapSymbolsV2;

@UsedFromLua
public class UIWorldMapV2 extends UIWorldMapV1 {
    protected WorldMapSymbolsV2 symbolsV2;

    public UIWorldMapV2(UIWorldMap ui) {
        super(ui);
    }

    @Override
    public WorldMapSymbolsAPI getSymbolsAPI() {
        return this.getSymbolsAPIv2();
    }

    public WorldMapSymbolsV2 getSymbolsAPIv2() {
        if (this.symbolsV2 == null) {
            this.symbolsV2 = new WorldMapSymbolsV2(this.ui, this.ui.symbols);
        }

        return this.symbolsV2;
    }

    public boolean isDimUnsharedSymbols() {
        return this.renderer.isDimUnsharedSymbols();
    }
}
