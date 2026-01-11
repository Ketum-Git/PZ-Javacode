// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gizmo;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.vehicles.UI3DScene;

public class StateData {
    float zoomF;
    final Matrix4f projection = new Matrix4f();
    final Matrix4f modelView = new Matrix4f();
    Gizmo gizmo;
    SceneObject gizmoChild;
    final Vector3f gizmoTranslate = new Vector3f();
    final Vector3f gizmoRotate = new Vector3f();
    final Vector3f gizmoWorldPos = new Vector3f();
    final Matrix4f gizmoParentTransform = new Matrix4f();
    final Matrix4f gizmoOriginTransform = new Matrix4f();
    final Matrix4f gizmoChildTransform = new Matrix4f();
    final Matrix4f gizmoChildAttachmentTransform = new Matrix4f();
    final Matrix4f gizmoChildAttachmentTransformInv = new Matrix4f();
    final Matrix4f gizmoTransform = new Matrix4f();
    boolean hasGizmoOrigin;
    boolean gizmoOriginIsGeometry;
    boolean selectedAttachmentIsChildAttachment;
    Axis gizmoAxis = Axis.None;
    final TranslateGizmoRenderData translateGizmoRenderData = new TranslateGizmoRenderData();
    final UI3DScene.Ray cameraRay = new UI3DScene.Ray();
}
