// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.nativeCode;

import java.util.ArrayDeque;

final class RequestQueue {
    final ArrayDeque<PathFindRequest> playerQ = new ArrayDeque<>();
    final ArrayDeque<PathFindRequest> aggroZombieQ = new ArrayDeque<>();
    final ArrayDeque<PathFindRequest> otherQ = new ArrayDeque<>();

    boolean isEmpty() {
        return this.playerQ.isEmpty() && this.aggroZombieQ.isEmpty() && this.otherQ.isEmpty();
    }

    PathFindRequest removeFirst() {
        if (!this.playerQ.isEmpty()) {
            return this.playerQ.removeFirst();
        } else {
            return !this.aggroZombieQ.isEmpty() ? this.aggroZombieQ.removeFirst() : this.otherQ.removeFirst();
        }
    }

    PathFindRequest removeLast() {
        if (!this.otherQ.isEmpty()) {
            return this.otherQ.removeLast();
        } else {
            return !this.aggroZombieQ.isEmpty() ? this.aggroZombieQ.removeLast() : this.playerQ.removeLast();
        }
    }
}
