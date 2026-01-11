// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model.jassimp;

import jassimp.AiBuiltInWrapperProvider;
import jassimp.AiMesh;
import jassimp.AiNode;
import jassimp.AiScene;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import org.lwjgl.util.vector.Matrix4f;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.physics.PhysicsShape;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.animation.Keyframe;
import zombie.core.skinnedmodel.model.AnimationAsset;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.debug.DebugLog;
import zombie.util.StringUtils;

public final class ProcessedAiScene {
    private ImportedSkeleton skeleton;
    private ImportedSkinnedMesh skinnedMesh;
    private final ArrayList<ImportedStaticMesh> staticMeshes = new ArrayList<>();

    private ProcessedAiScene() {
    }

    public static ProcessedAiScene process(ProcessedAiSceneParams params) {
        ProcessedAiScene processedAiScene = new ProcessedAiScene();
        if (params.allMeshes) {
            processedAiScene.processAllMeshes(params);
            return processedAiScene;
        } else {
            processedAiScene.processAiScene(params);
            return processedAiScene;
        }
    }

    private void processAiScene(ProcessedAiSceneParams params) {
        AiScene scene = params.scene;
        JAssImpImporter.LoadMode mode = params.mode;
        String meshName = params.meshName;
        AiMesh mesh = this.findMesh(scene, meshName);
        if (mesh == null) {
            DebugLog.General.error("No such mesh \"%s\"", meshName);
        } else {
            if (mode != JAssImpImporter.LoadMode.StaticMesh && mesh.hasBones()) {
                ImportedSkeletonParams skeletonParams = ImportedSkeletonParams.create(params, mesh);
                this.skeleton = ImportedSkeleton.process(skeletonParams);
                if (mode != JAssImpImporter.LoadMode.AnimationOnly) {
                    this.skinnedMesh = new ImportedSkinnedMesh(this.skeleton, mesh);
                    this.skinnedMesh.transform = this.initMeshTransform(scene, mesh);
                }
            } else {
                ImportedStaticMesh staticMesh = new ImportedStaticMesh(mesh);
                staticMesh.transform = this.initMeshTransform(scene, mesh);
                this.staticMeshes.add(staticMesh);
            }
        }
    }

    private void processAllMeshes(ProcessedAiSceneParams params) {
        AiScene scene = params.scene;
        JAssImpImporter.LoadMode mode = params.mode;
        if (mode == JAssImpImporter.LoadMode.StaticMesh) {
            for (AiMesh mesh : scene.getMeshes()) {
                ImportedStaticMesh staticMesh = new ImportedStaticMesh(mesh);
                staticMesh.transform = this.initMeshTransform(scene, mesh);
                this.staticMeshes.add(staticMesh);
            }
        }
    }

    private Matrix4f initMeshTransform(AiScene scene, AiMesh mesh) {
        AiBuiltInWrapperProvider wrapper = new AiBuiltInWrapperProvider();
        AiNode rootNode = scene.getSceneRoot(wrapper);
        AiNode node = this.findParentNodeForMesh(scene.getMeshes().indexOf(mesh), rootNode);
        if (node == null) {
            return null;
        } else {
            Matrix4f transform = JAssImpImporter.getMatrixFromAiMatrix(node.getTransform(wrapper));
            Matrix4f pXfrm = new Matrix4f();

            for (AiNode parent = node.getParent(); parent != null; parent = parent.getParent()) {
                JAssImpImporter.getMatrixFromAiMatrix(parent.getTransform(wrapper), pXfrm);
                Matrix4f.mul(pXfrm, transform, transform);
            }

            return transform;
        }
    }

    private AiMesh findMesh(AiScene scene, String name) {
        if (scene.getNumMeshes() == 0) {
            return null;
        } else if (StringUtils.isNullOrWhitespace(name)) {
            for (AiMesh mesh : scene.getMeshes()) {
                if (mesh.hasBones()) {
                    return mesh;
                }
            }

            return scene.getMeshes().get(0);
        } else {
            for (AiMesh meshx : scene.getMeshes()) {
                if (meshx.getName().equalsIgnoreCase(name)) {
                    return meshx;
                }
            }

            AiBuiltInWrapperProvider wrapper = new AiBuiltInWrapperProvider();
            AiNode rootNode = scene.getSceneRoot(wrapper);
            AiNode node = JAssImpImporter.FindNode(name, rootNode);
            if (node != null && node.getNumMeshes() == 1) {
                int meshRef = node.getMeshes()[0];
                return scene.getMeshes().get(meshRef);
            } else {
                return null;
            }
        }
    }

    private AiNode findParentNodeForMesh(int meshRef, AiNode node) {
        for (int i = 0; i < node.getNumMeshes(); i++) {
            if (node.getMeshes()[i] == meshRef) {
                return node;
            }
        }

        for (AiNode child : node.getChildren()) {
            AiNode node1 = this.findParentNodeForMesh(meshRef, child);
            if (node1 != null) {
                return node1;
            }
        }

        return null;
    }

