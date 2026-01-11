// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.character;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.IPositional;

public class CharacterID implements INetworkPacketField, IPositional {
    public static final byte UNKNOWN = 0;
    public static final byte PLAYER = 1;
    public static final byte ZOMBIE = 2;
    public static final byte ANIMAL = 3;
    @JSONField
    private final PlayerID playerId = new PlayerID();
    @JSONField
    private final ZombieID zombieId = new ZombieID();
    @JSONField
    private final AnimalID animalId = new AnimalID();
    @JSONField
    private byte type = 0;

    public void set(short ID, byte type) {
        this.type = type;
        switch (type) {
            case 1:
                this.playerId.setID(ID);
                break;
            case 2:
                this.zombieId.setID(ID);
                break;
            case 3:
                this.animalId.setID(ID);
        }
    }

    public void set(IsoGameCharacter character) {
        if (character instanceof IsoAnimal isoAnimal) {
            this.type = 3;
            this.animalId.set(isoAnimal);
        } else if (character instanceof IsoPlayer isoPlayer) {
            this.type = 1;
            this.playerId.set(isoPlayer);
        } else if (character instanceof IsoZombie isoZombie) {
            this.type = 2;
            this.zombieId.set(isoZombie);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.type = b.get();
        switch (this.type) {
            case 1:
                this.playerId.parse(b, connection);
                break;
            case 2:
                this.zombieId.parse(b, connection);
                break;
            case 3:
                this.animalId.parse(b, connection);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        b.put(this.type);
        switch (this.type) {
            case 1:
                this.playerId.write(b);
                break;
            case 2:
                this.zombieId.write(b);
                break;
            case 3:
                this.animalId.write(b);
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        boolean isConsistent = false;
        switch (this.type) {
            case 1:
                isConsistent = this.playerId.isConsistent(connection);
                break;
            case 2:
                isConsistent = this.zombieId.isConsistent(connection);
                break;
            case 3:
                isConsistent = this.animalId.isConsistent(connection);
        }

        return isConsistent;
    }

    @Override
    public float getX() {
        switch (this.type) {
            case 1:
                this.playerId.getX();
                break;
            case 2:
                this.zombieId.getX();
                break;
            case 3:
                this.animalId.getX();
        }

        return 0.0F;
    }

    @Override
    public float getY() {
        switch (this.type) {
            case 1:
                this.playerId.getY();
                break;
            case 2:
                this.zombieId.getY();
                break;
            case 3:
                this.animalId.getY();
        }

        return 0.0F;
    }

    @Override
    public float getZ() {
        switch (this.type) {
            case 1:
                this.playerId.getZ();
                break;
            case 2:
                this.zombieId.getZ();
                break;
            case 3:
                this.animalId.getZ();
        }

        return 0.0F;
    }

    public IsoGameCharacter getCharacter() {
        IsoGameCharacter character = null;
        switch (this.type) {
            case 1:
                character = this.playerId.getPlayer();
                break;
            case 2:
                character = this.zombieId.getZombie();
                break;
            case 3:
                character = this.animalId.getAnimal();
        }

        return character;
    }

    public short getID() {
        short id = -1;
        switch (this.type) {
            case 1:
                id = this.playerId.getID();
                break;
            case 2:
                id = this.zombieId.getID();
                break;
            case 3:
                id = this.animalId.getID();
        }

        return id;
    }
}
