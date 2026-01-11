// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import java.nio.ByteBuffer;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import zombie.core.math.PZMath;
import zombie.core.physics.Transform;
import zombie.iso.Vector2;
import zombie.scripting.objects.VehicleScript;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.QuadranglesIntersection;

public final class VehiclePoly {
    private static final Vector3f tempVec3f_1 = new Vector3f();
    public Transform t = new Transform();
    public float x1;
    public float y1;
    public float x2;
    public float y2;
    public float x3;
    public float y3;
    public float x4;
    public float y4;
    public float z;
    public final Vector2[] borders = new Vector2[4];
    private static final Quaternionf tempQuat = new Quaternionf();

    public VehiclePoly() {
        for (int i = 0; i < this.borders.length; i++) {
            this.borders[i] = new Vector2();
        }
    }

    public VehiclePoly init(VehiclePoly other) {
        this.x1 = other.x1;
        this.y1 = other.y1;
        this.x2 = other.x2;
        this.y2 = other.y2;
        this.x3 = other.x3;
        this.y3 = other.y3;
        this.x4 = other.x4;
        this.y4 = other.y4;
        this.z = other.z;

        for (int i = 0; i < 4; i++) {
            this.borders[i].set(other.borders[i]);
        }

        return this;
    }

    public VehiclePoly init(BaseVehicle vehicle, float RADIUS) {
        VehicleScript script = vehicle.getScript();
        Vector3f ext = script.getExtents();
        Vector3f com = script.getCenterOfMassOffset();
        float scale = 1.0F;
        Vector2[] coords = this.borders;
        Quaternionf q = tempQuat;
        vehicle.getWorldTransform(this.t);
        this.t.getRotation(q);
        float width = ext.x * 1.0F + RADIUS * 2.0F;
        float length = ext.z * 1.0F + RADIUS * 2.0F;
        float height = ext.y * 1.0F + RADIUS * 2.0F;
        width /= 2.0F;
        length /= 2.0F;
        height /= 2.0F;
        Vector3f worldPos = tempVec3f_1;
        if (q.x < 0.0F) {
            vehicle.getWorldPos(com.x - width, 0.0F, com.z + length, worldPos);
            coords[0].set(worldPos.x, worldPos.y);
            vehicle.getWorldPos(com.x + width, height, com.z + length, worldPos);
            coords[1].set(worldPos.x, worldPos.y);
            vehicle.getWorldPos(com.x + width, height, com.z - length, worldPos);
            coords[2].set(worldPos.x, worldPos.y);
            vehicle.getWorldPos(com.x - width, 0.0F, com.z - length, worldPos);
            coords[3].set(worldPos.x, worldPos.y);
            this.z = vehicle.getZ();
        } else {
            vehicle.getWorldPos(com.x - width, height, com.z + length, worldPos);
            coords[0].set(worldPos.x, worldPos.y);
            vehicle.getWorldPos(com.x + width, 0.0F, com.z + length, worldPos);
            coords[1].set(worldPos.x, worldPos.y);
            vehicle.getWorldPos(com.x + width, 0.0F, com.z - length, worldPos);
            coords[2].set(worldPos.x, worldPos.y);
            vehicle.getWorldPos(com.x - width, height, com.z - length, worldPos);
            coords[3].set(worldPos.x, worldPos.y);
            this.z = vehicle.getZ();
        }

        int sum = 0;

        for (int i = 0; i < coords.length; i++) {
            Vector2 v1 = coords[i];
            Vector2 v2 = coords[(i + 1) % coords.length];
            sum = (int)(sum + (v2.x - v1.x) * (v2.y + v1.y));
        }

        if (sum < 0) {
            Vector2 v1 = coords[1];
            Vector2 v2 = coords[2];
            Vector2 v3 = coords[3];
            coords[1] = v3;
            coords[2] = v2;
            coords[3] = v1;
        }

        this.x1 = coords[0].x;
        this.y1 = coords[0].y;
        this.x2 = coords[1].x;
        this.y2 = coords[1].y;
        this.x3 = coords[2].x;
        this.y3 = coords[2].y;
        this.x4 = coords[3].x;
        this.y4 = coords[3].y;
        return this;
    }

    public static Vector2 lineIntersection(Vector2 start1, Vector2 end1, Vector2 start2, Vector2 end2) {
        Vector2 p = new Vector2();
        float A1 = start1.y - end1.y;
        float B1 = end1.x - start1.x;
        float C1 = -A1 * start1.x - B1 * start1.y;
        float A2 = start2.y - end2.y;
        float B2 = end2.x - start2.x;
        float C2 = -A2 * start2.x - B2 * start2.y;
        float zn = QuadranglesIntersection.det(A1, B1, A2, B2);
        if (zn != 0.0F) {
            p.x = -QuadranglesIntersection.det(C1, B1, C2, B2) * 1.0F / zn;
            p.y = -QuadranglesIntersection.det(A1, C1, A2, C2) * 1.0F / zn;
            return p;
        } else {
            return null;
        }
    }

    VehicleRect getAABB(VehicleRect rect) {
        float minX = Math.min(this.x1, Math.min(this.x2, Math.min(this.x3, this.x4)));
        float minY = Math.min(this.y1, Math.min(this.y2, Math.min(this.y3, this.y4)));
        float maxX = Math.max(this.x1, Math.max(this.x2, Math.max(this.x3, this.x4)));
        float maxY = Math.max(this.y1, Math.max(this.y2, Math.max(this.y3, this.y4)));
        return rect.init(
            null,
            PZMath.fastfloor(minX),
            PZMath.fastfloor(minY),
            (int)Math.ceil(maxX) - PZMath.fastfloor(minX),
            (int)Math.ceil(maxY) - PZMath.fastfloor(minY),
            PZMath.fastfloor(this.z)
        );
    }

    float isLeft(float x0, float y0, float x1, float y1, float x2, float y2) {
        return (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
    }

    public boolean containsPoint(float x, float y) {
        int wn = 0;

        for (int i = 0; i < 4; i++) {
            Vector2 v1 = this.borders[i];
            Vector2 v2 = i == 3 ? this.borders[0] : this.borders[i + 1];
            if (v1.y <= y) {
                if (v2.y > y && this.isLeft(v1.x, v1.y, v2.x, v2.y, x, y) > 0.0F) {
                    wn++;
                }
            } else if (v2.y <= y && this.isLeft(v1.x, v1.y, v2.x, v2.y, x, y) < 0.0F) {
                wn--;
            }
        }

        return wn != 0;
    }

    public boolean isEqual(VehiclePoly other) {
        return PZMath.equal(this.x1, other.x1, 0.001F)
            && PZMath.equal(this.y1, other.y1, 0.001F)
            && PZMath.equal(this.x2, other.x2, 0.001F)
            && PZMath.equal(this.y2, other.y2, 0.001F)
            && PZMath.equal(this.x3, other.x3, 0.001F)
            && PZMath.equal(this.y3, other.y3, 0.001F)
            && PZMath.equal(this.x4, other.x4, 0.001F)
            && PZMath.equal(this.y4, other.y4, 0.001F);
    }

    public void toByteBuffer(ByteBuffer bb) {
        bb.putFloat(this.x1);
        bb.putFloat(this.y1);
        bb.putFloat(this.x2);
        bb.putFloat(this.y2);
        bb.putFloat(this.x3);
        bb.putFloat(this.y3);
        bb.putFloat(this.x4);
        bb.putFloat(this.y4);
        bb.putFloat(this.z + 32.0F);
    }
}
