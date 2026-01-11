// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gizmo;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.Model;
import zombie.input.Mouse;
import zombie.iso.IsoCamera;
import zombie.iso.PlayerCamera;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.UI3DScene;

public class Scene {
    private int x;
    private int y;
    private int width;
    private int height;
    private final View view = View.Right;
    private final GridPlane gridPlane = GridPlane.YZ;
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f modelView = new Matrix4f();
    private final StateData[] stateData = new StateData[3];
    final int[] viewport = new int[]{0, 0, 0, 0};
    Gizmo gizmo;
    String selectedAttachment;
    final PolygonEditor polygonEditor = new PolygonEditor(this);
    boolean mouseDown;

    public Scene() {
        for (int i = 0; i < this.stateData.length; i++) {
            this.stateData[i] = new StateData();
        }
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    public void setGizmo(Gizmo gizmo) {
        this.gizmo = gizmo;
    }

    int getAbsoluteX() {
        return this.x;
    }

    int getAbsoluteY() {
        return this.y;
    }

    StateData stateDataMain() {
        return this.stateData[SpriteRenderer.instance.getMainStateIndex()];
    }

    StateData stateDataRender() {
        return this.stateData[SpriteRenderer.instance.getRenderStateIndex()];
    }

    int screenWidth() {
        return this.width;
    }

    int screenHeight() {
        return this.height;
    }

    void calcMatrices(Matrix4f projection, Matrix4f modelView) {
        float cx = IsoCamera.frameState.camCharacterX;
        float cy = IsoCamera.frameState.camCharacterY;
        float cz = IsoCamera.frameState.camCharacterZ;
        int playerIndex = 0;
        PlayerCamera cam = IsoCamera.cameras[0];
        float rcx = cam.rightClickX;
        float rcy = cam.rightClickY;
        float tox = cam.getTOffX();
        float toy = cam.getTOffY();
        float defx = cam.deferedX;
        float defy = cam.deferedY;
        float x = cx - cam.XToIso(-tox - rcx, -toy - rcy, 0.0F);
        float y = cy - cam.YToIso(-tox - rcx, -toy - rcy, 0.0F);
        x += defx;
        y += defy;
        float zoom = Core.getInstance().getZoom(0);
        double screenWidth = IsoCamera.getScreenWidth(0) * zoom / 1920.0F;
        double screenHeight = IsoCamera.getScreenHeight(0) * zoom / 1920.0F;
        projection.setOrtho(-((float)screenWidth) / 2.0F, (float)screenWidth / 2.0F, -((float)screenHeight) / 2.0F, (float)screenHeight / 2.0F, -1.0F, 1.0F);
        modelView.identity();
        modelView.scale(Core.scale * Core.tileScale / 2.0F);
        modelView.rotate((float) (Math.PI / 6), 1.0F, 0.0F, 0.0F);
        modelView.rotate((float) (Math.PI * 3.0 / 4.0), 0.0F, 1.0F, 0.0F);
        double difX = cx - x;
        double difY = cy - y;
        modelView.translate(-((float)difX), (cz - cz) * 2.44949F, -((float)difY));
        modelView.scale(-1.0F, 1.0F, 1.0F);
        modelView.translate(0.0F, -0.71999997F, 0.0F);
    }

    void setModelViewProjection(StateData stateData) {
        this.calcMatrices(this.projection, this.modelView);
        stateData.projection.set(this.projection);
        stateData.modelView.set(this.modelView);
        stateData.zoomF = IsoCamera.frameState.zoom;
    }

    void setGizmoTransforms(StateData stateData) {
        stateData.gizmo = this.gizmo;
        SceneObject gizmoParent = null;
        SceneObject gizmoChild = null;
        SceneObject gizmoOrigin = null;
        if (this.gizmo != null) {
            Vector3f worldPos = this.gizmo.getWorldPosition();
            stateData.gizmoTranslate
                .set(
                    worldPos.x - IsoCamera.frameState.camCharacterX,
                    (worldPos.z - IsoCamera.frameState.camCharacterZ) * 3.0F * 0.8164967F,
                    -(worldPos.y - IsoCamera.frameState.camCharacterY)
                );
            stateData.gizmoTranslate.set(stateData.gizmoTranslate);
            Vector3f rotation = this.gizmo.getRotation();
            stateData.gizmoRotate.set(rotation);
            stateData.gizmoWorldPos.set(worldPos);
            stateData.gizmoTransform.translation(stateData.gizmoTranslate);
            stateData.gizmoTransform
                .rotateXYZ(rotation.x * (float) (Math.PI / 180.0), rotation.y * (float) (Math.PI / 180.0), rotation.z * (float) (Math.PI / 180.0));
            gizmoParent = this.gizmo.getParent();
            gizmoChild = this.gizmo.getChild();
            gizmoOrigin = this.gizmo.getOrigin();
        }

        stateData.gizmoChildTransform.identity();
        stateData.gizmoChildAttachmentTransform.identity();
        stateData.selectedAttachmentIsChildAttachment = gizmoChild != null
            && gizmoChild.attachment != null
            && gizmoChild.attachment.equals(this.selectedAttachment);
        if (gizmoChild != null) {
            gizmoChild.getLocalTransform(stateData.gizmoChildTransform);
            gizmoChild.getAttachmentTransform(gizmoChild.attachment, stateData.gizmoChildAttachmentTransform);
            stateData.gizmoChildAttachmentTransformInv.set(stateData.gizmoChildAttachmentTransform).invert();
        }

        stateData.gizmoOriginTransform.identity();
        stateData.hasGizmoOrigin = gizmoOrigin != null;
        if (gizmoOrigin != null && gizmoOrigin != gizmoParent) {
            gizmoOrigin.getGlobalTransform(stateData.gizmoOriginTransform);
        }

        stateData.gizmoParentTransform.identity();
        if (gizmoParent != null) {
            gizmoParent.getGlobalTransform(stateData.gizmoParentTransform);
        }
    }

    public Vector3f uiToScene(float uiX, float uiY, float uiZ, Vector3f out) {
        this.uiToScene(null, uiX, uiY, uiZ, out);
        switch (this.view) {
            case Left:
            case Right:
                out.x = 0.0F;
                break;
            case Top:
            case Bottom:
                out.y = 0.0F;
                break;
            case Front:
            case Back:
                out.z = 0.0F;
        }

        if (this.view == View.UserDefined) {
            Vector3f orientation = this.allocVector3f();
            switch (this.gridPlane) {
                case XY:
                    orientation.set(0.0F, 0.0F, 1.0F);
                    break;
                case XZ:
                    orientation.set(0.0F, 1.0F, 0.0F);
                    break;
                case YZ:
                    orientation.set(1.0F, 0.0F, 0.0F);
            }

            Vector3f center = this.allocVector3f().set(0.0F);
            UI3DScene.Plane plane = this.allocPlane().set(orientation, center);
            this.releaseVector3f(orientation);
            this.releaseVector3f(center);
            UI3DScene.Ray cameraRay = this.getCameraRay(uiX, this.screenHeight() - uiY, this.allocRay());
            if (UI3DScene.intersect_ray_plane(plane, cameraRay, out) != 1) {
                out.set(0.0F);
            }

            this.releasePlane(plane);
            this.releaseRay(cameraRay);
        }

        return out;
    }

    public Vector3f uiToScene(Matrix4f modelTransform, float uiX, float uiY, float uiZ, Vector3f out) {
        uiY = this.screenHeight() - uiY;
        Matrix4f matrix4f = this.allocMatrix4f();
        matrix4f.set(this.projection);
        matrix4f.mul(this.modelView);
        if (modelTransform != null) {
            matrix4f.mul(modelTransform);
        }

        matrix4f.invert();
        this.viewport[2] = this.screenWidth();
        this.viewport[3] = this.screenHeight();
        matrix4f.unprojectInv(uiX, uiY, uiZ, this.viewport, out);
        this.releaseMatrix4f(matrix4f);
        return out;
    }

    public float sceneToUIX(float sceneX, float sceneY, float sceneZ) {
        Matrix4f matrix4f = this.allocMatrix4f();
        matrix4f.set(this.projection);
        matrix4f.mul(this.modelView);
        this.viewport[2] = this.screenWidth();
        this.viewport[3] = this.screenHeight();
        Vector3f tempVector3f = this.allocVector3f();
        matrix4f.project(sceneX, sceneY, sceneZ, this.viewport, tempVector3f);
        this.releaseMatrix4f(matrix4f);
        float x = tempVector3f.x();
        this.releaseVector3f(tempVector3f);
        return x;
    }

    public float sceneToUIY(float sceneX, float sceneY, float sceneZ) {
        Matrix4f matrix4f = this.allocMatrix4f();
        matrix4f.set(this.projection);
        matrix4f.mul(this.modelView);
        this.viewport[2] = this.screenWidth();
        this.viewport[3] = this.screenHeight();
        Vector3f tempVector3f = this.allocVector3f();
        matrix4f.project(sceneX, sceneY, sceneZ, this.viewport, tempVector3f);
        this.releaseMatrix4f(matrix4f);
        float y = this.screenHeight() - tempVector3f.y();
        this.releaseVector3f(tempVector3f);
        return y;
    }

    public float sceneToUIX(Vector3f scenePos) {
        return this.sceneToUIX(scenePos.x, scenePos.y, scenePos.z);
    }

    public float sceneToUIY(Vector3f scenePos) {
        return this.sceneToUIY(scenePos.x, scenePos.y, scenePos.z);
    }

    UI3DScene.Ray getCameraRay(float uiX, float uiY, UI3DScene.Ray camera_ray) {
        return this.getCameraRay(uiX, uiY, this.projection, this.modelView, camera_ray);
    }

    UI3DScene.Ray getCameraRay(float uiX, float uiY, Matrix4f projection, Matrix4f modelView, UI3DScene.Ray camera_ray) {
        return this.getCameraRay(uiX, uiY, projection, modelView, this.screenWidth(), this.screenHeight(), camera_ray);
    }

    UI3DScene.Ray getCameraRay(float uiX, float uiY, Matrix4f projection, Matrix4f modelView, int viewWidth, int viewHeight, UI3DScene.Ray camera_ray) {
        Matrix4f matrix4f = this.allocMatrix4f();
        matrix4f.set(projection);
        matrix4f.mul(modelView);
        matrix4f.invert();
        this.viewport[2] = viewWidth;
        this.viewport[3] = viewHeight;
        Vector3f ray_start = matrix4f.unprojectInv(uiX, uiY, 0.0F, this.viewport, this.allocVector3f());
        Vector3f ray_end = matrix4f.unprojectInv(uiX, uiY, 1.0F, this.viewport, this.allocVector3f());
        camera_ray.origin.set(ray_start);
        camera_ray.direction.set(ray_end.sub(ray_start).normalize());
        this.releaseVector3f(ray_end);
        this.releaseVector3f(ray_start);
        this.releaseMatrix4f(matrix4f);
        return camera_ray;
    }

    void renderAxis(Vector3f pos, Vector3f rot, boolean bRelativeToOrigin) {
        StateData stateData = this.stateDataRender();
        VBORenderer.getInstance().flush();
        Matrix4f matrix4f = this.allocMatrix4f().identity();
        if (!bRelativeToOrigin) {
            matrix4f.mul(stateData.gizmoParentTransform);
            matrix4f.mul(stateData.gizmoOriginTransform);
            matrix4f.mul(stateData.gizmoChildTransform);
            if (stateData.selectedAttachmentIsChildAttachment) {
                matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
            }
        }

        matrix4f.translate(pos);
        matrix4f.rotateXYZ(rot.x * (float) (Math.PI / 180.0), rot.y * (float) (Math.PI / 180.0), rot.z * (float) (Math.PI / 180.0));
        stateData.modelView.mul(matrix4f, matrix4f);
        PZGLUtil.pushAndLoadMatrix(5888, matrix4f);
        this.releaseMatrix4f(matrix4f);
        float LENGTH = 0.1F;
        Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 0.1F, 3.0F);
        PZGLUtil.popMatrix(5888);
    }

