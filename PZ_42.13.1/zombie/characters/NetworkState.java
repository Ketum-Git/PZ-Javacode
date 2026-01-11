// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.LinkedList;
import zombie.ai.State;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.Vector3;
import zombie.network.PacketTypes;
import zombie.network.packets.actions.StatePacket;

public class NetworkState {
    private final LinkedList<StatePacket> enterStatePackets = new LinkedList<>();
    private final LinkedList<StatePacket> enterSubStatePackets = new LinkedList<>();
    private final LinkedList<StatePacket> exitStatePackets = new LinkedList<>();
    private final LinkedList<StatePacket> exitSubStatePackets = new LinkedList<>();

    public boolean processEnterState(Vector3 target) {
        boolean moveToEvent = false;
        StatePacket statePacket = this.enterStatePackets.peek();
        if (statePacket != null && statePacket.getState().isSyncOnSquare()) {
            target.set(statePacket.getX(), statePacket.getY(), statePacket.getZ());
            if (statePacket.isAway()) {
                moveToEvent = true;
            } else if (!statePacket.getState().isSyncInIdle() || statePacket.isReady()) {
                statePacket.apply();
                this.enterStatePackets.remove(statePacket);
            }
        }

        return moveToEvent;
    }

    public boolean processEnterSubState(Vector3 target) {
        boolean moveToEvent = false;
        StatePacket statePacket = this.enterSubStatePackets.peek();
        if (statePacket != null && statePacket.getState().isSyncOnSquare()) {
            target.set(statePacket.getX(), statePacket.getY(), statePacket.getZ());
            if (statePacket.isAway()) {
                moveToEvent = true;
            } else if (!statePacket.getState().isSyncInIdle() || statePacket.isReady()) {
                statePacket.apply();
                this.enterSubStatePackets.remove(statePacket);
            }
        }

        return moveToEvent;
    }

    public void processExitSubState() {
        StatePacket statePacket = this.exitSubStatePackets.peek();
        if (statePacket != null) {
            statePacket.apply();
            this.exitSubStatePackets.remove(statePacket);
        }
    }

    public void processExitState() {
        StatePacket statePacket = this.exitStatePackets.peek();
        if (statePacket != null) {
            statePacket.apply();
            this.exitStatePackets.remove(statePacket);
        }
    }

    public void addEnterState(StatePacket packet) {
        this.enterStatePackets.add(packet);
    }

    public void addEnterSubState(StatePacket packet) {
        this.enterSubStatePackets.add(packet);
    }

    public void addExitState(StatePacket packet) {
        this.exitStatePackets.add(packet);
    }

    public void addExitSubState(StatePacket packet) {
        this.exitSubStatePackets.add(packet);
    }

    public void removeEnterState(StatePacket statePacket) {
        this.enterStatePackets.remove(statePacket);
    }

    public void removeEnterSubState(StatePacket statePacket) {
        this.enterSubStatePackets.remove(statePacket);
    }

    public void removeExitState(StatePacket statePacket) {
        this.exitStatePackets.remove(statePacket);
    }

    public void removeExitSubState(StatePacket statePacket) {
        this.exitSubStatePackets.remove(statePacket);
    }

    public StatePacket getEnterState() {
        return this.enterStatePackets.isEmpty() ? null : this.enterStatePackets.getLast();
    }

    public StatePacket getEnterSubState() {
        return this.enterSubStatePackets.isEmpty() ? null : this.enterSubStatePackets.getLast();
    }

    public StatePacket getExitState() {
        return this.exitStatePackets.isEmpty() ? null : this.exitStatePackets.getLast();
    }

    public StatePacket getExitSubState() {
        return this.exitSubStatePackets.isEmpty() ? null : this.exitSubStatePackets.getLast();
    }

    public String getEnterStateName() {
        return this.enterStatePackets.isEmpty() ? "" : this.enterStatePackets.getLast().getState().getName();
    }

    public String getEnterSubStateName() {
        return this.enterSubStatePackets.isEmpty() ? "" : this.enterSubStatePackets.getLast().getState().getName();
    }

