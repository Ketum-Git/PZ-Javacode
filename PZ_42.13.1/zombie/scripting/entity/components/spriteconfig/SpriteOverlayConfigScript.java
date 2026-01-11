// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.spriteconfig;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.ComponentType;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.world.ScriptsDictionary;
import zombie.world.scripts.IVersionHash;

@DebugClassFields
public class SpriteOverlayConfigScript extends ComponentScript {
    private final HashMap<String, SpriteOverlayConfigScript.OverlayStyle> allStyles = new HashMap<>();
    private final ArrayList<String> allStyleNames = new ArrayList<>();
    private boolean isValid;

    public SpriteOverlayConfigScript.OverlayStyle getStyle(String styleName) {
        return this.allStyles.get(styleName);
    }

    public ArrayList<String> getAllStyleNames() {
        return this.allStyleNames;
    }

    public boolean isValid() {
        return this.isValid;
    }

    private SpriteOverlayConfigScript() {
        super(ComponentType.SpriteOverlayConfig);
    }

    @Override
    public void getVersion(IVersionHash hash) {
        hash.add(this.getName());
        hash.add(String.valueOf(this.isValid()));
        if (this.isValid()) {
            for (SpriteOverlayConfigScript.OverlayStyle style : this.allStyles.values()) {
                style.getVersion(hash);
            }
        }
    }

