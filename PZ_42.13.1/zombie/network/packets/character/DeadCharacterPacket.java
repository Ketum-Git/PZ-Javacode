// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.JSONField;
import zombie.network.fields.Position;
import zombie.network.fields.character.CharacterID;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.INetworkPacket;

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
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.objectId.load(b);
        this.characterId.parse(b, connection);
        this.killerId.parse(b, connection);
        this.angle = b.getFloat();
        this.isFallOnFront = b.get() != 0;
        this.reanimateTime = b.getFloat();
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.characterId.getCharacter() != null;
    }

    @Override
    public void postpone() {
        this.characterId.getCharacter().getNetworkCharacterAI().setCorpse(this);
    }

    @Override
    public boolean isPostponed() {
        return true;
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare((double)this.x, (double)this.y, (double)this.z);
        if (this.characterId.getCharacter().getCurrentSquare() != square) {
            DebugLog.Multiplayer
                .debugln(
                    String.format(
                        "Corpse %s(%d) teleport: position (%f ; %f) => (%f ; %f)",
                        this.characterId.getCharacter().getClass().getSimpleName(),
                        this.characterId.getID(),
                        this.characterId.getCharacter().getX(),
                        this.characterId.getCharacter().getY(),
                        this.x,
                        this.y
                    )
                );
            this.characterId.getCharacter().setX(this.x);
            this.characterId.getCharacter().setY(this.y);
            this.characterId.getCharacter().setZ(this.z);
            this.characterId.getCharacter().setLastX(this.x);
            this.characterId.getCharacter().setLastY(this.y);
            this.characterId.getCharacter().setLastZ(this.z);
        }

        if (this.characterId.getCharacter().getAnimAngleRadians() - this.angle > 1.0E-4F) {
            DebugLog.Multiplayer
                .debugln(
                    String.format(
                        "Corpse %s(%d) teleport: direction (%f) => (%f)",
                        this.characterId.getCharacter().getClass().getSimpleName(),
                        this.characterId.getID(),
                        this.characterId.getCharacter().getAnimAngleRadians(),
                        this.angle
                    )
                );
            if (this.characterId.getCharacter().hasAnimationPlayer()
                && this.characterId.getCharacter().getAnimationPlayer().isReady()
                && !this.characterId.getCharacter().getAnimationPlayer().isBoneTransformsNeedFirstFrame()) {
                this.characterId.getCharacter().getAnimationPlayer().setAngle(this.angle);
            }
        }

        if (this.isFallOnFront != this.characterId.getCharacter().isFallOnFront()) {
            DebugLog.Multiplayer
                .debugln(
                    String.format(
                        "Corpse %s(%d) teleport: pose (%s) => (%s)",
                        this.characterId.getCharacter().getClass().getSimpleName(),
                        this.characterId.getID(),
                        this.characterId.getCharacter().isFallOnFront() ? "front" : "back",
                        this.isFallOnFront ? "front" : "back"
                    )
                );
            this.characterId.getCharacter().setFallOnFront(this.isFallOnFront);
        }

        this.characterId.getCharacter().setCurrent(square);
        this.characterId.getCharacter().setAttackedBy(this.killerId.getCharacter());
        IsoDeadBody body = this.characterId.getCharacter().becomeCorpse();
        body.getObjectID().set(this.objectId);
        ObjectIDManager.getInstance().addObject(body);
        body.setCharacterOnlineID(this.characterId.getID());
        body.setReanimateTime(this.reanimateTime);
        this.characterId.getCharacter().setStateMachineLocked(true);
    }

    protected void writeCharacterInventory(ByteBufferWriter b) {
        if (this.body != null) {
            this.body.writeInventory(b.bb);
        }
    }

    protected void parseCharacterInventory(ByteBuffer b) {
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