    public String getExitStateName() {
        return this.exitStatePackets.isEmpty() ? "" : this.exitStatePackets.getLast().getState().getName();
    }

    public String getExitSubStateName() {
        return this.exitSubStatePackets.isEmpty() ? "" : this.exitSubStatePackets.getLast().getState().getName();
    }

    private StatePacket getState(LinkedList<StatePacket> statePackets, State state) {
        for (StatePacket statePacket : statePackets) {
            if (statePacket.getState().getName().equals(state.getName())) {
                return statePacket;
            }
        }

        return null;
    }

    public StatePacket getEnterState(State state) {
        return this.getState(this.enterStatePackets, state);
    }

    public StatePacket getEnterSubState(State state) {
        return this.getState(this.enterSubStatePackets, state);
    }

    public StatePacket getExitState(State state) {
        return this.getState(this.exitStatePackets, state);
    }

    public StatePacket getExitSubState(State state) {
        return this.getState(this.exitSubStatePackets, state);
    }

    private void addState(LinkedList<StatePacket> statePackets, StatePacket packet) {
        statePackets.add(packet);
    }

    private void updateState(LinkedList<StatePacket> statePackets, StatePacket packet) {
        if (packet != null) {
            if (!statePackets.isEmpty()) {
                packet.getEvents().get().putAll(statePackets.getLast().getEvents().get());
            }

            statePackets.clear();
            statePackets.add(packet);
        } else {
            statePackets.clear();
        }
    }

    public void updateEnterState(StatePacket packet) {
        this.updateState(this.enterStatePackets, packet);
    }

    public void updateEnterSubState(StatePacket packet) {
        this.updateState(this.enterSubStatePackets, packet);
    }

    public void updateExitState(StatePacket packet) {
        this.updateState(this.exitStatePackets, packet);
    }

    public void updateExitSubState(StatePacket packet) {
        this.updateState(this.exitSubStatePackets, packet);
    }

    private void sync(LinkedList<StatePacket> packets, UdpConnection connection) {
        if (!packets.isEmpty()) {
            packets.getLast().sendToClient(PacketTypes.PacketType.State, connection);
        }
    }

    public void sync(UdpConnection connection) {
        this.sync(this.enterStatePackets, connection);
        this.sync(this.enterSubStatePackets, connection);
    }

    private void send(LinkedList<StatePacket> packets) {
        if (!packets.isEmpty()) {
            packets.getLast().sendToServer(PacketTypes.PacketType.State);
            packets.clear();
        }
    }

    public void send() {
        this.send(this.exitSubStatePackets);
        this.send(this.exitStatePackets);
        this.send(this.enterStatePackets);
        this.send(this.enterSubStatePackets);
    }

    private boolean timeout(LinkedList<StatePacket> packets) {
        StatePacket packet = packets.peek();
        if (packet != null && System.currentTimeMillis() - packet.timestamp > 5000L) {
            packets.remove(packet);
            DebugLog.Multiplayer.noise("State %s timeout", packet.getState().getName());
            return true;
        } else {
            return false;
        }
    }

    public boolean timeout() {
        boolean result = false;
        result |= this.timeout(this.exitSubStatePackets);
        result |= this.timeout(this.exitStatePackets);
        result |= this.timeout(this.enterStatePackets);
        return result | this.timeout(this.enterSubStatePackets);
    }

    public void reset() {
        this.enterStatePackets.clear();
        this.enterSubStatePackets.clear();
        this.exitStatePackets.clear();
        this.exitSubStatePackets.clear();
    }

    public void reportEvent(String state, String event) {
        if (this.getEnterState() != null) {
            this.getEnterState().getEvents().get().put(event, state);
        }

        if (this.getEnterSubState() != null) {
            this.getEnterSubState().getEvents().get().put(event, state);
        }

        if (this.getExitState() != null) {
            this.getExitState().getEvents().get().put(event, state);
        }

        if (this.getExitSubState() != null) {
            this.getExitSubState().getEvents().get().put(event, state);
        }
    }
}
