// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.actions;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import zombie.ai.State;
import zombie.ai.states.ClimbDownSheetRopeState;
import zombie.ai.states.ClimbSheetRopeState;
import zombie.ai.states.FitnessState;
import zombie.ai.states.IdleState;
import zombie.ai.states.PlayerActionsState;
import zombie.ai.states.PlayerFallingState;
import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.JSONField;
import zombie.network.PZNetKahluaTableImpl;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.Position;
import zombie.network.fields.StateID;
import zombie.network.fields.Variables;
import zombie.network.fields.character.CharacterID;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;

@PacketSetting(ordering = 0, priority = 0, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class StatePacket extends Position implements INetworkPacket {
    @JSONField
    protected final CharacterID characterId = new CharacterID();
    @JSONField
    protected final StateID state = new StateID();
    @JSONField
    protected boolean isSubState;
    @JSONField
    protected PZNetKahluaTableImpl params = new PZNetKahluaTableImpl(new LinkedHashMap<>());
    @JSONField
    protected final Variables events = new Variables();
    @JSONField
    protected State.Stage stage;
    @JSONField
    public long timestamp;

    @Override
    public void setData(Object... values) {
        IsoGameCharacter character = (IsoGameCharacter)values[0];
        this.set(character.getX(), character.getY(), character.getZ());
        this.characterId.set(character);
        this.state.set((State)values[1]);
        this.stage = (State.Stage)values[2];
        this.params.wipe();
        this.state.getState().setParams(character, this.stage);
        character.getStateMachineParams(this.state.getState()).forEach(this::setParam);
        this.events.clear();
        this.characterId.getCharacter().getActionContext().getEvents(this.events.get());
        this.isSubState = this.getCharacter().getStateMachine().isSubstate(this.state.getState());
        this.timestamp = System.nanoTime();
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.characterId.parse(b, connection);
        this.state.parse(b, connection);
        this.isSubState = b.get() != 0;
        this.params.wipe();
        this.params.load(b, connection);
        this.events.clear();
        this.events.parse(b, connection);
        this.timestamp = System.currentTimeMillis();
        this.stage = State.Stage.values()[b.getInt()];
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.characterId.write(b);
        this.state.write(b);
        b.putBoolean(this.isSubState);
        this.params.save(b.bb);
        this.events.write(b.bb);
        b.putInt(this.stage.ordinal());
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (IsoWorld.instance.getCell().getObjectList().contains(this.characterId.getCharacter())) {
            if (this.state.getState().isSyncOnSquare() && State.Stage.Enter == this.stage) {
                if (this.isSubState) {
                    this.characterId.getCharacter().getNetworkCharacterAI().getState().addEnterSubState(this);
                } else {
                    this.characterId.getCharacter().getNetworkCharacterAI().getState().addEnterState(this);
                }
            } else if (State.Stage.Exit != this.stage || this.characterId.getCharacter().isCurrentState(this.state.getState())) {
                this.apply();
            } else if (this.isSubState) {
                StatePacket statePacket = this.characterId.getCharacter().getNetworkCharacterAI().getState().getEnterSubState(this.state.getState());
                if (statePacket != null) {
                    this.characterId.getCharacter().getNetworkCharacterAI().getState().removeEnterSubState(statePacket);
                } else {
                    this.apply();
                }
            } else {
                StatePacket statePacket = this.characterId.getCharacter().getNetworkCharacterAI().getState().getEnterState(this.state.getState());
                if (statePacket != null) {
                    this.characterId.getCharacter().getNetworkCharacterAI().getState().removeEnterState(statePacket);
                } else {
                    this.apply();
                }
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (State.Stage.Exit == this.stage) {
            if (this.state.getState().isProcessedOnExit()) {
                this.state.getState().processOnExit(this.characterId.getCharacter(), this.params.delegate);
                return;
            }

            if (this.isSubState) {
                this.characterId.getCharacter().getNetworkCharacterAI().getState().removeLastExitSubState();
                this.characterId.getCharacter().getNetworkCharacterAI().getState().addExitSubState(null);
                StatePacket enterSubStatePacket = this.characterId.getCharacter().getNetworkCharacterAI().getState().getEnterSubState();
                if (enterSubStatePacket != null && enterSubStatePacket.getState().getName().equals(this.getState().getName())) {
                    this.characterId.getCharacter().getNetworkCharacterAI().getState().removeEnterSubState(enterSubStatePacket);
                }
            } else {
                this.characterId.getCharacter().getNetworkCharacterAI().getState().removeLastExitState();
                this.characterId.getCharacter().getNetworkCharacterAI().getState().addExitState(null);
                StatePacket enterStatePacket = this.characterId.getCharacter().getNetworkCharacterAI().getState().getEnterState();
                if (enterStatePacket != null && enterStatePacket.getState().getName().equals(this.getState().getName())) {
                    this.characterId.getCharacter().getNetworkCharacterAI().getState().removeEnterState(enterStatePacket);
                }
            }
        } else if (State.Stage.Enter == this.stage) {
            if (this.isSubState) {
                StatePacket statePacket = this.characterId.getCharacter().getNetworkCharacterAI().getState().getEnterSubState();
                if (statePacket != null) {
                    this.characterId.getCharacter().getNetworkCharacterAI().getState().removeEnterSubState(statePacket);
                }

                this.characterId.getCharacter().getNetworkCharacterAI().getState().addEnterSubState(this);
            } else {
                StatePacket statePacket = this.characterId.getCharacter().getNetworkCharacterAI().getState().getEnterState();
                if (statePacket != null) {
                    this.characterId.getCharacter().getNetworkCharacterAI().getState().removeEnterState(statePacket);
                }

                this.characterId.getCharacter().getNetworkCharacterAI().getState().addEnterState(this);
            }

            if (this.state.getState().isProcessedOnEnter()) {
                this.state.getState().processOnEnter(this.characterId.getCharacter(), this.params.delegate);
            }
        } else if (State.Stage.Execute == this.stage) {
            if (FitnessState.instance() == this.getState() && this.characterId.getCharacter() instanceof IsoPlayer player) {
                player.getFitness().exerciseRepeat();
            }

            return;
        }

        this.sendToRelativeClients(packetType, connection, this.getX(), this.getY());
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.characterId.isConsistent(connection) && this.state.isConsistent(connection) && this.params != null;
    }

    public boolean isAway() {
        float distance = IsoUtils.DistanceManhatten(this.getX(), this.getY(), this.characterId.getCharacter().getX(), this.characterId.getCharacter().getY());
        return distance > 0.2F || PZMath.fastfloor(this.getZ()) != PZMath.fastfloor(this.characterId.getCharacter().getZ());
    }

    public boolean isReady() {
        return this.isSubState
            || this.getCharacter().getCurrentState() == null
            || this.getCharacter().isCurrentState(this.getState())
            || this.getCharacter().isCurrentState(ClimbSheetRopeState.instance())
                && (this.getState() == ClimbDownSheetRopeState.instance() || this.getState() == PlayerFallingState.instance())
            || this.getCharacter().isCurrentState(ClimbDownSheetRopeState.instance())
                && (this.getState() == ClimbSheetRopeState.instance() || this.getState() == PlayerFallingState.instance())
            || this.getCharacter().isCurrentState(IdleState.instance())
                && (this.getCharacter().getStateMachine().getSubStateCount() == 0 || this.getCharacter().getStateMachine().getSubStateAt(0) == null);
    }

    public IsoGameCharacter getCharacter() {
        return this.characterId.getCharacter();
    }

    public State getState() {
        return this.state.getState();
    }

    public State.Stage getStage() {
        return this.stage;
    }

    public Variables getEvents() {
        return this.events;
    }

    public void update(StatePacket packet) {
        this.events.get().putAll(packet.events.get());
        this.timestamp = packet.timestamp;
    }

    public void apply() {
        HashMap<Object, Object> currentStateMachineParams = this.getCharacter().getStateMachineParams(this.state.getState());
        currentStateMachineParams.clear();
        currentStateMachineParams.putAll(this.params.delegate);
        this.state.getState().setParams(this.getCharacter(), this.stage);
        if (this.state.getState().isSyncOnSquare()) {
            for (Entry<String, String> event : this.events.get().entrySet()) {
                if (StringUtils.isNullOrEmpty(event.getValue())) {
                    boolean isClimbEvent = !StringUtils.isNullOrEmpty(event.getKey()) && event.getKey().startsWith("EventClimb");
                    if (!PlayerActionsState.instance().equals(this.getState()) || !isClimbEvent) {
                        this.getCharacter().reportEvent(event.getKey());
                    }
                }
            }
        }
    }

    private void setParam(Object key, Object val) {
        this.params.rawset(key, val);
    }
}
