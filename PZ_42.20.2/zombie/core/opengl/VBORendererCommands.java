// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public final class VBORendererCommands {
    public static final short COMMAND_StartRun = 0;
    public static final short COMMAND_RenderRun = 1;
    public static final short COMMAND_PushAndLoadMatrix = 2;
    public static final short COMMAND_PushAndMultMatrix = 3;
    public static final short COMMAND_PopMatrix = 4;
    public static final short COMMAND_UseProgram = 5;
    public static final short COMMAND_Shader1f = 6;
    public static final short COMMAND_Shader2f = 7;
    public static final short COMMAND_Shader3f = 8;
    public static final short COMMAND_Shader4f = 9;
    private final VBORenderer vboRenderer;
    private int commandCount;
    private ByteBuffer commandBuffer;
    private final ArrayList<Object> objects = new ArrayList<>();
    private final Matrix4f tempMatrix4f = new Matrix4f();

    VBORendererCommands(VBORenderer vbor) {
        this.vboRenderer = vbor;
    }

    public void adopt(VBORendererCommands rhs) {
        if (rhs.commandCount != 0) {
            this.commandCount = this.commandCount + rhs.commandCount;
            this.reserve(rhs.commandBuffer.position());
            rhs.commandBuffer.flip();
            this.commandBuffer.put(rhs.commandBuffer);
            PZArrayUtil.addAll(this.objects, rhs.objects);
            rhs.clear();
        }
    }

    public void clear() {
        if (this.commandBuffer != null) {
            this.commandCount = 0;
            this.commandBuffer.clear();
            this.objects.clear();
        }
    }

    public int position() {
        return this.commandBuffer == null ? 0 : this.commandBuffer.position();
    }

    public void putFloat(float value) {
        this.reserve(4);
        this.commandBuffer.putFloat(value);
    }

    public void putInt(int value) {
        this.reserve(4);
        this.commandBuffer.putInt(value);
    }

    public void putMatrix4f(Matrix4f m) {
        this.reserve(64);
        m.get(this.commandBuffer);
        this.commandBuffer.position(this.commandBuffer.position() + 64);
    }

    public void putShort(short value) {
        this.reserve(2);
        this.commandBuffer.putShort(value);
    }

    public void putObject(Object object) {
        this.objects.add(object);
    }

    public float getFloat() {
        return this.commandBuffer.getFloat();
    }

    public int getInt() {
        return this.commandBuffer.getInt();
    }

    public float getShort() {
        return this.commandBuffer.getShort();
    }

    public <C> C getObject(Class<C> clazz) {
        Object object = this.objects.remove(0);
        return Type.tryCastTo(object, clazz);
    }

    private void reserve(int numBytes) {
        if (this.commandBuffer == null) {
            int blocks = (int)PZMath.ceil(numBytes / 512.0F);
            this.commandBuffer = ByteBuffer.allocateDirect(blocks * 512);
        } else if (this.commandBuffer.position() + numBytes > this.commandBuffer.capacity()) {
            int blocks = (int)PZMath.ceil((this.commandBuffer.position() + numBytes) / 512.0F);
            ByteBuffer old = this.commandBuffer;
            this.commandBuffer = ByteBuffer.allocateDirect(blocks * 512);
            if (old.position() > 0) {
                old.flip();
                this.commandBuffer.put(old);
            }
        }
    }

    public void invoke() {
        if (this.commandBuffer != null) {
            int position = this.commandBuffer.position();
            this.commandBuffer.position(0);

            try {
                for (int i = 0; i < this.commandCount; i++) {
                    short command = this.commandBuffer.getShort();
                    this.invokeCommand(command);
                }
            } finally {
                this.commandBuffer.position(position);
            }
        }
    }

    private void invokeCommand(short command) {
        switch (command) {
            case 0:
                this.vboRenderer.startNextRun();
                break;
            case 1:
                this.vboRenderer.renderNextRun();
                break;
            case 2: {
                int mode = this.getInt();
                this.tempMatrix4f.set(this.commandBuffer);
                this.commandBuffer.position(this.commandBuffer.position() + 64);
                PZGLUtil.pushAndLoadMatrix(mode, this.tempMatrix4f);
                break;
            }
            case 3: {
                int mode = this.getInt();
                this.tempMatrix4f.set(this.commandBuffer);
                this.commandBuffer.position(this.commandBuffer.position() + 64);
                PZGLUtil.pushAndMultMatrix(mode, this.tempMatrix4f);
                break;
            }
            case 4:
                PZGLUtil.popMatrix(this.getInt());
                break;
            case 5:
                ShaderProgram shaderProgram = this.getObject(ShaderProgram.class);
                shaderProgram.Start();
                VertexBufferObject.setModelViewProjection(shaderProgram);
                break;
            case 6:
                GL20.glUniform1f(this.getInt(), this.getFloat());
                break;
            case 7:
                GL20.glUniform2f(this.getInt(), this.getFloat(), this.getFloat());
            case 8:
            default:
                break;
            case 9:
                GL20.glUniform4f(this.getInt(), this.getFloat(), this.getFloat(), this.getFloat(), this.getFloat());
        }
    }

    public void cmdStartRun() {
        this.putShort((short)0);
        this.commandCount++;
    }

    public void cmdRenderRun() {
        this.putShort((short)1);
        this.commandCount++;
    }

    public void cmdPushAndLoadMatrix(int mode, Matrix4f m) {
        this.putShort((short)2);
        this.putInt(mode);
        this.putMatrix4f(m);
        this.commandCount++;
    }

    public void cmdPushAndMultMatrix(int mode, Matrix4f m) {
        this.putShort((short)3);
        this.putInt(mode);
        this.putMatrix4f(m);
        this.commandCount++;
    }

    public void cmdPopMatrix(int mode) {
        this.putShort((short)4);
        this.putInt(mode);
        this.commandCount++;
    }

    public void cmdShader1f(int uniform, float f1) {
        this.putShort((short)6);
        this.putInt(uniform);
        this.putFloat(f1);
        this.commandCount++;
    }

    public void cmdShader2f(int uniform, float f1, float f2) {
        this.putShort((short)7);
        this.putInt(uniform);
        this.putFloat(f1);
        this.putFloat(f2);
        this.commandCount++;
    }

    public void cmdShader3f(int uniform, float f1, float f2, float f3) {
        this.putShort((short)8);
        this.putInt(uniform);
        this.putFloat(f1);
        this.putFloat(f2);
        this.putFloat(f3);
        this.commandCount++;
    }

    public void cmdShader4f(int uniform, float f1, float f2, float f3, float f4) {
        this.putShort((short)9);
        this.putInt(uniform);
        this.putFloat(f1);
        this.putFloat(f2);
        this.putFloat(f3);
        this.putFloat(f4);
        this.commandCount++;
    }

    public void cmdUseProgram(ShaderProgram shaderProgram) {
        this.putShort((short)5);
        this.putObject(shaderProgram);
        this.commandCount++;
    }
}
