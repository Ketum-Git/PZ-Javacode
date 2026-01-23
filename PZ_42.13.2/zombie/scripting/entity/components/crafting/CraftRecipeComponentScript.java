// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.entity.components.crafting;

import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.entity.ComponentType;
import zombie.scripting.ScriptLoadMode;
import zombie.scripting.ScriptParser;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.entity.components.ui.UiConfigScript;
import zombie.scripting.objects.CraftRecipeTag;
import zombie.scripting.ui.XuiManager;
import zombie.scripting.ui.XuiSkin;

@UsedFromLua
public class CraftRecipeComponentScript extends ComponentScript {
    private CraftRecipe craftRecipe;

    private CraftRecipeComponentScript() {
        super(ComponentType.CraftRecipe);
    }

    @Override
    protected void copyFrom(ComponentScript other) {
    }

    @Override
    public boolean isoMasterOnly() {
        return true;
    }

    @Override
    public void PreReload() {
        this.craftRecipe = null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
    }

    @Override
    public void OnScriptsLoaded(ScriptLoadMode loadMode) throws Exception {
        super.OnScriptsLoaded(loadMode);
        this.craftRecipe.OnScriptsLoaded(loadMode);
    }

    @Override
    public void OnLoadedAfterLua() throws Exception {
        this.craftRecipe.OnLoadedAfterLua();
        this.craftRecipe.overrideTranslationName(this.getTranslationName());
        this.craftRecipe.overrideIconTexture(this.getIconTexture());
    }

    @Override
    public void OnPostWorldDictionaryInit() throws Exception {
        this.craftRecipe.OnPostWorldDictionaryInit();
    }

    @Override
    protected void load(ScriptParser.Block block) throws Exception {
        this.craftRecipe = new CraftRecipe();
        this.craftRecipe.InitLoadPP(this.getScriptObjectName());
        this.craftRecipe.setModule(this.getModule());
        block.addValue("tags", CraftRecipeTag.ENTITY_RECIPE.toString());
        this.craftRecipe.Load(this.getScriptObjectName(), block);
    }

    public CraftRecipe getCraftRecipe() {
        return this.craftRecipe;
    }

    public Texture getIconTexture() {
        Texture icon = this.craftRecipe.getIconTexture();
        GameEntityScript gameEntityScript = (GameEntityScript)this.getParent();
        UiConfigScript uiConfig = gameEntityScript.getComponentScriptFor(ComponentType.UiConfig);
        SpriteConfigScript spriteConfig = gameEntityScript.getComponentScriptFor(ComponentType.SpriteConfig);
        if (uiConfig != null) {
            String skinName = uiConfig.getXuiSkinName();
            String entityStyle = uiConfig.getEntityStyle();
            XuiSkin skin = XuiManager.GetSkin(skinName);
            if (skin == null) {
                skin = XuiManager.GetDefaultSkin();
            }

            if (skin != null) {
                XuiSkin.EntityUiStyle style = skin.getEntityUiStyle(entityStyle);
                if (style != null && style.getIcon() != null) {
                    icon = style.getIcon();
                }
            }
        } else if (spriteConfig != null) {
            DebugLog.General
                .error(
                    "CraftRecipeComponentScript: Recipe "
                        + this.craftRecipe.getName()
                        + " missing UiConfigScript - falling back to SpriteConfigScript first world object face for recipe icon."
                );
            String textureName = !spriteConfig.getAllTileNames().isEmpty() ? spriteConfig.getAllTileNames().get(0) : "default";
            icon = Texture.getSharedTexture(textureName);
        }

        return icon;
    }

    private String getTranslationName() {
        String displayName = Translator.getText("EC_Entity_DisplayName_Default");
        GameEntityScript gameEntityScript = (GameEntityScript)this.getParent();
        UiConfigScript uiConfig = gameEntityScript.getComponentScriptFor(ComponentType.UiConfig);
        if (uiConfig != null) {
            String skinName = uiConfig.getXuiSkinName();
            String entityStyle = uiConfig.getEntityStyle();
            XuiSkin skin = XuiManager.GetSkin(skinName);
            if (skin == null) {
                skin = XuiManager.GetDefaultSkin();
            }

            if (skin != null) {
                XuiSkin.EntityUiStyle style = skin.getEntityUiStyle(entityStyle);
                if (style != null && style.getDisplayName() != null) {
                    displayName = style.getDisplayName();
                }
            }
        } else {
            displayName = this.craftRecipe.getTranslationName();
        }

        return displayName;
    }

    public String getBuildCategory() {
        return !this.craftRecipe.getTags().isEmpty() ? this.craftRecipe.getTags().get(0) : null;
    }
}
