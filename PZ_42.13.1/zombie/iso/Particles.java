// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjglx.BufferUtils;
import zombie.GameTime;
import zombie.core.SpriteRenderer;
import zombie.core.VBO.GLBufferObject15;
import zombie.core.VBO.GLBufferObjectARB;
import zombie.core.VBO.IGLBufferObject;
import zombie.core.opengl.RenderThread;
import zombie.debug.DebugLog;

public abstract class Particles {
    private float particlesTime;
    public static int particleSystemsCount;
    public static int particleSystemsLast;
    public static final ArrayList<Particles> ParticleSystems = new ArrayList<>();
    private final int id;
    int particleVertexBuffer;
    public static IGLBufferObject funcs;
    private Matrix4f projectionMatrix;
    private Matrix4f mvpMatrix;
    private final FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);

    public static synchronized int addParticle(Particles p) {
        if (ParticleSystems.size() == particleSystemsCount) {
            ParticleSystems.add(p);
            particleSystemsCount++;
            return ParticleSystems.size() - 1;
        } else {
            int i = particleSystemsLast;
            if (i < ParticleSystems.size()) {
                if (ParticleSystems.get(i) == null) {
                    particleSystemsLast = i;
                    ParticleSystems.set(i, p);
                    particleSystemsCount++;
                }

                return i;
            } else {
                i = 0;
                if (i < particleSystemsLast) {
                    if (ParticleSystems.get(i) == null) {
                        particleSystemsLast = i;
                        ParticleSystems.set(i, p);
                        particleSystemsCount++;
                    }

                    return i;
                } else {
                    DebugLog.log("ERROR: addParticle has unknown error");
                    return -1;
                }
            }
        }
    }

    public static synchronized void deleteParticle(int i) {
        ParticleSystems.set(i, null);
        particleSystemsCount--;
    }

    public static void init() {
        if (funcs == null) {
            if (!GL.getCapabilities().OpenGL33) {
                System.out.println("OpenGL 3.3 don't supported");
            }

            if (GL.getCapabilities().OpenGL15) {
                System.out.println("OpenGL 1.5 buffer objects supported");
                funcs = new GLBufferObject15();
            } else {
                if (!GL.getCapabilities().GL_ARB_vertex_buffer_object) {
                    throw new RuntimeException("Neither OpenGL 1.5 nor GL_ARB_vertex_buffer_object supported");
                }

                System.out.println("GL_ARB_vertex_buffer_object supported");
                funcs = new GLBufferObjectARB();
            }
        }
    }

    public void initBuffers() {
        ByteBuffer vertices = MemoryUtil.memAlloc(48);
        vertices.clear();
        vertices.putFloat(-1.0F);
        vertices.putFloat(-1.0F);
        vertices.putFloat(0.0F);
        vertices.putFloat(1.0F);
        vertices.putFloat(-1.0F);
        vertices.putFloat(0.0F);
        vertices.putFloat(-1.0F);
        vertices.putFloat(1.0F);
        vertices.putFloat(0.0F);
        vertices.putFloat(1.0F);
        vertices.putFloat(1.0F);
        vertices.putFloat(0.0F);
        vertices.flip();
        this.particleVertexBuffer = funcs.glGenBuffers();
        funcs.glBindBuffer(34962, this.particleVertexBuffer);
        funcs.glBufferData(34962, vertices, 35044);
        MemoryUtil.memFree(vertices);
        this.createParticleBuffers();
    }

    public void destroy() {
        deleteParticle(this.id);
        funcs.glDeleteBuffers(this.particleVertexBuffer);
        this.destroyParticleBuffers();
    }

    public abstract void reloadShader();

    public Particles() {
        RenderThread.invokeOnRenderContext(() -> {
            init();
            this.initBuffers();
            this.projectionMatrix = new Matrix4f();
        });
        this.reloadShader();
        this.id = addParticle(this);
    }

    private static Matrix4f orthogonal(float left, float right, float bottom, float top, float near, float far) {
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.m00 = 2.0F / (right - left);
        matrix.m11 = 2.0F / (top - bottom);
        matrix.m22 = -2.0F / (far - near);
        matrix.m32 = (-far - near) / (far - near);
        matrix.m30 = (-right - left) / (right - left);
        matrix.m31 = (-top - bottom) / (top - bottom);
        return matrix;
    }

    public void render() {
        int nPlayer = IsoCamera.frameState.playerIndex;
        this.particlesTime = this.particlesTime + 0.0166F * GameTime.getInstance().getMultiplier();
        this.updateMVPMatrix();
        SpriteRenderer.instance.drawParticles(nPlayer, 0, 0);
    }

    private void updateMVPMatrix() {
        this.projectionMatrix = orthogonal(
            IsoCamera.frameState.offX,
            IsoCamera.frameState.offX + IsoCamera.frameState.offscreenWidth,
            IsoCamera.frameState.offY + IsoCamera.frameState.offscreenHeight,
            IsoCamera.frameState.offY,
            -1.0F,
            1.0F
        );
        this.mvpMatrix = this.projectionMatrix;
    }

    public FloatBuffer getMVPMatrix() {
        this.floatBuffer.clear();
        this.floatBuffer.put(this.mvpMatrix.m00);
        this.floatBuffer.put(this.mvpMatrix.m10);
        this.floatBuffer.put(this.mvpMatrix.m20);
        this.floatBuffer.put(this.mvpMatrix.m30);
        this.floatBuffer.put(this.mvpMatrix.m01);
        this.floatBuffer.put(this.mvpMatrix.m11);
        this.floatBuffer.put(this.mvpMatrix.m21);
        this.floatBuffer.put(this.mvpMatrix.m31);
        this.floatBuffer.put(this.mvpMatrix.m02);
        this.floatBuffer.put(this.mvpMatrix.m12);
        this.floatBuffer.put(this.mvpMatrix.m22);
        this.floatBuffer.put(this.mvpMatrix.m32);
        this.floatBuffer.put(this.mvpMatrix.m03);
        this.floatBuffer.put(this.mvpMatrix.m13);
        this.floatBuffer.put(this.mvpMatrix.m23);
        this.floatBuffer.put(this.mvpMatrix.m33);
        this.floatBuffer.flip();
        return this.floatBuffer;
    }

    public void getGeometry(int val1) {
        this.updateParticleParams();
        GL20.glEnableVertexAttribArray(0);
        funcs.glBindBuffer(34962, this.particleVertexBuffer);
        GL20.glVertexAttribPointer(0, 3, 5126, false, 0, 0L);
        GL33.glVertexAttribDivisor(0, 0);
        GL31.glDrawArraysInstanced(5, 0, 4, this.getParticleCount());
    }

    public void getGeometryFire(int val1) {
        this.updateParticleParams();
        GL20.glEnableVertexAttribArray(0);
        funcs.glBindBuffer(34962, this.particleVertexBuffer);
        GL20.glVertexAttribPointer(0, 3, 5126, false, 0, 0L);
        GL33.glVertexAttribDivisor(0, 0);
        GL31.glDrawArraysInstanced(5, 0, 4, this.getParticleCount());
    }

    public float getShaderTime() {
        return this.particlesTime;
    }

    abstract void createParticleBuffers();

    abstract void destroyParticleBuffers();

    abstract void updateParticleParams();

    abstract int getParticleCount();
}
