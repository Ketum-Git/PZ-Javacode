// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.character;

import java.nio.ByteBuffer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.fields.IDShort;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.IPositional;
import zombie.popman.animal.AnimalInstanceManager;

public class AnimalID extends IDShort implements INetworkPacketField, IPositional {
    protected IsoAnimal animal;

    public void set(IsoAnimal animal) {
        this.setID(animal.getOnlineID());
        this.animal = animal;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.animal = AnimalInstanceManager.getInstance().get(this.getID());
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    public IsoAnimal getAnimal() {
        return this.animal;
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.getAnimal() != null;
    }

    @Override
    public String toString() {
        return this.animal == null ? "?" : this.animal.getAnimalType() + "(" + this.getID() + ")";
    }

    public void copy(AnimalID other) {
        this.setID(other.getID());
        this.animal = other.animal;
    }

    @Override
    public float getX() {
        return this.animal != null ? this.animal.getX() : 0.0F;
    }

    @Override
    public float getY() {
        return this.animal != null ? this.animal.getY() : 0.0F;
    }

    @Override
    public float getZ() {
        return this.animal != null ? this.animal.getZ() : 0.0F;
    }

    public boolean isRelevant(UdpConnection connection) {
        return connection.RelevantTo(this.getX(), this.getX());
    }
}
