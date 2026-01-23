// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.io;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;
import zombie.core.utils.Bits;
import zombie.debug.DebugLog;

public final class BitHeader {
    private static final ConcurrentLinkedDeque<BitHeader.BitHeaderByte> pool_byte = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<BitHeader.BitHeaderShort> pool_short = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<BitHeader.BitHeaderInt> pool_int = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<BitHeader.BitHeaderLong> pool_long = new ConcurrentLinkedDeque<>();
    public static final boolean DEBUG = true;

    private static BitHeader.BitHeaderBase getHeader(BitHeader.HeaderSize type, ByteBuffer buffer, boolean isWrite) {
        if (type == BitHeader.HeaderSize.Byte) {
            BitHeader.BitHeaderByte header = pool_byte.poll();
            if (header == null) {
                header = new BitHeader.BitHeaderByte();
            }

            header.setBuffer(buffer);
            header.setWrite(isWrite);
            return header;
        } else if (type == BitHeader.HeaderSize.Short) {
            BitHeader.BitHeaderShort header = pool_short.poll();
            if (header == null) {
                header = new BitHeader.BitHeaderShort();
            }

            header.setBuffer(buffer);
            header.setWrite(isWrite);
            return header;
        } else if (type == BitHeader.HeaderSize.Integer) {
            BitHeader.BitHeaderInt header = pool_int.poll();
            if (header == null) {
                header = new BitHeader.BitHeaderInt();
            }

            header.setBuffer(buffer);
            header.setWrite(isWrite);
            return header;
        } else if (type == BitHeader.HeaderSize.Long) {
            BitHeader.BitHeaderLong header = pool_long.poll();
            if (header == null) {
                header = new BitHeader.BitHeaderLong();
            }

            header.setBuffer(buffer);
            header.setWrite(isWrite);
            return header;
        } else {
            return null;
        }
    }

    private BitHeader() {
    }

    public static void debug_print() {
        DebugLog.log("*********************************************");
        DebugLog.log("ByteHeader = " + pool_byte.size());
        DebugLog.log("ShortHeader = " + pool_short.size());
        DebugLog.log("IntHeader = " + pool_int.size());
        DebugLog.log("LongHeader = " + pool_long.size());
    }

    public static BitHeaderWrite allocWrite(BitHeader.HeaderSize size, ByteBuffer buffer) {
        return allocWrite(size, buffer, false);
    }

    public static BitHeaderWrite allocWrite(BitHeader.HeaderSize size, ByteBuffer buffer, boolean allocOnly) {
        BitHeaderWrite header = getHeader(size, buffer, true);
        if (!allocOnly) {
            header.create();
        }

        return header;
    }

    public static BitHeaderRead allocRead(BitHeader.HeaderSize size, ByteBuffer buffer) {
        return allocRead(size, buffer, false);
    }

    public static BitHeaderRead allocRead(BitHeader.HeaderSize size, ByteBuffer buffer, boolean allocOnly) {
        BitHeaderRead header = getHeader(size, buffer, false);
        if (!allocOnly) {
            header.read();
        }

        return header;
    }

    public abstract static class BitHeaderBase implements BitHeaderRead, BitHeaderWrite {
        protected boolean isWrite;
        protected ByteBuffer buffer;
        protected int startPos = -1;

        protected void setBuffer(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        protected void setWrite(boolean isWrite) {
            this.isWrite = isWrite;
        }

        @Override
        public int getStartPosition() {
            return this.startPos;
        }

        protected void reset() {
            this.buffer = null;
            this.isWrite = false;
            this.startPos = -1;
            this.reset_header();
        }

        @Override
        public abstract int getLen();

        @Override
        public abstract void release();

        protected abstract void reset_header();

        protected abstract void write_header();

        protected abstract void read_header();

        protected abstract void addflags_header(int var1);

        protected abstract void addflags_header(long var1);

        protected abstract boolean hasflags_header(int var1);

        protected abstract boolean hasflags_header(long var1);

        protected abstract boolean equals_header(int var1);

        protected abstract boolean equals_header(long var1);

        @Override
        public void create() {
            if (this.isWrite) {
                this.startPos = this.buffer.position();
                this.reset_header();
                this.write_header();
            } else {
                throw new RuntimeException("BitHeader -> Cannot write to a non write Header.");
            }
        }

        @Override
        public void write() {
            if (this.isWrite) {
                int cur_pos = this.buffer.position();
                this.buffer.position(this.startPos);
                this.write_header();
                this.buffer.position(cur_pos);
            } else {
                throw new RuntimeException("BitHeader -> Cannot write to a non write Header.");
            }
        }

        @Override
        public void read() {
            if (!this.isWrite) {
                this.startPos = this.buffer.position();
                this.read_header();
            } else {
                throw new RuntimeException("BitHeader -> Cannot read from a non read Header.");
            }
        }

        @Override
        public void addFlags(int flags) {
            if (this.isWrite) {
                this.addflags_header(flags);
            } else {
                throw new RuntimeException("BitHeader -> Cannot set bits on a non write Header.");
            }
        }

        @Override
        public void addFlags(long flags) {
            if (this.isWrite) {
                this.addflags_header(flags);
            } else {
                throw new RuntimeException("BitHeader -> Cannot set bits on a non write Header.");
            }
        }

        @Override
        public boolean hasFlags(int flags) {
            return this.hasflags_header(flags);
        }

        @Override
        public boolean hasFlags(long flags) {
            return this.hasflags_header(flags);
        }

        @Override
        public boolean equals(int flags) {
            return this.equals_header(flags);
        }

        @Override
        public boolean equals(long flags) {
            return this.equals_header(flags);
        }
    }

