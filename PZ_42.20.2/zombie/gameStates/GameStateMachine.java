// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gameStates;

import java.util.ArrayList;
import java.util.Stack;

public final class GameStateMachine {
    public boolean firstrun = true;
    public boolean loop = true;
    public int stateIndex;
    public int loopToState;
    public final ArrayList<GameState> states = new ArrayList<>();
    private final Stack<GameState> yieldStack = new Stack<>();
    public GameState current;
    public GameState forceNext;

    public void render() {
        if (this.current != null) {
            this.current.render();
        }
    }

    public void update() {
        if (this.states.isEmpty()) {
            if (this.forceNext == null) {
                return;
            }

            this.states.add(this.forceNext);
            this.forceNext = null;
        }

        if (this.firstrun) {
            if (this.current == null) {
                this.current = this.states.get(this.stateIndex);
            }

            this.current.enter();
            this.firstrun = false;
        }

        if (this.current == null) {
            if (!this.loop) {
                return;
            }

            this.stateIndex = this.loopToState;
            if (this.states.isEmpty()) {
                return;
            }

            this.current = this.states.get(this.stateIndex);
            this.current.enter();
        }

        if (this.current != null) {
            GameState next = null;
            if (this.forceNext != null) {
                System.out.println("STATE: exit " + this.current.getClass().getName());
                this.current.exit();
                next = this.forceNext;
                this.forceNext = null;
            } else {
                GameStateMachine.StateAction action = this.current.update();
                if (action == GameStateMachine.StateAction.Continue) {
                    System.out.println("STATE: exit " + this.current.getClass().getName());
                    this.current.exit();
                    if (!this.yieldStack.isEmpty()) {
                        this.current = this.yieldStack.pop();
                        System.out.println("STATE: reenter " + this.current.getClass().getName());
                        this.current.reenter();
                        return;
                    }

                    next = this.current.redirectState();
                } else {
                    if (action != GameStateMachine.StateAction.Yield) {
                        return;
                    }

                    System.out.println("STATE: yield " + this.current.getClass().getName());
                    this.current.yield();
                    this.yieldStack.push(this.current);
                    next = this.current.redirectState();
                }
            }

            if (next == null) {
                this.stateIndex++;
                if (this.stateIndex < this.states.size()) {
                    this.current = this.states.get(this.stateIndex);
                    this.current.enter();
                } else {
                    this.current = null;
                }
            } else {
                next.enter();
                this.current = next;
            }
        }
    }

    public void forceNextState(GameState state) {
        this.forceNext = state;
    }

    public static enum StateAction {
        Continue,
        Remain,
        Yield;
    }
}
