// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import zombie.GameWindow;

public final class SymbolSaveData {
    public int worldVersion;
    public int symbolsVersion;
    public final HashMap<String, Integer> fontNameToIndex = new HashMap<>();
    public final HashMap<Integer, String> indexToFontName = new HashMap<>();

    public SymbolSaveData(int WorldVersion, int SymbolsVersion) {
        this.worldVersion = WorldVersion;
        this.symbolsVersion = SymbolsVersion;
    }

    public void load(ByteBuffer input) throws IOException {
        int numFonts = input.get() & 255;

        for (int i = 0; i < numFonts; i++) {
            String fontID = GameWindow.ReadString(input);
            this.indexToFontName.put(i, fontID);
            this.fontNameToIndex.put(fontID, i);
        }
    }

    public void save(ByteBuffer output, WorldMapSymbols symbols) throws IOException {
        String[] fonts = this.initFontLookup(symbols);
        output.put((byte)fonts.length);

        for (int i = 0; i < fonts.length; i++) {
            String fontID = fonts[i];
            GameWindow.WriteString(output, fontID);
        }
    }

    String[] initFontLookup(WorldMapSymbols symbols) {
        HashSet<String> fontSet = new HashSet<>();

        for (int i = 0; i < symbols.getSymbolCount(); i++) {
            if (symbols.getSymbolByIndex(i) instanceof WorldMapTextSymbol textSymbol) {
                fontSet.add(textSymbol.getLayerID());
            }
        }

        String[] fonts = fontSet.toArray(new String[0]);

        for (int ix = 0; ix < fonts.length; ix++) {
            String fontID = fonts[ix];
            this.indexToFontName.put(ix, fontID);
            this.fontNameToIndex.put(fontID, ix);
        }

        return fonts;
    }

    public void save(ByteBuffer output, WorldMapBaseSymbol symbol) throws IOException {
        this.worldVersion = 241;
        this.symbolsVersion = 2;
        String[] fonts = new String[0];
        if (symbol instanceof WorldMapTextSymbol textSymbol) {
            String fontID = textSymbol.getLayerID();
            this.indexToFontName.put(0, fontID);
            this.fontNameToIndex.put(fontID, 0);
            fonts = new String[]{textSymbol.getLayerID()};
        }

        output.put((byte)fonts.length);

        for (int i = 0; i < fonts.length; i++) {
            String fontName = fonts[i];
            GameWindow.WriteString(output, fontName);
        }
    }
}
