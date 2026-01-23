// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

import java.util.HashMap;
import java.util.Objects;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL43;
import zombie.core.ShaderHelper;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.SkinningData;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureDraw;

public class Shader implements IShaderProgramListener {
    public static final HashMap<Integer, Shader> ShaderMap = new HashMap<>();
    private final String name;
    private int shaderMapId;
    private final Object shaderProgramLock = new Object();
    private volatile boolean shaderProgramInitialized;
    private Shader.ShaderProgramSpecific shaderProgramSpecific;

    public Shader(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean GetRequiresSkinning() {
        return this.getRequiresSkinning();
    }

    private void Init() {
        this.initShaderProgram();
    }

    public void Activate() {
        GL20.glUseProgram(this.getID());
    }

    public void SetupInstancedData() {
    }

    public void SetupBones(ModelMesh mesh) {
        if (this.getRequiresSkinning()) {
            this.shaderProgramSpecific.skinning.SetupBones(mesh);
        }
    }

    public void setTexture(Texture tex) {
        this.initShaderProgram();
        this.shaderProgramSpecific.tex = tex;
    }

    public int getID() {
        return this.getShaderProgram().getShaderID();
    }

    public void Start() {
        ShaderHelper.glUseProgramObjectARB(this.getShaderProgram().getShaderID());
    }

    public void End() {
        ShaderHelper.glUseProgramObjectARB(0);
    }

    public void destroy() {
        this.getShaderProgram().destroy();
        ShaderMap.remove(this.shaderMapId);
        this.shaderMapId = 0;
    }

    public void startMainThread(TextureDraw texd, int playerIndex) {
        int n = 0;
    }

    public void startRenderThread(TextureDraw tex) {
        int n = 0;
    }

    public void postRender(TextureDraw texd) {
        int n = 0;
    }

    public boolean isCompiled() {
        return this.getShaderProgram().isCompiled();
    }

    @Override
    public void callback(ShaderProgram sender) {
        ShaderMap.remove(this.shaderMapId);
        this.shaderMapId = sender.getShaderID();
        ShaderMap.put(this.shaderMapId, this);
        this.onCompileSuccess(sender);
    }

    protected void onCompileSuccess(ShaderProgram shaderProgram) {
        this.shaderProgramSpecific.requiresSkinning = this.shaderProgramSpecific.skinning.Init();
    }

    public ShaderProgram getProgram() {
        return this.getShaderProgram();
    }

    public ShaderProgram getShaderProgram() {
        this.initShaderProgram();
        return this.shaderProgramSpecific.shaderProgram;
    }

    protected void initShaderProgram() {
        if (!this.shaderProgramInitialized) {
            synchronized (this.shaderProgramLock) {
                if (!this.shaderProgramInitialized) {
                    this.shaderProgramSpecific = new Shader.ShaderProgramSpecific();
                    RenderThread.invokeOnRenderContext(this, _this -> {
                        _this.shaderProgramSpecific.shaderProgram = ShaderProgram.createShaderProgram(this.getName(), false, false, false);
                        _this.shaderProgramSpecific.shaderProgram.addCompileListener(this);
                        _this.shaderProgramInitialized = true;
                        _this.shaderProgramSpecific.shaderProgram.compile();
                    });
                }
            }
        }
    }

    public int getWidth() {
        this.initShaderProgram();
        return this.shaderProgramSpecific.width;
    }

    public int getHeight() {
        this.initShaderProgram();
        return this.shaderProgramSpecific.height;
    }

    public void setWidth(int in_newWidth) {
        this.initShaderProgram();
        this.shaderProgramSpecific.width = in_newWidth;
    }

    public void setHeight(int in_newHeight) {
        this.initShaderProgram();
        this.shaderProgramSpecific.height = in_newHeight;
    }

    public boolean getRequiresSkinning() {
        this.initShaderProgram();
        return this.shaderProgramSpecific.requiresSkinning;
    }

    private class ShaderProgramSpecific {
        private ShaderProgram shaderProgram;
        private Texture tex;
        private int width;
        private int height;
        private final Shader.Skinning skinning;
        private boolean requiresSkinning;

        private ShaderProgramSpecific() {
            Objects.requireNonNull(Shader.this);
            super();
            this.skinning = Shader.this.new Skinning();
        }
    }

    private class Skinning {
        public Shader.Skinning.IDs ids;

        private Skinning() {
            Objects.requireNonNull(Shader.this);
            super();
            this.ids = new Shader.Skinning.IDs();
        }

        public boolean Init() {
            int id = Shader.this.getID();
            this.ids.boneCount = GL20.glGetUniformLocation(id, "boneCount");
            this.ids.boneMatrices = GL20.glGetUniformLocation(id, "boneMatrices");
            this.ids.boneWeights = GL20.glGetAttribLocation(id, "boneWeights");
            this.ids.boneIds = GL20.glGetAttribLocation(id, "boneIDs");
            return this.ids.boneCount >= 0 && this.ids.boneWeights >= 0 && this.ids.boneMatrices >= 0 && this.ids.boneIds >= 0;
        }

        public void SetupBones(ModelMesh mesh) {
            int numBones = mesh.skinningData.numBones();
            GL20.glUniform1i(this.ids.boneCount, numBones);
            SkinningData.Buffers buffers = mesh.skinningData.buffers;
            GL20.glUniform4fv(this.ids.boneMatrices, buffers.boneMatrices);
            GL20.glEnableVertexAttribArray(this.ids.boneWeights);
            GL20.glVertexAttribPointer(this.ids.boneWeights, 4, 5126, false, 0, buffers.boneWeights);
            GL20.glEnableVertexAttribArray(this.ids.boneIds);
            GL20.glVertexAttribPointer(this.ids.boneIds, 4, 5123, false, 0, buffers.boneIds);
            GL20.glEnableVertexAttribArray(this.ids.boneMatrices);
            GL20.glVertexAttribPointer(this.ids.boneMatrices, 16, 35676, false, 0, buffers.boneMatrices);
            GL43.glVertexAttribDivisor(this.ids.boneMatrices, 0);
        }

        public class IDs {
            public int boneCount;
            public int boneWeights;
            public int boneMatrices;
            public int boneIds;

            public IDs() {
                Objects.requireNonNull(Skinning.this);
                super();
                this.boneCount = -1;
                this.boneWeights = -1;
                this.boneMatrices = -1;
                this.boneIds = -1;
            }
        }
    }
}
