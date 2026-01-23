// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.rendering;

import org.lwjgl.opengl.GL15;
import org.lwjglx.BufferUtils;
import org.lwjglx.opengl.Util;
import zombie.core.Core;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.debug.DebugOptions;

public class InstancedBuffer extends ShaderBuffer {
    private final ShaderBufferData bufferData;

    public InstancedBuffer(Shader shader, int maxInstances) {
        this.bufferData = new ShaderBufferData(shader);
        int size = this.bufferData.GetSize() * maxInstances;
        if (size > 0) {
            this.bufferId = GL15.glGenBuffers();
            this.data = BufferUtils.createByteBuffer(size);
            GL15.glBindBuffer(37074, this.bufferId);
            GL15.glBufferData(37074, this.data, 35048);
            Util.checkGLError();
        } else {
            this.bufferId = -1;
            this.data = null;
        }
    }

    public ShaderBufferData GetBufferData() {
        return this.bufferData;
    }

    public void PushProperties(ShaderPropertyBlock properties) {
        this.bufferData.ResetParameters();
        this.bufferData.CopyParameters(properties);
        this.bufferData.PushParameters(this);
    }

    public void PushInstanced(ShaderPropertyBlock properties) {
        if (Core.debug && DebugOptions.instance.newedDebugOnlyOption.getValue()) {
            properties.StoreProperties();
            this.bufferData.PushInstanced(this, properties);
        } else {
            this.bufferData.ResetInstanced();
            this.bufferData.CopyInstanced(properties);
            this.bufferData.PushInstanced(this);
        }
    }

    public void PushUniforms(ShaderPropertyBlock properties) {
        this.bufferData.ResetUniforms();
        this.bufferData.CopyUniforms(properties);
        this.bufferData.PushUniforms();
    }

    @Override
    protected void PreUpdate() {
        this.data.position(this.bufferData.GetCurrentInstance() * this.bufferData.GetSize());
    }

    @Override
    public void OnReset() {
        this.bufferData.Reset();
    }
}
