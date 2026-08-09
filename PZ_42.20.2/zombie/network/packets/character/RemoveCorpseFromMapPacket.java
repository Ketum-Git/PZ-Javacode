// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.characters.IsoGameCharacter;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.CharacterID;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class RemoveCorpseFromMapPacket implements INetworkPacket {
    protected final ObjectID objectId = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
    protected final CharacterID characterId = new CharacterID();
    private IsoDeadBody deadBody;

    public void set(IsoDeadBody deadBody) {
        this.deadBody = deadBody;
        this.objectId.set(deadBody.getObjectID());
        if (deadBody.isZombie()) {
            this.characterId.set(deadBody.getCharacterOnlineID(), (byte)2);
        } else if (deadBody.isAnimal()) {
            this.characterId.set(deadBody.getCharacterOnlineID(), (byte)3);
        } else if (deadBody.isPlayer()) {
            this.characterId.set(deadBody.getCharacterOnlineID(), (byte)1);
        }
    }

    @Override
    public void setData(Object... values) {
        this.set((IsoDeadBody)values[0]);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.objectId.load(b.bb);
        this.characterId.parse(b, connection);
        this.deadBody = (IsoDeadBody)this.objectId.getObject();
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.objectId.save(b.bb);
        this.characterId.write(b);
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoDeadBody.removeDeadBody(this.objectId);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoDeadBody.removeDeadBody(this.objectId);
        INetworkPacket.sendToRelative(PacketTypes.PacketType.RemoveCorpseFromMap, this.deadBody.getX(), this.deadBody.getY(), this.deadBody);
    }

    @Override
    public String getDescription() {
        return String.format(this.getClass().getSimpleName() + " id=%s", this.objectId);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.characterId.getCharacter() != null ? true : this.deadBody != null && this.deadBody.getSquare() != null;
    }

    public boolean isRelevant(UdpConnection connection) {
        if (this.deadBody != null) {
            return connection.isRelevantTo(this.deadBody.getX(), this.deadBody.getY());
        } else {
            IsoGameCharacter character = this.characterId.getCharacter();
            return character != null ? connection.isRelevantTo(character.getX(), character.getY()) : false;
        }
    }

    @Override
    public void postpone() {
        this.characterId.getCharacter().getNetworkCharacterAI().setRemoveCorpse(this);
    }

    @Override
    public boolean isPostponed() {
        return this.deadBody == null && this.characterId.getCharacter() != null;
    }
}
