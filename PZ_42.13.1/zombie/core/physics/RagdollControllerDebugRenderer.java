// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.physics;

import java.util.ArrayList;
import org.lwjgl.util.vector.Vector3f;
import zombie.characters.IsoGameCharacter;
import zombie.core.Core;
import zombie.core.skinnedmodel.model.Model;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.iso.IsoCamera;
import zombie.iso.Vector2;
import zombie.iso.Vector3;

public class RagdollControllerDebugRenderer {
    private static final ArrayList<RagdollController> closestRagdollControllers = new ArrayList<>();

    public static void drawIsoDebug(
        IsoGameCharacter in_gameCharacterObject, boolean isOnBack, boolean isUpright, Vector3 pelvisPosition, RagdollStateData ragdollStateData
    ) {
        Vector3 groundPosition = new Vector3();
        Model.BoneToWorldCoords(in_gameCharacterObject, 0, groundPosition);
        Vector3f forward = new Vector3f();
        Vector2 forward2 = in_gameCharacterObject.getForwardDirection();
        forward.set(forward2.x, forward2.y, 0.0F);
        if (forward.length() != 0.0F) {
            forward.normalise();
        }

        Vector3f perp = new Vector3f();
        Vector3f.cross(forward, Core._UNIT_Z, perp);
        float w = 0.15F;
        float fm = 0.15F;
        float bm = 0.15F;
        float height = 0.015F;
        float alpha = 0.5F;
        Vector3f originColor = new Vector3f(1.0F, 1.0F, 0.0F);
        Vector3f pelvisColor = new Vector3f(1.0F, 0.0F, 1.0F);
        if (isOnBack) {
            pelvisColor.set(0.0F, 1.0F, 0.0F);
        }

        drawIsoPerspectiveSquare(
            new Vector3(in_gameCharacterObject.getX(), in_gameCharacterObject.getY(), in_gameCharacterObject.getZ()),
            0.15F,
            0.15F,
            0.15F,
            0.015F,
            forward,
            perp,
            originColor,
            0.5F
        );
        drawIsoPerspectiveSquare(pelvisPosition, 0.15F, 0.15F, 0.15F, 0.015F, forward, perp, pelvisColor, 0.5F);
        Vector3f color = new Vector3f(1.0F, 1.0F, 1.0F);
        LineDrawer.addLine(
            pelvisPosition.x,
            pelvisPosition.y,
            pelvisPosition.z,
            ragdollStateData.pelvisDirection.x,
            ragdollStateData.pelvisDirection.y,
            ragdollStateData.pelvisDirection.z,
            color.x,
            color.y,
            color.z,
            0.5F
        );
        if (isUpright) {
            color.set(0.0F, 1.0F, 0.0F);
        }

        LineDrawer.addLine(
            groundPosition.x, groundPosition.y, groundPosition.z, groundPosition.x, groundPosition.y, groundPosition.z + 0.6F, color.x, color.y, color.z, 0.5F
        );
    }

    private static void drawIsoPerspectiveSquare(
        Vector3 position, float w, float fm, float bm, float height, Vector3f forward, Vector3f perp, Vector3f color, float alpha
    ) {
        float fx = position.x + forward.x * fm;
        float fy = position.y + forward.y * fm;
        float bx = position.x - forward.x * bm;
        float by = position.y - forward.y * bm;
        float z = position.z;
        float px = perp.x * w;
        float py = perp.y * w;
        float fx1 = fx - px;
        float fx2 = fx + px;
        float bx1 = bx - px;
        float bx2 = bx + px;
        float by1 = by - py;
        float by2 = by + py;
        float fy1 = fy - py;
        float fy2 = fy + py;
        LineDrawer.addLine(fx1, fy1, z, fx2, fy2, z, color.x, color.y, color.z, alpha);
        LineDrawer.addLine(fx2, fy2, z, bx2, by2, z, color.x, color.y, color.z, alpha);
        LineDrawer.addLine(bx2, by2, z, bx1, by1, z, color.x, color.y, color.z, alpha);
        LineDrawer.addLine(bx1, by1, z, fx1, fy1, z, color.x, color.y, color.z, alpha);
        z += height;
        LineDrawer.addLine(fx1, fy1, z, fx2, fy2, z, color.x, color.y, color.z, alpha);
        LineDrawer.addLine(fx2, fy2, z, bx2, by2, z, color.x, color.y, color.z, alpha);
        LineDrawer.addLine(bx2, by2, z, bx1, by1, z, color.x, color.y, color.z, alpha);
        LineDrawer.addLine(bx1, by1, z, fx1, fy1, z, color.x, color.y, color.z, alpha);
    }