    public void applyToMesh(ModelMesh mesh, JAssImpImporter.LoadMode mode, boolean bReverse, SkinningData skinnedTo) {
        mesh.transform = null;
        ImportedStaticMesh staticMesh = this.staticMeshes.isEmpty() ? null : this.staticMeshes.get(0);
        if (staticMesh != null) {
            mesh.minXyz.set(staticMesh.minXyz);
            mesh.maxXyz.set(staticMesh.maxXyz);
            if (staticMesh.transform != null) {
                mesh.transform = PZMath.convertMatrix(staticMesh.transform, new org.joml.Matrix4f());
            }

            if (!ModelManager.noOpenGL) {
                mesh.hasVbo = true;
                VertexBufferObject.VertexArray vertices = staticMesh.verticesUnskinned;
                int[] elements = staticMesh.elements;
                RenderThread.queueInvokeOnRenderContext(() -> mesh.SetVertexBuffer(new VertexBufferObject(vertices, elements)));
            }
        }

        if (mesh.skinningData != null) {
            if (skinnedTo == null || mesh.skinningData.animationClips != skinnedTo.animationClips) {
                mesh.skinningData.animationClips.clear();
            }

            mesh.skinningData.inverseBindPose.clear();
            mesh.skinningData.bindPose.clear();
            mesh.skinningData.boneOffset.clear();
            mesh.skinningData.boneIndices.clear();
            mesh.skinningData.skeletonHierarchy.clear();
            mesh.skinningData = null;
        }

        if (this.skeleton != null) {
            ImportedSkeleton sk = this.skeleton;
            HashMap<String, AnimationClip> clips = sk.clips;
            mesh.meshAnimationClips.clear();
            mesh.meshAnimationClips.putAll(clips);
            if (skinnedTo != null) {
                sk.clips.clear();
                clips = skinnedTo.animationClips;
            }

            JAssImpImporter.replaceHashMapKeys(sk.boneIndices, "SkinningData.boneIndices");
            mesh.skinningData = new SkinningData(clips, sk.bindPose, sk.invBindPose, sk.skinOffsetMatrices, sk.skeletonHierarchy, sk.boneIndices);
        }

        if (this.skinnedMesh != null) {
            if (this.skinnedMesh.transform != null) {
                mesh.transform = PZMath.convertMatrix(this.skinnedMesh.transform, new org.joml.Matrix4f());
            }

            if (!ModelManager.noOpenGL) {
                mesh.hasVbo = true;
                VertexBufferObject.VertexArray vertices = this.skinnedMesh.vertices;
                int[] elements = this.skinnedMesh.elements;
                RenderThread.queueInvokeOnRenderContext(() -> mesh.SetVertexBuffer(new VertexBufferObject(vertices, elements, bReverse)));
            }
        }

        this.skeleton = null;
        this.skinnedMesh = null;
        this.staticMeshes.clear();
    }

    public void applyToPhysicsShape(PhysicsShape physicsShape) {
        for (int i = 0; i < this.staticMeshes.size(); i++) {
            ImportedStaticMesh staticMesh = this.staticMeshes.get(i);
            PhysicsShape.OneMesh oneMesh = new PhysicsShape.OneMesh();
            oneMesh.transform = null;
            if (staticMesh.transform != null) {
                oneMesh.transform = PZMath.convertMatrix(staticMesh.transform, new org.joml.Matrix4f());
            }

            oneMesh.minXyz.set(staticMesh.minXyz);
            oneMesh.maxXyz.set(staticMesh.maxXyz);
            VertexBufferObject.VertexArray vertices = staticMesh.verticesUnskinned;
            int vertexArrayIndex = vertices.format.indexOf(VertexBufferObject.VertexType.VertexArray);
            if (vertexArrayIndex != -1) {
                oneMesh.points = new float[vertices.numVertices * 3];

                for (int j = 0; j < vertices.numVertices; j++) {
                    float x = vertices.getElementFloat(j, vertexArrayIndex, 0);
                    float y = vertices.getElementFloat(j, vertexArrayIndex, 1);
                    float z = vertices.getElementFloat(j, vertexArrayIndex, 2);
                    oneMesh.points[j * 3] = x;
                    oneMesh.points[j * 3 + 1] = y;
                    oneMesh.points[j * 3 + 2] = z;
                }

                physicsShape.meshes.add(oneMesh);
            }
        }

        this.skeleton = null;
        this.skinnedMesh = null;
        this.staticMeshes.clear();
    }

    public void applyToAnimation(AnimationAsset anim) {
        for (Entry<String, AnimationClip> e : this.skeleton.clips.entrySet()) {
            for (Keyframe keyframe : e.getValue().getKeyframes()) {
                keyframe.boneName = JAssImpImporter.getSharedString(keyframe.boneName, "Keyframe.BoneName");
            }
        }

        anim.animationClips = this.skeleton.clips;
        this.skeleton = null;
    }
}
