// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.util.HashMap;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;
import zombie.core.skinnedmodel.animation.AnimationClip;
import zombie.core.skinnedmodel.model.jassimp.JAssImpImporter;
import zombie.core.skinnedmodel.model.jassimp.ProcessedAiScene;
import zombie.core.skinnedmodel.shader.Shader;

/**
 * Created by LEMMYATI on 03/01/14.
 */
public final class ModelMesh extends Asset {
    public VertexBufferObject vb;
    public final Vector3f minXyz = new Vector3f(Float.MAX_VALUE);
    public final Vector3f maxXyz = new Vector3f(-Float.MAX_VALUE);
    public final HashMap<String, AnimationClip> meshAnimationClips = new HashMap<>();
    public SkinningData skinningData;
    public SoftwareModelMesh softwareMesh;
    public ModelMesh.MeshAssetParams assetParams;
    public Matrix4f transform;
    public boolean hasVbo;
    protected boolean isStatic;
    public ModelMesh animationsMesh;
    public String postProcess;
    public int modificationCount;
    public String fullPath;
    public static final AssetType ASSET_TYPE = new AssetType("Mesh");

    public ModelMesh(AssetPath path, AssetManager manager, ModelMesh.MeshAssetParams params) {
        super(path, manager);
        this.assetParams = params;
        this.isStatic = this.assetParams != null && this.assetParams.isStatic;
        if (!this.isStatic && this.assetParams.animationsMesh == null) {
            this.assetParams.animationsMesh = this;
        }

        this.animationsMesh = this.assetParams == null ? null : this.assetParams.animationsMesh;
        this.postProcess = this.assetParams == null ? null : this.assetParams.postProcess;
    }

    protected void onLoadedX(ProcessedAiScene processedAiScene) {
        JAssImpImporter.LoadMode mode = this.assetParams.isStatic ? JAssImpImporter.LoadMode.StaticMesh : JAssImpImporter.LoadMode.Normal;
        SkinningData skinningData = this.assetParams.animationsMesh == null ? null : this.assetParams.animationsMesh.skinningData;
        processedAiScene.applyToMesh(this, mode, false, skinningData);
        if (this == this.assetParams.animationsMesh) {
            if (this.skinningData == null) {
                boolean var4 = true;
            } else {
                this.skinningData.animationClips.putAll(this.meshAnimationClips);
            }
        }

        this.modificationCount++;
    }

    protected void onLoadedTxt(ModelTxt modelTxt) {
        SkinningData skinningData = this.assetParams.animationsMesh == null ? null : this.assetParams.animationsMesh.skinningData;
        ModelLoader.instance.applyToMesh(modelTxt, this, skinningData);
    }

    public void SetVertexBuffer(VertexBufferObject vb) {
        this.clear();
        this.vb = vb;
        this.isStatic = vb == null || vb.isStatic;
    }

    public void Draw(Shader shader) {
        if (this.vb != null) {
            this.vb.Draw(shader);
        }
    }

    public void DrawInstanced(Shader shader, int instanceCount) {
        this.vb.DrawInstanced(shader, instanceCount);
    }

    @Override
    public void onBeforeReady() {
        super.onBeforeReady();
        if (this.assetParams != null) {
            this.assetParams.animationsMesh = null;
            this.assetParams = null;
        }
    }

    @Override
    public boolean isReady() {
        return super.isReady() && (!this.hasVbo || this.vb != null);
    }

    @Override
    public void setAssetParams(AssetManager.AssetParams params) {
        this.assetParams = (ModelMesh.MeshAssetParams)params;
    }

    @Override
    public AssetType getType() {
        return ASSET_TYPE;
    }

    public void clear() {
        if (this.vb != null) {
            this.vb.clear();
            this.vb = null;
        }
    }

    public static final class MeshAssetParams extends AssetManager.AssetParams {
        public boolean isStatic;
        public ModelMesh animationsMesh;
        public String postProcess;
    }
}
