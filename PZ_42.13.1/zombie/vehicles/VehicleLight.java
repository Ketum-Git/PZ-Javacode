// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.joml.Vector3f;
import zombie.UsedFromLua;

@UsedFromLua
public final class VehicleLight {
    public boolean active;
    public final Vector3f offset = new Vector3f();
    public float dist = 16.0F;
    public float intensity = 1.0F;
    public float dot = 0.96F;
    public int focusing;

    public boolean getActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Deprecated
    public int getFocusing() {
        return this.focusing;
    }

    public float getIntensity() {
        return this.intensity;
    }

    @Deprecated
    public float getDistanization() {
        return this.dist;
    }

    @Deprecated
    public boolean canFocusingUp() {
        return this.focusing != 0;
    }

    @Deprecated
    public boolean canFocusingDown() {
        return this.focusing != 1;
    }

    @Deprecated
    public void setFocusingUp() {
        if (this.focusing != 0) {
            if (this.focusing < 4) {
                this.focusing = 4;
            } else if (this.focusing < 10) {
                this.focusing = 10;
            } else if (this.focusing < 30) {
                this.focusing = 30;
            } else if (this.focusing < 100) {
                this.focusing = 100;
            } else {
                this.focusing = 0;
            }
        }
    }

    @Deprecated
    public void setFocusingDown() {
        if (this.focusing != 1) {
            if (this.focusing == 0) {
                this.focusing = 100;
            } else if (this.focusing > 30) {
                this.focusing = 30;
            } else if (this.focusing > 10) {
                this.focusing = 10;
            } else if (this.focusing > 4) {
                this.focusing = 4;
            } else {
                this.focusing = 1;
            }
        }
    }

    public void save(ByteBuffer output) throws IOException {
        output.put((byte)(this.active ? 1 : 0));
        output.putFloat(this.offset.x);
        output.putFloat(this.offset.y);
        output.putFloat(this.intensity);
        output.putFloat(this.dist);
        output.putInt(this.focusing);
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.active = input.get() == 1;
        this.offset.x = input.getFloat();
        this.offset.y = input.getFloat();
        this.intensity = input.getFloat();
        this.dist = input.getFloat();
        this.focusing = input.getInt();
    }
}
