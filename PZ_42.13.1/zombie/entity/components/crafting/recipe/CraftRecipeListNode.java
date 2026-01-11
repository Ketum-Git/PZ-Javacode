// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting.recipe;

import java.util.ArrayList;
import java.util.List;
import zombie.UsedFromLua;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.CraftRecipeGroup;

@UsedFromLua
public class CraftRecipeListNode {
    protected CraftRecipeListNode.CraftRecipeListNodeType type;
    protected CraftRecipeListNode parent;
    protected List<CraftRecipeListNode> children;
    protected CraftRecipe recipe;
    protected Texture iconTexture;
    protected String title;
    protected CraftRecipeGroup group;
    protected CraftRecipeListNode.CraftRecipeListNodeExpandedState expandedState;

    public static CraftRecipeListNode createGroupNode(
        CraftRecipeGroup group, String title, Texture iconTexture, CraftRecipeListNode.CraftRecipeListNodeExpandedState expandedState
    ) {
        CraftRecipeListNode node = new CraftRecipeListNode();
        node.type = CraftRecipeListNode.CraftRecipeListNodeType.GROUP;
        node.title = title;
        node.group = group;
        node.children = new ArrayList<>();
        node.iconTexture = iconTexture;
        node.expandedState = expandedState;
        return node;
    }

    public static CraftRecipeListNode createRecipeNode(CraftRecipe recipe, CraftRecipeListNode parent) {
        CraftRecipeListNode node = new CraftRecipeListNode();
        node.type = CraftRecipeListNode.CraftRecipeListNodeType.RECIPE;
        node.parent = parent;
        node.recipe = recipe;
        node.expandedState = CraftRecipeListNode.CraftRecipeListNodeExpandedState.PARTIAL;
        if (recipe != null) {
            node.group = recipe.getRecipeGroup();
            node.title = recipe.getTranslationName();
            node.iconTexture = recipe.getIconTexture();
        } else {
            DebugLog.CraftLogic.error("recipe == null when calling CraftRecipeListNode:allocRecipe - This should not happen!");
        }

        return node;
    }

    public CraftRecipeListNode.CraftRecipeListNodeType getType() {
        return this.type;
    }

    public CraftRecipeListNode getParent() {
        return this.parent;
    }

    public CraftRecipe getRecipe() {
        return this.recipe;
    }

    public Texture getIconTexture() {
        return this.iconTexture;
    }

    public String getTitle() {
        return this.title;
    }

    public CraftRecipeGroup getGroup() {
        return this.group;
    }

    public CraftRecipeListNode.CraftRecipeListNodeExpandedState getExpandedState() {
        return this.expandedState;
    }

    public void setExpandedState(CraftRecipeListNode.CraftRecipeListNodeExpandedState state) {
        this.expandedState = state;
    }

    public void toggleExpandedState() {
        this.setExpandedState(switch (this.expandedState) {
            case CLOSED -> CraftRecipeListNode.CraftRecipeListNodeExpandedState.PARTIAL;
            case PARTIAL -> CraftRecipeListNode.CraftRecipeListNodeExpandedState.OPEN;
            case OPEN -> CraftRecipeListNode.CraftRecipeListNodeExpandedState.CLOSED;
        });
    }

    public List<CraftRecipeListNode> getChildren() {
        return this.children;
    }

    @UsedFromLua
    public static enum CraftRecipeListNodeExpandedState {
        CLOSED,
        PARTIAL,
        OPEN;
    }

    @UsedFromLua
    public static enum CraftRecipeListNodeType {
        GROUP,
        RECIPE;
    }
}
