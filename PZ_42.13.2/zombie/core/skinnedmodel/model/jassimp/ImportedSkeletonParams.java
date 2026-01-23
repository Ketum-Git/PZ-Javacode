// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model.jassimp;

import jassimp.AiMesh;

public class ImportedSkeletonParams extends ProcessedAiSceneParams {
    AiMesh mesh;

    ImportedSkeletonParams() {
    }

    public static ImportedSkeletonParams create(ProcessedAiSceneParams aiSceneParams, AiMesh mesh) {
        ImportedSkeletonParams params = new ImportedSkeletonParams();
        params.set(aiSceneParams);
        params.mesh = mesh;
        return params;
    }
}
