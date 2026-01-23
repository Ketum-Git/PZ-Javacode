// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.animation.debug;

import zombie.util.list.PZArrayUtil;

public class GenericNameWeightRecordingFrame extends GenericNameValueRecordingFrame {
    private float[] weights = new float[0];

    public GenericNameWeightRecordingFrame(String fileKey) {
        super(fileKey, "_weights");
    }

    @Override
    protected void onColumnAdded() {
        this.weights = PZArrayUtil.add(this.weights, 0.0F);
    }

    public void logWeight(String name, int layer, float weight) {
        int columnIndex = this.getOrCreateColumn(name, layer);
        this.weights[columnIndex] = this.weights[columnIndex] + weight;
    }

    public int getOrCreateColumn(String name, int layer) {
        String layerKey = layer != 0 ? layer + ":" : "";
        String rawNameKey = String.format("%s%s", layerKey, name);
        int columnIndex = this.getOrCreateColumn(rawNameKey);
        if (this.weights[columnIndex] == 0.0F) {
            return columnIndex;
        } else {
            int d = 1;

            while (true) {
                String duplicateNameKey = String.format("%s%s-%d", layerKey, name, d);
                columnIndex = this.getOrCreateColumn(duplicateNameKey);
                if (this.weights[columnIndex] == 0.0F) {
                    return columnIndex;
                }

                d++;
            }
        }
    }

    public float getWeightAt(int i) {
        return this.weights[i];
    }

    @Override
    public String getValueAt(int i) {
        return String.valueOf(this.getWeightAt(i));
    }

    @Override
    public void reset() {
        int i = 0;

        for (int weightCount = this.weights.length; i < weightCount; i++) {
            this.weights[i] = 0.0F;
        }
    }
}