    public void renderMain() {
        StateData stateData = this.stateDataMain();
        this.setModelViewProjection(stateData);
        this.setGizmoTransforms(stateData);
        float mouseX = Mouse.getXA() - this.getAbsoluteX();
        float mouseY = Mouse.getYA() - this.getAbsoluteY();
        this.getCameraRay(mouseX, this.screenHeight() - mouseY, stateData.projection, stateData.modelView, stateData.cameraRay);
    }

    public boolean hitTest(int mouseX, int mouseY) {
        StateData stateData = this.stateDataMain();
        this.setModelViewProjection(stateData);
        this.setGizmoTransforms(stateData);
        if (stateData.gizmo != null && stateData.gizmo.isVisible()) {
            mouseX -= this.getAbsoluteX();
            mouseY -= this.getAbsoluteY();
            if (Mouse.isButtonDown(0)) {
                if (this.mouseDown) {
                    stateData.gizmo.updateTracking(mouseX, mouseY);
                } else if (!Mouse.wasButtonDown(0)) {
                    stateData.gizmoAxis = stateData.gizmo.hitTest(mouseX, mouseY);
                    if (stateData.gizmoAxis != Axis.None) {
                        this.mouseDown = true;
                        stateData.gizmo.startTracking(mouseX, mouseY, stateData.gizmoAxis);
                    }
                }
            } else {
                if (this.mouseDown) {
                    this.mouseDown = false;
                    stateData.gizmo.stopTracking();
                }

                stateData.gizmoAxis = stateData.gizmo.hitTest(mouseX, mouseY);
            }
        }

        return this.mouseDown;
    }

