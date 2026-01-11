// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import zombie.core.math.PZMath;
import zombie.iso.sprite.IsoSprite;

public class IsoFloorBloodSplat {
    public static final float FADE_HOURS = 72.0F;
    public static HashMap<String, IsoSprite> spriteMap = new HashMap<>();
    public static final String[] FLOOR_BLOOD_TYPES = new String[]{
        "blood_floor_small_01",
        "blood_floor_small_02",
        "blood_floor_small_03",
        "blood_floor_small_04",
        "blood_floor_small_05",
        "blood_floor_small_06",
        "blood_floor_small_07",
        "blood_floor_small_08",
        "blood_floor_med_01",
        "blood_floor_med_02",
        "blood_floor_med_03",
        "blood_floor_med_04",
        "blood_floor_med_05",
        "blood_floor_med_06",
        "blood_floor_med_07",
        "blood_floor_med_08",
        "blood_floor_large_01",
        "blood_floor_large_02",
        "blood_floor_large_03",
        "blood_floor_large_04",
        "blood_floor_large_05"
    };
    public float x;
    public float y;
    public float z;
    public int type;
    public float worldAge;
    public int index;
    public int fade;
    public IsoChunk chunk;

    public IsoFloorBloodSplat() {
    }

    public IsoFloorBloodSplat(float x, float y, float z, int Type, float worldAge) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = Type;
        this.worldAge = worldAge;
    }

    public void save(ByteBuffer output) {
        int x = PZMath.fastfloor(this.x / 8.0F * 255.0F);
        if (x < 0) {
            x = 0;
        }

        if (x > 255) {
            x = 255;
        }

        int y = PZMath.fastfloor(this.y / 8.0F * 255.0F);
        if (y < 0) {
            y = 0;
        }

        if (y > 255) {
            y = 255;
        }

        int z = PZMath.fastfloor(this.z / 8.0F * 255.0F);
        if (z < 0) {
            z = 0;
        }

        if (z > 255) {
            z = 255;
        }

        output.put((byte)x);
        output.put((byte)y);
        output.put((byte)z);
        output.put((byte)this.type);
        output.putFloat(this.worldAge);
        output.put((byte)this.index);
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.x = PZMath.fastfloor((float)(input.get() & 255)) / 255.0F * 8.0F;
        this.y = PZMath.fastfloor((float)(input.get() & 255)) / 255.0F * 8.0F;
        this.z = PZMath.fastfloor((float)(input.get() & 255)) / 255.0F * 8.0F;
        this.type = input.get();
        this.worldAge = input.getFloat();
        this.index = input.get();
    }
}
