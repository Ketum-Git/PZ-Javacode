// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import zombie.util.ByteBufferPool;
import zombie.util.ByteBufferPooledObject;
import zombie.util.Pool;

public class SaveBufferMap {
    private final ByteBufferPool saveBufferPool = new ByteBufferPool();
    private final HashMap<String, SaveBufferMap.Buffer> saveBufferMap = new HashMap<>();

    public ByteBufferPooledObject allocate(int size) {
        return this.saveBufferPool.allocate(size);
    }

    public void put(String key, ByteBufferPooledObject value, SaveBufferMap.IWriter writer) {
        this.saveBufferMap.put(key, new SaveBufferMap.Buffer(value, writer));
    }

    public void put(String key, ByteBufferPooledObject value) {
        this.saveBufferMap.put(key, new SaveBufferMap.Buffer(value, null));
    }

    public ByteBufferPooledObject get(String key) {
        return this.saveBufferMap.get(key).buffer();
    }

    public void save(SaveBufferMap.IWriter writer) throws IOException {
        if (!this.saveBufferMap.isEmpty()) {
            for (Entry<String, SaveBufferMap.Buffer> entry : this.saveBufferMap.entrySet()) {
                String fileName = entry.getKey();
                SaveBufferMap.Buffer buffer = entry.getValue();
                if (buffer.writer() == null) {
                    writer.accept(fileName, buffer.buffer());
                } else {
                    buffer.writer().accept(fileName, buffer.buffer());
                }
            }
        }
    }

    public void clear() {
        for (SaveBufferMap.Buffer buffer : this.saveBufferMap.values()) {
            Pool.tryRelease(buffer.buffer());
        }

        this.saveBufferPool.resetBuffer();
        this.saveBufferMap.clear();
    }

    public Set<String> keySet() {
        return this.saveBufferMap.keySet();
    }

    private record Buffer(ByteBufferPooledObject buffer, SaveBufferMap.IWriter writer) {
    }

    public interface IWriter {
        void accept(String arg0, ByteBufferPooledObject arg1) throws IOException;
    }
}