    public void render() {
        StateData stateData = this.stateDataRender();
        Matrix4f projection = Core.getInstance().projectionMatrixStack.alloc().set(stateData.projection);
        Matrix4f modelView = Core.getInstance().modelViewMatrixStack.alloc().set(stateData.modelView);
        Core.getInstance().projectionMatrixStack.push(projection);
        Core.getInstance().modelViewMatrixStack.push(modelView);
        if (stateData.gizmo != null) {
            VBORenderer.getInstance().flush();
            stateData.gizmo.render();
            VBORenderer.getInstance().flush();
        }

        Core.getInstance().projectionMatrixStack.pop();
        Core.getInstance().modelViewMatrixStack.pop();
        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        GLStateRenderThread.restore();
    }

    protected Matrix4f allocMatrix4f() {
        return BaseVehicle.allocMatrix4f();
    }

    protected void releaseMatrix4f(Matrix4f m) {
        BaseVehicle.releaseMatrix4f(m);
    }

    protected UI3DScene.Plane allocPlane() {
        return UI3DScene.allocPlane();
    }

    protected void releasePlane(UI3DScene.Plane Plane) {
        UI3DScene.releasePlane(Plane);
    }

    protected UI3DScene.Ray allocRay() {
        return UI3DScene.allocRay();
    }

    public void releaseRay(UI3DScene.Ray ray) {
        UI3DScene.releaseRay(ray);
    }

    protected Vector3f allocVector3f() {
        return BaseVehicle.allocVector3f();
    }

    protected void releaseVector3f(Vector3f vector3f) {
        BaseVehicle.releaseVector3f(vector3f);
    }
}