    @Override
    public void PreReload() {
        this.allStyles.clear();
        this.allStyleNames.clear();
        this.isValid = false;
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);
        if (loadMode == ScriptLoadMode.Init) {
            ScriptsDictionary.registerScript(this);
        }
    }

    @Override
    protected void copyFrom(ComponentScript componentScript) {
        SpriteOverlayConfigScript other = (SpriteOverlayConfigScript)componentScript;
        this.isValid = other.isValid;
        this.allStyles.clear();

        for (String styleName : other.allStyles.keySet()) {
            this.allStyles.put(styleName, SpriteOverlayConfigScript.OverlayStyle.copy(other.allStyles.get(styleName)));
        }
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        super.load(block);

        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("style")) {
                String styleName = child.id;
                if (this.allStyles.containsKey(styleName)) {
                    DebugLog.General.error("Duplicate style name (not allowed): " + child.id);
                } else {
                    this.loadStyle(styleName, child);
                }
            }
        }

        this.checkScripts();
    }

    private void loadStyle(String styleName, ScriptParser.Block block) {
        SpriteOverlayConfigScript.OverlayStyle style = new SpriteOverlayConfigScript.OverlayStyle();
        style.styleName = styleName;

        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("face") && !style.isSingleFace) {
                int faceID = SpriteConfigManager.GetFaceIdForString(child.id);
                if (faceID == -1) {
                    DebugLog.General.error("Cannot find face for: " + child.id);
                } else {
                    SpriteOverlayConfigScript.FaceScript faceScript = new SpriteOverlayConfigScript.FaceScript();
                    faceScript.faceName = child.id;
                    faceScript.faceId = faceID;
                    this.loadFace(faceScript, child);
                    style.faces[faceID] = faceScript;
                    if (child.id.equalsIgnoreCase("single")) {
                        style.isSingleFace = true;
                    }
                }
            } else {
                DebugLog.General.error("Unknown block '" + child.type + "' in entity iso script: " + this.getName());
            }
        }

        this.allStyles.put(styleName, style);
    }

    private void warnOrError(String msg) {
        if (Core.debug) {
            throw new RuntimeException("[" + this.getName() + "] " + msg);
        } else {
            DebugLog.General.warn("[" + this.getName() + "] " + msg);
        }
    }

    private void checkScripts() {
        for (SpriteOverlayConfigScript.OverlayStyle style : this.allStyles.values()) {
            if (style.isSingleFace) {
                for (int i = 0; i < 6; i++) {
                    SpriteOverlayConfigScript.FaceScript faceScript = style.faces[i];
                    if (faceScript != null && faceScript.faceId != 0) {
                        this.warnOrError("SingleFace has other faces defined.");
                        style.faces[i] = null;
                    }
                }
            }

            boolean hasFace = false;
            int faceCount = 0;

            for (SpriteOverlayConfigScript.FaceScript faceScript : style.faces) {
                if (faceScript != null) {
                    hasFace = true;
                    faceCount++;

                    for (SpriteOverlayConfigScript.ZLayer layer : faceScript.layers) {
                        faceScript.totalHeight = PZMath.max(faceScript.totalHeight, layer.rows.size());

                        for (SpriteOverlayConfigScript.XRow row : layer.rows) {
                            faceScript.totalWidth = PZMath.max(faceScript.totalWidth, row.tiles.size());

                            for (SpriteOverlayConfigScript.TileScript tileScript : row.tiles) {
                                if (tileScript.tileName != null && !style.allTileNames.contains(tileScript.tileName)) {
                                    style.allTileNames.add(tileScript.tileName);
                                }
                            }
                        }
                    }

                    if (faceCount > 1 || faceScript.totalHeight > 1 || faceScript.totalWidth > 1) {
                        style.isMultiTile = true;
                    }
                }
            }

            for (SpriteOverlayConfigScript.FaceScript faceScriptx : style.faces) {
                if (faceScriptx != null) {
                    int complementaryFace = -1;
                    switch (faceScriptx.faceId) {
                        case 0:
                            complementaryFace = 4;
                            break;
                        case 1:
                            complementaryFace = 5;
                        case 2:
                        case 3:
                        default:
                            break;
                        case 4:
                            complementaryFace = 0;
                            break;
                        case 5:
                            complementaryFace = 1;
                    }

                    if (complementaryFace != -1
                        && style.getFace(complementaryFace) != null
                        && (
                            faceScriptx.totalWidth != style.getFace(complementaryFace).totalWidth
                                || faceScriptx.totalHeight != style.getFace(complementaryFace).totalHeight
                        )) {
                        DebugLog.General.error("Complementary faces are different sizes. This is not supported. In entity iso script: " + this.getName());
                    }
                }
            }

            if (hasFace) {
                style.isValid = true;
            }
        }

        this.isValid = true;

        for (SpriteOverlayConfigScript.OverlayStyle style : this.allStyles.values()) {
            this.allStyleNames.add(style.styleName);
            if (!style.isValid()) {
                this.isValid = false;
            }
        }
    }

    private void loadFace(SpriteOverlayConfigScript.FaceScript faceScript, ScriptParser.Block block) {
        SpriteOverlayConfigScript.ZLayer layer = new SpriteOverlayConfigScript.ZLayer();
        this.loadLayer(layer, block);
        if (!layer.rows.isEmpty()) {
            faceScript.layers.add(layer);
        } else {
            for (ScriptParser.Block child : block.children) {
                if (child.type.equalsIgnoreCase("layer")) {
                    this.loadLayer(layer, child);
                    faceScript.layers.add(layer);
                    layer = new SpriteOverlayConfigScript.ZLayer();
                }
            }
        }
    }

    private void loadLayer(SpriteOverlayConfigScript.ZLayer layer, ScriptParser.Block block) {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.equalsIgnoreCase("row")) {
                SpriteOverlayConfigScript.XRow row = new SpriteOverlayConfigScript.XRow();
                String[] tiles = val.split("\\s+");

                for (String tileName : tiles) {
                    SpriteOverlayConfigScript.TileScript tileScript = new SpriteOverlayConfigScript.TileScript();
                    if (!tileName.equalsIgnoreCase("true") && !tileName.equalsIgnoreCase("false")) {
                        tileScript.tileName = tileName;
                        tileScript.isEmptySpace = false;
                        tileScript.blocksSquare = true;
                    } else {
                        boolean b = tileName.equalsIgnoreCase("true");
                        tileScript.isEmptySpace = true;
                        tileScript.blocksSquare = b;
                    }

                    row.tiles.add(tileScript);
                }

                layer.rows.add(row);
            }
        }
    }

    @DebugClassFields
    public static class FaceScript {
        private String faceName;
        private int faceId = -1;
        private int totalWidth;
        private int totalHeight;
        private final ArrayList<SpriteOverlayConfigScript.ZLayer> layers = new ArrayList<>();

        public String getFaceName() {
            return this.faceName.toLowerCase();
        }

        public int getTotalWidth() {
            return this.totalWidth;
        }

        public int getTotalHeight() {
            return this.totalHeight;
        }

        public int getZLayers() {
            return this.layers.size();
        }

        public SpriteOverlayConfigScript.ZLayer getLayer(int z) {
            return this.layers.get(z);
        }

        protected int getFaceID() {
            return this.faceId;
        }

        private void getVersion(IVersionHash hash) {
            hash.add(this.faceName);
            hash.add(String.valueOf(this.layers.size()));

            for (SpriteOverlayConfigScript.ZLayer layer : this.layers) {
                layer.getVersion(hash);
            }
        }

        private static SpriteOverlayConfigScript.FaceScript copy(SpriteOverlayConfigScript.FaceScript other) {
            SpriteOverlayConfigScript.FaceScript script = new SpriteOverlayConfigScript.FaceScript();
            script.faceName = other.faceName;
            script.faceId = other.faceId;
            script.totalWidth = other.totalWidth;
            script.totalHeight = other.totalHeight;

            for (SpriteOverlayConfigScript.ZLayer layer : other.layers) {
                script.layers.add(SpriteOverlayConfigScript.ZLayer.copy(layer));
            }

            return script;
        }
    }

    @DebugClassFields
    public static class OverlayStyle {
        private String styleName;
        private final SpriteOverlayConfigScript.FaceScript[] faces = new SpriteOverlayConfigScript.FaceScript[6];
        private boolean isSingleFace;
        private boolean isMultiTile;
        private boolean isValid;
        private final ArrayList<String> allTileNames = new ArrayList<>();

        public String getStyleName() {
            return this.styleName;
        }

        public SpriteOverlayConfigScript.FaceScript getFace(int id) {
            return this.faces[id];
        }

        public ArrayList<String> getAllTileNames() {
            return this.allTileNames;
        }

        public boolean isSingleFace() {
            return this.isSingleFace;
        }

        public boolean isMultiTile() {
            return this.isMultiTile;
        }

        public boolean isValid() {
            return this.isValid;
        }

        private void getVersion(IVersionHash hash) {
            hash.add(this.styleName);

            for (SpriteOverlayConfigScript.FaceScript face : this.faces) {
                if (face != null) {
                    face.getVersion(hash);
                }
            }
        }

        private static SpriteOverlayConfigScript.OverlayStyle copy(SpriteOverlayConfigScript.OverlayStyle other) {
            SpriteOverlayConfigScript.OverlayStyle style = new SpriteOverlayConfigScript.OverlayStyle();
            style.styleName = other.styleName;
            style.isSingleFace = other.isSingleFace;
            style.isMultiTile = other.isMultiTile;
            style.isValid = other.isValid;
            style.allTileNames.addAll(other.allTileNames);

            for (int i = 0; i < 6; i++) {
                style.faces[i] = null;
                if (other.faces[i] != null) {
                    style.faces[i] = SpriteOverlayConfigScript.FaceScript.copy(other.faces[i]);
                }
            }

            return style;
        }
    }

    @DebugClassFields
    public static class TileScript {
        private String tileName;
        private boolean isEmptySpace;
        private boolean blocksSquare;

        public String getTileName() {
            return this.tileName;
        }

        public boolean isEmptySpace() {
            return this.isEmptySpace;
        }

        public boolean isBlocksSquare() {
            return this.blocksSquare;
        }

        private void getVersion(IVersionHash hash) {
            if (this.tileName != null) {
                hash.add(this.tileName);
            }

            hash.add(String.valueOf(this.isEmptySpace));
            hash.add(String.valueOf(this.blocksSquare));
        }

        private static SpriteOverlayConfigScript.TileScript copy(SpriteOverlayConfigScript.TileScript other) {
            SpriteOverlayConfigScript.TileScript script = new SpriteOverlayConfigScript.TileScript();
            script.tileName = other.tileName;
            script.isEmptySpace = other.isEmptySpace;
            script.blocksSquare = other.blocksSquare;
            return script;
        }
    }

    @DebugClassFields
    public static class XRow {
        private final ArrayList<SpriteOverlayConfigScript.TileScript> tiles = new ArrayList<>();

        public int getWidth() {
            return this.tiles.size();
        }

        public SpriteOverlayConfigScript.TileScript getTile(int x) {
            return this.tiles.get(x);
        }

        private void getVersion(IVersionHash hash) {
            hash.add(String.valueOf(this.getWidth()));

            for (SpriteOverlayConfigScript.TileScript tile : this.tiles) {
                tile.getVersion(hash);
            }
        }

        private static SpriteOverlayConfigScript.XRow copy(SpriteOverlayConfigScript.XRow other) {
            SpriteOverlayConfigScript.XRow row = new SpriteOverlayConfigScript.XRow();

            for (SpriteOverlayConfigScript.TileScript tile : other.tiles) {
                row.tiles.add(SpriteOverlayConfigScript.TileScript.copy(tile));
            }

            return row;
        }
    }

    @DebugClassFields
    public static class ZLayer {
        private final ArrayList<SpriteOverlayConfigScript.XRow> rows = new ArrayList<>();

        public int getHeight() {
            return this.rows.size();
        }

        public SpriteOverlayConfigScript.XRow getRow(int y) {
            return this.rows.get(y);
        }

        private void getVersion(IVersionHash hash) {
            hash.add(String.valueOf(this.getHeight()));

            for (SpriteOverlayConfigScript.XRow row : this.rows) {
                row.getVersion(hash);
            }
        }

        private static SpriteOverlayConfigScript.ZLayer copy(SpriteOverlayConfigScript.ZLayer other) {
            SpriteOverlayConfigScript.ZLayer layer = new SpriteOverlayConfigScript.ZLayer();

            for (SpriteOverlayConfigScript.XRow row : other.rows) {
                layer.rows.add(SpriteOverlayConfigScript.XRow.copy(row));
            }

            return layer;
        }
    }
}
