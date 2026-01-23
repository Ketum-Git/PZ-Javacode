// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world.scripts;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;
import zombie.world.WorldDictionaryException;

public class VersionHash implements IVersionHash {
    private boolean dirty;
    private boolean hasHashed;
    private long hash;
    private String toHash = "";
    private boolean corrupted;

    @Override
    public String getString() {
        return this.toHash;
    }

    public void reset() {
        this.toHash = "";
        this.dirty = false;
        this.hash = 0L;
        this.corrupted = false;
        this.hasHashed = false;
    }

    @Override
    public boolean isEmpty() {
        return StringUtils.isNullOrEmpty(this.toHash);
    }

    public boolean isCorrupted() {
        return this.corrupted;
    }

    @Override
    public void add(String element) {
        if (element != null) {
            this.toHash = this.toHash + element;
            this.dirty = true;
        } else {
            this.corrupted = true;
            DebugLog.General.error("Trying to add a null String to hash.");
        }
    }

    @Override
    public void add(IVersionHash other) {
        if (other != null) {
            this.toHash = this.toHash + other.getString();
            this.dirty = true;
        } else {
            this.corrupted = true;
            DebugLog.General.error("Trying to add a null IVersionHash to hash.");
        }
    }

    @Override
    public long getHash() throws WorldDictionaryException {
        if (this.corrupted) {
            throw new WorldDictionaryException("Corrupted hash");
        } else if (this.hasHashed) {
            if (this.dirty) {
                throw new WorldDictionaryException("ToHash is dirty");
            } else {
                return this.hash;
            }
        } else {
            this.dirty = false;
            HashCode hashCode = Hashing.sha256().hashString(this.toHash, StandardCharsets.UTF_8);
            this.hash = hashCode.asLong();
            this.hasHashed = true;
            return this.hash;
        }
    }
}
