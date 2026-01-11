// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.IPositional;
import zombie.network.fields.Square;
import zombie.util.Type;

public class Thumpable extends Square implements IPositional, INetworkPacketField {
    @JSONField
    protected int index;
    protected IsoObject isoObject;

    public void set(IsoObject isoObject) {
        this.set(isoObject.getSquare());
        this.index = isoObject.getObjectIndex();
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.index = b.getInt();
        if (this.square != null && this.index < this.square.getObjects().size()) {
            this.isoObject = this.square.getObjects().get(this.index);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putInt(this.index);
    }

    @Override
    public void process(IsoGameCharacter character) {
        IsoAnimal animal = Type.tryCastTo(character, IsoAnimal.class);
        IsoZombie zombie = Type.tryCastTo(character, IsoZombie.class);
        IsoThumpable thumpable = Type.tryCastTo(this.isoObject, IsoThumpable.class);
        IsoDoor door = Type.tryCastTo(this.isoObject, IsoDoor.class);
        if (GameServer.server) {
            if (thumpable != null) {
                if (animal != null) {
                    thumpable.animalHit(animal);
                    if (thumpable.health <= 0.0F) {
                        thumpable.destroy();
                        animal.thumpTarget = null;
                        return;
                    }
                }

                if (zombie != null) {
                    thumpable.Thump(zombie);
                }
            }

            if (door != null && zombie != null) {
                door.Thump(zombie);
            }
        } else if (GameClient.client && animal != null) {
            animal.thumpTarget = this.isoObject;
            animal.setPath2(null);
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.isoObject != null;
    }

    public boolean isRelevant(UdpConnection connection) {
        return connection.RelevantTo(this.getX(), this.getY());
    }

    public IsoObject getIsoObject() {
        return this.isoObject;
    }

    public String getName() {
        return this.isoObject.getName() != null ? this.isoObject.getName() : this.isoObject.getObjectName();
    }
}
