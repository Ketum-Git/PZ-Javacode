// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.spriteconfig;

import java.util.ArrayList;
import zombie.UsedFromLua;
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
@UsedFromLua
public class SpriteConfigScript extends ComponentScript {
    private final SpriteConfigScript.FaceScript[] faces = new SpriteConfigScript.FaceScript[6];
    private boolean isSingleFace;
    private boolean isMultiTile;
    private boolean isValid;
    private boolean isProp;
    private final ArrayList<String> allTileNames = new ArrayList<>();
    private String cornerSprite;
    private int health = -1;
    private int skillBaseHealth = 20;
    private boolean isThumpable = true;
    private int lightRadius;
    private String lightsourceItem;
    private String lightsourceFuel;
    private String debugItem;
    private ArrayList<String> lightsourceTagItem;
    private final String fuel = null;
    private String breakSound = "BreakObject";
    private boolean dontNeedFrame;
    private boolean needWindowFrame;
    private boolean isPole;
    private final ArrayList<String> previousStage = new ArrayList<>();
    private int bonusHealth;
    private String onCreate;
    private String onIsValid;
    private String timedActionOnIsValid;
    private boolean needToBeAgainstWall;
    private boolean canBePadlocked;

    public SpriteConfigScript.FaceScript getFace(int id) {
        return this.faces[id];
    }

