// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gizmo;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjglx.util.glu.PartialDisk;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.model.Model;
import zombie.ui.UIManager;
import zombie.vehicles.UI3DScene;

@UsedFromLua
public class TranslateGizmo extends Gizmo {
    final Matrix4f startXfrm = new Matrix4f();
    final Matrix4f startInvXfrm = new Matrix4f();
    final Vector3f startPos = new Vector3f();
    final Vector3f currentPos = new Vector3f();
    Axis trackAxis = Axis.None;
    boolean doubleAxis;
    final PartialDisk disk = new PartialDisk();
    final Vector3f startTranslate = new Vector3f();

    TranslateGizmo(Scene scene) {
        super(scene);
        this.reverseZAxis = true;
    }

    @Override
    Axis hitTest(float uiX, float uiY) {
        if (!this.visible) {
            return Axis.None;
        } else {
            StateData stateData = this.scene.stateDataMain();
            this.scene.setModelViewProjection(stateData);
            this.scene.setGizmoTransforms(stateData);
            Matrix4f gizmoXfrm = this.allocMatrix4f();
            gizmoXfrm.set(stateData.gizmoParentTransform);
            gizmoXfrm.mul(stateData.gizmoOriginTransform);
            gizmoXfrm.mul(stateData.gizmoChildTransform);
            if (stateData.selectedAttachmentIsChildAttachment) {
                gizmoXfrm.mul(stateData.gizmoChildAttachmentTransformInv);
            }

            gizmoXfrm.mul(stateData.gizmoTransform);
            if (this.transformMode == TransformMode.Global) {
                gizmoXfrm.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            uiY = this.scene.screenHeight() - uiY;
            UI3DScene.Ray camera_ray = this.scene.getCameraRay(uiX, uiY, this.allocRay());
            UI3DScene.Ray axis = this.allocRay();
            gizmoXfrm.transformPosition(axis.origin.set(0.0F, 0.0F, 0.0F));
            float SCALE = this.getScale();
            float LENGTH = 0.5F * SCALE;
            float THICKNESS = 0.05F * SCALE;
            float OFFSET = 0.3F * SCALE;
            gizmoXfrm.transformDirection(axis.direction.set(1.0F, 0.0F, 0.0F)).normalize();
            float distX = UI3DScene.closest_distance_between_lines(axis, camera_ray);
            float x_t = axis.t;
            float cam_x_t = camera_ray.t;
            if (!this.gizmoAxisVisibleX || x_t < OFFSET || x_t >= OFFSET + LENGTH) {
                x_t = Float.MAX_VALUE;
                distX = Float.MAX_VALUE;
            }

            float xdot = axis.direction.dot(camera_ray.direction);
            stateData.translateGizmoRenderData.hideX = !this.gizmoAxisVisibleX || Math.abs(xdot) > 0.9F;
            gizmoXfrm.transformDirection(axis.direction.set(0.0F, 1.0F, 0.0F)).normalize();
            float distY = UI3DScene.closest_distance_between_lines(axis, camera_ray);
            float y_t = axis.t;
            float cam_y_t = camera_ray.t;
            if (!this.gizmoAxisVisibleY || y_t < OFFSET || y_t >= OFFSET + LENGTH) {
                y_t = Float.MAX_VALUE;
                distY = Float.MAX_VALUE;
            }

            float ydot = axis.direction.dot(camera_ray.direction);
            stateData.translateGizmoRenderData.hideY = !this.gizmoAxisVisibleY || Math.abs(ydot) > 0.9F;
            gizmoXfrm.transformDirection(axis.direction.set(0.0F, 0.0F, this.transformMode == TransformMode.Global && this.reverseZAxis ? -1.0F : 1.0F))
                .normalize();
            float distZ = UI3DScene.closest_distance_between_lines(axis, camera_ray);
            float z_t = axis.t;
            float cam_z_t = camera_ray.t;
            if (!this.gizmoAxisVisibleZ || z_t < OFFSET || z_t >= OFFSET + LENGTH) {
                z_t = Float.MAX_VALUE;
                distZ = Float.MAX_VALUE;
            }

            float zdot = axis.direction.dot(camera_ray.direction);
            stateData.translateGizmoRenderData.hideZ = !this.gizmoAxisVisibleZ || Math.abs(zdot) > 0.9F;
            Axis doubleAxis = Axis.None;
            if (this.doubleAxis) {
                float offset = THICKNESS * 1.5F;
                float inner = offset + OFFSET;
                float outer = inner + LENGTH / 2.0F;
                Vector3f pointOnPlane3D = this.allocVector3f();
                Vector2f pointOnPlane2D = this.allocVector2f();
                if (this.getPointOnDualAxis(uiX, -(uiY - this.scene.screenHeight()), Axis.XY, gizmoXfrm, pointOnPlane3D, pointOnPlane2D)
                    && pointOnPlane2D.x >= 0.0F
                    && pointOnPlane2D.y >= 0.0F
                    && pointOnPlane2D.length() >= inner
                    && pointOnPlane2D.length() < outer) {
                    doubleAxis = Axis.XY;
                }

                if (this.getPointOnDualAxis(uiX, -(uiY - this.scene.screenHeight()), Axis.XZ, gizmoXfrm, pointOnPlane3D, pointOnPlane2D)
                    && pointOnPlane2D.x >= 0.0F
                    && pointOnPlane2D.y >= 0.0F
                    && pointOnPlane2D.length() >= inner
                    && pointOnPlane2D.length() < outer) {
                    doubleAxis = Axis.XZ;
                }

                if (this.getPointOnDualAxis(uiX, -(uiY - this.scene.screenHeight()), Axis.YZ, gizmoXfrm, pointOnPlane3D, pointOnPlane2D)
                    && pointOnPlane2D.x >= 0.0F
                    && pointOnPlane2D.y >= 0.0F
                    && pointOnPlane2D.length() >= inner
                    && pointOnPlane2D.length() < outer) {
                    doubleAxis = Axis.YZ;
                }

                this.releaseVector3f(pointOnPlane3D);
                this.releaseVector2f(pointOnPlane2D);
            }

            releaseRay(axis);
            releaseRay(camera_ray);
            this.releaseMatrix4f(gizmoXfrm);
            if (doubleAxis != Axis.None) {
                return doubleAxis;
            } else if (x_t >= OFFSET && x_t < OFFSET + LENGTH && distX < distY && distX < distZ) {
                return distX <= THICKNESS / 2.0F ? Axis.X : Axis.None;
            } else if (y_t >= OFFSET && y_t < OFFSET + LENGTH && distY < distX && distY < distZ) {
                return distY <= THICKNESS / 2.0F ? Axis.Y : Axis.None;
            } else if (z_t >= OFFSET && z_t < OFFSET + LENGTH && distZ < distX && distZ < distY) {
                return distZ <= THICKNESS / 2.0F ? Axis.Z : Axis.None;
            } else {
                return Axis.None;
            }
        }
    }

    @Override
    void startTracking(float uiX, float uiY, Axis axis) {
        StateData stateData = this.scene.stateDataMain();
        this.scene.setModelViewProjection(stateData);
        this.scene.setGizmoTransforms(stateData);
        this.startXfrm.set(stateData.gizmoParentTransform);
        this.startXfrm.mul(stateData.gizmoOriginTransform);
        this.startXfrm.mul(stateData.gizmoChildTransform);
        if (!stateData.selectedAttachmentIsChildAttachment) {
            this.startXfrm.mul(stateData.gizmoTransform);
        }

        if (this.transformMode == TransformMode.Global) {
            this.startXfrm.setRotationXYZ(0.0F, 0.0F, 0.0F);
        }

        this.startInvXfrm.set(this.startXfrm);
        this.startInvXfrm.invert();
        this.trackAxis = axis;
        this.getPointOnAxis(uiX, uiY, axis, this.startXfrm, this.startPos);
        this.startTranslate.set(stateData.gizmoWorldPos);
    }

    @Override
    void updateTracking(float uiX, float uiY) {
        Vector3f pos = this.getPointOnAxis(uiX, uiY, this.trackAxis, this.startXfrm, this.allocVector3f());
        if (this.currentPos.equals(pos)) {
            this.releaseVector3f(pos);
        } else {
            this.currentPos.set(pos);
            this.releaseVector3f(pos);
            StateData stateData = this.scene.stateDataMain();
            this.scene.setModelViewProjection(stateData);
            this.scene.setGizmoTransforms(stateData);
            Vector3f delta = this.allocVector3f().set(this.currentPos).sub(this.startPos);
            if (this.scene.selectedAttachment == null && stateData.gizmoChild == null && !stateData.gizmoOriginIsGeometry) {
                delta.set(this.currentPos).sub(this.startPos);
            } else if (this.transformMode == TransformMode.Global) {
                Vector3f vs = this.startInvXfrm.transformPosition(this.startPos, this.allocVector3f());
                Vector3f vc = this.startInvXfrm.transformPosition(this.currentPos, this.allocVector3f());
                Matrix4f m = this.allocMatrix4f();
                m.set(stateData.gizmoParentTransform);
                m.mul(stateData.gizmoOriginTransform);
                if (!stateData.selectedAttachmentIsChildAttachment) {
                    m.mul(stateData.gizmoChildTransform);
                }

                m.invert();
                m.transformPosition(vs);
                m.transformPosition(vc);
                this.releaseMatrix4f(m);
                delta.set(vc).sub(vs);
                this.releaseVector3f(vs);
                this.releaseVector3f(vc);
            } else {
                Vector3f vs = this.startInvXfrm.transformPosition(this.startPos, this.allocVector3f());
                Vector3f vc = this.startInvXfrm.transformPosition(this.currentPos, this.allocVector3f());
                Matrix4f m = this.allocMatrix4f();
                m.set(stateData.gizmoTransform);
                m.transformPosition(vs);
                m.transformPosition(vc);
                this.releaseMatrix4f(m);
                delta.set(vc).sub(vs);
                this.releaseVector3f(vs);
                this.releaseVector3f(vc);
            }

            if (this.getTable() != null) {
                float x = this.startTranslate.x + delta.x;
                float y = this.startTranslate.y - delta.z;
                float z = this.startTranslate.z + delta.y / 2.44949F;
                delta.set(x, y, z);
                LuaManager.caller.pcall(UIManager.getDefaultThread(), this.getTable().rawget("onTranslateGizmo"), this.getTable(), delta);
            }

            this.releaseVector3f(delta);
        }
    }

    @Override
    void stopTracking() {
        this.trackAxis = Axis.None;
    }

    @Override
    void render() {
        if (this.visible) {
            StateData stateData = this.scene.stateDataRender();
            Matrix4f matrix4f = this.allocMatrix4f();
            matrix4f.set(stateData.gizmoParentTransform);
            matrix4f.mul(stateData.gizmoOriginTransform);
            matrix4f.mul(stateData.gizmoChildTransform);
            if (stateData.selectedAttachmentIsChildAttachment) {
                matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
            }

            matrix4f.mul(stateData.gizmoTransform);
            Vector3f scale = matrix4f.getScale(this.allocVector3f());
            matrix4f.scale(1.0F / scale.x, 1.0F / scale.y, 1.0F / scale.z);
            this.releaseVector3f(scale);
            if (this.transformMode == TransformMode.Global) {
                matrix4f.setRotationXYZ(0.0F, 0.0F, 0.0F);
            }

            stateData.modelView.mul(matrix4f, matrix4f);
            VBORenderer vbor = VBORenderer.getInstance();
            boolean flipZ = this.transformMode == TransformMode.Global && this.reverseZAxis;
            vbor.cmdPushAndLoadMatrix(5888, matrix4f);
            Model.debugDrawAxis(0.0F, 0.0F, 0.0F, false, false, flipZ, 0.5F, 1.0F);
            vbor.cmdPopMatrix(5888);
            float SCALE = this.getScale();
            float THICKNESS = 0.05F * SCALE;
            float LENGTH = 0.5F * SCALE;
            float OFFSET = 0.3F * SCALE;
            vbor.cmdPushAndLoadMatrix(5888, matrix4f);
            if (!stateData.translateGizmoRenderData.hideX) {
                boolean highlight = stateData.gizmoAxis == Axis.X || this.trackAxis == Axis.X;
                highlight |= stateData.gizmoAxis == Axis.XY || this.trackAxis == Axis.XY;
                highlight |= stateData.gizmoAxis == Axis.XZ || this.trackAxis == Axis.XZ;
                float r = highlight ? 1.0F : 0.5F;
                float g = 0.0F;
                float b = 0.0F;
                float a = 1.0F;
                matrix4f.rotation((float) (Math.PI / 2), 0.0F, 1.0F, 0.0F);
                matrix4f.translate(0.0F, 0.0F, OFFSET);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(THICKNESS / 2.0F, THICKNESS / 2.0F, LENGTH, 8, 1, r, 0.0F, 0.0F, 1.0F);
                vbor.cmdPopMatrix(5888);
                matrix4f.translate(0.0F, 0.0F, LENGTH);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(THICKNESS / 2.0F * 2.0F, 0.0F, 0.1F * SCALE, 8, 1, r, 0.0F, 0.0F, 1.0F);
                vbor.cmdPopMatrix(5888);
            }

            if (!stateData.translateGizmoRenderData.hideY) {
                boolean highlight = stateData.gizmoAxis == Axis.Y || this.trackAxis == Axis.Y;
                highlight |= stateData.gizmoAxis == Axis.XY || this.trackAxis == Axis.XY;
                highlight |= stateData.gizmoAxis == Axis.YZ || this.trackAxis == Axis.YZ;
                float r = 0.0F;
                float g = highlight ? 1.0F : 0.5F;
                float b = 0.0F;
                float a = 1.0F;
                matrix4f.rotation((float) (-Math.PI / 2), 1.0F, 0.0F, 0.0F);
                matrix4f.translate(0.0F, 0.0F, OFFSET);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(THICKNESS / 2.0F, THICKNESS / 2.0F, LENGTH, 8, 1, 0.0F, g, 0.0F, 1.0F);
                vbor.cmdPopMatrix(5888);
                matrix4f.translate(0.0F, 0.0F, LENGTH);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(THICKNESS / 2.0F * 2.0F, 0.0F, 0.1F * SCALE, 8, 1, 0.0F, g, 0.0F, 1.0F);
                vbor.cmdPopMatrix(5888);
            }

            if (!stateData.translateGizmoRenderData.hideZ) {
                boolean highlight = stateData.gizmoAxis == Axis.Z || this.trackAxis == Axis.Z;
                highlight |= stateData.gizmoAxis == Axis.XZ || this.trackAxis == Axis.XZ;
                highlight |= stateData.gizmoAxis == Axis.YZ || this.trackAxis == Axis.YZ;
                float r = 0.0F;
                float g = 0.0F;
                float b = highlight ? 1.0F : 0.5F;
                float a = 1.0F;
                matrix4f.translation(0.0F, 0.0F, -OFFSET);
                matrix4f.rotateY((float) Math.PI);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(THICKNESS / 2.0F, THICKNESS / 2.0F, LENGTH, 8, 1, 0.0F, 0.0F, b, 1.0F);
                vbor.cmdPopMatrix(5888);
                matrix4f.translate(0.0F, 0.0F, LENGTH);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(THICKNESS / 2.0F * 2.0F, 0.0F, 0.1F * SCALE, 8, 1, 0.0F, 0.0F, b, 1.0F);
                vbor.cmdPopMatrix(5888);
            }

            if (this.doubleAxis) {
                float offset = THICKNESS * 1.5F;
                if (!stateData.translateGizmoRenderData.hideX && !stateData.translateGizmoRenderData.hideY) {
                    boolean highlight = stateData.gizmoAxis == Axis.XY || this.trackAxis == Axis.XY;
                    GL11.glColor4f(1.0F, 1.0F, 0.0F, highlight ? 1.0F : 0.5F);
                    GL11.glTranslatef(offset, offset, 0.0F);
                    this.disk.draw(OFFSET, OFFSET + LENGTH / 2.0F, 5, 1, 0.0F, 90.0F);
                    GL11.glTranslatef(-offset, -offset, 0.0F);
                }

                if (!stateData.translateGizmoRenderData.hideX && !stateData.translateGizmoRenderData.hideZ) {
                    boolean highlight = stateData.gizmoAxis == Axis.XZ || this.trackAxis == Axis.XZ;
                    GL11.glColor4f(1.0F, 0.0F, 1.0F, highlight ? 1.0F : 0.5F);
                    GL11.glTranslatef(offset, 0.0F, offset);
                    GL11.glRotated(-90.0, 1.0, 0.0, 0.0);
                    this.disk.draw(OFFSET, OFFSET + LENGTH / 2.0F, 5, 1, 90.0F, 90.0F);
                    GL11.glRotated(90.0, 1.0, 0.0, 0.0);
                    GL11.glTranslatef(-offset, 0.0F, -offset);
                }

                if (!stateData.translateGizmoRenderData.hideY && !stateData.translateGizmoRenderData.hideZ) {
                    boolean highlight = stateData.gizmoAxis == Axis.YZ || this.trackAxis == Axis.YZ;
                    GL11.glColor4f(0.0F, 1.0F, 1.0F, highlight ? 1.0F : 0.5F);
                    GL11.glTranslatef(0.0F, offset, offset);
                    GL11.glRotated(-90.0, 0.0, 1.0, 0.0);
                    this.disk.draw(OFFSET, OFFSET + LENGTH / 2.0F, 5, 1, 0.0F, 90.0F);
                    GL11.glRotated(90.0, 0.0, 1.0, 0.0);
                    GL11.glTranslatef(0.0F, -offset, -offset);
                }
            }

            vbor.cmdPopMatrix(5888);
            this.releaseMatrix4f(matrix4f);
            this.renderLineToOrigin();
            GLStateRenderThread.restore();
        }
    }
}
