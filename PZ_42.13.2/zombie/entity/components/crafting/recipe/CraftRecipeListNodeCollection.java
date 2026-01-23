// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting.recipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import zombie.UsedFromLua;
import zombie.entity.components.crafting.BaseCraftingLogic;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.CraftRecipeGroup;

@UsedFromLua
public class CraftRecipeListNodeCollection {
    private final List<CraftRecipeListNode> nodes = new ArrayList<>();
    private final Map<CraftRecipeGroup, CraftRecipeListNode> groupNodes = new HashMap<>();

    public List<CraftRecipeListNode> getNodes() {
        return this.nodes;
    }

    public void add(CraftRecipe recipe) {
        if (recipe != null) {
            CraftRecipeListNode groupParent = null;
            List<CraftRecipeListNode> parentNodeArray = this.nodes;
            if (recipe.getRecipeGroup() != null) {
                groupParent = this.groupNodes.get(recipe.getRecipeGroup());
                if (groupParent == null) {
                    CraftRecipeGroup recipeGroup = recipe.getRecipeGroup();
                    groupParent = CraftRecipeListNode.createGroupNode(
                        recipeGroup,
                        recipeGroup.getTranslationName(),
                        recipeGroup.getIconTexture(),
                        CraftRecipeListNode.CraftRecipeListNodeExpandedState.PARTIAL
                    );
                    this.groupNodes.put(recipe.getRecipeGroup(), groupParent);
                    this.nodes.add(groupParent);
                }

                parentNodeArray = groupParent.children;
            }

            CraftRecipeListNode recipeNode = CraftRecipeListNode.createRecipeNode(recipe, groupParent);
            parentNodeArray.add(recipeNode);
        }
    }

    public void addAll(List<CraftRecipe> recipeList) {
        if (recipeList != null) {
            for (CraftRecipe recipe : recipeList) {
                this.add(recipe);
            }
        }
    }

    public void setInitialExpandedStates(BaseCraftingLogic logic, boolean isBuildCheat) {
        this.setInitialExpandedStates(logic, isBuildCheat, null, this.nodes);
    }

    private void setInitialExpandedStates(BaseCraftingLogic logic, boolean isBuildCheat, CraftRecipeListNode groupNode, List<CraftRecipeListNode> childNodes) {
        if (logic != null) {
            for (CraftRecipeListNode node : childNodes) {
                if (node.getType() == CraftRecipeListNode.CraftRecipeListNodeType.RECIPE) {
                    BaseCraftingLogic.CachedRecipeInfo cachedRecipeInfo = logic.getCachedRecipeInfo(node.getRecipe());
                    if (cachedRecipeInfo != null) {
                        boolean canBeCrafted = isBuildCheat || cachedRecipeInfo.isValid() && cachedRecipeInfo.isCanPerform();
                        node.expandedState = canBeCrafted
                            ? CraftRecipeListNode.CraftRecipeListNodeExpandedState.OPEN
                            : CraftRecipeListNode.CraftRecipeListNodeExpandedState.CLOSED;
                    }
                }

                if (node.getType() == CraftRecipeListNode.CraftRecipeListNodeType.GROUP) {
                    this.setInitialExpandedStates(logic, isBuildCheat, node, node.getChildren());
                }
            }

            if (groupNode != null) {
                if (childNodes.stream().allMatch(nodex -> nodex.expandedState == CraftRecipeListNode.CraftRecipeListNodeExpandedState.OPEN)) {
                    groupNode.expandedState = CraftRecipeListNode.CraftRecipeListNodeExpandedState.OPEN;
                } else if (childNodes.stream().allMatch(nodex -> nodex.expandedState == CraftRecipeListNode.CraftRecipeListNodeExpandedState.CLOSED)) {
                    groupNode.expandedState = CraftRecipeListNode.CraftRecipeListNodeExpandedState.CLOSED;
                }
            }
        }
    }

    public void clear() {
        this.nodes.clear();
        this.groupNodes.clear();
    }

