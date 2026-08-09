// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.Position;
import zombie.network.fields.character.CharacterID;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.objects.SoundKey;

public abstract class DeadCharacterPacket extends Position implements INetworkPacket {
    @JSONField
    protected final ObjectID objectId = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
    @JSONField
    protected final CharacterID characterId = new CharacterID();
    @JSONField
    protected final CharacterID killerId = new CharacterID();
    @JSONField
    protected float angle;
    @JSONField
    protected boolean isFallOnFront;
    @JSONField
    protected float reanimateTime;
    @JSONField
    protected short index;
    private IsoDeadBody body;

    @Override
    public void setData(Object... values) {
        this.set((IsoDeadBody)values[0]);
    }

    protected void set(IsoDeadBody body) {
        this.set(body.getX(), body.getY(), body.getZ());
        if (body.isZombie()) {
            this.characterId.set(body.getCharacterOnlineID(), (byte)2);
        } else if (body.isAnimal()) {
            this.characterId.set(body.getCharacterOnlineID(), (byte)3);
        } else if (body.isPlayer()) {
            this.characterId.set(body.getCharacterOnlineID(), (byte)1);
        }

        this.objectId.set(body.getObjectID());
        this.killerId.set(body.getKilledBy());
        this.angle = body.getAngle();
        this.isFallOnFront = body.isFallOnFront();
        this.reanimateTime = body.getReanimateTime();
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare((double)this.x, (double)this.y, (double)this.z);
        this.index = (short)square.getStaticMovingObjects().indexOf(body);
        this.body = body;
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        this.objectId.save(b.bb);
        this.characterId.write(b);
        this.killerId.write(b);
        b.putFloat(this.angle);
        b.putBoolean(this.isFallOnFront);
        b.putFloat(this.reanimateTime);
        b.putShort(this.index);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.objectId.parse(b, connection);
        this.characterId.parse(b, connection);
        this.killerId.parse(b, connection);
        this.angle = b.getFloat();
        this.isFallOnFront = b.getBoolean();
        this.reanimateTime = b.getFloat();
        this.index = b.getShort();
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.characterId.getCharacter() != null;
    }

    @Override
    public void postpone() {
        this.characterId
            .getCharacter()
            .getNetworkCharacterAI()
            .setCorpse(this, IsoWorld.instance.currentCell.getGridSquare((double)this.x, (double)this.y, (double)this.z), this.index);
    }

    @Override
    public boolean isPostponed() {
        return true;
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare((double)this.x, (double)this.y, (double)this.z);
        IsoGameCharacter character = this.characterId.getCharacter();
        if (character.getCurrentSquare() != square) {
            DebugType.Multiplayer
                .debugln(
                    String.format(
                        "Corpse %s(%d) teleport: position (%f ; %f) => (%f ; %f)",
                        character.getClass().getSimpleName(),
                        this.characterId.getID(),
                        character.getX(),
                        character.getY(),
                        this.x,
                        this.y
                    )
                );
            character.setX(this.x);
            character.setY(this.y);
            character.setZ(this.z);
            character.setLastX(this.x);
            character.setLastY(this.y);
            character.setLastZ(this.z);
        }

        if (character.getAnimAngleRadians() - this.angle > 1.0E-4F) {
            DebugType.Multiplayer
                .debugln(
                    String.format(
                        "Corpse %s(%d) teleport: direction (%f) => (%f)",
                        character.getClass().getSimpleName(),
                        this.characterId.getID(),
                        character.getAnimAngleRadians(),
                        this.angle
                    )
                );
            character.setDirectionAngle(this.angle * (180.0F / (float)Math.PI));
            if (character.hasAnimationPlayer() && character.getAnimationPlayer().isReady() && !character.getAnimationPlayer().isBoneTransformsNeedFirstFrame()) {
                character.getAnimationPlayer().setTargetAngle(this.angle);
                character.getAnimationPlayer().setAngleToTarget();
            }
        }

        if (this.isFallOnFront != character.isFallOnFront()) {
            DebugType.Multiplayer
                .debugln(
                    String.format(
                        "Corpse %s(%d) teleport: pose (%s) => (%s)",
                        character.getClass().getSimpleName(),
                        this.characterId.getID(),
                        character.isFallOnFront() ? "front" : "back",
                        this.isFallOnFront ? "front" : "back"
                    )
                );
            character.setFallOnFront(this.isFallOnFront);
        }

        character.setCurrent(square);
        if (character instanceof IsoPlayer player
            && !player.isAnimal()
            && player.getVehicle() == null
            && !player.getEmitter().isPlaying(SoundKey.BODY_HIT_GROUND.getSoundName())) {
            player.getEmitter().playSoundImpl(SoundKey.BODY_HIT_GROUND.getSoundName(), null);
        }

        ObjectID _objectId = this.objectId.clone();
        CharacterID _characterId = this.characterId.clone();
        float _reanimateTime = this.reanimateTime;
        character.dieNetwork(this.killerId.getCharacter(), null, false, (died, body) -> {
            body.getObjectID().set(_objectId);
            ObjectIDManager.getInstance().addObject(body);
            body.setCharacterOnlineID(_characterId.getID());
            body.setReanimateTime(_reanimateTime);
            died.setStateMachineLocked(true);
        });
    }

    protected void writeCharacterInventory(ByteBufferWriter b) {
        if (this.body != null) {
            this.body.writeInventory(b);
        }
    }

    protected void parseCharacterInventory(ByteBufferReader b) {
        if (this.characterId.getCharacter() != null) {
            if (this.characterId.getCharacter().getContainer() != null) {
                this.characterId.getCharacter().getContainer().clear();
            }

            if (this.characterId.getCharacter().getInventory() != null) {
                this.characterId.getCharacter().getInventory().clear();
            }

            if (this.characterId.getCharacter().getWornItems() != null) {
                this.characterId.getCharacter().getWornItems().clear();
            }

            if (this.characterId.getCharacter().getAttachedItems() != null) {
                this.characterId.getCharacter().getAttachedItems().clear();
            }

            this.characterId.getCharacter().getInventory().setSourceGrid(this.characterId.getCharacter().getCurrentSquare());
            this.characterId.getCharacter().readInventory(b);
            this.characterId.getCharacter().resetModelNextFrame();
        }
    }
}
