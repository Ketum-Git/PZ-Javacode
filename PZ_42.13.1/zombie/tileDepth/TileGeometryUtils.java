// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.Core;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.UI3DScene;

public final class TileGeometryUtils {
    private static final UI3DScene.Plane m_plane = new UI3DScene.Plane();
    private static final Vector3f m_viewRotation = new Vector3f(30.0F, 315.0F, 0.0F);
    private static final int[] m_viewport = new int[]{0, 0, 0, 0};
    private static final int m_viewWidth = 1;
    private static final int m_viewHeight = 2;

    static void calcMatricesForSquare(Matrix4f projection, Matrix4f modelView) {
        float S = (float)Math.sqrt(2.0);
        projection.setOrtho(-1.0F * S / 2.0F, 1.0F * S / 2.0F, -2.0F * S / 2.0F, 2.0F * S / 2.0F, -2.0F, 2.0F);
        projection.translate(0.0F, -2.0F * S * 0.375F, 0.0F);
        modelView.rotationXYZ(
            m_viewRotation.x * (float) (Math.PI / 180.0), m_viewRotation.y * (float) (Math.PI / 180.0), m_viewRotation.z * (float) (Math.PI / 180.0)
        );
    }

    public static float getDepthOnBoxAt(float tileX, float tileY, Vector3f center, Vector3f rotation, Vector3f min, Vector3f max) {
        Matrix4f projection = allocMatrix4f();
        Matrix4f modelView = allocMatrix4f();
        calcMatricesForSquare(projection, modelView);
        tileX *= 1.0F / (64.0F * Core.tileScale);
        tileY *= 2.0F / (128.0F * Core.tileScale);
        Vector3f scenePos = allocVector3f();
        UI3DScene.Ray cameraRay = getCameraRay(tileX, 2.0F - tileY, projection, modelView, 1, 2, UI3DScene.allocRay());
        Matrix4f boxMatrix = allocMatrix4f();
        boxMatrix.translation(center);
        boxMatrix.rotateXYZ(rotation.x * (float) (Math.PI / 180.0), rotation.y * (float) (Math.PI / 180.0), rotation.z * (float) (Math.PI / 180.0));
        boxMatrix.invert();
        boxMatrix.transformPosition(cameraRay.origin);
        boxMatrix.transformDirection(cameraRay.direction);
        releaseMatrix4f(boxMatrix);
        Vector2f nearFar = allocVector2f();
        boolean ok = intersectRayAab(
            cameraRay.origin.x,
            cameraRay.origin.y,
            cameraRay.origin.z,
            cameraRay.direction.x,
            cameraRay.direction.y,
            cameraRay.direction.z,
            min.x,
            min.y,
            min.z,
            max.x,
            max.y,
            max.z,
            nearFar
        );
        if (ok) {
            scenePos.set(cameraRay.origin).add(cameraRay.direction.mul(nearFar.x));
            Matrix4f boxMatrixx = allocMatrix4f();
            boxMatrixx.translation(center);
            boxMatrixx.rotateXYZ(rotation.x * (float) (Math.PI / 180.0), rotation.y * (float) (Math.PI / 180.0), rotation.z * (float) (Math.PI / 180.0));
            boxMatrixx.transformPosition(scenePos);
            releaseMatrix4f(boxMatrixx);
        }

        releaseVector2f(nearFar);
        UI3DScene.releaseRay(cameraRay);
        Matrix4f mvp = allocMatrix4f();
        mvp.set(projection);
        mvp.mul(modelView);
        mvp.transformPosition(scenePos);
        float depth = scenePos.z;
        releaseMatrix4f(mvp);
        releaseVector3f(scenePos);
        releaseMatrix4f(projection);
        releaseMatrix4f(modelView);
        return ok ? depth : 666.0F;
    }