    public boolean contains(CraftRecipe recipe) {
        return this.collectionContains(recipe, this.nodes);
    }

    private boolean collectionContains(CraftRecipe recipe, List<CraftRecipeListNode> collection) {
        for (CraftRecipeListNode node : collection) {
            if (node.getType() == CraftRecipeListNode.CraftRecipeListNodeType.RECIPE && node.getRecipe().equals(recipe)) {
                return true;
            }

            if (node.getType() == CraftRecipeListNode.CraftRecipeListNodeType.GROUP && this.collectionContains(recipe, node.children)) {
                return true;
            }
        }

        return false;
    }

    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    public void removeIf(Predicate<? super CraftRecipe> filter) {
        this.removeIf(filter, this.nodes);
    }

    private void removeIf(Predicate<? super CraftRecipe> filter, List<CraftRecipeListNode> collection) {
        for (int i = collection.size() - 1; i >= 0; i--) {
            CraftRecipeListNode node = collection.get(i);
            if (node.getType() == CraftRecipeListNode.CraftRecipeListNodeType.RECIPE && filter.test(node.getRecipe())) {
                collection.remove(i);
            }

            if (node.getType() == CraftRecipeListNode.CraftRecipeListNodeType.GROUP) {
                this.removeIf(filter, node.children);
            }
        }
    }

    public void sort(Comparator<? super CraftRecipe> comparator) {
        CraftRecipeListNodeCollection.RecipeNodeComparator nodeComparator = new CraftRecipeListNodeCollection.RecipeNodeComparator(comparator);
        this.nodes.sort(nodeComparator);
    }

    public CraftRecipe getFirstRecipe() {
        return this.getFirstRecipe(this.nodes);
    }

    private CraftRecipe getFirstRecipe(List<CraftRecipeListNode> collection) {
        for (CraftRecipeListNode node : collection) {
            CraftRecipe recipe = switch (node.getType()) {
                case RECIPE -> node.getRecipe();
                case GROUP -> this.getFirstRecipe(node.children);
            };
            if (recipe != null) {
                return recipe;
            }
        }

        return null;
    }

    public List<CraftRecipe> getAllRecipes() {
        return this.getRecipesFromCollection(this.nodes);
    }

    private List<CraftRecipe> getRecipesFromCollection(List<CraftRecipeListNode> nodes) {
        List<CraftRecipe> recipes = new ArrayList<>();

        for (CraftRecipeListNode node : nodes) {
            switch (node.getType()) {
                case RECIPE:
                    recipes.add(node.getRecipe());
                    break;
                case GROUP:
                    recipes.addAll(this.getRecipesFromCollection(node.getChildren()));
            }
        }

        return recipes;
    }

    private static class RecipeNodeComparator implements Comparator<CraftRecipeListNode> {
        private final Comparator<? super CraftRecipe> recipeComparator;

        public RecipeNodeComparator(Comparator<? super CraftRecipe> recipeComparator) {
            this.recipeComparator = recipeComparator;
        }

        public int compare(CraftRecipeListNode v1, CraftRecipeListNode v2) {
            if (v1 == null && v2 == null) {
                return 0;
            } else if (v1 == null) {
                return 1;
            } else if (v2 == null) {
                return -1;
            } else {
                CraftRecipe bestV1 = null;
                CraftRecipe bestV2 = null;
                if (v1.getType() == CraftRecipeListNode.CraftRecipeListNodeType.RECIPE) {
                    bestV1 = v1.recipe;
                } else if (!v1.children.isEmpty()) {
                    v1.children.sort(this);
                    bestV1 = v1.children.getFirst().recipe;
                }

                if (v2.getType() == CraftRecipeListNode.CraftRecipeListNodeType.RECIPE) {
                    bestV2 = v2.recipe;
                } else if (!v2.children.isEmpty()) {
                    v2.children.sort(this);
                    bestV2 = v2.children.getFirst().recipe;
                }

                return this.recipeComparator.compare(bestV1, bestV2);
            }
        }
    }
}
