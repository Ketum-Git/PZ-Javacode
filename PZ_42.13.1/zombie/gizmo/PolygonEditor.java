// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gizmo;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.vehicles.UI3DScene;

public class PolygonEditor {
    final Scene scene;
    final UI3DScene.Plane plane = new UI3DScene.Plane();
    final Vector3f rotate = new Vector3f();
    GridPlane gridPlane = GridPlane.XY;

    PolygonEditor(Scene scene) {
        this.scene = scene;
    }

    void setPlane(Vector3f translate, Vector3f rotate, GridPlane gridPlane) {
        this.plane.point.set(translate);
        this.plane.normal.set(0.0F, 0.0F, 1.0F);
        Matrix4f m = this.scene
            .allocMatrix4f()
            .rotationXYZ(rotate.x * (float) (Math.PI / 180.0), rotate.y * (float) (Math.PI / 180.0), rotate.z * (float) (Math.PI / 180.0));
        m.transformDirection(this.plane.normal);
        this.scene.releaseMatrix4f(m);
        this.rotate.set(rotate);
        this.gridPlane = gridPlane;
    }

    boolean uiToPlane3D(float uiX, float uiY, Vector3f result) {
        boolean hit = false;
        UI3DScene.Ray cameraRay = this.scene.getCameraRay(uiX, this.scene.screenHeight() - uiY, this.scene.allocRay());
        if (UI3DScene.intersect_ray_plane(this.plane, cameraRay, result) == 1) {
            hit = true;
        }

        this.scene.releaseRay(cameraRay);
        return hit;
    }

    boolean uiToPlane2D(float uiX, float uiY, Vector2f result) {
        Vector3f pointOnPlane = this.scene.allocVector3f();
        boolean hit = this.uiToPlane3D(uiX, uiY, pointOnPlane);
        if (hit) {
            Matrix4f m = this.scene.allocMatrix4f();
            m.translation(this.plane.point);
            m.rotateXYZ(this.rotate.x * (float) (Math.PI / 180.0), this.rotate.y * (float) (Math.PI / 180.0), this.rotate.z * (float) (Math.PI / 180.0));
            m.invert();
            m.transformPosition(pointOnPlane);
            result.set(pointOnPlane.x, pointOnPlane.y);
            this.scene.releaseMatrix4f(m);
        }

        this.scene.releaseVector3f(pointOnPlane);
        return hit;
    }

    Vector3f planeTo3D(Vector2f pointOnPlane, Vector3f result) {
        Matrix4f m = this.scene.allocMatrix4f();
        m.translation(this.plane.point);
        m.rotateXYZ(this.rotate.x * (float) (Math.PI / 180.0), this.rotate.y * (float) (Math.PI / 180.0), this.rotate.z * (float) (Math.PI / 180.0));
        m.transformPosition(pointOnPlane.x, pointOnPlane.y, 0.0F, result);
        this.scene.releaseMatrix4f(m);
        return result;
    }

    Vector2f planeToUI(Vector2f pointOnPlane, Vector2f result) {
        Vector3f scenePos = this.planeTo3D(pointOnPlane, this.scene.allocVector3f());
        result.set(this.scene.sceneToUIX(scenePos), this.scene.sceneToUIY(scenePos));
        this.scene.releaseVector3f(scenePos);
        return result;
    }
}