    public static float getDepthOnCylinderAt(float tileX, float tileY, Vector3f center, Vector3f rotation, float radius, float zLength) {
        Matrix4f projection = allocMatrix4f();
        Matrix4f modelView = allocMatrix4f();
        calcMatricesForSquare(projection, modelView);
        tileX *= 1.0F / (64.0F * Core.tileScale);
        tileY *= 2.0F / (128.0F * Core.tileScale);
        Vector3f scenePos = allocVector3f();
        UI3DScene.Ray cameraRay = getCameraRay(tileX, 2.0F - tileY, projection, modelView, 1, 2, UI3DScene.allocRay());
        Matrix4f cylinderMatrix = allocMatrix4f();
        cylinderMatrix.translation(center);
        cylinderMatrix.rotateXYZ(rotation.x * (float) (Math.PI / 180.0), rotation.y * (float) (Math.PI / 180.0), rotation.z * (float) (Math.PI / 180.0));
        cylinderMatrix.invert();
        cylinderMatrix.transformPosition(cameraRay.origin);
        cylinderMatrix.transformDirection(cameraRay.direction);
        releaseMatrix4f(cylinderMatrix);
        CylinderUtils.IntersectionRecord intersectionRecord = new CylinderUtils.IntersectionRecord();
        boolean ok = CylinderUtils.intersect(radius, zLength, cameraRay, intersectionRecord);
        if (ok) {
            Matrix4f cylinderMatrixx = allocMatrix4f();
            cylinderMatrixx.translation(center);
            cylinderMatrixx.rotateXYZ(rotation.x * (float) (Math.PI / 180.0), rotation.y * (float) (Math.PI / 180.0), rotation.z * (float) (Math.PI / 180.0));
            cylinderMatrixx.transformPosition(intersectionRecord.location);
            cylinderMatrixx.transformDirection(intersectionRecord.normal);
            releaseMatrix4f(cylinderMatrixx);
            scenePos.set(intersectionRecord.location);
        }

        UI3DScene.releaseRay(cameraRay);
        Matrix4f mvp = allocMatrix4f();
        mvp.set(projection);
        mvp.mul(modelView);
        mvp.transformPosition(scenePos);
        float depth = scenePos.z;
        releaseMatrix4f(mvp);
        releaseVector3f(scenePos);
        releaseMatrix4f(projection);
        releaseMatrix4f(modelView);
        return ok ? depth : 666.0F;
    }

    public static float getDepthOnPlaneAt(float tileX, float tileY, UI3DScene.GridPlane gridPlane, Vector3f planePoint) {
        Vector3f normal = allocVector3f();
        switch (gridPlane) {
            case XY:
                normal.set(0.0F, 0.0F, 1.0F);
                break;
            case XZ:
                normal.set(0.0F, 1.0F, 0.0F);
                break;
            case YZ:
                normal.set(1.0F, 0.0F, 0.0F);
        }

        float depth = getDepthOnPlaneAt(tileX, tileY, planePoint, normal);
        releaseVector3f(normal);
        return depth;
    }

    public static float getDepthOnPlaneAt(float tileX, float tileY, Vector3f planePoint, Vector3f planeNormal) {
        return getDepthOnPlaneAt(tileX, tileY, planePoint, planeNormal, null);
    }

    public static float getDepthOnPlaneAt(float tileX, float tileY, Vector3f planePoint, Vector3f planeNormal, Vector3f pointOnPlane) {
        Matrix4f projection = allocMatrix4f();
        Matrix4f modelView = allocMatrix4f();
        calcMatricesForSquare(projection, modelView);
        m_plane.point.set(planePoint);
        m_plane.normal.set(planeNormal);
        tileX *= 1.0F / (64.0F * Core.tileScale);
        tileY *= 2.0F / (128.0F * Core.tileScale);
        Vector3f scenePos = allocVector3f();
        UI3DScene.Ray cameraRay = getCameraRay(tileX, 2.0F - tileY, projection, modelView, 1, 2, UI3DScene.allocRay());
        boolean ok = UI3DScene.intersect_ray_plane(m_plane, cameraRay, scenePos) == 1;
        UI3DScene.releaseRay(cameraRay);
        if (pointOnPlane != null) {
            pointOnPlane.set(scenePos);
        }

        Matrix4f mvp = allocMatrix4f();
        mvp.set(projection);
        mvp.mul(modelView);
        mvp.transformPosition(scenePos);
        float depth = scenePos.z;
        releaseMatrix4f(mvp);
        releaseVector3f(scenePos);
        releaseMatrix4f(projection);
        releaseMatrix4f(modelView);
        return ok ? depth : 666.0F;
    }

    static float getDepthOfScenePoint(float x, float y, float z) {
        Matrix4f projection = allocMatrix4f();
        Matrix4f modelView = allocMatrix4f();
        calcMatricesForSquare(projection, modelView);
        Matrix4f mvp = allocMatrix4f();
        mvp.set(projection);
        mvp.mul(modelView);
        Vector3f pos = allocVector3f().set(x, y, z);
        mvp.transformPosition(pos);
        float depth = pos.z;
        releaseMatrix4f(mvp);
        releaseVector3f(pos);
        releaseMatrix4f(projection);
        releaseMatrix4f(modelView);
        return depth;
    }

    static float getNormalizedDepth(float depth) {
        float depthNW = Math.abs(getDepthOfScenePoint(-0.5F, 0.0F, -0.5F));
        float scale = 1.0F / depthNW;
        scale *= 0.25F;
        float offset = 0.75F;
        return depth * scale + 0.75F;
    }

    public static float getNormalizedDepthOnBoxAt(float tileX, float tileY, Vector3f center, Vector3f rotation, Vector3f min, Vector3f max) {
        float depth = getDepthOnBoxAt(tileX, tileY, center, rotation, min, max);
        return depth == 666.0F ? -1.0F : getNormalizedDepth(depth);
    }

