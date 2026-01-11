// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import zombie.ai.State;
import zombie.core.raknet.UdpConnection;

public class StateID extends IDString implements INetworkPacketField {
    protected State state;

    public void set(State state) {
        this.set(state.getClass().getTypeName());
        this.state = state;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);

        try {
            Class<?> cls = Class.forName(this.get());
            Method m = cls.getMethod("instance");
            this.state = (State)m.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException var5) {
            throw new RuntimeException("invalid state " + this.get());
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.state != null;
    }

    public State getState() {
        return this.state;
    }
}
