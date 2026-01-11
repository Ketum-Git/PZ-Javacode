// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

import java.util.ArrayList;
import org.joml.Matrix4f;
import zombie.core.Core;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.vehicles.BaseVehicle;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapRenderer;
import zombie.worldMap.styles.WorldMapStyle;

public final class SymbolsRenderData {
    final ArrayList<WorldMapBaseSymbol> symbols = new ArrayList<>();
    boolean miniMapSymbols;
    boolean mapEditor;
    boolean userEditing;

    public void renderMain(UIWorldMap ui, WorldMapSymbols symbols, SymbolsLayoutData layoutData, WorldMapStyle styleCopy) {
        this.symbols.clear();
        this.miniMapSymbols = ui.getSymbolsLayoutData().getMiniMapSymbols();
        this.mapEditor = ui.isMapEditor();
        this.userEditing = ui.getSymbolsDirect() != null && ui.getSymbolsDirect().isUserEditing();
        if (symbols != null) {
            ui.checkSymbolsLayout();
            layoutData.initMainThread();

            for (int i = 0; i < symbols.getSymbolCount(); i++) {
                WorldMapBaseSymbol symbol = symbols.getSymbolByIndex(i);
                if (symbols.isSymbolVisible(ui, symbol) && symbol.isOnScreen(ui)) {
                    if (Core.debug) {
                    }

                    WorldMapBaseSymbol symbolCopy = symbol.createCopy();
                    this.symbols.add(symbolCopy);
                    layoutData.initMainThread(symbol, symbolCopy, ui.getSymbolsLayoutData(), styleCopy);
                }
            }
        }
    }

    public void render(WorldMapRenderer.Drawer drawer, boolean bUserDefined) {
        VBORenderer.getInstance().flush();
        Matrix4f proj = BaseVehicle.allocMatrix4f();
        proj.setOrtho2D(0.0F, drawer.width, drawer.height, 0.0F);
        Matrix4f view = BaseVehicle.allocMatrix4f();
        view.identity();
        PZGLUtil.pushAndLoadMatrix(5889, proj);
        PZGLUtil.pushAndLoadMatrix(5888, view);
        BaseVehicle.releaseMatrix4f(proj);
        BaseVehicle.releaseMatrix4f(view);

        for (int i = 0; i < this.symbols.size(); i++) {
            WorldMapBaseSymbol symbol = this.symbols.get(i);
            if (symbol.isUserDefined() == bUserDefined) {
                symbol.render(drawer);
            }
        }

        VBORenderer.getInstance().flush();
        PZGLUtil.popMatrix(5889);
        PZGLUtil.popMatrix(5888);
    }

    public void postRender() {
        for (int i = 0; i < this.symbols.size(); i++) {
            WorldMapBaseSymbol symbol = this.symbols.get(i);
            symbol.release();
        }

        this.symbols.clear();
    }
}
