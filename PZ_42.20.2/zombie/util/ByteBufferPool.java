// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;
import zombie.debug.DebugType;

public class ByteBufferPool {
    private static final Pool<ByteBufferPooledObject> byteBufferPool = new Pool<>(ByteBufferPooledObject::new);
    private static final int MEMORY_BLOCK_SIZE = 10485760;
    private ByteBufferPool.MemoryBlock memoryPool = new ByteBufferPool.MemoryBlock(10485760);
    private int memoryPoolIndex;

    public ByteBufferPooledObject allocate(int size) {
        int freeMemory = this.memoryPool.capacity() - this.memoryPoolIndex;
        if (size > freeMemory) {
            ByteBufferPool.MemoryBlock oldPool = this.memoryPool;
            int additionalRequiredMemoryBlockCount = (size - freeMemory) / 10485760 + 1;
            this.memoryPool = new ByteBufferPool.MemoryBlock(this.memoryPool.capacity() + 10485760 * additionalRequiredMemoryBlockCount);
            this.memoryPool.next = oldPool;
            this.memoryPoolIndex = 0;
            DebugType.General.debugln("ByteBufferPool: Allocating larger pool - new size: %d", this.memoryPool.capacity());
        }

        ByteBufferPooledObject newInstance = byteBufferPool.alloc();
        newInstance.buffer = this.memoryPool.bb;
        newInstance.startIndex = this.memoryPoolIndex;
        newInstance.capacity = size;
        this.memoryPoolIndex += size;
        return newInstance;
    }

    public void resetBuffer() {
        ByteBufferPool.MemoryBlock block = this.memoryPool.next;
        this.memoryPool.next = null;

        while (block != null) {
            block = block.release();
        }

        this.memoryPoolIndex = 0;
    }

    private static final class MemoryBlock {
        final ByteBuffer bb;
        ByteBufferPool.MemoryBlock next;

        MemoryBlock(int size) {
            this.bb = MemoryUtil.memAlloc(size);
        }

        int capacity() {
            return this.bb.capacity();
        }

        ByteBufferPool.MemoryBlock release() {
            MemoryUtil.memFree(this.bb);
            return this.next;
        }
    }
}
