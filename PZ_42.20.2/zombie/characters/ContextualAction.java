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
    static final ObjectPool<ContextualAction> pool = new ObjectPool<>(ContextualAction::new, "ContextualAction.pool");
    public ContextualAction.Action action = ContextualAction.Action.NONE;
    public IsoDirections dir;
    public IsoGridSquare square;
    public IsoObject object;
    public ItemContainer targetContainer;
    public InventoryItem inventoryItem;
    public int priority;
    public boolean behind;

    public ContextualAction reset() {
        this.action = ContextualAction.Action.NONE;
        this.dir = null;
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

    public static ContextualAction alloc(ContextualAction.Action action) {
        ContextualAction newAction = alloc();
        newAction.action = action;
        newAction.priority = action.priority;
        return newAction;
    }

    public static ContextualAction alloc(ContextualAction.Action action, IsoDirections dir, IsoGridSquare square, IsoObject object) {
        ContextualAction newAction = alloc(action);
        newAction.dir = dir;
        newAction.square = square;
        newAction.object = object;
        return newAction;
    }

    public static ContextualAction alloc(ContextualAction.Action action, Invokers.Params1.ICallback<ContextualAction> populator) {
        ContextualAction newAction = alloc(action);
        populator.accept(newAction);
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

    @Override
    public String toString() {
        return String.format(
            "%s{action=%s,dir=%s,square=%s,object=%s,targetContainer=%s,inventoryItem=%s,priority=%s,behind=%s}",
            this.getClass().getSimpleName(),
            this.action,
            this.dir,
            this.square,
            this.object,
            this.targetContainer,
            this.inventoryItem,
            this.priority,
            this.behind
        );
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
