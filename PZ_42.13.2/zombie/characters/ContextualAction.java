// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.popman.ObjectPool;
import zombie.util.lambda.Invokers;

public final class ContextualAction {
    static final ObjectPool<ContextualAction> pool = new ObjectPool<>(ContextualAction::new);
    public ContextualAction.Action action = ContextualAction.Action.NONE;
    public IsoDirections dir = IsoDirections.Max;
    public IsoGridSquare square;
    public IsoObject object;
    public ItemContainer targetContainer;
    public InventoryItem inventoryItem;
    public int priority;
    public boolean behind;

    public ContextualAction reset() {
        this.action = ContextualAction.Action.NONE;
        this.dir = IsoDirections.Max;
        this.square = null;
        this.object = null;
        this.targetContainer = null;
        this.inventoryItem = null;
        this.priority = 0;
        this.behind = false;
        return this;
    }

    public static ContextualAction alloc() {
        return pool.alloc().reset();
    }

    public static ContextualAction alloc(ContextualAction.Action in_action) {
        ContextualAction newAction = alloc();
        newAction.action = in_action;
        newAction.priority = in_action.priority;
        return newAction;
    }

    public static ContextualAction alloc(ContextualAction.Action in_action, IsoDirections in_dir, IsoGridSquare in_square, IsoObject in_object) {
        ContextualAction newAction = alloc(in_action);
        newAction.dir = in_dir;
        newAction.square = in_square;
        newAction.object = in_object;
        return newAction;
    }

    public static ContextualAction alloc(ContextualAction.Action in_action, Invokers.Params1.ICallback<ContextualAction> in_populator) {
        ContextualAction newAction = alloc(in_action);
        in_populator.accept(newAction);
        return newAction;
    }

    public void release() {
        this.reset();
        pool.release(this);
    }

    public static void releaseAll(ArrayList<ContextualAction> actions) {
        pool.releaseAll(actions);
        actions.clear();
    }

    public static enum Action {
        NONE(0),
        AnimalInteraction(90),
        ClimbOverFence(100),
        ClimbOverWall(100),
        ClimbSheetRope(100),
        ClimbThroughWindow(100),
        OpenButcherHook(90),
        OpenHutch(90),
        RestOnFurniture(90),
        ThrowGrappledTargetOutWindow(100),
        ThrowGrappledOverFence(100),
        ThrowGrappledIntoContainer(100),
        ToggleCurtain(100),
        ToggleDoor(100),
        ToggleWindow(100);

        public final int priority;

        private Action(final int priority) {
            this.priority = priority;
        }
    }
}
