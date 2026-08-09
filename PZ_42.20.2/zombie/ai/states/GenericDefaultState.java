// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;

@UsedFromLua
public final class GenericDefaultState extends State {
    private static final GenericDefaultState INSTANCE = new GenericDefaultState();

    public static GenericDefaultState instance() {
        return INSTANCE;
    }

    private GenericDefaultState() {
        super(false, false, false, false);
    }
}
