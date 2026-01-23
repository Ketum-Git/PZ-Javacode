// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.Core;

public class ByteBlock {
    private static final ConcurrentLinkedDeque<ByteBlock> pool_data_block = new ConcurrentLinkedDeque<>();
    private ByteBlock.Mode mode;
    private int startPos;
    private int length;
    private boolean safelyForceSkipOnEnd;

    public static ByteBlock Start(ByteBuffer bb, ByteBlock.Mode mode) throws IOException {
        ByteBlock block = pool_data_block.poll();
        if (block == null) {
            block = new ByteBlock();
        }

        block.mode = mode;
        if (mode == ByteBlock.Mode.Save) {
            block.start_save(bb);
        } else {
            block.load(bb);
        }

        return block;
    }

    public static void SkipAndEnd(ByteBuffer bb, ByteBlock block) throws IOException {
        if (block.mode == ByteBlock.Mode.Load) {
            block.skipBytes(bb);
            End(bb, block);
        } else {
            throw new IOException("Cannot skip on block of type input.");
        }
    }

    public static void End(ByteBuffer bb, ByteBlock block) throws IOException {
        assert !Core.debug || !pool_data_block.contains(block) : "Object already in pool.";

        if (block.mode == ByteBlock.Mode.Save) {
            block.end_save(bb);
        } else {
            if (block.safelyForceSkipOnEnd) {
                block.skipBytes(bb);
            }

            if (!block.verify(bb)) {
                throw new IOException("DataBlock size mismatch during load.");
            }
        }

        block.reset();
        pool_data_block.offer(block);
    }

    private ByteBlock() {
        this.reset();
    }

    private void reset() {
        this.startPos = -1;
        this.length = -1;
        this.safelyForceSkipOnEnd = false;
    }

    public void safelyForceSkipOnEnd() {
        this.safelyForceSkipOnEnd(true);
    }

    public void safelyForceSkipOnEnd(boolean b) {
        this.safelyForceSkipOnEnd = b;
    }

    private void validate(ByteBlock.Mode mode) throws IOException {
        if (this.mode != mode) {
            throw new IOException("DataBlock mode mismatch.");
        }
    }

    private void start_save(ByteBuffer output) throws IOException {
        this.validate(ByteBlock.Mode.Save);
        output.putInt(0);
        this.startPos = output.position();
    }

    private void end_save(ByteBuffer output) throws IOException {
        this.validate(ByteBlock.Mode.Save);
        this.length = output.position() - this.startPos;
        output.position(this.startPos - 4);
        output.putInt(this.length);
        output.position(this.startPos + this.length);
    }

    public int length() throws IOException {
        return this.length;
    }

    private void load(ByteBuffer input) throws IOException {
        this.validate(ByteBlock.Mode.Load);
        this.length = input.getInt();
        this.startPos = input.position();
        if (this.startPos < 0 || this.length < 0) {
            throw new IOException("DataBlock possible corruption.");
        }
    }

    public boolean verify(ByteBuffer input) throws IOException {
        this.validate(ByteBlock.Mode.Load);
        return input.position() == this.startPos + this.length;
    }

    private void skipBytes(ByteBuffer input) throws IOException {
        this.validate(ByteBlock.Mode.Load);
        input.position(this.startPos + this.length);
    }

    public static enum Mode {
        Save,
        Load;
    }
}
