// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.debug;

import zombie.util.list.IntMap;

public class NodeWeightSlot {
    private final IntMap<NodeWeightEntry> nodeEntries = new IntMap<>();

    public void logWeight(int nodeId, int layer, float weight) {
        NodeWeightEntry entry = this.getNodeEntry(nodeId);
        entry.weight += weight;
        entry.layerIdx = layer;
    }

    private NodeWeightEntry getNodeEntry(int nodeId) {
        NodeWeightEntry existingEntry = this.nodeEntries.get(nodeId);
        if (existingEntry != null) {
            return existingEntry;
        } else {
            NodeWeightEntry newEntry = this.nodeEntries.set(nodeId, new NodeWeightEntry());
            newEntry.nodeId = nodeId;
            return newEntry;
        }
    }

    public boolean isEmpty() {
        for (int i = 0; i < this.nodeEntries.count(); i++) {
            NodeWeightEntry entry = this.nodeEntries.elementAt(i);
            if (!entry.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public String getCellString() {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < this.nodeEntries.count(); i++) {
            NodeWeightEntry entry = this.nodeEntries.elementAt(i);
            if (!entry.isEmpty()) {
                if (!stringBuilder.isEmpty()) {
                    stringBuilder.append(';');
                }

                stringBuilder.append(String.format("%d:%d:%f", entry.nodeId, entry.layerIdx, entry.weight));
            }
        }

        return stringBuilder.toString();
    }

    public void reset() {
        for (int i = 0; i < this.nodeEntries.count(); i++) {
            NodeWeightEntry entry = this.nodeEntries.elementAt(i);
            entry.reset();
        }
    }
}
