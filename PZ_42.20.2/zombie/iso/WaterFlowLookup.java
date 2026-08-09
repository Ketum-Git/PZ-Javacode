// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public final class WaterFlowLookup {
    private WaterFlowObject root;
    PriorityQueue<WaterFlowObject> queue = new PriorityQueue<>();

    private WaterFlowObject insert(WaterFlowObject node, WaterFlowObject point, boolean compareX) {
        if (node == null) {
            return point;
        } else {
            if (compareX) {
                if (point.x < node.x) {
                    node.kdLeft = this.insert(node.kdLeft, point, false);
                } else {
                    node.kdRight = this.insert(node.kdRight, point, false);
                }
            } else if (point.y < node.y) {
                node.kdLeft = this.insert(node.kdLeft, point, true);
            } else {
                node.kdRight = this.insert(node.kdRight, point, true);
            }

            return node;
        }
    }

    public void insert(WaterFlowObject point) {
        this.root = this.insert(this.root, point, true);
    }

    public WaterFlowObject nearest(WaterFlowObject point) {
        return this.root == null ? null : this.nearest(this.root, point, this.root, true);
    }

    private WaterFlowObject nearest(WaterFlowObject node, WaterFlowObject point, WaterFlowObject closest, boolean compareX) {
        if (node == null) {
            return closest;
        } else if (node.x == point.x && node.y == point.y) {
            return node;
        } else {
            if (node.distSq(point) < closest.distSq(point)) {
                closest = node;
            }

            float distFromPartition = compareX ? point.x - node.x : point.y - node.y;
            if (distFromPartition < 0.0F) {
                closest = this.nearest(node.kdLeft, point, closest, !compareX);
                if (closest.distSq(point) >= distFromPartition * distFromPartition) {
                    closest = this.nearest(node.kdRight, point, closest, !compareX);
                }
            } else {
                closest = this.nearest(node.kdRight, point, closest, !compareX);
                if (closest.distSq(point) >= distFromPartition * distFromPartition) {
                    closest = this.nearest(node.kdLeft, point, closest, !compareX);
                }
            }

            return closest;
        }
    }

    public List<WaterFlowObject> nearestMultiple(WaterFlowObject point, int max, List<WaterFlowObject> nearest) {
        this.queue.clear();
        this.nearestMultiple(this.root, point, max, true, this.queue);
        nearest.clear();

        while (!this.queue.isEmpty()) {
            nearest.add(this.queue.poll());
        }

        Collections.reverse(nearest);
        return nearest;
    }

    private void nearestMultiple(WaterFlowObject current, WaterFlowObject query, int max, boolean checkX, PriorityQueue<WaterFlowObject> queue) {
        if (current != null) {
            current.distSq = query.distSq(current);
            if (queue.size() < max) {
                queue.offer(current);
            } else if (current.distSq < queue.peek().distSq) {
                queue.poll();
                queue.offer(current);
            }

            double planeDiff = checkX ? query.x - current.x : query.y - current.y;
            WaterFlowObject primaryChild = planeDiff < 0.0 ? current.kdLeft : current.kdRight;
            WaterFlowObject secondaryChild = planeDiff < 0.0 ? current.kdRight : current.kdLeft;
            this.nearestMultiple(primaryChild, query, max, !checkX, queue);
            if (queue.size() < max || planeDiff * planeDiff < queue.peek().distSq) {
                this.nearestMultiple(secondaryChild, query, max, !checkX, queue);
            }
        }
    }

    public void clear() {
        this.root = null;
    }
}