    public static class BitHeaderByte extends BitHeader.BitHeaderBase {
        private byte header;

        private BitHeaderByte() {
        }

        @Override
        public void release() {
            this.reset();
            BitHeader.pool_byte.offer(this);
        }

        @Override
        public int getLen() {
            return Bits.getLen(this.header);
        }

        @Override
        protected void reset_header() {
            this.header = 0;
        }

        @Override
        protected void write_header() {
            this.buffer.put(this.header);
        }

        @Override
        protected void read_header() {
            this.header = this.buffer.get();
        }

        @Override
        protected void addflags_header(int flags) {
            this.header = Bits.addFlags(this.header, flags);
        }

        @Override
        protected void addflags_header(long flags) {
            this.header = Bits.addFlags(this.header, flags);
        }

        @Override
        protected boolean hasflags_header(int flags) {
            return Bits.hasFlags(this.header, flags);
        }

        @Override
        protected boolean hasflags_header(long flags) {
            return Bits.hasFlags(this.header, flags);
        }

        @Override
        protected boolean equals_header(int flags) {
            return this.header == flags;
        }

        @Override
        protected boolean equals_header(long flags) {
            return this.header == flags;
        }
    }

    public static class BitHeaderInt extends BitHeader.BitHeaderBase {
        private int header;

        private BitHeaderInt() {
        }

        @Override
        public void release() {
            this.reset();
            BitHeader.pool_int.offer(this);
        }

        @Override
        public int getLen() {
            return Bits.getLen(this.header);
        }

        @Override
        protected void reset_header() {
            this.header = 0;
        }

        @Override
        protected void write_header() {
            this.buffer.putInt(this.header);
        }

        @Override
        protected void read_header() {
            this.header = this.buffer.getInt();
        }

        @Override
        protected void addflags_header(int flags) {
            this.header = Bits.addFlags(this.header, flags);
        }

        @Override
        protected void addflags_header(long flags) {
            this.header = Bits.addFlags(this.header, flags);
        }

        @Override
        protected boolean hasflags_header(int flags) {
            return Bits.hasFlags(this.header, flags);
        }

        @Override
        protected boolean hasflags_header(long flags) {
            return Bits.hasFlags(this.header, flags);
        }

        @Override
        protected boolean equals_header(int flags) {
            return this.header == flags;
        }

        @Override
        protected boolean equals_header(long flags) {
            return this.header == flags;
        }
    }

    public static class BitHeaderLong extends BitHeader.BitHeaderBase {
        private long header;

        private BitHeaderLong() {
        }

        @Override
        public void release() {
            this.reset();
            BitHeader.pool_long.offer(this);
        }

        @Override
        public int getLen() {
            return Bits.getLen(this.header);
        }

        @Override
        protected void reset_header() {
            this.header = 0L;
        }

        @Override
        protected void write_header() {
            this.buffer.putLong(this.header);
        }

        @Override
        protected void read_header() {
            this.header = this.buffer.getLong();
        }

        @Override
        protected void addflags_header(int flags) {
            this.header = Bits.addFlags(this.header, flags);
        }

        @Override
        protected void addflags_header(long flags) {
            this.header = Bits.addFlags(this.header, flags);
        }

        @Override
        protected boolean hasflags_header(int flags) {
            return Bits.hasFlags(this.header, flags);
        }

        @Override
        protected boolean hasflags_header(long flags) {
            return Bits.hasFlags(this.header, flags);
        }

        @Override
        protected boolean equals_header(int flags) {
            return this.header == flags;
        }

        @Override
        protected boolean equals_header(long flags) {
            return this.header == flags;
        }
    }

    public static class BitHeaderShort extends BitHeader.BitHeaderBase {
        private short header;

        private BitHeaderShort() {
        }

        @Override
        public void release() {
            this.reset();
            BitHeader.pool_short.offer(this);
        }

        @Override
        public int getLen() {
            return Bits.getLen(this.header);
        }

        @Override
        protected void reset_header() {
            this.header = 0;
        }

        @Override
        protected void write_header() {
            this.buffer.putShort(this.header);
        }

        @Override
        protected void read_header() {
            this.header = this.buffer.getShort();
        }

        @Override
        protected void addflags_header(int flags) {
            this.header = Bits.addFlags(this.header, flags);
        }

        @Override
        protected void addflags_header(long flags) {
            this.header = Bits.addFlags(this.header, flags);
        }

        @Override
        protected boolean hasflags_header(int flags) {
            return Bits.hasFlags(this.header, flags);
        }

        @Override
        protected boolean hasflags_header(long flags) {
            return Bits.hasFlags(this.header, flags);
        }

        @Override
        protected boolean equals_header(int flags) {
            return this.header == flags;
        }

        @Override
        protected boolean equals_header(long flags) {
            return this.header == flags;
        }
    }

    public static enum HeaderSize {
        Byte,
        Short,
        Integer,
        Long;
    }
}