    public static boolean renderDebugPhysics() {
        return DebugOptions.instance.character.debug.ragdoll.render.enable.getValue()
            || DebugOptions.instance.character.debug.ragdoll.render.skeleton.getValue()
            || DebugOptions.instance.character.debug.ragdoll.render.body.getValue()
            || DebugOptions.instance.character.debug.ragdoll.render.bodySinglePart.getValue()
            || DebugOptions.instance.character.debug.ragdoll.render.skeletonSinglePart.getValue();
    }

    public static void updateDebug(RagdollController ragdollController) {
        if (DebugOptions.instance.character.debug.ragdoll.debug.getValue()) {
            int id = ragdollController.getID();
            boolean simulationActive = DebugOptions.instance.character.debug.ragdoll.simulationActive.getValue();
            Bullet.setRagdollActive(ragdollController.getID(), simulationActive);
            calculateClosestRagdollControllers(ragdollController);
            RagdollControllerDebugRenderer.DebugDrawSettings debugDrawSettings = ragdollController.getDebugDrawSettings();
            if (DebugOptions.instance.character.debug.ragdoll.render.skeleton.getValue() != debugDrawSettings.drawRagdollSkeleton
                || DebugOptions.instance.character.debug.ragdoll.render.skeletonSinglePart.getValue() != debugDrawSettings.drawRagdollSkeletonSinglePart) {
                debugDrawSettings.drawRagdollSkeleton = DebugOptions.instance.character.debug.ragdoll.render.skeleton.getValue();
                debugDrawSettings.drawRagdollSkeletonSinglePart = DebugOptions.instance.character.debug.ragdoll.render.skeletonSinglePart.getValue();
                Bullet.drawDebugRagdollSkeleton(id, debugDrawSettings.drawRagdollSkeleton, debugDrawSettings.drawRagdollSkeletonSinglePart);
            }

            if (DebugOptions.instance.character.debug.ragdoll.render.body.getValue() != debugDrawSettings.drawRagdollBody
                || DebugOptions.instance.character.debug.ragdoll.render.bodySinglePart.getValue() != debugDrawSettings.drawRagdollBodySinglePart) {
                debugDrawSettings.drawRagdollBody = DebugOptions.instance.character.debug.ragdoll.render.body.getValue();
                debugDrawSettings.drawRagdollBodySinglePart = DebugOptions.instance.character.debug.ragdoll.render.bodySinglePart.getValue();
                Bullet.drawDebugRagdollBodyParts(id, debugDrawSettings.drawRagdollBody, debugDrawSettings.drawRagdollBodySinglePart);
            }

            if (DebugOptions.instance.character.debug.ragdoll.render.enable.getValue()) {
                if (closestRagdollControllers.contains(ragdollController)) {
                    PhysicsDebugRenderer.addRagdollRender(ragdollController);
                } else {
                    PhysicsDebugRenderer.removeRagdollRender(ragdollController);
                }
            }
        }
    }

    private static void calculateClosestRagdollControllers(RagdollController in_ragdollController) {
        if (!closestRagdollControllers.contains(in_ragdollController)) {
            if (closestRagdollControllers.size() < 5 && !closestRagdollControllers.contains(in_ragdollController)) {
                closestRagdollControllers.add(in_ragdollController);
            } else {
                IsoGameCharacter gameCharacterObject = in_ragdollController.getGameCharacterObject();
                IsoGameCharacter cameraCharacter = IsoCamera.getCameraCharacter();
                if (cameraCharacter != gameCharacterObject) {
                    for (int i = 0; i < closestRagdollControllers.size(); i++) {
                        RagdollController ragdollController = closestRagdollControllers.get(i);
                        if (ragdollController.isFree()) {
                            closestRagdollControllers.remove(ragdollController);
                            return;
                        }

                        if (gameCharacterObject.distToNearestCamCharacter() < ragdollController.getGameCharacterObject().distToNearestCamCharacter()) {
                            closestRagdollControllers.remove(ragdollController);
                            closestRagdollControllers.add(ragdollController);
                            return;
                        }
                    }
                }
            }
        }
    }

    public static class DebugDrawSettings {
        private boolean drawRagdollBody;
        private boolean drawRagdollBodySinglePart;
        private boolean drawRagdollSkeleton;
        private boolean drawRagdollSkeletonSinglePart;
    }
}
