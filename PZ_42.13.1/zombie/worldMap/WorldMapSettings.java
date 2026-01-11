// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.util.ArrayList;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.config.BooleanConfigOption;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.core.Core;

@UsedFromLua
public final class WorldMapSettings {
    public static final int VERSION1 = 1;
    public static final int VERSION = 1;
    private static WorldMapSettings instance;
    final ArrayList<ConfigOption> options = new ArrayList<>();
    final WorldMapSettings.WorldMap mWorldMap = new WorldMapSettings.WorldMap();
    final WorldMapSettings.MiniMap mMiniMap = new WorldMapSettings.MiniMap();
    private int readVersion;

    public static WorldMapSettings getInstance() {
        if (instance == null) {
            instance = new WorldMapSettings();
            instance.load();
        }

        return instance;
    }

    private BooleanConfigOption newOption(String name, boolean defaultValue) {
        BooleanConfigOption option = new BooleanConfigOption(name, defaultValue);
        this.options.add(option);
        return option;
    }

    private DoubleConfigOption newOption(String name, double min, double max, double defaultValue) {
        DoubleConfigOption option = new DoubleConfigOption(name, min, max, defaultValue);
        this.options.add(option);
        return option;
    }

    public ConfigOption getOptionByName(String name) {
        for (int i = 0; i < this.options.size(); i++) {
            ConfigOption setting = this.options.get(i);
            if (setting.getName().equals(name)) {
                return setting;
            }
        }

        return null;
    }

    public int getOptionCount() {
        return this.options.size();
    }

    public ConfigOption getOptionByIndex(int index) {
        return this.options.get(index);
    }

    public void setBoolean(String name, boolean value) {
        if (this.getOptionByName(name) instanceof BooleanConfigOption booleanConfigOption) {
            booleanConfigOption.setValue(value);
        }
    }

    public boolean getBoolean(String name) {
        return this.getOptionByName(name) instanceof BooleanConfigOption booleanConfigOption ? booleanConfigOption.getValue() : false;
    }

    public void setDouble(String name, double value) {
        if (this.getOptionByName(name) instanceof DoubleConfigOption doubleConfigOption) {
            doubleConfigOption.setValue(value);
        }
    }

    public double getDouble(String name, double defaultValue) {
        return this.getOptionByName(name) instanceof DoubleConfigOption doubleConfigOption ? doubleConfigOption.getValue() : defaultValue;
    }

    public int getFileVersion() {
        return this.readVersion;
    }

    public void save() {
        if (!Core.getInstance().isNoSave()) {
            String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("InGameMap.ini");
            ConfigFile configFile = new ConfigFile();
            configFile.write(fileName, 1, this.options);
            this.readVersion = 1;
        }
    }

    public void load() {
        this.readVersion = 0;
        String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("InGameMap.ini");
        ConfigFile configFile = new ConfigFile();
        if (configFile.read(fileName)) {
            this.readVersion = configFile.getVersion();
            if (this.readVersion >= 1 && this.readVersion <= 1) {
                for (int i = 0; i < configFile.getOptions().size(); i++) {
                    ConfigOption configOption = configFile.getOptions().get(i);

                    try {
                        ConfigOption myOption = this.getOptionByName(configOption.getName());
                        if (myOption != null) {
                            myOption.parse(configOption.getValueAsString());
                        }
                    } catch (Exception var6) {
                    }
                }
            }
        }
    }

    public static void Reset() {
        if (instance != null) {
            instance.options.clear();
            instance = null;
        }
    }

    public class MiniMap {
        public DoubleConfigOption zoom;
        public BooleanConfigOption isometric;
        public BooleanConfigOption showSymbols;
        public BooleanConfigOption startVisible;
        public BooleanConfigOption terrainImage;

        public MiniMap() {
            Objects.requireNonNull(WorldMapSettings.this);
            super();
            this.zoom = WorldMapSettings.this.newOption("MiniMap.Zoom", 0.0, 24.0, 19.0);
            this.isometric = WorldMapSettings.this.newOption("MiniMap.Isometric", true);
            this.showSymbols = WorldMapSettings.this.newOption("MiniMap.ShowSymbols", false);
            this.startVisible = WorldMapSettings.this.newOption("MiniMap.StartVisible", true);
            this.terrainImage = WorldMapSettings.this.newOption("MiniMap.TerrainImage", false);
        }
    }

    public final class WorldMap {
        public DoubleConfigOption centerX;
        public DoubleConfigOption centerY;
        public BooleanConfigOption highlightStreet;
        public BooleanConfigOption isometric;
        public BooleanConfigOption largeStreetLabel;
        public BooleanConfigOption placeNames;
        public BooleanConfigOption players;
        public BooleanConfigOption showPrintMedia;
        public BooleanConfigOption showStreetNames;
        public BooleanConfigOption showSymbolsUi;
        public BooleanConfigOption symbols;
        public BooleanConfigOption terrainImage;
        public DoubleConfigOption zoom;

        public WorldMap() {
            Objects.requireNonNull(WorldMapSettings.this);
            super();
            this.centerX = WorldMapSettings.this.newOption("WorldMap.CenterX", -Double.MAX_VALUE, Double.MAX_VALUE, 0.0);
            this.centerY = WorldMapSettings.this.newOption("WorldMap.CenterY", -Double.MAX_VALUE, Double.MAX_VALUE, 0.0);
            this.highlightStreet = WorldMapSettings.this.newOption("WorldMap.HighlightStreet", true);
            this.isometric = WorldMapSettings.this.newOption("WorldMap.Isometric", true);
            this.largeStreetLabel = WorldMapSettings.this.newOption("WorldMap.LargeStreetLabel", true);
            this.placeNames = WorldMapSettings.this.newOption("WorldMap.PlaceNames", true);
            this.players = WorldMapSettings.this.newOption("WorldMap.Players", true);
            this.showPrintMedia = WorldMapSettings.this.newOption("WorldMap.ShowPrintMedia", false);
            this.showStreetNames = WorldMapSettings.this.newOption("WorldMap.ShowStreetNames", true);
            this.showSymbolsUi = WorldMapSettings.this.newOption("WorldMap.ShowSymbolsUI", true);
            this.symbols = WorldMapSettings.this.newOption("WorldMap.Symbols", true);
            this.terrainImage = WorldMapSettings.this.newOption("WorldMap.TerrainImage", false);
            this.zoom = WorldMapSettings.this.newOption("WorldMap.Zoom", 0.0, 24.0, 0.0);
        }
    }
}
