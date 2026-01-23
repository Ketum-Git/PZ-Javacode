// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.gizmo;

import org.joml.Matrix4f;

public abstract class SceneObject {
    String attachment;
    String parentAttachment;

    Matrix4f getLocalTransform(Matrix4f transform) {
        return transform;
    }

    Matrix4f getGlobalTransform(Matrix4f transform) {
        return transform;
    }

    Matrix4f getAttachmentTransform(String attachmentName, Matrix4f transform) {
        return transform;
    }
}
