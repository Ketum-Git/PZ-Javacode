// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.entity.energy.EnergyType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;

@UsedFromLua
public class EnergyDefinitionScript extends BaseScriptObject {
    private static Texture defaultIconTexture;
    private static Texture defaultHorizontalBarTexture;
    private static Texture defaultVerticalBarTexture;
    private boolean existsAsVanilla;
    private String modId;
    private EnergyType energyType = EnergyType.None;
    private String energyTypeString;
    private String displayName;
    private final Color color = new Color(1.0F, 1.0F, 1.0F, 1.0F);
    private String iconTextureName;
    private String horizontalBarTextureName;
    private String verticalBarTextureName;
    private Texture iconTexture;
    private Texture horizontalBarTexture;
    private Texture verticalBarTexture;

    public static Texture getDefaultIconTexture() {
        if (defaultIconTexture == null) {
            defaultIconTexture = Texture.getSharedTexture("media/inventory/Question_On.png");
        }

        return defaultIconTexture;
    }

    public static Texture getDefaultHorizontalBarTexture() {
        if (defaultHorizontalBarTexture == null) {
            defaultHorizontalBarTexture = Texture.getSharedTexture("media/ui/Entity/Bars/bars_horz_yellow.png");
        }

        return defaultHorizontalBarTexture;
    }

    public static Texture getDefaultVerticalBarTexture() {
        if (defaultVerticalBarTexture == null) {
            defaultVerticalBarTexture = Texture.getSharedTexture("media/ui/Entity/Bars/bars_vert_yellow.png");
        }

        return defaultVerticalBarTexture;
    }

    protected EnergyDefinitionScript() {
        super(ScriptType.EnergyDefinition);
    }

    public boolean getExistsAsVanilla() {
        return this.existsAsVanilla;
    }

    public boolean isVanilla() {
        return this.modId != null && this.modId.equals("pz-vanilla");
    }

    public String getModID() {
        return this.modId;
    }

    public EnergyType getEnergyType() {
        return this.energyType;
    }

    public String getEnergyTypeString() {
        return this.energyTypeString;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Color getColor() {
        return this.color;
    }

    public Texture getIconTexture() {
        return this.iconTexture;
    }

    public Texture getHorizontalBarTexture() {
        return this.horizontalBarTexture;
    }

    public Texture getVerticalBarTexture() {
        return this.verticalBarTexture;
    }

    @Override
    public void PreReload() {
        super.PreReload();
        this.enabled = true;
        this.existsAsVanilla = false;
        this.modId = null;
        this.energyType = EnergyType.None;
        this.energyTypeString = null;
        this.displayName = null;
        this.color.set(1.0F, 1.0F, 1.0F, 1.0F);
        this.iconTextureName = null;
        this.horizontalBarTextureName = null;
        this.verticalBarTextureName = null;
        this.iconTexture = null;
        this.horizontalBarTexture = null;
        this.verticalBarTexture = null;
    }

    @Override
    public void reset() {
        super.reset();
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);
        if (this.energyType == EnergyType.None && this.energyTypeString == null) {
            throw new Exception("No energy type set.");
        } else {
            if (this.iconTextureName != null) {
                this.iconTexture = Texture.trygetTexture(this.iconTextureName);
            }

            if (this.iconTexture == null) {
                this.iconTexture = getDefaultIconTexture();
            }

            if (this.horizontalBarTextureName != null) {
                this.horizontalBarTexture = Texture.trygetTexture(this.horizontalBarTextureName);
            }

            if (this.horizontalBarTexture == null) {
                this.horizontalBarTexture = getDefaultHorizontalBarTexture();
            }

            if (this.verticalBarTextureName != null) {
                this.verticalBarTexture = Texture.trygetTexture(this.verticalBarTextureName);
            }

            if (this.verticalBarTexture == null) {
                this.verticalBarTexture = getDefaultVerticalBarTexture();
            }
        }
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
        super.OnLoadedAfterLua();
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
        super.OnPostWorldDictionaryInit();
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
        this.modId = ScriptManager.getCurrentLoadFileMod();
        if (this.modId.equals("pz-vanilla")) {
            this.existsAsVanilla = true;
        }
    }

    @Override
    public void Load(String name, String body) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(body);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        if (EnergyType.containsNameLowercase(name)) {
            this.energyType = EnergyType.FromNameLower(name);
        } else {
            this.energyType = EnergyType.Modded;
            this.energyTypeString = name;
        }

        for (ScriptParser.Value value : block.values) {
            String key = value.getKey().trim();
            String val = value.getValue().trim();
            if (!key.isEmpty() && !val.isEmpty()) {
                if (key.equalsIgnoreCase("displayName")) {
                    this.displayName = Translator.getEntityText(val);
                } else if ("r".equalsIgnoreCase(key)) {
                    this.color.r = Float.parseFloat(val);
                } else if ("g".equalsIgnoreCase(key)) {
                    this.color.g = Float.parseFloat(val);
                } else if ("b".equalsIgnoreCase(key)) {
                    this.color.b = Float.parseFloat(val);
                } else if (key.equalsIgnoreCase("color")) {
                    String[] opt = val.split(":");
                    if (opt.length == 3) {
                        this.color.r = Float.parseFloat(opt[0]);
                        this.color.g = Float.parseFloat(opt[1]);
                        this.color.b = Float.parseFloat(opt[2]);
                    }
                } else if (key.equalsIgnoreCase("iconTexture")) {
                    this.iconTextureName = val;
                } else if (key.equalsIgnoreCase("horizontalBarTexture")) {
                    this.horizontalBarTextureName = val;
                } else if (key.equalsIgnoreCase("verticalBarTexture")) {
                    this.verticalBarTextureName = val;
                } else {
                    DebugLog.General.error("Unknown key '" + key + "' val(" + val + ") in energy definition: " + name);
                    if (Core.debug) {
                        throw new Exception("EnergyDefinition error.");
                    }
                }
            }
        }
    }
}