    public static float getNormalizedDepthOnCylinderAt(float tileX, float tileY, Vector3f center, Vector3f rotation, float radius, float zLength) {
        float depth = getDepthOnCylinderAt(tileX, tileY, center, rotation, radius, zLength);
        return depth == 666.0F ? -1.0F : getNormalizedDepth(depth);
    }

    public static float getNormalizedDepthOnPlaneAt(float tileX, float tileY, Vector3f planePoint, Vector3f planeNormal) {
        float depth = getDepthOnPlaneAt(tileX, tileY, planePoint, planeNormal);
        return depth == 666.0F ? -1.0F : getNormalizedDepth(depth);
    }

    public static float getNormalizedDepthOnPlaneAt(float tileX, float tileY, UI3DScene.GridPlane gridPlane, Vector3f planePoint) {
        float depth = getDepthOnPlaneAt(tileX, tileY, gridPlane, planePoint);
        return depth == 666.0F ? -1.0F : getNormalizedDepth(depth);
    }

    public static UI3DScene.Ray getCameraRay(
        float uiX, float uiY, Matrix4f projection, Matrix4f modelView, int viewWidth, int viewHeight, UI3DScene.Ray camera_ray
    ) {
        Matrix4f matrix4f = allocMatrix4f();
        matrix4f.set(projection);
        matrix4f.mul(modelView);
        matrix4f.invert();
        m_viewport[2] = viewWidth;
        m_viewport[3] = viewHeight;
        Vector3f ray_start = matrix4f.unprojectInv(uiX, uiY, 0.0F, m_viewport, allocVector3f());
        Vector3f ray_end = matrix4f.unprojectInv(uiX, uiY, 1.0F, m_viewport, allocVector3f());
        camera_ray.origin.set(ray_start);
        camera_ray.direction.set(ray_end.sub(ray_start).normalize());
        releaseVector3f(ray_end);
        releaseVector3f(ray_start);
        releaseMatrix4f(matrix4f);
        return camera_ray;
    }

    public static boolean intersectRayAab(
        float originX,
        float originY,
        float originZ,
        float dirX,
        float dirY,
        float dirZ,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ,
        Vector2f result
    ) {
        float invDirX = 1.0F / dirX;
        float invDirY = 1.0F / dirY;
        float invDirZ = 1.0F / dirZ;
        float tNear;
        float tFar;
        if (invDirX >= 0.0F) {
            tNear = (minX - originX) * invDirX;
            tFar = (maxX - originX) * invDirX;
        } else {
            tNear = (maxX - originX) * invDirX;
            tFar = (minX - originX) * invDirX;
        }

        float tymin;
        float tymax;
        if (invDirY >= 0.0F) {
            tymin = (minY - originY) * invDirY;
            tymax = (maxY - originY) * invDirY;
        } else {
            tymin = (maxY - originY) * invDirY;
            tymax = (minY - originY) * invDirY;
        }

        if (!(tNear > tymax) && !(tymin > tFar)) {
            float tzmin;
            float tzmax;
            if (invDirZ >= 0.0F) {
                tzmin = (minZ - originZ) * invDirZ;
                tzmax = (maxZ - originZ) * invDirZ;
            } else {
                tzmin = (maxZ - originZ) * invDirZ;
                tzmax = (minZ - originZ) * invDirZ;
            }

            if (!(tNear > tzmax) && !(tzmin > tFar)) {
                tNear = !(tymin > tNear) && !Float.isNaN(tNear) ? tNear : tymin;
                tFar = !(tymax < tFar) && !Float.isNaN(tFar) ? tFar : tymax;
                tNear = tzmin > tNear ? tzmin : tNear;
                tFar = tzmax < tFar ? tzmax : tFar;
                if (tNear < tFar && tFar >= 0.0F) {
                    result.x = tNear;
                    result.y = tFar;
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static Matrix4f allocMatrix4f() {
        return BaseVehicle.TL_matrix4f_pool.get().alloc();
    }

    private static void releaseMatrix4f(Matrix4f matrix) {
        BaseVehicle.TL_matrix4f_pool.get().release(matrix);
    }

    private static Quaternionf allocQuaternionf() {
        return BaseVehicle.TL_quaternionf_pool.get().alloc();
    }

    private static void releaseQuaternionf(Quaternionf q) {
        BaseVehicle.TL_quaternionf_pool.get().release(q);
    }

    private static Vector2 allocVector2() {
        return Vector2ObjectPool.get().alloc();
    }

    private static void releaseVector2(Vector2 vector2) {
        Vector2ObjectPool.get().release(vector2);
    }

    private static Vector2f allocVector2f() {
        return BaseVehicle.allocVector2f();
    }

    private static void releaseVector2f(Vector2f vector2f) {
        BaseVehicle.releaseVector2f(vector2f);
    }

    private static Vector3f allocVector3f() {
        return BaseVehicle.TL_vector3f_pool.get().alloc();
    }

    private static void releaseVector3f(Vector3f vector3f) {
        BaseVehicle.TL_vector3f_pool.get().release(vector3f);
    }
}
