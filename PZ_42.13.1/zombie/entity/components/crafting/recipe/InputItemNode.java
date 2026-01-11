// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.crafting.recipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import zombie.UsedFromLua;
import zombie.inventory.InventoryItem;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.objects.Item;

@UsedFromLua
public class InputItemNode {
    private static final ArrayDeque<InputItemNode> pool = new ArrayDeque<>();
    protected final ArrayList<InventoryItem> items = new ArrayList<>();
    protected InputScript firstMatchedInputScript;
    private CraftRecipe recipe;
    protected Item scriptItem;
    private String name;
    protected boolean expandedUsed;
    protected boolean expandedAvailable;
    protected boolean isToolLeft;
    protected boolean isToolRight;
    protected boolean isTool;
    protected boolean isKeep;
    protected boolean isItemCount;
    protected static final Comparator<InputItemNode> inputItemNodeComparator = new Comparator<InputItemNode>() {
        public int compare(InputItemNode o1, InputItemNode o2) {
            return o1.name.compareTo(o2.name);
        }
    };

    protected static InputItemNode Alloc(CraftRecipe recipe, Item scriptItem) {
        InputItemNode node = pool.poll();
        if (node == null) {
            node = new InputItemNode();
        }

        node.recipe = recipe;
        node.scriptItem = scriptItem;
        node.name = scriptItem.getScriptObjectFullType();
        return node;
    }

    protected static void Release(InputItemNode node) {
        node.reset();

        assert !pool.contains(node);

        pool.offer(node);
    }

    public CraftRecipe getRecipe() {
        return this.recipe;
    }

    public Item getScriptItem() {
        return this.scriptItem;
    }

    public String getName() {
        return this.name;
    }

    public InputScript getFirstMatchedInputScript() {
        return this.firstMatchedInputScript;
    }

    public boolean isExpandedUsed() {
        return this.expandedUsed;
    }

    public boolean isExpandedAvailable() {
        return this.expandedAvailable;
    }

    public void setExpandedUsed(boolean b) {
        this.expandedUsed = b;
    }

    public void setExpandedAvailable(boolean b) {
        this.expandedAvailable = b;
    }

    public void toggleExpandedUsed() {
        this.expandedUsed = !this.expandedUsed;
    }

    public void toggleExpandedAvailable() {
        this.expandedAvailable = !this.expandedAvailable;
    }

    public boolean isToolRight() {
        return this.isToolRight;
    }

    public boolean isToolLeft() {
        return this.isToolLeft;
    }

    public boolean isTool() {
        return this.isTool;
    }

    public boolean isKeep() {
        return this.isKeep;
    }

    public boolean isItemCount() {
        return this.isItemCount;
    }

    public ArrayList<InventoryItem> getItems() {
        return this.items;
    }

    private void reset() {
        this.firstMatchedInputScript = null;
        this.recipe = null;
        this.scriptItem = null;
        this.expandedUsed = false;
        this.expandedAvailable = false;
        this.isTool = false;
        this.isToolLeft = false;
        this.isToolRight = false;
        this.items.clear();
    }
}
