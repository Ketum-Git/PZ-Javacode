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
            UI3DScene.Ray cameraRay = this.scene.getCameraRay(uiX, uiY, this.allocRay());
            UI3DScene.Ray axis = this.allocRay();
            gizmoXfrm.transformPosition(axis.origin.set(0.0F, 0.0F, 0.0F));
            float scale = this.getScale();
            float length = 0.5F * scale;
            float thickness = 0.05F * scale;
            float offset = 0.3F * scale;
            gizmoXfrm.transformDirection(axis.direction.set(1.0F, 0.0F, 0.0F)).normalize();
            float distX = UI3DScene.closest_distance_between_lines(axis, cameraRay);
            float xT = axis.t;
            float camXT = cameraRay.t;
            if (!this.gizmoAxisVisibleX || xT < offset || xT >= offset + length) {
                xT = Float.MAX_VALUE;
                distX = Float.MAX_VALUE;
            }

            float xdot = axis.direction.dot(cameraRay.direction);
            stateData.translateGizmoRenderData.hideX = !this.gizmoAxisVisibleX || Math.abs(xdot) > 0.9F;
            gizmoXfrm.transformDirection(axis.direction.set(0.0F, 1.0F, 0.0F)).normalize();
            float distY = UI3DScene.closest_distance_between_lines(axis, cameraRay);
            float yT = axis.t;
            float camYT = cameraRay.t;
            if (!this.gizmoAxisVisibleY || yT < offset || yT >= offset + length) {
                yT = Float.MAX_VALUE;
                distY = Float.MAX_VALUE;
            }

            float ydot = axis.direction.dot(cameraRay.direction);
            stateData.translateGizmoRenderData.hideY = !this.gizmoAxisVisibleY || Math.abs(ydot) > 0.9F;
            gizmoXfrm.transformDirection(axis.direction.set(0.0F, 0.0F, this.transformMode == TransformMode.Global && this.reverseZAxis ? -1.0F : 1.0F))
                .normalize();
            float distZ = UI3DScene.closest_distance_between_lines(axis, cameraRay);
            float zT = axis.t;
            float camZT = cameraRay.t;
            if (!this.gizmoAxisVisibleZ || zT < offset || zT >= offset + length) {
                zT = Float.MAX_VALUE;
                distZ = Float.MAX_VALUE;
            }

            float zdot = axis.direction.dot(cameraRay.direction);
            stateData.translateGizmoRenderData.hideZ = !this.gizmoAxisVisibleZ || Math.abs(zdot) > 0.9F;
            Axis doubleAxis = Axis.None;
            if (this.doubleAxis) {
                float localOffset = thickness * 1.5F;
                float inner = localOffset + offset;
                float outer = inner + length / 2.0F;
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
            releaseRay(cameraRay);
            this.releaseMatrix4f(gizmoXfrm);
            if (doubleAxis != Axis.None) {
                return doubleAxis;
            } else if (xT >= offset && xT < offset + length && distX < distY && distX < distZ) {
                return distX <= thickness / 2.0F ? Axis.X : Axis.None;
            } else if (yT >= offset && yT < offset + length && distY < distX && distY < distZ) {
                return distY <= thickness / 2.0F ? Axis.Y : Axis.None;
            } else if (zT >= offset && zT < offset + length && distZ < distX && distZ < distY) {
                return distZ <= thickness / 2.0F ? Axis.Z : Axis.None;
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
            float scalex = this.getScale();
            float thickness = 0.05F * scalex;
            float length = 0.5F * scalex;
            float offset = 0.3F * scalex;
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
                matrix4f.translate(0.0F, 0.0F, offset);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickness / 2.0F, thickness / 2.0F, length, 8, 1, r, 0.0F, 0.0F, 1.0F);
                vbor.cmdPopMatrix(5888);
                matrix4f.translate(0.0F, 0.0F, length);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickness / 2.0F * 2.0F, 0.0F, 0.1F * scalex, 8, 1, r, 0.0F, 0.0F, 1.0F);
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
                matrix4f.translate(0.0F, 0.0F, offset);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickness / 2.0F, thickness / 2.0F, length, 8, 1, 0.0F, g, 0.0F, 1.0F);
                vbor.cmdPopMatrix(5888);
                matrix4f.translate(0.0F, 0.0F, length);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickness / 2.0F * 2.0F, 0.0F, 0.1F * scalex, 8, 1, 0.0F, g, 0.0F, 1.0F);
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
                matrix4f.translation(0.0F, 0.0F, -offset);
                matrix4f.rotateY((float) Math.PI);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickness / 2.0F, thickness / 2.0F, length, 8, 1, 0.0F, 0.0F, b, 1.0F);
                vbor.cmdPopMatrix(5888);
                matrix4f.translate(0.0F, 0.0F, length);
                vbor.cmdPushAndMultMatrix(5888, matrix4f);
                vbor.addCylinder_Fill(thickness / 2.0F * 2.0F, 0.0F, 0.1F * scalex, 8, 1, 0.0F, 0.0F, b, 1.0F);
                vbor.cmdPopMatrix(5888);
            }

            if (this.doubleAxis) {
                float localOffset = thickness * 1.5F;
                if (!stateData.translateGizmoRenderData.hideX && !stateData.translateGizmoRenderData.hideY) {
                    boolean highlight = stateData.gizmoAxis == Axis.XY || this.trackAxis == Axis.XY;
                    GL11.glColor4f(1.0F, 1.0F, 0.0F, highlight ? 1.0F : 0.5F);
                    GL11.glTranslatef(localOffset, localOffset, 0.0F);
                    this.disk.draw(offset, offset + length / 2.0F, 5, 1, 0.0F, 90.0F);
                    GL11.glTranslatef(-localOffset, -localOffset, 0.0F);
                }

                if (!stateData.translateGizmoRenderData.hideX && !stateData.translateGizmoRenderData.hideZ) {
                    boolean highlight = stateData.gizmoAxis == Axis.XZ || this.trackAxis == Axis.XZ;
                    GL11.glColor4f(1.0F, 0.0F, 1.0F, highlight ? 1.0F : 0.5F);
                    GL11.glTranslatef(localOffset, 0.0F, localOffset);
                    GL11.glRotated(-90.0, 1.0, 0.0, 0.0);
                    this.disk.draw(offset, offset + length / 2.0F, 5, 1, 90.0F, 90.0F);
                    GL11.glRotated(90.0, 1.0, 0.0, 0.0);
                    GL11.glTranslatef(-localOffset, 0.0F, -localOffset);
                }

                if (!stateData.translateGizmoRenderData.hideY && !stateData.translateGizmoRenderData.hideZ) {
                    boolean highlight = stateData.gizmoAxis == Axis.YZ || this.trackAxis == Axis.YZ;
                    GL11.glColor4f(0.0F, 1.0F, 1.0F, highlight ? 1.0F : 0.5F);
                    GL11.glTranslatef(0.0F, localOffset, localOffset);
                    GL11.glRotated(-90.0, 0.0, 1.0, 0.0);
                    this.disk.draw(offset, offset + length / 2.0F, 5, 1, 0.0F, 90.0F);
                    GL11.glRotated(90.0, 0.0, 1.0, 0.0);
                    GL11.glTranslatef(0.0F, -localOffset, -localOffset);
                }
            }

            vbor.cmdPopMatrix(5888);
            this.releaseMatrix4f(matrix4f);
            this.renderLineToOrigin();
            GLStateRenderThread.restore();
        }
    }
}
