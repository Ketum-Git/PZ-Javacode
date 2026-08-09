// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world.scripts;

import zombie.world.WorldDictionaryException;

public interface IVersionHash {
    boolean isEmpty();

    String getString();

    void add(String arg0);

    void add(IVersionHash arg0);

    long getHash() throws WorldDictionaryException;
}
