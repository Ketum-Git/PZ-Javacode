// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.physics;

import jassimp.AiPostProcessSteps;
import jassimp.AiScene;
import jassimp.Jassimp;
import java.io.IOException;
import java.util.EnumSet;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector4f;
import zombie.core.skinnedmodel.model.FileTask_AbstractLoadModel;
import zombie.core.skinnedmodel.model.ModelTxt;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiSceneParams;
import zombie.debug.DebugLog;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.IFileTaskCallback;

public class FileTask_LoadPhysicsShape extends FileTask_AbstractLoadModel {
    PhysicsShape physicsShape;

    public FileTask_LoadPhysicsShape(PhysicsShape mesh, FileSystem fileSystem, IFileTaskCallback cb) {
        super(fileSystem, cb, "media/models", "media/models_x");
        this.physicsShape = mesh;
    }

    @Override
    public String getErrorMessage() {
        return this.fileName;
    }

    @Override
    public void done() {
        PhysicsShapeAssetManager.instance.addWatchedFile(this.fileName);
        this.physicsShape.fullPath = this.fileName;
        this.fileName = null;
        this.physicsShape = null;
    }

    @Override
    public String getRawFileName() {
        String pathStr = this.physicsShape.getPath().getPath();
        int p = pathStr.indexOf(124);
        return p != -1 ? pathStr.substring(0, p) : pathStr;
    }

    private String getMeshName() {
        String pathStr = this.physicsShape.getPath().getPath();
        int p = pathStr.indexOf(124);
        return p != -1 ? pathStr.substring(p + 1) : null;
    }

    @Override
    public ProcessedAiScene loadX() throws IOException {
        DebugLog.Asset.debugln("Loading: %s", this.fileName);
        EnumSet<AiPostProcessSteps> postProcessStepSet = EnumSet.of(
            AiPostProcessSteps.FIND_INSTANCES,
            AiPostProcessSteps.MAKE_LEFT_HANDED,
            AiPostProcessSteps.LIMIT_BONE_WEIGHTS,
            AiPostProcessSteps.TRIANGULATE,
            AiPostProcessSteps.OPTIMIZE_MESHES,
            AiPostProcessSteps.REMOVE_REDUNDANT_MATERIALS,
            AiPostProcessSteps.JOIN_IDENTICAL_VERTICES
        );
        AiScene aiScene = Jassimp.importFile(this.fileName, postProcessStepSet);
        JAssImpImporter.LoadMode mode = JAssImpImporter.LoadMode.StaticMesh;
        ProcessedAiSceneParams params = ProcessedAiSceneParams.create();
        params.scene = aiScene;
        params.mode = mode;
        params.skinnedTo = null;
        params.meshName = this.getMeshName();
        ProcessedAiScene processedAiScene = ProcessedAiScene.process(params);
        JAssImpImporter.takeOutTheTrash(aiScene);
        return processedAiScene;
    }

    @Override
    public ProcessedAiScene loadFBX() throws IOException {
        DebugLog.Asset.debugln("Loading: %s", this.fileName);
        EnumSet<AiPostProcessSteps> postProcessStepSet = EnumSet.of(
            AiPostProcessSteps.FIND_INSTANCES,
            AiPostProcessSteps.MAKE_LEFT_HANDED,
            AiPostProcessSteps.LIMIT_BONE_WEIGHTS,
            AiPostProcessSteps.TRIANGULATE,
            AiPostProcessSteps.OPTIMIZE_MESHES,
            AiPostProcessSteps.REMOVE_REDUNDANT_MATERIALS,
            AiPostProcessSteps.JOIN_IDENTICAL_VERTICES
        );
        this.handlePostProcessFlags(postProcessStepSet, this.physicsShape.assetParams.postProcess);
        AiScene aiScene = Jassimp.importFile(this.fileName, postProcessStepSet);
        JAssImpImporter.LoadMode mode = JAssImpImporter.LoadMode.StaticMesh;
        Quaternion rotateModifier = new Quaternion();
        float angle = (float) (Math.PI / 2);
        Vector4f axisAngle = new Vector4f(1.0F, 0.0F, 0.0F, (float) (-Math.PI / 2));
        rotateModifier.setFromAxisAngle(axisAngle);
        ProcessedAiSceneParams params = ProcessedAiSceneParams.create();
        params.scene = aiScene;
        params.mode = mode;
        params.skinnedTo = null;
        params.meshName = this.getMeshName();
        params.animBonesScaleModifier = 0.01F;
        params.animBonesRotateModifier = rotateModifier;
        params.allMeshes = this.physicsShape.assetParams.allMeshes;
        ProcessedAiScene processedAiScene = ProcessedAiScene.process(params);
        JAssImpImporter.takeOutTheTrash(aiScene);
        return processedAiScene;
    }

    @Override
    public ProcessedAiScene loadGLTF() throws IOException {
        DebugLog.Asset.debugln("Loading: %s", this.fileName);
        EnumSet<AiPostProcessSteps> postProcessStepSet = EnumSet.of(
            AiPostProcessSteps.FIND_INSTANCES,
            AiPostProcessSteps.MAKE_LEFT_HANDED,
            AiPostProcessSteps.LIMIT_BONE_WEIGHTS,
            AiPostProcessSteps.TRIANGULATE,
            AiPostProcessSteps.OPTIMIZE_MESHES,
            AiPostProcessSteps.REMOVE_REDUNDANT_MATERIALS,
            AiPostProcessSteps.JOIN_IDENTICAL_VERTICES
        );
        this.handlePostProcessFlags(postProcessStepSet, this.physicsShape.assetParams.postProcess);
        AiScene aiScene = Jassimp.importFile(this.fileName, postProcessStepSet);
        JAssImpImporter.LoadMode mode = JAssImpImporter.LoadMode.StaticMesh;
        ProcessedAiSceneParams params = ProcessedAiSceneParams.create();
        params.scene = aiScene;
        params.mode = mode;
        params.skinnedTo = null;
        params.meshName = this.getMeshName();
        params.animBonesScaleModifier = 1.0F;
        params.animBonesRotateModifier = null;
        params.allMeshes = this.physicsShape.assetParams.allMeshes;
        ProcessedAiScene processedAiScene = ProcessedAiScene.process(params);
        JAssImpImporter.takeOutTheTrash(aiScene);
        return processedAiScene;
    }

    @Override
    public ModelTxt loadTxt() throws IOException {
        throw new IOException("unsupported format");
    }

    private void handlePostProcessFlags(EnumSet<AiPostProcessSteps> postProcessStepSet, String postProcess) {
        if (postProcess != null) {
            String[] ss = this.physicsShape.assetParams.postProcess.split(";");

            for (String flagStr : ss) {
                if (flagStr.startsWith("+")) {
                    postProcessStepSet.add(AiPostProcessSteps.valueOf(flagStr.substring(1)));
                } else if (flagStr.startsWith("-")) {
                    postProcessStepSet.remove(AiPostProcessSteps.valueOf(flagStr.substring(1)));
                }
            }
        }
    }
}
