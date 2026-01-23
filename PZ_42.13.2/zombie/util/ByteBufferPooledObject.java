// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public class ByteBufferPooledObject extends PooledObject {
    protected ByteBuffer buffer;
    protected int startIndex;
    protected int capacity;
    protected int currentPosition;

    @Override
    public void onReleased() {
        this.buffer = null;
        this.startIndex = 0;
        this.capacity = 0;
        this.currentPosition = 0;
    }

    public void put(byte[] data) {
        this.put(data, 0, data.length);
    }

    public void put(byte[] data, int start, int length) {
        if (this.currentPosition + length > this.capacity) {
            throw new BufferOverflowException();
        } else {
            this.buffer.put(this.startIndex + this.currentPosition, data, start, length);
            this.currentPosition += length;
        }
    }

    public byte get(int index) {
        return this.buffer.get(this.startIndex + index);
    }

    public ByteBuffer slice() {
        return this.buffer.slice(this.startIndex, this.capacity);
    }

    public int capacity() {
        return this.capacity;
    }

    public int position() {
        return this.currentPosition;
    }
}