    public String getCornerSprite() {
        return this.cornerSprite;
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

    public boolean isProp() {
        return this.isProp;
    }

    public int getHealth() {
        return this.health;
    }

    public int getSkillBaseHealth() {
        return this.skillBaseHealth;
    }

    public boolean getIsThumpable() {
        return this.isThumpable;
    }

    public boolean getCanBePadlocked() {
        return this.canBePadlocked;
    }

    public String getBreakSound() {
        return this.breakSound;
    }

    public boolean getDontNeedFrame() {
        return this.dontNeedFrame;
    }

    public int getLightRadius() {
        return this.lightRadius;
    }

    public String getLightsourceItem() {
        return this.lightsourceItem;
    }

    public String getLightsourceFuel() {
        return this.lightsourceFuel;
    }

    public String getDebugItem() {
        return this.debugItem;
    }

    public ArrayList<String> getLightsourceTagItem() {
        return this.lightsourceTagItem;
    }

    public ArrayList<String> getPreviousStages() {
        return this.previousStage;
    }

    public int getBonusHealth() {
        return this.bonusHealth;
    }

    public String getOnCreate() {
        return this.onCreate;
    }

    public String getOnIsValid() {
        return this.onIsValid;
    }

    public String getTimedActionOnIsValid() {
        return this.timedActionOnIsValid;
    }

    private SpriteConfigScript() {
        super(ComponentType.SpriteConfig);
    }

    @Override
    public boolean isoMasterOnly() {
        return false;
    }

    @Override
    public void getVersion(IVersionHash hash) {
        hash.add(this.getName());
        hash.add(String.valueOf(this.isValid()));
        if (this.isValid()) {
            for (SpriteConfigScript.FaceScript face : this.faces) {
                if (face != null) {
                    face.getVersion(hash);
                }
            }
        }
    }

    @Override
    public void PreReload() {
        for (int i = 0; i < this.faces.length; i++) {
            this.faces[i] = null;
        }

        this.cornerSprite = null;
        this.isSingleFace = false;
        this.isMultiTile = false;
        this.isValid = false;
        this.allTileNames.clear();
        this.isProp = false;
        this.health = -1;
        this.skillBaseHealth = 0;
        this.isThumpable = true;
        this.breakSound = "BreakObject";
        this.dontNeedFrame = false;
        this.needWindowFrame = false;
        this.isPole = false;
        this.needToBeAgainstWall = false;
        this.previousStage.clear();
        this.bonusHealth = 0;
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
        throw new RuntimeException("Unfinished Todo");
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        super.load(block);

        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && val.isEmpty()) {
            }
        }

        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("face") && !this.isSingleFace) {
                int faceID = SpriteConfigManager.GetFaceIdForString(child.id);
                if (faceID == -1) {
                    DebugLog.General.error("Cannot find face for: " + child.id);
                    continue;
                }

                SpriteConfigScript.FaceScript faceScript = new SpriteConfigScript.FaceScript();
                faceScript.faceName = child.id;
                faceScript.faceId = faceID;
                this.loadFace(faceScript, child);
                this.faces[faceID] = faceScript;
                if (child.id.equalsIgnoreCase("single")) {
                    this.isSingleFace = true;
                }
            } else {
                DebugLog.General.error("Unknown block '" + child.type + "' in entity iso script: " + this.getName());
            }

            for (ScriptParser.Value valuex : block.values) {
                String k = valuex.getKey().trim();
                String v = valuex.getValue().trim();
                if (k.equalsIgnoreCase("health")) {
                    this.health = PZMath.tryParseInt(v, -1);
                } else if (k.equalsIgnoreCase("isProp")) {
                    this.isProp = Boolean.parseBoolean(v);
                } else if (k.equalsIgnoreCase("skillBaseHealth")) {
                    this.skillBaseHealth = PZMath.tryParseInt(v, 0);
                } else if (k.equalsIgnoreCase("lightRadius")) {
                    this.lightRadius = PZMath.tryParseInt(v, 10);
                } else if (k.equalsIgnoreCase("lightsourceItem")) {
                    if (v.contains("tags")) {
                        this.lightsourceTagItem = new ArrayList<>();
                        v = v.replace("tags[", "").replace("]", "");
                        String[] split = v.split(";");

                        for (int i = 0; i < split.length; i++) {
                            this.lightsourceTagItem.add(split[i].trim());
                        }
                    } else {
                        this.lightsourceItem = v;
                    }
                } else if (k.equalsIgnoreCase("lightsourceFuel")) {
                    this.lightsourceFuel = v;
                } else if (k.equalsIgnoreCase("debugItem")) {
                    this.debugItem = v;
                } else if (k.equalsIgnoreCase("OnCreate")) {
                    this.onCreate = v;
                } else if (k.equalsIgnoreCase("OnIsValid")) {
                    this.onIsValid = v;
                } else if (k.equalsIgnoreCase("TimedActionOnIsValid")) {
                    this.timedActionOnIsValid = v;
                } else if (k.equalsIgnoreCase("isThumpable")) {
                    this.isThumpable = v.equalsIgnoreCase("true");
                } else if (k.equalsIgnoreCase("breakSound")) {
                    this.breakSound = v;
                } else if (k.equalsIgnoreCase("corner")) {
                    this.cornerSprite = v;
                } else if (k.equalsIgnoreCase("dontNeedFrame")) {
                    this.dontNeedFrame = v.equalsIgnoreCase("true");
                } else if (k.equalsIgnoreCase("needWindowFrame")) {
                    this.needWindowFrame = v.equalsIgnoreCase("true");
                } else if (k.equalsIgnoreCase("isPole")) {
                    this.isPole = v.equalsIgnoreCase("true");
                } else if (k.equalsIgnoreCase("needToBeAgainstWall")) {
                    this.needToBeAgainstWall = Boolean.parseBoolean(v);
                } else if (k.equalsIgnoreCase("canBePadlocked")) {
                    this.canBePadlocked = Boolean.parseBoolean(v);
                } else if (k.equalsIgnoreCase("previousStage")) {
                    String[] stages = v.split(";");

                    for (int j = 0; j < stages.length; j++) {
                        this.previousStage.add(stages[j]);
                    }
                } else if (k.equalsIgnoreCase("bonusHealth")) {
                    this.bonusHealth = PZMath.tryParseInt(v, 0);
                }
            }
        }

        this.checkScripts();
    }

    private void warnOrError(String msg) {
        if (Core.debug) {
            throw new RuntimeException("[" + this.getName() + "] " + msg);
        } else {
            DebugLog.General.warn("[" + this.getName() + "] " + msg);
        }
    }

    private void checkScripts() {
        if (this.isSingleFace) {
            for (int i = 0; i < 6; i++) {
                SpriteConfigScript.FaceScript faceScript = this.faces[i];
                if (faceScript != null && faceScript.faceId != 0) {
                    this.warnOrError("SingleFace has other faces defined.");
                    this.faces[i] = null;
                }
            }
        }

        boolean hasFace = false;
        boolean hasDuplicates = false;
        int faceCount = 0;

        for (SpriteConfigScript.FaceScript faceScript : this.faces) {
            if (faceScript != null) {
                hasFace = true;
                faceCount++;

                for (SpriteConfigScript.ZLayer layer : faceScript.layers) {
                    faceScript.totalHeight = PZMath.max(faceScript.totalHeight, layer.rows.size());

                    for (SpriteConfigScript.XRow row : layer.rows) {
                        faceScript.totalWidth = PZMath.max(faceScript.totalWidth, row.tiles.size());

                        for (SpriteConfigScript.TileScript tileScript : row.tiles) {
                            if (tileScript.tileName != null) {
                                if (this.allTileNames.contains(tileScript.tileName)) {
                                    this.warnOrError("Tile duplicate: " + tileScript.tileName);
                                    hasDuplicates = true;
                                } else {
                                    this.allTileNames.add(tileScript.tileName);
                                }
                            }
                        }
                    }
                }

                if (faceCount > 1 || faceScript.totalHeight > 1 || faceScript.totalWidth > 1) {
                    this.isMultiTile = true;
                }
            }
        }

        for (SpriteConfigScript.FaceScript faceScriptx : this.faces) {
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
                    && this.getFace(complementaryFace) != null
                    && (
                        faceScriptx.totalWidth != this.getFace(complementaryFace).totalWidth
                            || faceScriptx.totalHeight != this.getFace(complementaryFace).totalHeight
                    )) {
                    DebugLog.General.error("Complementary faces are different sizes. This is not supported. In entity iso script: " + this.getName());
                }
            }
        }

        if (hasFace && !hasDuplicates) {
            this.isValid = true;
        }
    }

    private void loadFace(SpriteConfigScript.FaceScript faceScript, ScriptParser.Block block) {
        SpriteConfigScript.ZLayer layer = new SpriteConfigScript.ZLayer();
        this.loadLayer(layer, block);
        if (!layer.rows.isEmpty()) {
            faceScript.layers.add(layer);
        } else {
            for (ScriptParser.BlockElement element : block.elements) {
                if (element.asValue() != null) {
                    String s = element.asValue().string;
                    if (!s.trim().isEmpty() && s.contains("=")) {
                        String[] split = s.split("=");
                        String k = split[0].trim();
                        String v = split[1].trim();
                        if (k.equalsIgnoreCase("lightOffsetX")) {
                            faceScript.lightsourceOffsetX = Integer.parseInt(v);
                        }

                        if (k.equalsIgnoreCase("lightOffsetY")) {
                            faceScript.lightsourceOffsetY = Integer.parseInt(v);
                        }

                        if (k.equalsIgnoreCase("lightOffsetZ")) {
                            faceScript.lightsourceOffsetZ = Integer.parseInt(v);
                        }
                    }
                }
            }

            for (ScriptParser.Block child : block.children) {
                if (child.type.equalsIgnoreCase("layer")) {
                    this.loadLayer(layer, child);
                    faceScript.layers.add(layer);
                    layer = new SpriteConfigScript.ZLayer();
                }
            }
        }
    }

    private void loadLayer(SpriteConfigScript.ZLayer layer, ScriptParser.Block block) {
        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (key.equalsIgnoreCase("row")) {
                SpriteConfigScript.XRow row = new SpriteConfigScript.XRow();
                String[] tiles = val.split("\\s+");

                for (String tileName : tiles) {
                    SpriteConfigScript.TileScript tileScript = new SpriteConfigScript.TileScript();
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

    public boolean getNeedWindowFrame() {
        return this.needWindowFrame;
    }

    public boolean getNeedToBeAgainstWall() {
        return this.needToBeAgainstWall;
    }

    public boolean isPole() {
        return this.isPole;
    }

    @DebugClassFields
    @UsedFromLua
    public static class FaceScript {
        private String faceName;
        private int faceId = -1;
        private int totalWidth;
        private int totalHeight;
        private int lightsourceOffsetX;
        private int lightsourceOffsetY;
        private int lightsourceOffsetZ;
        private final ArrayList<SpriteConfigScript.ZLayer> layers = new ArrayList<>();

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

        public SpriteConfigScript.ZLayer getLayer(int z) {
            return this.layers.get(z);
        }

        public int getLightsourceOffsetX() {
            return this.lightsourceOffsetX;
        }

        public int getLightsourceOffsetY() {
            return this.lightsourceOffsetY;
        }

        public int getLightsourceOffsetZ() {
            return this.lightsourceOffsetZ;
        }

        private void getVersion(IVersionHash hash) {
            hash.add(this.faceName);
            hash.add(String.valueOf(this.layers.size()));

            for (SpriteConfigScript.ZLayer layer : this.layers) {
                layer.getVersion(hash);
            }
        }
    }

    @DebugClassFields
    @UsedFromLua
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
    }

    @DebugClassFields
    @UsedFromLua
    public static class XRow {
        private final ArrayList<SpriteConfigScript.TileScript> tiles = new ArrayList<>();

        public int getWidth() {
            return this.tiles.size();
        }

        public SpriteConfigScript.TileScript getTile(int x) {
            return this.tiles.get(x);
        }

        private void getVersion(IVersionHash hash) {
            hash.add(String.valueOf(this.getWidth()));

            for (SpriteConfigScript.TileScript tile : this.tiles) {
                tile.getVersion(hash);
            }
        }
    }

    @DebugClassFields
    @UsedFromLua
    public static class ZLayer {
        private final ArrayList<SpriteConfigScript.XRow> rows = new ArrayList<>();

        public int getHeight() {
            return this.rows.size();
        }

        public SpriteConfigScript.XRow getRow(int y) {
            return this.rows.get(y);
        }

        private void getVersion(IVersionHash hash) {
            hash.add(String.valueOf(this.getHeight()));

            for (SpriteConfigScript.XRow row : this.rows) {
                row.getVersion(hash);
            }
        }
    }
}
