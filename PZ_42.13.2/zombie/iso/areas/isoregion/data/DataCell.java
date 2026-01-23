// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas.isoregion.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import zombie.UsedFromLua;

/**
 * TurboTuTone.
 */
@UsedFromLua
public final class DataCell {
    public final DataRoot dataRoot;
    final Map<Integer, DataChunk> dataChunks = new HashMap<>();

    DataCell(DataRoot dataRoot) {
        this.dataRoot = dataRoot;
    }

    private DataRoot getDataRoot() {
        return this.dataRoot;
    }

    DataChunk getChunk(int chunkID) {
        return this.dataChunks.get(chunkID);
    }

    DataChunk addChunk(int chunkX, int chunkY, int chunkID) {
        DataChunk chunk = new DataChunk(chunkX, chunkY, this, chunkID);
        this.dataChunks.put(chunkID, chunk);
        return chunk;
    }

    void setChunk(DataChunk chunk) {
        this.dataChunks.put(chunk.getHashId(), chunk);
    }

    void getAllChunks(List<DataChunk> list) {
        for (Entry<Integer, DataChunk> entry : this.dataChunks.entrySet()) {
            list.add(entry.getValue());
        }
    }
}
