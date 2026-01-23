// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

import java.util.ArrayList;
import java.util.HashMap;
import org.joml.Quaternionf;
import zombie.popman.ObjectPool;
import zombie.vehicles.BaseVehicle;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.styles.WorldMapStyle;

public final class SymbolsLayoutData {
    static final ObjectPool<SymbolLayout> s_layoutPool = new ObjectPool<>(SymbolLayout::new);
    public final UIWorldMap ui;
    private long modificationCount = -1L;
    public final WorldMapSymbolCollisions collision = new WorldMapSymbolCollisions();
    public float worldScale;
    public final Quaternionf layoutRotation = new Quaternionf();
    public boolean isometric = true;
    public boolean miniMapSymbols;
    final HashMap<WorldMapBaseSymbol, SymbolLayout> symbolToLayout = new HashMap<>();

    public SymbolsLayoutData() {
        this.ui = null;
    }

    public SymbolsLayoutData(UIWorldMap ui) {
        this.ui = ui;
    }

    public boolean getMiniMapSymbols() {
        return this.miniMapSymbols;
    }

    public float getWorldScale() {
        return this.worldScale;
    }

    public void checkLayout() {
        UIWorldMap ui = this.ui;
        WorldMapSymbols symbols = ui.getSymbolsDirect();
        if (symbols != null) {
            Quaternionf q = BaseVehicle.TL_quaternionf_pool.get().alloc().setFromUnnormalized(ui.getAPI().getRenderer().getModelViewMatrix());
            if (this.modificationCount == symbols.getModificationCount()
                && this.worldScale == ui.getAPI().getWorldScale()
                && this.isometric == ui.getAPI().getBoolean("Isometric")
                && this.miniMapSymbols == ui.getAPI().getBoolean("MiniMapSymbols")
                && this.layoutRotation.equals(q)) {
                BaseVehicle.TL_quaternionf_pool.get().release(q);
            } else {
                this.modificationCount = symbols.getModificationCount();
                this.worldScale = ui.getAPI().getWorldScale();
                this.isometric = ui.getAPI().getBoolean("Isometric");
                this.miniMapSymbols = ui.getAPI().getBoolean("MiniMapSymbols");
                this.layoutRotation.set(q);
                BaseVehicle.TL_quaternionf_pool.get().release(q);
                float rox = ui.getAPI().worldOriginX();
                float roy = ui.getAPI().worldOriginY();
                s_layoutPool.releaseAll(new ArrayList<>(this.symbolToLayout.values()));
                this.symbolToLayout.clear();
                this.collision.boxes.clear();
                boolean collided = false;

                for (int i = 0; i < symbols.getSymbolCount(); i++) {
                    WorldMapBaseSymbol symbol = symbols.getSymbolByIndex(i);
                    SymbolLayout layout = this.getLayout(symbol);
                    symbol.layout(ui, this.collision, rox, roy, layout);
                    collided |= layout.collided;
                }

                if (collided) {
                    for (int i = 0; i < symbols.getSymbolCount(); i++) {
                        WorldMapBaseSymbol symbol = symbols.getSymbolByIndex(i);
                        SymbolLayout layout = this.getLayout(symbol);
                        if (!layout.collided && this.collision.isCollision(i)) {
                            layout.collided = true;
                        }
                    }
                }
            }
        }
    }

    SymbolLayout getLayout(WorldMapBaseSymbol symbol) {
        SymbolLayout layout = this.symbolToLayout.get(symbol);
        if (layout == null) {
            layout = s_layoutPool.alloc();
            this.symbolToLayout.put(symbol, layout);
        }

        return layout;
    }

    void initMainThread() {
        s_layoutPool.releaseAll(new ArrayList<>(this.symbolToLayout.values()));
        this.symbolToLayout.clear();
    }

    void initMainThread(WorldMapBaseSymbol originalSymbol, WorldMapBaseSymbol symbolCopy, SymbolsLayoutData originalLayoutData, WorldMapStyle styleCopy) {
        SymbolLayout layout = originalLayoutData.getLayout(originalSymbol);
        SymbolLayout layoutCopy = s_layoutPool.alloc().set(layout);
        if (symbolCopy instanceof WorldMapTextSymbol textSymbol) {
            layoutCopy.textLayout.textLayer = styleCopy.getTextStyleLayerOrDefault(textSymbol.getLayerID());
        }

        this.symbolToLayout.put(symbolCopy, layoutCopy);
    }
}
