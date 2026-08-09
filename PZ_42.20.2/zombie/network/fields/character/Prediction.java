// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.character;

import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.Vector3;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.packets.INetworkPacket;

public class Prediction implements INetworkPacket {
    @JSONField
    public byte type = 0;
    @JSONField
    public float x;
    @JSONField
    public float y;
    @JSONField
    public byte z;
    @JSONField
    public float direction;
    @JSONField
    public float moveDirection;
    @JSONField
    public float speed;
    @JSONField
    public byte distance;
    @JSONField
    public float pathFindX;
    @JSONField
    public float pathFindY;
    public final Vector3 position = new Vector3();
    public float predictionTimeRemaining;
    public static final float DISTANCE_SCALE = 8.0F;
    public static final byte MAX_LERP_DISTANCE = 10;

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.type = b.getByte();
        this.x = b.getFloat();
        this.y = b.getFloat();
        this.z = b.getByte();
        this.direction = b.getFloat();
        this.moveDirection = b.getFloat();
        this.speed = b.getFloat();
        this.distance = b.getByte();
        if (this.type == 2) {
            this.pathFindX = b.getFloat();
            this.pathFindY = b.getFloat();
        } else {
            this.pathFindX = this.x;
            this.pathFindY = this.y;
        }

        this.position.x = this.x;
        this.position.y = this.y;
        this.position.z = this.z;
        this.predictionTimeRemaining = this.distance > 0 ? this.distance / (this.speed * 8.0F) : 0.0F;
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        b.put(this.type);
        b.putFloat(this.x);
        b.putFloat(this.y);
        b.put(this.z);
        b.putFloat(this.direction);
        b.putFloat(this.moveDirection);
        b.putFloat(this.speed);
        b.put(this.distance);
        if (this.type == 2) {
            b.putFloat(this.pathFindX);
            b.putFloat(this.pathFindY);
        }
    }

    public void copy(Prediction other) {
        this.type = other.type;
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.direction = other.direction;
        this.moveDirection = other.moveDirection;
        this.speed = other.speed;
        this.distance = other.distance;
        this.pathFindX = other.pathFindX;
        this.pathFindY = other.pathFindY;
        this.position.set(other.position);
        this.predictionTimeRemaining = other.predictionTimeRemaining;
    }

    public void update() {
        if (!(this.predictionTimeRemaining <= 0.0F)) {
            float delta = GameTime.instance.getTimeDelta();
            this.predictionTimeRemaining -= delta;
            this.position
                .set(
                    delta * this.speed * (float)Math.cos(this.moveDirection) + this.position.x,
                    delta * this.speed * (float)Math.sin(this.moveDirection) + this.position.y,
                    this.position.z
                );
        }
    }

    public void updateLerp(IsoPlayer player) {
        if (!(this.predictionTimeRemaining <= 0.0F)) {
            float totalPredictionTime = this.distance / (this.speed * 8.0F);
            float delta = GameTime.instance.getTimeDelta();
            this.predictionTimeRemaining -= delta;
            float progress = totalPredictionTime > 0.0F ? 1.0F - this.predictionTimeRemaining / totalPredictionTime : 1.0F;
            progress = Math.clamp(progress, 0.0F, 1.0F);
            this.position.x = PZMath.lerp(player.getX(), this.x, progress);
            this.position.y = PZMath.lerp(player.getY(), this.y, progress);
        }
    }
}
